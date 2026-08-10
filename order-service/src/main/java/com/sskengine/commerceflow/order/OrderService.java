package com.sskengine.commerceflow.order;

import com.sskengine.commerceflow.contracts.CommerceEvents;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import com.sskengine.commerceflow.contracts.Money;
import com.sskengine.commerceflow.messaging.DomainEventPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/** FR-021, FR-022, DATA-002, BRULE-001, BRULE-003, AC-001. */
@Service
class OrderService {
    private final RestClient inventory;
    private final OrderRepository orders;
    private final DomainEventPublisher events;

    OrderService(RestClient inventoryClient, OrderRepository orders, DomainEventPublisher events) {
        this.inventory = inventoryClient;
        this.orders = orders;
        this.events = events;
    }

    @Transactional
    OrderView create(CreateOrderRequest request, String correlationId) {
        UUID orderId = UUID.randomUUID();
        Money total = request.items().stream()
                .map(item -> new Money(item.unitPrice(), request.currency()).multiply(item.quantity()))
                .reduce(new Money(BigDecimal.ZERO, request.currency()),
                        (a, b) -> new Money(a.amount().add(b.amount()), a.currency()));

        for (OrderItemRequest item : request.items()) {
            inventory.post().uri("/api/v1/reservations")
                    .header("X-Correlation-Id", correlationId)
                    .body(new ReserveStockRequest(orderId, item.productId(), item.quantity()))
                    .retrieve().toBodilessEntity();
        }

        OrderEntity order = new OrderEntity(orderId, request.customerId(), request.currency(), total,
                "CONFIRMED", correlationId, Instant.now());
        orders.insert(order);
        for (OrderItemRequest item : request.items()) {
            OrderItemEntity line = new OrderItemEntity(UUID.randomUUID(), orderId, item.productId(),
                    item.quantity(), item.unitPrice());
            orders.insertItem(line);
            order.addItem(line);
        }

        List<CommerceEvents.OrderLine> lines = request.items().stream()
                .map(i -> new CommerceEvents.OrderLine(i.productId(), i.quantity(),
                        new Money(i.unitPrice(), request.currency())))
                .toList();
        events.publish(CommerceTopics.ORDER_CREATED, EventEnvelope.v1(
                "OrderCreated", correlationId,
                new CommerceEvents.OrderCreated(orderId, request.customerId(), total, lines)));
        return order.toView();
    }

    OrderView get(UUID id) {
        OrderEntity order = loadOrder(id);
        return order.toView();
    }

    @Transactional
    OrderView cancel(UUID id, String correlationId) {
        OrderEntity order = loadOrder(id);
        if ("CANCELLED".equals(order.getStatus())) {
            return order.toView();
        }
        String oldStatus = order.getStatus();
        inventory.post().uri("/api/v1/reservations/release/{orderId}", id)
                .header("X-Correlation-Id", correlationId)
                .retrieve().toBodilessEntity();
        order.changeStatus("CANCELLED");
        orders.updateStatus(order);
        events.publish(CommerceTopics.ORDER_STATUS_CHANGED, EventEnvelope.v1(
                "OrderStatusChanged", correlationId,
                new CommerceEvents.OrderStatusChanged(id, oldStatus, "CANCELLED")));
        return order.toView();
    }

    private OrderEntity loadOrder(UUID id) {
        OrderEntity order = orders.findById(id);
        if (order == null) {
            throw new OrderNotFoundException();
        }
        order.setItems(orders.findItemsByOrderId(id));
        return order;
    }
}
