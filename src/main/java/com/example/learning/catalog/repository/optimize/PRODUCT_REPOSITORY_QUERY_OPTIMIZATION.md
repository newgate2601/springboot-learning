# ProductRepository Query Optimization Notes

Tài liệu này ghi lại phân tích và tối ưu query trong `ProductRepository`.

Hiện tại tài liệu chỉ phân tích:

- `findNewestVisibleProductsQuery`

Các hàm khác sẽ được bổ sung sau khi có execution plan và yêu cầu phân tích riêng.

## 1. `findNewestVisibleProductsQuery`

### 1.1. Mục đích nghiệp vụ

`findNewestVisibleProductsQuery` lấy danh sách sản phẩm mới nhất để hiển thị ở khu vực "Sản phẩm mới" trên Home.

Điều kiện visibility:

- Product phải `ACTIVE`.
- Product phải đã publish: `published_at is not null`.
- Category phải `ACTIVE`.
- Product phải có ít nhất một SKU `ACTIVE` và `sale_price > 0`.
- Brand chỉ là thông tin bổ sung, không bắt buộc product phải có brand active.

Sort:

```sql
order by p.published_at desc, p.id desc
```

`p.id desc` là tie-breaker để thứ tự ổn định khi nhiều product có cùng `published_at`.

### 1.2. Vì sao join khác nhau?

Category dùng `join` vì category là điều kiện bắt buộc để product được public:

```sql
join catalog_categories c
  on c.id = p.category_id
 and c.status = 'ACTIVE'
```

Nếu category không active, product bị loại khỏi kết quả.

Brand dùng `left join` vì brand là thông tin bổ sung:

```sql
left join catalog_brands b
  on b.id = p.brand_id
 and b.status = 'ACTIVE'
```

Nếu brand null hoặc inactive, product vẫn được trả về, chỉ có `brand_name = null`.

SKU dùng `exists` vì một product có nhiều SKU. Nếu join SKU trực tiếp vào query chính, product sẽ bị nhân bản theo số SKU. Query chỉ cần biết product có ít nhất một SKU bán được hay không:

```sql
exists (
    select 1
    from catalog_product_skus s
    where s.product_id = p.id
      and s.status = 'ACTIVE'
      and s.sale_price > 0
)
```

Trong execution plan, PostgreSQL có thể biến `exists` thành `Semi Join`. `Semi Join` chỉ kiểm tra tồn tại, không nhân bản row product.

## 2. Query PostgreSQL Dùng Để Explain

```sql
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT
    p.id AS product_id,
    p.name AS product_name,
    p.slug AS slug,
    p.short_description AS short_description,
    b.name AS brand_name,
    c.name AS category_name,
    p.image_urls AS image_urls,
    (
        SELECT MIN(s2.sale_price)
        FROM catalog_product_skus s2
        WHERE s2.product_id = p.id
          AND s2.status = 'ACTIVE'
          AND s2.sale_price > 0
    ) AS min_sale_price,
    (
        SELECT MIN(s3.compare_price)
        FROM catalog_product_skus s3
        WHERE s3.product_id = p.id
          AND s3.status = 'ACTIVE'
          AND s3.sale_price > 0
    ) AS min_compare_price
FROM catalog_products p
JOIN catalog_categories c
  ON c.id = p.category_id
 AND c.status = 'ACTIVE'
LEFT JOIN catalog_brands b
  ON b.id = p.brand_id
 AND b.status = 'ACTIVE'
WHERE p.status = 'ACTIVE'
  AND p.published_at IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM catalog_product_skus s
      WHERE s.product_id = p.id
        AND s.status = 'ACTIVE'
        AND s.sale_price > 0
  )
ORDER BY p.published_at DESC, p.id DESC
LIMIT 12 OFFSET 0;
```

## 3. Plan Trước Khi Thêm Index

Execution time: khoảng `53742 ms`.

