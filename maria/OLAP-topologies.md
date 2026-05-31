# OLAP Topologies: ColumnStore Object Storage vs Shared Local Storage

## 1. OLAP là gì?

OLAP là viết tắt của **Online Analytical Processing**. Đây là nhóm workload dùng để phân tích dữ liệu lớn, thường gặp trong:

- Data warehouse
- BI reporting
- Dashboard doanh thu
- Historical analytics
- DSS: Decision Support System
- Phân tích log, event, clickstream

OLAP khác với OLTP.

OLTP tập trung vào giao dịch nhỏ, nhanh, chính xác:

```sql
UPDATE orders SET status = 'PAID' WHERE id = 123;
SELECT * FROM users WHERE id = 999;
INSERT INTO payments (...);
```

OLAP tập trung vào đọc rất nhiều dòng, tính toán tổng hợp:

```sql
SELECT region, product_category, SUM(revenue) AS total_revenue
FROM sales_fact
WHERE order_date BETWEEN '2026-01-01' AND '2026-03-31'
GROUP BY region, product_category
ORDER BY total_revenue DESC;
```

Query OLAP thường có đặc điểm:

- Scan nhiều triệu đến nhiều tỷ dòng.
- Chỉ dùng một số cột trong bảng.
- Dùng `SUM`, `COUNT`, `AVG`, `MIN`, `MAX`.
- Dùng `GROUP BY`, `ORDER BY`.
- Lọc theo ngày/tháng/quý/năm.
- Ít update từng dòng trực tiếp.

## 2. ColumnStore là gì?

ColumnStore là kiểu lưu trữ dữ liệu theo cột thay vì theo dòng.

Giả sử có bảng:

```text
orders(id, customer, region, amount, order_date)
```

Dữ liệu logic:

| id | customer | region | amount | order_date |
|---:|---|---|---:|---|
| 1 | A | HCM | 100 | 2026-01-01 |
| 2 | B | HN | 200 | 2026-01-02 |
| 3 | C | HCM | 150 | 2026-01-03 |

### Row-store

Row-store lưu gần nhau theo từng dòng:

```text
row1: [id=1, customer=A, region=HCM, amount=100, order_date=2026-01-01]
row2: [id=2, customer=B, region=HN,  amount=200, order_date=2026-01-02]
row3: [id=3, customer=C, region=HCM, amount=150, order_date=2026-01-03]
```

Kiểu này rất hợp OLTP vì khi cần lấy hoặc sửa một dòng, database có thể dùng index để tìm đúng row.

### Column-store

Column-store lưu riêng từng cột:

```text
id:         [1, 2, 3]
customer:   [A, B, C]
region:     [HCM, HN, HCM]
amount:     [100, 200, 150]
order_date: [2026-01-01, 2026-01-02, 2026-01-03]
```

Kiểu này rất hợp OLAP vì query phân tích thường chỉ cần vài cột.

Ví dụ:

```sql
SELECT region, SUM(amount)
FROM orders
WHERE order_date >= '2026-01-01'
GROUP BY region;
```

Query này chỉ cần:

```text
region
amount
order_date
```

Nếu bảng có 100 cột, ColumnStore không cần đọc 97 cột còn lại.

## 3. Lợi ích đọc ít cột so với row-store

Giả sử bảng `orders` có 100 cột:

```text
id, customer_id, name, phone, address, note, status, payment_method,
region, amount, order_date, campaign_id, device, ...
```

Query báo cáo:

```sql
SELECT region, SUM(amount)
FROM orders
WHERE order_date >= '2026-01-01'
GROUP BY region;
```

Nếu mỗi row nặng khoảng 1 KB và có 100 triệu row:

```text
100 triệu row * 1 KB = khoảng 100 GB
```

Row-store có xu hướng phải đọc page chứa cả row, tức đọc cả nhiều cột không cần dùng như `name`, `phone`, `address`, `note`.

Nếu trong ColumnStore, 3 cột `region`, `amount`, `order_date` chỉ tốn khoảng 80 bytes/row:

```text
100 triệu row * 80 bytes = khoảng 8 GB
```

Lợi ích:

- Ít I/O hơn.
- Ít dữ liệu đi qua network hơn.
- Ít dữ liệu đưa vào memory hơn.
- CPU cache hiệu quả hơn.
- Nén theo cột tốt hơn.
- Aggregate nhanh hơn khi scan dữ liệu lớn.

Kết luận:

```text
Lookup/update 1 dòng -> RowStore mạnh.
Scan/aggregate nhiều triệu dòng -> ColumnStore mạnh.
```

## 4. Segment, block và row position trong ColumnStore

Thực tế ColumnStore không lưu một cột thành một file khổng lồ duy nhất. Nó chia thành các segment hoặc block.

Ví dụ mỗi segment chứa 1 triệu row:

```text
Segment 1: row 1 -> row 1,000,000
  id column chunk
  region column chunk
  category column chunk
  amount column chunk
  order_date column chunk

Segment 2: row 1,000,001 -> row 2,000,000
  id column chunk
  region column chunk
  category column chunk
  amount column chunk
  order_date column chunk
```

Các cột trong cùng segment được căn chỉnh bằng **row position**.

Ví dụ:

```text
position:  1      2       3
category:  phone  laptop  phone
amount:    100    200     150
```

Dòng thứ 1 có:

```text
category[1] = phone
amount[1] = 100
```

Dòng thứ 2 có:

```text
category[2] = laptop
amount[2] = 200
```

Vậy khi query:

```sql
SELECT category, SUM(amount)
FROM sales
GROUP BY category;
```

Engine duyệt đồng bộ theo position:

```text
i = 1: category=phone,  amount=100
i = 2: category=laptop, amount=200
i = 3: category=phone,  amount=150
```

ColumnStore không cần join giữa cột `category` và cột `amount`. Nó chỉ cần đọc cùng position trong các column chunk.

## 5. Metadata của segment

Mỗi segment thường có metadata, ví dụ:

```text
row count
min/max của cột
null count
dictionary
encoding type
compression type
giá trị distinct gần đúng
```

Ví dụ segment:

```text
Segment A:
order_date min = 2026-01-01
order_date max = 2026-01-31

Segment B:
order_date min = 2026-02-01
order_date max = 2026-02-28
```

Query:

```sql
SELECT SUM(amount)
FROM sales
WHERE order_date BETWEEN '2026-01-01' AND '2026-01-31';
```

Engine có thể skip Segment B vì metadata cho biết Segment B không có dữ liệu tháng 1.

Đây gọi là segment pruning hoặc data skipping.

## 6. Encoding và compression trong ColumnStore

ColumnStore thường nén dữ liệu theo cột. Nén ở đây không chỉ là ZIP/GZIP. Thường có cả encoding chuyên biệt.

### 6.1 Dictionary encoding

Cột `category`:

```text
phone, laptop, phone, accessory, phone, laptop
```

Có thể tạo dictionary:

```text
1 = phone
2 = laptop
3 = accessory
```

Dữ liệu thật lưu thành:

```text
1, 2, 1, 3, 1, 2
```

Query:

```sql
WHERE category = 'phone'
```

Engine tra dictionary một lần:

```text
phone -> 1
```

Sau đó filter trên số:

```text
category_code = 1
```

Lợi ích:

- Giảm dung lượng lưu trữ.
- So sánh integer nhanh hơn string.
- Hash/group integer nhanh hơn string.
- Giảm memory khi aggregate.

### 6.2 Run-length encoding

Nếu cột có nhiều giá trị lặp lại:

```text
HCM, HCM, HCM, HCM, HN, HN, DN, DN, DN
```

Có thể lưu:

```text
HCM x 4
HN x 2
DN x 3
```

Query `COUNT` hoặc `GROUP BY` có thể cộng theo run thay vì đọc từng dòng logic.

### 6.3 Delta encoding

Cột có giá trị tăng dần:

```text
1000, 1001, 1002, 1003
```

Có thể lưu:

```text
base = 1000
delta = 0, 1, 2, 3
```

Delta nhỏ hơn giá trị gốc nên dễ nén hơn.

### 6.4 Bit-packing

Nếu cột chỉ có ít giá trị, ví dụ status có 4 trạng thái:

```text
NEW, PAID, FAILED, CANCELLED
```

Dictionary:

```text
0 = NEW
1 = PAID
2 = FAILED
3 = CANCELLED
```

Chỉ cần 2 bits để biểu diễn mỗi value:

```text
00, 01, 10, 11
```

Thay vì dùng 4 bytes hoặc 8 bytes mỗi value, ColumnStore có thể dùng rất ít bit.

## 7. Encoding có chỉ giúp giảm disk không?

Không. Encoding giúp giảm disk, nhưng trong OLAP, giảm disk cũng chính là tăng tốc query.

Lý do:

### 7.1 Giảm I/O

Nếu cột `category` là string trung bình 20 bytes/value:

```text
1 tỷ row * 20 bytes = khoảng 20 GB
```

Dictionary encode thành 2 bytes/value:

```text
1 tỷ row * 2 bytes = khoảng 2 GB
```

