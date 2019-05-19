package com.felgt.app.felgt.crypto.aes;

import com.felgt.app.felgt.utils.CryptoHelper;

import java.security.KeyException;

public class AES {


  // key 256bit , iv 128 bit
  public static String encrypt(byte[] message, byte[] key, byte[] iv) throws KeyException {
    if (iv.length != 16 || key.length != 32) {
      throw new KeyException("Wrong key length!");
    }

    int numRounds = 10 + (((key.length * 8 - 128) / 32));
    int[][] state;
    int[][] initvector = CryptoHelper.byteArrayToMatrix(iv);
    int[][] keymatrix = KeyAddition.keySchedule(key);
    StringBuilder encryptBuilder = new StringBuilder();

    byte[][] messageParts = CryptoHelper.convertByteArrayTo16byteArrays(message);

    for (byte[] line : messageParts) {
      state = CryptoHelper.byteArrayToMatrix(line);

      KeyAddition.addRoundKey(state, initvector);
      KeyAddition.addRoundKey(state, KeyAddition.subKey(keymatrix, 0)); //Starts the addRoundKey with the first part of Key Expansion
      for (int i = 1; i < numRounds; i++) {
        ByteSubstitution.subBytes(state); //implements the Sub-Bytes subroutine.
        ShiftRows.shiftRows(state); //implements Shift-Rows subroutine.
        MixColumn.mixColumns(state);
        KeyAddition.addRoundKey(state, KeyAddition.subKey(keymatrix, i));
      }
      ByteSubstitution.subBytes(state); //implements the Sub-Bytes subroutine.
      ShiftRows.shiftRows(state); //implements Shift-Rows subroutine.
      KeyAddition.addRoundKey(state, KeyAddition.subKey(keymatrix, numRounds));
      initvector = state;

      encryptBuilder.append(CryptoHelper.matrixToString(state));
    }
    return encryptBuilder.toString();
  }


  // key 256bit , iv 128 bit
  public static String decrypt(byte[] message, byte[] key, byte[] iv) throws KeyException {
    if (iv.length != 16 || key.length != 32) {
      throw new KeyException("Wrong key length!");
    }

    int numRounds = 10 + (((key.length * 8 - 128) / 32));
    int[][] state;
    int[][] initvector = CryptoHelper.byteArrayToMatrix(iv);
    int[][] nextvector = new int[4][4];
    int[][] keymatrix = KeyAddition.keySchedule(key);
    StringBuilder decryptBuilder = new StringBuilder();


    byte[][] messageParts = CryptoHelper.convertByteArrayTo16byteArrays(message);
    for (byte[] line : messageParts) {
      state = CryptoHelper.byteArrayToMatrix(line);

      CryptoHelper.deepCopy2DArray(nextvector, state);
      KeyAddition.addRoundKey(state, KeyAddition.subKey(keymatrix, numRounds));
      for (int i = numRounds - 1; i > 0; i--) {
        ShiftRows.invShiftRows(state);
        ByteSubstitution.invSubBytes(state);
        KeyAddition.addRoundKey(state, KeyAddition.subKey(keymatrix, i));
        MixColumn.invMixColumns(state);
      }
      ShiftRows.invShiftRows(state);
      ByteSubstitution.invSubBytes(state);

      KeyAddition.addRoundKey(state, KeyAddition.subKey(keymatrix, 0));

      KeyAddition.addRoundKey(state, initvector);
      CryptoHelper.deepCopy2DArray(initvector, nextvector);

      decryptBuilder.append(CryptoHelper.matrixToString(state));
    }

    return decryptBuilder.toString();

  }
}

