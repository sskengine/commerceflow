package com.sskengine.commerceflow.order;

import com.sskengine.commerceflow.contracts.CommerceEvents;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import com.sskengine.commerceflow.contracts.Money;
import com.sskengine.commerceflow.messaging.DomainEventPublisher;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@SpringBootApplication(scanBasePackages = {
        "com.sskengine.commerceflow.order",
        "com.sskengine.commerceflow.messaging"
})
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

    @Bean
    RestClient inventoryClient(@Value("${clients.inventory.base-url}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}

@Entity
@Table(name = "customer_order")
class OrderEntity {
    @Id
    private UUID orderId;
    private UUID customerId;
    @Column(length = 3, nullable = false)
    private String currency;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false, length = 128)
    private String correlationId;
    @Column(nullable = false)
    private Instant createdAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItemEntity> items = new ArrayList<>();

    protected OrderEntity() {}

    OrderEntity(UUID orderId, UUID customerId, String currency, Money total, String status,
                String correlationId, Instant createdAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.currency = currency;
        this.totalAmount = total.amount();
        this.status = status;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    void addItem(OrderItemEntity item) {
        items.add(item);
        item.setOrder(this);
    }

    UUID getOrderId() { return orderId; }
    UUID getCustomerId() { return customerId; }
    String getCurrency() { return currency; }
    Money total() { return new Money(totalAmount, currency); }
    String getStatus() { return status; }
    String getCorrelationId() { return correlationId; }
    Instant getCreatedAt() { return createdAt; }
    List<OrderItemEntity> getItems() { return items; }

    void changeStatus(String newStatus) {
        this.status = newStatus;
    }

    OrderView toView() {
        List<OrderItemRequest> lines = items.stream()
                .map(i -> new OrderItemRequest(i.getProductId(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new OrderView(orderId, customerId, currency, total(), lines, status, createdAt, correlationId);
    }
}

@Entity
@Table(name = "order_item")
class OrderItemEntity {
    @Id
    private UUID id;
    @jakarta.persistence.ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;
    private UUID productId;
    private int quantity;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    protected OrderItemEntity() {}

    OrderItemEntity(UUID id, UUID productId, int quantity, BigDecimal unitPrice) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    void setOrder(OrderEntity order) { this.order = order; }
    UUID getProductId() { return productId; }
    int getQuantity() { return quantity; }
    BigDecimal getUnitPrice() { return unitPrice; }
}

@Repository
interface OrderRepository extends JpaRepository<OrderEntity, UUID> {}

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
        for (OrderItemRequest item : request.items()) {
            order.addItem(new OrderItemEntity(UUID.randomUUID(), item.productId(), item.quantity(), item.unitPrice()));
        }
        orders.save(order);

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
        return orders.findById(id).map(OrderEntity::toView).orElseThrow(OrderNotFoundException::new);
    }

    @Transactional
    OrderView cancel(UUID id, String correlationId) {
        OrderEntity order = orders.findById(id).orElseThrow(OrderNotFoundException::new);
        if ("CANCELLED".equals(order.getStatus())) {
            return order.toView();
        }
        String oldStatus = order.getStatus();
        inventory.post().uri("/api/v1/reservations/release/{orderId}", id)
                .header("X-Correlation-Id", correlationId)
                .retrieve().toBodilessEntity();
        order.changeStatus("CANCELLED");
        events.publish(CommerceTopics.ORDER_STATUS_CHANGED, EventEnvelope.v1(
                "OrderStatusChanged", correlationId,
                new CommerceEvents.OrderStatusChanged(id, oldStatus, "CANCELLED")));
        return order.toView();
    }
}

@RestController
@RequestMapping("/api/v1/orders")
class OrderController {
    private final OrderService service;

    OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderView create(@Valid @RequestBody CreateOrderRequest request,
                     @RequestHeader(name = "X-Correlation-Id") @Size(min = 8, max = 128) String correlationId) {
        return service.create(request, correlationId);
    }

    @GetMapping("/{id}")
    OrderView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/{id}/cancel")
    OrderView cancel(@PathVariable UUID id,
                     @RequestHeader(name = "X-Correlation-Id") @Size(min = 8, max = 128) String correlationId) {
        return service.cancel(id, correlationId);
    }
}

record CreateOrderRequest(@NotNull UUID customerId, @NotNull @Size(min = 3, max = 3) String currency,
                          @NotEmpty List<@Valid OrderItemRequest> items) {}
record OrderItemRequest(@NotNull UUID productId, @Positive int quantity, @NotNull BigDecimal unitPrice) {}
record ReserveStockRequest(UUID orderId, UUID productId, int quantity) {}
record OrderView(UUID orderId, UUID customerId, String currency, Money total, List<OrderItemRequest> items,
                 String status, Instant createdAt, String correlationId) {}

@ResponseStatus(HttpStatus.NOT_FOUND)
class OrderNotFoundException extends RuntimeException {}
