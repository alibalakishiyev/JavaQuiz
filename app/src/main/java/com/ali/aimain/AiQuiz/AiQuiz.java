package com.ali.aimain.AiQuiz;

import static com.ali.quizutility.AboutActivity.MyPREFERENCES;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.ali.pymain.PythonMain;
import com.ali.quizutility.QuesitionsItem;
import com.ali.quizutility.ResultActivity;
import com.ali.systemIn.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AiQuiz extends AppCompatActivity {

    MaterialTextView quiztext, aans, bans, cans, dans;
    MaterialButton nextButton;

    List<QuesitionsItem> quesitionsItems;
    private int questionCount = 0;
    private static final int MAX_QUESTIONS = 10;
    int currentQuestions = 0;
    int correct = 0, wrong = 0;
    public static int checked;
    List<String> correctAnswersList = new ArrayList<>();
    private AdView mAdView;
    boolean isAnswered = false;

    private String quizName = "";
    private String jsonFileName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Intent-dən məlumatları al
        Intent intent = getIntent();
        if (intent != null) {
            quizName = intent.getStringExtra("QUIZ_TYPE");
            jsonFileName = intent.getStringExtra("JSON_FILE");
        }



        SharedPreferences sharedPreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        int default1;
        switch (Configuration.UI_MODE_NIGHT_MASK & AppCompatDelegate.getDefaultNightMode()) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                default1 = 0;
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                default1 = 1;
                break;
            default:
                default1 = 2;
        }

        checked = sharedPreferences.getInt("checked", default1);
        switch (checked) {
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

        quiztext = findViewById(R.id.quizText);
        aans = findViewById(R.id.answer);
        bans = findViewById(R.id.banswer);
        cans = findViewById(R.id.cnswer);
        dans = findViewById(R.id.dnswer);
        nextButton = findViewById(R.id.next_btn);

        loadAllQuestions();

        if (quesitionsItems != null && quesitionsItems.size() > 0) {
            Collections.shuffle(quesitionsItems);
            setQuestionScreen(currentQuestions);
        } else {
            Toast.makeText(this, "Heç bir sual tapılmadı!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupAnswerListeners();

        mAdView = findViewById(R.id.adView2);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void setupAnswerListeners() {
        aans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer(aans, quesitionsItems.get(currentQuestions).getAnswer1());
            }
        });

        bans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer(bans, quesitionsItems.get(currentQuestions).getAnswer2());
            }
        });

        cans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer(cans, quesitionsItems.get(currentQuestions).getAnswer3());
            }
        });

        dans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswer(dans, quesitionsItems.get(currentQuestions).getAnswer4());
            }
        });
    }

    private void checkAnswer(MaterialTextView selectedButton, String selectedAnswer) {
        if (isAnswered) return;

        isAnswered = true;
        String correctAnswer = quesitionsItems.get(currentQuestions).getCorrect();

        if (selectedAnswer.equals(correctAnswer)) {
            correct++;
            correctAnswersList.add(selectedAnswer);
            selectedButton.setBackgroundResource(R.color.green);
            selectedButton.setTextColor(getResources().getColor(R.color.white));
        } else {
            wrong++;
            selectedButton.setBackgroundResource(R.color.red);
            selectedButton.setTextColor(getResources().getColor(R.color.white));
            highlightCorrectAnswer(correctAnswer);
        }

        disableButtonsAndShowNext();
    }

    private void highlightCorrectAnswer(String correctAnswer) {
        if (aans.getText().toString().equals(correctAnswer)) {
            aans.setBackgroundResource(R.color.green);
            aans.setTextColor(getResources().getColor(R.color.white));
        } else if (bans.getText().toString().equals(correctAnswer)) {
            bans.setBackgroundResource(R.color.green);
            bans.setTextColor(getResources().getColor(R.color.white));
        } else if (cans.getText().toString().equals(correctAnswer)) {
            cans.setBackgroundResource(R.color.green);
            cans.setTextColor(getResources().getColor(R.color.white));
        } else if (dans.getText().toString().equals(correctAnswer)) {
            dans.setBackgroundResource(R.color.green);
            dans.setTextColor(getResources().getColor(R.color.white));
        }
    }

    private void disableButtonsAndShowNext() {
        aans.setEnabled(false);
        bans.setEnabled(false);
        cans.setEnabled(false);
        dans.setEnabled(false);
        nextButton.setVisibility(View.VISIBLE);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAnswered) {
                    Toast.makeText(AiQuiz.this, "Cavab seçin", Toast.LENGTH_SHORT).show();
                } else {
                    isAnswered = false;
                    if (currentQuestions < quesitionsItems.size() - 1 && questionCount < MAX_QUESTIONS) {
                        currentQuestions++;
                        setQuestionScreen(currentQuestions);
                        resetButtons();
                        nextButton.setVisibility(View.INVISIBLE);
                        aans.setEnabled(true);
                        bans.setEnabled(true);
                        cans.setEnabled(true);
                        dans.setEnabled(true);
                    } else {
                        goToResult();
                    }
                }
            }
        });
    }

    private void resetButtons() {
        resetButtonState(aans);
        resetButtonState(bans);
        resetButtonState(cans);
        resetButtonState(dans);
    }

    private void resetButtonState(MaterialTextView button) {
        button.setBackgroundResource(R.color.card_background);
        button.setTextColor(getResources().getColor(R.color.text_secondery_color));
    }

    private void goToResult() {
        Intent intent = new Intent(AiQuiz.this, ResultActivity.class);
        intent.putStringArrayListExtra("correctAnswersList", (ArrayList<String>) correctAnswersList);
        intent.putExtra("correct", correct);
        intent.putExtra("wrong", wrong);
        startActivity(intent);
        finish();
    }

    private void setQuestionScreen(int currentQuestions) {
        if (questionCount < MAX_QUESTIONS && currentQuestions < quesitionsItems.size()) {
            quiztext.setText(quesitionsItems.get(currentQuestions).getQuestions());
            aans.setText(quesitionsItems.get(currentQuestions).getAnswer1());
            bans.setText(quesitionsItems.get(currentQuestions).getAnswer2());
            cans.setText(quesitionsItems.get(currentQuestions).getAnswer3());
            dans.setText(quesitionsItems.get(currentQuestions).getAnswer4());
            questionCount++;
        } else {
            goToResult();
        }
    }

    private void loadAllQuestions() {
        quesitionsItems = new ArrayList<>();

        // JSON faylını yüklə
        String jsonContent = loadJsonFromAsset(jsonFileName);

        if (jsonContent == null || jsonContent.isEmpty()) {
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(jsonContent);

            // JSON faylının daxilindəki bütün bölmələri tap və yüklə
            JSONArray names = jsonObject.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String sectionName = names.getString(i);
                    addQuestionsFromJsonArray(jsonObject, sectionName);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addQuestionsFromJsonArray(JSONObject jsonObject, String arrayName) {
        try {
            if (jsonObject.has(arrayName)) {
                JSONArray questions = jsonObject.getJSONArray(arrayName);
                for (int i = 0; i < questions.length(); i++) {
                    JSONObject question = questions.getJSONObject(i);

                    String questionsString = question.getString("question");
                    String answer1String = question.getString("answer1");
                    String answer2String = question.getString("answer2");
                    String answer3String = question.getString("answer3");
                    String answer4String = question.getString("answer4");
                    String correctString = question.getString("correct");

                    quesitionsItems.add(new QuesitionsItem(
                            questionsString, answer1String, answer2String,
                            answer3String, answer4String, correctString
                    ));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String loadJsonFromAsset(String fileName) {
        String json = "";
        try {
            // Əvvəlcə dataPython/ qovluğunda yoxla
            InputStream inputStream = getAssets().open("dataPython/" + fileName);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
            // Əgər tapılmazsa, birbaşa assets qovluğunda yoxla
            try {
                InputStream inputStream = getAssets().open(fileName);
                int size = inputStream.available();
                byte[] buffer = new byte[size];
                inputStream.read(buffer);
                inputStream.close();
                json = new String(buffer, "UTF-8");
            } catch (IOException e2) {
                e2.printStackTrace();
                Toast.makeText(this, "Fayl tapılmadı: " + fileName, Toast.LENGTH_LONG).show();
            }
        }
        return json;
    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(AiQuiz.this);
            materialAlertDialogBuilder.setTitle(R.string.app_name);
            materialAlertDialogBuilder.setMessage("Are you sure want to exit the quiz?");
            materialAlertDialogBuilder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    dialog.dismiss();
                }
            });
            materialAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    startActivity(new Intent(AiQuiz.this, PythonMain.class));
                    finish();
                }
            });
            materialAlertDialogBuilder.show();
        }
    };
}