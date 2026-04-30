# So Sánh OR, UNION, UNION ALL Trong PostgreSQL

Tài liệu này so sánh `OR`, `UNION`, `UNION ALL` theo góc nhìn tối ưu query PostgreSQL, đặc biệt với các điều kiện nằm trên nhiều field khác nhau.

Ví dụ thực tế dựa trên database `banking`:

- `catalog_products`: khoảng 3,000,000 rows.
- `catalog_product_skus`: khoảng 10,000,000 rows.
- PostgreSQL có nhiều index partial cho data `ACTIVE`.

## 1. Khác Biệt Logic

### 1.1. OR

```sql
select *
from catalog_product_skus
where status = 'ACTIVE'
  and sale_price > 0
  and (
    product_id <= 500000
    or sale_price < 1000
  );
```

`OR` trả về mỗi row tối đa một lần. Nếu một row thỏa cả hai điều kiện, row đó vẫn chỉ xuất hiện một lần.

Logic:

```text
lấy row nếu điều kiện A đúng hoặc điều kiện B đúng
```

### 1.2. UNION

```sql
select id
from catalog_product_skus
where status = 'ACTIVE'
  and sale_price > 0
  and product_id <= 500000

union

select id
from catalog_product_skus
where status = 'ACTIVE'
  and sale_price > 0
  and sale_price < 1000;
```

`UNION` gộp kết quả từ nhiều query và loại duplicate.

Logic gần giống `OR`, nhưng PostgreSQL phải thêm bước loại trùng:

```text
chạy nhánh A
chạy nhánh B
gộp lại
loại duplicate
```

### 1.3. UNION ALL

```sql
select id
from catalog_product_skus
where status = 'ACTIVE'
  and sale_price > 0
  and product_id <= 500000

union all

select id
from catalog_product_skus
where status = 'ACTIVE'
  and sale_price > 0
  and sale_price < 1000;
```

`UNION ALL` gộp kết quả nhưng không loại duplicate.

Nếu một row thỏa cả hai nhánh, row đó sẽ xuất hiện hai lần.

Logic:

```text
chạy nhánh A
chạy nhánh B
append kết quả
không loại duplicate
```

Vì vậy `UNION ALL` chỉ tương đương `OR` khi các nhánh không trùng nhau, hoặc khi duplicate là điều được chấp nhận.

## 2. Cách Viết UNION ALL Tương Đương OR

Query `OR`:

```sql
select count(*)
from catalog_product_skus
where status = 'ACTIVE'
  and sale_price > 0
  and (
    product_id <= 500000
    or sale_price < 1000
  );
```

Nếu viết thẳng bằng `UNION ALL`, có thể bị duplicate vì một SKU có thể vừa `product_id <= 500000`, vừa `sale_price < 1000`.

Muốn `UNION ALL` tương đương logic `OR`, cần làm các nhánh không giao nhau:

```sql
select count(*)
from (
  select 1
  from catalog_product_skus
  where status = 'ACTIVE'
    and sale_price > 0
    and product_id <= 500000

  union all

  select 1
  from catalog_product_skus
  where status = 'ACTIVE'
    and sale_price > 0
    and product_id > 500000
    and sale_price < 1000
) q;
```

Ở đây nhánh 2 có thêm:

```sql
product_id > 500000
```

Điều này đảm bảo nhánh 2 không lấy lại row đã thuộc nhánh 1.

## 3. Chiến Lược Thực Thi Của OR

PostgreSQL có nhiều cách thực thi `OR`, tùy index, độ chọn lọc, số row match và chi phí đọc heap.

### 3.1. OR Trên Cùng Một Field

Ví dụ:

```sql
select id, name, category_id, brand_id
from catalog_products
where status = 'ACTIVE'
  and published_at is not null
  and (category_id = 1 or category_id = 2);
```

PostgreSQL có thể rewrite thành dạng tương đương:

```sql
category_id = any ('{1,2}')
```

Plan thực tế:

```text
Index Scan using idx_catalog_products_listing_cat_brand_new_active
Index Cond: category_id = ANY ('{1,2}')
Execution Time: ~0.130 ms
```

