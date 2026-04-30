# ProductListingRepository Query Optimization Notes

Tài liệu này ghi lại phân tích và tối ưu query trong `ProductListingRepository`.

Hiện tại tài liệu phân tích:

- `search`

## 1. `search`

### 1.1. Mục đích nghiệp vụ

`search` lấy danh sách product cho màn hình product listing/search.

Query trả về các thông tin chính:

- Product: `id`, `name`, `slug`, `short_description`, `image_urls`, `published_at`.
- Tên category.
- Tên brand nếu brand đang `ACTIVE`.
- Giá nhỏ nhất của SKU đang bán được.

Điều kiện visibility:

- Product phải `ACTIVE`.
- Product phải đã publish: `published_at is not null`.
- Category phải `ACTIVE`.
- Brand là thông tin bổ sung, chỉ join khi brand `ACTIVE`.
- Product phải có SKU `ACTIVE`, `sale_price > 0`, và `min_sale_price` nằm trong filter giá nếu có.

Điều kiện filter trong execution plan này:

```sql
p.category_id in (1)
and p.brand_id in (1)
and sku_price.min_sale_price >= 0
and sku_price.min_sale_price <= 200
and (
    lower(p.name) like '%product 1%'
    or lower(b.name) like '%product 1%'
    or exists (
        select 1
        from catalog_product_skus keyword_sku
        where keyword_sku.product_id = p.id
          and keyword_sku.status = 'ACTIVE'
          and lower(keyword_sku.sku_code) like '%product 1%'
    )
)
```

Sort:

```sql
order by p.published_at desc, p.id desc
limit 20 offset 0
```

`p.id desc` là tie-breaker để thứ tự ổn định khi nhiều product có cùng `published_at`.

### 1.2. Điểm cần lưu ý về thứ tự cột trong index

Với query có filter:

```sql
category_id = 1
and brand_id = 1
order by published_at desc, id desc
```

index phù hợp là:

```sql
(category_id, brand_id, published_at desc, id desc)
```

Lý do:

- `category_id` và `brand_id` là equality filter, giúp PostgreSQL seek thẳng vào vùng index của đúng cặp `(category_id, brand_id)`.
- Bên trong vùng index đó, row đã được sắp xếp theo `published_at desc, id desc`.
- Khi `category_id in (1)` và `brand_id in (1)` chỉ có một nhóm giá trị, order trong nhóm này cũng chính là global order của tập kết quả sau filter.

Nếu query có nhiều category/brand:

```sql
category_id in (1, 2, 3)
and brand_id in (1, 2)
```

thì mỗi cặp `(category_id, brand_id)` có order riêng. Khi đó PostgreSQL có thể vẫn cần sort/merge để tạo global order theo `published_at desc, id desc`.

Công thức thực dụng cho B-tree index:

```text
WHERE equality columns -> WHERE range columns -> ORDER BY columns -> INCLUDE select-only columns
```

## 2. Query PostgreSQL Dùng Để Explain

```sql
EXPLAIN (ANALYZE, BUFFERS)
select
    p.id as "productId",
    p.name as "productName",
    p.slug as "slug",
    p.short_description as "shortDescription",
    b.name as "brandName",
    c.name as "categoryName",
    p.image_urls as "imageUrls",
    sku_price.min_sale_price as "minSalePrice",
    sku_price.min_compare_price as "minComparePrice",
    p.published_at as "publishedAt"
from catalog_products p
join catalog_categories c on c.id = p.category_id
left join catalog_brands b
    on b.id = p.brand_id
   and b.status = 'ACTIVE'
left join lateral (
    select
        min(s.sale_price) as min_sale_price,
        min(s.compare_price) as min_compare_price
    from catalog_product_skus s
    where s.product_id = p.id
      and s.status = 'ACTIVE'
      and s.sale_price > 0
) sku_price on true
where p.status = 'ACTIVE'
  and p.published_at is not null
  and c.status = 'ACTIVE'
  and sku_price.min_sale_price is not null
  and (
      lower(p.name) like '%product 1%'
      or lower(b.name) like '%product 1%'
      or exists (
          select 1
          from catalog_product_skus keyword_sku
          where keyword_sku.product_id = p.id
            and keyword_sku.status = 'ACTIVE'
            and lower(keyword_sku.sku_code) like '%product 1%'
      )
  )
  and p.category_id in (1)
  and p.brand_id in (1)
  and sku_price.min_sale_price >= 0
  and sku_price.min_sale_price <= 200
order by
  p.published_at desc,
  p.id desc
limit 20 offset 0;
```

