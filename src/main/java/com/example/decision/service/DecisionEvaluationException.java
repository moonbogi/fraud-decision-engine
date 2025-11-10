package com.example.decision.service;

/**
 * Exception thrown when decision evaluation fails.
 */
public class DecisionEvaluationException extends RuntimeException {
    public DecisionEvaluationException(String message) {
        super(message);
    }

    public DecisionEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
