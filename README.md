# CommerceFlow Services v0.1

Java 21 / Spring Boot microservice testbed used as the controlled application landscape for ECA.

This repository implements an intentionally small but executable baseline of the target CommerceFlow architecture. It is not presented as the completed technical specification. Every implemented behavior is linked to requirement IDs; missing target capabilities remain visible in `docs/IMPLEMENTATION-STATUS.md` so ECA can distinguish planned scope from a regression.

## Services

| Module | Port | Responsibility |
|---|---:|---|
| api-gateway | 8080 | Public entry point/BFF |
| identity-service | 8081 | Demo identities and roles |
| catalog-service | 8082 | Products and prices |
| inventory-service | 8083 | Stock and reservations |
| order-service | 8084 | Order creation and status |
| payment-service | 8085 | Idempotent payment operations |
| delivery-service | 8086 | Shipments and delivery status |
| notification-service | 8087 | Deduplicated notifications |
| reporting-service | 8088 | Operational read model |
| audit-service | 8089 | Immutable audit entries |
| provider-stubs | 8090 | Payment/logistics success, failure and timeout |

`commerceflow-contracts` contains shared versioned DTOs and event envelopes. It intentionally contains no service implementation.

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

Recommended repository name: `commerceflow-services`. Do not commit real tokens, webhook secrets or provider credentials.

## Important baseline choices

- Stores are in memory in v0.1; PostgreSQL/Flyway ownership is introduced in v0.2.
- Synchronous REST is used for the first vertical slice; Kafka/AsyncAPI delivery is introduced in v0.3.
- Identity tokens are opaque demo tokens, not production JWTs.
- `X-Correlation-Id` and `Idempotency-Key` are explicit contract fields for ECA analysis.
- Provider stubs are deterministic and controlled by `X-Stub-Scenario`.

