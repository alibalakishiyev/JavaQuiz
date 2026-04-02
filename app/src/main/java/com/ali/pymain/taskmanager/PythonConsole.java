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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ali.aimain.taskAimanager.AiTaskModel;
import com.ali.pymain.taskmanager.library.AutoCompleteAdapter;
import com.ali.pymain.taskmanager.library.AutoCompleteEngine;
import com.ali.systemIn.R;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
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

    // ── UI ────────────────────────────────────────────────────────────────
    private EditText codeInput;
    private TextView outputText;
    private AdView mAdPythonCode;
    private TextView lineNumbers;
    private TextView taskDescription;
    private ListView suggestionsList;
    private LinearLayout outputContainer;
    private LinearLayout codeEditorLayout;
    private Button hideOutputBtn;
    private Button dontKnowBtn;
    private Button undoBtn;
    private Button redoBtn;
    private Button menuBtn;
    private Button backBtn;
    private Button runBtn;
    private Button clearOutputBtn;
    private Button copyOutputBtn;

    // ── Məlumat ───────────────────────────────────────────────────────────
    private Python py;
    private FirebaseAuth fAuth;
    private SharedPreferences preferences;
    private static final String PREFS_NAME = "PythonConsolePrefs";
    private static final String PREF_USER_LOGGED_IN = "user_logged_in";
    private static final String PROJECTS_DIR = "PythonProjects";

    private int currentTaskId;
    private List<AiTaskModel.Test> tests;
    private String solution;
    private String initialCode;

    // ── AutoComplete Engine ──────────────────────────────────────────────
    private AutoCompleteEngine acEngine;
    private AutoCompleteAdapter acAdapter;
    private List<AutoCompleteEngine.CompletionItem> currentSuggestions = new ArrayList<>();
    private static final int MAX_SUGGESTIONS = 12;

    // ── Undo / Redo ───────────────────────────────────────────────────────
    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();
    private boolean isUndoRedoOp = false;

    // ── Syntax highlighting ───────────────────────────────────────────────
    private boolean isApplyingHighlight = false;
    private boolean syntaxHighlightingEnabled = true;
    private boolean autoIndentEnabled = true;
    private boolean autoCompleteEnabled = true;

    private static final int C_CLASS = Color.parseColor("#FF9800");
    private static final int C_FUNC = Color.parseColor("#4CAF50");
    private static final int C_KW = Color.parseColor("#82AAFF");
    private static final int C_STRING = Color.parseColor("#C3E88D");
    private static final int C_COMMENT = Color.parseColor("#546E7A");
    private static final int C_NUMBER = Color.parseColor("#F78C6C");
    private static final int C_BUILTIN = Color.parseColor("#FFCB6B");
    private static final int C_MODULE = Color.parseColor("#89DDFF");

    // ── Daxili siniflər ───────────────────────────────────────────────────
    private static class TestResult {
        boolean passed;
        String actual;
        String error;
        TestResult(boolean p, String a, String e) {
            passed = p;
            actual = a;
            error = e;
        }
    }

    private static class CompilerError {
        String message;
        int lineNumber;
        String errorType;
        CompilerError(String m, int l, String t) {
            message = m;
            lineNumber = l;
            errorType = t;
        }
    }

    private static class ProjectData {
        String name;
        String code;
        int taskId;
        long timestamp;
        String description;
    }

    // ═════════════════════════════════════════════════════════════════════
    // onCreate
    // ═════════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_python_console);

        initViews();
        setupTaskData();
        setupFirebase();
        setupPython();
        setupPreferences();

        acEngine = new AutoCompleteEngine(this);
        Log.d("PythonConsole", "AutoComplete yükləndi: " + acEngine.getTotalCount() + " element");

        setupAutoCompleteList();
        setupEventListeners();
        setupSyntaxHighlighting();
        setupLineNumbers();
        setupAdMob();
        setupBackPressedCallback();

        saveToUndoStack();
        updateUndoRedoButtons();
    }

    // ═════════════════════════════════════════════════════════════════════
    // View inisializasiyası
    // ═════════════════════════════════════════════════════════════════════
    private void initViews() {
        codeInput = findViewById(R.id.codeInput);
        outputText = findViewById(R.id.outputText);
        lineNumbers = findViewById(R.id.lineNumbers);
        outputContainer = findViewById(R.id.outputContainer);
        codeEditorLayout = findViewById(R.id.codeEditorLayout);
        hideOutputBtn = findViewById(R.id.hideOutputBtn);
        suggestionsList = findViewById(R.id.suggestionsList);
        taskDescription = findViewById(R.id.taskDescription);
        dontKnowBtn = findViewById(R.id.dontKnowBtn);
        undoBtn = findViewById(R.id.undoBtn);
        redoBtn = findViewById(R.id.redoBtn);
        menuBtn = findViewById(R.id.menuBtn);
        backBtn = findViewById(R.id.backBtn);
        runBtn = findViewById(R.id.runBtn);
        clearOutputBtn = findViewById(R.id.clearOutputBtn);
        copyOutputBtn = findViewById(R.id.copyOutputBtn);
    }

    // ═════════════════════════════════════════════════════════════════════
    // AutoComplete
    // ═════════════════════════════════════════════════════════════════════
    private void setupAutoCompleteList() {
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
            while (wordStart >= 0 &&
                    (Character.isLetterOrDigit(text.charAt(wordStart)) || text.charAt(wordStart) == '_')) {
                wordStart--;
            }
            wordStart = Math.max(0, wordStart + 1);

            boolean afterDot = wordStart > 0 && text.charAt(wordStart - 1) == '.';

            Editable editable = codeInput.getText();
            String insertText = item.getInsertText();

            if (afterDot) {
                editable.replace(wordStart, cursor, insertText);
            } else {
                editable.replace(wordStart, cursor, insertText);
            }

            String newText = editable.toString();
            int parenPos = insertText.indexOf('(');
            if (parenPos != -1) {
                int newCursor = wordStart + parenPos + 1;
                if (newCursor <= newText.length()) {
                    codeInput.setSelection(newCursor);
                }
            } else {
                int newCursor = wordStart + insertText.length();
                if (newCursor <= newText.length()) {
                    codeInput.setSelection(newCursor);
                }
            }
        } catch (Exception e) {
            Log.e("AC", "insertCompletion xətası: " + e.getMessage());
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
            if (cursor > 1 && fullText.charAt(cursor - 1) == '.') {
                String varName = getLastWord(fullText, cursor - 1);
                if (!varName.isEmpty()) {
                    List<AutoCompleteEngine.CompletionItem> dotItems =
                            acEngine.getDotCompletions(fullText, varName);
                    if (!dotItems.isEmpty()) {
                        showSuggestionList(dotItems);
                        return;
                    }
                }
            }

            String prefix = getLastWord(fullText, cursor);
            if (prefix.length() < 1) {
                hideSuggestions();
                return;
            }

            List<AutoCompleteEngine.CompletionItem> results =
                    acEngine.getSuggestions(prefix, MAX_SUGGESTIONS);

            if (results.isEmpty()) {
                hideSuggestions();
            } else {
                showSuggestionList(results);
            }

        } catch (Exception e) {
            Log.e("AC", "updateSuggestions xətası: " + e.getMessage());
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
        while (start >= 0 &&
                (Character.isLetterOrDigit(text.charAt(start)) || text.charAt(start) == '_')) {
            start--;
        }
        start = Math.max(0, start + 1);
        return text.substring(start, cursor);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Event Listeners - ƏSAS DÜZƏLİŞ
    // ═════════════════════════════════════════════════════════════════════
    private void setupEventListeners() {
        // Geri düyməsi
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> {
                saveCode();
                onBackPressed();
            });
        }

        // Menyu düyməsi - PopupMenu
        if (menuBtn != null) {
            menuBtn.setOnClickListener(v -> showPopupMenu(v));
        }

        // Run düyməsi
        if (runBtn != null) {
            runBtn.setOnClickListener(v -> {
                showOutputPanel();
                runPythonCode();
            });
        }

        // Bilmirəm düyməsi
        if (dontKnowBtn != null) {
            dontKnowBtn.setOnClickListener(v -> showSolutionInCode());
        }

        // Konsol təmizləmə
        if (clearOutputBtn != null) {
            clearOutputBtn.setOnClickListener(v -> {
                outputText.setText("> Konsol təmizləndi");
                Toast.makeText(this, "Konsol təmizləndi", Toast.LENGTH_SHORT).show();
            });
        }

        // Konsol kopyalama
        if (copyOutputBtn != null) {
            copyOutputBtn.setOnClickListener(v -> {
                String output = outputText.getText().toString();
                if (!output.isEmpty()) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Output", output);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "✅ Konsol mətni kopyalandı", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Konsol gizlətmə
        if (hideOutputBtn != null) {
            hideOutputBtn.setOnClickListener(v -> hideOutputPanel());
        }

        // Undo/Redo
        if (undoBtn != null) {
            undoBtn.setOnClickListener(v -> undo());
        }
        if (redoBtn != null) {
            redoBtn.setOnClickListener(v -> redo());
        }

        // TextWatcher - kod dəyişiklikləri
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
                updateUndoRedoButtons();
            }
        });

        // Fokus itdikdə suggestion-ları gizlət
        codeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                new Handler().postDelayed(this::hideSuggestions, 200);
            }
        });

        // Auto-indent üçün
        setupAutoIndent();
    }

    // ═════════════════════════════════════════════════════════════════════
    // PopupMenu
    // ═════════════════════════════════════════════════════════════════════
    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.editor_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_save_project) {
                showSaveProjectDialog();
                return true;
            } else if (id == R.id.menu_load_project) {
                showLoadProjectDialog();
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
            return false;
        });

        popupMenu.show();
    }

    // ═════════════════════════════════════════════════════════════════════
    // Save/Load Project
    // ═════════════════════════════════════════════════════════════════════
    private void showSaveProjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("💾 Layihəni Saxla");

        final EditText input = new EditText(this);
        input.setHint("Layihə adı daxil edin...");
        String defaultName = "task_" + currentTaskId + "_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        input.setText(defaultName);

        builder.setView(input);
        builder.setPositiveButton("Saxla", (dialog, which) -> {
            String projectName = input.getText().toString().trim();
            if (projectName.isEmpty()) {
                Toast.makeText(this, "Layihə adı boş ola bilməz!", Toast.LENGTH_SHORT).show();
                return;
            }
            saveProject(projectName);
        });
        builder.setNegativeButton("Ləğv et", null);
        builder.show();
    }

    private void saveProject(String projectName) {
        try {
            File projectsDir = new File(getFilesDir(), PROJECTS_DIR);
            if (!projectsDir.exists()) projectsDir.mkdirs();

            String safeFileName = projectName.replaceAll("[^a-zA-Z0-9_.-]", "_");
            File projectFile = new File(projectsDir, safeFileName + ".pyproj");

            ProjectData projectData = new ProjectData();
            projectData.name = projectName;
            projectData.code = codeInput.getText().toString();
            projectData.taskId = currentTaskId;
            projectData.timestamp = System.currentTimeMillis();
            projectData.description = taskDescription.getText().toString();

            Gson gson = new Gson();
            String jsonData = gson.toJson(projectData);

            FileOutputStream fos = new FileOutputStream(projectFile);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(jsonData);
            writer.close();
            fos.close();

            Toast.makeText(this, "✅ Layihə saxlandı: " + projectName, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "❌ Saxlama xətası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoadProjectDialog() {
        try {
            File projectsDir = new File(getFilesDir(), PROJECTS_DIR);
            if (!projectsDir.exists()) {
                Toast.makeText(this, "Heç bir layihə tapılmadı!", Toast.LENGTH_SHORT).show();
                return;
            }

            File[] projectFiles = projectsDir.listFiles((dir, name) -> name.endsWith(".pyproj"));
            if (projectFiles == null || projectFiles.length == 0) {
                Toast.makeText(this, "Heç bir layihə tapılmadı!", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> projectNames = new ArrayList<>();
            List<File> files = new ArrayList<>();

            for (File file : projectFiles) {
                try {
                    FileInputStream fis = new FileInputStream(file);
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
                    if (data != null && data.name != null) {
                        projectNames.add(data.name + " (Task " + data.taskId + ")");
                        files.add(file);
                    }
                } catch (Exception e) {
                    projectNames.add(file.getName().replace(".pyproj", ""));
                    files.add(file);
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("📂 Layihəni Yüklə");
            builder.setItems(projectNames.toArray(new String[0]), (dialog, which) -> loadProject(files.get(which)));
            builder.setNegativeButton("Ləğv et", null);
            builder.show();

        } catch (Exception e) {
            Toast.makeText(this, "Xəta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                clearRedoStack();
                codeInput.setText(data.code);
                updateLineNumbers(data.code);
                if (syntaxHighlightingEnabled) applySyntaxHighlighting(codeInput.getText());

                if (data.taskId != currentTaskId) {
                    Toast.makeText(this, "⚠️ Bu layihə fərqli tapşırıq üçün yaradılıb (Task " + data.taskId + ")", Toast.LENGTH_LONG).show();
                }
                Toast.makeText(this, "✅ Layihə yükləndi: " + data.name, Toast.LENGTH_LONG).show();
                saveCode();
                updateUndoRedoButtons();
            }
        } catch (Exception e) {
            Toast.makeText(this, "❌ Yükləmə xətası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Clear Code, Copy, Paste, Syntax Check
    // ═════════════════════════════════════════════════════════════════════
    private void clearCurrentCode() {
        new AlertDialog.Builder(this)
                .setTitle("🗑️ Kodu Təmizlə")
                .setMessage("Bütün kodu silmək istədiyinizə əminsiniz?")
                .setPositiveButton("Bəli", (d, w) -> {
                    saveToUndoStack();
                    clearRedoStack();
                    codeInput.setText("");
                    updateLineNumbers("");
                    outputText.setText("> Kod təmizləndi");
                    Toast.makeText(this, "Kod təmizləndi", Toast.LENGTH_SHORT).show();
                    updateUndoRedoButtons();
                })
                .setNegativeButton("Xeyr", null)
                .show();
    }

    private void copyAllCode() {
        String code = codeInput.getText().toString();
        if (code.isEmpty()) {
            Toast.makeText(this, "Kopyalanacaq kod yoxdur!", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Python Code", code);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "✅ Kod kopyalandı (" + code.length() + " simvol)", Toast.LENGTH_SHORT).show();
    }

    private void pasteCode() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip()) {
            android.content.ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            String pastedText = item.getText().toString();
            if (pastedText != null && !pastedText.isEmpty()) {
                int cursorPosition = codeInput.getSelectionStart();
                Editable editable = codeInput.getText();
                editable.insert(cursorPosition, pastedText);
                Toast.makeText(this, "📄 Kod yapışdırıldı", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Yapışdırılacaq mətn tapılmadı!", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkSyntax() {
        String code = codeInput.getText().toString();
        if (code.trim().isEmpty()) {
            Toast.makeText(this, "Kod yazın!", Toast.LENGTH_SHORT).show();
            return;
        }
        List<CompilerError> errors = checkSyntaxErrors(code);
        if (errors.isEmpty()) {
            outputText.setText("✅ Sintaksis yoxlaması keçdi!\nHeç bir xəta tapılmadı.");
            Toast.makeText(this, "✅ Sintaksis düzgündür!", Toast.LENGTH_SHORT).show();
        } else {
            StringBuilder errorMsg = new StringBuilder("❌ SİNTAKSİS XƏTALARI:\n\n");
            for (CompilerError e : errors) {
                errorMsg.append("📌 Sətir ").append(e.lineNumber).append(": ")
                        .append(e.errorType).append("\n   ").append(e.message).append("\n\n");
            }
            outputText.setText(errorMsg.toString());
            Toast.makeText(this, "Xəta aşkarlandı! Konsolu yoxlayın", Toast.LENGTH_LONG).show();
        }
        showOutputPanel();
    }

    // ═════════════════════════════════════════════════════════════════════
    // Settings & About
    // ═════════════════════════════════════════════════════════════════════
    private void showSettings() {
        new AlertDialog.Builder(this)
                .setTitle("⚙️ Parametrlər")
                .setItems(new String[]{
                        "🎨 Syntax Highlighting: " + (syntaxHighlightingEnabled ? "Aktiv" : "Deaktiv"),
                        "📝 Auto-indent: " + (autoIndentEnabled ? "Aktiv" : "Deaktiv"),
                        "💡 Auto-complete: " + (autoCompleteEnabled ? "Aktiv" : "Deaktiv"),
                        "🗑️ Bütün layihələri sil"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            syntaxHighlightingEnabled = !syntaxHighlightingEnabled;
                            if (syntaxHighlightingEnabled) {
                                applySyntaxHighlighting(codeInput.getText());
                            } else {
                                Editable ed = codeInput.getText();
                                for (ForegroundColorSpan s : ed.getSpans(0, ed.length(), ForegroundColorSpan.class))
                                    ed.removeSpan(s);
                            }
                            Toast.makeText(this, "Syntax highlighting: " + (syntaxHighlightingEnabled ? "Aktiv" : "Deaktiv"), Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            autoIndentEnabled = !autoIndentEnabled;
                            Toast.makeText(this, "Auto-indent: " + (autoIndentEnabled ? "Aktiv" : "Deaktiv"), Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            autoCompleteEnabled = !autoCompleteEnabled;
                            if (!autoCompleteEnabled) hideSuggestions();
                            Toast.makeText(this, "Auto-complete: " + (autoCompleteEnabled ? "Aktiv" : "Deaktiv"), Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            deleteAllProjects();
                            break;
                    }
                })
                .setNegativeButton("Bağla", null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("ℹ️ Python Editor")
                .setMessage("📱 Versiya: 1.0.0\n\n" +
                        "🐍 Python Editor - Professional Python IDE\n\n" +
                        "✨ Xüsusiyyətlər:\n" +
                        "• Sintaksis vurğulama\n" +
                        "• Auto-complete (IntelliSense)\n" +
                        "• Layihə idarəetməsi\n" +
                        "• Real-time test icrası\n" +
                        "• Undo/Redo dəstəyi\n\n" +
                        "© 2024 Ali PyMain\n" +
                        "Bütün hüquqlar qorunur.")
                .setPositiveButton("Bağla", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void deleteAllProjects() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Bütün Layihələri Sil")
                .setMessage("Bütün saxlanmış layihələr silinəcək. Bu əməliyyat geri alına bilməz!\n\nDavam etmək istəyirsiniz?")
                .setPositiveButton("Bəli, Sil", (d, w) -> {
                    File projectsDir = new File(getFilesDir(), PROJECTS_DIR);
                    if (projectsDir.exists()) {
                        File[] files = projectsDir.listFiles();
                        if (files != null) {
                            for (File file : files) file.delete();
                        }
                        projectsDir.delete();
                    }
                    Toast.makeText(this, "✅ Bütün layihələr silindi!", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Xeyr", null)
                .show();
    }

    // ═════════════════════════════════════════════════════════════════════
    // Undo / Redo - GERİ QAYTARMA
    // ═════════════════════════════════════════════════════════════════════
    private void saveToUndoStack() {
        String code = codeInput.getText().toString();
        if (undoStack.isEmpty() || !undoStack.peek().equals(code)) {
            undoStack.push(code);
            if (undoStack.size() > 50) undoStack.removeElementAt(0);
        }
        updateUndoRedoButtons();
    }

    private void clearRedoStack() {
        redoStack.clear();
        updateUndoRedoButtons();
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
            updateUndoRedoButtons();
            Toast.makeText(this, "↩ Geri qaytarıldı", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Geri qaytarmaq mümkün deyil", Toast.LENGTH_SHORT).show();
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
            updateUndoRedoButtons();
            Toast.makeText(this, "↪ İrəli qaytarıldı", Toast.LENGTH_SHORT).show();
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

    // ═════════════════════════════════════════════════════════════════════
    // Auto-indent
    // ═════════════════════════════════════════════════════════════════════
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

        String prevLine = fullText.substring(lineStart,
                Math.min(newlinePos, fullText.length())).trim();

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

    // ═════════════════════════════════════════════════════════════════════
    // Syntax Highlighting
    // ═════════════════════════════════════════════════════════════════════
    private static final String[] KEYWORDS = {
            "def","class","if","else","elif","while","for","in","return","import",
            "from","as","try","except","finally","with","lambda","and","or","not",
            "is","None","True","False","pass","break","continue","global","nonlocal",
            "assert","raise","yield","async","await","del","print"
    };
    private static final String[] BUILTINS = {
            "abs","all","any","bin","bool","chr","dict","dir","enumerate","filter",
            "float","int","len","list","map","max","min","ord","range","reversed",
            "round","set","sorted","str","sum","tuple","type","zip","open","input",
            "format","help","id","isinstance","issubclass","super","repr","hash","vars"
    };
    private static final String[] MODULES = {
            "math","random","datetime","json","re","os","sys","time","collections",
            "itertools","functools","numpy","pandas","torch","np","pd","plt","copy","string"
    };

    private void setupSyntaxHighlighting() {}

    private void applySyntaxHighlighting(Editable ed) {
        if (!syntaxHighlightingEnabled || isApplyingHighlight || ed == null) return;
        isApplyingHighlight = true;
        try {
            for (ForegroundColorSpan s : ed.getSpans(0, ed.length(), ForegroundColorSpan.class))
                ed.removeSpan(s);

            if (ed.length() == 0) return;

            ed.setSpan(new ForegroundColorSpan(Color.WHITE), 0, ed.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

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
            for (String mo : MODULES) hlPattern(ed, "\\b" + mo + "\\b", C_MODULE);

        } finally {
            isApplyingHighlight = false;
        }
    }

    private void hlPattern(Editable ed, String pattern, int color) {
        try {
            Matcher m = Pattern.compile(pattern).matcher(ed);
            while (m.find())
                ed.setSpan(new ForegroundColorSpan(color), m.start(), m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } catch (Exception e) { /* ignore */ }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Line Numbers
    // ═════════════════════════════════════════════════════════════════════
    private void setupLineNumbers() {
        codeInput.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE)
                lineNumbers.scrollTo(0, codeInput.getScrollY());
            return false;
        });
        updateLineNumbers(codeInput.getText().toString());
    }

    private void updateLineNumbers(String text) {
        int count = text.split("\n", -1).length;
        if (text.length() > 0 && text.charAt(text.length() - 1) == '\n') count++;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= count; i++) sb.append(i).append("\n");
        lineNumbers.setText(sb.toString());
    }

    // ═════════════════════════════════════════════════════════════════════
    // Output Panel
    // ═════════════════════════════════════════════════════════════════════
    private void showOutputPanel() {
        if (outputContainer != null && outputContainer.getVisibility() != View.VISIBLE) {
            outputContainer.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams ep = (LinearLayout.LayoutParams) codeEditorLayout.getLayoutParams();
            LinearLayout.LayoutParams op = (LinearLayout.LayoutParams) outputContainer.getLayoutParams();
            ep.weight = 1f;
            op.weight = 1f;
            codeEditorLayout.requestLayout();
            outputContainer.requestLayout();
        }
    }

    private void hideOutputPanel() {
        if (outputContainer != null) outputContainer.setVisibility(View.GONE);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Python Code Execution (qısaldılmış - əvvəlki kodunuzda olduğu kimi)
    // ═════════════════════════════════════════════════════════════════════
    private void runPythonCode() {
        String code = codeInput.getText().toString();
        if (code.trim().isEmpty()) {
            showResult("> Python kodu yazın", true);
            return;
        }

        try {
            py.getModule("warnings").callAttr("filterwarnings", "ignore");
        } catch (Exception e) {
            // ignore
        }

        StringBuilder out = new StringBuilder();
        List<CompilerError> errs = checkSyntaxErrors(code);

        if (!errs.isEmpty()) {
            out.append("❌ XƏTALAR:\n══════════════════\n\n");
            for (CompilerError e : errs)
                out.append("📌 Sətir ").append(e.lineNumber).append(": ")
                        .append(e.errorType).append("\n   ").append(e.message).append("\n\n");
            out.append("══════════════════\n\n");
        }

        if (tests != null && !tests.isEmpty()) runPythonTests(code, out);
        else runBasicAnalysis(code, out);

        executePythonCode(code, out);
    }

    private List<CompilerError> checkSyntaxErrors(String code) {
        List<CompilerError> errors = new ArrayList<>();
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int ln = i + 1;
            if (countOcc(line, "(") != countOcc(line, ")"))
                errors.add(new CompilerError("Mötərizə balanssızlığı", ln, "SintaksisXətası"));
            if (countOcc(line, "[") != countOcc(line, "]"))
                errors.add(new CompilerError("Kvadrat mötərizə balanssızlığı", ln, "SintaksisXətası"));
            if (countOcc(line, "{") != countOcc(line, "}"))
                errors.add(new CompilerError("Fiqurlu mötərizə balanssızlığı", ln, "SintaksisXətası"));
        }
        return errors;
    }

    private void runPythonTests(String code, StringBuilder out) {
        out.append("🧪 TEST NƏTİCƏLƏRİ:\n\n");
        int passed = 0;
        boolean allOk = true;

        for (int i = 0; i < tests.size(); i++) {
            AiTaskModel.Test t = tests.get(i);
            TestResult r = runSingleTest(code, t);
            if (r.passed) {
                passed++;
                out.append("✅ TEST ").append(i + 1).append(": ").append(t.getDescription()).append("\n");
                out.append("   Giriş: ").append(t.getInput()).append("\n");
                out.append("   Gözlənilən: ").append(t.getExpected()).append("\n");
                out.append("   Alınan: ").append(r.actual).append("\n");
                out.append("   Status: UĞURLU\n\n");
            } else {
                allOk = false;
                out.append("❌ TEST ").append(i + 1).append(": ").append(t.getDescription()).append("\n");
                out.append("   Giriş: ").append(t.getInput()).append("\n");
                out.append("   Gözlənilən: ").append(t.getExpected()).append("\n");
                out.append("   Alınan: ").append(r.actual).append("\n");
                if (!r.error.isEmpty()) out.append("   Xəta: ").append(r.error).append("\n");
                out.append("   Status: UĞURSUZ\n\n");
            }
        }

        out.append("📊 ").append(passed).append("/").append(tests.size());
        if (allOk && tests.size() > 0) {
            out.append("\n🎉 TƏBRİKLƏR! Bütün testlər keçdi!\n");
            markTaskAsCompleted();
        } else {
            out.append("\n⚠ Bəzi testlər uğursuz oldu.\n");
        }
        out.append("════════════════════\n");
        outputText.setText(out.toString());
    }

    private TestResult runSingleTest(String code, AiTaskModel.Test test) {
        try {
            String full = code + "\n\ntry:\n"
                    + "    actual_result = " + test.getInput() + "\n"
                    + "    print('ACTUAL:' + repr(actual_result))\n"
                    + "    expected = " + test.getExpected() + "\n"
                    + "    print('TEST_PASSED' if actual_result == expected else 'TEST_FAILED')\n"
                    + "except Exception as e:\n"
                    + "    print('TEST_ERROR:' + repr(e))";

            String output = py.getModule("executor").callAttr("run_code", full, "").toString();
            String actual = "";
            boolean passed = false;
            String error = "";

            for (String line : output.split("\n")) {
                if (line.startsWith("ACTUAL:")) actual = line.substring(7).trim();
                else if (line.startsWith("TEST_PASSED")) passed = true;
                else if (line.startsWith("TEST_ERROR:")) error = line.substring(11).trim();
            }
            return new TestResult(passed, actual, error);
        } catch (Exception e) {
            return new TestResult(false, "", "Xəta: " + e.getMessage());
        }
    }

    private void runBasicAnalysis(String code, StringBuilder out) {
        out.append("📊 KOD ANALİZİ:\n\n");
        Matcher fm = Pattern.compile("def\\s+(\\w+)").matcher(code);
        List<String> funcs = new ArrayList<>();
        while (fm.find()) funcs.add(fm.group(1));
        out.append("✓ Funksiyalar: ").append(funcs.isEmpty() ? "yoxdur" : String.join(", ", funcs)).append("\n\n");
    }

    private void executePythonCode(String code, StringBuilder out) {
        out.append("🚀 PYTHON ÇIXIŞI:\n══════════════════\n");
        try {
            String result = py.getModule("executor").callAttr("run_code", code, "").toString();
            out.append(result.isEmpty() ? "> Kod uğurla icra olundu" : result).append("\n");
        } catch (Exception e) {
            out.append("❌ Xəta: ").append(e.getMessage()).append("\n");
        }
        out.append("══════════════════\n");
        outputText.setText(out.toString());
    }

    private void showSolutionInCode() {
        if (solution == null || solution.isEmpty()) {
            Toast.makeText(this, "Həll kodu mövcud deyil!", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Həll kodunu göstər")
                .setMessage("Cari kodun üzərinə yazılacaq. Davam etmək istəyirsiniz?")
                .setPositiveButton("Bəli", (d, w) -> {
                    saveToUndoStack();
                    clearRedoStack();
                    codeInput.setText(solution);
                    updateLineNumbers(solution);
                    if (syntaxHighlightingEnabled) applySyntaxHighlighting(codeInput.getText());
                    saveCode();
                    showOutputPanel();
                    outputText.setText("> Həll kodu əlavə edildi.");
                    Toast.makeText(this, "Həll kodu əlavə edildi!", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Xeyr", null).show();
    }

    private void markTaskAsCompleted() {
        try {
            String taskType = getIntent().getStringExtra("TASK_TYPE");
            SharedPreferences prefs;
            String key;

            if ("ai".equals(taskType)) {
                prefs = getSharedPreferences("AiTaskProgress", MODE_PRIVATE);
                key = "ai_task_" + currentTaskId + "_completed";
            } else {
                prefs = getSharedPreferences("TaskProgress", MODE_PRIVATE);
                key = "task_" + currentTaskId + "_completed";
            }
            prefs.edit().putBoolean(key, true).apply();

            runOnUiThread(() -> {
                Toast.makeText(this, "✅ Task " + currentTaskId + " tamamlandı!", Toast.LENGTH_LONG).show();
                Intent ri = new Intent();
                ri.putExtra("TASK_ID", currentTaskId);
                ri.putExtra("COMPLETED", true);
                ri.putExtra("TASK_TYPE", taskType);
                setResult(RESULT_OK, ri);
                finish();
            });
        } catch (Exception e) {
            Log.e("PythonConsole", "markTaskAsCompleted xətası: " + e.getMessage());
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

    private void saveCode() {
        if (preferences != null) {
            preferences.edit()
                    .putString("task_" + currentTaskId + "_code", codeInput.getText().toString())
                    .apply();
        }
    }

    private void loadSavedCode() {
        String saved = preferences.getString("task_" + currentTaskId + "_code", "");
        if (saved.isEmpty() && initialCode != null) saved = initialCode;
        if (!saved.isEmpty()) {
            codeInput.setText(saved);
            updateLineNumbers(saved);
            if (syntaxHighlightingEnabled) applySyntaxHighlighting(codeInput.getText());
        }
    }

    private void showResult(String text, boolean isError) {
        runOnUiThread(() -> {
            outputText.setTextColor(isError ? 0xFFFF4444 : 0xFF4CAF50);
            outputText.setText(text);
        });
    }

    private void setupTaskData() {
        Intent intent = getIntent();
        if (intent == null) return;

        currentTaskId = intent.getIntExtra("TASK_ID", 1);
        String title = intent.getStringExtra("TASK_TITLE");
        String description = intent.getStringExtra("TASK_DESCRIPTION");
        initialCode = intent.getStringExtra("INITIAL_CODE");
        solution = intent.getStringExtra("TASK_SOLUTION");
        String testsJson = intent.getStringExtra("TASK_TESTS");

        if (title != null && description != null)
            taskDescription.setText("Tapşırıq " + currentTaskId + ": " + title + "\n" + description);
        else if (description != null)
            taskDescription.setText(description);
        else
            taskDescription.setVisibility(View.GONE);

        if (testsJson != null && !testsJson.isEmpty()) {
            Type type = new TypeToken<List<AiTaskModel.Test>>() {
            }.getType();
            tests = new Gson().fromJson(testsJson, type);
        }
    }

    private void setupFirebase() {
        fAuth = FirebaseAuth.getInstance();
    }

    private void setupPython() {
        if (!Python.isStarted()) Python.start(new AndroidPlatform(this));
        py = Python.getInstance();
    }

    private void setupPreferences() {
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        loadSavedCode();
        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean(PREF_USER_LOGGED_IN, fAuth.getCurrentUser() != null).apply();
    }

    private void setupAdMob() {
        mAdPythonCode = findViewById(R.id.adPythoneCode);
        if (mAdPythonCode != null) {
            mAdPythonCode.loadAd(new AdRequest.Builder().build());
        }
    }

    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, backCallback);
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

    @Override
    public void onBackPressed() {
        saveCode();
        super.onBackPressed();
    }

    OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            new MaterialAlertDialogBuilder(PythonConsole.this)
                    .setTitle(R.string.app_name)
                    .setMessage("Çıxmaq istəyirsiniz?")
                    .setNegativeButton(android.R.string.no, (d, i) -> d.dismiss())
                    .setPositiveButton(android.R.string.yes, (d, i) -> {
                        saveCode();
                        if (fAuth.getCurrentUser() != null) finish();
                    })
                    .show();
        }
    };
}