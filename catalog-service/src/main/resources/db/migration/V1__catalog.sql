-- FR-010, DATA-004: durable catalog products.
CREATE TABLE product (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    published BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO product (id, name, amount, currency, published) VALUES
    ('22222222-2222-2222-2222-222222222222', 'Demo keyboard', 25.00, 'USD', TRUE),
    ('33333333-3333-3333-3333-333333333333', 'Demo mouse', 15.00, 'USD', TRUE);
