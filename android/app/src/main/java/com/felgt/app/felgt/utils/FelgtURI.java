package com.felgt.app.felgt.utils;

import android.net.Uri;
import android.util.Base64;

import com.felgt.app.felgt.entities.ExtractedUriInformation;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class FelgtURI {

  public static final String FELGT_URI_PREFIX = "felgt";
  public static final String FELGT_URI_PUBLICKEY = "publickey";


  static private byte[] parsePublicKey(String query) {
    String[] parts = query.split("=", 2);
    if (parts.length == 2) {
      String key = parts[0].toLowerCase(Locale.US);
      String value = parts[1].toLowerCase(Locale.US);
      if (key.equals(FELGT_URI_PUBLICKEY)) {
        return CryptoHelper.hexToBytes(value);
      }
    }
    return null;
  }

  static public ExtractedUriInformation parse(final Uri uri) {
    if (uri == null) {
      return null;
    }

    String scheme = uri.getScheme();
    if (FELGT_URI_PREFIX.equalsIgnoreCase(scheme)) {
      // sample: felgt:BASE64::username?publicy=HEXSTRING

      String username;
      try {
        // get username
        if (uri.getAuthority() != null) {
          username = new String(Base64.decode(uri.getAuthority(), Base64.DEFAULT), StandardCharsets.UTF_8);

        } else {
          String[] parts = uri.getSchemeSpecificPart().split("\\?");
          if (parts.length > 0) {
            username = new String(Base64.decode(parts[0], Base64.DEFAULT), StandardCharsets.UTF_8);
          } else {
            return null;
          }
        }

        // get publicKey
        byte[] publicKey = parsePublicKey(uri.getQuery());
        if (publicKey != null) {
          return new ExtractedUriInformation(username, publicKey);
        }
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }


}
