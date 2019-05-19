package com.felgt.app.felgt.entities;


import android.content.ContentValues;
import android.database.Cursor;
import android.util.Base64;
import android.util.Log;

import com.felgt.app.felgt.Config;
import com.felgt.app.felgt.crypto.ecdh.ECKeyGenerator;
import com.felgt.app.felgt.utils.CryptoHelper;
import com.felgt.app.felgt.utils.FelgtURI;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

public class Identity {


  public static final String TABLENAME = "identity";
  public static final String ID = "uuid";
  public static final String USERNAME = "username";
  public static final String PRIVATEKEY = "privateKey";
  public static final String PUBLICKEY = "publicKey";
  public static final String TRUSTED = "trusted";
  public static final String PICTUREPATH = "picturepath";

  private String username;
  private String uuid;
  private boolean trusted;
  private byte[] privateKey;
  private byte[] publicKey;
  private String picturePath;

  // Use this constructor only for own Identity
  public Identity(String username, Boolean own) throws NoSuchAlgorithmException {
    ECKeyGenerator gen = new ECKeyGenerator();

    this.trusted = false;
    this.privateKey = gen.getPrivateKey();
    this.publicKey = gen.getPublicKey();
    this.username = username;
    this.uuid = UUID.randomUUID().toString();
    this.picturePath = "";
    if (own) {
      this.uuid = new UUID(0, 0).toString();
    }
  }

  public Identity(String username, byte[] publicKey, byte[] ownPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
    this.privateKey = ECKeyGenerator.getSharedSecret(ECKeyGenerator.bytesToPrivateKey(ownPrivateKey), ECKeyGenerator.bytesToPublicKey(publicKey));

    this.trusted = false;
    this.publicKey = publicKey;
    this.username = username;
    this.uuid = UUID.randomUUID().toString();
    this.picturePath = "";
  }

  public Identity(String uuid, String username, String privateKey, String publicKey, boolean trusted, String picturePath) {
    this.uuid = uuid;
    this.privateKey = Base64.decode(privateKey, Base64.DEFAULT);
    this.publicKey = Base64.decode(publicKey, Base64.DEFAULT);
    this.username = username;
    this.trusted = trusted;
    this.picturePath = picturePath;
  }

  static public Identity fromCursor(Cursor cursor) {
    return new Identity(
        cursor.getString(cursor.getColumnIndex(ID)),
        cursor.getString(cursor.getColumnIndex(USERNAME)),
        cursor.getString(cursor.getColumnIndex(PRIVATEKEY)),
        cursor.getString(cursor.getColumnIndex(PUBLICKEY)),
        cursor.getInt(cursor.getColumnIndex(TRUSTED)) > 0,
        cursor.getString(cursor.getColumnIndex(PICTUREPATH))
    );
  }

  public ContentValues getContentValues() {
    final ContentValues values = new ContentValues();
    values.put(ID, uuid);
    values.put(USERNAME, username);
    values.put(PRIVATEKEY, Base64.encodeToString(privateKey, Base64.DEFAULT));
    values.put(PUBLICKEY, Base64.encodeToString(publicKey, Base64.DEFAULT));
    values.put(TRUSTED, trusted);
    values.put(PICTUREPATH, picturePath);
    return values;
  }

  public byte[] getFingerprint() {
    byte[] fingerprint = publicKey;
    try {
      fingerprint = ECKeyGenerator.getFingerprint(publicKey);
    } catch (NoSuchAlgorithmException e) {
      Log.w(Config.LOGTAG, "Could not create Fingerprint!");
    }
    return fingerprint;
  }

  public String getShareableUri() throws URISyntaxException, UnsupportedEncodingException {
    String publicKey = CryptoHelper.bytesToHex(getPublicKey());

    // maybe switch to android Uri ... but does not matter
    URI uri = new URI(
        FelgtURI.FELGT_URI_PREFIX + ":" +
            Base64.encodeToString(getUsername().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP | Base64.NO_PADDING) + "?" +
            FelgtURI.FELGT_URI_PUBLICKEY + "=" + publicKey);

    return uri.toASCIIString();
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public byte[] getPrivateKey() {
    return privateKey;
  }

  public byte[] getPublicKey() {
    return publicKey;
  }

  public String getUuid() {
    return uuid;
  }

  public boolean isTrusted() {
    return trusted;
  }

  public void setTrusted(boolean trusted) {
    this.trusted = trusted;
  }

  public String getPicturePath() {
    return picturePath;
  }

  public void setPicturePath(String picturePath) {
    this.picturePath = picturePath;
  }
}
