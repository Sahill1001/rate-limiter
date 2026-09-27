# Redis Rate Limiter

A personal Java project demonstrating token-bucket request throttling with Spring Boot and Redis Lua. This is a learning implementation; no production traffic, throughput benchmark or latency improvement is claimed.

**[Read the engineering case study](docs/CASE_STUDY.md)** — request flow, implementation choices, 9 passing tests, and current limitations.

## Stack

Java 17+, Spring Boot 4.0.3, Spring Data Redis, Redis, Maven, JUnit and Mockito.

## Behavior

- GET /api/request identifies the client by IP and checks its Redis bucket.
- The Lua script refills and consumes tokens in a single Redis operation.
- Available token: HTTP 200. Exhausted bucket: HTTP 429 with Retry-After: 60.
- Default settings: capacity 10, refill 10 tokens every 60 seconds.
- Settings live in src/main/resources/application.yaml.

## Run locally

Start Redis on localhost:6379, then run:

```sh
mvn spring-boot:run
curl -i http://localhost:8080/api/request
```

## Tests

```sh
mvn test
# With a dedicated Redis instance on localhost:6379:
RUN_REDIS_TESTS=true mvn test
```

PowerShell: set $env:RUN_REDIS_TESTS='true' before running mvn test.

The four controller tests cover accepted/rejected requests and client identification. Five opt-in tests execute the real Lua script against Redis: capacity, refill boundary, capacity cap, client isolation and concurrent token consumption. GitHub Actions supplies Redis and runs both groups. Real-Redis tests are skipped when RUN_REDIS_TESTS is absent. Test cleanup deletes only randomly named test keys.

## Current limitations

- X-Forwarded-For is trusted directly. An internet-facing deployment needs a trusted proxy that replaces this header.
- Retry-After is fixed at 60 seconds, rather than calculated from the bucket.
- Redis failures propagate; no fallback policy is implemented.
- Application clocks supply timestamps. Multi-instance clock skew is not addressed.
- Bucket TTL is twice the refill duration; configurations with a small refill relative to capacity need a longer retention policy.

These are follow-up design tasks, not implemented production guarantees.
