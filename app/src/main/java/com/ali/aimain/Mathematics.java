package com.ali.aimain;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import com.ali.quizutility.MathQuiz;
import com.ali.systemIn.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Mathematics extends AppCompatActivity {

    private TextView questionText;
    private EditText answerInput;
    private Button checkButton;

    private List<MathQuiz> questionList;
    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mathematics);
        setupCalculatorButtons();


        questionText = findViewById(R.id.questionText);
        answerInput = findViewById(R.id.answerInput);
        checkButton = findViewById(R.id.checkButton);

        loadQuestions();
        showQuestion();

        checkButton.setOnClickListener(v -> checkAnswer()); {
            String input = answerInput.getText().toString().trim();
            if (!input.isEmpty()) {
                double userAnswer = Double.parseDouble(input);
                double correct = (double) questionList.get(currentIndex).getCorrectAnswer();

                if (Math.abs(userAnswer - correct) < 0.05) {
                    currentIndex++;
                    if (currentIndex < questionList.size()) {
                        showDialog("✅ Düzgün cavab!", this::showQuestion);
                    } else {
                        showDialog("🎉 Bütün sualları tamamladın!", () -> finish());
                    }
                } else {
                    showDialog("❌ Səhv cavab. Yenidən cəhd et.", null);
                }
            }
        };
    }

    private void loadQuestions() {
        questionList = new ArrayList<>();
        try {
            InputStream is = getAssets().open("dataAi/MathQuest/questmath.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");

            JSONArray categoriesArray = new JSONArray(json);  // Kategoriyaların array-i

            List<MathQuiz> tempList = new ArrayList<>();

            // Hər bir kategoriya üzrə sualları əldə et
            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject categoryObj = categoriesArray.getJSONObject(i);
                JSONArray questions = categoryObj.getJSONArray("questions");  // Sualların array-i

                // Sualları əlavə et
                for (int j = 0; j < questions.length(); j++) {
                    JSONObject questionObj = questions.getJSONObject(j);
                    String text = questionObj.getString("text");

                    Object answerObj = questionObj.get("correctAnswer");

                    if (answerObj instanceof Number) {
                        double answer = ((Number) answerObj).doubleValue();
                        tempList.add(new MathQuiz(text, answer));
                    } else if (answerObj instanceof String) {
                        tempList.add(new MathQuiz(text, answerObj));
                    } else if (answerObj == JSONObject.NULL) {
                        tempList.add(new MathQuiz(text, "Naməlum"));
                    }

                }
            }

            // Sualları qarışdır:
            Collections.shuffle(tempList);
            questionList.addAll(tempList);

        } catch (Exception e) {
            e.printStackTrace();
            showDialog("❌ Suallar yüklənə bilmədi.", () -> finish());
        }
    }





    private void showQuestion() {
        if (questionList != null && !questionList.isEmpty()) {
            if (currentIndex < questionList.size()) {
                MathQuiz q = questionList.get(currentIndex);
                if (q != null && q.getText() != null) {
                    questionText.setText(q.getText());
                } else {
                    questionText.setText("❗ Sual mövcud deyil.");
                }
                answerInput.setText(""); // Cavab hissəsini təmizlə
            } else {
                Toast.makeText(this, "✅ Bütün suallar bitdi!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "❌ Suallar tapılmadı!", Toast.LENGTH_SHORT).show();
        }
    }
    private void checkAnswer() {
        if (currentIndex >= questionList.size()) return;

        MathQuiz currentQuestion = questionList.get(currentIndex);
        String userInput = answerInput.getText().toString().trim();

        Object correctAnswer = currentQuestion.getCorrectAnswer();

        if (correctAnswer instanceof Number) {
            try {
                double userValue = Double.parseDouble(userInput);
                double correctValue = ((Number) correctAnswer).doubleValue();

                if (Math.abs(userValue - correctValue) < 0.01) {
                    showDialog("✅ Cavab doğrudur!", null);
                } else {
                    showDialog("❌ Səhv cavab. Düzgün cavab: " + correctValue, null);
                }
            } catch (NumberFormatException e) {
                // İstifadəçi rəqəm əvəzinə mətn yazıbsa
                showDialog("❌ Bu sual rəqəmlə cavablandırılmalıdır! " , null);
            }
        } else if (correctAnswer instanceof String) {
            try {
                // Əgər istifadəçi rəqəm yazıbsa amma cavab stringdirsə
                Double.parseDouble(userInput);
                showDialog("❌ Bu sual sözlə cavablandırılmalıdır! Düzgün cavab: ", null);
            } catch (NumberFormatException e) {
                // Hər iki tərəfi lower edib müqayisə
                if (userInput.equalsIgnoreCase((String) correctAnswer)) {
                    showDialog("✅ Cavab doğrudur!", null);
                } else {
                    showDialog("❌ Səhv cavab. Düzgün cavab: " ,null);
                }
            }
        } else {
            showDialog("⚠️ Cavab üçün uyğun format tapılmadı.", null);
        }

        currentIndex++;
        showQuestion();
    }




    private void showDialog(String message, Runnable onOk) {
        new AlertDialog.Builder(this)
                .setTitle("Nəticə")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (onOk != null) onOk.run();
                })
                .show();
    }

    private void setupCalculatorButtons() {
        GridLayout calculatorLayout = findViewById(R.id.calculatorLayout);

        for (int i = 0; i < calculatorLayout.getChildCount(); i++) {
            View child = calculatorLayout.getChildAt(i);
            if (child instanceof Button) {
                Button btn = (Button) child;
                btn.setOnClickListener(v -> {
                    String text = btn.getText().toString();
                    String current = answerInput.getText().toString();

                    switch (text) {
                        case "=":
                            try {
                                double result = eval(current);
                                answerInput.setText(String.valueOf(result));
                            } catch (Exception e) {
                                answerInput.setText("Səhv");
                            }
                            break;
                        case "C":
                            answerInput.setText("");
                            break;
                        case "Sil":
                            if (!current.isEmpty())
                                answerInput.setText(current.substring(0, current.length() - 1));
                            break;
                        default:
                            answerInput.append(text);
                            break;
                    }
                });
            }
        }
    }

    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (; ; ) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (; ; ) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                return x;
            }
        }.parse();
    }


}
