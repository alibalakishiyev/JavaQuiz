package com.ali.javaquizbyali;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ali.systemIn.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Hazirliq extends AppCompatActivity {


    MaterialButton cavabButton;
    private AdView mAdView;
    private Iterator<Map.Entry<String, String>> questionIterator;
    private TextView quizText, cavablarText;
    private Map.Entry<String, String> currentEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hazirliq);

        cavabButton = findViewById(R.id.cavab);
        quizText = findViewById(R.id.quizText);
        cavablarText = findViewById(R.id.cavablar);

        HashMap<String, String> qaMap = loadQuestionsWithAnswers(this, "dataJava/SualVeCavablar.txt");
        questionIterator = qaMap.entrySet().iterator();

        // İlk sualı göstəririk
        if (questionIterator.hasNext()) {
            currentEntry = questionIterator.next();
            quizText.setText(currentEntry.getKey());
            cavablarText.setVisibility(View.GONE);
        }


        cavabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (cavablarText.getVisibility() == View.GONE) {
                    // Cavabı göstər
                    cavablarText.setText(currentEntry.getValue()); // Cavab
                    cavablarText.setVisibility(View.VISIBLE);
                    cavabButton.setText("Next");
                } else if (questionIterator.hasNext()) {
                    // Növbəti suala keç
                    currentEntry = questionIterator.next();
                    quizText.setText(currentEntry.getKey()); // Növbəti sual
                    cavablarText.setVisibility(View.GONE);   // Cavabı gizlət
                    cavabButton.setText("Cavab");
                } else {
                    // Suallar bitdi
                    quizText.setText("Bütün suallar bitdi!");
                    cavablarText.setVisibility(View.GONE);
                }

            }
        });

        mAdView = findViewById(R.id.adView5);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);




    }

    public static HashMap<String, String> loadQuestionsWithAnswers(Context context, String fileName) {
        HashMap<String, String> qaMap = new HashMap<>();
        try {
            // Faylı assets qovluğundan oxumaq
            InputStream inputStream = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            String sual = null;
            StringBuilder cavab = new StringBuilder(); // Cavabları yığmaq üçün StringBuilder

            while ((line = reader.readLine()) != null) {
                line = line.trim(); // Hər bir sətrin başında və sonunda boşluqları təmizləyirik

                // Sual tanımaq (nömrə ilə başlayan sətirlər)
                if (line.matches("^\\d+\\.\\s.*")) {
                    if (sual != null && cavab.length() > 0) {
                        qaMap.put(sual, cavab.toString().trim()); // Əvvəlki sualın cavabını əlavə edirik
                    }
                    sual = line; // Yeni sual
                    cavab.setLength(0); // Cavab üçün buffer-i təmizləyirik
                } else if (line.startsWith("Cavab:")) { // Cavab sətirini tanımaq
                    // "Cavab:" sözünü çıxarıb, cavabı əlavə edirik
                    cavab.append(line.replace("Cavab:", "").trim()).append("\n");
                }
                else {
                    // Digər sətirləri (məsələn, "•" simvolu ilə başlayan) cavaba əlavə edirik
                    cavab.append(line).append("\n");
                }
            }

            // Sonuncu sualı və cavabı əlavə edirik
            if (sual != null && cavab.length() > 0) {
                qaMap.put(sual, cavab.toString().trim());
            }

            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return qaMap;
    }
}