package com.felgt.app.felgt.persistance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;

import com.felgt.app.felgt.Config;
import com.felgt.app.felgt.entities.IV;
import com.felgt.app.felgt.entities.Identity;
import com.felgt.app.felgt.entities.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseBackend extends SQLiteOpenHelper {

  public static final String DATABASE_NAME = "felgt.db";
  private static final int DATABASE_VERSION = 2;
  private static DatabaseBackend instance = null;

  public DatabaseBackend(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  public static synchronized DatabaseBackend getInstance(Context context) {
    if (instance == null) {
      instance = new DatabaseBackend(context);
    }
    return instance;
  }

  /* Alternative
  private static String CREATE_IV_TABLE = "CREATE TABLE "
      + IV.TABLENAME + "("
      + IV.NONCE + " TEXT,"
      + "UNIQUE(" + IV.VALUE + ") ON CONFLICT REPLACE"
      + ");";
  */

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("create table " + IV.TABLENAME + "(" + IV.NONCE + " TEXT PRIMARY KEY)");

    db.execSQL("create table " + Identity.TABLENAME + "(" + Identity.ID + " TEXT PRIMARY KEY,"
        + Identity.USERNAME + " TEXT, "
        + Identity.PRIVATEKEY + " TEXT, "
        + Identity.PUBLICKEY + " TEXT, "
        + Identity.TRUSTED + " INTEGER DEFAULT 0,"
        + Identity.PICTUREPATH + " TEXT)");


    db.execSQL("create table " + Message.TABLENAME + "(" + Message.ID + " TEXT PRIMARY KEY,"
        + Message.SENDER + " TEXT, "
        + Message.RESEIVER + " TEXT, "
        + Message.MESSAGE + " TEXT, "
        + Message.DATE + " NUMBER)");

  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
  }


  public void addIV(IV iv) {
    SQLiteDatabase db = this.getWritableDatabase();
    db.insert(IV.TABLENAME, null, iv.getContentValues());
  }

  private Cursor getCursorForIV(IV iv) {
    SQLiteDatabase db = this.getReadableDatabase();
    String[] columns = {IV.NONCE};
    ArrayList<String> selectionArgs = new ArrayList<>(1);

    selectionArgs.add(Base64.encodeToString(iv.getNonce(), Base64.DEFAULT));
    String selectionString = IV.NONCE + " = ?";

    return db.query(IV.TABLENAME,
        columns,
        selectionString,
        selectionArgs.toArray(new String[selectionArgs.size()]),
        null, null, null);
  }

  public boolean containsIV(IV iv) {
    Cursor cursor = getCursorForIV(iv);
    int count = cursor.getCount();
    cursor.close();
    return count != 0;
  }


  public boolean containsIdentity(Identity identity) {
    Cursor cursor = getCursorForIdentity(identity.getUuid());
    int count = cursor.getCount();
    cursor.close();
    return count != 0;
  }

  public boolean containsIdentity(String username, byte[] publicKey) {
    Cursor cursor = getCursorForIdentity(username, publicKey);
    int count = cursor.getCount();
    cursor.close();
    return count != 0;
  }

  public void storeIdentity(Identity identity) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = identity.getContentValues();
    String where = Identity.ID + "=?"; // maybe check here also for name and fingerprint
    String[] whereArgs = {identity.getUuid()};
    int rows = db.update(Identity.TABLENAME, values, where, whereArgs);
    if (rows == 0) {
      db.insert(Identity.TABLENAME, null, values);
    }
  }

  public void storeIV(IV iv) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = iv.getContentValues();
    String where = IV.NONCE + "=?"; // maybe check here also for name and fingerprint
    String[] whereArgs = {Base64.encodeToString(iv.getNonce(), Base64.DEFAULT)};
    int rows = db.update(IV.TABLENAME, values, where, whereArgs);
    if (rows == 0) {
      db.insert(IV.TABLENAME, null, values);
    }
  }


  public void storeMessage(Message message) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = message.getContentValues();
    String where = Message.ID + "=?";
    String[] whereArgs = {message.getUuid()};
    int rows = db.update(Message.TABLENAME, values, where, whereArgs);
    if (rows == 0) {
      db.insert(Message.TABLENAME, null, values);
    }
  }

  private Cursor getCursorForIdentity(String uuid) {
    SQLiteDatabase db = this.getReadableDatabase();
    String[] args = {uuid};
    String selectionString = Identity.ID + " = ?";

    return db.query(Identity.TABLENAME,
        null,
        selectionString,
        args,
        null, null, null);
  }

  private Cursor getCursorForIdentity(String username, byte[] publicKey) {
    SQLiteDatabase db = this.getReadableDatabase();
    String[] args = {username, Base64.encodeToString(publicKey, Base64.DEFAULT)};
    String selectionString = Identity.USERNAME + " = ? AND " + Identity.PUBLICKEY + " = ?";

    return db.query(Identity.TABLENAME,
        null,
        selectionString,
        args,
        null, null, null);
  }

  public List<Identity> getContacts() {
    SQLiteDatabase db = this.getReadableDatabase();
    List<Identity> list = new ArrayList<>();
    String[] args = {new UUID(0, 0).toString()};
    Cursor cursor = db.query(Identity.TABLENAME, null, Identity.ID + "!=?", args, null,
        null, null);
    while (cursor.moveToNext()) {
      list.add(Identity.fromCursor(cursor));
    }
    cursor.close();
    return list;
  }

  public List<Message> getMessages(Identity identity) {
    SQLiteDatabase db = this.getReadableDatabase();
    List<Message> list = new ArrayList<>();
    String[] args = {identity.getUuid(), identity.getUuid()};
    Cursor cursor = db.query(Message.TABLENAME, null, Message.RESEIVER + "=? or " + Message.SENDER + " =?", args, null,
        null, Message.DATE + " ASC");
    while (cursor.moveToNext()) {
      list.add(Message.fromCursor(cursor));
    }
    cursor.close();
    return list;
  }

  public Identity loadOwnIdentity() {
    Cursor cursor = getCursorForIdentity(new UUID(0, 0).toString());
    if (cursor.getCount() != 0) {
      cursor.moveToFirst();
      Identity result = Identity.fromCursor(cursor);
      cursor.close();
      return result;
    }
    return null;
  }

  public Identity loadIdentity(String uuid) {
    Cursor cursor = getCursorForIdentity(uuid);
    if (cursor.getCount() != 0) {
      cursor.moveToFirst();
      Identity result = Identity.fromCursor(cursor);
      cursor.close();
      return result;
    }
    return null;
  }

  public Identity loadIdentity(String username, byte[] publicKey) {
    Cursor cursor = getCursorForIdentity(username, publicKey);
    if (cursor.getCount() != 0) {
      cursor.moveToFirst();
      Identity result = Identity.fromCursor(cursor);
      cursor.close();
      return result;
    }
    return null;
  }

  public Message getLastMessageReceived(Identity identity) {
    Cursor cursor = null;
    try {
      SQLiteDatabase db = this.getReadableDatabase();
      String[] args = {identity.getUuid(), identity.getUuid()};
      cursor = db.query(Message.TABLENAME, null, Message.SENDER + "=? OR " + Message.RESEIVER + "=?", args, null,
          null, Message.DATE + " DESC");
      if (cursor.getCount() == 0) {
        return null;
      } else {
        cursor.moveToFirst();
        return Message.fromCursor(cursor);
      }
    } catch (Exception e) {
      return null;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  public boolean deleteIdentity(Identity identity) {
    deleteAllMessagesInChatWith(identity);
    SQLiteDatabase db = this.getWritableDatabase();
    String[] args = {identity.getUuid()};
    final int rows = db.delete(Identity.TABLENAME, Identity.ID + "=?", args);
    return rows == 1;
  }

  public boolean deleteMessage(Message message) {
    SQLiteDatabase db = this.getWritableDatabase();
    String[] args = {message.getUuid()};
    final int rows = db.delete(Message.TABLENAME, Message.ID + "=?", args);
    return rows == 1;
  }

  public void deleteAllMessagesInChatWith(Identity identity) {
    long start = SystemClock.elapsedRealtime();
    final SQLiteDatabase db = this.getWritableDatabase();
    db.beginTransaction();
    String[] args = {identity.getUuid(), identity.getUuid()};
    int num = db.delete(Message.TABLENAME, Message.RESEIVER + "=? OR " + Message.SENDER + "=?", args);
    db.setTransactionSuccessful();
    db.endTransaction();
    Log.d(Config.LOGTAG, "deleted " + num + " messages for " + identity.getUsername() + " in " + (SystemClock.elapsedRealtime() - start) + "ms");
  }

}
