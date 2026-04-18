CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_amount NUMERIC(18, 2) NOT NULL,
    order_date TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS order_line (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    line_no INTEGER NOT NULL,
    product_code VARCHAR(32) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(18, 2) NOT NULL,
    line_amount NUMERIC(18, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_customer_id ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_order_date ON orders(order_date);
CREATE INDEX IF NOT EXISTS idx_order_line_order_id ON order_line(order_id);
CREATE INDEX IF NOT EXISTS idx_order_line_product_code ON order_line(product_code);

INSERT INTO orders (
    order_no,
    customer_id,
    status,
    total_amount,
    order_date,
    created_at,
    updated_at
)
SELECT
    'ORD-' || LPAD(gs::text, 10, '0'),
    ((gs - 1) % 100000) + 1,
    CASE gs % 4
        WHEN 0 THEN 'NEW'
        WHEN 1 THEN 'CONFIRMED'
        WHEN 2 THEN 'SHIPPED'
        ELSE 'COMPLETED'
    END,
    (30 + ((gs % 50) * 7.5))::NUMERIC(18, 2),
    TIMESTAMPTZ '2025-01-01 00:00:00+00' + ((gs % 365) * INTERVAL '1 day'),
    NOW(),
    NOW()
FROM generate_series(1, 1000000) AS gs
ON CONFLICT (order_no) DO NOTHING;

INSERT INTO order_line (
    order_id,
    line_no,
    product_code,
    quantity,
    unit_price,
    line_amount,
    created_at
)
SELECT
    o.id,
    line_no,
    'SKU-' || LPAD((((o.id - 1) * 3 + line_no - 1) % 50000 + 1)::text, 8, '0'),
    ((o.id + line_no) % 5) + 1,
    (10 + (((o.id + line_no) % 30) * 2.5))::NUMERIC(18, 2),
    ((((o.id + line_no) % 5) + 1) * (10 + (((o.id + line_no) % 30) * 2.5)))::NUMERIC(18, 2),
    NOW()
FROM orders o
CROSS JOIN generate_series(1, 3) AS line_no
WHERE NOT EXISTS (
    SELECT 1
    FROM order_line ol
    WHERE ol.order_id = o.id
      AND ol.line_no = line_no
);
