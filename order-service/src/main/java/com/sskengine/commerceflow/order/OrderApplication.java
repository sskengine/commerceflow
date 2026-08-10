package com.sskengine.commerceflow.order;

import com.sskengine.commerceflow.contracts.CommerceEvents;
import com.sskengine.commerceflow.contracts.CommerceTopics;
import com.sskengine.commerceflow.contracts.EventEnvelope;
import com.sskengine.commerceflow.contracts.Money;
import com.sskengine.commerceflow.messaging.DomainEventPublisher;
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
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
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

class OrderEntity {
    private UUID orderId;
    private UUID customerId;
    private String currency;
    private BigDecimal totalAmount;
    private String status;
    private String correlationId;
    private Instant createdAt;
    private List<OrderItemEntity> items = new ArrayList<>();

    OrderEntity() {}

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
    }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    Money total() { return new Money(totalAmount, currency); }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<OrderItemEntity> getItems() { return items; }
    public void setItems(List<OrderItemEntity> items) { this.items = items; }

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

class OrderItemEntity {
    private UUID id;
    private UUID orderId;
    private UUID productId;
    private int quantity;
    private BigDecimal unitPrice;

    OrderItemEntity() {}

    OrderItemEntity(UUID id, UUID orderId, UUID productId, int quantity, BigDecimal unitPrice) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}

@Mapper
interface OrderRepository {
    @Insert("""
            INSERT INTO customer_order (order_id, customer_id, currency, total_amount, status, correlation_id, created_at)
            VALUES (#{orderId}, #{customerId}, #{currency}, #{totalAmount}, #{status}, #{correlationId}, #{createdAt})
            """)
    void insert(OrderEntity order);

    @Insert("""
            INSERT INTO order_item (id, order_id, product_id, quantity, unit_price)
            VALUES (#{id}, #{orderId}, #{productId}, #{quantity}, #{unitPrice})
            """)
    void insertItem(OrderItemEntity item);

    @Select("""
            SELECT order_id, customer_id, currency, total_amount, status, correlation_id, created_at
            FROM customer_order
            WHERE order_id = #{id}
            """)
    OrderEntity findById(@Param("id") UUID id);

    @Select("""
            SELECT id, order_id, product_id, quantity, unit_price
            FROM order_item
            WHERE order_id = #{orderId}
            """)
    List<OrderItemEntity> findItemsByOrderId(@Param("orderId") UUID orderId);

    @Update("UPDATE customer_order SET status = #{status} WHERE order_id = #{orderId}")
    void updateStatus(OrderEntity order);
}

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
