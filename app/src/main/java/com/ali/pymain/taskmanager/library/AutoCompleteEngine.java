package com.ali.pymain.taskmanager.library;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AutoCompleteEngine - python_completions.json faylından yükləyir
 * PyCharm kimi real-time autocomplete təmin edir
 */
public class AutoCompleteEngine {

    private static final String TAG = "AutoCompleteEngine";
    private static final String JSON_FILE = "dataPython/python_completions.json";

    // Bütün completion məlumatlarını saxlayan siyahılar
    private final List<CompletionItem> allItems = new ArrayList<>();
    private final Map<String, List<CompletionItem>> typeMethodsMap = new HashMap<>();

    // Task-specific suggestions üçün
    private final Map<Integer, List<CompletionItem>> taskSpecificSuggestions = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────
    // CompletionItem - hər bir autocomplete elementi
    // ─────────────────────────────────────────────────────────────────────
    public static class CompletionItem implements Comparable<CompletionItem> {
        public final String label;      // Görünən ad: "print"
        public final String type;       // "keyword" | "builtin" | "module" | "snippet"
        public final String snippet;    // Əlavə edilən kod: "print(${value})"
        public final String detail;     // Açıqlama: "Ekrana çıxarır"
        public final String icon;       // Emoji icon
        public final String params;     // Parametrlər: "(*objects, sep=' ', end='\\n')"
        public final int priority;      // Sıralama prioriteti

        public CompletionItem(String label, String type, String snippet,
                              String detail, String icon, String params) {
            this.label   = label   != null ? label   : "";
            this.type    = type    != null ? type    : "keyword";
            this.snippet = snippet != null ? snippet : label;
            this.detail  = detail  != null ? detail  : "";
            this.icon    = icon    != null ? icon    : "•";
            this.params  = params  != null ? params  : "";
            this.priority = calcPriority(type);
        }

        private int calcPriority(String t) {
            if (t == null) return 3;
            switch (t) {
                case "keyword":  return 1;
                case "builtin":  return 2;
                case "snippet":  return 3;
                case "module":   return 4;
                default:         return 5;
            }
        }

        /**
         * Kursoru doğru yerə qoymaq üçün snippet-i sadələşdirir.
         * ${1:name} → name, ${name} → name, ${1} → ""
         */
        public String getInsertText() {
            return snippet
                    .replaceAll("\\$\\{\\d+:([^}]+)\\}", "$1")
                    .replaceAll("\\$\\{([^}]+)\\}", "$1")
                    .replaceAll("\\$\\{\\d+\\}", "");
        }

        /** Popap-da göstəriləcək formatlanmış mətn */
        public String getDisplayText() {
            return icon + " " + label;
        }

        /** İkinci sətir - tip və açıqlama */
        public String getSubText() {
            String t = "[" + type + "]";
            return params.isEmpty() ? t + " " + detail : t + " " + detail + "  " + params;
        }

        @Override
        public int compareTo(CompletionItem o) {
            if (this.priority != o.priority) return this.priority - o.priority;
            return this.label.compareToIgnoreCase(o.label);
        }

        @Override
        public String toString() {
            return getDisplayText() + "\n" + getSubText();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Yükləmə
    // ─────────────────────────────────────────────────────────────────────

    public AutoCompleteEngine(Context context) {
        loadFromAssets(context);
    }

    private void loadFromAssets(Context context) {
        try {
            InputStream is = context.getAssets().open(JSON_FILE);
            int size = is.available();
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            parseJson(json);

            Log.d(TAG, "Yükləndi: " + allItems.size() + " element");
        } catch (IOException e) {
            Log.e(TAG, "JSON yükləmə xətası: " + e.getMessage());
            loadDefaults();
        }
    }

    private void parseJson(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            // ── Keywords ──────────────────────────────────────────────
            parseArray(root, "keywords");

            // ── Builtins ──────────────────────────────────────────────
            parseArray(root, "builtins");

            // ── Snippets ──────────────────────────────────────────────
            parseArray(root, "snippets");

            // ── Modules + metodlar ────────────────────────────────────
            if (root.has("modules")) {
                for (JsonElement el : root.getAsJsonArray("modules")) {
                    JsonObject mod = el.getAsJsonObject();
                    String modLabel  = getString(mod, "label");
                    String modDetail = getString(mod, "detail");
                    String modIcon   = getString(mod, "icon");

                    // Modulun özünü əlavə et
                    allItems.add(new CompletionItem(
                            modLabel, "module",
                            modLabel, modDetail, modIcon, ""));

                    // Modulun metodlarını əlavə et
                    if (mod.has("methods")) {
                        for (JsonElement me : mod.getAsJsonArray("methods")) {
                            JsonObject m = me.getAsJsonObject();
                            allItems.add(new CompletionItem(
                                    getString(m, "label"),
                                    "module",
                                    getString(m, "snippet"),
                                    getString(m, "detail"),
                                    modIcon,
                                    getString(m, "params")));
                        }
                    }
                }
            }

            // ── Tip metodları (str, list, dict ...) ───────────────────
            if (root.has("type_methods")) {
                JsonObject typeMethods = root.getAsJsonObject("type_methods");
                for (Map.Entry<String, JsonElement> entry : typeMethods.entrySet()) {
                    String typeName = entry.getKey();
                    List<CompletionItem> methods = new ArrayList<>();

                    for (JsonElement me : entry.getValue().getAsJsonArray()) {
                        JsonObject m = me.getAsJsonObject();
                        methods.add(new CompletionItem(
                                getString(m, "label"),
                                "method",
                                getString(m, "snippet"),
                                getString(m, "detail"),
                                "🔧",
                                getString(m, "params")));
                    }
                    typeMethodsMap.put(typeName, methods);
                }
            }

            Collections.sort(allItems);

        } catch (Exception e) {
            Log.e(TAG, "JSON parse xətası: " + e.getMessage());
            loadDefaults();
        }
    }