Use case:

- `OR` trên cùng một column.
- Điều kiện là equality.
- Nên ưu tiên `IN`.

Viết tốt hơn:

```sql
and category_id in (1, 2)
```

Trong case này, đổi sang `UNION` thường không có lợi vì PostgreSQL đã xử lý tốt.

### 3.2. OR Trên Nhiều Field Khác Nhau, Có Index Riêng

Ví dụ:

```sql
select id, name, category_id, brand_id
from catalog_products
where status = 'ACTIVE'
  and published_at is not null
  and (category_id = 1 or brand_id = 2);
```

Với hai index riêng:

```sql
create index idx_catalog_products_active_category
on catalog_products (category_id)
where status = 'ACTIVE' and published_at is not null;

create index idx_catalog_products_active_brand
on catalog_products (brand_id)
where status = 'ACTIVE' and published_at is not null;
```

PostgreSQL có thể dùng:

```text
Bitmap Heap Scan
  Recheck Cond: category_id = 1 OR brand_id = 2
  -> BitmapOr
       -> Bitmap Index Scan on idx_catalog_products_active_category
       -> Bitmap Index Scan on idx_catalog_products_active_brand
```

Ý nghĩa:

```text
scan index category để lấy TID
scan index brand để lấy TID
OR hai bitmap lại
đọc heap theo page
recheck điều kiện thật
```

Use case phù hợp:

- Điều kiện `OR` nằm trên nhiều field.
- Mỗi field có index riêng.
- Query cần đọc thêm nhiều column không nằm trong index.
- Số row match vừa hoặc lớn.
- Đọc heap theo page rẻ hơn đọc từng row ngẫu nhiên.

Điểm cần xem trong plan:

```text
BitmapOr
Bitmap Index Scan
Bitmap Heap Scan
Heap Blocks
Recheck Cond
Rows Removed by Filter
Buffers
```

### 3.3. OR Bị Biến Thành Filter Trên Một Index Lớn

Ví dụ thực tế trên `catalog_product_skus`:

```sql
explain analyze
select count(*)
from catalog_product_skus
where status = 'ACTIVE'
  and sale_price > 0
  and (
    product_id <= 500000
    or (product_id > 500000 and sale_price < 1000)
  );
```

Plan:

```text
Parallel Index Only Scan using idx_catalog_product_skus_active_sale_price_product
  Filter: product_id <= 500000 OR (product_id > 500000 AND sale_price < 1000)
  Rows Removed by Filter: 2098703
  Heap Fetches: 0
Execution Time: ~942 ms
```

Điểm quan trọng:

```text
Index Only Scan không có nghĩa là chắc chắn nhanh.
```

Plan này không đọc heap vì:

```text
Heap Fetches: 0
```

Nhưng nó vẫn phải đọc rất nhiều entry trong index rồi mới lọc bằng `Filter`.

Dấu hiệu không tốt:

```text
Rows Removed by Filter lớn
Buffers rất lớn
Điều kiện quan trọng nằm ở Filter thay vì Index Cond
```

Trong case này:

```text
OR: khoảng 8.78 triệu buffer hit/read
Execution Time: khoảng 942 ms
```

## 4. Chiến Lược Thực Thi Của UNION

`UNION` tách query thành nhiều nhánh độc lập, sau đó loại duplicate.

Ví dụ:

```sql
select id
from catalog_product_skus
where status = 'ACTIVE'
  and sale_price > 0
  and product_id <= 500000

union

select id
from catalog_product_skus
where status = 'ACTIVE'
  and sale_price > 0
  and sale_price < 1000;
```

Plan thường gặp:

```text
HashAggregate hoặc Sort + Unique
  -> Append
       -> nhánh 1 dùng index A
       -> nhánh 2 dùng index B
```

Ý nghĩa:

```text
PostgreSQL chạy từng SELECT riêng
append kết quả
sort/hash để loại duplicate
trả kết quả cuối
```

Use case phù hợp:

- Cần kết quả giống `OR`.
- Các nhánh có thể trùng.
- Bắt buộc loại duplicate.
- Mỗi nhánh có index tốt hơn so với để `OR`.

