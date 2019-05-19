package com.felgt.app.felgt.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.felgt.app.felgt.utils.CryptoHelper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;

public class BarcodeProvider {

  private BarcodeProvider() {
  }

  public static Bitmap create2dBarcodeBitmap(String input, int size) {
    try {
      final QRCodeWriter barcodeWriter = new QRCodeWriter();
      final Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
      hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
      final BitMatrix result = barcodeWriter.encode(input, BarcodeFormat.QR_CODE, size, size, hints);
      final int width = result.getWidth();
      final int height = result.getHeight();
      final int[] pixels = new int[width * height];
      for (int y = 0; y < height; y++) {
        final int offset = y * width;
        for (int x = 0; x < width; x++) {
          pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
        }
      }
      final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
      bitmap.setPixels(pixels, 0, width, 0, 0, width, height);


      return bitmap;
    } catch (final Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static File getCached2dBarcodeBitmap(Context context, String input, int size) throws IOException, NullPointerException {
    String hash = CryptoHelper.getFingerprint(input);
    File file = new File(context.getCacheDir().getAbsolutePath() + "/imageCache/" + hash);
    if (file.exists() && file.isFile()) {
      return file;
    }
    Bitmap newQRCode = create2dBarcodeBitmap(input, size);
    file.getParentFile().mkdirs();
    file.createNewFile();
    OutputStream outputStream = new FileOutputStream(file);
    newQRCode.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
    outputStream.close();
    outputStream.flush();
    return file;
  }
}
