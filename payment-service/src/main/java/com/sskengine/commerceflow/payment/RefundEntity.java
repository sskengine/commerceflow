package com.sskengine.commerceflow.payment;

import com.sskengine.commerceflow.contracts.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


class RefundEntity {
    private UUID refundId;
    private UUID paymentId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String idempotencyKey;
    private String correlationId;
    private Instant createdAt;

    RefundEntity() {}

    RefundEntity(UUID refundId, UUID paymentId, Money money, String status, String idempotencyKey,
                 String correlationId, Instant createdAt) {
        this.refundId = refundId;
        this.paymentId = paymentId;
        this.amount = money.amount();
        this.currency = money.currency();
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    public UUID getRefundId() { return refundId; }
    public void setRefundId(UUID refundId) { this.refundId = refundId; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    RefundView toView() {
        return new RefundView(refundId, paymentId, new Money(amount, currency), status, createdAt, correlationId);
    }
}
