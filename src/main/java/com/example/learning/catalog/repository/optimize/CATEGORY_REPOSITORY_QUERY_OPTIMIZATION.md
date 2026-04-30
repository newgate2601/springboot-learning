# com.example.learning.catalog.repository.CategoryRepository Query Optimization Notes

Tài liệu này ghi lại phân tích và tối ưu query trong `com.example.learning.catalog.repository.CategoryRepository`.

Hiện tại tài liệu phân tích:

- `findFeaturedCategories`

## 1. `findFeaturedCategories`

### 1.1. Mục đích nghiệp vụ

`findFeaturedCategories` lấy danh sách danh mục nổi bật để hiển thị ở khu vực Home.

Điều kiện visibility:

- Category phải `ACTIVE`.
- Category phải được đánh dấu `featured = true`.

Sort:

```sql
order by c.sort_order asc, c.name asc
```

`sort_order` quyết định thứ tự ưu tiên hiển thị. `name` là tie-breaker để thứ tự ổn định khi nhiều category có cùng `sort_order`.

### 1.2. Repository hiện tại

```java
@Query(
        value = """
        select
            c.id as "categoryId",
            c.name as name,
            c.slug as slug
        from catalog_categories c
        where c.status = :activeStatus
          and c.featured = true
        order by c.sort_order asc, c.name asc
        """,
        countQuery = """
        select count(c.id)
        from catalog_categories c
        where c.status = :activeStatus
          and c.featured = true
        """,
        nativeQuery = true
)
Page<HomeCategoryProjection> findFeaturedCategories(
        @Param("activeStatus") CategoryStatus activeStatus,
        Pageable pageable
);
```

## 2. Query PostgreSQL Dùng Để Explain

```sql
EXPLAIN ANALYZE
select
    c.id as "categoryId",
    c.name as name,
    c.slug as slug
from catalog_categories c
where c.status = 'ACTIVE'
  and c.featured = true
order by c.sort_order asc, c.name asc
limit 8 offset 0;
```

Count query:

```sql
EXPLAIN ANALYZE
select count(c.id)
from catalog_categories c
where c.status = 'ACTIVE'
  and c.featured = true;
```

## 3. Data Pattern Trong Database `banking`

Dữ liệu quan sát được:

```text
total_rows              = 1,000,000
ACTIVE rows             = 950,000
featured = true rows    = 1,000
ACTIVE + featured rows  = 0
```

Index trước khi tối ưu:

```text
catalog_categories_pkey on id
uk_catalog_categories_slug on slug
```

Điểm quan trọng:

- `status = 'ACTIVE'` không selective vì match khoảng 95% bảng.
- `featured = true` rất selective vì chỉ match khoảng 0.1% bảng.
- Query cần kết quả theo đúng thứ tự `sort_order asc, name asc`.
- Query Home có `limit 8`, nên database nên đọc được vài row đầu theo đúng thứ tự thay vì scan và sort toàn bảng.

Vì vậy, index nên bắt đầu từ pattern hiếm nhất là `featured = true`, nhưng dùng partial index thay vì đưa `featured` vào key.

## 4. Plan Trước Khi Thêm Index

Execution time: khoảng `201.995 ms`.

```text
Limit  (cost=19648.53..19648.55 rows=8 width=42) (actual time=197.907..201.967 rows=0.00 loops=1)
  Buffers: shared hit=288 read=13052
  ->  Sort  (cost=19648.53..19650.75 rows=885 width=42) (actual time=197.906..201.965 rows=0.00 loops=1)
        Sort Key: sort_order, name
        Sort Method: quicksort  Memory: 25kB
        Buffers: shared hit=288 read=13052
        ->  Gather  (cost=1000.00..19630.83 rows=885 width=42) (actual time=197.881..201.939 rows=0.00 loops=1)
              Workers Planned: 2
              Workers Launched: 2
              Buffers: shared hit=282 read=13052
              ->  Parallel Seq Scan on catalog_categories c  (cost=0.00..18542.33 rows=369 width=42) (actual time=159.357..159.358 rows=0.00 loops=3)
                    Filter: (featured AND ((status)::text = 'ACTIVE'::text))
                    Rows Removed by Filter: 333333
                    Buffers: shared hit=282 read=13052
Planning:
  Buffers: shared hit=75 read=4
Planning Time: 1.617 ms
Execution Time: 201.995 ms
```

### 4.1. Thứ Tự Thực Thi Thực Tế

