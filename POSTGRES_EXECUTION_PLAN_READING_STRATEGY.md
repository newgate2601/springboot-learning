# PostgreSQL Execution Plan Reading Strategy

Tài liệu này mô tả cách đọc `EXPLAIN` / `EXPLAIN ANALYZE` của PostgreSQL để hiểu **thứ tự thực thi thực tế** và nhận diện điểm chậm.

Nguyên tắc quan trọng: không đọc plan máy móc từ trên xuống hoặc từ dưới lên. Phải đọc theo **loại node**.

## 1. Ý tưởng nền tảng

PostgreSQL executor hoạt động theo mô hình pull-based:

- Node cha cần row.
- Node cha gọi node con để lấy row.
- Node con lại gọi node con sâu hơn.
- Node lá như `Seq Scan`, `Index Scan`, `Index Only Scan` mới là nơi thật sự đọc dữ liệu.

Tuy nhiên, khi phân tích thực tế, thứ tự "node nào chạy trước" phụ thuộc vào loại node:

- `Nested Loop`: outer child trước, inner child sau, inner lặp lại theo từng row outer.
- `Hash Join`: build hash side trước, rồi probe side sau.
- `Sort`: phải lấy đủ input cần sort trước khi trả row.
- `Aggregate`: thường phải đọc hết input group trước khi trả kết quả.
- `Limit`: có thể dừng sớm nếu node dưới trả row sớm.
- `SubPlan`: có thể chạy lại nhiều lần nếu phụ thuộc row bên ngoài.

## 2. Các Chỉ Số Cần Nhìn

Một dòng plan thường có dạng:

```text
Index Scan using idx_users_email on users u
  (cost=0.42..8.44 rows=1 width=120)
  (actual time=0.021..0.023 rows=1 loops=1)
```

Ý nghĩa:

- `cost`: ước lượng chi phí của planner, không phải thời gian thật.
- `rows`: số row planner ước lượng node sẽ trả.
- `width`: kích thước trung bình mỗi row, tính bằng byte.
- `actual time`: thời gian thật khi chạy node.
- `actual rows`: số row thật node trả về mỗi loop.
- `loops`: node này chạy bao nhiêu lần.

Khi debug performance, ưu tiên nhìn:

- `actual time`
- `actual rows`
- `loops`
- `Rows Removed by Filter`
- `Buffers: shared hit/read`
- `temp read/written`
- `Heap Fetches`

## 3. `Nested Loop`

Plan mẫu:

```text
Nested Loop
  -> Index Scan using idx_orders_customer_id on orders o
  -> Index Scan using customers_pkey on customers c
       Index Cond: (id = o.customer_id)
```

Cách đọc:

```text
for each row from orders:
    lookup customer by customer_id
```

Thứ tự thực tế:

1. Chạy outer child trước:

   ```text
   Index Scan on orders
   ```

2. Với mỗi order, lấy `o.customer_id`.

3. Chạy inner child:

   ```text
   Index Scan on customers
   ```

4. Lặp lại cho từng order.

Dấu hiệu cần chú ý:

```text
Index Scan on customers ... loops=100000
```

Nếu inner side loop rất nhiều lần mà mỗi lần đắt, query sẽ chậm.

`Nested Loop` tốt khi:

- Outer side trả ít row.
- Inner side lookup bằng index rẻ.
- Có `LIMIT` giúp dừng sớm.

`Nested Loop` xấu khi:

- Outer side trả nhiều row.
- Inner side scan lớn hoặc không có index.
- Inner side có `Seq Scan` và `loops` cao.

Ví dụ xấu:

```text
Nested Loop
  -> Seq Scan on orders o
  -> Seq Scan on order_items oi
       Filter: (order_id = o.id)
```

Cách hiểu:

```text
for each order:
    scan toàn bộ order_items
```

Đây thường là pattern rất chậm.

## 4. `Nested Loop Left Join`

Plan mẫu:

```text
Nested Loop Left Join
  -> Index Scan using idx_products_active on products p
  -> Index Scan using brands_pkey on brands b
       Index Cond: (id = p.brand_id)
```

Cách đọc:

```text
for each product:
    lookup brand
    nếu không thấy brand thì vẫn giữ product
```

Khác biệt với inner join:

- Inner join: không match thì row bị loại.
- Left join: không match thì row vẫn giữ, các cột bên phải là `null`.

`Nested Loop Left Join` thường ổn nếu:

