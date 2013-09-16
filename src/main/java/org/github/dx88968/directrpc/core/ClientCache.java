/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.github.dx88968.directrpc.core;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import javax.net.SocketFactory;

import org.github.dx88968.directrpc.io.Writable;

/**
 *
 * @author DX
 */
public class ClientCache {
  private Map<InetSocketAddress, Client> clients =
    new HashMap();

  /**
   * Construct & cache an IPC client with the user-provided SocketFactory 
   * if no cached client exists.
   * 
   * @param conf Configuration
   * @param factory SocketFactory for client socket
   * @param valueClass Class of the expected response
   * @return an IPC client
   */
  public synchronized Client getClient(InetSocketAddress ip) {
    // Construct & cache client.  The configuration is only used for timeout,
    // and Clients have connection pools.  So we can either (a) lose some
    // connection pooling and leak sockets, or (b) use the same timeout for all
    // configurations.  Since the IPC is usually intended globally, not
    // per-job, we choose (a).
    Client client = clients.get(ip);
    if (client == null) {
      client = new Client();
      clients.put(ip, client);
    } 
    return client;
  }


  /**
   * Stop a RPC client connection 
   * A RPC client is closed only when its reference count becomes zero.
   */
  public void stopClient(Client client) {
    client.stop();
  }
}

