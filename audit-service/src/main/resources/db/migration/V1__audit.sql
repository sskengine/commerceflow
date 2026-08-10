-- FR-090, SEC-005: append-only audit store.
CREATE TABLE audit_entry (
    event_id UUID PRIMARY KEY,
    actor_id VARCHAR(128) NOT NULL,
    action VARCHAR(128) NOT NULL,
    resource_type VARCHAR(128) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);