- Outer side ít row.
- Inner lookup bằng primary key hoặc index.

## 5. `Nested Loop Semi Join`

Plan mẫu:

```text
Nested Loop Semi Join
  -> Index Scan using idx_products_newest on products p
  -> Index Only Scan using idx_skus_product_active on product_skus s
       Index Cond: (product_id = p.id)
```

Semi join thường xuất hiện từ `EXISTS`:

```sql
where exists (
    select 1
    from product_skus s
    where s.product_id = p.id
)
```

Cách đọc:

```text
for each product:
    lookup SKU
    chỉ cần thấy 1 SKU là pass
```

Điểm quan trọng:

- Semi join không nhân bản row bên trái.
- Dù product có 10 SKU, output vẫn là 1 product.
- Inner side có thể dừng sớm khi thấy match đầu tiên.

Semi join tốt khi inner side có index theo khóa tương quan:

```sql
create index on product_skus(product_id);
```

## 6. `Hash Join`

Plan mẫu:

```text
Hash Join
  Hash Cond: (o.customer_id = c.id)
  -> Seq Scan on orders o
  -> Hash
       -> Seq Scan on customers c
```

Thứ tự thực tế:

1. Chạy nhánh dưới `Hash` trước:

   ```text
   Seq Scan on customers
   ```

2. Build hash table từ `customers`.

3. Scan `orders`.

4. Với mỗi order, probe vào hash table theo `customer_id`.

Cách đọc:

```text
build hash(customers)
for each order:
    find matching customer in hash
```

`Hash Join` tốt khi:

- Join nhiều row.
- Không có index phù hợp cho nested loop.
- Build side đủ nhỏ để vừa memory.

Dấu hiệu cần chú ý:

```text
Hash
  Buckets: ...
  Batches: 64
  Memory Usage: ...
```

Nếu `Batches` lớn hơn 1, hash bị chia batch, thường do không đủ memory, có thể phát sinh temp I/O.

Ví dụ:

```text
Buffers: temp read=10000 written=12000
```

Đây là dấu hiệu hash join spill ra disk.

## 7. `Hash Semi Join`

Plan mẫu:

```text
Hash Semi Join
  Hash Cond: (p.id = s.product_id)
  -> Seq Scan on products p
  -> Hash
       -> Seq Scan on product_skus s
```

Thường xuất hiện từ `EXISTS`.

Thứ tự thực tế:

1. Scan `product_skus`.
2. Build hash từ `s.product_id`.
3. Scan `products`.
4. Product nào có `p.id` nằm trong hash thì pass.

Cách đọc:

```text
build hash(product_id có SKU)
for each product:
    check product.id in hash
```

`Hash Semi Join` tốt khi:

- Cần kiểm tra tồn tại trên tập lớn.
- Hash side vừa memory.

`Hash Semi Join` có thể xấu khi:

- Hash side rất lớn.
- Hash bị spill ra temp disk.
- Sau đó vẫn phải sort rất nhiều row.

## 8. `Merge Join`

Plan mẫu:

```text
Merge Join
  Merge Cond: (o.customer_id = c.id)
  -> Sort
       Sort Key: o.customer_id
       -> Seq Scan on orders o
  -> Index Scan using customers_pkey on customers c
```

Thứ tự thực tế:

1. Chuẩn bị input bên trái theo thứ tự join key.
2. Chuẩn bị input bên phải theo thứ tự join key.
3. Merge hai stream đã sort.

Cách đọc:

```text
sort orders by customer_id
read customers by id order
merge two sorted streams
```

`Merge Join` tốt khi:

- Hai input đã có sẵn thứ tự từ index.
- Cần join nhiều row.

`Merge Join` có thể xấu khi:

- Phải sort input lớn.
- Sort bị external merge ra disk.

## 9. `Sort`

Plan mẫu:

```text
Sort
  Sort Key: created_at DESC, id DESC
  Sort Method: external merge  Disk: 200000kB
  -> Seq Scan on orders
```

Thứ tự thực tế:

1. Chạy child node để lấy rows.
2. Sort toàn bộ rows cần sort.
3. Trả row đã sort lên node cha.

Nếu sort nằm dưới `Limit`:

```text
Limit
  -> Sort
       -> Seq Scan on orders
```

Không có nghĩa là chỉ sort vài row. Thường PostgreSQL vẫn phải lấy đủ rows từ child để biết top rows là gì.

Dấu hiệu xấu:

