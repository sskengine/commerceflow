# Implementation status

| Capability | Status in v0.1 | Next milestone |
|---|---|---|
| Module boundaries and shared contracts | Implemented | enforce with ArchUnit |
| Catalog browse + create | PostgreSQL/Flyway | search ranking |
| Order with mandatory currency | Durable store + Kafka `OrderCreated` | outbox pattern |
| Order cancel + stock release | Implemented | saga compensation |
| Inventory overselling protection | Pessimistic row lock | reservation expiry job |
| Payment idempotency | Durable unique constraint + provider call | durable outbox |
| Payment refund | Idempotent refund + Kafka | partial refunds |
| Delivery lifecycle | DB + Kafka consumer on `PaymentAuthorized` | carrier webhooks |
| Notification deduplication | Kafka consumers + durable event id | broker DLQ |
| Reporting and audit | Kafka projections to PostgreSQL | analytics warehouse |
| Authentication | Demo-only opaque token | OAuth2/JWT |
| Kafka topics (AsyncAPI v1) | Implemented | schema registry |
| Distributed tracing | Planned | v0.3-v0.6 |
| Discounts and advanced refunds | Planned | v0.5 |

ECA must treat an item marked `Planned` as roadmap scope, not as a newly introduced defect. It may still report that target specification coverage is incomplete.
