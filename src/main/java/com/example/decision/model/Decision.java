package com.example.decision.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;

/**
 * Result of a decision evaluation.
 */
public record Decision(
    String transactionId,
    String userId,
    DecisionOutcome outcome,
    double riskScore,
    List<String> reasonCodes,
    String ruleVersion,
    long latencyMs,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
) {
    public Decision(String transactionId, String userId, DecisionOutcome outcome, 
                    double riskScore, List<String> reasonCodes, String ruleVersion, long latencyMs) {
        this(transactionId, userId, outcome, riskScore, reasonCodes, ruleVersion, latencyMs, Instant.now());
    }
}
