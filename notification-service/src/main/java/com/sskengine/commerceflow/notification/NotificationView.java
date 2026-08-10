package com.sskengine.commerceflow.notification;

import java.time.Instant;
import java.util.UUID;

record NotificationView(UUID eventId, String eventType, String correlationId, String status, Instant processedAt) {}