```text
Limit  (cost=1044640.21..10825811.26 rows=12 width=309) (actual time=17947.760..53715.486 rows=12.00 loops=1)
  Buffers: shared hit=19465 read=6046111 dirtied=25, temp read=199241 written=280945
  ->  Nested Loop Left Join  (cost=1044640.21..1866348737066.27 rows=2289723 width=309) (actual time=17947.758..53715.474 rows=12.00 loops=1)
        Buffers: shared hit=19465 read=6046111 dirtied=25, temp read=199241 written=280945
        ->  Nested Loop  (cost=1044639.78..2464122.55 rows=2289723 width=241) (actual time=13159.428..13305.300 rows=12.00 loops=1)
              Buffers: shared hit=8351 read=476025 dirtied=25, temp read=199241 written=280945
              ->  Gather Merge  (cost=1044639.36..1325745.30 rows=2413621 width=234) (actual time=13157.726..13276.045 rows=12.00 loops=1)
                    Workers Planned: 2
                    Workers Launched: 2
                    Buffers: shared hit=8327 read=476001 dirtied=25, temp read=199241 written=280945
                    ->  Sort  (cost=1043639.33..1046153.52 rows=1005675 width=234) (actual time=13034.089..13034.147 rows=167.33 loops=3)
                          Sort Key: p.published_at DESC, p.id DESC
                          Sort Method: external merge  Disk: 220096kB
                          Buffers: shared hit=8327 read=476001 dirtied=25, temp read=199241 written=280945
                          Worker 0:  Sort Method: external merge  Disk: 219688kB
                          Worker 1:  Sort Method: external merge  Disk: 220104kB
                          ->  Parallel Hash Semi Join  (cost=354990.34..716504.88 rows=1005675 width=234) (actual time=8667.630..9857.725 rows=900000.00 loops=3)
                                Hash Cond: (p.id = s.product_id)
                                Buffers: shared hit=8299 read=476001 dirtied=25, temp read=115285 written=115804
                                ->  Parallel Seq Scan on catalog_products p  (cost=0.00..267337.42 rows=1005675 width=234) (actual time=2.178..5412.957 rows=900000.00 loops=3)
                                      Filter: ((published_at IS NOT NULL) AND ((status)::text = 'ACTIVE'::text))
                                      Rows Removed by Filter: 100000
                                      Buffers: shared hit=8111 read=243641 dirtied=25
                                ->  Parallel Hash  (cost=295048.09..295048.09 rows=3653620 width=8) (actual time=2509.609..2509.611 rows=2918919.00 loops=3)
                                      Buckets: 262144  Batches: 64  Memory Usage: 7488kB
                                      Buffers: shared hit=188 read=232360, temp written=29788
                                      ->  Parallel Seq Scan on catalog_product_skus s  (cost=0.00..295048.09 rows=3653620 width=8) (actual time=0.737..2016.800 rows=2918919.00 loops=3)
                                            Filter: ((sale_price > '0'::numeric) AND ((status)::text = 'ACTIVE'::text))
                                            Rows Removed by Filter: 414414
                                            Buffers: shared hit=188 read=232360
              ->  Index Scan using catalog_categories_pkey on catalog_categories c  (cost=0.42..0.47 rows=1 width=23) (actual time=2.432..2.432 rows=1.00 loops=12)
                    Index Cond: (id = p.category_id)
                    Filter: ((status)::text = 'ACTIVE'::text)
                    Index Searches: 12
                    Buffers: shared hit=24 read=24
        ->  Memoize  (cost=0.43..0.47 rows=1 width=20) (actual time=2.409..2.410 rows=1.00 loops=12)
              Cache Key: p.brand_id
              Cache Mode: logical
              Hits: 0  Misses: 12  Evictions: 0  Overflows: 0  Memory Usage: 2kB
              Buffers: shared hit=24 read=24
              ->  Index Scan using catalog_brands_pkey on catalog_brands b  (cost=0.42..0.46 rows=1 width=20) (actual time=2.335..2.335 rows=1.00 loops=12)
                    Index Cond: (id = p.brand_id)
                    Filter: ((status)::text = 'ACTIVE'::text)
                    Index Searches: 12
                    Buffers: shared hit=24 read=24
        SubPlan 1
          ->  Aggregate  (cost=407548.25..407548.26 rows=1 width=32) (actual time=1787.264..1787.264 rows=1.00 loops=12)
                Buffers: shared hit=5498 read=2785078
                ->  Seq Scan on catalog_product_skus s2  (cost=0.00..407548.24 rows=3 width=6) (actual time=550.197..1787.217 rows=2.83 loops=12)
                      Filter: ((sale_price > '0'::numeric) AND (product_id = p.id) AND ((status)::text = 'ACTIVE'::text))
                      Rows Removed by Filter: 9999997
                      Buffers: shared hit=5498 read=2785078
        SubPlan 2
          ->  Aggregate  (cost=407548.25..407548.26 rows=1 width=32) (actual time=1577.823..1577.824 rows=1.00 loops=12)
                Buffers: shared hit=5592 read=2784984
                ->  Seq Scan on catalog_product_skus s3  (cost=0.00..407548.24 rows=3 width=6) (actual time=478.103..1577.805 rows=2.83 loops=12)
                      Filter: ((sale_price > '0'::numeric) AND (product_id = p.id) AND ((status)::text = 'ACTIVE'::text))
                      Rows Removed by Filter: 9999997
                      Buffers: shared hit=5592 read=2784984
Planning:
  Buffers: shared hit=292 read=20
Planning Time: 15.857 ms
Execution Time: 53742.095 ms
```

### 3.1. Thứ Tự Thực Thi Thực Tế

1. `Parallel Seq Scan on catalog_product_skus s`

   PostgreSQL quét gần như toàn bộ bảng SKU để lấy SKU thỏa:

   ```sql
   s.status = 'ACTIVE'
   and s.sale_price > 0
   ```

2. `Parallel Hash`

   Từ SKU hợp lệ, PostgreSQL build hash table theo `s.product_id`.

3. `Parallel Seq Scan on catalog_products p`

   PostgreSQL quét gần như toàn bộ bảng product để lấy product thỏa:

   ```sql
   p.status = 'ACTIVE'
   and p.published_at is not null
   ```

4. `Parallel Hash Semi Join`

   Với mỗi product, PostgreSQL kiểm tra `p.id` có nằm trong hash SKU không. Đây là cách thực thi điều kiện `exists`.

5. `Sort`

   Sau khi có tập product hợp lệ, PostgreSQL sort theo:

   ```sql
   p.published_at desc, p.id desc
   ```

   Sort bị spill ra disk:

   ```text
   Sort Method: external merge
   ```

6. `Gather Merge`

   Gộp các stream đã sort từ worker thành một stream đúng thứ tự toàn cục.

7. `Nested Loop` + `Index Scan` category

   Lookup category cho product candidate bằng primary key, rồi filter `c.status = 'ACTIVE'`.

8. `Memoize` + `Index Scan` brand

   Lookup brand bằng primary key. `Memoize` cache theo `p.brand_id`.

9. `SubPlan 1`

   Với từng product trong kết quả, chạy subquery tính `min(sale_price)`. Do chưa có index phù hợp, mỗi lần lại `Seq Scan` toàn bộ `catalog_product_skus`.

10. `SubPlan 2`

   Với từng product trong kết quả, chạy subquery tính `min(compare_price)`. Nó cũng `Seq Scan` toàn bộ `catalog_product_skus`.

11. `Limit`

   Trả 12 row cuối cùng cho client.

### 3.2. Vấn Đề Chính

Các điểm chậm chính:

- Product không có index phù hợp với `order by published_at desc, id desc`, nên phải scan và sort hàng triệu product.
- Sort bị spill ra temp disk.
- Hai scalar subquery tính giá scan full bảng SKU nhiều lần.
- `SubPlan 1` scan khoảng 10 triệu SKU cho mỗi product, lặp 12 lần.
- `SubPlan 2` scan thêm khoảng 10 triệu SKU cho mỗi product, lặp 12 lần.

