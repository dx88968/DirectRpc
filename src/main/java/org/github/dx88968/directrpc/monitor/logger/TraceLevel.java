package org.github.dx88968.directrpc.monitor.logger;


public enum TraceLevel {
    ALL(0),DEBUG(1),INFO(2),WARN(3),ERROR(4),FATAL(5);
	
	private final int value;

    private TraceLevel(int value) {
    	this.value = value;
    }
    
    public int getValue() {
        return value;
    }
}

