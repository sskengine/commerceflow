package com.sskengine.commerceflow.inventory;

import java.time.Instant;
import java.util.UUID;

record ReservationView(UUID reservationId, UUID orderId, UUID productId, int quantity, Instant expiresAt, String status) {}
