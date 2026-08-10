package com.sskengine.commerceflow.delivery;

import java.time.Instant;
import java.util.UUID;

record ShipmentView(UUID shipmentId, UUID orderId, String addressRef, String trackingNumber, String status, Instant createdAt) {}
