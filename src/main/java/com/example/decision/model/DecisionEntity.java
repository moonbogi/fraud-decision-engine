package com.example.decision.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Audit entity for persisting decisions.
 */
@Entity
@Table(name = "decisions", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_outcome", columnList = "outcome")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String transactionId;
    
    @Column(nullable = false)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionOutcome outcome;
    
    @Column(nullable = false)
    private Double riskScore;
    
    @Column(columnDefinition = "TEXT")
    private String reasonCodes;
    
    @Column(nullable = false)
    private String ruleVersion;
    
    @Column(nullable = false)
    private Long latencyMs;
    
    @Column(nullable = false)
    private Instant timestamp;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
