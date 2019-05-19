/*
 * Copyright 2016 Kevin Mark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * --
 * An example of how to read in raw PCM data from Android's AudioRecord API (microphone input, for
 * instance) and output it to a valid WAV file. Tested on API 21/23 on Android and API 23 on
 * Android Wear (modified activity) where AudioRecord is the only available audio recording API.
 * MediaRecorder doesn't work. Compiles against min API 15 and probably even earlier.
 *
 * Many thanks to Craig Stuart Sapp for his invaluable WAV specification:
 * http://soundfile.sapp.org/doc/WaveFormat/
 */

package com.felgt.app.felgt.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.felgt.app.felgt.Config;
import com.felgt.app.felgt.R;
import com.felgt.app.felgt.entities.Identity;
import com.felgt.app.felgt.persistance.DatabaseBackend;
import com.felgt.app.felgt.utils.CryptoHelper;
import com.felgt.app.felgt.utils.PRNG;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class RecordingActivity extends Activity implements View.OnClickListener {

  public static final int RequestPermission = 96;
  public static final String FORIDENTITY = "com.felgt.app.foridentity";
  public static final String TOENCRYPT = "com.felgt.app.toencrypt";

  private TextView mTimerTextView;
  private Button mCancelButton;
  private Button mStopButton;

  private RecordWaveTask recordTask = null;
  private long mStartTime = 0;
  private Context context;
  private boolean shared = false;
  private Identity identity;
  private String toEncrypt;
  private Random secureRandom;

  private Handler mHandler = new Handler();
  private Runnable mTickExecutor = new Runnable() {
    @Override
    public void run() {
      tick();
      mHandler.postDelayed(mTickExecutor, 100);
    }
  };

  private File mOutputFile;

  private static File generateOutputFilename(Context context) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US);
    String filename = "RECORDING_" + dateFormat.format(new Date()) + ".wav";
    return new File(
        //Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + context.getString(R.string.app_name) + "/Media"
        context.getFilesDir().getAbsolutePath()
            + "/Recordings/", filename);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_recording);
    this.mTimerTextView = this.findViewById(R.id.timer);
    this.mCancelButton = this.findViewById(R.id.cancel_button);
    this.mCancelButton.setOnClickListener(this);
    this.mStopButton = this.findViewById(R.id.share_button);
    this.mStopButton.setOnClickListener(this);
    this.setFinishOnTouchOutside(false);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    context = this;

    Intent thisItent = getIntent();

    DatabaseBackend databaseBackend = DatabaseBackend.getInstance(context.getApplicationContext());

    String contactUUID = thisItent.getStringExtra(FORIDENTITY);
    identity = databaseBackend.loadIdentity(contactUUID);
    toEncrypt = thisItent.getStringExtra(TOENCRYPT);

    secureRandom = PRNG.getPRNG(identity.getPrivateKey());

    recordTask = new RecordWaveTask(this, mStopButton, toEncrypt, secureRandom);
  }

  @Override
  protected void onStart() {
    super.onStart();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (this.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        // || this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
      ) {
        requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO
            //   , Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, RequestPermission);
        return;
      }
    }

    if (!startRecording()) {
      mStopButton.setEnabled(false);
      Toast.makeText(this, R.string.unableToStartRecording, Toast.LENGTH_SHORT).show();
    }

  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == RequestPermission) {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        if (!startRecording()) {
          mStopButton.setEnabled(false);
          Toast.makeText(this.getApplicationContext(), R.string.unableToStartRecording, Toast.LENGTH_SHORT).show();

        }

      } else {
        Toast.makeText(context.getApplicationContext(), R.string.noPermissionToRecord, Toast.LENGTH_LONG).show();

      }
    }
  }


  @Override
  protected void onStop() {
    super.onStop();
    if (!shared) {
      mHandler.removeCallbacks(mTickExecutor);
      stopRecording(false);
    }
  }

  private boolean startRecording() {
    setupOutputFile();

    launchTask();
    try {
      mStartTime = SystemClock.elapsedRealtime();
      mHandler.postDelayed(mTickExecutor, 100);
      Log.d("Voice Recorder", "started recording to " + mOutputFile.getAbsolutePath());
      return true;
    } catch (Exception e) {
      Log.e("Voice Recorder", "prepare() failed " + e.getMessage());
      return false;
    }
  }

  private void launchTask() {
    switch (recordTask.getStatus()) {
      case RUNNING:
        Toast.makeText(this, "Task already running...", Toast.LENGTH_SHORT).show();
        return;
      case FINISHED:
        recordTask = new RecordWaveTask(this, mStopButton, toEncrypt, secureRandom);
        break;
      case PENDING:
        if (recordTask.isCancelled()) {
          recordTask = new RecordWaveTask(this, mStopButton, toEncrypt, secureRandom);
        }
    }
    recordTask.execute(mOutputFile);
  }

  protected void stopRecording(boolean saveFile) {
    try {
      if (!recordTask.isCancelled() && recordTask.getStatus() == AsyncTask.Status.RUNNING) {
        recordTask.cancel(false);
      }
    } catch (Exception e) {
      if (saveFile) {
        Toast.makeText(this, R.string.unableToSaveRecording, Toast.LENGTH_SHORT).show();
      }
    } finally {
      mStartTime = 0;
    }
    if (!saveFile && mOutputFile != null) {
      if (mOutputFile.delete()) {
        Log.d(Config.LOGTAG, "deleted canceled recording");
      }
    }
  }

  private void setupOutputFile() {
    mOutputFile = generateOutputFilename(this);
    File parentDirectory = mOutputFile.getParentFile();
    if (parentDirectory.mkdirs()) {
      Log.d(Config.LOGTAG, "created " + parentDirectory.getAbsolutePath());
    }
  }


  private void tick() {
    long time = (mStartTime < 0) ? 0 : (SystemClock.elapsedRealtime() - mStartTime);
    int minutes = (int) (time / 60000);
    int seconds = (int) (time / 1000) % 60;
    int milliseconds = (int) (time / 100) % 10;
    mTimerTextView.setText(minutes + ":" + (seconds < 10 ? "0" + seconds : seconds) + "." + milliseconds);
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.cancel_button:
        mHandler.removeCallbacks(mTickExecutor);
        stopRecording(false);
        setResult(RESULT_CANCELED);
        finish();
        break;
      case R.id.share_button:
        shared = true;
        stopRecording(true);
        mStopButton.setEnabled(false);
        mHandler.removeCallbacks(mTickExecutor);
        Uri uri = Uri.fromFile(mOutputFile);
        setResult(Activity.RESULT_OK, new Intent().setData(uri));
        finish();
        break;
    }
  }

  private static class RecordWaveTask extends AsyncTask<File, Void, Object[]> {

    // Configure me!
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int SAMPLE_RATE = 8000;// 44100; // Hz
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
    //

    private static final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);

    private Context ctx;
    private Button stopButton;
    private byte[] toEncrypt;
    private Random secureRandom;


    private RecordWaveTask(Context ctx, Button stopButton, String toEncrypt, Random secureRandom) {
      this.toEncrypt = CryptoHelper.hexToBytes(toEncrypt);
      this.secureRandom = secureRandom;
      setContext(ctx);
      this.stopButton = stopButton;
    }

    /**
     * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
     * Two size fields are left empty/null since we do not yet know the final stream size
     *
     * @param out         The stream to write the header to
     * @param channelMask An AudioFormat.CHANNEL_* mask
     * @param sampleRate  The sample rate in hertz
     * @param encoding    An AudioFormat.ENCODING_PCM_* value
     * @throws IOException
     */
    private static void writeWavHeader(OutputStream out, int channelMask, int sampleRate, int encoding) throws IOException {
      short channels;
      switch (channelMask) {
        case AudioFormat.CHANNEL_IN_MONO:
          channels = 1;
          break;
        case AudioFormat.CHANNEL_IN_STEREO:
          channels = 2;
          break;
        default:
          throw new IllegalArgumentException("Unacceptable channel mask");
      }

      short bitDepth;
      switch (encoding) {
        case AudioFormat.ENCODING_PCM_8BIT:
          bitDepth = 8;
          break;
        case AudioFormat.ENCODING_PCM_16BIT:
          bitDepth = 16;
          break;
        case AudioFormat.ENCODING_PCM_FLOAT:
          bitDepth = 32;
          break;
        default:
          throw new IllegalArgumentException("Unacceptable encoding");
      }

      writeWavHeader(out, channels, sampleRate, bitDepth);
    }

    /**
     * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
     * Two size fields are left empty/null since we do not yet know the final stream size
     *
     * @param out        The stream to write the header to
     * @param channels   The number of channels
     * @param sampleRate The sample rate in hertz
     * @param bitDepth   The bit depth
     * @throws IOException
     */
    private static void writeWavHeader(OutputStream out, short channels, int sampleRate, short bitDepth) throws IOException {
      // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
      byte[] littleBytes = ByteBuffer
          .allocate(14)
          .order(ByteOrder.LITTLE_ENDIAN)
          .putShort(channels)
          .putInt(sampleRate)
          .putInt(sampleRate * channels * (bitDepth / 8))
          .putShort((short) (channels * (bitDepth / 8)))
          .putShort(bitDepth)
          .array();

      // Not necessarily the best, but it's very easy to visualize this way
      out.write(new byte[] {
          // RIFF header
          'R', 'I', 'F', 'F', // ChunkID
          0, 0, 0, 0, // ChunkSize (must be updated later)
          'W', 'A', 'V', 'E', // Format
          // fmt subchunk
          'f', 'm', 't', ' ', // Subchunk1ID
          16, 0, 0, 0, // Subchunk1Size
          1, 0, // AudioFormat
          littleBytes[0], littleBytes[1], // NumChannels
          littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
          littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
          littleBytes[10], littleBytes[11], // BlockAlign
          littleBytes[12], littleBytes[13], // BitsPerSample
          // data subchunk
          'd', 'a', 't', 'a', // Subchunk2ID
          0, 0, 0, 0, // Subchunk2Size (must be updated later)
      });
    }

    /**
     * Updates the given wav file's header to include the final chunk sizes
     *
     * @param wav The wav file to update
     * @throws IOException
     */
    private static void updateWavHeader(File wav) throws IOException {
      byte[] sizes = ByteBuffer
          .allocate(8)
          .order(ByteOrder.LITTLE_ENDIAN)
          // There are probably a bunch of different/better ways to calculate
          // these two given your circumstances. Cast should be safe since if the WAV is
          // > 4 GB we've already made a terrible mistake.
          .putInt((int) (wav.length() - 8)) // ChunkSize
          .putInt((int) (wav.length() - 44)) // Subchunk2Size
          .array();

      RandomAccessFile accessWave = null;
      //noinspection CaughtExceptionImmediatelyRethrown
      try {
        accessWave = new RandomAccessFile(wav, "rw");
        // ChunkSize
        accessWave.seek(4);
        accessWave.write(sizes, 0, 4);

        // Subchunk2Size
        accessWave.seek(40);
        accessWave.write(sizes, 4, 4);
      } catch (IOException ex) {
        // Rethrow but we still close accessWave in our finally
        throw ex;
      } finally {
        if (accessWave != null) {
          try {
            accessWave.close();
          } catch (IOException ex) {
            //
          }
        }
      }
    }

    private void setContext(Context ctx) {
      this.ctx = ctx;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
      super.onProgressUpdate(values);

      Log.i(Config.LOGTAG, "Finished!");
      stopButton.setTextColor(Color.parseColor("#ff388E3C"));
      stopButton.setEnabled(true);
    }

    /**
     * Opens up the given file, writes the header, and keeps filling it with raw PCM bytes from
     * AudioRecord until it reaches 4GB or is stopped by the user. It then goes back and updates
     * the WAV header to include the proper final chunk sizes.
     *
     * @param files Index 0 should be the file to write to
     * @return Either an Exception (error) or two longs, the filesize, elapsed time in ms (success)
     */
    @Override
    protected Object[] doInBackground(File... files) {
      AudioRecord audioRecord = null;
      FileOutputStream wavOut = null;
      long startTime = 0;
      long endTime = 0;

      // Stegano!
      int tillnext = secureRandom.nextInt(20) + 1;
      int indextoEncrypt = 0;
      boolean pubished = false;
      long total = 0;

      long summWithout = 0;
      long summWith = 0;

      try {
        // Open our two resources
        audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE);
        wavOut = new FileOutputStream(files[0]);

        // Write out the wav file header
        writeWavHeader(wavOut, CHANNEL_MASK, SAMPLE_RATE, ENCODING);

        // Avoiding loop allocations
        byte[] buffer = new byte[BUFFER_SIZE];
        boolean run = true;
        int read;

        // Let's go
        startTime = SystemClock.elapsedRealtime();
        audioRecord.startRecording();
        while (run && !isCancelled()) {
          read = audioRecord.read(buffer, 0, buffer.length);

          for (int i = 0; i < read && total <= 4294967295L; i++, total++) {
            if (toEncrypt.length > indextoEncrypt / 8) {
              if (i % 2 == 0) {
                tillnext--;
                if (tillnext == 0) {
                  tillnext = secureRandom.nextInt(20) + 1;
                  //Log.i(Config.LOGTAG, indextoEncrypt + " Byte Vorher: " + buffer[i]);
                  summWithout += (buffer[i] & 0x01) & 0xFF;

                  buffer[i] = (byte) (((buffer[i] & 0xFE) | ((toEncrypt[indextoEncrypt / 8] >> (indextoEncrypt % 8)) & 0x01)) & 0xFF);

                  summWith += (buffer[i] & 0x01) & 0xFF;
                  //Log.i(Config.LOGTAG, indextoEncrypt + " Byte Hinterher: " + buffer[i]);
                  indextoEncrypt++;
                }
                else
                {
                  summWithout += (buffer[i] & 0x01) & 0xFF;
                  summWith += (buffer[i] & 0x01) & 0xFF;
                }
              }

            } else if (!pubished) {
              pubished = true;
              publishProgress();
            }

            wavOut.write(buffer[i]);
          }
        }
      } catch (IOException ex) {
        return new Object[] {ex};
      } finally {
        if (audioRecord != null) {
          try {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
              audioRecord.stop();
              endTime = SystemClock.elapsedRealtime();
            }
          } catch (IllegalStateException ex) {
            //
          }
          if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.release();
          }
        }
        if (wavOut != null) {
          try {
            wavOut.close();
          } catch (IOException ex) {
            //
          }
        }
      }

      try {
        // This is not put in the try/catch/finally above since it needs to run
        // after we close the FileOutputStream
        updateWavHeader(files[0]);
      } catch (IOException ex) {
        return new Object[] {ex};
      }

      return new Object[] {files[0].length(), endTime - startTime, indextoEncrypt, pubished, summWith ,summWithout };
    }

    @Override
    protected void onCancelled(Object[] results) {
      // Handling cancellations and successful runs in the same way
      onPostExecute(results);
    }

    @Override
    protected void onPostExecute(Object[] results) {
      Throwable throwable = null;
      if (results[0] instanceof Throwable) {
        // Error
        throwable = (Throwable) results[0];
        Log.e(RecordWaveTask.class.getSimpleName(), throwable.getMessage(), throwable);
      }

      // If we're attached to an activity
      if (ctx != null) {
        if (throwable == null) {
          // Display final recording stats
          double size = (long) results[0] / 1000000.00;
          long time = (long) results[1] / 1000;
          int bitsEncoded = (int) results[2];
          boolean done = (boolean) results[3];
          long sumWith = (long) results[4];
          long sumWithout = (long) results[5];
          Log.i(Config.LOGTAG, "sumWith: " + sumWith);
          Log.i(Config.LOGTAG, "sumWithout: " + sumWithout);
          Toast.makeText(ctx, String.format(Locale.getDefault(), "%.2f MB / %d seconds / %d Bits Encoded / Finished Successfully %b",
              size, time, bitsEncoded, done), Toast.LENGTH_LONG).show();
        } else {
          // Error
          Toast.makeText(ctx, throwable.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
      }
    }
  }

}