Điểm yếu:

- Phải distinct.
- Có thể dùng `Sort + Unique`.
- Có thể dùng `HashAggregate`.
- Result lớn có thể spill ra temp disk.

Dấu hiệu trong plan:

```text
Sort Method
Memory
Disk Usage
temp read
temp written
Batches
```

Ví dụ thực tế khi result lớn:

```text
HashAggregate
Planned Partitions: 32
Batches: 33
Disk Usage: 95360kB
temp read/write xuất hiện
Execution Time: ~2902 ms
```

Trong case này, `UNION` chậm hơn cả `OR` vì chi phí loại duplicate quá lớn.

## 5. Chiến Lược Thực Thi Của UNION ALL

`UNION ALL` tách query thành nhiều nhánh độc lập, nhưng không loại duplicate.

Ví dụ tương đương `OR` vì đã tách nhánh không giao nhau:

```sql
explain analyze
select count(*)
from (
  select 1
  from catalog_product_skus
  where status = 'ACTIVE'
    and sale_price > 0
    and product_id <= 500000

  union all

  select 1
  from catalog_product_skus
  where status = 'ACTIVE'
    and sale_price > 0
    and product_id > 500000
    and sale_price < 1000
) q;
```

Plan thực tế:

```text
Parallel Append
  -> Parallel Index Only Scan using idx_catalog_product_skus_active_product_sale_price
       Index Cond: product_id <= 500000
  -> Parallel Index Only Scan using idx_catalog_product_skus_active_sale_price_product
       Index Cond: sale_price < 1000 AND product_id > 500000
Execution Time: ~381 ms
```

Điểm mạnh:

```text
Mỗi nhánh có Index Cond riêng.
Không cần Sort.
Không cần Unique.
Không cần HashAggregate để distinct.
Có thể chạy Parallel Append.
```

So với `OR`:

```text
OR:
  scan một vùng index lớn
  filter nhiều row
  Execution Time: ~942 ms

UNION ALL:
  nhánh 1 seek theo product_id
  nhánh 2 seek theo sale_price, product_id
  append kết quả
  Execution Time: ~381 ms
```

Use case rất phù hợp:

- `OR` nằm trên nhiều field khác nhau.
- Mỗi nhánh có index riêng tốt.
- Có thể viết các nhánh không giao nhau.
- Không cần loại duplicate.
- Query trả nhiều row hoặc scan bảng lớn.

## 6. Bitmap Index Scan + Bitmap Heap Scan So Với UNION ALL

### 6.1. Bitmap Có Thể Tốt Khi Nào?

Bitmap strategy thường tốt khi:

- Có nhiều row match.
- Row nằm rải rác trong heap.
- Query cần đọc nhiều column không nằm trong index.
- PostgreSQL muốn gom TID trước rồi đọc heap theo page.

Ví dụ plan:

```text
Bitmap Heap Scan
  Recheck Cond: condition A OR condition B
  Heap Blocks: exact=...
  -> BitmapOr
       -> Bitmap Index Scan on index_a
       -> Bitmap Index Scan on index_b
```

Ưu điểm:

- Gom nhiều index scan lại.
- Tránh đọc heap quá ngẫu nhiên.
- Tự loại trùng ở mức bitmap TID.
- Semantics giống `OR`, không bị duplicate.

Nhược điểm:

- Vẫn phải đọc heap nếu cần column ngoài index.
- Với result quá lớn, bitmap có thể tốn memory.
- Có thể có `Recheck Cond`.
- Có thể không tốt bằng `UNION ALL` nếu từng nhánh có index cực kỳ chọn lọc.

### 6.2. UNION ALL Có Thể Tốt Hơn Bitmap Khi Nào?

`UNION ALL` thường tốt hơn khi:

- Mỗi nhánh có index covering hoặc index-only scan tốt.
- Điều kiện của từng nhánh đẩy được vào `Index Cond`.
- Các nhánh không trùng nhau.
- Không cần đọc heap.
- Không cần distinct.

Plan tốt:

