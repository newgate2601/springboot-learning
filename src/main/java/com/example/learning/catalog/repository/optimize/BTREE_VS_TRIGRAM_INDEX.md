# B-tree Index vs Trigram Index

Tài liệu này so sánh B-tree index và trigram index trong PostgreSQL, dựa trên các query catalog hiện tại.

## 1. Tóm Tắt Nhanh

| Tiêu chí | B-tree index | Trigram index |
| --- | --- | --- |
| Kiểu index thường dùng | `btree` | `gin` + `gin_trgm_ops` |
| Mạnh nhất với | Equality, range, prefix, sort | Contains search trong text |
| Ví dụ tốt | `category_id = 1`, `price between 0 and 200`, `order by published_at desc` | `lower(name) like '%product%'` |
| Ví dụ yếu | `like '%product%'` | `order by published_at desc limit 20` |
| Có giữ thứ tự sort không? | Có | Không phù hợp để giữ order sort |
| Dung lượng | Thường nhỏ hơn | Thường lớn hơn |
| Ghi dữ liệu | Rẻ hơn | Đắt hơn vì một text tạo nhiều trigram |
| Query cần recheck? | Thường không | Thường có `Recheck Cond` |

## 2. B-tree Index Là Gì?

B-tree index lưu giá trị theo thứ tự đã sắp xếp. Có thể hiểu gần giống danh bạ:

```text
category_id=1, brand_id=1, published_at=2026-04-29
category_id=1, brand_id=1, published_at=2026-04-28
category_id=1, brand_id=2, published_at=2026-04-29
category_id=2, brand_id=1, published_at=2026-04-29
```

Vì dữ liệu được sắp theo key, B-tree rất mạnh với:

- Tìm đúng giá trị: `category_id = 1`.
- Tìm trong khoảng: `sale_price >= 0 and sale_price <= 200`.
- Sort/pagination: `order by published_at desc, id desc limit 20`.
- Prefix search: `sku_code like 'ABC%'` nếu index/operator class phù hợp.

Ví dụ index listing:

```sql
create index concurrently if not exists idx_catalog_products_listing_cat_brand_new_active
on catalog_products (category_id, brand_id, published_at desc, id desc)
where status = 'ACTIVE'
  and published_at is not null;
```

Query phù hợp:

```sql
where p.category_id = 1
  and p.brand_id = 1
  and p.status = 'ACTIVE'
  and p.published_at is not null
order by p.published_at desc, p.id desc
limit 20;
```

PostgreSQL có thể seek vào đúng vùng `(category_id=1, brand_id=1)`, sau đó đọc theo thứ tự `published_at desc, id desc`.

## 3. Vì Sao B-tree Yếu Với `LIKE '%keyword%'`?

B-tree giỏi khi biết điểm bắt đầu của chuỗi.

Ví dụ:

```sql
where sku_code like 'PRODUCT%'
```

Database có thể nhảy đến vùng chuỗi bắt đầu bằng `PRODUCT`.

Nhưng với:

```sql
where sku_code like '%PRODUCT%'
```

keyword có thể nằm ở bất kỳ vị trí nào:

```text
ABC-PRODUCT-001
SKU-PRODUCT-RED
X-PRODUCT-FINAL
```

B-tree không biết phải nhảy vào đâu vì phần đầu chuỗi không cố định. Kết quả thường là `Seq Scan` hoặc scan rất rộng.

## 4. Trigram Index Là Gì?

Trigram tách text thành các mảnh 3 ký tự.

Ví dụ:

```text
product
```

có các trigram:

```text
pro
rod
odu
duc
uct
```

Khi tạo index:

```sql
create extension if not exists pg_trgm;

create index concurrently if not exists idx_catalog_product_skus_active_sku_code_trgm
on catalog_product_skus
using gin (lower(sku_code) gin_trgm_ops)
where status = 'ACTIVE';
```

PostgreSQL lưu kiểu inverted index:

```text
"pro" -> row 10, row 82, row 150
"rod" -> row 10, row 82
"uct" -> row 10, row 82, row 300
```

Khi query:

```sql
where lower(sku_code) like '%product%'
```

PostgreSQL tách `product` thành trigram, hỏi index xem row nào chứa các mảnh đó, rồi đọc lại candidate rows để kiểm tra điều kiện thật.

## 5. Vì Sao Trigram Có `Recheck Cond`?

Trong execution plan trigram thường thấy:

```text
Bitmap Heap Scan
  Recheck Cond: lower(sku_code) ~~ '%product%'
  -> Bitmap Index Scan on idx_catalog_product_skus_active_sku_code_trgm
```

Lý do:

- Trigram index lưu các mảnh 3 ký tự, không lưu đầy đủ logic của câu `LIKE`.
- Một row có nhiều trigram giống pattern chưa chắc match đúng substring theo thứ tự/khoảng cách mong muốn.
- Bitmap scan đôi khi có thể lossy ở cấp page, nghĩa là PostgreSQL biết page có khả năng chứa row match nhưng vẫn phải kiểm tra lại từng row trên page.

Vì vậy luồng thực thi là:

```text
GIN trigram index tìm candidate nhanh
-> đọc heap rows
-> recheck điều kiện LIKE thật
```

Recheck là cơ chế đảm bảo kết quả đúng, không phải lỗi.

## 6. Trade-off Của Trigram

### 6.1. Keyword dưới 3 ký tự rất yếu

Trigram cần mảnh 3 ký tự. Với:

```sql
like '%1%'
```

keyword chỉ có 1 ký tự, không có trigram đủ tốt để lọc mạnh. Nếu nhiều SKU/name chứa `1`, query có thể match rất nhiều row. Khi đó planner có thể không dùng index, hoặc dùng index nhưng vẫn phải đọc rất nhiều candidate.

Rule thực dụng:

```text
Contains search nên yêu cầu keyword tối thiểu 3 ký tự.
```

Với keyword ngắn như `1`, nên dùng exact/prefix search hoặc tách numeric token thay vì contains search toàn text.

### 6.2. Index lớn hơn B-tree

Một text tạo nhiều trigram. Ví dụ:

```text
PRODUCT-123-RED
```

có thể tạo nhiều mảnh:

```text
pro, rod, odu, duc, uct, ct-, t-1, -12, 123, 23-, 3-r, -re, red
```

Vì vậy GIN trigram index thường lớn hơn B-tree, tốn thêm disk, cache, backup/restore và maintenance.

### 6.3. Ghi dữ liệu chậm hơn

Khi insert/update text, PostgreSQL phải tính trigram và cập nhật nhiều entry trong GIN index. Điều này làm write path đắt hơn B-tree.

Với catalog đọc nhiều hơn ghi, chi phí này thường chấp nhận được. Với bảng update text liên tục, cần cân nhắc kỹ.

### 6.4. Không giải quyết sort/pagination

Trigram giúp tìm text chứa keyword, nhưng không giữ order theo:

```sql
order by published_at desc, id desc
```

Sau khi tìm row match bằng trigram, PostgreSQL vẫn có thể phải sort kết quả. Vì vậy listing query thường cần cả:

```sql
-- B-tree cho filter/sort listing
(category_id, brand_id, published_at desc, id desc)

-- GIN trigram cho contains search
lower(name) gin_trgm_ops
```

## 7. Search Không Phân Biệt Hoa Thường Và Dấu

Nếu muốn search:

```text
Phu
```

mà match được:

```text
phúc
Phúc
PHÚC
```

cần normalize cả dữ liệu và keyword:

```text
Phúc -> phuc
PHÚC -> phuc
Phu  -> phu
```

PostgreSQL có thể dùng `unaccent` + `lower`:

```sql
create extension if not exists unaccent;
create extension if not exists pg_trgm;

create or replace function immutable_unaccent(input text)
returns text
language sql
immutable
parallel safe
strict
as $$
    select unaccent('unaccent', input)
$$;
```

Index:

```sql
create index concurrently if not exists idx_catalog_products_name_unaccent_trgm
on catalog_products
using gin (lower(immutable_unaccent(name)) gin_trgm_ops)
where status = 'ACTIVE'
  and published_at is not null;
```

Query phải dùng đúng expression với index:

```sql
where lower(immutable_unaccent(p.name))
      like '%' || lower(immutable_unaccent(:keyword)) || '%'
```

Nếu query viết lệch expression, planner có thể không dùng được index expression này.

## 8. Khi Nào Chọn B-tree?

Chọn B-tree khi query chủ yếu là:

- `=`, `in`, `>`, `<`, `between`.
- Join theo khóa.
- Sort/pagination theo một thứ tự cố định.
- Prefix search như `like 'ABC%'`.

Ví dụ trong catalog:

```sql
create index concurrently if not exists idx_catalog_products_listing_cat_brand_new_active
on catalog_products (category_id, brand_id, published_at desc, id desc)
where status = 'ACTIVE'
  and published_at is not null;
```

```sql
create index concurrently if not exists idx_catalog_product_skus_active_product_sale_price
on catalog_product_skus (product_id, sale_price)
include (compare_price)
where status = 'ACTIVE'
  and sale_price > 0;
```

## 9. Khi Nào Chọn Trigram?

Chọn trigram khi query cần contains search:

```sql
like '%keyword%'
ilike '%keyword%'
similarity search
```

Ví dụ trong catalog:

