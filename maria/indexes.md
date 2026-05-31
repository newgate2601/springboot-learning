# MariaDB Indexes

## 1. Index là gì?

**Index** trong MariaDB là cấu trúc dữ liệu giúp database tìm row nhanh hơn thay vì phải quét toàn bộ bảng.

Có thể hiểu index giống như mục lục của sách:

- Không có index: database đọc từ đầu đến cuối bảng để tìm dữ liệu.
- Có index: database tìm trong cấu trúc index trước, sau đó lấy row từ bảng.

Ví dụ:

```sql
SELECT *
FROM orders
WHERE customer_id = 10;
```

Nếu bảng `orders` có rất nhiều row và thường xuyên query theo `customer_id`, ta có thể tạo index:

```sql
CREATE INDEX idx_orders_customer_id
ON orders(customer_id);
```

Trong MariaDB/MySQL, `KEY` và `INDEX` gần như là alias:

```sql
KEY idx_name (name)
```

gần như tương đương:

```sql
INDEX idx_name (name)
```

---

## 2. Điểm dễ nhầm: Primary Key, Unique và Index

Khi khai báo `PRIMARY KEY` hoặc `UNIQUE`, MariaDB sẽ tự tạo index tương ứng.

Ví dụ:

```sql
CREATE TABLE users (
  id BIGINT NOT NULL,
  email VARCHAR(255) NOT NULL,
  name VARCHAR(100),

  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB;
```

MariaDB sẽ tạo:

- một index cho `PRIMARY KEY (id)`
- một unique index cho `UNIQUE KEY uk_users_email (email)`

Không cần tạo thêm:

```sql
CREATE INDEX idx_users_id ON users(id);
CREATE INDEX idx_users_email ON users(email);
```

Hai index này thường là thừa, vì `PRIMARY KEY` và `UNIQUE KEY` đã tạo index rồi.

### Cách hiểu đúng

```text
PRIMARY KEY
= constraint: không trùng, không NULL, định danh row
= index: database tạo index để enforce constraint và query nhanh

UNIQUE KEY
= constraint: không trùng giá trị
= index: database tạo unique index để enforce constraint và query nhanh

INDEX / KEY
= index bình thường
= không enforce unique, chỉ hỗ trợ query nhanh hơn
```

Tài liệu về index thường nói cả `PRIMARY KEY` và `UNIQUE` vì về mặt implementation, chúng cũng được backed by index.

---

## 3. BTREE là mặc định

Với bảng InnoDB thông thường, các loại sau mặc định là **BTREE**:

- `PRIMARY KEY`
- `UNIQUE KEY`
- `INDEX`
- `KEY`

Ví dụ:

```sql
CREATE TABLE users (
  id BIGINT NOT NULL,
  email VARCHAR(255) NOT NULL,
  city VARCHAR(100),

  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  KEY idx_users_city (city)
) ENGINE=InnoDB;
```

Có thể hiểu gần như:

```sql
PRIMARY KEY (id) USING BTREE
UNIQUE KEY uk_users_email (email) USING BTREE
KEY idx_users_city (city) USING BTREE
```

Thông thường không cần viết `USING BTREE` vì đó là mặc định.

Một số trường hợp đặc biệt có thể dùng loại khác:

- `FULLTEXT`
- `SPATIAL`
- `HASH` trong một số engine/trường hợp đặc biệt

---

## 4. Primary Key

`PRIMARY KEY` dùng để định danh duy nhất mỗi row trong bảng.

Ví dụ:

```sql
CREATE TABLE employees (
  id BIGINT NOT NULL AUTO_INCREMENT,
  employee_code VARCHAR(50) NOT NULL,
  full_name VARCHAR(255) NOT NULL,

  PRIMARY KEY (id)
) ENGINE=InnoDB;
```

Đặc điểm:

- Mỗi bảng chỉ có một `PRIMARY KEY`.
- Giá trị không được trùng.
- Giá trị không được `NULL`.
- Thường dùng với `AUTO_INCREMENT`.
- Trong InnoDB, primary key là clustered index.

### Clustered index trong InnoDB

Với InnoDB, data row được tổ chức theo primary key. Vì vậy primary key nên:

- ngắn gọn
- ổn định, ít thay đổi
- có tính tăng dần nếu phù hợp, ví dụ `BIGINT AUTO_INCREMENT`

Không nên chọn primary key quá dài nếu không cần thiết, vì primary key sẽ được gắn kèm vào các secondary index.

### Thêm primary key sau khi tạo bảng

```sql
ALTER TABLE employees
ADD PRIMARY KEY (id);
```

Không tạo primary key bằng `CREATE INDEX`, vì primary key là constraint đặc biệt.

---

