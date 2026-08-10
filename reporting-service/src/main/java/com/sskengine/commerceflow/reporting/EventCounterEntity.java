package com.sskengine.commerceflow.reporting;


class EventCounterEntity {
    private String eventType;
    private long eventCount;

    EventCounterEntity() {}

    EventCounterEntity(String eventType, long eventCount) {
        this.eventType = eventType;
        this.eventCount = eventCount;
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public long getEventCount() { return eventCount; }
    public void setEventCount(long eventCount) { this.eventCount = eventCount; }

    void increment() {
        eventCount++;
    }
}
