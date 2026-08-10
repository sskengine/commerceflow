package com.sskengine.commerceflow.notification;

import java.time.Instant;
import java.util.UUID;


record NotificationResult(UUID eventId, String status, Instant processedAt) {}