Scan 2 GB nhanh hơn scan 20 GB.

### 7.2 Giảm network

Nếu compute đọc data từ object storage hoặc shared storage qua network, dữ liệu nén nhỏ hơn nghĩa là ít network transfer hơn.

### 7.3 Filter nhanh hơn

So sánh:

```text
category_code == 17
```

thường nhanh hơn so sánh:

```text
category_string == "smartphone-accessories"
```

### 7.4 GROUP BY nhanh hơn

Group trên integer code:

```text
17 -> sum
42 -> sum
```

thường nhanh hơn group trên string:

```text
"smartphone-accessories" -> sum
"gaming-laptop" -> sum
```

Cuối query mới map code về string.

### 7.5 Một số encoding cho phép tính trực tiếp

Nếu RLE có:

```text
phone x 1,000,000
laptop x 500,000
```

Query:

```sql
SELECT category, COUNT(*)
FROM sales
GROUP BY category;
```

Engine có thể cộng:

```text
phone += 1,000,000
laptop += 500,000
```

không cần xử lý từng row logic.

## 8. Ví dụ kinh điển: ColumnStore vs RowStore theo từng pattern query

Không nên hiểu đơn giản là:

```text
GROUP BY -> ColumnStore luôn nhanh hơn
SELECT theo id -> RowStore luôn nhanh hơn
```

Đúng hơn là phải nhìn vào pattern query:

- Query đọc bao nhiêu row?
- Query dùng bao nhiêu cột?
- Có index lọc tốt không?
- Dữ liệu có nằm trong RAM/cache không?
- I/O pattern là sequential scan hay random lookup?
- Query là point lookup, range scan, aggregate, sort, join hay export chi tiết?
- Kết quả group có bao nhiêu nhóm?
- Query có cần dựng lại toàn bộ row không?

Các ví dụ dưới đây không chỉ nói về `GROUP BY`, mà bao gồm cả query cơ bản và aggregate thường gặp như `COUNT`, `SUM`, `AVG`, `MIN/MAX`, `ORDER BY LIMIT`, join và `SELECT *`.

### 8.1 Vector trong database là gì?

Trong ngữ cảnh ColumnStore, **vector** không phải là vector embedding hay AI vector. Vector ở đây nghĩa là: database xử lý dữ liệu theo **một batch giá trị liên tiếp của cùng một cột**.

Ví dụ cột `amount`:

```text
amount:
100, 200, 150, 80, 300, 50, 120, 90, ...
```

Thay vì xử lý từng row một:

```text
row 1 -> đọc amount -> xử lý
row 2 -> đọc amount -> xử lý
row 3 -> đọc amount -> xử lý
```

ColumnStore thường đọc một batch nhiều giá trị:

```text
amount vector batch:
[100, 200, 150, 80, 300, 50, 120, 90, ...]
```

Sau đó CPU xử lý cả batch:

```text
sum batch
filter batch
compare batch
apply mask
```

Ý nghĩa thực tế:

- Dữ liệu cùng kiểu nằm gần nhau.
- CPU cache hiệu quả hơn.
- Ít phải nhảy qua nhiều field khác nhau trong row.
- Dễ dùng SIMD/vectorized execution nếu engine hỗ trợ.
- Giảm overhead gọi hàm/xử lý từng row.

So sánh:

```text
Row-at-a-time:
  đọc row 1 -> lấy order_date -> lấy amount -> xử lý
  đọc row 2 -> lấy order_date -> lấy amount -> xử lý
  đọc row 3 -> lấy order_date -> lấy amount -> xử lý

Vectorized:
  đọc 4096 order_date
  đọc 4096 amount
  tạo filter mask
  sum các amount thỏa mask
```

Ví dụ filter:

```text
order_date vector:
[2026-01-01, 2026-01-02, 2026-02-01, 2026-01-10]

Điều kiện:
order_date trong tháng 1

filter mask:
[true, true, false, true]
```

Sau đó áp mask lên `amount`:

```text
amount vector:
[100, 200, 300, 400]

mask:
[true, true, false, true]

amount được dùng:
[100, 200, 400]
```

Đây là lý do ColumnStore hay nói tới vector scan/vectorized execution.

### 8.2 ColumnStore thật sự lấy từng cột như thế nào?

Giả sử có bảng:

```text
orders(id, order_date, region, amount, status, customer_name, address)
```

Dữ liệu được chia thành segment:

```text
Segment 1: row position 1 -> 1,000,000
Segment 2: row position 1,000,001 -> 2,000,000
```

Trong mỗi segment, từng cột có chunk riêng:

```text
Segment 1
  id chunk
  order_date chunk
  region chunk
  amount chunk
  status chunk
  customer_name chunk
  address chunk
```

Mỗi chunk lại có thể gồm nhiều page/block:

```text
Segment 1
  order_date chunk
    page 1
    page 2
    page 3

  amount chunk
    page 1
    page 2
    page 3

  region chunk
    page 1
    page 2
    page 3
```

Nếu query là:

```sql
SELECT region, SUM(amount)
FROM orders
WHERE order_date BETWEEN '2026-01-01' AND '2026-01-31'
GROUP BY region;
```

Engine không đọc `customer_name`, `address`, `status` nếu chúng không cần cho query.

Behavior thường là:

```text
1. Xác định các cột query cần:
   - order_date để filter
   - region để group
   - amount để sum

2. Duyệt từng segment:
   - kiểm tra metadata min/max của order_date
   - segment nào chắc chắn ngoài tháng 1 thì skip

3. Với segment còn lại:
   - đọc order_date chunk theo batch/vector
   - tạo filter mask

4. Dựa vào filter mask:
   - đọc hoặc sử dụng region chunk tại các position tương ứng
   - đọc hoặc sử dụng amount chunk tại các position tương ứng

5. Aggregate:
   - region_code -> SUM(amount)

6. Cuối cùng:
   - map region_code về region string nếu có dictionary encoding
```

Điểm quan trọng: các cột không nằm chung page, nhưng cùng **row position**.

Ví dụ:

```text
position:    1          2          3          4
order_date:  2026-01-01 2026-02-01 2026-01-05 2026-01-20
region:      HCM        HN         HCM        DN
amount:      100        200        150        80
```

Filter tháng 1 tạo mask:

```text
position:    1     2      3     4
mask:        true  false  true  true
```

Engine lấy `region` và `amount` ở cùng position có mask `true`:

```text
position 1 -> region=HCM, amount=100
position 3 -> region=HCM, amount=150
position 4 -> region=DN,  amount=80
```

Aggregate:

```text
HCM -> 250
DN  -> 80
```

Không có join giữa các cột. Việc "ghép" cột là dựa vào position.

### 8.3 Behavior khi query chỉ cần một cột

Query:

```sql
SELECT COUNT(*)
FROM orders
WHERE order_date >= '2026-01-01';
```

ColumnStore cần chủ yếu cột:

```text
order_date
```

Behavior:

```text
1. Đọc metadata segment của order_date.
2. Skip segment chắc chắn không thỏa.
3. Đọc order_date chunk của segment còn lại.
4. Tạo mask hoặc đếm trực tiếp các value thỏa.
5. Không đọc amount, region, customer_name, address.
```

I/O pattern:

```text
đọc một cột
đọc tuần tự theo chunk/page
ít random I/O
ít byte thừa
```

RowStore nếu không có index phải đọc page chứa full row, dù query chỉ cần `order_date`.

### 8.4 Behavior khi query cần filter một cột, aggregate một cột

Query:

```sql
SELECT SUM(amount)
FROM orders
WHERE order_date >= '2026-01-01';
```

ColumnStore cần:

```text
order_date
amount
```

Behavior:

```text
1. Đọc order_date theo batch.
2. Tạo mask: row nào thỏa điều kiện.
3. Đọc amount theo cùng batch/position.
4. Chỉ cộng amount ở position có mask true.
```

Ví dụ:

```text
order_date vector:
[2025-12-30, 2026-01-01, 2026-01-02, 2025-11-01]

mask:
[false, true, true, false]

amount vector:
[50, 100, 200, 300]

SUM chỉ dùng:
100 + 200 = 300
```

Nếu filter rất selectivity thấp, ví dụ chỉ lấy 0.001% row, RowStore có index tốt có thể nhanh hơn vì nó nhảy thẳng tới vài row. Nhưng nếu filter lấy 10%, 30%, 70% bảng, ColumnStore thường hợp hơn vì scan cột tuần tự hiệu quả.

### 8.5 Behavior khi query cần nhiều cột để SELECT chi tiết

Query:

```sql
SELECT id, order_date, region, amount, status, customer_name, address
FROM orders
WHERE id = 123;
```

Đây là case bất lợi cho ColumnStore.

ColumnStore phải:

```text
1. Tìm id=123 nằm ở segment/position nào.
2. Với position đó, lấy giá trị từ nhiều cột:
   - id[position]
   - order_date[position]
   - region[position]
   - amount[position]
   - status[position]
   - customer_name[position]
   - address[position]
3. Dựng lại row kết quả.
```

I/O pattern:

```text
nhiều access nhỏ tới nhiều column chunk
khó tận dụng sequential scan lớn
```

