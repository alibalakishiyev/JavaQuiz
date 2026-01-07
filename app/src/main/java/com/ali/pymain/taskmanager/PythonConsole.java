package com.ali.pymain.taskmanager;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.ali.javaquizbyali.codemodel.TaskModel;

import com.ali.systemIn.R;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PythonConsole extends AppCompatActivity {

    private EditText codeInput;
    private TextView outputText;
    private AdView mAdPythonCode;
    private TextView lineNumbers;
    private TextView taskDescription;

    private Python py;
    private FirebaseAuth fAuth;
    private SharedPreferences preferences;
    private static final String PREF_USER_LOGGED_IN = "user_logged_in";
    private static final String PREFS_NAME = "PythonConsolePrefs";
    private static final String KEY_SAVED_CODE = "saved_code";
    private static final String TASK_PREFS = "TaskProgress";

    // Task məlumatları
    private int currentTaskId;
    private List<TaskModel.Test> tests;
    private String solution;
    private String initialCode;

    private LinearLayout outputContainer;

    private LinearLayout codeEditorLayout;
    private Button hideOutputBtn;

    // Auto-complete üçün
    private ListView suggestionsList;
    private ArrayAdapter<String> suggestionsAdapter;
    private List<String> suggestions;

    // Python tip metodları
    private final Map<String, String[]> TYPE_METHODS = new HashMap<String, String[]>() {{
        put("str", new String[]{
                "upper()", "lower()", "strip()", "replace(old, new)", "split(separator)",
                "join(iterable)", "find(substring)", "index(substring)", "count(substring)",
                "startswith(prefix)", "endswith(suffix)", "isalpha()", "isdigit()",
                "islower()", "isupper()", "capitalize()", "title()", "swapcase()"
        });
        put("list", new String[]{
                "append(item)", "extend(iterable)", "insert(index, item)", "remove(item)",
                "pop(index)", "clear()", "index(item)", "count(item)", "sort()", "reverse()",
                "copy()", "len()"
        });
        put("dict", new String[]{
                "keys()", "values()", "items()", "get(key, default)", "pop(key)",
                "popitem()", "clear()", "copy()", "update(other_dict)", "setdefault(key, default)"
        });
        put("int", new String[]{
                "bit_length()", "to_bytes(length, byteorder)", "from_bytes(bytes, byteorder)",
                "abs()", "bin()", "hex()", "oct()"
        });
        put("float", new String[]{
                "is_integer()", "hex()", "fromhex(string)", "as_integer_ratio()"
        });
    }};

    // Python anahtar kelimeleri
    private static final String[] PYTHON_KEYWORDS = {
            "def", "class", "if", "else", "elif", "while", "for", "in", "return", "switch",
            "import", "from", "as", "try", "except", "finally", "with", "lambda",
            "and", "or", "not", "is", "None", "True", "False", "pass", "break", "print",
            "continue", "global", "nonlocal", "assert", "raise", "yield", "async", "await"
    };

    // Python built-in funksiyaları
    private static final String[] PYTHON_BUILTINS = {
            "abs()", "all()", "any()", "bin()", "bool()", "chr()", "dict()", "dir()", "enumerate()",
            "filter()", "float()", "int()", "len()", "list()", "map()", "max()", "min()", "ord()",
            "range()", "reversed()", "round()", "set()", "sorted()", "str()", "sum()", "tuple()", "type()",
            "zip()", "open()", "input()", "format()", "help()", "id()", "isinstance()", "issubclass()",
            "print()", "range()", "input()", "len()", "sum()", "sorted()"
    };

    // Python module adları
    private static final String[] PYTHON_MODULES = {
            "math", "random", "datetime", "json", "re", "os", "sys", "time",
            "collections", "itertools", "functools", "decimal", "fractions",
            "statistics", "hashlib", "base64", "csv", "html"
    };

    // Renkler
    private static final int COLOR_CLASS = Color.parseColor("#FF9800"); // Turuncu
    private static final int COLOR_FUNCTION = Color.parseColor("#4CAF50"); // Yeşil
    private static final int COLOR_KEYWORD = Color.parseColor("#2196F3"); // Mavi
    private static final int COLOR_STRING = Color.parseColor("#E91E63"); // Pembe
    private static final int COLOR_COMMENT = Color.parseColor("#9E9E9E"); // Gri
    private static final int COLOR_NUMBER = Color.parseColor("#9C27B0"); // Mor
    private static final int COLOR_BUILTIN = Color.parseColor("#FF5722"); // Qırmızı-turuncu
    private static final int COLOR_MODULE = Color.parseColor("#673AB7"); // Tünd bənövşəyi

    private boolean isApplyingHighlighting = false;

    // Compiler xətası sinfi
    private class CompilerError {
        String message;
        int lineNumber;
        String errorType;

        CompilerError(String message, int lineNumber, String errorType) {
            this.message = message;
            this.lineNumber = lineNumber;
            this.errorType = errorType;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_python_console);

        initializeViews();
        setupTaskData();
        setupFirebase();
        setupPython();
        outputContainer = findViewById(R.id.outputContainer); // YENİ
        hideOutputBtn = findViewById(R.id.hideOutputBtn); // YENİ
        setupPreferences();
        setupEventListeners();
        setupAutoComplete();
        setupSuggestions();
        setupAdMob();
        setupBackPressedCallback();
    }

    private void initializeViews() {
        codeInput = findViewById(R.id.codeInput);
        outputText = findViewById(R.id.outputText);
        lineNumbers = findViewById(R.id.lineNumbers);
        outputContainer = findViewById(R.id.outputContainer);
        codeEditorLayout = findViewById(R.id.codeEditorLayout); // BU SƏTRİ ƏLAVƏ EDİN
        hideOutputBtn = findViewById(R.id.hideOutputBtn);
        Button backBtn = findViewById(R.id.backBtn);
        Button clearBtn = findViewById(R.id.clearBtn);
        Button runBtn = findViewById(R.id.runBtn);
        Button clearOutputBtn = findViewById(R.id.clearOutputBtn);
        suggestionsList = findViewById(R.id.suggestionsList);
        taskDescription = findViewById(R.id.taskDescription);

        // DEBUG: Dəyişənlərin null olmadığını yoxlayaq
        Log.d("PythonConsole", "codeEditorLayout: " + (codeEditorLayout != null ? "tapıldı" : "NULL"));
        Log.d("PythonConsole", "outputContainer: " + (outputContainer != null ? "tapıldı" : "NULL"));
    }

    private void setupTaskData() {
        Intent intent = getIntent();
        if (intent != null) {
            currentTaskId = intent.getIntExtra("TASK_ID", 1);
            String title = intent.getStringExtra("TASK_TITLE");
            String description = intent.getStringExtra("TASK_DESCRIPTION");
            initialCode = intent.getStringExtra("INITIAL_CODE");
            String testsJson = intent.getStringExtra("TASK_TESTS");
            solution = intent.getStringExtra("TASK_SOLUTION");

            if (title != null && description != null) {
                String fullDescription = "Tapşırıq " + currentTaskId + ": " + title + "\n" + description;
                taskDescription.setText(fullDescription);
            } else if (description != null) {
                taskDescription.setText(description);
            } else {
                taskDescription.setVisibility(View.GONE);
            }

            // Parse tests from JSON
            if (testsJson != null && !testsJson.isEmpty()) {
                Gson gson = new Gson();
                Type testListType = new TypeToken<List<TaskModel.Test>>() {}.getType();
                tests = gson.fromJson(testsJson, testListType);
            }
        }
    }

    private void setupFirebase() {
        fAuth = FirebaseAuth.getInstance();
    }

    private void setupPython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        py = Python.getInstance();
    }

    private void setupPreferences() {
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        loadSavedCode();

        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        boolean isCurrentlyLoggedIn = fAuth.getCurrentUser() != null;
        prefs.edit().putBoolean(PREF_USER_LOGGED_IN, isCurrentlyLoggedIn).apply();
    }

    private void setupEventListeners() {
        Button backBtn = findViewById(R.id.backBtn);
        Button runBtn = findViewById(R.id.runBtn);
        Button clearBtn = findViewById(R.id.clearBtn);
        Button clearOutputBtn = findViewById(R.id.clearOutputBtn);

        setupLineNumbers();
        setupSyntaxHighlighting();
        setupAutoIndent();

        // Back butonu
        backBtn.setOnClickListener(v -> {
            saveCodeToStorage();
            onBackPressed();
        });

        // Run butonu - DEBUG üçün Toast əlavə edin
        runBtn.setOnClickListener(v -> {
            Log.d("PythonConsole", "RUN düyməsinə basıldı");
            Toast.makeText(PythonConsole.this, "RUN basıldı", Toast.LENGTH_SHORT).show();
            showOutputPanel();
            runPythonCode();
        });

        // Clear butonu
        clearBtn.setOnClickListener(v -> {
            codeInput.setText(initialCode != null ? initialCode : "");
            outputText.setText("> ...");
            hideSuggestions();
            updateLineNumbers(codeInput.getText().toString());
        });

        // Clear output butonu
        clearOutputBtn.setOnClickListener(v -> {
            outputText.setText("> ...");
            Toast.makeText(PythonConsole.this, "Çıxış təmizləndi", Toast.LENGTH_SHORT).show();
        });

        // Hide output butonu
        hideOutputBtn.setOnClickListener(v -> hideOutputPanel());
    }

    // Çıxış panelini göstər
    // Çıxış panelini göstər - SADƏ VERSİYA
    private void showOutputPanel() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("PythonConsole", "showOutputPanel çağırıldı");

                    // Əgər outputContainer null-dursa, tap
                    if (outputContainer == null) {
                        outputContainer = findViewById(R.id.outputContainer);
                        Log.d("PythonConsole", "outputContainer yenidən tapıldı: " + (outputContainer != null));
                    }

                    if (outputContainer != null) {
                        if (outputContainer.getVisibility() != View.VISIBLE) {
                            Log.d("PythonConsole", "Çıxış paneli VISIBLE edilir");
                            outputContainer.setVisibility(View.VISIBLE);

                            // Weight-ləri tənzimlə
                            LinearLayout.LayoutParams editorParams = (LinearLayout.LayoutParams) findViewById(R.id.codeEditorLayout).getLayoutParams();
                            LinearLayout.LayoutParams outputParams = (LinearLayout.LayoutParams) outputContainer.getLayoutParams();

                            editorParams.weight = 1f;
                            outputParams.weight = 1f;

                            // Layout-u yenilə
                            outputContainer.requestLayout();
                            findViewById(R.id.codeEditorLayout).requestLayout();

                            // Scroll-u aşağı apar
                            ScrollView scrollView = (ScrollView) outputContainer.getChildAt(1);
                            if (scrollView != null) {
                                scrollView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        scrollView.fullScroll(View.FOCUS_DOWN);
                                    }
                                });
                            }
                        }
                    } else {
                        Log.e("PythonConsole", "outputContainer NULL!");
                        Toast.makeText(PythonConsole.this, "Çıxış paneli tapılmadı", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e("PythonConsole", "showOutputPanel xətası: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void hideOutputPanel() {
        if (outputContainer.getVisibility() == View.VISIBLE) {
            outputContainer.setVisibility(View.GONE);
        }
    }

    private void setupAutoComplete() {
        codeInput.addTextChangedListener(new TextWatcher() {
            private int previousLength = 0;
            private boolean isDeleting = false;
            private String lastWord = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                previousLength = s.length();
                isDeleting = after < count;

                // Son kelimeyi al
                if (start > 0 && s.length() > 0) {
                    int wordStart = start;
                    while (wordStart > 0 && Character.isLetterOrDigit(s.charAt(wordStart - 1))) {
                        wordStart--;
                    }
                    lastWord = s.subSequence(wordStart, start).toString();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLineNumbers(s.toString());

                // Nöqtə daxil etdikdə smart completion göstər
                if (!isDeleting && count == 1 && s.charAt(start) == '.') {
                    showSmartCompletion(s.toString(), start);
                } else if (s.length() > 0) {
                    showSuggestions(s.toString(), start + count);
                } else {
                    hideSuggestions();
                }

                // Avtomatik indentasiya əlavə et
                if (!isDeleting && before == 0 && count == 1) {
                    char lastChar = s.charAt(start);
                    if (lastChar == '\n') {
                        autoIndent(start + 1, s.toString(), start);
                    } else if (lastChar == ':') {
                        autoAddIndentAfterColon(start + 1);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isApplyingHighlighting) {
                    applySyntaxHighlighting(s);
                }
                saveCodeToStorage();
            }
        });
    }

    private void autoIndent(int position, String fullText, int newlinePosition) {
        Editable editable = codeInput.getText();
        if (position > 1) {
            // Cari sətri al (Enter basılan sətir)
            int currentLineStart = fullText.lastIndexOf('\n', newlinePosition - 1);
            if (currentLineStart == -1) currentLineStart = 0;
            else currentLineStart++;

            String currentLine = fullText.substring(currentLineStart, Math.min(newlinePosition, fullText.length()));
            String currentLineTrimmed = currentLine.trim();

            // Əgər cari sətirdə pass, return və ya comment varsa, indent azalt
            if (currentLineTrimmed.equals("pass") || currentLineTrimmed.equals("return") ||
                    currentLineTrimmed.startsWith("#") || currentLineTrimmed.isEmpty()) {
                // Əvvəlki sətirləri yoxla
                int indentLevel = calculateIndentLevelForPython(fullText, newlinePosition);

                // Indentasiya yarat
                StringBuilder indent = new StringBuilder();
                for (int i = 0; i < indentLevel; i++) {
                    indent.append("    ");
                }

                // Cari sətirdə artıq indent var?
                String existingText = editable.toString();
                if (position < existingText.length()) {
                    int spaces = 0;
                    while (position + spaces < existingText.length() &&
                            existingText.charAt(position + spaces) == ' ') {
                        spaces++;
                    }

                    if (spaces < indent.length()) {
                        if (spaces > 0) {
                            editable.delete(position, position + spaces);
                        }
                        editable.insert(position, indent.toString());
                    }
                } else {
                    editable.insert(position, indent.toString());
                }
            } else {
                // Normal indent hesabla
                int indentLevel = calculateIndentLevelForPython(fullText, newlinePosition);

                StringBuilder indent = new StringBuilder();
                for (int i = 0; i < indentLevel; i++) {
                    indent.append("    ");
                }

                editable.insert(position, indent.toString());
            }
        }
    }

    private int calculateIndentLevelForPython(String fullText, int position) {
        String[] lines = fullText.substring(0, position).split("\n");
        int indentLevel = 0;
        boolean inBlockComment = false;

        for (String line : lines) {
            String trimmedLine = line.trim();

            // Çoxsətirli yorumları idarə et
            if (trimmedLine.contains("\"\"\"") || trimmedLine.contains("'''")) {
                int count = countOccurrences(trimmedLine, "\"\"\"") + countOccurrences(trimmedLine, "'''");
                if (count % 2 != 0) {
                    inBlockComment = !inBlockComment;
                }
            }

            if (!inBlockComment && !trimmedLine.startsWith("#")) {
                // Sətirdə : varsa və boşluq yoxdursa, indent artır
                if (trimmedLine.endsWith(":") && !trimmedLine.contains("#")) {
                    indentLevel++;
                }

                // Sətir boşdursa və ya pass/return varsa, indent azalt
                if (trimmedLine.isEmpty() || trimmedLine.equals("pass") ||
                        trimmedLine.equals("return") || trimmedLine.startsWith("return ")) {
                    indentLevel = Math.max(0, indentLevel - 1);
                }
            }
        }

        return Math.max(0, indentLevel);
    }

    private void autoAddIndentAfterColon(int position) {
        new android.os.Handler().postDelayed(() -> {
            Editable editable = codeInput.getText();
            int currentPos = codeInput.getSelectionStart();

            // Cari sətirdə neçə indent olduğunu hesabla
            String text = editable.toString();
            int lineStart = text.lastIndexOf('\n', currentPos - 1);
            if (lineStart == -1) lineStart = 0;

            String currentLine = text.substring(lineStart, Math.min(currentPos, text.length()));
            int indentCount = 0;
            for (int i = 0; i < currentLine.length(); i++) {
                if (currentLine.charAt(i) == ' ') {
                    indentCount++;
                } else {
                    break;
                }
            }

            // Indent sayını 4-ə böl
            int indentLevel = indentCount / 4;

            // : sonra bir artır
            indentLevel++;

            // Yeni indent yarat
            StringBuilder newIndent = new StringBuilder();
            for (int i = 0; i < indentLevel; i++) {
                newIndent.append("    ");
            }

            String newText = "\n" + newIndent.toString();
            editable.insert(currentPos, newText);

            // İmleci yeni sətirə qoy
            codeInput.setSelection(currentPos + 1 + newIndent.length());
        }, 100);
    }

    private void showSmartCompletion(String text, int position) {
        try {
            // Nöqtədən əvvəlki dəyişəni tap
            int dotPos = position;
            int varStart = dotPos - 1;

            while (varStart >= 0 && (Character.isLetterOrDigit(text.charAt(varStart)) || text.charAt(varStart) == '_')) {
                varStart--;
            }
            varStart++;

            if (varStart < dotPos && varStart >= 0 && dotPos <= text.length()) {
                String varName = text.substring(varStart, dotPos);
                String varType = detectVariableType(text, varName);

                if (varType != null && TYPE_METHODS.containsKey(varType)) {
                    String[] methods = TYPE_METHODS.get(varType);
                    if (methods != null && methods.length > 0) {
                        // Yeni adapter yarat
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_list_item_1,
                                methods
                        );
                        suggestionsList.setAdapter(adapter);
                        suggestionsList.setVisibility(View.VISIBLE);

                        // Yeni adapter-i saxla
                        suggestionsAdapter = adapter;
                        return;
                    }
                }
            }

            // Standart suggestionları göstər
            List<String> allSuggestions = new ArrayList<>();
            allSuggestions.addAll(Arrays.asList(PYTHON_KEYWORDS));
            allSuggestions.addAll(Arrays.asList(PYTHON_BUILTINS));
            allSuggestions.addAll(Arrays.asList(PYTHON_MODULES));

            // Yeni adapter yarat
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    allSuggestions
            );
            suggestionsList.setAdapter(adapter);
            suggestionsList.setVisibility(View.VISIBLE);

            // Yeni adapter-i saxla
            suggestionsAdapter = adapter;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String detectVariableType(String code, String varName) {
        String[] lines = code.split("\n");

        for (String line : lines) {
            line = line.trim();

            // Dəyişən bəyannaməsi axtar
            if (line.contains(varName + " = ") || line.contains(varName + "=")) {
                String rightSide = line.substring(line.indexOf('=') + 1).trim();

                if (rightSide.startsWith("\"") || rightSide.startsWith("'")) {
                    return "str";
                } else if (rightSide.startsWith("[")) {
                    return "list";
                } else if (rightSide.startsWith("{")) {
                    return "dict";
                } else if (rightSide.startsWith("int(") || line.matches(".*\\bint\\s+" + varName + ".*")) {
                    return "int";
                } else if (rightSide.startsWith("float(") || line.matches(".*\\bfloat\\s+" + varName + ".*")) {
                    return "float";
                } else if (rightSide.startsWith("str(") || line.matches(".*\\bstr\\s+" + varName + ".*")) {
                    return "str";
                } else if (rightSide.startsWith("list(") || line.matches(".*\\blist\\s+" + varName + ".*")) {
                    return "list";
                } else if (rightSide.startsWith("dict(") || line.matches(".*\\bdict\\s+" + varName + ".*")) {
                    return "dict";
                }
            }
        }

        return null;
    }

    private void setupSuggestions() {
        try {
            suggestions = new ArrayList<>();
            Collections.addAll(suggestions, PYTHON_KEYWORDS);
            Collections.addAll(suggestions, PYTHON_BUILTINS);
            Collections.addAll(suggestions, PYTHON_MODULES);
            Collections.sort(suggestions);

            suggestionsAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, suggestions);
            suggestionsList.setAdapter(suggestionsAdapter);

            suggestionsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        if (suggestionsAdapter != null && position >= 0 && position < suggestionsAdapter.getCount()) {
                            String selected = suggestionsAdapter.getItem(position);
                            if (selected != null) {
                                insertSuggestion(selected);
                                hideSuggestions();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            // EditText-dən kənara kliklənəndə suggestions-u gizlət
            codeInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        hideSuggestions();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSuggestions(String text, int cursorPosition) {
        try {
            if (text.isEmpty() || cursorPosition > text.length() || suggestionsAdapter == null) {
                hideSuggestions();
                return;
            }

            String lastWord = getLastWord(text, cursorPosition);
            if (lastWord.length() < 1) {
                hideSuggestions();
                return;
            }

            List<String> filtered = new ArrayList<>();
            for (String suggestion : suggestions) {
                if (suggestion.toLowerCase().startsWith(lastWord.toLowerCase())) {
                    filtered.add(suggestion);
                }
            }

            if (filtered.isEmpty()) {
                hideSuggestions();
                return;
            }

            // Clear və notify əvvəlcə
            suggestionsAdapter.clear();

            // Yeni list-i əlavə et
            for (String item : filtered) {
                suggestionsAdapter.add(item);
            }

            // Notify dataset changed
            suggestionsAdapter.notifyDataSetChanged();
            suggestionsList.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            e.printStackTrace();
            hideSuggestions();
        }
    }

    private String getLastWord(String text, int cursorPosition) {
        if (cursorPosition == 0) return "";

        int start = cursorPosition - 1;
        while (start >= 0 && (Character.isLetterOrDigit(text.charAt(start)) || text.charAt(start) == '_')) {
            start--;
        }

        start = Math.max(0, start + 1);
        return text.substring(start, cursorPosition);
    }

    private void insertSuggestion(String suggestion) {
        try {
            if (suggestion == null || suggestion.isEmpty()) {
                return;
            }

            int cursorPos = codeInput.getSelectionStart();
            String text = codeInput.getText().toString();

            if (cursorPos < 0 || cursorPos > text.length()) {
                cursorPos = text.length();
            }

            int wordStart = cursorPos - 1;
            while (wordStart >= 0 && (Character.isLetterOrDigit(text.charAt(wordStart)) || text.charAt(wordStart) == '_')) {
                wordStart--;
            }
            wordStart = Math.max(0, wordStart + 1);

            Editable editable = codeInput.getText();
            if (wordStart <= cursorPos && cursorPos <= editable.length()) {
                editable.replace(wordStart, cursorPos, suggestion);

                // Əgər suggestionda mötərizə varsa, imleci içinə qoy
                if (suggestion.contains("()")) {
                    int parenPos = suggestion.indexOf('(');
                    if (parenPos != -1) {
                        int newPos = wordStart + parenPos + 1;
                        if (newPos <= editable.length()) {
                            codeInput.setSelection(newPos);
                        }
                    }
                } else {
                    int newPos = wordStart + suggestion.length();
                    if (newPos <= editable.length()) {
                        codeInput.setSelection(newPos);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideSuggestions() {
        suggestionsList.setVisibility(View.GONE);
    }

    private void showInputDialog(final String prompt) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(PythonConsole.this);
                builder.setTitle("Input tələb olunur");
                builder.setMessage(prompt);

                final EditText input = new EditText(PythonConsole.this);
                input.setTextColor(Color.WHITE);
                input.setHint("Daxil edin...");
                input.setHintTextColor(Color.GRAY);
                builder.setView(input);

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String userInput = input.getText().toString();
                        executeWithInput(userInput);
                    }
                });

                builder.setNegativeButton("Ləğv et", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executeWithInput("");
                    }
                });

                builder.show();
            }
        });
    }



    private void executeWithInput(String userInput) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String code = codeInput.getText().toString();
                    String result = py.getModule("executor")
                            .callAttr("run_code", code, userInput)
                            .toString();
                    showResult(result, false);
                } catch (Exception e) {
                    showResult("Xəta: " + e.getMessage(), true);
                }
            }
        }).start();
    }

    private void setupAdMob() {
        mAdPythonCode = findViewById(R.id.adPythoneCode);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdPythonCode.loadAd(adRequest);
    }

    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void setupSyntaxHighlighting() {
        codeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                applySyntaxHighlighting(editable);
            }
        });
    }

    private void applySyntaxHighlighting(Editable editable) {
        if (isApplyingHighlighting) return;

        isApplyingHighlighting = true;

        try {
            String text = editable.toString();

            // Mevcut vurgulamaları temizle
            ForegroundColorSpan[] spans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
            for (ForegroundColorSpan span : spans) {
                editable.removeSpan(span);
            }

            if (text.isEmpty()) return;

            // Default color
            editable.setSpan(new ForegroundColorSpan(Color.WHITE), 0, editable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Comments
            highlightPattern(editable, "#.*", COLOR_COMMENT);
            highlightPattern(editable, "\"\"\"[\\s\\S]*?\"\"\"", COLOR_COMMENT);
            highlightPattern(editable, "'''[\\s\\S]*?'''", COLOR_COMMENT);

            // Strings
            highlightPattern(editable, "\"[^\"]*\"", COLOR_STRING);
            highlightPattern(editable, "'[^']*'", COLOR_STRING);

            // Numbers
            highlightPattern(editable, "\\b\\d+\\.?\\d*\\b", COLOR_NUMBER);
            highlightPattern(editable, "\\b\\d+\\.\\d+\\b", COLOR_NUMBER);

            // Classes
            highlightPattern(editable, "\\bclass\\s+(\\w+)", COLOR_CLASS);

            // Functions
            highlightPattern(editable, "\\bdef\\s+(\\w+)", COLOR_FUNCTION);

            // Keywords
            for (String keyword : PYTHON_KEYWORDS) {
                highlightPattern(editable, "\\b" + keyword + "\\b", COLOR_KEYWORD);
            }

            // Built-in functions
            for (String builtin : PYTHON_BUILTINS) {
                String funcName = builtin.replace("()", "");
                highlightPattern(editable, "\\b" + funcName + "\\b", COLOR_BUILTIN);
            }

            // Modules
            for (String module : PYTHON_MODULES) {
                highlightPattern(editable, "\\b" + module + "\\b", COLOR_MODULE);
            }

        } finally {
            isApplyingHighlighting = false;
        }
    }

    private void highlightPattern(Editable editable, String pattern, int color) {
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(editable);

            while (m.find()) {
                editable.setSpan(new ForegroundColorSpan(color), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupLineNumbers() {
        codeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLineNumbers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        codeInput.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    lineNumbers.scrollTo(0, codeInput.getScrollY());
                }
                return false;
            }
        });

        updateLineNumbers(codeInput.getText().toString());
    }

    private void updateLineNumbers(String text) {
        int lineCount = text.split("\n").length;
        if (text.length() > 0 && text.charAt(text.length() - 1) == '\n') {
            lineCount++;
        }

        StringBuilder numbers = new StringBuilder();
        for (int i = 1; i <= lineCount; i++) {
            numbers.append(i).append("\n");
        }

        lineNumbers.setText(numbers.toString());
    }

    private void setupAutoIndent() {
        codeInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                    // Biz artıq TextWatcher-də idarə edirik
                    return false;
                }
                return false;
            }
        });
    }

    private void saveCodeToStorage() {
        String code = codeInput.getText().toString();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("task_" + currentTaskId + "_code", code);
        editor.apply();
    }

    private void loadSavedCode() {
        String savedCode = preferences.getString("task_" + currentTaskId + "_code", "");
        if (savedCode.isEmpty() && initialCode != null) {
            savedCode = initialCode;
        }

        if (!savedCode.isEmpty()) {
            codeInput.setText(savedCode);
            updateLineNumbers(savedCode);
            applySyntaxHighlighting(codeInput.getText());
        }
    }

    private void runPythonCode() {
        String code = codeInput.getText().toString();

        if (code.trim().isEmpty()) {
            showResult("> Lütfen Python kodu yazın", true);
            return;
        }

        StringBuilder output = new StringBuilder();

        // Əvvəlcə sintaksis xətalarını yoxla
        List<CompilerError> errors = checkPythonSyntaxErrors(code);

        if (!errors.isEmpty()) {
            output.append("❌ PYTHON XƏTALARI:\n");
            output.append("══════════════════════════════════════\n\n");

            for (CompilerError error : errors) {
                output.append("📌 Sətir ").append(error.lineNumber).append(": ")
                        .append(error.errorType).append("\n");
                output.append("   ").append(error.message).append("\n\n");
            }

            output.append("⚠ Xətaları düzəldin və yenidən cəhd edin.\n");
            output.append("══════════════════════════════════════\n\n");
        } else {

            // JSON testləri varsa onları işlə
            if (tests != null && !tests.isEmpty()) {
                runPythonTests(code, output);
            } else {
                runBasicPythonAnalysis(code, output);
            }
        }

        // Real Python kodunu işlə
        executePythonCode(code, output);
    }

    private List<CompilerError> checkPythonSyntaxErrors(String code) {
        List<CompilerError> errors = new ArrayList<>();
        try {
            String[] lines = code.split("\n");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int lineNum = i + 1;

                // 1. Qeyri-balanse mötərizələr
                int openPar = countOccurrences(line, "(");
                int closePar = countOccurrences(line, ")");
                int openBracket = countOccurrences(line, "[");
                int closeBracket = countOccurrences(line, "]");
                int openBrace = countOccurrences(line, "{");
                int closeBrace = countOccurrences(line, "}");

                if (openPar != closePar) {
                    errors.add(new CompilerError("Mötərizələrdə balanssızlıq", lineNum, "Sintaksis xətası"));
                }
                if (openBracket != closeBracket) {
                    errors.add(new CompilerError("Kvadrat mötərizələrdə balanssızlıq", lineNum, "Sintaksis xətası"));
                }
                if (openBrace != closeBrace) {
                    errors.add(new CompilerError("Fiquarlı mötərizələrdə balanssızlıq", lineNum, "Sintaksis xətası"));
                }

                // 2. Qeyri-bağlanmış string
                int singleQuotes = countOccurrences(line, "'");
                int doubleQuotes = countOccurrences(line, "\"");

                if ((singleQuotes % 2 != 0) || (doubleQuotes % 2 != 0)) {
                    errors.add(new CompilerError("Qeyri-bağlanmış string", lineNum, "String xətası"));
                }

                String trimmedLine = line.trim();

                // 3. Def olmadan : istifadəsi
                if (trimmedLine.endsWith(":") && !trimmedLine.matches(".*\\b(def|class|if|elif|else|for|while|try|except|finally|with)\\b.*")) {
                    errors.add(new CompilerError("Yalnış : istifadəsi", lineNum, "Sintaksis xətası"));
                }

                // 4. Def sonra boşluq yoxluğu - DÜZƏLDİLDİ
                if (trimmedLine.startsWith("def ")) {
                    // "def " sonrasını al
                    String afterDef = trimmedLine.substring(4).trim();
                    if (afterDef.isEmpty()) {
                        errors.add(new CompilerError("def-dən sonra funksiya adı tələb olunur", lineNum, "Funksiya xətası"));
                    } else {
                        // Funksiya adını tap
                        String funcName = afterDef.split("[\\s(]")[0];
                        if (funcName.isEmpty() || !funcName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                            errors.add(new CompilerError("Yalnış funksiya adı: " + funcName, lineNum, "Funksiya xətası"));
                        }
                    }
                }

                // 5. Class sonra boşluq yoxluğu - DÜZƏLDİLDİ
                if (trimmedLine.startsWith("class ")) {
                    // "class " sonrasını al
                    String afterClass = trimmedLine.substring(6).trim();
                    if (afterClass.isEmpty()) {
                        errors.add(new CompilerError("class-dən sonra sinif adı tələb olunur", lineNum, "Sinif xətası"));
                    } else {
                        // Sinif adını tap
                        String className = afterClass.split("[\\s(:]")[0];
                        if (className.isEmpty() || !className.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                            errors.add(new CompilerError("Yalnış sinif adı: " + className, lineNum, "Sinif xətası"));
                        }
                    }
                }

                // 6. Import xətası
                if (trimmedLine.startsWith("import") && trimmedLine.contains(";")) {
                    errors.add(new CompilerError("import-da ; istifadə etməyin", lineNum, "Import xətası"));
                }

                // 7. Indentasiya xətası (qeyri-bərabər boşluqlar)
                if (!trimmedLine.isEmpty() && line.startsWith(" ") && !line.startsWith("    ")) {
                    int spaces = 0;
                    while (spaces < line.length() && line.charAt(spaces) == ' ') {
                        spaces++;
                    }
                    if (spaces % 4 != 0) {
                        errors.add(new CompilerError("Indentasiya 4 boşluq olmalıdır", lineNum, "Indentasiya xətası"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return errors;
    }

    private int countOccurrences(String str, String substr) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(substr, idx)) != -1) {
            count++;
            idx += substr.length();
        }
        return count;
    }

    private void runPythonTests(String code, StringBuilder output) {
        output.append("🧪 PYTHON TEST NƏTİCƏLƏRİ:\n\n");

        int passedTests = 0;
        int totalTests = tests.size();

        for (int i = 0; i < tests.size(); i++) {
            TaskModel.Test test = tests.get(i);
            boolean passed = runPythonSingleTest(code, test);
            if (passed) {
                passedTests++;
                output.append("✅ TEST ").append(i + 1).append(": ").append(test.getDescription()).append("\n");
                output.append("   Giriş: ").append(test.getInput()).append("\n");
                output.append("   Gözlənilən: ").append(test.getExpected()).append("\n");
                output.append("   Status: UĞURLU\n\n");
            } else {
                output.append("❌ TEST ").append(i + 1).append(": ").append(test.getDescription()).append("\n");
                output.append("   Giriş: ").append(test.getInput()).append("\n");
                output.append("   Gözlənilən: ").append(test.getExpected()).append("\n");
                output.append("   Status: UĞURSUZ\n\n");
            }
        }

        output.append("📊 ÜMUMİ TEST NƏTİCƏSİ: ").append(passedTests).append("/").append(totalTests);

        if (passedTests == totalTests) {
            output.append("\n\n🎉 TƏBRİKLƏR! Bütün testlər keçdi!");
            markTaskAsCompleted();
        } else {
            output.append("\n\n⚠ Bəzi testlər uğursuz oldu. Kodunuzu yoxlayın.");
        }

        output.append("\n════════════════════\n");
    }

    private boolean runPythonSingleTest(String code, TaskModel.Test test) {
        try {
            // Tapşırıq nömrəsinə görə xüsusi yoxlamalar
            switch (currentTaskId) {
                case 1: // Kvadrat tapşırığı
                    return checkSquareTask(code, test);
                case 2: // Faktorial tapşırığı
                    return checkFactorialTask(code, test);
                case 3: // Tək/cüt tapşırığı
                    return checkOddEvenTask(code, test);
                case 4: // Maksimum tapşırığı
                    return checkMaxTask(code, test);
                case 5: // Tərsinə çevir tapşırığı
                    return checkReverseTask(code, test);
                case 6: // Toplama tapşırığı
                    return checkSumTask(code, test);
                default:
                    return checkGenericTask(code, test);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Kvadrat tapşırığı üçün yoxlama
    private boolean checkSquareTask(String code, TaskModel.Test test) {
        String input = test.getInput();
        String expected = test.getExpected();

        // Ədədin kvadratını hesablayan kod olmalıdır
        boolean hasSquareLogic = code.contains("*") && code.contains("return") ||
                code.contains("** 2") ||
                code.contains("math.pow") ||
                code.contains("eded * eded");

        // Funksiya düzgün mü?
        boolean hasCorrectFunction = code.contains("def kvadrat") ||
                code.contains("def kvadrat(") ||
                code.contains("def kvadrat (");

        // Əgər test input və expected varsa, simulyasiya edək
        if (input != null && expected != null) {
            try {
                // Sadə simulyasiya
                int inputNum = Integer.parseInt(input.trim());
                int expectedNum = Integer.parseInt(expected.trim());

                // Kodda kvadrat əməliyyatı var?
                if (code.contains("return " + inputNum + " * " + inputNum) ||
                        code.contains("return " + inputNum + "*" + inputNum) ||
                        code.contains("return " + inputNum + "**2") ||
                        code.contains("return eded * eded") ||
                        code.contains("return eded**2")) {
                    return true;
                }
            } catch (NumberFormatException e) {
                // Ədəd deyil
            }
        }

        return hasSquareLogic && hasCorrectFunction;
    }

    // Faktorial tapşırığı üçün yoxlama
    private boolean checkFactorialTask(String code, TaskModel.Test test) {
        String input = test.getInput();
        String expected = test.getExpected();

        // Faktorial hesablayan məntiq
        boolean hasFactorialLogic = (code.contains("for") || code.contains("while")) &&
                (code.contains("*=") || code.contains("result *")) &&
                code.contains("return");

        // Funksiya adı düzgün mü?
        boolean hasCorrectFunction = code.contains("def faktorial") ||
                code.contains("def faktorial(");

        // Test dəyərləri ilə yoxlama
        if (input != null && expected != null) {
            try {
                int inputNum = Integer.parseInt(input.trim());
                int expectedNum = Integer.parseInt(expected.trim());

                // Məsələn: faktorial(5) = 120
                if (inputNum == 5 && expectedNum == 120) {
                    // Kodda 5-in faktorialını hesablamaq olmalı
                    if (code.contains("for") && code.contains("range") &&
                            (code.contains("1,") || code.contains("1, ")) &&
                            code.contains("result = 1")) {
                        return true;
                    }
                }
            } catch (NumberFormatException e) {
                // Ədəd deyil
            }
        }

        return hasFactorialLogic && hasCorrectFunction;
    }

    // Tək/cüt tapşırığı üçün yoxlama
    private boolean checkOddEvenTask(String code, TaskModel.Test test) {
        String input = test.getInput();
        String expected = test.getExpected();

        // Tək/cüt yoxlayan məntiq
        boolean hasOddEvenLogic = code.contains("% 2") &&
                (code.contains("==") || code.contains("!=")) &&
                (code.contains("Tək") || code.contains("Cüt") ||
                        code.contains("tek") || code.contains("cüt") ||
                        code.contains("odd") || code.contains("even"));

        // Test dəyərləri ilə yoxlama
        if (input != null && expected != null) {
            try {
                int inputNum = Integer.parseInt(input.trim());
                boolean isEven = expected.contains("Cüt") || expected.contains("cüt") || expected.contains("even");

                // Kod düzgün yoxlayır?
                if (isEven) {
                    // Cüt üçün
                    if (code.contains("% 2 == 0") && code.contains("Cüt")) {
                        return true;
                    }
                } else {
                    // Tək üçün
                    if ((code.contains("% 2 == 1") || code.contains("% 2 != 0")) && code.contains("Tək")) {
                        return true;
                    }
                }
            } catch (NumberFormatException e) {
                // Ədəd deyil
            }
        }

        return hasOddEvenLogic;
    }

    // Maksimum tapşırığı üçün yoxlama
    private boolean checkMaxTask(String code, TaskModel.Test test) {
        String input = test.getInput();
        String expected = test.getExpected();

        // Maksimum tapma məntiqi
        boolean hasMaxLogic = code.contains("if") &&
                code.contains(">") &&
                code.contains("return");

        // Funksiya adı
        boolean hasCorrectFunction = code.contains("def maksimum") ||
                code.contains("def max") ||
                code.contains("def maksimum(");

        // Test dəyərləri ilə yoxlama
        if (input != null && expected != null) {
            try {
                // Input formatı: "5, 10" kimi
                String[] parts = input.split(",");
                if (parts.length == 2) {
                    int a = Integer.parseInt(parts[0].trim());
                    int b = Integer.parseInt(parts[1].trim());
                    int expectedNum = Integer.parseInt(expected.trim());

                    // Kod düzgün müqayisə edir?
                    if ((a > b && expectedNum == a) || (b > a && expectedNum == b)) {
                        // Kodda bu məntiq var?
                        if ((code.contains("a > b") && code.contains("return a")) ||
                                (code.contains("b > a") && code.contains("return b")) ||
                                code.contains("max(a, b)")) {
                            return true;
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // Ədəd deyil
            }
        }

        return hasMaxLogic && hasCorrectFunction;
    }

    // Tərsinə çevir tapşırığı üçün yoxlama
    private boolean checkReverseTask(String code, TaskModel.Test test) {
        String input = test.getInput();
        String expected = test.getExpected();

        // String-i tərsinə çevirmə məntiqi
        boolean hasReverseLogic = (code.contains("for") || code.contains("while")) &&
                (code.contains("[::-1]") || code.contains("reversed") ||
                        code.contains("charAt") || code.contains("append"));

        // Test dəyərləri ilə yoxlama
        if (input != null && expected != null) {
            // Məsələn: "salam" -> "malas"
            String reversed = new StringBuilder(input).reverse().toString();
            if (expected.equals(reversed)) {
                // Kodda tərsinə çevirmə var?
                if (code.contains("[::-1]") ||
                        (code.contains("for") && code.contains("len") && code.contains("-1"))) {
                    return true;
                }
            }
        }

        return hasReverseLogic;
    }

    // Toplama tapşırığı üçün yoxlama
    private boolean checkSumTask(String code, TaskModel.Test test) {
        String input = test.getInput();
        String expected = test.getExpected();

        // Toplama məntiqi
        boolean hasSumLogic = code.contains("+") &&
                code.contains("return");

        // Test dəyərləri ilə yoxlama
        if (input != null && expected != null) {
            try {
                String[] parts = input.split(",");
                if (parts.length == 2) {
                    int a = Integer.parseInt(parts[0].trim());
                    int b = Integer.parseInt(parts[1].trim());
                    int expectedSum = Integer.parseInt(expected.trim());

                    if (a + b == expectedSum) {
                        // Kodda toplama var?
                        if (code.contains("return a + b") ||
                                code.contains("return " + a + " + " + b) ||
                                code.contains("cəm") || code.contains("cem") || code.contains("sum")) {
                            return true;
                        }
                    }
                }
            } catch (NumberFormatException e) {
                // Ədəd deyil
            }
        }

        return hasSumLogic;
    }

    // Ümumi tapşırıq yoxlaması
    private boolean checkGenericTask(String code, TaskModel.Test test) {
        // Həll yolunu yoxla
        if (solution != null && !solution.isEmpty()) {
            String cleanCode = code.replaceAll("\\s+", "").toLowerCase();
            String cleanSolution = solution.replaceAll("\\s+", "").toLowerCase();

            // Əgər həll yolu kodun içindədirsə
            if (cleanCode.contains(cleanSolution)) {
                return true;
            }

            // Əgər həll yolunun əhəmiyyətli hissəsi varsa
            if (cleanSolution.length() > 10) {
                String partialSolution = cleanSolution.substring(0, Math.min(10, cleanSolution.length()));
                if (cleanCode.contains(partialSolution)) {
                    return true;
                }
            }
        }

        // Əsas Python elementləri
        boolean hasBasicStructure = code.contains("def ") &&
                code.contains("return") &&
                !code.contains("return 0") &&
                !code.contains("return None");

        // Test output-u gözlənilən nəticəyə uyğundurmu?
        String expected = test.getExpected();
        if (expected != null) {
            // Kodda expected dəyər var?
            if (code.contains(expected)) {
                return true;
            }

            // Əgər expected ədəddirsə, kodda hesablama var?
            try {
                int expectedNum = Integer.parseInt(expected.trim());
                if (code.contains(String.valueOf(expectedNum))) {
                    return true;
                }
            } catch (NumberFormatException e) {
                // Ədəd deyil
            }
        }

        return hasBasicStructure;
    }

    private void runBasicPythonAnalysis(String code, StringBuilder output) {
        output.append("📊 KOD ANALİZİ:\n\n");

        // Sinif kontrolü
        if (code.contains("class")) {
            output.append("✓ Sinif tapıldı\n");
            Pattern classPattern = Pattern.compile("class\\s+(\\w+)");
            Matcher classMatcher = classPattern.matcher(code);
            if (classMatcher.find()) {
                output.append("✓ Sinif adı: ").append(classMatcher.group(1)).append("\n");
            }
        }

        // Funksiya sayı
        Pattern funcPattern = Pattern.compile("def\\s+(\\w+)");
        Matcher funcMatcher = funcPattern.matcher(code);
        int funcCount = 0;
        List<String> functions = new ArrayList<>();
        while (funcMatcher.find()) {
            funcCount++;
            functions.add(funcMatcher.group(1));
        }
        output.append("✓ Toplam funksiya: ").append(funcCount);
        if (!functions.isEmpty()) {
            output.append(" (");
            for (int i = 0; i < functions.size(); i++) {
                if (i > 0) output.append(", ");
                output.append(functions.get(i));
            }
            output.append(")");
        }
        output.append("\n");

        // Import sayı
        Pattern importPattern = Pattern.compile("import\\s+(\\w+)");
        Matcher importMatcher = importPattern.matcher(code);
        int importCount = 0;
        while (importMatcher.find()) {
            importCount++;
        }
        output.append("✓ Import sayı: ").append(importCount).append("\n");

        // Print sayı
        Pattern printPattern = Pattern.compile("print\\s*\\(");
        Matcher printMatcher = printPattern.matcher(code);
        int printCount = 0;
        while (printMatcher.find()) {
            printCount++;
        }
        output.append("✓ Print sayı: ").append(printCount).append("\n");

        output.append("\n");
    }

    private void executePythonCode(String code, StringBuilder output) {
        output.append("🚀 PYTHON ÇIXIŞI:\n");
        output.append("══════════════════\n");

        if (code.contains("input(")) {
            Pattern pattern = Pattern.compile("input\\(['\"]([^'\"]+)['\"]\\)");
            Matcher matcher = pattern.matcher(code);
            String prompt = "Dəyər daxil edin:";
            if (matcher.find()) {
                prompt = matcher.group(1);
            }
            showInputDialog(prompt);
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String result = py.getModule("executor")
                                .callAttr("run_code", code, "")
                                .toString();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                output.append("📋 Nəticə:\n");
                                output.append(result).append("\n");
                                output.append("══════════════════\n");

                                outputText.setText(output.toString());
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                output.append("❌ Çalışdırma xətası:\n");
                                output.append(e.getMessage()).append("\n");
                                output.append("══════════════════\n");

                                outputText.setText(output.toString());
                            }
                        });
                    }
                }
            }).start();
        }
    }

    // PythonConsole.java faylında
    private void markTaskAsCompleted() {
        try {
            Log.d("PythonConsole", "markTaskAsCompleted çağırıldı - Task ID: " + currentTaskId);

            // EYNİ KEY-İ İSTİFADƏ ET - CodeActivity ilə eyni olsun
            SharedPreferences taskPrefs = getSharedPreferences("TaskProgress", MODE_PRIVATE);
            SharedPreferences.Editor editor = taskPrefs.edit();

            // CodeActivity ilə EYNİ key strukturunu istifadə et
            editor.putBoolean("task_" + currentTaskId + "_completed", true);

            // Python üçün əlavə key də saxla (əgər PythonTaskAdapter ayrıca istifadə edirsə)
            editor.putBoolean("python_task_" + currentTaskId + "_completed", true);

            editor.apply();

            Log.d("PythonConsole", "Task " + currentTaskId + " SharedPreferences-ə yazıldı");

            // Toast və avtomatik geri qayıt
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PythonConsole.this,
                            "✅ Task " + currentTaskId + " tamamlandı!",
                            Toast.LENGTH_LONG).show();

                    // 1.5 saniyə sonra avtomatik geri qayıt
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Result göndər ki, list yenilənsin
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("task_id", currentTaskId);
                            returnIntent.putExtra("completed", true);
                            setResult(RESULT_OK, returnIntent);
                            finish();
                        }
                    }, 2500);
                }
            });

        } catch (Exception e) {
            Log.e("PythonConsole", "markTaskAsCompleted xətası: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCodeToStorage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveCodeToStorage();
    }

    @Override
    public void onBackPressed() {
        saveCodeToStorage();
        super.onBackPressed();
    }

    private void showResult(String text, boolean isError) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                outputText.setTextColor(isError ? 0xFFFF0000 : 0xFF00AA00);
                outputText.setText(text);
            }
        });
    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(PythonConsole.this);
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
                    saveCodeToStorage();
                    if (fAuth.getCurrentUser() != null) {
                        finish();

                    }

                }
            });
            materialAlertDialogBuilder.show();
        }
    };
}