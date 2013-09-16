/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.github.dx88968.directrpc.test.hadoopipcmini;

import com.google.protobuf.BlockingService;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.github.dx88968.directrpc.engine.ContainerFactory;
import org.github.dx88968.directrpc.engine.ServerObjectContainer;
import org.github.dx88968.directrpc.test.generatedProtos.BankServiceProtos.BankService;
import org.github.dx88968.directrpc.test.generatedProtos.IRSServiceProtos.IRSService;
import org.github.dx88968.directrpc.test.protobufPB.BankServiceProtocolPB;
import org.github.dx88968.directrpc.test.protobufPB.BankServiceProtocolTranslatorPB;
import org.github.dx88968.directrpc.test.protobufPB.IRSServiceProtocolPB;
import org.github.dx88968.directrpc.test.protobufPB.IRSServiceProtocolTranslatorPB;


/**
 *
 * @author DX
 */
public class HadoopIPCMini {

    /**
     * @param args the command line arguments
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        try {
            // TODO code application logic here
            
            IRSServiceProtocolTranslatorPB IRSServiceProtocolTranslator=new IRSServiceProtocolTranslatorPB(org.github.dx88968.directrpc.test.IRS.IRS.getInstance());
            BlockingService irsService=IRSService.newReflectiveBlockingService(IRSServiceProtocolTranslator);
            
            BankServiceProtocolTranslatorPB bankservicetranslatorpb=new BankServiceProtocolTranslatorPB();
            BlockingService bankservice=BankService.newReflectiveBlockingService(bankservicetranslatorpb);
            ContainerFactory.enableAudit(9002);
            ServerObjectContainer soc=ContainerFactory.createServersideContainer("0.0.0.0", 9001, 3, 3);
            soc.registerProtocolAndImpl(IRSServiceProtocolPB.class, irsService);
            soc.registerProtocolAndImpl(BankServiceProtocolPB.class, bankservice);
            soc.enable();
        } catch (IOException ex) {
            Logger.getLogger(HadoopIPCMini.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
