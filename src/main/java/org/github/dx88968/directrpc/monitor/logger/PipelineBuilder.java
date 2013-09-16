package org.github.dx88968.directrpc.monitor.logger;

import java.io.IOException;


public class PipelineBuilder {
	
	int Max_Buffer_Size=0;
	String name;
	String accessibleID;
	
	
	/*
	 * if size<=0, then no upper limit will be set, could cause OFM 
	 */
	public PipelineBuilder setMaxBufferSize(int size){
		Max_Buffer_Size=size;
		return this;
	}
	
	public PipelineBuilder setName(String name) {
		this.name=name;
		return this;
	}
	
	
	 public Pipeline build() throws IOException{ 
		 Pipeline pipeline= new Pipeline(Max_Buffer_Size);
		 pipeline.setName(name);
		 return pipeline;
	 }

}