## 5. Unique Key / Unique Index

`UNIQUE KEY` đảm bảo giá trị trong một cột hoặc một nhóm cột không bị trùng.

Ví dụ:

```sql
CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  phone VARCHAR(30),

  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_phone (phone)
) ENGINE=InnoDB;
```

`email` không được trùng. `phone` cũng không được trùng nếu có giá trị.

Có thể tạo unique key sau:

```sql
ALTER TABLE users
ADD UNIQUE KEY uk_users_email (email);
```

Hoặc:

```sql
CREATE UNIQUE INDEX uk_users_email
ON users(email);
```

### Unique khác Primary Key như thế nào?

| Tiêu chí | Primary Key | Unique Key |
| --- | --- | --- |
| Không trùng | Có | Có |
| Cho phép `NULL` | Không | Có thể có |
| Số lượng trên một bảng | Chỉ 1 | Có thể nhiều |
| Vai trò chính | Định danh row | Chống trùng dữ liệu |
| Có tạo index không | Có | Có |

### Unique và NULL

Trong SQL, `NULL` nghĩa là unknown. Vì vậy `NULL` không được xem là bằng một `NULL` khác.

Ví dụ:

```sql
CREATE TABLE t1 (
  id BIGINT NOT NULL AUTO_INCREMENT,
  phone VARCHAR(30),

  PRIMARY KEY (id),
  UNIQUE KEY uk_t1_phone (phone)
) ENGINE=InnoDB;
```

Các row sau có thể hợp lệ:

```sql
INSERT INTO t1(phone) VALUES (NULL);
INSERT INTO t1(phone) VALUES (NULL);
```

Nhưng các row sau sẽ lỗi duplicate key:

```sql
INSERT INTO t1(phone) VALUES ('0900000000');
INSERT INTO t1(phone) VALUES ('0900000000');
```

---

## 6. Plain Index / Regular Index / Normal Index

**Plain index** là index bình thường, không unique.

Nó chỉ dùng để tăng tốc query, không cấm dữ liệu bị trùng.

Ví dụ:

```sql
CREATE TABLE orders (
  id BIGINT NOT NULL AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at DATETIME NOT NULL,

  PRIMARY KEY (id),
  KEY idx_orders_customer_id (customer_id),
  KEY idx_orders_status_created_at (status, created_at)
) ENGINE=InnoDB;
```

Ở đây:

```sql
KEY idx_orders_customer_id (customer_id)
```

là plain index.

Nhiều order có thể cùng `customer_id`, nên không thể dùng unique:

```text
id | customer_id | status
---+-------------+--------
1  | 10          | PAID
2  | 10          | PENDING
3  | 10          | PAID
```

Plain index giúp query nhanh hơn:

```sql
SELECT *
FROM orders
WHERE customer_id = 10;
```

Nhưng nó không ngăn duplicate:

```sql
INSERT INTO orders(customer_id, status, created_at)
VALUES
  (10, 'PAID', NOW()),
  (10, 'PAID', NOW());
```

Hai row trên vẫn hợp lệ.

### Tóm tắt 3 loại hay gặp

| Khai báo | Tên gọi | Có unique? | Có tạo index? | Mục đích |
| --- | --- | --- | --- | --- |
| `PRIMARY KEY (id)` | Primary key | Có | Có | Định danh row |
| `UNIQUE KEY uk_email (email)` | Unique index | Có | Có | Chống trùng |
| `KEY idx_city (city)` | Plain/regular index | Không | Có | Tăng tốc query |

---

## 7. Multi-column index

Index có thể nằm trên nhiều cột.

Ví dụ:

```sql
CREATE TABLE orders (
  id BIGINT NOT NULL AUTO_INCREMENT,
  customer_id BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at DATETIME NOT NULL,

  PRIMARY KEY (id),
  KEY idx_orders_customer_status_created (customer_id, status, created_at)
) ENGINE=InnoDB;
```

Index `(customer_id, status, created_at)` có thể hữu ích cho:

```sql
SELECT *
FROM orders
WHERE customer_id = 10;
```

```sql
SELECT *
FROM orders
WHERE customer_id = 10
  AND status = 'PAID';
```

```sql
SELECT *
FROM orders
WHERE customer_id = 10
  AND status = 'PAID'
ORDER BY created_at DESC;
```

### Leftmost prefix rule

Với index:

```sql
KEY idx_a_b_c (a, b, c)
```

MariaDB có thể tận dụng tốt index cho:

- `WHERE a = ?`
- `WHERE a = ? AND b = ?`
- `WHERE a = ? AND b = ? AND c = ?`

Nhưng thường không tận dụng tốt cho:

- `WHERE b = ?`
- `WHERE c = ?`
- `WHERE b = ? AND c = ?`

