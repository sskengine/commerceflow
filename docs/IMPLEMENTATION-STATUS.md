# Implementation status

| Capability | Status in v0.1 | Next milestone |
|---|---|---|
| Module boundaries and shared contracts | Implemented | enforce with ArchUnit |
| Catalog browse | Implemented in memory | PostgreSQL/Flyway |
| Order with mandatory currency | Implemented | durable order store |
| Inventory overselling protection | Implemented per process | transactional row locking |
| Payment idempotency | Implemented per process | durable unique constraint |
| Delivery lifecycle | Minimal implementation | provider integration |
| Notification deduplication | Minimal implementation | broker and DLQ |
| Reporting and audit | Minimal implementation | event projections |
| Authentication | Demo-only opaque token | OAuth2/JWT |
| Kafka, tracing, production observability | Planned | v0.3-v0.6 |
| Discounts and advanced refunds | Planned | v0.5 |

ECA must treat an item marked `Planned` as roadmap scope, not as a newly introduced defect. It may still report that target specification coverage is incomplete.

