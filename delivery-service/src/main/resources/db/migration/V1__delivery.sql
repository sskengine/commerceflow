-- FR-050: durable shipment lifecycle.
CREATE TABLE shipment (
    shipment_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    address_ref VARCHAR(128) NOT NULL,
    tracking_number VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider_reference VARCHAR(128),
    correlation_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_shipment_order UNIQUE (order_id)
);
