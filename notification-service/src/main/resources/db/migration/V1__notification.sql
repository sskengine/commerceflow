-- FR-060, INT-006: durable notification deduplication by event id.
CREATE TABLE notification_message (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
