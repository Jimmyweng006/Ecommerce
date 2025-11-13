# Ecommerce Store

## Introduction

Spring Boot–based ecommerce backend that exposes JWT-secured REST APIs for product administration and order checkout. The service uses Liquibase-managed MySQL schemas, optimistic locking to protect concurrent product updates, and a consistent API response envelope for both success and error payloads. Swagger/OpenAPI documentation is published alongside the running application.

## Quick Start

### Required Environment

- Java 21 (Temurin distribution recommended)
- Maven 3.9 or newer
- Docker Compose v2.40.3+

Export or configure the following environment variables when overriding defaults:

| Variable | Purpose | Default |
|----------|---------|---------|
| `SECURITY_JWT_SECRET` | HMAC secret for signing JWTs | `change-me-change-me-change-me-change-me-change` |
| `SECURITY_JWT_EXPIRY_SECONDS` | Token lifetime in seconds | `3600` |
| `SPRING_DATASOURCE_URL` | JDBC URL for the primary database | `jdbc:mysql://localhost:3306/ecommerce?...` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Database credentials | `root` / `rootpassword` |
| `APP_READ_REPLICA_<N>_URL` / `_USERNAME` / `_PASSWORD` | Optional read-only JDBC endpoints (used for read routing) | unset |

### Run with Docker Compose

```bash
docker compose up --build
```

This boots one MySQL primary plus three read-only replicas (official `mysql:8.0` images) along with the Spring Boot application using the `docker` profile. The primary picks up `docker/mysql/conf.d/primary.cnf` and `docker/mysql/initdb/primary/01-create-replication-user.sql`; replicas use `docker/mysql/conf.d/replica.cnf` together with `docker/mysql/initdb/replica/01-configure-replication.sh` to run `CHANGE REPLICATION SOURCE TO ... START REPLICA`. First launch may take ~1–2 minutes while Liquibase migrates and replicas catch up.

Stop and clean all containers and volumes when finished:

```bash
docker compose down -v
```

## Run Book

### Swagger

The API is documented with OpenAPI 3 via springdoc-openapi:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Raw OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### API Endpoint

- **Auth**
  - `POST /api/v1/auth/login` – Exchange credentials for a JWT access token. (Registration endpoint to be added.)
- **Admin**
  - `POST /api/v1/admin/products` – Create a product (requires `ROLE_ADMIN`, optimistic locking enabled).
  - `PUT /api/v1/admin/products/{productId}` – Update product details and stock (requires `ROLE_ADMIN`).
  - `DELETE /api/v1/admin/products/{productId}` – Soft-delete a product (requires `ROLE_ADMIN`).
- **Orders**
  - `POST /api/v1/orders` – Create an order for the authenticated user with atomic stock decrement (requires `ROLE_USER`).
  - `GET /api/v1/orders/{orderId}` – Retrieve order details (owner or `ROLE_ADMIN` only).
- **Products**
  - `GET /api/v1/products` – Browse active products with pagination, category filter, and keyword search (public).
  - `GET /api/v1/products/{productId}` – Retrieve a specific product's details (public).
- **Favorites**
  - `POST /api/v1/favorites` – Add a product to the authenticated user's favorites list (requires `ROLE_USER`).
  - `GET /api/v1/favorites` – Retrieve the authenticated user's favorite products in reverse chronological order.
  - `DELETE /api/v1/favorites/{productId}` – Remove a product from the favorites list (idempotent, requires `ROLE_USER`).

### Read/Write Routing & Replication

- The Spring Boot app uses a routing datasource: non-read-only transactions always hit the primary; `@Transactional(readOnly = true)` methods route round-robin across replicas unless they are annotated with `@ReadFromPrimary`.
- Replica JDBC URLs are supplied via `app.read-replicas[n].*` (mapped from `APP_READ_REPLICA_*` environment variables in Docker Compose). Leave the URL empty to skip a slot.
- Docker Compose wires replication through the files under `docker/mysql`:
  - `conf.d/primary.cnf` enables binlogs, GTID, and raises `max_connections`.
  - `initdb/primary/01-create-replication-user.sql` creates `repl_user`/`repl_password`.
  - Each replica mounts `conf.d/replica.cnf` and `initdb/replica/01-configure-replication.sh`, which waits for the primary and executes `CHANGE REPLICATION SOURCE TO ... START REPLICA`.
- Verify replica health with `docker exec -it ecommerce-db-replica-1 mysql -uroot -prootpassword -e "SHOW REPLICA STATUS\G"` (expect `Replica_IO_Running` and `Replica_SQL_Running` = Yes, `Seconds_Behind_Master` near 0).

### Request Correlation

- `RequestCorrelationFilter` copies `X-Request-ID` from the inbound request (or generates a UUID) and echoes it back in the response while storing it in the MDC.
- Any method annotated with `@LogExecution` will log entry/exit, arguments, and execution time including `corrId=*`, so you can trace a single request across controller/service boundaries.

### Logging

- Logback is configured via `logback-spring.xml` to write both to the console and to `logs/application.log` (daily rollover, 14-day retention).
- Override the file log directory with `LOG_HOME=/var/log/ecommerce` (the Docker image sets this by default) to emit logs under that path.
- View file logs via `tail -f logs/application.log` on your host or `docker exec ecommerce-app tail -f /var/log/ecommerce/application.log` inside the container.

## Test

### Unit tests

```bash
mvn test
```

Executes fast unit tests via Maven Surefire.

### Integration tests

```bash
mvn verify
```

Runs both unit and integration suites (Failsafe picks up `*IntegrationTests`). To target a specific integration test, use `mvn failsafe:integration-test failsafe:verify -Dit.test=OrderControllerIntegrationTests`.

```bash
mvn verify -Dskip.unit.tests=true
```

Skip unit tests when running integration tests only:

### generate test report

```bash
mvn jacoco:report
```

Generates coverage reports under `target/site/jacoco/index.html`.

## Document

- [Tech Design](ARCHITECTURE.md)
- [Load Testing Experiment](EXPERIMENT.md)
