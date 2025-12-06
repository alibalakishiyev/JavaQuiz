package com.ali.javaquizbyali.codemodel;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.ali.systemIn.R;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeActivity extends AppCompatActivity {

    private EditText codeInput;
    private AdView mAdJavaCode;

    private TextView lineNumbers;
    private TextView outputText;
    private TextView taskDescription;
    private Button backBtn, clearBtn, runBtn, clearOutputBtn;
    private ListView suggestionsList;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "JavaCodePrefs";
    private static final String TASK_PREFS = "TaskProgress";

    private boolean isApplyingHighlighting = false;
    private int currentTaskId;
    private List<TaskModel.Test> tests;
    private String solution;
    private String initialCode;

    // Java anahtar kelimeleri ve renkleri
    private final String[] JAVA_KEYWORDS = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null"
    };

    // Java otomatik tamamlama önerileri
    private final String[] JAVA_SUGGESTIONS = {
            "public class ", "public static void main", "System.out.println", "for (int i = 0; i < ; i++)",
            "if () {\n    \n}", "while () {\n    \n}", "try {\n    \n} catch () {\n    \n}",
            "Scanner scanner = new Scanner(System.in);", "ArrayList<> list = new ArrayList<>();",
            "HashMap<> map = new HashMap<>();", "public void () {\n    \n}", "public int () {\n    \n}",
            "public String () {\n    \n}", "private ", "protected ", "static "
    };

    // Değişken türlerine göre methodlar
    private final Map<String, String[]> TYPE_METHODS = new HashMap<String, String[]>() {{
        put("String", new String[]{
                "length()", "charAt(int)", "substring(int)", "substring(int, int)",
                "toLowerCase()", "toUpperCase()", "trim()", "replace(char, char)",
                "replace(String, String)", "split(String)", "indexOf(String)",
                "lastIndexOf(String)", "contains(String)", "startsWith(String)",
                "endsWith(String)", "equals(String)", "equalsIgnoreCase(String)",
                "compareTo(String)", "isEmpty()", "concat(String)"
        });
        put("Scanner", new String[]{
                "next()", "nextLine()", "nextInt()", "nextDouble()", "nextFloat()",
                "nextLong()", "nextShort()", "nextByte()", "nextBoolean()", "hasNext()",
                "hasNextInt()", "hasNextDouble()", "close()"
        });
        put("ArrayList", new String[]{
                "add(Object)", "add(int, Object)", "get(int)", "set(int, Object)",
                "remove(int)", "remove(Object)", "size()", "clear()", "isEmpty()",
                "contains(Object)", "indexOf(Object)", "toArray()", "sort(Comparator)"
        });
        put("int", new String[]{
                "parseInt(String)", "toString()", "valueOf(String)", "compare(int, int)",
                "max(int, int)", "min(int, int)", "sum(int, int)", "toBinaryString(int)"
        });
        put("Math", new String[]{
                "abs(double)", "sqrt(double)", "pow(double, double)", "max(int, int)",
                "min(int, int)", "random()", "round(double)", "ceil(double)", "floor(double)",
                "sin(double)", "cos(double)", "tan(double)", "log(double)", "exp(double)"
        });
    }};

    // Java compiler emülasyonu için hata tespiti
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
        setContentView(R.layout.activity_code);

        initializeViews();
        setupSharedPreferences();
        setupTaskData();
        setupEventListeners();
        loadSavedCode();
        setupAdMob();
    }

    private void initializeViews() {
        codeInput = findViewById(R.id.codeInput);
        lineNumbers = findViewById(R.id.lineNumbers);
        outputText = findViewById(R.id.outputText);
        taskDescription = findViewById(R.id.taskDescription);
        backBtn = findViewById(R.id.backBtn);
        clearBtn = findViewById(R.id.clearBtn);
        runBtn = findViewById(R.id.runBtn);
        clearOutputBtn = findViewById(R.id.clearOutputBtn);
        suggestionsList = findViewById(R.id.suggestionsList);

        // Otomatik tamamlama listesini ayarla
        ArrayAdapter<String> suggestionsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                JAVA_SUGGESTIONS
        );
        suggestionsList.setAdapter(suggestionsAdapter);
    }

    private void setupAdMob() {
        mAdJavaCode = findViewById(R.id.adJavaCode);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdJavaCode.loadAd(adRequest);
    }

    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }

    private void setupTaskData() {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentTaskId = extras.getInt("TASK_ID", 1);
            String title = extras.getString("TASK_TITLE", "");
            String description = extras.getString("TASK_DESCRIPTION", "");
            initialCode = extras.getString("INITIAL_CODE", "");
            String testsJson = extras.getString("TASK_TESTS", "");
            solution = extras.getString("TASK_SOLUTION", "");

            // Set task description
            taskDescription.setText("Tapşırıq " + currentTaskId + ": " + title + "\n" + description);

            // Parse tests from JSON
            if (testsJson != null && !testsJson.isEmpty()) {
                Gson gson = new Gson();
                Type testListType = new TypeToken<List<TaskModel.Test>>() {}.getType();
                tests = gson.fromJson(testsJson, testListType);
            }

            // Set initial code if no saved code exists
            String savedCode = sharedPreferences.getString("task_" + currentTaskId + "_code", initialCode);
            if (savedCode.equals(initialCode)) {
                codeInput.setText(initialCode);
            }
        }
    }

    private void setupEventListeners() {
        // Kod değişiklik dinleyicisi
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
                updateLineNumbers();

                // Otomatik tamamlama göster/gizle
                if (s.length() > 0) {
                    // Nöqtə daxil etdikdə smart completion göstər
                    if (count == 1 && s.charAt(start) == '.') {
                        showSmartCompletion(s.toString(), start);
                    } else {
                        showSuggestions();
                    }
                } else {
                    hideSuggestions();
                }

                // Avtomatik indentasiya əlavə et
                if (!isDeleting && before == 0 && count == 1) {
                    char lastChar = s.charAt(start);
                    if (lastChar == '\n') {
                        autoIndent(start + 1, s.toString(), start);
                    } else if (lastChar == '{') {
                        autoAddClosingBrace(start + 1);
                    } else if (lastChar == '(') {
                        autoAddClosingParenthesis(start + 1);
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

        // Geri butonu
        backBtn.setOnClickListener(v -> {
            saveCodeToStorage();
            finish();
        });

        // Temizle butonu
        clearBtn.setOnClickListener(v -> {
            codeInput.setText(initialCode != null ? initialCode : "");
            outputText.setText("> ...");
            hideSuggestions();
        });

        // Çalıştır butonu
        runBtn.setOnClickListener(v -> runJavaCode());

        // Çıktı temizle butonu
        clearOutputBtn.setOnClickListener(v -> outputText.setText("> ..."));

        // Otomatik tamamlama tıklama olayı
        suggestionsList.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSuggestion = (String) suggestionsList.getAdapter().getItem(position);
            insertSuggestion(selectedSuggestion);
            hideSuggestions();
        });
    }

    private void autoIndent(int position, String fullText, int newlinePosition) {
        Editable editable = codeInput.getText();
        if (position > 1) {
            // Cari sətri al (Enter basılan sətir)
            int currentLineStart = fullText.lastIndexOf('\n', newlinePosition - 1);
            if (currentLineStart == -1) currentLineStart = 0;
            else currentLineStart++; // \n-dən sonra başlasın

            String currentLine = fullText.substring(currentLineStart, Math.min(newlinePosition, fullText.length()));
            String currentLineTrimmed = currentLine.trim();

            // Əvvəlki sətirləri yoxla
            String previousText = fullText.substring(0, Math.min(newlinePosition, fullText.length()));
            String[] allLines = previousText.split("\n");

            int indentCount = 0;
            boolean inBlockComment = false;

            for (String line : allLines) {
                String trimmedLine = line.trim();

                // Çoxsətirli yorumları idarə et
                if (trimmedLine.contains("/*") && !trimmedLine.contains("*/")) {
                    inBlockComment = true;
                }
                if (trimmedLine.contains("*/")) {
                    inBlockComment = false;
                }

                if (!inBlockComment && !trimmedLine.startsWith("//")) {
                    // { və } sayma
                    for (int i = 0; i < line.length(); i++) {
                        char c = line.charAt(i);
                        if (c == '{') {
                            indentCount++;
                        }
                        if (c == '}') {
                            // Sətrin əvvəlində } varsa, bloku bağlayır
                            boolean isStartOfLine = true;
                            for (int j = 0; j < i; j++) {
                                if (!Character.isWhitespace(line.charAt(j))) {
                                    isStartOfLine = false;
                                    break;
                                }
                            }
                            if (isStartOfLine) {
                                indentCount--;
                            }
                        }
                    }
                }
            }

            // Cari sətirdə } varsa və { yoxdursa, bir azalt
            if (currentLineTrimmed.equals("}") ||
                    (currentLineTrimmed.endsWith("}") && !currentLineTrimmed.contains("{"))) {
                indentCount = Math.max(0, indentCount - 1);
            }

            // Cari sətirdə { varsa, indi onu da saydıq, ona görə bir əlavə et
            if (currentLineTrimmed.endsWith("{") && !currentLineTrimmed.equals("{")) {
                indentCount++;
            }

            // İndentasiya yarat
            StringBuilder indent = new StringBuilder();
            for (int i = 0; i < indentCount; i++) {
                indent.append("    ");
            }

            // Yalnız əgər mövcud indent yoxdursa, əlavə et
            String existingText = editable.toString();
            if (position < existingText.length()) {
                // Yeni sətirdə artıq indent var?
                int spaces = 0;
                while (position + spaces < existingText.length() &&
                        existingText.charAt(position + spaces) == ' ') {
                    spaces++;
                }

                // Əgər artıq indent yoxdursa, əlavə et
                if (spaces < indent.length()) {
                    // Artıq olan boşluqları sil
                    if (spaces > 0) {
                        editable.delete(position, position + spaces);
                    }
                    editable.insert(position, indent.toString());
                }
            } else {
                // Sətrin sonundadırsa, sadəcə əlavə et
                editable.insert(position, indent.toString());
            }
        }
    }

    private void autoAddClosingBrace(int position) {
        Editable editable = codeInput.getText();
        String text = editable.toString();

        // Sətirin sonuna yoxsa if, for, while, class daxilində yoxlayırıq
        if (position < text.length() && text.charAt(position) != '\n') {
            return;
        }

        // Avtomatik bağlanan mötərizə əlavə et
        new android.os.Handler().postDelayed(() -> {
            int currentPos = codeInput.getSelectionStart();

            // Cari sətirdə neçə indent olduğunu hesabla
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

            // Bağlanan } üçün bir azalt
            indentLevel = Math.max(0, indentLevel);

            // Yeni indent yarat
            StringBuilder closingIndent = new StringBuilder();
            for (int i = 0; i < indentLevel; i++) {
                closingIndent.append("    ");
            }

            String newText = "\n" + closingIndent.toString() + "}";
            editable.insert(currentPos, newText);

            // İmleci } ilə eyni səviyyəyə qoy
            codeInput.setSelection(currentPos + 1 + closingIndent.length());
        }, 50);
    }

    private void autoAddClosingParenthesis(int position) {
        Editable editable = codeInput.getText();
        String text = editable.toString();

        // Mötərizə bağlamaq üçün yoxlayırıq
        if (position < text.length() && text.charAt(position) != '\n' && text.charAt(position) != ')') {
            new android.os.Handler().postDelayed(() -> {
                int currentPos = codeInput.getSelectionStart();
                editable.insert(currentPos, ")");
                codeInput.setSelection(currentPos);
            }, 50);
        }
    }

    private void showSmartCompletion(String text, int position) {
        // Nöqtədən əvvəlki dəyişəni tap
        int dotPos = position;
        int varStart = dotPos - 1;

        while (varStart >= 0 && Character.isLetterOrDigit(text.charAt(varStart))) {
            varStart--;
        }
        varStart++; // Hərf başlanğıcına qaytar

        if (varStart < dotPos) {
            String varName = text.substring(varStart, dotPos);
            String varType = detectVariableType(text, varName);

            if (varType != null && TYPE_METHODS.containsKey(varType)) {
                String[] methods = TYPE_METHODS.get(varType);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_list_item_1,
                        methods
                );
                suggestionsList.setAdapter(adapter);
                suggestionsList.setVisibility(View.VISIBLE);
                return;
            }
        }

        // Standart suggestionları göstər
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                JAVA_SUGGESTIONS
        );
        suggestionsList.setAdapter(adapter);
        suggestionsList.setVisibility(View.VISIBLE);
    }

    private String detectVariableType(String code, String varName) {
        // Dəyişən tipini təyin etməyə çalış
        String[] lines = code.split("\n");

        for (String line : lines) {
            line = line.trim();

            // Dəyişən bəyannaməsi axtar
            for (String type : TYPE_METHODS.keySet()) {
                String pattern = "\\b" + type + "\\s+" + varName + "\\s*[=;]";
                if (Pattern.compile(pattern).matcher(line).find()) {
                    return type;
                }
            }

            // String literal axtar
            if (line.contains(varName + " = \"") || line.contains(varName + "= \"")) {
                return "String";
            }

            // Scanner axtar
            if (line.contains("new Scanner(") && line.contains(varName)) {
                return "Scanner";
            }

            // ArrayList axtar
            if (line.contains("new ArrayList") && line.contains(varName)) {
                return "ArrayList";
            }

            // Math axtar
            if (line.contains("Math.") && line.contains(varName)) {
                return "Math";
            }
        }

        return null;
    }

    private void updateLineNumbers() {
        String text = codeInput.getText().toString();
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

            // Anahtar kelimeleri vurgula (MAVİ)
            for (String keyword : JAVA_KEYWORDS) {
                Pattern pattern = Pattern.compile("\\b" + keyword + "\\b");
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    editable.setSpan(new ForegroundColorSpan(Color.parseColor("#569CD6")),
                            matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            // String değerleri vurgula (TURUNCU)
            Pattern stringPattern = Pattern.compile("\"(.*?)\"");
            Matcher stringMatcher = stringPattern.matcher(text);
            while (stringMatcher.find()) {
                editable.setSpan(new ForegroundColorSpan(Color.parseColor("#CE9178")),
                        stringMatcher.start(), stringMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Tek satır yorumları vurgula (YEŞİL)
            Pattern singleLineCommentPattern = Pattern.compile("//.*");
            Matcher singleLineCommentMatcher = singleLineCommentPattern.matcher(text);
            while (singleLineCommentMatcher.find()) {
                editable.setSpan(new ForegroundColorSpan(Color.parseColor("#6A9955")),
                        singleLineCommentMatcher.start(), singleLineCommentMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Çok satırlı yorumları vurgula (YEŞİL)
            Pattern multiLineCommentPattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
            Matcher multiLineCommentMatcher = multiLineCommentPattern.matcher(text);
            while (multiLineCommentMatcher.find()) {
                editable.setSpan(new ForegroundColorSpan(Color.parseColor("#6A9955")),
                        multiLineCommentMatcher.start(), multiLineCommentMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Sayıları vurgula (AÇIK YEŞİL)
            Pattern numberPattern = Pattern.compile("\\b\\d+\\.?\\d*\\b");
            Matcher numberMatcher = numberPattern.matcher(text);
            while (numberMatcher.find()) {
                editable.setSpan(new ForegroundColorSpan(Color.parseColor("#B5CEA8")),
                        numberMatcher.start(), numberMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Sınıf isimlerini vurgula (AÇIK MAVİ)
            Pattern classPattern = Pattern.compile("\\b[A-Z][a-zA-Z0-9_]*\\b");
            Matcher classMatcher = classPattern.matcher(text);
            while (classMatcher.find()) {
                String word = classMatcher.group();
                if (!isKeyword(word)) {
                    editable.setSpan(new ForegroundColorSpan(Color.parseColor("#4EC9B0")),
                            classMatcher.start(), classMatcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            // Metot isimlerini vurgula (SARI)
            Pattern methodPattern = Pattern.compile("\\b[a-z][a-zA-Z0-9_]*\\s*\\(");
            Matcher methodMatcher = methodPattern.matcher(text);
            while (methodMatcher.find()) {
                editable.setSpan(new ForegroundColorSpan(Color.parseColor("#DCDCAA")),
                        methodMatcher.start(), methodMatcher.end() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } finally {
            isApplyingHighlighting = false;
        }
    }

    private boolean isKeyword(String word) {
        for (String keyword : JAVA_KEYWORDS) {
            if (keyword.equals(word)) {
                return true;
            }
        }
        return false;
    }

    private void showSuggestions() {
        suggestionsList.setVisibility(View.VISIBLE);
    }

    private void hideSuggestions() {
        suggestionsList.setVisibility(View.GONE);
    }

    private void insertSuggestion(String suggestion) {
        int start = Math.max(codeInput.getSelectionStart(), 0);
        int end = Math.max(codeInput.getSelectionEnd(), 0);

        Editable editable = codeInput.getText();
        editable.replace(start, end, suggestion);

        // Eğer suggestion parantez içeriyorsa, imleci parantez içine yerleştir
        if (suggestion.contains("()")) {
            int newPosition = start + suggestion.indexOf("(") + 1;
            codeInput.setSelection(newPosition);
        } else if (suggestion.contains("{\n    \n}")) {
            int newPosition = start + suggestion.indexOf("\n    ") + 5;
            codeInput.setSelection(newPosition);
        }
    }

    private void saveCodeToStorage() {
        String code = codeInput.getText().toString();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("task_" + currentTaskId + "_code", code);
        editor.apply();
    }

    private void loadSavedCode() {
        String savedCode = sharedPreferences.getString("task_" + currentTaskId + "_code", "");
        if (!savedCode.isEmpty()) {
            codeInput.setText(savedCode);
            updateLineNumbers();
        }
    }

    private void runJavaCode() {
        String code = codeInput.getText().toString();

        if (code.trim().isEmpty()) {
            outputText.setText("> Lütfen çalıştırmak için bir Java kodu girin.");
            return;
        }

        StringBuilder output = new StringBuilder();

        // Əvvəlcə sintaksis xətalarını yoxla
        List<CompilerError> errors = checkSyntaxErrors(code);

        if (!errors.isEmpty()) {
            output.append("❌ KOMPİLYASİYA XƏTALARI:\n");
            output.append("════════════════════════\n\n");

            for (CompilerError error : errors) {
                output.append("📌 Sətir ").append(error.lineNumber).append(": ")
                        .append(error.errorType).append("\n");
                output.append("   ").append(error.message).append("\n\n");
            }

            output.append("⚠ Lütfen xətaları düzəldin və yenidən cəhd edin.\n");
            output.append("════════════════════════\n\n");
        } else {
            output.append("✅ Sintaksis xətası yoxdur. Kod kompilyasiya oluna bilər.\n\n");

            // JSON testləri varsa onları işlə
            if (tests != null && !tests.isEmpty()) {
                runJsonTests(code, output);
            } else {
                runBasicAnalysis(code, output);
            }
        }

        // Kodun təxmini nəticəsini göstər
        showCodeSimulation(code, output);

        outputText.setText(output.toString());
    }

    private List<CompilerError> checkSyntaxErrors(String code) {
        List<CompilerError> errors = new ArrayList<>();
        String[] lines = code.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            int lineNum = i + 1;

            // 1. Qeyri-bağlanmış string
            if (countOccurrences(line, "\"") % 2 != 0) {
                errors.add(new CompilerError("Qeyri-bağlanmış string dəyəri", lineNum, "String xətası"));
            }

            // 2. Qeyri-bağlanmış yorum
            if (line.contains("/*") && !line.contains("*/")) {
                boolean hasClosingInNextLines = false;
                for (int j = i + 1; j < lines.length; j++) {
                    if (lines[j].contains("*/")) {
                        hasClosingInNextLines = true;
                        break;
                    }
                }
                if (!hasClosingInNextLines) {
                    errors.add(new CompilerError("Qeyri-bağlanmış çoxsətirli yorum", lineNum, "Yorum xətası"));
                }
            }

            // 3. Nöqtəli vergül yoxluğu
            if (!line.isEmpty() && !line.startsWith("//") && !line.startsWith("/*") &&
                    !line.startsWith("*") && !line.endsWith("*/") && !line.endsWith("{") &&
                    !line.endsWith("}") && !line.contains("class ") && !line.contains("if ") &&
                    !line.contains("for ") && !line.contains("while ") && !line.contains("try ") &&
                    !line.contains("catch ") && !line.endsWith(";") && !line.startsWith("package") &&
                    !line.startsWith("import") && !line.contains("//")) {

                // Bəzi xüsusi halları çıxar
                if (!line.matches(".*\\b(if|for|while|try|catch|else|do)\\b.*\\(.*\\)") &&
                        !line.matches(".*\\b(class|interface|enum|@)\\b.*") &&
                        !line.matches(".*\\{\\s*") && !line.matches(".*\\}\\s*")) {

                    errors.add(new CompilerError("Nöqtəli vergül (;) çatışmır", lineNum, "Sintaksis xətası"));
                }
            }

            // 4. Qeyri-balanse mötərizələr
            int openPar = countOccurrences(line, "(");
            int closePar = countOccurrences(line, ")");
            int openBrace = countOccurrences(line, "{");
            int closeBrace = countOccurrences(line, "}");

            // Bu sətirdə balanssızlıq yoxla
            if ((openPar > 0 || closePar > 0) && openPar != closePar) {
                errors.add(new CompilerError("Mötərizələrdə balanssızlıq", lineNum, "Mötərizə xətası"));
            }

            // 5. Təyin etmə xətası
            if (line.matches(".*[a-zA-Z_$][a-zA-Z0-9_$]*\\s*=\\s*[^;]+") && !line.endsWith(";") &&
                    !line.contains("//") && !line.startsWith("//")) {
                errors.add(new CompilerError("Təyin operatoru tam deyil", lineNum, "Təyin xətası"));
            }

            // 6. Dublikat nöqtəli vergül
            if (line.contains(";;") && !line.contains("for")) {
                errors.add(new CompilerError("Dublikat nöqtəli vergül", lineNum, "Sintaksis xətası"));
            }
        }

        // 7. Ümumi balans yoxlaması
        int totalOpenPar = countOccurrences(code, "(");
        int totalClosePar = countOccurrences(code, ")");
        int totalOpenBrace = countOccurrences(code, "{");
        int totalCloseBrace = countOccurrences(code, "}");

        if (totalOpenPar != totalClosePar) {
            errors.add(new CompilerError(
                    "Ümumi mötərizə balanssızlığı: Açıq(" + totalOpenPar + ") ≠ Bağlı(" + totalClosePar + ")",
                    0, "Mötərizə balans xətası"
            ));
        }

        if (totalOpenBrace != totalCloseBrace) {
            errors.add(new CompilerError(
                    "Ümumi fiqurlu mötərizə balanssızlığı: Açıq{" + totalOpenBrace + "} ≠ Bağlı{" + totalCloseBrace + "}",
                    0, "Blok balans xətası"
            ));
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

    private void runJsonTests(String code, StringBuilder output) {
        output.append("🧪 TAPŞIRIQ TEST NƏTİCƏLƏRİ:\n\n");

        int passedTests = 0;
        int totalTests = tests.size();

        for (int i = 0; i < tests.size(); i++) {
            TaskModel.Test test = tests.get(i);
            boolean passed = runSingleTest(code, test);
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

        output.append("📊 ÜMUMİ TAPŞIRIQ TEST NƏTİCƏSİ: ").append(passedTests).append("/").append(totalTests);

        if (passedTests == totalTests) {
            output.append("\n\n🎉 TƏBRİKLƏR! Bütün testlər keçdi!");
            markTaskAsCompleted();
        } else {
            output.append("\n\n⚠ Bəzi testlər uğursuz oldu. Kodunuzu yoxlayın.");
        }

        output.append("\n════════════════════════\n");
    }

    private boolean runSingleTest(String code, TaskModel.Test test) {
        String cleanCode = code.toLowerCase().replaceAll("\\s+", "");
        String cleanSolution = solution != null ? solution.toLowerCase().replaceAll("\\s+", "") : "";

        if (!cleanSolution.isEmpty() && cleanCode.contains(cleanSolution)) {
            return true;
        }

        switch (currentTaskId) {
            case 1:
                return code.contains("eded * eded") || code.contains("Math.pow(eded,2)");
            case 2:
                return (code.contains("for") || code.contains("while")) &&
                        code.contains("faktorial") && code.contains("return");
            case 3:
                return code.contains("% 2") && (code.contains("Cüt") || code.contains("Tək"));
            case 4:
                return code.contains("if") && code.contains("return") &&
                        (code.contains("a > b") || code.contains("b > a"));
            case 5:
                return code.contains("for") && code.contains("charAt") && code.contains("length");
            default:
                return code.contains("return") && !code.contains("return 0;");
        }
    }

    private void runBasicAnalysis(String code, StringBuilder output) {
        output.append("📊 KOD ANALİZİ:\n\n");

        // Sınıf kontrolü
        if (code.contains("class")) {
            output.append("✓ Sınıf tanımı bulundu\n");
            Pattern classPattern = Pattern.compile("class\\s+(\\w+)");
            Matcher classMatcher = classPattern.matcher(code);
            if (classMatcher.find()) {
                output.append("✓ Sınıf adı: ").append(classMatcher.group(1)).append("\n");
            }
        }

        // Main metodu kontrolü
        if (code.contains("public static void main")) {
            output.append("✓ Main metodu bulundu\n");
        }

        // Method sayısı
        Pattern methodPattern = Pattern.compile("(public|private|protected|static)\\s+[\\w<>\\[\\]]+\\s+\\w+\\s*\\(");
        Matcher methodMatcher = methodPattern.matcher(code);
        int methodCount = 0;
        while (methodMatcher.find()) {
            methodCount++;
        }
        output.append("✓ Toplam metod sayısı: ").append(methodCount).append("\n");

        // Değişken sayısı
        String[] types = {"int", "String", "double", "float", "boolean", "char", "long"};
        int varCount = 0;
        for (String type : types) {
            Pattern varPattern = Pattern.compile("\\b" + type + "\\s+\\w+");
            Matcher varMatcher = varPattern.matcher(code);
            while (varMatcher.find()) {
                varCount++;
            }
        }
        output.append("✓ Toplam dəyişən sayısı: ").append(varCount).append("\n");

        output.append("\n");
    }

    private void showCodeSimulation(String code, StringBuilder output) {
        output.append("🚀 KOD SİMULYASIYASI:\n");
        output.append("══════════════════════\n");

        try {
            // Println ifadələrini tap
            List<String> outputs = simulateCodeExecution(code);

            if (!outputs.isEmpty()) {
                output.append("📋 Çıxış nəticələri:\n");
                for (int i = 0; i < outputs.size(); i++) {
                    output.append(i + 1).append(". ").append(outputs.get(i)).append("\n");
                }
            } else {
                output.append("ℹ️ Kod çıxış istehsal etmir (System.out.println tapılmadı)\n");
            }

            // Tapşırığa xas məlumat
            output.append("\n📌 TAPŞIRIQ MƏQSƏDİ:\n");
            switch (currentTaskId) {
                case 1:
                    output.append("• Ədədin kvadratını hesablama\n");
                    output.append("• Misal: kvadrat(5) = 25\n");
                    output.append("• Düzgün həll: return eded * eded;\n");
                    break;
                case 2:
                    output.append("• Faktorial hesablama\n");
                    output.append("• Misal: faktorial(5) = 120\n");
                    output.append("• Düzgün həll: for/while ilə vurma\n");
                    break;
                case 3:
                    output.append("• Tək/cüt yoxlama\n");
                    output.append("• Misal: 4 → Cüt, 7 → Tək\n");
                    output.append("• Düzgün həll: eded % 2 == 0\n");
                    break;
                case 4:
                    output.append("• İki ədəd arasında maksimum tapma\n");
                    output.append("• Misal: maksimum(5, 10) = 10\n");
                    output.append("• Düzgün həll: if(a > b) return a;\n");
                    break;
                case 5:
                    output.append("• Sətri tərsinə çevirmə\n");
                    output.append("• Misal: tərs(\"salam\") = \"malas\"\n");
                    output.append("• Düzgün həll: for döngüsü ilə\n");
                    break;
                default:
                    output.append("• Kodunuzun düzgün işlədiyindən əmin olun\n");
                    output.append("• Bütün test hallarını yoxlayın\n");
            }

        } catch (Exception e) {
            output.append("❌ Simulyasiya xətası: ").append(e.getMessage()).append("\n");
        }
    }

    private List<String> simulateCodeExecution(String code) {
        List<String> outputs = new ArrayList<>();

        // Sadə println analizi
        Pattern printPattern = Pattern.compile("System\\.out\\.print(ln)?\\(([^;]+)\\)");
        Matcher matcher = printPattern.matcher(code);

        while (matcher.find()) {
            String content = matcher.group(2).trim();
            String result = evaluateExpression(content, code);
            outputs.add(result);
        }

        return outputs;
    }

    private String evaluateExpression(String expr, String code) {
        expr = expr.trim();

        // String literal
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            return expr.substring(1, expr.length() - 1);
        }

        // Ədədi ifadə
        if (expr.matches("-?\\d+(\\.\\d+)?")) {
            return expr;
        }

        // Toplama
        if (expr.contains("+")) {
            String[] parts = expr.split("\\+");
            StringBuilder result = new StringBuilder();
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("\"")) {
                    result.append(part.substring(1, part.length() - 1));
                } else if (part.matches("\\d+")) {
                    result.append(part);
                } else if (isVariable(part, code)) {
                    result.append("[").append(part).append("]");
                } else {
                    result.append("?");
                }
            }
            return result.toString();
        }

        // Dəyişən
        if (isVariable(expr, code)) {
            return "[" + expr + "]";
        }

        return "[" + expr + "]";
    }

    private boolean isVariable(String name, String code) {
        // Sadə dəyişən yoxlaması
        return code.matches(".*\\b(int|String|double|float|boolean)\\s+" + name + "\\b.*");
    }

    private void markTaskAsCompleted() {
        SharedPreferences taskPrefs = getSharedPreferences(TASK_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = taskPrefs.edit();
        editor.putBoolean("task_" + currentTaskId + "_completed", true);
        editor.apply();
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
}