```text
Parallel Append
  -> Index Only Scan
       Index Cond: condition A
  -> Index Only Scan
       Index Cond: condition B
```

Điểm cần nhìn:

```text
Heap Fetches: 0
Index Cond rõ ràng
Rows Removed by Filter thấp hoặc không có
Buffers thấp hơn OR/Bitmap
Không có Sort/HashAggregate distinct
```

### 6.3. Bitmap Có Thể Nhanh Hơn UNION Khi Nào?

Bitmap có thể nhanh hơn `UNION` nếu:

- `UNION` phải loại duplicate trên result lớn.
- `UNION` sinh `HashAggregate` hoặc `Sort + Unique` tốn memory.
- Result bị spill ra disk.
- Hai nhánh overlap nhiều.

Ví dụ dấu hiệu `UNION` bị đắt:

```text
HashAggregate
Batches: nhiều
Disk Usage: lớn
temp read/write lớn
```

Trong trường hợp này, `OR` với `BitmapOr` có thể thắng vì bitmap tự hợp nhất TID trước khi đọc heap, không cần distinct toàn bộ result set.

## 7. So Sánh Theo Use Case

### 7.1. OR Trên Cùng Một Column

Ví dụ:

```sql
where status = 'ACTIVE'
   or status = 'DRAFT'
```

Nên viết:

```sql
where status in ('ACTIVE', 'DRAFT')
```

Chiến lược thường tốt:

```text
Index Scan hoặc Bitmap Index Scan với ANY/IN
```

Khuyến nghị:

```text
Dùng IN.
Không cần đổi sang UNION.
```

### 7.2. OR Trên Nhiều Column, Result Nhỏ

Ví dụ:

```sql
where category_id = 1
   or brand_id = 2
```

Nếu có index riêng:

```text
BitmapOr có thể đủ nhanh.
UNION ALL cũng có thể nhanh.
```

Khuyến nghị:

```text
Đo cả hai bằng EXPLAIN ANALYZE, BUFFERS.
Nếu chênh lệch nhỏ, ưu tiên query dễ đọc.
```

### 7.3. OR Trên Nhiều Column, Result Lớn

Ví dụ trên `catalog_product_skus`:

```sql
product_id <= 500000
or sale_price < 1000
```

Nếu `OR` bị đưa vào `Filter`, có thể chậm:

```text
Rows Removed by Filter lớn
Buffers lớn
Execution Time cao
```

Khuyến nghị:

```text
Thử tách UNION ALL.
Làm các nhánh không giao nhau nếu cần giữ semantics giống OR.
Tạo index đúng cho từng nhánh.
```

### 7.4. Các Nhánh Có Thể Trùng Nhau

Nếu dùng:

```sql
condition A
union all
condition B
```

và một row thỏa cả A lẫn B, kết quả sẽ bị duplicate.

Có ba hướng xử lý:

1. Dùng `UNION`.
2. Dùng `UNION ALL` nhưng thêm điều kiện loại giao nhau.
3. Giữ `OR`.

Ví dụ loại giao nhau:

```sql
select id
from catalog_product_skus
where product_id <= 500000

union all

select id
from catalog_product_skus
where product_id > 500000
  and sale_price < 1000;
```

### 7.5. Cần DISTINCT Thật Sự

Nếu nghiệp vụ cần loại duplicate, `UNION` là đúng về logic.

Nhưng cần hiểu chi phí:

```text
UNION = UNION ALL + DISTINCT
```

Plan có thể có:

```text
Sort + Unique
```

hoặc:

```text
HashAggregate
```

Khuyến nghị:

```text
Nếu result lớn, kiểm tra temp read/write.
Nếu spill disk, UNION có thể rất đắt.
```

### 7.6. Query Chỉ COUNT

Với `count(*)`, nếu index đủ điều kiện, PostgreSQL có thể dùng `Index Only Scan`.

Nhưng vẫn cần phân biệt:

```text
Index Cond: điều kiện dùng để giới hạn vùng index cần đọc.
Filter: điều kiện kiểm tra sau khi đã đọc index entry.
```

`Index Only Scan` tốt nhất khi:

