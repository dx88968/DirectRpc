/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.github.dx88968.directrpc.engine;


import java.io.IOException;
import java.net.InetSocketAddress;

import org.github.dx88968.directrpc.core.RPC;
import org.github.dx88968.directrpc.core.RPC.RPCServer;
import org.github.dx88968.directrpc.monitor.Auditor;
import org.github.dx88968.directrpc.monitor.ResourceType;
import org.github.dx88968.directrpc.monitor.Traceable;
import org.github.dx88968.directrpc.monitor.Event.EventType;



/**
 *
 * @author DX
 */
public class ServerObjectContainer extends Traceable{
    private InetSocketAddress ip;
    private int numHandlers = 1;
    private int numReaders = 1;
    private RPCServer server;
    private String name;
    private ResourceType type;
    private String accessibleID;
    private long StartAt;
    private ResourceStates state;
	

    public ServerObjectContainer(InetSocketAddress ip) throws IOException{
        this(ip,100,100);
    }
    
    public ServerObjectContainer(InetSocketAddress ip,int numhandlers,int numreaders) throws IOException{
    	super();
        this.ip=ip;
        this.numHandlers=numhandlers;
        this.numReaders=numreaders;
        this.name=ip.toString();
        this.type=ResourceType.ServerObjectContainer;
        this.StartAt=System.currentTimeMillis();
        server=(RPCServer) new RPC.Builder().setBindAddress(ip.getAddress().getHostAddress()).setPort(this.ip.getPort())
                    .setnumReaders(numReaders).setNumHandlers(numHandlers).build();
    }
    
    public void registerProtocolAndImpl(Class<?> protocolClass, 
       Object protocolImpl) throws IOException{
        server.registerProtocolImpl(protocolClass.getName(), protocolImpl);
    }
    
    public void enable() throws IOException{
    	System.out.println("ServerObjectContainer is ready on "+ip.toString());
    	setState(ResourceStates.active);
        server.startListen();
    }
    
    public void disable(){
    	setState(ResourceStates.terminated);
        server.stop();
    }

	@Override
	public void stop() {
		server.stop();
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public void setState(ResourceStates state) {
		// TODO Auto-generated method stub
		this.state=state;
		EventType type=null;
		String message=null;
		switch(state){
			case active:
				type=EventType.started;
				message="This container is started";
				break;
			case inactive:
				type=EventType.terminated;
				message="This container has been closed";
				break;
			case terminated:
				type=EventType.terminated;
				message="This container has been closed";
				try {
					Auditor.getInstance().unregister(this);
				} catch (Exception e) {}
				break;
			case completed:
				type=EventType.completed;
				message="Illegel state";
				break;
		default:
			break;	
		}
		try {
			Auditor.getInstance().getEventFactory().createEvent(accessibleID, type, message).emit();
		} catch (Exception e) {}
	}

	@Override
	public ResourceType getType() {
		// TODO Auto-generated method stub
		return type;
	}

	@Override
	public void setAccessibleID(String id) {
		// TODO Auto-generated method stub
		accessibleID=id;
		server.setOwner(accessibleID);
	}

	@Override
	public String getAccessibleID() {
		// TODO Auto-generated method stub
		return accessibleID;
	}

	@Override
	public ResourceStates getState() {
		// TODO Auto-generated method stub
		return state;
	}

	@Override
	public long getStartAt() {
		// TODO Auto-generated method stub
		return StartAt;
	}

	@Override
	public String getSourceID() {
		// null indicates this resource below to nothing.
		return null;
	}


}
