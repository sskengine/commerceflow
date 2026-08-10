-- FR-040, FR-041, AC-002: durable payment idempotency.
CREATE TABLE payment (
    payment_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    provider_reference VARCHAR(128),
    correlation_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_payment_idempotency UNIQUE (idempotency_key)
);

CREATE TABLE refund (
    refund_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payment(payment_id),
    amount NUMERIC(19, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_refund_idempotency UNIQUE (idempotency_key)
);
