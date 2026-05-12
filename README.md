# Scalable Cache API

A hands-on learning project for building a production-grade Spring Boot service, used as a sandbox to explore caching, authentication, containerization, orchestration, and event streaming as the stack grows.

The base scenario is intentionally small — a `Product` REST API backed by PostgreSQL and Redis — so that each new concept can be layered on top of working code rather than a greenfield rewrite.

## Learning Goals

The repository evolves through these focus areas:

- **Caching** — Redis-backed read-through caching with Spring's `@Cacheable`, `@CachePut`, `@CacheEvict`, TTL configuration, and polymorphic JSON serialization via Jackson 3 / `GenericJacksonJsonRedisSerializer`.
- **Authentication & Authorization** — Spring Security, JWT / OAuth2 flows, method-level security, and protecting the existing endpoints.
- **Docker** — multi-stage `Dockerfile`s, local stacks with `docker-compose` (app + PostgreSQL + Redis), and image hygiene.
- **Kubernetes** — manifests for Deployments, Services, ConfigMaps, Secrets, HPA, probes, and running the same stack on a local cluster (kind / minikube).
- **Kafka streaming** — publishing product domain events, consuming them downstream, and exploring Kafka Streams for projections / cache invalidation as a more decoupled alternative to direct cache writes.

Each area gets added incrementally; the README will be updated as the stack grows.

## Current Stack

| Layer        | Tech                                      |
|--------------|-------------------------------------------|
| Language     | Java 21                                   |
| Framework    | Spring Boot 4.0.x                         |
| Persistence  | Spring Data JPA + PostgreSQL              |
| Cache        | Spring Cache + Redis (Spring Data Redis 4)|
| Serializer   | Jackson 3 (`tools.jackson`)               |
| Validation   | Jakarta Bean Validation                   |
| Build        | Maven (wrapper included)                  |
| Observability| Spring Boot Actuator                      |

## Project Layout

```
src/main/java/com/jdevs/scalablecacheapi
├── config/        # RedisCacheConfig — cache manager + serializer wiring
├── controller/    # ProductController — REST endpoints
├── dto/           # ProductRequest / ProductResponse records
├── entity/        # Product JPA entity
├── exception/     # GlobalExceptionHandler + custom exceptions
├── repository/    # ProductRepository (Spring Data JPA)
└── service/       # ProductService + cache-annotated implementation
```

## Prerequisites

- JDK 21
- PostgreSQL 14+ running locally on `5432`
- Redis 6+ running locally on `6379`
- Docker (optional, recommended once the Docker phase begins)

## Getting Started

1. **Create the database**

   ```sql
   CREATE DATABASE scalable_cache_db;
   ```

2. **Update credentials** in [`src/main/resources/application.yaml`](src/main/resources/application.yaml) if your local Postgres differs from the defaults.

3. **Run the app**

   ```bash
   ./mvnw spring-boot:run
   ```

The service listens on `http://localhost:8080`.

## API

Base path: `/api/v1/products`

| Method | Path           | Description           | Cache behavior         |
|--------|----------------|-----------------------|------------------------|
| POST   | `/`            | Create a product      | —                      |
| GET    | `/{id}`        | Fetch a product       | `@Cacheable` (read-through, TTL 10 min) |
| GET    | `/`            | List all products     | —                      |
| PUT    | `/{id}`        | Update a product      | `@CachePut`            |
| DELETE | `/{id}`        | Delete a product      | `@CacheEvict`          |

### Example

```bash
# Create
curl -X POST http://localhost:8080/api/v1/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Widget","description":"Test","price":9.99,"quantityAvailable":10}'

# First read — hits Postgres, writes to Redis
curl http://localhost:8080/api/v1/products/1

# Subsequent reads — served from Redis until TTL or eviction
curl http://localhost:8080/api/v1/products/1
```

You can confirm cache hits by tailing the app logs — a Postgres fetch logs `Fetching product from database. productId=...`; a cache hit is silent.

## Cache Notes

- Cache name: `products`, TTL: 10 minutes (configured in [`RedisCacheConfig`](src/main/java/com/jdevs/scalablecacheapi/config/RedisCacheConfig.java)).
- Default typing is enabled so polymorphic values (e.g. `Object` fields, collections) round-trip cleanly. A `@class` hint is stored alongside cached JSON.
- If you change the serializer or the cached DTO shape, flush Redis before retesting (`redis-cli FLUSHDB`) — old entries written with a different layout will not deserialize.

## Health & Metrics

Actuator exposes:

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/metrics`

## Roadmap

- [x] REST CRUD + JPA persistence
- [x] Redis caching with Spring Cache annotations
- [ ] Authentication (JWT) and role-based authorization
- [ ] Dockerfile + `docker-compose.yml` for the full stack
- [ ] Kubernetes manifests + local cluster walkthrough
- [ ] Kafka producer for domain events
- [ ] Kafka Streams consumer for projections / cache sync

## License

Educational / personal use.
