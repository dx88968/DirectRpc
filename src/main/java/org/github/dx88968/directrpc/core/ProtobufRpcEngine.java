/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.github.dx88968.directrpc.core;


import com.google.protobuf.BlockingService;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.ServiceException;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.SocketFactory;

import org.github.dx88968.directrpc.core.RPC.RPCServer;
import org.github.dx88968.directrpc.coreProtos.RequestHeaderProtos.RequestHeaderProto;
import org.github.dx88968.directrpc.coreProtos.RequestHeaderProtos.ResponseHeaderProto;
import org.github.dx88968.directrpc.io.DataOutputOutputStream;
import org.github.dx88968.directrpc.io.Writable;

/**
 *
 * @author DX
 */
public class ProtobufRpcEngine implements RpcEngine{
    
    private static final ClientCache CLIENTS = new ClientCache();
    
  interface RpcWrapper extends Writable {
    int getLength();
  }
  /**
   * Wrapper for Protocol Buffer Requests
   * 
   * Note while this wrapper is writable, the request on the wire is in
   * Protobuf. Several methods on {@link org.apache.hadoop.ipc.Server and RPC} 
   * use type Writable as a wrapper to work across multiple RpcEngine kinds.
   */
  public static class RpcRequestWrapper implements RpcWrapper {
    RequestHeaderProto requestHeader;
    Message theRequest; // for clientSide, the request is here
    byte[] theRequestRead; // for server side, the request is here

    public RpcRequestWrapper() {
    }

    public RpcRequestWrapper(RequestHeaderProto requestHeader, Message theRequest) {
      this.requestHeader = requestHeader;
      this.theRequest = theRequest;
    }

    
    public void write(DataOutput out) throws IOException {
      OutputStream os = DataOutputOutputStream.constructOutputStream(out);
      
      requestHeader.writeDelimitedTo(os);
      theRequest.writeDelimitedTo(os);
    }

    
    public void readFields(DataInput in) throws IOException {
      int length = ProtoUtils.readRawVarint32(in);
      byte[] bytes = new byte[length];
      in.readFully(bytes);
      requestHeader = RequestHeaderProto.parseFrom(bytes);
      length = ProtoUtils.readRawVarint32(in);
      theRequestRead = new byte[length];
      in.readFully(theRequestRead);
    }
    
    
    public String toString() {
      return requestHeader.getMethodName();
    }

    
    public int getLength() {
      int headerLen = requestHeader.getSerializedSize();
      int reqLen;
      if (theRequest != null) {
        reqLen = theRequest.getSerializedSize();
      } else if (theRequestRead != null ) {
        reqLen = theRequestRead.length;
      } else {
        throw new IllegalArgumentException(
            "getLenght on uninilialized RpcWrapper");      
      }
      return CodedOutputStream.computeRawVarint32Size(headerLen) +  headerLen
          + CodedOutputStream.computeRawVarint32Size(reqLen) + reqLen;
    }
  }

  /**
   *  Wrapper for Protocol Buffer Responses
   * 
   * Note while this wrapper is writable, the request on the wire is in
   * Protobuf. Several methods on {@link org.apache.hadoop.ipc.Server and RPC} 
   * use type Writable as a wrapper to work across multiple RpcEngine kinds.
   */
  public static class RpcResponseWrapper implements RpcWrapper {
    public ResponseHeaderProto  header;  
    Message theResponse; // for senderSide, the response is here
    public byte[] theResponseRead; // for receiver side, the response is here

    public RpcResponseWrapper() {
    }

    public RpcResponseWrapper(ResponseHeaderProto  header,Message message) {
      this.header=header;
      this.theResponse = message;
    }

   
    public void write(DataOutput out) throws IOException {
      OutputStream os = DataOutputOutputStream.constructOutputStream(out);
      header.writeDelimitedTo(os);
      theResponse.writeDelimitedTo(os);   
    }

    
    public void readFields(DataInput in) throws IOException {
      int length = ProtoUtils.readRawVarint32(in);
      byte[] bytes = new byte[length];
      in.readFully(bytes);
      header=ResponseHeaderProto .parseFrom(bytes);
      length = ProtoUtils.readRawVarint32(in);
      theResponseRead = new byte[length];
      in.readFully(theResponseRead);
    }
    
    
    public int getLength() {
      int resLen;
      if (theResponse != null) {
        resLen = theResponse.getSerializedSize();
      } else if (theResponseRead != null ) {
        resLen = theResponseRead.length;
      } else {
        throw new IllegalArgumentException(
            "getLenght on uninilialized RpcWrapper");      
      }
      return CodedOutputStream.computeRawVarint32Size(resLen) + resLen;
    }

       
  }
    
  private static class Invoker implements InvocationHandler{
      
