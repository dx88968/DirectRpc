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
public class ConnectionFailedException extends IOException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 4805454074004089132L;

	/**
     * Creates a new instance of
     * <code>ConnectionFailedException</code> without detail message.
     */
    public ConnectionFailedException() {
    }

    /**
     * Constructs an instance of
     * <code>ConnectionFailedException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public ConnectionFailedException(String msg) {
        super(msg);
    }
}
