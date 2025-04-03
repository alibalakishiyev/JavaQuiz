package com.ali;

import static com.ali.quizutility.AboutActivity.MyPREFERENCES;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.ali.autorized.Login;
import com.ali.autorized.Register;
import com.ali.systemIn.R;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    public static int checked;
    private FirebaseAuth fAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        fAuth = FirebaseAuth.getInstance();


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

        Thread mSplashThread;
        mSplashThread=new Thread(){
            @Override public void run(){
                try{
                    synchronized (this){
                        wait(2000);
                    }
                }catch (InterruptedException ignored){

                }finally {

                    if (fAuth.getCurrentUser() != null) {
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                    }else {
                        startActivity(new Intent(getApplicationContext(), Login.class));
                        finish();
                    }

                }
            }
        };
        mSplashThread.start();
    }
}