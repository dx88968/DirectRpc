package org.github.dx88968.directrpc.test.hadoopipcmini;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.github.dx88968.directrpc.monitor.utils.AddressUtil;
import org.github.dx88968.directrpc.monitor.utils.StackUtils;


/*   
 * 1.获取当前运行代码的类名，方法名，行号，主要是通过java.lang.StackTraceElement类
 * 
 * 2. 
 *   [1]获得调用者的方法名, 同new Throwable
 *         String _methodName = new Exception().getStackTrace()[1].getMethodName();
 *   [0]获得当前的方法名, 同new Throwable
 *         String _thisMethodName = new Exception().getStackTrace()[0].getMethodName();
 * */
public class StackTraceElementTest {
	
	public static void main(String[] args) throws SecurityException, ClassNotFoundException {
		System.out.println("line1: " + new Throwable().getStackTrace()[0].getLineNumber());
		System.out.println("line2: " + getLineInfo());
		System.out.println("line3: " + getTraceInfo());
		
		Pattern pattern = Pattern.compile("(.+?)asaj(.+?)");
		  Matcher matcher = pattern.matcher("hi.asajdfghj.Lucy");
		  boolean b= matcher.matches();
		  //当条件满足时，将返回true，否则返回false
		  System.out.println(b);
		StackTraceElement ste1 = null;
		StackTraceElement[] steArray = Thread.currentThread().getStackTrace();
		int steArrayLength = steArray.length;
		String s = null;
		// output all related info of the existing stack traces
		if(steArrayLength==0) {
			System.err.println("No Stack Trace.");
		} else {
			System.out.println(StackUtils.parseMethod(steArray[1]));
		}
	}

	public static String getTraceInfo(){  
                StringBuffer sb = new StringBuffer();   
          
                StackTraceElement[] stacks = new Throwable().getStackTrace();  
                int stacksLen = stacks.length;  
                sb.append("class: " ).append(stacks[1].getClassName())
                .append("; method: ").append(stacks[1].getMethodName())
                .append("; number: ").append(stacks[1].getLineNumber());  
          
                return sb.toString();  
         }  
	
	public static String getLineInfo() {
                StackTraceElement ste = new Throwable().getStackTrace()[1];
                return ste.getFileName() + ": Line " + ste.getLineNumber();
         }
	
}