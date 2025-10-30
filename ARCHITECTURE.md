# Architecture Overview

This document captures the current relational schema that Liquibase applies when the service boots. Keep this section synchronized with the change logs under `src/main/resources/db/changelog`.

## Database Schema (MySQL)

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('USER','ADMIN') NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_users_email (email)
) ENGINE = InnoDB;

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL
) ENGINE = InnoDB;

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status ENUM('PENDING','PROCESSING','FAILED','COMPLETED') NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_orders_idempotency (idempotency_key),
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE favorites (
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_favorites PRIMARY KEY (user_id, product_id),
    CONSTRAINT fk_favorites_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_favorites_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE CASCADE
) ENGINE = InnoDB;
```

## Authentication Flow

The diagram below illustrates how Spring Security mediates requests, checking credentials on login and validating JWTs on subsequent calls before delegating to application controllers.

```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant SecurityConfig
    participant JwtFilter as JwtAuthenticationFilter
    participant JwtService

    Client->>AuthController: POST /api/v1/auth/login
    AuthController->>SecurityConfig: delegate to AuthenticationManager
    SecurityConfig-->>AuthController: Authenticated principal
    AuthController->>JwtService: generateToken(principal)
    JwtService-->>AuthController: JWT string
    AuthController-->>Client: 200 OK + token

    Client->>JwtFilter: GET /api/v1/resource (Authorization: Bearer)
    JwtFilter->>JwtService: validateToken(token)
    JwtService-->>JwtFilter: 主體(sub) + 角色 (roles claim)
    JwtFilter->>SecurityConfig: populate SecurityContext
    JwtFilter-->>Client: request proceeds to controllers
```

### Spring Security Components Wiring Snapshot

```mermaid
graph TD
    SecurityConfig -->|registers| JwtAuthenticationFilter
    SecurityConfig -->|exposes| AuthenticationManager
    SecurityConfig --> PasswordEncoder
    JwtAuthenticationFilter --> JwtService
    JwtAuthenticationFilter --> UserDetailsService
    AuthController --> AuthenticationManager
    AuthController --> JwtService
```