RowStore tốt hơn vì toàn bộ row thường nằm gần nhau:

```text
id=123 -> page X -> offset Y -> lấy full row
```

Kết luận:

```text
ColumnStore mạnh khi đọc ít cột của nhiều row.
RowStore mạnh khi đọc nhiều cột của ít row.
```

### 8.6 Behavior khi query cần GROUP BY

Query:

```sql
SELECT region, SUM(amount)
FROM orders
WHERE order_date >= '2026-01-01'
GROUP BY region;
```

ColumnStore cần:

```text
order_date
region
amount
```

Nếu `region` dùng dictionary encoding:

```text
1 = HCM
2 = HN
3 = DN
```

Thì `region` chunk có thể là:

```text
1, 2, 1, 3, 1, 2
```

Behavior:

```text
1. Đọc order_date batch.
2. Tạo mask.
3. Đọc region_code batch.
4. Đọc amount batch.
5. Với mỗi position thỏa mask:
   hash_table[region_code].sum += amount
6. Cuối query map code về string.
```

Ví dụ:

```text
position:       1     2     3     4
order_date ok:  yes   no    yes   yes
region_code:    1     2     1     3
amount:         100   200   150   80

Aggregate:
1 -> 250
3 -> 80

Map:
1 -> HCM
3 -> DN
```

RAM dùng cho:

```text
batch/vector đang xử lý
filter mask
hash table aggregate
dictionary nhỏ nếu cần
```

Nếu số group ít như `region`, `status`, `category`, hash table nhỏ. Nếu group theo `user_id` có hàng trăm triệu key, hash table rất lớn và có thể spill ra disk.

### 8.7 Pattern query 1: COUNT trên bảng lớn

Ví dụ bảng `orders` có 500 triệu row và 80 cột.

Query:

```sql
SELECT COUNT(*)
FROM orders
WHERE order_date BETWEEN '2026-01-01' AND '2026-01-31';
```

Query này không cần đọc toàn bộ row. Nó chủ yếu cần biết dòng nào thỏa `order_date`.

#### RowStore sẽ làm gì?

Nếu không có index tốt trên `order_date`, RowStore phải scan nhiều page chứa full row.

I/O pattern:

```text
scan nhiều row page
mỗi page chứa cả row đầy đủ
đọc nhiều cột không cần dùng
```

RAM/cache pattern:

```text
buffer pool chứa nhiều page full row
cache bị chiếm bởi dữ liệu không cần cho COUNT
```

Nếu có index tốt:

```text
idx_orders_order_date(order_date)
```

RowStore có thể scan index thay vì table, lúc đó nhanh hơn nhiều. Đây là lý do với query đơn giản và index tốt, RowStore vẫn có thể rất ổn.

#### ColumnStore sẽ làm gì?

ColumnStore chỉ cần đọc `order_date` column chunk, hoặc thậm chí dùng metadata/segment statistics nếu đủ.

I/O pattern:

```text
đọc một cột order_date
skip segment ngoài tháng 1
đếm row thỏa điều kiện
```

RAM/cache pattern:

```text
RAM chứa vector order_date và bitmap/filter mask
không cần đưa full row vào cache
```

Kết luận:

```text
COUNT trên rất nhiều row, filter theo cột thời gian -> ColumnStore thường rất tốt.
Nhưng RowStore + index phù hợp vẫn có thể đủ nhanh nếu query đơn giản và selectivity tốt.
```

### 8.8 Pattern query 2: SUM/AVG trên bảng rất lớn, không cần GROUP BY

Query:

```sql
SELECT SUM(amount), AVG(amount)
FROM orders
WHERE order_date BETWEEN '2026-01-01' AND '2026-01-31';
```

Query chỉ cần 2 cột:

```text
order_date
amount
```

#### RowStore

Nếu scan table, RowStore phải đọc page chứa full row:

```text
id, customer_id, name, phone, address, note, status, region, amount, order_date, ...
```

I/O pattern:

```text
sequential scan nhiều page lớn
đọc nhiều byte thừa
```

CPU pattern:

```text
lọc order_date
lấy amount
cộng amount
bỏ qua phần lớn cột khác
```

#### ColumnStore

ColumnStore đọc:

```text
order_date chunk
amount chunk
```

I/O pattern:

```text
sequential scan 2 cột
đọc ít byte hơn nhiều
```

RAM/CPU pattern:

```text
amount là numeric vector
CPU có thể cộng theo batch/vector
RAM không phải chứa full row
```

Kết luận:

```text
SUM/AVG trên nhiều row và ít cột -> ColumnStore rất hợp, kể cả không có GROUP BY.
```

### 8.9 Pattern query 3: Aggregate có GROUP BY trên bảng rất lớn, chỉ dùng vài cột

Ví dụ bảng `orders` có 500 triệu row và 80 cột.

Query:

```sql
SELECT region, SUM(amount)
FROM orders
WHERE order_date BETWEEN '2026-01-01' AND '2026-01-31'
GROUP BY region;
```

Query chỉ cần 3 cột:

```text
region
amount
order_date
```

#### RowStore sẽ làm gì?

RowStore lưu theo dòng. Mỗi page chứa nhiều row đầy đủ:

```text
page 1:
  row1: id, customer_id, name, phone, address, note, region, amount, order_date, ...
  row2: id, customer_id, name, phone, address, note, region, amount, order_date, ...
```

Nếu không có index đủ tốt để lọc rất ít row, database phải scan rất nhiều row. Khi đọc page, nó kéo theo cả các cột không cần:

```text
name
phone
address
note
payment_method
shipping_info
...
```

I/O pattern:

```text
sequential scan nhiều page row-store
đọc nhiều byte không cần thiết
```

RAM/cache pattern:

```text
buffer pool bị lấp bởi page chứa full row
cache chứa nhiều dữ liệu query không dùng
ít hiệu quả nếu bảng lớn hơn RAM
```

CPU pattern:

```text
CPU phải đọc/parse row layout
lọc order_date
lấy region và amount
bỏ qua phần lớn cột còn lại
```

#### ColumnStore sẽ làm gì?

ColumnStore chỉ đọc column chunk cần thiết:

```text
order_date chunk
region chunk
amount chunk
```

I/O pattern:

```text
sequential scan một vài cột
đọc ít byte hơn nhiều
skip segment không thuộc tháng 1 nếu metadata cho phép
```

RAM/cache pattern:

```text
RAM chứa đúng các vector/cột đang cần xử lý
ít làm bẩn cache bởi cột không liên quan
có thể xử lý theo batch/vector hiệu quả hơn
```

CPU pattern:

```text
filter order_date theo vector
group region bằng dictionary code nếu có
sum amount theo vector numeric
```

Kết luận:

```text
Case này ColumnStore thường thắng rõ.
Lý do chính không chỉ là `GROUP BY`, mà là đọc rất nhiều row nhưng chỉ cần rất ít cột.
```

### 8.10 Pattern query 4: Aggregate sau khi index đã lọc còn rất ít row

Ví dụ:

```sql
SELECT status, COUNT(*)
FROM orders
WHERE customer_id = 123
GROUP BY status;
```

Giả sử `customer_id = 123` chỉ có 20 đơn hàng và RowStore có index:

```text
idx_orders_customer_id(customer_id)
```

#### RowStore sẽ làm gì?

I/O pattern:

```text
index lookup customer_id = 123
đọc khoảng vài page chứa 20 row
```

RAM/cache pattern:

```text
chỉ cần vài page
dễ nằm sẵn trong buffer pool
```

CPU pattern:

```text
group 20 row theo status
chi phí gần như không đáng kể
```

#### ColumnStore sẽ làm gì?

ColumnStore có thể không có lợi nhiều vì workload quá nhỏ. Nó vẫn phải xác định segment/chunk, đọc cột liên quan, lọc theo `customer_id`, rồi group.

I/O pattern có thể là:

```text
đọc customer_id chunk
đọc status chunk
lọc ra 20 vị trí
```

Nếu segment lớn và filter không giúp skip tốt, lượng dữ liệu đọc có thể nhiều hơn RowStore.

Kết luận:

```text
Case này RowStore thường nhanh hơn.
Aggregate hoặc GROUP BY không đủ để kết luận nên dùng ColumnStore.
Nếu index lọc còn rất ít row, RowStore rất mạnh.
```

### 8.11 Pattern query 5: Point lookup theo primary key

Ví dụ:

```sql
SELECT *
FROM users
WHERE id = 999;
```

#### RowStore

RowStore có primary key index:

```text
id = 999 -> page X -> row offset Y
```

I/O pattern:

```text
random lookup rất nhỏ
đọc 1 hoặc vài page
```

RAM/cache pattern:

```text
page nóng có thể đã nằm trong buffer pool
lookup cực nhanh
```

Vì query `SELECT *`, row-store có lợi vì toàn bộ cột của user nằm gần nhau trong cùng row/page.

#### ColumnStore

ColumnStore lưu mỗi cột riêng:

```text
id chunk
name chunk
email chunk
phone chunk
address chunk
created_at chunk
...
```

