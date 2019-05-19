package com.felgt.app.felgt.crypto.ecdh;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;

public class ECKeyGenerator {
  private PrivateKey privateKey;
  private PublicKey publicKey;

  public ECKeyGenerator() throws NoSuchAlgorithmException {
    KeyPairGenerator keygen = KeyPairGenerator.getInstance("EC");
    keygen.initialize(256, new SecureRandom());
    KeyPair keyPair = keygen.genKeyPair();

    this.privateKey = keyPair.getPrivate();
    this.publicKey = keyPair.getPublic();
  }

  static public byte[] getSharedSecret(Key ownPrivate, Key foreignPublic) throws NoSuchAlgorithmException, InvalidKeyException {
    KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
    keyAgreement.init(ownPrivate);
    keyAgreement.doPhase(foreignPublic, true);
    return keyAgreement.generateSecret();
  }

  static public byte[] getFingerprint(byte[] publicKey) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    return md.digest(publicKey);
  }

  static public PublicKey bytesToPublicKey(byte[] publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
    X509EncodedKeySpec ks = new X509EncodedKeySpec(publicKey);
    KeyFactory kf = java.security.KeyFactory.getInstance("EC");

    return kf.generatePublic(ks);
  }

  static public PrivateKey bytesToPrivateKey(byte[] privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
    PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(privateKey);
    KeyFactory kf = java.security.KeyFactory.getInstance("EC");

    return kf.generatePrivate(ks);
  }

  public byte[] getPrivateKey() {
    return privateKey.getEncoded();
  }

  public byte[] getPublicKey() {
    return publicKey.getEncoded();
  }
}