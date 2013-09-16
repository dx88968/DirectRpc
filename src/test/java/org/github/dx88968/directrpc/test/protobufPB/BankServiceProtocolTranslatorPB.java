/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.github.dx88968.directrpc.test.protobufPB;


import org.github.dx88968.directrpc.monitor.logger.DirectOutputTracker;
import org.github.dx88968.directrpc.monitor.logger.TraceLevel;
import org.github.dx88968.directrpc.test.IRS.Bank;
import org.github.dx88968.directrpc.test.generatedProtos.BankServiceProtos.depositRequestProto;
import org.github.dx88968.directrpc.test.generatedProtos.BankServiceProtos.inquryRequestProto;
import org.github.dx88968.directrpc.test.generatedProtos.BankServiceProtos.inquryResponseProto;
import org.github.dx88968.directrpc.test.generatedProtos.BankServiceProtos.operationResponseProto;
import org.github.dx88968.directrpc.test.generatedProtos.BankServiceProtos.registerRequestProto;
import org.github.dx88968.directrpc.test.generatedProtos.BankServiceProtos.transferRequestProto;

import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;

/**
 *
 * @author DX
 */
public class BankServiceProtocolTranslatorPB implements BankServiceProtocolPB{

    @Override
    public operationResponseProto register(RpcController controller, registerRequestProto request) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public operationResponseProto deposit(RpcController controller, depositRequestProto request) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public operationResponseProto transfer(RpcController controller, transferRequestProto request) throws ServiceException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public inquryResponseProto inqury(RpcController controller, inquryRequestProto request) throws ServiceException {
        Bank bank=new Bank();
        long check=bank.getCheckAccountAmount(request.getId());
        long deposit=bank.getDepositAccountAmount(request.getId());
        inquryResponseProto response=inquryResponseProto.newBuilder().setCheckAccount(check).setId(request.getId()).setDepositAccount(deposit).build();
        while(true){
            if(request.getId()>100000) {
                break;
            }
            //System.out.println("Still running~~~~");
            DirectOutputTracker.instance.print(null, TraceLevel.DEBUG, "still running~~~");
            try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        DirectOutputTracker.instance.print(null, TraceLevel.DEBUG, "still running~~~");
        return response;
    }
    
}
