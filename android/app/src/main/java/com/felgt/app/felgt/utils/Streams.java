package com.felgt.app.felgt.utils;

import java.io.InputStream;
import java.io.OutputStream;

public class Streams {
  private Streams() {
  }

  public static void CopyStream(InputStream is, OutputStream os) {
    byte[] buffer = new byte[1024];
    int bufferLength = 0;

    try {
      while ((bufferLength = is.read(buffer)) > 0) {
        os.write(buffer, 0, bufferLength);
      }

    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
