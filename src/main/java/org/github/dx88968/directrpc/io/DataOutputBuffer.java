/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.github.dx88968.directrpc.io;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author DX
 */
public class DataOutputBuffer extends DataOutputStream {

  private static class Buffer extends ByteArrayOutputStream {
    public byte[] getData() { return buf; }
    public int getLength() { return count; }

    public Buffer() {
      super();
    }
    
    public Buffer(int size) {
      super(size);
    }
    
    public void write(DataInput in, int len) throws IOException {
      int newcount = count + len;
      if (newcount > buf.length) {
        byte newbuf[] = new byte[Math.max(buf.length << 1, newcount)];
        System.arraycopy(buf, 0, newbuf, 0, count);
        buf = newbuf;
      }
      in.readFully(buf, count, len);
      count = newcount;
    }
  }

  private Buffer buffer;
  
  /** Constructs a new empty buffer. */
  public DataOutputBuffer() {
    this(new Buffer());
  }
  
  public DataOutputBuffer(int size) {
    this(new Buffer(size));
  }
  
  private DataOutputBuffer(Buffer buffer) {
    super(buffer);
    this.buffer = buffer;
  }

  /** Returns the current contents of the buffer.
   *  Data is only valid to {@link #getLength()}.
   */
  public byte[] getData() { return buffer.getData(); }

  /** Returns the length of the valid data currently in the buffer. */
  public int getLength() { return buffer.getLength(); }

  /** Resets the buffer to empty. */
  public DataOutputBuffer reset() {
    this.written = 0;
    buffer.reset();
    return this;
  }

  /** Writes bytes from a DataInput directly into the buffer. */
  public void write(DataInput in, int length) throws IOException {
    buffer.write(in, length);
  }

  /** Write to a file stream */
  public void writeTo(OutputStream out) throws IOException {
    buffer.writeTo(out);
  }
}

