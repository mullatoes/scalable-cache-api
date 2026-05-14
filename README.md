# Scalable Cache API

A production-shaped Spring Boot service exploring caching, JWT authentication, containerization, and orchestration on top of a small `Product` REST API. The domain is intentionally minimal so the focus stays on the cross-cutting concerns: Redis-backed read-through cache, role-based access control with rotating tokens, and the Docker / Kubernetes plumbing to run it.

## What it does

- Exposes a CRUD REST API for `Product` resources backed by PostgreSQL.
- Caches reads in Redis with Spring Cache annotations (`@Cacheable`, `@CachePut`, `@CacheEvict`) and a 10-minute TTL.
- Authenticates users with JWT access tokens + opaque refresh tokens persisted in Postgres.
- Enforces role-based access: `USER` can read products, `ADMIN` can create / update / delete them.
- Revokes access tokens on logout via a Redis blacklist (TTL = remaining token validity) and revokes refresh tokens in the database.
- Seeds a default `ADMIN` account on first boot.
- Ships with a multi-stage `Dockerfile`, `docker-compose` for the full stack, and Kubernetes manifests (Deployment + Service + ConfigMap + Secret) for the app, Postgres, and Redis.

## Stack

| Layer        | Tech                                       |
|--------------|--------------------------------------------|
| Language     | Java 21                                    |
| Framework    | Spring Boot 4.0.x                          |
| Security     | Spring Security 6 + JJWT 0.12              |
| Persistence  | Spring Data JPA + PostgreSQL 16            |
| Cache        | Spring Cache + Redis 7 (Spring Data Redis 4)|
| Serializer   | Jackson 3 (`tools.jackson`)                |
| Validation   | Jakarta Bean Validation                    |
| Build        | Maven (wrapper included)                   |
| Observability| Spring Boot Actuator                       |

## Project layout

```
src/main/java/com/jdevs/scalablecacheapi
├── ScalableCacheApiApplication.java
├── auth/          # AuthController, AuthService, AuthResponse
├── config/        # RedisCacheConfig, AdminSeeder
├── controller/    # ProductController
├── dto/           # ProductRequest/Response, RegisterRequest, LoginRequest, RefreshTokenRequest, LogoutRequest, MessageResponse
├── entity/        # Product, RefreshToken
├── exception/     # GlobalExceptionHandler, ResourceNotFoundException, ErrorResponse
├── repository/    # ProductRepository, RefreshTokenRepository
├── security/      # SecurityConfig, JwtService, JwtAuthenticationFilter, TokenBlacklistService, entry/access handlers
├── service/       # ProductService(+Impl), RefreshTokenService
└── user/          # AppUser, AppUserRepository, Role
```

## Prerequisites

- JDK 21
- One of:
   - Docker + Docker Compose (recommended), OR
   - PostgreSQL 14+ on `5432` and Redis 6+ on `6379` running locally

## Run it

### Option A — Docker Compose (one command, full stack)

```bash
docker compose up --build
```

This builds the app image, starts Postgres (host port `5434`), Redis (`6379`), and the API (`8080`), and seeds the default admin on first boot. Tear down with:

```bash
docker compose down       # keep data
docker compose down -v    # wipe Postgres volume
```

### Option B — Local JVM with local services

1. Start Postgres and Redis (or just the dependencies from Compose: `docker compose up postgres redis`).
2. Create the database (skip if Postgres auto-created it):

   ```sql
   CREATE DATABASE scalable_cache_db;
   ```

