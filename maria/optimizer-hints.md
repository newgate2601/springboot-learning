# MariaDB Optimizer Hints

## 1. SELECT modifier hint

### 1.1. SELECT modifier hint là gì?

**SELECT modifier hint** là các từ khóa đặt ngay sau `SELECT` để đưa thêm tín hiệu cho optimizer hoặc executor của MariaDB.

Ví dụ:

```sql
SELECT SQL_NO_CACHE *
FROM orders
WHERE customer_id = 10;
```

Trong câu trên, `SQL_NO_CACHE` nói với MariaDB rằng query này không nên dùng Query Cache.

Cú pháp tổng quát:

```sql
SELECT [modifier_1] [modifier_2] column_list
FROM table_name
WHERE condition;
```

Cần hiểu đúng:

```text
optimizer hint
= tín hiệu bổ sung cho optimizer/executor
= có thể ảnh hưởng cách lập plan hoặc cách xử lý result
= không thay thế việc thiết kế index, viết query và cập nhật statistics đúng
```

SELECT modifier hint thường dùng khi:

- optimizer chọn plan không tốt trong một query cụ thể;
- cần điều khiển cache kết quả cho từng câu `SELECT`;
- cần ép thứ tự join;
- cần nói cho optimizer biết result set có xu hướng nhỏ/lớn;
- cần tối ưu cách trả result để giải phóng lock sớm;
- cần đo tổng số row phù hợp khi query có `LIMIT`.

### 1.2. Vì sao cần cẩn thận với hint?

Hint có thể giúp query nhanh hơn trong một tình huống cụ thể, nhưng cũng có thể làm hệ thống chậm hơn khi dữ liệu, statistics, version MariaDB hoặc workload thay đổi.

Ví dụ:

```sql
SELECT STRAIGHT_JOIN o.id, c.name
FROM orders o
JOIN customers c ON c.id = o.customer_id
WHERE c.status = 'ACTIVE';
```

`STRAIGHT_JOIN` ép MariaDB đọc bảng theo thứ tự xuất hiện trong query. Nếu thứ tự đó đúng, query có thể nhanh hơn. Nếu sau này bảng `customers` nhỏ hơn nhiều hoặc predicate `c.status = 'ACTIVE'` rất selective, việc ép đọc `orders` trước có thể làm query chậm hơn.

Nguyên tắc thực dụng:

```text
Dùng hint khi đã có bằng chứng bằng EXPLAIN/ANALYZE/benchmark.
Không dùng hint chỉ vì "có vẻ nhanh hơn".
Luôn ghi rõ lý do dùng hint trong code review hoặc tài liệu vận hành.
```

## 2. Các SELECT modifier hints

### 2.1. HIGH_PRIORITY

`HIGH_PRIORITY` tăng ưu tiên cho câu `SELECT`.

Cú pháp:

```sql
SELECT HIGH_PRIORITY *
FROM reports
WHERE status = 'READY';
```

Ý nghĩa:

- nếu bảng đang bị lock, câu `SELECT HIGH_PRIORITY` được ưu tiên chạy sớm khi lock được giải phóng;
- chỉ có ý nghĩa với storage engine dùng table-level locking như `MyISAM`, `MEMORY`, `MERGE`;
- với InnoDB, tác dụng thực tế thường rất hạn chế vì InnoDB dùng row-level locking và MVCC.

Use case phù hợp:

- hệ thống cũ còn dùng MyISAM;
- bảng `MEMORY` dùng làm lookup/cache nội bộ;
- query đọc ngắn cần được ưu tiên hơn các thao tác khác trên bảng table-level lock.

Không nên dùng khi:

- hệ thống chủ yếu dùng InnoDB;
- workload ghi nhiều và việc ưu tiên đọc có thể làm thao tác ghi bị đợi lâu;
- query đọc lớn, quét nhiều row, chiếm tài nguyên lâu.

Ưu điểm:

- dễ dùng, chỉ cần thêm modifier sau `SELECT`;
- có thể giảm latency cho một số query đọc quan trọng trên table-level locking engine.

Nhược điểm:

- không phải tối ưu hữu ích cho InnoDB hiện đại;
- có thể gây starvation cho write workload nếu lạm dụng;
- không sửa được query/index kém.

Tuning như thế nào:

1. Kiểm tra engine của bảng:

```sql
SHOW TABLE STATUS LIKE 'reports';
```

