package com.sskengine.commerceflow.audit;

import java.time.Instant;
import java.util.UUID;

record AuditEntry(UUID eventId, String actorId, String action, String resourceType,
                  String resourceId, String correlationId, Instant occurredAt) {}
