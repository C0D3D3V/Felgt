package com.felgt.app.felgt.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.felgt.app.felgt.R;
import com.felgt.app.felgt.entities.Identity;
import com.felgt.app.felgt.persistance.DatabaseBackend;

import java.security.NoSuchAlgorithmException;

public class WelcomeActivity extends AppCompatActivity {

  private Context context;
  private DatabaseBackend databaseBackend;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_welcome);

    context = this;
    databaseBackend = DatabaseBackend.getInstance(this.getApplicationContext());

    final EditText txUsername = findViewById(R.id.txUsername);
    final Button btnSetIdentity = findViewById(R.id.btnSetIdentity);
    final ProgressBar pbWaitKeys = findViewById(R.id.pbWaitKeys);

    btnSetIdentity.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (txUsername.getText().toString().length() > 0) {
          pbWaitKeys.setVisibility(View.VISIBLE);
          try {
            Identity identity = new Identity(txUsername.getText().toString(), true);
            databaseBackend.storeIdentity(identity);


          } catch (NoSuchAlgorithmException e) {
            Toast.makeText(context.getApplicationContext(), R.string.CouldNotCreateKeyPair,
                Toast.LENGTH_SHORT).show();
            pbWaitKeys.setVisibility(View.GONE);
            return;
          }

          Intent mainActivity = new Intent(context.getApplicationContext(),
              MainActivity.class);
          startActivity(mainActivity);
          finish();
        } else {
          Toast.makeText(context.getApplicationContext(), R.string.youNeedToSetAUsername,
              Toast.LENGTH_SHORT).show();
        }
      }
    });
  }


}
