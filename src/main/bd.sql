-- =========================================
-- BIGDATA PRACTICE DATASET (5 tables)
-- PostgreSQL
-- Load order: DDL -> (optional) UNLOGGED -> INSERT (at the end) -> ANALYZE -> INDEX
-- =========================================

-- (0) Extensions + schema
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

CREATE SCHEMA IF NOT EXISTS bd;
SET search_path = bd, public;

-- (1) Drop (optional, nếu muốn làm lại từ đầu)
-- DROP TABLE IF EXISTS fact_events;
-- DROP TABLE IF EXISTS fact_orders;
-- DROP TABLE IF EXISTS dim_location;
-- DROP TABLE IF EXISTS dim_product;
-- DROP TABLE IF EXISTS dim_customer;

-- =========================================
-- (2) TABLES (DDL)
-- =========================================

-- 2.1 dim_customer
CREATE TABLE IF NOT EXISTS dim_customer (
  customer_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  customer_uuid UUID NOT NULL, -- khoá tự nhiên giả lập, giúp luyện query theo UUID.
  email         CITEXT NOT NULL, -- email không phân biệt hoa thường (tốt cho dedup)
  full_name     TEXT NOT NULL, -- tên đầy đủ
  gender        CHAR(1) NOT NULL, -- ‘M’, ‘F’, ‘U’.
  birth_date    DATE NOT NULL, -- ngày sinh
  signup_at     TIMESTAMPTZ NOT NULL, -- thời điểm đăng ký
  status        SMALLINT NOT NULL, --  0=inactive,1=active,2=banned (giả lập)
  segment       SMALLINT NOT NULL, -- phân khúc khách hàng (1..20)
  updated_at    TIMESTAMPTZ NOT NULL -- thời điểm cập nhật thông tin cuối cùng (giả lập)
);

-- 2.2 dim_product
CREATE TABLE IF NOT EXISTS dim_product (
  product_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  sku          TEXT NOT NULL,
  category_id  INT NOT NULL, -- nhóm lớn (1..5000) để group-by.
  brand_id     INT NOT NULL, -- thương hiệu (1..2000).
  title        TEXT NOT NULL, -- tên sản phẩm.
  price_cents  INT NOT NULL, -- giá cơ bản dạng cents (tránh float)
  cost_cents   INT NOT NULL, --  giá vốn.
  is_active    BOOLEAN NOT NULL, --  còn bán không.
  created_at   TIMESTAMPTZ NOT NULL, -- ngày tạo SP.
  updated_at   TIMESTAMPTZ NOT NULL -- cập nhật.
);

-- 2.3 dim_location
CREATE TABLE IF NOT EXISTS dim_location (
  location_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  country_code  CHAR(2) NOT NULL, -- mã quốc gia ISO (US, CA, GB...)
  region        TEXT NOT NULL, -- bang/vùng
  city          TEXT NOT NULL, -- thành phố
  zip           TEXT NOT NULL, -- mã bưu điện
  tz            TEXT NOT NULL, -- múi giờ (UTC, America/New_York...)
  lat           NUMERIC(9,6) NOT NULL, -- để luyện geo-ish query (không dùng PostGIS).
  lon           NUMERIC(9,6) NOT NULL, -- để luyện geo-ish query (không dùng PostGIS).
  created_at    TIMESTAMPTZ NOT NULL
);

-- 2.4 fact_orders (giả lập 1 order ~ 1 product)
CREATE TABLE IF NOT EXISTS fact_orders (
  order_id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  order_ts          TIMESTAMPTZ NOT NULL, -- thời điểm đặt hàng
  customer_id       BIGINT NOT NULL,
  product_id        BIGINT NOT NULL,
  location_id       BIGINT NOT NULL, -- địa điểm giao/nhận
  qty               INT NOT NULL, -- số lượng
  unit_price_cents  INT NOT NULL, -- giá bán tại thời điểm đặt hàng (có thể khác với dim_product.price_cents do khuyến mãi, thay đổi giá...)
  discount_cents    INT NOT NULL, -- tổng tiền giảm giá (có thể do coupon, khuyến mãi...)
  shipping_cents    INT NOT NULL, -- phí vận chuyển
  tax_cents         INT NOT NULL, -- thuế
  status            SMALLINT NOT NULL, -- 1=placed,2=shipped,3=delivered,4=canceled (giả lập)
  payment_method    SMALLINT NOT NULL, -- 1=credit_card,2=paypal,3=bank_transfer,4=cash_on_delivery (giả lập)
  channel           SMALLINT NOT NULL -- 1=web,2=mobile_app,3=phone,4=store (giả lập)
);

