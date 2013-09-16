package org.github.dx88968.directrpc.monitor.resources;

import java.io.IOException;
import java.util.ArrayList;



import org.github.dx88968.directrpc.monitor.Auditor;
import org.github.dx88968.directrpc.monitor.ResourceType;
import org.github.dx88968.directrpc.monitor.Traceable;
import org.github.dx88968.directrpc.monitor.utils.AddressUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Options;
import org.restlet.resource.ServerResource;


public class ServerObjectContainerResource extends ServerResource{


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
			System.out.println(getRootRef());
			 Form responseHeaders = (Form) getResponse().getAttributes().get("org.restlet.http.headers");
			 if (responseHeaders == null) {
	            responseHeaders = new Form();
	            getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);
	        }
	        responseHeaders.add("Access-Control-Allow-Origin", "*");
	        JSONObject returnMessage=new JSONObject();
	        String id=extractIdFromURL(getReference());
	        returnMessage.put("title", "Calls in "+id);
	        Traceable container=Auditor.getInstance().getResource(id);
	        if (container==null) {
	        	returnMessage.put("log", "/log?id="+id);
	        	return new StringRepresentation(returnMessage.toJSONString(),MediaType.APPLICATION_JSON);
			}
			ArrayList<Traceable> resources=Auditor.getInstance().filter(ResourceType.Call);
			JSONArray calls=new JSONArray();
			for(Traceable r:resources){
				if (id.equals(r.getSourceID())) {
					calls.add(AddressUtil.constructJsonObject(getReference(),r));
				}
			}
			returnMessage.put("calls", calls);
			Representation  rep = new StringRepresentation(returnMessage.toJSONString(),MediaType.APPLICATION_JSON);
			return rep;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private String extractIdFromURL(Reference ref) throws IOException{
		String host=ref.getHostDomain()+":"+ref.getHostPort();
		try{
			return ref.toString().split(host)[1].split("\\?")[0];
		}catch(Exception ex){
			throw new IOException("Illegal url for extracting id");
		}
	}

}
