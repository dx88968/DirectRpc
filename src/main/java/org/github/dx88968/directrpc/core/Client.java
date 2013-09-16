/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.github.dx88968.directrpc.core;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.SocketChannel;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.SocketFactory;

import org.github.dx88968.directrpc.core.ProtobufRpcEngine.RpcRequestWrapper;
import org.github.dx88968.directrpc.core.ProtobufRpcEngine.RpcResponseWrapper;
import org.github.dx88968.directrpc.coreProtos.RpcHeaderProtos.RpcRequestHeaderProto;
import org.github.dx88968.directrpc.coreProtos.RpcHeaderProtos.RpcResponseHeaderProto;
import org.github.dx88968.directrpc.io.DataOutputBuffer;
import org.github.dx88968.directrpc.io.SocketIOWithTimeout;
import org.github.dx88968.directrpc.io.Writable;

/**
 *
 * @author DX
 */
public class Client {
    static Integer id=0;
    static int counter=0;
    private AtomicBoolean running = new AtomicBoolean(true); // if client runs
    private SocketFactory socketFactory; 
    private Hashtable<InetSocketAddress, Connection> connections =new Hashtable();
    private int rpcTimeout=0;
    public final static int PING_CALL_ID = -1;
    public final static int TERMINATE_CALL_ID = -2;

    public void setRpcTimeout(int rpcTimeout) {
        this.rpcTimeout = rpcTimeout;
    }
    
     /**
   * Executor on which IPC calls' parameters are sent. Deferring
   * the sending of parameters to a separate thread isolates them
   * from thread interruptions in the calling code.
   */
  private static final ExecutorService SEND_PARAMS_EXECUTOR = 
    Executors.newCachedThreadPool(
        new ThreadFactoryBuilder()
        .setDaemon(true)
        .setNameFormat("IPC Parameter Sending Thread #%d")
        .build());
    
    public Client(){
        socketFactory=SocketFactory.getDefault();
    }
    
    private class Call {
    final int id;               // call id
    final Writable rpcRequest;  // the serialized rpc request
    Writable rpcResponse;       // null if rpc has error
    IOException error;          // exception, null if success
    final String rpcKind;      // Rpc EngineKind
    boolean done;               // true when call is done
    long startAt;
            
    protected Call(String rpcKind, Writable param) {
      this.rpcKind = rpcKind;
      this.rpcRequest = param;
      synchronized (Client.this) {
        this.id = counter++;
      }
    }

    /** Indicate when the call is complete and the
     * value or error are available.  Notifies by default.  */
    protected synchronized void callComplete() {
      this.done = true;
      notify();                                 // notify caller
    }

    /** Set the exception when there is an error.
     * Notify the caller the call is done.
     * 
     * @param error exception thrown by the call; either local or remote
     */
    public synchronized void setException(IOException error) {
      this.error = error;
      callComplete();
    }
    
    /** Set the return value when there is no error. 
     * Notify the caller the call is done.
     * 
     * @param rpcResponse return value of the rpc call.
     */
    public synchronized void setRpcResponse(Writable rpcResponse) {
      this.rpcResponse = rpcResponse;
      callComplete();
    }
    
    public synchronized Writable getRpcResponse() {
      return rpcResponse;
    }
    
    public synchronized void setStartTime(long time){
        this.startAt=time;
    }
    
    public synchronized boolean vaildate(){
        if(rpcTimeout==0){//Timeout is not set
            return true;
        }
        if(System.currentTimeMillis()-startAt>rpcTimeout){
            error=new RemoteException("Server didn't respond within required time,\n if you want to wait, set rpcTimeout=0");
            callComplete();
            return false;
        }
        return true;
    }
  }
    
      /** Thread that reads responses and notifies callers.  Each connection owns a
   * socket connected to a remote address.  Calls are multiplexed through this
   * socket: responses may be delivered out of order. */
  private class Connection extends Thread {
    private InetSocketAddress server;             // server ip:port
    private String serverPrincipal;  // server's krb5 principal name
    private int serviceClass;
    
    private Socket socket = null;                 // connected socket
    private DataInputStream in;
    private DataOutputStream out;
    private int rpcTimeout;
    private int maxIdleTime; //connections will be culled if it was idle for 
    //maxIdleTime msecs
    private int maxRetriesOnSocketTimeouts;
    private boolean tcpNoDelay; // if T then disable Nagle's Algorithm
    private boolean doPing; //do we need to send ping message
    private int pingInterval; // how often sends ping to the server in msecs
    private int connectionTimeout;
    