2. Nếu bảng là InnoDB, thường không nên kỳ vọng `HIGH_PRIORITY` cải thiện rõ.
3. Nếu bảng là MyISAM/MEMORY và có lock wait, thử so sánh latency query có và không có `HIGH_PRIORITY`.
4. Theo dõi tác động lên write latency. Nếu write bị đợi lâu hơn đáng kể, không nên dùng lâu dài.

### 2.2. SQL_CACHE và SQL_NO_CACHE

`SQL_CACHE` và `SQL_NO_CACHE` liên quan đến **Query Cache** của MariaDB.

Cú pháp:

```sql
SELECT SQL_CACHE id, name
FROM product_categories
WHERE active = 1;
```

```sql
SELECT SQL_NO_CACHE *
FROM orders
WHERE created_at >= NOW() - INTERVAL 1 DAY;
```

Ý nghĩa:

- `SQL_CACHE`: yêu cầu cache result set nếu query đủ điều kiện cache;
- `SQL_NO_CACHE`: không dùng Query Cache cho query này;
- `SQL_CACHE` thường có ý nghĩa khi `query_cache_type = DEMAND`;
- `SQL_NO_CACHE` thường dùng khi `query_cache_type = ON` nhưng có query không đáng cache.

Liên hệ với `query_cache_type`:

| `query_cache_type` | Ý nghĩa | Ảnh hưởng của hint |
| --- | --- | --- |
| `OFF` / `0` | Tắt Query Cache | `SQL_CACHE` và `SQL_NO_CACHE` gần như không có lợi ích |
| `ON` / `1` | Cache query đủ điều kiện mặc định | `SQL_NO_CACHE` dùng để loại trừ query |
| `DEMAND` / `2` | Chỉ cache query có yêu cầu | `SQL_CACHE` dùng để chọn lọc query cần cache |

Use case phù hợp với `SQL_CACHE`:

- query đọc lặp lại rất nhiều;
- dữ liệu ít thay đổi;
- result set nhỏ hoặc vừa;
- bảng danh mục, cấu hình, feature flag, metadata;
- dashboard đọc cùng một bộ filter lặp lại.

Ví dụ:

```sql
SELECT SQL_CACHE id, code, display_name
FROM countries
WHERE enabled = 1
ORDER BY display_name;
```

Use case phù hợp với `SQL_NO_CACHE`:

- query ad-hoc, mỗi lần một filter khác;
- query trả result set lớn;
- query trên bảng thay đổi liên tục;
- query benchmark, muốn đo chi phí thật thay vì bị cache che;
- query dùng hàm không ổn định hoặc phụ thuộc session.

Ví dụ:

```sql
SELECT SQL_NO_CACHE *
FROM transaction_logs
WHERE created_at >= '2026-06-01'
  AND created_at < '2026-06-02';
```

Ưu điểm:

- cho phép điều khiển cache theo từng query;
- hữu ích khi `query_cache_type = DEMAND`;
- tránh đưa các query lớn/ít lặp lại vào cache.

Nhược điểm:

- Query Cache có thể gây lock contention trên workload concurrency cao;
- nếu dữ liệu thay đổi liên tục, chi phí invalidate cache có thể lớn hơn lợi ích;
- MySQL 8 đã bỏ Query Cache, nên code phụ thuộc nặng vào `SQL_CACHE` kém portable;
- không phải query nào cũng cache được.

Tuning như thế nào:

1. Kiểm tra Query Cache có đang bật không:

```sql
SHOW VARIABLES LIKE 'query_cache%';
```

2. Xem tỉ lệ hit và mức độ bị prune:

```sql
SHOW GLOBAL STATUS LIKE 'Qcache%';
```

3. Nếu `query_cache_type = DEMAND`, chỉ thêm `SQL_CACHE` cho query nhỏ, lặp lại nhiều, dữ liệu ít đổi.
4. Nếu `query_cache_type = ON`, thêm `SQL_NO_CACHE` cho query lớn, query ad-hoc, query report hoặc query chạy một lần.
5. Theo dõi `Qcache_hits`, `Qcache_inserts`, `Qcache_not_cached`, `Qcache_lowmem_prunes`.
6. Nếu `Qcache_lowmem_prunes` tăng nhanh và hit thấp, Query Cache có thể đang gây tốn chi phí hơn lợi ích.

Lưu ý:

```text
SQL_CACHE không đảm bảo query chắc chắn được cache.
MariaDB vẫn kiểm tra kích thước result, loại statement, table liên quan, hàm trong query và cấu hình Query Cache.
```

### 2.3. SQL_BUFFER_RESULT

