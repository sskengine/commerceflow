package com.sskengine.commerceflow.payment;

import com.sskengine.commerceflow.contracts.Money;
import java.time.Instant;
import java.util.UUID;

record RefundView(UUID refundId, UUID paymentId, Money amount, String status, Instant createdAt, String correlationId) {}
