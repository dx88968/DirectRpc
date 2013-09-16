/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.github.dx88968.directrpc.engine;

import java.io.IOException;
import java.net.InetSocketAddress;
import javax.net.SocketFactory;

import org.github.dx88968.directrpc.core.EmptyProtocolPB;
import org.github.dx88968.directrpc.core.ProtobufRpcEngine;
import org.github.dx88968.directrpc.core.RPC;

/**
 *
 * @author DX
 */
public class ClientObjectContainer {
    
    public ClientObjectContainer(){
        RPC.setProtocolEngine(EmptyProtocolPB.class, ProtobufRpcEngine.class);
    }
    
    public <T> T getProtocolProxy (Class<T> protocol,
                                InetSocketAddress addr, int rpcTimeout) throws IOException {   
        return RPC.getProtocolProxy(protocol, addr, SocketFactory.getDefault(), rpcTimeout).getProxy();
    }
    
}