    // currently active calls
    private Hashtable<Integer, Call> calls = new Hashtable<Integer, Call>();
    private AtomicLong lastActivity = new AtomicLong();// last I/O activity time
    private AtomicBoolean shouldCloseConnection = new AtomicBoolean();  // indicate if the connection is closed
    private IOException closeException; // close reason
    
    private final Object sendRpcRequestLock = new Object();

    public Connection(InetSocketAddress address, int rpcTimeout, int maxIdleTime,int maxRetriesOnSocketTimeouts,int connectionTimeout,boolean tcpNoDelay, boolean doPing,
            int pingInterval,int serviceClass) throws IOException {
      this.server = address;
      if (server.isUnresolved()) {
        
      }
      this.rpcTimeout = rpcTimeout;
      this.maxIdleTime = maxIdleTime;
      //Implement Retry Policy if possible
      this.maxRetriesOnSocketTimeouts = maxRetriesOnSocketTimeouts;
      this.tcpNoDelay = tcpNoDelay;
      this.doPing = doPing;
      this.pingInterval = pingInterval;
      this.serviceClass = serviceClass;
      this.connectionTimeout=connectionTimeout;
      
      
      this.setName("IPC Client connection to " +
          server.toString() +
          " from an unknown user");
      this.setDaemon(true);
    }

    /** Update lastActivity with the current time. */
    private void touch() {
      lastActivity.set(System.currentTimeMillis());
    }

    /**
     * Add a call to this connection's call queue and notify
     * a listener; synchronized.
     * Returns false if called during shutdown.
     * @param call to add
     * @return true if the call was added.
     */
    private synchronized boolean addCall(Call call) {
      if (shouldCloseConnection.get())
        return false;
      calls.put(call.id, call);
      notify();
      return true;
    }

    /** This class sends a ping to the remote side when timeout on
     * reading. If no failure is detected, it retries until at least
     * a byte is read.
     */
    private class PingInputStream extends FilterInputStream {
      /* constructor */
      protected PingInputStream(InputStream in) {
        super(in);
      }

      /* Process timeout exception
       * if the connection is not going to be closed or 
       * is not configured to have a RPC timeout, send a ping.
       * (if rpcTimeout is not set to be 0, then RPC should timeout.
       * otherwise, throw the timeout exception.
       */
      private void handleTimeout(SocketTimeoutException e) throws IOException {
        if (shouldCloseConnection.get() || !running.get()) {
          throw e;
        } else {
          sendPing();
          vaildCalls();
        }
      }
      
      /** Read a byte from the stream.
       * Send a ping if timeout on read. Retries if no failure is detected
       * until a byte is read.
       * @throws IOException for any IO problem other than socket timeout
       */
      @Override
      public int read() throws IOException {
        do {
          try {
            return super.read();
          } catch (SocketTimeoutException e) {
            handleTimeout(e);
          }
        } while (true);
      }

      /** Read bytes into a buffer starting from offset <code>off</code>
       * Send a ping if timeout on read. Retries if no failure is detected
       * until a byte is read.
       * 
       * @return the total number of bytes read; -1 if the connection is closed.
       */
      @Override
      public int read(byte[] buf, int off, int len) throws IOException {
        do {
          try {
            return super.read(buf, off, len);
          } catch (SocketTimeoutException e) {
            handleTimeout(e);
          }
        } while (true);
      }
    }
    


    /**
     * Update the server address if the address corresponding to the host
     * name has changed.
     *
     * @return true if an addr change was detected.
     * @throws IOException when the hostname cannot be resolved.
     */
    private synchronized boolean updateAddress() throws IOException {
      // Do a fresh lookup with the old host name.
      InetSocketAddress currentAddr = new InetSocketAddress(
                               server.getHostName(), server.getPort());

      if (!server.equals(currentAddr)) {
        server = currentAddr;
        return true;
      }
      return false;
    }
    
