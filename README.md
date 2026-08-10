# CommerceFlow Services v0.1

Java 21 / Spring Boot microservice testbed used as the controlled application landscape for ECA.

This repository implements an intentionally small but executable baseline of the target CommerceFlow architecture. It is not presented as the completed technical specification. Every implemented behavior is linked to requirement IDs; missing target capabilities remain visible in `docs/IMPLEMENTATION-STATUS.md` so ECA can distinguish planned scope from a regression.

## Services

| Module | Port | Responsibility |
|---|---:|---|
| api-gateway | 8080 | Public entry point/BFF |
| identity-service | 8081 | Demo identities and roles |
| catalog-service | 8082 | Products and prices (PostgreSQL) |
| inventory-service | 8083 | Stock reservations + Kafka `StockReserved` |
| order-service | 8084 | Orders + Kafka `OrderCreated` |
| payment-service | 8085 | Idempotent payments + Kafka payment events |
| delivery-service | 8086 | Shipments (HTTP + Kafka consumer) |
| notification-service | 8087 | Deduplicated notifications from Kafka |
| reporting-service | 8088 | Operational projections from Kafka |
| audit-service | 8089 | Immutable audit entries |
| provider-stubs | 8090 | Payment/logistics success, failure and timeout |

Shared modules:

- `commerceflow-contracts` — versioned DTOs, event payloads, topic names
- `commerceflow-messaging` — Kafka `EventEnvelope` publisher

Infrastructure in `docker compose`: PostgreSQL (schemas per service) and Kafka (KRaft).

## Build

```powershell
mvn clean verify
```

Run all services:

```powershell
docker compose up --build
```

Smoke test:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

## First ECA workflow

1. Publish this repository to GitHub, keeping `main` as the clean baseline.
2. Configure the repository in `GITHUB_ALLOWED_REPOSITORIES` of ECA.
3. Create Jira issue `SCRUM-15` and mention requirement `FR-021` in its description.
4. Create branch `SCRUM-15-payment-validation`.
5. Make one controlled change from `docs/ECA-EXPERIMENTS.md` and open a PR.
6. Let the GitHub webhook start ECA analysis; compare its findings with the experiment oracle.