Nếu query `SELECT *`, engine phải dựng lại row từ nhiều cột:

```text
name[position]
email[position]
phone[position]
address[position]
...
```

I/O pattern:

```text
nhiều access nhỏ tới nhiều column chunk
không hợp với layout columnar
```

Kết luận:

```text
Point lookup hoặc SELECT * theo primary key -> RowStore thắng.
```

### 8.12 Pattern query 6: Query cần nhiều row nhưng gần như tất cả cột

Ví dụ export dữ liệu chi tiết:

```sql
SELECT *
FROM orders
WHERE order_date = '2026-01-01';
```

Giả sử ngày đó có 2 triệu order và bảng có 80 cột.

#### RowStore

RowStore đọc row page. Vì query cần gần như tất cả cột, việc dữ liệu nằm cùng row là lợi thế.

I/O pattern:

```text
scan các page chứa row của ngày đó
đọc một lần là có đủ nhiều cột
```

#### ColumnStore

ColumnStore phải đọc nhiều column chunk:

```text
id
customer_id
name
phone
address
status
amount
...
80 cột
```

Sau đó phải dựng lại row từ các position.

I/O pattern:

```text
đọc rất nhiều cột riêng biệt
reconstruct row
```

Kết luận:

```text
Nếu query cần gần như tất cả cột, lợi thế "chỉ đọc cột cần thiết" của ColumnStore biến mất.
RowStore có thể cạnh tranh tốt hoặc nhanh hơn, tùy storage/cache.
```

### 8.13 Pattern query 7: MIN/MAX theo cột có metadata hoặc index

Ví dụ:

```sql
SELECT MIN(order_date), MAX(order_date)
FROM orders;
```

Hoặc:

```sql
SELECT MAX(amount)
FROM orders
WHERE order_date >= '2026-01-01';
```

#### RowStore

Nếu có index phù hợp:

```text
idx_orders_order_date(order_date)
idx_orders_amount(amount)
```

RowStore có thể lấy min/max rất nhanh bằng cách đi tới đầu/cuối index.

I/O pattern:

```text
đọc rất ít index page
random lookup nhỏ
```

RAM pattern:

```text
index page nóng có thể nằm sẵn trong buffer pool
```

#### ColumnStore

ColumnStore thường có metadata min/max theo segment.

Ví dụ:

```text
Segment 1: amount min=10,  max=500
Segment 2: amount min=20,  max=900
Segment 3: amount min=5,   max=700
```

Query `MAX(amount)` có thể dùng metadata để giảm lượng data cần đọc, tùy engine và điều kiện filter.

I/O pattern:

```text
đọc metadata segment
chỉ đọc data segment/cột khi cần xác nhận hoặc filter phức tạp
```

Kết luận:

```text
MIN/MAX đơn giản trên cột có index -> RowStore có thể cực nhanh.
MIN/MAX hoặc filter trên data lớn theo segment/time range -> ColumnStore cũng rất tốt nhờ metadata.
```

### 8.14 Pattern query 8: ORDER BY + LIMIT

Ví dụ OLTP:

```sql
SELECT *
FROM orders
WHERE customer_id = 123
ORDER BY created_at DESC
LIMIT 20;
```

Nếu RowStore có index:

```text
(customer_id, created_at)
```

RowStore rất mạnh.

I/O pattern:

```text
đi theo index đúng customer_id
lấy 20 row mới nhất
đọc rất ít page
```

Đây là query phục vụ màn hình app, không phải analytical query.

Ví dụ analytical:

```sql
SELECT product_id, SUM(amount) AS revenue
FROM order_items
WHERE order_date >= '2026-01-01'
GROUP BY product_id
ORDER BY revenue DESC
LIMIT 100;
```

Query này phải:

```text
scan rất nhiều order_items
tính SUM theo product_id
sort/top-N theo revenue
```

ColumnStore hợp hơn vì phần nặng nhất là scan/aggregate trên nhiều row và ít cột.

Kết luận:

```text
ORDER BY LIMIT trên index để lấy vài record mới nhất -> RowStore.
ORDER BY LIMIT sau aggregate lớn -> ColumnStore.
```

### 8.15 Pattern query 9: Join fact table lớn với dimension table nhỏ

Ví dụ data warehouse:

```sql
SELECT d.month, p.category, SUM(f.revenue)
FROM fact_orders f
JOIN dim_date d ON f.date_id = d.id
JOIN dim_product p ON f.product_id = p.id
WHERE d.year = 2026
GROUP BY d.month, p.category;
```

Đây là pattern kinh điển của OLAP:

```text
fact_orders rất lớn
dim_date nhỏ
dim_product nhỏ hơn nhiều so với fact
query chỉ cần vài cột từ fact
```

ColumnStore có lợi vì:

- Scan các cột cần thiết của fact table.
- Dimension nhỏ có thể được hash/join trong memory.
- Aggregate sau join.
- Dữ liệu fact có thể được segment theo thời gian.

RAM pattern:

```text
RAM giữ hash table dimension nhỏ
RAM giữ hash table aggregate kết quả
fact table được scan theo vector/batch
```

RowStore vẫn có thể tốt nếu query rất selective và index join/filter cực tốt. Nhưng với fact table lớn và báo cáo nhiều chiều, RowStore thường cần nhiều index/materialized summary hơn.

Kết luận:

```text
Join fact lớn + dimension nhỏ + aggregate -> ColumnStore/data warehouse pattern.
Join vài row theo key trong transaction runtime -> RowStore pattern.
```

### 8.16 Pattern query 10: Dashboard aggregate lặp lại nhiều lần

Ví dụ dashboard mỗi phút chạy:

```sql
SELECT product_category, COUNT(*) AS orders, SUM(amount) AS revenue
FROM orders
WHERE order_date >= CURRENT_DATE - INTERVAL 30 DAY
GROUP BY product_category
ORDER BY revenue DESC;
```

Đây là workload rất hợp ColumnStore.

Lý do:

- Query đọc rất nhiều row trong 30 ngày.
- Chỉ dùng vài cột.
- `product_category` có thể dictionary encoded.
- `amount` là numeric column, dễ vectorize.
- `order_date` giúp skip segment/partition.
- Kết quả group thường ít hơn số row rất nhiều.

I/O pattern ColumnStore:

```text
đọc tuần tự các column chunk của 3 cột
ít random I/O
ít byte thừa
```

RAM pattern:

```text
RAM dùng cho vector batch và hash table aggregate
không cần cache full row
```

Hash aggregate trong RAM:

```text
category_code -> count, sum
```

Ví dụ:

```text
17 -> count=10,000,000, sum=500,000,000
42 -> count=5,000,000,  sum=230,000,000
```

Cuối query mới map:

```text
17 -> phone
42 -> laptop
```

Kết luận:

```text
Dashboard aggregate trên data lớn -> ColumnStore rất phù hợp.
Nếu chạy trực tiếp trên OLTP RowStore, dashboard có thể làm nghẽn I/O và buffer pool của app chính.
```

### 8.17 Pattern query 11: GROUP BY cardinality rất cao

Không phải cứ ColumnStore là GROUP BY nào cũng nhẹ.

Ví dụ:

```sql
SELECT user_id, SUM(amount)
FROM orders
WHERE order_date >= '2026-01-01'
GROUP BY user_id;
```

Nếu có 200 triệu `user_id` khác nhau, hash table aggregate có thể rất lớn.

RAM pattern:

```text
hash table có hàng chục/hàng trăm triệu key
có thể vượt RAM
phải spill ra disk
```

ColumnStore vẫn có lợi ở phần scan ít cột:

```text
user_id
amount
order_date
```

Nhưng phần aggregate vẫn nặng vì số group quá lớn.

Kết luận:

```text
ColumnStore giảm chi phí đọc dữ liệu, nhưng không xóa bỏ chi phí group cardinality cao.
GROUP BY ít nhóm như region/category/status thường nhẹ hơn GROUP BY user_id/order_id.
```

### 8.18 Pattern query 12: Query có index covering trong RowStore

RowStore có thể rất nhanh nếu index đã bao phủ đủ cột.

Ví dụ:

```sql
SELECT status, COUNT(*)
FROM orders
WHERE order_date >= '2026-01-01'
GROUP BY status;
```

Nếu có covering index:

```text
(order_date, status)
```

RowStore có thể chỉ scan index, không cần đọc full row.

I/O pattern:

```text
scan index nhỏ hơn table
đọc ít page hơn full row
```

Trong case này RowStore có thể khá tốt.

Nhưng nếu dashboard có nhiều query khác nhau:

```text
GROUP BY region
GROUP BY category
GROUP BY payment_method
GROUP BY campaign_id
GROUP BY device
```

Bạn có thể phải tạo rất nhiều index:

```text
(order_date, region)
(order_date, category)
(order_date, payment_method)
(order_date, campaign_id)
(order_date, device)
...
```

Hệ quả:

- Tốn storage.
- Insert/update chậm hơn vì phải cập nhật nhiều index.
- Buffer pool bị chia cho nhiều index.
- Tối ưu cho query này có thể không tối ưu cho query khác.

