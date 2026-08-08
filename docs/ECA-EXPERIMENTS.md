# Controlled ECA experiments

Apply each experiment in a separate branch and PR. Never merge a deliberately broken experiment into `main`.

| ID | Branch example | Controlled change | Expected ECA result |
|---|---|---|---|
| EXP-001 | `SCRUM-15-remove-currency` | Remove `currency` from `CreateOrderRequest` but not from FR-021/OpenAPI | Requirement and contract violation; affected payment/reporting consumers |
| EXP-002 | `SCRUM-16-break-event-v1` | Rename a field in `OrderCreated` without increasing event version | Breaking event contract and stale consumers |
| EXP-003 | `SCRUM-17-payment-duplicate` | Replace `putIfAbsent` with unconditional insert | AC-002/idempotency regression |
| EXP-004 | `SCRUM-18-overselling` | Remove atomic stock update | BR-002/AC-003 concurrency risk |
| EXP-005 | `SCRUM-19-sensitive-log` | Log a payment token | SEC-004/AC-004 violation |
| EXP-006 | `SCRUM-20-schema-without-migration` | Add a persisted field without Flyway migration after v0.2 | DATA-004 violation |
| EXP-007 | `no-ticket-ref` | Commit without Jira key | Traceability violation |

The PR description should contain: Jira key, requirement IDs, intended scope, changed contracts, migration impact, tests and rollback plan.

