package org.github.dx88968.directrpc.monitor.logger;

import java.util.regex.Pattern;

public class Condition {

	String traceMark;
	String className;
	String methodRep;//combines methodName and its argument types
	TraceLevel level;
	
	
	
	public Condition(String traceMark, String className, String methodRep,TraceLevel level) {
		this.traceMark = (traceMark!=null && traceMark.length()>0)?traceMark:null;
		this.className = (className!=null && className.length()>0)?className:null;
		this.methodRep = (methodRep!=null && methodRep.length()>0)?methodRep:null;;
		this.level=level;
	}

	public boolean satisfy(Condition c){
		return satisfy(c.traceMark,c.className,c.methodRep,c.level);
	}
	
	public boolean satisfy(String traceMark,String className, String methodRep,TraceLevel level){
		Pattern pattern;
		if (traceMark != null) {
			if(this.traceMark==null){
				return false;
			}
			pattern=Pattern.compile(traceMark);
			if (!pattern.matcher(this.traceMark).matches()) {
				return false;
			}
		}
		if (className != null) {
			if (this.className==null) {
				return false;
			}
			pattern=Pattern.compile(className);
			if (!pattern.matcher(this.className).matches()) {
				return false;
			}
		}
		if (methodRep != null) {
			if (this.methodRep==null) {
				return false;
			}
			pattern=Pattern.compile(methodRep);
			if (!pattern.matcher(this.methodRep).matches()) {
				return false;
			}
		}
		if (this.level!=null && level!=null && level.getValue()>this.level.getValue()) {
				return false;
		}
		return true;	
	}
	
	

	@Override
	public String toString() {
		return "Condition [traceMark=" + traceMark + ", className=" + className
				+ ", methodRep=" + methodRep + ", level=" + level + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((className == null) ? 0 : className.hashCode());
		result = prime * result + ((level == null) ? 0 : level.hashCode());
		result = prime * result
				+ ((methodRep == null) ? 0 : methodRep.hashCode());
		result = prime * result
				+ ((traceMark == null) ? 0 : traceMark.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Condition)) {
			return false;
		}
		Condition other = (Condition) obj;
		if (className == null) {
			if (other.className != null) {
				return false;
			}
		} else if (!className.equals(other.className)) {
			return false;
		}
		if (level != other.level) {
			return false;
		}
		if (methodRep == null) {
			if (other.methodRep != null) {
				return false;
			}
		} else if (!methodRep.equals(other.methodRep)) {
			return false;
		}
		if (traceMark == null) {
			if (other.traceMark != null) {
				return false;
			}
		} else if (!traceMark.equals(other.traceMark)) {
			return false;
		}
		return true;
	}

	
	
	
	
}