ColumnStore giải quyết tự nhiên hơn vì nó đã lưu từng cột riêng.

Kết luận:

```text
Một vài report cố định -> RowStore + covering index có thể đủ.
Nhiều chiều phân tích linh hoạt trên data lớn -> ColumnStore hợp hơn.
```

### 8.19 Tổng kết chọn RowStore hay ColumnStore theo I/O và RAM

| Tình huống | RowStore | ColumnStore |
|---|---|---|
| `SELECT * WHERE id = ?` | Rất mạnh | Yếu hơn |
| Update một row theo primary key | Rất mạnh | Không tối ưu |
| Query lấy vài chục row qua index | Rất mạnh | Thường không đáng |
| `COUNT(*)` trên bảng lớn/time range lớn | Tốt nếu có index phù hợp | Rất mạnh nếu scan nhiều row |
| `SUM/AVG` trên nhiều row, ít cột | Dễ nặng I/O nếu scan table | Rất mạnh |
| Aggregate hàng trăm triệu row, ít cột | Dễ nặng I/O | Rất mạnh |
| Dashboard nhiều `SUM/COUNT/GROUP BY` | Có thể ảnh hưởng OLTP | Phù hợp |
| `MIN/MAX` đơn giản trên index | Rất mạnh | Tốt nếu tận dụng metadata |
| `ORDER BY LIMIT` lấy vài record qua index | Rất mạnh | Không phải điểm mạnh |
| `ORDER BY LIMIT` sau aggregate lớn | Nặng nếu scan/sort lớn | Phù hợp hơn |
| Join vài row theo key | Rất mạnh | Không tối ưu |
| Join fact lớn với dimension nhỏ rồi aggregate | Thường nặng nếu data lớn | Phù hợp |
| Query cần gần như tất cả cột | Có lợi vì row nằm chung | Lợi thế giảm |
| GROUP BY ít nhóm | Tốt nếu data nhỏ/index tốt | Rất tốt nếu data lớn |
| GROUP BY cực nhiều nhóm | Có thể nặng RAM | Vẫn nặng RAM, dù scan tốt hơn |
| Nhiều report theo nhiều chiều | Cần nhiều index | Tự nhiên hơn vì lưu theo cột |

Quy tắc thực tế:

```text
Nếu I/O pattern là random lookup nhỏ -> RowStore.
Nếu I/O pattern là sequential scan lớn trên vài cột -> ColumnStore.

Nếu RAM chủ yếu cần cache hot rows/index -> RowStore.
Nếu RAM chủ yếu dùng cho vector scan và hash aggregate -> ColumnStore.

Nếu query trả lời "record này là gì?" -> RowStore.
Nếu query trả lời "tổng/đếm/trung bình/min/max/top-N trên rất nhiều record là bao nhiêu?" -> ColumnStore.
```

### 8.20 Đánh giá theo lượng data, loại data và behavior query

Khi chọn RowStore hay ColumnStore, không nên chỉ nhìn vào câu SQL có `GROUP BY` hay không. Cần nhìn theo 3 trục:

```text
1. Lượng data query phải đọc.
2. Loại data trong bảng.
3. Behavior query truy cập data như thế nào.
```

#### 8.20.1 Lượng data

Các mốc dưới đây không phải luật cứng, nhưng là rule of thumb thực tế.

| Lượng data / bảng chính | Nhận xét |
|---|---|
| Dưới 10 GB | RowStore thường đủ, trừ khi dashboard query rất nặng hoặc chạy liên tục |
| 10 - 100 GB | Bắt đầu cần xem query có scan nhiều không, có ảnh hưởng OLTP không |
| 100 GB - vài TB | Nên cân nhắc OLAP/ColumnStore nếu có báo cáo nhiều chiều |
| Vài TB trở lên | Thường nên có hệ OLAP riêng, ColumnStore/data warehouse là lựa chọn tự nhiên hơn |

Quan trọng hơn tổng dung lượng database là dung lượng **fact table** hoặc bảng bị scan nhiều.

Ví dụ:

```text
orders:       80 triệu row
order_items:  500 triệu row
events:       5 tỷ row
```

Dù database tổng chưa quá lớn, nếu dashboard thường scan `order_items` hoặc `events`, đó đã là workload OLAP.

#### 8.20.2 Tỷ lệ dữ liệu bị đọc trong mỗi query

Đây là yếu tố rất quan trọng.

| Query đọc bao nhiêu dữ liệu? | Thường hợp với |
|---|---|
| 1 row | RowStore |
| Vài chục/vài trăm row qua index | RowStore |
| Dưới 0.1% bảng và có index tốt | RowStore |
| 1% - 5% bảng | Tùy index, cột cần đọc, cache và storage |
| 10% - 100% bảng | ColumnStore thường đáng cân nhắc |

Ví dụ RowStore mạnh:

```sql
SELECT *
FROM orders
WHERE id = 123;
```

Query đọc 1 row.

Ví dụ ColumnStore mạnh:

```sql
SELECT SUM(amount)
FROM orders
WHERE order_date >= '2026-01-01';
```

Nếu điều kiện này quét 40% bảng, ColumnStore hợp hơn vì scan ít cột tuần tự.

#### 8.20.3 Số cột query cần đọc

ColumnStore có lợi nhất khi:

```text
bảng nhiều cột
query chỉ dùng ít cột
query đọc nhiều row
```

Ví dụ bảng 100 cột nhưng query chỉ dùng 3 cột:

```sql
SELECT region, SUM(amount)
FROM orders
WHERE order_date >= '2026-01-01'
GROUP BY region;
```

ColumnStore đọc:

```text
region
amount
order_date
```

RowStore có thể phải đọc page chứa cả row, kéo theo nhiều cột không dùng.

Ngược lại, nếu query cần gần như tất cả cột:

```sql
SELECT *
FROM orders
WHERE order_date = '2026-01-01';
```

ColumnStore phải đọc nhiều column chunk rồi dựng lại row. Lợi thế giảm rõ.

Rule of thumb:

```text
Query dùng < 10% số cột và đọc nhiều row -> ColumnStore có lợi lớn.
Query dùng 70-100% số cột -> RowStore có thể cạnh tranh tốt hơn.
```

#### 8.20.4 Loại data nào hợp ColumnStore?

ColumnStore hợp với data có tính lặp, tính cột rõ, và ít update từng dòng.

Ví dụ data rất hợp:

```text
status: NEW, PAID, CANCELLED, REFUNDED
region: HCM, HN, DN, CT
category: phone, laptop, accessory
order_date: ngày/tháng tăng dần
amount: numeric
event_type: view, click, add_to_cart, purchase
```

Lý do:

- `status`, `region`, `category`, `event_type` có cardinality thấp hoặc vừa, dictionary encoding tốt.
- `order_date` giúp partition/segment pruning tốt.
- `amount`, `quantity`, `revenue` là numeric, aggregate tốt.
- Dữ liệu event/order lịch sử thường append nhiều hơn update.

ColumnStore kém lợi hơn với:

```text
text dài unique
JSON blob lớn
address dài
note/comment tự do
payload khác nhau nhiều
ảnh/file/binary blob
```

Không phải không lưu được, nhưng query phân tích thường không scan toàn bộ các cột này. Nếu thường xuyên `SELECT *` lấy text/blob chi tiết, RowStore hoặc storage khác có thể hợp hơn.

#### 8.20.5 Cardinality của cột

Cardinality là số lượng giá trị khác nhau trong một cột.

Cardinality thấp:

```text
status: 5 giá trị
region: 63 tỉnh/thành
payment_method: 10 loại
```

Rất hợp ColumnStore:

- Nén tốt.
- Dictionary nhỏ.
- GROUP BY nhẹ.
- Hash table aggregate nhỏ.

Cardinality cao:

```text
user_id: 200 triệu giá trị
order_id: 1 tỷ giá trị
request_id: gần như unique
```

ColumnStore vẫn có thể scan nhanh, nhưng aggregate/group theo cột này nặng RAM hơn.

Ví dụ:

```sql
SELECT user_id, SUM(amount)
FROM orders
GROUP BY user_id;
```

Behavior:

```text
scan ít cột: tốt
hash table có rất nhiều key: nặng RAM
có thể spill ra disk
```

Vì vậy cần phân biệt:

```text
ColumnStore giúp đọc nhanh hơn.
Nhưng số group quá lớn vẫn có thể làm aggregate nặng.
```

#### 8.20.6 Behavior query RowStore tối ưu nhất

RowStore tối ưu nhất cho behavior:

```text
random lookup nhỏ
update/insert/delete từng row
transaction nhiều
SELECT * theo primary key
range query nhỏ qua index
ORDER BY LIMIT qua index
```

Ví dụ:

```sql
SELECT *
FROM orders
WHERE id = 123;
```

Behavior:

```text
1. Dùng B-tree primary key.
2. Tìm page chứa row.
3. Đọc một page hoặc vài page.
4. Trả full row.
```

I/O:

```text
random I/O nhỏ
ít page
```

RAM:

```text
buffer pool giữ hot rows và hot index pages
```