`SQL_BUFFER_RESULT` ép MariaDB đưa result vào bảng tạm trước khi trả cho client.

Cú pháp:

```sql
SELECT SQL_BUFFER_RESULT *
FROM large_report_source
WHERE report_date = '2026-06-15';
```

Ý nghĩa:

```text
Không có SQL_BUFFER_RESULT:
MariaDB có thể giữ lock/resource trong lúc client đọc result chậm.

Có SQL_BUFFER_RESULT:
MariaDB tạo result tạm thời, giải phóng table lock/resource sớm hơn,
sau đó trả result cho client từ bảng tạm.
```

Use case phù hợp:

- client đọc result chậm;
- query đọc từ storage engine có table-level lock;
- cần giải phóng lock sớm để query khác tiếp tục;
- ứng dụng batch/reporting trả result vừa phải nhưng client xử lý từng dòng chậm.

Ví dụ tình huống:

```text
Client A chạy SELECT lớn và đọc từng row rất chậm.
Nếu MariaDB giữ lock trong lúc client đọc, Client B có thể bị đợi.
SQL_BUFFER_RESULT giúp MariaDB materialize result sớm và giải phóng lock sớm hơn.
```

Ưu điểm:

- có thể giảm thời gian giữ lock;
- hữu ích khi client/network chậm;
- có thể giúp workload khác ít bị block hơn.

Nhược điểm:

- tốn memory hoặc disk temporary table;
- thêm chi phí tạo bảng tạm;
- nếu result set lớn, có thể làm tăng I/O và làm chậm query;
- không nên dùng mặc định cho mọi query.

Tuning như thế nào:

1. Xác định query có bị chậm do client đọc chậm hay do database xử lý chậm.
2. So sánh thời gian chạy có và không có `SQL_BUFFER_RESULT`.
3. Theo dõi temporary table:

```sql
SHOW GLOBAL STATUS LIKE 'Created_tmp%';
```

4. Nếu `Created_tmp_disk_tables` tăng mạnh, result đang spill ra disk và hint này có thể làm tệ hơn.
5. Dùng cho query có result vừa phải. Tránh dùng cho report trả hàng triệu row.

### 2.4. SQL_SMALL_RESULT và SQL_BIG_RESULT

`SQL_SMALL_RESULT` và `SQL_BIG_RESULT` nói cho optimizer biết kỳ vọng về kích thước result khi query có thao tác như `GROUP BY` hoặc `DISTINCT`.

Cú pháp:

```sql
SELECT SQL_SMALL_RESULT customer_id, COUNT(*) AS total_orders
FROM orders
GROUP BY customer_id;
```

```sql
SELECT SQL_BIG_RESULT customer_id, COUNT(*) AS total_orders
FROM orders
GROUP BY customer_id;
```

Ý nghĩa tổng quát:

- `SQL_SMALL_RESULT`: gợi ý result sau `GROUP BY`/`DISTINCT` nhỏ;
- `SQL_BIG_RESULT`: gợi ý result sau `GROUP BY`/`DISTINCT` lớn;
- optimizer có thể quyết định dùng temporary table, sort hoặc cách xử lý aggregation phù hợp hơn.

Use case phù hợp với `SQL_SMALL_RESULT`:

- group theo cột có cardinality thấp;
- result sau aggregate rất nhỏ so với input;
- ví dụ group theo `status`, `type`, `country_code`.

Ví dụ:

```sql
SELECT SQL_SMALL_RESULT status, COUNT(*) AS total
FROM orders
GROUP BY status;
```

Use case phù hợp với `SQL_BIG_RESULT`:

- group theo cột có cardinality cao;
- result sau aggregate rất lớn;
- query reporting/analytics đọc nhiều row;
- cần tránh một cách dùng temporary table không phù hợp.

Ví dụ:

```sql
SELECT SQL_BIG_RESULT user_id, DATE(created_at) AS order_date, COUNT(*) AS total
FROM orders
GROUP BY user_id, DATE(created_at);
```

Ưu điểm:

- có thể giúp optimizer chọn chiến lược tốt hơn khi estimate sai;
- cú pháp đơn giản;
- hữu ích với query `GROUP BY`/`DISTINCT` đặc thù.

Nhược điểm:

- nếu gợi ý sai, query có thể chậm hơn;
- khi statistics tốt, optimizer có thể đã chọn đúng mà không cần hint;
- cần benchmark trên dữ liệu gần production.

Tuning như thế nào:

1. Ước lượng số group thực tế:

