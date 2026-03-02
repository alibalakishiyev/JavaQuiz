package com.ali.pymain.taskmanager;

// Importları düzəldək
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

// DÜZƏLİŞ: AiTaskModel import edirik
import com.ali.aimain.taskAimanager.AiTaskModel;
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
import java.util.Stack;
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

    // DÜZƏLİŞ: AiTaskModel.Test istifadə edirik
    private int currentTaskId;
    private List<AiTaskModel.Test> tests;
    private String solution;
    private String initialCode;

    private LinearLayout outputContainer;
    private LinearLayout codeEditorLayout;
    private Button hideOutputBtn;
    private Button dontKnowBtn;

    // Undo/Redo üçün dəyişənlər
    private Button undoBtn;
    private Button redoBtn;
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private boolean isUndoRedoOperation = false;

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
                "copy()"
        });
        put("dict", new String[]{
                "keys()", "values()", "items()", "get(key, default)", "pop(key)",
                "popitem()", "clear()", "copy()", "update(other_dict)", "setdefault(key, default)"
        });
        put("int", new String[]{
                "bit_length()", "to_bytes(length, byteorder)", "from_bytes(bytes, byteorder)"
        });
        put("float", new String[]{
                "is_integer()", "hex()", "fromhex(string)", "as_integer_ratio()"
        });
    }};

    // Python anahtar kelimeleri
    private static final String[] PYTHON_KEYWORDS = {
            "def", "class", "if", "else", "elif", "while", "for", "in", "return",
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
            "print()"
    };

    // Python module adları
    private static final String[] PYTHON_MODULES = {
            "math", "random", "datetime", "json", "re", "os", "sys", "time",
            "collections", "itertools", "functools", "decimal", "fractions",
            "statistics", "hashlib", "base64", "csv", "html", "numpy", "pandas"
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

    // Test nəticəsi üçün inner class
    private class TestResult {
        boolean passed;
        String actual;
        String error;

        TestResult(boolean passed, String actual, String error) {
            this.passed = passed;
            this.actual = actual;
            this.error = error;
        }
    }

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
        setupPreferences();
        setupEventListeners();
        setupAutoComplete();
        setupSuggestions();
        setupAdMob();
        setupBackPressedCallback();

        // Undo stack-ə ilkin kodu yadda saxla
        saveToUndoStack();
    }

    private void initializeViews() {
        codeInput = findViewById(R.id.codeInput);
        outputText = findViewById(R.id.outputText);
        lineNumbers = findViewById(R.id.lineNumbers);
        outputContainer = findViewById(R.id.outputContainer);
        codeEditorLayout = findViewById(R.id.codeEditorLayout);
        hideOutputBtn = findViewById(R.id.hideOutputBtn);
        Button backBtn = findViewById(R.id.backBtn);
        Button clearBtn = findViewById(R.id.clearBtn);
        Button runBtn = findViewById(R.id.runBtn);
        Button clearOutputBtn = findViewById(R.id.clearOutputBtn);
        suggestionsList = findViewById(R.id.suggestionsList);
        taskDescription = findViewById(R.id.taskDescription);

        // Yeni düymələr
        dontKnowBtn = findViewById(R.id.dontKnowBtn);
        undoBtn = findViewById(R.id.undoBtn);
        redoBtn = findViewById(R.id.redoBtn);

        Log.d("PythonConsole", "Views initialized - dontKnowBtn: " + (dontKnowBtn != null));
        Log.d("PythonConsole", "undoBtn: " + (undoBtn != null) + ", redoBtn: " + (redoBtn != null));
    }

    // DÜZƏLİŞ: AiTaskModel.Test istifadə edirik
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

            // Parse tests from JSON - AiTaskModel.Test üçün
            if (testsJson != null && !testsJson.isEmpty()) {
                Gson gson = new Gson();
                Type testListType = new TypeToken<List<AiTaskModel.Test>>() {}.getType();
                tests = gson.fromJson(testsJson, testListType);
                Log.d("PythonConsole", "Loaded " + (tests != null ? tests.size() : 0) + " tests");
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
        setupUndoRedo();

        // Back butonu
        backBtn.setOnClickListener(v -> {
            saveCodeToStorage();
            onBackPressed();
        });

        // Run butonu
        runBtn.setOnClickListener(v -> {
            Log.d("PythonConsole", "RUN düyməsinə basıldı");
            showOutputPanel();
            runPythonCode();
        });

        // Clear butonu
        clearBtn.setOnClickListener(v -> {
            saveToUndoStack();
            clearRedoStack();
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

        // Bilmirəm düyməsi
        if (dontKnowBtn != null) {
            dontKnowBtn.setOnClickListener(v -> {
                Log.d("PythonConsole", "Bilmirəm düyməsinə basıldı");
                showSolutionInCode();
            });
        }

        // Undo düyməsi
        if (undoBtn != null) {
            undoBtn.setOnClickListener(v -> undo());
        }

        // Redo düyməsi
        if (redoBtn != null) {
            redoBtn.setOnClickListener(v -> redo());
        }
    }

    private void setupUndoRedo() {
        codeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isUndoRedoOperation) {
                    saveToUndoStack();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateUndoRedoButtons();
            }
        });
    }

    private void saveToUndoStack() {
        String currentCode = codeInput.getText().toString();
        if (undoStack.isEmpty() || !undoStack.peek().equals(currentCode)) {
            undoStack.push(currentCode);
            if (undoStack.size() > 50) {
                undoStack.removeElementAt(0);
            }
        }
    }

    private void clearRedoStack() {
        redoStack.clear();
    }

    private void undo() {
        if (undoStack.size() > 1) {
            isUndoRedoOperation = true;

            String currentCode = codeInput.getText().toString();
            redoStack.push(currentCode);

            undoStack.pop();
            String previousCode = undoStack.peek();

            codeInput.setText(previousCode);
            updateLineNumbers(previousCode);
            applySyntaxHighlighting(codeInput.getText());

            isUndoRedoOperation = false;
            updateUndoRedoButtons();

            Toast.makeText(this, "Geri qaytarıldı", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Geri qaytarmaq mümkün deyil", Toast.LENGTH_SHORT).show();
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            isUndoRedoOperation = true;

            String codeToRestore = redoStack.pop();

            undoStack.push(codeInput.getText().toString());

            codeInput.setText(codeToRestore);
            updateLineNumbers(codeToRestore);
            applySyntaxHighlighting(codeInput.getText());

            isUndoRedoOperation = false;
            updateUndoRedoButtons();

            Toast.makeText(this, "İrəli qaytarıldı", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "İrəli qaytarmaq mümkün deyil", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUndoRedoButtons() {
        if (undoBtn != null) {
            undoBtn.setEnabled(undoStack.size() > 1);
        }
        if (redoBtn != null) {
            redoBtn.setEnabled(!redoStack.isEmpty());
        }
    }

    private void showOutputPanel() {
        runOnUiThread(() -> {
            try {
                if (outputContainer == null) {
                    outputContainer = findViewById(R.id.outputContainer);
                }

                if (outputContainer != null) {
                    if (outputContainer.getVisibility() != View.VISIBLE) {
                        outputContainer.setVisibility(View.VISIBLE);

                        LinearLayout.LayoutParams editorParams = (LinearLayout.LayoutParams) findViewById(R.id.codeEditorLayout).getLayoutParams();
                        LinearLayout.LayoutParams outputParams = (LinearLayout.LayoutParams) outputContainer.getLayoutParams();

                        editorParams.weight = 1f;
                        outputParams.weight = 1f;

                        outputContainer.requestLayout();
                        findViewById(R.id.codeEditorLayout).requestLayout();

                        ScrollView scrollView = (ScrollView) outputContainer.getChildAt(1);
                        if (scrollView != null) {
                            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("PythonConsole", "showOutputPanel xətası: " + e.getMessage());
            }
        });
    }

    private void hideOutputPanel() {
        if (outputContainer != null && outputContainer.getVisibility() == View.VISIBLE) {
            outputContainer.setVisibility(View.GONE);
        }
    }

    private void showSolutionInCode() {
        if (solution != null && !solution.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Həll kodunu göstər")
                    .setMessage("Bu əməliyyat cari kodun üzərinə həll kodunu yazacaq. Davam etmək istəyirsiniz?")
                    .setPositiveButton("Bəli", (dialog, which) -> {
                        saveToUndoStack();
                        clearRedoStack();

                        String fullSolution = prepareSolutionCode(solution);
                        codeInput.setText(fullSolution);
                        updateLineNumbers(fullSolution);
                        applySyntaxHighlighting(codeInput.getText());
                        saveCodeToStorage();

                        Toast.makeText(PythonConsole.this,
                                "Həll kodu əlavə edildi! Kodları öyrənin və sonra test edin.",
                                Toast.LENGTH_LONG).show();

                        showOutputPanel();
                        outputText.setText("> Həll kodu əlavə edildi. İndi kodu işlədə bilərsiniz.");
                    })
                    .setNegativeButton("Xeyr", null)
                    .show();
        } else {
            Toast.makeText(this, "Bu tapşırıq üçün həll kodu mövcud deyil!", Toast.LENGTH_SHORT).show();
        }
    }

    private String prepareSolutionCode(String solutionCode) {
        StringBuilder fullCode = new StringBuilder();

        String taskTitle = getIntent().getStringExtra("TASK_TITLE");

        fullCode.append("# ===========================================\n");
        fullCode.append("# TAPŞIRIQ: ").append(taskTitle != null ? taskTitle : "Task " + currentTaskId).append("\n");
        fullCode.append("# HƏLL KODU (avtomatik əlavə edildi)\n");
        fullCode.append("# ===========================================\n\n");

        String functionName = extractFunctionName(initialCode);

        if (functionName != null && !functionName.isEmpty()) {
            String functionHeader = findFunctionHeader(initialCode, functionName);
            if (functionHeader != null) {
                fullCode.append(functionHeader).append("\n");

                String[] solutionLines = solutionCode.split("\n");
                for (String line : solutionLines) {
                    fullCode.append("    ").append(line).append("\n");
                }
                fullCode.append("\n");
            } else {
                fullCode.append(initialCode).append("\n\n");
                fullCode.append("# Həll kodu:\n");
                fullCode.append(solutionCode).append("\n\n");
            }
        } else {
            fullCode.append(initialCode).append("\n\n");
            fullCode.append("# Həll kodu:\n");
            fullCode.append(solutionCode).append("\n\n");
        }

        String testCode = extractTestCode(initialCode);
        if (testCode != null && !testCode.isEmpty()) {
            fullCode.append("\n# ===========================================\n");
            fullCode.append("# TEST KODU:\n");
            fullCode.append("# ===========================================\n");
            fullCode.append(testCode).append("\n");
        }

        return fullCode.toString();
    }

    private String extractFunctionName(String code) {
        if (code == null || code.isEmpty()) return null;

        Pattern pattern = Pattern.compile("def\\s+(\\w+)\\s*\\(");
        Matcher matcher = pattern.matcher(code);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String findFunctionHeader(String code, String functionName) {
        if (code == null || functionName == null) return null;

        String[] lines = code.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith("def " + functionName)) {
                return line.trim();
            }
        }
        return null;
    }

    private String extractTestCode(String code) {
        if (code == null || code.isEmpty()) return null;

        StringBuilder testCode = new StringBuilder();
        String[] lines = code.split("\n");
        boolean inTestSection = false;

        for (String line : lines) {
            if (line.trim().startsWith("# Test") || line.trim().contains("print(")) {
                inTestSection = true;
            }

            if (inTestSection) {
                testCode.append(line).append("\n");
            }
        }

        return testCode.length() > 0 ? testCode.toString() : null;
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

                if (!isDeleting && count == 1 && s.charAt(start) == '.') {
                    showSmartCompletion(s.toString(), start);
                } else if (s.length() > 0) {
                    showSuggestions(s.toString(), start + count);
                } else {
                    hideSuggestions();
                }

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
            int currentLineStart = fullText.lastIndexOf('\n', newlinePosition - 1);
            if (currentLineStart == -1) currentLineStart = 0;
            else currentLineStart++;

            String currentLine = fullText.substring(currentLineStart, Math.min(newlinePosition, fullText.length()));
            String currentLineTrimmed = currentLine.trim();

            if (currentLineTrimmed.equals("pass") || currentLineTrimmed.equals("return") ||
                    currentLineTrimmed.startsWith("#") || currentLineTrimmed.isEmpty()) {
                int indentLevel = calculateIndentLevelForPython(fullText, newlinePosition);

                StringBuilder indent = new StringBuilder();
                for (int i = 0; i < indentLevel; i++) {
                    indent.append("    ");
                }

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

            if (trimmedLine.contains("\"\"\"") || trimmedLine.contains("'''")) {
                int count = countOccurrences(trimmedLine, "\"\"\"") + countOccurrences(trimmedLine, "'''");
                if (count % 2 != 0) {
                    inBlockComment = !inBlockComment;
                }
            }

            if (!inBlockComment && !trimmedLine.startsWith("#")) {
                if (trimmedLine.endsWith(":") && !trimmedLine.contains("#")) {
                    indentLevel++;
                }

                if (trimmedLine.isEmpty() || trimmedLine.equals("pass") ||
                        trimmedLine.equals("return") || trimmedLine.startsWith("return ")) {
                    indentLevel = Math.max(0, indentLevel - 1);
                }
            }
        }

        return Math.max(0, indentLevel);
    }

    private void autoAddIndentAfterColon(int position) {
        new Handler().postDelayed(() -> {
            Editable editable = codeInput.getText();
            int currentPos = codeInput.getSelectionStart();

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

            int indentLevel = indentCount / 4;
            indentLevel++;

            StringBuilder newIndent = new StringBuilder();
            for (int i = 0; i < indentLevel; i++) {
                newIndent.append("    ");
            }

            String newText = "\n" + newIndent.toString();
            editable.insert(currentPos, newText);

            codeInput.setSelection(currentPos + 1 + newIndent.length());
        }, 100);
    }

    private void showSmartCompletion(String text, int position) {
        try {
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
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this,
                                android.R.layout.simple_list_item_1,
                                methods
                        );
                        suggestionsList.setAdapter(adapter);
                        suggestionsList.setVisibility(View.VISIBLE);
                        suggestionsAdapter = adapter;
                        return;
                    }
                }
            }

            List<String> allSuggestions = new ArrayList<>();
            allSuggestions.addAll(Arrays.asList(PYTHON_KEYWORDS));
            allSuggestions.addAll(Arrays.asList(PYTHON_BUILTINS));
            allSuggestions.addAll(Arrays.asList(PYTHON_MODULES));

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_list_item_1,
                    allSuggestions
            );
            suggestionsList.setAdapter(adapter);
            suggestionsList.setVisibility(View.VISIBLE);
            suggestionsAdapter = adapter;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String detectVariableType(String code, String varName) {
        String[] lines = code.split("\n");

        for (String line : lines) {
            line = line.trim();

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

            suggestionsList.setOnItemClickListener((parent, view, position, id) -> {
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
            });

            codeInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    hideSuggestions();
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

            suggestionsAdapter.clear();
            for (String item : filtered) {
                suggestionsAdapter.add(item);
            }
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
        if (suggestionsList != null) {
            suggestionsList.setVisibility(View.GONE);
        }
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

            ForegroundColorSpan[] spans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
            for (ForegroundColorSpan span : spans) {
                editable.removeSpan(span);
            }

            if (text.isEmpty()) return;

            editable.setSpan(new ForegroundColorSpan(Color.WHITE), 0, editable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            highlightPattern(editable, "#.*", COLOR_COMMENT);
            highlightPattern(editable, "\"\"\"[\\s\\S]*?\"\"\"", COLOR_COMMENT);
            highlightPattern(editable, "'''[\\s\\S]*?'''", COLOR_COMMENT);
            highlightPattern(editable, "\"[^\"]*\"", COLOR_STRING);
            highlightPattern(editable, "'[^']*'", COLOR_STRING);
            highlightPattern(editable, "\\b\\d+\\.?\\d*\\b", COLOR_NUMBER);
            highlightPattern(editable, "\\bclass\\s+(\\w+)", COLOR_CLASS);
            highlightPattern(editable, "\\bdef\\s+(\\w+)", COLOR_FUNCTION);

            for (String keyword : PYTHON_KEYWORDS) {
                highlightPattern(editable, "\\b" + keyword + "\\b", COLOR_KEYWORD);
            }

            for (String builtin : PYTHON_BUILTINS) {
                String funcName = builtin.replace("()", "");
                highlightPattern(editable, "\\b" + funcName + "\\b", COLOR_BUILTIN);
            }

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

        codeInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                lineNumbers.scrollTo(0, codeInput.getScrollY());
            }
            return false;
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
        codeInput.setOnKeyListener((v, keyCode, event) -> false);
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
        }

        if (tests != null && !tests.isEmpty()) {
            runPythonTests(code, output);
        } else {
            runBasicPythonAnalysis(code, output);
        }

        executePythonCode(code, output);
    }

    private List<CompilerError> checkPythonSyntaxErrors(String code) {
        List<CompilerError> errors = new ArrayList<>();
        try {
            String[] lines = code.split("\n");

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                int lineNum = i + 1;

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

                int singleQuotes = countOccurrences(line, "'");
                int doubleQuotes = countOccurrences(line, "\"");

                if ((singleQuotes % 2 != 0) || (doubleQuotes % 2 != 0)) {
                    errors.add(new CompilerError("Qeyri-bağlanmış string", lineNum, "String xətası"));
                }

                String trimmedLine = line.trim();

                if (trimmedLine.endsWith(":") && !trimmedLine.matches(".*\\b(def|class|if|elif|else|for|while|try|except|finally|with)\\b.*")) {
                    errors.add(new CompilerError("Yalnış : istifadəsi", lineNum, "Sintaksis xətası"));
                }

                if (trimmedLine.startsWith("def ")) {
                    String afterDef = trimmedLine.substring(4).trim();
                    if (afterDef.isEmpty()) {
                        errors.add(new CompilerError("def-dən sonra funksiya adı tələb olunur", lineNum, "Funksiya xətası"));
                    } else {
                        String funcName = afterDef.split("[\\s(]")[0];
                        if (funcName.isEmpty() || !funcName.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                            errors.add(new CompilerError("Yalnış funksiya adı: " + funcName, lineNum, "Funksiya xətası"));
                        }
                    }
                }

                if (trimmedLine.startsWith("class ")) {
                    String afterClass = trimmedLine.substring(6).trim();
                    if (afterClass.isEmpty()) {
                        errors.add(new CompilerError("class-dən sonra sinif adı tələb olunur", lineNum, "Sinif xətası"));
                    } else {
                        String className = afterClass.split("[\\s(:]")[0];
                        if (className.isEmpty() || !className.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                            errors.add(new CompilerError("Yalnış sinif adı: " + className, lineNum, "Sinif xətası"));
                        }
                    }
                }

                if (trimmedLine.startsWith("import") && trimmedLine.contains(";")) {
                    errors.add(new CompilerError("import-da ; istifadə etməyin", lineNum, "Import xətası"));
                }

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

    // DÜZƏLİŞ: AiTaskModel.Test istifadə edirik
    private void runPythonTests(String code, StringBuilder output) {
        output.append("🧪 PYTHON TEST NƏTİCƏLƏRİ:\n\n");

        int passedTests = 0;
        int totalTests = tests.size();
        boolean allTestsPassed = true;

        for (int i = 0; i < tests.size(); i++) {
            AiTaskModel.Test test = tests.get(i);
            TestResult result = runPythonSingleTestWithDetails(code, test);

            if (result.passed) {
                passedTests++;
                output.append("✅ TEST ").append(i + 1).append(": ").append(test.getDescription()).append("\n");
                output.append("   Giriş: ").append(test.getInput()).append("\n");
                output.append("   Gözlənilən: ").append(test.getExpected()).append("\n");
                output.append("   Alınan: ").append(result.actual).append("\n");
                output.append("   Status: UĞURLU\n\n");
            } else {
                allTestsPassed = false;
                output.append("❌ TEST ").append(i + 1).append(": ").append(test.getDescription()).append("\n");
                output.append("   Giriş: ").append(test.getInput()).append("\n");
                output.append("   Gözlənilən: ").append(test.getExpected()).append("\n");
                output.append("   Alınan: ").append(result.actual).append("\n");
                if (!result.error.isEmpty()) {
                    output.append("   Xəta: ").append(result.error).append("\n");
                }
                output.append("   Status: UĞURSUZ\n\n");
            }
        }

        output.append("📊 ÜMUMİ TEST NƏTİCƏSİ: ").append(passedTests).append("/").append(totalTests);

        if (allTestsPassed && totalTests > 0) {
            output.append("\n\n🎉 TƏBRİKLƏR! Bütün testlər keçdi!");
            markTaskAsCompleted();
        } else {
            output.append("\n\n⚠ Bəzi testlər uğursuz oldu. Kodunuzu yoxlayın.");
            if (passedTests == totalTests - 1) {
                output.append("\n💪 Az qalıb! Bir test daha keçsəydi tamamlanacaqdı.");
            }
        }

        output.append("\n════════════════════\n");
        outputText.setText(output.toString());
    }

    // DÜZƏLİŞ: AiTaskModel.Test istifadə edirik
    private TestResult runPythonSingleTestWithDetails(String code, AiTaskModel.Test test) {
        try {
            String testInput = test.getInput();
            String expectedStr = test.getExpected();

            String fullCode = code + "\n\n";
            fullCode += "# Test icrası\n";
            fullCode += "try:\n";
            fullCode += "    # Funksiyanı çağır\n";
            fullCode += "    actual_result = " + testInput + "\n";
            fullCode += "    \n";
            fullCode += "    # Nəticəni yoxla\n";
            fullCode += "    print('ACTUAL:' + repr(actual_result))\n";
            fullCode += "    \n";
            fullCode += "    # Gözlənilən nəticə\n";
            fullCode += "    expected = " + expectedStr + "\n";
            fullCode += "    print('EXPECTED:' + repr(expected))\n";
            fullCode += "    \n";
            fullCode += "    # Müqayisə et\n";
            fullCode += "    if actual_result == expected:\n";
            fullCode += "        print('TEST_PASSED')\n";
            fullCode += "    else:\n";
            fullCode += "        print('TEST_FAILED')\n";
            fullCode += "        \n";
            fullCode += "except Exception as e:\n";
            fullCode += "    print('TEST_ERROR:' + repr(e))";

            Log.d("PythonTest", "Test kodu:\n" + fullCode);

            String output = py.getModule("executor")
                    .callAttr("run_code", fullCode, "")
                    .toString();

            Log.d("PythonTest", "Test output:\n" + output);

            String[] lines = output.split("\n");
            String actual = "";
            boolean passed = false;
            String error = "";

            for (String line : lines) {
                if (line.startsWith("ACTUAL:")) {
                    actual = line.substring(7).trim();
                } else if (line.startsWith("TEST_PASSED")) {
                    passed = true;
                } else if (line.startsWith("TEST_ERROR:")) {
                    error = line.substring(11).trim();
                }
            }

            return new TestResult(passed, actual, error);

        } catch (Exception e) {
            Log.e("PythonTest", "Test icrası xətası: " + e.getMessage());
            e.printStackTrace();
            return new TestResult(false, "", "Xəta: " + e.getMessage());
        }
    }

    private void runBasicPythonAnalysis(String code, StringBuilder output) {
        output.append("📊 KOD ANALİZİ:\n\n");

        if (code.contains("class")) {
            output.append("✓ Sinif tapıldı\n");
            Pattern classPattern = Pattern.compile("class\\s+(\\w+)");
            Matcher classMatcher = classPattern.matcher(code);
            if (classMatcher.find()) {
                output.append("✓ Sinif adı: ").append(classMatcher.group(1)).append("\n");
            }
        }

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

        Pattern importPattern = Pattern.compile("import\\s+(\\w+)");
        Matcher importMatcher = importPattern.matcher(code);
        int importCount = 0;
        while (importMatcher.find()) {
            importCount++;
        }
        output.append("✓ Import sayı: ").append(importCount).append("\n");

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

        try {
            String result = py.getModule("executor")
                    .callAttr("run_code", code, "")
                    .toString();

            output.append("📋 Nəticə:\n");
            output.append(result).append("\n");

            if (code.contains("input(")) {
                Pattern pattern = Pattern.compile("input\\(['\"]([^'\"]+)['\"]\\)");
                Matcher matcher = pattern.matcher(code);
                if (matcher.find()) {
                    String prompt = matcher.group(1);
                    output.append("\n⚠ Input tələb olunur: ").append(prompt).append("\n");
                }
            }

        } catch (Exception e) {
            output.append("❌ Çalışdırma xətası:\n");
            output.append(e.getMessage()).append("\n");
        }

        output.append("══════════════════\n");
        outputText.setText(output.toString());
    }

    private void markTaskAsCompleted() {
        try {
            Log.d("PythonConsole", "=== TASK TAMAMLAMA BAŞLADI ===");
            Log.d("PythonConsole", "Task ID: " + currentTaskId);

            // Intent-dən task tipini al
            String taskType = getIntent().getStringExtra("TASK_TYPE");
            Log.d("PythonConsole", "Task Type: " + taskType);

            SharedPreferences prefs;
            String key;

            // Task tipinə görə preference açarını təyin et
            if (taskType != null && taskType.equals("ai")) {
                // AI taskı üçün
                prefs = getSharedPreferences("AiTaskProgress", MODE_PRIVATE);
                key = "ai_task_" + currentTaskId + "_completed";
                Log.d("PythonConsole", "AI taskı tamamlanır, açar: " + key);
            } else {
                // Python taskı üçün
                prefs = getSharedPreferences("TaskProgress", MODE_PRIVATE);
                key = "task_" + currentTaskId + "_completed";
                Log.d("PythonConsole", "Python taskı tamamlanır, açar: " + key);
            }

            // Preference-a yaz
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(key, true);
            editor.apply();

            // Yoxlama
            boolean saved = prefs.getBoolean(key, false);
            Log.d("PythonConsole", "Yazıldı? " + saved);

            // AI taskı üçün əlavə olaraq digər preference-lərə də yazaq (köməkçi)
            if (taskType != null && taskType.equals("ai")) {
                SharedPreferences extraPrefs = getSharedPreferences("TaskProgress", MODE_PRIVATE);
                extraPrefs.edit().putBoolean("task_" + currentTaskId + "_completed", true).apply();
                Log.d("PythonConsole", "Əlavə olaraq TaskProgress-ə də yazıldı");
            }

            Log.d("PythonConsole", "=== TASK TAMAMLAMA BITDI ===");

            runOnUiThread(() -> {
                Toast.makeText(PythonConsole.this,
                        "✅ Task " + currentTaskId + " tamamlandı!",
                        Toast.LENGTH_LONG).show();

                Intent returnIntent = new Intent();
                returnIntent.putExtra("TASK_ID", currentTaskId);
                returnIntent.putExtra("COMPLETED", true);
                returnIntent.putExtra("TASK_TYPE", taskType); // Task tipini geri qaytar
                setResult(RESULT_OK, returnIntent);

                // Activity-ni bağla
                finish();
            });

        } catch (Exception e) {
            Log.e("PythonConsole", "Xəta: " + e.getMessage());
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
        runOnUiThread(() -> {
            outputText.setTextColor(isError ? 0xFFFF0000 : 0xFF00AA00);
            outputText.setText(text);
        });
    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(PythonConsole.this);
            materialAlertDialogBuilder.setTitle(R.string.app_name);
            materialAlertDialogBuilder.setMessage("Are you sure want to exit?");
            materialAlertDialogBuilder.setNegativeButton(android.R.string.no, (dialog, i) -> dialog.dismiss());
            materialAlertDialogBuilder.setPositiveButton(android.R.string.yes, (dialog, i) -> {
                saveCodeToStorage();
                if (fAuth.getCurrentUser() != null) {
                    finish();
                }
            });
            materialAlertDialogBuilder.show();
        }
    };
}