Ví dụ:

```sql
SELECT *
FROM orders
WHERE customer_id = 123
ORDER BY created_at DESC
LIMIT 20;
```

Nếu có index:

```text
(customer_id, created_at)
```

RowStore gần như đi đúng đường:

```text
tìm customer_id
đi theo created_at desc
lấy 20 row
dừng
```

Đây là kiểu query ColumnStore không tối ưu bằng.

#### 8.20.7 Behavior query ColumnStore tối ưu nhất

ColumnStore tối ưu nhất cho behavior:

```text
scan nhiều row
đọc ít cột
filter theo time range/partition
aggregate numeric
GROUP BY cột low/medium cardinality
dashboard lặp lại
report nhiều chiều
join fact lớn với dimension nhỏ
```

Ví dụ:

```sql
SELECT product_category, COUNT(*), SUM(amount)
FROM orders
WHERE order_date >= '2026-01-01'
GROUP BY product_category;
```

Behavior:

```text
1. Đọc metadata segment.
2. Skip segment ngoài khoảng thời gian.
3. Đọc order_date chunk để tạo filter mask.
4. Đọc product_category chunk theo position.
5. Đọc amount chunk theo position.
6. Aggregate vào hash table.
7. Trả kết quả nhỏ hơn rất nhiều so với input.
```

I/O:

```text
sequential scan vài cột
ít byte thừa
ít random I/O
```

RAM:

```text
vector batch
filter mask
hash table aggregate
dictionary nhỏ
```

Kết quả:

```text
input: 500 triệu row
output: 20 category
```

Đây là kiểu query ColumnStore được sinh ra để xử lý.

#### 8.20.8 Query behavior dễ gây hiểu nhầm

Không phải query có `WHERE` là RowStore tốt.

Ví dụ:

```sql
SELECT SUM(amount)
FROM orders
WHERE order_date >= '2026-01-01';
```

Nếu `order_date >= '2026-01-01'` lấy 60% bảng, index RowStore không còn quá lợi. Database có thể phải đọc rất nhiều row. ColumnStore thường tốt hơn vì scan 2 cột tuần tự.

Không phải query có `GROUP BY` là ColumnStore luôn tốt.

Ví dụ:

```sql
SELECT status, COUNT(*)
FROM orders
WHERE customer_id = 123
GROUP BY status;
```

Nếu `customer_id = 123` chỉ có 20 row và có index tốt, RowStore nhanh hơn.

Không phải `SELECT *` là luôn xấu.

Nếu query:

```sql
SELECT *
FROM orders
WHERE id = 123;
```

RowStore rất tốt.

Nhưng nếu query:

```sql
SELECT *
FROM orders
WHERE order_date >= '2026-01-01';
```

và trả hàng triệu row, cả RowStore lẫn ColumnStore đều có thể nặng, vì output quá lớn. Vấn đề không còn chỉ là storage layout, mà là query đang đẩy quá nhiều dữ liệu ra ngoài.

#### 8.20.9 Checklist tối ưu RowStore

RowStore nên tối ưu bằng:

- Primary key đúng cho lookup.
- Index theo access pattern thật.
- Composite index cho filter + sort.
- Covering index cho report nhỏ/cố định.
- Tránh tạo quá nhiều index làm chậm write.
- Giữ hot data/index trong buffer pool.
- Tách read replica nếu report nhẹ nhưng ảnh hưởng primary.

Ví dụ index tốt:

```sql
CREATE INDEX idx_orders_customer_created
ON orders(customer_id, created_at);
```

Hợp query:

```sql
SELECT *
FROM orders
WHERE customer_id = 123
ORDER BY created_at DESC
LIMIT 20;
```

#### 8.20.10 Checklist tối ưu ColumnStore

ColumnStore nên tối ưu bằng:

- Thiết kế fact/dimension rõ.
- Chọn cột filter thời gian tốt.
- Partition/segment theo ngày/tháng nếu phù hợp.
- Load dữ liệu theo batch/micro-batch.
- Tránh update từng row liên tục.
- Chỉ select các cột cần thiết.
- Tránh `SELECT *` trong dashboard.
- Dùng cột low/medium cardinality cho dashboard group phổ biến.
- Cân nhắc pre-aggregate/materialized summary cho query lặp lại rất nặng.

Ví dụ tốt:

```sql
SELECT region, product_category, SUM(amount)
FROM fact_orders
WHERE order_date BETWEEN '2026-01-01' AND '2026-01-31'
GROUP BY region, product_category;
```

Ví dụ nên tránh trong dashboard:

```sql
SELECT *
FROM fact_orders
WHERE order_date BETWEEN '2026-01-01' AND '2026-01-31';
```

#### 8.20.11 Ma trận quyết định nhanh

| Câu hỏi | Nếu câu trả lời là có | Nghiêng về |
|---|---|---|
| Query chỉ lấy 1 hoặc vài row? | Có | RowStore |
| Query cần update/insert realtime từng row? | Có | RowStore |
| Query cần `SELECT *` theo id? | Có | RowStore |
| Query scan hàng triệu/hàng tỷ row? | Có | ColumnStore |
| Query chỉ dùng vài cột trong bảng rộng? | Có | ColumnStore |
| Query chủ yếu `COUNT/SUM/AVG/MIN/MAX` trên data lớn? | Có | ColumnStore |
| Query group theo status/region/category? | Có, data lớn | ColumnStore |
| Query group theo user_id/order_id cực lớn? | Có | ColumnStore vẫn giúp scan, nhưng cần kiểm tra RAM/spill |
| Query có index lọc còn rất ít row? | Có | RowStore |
| Report làm chậm OLTP? | Có | Tách OLAP/ColumnStore |

Quy tắc cuối cùng:

```text
RowStore tối ưu cho "tìm và sửa một ít row thật nhanh".
ColumnStore tối ưu cho "đọc rất nhiều row, nhưng chỉ vài cột, rồi tính toán ra kết quả nhỏ".
```

## 9. Vì sao update nhỏ không hợp ColumnStore?

Case OLTP:

```sql
UPDATE orders SET status = 'PAID' WHERE id = 123;
```

### Row-store

Row-store có index:

```text
id=123 -> page X, offset Y
```

Update:

```text
đọc page X vào memory
sửa status tại offset Y
ghi WAL/redo log
commit
flush page sau
```

Row-store được tối ưu cho việc tìm và sửa một row.

### Column-store

ColumnStore phải:

```text
tìm id=123 nằm ở segment nào
tìm row position nào
tìm status column chunk tương ứng
xử lý chunk có thể đang được nén
ghi delta hoặc cơ chế update phụ
cập nhật metadata
merge/compaction về sau
```

Việc update một value nhỏ có thể ảnh hưởng block/segment nén.

## 10. Vì sao sửa một giá trị có thể làm hỏng cấu trúc nén hiện tại?

"Làm hỏng" ở đây không có nghĩa là database bị corrupt. Nghĩa là layout/encoding hiện tại không còn đúng hoặc không còn tối ưu sau khi update.

### 10.1 Ví dụ RLE

Cột `status` ban đầu:

```text
PAID, PAID, PAID, PAID, PAID, PAID, PAID, PAID
```

RLE lưu:

```text
PAID x 8
```

Update dòng thứ 4:

```sql
UPDATE orders SET status = 'FAILED' WHERE id = 4;
```

Dữ liệu logical mới:

```text
PAID, PAID, PAID, FAILED, PAID, PAID, PAID, PAID
```

Encoding mới phải thành:

```text
PAID x 3
FAILED x 1
PAID x 4
```

Suy ra sửa 1 value làm thay đổi cả cấu trúc run.

### 10.2 Ví dụ dictionary + bit-packing

Ban đầu cột `status` có 4 value:

```text
NEW
PAID
FAILED
CANCELLED
```

Dictionary:

```text
0 = NEW
1 = PAID
2 = FAILED
3 = CANCELLED
```

Chỉ cần 2 bits/value.

Nếu update một row thành:

```text
REFUNDED
```

Dictionary cần thêm:

```text
4 = REFUNDED
```

Nhưng 2 bits chỉ biểu diễn được 0..3. Để biểu diễn giá trị 4 cần ít nhất 3 bits. Cả block có thể phải repack từ:

```text
2 bits/value
```

sang:

```text
3 bits/value
```

### 10.3 Ví dụ delta encoding

Cột `amount`:

```text
100, 101, 102, 103, 104
```

Có thể lưu:

```text
base = 100
delta = 0, 1, 2, 3, 4
```

Update:

```text
100, 101, 999999, 103, 104
```

Delta thành:

```text
0, 1, 999899, 3, 4
```

Giá trị delta lớn bất thường làm block cần nhiều bit hơn hoặc phải đổi encoding.

## 11. Delta store là gì?

Vì sửa trực tiếp block nén có thể đắt, nhiều ColumnStore dùng delta store.

Thay vì sửa segment nén ngay lập tức:

```text
Base segment:
row 123 status = NEW
```

Update:

```sql
UPDATE orders SET status = 'PAID' WHERE id = 123;
```

Hệ thống ghi vào delta:

