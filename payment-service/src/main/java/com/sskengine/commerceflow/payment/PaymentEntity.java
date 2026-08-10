package com.sskengine.commerceflow.payment;

import com.sskengine.commerceflow.contracts.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


class PaymentEntity {
    private UUID paymentId;
    private UUID orderId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String idempotencyKey;
    private String providerReference;
    private String correlationId;
    private Instant createdAt;

    PaymentEntity() {}

    PaymentEntity(UUID paymentId, UUID orderId, Money money, String status, String idempotencyKey,
                  String providerReference, String correlationId, Instant createdAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = money.amount();
        this.currency = money.currency();
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        this.providerReference = providerReference;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
    }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    Money money() { return new Money(amount, currency); }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String providerReference) { this.providerReference = providerReference; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    PaymentView toView() {
        return new PaymentView(paymentId, orderId, money(), status, createdAt, correlationId);
    }
}
