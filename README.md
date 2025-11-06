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

### Run with Docker Compose

```bash
docker compose up --build
```

This starts a local MySQL 8.4 container plus the Spring Boot application with the `docker` profile and matching datasource settings.

```bash
docker compose down -v
```

Stop and clean all containers and volumes when finished.

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
