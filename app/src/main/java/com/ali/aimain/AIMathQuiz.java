package com.ali.aimain;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ali.systemIn.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Scanner;

public class AIMathQuiz extends AppCompatActivity {
    private TextView tvQuestion, tvCategory, tvQuestionCount, tvExplanation;
    private RadioGroup rgOptions;
    private RadioButton[] rbOptions;
    private Button btnNext;

    private JSONArray questions;
    private int currentQuestion = 0;
    private int score = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aimath_quiz);

        // UI komponentləri tapırıq
        tvQuestion = findViewById(R.id.tv_question);
        tvCategory = findViewById(R.id.tv_category);
        tvQuestionCount = findViewById(R.id.tv_question_count);
        tvExplanation = findViewById(R.id.tv_explanation);
        rgOptions = findViewById(R.id.rg_options);
        btnNext = findViewById(R.id.btn_next);

        // RadioButton-ları MaterialCardView daxilindən tapırıq
        rbOptions = new RadioButton[4];
        for (int i = 0; i < 4; i++) {
            View card = rgOptions.getChildAt(i); // MaterialCardView
            rbOptions[i] = card.findViewById(getRadioButtonIdByIndex(i));
        }

        loadQuestions();
        displayQuestion();

        btnNext.setOnClickListener(v -> {
            if (!checkAnswer()) return;

            currentQuestion++;
            if (currentQuestion < questions.length()) {
                displayQuestion();
            } else {
                showResult();
            }
        });
    }

    private int getRadioButtonIdByIndex(int index) {
        switch (index) {
            case 0: return R.id.rb_option1;
            case 1: return R.id.rb_option2;
            case 2: return R.id.rb_option3;
            case 3: return R.id.rb_option4;
            default: return -1;
        }
    }

    private void loadQuestions() {
        try {
            InputStream is = getAssets().open("dataAi/MathQuest/quiz_ai_math.json");
            String json = new Scanner(is).useDelimiter("\\A").next();
            questions = new JSONObject(json).getJSONArray("questions");
        } catch (Exception e) {
            Toast.makeText(this, "Suallar yüklənərkən xəta baş verdi", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void displayQuestion() {
        try {
            JSONObject q = questions.getJSONObject(currentQuestion);

            tvQuestion.setText(q.getString("question"));
            tvCategory.setText(q.getString("category"));
            tvQuestionCount.setText((currentQuestion + 1) + "/" + questions.length());

            JSONArray options = q.getJSONArray("options");
            for (int i = 0; i < options.length(); i++) {
                rbOptions[i].setText(options.getString(i));
            }

            rgOptions.clearCheck();
            tvExplanation.setVisibility(View.GONE); // İzahatı gizlət
            btnNext.setText(currentQuestion == questions.length() - 1 ? "Nəticəni gör" : "Növbəti");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean checkAnswer() {
        try {
            JSONObject q = questions.getJSONObject(currentQuestion);
            int selectedRadioButtonId = rgOptions.getCheckedRadioButtonId();

            if (selectedRadioButtonId == -1) {
                Toast.makeText(this, "Zəhmət olmasa bir cavab seçin", Toast.LENGTH_SHORT).show();
                return false;
            }

            int selectedIndex = -1;
            for (int i = 0; i < rbOptions.length; i++) {
                if (rbOptions[i].getId() == selectedRadioButtonId) {
                    selectedIndex = i;
                    break;
                }
            }

            if (selectedIndex == q.getInt("answer")) {
                score++;
                Toast.makeText(this, "Düzgün!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Səhv! Düzgün cavab: " + (q.getInt("answer") + 1), Toast.LENGTH_SHORT).show();
            }

            tvExplanation.setText("İzahat: " + q.getString("explanation"));
            tvExplanation.setVisibility(View.VISIBLE);
            return true;

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showResult() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quiz Nəticəsi")
                .setMessage(score + " / " + questions.length() + " düzgün cavab")
                .setPositiveButton("Yenidən başla", (dialog, which) -> {
                    currentQuestion = 0;
                    score = 0;
                    displayQuestion();
                })
                .setNegativeButton("Bitir", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}