Tóm lại: database đang lọc toàn bộ catalog, sort toàn bộ, lấy 12 dòng, rồi với 12 dòng đó lại quét bảng SKU nhiều lần để tính giá.

## 4. Index Đã Thêm

### 4.1. Index cho newest product

```sql
create index concurrently if not exists idx_catalog_products_home_new_active
on catalog_products (published_at desc, id desc)
where status = 'ACTIVE' and published_at is not null;
```

Index này khớp với pattern:

```sql
where status = 'ACTIVE'
  and published_at is not null
order by published_at desc, id desc
limit ?
```

Vì là partial index, nó chỉ chứa product public và đã publish. PostgreSQL có thể đọc index theo đúng thứ tự newest và dừng sớm khi đủ `LIMIT`.

### 4.2. Index cho sellable SKU theo product

```sql
create index on catalog_product_skus (product_id, sale_price)
include (compare_price)
where status = 'ACTIVE' and sale_price > 0;
```

Index này khớp với các điều kiện SKU:

```sql
where s.product_id = p.id
  and s.status = 'ACTIVE'
  and s.sale_price > 0
```

Vì index là partial index, mọi row trong index đã thỏa:

```sql
status = 'ACTIVE'
and sale_price > 0
```

`product_id` giúp lookup SKU theo product hiện tại. `sale_price` giúp tìm `min(sale_price)` nhanh. `include (compare_price)` giúp PostgreSQL đọc `compare_price` ngay từ index và có thể dùng `Index Only Scan`.

## 5. Plan Sau Khi Thêm Index

Execution time: khoảng `1.230 ms`.

```text
Limit  (cost=1.72..101.53 rows=12 width=309) (actual time=0.793..0.961 rows=12.00 loops=1)
  Buffers: shared hit=255
  ->  Nested Loop Semi Join  (cost=1.72..19092205.85 rows=2295538 width=309) (actual time=0.792..0.959 rows=12.00 loops=1)
        Buffers: shared hit=255
        ->  Nested Loop Left Join  (cost=1.29..3236885.81 rows=2295538 width=245) (actual time=0.752..0.853 rows=12.00 loops=1)
              Buffers: shared hit=111
              ->  Nested Loop  (cost=0.85..2221666.39 rows=2295538 width=241) (actual time=0.034..0.092 rows=12.00 loops=1)
                    Buffers: shared hit=63
                    ->  Index Scan using idx_catalog_products_home_new_active on catalog_products p  (cost=0.43..1080561.28 rows=2419751 width=234) (actual time=0.021..0.035 rows=12.00 loops=1)
                          Index Searches: 1
                          Buffers: shared hit=15
                    ->  Index Scan using catalog_categories_pkey on catalog_categories c  (cost=0.42..0.47 rows=1 width=23) (actual time=0.004..0.004 rows=1.00 loops=12)
                          Index Cond: (id = p.category_id)
                          Filter: ((status)::text = 'ACTIVE'::text)
                          Index Searches: 12
                          Buffers: shared hit=48
              ->  Memoize  (cost=0.43..0.47 rows=1 width=20) (actual time=0.063..0.063 rows=1.00 loops=12)
                    Cache Key: p.brand_id
                    Cache Mode: logical
                    Hits: 0  Misses: 12  Evictions: 0  Overflows: 0  Memory Usage: 2kB
                    Buffers: shared hit=48
                    ->  Index Scan using catalog_brands_pkey on catalog_brands b  (cost=0.42..0.46 rows=1 width=20) (actual time=0.005..0.005 rows=1.00 loops=12)
                          Index Cond: (id = p.brand_id)
                          Filter: ((status)::text = 'ACTIVE'::text)
                          Index Searches: 12
                          Buffers: shared hit=48
        ->  Index Only Scan using catalog_product_skus_product_id_sale_price_compare_price_idx on catalog_product_skus s  (cost=0.43..0.73 rows=3 width=8) (actual time=0.004..0.004 rows=1.00 loops=12)
              Index Cond: (product_id = p.id)
              Heap Fetches: 0
              Index Searches: 12
              Buffers: shared hit=37
        SubPlan 2
          ->  Result  (cost=1.79..1.80 rows=1 width=32) (actual time=0.002..0.002 rows=1.00 loops=12)
                Buffers: shared hit=37
                InitPlan 1
                  ->  Limit  (cost=0.43..1.79 rows=1 width=6) (actual time=0.001..0.001 rows=1.00 loops=12)
                        Buffers: shared hit=37
                        ->  Index Only Scan using catalog_product_skus_product_id_sale_price_compare_price_idx on catalog_product_skus s2  (cost=0.43..4.49 rows=3 width=6) (actual time=0.001..0.001 rows=1.00 loops=12)
                              Index Cond: (product_id = p.id)
                              Heap Fetches: 0
                              Index Searches: 12
                              Buffers: shared hit=37
        SubPlan 3
          ->  Aggregate  (cost=4.50..4.50 rows=1 width=32) (actual time=0.002..0.002 rows=1.00 loops=12)
                Buffers: shared hit=70
                ->  Index Only Scan using catalog_product_skus_product_id_sale_price_compare_price_idx on catalog_product_skus s3  (cost=0.43..4.49 rows=3 width=6) (actual time=0.001..0.002 rows=2.83 loops=12)
                      Index Cond: (product_id = p.id)
                      Heap Fetches: 0
                      Index Searches: 12
                      Buffers: shared hit=70
Planning:
  Buffers: shared hit=32
Planning Time: 0.977 ms
Execution Time: 1.230 ms
```

### 5.1. Thứ Tự Thực Thi Sau Tối Ưu

1. `Index Scan using idx_catalog_products_home_new_active`

   PostgreSQL đọc product từ index đã sắp theo:

   ```sql
   published_at desc, id desc
   ```

   Vì query có `LIMIT 12`, PostgreSQL chỉ cần đọc một lượng rất nhỏ product candidate.

