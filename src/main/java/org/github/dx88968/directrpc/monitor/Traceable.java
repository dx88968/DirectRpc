package org.github.dx88968.directrpc.monitor;


import org.github.dx88968.directrpc.engine.ResourceStates;
import org.json.simple.JSONObject;



public abstract class Traceable {

	JSONObject info;
	
	public Traceable() {
		info=new JSONObject();
	}
	
	@SuppressWarnings("unchecked")
	public void add(String key, String value){
		info.put(key, value);
	}
	
	public void remove(String key){
		info.remove(key);
	}
	
	public void clear(){
		info.clear();
	}
	
	public abstract void stop();
	public abstract String getName();
	public abstract void setState(ResourceStates state);
	public abstract ResourceType getType();
	public abstract void setAccessibleID(String id);
	public abstract String getAccessibleID();
	public abstract ResourceStates getState();
	public abstract long getStartAt();
	public abstract String getSourceID();
	
	public JSONObject getInfo(){
		return info;
	}
	
}
