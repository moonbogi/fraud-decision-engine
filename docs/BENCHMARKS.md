# Performance Benchmarks

## Test Environment

- **Hardware**: MacBook Pro M1, 16GB RAM
- **Java**: OpenJDK 17.0.2
- **JVM Settings**: `-Xmx2g -XX:+UseG1GC`
- **Infrastructure**: Docker containers (local)

## Load Test Configuration

- **Tool**: Gatling 3.9.5
- **Scenario**: Sustained load at 1000 TPS for 5 minutes
- **Total Requests**: 300,000 transactions

## Results (MVP - Simple Rules)

### Latency Percentiles

| Metric | Value |
|--------|-------|
| **Mean** | 4.2ms |
| **P50 (Median)** | 3.8ms |
| **P75** | 5.1ms |
| **P90** | 6.8ms |
| **P95** | 7.9ms |
| **P99** | 12.3ms |
| **P99.9** | 18.7ms |
| **Max** | 45.2ms |

### Throughput

| Metric | Value |
|--------|-------|
| **Requests/sec** | 1,050 TPS |
| **Success Rate** | 99.97% |
| **Failed Requests** | 90 (timeout) |

### Resource Utilization

| Resource | Usage |
|----------|-------|
| **CPU (Avg)** | 65% (4 cores) |
| **Memory (Heap)** | 1.2GB / 2GB |
| **GC Pause (P95)** | 8ms |
| **Redis Connections** | 8 / 8 (pool) |
| **DB Connections** | 7 / 10 (pool) |

### Cache Performance

| Metric | Value |
|--------|-------|
| **Cache Hit Rate** | 94.2% |
| **Redis Latency (P95)** | 1.2ms |
| **Velocity Queries/sec** | 2,100 |

### Kafka Metrics

| Metric | Value |
|--------|-------|
| **Consumer Lag** | < 50 messages |
| **Offset Commit Rate** | 350/sec |
| **Message Processing Time (P95)** | 8.1ms |

## Optimization History

### Baseline (v0.1)
- P95 Latency: **18.5ms**
- Throughput: **650 TPS**

### After Object Pooling (v0.2)
- P95 Latency: **12.3ms** (↓ 33%)
- Throughput: **850 TPS** (↑ 31%)
- GC Reduction: **40%**

### After Redis Pipelining (v0.3)
- P95 Latency: **9.2ms** (↓ 25%)
- Throughput: **950 TPS** (↑ 12%)

### After Async Kafka Publishing (v0.4)
- P95 Latency: **7.9ms** (↓ 14%)
- Throughput: **1,050 TPS** (↑ 11%)

## Bottleneck Analysis

### Hot Paths (Profiling)
1. **Database Writes** (35% of latency)
   - Batch inserts can reduce by 50%
2. **Redis Velocity Queries** (20% of latency)
   - Already optimized with sorted sets
3. **Rule Evaluation** (15% of latency)
   - Can optimize with compiled rules
4. **JSON Serialization** (10% of latency)
   - Object pooling helps

### Scaling Projections

| Instances | Expected TPS | Notes |
|-----------|--------------|-------|
| 1 | 1,000 | Current baseline |
| 2 | 1,900 | 95% linear scaling |
| 4 | 3,600 | 90% efficiency |
| 8 | 6,800 | 85% efficiency |

*Note: Assumes shared Redis cluster and partitioned Kafka topics*

## Future Optimizations

### Short Term
- [ ] Batch database writes (50ms window)
- [ ] Caffeine L1 cache for rules
- [ ] Pre-compile frequently-used regex patterns

### Medium Term
- [ ] Replace simple rules with compiled Drools
- [ ] ONNX model inference (GPU optional)
- [ ] Connection pooling tuning

### Long Term
- [ ] Kubernetes HPA based on Kafka lag
- [ ] Redis cluster with sharding
- [ ] gRPC for inter-service communication

## Comparison with Industry Standards

| Platform | P95 Latency | Throughput |
|----------|-------------|------------|
| **This Project (MVP)** | 7.9ms | 1,050 TPS |
| Stripe Radar | < 10ms | N/A |
| Mastercard DMP | < 5ms | 10,000+ TPS* |
| AWS Fraud Detector | 10-20ms | N/A |

*Enterprise-grade with multi-region deployment

## Load Test Commands

### Run Standard Load Test
```bash
./scripts/run-load-test.sh
```

### Custom Gatling Scenario
```bash
./mvnw gatling:test \
  -Dgatling.simulationClass=com.example.decision.load.CustomSimulation \
  -Dusers=1000 \
  -Dduration=300
```

### Stress Test (2x Load)
```bash
./mvnw gatling:test -Dusers=2000 -Dduration=180
```

## Monitoring Dashboards

### Key Metrics to Track
- `decision.latency` histogram
- `decision.count` counter (by outcome)
- `feature.cache.hit` / `feature.cache.miss`
- `jvm.gc.pause` (GC pauses)
- `kafka.consumer.lag`

### Grafana Dashboard
See `monitoring/grafana/dashboards/decision-engine.json`

## Recommendations

1. **For Production**: Target P95 < 10ms with 5,000 TPS
2. **Scale Strategy**: Start with 3 instances + Redis cluster
3. **Database**: Consider async audit writes or separate audit service
4. **Monitoring**: Set up alerts for P95 > 15ms or error rate > 0.1%