2. `Index Scan using catalog_categories_pkey`

   Với từng product candidate, PostgreSQL lookup category bằng primary key và kiểm tra `status = 'ACTIVE'`.

3. `Memoize` + `Index Scan using catalog_brands_pkey`

   PostgreSQL lookup brand bằng primary key. `Memoize` cache theo `brand_id`.

4. `Nested Loop Semi Join`

   PostgreSQL kiểm tra product có SKU bán được hay không.

5. `Index Only Scan` trên index SKU

   Thay vì scan toàn bộ SKU, PostgreSQL lookup trực tiếp theo:

   ```sql
   product_id = p.id
   ```

   Vì index là partial index, các row trong index đã là SKU `ACTIVE` và `sale_price > 0`.

6. `SubPlan` tính `min(sale_price)`

   PostgreSQL dùng index SKU để lấy giá nhỏ nhất theo product. Plan có `Limit` bên trong vì `min(sale_price)` có thể được tối ưu thành lấy row đầu tiên theo thứ tự `sale_price`.

7. `SubPlan` tính `min(compare_price)`

   PostgreSQL dùng `Index Only Scan` và đọc `compare_price` từ phần `include`.

8. `Limit`

   Trả 12 row cho client.

### 5.2. Dấu Hiệu Plan Đã Tốt

Các dấu hiệu quan trọng:

```text
Index Scan using idx_catalog_products_home_new_active
Index Only Scan using catalog_product_skus_product_id_sale_price_compare_price_idx
Heap Fetches: 0
Buffers: shared hit=255
Execution Time: 1.230 ms
```

Những thứ đã biến mất:

- Không còn `Parallel Seq Scan` trên hàng triệu product.
- Không còn `Parallel Seq Scan` SKU để build hash lớn.
- Không còn `Sort Method: external merge`.
- Không còn temp read/write.
- Không còn full scan SKU trong subquery.

## 6. So Sánh Trước Và Sau

| Tiêu chí | Trước index | Sau index |
| --- | --- | --- |
| Execution time | `53742.095 ms` | `1.230 ms` |
| Product access | Parallel seq scan | Index scan theo newest |
| SKU exists check | Parallel hash semi join từ full scan SKU | Index only scan theo product |
| Sort | External merge trên disk | Không cần sort lớn |
| Subquery min price | Seq scan SKU, `loops=12` | Index only scan, `loops=12` |
| Heap fetch SKU | Nhiều read từ heap | `Heap Fetches: 0` |
| Temp I/O | Có temp read/write lớn | Không có temp I/O |

Tóm lại:

- Trước index: lọc toàn bộ, sort toàn bộ, lấy 12, rồi scan SKU nhiều lần.
- Sau index: đọc product mới nhất theo index, lookup category/brand/SKU bằng index, rồi trả 12 row.

## 7. Ghi Chú Về `include`

Index product không cần vội `include (category_id, brand_id)`:

```sql
on catalog_products (published_at desc, id desc)
where status = 'ACTIVE' and published_at is not null
```

Lý do: query vẫn cần đọc heap `catalog_products` để lấy:

- `name`
- `slug`
- `short_description`
- `image_urls`

Khi đã phải đọc heap product, lấy thêm `category_id` và `brand_id` từ cùng row không đáng kể.

Ngược lại, index SKU có `include (compare_price)` là hợp lý, vì plan sau index cho thấy PostgreSQL dùng được `Index Only Scan`:

```text
Heap Fetches: 0
```

## 8. Ghi Chú Về Count Query

Spring Data `Page` sẽ chạy thêm `countQuery`.

Data query hiện đã rất nhanh nhờ `LIMIT` và index, nhưng count query có thể vẫn đắt nếu phải đếm toàn bộ product visible.

Nếu Home không cần tổng số chính xác, có thể cân nhắc sau:

- Đổi từ `Page` sang `Slice`.
- Query `pageSize + 1` để biết còn trang sau không.
- Bỏ count query cho Home highlights.

## 9. Count Query Của `findNewestVisibleProductsQuery`

### 9.1. Count Query Đang Chạy

Khi repository trả về `Page`, Spring Data JPA chạy thêm count query sau:

```sql
EXPLAIN (ANALYZE, BUFFERS)
select count(p.id)
from catalog_products p
join catalog_categories c
  on c.id = p.category_id
 and c.status = 'ACTIVE'
where p.status = 'ACTIVE'
  and p.published_at is not null
  and exists (
      select 1
      from catalog_product_skus s
      where s.product_id = p.id
        and s.status = 'ACTIVE'
        and s.sale_price > 0
  );
```

Plan thực tế đo được:

```text
Finalize Aggregate  (actual time=3071.173..3197.061 rows=1.00 loops=1)
  Buffers: shared hit=8744370 read=308226, temp read=53622 written=54296
  ->  Gather
        Workers Planned: 2
        Workers Launched: 2
        ->  Partial Aggregate
              ->  Parallel Hash Semi Join  (actual time=2533.787..3010.883 rows=900000.00 loops=3)
                    Hash Cond: (p.id = s.product_id)
                    ->  Parallel Hash Join  (actual time=815.646..1051.919 rows=900000.00 loops=3)
                          Hash Cond: (p.category_id = c.id)
                          ->  Parallel Seq Scan on catalog_products p
                                Filter: ((published_at IS NOT NULL) AND ((status)::text = 'ACTIVE'::text))
                                Rows Removed by Filter: 100000
                          ->  Parallel Hash
                                ->  Parallel Seq Scan on catalog_categories c
                                      Filter: ((status)::text = 'ACTIVE'::text)
                    ->  Parallel Hash
                          ->  Parallel Index Only Scan using catalog_product_skus_product_id_sale_price_compare_price_idx on catalog_product_skus s
                                Heap Fetches: 0
Planning Time: 1.739 ms
Execution Time: 3197.188 ms
```

### 9.2. Vì Sao Count Query Vẫn Chậm?

Data pattern hiện tại:

