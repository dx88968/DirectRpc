package org.github.dx88968.directrpc.monitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dapacode.tree4j.MultimapTree;
import org.dapacode.tree4j.MutableTree;
import org.github.dx88968.directrpc.monitor.Event.EventType;
import org.github.dx88968.directrpc.monitor.logger.DirectOutputTracker;
import org.github.dx88968.directrpc.monitor.resources.CallResource;
import org.github.dx88968.directrpc.monitor.resources.PipelineResource;
import org.github.dx88968.directrpc.monitor.resources.ServerObjectContainerResource;
import org.github.dx88968.directrpc.monitor.resources.TraceResource;
import org.github.dx88968.directrpc.monitor.utils.Constants;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.restlet.Component;
import org.restlet.data.Protocol;
import org.restlet.routing.Route;
import org.restlet.util.RouteList;



@SuppressWarnings("deprecation")
public class Auditor {

	static Auditor instance;
	ConcurrentHashMap<String, Traceable> resources;
	Component component;
	EventLogger logger;
	EventFactory eventFactory;
	ResourceRelationship relationship;
	
	private Auditor(int port) throws Exception{
		resources=new ConcurrentHashMap<>();
		component = new Component();
		logger=new EventLogger();
		eventFactory=new EventFactory(logger);
		relationship=new ResourceRelationship();
		Queue<ResourceType> pQueue=new LinkedBlockingQueue<ResourceType>();
		pQueue.add(ResourceType.Top);
		pQueue.add(ResourceType.ServerObjectContainer);
		pQueue.add(ResourceType.Call);
		relationship.registerRule(pQueue);
		DirectOutputTracker.init();
		// Disable annoying console logging of requests..
	    Logger logger = Logger.getLogger("org.restlet");
	    for (Handler handler : logger.getParent().getHandlers()) {
	        // Find the console handler
	        if (handler.getClass().equals(java.util.logging.ConsoleHandler.class)) {
	            // set level to SEVERE. We could disable it completely with 
	            // a custom filter but this is good enough.
	            handler.setLevel(Level.SEVERE);
	        }
	    }
        component.getServers().add(Protocol.HTTP, port);
        component.start();
	}
	
	public static Auditor getInstance(int port,Class<?> rootService) throws Exception{
		if(instance==null){
			instance=new Auditor(port);
			instance.startService("/", rootService);
			instance.startService("/log", CallResource.class);
			instance.startService("/Trace", TraceResource.class);
		}
		return instance;
	}
	
	public static Auditor getInstance() throws Exception{
		if(instance==null){
			throw new Exception("No active instance by now!");
		}
		return instance;
	}
	
	public Traceable getResource(String id){
		return resources.get(id);
	}
	
	public void register(Traceable resourse){
		String id=getAvailibleID(resourse.getName(), resourse.getType());
		resourse.setAccessibleID(id);
		resources.put(id, resourse);
		switch(resourse.getType()){
			case ServerObjectContainer:
				instance.registerResource(null, resourse);
				instance.startService(resourse.getAccessibleID(), ServerObjectContainerResource.class);
				break;
			case Call:
				instance.registerResource(resourse.getSourceID(), resourse);
				instance.startService(resourse.getAccessibleID(), CallResource.class);
				break;
			case Pipeline:
				instance.startService(resourse.getAccessibleID(), PipelineResource.class);
				break;
		default:
			System.out.println("Unsoppurted resource type");
			break;
		}
	}
	
	public void registerResource(String parentId,Traceable resource) {
		relationship.registerResource(parentId, resource);
	}
	
	public void unregister(Traceable resourse){
		switch (resourse.getType()) {
		case ServerObjectContainer:
			if (!resources.containsKey(resourse.getAccessibleID())) {
				return;
			}
			Collection<String> callIDs = relationship.getChildren(resourse.getAccessibleID());
	        Iterator<String> i = callIDs.iterator();
	        while (i.hasNext()) {
				unregister(resources.get(i));
			}
	        resources.remove(resourse.getAccessibleID());
			break;
		case Call:
			if(!resources.containsKey(resourse.getAccessibleID())){
				return;
			}
			resources.remove(resourse.getAccessibleID());
			break;
		case Pipeline:
			if(!resources.containsKey(resourse.getAccessibleID())){
				return;
			}
			resources.remove(resourse.getAccessibleID());
			stopService(resourse.getAccessibleID());
			break;
		default:
			break;
		}	
	}
	
	public ArrayList<Traceable> filter(ResourceType type){
		ArrayList<Traceable> results=new ArrayList<>();
		Enumeration<String> keys = resources.keys();
		while(keys.hasMoreElements()){
			String id=keys.nextElement();
			if (extractTypeFromID(id).equals(type)) {
				results.add(resources.get(id));
			}
		}
		return results;
	}
	
