package com.sskengine.commerceflow.audit;

import java.time.Instant;
import java.util.UUID;


class AuditEntryEntity {
    private UUID eventId;
    private String actorId;
    private String action;
    private String resourceType;
    private String resourceId;
    private String correlationId;
    private Instant occurredAt;

    AuditEntryEntity() {}

    AuditEntryEntity(UUID eventId, String actorId, String action, String resourceType, String resourceId,
                     String correlationId, Instant occurredAt) {
        this.eventId = eventId;
        this.actorId = actorId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    AuditEntry toView() {
        return new AuditEntry(eventId, actorId, action, resourceType, resourceId, correlationId, occurredAt);
    }
}
