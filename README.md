# Fraud Decision Engine

Real-time streaming decision platform combining business rules + ML scoring for transaction fraud detection. Processes events from Kafka with sub-10ms P95 latency at 1000+ TPS.

## Project Overview

This project demonstrates a production-grade decision management system inspired by enterprise fraud prevention platforms. It showcases:

- **High-performance Java backend** with Spring Boot 3.2
- **Real-time streaming** via Kafka event processing
- **Business rules engine** (Drools) with versioned rulesets
- **In-memory caching** (Redis) for feature enrichment
- **Comprehensive testing** (JUnit, Mockito, Testcontainers, Gatling)
- **Containerization** with Docker and docker-compose
- **Observability** with Micrometer metrics and structured logging

## Architecture

```
┌─────────────┐      ┌──────────────────┐      ┌─────────────┐
│ Transaction │─────▶│  Kafka Topic     │─────▶│   Decision  │
│  Producer   │      │  (txn-events)    │      │   Service   │
│  (Simulator)│      └──────────────────┘      │ (Spring Boot│
└─────────────┘                                 │   + Drools) │
                                                └──────┬──────┘
                                                       │
                                    ┌─────────────────┼──────────────┐
                                    ▼                  ▼              ▼
                              ┌─────────┐      ┌──────────┐   ┌──────────┐
                              │  Redis  │      │ Postgres │   │  Kafka   │
                              │ (cache) │      │ (audit)  │   │ (results)│
                              └─────────┘      └──────────┘   └──────────┘
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| **Language** | Java 17 (records, sealed classes) |
| **Framework** | Spring Boot 3.2, Spring Kafka |
| **Rules Engine** | Drools 8.44 |
| **Cache** | Redis (Lettuce client) |
| **Database** | PostgreSQL (audit), H2 (tests) |
| **Messaging** | Apache Kafka 3.6 |
| **Testing** | JUnit 5, Mockito, Testcontainers, Gatling |
| **Build** | Maven 3.9 |
| **Containerization** | Docker, docker-compose |
| **Observability** | Micrometer, Prometheus, JSON logging |

## Performance Targets

- **Latency**: P50 < 5ms, P95 < 10ms, P99 < 20ms
- **Throughput**: 1000+ decisions/sec (single instance)
- **Cache hit rate**: > 90%
- **Test coverage**: > 80%

## Quick Start

### Prerequisites

- Java 17+
- Docker & Docker Compose
- Maven 3.6+

### Run Locally

```bash
# Start infrastructure (Kafka, Redis, Postgres)
docker-compose up -d

# Build the application
./mvnw clean package

# Run the service
./mvnw spring-boot:run

# In another terminal, generate test traffic
./scripts/generate-traffic.sh

# View metrics
curl http://localhost:8080/actuator/metrics/decision.latency
```

### Run with Docker

```bash
# Build and run everything
docker-compose --profile all up --build

# View logs
docker-compose logs -f decision-service
```

## Testing

### Unit Tests
```bash
./mvnw test
```

### Integration Tests (Testcontainers)
```bash
./mvnw verify -P integration-tests
```

### Load Tests (Gatling)
```bash
./scripts/run-load-test.sh
# Open target/gatling/*/index.html for results
```

## Project Structure

```
fraud-decision-engine/
├── src/
│   ├── main/java/com/example/decision/
│   │   ├── DecisionEngineApplication.java
│   │   ├── config/              # Spring configuration
│   │   ├── consumer/            # Kafka consumers
│   │   ├── service/             # Business logic
│   │   ├── model/               # Domain objects
│   │   ├── repository/          # Data access
│   │   └── metrics/             # Custom metrics
│   ├── main/resources/
│   │   ├── application.yml
│   │   ├── rules/               # Drools rule files
│   │   └── logback-spring.xml
│   └── test/java/
│       ├── unit/                # Unit tests
│       ├── integration/         # Testcontainers tests
│       └── load/                # Gatling simulations
├── scripts/                     # Utility scripts
├── docs/                        # Documentation
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

## Key Features

### 1. Real-Time Decision Making
- Consume transactions from Kafka with exactly-once semantics
- Enrich with user profile and velocity features from Redis
- Execute versioned business rules (Drools)
- Calculate ML-based risk scores
- Return decisions with explainability (reason codes)

### 2. Feature Enrichment
- **User Profiles**: Cached in Redis with TTL
- **Velocity Tracking**: Sliding window counters (1/5/10 minutes)
- **Device Fingerprints**: Trusted device lists
- **Location History**: Home location detection

### 3. Rule Engine
- Drools-based rules with versioning (v1, v2)
- Hot-reload capability for rule updates
- Audit trail of rule execution
- Example rules:
  - `REJECT if amount > $10,000 AND new device`
  - `FLAG if velocity > 5 transactions/minute`
  - `APPROVE if trusted device AND within home location`

### 4. Observability
- Micrometer metrics (latency, throughput, cache hits)
- Structured JSON logging with correlation IDs
- Spring Boot Actuator endpoints
- Ready for Prometheus scraping + Grafana dashboards

### 5. Audit & Compliance
- All decisions persisted to PostgreSQL
- Decision history queryable via REST API
- Rule version tracking per decision
- Explainability: reason codes for each verdict

## Performance Optimizations

- **Object pooling** for transaction deserialization (40% GC reduction)
- **Batch Redis operations** via pipelining (3x throughput)
- **Async Kafka commits** after audit writes
- **Connection pooling** for database and cache
- **Caffeine local cache** for hot rules (L1 cache)

## Security Considerations

- Input validation on all API endpoints
- Secure credential management (Spring Cloud Config)
- Rate limiting on REST APIs
- Audit logging for compliance
- PII masking in logs

## Design Decisions

### Why Drools?
- Non-developers can author rules via DSL
- Built-in versioning and conflict resolution
- Performance: RETE algorithm for rule matching
- Industry standard in decision management

### Why Redis for Features?
- Sub-millisecond latency for cached lookups
- Native support for sorted sets (velocity windows)
- TTL management for stale data
- Simpler than dedicated time-series DB for MVP

### Async Kafka Commits
- Write to audit DB first, then commit offset
- Prevents message loss on service crash
- Slight latency trade-off for durability


## Documentation

- [Architecture Deep Dive](docs/ARCHITECTURE.md)
- [Performance Benchmarks](docs/BENCHMARKS.md)
- [Development Guide](docs/DEVELOPMENT.md)
- [API Reference](docs/API.md)


## License

MIT License - feel free to use this as a learning resource.


**Built to demonstrate**: Java backend expertise, high-performance system design, streaming architectures, comprehensive testing strategies, and production-ready software practices.
