package com.felgt.app.felgt.entities;

import android.content.ContentValues;
import android.database.Cursor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Message {


  public static final String TABLENAME = "message";
  public static final String ID = "uuid";
  public static final String SENDER = "sender";
  public static final String RESEIVER = "receiver";
  public static final String DATE = "date";
  public static final String MESSAGE = "message";

  private String uuid;
  private String sender;
  private String receiver;
  private String message;
  private Long date; //Number


  public Message(String id, String sender, String receiver, String message, Long date) {
    this.uuid = id;
    this.sender = sender;
    this.receiver = receiver;
    this.message = message;
    this.date = date;
  }

  public Message(Identity sender, Identity receiver, String message) {
    this.uuid = UUID.randomUUID().toString();
    this.sender = sender.getUuid();
    this.receiver = receiver.getUuid();
    this.message = message;
    this.date = System.currentTimeMillis();
  }

  static public Message fromCursor(Cursor cursor) {
    return new Message(
        cursor.getString(cursor.getColumnIndex(ID)),
        cursor.getString(cursor.getColumnIndex(SENDER)),
        cursor.getString(cursor.getColumnIndex(RESEIVER)),
        cursor.getString(cursor.getColumnIndex(MESSAGE)),
        cursor.getLong(cursor.getColumnIndex(DATE))
    );
  }

  public ContentValues getContentValues() {
    final ContentValues values = new ContentValues();
    values.put(ID, uuid);
    values.put(SENDER, sender);
    values.put(RESEIVER, receiver);
    values.put(MESSAGE, message);
    values.put(DATE, date);
    return values;
  }

  public String getUuid() {
    return uuid;
  }

  public String getSender() {
    return sender;
  }

  public String getReceiver() {
    return receiver;
  }

  public String getMessage() {
    return message;
  }

  public String getShortVersion() {
    if (message.length() > 28) {
      return message.substring(0, 25) + "...";
    }

    return message;
  }

  public String getDate() {
    SimpleDateFormat dayTime = new SimpleDateFormat("dd.MM.yy hh:mm");
    return dayTime.format(new Date(date));
  }
}
