package com.ali.pymain.taskmanager;

import android.content.ClipboardManager;
import android.content.Context;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.ali.aimain.taskAimanager.AiTaskModel;
import com.ali.pymain.taskmanager.library.AutoCompleteAdapter;
import com.ali.pymain.taskmanager.library.AutoCompleteEngine;
import com.ali.utils.SyncScrollView;
import com.ali.systemIn.R;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PythonConsole extends AppCompatActivity {

    // UI Elements
    private EditText codeInput;
    private TextView outputText;
    private TextView lineNumbers;
    private TextView taskDescription;
    private TextView taskTitle;
    private MaterialCardView taskCard;      // <--- YENİ: Task Card
    private ListView suggestionsList;
    private MaterialCardView outputContainer;
    private SyncScrollView lineNumbersScroll;
    private ScrollView codeScrollView;
    private Button runBtn;
    private Button dontKnowBtn;
    private Button copyOutputBtn;
    private Button clearOutputBtn;
    private Button hideOutputBtn;
    private AdView mAdPythonCode;
    private Toolbar toolbar;

    private static final String SYSTEM_STORAGE_DIR = "PythonEditorSystem";
    private static final String AUTO_SAVE_FILE = "autosave.pysave";

    // Data
    private Python py;
    private FirebaseAuth fAuth;
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "PythonConsolePrefs";
    private static final String PROJECTS_DIR = "PythonProjects";

    private int currentTaskId;
    private List<AiTaskModel.Test> tests;
    private String solution;
    private String initialCode;
    private String taskTitleText;
    private String taskDescriptionText;

    private boolean isTaskMode = false;  // <--- YENİ: Task rejimi yoxlamaq üçün

    // AutoComplete
    private AutoCompleteEngine acEngine;
    private AutoCompleteAdapter acAdapter;
    private List<AutoCompleteEngine.CompletionItem> currentSuggestions = new ArrayList<>();
    private static final int MAX_SUGGESTIONS = 12;

    // Undo/Redo
    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();
    private boolean isUndoRedoOp = false;

    // Syntax Highlighting
    private boolean isApplyingHighlight = false;
    private boolean syntaxHighlightingEnabled = true;
    private boolean autoIndentEnabled = true;
    private boolean autoCompleteEnabled = true;

    // Colors
    private static final int C_CLASS = Color.parseColor("#FF9800");
    private static final int C_FUNC = Color.parseColor("#4CAF50");
    private static final int C_KW = Color.parseColor("#82AAFF");
    private static final int C_STRING = Color.parseColor("#C3E88D");
    private static final int C_COMMENT = Color.parseColor("#546E7A");
    private static final int C_NUMBER = Color.parseColor("#F78C6C");
    private static final int C_BUILTIN = Color.parseColor("#FFCB6B");

    private static final String[] KEYWORDS = {
            "def", "class", "if", "else", "elif", "while", "for", "in", "return", "import",
            "from", "as", "try", "except", "finally", "with", "lambda", "and", "or", "not",
            "is", "None", "True", "False", "pass", "break", "continue", "global", "nonlocal"
    };

    private static final String[] BUILTINS = {
            "abs", "all", "any", "bin", "bool", "chr", "dict", "dir", "enumerate", "filter",
            "float", "int", "len", "list", "map", "max", "min", "ord", "range", "reversed",
            "round", "set", "sorted", "str", "sum", "tuple", "type", "zip", "open", "input",
            "print", "format"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_python_console);

        initViews();
        setupToolbar();
        setupTaskData();      // Task datasını yüklə
        setupFirebase();
        setupPython();
        setupPreferences();
        setupAutoComplete();
        setupEventListeners();
        setupSyncScrolling();
        setupAdMob();
        setupBackPressedCallback();

        saveToUndoStack();
        displayTaskInfo();

        // ========== SİSTEM QOVLUĞU - AVTOMATİK İŞƏ SAL ==========
        createSystemFolder();      // 1. Qovluğu yarat
        autoLoadFromSystem();      // 2. Əgər varsa avtomatik yüklə

        // Task-specific auto complete suggestions yüklə
        if (acEngine != null && taskTitleText != null) {
            acEngine.loadTaskSuggestions(currentTaskId, taskTitleText, taskDescriptionText);
        }
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
        taskCard = findViewById(R.id.taskCard);      // <--- YENİ
        toolbar = findViewById(R.id.toolbar);

        runBtn = findViewById(R.id.runBtn);
        dontKnowBtn = findViewById(R.id.dontKnowBtn);
        copyOutputBtn = findViewById(R.id.copyOutputBtn);
        clearOutputBtn = findViewById(R.id.clearOutputBtn);
        hideOutputBtn = findViewById(R.id.hideOutputBtn);
        mAdPythonCode = findViewById(R.id.adPythoneCode);
    }

    private void createSystemFolder() {
        try {
            File systemDir = new File(getFilesDir(), SYSTEM_STORAGE_DIR);
            if (!systemDir.exists()) {
                boolean created = systemDir.mkdirs();
                if (created) {
                    Log.d("SystemFolder", "✅ Sistem qovluğu yaradıldı: " + systemDir.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            Log.e("SystemFolder", "Xəta: " + e.getMessage());
        }
    }
    // 2. Faylı saxla (avtomatik)
    private void saveToSystemFolder(String content) {
        try {
            File systemDir = new File(getFilesDir(), SYSTEM_STORAGE_DIR);
            if (!systemDir.exists()) {
                createSystemFolder();
            }

            File file = new File(systemDir, AUTO_SAVE_FILE);
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(content);
            writer.close();
            fos.close();

            Log.d("SystemFolder", "💾 Avtomatik saxlanıldı");

        } catch (Exception e) {
            Log.e("SystemFolder", "Saxlama xətası: " + e.getMessage());
        }
    }
    // 3. Faylı yüklə (avtomatik)
    private String loadFromSystemFolder() {
        try {
            File systemDir = new File(getFilesDir(), SYSTEM_STORAGE_DIR);
            File file = new File(systemDir, AUTO_SAVE_FILE);

            if (!file.exists()) {
                Log.d("SystemFolder", "Heç bir saxlanmış fayl yoxdur");
                return null;
            }

            FileInputStream fis = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(reader);
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
            br.close();
            reader.close();
            fis.close();

            Log.d("SystemFolder", "📂 Avtomatik yükləndi");
            return content.toString();

        } catch (Exception e) {
            Log.e("SystemFolder", "Yükləmə xətası: " + e.getMessage());
            return null;
        }
    }

    // 4. Avtomatik yükləmə (onCreate üçün)
    private void autoLoadFromSystem() {
        String savedCode = loadFromSystemFolder();

        if (savedCode != null && !savedCode.isEmpty()) {
            codeInput.setText(savedCode);
            updateLineNumbers(savedCode);
            if (syntaxHighlightingEnabled) {
                applySyntaxHighlighting(codeInput.getText());
            }
            Log.d("SystemFolder", "✅ Son kod avtomatik yükləndi");
        } else if (isTaskMode && initialCode != null && !initialCode.isEmpty()) {
            // Task rejimindədirsə və heç bir saxlanmış kod yoxdursa, ilkin kodu yüklə
            codeInput.setText(initialCode);
            updateLineNumbers(initialCode);
            Log.d("SystemFolder", "📋 İlkin task kodu yükləndi");
        }
    }

    // 5. Avtomatik saxlama (hər dəfə kod dəyişdikdə)
    private void autoSaveToSystem() {
        String currentCode = codeInput.getText().toString();
        if (!currentCode.isEmpty()) {
            saveToSystemFolder(currentCode);
        }
    }

    private void setupEventListeners() {
        runBtn.setOnClickListener(v -> runPythonCode());

        if (dontKnowBtn != null) {
            if (isTaskMode) {
                dontKnowBtn.setOnClickListener(v -> showSolutionInCode());
            } else {
                dontKnowBtn.setOnClickListener(v -> showHelpForConsole());
            }
        }

        clearOutputBtn.setOnClickListener(v -> {
            outputText.setText("> Console cleared. Ready to run code...");
            Toast.makeText(this, "Console cleared", Toast.LENGTH_SHORT).show();
        });

        copyOutputBtn.setOnClickListener(v -> {
            String output = outputText.getText().toString();
            if (!output.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Output", output);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Output copied", Toast.LENGTH_SHORT).show();
            }
        });

        hideOutputBtn.setOnClickListener(v -> {
            if (outputContainer.getVisibility() == View.VISIBLE) {
                outputContainer.setVisibility(View.GONE);
                hideOutputBtn.setText("Show");
            } else {
                outputContainer.setVisibility(View.VISIBLE);
                hideOutputBtn.setText("Hide");
            }
        });

        codeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isUndoRedoOp) saveToUndoStack();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLineNumbers(s.toString());
                int cursor = start + count;
                if (cursor > s.length()) cursor = s.length();
                if (before > count && count == 0) {
                    hideSuggestions();
                } else {
                    updateSuggestions(s.toString(), cursor);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isApplyingHighlight && syntaxHighlightingEnabled) {
                    applySyntaxHighlighting(s);
                }
                saveCode();

                // ========== HƏR DƏFƏ KOD DƏYİŞDİKDƏ AVTOMATİK SAXLA ==========
                autoSaveToSystem();
            }
        });

        setupAutoIndent();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Task yoxdursa toolbar başlığını dəyiş
        if (!isTaskMode) {
            toolbar.setTitle("Python Console");
        } else {
            toolbar.setTitle("Python Editor");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            saveCode();
            finish();
            return true;
        } else if (id == R.id.menu_save_project) {
            showSaveProjectDialog();
            return true;
        } else if (id == R.id.menu_load_project) {
            showLoadProjectDialog();
            return true;
        } else if (id == R.id.menu_system_manage) {  // <--- YENİ
            showSystemManagementDialog();
            return true;
        } else if (id == R.id.menu_clear_code) {
            clearCurrentCode();
            return true;
        } else if (id == R.id.menu_copy_code) {
            copyAllCode();
            return true;
        } else if (id == R.id.menu_paste) {
            pasteCode();
            return true;
        } else if (id == R.id.menu_undo) {
            undo();
            return true;
        } else if (id == R.id.menu_redo) {
            redo();
            return true;
        } else if (id == R.id.menu_check_syntax) {
            checkSyntax();
            return true;
        } else if (id == R.id.menu_settings) {
            showSettings();
            return true;
        } else if (id == R.id.menu_about) {
            showAbout();
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
                updateLineNumbers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateLineNumbers(String text) {
        if (lineNumbers == null) return;

        int lineCount = text.split("\n", -1).length;
        if (text.endsWith("\n")) lineCount++;

        StringBuilder numbers = new StringBuilder();
        for (int i = 1; i <= lineCount; i++) {
            numbers.append(i).append("\n");
        }
        lineNumbers.setText(numbers.toString());
    }

    private void displayTaskInfo() {
        // Əgər task rejimi DEYİLSE, task card-ı GİZLƏT
        if (!isTaskMode) {
            if (taskCard != null) {
                taskCard.setVisibility(View.GONE);
            }
            // "Need Help" düyməsini də gizlət (task olmayanda lazım deyil)
            if (dontKnowBtn != null) {
                dontKnowBtn.setVisibility(View.GONE);
            }
            return;
        }

        // Task rejimindədirsə, card-ı göstər və məlumatları yenilə
        if (taskCard != null) {
            taskCard.setVisibility(View.VISIBLE);
        }
        if (dontKnowBtn != null) {
            dontKnowBtn.setVisibility(View.VISIBLE);
        }

        if (taskTitle != null && taskTitleText != null) {
            taskTitle.setText(taskTitleText);
        }
        if (taskDescription != null && taskDescriptionText != null) {
            taskDescription.setText(taskDescriptionText);
        }
    }

    private void setupAutoComplete() {
        acEngine = new AutoCompleteEngine(this);
        acAdapter = new AutoCompleteAdapter(this, currentSuggestions);
        suggestionsList.setAdapter(acAdapter);

        suggestionsList.setOnItemClickListener((parent, view, pos, id) -> {
            if (pos >= 0 && pos < currentSuggestions.size()) {
                insertCompletion(currentSuggestions.get(pos));
                hideSuggestions();
            }
        });
    }

    private void insertCompletion(AutoCompleteEngine.CompletionItem item) {
        try {
            int cursor = codeInput.getSelectionStart();
            String text = codeInput.getText().toString();

            int wordStart = cursor - 1;
            while (wordStart >= 0 && (Character.isLetterOrDigit(text.charAt(wordStart)) || text.charAt(wordStart) == '_')) {
                wordStart--;
            }
            wordStart = Math.max(0, wordStart + 1);

            Editable editable = codeInput.getText();
            String insertText = item.getInsertText();
            editable.replace(wordStart, cursor, insertText);
        } catch (Exception e) {
            Log.e("AC", "insertCompletion error: " + e.getMessage());
        }
    }

    private void updateSuggestions(String fullText, int cursor) {
        if (!autoCompleteEnabled) {
            hideSuggestions();
            return;
        }

        if (fullText == null || fullText.isEmpty() || cursor <= 0) {
            hideSuggestions();
            return;
        }

        try {
            String prefix = getLastWord(fullText, cursor);
            if (prefix.length() < 1) {
                hideSuggestions();
                return;
            }

            List<AutoCompleteEngine.CompletionItem> filtered = new ArrayList<>();

            // Task rejimindədirsə, task-specific suggestion-ları göstər
            if (isTaskMode && acEngine != null) {
                List<AutoCompleteEngine.CompletionItem> taskResults = acEngine.getTaskSuggestions(currentTaskId);
                for (AutoCompleteEngine.CompletionItem item : taskResults) {
                    // DÜZƏLİŞ: item.label istifadə et (getText() yox)
                    if (item.label.toLowerCase().startsWith(prefix.toLowerCase())) {
                        filtered.add(item);
                        if (filtered.size() >= MAX_SUGGESTIONS) break;
                    }
                }
            }

            // Global suggestionlar
            if (filtered.size() < MAX_SUGGESTIONS && acEngine != null) {
                List<AutoCompleteEngine.CompletionItem> globalResults = acEngine.getSuggestions(prefix, MAX_SUGGESTIONS - filtered.size());
                filtered.addAll(globalResults);
            }

            if (filtered.isEmpty()) {
                hideSuggestions();
            } else {
                showSuggestionList(filtered);
            }
        } catch (Exception e) {
            Log.e("PythonConsole", "updateSuggestions error: " + e.getMessage());
            hideSuggestions();
        }
    }

    private void showSuggestionList(List<AutoCompleteEngine.CompletionItem> items) {
        runOnUiThread(() -> {
            currentSuggestions.clear();
            currentSuggestions.addAll(items);
            acAdapter.notifyDataSetChanged();
            suggestionsList.setVisibility(View.VISIBLE);
        });
    }

    private void hideSuggestions() {
        runOnUiThread(() -> {
            currentSuggestions.clear();
            acAdapter.notifyDataSetChanged();
            suggestionsList.setVisibility(View.GONE);
        });
    }

    private String getLastWord(String text, int cursor) {
        if (cursor <= 0) return "";
        int start = cursor - 1;
        while (start >= 0 && (Character.isLetterOrDigit(text.charAt(start)) || text.charAt(start) == '_')) {
            start--;
        }
        start = Math.max(0, start + 1);
        return text.substring(start, cursor);
    }


    // Console rejimi üçün kömək
    private void showHelpForConsole() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Python Console Help")
                .setMessage("Python Console - İstənilən Python kodunu yazıb işlədə bilərsiniz.\n\n" +
                        "📝 İpuçları:\n" +
                        "• print() ilə nəticələri görə bilərsiniz\n" +
                        "• Ctrl+Z - Undo\n" +
                        "• Ctrl+Y - Redo\n" +
                        "• Ctrl+R - Run code\n" +
                        "• Ctrl+S - Save project\n\n" +
                        "💡 Misal:\n" +
                        "print('Salam Dünya!')\n" +
                        "x = 5 + 3\n" +
                        "print(x)")
                .setPositiveButton("OK", null)
                .show();
    }

    private void setupAutoIndent() {
        codeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (autoIndentEnabled && before == 0 && count == 1) {
                    char ch = s.charAt(start);
                    if (ch == '\n') autoIndent(start + 1, s.toString(), start);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void autoIndent(int pos, String fullText, int newlinePos) {
        Editable ed = codeInput.getText();
        if (pos <= 0) return;

        int lineStart = fullText.lastIndexOf('\n', newlinePos - 1);
        if (lineStart == -1) lineStart = 0; else lineStart++;

        String prevLine = fullText.substring(lineStart, Math.min(newlinePos, fullText.length())).trim();
        int level = calcIndentLevel(fullText, newlinePos);
        if (prevLine.endsWith(":")) level = Math.max(0, level);

        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < level; i++) indent.append("    ");
        if (pos <= ed.length()) ed.insert(pos, indent);
    }

    private int calcIndentLevel(String fullText, int position) {
        String[] lines = fullText.substring(0, Math.min(position, fullText.length())).split("\n");
        int level = 0;
        for (String line : lines) {
            String t = line.trim();
            if (t.endsWith(":") && !t.startsWith("#")) level++;
            if (t.equals("pass") || t.equals("return") || t.startsWith("return ")) {
                level = Math.max(0, level - 1);
            }
        }
        return Math.max(0, level);
    }

    private void applySyntaxHighlighting(Editable ed) {
        if (!syntaxHighlightingEnabled || isApplyingHighlight || ed == null) return;
        isApplyingHighlight = true;
        try {
            for (ForegroundColorSpan s : ed.getSpans(0, ed.length(), ForegroundColorSpan.class))
                ed.removeSpan(s);
            if (ed.length() == 0) return;

            ed.setSpan(new ForegroundColorSpan(Color.WHITE), 0, ed.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            hlPattern(ed, "#[^\\n]*", C_COMMENT);
            hlPattern(ed, "\"\"\"[\\s\\S]*?\"\"\"", C_COMMENT);
            hlPattern(ed, "'''[\\s\\S]*?'''", C_COMMENT);
            hlPattern(ed, "\"[^\"\\n]*\"", C_STRING);
            hlPattern(ed, "'[^'\\n]*'", C_STRING);
            hlPattern(ed, "\\b\\d+\\.?\\d*\\b", C_NUMBER);
            hlPattern(ed, "\\bclass\\s+\\w+", C_CLASS);
            hlPattern(ed, "\\bdef\\s+\\w+", C_FUNC);
            for (String kw : KEYWORDS) hlPattern(ed, "\\b" + kw + "\\b", C_KW);
            for (String bi : BUILTINS) hlPattern(ed, "\\b" + bi + "\\b", C_BUILTIN);
        } finally {
            isApplyingHighlight = false;
        }
    }

    private void hlPattern(Editable ed, String pattern, int color) {
        try {
            Matcher m = Pattern.compile(pattern).matcher(ed);
            while (m.find())
                ed.setSpan(new ForegroundColorSpan(color), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } catch (Exception e) {}
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
            updateLineNumbers(prev);
            if (syntaxHighlightingEnabled) applySyntaxHighlighting(codeInput.getText());
            isUndoRedoOp = false;
            Toast.makeText(this, "Undo", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show();
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            isUndoRedoOp = true;
            undoStack.push(codeInput.getText().toString());
            String next = redoStack.pop();
            codeInput.setText(next);
            updateLineNumbers(next);
            if (syntaxHighlightingEnabled) applySyntaxHighlighting(codeInput.getText());
            isUndoRedoOp = false;
            Toast.makeText(this, "Redo", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Nothing to redo", Toast.LENGTH_SHORT).show();
        }
    }

    private void runPythonCode() {
        String code = codeInput.getText().toString();
        if (code.trim().isEmpty()) {
            showOutput("Please write some Python code first", true);
            return;
        }

        runBtn.setEnabled(false);
        runBtn.setText("Running...");

        new Handler().postDelayed(() -> {
            try {
                StringBuilder out = new StringBuilder();
                out.append("═══════════════════════════════\n");
                out.append("         EXECUTION RESULT        \n");
                out.append("═══════════════════════════════\n\n");

                String result = py.getModule("executor").callAttr("run_code", code, "").toString();
                out.append(result.isEmpty() ? "Code executed successfully\n" : result);
                out.append("\n═══════════════════════════════\n");
                showOutput(out.toString(), false);
            } catch (Exception e) {
                showOutput("Error: " + e.getMessage(), true);
            } finally {
                runBtn.setEnabled(true);
                runBtn.setText("Run Code");
            }
        }, 100);
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

    private void showSolutionInCode() {
        if (solution == null || solution.isEmpty()) {
            Toast.makeText(this, "No solution available!", Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Show Solution")
                .setMessage("This will replace your current code. Are you sure?")
                .setPositiveButton("Yes", (d, w) -> {
                    saveToUndoStack();
                    redoStack.clear();
                    codeInput.setText(solution);
                    updateLineNumbers(solution);
                    if (syntaxHighlightingEnabled) applySyntaxHighlighting(codeInput.getText());
                    showOutput("Solution code loaded.", false);
                    Toast.makeText(this, "Solution loaded!", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void clearCurrentCode() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear Code")
                .setMessage("Are you sure you want to clear all code?")
                .setPositiveButton("Yes", (d, w) -> {
                    saveToUndoStack();
                    redoStack.clear();
                    codeInput.setText("");
                    updateLineNumbers("");
                    showOutput("Code cleared.", false);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void copyAllCode() {
        String code = codeInput.getText().toString();
        if (code.isEmpty()) {
            Toast.makeText(this, "No code to copy", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Python Code", code);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Code copied", Toast.LENGTH_SHORT).show();
    }

    private void pasteCode() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            android.content.ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            String pastedText = item.getText().toString();
            if (pastedText != null && !pastedText.isEmpty()) {
                int cursorPosition = codeInput.getSelectionStart();
                codeInput.getText().insert(cursorPosition, pastedText);
                Toast.makeText(this, "Code pasted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 1. Sistem qovluğundakı bütün faylları siyahılaşdır
    private List<String> listAllSystemFiles() {
        List<String> files = new ArrayList<>();
        try {
            File systemDir = new File(getFilesDir(), SYSTEM_STORAGE_DIR);
            if (systemDir.exists()) {
                File[] fileList = systemDir.listFiles();
                if (fileList != null) {
                    for (File file : fileList) {
                        if (file.isFile()) {
                            files.add(file.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("SystemFolder", "Listeleme xətası: " + e.getMessage());
        }
        return files;
    }

    // 2. Spesifik faylı sil
    private void deleteFileFromSystem(String fileName) {
        try {
            File systemDir = new File(getFilesDir(), SYSTEM_STORAGE_DIR);
            File file = new File(systemDir, fileName);

            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    Toast.makeText(this, "🗑️ Silindi: " + fileName, Toast.LENGTH_SHORT).show();
                    Log.d("SystemFolder", "Fayl silindi: " + file.getAbsolutePath());
                } else {
                    Toast.makeText(this, "❌ Silinə bilmədi: " + fileName, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "❌ Fayl tapılmadı: " + fileName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("SystemFolder", "Silinmə xətası: " + e.getMessage());
            Toast.makeText(this, "Xəta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 3. Bütün sistem qovluğunu təmizlə
    private void clearAllSystemFiles() {
        try {
            File systemDir = new File(getFilesDir(), SYSTEM_STORAGE_DIR);
            if (systemDir.exists()) {
                File[] fileList = systemDir.listFiles();
                if (fileList != null) {
                    int deletedCount = 0;
                    for (File file : fileList) {
                        if (file.isFile() && file.delete()) {
                            deletedCount++;
                        }
                    }
                    Toast.makeText(this, "✅ " + deletedCount + " fayl silindi", Toast.LENGTH_SHORT).show();
                    Log.d("SystemFolder", "Bütün fayllar silindi. Silinən sayı: " + deletedCount);
                }
            }
        } catch (Exception e) {
            Log.e("SystemFolder", "Təmizləmə xətası: " + e.getMessage());
            Toast.makeText(this, "Xəta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 4. Sistem qovluğunun ünvanını göstər
    private void showSystemFolderPath() {
        File systemDir = new File(getFilesDir(), SYSTEM_STORAGE_DIR);
        String path = systemDir.getAbsolutePath();

        // Fayl ölçüsünü hesabla
        long totalSize = 0;
        if (systemDir.exists()) {
            File[] files = systemDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        totalSize += file.length();
                    }
                }
            }
        }

        String sizeText = formatFileSize(totalSize);
        int fileCount = systemDir.exists() ? (systemDir.listFiles() != null ? systemDir.listFiles().length : 0) : 0;

        new MaterialAlertDialogBuilder(this)
                .setTitle("📁 Sistem Qovluğu Məlumatı")
                .setMessage("📍 Ünvan:\n" + path + "\n\n" +
                        "📄 Fayl sayı: " + fileCount + "\n" +
                        "💾 Ümumi ölçü: " + sizeText + "\n\n" +
                        "💡 Bu ünvanı kopyalaya bilərsiniz")
                .setPositiveButton("📋 Kopyala", (d, w) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Path", path);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Ünvan kopyalandı", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("🗑️ Təmizlə", (d, w) -> {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Təsdiq")
                            .setMessage("Bütün faylları silmək istədiyinizə əminsiniz?")
                            .setPositiveButton("Bəli", (d2, w2) -> clearAllSystemFiles())
                            .setNegativeButton("Xeyr", null)
                            .show();
                })
                .setNegativeButton("Bağla", null)
                .show();
    }

    // 5. Bütün faylları göstərən dialoq (silme seçimi ilə)
    private void showAllFilesWithDeleteOption() {
        List<String> files = listAllSystemFiles();

        if (files.isEmpty()) {
            Toast.makeText(this, "📁 Sistem qovluğunda heç bir fayl yoxdur", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fayl adlarını ölçüləri ilə birlikdə göstər
        String[] fileArray = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
            String fileName = files.get(i);
            long fileSize = getFileSize(fileName);
            fileArray[i] = fileName + " (" + formatFileSize(fileSize) + ")";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📁 Sistem Qovluğu (" + files.size() + " fayl)");
        builder.setItems(fileArray, (dialog, which) -> {
            String selectedFile = files.get(which);

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Fayl: " + selectedFile)
                    .setMessage("Nə etmək istəyirsiniz?")
                    .setPositiveButton("📂 Yüklə", (d, w) -> {
                        String content = loadFromSystemFolder(); // Bu ən son avtomatik saxlanmış faylı yükləyir
                        if (content != null) {
                            saveToUndoStack();
                            redoStack.clear();
                            codeInput.setText(content);
                            updateLineNumbers(content);
                            if (syntaxHighlightingEnabled) {
                                applySyntaxHighlighting(codeInput.getText());
                            }
                            Toast.makeText(this, "✅ Yükləndi: " + selectedFile, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNeutralButton("🗑️ Sil", (d, w) -> {
                        deleteFileFromSystem(selectedFile);
                    })
                    .setNegativeButton("❌ Ləğv", null)
                    .show();
        });
        builder.setNegativeButton("Bağla", null);
        builder.show();
    }

    // 6. Fayl ölçüsünü almaq
    private long getFileSize(String fileName) {
        try {
            File systemDir = new File(getFilesDir(), SYSTEM_STORAGE_DIR);
            File file = new File(systemDir, fileName);
            if (file.exists()) {
                return file.length();
            }
        } catch (Exception e) {
            Log.e("SystemFolder", "Ölçü hesablama xətası: " + e.getMessage());
        }
        return 0;
    }

    // 7. Fayl ölçüsünü formatla (KB, MB, GB)
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    // 8. Cari fayl haqqında məlumat (hansı fayl saxlanılır)
    private void showCurrentFileInfo() {
        File systemDir = new File(getFilesDir(), SYSTEM_STORAGE_DIR);
        File currentFile = new File(systemDir, AUTO_SAVE_FILE);

        String status = currentFile.exists() ? "✅ Mövcuddur" : "❌ Mövcud deyil";
        String size = currentFile.exists() ? formatFileSize(currentFile.length()) : "0 B";
        String lastModified = currentFile.exists() ?
                new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(new Date(currentFile.lastModified())) :
                "Yoxdur";

        new MaterialAlertDialogBuilder(this)
                .setTitle("📄 Cari Fayl Məlumatı")
                .setMessage("📁 Fayl adı: " + AUTO_SAVE_FILE + "\n" +
                        "📍 Tam ünvan:\n" + currentFile.getAbsolutePath() + "\n\n" +
                        "📊 Vəziyyət: " + status + "\n" +
                        "💾 Ölçü: " + size + "\n" +
                        "🕐 Son dəyişiklik: " + lastModified + "\n\n" +
                        "💡 Qeyd: Hər kod dəyişikliyində avtomatik saxlanılır")
                .setPositiveButton("📂 Qovluğu Aç", (d, w) -> {
                    showSystemFolderPath();
                })
                .setNegativeButton("Bağla", null)
                .show();
    }

    // 9. Menyudan çağırmaq üçün dialoq (BÜTÜN ƏMƏLİYYATLAR BİR YERDƏ)
    private void showSystemManagementDialog() {
        String[] options = {
                "📁 Qovluq Məlumatı (Ünvan)",
                "📄 Cari Fayl Məlumatı",
                "📋 Bütün Faylları Göstər",
                "🗑️ Bütün Faylları Sil",
                "💾 Yaddaş Məlumatı"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("📁 Sistem Qovluğu İdarəetmə")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showSystemFolderPath();
                            break;
                        case 1:
                            showCurrentFileInfo();
                            break;
                        case 2:
                            showAllFilesWithDeleteOption();
                            break;
                        case 3:
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle("⚠️ Təsdiq")
                                    .setMessage("BÜTÜN sistem fayllarını silmək istədiyinizə əminsiniz?\nBu əməliyyat geri alına bilməz!")
                                    .setPositiveButton("Bəli, Sil", (d, w) -> clearAllSystemFiles())
                                    .setNegativeButton("Xeyr", null)
                                    .show();
                            break;
                        case 4:
                            showStorageInfo();
                            break;
                    }
                })
                .setNegativeButton("Bağla", null)
                .show();
    }

    // 10. Yaddaş məlumatını göstər
    private void showStorageInfo() {
        File systemDir = new File(getFilesDir(), SYSTEM_STORAGE_DIR);

        // Ümumi və boş yaddaş
        long totalSpace = getFilesDir().getTotalSpace();
        long freeSpace = getFilesDir().getFreeSpace();
        long usedSpace = totalSpace - freeSpace;

        // Sistem qovluğunun ölçüsü
        long systemFolderSize = 0;
        int fileCount = 0;
        if (systemDir.exists()) {
            File[] files = systemDir.listFiles();
            if (files != null) {
                fileCount = files.length;
                for (File file : files) {
                    if (file.isFile()) {
                        systemFolderSize += file.length();
                    }
                }
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("💾 Yaddaş Məlumatı")
                .setMessage("📱 Ümumi yaddaş: " + formatFileSize(totalSpace) + "\n" +
                        "✅ Boş yaddaş: " + formatFileSize(freeSpace) + "\n" +
                        "📊 İstifadə olunan: " + formatFileSize(usedSpace) + "\n\n" +
                        "📁 Sistem qovluğu: " + formatFileSize(systemFolderSize) + "\n" +
                        "📄 Fayl sayı: " + fileCount + "\n\n" +
                        "📍 Qovluq ünvanı:\n" + systemDir.getAbsolutePath())
                .setPositiveButton("OK", null)
                .show();
    }

    private void checkSyntax() {
        String code = codeInput.getText().toString();
        if (code.trim().isEmpty()) {
            showOutput("No code to check", true);
            return;
        }

        StringBuilder errors = new StringBuilder();
        String[] lines = code.split("\n");
        boolean hasError = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNum = i + 1;
            if (countOcc(line, "(") != countOcc(line, ")")) {
                errors.append("Line ").append(lineNum).append(": Parenthesis mismatch\n");
                hasError = true;
            }
            if (countOcc(line, "[") != countOcc(line, "]")) {
                errors.append("Line ").append(lineNum).append(": Bracket mismatch\n");
                hasError = true;
            }
        }

        if (hasError) {
            showOutput("Syntax Errors:\n" + errors.toString(), true);
        } else {
            showOutput("No syntax errors found!", false);
        }
    }

    private int countOcc(String str, String sub) {
        int count = 0, idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    private void showSaveProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Project");
        final EditText input = new EditText(this);
        input.setHint("Project name...");
        String defaultName = "project_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        if (isTaskMode) {
            defaultName = "task_" + currentTaskId + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        }
        input.setText(defaultName);
        builder.setView(input);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String projectName = input.getText().toString().trim();
            if (!projectName.isEmpty()) saveProject(projectName);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveProject(String projectName) {
        try {
            // 1. Öz qovluğuna saxla (.pyproj)
            File projectsDir = new File(getFilesDir(), PROJECTS_DIR);
            if (!projectsDir.exists()) projectsDir.mkdirs();

            String safeFileName = projectName.replaceAll("[^a-zA-Z0-9_.-]", "_");
            File projectFile = new File(projectsDir, safeFileName + ".pyproj");

            Gson gson = new Gson();
            ProjectData projectData = new ProjectData();
            projectData.name = projectName;
            projectData.code = codeInput.getText().toString();
            projectData.taskId = isTaskMode ? currentTaskId : -1;
            projectData.timestamp = System.currentTimeMillis();

            FileOutputStream fos = new FileOutputStream(projectFile);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(gson.toJson(projectData));
            writer.close();
            fos.close();

            Toast.makeText(this, "✅ Project saved: " + projectName, Toast.LENGTH_LONG).show();

            // 2. DOWNLOADS QOVLUĞUNA .py UZANTISI İLƏ YAZ
            String downloadsFileName = projectName + ".py";  // <--- .py uzantısı

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, downloadsFileName);
                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/x-python");
                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS);

                android.net.Uri uri = getContentResolver().insert(android.provider.MediaStore.Files.getContentUri("external"), values);
                if (uri != null) {
                    java.io.OutputStream os = getContentResolver().openOutputStream(uri);
                    os.write(codeInput.getText().toString().getBytes());
                    os.close();
                    Toast.makeText(this, "✅ Downloads-a yazıldı: " + downloadsFileName, Toast.LENGTH_SHORT).show();
                }
            } else {
                File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                File downloadsFile = new File(downloadsDir, downloadsFileName);
                FileOutputStream fos2 = new FileOutputStream(downloadsFile);
                OutputStreamWriter writer2 = new OutputStreamWriter(fos2);
                writer2.write(codeInput.getText().toString());
                writer2.close();
                fos2.close();
                Toast.makeText(this, "✅ Downloads-a yazıldı: " + downloadsFileName, Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Save error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoadProjectDialog() {
        try {
            File projectsDir = new File(getFilesDir(), PROJECTS_DIR);
            if (!projectsDir.exists()) {
                Toast.makeText(this, "No projects found", Toast.LENGTH_SHORT).show();
                return;
            }

            File[] projectFiles = projectsDir.listFiles((dir, name) -> name.endsWith(".pyproj"));
            if (projectFiles == null || projectFiles.length == 0) {
                Toast.makeText(this, "No projects found", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> projectNames = new ArrayList<>();
            List<File> files = new ArrayList<>();

            for (File file : projectFiles) {
                projectNames.add(file.getName().replace(".pyproj", ""));
                files.add(file);
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Load Project");
            builder.setItems(projectNames.toArray(new String[0]), (dialog, which) -> loadProject(files.get(which)));
            builder.setNegativeButton("Cancel", null);
            builder.show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProject(File projectFile) {
        try {
            FileInputStream fis = new FileInputStream(projectFile);
            InputStreamReader reader = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(reader);
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) json.append(line);
            br.close();
            reader.close();
            fis.close();

            Gson gson = new Gson();
            ProjectData data = gson.fromJson(json.toString(), ProjectData.class);

            if (data != null) {
                saveToUndoStack();
                redoStack.clear();
                codeInput.setText(data.code);
                updateLineNumbers(data.code);
                if (syntaxHighlightingEnabled) applySyntaxHighlighting(codeInput.getText());
                Toast.makeText(this, "Loaded: " + data.name, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Load error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showSettings() {
        new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setItems(new String[]{
                        "Syntax Highlighting: " + (syntaxHighlightingEnabled ? "ON" : "OFF"),
                        "Auto-indent: " + (autoIndentEnabled ? "ON" : "OFF"),
                        "Auto-complete: " + (autoCompleteEnabled ? "ON" : "OFF")
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            syntaxHighlightingEnabled = !syntaxHighlightingEnabled;
                            if (syntaxHighlightingEnabled) {
                                applySyntaxHighlighting(codeInput.getText());
                            }
                            Toast.makeText(this, "Syntax highlighting: " + (syntaxHighlightingEnabled ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            autoIndentEnabled = !autoIndentEnabled;
                            Toast.makeText(this, "Auto-indent: " + (autoIndentEnabled ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            autoCompleteEnabled = !autoCompleteEnabled;
                            if (!autoCompleteEnabled) hideSuggestions();
                            Toast.makeText(this, "Auto-complete: " + (autoCompleteEnabled ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("About Python Editor")
                .setMessage("Python Editor v1.0\n\nProfessional Python IDE with:\n• Syntax Highlighting\n• Auto-complete\n• Project Management\n• Undo/Redo\n\n© 2024")
                .setPositiveButton("OK", null)
                .show();
    }

    private void saveCode() {
        if (preferences != null) {
            String key = isTaskMode ? "task_" + currentTaskId + "_code" : "console_code";
            preferences.edit().putString(key, codeInput.getText().toString()).apply();
        }
    }

    private void loadSavedCode() {
        if (preferences == null) return;

        String key = isTaskMode ? "task_" + currentTaskId + "_code" : "console_code";
        String saved = preferences.getString(key, "");

        // Əgər saved code yoxdursa və task rejimindədirsə, INITIAL CODE-dan istifadə et
        if (saved.isEmpty() && isTaskMode && initialCode != null && !initialCode.isEmpty()) {
            saved = initialCode;
        }

        if (!saved.isEmpty()) {
            codeInput.setText(saved);
            updateLineNumbers(saved);
            if (syntaxHighlightingEnabled) applySyntaxHighlighting(codeInput.getText());
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // TASK DATA - Task rejimini yoxla
    // ═════════════════════════════════════════════════════════════════════
    private void setupTaskData() {
        Intent intent = getIntent();
        if (intent == null) return;

        // Task ID -1 və ya yoxdursa, console rejimi
        currentTaskId = intent.getIntExtra("TASK_ID", -1);

        // Əgər TASK_ID varsa və -1 deyilsə, task rejimi
        if (currentTaskId != -1) {
            isTaskMode = true;
            taskTitleText = intent.getStringExtra("TASK_TITLE");
            taskDescriptionText = intent.getStringExtra("TASK_DESCRIPTION");
            initialCode = intent.getStringExtra("INITIAL_CODE");
            solution = intent.getStringExtra("TASK_SOLUTION");
            String testsJson = intent.getStringExtra("TASK_TESTS");

            Log.d("PythonConsole", "Task Mode ACTIVE - ID: " + currentTaskId);
            Log.d("PythonConsole", "Task Title: " + taskTitleText);
            Log.d("PythonConsole", "Initial Code: " + (initialCode != null ? initialCode.substring(0, Math.min(100, initialCode.length())) : "NULL"));

            if (testsJson != null && !testsJson.isEmpty()) {
                Type type = new TypeToken<List<AiTaskModel.Test>>() {}.getType();
                tests = new Gson().fromJson(testsJson, type);
            }
        } else {
            isTaskMode = false;
            Log.d("PythonConsole", "Console Mode ACTIVE - No Task");
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
    }

    private void setupAdMob() {
        if (mAdPythonCode != null) {
            mAdPythonCode.loadAd(new AdRequest.Builder().build());
        }
    }

    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new MaterialAlertDialogBuilder(PythonConsole.this)
                        .setTitle("Exit")
                        .setMessage("Save your code before exiting?")
                        .setPositiveButton("Save & Exit", (d, i) -> {
                            saveCode();
                            finish();
                        })
                        .setNegativeButton("Exit", (d, i) -> finish())
                        .setNeutralButton("Cancel", (d, i) -> d.dismiss())
                        .show();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.isCtrlPressed()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_Z:
                    undo();
                    return true;
                case KeyEvent.KEYCODE_Y:
                    redo();
                    return true;
                case KeyEvent.KEYCODE_R:
                    runPythonCode();
                    return true;
                case KeyEvent.KEYCODE_S:
                    showSaveProjectDialog();
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
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

        // ========== ÇIXIŞ ZAMANI SON VƏZİYYƏTİ SAXLA ==========
        autoSaveToSystem();
    }

    // ProjectData inner class
    private static class ProjectData {
        String name;
        String code;
        int taskId;
        long timestamp;
        String description;
    }
}