```text
catalog_products      = 3,000,000 rows
catalog_product_skus  = 10,000,000 rows
catalog_categories    = 1,000,000 rows

Product ACTIVE + published_at not null = 2,700,000 rows
Category ACTIVE                        = 950,000 rows
SKU ACTIVE + sale_price > 0            = 8,756,757 rows
```

Count query không có `LIMIT`. Nó phải đếm chính xác toàn bộ product visible.

Điều kiện:

```sql
p.status = 'ACTIVE'
and p.published_at is not null
and c.status = 'ACTIVE'
and exists sellable sku
```

match phần lớn data. Khi predicate match quá nhiều row, index không còn tác dụng "nhảy tới tập nhỏ". PostgreSQL vẫn phải xử lý hàng triệu row để đưa ra exact count.

Dấu hiệu quan trọng trong plan:

```text
Parallel Seq Scan on catalog_products
Parallel Seq Scan on catalog_categories
Parallel Index Only Scan on catalog_product_skus
temp read=53622 written=54296
Execution Time=3197.188 ms
```

Nghĩa là database đang:

1. Quét 2.7M product visible.
2. Quét 950k category active.
3. Quét 8.75M SKU sellable từ index.
4. Build hash join/semi join lớn.
5. Bị spill ra temp file do hash table lớn.

### 9.3. So Sánh Các Hướng Đánh Index Cho Count Query

#### 9.3.1. Product visible theo `(category_id, id)`

```sql
create index concurrently idx_catalog_products_visible_count_category_id
on catalog_products (category_id, id)
where status = 'ACTIVE'
  and published_at is not null;
```

Mục tiêu:

- `category_id` phục vụ join `c.id = p.category_id`.
- `id` phục vụ `count(p.id)` và `s.product_id = p.id`.

Kỳ vọng:

- Có thể giúp PostgreSQL đọc product visible từ partial index mỏng hơn heap.
- Có thể giảm heap read nếu planner chọn index-only/index scan.

Hạn chế:

- Product visible là 2.7M/3M rows, tức khoảng 90% bảng.
- Vì match quá rộng, PostgreSQL có thể vẫn chọn Parallel Seq Scan vì scan gần cả bảng rẻ hơn đi qua index.
- Khó tạo cải thiện lớn cho exact count.

#### 9.3.2. Product visible theo `(id, category_id)`

```sql
create index concurrently idx_catalog_products_visible_count_id_category
on catalog_products (id, category_id)
where status = 'ACTIVE'
  and published_at is not null;
```

Mục tiêu:

- `id` đặt trước để phục vụ semi join với SKU: `s.product_id = p.id`.
- `category_id` vẫn có trong index để join sang category.

Kỳ vọng:

- Hợp hơn nếu planner chọn hướng đi từ SKU/product id sang product.
- Có thể giúp index-only hơn cho phần product.

Hạn chế:

- SKU sellable cũng rất rộng, khoảng 8.75M/10M rows.
- Nếu cả hai phía join đều rộng, index không giúp giảm cardinality đủ mạnh.

#### 9.3.3. Category active theo `(id)`

```sql
create index concurrently idx_catalog_categories_active_id
on catalog_categories (id)
where status = 'ACTIVE';
```

Mục tiêu:

- Chỉ chứa category `ACTIVE`.
- Cung cấp `id` để join với product.

Kỳ vọng:

- Có thể giảm đọc heap category nếu PostgreSQL chọn scan partial index.

Hạn chế:

- Category active là 950k/1M rows, tức khoảng 95% bảng.
- Partial index gần như to bằng bảng gốc.
- Plan hiện tại scan category không phải bottleneck chính.

#### 9.3.4. SKU sellable theo `(product_id)`

```sql
create index concurrently idx_catalog_product_skus_sellable_product_id
on catalog_product_skus (product_id)
where status = 'ACTIVE'
  and sale_price > 0;
```

Remove:

```sql
drop index concurrently if exists idx_catalog_product_skus_sellable_product_id;
```

Mục tiêu:

- Count query chỉ cần biết product có SKU bán được hay không.
- Count query không cần `sale_price` trong key và không cần `compare_price`.

So với index đang có:

```sql
catalog_product_skus_product_id_sale_price_compare_price_idx
on catalog_product_skus (product_id, sale_price)
include (compare_price)
where status = 'ACTIVE' and sale_price > 0
```

index `(product_id)` mỏng hơn cho riêng `exists/count`.

Kỳ vọng:

- Có thể giảm I/O khi scan SKU sellable.
- Có thể giảm kích thước index được đọc cho count query.

Hạn chế:

- Vẫn phải đọc khoảng 8.75M SKU sellable.
- Vẫn phải build hash/semi join lớn.
- Có thể nhanh hơn index hiện tại, nhưng khó biến query 3 giây thành vài mili giây.

### 9.4. Bảng So Sánh Các Index

| Index | Mục tiêu | Khả năng giúp plan hiện tại | Nhận xét |
| --- | --- | --- | --- |
| `catalog_products (category_id, id) where status='ACTIVE' and published_at is not null` | Join product -> category và lấy `p.id` cho SKU exists | Thấp đến trung bình | Điều kiện product match khoảng 90% bảng, planner có thể vẫn chọn seq scan. |
| `catalog_products (id, category_id) where status='ACTIVE' and published_at is not null` | Semi join product -> SKU theo `p.id`, vẫn có `category_id` để join category | Thấp đến trung bình | Hợp hơn nếu planner đi theo hướng SKU/product id, nhưng SKU sellable cũng quá rộng. |
| `catalog_categories (id) where status='ACTIVE'` | Tránh scan category inactive | Thấp | Category active khoảng 95% bảng, index gần như không selective. |
| `catalog_product_skus (product_id) where status='ACTIVE' and sale_price > 0` | Index mỏng cho `exists`/count SKU sellable | Trung bình | Đáng test nhất trong nhóm index count, nhưng vẫn phải scan hàng triệu SKU. |

