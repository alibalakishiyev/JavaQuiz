package com.ali.aimain;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ali.systemIn.R;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class AICalculate extends AppCompatActivity {

    // UI Components
    private EditText etDataSize, etCorrectAnswers, etTotalQuestions, etGpuMemory;
    private EditText etCurrentLoss, etValLoss, etMethodName;
    private TextView tvBatchSize, tvLoss, tvNHead, tvNLayer, tvResult;
    private TextView tvGpuUtil, tvLossAnalysis, tvGradientStatus, tvMethodInfo;
    private TextView tvNEmb, tvLearningRate, tvDropout, tvBlockSize, tvGradAccum, tvMaxIters;
    private Button btnCalculate, btnValidate, btnAdvancedCalc, btnMethodInfo;
    private AutoCompleteTextView spinnerModelType, spinnerPreset;

    private JSONObject configRules;
    private ModelMathObject model;
    private String currentModelType;
    private String currentPreset = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_calculate);
        initializeViews();
        loadConfigAndQuestions();
        setupSpinners();
        setupButtonListeners();
    }

    private void initializeViews() {
        // Input fields
        etDataSize = findViewById(R.id.et_data_size);
        etCorrectAnswers = findViewById(R.id.et_correct_answers);
        etTotalQuestions = findViewById(R.id.et_total_questions);
        etGpuMemory = findViewById(R.id.et_gpu_memory);
        etCurrentLoss = findViewById(R.id.et_current_loss);
        etValLoss = findViewById(R.id.et_val_loss);
        etMethodName = findViewById(R.id.et_method_name);

        // Result views
        tvBatchSize = findViewById(R.id.tv_batch_size);
        tvLoss = findViewById(R.id.tv_loss);
        tvNHead = findViewById(R.id.tv_n_head);
        tvNLayer = findViewById(R.id.tv_n_layer);
        tvResult = findViewById(R.id.tv_result);
        tvGpuUtil = findViewById(R.id.tv_gpu_util);
        tvLossAnalysis = findViewById(R.id.tv_loss_analysis);
        tvGradientStatus = findViewById(R.id.tv_gradient_status);
        tvMethodInfo = findViewById(R.id.tv_method_info);
        tvNEmb = findViewById(R.id.tv_n_emb);
        tvLearningRate = findViewById(R.id.tv_learning_rate);
        tvDropout = findViewById(R.id.tv_dropout);
        tvBlockSize = findViewById(R.id.tv_block_size);
        tvGradAccum = findViewById(R.id.tv_grad_accum);
        tvMaxIters = findViewById(R.id.tv_max_iters);

        // Buttons
        btnCalculate = findViewById(R.id.btn_calculate);
        btnValidate = findViewById(R.id.btn_validate);
        btnAdvancedCalc = findViewById(R.id.btn_advanced_calc);
        btnMethodInfo = findViewById(R.id.btn_method_info);

        // Spinners
        spinnerModelType = findViewById(R.id.spinner_model_type);
        spinnerPreset = findViewById(R.id.spinner_preset);
    }

    private void loadConfigAndQuestions() {
        try (InputStream is = getAssets().open("dataAi/MathQuest/ai_calculate.json");
             Scanner scanner = new Scanner(is).useDelimiter("\\A")) {
            String json = scanner.hasNext() ? scanner.next() : "";
            configRules = new JSONObject(json);
            currentModelType = configRules.optString("default_type", "NLP");
            Log.d("ConfigLoad", "Configuration loaded successfully");
        } catch (Exception e) {
            Log.e("ConfigLoad", "Error loading config: " + e.getMessage());
            Toast.makeText(this, "Konfiqurasiya yüklənərkən xəta baş verdi", Toast.LENGTH_LONG).show();
        }
    }

    private void setupSpinners() {
        try {
            // Model Type Spinner
            JSONArray availableTypes = configRules.getJSONArray("available_types");
            List<String> modelTypes = new ArrayList<>();
            for (int i = 0; i < availableTypes.length(); i++) {
                modelTypes.add(availableTypes.getString(i));
            }

            ArrayAdapter<String> modelTypeAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_dropdown_item_1line, modelTypes);
            spinnerModelType.setAdapter(modelTypeAdapter);
            spinnerModelType.setText(currentModelType, false);

            spinnerModelType.setOnItemClickListener((parent, view, position, id) -> {
                currentModelType = modelTypes.get(position);
                currentPreset = null;
                updatePresetSpinner();
                resetCalculationResults();
            });

            // Preset Spinner
            updatePresetSpinner();
            spinnerPreset.setOnItemClickListener((parent, view, position, id) -> {
                currentPreset = (String) parent.getItemAtPosition(position);
                applyPresetSettings();
            });

        } catch (JSONException e) {
            Log.e("SpinnerSetup", "Error setting up spinners", e);
            Toast.makeText(this, "Spinner configuration error", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetCalculationResults() {
        tvBatchSize.setText("Batch: -");
        tvLoss.setText("Loss: -");
        tvNHead.setText("Heads: -");
        tvNLayer.setText("Layers: -");
        tvNEmb.setText("Embed Dim: -");
        tvLearningRate.setText("LR: -");
        tvDropout.setText("Dropout: -");
        tvBlockSize.setText("Block Size: -");
        tvGradAccum.setText("Grad Steps: -");
        tvMaxIters.setText("Max Iters: -");
        tvResult.setText("Select parameters and calculate");
        tvResult.setTextColor(ContextCompat.getColor(this, R.color.text_default));
    }

    private void updatePresetSpinner() {
        try {
            JSONObject presets = configRules.getJSONObject("presets");
            if (presets.has(currentModelType)) {
                JSONObject typePresets = presets.getJSONObject(currentModelType);

                List<String> presetNames = new ArrayList<>();
                Iterator<String> keys = typePresets.keys();
                while (keys.hasNext()) {
                    presetNames.add(keys.next());
                }

                ArrayAdapter<String> presetAdapter = new ArrayAdapter<>(
                        this, android.R.layout.simple_dropdown_item_1line, presetNames);
                spinnerPreset.setAdapter(presetAdapter);
                spinnerPreset.setText("", false);
            }
        } catch (JSONException e) {
            Log.e("PresetSpinner", "Error updating preset spinner", e);
        }
    }

    private void applyPresetSettings() {
        try {
            if (currentPreset == null || currentPreset.isEmpty()) return;

            JSONObject preset = configRules.getJSONObject("presets")
                    .getJSONObject(currentModelType)
                    .getJSONObject(currentPreset);

            // Apply preset values to input fields
            etDataSize.setText(String.valueOf(preset.optInt("n_layer") * 1000));

            // Update displayed values
            tvNLayer.setText(String.format("Layers: %d", preset.optInt("n_layer")));
            tvNHead.setText(String.format("Heads: %d", preset.optInt("n_head")));
            tvNEmb.setText(String.format("Embed Dim: %d", preset.optInt("n_embd")));

            Toast.makeText(this, String.format("%s preset applied", currentPreset),
                    Toast.LENGTH_SHORT).show();

        } catch (JSONException e) {
            Log.e("ApplyPreset", "Error applying preset", e);
            Toast.makeText(this, "Error applying preset", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupButtonListeners() {
        btnCalculate.setOnClickListener(v -> calculate());
        btnValidate.setOnClickListener(v -> validate());
        btnAdvancedCalc.setOnClickListener(v -> advancedCalculate());
        btnMethodInfo.setOnClickListener(v -> showMethodInfo());
    }

    private boolean validateInputs() {
        if (etDataSize.getText().toString().isEmpty() ||
                etCorrectAnswers.getText().toString().isEmpty() ||
                etTotalQuestions.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            double totalQuestions = Double.parseDouble(etTotalQuestions.getText().toString());
            double correctAnswers = Double.parseDouble(etCorrectAnswers.getText().toString());

            if (totalQuestions <= 0 || correctAnswers < 0 || correctAnswers > totalQuestions) {
                Toast.makeText(this, "Invalid accuracy values", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void calculate() {
        if (!validateInputs()) return;

        try {
            double dataSize = Double.parseDouble(etDataSize.getText().toString());
            double correctAnswers = Double.parseDouble(etCorrectAnswers.getText().toString());
            double totalQuestions = Double.parseDouble(etTotalQuestions.getText().toString());

            model = new ModelMathObject(configRules, currentModelType, dataSize,
                    correctAnswers, totalQuestions);
            updateBasicResults();

        } catch (Exception e) {
            Toast.makeText(this, "Calculation error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("CalculateError", "Error in calculation", e);
        }
    }

    private void updateBasicResults() {
        tvBatchSize.setText(String.format("Batch: %.0f", model.getBatchSize()));
        tvLoss.setText(String.format("Loss: %.4f", model.getLoss()));
        tvNHead.setText(String.format("Heads: %.0f", model.getNHead()));
        tvNLayer.setText(String.format("Layers: %.0f", model.getNLayer()));
        tvNEmb.setText(String.format("Embed Dim: %.0f", model.getNEmb()));
        tvLearningRate.setText(String.format("LR: %.1e", model.getLearningRate()));
        tvDropout.setText(String.format("Dropout: %.2f", model.getDropout()));
        tvBlockSize.setText(String.format("Block Size: %.0f", model.getBlockSize()));
        tvGradAccum.setText(String.format("Grad Steps: %.0f", model.getGradientAccumulationSteps()));
        tvMaxIters.setText(String.format("Max Iters: %.0f", model.getMaxIters()));
        tvResult.setText("Ready for validation");
        tvResult.setTextColor(ContextCompat.getColor(this, R.color.text_default));
    }

    private void validate() {
        if (model == null) {
            Toast.makeText(this, "Calculate parameters first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject typeRules = configRules.getJSONObject("type_specific_rules")
                    .getJSONObject(currentModelType);

            StringBuilder result = new StringBuilder("Validation Results:\n\n");
            boolean allValid = true;

            // Validate architecture parameters
            JSONObject archRules = typeRules.getJSONObject("model_architecture");
            allValid &= validateParameter("N Head", model.getNHead(),
                    archRules.getJSONObject("n_head"), result);
            allValid &= validateParameter("N Layer", model.getNLayer(),
                    archRules.getJSONObject("n_layer"), result);

            // Validate training parameters
            JSONObject trainRules = typeRules.getJSONObject("training_parameters");
            allValid &= validateParameter("Batch Size", model.getBatchSize(),
                    trainRules.getJSONObject("batch_size"), result);

            result.append("\n").append(allValid ? "✓ ALL VALID" : "✗ SOME INVALID");
            tvResult.setText(result.toString());
            tvResult.setTextColor(ContextCompat.getColor(this,
                    allValid ? R.color.valid : R.color.invalid));

        } catch (JSONException e) {
            Toast.makeText(this, "Configuration error", Toast.LENGTH_SHORT).show();
            Log.e("Validate", "JSON error: ", e);
        }
    }

    private boolean validateParameter(String name, double value,
                                      JSONObject rule, StringBuilder output)
            throws JSONException {
        double min = rule.getDouble("minimum");
        double max = rule.getDouble("maximum");
        boolean isValid = value >= min && value <= max;

        output.append(String.format("%s: %.2f (Range %.2f-%.2f) %s\n",
                name, value, min, max, isValid ? "✓" : "✗"));
        return isValid;
    }

    private void advancedCalculate() {
        if (model == null) {
            Toast.makeText(this, "Calculate basic parameters first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (etGpuMemory.getText().toString().isEmpty() ||
                    etCurrentLoss.getText().toString().isEmpty() ||
                    etValLoss.getText().toString().isEmpty()) {
                Toast.makeText(this, "Please fill all advanced fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double gpuMemory = Double.parseDouble(etGpuMemory.getText().toString());
            double currentLoss = Double.parseDouble(etCurrentLoss.getText().toString());
            double valLoss = Double.parseDouble(etValLoss.getText().toString());

            model.updateAdvancedParams(gpuMemory, currentLoss, valLoss);

            tvGpuUtil.setText(String.format("GPU Use: %.1f%%", model.getGpuUtilization()));
            tvLossAnalysis.setText(String.format("Loss Ratio: %.2f", model.getLossRatio()));
            tvGradientStatus.setText(model.getGradientStatus());

            showRecommendations();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Advanced calculation failed", Toast.LENGTH_LONG).show();
            Log.e("AdvancedCalc", "Error: ", e);
        }
    }

    private void showMethodInfo() {
        try {
            String methodName = etMethodName.getText().toString().trim();
            if (methodName.isEmpty()) {
                Toast.makeText(this, "Please enter a method name", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject methods = configRules.getJSONObject("common_parameters")
                    .optJSONObject("pytorch_methods");
            if (methods != null) {
                String description = methods.optString(methodName, "Method not found");
                tvMethodInfo.setText(description);
            } else {
                tvMethodInfo.setText("Method info not available");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Cannot load method info", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRecommendations() {
        try {
            StringBuilder advice = new StringBuilder("Recommendations:\n\n");

            // Dynamic recommendations based on model state
            if (model.getGpuUtilization() < 70) {
                advice.append("- Increase batch size for better GPU utilization\n");
            }
            if (model.getLossRatio() > 1.5) {
                advice.append("- Model may be too simple for the data\n");
            }

            // Type-specific recommendations
            JSONObject typeRules = configRules.getJSONObject("type_specific_rules")
                    .getJSONObject(currentModelType);
            if (typeRules.has("recommendations")) {
                advice.append(typeRules.getJSONObject("recommendations")
                        .getString("general"));
            }

            Toast.makeText(this, advice.toString(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Cannot load recommendations", Toast.LENGTH_SHORT).show();
        }
    }
}