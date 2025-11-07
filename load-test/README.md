# Load Test Plan (k6)

This folder will host k6 scripts for exercising the most critical flows. Each scenario below documents its purpose, traffic pattern, success criteria, and potential remediation steps.

## Scenarios

Enable the load-test profile to seed synthetic products before running k6: SPRING_PROFILES_ACTIVE=docker,load-test

Seed count defaults to 10000

Use `LOAD_TEST_SEED_COUNT`, `LOAD_TEST_BATCH_SIZE` to override

### 1. Product Browsing Surge
- **Purpose**: Validate `/api/v1/products` (pagination + keyword filter) under marketing spikes to ensure the DB/cache layer absorbs high RPS without latency spikes.
- **Environment baseline**: Docker Desktop (Linux) with 12 vCPU / 7.65 GiB RAM, `SPRING_PROFILES_ACTIVE=docker,load-test`, seed count 10k products.
- **Traffic shape**: `constant-arrival-rate` targeting 1000 req/s for 1 min (preallocated VUs: 600, max VUs: 2000).
- **Current baseline**: ~690 req/s, P95 ≈ 3.2 s, dropped iterations once 2000 VUs are reached → indicates server latency bottleneck.
- **Success criteria**:
  - P95 latency < 300 ms
  - Error rate (HTTP != 200) < 1%
- **Remediation ideas if failing**:
  - Introduce Redis/HTTP cache for popular searches.
  - Add covering indexes / full-text search.
  - Shard read replicas or paginate aggressively.

### 2. Hot Product Checkout
- **Purpose**: Stress `/api/v1/orders` to mimic flash sales where thousands of users buy the same SKU simultaneously, validating atomic stock decrement and concurrency control.
- **Traffic shape**: 200 VUs constantly posting an order payload targeting the same product ID, reusing unique idempotency keys per VU.
- **Success criteria**:
  - No oversell (inventory never < 0, conflict responses for insufficient stock).
  - P99 latency < 500 ms.
- **Remediation ideas**:
  - Scale DB write capacity, review atomic SQL and lock ordering.
  - Queue writes via Kafka/Saga if synchronous flow becomes bottleneck.
  - Optimize serialization/JWT verification path.

### 3. Order Detail Reads Burst
- **Purpose**: Ensure `/api/v1/orders/{orderId}` can serve large numbers of post-purchase checks (e.g., mobile apps refreshing order status).
- **Traffic shape**: 300 VUs, random mix of owner/admin tokens hitting different order IDs; mix of cache hits/misses.
- **Success criteria**:
  - P95 latency < 250 ms.
  - Error rate < 0.5% (mostly 401/403).
- **Remediation ideas**:
  - Add caching layer for read-only order summaries.
  - Preload relationships using entity graph or DTO projections.
  - Offload order history to read replica or Elasticsearch for analytics.

### 4. Notification Fan-out (optional)
- **Purpose**: Once Kafka/event processing is added, stress-test the notification pipeline triggered by `OrderCreated` events.
- **Traffic shape**: Use k6 to send burst of mock events (or HTTP endpoint that publishes events) while monitoring consumer lag.
- **Success criteria**:
  - Consumer lag stays < 2 seconds.
  - No dead-lettered messages.
- **Remediation ideas**:
  - Scale consumer group, add retries/backoff policies, move heavy work to background jobs.

## Next Steps
1. Install k6 locally or via Docker (`docker run -i loadimpact/k6 run - <script.js`).
2. Create script files under this directory (e.g., `products-browse.js`, `orders-checkout.js`) following the scenarios above.
3. Wire scripts into CI if needed (nightly runs or before major releases).
4. Track results (k6 summary JSON, Grafana dashboards) and update this plan with observed bottlenecks + mitigations.
