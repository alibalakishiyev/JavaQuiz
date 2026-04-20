package com.ali.aimain;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.ali.aimain.model.ModelMathObject;

import com.ali.systemIn.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class AICalculate extends AppCompatActivity {

    // ── Input fields ──────────────────────────────────────────────────
    private TextInputEditText etDataSize, etCorrectAnswers, etTotalQuestions;
    private TextInputEditText etGpuMemory, etCurrentLoss, etValLoss;
    private TextInputLayout tilDataSize, tilCorrectAnswers, tilTotalQuestions;
    private TextInputLayout tilGpuMemory, tilCurrentLoss, tilValLoss;

    // ── Result TextViews ───────────────────────────────────────────────
    private TextView tvBatchSize, tvLoss, tvNHead, tvNLayer;
    private TextView tvNEmb, tvLearningRate, tvDropout, tvBlockSize;
    private TextView tvGradAccum, tvMaxIters, tvResult;
    private TextView tvGpuUtil, tvLossAnalysis, tvGradientStatus, tvMethodInfo;

    // ── Buttons ────────────────────────────────────────────────────────
    private Button btnCalculate, btnValidate, btnAdvancedCalc, btnMethodInfo;

    // ── Spinners ───────────────────────────────────────────────────────
    private AutoCompleteTextView spinnerModelType, spinnerPreset;

    // ── State ──────────────────────────────────────────────────────────
    private JSONObject configRules;
    private ModelMathObject model;
    private String currentModelType;
    private String currentPreset = null;

    private AutoCompleteTextView etMethodName;  // Tipi dəyişdirin
    private List<PyTorchMethod> pytorchMethods;

    // ── Advanced card reference ────────────────────────────────────────
    private View advancedCard;
    private View resultCard;

    private static class PyTorchMethod {
        String name;
        String description;
        String category;

        PyTorchMethod(String name, String description, String category) {
            this.name = name;
            this.description = description;
            this.category = category;
        }

        @Override
        public String toString() {
            return name + " (" + category + ")";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_calculate);
        initializeViews();
        loadConfig();
        setupSpinners();
        setupButtons();
        setupPyTorchMethods();
    }

    // ══════════════════════════════════════════════════════════════════
    //  INITIALISATION
    // ══════════════════════════════════════════════════════════════════

    private void initializeViews() {
        // Inputs
        etDataSize        = findViewById(R.id.et_data_size);
        etCorrectAnswers  = findViewById(R.id.et_correct_answers);
        etTotalQuestions  = findViewById(R.id.et_total_questions);
        etGpuMemory       = findViewById(R.id.et_gpu_memory);
        etCurrentLoss     = findViewById(R.id.et_current_loss);
        etValLoss         = findViewById(R.id.et_val_loss);
        etMethodName      = findViewById(R.id.et_method_name);

        // TextInputLayouts for error display
        tilDataSize       = findViewById(R.id.til_data_size);
        tilCorrectAnswers = findViewById(R.id.til_correct_answers);
        tilTotalQuestions = findViewById(R.id.til_total_questions);
        tilGpuMemory      = findViewById(R.id.til_gpu_memory);
        tilCurrentLoss    = findViewById(R.id.til_current_loss);
        tilValLoss        = findViewById(R.id.til_val_loss);

        // Basic results
        tvBatchSize    = findViewById(R.id.tv_batch_size);
        tvLoss         = findViewById(R.id.tv_loss);
        tvNHead        = findViewById(R.id.tv_n_head);
        tvNLayer       = findViewById(R.id.tv_n_layer);
        tvNEmb         = findViewById(R.id.tv_n_emb);
        tvLearningRate = findViewById(R.id.tv_learning_rate);
        tvDropout      = findViewById(R.id.tv_dropout);
        tvBlockSize    = findViewById(R.id.tv_block_size);
        tvGradAccum    = findViewById(R.id.tv_grad_accum);
        tvMaxIters     = findViewById(R.id.tv_max_iters);
        tvResult       = findViewById(R.id.tv_result);

        // Advanced results
        tvGpuUtil        = findViewById(R.id.tv_gpu_util);
        tvLossAnalysis   = findViewById(R.id.tv_loss_analysis);
        tvGradientStatus = findViewById(R.id.tv_gradient_status);
        tvMethodInfo     = findViewById(R.id.tv_method_info);

        // Buttons
        btnCalculate    = findViewById(R.id.btn_calculate);
        btnValidate     = findViewById(R.id.btn_validate);
        btnAdvancedCalc = findViewById(R.id.btn_advanced_calc);
        btnMethodInfo   = findViewById(R.id.btn_method_info);

        // Spinners
        spinnerModelType = findViewById(R.id.spinner_model_type);
        spinnerPreset    = findViewById(R.id.spinner_preset);

        // Cards
        advancedCard = findViewById(R.id.card_advanced);
        resultCard   = findViewById(R.id.card_results);

        // Initially hide advanced results
        setVisible(tvGpuUtil,        false);
        setVisible(tvLossAnalysis,   false);
        setVisible(tvGradientStatus, false);
        setVisible(tvMethodInfo,     false);
    }

    private void loadConfig() {
        try (InputStream is = getAssets().open("dataAi/MathQuest/ai_calculate.json");
             Scanner sc = new Scanner(is).useDelimiter("\\A")) {
            String json = sc.hasNext() ? sc.next() : "";
            configRules = new JSONObject(json);
            currentModelType = configRules.optString("default_type", "NLP");

            // PyTorch metodlarını yüklə
            loadPyTorchMethods();

        } catch (Exception e) {
            Log.e("AICalculate", "Config load failed", e);
            showToast("Konfiqurasiya yüklənərkən xəta baş verdi");
        }
    }

    private void loadPyTorchMethods() {
        pytorchMethods = new ArrayList<>();
        try {
            if (configRules.has("pytorch_methods")) {
                JSONArray methodsArray = configRules.getJSONArray("pytorch_methods");
                for (int i = 0; i < methodsArray.length(); i++) {
                    JSONObject method = methodsArray.getJSONObject(i);
                    String name = method.getString("name");
                    String description = method.optString("description", "");
                    String category = method.optString("category", "General");
                    pytorchMethods.add(new PyTorchMethod(name, description, category));
                }
            }
        } catch (JSONException e) {
            Log.e("AICalculate", "Failed to load PyTorch methods", e);
            // Fallback metodları
            addFallbackMethods();
        }
    }
    // Model tipi dəyişdikdə metodları filtrlə (opsiyonel)
    private void filterMethodsByCategory(String category) {
        List<PyTorchMethod> filtered = new ArrayList<>();
        for (PyTorchMethod method : pytorchMethods) {
            if (method.category.equals(category) || category.equals("All")) {
                filtered.add(method);
            }
        }

        ArrayAdapter<PyTorchMethod> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                filtered
        );
        etMethodName.setAdapter(adapter);
        etMethodName.setText("", false);
    }

    // ── METHOD INFO (yenilənmiş) ─────────────────────────────────────
    private void lookupMethod() {
        String name = etMethodName.getText() != null
                ? etMethodName.getText().toString().trim() : "";

        if (name.isEmpty()) {
            showToast("Bir PyTorch metodu seçin və ya adını yazın");
            return;
        }

        // Seçilmiş metodu tap
        PyTorchMethod selectedMethod = null;
        if (etMethodName.getTag() instanceof PyTorchMethod) {
            selectedMethod = (PyTorchMethod) etMethodName.getTag();
        } else {
            // Tag yoxdursa, listdən axtar
            for (PyTorchMethod method : pytorchMethods) {
                if (method.name.equalsIgnoreCase(name)) {
                    selectedMethod = method;
                    break;
                }
            }
        }

        if (selectedMethod != null) {
            String detailedInfo = String.format(
                    "📌 %s\n📂 Category: %s\n📝 %s\n\n💡 Recommended for: %s tasks",
                    selectedMethod.name,
                    selectedMethod.category,
                    selectedMethod.description,
                    getRecommendedUsage(selectedMethod.category)
            );
            animateTextView(tvMethodInfo, detailedInfo);
        } else {
            // Metod tapılmadısa, internetdə axtarış üçün link təklif et
            String info = String.format(
                    "❌ Method '%s' not found in local database.\n\n🔍 Try searching: https://pytorch.org/docs/stable/search.html?q=%s\n\n📚 Common methods: Transformer, BERT, GPT, ResNet, ViT, YOLO, Whisper",
                    name, name.replace(" ", "+")
            );
            animateTextView(tvMethodInfo, info);
        }

        setVisible(tvMethodInfo, true);
        pulseCard(findViewById(R.id.card_advanced));
    }

    private String getRecommendedUsage(String category) {
        switch (category) {
            case "NLP": return "text classification, translation, language modeling";
            case "Vision": return "image classification, object detection, segmentation";
            case "TTS": return "speech synthesis, voice cloning";
            case "ASR": return "speech recognition, transcription";
            default: return "general deep learning tasks";
        }
    }
    private void addFallbackMethods() {
        pytorchMethods.add(new PyTorchMethod("Transformer", "Standard Transformer architecture", "NLP"));
        pytorchMethods.add(new PyTorchMethod("BERT", "Bidirectional Encoder Representations", "NLP"));
        pytorchMethods.add(new PyTorchMethod("GPT", "Generative Pre-trained Transformer", "NLP"));
        pytorchMethods.add(new PyTorchMethod("ResNet", "Residual Neural Network", "Vision"));
        pytorchMethods.add(new PyTorchMethod("ViT", "Vision Transformer", "Vision"));
        pytorchMethods.add(new PyTorchMethod("Tacotron2", "Text-to-Speech synthesis", "TTS"));
    }

    private void setupPyTorchMethods() {
        if (pytorchMethods == null || pytorchMethods.isEmpty()) {
            loadPyTorchMethods();
        }

        // Metod adlarını ArrayAdapter üçün hazırlayın
        ArrayAdapter<PyTorchMethod> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                pytorchMethods
        );

        etMethodName.setAdapter(adapter);
        etMethodName.setThreshold(1); // 1 hərf yazıldıqda filtrasiya başlasın

        // Metod seçildikdə
        etMethodName.setOnItemClickListener((parent, view, position, id) -> {
            PyTorchMethod selectedMethod = (PyTorchMethod) parent.getItemAtPosition(position);
            etMethodName.setText(selectedMethod.name, false);

            // Seçilmiş metod haqqında qısa məlumat göstər
            String info = selectedMethod.name + ": " + selectedMethod.description;
            animateTextView(tvMethodInfo, info);
            setVisible(tvMethodInfo, true);

            // JSON-a qeyd etmək üçün
            etMethodName.setTag(selectedMethod);
        });

        // Filtrləməni kateqoriyaya görə də etmək üçün (opsiyonel)
        etMethodName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etMethodName.getText().toString().isEmpty()) {
                etMethodName.showDropDown();
            }
        });
    }


    // ══════════════════════════════════════════════════════════════════
    //  SPINNERS
    // ══════════════════════════════════════════════════════════════════

    private void setupSpinners() {
        if (configRules == null) return;
        try {
            JSONArray available = configRules.getJSONArray("available_types");
            List<String> types = new ArrayList<>();
            for (int i = 0; i < available.length(); i++) types.add(available.getString(i));

            ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_dropdown_item_1line, types);
            spinnerModelType.setAdapter(typeAdapter);
            spinnerModelType.setText(currentModelType, false);

            spinnerModelType.setOnItemClickListener((p, v, pos, id) -> {
                currentModelType = types.get(pos);
                currentPreset = null;
                refreshPresets();
                resetResults();
            });

            spinnerModelType.setOnItemClickListener((p, v, pos, id) -> {
                currentModelType = types.get(pos);
                currentPreset = null;
                refreshPresets();
                resetResults();

                // Metodları kateqoriyaya görə filtrlə (opsiyonel)
                filterMethodsByCategory(currentModelType);
            });

            refreshPresets();

            spinnerPreset.setOnItemClickListener((p, v, pos, id) -> {
                currentPreset = (String) p.getItemAtPosition(pos);
                applyPreset();
            });

        } catch (JSONException e) {
            Log.e("AICalculate", "Spinner setup failed", e);
        }
    }

    private void refreshPresets() {
        try {
            JSONObject allPresets = configRules.getJSONObject("presets");
            if (!allPresets.has(currentModelType)) return;

            JSONObject typePresets = allPresets.getJSONObject(currentModelType);
            List<String> names = new ArrayList<>();
            Iterator<String> keys = typePresets.keys();
            while (keys.hasNext()) names.add(keys.next());

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_dropdown_item_1line, names);
            spinnerPreset.setAdapter(adapter);
            spinnerPreset.setText("", false);

        } catch (JSONException e) {
            Log.e("AICalculate", "Preset refresh failed", e);
        }
    }

    private void applyPreset() {
        if (currentPreset == null || currentPreset.isEmpty()) return;
        try {
            JSONObject preset = configRules.getJSONObject("presets")
                    .getJSONObject(currentModelType)
                    .getJSONObject(currentPreset);

            // Pre-fill data size from preset n_layer as a hint
            etDataSize.setText(String.valueOf(preset.optInt("n_layer") * 1000));

            animateTextView(tvNLayer, String.format("Layers: %d",  preset.optInt("n_layer")));
            animateTextView(tvNHead,  String.format("Heads: %d",   preset.optInt("n_head")));
            animateTextView(tvNEmb,   String.format("Embed: %d",   preset.optInt("n_embd")));

            showToast(currentPreset + " preset tətbiq edildi");

        } catch (JSONException e) {
            Log.e("AICalculate", "Apply preset failed", e);
            showToast("Preset tətbiq edilərkən xəta baş verdi");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  BUTTONS
    // ══════════════════════════════════════════════════════════════════

    private void setupButtons() {
        btnCalculate.setOnClickListener(v -> calculate());
        btnValidate.setOnClickListener(v -> validate());
        btnAdvancedCalc.setOnClickListener(v -> advancedCalculate());
        btnMethodInfo.setOnClickListener(v -> lookupMethod());
    }

    // ── CALCULATE ─────────────────────────────────────────────────────

    private void calculate() {
        clearFieldErrors();
        if (!validateBasicInputs()) return;

        try {
            double dataSize       = parseDouble(etDataSize);
            double correctAnswers = parseDouble(etCorrectAnswers);
            double totalQuestions = parseDouble(etTotalQuestions);

            model = new ModelMathObject(configRules, currentModelType,
                    dataSize, correctAnswers, totalQuestions);

            updateBasicResultViews();
            pulseCard(resultCard);
            setResult("Parametrlər hesablandı — Validate ilə yoxlayın",
                    R.color.text_default);

        } catch (Exception e) {
            Log.e("AICalculate", "Calculate error", e);
            showToast("Hesablama xətası: " + e.getMessage());
        }
    }

    private void updateBasicResultViews() {
        animateTextView(tvBatchSize,    fmt("Batch", model.getBatchSize(), "%.0f"));
        animateTextView(tvLoss,         fmt("Loss",  model.getLoss(),      "%.4f"));
        animateTextView(tvNHead,        fmt("Heads", model.getNHead(),     "%.0f"));
        animateTextView(tvNLayer,       fmt("Layers",model.getNLayer(),    "%.0f"));
        animateTextView(tvNEmb,         fmt("Embed", model.getNEmb(),      "%.0f"));
        animateTextView(tvLearningRate, fmt("LR",    model.getLearningRate(), "%.2e"));
        animateTextView(tvDropout,      fmt("Dropout",model.getDropout(), "%.2f"));
        animateTextView(tvBlockSize,    fmt("Block", model.getBlockSize(),"%.0f"));
        animateTextView(tvGradAccum,    fmt("Grad Steps",model.getGradientAccumulationSteps(),"%.0f"));
        animateTextView(tvMaxIters,     fmt("Max Iters",model.getMaxIters(),"%.0f"));
    }

    // ── VALIDATE ──────────────────────────────────────────────────────

    private void validate() {
        if (model == null) {
            showToast("Əvvəlcə Calculate edin");
            return;
        }
        try {
            JSONObject typeRules = configRules
                    .getJSONObject("type_specific_rules")
                    .getJSONObject(currentModelType);

            StringBuilder sb = new StringBuilder();
            boolean allOk = true;

            JSONObject arch = typeRules.getJSONObject("model_architecture");
            allOk &= checkParam("N Head",  model.getNHead(),  arch.getJSONObject("n_head"),  sb);
            allOk &= checkParam("N Layer", model.getNLayer(), arch.getJSONObject("n_layer"), sb);

            JSONObject train = typeRules.getJSONObject("training_parameters");
            allOk &= checkParam("Batch Size", model.getBatchSize(),
                    train.getJSONObject("batch_size"), sb);

            sb.append("\n").append(allOk ? "✓ Bütün parametrlər düzgündür" : "✗ Bəzi parametrlər sərhəddən kənardadır");
            setResult(sb.toString(), allOk ? R.color.valid : R.color.invalid);
            pulseCard(resultCard);

        } catch (JSONException e) {
            Log.e("AICalculate", "Validate error", e);
            showToast("Konfiqurasiya xətası");
        }
    }

    private boolean checkParam(String name, double value, JSONObject rule,
                               StringBuilder out) throws JSONException {
        double min = rule.getDouble("minimum");
        double max = rule.getDouble("maximum");
        boolean ok = value >= min && value <= max;
        out.append(String.format("%-12s %.2f  [%.0f – %.0f]  %s\n",
                name + ":", value, min, max, ok ? "✓" : "✗"));
        return ok;
    }

    // ── ADVANCED CALCULATE ────────────────────────────────────────────

    private void advancedCalculate() {
        if (model == null) {
            showToast("Əvvəlcə əsas Calculate edin");
            return;
        }
        clearAdvancedErrors();
        if (!validateAdvancedInputs()) return;

        try {
            double gpuMem  = parseDouble(etGpuMemory);
            double curLoss = parseDouble(etCurrentLoss);
            double valLoss = parseDouble(etValLoss);

            model.updateAdvancedParams(gpuMem, curLoss, valLoss);

            setVisible(tvGpuUtil,        true);
            setVisible(tvLossAnalysis,   true);
            setVisible(tvGradientStatus, true);

            animateTextView(tvGpuUtil,
                    String.format("GPU: %.1f%%", model.getGpuUtilization()));
            animateTextView(tvLossAnalysis,
                    String.format("Loss ratio: %.2f", model.getLossRatio()));
            animateTextView(tvGradientStatus,
                    "Gradient: " + model.getGradientStatus());

            showRecommendations();
            pulseCard(resultCard);

        } catch (NumberFormatException e) {
            showToast("Zəhmət olmasa düzgün rəqəm daxil edin");
        } catch (Exception e) {
            Log.e("AICalculate", "Advanced calc error", e);
            showToast("Qabaqcıl hesablama xətası");
        }
    }

    private void showRecommendations() {
        StringBuilder advice = new StringBuilder("Tövsiyələr:\n");

        if (model.getGpuUtilization() < 70)
            advice.append("• Batch size-ı artırın — GPU utilizasiyası aşağıdır\n");
        if (model.getLossRatio() > 1.5)
            advice.append("• Model overfit edir — dropout artırın\n");
        if (model.getLossRatio() < 0.8)
            advice.append("• Model underfit edir — model ölçüsünü artırın\n");
        if (model.getGpuUtilization() > 95)
            advice.append("• GPU yaddaşı dolmaq üzrədir — batch size azaldın\n");

        try {
            JSONObject typeRules = configRules
                    .getJSONObject("type_specific_rules")
                    .getJSONObject(currentModelType);
            if (typeRules.has("recommendations")) {
                String general = typeRules.getJSONObject("recommendations")
                        .optString("general", "");
                if (!general.isEmpty()) advice.append("• ").append(general);
            }
        } catch (JSONException ignored) {}

        if (advice.toString().equals("Tövsiyələr:\n"))
            advice.append("• Parametrlər optimal görünür");

        Toast.makeText(this, advice.toString(), Toast.LENGTH_LONG).show();
    }

    // ── METHOD INFO ───────────────────────────────────────────────────


    // ══════════════════════════════════════════════════════════════════
    //  INPUT VALIDATION
    // ══════════════════════════════════════════════════════════════════

    private boolean validateBasicInputs() {
        boolean ok = true;

        if (etDataSize.getText() == null || etDataSize.getText().toString().isEmpty()) {
            tilDataSize.setError("Data size daxil edin");
            ok = false;
        } else {
            double d = parseDouble(etDataSize);
            if (d <= 0) { tilDataSize.setError("0-dan böyük olmalıdır"); ok = false; }
        }

        if (etCorrectAnswers.getText() == null || etCorrectAnswers.getText().toString().isEmpty()) {
            tilCorrectAnswers.setError("Düzgün cavabları daxil edin");
            ok = false;
        }

        if (etTotalQuestions.getText() == null || etTotalQuestions.getText().toString().isEmpty()) {
            tilTotalQuestions.setError("Ümumi sualları daxil edin");
            ok = false;
        } else {
            double total   = parseDouble(etTotalQuestions);
            double correct = etCorrectAnswers.getText() != null &&
                    !etCorrectAnswers.getText().toString().isEmpty()
                    ? parseDouble(etCorrectAnswers) : -1;
            if (total <= 0) {
                tilTotalQuestions.setError("0-dan böyük olmalıdır");
                ok = false;
            } else if (correct < 0 || correct > total) {
                tilCorrectAnswers.setError("0 ilə " + (int) total + " arasında olmalıdır");
                ok = false;
            }
        }
        return ok;
    }

    private boolean validateAdvancedInputs() {
        boolean ok = true;
        if (etGpuMemory.getText() == null || etGpuMemory.getText().toString().isEmpty()) {
            tilGpuMemory.setError("GPU yaddaşını daxil edin"); ok = false;
        }
        if (etCurrentLoss.getText() == null || etCurrentLoss.getText().toString().isEmpty()) {
            tilCurrentLoss.setError("Cari loss daxil edin"); ok = false;
        }
        if (etValLoss.getText() == null || etValLoss.getText().toString().isEmpty()) {
            tilValLoss.setError("Validasiya loss daxil edin"); ok = false;
        }
        return ok;
    }

    private void clearFieldErrors() {
        tilDataSize.setError(null);
        tilCorrectAnswers.setError(null);
        tilTotalQuestions.setError(null);
    }

    private void clearAdvancedErrors() {
        tilGpuMemory.setError(null);
        tilCurrentLoss.setError(null);
        tilValLoss.setError(null);
    }

    // ══════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════

    private void resetResults() {
        String dash = " —";
        tvBatchSize.setText("Batch" + dash);
        tvLoss.setText("Loss" + dash);
        tvNHead.setText("Heads" + dash);
        tvNLayer.setText("Layers" + dash);
        tvNEmb.setText("Embed" + dash);
        tvLearningRate.setText("LR" + dash);
        tvDropout.setText("Dropout" + dash);
        tvBlockSize.setText("Block" + dash);
        tvGradAccum.setText("Grad Steps" + dash);
        tvMaxIters.setText("Max Iters" + dash);
        tvResult.setText("Parametrləri seçib Calculate edin");
        tvResult.setTextColor(ContextCompat.getColor(this, R.color.text_default));
        setVisible(tvGpuUtil, false);
        setVisible(tvLossAnalysis, false);
        setVisible(tvGradientStatus, false);
        setVisible(tvMethodInfo, false);
    }

    private void setResult(String text, int colorRes) {
        tvResult.setText(text);
        tvResult.setTextColor(ContextCompat.getColor(this, colorRes));
    }

    private void setVisible(View v, boolean visible) {
        v.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private double parseDouble(TextInputEditText et) {
        try {
            return Double.parseDouble(et.getText().toString().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String fmt(String label, double value, String format) {
        return label + ": " + String.format(format, value);
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /** Fades a TextView from 0→1 alpha while updating its text. */
    private void animateTextView(final TextView tv, final String newText) {
        ValueAnimator fadeOut = ValueAnimator.ofFloat(1f, 0f);
        fadeOut.setDuration(120);
        fadeOut.addUpdateListener(a -> tv.setAlpha((float) a.getAnimatedValue()));
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                tv.setText(newText);
                ValueAnimator fadeIn = ValueAnimator.ofFloat(0f, 1f);
                fadeIn.setDuration(200);
                fadeIn.addUpdateListener(b -> tv.setAlpha((float) b.getAnimatedValue()));
                fadeIn.start();
            }
        });
        fadeOut.start();
    }

    /** Briefly scales a card up and back to give a "pop" feel. */
    private void pulseCard(View card) {
        if (card == null) return;
        card.animate().scaleX(1.02f).scaleY(1.02f).setDuration(120)
                .withEndAction(() ->
                        card.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                ).start();
    }
}