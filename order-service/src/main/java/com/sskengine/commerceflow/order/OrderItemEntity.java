package com.sskengine.commerceflow.order;

import java.math.BigDecimal;
import java.util.UUID;


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
