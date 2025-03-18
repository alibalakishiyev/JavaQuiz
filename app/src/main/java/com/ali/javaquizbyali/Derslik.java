package com.ali.javaquizbyali;

import static com.ali.quizutility.AboutActivity.MyPREFERENCES;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ali.MainActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Derslik extends AppCompatActivity {

    MaterialButton home;
    ImageButton editorFile;
    private AdView mAdView4;
    EditText editText;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_derslik);



        SharedPreferences sharedPreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();


        TextView textView = findViewById(R.id.myTextView); // TextView-i tapın
        String text = loadTextFromAssets(this, "SualVeCavablar.txt"); // Faylın məzmununu yükləyin
        textView.setText(text);

        editor.putString("key", text);
        editor.apply();

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {

            }
        });

        home = findViewById(R.id.returnHome);
        editorFile = findViewById(R.id.editorFile);
        editText = findViewById(R.id.myEditText);

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Derslik.this, MainActivity.class));
                finish();
            }
        });

        editorFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Əgər TextView görünürsə
                if (textView.getVisibility() == View.VISIBLE) {
                    // TextView-ni gizlədin və EditText-i göstərin
                    textView.setVisibility(View.GONE);
                    editText.setVisibility(View.VISIBLE);
                    editText.setText(textView.getText());  // TextView-in mətnini EditText-ə qoyun
                } else {
                    // EditText-dən dəyişiklikləri alıb, TextView-ə yazın
                    textView.setText(editText.getText());
                    saveTextToFile(editText.getText().toString(), "SualVeCavablar.txt");
                    Toast.makeText(Derslik.this, "Dəyişikliklər uğurla yadda saxlanıldı!", Toast.LENGTH_SHORT).show();
                    // EditText-i gizlədin və TextView-i yenidən göstərin
                    textView.setVisibility(View.VISIBLE);
                    editText.setVisibility(View.GONE);
                }
            }
        });


        mAdView4 = findViewById(R.id.adView4);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView4.loadAd(adRequest);


        getOnBackPressedDispatcher().addCallback(this, callback);

    }


    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(Derslik.this);
            materialAlertDialogBuilder.setTitle(R.string.app_name);
            materialAlertDialogBuilder.setMessage("Are you sure want to exit the app?");
            materialAlertDialogBuilder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    dialog.dismiss();
                }
            });
            materialAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    startActivity(new Intent(Derslik.this, MainActivity.class));
                    finish();
                }
            });
            materialAlertDialogBuilder.show();
        }
    };

    String loadTextFromAssets(Context context, String filename) {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(filename)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

    private void saveTextToFile(String text, String filename) {
        try (FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE)) {
            fos.write(text.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}