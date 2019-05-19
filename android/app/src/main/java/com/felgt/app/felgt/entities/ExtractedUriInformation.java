package com.felgt.app.felgt.entities;

public class ExtractedUriInformation {

  public final String username;
  public final byte[] publicKey;

  public ExtractedUriInformation(String username, byte[] publicKey) {
    this.username = username;
    this.publicKey = publicKey;
  }

}