    private final InetSocketAddress remoteId;
    private final Client client;
    private final String protocolName;
    private final Map<String, Message> returnTypes = 
        new ConcurrentHashMap<>();
      /**
     * This constructor takes a connectionId, instead of creating a new one.
     */
    public Invoker(Class<?> protocol, InetSocketAddress connId, SocketFactory factory,int rpcTimeout) {
      this.remoteId = connId;
      this.client = CLIENTS.getClient(connId);
      this.client.setRpcTimeout(rpcTimeout);
      this.protocolName = protocol.getName();
    }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            RequestHeaderProto rpcRequestHeader = constructRpcRequestHeader(method);
            RpcResponseWrapper val = null;
            Message theRequest = (Message) args[1];
            try {
                val = (RpcResponseWrapper) client.call(ProtobufRpcEngine.class.getName(),
                    new RpcRequestWrapper(rpcRequestHeader, theRequest), remoteId,0);

            } catch (InterruptedException | IOException e) {
              throw new ServiceException(e);
            }
            Message prototype = null;
            try {
              prototype = getReturnProtoType(method);
            } catch (Exception e) {
              throw new ServiceException(e);
            }
            Message returnMessage;
            try {
                returnMessage = prototype.newBuilderForType()
                    .mergeFrom(val.theResponseRead).build();
              } catch (Throwable e) {
                throw new ServiceException(e);
              }
            return returnMessage;
        }
        
          private RequestHeaderProto constructRpcRequestHeader(Method method) {
            RequestHeaderProto.Builder builder = RequestHeaderProto
                .newBuilder();
            builder.setMethodName(method.getName());


            // For protobuf, {@code protocol} used when creating client side proxy is
            // the interface extending BlockingInterface, which has the annotations 
            // such as ProtocolName etc.
            //
            // Using Method.getDeclaringClass(), as in WritableEngine to get at
            // the protocol interface will return BlockingInterface, from where 
            // the annotation ProtocolName and Version cannot be
            // obtained.
            //
            // Hence we simply use the protocol class used to create the proxy.
            // For PB this may limit the use of mixins on client side.
            builder.setProtocolName(protocolName);
            return builder.build();
          }
          
          private Message getReturnProtoType(Method method) throws Exception {
            if (returnTypes.containsKey(method.getName())) {
              return returnTypes.get(method.getName());
            }

            Class<?> returnType = method.getReturnType();
            Method newInstMethod = returnType.getMethod("getDefaultInstance");
            newInstMethod.setAccessible(true);
            Message prototype = (Message) newInstMethod.invoke(null, (Object[]) null);
            returnTypes.put(method.getName(), prototype);
            return prototype;
          }
          
  }
  

    

    @Override
    public <T> ProtocolProxy<T> getProxy(Class<T> protocol, InetSocketAddress addr, SocketFactory factory, int rpcTimeout) throws IOException {
        final Invoker invoker = new Invoker(protocol, addr, factory,rpcTimeout);
        return new ProtocolProxy<>((T)Proxy.newProxyInstance(protocol.getClassLoader(), new Class[]{protocol}, invoker));
    }

    @Override
    public RPCServer getServer(Class<?> protocol, Object instance, String bindAddress, int port, int numHandlers, int numReaders, int queueSizePerHandler, boolean verbose, String portRangeConfig, String owner) throws IOException {
        ProtobufRpcServer server = new ProtobufRpcServer();
        server.setIP(new InetSocketAddress(bindAddress,port));
        server.setNumHandlers(numHandlers);
        server.setNumReaders(numReaders);
        server.setMaxQueueSize(queueSizePerHandler);
        server.registerProtocolImpl(protocol.getName(), instance);
        server.setOwner(owner);
        return server;
    }
    
    private class ProtobufRpcServer extends RPC.RPCServer{
        
        public ProtobufRpcServer() throws IOException{
            super();
        }
        
        @Override
        Writable call(Writable rpcRequest) {
            RpcRequestWrapper request = (RpcRequestWrapper) rpcRequest;
            RequestHeaderProto header = request.requestHeader;
            BlockingService service=(BlockingService)protocols.get(header.getProtocolName());
            if(service!=null){
                try {
                MethodDescriptor methodDescriptor = service.getDescriptorForType()
                .findMethodByName(header.getMethodName());
                Message prototype = service.getRequestPrototype(methodDescriptor);
                Message param = prototype.newBuilderForType().mergeFrom(request.theRequestRead).build();
                Message result = service.callBlockingMethod(methodDescriptor, null, param);
                ResponseHeaderProto resHeader=ResponseHeaderProto .newBuilder().setResultCode(0).build();
                RpcResponseWrapper response=new RpcResponseWrapper(resHeader,result);
                return response;
                } catch (Exception ex) {
                    Logger.getLogger(ProtobufRpcEngine.class.getName()).log(Level.SEVERE, null, ex);
                    ResponseHeaderProto resHeader=ResponseHeaderProto .newBuilder().setResultCode(1).setMessage("Runtime Exception on server side:"+ex).build();
                    RpcResponseWrapper response=new RpcResponseWrapper(resHeader,resHeader);
                    return response;
                }
            }
            ResponseHeaderProto resHeader=ResponseHeaderProto .newBuilder().setResultCode(1).setMessage(header.getProtocolName()+" is not registered in Server's object container").build();
            RpcResponseWrapper response=new RpcResponseWrapper(resHeader,resHeader);
            return response;
        }
        
    }
    
}
