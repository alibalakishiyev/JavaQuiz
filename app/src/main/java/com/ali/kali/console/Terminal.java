package com.ali.kali.console;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.ali.MainActivity;
import com.ali.autorized.Login;
import com.ali.kali.LinuxMain;
import com.ali.systemIn.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Terminal extends AppCompatActivity {

    private TextView terminalOutput,questionText,answerText;
    private EditText commandInput;
    private ImageButton btnNextQuest;
    ScrollView scrollView;

    private List<Question> questionList = new ArrayList<>();
    private Question currentQuestion;
    private AdView mAdView6;

    private MediaPlayer mediaPlayerBingo;
    private MediaPlayer mediaPlayerError;

    private FirebaseAuth fAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        terminalOutput = findViewById(R.id.terminal_output);
        commandInput = findViewById(R.id.command_input);
        questionText = findViewById(R.id.question_text);
        btnNextQuest = findViewById(R.id.btnNextQuestions);
        answerText = findViewById(R.id.answerText);
        scrollView = findViewById(R.id.scrollView);
        fAuth = FirebaseAuth.getInstance();

        mediaPlayerBingo = MediaPlayer.create(this, R.raw.accept);
        mediaPlayerError = MediaPlayer.create(this, R.raw.error2);


        loadCommandsFromJson();
        showRandomQuestion();

        commandInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                String input = commandInput.getText().toString().trim();
                handleCommand(input);
                commandInput.setText("");
                return true;
            }
            return false;
        });

        btnNextQuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRandomQuestion();
            }
        });

        questionText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (questionText != null){
                    answerText.setTextColor(Color.parseColor("#00FF00"));
                    answerText.setText("Answer code :"+currentQuestion.getAnswer());

                }
            }
        });


        mAdView6 = findViewById(R.id.adView6);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView6.loadAd(adRequest);

        getOnBackPressedDispatcher().addCallback(this, callback);

    }

    private void handleCommand(String cmd) {
        if (cmd.isEmpty()) return;

        String newOutput = "> " + cmd + "\n";

        if (cmd.equalsIgnoreCase(currentQuestion.getAnswer())) {
            newOutput += "Success ->" + currentQuestion.getDescription() + "\n";

            if (mediaPlayerBingo != null) {
                mediaPlayerBingo.start();
            }

            showRandomQuestion();
        } else {
            newOutput += "Error:bash:Command not found\n";

            if (mediaPlayerError != null) {
                mediaPlayerError.start();
            }
        }

        // Mövcud mətni əvvəlki kimi saxla
        String previous = terminalOutput.getText().toString();

        // Yenini yuxarı yaz
        terminalOutput.setText(newOutput + previous);

        // ScrollView-un başına çıx
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_UP));
    }



    private void loadCommandsFromJson() {
        try {
            InputStream is = getAssets().open("terminalCode/commands.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONObject jsonObject = new JSONObject(json);
            JSONArray questions = jsonObject.getJSONArray("questions");

            for (int i = 0; i < questions.length(); i++) {
                JSONObject item = questions.getJSONObject(i);
                String question = item.getString("question").trim();
                String answer = item.getString("answer").trim();
                String description = item.getString("description").trim();

                questionList.add(new Question(question, answer, description));
            }

        } catch (Exception e) {
            e.printStackTrace();
            terminalOutput.setText("Error loading commands.\n");
        }
    }

    private void showRandomQuestion() {
        if (questionList.isEmpty()) {
            questionText.setText("Sual tapılmadı.");
            return;
        }
        int randomIndex = new Random().nextInt(questionList.size());
        currentQuestion = questionList.get(randomIndex);
        questionText.setText("💡Questions: " + currentQuestion.getQuestion());
        answerText.setText(" ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayerBingo != null) {
            mediaPlayerBingo.release();
            mediaPlayerBingo = null;
        }
        if (mediaPlayerError != null) {
            mediaPlayerError.release();
            mediaPlayerError = null;
        }
    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(Terminal.this);
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

                    if (fAuth.getCurrentUser() != null) {
                        startActivity(new Intent(getApplicationContext(), LinuxMain.class));
                        finish();
                    }else{
                        startActivity(new Intent(Terminal.this, Login.class));
                        finish();
                    }

                }
            });
            materialAlertDialogBuilder.show();
        }
    };




}
