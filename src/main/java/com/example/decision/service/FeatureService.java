package com.example.decision.service;

import com.example.decision.model.UserProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Feature enrichment service using Redis for caching.
 * Provides user profiles and velocity tracking.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeatureService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    private static final String PROFILE_KEY_PREFIX = "profile:";
    private static final String VELOCITY_KEY_PREFIX = "velocity:";
    private static final long PROFILE_TTL_HOURS = 1;

    public UserProfile getUserProfile(String userId) {
        String key = PROFILE_KEY_PREFIX + userId;
        
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            
            if (cached != null) {
                meterRegistry.counter("feature.cache.hit", "type", "profile").increment();
                log.debug("Cache hit for user profile: {}", userId);
                return objectMapper.convertValue(cached, UserProfile.class);
            }
            
            meterRegistry.counter("feature.cache.miss", "type", "profile").increment();
            log.debug("Cache miss for user profile: {}, creating default", userId);
            
            // Create default profile (in production, fetch from database)
            UserProfile profile = createDefaultProfile(userId);
            cacheUserProfile(userId, profile);
            return profile;
            
        } catch (Exception e) {
            log.error("Error retrieving user profile for: {}", userId, e);
            meterRegistry.counter("feature.errors", "type", "profile").increment();
            return createDefaultProfile(userId);
        }
    }

    public void cacheUserProfile(String userId, UserProfile profile) {
        String key = PROFILE_KEY_PREFIX + userId;
        try {
            redisTemplate.opsForValue().set(key, profile, PROFILE_TTL_HOURS, TimeUnit.HOURS);
            log.debug("Cached user profile: {}", userId);
        } catch (Exception e) {
            log.error("Error caching user profile: {}", userId, e);
        }
    }

    public int getVelocity(String userId, Duration window) {
        String key = VELOCITY_KEY_PREFIX + userId;
        long windowSeconds = window.getSeconds();
        long cutoffTime = Instant.now().minusSeconds(windowSeconds).toEpochMilli();
        
        try {
            // Use Redis sorted set with timestamp scores
            Long count = redisTemplate.opsForZSet().count(key, cutoffTime, Double.MAX_VALUE);
            
            int velocity = count != null ? count.intValue() : 0;
            log.debug("Velocity for user {} in window {}: {}", userId, window, velocity);
            
            meterRegistry.gauge("feature.velocity", velocity);
            return velocity;
            
        } catch (Exception e) {
            log.error("Error calculating velocity for user: {}", userId, e);
            meterRegistry.counter("feature.errors", "type", "velocity").increment();
            return 0;
        }
    }

    public void incrementVelocity(String userId) {
        String key = VELOCITY_KEY_PREFIX + userId;
        long timestamp = System.currentTimeMillis();
        
        try {
            // Add current timestamp to sorted set
            redisTemplate.opsForZSet().add(key, String.valueOf(timestamp), timestamp);
            
            // Set expiration (keep last 10 minutes of data)
            redisTemplate.expire(key, 10, TimeUnit.MINUTES);
            
            // Clean old entries (older than 10 minutes)
            long tenMinutesAgo = Instant.now().minusSeconds(600).toEpochMilli();
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, tenMinutesAgo);
            
            log.debug("Incremented velocity for user: {}", userId);
            
        } catch (Exception e) {
            log.error("Error incrementing velocity for user: {}", userId, e);
        }
    }

    private UserProfile createDefaultProfile(String userId) {
        // In production, this would query a user database
        Set<String> trustedDevices = new HashSet<>();
        Set<String> frequentMerchants = new HashSet<>();
        
        return new UserProfile(
            userId,
            new BigDecimal("100.00"),  // Default average
            "US",  // Default home location
            trustedDevices,
            frequentMerchants,
            0,
            false
        );
    }

    /**
     * Simulate updating profile after transaction (for demo purposes).
     */
    public void updateProfile(String userId, String deviceId, String merchant, BigDecimal amount) {
        UserProfile current = getUserProfile(userId);
        
        Set<String> devices = new HashSet<>(current.trustedDevices());
        devices.add(deviceId);
        
        Set<String> merchants = new HashSet<>(current.frequentMerchants());
        merchants.add(merchant);
        
        int newCount = current.totalTransactionCount() + 1;
        BigDecimal newAvg = calculateNewAverage(
            current.averageTransactionAmount(), 
            amount, 
            newCount
        );
        
        UserProfile updated = new UserProfile(
            userId,
            newAvg,
            current.homeLocation(),
            devices,
            merchants,
            newCount,
            current.isPremiumCustomer()
        );
        
        cacheUserProfile(userId, updated);
    }

    private BigDecimal calculateNewAverage(BigDecimal currentAvg, BigDecimal newAmount, int count) {
        if (currentAvg == null || count <= 1) {
            return newAmount;
        }
        BigDecimal total = currentAvg.multiply(BigDecimal.valueOf(count - 1)).add(newAmount);
        return total.divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP);
    }
}
