package com.ali.aimain;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;
import java.util.Stack;

public class ModelMathObject {
    private final JSONObject config;
    private final String modelType;

    // Parameters actually used in calculations and UI
    private double batchSize;
    private double loss;
    private double nHead;
    private double nLayer;
    private double nEmb;
    private double dropout;
    private double blockSize;
    private double gradientAccumulationSteps;
    private double learningRate;
    private double maxIters;
    private boolean useRotary;
    private boolean bias;

    // Advanced parameters used in calculations
    private double gpuUtilization;
    private double lossRatio;
    private String gradientStatus;

    public ModelMathObject(JSONObject config, String modelType,
                           double dataSize, double correctAnswers,
                           double totalQuestions) {
        this.config = config;
        this.modelType = modelType;
        calculateAllParameters(dataSize, correctAnswers, totalQuestions);
    }

    private void calculateAllParameters(double dataSize,
                                        double correctAnswers,
                                        double totalQuestions) {
        try {
            // Calculate accuracy-based parameters
            double accuracy = correctAnswers / totalQuestions;
            this.loss = 1.0 - accuracy;

            // Get type-specific rules
            JSONObject typeRules = config.getJSONObject("type_specific_rules")
                    .getJSONObject(modelType);
            JSONObject archRules = typeRules.getJSONObject("model_architecture");
            JSONObject trainRules = typeRules.getJSONObject("training_parameters");

            // Calculate model architecture parameters
            this.nHead = calculateParameter("n_head", archRules, dataSize);
            this.nLayer = calculateParameter("n_layer", archRules, dataSize);
            this.useRotary = archRules.optBoolean("use_rotary", false);
            this.bias = archRules.optBoolean("bias", true);

            // Calculate common parameters
            JSONObject commonParams = config.getJSONObject("common_parameters");
            this.nEmb = calculateEmbeddingDimension(dataSize, commonParams.getJSONObject("n_embd"));
            this.dropout = calculateDropout(dataSize, commonParams.getJSONObject("dropout"));
            this.learningRate = calculateLearningRate(dataSize, commonParams.getJSONObject("learning_rate"));

            // Calculate training parameters
            this.batchSize = calculateParameter("batch_size", trainRules, dataSize);
            this.blockSize = trainRules.optDouble("block_size", 1024);
            this.gradientAccumulationSteps = calculateGradientSteps(dataSize);
            this.maxIters = calculateMaxIters(dataSize);

            logCalculatedParameters();

        } catch (JSONException e) {
            Log.e("ModelError", "Configuration error: ", e);
            throw new RuntimeException("Calculation failed due to configuration error", e);
        } catch (Exception e) {
            Log.e("ModelError", "Calculation error: ", e);
            throw new RuntimeException("Calculation failed", e);
        }
    }

    private double calculateParameter(String paramName, JSONObject rules, double dataSize)
            throws JSONException {
        JSONObject paramRules = rules.getJSONObject(paramName);
        double baseValue = evaluateFormula(paramRules.getString("base_formula"), dataSize);
        double min = paramRules.getDouble("minimum");
        double max = paramRules.getDouble("maximum");
        return Math.min(max, Math.max(min, Math.round(baseValue)));
    }

    private double evaluateFormula(String formula, double dataSize) {
        try {
            formula = formula.replace("data_size", String.valueOf(dataSize))
                    .replace("log", "Math.log")
                    .replace("sqrt", "Math.sqrt")
                    .replace("round", "Math.round")
                    .replace("max", "Math.max")
                    .replace("min", "Math.min");

            return evaluateMathExpression(formula);
        } catch (Exception e) {
            Log.e("FormulaEval", "Error evaluating formula: " + formula, e);
            return 0;
        }
    }

    private double evaluateMathExpression(String expression) {
        expression = expression.replaceAll("\\s+", "");
        Stack<Double> operands = new Stack<>();
        Stack<Character> operators = new Stack<>();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                StringBuilder numStr = new StringBuilder();
                while (i < expression.length() &&
                        (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) {
                    numStr.append(expression.charAt(i++));
                }
                i--;
                operands.push(Double.parseDouble(numStr.toString()));
            }
            else if (c == '(') {
                operators.push(c);
            }
            else if (c == ')') {
                while (operators.peek() != '(') {
                    operands.push(applyOperator(operators.pop(), operands.pop(), operands.pop()));
                }
                operators.pop();
            }
            else if (isOperator(c)) {
                while (!operators.empty() && hasPrecedence(c, operators.peek())) {
                    operands.push(applyOperator(operators.pop(), operands.pop(), operands.pop()));
                }
                operators.push(c);
            }
        }

