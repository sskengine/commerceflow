package com.sskengine.commerceflow.inventory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;


record ReserveStockRequest(@NotNull UUID orderId, @NotNull UUID productId, @Positive int quantity) {}
