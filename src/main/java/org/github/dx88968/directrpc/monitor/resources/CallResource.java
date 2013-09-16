package org.github.dx88968.directrpc.monitor.resources;

import java.io.IOException;



import org.github.dx88968.directrpc.monitor.Auditor;
import org.github.dx88968.directrpc.monitor.Traceable;
import org.github.dx88968.directrpc.monitor.utils.AddressUtil;
import org.json.simple.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Options;
import org.restlet.resource.ServerResource;


public class CallResource extends ServerResource{
	
	@Options
    public void doOptions(Representation entity) {
        Form responseHeaders = (Form) getResponse().getAttributes().get("org.restlet.http.headers");
        if (responseHeaders == null) {
            responseHeaders = new Form();
            getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);
        }
        responseHeaders.add("Access-Control-Allow-Origin", "*");
        responseHeaders.add("Access-Control-Allow-Methods", "GET,POST,DELETE,OPTIONS");
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
	        String id=extractIdFromURL(getReference());
	        Traceable call=Auditor.getInstance().getResource(id);
	        if (call==null) {
				JSONObject log = Auditor.getInstance().getLog(id);
				if(log==null){
					return new StringRepresentation("{message:Invaild id}");
				}
				JSONObject returnMessage=new JSONObject();
				returnMessage.put("title", "Log for "+id);
				returnMessage.put("log", log);
				return new StringRepresentation(returnMessage.toJSONString(),MediaType.APPLICATION_JSON);
			}
			JSONObject returnMessage=new JSONObject();
			returnMessage.put("title", call.getName());
			returnMessage.put("info", AddressUtil.constructJsonObject(getReference(), call));
			Representation  rep = new StringRepresentation(returnMessage.toJSONString(),MediaType.APPLICATION_JSON);
			return rep;
		} catch (Exception e) {
			e.printStackTrace();
			return new StringRepresentation("{message:error}");
		}
	}
	
	@SuppressWarnings("unchecked")
	@Delete
	public Representation handleDelete(){
		try{
			 Form responseHeaders = (Form) getResponse().getAttributes().get("org.restlet.http.headers");
			 if (responseHeaders == null) {
	            responseHeaders = new Form();
	            getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);
	        }
	        responseHeaders.add("Access-Control-Allow-Origin", "*");
	        String id=extractIdFromURL(getReference());
	        Traceable call=Auditor.getInstance().getResource(id);
	        call.stop();
			JSONObject returnMessage=new JSONObject();
			returnMessage.put("title", call.getName());
			returnMessage.put("info", AddressUtil.constructJsonObject(getReference(), call));
			Representation  rep = new StringRepresentation(returnMessage.toJSONString(),MediaType.APPLICATION_JSON);
			return rep;
		} catch (Exception e) {
			e.printStackTrace();
			return new StringRepresentation("{message:error}");
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