-- 2.5 fact_events (clickstream)
CREATE TABLE IF NOT EXISTS fact_events (
  event_id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  event_ts     TIMESTAMPTZ NOT NULL, -- thời điểm xảy ra sự kiện
  event_type   SMALLINT NOT NULL, -- 1=view_product,2=add_to_cart,3=remove_from_cart,4=checkout,5=payment,6=search (giả lập)
  customer_id  BIGINT NULL,
  session_id   UUID NOT NULL, -- phiên truy cập (giả lập), giúp luyện query theo session_id.
  product_id   BIGINT NULL, -- nullable (search/login không có product).
  location_id  BIGINT NOT NULL, -- địa điểm xảy ra sự kiện (có thể khác location của đơn hàng, ví dụ: xem sản phẩm ở nhà, đặt hàng ở cửa hàng)
  device_type  SMALLINT NOT NULL, -- 1=desktop,2=mobile,3=tablet (giả lập)
  referrer     TEXT NOT NULL, -- nguồn giới thiệu (google, facebook, direct, email, affiliate, tiktok...)
  properties   JSONB NOT NULL -- thuộc tính tuỳ biến theo event_type, ví dụ: { "ab": "A", "utm_campaign": "camp_123", "q": "keyword_abc", "value": 123 }
);

-- =========================================
-- (3) OPTIONAL: SPEED FOR LAB (UNLOGGED)
--     Chấp nhận rủi ro: crash/restart có thể mất data UNLOGGED
-- =========================================
-- BỎ comment nếu bạn muốn load nhanh hơn:
ALTER TABLE bd.dim_customer SET UNLOGGED;
ALTER TABLE bd.dim_product  SET UNLOGGED;
ALTER TABLE bd.dim_location SET UNLOGGED;
ALTER TABLE bd.fact_orders  SET UNLOGGED;
ALTER TABLE bd.fact_events  SET UNLOGGED;

-- =========================================
-- (4) OPTIONAL: constraints/indexes
--     Khuyến nghị: tạo index SAU khi load xong để nhanh hơn
-- =========================================
-- (sẽ tạo ở phần (7) sau khi INSERT + ANALYZE)

-- =========================================
-- (5) OPTIONAL: session tuning (chỉ cho lúc load)
-- =========================================
SET synchronous_commit = off;
SET maintenance_work_mem = '1GB';  -- tuỳ RAM
SET work_mem = '128MB';

-- =========================================
-- (6) INSERT DATA (ĐẶT CUỐI NHƯ BẠN YÊU CẦU)
--     Mặc định: 10,000,000 rows mỗi bảng
-- =========================================

-- 6.1 dim_customer: 10M
INSERT INTO bd.dim_customer (customer_uuid, email, full_name, gender, birth_date, signup_at, status, segment, updated_at)
SELECT
  gen_random_uuid(),
  ('user' || gs::text || '@example.com')::citext,
  'Customer ' || gs::text,
  (ARRAY['M','F','U'])[1 + (gs % 3)],
  DATE '1950-01-01' + ((gs * 13) % 25000),
  TIMESTAMPTZ '2018-01-01' + ((gs % 3000) || ' minutes')::interval,
  (ARRAY[0,1,1,1,2])[1 + (gs % 5)],
  1 + (gs % 20),
  TIMESTAMPTZ '2023-01-01' + ((gs % 200000) || ' seconds')::interval
