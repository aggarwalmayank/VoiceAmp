package com.mayank.appsaga.voiceamp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.BassBoost;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Microphone extends AppCompatActivity {


    //Audio
    private Button mOn;
    private boolean isOn;
    private TextView tv;
    private boolean isRecording;
    private AudioRecord record;
    private AudioTrack player;
    private AudioManager manager;
    private int recordState, playerState;
    private int minBuffer;

    public static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    //Audio Settings
    private final int source = MediaRecorder.AudioSource.CAMCORDER;
    private final int channel_in = AudioFormat.CHANNEL_IN_MONO;
    private final int channel_out = AudioFormat.CHANNEL_OUT_MONO;
    private final int format = AudioFormat.ENCODING_PCM_16BIT;

    private final static int REQUEST_ENABLE_BT = 1;
    private boolean IS_HEADPHONE_AVAILBLE = false;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_microphone);

        //Reduce latancy
        if (ContextCompat.checkSelfPermission(Microphone.this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(Microphone.this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1234);

        }
        setVolumeControlStream(AudioManager.MODE_IN_COMMUNICATION);
        setVolumeControlStream(AudioManager.MODE_IN_COMMUNICATION);


        mOn = (Button) findViewById(R.id.button);
        tv=findViewById(R.id.txt);
        tv.setText("Off");
        isOn = false;
        isRecording = false;

        manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        manager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        initializePlayerAndStartRecording();
//        Check for headset availability
//        while(!IS_HEADPHONE_AVAILBLE){
        AudioDeviceInfo[] audioDevices = manager.getDevices(AudioManager.GET_DEVICES_ALL);
        for (AudioDeviceInfo deviceInfo : audioDevices) {
            if (deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET || deviceInfo.getType() == AudioDeviceInfo.TYPE_USB_HEADSET) {
                IS_HEADPHONE_AVAILBLE = true;
            }
        }
        if (!IS_HEADPHONE_AVAILBLE) {
            new AlertDialog.Builder(this)
                    .setMessage("Connect HeadPhone or Bluetooth Device")
                    .setPositiveButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    Intent intent = getIntent();
                                    finish();
                                    startActivity(intent);
                                }
                            }).show();
       // }
            // get delete_audio_dialog.xml view

//            LayoutInflater layoutInflater = LayoutInflater.from(Microphone.this);
////            View promptView = layoutInflater.inflate(R.layout.insert_headphone_dialog, null);
//            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Microphone.this);
////            alertDialogBuilder.setView(promptView);
//
//            // setup a dialog window
//            alertDialogBuilder.setCancelable(false)
//                    .setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                            startActivity(new Intent(getIntent()));
//                        }
//                    })
//                    .setNegativeButton("Cancel",
//                            new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int id) {
//                                    startActivity(new Intent(Microphone.this,MainActivity.class));
//                                    dialog.cancel();
//                                }
//                            });
//
//            // create an alert dialog
//            AlertDialog alert = alertDialogBuilder.create();
//            alert.show();
        }


    }

    public void initAudio() {
        //Tests all sample rates before selecting one that works
        int sample_rate = getSampleRate();
        minBuffer = AudioRecord.getMinBufferSize(sample_rate, channel_in, format);

        record = new AudioRecord(source, sample_rate, channel_in, format, minBuffer);
        recordState = record.getState();
        int id = record.getAudioSessionId();
        Log.d("Record", "ID: " + id);
        playerState = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            player = new AudioTrack(
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
                    new AudioFormat.Builder().setEncoding(format).setSampleRate(sample_rate).setChannelMask(channel_out).build(),
                    minBuffer,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE);
            playerState = player.getState();
            // Formatting Audio
            if (AcousticEchoCanceler.isAvailable()) {
                AcousticEchoCanceler echo = AcousticEchoCanceler.create(id);
                echo.setEnabled(true);
                Log.d("Echo", "Off");
            }
            if (NoiseSuppressor.isAvailable()) {
                NoiseSuppressor noise = NoiseSuppressor.create(id);
                noise.setEnabled(true);
                Log.d("Noise", "Off");
            }
            if (AutomaticGainControl.isAvailable()) {
                AutomaticGainControl gain = AutomaticGainControl.create(id);
                gain.setEnabled(false);
                Log.d("Gain", "Off");
            }
            BassBoost base = new BassBoost(1, player.getAudioSessionId());
            base.setStrength((short) 1000);
        }
    }

    public void startAudio() {
        int read = 0, write = 0;
        if (recordState == AudioRecord.STATE_INITIALIZED && playerState == AudioTrack.STATE_INITIALIZED) {
            record.startRecording();
            player.play();
            isRecording = true;
            Log.d("Record", "Recording...");
        }
        while (isRecording) {
            short[] audioData = new short[minBuffer];
            if (record != null)
                read = record.read(audioData, 0, minBuffer);
            else
                break;
            Log.d("Record", "Read: " + read);
            if (player != null)
                write = player.write(audioData, 0, read);
            else
                break;
            Log.d("Record", "Write: " + write);
        }
    }

    public void endAudio() {
        if (record != null) {
            if (record.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
                record.stop();
            isRecording = false;
            Log.d("Record", "Stopping...");
        }
        if (player != null) {
            if (player.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
                player.stop();
            isRecording = false;
            Log.d("Player", "Stopping...");
        }
    }

    public int getSampleRate() {
        //Find a sample rate that works with the device
        for (int rate : new int[]{8000, 11025, 16000, 22050, 44100, 48000}) {
            int buffer = AudioRecord.getMinBufferSize(rate, channel_in, format);
            if (buffer > 0)
                return rate;
        }
        return -1;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initializePlayerAndStartRecording();

                } else {
                    Log.d("TAG", "permission denied by user");
                }
                return;
            }
        }
    }

    public void initializePlayerAndStartRecording() {
        initAudio();

        mOn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mOn.getBackground().setColorFilter(getResources().getColor(!isOn ? R.color.colorPrimary : R.color.colorPrimaryDark), PorterDuff.Mode.SRC_ATOP);
                isOn = !isOn;
                if (isOn) {
                    tv.setText("On");
                    (new Thread() {
                        @Override
                        public void run() {
                            startAudio();
                        }
                    }).start();
                } else {
                    tv.setText("Off");
                    endAudio();
                }
            }
        });
    }
}
