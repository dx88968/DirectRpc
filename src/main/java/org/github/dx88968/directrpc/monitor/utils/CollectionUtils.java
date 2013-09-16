package org.github.dx88968.directrpc.monitor.utils;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionUtils {
	
	public static <T1,T2> T2 getHashMapElementByHash(ConcurrentHashMap<T1,T2> target,int hashcode){
		Iterator<T1> iter=target.keySet().iterator();
		Object key=null;
		while (iter.hasNext()) {
			key=iter.next();
			if (key.hashCode()==hashcode) {
				return target.get(key);
			}
		}
		return null;
	}
	
	public static <T1,T2> void removeHashMapElementByHash(ConcurrentHashMap<T1,T2> target,int hashcode){
		Iterator<T1> iter=target.keySet().iterator();
		Object key=null;
		while (iter.hasNext()) {
			key=iter.next();
			if (key.hashCode()==hashcode) {
				target.remove(key);
			}
		}
	}

}
