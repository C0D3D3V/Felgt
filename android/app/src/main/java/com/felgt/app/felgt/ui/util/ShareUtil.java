package com.felgt.app.felgt.ui.util;

import android.content.Context;
import android.content.Intent;

import com.felgt.app.felgt.R;

public class ShareUtil {

  private ShareUtil() {
  }

  public static void ShareText(Context context, String text) {
    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
    sharingIntent.setType("text/plain");
    sharingIntent.putExtra(Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.myFelgtContactInfos));
    sharingIntent.putExtra(Intent.EXTRA_TEXT, text);
    context.startActivity(Intent.createChooser(sharingIntent, context.getResources().getString(R.string.share_using)));
  }
}
