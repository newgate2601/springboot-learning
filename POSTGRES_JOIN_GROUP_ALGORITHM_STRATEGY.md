# PostgreSQL Join and Group Algorithm Strategy

Tài liệu này giải thích PostgreSQL chọn thuật toán join/group như thế nào, khi nào từng loại phù hợp, và I/O pattern thường gặp của từng loại.

Mục tiêu:

- Hiểu vì sao planner chọn `Nested Loop`, `Hash Join`, `Merge Join`.
- Hiểu vì sao planner chọn `HashAggregate`, `GroupAggregate`, `Sort + GroupAggregate`, parallel aggregate.
- Biết đọc I/O pattern để nhận diện query đang chậm vì random I/O, sequential scan, sort spill hay hash spill.

## 1. Planner Đang Chọn Cái Gì?

SQL chỉ mô tả kết quả cần lấy:

```sql
select *
from orders o
join customers c on c.id = o.customer_id
where o.status = 'PAID';
```

SQL không nói phải join bằng cách nào.

PostgreSQL planner sẽ chọn physical algorithm, ví dụ:

- `Nested Loop`
- `Hash Join`
- `Merge Join`

Tương tự với group:

```sql
select customer_id, count(*)
from orders
group by customer_id;
```

PostgreSQL có thể chọn:

- `HashAggregate`
- `GroupAggregate`
- `Sort + GroupAggregate`
- `Partial Aggregate` + `Finalize Aggregate`

Planner chọn dựa trên cost estimate:

- Số row ước lượng sau filter.
- Số row ước lượng sau join.
- Số distinct group key.
- Có index hay không.
- Input có sorted sẵn hay không.
- `work_mem`.
- Chi phí random I/O và sequential I/O.
- Statistics của bảng/cột.

## 2. Nested Loop Join

### 2.1. Cách hoạt động

Plan mẫu:

```text
Nested Loop
  -> Index Scan using idx_orders_recent on orders o
  -> Index Scan using customers_pkey on customers c
       Index Cond: (id = o.customer_id)
```

Cách chạy:

```text
for each row from orders:
    lookup customer by customer_id
```

Outer child chạy trước. Inner child chạy sau và lặp lại theo từng row outer.

### 2.2. Khi nào planner thích Nested Loop?

Nested Loop thường được chọn khi:

- Outer side nhỏ.
- Inner side có index lookup tốt.
- Query có `LIMIT` và có thể dừng sớm.
- Join condition là key lookup.
- Planner ước lượng số vòng lặp thấp.

Ví dụ:

```sql
select *
from orders o
join customers c on c.id = o.customer_id
where o.id = 100;
```

Số liệu:

```text
orders filter by id = 100 -> 1 row
customers lookup by primary key -> 1 row
```

Plan hợp lý:

```text
Nested Loop
  -> Index Scan orders_pkey       rows=1
  -> Index Scan customers_pkey    rows=1 loops=1
```

Tổng chi phí gần như:

```text
1 index lookup order
+ 1 index lookup customer
```

### 2.3. Ví dụ Nested Loop tốt

Query:

```sql
select *
from orders o
join order_items oi on oi.order_id = o.id
where o.created_at >= now() - interval '1 hour';
```

Số liệu:

```text
orders total: 10,000,000
orders trong 1 giờ gần nhất: 50
order_items mỗi order: 5
index orders(created_at): có
index order_items(order_id): có
```

Plan hợp lý:

```text
Nested Loop
  -> Index Scan using idx_orders_created_at on orders o
  -> Index Scan using idx_order_items_order_id on order_items oi
       Index Cond: (order_id = o.id)
```

Cách chạy:

```text
lấy 50 orders gần nhất
với mỗi order, lookup order_items theo order_id
```

Tổng inner lookup khoảng:

```text
50 index lookups
```

Rất rẻ.

### 2.4. Ví dụ Nested Loop xấu

Query:

```sql
select *
from orders o
join order_items oi on oi.order_id = o.id;
```

Số liệu:

```text
orders: 1,000,000 rows
order_items: 10,000,000 rows
không có index order_items(order_id)
```

