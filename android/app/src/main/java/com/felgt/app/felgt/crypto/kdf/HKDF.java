package com.felgt.app.felgt.crypto.kdf;

import android.util.Log;

import com.felgt.app.felgt.Config;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HKDF {

  static public byte[] getMessageKey(byte[] sharedKey, byte[] iv) {
    byte[] stepResult = sharedKey;
    try {

      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(sharedKey, "HmacSHA256"));
      for (int i = 0; i < 5; i++) {
        mac.update(iv);
        mac.update((byte) i);
      }

      stepResult = mac.doFinal();
    } catch (Exception e) {
      Log.w(Config.LOGTAG, "Could not generate Message key!");
    }
    return stepResult;
  }
}
