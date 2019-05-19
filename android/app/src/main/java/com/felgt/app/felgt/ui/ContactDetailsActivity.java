package com.felgt.app.felgt.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.felgt.app.felgt.Config;
import com.felgt.app.felgt.R;
import com.felgt.app.felgt.entities.Identity;
import com.felgt.app.felgt.persistance.DatabaseBackend;
import com.felgt.app.felgt.persistance.FileBackend;
import com.felgt.app.felgt.ui.util.AvatarWorkerTask;
import com.felgt.app.felgt.utils.CryptoHelper;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;

public class ContactDetailsActivity extends AppCompatActivity {
  public static final String CONTACTID = "com.felgt.app.contactid";
  private String contactUUID;
  private DatabaseBackend databaseBackend;
  private Identity identity;
  private AvatarWorkerTask avatarLoader;
  private ImageView ivContactPicture;
  private TextView lbFriendFingerprint;
  private EditText txContactName;
  private Switch swTrustFingerprint;
  private String newPicturePath = "";
  private Uri avatarUri;
  private boolean pictureChanged = false;
  private boolean changed = false;
  private Activity context;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_contact_details);
    databaseBackend = DatabaseBackend.getInstance(this);

    Intent intent = getIntent();
    context = this;

    contactUUID = intent.getStringExtra(CONTACTID);
    if (savedInstanceState != null) {
      contactUUID = savedInstanceState.getString(CONTACTID);
    }

    lbFriendFingerprint = findViewById(R.id.lbFriendFingerprint);
    ivContactPicture = findViewById(R.id.ivContactPicture);
    txContactName = findViewById(R.id.txContactName);
    swTrustFingerprint = findViewById(R.id.swTrustFingerprint);
    Button btSaveContactDetails = findViewById(R.id.btSaveContactDetails);


    identity = databaseBackend.loadIdentity(contactUUID);

    if (identity == null) {
      Log.w(Config.LOGTAG, "No Identity found with this uuid: " + contactUUID);
      return;
    }


    avatarLoader = new AvatarWorkerTask(this);

    Resources res = getResources();
    String fingerprint = res.getString(R.string.lbFriendFingerprint, identity.getUsername(), CryptoHelper.prettifyFingerprint(CryptoHelper.bytesToHex(identity.getFingerprint())));
    lbFriendFingerprint.setText(fingerprint);
    txContactName.setText(identity.getUsername());
    swTrustFingerprint.setChecked(identity.isTrusted());

    newPicturePath = identity.getPicturePath();
    if (!identity.getPicturePath().isEmpty()) {
      avatarLoader.displayImage(identity.getPicturePath(), ivContactPicture);
    }

    btSaveContactDetails.setOnClickListener(view -> showSaveDialog());

    ivContactPicture.setOnClickListener(view -> {
      //Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
      //startActivityForResult(pickIntent, IMAGE_PICKER_SELECT);
      CropImage.activity(null).setOutputCompressFormat(Bitmap.CompressFormat.PNG)
          .setAspectRatio(1, 1)
          .setMinCropResultSize(Config.AVATAR_SIZE, Config.AVATAR_SIZE)
          .start(context);
    });
  }


  private void showSaveDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.DialogAreYouSure);

    builder.setPositiveButton(R.string.DialogYes, (dialog, which) -> {
      identity.setPicturePath(newPicturePath);
      identity.setTrusted(swTrustFingerprint.isChecked());
      identity.setUsername(txContactName.getText().toString());

      databaseBackend.storeIdentity(identity);

      if (pictureChanged) {
        try {
          new FileBackend(context).saveAvatar(newPicturePath, avatarUri);
        } catch (Exception e) {
          Log.w(Config.LOGTAG, "Could not set new avatar!", e);
          Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
      }

      Toast.makeText(context, R.string.ContactDetailsSaved, Toast.LENGTH_SHORT).show();

      Resources res = getResources();
      String fingerprint = res.getString(R.string.lbFriendFingerprint, identity.getUsername(), CryptoHelper.prettifyFingerprint(CryptoHelper.bytesToHex(identity.getFingerprint())));
      lbFriendFingerprint.setText(fingerprint);

      changed = true;
    });

    builder.setNegativeButton(R.string.DialogNo, (dialog, which) -> dialog.cancel());

    builder.show();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
      CropImage.ActivityResult result = CropImage.getActivityResult(data);
      if (resultCode == RESULT_OK) {
        this.avatarUri = result.getUri();
        Log.i(Config.LOGTAG, avatarUri.toString());
        loadImageIntoPreview(avatarUri);

        newPicturePath = identity.getUuid() + System.currentTimeMillis();
        pictureChanged = true;
      } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
        Exception error = result.getError();
        if (error != null) {
          Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
        }
      }
    }
  }

  private void loadImageIntoPreview(Uri avatarUri) {
    ivContactPicture.setImageBitmap(new AvatarWorkerTask(context).decodeFile(new File(avatarUri.getPath())));

  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putString(CONTACTID, contactUUID);
    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      if (changed) {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
      }
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

}