```sql
SELECT COUNT(DISTINCT status)
FROM orders;
```

2. Dùng `SQL_SMALL_RESULT` khi số group nhỏ rõ ràng so với số row input.
3. Dùng `SQL_BIG_RESULT` khi số group lớn, query thiên về analytics/reporting.
4. So sánh `EXPLAIN` trước/sau, chú ý `Using temporary`, `Using filesort`.
5. Đo thời gian thực tế. Với hint nhóm này, `EXPLAIN` chưa đủ, vì chi phí sort/temp table phụ thuộc dữ liệu thật.

Cách quyết định nhanh:

```text
GROUP BY tạo rất ít group -> cân nhắc SQL_SMALL_RESULT.
GROUP BY tạo rất nhiều group -> cân nhắc SQL_BIG_RESULT.
Không chắc -> để optimizer tự quyết định, sau đó đo bằng EXPLAIN/ANALYZE.
```

### 2.5. STRAIGHT_JOIN

`STRAIGHT_JOIN` ép MariaDB đọc bảng theo thứ tự xuất hiện trong câu `SELECT`.

Có hai cách dùng phổ biến.

Cách 1: đặt sau `SELECT`:

```sql
SELECT STRAIGHT_JOIN o.id, c.name
FROM orders o
JOIN customers c ON c.id = o.customer_id
WHERE o.created_at >= '2026-06-01';
```

Cách 2: dùng như join operator:

```sql
SELECT o.id, c.name
FROM orders o
STRAIGHT_JOIN customers c ON c.id = o.customer_id
WHERE o.created_at >= '2026-06-01';
```

Ý nghĩa:

```text
Không có STRAIGHT_JOIN:
optimizer có thể reorder join để tìm plan rẻ hơn.

Có STRAIGHT_JOIN:
optimizer bị ràng buộc đọc bảng theo thứ tự mình viết.
```

Use case phù hợp:

- optimizer chọn sai join order do statistics không chính xác;
- query có predicate rất selective trên bảng đầu tiên nhưng optimizer không nhận ra;
- cần fix tạm thời một query production trong lúc chưa thể sửa statistics/index;
- query reporting ổn định, dữ liệu và plan đã được benchmark kỹ.

Ví dụ:

```sql
SELECT STRAIGHT_JOIN o.id, oi.product_id, oi.quantity
FROM orders o
JOIN order_items oi ON oi.order_id = o.id
WHERE o.created_at >= '2026-06-15'
  AND o.status = 'PAID';
```

Nếu điều kiện trên `orders` rất selective, đọc `orders` trước rồi lookup `order_items` có thể tốt hơn.

Ưu điểm:

- ép được join order khi optimizer chọn sai;
- dễ đọc và dễ thử nghiệm;
- hữu ích cho hotfix query cụ thể.

Nhược điểm:

- làm mất khả năng reorder join của optimizer;
- có thể tốt hôm nay nhưng xấu sau khi data distribution thay đổi;
- dễ che lấp vấn đề gốc như thiếu index, statistics cũ, predicate viết kém;
- với một số bảng `const` hoặc `system`, MariaDB có thể xử lý đặc biệt và không hoàn toàn giống thứ tự mình hình dung.

Tuning như thế nào:

1. Chạy `EXPLAIN` query gốc:

```sql
EXPLAIN
SELECT ...
```

2. Xác định join order hiện tại qua thứ tự table trong `EXPLAIN`.
3. Đặt bảng có filter selective nhất lên trước, rồi thử `STRAIGHT_JOIN`.
4. So sánh `rows`, `key`, `Extra` và thời gian thực tế.
5. Nếu `STRAIGHT_JOIN` nhanh hơn, vẫn cần kiểm tra có thể sửa gốc bằng index/statistics không.
6. Review lại hint này định kỳ vì join order tối ưu rất phụ thuộc phân bố dữ liệu.

### 2.6. SQL_CALC_FOUND_ROWS

`SQL_CALC_FOUND_ROWS` dùng với `LIMIT` để MariaDB tính tổng số row có thể match nếu không có `LIMIT`.

Cú pháp:

```sql
SELECT SQL_CALC_FOUND_ROWS id, title
FROM articles
WHERE category_id = 10
ORDER BY published_at DESC
LIMIT 20 OFFSET 40;

SELECT FOUND_ROWS();
```

Ý nghĩa:

```text
Query đầu trả về 20 row theo LIMIT.
MariaDB vẫn tính tổng số row match nếu bỏ LIMIT.
Query FOUND_ROWS() lấy con số đó.
```