Plan xấu:

```text
Nested Loop
  -> Seq Scan on orders o
  -> Seq Scan on order_items oi
       Filter: (order_id = o.id)
```

Cách chạy:

```text
for each of 1,000,000 orders:
    scan 10,000,000 order_items
```

Đây là pattern cực xấu.

Dấu hiệu trong plan:

```text
Seq Scan on order_items ... loops=1000000
Rows Removed by Filter: very large
```

### 2.5. I/O pattern của Nested Loop

Nested Loop thường tạo I/O pattern:

```text
outer scan + many repeated inner lookups
```

Nếu inner side dùng index:

- I/O thường là random access.
- Nếu data/index đã cache, rất nhanh.
- Nếu outer rows nhiều và cache không đủ, random I/O có thể rất đắt.

Nếu inner side là seq scan:

- I/O cực tệ khi `loops` cao.
- Cùng một bảng có thể bị scan lặp lại nhiều lần.

Nested Loop tốt cho:

```text
ít outer rows + index lookup nhỏ
```

Nested Loop xấu cho:

```text
nhiều outer rows + inner scan lớn
```

## 3. Hash Join

### 3.1. Cách hoạt động

Plan mẫu:

```text
Hash Join
  Hash Cond: (o.customer_id = c.id)
  -> Seq Scan on orders o
  -> Hash
       -> Seq Scan on customers c
```

Thứ tự thực tế:

1. Scan `customers`.
2. Build hash table từ `customers.id`.
3. Scan `orders`.
4. Probe từng `o.customer_id` vào hash table.

Cách chạy:

```text
build hash(customers)
for each order:
    find matching customer in hash
```

### 3.2. Khi nào planner thích Hash Join?

Hash Join thường được chọn khi:

- Join nhiều rows.
- Không cần output sorted.
- Nested Loop sẽ phải lookup quá nhiều lần.
- Hash side ước lượng vừa memory hoặc vẫn rẻ hơn cách khác.
- Join condition là equality.

Ví dụ:

```sql
select *
from orders o
join customers c on c.id = o.customer_id;
```

Số liệu:

```text
orders: 10,000,000 rows
customers: 100,000 rows
không filter nhiều
```

Nested Loop có thể cần:

```text
10,000,000 customer lookups
```

Hash Join làm:

```text
scan customers 100,000 rows -> build hash
scan orders 10,000,000 rows -> probe hash
```

### 3.3. Ví dụ Hash Join tốt

Query:

```sql
select *
from order_items oi
join products p on p.id = oi.product_id
where oi.created_at >= date '2026-01-01';
```

Số liệu:

```text
order_items sau filter: 5,000,000 rows
products: 200,000 rows
products row width nhỏ
work_mem đủ build hash products
```

Plan hợp lý:

```text
Hash Join
  Hash Cond: (oi.product_id = p.id)
  -> Seq Scan on order_items oi
  -> Hash
       -> Seq Scan on products p
```

Cách chạy:

```text
build hash 200,000 products
scan 5,000,000 order_items
probe product_id vào hash
```

### 3.4. Ví dụ Hash Join xấu

Số liệu:

```text
build side: 50,000,000 rows
row width lớn
work_mem: 4MB
```

Plan có thể có:

```text
Hash
  Buckets: 262144
  Batches: 128
  Memory Usage: 4096kB
  Buffers: temp written=...
```

`Batches > 1` nghĩa là hash không vừa memory, PostgreSQL phải chia batch và dùng temp file.

### 3.5. I/O pattern của Hash Join

Hash Join thường có I/O pattern:

```text
sequential scan build side
build hash in memory
sequential scan probe side
random access trong memory hash table
```

Nếu hash vừa memory:

- I/O chủ yếu là sequential scan.
- Thường ổn với bảng lớn.

Nếu hash không vừa memory:

- PostgreSQL ghi temp file.
- Sau đó đọc temp file theo batch.
- Plan có `temp read/written`.

Dấu hiệu cần chú ý:

```text
Batches: 64
temp read=...
temp written=...
```

