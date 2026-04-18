package com.ali.kali.console;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ali.MainActivity;
import com.ali.autorized.Login;
import com.ali.kali.LinuxMain;
import com.ali.systemIn.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Terminal extends AppCompatActivity {

    // UI Elements
    private TextView terminalOutput, questionText, answerText, connectionStatus;
    private EditText commandInput;
    private MaterialButton btnNextQuest, btnPrevQuest;
    private ImageButton btnSendCommand;
    private ScrollView scrollView;
    private MaterialCardView questionCard;
    private AdView mAdView6;

    // Data
    private List<Question> questionList = new ArrayList<>();
    private List<Question> answeredQuestions = new ArrayList<>();
    private Question currentQuestion;
    private int currentIndex = 0;
    private int score = 0;
    private int totalQuestions = 0;

    // Media
    private MediaPlayer mediaPlayerSuccess;
    private MediaPlayer mediaPlayerError;
    private MediaPlayer mediaPlayerComplete;

    // Firebase
    private FirebaseAuth fAuth;

    private ProgressBar progressBar;

    // Command History
    private List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;

    // Help commands
    private Map<String, String> helpCommands = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        initViews();
        setupToolbar();
        setupHelpCommands();
        loadCommandsFromJson();
        setupEventListeners();
        setupAnimations();
        loadAd();

        // Show first question
        showCurrentQuestion();
        appendToTerminal("Welcome to Kali Linux Terminal!\nType 'help' for available commands\nType 'score' to see your progress\n\n");

        getOnBackPressedDispatcher().addCallback(this, backCallback);
    }

    private void initViews() {
        terminalOutput = findViewById(R.id.terminal_output);
        commandInput = findViewById(R.id.command_input);
        questionText = findViewById(R.id.question_text);
        answerText = findViewById(R.id.answerText);
        btnNextQuest = findViewById(R.id.btnNextQuestions);
        btnPrevQuest = findViewById(R.id.btnPrevQuestion);
        btnSendCommand = findViewById(R.id.btnSendCommand);
        scrollView = findViewById(R.id.scrollView);
        questionCard = findViewById(R.id.questionCard);
        progressBar = findViewById(R.id.progressBar);
        connectionStatus = findViewById(R.id.connectionStatus);
        mAdView6 = findViewById(R.id.adView6);

        fAuth = FirebaseAuth.getInstance();

        // Initialize media players
        mediaPlayerSuccess = MediaPlayer.create(this, R.raw.accept);
        mediaPlayerError = MediaPlayer.create(this, R.raw.error2);
        mediaPlayerComplete = MediaPlayer.create(this, R.raw.accept);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupHelpCommands() {
        helpCommands.put("help", "Show all available commands");
        helpCommands.put("clear", "Clear terminal screen");
        helpCommands.put("score", "Show your current score");
        helpCommands.put("hint", "Show hint for current question");
        helpCommands.put("skip", "Skip current question");
        helpCommands.put("list", "Show all questions");
        helpCommands.put("reset", "Reset progress");
        helpCommands.put("exit", "Exit terminal");
        helpCommands.put("history", "Show command history");
        helpCommands.put("whoami", "Show current user");
        helpCommands.put("date", "Show current date and time");
        helpCommands.put("status", "Show connection status");
    }

    private void setupEventListeners() {
        // Send command button
        btnSendCommand.setOnClickListener(v -> {
            String input = commandInput.getText().toString().trim();
            if (!input.isEmpty()) {
                handleCommand(input);
                commandInput.setText("");
            }
        });

        // Enter key
        commandInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String input = commandInput.getText().toString().trim();
                if (!input.isEmpty()) {
                    handleCommand(input);
                    commandInput.setText("");
                }
                return true;
            }
            return false;
        });

        // Next question button
        btnNextQuest.setOnClickListener(v -> nextQuestion());

        // Previous question button
        btnPrevQuest.setOnClickListener(v -> previousQuestion());

        // Show answer on question click
        questionCard.setOnClickListener(v -> toggleAnswer());

        // Question text click
        questionText.setOnClickListener(v -> toggleAnswer());
    }

    private void setupAnimations() {
        // Pulse animation for connection status
        ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(),
                Color.parseColor("#00FF00"), Color.parseColor("#4CAF50"));
        colorAnim.setDuration(1000);
        colorAnim.setRepeatCount(ValueAnimator.INFINITE);
        colorAnim.setRepeatMode(ValueAnimator.REVERSE);
        colorAnim.addUpdateListener(animator -> {
            if (connectionStatus != null) {
                connectionStatus.setTextColor((int) animator.getAnimatedValue());
            }
        });
        colorAnim.start();
    }

    private void handleCommand(String cmd) {
        // Add to history
        commandHistory.add(cmd);
        historyIndex = commandHistory.size();

        // Add to terminal
        appendToTerminal("$ " + cmd + "\n");

        // Handle special commands
        if (handleSpecialCommands(cmd)) {
            return;
        }

        // Check if answer is correct
        if (currentQuestion != null && cmd.equalsIgnoreCase(currentQuestion.getAnswer())) {
            handleCorrectAnswer();
        } else {
            handleWrongAnswer(cmd);
        }

        // Scroll to top
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_UP));
    }

    private boolean handleSpecialCommands(String cmd) {
        String lowerCmd = cmd.toLowerCase();

        switch (lowerCmd) {
            case "help":
                showHelp();
                return true;
            case "clear":
                clearTerminal();
                return true;
            case "score":
                showScore();
                return true;
            case "hint":
                showHint();
                return true;
            case "skip":
                skipQuestion();
                return true;
            case "list":
                listQuestions();
                return true;
            case "reset":
                resetProgress();
                return true;
            case "exit":
                exitTerminal();
                return true;
            case "history":
                showHistory();
                return true;
            case "whoami":
                appendToTerminal("user@kali:~$ " + (fAuth.getCurrentUser() != null ?
                        fAuth.getCurrentUser().getEmail() : "guest") + "\n");
                return true;
            case "date":
                appendToTerminal(java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()) + "\n");
                return true;
            case "status":
                appendToTerminal("Connection: ONLINE\nServer: Kali Linux Terminal v2.0\nUptime: " +
                        android.text.format.DateUtils.formatElapsedTime(android.os.SystemClock.elapsedRealtime() / 1000) + "\n");
                return true;
        }
        return false;
    }

    private void showHelp() {
        StringBuilder help = new StringBuilder();
        help.append("┌─────────────────────────────────────────┐\n");
        help.append("│           AVAILABLE COMMANDS            │\n");
        help.append("├─────────────────────────────────────────┤\n");
        for (Map.Entry<String, String> entry : helpCommands.entrySet()) {
            help.append(String.format("│ %-10s : %-30s │\n", entry.getKey(), entry.getValue()));
        }
        help.append("└─────────────────────────────────────────┘\n");
        appendToTerminal(help.toString());
    }

    private void clearTerminal() {
        terminalOutput.setText("");
        appendToTerminal("Terminal cleared.\n");
    }

    private void showScore() {
        appendToTerminal(String.format("\n╔════════════════════════════╗\n" +
                        "║        YOUR SCORE           ║\n" +
                        "╠════════════════════════════╣\n" +
                        "║  Score: %d/%d               ║\n" +
                        "║  Progress: %.1f%%           ║\n" +
                        "╚════════════════════════════╝\n\n",
                score, totalQuestions, (totalQuestions > 0 ? (float) score / totalQuestions * 100 : 0)));
    }

    private void showHint() {
        if (currentQuestion != null && currentQuestion.getHint() != null) {
            appendToTerminal("💡 HINT: " + currentQuestion.getHint() + "\n");
        } else {
            appendToTerminal("💡 HINT: Use 'man " + (currentQuestion != null ? currentQuestion.getAnswer() : "command") + "' for help\n");
        }
    }

    private void skipQuestion() {
        appendToTerminal("⚠ Skipping current question...\n");
        nextQuestion();
    }

    private void listQuestions() {
        StringBuilder list = new StringBuilder();
        list.append("📋 QUESTIONS LIST:\n");
        for (int i = 0; i < questionList.size(); i++) {
            Question q = questionList.get(i);
            String status = answeredQuestions.contains(q) ? "✓" : "○";
            list.append(String.format("[%s] %d. %s\n", status, i + 1, q.getQuestion()));
        }
        appendToTerminal(list.toString());
    }

    private void resetProgress() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset Progress")
                .setMessage("Are you sure you want to reset all progress?")
                .setPositiveButton("Yes", (d, w) -> {
                    score = 0;
                    answeredQuestions.clear();
                    currentIndex = 0;
                    appendToTerminal("⚠ Progress has been reset!\n");
                    showCurrentQuestion();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void exitTerminal() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Exit Terminal")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (d, w) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    private void showHistory() {
        if (commandHistory.isEmpty()) {
            appendToTerminal("No command history.\n");
            return;
        }
        appendToTerminal("Command History:\n");
        for (int i = 0; i < commandHistory.size(); i++) {
            appendToTerminal(String.format("  %d: %s\n", i + 1, commandHistory.get(i)));
        }
    }

    private void handleCorrectAnswer() {
        score++;
        if (!answeredQuestions.contains(currentQuestion)) {
            answeredQuestions.add(currentQuestion);
        }

        String successMsg = String.format("✅ SUCCESS! +1 point\n📝 %s\n\n", currentQuestion.getDescription());
        appendToTerminal(successMsg);

        // Play success sound
        if (mediaPlayerSuccess != null) {
            mediaPlayerSuccess.start();
        }

        // Animate question card
        animateSuccess();

        // Update progress
        updateProgress();

        // Auto next question after delay
        new Handler().postDelayed(this::nextQuestion, 1500);
    }

    private void handleWrongAnswer(String cmd) {
        String errorMsg = String.format("❌ ERROR: '%s' command not found\n", cmd);
        appendToTerminal(errorMsg);

        // Play error sound
        if (mediaPlayerError != null) {
            mediaPlayerError.start();
        }

        // Shake animation
        animateError();
    }

    private void animateSuccess() {
        questionCard.animate()
                .scaleX(1.02f)
                .scaleY(1.02f)
                .setDuration(200)
                .withEndAction(() -> questionCard.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start())
                .start();
    }

    private void animateError() {
        commandInput.animate()
                .translationX(10f)
                .setDuration(50)
                .withEndAction(() -> commandInput.animate()
                        .translationX(-10f)
                        .setDuration(50)
                        .withEndAction(() -> commandInput.animate()
                                .translationX(0f)
                                .setDuration(50)
                                .start())
                        .start())
                .start();
    }

    private void updateProgress() {
        if (totalQuestions > 0) {
            int progress = (int) ((float) answeredQuestions.size() / totalQuestions * 100);
            progressBar.setProgress(progress);

            if (answeredQuestions.size() == totalQuestions && totalQuestions > 0) {
                if (mediaPlayerComplete != null) {
                    mediaPlayerComplete.start();
                }
                appendToTerminal("\n🎉 CONGRATULATIONS! 🎉\nYou have completed all " + totalQuestions + " questions!\nScore: " + score + "/" + totalQuestions + "\n\n");
            }
        }
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
                String hint = item.optString("hint", "Try 'man " + answer + "'");

                questionList.add(new Question(question, answer, description, hint));
            }

            totalQuestions = questionList.size();
            progressBar.setMax(totalQuestions);

        } catch (Exception e) {
            e.printStackTrace();
            appendToTerminal("Error loading commands.\n");
            loadDefaultQuestions();
        }
    }

    private void loadDefaultQuestions() {
        questionList.add(new Question("How to list files in directory?", "ls", "Lists all files and directories", "Try 'ls -la' for details"));
        questionList.add(new Question("How to change directory?", "cd", "Changes current directory", "Usage: cd /path/to/dir"));
        questionList.add(new Question("How to show current directory path?", "pwd", "Prints working directory", "Shows absolute path"));
        totalQuestions = questionList.size();
        progressBar.setMax(totalQuestions);
    }

    private void showCurrentQuestion() {
        if (currentIndex >= 0 && currentIndex < questionList.size()) {
            currentQuestion = questionList.get(currentIndex);
            questionText.setText("💡 " + currentQuestion.getQuestion());
            answerText.setVisibility(View.GONE);
            answerText.setText("");

            // Update progress display
            updateProgress();
        } else if (questionList.isEmpty()) {
            questionText.setText("No questions available.");
        }
    }

    private void nextQuestion() {
        if (currentIndex < questionList.size() - 1) {
            currentIndex++;
            showCurrentQuestion();
            appendToTerminal("\n▶ Moving to next question...\n\n");
        } else {
            appendToTerminal("\n🏁 You've reached the end!\nType 'reset' to start over or 'list' to see all questions.\n\n");
            if (mediaPlayerComplete != null) {
                mediaPlayerComplete.start();
            }
        }
    }

    private void previousQuestion() {
        if (currentIndex > 0) {
            currentIndex--;
            showCurrentQuestion();
            appendToTerminal("\n◀ Going back to previous question...\n\n");
        } else {
            appendToTerminal("\n⚠ This is the first question.\n\n");
        }
    }

    private void toggleAnswer() {
        if (currentQuestion != null) {
            if (answerText.getVisibility() == View.GONE) {
                answerText.setText("🔑 ANSWER: " + currentQuestion.getAnswer());
                answerText.setVisibility(View.VISIBLE);

                Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
                answerText.startAnimation(fadeIn);
            } else {
                Animation fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
                answerText.startAnimation(fadeOut);
                answerText.setVisibility(View.GONE);
            }
        }
    }

    private void appendToTerminal(String text) {
        runOnUiThread(() -> {
            String current = terminalOutput.getText().toString();
            terminalOutput.setText(current + text);
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void loadAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView6.loadAd(adRequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.terminal_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.menu_copy) {
            copyTerminalContent();
            return true;
        } else if (id == R.id.menu_clear) {
            clearTerminal();
            return true;
        } else if (id == R.id.menu_help) {
            showHelp();
            return true;
        } else if (id == R.id.menu_score) {
            showScore();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void copyTerminalContent() {
        String content = terminalOutput.getText().toString();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Terminal Output", content);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Terminal content copied", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayerSuccess != null) {
            mediaPlayerSuccess.release();
            mediaPlayerSuccess = null;
        }
        if (mediaPlayerError != null) {
            mediaPlayerError.release();
            mediaPlayerError = null;
        }
        if (mediaPlayerComplete != null) {
            mediaPlayerComplete.release();
            mediaPlayerComplete = null;
        }
    }

    OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            new MaterialAlertDialogBuilder(Terminal.this)
                    .setTitle("Exit Terminal")
                    .setMessage("Are you sure you want to exit?")
                    .setNegativeButton("No", (d, i) -> d.dismiss())
                    .setPositiveButton("Yes", (d, i) -> {
                        if (fAuth.getCurrentUser() != null) {
                            startActivity(new Intent(getApplicationContext(), LinuxMain.class));
                            finish();
                        } else {
                            startActivity(new Intent(Terminal.this, Login.class));
                            finish();
                        }
                    })
                    .show();
        }
    };

    // Question class with hint
    private static class Question {
        private String question;
        private String answer;
        private String description;
        private String hint;

        public Question(String question, String answer, String description, String hint) {
            this.question = question;
            this.answer = answer;
            this.description = description;
            this.hint = hint;
        }

        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
        public String getDescription() { return description; }
        public String getHint() { return hint; }
    }
}