3. Adjust credentials in [application.yaml](src/main/resources/application.yaml) or export overrides (see [Configuration](#configuration)).
4. Run the app:

   ```bash
   ./mvnw spring-boot:run
   ```

The service listens on `http://localhost:8080`.

### Option C — Kubernetes (kind / minikube / Docker Desktop)

```bash
# Build the image into the local daemon first
docker build -t scalable-cache-api:1.0.5 .

# Then apply manifests
kubectl apply -f k8s/
```

The app is exposed via NodePort `30082` (`http://localhost:30082` on Docker Desktop / kind with port mapping). See [k8s/](k8s/) for Deployments, Services, ConfigMap, and Secret. Tear down with `kubectl delete -f k8s/`.

## Configuration

All settings have sensible defaults for local dev. Override via environment variables:

| Variable                       | Default                                                    | Notes                            |
|--------------------------------|------------------------------------------------------------|----------------------------------|
| `DB_URL`                       | `jdbc:postgresql://localhost:5432/scalable_cache_db`       |                                  |
| `DB_USERNAME` / `DB_PASSWORD`  | `postgres` / `Admin@123`                                   |                                  |
| `REDIS_HOST` / `REDIS_PORT`    | `localhost` / `6379`                                       |                                  |
| `SERVER_PORT`                  | `8080`                                                     |                                  |
| `JWT_SECRET`                   | (a long dev placeholder)                                   | **Override in any real deployment** |
| `JWT_EXPIRATION_MS`            | `900000` (15 min)                                          | Access-token TTL                 |
| `REFRESH_TOKEN_EXPIRATION_MS`  | `604800000` (7 days)                                       | Refresh-token TTL                |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@test.com` / `password123`                         | Used by `AdminSeeder` on first boot |

## Authentication model

- **Register** (`POST /api/v1/auth/register`) — creates a `USER`, returns access + refresh tokens.
- **Login** (`POST /api/v1/auth/login`) — returns access + refresh tokens.
- **Refresh** (`POST /api/v1/auth/refresh`) — exchanges a valid refresh token for a new access token.
- **Logout** (`POST /api/v1/auth/logout`, authenticated) — revokes the refresh token in Postgres and blacklists the bearer access token in Redis until its natural expiry.

The `JwtAuthenticationFilter` validates every request: it checks the signature, expiry, and the Redis blacklist before populating the security context.

### Endpoints & required roles

| Method | Path                         | Auth                  |
|--------|------------------------------|-----------------------|
| POST   | `/api/v1/auth/register`      | public                |
| POST   | `/api/v1/auth/login`         | public                |
| POST   | `/api/v1/auth/refresh`       | public                |
| POST   | `/api/v1/auth/logout`        | authenticated         |
| GET    | `/api/v1/products`           | `USER` or `ADMIN`     |
| GET    | `/api/v1/products/{id}`      | `USER` or `ADMIN`     |
| POST   | `/api/v1/products`           | `ADMIN`               |
| PUT    | `/api/v1/products/{id}`      | `ADMIN`               |
| DELETE | `/api/v1/products/{id}`      | `ADMIN`               |
| GET    | `/actuator/health/**`        | public                |
| GET    | `/actuator/info`, `/metrics` | authenticated         |

### Product caching

| Endpoint                | Cache behavior                                             |
|-------------------------|------------------------------------------------------------|
| `GET /products/{id}`    | `@Cacheable("products")` — read-through, 10-min TTL        |
| `PUT /products/{id}`    | `@CachePut` — refreshes the entry                          |
| `DELETE /products/{id}` | `@CacheEvict` — drops the entry                            |
| `GET /products`         | not cached                                                 |

Cache hits log nothing; cache misses log `Fetching product from database. productId=...` from `ProductServiceImpl`.

## Sample commands

The flow below assumes a freshly started stack. The seeded admin is `admin@test.com` / `password123` (override via `ADMIN_*` env vars).

### 1. Log in as the seeded admin

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@test.com","password":"password123"}'
```

Response:

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "f5c0...",
  "tokenType": "Bearer"
}
```

Stash the tokens in shell vars so the rest of the commands stay short:

```bash
TOKENS=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@test.com","password":"password123"}')
ACCESS=$(echo "$TOKENS"  | sed -E 's/.*"accessToken":"([^"]+)".*/\1/')
REFRESH=$(echo "$TOKENS" | sed -E 's/.*"refreshToken":"([^"]+)".*/\1/')
```

(Or pipe to `jq -r .accessToken` if you have it installed.)

### 2. Register a regular user (optional)

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"fullName":"Jane Doe","email":"jane@example.com","password":"password123"}'
```

A registered user has role `USER` and can read but not modify products.

### 3. Create a product (admin only)

```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $ACCESS" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Mechanical Keyboard","description":"75% hot-swap","price":129.99,"quantityAvailable":25}'
```

Expect `201 Created` with the persisted product (`id`, `createdAt`, …).

### 4. Read a product — cache demo

```bash
# First call — hits Postgres, populates Redis (log: "Fetching product from database...")
curl -H "Authorization: Bearer $ACCESS" http://localhost:8080/api/v1/products/1

# Second call — served from Redis, no DB log line
curl -H "Authorization: Bearer $ACCESS" http://localhost:8080/api/v1/products/1
```

You can confirm directly in Redis:

```bash
docker exec -it scalable_cache_redis redis-cli KEYS 'products::*'
docker exec -it scalable_cache_redis redis-cli GET   'products::1'
```

### 5. Update a product (refreshes the cache via `@CachePut`)

```bash
curl -X PUT http://localhost:8080/api/v1/products/1 \
  -H "Authorization: Bearer $ACCESS" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Mechanical Keyboard v2","description":"RGB","price":149.99,"quantityAvailable":30}'
```

### 6. Delete a product (evicts via `@CacheEvict`)

```bash
curl -X DELETE -H "Authorization: Bearer $ACCESS" http://localhost:8080/api/v1/products/1
```

### 7. Refresh the access token

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH\"}"
```

### 8. Log out

```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $ACCESS" \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH\"}"
```

After this, the access token is blacklisted in Redis (`blacklisted_access_token:<jwt>`) and the refresh token row is marked revoked — re-using either returns `401`.

### 9. Health check (public)

```bash
curl http://localhost:8080/actuator/health
```

### What to expect when things go wrong

| Situation                          | Status | Body shape (from `GlobalExceptionHandler`) |
|-----------------------------------|--------|--------------------------------------------|
| Missing/invalid JWT               | `401`  | `{ "error": "...", "message": "..." }`     |
| Authenticated but wrong role      | `403`  | same                                       |
| Validation failure (bad payload)  | `400`  | field-level errors                         |
| Product not found                 | `404`  | `Resource not found`                       |
| Email already registered          | `400`  | `Email already exists`                     |

The [requests/](requests/) folder has ready-to-run `.http` files for IntelliJ / VS Code REST Client covering the happy path, validation errors, and Actuator.

## Cache notes

- Cache name: `products`, TTL: 10 minutes — see [RedisCacheConfig.java](src/main/java/com/jdevs/scalablecacheapi/config/RedisCacheConfig.java).
- Default typing is enabled so polymorphic JSON round-trips cleanly; a `@class` hint is stored alongside cached values.
- If you change the cached DTO shape or serializer settings, flush Redis (`redis-cli FLUSHDB`) before retesting — old entries written with a different layout won't deserialize.

## Health & metrics

Actuator exposes:

- `GET /actuator/health` — public
- `GET /actuator/health/readiness`, `/actuator/health/liveness` — used by the K8s probes
- `GET /actuator/info`, `/actuator/metrics` — require authentication

## Roadmap

- [x] REST CRUD + JPA persistence
- [x] Redis caching with Spring Cache annotations
- [x] JWT authentication, refresh tokens, role-based authorization, access-token blacklist on logout
- [x] Multi-stage `Dockerfile` + `docker-compose.yml` for the full stack
- [x] Kubernetes manifests for app + Postgres + Redis with probes
- [ ] HPA + resource tuning walkthrough
- [ ] Kafka producer for domain events
- [ ] Kafka Streams consumer for projections / cache sync

## License

Educational / personal use.
