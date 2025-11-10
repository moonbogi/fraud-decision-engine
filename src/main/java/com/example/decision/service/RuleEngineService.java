package com.example.decision.service;

import com.example.decision.model.DecisionOutcome;
import com.example.decision.model.RuleResult;
import com.example.decision.model.Transaction;
import com.example.decision.model.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;

/**
 * Drools-based rule engine service.
 * Evaluates transactions against business rules.
 */
@Service
@Slf4j
public class RuleEngineService {

    @Value("${decision.rules.version:v1}")
    private String ruleVersion;

    private KieContainer kieContainer;

    @PostConstruct
    public void init() {
        try {
            KieServices kieServices = KieServices.Factory.get();
            kieContainer = kieServices.getKieClasspathContainer();
            log.info("Drools rule engine initialized with version: {}", ruleVersion);
        } catch (Exception e) {
            log.error("Failed to initialize Drools engine", e);
            // Fallback to simple rule engine
        }
    }

    public RuleResult evaluate(Transaction transaction, UserProfile profile, 
                               int velocity1m, int velocity5m) {
        RuleResult result = new RuleResult();
        result.setRuleVersion(ruleVersion);

        // For MVP, use simple Java-based rules
        // In production, this would use Drools KieSession
        applySimpleRules(transaction, profile, velocity1m, velocity5m, result);

        log.debug("Rule evaluation completed: {} rules fired, outcome: {}", 
                 result.getFiredRules().size(), result.getSuggestedOutcome());

        return result;
    }

    private void applySimpleRules(Transaction txn, UserProfile profile, 
                                  int velocity1m, int velocity5m, RuleResult result) {
        
        // Rule 1: High amount with new device -> REJECT
        if (txn.amount().compareTo(new BigDecimal("10000")) > 0 
            && profile.isNewDevice(txn.deviceId())) {
            result.setSuggestedOutcome(DecisionOutcome.REJECT);
            result.addFiredRule("HIGH_AMOUNT_NEW_DEVICE");
            result.addReasonCode("HIGH_AMOUNT_NEW_DEVICE");
            return;
        }

        // Rule 2: High velocity -> REJECT
        if (velocity1m >= 5) {
            result.setSuggestedOutcome(DecisionOutcome.REJECT);
            result.addFiredRule("HIGH_VELOCITY_1M");
            result.addReasonCode("HIGH_VELOCITY");
            return;
        }

        // Rule 3: Medium velocity -> REVIEW
        if (velocity5m >= 10) {
            result.setSuggestedOutcome(DecisionOutcome.REVIEW);
            result.addFiredRule("MEDIUM_VELOCITY_5M");
            result.addReasonCode("ELEVATED_VELOCITY");
        }

        // Rule 4: New device with unusual location -> REVIEW
        if (profile.isNewDevice(txn.deviceId()) 
            && profile.isUnusualLocation(txn.location())) {
            result.setSuggestedOutcome(DecisionOutcome.REVIEW);
            result.addFiredRule("NEW_DEVICE_UNUSUAL_LOCATION");
            result.addReasonCode("NEW_DEVICE");
            result.addReasonCode("UNUSUAL_LOCATION");
        }

        // Rule 5: Very high amount -> REVIEW (even if trusted)
        if (txn.amount().compareTo(new BigDecimal("5000")) > 0) {
            if (result.getSuggestedOutcome() == null) {
                result.setSuggestedOutcome(DecisionOutcome.REVIEW);
            }
            result.addFiredRule("HIGH_AMOUNT_THRESHOLD");
            result.addReasonCode("HIGH_AMOUNT");
        }

        // Rule 6: Amount significantly higher than average -> REVIEW
        if (profile.averageTransactionAmount() != null) {
            BigDecimal threshold = profile.averageTransactionAmount().multiply(new BigDecimal("5"));
            if (txn.amount().compareTo(threshold) > 0) {
                if (result.getSuggestedOutcome() == null) {
                    result.setSuggestedOutcome(DecisionOutcome.REVIEW);
                }
                result.addFiredRule("AMOUNT_DEVIATION");
                result.addReasonCode("AMOUNT_ANOMALY");
            }
        }

        // Default: APPROVE if no rules fired
        if (result.getSuggestedOutcome() == null) {
            result.setSuggestedOutcome(DecisionOutcome.APPROVE);
            result.addFiredRule("DEFAULT_APPROVE");
        }
    }

    /**
     * Hot-reload rules (for future enhancement).
     */
    public void reloadRules() {
        log.info("Reloading rules (version: {})", ruleVersion);
        init();
    }
}
