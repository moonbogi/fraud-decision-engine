package com.example.decision.model;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Cached user profile for feature enrichment.
 */
public record UserProfile(
    String userId,
    BigDecimal averageTransactionAmount,
    String homeLocation,
    Set<String> trustedDevices,
    Set<String> frequentMerchants,
    int totalTransactionCount,
    boolean isPremiumCustomer
) {
    public boolean isNewDevice(String deviceId) {
        return !trustedDevices.contains(deviceId);
    }
    
    public boolean isUnusualLocation(String location) {
        return homeLocation != null && !homeLocation.equalsIgnoreCase(location);
    }
}
