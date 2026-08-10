package com.sskengine.commerceflow.inventory;

import java.time.Instant;
import java.util.UUID;


class ReservationEntity {
    private UUID reservationId;
    private UUID orderId;
    private UUID productId;
    private int quantity;
    private Instant expiresAt;
    private String status;

    ReservationEntity() {}

    ReservationEntity(UUID reservationId, UUID orderId, UUID productId, int quantity, Instant expiresAt, String status) {
        this.reservationId = reservationId;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public UUID getReservationId() { return reservationId; }
    public void setReservationId(UUID reservationId) { this.reservationId = reservationId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    void release() {
        this.status = "RELEASED";
    }

    ReservationView toView() {
        return new ReservationView(reservationId, orderId, productId, quantity, expiresAt, status);
    }
}
