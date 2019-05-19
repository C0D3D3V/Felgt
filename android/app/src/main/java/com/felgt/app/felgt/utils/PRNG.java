package com.felgt.app.felgt.utils;

import android.util.Log;

import com.felgt.app.felgt.Config;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class PRNG {


  public static long bytesToLong(byte[] bytes) {
    byte[] longbytes;
    if (bytes.length >= 8) {
      longbytes = new byte[] {bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7]};
    } else {
      Log.w(Config.LOGTAG, "This should not happen! To less bytes for Long!");
      longbytes = new byte[8];
      for (int i = 0; i < bytes.length; i++) {
        longbytes[i] = bytes[i];
      }
    }
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.put(longbytes);
    buffer.flip();//need flip
    return buffer.getLong();
  }

  public static Random getPRNG(byte[] seed)  {
    Random sr;
    sr = new Random(bytesToLong(seed));
    return sr;
  }
}
