package com.sskengine.commerceflow.order;

import com.sskengine.commerceflow.contracts.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record OrderView(UUID orderId, UUID customerId, String currency, Money total, List<OrderItemRequest> items,
                 String status, Instant createdAt, String correlationId) {}