Khi hash spill, cần cân nhắc:

- Tăng `work_mem` cho query.
- Giảm row width của build side.
- Filter build side sớm hơn.
- Thêm index để planner chọn nested loop nếu outer nhỏ.
- Pre-aggregate hoặc dùng summary table.

## 4. Hash Semi Join

### 4.1. Cách hoạt động

Hash Semi Join thường đến từ `EXISTS`.

Query:

```sql
select *
from products p
where exists (
    select 1
    from product_skus s
    where s.product_id = p.id
);
```

Plan:

```text
Hash Semi Join
  Hash Cond: (p.id = s.product_id)
  -> Seq Scan on products p
  -> Hash
       -> Seq Scan on product_skus s
```

Thứ tự thực tế:

1. Scan `product_skus`.
2. Build hash từ `s.product_id`.
3. Scan `products`.
4. Product nào có `p.id` trong hash thì pass.

### 4.2. Khi nào tốt?

Hash Semi Join tốt khi:

- Cần kiểm tra tồn tại trên tập lớn.
- Hash side vừa memory.
- Không cần lấy dữ liệu từ bảng bên phải.

Ví dụ:

```text
products: 3,000,000
product_skus active: 8,000,000
cần tìm products có ít nhất 1 SKU
```

Nếu không có index tốt theo `product_skus(product_id)`, planner có thể chọn hash semi join.

### 4.3. I/O pattern

Hash Semi Join giống Hash Join:

```text
scan bảng exists side
build hash key
scan bảng outer
probe hash
```

Ưu điểm:

- Không nhân bản row bên trái.
- Probe hash nhanh nếu vừa memory.

Nhược điểm:

- Nếu exists side rất lớn, hash có thể spill.
- Nếu sau đó còn phải sort nhiều row, tổng chi phí vẫn cao.

## 5. Merge Join

### 5.1. Cách hoạt động

Plan mẫu:

```text
Merge Join
  Merge Cond: (o.customer_id = c.id)
  -> Index Scan using idx_orders_customer_id on orders o
  -> Index Scan using customers_pkey on customers c
```

Cách chạy:

```text
đọc orders theo customer_id
đọc customers theo id
merge hai stream đã sorted
```

Merge Join cần hai input sorted theo join key.

### 5.2. Khi nào planner thích Merge Join?

Merge Join thường được chọn khi:

- Hai input đã sorted sẵn từ index.
- Query cần output sorted theo join key.
- Join nhiều rows.
- Hash Join không phù hợp hoặc cost cao.

Ví dụ:

```sql
select *
from orders o
join customers c on c.id = o.customer_id
order by o.customer_id;
```

Số liệu:

```text
orders có index(customer_id)
customers có primary key(id)
orders: 10,000,000 rows
customers: 100,000 rows
```

Plan có thể:

```text
Merge Join
  Merge Cond: (o.customer_id = c.id)
  -> Index Scan using idx_orders_customer_id on orders o
  -> Index Scan using customers_pkey on customers c
```

Không cần sort thêm.

### 5.3. Ví dụ Merge Join xấu

Nếu không có index sorted:

```text
Merge Join
  Merge Cond: (o.customer_id = c.id)
  -> Sort
       Sort Key: o.customer_id
       -> Seq Scan on orders o
  -> Sort
       Sort Key: c.id
       -> Seq Scan on customers c
```

Nếu `orders` lớn và sort spill:

```text
Sort Method: external merge
Disk: 1000000kB
```

Merge Join có thể trở nên đắt.

### 5.4. I/O pattern của Merge Join

Nếu input đã sorted bằng index:

- I/O là index scan tuần tự theo key.
- Có thể kéo theo heap access nếu không index-only.
- Không cần temp sort.

Nếu phải sort:

- Scan input.
- Ghi/đọc temp file nếu sort không vừa memory.
- Sau đó merge sequentially.

Dấu hiệu cần chú ý:

```text
Sort Method: external merge
temp read=...
temp written=...
```

## 6. So Sánh Nhanh Join Algorithms

