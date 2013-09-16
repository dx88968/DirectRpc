package org.github.dx88968.directrpc.engine;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.github.dx88968.directrpc.monitor.Auditor;
import org.github.dx88968.directrpc.monitor.resources.TopResource;





public class ContainerFactory {
	
	public static ClientObjectContainer createClientsideContainer(){
        return new ClientObjectContainer();
	}
	
	public static ServerObjectContainer createServersideContainer(String ip,int port,int numHandlers,int numReaders) throws IOException{
		InetSocketAddress addr=new InetSocketAddress(ip,port);
		ServerObjectContainer container= new ServerObjectContainer(addr,numHandlers , numReaders);
		try {
			Auditor auditor = Auditor.getInstance();
			auditor.register(container);
		} catch (Exception e) {
		}
		return container;
	}
	
	public static void enableAudit(int port) throws Exception{
		Auditor.getInstance(port,TopResource.class);
	}

}