    private void parseArray(JsonObject root, String key) {
        if (!root.has(key)) return;
        for (JsonElement el : root.getAsJsonArray(key)) {
            JsonObject obj = el.getAsJsonObject();
            allItems.add(new CompletionItem(
                    getString(obj, "label"),
                    getString(obj, "type"),
                    getString(obj, "snippet"),
                    getString(obj, "detail"),
                    getString(obj, "icon"),
                    getString(obj, "params")));
        }
    }

    private String getString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }

    /** JSON açıla bilmədikdə minimal backup siyahısı */
    private void loadDefaults() {
        String[][] defaults = {
                {"print",  "builtin", "print(${value})", "Ekrana çıxarır", "🖨", "(value)"},
                {"len",    "builtin", "len(${obj})",     "Uzunluq",         "📏", "(obj)"},
                {"range",  "builtin", "range(${stop})",  "Ardıcıllıq",      "🔢", "(stop)"},
                {"def",    "keyword", "def ${name}():",  "Funksiya",        "⚡", ""},
                {"class",  "keyword", "class ${Name}:",  "Sinif",           "🏗", ""},
                {"for",    "keyword", "for ${i} in ${l}:","For döngüsü",   "🔁", ""},
                {"if",     "keyword", "if ${cond}:",     "Şərt",            "🔀", ""},
                {"return", "keyword", "return ${val}",   "Qaytarır",        "↩", ""},
                {"import", "keyword", "import ${mod}",   "İmport edir",     "📦", ""},
                {"True",   "keyword", "True",            "Doğru",           "✓", ""},
                {"False",  "keyword", "False",           "Yanlış",          "✗", ""},
                {"None",   "keyword", "None",            "Boş",             "∅", ""},
        };
        for (String[] d : defaults) {
            allItems.add(new CompletionItem(d[0], d[1], d[2], d[3], d[4], d[5]));
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // YENİ METOD - Task-a uyğun suggestionları yüklə
    // ═════════════════════════════════════════════════════════════════════
    public void loadTaskSuggestions(int taskId, String taskTitle, String taskDescription) {
        List<CompletionItem> taskSpecific = new ArrayList<>();

        String title = taskTitle != null ? taskTitle.toLowerCase() : "";
        String desc = taskDescription != null ? taskDescription.toLowerCase() : "";

        Log.d(TAG, "Loading suggestions for task: " + taskId + " - " + title);

        // Task başlığına görə xüsusi təkliflər
        if (title.contains("kvadrat") || desc.contains("kvadrat")) {
            taskSpecific.add(new CompletionItem("eded ** 2", "snippet", "eded * eded", "Ədədin kvadratı", "📐", ""));
            taskSpecific.add(new CompletionItem("return eded * eded", "snippet", "return eded * eded", "Kvadrat qaytar", "↩", ""));
        }

        if (title.contains("faktorial") || desc.contains("faktorial")) {
            taskSpecific.add(new CompletionItem("faktorial (loop)", "snippet",
                    "faktorial = 1\nfor i in range(1, n + 1):\n    faktorial *= i\nreturn faktorial",
                    "Loop ilə faktorial", "🔄", ""));
            taskSpecific.add(new CompletionItem("faktorial (recursive)", "snippet",
                    "if n <= 1:\n    return 1\nreturn n * faktorial(n - 1)",
                    "Rekursiv faktorial", "🔁", ""));
        }

        if (title.contains("palindrom") || desc.contains("palindrom")) {
            taskSpecific.add(new CompletionItem("text[::-1]", "snippet", "text == text[::-1]", "Palindrom yoxlaması", "🔄", ""));
        }

        if (title.contains("tək") || title.contains("cüt") || desc.contains("tək") || desc.contains("cüt")) {
            taskSpecific.add(new CompletionItem("tək/cüt", "snippet",
                    "if eded % 2 == 0:\n    return \"Cüt\"\nelse:\n    return \"Tək\"",
                    "Tək/Cüt yoxlaması", "🔢", ""));
        }

        if (title.contains("maksimum") || desc.contains("maksimum")) {
            taskSpecific.add(new CompletionItem("max(a, b)", "builtin", "max(a, b)", "Maksimum dəyər", "📊", "(a, b)"));
        }

        if (title.contains("rəqəmlərin cəmi") || desc.contains("rəqəm") && desc.contains("cəm")) {
            taskSpecific.add(new CompletionItem("sum(map(int, str(eded)))", "builtin", "sum(map(int, str(abs(eded))))", "Rəqəmlər cəmi", "➕", ""));
        }

        if (title.contains("fibonacci") || desc.contains("fibonacci")) {
            taskSpecific.add(new CompletionItem("fibonacci", "snippet",
                    "if n <= 1:\n    return n\na, b = 0, 1\nfor i in range(2, n + 1):\n    a, b = b, a + b\nreturn b",
                    "Fibonacci ədədi", "🔢", ""));
        }

        if (title.contains("sadə") || desc.contains("sadə") && desc.contains("ədəd")) {
            taskSpecific.add(new CompletionItem("sadə ədəd", "snippet",
                    "if eded <= 1:\n    return False\nfor i in range(2, int(eded**0.5) + 1):\n    if eded % i == 0:\n        return False\nreturn True",
                    "Sadə ədəd yoxlaması", "🔢", ""));
        }

        if (title.contains("list") && (title.contains("cəm") || desc.contains("cəm"))) {
            taskSpecific.add(new CompletionItem("sum(lst)", "builtin", "sum(lst)", "Listin cəmi", "➕", "(list)"));
        }

        if (title.contains("tərsinə") || desc.contains("tərs")) {
            taskSpecific.add(new CompletionItem("text[::-1]", "snippet", "text[::-1]", "String tərsinə", "🔄", ""));
        }

        if (title.contains("vurma cədvəli") || desc.contains("vurma")) {
            taskSpecific.add(new CompletionItem("vurma cədvəli", "snippet",
                    "[eded * i for i in range(1, 11)]", "Vurma cədvəli", "📊", ""));
        }

        if (title.contains("böyük hərf") || desc.contains("böyük")) {
            taskSpecific.add(new CompletionItem("text.upper()", "method", "text.upper()", "Böyük hərflər", "🔠", ""));
        }

        if (title.contains("fahrenheit") || desc.contains("celsius")) {
            taskSpecific.add(new CompletionItem("(f - 32) * 5 / 9", "snippet", "(f - 32) * 5 / 9", "Fahrenheit → Celsius", "🌡️", ""));
        }

        if (title.contains("orta qiymət") || desc.contains("orta")) {
            taskSpecific.add(new CompletionItem("sum(lst) / len(lst)", "builtin", "sum(lst) / len(lst)", "Orta qiymət", "📊", ""));
        }

        // Döngü ilə bağlı tasklar
        if (title.contains("for") || desc.contains("döngü")) {
            taskSpecific.add(new CompletionItem("for range", "snippet", "for i in range(n):\n    ", "For döngüsü", "🔄", ""));
        }

        if (title.contains("while") || desc.contains("while")) {
            taskSpecific.add(new CompletionItem("while", "snippet", "while condition:\n    ", "While döngüsü", "🔄", ""));
        }

        if (title.contains("comprehension") || desc.contains("comprehension")) {
            taskSpecific.add(new CompletionItem("list comprehension", "snippet", "[x for x in range(10)]", "List comprehension", "📋", ""));
        }

        if (!taskSpecific.isEmpty()) {
            taskSpecificSuggestions.put(taskId, taskSpecific);
            Log.d(TAG, "Loaded " + taskSpecific.size() + " task-specific suggestions for task " + taskId);
        }
    }

    /**
     * Task-a xüsusi suggestionları qaytarır
     */
    public List<CompletionItem> getTaskSuggestions(int taskId) {
        List<CompletionItem> suggestions = taskSpecificSuggestions.get(taskId);
        return suggestions != null ? suggestions : new ArrayList<>();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Axtarış API
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Söz prefiksinə görə uyğun elementləri qaytarır.
     * Ən az 1 hərf yazılanda başlayır.
     *
     * @param prefix İstifadəçinin yazdığı son söz
     * @param limit  Maksimum nəticə sayı
     */
    public List<CompletionItem> getSuggestions(String prefix, int limit) {
        if (prefix == null || prefix.isEmpty()) return Collections.emptyList();

        String lower = prefix.toLowerCase();
        List<CompletionItem> exact   = new ArrayList<>();
        List<CompletionItem> starts  = new ArrayList<>();
        List<CompletionItem> contains = new ArrayList<>();

        for (CompletionItem item : allItems) {
            String lbl = item.label.toLowerCase();
            if (lbl.equals(lower)) {
                exact.add(item);
            } else if (lbl.startsWith(lower)) {
                starts.add(item);
            } else if (lbl.contains(lower)) {
                contains.add(item);
            }
            if (exact.size() + starts.size() + contains.size() >= limit * 3) break;
        }

        List<CompletionItem> result = new ArrayList<>();
        result.addAll(exact);
        result.addAll(starts);
        result.addAll(contains);

        if (result.size() > limit) result = result.subList(0, limit);
        return result;
    }

    /**
     * Nöqtəli tamamlama: var.| yazılanda
     * Əvvəlcə dəyişənin tipini koddan aşkar edir, sonra o tipin metodlarını qaytarır.
     *
     * @param code     Tam kod mətni
     * @param varName  Nöqtədən əvvəlki dəyişənin adı
     */
    public List<CompletionItem> getDotCompletions(String code, String varName) {
        String detectedType = detectType(code, varName);
        if (detectedType != null && typeMethodsMap.containsKey(detectedType)) {
            return typeMethodsMap.get(detectedType);
        }
        // Tip müəyyən edilə bilmədisə, modul metodlarını axtar
        return getModuleCompletions(varName);
    }

    /**
     * Modul adı ilə uyğun metodları qaytarır.
     * Məs: "np" → numpy metodları, "math" → math metodları
     */
    public List<CompletionItem> getModuleCompletions(String varName) {
        // Modul aliasları
        Map<String, String> aliases = new HashMap<>();
        aliases.put("np",   "numpy");
        aliases.put("pd",   "pandas");
        aliases.put("plt",  "matplotlib");

        String resolved = aliases.containsKey(varName) ? aliases.get(varName) : varName;
        String prefix = resolved + ".";

        List<CompletionItem> result = new ArrayList<>();
        for (CompletionItem item : allItems) {
            if (item.label.startsWith(prefix) && item.type.equals("module")) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Tip metodlarını birbaşa tipə görə qaytarır.
     */
    public List<CompletionItem> getTypeMethodCompletions(String typeName) {
        List<CompletionItem> methods = typeMethodsMap.get(typeName);
        return methods != null ? methods : Collections.emptyList();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Dəyişən tip aşkarlaması
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Kodda dəyişənin tipini müəyyən edir.
     * Məs: x = "hello" → "str", y = [1,2] → "list"
     */
    public String detectType(String code, String varName) {
        if (code == null || varName == null) return null;
        String[] lines = code.split("\n");

        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();

            // x = "..." → str
            if (matchAssignment(line, varName, "\"") || matchAssignment(line, varName, "'"))
                return "str";
            // x = [...] → list
            if (matchAssignment(line, varName, "["))
                return "list";
            // x = {...} → dict (lüğət) yoxsa set
            if (matchAssignment(line, varName, "{"))
                return line.contains(":") ? "dict" : "set";
            // x = (...) → tuple
            if (matchAssignment(line, varName, "("))
                return "tuple";
            // Explicit tipler: x = int(...), x = str(...)
            for (String t : new String[]{"str", "list", "dict", "set", "tuple", "int", "float"}) {
                if (line.matches(".*\\b" + varName + "\\s*=\\s*" + t + "\\s*\\(.*"))
                    return t;
            }
            // x: str = ...
            if (line.matches(".*\\b" + varName + "\\s*:\\s*(str|int|float|list|dict|set|tuple|bool)\\b.*")) {
                for (String t : new String[]{"str","int","float","list","dict","set","tuple","bool"}) {
                    if (line.contains(": " + t) || line.contains(":" + t)) return t;
                }
            }
        }
        return null;
    }

    private boolean matchAssignment(String line, String varName, String rhs) {
        return line.matches(".*\\b" + varName + "\\s*=\\s*" + java.util.regex.Pattern.quote(rhs) + ".*");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────

    public int getTotalCount() {
        return allItems.size();
    }

    public boolean isLoaded() {
        return !allItems.isEmpty();
    }
}