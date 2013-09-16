/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.github.dx88968.directrpc.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import javax.net.SocketFactory;

/**
 *
 * @author DX
 */
interface RpcEngine {
    
    /** Construct a client-side proxy object. 
   * @param <T>*/
  <T> ProtocolProxy<T> getProxy(Class<T> protocol, InetSocketAddress addr,
                  SocketFactory factory, int rpcTimeout) throws IOException;
  
    /** 
   * Construct a server for a protocol implementation instance.
   * 
   * @param protocol the class of protocol to use
   * @param instance the instance of protocol whose methods will be called
   * @param conf the configuration to use
   * @param bindAddress the address to bind on to listen for connection
   * @param port the port to listen for connections on
   * @param numHandlers the number of method handler threads to run
   * @param numReaders the number of reader threads to run
   * @param queueSizePerHandler the size of the queue per hander thread
   * @param verbose whether each call should be logged
   * @param secretManager The secret manager to use to validate incoming requests.
   * @param portRangeConfig A config parameter that can be used to restrict
   *        the range of ports used when port is 0 (an ephemeral port)
   * @return The Server instance
   * @throws IOException on any error
   */
  RPC.RPCServer getServer(Class<?> protocol, Object instance, String bindAddress,
                       int port, int numHandlers, int numReaders,
                       int queueSizePerHandler, boolean verbose,
                       String portRangeConfig,String owner
                       ) throws IOException;
    
}