Xếp hạng đáng test:

1. SKU sellable mong:

   ```sql
   create index concurrently idx_catalog_product_skus_sellable_product_id
   on catalog_product_skus (product_id)
   where status = 'ACTIVE'
     and sale_price > 0;
   ```

2. Product visible theo `id, category_id`:

   ```sql
   create index concurrently idx_catalog_products_visible_count_id_category
   on catalog_products (id, category_id)
   where status = 'ACTIVE'
     and published_at is not null;
   ```

3. Product visible theo `category_id, id`:

   ```sql
   create index concurrently idx_catalog_products_visible_count_category_id
   on catalog_products (category_id, id)
   where status = 'ACTIVE'
     and published_at is not null;
   ```

4. Category active:

   ```sql
   create index concurrently idx_catalog_categories_active_id
   on catalog_categories (id)
   where status = 'ACTIVE';
   ```

Sau mỗi lần tạo/drop index nên refresh statistics:

```sql
analyze catalog_products;
analyze catalog_categories;
analyze catalog_product_skus;
```

### 9.5. Khi Data Quá To Và Predicate Quá Rộng Thì Không Nên Cố Đánh Index

Trong case này, index không phải giải pháp chính vì:

- `p.status = 'ACTIVE' and p.published_at is not null` match khoảng 90% product.
- `c.status = 'ACTIVE'` match khoảng 95% category.
- `s.status = 'ACTIVE' and s.sale_price > 0` match khoảng 87.5% SKU.
- Exact count phải xử lý hàng triệu row dù có index.

Nói cách khác: index tốt khi điều kiện lọc đủ selective. Ở đây điều kiện lọc không selective, nên database vẫn phải đọc phần lớn data.

#### 9.5.1. Giải Pháp Tốt Nhất Cho Màn Home: Bỏ Exact Count

Màn Home thường chỉ cần danh sách item đầu tiên:

```text
new products: top 20
featured products: top 20
featured categories: top 20
```

Home không nhất thiết cần:

```text
totalElements = exact visible product count
totalPages = exact visible product pages
```

Nếu vẫn dùng `Page`, Spring Data JPA bắt buộc chạy `countQuery`. Với data lớn, `countQuery` có thể chậm hơn data query rất nhiều.

Hướng tốt hơn:

- Đổi response của Home aggregate sang `List`.
- Hoặc đổi sang `Slice`.
- Hoặc tạo DTO riêng cho Home section, không expose `Page`.

Ví dụ logic:

```text
query pageSize + 1 rows
if result size > pageSize -> hasNext = true
drop row thua
return content + hasNext
```

Lợi ích:

- Không chạy exact count.
- Data query đang chạy rất nhanh nhờ index `idx_catalog_products_home_new_active`.
- Phù hợp với UX Home vì Home không cần biết tổng số product visible chính xác.

#### 9.5.2. Giữ Endpoint Riêng Dùng `Page`, Aggregate Dùng Content-Only

Không nhất thiết phải bỏ `Page` ở mọi nơi.

Có thể tách:

- `/api/v1/home/highlights`: dùng content-only, không exact count.
- `/api/v1/home/highlights/new-products`: nếu UI cần pagination đầy đủ thì vẫn dùng `Page`.
- `/api/v1/home/highlights/featured-products`: tương tự.

Cách này giữ được API chi tiết cho màn listing, nhưng không bắt endpoint tổng hợp Home phải trả giá 3-7 giây cho metadata pagination.

#### 9.5.3. Dùng Cached Count Hoặc Summary Table

Nếu business thật sự cần total count, nên tính sẵn:

```sql
create table catalog_product_visibility_summary (
    summary_key varchar(64) primary key,
    total_count bigint not null,
    updated_at timestamp not null
);
```

Hoac materialized view:

```sql
create materialized view mv_catalog_visible_product_count as
select count(p.id) as total_count
from catalog_products p
join catalog_categories c
  on c.id = p.category_id
 and c.status = 'ACTIVE'
where p.status = 'ACTIVE'
  and p.published_at is not null
  and exists (
      select 1
      from catalog_product_skus s
      where s.product_id = p.id
        and s.status = 'ACTIVE'
        and s.sale_price > 0
  );
```

Sau đó refresh theo job:

```sql
refresh materialized view mv_catalog_visible_product_count;
```

Hướng này phù hợp khi:

- Count không cần realtime từng mili giây.
- Được phép chậm vài giây/phút.
- UI cần con số để hiển thị.

#### 9.5.4. Denormalize Visibility

Tạo bảng trạng thái visibility:

```sql
create table catalog_product_visibility (
    product_id bigint primary key,
    visible boolean not null,
    featured boolean not null,
    published_at timestamp,
    updated_at timestamp not null
);
```

Khi product/category/SKU thay đổi, update lại visibility cho product liên quan.

Count khi đó rất nhẹ:

```sql
select count(*)
from catalog_product_visibility
where visible = true;
```

Và query Home có thể dùng:

```sql
create index concurrently idx_catalog_product_visibility_new
on catalog_product_visibility (published_at desc, product_id desc)
where visible = true;
```

Trade-off:

- Query đọc nhanh hơn rất nhiều.
- Logic ghi phức tạp hơn vì phải duy trì bảng visibility đúng khi product/category/SKU thay đổi.

#### 9.5.5. Tăng `work_mem` Chỉ Là Giải Pháp Phụ

Plan hiện tại bị spill:

```text
temp read=53622 written=54296
```

Có thể test:

```sql
begin;
set local work_mem = '512MB';

EXPLAIN (ANALYZE, BUFFERS)
select count(p.id)
from catalog_products p
join catalog_categories c
  on c.id = p.category_id
 and c.status = 'ACTIVE'
where p.status = 'ACTIVE'
  and p.published_at is not null
  and exists (
      select 1
      from catalog_product_skus s
      where s.product_id = p.id
        and s.status = 'ACTIVE'
        and s.sale_price > 0
  );

rollback;
```