## 3. Plan Trước Khi Thêm Index

Execution time: khoảng `10738.962 ms`.

```text
Limit  (cost=682175.90..1114740.11 rows=1 width=309) (actual time=7179.163..10737.965 rows=1.00 loops=1)
  Buffers: shared hit=8772 read=940739
  ->  Nested Loop  (cost=682175.90..1114740.11 rows=1 width=309) (actual time=7179.161..10737.962 rows=1.00 loops=1)
        Buffers: shared hit=8772 read=940739
        ->  Nested Loop Left Join  (cost=274627.88..707192.07 rows=1 width=245) (actual time=3525.213..5143.049 rows=2.00 loops=1)
              Filter: ((lower((p.name)::text) ~~ '%product 1%'::text) OR (lower((b.name)::text) ~~ '%product 1%'::text) OR (ANY (p.id = (hashed SubPlan 2).col1)))
              Rows Removed by Filter: 1
              Buffers: shared hit=7738 read=476677
              ->  Nested Loop  (cost=274627.46..274635.61 rows=1 width=241) (actual time=3524.943..3525.445 rows=3.00 loops=1)
                    Buffers: shared hit=7256 read=244599
                    ->  Gather Merge  (cost=274627.03..274627.15 rows=1 width=234) (actual time=3524.347..3524.394 rows=3.00 loops=1)
                          Workers Planned: 2
                          Workers Launched: 2
                          Buffers: shared hit=7241 read=244599
                          ->  Sort  (cost=273627.01..273627.02 rows=1 width=234) (actual time=3478.707..3478.708 rows=1.00 loops=3)
                                Sort Key: p.published_at DESC, p.id DESC
                                Sort Method: quicksort  Memory: 25kB
                                Buffers: shared hit=7241 read=244599
                                Worker 0:  Sort Method: quicksort  Memory: 25kB
                                Worker 1:  Sort Method: quicksort  Memory: 25kB
                                ->  Parallel Seq Scan on catalog_products p  (cost=0.00..273627.00 rows=1 width=234) (actual time=2627.012..3478.609 rows=1.00 loops=3)
                                      Filter: ((published_at IS NOT NULL) AND (category_id = 1) AND (brand_id = 1) AND ((status)::text = 'ACTIVE'::text))
                                      Rows Removed by Filter: 999999
                                      Buffers: shared hit=7153 read=244599
                    ->  Index Scan using catalog_categories_pkey on catalog_categories c  (cost=0.42..8.45 rows=1 width=23) (actual time=0.339..0.344 rows=1.00 loops=3)
                          Index Cond: (id = 1)
                          Filter: ((status)::text = 'ACTIVE'::text)
                          Index Searches: 3
                          Buffers: shared hit=15
              ->  Index Scan using catalog_brands_pkey on catalog_brands b  (cost=0.42..8.44 rows=1 width=20) (actual time=0.093..0.093 rows=1.00 loops=3)
                    Index Cond: (id = 1)
                    Filter: ((status)::text = 'ACTIVE'::text)
                    Index Searches: 3
                    Buffers: shared hit=12
              SubPlan 2
                ->  Gather  (cost=1000.00..306554.77 rows=901 width=8) (actual time=1617.218..1617.237 rows=0.00 loops=1)
                      Workers Planned: 2
                      Workers Launched: 2
                      Buffers: shared hit=470 read=232078
                      ->  Parallel Seq Scan on catalog_product_skus keyword_sku  (cost=0.00..305464.67 rows=375 width=8) (actual time=1576.612..1576.612 rows=0.00 loops=3)
                            Filter: (((status)::text = 'ACTIVE'::text) AND (lower((sku_code)::text) ~~ '%product 1%'::text))
                            Rows Removed by Filter: 3333333
                            Buffers: shared hit=470 read=232078
        ->  Aggregate  (cost=407548.02..407548.03 rows=1 width=64) (actual time=2797.449..2797.449 rows=0.50 loops=2)
              Filter: ((min(s.sale_price) IS NOT NULL) AND (min(s.sale_price) >= '0'::numeric) AND (min(s.sale_price) <= '200'::numeric))
              Rows Removed by Filter: 0
              Buffers: shared hit=1034 read=464062
              ->  Seq Scan on catalog_product_skus s  (cost=0.00..407548.00 rows=3 width=12) (actual time=104.178..2797.411 rows=3.50 loops=2)
                    Filter: ((sale_price > '0'::numeric) AND (product_id = p.id) AND ((status)::text = 'ACTIVE'::text))
                    Rows Removed by Filter: 9999996
                    Buffers: shared hit=1034 read=464062
Planning:
  Buffers: shared hit=314
Planning Time: 3.580 ms
Execution Time: 10738.962 ms
```

