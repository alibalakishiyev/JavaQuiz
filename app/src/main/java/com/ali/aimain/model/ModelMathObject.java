package com.ali.aimain.model;


import org.json.JSONObject;
import org.json.JSONException;

public class ModelMathObject {

    private final JSONObject configRules;
    private final String modelType;
    private final double dataSize;
    private final double correctAnswers;
    private final double totalQuestions;

    // Computed basic parameters
    private double batchSize;
    private double loss;
    private double nHead;
    private double nLayer;
    private double nEmb;
    private double learningRate;
    private double dropout;
    private double blockSize;
    private double gradientAccumulationSteps;
    private double maxIters;

    // Advanced parameters
    private double gpuMemory;
    private double currentLoss;
    private double valLoss;
    private double gpuUtilization;
    private double lossRatio;
    private String gradientStatus;

    public ModelMathObject(JSONObject configRules, String modelType,
                           double dataSize, double correctAnswers, double totalQuestions)
            throws JSONException {
        this.configRules = configRules;
        this.modelType = modelType;
        this.dataSize = dataSize;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        compute();
    }

    private void compute() throws JSONException {
        double accuracy = totalQuestions > 0 ? correctAnswers / totalQuestions : 0.0;

        JSONObject typeRules = configRules
                .getJSONObject("type_specific_rules")
                .getJSONObject(modelType);
        JSONObject commonParams = configRules.getJSONObject("common_parameters");

        // n_head
        double rawNHead = Math.log(dataSize + 1) * 1.5;
        JSONObject nHeadRules = typeRules.getJSONObject("model_architecture").getJSONObject("n_head");
        nHead = clamp(Math.round(rawNHead), nHeadRules.getDouble("minimum"), nHeadRules.getDouble("maximum"));

        // n_layer
        double rawNLayer = Math.sqrt(dataSize) / 1000.0;
        JSONObject nLayerRules = typeRules.getJSONObject("model_architecture").getJSONObject("n_layer");
        nLayer = clamp(Math.round(rawNLayer), nLayerRules.getDouble("minimum"), nLayerRules.getDouble("maximum"));

        // n_embd
        double rawNEmb = (Math.round(dataSize / 10000.0)) * 64.0;
        JSONObject nEmbRules = commonParams.getJSONObject("n_embd");
        nEmb = clamp(rawNEmb, nEmbRules.getDouble("minimum"), nEmbRules.getDouble("maximum"));
        // Make divisible by n_head
        if (nHead > 0) {
            nEmb = Math.round(nEmb / nHead) * nHead;
            nEmb = clamp(nEmb, nEmbRules.getDouble("minimum"), nEmbRules.getDouble("maximum"));
        }

        // batch_size
        double rawBatch = 8.0 * Math.log(dataSize + 1);
        JSONObject batchRules = typeRules.getJSONObject("training_parameters").getJSONObject("batch_size");
        batchSize = clamp(Math.round(rawBatch), batchRules.getDouble("minimum"), batchRules.getDouble("maximum"));

        // block_size
        blockSize = typeRules.getJSONObject("training_parameters").optDouble("block_size", 1024);

        // learning_rate
        double rawLR = 6e-5 * (1 - Math.log(dataSize + 1) / 20.0);
        JSONObject lrRules = commonParams.getJSONObject("learning_rate");
        learningRate = clamp(rawLR, lrRules.getDouble("minimum"), lrRules.getDouble("maximum"));

        // dropout
        double rawDropout = Math.max(0.1, Math.min(0.3, 0.2 - (dataSize / 100_000_000.0)));
        dropout = Math.max(0.0, Math.min(0.5, rawDropout));

        // loss based on accuracy
        loss = accuracy > 0 ? -Math.log(accuracy) : Double.MAX_VALUE;

        // gradient accumulation steps
        double targetEffectiveBatch = 256.0;
        gradientAccumulationSteps = Math.max(1, Math.round(targetEffectiveBatch / batchSize));

        // max_iters
        maxIters = Math.max(1000, Math.round(dataSize / batchSize) * 10);
    }

    public void updateAdvancedParams(double gpuMemory, double currentLoss, double valLoss) {
        this.gpuMemory = gpuMemory;
        this.currentLoss = currentLoss;
        this.valLoss = valLoss;

        // GPU utilization: estimate based on model size and GPU memory
        double modelMemoryGB = (nEmb * nLayer * 4 * 4) / (1024.0 * 1024.0 * 1024.0);
        double activationMemoryGB = (batchSize * blockSize * nEmb * 4) / (1024.0 * 1024.0 * 1024.0);
        double totalMemory = modelMemoryGB + activationMemoryGB;
        gpuUtilization = gpuMemory > 0 ? Math.min(100.0, (totalMemory / gpuMemory) * 100.0) : 0.0;
        // Clamp to realistic range
        gpuUtilization = Math.max(10.0, Math.min(98.0, gpuUtilization * 15));

        // Loss ratio
        lossRatio = currentLoss > 0 ? valLoss / currentLoss : 1.0;

        // Gradient status
        if (lossRatio > 2.0) {
            gradientStatus = "Overfitting detected";
        } else if (lossRatio < 0.8) {
            gradientStatus = "Underfitting — increase model size";
        } else if (currentLoss < 0.1) {
            gradientStatus = "Converged";
        } else {
            gradientStatus = "Training stable";
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // Getters
    public double getBatchSize()                { return batchSize; }
    public double getLoss()                     { return loss; }
    public double getNHead()                    { return nHead; }
    public double getNLayer()                   { return nLayer; }
    public double getNEmb()                     { return nEmb; }
    public double getLearningRate()             { return learningRate; }
    public double getDropout()                  { return dropout; }
    public double getBlockSize()                { return blockSize; }
    public double getGradientAccumulationSteps(){ return gradientAccumulationSteps; }
    public double getMaxIters()                 { return maxIters; }
    public double getGpuUtilization()           { return gpuUtilization; }
    public double getLossRatio()                { return lossRatio; }
    public String getGradientStatus()           { return gradientStatus; }
}
