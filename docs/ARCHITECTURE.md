# CommerceFlow architecture baseline

## Runtime flow

`API Gateway -> Order Service -> Inventory Service`

Payment, delivery, notification, reporting and audit are separate bounded contexts ready to join the flow through versioned events. No service is allowed to modify another service's store.

## Ownership

| Service | Owned aggregates | Produced/consumed contracts |
|---|---|---|
| Catalog | Product, Price | Product response |
| Inventory | Stock, Reservation | StockReserved |
| Order | Order, OrderItem | OrderCreated, OrderStatusChanged |
| Payment | Payment, Refund | PaymentAuthorized, PaymentFailed, RefundCompleted |
| Delivery | Shipment | ShipmentCreated |
| Notification | Notification | consumes business events |
| Reporting | report projections | consumes all business events |
| Audit | AuditEvent | consumes privileged action records |

## Architectural rules for ECA

- Domain code may depend on `commerceflow-contracts`, never on another service module.
- Service-to-service calls use versioned HTTP contracts or versioned events.
- Money always carries a valid ISO 4217 currency.
- Critical POST operations use an idempotency key.
- Logs identify operations by correlation ID and must not contain payment tokens or personal data.
- Breaking contract changes require a new version or an explicit migration plan.

