-- FR-080: operational projections with event idempotency.
CREATE TABLE processed_event (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE event_counter (
    event_type VARCHAR(128) PRIMARY KEY,
    event_count BIGINT NOT NULL
);
