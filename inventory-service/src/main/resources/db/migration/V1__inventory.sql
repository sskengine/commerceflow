-- BR-002, FR-030: stock with row-level locking target and reservation history.
CREATE TABLE stock_item (
    product_id UUID PRIMARY KEY,
    available_qty INT NOT NULL CHECK (available_qty >= 0)
);

CREATE TABLE reservation (
    reservation_id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL REFERENCES stock_item(product_id),
    quantity INT NOT NULL CHECK (quantity > 0),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE INDEX idx_reservation_order ON reservation(order_id);

INSERT INTO stock_item (product_id, available_qty) VALUES
    ('22222222-2222-2222-2222-222222222222', 10),
    ('33333333-3333-3333-3333-333333333333', 5);