```text
Sort Method: external merge
```

Nghĩa là sort không vừa memory, phải dùng disk.

Cách tối ưu thường gặp:

```sql
create index on orders(created_at desc, id desc);
```

Nếu query là:

```sql
select *
from orders
order by created_at desc, id desc
limit 20;
```

Index đúng thứ tự có thể giúp PostgreSQL tránh sort lớn.

## 10. `Limit`

Plan mẫu tốt:

```text
Limit
  -> Index Scan using idx_orders_created_at_desc on orders
```

Cách đọc:

```text
read orders từ index theo đúng order
đủ limit thì dừng
```

Plan này tốt vì node dưới trả row theo đúng thứ tự cần.

Plan mẫu chưa tốt:

```text
Limit
  -> Sort
       -> Seq Scan on orders
```

Cách đọc:

```text
scan nhiều rows
sort nhiều rows
sau đó limit mới lấy vài rows
```

`Limit` chỉ giúp dừng sớm khi node dưới có thể trả row sớm theo đúng điều kiện.

## 11. `Aggregate`

Plan mẫu:

```text
Aggregate
  -> Seq Scan on order_items
```

Cách đọc:

```text
scan order_items
tính aggregate
trả kết quả
```

Aggregate thường phải đọc toàn bộ input group.

Ví dụ:

```sql
select count(*)
from order_items
where order_id = 10;
```

Nếu không có index:

```text
Aggregate
  -> Seq Scan on order_items
       Filter: (order_id = 10)
```

Nếu có index:

```text
Aggregate
  -> Index Only Scan using idx_order_items_order_id on order_items
       Index Cond: (order_id = 10)
```

## 12. `min()` / `max()` Được Tối Ưu Bằng Index

Query:

```sql
select min(price)
from product_skus
where product_id = 100;
```

Index:

```sql
create index on product_skus(product_id, price);
```

Plan tốt có thể là:

```text
Result
  InitPlan 1
    -> Limit
         -> Index Only Scan using idx_product_skus_product_price on product_skus
              Index Cond: (product_id = 100)
```

Cách đọc:

```text
lookup product_id trong index
lấy row đầu tiên theo price
dừng
```

Đây là trường hợp `min(price)` không cần scan toàn bộ SKU của product.

Nhưng nếu query là:

```sql
select min(compare_price)
from product_skus
where product_id = 100;
```

và index là:

```sql
(product_id, price)
```

thì index không sort theo `compare_price`. PostgreSQL có thể vẫn phải đọc nhiều SKU của product rồi aggregate.

## 13. `SubPlan`

Plan mẫu:

```text
Seq Scan on products p
  SubPlan 1
    -> Aggregate
         -> Index Scan using idx_skus_product_id on product_skus s
              Index Cond: (product_id = p.id)
```

SubPlan thường đến từ correlated subquery:

```sql
select
    p.id,
    (
        select count(*)
        from product_skus s
        where s.product_id = p.id
    ) as sku_count
from products p;
```

Cách đọc:

```text
for each product:
    run subquery count SKU
```

Điểm cần nhìn:

```text
loops=...
```

Nếu:

```text
SubPlan ... loops=100000
```

thì subquery chạy 100000 lần.

SubPlan có thể ổn nếu:

- Outer rows ít.
- Subquery dùng index rất rẻ.

SubPlan xấu nếu:

- Outer rows nhiều.
- Subquery scan bảng lớn.

Ví dụ xấu:

```text
SubPlan 1
  -> Seq Scan on product_skus
       Filter: (product_id = p.id)
       Rows Removed by Filter: 999999
       loops=10000
```

Cách hiểu:

```text
for each product:
    scan toàn bộ product_skus
```

Đây là pattern cần tối ưu.

## 14. `Index Scan` và `Index Only Scan`

### Index Scan

Plan:

```text
Index Scan using users_pkey on users
  Index Cond: (id = 10)
```

Cách đọc:

```text
dùng index tìm row id = 10
đọc heap/table để lấy dữ liệu row
```

### Index Only Scan

Plan:

```text
Index Only Scan using idx_users_email_name on users
  Index Cond: (email = 'a@test.com')
  Heap Fetches: 0
```

Cách đọc:

```text
dùng index tìm row
lấy đủ dữ liệu từ index
không cần đọc heap
```

`Heap Fetches: 0` là dấu hiệu rất tốt.

