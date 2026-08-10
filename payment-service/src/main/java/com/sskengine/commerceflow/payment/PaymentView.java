package com.sskengine.commerceflow.payment;

import com.sskengine.commerceflow.contracts.Money;
import java.time.Instant;
import java.util.UUID;

record PaymentView(UUID paymentId, UUID orderId, Money amount, String status, Instant createdAt, String correlationId) {}
