-- FR-021, DATA-002: durable orders with mandatory currency.
CREATE TABLE customer_order (
    order_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    currency CHAR(3) NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    correlation_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE order_item (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES customer_order(order_id),
    product_id UUID NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(19, 2) NOT NULL
);

CREATE INDEX idx_order_item_order ON order_item(order_id);