	private ResourceType extractTypeFromID(String id){
		try{
			String type_str=id.split(Constants.IDInterval)[0];
			ResourceType type=ResourceType.valueOf(type_str.substring(1));
			return type;
		}catch(Exception ex){
			System.out.println("Unknown type or illegel id!");
			return  ResourceType.Unknown;
		}
	}
	
	private String getAvailibleID(String name,ResourceType type){
		String expectedName =Constants.IDHEAD+ type.toString()+Constants.IDInterval+name+Constants.IDInterval+System.currentTimeMillis();
		if(resources.contains(expectedName)){
			expectedName=getAvailibleID(name, type);
		}
		return expectedName;
	}
	
	public JSONObject getInfo(String id){
		if(resources.contains(id)){
			return resources.get(id).getInfo();
		}
		return null;
	}
	
	public void stop(String id){
		if(resources.contains(id)){
			resources.get(id).stop();
		}
	}
	
	
	public void startService(String address,Class<?> targetClass){
		component.getDefaultHost().attach(address, targetClass);
	}
	
	public void stopService(String address){
		RouteList a = component.getDefaultHost().getRoutes();
        for(Route r:a){
        	if(r.getTemplate().getPattern().equals(address)){
        		a.remove(r);
        	}
        }
	}
	
	public JSONObject getLog(String id){
		return logger.getLog(id);
	}
	
	public EventFactory getEventFactory(){
		return eventFactory;
	}
	
	class EventLogger{
		public ConcurrentHashMap<String, ConcurrentLinkedQueue<Event>> log;
		
		public EventLogger(){
			log=new ConcurrentHashMap<String, ConcurrentLinkedQueue<Event>>();
		}
		
		public void addEvent(String id, Event e){
			if(log.containsKey(id)){
				log.get(id).add(e);
			}else{
				ConcurrentLinkedQueue<Event> lEvents=new ConcurrentLinkedQueue<>();
				lEvents.add(e);
				log.put(id, lEvents);
			}
		}
		
		@SuppressWarnings("unchecked")
		public JSONObject getLog(String id){
			JSONObject info=new JSONObject();
			info.put("type", "log");
			if(!log.containsKey(id)){
				info.put("error", "No log is availible for "+id);
				return info;
			}
			JSONArray jArray=new JSONArray();
			ConcurrentLinkedQueue<Event> lEvents=log.get(id);
			Event event;
			while((event=lEvents.poll())!=null){
				jArray.add(event.toString());
			}
			info.put("content", jArray);
			return info;
		}
		
		public void clearLog(String id){
			log.remove(id);
		}
		
		
	}
	
	public class EventFactory{
		
		EventLogger logger;
		
		public EventFactory(EventLogger logger) {
			this.logger=logger;
		}
		
		public Event createEvent(String id,EventType type,String message){
			Date   curDate   =   new   Date(System.currentTimeMillis());
			return new Event().setId(id).setType(type).setMessage(message).setDate(curDate).setLogger(logger);
		}
	}
	
	class ResourceRelationship{
		MutableTree<String> relations;
		MutableTree<ResourceType> rule;
		static final String ROOT="R";
		
		private ResourceRelationship(){
			relations= MultimapTree.<String>create();
			relations.setRoot(ROOT);
		}
		
		public void registerRule(Queue<ResourceType> pQueue){
			rule=MultimapTree.<ResourceType>create();
			if (pQueue.isEmpty()) {
				return;
			}
			rule.setRoot(pQueue.peek());
			while (pQueue.size()>1) {
				rule.add(pQueue.poll(), pQueue.peek());
			}
		}
		
		public void registerResource(String parentId,Traceable resource) {
			if (verify(parentId, resource.getType())) {
				if(parentId==null){
					//System.out.println("under root: "+resource.getAccessibleID());
					relations.add(ROOT, resource.getAccessibleID());
				}else {
					//System.out.println("under "+parentId+": "+resource.getAccessibleID());
					relations.add(parentId, resource.getAccessibleID());
				}
			}
		}
		
		public Collection<String> getChildren(String id){
			return relations.getChildren(id);
		}
		
		public void remove(String id){
			relations.remove(id);
		}
		
		public void clear(){
			relations.clear();
		}
		
		private boolean verify(String parentID,ResourceType type){
			if (parentID==null) {
				return rule.getParent(type).equals(rule.getRoot());
			}
			return getRelationLayerNum(parentID, relations)+1==getRelationLayerNum(type, rule);
		}
		
		
		private <T> int getRelationLayerNum(T node,MutableTree<T> tree){
			int layer=0;
			T cursor=node;
			while (cursor!=tree.getRoot()) {
				cursor=tree.getParent(cursor);
				layer++;
			}
			return layer;
		}
		
	}
	
}
