package org.github.dx88968.directrpc.monitor.utils;

import java.text.SimpleDateFormat;


import org.github.dx88968.directrpc.monitor.Traceable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.restlet.data.Reference;


public class AddressUtil {
	
	public static JSONObject constructJsonObject(Reference r,Traceable resource) throws ParseException{
		String id=resource.getAccessibleID();
		String source=resource.getSourceID();
		resource.add("name", resource.getName());
		resource.add("type", resource.getType().toString());
		SimpleDateFormat   formatter   =   new   SimpleDateFormat   ("yyyy-MM-dd   HH:mm:ss     "); 
		resource.add("startAt", formatter.format(resource.getStartAt()));
		resource.add("state", resource.getState().toString());
		resource.add("accessibleAddress", r.getHostDomain()+":"+r.getHostPort()+id);
		if (source!=null) {
			resource.add("returnAddr", r.getHostDomain()+":"+r.getHostPort()+source);
		}else{
			resource.add("returnAddr", r.getHostDomain()+":"+r.getHostPort()+"/");
		}
		return resource.getInfo();
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject parseArgs(String arg) {
		JSONObject returnMessage=new JSONObject();
		String[] group1 = arg.split("&");
		for(String s:group1){
			String[] group2=s.split("=");
			if (group2.length<2) {
				returnMessage.put(group2[0], null);
			}else{
				returnMessage.put(group2[0], group2[1]);
			}
		}
		return returnMessage;
	}
	
	 public static String  unescape (String src)
	 {
	  StringBuffer tmp = new StringBuffer();
	  tmp.ensureCapacity(src.length());
	  int  lastPos=0,pos=0;
	  char ch;
	  while (lastPos<src.length())
	  {
	   pos = src.indexOf("%",lastPos);
	   if (pos == lastPos)
	    {
	    if (src.charAt(pos+1)=='u')
	     {
	     ch = (char)Integer.parseInt(src.substring(pos+2,pos+6),16);
	     tmp.append(ch);
	     lastPos = pos+6;
	     }
	    else
	     {
	     ch = (char)Integer.parseInt(src.substring(pos+1,pos+3),16);
	     tmp.append(ch);
	     lastPos = pos+3;
	     }
	    }
	   else
	    {
	    if (pos == -1)
	     {
	     tmp.append(src.substring(lastPos));
	     lastPos=src.length();
	     }
	    else
	     {
	     tmp.append(src.substring(lastPos,pos));
	     lastPos=pos;
	     }
	    }
	  }
	  return tmp.toString();
	 }

}

