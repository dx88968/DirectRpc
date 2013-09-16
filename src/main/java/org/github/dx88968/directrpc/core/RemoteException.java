/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.github.dx88968.directrpc.core;

import java.io.IOException;

/**
 *
 * @author DX
 */
class RemoteException extends IOException{
    
    /**
	 * 
	 */
	private static final long serialVersionUID = -5400446967692103279L;

	public RemoteException(String message){
        super(message);
    }
    
    public RemoteException(String message,int code){
        super(message+"   code:"+code);
    }
    
}
