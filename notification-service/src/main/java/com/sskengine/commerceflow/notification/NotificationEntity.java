package com.sskengine.commerceflow.notification;

import java.time.Instant;
import java.util.UUID;


class NotificationEntity {
    private UUID eventId;
    private String eventType;
    private String correlationId;
    private String status;
    private Instant processedAt;

    NotificationEntity() {}

    NotificationEntity(UUID eventId, String eventType, String correlationId, String status, Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.correlationId = correlationId;
        this.status = status;
        this.processedAt = processedAt;
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }

    NotificationResult toResult() {
        return new NotificationResult(eventId, status, processedAt);
    }

    NotificationView toView() {
        return new NotificationView(eventId, eventType, correlationId, status, processedAt);
    }
}
