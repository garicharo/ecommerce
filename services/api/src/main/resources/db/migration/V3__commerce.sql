CREATE TABLE carts (
    id UUID PRIMARY KEY DEFAULT GEN_RANDOM_UUID(),
    user_id UUID NOT NULL UNIQUE REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cart_items (
    id UUID PRIMARY KEY DEFAULT GEN_RANDOM_UUID(),
    cart_id UUID NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    sku VARCHAR(255) NOT NULL,
    qty INT NOT NULL CHECK (qty > 0),
    UNIQUE (cart_id, sku)
);

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT GEN_RANDOM_UUID(),
    user_id UUID NOT NULL REFERENCES users (id),
    idempotency_key UUID NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'REJECTED', 'FAILED')),
    total NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, idempotency_key)
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT GEN_RANDOM_UUID(),
    order_id UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products (id) ON DELETE RESTRICT,
    sku VARCHAR(255) NOT NULL,
    name_snapshot VARCHAR(255) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    qty INT NOT NULL CHECK (qty > 0),
    line_total NUMERIC(12, 2) NOT NULL CHECK (line_total >= 0)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_product ON order_items (product_id);
