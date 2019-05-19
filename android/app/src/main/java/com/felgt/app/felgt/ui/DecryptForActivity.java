package com.felgt.app.felgt.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.felgt.app.felgt.Config;
import com.felgt.app.felgt.R;
import com.felgt.app.felgt.entities.Identity;
import com.felgt.app.felgt.persistance.DatabaseBackend;
import com.felgt.app.felgt.ui.adapter.ContactAdapter;

import java.util.List;

public class DecryptForActivity extends AppCompatActivity {

  private Context context;
  private Identity identity;
  private DatabaseBackend databaseBackend;
  private ContactAdapter contactAdapter;
  private List<Identity> contacts;
  private TextView lbNoContacsToDecryptFor;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_decrypt_for);
    setTitle(R.string.activity_decrypt_for);
    databaseBackend = DatabaseBackend.getInstance(this.getApplicationContext());

    context = this;

    identity = getIdentity();
    if (identity == null) {
      return;
    }


    lbNoContacsToDecryptFor = findViewById(R.id.lbNoContacsToDecryptFor);
    RecyclerView rvContactList = findViewById(R.id.rvContactList);


    contacts = databaseBackend.getContacts();

    Intent thisIntent = getIntent();
    if (thisIntent == null) {
      return;
    }

    final String type = thisIntent.getType();
    final String action = thisIntent.getAction();
    if (Intent.ACTION_SEND.equals(action)) {
      final Uri audioUri = thisIntent.getParcelableExtra(Intent.EXTRA_STREAM);
      if (audioUri == null || !type.equals("audio/x-wav")) {
        return;
      }

      if (!contacts.isEmpty()) {
        lbNoContacsToDecryptFor.setVisibility(View.GONE);
        contactAdapter = new ContactAdapter(contacts, getApplicationContext());
        rvContactList.setAdapter(contactAdapter);
        rvContactList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));

        contactAdapter.setOnOpenChatListener(identity -> {
          Intent startChat = new Intent(context.getApplicationContext(), ChatActivity.class);
          startChat.putExtra(ChatActivity.CONTACTID, identity.getUuid());
          startChat.putExtra(ChatActivity.ENCRYPTEDAUDIO, audioUri);
          startActivity(startChat);
          finish();
        });
      }
    }
  }

  private Identity getIdentity() {
    Identity oldIdentity = databaseBackend.loadOwnIdentity();
    if (oldIdentity == null) {
      Log.i(Config.LOGTAG, "Could not retrieve own identity");
      // Create new Identity - this is the first time the user opens the app
      Intent welcomeActivity = new Intent(this, WelcomeActivity.class);
      startActivity(welcomeActivity);
      finish();
    }
    return oldIdentity;
  }


}
