package org.github.dx88968.directrpc.monitor.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;



public class Session {

	List<String> cache;
	long timestamp;
	int key;
	
	public Session(int key){
		cache=new CopyOnWriteArrayList<>();
		this.key=key;
		timestamp=System.currentTimeMillis();
	}
	
	public void add(List<String> list){
		if (cache==null) {
			return;
		}
		cache.addAll(list);
	}
	
	public int getKey() {
		return key;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public List<String> getBuffer(){
		if (cache==null) {
			return null;
		}
		List<String> result=new ArrayList<>(cache);
		cache.clear();
		return result;
	}
	
	public void stop(){
		cache.clear();
		cache=null;
	}
	
}
