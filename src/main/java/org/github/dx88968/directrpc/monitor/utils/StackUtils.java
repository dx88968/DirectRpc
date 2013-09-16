package org.github.dx88968.directrpc.monitor.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;


public class StackUtils {
	
	public static String parseMethod(StackTraceElement stack) throws SecurityException, ClassNotFoundException{
		String methodName=stack.getMethodName();
		try {
			Method[] methods = Class.forName(stack.getClassName()).getDeclaredMethods();
			StringBuilder params = new StringBuilder(); 
			for (int i = 0; i < methods.length; i++) {
				if (methods[i].getName().equals(methodName)) {
					Type[] types = methods[i].getParameterTypes();
					for (int j = 0; j < types.length; j++) {
						params.append(format(types[j]));
					}
					break;
				}
			}
			if (params.length()==0) {
				return methodName;
			}
			return methodName+"_"+params.toString();
		} catch (ClassNotFoundException e) {
			// TODO: handle exception
			/*
			 * method name is not suppose to be supported in this case, which indicates "print" is invoked in a jsp 
			 * page or something else beyond user's code scope
			 */
			return null;
		}
	}
	
	public static String parseMethod(String methodName,String params,String split) {
		if (methodName==null) {
			return null;
		}
		if (params==null || params.equals("")) {
			return methodName;
		}
		if (split==null || split.equals("")) {
			return methodName+"_"+params;
		}
		return methodName+"_"+params.replace(split, Constants.ParamsInterval);
	}
	
	public static String summerize(String className, String methodName){
		SimpleDateFormat   formatter   =   new   SimpleDateFormat   ("yyyy-MM-dd HH:mm:ss"); 
		String timepart=formatter.format(System.currentTimeMillis());
		String classpart=className;
		String methodpart="Unsupported";
		if (methodName!=null) {
			methodpart=methodName.split("_")[0];
		}
		StringBuffer returnBuffer =new StringBuffer();
		returnBuffer.append(timepart);
		returnBuffer.append(Constants.TIMEInterval);
		returnBuffer.append(classpart);
		returnBuffer.append(Constants.BEWTEEN_CALSS_METHOD);
		returnBuffer.append(methodpart);
		returnBuffer.append(Constants.END_SUMMERY);
		return returnBuffer.toString();
	}
	
	private static String format(Type type){
		Class<?> pType = (Class<?> ) type;
		String origin = pType.getName();
		//System.out.println(origin);
		return origin;
	}


}
