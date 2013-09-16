/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.github.dx88968.directrpc.test.hadoopipcmini;

import com.google.protobuf.ServiceException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.SocketFactory;

import org.github.dx88968.directrpc.engine.ClientObjectContainer;
import org.github.dx88968.directrpc.test.generatedProtos.BankServiceProtos;
import org.github.dx88968.directrpc.test.generatedProtos.IRSServiceProtos;
import org.github.dx88968.directrpc.test.protobufPB.BankServiceProtocolPB;
import org.github.dx88968.directrpc.test.protobufPB.IRSServiceProtocolPB;


/**
 *
 * @author DX
 */
public class TestClient {
    
    public static void main(String[] args){
        while(true){
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        InetSocketAddress remoteId=new InetSocketAddress("0.0.0.0", 9001);
                        ClientObjectContainer coc=new ClientObjectContainer();
                        
                        IRSServiceProtocolPB IRSproxy=coc.getProtocolProxy(IRSServiceProtocolPB.class, remoteId,0);
                        IRSServiceProtos.checkTaxRequestProto params=IRSServiceProtos.checkTaxRequestProto.newBuilder().setID(123).build();
                        try {
                            IRSServiceProtos.checkTaxResponseProto respond=IRSproxy.checkTax(null, params);
                            System.out.println(respond.getTax());
                        } catch (ServiceException ex) { ex.printStackTrace();}
                        
                        BankServiceProtocolPB BANKproxy=coc.getProtocolProxy(BankServiceProtocolPB.class, remoteId,0);
                        BankServiceProtos.inquryRequestProto request=BankServiceProtos.inquryRequestProto.newBuilder().setId(123).build();
                        try{
                            BankServiceProtos.inquryResponseProto response=BANKproxy.inqury(null, request);
                            System.out.println(response.getCheckAccount());
                        }catch (ServiceException ex) {ex.printStackTrace();}
                    } catch (IOException ex) {} 
                }
            }).start();
            break;
        }
    }
    
}