| Algorithm | Tốt khi | Xấu khi | I/O pattern |
| --- | --- | --- | --- |
| Nested Loop | Outer nhỏ, inner có index, có LIMIT | Outer lớn, inner không có index | Nhiều index lookup/random I/O |
| Hash Join | Hai bên lớn, không cần order | Hash side quá lớn, spill disk | Seq scan + memory hash, có thể temp I/O |
| Hash Semi Join | EXISTS trên tập lớn | Hash key quá nhiều, spill | Seq scan exists side + hash + probe |
| Merge Join | Input sorted sẵn hoặc cần sorted output | Phải sort input lớn | Index/sorted stream hoặc temp sort |

## 7. HashAggregate

### 7.1. Cách hoạt động

Query:

```sql
select customer_id, count(*)
from orders
group by customer_id;
```

Plan:

```text
HashAggregate
  Group Key: customer_id
  -> Seq Scan on orders
```

Cách chạy:

```text
scan orders
hash by customer_id
update aggregate state trong hash table
```

### 7.2. Khi nào planner thích HashAggregate?

HashAggregate thường được chọn khi:

- Input chưa sorted theo group key.
- Số group ước lượng vừa memory.
- Tránh được sort lớn.

Ví dụ:

```text
orders: 10,000,000 rows
distinct customer_id: 100,000
aggregate state mỗi group khoảng 64 bytes
work_mem đủ
```

Planner có thể chọn:

```text
HashAggregate
  -> Seq Scan orders
```

### 7.3. Ví dụ HashAggregate tốt

Query:

```sql
select status, count(*)
from orders
group by status;
```

Số liệu:

```text
orders: 10,000,000 rows
status distinct: 5
```

Hash table chỉ có khoảng 5 group.

Plan hợp lý:

```text
HashAggregate
  Group Key: status
  -> Seq Scan on orders
```

I/O chủ yếu là scan bảng `orders` một lần.

### 7.4. Ví dụ HashAggregate xấu

Query:

```sql
select user_id, product_id, count(*)
from events
group by user_id, product_id;
```

Số liệu:

```text
events: 100,000,000 rows
distinct (user_id, product_id): 50,000,000
work_mem thấp
```

Plan có thể:

```text
HashAggregate
  Group Key: user_id, product_id
  Batches: 256
  Disk Usage: 8000000kB
  -> Seq Scan on events
```

Hash table không vừa memory, phải spill.

### 7.5. I/O pattern của HashAggregate

Nếu hash vừa memory:

```text
scan input một lần
update hash table trong memory
output groups
```

I/O thường tốt.

Nếu hash spill:

```text
scan input
ghi hash batches ra temp
đọc lại temp để aggregate tiếp
```

Dấu hiệu:

```text
Batches: many
Disk Usage: ...
temp read/written
```

Cách tối ưu:

- Tăng `work_mem` cho query.
- Giảm số group.
- Filter sớm.
- Pre-aggregate.
- Dùng materialized view/summary table.
- Tạo index để dùng `GroupAggregate` nếu phù hợp.

## 8. GroupAggregate

### 8.1. Cách hoạt động

`GroupAggregate` cần input sorted theo group key.

Query:

```sql
select customer_id, count(*)
from orders
group by customer_id;
```

Plan tốt:

```text
GroupAggregate
  Group Key: customer_id
  -> Index Only Scan using idx_orders_customer_id on orders
```

Cách chạy:

```text
đọc rows theo customer_id
tích lũy count cho customer hiện tại
khi customer_id đổi, output group trước
```

### 8.2. Khi nào planner thích GroupAggregate?

GroupAggregate thường được chọn khi:

- Input đã sorted theo group key từ index.
- Query có `ORDER BY` trùng hoặc gần trùng group key.
- HashAggregate ước lượng không vừa memory.
- Sort cost được xem là chấp nhận được.

### 8.3. Ví dụ GroupAggregate tốt

Số liệu:

```text
orders: 10,000,000 rows
customer_id distinct: 1,000,000
index orders(customer_id): có
```

Query:

```sql
select customer_id, count(*)
from orders
group by customer_id;
```