Vì index được sắp xếp theo thứ tự từ trái sang phải.

---

## 8. Nên tạo index khi nào?

Nên cân nhắc tạo index cho các cột hay xuất hiện trong:

- `WHERE`
- `JOIN`
- `ORDER BY`
- `GROUP BY`

Ví dụ:

```sql
SELECT *
FROM orders
WHERE customer_id = 10
ORDER BY created_at DESC;
```

Có thể cần index:

```sql
KEY idx_orders_customer_created (customer_id, created_at)
```

Ví dụ join:

```sql
SELECT o.*
FROM orders o
JOIN users u ON u.id = o.customer_id
WHERE u.email = 'a@example.com';
```

Có thể cần:

```sql
users.email
orders.customer_id
```

Nếu `users.email` đã là `UNIQUE KEY`, không cần tạo thêm plain index cho `email`.

---

## 9. Không nên over-index

Index giúp đọc nhanh hơn, nhưng làm ghi chậm hơn.

Khi chạy:

```sql
INSERT
UPDATE
DELETE
```

MariaDB phải cập nhật cả data và các index liên quan.

Quá nhiều index có thể gây:

- tốn storage
- insert/update/delete chậm
- query optimizer có nhiều lựa chọn hơn
- schema khó bảo trì hơn

Ví dụ có thể bị thừa:

```sql
KEY idx_customer_id (customer_id),
KEY idx_customer_id_status (customer_id, status)
```

Trong nhiều query, `idx_customer_id_status` đã có thể dùng cho điều kiện theo `customer_id`, nên `idx_customer_id` có thể bị thừa. Tuy nhiên vẫn cần kiểm tra bằng query thực tế và `EXPLAIN`.

---

## 10. LIKE và index

BTREE index dùng tốt cho prefix search:

```sql
SELECT *
FROM users
WHERE name LIKE 'nguyen%';
```

Nhưng thường không dùng tốt cho leading wildcard:

```sql
SELECT *
FROM users
WHERE name LIKE '%nguyen%';
```

Vì `%` ở đầu làm database không biết bắt đầu tìm từ đâu trong BTREE.

Nếu cần search text toàn văn, nên cân nhắc `FULLTEXT INDEX`:

```sql
CREATE FULLTEXT INDEX ft_articles_title_body
ON articles(title, body);
```

Query:

```sql
SELECT *
FROM articles
WHERE MATCH(title, body) AGAINST ('database index');
```

---

## 11. Xem index hiện có

Xem index của bảng:

```sql
SHOW INDEX FROM users;
```

Xem câu `CREATE TABLE`, bao gồm cả primary key, unique key và index:

```sql
SHOW CREATE TABLE users\G
```

---

## 12. Kiểm tra query có dùng index không

Dùng `EXPLAIN`:

```sql
EXPLAIN
SELECT *
FROM orders
WHERE customer_id = 10;
```

Cần chú ý các cột như:

- `type`: cách access table
- `possible_keys`: index có thể dùng
- `key`: index thực sự được dùng
- `rows`: số row ước tính phải đọc
- `Extra`: thông tin phụ, ví dụ `Using where`, `Using index`, `Using filesort`

Không nên tạo index chỉ theo cảm tính. Nên kiểm tra query thực tế bằng `EXPLAIN`.

---

## 13. Khi nào nên xóa index?

Có thể xóa index nếu:

- index hiếm khi được dùng
- index trùng tác dụng với index khác
- index làm write chậm rõ rệt
- workload đã thay đổi và query cũ không còn nữa

Xóa plain/unique index:

```sql
DROP INDEX idx_orders_customer_id ON orders;
```

Hoặc:

```sql
ALTER TABLE orders
DROP INDEX idx_orders_customer_id;
```

Xóa primary key:

```sql
ALTER TABLE orders
DROP PRIMARY KEY;
```

Cần cẩn thận khi xóa index trên production. Nên xem slow query log, query pattern và `EXPLAIN` trước.

---

## 14. Checklist ngắn gọn

- `PRIMARY KEY` tự tạo index.
- `UNIQUE KEY` tự tạo unique index.
- `KEY` / `INDEX` là plain index nếu không có `UNIQUE`.
- Với InnoDB thông thường, các index trên mặc định là BTREE.
- Plain index cho phép duplicate, chỉ dùng để tăng tốc query.
- Unique index cấm duplicate, nhưng có thể cho phép nhiều `NULL`.
- Multi-column index phụ thuộc thứ tự cột, hãy nhớ leftmost prefix rule.
- Index tăng tốc read nhưng làm write tốn chi phí hơn.
- Dùng `EXPLAIN` để xác nhận query có dùng index hay không.