```sql
create index concurrently if not exists idx_catalog_product_skus_active_sku_code_trgm
on catalog_product_skus
using gin (lower(sku_code) gin_trgm_ops)
where status = 'ACTIVE';
```

```sql
create index concurrently if not exists idx_catalog_products_active_name_trgm
on catalog_products
using gin (lower(name) gin_trgm_ops)
where status = 'ACTIVE'
  and published_at is not null;
```

## 10. Khuyến Nghị Cho Product Listing

Với `ProductListingRepository.search`, nên dùng phối hợp:

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
```

Nếu search product name là case phổ biến:

```sql
create index concurrently if not exists idx_catalog_products_active_name_trgm
on catalog_products
using gin (lower(name) gin_trgm_ops)
where status = 'ACTIVE'
  and published_at is not null;
```

Rule ở application:

```text
Keyword contains search nên có ít nhất 3 ký tự sau khi trim/normalize.
```

Nếu keyword dưới 3 ký tự:

- Không chạy contains search.
- Hoặc yêu cầu user nhập thêm ký tự.
- Hoặc chuyển sang exact/prefix search cho mã SKU.
- Hoặc dùng bảng token riêng cho numeric token.

## 11. Case Khó: `LIKE '%a%' ORDER BY name LIMIT 10`

Query dạng này rất khó tối ưu tốt:

```sql
select *
from product
where name like '%a%'
order by name
limit 10;
```

Lý do là query đang cần hai thứ khác nhau:

```text
name like '%a%' -> cần contains search, hợp với trigram/GIN
order by name   -> cần dữ liệu đã sorted theo name, hợp với B-tree
```

GIN trigram có thể tìm row chứa keyword, nhưng không trả kết quả theo thứ tự `name`.

B-tree theo `name` có thể trả kết quả theo thứ tự `name`, nhưng không biết nhảy đến các chuỗi có ký tự `a` ở giữa. Nó phải đọc theo thứ tự index rồi check từng row:

```text
row này có chứa "a" không?
row tiếp theo có chứa "a" không?
...
đủ 10 row thì dừng
```

Nếu `a` rất phổ biến, cách này có thể nhanh vì đọc vài row là đủ. Nếu keyword hiếm, PostgreSQL có thể phải đọc rất nhiều row theo order `name` mới tìm đủ 10 row.

Với `%a%`, trigram cũng yếu vì keyword chỉ có 1 ký tự. Trigram cần mảnh 3 ký tự để lọc tốt, nên pattern 1-2 ký tự thường không đủ selectivity.

Nói cách khác:

```text
%a% + order by name + limit 10
```

không có một index thần kỳ trong PostgreSQL để vừa contains search một ký tự, vừa trả kết quả sorted theo `name` thật nhanh cho mọi data pattern.

### 11.1. Hướng xử lý thực dụng

Nếu mục tiêu là autocomplete, nên đổi sang prefix search:

```sql
where lower(name) like 'a%'
order by lower(name)
limit 10;
```

Index:

```sql
create index concurrently if not exists idx_product_lower_name_pattern
on product (lower(name) text_pattern_ops);
```

Nếu mục tiêu là contains search, nên yêu cầu keyword tối thiểu 3 ký tự và dùng trigram:

```sql
where lower(name) like '%abc%'
order by name
limit 10;
```

Index:

```sql
create index concurrently if not exists idx_product_lower_name_trgm
on product
using gin (lower(name) gin_trgm_ops);
```

PostgreSQL sẽ dùng trigram để tìm candidate rows, sau đó sort candidate theo `name`. Cách này nhanh khi candidate ít, nhưng vẫn có thể chậm nếu keyword quá phổ biến.

Nếu user nhập keyword dưới 3 ký tự:

- Không chạy contains search.
- Yêu cầu nhập thêm ký tự.
- Chuyển sang prefix search nếu phù hợp với UX.
- Chuyển sang exact search cho mã SKU/code nếu keyword là mã cụ thể.
- Dùng bảng token riêng nếu cần search số ngắn hoặc token đặc biệt.

Quy tắc thiết kế nên dùng:

```text
Autocomplete -> prefix search -> B-tree
Contains search -> keyword >= 3 ký tự -> trigram
Keyword 1-2 ký tự -> không contains search trên bảng lớn
```

## 12. Kết Luận

B-tree và trigram không thay thế nhau.

B-tree trả lời tốt câu hỏi:

```text
Tôi cần đúng nhóm dữ liệu nào, theo thứ tự nào?
```

Trigram trả lời tốt câu hỏi:

```text
Text nào chứa keyword này ở bất kỳ vị trí nào?
```

Với catalog search lớn, thiết kế tốt thường cần cả hai:

- B-tree cho filter, join, min price, sort, pagination.
- Trigram cho keyword contains search.
