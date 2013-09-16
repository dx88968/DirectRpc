package org.github.dx88968.directrpc.monitor.resources;

import java.util.List;



import org.github.dx88968.directrpc.monitor.Auditor;
import org.github.dx88968.directrpc.monitor.logger.Condition;
import org.github.dx88968.directrpc.monitor.logger.DirectOutputTracker;
import org.github.dx88968.directrpc.monitor.logger.Pipeline;
import org.github.dx88968.directrpc.monitor.logger.TraceLevel;
import org.github.dx88968.directrpc.monitor.utils.AddressUtil;
import org.github.dx88968.directrpc.monitor.utils.StackUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Options;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class TraceResource extends ServerResource{
	
	@Options
    public void doOptions(Representation entity) {
        Form responseHeaders = (Form) getResponse().getAttributes().get("org.restlet.http.headers");
        if (responseHeaders == null) {
            responseHeaders = new Form();
            getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);
        }
        responseHeaders.add("Access-Control-Allow-Origin", "*");
        responseHeaders.add("Access-Control-Allow-Methods", "GET,POST,OPTIONS,DELETE");
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
			List<Condition> conditions = DirectOutputTracker.instance.getConditions();
			JSONObject returnMessage=new JSONObject();
			returnMessage.put("title", "Tracer");
			JSONArray pipeArray=new JSONArray();
			for (int i = 0; i < conditions.size(); i++) {
				JSONObject pObject=new JSONObject();
				Condition condition=conditions.get(i);
				Pipeline pipeline=DirectOutputTracker.instance.getPipeline(Integer.toString(condition.hashCode()));
				pObject.put("name", pipeline.getName());
				pObject.put("address", AddressUtil.constructJsonObject(getReference(), pipeline).get("accessibleAddress"));
				pObject.put("condition", condition.toString());
				pObject.put("activeSessions", pipeline.countActiveSessions());
				pipeArray.add(pObject);
			}
			returnMessage.put("pipelines", pipeArray);
			Representation  rep = new StringRepresentation(returnMessage.toJSONString(),MediaType.APPLICATION_JSON);
			return rep;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Post
	public Representation handlePost(Representation entity){
		try{
			 Form responseHeaders = (Form) getResponse().getAttributes().get("org.restlet.http.headers");
			 if (responseHeaders == null) {
	            responseHeaders = new Form();
	            getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);
	        }
	        responseHeaders.add("Access-Control-Allow-Origin", "*");
	        
			JSONObject returnMessage=new JSONObject();
			JSONObject args=AddressUtil.parseArgs(entity.getText());
			String tracemark=(String) args.get("tracemark")==null?null:AddressUtil.unescape((String) args.get("tracemark"));
			String classname = (String) args.get("classname")==null?null:AddressUtil.unescape((String) args.get("classname"));
			String methodname = (String) args.get("methodname")==null?null:AddressUtil.unescape((String) args.get("methodname"));
			String params = (String) args.get("params")==null?null:AddressUtil.unescape((String) args.get("params"));
			String levelStr= (String) args.get("level");
			TraceLevel level = null;
			if (levelStr!=null) {
				level =  TraceLevel.valueOf(levelStr);
			}else {
				level = TraceLevel.ALL;
			}
			 Pipeline pipeline = DirectOutputTracker.instance.getPipeline(tracemark, classname, StackUtils.parseMethod(methodname, params, "%3B"),level);//%3B equals ';'
			 Auditor.getInstance().register(pipeline);
			 String link=(String) AddressUtil.constructJsonObject(getReference(), pipeline).get("accessibleAddress");
			returnMessage.put("link", link);
			Representation  rep = new StringRepresentation(returnMessage.toJSONString(),MediaType.APPLICATION_JSON);
			return rep;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	
	
	

}
