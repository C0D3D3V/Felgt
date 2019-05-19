package com.felgt.app.felgt.persistance;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.felgt.app.felgt.Config;
import com.felgt.app.felgt.R;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class FileBackend {
  private static final String FILE_PROVIDER = "com.felgt.app.felgt.provider";
  private Context context;

  public FileBackend(Context context) {
    this.context = context;
  }

  public static void close(Closeable stream) {
    if (stream != null) {
      try {
        stream.close();
      } catch (IOException e) {
      }
    }
  }

  public static Uri getUriForFile(Context context, File file) {
    try {
      return FileProvider.getUriForFile(context, FILE_PROVIDER, file);
    } catch (IllegalArgumentException e) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        throw new SecurityException(e);
      } else {
        return Uri.fromFile(file);
      }
    }
  }

  public String getExtensionFromUri(Uri uri) {
    String[] projection = {MediaStore.MediaColumns.DATA};
    String filename = null;
    Cursor cursor;
    try {
      cursor = context.getContentResolver().query(uri, projection, null, null, null);
    } catch (IllegalArgumentException e) {
      cursor = null;
    }
    if (cursor != null) {
      try {
        if (cursor.moveToFirst()) {
          filename = cursor.getString(0);
        }
      } catch (Exception e) {
        filename = null;
      } finally {
        cursor.close();
      }
    }
    if (filename == null) {
      final List<String> segments = uri.getPathSegments();
      if (segments.size() > 0) {
        filename = segments.get(segments.size() - 1);
      }
    }
    int pos = filename == null ? -1 : filename.lastIndexOf('.');
    return pos > 0 ? filename.substring(pos + 1) : null;
  }

  public void saveAvatar(String uuid, Uri uri) throws FileCopyException {
    File f = new FileCache(context).getFile(uuid, true);
    if (f.exists()) {
      Log.d(Config.LOGTAG, "delete old avatar " + f.getAbsolutePath());

      f.delete();
    }
    copyFileToPrivateStorage(f, uri);
  }

  public void copyFileToPrivateStorage(File file, Uri uri) throws FileCopyException {
    Log.d(Config.LOGTAG, "copy file (" + uri.toString() + ") to private storage " + file.getAbsolutePath());
    file.getParentFile().mkdirs();
    OutputStream os = null;
    InputStream is = null;
    try {
      file.createNewFile();
      os = new FileOutputStream(file);
      is = context.getContentResolver().openInputStream(uri);
      byte[] buffer = new byte[1024];
      int length;
      while ((length = is.read(buffer)) > 0) {
        os.write(buffer, 0, length);
      }
      os.flush();
    } catch (FileNotFoundException e) {
      Log.w(Config.LOGTAG, "Could not copy file.", e);
      throw new FileCopyException(R.string.fileNotFound);
    } catch (Exception e) {
      Log.w(Config.LOGTAG, "Could not copy file.", e);
      throw new FileCopyException(R.string.ioException);
    } finally {
      close(os);
      close(is);
    }
    Log.i(Config.LOGTAG, "Copied file.");
  }

  public class FileCopyException extends Exception {
    private int resId;

    public FileCopyException(int resId) {
      this.resId = resId;
    }

    public int getResId() {
      return resId;
    }
  }

  public void deleteCachedAudioFiles()
  {
    File cacheDir = new File(context.getFilesDir().getAbsolutePath(), "Recordings");
    File[] files = cacheDir.listFiles();
    if (files == null) {
      return;
    }
    for (File f : files) {
      Log.i(Config.LOGTAG, "Delete Recording " + f.getAbsolutePath());
      f.delete();
    }
    cacheDir = new File(context.getCacheDir().getAbsolutePath(), "Received");
    files = cacheDir.listFiles();
    if (files == null) {
      return;
    }
    for (File f : files) {
      Log.i(Config.LOGTAG, "Delete Received " + f.getAbsolutePath());
      f.delete();
    }

  }
}
