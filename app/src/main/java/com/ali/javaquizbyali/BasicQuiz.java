package com.ali.javaquizbyali;

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

public class BasicQuiz extends AppCompatActivity {

    MaterialTextView quiztext,aans,bans,cans,dans;
    MaterialButton nextButton;

    List<QuesitionsItem> quesitionsItems;
    private int questionCount = 0;
    private static final int MAX_QUESTIONS = 10;
    int currentQuestions = 0;
    int correct= 0,wrong=0;
    public static int checked;
    List<String> correctAnswersList = new ArrayList<>();
    private AdView mAdView;
    boolean isAnswered = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);


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


        quiztext=findViewById(R.id.quizText);
        aans = findViewById(R.id.answer);
        bans = findViewById(R.id.banswer);
        cans = findViewById(R.id.cnswer);
        dans = findViewById(R.id.dnswer);
        nextButton = findViewById(R.id.next_btn);


        loadAllQuestions();
        Collections.shuffle(quesitionsItems);
        setQuestionScreen(currentQuestions);



        aans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String correctAnswer = quesitionsItems.get(currentQuestions).getCorrect();
                isAnswered = true;
                if (quesitionsItems.get(currentQuestions).getAnswer1().equals(quesitionsItems.get(currentQuestions).getCorrect())) {
                    correct++;
                    correctAnswersList.add(quesitionsItems.get(currentQuestions).getAnswer1());
                    aans.setBackgroundResource(R.color.green);
                    aans.setTextColor(getResources().getColor(R.color.white));
                } else {
                    wrong++;
                    aans.setBackgroundResource(R.color.red);
                    aans.setTextColor(getResources().getColor(R.color.white));

                    if (bans.getText().toString().equals(correctAnswer)) {
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

                nextButton.setVisibility(View.VISIBLE);
                aans.setEnabled(false);
                bans.setEnabled(false);
                cans.setEnabled(false);
                dans.setEnabled(false);

                nextButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isAnswered){
                            Toast.makeText(BasicQuiz.this, "Cavab seçin", Toast.LENGTH_SHORT).show();
                        }else {
                            isAnswered = false;
                            if (currentQuestions < quesitionsItems.size() - 1) {
                                currentQuestions++;
                                setQuestionScreen(currentQuestions);
                                aans.setBackgroundResource(R.color.card_background);
                                aans.setTextColor(getResources().getColor(R.color.text_secondery_color));

                                // Bütün düymələri yenidən aktivləşdiririk və rənglərini sıfırlayırıq
                                resetButtonState(aans);
                                resetButtonState(bans);
                                resetButtonState(cans);
                                resetButtonState(dans);

                                nextButton.setVisibility(View.VISIBLE);
                                aans.setEnabled(true);
                                bans.setEnabled(true);
                                cans.setEnabled(true);
                                dans.setEnabled(true);
                            } else {
                                Intent intent = new Intent(BasicQuiz.this, ResultActivity.class);
                                intent.putStringArrayListExtra("correctAnswersList", (ArrayList<String>) correctAnswersList);
                                intent.putExtra("correct", correct);
                                intent.putExtra("wrong", wrong);
                                startActivity(intent);
                            }
                        }
                    }
                });
            }
        });


        bans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAnswered = true;
                String correctAnswer = quesitionsItems.get(currentQuestions).getCorrect();
                if (quesitionsItems.get(currentQuestions).getAnswer2().equals(quesitionsItems.get(currentQuestions).getCorrect())) {
                    correct++;
                    correctAnswersList.add(quesitionsItems.get(currentQuestions).getAnswer2());
                    bans.setBackgroundResource(R.color.green);
                    bans.setTextColor(getResources().getColor(R.color.white));

                } else {
                    wrong++;
                    bans.setBackgroundResource(R.color.red);
                    bans.setTextColor(getResources().getColor(R.color.white));

                    if (aans.getText().toString().equals(correctAnswer)) {
                        aans.setBackgroundResource(R.color.green);
                        aans.setTextColor(getResources().getColor(R.color.white));
                    } else if (cans.getText().toString().equals(correctAnswer)) {
                        cans.setBackgroundResource(R.color.green);
                        cans.setTextColor(getResources().getColor(R.color.white));
                    } else if (dans.getText().toString().equals(correctAnswer)) {
                        dans.setBackgroundResource(R.color.green);
                        dans.setTextColor(getResources().getColor(R.color.white));
                    }
                }

                nextButton.setVisibility(View.VISIBLE);
                aans.setEnabled(false);
                bans.setEnabled(false);
                cans.setEnabled(false);
                dans.setEnabled(false);

                nextButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isAnswered) {
                            Toast.makeText(BasicQuiz.this, "Cavab seçin", Toast.LENGTH_SHORT).show();
                        } else {
                            isAnswered = false;
                            if (currentQuestions < quesitionsItems.size() - 1) {
                                currentQuestions++;
                                setQuestionScreen(currentQuestions);
                                bans.setBackgroundResource(R.color.card_background);
                                bans.setTextColor(getResources().getColor(R.color.text_secondery_color));

                                // Bütün düymələri yenidən aktivləşdiririk və rənglərini sıfırlayırıq
                                resetButtonState(aans);
                                resetButtonState(bans);
                                resetButtonState(cans);
                                resetButtonState(dans);

                                // "Next" düyməsini gizlət və cavab düymələrini aktiv et
                                nextButton.setVisibility(View.VISIBLE);
                                aans.setEnabled(true);
                                bans.setEnabled(true);
                                cans.setEnabled(true);
                                dans.setEnabled(true);
                            } else {
                                Intent intent = new Intent(BasicQuiz.this, ResultActivity.class);
                                intent.putStringArrayListExtra("correctAnswersList", (ArrayList<String>) correctAnswersList);
                                intent.putExtra("correct", correct);
                                intent.putExtra("wrong", wrong);
                                startActivity(intent);
                            }
                        }
                    }
                });
            }
        });


        cans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAnswered = true;
                String correctAnswer = quesitionsItems.get(currentQuestions).getCorrect();

                if (quesitionsItems.get(currentQuestions).getAnswer3().equals(quesitionsItems.get(currentQuestions).getCorrect())) {
                    correct++;
                    correctAnswersList.add(quesitionsItems.get(currentQuestions).getAnswer3());
                    cans.setBackgroundResource(R.color.green);
                    cans.setTextColor(getResources().getColor(R.color.white));

                } else {
                    wrong++;
                    cans.setBackgroundResource(R.color.red);
                    cans.setTextColor(getResources().getColor(R.color.white));

                    if (aans.getText().toString().equals(correctAnswer)) {
                        aans.setBackgroundResource(R.color.green);
                        aans.setTextColor(getResources().getColor(R.color.white));
                    } else if (bans.getText().toString().equals(correctAnswer)) {
                        bans.setBackgroundResource(R.color.green);
                        bans.setTextColor(getResources().getColor(R.color.white));
                    } else if (dans.getText().toString().equals(correctAnswer)) {
                        dans.setBackgroundResource(R.color.green);
                        dans.setTextColor(getResources().getColor(R.color.white));
                    }
                }


                nextButton.setVisibility(View.VISIBLE);
                aans.setEnabled(false);
                bans.setEnabled(false);
                cans.setEnabled(false);
                dans.setEnabled(false);

                nextButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isAnswered) {
                            Toast.makeText(BasicQuiz.this, "Cavab seçin", Toast.LENGTH_SHORT).show();
                        } else {
                            isAnswered = false;
                            if (currentQuestions < quesitionsItems.size() - 1) {
                                currentQuestions++;
                                setQuestionScreen(currentQuestions);
                                cans.setBackgroundResource(R.color.card_background);
                                cans.setTextColor(getResources().getColor(R.color.text_secondery_color));

                                // Bütün düymələri yenidən aktivləşdiririk və rənglərini sıfırlayırıq
                                resetButtonState(aans);
                                resetButtonState(bans);
                                resetButtonState(cans);
                                resetButtonState(dans);

                                // "Next" düyməsini gizlət və cavab düymələrini aktiv et
                                nextButton.setVisibility(View.VISIBLE);
                                aans.setEnabled(true);
                                bans.setEnabled(true);
                                cans.setEnabled(true);
                                dans.setEnabled(true);
                            } else {
                                Intent intent = new Intent(BasicQuiz.this, ResultActivity.class);
                                intent.putStringArrayListExtra("correctAnswersList", (ArrayList<String>) correctAnswersList);
                                intent.putExtra("correct", correct);
                                intent.putExtra("wrong", wrong);
                                startActivity(intent);
                            }
                        }
                    }
                });
            }
        });

        dans.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAnswered = true;

                String correctAnswer = quesitionsItems.get(currentQuestions).getCorrect();

                if (quesitionsItems.get(currentQuestions).getAnswer4().equals(correctAnswer)) {
                    correct++;
                    correctAnswersList.add(quesitionsItems.get(currentQuestions).getAnswer4());
                    dans.setBackgroundResource(R.color.green);
                    dans.setTextColor(getResources().getColor(R.color.white));
                } else {
                    wrong++;
                    dans.setBackgroundResource(R.color.red);
                    dans.setTextColor(getResources().getColor(R.color.white));

                    // Düzgün cavabı tapıb yaşıllaşdırırıq
                    if (aans.getText().toString().equals(correctAnswer)) {
                        aans.setBackgroundResource(R.color.green);
                        aans.setTextColor(getResources().getColor(R.color.white));
                    } else if (bans.getText().toString().equals(correctAnswer)) {
                        bans.setBackgroundResource(R.color.green);
                        bans.setTextColor(getResources().getColor(R.color.white));
                    } else if (cans.getText().toString().equals(correctAnswer)) {
                        cans.setBackgroundResource(R.color.green);
                        cans.setTextColor(getResources().getColor(R.color.white));
                    }
                }


                // Bütün cavab düymələrini qeyri-aktiv edirik
                nextButton.setVisibility(View.VISIBLE);
                aans.setEnabled(false);
                bans.setEnabled(false);
                cans.setEnabled(false);
                dans.setEnabled(false);

                nextButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!isAnswered) {
                            Toast.makeText(BasicQuiz.this, "Cavab seçin", Toast.LENGTH_SHORT).show();
                        } else {
                            isAnswered = false;
                            if (currentQuestions < quesitionsItems.size() - 1) {
                                currentQuestions++;
                                setQuestionScreen(currentQuestions);

                                // Bütün düymələri yenidən aktivləşdiririk və rənglərini sıfırlayırıq
                                resetButtonState(aans);
                                resetButtonState(bans);
                                resetButtonState(cans);
                                resetButtonState(dans);

                                nextButton.setVisibility(View.INVISIBLE);
                                aans.setEnabled(true);
                                bans.setEnabled(true);
                                cans.setEnabled(true);
                                dans.setEnabled(true);
                            } else {
                                Intent intent = new Intent(BasicQuiz.this, ResultActivity.class);
                                intent.putStringArrayListExtra("correctAnswersList", (ArrayList<String>) correctAnswersList);
                                intent.putExtra("correct", correct);
                                intent.putExtra("wrong", wrong);
                                startActivity(intent);
                            }
                        }
                    }
                });
            }
        });




        mAdView = findViewById(R.id.adView2);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        getOnBackPressedDispatcher().addCallback(this, callback);


    }

    // **Yeni əlavə edilmiş metod** - bütün düymələrin rənglərini və mətn rənglərini sıfırlayır
    private void resetButtonState(MaterialTextView button) {
        button.setBackgroundResource(R.color.card_background);
        button.setTextColor(getResources().getColor(R.color.text_secondery_color));
    }


    private void setQuestionScreen(int currentQuestions) {
        if (questionCount < MAX_QUESTIONS) {
            quiztext.setText(quesitionsItems.get(currentQuestions).getQuestions());
            aans.setText(quesitionsItems.get(currentQuestions).getAnswer1());
            bans.setText(quesitionsItems.get(currentQuestions).getAnswer2());
            cans.setText(quesitionsItems.get(currentQuestions).getAnswer3());
            dans.setText(quesitionsItems.get(currentQuestions).getAnswer4());
            questionCount++;
        } else {
            // 10 sualdan sonra göstərilən hər hansı bir sualı keçmə
            Intent intent = new Intent(BasicQuiz.this, ResultActivity.class);
            intent.putExtra("correct", correct);
            intent.putExtra("wrong", wrong);
            intent.putStringArrayListExtra("wrongAnswersList", (ArrayList<String>) correctAnswersList);
            startActivity(intent);
            finish();
        }
    }

    private void loadAllQuestions() {
        quesitionsItems = new ArrayList<>();
        addQuestionsFromJsonArray("seniorquestions1");
        addQuestionsFromJsonArray("seniorquestions2");
        addQuestionsFromJsonArray("seniorquestions3");
        addQuestionsFromJsonArray("seniorquestions4");
        addQuestionsFromJsonArray("seniorquestions5");
        addQuestionsFromJsonArray("seniorquestions6");
        addQuestionsFromJsonArray("seniorquestions7");

    }

    private void addQuestionsFromJsonArray(String arrayName) {
        String jsonquiz = loadJsonFromAsset("dataJava/SeniorQuestions.json");
        try {
            JSONObject jsonObject = new JSONObject(jsonquiz);
            if(jsonObject.has(arrayName)){
                JSONArray questions = jsonObject.getJSONArray(arrayName);
                for (int i = 0; i < questions.length(); i++) {
                    JSONObject question = questions.getJSONObject(i);

                    String questionsString = question.getString("question");
                    String answer1String = question.getString("answer1");
                    String answer2String = question.getString("answer2");
                    String answer3String = question.getString("answer3");
                    String answer4String = question.getString("answer4");
                    String correctString = question.getString("correct");

                    quesitionsItems.add(new QuesitionsItem(questionsString, answer1String, answer2String, answer3String, answer4String, correctString));
                }

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    private String loadJsonFromAsset(String s) {
        String json = "";
        try {
            InputStream inputStream= getAssets().open(s);
            int size = inputStream.available();
            byte[]buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer,"UTF-8");
        }catch (IOException e){
            e.printStackTrace();
        }
        return json;
    }


    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(BasicQuiz.this);
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
                    startActivity(new Intent(BasicQuiz.this , JavaMain.class));
                    finish();
                }
            });

            materialAlertDialogBuilder.show();
        }

    };
}