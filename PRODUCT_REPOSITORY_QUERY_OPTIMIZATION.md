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

