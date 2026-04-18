package com.ali.javaquizbyali.codemodel;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.ali.systemIn.R;
import com.ali.utils.SyncScrollView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeActivity extends AppCompatActivity {

    // UI Elements
    private EditText codeInput;
    private TextView outputText;
    private TextView lineNumbers;
    private TextView taskDescription;
    private TextView taskTitle;
    private MaterialCardView taskCard;
    private MaterialCardView outputContainer;
    private SyncScrollView lineNumbersScroll;
    private ScrollView codeScrollView;
    private ListView suggestionsList;
    private Button clearBtn, runBtn, clearOutputBtn, hideOutputBtn;
    private AdView mAdJavaCode;
    private Toolbar toolbar;

    // Data
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "JavaCodePrefs";
    private static final String TASK_PREFS = "TaskProgress";
    private static final String PROJECTS_DIR = "JavaProjects";

    private boolean isTaskMode = false;
    private int currentTaskId;
    private List<TaskModel.Test> tests;
    private String solution;
    private String initialCode;
    private String taskTitleText;
    private String taskDescriptionText;

    // Undo/Redo
    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();
    private boolean isUndoRedoOp = false;

    // Syntax Highlighting
    private boolean isApplyingHighlighting = false;
    private boolean syntaxHighlightingEnabled = true;
    private boolean autoCompleteEnabled = true;
    private boolean autoIndentEnabled = true;

    // Java Keywords
    private final String[] JAVA_KEYWORDS = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null"
    };

    // Auto-complete Suggestions
    private final String[] JAVA_SUGGESTIONS = {
            "public class ", "public static void main", "System.out.println", "for (int i = 0; i < ; i++)",
            "if () {\n    \n}", "while () {\n    \n}", "try {\n    \n} catch () {\n    \n}",
            "Scanner scanner = new Scanner(System.in);", "ArrayList<> list = new ArrayList<>();",
            "HashMap<> map = new HashMap<>();"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code);

        initViews();
        setupToolbar();
        setupTaskData();
        setupSharedPreferences();
        setupEventListeners();
        setupSyncScrolling();
        setupAdMob();

        saveToUndoStack();
        displayTaskInfo();
    }

    private void initViews() {
        codeInput = findViewById(R.id.codeInput);
        outputText = findViewById(R.id.outputText);
        lineNumbers = findViewById(R.id.lineNumbers);
        lineNumbersScroll = findViewById(R.id.lineNumbersScroll);
        codeScrollView = findViewById(R.id.codeScrollView);
        outputContainer = findViewById(R.id.outputContainer);
        suggestionsList = findViewById(R.id.suggestionsList);
        taskDescription = findViewById(R.id.taskDescription);
        taskTitle = findViewById(R.id.taskTitle);
        taskCard = findViewById(R.id.taskCard);
        toolbar = findViewById(R.id.toolbar);
        clearBtn = findViewById(R.id.clearBtn);
        runBtn = findViewById(R.id.runBtn);
        clearOutputBtn = findViewById(R.id.clearOutputBtn);
        hideOutputBtn = findViewById(R.id.hideOutputBtn);
        mAdJavaCode = findViewById(R.id.adJavaCode);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            saveCode();
            finish();
            return true;
        } else if (id == R.id.menu_undo) {
            undo();
            return true;
        } else if (id == R.id.menu_redo) {
            redo();
            return true;
        } else if (id == R.id.menu_copy_code) {
            copyCode();
            return true;
        } else if (id == R.id.menu_paste) {
            pasteCode();
            return true;
        } else if (id == R.id.menu_settings) {
            showSettings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupSyncScrolling() {
        if (codeScrollView != null && lineNumbersScroll != null) {
            lineNumbersScroll.setSyncedScrollView(codeScrollView);
        }

        codeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLineNumbers();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateLineNumbers() {
        String text = codeInput.getText().toString();
        int lineCount = text.split("\n", -1).length;
        if (text.endsWith("\n")) lineCount++;

        StringBuilder numbers = new StringBuilder();
        for (int i = 1; i <= lineCount; i++) {
            numbers.append(i).append("\n");
        }
        lineNumbers.setText(numbers.toString());
    }

    private void displayTaskInfo() {
        if (!isTaskMode && taskCard != null) {
            taskCard.setVisibility(View.GONE);
            return;
        }

        if (taskCard != null) taskCard.setVisibility(View.VISIBLE);
        if (taskTitle != null && taskTitleText != null) taskTitle.setText(taskTitleText);
        if (taskDescription != null && taskDescriptionText != null) taskDescription.setText(taskDescriptionText);
    }

    private void setupTaskData() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentTaskId = extras.getInt("TASK_ID", -1);

            if (currentTaskId != -1) {
                isTaskMode = true;
                taskTitleText = extras.getString("TASK_TITLE", "");
                taskDescriptionText = extras.getString("TASK_DESCRIPTION", "");
                initialCode = extras.getString("INITIAL_CODE", "");
                String testsJson = extras.getString("TASK_TESTS", "");
                solution = extras.getString("TASK_SOLUTION", "");

                if (testsJson != null && !testsJson.isEmpty()) {
                    Gson gson = new Gson();
                    Type testListType = new TypeToken<List<TaskModel.Test>>() {}.getType();
                    tests = gson.fromJson(testsJson, testListType);
                }
            } else {
                isTaskMode = false;
            }
        }
    }

    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        loadSavedCode();
    }

    private void loadSavedCode() {
        String key = isTaskMode ? "task_" + currentTaskId + "_code" : "console_code";
        String saved = sharedPreferences.getString(key, "");

        if (saved.isEmpty() && isTaskMode && initialCode != null) {
            saved = initialCode;
        }

        if (!saved.isEmpty()) {
            codeInput.setText(saved);
            updateLineNumbers();
            if (syntaxHighlightingEnabled) applySyntaxHighlighting(codeInput.getText());
        }
    }

    private void saveCode() {
        String key = isTaskMode ? "task_" + currentTaskId + "_code" : "console_code";
        sharedPreferences.edit().putString(key, codeInput.getText().toString()).apply();
    }

    private void saveToUndoStack() {
        String code = codeInput.getText().toString();
        if (undoStack.isEmpty() || !undoStack.peek().equals(code)) {
            undoStack.push(code);
            if (undoStack.size() > 50) undoStack.removeElementAt(0);
        }
    }

    private void undo() {
        if (undoStack.size() > 1) {
            isUndoRedoOp = true;
            redoStack.push(codeInput.getText().toString());
            undoStack.pop();
            String prev = undoStack.peek();
            codeInput.setText(prev);
            updateLineNumbers();
            if (syntaxHighlightingEnabled) applySyntaxHighlighting(codeInput.getText());
            isUndoRedoOp = false;
            Toast.makeText(this, "Undo", Toast.LENGTH_SHORT).show();
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            isUndoRedoOp = true;
            undoStack.push(codeInput.getText().toString());
            String next = redoStack.pop();
            codeInput.setText(next);
            updateLineNumbers();
            if (syntaxHighlightingEnabled) applySyntaxHighlighting(codeInput.getText());
            isUndoRedoOp = false;
            Toast.makeText(this, "Redo", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyCode() {
        String code = codeInput.getText().toString();
        if (!code.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Java Code", code);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Code copied", Toast.LENGTH_SHORT).show();
        }
    }

    private void pasteCode() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            String pastedText = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
            if (pastedText != null) {
                int cursor = codeInput.getSelectionStart();
                codeInput.getText().insert(cursor, pastedText);
                Toast.makeText(this, "Code pasted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showSettings() {
        new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setItems(new String[]{
                        "Syntax Highlighting: " + (syntaxHighlightingEnabled ? "ON" : "OFF"),
                        "Auto-complete: " + (autoCompleteEnabled ? "ON" : "OFF"),
                        "Auto-indent: " + (autoIndentEnabled ? "ON" : "OFF")
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            syntaxHighlightingEnabled = !syntaxHighlightingEnabled;
                            if (syntaxHighlightingEnabled) {
                                applySyntaxHighlighting(codeInput.getText());
                            }
                            break;
                        case 1:
                            autoCompleteEnabled = !autoCompleteEnabled;
                            if (!autoCompleteEnabled) hideSuggestions();
                            break;
                        case 2:
                            autoIndentEnabled = !autoIndentEnabled;
                            break;
                    }
                    Toast.makeText(this, "Settings updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void setupEventListeners() {
        // Clear button
        clearBtn.setOnClickListener(v -> {
            saveToUndoStack();
            redoStack.clear();
            codeInput.setText("");
            updateLineNumbers();
            outputText.setText("> Code cleared. Ready to write new code...");
        });

        // Run button
        runBtn.setOnClickListener(v -> runJavaCode());

        // Clear output
        clearOutputBtn.setOnClickListener(v -> outputText.setText("> Ready to run Java code..."));

        // Hide output
        hideOutputBtn.setOnClickListener(v -> {
            if (outputContainer.getVisibility() == View.VISIBLE) {
                outputContainer.setVisibility(View.GONE);
                hideOutputBtn.setText("Show");
            } else {
                outputContainer.setVisibility(View.VISIBLE);
                hideOutputBtn.setText("Hide");
            }
        });

        // Code input text watcher
        codeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isUndoRedoOp) saveToUndoStack();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLineNumbers();
                if (autoCompleteEnabled && count == 1 && s.charAt(start) == '.') {
                    showSmartCompletion();
                } else if (autoCompleteEnabled) {
                    showSuggestions();
                }
                if (autoIndentEnabled && before == 0 && count == 1 && s.charAt(start) == '\n') {
                    autoIndent(start + 1, s.toString(), start);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isApplyingHighlighting && syntaxHighlightingEnabled) {
                    applySyntaxHighlighting(s);
                }
                saveCode();
            }
        });

        // Suggestion click
        suggestionsList.setOnItemClickListener((parent, view, position, id) -> {
            String suggestion = (String) suggestionsList.getAdapter().getItem(position);
            insertSuggestion(suggestion);
            hideSuggestions();
        });
    }

    private void autoIndent(int position, String fullText, int newlinePosition) {
        Editable editable = codeInput.getText();
        if (position <= 1) return;

        int lineStart = fullText.lastIndexOf('\n', newlinePosition - 1);
        if (lineStart == -1) lineStart = 0;
        else lineStart++;

        String prevLine = fullText.substring(lineStart, newlinePosition);
        int indentCount = 0;
        for (int i = 0; i < prevLine.length(); i++) {
            char c = prevLine.charAt(i);
            if (c == ' ') indentCount++;
            else if (c == '\t') indentCount += 4;
            else break;
        }

        if (prevLine.trim().endsWith("{")) indentCount += 4;

        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < indentCount; i++) indent.append(" ");

        if (position <= editable.length()) {
            editable.insert(position, indent.toString());
        }
    }

    private void showSuggestions() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, JAVA_SUGGESTIONS);
        suggestionsList.setAdapter(adapter);
        suggestionsList.setVisibility(View.VISIBLE);
    }

    private void showSmartCompletion() {
        String text = codeInput.getText().toString();
        int cursor = codeInput.getSelectionStart();

        int dotPos = cursor - 1;
        int varStart = dotPos - 1;
        while (varStart >= 0 && Character.isLetterOrDigit(text.charAt(varStart))) varStart--;
        varStart++;

        if (varStart < dotPos) {
            String varName = text.substring(varStart, dotPos);
            String[] methods = getTypeMethods(varName, text);
            if (methods != null && methods.length > 0) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_list_item_1, methods);
                suggestionsList.setAdapter(adapter);
                suggestionsList.setVisibility(View.VISIBLE);
                return;
            }
        }
        showSuggestions();
    }

    private String[] getTypeMethods(String varName, String code) {
        if (varName.equals("System") || varName.equals("out")) {
            return new String[]{"out.println()", "out.print()", "out.printf()"};
        }
        if (varName.equals("scanner")) {
            return new String[]{"next()", "nextLine()", "nextInt()", "nextDouble()", "close()"};
        }
        return null;
    }

    private void insertSuggestion(String suggestion) {
        int cursor = codeInput.getSelectionStart();
        Editable editable = codeInput.getText();

        editable.insert(cursor, suggestion);

        if (suggestion.contains("()")) {
            int newCursor = cursor + suggestion.indexOf("(") + 1;
            codeInput.setSelection(newCursor);
        }
    }

    private void hideSuggestions() {
        suggestionsList.setVisibility(View.GONE);
    }

    private void applySyntaxHighlighting(Editable editable) {
        if (isApplyingHighlighting) return;
        isApplyingHighlighting = true;

        try {
            String text = editable.toString();

            ForegroundColorSpan[] spans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
            for (ForegroundColorSpan span : spans) editable.removeSpan(span);

            // Keywords
            for (String keyword : JAVA_KEYWORDS) {
                Pattern pattern = Pattern.compile("\\b" + keyword + "\\b");
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    editable.setSpan(new ForegroundColorSpan(Color.parseColor("#569CD6")),
                            matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            // Strings
            Pattern stringPattern = Pattern.compile("\"(.*?)\"");
            Matcher stringMatcher = stringPattern.matcher(text);
            while (stringMatcher.find()) {
                editable.setSpan(new ForegroundColorSpan(Color.parseColor("#CE9178")),
                        stringMatcher.start(), stringMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Comments
            Pattern commentPattern = Pattern.compile("//.*|/\\*.*?\\*/", Pattern.DOTALL);
            Matcher commentMatcher = commentPattern.matcher(text);
            while (commentMatcher.find()) {
                editable.setSpan(new ForegroundColorSpan(Color.parseColor("#6A9955")),
                        commentMatcher.start(), commentMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Numbers
            Pattern numberPattern = Pattern.compile("\\b\\d+\\.?\\d*\\b");
            Matcher numberMatcher = numberPattern.matcher(text);
            while (numberMatcher.find()) {
                editable.setSpan(new ForegroundColorSpan(Color.parseColor("#B5CEA8")),
                        numberMatcher.start(), numberMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Classes
            Pattern classPattern = Pattern.compile("\\b[A-Z][a-zA-Z0-9_]*\\b");
            Matcher classMatcher = classPattern.matcher(text);
            while (classMatcher.find()) {
                String word = classMatcher.group();
                if (!isKeyword(word)) {
                    editable.setSpan(new ForegroundColorSpan(Color.parseColor("#4EC9B0")),
                            classMatcher.start(), classMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        } finally {
            isApplyingHighlighting = false;
        }
    }

    private boolean isKeyword(String word) {
        for (String kw : JAVA_KEYWORDS) {
            if (kw.equals(word)) return true;
        }
        return false;
    }

    private void runJavaCode() {
        String code = codeInput.getText().toString();
        if (code.trim().isEmpty()) {
            showOutput("Please write some Java code first", true);
            return;
        }

        runBtn.setEnabled(false);
        runBtn.setText("Running...");

        new Handler().postDelayed(() -> {
            StringBuilder output = new StringBuilder();
            output.append("═══════════════════════════════\n");
            output.append("         EXECUTION RESULT        \n");
            output.append("═══════════════════════════════\n\n");

            // Check syntax
            List<String> errors = checkSyntax(code);
            if (!errors.isEmpty()) {
                output.append("❌ COMPILATION ERRORS:\n\n");
                for (String error : errors) output.append(error).append("\n");
            } else {
                output.append("✓ Syntax check passed!\n\n");
                if (tests != null && !tests.isEmpty()) {
                    runTests(code, output);
                }
                simulateExecution(code, output);
            }

            output.append("\n═══════════════════════════════\n");
            showOutput(output.toString(), errors.isEmpty() ? false : true);

            runBtn.setEnabled(true);
            runBtn.setText("Run Code");
        }, 100);
    }

    private List<String> checkSyntax(String code) {
        List<String> errors = new ArrayList<>();
        String[] lines = code.split("\n");

        int openBraces = 0, closeBraces = 0;
        int openParen = 0, closeParen = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNum = i + 1;

            openBraces += countOcc(line, "{");
            closeBraces += countOcc(line, "}");
            openParen += countOcc(line, "(");
            closeParen += countOcc(line, ")");

            if (countOcc(line, "\"") % 2 != 0) {
                errors.add("Line " + lineNum + ": Unclosed string literal");
            }
        }

        if (openBraces != closeBraces) {
            errors.add("Unmatched braces: " + openBraces + " { vs " + closeBraces + " }");
        }
        if (openParen != closeParen) {
            errors.add("Unmatched parentheses: " + openParen + " ( vs " + closeParen + " )");
        }

        return errors;
    }

    private int countOcc(String str, String sub) {
        int count = 0, idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    private void runTests(String code, StringBuilder output) {
        output.append("🧪 TEST RESULTS:\n\n");
        if (solution != null && code.toLowerCase().contains(solution.toLowerCase())) {
            output.append("✓ Solution pattern found!\n");
        } else {
            output.append("⚠ Test pattern not found\n");
        }
    }

    private void simulateExecution(String code, StringBuilder output) {
        output.append("📋 CODE OUTPUT:\n\n");
        Pattern printPattern = Pattern.compile("System\\.out\\.print(ln)?\\(([^;]+)\\)");
        Matcher matcher = printPattern.matcher(code);

        boolean hasOutput = false;
        while (matcher.find()) {
            hasOutput = true;
            String content = matcher.group(2).trim();
            if (content.startsWith("\"") && content.endsWith("\"")) {
                output.append(content.substring(1, content.length() - 1)).append("\n");
            } else {
                output.append("[").append(content).append("]\n");
            }
        }

        if (!hasOutput) {
            output.append("(No System.out.println statements found)\n");
        }
    }

    private void showOutput(String text, boolean isError) {
        runOnUiThread(() -> {
            outputText.setText(text);
            outputText.setTextColor(isError ? Color.parseColor("#E57373") : Color.parseColor("#81C784"));
            if (outputContainer.getVisibility() != View.VISIBLE) {
                outputContainer.setVisibility(View.VISIBLE);
                hideOutputBtn.setText("Hide");
            }
        });
    }

    private void setupAdMob() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdJavaCode.loadAd(adRequest);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCode();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveCode();
    }
}