Nếu temp I/O giảm hoặc biến mất, query có thể nhanh hơn. Tuy nhiên đây chỉ là tuning, không phải fix gốc. Database vẫn phải xử lý hàng triệu row để exact count.

### 9.6. Kết Luận Cho Count Query

Với data pattern hiện tại, index có thể giảm một phần I/O nhưng không thể tối ưu triệt để exact count.

Nguyên nhân gốc:

```text
count query match qua nhieu du lieu
```

Khuyến nghị:

1. Giữ index `idx_catalog_products_home_new_active` và SKU index hiện tại cho data query vì data query đã rất nhanh.
2. Không cố thêm nhiều index chỉ để làm exact count nhanh hơn, trừ khi đo thấy lợi ích rõ ràng.
3. Với endpoint tổng hợp Home, nên bỏ exact count bằng `List` hoặc `Slice`.
4. Nếu buộc phải có total count, dùng cached count/materialized view/summary table thay vì count realtime trên 3M product và 10M SKU.

### 9.7. Kết Quả Test Thực Tế: Không Có Index Vs Thêm 3 Index Product/Category

#### 9.7.1. Baseline Khi Không Có Index Count Riêng

Plan:

```text
Finalize Aggregate  (cost=700917.62..700917.63 rows=1 width=8) (actual time=5946.973..6087.326 rows=1.00 loops=1)
  Buffers: shared hit=7422 read=490212, temp read=53621 written=54236
  ->  Gather  (cost=700917.41..700917.62 rows=2 width=8) (actual time=5945.888..6087.319 rows=3.00 loops=1)
        Workers Planned: 2
        Workers Launched: 2
        ->  Partial Aggregate  (cost=699917.41..699917.42 rows=1 width=8) (actual time=5924.872..5925.246 rows=1.00 loops=3)
              ->  Parallel Hash Semi Join  (cost=380018.50..697526.22 rows=956474 width=8) (actual time=5420.696..5882.421 rows=900000.00 loops=3)
                    Hash Cond: (p.id = s.product_id)
                    ->  Parallel Hash Join  (cost=25028.31..306442.92 rows=956474 width=8) (actual time=2782.904..3047.785 rows=900000.00 loops=3)
                          Hash Cond: (p.category_id = c.id)
                          ->  Parallel Seq Scan on catalog_products p  (actual time=0.842..2448.409 rows=900000.00 loops=3)
                                Filter: ((published_at IS NOT NULL) AND ((status)::text = 'ACTIVE'::text))
                                Rows Removed by Filter: 100000
                          ->  Parallel Hash
                                ->  Parallel Seq Scan on catalog_categories c  (actual time=0.422..130.443 rows=316666.67 loops=3)
                                      Filter: ((status)::text = 'ACTIVE'::text)
                    ->  Parallel Hash
                          ->  Parallel Seq Scan on catalog_product_skus s  (actual time=0.646..1770.965 rows=2918919.00 loops=3)
                                Filter: ((sale_price > '0'::numeric) AND ((status)::text = 'ACTIVE'::text))
Planning Time: 1.448 ms
Execution Time: 6087.497 ms
```

Tóm tắt baseline:

| Thành phần | Plan | Nhận xét |
| --- | --- | --- |
| Product | `Parallel Seq Scan on catalog_products` | Quét 3M product, lọc ra 2.7M active published. |
| Category | `Parallel Seq Scan on catalog_categories` | Quét 1M category, lọc ra 950k active. |
| SKU | `Parallel Seq Scan on catalog_product_skus` | Quét 10M SKU, lọc ra 8.75M sellable. |
| Join | `Parallel Hash Join` + `Parallel Hash Semi Join` | Hash lớn và bị spill temp. |
| Temp I/O | `temp read=53621 written=54236` | Hash table lớn vượt memory. |
| Time | `6087.497 ms` | Exact count mất khoảng 6.1s. |

#### 9.7.2. Thêm 3 Index Product/Category

Index đã thêm:

```sql
create index concurrently idx_catalog_products_visible_count_category_id
on catalog_products (category_id, id)
where status = 'ACTIVE'
  and published_at is not null;

create index concurrently idx_catalog_products_visible_count_id_category
on catalog_products (id, category_id)
where status = 'ACTIVE'
  and published_at is not null;

create index concurrently idx_catalog_categories_active_id
on catalog_categories (id)
where status = 'ACTIVE';
```

Plan sau khi thêm 3 index:

```text
Finalize Aggregate  (cost=493068.68..493068.69 rows=1 width=8) (actual time=2127.757..2266.928 rows=1.00 loops=1)
  Buffers: shared hit=24349 read=245600, temp read=53612 written=54156
  ->  Gather  (cost=493068.47..493068.68 rows=2 width=8) (actual time=2126.921..2266.920 rows=3.00 loops=1)
        Workers Planned: 2
        Workers Launched: 2
        ->  Partial Aggregate  (cost=492068.47..492068.48 rows=1 width=8) (actual time=2104.123..2104.449 rows=1.00 loops=3)
              ->  Parallel Hash Semi Join  (cost=380018.93..489677.28 rows=956474 width=8) (actual time=1662.779..2068.943 rows=900000.00 loops=3)
                    Hash Cond: (p.id = s.product_id)
                    ->  Parallel Hash Join  (cost=25028.74..98593.98 rows=956474 width=8) (actual time=261.540..495.653 rows=900000.00 loops=3)
                          Hash Cond: (p.category_id = c.id)
                          ->  Parallel Index Only Scan using idx_catalog_products_visible_count_id_category on catalog_products p  (actual time=0.080..64.874 rows=900000.00 loops=3)
                                Heap Fetches: 7
                          ->  Parallel Hash
                                ->  Parallel Seq Scan on catalog_categories c  (actual time=0.142..37.040 rows=316666.67 loops=3)
                                      Filter: ((status)::text = 'ACTIVE'::text)
                    ->  Parallel Hash
                          ->  Parallel Seq Scan on catalog_product_skus s  (actual time=0.169..605.795 rows=2918919.00 loops=3)
                                Filter: ((sale_price > '0'::numeric) AND ((status)::text = 'ACTIVE'::text))
Planning Time: 1.283 ms
Execution Time: 2267.030 ms
```