    private synchronized void setupConnection() throws IOException {
      short ioFailures = 0;
      short timeoutFailures = 0;
      while (true) {
        try {
          this.socket = socketFactory.createSocket();
          this.socket.setTcpNoDelay(tcpNoDelay);
          connect(this.socket, server,null, connectionTimeout);
          if (rpcTimeout > 0) {
            pingInterval = rpcTimeout;  // rpcTimeout overwrites pingInterval
          }
          this.socket.setSoTimeout(pingInterval);
          return;
        } catch (ConnectTimeoutException toe) {
          /* Check for an address change and update the local reference.
           * Reset the failure counter if the address was changed
           */
          if (updateAddress()) {
            timeoutFailures = ioFailures = 0;
          }
          handleConnectionTimeout(timeoutFailures++,maxRetriesOnSocketTimeouts, toe);
        } catch (IOException ie) {
          if (updateAddress()) {
            timeoutFailures = ioFailures = 0;
          }
          handleConnectionFailure(ioFailures++, ie);
        }
      }
    }
    

   

    
    /** Connect to the server and set up the I/O streams. It then sends
     * a header to the server and starts
     * the connection thread that waits for responses.
     */
    private synchronized void setupIOstreams() throws InterruptedException, ConnectionFailedException {
      if (socket != null || shouldCloseConnection.get()) {
        return;
      } 
      try {
        short numRetries = 0;
        final short MAX_RETRIES = 5;
        
        while (true) {
          setupConnection();
          InputStream inStream = socket.getInputStream();
          OutputStream outStream = socket.getOutputStream();
          writeConnectionHeader(outStream);
          
        
          if (doPing) {
            this.in = new DataInputStream(new BufferedInputStream(
                new PingInputStream(inStream)));
          } else {
            this.in = new DataInputStream(new BufferedInputStream(inStream));
          }
          this.out = new DataOutputStream(new BufferedOutputStream(outStream));
          
          // update last activity time
          touch();

          // start the receiver thread after the socket connection has been set
          // up
          start();
          return;
        }
      } catch (Throwable t) {
        if (t instanceof IOException) {
          markClosed((IOException)t);
        } else {
          markClosed(new IOException("Couldn't set up IO streams", t));
        }
        close();
      }
    }
    
    private void closeConnection() {
      if (socket == null) {
        return;
      }
      // close the current connection
      try {
        socket.close();
      } catch (IOException e) {
        System.out.println("Not able to close a socket");
      }
      // set socket to null so that the next call to setupIOstreams
      // can start the process of connect all over again.
      socket = null;
    }

    /* Handle connection failures due to timeout on connect
     *
     * If the current number of retries is equal to the max number of retries,
     * stop retrying and throw the exception; Otherwise backoff 1 second and
     * try connecting again.
     *
     * This Method is only called from inside setupIOstreams(), which is
     * synchronized. Hence the sleep is synchronized; the locks will be retained.
     *
     * @param curRetries current number of retries
     * @param maxRetries max number of retries allowed
     * @param ioe failure reason
     * @throws IOException if max number of retries is reached
     */
    private void handleConnectionTimeout(
        int curRetries, int maxRetries, IOException ioe) throws IOException {

      closeConnection();

      // throw the exception if the maximum number of retries is reached
      if (curRetries >= maxRetries) {
        throw ioe;
      }
      System.out.println("Retrying connect to server: " + server + ". Already tried "
          + curRetries + " time(s); maxRetries=" + maxRetries);
    }

    private void handleConnectionFailure(int curRetries, IOException ioe
        ) throws IOException {
      closeConnection();
      //To be implemented
    }

    /**
     * Write the connection header - this is sent when connection is established
     * +----------------------------------+
     * |  "hrpc" 4 bytes                  |      
     * +----------------------------------+
     * |  Version (1 byte)                |
     * +----------------------------------+
     * |  Service Class (1 byte)          |
     * +----------------------------------+
     * |  Authmethod (1 byte)             |      
     * +----------------------------------+
     * |  IpcSerializationType (1 byte)   |      
     * +----------------------------------+
     */
    private void writeConnectionHeader(OutputStream outStream)
        throws IOException {
      DataOutputStream out = new DataOutputStream(new BufferedOutputStream(outStream));
      // Write out the header, version and authentication method
      out.writeBytes("hrpc");
      out.flush();
    }
    
