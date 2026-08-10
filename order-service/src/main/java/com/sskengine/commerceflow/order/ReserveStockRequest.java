package com.sskengine.commerceflow.order;

import java.util.UUID;

record ReserveStockRequest(UUID orderId, UUID productId, int quantity) {}
