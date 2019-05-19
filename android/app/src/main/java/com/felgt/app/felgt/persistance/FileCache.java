package com.felgt.app.felgt.persistance;

import android.content.Context;

import com.felgt.app.felgt.utils.CryptoHelper;

import java.io.File;

public class FileCache {

  private File cacheDir;

  public FileCache(Context context) {
    //Find the dir to save cached images
    if (android.os.Environment.getExternalStorageState()
        .equals(android.os.Environment.MEDIA_MOUNTED)) {
      cacheDir = new File(context.getCacheDir(), "imageCache"); //android.os.Environment.getExternalStorageDirectory()
    } else {
      cacheDir = context.getCacheDir();
    }

    if (!cacheDir.exists()) {
      cacheDir.mkdirs();
    }
  }

  public File getFile(String url, boolean hashed) {
    String filename = url;
    if (hashed) {
      filename = CryptoHelper.getFingerprint(url);
    }
    //maybe URLEncoder.encode(url)
    return new File(cacheDir, filename);

  }

  public void clear() {
    File[] files = cacheDir.listFiles();
    if (files == null) {
      return;
    }
    for (File f : files) {
      f.delete();
    }
  }
}
