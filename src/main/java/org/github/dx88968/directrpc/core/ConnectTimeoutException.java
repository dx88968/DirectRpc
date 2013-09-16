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
class ConnectTimeoutException extends IOException {


	/**
	 * 
	 */
	private static final long serialVersionUID = -6056820857885023953L;

	public ConnectTimeoutException(String message) {
        super(message);
    }
    
}
