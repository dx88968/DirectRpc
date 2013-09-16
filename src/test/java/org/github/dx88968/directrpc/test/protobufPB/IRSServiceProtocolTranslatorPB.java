/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.github.dx88968.directrpc.test.protobufPB;

import java.awt.image.DataBuffer;

import org.github.dx88968.directrpc.monitor.logger.DirectOutputTracker;
import org.github.dx88968.directrpc.monitor.logger.TraceLevel;
import org.github.dx88968.directrpc.test.IRS.IRS;
import org.github.dx88968.directrpc.test.generatedProtos.IRSServiceProtos.checkTaxRequestProto;
import org.github.dx88968.directrpc.test.generatedProtos.IRSServiceProtos.checkTaxResponseProto;

import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;

/**
 *
 * @author DX
 */
public class IRSServiceProtocolTranslatorPB implements IRSServiceProtocolPB{
    
    IRS irs;
    
    public IRSServiceProtocolTranslatorPB(IRS irs){
        this.irs=irs;
    }

    @Override
    public checkTaxResponseProto checkTax(RpcController controller, checkTaxRequestProto request) throws ServiceException {
    	DataBuffer dBuffer=null;
    	dBuffer.getClass();
    	DirectOutputTracker.instance.print("dx1234", TraceLevel.DEBUG, Long.toString(request.getID()));
        checkTaxResponseProto response=checkTaxResponseProto.newBuilder().setResultCode(0).setTax(irs.getTax(request.getID())).build();
        return response;
    }
    
}
