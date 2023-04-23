package com.example.thirdeye;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private TextToSpeech textToSpeech;
    private final Handler handler = new Handler();
    private int progressStatus;
    private int progress;
    private int cdis;
    private int mode;
    private int dis;
    private int view;
    private boolean ready;
    private boolean internetConnected;
    private float DegreeStart = 0f;
    private SensorManager SensorManage;
    private Vibrator vibrator;
    private ImageView compass_icon, thirdeye_icon;
    private TextView textView, tv_distance;
    private ProgressBar progressBar;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        internetConnected = (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED || connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED);

        if(!internetConnected) { showInternetRequireDialog(); }

        SensorManage = (SensorManager) getSystemService(SENSOR_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        LinearLayout viewLayout = findViewById(R.id.viewLayout);
        compass_icon = findViewById(R.id.compass_icon);
        thirdeye_icon = findViewById(R.id.thirdeye_icon);

        progressBar = findViewById(R.id.progressBar);
        ImageButton btn1 = findViewById(R.id.btn1);
        ImageButton btn2 = findViewById(R.id.btn2);
        textView = findViewById(R.id.tv_mode);
        tv_distance = findViewById(R.id.tv_view);
        textView.setText("Choose a mode");

        view=1;
        mode=0;

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference Dist = database.getReference("Distance");

        //Vibration modes
        long[] activeVibration = new long[] {0, 500};
        long[] greenVibration = new long[] {0, 200, 100, 200};
        long[] yellowVibration = new long[] {0, 300, 200, 300, 200, 300, 200, 300, 200, 300};
        long[] redVibration = new long[] {0, 400, 100, 400, 100, 400, 100, 400, 100, 400, 200, 1000};

        Dist.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer v = snapshot.getValue(Integer.class);
                dis = v != null ? v : 0;

                if(view == 1) { tv_distance.setText(dis + "cm"); }

                //DISTANCE BAR
                progress = progressBar.getProgress();
                progressStatus = progressBar.getProgress();
                new Thread(() -> {
                    int diff;
                    if (cdis < dis) {
                        diff = dis - cdis;
                        for (int i = progress; i >= progress - diff; i--) {
                            handler.post(() -> {
                                progressStatus = progressStatus - 1;
                                progressBar.setProgress(progressStatus);
                            });
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        diff = cdis - dis;
                        for (int i = progress; i <= progress + diff; i++) {
                            handler.post(() -> {
                                progressStatus = progressStatus + 1;
                                progressBar.setProgress(progressStatus);
                            });
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    cdis = dis;
                }).start();

                //APP MODES
                switch (mode) {
                    //Vibration mode
                    case 1:
                        switch (dis) {
                            case 200: createVibration(greenVibration); break;
                            case 150: createVibration(yellowVibration); break;
                            case 50: createVibration(redVibration); break;
                        }
                        break;

                    //Speech mode
                    case 2:
                        switch (dis) {
                            case 200: speakOut("200cm"); break;
                            case 150: speakOut("150cm"); break;
                            case 50: speakOut("50cm"); break;
                        }
                        break;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "ERROR", Toast.LENGTH_LONG).show();
            }
        });

        viewLayout.setOnClickListener(v -> {
            internetConnected = (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED || connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED);
            if(!internetConnected) { showInternetRequireDialog(); }

            switch (view) {
                case 1:
                    compass_icon.requestLayout();
                    thirdeye_icon.requestLayout();
                    thirdeye_icon.getLayoutParams().width = 10;
                    compass_icon.getLayoutParams().width = 150;
                    view = 2;
                    break;
                case 2:
                    compass_icon.requestLayout();
                    thirdeye_icon.requestLayout();
                    thirdeye_icon.getLayoutParams().width = 250;
                    compass_icon.getLayoutParams().width = 10;
                    view = 1;
                    break;
            }
        });

        tv_distance.setOnClickListener(v -> speakOut(tv_distance.getText().toString()));

        btn1.setOnClickListener(v -> {
            textView.setText("Vibration mode");
            mode = 1;
            createVibration(activeVibration);
        });

        btn2.setOnClickListener(v -> {
            textView.setText("Speech mode");
            mode = 2;
            speakOut("Speech mode is on");
        });

        textToSpeech = new TextToSpeech(getApplicationContext(), status -> {
            Log.e("TTS", "TextToSpeech.OnInitListener.onInit...");
            printOutSupportedLanguages();
            setTextToSpeechLanguage();
        });
    }
    private int[] createAmplitude(long[] vib) {
        int vibLength = vib.length;
        int[] amp = new int[vibLength];
        for(int i = 0; i < vibLength; i++) {
            if(i%2!=0 ) { amp[i] = 255; }
            else { amp[i] = 0; }
            Log.e("AMP", "AMP["+i+"]= "+amp[i]);
        }
        return amp;
    }

    private void createVibration(long[] mVibratePattern) {
        int[] mAmplitudes = createAmplitude(mVibratePattern);
        VibrationEffect effect = VibrationEffect.createWaveform(mVibratePattern, mAmplitudes, -1);
        vibrator.vibrate(effect);
    }

    @Override
    public void onPause() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        } super.onPause();
        SensorManage.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SensorManage.registerListener(this, SensorManage.getDefaultSensor(Sensor.TYPE_ORIENTATION),SensorManager.SENSOR_DELAY_GAME);
    }
    private void printOutSupportedLanguages()  {
        // Supported Languages
        Set<Locale> supportedLanguages = textToSpeech.getAvailableLanguages();
        if(supportedLanguages!= null) {
            for (Locale lang : supportedLanguages) {
                Log.e("TTS", "Supported Language: " + lang);
            }
        }
    }

    private void setTextToSpeechLanguage() {
        int result = textToSpeech.setLanguage(Locale.ENGLISH);
        if (result == TextToSpeech.LANG_MISSING_DATA) {
            this.ready = false;
            Toast.makeText(this, "Missing language data", Toast.LENGTH_SHORT).show();
        } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
            this.ready = false;
            Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show();
        } else {
            this.ready = true;
        }
    }

    private void speakOut(String toSpeak) {
        if (!ready) {
            Toast.makeText(this, "Text to Speech not ready", Toast.LENGTH_LONG).show();
            return;
        }
        String utteranceId = UUID.randomUUID().toString();
        textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }
    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent event) {
        String[] directions = new String[] {"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest", "north"};
        int degree = Math.round(event.values[0]);
        int index = (degree + 23) / 45;

        if(view == 2) { tv_distance.setText(directions[index]); }

        RotateAnimation ra = new RotateAnimation(
                DegreeStart,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setFillAfter(true);
        ra.setDuration(1000);
        compass_icon.startAnimation(ra);
        DegreeStart = -degree;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void showInternetRequireDialog() {
        Dialog internetRequireDialog = new Dialog(MainActivity.this);
        internetRequireDialog.setContentView(R.layout.internet_require_dialog);
        internetRequireDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.bg_dialog));
        internetRequireDialog.setCancelable(false);
        internetRequireDialog.setCanceledOnTouchOutside(false);

        Button internetRequireBtn = internetRequireDialog.findViewById(R.id.btn_internetRequire);
        internetRequireBtn.setOnClickListener(v-> System.exit(0));
        internetRequireDialog.show();
    }
}