Nhưng `Index Only Scan` không phải lúc nào cũng tránh heap hoàn toàn. PostgreSQL cần visibility map để biết row có visible với transaction hiện tại hay không.

Nếu thấy:

```text
Heap Fetches: 10000
```

thì vẫn có nhiều lần quay lại heap.

## 15. `Bitmap Index Scan` và `Bitmap Heap Scan`

Plan mẫu:

```text
Bitmap Heap Scan on orders
  Recheck Cond: (customer_id = 10)
  -> Bitmap Index Scan on idx_orders_customer_id
       Index Cond: (customer_id = 10)
```

Thứ tự thực tế:

1. `Bitmap Index Scan`: đọc index, tạo bitmap các heap page/tuple cần lấy.
2. `Bitmap Heap Scan`: đọc heap theo bitmap.

Cách đọc:

```text
tìm nhiều row qua index
gom vị trí row thành bitmap
đọc heap theo page hiệu quả hơn
```

Bitmap scan thường xuất hiện khi:

- Điều kiện match nhiều row.
- Dùng index scan từng row sẽ tốn nhiều random I/O.

## 16. `Rows Removed by Filter`

Ví dụ:

```text
Seq Scan on products
  Filter: (status = 'ACTIVE')
  Rows Removed by Filter: 900000
```

Cách hiểu:

```text
PostgreSQL đọc rất nhiều row rồi bỏ đi nhiều row
```

Đây có thể là dấu hiệu cần index nếu:

- Bảng lớn.
- Query chạy thường xuyên.
- Filter có selectivity tốt.

Nhưng không phải lúc nào cũng cần index. Nếu `status = 'ACTIVE'` chiếm 90% bảng, index riêng trên `status` thường không hữu ích.

## 17. `Buffers`

Ví dụ:

```text
Buffers: shared hit=1000 read=500
```

Ý nghĩa:

- `shared hit`: page đã có trong PostgreSQL shared buffer.
- `shared read`: phải đọc page từ disk hoặc OS cache.
- `temp read/written`: đọc/ghi temp file, thường do sort/hash không vừa memory.

Dấu hiệu cần chú ý:

```text
temp read=...
temp written=...
```

Thường liên quan tới:

- `Sort Method: external merge`
- Hash join spill
- Hash aggregate spill

## 18. Checklist Đọc Plan Bất Kỳ

Khi đọc một plan mới, làm theo thứ tự:

1. Tìm các node lá: `Seq Scan`, `Index Scan`, `Index Only Scan`, `Bitmap Index Scan`.
2. Xác định node cha trực tiếp của từng scan.
3. Nếu cha là `Nested Loop`, đọc outer child trước, inner child sau.
4. Nếu cha là `Hash Join`, tìm nhánh `Hash`; nhánh đó build trước.
5. Nếu có `Sort`, kiểm tra sort bao nhiêu row và có external merge không.
6. Nếu có `Limit`, kiểm tra node dưới có trả row theo đúng order không.
7. Nếu có `Aggregate`, kiểm tra nó phải đọc bao nhiêu row.
8. Nếu có `SubPlan`, nhìn `loops` để biết subquery chạy bao nhiêu lần.
9. So sánh `rows` estimate và `actual rows`; lệch lớn có thể do statistics không tốt.
10. Nhìn `Rows Removed by Filter`; nhiều row bị bỏ có thể là dấu hiệu scan thừa.
11. Nhìn `Buffers shared read`; read lớn nghĩa là đọc nhiều page.
12. Nhìn `temp read/written`; có temp I/O là dấu hiệu sort/hash/aggregate spill.
13. Nhìn `Heap Fetches` với `Index Only Scan`.
14. Tìm node có `actual time` lớn nhất hoặc `loops` lớn nhất.
15. Đặt câu hỏi: có thể giảm row sớm hơn không, dùng index đúng order không, hoặc tránh subquery lặp không?

## 19. Câu Nhớ Nhanh

```text
Nested Loop: outer trước, inner sau, inner lặp.
Hash Join: hash side build trước, outer side probe sau.
Merge Join: hai input phải sorted trước, rồi mới merge.
Sort: ăn input trước, sort xong mới trả row.
Aggregate: thường ăn input trước, aggregate xong mới trả row.
Limit: chỉ dừng sớm nếu node dưới trả row sớm.
SubPlan: nhìn loops, vì có thể chạy lại rất nhiều lần.
Index Only Scan: tốt nhất khi Heap Fetches thấp hoặc bằng 0.
```