### 3.1. Thứ Tự Thực Thi Thực Tế

1. `Parallel Seq Scan on catalog_products p`

   PostgreSQL scan gần như toàn bộ `catalog_products` để tìm product thỏa:

   ```sql
   p.status = 'ACTIVE'
   and p.published_at is not null
   and p.category_id = 1
   and p.brand_id = 1
   ```

   Mỗi worker loại khoảng `999999` rows. Tổng thời gian product scan và sort khoảng `3.5s`.

2. `Sort` + `Gather Merge`

   Sau khi scan product, PostgreSQL sort theo:

   ```sql
   p.published_at desc, p.id desc
   ```

   Sort không tốn bộ nhớ lớn trong case này vì chỉ có vài row match, nhưng chi phí lớn nằm ở việc scan bảng product trước đó.

3. `SubPlan 2` cho keyword SKU

   Điều kiện:

   ```sql
   lower(keyword_sku.sku_code) like '%product 1%'
   ```

   không dùng được B-tree index thông thường. PostgreSQL phải `Parallel Seq Scan` gần 10 triệu SKU và loại `3333333` rows mỗi worker.

4. `LATERAL sku_price`

   Với mỗi product còn lại sau keyword filter, PostgreSQL tính:

   ```sql
   min(s.sale_price),
   min(s.compare_price)
   ```

   Do chưa có index phù hợp trên database tại thời điểm explain, mỗi lần lateral lại `Seq Scan` toàn bộ `catalog_product_skus`.

### 3.2. Vấn Đề Chính

Các điểm chậm chính:

- Product listing filter không có composite index khớp `category_id`, `brand_id`, `status`, `published_at`.
- SKU keyword search dùng `like '%...%'`, cần trigram index thay vì B-tree.
- SKU min price theo từng `product_id` cần partial index theo SKU bán được.
- Query có `OR` qua product name, brand name và SKU code, làm planner khó chọn một access path duy nhất.

## 4. Index Đã Thêm

### 4.1. Product listing filter + newest sort

```sql
create index concurrently if not exists idx_catalog_products_listing_cat_brand_new_active
on catalog_products (category_id, brand_id, published_at desc, id desc)
where status = 'ACTIVE'
  and published_at is not null;
```

Lý do phù hợp:

- `where status = 'ACTIVE' and published_at is not null` làm partial index nhỏ hơn index full table.
- `category_id`, `brand_id` nằm đầu index vì là equality filter trong query.
- `published_at desc, id desc` khớp `order by` khi query chỉ có một category và một brand.
- Không `include` các cột lớn như `image_urls` để tránh làm index phình to khi lợi ích covering không rõ ràng.

### 4.2. SKU min price theo product

```sql
create index concurrently if not exists idx_catalog_product_skus_active_product_sale_price
on catalog_product_skus (product_id, sale_price)
include (compare_price)
where status = 'ACTIVE'
  and sale_price > 0;
```

Lý do phù hợp:

- Lateral subquery luôn tìm SKU theo `s.product_id = p.id`.
- Partial condition khớp chính xác:

  ```sql
  s.status = 'ACTIVE'
  and s.sale_price > 0
  ```

- `sale_price` nằm trong key để hỗ trợ đọc các SKU của product theo giá.
- `compare_price` nằm trong `include` để giảm heap fetch khi tính `min(compare_price)`.

### 4.3. Trigram cho SKU keyword search

```sql
create extension if not exists pg_trgm;

create index concurrently if not exists idx_catalog_product_skus_active_sku_code_trgm
on catalog_product_skus
using gin (lower(sku_code) gin_trgm_ops)
where status = 'ACTIVE';
```

Lý do phù hợp:

- Pattern `like '%product 1%'` không dùng được B-tree index thông thường.
- GIN trigram index hỗ trợ contains search trên `lower(sku_code)`.
- Partial condition `where status = 'ACTIVE'` khớp query và giảm kích thước index.

### 4.4. Trigram cho product name nếu keyword search product name phổ biến

