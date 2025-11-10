package com.example.decision.controller;

import com.example.decision.model.Decision;
import com.example.decision.model.Transaction;
import com.example.decision.repository.DecisionRepository;
import com.example.decision.service.DecisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST API for synchronous decision evaluation and history queries.
 */
@RestController
@RequestMapping("/api/v1/decisions")
@Slf4j
@RequiredArgsConstructor
public class DecisionController {

    private final DecisionService decisionService;
    private final DecisionRepository repository;

    /**
     * Synchronous decision evaluation endpoint.
     * For testing and low-volume scenarios.
     */
    @PostMapping("/evaluate")
    public ResponseEntity<Decision> evaluate(@Valid @RequestBody Transaction transaction) {
        log.info("REST: Evaluating transaction: {}", transaction.transactionId());
        Decision decision = decisionService.evaluate(transaction);
        return ResponseEntity.ok(decision);
    }

    /**
     * Get decision history for a user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Decision>> getUserDecisions(@PathVariable String userId) {
        var entities = repository.findByUserIdOrderByTimestampDesc(userId);
        var decisions = entities.stream()
            .map(e -> new Decision(
                e.getTransactionId(),
                e.getUserId(),
                e.getOutcome(),
                e.getRiskScore(),
                List.of(e.getReasonCodes().split(",")),
                e.getRuleVersion(),
                e.getLatencyMs(),
                e.getTimestamp()
            ))
            .toList();
        return ResponseEntity.ok(decisions);
    }

    /**
     * Get decision by transaction ID.
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<Decision> getDecision(@PathVariable String transactionId) {
        return repository.findByTransactionId(transactionId)
            .map(e -> new Decision(
                e.getTransactionId(),
                e.getUserId(),
                e.getOutcome(),
                e.getRiskScore(),
                List.of(e.getReasonCodes().split(",")),
                e.getRuleVersion(),
                e.getLatencyMs(),
                e.getTimestamp()
            ))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<DecisionStats> getStats() {
        long totalDecisions = repository.count();
        Double avgLatency = repository.getAverageLatencySince(
            Instant.now().minusSeconds(3600)
        );
        
        return ResponseEntity.ok(new DecisionStats(
            totalDecisions,
            avgLatency != null ? avgLatency : 0.0
        ));
    }

    record DecisionStats(long totalDecisions, double avgLatencyMs) {}
}
