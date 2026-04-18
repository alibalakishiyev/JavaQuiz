package com.ali.postgresql.taskPostgreSqlManager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.ali.systemIn.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class PostgreSqlTaskDetail extends AppCompatActivity {

    private static final String TAG = "SqlTaskDetail";
    private static final String PREFS_NAME = "SqlEditorPrefs";
    private static final String KEY_QUERY_HISTORY = "query_history";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final int MAX_HISTORY_SIZE = 20;
    private static final int PAGE_SIZE = 25;

    // ── SQL Keywords for syntax highlighting ──────────────────────
    private static final String[] SQL_KEYWORDS = {
            "SELECT", "FROM", "WHERE", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER",
            "ON", "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET", "UNION",
            "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE",
            "TABLE", "DROP", "ALTER", "ADD", "COLUMN", "INDEX", "PRIMARY", "KEY",
            "FOREIGN", "REFERENCES", "NOT", "NULL", "UNIQUE", "DEFAULT", "AUTO_INCREMENT",
            "AUTOINCREMENT", "AND", "OR", "IN", "BETWEEN", "LIKE", "IS", "AS",
            "DISTINCT", "COUNT", "SUM", "AVG", "MAX", "MIN", "CASE", "WHEN",
            "THEN", "ELSE", "END", "WITH", "RECURSIVE"
    };

    // ── SQL Autocomplete suggestions ──────────────────────────────
    private static final String[] SQL_SNIPPETS = {
            "SELECT * FROM ", "WHERE id = ", "ORDER BY ", "GROUP BY ", "HAVING ",
            "LIMIT 10", "INNER JOIN  ON ", "LEFT JOIN  ON ", "COUNT(*)", "AVG()",
            "SUM()", "MAX()", "MIN()", "DISTINCT ", "AS alias", "IS NOT NULL",
            "BETWEEN  AND ", "LIKE '%'", "IN ()", "CASE WHEN  THEN  END"
    };

    // ── Views ─────────────────────────────────────────────────────
    private TextView taskTitle, taskDescription, taskDifficulty;
    private TextView resultStats, initialQueryText, toolbarSubtitle;
    private TextView pageInfo, scrollHint, executionTimeView, connectionStatus;
    private EditText sqlQueryInput;
    private MaterialButton executeButton, showTablesButton, checkButton, btnAiHint;
    private MaterialButton showSolutionButton, exportButton, adminModeButton;
    private MaterialButton resetButton, newTableButton, btnPrevPage, btnNextPage, btnImport;
    private LinearLayout resultContainer, paginationContainer, adminSection;
    private LinearLayout lineNumberContainer, autoCompleteContainer;
    private HorizontalScrollView horizontalScrollView, autoCompleteScroll;
    private ScrollView verticalScrollView, lineNumberScroll;
    private ImageView btnFormatSql, btnHistory, btnChart, btnClearQuery;
    private ImageView btnUndo, btnRedo, btnCopyResult, btnThemeToggle;
    private Toolbar toolbar;
    private AdView SqlAdView;
    private NestedScrollView mainContainer;
    private ExtendedFloatingActionButton fabExecute;
    private LinearProgressIndicator taskProgress;
    private TextView taskProgressText, taskNumberBadge;
    private View statusDot;

    // ── State ─────────────────────────────────────────────────────
    private int taskId;
    private String correctSolution;
    private VirtualDatabaseHelper dbHelper;
    private boolean isAdminMode = false;
    private boolean isDarkMode = true;
    private List<List<String>> currentResult = null;
    private int currentPage = 0;
    private List<String> queryHistory = new ArrayList<>();
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private MediaPlayer successSound;
    private Vibrator vibrator;
    private SharedPreferences prefs;
    private Handler highlightHandler = new Handler();
    private Runnable highlightRunnable;
    private boolean isHighlighting = false;

    // ── Firebase ──────────────────────────────────────────────────
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;

    // ═════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ═════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postgre_sql_task_detail);

        initViews();
        initHelpers();
        applyTheme(isDarkMode);
        setupToolbar();
        loadIntentData();
        setupButtonListeners();
        setupSqlEditor();
        loadQueryHistory();
        animateEntrance();

        Log.d(TAG, "PostgreSqlTaskDetail started — Pro v2.0");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) dbHelper.close();
        if (successSound != null) successSound.release();
        highlightHandler.removeCallbacksAndMessages(null);
    }

    // ═════════════════════════════════════════════════════════════
    //  INIT
    // ═════════════════════════════════════════════════════════════

    private void initViews() {
        taskTitle         = findViewById(R.id.taskTitle);
        taskDescription   = findViewById(R.id.taskDescription);
        taskDifficulty    = findViewById(R.id.taskDifficulty);
        taskNumberBadge   = findViewById(R.id.taskNumberBadge);
        taskProgress      = findViewById(R.id.taskProgress);
        taskProgressText  = findViewById(R.id.taskProgressText);
        initialQueryText  = findViewById(R.id.initialQueryText);
        toolbarSubtitle   = findViewById(R.id.toolbarSubtitle);
        executionTimeView = findViewById(R.id.executionTime);
        connectionStatus  = findViewById(R.id.connectionStatus);
        statusDot         = findViewById(R.id.statusDot);

        sqlQueryInput      = findViewById(R.id.sqlQueryInput);
        executeButton      = findViewById(R.id.executeButton);
        showTablesButton   = findViewById(R.id.showTablesButton);
        checkButton        = findViewById(R.id.checkButton);
        showSolutionButton = findViewById(R.id.showSolutionButton);
        exportButton       = findViewById(R.id.exportButton);
        adminModeButton    = findViewById(R.id.adminModeButton);
        resetButton        = findViewById(R.id.resetButton);
        newTableButton     = findViewById(R.id.newTableButton);
        btnImport          = findViewById(R.id.btnImport);
        btnPrevPage        = findViewById(R.id.btnPrevPage);
        btnNextPage        = findViewById(R.id.btnNextPage);
        fabExecute         = findViewById(R.id.fabExecute);



        btnFormatSql  = findViewById(R.id.btnFormatSql);
        btnHistory    = findViewById(R.id.btnHistory);
        btnChart      = findViewById(R.id.btnChart);
        btnClearQuery = findViewById(R.id.btnClearQuery);
        btnUndo       = findViewById(R.id.btnUndo);
        btnRedo       = findViewById(R.id.btnRedo);
        btnCopyResult = findViewById(R.id.btnCopyResult);
        btnThemeToggle= findViewById(R.id.btnThemeToggle);
        btnAiHint = findViewById(R.id.btnAiHint);

        resultContainer      = findViewById(R.id.resultContainer);
        horizontalScrollView = findViewById(R.id.horizontalScrollView);
        verticalScrollView   = findViewById(R.id.verticalScrollView);
        lineNumberContainer  = findViewById(R.id.lineNumberContainer);
        lineNumberScroll     = findViewById(R.id.lineNumberScroll);
        autoCompleteContainer= findViewById(R.id.autoCompleteContainer);
        autoCompleteScroll   = findViewById(R.id.resultsHeader);
        scrollHint           = findViewById(R.id.scrollHint);
        resultStats          = findViewById(R.id.resultStats);
        paginationContainer  = findViewById(R.id.paginationContainer);
        pageInfo             = findViewById(R.id.pageInfo);
        adminSection         = findViewById(R.id.adminSection);
        mainContainer        = findViewById(R.id.mainContainer);
        toolbar              = findViewById(R.id.toolbar);
        SqlAdView            = findViewById(R.id.SqlAdView);
    }

    private void initHelpers() {
        dbHelper = new VirtualDatabaseHelper(this);
        prefs    = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true);
        try {
            successSound = MediaPlayer.create(this, R.raw.accept);
        } catch (Exception e) {
            Log.e(TAG, "Sound not loaded", e);
        }
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        if (btnThemeToggle != null) {
            btnThemeToggle.setOnClickListener(v -> toggleTheme());
        }
    }

    private void loadIntentData() {
        Intent intent = getIntent();
        if (intent == null) return;

        taskId = intent.getIntExtra("taskId", 0);
        String title       = intent.getStringExtra("title");
        String description = intent.getStringExtra("description");
        String initialQuery= intent.getStringExtra("initialQuery");
        correctSolution    = intent.getStringExtra("solution");
        String difficulty  = intent.getStringExtra("difficulty");

        // Task number badge
        if (taskNumberBadge != null) taskNumberBadge.setText(String.valueOf(taskId));
        if (title != null) taskTitle.setText(title);
        if (description != null) taskDescription.setText(description);

        if (initialQuery != null && !initialQuery.isEmpty()) {
            View card = findViewById(R.id.initialQueryCard);
            if (card != null) card.setVisibility(View.VISIBLE);
            initialQueryText.setText(initialQuery);
            sqlQueryInput.setText(initialQuery);
            updateLineNumbers(initialQuery);
        }

        if (difficulty != null) {
            taskDifficulty.setVisibility(View.VISIBLE);
            taskDifficulty.setText(difficulty);
            switch (difficulty) {
                case "Easy":
                    taskDifficulty.setBackgroundColor(Color.parseColor("#1A4CAF50"));
                    taskDifficulty.setTextColor(Color.parseColor("#4CAF50"));
                    break;
                case "Medium":
                    taskDifficulty.setBackgroundColor(Color.parseColor("#1AFF9800"));
                    taskDifficulty.setTextColor(Color.parseColor("#FF9800"));
                    break;
                case "Hard":
                    taskDifficulty.setBackgroundColor(Color.parseColor("#1AF44336"));
                    taskDifficulty.setTextColor(Color.parseColor("#F44336"));
                    break;
            }
        }

        // Animate progress bar
        animateProgressBar(35);
    }

    // ═════════════════════════════════════════════════════════════
    //  THEME
    // ═════════════════════════════════════════════════════════════

    private void applyTheme(boolean dark) {
        isDarkMode = dark;
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply();

        // CoordinatorLayout background
        CoordinatorLayout coordinator = findViewById(R.id.coordinatorLayout);
        if (coordinator != null) {
            coordinator.setBackgroundColor(ContextCompat.getColor(this,
                    dark ? R.color.bg_dark : R.color.bg_light));
        }

        // AppBarLayout
        AppBarLayout appBar = findViewById(R.id.appBarLayout);
        if (appBar != null) {
            appBar.setBackgroundColor(ContextCompat.getColor(this,
                    dark ? R.color.surface_dark : R.color.surface_light));
        }

        // Toolbar
        if (toolbar != null) {
            toolbar.setBackgroundColor(ContextCompat.getColor(this,
                    dark ? R.color.surface_dark : R.color.surface_light));
        }

        // Connection status text
        if (connectionStatus != null) {
            connectionStatus.setTextColor(ContextCompat.getColor(this,
                    dark ? R.color.text_secondary : R.color.text_secondary_light));
        }

        // Task title
        if (taskTitle != null) {
            taskTitle.setTextColor(ContextCompat.getColor(this,
                    dark ? R.color.text_primary : R.color.text_primary_light));
        }

        // Task description
        if (taskDescription != null) {
            taskDescription.setTextColor(ContextCompat.getColor(this,
                    dark ? R.color.text_secondary : R.color.text_secondary_light));
        }

        // SQL Input background and text
        if (sqlQueryInput != null) {
            sqlQueryInput.setBackgroundColor(ContextCompat.getColor(this,
                    dark ? R.color.editor_bg : R.color.editor_bg_light));
            sqlQueryInput.setTextColor(ContextCompat.getColor(this,
                    dark ? R.color.text_primary : R.color.text_primary_light));
            sqlQueryInput.setHintTextColor(ContextCompat.getColor(this,
                    dark ? R.color.text_hint : R.color.text_hint_light));
        }

        // Toolbar icons tint
        int iconColor = ContextCompat.getColor(this,
                dark ? R.color.text_secondary : R.color.text_secondary_light);

        // Theme toggle icon
        if (btnThemeToggle != null) {
            btnThemeToggle.setColorFilter(iconColor);
        }

        // Execute button
        if (executeButton != null) {
            executeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.accent_blue)));
        }

        // Admin mode button
        if (adminModeButton != null) {
            adminModeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, dark ? R.color.surface_dark_2 : R.color.surface_light_2)));
            adminModeButton.setTextColor(ContextCompat.getColor(this,
                    dark ? R.color.text_secondary : R.color.text_secondary_light));
        }

        // Scroll hint
        if (scrollHint != null) {
            scrollHint.setTextColor(ContextCompat.getColor(this,
                    dark ? R.color.text_secondary : R.color.text_secondary_light));
            scrollHint.setBackgroundColor(ContextCompat.getColor(this,
                    dark ? R.color.surface_dark_3 : R.color.surface_light_3));
        }

        // Result stats
        if (resultStats != null) {
            resultStats.setTextColor(ContextCompat.getColor(this,
                    dark ? R.color.accent_green : R.color.accent_green));
        }
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        applyTheme(isDarkMode);
        showToast(isDarkMode ? "🌙 Dark mode" : "☀️ Light mode");
    }

    // ═════════════════════════════════════════════════════════════
    //  SQL EDITOR SETUP
    // ═════════════════════════════════════════════════════════════

    private void setupSqlEditor() {
        // Line numbers
        updateLineNumbers("");

        // Text watcher for line numbers + highlighting + autocomplete
        sqlQueryInput.addTextChangedListener(new TextWatcher() {
            private String previousText = "";

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                previousText = s.toString();
            }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String newText = s.toString();
                updateLineNumbers(newText);
                scheduleHighlighting(newText);
                updateAutocomplete(newText);

                // Push to undo stack on meaningful change
                if (!newText.equals(previousText) && !previousText.isEmpty()) {
                    if (undoStack.isEmpty() || !undoStack.peek().equals(previousText)) {
                        undoStack.push(previousText);
                        redoStack.clear();
                    }
                }
            }
        });

        // Ctrl+Enter → run
        sqlQueryInput.setOnEditorActionListener((v, actionId, event) -> {
            if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.isCtrlPressed()) {
                executeSqlQuery();
                return true;
            }
            return false;
        });


        // Handle Delete/Backspace more smoothly
        sqlQueryInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DEL) {
                    // Better delete handling - if text is selected, delete it
                    int selStart = sqlQueryInput.getSelectionStart();
                    int selEnd = sqlQueryInput.getSelectionEnd();
                    if (selEnd > selStart) {
                        // Delete selected text
                        String text = sqlQueryInput.getText().toString();
                        String newText = text.substring(0, selStart) + text.substring(selEnd);
                        sqlQueryInput.setText(newText);
                        sqlQueryInput.setSelection(selStart);
                        return true;
                    }
                } else if (keyCode == KeyEvent.KEYCODE_FORWARD_DEL) {
                    // Handle forward delete on hardware keyboard
                    int selStart = sqlQueryInput.getSelectionStart();
                    if (selStart < sqlQueryInput.getText().length()) {
                        String text = sqlQueryInput.getText().toString();
                        String newText = text.substring(0, selStart) + text.substring(selStart + 1);
                        sqlQueryInput.setText(newText);
                        sqlQueryInput.setSelection(selStart);
                        return true;
                    }
                }
            }
            return false;
        });

        // Sync line number scroll with editor scroll
        sqlQueryInput.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                lineNumberScroll.scrollTo(0, scrollY));
    }

    private void updateLineNumbers(String text) {
        if (lineNumberContainer == null) return;
        lineNumberContainer.removeAllViews();

        String[] lines = text.isEmpty() ? new String[]{""} : text.split("\n", -1);
        int lineCount = Math.max(lines.length, 1);

        for (int i = 1; i <= lineCount; i++) {
            TextView lineNum = new TextView(this);
            lineNum.setText(String.valueOf(i));
            lineNum.setTextSize(11f);
            lineNum.setTextColor(Color.parseColor("#4A5568"));
            lineNum.setTypeface(Typeface.MONOSPACE);
            lineNum.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
            lineNum.setPadding(4, 2, 8, 2);
            lineNum.setMinWidth(dpToPx(28));
            lineNum.setHeight(dpToPx(20));
            lineNumberContainer.addView(lineNum);
        }
    }

    private void scheduleHighlighting(String text) {
        highlightHandler.removeCallbacks(highlightRunnable);
        highlightRunnable = () -> applySyntaxHighlighting(text);
        highlightHandler.postDelayed(highlightRunnable, 300);
    }

    /**
     * Basic SQL syntax highlighting using SpannableString.
     * Highlights keywords, strings, numbers, and comments.
     */
    private void applySyntaxHighlighting(String text) {
        if (isHighlighting || text == null || text.isEmpty()) return;
        isHighlighting = true;

        try {
            SpannableString spannable = new SpannableString(text);

            // Keyword color
            int keywordColor = Color.parseColor("#61DAFB");
            // String literal color
            int stringColor  = Color.parseColor("#98C379");
            // Number color
            int numberColor  = Color.parseColor("#D19A66");
            // Comment color
            int commentColor = Color.parseColor("#5C6370");

            // Highlight keywords (case-insensitive)
            for (String keyword : SQL_KEYWORDS) {
                int start = 0;
                String upperText = text.toUpperCase();
                while ((start = upperText.indexOf(keyword, start)) != -1) {
                    int end = start + keyword.length();
                    // Word boundary check
                    boolean validStart = (start == 0) || !Character.isLetterOrDigit(text.charAt(start - 1));
                    boolean validEnd   = (end >= text.length()) || !Character.isLetterOrDigit(text.charAt(end));
                    if (validStart && validEnd) {
                        spannable.setSpan(new ForegroundColorSpan(keywordColor),
                                start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    start = end;
                }
            }

            // Highlight string literals
            int pos = 0;
            while (pos < text.length()) {
                if (text.charAt(pos) == '\'') {
                    int closePos = text.indexOf('\'', pos + 1);
                    if (closePos != -1) {
                        spannable.setSpan(new ForegroundColorSpan(stringColor),
                                pos, closePos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        pos = closePos + 1;
                    } else break;
                } else {
                    pos++;
                }
            }

            // Highlight numbers
            java.util.regex.Matcher numMatcher =
                    java.util.regex.Pattern.compile("\\b\\d+(\\.\\d+)?\\b").matcher(text);
            while (numMatcher.find()) {
                spannable.setSpan(new ForegroundColorSpan(numberColor),
                        numMatcher.start(), numMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Highlight comments (-- style)
            java.util.regex.Matcher commentMatcher =
                    java.util.regex.Pattern.compile("--[^\n]*").matcher(text);
            while (commentMatcher.find()) {
                spannable.setSpan(new ForegroundColorSpan(commentColor),
                        commentMatcher.start(), commentMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Apply without triggering text watcher
            int cursorPos = sqlQueryInput.getSelectionStart();
            sqlQueryInput.removeTextChangedListener(null); // safely set text
            sqlQueryInput.setText(spannable);
            try {
                if (cursorPos <= spannable.length()) {
                    sqlQueryInput.setSelection(cursorPos);
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            Log.e(TAG, "Highlighting error", e);
        } finally {
            isHighlighting = false;
        }
    }

    private void updateAutocomplete(String text) {
        if (autoCompleteContainer == null || autoCompleteScroll == null) return;

        // Find current word being typed
        int cursor = sqlQueryInput.getSelectionStart();
        if (cursor <= 0 || text.isEmpty()) {
            autoCompleteScroll.setVisibility(View.GONE);
            return;
        }

        String before = text.substring(0, Math.min(cursor, text.length()));
        String[] words = before.split("[\\s,;()]+");
        String currentWord = words.length > 0 ? words[words.length - 1].toUpperCase() : "";

        if (currentWord.length() < 2) {
            autoCompleteScroll.setVisibility(View.GONE);
            return;
        }

        autoCompleteContainer.removeAllViews();
        boolean hasMatches = false;

        for (String keyword : SQL_KEYWORDS) {
            if (keyword.startsWith(currentWord) && !keyword.equals(currentWord)) {
                addAutocompleteChip(keyword);
                hasMatches = true;
            }
        }

        autoCompleteScroll.setVisibility(hasMatches ? View.VISIBLE : View.GONE);
    }

    private void addAutocompleteChip(String keyword) {
        TextView chip = new TextView(this);
        chip.setText(keyword);
        chip.setTextSize(11f);
        chip.setTextColor(Color.parseColor("#61DAFB"));
        chip.setTypeface(Typeface.MONOSPACE);
        chip.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        chip.setBackground(ContextCompat.getDrawable(this, R.drawable.chip_outline));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(dpToPx(6));
        chip.setLayoutParams(params);

        chip.setOnClickListener(v -> {
            // Insert keyword at cursor position
            int cursor = sqlQueryInput.getSelectionStart();
            String current = sqlQueryInput.getText().toString();
            // Remove partial word and insert full keyword
            int wordStart = Math.max(0, cursor - 1);
            while (wordStart > 0 && Character.isLetter(current.charAt(wordStart - 1))) wordStart--;
            String newText = current.substring(0, wordStart) + keyword + " " + current.substring(cursor);
            sqlQueryInput.setText(newText);
            sqlQueryInput.setSelection(wordStart + keyword.length() + 1);
            autoCompleteScroll.setVisibility(View.GONE);
        });

        autoCompleteContainer.addView(chip);
    }

    // ═════════════════════════════════════════════════════════════
    //  BUTTON LISTENERS
    // ═════════════════════════════════════════════════════════════

    private void setupButtonListeners() {
        // Primary actions
        executeButton.setOnClickListener(v -> { pulseButton(executeButton); executeSqlQuery(); });
        fabExecute.setOnClickListener(v -> executeSqlQuery());
        showTablesButton.setOnClickListener(v -> showTableList());
        checkButton.setOnClickListener(v -> checkTaskSolution());
        showSolutionButton.setOnClickListener(v -> showSolution());
        adminModeButton.setOnClickListener(v -> toggleAdminMode());
        resetButton.setOnClickListener(v -> confirmResetDatabase());
        newTableButton.setOnClickListener(v -> showCreateTableDialog());
        exportButton.setOnClickListener(v -> exportResults());
        btnPrevPage.setOnClickListener(v -> previousPage());
        btnNextPage.setOnClickListener(v -> nextPage());

        // Editor toolbar
        if (btnFormatSql != null)  btnFormatSql.setOnClickListener(v -> formatSqlQuery());
        if (btnHistory != null)    btnHistory.setOnClickListener(v -> showQueryHistory());
        if (btnChart != null)      btnChart.setOnClickListener(v -> showChartDialog());
        if (btnClearQuery != null) btnClearQuery.setOnClickListener(v -> confirmClearQuery());
        if (btnUndo != null)       btnUndo.setOnClickListener(v -> undoAction());
        if (btnRedo != null)       btnRedo.setOnClickListener(v -> redoAction());
        if (btnCopyResult != null) btnCopyResult.setOnClickListener(v -> copyToClipboard());
        if (btnImport != null)     btnImport.setOnClickListener(v -> showImportDialog());
        if (btnAiHint != null)     btnAiHint.setOnClickListener(v -> showAiHint());

        // SQL keyword chips
        setupChip(R.id.chipSelect,  "SELECT * FROM ");
        setupChip(R.id.chipWhere,   "WHERE ");
        setupChip(R.id.chipJoin,    "INNER JOIN  ON ");
        setupChip(R.id.chipGroupBy, "GROUP BY ");
        setupChip(R.id.chipOrderBy, "ORDER BY ");
        setupChip(R.id.chipHaving,  "HAVING ");
        setupChip(R.id.chipLimit,   "LIMIT 10");

        // FAB scroll behavior — hide when scrolling down
        mainContainer.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    if (scrollY > oldScrollY + 10) fabExecute.shrink();
                    else if (scrollY < oldScrollY - 10) fabExecute.extend();
                });

        // Ads
        if (SqlAdView != null) {
            SqlAdView.loadAd(new AdRequest.Builder().build());
        }

        // Back press
        getOnBackPressedDispatcher().addCallback(this, backCallback);
    }

    private void setupChip(int chipId, String text) {
        Chip chip = findViewById(chipId);
        if (chip == null) return;
        chip.setOnClickListener(v -> {
            int cursor = sqlQueryInput.getSelectionStart();
            String current = sqlQueryInput.getText().toString();
            String newText = current.substring(0, cursor) + text + current.substring(cursor);
            sqlQueryInput.setText(newText);
            sqlQueryInput.setSelection(cursor + text.length());
            sqlQueryInput.requestFocus();
        });
    }

    // ═════════════════════════════════════════════════════════════
    //  SQL EXECUTION
    // ═════════════════════════════════════════════════════════════

    private void executeSqlQuery() {
        String query = sqlQueryInput.getText().toString().trim();
        if (TextUtils.isEmpty(query)) {
            showError("Please enter a SQL query");
            return;
        }

        addToHistory(query);

        // Show running state
        setExecutingState(true);

        new Handler().postDelayed(() -> {
            long start = System.currentTimeMillis();
            List<List<String>> result = dbHelper.executeQuery(query);
            long elapsed = System.currentTimeMillis() - start;

            runOnUiThread(() -> {
                setExecutingState(false);
                currentResult = result;
                currentPage = 0;

                // Update status bar
                if (executionTimeView != null) {
                    executionTimeView.setText(elapsed + "ms");
                }

                displayPaginatedResult(result, elapsed);
                hapticFeedback();

                // Scroll to results
                new Handler().postDelayed(() ->
                        mainContainer.smoothScrollTo(0, resultContainer.getTop()), 300);
            });
        }, 150);
    }

    private void setExecutingState(boolean running) {
        if (executeButton != null) {
            executeButton.setEnabled(!running);
            executeButton.setText(running ? "Running..." : "▶  Run Query");
        }
        if (fabExecute != null) {
            fabExecute.setEnabled(!running);
        }
        if (statusDot != null && connectionStatus != null) {
            if (running) {
                connectionStatus.setText("  Executing query...");
                // Pulse animation on statusDot
                ObjectAnimator pulse = ObjectAnimator.ofFloat(statusDot, "alpha", 1f, 0.2f);
                pulse.setDuration(500);
                pulse.setRepeatCount(ValueAnimator.INFINITE);
                pulse.setRepeatMode(ValueAnimator.REVERSE);
                statusDot.setTag(pulse);
                pulse.start();
            } else {
                connectionStatus.setText("  Connected to SQLite • In-Memory DB");
                Object tag = statusDot.getTag();
                if (tag instanceof ObjectAnimator) ((ObjectAnimator) tag).cancel();
                statusDot.setAlpha(1f);
            }
        }
    }

    private void displayPaginatedResult(List<List<String>> result, long execTime) {
        resultContainer.removeAllViews();
        paginationContainer.setVisibility(View.GONE);
        if (btnCopyResult != null) btnCopyResult.setVisibility(View.GONE);

        if (result == null || result.isEmpty()) {
            showEmptyState("No results");
            return;
        }

        // Error check
        if (result.size() == 1 && result.get(0).size() == 2
                && "Xəta".equals(result.get(0).get(0))) {
            showErrorState(result.get(0).get(1));
            return;
        }

        // Success — show copy button
        if (btnCopyResult != null) btnCopyResult.setVisibility(View.VISIBLE);
        if (btnChart != null) btnChart.setVisibility(View.VISIBLE);

        int rowCount = result.size() - 1;

        // Stats
        if (resultStats != null) {
            resultStats.setVisibility(View.VISIBLE);
            resultStats.setText(rowCount + " rows • " + execTime + "ms");
        }

        // Pagination
        if (rowCount > PAGE_SIZE) {
            int totalPages = (int) Math.ceil((double) rowCount / PAGE_SIZE);
            pageInfo.setText((currentPage + 1) + " / " + totalPages);
            paginationContainer.setVisibility(View.VISIBLE);
        }

        int startRow = currentPage * PAGE_SIZE;
        int endRow   = Math.min(startRow + PAGE_SIZE, rowCount);

        List<List<String>> page = new ArrayList<>();
        page.add(result.get(0));
        for (int i = startRow + 1; i <= endRow; i++) {
            page.add(result.get(i));
        }

        displayTable(page);
    }

    private void displayTable(List<List<String>> result) {
        if (result == null || result.isEmpty()) return;

        List<String> headers = result.get(0);
        int colCount = headers.size();

        // Calculate column widths
        int[] colWidths = new int[colCount];
        for (int c = 0; c < colCount; c++) {
            colWidths[c] = Math.max(headers.get(c).length(), 8);
            for (int r = 1; r < result.size(); r++) {
                if (c < result.get(r).size()) {
                    colWidths[c] = Math.max(colWidths[c], Math.min(result.get(r).get(c).length(), 24));
                }
            }
            colWidths[c] = dpToPx(colWidths[c] * 7 + 24); // char width estimate
        }

        TableLayout table = new TableLayout(this);
        table.setStretchAllColumns(false);
        table.setShrinkAllColumns(false);
        table.setPadding(0, 0, 0, 0);

        // ── Header row ─────────────────────────────────────────
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.parseColor("#0D61DAFB")); // subtle blue

        for (int i = 0; i < headers.size(); i++) {
            TextView cell = buildHeaderCell(headers.get(i), colWidths[i]);
            headerRow.addView(cell);

            // Divider
            if (i < headers.size() - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new TableRow.LayoutParams(dpToPx(1), TableRow.LayoutParams.MATCH_PARENT));
                divider.setBackgroundColor(Color.parseColor("#2D3748"));
                headerRow.addView(divider);
            }
        }
        table.addView(headerRow);

        // Header bottom border
        View headerBorder = new View(this);
        headerBorder.setLayoutParams(new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
        headerBorder.setBackgroundColor(Color.parseColor("#61DAFB40"));
        table.addView(headerBorder);

        // ── Data rows ──────────────────────────────────────────
        for (int r = 1; r < result.size(); r++) {
            TableRow dataRow = new TableRow(this);
            boolean isEven = r % 2 == 0;
            dataRow.setBackgroundColor(isEven
                    ? Color.parseColor("#0A1A202C")
                    : Color.parseColor("#00000000"));

            List<String> row = result.get(r);
            for (int c = 0; c < row.size(); c++) {
                TextView cell = buildDataCell(row.get(c), colWidths[Math.min(c, colWidths.length - 1)]);
                int finalR = r;
                int finalC = c;
                cell.setOnLongClickListener(v -> {
                    showCellDetails(row.get(finalC), headers.get(finalC), finalR);
                    return true;
                });
                dataRow.addView(cell);

                if (c < row.size() - 1) {
                    View div = new View(this);
                    div.setLayoutParams(new TableRow.LayoutParams(dpToPx(1), TableRow.LayoutParams.MATCH_PARENT));
                    div.setBackgroundColor(Color.parseColor("#1A2D3748"));
                    dataRow.addView(div);
                }
            }

            // Row separator
            table.addView(dataRow);
            View rowSep = new View(this);
            rowSep.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
            rowSep.setBackgroundColor(Color.parseColor("#0D2D3748"));
            table.addView(rowSep);
        }

        resultContainer.addView(table);

        // Scroll hint
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int tableWidth  = 0;
        for (int w : colWidths) tableWidth += w;

        if (tableWidth > screenWidth * 0.9 && scrollHint != null) {
            scrollHint.setVisibility(View.VISIBLE);
        } else if (scrollHint != null) {
            scrollHint.setVisibility(View.GONE);
        }

        // Reset scroll
        horizontalScrollView.post(() -> {
            horizontalScrollView.scrollTo(0, 0);
            verticalScrollView.scrollTo(0, 0);
        });

        // Animate table in
        table.setAlpha(0f);
        table.animate().alpha(1f).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    private TextView buildHeaderCell(String text, int minWidth) {
        TextView cell = new TextView(this);
        cell.setText(text.toUpperCase());
        cell.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        cell.setTextColor(Color.parseColor("#61DAFB"));
        cell.setTextSize(11f);
        cell.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        cell.setMinWidth(minWidth);
        cell.setMaxWidth(dpToPx(260));
        cell.setSingleLine(true);
        cell.setEllipsize(android.text.TextUtils.TruncateAt.END);
        cell.setLetterSpacing(0.05f);
        return cell;
    }

    private TextView buildDataCell(String text, int minWidth) {
        TextView cell = new TextView(this);
        String display = (text == null || text.equals("null")) ? "NULL" : text;

        cell.setText(display);
        cell.setTypeface(Typeface.MONOSPACE);
        cell.setTextSize(12f);
        cell.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        cell.setMinWidth(minWidth);
        cell.setMaxWidth(dpToPx(260));
        cell.setSingleLine(true);
        cell.setEllipsize(android.text.TextUtils.TruncateAt.END);

        // Color by type
        if (display.equals("NULL")) {
            cell.setTextColor(Color.parseColor("#4A5568"));
            cell.setTypeface(Typeface.MONOSPACE, Typeface.ITALIC);
        } else if (display.matches("-?\\d+(\\.\\d+)?")) {
            cell.setTextColor(Color.parseColor("#D19A66")); // number = orange
        } else if (display.equalsIgnoreCase("true") || display.equalsIgnoreCase("false")) {
            cell.setTextColor(display.equalsIgnoreCase("true")
                    ? Color.parseColor("#98C379")
                    : Color.parseColor("#E06C75"));
        } else {
            cell.setTextColor(Color.parseColor("#ABB2BF")); // text = light gray
        }

        return cell;
    }

    private void showCellDetails(String value, String column, int row) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Column: " + column + "  •  Row " + row)
                .setMessage(value)
                .setPositiveButton("Copy", (d, w) -> {
                    ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cb.setPrimaryClip(ClipData.newPlainText("cell", value));
                    showToast("Copied");
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // ═════════════════════════════════════════════════════════════
    //  TABLES PANEL
    // ═════════════════════════════════════════════════════════════

    private void showTableList() {
        List<String> tables = dbHelper.getTableList();
        resultContainer.removeAllViews();
        paginationContainer.setVisibility(View.GONE);
        scrollHint.setVisibility(View.GONE);

        if (resultStats != null) {
            resultStats.setVisibility(View.VISIBLE);
            resultStats.setText(tables.size() + " tables");
        }

        // Section header
        resultContainer.addView(buildSectionHeader("Database Explorer", "#61DAFB"));

        Map<String, Object> stats = dbHelper.getDatabaseStats();
        TextView statView = new TextView(this);
        statView.setText("Tables: " + stats.get("table_count") + " • Total rows: " + stats.get("total_rows"));
        statView.setTextSize(11f);
        statView.setTextColor(Color.parseColor("#718096"));
        statView.setPadding(dpToPx(8), 0, dpToPx(8), dpToPx(12));
        statView.setTypeface(Typeface.MONOSPACE);
        resultContainer.addView(statView);

        for (String tableName : tables) {
            resultContainer.addView(buildTableCard(tableName));
        }
    }

    private View buildTableCard(String tableName) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dpToPx(12));
        card.setCardElevation(0);
        card.setStrokeColor(Color.parseColor("#2D3748"));
        card.setStrokeWidth(dpToPx(1));
        card.setCardBackgroundColor(Color.parseColor("#1A2D3748"));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(dpToPx(4), 0, dpToPx(4), dpToPx(10));
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));

        // Table name
        TextView nameView = new TextView(this);
        nameView.setText("⊞  " + tableName);
        nameView.setTextSize(14f);
        nameView.setTextColor(Color.parseColor("#E2E8F0"));
        nameView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        LinearLayout.LayoutParams nameParams = (LinearLayout.LayoutParams) nameView.getLayoutParams();
        if (nameParams == null) {
            nameParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
        }
        nameParams.bottomMargin = dpToPx(10);
        nameView.setLayoutParams(nameParams);
        content.addView(nameView);

        // Action buttons
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        btnRow.addView(buildMiniButton("SELECT *", "#61DAFB20", "#61DAFB", v -> {
            sqlQueryInput.setText("SELECT * FROM " + tableName + " LIMIT 20;");
            executeSqlQuery();
        }));

        btnRow.addView(buildMiniButton("Structure", "#98C37920", "#98C379", v ->
                showTableStructure(tableName)));

        btnRow.addView(buildMiniButton("Details", "#D19A6620", "#D19A66", v ->
                showTableDetails(tableName)));

        if (isAdminMode) {
            btnRow.addView(buildMiniButton("Drop ⚠", "#E06C7520", "#E06C75", v ->
                    dropTable(tableName)));
        }

        content.addView(btnRow);
        card.addView(content);
        return card;
    }

    private MaterialButton buildMiniButton(String text, String bgHex, String textHex, View.OnClickListener listener) {
        MaterialButton btn = new MaterialButton(this, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btn.setText(text);
        btn.setTextSize(11f);
        btn.setTextColor(Color.parseColor(textHex));
        btn.setStrokeColor(android.content.res.ColorStateList.valueOf(Color.parseColor(textHex + "60")));
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(bgHex)));
        btn.setCornerRadius(dpToPx(8));
        btn.setOnClickListener(listener);
        btn.setAllCaps(false);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dpToPx(32), 1f);
        params.setMarginEnd(dpToPx(6));
        btn.setLayoutParams(params);
        return btn;
    }

    private void showTableDetails(String tableName) {
        resultContainer.removeAllViews();
        resultContainer.addView(buildSectionHeader("Table: " + tableName, "#D19A66"));

        List<List<String>> structure = dbHelper.getTableStructure(tableName);
        displayTable(structure);

        List<List<String>> sample = dbHelper.getSampleData(tableName, 10);
        if (sample != null && sample.size() > 1) {
            resultContainer.addView(buildSectionHeader("Sample Data (first 10)", "#98C379"));
            displayTable(sample);
        }
    }

    private void showTableStructure(String tableName) {
        List<List<String>> structure = dbHelper.getTableStructure(tableName);
        displayTable(structure);
    }

    // ═════════════════════════════════════════════════════════════
    //  TASK CHECK
    // ═════════════════════════════════════════════════════════════

    private void checkTaskSolution() {
        String userQuery = sqlQueryInput.getText().toString().trim();
        if (TextUtils.isEmpty(userQuery)) { showError("Enter a SQL query first"); return; }
        if (correctSolution == null || correctSolution.isEmpty()) { showError("No solution data"); return; }

        new Handler().postDelayed(() -> {
            List<List<String>> userResult    = dbHelper.executeQuery(userQuery);
            List<List<String>> correctResult = dbHelper.executeQuery(correctSolution);

            if (compareResults(userResult, correctResult)) {
                animateSuccess();
                playSuccessSound();
                saveTaskCompletion();
                animateProgressBar(100);

                new Handler().postDelayed(() -> {
                    Intent result = new Intent();
                    result.putExtra("taskId", taskId);
                    result.putExtra("completed", true);
                    setResult(RESULT_OK, result);
                    finish();
                }, 2000);
            } else {
                animateError();
                displayComparison(userResult, correctResult);
            }
        }, 200);
    }

    private boolean compareResults(List<List<String>> user, List<List<String>> correct) {
        if (user == null || correct == null) return false;
        if (user.size() != correct.size()) return false;
        for (int i = 0; i < user.size(); i++) {
            if (user.get(i).size() != correct.get(i).size()) return false;
            for (int j = 0; j < user.get(i).size(); j++) {
                if (!user.get(i).get(j).equals(correct.get(i).get(j))) return false;
            }
        }
        return true;
    }

    private void displayComparison(List<List<String>> user, List<List<String>> correct) {
        resultContainer.removeAllViews();
        resultContainer.addView(buildSectionHeader("Your Result", "#E06C75"));
        if (user != null && !user.isEmpty()) displayTable(user);
        resultContainer.addView(buildSectionHeader("Expected Result", "#98C379"));
        if (correct != null && !correct.isEmpty()) displayTable(correct);
    }

    private void showSolution() {
        if (correctSolution == null || correctSolution.isEmpty()) {
            showError("No solution available");
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("💡 Solution")
                .setMessage("This will replace your current query with the solution. Continue?")
                .setPositiveButton("Show Solution", (d, w) -> {
                    sqlQueryInput.setText(correctSolution);
                    executeSqlQuery();
                    showSuccess("Solution applied");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ═════════════════════════════════════════════════════════════
    //  ADMIN MODE
    // ═════════════════════════════════════════════════════════════

    private void toggleAdminMode() {
        isAdminMode = !isAdminMode;
        if (isAdminMode) {
            adminModeButton.setText("🔓  Admin Mode: ON");
            adminModeButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#1A98C379")));
            adminModeButton.setTextColor(Color.parseColor("#98C379"));
            adminSection.setVisibility(View.VISIBLE);
            exportButton.setVisibility(View.VISIBLE);
            if (btnChart != null) btnChart.setVisibility(View.VISIBLE);
            showSuccess("Admin mode enabled");
        } else {
            adminModeButton.setText("🔒  Admin Mode: OFF");
            adminModeButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#1A718096")));
            adminModeButton.setTextColor(Color.parseColor("#718096"));
            adminSection.setVisibility(View.GONE);
            exportButton.setVisibility(View.GONE);
            if (btnChart != null) btnChart.setVisibility(View.GONE);
            showToast("Admin mode disabled");
        }
        showTableList();
    }

    private void confirmResetDatabase() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("⚠ Reset Database")
                .setMessage("All data will be permanently deleted. This cannot be undone.")
                .setPositiveButton("Reset", (d, w) -> {
                    dbHelper.resetDatabase();
                    showSuccess("Database reset");
                    showTableList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCreateTableDialog() {
        String[] templates = {"Students table", "Products table", "Orders table", "Custom..."};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Create New Table")
                .setItems(templates, (d, which) -> {
                    String sql = "";
                    switch (which) {
                        case 0:
                            sql = "CREATE TABLE students (\n  id INTEGER PRIMARY KEY AUTOINCREMENT,\n  name TEXT NOT NULL,\n  grade INTEGER,\n  major TEXT\n);\n\nINSERT INTO students (name, grade, major) VALUES\n  ('Ali', 85, 'Computer Science'),\n  ('Ayşe', 92, 'Mathematics'),\n  ('Mehmet', 78, 'Physics');";
                            break;
                        case 1:
                            sql = "CREATE TABLE products (\n  id INTEGER PRIMARY KEY AUTOINCREMENT,\n  name TEXT NOT NULL,\n  price REAL,\n  stock INTEGER\n);\n\nINSERT INTO products (name, price, stock) VALUES\n  ('Laptop', 1500.00, 10),\n  ('Mouse', 25.50, 50),\n  ('Keyboard', 75.00, 30);";
                            break;
                        case 2:
                            sql = "CREATE TABLE orders (\n  id INTEGER PRIMARY KEY AUTOINCREMENT,\n  customer_name TEXT,\n  order_date TEXT,\n  total REAL\n);\n\nINSERT INTO orders (customer_name, order_date, total) VALUES\n  ('Ali', '2024-01-15', 250.00),\n  ('Ayşe', '2024-01-20', 1250.00);";
                            break;
                        case 3:
                            sql = "CREATE TABLE my_table (\n  id INTEGER PRIMARY KEY AUTOINCREMENT,\n  column1 TEXT,\n  column2 INTEGER\n);";
                            break;
                    }
                    sqlQueryInput.setText(sql);
                })
                .show();
    }

    private void dropTable(String tableName) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Drop Table")
                .setMessage("Drop '" + tableName + "'? This cannot be undone.")
                .setPositiveButton("Drop", (d, w) -> {
                    if (dbHelper.dropTable(tableName)) {
                        showSuccess("Table dropped: " + tableName);
                        showTableList();
                    } else {
                        showError("Failed to drop table");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ═════════════════════════════════════════════════════════════
    //  HISTORY & UNDO
    // ═════════════════════════════════════════════════════════════

    private void addToHistory(String query) {
        if (TextUtils.isEmpty(query)) return;
        queryHistory.remove(query);
        queryHistory.add(0, query);
        while (queryHistory.size() > MAX_HISTORY_SIZE) queryHistory.remove(queryHistory.size() - 1);
        saveQueryHistory();
    }

    private void saveQueryHistory() {
        Set<String> set = new LinkedHashSet<>(queryHistory);
        prefs.edit().putStringSet(KEY_QUERY_HISTORY, set).apply();
    }

    private void loadQueryHistory() {
        Set<String> set = prefs.getStringSet(KEY_QUERY_HISTORY, new LinkedHashSet<>());
        queryHistory.clear();
        queryHistory.addAll(set);
    }

    private void showQueryHistory() {
        if (queryHistory.isEmpty()) { showToast("History is empty"); return; }
        String[] items = queryHistory.toArray(new String[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Query History  (" + queryHistory.size() + ")")
                .setItems(items, (d, w) -> {
                    sqlQueryInput.setText(items[w]);
                    executeSqlQuery();
                })
                .setPositiveButton("Clear All", (d, w) -> {
                    queryHistory.clear();
                    saveQueryHistory();
                    showToast("History cleared");
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void undoAction() {
        if (!undoStack.isEmpty()) {
            redoStack.push(sqlQueryInput.getText().toString());
            String prev = undoStack.pop();
            isHighlighting = true;
            sqlQueryInput.setText(prev);
            sqlQueryInput.setSelection(prev.length());
            isHighlighting = false;
        } else {
            showToast("Nothing to undo");
        }
    }

    private void redoAction() {
        if (!redoStack.isEmpty()) {
            undoStack.push(sqlQueryInput.getText().toString());
            String next = redoStack.pop();
            isHighlighting = true;
            sqlQueryInput.setText(next);
            sqlQueryInput.setSelection(next.length());
            isHighlighting = false;
        } else {
            showToast("Nothing to redo");
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  FORMAT SQL
    // ═════════════════════════════════════════════════════════════

    private void formatSqlQuery() {
        String q = sqlQueryInput.getText().toString()
                .replaceAll("(?i)\\bselect\\b", "\nSELECT")
                .replaceAll("(?i)\\bfrom\\b",   "\nFROM")
                .replaceAll("(?i)\\bwhere\\b",  "\nWHERE")
                .replaceAll("(?i)\\bjoin\\b",   "\nJOIN")
                .replaceAll("(?i)\\bleft join\\b",  "\nLEFT JOIN")
                .replaceAll("(?i)\\bright join\\b", "\nRIGHT JOIN")
                .replaceAll("(?i)\\binner join\\b",  "\nINNER JOIN")
                .replaceAll("(?i)\\bgroup by\\b", "\nGROUP BY")
                .replaceAll("(?i)\\border by\\b", "\nORDER BY")
                .replaceAll("(?i)\\bhaving\\b",  "\nHAVING")
                .replaceAll("(?i)\\blimit\\b",   "\nLIMIT")
                .replaceAll("(?i)\\boffset\\b",  "\nOFFSET")
                .replaceAll("(?i)\\band\\b",     "\n  AND")
                .replaceAll("(?i)\\bor\\b",      "\n  OR")
                .trim();
        isHighlighting = true;
        sqlQueryInput.setText(q);
        isHighlighting = false;
        showToast("SQL formatted");
    }

    // ═════════════════════════════════════════════════════════════
    //  EXPORT
    // ═════════════════════════════════════════════════════════════

    private void exportResults() {
        if (currentResult == null || currentResult.isEmpty()) {
            showError("No results to export");
            return;
        }
        String[] options = {"JSON", "CSV", "Copy to clipboard"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Export Results")
                .setItems(options, (d, w) -> {
                    switch (w) {
                        case 0: exportAsJson(); break;
                        case 1: exportAsCsv();  break;
                        case 2: copyToClipboard(); break;
                    }
                })
                .show();
    }

    private void exportAsJson() {
        try {
            JSONArray arr = new JSONArray();
            if (currentResult != null && currentResult.size() > 0) {
                List<String> headers = currentResult.get(0);
                for (int i = 1; i < currentResult.size(); i++) {
                    org.json.JSONObject obj = new org.json.JSONObject();
                    for (int j = 0; j < currentResult.get(i).size(); j++) {
                        obj.put(j < headers.size() ? headers.get(j) : "col" + j,
                                currentResult.get(i).get(j));
                    }
                    arr.put(obj);
                }
            }
            String fname = "sql_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".json";
            File file = new File(getExternalFilesDir(null), fname);
            try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(file))) {
                w.write(arr.toString(2));
            }
            showSuccess("Exported: " + fname);
        } catch (Exception e) {
            showError("Export failed: " + e.getMessage());
        }
    }

    private void exportAsCsv() {
        try {
            StringBuilder csv = new StringBuilder();
            if (currentResult != null && !currentResult.isEmpty()) {
                csv.append(String.join(",", currentResult.get(0))).append("\n");
                for (int i = 1; i < currentResult.size(); i++) {
                    List<String> row = currentResult.get(i);
                    StringBuilder line = new StringBuilder();
                    for (int j = 0; j < row.size(); j++) {
                        if (j > 0) line.append(",");
                        line.append("\"").append(row.get(j).replace("\"", "\"\"")).append("\"");
                    }
                    csv.append(line).append("\n");
                }
            }
            String fname = "sql_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".csv";
            File file = new File(getExternalFilesDir(null), fname);
            try (OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(file))) {
                w.write(csv.toString());
            }
            showSuccess("CSV exported: " + fname);
        } catch (Exception e) {
            showError("Export failed: " + e.getMessage());
        }
    }

    private void copyToClipboard() {
        if (currentResult == null || currentResult.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (List<String> row : currentResult) {
            sb.append(String.join("\t", row)).append("\n");
        }
        ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cb.setPrimaryClip(ClipData.newPlainText("SQL", sb.toString()));
        showToast("Copied to clipboard");
    }

    // ═════════════════════════════════════════════════════════════
    //  AI HINT
    // ═════════════════════════════════════════════════════════════

    private void showAiHint() {
        // Smart hint based on task description / query state
        String hint = generateContextualHint();
        new MaterialAlertDialogBuilder(this)
                .setTitle("💡 SQL Hint")
                .setMessage(hint)
                .setPositiveButton("Apply Snippet", (d, w) -> {
                    String snippet = extractSnippet(hint);
                    if (!snippet.isEmpty()) {
                        sqlQueryInput.append("\n-- Hint: " + snippet);
                    }
                })
                .setNegativeButton("Got it", null)
                .show();
    }

    private String generateContextualHint() {
        String query = sqlQueryInput.getText().toString().toUpperCase();
        if (query.contains("GROUP BY") && !query.contains("HAVING")) {
            return "💡 Tip: Use HAVING to filter grouped results.\n\nExample:\nSELECT department, COUNT(*)\nFROM employees\nGROUP BY department\nHAVING COUNT(*) > 2;";
        } else if (query.contains("JOIN") && !query.contains("ON")) {
            return "💡 Tip: Don't forget the ON clause with JOIN!\n\nExample:\nSELECT * FROM a\nINNER JOIN b ON a.id = b.a_id;";
        } else if (!query.contains("WHERE") && query.contains("SELECT")) {
            return "💡 Tip: Add a WHERE clause to filter results.\n\nExample:\nWHERE salary > 3000\n  AND department = 'Engineering'";
        } else if (query.isEmpty()) {
            return "💡 Start with:\nSELECT * FROM table_name\nWHERE condition\nORDER BY column;";
        }
        return "💡 Common SQL patterns:\n\n• Aggregate: SELECT dept, AVG(salary) FROM emp GROUP BY dept\n• Subquery: SELECT * FROM emp WHERE salary > (SELECT AVG(salary) FROM emp)\n• Window: SELECT *, ROW_NUMBER() OVER (ORDER BY salary DESC) FROM emp";
    }

    private String extractSnippet(String hint) {
        // Extract the first code example from hint
        int start = hint.indexOf('\n');
        return start >= 0 ? hint.substring(start).trim() : "";
    }

    // ═════════════════════════════════════════════════════════════
    //  IMPORT
    // ═════════════════════════════════════════════════════════════

    private void showImportDialog() {
        String[] options = {"Paste SQL script", "Load sample dataset"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Import Data")
                .setItems(options, (d, w) -> {
                    if (w == 0) {
                        // Show text input
                        EditText input = new EditText(this);
                        input.setHint("Paste SQL here...");
                        input.setMinLines(5);
                        input.setTypeface(Typeface.MONOSPACE);
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Paste SQL Script")
                                .setView(input)
                                .setPositiveButton("Import", (d2, w2) -> {
                                    sqlQueryInput.setText(input.getText().toString());
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    } else {
                        // Load sample dataset
                        sqlQueryInput.setText(getSampleDataset());
                        executeSqlQuery();
                        showSuccess("Sample dataset loaded");
                    }
                })
                .show();
    }

    private String getSampleDataset() {
        return "CREATE TABLE IF NOT EXISTS employees (\n" +
                "  id INTEGER PRIMARY KEY,\n" +
                "  name TEXT, department TEXT,\n" +
                "  salary REAL, hire_date TEXT\n" +
                ");\n" +
                "INSERT OR IGNORE INTO employees VALUES\n" +
                "  (1,'Alice','Engineering',5000,'2020-01-15'),\n" +
                "  (2,'Bob','Marketing',3500,'2019-06-20'),\n" +
                "  (3,'Carol','Engineering',6200,'2018-03-01'),\n" +
                "  (4,'David','HR',3000,'2021-09-10'),\n" +
                "  (5,'Eve','Engineering',5800,'2020-11-05');\n\n" +
                "SELECT * FROM employees ORDER BY salary DESC;";
    }

    // ═════════════════════════════════════════════════════════════
    //  MISC ACTIONS
    // ═════════════════════════════════════════════════════════════

    private void confirmClearQuery() {
        String currentQuery = sqlQueryInput.getText().toString();
        if (currentQuery.isEmpty()) {
            showToast("Editor is already empty");
            return;
        }

        // Save to undo stack before clearing
        undoStack.push(currentQuery);
        redoStack.clear();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear Editor")
                .setMessage("Are you sure you want to clear the query?")
                .setPositiveButton("Clear", (d, w) -> {
                    sqlQueryInput.setText("");
                    updateLineNumbers("");
                    showToast("Query cleared ✓");
                    // Request focus and show keyboard
                    sqlQueryInput.requestFocus();
                    // Show keyboard
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        sqlQueryInput.getContext().getSystemService(android.view.inputmethod.InputMethodManager.class)
                                .showSoftInput(sqlQueryInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChartDialog() {
        if (currentResult == null || currentResult.size() <= 1) {
            showToast("No data for chart");
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Data Summary")
                .setMessage(
                        "Rows: " + (currentResult.size() - 1) + "\n" +
                                "Columns: " + currentResult.get(0).size() + "\n\n" +
                                "Columns: " + String.join(", ", currentResult.get(0))
                )
                .setPositiveButton("OK", null)
                .show();
    }

    private void previousPage() {
        if (currentPage > 0) { currentPage--; displayPaginatedResult(currentResult, 0); }
    }

    private void nextPage() {
        if (currentResult == null) return;
        int total = (int) Math.ceil((double)(currentResult.size() - 1) / PAGE_SIZE);
        if (currentPage + 1 < total) { currentPage++; displayPaginatedResult(currentResult, 0); }
    }

    // ═════════════════════════════════════════════════════════════
    //  FIREBASE
    // ═════════════════════════════════════════════════════════════

    private void saveTaskCompletion() {
        if (fAuth.getCurrentUser() == null) return;
        String uid = fAuth.getCurrentUser().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("completedAt", new Date());
        data.put("userId", uid);
        fStore.collection("taskCompletions")
                .document(uid + "_" + taskId)
                .set(data)
                .addOnSuccessListener(v -> Log.d(TAG, "Completion saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Save failed", e));
    }

    // ═════════════════════════════════════════════════════════════
    //  ANIMATIONS
    // ═════════════════════════════════════════════════════════════

    private void animateEntrance() {
        View[] views = {
                findViewById(R.id.taskInfoCard),
                resultContainer.getParent() instanceof View ? (View) resultContainer.getParent().getParent().getParent() : null
        };
        for (int i = 0; i < views.length; i++) {
            if (views[i] == null) continue;
            views[i].setAlpha(0f);
            views[i].setTranslationY(30f);
            views[i].animate()
                    .alpha(1f).translationY(0f)
                    .setDuration(400)
                    .setStartDelay(i * 100L)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
    }

    private void animateProgressBar(int target) {
        if (taskProgress == null) return;
        ObjectAnimator anim = ObjectAnimator.ofInt(taskProgress, "progress", 0, target);
        anim.setDuration(800);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();
        if (taskProgressText != null) taskProgressText.setText(target + "%");
    }

    private void animateSuccess() {
        View root = findViewById(android.R.id.content);
        ObjectAnimator flash = ObjectAnimator.ofFloat(root, "alpha", 1f, 0.7f, 1f);
        flash.setDuration(600);
        flash.start();
        showSuccess("✅ Correct! Well done!");
    }

    private void animateError() {
        View editorCard = sqlQueryInput.getRootView();
        ObjectAnimator shake = ObjectAnimator.ofFloat(sqlQueryInput, "translationX",
                0, -12, 12, -8, 8, -4, 4, 0);
        shake.setDuration(400);
        shake.start();
        showError("❌ Incorrect. Check the results below.");
    }

    private void pulseButton(View view) {
        view.animate()
                .scaleX(0.95f).scaleY(0.95f).setDuration(100)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
    }

    // ═════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ═════════════════════════════════════════════════════════════

    private View buildSectionHeader(String text, String hexColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(8), dpToPx(16), dpToPx(8), dpToPx(8));

        View dot = new View(this);
        dot.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(3), dpToPx(16)));
        dot.setBackgroundColor(Color.parseColor(hexColor));

        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(13f);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor(hexColor));
        title.setPadding(dpToPx(10), 0, 0, 0);
        title.setLetterSpacing(0.05f);

        row.addView(dot);
        row.addView(title);
        return row;
    }

    private void showEmptyState(String msg) {
        TextView tv = new TextView(this);
        tv.setText("○  " + msg);
        tv.setTextSize(13f);
        tv.setTextColor(Color.parseColor("#4A5568"));
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(dpToPx(16), dpToPx(40), dpToPx(16), dpToPx(40));
        resultContainer.addView(tv);
    }

    private void showErrorState(String msg) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
        box.setBackgroundColor(Color.parseColor("#1AE06C75"));

        TextView icon = new TextView(this);
        icon.setText("✕  Query Error");
        icon.setTextSize(13f);
        icon.setTypeface(null, Typeface.BOLD);
        icon.setTextColor(Color.parseColor("#E06C75"));
        box.addView(icon);

        TextView detail = new TextView(this);
        detail.setText(msg);
        detail.setTextSize(12f);
        detail.setTextColor(Color.parseColor("#E06C7590"));
        detail.setTypeface(Typeface.MONOSPACE);
        detail.setPadding(0, dpToPx(8), 0, 0);
        box.addView(detail);

        resultContainer.addView(box);
        if (scrollHint != null) scrollHint.setVisibility(View.GONE);
    }

    private void showSuccess(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(Color.parseColor("#98C379"))
                .setTextColor(Color.parseColor("#1A202C"))
                .show();
    }

    private void showError(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG)
                .setBackgroundTint(Color.parseColor("#E06C75"))
                .setTextColor(Color.WHITE)
                .show();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void playSuccessSound() {
        try { if (successSound != null) successSound.start(); } catch (Exception ignored) {}
    }

    private void hapticFeedback() {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    // Small helper to avoid calling deprecated setPadding etc in TextView
    private void setLayout_marginBottom(TextView tv, int px) {
        LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) tv.getLayoutParams();
        if (p == null) p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = px;
        tv.setLayoutParams(p);
    }

    // ═════════════════════════════════════════════════════════════
    //  BACK PRESS
    // ═════════════════════════════════════════════════════════════

    private final OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            new MaterialAlertDialogBuilder(PostgreSqlTaskDetail.this)
                    .setTitle("Leave Editor?")
                    .setMessage("Your unsaved query will be lost.")
                    .setPositiveButton("Leave", (d, w) -> finish())
                    .setNegativeButton("Stay", null)
                    .show();
        }
    };
}