```text
Delta:
row 123 status = PAID
```

Khi query:

```text
base nói: row 123 status = NEW
delta nói: row 123 status = PAID
kết quả đúng: PAID
```

Query phải overlay delta lên base.

Nếu có nhiều update:

```text
Base:
row 123 status = NEW

Delta 1:
row 123 status = PROCESSING

Delta 2:
row 123 status = PAID
```

Kết quả mới nhất là:

```text
PAID
```

## 12. Merge/compaction là gì?

Nếu để delta quá nhiều, query sẽ chậm vì phải đọc base + nhiều delta.

Compaction là quá trình:

```text
đọc base segment cũ
áp dụng các delta mới
ghi ra segment mới đã được nén lại
đánh dấu segment cũ và delta cũ không còn dùng
dọn dẹp về sau
```

Ví dụ trước compaction:

```text
Base segment:
row 123 status = NEW
row 200 status = NEW

Delta:
row 123 status = PAID
row 200 status = CANCELLED
```

Sau compaction:

```text
New base segment:
row 123 status = PAID
row 200 status = CANCELLED
```

Đây là lý do update nhỏ có write amplification cao.

## 13. Write amplification là gì?

User chỉ update một giá trị:

```text
status: NEW -> PAID
```

Nhưng hệ thống có thể phải:

```text
ghi delta record
ghi transaction log
cập nhật metadata
về sau đọc base + delta
ghi lại segment mới
xóa/đánh dấu segment cũ
```

Dữ liệu ghi thật ở storage lớn hơn nhiều so với dữ liệu logic cần sửa.

Do đó:

```text
Update từng dòng liên tục -> RowStore tốt hơn.
Batch load/scan/aggregate -> ColumnStore tốt hơn.
```

## 14. Bulk data import vì sao hợp ColumnStore?

ColumnStore thích dữ liệu vào theo lô lớn.

Ví dụ import 10 triệu dòng một lần:

```text
10 triệu orders mới
```

Engine có thể:

- Gom dữ liệu thành các column chunk.
- Nén từng cột hiệu quả.
- Tạo metadata min/max.
- Tạo dictionary.
- Ghi thành segment/object/block lớn.
- Giảm overhead transaction từng dòng.

Nếu insert từng row:

```text
insert row 1
insert row 2
insert row 3
...
```

Mỗi row đều có overhead riêng:

- Transaction overhead.
- Metadata overhead.
- Flush/write overhead.
- Cluster coordination.
- Buffer management.

Ví dụ tốt:

```text
App -> Kafka/CDC/staging -> mỗi 5 phút bulk load 500,000 events -> ColumnStore
```

Ví dụ kém:

```text
App -> insert từng event trực tiếp vào ColumnStore mỗi request
```

## 15. Object storage là gì?

Object storage lưu dữ liệu thành object qua API, thường là S3-compatible API.

Ví dụ:

```text
AWS S3
MinIO
Ceph Object Gateway
Wasabi
```

Dữ liệu được truy cập kiểu:

```text
s3://warehouse/orders/segment_001
s3://warehouse/orders/segment_002
```

Thao tác thường gặp:

```text
GET object
PUT object
DELETE object
LIST bucket
```

Object storage không giống filesystem truyền thống. Nó không tối ưu cho sửa một vài byte trong file. Nó mạnh ở:

- Lưu dữ liệu rất lớn.
- Ghi/đọc object lớn.
- Độ bền cao.
- Scale ngang.
- Chi phí tốt cho historical data.

## 16. Shared filesystem / NFS là gì?

NFS là viết tắt của **Network File System**.

Hiểu đơn giản: một storage server chia sẻ thư mục qua network, các server khác mount thư mục đó và dùng như thư mục local.

Ví dụ:

```text
Storage Server:
/export/columnstore

DB Node 1 mount -> /data/columnstore
DB Node 2 mount -> /data/columnstore
DB Node 3 mount -> /data/columnstore
```

Mỗi DB node nhìn thấy:

```text
/data/columnstore/segment_001
/data/columnstore/segment_002
/data/columnstore/metadata
```

Nhưng dữ liệu thật nằm trên storage chung.

NFS giống thư mục mạng trong công ty:

```text
\\fileserver\SharedFolder
```

Nhiều máy cùng mở được, file thật nằm trên file server.

## 17. Topology 1: ColumnStore Object Storage

Topology này dùng ColumnStore với S3-compatible object storage.

Sơ đồ logic:

```text
BI Tool / SQL Client
        |
        v
     MaxScale
        |
   -----------------
   |       |       |
  ES      ES      ES
Column  Column  Column
Store   Store   Store
   |       |       |
   ---- Object Storage ----
       S3 / MinIO / Ceph
```

Thành phần:

- **BI Tool / App**: nơi gửi query.
- **MaxScale**: proxy/load balancer, route traffic, hỗ trợ failover.
- **Enterprise Server nodes**: các node database nhận query.
- **ColumnStore engine**: engine xử lý columnar analytical query.
- **Object Storage**: nơi lưu segment/object.
- **CMAPI**: ColumnStore Management API, hỗ trợ quản lý cluster/failover.

### Đặc điểm

- Highly available.
- Automatic failover via MaxScale và CMAPI.
- Scales reads via MaxScale.
- Bulk data import.
- Storage backend là S3-compatible object storage.

### Use case phù hợp

Phù hợp khi:

- Dữ liệu rất lớn.
- Cần lưu lịch sử lâu dài.
- Đã có S3/MinIO/Ceph.
- Chạy trên cloud hoặc object-store-ready environment.
- Cần scale storage độc lập với compute.
- Workload chủ yếu append/read/aggregate.

Ví dụ:

```text
Mobile app có 500 triệu events/ngày.
Dữ liệu được đẩy vào S3/MinIO mỗi 5 phút.
ColumnStore dùng để phân tích campaign, conversion, revenue, retention.
```

Query:

```sql
SELECT campaign_id, COUNT(*), SUM(revenue)
FROM events
WHERE event_date >= '2026-05-01'
GROUP BY campaign_id;
```

### Ưu điểm

- Scale dung lượng rất tốt.
- Hợp dữ liệu hàng chục/hàng trăm TB.
- Chi phí lưu trữ tốt hơn cho historical data lớn.
- Tách compute và storage rõ.
- Compute node có thể tăng/giảm theo nhu cầu.
- Object storage thường có replication/erasure coding.
- Hợp với data lake/cloud-native architecture.

### Nhược điểm

- Latency cao hơn filesystem/local storage.
- Không hợp nhiều object nhỏ.
- Tự vận hành MinIO/Ceph có thể phức tạp.
- Phụ thuộc network nhiều.
- Không hợp update nhỏ liên tục.
- Cần tuning object size, batch size, network, credential/security.

## 18. Topology 2: ColumnStore Shared Local Storage

Topology này dùng ColumnStore với shared filesystem/local shared storage, ví dụ NFS/NAS.

Sơ đồ logic:

```text
BI Tool / SQL Client
        |
        v
     MaxScale
        |
   -----------------
   |       |       |
  ES      ES      ES
Column  Column  Column
Store   Store   Store
   |       |       |
   ---- Shared Filesystem ----
          NFS / NAS
```

Data nằm trên một filesystem dùng chung. Các node ColumnStore cùng mount và truy cập.

Ví dụ:

```text
DB Node 1 -> /data/columnstore
DB Node 2 -> /data/columnstore
DB Node 3 -> /data/columnstore
```

### Đặc điểm

- Highly available nếu storage cũng được HA.
- Automatic failover via MaxScale và CMAPI.
- Scales reads via MaxScale.
- Bulk data import.
- Storage backend là shared filesystem.

### Use case phù hợp

Phù hợp khi:

- Môi trường on-prem/datacenter.
- Đã có NAS/NFS mạnh.
- Dữ liệu vừa đến lớn nhưng chưa quá khổng lồ.
- Team quen quản trị filesystem.
- Muốn setup gần với database truyền thống hơn.

Ví dụ:

```text
Công ty bán lẻ có 5 TB dữ liệu orders/inventory/customer.
ETL chạy mỗi đêm từ ERP.
Có sẵn NAS HA trong datacenter.
Dùng ColumnStore Shared Local Storage cho dashboard Power BI.
```

### Ưu điểm

- Dễ hiểu vì là filesystem/path.
- Dễ tích hợp với hệ thống đã quen file.
- Latency thường thấp hơn object storage nếu NFS/NAS tốt.
- Hợp on-prem.
- Nếu một compute node chết, node khác vẫn đọc được data từ shared storage.
- Vận hành đơn giản hơn nếu công ty đã có NAS/NFS HA.

### Nhược điểm

- Shared storage dễ thành bottleneck.
- Tất cả node cùng đọc/ghi vào một storage chung.
- Scale lên hàng trăm TB có thể đắt và phức tạp.
- Nếu NFS/NAS lỗi, nhiều node cùng bị ảnh hưởng.
- Cần HA cho storage: redundant controller, replication, backup, network.
- NAS enterprise mạnh có thể rất đắt.

## 19. Object Storage vs Shared Local Storage

