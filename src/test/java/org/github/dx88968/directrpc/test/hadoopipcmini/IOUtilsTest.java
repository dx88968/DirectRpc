package org.github.dx88968.directrpc.test.hadoopipcmini;


import java.io.IOException;
import java.util.List;

import org.github.dx88968.directrpc.monitor.logger.Pipeline;
import org.github.dx88968.directrpc.monitor.logger.PipelineBuilder;
import org.github.dx88968.directrpc.monitor.logger.Session;


public class IOUtilsTest {

	public static void main(String[] args) throws IOException {
		Pipeline pipeline=new PipelineBuilder().setMaxBufferSize(0).build();
		pipeline.writeline("1");
		pipeline.writeline("2");
		pipeline.writeline("3");
		Session session=pipeline.createSession();
		List<String> lines = session.getBuffer();
		for (int i = 0; i < lines.size(); i++) {
			System.out.println(lines.get(i));
		}
	}
}
