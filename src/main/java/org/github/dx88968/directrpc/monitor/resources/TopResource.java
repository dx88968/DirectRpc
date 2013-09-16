package org.github.dx88968.directrpc.monitor.resources;

import java.util.ArrayList;


import org.github.dx88968.directrpc.monitor.Auditor;
import org.github.dx88968.directrpc.monitor.ResourceType;
import org.github.dx88968.directrpc.monitor.Traceable;
import org.github.dx88968.directrpc.monitor.utils.AddressUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Options;
import org.restlet.resource.ServerResource;




public class TopResource extends ServerResource{


	@Options
    public void doOptions(Representation entity) {
        Form responseHeaders = (Form) getResponse().getAttributes().get("org.restlet.http.headers");
        if (responseHeaders == null) {
            responseHeaders = new Form();
            getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);
        }
        responseHeaders.add("Access-Control-Allow-Origin", "*");
        responseHeaders.add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        responseHeaders.add("Access-Control-Allow-Headers", "Content-Type");
        responseHeaders.add("Access-Control-Allow-Credentials", "false");
        responseHeaders.add("Access-Control-Max-Age", "60");
    }
	
	@SuppressWarnings("unchecked")
	@Get
	public Representation handleGet(){
		try{
			 Form responseHeaders = (Form) getResponse().getAttributes().get("org.restlet.http.headers");
			 if (responseHeaders == null) {
	            responseHeaders = new Form();
	            getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);
	        }
	        responseHeaders.add("Access-Control-Allow-Origin", "*");
			ArrayList<Traceable> resources=Auditor.getInstance().filter(ResourceType.ServerObjectContainer);
			JSONObject returnMessage=new JSONObject();
			if (getReference().getRemainingPart().length()>0) {
				return getError();
			}
			returnMessage.put("title", "List of Containers @"+getReference().toString());
			JSONArray containers=new JSONArray();
			for(Traceable r:resources){
				containers.add(AddressUtil.constructJsonObject(getReference(),r));
			}
			returnMessage.put("containers", containers);
			Representation  rep = new StringRepresentation(returnMessage.toJSONString(),MediaType.APPLICATION_JSON);
			return rep;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	
	@Delete
	public Representation handleDelete(){
		if (getReference().getRemainingPart().length()>0) {
			return getError();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private Representation getError(){
		JSONObject returnMessage=new JSONObject();
		returnMessage.put("error", "This resource has been closed");
		Representation  rep = new StringRepresentation(returnMessage.toJSONString(),MediaType.APPLICATION_JSON);
		return rep;
	}

}
