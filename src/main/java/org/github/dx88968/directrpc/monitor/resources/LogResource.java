package org.github.dx88968.directrpc.monitor.resources;

import org.github.dx88968.directrpc.monitor.Auditor;
import org.json.simple.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Options;
import org.restlet.resource.ServerResource;

public class LogResource extends ServerResource {
	
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
	        
			JSONObject returnMessage=new JSONObject();
			Form queryParams = getRequest().getResourceRef().getQueryAsForm();
			String id = queryParams.getFirstValue("id");
			returnMessage.put("title","Log "+id);
			returnMessage.put("content", Auditor.getInstance().getLog(id));
			Representation  rep = new StringRepresentation(returnMessage.toJSONString(),MediaType.APPLICATION_JSON);
			return rep;
		} catch (Exception e) {
			JSONObject returnMessage=new JSONObject();
			returnMessage.put("error",e.getMessage());
			e.printStackTrace();
			return new StringRepresentation(returnMessage.toJSONString(),MediaType.APPLICATION_JSON);
		}
	}

	
	@Delete
	public String handleDelete(){
		return "success";
	}


}
