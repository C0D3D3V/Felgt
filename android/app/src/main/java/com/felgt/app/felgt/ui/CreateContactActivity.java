package com.felgt.app.felgt.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.felgt.app.felgt.Config;
import com.felgt.app.felgt.R;
import com.felgt.app.felgt.entities.Identity;
import com.felgt.app.felgt.persistance.DatabaseBackend;
import com.felgt.app.felgt.services.BarcodeProvider;
import com.felgt.app.felgt.ui.util.AvatarWorkerTask;
import com.felgt.app.felgt.ui.util.ShareUtil;
import com.felgt.app.felgt.utils.CryptoHelper;

public class CreateContactActivity extends AppCompatActivity {
  DatabaseBackend databaseBackend;
  Identity ownIdentity;
  Activity context;
  ImageView ivQrCode;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_create_contact);

    LinearLayout btScanContact = findViewById(R.id.btScanContact);
    LinearLayout btShareContact = findViewById(R.id.btShareContact);
    TextView lbYourFingerprint = findViewById(R.id.lbYourFingerprint);
    ivQrCode = findViewById(R.id.ivQrCode);

    context = this;
    databaseBackend = DatabaseBackend.getInstance(getApplicationContext());

    ownIdentity = databaseBackend.loadOwnIdentity();
    Resources res = getResources();
    String fingerprint = res.getString(R.string.lbYourFingerprintExpand, CryptoHelper.prettifyFingerprint(CryptoHelper.bytesToHex(ownIdentity.getFingerprint())));
    lbYourFingerprint.setText(fingerprint);

    new LoadQRCode().execute();

    btScanContact.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        ScanActivity.scan(context);
      }
    });

    btShareContact.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        try {
          ShareUtil.ShareText(context, ownIdentity.getShareableUri());
        } catch (Exception e) {
          Toast.makeText(context.getApplicationContext(), R.string.couldNotShareOwnContact,
              Toast.LENGTH_SHORT).show();
          Log.i(Config.LOGTAG, "Could not share own text contact!", e);

        }
      }
    });

  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, requestCode, intent);
    if (requestCode == ScanActivity.REQUEST_SCAN_QR_CODE && resultCode == RESULT_OK) {
      String result = intent.getStringExtra(ScanActivity.INTENT_EXTRA_RESULT);
      if (result != null && !"".equals(result)) {
      }
      {
        parseUri(result);
      }

    }
  }

  private void parseUri(String string) {
    Intent intent = new Intent(getApplicationContext(), UriHandlerActivity.class);
    intent.setData(Uri.parse(string));
    intent.setAction(Intent.ACTION_VIEW);
    startActivity(intent);
    setResult(Activity.RESULT_OK);
    finish();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.create_contact_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.create_contact_menu_addViewText:
        showTextInputDialog();
        break;

      case android.R.id.home:
        finish();
        break;

      default:
        return false;
    }

    return true;
  }

  private void showTextInputDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.ContactInoutDialogTitle);

    final EditText input = new EditText(this);

    input.setInputType(InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

    builder.setView(input);

    builder.setPositiveButton(R.string.DialogAddContact, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        parseUri(input.getText().toString());
      }
    });

    builder.setNegativeButton(R.string.DialogCancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.cancel();
      }
    });

    builder.show();
  }

  private class LoadQRCode extends AsyncTask<Integer, Integer, String> {
    protected String doInBackground(Integer... nothing) {
      String qrBitmap = null;
      try {
        qrBitmap = BarcodeProvider.getCached2dBarcodeBitmap(context, ownIdentity.getShareableUri(), 1024).getName();
      } catch (Exception e) {
        Log.w(Config.LOGTAG, "Could not generate QR Code!", e);
      }
      return qrBitmap;
    }

    protected void onProgressUpdate(Integer... progress) {
    }

    protected void onPostExecute(String result) {
      if (result == null) {
        Toast.makeText(context.getApplicationContext(), R.string.couldNotGenerateQRCode,
            Toast.LENGTH_SHORT).show();
      } else {
        ivQrCode.setBackgroundColor(Color.WHITE);
        new AvatarWorkerTask(context).displayImage(result, ivQrCode);

      }
    }
  }

}
