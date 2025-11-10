package com.example.decision.service;

import com.example.decision.model.*;
import com.example.decision.repository.DecisionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock
    private RuleEngineService ruleEngineService;

    @Mock
    private FeatureService featureService;

    @Mock
    private MLScoringService mlScoringService;

    @Mock
    private DecisionRepository repository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private MeterRegistry meterRegistry;
    private DecisionService decisionService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        decisionService = new DecisionService(
            ruleEngineService,
            featureService,
            mlScoringService,
            repository,
            kafkaTemplate,
            meterRegistry
        );
    }

    @Test
    void shouldApproveTransactionWithLowRiskScore() {
        // Given
        Transaction transaction = createTransaction("txn-001", "user-001", "100.00");
        UserProfile profile = createProfile("user-001", false);
        RuleResult ruleResult = createRuleResult(DecisionOutcome.APPROVE);

        when(featureService.getUserProfile("user-001")).thenReturn(profile);
        when(featureService.getVelocity(eq("user-001"), any())).thenReturn(0);
        when(ruleEngineService.evaluate(any(), any(), anyInt(), anyInt())).thenReturn(ruleResult);
        when(mlScoringService.calculateRiskScore(any(), any(), anyInt(), anyInt())).thenReturn(15.0);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(mock());

        // When
        Decision decision = decisionService.evaluate(transaction);

        // Then
        assertThat(decision.outcome()).isEqualTo(DecisionOutcome.APPROVE);
        assertThat(decision.riskScore()).isEqualTo(15.0);
        assertThat(decision.transactionId()).isEqualTo("txn-001");

        verify(repository).save(any(DecisionEntity.class));
        verify(kafkaTemplate).send(eq("decision-results"), eq("txn-001"), any());
    }

    @Test
    void shouldRejectTransactionWithHighRiskScore() {
        // Given
        Transaction transaction = createTransaction("txn-002", "user-002", "5000.00");
        UserProfile profile = createProfile("user-002", true);
        RuleResult ruleResult = createRuleResult(DecisionOutcome.REVIEW);

        when(featureService.getUserProfile("user-002")).thenReturn(profile);
        when(featureService.getVelocity(eq("user-002"), any())).thenReturn(0);
        when(ruleEngineService.evaluate(any(), any(), anyInt(), anyInt())).thenReturn(ruleResult);
        when(mlScoringService.calculateRiskScore(any(), any(), anyInt(), anyInt())).thenReturn(85.0);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(mock());

        // When
        Decision decision = decisionService.evaluate(transaction);

        // Then
        assertThat(decision.outcome()).isEqualTo(DecisionOutcome.REJECT);
        assertThat(decision.riskScore()).isEqualTo(85.0);
    }

    @Test
    void shouldRespectRuleEngineRejectDecision() {
        // Given
        Transaction transaction = createTransaction("txn-003", "user-003", "1000.00");
        UserProfile profile = createProfile("user-003", false);
        RuleResult ruleResult = createRuleResult(DecisionOutcome.REJECT);

        when(featureService.getUserProfile("user-003")).thenReturn(profile);
        when(featureService.getVelocity(eq("user-003"), any())).thenReturn(0);
        when(ruleEngineService.evaluate(any(), any(), anyInt(), anyInt())).thenReturn(ruleResult);
        when(mlScoringService.calculateRiskScore(any(), any(), anyInt(), anyInt())).thenReturn(30.0);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(mock());

        // When
        Decision decision = decisionService.evaluate(transaction);

        // Then
        assertThat(decision.outcome()).isEqualTo(DecisionOutcome.REJECT);
    }

    @Test
    void shouldRecordLatencyMetrics() {
        // Given
        Transaction transaction = createTransaction("txn-004", "user-004", "50.00");
        UserProfile profile = createProfile("user-004", false);
        RuleResult ruleResult = createRuleResult(DecisionOutcome.APPROVE);

        when(featureService.getUserProfile("user-004")).thenReturn(profile);
        when(featureService.getVelocity(eq("user-004"), any())).thenReturn(0);
        when(ruleEngineService.evaluate(any(), any(), anyInt(), anyInt())).thenReturn(ruleResult);
        when(mlScoringService.calculateRiskScore(any(), any(), anyInt(), anyInt())).thenReturn(10.0);
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(mock());

        // When
        Decision decision = decisionService.evaluate(transaction);

        // Then
        assertThat(decision.latencyMs()).isGreaterThanOrEqualTo(0);
        verify(featureService).incrementVelocity("user-004");
    }

    private Transaction createTransaction(String txnId, String userId, String amount) {
        return new Transaction(
            txnId,
            userId,
            new BigDecimal(amount),
            "USD",
            "Merchant-A",
            "RETAIL",
            "device-123",
            "US",
            Instant.now()
        );
    }

    private UserProfile createProfile(String userId, boolean newDevice) {
        Set<String> devices = newDevice ? Set.of() : Set.of("device-123");
        return new UserProfile(
            userId,
            new BigDecimal("100.00"),
            "US",
            devices,
            Set.of(),
            10,
            false
        );
    }

    private RuleResult createRuleResult(DecisionOutcome outcome) {
        RuleResult result = new RuleResult();
        result.setSuggestedOutcome(outcome);
        result.setRuleVersion("v1");
        result.addReasonCode("TEST_REASON");
        return result;
    }
}
