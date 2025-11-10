package com.example.decision.consumer;

import com.example.decision.model.Decision;
import com.example.decision.model.Transaction;
import com.example.decision.service.DecisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for transaction events.
 * Processes events and commits offsets manually after successful processing.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionConsumer {

    private final DecisionService decisionService;

    @KafkaListener(
        topics = "${decision.topics.input:txn-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Payload Transaction transaction,
                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                       @Header(KafkaHeaders.OFFSET) long offset,
                       Acknowledgment acknowledgment) {
        
        log.info("Received transaction from partition {}, offset {}: {}", 
                 partition, offset, transaction.transactionId());

        try {
            Decision decision = decisionService.evaluate(transaction);
            
            // Commit offset only after successful processing
            acknowledgment.acknowledge();
            
            log.info("Transaction processed successfully: {} with outcome: {}", 
                     transaction.transactionId(), decision.outcome());
            
        } catch (Exception e) {
            log.error("Failed to process transaction: {}, will retry", 
                     transaction.transactionId(), e);
            // Don't acknowledge - message will be reprocessed
            // In production, add dead letter queue after N retries
        }
    }
}
