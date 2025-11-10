# Architecture Deep Dive

## System Overview

The Fraud Decision Engine is a real-time streaming decision platform that evaluates transactions using a combination of business rules and machine learning risk scoring.

## Key Components

### 1. Event Ingestion Layer
- **Kafka Consumer**: Processes transaction events from `txn-events` topic
- **Manual Offset Management**: Ensures exactly-once semantics by committing offsets only after successful processing
- **Concurrency**: 3 consumer threads for parallel processing

### 2. Decision Orchestration
- **DecisionService**: Central orchestrator coordinating all decision steps
- **Feature Enrichment**: Retrieves user profiles and velocity metrics from Redis
- **Rule Evaluation**: Executes Drools business rules
- **ML Scoring**: Calculates risk scores using weighted features
- **Final Decision**: Combines rule outcomes and ML scores

### 3. Feature Store (Redis)
- **User Profiles**: Cached profiles with average amounts, trusted devices, frequent merchants
- **Velocity Tracking**: Sorted sets with timestamp scores for sliding window queries
- **TTL Management**: 1-hour TTL for profiles, 10-minute TTL for velocity data

### 4. Rule Engine
- **Simple Rules (MVP)**: Java-based rule logic
- **Drools Integration (Future)**: Production-grade rule engine with versioning
- **Rule Examples**:
  - High amount + new device → REJECT
  - Velocity > 5 txn/min → REJECT
  - New device + unusual location → REVIEW

### 5. Audit Trail
- **PostgreSQL**: Persistent storage for all decisions
- **Indexed Queries**: Fast lookups by user, transaction, outcome, timestamp
- **Compliance**: Full decision history with reason codes and rule versions

### 6. Observability
- **Micrometer Metrics**: Latency histograms, throughput counters, cache hit rates
- **Structured Logging**: JSON logs with correlation IDs
- **Spring Boot Actuator**: Health checks, metrics endpoints

## Data Flow

```
1. Kafka Event → TransactionConsumer.consume()
2. DecisionService.evaluate()
   a. FeatureService.getUserProfile() [Redis cache]
   b. FeatureService.getVelocity() [Redis sorted set]
   c. RuleEngineService.evaluate() [Business rules]
   d. MLScoringService.calculateRiskScore() [Feature scoring]
   e. determineOutcome() [Decision logic]
   f. DecisionRepository.save() [Postgres audit]
   g. FeatureService.incrementVelocity() [Redis update]
   h. KafkaTemplate.send() [Output topic]
3. Kafka Offset Commit [Manual acknowledgment]
```

## Latency Breakdown (Target)

| Step | Target Latency |
|------|----------------|
| Feature retrieval (Redis) | 1-2ms |
| Rule evaluation | 1-2ms |
| ML scoring | 1ms |
| Database audit write | 2-3ms |
| Kafka publish | 1-2ms |
| **Total P95** | **< 10ms** |

## Scalability Patterns

### Horizontal Scaling
- Add more Kafka consumer instances
- Partition transactions by userId for affinity
- Redis cluster for distributed caching

### Vertical Scaling
- Increase JVM heap for larger caches
- More Kafka consumer threads per instance
- Connection pool tuning

### Performance Optimizations
- Object pooling for transaction deserialization
- Redis pipelining for batch operations
- Async Kafka publishing
- Caffeine L1 cache for hot rules

## Fault Tolerance

### Kafka Consumer
- Manual offset commits prevent message loss
- Retry logic with exponential backoff
- Dead letter queue for poison messages

### Redis Cache
- Graceful degradation: fallback to defaults if cache unavailable
- Circuit breaker pattern (Resilience4j)

### Database
- Connection pooling with health checks
- Transaction isolation for audit writes
- Retry on transient failures

## Security Considerations

- **Input Validation**: Bean Validation on API requests
- **PII Protection**: Masking in logs
- **Secrets Management**: Externalized configuration
- **Network Isolation**: Service mesh (future)

## Future Enhancements

1. **Drools Integration**: Replace simple rules with full Drools engine
2. **ONNX ML Models**: Replace heuristic scoring with trained models
3. **A/B Testing**: Route traffic to different rule versions
4. **Real-time Dashboard**: WebSocket streaming of decisions
5. **GraphQL API**: Flexible decision history queries
6. **Kubernetes Deployment**: HPA based on Kafka consumer lag
