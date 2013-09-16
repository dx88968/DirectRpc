package org.github.dx88968.directrpc.monitor;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.github.dx88968.directrpc.monitor.Auditor.EventLogger;




public class Event{
	EventType type;
	Date date;
	String id;
	String message;
	EventLogger logger;

	
	
	public Event setLogger(EventLogger logger) {
		this.logger = logger;
		return this;
	}



	public Event setType(EventType type) {
		this.type = type;
		return this;
	}



	public Event setDate(Date date) {
		this.date = date;
		return this;
	}



	public Event setId(String id) {
		this.id = id;
		return this;
	}



	public Event setMessage(String message) {
		this.message = message;
		return this;
	}

	public String toString(){
		SimpleDateFormat   formatter   =   new   SimpleDateFormat   ("yyyy-MM-dd   HH:mm:ss     "); 
		return formatter.format(date)+" "+id+"  "+type.toString()+"  "+message;
	}
	
	public void emit(){
		logger.addEvent(id, this);
	}

	public enum EventType{started,terminated,completed};
}