1. `Parallel Seq Scan on catalog_categories`

   PostgreSQL chia bảng cho 3 process, mỗi process scan khoảng 333,333 rows.

2. Filter:

   ```sql
   featured = true
   and status = 'ACTIVE'
   ```

   Không có row nào match trong data hiện tại.

3. `Gather`

   Gom kết quả từ các worker.

4. `Sort`

   PostgreSQL vẫn giữ bước sort theo:

   ```sql
   sort_order, name
   ```

5. `Limit`

   Trả về 0 row.

### 4.2. Vấn Đề Chính

Các điểm chậm chính:

- Không có index nào phục vụ `featured = true`.
- `status = 'ACTIVE'` match quá nhiều row nên không phải điều kiện tốt để scan.
- PostgreSQL phải đọc gần như toàn bộ bảng 1,000,000 rows.
- Query trả 0 row nhưng vẫn mất khoảng 202 ms vì phải chứng minh không có row match bằng full scan.

## 5. Index Đã Thêm

```sql
create index concurrently idx_catalog_categories_featured_status_sort_name
on catalog_categories (status, sort_order asc, name asc)
where featured = true;
```

Lý do index này phù hợp:

- `where featured = true` làm index rất nhỏ, chỉ khoảng 1,000 rows.
- `status` nằm trong key để PostgreSQL dùng `Index Cond` cho `status = 'ACTIVE'`.
- `sort_order asc, name asc` khớp `order by`, nên không cần sort tập lớn.
- Không thêm `include (slug)` vì plan thực tế đã đủ nhanh; result Home chỉ lấy rất ít row.
- Không thêm `id` vào key vì query hiện tại không sort theo `id`, và index tối thiểu này đã loại bỏ bottleneck chính.

## 6. Plan Sau Khi Thêm Index

Execution time: khoảng `0.023 ms`.

```text
Limit  (cost=0.28..17.62 rows=8 width=42) (actual time=0.010..0.010 rows=0.00 loops=1)
  Buffers: shared hit=2
  ->  Index Scan using idx_catalog_categories_featured_status_sort_name on catalog_categories c  (cost=0.28..1918.64 rows=885 width=42) (actual time=0.009..0.009 rows=0.00 loops=1)
        Index Cond: ((status)::text = 'ACTIVE'::text)
        Index Searches: 1
        Buffers: shared hit=2
Planning:
  Buffers: shared hit=82 read=1
Planning Time: 0.505 ms
Execution Time: 0.023 ms
```

### 6.1. Thứ Tự Thực Thi Sau Tối Ưu

1. `Index Scan using idx_catalog_categories_featured_status_sort_name`

   PostgreSQL chỉ scan partial index chứa các row `featured = true`.

2. `Index Cond`

   PostgreSQL tìm trong index theo:

   ```sql
   status = 'ACTIVE'
   ```

3. `Limit`

   Vì không có row `ACTIVE + featured`, query dừng gần như ngay lập tức.

### 6.2. Dấu Hiệu Plan Đã Tốt

```text
Index Scan using idx_catalog_categories_featured_status_sort_name
Buffers: shared hit=2
Execution Time: 0.023 ms
```

Những thứ đã biến mất:

- Không còn `Parallel Seq Scan`.
- Không còn scan 1,000,000 rows.
- Không còn đọc 13,052 buffers từ disk.
- Không còn sort sau khi scan toàn bảng.

## 7. So Sánh Trước Và Sau

| Tiêu chí | Trước index | Sau index |
| --- | --- | --- |
| Execution time | `201.995 ms` | `0.023 ms` |
| Access path | Parallel seq scan | Index scan |
| Rows removed by filter | khoảng `1,000,000` | không còn full-table filter |
| Buffers read | `13052` | `0` |
| Buffers hit | `288` | `2` |
| Sort | Có `Sort` sau scan | Không còn sort lớn |

Tóm lại:

- Trước index: scan toàn bộ bảng để tìm một tập rất nhỏ.
- Sau index: scan partial index rất nhỏ, đúng theo pattern `featured = true`.

## 8. Ghi Chú Về Data Seed

Data hiện tại không có category nào vừa:

```sql
status = 'ACTIVE'
and featured = true
```

Vì vậy endpoint Home Featured Categories sẽ trả page rỗng.

Nếu mục tiêu là có dữ liệu hiển thị trên Home, cần cập nhật seed/test data để có một số category `ACTIVE` và `featured = true`.

Index vẫn đúng cho production pattern vì thông thường số category featured sẽ nhỏ hơn rất nhiều so với tổng số category.