Use case:

- pagination kiểu cũ cần hiển thị tổng số bản ghi;
- ứng dụng legacy đã dùng `FOUND_ROWS()`;
- query không quá lớn và chi phí tính tổng chấp nhận được.

Ưu điểm:

- tiện lợi, lấy data page và tổng row theo một cơ chế;
- không cần viết thêm query `COUNT(*)` riêng trong code legacy.

Nhược điểm:

- có thể làm query chậm vì MariaDB phải tính thêm row ngoài `LIMIT`;
- với pagination lớn, chi phí có thể rất cao;
- khó tối ưu hơn cách viết `COUNT(*)` riêng trong nhiều trường hợp;
- dễ tạo cảm giác `LIMIT 20` sẽ nhẹ, nhưng thực tế database vẫn phải làm thêm việc.

Tuning như thế nào:

1. Đo query có `SQL_CALC_FOUND_ROWS`.
2. So sánh với hai query riêng:

```sql
SELECT id, title
FROM articles
WHERE category_id = 10
ORDER BY published_at DESC
LIMIT 20 OFFSET 40;
```

```sql
SELECT COUNT(*)
FROM articles
WHERE category_id = 10;
```

3. Nếu `COUNT(*)` riêng nhanh hơn hoặc dễ tối ưu bằng index hơn, nên bỏ `SQL_CALC_FOUND_ROWS`.
4. Với hệ thống lớn, cân nhắc keyset pagination:

```sql
SELECT id, title, published_at
FROM articles
WHERE category_id = 10
  AND published_at < ?
ORDER BY published_at DESC
LIMIT 20;
```

5. Nếu UI không bắt buộc tổng số chính xác, chỉ trả `has_next_page` thường rẻ hơn nhiều.

### 2.7. USE INDEX, FORCE INDEX, IGNORE INDEX

Nhóm này là **index hint**, nhưng thường được nhắc cùng với optimizer hints vì cũng tác động tới query plan.

Cú pháp:

```sql
SELECT *
FROM orders USE INDEX (idx_orders_customer_created)
WHERE customer_id = 10
ORDER BY created_at DESC;
```

```sql
SELECT *
FROM orders FORCE INDEX (idx_orders_status)
WHERE status = 'PAID';
```

```sql
SELECT *
FROM orders IGNORE INDEX (idx_orders_status)
WHERE status = 'PAID';
```

Ý nghĩa:

| Hint | Tác dụng |
| --- | --- |
| `USE INDEX` | Giới hạn danh sách index optimizer nên xem xét |
| `FORCE INDEX` | Giống `USE INDEX`, nhưng làm full table scan có vẻ đắt hơn, để optimizer nghiêng về index được chỉ định |
| `IGNORE INDEX` | Loại một hoặc nhiều index khỏi danh sách optimizer xem xét |

Có thể giới hạn theo ngữ cảnh:

```sql
SELECT *
FROM orders USE INDEX FOR WHERE (idx_orders_customer_id)
WHERE customer_id = 10
ORDER BY created_at DESC;
```

```sql
SELECT *
FROM orders USE INDEX FOR ORDER BY (idx_orders_customer_created)
WHERE customer_id = 10
ORDER BY created_at DESC;
```

```sql
SELECT customer_id, COUNT(*)
FROM orders USE INDEX FOR GROUP BY (idx_orders_customer_id)
GROUP BY customer_id;
```

Use case phù hợp:

- optimizer chọn index kém do statistics sai;
- có nhiều index gần giống nhau và optimizer chọn index không phù hợp;
- cần tránh một index cũ/kém trong lúc chưa thể drop;
- query production nóng cần fix nhanh nhưng chưa thể thay schema.

Ưu điểm:

- tác động trực tiếp tới plan;
- hữu ích để test giả thuyết về index;
- có thể fix nhanh một query cụ thể.

Nhược điểm:

- gắn chặt query với tên index;
- đổi tên/drop index có thể làm query lỗi;
- có thể kém tối ưu khi data distribution thay đổi;
- dễ bị lạm dụng thay vì sửa index/statistics/query.

Tuning như thế nào:

1. Chạy `EXPLAIN` query gốc, xem `possible_keys`, `key`, `rows`, `Extra`.
2. Nếu `possible_keys` có index tốt hơn nhưng `key` lại chọn index khác, thử `USE INDEX` hoặc `FORCE INDEX`.
3. Nếu một index làm optimizer chọn sai lặp lại, thử `IGNORE INDEX`.
4. So sánh:

```sql
EXPLAIN
SELECT ...
```

```sql
EXPLAIN
SELECT ...
FROM orders FORCE INDEX (idx_orders_customer_created)
...
```

5. Đo thời gian thực tế với tham số đại diện cho production.
6. Nếu hint tốt hơn, kiểm tra có thể tạo composite index đúng hơn không. Hint nên là giải pháp có kiểm soát, không phải cách che index sai mãi mãi.

Nguyên tắc:

```text
USE INDEX khi muốn giới hạn lựa chọn.
FORCE INDEX khi optimizer quá dễ chọn full scan nhưng mình đã chứng minh index tốt hơn.
IGNORE INDEX khi một index làm optimizer bị "hấp dẫn" sai.
```

### 2.8. Bảng tóm tắt SELECT modifier hints

| Hint | Mục đích chính | Phù hợp khi | Cần cẩn thận với |
| --- | --- | --- | --- |
| `HIGH_PRIORITY` | Ưu tiên `SELECT` | Table-level locking engine | Ít ý nghĩa với InnoDB, có thể ảnh hưởng write |
| `SQL_CACHE` | Yêu cầu cache result | Query lặp lại, result nhỏ, dữ liệu ít đổi | Query Cache contention, không portable sang MySQL 8 |
| `SQL_NO_CACHE` | Không dùng Query Cache | Query ad-hoc, result lớn, benchmark | Không giúp nếu Query Cache đã tắt |
| `SQL_BUFFER_RESULT` | Materialize result để giải phóng lock sớm | Client đọc chậm, table-level lock | Tốn temporary table, memory/disk |
| `SQL_SMALL_RESULT` | Gợi ý result sau `GROUP BY`/`DISTINCT` nhỏ | Ít group | Gợi ý sai làm query chậm |
| `SQL_BIG_RESULT` | Gợi ý result sau `GROUP BY`/`DISTINCT` lớn | Nhiều group, reporting | Thêm sort/temp cost nếu không cần |
| `STRAIGHT_JOIN` | Ép join order | Optimizer chọn sai thứ tự join | Mất khả năng reorder, phụ thuộc dữ liệu hiện tại |
| `SQL_CALC_FOUND_ROWS` | Tính tổng row match bỏ qua `LIMIT` | Legacy pagination nhỏ | Có thể rất chậm, nên cân nhắc `COUNT(*)` riêng |
| `USE/FORCE/IGNORE INDEX` | Điều khiển index optimizer xem xét | Optimizer chọn sai index | Phụ thuộc tên index, có thể lỗi thời |

## 3. Khi nào nên dùng optimizer hint?

Nên dùng khi có ít nhất một trong các dấu hiệu:

- `EXPLAIN` cho thấy optimizer chọn full scan trong khi index tốt rõ ràng tốt hơn;
- optimizer chọn sai join order và query chậm nặng;
- statistics không phản ánh đúng phân bố dữ liệu;
- query critical cần fix nhanh trong lúc đợi schema/index migration;
- workload đặc thù, optimizer không ước lượng đúng cardinality;
- cần điều khiển Query Cache theo từng query.

Không nên dùng khi:

- chưa có `EXPLAIN`;
- chưa benchmark trên dữ liệu gần production;
- query chậm do thiếu index rõ ràng;
- query viết không sargable, ví dụ bọc cột indexed trong function;
- hint chỉ làm đẹp một test case nhỏ nhưng chưa rõ tác động production;
- optimizer đã chọn đúng plan.

Ví dụ query không sargable:

```sql
SELECT *
FROM orders
WHERE DATE(created_at) = '2026-06-15';
```

Nên rewrite trước khi nghĩ đến hint:

```sql
SELECT *
FROM orders
WHERE created_at >= '2026-06-15'
  AND created_at < '2026-06-16';
```

## 4. Quy trình tuning bằng optimizer hint

### 4.1. Bước 1: Xác định query chậm

Lấy query từ:

- slow query log;
- APM;
- performance dashboard;
- log ứng dụng;
- query mẫu của user/report.

Ghi lại:

- SQL đầy đủ;
- tham số thực tế;
- thời gian chạy;
- số row trả về;
- tần suất chạy;
- version MariaDB;
- schema/index liên quan.

### 4.2. Bước 2: Đọc plan hiện tại

```sql
EXPLAIN
SELECT ...
```

Cần nhìn:

| Cột | Ý nghĩa |
| --- | --- |
| `type` | Cách access table: `const`, `ref`, `range`, `index`, `ALL`, ... |
| `possible_keys` | Index optimizer có thể dùng |
| `key` | Index thực sự được dùng |
| `rows` | Số row ước lượng phải đọc |
| `Extra` | Thông tin phụ: `Using where`, `Using temporary`, `Using filesort`, ... |

Nếu MariaDB version hỗ trợ:

```sql
ANALYZE FORMAT=JSON
SELECT ...
```

Dùng để so sánh ước lượng với row thực tế.

### 4.3. Bước 3: Thử phương án không hint trước

Trước khi thêm hint, cần kiểm tra:

- có thiếu index không;
- index hiện có có đúng thứ tự cột không;
- query có viết lại để sargable hơn không;
- có `SELECT *` không cần thiết không;
- statistics có cũ không;
- bảng có data skew không;
- có cần tách query phức tạp thành query nhỏ hơn không.

Cập nhật statistics nếu cần:

```sql
ANALYZE TABLE orders;
```

### 4.4. Bước 4: Thử hint có mục tiêu

Chỉ thêm hint cho query cụ thể.

Ví dụ thử `FORCE INDEX`:

```sql
EXPLAIN
SELECT *
FROM orders FORCE INDEX (idx_orders_customer_created)
WHERE customer_id = 10
ORDER BY created_at DESC
LIMIT 50;
```

So sánh với plan gốc:

```text
plan gốc:
- key = idx_orders_status
- rows = 500000
- Extra = Using where; Using filesort

plan có hint:
- key = idx_orders_customer_created
- rows = 120
- Extra = Using where
```

Nếu thời gian thực tế tốt hơn ổn định, hint có thể chấp nhận.

### 4.5. Bước 5: Đo bằng tham số đại diện

Không chỉ test một giá trị.

Ví dụ với query theo `customer_id`, nên test:

- customer có ít order;
- customer có số order trung bình;
- customer có rất nhiều order;
- khoảng thời gian ngắn;
- khoảng thời gian dài.

Lý do:

```text
Một hint có thể tốt với customer nhỏ nhưng tệ với customer lớn.
Một index có thể tốt với filter selective nhưng tệ khi filter match phần lớn bảng.
```

### 4.6. Bước 6: Ghi chú lý do

Nếu đưa hint vào code, nên ghi chú ngắn gọn:

```sql
SELECT /* force customer/date index: optimizer picks status index on skewed data */
       *
FROM orders FORCE INDEX (idx_orders_customer_created)
WHERE customer_id = ?
ORDER BY created_at DESC
LIMIT ?;
```

Comment nên nói:

- tại sao dùng hint;
- vấn đề optimizer gặp phải;
- khi nào cần review lại.

## 5. Ưu điểm của optimizer hint

Optimizer hint hữu ích vì:

- cho phép can thiệp nhanh vào query plan;
- giảm rủi ro thay đổi schema nếu chỉ một query có vấn đề;
- hữu ích khi optimizer estimate sai;
- có thể dùng để thực nghiệm và so sánh plan;
- giúp ổn định latency cho query critical;
- có thể là giải pháp tạm thời trước khi tối ưu lâu dài.

Ví dụ:

```text
Một query production bắt đầu chậm sau khi data skew.
Thêm IGNORE INDEX để tránh index sai có thể là hotfix nhanh.
Sau đó vẫn cần review index/statistics để sửa gốc.
```

## 6. Nhược điểm và rủi ro

Rủi ro lớn nhất của hint là **đóng băng quyết định của optimizer** trong khi dữ liệu luôn thay đổi.

Những rủi ro thường gặp:

- plan tốt với dữ liệu hiện tại nhưng xấu sau 3 tháng;
- query phụ thuộc tên index;
- upgrade MariaDB có optimizer mới nhưng hint cũ vẫn ép plan cũ;
- developer khác không biết tại sao hint tồn tại;
- hint che lấp vấn đề thiết kế schema/query;
- dùng quá nhiều hint làm SQL khó bảo trì.

Ví dụ:

```sql
SELECT *
FROM orders FORCE INDEX (idx_orders_status)
WHERE status = 'PAID';
```

Nếu ban đầu `PAID` chỉ chiếm 1% dữ liệu, index này tốt. Sau này `PAID` chiếm 90% dữ liệu, full scan hoặc index khác có thể tốt hơn, nhưng `FORCE INDEX` vẫn ép optimizer nghiêng về index cũ.

## 7. Use case tổng hợp

### 7.1. Dashboard đọc danh mục ít đổi

