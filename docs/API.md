## API Reference

### Base URL
```
http://localhost:8080/api/v1
```

### Endpoints

#### 1. Evaluate Transaction (Synchronous)

**POST** `/decisions/evaluate`

Evaluate a transaction and return a decision immediately.

**Request Body:**
```json
{
  "transactionId": "txn-12345",
  "userId": "user-001",
  "amount": 150.00,
  "currency": "USD",
  "merchant": "Amazon",
  "merchantCategory": "RETAIL",
  "deviceId": "device-123",
  "location": "US",
  "timestamp": "2025-11-10T10:30:00Z"
}
```

**Response:** (200 OK)
```json
{
  "transactionId": "txn-12345",
  "userId": "user-001",
  "outcome": "APPROVE",
  "riskScore": 15.5,
  "reasonCodes": ["DEFAULT_APPROVE"],
  "ruleVersion": "v1",
  "latencyMs": 4,
  "timestamp": "2025-11-10T10:30:00.123Z"
}
```

**Outcomes:**
- `APPROVE`: Transaction is safe
- `REVIEW`: Manual review required
- `REJECT`: Transaction blocked

---

#### 2. Get User Decision History

**GET** `/decisions/user/{userId}`

Retrieve all decisions for a specific user.

**Response:** (200 OK)
```json
[
  {
    "transactionId": "txn-12345",
    "userId": "user-001",
    "outcome": "APPROVE",
    "riskScore": 15.5,
    "reasonCodes": ["DEFAULT_APPROVE"],
    "ruleVersion": "v1",
    "latencyMs": 4,
    "timestamp": "2025-11-10T10:30:00.123Z"
  }
]
```

---

#### 3. Get Decision by Transaction ID

**GET** `/decisions/transaction/{transactionId}`

Retrieve a specific decision.

**Response:** (200 OK)
```json
{
  "transactionId": "txn-12345",
  "userId": "user-001",
  "outcome": "APPROVE",
  "riskScore": 15.5,
  "reasonCodes": ["DEFAULT_APPROVE"],
  "ruleVersion": "v1",
  "latencyMs": 4,
  "timestamp": "2025-11-10T10:30:00.123Z"
}
```

**Response:** (404 Not Found)
```json
{
  "error": "Transaction not found"
}
```

---

#### 4. Get Statistics

**GET** `/decisions/stats`

Get aggregate statistics.

**Response:** (200 OK)
```json
{
  "totalDecisions": 150000,
  "avgLatencyMs": 4.2
}
```

---

### Actuator Endpoints

#### Health Check
**GET** `/actuator/health`

```json
{
  "status": "UP"
}
```

#### Metrics
**GET** `/actuator/metrics`

List all available metrics.

**GET** `/actuator/metrics/decision.latency`

```json
{
  "name": "decision.latency",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 10000
    },
    {
      "statistic": "TOTAL_TIME",
      "value": 42000
    },
    {
      "statistic": "MAX",
      "value": 45.2
    }
  ]
}
```

#### Prometheus Metrics
**GET** `/actuator/prometheus`

Returns Prometheus-formatted metrics.

---

### Error Responses

**400 Bad Request** - Invalid input
```json
{
  "error": "Validation failed",
  "details": ["amount must be positive"]
}
```

**500 Internal Server Error** - Processing failure
```json
{
  "error": "Decision evaluation failed",
  "message": "Redis connection timeout"
}
```

---

### cURL Examples

**Evaluate Transaction:**
```bash
curl -X POST http://localhost:8080/api/v1/decisions/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "txn-test-001",
    "userId": "user-test",
    "amount": 250.00,
    "currency": "USD",
    "merchant": "TestMerchant",
    "merchantCategory": "RETAIL",
    "deviceId": "device-test",
    "location": "US",
    "timestamp": "2025-11-10T12:00:00Z"
  }'
```

**Get User History:**
```bash
curl http://localhost:8080/api/v1/decisions/user/user-001
```

**Check Health:**
```bash
curl http://localhost:8080/actuator/health
```

**View Metrics:**
```bash
curl http://localhost:8080/actuator/metrics/decision.latency
```