    /* wait till someone signals us to start reading RPC response or
     * it is idle too long, it is marked as to be closed, 
     * or the client is marked as not running.
     * 
     * Return true if it is time to read a response; false otherwise.
     */
    private synchronized boolean waitForWork() {
      if (calls.isEmpty() && !shouldCloseConnection.get()  && running.get())  {
        long timeout = maxIdleTime-
              (System.currentTimeMillis()-lastActivity.get());
        if (timeout>0) {
          try {
            wait(timeout);
          } catch (InterruptedException e) {}
        }
      }
      
      if (!calls.isEmpty() && !shouldCloseConnection.get() && running.get()) {
        return true;
      } else if (shouldCloseConnection.get()) {
        return false;
      } else if (calls.isEmpty()) { // idle connection closed or stopped
        markClosed(null);
        return false;
      } else { // get stopped but there are still pending requests 
        markClosed((IOException)new IOException().initCause(
            new InterruptedException()));
        return false;
      }
    }

    public InetSocketAddress getRemoteAddress() {
      return server;
    }
    
    private synchronized void vaildCalls() {
          Iterator<Integer> iter = calls.keySet().iterator();
          while(iter.hasNext()){
              Integer id=iter.next();
              if(!calls.get(id).vaildate()){
                  try {
                      sendTerminateSignal(id);
                  } catch (IOException ex) {
                      ex.printStackTrace();
                  }
                  calls.remove(id);
              }
          }
    }

    /* Send a ping to the server if the time elapsed 
     * since last I/O activity is equal to or greater than the ping interval
     */
    private synchronized void sendPing() throws IOException {
      long curTime = System.currentTimeMillis();
      if ( curTime - lastActivity.get() >= pingInterval) {
        lastActivity.set(curTime);
        synchronized (out) {
          out.writeInt(PING_CALL_ID);
          out.flush();
        }
      }
    }
    
    private synchronized void sendTerminateSignal(int Callid) throws IOException{
        synchronized (out) {
          out.writeInt(TERMINATE_CALL_ID);
          out.writeInt(Callid);
          out.flush();
        }
    }

    @Override
    public void run() {
      try {
        while (waitForWork()) {//wait here for work - read or close connection
          receiveRpcResponse();
        }
      } catch (Throwable t) {
        // This truly is unexpected, since we catch IOException in receiveResponse
        // -- this is only to be really sure that we don't leave a client hanging
        // forever.
        System.out.println("Unexpected error reading responses on connection " + this);
        markClosed(new IOException("Error reading responses", t));
      }
        try {
            close();
        } catch (ConnectionFailedException ex) { }
      
    }

    /** Initiates a rpc call by sending the rpc request to the remote server.
     * Note: this is not called from the Connection thread, but by other
     * threads.
     * @param call - the rpc request
     */
    public void sendRpcRequest(final Call call)
        throws InterruptedException, IOException {
      if (shouldCloseConnection.get()) {
        return;
      }

      // Serialize the call to be sent. This is done from the actual
      // caller thread, rather than the SEND_PARAMS_EXECUTOR thread,
      // so that if the serialization throws an error, it is reported
      // properly. This also parallelizes the serialization.
      //
      // Format of a call on the wire:
      // 0) Length of rest below (1 + 2)
      // 1) RpcRequestHeader  - is serialized Delimited hence contains length
      // 2) RpcRequest
      //
      // Items '1' and '2' are prepared here. 
      final DataOutputBuffer d = new DataOutputBuffer();
      RpcRequestHeaderProto RpcHeader=RpcRequestHeaderProto.newBuilder().setCallid(call.id).build();
      RpcHeader.writeDelimitedTo(d);
      call.rpcRequest.write(d);


      synchronized (sendRpcRequestLock) {
        Future<?> senderFuture = SEND_PARAMS_EXECUTOR.submit(new Runnable() {
          
          public void run() {
            try {
              synchronized (Connection.this.out) {
                if (shouldCloseConnection.get()) {
                  return;
                }
                byte[] data = d.getData();
                int totalLength = data.length;
                out.writeInt(totalLength); // Total Length
                out.write(data, 0, totalLength);// RpcRequestHeader + RpcRequest
                out.flush();
              }
            } catch (IOException e) {
              // exception at this point would leave the connection in an
              // unrecoverable state (eg half a call left on the wire).
              // So, close the connection, killing any outstanding calls
              markClosed(e);
            }
          }
        });
      
        try {
          senderFuture.get();
        } catch (Exception e) {
          Throwable cause = e.getCause();
          
          // cause should only be a RuntimeException as the Runnable above
          // catches IOException
          if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
          } else {
            throw new RuntimeException("unexpected checked exception", cause);
          }
        }
      }
    }

