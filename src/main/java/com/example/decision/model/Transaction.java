package com.example.decision.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents an incoming transaction event from Kafka.
 */
public record Transaction(
    @NotBlank String transactionId,
    @NotBlank String userId,
    @NotNull @Positive BigDecimal amount,
    @NotBlank String currency,
    @NotBlank String merchant,
    @NotBlank String merchantCategory,
    @NotBlank String deviceId,
    String location,
    @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
) {
    public Transaction {
        // Compact constructor for validation
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
}
