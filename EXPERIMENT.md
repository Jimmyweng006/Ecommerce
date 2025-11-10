# Experiment Plan

## Baseline
- Environment: Docker Desktop (Linux) with 12 vCPU, 7.65 GiB RAM.
- Profiles: `docker,load-test` (seed 500k products).
- Scenario: k6 `constant-arrival-rate`, 1000 req/s, preallocated VUs 50, max VUs 100.
- Result: ~16 req/s, P95 ≈ 6 s, significant dropped iterations → DB full-table scans under high concurrency.
- Observations:
  - Performance Schema shows average 6–8 ms per query, but high concurrency causes queueing.
  - Slow query log / EXPLAIN indicates `%keyword%` search requires full scan; count query also does full scan.
  - Docker stats: DB container CPU near saturation during load tests.

## Optimization Experiments

### 1. Full-Text Index / Search
- **Goal**: Reduce scan time for keyword searches by leveraging `FULLTEXT` indices or external search engine (e.g., Elasticsearch).
- **Steps**:
  1. Add `FULLTEXT(title, description)` to match keywords.
  2. Modify query to use `MATCH ... AGAINST` instead of `%keyword%`.
  3. Re-run baseline load test; record RPS/P95 and DB CPU usage.
- **Success Criteria**: At least 2× throughput improvement vs. 500k seed baseline (~16 req/s on k6 cloud). Target local P95 latency < 500 ms.
- **Result (k6 cloud local-execution, 50→100 VUs, 1k iters/s)**:
  - Throughput 4.5 req/s (357 completed, 59,644 dropped) with `MATCH ... AGAINST ('+board*')`.
  - HTTP P95 ≈ 26.8 s; 3% 5xx due to DB connection exhaustion despite `max_connections=300`.
  - Full-text plan confirmed (`type=fulltext`, `key=idx_products_title_description_fulltext`) but still returns ~71k rows per query, so latency dominated by high concurrency + connection starvation, not scanning cost alone.

### 2. Read/Write Separation
- **Goal**: Offload read traffic to replicas, keeping primary DB free for writes.
- **Steps**:
  1. Add MySQL read replica (or simulate via second container).
  2. Configure Spring routing (e.g., Spring Cloud LoadBalancer, manual routing for read-only endpoints).
  3. Re-run tests focusing on GET /products; measure throughput per DB node.
- **Success Criteria**: Primary CPU utilization decreases, GET endpoints scale horizontally.

### 3. Redis Cache for Product List/Search
- **Goal**: Avoid hitting DB for repeated queries.
- **Steps**:
  1. Integrate Redis (via docker-compose).
  2. Cache popular queries based on category / keyword combination with TTL.
  3. Ensure invalidation strategy (e.g., on product change).
  4. Run targeted load tests to confirm DB load reduction.
- **Success Criteria**: Cache hit ratio > 80% for hot queries, DB CPU drops significantly, P95 latency < 200 ms.

## Notes
- Each experiment should rerun the same k6 scenario to keep benchmarks consistent.
- Record Docker stats and Performance Schema data before/after to quantify improvements.
- Consider combining techniques (e.g., cache + read replica) once individual benefits are measured.