    /* Receive a response.
     * Because only one receiver, so no synchronization on in.
     */
    private void receiveRpcResponse() {
      if (shouldCloseConnection.get()) {
        return;
      }
      touch();
      try {
        int callId=in.readInt();
        RpcResponseHeaderProto header=RpcResponseHeaderProto.parseDelimitedFrom(in); 
        RpcResponseWrapper response=new RpcResponseWrapper();
          response.readFields(in);
        if (response.header == null) {
          throw new IOException("Response is null.");
        }
        
        Call call = calls.get(header.getCallid());
        int status = response.header.getResultCode();
        if (status == 0) {//Code 0 means success
          
          /*
          int length=ProtoUtils.readRawVarint32(in);//Get the length of the buffer
          byte[] responseBuffer=new byte[length];
          in.readFully(responseBuffer);*/
          call.setRpcResponse(response);
          calls.remove(callId);
          
          // verify that length was correct------------------To be implemented
          // only for ProtobufEngine where len can be verified easily
          /*
          if (call.getRpcResponse() instanceof Message) {
            Message resWrapper = 
                (ProtobufRpcEngine.RpcWrapper) call.getRpcResponse();
            if (totalLen != headerLen + resWrapper.getLength()) { 
              throw new RpcClientException(
                  "RPC response length mismatch on rpc success");
            }
          }*/
        } else { // Rpc Request failed
          final String errorMsg = response.header.hasMessage() ? 
                response.header.getMessage() : "ServerDidNotSetErrorMsg" ;
          final int erCode = response.header.getResultCode();   
          if (erCode == 0) {
             System.out.println("Detailed error code not set by server on rpc error");
          }
          RemoteException re = 
              ( (erCode == 0) ? 
                  new RemoteException( errorMsg) :
              new RemoteException(errorMsg, erCode));
          if (status == 1) {//1 means ERROR
            call.setException(re);
            calls.remove(callId);
          } else if (status == 2) {//2 means fatal error and the connection must close
            // Close the connection
            markClosed(re);
          }
        }
      } 
      catch(SocketTimeoutException timeout){
          System.out.println(timeout);
          
      }
      catch (IOException e) {
        markClosed(e);
      }
    }
    
    private synchronized void markClosed(IOException e) {
      if (shouldCloseConnection.compareAndSet(false, true)) {
        closeException = e;
        notifyAll();
      }
    }
    
    /** Close the connection. */
    private synchronized void close() throws ConnectionFailedException  {
      if (!shouldCloseConnection.get()) {
        return;
      }
      connections.remove(server);
        try {
            // close the streams and therefore the socket
            out.close();
            in.close();
        } catch (IOException ex) {}
        catch(NullPointerException nx){
            cleanupCalls();
            throw new ConnectionFailedException("Can not connect to server");
        }
      // clean up all calls
      if (closeException == null) {
        if (!calls.isEmpty()) {
          System.out.println(
              "A connection is closed for no cause and calls are not empty");

          // clean up calls anyway
          closeException = new IOException("Unexpected closed connection");
          cleanupCalls();
        }
      } else {

        // cleanup calls
        cleanupCalls();
      }
    }
    
