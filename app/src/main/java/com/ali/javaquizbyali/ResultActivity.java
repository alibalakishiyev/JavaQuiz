package com.ali.javaquizbyali;

import static com.ali.javaquizbyali.AboutActivity.MyPREFERENCES;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {
    MaterialButton home;
    TextView correctt, wrongt, resultinfo, resultscore,wrongAnswersText;
    ImageView resultImage;
    public static int checked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);



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


        home = findViewById(R.id.returnHome);
        correctt = findViewById(R.id.correctScore);
        wrongt = findViewById(R.id.wrongScore);
        resultinfo = findViewById(R.id.resultInfo);
        resultscore = findViewById(R.id.resultScore);
        resultImage = findViewById(R.id.resultImage);
        wrongAnswersText= findViewById(R.id.wrongAnswers);


        int correct = getIntent().getIntExtra("correct", 0);
        int wrong = getIntent().getIntExtra("wrong", 0);



        int score = correct * 5;

        correctt.setText("" + correct);
        wrongt.setText("" + wrong);
        resultscore.setText("" + score);



        if (correct >= 0 && correct <= 3) {
            resultinfo.setText("Testi yenidən keçməlisiniz.");
            resultImage.setImageResource(R.drawable.ic_sad);
        } else if (correct >= 4 && correct <= 7){
            resultinfo.setText("Bir az daha cəhd etməlisən.");
            resultImage.setImageResource(R.drawable.ic_neutral);
        } else if (correct >= 8 && correct <= 10){
            resultinfo.setText("Sən çox yaxşısan!");
            resultImage.setImageResource(R.drawable.ic_smile);
        } else {
            resultinfo.setText("Təbriklər, doğru cavab!");
            resultImage.setImageResource(R.drawable.ic_smile);
        }

        ArrayList<String> wrongAnswersList = getIntent().getStringArrayListExtra("wrongAnswersList");
        Log.d("ResultActivity", "Wrong Answers List: " + wrongAnswersList);
        if (wrongAnswersList != null && !wrongAnswersList.isEmpty()) {
            StringBuilder wrongAnswers = new StringBuilder();
            for (String answer : wrongAnswersList) {
                wrongAnswers.append(answer).append("\n\n");
            }
            wrongAnswersText.setText(wrongAnswers.toString());
        } else {
            wrongAnswersText.setText("Səhv cavab yoxdur.");
        }



        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ResultActivity.this , MainActivity.class));
            }
        });
    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            startActivity(new Intent(ResultActivity.this , MainActivity.class));
        }
    };
}