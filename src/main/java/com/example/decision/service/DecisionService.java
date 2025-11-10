package com.example.decision.service;

import com.example.decision.model.*;
import com.example.decision.repository.DecisionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Core decision orchestration service.
 * Coordinates rule evaluation, feature enrichment, ML scoring, and audit.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DecisionService {

    private final RuleEngineService ruleEngineService;
    private final FeatureService featureService;
    private final MLScoringService mlScoringService;
    private final DecisionRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Transactional
    public Decision evaluate(Transaction transaction) {
        long startTime = System.nanoTime();
        String correlationId = transaction.transactionId();
        
        log.info("Starting decision evaluation for transaction: {}, user: {}", 
                 correlationId, transaction.userId());

        try {
            // Step 1: Enrich with cached features
            UserProfile profile = featureService.getUserProfile(transaction.userId());
            int velocity1m = featureService.getVelocity(transaction.userId(), Duration.ofMinutes(1));
            int velocity5m = featureService.getVelocity(transaction.userId(), Duration.ofMinutes(5));
            
            log.debug("Features enriched - velocity1m: {}, velocity5m: {}, isNewDevice: {}", 
                     velocity1m, velocity5m, profile.isNewDevice(transaction.deviceId()));

            // Step 2: Execute rules
            RuleResult ruleResult = ruleEngineService.evaluate(
                transaction, profile, velocity1m, velocity5m
            );

            // Step 3: Calculate ML risk score
            double riskScore = mlScoringService.calculateRiskScore(
                transaction, profile, velocity1m, velocity5m
            );

            // Step 4: Make final decision
            DecisionOutcome outcome = determineOutcome(ruleResult, riskScore);

            // Step 5: Record metrics
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            recordMetrics(outcome, latencyMs, riskScore);

            // Step 6: Create decision
            Decision decision = new Decision(
                transaction.transactionId(),
                transaction.userId(),
                outcome,
                riskScore,
                ruleResult.getReasonCodes(),
                ruleResult.getRuleVersion(),
                latencyMs
            );

            // Step 7: Persist to audit trail
            persistDecision(decision);

            // Step 8: Update velocity counters
            featureService.incrementVelocity(transaction.userId());

            // Step 9: Publish result to output topic
            publishDecision(decision);

            log.info("Decision completed: {} for transaction: {} (score: {}, latency: {}ms)", 
                     outcome, correlationId, riskScore, latencyMs);

            return decision;

        } catch (Exception e) {
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            log.error("Error evaluating transaction: {}, latency: {}ms", correlationId, latencyMs, e);
            meterRegistry.counter("decision.errors", "transaction", correlationId).increment();
            throw new DecisionEvaluationException("Failed to evaluate transaction: " + correlationId, e);
        }
    }

    private DecisionOutcome determineOutcome(RuleResult ruleResult, double riskScore) {
        // Rule engine has priority
        if (ruleResult.getSuggestedOutcome() == DecisionOutcome.REJECT) {
            return DecisionOutcome.REJECT;
        }
        
        // High risk score overrides
        if (riskScore >= 80.0) {
            return DecisionOutcome.REJECT;
        } else if (riskScore >= 50.0) {
            return DecisionOutcome.REVIEW;
        }
        
        // Use rule suggestion or approve
        return ruleResult.getSuggestedOutcome() != null 
            ? ruleResult.getSuggestedOutcome() 
            : DecisionOutcome.APPROVE;
    }

    private void recordMetrics(DecisionOutcome outcome, long latencyMs, double riskScore) {
        Timer.builder("decision.latency")
            .tag("outcome", outcome.name())
            .register(meterRegistry)
            .record(latencyMs, TimeUnit.MILLISECONDS);

        meterRegistry.counter("decision.count", "outcome", outcome.name()).increment();
        
        meterRegistry.gauge("decision.risk_score", riskScore);
    }

    private void persistDecision(Decision decision) {
        DecisionEntity entity = DecisionEntity.builder()
            .transactionId(decision.transactionId())
            .userId(decision.userId())
            .outcome(decision.outcome())
            .riskScore(decision.riskScore())
            .reasonCodes(String.join(",", decision.reasonCodes()))
            .ruleVersion(decision.ruleVersion())
            .latencyMs(decision.latencyMs())
            .timestamp(decision.timestamp())
            .build();
        
        repository.save(entity);
    }

    private void publishDecision(Decision decision) {
        kafkaTemplate.send("decision-results", decision.transactionId(), decision)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish decision for transaction: {}", 
                             decision.transactionId(), ex);
                    meterRegistry.counter("decision.publish.errors").increment();
                } else {
                    log.debug("Published decision to Kafka: {}", decision.transactionId());
                }
            });
    }
}
