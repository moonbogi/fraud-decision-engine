# Development Guide

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Docker & Docker Compose
- Git

### Initial Setup

1. Clone the repository:
```bash
git clone https://github.com/8leo8/fraud-decision-engine.git
cd fraud-decision-engine
```

2. Start infrastructure:
```bash
docker-compose up -d
```

3. Build the project:
```bash
./mvnw clean install
```

4. Run the application:
```bash
./mvnw spring-boot:run
```

## Project Structure

```
src/main/java/com/example/decision/
├── DecisionEngineApplication.java  # Main entry point
├── config/                          # Spring configuration
│   ├── KafkaConfig.java            # Kafka producer/consumer setup
│   └── RedisConfig.java            # Redis cache configuration
├── consumer/                        # Kafka event consumers
│   └── TransactionConsumer.java    # Transaction event handler
├── controller/                      # REST API endpoints
│   └── DecisionController.java     # Decision API
├── model/                           # Domain objects
│   ├── Transaction.java            # Input transaction
│   ├── Decision.java               # Output decision
│   ├── UserProfile.java            # Cached user features
│   └── DecisionEntity.java         # Audit entity
├── repository/                      # Data access
│   └── DecisionRepository.java     # JPA repository
└── service/                         # Business logic
    ├── DecisionService.java        # Main orchestrator
    ├── RuleEngineService.java      # Rule evaluation
    ├── FeatureService.java         # Feature enrichment
    └── MLScoringService.java       # Risk scoring
```

## Running Tests

### Unit Tests
```bash
./mvnw test
```

### Integration Tests
```bash
./mvnw verify
```

### Code Coverage
```bash
./mvnw jacoco:report
open target/site/jacoco/index.html
```

## Local Development Workflow

### 1. Start Infrastructure Only
```bash
docker-compose up -d kafka redis postgres
```

### 2. Run Application from IDE
- Import as Maven project
- Set active profile: `default` or `local`
- Run `DecisionEngineApplication.main()`

### 3. Generate Test Events
```bash
./scripts/generate-traffic.sh
```

### 4. Monitor Kafka Topics
```bash
# Consumer events
docker exec -it fraud-decision-engine-kafka-1 \
  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic txn-events \
  --from-beginning

# Decision results
docker exec -it fraud-decision-engine-kafka-1 \
  kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic decision-results \
  --from-beginning
```

### 5. Query Database
```bash
docker exec -it fraud-decision-engine-postgres-1 \
  psql -U decision_user -d decision_db

# Example queries
SELECT outcome, COUNT(*) FROM decisions GROUP BY outcome;
SELECT AVG(latency_ms) FROM decisions;
```

## Adding New Features

### Add a New Rule
1. Edit `RuleEngineService.applySimpleRules()`
2. Add rule logic and reason codes
3. Write unit tests in `RuleEngineServiceTest`

### Add a New Feature
1. Update `UserProfile` model
2. Modify `FeatureService` to cache/retrieve feature
3. Update `MLScoringService` to use feature in scoring
4. Add tests

### Add a New API Endpoint
1. Add method to `DecisionController`
2. Use `@GetMapping` / `@PostMapping`
3. Add OpenAPI annotations (future)
4. Write integration test

## Debugging

### Enable Debug Logging
In `application.yml`:
```yaml
logging:
  level:
    com.example.decision: DEBUG
    org.springframework.kafka: DEBUG
```

### Profile Application Performance
```bash
# Run with async-profiler
java -agentpath:/path/to/libasyncProfiler.so \
  -jar target/fraud-decision-engine-1.0.0-SNAPSHOT.jar

# Generate flame graph
./profiler.sh -d 30 -f flamegraph.html <PID>
```

### Monitor JVM Metrics
```bash
# Heap usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# GC stats
curl http://localhost:8080/actuator/metrics/jvm.gc.pause
```

## Code Style

### Checkstyle
```bash
./mvnw checkstyle:check
```

### Formatting
- Use Google Java Style Guide
- 4 spaces for indentation
- 100 character line limit

## Common Issues

### Kafka Connection Refused
- Ensure Kafka is running: `docker-compose ps`
- Check advertised listeners in docker-compose.yml

### Redis Timeout
- Verify Redis is healthy: `docker exec fraud-decision-engine-redis-1 redis-cli ping`
- Check connection pool settings

### Database Connection Failed
- Ensure Postgres is running
- Verify credentials in application.yml

## Performance Tips

1. **Increase Kafka Consumer Threads**:
   ```yaml
   # application.yml
   spring.kafka.listener.concurrency: 5
   ```

2. **Tune JVM**:
   ```bash
   export JAVA_OPTS="-Xmx2g -XX:+UseG1GC"
   ./mvnw spring-boot:run
   ```

3. **Redis Connection Pooling**:
   ```yaml
   spring.data.redis.lettuce.pool.max-active: 16
   ```

## Resources

- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/html/)
- [Drools Documentation](https://docs.drools.org/)
- [Micrometer Docs](https://micrometer.io/docs)
