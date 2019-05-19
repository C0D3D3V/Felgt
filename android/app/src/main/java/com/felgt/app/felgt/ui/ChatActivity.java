package com.felgt.app.felgt.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.felgt.app.felgt.Config;
import com.felgt.app.felgt.R;
import com.felgt.app.felgt.crypto.aes.AES;
import com.felgt.app.felgt.crypto.ecdh.ECKeyGenerator;
import com.felgt.app.felgt.crypto.kdf.HKDF;
import com.felgt.app.felgt.entities.IV;
import com.felgt.app.felgt.entities.Identity;
import com.felgt.app.felgt.entities.Message;
import com.felgt.app.felgt.persistance.DatabaseBackend;
import com.felgt.app.felgt.persistance.FileBackend;
import com.felgt.app.felgt.ui.adapter.MessageAdapter;
import com.felgt.app.felgt.utils.CryptoHelper;
import com.felgt.app.felgt.utils.PRNG;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ChatActivity extends AppCompatActivity {
  public static final String CONTACTID = "com.felgt.app.contactid";
  public static final String ENCRYPTEDAUDIO = "com.felgt.app.encryptedaudio";
  public static final int RUNCHATDETAILS = 92;
  public static final int REQUEST_VOICE_RECORD = 95;
  public static final int REQUEST_SELECT_AUDIO_FILE = 96;

  private String contactUUID;
  private DatabaseBackend databaseBackend;
  private Identity identity;
  private Identity ownIdentity;
  private boolean changed;
  private MessageAdapter messageAdapter;
  private List<Message> messages;
  private Context context;
  private TextView lbNoMessages;
  private RecyclerView rvMessageList;
  private EditText txChatbox;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_chat);
    databaseBackend = DatabaseBackend.getInstance(this.getApplicationContext());

    Intent intent = getIntent();

    contactUUID = intent.getStringExtra(CONTACTID);
    if (savedInstanceState != null) {
      contactUUID = savedInstanceState.getString(CONTACTID);
    }

    rvMessageList = findViewById(R.id.rvMessageList);
    txChatbox = findViewById(R.id.txChatbox);
    lbNoMessages = findViewById(R.id.lbNoMessages);
    Button btEncrypt = findViewById(R.id.btEncrypt);
    Button btDecrypt = findViewById(R.id.btDecrypt);
    context = this;

    identity = databaseBackend.loadIdentity(contactUUID);

    ownIdentity = databaseBackend.loadOwnIdentity();
    if (identity == null || ownIdentity == null) {
      Log.w(Config.LOGTAG, "No Identity found with this uuid: " + contactUUID);
      return;
    }

    this.setTitle(identity.getUsername());

    messages = databaseBackend.getMessages(identity);

    btEncrypt.setOnClickListener(view -> {
      encryptMessage(false);
    });

    btDecrypt.setOnClickListener(view -> {

      showFileChooser();
    });


    if (!messages.isEmpty()) {
      lbNoMessages.setVisibility(View.GONE);
    }
    messageAdapter = new MessageAdapter(messages, identity, getApplicationContext());
    rvMessageList.setAdapter(messageAdapter);
    rvMessageList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
    rvMessageList.scrollToPosition(messages.size() - 1);

    messageAdapter.setOnClickOnMessage((message, view, position) -> {
      PopupMenu popup = new PopupMenu(context, view);

      popup.inflate(R.menu.message_menu);

      popup.setOnMenuItemClickListener(item -> {
        switch (item.getItemId()) {
          case R.id.message_menu_copy:
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text message", message.getMessage());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, R.string.messageCopied, Toast.LENGTH_SHORT).show();
            return true;
          case R.id.message_menu_delete:
            databaseBackend.deleteMessage(message);
            messages.remove(message);
            messageAdapter.notifyItemRemoved(position);
            changed = true;

            if (messages.isEmpty()) {
              lbNoMessages.setVisibility(View.VISIBLE);
            } else {
              lbNoMessages.setVisibility(View.GONE);
            }
            return true;
        }
        return false;
      });

      popup.show();

    });

  }

  @Override
  protected void onStart() {
    super.onStart();

    Intent intent = getIntent();

    Uri encryprtyptedAudio = intent.getParcelableExtra(ENCRYPTEDAUDIO);
    if (encryprtyptedAudio != null) {
      copyEncryptedAudio(encryprtyptedAudio);
      getIntent().removeExtra(ENCRYPTEDAUDIO);
    }
  }

  private void showFileChooser() {
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
    intent.setType("audio/x-wav");
    intent.addCategory(Intent.CATEGORY_OPENABLE);

    try {
      startActivityForResult(
          Intent.createChooser(intent, "Select an Audiofile to decode"),
          REQUEST_SELECT_AUDIO_FILE);
    } catch (android.content.ActivityNotFoundException ex) {
      Toast.makeText(this, R.string.missingFilemanager,
          Toast.LENGTH_SHORT).show();
    }
  }

  private void decryptMessage(String messageText) {

    // Delete Received and Sended Audio files
    FileBackend fileBackend = new FileBackend(context);
    fileBackend.deleteCachedAudioFiles();

    Log.i(Config.LOGTAG, "To Decrypt Message: " + messageText);

    // iv + body (min. 64 da checksum 32 byte hat und hexcodiert ist) + length + header
    if (messageText.length() < 32 + 64 + 8 + 2 || !messageText.matches("[0-9a-f]+") || messageText.length() % 2 != 0) {
      Toast.makeText(context, R.string.notEncrypted, Toast.LENGTH_SHORT).show();
      return;
    }
    String correctHeader = CryptoHelper.bytesToHex("#".getBytes(StandardCharsets.UTF_8));

    String header = messageText.substring(0, 2);
    int size = CryptoHelper.bytesToInt(CryptoHelper.hexToBytes(messageText.substring(2, 10)));
    byte[] messageIV = CryptoHelper.hexToBytes(messageText.substring(10, 42));
    byte[] encryptedMessage = CryptoHelper.hexToBytes(messageText.substring(42));

    if(!header.equals(correctHeader))
    {
      Toast.makeText(context, R.string.headerDoesNotMatch, Toast.LENGTH_SHORT).show();
      Log.i(Config.LOGTAG, "Header does not match!");
    }

    if(size != messageIV.length + encryptedMessage.length)
    {
      Toast.makeText(context, R.string.sizeDoesNotMatch, Toast.LENGTH_LONG).show();
      Log.i(Config.LOGTAG, "Size does not match!");
    }

    byte[] messageKey = HKDF.getMessageKey(identity.getPrivateKey(), messageIV);
    String decrypted = "";
    try {
      decrypted = AES.decrypt(encryptedMessage, messageKey, messageIV);
    } catch (Exception e) {
      Log.w(Config.LOGTAG, "Could not decrypt Text!", e);
    }
    String decryptedClear = CryptoHelper.hexToString(decrypted);

    try {
      decryptedClear = CryptoHelper.cutStringAtEnd(decryptedClear);
    } catch (Exception e) {
      Log.w(Config.LOGTAG, "Could not cut String!", e);
    }

    if (decryptedClear.length() < 64) {
      Toast.makeText(context, R.string.noChecksumFound, Toast.LENGTH_SHORT).show();
    } else {
      String checksum = decryptedClear.substring(0, 64);
      decryptedClear = decryptedClear.substring(64);
      String ownChecksum = "";
      try {
        ownChecksum = CryptoHelper.bytesToHex(ECKeyGenerator.getFingerprint(decryptedClear.getBytes(StandardCharsets.UTF_8)));

      } catch (NoSuchAlgorithmException e) {
        Log.w(Config.LOGTAG, "Could create fingerprint of message!");
      }
      if (!checksum.equals(ownChecksum)) {
        Log.w(Config.LOGTAG, "Checksum not identical: " + ownChecksum + " " + checksum);
        Toast.makeText(context, R.string.checksumDoesNotMatch, Toast.LENGTH_LONG).show();
      }
    }

            /*Log.i(Config.LOGTAG, "Key: " + CryptoHelper.bytesToHex(messageKey)
                + "\nIV: " + CryptoHelper.bytesToHex(messageIV)
                + "\nMessage: " + CryptoHelper.bytesToHex(encryptedMessage)
                + "\nDecryptedHex: " + decrypted
                + "\nDecrypted: " + decryptedClear);*/

    Message message = new Message(identity, ownIdentity, decryptedClear);
    databaseBackend.storeMessage(message);
    messages.add(message);
    messageAdapter.notifyItemInserted(messages.size() - 1);
    rvMessageList.scrollToPosition(messages.size() - 1);
    txChatbox.setText("");
    changed = true;
    lbNoMessages.setVisibility(View.GONE);
  }


  private void encryptMessage(boolean toHexString) {
    String messageText = txChatbox.getText().toString();

    IV messageIV = new IV(databaseBackend);
    byte[] messageKey = HKDF.getMessageKey(identity.getPrivateKey(), messageIV.getNonce());
    String checksum = "";
    try {
      checksum = CryptoHelper.bytesToHex(ECKeyGenerator.getFingerprint(messageText.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      Log.w(Config.LOGTAG, "Could not Encrypt Text!", e);
      Toast.makeText(this, R.string.couldNotEncryptMessage, Toast.LENGTH_SHORT).show();
      return;
    }


    String encrypted = "";
    try {
      encrypted = AES.encrypt((checksum + messageText).getBytes(StandardCharsets.UTF_8), messageKey, messageIV.getNonce());
    } catch (Exception e) {
      Log.w(Config.LOGTAG, "Could not Encrypt Text!", e);
      Toast.makeText(this, R.string.couldNotEncryptMessage, Toast.LENGTH_SHORT).show();
      return;
    }

            /*Log.i(Config.LOGTAG, "Key: " + CryptoHelper.bytesToHex(messageKey)
                + "\nIV: " + CryptoHelper.bytesToHex(messageIV.getNonce())
                + "\nMessage: " checksum +  messageText
                + "\nMessageHex: " + CryptoHelper.stringtoHex(checksum + messageText )
                + "\nEncrypted: " + encrypted);*/


    String combinedMessage = CryptoHelper.bytesToHex(messageIV.getNonce()) + encrypted;
    combinedMessage = CryptoHelper.bytesToHex("#".getBytes(StandardCharsets.UTF_8)) + CryptoHelper.bytesToHex(CryptoHelper.intToBytes(combinedMessage.length() / 2)) + combinedMessage;

    Message message = new Message(ownIdentity, identity, messageText);
    if(toHexString)
    {
      message = new Message(ownIdentity, identity, combinedMessage);
    }
    databaseBackend.storeMessage(message);
    messages.add(message);
    messageAdapter.notifyItemInserted(messages.size() - 1);
    rvMessageList.scrollToPosition(messages.size() - 1);

    txChatbox.setText("");
    changed = true;
    lbNoMessages.setVisibility(View.GONE);

    Log.i(Config.LOGTAG, "Encrypted Message: " + combinedMessage);
    if(!toHexString) {
      Intent recorder = new Intent(this, RecordingActivity.class);
      recorder.putExtra(RecordingActivity.TOENCRYPT, combinedMessage);
      recorder.putExtra(RecordingActivity.FORIDENTITY, identity.getUuid());
      startActivityForResult(recorder, REQUEST_VOICE_RECORD);
    }
  }

  private void getMessageInAudio(File fileIn) {

    // TODO get Message IN Audio File
    InputStream is = null;
    int headerCount = 0;

    Random secureRandom = PRNG.getPRNG(identity.getPrivateKey());

    String correctHeader = CryptoHelper.bytesToHex("#".getBytes(StandardCharsets.UTF_8));

    byte[] header = new byte[1];
    int headerIndex = 0;
    byte[] toRead = new byte[4];
    int toReadIndex = 0;
    int bitsToRead = 0;
    int tillnext =  secureRandom.nextInt(20) + 1;
    byte[] hiddenMessage = new byte[0];
    int totalBitsRead = 0;

    try {
      is = new FileInputStream(fileIn);
      byte[] buffer = new byte[1024];
      int read;
      boolean stop = false;
      while ((read = is.read(buffer)) > 0 && !stop) {

        for (int i = 0; i < read; i++) {
          if(headerCount < 44)
          {
            headerCount++;
          }
          else
          {
            if(headerIndex < 8) {
              if (i % 2 == 0) {
                tillnext--;
                if (tillnext == 0) {
                  tillnext = secureRandom.nextInt(20) + 1;
                  header[headerIndex/8] = (byte) ((header[headerIndex/8] | ((buffer[i] & 0x01) << (headerIndex % 8))) & 0xFF );
                  headerIndex++;
                  if(headerIndex == 8 )
                  {
                    String readHeader = CryptoHelper.bytesToHex(header);
                    if(!readHeader.equals(correctHeader))
                    {
                      stop = true;
                      break;
                    }
                  }
                }
              }
            }
            else {
              if(toReadIndex < 8*4)
              {
                if (i % 2 == 0) {
                  tillnext--;
                  if (tillnext == 0) {
                    tillnext = secureRandom.nextInt(20) + 1;
                    toRead[toReadIndex/8] = (byte) ((toRead[toReadIndex/8] | ((buffer[i] & 0x01) << (toReadIndex % 8))) & 0xFF );
                    toReadIndex++;
                    if(toReadIndex == 8*4 )
                    {
                      bitsToRead = CryptoHelper.bytesToInt(toRead) * 8;
                      hiddenMessage = new byte[bitsToRead / 8];
                    }
                  }
                }

              }
              else{
                if(bitsToRead ==  totalBitsRead)
                {
                  break;
                }
                if (i % 2 == 0) {
                  tillnext--;
                  if (tillnext == 0) {
                    tillnext = secureRandom.nextInt(20) + 1;
                    hiddenMessage[totalBitsRead/8] = (byte) ((hiddenMessage[totalBitsRead/8] | ((buffer[i] & 0x01) << (totalBitsRead % 8))) & 0xFF);
                    totalBitsRead++;
                  }
                }
              }
            }

          }
        }
      }

    } catch (Exception e) {
      Toast.makeText(context.getApplicationContext(), R.string.notEncrypted, Toast.LENGTH_LONG).show();
      Log.w(Config.LOGTAG, "Could not open file.", e);
      return;
    } finally {
      if(is != null)
      {
        try {
          is.close();
        } catch (IOException e) {
        }
      }
    }

    if(hiddenMessage.length == 0)
    {
      Log.w(Config.LOGTAG, "No Message Found.");
      Toast.makeText(context.getApplicationContext(), R.string.notEncrypted, Toast.LENGTH_LONG).show();
      return;
    }
    decryptMessage(CryptoHelper.bytesToHex(header) + CryptoHelper.bytesToHex(toRead) + CryptoHelper.bytesToHex(hiddenMessage));
  }


  private void shareAudio(Uri uri) {
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    intent.setType("audio/x-wav");
    intent.putExtra(Intent.EXTRA_STREAM, FileBackend.getUriForFile(context, new File(uri.getPath())));
    startActivity(Intent.createChooser(intent, "Share Sound File"));
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putString(CONTACTID, contactUUID);
    super.onSaveInstanceState(savedInstanceState);
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.chat_menu, menu);
    return true;
  }


  private void copyEncryptedAudio(Uri path) {
    Log.d(Config.LOGTAG, "File Uri: " + path.toString());
    // Get the path
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US);
    String filename = "RECEIVED_" + dateFormat.format(new Date()) + ".wav";
    File f = new File(context.getCacheDir() + "/Received/", filename);
    if (f.exists()) {
      Log.d(Config.LOGTAG, "delete old copy of audio " + f.getAbsolutePath());

      f.delete();
    }
    FileBackend fileBackend = new FileBackend(context);
    try {

      fileBackend.copyFileToPrivateStorage(f, path);
    } catch (FileBackend.FileCopyException e) {
      Toast.makeText(context, R.string.canNotOpenFile, Toast.LENGTH_SHORT).show();
      Log.w(Config.LOGTAG, "Could not copy Audio file!", e);
      return;
    }
    getMessageInAudio(f);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case RUNCHATDETAILS:
        if (resultCode == RESULT_OK) {
          identity = databaseBackend.loadIdentity(contactUUID);
          this.setTitle(identity.getUsername());
          changed = true;
          messageAdapter.setNewIdentity(identity);
          messageAdapter.notifyDataSetChanged();
          Log.i(Config.LOGTAG, "Reload changed identity!");
        }
        break;
      case REQUEST_VOICE_RECORD:
        if (resultCode == RESULT_OK) {
          shareAudio(data.getData());
        }
        break;
      case REQUEST_SELECT_AUDIO_FILE:
        if (resultCode == RESULT_OK) {
          // Get the Uri of the selected file
          Uri uri = data.getData();
          copyEncryptedAudio(uri);
        }
        break;
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.chat_menu_contactDetails:
        Intent contactDetailsActivity = new Intent(this, ContactDetailsActivity.class);
        contactDetailsActivity.putExtra(ContactDetailsActivity.CONTACTID, contactUUID);
        startActivityForResult(contactDetailsActivity, RUNCHATDETAILS);
        break;
      case R.id.chat_menu_deleteAllMessages:
        showSureDialog((dialog, which) -> {
          databaseBackend.deleteAllMessagesInChatWith(identity);
          messages.clear();
          messageAdapter.notifyDataSetChanged();
          changed = true;

          lbNoMessages.setVisibility(View.VISIBLE);
        });
        break;
      case R.id.chat_menu_deleteContact:
        showSureDialog((dialog, which) -> {
          databaseBackend.deleteIdentity(identity);
          Intent intent = new Intent();
          setResult(RESULT_OK, intent);
          finish();
        });
        break;
      case R.id.chat_menu_encryptToHexString:
        encryptMessage(true);
        break;
      case R.id.chat_menu_decryptToHexString:
        decryptMessage(txChatbox.getText().toString());
        break;



      case android.R.id.home:
        if (changed) {
          Intent intent = new Intent();
          setResult(RESULT_OK, intent);
        }
        finish();
        break;

      default:
        return false;
    }

    return true;
  }

  private void showSureDialog(DialogInterface.OnClickListener onClickListener) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.DialogAreYouSure);

    builder.setPositiveButton(R.string.DialogYes, onClickListener);

    builder.setNegativeButton(R.string.DialogNo, (dialog, which) -> dialog.cancel());

    builder.show();
  }
}