| Tiêu chí | Object Storage Topology | Shared Local Storage Topology |
|---|---|---|
| Storage backend | S3-compatible object storage | NFS/NAS/shared filesystem |
| Ví dụ | AWS S3, MinIO, Ceph, Wasabi | NFS, NAS, AWS EFS, Azure Files |
| Truy cập | API object: GET/PUT/LIST | File path: open/read/write |
| Kiểu data | Object/segment | File/segment trên filesystem |
| Hợp cloud | Rất hợp | Có thể, nhưng không phải chính |
| Hợp on-prem | Có, nếu dùng MinIO/Ceph | Rất hợp nếu có NAS/NFS |
| Scale storage | Rất tốt | Phụ thuộc NAS/NFS |
| Latency | Thường cao hơn | Thường thấp hơn nếu storage tốt |
| Chi phí data lớn | Thường tốt hơn | Có thể đắt khi scale lớn |
| Vận hành | Phức tạp nếu tự host object store | Dễ hơn nếu đã có NAS/NFS |
| Bottleneck | Network/object request/object size | NFS/NAS throughput/metadata |
| Use case chính | Data lake, warehouse lớn, historical analytics | Warehouse nội bộ, data vừa, datacenter |

## 20. Tách compute và storage

Tách compute và storage nghĩa là:

```text
Compute layer:
- SQL parser
- query planner
- execution engine
- join
- aggregate
- filter

Storage layer:
- nơi giữ dữ liệu lâu dài
- object storage hoặc shared filesystem
```

Trong ColumnStore topology:

```text
Compute nodes:
ES + ColumnStore engine

Storage:
S3/MinIO/Ceph hoặc NFS/NAS
```

Lợi ích:

- Compute node chết không làm mất data.
- Có thể thêm compute node để đọc/query nhiều hơn.
- Có thể scale storage riêng.
- Hợp workload BI theo đợt, vì có lúc cần nhiều compute hơn bình thường.

Ví dụ:

```text
Bình thường: 3 compute nodes
Cuối tháng: 10 compute nodes để chạy báo cáo
Sau đó: giảm về 3 nodes
Storage vẫn là 200 TB dùng chung
```

Với object storage, ý tưởng tách compute/storage rõ hơn vì data nằm trong object store chung. Với shared local storage, compute cũng tách khỏi storage nhưng storage là filesystem dùng chung.

## 21. High availability, MaxScale và CMAPI

Cả hai topology đều có:

```text
Highly available
Automatic failover via MaxScale and CMAPI
Scales reads via MaxScale
```

### MaxScale

MaxScale là proxy/load balancer cho MariaDB.

Vai trò:

- Nhận connection từ client.
- Route query tới node phù hợp.
- Phát hiện node lỗi.
- Chuyển traffic sang node khác.
- Hỗ trợ read scaling.

Ví dụ:

```text
BI Dashboard 1 -> MaxScale -> ES Node 1
BI Dashboard 2 -> MaxScale -> ES Node 2
BI Dashboard 3 -> MaxScale -> ES Node 3
```

Nếu ES Node 1 chết:

```text
MaxScale route traffic sang ES Node 2/3
```

### CMAPI

CMAPI là ColumnStore Management API.

Vai trò tổng quát:

- Quản lý cluster ColumnStore.
- Hỗ trợ failover.
- Phối hợp trạng thái node.
- Phối hợp với MaxScale trong topology HA.

## 22. Vì sao object storage hợp dữ liệu TB/PB?

Data warehouse và historical analytics thường có:

```text
dữ liệu rất lớn
append nhiều
ít sửa dữ liệu cũ
đọc theo batch/scan
giữ nhiều năm
```

Object storage hợp vì:

- Scale dung lượng ngang tốt.
- Chi phí lưu trữ lớn thường tốt.
- Độ bền dữ liệu cao.
- Phù hợp object/segment lớn.
- Tách compute/storage tốt.
- Hợp pipeline ETL/data lake.

Ví dụ:

```text
Clickstream 5 TB/ngày
Giữ 2 năm
Tổng dữ liệu nhiều PB
```

Lưu tất cả trên SSD/NAS cao cấp sẽ rất đắt. Object storage thường hợp lý hơn.

## 23. Vì sao shared local storage hợp on-prem?

Nhiều doanh nghiệp đã có sẵn:

```text
NetApp
Dell storage
TrueNAS
NFS cluster
SAN/NAS infrastructure
```

Nếu dữ liệu mức vừa:

```text
3 TB
10 TB
30 TB
```

và có NAS/NFS mạnh, Shared Local Storage có thể đơn giản hơn object storage.

Nó hợp khi:

- Team quen mount filesystem.
- Backup/monitoring đã có sẵn cho NAS.
- Không muốn quản lý S3/MinIO/Ceph.
- Query cần latency storage tốt trong datacenter.

## 24. Use case phù hợp và không phù hợp của ColumnStore

### Phù hợp

ColumnStore phù hợp với:

- Data warehouse.
- BI dashboard.
- Historical analytics.
- DSS.
- Read-heavy analytical workload.
- Fact table lớn.
- Bulk import.
- Batch/micro-batch ETL.

Ví dụ:

```sql
SELECT d.month, r.region, SUM(f.revenue)
FROM fact_orders f
JOIN dim_date d ON f.date_id = d.id
JOIN dim_region r ON f.region_id = r.id
GROUP BY d.month, r.region;
```

Lý do phù hợp:

- Đọc nhiều row.
- Dùng ít cột.
- Aggregate nhiều.
- Dữ liệu lịch sử lớn.
- Ít update từng dòng.
- Nén theo cột và segment pruning có lợi.

### Không phù hợp

ColumnStore không nên làm database chính cho:

- Checkout order.
- Payment transaction.
- Banking ledger.
- User session store.
- Chat realtime.
- Queue/event processing.
- Search full-text chính.
- High-frequency update.

Lý do:

- Cần update/insert nhỏ liên tục.
- Cần transaction latency thấp.
- Cần lookup theo key rất nhanh.
- Cần ACID runtime workload.
- Cần data structure chuyên biệt hơn, ví dụ inverted index cho search hoặc queue semantics cho message broker.

## 25. Ví dụ kiến trúc thực tế

Kiến trúc hay gặp:

```text
Application / Spring Boot
        |
        v
OLTP Database: MariaDB/InnoDB, PostgreSQL, MySQL
        |
        | CDC / ETL / Batch / Micro-batch
        v
ColumnStore + Object Storage hoặc Shared Local Storage
        |
        v
BI / Reporting / DSS
```

OLTP database xử lý:

- Tạo order.
- Thanh toán.
- Cập nhật tồn kho.
- User login.
- Transaction runtime.

ColumnStore xử lý:

- Doanh thu theo tháng.
- Top sản phẩm.
- Conversion rate.
- Refund rate.
- Phân tích theo khu vực.
- Báo cáo lịch sử.

## 26. Chọn topology nào?

### Chọn Object Storage nếu

- Dữ liệu lớn và tăng nhanh.
- Cần lưu lịch sử dài hạn.
- Đã có S3/MinIO/Ceph.
- Chạy cloud-native.
- Cần scale storage lâu dài.
- Muốn tách compute/storage rõ.
- Chi phí storage là vấn đề quan trọng.

### Chọn Shared Local Storage nếu

- On-prem/datacenter.
- Đã có NAS/NFS mạnh.
- Dữ liệu vừa đến lớn.
- Team quen filesystem.
- Muốn vận hành đơn giản hơn object store.
- Latency storage quan trọng hơn khả năng scale cực lớn.

### Chọn RowStore/OLTP nếu

- Cần transaction runtime.
- Cần update từng dòng liên tục.
- Cần lookup theo primary key cực nhanh.
- Dữ liệu nhỏ hoặc query báo cáo đơn giản.
- App cần response latency thấp.

## 27. Tóm tắt ngắn gọn

```text
RowStore:
- Lưu theo dòng.
- Mạnh cho OLTP.
- Lookup/update một vài row nhanh.
- Hợp checkout, payment, session, app runtime.

ColumnStore:
- Lưu theo cột.
- Mạnh cho OLAP.
- Scan/aggregate nhiều row nhanh.
- Hợp BI, data warehouse, historical analytics.

Object Storage Topology:
- ColumnStore + S3-compatible object storage.
- Hợp dữ liệu rất lớn, cloud/data lake, scale dài hạn.
- Latency cao hơn, tuning phức tạp hơn.

Shared Local Storage Topology:
- ColumnStore + NFS/NAS/shared filesystem.
- Hợp on-prem, data vừa, có NAS/NFS mạnh.
- Dễ hiểu hơn, latency tốt hơn nếu storage tốt.
- Dễ nghẽn ở shared storage khi scale lớn.
```

Quy tắc chọn nhanh:

```text
Cần xử lý giao dịch từng dòng -> RowStore.
Cần báo cáo trên hàng trăm triệu dòng -> ColumnStore.
Data warehouse lớn/cloud/S3 -> Object Storage Topology.
Data warehouse nội bộ/on-prem/NAS -> Shared Local Storage Topology.
```
