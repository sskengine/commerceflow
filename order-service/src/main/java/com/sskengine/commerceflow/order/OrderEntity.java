package com.sskengine.commerceflow.order;

import com.sskengine.commerceflow.contracts.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


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
