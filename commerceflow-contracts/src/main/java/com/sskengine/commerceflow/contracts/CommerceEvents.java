package com.sskengine.commerceflow.contracts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CommerceEvents {
    private CommerceEvents() {}

    public record OrderLine(UUID productId, int quantity, Money unitPrice) {}
    public record OrderCreated(UUID orderId, UUID customerId, Money total, List<OrderLine> items) {}
    public record StockReserved(UUID reservationId, UUID orderId, Instant expiresAt) {}
    public record PaymentAuthorized(UUID paymentId, UUID orderId, Money amount) {}
    public record PaymentFailed(UUID orderId, String reasonCode) {}
    public record RefundCompleted(UUID refundId, UUID paymentId, Money amount) {}
    public record ShipmentCreated(UUID shipmentId, UUID orderId, String trackingNumber) {}
    public record OrderStatusChanged(UUID orderId, String oldStatus, String newStatus) {}
}

