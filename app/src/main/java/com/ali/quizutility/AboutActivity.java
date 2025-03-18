package com.ali.quizutility;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.ali.MainActivity;
import com.ali.javaquizbyali.JavaMain;
import com.ali.javaquizbyali.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AboutActivity extends AppCompatActivity {

    MaterialCardView developerCard,darkCard;
    MaterialButton home;

    public static int checked;
    public static final String MyPREFERENCES ="QuizPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);


        developerCard = findViewById(R.id.developerCard);
        darkCard = findViewById(R.id.darkMode);

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
        final String[] strings = {getString(R.string.light),getString(R.string.dark),getString(R.string.system_default),getString(R.string.set_battery)};
        darkCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(AboutActivity.this);
                builder.setTitle(R.string.dark_mode);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(checked == 0){
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                        }
                        if(checked == 1){
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        }
                        if(checked == 2){
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        }
                        if(checked == 3){
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                        }
                        restartApp();
                    }
                });
                builder.setSingleChoiceItems(strings, checked, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checked=which;
                        editor.putInt("checked",checked);
                        editor.apply();
                    }
                });
                builder.show();
            }
        });

        home = findViewById(R.id.returnHome);



        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AboutActivity.this , MainActivity.class));
                finish();
            }
        });

        developerCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://www.linkedin.com/in/ali-balakishiyev-6b1048339/");
                Intent likeIng = new Intent(Intent.ACTION_VIEW,uri);
                likeIng.setPackage("com.facebook.android");

                try{
                    startActivity(likeIng);
                }catch (ActivityNotFoundException e){
                    startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.linkedin.com/in/ali-balakishiyev-6b1048339/")));
                }
            }
        });
        getOnBackPressedDispatcher().addCallback(this, callback);

    }

    private void restartApp() {
        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            startActivity(new Intent(AboutActivity.this,MainActivity.class));
            finish();
        }
    };
}