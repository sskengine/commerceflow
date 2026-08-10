# CommerceFlow architecture baseline

## Runtime flow

`API Gateway -> Order Service -> Inventory Service`

After order confirmation, asynchronous flow continues:

`PaymentAuthorized (Kafka) -> Delivery Service -> ShipmentCreated`

Notification, reporting and audit consume versioned domain events. No service is allowed to modify another service's store (separate PostgreSQL schemas).

## Ownership

| Service | Owned aggregates | Produced/consumed contracts |
|---|---|---|
| Catalog | Product, Price | Product response |
| Inventory | Stock, Reservation | StockReserved |
| Order | Order, OrderItem | OrderCreated, OrderStatusChanged |
| Payment | Payment, Refund | PaymentAuthorized, PaymentFailed, RefundCompleted |
| Delivery | Shipment | consumes PaymentAuthorized, produces ShipmentCreated |
| Notification | Notification | consumes business events |
| Reporting | report projections | consumes all business events |
| Audit | AuditEvent | consumes privileged/domain action records |

## Persistence and messaging

- PostgreSQL schemas per bounded context (`catalog`, `inventory`, `orders`, `payment`, `delivery`, `notification`, `reporting`, `audit`).
- Flyway migrations are the schema source of truth (DATA-004).
- Kafka topics are named in `commerceflow-contracts` / AsyncAPI and carry `EventEnvelope`.

## Architectural rules for ECA

- Domain code may depend on `commerceflow-contracts` / `commerceflow-messaging`, never on another service module.
- Service-to-service calls use versioned HTTP contracts or versioned events.
- Money always carries a valid ISO 4217 currency.
- Critical POST operations use an idempotency key.
- Logs identify operations by correlation ID and must not contain payment tokens or personal data.
- Breaking contract changes require a new version or an explicit migration plan.
