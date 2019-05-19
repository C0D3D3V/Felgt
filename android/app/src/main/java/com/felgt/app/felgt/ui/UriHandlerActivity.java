package com.felgt.app.felgt.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.felgt.app.felgt.Config;
import com.felgt.app.felgt.R;
import com.felgt.app.felgt.entities.ExtractedUriInformation;
import com.felgt.app.felgt.entities.Identity;
import com.felgt.app.felgt.persistance.DatabaseBackend;
import com.felgt.app.felgt.utils.FelgtURI;

public class UriHandlerActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_uri_handler);

  }

  @Override
  public void onStart() {
    super.onStart();
    handleIntent(getIntent());
  }

  @Override
  public void onNewIntent(Intent intent) {
    handleIntent(intent);
  }


  private void handleIntent(Intent data) {
    if (data == null || data.getAction() == null) {
      finish();
      return;
    }

    if (data.getAction() == Intent.ACTION_VIEW) {
      handleUri(data.getData());
    }

    finish();
  }

  private void handleUri(Uri uri) {

    ExtractedUriInformation extractIdentity = FelgtURI.parse(uri);
    if (extractIdentity == null) {
      Toast.makeText(this, R.string.invalidUri, Toast.LENGTH_SHORT).show();
      return;
    }
    DatabaseBackend databaseBackend = DatabaseBackend.getInstance(this.getApplicationContext());


    // check if own identity exists
    Identity ownIdentity = databaseBackend.loadOwnIdentity();

    if (ownIdentity != null) {
      // check if contact already exist
      if (databaseBackend.containsIdentity(extractIdentity.username, extractIdentity.publicKey)) {
        Toast.makeText(this, R.string.alreadyInContactList, Toast.LENGTH_SHORT).show();
        Identity oldIdentity = databaseBackend.loadIdentity(extractIdentity.username, extractIdentity.publicKey);
        if (oldIdentity.getUuid() == ownIdentity.getUuid()) {
          Toast.makeText(this, R.string.dontTryToAddYourself, Toast.LENGTH_SHORT).show();
          return;
        }
        startChatWith(oldIdentity);
        return;
      } else {
        // add contact
        try {
          Identity newIdentity = new Identity(extractIdentity.username, extractIdentity.publicKey, ownIdentity.getPrivateKey());

          databaseBackend.storeIdentity(newIdentity);

          startChatWith(newIdentity);
        } catch (Exception e) {
          Log.w(Config.LOGTAG, "Could not create new identity!", e);
          Toast.makeText(this, R.string.couldNotAddContact, Toast.LENGTH_SHORT).show();

        }

      }
    } else {
      Toast.makeText(this, R.string.createFirstAccount, Toast.LENGTH_SHORT).show();

      showWelcomeScreen();
      return;
    }

  }

  private void startChatWith(Identity identity) {
    final Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
    intent.putExtra(ChatActivity.CONTACTID, identity.getUuid());
    startActivity(intent);
  }

  private void showWelcomeScreen() {
    final Intent intent = new Intent(getApplicationContext(), WelcomeActivity.class);
    startActivity(intent);
  }

}
