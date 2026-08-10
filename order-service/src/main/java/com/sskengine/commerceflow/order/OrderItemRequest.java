package com.sskengine.commerceflow.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

record OrderItemRequest(@NotNull UUID productId, @Positive int quantity, @NotNull BigDecimal unitPrice) {}