Tóm tắt sau khi thêm 3 index:

| Thành phần | Plan | Nhận xét |
| --- | --- | --- |
| Product | `Parallel Index Only Scan using idx_catalog_products_visible_count_id_category` | Đây là index có tác dụng rõ nhất. Product scan giảm từ khoảng `2448 ms` xuống `65 ms` mỗi worker. |
| Category | Vẫn `Parallel Seq Scan on catalog_categories` | Index `idx_catalog_categories_active_id` không được dùng vì `ACTIVE` match 95% bảng. |
| SKU | Vẫn `Parallel Seq Scan on catalog_product_skus` | Chưa có SKU count index riêng, nên vẫn scan 10M SKU. |
| Temp I/O | Gần như không đổi: `temp read=53612 written=54156` | Bottleneck hash lớn vẫn còn. |
| Time | `6087.497 ms` -> `2267.030 ms` | Giảm khoảng 62.8%, nhưng vẫn còn hơn 2s vì SKU/category/hash vẫn rất lớn. |

#### 9.7.3. So Sánh Baseline Và Sau 3 Index

| Tiêu chí | Không có index count riêng | Thêm 3 index product/category |
| --- | --- | --- |
| Execution time | `6087.497 ms` | `2267.030 ms` |
| Product access | `Parallel Seq Scan` | `Parallel Index Only Scan` |
| Product scan time | Khoảng `2448 ms` mỗi worker | Khoảng `65 ms` mỗi worker |
| Category access | `Parallel Seq Scan` | Vẫn `Parallel Seq Scan` |
| SKU access | `Parallel Seq Scan` | Vẫn `Parallel Seq Scan` |
| Temp I/O | `temp read=53621 written=54236` | `temp read=53612 written=54156` |
| Heap fetch product | Đọc heap product lớn | `Heap Fetches: 7` |

Nhận xét:

- Index `idx_catalog_products_visible_count_id_category` có tác dụng thực tế.
- Index `idx_catalog_products_visible_count_category_id` không được planner chọn trong plan này.
- Index `idx_catalog_categories_active_id` không được planner chọn vì `status='ACTIVE'` quá rộng.
- Tổng time giảm mạnh từ 6.1s xuống 2.27s, nhưng vẫn không thể nhanh như data query vì exact count vẫn phải xử lý hàng triệu row.

#### 9.7.4. Nên Giữ Index Nào?

Từ plan trên, index nên giữ nếu chấp nhận tối ưu cho exact count:

```sql
create index concurrently idx_catalog_products_visible_count_id_category
on catalog_products (id, category_id)
where status = 'ACTIVE'
  and published_at is not null;
```

Lý do:

- Planner đã chọn index này.
- Scan product chuyển từ heap scan sang index-only scan.
- `Heap Fetches: 7`, gần như không cần đọc heap product.
- Nó cung cấp cả `p.id` cho semi join SKU và `p.category_id` cho join category.

Hai index nên cân nhắc drop nếu chỉ phục vụ count query này và không có query khác dùng:

```sql
drop index concurrently if exists idx_catalog_products_visible_count_category_id;
drop index concurrently if exists idx_catalog_categories_active_id;
```

Lý do:

- Không xuất hiện trong plan.
- Thêm chi phí storage và write maintenance.
- `idx_catalog_categories_active_id` kém selective vì category active chiếm 95% bảng.

#### 9.7.5. Bước Test Tiếp Theo Nếu Vẫn Muốn Tối Ưu Bằng Index

Hiện tại SKU vẫn là phần lớn:

```text
Parallel Seq Scan on catalog_product_skus s
actual time=0.169..605.795 rows=2918919.00 loops=3
```

Nên test tiếp SKU sellable index mỏng:

```sql
create index concurrently idx_catalog_product_skus_sellable_product_id
on catalog_product_skus (product_id)
where status = 'ACTIVE'
  and sale_price > 0;

analyze catalog_product_skus;
```

Sau đó chạy lại:

```sql
EXPLAIN (ANALYZE, BUFFERS)
select count(p.id)
from catalog_products p
join catalog_categories c
  on c.id = p.category_id
 and c.status = 'ACTIVE'
where p.status = 'ACTIVE'
  and p.published_at is not null
  and exists (
      select 1
      from catalog_product_skus s
      where s.product_id = p.id
        and s.status = 'ACTIVE'
        and s.sale_price > 0
  );
```

Kỳ vọng:

- Nếu planner dùng `idx_catalog_product_skus_sellable_product_id`, SKU scan có thể nhẹ hơn.
- Nhưng vẫn phải đọc khoảng 8.75M SKU sellable, nên không nên kỳ vọng về mức mili giây.

Remove nếu không có lợi ích rõ:

```sql
drop index concurrently if exists idx_catalog_product_skus_sellable_product_id;
```

#### 9.7.6. Kết Luận Sau Kết Quả Test Mới

Kết quả mới cho thấy index vẫn có lợi ích, nhưng lợi ích nằm ở việc giảm product scan:

```text
6087 ms -> 2267 ms
```

Tuy nhiên exact count vẫn còn chậm vì:

- Category active quá rộng nên index category không có tác dụng.
- SKU sellable quá rộng nên vẫn phải scan và hash hàng triệu row.
- Hash join/semi join vẫn spill temp gần như bằng baseline.

Vì vậy kết luận cũ vẫn giữ nguyên:

- Nếu cần exact count realtime, giữ index product `(id, category_id)` và test thêm SKU `(product_id)`.
- Nếu đây là Home aggregate, cách hiệu quả nhất vẫn là không chạy exact count, dùng `List`/`Slice`/cached count thay vì thêm quá nhiều index.
