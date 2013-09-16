/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.github.dx88968.directrpc.core;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.SocketFactory;

/**
 *
 * @author DX
 */
public class RPC {
    
    private static final Class<?>[] EMPTY_ARRAY = new Class[]{};
    static HashMap<Class<?>,RpcEngine> PROTOCOL_ENGINES=new HashMap();
   
   
   public static <T> ProtocolProxy<T> getProtocolProxy (Class<T> protocol,
                                InetSocketAddress addr,SocketFactory factory,
                                int rpcTimeout) throws IOException {   
    return getProtocolEngine(protocol).getProxy(protocol,addr, factory, rpcTimeout);
  }
   
   public static synchronized void setProtocolEngine(Class<?> protocol,Class<? extends RpcEngine> rpcEngine){
       try {
              Class<?> impl = Class.forName(rpcEngine.getName(), true, Thread.currentThread().getContextClassLoader());
              Constructor<?> meth = impl.getDeclaredConstructor(EMPTY_ARRAY);
              meth.setAccessible(true);
              RpcEngine engine = (RpcEngine) meth.newInstance();
              PROTOCOL_ENGINES.put(protocol, engine);
          } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
              Logger.getLogger(RPC.class.getName()).log(Level.SEVERE, null, ex);
          }
      
   }

    
      // return the RpcEngine configured to handle a protocol
  static synchronized RpcEngine getProtocolEngine(Class<?> protocol) {
    RpcEngine engine = PROTOCOL_ENGINES.get(protocol);
    if (engine == null) {
          try {
              Class<?> impl = Class.forName(ProtobufRpcEngine.class.getName(), true, Thread.currentThread().getContextClassLoader());
              Constructor<?> meth = impl.getDeclaredConstructor(EMPTY_ARRAY);
              meth.setAccessible(true);
              engine=(RpcEngine) meth.newInstance();
          } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
              Logger.getLogger(RPC.class.getName()).log(Level.SEVERE, null, ex);
          }
          PROTOCOL_ENGINES.put(protocol, engine);
    }
    return engine;
  }
  
  /** An RPC Server. */
  public abstract static class RPCServer extends Server {
      
      public RPCServer() throws IOException{
          super();
      }
      
      
        Map<String,Object> protocols=new HashMap();
        
        public void registerProtocolImpl(String name,Object impl){
            protocols.put(name, impl);
        }


        boolean verbose;
        static String classNameBase(String className) {
           String[] names = className.split("\\.", -1);
           if (names == null || names.length == 0) {
             return className;
           }
           return names[names.length-1];
         }

        /**
         * Store a map of protocol and version to its implementation
         */
        /**
         *  The key in Map
         */
        static class ProtoNameVer {
          final String protocol;
          final long   version;
          ProtoNameVer(String protocol, long ver) {
            this.protocol = protocol;
            this.version = ver;
          }
          @Override
          public boolean equals(Object o) {
            if (o == null) {
                  return false;
              }
            if (this == o) {
                  return true;
              }
            if (! (o instanceof ProtoNameVer)) {
                  return false;
              }
            ProtoNameVer pv = (ProtoNameVer) o;
            return ((pv.protocol.equals(this.protocol)) && 
                (pv.version == this.version));     
          }
          @Override
          public int hashCode() {
            return protocol.hashCode() * 37 + (int) version;    
          }
        }
  }
     /**
   * Class to construct instances of RPC server with specific options.
   */
  public static class Builder {
    private Class<?> protocol = null;
    private Object instance = null;
    private String bindAddress = "0.0.0.0";
    private int port = 0;
    private int numHandlers = 1;
    private int numReaders = -1;
    private int queueSizePerHandler = -1;
    private boolean verbose = false;
    private String portRangeConfig = null;
    private String owner=null;
    
    public Builder() {
    }

    /** Mandatory field */
    public Builder setProtocol(Class<?> protocol) {
      this.protocol = protocol;
      return this;
    }
    
    /** Mandatory field */
    public Builder setInstance(Object instance) {
      this.instance = instance;
      return this;
    }
    
    /** Default: 0.0.0.0 */
    public Builder setBindAddress(String bindAddress) {
      this.bindAddress = bindAddress;
      return this;
    }
    
    /** Default: 0 */
    public Builder setPort(int port) {
      this.port = port;
      return this;
    }
    
    /** Default: 1 */
    public Builder setNumHandlers(int numHandlers) {
      this.numHandlers = numHandlers;
      return this;
    }
    
    /** Default: -1 */
    public Builder setnumReaders(int numReaders) {
      this.numReaders = numReaders;
      return this;
    }
    
    /** Default: -1 */
    public Builder setQueueSizePerHandler(int queueSizePerHandler) {
      this.queueSizePerHandler = queueSizePerHandler;
      return this;
    }
    
    /** Default: false */
    public Builder setVerbose(boolean verbose) {
      this.verbose = verbose;
      return this;
    }

    
    /** Default: null */
    public Builder setPortRangeConfig(String portRangeConfig) {
      this.portRangeConfig = portRangeConfig;
      return this;
    }
    
    public Builder setOwner(String ownerID){
    	this.owner=ownerID;
    	return this;
    }
    
    /**
     * Build the RPC Server. 
     * @throws IOException on error
     * @throws HadoopIllegalArgumentException when mandatory fields are not set
     */
    public Server build() throws IOException {
        if(this.protocol==null){
            setProtocolEngine(EmptyProtocolPB.class, ProtobufRpcEngine.class);
            this.protocol=EmptyProtocolPB.class;
        }
      return getProtocolEngine(this.protocol).getServer(
          this.protocol, this.instance, this.bindAddress, this.port,
          this.numHandlers, this.numReaders, this.queueSizePerHandler,
          this.verbose, this.portRangeConfig,this.owner);
    }
  }

    
}