        while (!operators.empty()) {
            operands.push(applyOperator(operators.pop(), operands.pop(), operands.pop()));
        }

        return operands.pop();
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }

    private boolean hasPrecedence(char op1, char op2) {
        if (op2 == '(' || op2 == ')') return false;
        if ((op1 == '^') && (op2 == '*' || op2 == '/' || op2 == '+' || op2 == '-')) return false;
        if ((op1 == '*' || op1 == '/') && (op2 == '+' || op2 == '-')) return false;
        return true;
    }

    private double applyOperator(char op, double b, double a) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/':
                if (b == 0) throw new ArithmeticException("Division by zero");
                return a / b;
            case '^': return Math.pow(a, b);
        }
        return 0;
    }

    private double calculateEmbeddingDimension(double dataSize, JSONObject embRules)
            throws JSONException {
        double baseEmb = calculateParameter("n_embd", embRules, dataSize);
        return baseEmb - (baseEmb % nHead);
    }

    private double calculateDropout(double dataSize, JSONObject dropoutRules)
            throws JSONException {
        String formula = dropoutRules.getString("base_formula");
        return evaluateFormula(formula, dataSize);
    }

    private double calculateLearningRate(double dataSize, JSONObject lrRules)
            throws JSONException {
        return calculateParameter("learning_rate", lrRules, dataSize);
    }

    private double calculateGradientSteps(double dataSize) {
        return Math.max(1, Math.min(8, (int)(5000000 / dataSize)));
    }

    private double calculateMaxIters(double dataSize) {
        return Math.max(10000, Math.min(100000, (int)(dataSize / 1000)));
    }

    private void logCalculatedParameters() {
        Log.d("ModelConfig", String.format(Locale.US,
                "Model Configuration [%s]:\n" +
                        "Layers: %.0f\nHeads: %.0f\nEmbedding: %.0f\n" +
                        "Batch: %.0f\nBlock: %.0f\nGradSteps: %.0f\n" +
                        "LR: %.1e\nDropout: %.2f\nMaxIters: %.0f\n" +
                        "Rotary: %b\nBias: %b",
                modelType, nLayer, nHead, nEmb, batchSize, blockSize,
                gradientAccumulationSteps, learningRate, dropout, maxIters,
                useRotary, bias));
    }

    public void updateAdvancedParams(double gpuMemory, double currentLoss, double valLoss) {
        try {
            double modelSizeMB = (nEmb * nLayer * 16 * 4) / (1024 * 1024);
            double batchMemMB = modelSizeMB * batchSize * blockSize;
            this.gpuUtilization = Math.min(100, (batchMemMB / (gpuMemory * 1024)) * 100);
            this.lossRatio = valLoss / currentLoss;

            if (lossRatio > 1.5) {
                this.gradientStatus = "Warning: Possible overfitting";
            } else if (lossRatio < 0.8) {
                this.gradientStatus = "Warning: Possible underfitting";
            } else {
                this.gradientStatus = "Good: Healthy training";
            }

            Log.d("AdvancedParams", String.format(Locale.US,
                    "GPU Utilization: %.1f%%\nLoss Ratio: %.2f\nStatus: %s",
                    gpuUtilization, lossRatio, gradientStatus));

        } catch (Exception e) {
            throw new RuntimeException("Advanced calculations failed", e);
        }
    }

    // Only keep getters for parameters that are actually used
    public double getBatchSize() { return batchSize; }
    public double getLoss() { return loss; }
    public double getNHead() { return nHead; }
    public double getNLayer() { return nLayer; }
    public double getNEmb() { return nEmb; }
    public double getDropout() { return dropout; }
    public double getBlockSize() { return blockSize; }
    public double getGradientAccumulationSteps() { return gradientAccumulationSteps; }
    public double getLearningRate() { return learningRate; }
    public double getMaxIters() { return maxIters; }
    public boolean getUseRotary() { return useRotary; }
    public boolean getBias() { return bias; }
    public double getGpuUtilization() { return gpuUtilization; }
    public double getLossRatio() { return lossRatio; }
    public String getGradientStatus() { return gradientStatus; }
}