FROM generate_series(1, 10000000) AS gs;

ANALYZE bd.dim_customer;

-- 6.2 dim_product: 10M
INSERT INTO bd.dim_product (sku, category_id, brand_id, title, price_cents, cost_cents, is_active, created_at, updated_at)
SELECT
  'SKU-' || lpad(gs::text, 10, '0'),
  1 + (gs % 5000),
  1 + (gs % 2000),
  'Product ' || gs::text,
  100 + (gs % 200000),
  50 + (gs % 150000),
  (gs % 20) <> 0,
  TIMESTAMPTZ '2017-01-01' + ((gs % 300000) || ' minutes')::interval,
  TIMESTAMPTZ '2024-01-01' + ((gs % 300000) || ' minutes')::interval
FROM generate_series(1, 10000000) AS gs;

ANALYZE bd.dim_product;

-- 6.3 dim_location: 10M
INSERT INTO bd.dim_location (country_code, region, city, zip, tz, lat, lon, created_at)
SELECT
  (ARRAY['US','CA','GB','DE','FR','JP','AU','SG','VN','IN'])[1 + (gs % 10)],
  'Region ' || (1 + (gs % 2000))::text,
  'City ' || (1 + (gs % 50000))::text,
  lpad((gs % 100000)::text, 5, '0'),
  (ARRAY['UTC','America/New_York','America/Los_Angeles','Europe/Berlin','Asia/Ho_Chi_Minh'])[1 + (gs % 5)],
  (-90 + (gs % 1800000) / 10000.0)::numeric(9,6),
  (-180 + (gs % 3600000) / 10000.0)::numeric(9,6),
  TIMESTAMPTZ '2016-01-01' + ((gs % 500000) || ' minutes')::interval
FROM generate_series(1, 10000000) AS gs;

ANALYZE bd.dim_location;

-- 6.4 fact_orders: 10M
INSERT INTO bd.fact_orders (
  order_ts, customer_id, product_id, location_id,
  qty, unit_price_cents, discount_cents, shipping_cents, tax_cents,
  status, payment_method, channel
)
SELECT
  TIMESTAMPTZ '2022-01-01' + ((gs % 20000000) || ' seconds')::interval,
  1 + (gs % 10000000),
  1 + ((gs * 7) % 10000000),
  1 + ((gs * 13) % 10000000),
  1 + (gs % 5),
  100 + (gs % 200000),
  (gs % 5000),
  100 + (gs % 2000),
  50 + (gs % 5000),
  (ARRAY[1,1,1,2,2,2,3,4])[1 + (gs % 8)],
  1 + (gs % 4),
  1 + (gs % 4)
FROM bd.generate_series(1, 10000000) AS gs;

ANALYZE bd.fact_orders;

-- 6.5 fact_events: 10M
INSERT INTO bd.fact_events (
  event_ts, event_type, customer_id, session_id, product_id, location_id,
  device_type, referrer, properties
)
SELECT
  TIMESTAMPTZ '2022-01-01' + ((gs % 30000000) || ' seconds')::interval,
  1 + (gs % 6),
  CASE WHEN (gs % 10) = 0 THEN NULL ELSE 1 + (gs % 10000000) END,
  gen_random_uuid(),
  CASE WHEN (gs % 6) IN (2,5,6) THEN NULL ELSE 1 + ((gs * 7) % 10000000) END,
  1 + ((gs * 13) % 10000000),
  1 + (gs % 3),
  (ARRAY['google','facebook','direct','email','affiliate','tiktok'])[1 + (gs % 6)],
  jsonb_build_object(
    'ab', (ARRAY['A','B','C'])[1 + (gs % 3)],
    'utm_campaign', 'camp_' || (gs % 1000),
    'q', CASE WHEN (gs % 6)=2 THEN 'keyword_' || (gs % 50000) ELSE NULL END,
    'value', (gs % 10000)
  )
FROM generate_series(1, 10000000) AS gs;

ANALYZE bd.fact_events;