Plan:

```text
GroupAggregate
  Group Key: customer_id
  -> Index Only Scan using idx_orders_customer_id on orders
```

Không cần hash lớn. Không cần sort.

### 8.4. Ví dụ GroupAggregate với sort

Nếu không có index:

```text
GroupAggregate
  Group Key: customer_id
  -> Sort
       Sort Key: customer_id
       -> Seq Scan on orders
```

Nếu sort lớn:

```text
Sort Method: external merge
Disk: 2000000kB
```

Query có thể chậm do temp I/O.

### 8.5. I/O pattern của GroupAggregate

Nếu input sorted từ index:

```text
index scan theo group key
aggregate streaming
ít memory
```

Nếu phải sort:

```text
scan input
sort input
có thể ghi temp
aggregate streaming sau sort
```

GroupAggregate thường dùng memory ít hơn HashAggregate, nhưng có thể phải trả giá bằng sort.

## 9. Sort + GroupAggregate vs HashAggregate

Query:

```sql
select customer_id, sum(total_amount)
from orders
group by customer_id;
```

### Case A: ít group, không có index

Số liệu:

```text
orders: 10,000,000
distinct customer_id: 100,000
work_mem đủ
```

Planner thường chọn:

```text
HashAggregate
  -> Seq Scan orders
```

Vì hash vừa memory và tránh sort.

### Case B: input sorted theo group key

Số liệu:

```text
orders: 10,000,000
index orders(customer_id)
```

Planner có thể chọn:

```text
GroupAggregate
  -> Index Scan orders_customer_id
```

Vì không cần sort và không cần hash lớn.

### Case C: nhiều group, memory thấp

Số liệu:

```text
orders: 100,000,000
distinct customer_id: 50,000,000
work_mem thấp
```

Planner có thể chọn một trong hai plan xấu:

```text
HashAggregate
  Batches: many
  Disk Usage: large
```

hoặc:

```text
GroupAggregate
  -> Sort
       Sort Method: external merge
```

Lúc này cần nghĩ tới thiết kế dữ liệu:

- Summary table.
- Materialized view.
- Partitioning.
- Index theo group key.
- Aggregate incremental.

## 10. Aggregate Với `DISTINCT`

Query:

```sql
select customer_id, count(distinct product_id)
from order_items
group by customer_id;
```

PostgreSQL phải xử lý:

- group theo `customer_id`
- distinct theo `product_id` trong từng group

Index hữu ích:

```sql
create index on order_items(customer_id, product_id);
```

Vì rows được đọc theo:

```text
customer_id -> product_id
```

Plan có thể dùng sorted input để giảm sort/hash phụ.

I/O pattern nếu không có index:

```text
scan order_items
sort/hash lượng lớn dữ liệu
có thể temp spill
```

## 11. Parallel Aggregate

Query:

```sql
select customer_id, count(*)
from orders
group by customer_id;
```

Plan:

```text
Finalize HashAggregate
  -> Gather
       -> Partial HashAggregate
            -> Parallel Seq Scan on orders
```

Cách chạy:

```text
worker 1 scan một phần orders và aggregate local
worker 2 scan một phần orders và aggregate local
worker 3 scan một phần orders và aggregate local
Gather gom partial result
Finalize aggregate kết quả cuối
```

I/O pattern:

- Parallel sequential scan đọc nhiều page nhanh hơn.
- Mỗi worker có hash table riêng.
- Finalize phase merge partial groups.
- Nếu mỗi worker spill hash, temp I/O có thể tăng mạnh.

Parallel aggregate tốt khi:

- Bảng lớn.
- Aggregate có thể parallelize.
- I/O subsystem chịu được parallel read.
- Work_mem đủ cho mỗi worker.

Lưu ý: `work_mem` tính theo node/worker, không phải toàn query đơn giản. Tăng quá cao có thể gây áp lực RAM nếu nhiều query chạy đồng thời.

## 12. Planner Chọn Dựa Trên Statistics

Planner không biết kết quả thật trước khi chạy. Nó ước lượng dựa trên statistics:

