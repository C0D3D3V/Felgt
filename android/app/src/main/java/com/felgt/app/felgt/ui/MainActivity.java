package com.felgt.app.felgt.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.felgt.app.felgt.Config;
import com.felgt.app.felgt.R;
import com.felgt.app.felgt.entities.Identity;
import com.felgt.app.felgt.persistance.DatabaseBackend;
import com.felgt.app.felgt.ui.adapter.ContactAdapter;

import java.util.List;

public class MainActivity extends AppCompatActivity {

  public static final int RUNCHAT = 90;
  public static final int RUNADDCONTACT = 94;
  private Context context;
  private Identity identity;
  private DatabaseBackend databaseBackend;
  private ContactAdapter contactAdapter;
  private List<Identity> contacts;
  private TextView lbNoConatcs;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    databaseBackend = DatabaseBackend.getInstance(this.getApplicationContext());

    context = this;

    identity = getIdentity();
    if (identity == null) {
      return;
    }


    lbNoConatcs = findViewById(R.id.lbNoContacs);
    FloatingActionButton btAddContact = findViewById(R.id.btAddContact);
    RecyclerView rvContactList = findViewById(R.id.rvContactList);


    contacts = databaseBackend.getContacts();

    if (!contacts.isEmpty()) {
      lbNoConatcs.setVisibility(View.GONE);
    }
    contactAdapter = new ContactAdapter(contacts, getApplicationContext());
    rvContactList.setAdapter(contactAdapter);
    rvContactList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));

    contactAdapter.setOnOpenChatListener(identity -> {
      Intent intent = new Intent(context.getApplicationContext(), ChatActivity.class);
      intent.putExtra(ChatActivity.CONTACTID, identity.getUuid());
      startActivityForResult(intent, RUNCHAT);
    });

    btAddContact.setOnClickListener(view -> {
      Intent intent = new Intent(context.getApplicationContext(), CreateContactActivity.class);
      startActivityForResult(intent, RUNADDCONTACT);
    });

  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      // Username changed
      case RUNCHAT:
        // New Contact
      case RUNADDCONTACT:
        if (resultCode == RESULT_OK) {
          List<Identity> newContacts = databaseBackend.getContacts();
          if(contactAdapter != null){
            contacts.clear();
            contacts.addAll(newContacts);
            contactAdapter.notifyDataSetChanged();

            if (contacts.isEmpty()) {
              lbNoConatcs.setVisibility(View.VISIBLE);
            } else {
              lbNoConatcs.setVisibility(View.GONE);
            }
          }
          Log.i(Config.LOGTAG, "Reload contact list!");
        }
        break;
    }
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_menu, menu);
    return true;
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

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
      case R.id.main_menu_about:
        intent = new Intent(getApplicationContext(), AboutActivity.class);
        startActivity(intent);
        break;

      case android.R.id.home:
        finish();
        break;

      default:
        return false;
    }

    return true;
  }


}
