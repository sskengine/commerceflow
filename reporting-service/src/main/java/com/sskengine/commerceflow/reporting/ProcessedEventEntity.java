package com.sskengine.commerceflow.reporting;

import java.time.Instant;
import java.util.UUID;


class ProcessedEventEntity {
    private UUID eventId;
    private String eventType;
    private Instant processedAt;

    ProcessedEventEntity() {}

    ProcessedEventEntity(UUID eventId, String eventType, Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
