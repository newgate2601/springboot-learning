-- =============================================
-- BƯỚC 1: TẠO TABLES - CHẠY ĐẦU TIÊN
-- =============================================

-- Xóa tables cũ nếu tồn tại
DROP TABLE IF EXISTS order_details CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS customers CASCADE;

-- Tạo bảng customers
CREATE TABLE customers (
    customer_id SERIAL PRIMARY KEY,
    customer_code VARCHAR(20) NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(20),
    address TEXT,
    city VARCHAR(50),
    country VARCHAR(50),
    registration_date DATE,
    customer_type VARCHAR(20),
    credit_rating INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tạo bảng products
CREATE TABLE products (
    product_id SERIAL PRIMARY KEY,
    product_code VARCHAR(20) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    category VARCHAR(50),
    subcategory VARCHAR(50),
    brand VARCHAR(50),
    unit_price DECIMAL(15,2),
    cost_price DECIMAL(15,2),
    stock_quantity INTEGER,
    supplier_id INTEGER,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tạo bảng orders
CREATE TABLE orders (
    order_id SERIAL PRIMARY KEY,
    order_number VARCHAR(30) NOT NULL,
    customer_id INTEGER,
    order_date DATE NOT NULL,
    required_date DATE,
    shipped_date DATE,
    order_status VARCHAR(20),
    payment_method VARCHAR(30),
    payment_status VARCHAR(20),
    total_amount DECIMAL(15,2),
    shipping_address TEXT,
    shipping_city VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tạo bảng order_details
CREATE TABLE order_details (
    order_detail_id SERIAL PRIMARY KEY,
    order_id INTEGER,
    product_id INTEGER,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    discount DECIMAL(5,2) DEFAULT 0,
    line_total DECIMAL(15,2) GENERATED ALWAYS AS (quantity * unit_price * (1 - discount/100)) STORED,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

SELECT 'TABLES CREATED SUCCESSFULLY';

-- =============================================
-- BƯỚC 2: INSERT CUSTOMERS - 10 TRIỆU RECORDS
-- Chạy lần lượt từng BLOCK 1 đến 10
-- =============================================

-- BLOCK 1: Customers 1-1,000,000
INSERT INTO customers (customer_code, customer_name, email, phone, address, city, country, registration_date, customer_type, credit_rating)
SELECT
    'CUST' || LPAD(seq::text, 8, '0'),
    'Customer_' || seq,
    'user' || seq || '@company.com',
    '+84-9' || LPAD((seq % 1000000)::text, 6, '0'),
    'Address_' || (seq % 50000) || '_Street',
    (CASE (seq % 25)
        WHEN 0 THEN 'Hanoi' WHEN 1 THEN 'HCMC' WHEN 2 THEN 'Danang' WHEN 3 THEN 'Hue'
        WHEN 4 THEN 'CanTho' WHEN 5 THEN 'NhaTrang' WHEN 6 THEN 'VungTau' WHEN 7 THEN 'Haiphong'
        WHEN 8 THEN 'Bangkok' WHEN 9 THEN 'Singapore' WHEN 10 THEN 'Tokyo' WHEN 11 THEN 'Seoul'
        WHEN 12 THEN 'Beijing' WHEN 13 THEN 'Shanghai' WHEN 14 THEN 'Sydney' WHEN 15 THEN 'Melbourne'
        WHEN 16 THEN 'London' WHEN 17 THEN 'NewYork' WHEN 18 THEN 'Paris' WHEN 19 THEN 'Berlin'
        WHEN 20 THEN 'Dubai' WHEN 21 THEN 'Mumbai' WHEN 22 THEN 'Toronto' WHEN 23 THEN 'LosAngeles'
        ELSE 'OtherCity'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Vietnam' WHEN 1 THEN 'USA' WHEN 2 THEN 'UK' WHEN 3 THEN 'Japan'
        WHEN 4 THEN 'Korea' WHEN 5 THEN 'China' WHEN 6 THEN 'Australia' WHEN 7 THEN 'Germany'
        WHEN 8 THEN 'France' WHEN 9 THEN 'Canada' WHEN 10 THEN 'India' WHEN 11 THEN 'UAE'
        WHEN 12 THEN 'Thailand' WHEN 13 THEN 'Singapore' ELSE 'Malaysia'
     END),
    DATE '2015-01-01' + (seq % 3650),
    (CASE (seq % 3)
        WHEN 0 THEN 'individual'
        WHEN 1 THEN 'business'
        ELSE 'premium'
     END),
    (seq % 1001)
FROM generate_series(1, 1000000) seq;

SELECT 'CUSTOMERS BATCH 1 COMPLETED: ' || COUNT(*) FROM customers;

-- BLOCK 2: Customers 1,000,001-2,000,000
INSERT INTO customers (customer_code, customer_name, email, phone, address, city, country, registration_date, customer_type, credit_rating)
SELECT
    'CUST' || LPAD(seq::text, 8, '0'),
    'Customer_' || seq,
    'user' || seq || '@company.com',
    '+84-9' || LPAD((seq % 1000000)::text, 6, '0'),
    'Address_' || (seq % 50000) || '_Street',
    (CASE (seq % 25)
        WHEN 0 THEN 'Hanoi' WHEN 1 THEN 'HCMC' WHEN 2 THEN 'Danang' WHEN 3 THEN 'Hue'
        WHEN 4 THEN 'CanTho' WHEN 5 THEN 'NhaTrang' WHEN 6 THEN 'VungTau' WHEN 7 THEN 'Haiphong'
        WHEN 8 THEN 'Bangkok' WHEN 9 THEN 'Singapore' WHEN 10 THEN 'Tokyo' WHEN 11 THEN 'Seoul'
        WHEN 12 THEN 'Beijing' WHEN 13 THEN 'Shanghai' WHEN 14 THEN 'Sydney' WHEN 15 THEN 'Melbourne'
        WHEN 16 THEN 'London' WHEN 17 THEN 'NewYork' WHEN 18 THEN 'Paris' WHEN 19 THEN 'Berlin'
        WHEN 20 THEN 'Dubai' WHEN 21 THEN 'Mumbai' WHEN 22 THEN 'Toronto' WHEN 23 THEN 'LosAngeles'
        ELSE 'OtherCity'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Vietnam' WHEN 1 THEN 'USA' WHEN 2 THEN 'UK' WHEN 3 THEN 'Japan'
        WHEN 4 THEN 'Korea' WHEN 5 THEN 'China' WHEN 6 THEN 'Australia' WHEN 7 THEN 'Germany'
        WHEN 8 THEN 'France' WHEN 9 THEN 'Canada' WHEN 10 THEN 'India' WHEN 11 THEN 'UAE'
        WHEN 12 THEN 'Thailand' WHEN 13 THEN 'Singapore' ELSE 'Malaysia'
     END),
    DATE '2015-01-01' + (seq % 3650),
    (CASE (seq % 3)
        WHEN 0 THEN 'individual'
        WHEN 1 THEN 'business'
        ELSE 'premium'
     END),
    (seq % 1001)
FROM generate_series(1000001, 2000000) seq;

SELECT 'CUSTOMERS BATCH 2 COMPLETED: ' || COUNT(*) FROM customers;

-- BLOCK 3: Customers 2,000,001-3,000,000
INSERT INTO customers (customer_code, customer_name, email, phone, address, city, country, registration_date, customer_type, credit_rating)
SELECT
    'CUST' || LPAD(seq::text, 8, '0'),
    'Customer_' || seq,
    'user' || seq || '@company.com',
    '+84-9' || LPAD((seq % 1000000)::text, 6, '0'),
    'Address_' || (seq % 50000) || '_Street',
    (CASE (seq % 25)
        WHEN 0 THEN 'Hanoi' WHEN 1 THEN 'HCMC' WHEN 2 THEN 'Danang' WHEN 3 THEN 'Hue'
        WHEN 4 THEN 'CanTho' WHEN 5 THEN 'NhaTrang' WHEN 6 THEN 'VungTau' WHEN 7 THEN 'Haiphong'
        WHEN 8 THEN 'Bangkok' WHEN 9 THEN 'Singapore' WHEN 10 THEN 'Tokyo' WHEN 11 THEN 'Seoul'
        WHEN 12 THEN 'Beijing' WHEN 13 THEN 'Shanghai' WHEN 14 THEN 'Sydney' WHEN 15 THEN 'Melbourne'
        WHEN 16 THEN 'London' WHEN 17 THEN 'NewYork' WHEN 18 THEN 'Paris' WHEN 19 THEN 'Berlin'
        WHEN 20 THEN 'Dubai' WHEN 21 THEN 'Mumbai' WHEN 22 THEN 'Toronto' WHEN 23 THEN 'LosAngeles'
        ELSE 'OtherCity'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Vietnam' WHEN 1 THEN 'USA' WHEN 2 THEN 'UK' WHEN 3 THEN 'Japan'
        WHEN 4 THEN 'Korea' WHEN 5 THEN 'China' WHEN 6 THEN 'Australia' WHEN 7 THEN 'Germany'
        WHEN 8 THEN 'France' WHEN 9 THEN 'Canada' WHEN 10 THEN 'India' WHEN 11 THEN 'UAE'
        WHEN 12 THEN 'Thailand' WHEN 13 THEN 'Singapore' ELSE 'Malaysia'
     END),
    DATE '2015-01-01' + (seq % 3650),
    (CASE (seq % 3)
        WHEN 0 THEN 'individual'
        WHEN 1 THEN 'business'
        ELSE 'premium'
     END),
    (seq % 1001)
FROM generate_series(2000001, 3000000) seq;

SELECT 'CUSTOMERS BATCH 3 COMPLETED: ' || COUNT(*) FROM customers;

-- BLOCK 4: Customers 3,000,001-4,000,000
INSERT INTO customers (customer_code, customer_name, email, phone, address, city, country, registration_date, customer_type, credit_rating)
SELECT
    'CUST' || LPAD(seq::text, 8, '0'),
    'Customer_' || seq,
    'user' || seq || '@company.com',
    '+84-9' || LPAD((seq % 1000000)::text, 6, '0'),
    'Address_' || (seq % 50000) || '_Street',
    (CASE (seq % 25)
        WHEN 0 THEN 'Hanoi' WHEN 1 THEN 'HCMC' WHEN 2 THEN 'Danang' WHEN 3 THEN 'Hue'
        WHEN 4 THEN 'CanTho' WHEN 5 THEN 'NhaTrang' WHEN 6 THEN 'VungTau' WHEN 7 THEN 'Haiphong'
        WHEN 8 THEN 'Bangkok' WHEN 9 THEN 'Singapore' WHEN 10 THEN 'Tokyo' WHEN 11 THEN 'Seoul'
        WHEN 12 THEN 'Beijing' WHEN 13 THEN 'Shanghai' WHEN 14 THEN 'Sydney' WHEN 15 THEN 'Melbourne'
        WHEN 16 THEN 'London' WHEN 17 THEN 'NewYork' WHEN 18 THEN 'Paris' WHEN 19 THEN 'Berlin'
        WHEN 20 THEN 'Dubai' WHEN 21 THEN 'Mumbai' WHEN 22 THEN 'Toronto' WHEN 23 THEN 'LosAngeles'
        ELSE 'OtherCity'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Vietnam' WHEN 1 THEN 'USA' WHEN 2 THEN 'UK' WHEN 3 THEN 'Japan'
        WHEN 4 THEN 'Korea' WHEN 5 THEN 'China' WHEN 6 THEN 'Australia' WHEN 7 THEN 'Germany'
        WHEN 8 THEN 'France' WHEN 9 THEN 'Canada' WHEN 10 THEN 'India' WHEN 11 THEN 'UAE'
        WHEN 12 THEN 'Thailand' WHEN 13 THEN 'Singapore' ELSE 'Malaysia'
     END),
    DATE '2015-01-01' + (seq % 3650),
    (CASE (seq % 3)
        WHEN 0 THEN 'individual'
        WHEN 1 THEN 'business'
        ELSE 'premium'
     END),
    (seq % 1001)
FROM generate_series(3000001, 4000000) seq;

SELECT 'CUSTOMERS BATCH 4 COMPLETED: ' || COUNT(*) FROM customers;

-- BLOCK 5: Customers 4,000,001-5,000,000
INSERT INTO customers (customer_code, customer_name, email, phone, address, city, country, registration_date, customer_type, credit_rating)
SELECT
    'CUST' || LPAD(seq::text, 8, '0'),
    'Customer_' || seq,
    'user' || seq || '@company.com',
    '+84-9' || LPAD((seq % 1000000)::text, 6, '0'),
    'Address_' || (seq % 50000) || '_Street',
    (CASE (seq % 25)
        WHEN 0 THEN 'Hanoi' WHEN 1 THEN 'HCMC' WHEN 2 THEN 'Danang' WHEN 3 THEN 'Hue'
        WHEN 4 THEN 'CanTho' WHEN 5 THEN 'NhaTrang' WHEN 6 THEN 'VungTau' WHEN 7 THEN 'Haiphong'
        WHEN 8 THEN 'Bangkok' WHEN 9 THEN 'Singapore' WHEN 10 THEN 'Tokyo' WHEN 11 THEN 'Seoul'
        WHEN 12 THEN 'Beijing' WHEN 13 THEN 'Shanghai' WHEN 14 THEN 'Sydney' WHEN 15 THEN 'Melbourne'
        WHEN 16 THEN 'London' WHEN 17 THEN 'NewYork' WHEN 18 THEN 'Paris' WHEN 19 THEN 'Berlin'
        WHEN 20 THEN 'Dubai' WHEN 21 THEN 'Mumbai' WHEN 22 THEN 'Toronto' WHEN 23 THEN 'LosAngeles'
        ELSE 'OtherCity'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Vietnam' WHEN 1 THEN 'USA' WHEN 2 THEN 'UK' WHEN 3 THEN 'Japan'
        WHEN 4 THEN 'Korea' WHEN 5 THEN 'China' WHEN 6 THEN 'Australia' WHEN 7 THEN 'Germany'
        WHEN 8 THEN 'France' WHEN 9 THEN 'Canada' WHEN 10 THEN 'India' WHEN 11 THEN 'UAE'
        WHEN 12 THEN 'Thailand' WHEN 13 THEN 'Singapore' ELSE 'Malaysia'
     END),
    DATE '2015-01-01' + (seq % 3650),
    (CASE (seq % 3)
        WHEN 0 THEN 'individual'
        WHEN 1 THEN 'business'
        ELSE 'premium'
     END),
    (seq % 1001)
FROM generate_series(4000001, 5000000) seq;

SELECT 'CUSTOMERS BATCH 5 COMPLETED: ' || COUNT(*) FROM customers;

-- BLOCK 6: Customers 5,000,001-6,000,000
INSERT INTO customers (customer_code, customer_name, email, phone, address, city, country, registration_date, customer_type, credit_rating)
SELECT
    'CUST' || LPAD(seq::text, 8, '0'),
    'Customer_' || seq,
    'user' || seq || '@company.com',
    '+84-9' || LPAD((seq % 1000000)::text, 6, '0'),
    'Address_' || (seq % 50000) || '_Street',
    (CASE (seq % 25)
        WHEN 0 THEN 'Hanoi' WHEN 1 THEN 'HCMC' WHEN 2 THEN 'Danang' WHEN 3 THEN 'Hue'
        WHEN 4 THEN 'CanTho' WHEN 5 THEN 'NhaTrang' WHEN 6 THEN 'VungTau' WHEN 7 THEN 'Haiphong'
        WHEN 8 THEN 'Bangkok' WHEN 9 THEN 'Singapore' WHEN 10 THEN 'Tokyo' WHEN 11 THEN 'Seoul'
        WHEN 12 THEN 'Beijing' WHEN 13 THEN 'Shanghai' WHEN 14 THEN 'Sydney' WHEN 15 THEN 'Melbourne'
        WHEN 16 THEN 'London' WHEN 17 THEN 'NewYork' WHEN 18 THEN 'Paris' WHEN 19 THEN 'Berlin'
        WHEN 20 THEN 'Dubai' WHEN 21 THEN 'Mumbai' WHEN 22 THEN 'Toronto' WHEN 23 THEN 'LosAngeles'
        ELSE 'OtherCity'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Vietnam' WHEN 1 THEN 'USA' WHEN 2 THEN 'UK' WHEN 3 THEN 'Japan'
        WHEN 4 THEN 'Korea' WHEN 5 THEN 'China' WHEN 6 THEN 'Australia' WHEN 7 THEN 'Germany'
        WHEN 8 THEN 'France' WHEN 9 THEN 'Canada' WHEN 10 THEN 'India' WHEN 11 THEN 'UAE'
        WHEN 12 THEN 'Thailand' WHEN 13 THEN 'Singapore' ELSE 'Malaysia'
     END),
    DATE '2015-01-01' + (seq % 3650),
    (CASE (seq % 3)
        WHEN 0 THEN 'individual'
        WHEN 1 THEN 'business'
        ELSE 'premium'
     END),
    (seq % 1001)
FROM generate_series(5000001, 6000000) seq;

SELECT 'CUSTOMERS BATCH 6 COMPLETED: ' || COUNT(*) FROM customers;

-- BLOCK 7: Customers 6,000,001-7,000,000
INSERT INTO customers (customer_code, customer_name, email, phone, address, city, country, registration_date, customer_type, credit_rating)
SELECT
    'CUST' || LPAD(seq::text, 8, '0'),
    'Customer_' || seq,
    'user' || seq || '@company.com',
    '+84-9' || LPAD((seq % 1000000)::text, 6, '0'),
    'Address_' || (seq % 50000) || '_Street',
    (CASE (seq % 25)
        WHEN 0 THEN 'Hanoi' WHEN 1 THEN 'HCMC' WHEN 2 THEN 'Danang' WHEN 3 THEN 'Hue'
        WHEN 4 THEN 'CanTho' WHEN 5 THEN 'NhaTrang' WHEN 6 THEN 'VungTau' WHEN 7 THEN 'Haiphong'
        WHEN 8 THEN 'Bangkok' WHEN 9 THEN 'Singapore' WHEN 10 THEN 'Tokyo' WHEN 11 THEN 'Seoul'
        WHEN 12 THEN 'Beijing' WHEN 13 THEN 'Shanghai' WHEN 14 THEN 'Sydney' WHEN 15 THEN 'Melbourne'
        WHEN 16 THEN 'London' WHEN 17 THEN 'NewYork' WHEN 18 THEN 'Paris' WHEN 19 THEN 'Berlin'
        WHEN 20 THEN 'Dubai' WHEN 21 THEN 'Mumbai' WHEN 22 THEN 'Toronto' WHEN 23 THEN 'LosAngeles'
        ELSE 'OtherCity'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Vietnam' WHEN 1 THEN 'USA' WHEN 2 THEN 'UK' WHEN 3 THEN 'Japan'
        WHEN 4 THEN 'Korea' WHEN 5 THEN 'China' WHEN 6 THEN 'Australia' WHEN 7 THEN 'Germany'
        WHEN 8 THEN 'France' WHEN 9 THEN 'Canada' WHEN 10 THEN 'India' WHEN 11 THEN 'UAE'
        WHEN 12 THEN 'Thailand' WHEN 13 THEN 'Singapore' ELSE 'Malaysia'
     END),
    DATE '2015-01-01' + (seq % 3650),
    (CASE (seq % 3)
        WHEN 0 THEN 'individual'
        WHEN 1 THEN 'business'
        ELSE 'premium'
     END),
    (seq % 1001)
FROM generate_series(6000001, 7000000) seq;

SELECT 'CUSTOMERS BATCH 7 COMPLETED: ' || COUNT(*) FROM customers;

-- BLOCK 8: Customers 7,000,001-8,000,000
INSERT INTO customers (customer_code, customer_name, email, phone, address, city, country, registration_date, customer_type, credit_rating)
SELECT
    'CUST' || LPAD(seq::text, 8, '0'),
    'Customer_' || seq,
    'user' || seq || '@company.com',
    '+84-9' || LPAD((seq % 1000000)::text, 6, '0'),
    'Address_' || (seq % 50000) || '_Street',
    (CASE (seq % 25)
        WHEN 0 THEN 'Hanoi' WHEN 1 THEN 'HCMC' WHEN 2 THEN 'Danang' WHEN 3 THEN 'Hue'
        WHEN 4 THEN 'CanTho' WHEN 5 THEN 'NhaTrang' WHEN 6 THEN 'VungTau' WHEN 7 THEN 'Haiphong'
        WHEN 8 THEN 'Bangkok' WHEN 9 THEN 'Singapore' WHEN 10 THEN 'Tokyo' WHEN 11 THEN 'Seoul'
        WHEN 12 THEN 'Beijing' WHEN 13 THEN 'Shanghai' WHEN 14 THEN 'Sydney' WHEN 15 THEN 'Melbourne'
        WHEN 16 THEN 'London' WHEN 17 THEN 'NewYork' WHEN 18 THEN 'Paris' WHEN 19 THEN 'Berlin'
        WHEN 20 THEN 'Dubai' WHEN 21 THEN 'Mumbai' WHEN 22 THEN 'Toronto' WHEN 23 THEN 'LosAngeles'
        ELSE 'OtherCity'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Vietnam' WHEN 1 THEN 'USA' WHEN 2 THEN 'UK' WHEN 3 THEN 'Japan'
        WHEN 4 THEN 'Korea' WHEN 5 THEN 'China' WHEN 6 THEN 'Australia' WHEN 7 THEN 'Germany'
        WHEN 8 THEN 'France' WHEN 9 THEN 'Canada' WHEN 10 THEN 'India' WHEN 11 THEN 'UAE'
        WHEN 12 THEN 'Thailand' WHEN 13 THEN 'Singapore' ELSE 'Malaysia'
     END),
    DATE '2015-01-01' + (seq % 3650),
    (CASE (seq % 3)
        WHEN 0 THEN 'individual'
        WHEN 1 THEN 'business'
        ELSE 'premium'
     END),
    (seq % 1001)
FROM generate_series(7000001, 8000000) seq;

SELECT 'CUSTOMERS BATCH 8 COMPLETED: ' || COUNT(*) FROM customers;

-- BLOCK 9: Customers 8,000,001-9,000,000
INSERT INTO customers (customer_code, customer_name, email, phone, address, city, country, registration_date, customer_type, credit_rating)
SELECT
    'CUST' || LPAD(seq::text, 8, '0'),
    'Customer_' || seq,
    'user' || seq || '@company.com',
    '+84-9' || LPAD((seq % 1000000)::text, 6, '0'),
    'Address_' || (seq % 50000) || '_Street',
    (CASE (seq % 25)
        WHEN 0 THEN 'Hanoi' WHEN 1 THEN 'HCMC' WHEN 2 THEN 'Danang' WHEN 3 THEN 'Hue'
        WHEN 4 THEN 'CanTho' WHEN 5 THEN 'NhaTrang' WHEN 6 THEN 'VungTau' WHEN 7 THEN 'Haiphong'
        WHEN 8 THEN 'Bangkok' WHEN 9 THEN 'Singapore' WHEN 10 THEN 'Tokyo' WHEN 11 THEN 'Seoul'
        WHEN 12 THEN 'Beijing' WHEN 13 THEN 'Shanghai' WHEN 14 THEN 'Sydney' WHEN 15 THEN 'Melbourne'
        WHEN 16 THEN 'London' WHEN 17 THEN 'NewYork' WHEN 18 THEN 'Paris' WHEN 19 THEN 'Berlin'
        WHEN 20 THEN 'Dubai' WHEN 21 THEN 'Mumbai' WHEN 22 THEN 'Toronto' WHEN 23 THEN 'LosAngeles'
        ELSE 'OtherCity'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Vietnam' WHEN 1 THEN 'USA' WHEN 2 THEN 'UK' WHEN 3 THEN 'Japan'
        WHEN 4 THEN 'Korea' WHEN 5 THEN 'China' WHEN 6 THEN 'Australia' WHEN 7 THEN 'Germany'
        WHEN 8 THEN 'France' WHEN 9 THEN 'Canada' WHEN 10 THEN 'India' WHEN 11 THEN 'UAE'
        WHEN 12 THEN 'Thailand' WHEN 13 THEN 'Singapore' ELSE 'Malaysia'
     END),
    DATE '2015-01-01' + (seq % 3650),
    (CASE (seq % 3)
        WHEN 0 THEN 'individual'
        WHEN 1 THEN 'business'
        ELSE 'premium'
     END),
    (seq % 1001)
FROM generate_series(8000001, 9000000) seq;

SELECT 'CUSTOMERS BATCH 9 COMPLETED: ' || COUNT(*) FROM customers;

-- BLOCK 10: Customers 9,000,001-10,000,000
INSERT INTO customers (customer_code, customer_name, email, phone, address, city, country, registration_date, customer_type, credit_rating)
SELECT
    'CUST' || LPAD(seq::text, 8, '0'),
    'Customer_' || seq,
    'user' || seq || '@company.com',
    '+84-9' || LPAD((seq % 1000000)::text, 6, '0'),
    'Address_' || (seq % 50000) || '_Street',
    (CASE (seq % 25)
        WHEN 0 THEN 'Hanoi' WHEN 1 THEN 'HCMC' WHEN 2 THEN 'Danang' WHEN 3 THEN 'Hue'
        WHEN 4 THEN 'CanTho' WHEN 5 THEN 'NhaTrang' WHEN 6 THEN 'VungTau' WHEN 7 THEN 'Haiphong'
        WHEN 8 THEN 'Bangkok' WHEN 9 THEN 'Singapore' WHEN 10 THEN 'Tokyo' WHEN 11 THEN 'Seoul'
        WHEN 12 THEN 'Beijing' WHEN 13 THEN 'Shanghai' WHEN 14 THEN 'Sydney' WHEN 15 THEN 'Melbourne'
        WHEN 16 THEN 'London' WHEN 17 THEN 'NewYork' WHEN 18 THEN 'Paris' WHEN 19 THEN 'Berlin'
        WHEN 20 THEN 'Dubai' WHEN 21 THEN 'Mumbai' WHEN 22 THEN 'Toronto' WHEN 23 THEN 'LosAngeles'
        ELSE 'OtherCity'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Vietnam' WHEN 1 THEN 'USA' WHEN 2 THEN 'UK' WHEN 3 THEN 'Japan'
        WHEN 4 THEN 'Korea' WHEN 5 THEN 'China' WHEN 6 THEN 'Australia' WHEN 7 THEN 'Germany'
        WHEN 8 THEN 'France' WHEN 9 THEN 'Canada' WHEN 10 THEN 'India' WHEN 11 THEN 'UAE'
        WHEN 12 THEN 'Thailand' WHEN 13 THEN 'Singapore' ELSE 'Malaysia'
     END),
    DATE '2015-01-01' + (seq % 3650),
    (CASE (seq % 3)
        WHEN 0 THEN 'individual'
        WHEN 1 THEN 'business'
        ELSE 'premium'
     END),
    (seq % 1001)
FROM generate_series(9000001, 10000000) seq;

SELECT 'CUSTOMERS BATCH 10 COMPLETED: ' || COUNT(*) FROM customers;
SELECT 'TOTAL CUSTOMERS: ' || COUNT(*) FROM customers;

-- =============================================
-- BƯỚC 3: INSERT PRODUCTS - 10 TRIỆU RECORDS
-- Chạy lần lượt từng BLOCK 1 đến 10
-- =============================================

-- BLOCK 1: Products 1-1,000,000
INSERT INTO products (product_code, product_name, category, subcategory, brand, unit_price, cost_price, stock_quantity, supplier_id)
SELECT
    'PROD' || LPAD(seq::text, 8, '0'),
    'Product_' || seq || '_' ||
    (CASE (seq % 6)
        WHEN 0 THEN 'Standard' WHEN 1 THEN 'Premium' WHEN 2 THEN 'Professional'
        WHEN 3 THEN 'Enterprise' WHEN 4 THEN 'Ultimate' ELSE 'Basic'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Electronics' WHEN 1 THEN 'Clothing' WHEN 2 THEN 'Books' WHEN 3 THEN 'Home'
        WHEN 4 THEN 'Sports' WHEN 5 THEN 'Beauty' WHEN 6 THEN 'Toys' WHEN 7 THEN 'Automotive'
        WHEN 8 THEN 'Food' WHEN 9 THEN 'Health' WHEN 10 THEN 'Jewelry' WHEN 11 THEN 'Furniture'
        WHEN 12 THEN 'Office' WHEN 13 THEN 'Tools' ELSE 'Garden'
     END),
    'SubCategory_' || ((seq % 20) + 1),
    (CASE (seq % 20)
        WHEN 0 THEN 'BrandA' WHEN 1 THEN 'BrandB' WHEN 2 THEN 'BrandC' WHEN 3 THEN 'BrandD'
        WHEN 4 THEN 'BrandE' WHEN 5 THEN 'BrandF' WHEN 6 THEN 'BrandG' WHEN 7 THEN 'BrandH'
        WHEN 8 THEN 'BrandI' WHEN 9 THEN 'BrandJ' WHEN 10 THEN 'BrandK' WHEN 11 THEN 'BrandL'
        WHEN 12 THEN 'BrandM' WHEN 13 THEN 'BrandN' WHEN 14 THEN 'BrandO' WHEN 15 THEN 'BrandP'
        WHEN 16 THEN 'BrandQ' WHEN 17 THEN 'BrandR' WHEN 18 THEN 'BrandS' ELSE 'BrandT'
     END),
    (10 + (seq % 990))::decimal(15,2),
    (5 + (seq % 495))::decimal(15,2),
    (seq % 1000),
    (seq % 10000)
FROM generate_series(1, 1000000) seq;

SELECT 'PRODUCTS BATCH 1 COMPLETED: ' || COUNT(*) FROM products;

-- BLOCK 2: Products 1,000,001-2,000,000
INSERT INTO products (product_code, product_name, category, subcategory, brand, unit_price, cost_price, stock_quantity, supplier_id)
SELECT
    'PROD' || LPAD(seq::text, 8, '0'),
    'Product_' || seq || '_' ||
    (CASE (seq % 6)
        WHEN 0 THEN 'Standard' WHEN 1 THEN 'Premium' WHEN 2 THEN 'Professional'
        WHEN 3 THEN 'Enterprise' WHEN 4 THEN 'Ultimate' ELSE 'Basic'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Electronics' WHEN 1 THEN 'Clothing' WHEN 2 THEN 'Books' WHEN 3 THEN 'Home'
        WHEN 4 THEN 'Sports' WHEN 5 THEN 'Beauty' WHEN 6 THEN 'Toys' WHEN 7 THEN 'Automotive'
        WHEN 8 THEN 'Food' WHEN 9 THEN 'Health' WHEN 10 THEN 'Jewelry' WHEN 11 THEN 'Furniture'
        WHEN 12 THEN 'Office' WHEN 13 THEN 'Tools' ELSE 'Garden'
     END),
    'SubCategory_' || ((seq % 20) + 1),
    (CASE (seq % 20)
        WHEN 0 THEN 'BrandA' WHEN 1 THEN 'BrandB' WHEN 2 THEN 'BrandC' WHEN 3 THEN 'BrandD'
        WHEN 4 THEN 'BrandE' WHEN 5 THEN 'BrandF' WHEN 6 THEN 'BrandG' WHEN 7 THEN 'BrandH'
        WHEN 8 THEN 'BrandI' WHEN 9 THEN 'BrandJ' WHEN 10 THEN 'BrandK' WHEN 11 THEN 'BrandL'
        WHEN 12 THEN 'BrandM' WHEN 13 THEN 'BrandN' WHEN 14 THEN 'BrandO' WHEN 15 THEN 'BrandP'
        WHEN 16 THEN 'BrandQ' WHEN 17 THEN 'BrandR' WHEN 18 THEN 'BrandS' ELSE 'BrandT'
     END),
    (10 + (seq % 990))::decimal(15,2),
    (5 + (seq % 495))::decimal(15,2),
    (seq % 1000),
    (seq % 10000)
FROM generate_series(1000001, 2000000) seq;

SELECT 'PRODUCTS BATCH 2 COMPLETED: ' || COUNT(*) FROM products;

-- BLOCK 3: Products 2,000,001-3,000,000
INSERT INTO products (product_code, product_name, category, subcategory, brand, unit_price, cost_price, stock_quantity, supplier_id)
SELECT
    'PROD' || LPAD(seq::text, 8, '0'),
    'Product_' || seq || '_' ||
    (CASE (seq % 6)
        WHEN 0 THEN 'Standard' WHEN 1 THEN 'Premium' WHEN 2 THEN 'Professional'
        WHEN 3 THEN 'Enterprise' WHEN 4 THEN 'Ultimate' ELSE 'Basic'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Electronics' WHEN 1 THEN 'Clothing' WHEN 2 THEN 'Books' WHEN 3 THEN 'Home'
        WHEN 4 THEN 'Sports' WHEN 5 THEN 'Beauty' WHEN 6 THEN 'Toys' WHEN 7 THEN 'Automotive'
        WHEN 8 THEN 'Food' WHEN 9 THEN 'Health' WHEN 10 THEN 'Jewelry' WHEN 11 THEN 'Furniture'
        WHEN 12 THEN 'Office' WHEN 13 THEN 'Tools' ELSE 'Garden'
     END),
    'SubCategory_' || ((seq % 20) + 1),
    (CASE (seq % 20)
        WHEN 0 THEN 'BrandA' WHEN 1 THEN 'BrandB' WHEN 2 THEN 'BrandC' WHEN 3 THEN 'BrandD'
        WHEN 4 THEN 'BrandE' WHEN 5 THEN 'BrandF' WHEN 6 THEN 'BrandG' WHEN 7 THEN 'BrandH'
        WHEN 8 THEN 'BrandI' WHEN 9 THEN 'BrandJ' WHEN 10 THEN 'BrandK' WHEN 11 THEN 'BrandL'
        WHEN 12 THEN 'BrandM' WHEN 13 THEN 'BrandN' WHEN 14 THEN 'BrandO' WHEN 15 THEN 'BrandP'
        WHEN 16 THEN 'BrandQ' WHEN 17 THEN 'BrandR' WHEN 18 THEN 'BrandS' ELSE 'BrandT'
     END),
    (10 + (seq % 990))::decimal(15,2),
    (5 + (seq % 495))::decimal(15,2),
    (seq % 1000),
    (seq % 10000)
FROM generate_series(2000001, 3000000) seq;

SELECT 'PRODUCTS BATCH 3 COMPLETED: ' || COUNT(*) FROM products;

-- BLOCK 4: Products 3,000,001-4,000,000
INSERT INTO products (product_code, product_name, category, subcategory, brand, unit_price, cost_price, stock_quantity, supplier_id)
SELECT
    'PROD' || LPAD(seq::text, 8, '0'),
    'Product_' || seq || '_' ||
    (CASE (seq % 6)
        WHEN 0 THEN 'Standard' WHEN 1 THEN 'Premium' WHEN 2 THEN 'Professional'
        WHEN 3 THEN 'Enterprise' WHEN 4 THEN 'Ultimate' ELSE 'Basic'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Electronics' WHEN 1 THEN 'Clothing' WHEN 2 THEN 'Books' WHEN 3 THEN 'Home'
        WHEN 4 THEN 'Sports' WHEN 5 THEN 'Beauty' WHEN 6 THEN 'Toys' WHEN 7 THEN 'Automotive'
        WHEN 8 THEN 'Food' WHEN 9 THEN 'Health' WHEN 10 THEN 'Jewelry' WHEN 11 THEN 'Furniture'
        WHEN 12 THEN 'Office' WHEN 13 THEN 'Tools' ELSE 'Garden'
     END),
    'SubCategory_' || ((seq % 20) + 1),
    (CASE (seq % 20)
        WHEN 0 THEN 'BrandA' WHEN 1 THEN 'BrandB' WHEN 2 THEN 'BrandC' WHEN 3 THEN 'BrandD'
        WHEN 4 THEN 'BrandE' WHEN 5 THEN 'BrandF' WHEN 6 THEN 'BrandG' WHEN 7 THEN 'BrandH'
        WHEN 8 THEN 'BrandI' WHEN 9 THEN 'BrandJ' WHEN 10 THEN 'BrandK' WHEN 11 THEN 'BrandL'
        WHEN 12 THEN 'BrandM' WHEN 13 THEN 'BrandN' WHEN 14 THEN 'BrandO' WHEN 15 THEN 'BrandP'
        WHEN 16 THEN 'BrandQ' WHEN 17 THEN 'BrandR' WHEN 18 THEN 'BrandS' ELSE 'BrandT'
     END),
    (10 + (seq % 990))::decimal(15,2),
    (5 + (seq % 495))::decimal(15,2),
    (seq % 1000),
    (seq % 10000)
FROM generate_series(3000001, 4000000) seq;

SELECT 'PRODUCTS BATCH 4 COMPLETED: ' || COUNT(*) FROM products;

-- BLOCK 5: Products 4,000,001-5,000,000
INSERT INTO products (product_code, product_name, category, subcategory, brand, unit_price, cost_price, stock_quantity, supplier_id)
SELECT
    'PROD' || LPAD(seq::text, 8, '0'),
    'Product_' || seq || '_' ||
    (CASE (seq % 6)
        WHEN 0 THEN 'Standard' WHEN 1 THEN 'Premium' WHEN 2 THEN 'Professional'
        WHEN 3 THEN 'Enterprise' WHEN 4 THEN 'Ultimate' ELSE 'Basic'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Electronics' WHEN 1 THEN 'Clothing' WHEN 2 THEN 'Books' WHEN 3 THEN 'Home'
        WHEN 4 THEN 'Sports' WHEN 5 THEN 'Beauty' WHEN 6 THEN 'Toys' WHEN 7 THEN 'Automotive'
        WHEN 8 THEN 'Food' WHEN 9 THEN 'Health' WHEN 10 THEN 'Jewelry' WHEN 11 THEN 'Furniture'
        WHEN 12 THEN 'Office' WHEN 13 THEN 'Tools' ELSE 'Garden'
     END),
    'SubCategory_' || ((seq % 20) + 1),
    (CASE (seq % 20)
        WHEN 0 THEN 'BrandA' WHEN 1 THEN 'BrandB' WHEN 2 THEN 'BrandC' WHEN 3 THEN 'BrandD'
        WHEN 4 THEN 'BrandE' WHEN 5 THEN 'BrandF' WHEN 6 THEN 'BrandG' WHEN 7 THEN 'BrandH'
        WHEN 8 THEN 'BrandI' WHEN 9 THEN 'BrandJ' WHEN 10 THEN 'BrandK' WHEN 11 THEN 'BrandL'
        WHEN 12 THEN 'BrandM' WHEN 13 THEN 'BrandN' WHEN 14 THEN 'BrandO' WHEN 15 THEN 'BrandP'
        WHEN 16 THEN 'BrandQ' WHEN 17 THEN 'BrandR' WHEN 18 THEN 'BrandS' ELSE 'BrandT'
     END),
    (10 + (seq % 990))::decimal(15,2),
    (5 + (seq % 495))::decimal(15,2),
    (seq % 1000),
    (seq % 10000)
FROM generate_series(4000001, 5000000) seq;

SELECT 'PRODUCTS BATCH 5 COMPLETED: ' || COUNT(*) FROM products;

-- BLOCK 6: Products 5,000,001-6,000,000
INSERT INTO products (product_code, product_name, category, subcategory, brand, unit_price, cost_price, stock_quantity, supplier_id)
SELECT
    'PROD' || LPAD(seq::text, 8, '0'),
    'Product_' || seq || '_' ||
    (CASE (seq % 6)
        WHEN 0 THEN 'Standard' WHEN 1 THEN 'Premium' WHEN 2 THEN 'Professional'
        WHEN 3 THEN 'Enterprise' WHEN 4 THEN 'Ultimate' ELSE 'Basic'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Electronics' WHEN 1 THEN 'Clothing' WHEN 2 THEN 'Books' WHEN 3 THEN 'Home'
        WHEN 4 THEN 'Sports' WHEN 5 THEN 'Beauty' WHEN 6 THEN 'Toys' WHEN 7 THEN 'Automotive'
        WHEN 8 THEN 'Food' WHEN 9 THEN 'Health' WHEN 10 THEN 'Jewelry' WHEN 11 THEN 'Furniture'
        WHEN 12 THEN 'Office' WHEN 13 THEN 'Tools' ELSE 'Garden'
     END),
    'SubCategory_' || ((seq % 20) + 1),
    (CASE (seq % 20)
        WHEN 0 THEN 'BrandA' WHEN 1 THEN 'BrandB' WHEN 2 THEN 'BrandC' WHEN 3 THEN 'BrandD'
        WHEN 4 THEN 'BrandE' WHEN 5 THEN 'BrandF' WHEN 6 THEN 'BrandG' WHEN 7 THEN 'BrandH'
        WHEN 8 THEN 'BrandI' WHEN 9 THEN 'BrandJ' WHEN 10 THEN 'BrandK' WHEN 11 THEN 'BrandL'
        WHEN 12 THEN 'BrandM' WHEN 13 THEN 'BrandN' WHEN 14 THEN 'BrandO' WHEN 15 THEN 'BrandP'
        WHEN 16 THEN 'BrandQ' WHEN 17 THEN 'BrandR' WHEN 18 THEN 'BrandS' ELSE 'BrandT'
     END),
    (10 + (seq % 990))::decimal(15,2),
    (5 + (seq % 495))::decimal(15,2),
    (seq % 1000),
    (seq % 10000)
FROM generate_series(5000001, 6000000) seq;

SELECT 'PRODUCTS BATCH 6 COMPLETED: ' || COUNT(*) FROM products;

-- BLOCK 7: Products 6,000,001-7,000,000
INSERT INTO products (product_code, product_name, category, subcategory, brand, unit_price, cost_price, stock_quantity, supplier_id)
SELECT
    'PROD' || LPAD(seq::text, 8, '0'),
    'Product_' || seq || '_' ||
    (CASE (seq % 6)
        WHEN 0 THEN 'Standard' WHEN 1 THEN 'Premium' WHEN 2 THEN 'Professional'
        WHEN 3 THEN 'Enterprise' WHEN 4 THEN 'Ultimate' ELSE 'Basic'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Electronics' WHEN 1 THEN 'Clothing' WHEN 2 THEN 'Books' WHEN 3 THEN 'Home'
        WHEN 4 THEN 'Sports' WHEN 5 THEN 'Beauty' WHEN 6 THEN 'Toys' WHEN 7 THEN 'Automotive'
        WHEN 8 THEN 'Food' WHEN 9 THEN 'Health' WHEN 10 THEN 'Jewelry' WHEN 11 THEN 'Furniture'
        WHEN 12 THEN 'Office' WHEN 13 THEN 'Tools' ELSE 'Garden'
     END),
    'SubCategory_' || ((seq % 20) + 1),
    (CASE (seq % 20)
        WHEN 0 THEN 'BrandA' WHEN 1 THEN 'BrandB' WHEN 2 THEN 'BrandC' WHEN 3 THEN 'BrandD'
        WHEN 4 THEN 'BrandE' WHEN 5 THEN 'BrandF' WHEN 6 THEN 'BrandG' WHEN 7 THEN 'BrandH'
        WHEN 8 THEN 'BrandI' WHEN 9 THEN 'BrandJ' WHEN 10 THEN 'BrandK' WHEN 11 THEN 'BrandL'
        WHEN 12 THEN 'BrandM' WHEN 13 THEN 'BrandN' WHEN 14 THEN 'BrandO' WHEN 15 THEN 'BrandP'
        WHEN 16 THEN 'BrandQ' WHEN 17 THEN 'BrandR' WHEN 18 THEN 'BrandS' ELSE 'BrandT'
     END),
    (10 + (seq % 990))::decimal(15,2),
    (5 + (seq % 495))::decimal(15,2),
    (seq % 1000),
    (seq % 10000)
FROM generate_series(6000001, 7000000) seq;

SELECT 'PRODUCTS BATCH 7 COMPLETED: ' || COUNT(*) FROM products;

-- BLOCK 8: Products 7,000,001-8,000,000
INSERT INTO products (product_code, product_name, category, subcategory, brand, unit_price, cost_price, stock_quantity, supplier_id)
SELECT
    'PROD' || LPAD(seq::text, 8, '0'),
    'Product_' || seq || '_' ||
    (CASE (seq % 6)
        WHEN 0 THEN 'Standard' WHEN 1 THEN 'Premium' WHEN 2 THEN 'Professional'
        WHEN 3 THEN 'Enterprise' WHEN 4 THEN 'Ultimate' ELSE 'Basic'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Electronics' WHEN 1 THEN 'Clothing' WHEN 2 THEN 'Books' WHEN 3 THEN 'Home'
        WHEN 4 THEN 'Sports' WHEN 5 THEN 'Beauty' WHEN 6 THEN 'Toys' WHEN 7 THEN 'Automotive'
        WHEN 8 THEN 'Food' WHEN 9 THEN 'Health' WHEN 10 THEN 'Jewelry' WHEN 11 THEN 'Furniture'
        WHEN 12 THEN 'Office' WHEN 13 THEN 'Tools' ELSE 'Garden'
     END),
    'SubCategory_' || ((seq % 20) + 1),
    (CASE (seq % 20)
        WHEN 0 THEN 'BrandA' WHEN 1 THEN 'BrandB' WHEN 2 THEN 'BrandC' WHEN 3 THEN 'BrandD'
        WHEN 4 THEN 'BrandE' WHEN 5 THEN 'BrandF' WHEN 6 THEN 'BrandG' WHEN 7 THEN 'BrandH'
        WHEN 8 THEN 'BrandI' WHEN 9 THEN 'BrandJ' WHEN 10 THEN 'BrandK' WHEN 11 THEN 'BrandL'
        WHEN 12 THEN 'BrandM' WHEN 13 THEN 'BrandN' WHEN 14 THEN 'BrandO' WHEN 15 THEN 'BrandP'
        WHEN 16 THEN 'BrandQ' WHEN 17 THEN 'BrandR' WHEN 18 THEN 'BrandS' ELSE 'BrandT'
     END),
    (10 + (seq % 990))::decimal(15,2),
    (5 + (seq % 495))::decimal(15,2),
    (seq % 1000),
    (seq % 10000)
FROM generate_series(7000001, 8000000) seq;

SELECT 'PRODUCTS BATCH 8 COMPLETED: ' || COUNT(*) FROM products;

-- BLOCK 9: Products 8,000,001-9,000,000
INSERT INTO products (product_code, product_name, category, subcategory, brand, unit_price, cost_price, stock_quantity, supplier_id)
SELECT
    'PROD' || LPAD(seq::text, 8, '0'),
    'Product_' || seq || '_' ||
    (CASE (seq % 6)
        WHEN 0 THEN 'Standard' WHEN 1 THEN 'Premium' WHEN 2 THEN 'Professional'
        WHEN 3 THEN 'Enterprise' WHEN 4 THEN 'Ultimate' ELSE 'Basic'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Electronics' WHEN 1 THEN 'Clothing' WHEN 2 THEN 'Books' WHEN 3 THEN 'Home'
        WHEN 4 THEN 'Sports' WHEN 5 THEN 'Beauty' WHEN 6 THEN 'Toys' WHEN 7 THEN 'Automotive'
        WHEN 8 THEN 'Food' WHEN 9 THEN 'Health' WHEN 10 THEN 'Jewelry' WHEN 11 THEN 'Furniture'
        WHEN 12 THEN 'Office' WHEN 13 THEN 'Tools' ELSE 'Garden'
     END),
    'SubCategory_' || ((seq % 20) + 1),
    (CASE (seq % 20)
        WHEN 0 THEN 'BrandA' WHEN 1 THEN 'BrandB' WHEN 2 THEN 'BrandC' WHEN 3 THEN 'BrandD'
        WHEN 4 THEN 'BrandE' WHEN 5 THEN 'BrandF' WHEN 6 THEN 'BrandG' WHEN 7 THEN 'BrandH'
        WHEN 8 THEN 'BrandI' WHEN 9 THEN 'BrandJ' WHEN 10 THEN 'BrandK' WHEN 11 THEN 'BrandL'
        WHEN 12 THEN 'BrandM' WHEN 13 THEN 'BrandN' WHEN 14 THEN 'BrandO' WHEN 15 THEN 'BrandP'
        WHEN 16 THEN 'BrandQ' WHEN 17 THEN 'BrandR' WHEN 18 THEN 'BrandS' ELSE 'BrandT'
     END),
    (10 + (seq % 990))::decimal(15,2),
    (5 + (seq % 495))::decimal(15,2),
    (seq % 1000),
    (seq % 10000)
FROM generate_series(8000001, 9000000) seq;

SELECT 'PRODUCTS BATCH 9 COMPLETED: ' || COUNT(*) FROM products;

-- BLOCK 10: Products 9,000,001-10,000,000
INSERT INTO products (product_code, product_name, category, subcategory, brand, unit_price, cost_price, stock_quantity, supplier_id)
SELECT
    'PROD' || LPAD(seq::text, 8, '0'),
    'Product_' || seq || '_' ||
    (CASE (seq % 6)
        WHEN 0 THEN 'Standard' WHEN 1 THEN 'Premium' WHEN 2 THEN 'Professional'
        WHEN 3 THEN 'Enterprise' WHEN 4 THEN 'Ultimate' ELSE 'Basic'
     END),
    (CASE (seq % 15)
        WHEN 0 THEN 'Electronics' WHEN 1 THEN 'Clothing' WHEN 2 THEN 'Books' WHEN 3 THEN 'Home'
        WHEN 4 THEN 'Sports' WHEN 5 THEN 'Beauty' WHEN 6 THEN 'Toys' WHEN 7 THEN 'Automotive'
        WHEN 8 THEN 'Food' WHEN 9 THEN 'Health' WHEN 10 THEN 'Jewelry' WHEN 11 THEN 'Furniture'
        WHEN 12 THEN 'Office' WHEN 13 THEN 'Tools' ELSE 'Garden'
     END),
    'SubCategory_' || ((seq % 20) + 1),
    (CASE (seq % 20)
        WHEN 0 THEN 'BrandA' WHEN 1 THEN 'BrandB' WHEN 2 THEN 'BrandC' WHEN 3 THEN 'BrandD'
        WHEN 4 THEN 'BrandE' WHEN 5 THEN 'BrandF' WHEN 6 THEN 'BrandG' WHEN 7 THEN 'BrandH'
        WHEN 8 THEN 'BrandI' WHEN 9 THEN 'BrandJ' WHEN 10 THEN 'BrandK' WHEN 11 THEN 'BrandL'
        WHEN 12 THEN 'BrandM' WHEN 13 THEN 'BrandN' WHEN 14 THEN 'BrandO' WHEN 15 THEN 'BrandP'
        WHEN 16 THEN 'BrandQ' WHEN 17 THEN 'BrandR' WHEN 18 THEN 'BrandS' ELSE 'BrandT'
     END),
    (10 + (seq % 990))::decimal(15,2),
    (5 + (seq % 495))::decimal(15,2),
    (seq % 1000),
    (seq % 10000)
FROM generate_series(9000001, 10000000) seq;

SELECT 'PRODUCTS BATCH 10 COMPLETED: ' || COUNT(*) FROM products;
SELECT 'TOTAL PRODUCTS: ' || COUNT(*) FROM products;

-- =============================================
-- BƯỚC 5: INSERT ORDER_DETAILS - 10 TRIỆU RECORDS
-- Chạy lần lượt từng BLOCK 1 đến 10
-- =============================================

-- BLOCK 1: Order_Details 1-1,000,000
INSERT INTO order_details (order_id, product_id, quantity, unit_price, discount)
SELECT
    1 + (seq % (SELECT count(*) FROM orders)),
    1 + ((seq * 13 + 7) % (SELECT count(*) FROM products)),
    (1 + (seq % 20))::int,
    (5 + (seq % 495))::decimal(15,2),
    (CASE WHEN (seq % 4) = 0 THEN (seq % 30)::decimal(5,2) ELSE 0 END)
FROM generate_series(1, 1000000) seq;

SELECT 'ORDER_DETAILS BATCH 1 COMPLETED: ' || COUNT(*) FROM order_details;

-- BLOCK 2: Order_Details 1,000,001-2,000,000
INSERT INTO order_details (order_id, product_id, quantity, unit_price, discount)
SELECT
    1 + (seq % (SELECT count(*) FROM orders)),
    1 + ((seq * 13 + 7) % (SELECT count(*) FROM products)),
    (1 + (seq % 20))::int,
    (5 + (seq % 495))::decimal(15,2),
    (CASE WHEN (seq % 4) = 0 THEN (seq % 30)::decimal(5,2) ELSE 0 END)
FROM generate_series(1000001, 2000000) seq;

SELECT 'ORDER_DETAILS BATCH 2 COMPLETED: ' || COUNT(*) FROM order_details;

-- BLOCK 3: Order_Details 2,000,001-3,000,000
INSERT INTO order_details (order_id, product_id, quantity, unit_price, discount)
SELECT
    1 + (seq % (SELECT count(*) FROM orders)),
    1 + ((seq * 13 + 7) % (SELECT count(*) FROM products)),
    (1 + (seq % 20))::int,
    (5 + (seq % 495))::decimal(15,2),
    (CASE WHEN (seq % 4) = 0 THEN (seq % 30)::decimal(5,2) ELSE 0 END)
FROM generate_series(2000001, 3000000) seq;

SELECT 'ORDER_DETAILS BATCH 3 COMPLETED: ' || COUNT(*) FROM order_details;

-- BLOCK 4: Order_Details 3,000,001-4,000,000
INSERT INTO order_details (order_id, product_id, quantity, unit_price, discount)
SELECT
    1 + (seq % (SELECT count(*) FROM orders)),
    1 + ((seq * 13 + 7) % (SELECT count(*) FROM products)),
    (1 + (seq % 20))::int,
    (5 + (seq % 495))::decimal(15,2),
    (CASE WHEN (seq % 4) = 0 THEN (seq % 30)::decimal(5,2) ELSE 0 END)
FROM generate_series(3000001, 4000000) seq;

SELECT 'ORDER_DETAILS BATCH 4 COMPLETED: ' || COUNT(*) FROM order_details;

-- BLOCK 5: Order_Details 4,000,001-5,000,000
INSERT INTO order_details (order_id, product_id, quantity, unit_price, discount)
SELECT
    1 + (seq % (SELECT count(*) FROM orders)),
    1 + ((seq * 13 + 7) % (SELECT count(*) FROM products)),
    (1 + (seq % 20))::int,
    (5 + (seq % 495))::decimal(15,2),
    (CASE WHEN (seq % 4) = 0 THEN (seq % 30)::decimal(5,2) ELSE 0 END)
FROM generate_series(4000001, 5000000) seq;

SELECT 'ORDER_DETAILS BATCH 5 COMPLETED: ' || COUNT(*) FROM order_details;

-- BLOCK 6: Order_Details 5,000,001-6,000,000
INSERT INTO order_details (order_id, product_id, quantity, unit_price, discount)
SELECT
    1 + (seq % (SELECT count(*) FROM orders)),
    1 + ((seq * 13 + 7) % (SELECT count(*) FROM products)),
    (1 + (seq % 20))::int,
    (5 + (seq % 495))::decimal(15,2),
    (CASE WHEN (seq % 4) = 0 THEN (seq % 30)::decimal(5,2) ELSE 0 END)
FROM generate_series(5000001, 6000000) seq;

SELECT 'ORDER_DETAILS BATCH 6 COMPLETED: ' || COUNT(*) FROM order_details;

-- BLOCK 7: Order_Details 6,000,001-7,000,000
INSERT INTO order_details (order_id, product_id, quantity, unit_price, discount)
SELECT
    1 + (seq % (SELECT count(*) FROM orders)),
    1 + ((seq * 13 + 7) % (SELECT count(*) FROM products)),
    (1 + (seq % 20))::int,
    (5 + (seq % 495))::decimal(15,2),
    (CASE WHEN (seq % 4) = 0 THEN (seq % 30)::decimal(5,2) ELSE 0 END)
FROM generate_series(6000001, 7000000) seq;

SELECT 'ORDER_DETAILS BATCH 7 COMPLETED: ' || COUNT(*) FROM order_details;

-- BLOCK 8: Order_Details 7,000,001-8,000,000
INSERT INTO order_details (order_id, product_id, quantity, unit_price, discount)
SELECT
    1 + (seq % (SELECT count(*) FROM orders)),
    1 + ((seq * 13 + 7) % (SELECT count(*) FROM products)),
    (1 + (seq % 20))::int,
    (5 + (seq % 495))::decimal(15,2),
    (CASE WHEN (seq % 4) = 0 THEN (seq % 30)::decimal(5,2) ELSE 0 END)
FROM generate_series(7000001, 8000000) seq;

SELECT 'ORDER_DETAILS BATCH 8 COMPLETED: ' || COUNT(*) FROM order_details;

-- BLOCK 9: Order_Details 8,000,001-9,000,000
INSERT INTO order_details (order_id, product_id, quantity, unit_price, discount)
SELECT
    1 + (seq % (SELECT count(*) FROM orders)),
    1 + ((seq * 13 + 7) % (SELECT count(*) FROM products)),
    (1 + (seq % 20))::int,
    (5 + (seq % 495))::decimal(15,2),
    (CASE WHEN (seq % 4) = 0 THEN (seq % 30)::decimal(5,2) ELSE 0 END)
FROM generate_series(8000001, 9000000) seq;

SELECT 'ORDER_DETAILS BATCH 9 COMPLETED: ' || COUNT(*) FROM order_details;

-- BLOCK 10: Order_Details 9,000,001-10,000,000
INSERT INTO order_details (order_id, product_id, quantity, unit_price, discount)
SELECT
    1 + (seq % (SELECT count(*) FROM orders)),
    1 + ((seq * 13 + 7) % (SELECT count(*) FROM products)),
    (1 + (seq % 20))::int,
    (5 + (seq % 495))::decimal(15,2),
    (CASE WHEN (seq % 4) = 0 THEN (seq % 30)::decimal(5,2) ELSE 0 END)
FROM generate_series(9000001, 10000000) seq;

SELECT 'ORDER_DETAILS BATCH 10 COMPLETED: ' || COUNT(*) FROM order_details;
SELECT 'TOTAL ORDER_DETAILS: ' || COUNT(*) FROM order_details;

// https://chat.deepseek.com/a/chat/s/6cd0ac43-e742-46a7-af9a-ce5390f19872