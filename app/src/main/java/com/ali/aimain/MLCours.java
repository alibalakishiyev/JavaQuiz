package com.ali.aimain;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Spinner;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ali.MainActivity;
import com.ali.pymain.PythonConsole;
import com.ali.quizutility.AboutActivity;
import com.ali.systemIn.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MLCours extends AppCompatActivity {

    MaterialButton home;
    private AdView mAdView5;
    private WebView courseWebView;
    Spinner courseSpinner;
    private ImageButton pyConsole;

    List<String> fileList = new ArrayList<>();
    List<String> displayList = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_cours);

        SharedPreferences sharedPreferences = getSharedPreferences(AboutActivity.MyPREFERENCES, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        home = findViewById(R.id.homeBack);
        courseSpinner = findViewById(R.id.courseSpinner);
        pyConsole = findViewById(R.id.btnPyConsole);

        courseWebView = findViewById(R.id.courseWebView);
        courseWebView.getSettings().setJavaScriptEnabled(true); // lazım olarsa
        courseWebView.setBackgroundColor(Color.TRANSPARENT);

        courseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    courseWebView.loadDataWithBaseURL(null, "", "text/html", "UTF-8", null);
                    return;
                }

                String selectedFile = fileList.get(position - 1); // "lesson1.html", "lesson2.html" və s.
                String filePath = "file:///android_asset/dataAi/CoursList/" + selectedFile;
                courseWebView.loadUrl(filePath);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });



        // Reklam
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
            }
        });

        // Home düyməsi
        home.setOnClickListener(v -> {
            startActivity(new Intent(MLCours.this, AiMain.class));
            finish();
        });

        try {
            // Fayl siyahısını assets/dataAi/ içindən al
            String[] files = getAssets().list("dataAi/CoursList");
            if (files != null) {
                fileList = Arrays.asList(files);
                displayList.add("Kurs seçin..."); // ilk boş seçim
                displayList.addAll(fileList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Adapter yaradılır
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, displayList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(Color.WHITE); // Spinner-in seçilmiş mətni
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(Color.BLACK); // Dropdown menyudakı mətnlər
                return view;
            }
        };

        pyConsole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MLCours.this, PythonConsole.class);
                startActivity(intent);
            }
        });

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        courseSpinner.setAdapter(adapter);

        courseSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    courseWebView.loadDataWithBaseURL(null, "", "text/html", "UTF-8", null);
                    return;
                }

                String filename = "dataAi/CoursList/" + fileList.get(position - 1);
                String content = loadTextFromAssets(MLCours.this, filename);

                // HTML-ə uyğun vurğulama
                String htmlContent = highlightKeywordInHtml(content, "Python"); // əgər istəsən birdən çox keyword də edə bilərik

                courseWebView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        // AdMob Banner
        mAdView5 = findViewById(R.id.adView5);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView5.loadAd(adRequest);

        // Geri düyməsi
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private String highlightKeywordInHtml(String content, String keyword) {
        // Açar sözü qırmızı rənglə dəyiş
        return "<html><body style='color:white; background-color:transparent;'>" +
                content.replace(keyword, "<span style='color:red;'>" + keyword + "</span>") +
                "</body></html>";
    }


    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(MLCours.this);
            materialAlertDialogBuilder.setTitle(R.string.app_name);
            materialAlertDialogBuilder.setMessage("Are you sure want to exit the app?");
            materialAlertDialogBuilder.setNegativeButton(android.R.string.no, (dialog, i) -> dialog.dismiss());
            materialAlertDialogBuilder.setPositiveButton(android.R.string.yes, (dialog, i) -> {
                startActivity(new Intent(MLCours.this, MainActivity.class));
                finish();
            });
            materialAlertDialogBuilder.show();
        }
    };

    // Fayldan mətn oxuma funksiyası
    String loadTextFromAssets(Context context, String filename) {
        StringBuilder text = new StringBuilder();
        try (InputStream is = context.getAssets().open(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

    // Mətnin müəyyən hissəsini qırmızıya rəngləmək üçün metod
    private String colorSpecificText(String content) {
        SpannableString spannableString = new SpannableString(content);

        // Məsələn, "Python" sözünü qırmızıya rəngləyək
        String keyword = "Python";
        int start = content.indexOf(keyword);
        int end = start + keyword.length();

        if (start >= 0) {
            spannableString.setSpan(new ForegroundColorSpan(Color.RED), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannableString.toString();
    }
}
