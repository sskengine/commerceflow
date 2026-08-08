package com.sskengine.commerceflow.contracts;

import java.time.Instant;
import java.util.UUID;

/** INT-004..006: versioned and deduplicatable event envelope. */
public record EventEnvelope<T>(UUID eventId, String eventType, int eventVersion,
                               Instant occurredAt, String correlationId, T payload) {
    public EventEnvelope {
        if (eventVersion < 1) throw new IllegalArgumentException("eventVersion must be positive");
    }

    public static <T> EventEnvelope<T> v1(String type, String correlationId, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), type, 1, Instant.now(), correlationId, payload);
    }
}