- `pg_class.reltuples`
- `pg_stats.n_distinct`
- `most_common_vals`
- `histogram_bounds`
- `correlation`
- `null_frac`

Nếu statistics sai, planner có thể chọn sai algorithm.

Ví dụ:

```text
Planner nghĩ status = 'PAID' còn 100 rows
Thực tế status = 'PAID' còn 8,000,000 rows
```

Planner có thể chọn Nested Loop vì tưởng outer nhỏ, nhưng thực tế outer rất lớn.

Fix cơ bản:

```sql
analyze orders;
```

Tăng statistics cho cột quan trọng:

```sql
alter table orders alter column status set statistics 1000;
analyze orders;
```

Với cột có tương quan, dùng extended statistics:

```sql
create statistics orders_status_created_stats
on status, created_at
from orders;

analyze orders;
```

## 13. Cách Đoán Planner Sẽ Chọn Gì

Khi nhìn một query, tự hỏi:

1. Sau `where`, bảng trái còn bao nhiêu row?
2. Sau `where`, bảng phải còn bao nhiêu row?
3. Join có index ở inner side không?
4. Outer side có nhỏ không?
5. Query có `LIMIT` giúp dừng sớm không?
6. Output có cần sorted không?
7. Input có index sẵn theo order/group key không?
8. Group có bao nhiêu distinct key?
9. Hash/group có vừa `work_mem` không?
10. Statistics có đáng tin không?

Quy tắc kinh nghiệm:

```text
Outer nhỏ + inner có index       -> Nested Loop
Hai bên lớn, không cần order     -> Hash Join
Hai bên sorted/cần sorted output -> Merge Join

Group ít key, memory đủ          -> HashAggregate
Input sorted theo group key      -> GroupAggregate
Không sorted + group lớn         -> HashAggregate hoặc Sort + GroupAggregate tùy cost
```

Đây không phải luật tuyệt đối. PostgreSQL luôn chọn theo cost estimate tại thời điểm plan.

## 14. I/O Pattern Tổng Hợp

| Operation | I/O tốt | I/O xấu |
| --- | --- | --- |
| Nested Loop + inner index | Ít random lookup, cache hit cao | Nhiều random lookup, cache miss cao |
| Nested Loop + inner seq scan | Hiếm khi tốt | Scan bảng lớn lặp lại nhiều lần |
| Hash Join | Seq scan + hash in memory | Hash spill temp, nhiều batch |
| Merge Join sorted by index | Streaming qua index order | Heap random access nếu index scan không covering |
| Merge Join with sort | Sort vừa memory | External merge, temp read/write |
| HashAggregate | Hash table vừa memory | Batches nhiều, disk usage lớn |
| GroupAggregate by index | Streaming, memory thấp | Index scan kéo heap quá nhiều random I/O |
| Sort + GroupAggregate | Sort vừa memory | Sort external merge |
| Index Only Scan | Heap Fetches thấp | Heap Fetches cao do visibility map chưa tốt |
| Bitmap Heap Scan | Đọc nhiều row theo page hiệu quả | Bitmap lossy/recheck nhiều, heap read lớn |

## 15. Tín Hiệu Cần Điều Tra

Các dấu hiệu thường chỉ ra vấn đề:

```text
Seq Scan on big_table
Rows Removed by Filter: very large
Nested Loop inner node loops rất cao
Sort Method: external merge
Hash Batches > 1
HashAggregate Batches > 1
Disk Usage: large
temp read=...
temp written=...
Index Only Scan nhưng Heap Fetches cao
estimated rows lệch xa actual rows
```

Khi gặp các tín hiệu này, hướng xử lý thường là:

- Thêm hoặc chỉnh index theo access pattern.
- Rewrite query để giảm row sớm hơn.
- Tránh correlated subquery chạy nhiều lần.
- Tăng/chỉnh statistics.
- Tăng `work_mem` có kiểm soát cho query nặng.
- Tạo summary table/materialized view cho workload báo cáo.
- Xem lại pagination, `LIMIT`, `ORDER BY`.

