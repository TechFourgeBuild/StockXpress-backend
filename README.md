# 📦 StockXpress — Backend Technical Documentation

**A B2B Inventory & Warehouse Management System**

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Aiven%20Cloud-336791?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-Reverse%20Proxy-009639?logo=nginx&logoColor=white)
![GCP](https://img.shields.io/badge/Google%20Cloud-Compute%20Engine-4285F4?logo=googlecloud&logoColor=white)
![License](https://img.shields.io/badge/License-Proprietary-lightgrey)
![Status](https://img.shields.io/badge/Status-Production-brightgreen)

| 🏷️ | |
|---|---|
| 📂 **Repository** | [github.com/TechFourgeBuild/StockXpress-backend](https://github.com/TechFourgeBuild/StockXpress-backend) |
| 🔖 **Version** | v0.0.1-SNAPSHOT |
| 🔢 **API Version** | v1 |
| 📘 **Live API Docs** | `https://stockxpress.duckdns.org/swagger-ui/index.html` |
| 🏢 **Author / Org** | TechFourgeBuild |
| 🕒 **Last Updated** | August 2026 |

---

## 📑 Table of Contents

1. [📋 Executive Summary](#1-executive-summary)
2. [🏗️ System Architecture](#2-system-architecture)
3. [🧰 Technology Stack](#3-technology-stack)
4. [📁 Project Structure](#4-project-structure)
5. [🗂️ Domain Model](#5-domain-model)
6. [🔐 Security Architecture](#6-security-architecture)
7. [📡 API Reference](#7-api-reference)
8. [⚠️ Error Handling](#8-error-handling)
9. [🔄 Concurrency & Data Integrity](#9-concurrency--data-integrity)
10. [🔧 Configuration & Environment Variables](#10-configuration--environment-variables)
11. [💻 Local Development Setup](#11-local-development-setup)
12. [🐳 Containerization](#12-containerization)
13. [☁️ Cloud Deployment Architecture (GCP)](#13-cloud-deployment-architecture-gcp)
14. [🔁 CI/CD & Version Control](#14-cicd--version-control)
15. [📊 Monitoring, Maintenance & Cost](#15-monitoring-maintenance--cost)
16. [📎 Appendix](#16-appendix)

---

## 1. 📋 Executive Summary

StockXpress is a **B2B Inventory & Warehouse Management System** built as a RESTful backend service using Spring Boot. It provides a secure, role-based platform for managing product catalogs, order lifecycles, inventory audit trails, and administrative operations for warehouse/retail businesses.

### ✨ Core Capabilities

| Capability | Description |
|---|---|
| **Authentication & Authorization** | Stateless JWT-based authentication with short-lived access tokens and long-lived, HttpOnly-cookie-backed refresh tokens |
| **Role-Based Access Control (RBAC)** | Three-tier role hierarchy — `CUSTOMER`, `WAREHOUSE_MANAGER`, `ADMIN` — enforced at the route level |
| **Product Management** | Full CRUD operations on products, including stock-level updates |
| **Order Management** | Order placement, cancellation, status transitions, and payment tracking |
| **Inventory Logs** | Immutable audit trail of all stock changes for compliance and traceability |
| **Admin Dashboard** | Aggregated statistics and centralized user management for administrators |
| **Concurrency Safety** | Optimistic locking (JPA `@Version`) combined with `ReentrantLock` and Spring Retry to prevent overselling and race conditions during concurrent stock updates |
| **API Documentation** | Auto-generated, interactive OpenAPI 3.1 specification via springdoc, served through Swagger UI |

### 🧭 Design Principles

- **Stateless services** — no server-side session state; horizontally scalable by design.
- **Separation of concerns** — strict layering across `controller → service → repository → entity`, with DTOs isolating the API contract from persistence models.
- **Fail-safe defaults** — every endpoint outside an explicit allow-list requires authentication; role checks are enforced centrally in the security filter chain rather than scattered across controllers.
- **Externalized configuration** — all secrets and environment-specific values (database credentials, JWT signing keys) are injected via environment variables, never hardcoded.

---

## 2. 🏗️ System Architecture

### 2.1 🖼️ High-Level Component Diagram

<img width="1060" height="654" alt="image" src="https://github.com/user-attachments/assets/72dbfc04-701d-474d-89c3-cf41d0eab9f6" />

### 2.2 🔃 Request Lifecycle

1. Client sends an HTTPS request to `stockxpress.duckdns.org`.
2. **Nginx** terminates TLS and reverse-proxies the request over plain HTTP to the Spring Boot container on `127.0.0.1:8081` (bound to localhost only — not directly internet-reachable).
3. Spring Security's **`JwtAuthenticationFilter`** intercepts the request, validates the `Authorization: Bearer <token>` header (if present), and populates the security context.
4. The request is routed to the appropriate **controller**, which delegates business logic to a **service**, which in turn interacts with a **repository** (Spring Data JPA) backed by PostgreSQL.
5. The response is serialized to JSON and returned through the same chain back to the client.

### 2.3 🧱 Layered Architecture

<img width="1472" height="800" alt="image" src="https://github.com/user-attachments/assets/6e3a250e-9c4c-4c0c-8020-cf459bffc764" />

---

## 3. 🧰 Technology Stack

### 3.1 ⚙️ Core Framework

| Component | Version | Purpose |
|---|---|---|
| Java | 21 (LTS) | Language runtime |
| Spring Boot | 4.1.0 | Application framework |
| Spring Boot Starter Data JPA | (managed) | ORM / persistence abstraction |
| Spring Boot Starter Security | (managed) | Authentication & authorization framework |
| Spring Boot Starter Web MVC | (managed) | REST controller support |
| Spring Boot Starter Validation | (managed) | Bean Validation (`jakarta.validation`) for request DTOs |
| Spring AOP (`spring-aspects`) | (managed) | Enables `@EnableRetry` proxy support |
| Spring Retry | 2.0.5 | Declarative retry logic for transient failures during concurrent operations |

### 3.2 🗄️ Persistence

| Component | Purpose |
|---|---|
| PostgreSQL (JDBC driver) | Relational database engine |
| Aiven Cloud PostgreSQL | Managed, cloud-hosted database provider (v17.10) |
| Hibernate ORM | JPA provider, auto-selected `PostgreSQLDialect` |

### 3.3 🔑 Security & Authentication

| Component | Version | Purpose |
|---|---|---|
| `jjwt-api` | 0.12.6 | JWT creation/parsing API |
| `jjwt-impl` | 0.12.6 | JJWT runtime implementation |
| `jjwt-jackson` | 0.12.6 | Jackson-based JSON serialization for JWT claims |
| BCrypt (Spring Security) | — | Password hashing (12 rounds) |

### 3.4 📘 API Documentation

| Component | Version | Purpose |
|---|---|---|
| springdoc-openapi-starter-webmvc-ui | 2.8.5 | Auto-generates OpenAPI 3.1 spec + Swagger UI |

### 3.5 🛠️ Developer Tooling

| Component | Purpose |
|---|---|
| Lombok | Boilerplate reduction (getters/setters/builders) — excluded from the final Spring Boot fat jar |
| Maven Wrapper (`mvnw`) | Reproducible builds without requiring a local Maven install |

### 3.6 🧪 Testing

| Component | Purpose |
|---|---|
| Spring Boot Starter Data JPA Test | Repository-layer test slices |
| Spring Boot Starter Security Test | Security context mocking for controller tests |
| Spring Boot Starter Web MVC Test | `MockMvc`-based controller testing |

### 3.7 🚀 Infrastructure & Deployment

| Component | Purpose |
|---|---|
| Docker | Containerization (multi-stage build) |
| Docker Hub | Container image registry |
| Nginx | Reverse proxy, TLS termination |
| Certbot / Let's Encrypt | Free, auto-renewing SSL certificates |
| DuckDNS | Free dynamic DNS (domain resolution for the VM's static IP) |
| Google Cloud Platform — Compute Engine | Hosting (`e2-micro`, Always Free tier) |
| Git / GitHub | Version control |

---

## 4. 📁 Project Structure

```
StockXpress/
├── .mvn/wrapper/                       # Maven wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/com.sxp.StockXpress/
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java          # Swagger/OpenAPI metadata configuration
│   │   │   │   └── SecurityConfig.java         # Spring Security filter chain, CORS, RBAC rules
│   │   │   ├── controller/
│   │   │   │   ├── AdminController.java        # Admin dashboard & user management
│   │   │   │   ├── AuthController.java         # Registration, login, token refresh, logout
│   │   │   │   ├── InventoryController.java    # Inventory audit log endpoints
│   │   │   │   ├── OrderController.java        # Order lifecycle endpoints
│   │   │   │   └── ProductController.java      # Product catalog & stock management
│   │   │   ├── dto/                            # Request/response contracts (15 DTOs)
│   │   │   ├── entity/                         # JPA entities (6 entities)
│   │   │   ├── enums/                          # Domain enumerations (4 enums)
│   │   │   ├── exception/                      # Custom exceptions + global handler
│   │   │   ├── repository/                     # Spring Data JPA repositories (6)
│   │   │   ├── security/
│   │   │   │   ├── jwt/                        # JWT filter, entry point, util
│   │   │   │   └── service/                    # CustomUserDetailsService
│   │   │   ├── service/                        # Business logic layer (6 services)
│   │   │   ├── util/
│   │   │   │   └── CookieUtil.java             # Refresh-token cookie management
│   │   │   └── StockXpressApplication.java     # Application entry point
│   │   └── resources/
│   │       └── application.properties          # Externalized configuration
│   └── test/java/com.sxp.StockXpress/
│       └── StockXpressApplicationTests.java
├── .dockerignore
├── .gitattributes                      # Enforces LF line endings for mvnw/shell scripts
├── .gitignore
├── Dockerfile                          # Multi-stage container build
├── mvnw / mvnw.cmd                     # Maven wrapper scripts
└── pom.xml                             # Maven project descriptor
```

---

## 5. 🗂️ Domain Model

### 5.1 🧬 Entities

| Entity | Responsibility |
|---|---|
| `User` | Represents a system account — customer, warehouse manager, or admin |
| `Product` | Represents a catalog item, including current stock level |
| `Order` | Represents a customer purchase, with a status/payment lifecycle |
| `OrderItem` | Line item within an order, linking a `Product` to a quantity/price |
| `InventoryLog` | Immutable audit record of every stock change (who, what, when, why) |
| `RefreshToken` | Persisted refresh-token record backing the long-lived login session |

### 5.2 🔤 Enumerations

| Enum | Values (representative) | Used By |
|---|---|---|
| `Role` | `CUSTOMER`, `WAREHOUSE_MANAGER`, `ADMIN` | `User` — drives RBAC decisions |
| `OrderStatus` | e.g. `PENDING`, `CONFIRMED`, `SHIPPED`, `CANCELLED`, `COMPLETED` | `Order` — order lifecycle state |
| `PaymentStatus` | e.g. `PENDING`, `PAID`, `FAILED`, `REFUNDED` | `Order` — payment lifecycle state |
| `ChangeType` | e.g. `ADDED`, `DEDUCTED`, `ADJUSTED` | `InventoryLog` — classifies the nature of a stock change |

> ℹ️ Exact enum constant sets are defined in `src/main/java/com/sxp/StockXpress/enums/`. Refer to the source or the live OpenAPI schema for the authoritative list.

### 5.3 🔗 Entity Relationship Overview

```
User (1) ──────< Order (M)
Order (1) ──────< OrderItem (M) >────── (1) Product
Product (1) ────< InventoryLog (M)
User (1) ───────< RefreshToken (M)
```

---

## 6. 🔐 Security Architecture

### 6.1 🪪 Authentication Model

StockXpress uses a **dual-token JWT strategy**:

| Token | Lifetime | Storage | Purpose |
|---|---|---|---|
| **Access Token** | 15 minutes (`900000` ms) | Returned in response body, sent as `Authorization: Bearer <token>` | Authorizes individual API requests |
| **Refresh Token** | 30 days (`2592000000` ms) | `HttpOnly`, `Secure`, `SameSite=None` cookie | Used solely to obtain a new access token via `/api/v1/auth/refresh-token` |

**Why `SameSite=None`:** The frontend (Vercel) and backend (custom domain) are served from different origins. Cookies marked `Strict` or `Lax` are withheld by browsers on cross-site requests; `SameSite=None` (paired mandatorily with `Secure=true`, since the API is served over HTTPS) permits the refresh-token cookie to be sent on legitimate cross-origin requests from the frontend.

The refresh-token cookie is scoped to `path=/api/v1/auth`, minimizing its exposure to only the endpoints that need it.

### 6.2 🧷 Session Model

The application is **fully stateless** (`SessionCreationPolicy.STATELESS`) — no server-side HTTP session is created. All authorization decisions are derived from the JWT presented on each request, making the service horizontally scalable without sticky sessions.

### 6.3 🛡️ Authorization Rules (Role-Based Access Control)

Enforced centrally in `SecurityConfig.java`'s `SecurityFilterChain`:

| Route Pattern | Access Level |
|---|---|
| `POST /api/v1/auth/register` | 🟢 Public |
| `POST /api/v1/auth/login` | 🟢 Public |
| `POST /api/v1/auth/refresh-token` | 🟢 Public (relies on the HttpOnly cookie, not a bearer token) |
| `/api/v1/public/**` | 🟢 Public |
| `/swagger-ui/**`, `/api-docs/**`, `/v3/api-docs/**` | 🟢 Public (API documentation) |
| `POST /api/v1/auth/logout` | 🟡 Any authenticated user |
| `/api/v1/admin/**` | 🔴 `ADMIN` only |
| `/api/v1/warehouseManager/**` | 🟠 `WAREHOUSE_MANAGER` or `ADMIN` |
| All other routes | 🟡 Any authenticated user (default-deny for unauthenticated requests) |

### 6.4 🌐 CORS Policy

Configured via a `CorsConfigurationSource` bean:

- **Allowed origins:** explicit allow-list of local dev servers (`http://localhost:5173`, `:5174`) and the production/preview Vercel deployment domains (matched via `setAllowedOriginPatterns()` to support wildcard preview URLs)
- **Allowed methods:** `GET, POST, PUT, PATCH, DELETE, OPTIONS`
- **Allowed headers:** `Authorization, Content-Type, Accept, X-Requested-With, Cookie, Set-Cookie`
- **Credentials:** enabled (`allowCredentials(true)`), required for the cookie-based refresh flow
- **Preflight cache:** 3600 seconds

### 6.5 🔒 Password Storage

Passwords are hashed with **BCrypt at 12 rounds** (`BCryptPasswordEncoder(12)`) before persistence — plaintext passwords are never stored or logged.

### 6.6 🚧 Exception Entry Point

Unauthenticated requests to protected routes are intercepted by a custom `JwtAuthEntryPoint`, returning a structured `401 Unauthorized` JSON response rather than the default container error page.

---

## 7. 📡 API Reference

**Base URL (production):** `https://stockxpress.duckdns.org/api/v1`
**Base URL (local):** `http://localhost:8080/api/v1`

> 📘 **Authoritative source:** The definitive, always-current API contract — including exact request/response schemas — is published as a live, interactive OpenAPI 3.1 specification at `/swagger-ui/index.html` (raw JSON at `/api-docs`). The tables below summarize the documented endpoint groups by controller and are intended as a navigable reference; consult Swagger UI for field-level schema details before integrating.

### 7.1 🔑 Authentication — `AuthController`

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Register a new customer account |
| `POST` | `/api/v1/auth/login` | Public | Authenticate and receive an access token + refresh-token cookie |
| `POST` | `/api/v1/auth/refresh-token` | Public (cookie-based) | Exchange a valid refresh-token cookie for a new access token |
| `POST` | `/api/v1/auth/logout` | Authenticated | Invalidate the current refresh token and clear the cookie |

**Related DTOs:** `RegisterRequest`, `LoginRequest`, `AuthResponse`, `AdminRegisterRequest`

### 7.2 📦 Products — `ProductController`

| Method | Endpoint (representative) | Access | Description |
|---|---|---|---|
| `GET` | `/api/v1/products` | Authenticated | List/search the product catalog |
| `GET` | `/api/v1/products/{id}` | Authenticated | Retrieve a single product |
| `POST` | `/api/v1/products` | `WAREHOUSE_MANAGER`, `ADMIN` | Create a new product |
| `PUT` | `/api/v1/products/{id}` | `WAREHOUSE_MANAGER`, `ADMIN` | Update product details |
| `PATCH` | `/api/v1/products/{id}/stock` | `WAREHOUSE_MANAGER`, `ADMIN` | Adjust stock quantity (triggers an `InventoryLog` entry) |
| `DELETE` | `/api/v1/products/{id}` | `ADMIN` | Remove a product |

**Related DTOs:** `ProductRequest`, `ProductResponse`, `StockUpdateRequest`

### 7.3 🛒 Orders — `OrderController`

| Method | Endpoint (representative) | Access | Description |
|---|---|---|---|
| `POST` | `/api/v1/orders` | Authenticated | Place a new order (one or more line items) |
| `GET` | `/api/v1/orders` | Authenticated | List orders (scoped to the requesting user, or all for staff roles) |
| `GET` | `/api/v1/orders/{id}` | Authenticated | Retrieve order details |
| `PATCH` | `/api/v1/orders/{id}/status` | `WAREHOUSE_MANAGER`, `ADMIN` | Transition order status (e.g. confirm, ship, complete) |
| `PATCH` | `/api/v1/orders/{id}/cancel` | Authenticated (owner) or staff | Cancel an order |
| `PATCH` | `/api/v1/orders/{id}/payment` | Authenticated / staff | Update payment status |

**Related DTOs:** `OrderRequest`, `OrderResponse`, `OrderItemRequest`, `OrderItemResponse`

### 7.4 📊 Inventory — `InventoryController`

| Method | Endpoint (representative) | Access | Description |
|---|---|---|---|
| `GET` | `/api/v1/inventory/logs` | `WAREHOUSE_MANAGER`, `ADMIN` | Retrieve the inventory audit trail |
| `GET` | `/api/v1/inventory/logs/{productId}` | `WAREHOUSE_MANAGER`, `ADMIN` | Retrieve audit history for a specific product |

**Related DTOs:** `InventoryLogResponse`

### 7.5 👑 Admin — `AdminController`

| Method | Endpoint (representative) | Access | Description |
|---|---|---|---|
| `GET` | `/api/v1/admin/dashboard` | `ADMIN` | Aggregated dashboard statistics (revenue, order counts, low-stock alerts, etc.) |
| `GET` | `/api/v1/admin/users` | `ADMIN` | List all system users |
| `GET` | `/api/v1/admin/users/{id}` | `ADMIN` | Retrieve a specific user's profile |
| `POST` | `/api/v1/admin/register` | `ADMIN` | Register a privileged account (warehouse manager / admin) |
| `PATCH` | `/api/v1/admin/users/{id}/role` | `ADMIN` | Modify a user's assigned role |

**Related DTOs:** `DashboardStatsResponse`, `UserResponse`, `UserProfileResponse`, `AdminRegisterRequest`

---

## 8. ⚠️ Error Handling

Centralized via `GlobalExceptionHandler` (`@RestControllerAdvice`), mapping domain exceptions to consistent, structured HTTP error responses.

| Exception | Typical HTTP Status | Scenario |
|---|---|---|
| `ResourceNotFoundException` | `404 Not Found` | Requested entity (product, order, user) does not exist |
| `DuplicateResourceException` | `409 Conflict` | Attempting to create a resource that violates a uniqueness constraint (e.g. duplicate email on registration) |
| `UnauthorizedAccessException` | `403 Forbidden` | Authenticated user lacks permission for the requested operation |
| `TokenRefreshException` | `403 Forbidden` | Refresh token is missing, expired, or invalid |
| `BusinessException` | `400 Bad Request` | Generic domain/business-rule violation (e.g. insufficient stock) |
| Bean Validation failures | `400 Bad Request` | Request DTO fails `jakarta.validation` constraints |

All error responses follow a consistent JSON shape (timestamp, status, error code/message, and — where applicable — field-level validation details), enabling predictable client-side error handling.

---

## 9. 🔄 Concurrency & Data Integrity

Given the inventory-management domain, preventing **overselling** and **lost updates** under concurrent load is a first-class concern.

### 9.1 🔓 Optimistic Locking

`Product` (and other mutable entities subject to concurrent writes) uses JPA's `@Version` field. Concurrent updates to the same row are detected at commit time; a conflicting write raises an `OptimisticLockException`, which is caught and translated into a retry or a client-facing conflict response.

### 9.2 🔁 Retry Strategy

`spring-retry` (with `spring-aspects` enabling the AOP proxy required for `@EnableRetry`) is used to automatically retry operations that fail due to transient optimistic-lock conflicts, reducing the need for manual client-side retry logic on high-contention endpoints (e.g. simultaneous stock decrements during a flash sale scenario).

### 9.3 🔐 Explicit Locking

A `ReentrantLock` is additionally used at the service layer for critical sections where JVM-level mutual exclusion is required alongside database-level optimistic locking — providing defense-in-depth for the highest-contention stock-adjustment code paths.

### 9.4 🧾 Audit Trail

Every stock mutation is recorded as an immutable `InventoryLog` entry (linked to the acting user, the `ChangeType`, and a timestamp), providing a complete, tamper-evident history for compliance and dispute resolution.

---

## 10. 🔧 Configuration & Environment Variables

All environment-specific and sensitive values are externalized. Locally, these are supplied via a `.env` file (loaded through the IntelliJ **EnvFile** plugin or an equivalent mechanism); in the containerized deployment, they are supplied via `docker run --env-file`.

| Variable | Description |
|---|---|
| `JWT_ACCESS_SECRET` | Signing key for access tokens |
| `JWT_ACCESS_EXPIRATION` | Access token TTL in milliseconds (default: `900000` — 15 min) |
| `JWT_REFRESH_SECRET` | Signing key for refresh tokens |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL in milliseconds (default: `2592000000` — 30 days) |
| `DB_URL` / `SPRING_DATASOURCE_URL` | JDBC connection string for the PostgreSQL database |
| `DB_USERNAME` / `SPRING_DATASOURCE_USERNAME` | Database username |
| `DB_PASSWORD` / `SPRING_DATASOURCE_PASSWORD` | Database password |
| `DDL_AUTO` / `SPRING_JPA_HIBERNATE_DDL_AUTO` | Hibernate schema management strategy (default: `update`) |
| `SHOW_SQL` / `SPRING_JPA_SHOW_SQL` | Toggles SQL statement logging |

> 📌 **Never commit `.env` to version control.** It is explicitly excluded via `.gitignore`. Secrets should be rotated immediately if ever accidentally exposed (committed, pasted into logs/chat, etc.).

---

## 11. 💻 Local Development Setup

### ✅ Prerequisites
- JDK 21
- Docker Desktop (for containerized local runs) or a local PostgreSQL instance
- IntelliJ IDEA (or any IDE with Spring Boot support) with the **EnvFile** plugin, for loading `.env` during local runs

### 🪜 Steps

```bash
# 1. Clone the repository
git clone https://github.com/TechFourgeBuild/StockXpress-backend.git
cd StockXpress-backend

# 2. Create a local .env file with the variables listed in Section 10

# 3. Build
./mvnw clean package -DskipTests

# 4. Run
./mvnw spring-boot:run
# — or, with the EnvFile plugin configured in your IDE's Run Configuration —
# run StockXpressApplication.java directly
```

Once running, the API is available at `http://localhost:8080`, with interactive documentation at `http://localhost:8080/swagger-ui/index.html`.

---

## 12. 🐳 Containerization

### 12.1 🐳 Multi-Stage Dockerfile

```dockerfile
# ---------- Stage 1: Build ----------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw package -DskipTests

# ---------- Stage 2: Run ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --gid 1001 appuser
COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

### 12.2 💡 Design Rationale

| Decision | Rationale |
|---|---|
| Multi-stage build | Final image contains only the JRE + compiled jar — no JDK, Maven wrapper, or source excluded from the runtime layer, minimizing image size and attack surface |
| `eclipse-temurin` base image | Actively maintained OpenJDK distribution (the `openjdk` official image is deprecated) |
| Non-root `appuser` | Container does not run as root — reduces the blast radius of a potential container-escape vulnerability |
| `-XX:MaxRAMPercentage=75.0` | Caps JVM heap relative to the container's available memory, preventing OOM kills on memory-constrained hosts (e.g. a 1 GB free-tier VM) |
| Wildcard `*.jar` in `COPY` | Avoids hardcoding the artifact filename/version, so the Dockerfile survives `pom.xml` version bumps without modification |

### 12.3 📤 Registry

The built image is published to Docker Hub as a public repository, enabling pull-based deployment on any host without rebuilding from source:

```bash
docker build -t <dockerhub-username>/stockxpress-backend .
docker push <dockerhub-username>/stockxpress-backend
```

---

## 13. ☁️ Cloud Deployment Architecture (GCP)

The backend is deployed on **Google Cloud Platform's Always Free tier**, using a Compute Engine VM as the host, with Nginx and Let's Encrypt providing a production-grade HTTPS front door — at zero ongoing infrastructure cost.

### 13.1 🖥️ Compute Resource

| Setting | Value |
|---|---|
| Instance type | `e2-micro` (0.25–2 vCPU, 1 GB RAM) — Always Free tier eligible |
| Region | `us-central1` (one of three Always Free eligible regions, alongside `us-west1` and `us-east1`) |
| OS | Ubuntu 22.04 LTS |
| Boot disk | 30 GB Standard persistent disk (the only disk type covered by the free tier) |
| External IP | Static (promoted from ephemeral, at no additional cost while attached to a running instance) |

### 13.2 🌐 Networking & Reverse Proxy

- **Firewall:** HTTP (80) and HTTPS (443) ingress explicitly allowed via VPC firewall rules (`default-allow-http`, `default-allow-https`); SSH (22) restricted to Google's IAP range.
- **Nginx** listens on ports 80/443 and reverse-proxies to the Spring Boot container, which is bound to `127.0.0.1:8081` — not directly reachable from the internet, reducing the attack surface to a single, well-understood ingress point.

### 13.3 🔏 Domain & TLS

- **Domain:** a free subdomain via **DuckDNS** (`stockxpress.duckdns.org`), pointed at the VM's static IP. (A domain name is required because Let's Encrypt cannot issue certificates for bare IP addresses.)
- **TLS certificate:** issued and auto-renewed via **Certbot** (Let's Encrypt), integrated directly into the Nginx configuration.

### 13.4 🐳 Container Runtime

```bash
docker run -d \
  -p 127.0.0.1:8081:8080 \
  --name stockxpress \
  --restart unless-stopped \
  --env-file .env \
  <dockerhub-username>/stockxpress-backend
```

`--restart unless-stopped` ensures the container automatically recovers after a VM reboot without manual intervention.

### 13.5 🛠️ VM Stability Hardening

Two production-hardening measures were applied after observing resource contention on the 1 GB RAM instance:

| Measure | Reason |
|---|---|
| **1 GB swap file** (`/swapfile`, permanently enabled via `/etc/fstab`) | Provides a memory buffer during high-pressure moments (e.g. application restarts), preventing multi-minute startup stalls |
| **`snapd` service disabled/masked** | The Snap package manager daemon is unused in this deployment (all tooling installed via `apt`) and was observed to crash-loop under low-memory conditions, consuming CPU and delaying SSH connectivity; permanently disabling it eliminated the issue |

### 13.6 🗺️ Deployment Topology Summary

```
Internet
   │  HTTPS :443 / HTTP :80
   ▼
[ Nginx ]  ── reverse proxy + TLS termination
   │  HTTP :8081 (localhost only)
   ▼
[ Docker: Spring Boot container ]  ── :8080 internal
   │  JDBC over TLS
   ▼
[ Aiven-managed PostgreSQL ]
```

### 13.7 💰 Cost Profile

All components are provisioned within free-tier limits:

| Resource | Free Tier Allowance | Usage |
|---|---|---|
| ✅ e2-micro compute | 1 instance/month, eligible regions | 1 instance, `us-central1` |
| ✅ Standard persistent disk | 30 GB-months | 30 GB |
| ✅ Network egress | 1 GB/month (North America) | Well within limits for JSON API traffic |
| ✅ Domain (DuckDNS) | Free, indefinitely | 1 subdomain |
| ✅ TLS certificate (Let's Encrypt) | Free, auto-renewing every 90 days | 1 certificate |

**💰 Net infrastructure cost: ₹0/month**, sustainable indefinitely under Google Cloud's Always Free policy (subject to Google's published terms, which may evolve over time).

---

## 14. 🔁 CI/CD & Version Control

### 14.1 📚 Source Control

- Hosted on GitHub: [TechFourgeBuild/StockXpress-backend](https://github.com/TechFourgeBuild/StockXpress-backend)
- `.gitattributes` enforces LF (Unix) line endings on `mvnw` and `*.sh` files — necessary because Windows-authored commits can otherwise introduce CRLF endings that break shell script execution inside the Linux-based Docker build stage.
- `.gitignore` excludes build artifacts (`target/`), IDE metadata, and — critically — the `.env` file containing local secrets.

### 14.2 🔄 Current Deployment Model

Deployment is currently a **manual pull-based release process**:

1. Code changes are built and tagged locally (`docker build`).
2. The image is pushed to Docker Hub (`docker push`).
3. The production VM pulls the updated image (`docker pull`) and recreates the running container (`docker stop && docker rm && docker run`).

### 14.3 🔮 Future CI/CD Recommendation

For teams scaling beyond a single maintainer, this manual flow is a natural candidate for automation via **GitHub Actions**: triggering a build-and-push job to Docker Hub on merge to `main`, followed by an SSH-based deployment step (or migrating to a managed container platform such as Cloud Run) to eliminate manual redeploy steps entirely.

---

## 15. 📊 Monitoring, Maintenance & Cost

### 15.1 💓 Health Checks

- `docker ps` / `docker logs -f stockxpress` — container status and live application logs
- `curl -I https://stockxpress.duckdns.org` — quick external reachability and TLS check
- `sudo systemctl status nginx` — reverse proxy health

### 15.2 🔏 Certificate Renewal

Fully automated by Certbot's scheduled systemd timer — no manual action required. Certificates are renewed automatically well ahead of their 90-day expiry.

### 15.3 💳 Billing Safety Net

A **Google Cloud Budget Alert** is configured on the billing account to send email notifications at 50%, 90%, and 100% of a defined spend threshold — providing visibility even though the account operates within Google's Free Trial safeguards (which stop resources rather than auto-billing beyond the trial's credit/duration, unless the account is explicitly upgraded to a paid billing account).

### 15.4 📈 Resource Monitoring

```bash
free -h                       # RAM / swap utilization
docker stats --no-stream      # per-container CPU/memory snapshot
```

Recommended cadence: periodic manual checks, or promotion to Google Cloud Monitoring / a managed APM tool if the workload grows beyond what ad-hoc `ssh` diagnostics comfortably support.

---

## 16. 📎 Appendix

### 16.1 🎮 Full Controller Index

| Controller | Domain |
|---|---|
| `AuthController` | Authentication & session lifecycle |
| `ProductController` | Product catalog & stock |
| `OrderController` | Order lifecycle & payments |
| `InventoryController` | Inventory audit trail |
| `AdminController` | Administrative dashboard & user management |

### 16.2 📇 Full DTO Index

`AdminRegisterRequest`, `AuthResponse`, `DashboardStatsResponse`, `InventoryLogResponse`, `LoginRequest`, `OrderItemRequest`, `OrderItemResponse`, `OrderRequest`, `OrderResponse`, `ProductRequest`, `ProductResponse`, `RegisterRequest`, `StockUpdateRequest`, `UserProfileResponse`, `UserResponse`

### 16.3 🚨 Full Exception Index

`BusinessException`, `DuplicateResourceException`, `GlobalExceptionHandler`, `ResourceNotFoundException`, `TokenRefreshException`, `UnauthorizedAccessException`

### 16.4 📖 Glossary

| Term | Definition |
|---|---|
| **RBAC** | Role-Based Access Control |
| **JWT** | JSON Web Token |
| **DTO** | Data Transfer Object |
| **ORM** | Object-Relational Mapping |
| **TLS** | Transport Layer Security |
| **Optimistic Locking** | Concurrency control strategy that detects conflicting writes at commit time rather than locking rows preemptively |

---

📄 This document reflects the StockXpress Backend production architecture and implementation as of August 2026. For the latest API contract, refer to the live OpenAPI/Swagger documentation.
© 2026 TechFourgeBuild. All rights reserved.