Dùng `SQL_CACHE` nếu Query Cache đang ở `DEMAND` và workload phù hợp:

```sql
SELECT SQL_CACHE id, name, display_order
FROM product_categories
WHERE enabled = 1
ORDER BY display_order;
```

Tuning:

- kiểm tra query có lặp lại nhiều không;
- kiểm tra bảng có bị update thường xuyên không;
- theo dõi `Qcache_hits` có tăng không;
- nếu hit thấp, bỏ `SQL_CACHE`.

### 7.2. Query report lớn không nên vào Query Cache

```sql
SELECT SQL_NO_CACHE *
FROM transaction_logs
WHERE created_at >= '2026-06-01'
  AND created_at < '2026-07-01';
```

Tuning:

- dùng `SQL_NO_CACHE` để tránh đẩy result lớn vào cache;
- kiểm tra index theo `created_at`;
- nếu report chạy thường xuyên, cân nhắc summary table hoặc partition theo thời gian.

### 7.3. Optimizer chọn sai index

```sql
SELECT *
FROM orders FORCE INDEX (idx_orders_customer_created)
WHERE customer_id = 10
ORDER BY created_at DESC
LIMIT 20;
```

Tuning:

- so sánh `rows` và `Extra` trước/sau `FORCE INDEX`;
- test với nhiều `customer_id`;
- nếu luôn cần index này, cân nhắc tạo hoặc sửa composite index đúng hơn;
- nếu chỉ một vài giá trị bị lệch, kiểm tra data skew và statistics.

### 7.4. Join order bị chọn sai

```sql
SELECT STRAIGHT_JOIN o.id, oi.product_id, oi.quantity
FROM orders o
JOIN order_items oi ON oi.order_id = o.id
WHERE o.created_at >= '2026-06-15'
  AND o.status = 'PAID';
```

Tuning:

- đặt bảng có filter selective nhất lên trước;
- đảm bảo bảng phía sau có index cho join key;
- so sánh join order bằng `EXPLAIN`;
- nếu optimizer sai vì statistics cũ, chạy `ANALYZE TABLE` trước khi giữ hint lâu dài.

### 7.5. Pagination cần tổng số row

Legacy:

```sql
SELECT SQL_CALC_FOUND_ROWS id, title
FROM articles
WHERE category_id = 10
ORDER BY published_at DESC
LIMIT 20 OFFSET 40;

SELECT FOUND_ROWS();
```

Tuning:

- đo chi phí của `SQL_CALC_FOUND_ROWS`;
- thử tách thành query page và query `COUNT(*)`;
- thêm index phù hợp cho cả filter và order;
- cân nhắc keyset pagination nếu `OFFSET` lớn;
- nếu UI không cần tổng số chính xác, trả `has_next_page` thay vì count.

## 8. Checklist trước khi đưa hint vào production

- Đã có query mẫu với tham số thực tế.
- Đã chạy `EXPLAIN` cho query gốc.
- Đã chạy `EXPLAIN` cho query có hint.
- Đã benchmark thời gian thực thi trên dữ liệu gần production.
- Đã kiểm tra index và statistics.
- Đã cân nhắc rewrite query trước khi hint.
- Đã test nhiều bộ tham số đại diện.
- Đã xác định hint chỉ áp dụng cho query cụ thể.
- Đã ghi chú lý do dùng hint.
- Đã có kế hoạch review lại sau khi data/version thay đổi.

## 9. Kết luận thực dụng

Optimizer hint trong MariaDB là công cụ can thiệp vào cách database lập plan hoặc xử lý query. Nó hữu ích khi optimizer thiếu thông tin hoặc estimate sai, nhưng không nên xem là giải pháp mặc định cho mọi query chậm.

Cách dùng đúng:

```text
1. Đo query chậm.
2. Đọc EXPLAIN.
3. Sửa query/index/statistics nếu có thể.
4. Chỉ dùng hint khi có bằng chứng.
5. Test nhiều tham số đại diện.
6. Ghi chú lý do và review định kỳ.
```

Tóm gọn:

```text
Hint tốt = có bằng chứng, phạm vi hẹp, có lý do rõ ràng.
Hint xấu = thêm theo cảm tính, không EXPLAIN, không benchmark, không ai biết tại sao tồn tại.
```

## 10. Tài liệu tham khảo

- MariaDB Docs: SELECT Modifier Hints
- MariaDB Docs: Index Hints / How to force query plans
- MariaDB Docs: Query Cache
- MariaDB Docs: EXPLAIN