    /* Cleanup all calls and mark them as done */
    private void cleanupCalls() {
      Iterator<Entry<Integer, Call>> itor = calls.entrySet().iterator() ;
      while (itor.hasNext()) {
        Call c = itor.next().getValue(); 
        c.setException(closeException); // local exception
        itor.remove();         
      }
    }
  }
  

  /**
   * Like {@link NetUtils#connect(Socket, SocketAddress, int)} but
   * also takes a local address and port to bind the socket to. 
   * 
   * @param socket
   * @param endpoint the remote address
   * @param localAddr the local address to bind the socket to
   * @param timeout timeout in milliseconds
   */
  public static void connect(Socket socket, 
                             SocketAddress endpoint,
                             SocketAddress localAddr,
                             int timeout) throws IOException, ConnectTimeoutException {
    if (socket == null || endpoint == null || timeout < 0) {
      throw new IllegalArgumentException("Illegal argument for connect()");
    }
    
    SocketChannel ch = socket.getChannel();
    
    if (localAddr != null) {
      socket.bind(localAddr);
    }

    try {
      if (ch == null) {
        // let the default implementation handle it.
        socket.connect(endpoint, timeout);
      } else {
        SocketIOWithTimeout.connect(ch, endpoint, timeout);
      }
    } catch (SocketTimeoutException ste) {
      throw new ConnectTimeoutException(ste.getMessage());
    }

    // There is a very rare case allowed by the TCP specification, such that
    // if we are trying to connect to an endpoint on the local machine,
    // and we end up choosing an ephemeral port equal to the destination port,
    // we will actually end up getting connected to ourself (ie any data we
    // send just comes right back). This is only possible if the target
    // daemon is down, so we'll treat it like connection refused.
    if (socket.getLocalPort() == socket.getPort() &&
        socket.getLocalAddress().equals(socket.getInetAddress())) {
      System.out.println("Detected a loopback TCP socket, disconnecting it");
      socket.close();
      throw new ConnectException(
        "Localhost targeted connection resulted in a loopback. " +
        "No daemon is listening on the target port.");
    }
  }
  
    /** Get a connection from the pool, or create a new one and add it to the
   * pool.  Connections to a given ConnectionId are reused. */
  private Connection getConnection(InetSocketAddress remoteId,
                                   Call call, int serviceClass)
                                   throws IOException, InterruptedException, ConnectionFailedException {
    if (!running.get()) {
      // the client is stopped
      throw new IOException("The client is stopped");
    }
    Connection connection;
    /* we could avoid this allocation for each RPC by having a  
     * connectionsId object and with set() method. We need to manage the
     * refs for keys in HashMap properly. For now its ok.
     */
    do {
      synchronized (connections) {
        connection = connections.get(remoteId);
        if (connection == null) {
          connection = new Connection(remoteId,rpcTimeout, 1000,3,5,true, true,
            10000,0);
          connections.put(remoteId, connection);
        }
      }
    } while (!connection.addCall(call));
    
    //we don't invoke the method below inside "synchronized (connections)"
    //block above. The reason for that is if the server happens to be slow,
    //it will take longer to establish a connection and that will slow the
    //entire system down.
    connection.setupIOstreams();
    return connection;
  }
    
    /** 
   * Make a call, passing <code>rpcRequest</code>, to the IPC server defined by
   * <code>remoteId</code>, returning the rpc respond.
   * 
   * @param rpcKind
   * @param rpcRequest -  contains serialized method and method parameters
   * @param remoteId - the target rpc server
   * @param serviceClass - service class for RPC
   * @returns the rpc response
   * Throws exceptions if there are network problems or if the remote code 
   * threw an exception.
   */
  public Writable call(String rpcKind, Writable rpcRequest,
      InetSocketAddress remoteId, int serviceClass)
      throws InterruptedException, IOException {
    RpcRequestWrapper request=(RpcRequestWrapper) rpcRequest;
    Call call = new Call(rpcKind, request);
    call.setStartTime(System.currentTimeMillis());
    Connection connection = getConnection(remoteId, call, serviceClass);
    try {
      connection.sendRpcRequest(call);                 // send the rpc request
    } catch (RejectedExecutionException e) {
      throw new IOException("connection has been closed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.out.println("interrupted waiting to send rpc request to server");
      throw new IOException(e);
    }

    boolean interrupted = false;
    synchronized (call) {
      while (!call.done) {
        try {
          call.wait();                           // wait for the result
        } catch (InterruptedException ie) {
          // save the fact that we were interrupted
          interrupted = true;
        }
      }

      if (interrupted) {
        // set the interrupt flag now that we are done waiting
        Thread.currentThread().interrupt();
      }

      if (call.error != null) {
        if (call.error instanceof RemoteException) {
          call.error.fillInStackTrace();
          throw call.error;
        } else { // local exception
          throw call.error;
        }
      } else {
        return call.getRpcResponse();
      }
    }
  }
  
  
   /** Stop all threads related to this client.  No further calls may be made
   * using this client. */
  public void stop() {


    if (!running.compareAndSet(true, false)) {
      return;
    }
    
    // wake up all connections
    synchronized (connections) {
      for (Connection conn : connections.values()) {
        conn.interrupt();
      }
    }
    
    // wait until all connections are closed
    while (!connections.isEmpty()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
      }
    }
  }
  
}
