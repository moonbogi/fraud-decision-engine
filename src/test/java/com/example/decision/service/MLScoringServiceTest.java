package com.example.decision.service;

import com.example.decision.model.Transaction;
import com.example.decision.model.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MLScoringServiceTest {

    private MLScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new MLScoringService();
    }

    @Test
    void shouldReturnLowScoreForNormalTransaction() {
        // Given
        Transaction transaction = createTransaction("100.00");
        UserProfile profile = createProfile("100.00", false, false);

        // When
        double score = scoringService.calculateRiskScore(transaction, profile, 0, 0);

        // Then
        assertThat(score).isLessThan(30.0);
    }

    @Test
    void shouldReturnHighScoreForNewDeviceAndUnusualLocation() {
        // Given
        Transaction transaction = createTransaction("500.00");
        UserProfile profile = createProfile("100.00", true, true);

        // When
        double score = scoringService.calculateRiskScore(transaction, profile, 0, 0);

        // Then
        assertThat(score).isGreaterThan(50.0);
    }

    @Test
    void shouldReturnHighScoreForHighVelocity() {
        // Given
        Transaction transaction = createTransaction("100.00");
        UserProfile profile = createProfile("100.00", false, false);

        // When
        double score = scoringService.calculateRiskScore(transaction, profile, 5, 15);

        // Then
        assertThat(score).isGreaterThan(60.0);
    }

    @Test
    void shouldReturnHighScoreForLargeAmountDeviation() {
        // Given
        Transaction transaction = createTransaction("10000.00");
        UserProfile profile = createProfile("100.00", false, false);

        // When
        double score = scoringService.calculateRiskScore(transaction, profile, 0, 0);

        // Then
        assertThat(score).isGreaterThan(70.0);
    }

    private Transaction createTransaction(String amount) {
        return new Transaction(
            "txn-test",
            "user-test",
            new BigDecimal(amount),
            "USD",
            "Merchant-A",
            "RETAIL",
            "device-001",
            "CA",
            Instant.now()
        );
    }

    private UserProfile createProfile(String avgAmount, boolean newDevice, boolean unusualLocation) {
        Set<String> devices = newDevice ? Set.of() : Set.of("device-001");
        String homeLocation = unusualLocation ? "US" : "CA";
        
        return new UserProfile(
            "user-test",
            new BigDecimal(avgAmount),
            homeLocation,
            devices,
            Set.of(),
            50,
            false
        );
    }
}
