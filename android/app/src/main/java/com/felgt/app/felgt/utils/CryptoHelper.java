package com.felgt.app.felgt.utils;

import android.util.Log;

import com.felgt.app.felgt.Config;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class CryptoHelper {

  private final static char[] hexArray = "0123456789abcdef".toCharArray();

  private CryptoHelper() {
  }

  public static byte[][] convertByteArrayTo16byteArrays(byte[] array) {
    int length = array.length / 16;
    if (array.length % 16 != 0) {
      length += 1;
    }

    byte[][] parts = new byte[length][16];
    for (int i = 0; i < length; i++) {
      for (int j = 0; j < 16; j++) {
        if (i * 16 + j < array.length) {
          parts[i][j] = array[i * 16 + j];
        } else {
          parts[i][j] = 0;
        }
      }
    }
    return parts;
  }

  public static String stringtoHex(String arg) {
    return String.format("%040x", new BigInteger(1, arg.getBytes(StandardCharsets.UTF_8)));
  }

  public static String matrixToString(int[][] matrix) {
    StringBuilder resultBilder = new StringBuilder();

    for (int j = 0; j < 4; j++) {
      byte[] tempLine = new byte[4];
      for (int i = 0; i < 4; i++) {
        tempLine[i] = (byte) matrix[i][j];
      }
      resultBilder.append(CryptoHelper.bytesToHex(tempLine));
    }
    return resultBilder.toString();
  }


  public static int[][] byteArrayToMatrix(byte[] array) {
    int[][] result = new int[4][array.length / 4];
    for (int i = 0; i < array.length / 4; i++) {
      for (int j = 0; j < 4; j++) {
        result[j][i] = array[i * 4 + j] & 0xFF; //Integer.parseInt(iv.substring((8 * i) + (2 * j), (8 * i) + (2 * j + 2)), 16);
      }
    }
    return result;
  }

  public static void byteArrayToMatrix(int[][] array, byte[] key) {
    for (int i = 0; i < key.length / 4; i++) {
      for (int j = 0; j < 4; j++) {
        array[j][i] = key[i * 4 + j] & 0xFF; //Integer.parseInt(iv.substring((8 * i) + (2 * j), (8 * i) + (2 * j + 2)), 16);
      }
    }
  }

  public static String hexToString(final String hexString) {
    return new String(hexToBytes(hexString), StandardCharsets.UTF_8);
  }


  public static void deepCopy2DArray(int[][] destination, int[][] source) {
    assert destination.length == source.length && destination[0].length == source[0].length;
    for (int i = 0; i < destination.length; i++) {
      System.arraycopy(source[i], 0, destination[i], 0, destination[0].length);
    }
  }


  public static byte[] createIV(int blockSizeInBits) {
    SecureRandom secureRandom = new SecureRandom();
    byte[] iv = new byte[blockSizeInBits / 8];
    secureRandom.nextBytes(iv);
    return iv;
  }


  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static byte[] hexToBytes(String hexString) {
    int len = hexString.length();
    byte[] array = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      array[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4) + Character
          .digit(hexString.charAt(i + 1), 16));
    }
    return array;
  }

  public static String cutStringAtEnd(String string) throws UnsupportedEncodingException {
    byte[] asArray = string.getBytes(StandardCharsets.UTF_8);

    int size = 0;
    while (size < asArray.length) {
      if (asArray[size] == 0) {
        break;
      }
      size++;
    }
    return new String(asArray, 0, size, StandardCharsets.UTF_8);
  }

  public static byte[] intToBytes(int x) {
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
    buffer.putInt(x);
    return buffer.array();
  }

  public static int bytesToInt(byte[] bytes) {
    byte[] intbytes;
    if (bytes.length >= 4) {
      intbytes = new byte[] {bytes[0], bytes[1], bytes[2], bytes[3]};
    } else {
      Log.w(Config.LOGTAG, "This should not happen! To less bytes for Int!");
      intbytes = new byte[4];
      for (int i = 0; i < bytes.length; i++) {
        intbytes[i] = bytes[i];
      }
    }
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
    buffer.put(intbytes);
    buffer.flip();//need flip
    return buffer.getInt();
  }
  public static String prettifyFingerprint(String fingerprint) {
    if (fingerprint == null) {
      return "";
    } else if (fingerprint.length() < 40) {
      return fingerprint;
    }
    StringBuilder builder = new StringBuilder(fingerprint);
    for (int i = 8; i < builder.length(); i += 9) {
      builder.insert(i, ' ');
    }
    return builder.toString();
  }

  public static String getFingerprint(String value) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      return bytesToHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      return "";
    }
  }
}
