package org.github.dx88968.directrpc.monitor.logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.github.dx88968.directrpc.engine.ResourceStates;
import org.github.dx88968.directrpc.monitor.Auditor;
import org.github.dx88968.directrpc.monitor.ResourceType;
import org.github.dx88968.directrpc.monitor.Traceable;
import org.github.dx88968.directrpc.monitor.Event.EventType;
import org.github.dx88968.directrpc.monitor.utils.Constants;



public class Pipeline extends Traceable{
	
	PipedInputStream inputStream;
	PipedOutputStream outputStream;
	BufferedReader reader;
	BufferedWriter writer;
	String name;
	long startAt;
	ResourceStates state;
	String accessibleID;
	ResourceType type;
    SessionManager sManager;
	
	public Pipeline(int MAX_BUFFER_SIZE) throws IOException{
		if (MAX_BUFFER_SIZE<=0) {
			inputStream = new PipedInputStream();
		}else {
			inputStream = new PipedInputStream(MAX_BUFFER_SIZE);
		}
		outputStream = new PipedOutputStream(inputStream); 
		reader = new BufferedReader(new InputStreamReader(inputStream));
		writer =new BufferedWriter(new OutputStreamWriter(outputStream));
		this.startAt=System.currentTimeMillis();
        this.state=ResourceStates.active;
        this.type=ResourceType.Pipeline;
        sManager=new SessionManager();
	}
	
	public Session createSession() throws IOException{
		if (state!=ResourceStates.active) {
			throw new IOException("Can not create session when this pipeline is closed");
		}
		return sManager.getSession();
	}
	
	public Session getSession(int key,String timestamp){
		return sManager.getSession(key,Long.parseLong(timestamp));
	}

	private List<String> readlines(int limit) throws IOException{
		List<String> list=new ArrayList<>();
		while (limit>0?list.size()<limit:true) {
			if (!reader.ready()) {
				return list;
			}
			list.add(reader.readLine());
		}
		return list;
	}
	
	public void writeline(String line){
		try {
			IOUtils.write(line+IOUtils.LINE_SEPARATOR, outputStream);
		} catch (IOException e) {
		}
	}
	
	public void setName(String name){
		this.name=name;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		setState(ResourceStates.completed);
		DirectOutputTracker.instance.stopPipeline(accessibleID.split(Constants.IDInterval)[1]);
		try {
			sManager.stop();
			writer.close();
			reader.close();
			inputStream.close();
			outputStream.close();
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public void setState(ResourceStates state) {
		this.state=state;
		EventType type=null;
		String message=null;
		switch(state){
			case active:
				type=EventType.started;
				message="This pipeline is started";
				break;
			case completed:
				type=EventType.completed;
				message="This pipeline is closed";
				try {
					Auditor.getInstance().unregister(this);
				} catch (Exception e) {
					e.printStackTrace();
				}
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
		this.accessibleID=id;
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
		return startAt;
	}

	@Override
	public String getSourceID() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public int countActiveSessions(){
		return sManager.countActiveSessions();
	}
	
	class SessionManager{
		int[] sessionflags;
		ConcurrentHashMap<Integer, Session> sessions;
	    Dispatcher dispatcher;
	    Cleaner cleaner;
		
		public SessionManager(){
			sessionflags=new int[Constants.MAX_NUM_SESSION_EACH_PIPELINE];
			sessions =  new ConcurrentHashMap<>();
			dispatcher=new Dispatcher();
			cleaner=new Cleaner();
			dispatcher.start();
			cleaner.start();
		}
		
		public synchronized Session getSession() throws IOException{
			int key=getFirstAvailiable();
			//System.out.println("Create session "+key);
			Session session=new Session(key);
			sessions.put(key, session);
			sessionflags[key]=1;
			return session;
		}
		
		private synchronized int getFirstAvailiable() throws IOException{
			if (sessionflags==null || sessionflags.length==0) {
				throw new IOException("Sessions are disabled");
			}
			for (int i = 0; i < sessionflags.length; i++) {
				if (sessionflags[i]==0) {
					return i;
				}
			}
			throw new IOException("Session Queue is full");
		}
		
		public int countActiveSessions(){
			return sessions.size();
		}
		
		public Session getSession(int key,Long timestamp){
			//System.out.println("Get session "+key);
			Session session=sessions.get(key);
			if (session!=null && session.timestamp==timestamp) {
				sessionflags[key]=1;
				return session;	
			}else {
				return null;
			}
		}
		
		public void stop(){
			sessions.clear();
			sessionflags=null;
			dispatcher.interrupt();
			cleaner.interrupt();
		}
		
		class Cleaner extends Thread{
			
			public void run(){
				if (sessionflags==null || sessionflags.length==0) {
					return;
				}
				while (!isInterrupted() && state==ResourceStates.active) {
					//System.out.println("------------------------------");
					for (int i = 0; i < sessionflags.length; i++) {
						if (sessionflags[i]!=0) {
							sessionflags[i]++;
							if (sessionflags[i]>Constants.SESSIONTIMEOUT) {
								//System.out.println("remove session"+i);
								sessions.remove(i);
								sessionflags[i]=0;
							}
						}
					}
					try {
						Cleaner.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
			}
		}
		
		class Dispatcher extends Thread{
			
			public void run(){
				if (sessionflags==null || sessionflags.length==0) {
					return;
				}
				while (!isInterrupted() && state==ResourceStates.active) {
					try {
						List<String> tar = readlines(Constants.MAX_THOURGHOUT);
						
						//System.out.println("pipeline "+name+" has read "+meter.measureDeep(tar));
						if (tar.size()>0) {
							Enumeration<Integer> se = sessions.keys();
							while (se.hasMoreElements()) {
								Integer key =  se.nextElement();
								Session session = sessions.get(key);
								if (session!=null) {
									//System.out.println(tar+" : fed in session "+session.getKey()+" in pipeline "+name);
									session.add(tar);
								}
							}
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						Dispatcher.sleep(800);
					} catch (InterruptedException e) {
					}
				}
			}
		}
	}
	
	
}
