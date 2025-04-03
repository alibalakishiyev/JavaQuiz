package com.ali.javaquizbyali;

import static com.ali.quizutility.AboutActivity.MyPREFERENCES;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.ali.systemIn.R;

public class SplashJavaActivity extends AppCompatActivity {

    public static int checked;
    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_java);

        videoView = findViewById(R.id.videoView);
        playVideo();

        SharedPreferences sharedPreferences =getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        int default1;
        switch (Configuration.UI_MODE_NIGHT_MASK & AppCompatDelegate.getDefaultNightMode()){
            case AppCompatDelegate.MODE_NIGHT_NO:
                default1=0;
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                default1=1;
                break;
            default:
                default1=2;
        }

        checked=sharedPreferences.getInt("checked",default1);
        switch (checked){
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case 3:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                break;
        }

    }
    private void playVideo() {
        Uri videoPath = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.java1);
        videoView.setVideoURI(videoPath);
        videoView.setOnCompletionListener(mp -> {
            startActivity(new Intent(SplashJavaActivity.this, JavaMain.class));
            finish();
        });
        videoView.start();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        videoView.stopPlayback();
    }
}