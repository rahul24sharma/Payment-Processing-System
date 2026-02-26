package com.payment.fraud.ml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Advanced ML model using ONNX Runtime
 * 
 * To use this:
 * 1. Train model in Python (scikit-learn, TensorFlow, PyTorch)
 * 2. Export to ONNX format
 * 3. Load here using ONNX Runtime
 * 
 * For MVP, we'll use the simple logistic regression above
 */
@Component
@Slf4j
public class TensorFlowFraudModel {
    
    private static final String MODEL_PATH = "models/fraud_model.onnx";
    
    // Uncomment when you have a trained model
    /*
    private OrtSession session;
    private OrtEnvironment env;
    
    @PostConstruct
    public void loadModel() throws OrtException, IOException {
        env = OrtEnvironment.getEnvironment();
        
        Path modelPath = Paths.get(MODEL_PATH);
        if (!Files.exists(modelPath)) {
            log.warn("ML model not found at: {}", MODEL_PATH);
            return;
        }
        
        byte[] modelBytes = Files.readAllBytes(modelPath);
        session = env.createSession(modelBytes);
        
        log.info("ML model loaded successfully: {}", MODEL_PATH);
    }
    
    public BigDecimal predict(double[] features) throws OrtException {
        if (session == null) {
            log.warn("ML model not loaded, using fallback");
            return BigDecimal.valueOf(25); // Default safe score
        }
        
        // Create input tensor
        long[] shape = {1, features.length};
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, 
            new float[][]{convertToFloatArray(features)});
        
        Map<String, OnnxTensor> inputs = Map.of("input", inputTensor);
        
        // Run inference
        OrtSession.Result result = session.run(inputs);
        
        // Get output
        float[][] output = (float[][]) result.get(0).getValue();
        double probability = output[0][0];
        
        // Scale to 0-100
        double score = probability * 100;
        
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }
    
    private float[] convertToFloatArray(double[] doubles) {
        float[] floats = new float[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            floats[i] = (float) doubles[i];
        }
        return floats;
    }
    */
}