```sql
create index concurrently if not exists idx_catalog_products_active_name_trgm
on catalog_products
using gin (lower(name) gin_trgm_ops)
where status = 'ACTIVE'
  and published_at is not null;
```

Index này tối ưu nhanh `lower(p.name) like '%...%'`. Trong plan sau index bên dưới, PostgreSQL không cần dùng index này vì filter category/brand đã chỉ còn 3 product, việc check `lower(p.name)` trên 3 row rẻ hơn dùng trigram.

## 5. Plan Sau Khi Thêm Index

Execution time: khoảng `0.453 ms`.

```text
Limit  (cost=17.75..3981.88 rows=1 width=309) (actual time=0.180..0.377 rows=1.00 loops=1)
  Buffers: shared hit=39 read=21
  ->  Nested Loop  (cost=17.75..3981.88 rows=1 width=309) (actual time=0.179..0.375 rows=1.00 loops=1)
        Buffers: shared hit=39 read=21
        ->  Nested Loop Left Join  (cost=1.28..3965.38 rows=1 width=245) (actual time=0.083..0.216 rows=2.00 loops=1)
              Filter: ((lower((p.name)::text) ~~ '%product 1%'::text) OR (lower((b.name)::text) ~~ '%product 1%'::text) OR (ANY (p.id = (hashed SubPlan 2).col1)))
              Rows Removed by Filter: 1
              Buffers: shared hit=38 read=9
              ->  Nested Loop  (cost=0.85..16.91 rows=1 width=241) (actual time=0.072..0.103 rows=3.00 loops=1)
                    Buffers: shared hit=12 read=6
                    ->  Index Scan using idx_catalog_products_listing_cat_brand_new_active on catalog_products p  (cost=0.43..8.45 rows=1 width=234) (actual time=0.061..0.085 rows=3.00 loops=1)
                          Index Cond: ((category_id = 1) AND (brand_id = 1))
                          Index Searches: 1
                          Buffers: shared read=6
                    ->  Index Scan using catalog_categories_pkey on catalog_categories c  (cost=0.42..8.45 rows=1 width=23) (actual time=0.004..0.005 rows=1.00 loops=3)
                          Index Cond: (id = 1)
                          Filter: ((status)::text = 'ACTIVE'::text)
                          Index Searches: 3
                          Buffers: shared hit=12
              ->  Index Scan using catalog_brands_pkey on catalog_brands b  (cost=0.42..8.44 rows=1 width=20) (actual time=0.004..0.004 rows=1.00 loops=3)
                    Index Cond: (id = 1)
                    Filter: ((status)::text = 'ACTIVE'::text)
                    Index Searches: 3
                    Buffers: shared hit=12
              SubPlan 2
                ->  Bitmap Heap Scan on catalog_product_skus keyword_sku  (cost=501.41..3938.00 rows=898 width=8) (actual time=0.087..0.087 rows=0.00 loops=1)
                      Recheck Cond: ((lower((sku_code)::text) ~~ '%product 1%'::text) AND ((status)::text = 'ACTIVE'::text))
                      Buffers: shared hit=14 read=3
                      ->  Bitmap Index Scan on idx_catalog_product_skus_active_sku_code_trgm  (cost=0.00..501.19 rows=898 width=0) (actual time=0.080..0.080 rows=0.00 loops=1)
                            Index Cond: (lower((sku_code)::text) ~~ '%product 1%'::text)
                            Index Searches: 1
                            Buffers: shared hit=14 read=3
        ->  Aggregate  (cost=16.47..16.48 rows=1 width=64) (actual time=0.078..0.079 rows=0.50 loops=2)
              Filter: ((min(s.sale_price) IS NOT NULL) AND (min(s.sale_price) >= '0'::numeric) AND (min(s.sale_price) <= '200'::numeric))
              Rows Removed by Filter: 0
              Buffers: shared hit=1 read=12
              ->  Index Scan using idx_catalog_product_skus_active_product_sale_price on catalog_product_skus s  (cost=0.43..16.45 rows=3 width=12) (actual time=0.044..0.074 rows=3.50 loops=2)
                    Index Cond: (product_id = p.id)
                    Index Searches: 2
                    Buffers: shared hit=1 read=12
Planning:
  Buffers: shared hit=132 read=6
Planning Time: 5.691 ms
Execution Time: 0.453 ms
```

### 5.1. Thứ Tự Thực Thi Sau Tối Ưu