```text
Index Cond chọn lọc tốt.
Heap Fetches = 0.
Rows Removed by Filter thấp.
```

Nếu `Rows Removed by Filter` rất lớn, query vẫn có thể chậm dù không đọc heap.

## 8. Bảng So Sánh Nhanh

| Tiêu chí | OR | UNION | UNION ALL |
|---|---:|---:|---:|
| Giữ đúng semantics không duplicate | Có | Có | Không mặc định |
| Có thể dùng nhiều index | Có, qua BitmapOr hoặc plan khác | Có | Có |
| Có chi phí distinct | Không | Có | Không |
| Dễ bị duplicate | Không | Không | Có |
| Dễ đọc | Thường dễ nhất | Trung bình | Trung bình |
| Tối ưu cho nhiều field khác nhau | Tùy planner | Có thể tốt | Thường tốt nhất nếu không trùng |
| Phù hợp result lớn | Tùy plan | Cẩn thận distinct | Tốt nếu từng nhánh chọn lọc |
| Dễ bị temp disk | Ít hơn | Có thể cao | Thấp hơn |

## 9. Kết Quả Đo Thực Tế Trong Database Banking

### 9.1. Bảng catalog_products

Điều kiện:

```sql
category_id = 1 or brand_id = 2
```

Sau khi có index riêng:

```text
OR:        ~0.167 ms
UNION:     ~0.131 ms
UNION ALL: ~0.078 ms
```

Vì result rất nhỏ, cả ba đều nhanh. Chênh lệch không đủ lớn để kết luận tuyệt đối.

### 9.2. Bảng catalog_product_skus

Điều kiện tương đương không duplicate:

```sql
product_id <= 500000
or (product_id > 500000 and sale_price < 1000)
```

Kết quả:

```text
OR:        ~942 ms
UNION ALL: ~381 ms
```

Khác biệt chính:

```text
OR:
  dùng Index Only Scan nhưng điều kiện OR nằm ở Filter
  phải đọc rất nhiều index entry rồi loại bỏ
  Buffers khoảng 8.78 triệu

UNION ALL:
  mỗi nhánh có Index Cond riêng
  dùng Parallel Append
  Buffers khoảng 2.47 triệu
```

Kết luận:

```text
UNION ALL nhanh hơn đáng kể vì đọc ít dữ liệu hơn, không phải vì bản thân UNION ALL luôn thần kỳ.
```

## 10. Checklist Khi Tối Ưu OR

Khi gặp query có `OR`, kiểm tra theo thứ tự:

1. `OR` có nằm trên cùng một column không?
2. Nếu cùng column, có đổi được sang `IN` không?
3. Nếu khác column, mỗi nhánh có index riêng phù hợp không?
4. `EXPLAIN ANALYZE` có `BitmapOr` không?
5. Điều kiện quan trọng nằm ở `Index Cond` hay `Filter`?
6. `Rows Removed by Filter` có lớn không?
7. `Buffers` của `OR` và `UNION ALL` chênh nhau nhiều không?
8. Các nhánh `UNION ALL` có bị duplicate không?
9. Nếu có duplicate, có thêm điều kiện loại giao nhau được không?
10. Nếu phải dùng `UNION`, có bị `Sort`, `HashAggregate`, `temp read/write` lớn không?

## 11. Quy Tắc Thực Dụng

```text
OR cùng column
  -> dùng IN.

OR khác column, result nhỏ
  -> giữ OR nếu plan tốt và dễ đọc.

OR khác column, result lớn, mỗi nhánh có index riêng
  -> thử UNION ALL.

UNION ALL có duplicate
  -> thêm điều kiện loại giao nhau, hoặc dùng UNION nếu bắt buộc.

UNION result lớn
  -> cẩn thận chi phí distinct, Sort, HashAggregate, temp disk.

Index Only Scan nhưng vẫn chậm
  -> xem Index Cond, Filter, Rows Removed by Filter, Buffers.
```

Điều quan trọng nhất:

```text
Không kết luận UNION hay UNION ALL luôn nhanh hơn OR.
Phải đo bằng EXPLAIN ANALYZE, BUFFERS trên data thật.
```
