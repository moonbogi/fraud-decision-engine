package com.example.decision.service;

import com.example.decision.model.Transaction;
import com.example.decision.model.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ML-based risk scoring service.
 * For MVP, uses simple weighted heuristics. Can be replaced with ONNX/TensorFlow model.
 */
@Service
@Slf4j
public class MLScoringService {

    private static final double WEIGHT_AMOUNT = 0.25;
    private static final double WEIGHT_VELOCITY = 0.30;
    private static final double WEIGHT_DEVICE = 0.20;
    private static final double WEIGHT_LOCATION = 0.15;
    private static final double WEIGHT_MERCHANT = 0.10;

    public double calculateRiskScore(Transaction txn, UserProfile profile, 
                                     int velocity1m, int velocity5m) {
        double score = 0.0;

        // Feature 1: Amount deviation
        score += calculateAmountScore(txn.amount(), profile.averageTransactionAmount()) * WEIGHT_AMOUNT;

        // Feature 2: Velocity
        score += calculateVelocityScore(velocity1m, velocity5m) * WEIGHT_VELOCITY;

        // Feature 3: Device trust
        score += calculateDeviceScore(txn.deviceId(), profile) * WEIGHT_DEVICE;

        // Feature 4: Location
        score += calculateLocationScore(txn.location(), profile) * WEIGHT_LOCATION;

        // Feature 5: Merchant familiarity
        score += calculateMerchantScore(txn.merchant(), profile) * WEIGHT_MERCHANT;

        // Normalize to 0-100
        score = Math.min(100.0, Math.max(0.0, score * 100));

        log.debug("Calculated risk score: {} for transaction: {}", score, txn.transactionId());
        return round(score, 2);
    }

    private double calculateAmountScore(BigDecimal amount, BigDecimal avgAmount) {
        if (avgAmount == null || avgAmount.compareTo(BigDecimal.ZERO) == 0) {
            // No history: score based on absolute amount
            return amount.compareTo(new BigDecimal("1000")) > 0 ? 0.5 : 0.1;
        }

        BigDecimal ratio = amount.divide(avgAmount, 4, RoundingMode.HALF_UP);
        
        if (ratio.compareTo(new BigDecimal("10")) > 0) return 1.0;
        if (ratio.compareTo(new BigDecimal("5")) > 0) return 0.8;
        if (ratio.compareTo(new BigDecimal("3")) > 0) return 0.5;
        if (ratio.compareTo(new BigDecimal("2")) > 0) return 0.3;
        return 0.1;
    }

    private double calculateVelocityScore(int velocity1m, int velocity5m) {
        double score = 0.0;

        if (velocity1m >= 5) return 1.0;
        if (velocity1m >= 3) score += 0.6;
        else if (velocity1m >= 2) score += 0.3;

        if (velocity5m >= 15) score += 0.4;
        else if (velocity5m >= 10) score += 0.2;

        return Math.min(1.0, score);
    }

    private double calculateDeviceScore(String deviceId, UserProfile profile) {
        return profile.isNewDevice(deviceId) ? 0.8 : 0.1;
    }

    private double calculateLocationScore(String location, UserProfile profile) {
        if (location == null) return 0.0;
        return profile.isUnusualLocation(location) ? 0.7 : 0.1;
    }

    private double calculateMerchantScore(String merchant, UserProfile profile) {
        return profile.frequentMerchants().contains(merchant) ? 0.0 : 0.4;
    }

    private double round(double value, int places) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Future: Load ONNX model and perform inference.
     */
    public void loadModel(String modelPath) {
        log.info("Model loading not implemented yet. Using heuristic scoring.");
        // TODO: Integrate ONNX Runtime or TensorFlow Java
    }
}