1. `Index Scan using idx_catalog_products_listing_cat_brand_new_active`

   PostgreSQL không còn scan toàn bảng `catalog_products`. Nó seek vào index theo:

   ```sql
   category_id = 1
   and brand_id = 1
   ```

   Kết quả chỉ còn 3 product candidate.

2. `Index Scan using catalog_categories_pkey`

   Với mỗi product candidate, PostgreSQL lookup category bằng primary key và check:

   ```sql
   c.status = 'ACTIVE'
   ```

3. `Index Scan using catalog_brands_pkey`

   Với mỗi product candidate, PostgreSQL lookup brand bằng primary key và check:

   ```sql
   b.status = 'ACTIVE'
   ```

4. Keyword filter

   Product/brand name được check trực tiếp trên 3 candidate rows.

   SKU keyword subplan dùng trigram:

   ```text
   Bitmap Index Scan on idx_catalog_product_skus_active_sku_code_trgm
   ```

   Thay vì quét 10 triệu SKU, PostgreSQL hỏi GIN trigram index để tìm `lower(sku_code) like '%product 1%'`.

5. `LATERAL sku_price`

   Lateral aggregate dùng:

   ```text
   Index Scan using idx_catalog_product_skus_active_product_sale_price
   Index Cond: (product_id = p.id)
   ```

   Thay vì quét cả bảng SKU, mỗi lần chỉ đọc SKU bán được của đúng product.

6. `Limit`

   Query trả về 1 row trong `0.453 ms`.

## 6. So Sánh Trước Và Sau

| Hạng mục | Trước index | Sau index |
| --- | --- | --- |
| Execution time | `10738.962 ms` | `0.453 ms` |
| Product access | `Parallel Seq Scan` trên `catalog_products` | `Index Scan` trên `idx_catalog_products_listing_cat_brand_new_active` |
| Product rows removed | Gần `3,000,000` rows | Chỉ đọc 3 candidate rows |
| SKU keyword access | `Parallel Seq Scan` trên `catalog_product_skus` | `Bitmap Index Scan` trên trigram GIN index |
| SKU keyword rows removed | Gần `10,000,000` rows | 0 row match, đọc 17 buffers |
| SKU min price access | `Seq Scan` SKU với `loops=2` | `Index Scan` theo `product_id` với `loops=2` |
| Buffers | `hit=8772 read=940739` | `hit=39 read=21` |

Mức cải thiện execution time:

```text
10738.962 ms / 0.453 ms = khoảng 23706 lần
```

## 7. Kết Luận

Bộ index hiệu quả nhất cho query hiện tại:

```sql
create extension if not exists pg_trgm;

create index concurrently if not exists idx_catalog_products_listing_cat_brand_new_active
on catalog_products (category_id, brand_id, published_at desc, id desc)
where status = 'ACTIVE'
  and published_at is not null;

create index concurrently if not exists idx_catalog_product_skus_active_product_sale_price
on catalog_product_skus (product_id, sale_price)
include (compare_price)
where status = 'ACTIVE'
  and sale_price > 0;

create index concurrently if not exists idx_catalog_product_skus_active_sku_code_trgm
on catalog_product_skus
using gin (lower(sku_code) gin_trgm_ops)
where status = 'ACTIVE';

create index concurrently if not exists idx_catalog_products_active_name_trgm
on catalog_products
using gin (lower(name) gin_trgm_ops)
where status = 'ACTIVE'
  and published_at is not null;

analyze catalog_products;
analyze catalog_product_skus;
```

Trong execution plan sau tối ưu, bottleneck lớn đã biến mất:

- Không còn `Parallel Seq Scan` trên `catalog_products`.
- Không còn `Parallel Seq Scan` trên `catalog_product_skus` cho keyword SKU.
- Không còn `Seq Scan` toàn bảng SKU trong lateral aggregate.

Ghi chú:

- Trigram index nên tách riêng với B-tree listing index. GIN trigram giải bài toán contains search, còn B-tree giải bài toán equality filter và ordered pagination.
- `idx_catalog_products_active_name_trgm` không xuất hiện trong plan này vì candidate product sau filter category/brand quá ít. Nó vẫn có giá trị nếu keyword search product name được dùng trong case không filter category/brand chặt.
- Nếu `category_id in (...)` hoặc `brand_id in (...)` có nhiều giá trị, PostgreSQL có thể vẫn cần sort vì index chỉ giữ order bên trong từng prefix `(category_id, brand_id)`, không đảm bảo global order giữa nhiều prefix.
