/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.github.dx88968.directrpc.core;

import java.io.DataInput;
import java.io.IOException;

/**
 *
 * @author DX
 */
public class ProtoUtils {
    
    
    /**
   * Read a variable length integer in the same format that ProtoBufs encodes.
   * @param in the input stream to read from
   * @return the integer
   * @throws IOException if it is malformed or EOF.
   */
  public static int readRawVarint32(DataInput in) throws IOException {
    byte tmp = in.readByte();
    if (tmp >= 0) {
      return tmp;
    }
    int result = tmp & 0x7f;
    if ((tmp = in.readByte()) >= 0) {
      result |= tmp << 7;
    } else {
      result |= (tmp & 0x7f) << 7;
      if ((tmp = in.readByte()) >= 0) {
        result |= tmp << 14;
      } else {
        result |= (tmp & 0x7f) << 14;
        if ((tmp = in.readByte()) >= 0) {
          result |= tmp << 21;
        } else {
          result |= (tmp & 0x7f) << 21;
          result |= (tmp = in.readByte()) << 28;
          if (tmp < 0) {
            // Discard upper 32 bits.
            for (int i = 0; i < 5; i++) {
              if (in.readByte() >= 0) {
                return result;
              }
            }
            throw new IOException("Malformed varint");
          }
        }
      }
    }
    return result;
  }
}
