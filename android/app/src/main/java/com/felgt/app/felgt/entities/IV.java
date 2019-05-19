package com.felgt.app.felgt.entities;

import android.content.ContentValues;
import android.util.Base64;

import com.felgt.app.felgt.persistance.DatabaseBackend;
import com.felgt.app.felgt.utils.CryptoHelper;

public class IV {

  public static final String TABLENAME = "iv";
  public static final String NONCE = "value";

  protected final byte[] nonce;

  public IV(DatabaseBackend databaseBackend) {
    IV nonceTemp;
    while (true) {

      nonceTemp = new IV(CryptoHelper.createIV(128));
      if (!databaseBackend.containsIV(nonceTemp)) {
        break;
      }
    }
    this.nonce = nonceTemp.getNonce();
    databaseBackend.storeIV(nonceTemp);
  }

  private IV(byte[] iv) {
    this.nonce = iv;
  }

  public ContentValues getContentValues() {
    final ContentValues values = new ContentValues();
    values.put(NONCE, Base64.encodeToString(nonce, Base64.DEFAULT));
    return values;
  }

  public byte[] getNonce() {
    return nonce;
  }
}
