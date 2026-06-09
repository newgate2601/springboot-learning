# MariaDB - Tối ưu và tinh chỉnh

## 1. Query Cache

### 1.1. Query Cache là gì?

**Query Cache** là cơ chế lưu lại kết quả của các câu `SELECT` để nếu MariaDB nhận lại cùng một câu query giống hệt trong tương lai, server có thể trả kết quả trực tiếp từ cache thay vì phải parse, optimize, đọc index, đọc bảng và dựng result set lại từ đầu.

Nói ngắn gọn:

```text
SELECT giống hệt + dữ liệu liên quan chưa đổi
= có thể trả kết quả từ Query Cache
= giảm thời gian xử lý và giảm tải đọc dữ liệu
```

Query Cache hữu ích nhất với hệ thống có đặc điểm:

- đọc nhiều hơn ghi;
- nhiều câu `SELECT` lặp lại giống nhau;
- dữ liệu thay đổi không quá thường xuyên;
- result set không quá lớn;
- workload không có quá nhiều connection cạnh tranh cùng lúc.

Ngược lại, Query Cache thường không phù hợp với hệ thống:

- ghi nhiều, cập nhật dữ liệu liên tục;
- workload chạy trên máy nhiều core với throughput cao;
- query rất đa dạng, ít khi lặp lại giống hệt;
- result set lớn, dễ chiếm hết cache;
- hệ thống nhạy với lock contention.

Điểm quan trọng: Query Cache có thể giúp nhanh hơn trong một số workload đọc nhiều, nhưng không phải lúc nào cũng là tối ưu. Vì mỗi lần dữ liệu trong bảng thay đổi, các kết quả cache liên quan đến bảng đó phải bị invalidate. Nếu hệ thống ghi nhiều, chi phí invalidate và tranh chấp lock có thể lớn hơn lợi ích cache.

### 1.2. PostgreSQL, Oracle, MySQL có Query Cache không?

Khi nói "database có cache không", rất dễ nhầm ba khái niệm khác nhau:

| Loại cache | Cache cái gì? | Mục đích |
| --- | --- | --- |
| Query Cache / Result Cache | Cache nguyên kết quả trả về của câu `SELECT` | Nếu gặp lại cùng query, trả luôn result set đã lưu. |
| Buffer Cache / Page Cache | Cache block/page dữ liệu và index đã đọc từ disk | Lần sau đọc cùng page thì không phải đọc disk nữa. |
| Plan Cache / Cursor Cache | Cache parse tree, execution plan hoặc cursor | Giảm chi phí parse/optimize, nhưng vẫn phải đọc dữ liệu để tạo result. |

Query Cache trong tài liệu này đang nói về loại thứ nhất: **cache nguyên kết quả của query**.

Ví dụ query:

```sql
SELECT name, price
FROM products
WHERE category_id = 10;
```

Nếu là Query Cache đúng nghĩa, database có thể lưu lại result kiểu:

```text
query text + session context -> danh sách row kết quả
```

Lần sau gặp lại đúng query đó trong cùng ngữ cảnh hợp lệ, database có thể trả danh sách row từ cache.

Nếu chỉ là Buffer Cache, database không lưu nguyên danh sách row của query. Nó chỉ lưu các page dữ liệu/index đã đọc. Lần sau chạy lại query, database vẫn phải parse, optimize, đi qua execution plan, đọc index/page từ memory rồi dựng lại result set. Nhanh hơn đọc disk, nhưng không giống Query Cache.

Nếu chỉ là Plan Cache, database nhớ cách chạy query, ví dụ nên dùng index nào, join theo thứ tự nào. Nhưng database vẫn phải thực thi plan để lấy dữ liệu hiện tại.

So sánh các database phổ biến:

| Database | Có Query Cache kiểu cache nguyên result set không? | Giải thích |
| --- | --- | --- |
| PostgreSQL | Không có built-in Query Cache | PostgreSQL có `shared_buffers`, tận dụng OS page cache và có plan cache cho prepared statement. Nhưng nó không tự cache nguyên kết quả `SELECT` như MariaDB Query Cache. Muốn cache result thường dùng materialized view, application cache, Redis hoặc cache ở tầng service. |
| Oracle | Có Server Result Cache | Oracle có cơ chế cache result set ở database level, thường gọi là Server Result Cache. Nó khác Query Cache cũ của MySQL/MariaDB vì có dependency tracking chặt hơn với object liên quan và có nhiều rule riêng của Oracle. |
| MySQL 5.x | Có Query Cache | MySQL 5.x từng có Query Cache tương tự concept của MariaDB: cache kết quả `SELECT`, invalidate khi bảng liên quan thay đổi. |
| MySQL 5.7 | Có nhưng deprecated | MySQL 5.7 vẫn còn Query Cache nhưng đã bị khuyến cáo không nên phụ thuộc lâu dài. |
| MySQL 8.0+ | Không còn Query Cache | MySQL 8.0 đã remove Query Cache, chủ yếu vì cơ chế này khó scale tốt trên workload nhiều core, nhiều connection và ghi nhiều. |
| MariaDB | Vẫn có Query Cache | MariaDB vẫn giữ Query Cache, nhưng nên dùng có chọn lọc thay vì bật mặc định cho mọi workload. |

Cách nhớ nhanh:

```text
PostgreSQL: không có query result cache built-in
Oracle: có Server Result Cache
MySQL 5.x: có Query Cache
MySQL 8.0+: đã bỏ Query Cache
MariaDB: vẫn có Query Cache
```

Vì vậy, khi nghe "PostgreSQL có cache", điều đó đúng nhưng thường là **cache page dữ liệu** hoặc **cache plan**, không phải **cache nguyên kết quả SELECT**. Đây là khác biệt quan trọng khi so sánh với MariaDB Query Cache.

### 1.3. Trạng thái mặc định và các biến chính

Query Cache có thể có sẵn trong MariaDB nhưng không nhất thiết đang hoạt động. Có ba nhóm biến cần phân biệt:

| Biến | Giá trị thường gặp / mặc định | Ý nghĩa |
| --- | --- | --- |
| `have_query_cache` | `YES` hoặc `NO` | Cho biết binary MariaDB có hỗ trợ Query Cache hay không. Nếu là `NO` thì không thể bật Query Cache nếu không dùng bản build khác. |
| `query_cache_type` | mặc định thường là `OFF` hoặc `0` | Quyết định server có dùng Query Cache hay không, và dùng theo kiểu tự động hay theo yêu cầu từng query. |
| `query_cache_size` | mặc định khoảng `1MB` trong tài liệu MariaDB cũ, nhưng nhiều môi trường production đặt về `0` | Tổng dung lượng bộ nhớ dành cho Query Cache. Nếu bằng `0`, Query Cache không hoạt động. |
| `query_cache_limit` | thường mặc định `1MB` | Kích thước tối đa của một result set được phép đưa vào Query Cache. Query trả về lớn hơn giới hạn này sẽ không được cache. |
| `query_cache_min_res_unit` | thường mặc định `4096` byte | Kích thước block tối thiểu dùng để cấp phát bộ nhớ cho result set trong Query Cache. |
| `query_cache_strip_comments` | thường mặc định `OFF` | Nếu bật, MariaDB có thể bỏ comment khỏi query trước khi so sánh cache key. |
| `query_cache_wlock_invalidate` | mặc định `OFF` | Quyết định cache của bảng có bị invalidate khi bảng đang bị write lock hay không. |

`query_cache_type` có các chế độ quan trọng:

| Giá trị | Tên thường dùng | Ý nghĩa |
| --- | --- | --- |
| `0` | `OFF` | Không lưu query mới vào cache và không lấy kết quả từ cache. |
| `1` | `ON` | Tự động cache các query đủ điều kiện, trừ query có `SQL_NO_CACHE`. |
| `2` | `DEMAND` | Chỉ cache query có chỉ định `SQL_CACHE`. |

Về mặt tư duy vận hành:

- `OFF` phù hợp khi workload ghi nhiều, concurrency cao hoặc không muốn trả giá lock của Query Cache.
- `ON` phù hợp khi phần lớn query đọc lặp lại và dữ liệu ít thay đổi.
- `DEMAND` phù hợp hơn khi chỉ muốn cache một số query thật sự đáng cache, ví dụ dashboard, danh mục, cấu hình, dữ liệu ít đổi.

### 1.4. Query Cache hoạt động như thế nào?

Khi Query Cache được bật và một câu `SELECT` được xử lý, MariaDB kiểm tra xem câu query đó đã có trong cache hay chưa.

Một query được xem là trùng cache khi không chỉ nội dung SQL giống nhau, mà nhiều yếu tố ngữ cảnh cũng phải giống nhau. Ví dụ:

- cùng nội dung query;
- cùng database/schema mặc định;
- cùng character set;
- cùng collation;
- cùng protocol;
- cùng trạng thái transaction;
- cùng `sql_mode`;
- cùng `time_zone`;
- cùng một số session variable có thể làm thay đổi kết quả.

Điều này có nghĩa là Query Cache không đơn giản chỉ so sánh text SQL. Nó so sánh cả query và các cờ nội bộ có thể ảnh hưởng đến kết quả.

Ví dụ về khác biệt cache key:

```text
SELECT * FROM t
```

khác với:

```text
SELECT * from t
```

Vì Query Cache so sánh query theo kiểu nhạy chữ hoa/thường ở phần text query.

Comment cũng có thể làm query khác nhau:

```text
/* retry */ SELECT * FROM t
```

khác với:

```text
/* retry2 */ SELECT * FROM t
```

Nếu workload hoặc framework thường tự thêm comment khác nhau vào SQL, tỉ lệ hit của Query Cache có thể giảm. Khi đó `query_cache_strip_comments` là biến cần biết, vì nó liên quan đến việc bỏ comment trước khi tìm cache.

### 1.5. Vòng đời của một kết quả trong Query Cache

Một kết quả cache thường đi qua các bước:

1. Client gửi câu `SELECT`.
2. MariaDB kiểm tra Query Cache trước khi xử lý query đầy đủ.
3. Nếu có cache hit, kết quả được trả về từ cache.
4. Nếu cache miss, MariaDB xử lý query bình thường.
5. Nếu query đủ điều kiện, result set được lưu vào Query Cache.
6. Khi dữ liệu trong bảng liên quan thay đổi, các result set bị ảnh hưởng sẽ bị xóa khỏi cache.
7. Khi vùng nhớ cache đầy, các kết quả cũ hoặc ít phù hợp sẽ bị loại bỏ để lấy chỗ cho kết quả mới.

Điểm cần nhớ: Query Cache không trả dữ liệu cũ đã stale. Khi bảng thay đổi bởi `INSERT`, `UPDATE`, `DELETE`, `TRUNCATE` hoặc các thao tác tương tự, cache liên quan sẽ bị invalidate.

Vì vậy, vấn đề của Query Cache không phải là sai dữ liệu, mà là chi phí duy trì cache trong workload có nhiều thay đổi.

### 1.6. Query nào được lưu vào Query Cache?

Khi `query_cache_type = ON`, MariaDB có thể cache các câu `SELECT` đủ điều kiện, trừ khi query có chỉ định `SQL_NO_CACHE`.

Khi `query_cache_type = DEMAND`, MariaDB chỉ cache query có chỉ định `SQL_CACHE`.

Query thường không được cache nếu:

- dùng hàm không xác định, tức kết quả có thể thay đổi giữa các lần chạy dù SQL giống nhau;
- dùng bảng tạm;
- không đọc bảng nào;
- tạo warning;
- truy cập `INFORMATION_SCHEMA`, `mysql` hoặc `performance_schema`;
- dùng user variable hoặc local variable;
- dùng stored function hoặc user-defined function;
- nằm trong transaction với isolation level `SERIALIZABLE`;
- là query trong transaction sau khi cùng bảng đó vừa có thao tác làm invalidate cache;
- result set vượt quá `query_cache_limit`;
- query có dạng xuất dữ liệu ra file hoặc dump file;
- query có `SQL_NO_CACHE`;
- storage engine hoặc môi trường không hỗ trợ Query Cache.

Các hàm thường làm query không được cache gồm:

| Nhóm | Hàm ví dụ |
| --- | --- |
| Thời gian hiện tại | `NOW()`, `CURDATE()`, `CURRENT_DATE()`, `CURRENT_TIME()`, `CURRENT_TIMESTAMP()`, `CURTIME()`, `SYSDATE()`, `UNIX_TIMESTAMP()` không có tham số |
| Session / connection | `CONNECTION_ID()`, `USER()`, `DATABASE()` |
| Random / UUID | `RAND()`, `UUID()`, `UUID_SHORT()` |
| Lock / trạng thái runtime | `GET_LOCK()`, `RELEASE_LOCK()`, `MASTER_POS_WAIT()`, `FOUND_ROWS()`, `LAST_INSERT_ID()` |
| File / hệ thống | `LOAD_FILE()` |
| Khác | `BENCHMARK()`, `CONVERT_TZ()`, `ENCRYPT()` một tham số, `SLEEP()` |

Lý do chung là các hàm này có thể trả kết quả phụ thuộc thời điểm, session, connection, lock, file system hoặc trạng thái runtime. Nếu cache bừa, kết quả có thể không còn đúng về mặt ngữ nghĩa.

### 1.7. `SQL_NO_CACHE` và `SQL_CACHE`

Query Cache có hai khía cạnh riêng:

- có lưu query vào cache hay không;
- có lấy kết quả từ cache hay không.

`SQL_NO_CACHE` dùng để nói rằng query không nên dùng Query Cache. Trong môi trường `query_cache_type = ON`, đây là cách loại trừ những query không đáng cache, ví dụ query ad-hoc, query trả result set lớn hoặc query ít khi lặp lại.

`SQL_CACHE` dùng để nói rằng query nên được cache. Trong môi trường `query_cache_type = DEMAND`, đây là cách chọn lọc chỉ những query có giá trị cache cao.

Về concept, `DEMAND` thường sạch hơn `ON` trong hệ thống lớn, vì nó buộc developer hoặc DBA phải chỉ rõ query nào thật sự đáng cache, thay vì để server cố cache quá nhiều thứ.

### 1.8. Giới hạn kích thước Query Cache

Có hai giới hạn kích thước quan trọng:

| Biến | Ý nghĩa |
| --- | --- |
| `query_cache_size` | Tổng dung lượng bộ nhớ dành cho toàn bộ Query Cache. |
| `query_cache_limit` | Kích thước tối đa của một result set riêng lẻ được phép cache. |

Nếu `query_cache_size` quá nhỏ:

- cache nhanh đầy;
- nhiều query vừa được thêm vào đã bị loại bỏ;
- `Qcache_lowmem_prunes` tăng nhanh;
- tỉ lệ hit thấp;
- lợi ích cache không đáng kể.

Nếu `query_cache_size` quá lớn:

- có thể tăng lock contention;
- thao tác invalidate hoặc quản lý block tốn chi phí hơn;
- bộ nhớ bị giữ cho cache thay vì phục vụ buffer pool hoặc workload khác;
- performance có thể giảm dù cache lớn hơn.

`query_cache_size` nên được hiểu là một vùng nhớ dùng chung có lock, không phải cứ tăng lớn là tốt.

`query_cache_limit` giúp tránh một query có result set quá lớn chiếm phần lớn cache và đẩy nhiều kết quả nhỏ, có ích hơn, ra khỏi cache.

`query_cache_min_res_unit` ảnh hưởng đến cách Query Cache cấp phát block bộ nhớ cho result set:

- giá trị nhỏ: giảm lãng phí bộ nhớ cho result set nhỏ, nhưng có thể tăng số block và tăng lock/fragmentation;
- giá trị lớn: giảm số block và có thể giảm lock, nhưng dễ lãng phí bộ nhớ nếu result set nhỏ nhiều.

Không có một giá trị tối ưu chung cho mọi hệ thống. Giá trị đúng phụ thuộc vào kích thước result set, tần suất query, tần suất ghi và concurrency thực tế.

### 1.9. Theo dõi Query Cache qua biến trạng thái

Các status variable quan trọng:

| Biến | Ý nghĩa |
| --- | --- |
| `Qcache_hits` | Số lần query lấy được kết quả từ cache. |
| `Qcache_inserts` | Số query được thêm vào cache. |
| `Qcache_not_cached` | Số query không được cache. |
| `Qcache_lowmem_prunes` | Số lần query bị loại khỏi cache vì thiếu bộ nhớ. |
| `Qcache_queries_in_cache` | Số query hiện đang nằm trong cache. |
| `Qcache_free_memory` | Bộ nhớ còn trống trong Query Cache. |
| `Qcache_free_blocks` | Số block trống trong Query Cache. |
| `Qcache_total_blocks` | Tổng số block trong Query Cache. |

Cách đọc nhanh:

- `Qcache_hits` cao so với `Qcache_inserts` thường là dấu hiệu tốt.
- `Qcache_inserts` cao nhưng `Qcache_hits` thấp nghĩa là server đang cache nhiều nhưng ít tái sử dụng.
- `Qcache_lowmem_prunes` tăng nhanh nghĩa là cache thiếu bộ nhớ hoặc đang cache quá nhiều thứ không đáng cache.
- `Qcache_free_blocks` cao so với `Qcache_total_blocks` có thể cho thấy fragmentation.
- `Qcache_not_cached` cao không hẳn là xấu nếu nhiều query vốn không nên cache.

Một Query Cache hoạt động kém thường có pattern:

```text
Qcache_inserts tăng nhiều
Qcache_lowmem_prunes tăng nhiều
Qcache_hits không tăng tương xứng
```

Điều này nghĩa là server đang mất công đưa kết quả vào cache, rồi lại nhanh chóng loại bỏ chúng trước khi được tái sử dụng.

### 1.10. Phân mảnh trong Query Cache

Query Cache dùng các block có độ dài biến đổi. Theo thời gian, khi result set được thêm vào, bị invalidate, bị loại bỏ, vùng nhớ cache có thể bị phân mảnh.

Dấu hiệu thường thấy:

- `Qcache_free_blocks` cao;
- `Qcache_free_memory` vẫn còn nhưng cache hoạt động không hiệu quả;
- nhiều block trống nhỏ rải rác thay vì một vùng trống lớn.

Fragmentation làm Query Cache khó dùng bộ nhớ hiệu quả. MariaDB có cơ chế defragment Query Cache để gom các block trống lại mà không nhất thiết phải xóa toàn bộ query đang cache.

Về concept:

```text
nhiều free block nhỏ
= bộ nhớ bị phân mảnh
= cache có thể kém hiệu quả dù vẫn còn free memory
```

### 1.11. Xóa, làm rỗng và tắt Query Cache

Có ba khái niệm cần tách biệt:

| Hành động | Ý nghĩa |
| --- | --- |
| Làm rỗng cache | Xóa toàn bộ result set đang nằm trong Query Cache. |
| Defragment cache | Gom lại block trống để giảm phân mảnh, không nhất thiết xóa query cache. |
| Tắt cache | Không dùng Query Cache nữa, thường bằng cách đưa `query_cache_type` hoặc `query_cache_size` về trạng thái vô hiệu. |

Nếu muốn giải phóng tài nguyên nhiều nhất, về mặt concept cần cả hai điều kiện:

- Query Cache không nhận query mới;
- vùng nhớ dành cho Query Cache không còn được cấp phát đáng kể.

Nói cách khác, chỉ tắt logic cache chưa chắc đã giải phóng hết bộ nhớ nếu `query_cache_size` vẫn còn lớn.

### 1.12. Query Cache và `LOCK TABLES`

Query Cache có thể hoạt động ngay cả khi bảng đang có write lock. Điều này nghe có vẻ ngược trực giác, vì write lock thường làm người ta nghĩ rằng read nên bị chặn để tránh dữ liệu không ổn định.

Biến liên quan là `query_cache_wlock_invalidate`:

| Giá trị | Ý nghĩa |
| --- | --- |
| `OFF` | Mặc định. Cached query vẫn có thể được trả về ngay cả khi bảng đang bị write lock. |
| `ON` | Khi có write lock, cache liên quan bị invalidate để tránh trả kết quả từ cache trong tình huống đó. |

Ý nghĩa thực tế:

- `OFF` ưu tiên khả năng đọc từ cache và giảm chờ đợi.
- `ON` nghiêm ngặt hơn với tình huống write lock, nhưng có thể làm mất lợi ích cache nhiều hơn.

### 1.13. Query Cache và transaction

Query Cache có xử lý khác nhau giữa query chạy ngoài transaction và query chạy trong transaction. Đây là phần dễ nhầm, vì nhìn ở application log thì câu SQL có thể giống hệt nhau:

```sql
SELECT * FROM T1;
```

Nhưng với Query Cache, "giống hệt câu SQL" vẫn chưa đủ. MariaDB còn xét thêm ngữ cảnh session và trạng thái transaction. Trong nội bộ, trạng thái "query này có đang chạy bên trong transaction không" là một phần của cache key.

Có thể hiểu đơn giản:

```text
cùng một SELECT ngoài transaction
khác với
cùng một SELECT trong transaction
```

Nói rõ hơn:

```text
cache key A = SELECT * FROM T1 + FLAGS_IN_TRANS = 0
cache key B = SELECT * FROM T1 + FLAGS_IN_TRANS = 1
```

Hai cache key này khác nhau, nên hai result set không được xem là cùng một cache entry.

`FLAGS_IN_TRANS = 0` có thể hiểu là query chạy ngoài transaction rõ ràng, thường gặp khi `autocommit = 1` và mỗi statement tự kết thúc ngay sau khi chạy.

`FLAGS_IN_TRANS = 1` có thể hiểu là query chạy bên trong một transaction đã bắt đầu, ví dụ sau `BEGIN` hoặc `START TRANSACTION`, trước khi `COMMIT` hoặc `ROLLBACK`.

Lý do MariaDB phải phân biệt như vậy là transaction không chỉ là một "nhóm câu SQL". Transaction còn tạo ra một ngữ cảnh nhất quán dữ liệu riêng. Với engine transactional như InnoDB, một transaction có thể nhìn dữ liệu theo read view riêng, bị ảnh hưởng bởi isolation level, row lock, các thay đổi chưa commit trong chính transaction đó và trạng thái consistency tại thời điểm transaction bắt đầu.

Ví dụ dễ hiểu:

```text
Session A:
BEGIN;
SELECT * FROM T1;
```

Query này chạy trong transaction. MariaDB không nên coi nó giống hoàn toàn với:

```text
Session B:
SELECT * FROM T1;
```

Query của Session B chạy ngoài transaction. Dù text SQL giống nhau, ngữ cảnh consistency khác nhau. Nếu dùng chung cache entry một cách đơn giản, database có nguy cơ trộn kết quả giữa hai hoàn cảnh đọc khác nhau.

Vì vậy, Query Cache chọn cách an toàn hơn: thêm trạng thái transaction vào cache key. Kết quả là:

```text
SELECT * FROM T1 ngoài transaction
= một cache entry riêng

SELECT * FROM T1 trong transaction
= một cache entry riêng khác
```

Điều này không có nghĩa là query trong transaction luôn luôn không được cache. Ý đúng là: nếu được cache, nó được cache dưới một key khác với query ngoài transaction.

Ví dụ timeline:

```text
1. Chạy ngoài transaction:
   SELECT * FROM T1;
   -> MariaDB có thể lưu cache entry với FLAGS_IN_TRANS = 0

2. Bắt đầu transaction:
   BEGIN;
   SELECT * FROM T1;
   -> MariaDB không dùng cache entry ở bước 1
   -> nếu đủ điều kiện, nó có thể lưu cache entry khác với FLAGS_IN_TRANS = 1

3. Vẫn trong transaction:
   SELECT * FROM T1;
   -> lần này có thể hit cache entry FLAGS_IN_TRANS = 1

4. Kết thúc transaction:
   COMMIT;

5. Chạy lại ngoài transaction:
   SELECT * FROM T1;
   -> quay lại nhóm cache entry FLAGS_IN_TRANS = 0
```

Điểm then chốt: cache entry không chỉ đại diện cho "text SQL", mà đại diện cho:

```text
text SQL + database hiện tại + charset/collation + sql_mode + time_zone + trạng thái transaction + nhiều session flag khác
```

Do đó, cùng một câu `SELECT` trong log có thể cache miss chỉ vì nó chạy trong một transaction khác trạng thái với lần trước.

#### Transaction có thay đổi dữ liệu

Khi một transaction có thao tác thay đổi dữ liệu như `INSERT`, `UPDATE`, `DELETE` hoặc `TRUNCATE`, các query cache liên quan đến bảng bị thay đổi sẽ bị invalidate. Với bảng transactional, trong thời gian transaction chưa `COMMIT` hoặc `ROLLBACK`, Query Cache có thể bị vô hiệu hóa cho bảng liên quan để đảm bảo row-level locking và consistency.

Ví dụ:

```text
BEGIN;
SELECT * FROM T1;
INSERT INTO T1 VALUES (...);
SELECT * FROM T1;
COMMIT;
```

Sau câu `INSERT`, dữ liệu của `T1` đã thay đổi trong transaction hiện tại. Lúc này cache cũ của `T1` không còn đáng tin cho transaction đó nữa. MariaDB phải ưu tiên consistency hơn tốc độ, nên các cache entry liên quan bị invalidate hoặc không được dùng trong giai đoạn nhạy cảm này.

Một điểm quan trọng khác: transaction có thể nhìn thấy thay đổi do chính nó tạo ra, dù thay đổi đó chưa commit cho session khác thấy. Query Cache là vùng dùng chung giữa nhiều session, nên nó không thể đơn giản cache một result set chứa dữ liệu chưa commit rồi cho session khác dùng. Vì vậy các query sau DML trong transaction phải được xử lý rất thận trọng.

Có thể hình dung:

```text
Session A trong transaction:
INSERT row mới nhưng chưa COMMIT
SELECT * FROM T1
-> Session A có thể nhìn thấy row mới

Session B ngoài transaction:
SELECT * FROM T1
-> Session B không được nhìn thấy row chưa commit của Session A
```

Nếu Query Cache dùng chung một result set cho cả hai session, kết quả có thể sai. Vì vậy MariaDB phải tách ngữ cảnh transaction và invalidate cache khi bảng thay đổi.

Điểm cần nhớ:

- Query trong transaction và ngoài transaction có thể không dùng chung cache entry vì trạng thái transaction là một phần của cache key.
- Cùng text SQL không đảm bảo cùng cache entry.
- Query trong transaction có thể có read view/consistency context khác query ngoài transaction.
- DML trong transaction làm cache của bảng liên quan không còn đáng tin để tái sử dụng.
- Query Cache không được phép làm lộ dữ liệu chưa commit từ transaction này sang session khác.
- Sau khi transaction kết thúc, Query Cache có thể hoạt động lại bình thường với các query đủ điều kiện.

### 1.14. Cấu trúc nội bộ của cache key

Một query không chỉ được định danh bằng chuỗi SQL. MariaDB còn đưa nhiều thông tin session và protocol vào cấu trúc nội bộ để phân biệt các query có thể cho kết quả khác nhau.

Các trường thường ảnh hưởng đến cache key:

- nội dung query;
- schema/database hiện tại;
- client long flag;
- protocol version;
- protocol type;
- trạng thái có nhiều result hay không;
- có đang trong transaction hay không;
- `autocommit`;
- packet number;
- `character_set_client`;
- `character_set_results`;
- `collation_connection`;
- `sql_select_limit`;
- `time_zone`;
- `sql_mode`;
- `max_sort_length`;
- `group_concat_max_len`;
- `default_week_format`;
- `div_precision_increment`;
- `lc_time_names`.

Điều này giải thích vì sao hai câu SQL nhìn giống nhau trong application log vẫn có thể không hit cùng một cache entry nếu session context khác nhau.

### 1.15. Timeout và tranh chấp mutex

Query Cache là tài nguyên dùng chung, nên khi nhiều thread cùng truy cập, MariaDB cần lock để bảo vệ cấu trúc cache.

Khi tìm query trong cache, MariaDB dùng cơ chế thử lấy lock với timeout nội bộ khoảng `50ms`. Nếu không lấy được lock trong khoảng này, query sẽ không đi qua Query Cache nữa mà được xử lý theo đường bình thường.

Điểm này rất quan trọng trong workload concurrency cao:

- cache hit có thể nhanh;
- nhưng tranh chấp mutex có thể làm Query Cache trở thành điểm nghẽn;
- nhiều core và nhiều connection không đồng nghĩa Query Cache hiệu quả hơn;
- trên hệ thống ghi nhiều hoặc query đa dạng, chi phí lock có thể vượt lợi ích.

Khi nhiều process chạy cùng một query tại cùng thời điểm, không phải tất cả đều lưu được result vào cache. Thường chỉ một process cuối cùng lưu result, các process khác có thể làm tăng `Qcache_not_cached`.

### 1.16. Khi nào nên dùng Query Cache?

Nên cân nhắc Query Cache khi:

- hệ thống đọc nhiều, ghi ít;
- dữ liệu có tính chất tham chiếu, danh mục, cấu hình, lookup;
- query lặp lại giống hệt nhiều lần;
- result set nhỏ hoặc vừa;
- concurrency không quá cao;
- metric cho thấy `Qcache_hits` có giá trị rõ ràng;
- `Qcache_lowmem_prunes` không tăng quá nhanh.

Ví dụ workload phù hợp:

- trang danh mục ít thay đổi;
- bảng cấu hình hệ thống;
- dữ liệu lookup như tỉnh/thành, loại sản phẩm, trạng thái;
- dashboard đọc cùng một query lặp lại trong thời gian ngắn;
- ứng dụng nhỏ hoặc vừa, read-heavy.

### 1.17. Khi nào không nên dùng Query Cache?

Không nên bật Query Cache chỉ vì muốn database "có cache". Nên tránh hoặc tắt Query Cache khi:

- hệ thống ghi nhiều;
- bảng bị update liên tục;
- query generated SQL có nhiều biến thể text khác nhau;
- ORM thêm comment, alias hoặc literal khiến query hiếm khi giống hệt;
- nhiều query trả result set lớn;
- concurrency cao và thấy dấu hiệu mutex contention;
- `Qcache_inserts` và `Qcache_lowmem_prunes` tăng nhanh nhưng `Qcache_hits` thấp;
- InnoDB buffer pool, index, query plan hoặc application cache mới là nơi cần tối ưu chính.

Trong nhiều hệ thống hiện đại, Query Cache cấp database không còn là lựa chọn tối ưu bằng các hướng sau:

- tối ưu index;
- tối ưu query plan;
- dùng application cache có key rõ ràng;
- cache ở tầng API;
- cache ở proxy hoặc CDN nếu dữ liệu phù hợp;
- tách read replica;
- dùng materialized summary table cho báo cáo.

### 1.18. Danh sách kiểm tra khi tinh chỉnh Query Cache

Khi đánh giá Query Cache, nên hỏi theo thứ tự:

1. Query có lặp lại giống hệt không?
2. Dữ liệu liên quan có ít thay đổi không?
3. Result set có đủ nhỏ để cache không?
4. `Qcache_hits` có cao không?
5. `Qcache_lowmem_prunes` có tăng nhanh không?
6. `Qcache_inserts` có quá cao so với `Qcache_hits` không?
7. Có fragmentation không?
8. Có concurrency cao gây lock contention không?
9. Có nên dùng `DEMAND` thay vì `ON` không?
10. Có nên tắt Query Cache và tối ưu bằng index/application cache không?

Kết luận thực dụng:

```text
Query Cache tốt khi ít ghi, query lặp lại, result nhỏ, hit cao.
Query Cache xấu khi nhiều ghi, query đa dạng, result lớn, prune cao, contention cao.
```

Query Cache nên được xem là một công cụ tối ưu có điều kiện, không phải cấu hình mặc định nên bật cho mọi MariaDB server.

## 2. Thread Pool trong MariaDB

### 2.1. Thread Pool giải quyết vấn đề gì?

Mô hình truyền thống của MySQL/MariaDB là **mỗi client connection có một thread riêng**. Cách này đơn giản và dễ hiểu:

```text
1 connection -> 1 server thread
```

Nhưng khi số lượng connection tăng cao, mô hình này bắt đầu có vấn đề:

- quá nhiều thread cùng tồn tại;
- hệ điều hành phải context switch liên tục;
- CPU cache bị mất locality;
- nhiều thread cùng tranh lock;
- nhiều thread bị block vì I/O hoặc row lock;
- memory dùng cho thread stack và metadata tăng;
- throughput có thể giảm dù CPU vẫn còn nhiều core.

Vấn đề chính không phải là "ít thread thì tốt" hay "nhiều thread thì tốt". Vấn đề là số thread đang chạy thực sự nên tương đối phù hợp với khả năng xử lý của CPU và workload.

Ví dụ máy có 16 CPU core nhưng có 2.000 connection active. Nếu để cả 2.000 connection tương ứng với 2.000 thread cùng tranh CPU, hệ điều hành phải liên tục chuyển qua lại giữa các thread. Chi phí quản lý thread có thể lớn đến mức database mất thời gian điều phối nhiều hơn là xử lý query.

Thread Pool giải quyết bằng cách:

```text
nhiều client connection
-> chia vào một số nhóm thread
-> giới hạn số worker thread active
-> giảm context switching
-> dùng CPU ổn định hơn
```

Mục tiêu thực tế:

- giữ số thread active thấp hơn số connection;
- tránh tạo quá nhiều thread vô ích;
- tận dụng CPU tốt hơn;
- giảm memory overhead;
- tránh một lượng lớn connection làm server quá tải vì scheduling;
- giữ throughput ổn định hơn khi concurrency tăng.

### 2.2. Các thuật ngữ cần hiểu trước

Trước khi đọc các biến như `thread_pool_size`, `thread_pool_max_threads` hoặc `thread_pool_stall_limit`, cần hiểu vài khái niệm nền.

| Thuật ngữ | Ý nghĩa ngắn |
| --- | --- |
| Worker thread | Thread thực sự chạy query hoặc xử lý công việc cho connection. |
| Idle thread | Thread đang tồn tại trong pool nhưng hiện không chạy query nào. |
| Stall | Tình trạng một thread group bị kẹt hoặc không tiến triển đủ nhanh, thường do worker đang chờ lock, I/O hoặc query chạy quá lâu. |
| Workload bursty | Workload có dạng im lặng một thời gian, sau đó đột ngột tăng tải rất mạnh trong thời gian ngắn. |
| Thread group | Nhóm connection và worker thread trong Thread Pool, đặc biệt quan trọng trên Unix. |
| Native Windows Thread Pool | Cơ chế thread pool do chính Windows cung cấp, MariaDB dùng trực tiếp trên Windows thay vì tự mô phỏng giống Unix. |

**Idle thread** không có nghĩa là thread "thừa" ngay lập tức. Một số idle thread giúp server phản ứng nhanh khi có request mới, vì không phải tạo thread từ đầu. Nhưng nếu idle thread quá nhiều trong thời gian dài, chúng vẫn chiếm memory và metadata. Vì vậy có biến như `thread_pool_idle_timeout` để thread rảnh lâu có thể tự thoát.

Có thể hiểu:

```text
idle thread ít vừa đủ
= phản ứng nhanh khi có request mới

idle thread quá nhiều
= giữ tài nguyên không cần thiết

idle thread quá ít trong workload bursty
= khi spike đến phải tạo thread lại, latency có thể tăng
```

**Stall** là khi một thread group có worker đang giữ quyền xử lý nhưng công việc không tiến triển như kỳ vọng. Ví dụ worker đang chờ row lock, chờ disk I/O, chờ network, hoặc chạy một query dài. Nếu thread group chỉ cho một worker active, các connection khác trong cùng group có thể bị đứng chờ phía sau.

Vì vậy Thread Pool cần cơ chế phát hiện stall:

```text
worker bị kẹt
-> thread group không phục vụ kịp queue
-> timer phát hiện stall
-> pool có thể cho thêm worker chạy tạm
```

**Workload bursty** là workload có tải không đều. Ví dụ hệ thống bán vé, flash sale, cron job đồng loạt, app mobile vừa gửi push notification xong và hàng nghìn client mở app cùng lúc. Trong workload này, nếu pool retire idle thread quá nhanh, spike tiếp theo sẽ phải trả chi phí tạo lại thread. Nếu giữ quá nhiều idle thread, hệ thống lại tốn RAM lúc bình thường.

**Thread group** là cách MariaDB trên Unix chia connection thành nhiều nhóm độc lập tương đối. Mỗi group có queue riêng và worker riêng. `thread_pool_size` không phải là số thread tối đa, mà là số thread group. Nếu có 16 thread group, connection sẽ được chia vào 16 nhóm để xử lý song song ở mức hợp lý với CPU.

Trong trạng thái bình thường, một thread group thường được phục vụ bởi một worker thread active, nên các query trong cùng group có xu hướng được xử lý tuần tự theo queue. Khi group bị stall hoặc có wait, MariaDB có thể cho phép thêm worker thread trong cùng group để tránh một query/connection làm nghẽn cả group.

Có thể hình dung:

```text
Thread Pool
  Thread Group 1: queue connection A, B, C -> worker
  Thread Group 2: queue connection D, E, F -> worker
  Thread Group 3: queue connection G, H, I -> worker
```

Nếu một thread group bị kẹt, không nhất thiết toàn bộ Thread Pool kẹt ngay. Nhưng nếu nhiều group cùng bị lock/I/O giữ lâu, toàn bộ pool có thể bị nghẽn.

**Native Windows Thread Pool** nghĩa là MariaDB không tự triển khai cùng một logic y hệt Unix trên Windows. Windows đã có sẵn API thread pool của hệ điều hành. MariaDB tận dụng cơ chế đó để hệ điều hành quản lý một phần việc tạo, giữ, retire và schedule thread. Vì vậy trên Windows có các biến như `thread_pool_min_threads`, còn trên Unix lại nhấn mạnh `thread_pool_size`, thread group và cơ chế I/O multiplexing riêng.

### 2.3. Thread Pool khác gì `one-thread-per-connection`?

| Mô hình | Cách hoạt động | Ưu điểm | Nhược điểm |
| --- | --- | --- | --- |
| `one-thread-per-connection` | Mỗi connection gắn với một server thread riêng | Đơn giản, latency dễ đoán hơn khi số connection thấp | Không scale tốt khi connection rất nhiều; nhiều context switch và memory overhead |
| `pool-of-threads` | Nhiều connection dùng chung một pool worker thread | Giới hạn thread active, giảm overhead, phù hợp concurrency cao | Query có thể phải xếp hàng; một số workload bursty hoặc long-running có thể bị delay |

Với `one-thread-per-connection`, nếu có 1.000 connection, server có thể có khoảng 1.000 thread phục vụ connection đó.

Với `pool-of-threads`, nếu có 1.000 connection, server không nhất thiết tạo 1.000 thread active. Nhiều connection sẽ được đưa vào queue và chỉ một số worker thread thực sự chạy tại một thời điểm.

Điểm cần nhớ:

```text
Thread Pool không làm query riêng lẻ tự nhiên nhanh hơn.
Thread Pool giúp hệ thống ổn định hơn khi có nhiều connection cạnh tranh.
```

### 2.4. Cách MariaDB Thread Pool hoạt động ở mức khái niệm

MariaDB Thread Pool là cơ chế dynamic/adaptive. Nó không chỉ tạo một số thread cố định rồi giữ nguyên mãi. Pool có thể tăng hoặc giảm số thread tùy tình huống:

- khi workload tăng, pool có thể tạo thêm worker thread;
- khi thread rảnh lâu, pool có thể retire thread;
- khi phát hiện một nhóm thread bị stall, pool có thể đánh thức hoặc tạo thêm worker để tránh bị nghẽn;
- khi đạt giới hạn tối đa, pool không tiếp tục tạo thread mới trong hầu hết trường hợp.

Trên Unix-like system, MariaDB chia connection thành các **thread group**. Mỗi thread group có queue riêng và worker thread riêng.

Có thể hình dung:

```text
Connection 1, 2, 3, ...
        |
        v
Thread Pool
        |
        +-- Thread Group 1 -> worker thread
        +-- Thread Group 2 -> worker thread
        +-- Thread Group 3 -> worker thread
        ...
```

`thread_pool_size` quyết định số thread group. Vì mỗi thread group có thể chạy statement độc lập, biến này ảnh hưởng trực tiếp đến mức độ song song của workload.

Trên Windows, MariaDB dùng native Windows Thread Pool dựa trên API của hệ điều hành. Vì implementation khác nhau, một số biến giữa Unix và Windows cũng khác nhau.

### 2.5. Khi nào nên dùng Thread Pool?

Thread Pool thường hiệu quả với workload:

- OLTP;
- query ngắn;
- số connection lớn;
- workload chủ yếu CPU-bound hoặc có nhiều connection cạnh tranh;
- nhiều client gửi query nhỏ, nhanh, lặp lại;
- hệ thống cần throughput ổn định hơn khi concurrency tăng.

Ví dụ phù hợp:

- ứng dụng web/API có nhiều request đồng thời;
- nhiều connection từ connection pool application;
- workload transaction ngắn;
- query đã có index tốt, thời gian chạy ngắn;
- bottleneck nằm ở concurrency/thread scheduling hơn là một query chậm riêng lẻ.

Thread Pool cũng có thể giúp khi workload không hoàn toàn CPU-bound, vì giới hạn số thread giúp tiết kiệm memory cho các vùng quan trọng hơn như buffer pool.

### 2.6. Khi nào Thread Pool kém hiệu quả?

Thread Pool không phải lúc nào cũng tốt hơn mô hình cũ.

Các trường hợp cần cẩn thận:

- workload rất bursty: có thời gian rảnh dài, sau đó đột ngột có spike lớn;
- nhiều query rất dài;
- nhiều query non-yielding, tức query chạy lâu nhưng không trả quyền điều phối cho pool;
- workload data warehouse hoặc analytical query nặng;
- workload yêu cầu simple query luôn phải trả về cực nhanh, không chấp nhận queue delay;
- ứng dụng bị lock contention nặng, khiến nhiều thread bị block lâu;
- benchmark chạy client và server trên cùng máy, làm thread benchmark tranh CPU với thread database.

Điểm quan trọng: Thread Pool có thể làm một query đơn giản bị chậm hơn trong lúc server bận, vì query đó phải xếp hàng chờ worker thread. Ví dụ một câu `SELECT 1` bình thường rất nhanh, nhưng nếu thread group đang bận, nó vẫn có thể phải chờ.

Vì vậy, Thread Pool tối ưu throughput tổng thể và khả năng chịu concurrency, không đảm bảo giảm latency cho từng query trong mọi tình huống.

### 2.7. Biến chính của Thread Pool

Biến trung tâm là `thread_handling`.

| Biến | Giá trị thường gặp / mặc định | Ý nghĩa |
| --- | --- | --- |
| `thread_handling` | Unix thường mặc định `one-thread-per-connection`; Windows thường dùng `pool-of-threads` | Chọn mô hình xử lý thread. `one-thread-per-connection` là mỗi connection một thread. `pool-of-threads` là dùng Thread Pool. |
| `thread_pool_size` | Mặc định thường bằng số CPU/core logic của hệ thống | Số thread group trong Thread Pool. Ảnh hưởng đến số statement có thể chạy song song. |
| `thread_pool_max_threads` | Unix thường mặc định `65536`; Windows thường mặc định `1000` | Số thread tối đa trong pool. Khi đạt giới hạn, pool thường không tạo thêm thread mới. |
| `thread_pool_stall_limit` | Mặc định thường `500` millisecond | Khoảng thời gian giữa các lần kiểm tra stall. Dùng để phát hiện thread group bị kẹt. |
| `thread_pool_oversubscribe` | Mặc định thường `3` | Cho phép nhiều worker trong cùng thread group cùng active khi cần xử lý stall hoặc tránh nghẽn. |
| `thread_pool_idle_timeout` | Mặc định thường `60` giây trên Unix | Thời gian một idle worker chờ trước khi tự thoát nếu không có việc. |
| `thread_pool_min_threads` | Mặc định thường `1` trên Windows | Số thread tối thiểu trong Windows Thread Pool. Hữu ích với workload bursty để tránh phải tạo lại thread khi spike đến. |
| `thread_pool_priority` | Mặc định thường `auto` | Điều khiển ưu tiên connection/query. `auto` thường ưu tiên query trong transaction để transaction kết thúc nhanh hơn. |
| `thread_pool_prio_kickup_timer` | Giá trị tùy phiên bản/cấu hình | Thời gian để connection priority thấp được đẩy lên priority cao hơn, tránh bị chờ vô hạn. |
| `extra_port` | Mặc định thường không bật | Port phụ dành cho kết nối quản trị khi main port bị nghẽn do Thread Pool bị block. |
| `extra_max_connections` | Giá trị nhỏ, tùy cấu hình | Số connection tối đa cho extra port. |

Không nên hiểu các biến này là càng lớn càng tốt. Đặc biệt:

- `thread_pool_size` quá thấp có thể làm query xếp hàng nhiều;
- `thread_pool_size` quá cao có thể làm mất lợi ích giới hạn thread;
- `thread_pool_max_threads` quá cao có thể cho phép server tạo quá nhiều thread khi bị block;
- `thread_pool_max_threads` quá thấp có thể làm pool bị nghẽn khi workload có nhiều lock hoặc I/O wait;
- `thread_pool_oversubscribe` cao hơn giúp tránh stall nhưng tăng cạnh tranh CPU;
- `thread_pool_idle_timeout` thấp giúp giảm thread rảnh nhưng workload bursty có thể phải trả chi phí tạo thread lại.

### 2.8. Tinh chỉnh theo CPU, RAM và loại workload

Thread Pool phải được hiểu theo tài nguyên thực tế của server. Các biến không nên chỉnh theo cảm tính, mà nên nhìn theo bốn nhóm:

```text
CPU -> quyết định mức song song hợp lý
RAM -> quyết định số thread/connection có thể giữ mà không lấn buffer pool
I/O -> quyết định query có hay bị chờ disk/network không
Workload -> OLTP, OLAP, mixed, bursty hay long-running
```

Với **OLTP**, workload thường là nhiều transaction ngắn, query nhỏ, truy cập bằng index, số connection đồng thời cao. Đây là nhóm phù hợp nhất với Thread Pool.

Nguyên tắc sizing cho OLTP:

- `thread_pool_size` thường bắt đầu quanh số CPU/core logic;
- nếu CPU còn rảnh nhưng queue trong thread group tích tụ, có thể `thread_pool_size` đang thấp;
- nếu CPU đã bão hòa và context switch cao, tăng `thread_pool_size` thường làm tệ hơn;
- `thread_pool_max_threads` không nên quá thấp nếu hệ thống hay có lock wait hoặc I/O wait;
- `thread_pool_stall_limit` cần đủ nhạy để phát hiện worker bị kẹt, nhưng không quá nhạy khiến pool tạo thêm worker quá thường xuyên;
- `thread_pool_oversubscribe` nên giữ gần default nếu chưa có bằng chứng thread group bị stall;
- RAM nên ưu tiên cho InnoDB buffer pool hơn là giữ quá nhiều thread/connection.

Với **OLAP** hoặc analytical workload, query thường dài, scan nhiều dữ liệu, sort/hash/join lớn, dùng nhiều CPU và memory cho từng query. Thread Pool có thể kém hiệu quả hơn vì giới hạn worker active có thể làm query xếp hàng, còn mỗi query riêng lẻ đã nặng.

Nguyên tắc sizing cho OLAP:

- không tăng `thread_pool_size` quá cao chỉ để chạy nhiều query nặng song song;
- giới hạn concurrency ở tầng application hoặc job scheduler thường quan trọng hơn;
- RAM cần dành cho buffer pool, sort/join memory và temporary table;
- nếu query dài chiếm worker lâu, cần theo dõi stall và queue thay vì chỉ tăng số thread;
- đôi khi `one-thread-per-connection` hoặc connection pool giới hạn nhỏ ở application dễ đoán hơn.

Với **mixed workload** vừa OLTP vừa báo cáo:

- nên tách workload nếu có thể: OLTP đi đường chính, report đi replica hoặc batch window;
- nếu không tách được, Thread Pool giúp tránh report tạo quá nhiều thread, nhưng report dài vẫn có thể làm query ngắn phải chờ;
- nên kiểm tra latency p95/p99 của OLTP, không chỉ nhìn throughput tổng;
- priority scheduling có thể hữu ích nếu transaction OLTP cần hoàn tất nhanh hơn.

Với **workload bursty**:

- `thread_pool_idle_timeout` quá thấp có thể làm thread bị retire trước spike tiếp theo;
- trên Windows, `thread_pool_min_threads` có thể giúp giữ sẵn một lượng thread tối thiểu;
- `thread_pool_max_threads` cần đủ chỗ cho spike hợp lý, nhưng không mở quá rộng đến mức spike tạo quá nhiều thread;
- application connection pool cũng phải giới hạn, vì database Thread Pool không nên là lớp duy nhất chống quá tải.

Với server **CPU ít, RAM ít**:

- không nên mở quá nhiều connection ở application;
- Thread Pool có thể giúp vì giảm số thread active;
- RAM nên ưu tiên buffer pool;
- theo dõi idle thread và max thread để tránh giữ tài nguyên không cần thiết.

Với server **nhiều CPU, nhiều RAM**:

- default `thread_pool_size` theo CPU là điểm khởi đầu hợp lý;
- không nên tự động tăng vượt CPU quá xa nếu chưa thấy queue/stall;
- có thể cần tăng giới hạn nếu workload có nhiều I/O wait, nhưng phải kiểm tra context switch;
- RAM nhiều không có nghĩa là nên cho nhiều thread hơn, vì CPU scheduling và lock contention vẫn là giới hạn thật.

Tóm tắt theo loại workload:

| Loại workload | Thread Pool có hợp không? | Hướng tinh chỉnh |
| --- | --- | --- |
| OLTP nhiều connection, query ngắn | Rất phù hợp | Bắt đầu `thread_pool_size` theo CPU, giữ oversubscribe gần default, theo dõi p95/p99 và queue. |
| OLTP nhưng lock wait cao | Có thể giúp nhưng không chữa gốc | Xem lại transaction dài, index, lock; `thread_pool_max_threads` quá thấp có thể làm nghẽn nặng hơn. |
| OLAP/report query dài | Cần cẩn thận | Giới hạn concurrency report, ưu tiên RAM cho buffer/sort/temp, không tăng thread quá nhiều. |
| Mixed OLTP + report | Dùng được nếu đo kỹ | Tách workload nếu có thể, theo dõi latency OLTP, cân nhắc priority. |
| Bursty traffic | Có lợi nếu giữ thread hợp lý | Tránh retire thread quá nhanh; cân nhắc min thread trên Windows; app pool phải có backpressure. |
| App ít connection, query nhẹ | Lợi ích ít | Mô hình cũ có thể đủ, Thread Pool không tạo khác biệt lớn. |

### 2.9. `thread_pool_size` nên hiểu như thế nào?

`thread_pool_size` không phải là "số thread tối đa". Nó là số **thread group**.

Trên Unix, mỗi thread group quản lý một nhóm connection và có worker/listener riêng. Vì vậy `thread_pool_size` quyết định mức độ chia nhỏ connection và mức song song của pool.

Default thường bằng số CPU của hệ thống vì đây là điểm khởi đầu hợp lý:

```text
số CPU/core logic
-> số thread group mặc định
-> cố gắng có mức song song gần với khả năng CPU
```

Nếu đặt quá thấp:

- ít thread group;
- nhiều connection dồn vào ít queue;
- query ngắn cũng có thể phải chờ;
- latency tăng khi concurrency cao.

Nếu đặt quá cao:

- quá nhiều thread group;
- overhead quản lý tăng;
- có thể quay lại vấn đề nhiều thread tranh CPU;
- lợi ích Thread Pool giảm.

Với OLTP, default theo số CPU thường là điểm bắt đầu hợp lý. Sau đó mới đánh giá bằng metric thực tế.

Một hiểu nhầm phổ biến:

```text
1 thread group = 1 thread = 1 CPU
```

Cách hiểu này không chính xác.

Đúng hơn là:

```text
thread_pool_size = số thread group
mỗi thread group = một đơn vị queue/scheduling nội bộ
mỗi thread group thường cố giữ số worker active thấp
CPU = tài nguyên thực thi do hệ điều hành schedule
```

Một thread group không bị "pin" cứng vào một CPU cụ thể. MariaDB không nói rằng Thread Group 1 luôn chạy trên CPU 1, Thread Group 2 luôn chạy trên CPU 2. Việc thread thật sự chạy trên CPU nào là do hệ điều hành scheduler quyết định, trừ khi hệ thống có cấu hình CPU affinity ở bên ngoài.

Vì sao default lại thường bằng số CPU? Vì đó là heuristic hợp lý:

```text
N CPU
-> bắt đầu với khoảng N thread group
-> cố tránh có quá nhiều dòng thực thi active hơn khả năng CPU
-> vẫn đủ group để nhiều connection được chia tải
```

Nhưng đây chỉ là điểm bắt đầu, không phải luật cứng.

Trong mỗi thread group, thường chỉ nên có một worker active trong điều kiện bình thường để giảm cạnh tranh CPU. Nhưng khi có stall, `thread_pool_oversubscribe` cho phép nhiều worker trong cùng group cùng active tạm thời. Vì vậy cũng không đúng nếu nói một thread group luôn chỉ có đúng một thread.

Nói cách khác, cách hiểu gần đúng là:

```text
1 thread group
-> 1 queue riêng
-> bình thường thường có 1 worker thread active
-> các query trong cùng group có xu hướng chạy tuần tự theo queue
-> khi stall/wait, có thể có thêm worker tạm thời
```

Cách hiểu thực tế:

```text
bình thường:
thread group -> ít worker active, thường gần 1

khi stall hoặc wait:
thread group -> có thể thêm worker tạm thời

giới hạn tổng:
thread_pool_max_threads kiểm soát trần số thread trong pool
```

#### Tiêu chí chọn `thread_pool_size`

Nên chọn `thread_pool_size` bằng cách bắt đầu từ CPU rồi kiểm chứng bằng metric.

Các tiêu chí chính:

| Dấu hiệu | Ý nghĩa | Hướng nghĩ |
| --- | --- | --- |
| CPU chưa bão hòa, nhưng query phải chờ queue | Có thể quá ít thread group hoặc group bị stall | Cân nhắc tăng `thread_pool_size` hoặc xử lý stall/lock. |
| CPU bão hòa, context switch cao | Đã có quá nhiều runnable work | Không nên tăng `thread_pool_size`; cần giảm concurrency hoặc tối ưu query. |
| Nhiều thread group có queue dài | Chia tải chưa đủ hoặc workload vượt năng lực server | Có thể tăng nhẹ `thread_pool_size`, nhưng phải đo latency và CPU. |
| Chỉ một vài group bị kẹt | Có thể do connection/query trong group đó bị lock/I/O lâu | Xem stall, lock wait, query dài; không vội tăng toàn cục. |
| Workload OLTP query ngắn | Thread Pool thường hiệu quả | Bắt đầu gần số CPU, đo p95/p99. |
| Workload OLAP query dài | Tăng group có thể làm nhiều query nặng chạy song song hơn | Thường nên giới hạn concurrency thay vì tăng group nhiều. |
| RAM bị áp lực | Quá nhiều thread/connection làm giảm RAM cho buffer pool | Giảm connection thật, app pool, hoặc giới hạn thread. |

Ví dụ tư duy:

```text
Server 16 CPU, OLTP nhiều query ngắn:
-> bắt đầu quanh 16 thread group
-> nếu queue dài nhưng CPU mới 50-60%, có thể thử tăng nhẹ
-> nếu CPU 95% và context switch cao, không tăng nữa

Server 16 CPU, report query dài:
-> không nên tự động cho 16-32 report chạy song song
-> giới hạn concurrency report quan trọng hơn tăng thread_pool_size
```

#### Query/connection được đưa vào thread group nào?

Ở mức concept, MariaDB không chọn group bằng cách nhìn nội dung SQL rồi load balance như proxy. Thread Pool hoạt động ở tầng connection/thread scheduling, không phải tầng query router.

Luồng đơn giản:

```text
client connection đi vào server
-> server gán connection đó vào một thread group
-> query của connection đó được đưa vào queue của group
-> worker trong group lấy work ra xử lý
```

Điều này nghĩa là:

- không phải mỗi query mới lại được random sang group khác;
- không phải MariaDB đọc SQL rồi quyết định group theo bảng/index/query type;
- connection thường gắn với một group trong quá trình phục vụ;
- group assignment là chi tiết nội bộ của Thread Pool, nhằm phân phối connection tương đối đều giữa các group;
- nếu một connection trong group chạy query dài, các query khác cùng group có thể bị ảnh hưởng cho đến khi stall detection/oversubscribe can thiệp.

Có thể hình dung gần đúng:

```text
Connection A -> Thread Group 1 -> các query của A xếp trong group 1
Connection B -> Thread Group 2 -> các query của B xếp trong group 2
Connection C -> Thread Group 1 -> các query của C cũng xếp trong group 1
```

Vì vậy `thread_pool_size` càng thấp thì càng nhiều connection phải chia sẻ cùng một group, queue dễ dài hơn. `thread_pool_size` càng cao thì connection được chia ra nhiều group hơn, nhưng overhead scheduling cũng tăng.

### 2.10. `thread_pool_stall_limit` và phát hiện kẹt

Thread Pool phải xử lý một vấn đề khó: nếu một worker thread trong thread group bị block quá lâu, các connection khác trong cùng group có thể bị chờ theo.

Ví dụ worker đang chạy query nhưng bị:

- chờ row lock;
- chờ disk I/O;
- chờ network I/O;
- chờ global lock;
- chạy query quá lâu.

Nếu thread group chỉ có một worker active, các query khác trong group có thể không được phục vụ kịp.

`thread_pool_stall_limit` là khoảng thời gian để timer thread kiểm tra xem thread group có bị stall không. Default thường `500ms`.

Nếu phát hiện stall, MariaDB có thể đánh thức hoặc tạo thêm worker trong thread group để các connection khác vẫn có cơ hội chạy.

Về concept:

```text
worker bị kẹt
-> timer phát hiện thread group bị stall
-> pool cho thêm worker chạy tạm thời
-> tránh một connection độc chiếm cả group
```

Nhưng nếu tạo quá nhiều worker để bù stall, hệ thống lại có thể tăng context switching. Vì vậy stall detection là cơ chế cân bằng giữa fairness và overhead.

### 2.11. `thread_pool_oversubscribe`

Thông thường, Thread Pool cố tránh để quá nhiều worker trong cùng thread group active cùng lúc. Nhưng nếu một worker bị stall, pool cần cho thêm worker khác chạy để tránh nghẽn.

`thread_pool_oversubscribe` quy định mức cho phép nhiều worker trong cùng thread group cùng active khi cần. Default thường `3`.

Nếu giá trị thấp:

- ít worker active hơn;
- giảm cạnh tranh CPU;
- nhưng dễ queue lâu nếu worker bị block.

Nếu giá trị cao:

- nhiều worker có thể chạy cùng lúc hơn;
- giảm nguy cơ một thread group bị kẹt;
- nhưng tăng overhead scheduling và context switching.

Biến này thường không nên chỉnh tùy tiện nếu chưa có bằng chứng stall hoặc queue delay.

### 2.12. Lập lịch ưu tiên

MariaDB Thread Pool có cơ chế ưu tiên connection/query thông qua `thread_pool_priority`.

Giá trị thường gặp:

| Giá trị | Ý nghĩa |
| --- | --- |
| `auto` | Mặc định phổ biến. MariaDB tự ưu tiên một số connection, đặc biệt là connection đang ở trong transaction. |
| `high` | Ưu tiên cao. |
| `low` | Ưu tiên thấp. |

Lý do `auto` thường ưu tiên connection trong transaction là để transaction kết thúc nhanh hơn. Transaction giữ lock càng lâu thì càng dễ làm các transaction khác chờ. Nếu ưu tiên query trong transaction, server có thể giảm số transaction treo đồng thời và giảm lock wait tổng thể.

Tuy nhiên, ưu tiên cao cũng cần cơ chế chống starvation. Nếu low priority bị bỏ đói quá lâu, MariaDB có thể đẩy connection đó lên high priority sau một khoảng thời gian do `thread_pool_prio_kickup_timer` kiểm soát.

Về concept:

```text
transaction đang chạy
-> nên hoàn tất nhanh
-> giảm lock giữ lâu
-> giảm số transaction chờ nhau
```

### 2.13. Cổng phụ để cứu hệ thống khi Thread Pool bị kẹt

Trong một số tình huống xấu, tất cả thread trong pool có thể bị block. Ví dụ:

- có global lock;
- có `FLUSH TABLES WITH READ LOCK`;
- nhiều connection bị chờ lock;
- `thread_pool_max_threads` đã đạt giới hạn;
- main port vẫn nhận connection nhưng không còn worker xử lý hiệu quả.

Khi đó DBA có thể không kết nối được vào server qua port chính để xử lý sự cố. `extra_port` tồn tại để giải quyết tình huống này.

Extra port là port phụ dành cho kết nối quản trị. Điểm đáng chú ý là extra port dùng cơ chế thread handling kiểu cũ hơn, không phụ thuộc hoàn toàn vào Thread Pool chính. Nhờ vậy, kể cả khi pool chính bị nghẽn, admin vẫn có thể có đường vào để kiểm tra và xử lý.

Các biến liên quan:

| Biến | Ý nghĩa |
| --- | --- |
| `extra_port` | Port phụ cho kết nối quản trị. Nếu không bật thì không có đường vào phụ. |
| `extra_max_connections` | Số connection tối đa được phép đi qua extra port. |

Extra port không phải để phục vụ traffic application. Nó là đường quản trị khẩn cấp.

### 2.14. Theo dõi Thread Pool

Các status variable cơ bản:

| Biến | Ý nghĩa |
| --- | --- |
| `Threadpool_threads` | Số thread hiện có trong Thread Pool. Trong một số trường hợp có thể cao hơn `thread_pool_max_threads` một chút vì mỗi thread group cần ít nhất worker/listener để tránh deadlock. |
| `Threadpool_idle_threads` | Số thread idle trong pool. Thread có thể idle vì chưa có work, hoặc đang chờ I/O/lock. Biến này có ý nghĩa rõ hơn trên Unix. |

Các bảng Information Schema liên quan:

| Bảng | Mục đích |
| --- | --- |
| `THREAD_POOL_GROUPS` | Xem trạng thái các thread group. |
| `THREAD_POOL_QUEUES` | Xem queue của Thread Pool. |
| `THREAD_POOL_STATS` | Xem thống kê hoạt động của pool. |
| `THREAD_POOL_WAITS` | Xem thông tin wait liên quan đến Thread Pool. |

Khi đánh giá Thread Pool, nên nhìn theo hướng:

- số thread có tăng sát `thread_pool_max_threads` không;
- idle thread có quá nhiều không;
- queue có tích tụ không;
- có thread group nào bị stall thường xuyên không;
- có nhiều connection chờ lock/I/O không;
- throughput tăng hay chỉ latency tăng;
- CPU context switch có giảm không.

### 2.15. Thread Pool bị block

Thread Pool có thể bị block nếu nhiều worker cùng bị giữ bởi lock hoặc operation dài. Một ví dụ kinh điển là global lock:

```text
Connection A giữ global/read lock và không nhả
Connection B, C, D, ... nối vào và bị chờ
Thread Pool tạo thêm worker để phục vụ nhưng cuối cùng cũng đạt giới hạn
Main port không còn xử lý được connection quản trị
```

Khi đó server không hẳn đã crash, nhưng gần như không điều khiển được qua đường bình thường.

Cách hiểu:

```text
Thread Pool bảo vệ server khỏi quá nhiều thread
nhưng nếu toàn bộ worker đều bị block
thì chính giới hạn đó có thể làm admin khó vào xử lý
```

Vì vậy extra port là một phần quan trọng của thiết kế vận hành nếu dùng Thread Pool trong production.

### 2.16. So sánh với các database SQL khác

Không phải database SQL nào cũng có "Thread Pool" cùng nghĩa với MariaDB. Mỗi hệ quản trị có mô hình concurrency riêng.

| Database | Có Thread Pool giống MariaDB không? | Nếu không giống thì dựa vào gì? | Ý nghĩa thực tế |
| --- | --- | --- | --- |
| MariaDB | Có, built-in | `pool-of-threads`, thread group trên Unix, native thread pool trên Windows | Dùng để giới hạn thread active khi nhiều connection, rất phù hợp OLTP concurrency cao. |
| MySQL Community | Không có Thread Pool Enterprise | Chủ yếu dùng mô hình thread/connection handler truyền thống, kết hợp connection pool ở application/proxy | Nếu có quá nhiều connection, thường phải kiểm soát bằng application pool, proxy hoặc giới hạn connection. |
| MySQL Enterprise | Có MySQL Enterprise Thread Pool | Thread Pool dạng plugin/feature Enterprise | Concept gần MariaDB nhưng là tính năng thương mại và có khác biệt implementation. |
| PostgreSQL | Không có Thread Pool kiểu MariaDB trong core | Process-per-connection; thường dùng PgBouncer hoặc application connection pool để giảm connection thật vào DB | PostgreSQL cô lập connection tốt bằng process riêng, nhưng nhiều connection thật sẽ tốn process/RAM, nên pooling bên ngoài rất quan trọng. |
| Oracle Database | Không gọi là Thread Pool kiểu MariaDB | Dedicated Server, Shared Server, DRCP, process/session architecture | Oracle xử lý áp lực session/connection bằng mô hình server process/session và pooling riêng, không phải thread group như MariaDB. |
| SQL Server | Có worker scheduler nội bộ | SQLOS, worker pool, scheduler per CPU, `max worker threads` | SQL Server tự điều phối worker trong engine; khi cạn worker có thể gặp wait kiểu `THREADPOOL`. |

Cách hiểu nhanh:

```text
MariaDB Thread Pool
= giảm số worker thread active so với số connection
= dùng thread group/queue trong Unix implementation

PostgreSQL
= không gom connection vào thread group trong core
= thường dùng PgBouncer/application pool để giảm số backend process

Oracle
= có Dedicated/Shared Server/DRCP để xử lý nhiều session
= không phải cùng mô hình thread group như MariaDB

SQL Server
= có scheduler và worker pool nội bộ của SQLOS
= DBA thường nhìn max worker threads và THREADPOOL waits
```

Điểm cần rút ra: mỗi database đều phải xử lý bài toán nhiều connection/query đồng thời, nhưng vị trí xử lý khác nhau. MariaDB Thread Pool xử lý trực tiếp trong database server bằng thread group. PostgreSQL thường đẩy phần "giảm connection thật" ra PgBouncer/application pool. Oracle có Shared Server/DRCP. SQL Server có SQLOS scheduler và worker pool nội bộ.

Vì vậy, khi so sánh database, không nên chỉ hỏi "có Thread Pool không?". Nên hỏi:

```text
Database xử lý nhiều connection bằng cách nào?
Một connection có tương ứng với thread/process riêng không?
Concurrency được giới hạn ở database hay ở application/proxy?
Nếu một query chậm, nó ảnh hưởng connection khác theo cách nào?
Khi quá nhiều worker/process/thread, database báo hiệu bằng metric/wait nào?
```

### 2.17. Thread group so với connection pool + one-thread-per-connection

Có hai cách phổ biến để kiểm soát số query đồng thời đi vào database:

```text
Thread group trong MariaDB Thread Pool
= database tự giới hạn và điều phối worker thread bên trong server

Connection pool + one-thread-per-connection
= application/proxy giới hạn số connection thật vào database,
   còn database vẫn xử lý mỗi connection bằng một thread riêng
```

Hai cách này giải quyết cùng một áp lực là **quá nhiều client/query đồng thời**, nhưng xử lý ở hai tầng khác nhau.

| Tiêu chí | Thread group trong MariaDB Thread Pool | Connection pool + `one-thread-per-connection` |
| --- | --- | --- |
| Tầng kiểm soát concurrency | Bên trong MariaDB server | Bên ngoài database, thường ở application hoặc proxy |
| Mô hình xử lý | Nhiều connection được chia vào thread group/queue, worker xử lý theo nhóm | Pool giới hạn số connection thật; mỗi connection thật vào DB có thread riêng |
| Query chậm ảnh hưởng ai? | Có thể kéo chậm các request phía sau trong cùng group cho đến khi stall detection/oversubscribe can thiệp | Chủ yếu ảnh hưởng thread/connection đang chạy query đó; connection khác trong pool vẫn có thread riêng nếu còn slot |
| Overhead khi rất nhiều connection | Thấp hơn vì DB không tạo một thread cho mỗi connection active | Phụ thuộc pool size; nếu pool giữ số connection thật thấp thì ổn, nếu mở quá nhiều vẫn nhiều thread |
| Cô lập query chậm | Kém hơn lúc đầu vì cùng group có queue chung | Tốt hơn, vì mỗi connection đang chạy có thread riêng |
| Kiểm soát số query chạy đồng thời | DB tự điều tiết qua thread group, stall, oversubscribe, max thread | App/proxy quyết định pool size, queue request ở ngoài DB |
| Rủi ro chính | Group bị stall, lệch tải giữa group, cần tuning `thread_pool_size`/`stall_limit` | Pool quá nhỏ gây queue ở app; pool quá lớn gây quá nhiều DB thread |
| Phù hợp | Rất nhiều connection, OLTP query ngắn, muốn giảm thread/context switching trong DB | Muốn kiểm soát rõ concurrency từ app, muốn cô lập query chậm tốt hơn, hệ thống có app pool tốt |

Với connection pool + `one-thread-per-connection`, luồng thường là:

```text
1000 request từ client
-> application connection pool chỉ cho 50 connection thật vào DB
-> MariaDB tạo khoảng 50 thread cho 50 connection đó
-> request còn lại chờ ở application
```

Ưu điểm là database không phải thấy toàn bộ 1000 connection cùng lúc. Query chậm thường chỉ giữ thread của connection đó:

```text
Connection A -> Thread A -> query chậm
Connection B -> Thread B -> vẫn có thể chạy
Connection C -> Thread C -> vẫn có thể chạy
```

Nhược điểm là nếu pool size đặt quá lớn, database vẫn quay lại bài toán cũ:

```text
pool size quá lớn
-> nhiều connection thật vào DB
-> nhiều DB thread
-> context switch, RAM, lock contention tăng
```

Nếu pool size đặt quá nhỏ:

```text
pool size quá nhỏ
-> request chờ ở application
-> DB nhàn hơn nhưng latency phía app tăng
-> có thể tạo timeout ở tầng app
```

Với MariaDB Thread Pool theo thread group, luồng là:

```text
nhiều connection vào MariaDB
-> MariaDB chia connection vào nhiều thread group
-> mỗi group có queue riêng
-> số worker active được giữ thấp hơn số connection
```

Ưu điểm là database tự giảm số thread active và giảm overhead scheduling. Nhược điểm là query chậm trong một group có thể làm request phía sau group đó chờ:

```text
Group 3:
Q1 chậm
Q2 chờ
Q3 chờ
Q4 chờ
```

MariaDB bù bằng:

```text
thread_pool_stall_limit
-> timer phát hiện group bị stall

thread_pool_oversubscribe
-> cho phép thêm worker active tạm thời trong group

sleeping worker / create worker
-> đánh thức hoặc tạo worker để xử lý request khác
```

Vì vậy, so sánh đúng nhất là:

```text
connection pool + one-thread-per-connection
= cô lập query chậm tốt hơn
= concurrency được kiểm soát bên ngoài database
= cần app/proxy pool được cấu hình tốt

thread group
= tiết kiệm thread hơn khi có rất nhiều connection
= database tự điều phối concurrency
= có rủi ro group bị kéo chậm bởi query chậm, nhưng có stall/oversubscribe để giảm rủi ro
```

Không có lựa chọn nào luôn tốt hơn. Nếu ứng dụng có connection pool tốt, số connection thật vào DB được giữ hợp lý, workload không quá nhiều connection, thì `one-thread-per-connection` có thể dễ đoán và cô lập query chậm tốt hơn.

Nếu hệ thống có rất nhiều connection, nhiều query ngắn kiểu OLTP và muốn giảm số thread active trong database, Thread Pool theo thread group thường hợp lý hơn.

Kết luận thực dụng:

```text
Muốn cô lập query chậm:
-> connection pool + one-thread-per-connection dễ đoán hơn

Muốn chịu nhiều connection với ít thread active hơn:
-> MariaDB Thread Pool / thread group hợp lý hơn

Muốn tốt nhất trong production:
-> vẫn nên có application connection pool hợp lý,
   dù database có dùng Thread Pool hay không
```

### 2.18. MariaDB Thread Pool so với MySQL Enterprise Thread Pool

MySQL bản cộng đồng không có Thread Pool tương đương MariaDB Thread Pool. Thread Pool trong hệ MySQL chủ yếu nằm ở Oracle MySQL Enterprise.

So sánh concept:

| Điểm | MariaDB Thread Pool | Oracle MySQL Enterprise Thread Pool |
| --- | --- | --- |
| Tính sẵn có | Built-in trong MariaDB | Có trong MySQL Enterprise, không phải MySQL Community |
| Unix thread group | Có concept thread group | Có concept tương tự |
| Windows implementation | Dùng native Windows thread pool | Implementation khác, dựa trên cách Oracle MySQL triển khai |
| I/O multiplexing | MariaDB chọn cơ chế phù hợp theo OS | Oracle MySQL Enterprise có implementation riêng |
| Plugin | Không phải plugin ngoài trong MariaDB | Trong MySQL Enterprise là feature thương mại |

Điểm thực dụng: nếu đang dùng MariaDB, Thread Pool là một tính năng có sẵn để xem xét cho workload concurrency cao. Nếu đang dùng MySQL Community, không nên mặc định nghĩ rằng có cùng feature này.

### 2.19. MariaDB Thread Pool so với Percona Thread Pool

Percona Thread Pool có nguồn gốc/ý tưởng gần với MariaDB Thread Pool nhưng có thêm một số khác biệt.

Một số điểm khác thường gặp:

- Percona có cơ chế priority scheduling riêng ở một số phiên bản;
- tên biến priority có thể khác MariaDB;
- MariaDB có `thread_pool_priority` với giá trị như `auto`, `high`, `low`;
- MariaDB có `thread_pool_prio_kickup_timer`, trong khi Percona có thể dùng biến khác để xử lý low priority;
- concept chung vẫn là giới hạn thread active và chia connection vào nhóm/queue.

Khi đọc tài liệu hoặc benchmark, cần kiểm tra đang nói về MariaDB, Oracle MySQL Enterprise hay Percona, vì tên biến và default có thể khác nhau.

### 2.20. Lưu ý khi đo hiệu năng Thread Pool

Benchmark Thread Pool dễ bị sai nếu client benchmark và database server chạy trên cùng một máy.

Ví dụ dùng công cụ benchmark tạo rất nhiều client thread trên cùng server database:

```text
client benchmark threads
vs
database worker threads
```

Cả hai cùng tranh CPU. Hệ điều hành có thể schedule client threads nhiều hơn database threads, làm kết quả méo. Khi đó benchmark có thể đo cả chi phí của công cụ benchmark, không chỉ đo MariaDB.

Nguyên tắc tốt hơn:

- chạy benchmark client và database trên máy khác nhau nếu có thể;
- nếu cùng máy, tách CPU affinity giữa client và server;
- chú ý NUMA node;
- không chỉ nhìn QPS, cần nhìn latency percentile;
- kiểm tra context switch, CPU utilization, queue, lock wait;
- so sánh với cùng workload khi dùng `one-thread-per-connection`.

Một kết quả benchmark tốt phải trả lời được:

```text
Thread Pool có tăng throughput không?
Latency p95/p99 có xấu đi không?
CPU context switch có giảm không?
Queue có tích tụ không?
Lock wait có phải nguyên nhân thật không?
```

### 2.21. Danh sách kiểm tra khi tinh chỉnh Thread Pool

Khi đánh giá Thread Pool, nên hỏi:

1. Workload có nhiều connection đồng thời không?
2. Query chủ yếu ngắn hay dài?
3. Bottleneck hiện tại là CPU scheduling, lock, I/O hay query plan?
4. `one-thread-per-connection` có tạo quá nhiều thread không?
5. CPU context switch có cao không?
6. Thread Pool có làm throughput tốt hơn không?
7. Latency p95/p99 có bị tăng quá mức không?
8. Có thread group nào bị stall thường xuyên không?
9. `thread_pool_size` có quá thấp hoặc quá cao so với CPU không?
10. Có cần extra port cho vận hành production không?

Kết luận thực dụng:

```text
Thread Pool tốt khi nhiều connection, query ngắn, OLTP, cần ổn định throughput.
Thread Pool kém hơn khi workload bursty, query dài, analytical, hoặc latency từng query quan trọng hơn throughput tổng.
```

Thread Pool nên được xem là cơ chế kiểm soát concurrency ở tầng database server. Nó không thay thế index tốt, query plan tốt, transaction ngắn và thiết kế connection pool hợp lý ở application.

## 3. Thread Group trong Unix Thread Pool

### 3.1. Phạm vi áp dụng

Mục này nói riêng về **Thread Group trong Unix implementation** của MariaDB Thread Pool. Trên Windows, MariaDB dùng native Windows Thread Pool của hệ điều hành, nên cách phân phối thread giữa CPU và cách quản lý pool không giống Unix.

Nói ngắn gọn:

```text
Unix/Linux:
MariaDB tự tổ chức Thread Pool thành các thread group.

Windows:
MariaDB dựa vào native Windows Thread Pool.
```

Vì vậy, các khái niệm như `thread_pool_size`, thread group, listener thread, worker thread, timer thread, stall detection và oversubscription trong mục này chủ yếu nên hiểu theo Unix/Linux.

### 3.2. Thread group là gì?

Trên Unix, Thread Pool dùng các object gọi là **thread group** để chia client connection thành nhiều nhóm độc lập tương đối.

`thread_pool_size` là biến quyết định số lượng thread group. Mặc định thường được auto-size theo số CPU/core logic của hệ thống.

Ý tưởng của default này là:

```text
số CPU ~= số thread group mặc định
```

Lý do: MariaDB muốn có mức song song đủ để tận dụng CPU, nhưng không tạo quá nhiều dòng thực thi active dẫn đến context switching.

Tuy nhiên:

```text
1 thread group không bằng 1 CPU cố định
1 thread group không luôn luôn chỉ có đúng 1 thread
1 thread group là một đơn vị queue/scheduling nội bộ
```

Thread thực sự chạy trên CPU nào là do OS scheduler quyết định, trừ khi hệ thống có cấu hình CPU affinity bên ngoài như giới hạn process chỉ chạy trên một số CPU.

#### Vì sao không dùng một queue chung cho toàn bộ Thread Pool?

Câu hỏi tự nhiên là: nếu mục tiêu chỉ là có nhiều worker thread xử lý nhiều query, tại sao không thiết kế đơn giản như sau?

```text
tất cả connection/query
-> một queue chung
-> nhiều worker thread cùng lấy việc ra xử lý
```

Thiết kế này có vẻ đơn giản hơn nhiều thread group. Chỉ có một hàng đợi, một nơi nhận việc, nhiều worker cùng lấy việc ra chạy. Với concurrency thấp hoặc workload nhỏ, cách này có thể đủ tốt.

Vấn đề xuất hiện khi số connection và số worker cùng tăng. Queue không chỉ là một danh sách trừu tượng. Nó là một cấu trúc dữ liệu dùng chung. Khi nhiều thread cùng thêm việc vào queue hoặc lấy việc khỏi queue, server cần đồng bộ để tránh hỏng dữ liệu nội bộ.

Luồng đơn giản của một queue chung:

```text
connection có request mới
-> lock queue chung
-> đưa request vào queue
-> unlock queue

worker muốn lấy request
-> lock queue chung
-> lấy request ra
-> unlock queue
-> xử lý request
```

Khi chỉ có vài thread, chi phí lock/unlock này nhỏ. Nhưng khi có hàng trăm hoặc hàng nghìn connection, queue chung trở thành điểm nóng:

```text
nhiều connection muốn enqueue
nhiều worker muốn dequeue
nhiều listener muốn kiểm tra event
-> tất cả cùng đụng vào một queue/mutex
-> lock contention tăng
```

Điểm nghẽn lúc này không còn nằm ở việc "có đủ worker thread hay không", mà nằm ở việc nhiều worker phải tranh nhau quyền truy cập queue chung. Thêm worker trong tình huống này chưa chắc tăng throughput, vì worker mới cũng phải tranh cùng lock.

Có thể hình dung:

```text
1 queue chung:

Worker 1 ----\
Worker 2 -----\
Worker 3 ------> cùng tranh Global Queue Lock
Worker 4 -----/
Worker 5 ----/
```

Nếu lock trên queue chung nóng, CPU có thể tốn thời gian cho:

- chờ mutex;
- wake/sleep thread;
- context switch;
- invalidation CPU cache line;
- đồng bộ metadata;
- tranh quyền lấy request tiếp theo.

MariaDB chia thành nhiều thread group để chia nhỏ điểm tranh chấp đó:

```text
nhiều connection
-> phân vào nhiều thread group
-> mỗi group có queue riêng
-> mỗi group có listener/worker riêng
-> lock contention được chia ra nhiều queue nhỏ
```

Thay vì:

```text
1000 connection tranh 1 queue
```

thành:

```text
1000 connection chia vào 16 group
-> mỗi group chịu khoảng một phần connection
-> mỗi queue có lock/metadata riêng
```

Đây là ý nghĩa chính của nhiều thread group: **không phải để chia một query thành nhiều thread**, mà để chia bài toán điều phối rất nhiều connection/query độc lập thành nhiều hàng đợi nhỏ hơn.

So sánh hai thiết kế:

| Thiết kế | Ưu điểm | Nhược điểm |
| --- | --- | --- |
| Một queue chung | Đơn giản, ít metadata, dễ hình dung, ít overhead khi tải thấp | Dễ thành điểm nóng mutex khi nhiều worker/connection cùng enqueue/dequeue |
| Nhiều thread group | Chia nhỏ lock contention, giảm áp lực lên một queue duy nhất, scale tốt hơn khi nhiều CPU/connection | Nhiều queue/listener/metadata hơn, quản lý phức tạp hơn, có thể lệch tải giữa group |

Vì vậy, trade-off của MariaDB là:

```text
chấp nhận:
  nhiều queue hơn
  nhiều metadata hơn
  cần timer/listener theo group
  có rủi ro lệch tải giữa các group

để đổi lấy:
  ít tranh chấp trên một queue toàn cục hơn
  ít worker cùng đụng một mutex hơn
  khả năng scale tốt hơn khi concurrency cao
  khả năng xử lý stall theo từng group
```

Một lợi ích khác là stall được cô lập tương đối theo group. Nếu một connection trong Group 3 chạy query dài hoặc bị lock wait, timer thread có thể phát hiện Group 3 bị stall và thêm worker cho group đó. Các group khác vẫn có queue/worker riêng và không nhất thiết bị ảnh hưởng trực tiếp.

```text
Group 1: vẫn xử lý bình thường
Group 2: vẫn xử lý bình thường
Group 3: bị stall -> timer can thiệp
Group 4: vẫn xử lý bình thường
```

Nếu chỉ có một queue chung, việc phân biệt "phần nào đang bị stall" và "phần nào vẫn khỏe" khó hơn, vì tất cả request nằm trong cùng một cấu trúc điều phối.

Cũng cần nhìn chiều ngược lại: nhiều thread group không miễn phí.

Nếu workload nhỏ:

```text
ít connection
ít query đồng thời
lock contention trên queue chung vốn không đáng kể
```

thì nhiều queue có thể không đem lại lợi ích rõ ràng. Khi đó chi phí quản lý nhiều group có thể không đáng kể nhưng cũng không giúp nhiều.

Nếu workload rất lớn:

```text
nhiều connection
nhiều query ngắn
nhiều worker cùng hoạt động
```

thì một queue chung dễ thành bottleneck. Lúc này chia thành nhiều thread group hợp lý hơn.

Kết luận đúng nhất:

```text
MariaDB không chia nhiều thread group vì một queue chung là sai.
MariaDB chia nhiều thread group vì với workload concurrency cao,
chi phí quản lý nhiều queue thường rẻ hơn chi phí lock contention
của một queue toàn cục.
```

Đây là giả định thiết kế cho server database cần scale với nhiều connection. Nó không phải luật tuyệt đối cho mọi workload, nên `thread_pool_size` vẫn cần được chọn theo CPU, số connection, queue, latency và context switch thực tế.

### 3.3. `thread_pool_size` trong Unix implementation

`thread_pool_size` là một trong các biến có ảnh hưởng dễ thấy nhất đến hiệu năng Thread Pool.

| Biến | Default / giới hạn thường gặp | Ý nghĩa |
| --- | --- | --- |
| `thread_pool_size` | Mặc định thường bằng số CPU/core logic | Số thread group trong Thread Pool. |
| Giá trị tối đa khi startup | khoảng `100000` | Giới hạn lý thuyết khi đặt từ lúc server start, nhưng không nên đặt quá cao. |
| Giá trị tối đa khi đổi runtime | thường là `128` hoặc giá trị đã đặt lúc startup, lấy giá trị lớn hơn | Giới hạn động để tránh thay đổi quá cực đoan khi server đang chạy. |

Về mặt concept, `thread_pool_size` gần tương đương với số nhóm có thể cho phép công việc chạy song song. Nhưng "chạy song song" ở đây không có nghĩa là mỗi group chiếm một CPU riêng.

Cách hiểu đúng hơn:

```text
thread_pool_size cao hơn
-> nhiều thread group hơn
-> connection được chia ra nhiều queue hơn
-> giảm nguy cơ quá nhiều connection dồn vào một group
-> nhưng tăng overhead scheduling/quản lý

thread_pool_size thấp hơn
-> ít thread group hơn
-> queue trong mỗi group có thể dài hơn
-> ít overhead hơn
-> nhưng dễ underutilize CPU nếu workload đủ nhẹ và còn CPU rảnh
```

Nếu MariaDB process bị giới hạn chỉ được dùng một số CPU, ví dụ bằng CPU affinity/cgroup/container limit, thì `thread_pool_size` nên dựa trên số CPU thực sự MariaDB được dùng, không phải tổng CPU vật lý của máy.

Ví dụ tư duy:

```text
Máy có 32 CPU
MariaDB chỉ được giới hạn chạy trên 8 CPU
-> thread_pool_size nên bắt đầu quanh 8, không phải 32
```

Nếu `thread_pool_size` bằng số CPU nhưng CPU vẫn chưa được dùng hết trong khi queue có tích tụ, có thể cân nhắc tăng nhẹ. Nhưng nếu CPU đã gần bão hòa và context switch cao, tăng `thread_pool_size` thường không giúp.

### 3.4. Connection được phân phối vào thread group như thế nào?

Khi một client connection mới được tạo, MariaDB xác định thread group cho connection đó bằng công thức concept:

```text
thread_group_id = connection_id % thread_pool_size
```

`connection_id` là số tăng dần dùng để định danh connection. Vì `connection_id` tăng dần, phép chia lấy dư với `thread_pool_size` giúp phân phối connection theo kiểu gần giống round-robin giữa các thread group.

Ví dụ `thread_pool_size = 4`:

```text
connection_id = 101 -> 101 % 4 = 1 -> Thread Group 1
connection_id = 102 -> 102 % 4 = 2 -> Thread Group 2
connection_id = 103 -> 103 % 4 = 3 -> Thread Group 3
connection_id = 104 -> 104 % 4 = 0 -> Thread Group 0
connection_id = 105 -> 105 % 4 = 1 -> Thread Group 1
```

Điểm quan trọng:

- phân phối theo connection, không phải theo từng SQL statement độc lập;
- không phải MariaDB đọc nội dung query rồi chọn group;
- không phải load balancer theo chi phí query;
- không phải random hoàn toàn;
- kết quả thường khá đều nếu connection_id tăng đều và connection sống không quá lệch.

Nếu một application giữ connection rất lâu, phân phối ban đầu có thể đều nhưng tải thực tế vẫn lệch, vì có connection trong group này chạy query nặng hơn group khác. Thread Pool xử lý phần nào bằng stall detection và oversubscription, nhưng nó không phải query load balancer thông minh.

### 3.5. Các loại thread trong Thread Pool

Trong Unix Thread Pool có ba nhóm thread chính:

| Loại thread | Phạm vi | Vai trò |
| --- | --- | --- |
| Worker thread | Thuộc thread group | Thực sự xử lý công việc cho client connection. |
| Listener thread | Thuộc thread group | Lắng nghe I/O event và phân phối work cho worker. |
| Timer thread | Global cho toàn Thread Pool | Kiểm tra stall, đảm bảo group có listener, hỗ trợ tạo thread khi cần. |

Thread group thường có ít nhất:

- một listener thread để nhận/phân phối event;
- một worker thread để xử lý request.

Trong một số trường hợp hiếm, tổng số thread có thể vượt nhẹ `thread_pool_max_threads`, vì MariaDB cần đảm bảo mỗi thread group có tối thiểu các thread cần thiết để tránh deadlock.

### 3.6. Worker thread

Worker thread là thread thực sự xử lý query hoặc công việc thay mặt client connection.

Trong trạng thái bình thường:

```text
thread group
-> một queue work
-> thường có một worker active
-> worker lấy work trong queue ra xử lý
```

Nếu workload ngắn và đều, mô hình này giúp giảm số thread active, giảm context switching và giữ CPU locality tốt hơn.

Nhưng nếu worker bị chờ lock, chờ disk I/O, chờ network I/O hoặc chạy query dài, group có thể bị kẹt. Khi đó Thread Pool có thể đánh thức worker đang ngủ hoặc tạo thêm worker mới nếu điều kiện cho phép.

Đây là rủi ro thật của thread group:

```text
Group 3 queue:
Q1 chạy lâu
Q2 chờ
Q3 chờ
Q4 chờ
```

Trong lúc bình thường, một group thường cố giữ ít worker active để giảm overhead. Vì vậy nếu `Q1` chạy lâu, các request phía sau có thể bị ảnh hưởng cho đến khi Thread Pool phát hiện stall.

MariaDB giảm rủi ro này bằng các cơ chế:

- `thread_pool_stall_limit`: timer kiểm tra group có bị stall không;
- `thread_pool_oversubscribe`: cho phép group có thêm worker active tạm thời;
- sleeping worker: ưu tiên đánh thức worker đang ngủ;
- create worker: tạo worker mới nếu không có worker ngủ và vẫn nằm trong giới hạn.

Vì vậy thiết kế thực tế là:

```text
bình thường:
ít worker active để giảm overhead

khi query chậm / wait:
timer phát hiện stall
-> thêm worker tạm thời
-> tránh cả group bị kéo chậm quá lâu
```

Tuy nhiên, nếu nhiều query trong cùng group đều dài, hoặc cả server đang bị lock/I/O wait nặng, thêm worker không giải quyết gốc vấn đề. Nó chỉ làm nhiều worker cùng chờ hơn. Đây là lý do Thread Pool hợp với OLTP query ngắn hơn là workload report/OLAP query dài.

### 3.7. Listener thread

Listener thread trong thread group lắng nghe I/O event và phân phối work cho worker thread.

Vai trò chính:

- nhận biết có request mới cần xử lý;
- đưa work vào queue của thread group;
- đánh thức worker đang ngủ nếu có;
- trong một số trường hợp, tạo worker mới;
- trong một số cấu hình, listener có thể tự trở thành worker để giảm overhead đánh thức/tạo thread.

Biến liên quan:

| Biến | Ý nghĩa |
| --- | --- |
| `thread_pool_dedicated_listener` | Nếu bật, listener được giữ chuyên trách làm listener. Nếu không bật, listener có thể trở thành worker trong một số tình huống. |

Nếu listener luôn chuyên trách, group có thể phản ứng với I/O event rõ ràng hơn. Nếu listener được phép kiêm worker, overhead có thể giảm trong workload nhẹ nhưng cần cơ chế đảm bảo group không mất listener quá lâu.

### 3.8. Timer thread

Thread Pool có một **timer thread** global cho toàn pool.

Timer thread làm các việc như:

- kiểm tra từng thread group có bị stall không;
- đảm bảo mỗi thread group có listener thread;
- đánh thức sleeping worker nếu cần;
- tạo listener/worker thread mới trong một số tình huống;
- áp dụng cơ chế throttling khi tạo thread.

Timer thread rất quan trọng vì Thread Pool không thể chỉ dựa vào worker hiện tại. Nếu worker bị kẹt và không tự nhường quyền đúng lúc, timer thread là thành phần phát hiện group đang không tiến triển.

### 3.9. Khi nào MariaDB tạo thread mới?

MariaDB thường ưu tiên **đánh thức sleeping worker thread đã có sẵn** thay vì tạo thread mới. Tạo thread mới tốn chi phí hơn đánh thức thread đang ngủ.

Thứ tự ưu tiên concept:

```text
có sleeping worker
-> đánh thức sleeping worker

không có sleeping worker
-> nếu đủ điều kiện, tạo worker/listener mới
```

Thread mới có thể được tạo trong các nhóm tình huống:

- listener thread tạo worker thread;
- worker thread tạo worker mới khi chính nó sắp phải wait;
- timer thread tạo listener thread;
- timer thread tạo worker thread khi phát hiện stall.

### 3.10. Listener thread tạo worker thread

Listener thread có thể tạo worker thread mới khi:

- có request mới cần xử lý;
- queue của group còn nhiều request cần phân phối;
- không có active worker trong group;
- không có sleeping worker để đánh thức;
- tổng số thread trong pool chưa vượt `thread_pool_max_threads`, hoặc group chưa có đủ số thread tối thiểu cần thiết.

Mục tiêu là đảm bảo thread group có worker để xử lý work, đặc biệt khi listener còn phải tiếp tục phân phối request khác.

Concept:

```text
listener thấy có work
-> không có worker active
-> không có worker ngủ để đánh thức
-> tạo worker mới nếu còn trong giới hạn
```

### 3.11. Worker thread tạo thread mới khi phải wait

Một worker thread có thể tạo worker mới khi nó sắp phải chờ một thứ gì đó, ví dụ:

- disk I/O;
- row lock;
- metadata lock;
- network wait;
- query gọi hàm ngủ/chờ;
- operation dài không thể hoàn tất ngay.

Lý do: nếu worker đang là worker active duy nhất trong group mà nó đi vào trạng thái wait, các request khác trong queue sẽ không được xử lý. Tạo hoặc đánh thức worker khác giúp group tiếp tục phục vụ connection khác.

Điều kiện concept:

- worker hiện tại phải wait;
- không còn active worker khác trong group;
- không có sleeping worker để đánh thức;
- queue còn work cần xử lý hoặc group cần listener;
- vẫn nằm trong giới hạn `thread_pool_max_threads`, hoặc group chưa có số thread tối thiểu.

Đây thường là cơ chế tạo worker mới chính trong nhiều workload thực tế, vì wait do lock/I/O rất phổ biến.

### 3.12. Timer thread tạo listener thread

Timer thread có thể tạo listener thread mới cho một group khi group có nhiều connection request cần phân phối nhưng hiện không có listener thread xử lý I/O event.

Điều kiện concept:

- group không xử lý I/O event kể từ lần kiểm tra trước;
- hiện không có listener thread;
- không có sleeping worker có thể đánh thức để làm listener;
- vẫn nằm trong giới hạn thread hoặc group chưa có số thread tối thiểu.

Mục tiêu là tránh tình trạng group có request nhưng không có listener để nhận và phân phối work.

### 3.13. Timer thread tạo worker thread khi group bị stall

Timer thread có thể tạo worker thread mới nếu nó cho rằng một thread group đang bị stall.

Một group được xem là stall khi:

- queue còn request mà listener vẫn cần phân phối cho worker;
- từ lần kiểm tra stall trước đến hiện tại, không có request nào được dequeued để chạy;
- không có sleeping worker để đánh thức;
- chưa vượt giới hạn tạo thread hoặc group chưa có số thread tối thiểu;
- chưa tạo worker mới cho group trong khoảng throttling interval.

Mục tiêu là tránh một query dài hoặc một connection bị kẹt độc chiếm cả thread group.

Concept:

```text
group có queue
nhưng không request nào được đưa ra chạy
-> timer nghi group bị stall
-> đánh thức worker ngủ hoặc tạo worker mới
-> group tạm thời có nhiều worker active hơn
```

### 3.14. Thread creation throttling

Thread Pool không nên tạo thread mới quá nhanh. Nếu cứ thấy wait/stall là tạo thread ngay, server có thể quay lại vấn đề ban đầu: quá nhiều thread, context switching cao, memory overhead lớn.

Vì vậy MariaDB có cơ chế **thread creation throttling**: trong một số tình huống, thread mới chỉ được tạo nếu trong thread group chưa có thread nào được tạo trong một khoảng thời gian nhất định.

Khoảng throttling phụ thuộc vào số thread đã có trong group và `thread_pool_stall_limit`.

Trong MariaDB 10.5 trở lên, concept thường là:

| Số thread trong thread group | Throttling interval |
| --- | --- |
| `0` đến `1 + thread_pool_oversubscribe` | `0` ms |
| `4` đến `7` | `50 * THROTTLING_FACTOR` |
| `8` đến `15` | `100 * THROTTLING_FACTOR` |
| `16` đến `65536` | `20 * THROTTLING_FACTOR` |

`THROTTLING_FACTOR` phụ thuộc vào `thread_pool_stall_limit`:

```text
THROTTLING_FACTOR = thread_pool_stall_limit / MAX(500, thread_pool_stall_limit)
```

Ý nghĩa:

- group còn ít thread thì MariaDB cho phép tạo thread nhanh hơn;
- group đã có nhiều thread thì việc tạo thêm bị kìm lại;
- `thread_pool_stall_limit` ảnh hưởng gián tiếp đến nhịp tạo thread.

### 3.15. Thread group stall

Thread group stall là tình huống một connection/query đang chạy quá lâu hoặc bị kẹt khiến các connection khác trong cùng group không được phục vụ.

Với Thread Pool, stall detection được thực hiện bởi timer thread. Biến chính là `thread_pool_stall_limit`, mặc định thường `500` millisecond.

Có thể hiểu `thread_pool_stall_limit` là ranh giới để Thread Pool đánh giá thế nào là "query đủ lâu để nghi ngờ đang làm nghẽn group".

Nếu giá trị thấp:

- phát hiện stall nhanh hơn;
- có thể giúp giảm nguy cơ deadlock/nghẽn group;
- nhưng dễ tạo/đánh thức thêm worker quá thường xuyên;
- có thể tăng parallelism ngoài ý muốn và tăng context switching.

Nếu giá trị cao:

- tránh tạo thêm worker quá sớm;
- phù hợp hơn nếu workload có nhiều query dài hợp lệ;
- nhưng query khác trong cùng group có thể phải chờ lâu hơn khi worker thật sự bị kẹt.

Tư duy chọn giá trị:

```text
OLTP query ngắn
-> stall_limit thấp hơn có thể hợp lý hơn nếu cần phát hiện kẹt nhanh

OLAP/query dài
-> stall_limit quá thấp dễ làm pool nghĩ nhầm query dài là stall
-> có thể tạo nhiều worker hơn và tăng cạnh tranh tài nguyên
```

### 3.16. Thread group oversubscription

Oversubscription xảy ra khi một thread group có nhiều active worker thread cùng lúc.

Bình thường Thread Pool cố giữ ít worker active để giảm overhead. Nhưng khi timer thread phát hiện stall, nó có thể:

- đánh thức sleeping worker;
- hoặc tạo worker mới.

Khi đó group có nhiều worker active, tức là group bị oversubscribed.

Biến liên quan là `thread_pool_oversubscribe`, default thường `3`.

Điểm dễ nhầm:

```text
thread_pool_oversubscribe không quyết định trực tiếp khi nào tạo worker mới.
Nó quyết định ngưỡng bao nhiêu worker có thể tiếp tục active trong một group sau khi group đã bị oversubscribed.
```

Nói cách khác, `thread_pool_oversubscribe` không phải nút "tạo thêm thread". Nó là giới hạn giúp Thread Pool biết khi nào nên bắt đầu giảm số worker active trong group sau khi trạng thái stall đã qua.

Nếu để quá thấp:

- group quay về ít worker nhanh hơn;
- giảm cạnh tranh CPU;
- nhưng có thể chưa đủ giúp group thoát nghẽn trong workload nhiều wait.

Nếu để quá cao:

- nhiều worker active cùng lúc hơn;
- giảm nguy cơ query dài độc chiếm group;
- nhưng tăng context switching và tranh CPU/lock.

Default `3` thường đủ cho đa số workload. Không nên chỉnh nếu chưa thấy bằng chứng từ queue, stall, latency hoặc context switch.

### 3.17. Cách đọc toàn bộ cơ chế bằng một luồng ví dụ

Ví dụ một thread group đang có nhiều connection:

```text
Thread Group 2
queue: Q1, Q2, Q3, Q4
worker active: W1 đang chạy Q1
listener: L1 đang phân phối event
```

Nếu `Q1` chạy nhanh:

```text
W1 chạy Q1 xong
-> W1 lấy tiếp Q2
-> group gần như xử lý tuần tự
-> ít overhead
```

Nếu `Q1` bị chờ row lock lâu:

```text
W1 bị wait
queue vẫn còn Q2, Q3, Q4
timer/listener thấy group không tiến triển
-> đánh thức W2 hoặc tạo W2
-> W2 xử lý Q2
-> group tạm thời có nhiều worker active
```

Khi lock được giải phóng và queue giảm:

```text
W1 hoàn tất
W2 hoàn tất
pool bắt đầu giảm worker active theo oversubscribe/throttling
group quay về trạng thái ít worker hơn
```

Đây là lý do Thread Pool vừa cố giữ số worker thấp, vừa có cơ chế thoát kẹt khi một worker bị wait.

### 3.18. Kết luận thực dụng

Thread group trong Unix Thread Pool nên được hiểu như một đơn vị điều phối connection:

```text
thread_pool_size
= số thread group
= số queue/scheduling lane nội bộ
```

Trong trạng thái bình thường, mỗi group có xu hướng được xử lý bởi ít worker active, thường gần một worker, để giảm overhead. Khi group bị kẹt, MariaDB có thể tạm thời thêm worker để tránh một query hoặc connection độc chiếm cả group.

Các biến cần nhớ:

| Biến | Vai trò |
| --- | --- |
| `thread_pool_size` | Số thread group. |
| `thread_pool_max_threads` | Trần số thread trong pool. |
| `thread_pool_stall_limit` | Chu kỳ/ranh giới phát hiện stall, default thường `500ms`. |
| `thread_pool_oversubscribe` | Ngưỡng oversubscription trong group, default thường `3`. |
| `thread_pool_dedicated_listener` | Quyết định listener có chuyên trách hay có thể kiêm worker trong một số tình huống. |

Tư duy cuối:

```text
thread_pool_size thấp
-> ít group, ít overhead, nhưng dễ queue

thread_pool_size cao
-> nhiều group, chia connection tốt hơn, nhưng overhead cao hơn

stall_limit thấp
-> phát hiện kẹt nhanh, nhưng dễ tạo thêm worker

stall_limit cao
-> ít tạo worker hơn, nhưng query khác có thể chờ lâu

oversubscribe cao
-> group thoát nghẽn tốt hơn, nhưng tăng cạnh tranh CPU
```

## 4. Biến hệ thống và biến trạng thái của Thread Pool

### 4.1. Phân biệt system variable và status variable

Khi đọc Thread Pool, cần phân biệt hai loại biến:

| Loại biến | Dùng để làm gì? | Ví dụ |
| --- | --- | --- |
| System variable | Điều khiển hành vi của Thread Pool. Đây là "núm chỉnh" của server. | `thread_handling`, `thread_pool_size`, `thread_pool_max_threads` |
| Status variable | Phản ánh trạng thái đang diễn ra. Đây là "đồng hồ đo" để quan sát. | `Threadpool_threads`, `Threadpool_idle_threads` |

System variable trả lời câu hỏi:

```text
Server được phép hoạt động như thế nào?
```

Status variable trả lời câu hỏi:

```text
Server hiện đang hoạt động ra sao?
```

Vì vậy, không nên chỉnh system variable nếu chưa nhìn status variable, CPU, RAM, lock wait, queue và latency. Thread Pool là cơ chế điều phối concurrency; chỉnh sai có thể làm hệ thống nhiều thread hơn nhưng không nhanh hơn.

### 4.2. Nhóm biến chọn mô hình xử lý thread

| Biến | Default / giá trị | Scope | Dynamic | Ý nghĩa |
| --- | --- | --- | --- | --- |
| `thread_handling` | Unix thường `one-thread-per-connection`; Windows thường `pool-of-threads` | Global | No | Chọn cách server xử lý thread cho client connection. |

Các giá trị hợp lệ thường gặp:

| Giá trị | Ý nghĩa |
| --- | --- |
| `one-thread-per-connection` | Mỗi client connection dùng một thread riêng. |
| `pool-of-threads` | Client connection dùng Thread Pool. |
| `no-threads` | Một thread xử lý tất cả connection, chủ yếu chỉ hữu ích cho debug. |

`thread_handling` là biến nền tảng. Nếu không dùng `pool-of-threads`, phần lớn biến Thread Pool khác sẽ không có ý nghĩa thực tế.

Điểm cần nhớ:

- trên Windows, default thường đã là `pool-of-threads`;
- trên Unix/non-Windows, default thường là `one-thread-per-connection`;
- trong MariaDB, Thread Pool là tính năng có sẵn; trong MySQL Community không nên mặc định nghĩ có cùng cơ chế.

### 4.3. Nhóm biến cổng phụ cho quản trị

| Biến | Default | Range | Scope | Dynamic | Ý nghĩa |
| --- | --- | --- | --- | --- | --- |
| `extra_port` | `0` | tùy port hợp lệ | Global | No | Port phụ dành cho kết nối TCP quản trị. Nếu là `0`, không dùng extra port. |
| `extra_max_connections` | `1` | `1` đến `100000` | Global | Yes | Số connection tối đa được phép vào qua `extra_port`. |

Extra port hữu ích khi Thread Pool chính bị nghẽn, ví dụ tất cả worker đang chờ lock hoặc main port không còn xử lý được connection quản trị.

Điểm quan trọng:

```text
extra_port không phải port phục vụ traffic application.
extra_port là đường vào khẩn cấp cho DBA/admin.
```

Vì extra port dùng cơ chế gần với `one-thread-per-connection`, nó có thể giúp admin vẫn vào được server ngay cả khi pool chính đang bị block.

### 4.4. Nhóm biến số lượng thread và thread group

| Biến | Default | Range / giới hạn | Scope | Dynamic | Ý nghĩa |
| --- | --- | --- | --- | --- | --- |
| `thread_pool_size` | Dựa trên số CPU/core logic | thường `1` đến `128` khi đổi runtime; khi startup có thể cao hơn nhiều | Global | Yes | Số thread group trong Unix Thread Pool. |
| `thread_pool_max_threads` | `65536` trên Unix; Windows thường `1000` | thường `1` đến `65536` trên Unix | Global | Yes | Trần số thread trong Thread Pool. |
| `thread_pool_min_threads` | `1` | tùy implementation | Global | thường dùng cho Windows | Số thread tối thiểu trong Windows Thread Pool. |

`thread_pool_size` quyết định số thread group, không phải số thread tối đa.

```text
thread_pool_size = số group
thread_pool_max_threads = trần tổng số thread
thread_pool_min_threads = sàn số thread, chủ yếu có ý nghĩa trên Windows
```

Với Unix:

- `thread_pool_size` ảnh hưởng lớn đến cách connection được chia vào group;
- default theo CPU là điểm bắt đầu hợp lý;
- không nên tăng quá cao nếu CPU đã bão hòa hoặc context switch cao;
- nếu CPU còn rảnh nhưng queue tích tụ, có thể cân nhắc tăng nhẹ.

Với Windows:

- MariaDB dùng native Windows Thread Pool;
- `thread_pool_min_threads` có thể hữu ích với workload bursty;
- nếu để min quá thấp, sau thời gian rảnh dài, spike tiếp theo có thể mất thời gian để pool tăng lại;
- nếu để min quá cao, server giữ nhiều thread hơn cần thiết.

### 4.5. Nhóm biến idle thread

| Biến | Default | Scope | Dynamic | Nền tảng | Ý nghĩa |
| --- | --- | --- | --- | --- | --- |
| `thread_pool_idle_timeout` | `60` giây | Global | Yes | Unix | Số giây một idle worker chờ trước khi thoát nếu không có việc. |
| `thread_pool_min_threads` | `1` | Global | tùy implementation | Windows | Số thread tối thiểu được giữ lại trong native Windows Thread Pool. |

`thread_pool_idle_timeout` trả lời câu hỏi:

```text
Nếu worker đang rảnh, nó nên chờ bao lâu trước khi tự thoát?
```

Nếu giá trị thấp:

- giảm số idle thread nhanh hơn;
- tiết kiệm tài nguyên hơn khi server rảnh;
- nhưng workload bursty có thể phải tạo thread lại khi spike đến.

Nếu giá trị cao:

- giữ thread rảnh lâu hơn;
- phản ứng tốt hơn với spike gần nhau;
- nhưng giữ memory/thread metadata lâu hơn.

Với workload OLTP đều, default `60` giây thường là điểm bắt đầu hợp lý. Với workload bursty, cần theo dõi latency khi spike đến trước khi giảm giá trị này.

### 4.6. Nhóm biến phát hiện stall

| Biến | Default | Range | Scope | Dynamic | Nền tảng | Ý nghĩa |
| --- | --- | --- | --- | --- | --- | --- |
| `thread_pool_stall_limit` | `500` ms | `1` đến `4294967295` | Global | Yes | Unix | Khoảng thời gian giữa các lần timer thread kiểm tra stall. |

`thread_pool_stall_limit` là biến quan trọng để xác định khi nào Thread Pool nghi ngờ một thread group đang bị kẹt.

Nó ảnh hưởng đến hai việc:

- timer thread kiểm tra stall thường xuyên ra sao;
- Thread Pool xem query thế nào là "đủ lâu để có thể đang độc chiếm group".

Nếu thấp:

- phát hiện kẹt nhanh;
- query khác trong group ít phải chờ lâu;
- nhưng dễ tạo/đánh thức worker nhiều hơn;
- có thể tăng context switch.

Nếu cao:

- ít can thiệp hơn;
- phù hợp hơn với workload query dài;
- nhưng query ngắn trong cùng group có thể chờ lâu nếu một worker bị kẹt.

Lưu ý khi so sánh với MySQL Enterprise Thread Pool: đơn vị và ý nghĩa có thể khác. Trong MariaDB, biến này dùng millisecond.

### 4.7. Nhóm biến oversubscription

| Biến | Default | Range | Scope | Dynamic | Nền tảng | Ý nghĩa |
| --- | --- | --- | --- | --- | --- | --- |
| `thread_pool_oversubscribe` | `3` | `1` đến `65536` | Global | Yes | Unix | Số worker thread có thể còn active trong một group sau khi group bị oversubscribed do stall. |

`thread_pool_oversubscribe` dễ bị hiểu nhầm là "số thread sẽ được tạo thêm". Không đúng.

Ý đúng:

```text
thread_pool_oversubscribe
= ngưỡng điều tiết số worker active trong một group đã bị oversubscribed
```

Nó không trực tiếp quyết định thời điểm tạo worker mới. Việc tạo worker mới phụ thuộc vào listener/timer/worker, stall, wait, sleeping worker và `thread_pool_max_threads`.

Nếu tăng quá cao:

- nhiều worker có thể active cùng lúc hơn trong một group;
- group thoát nghẽn tốt hơn trong workload nhiều wait;
- nhưng tăng tranh CPU, lock và context switching.

Nếu giảm quá thấp:

- group quay lại ít worker nhanh hơn;
- overhead thấp hơn;
- nhưng dễ làm queue chờ lâu nếu workload hay bị stall.

Default `3` thường đủ cho đa số workload, và biến này chủ yếu dành cho tinh chỉnh sâu.

### 4.8. Nhóm biến listener và thống kê queue

| Biến | Default | Scope | Dynamic | Nền tảng | Ý nghĩa |
| --- | --- | --- | --- | --- | --- |
| `thread_pool_dedicated_listener` | `0` | Global | tùy phiên bản | Unix | Nếu là `1`, mỗi group có listener riêng và listener không lấy work từ queue. |
| `thread_pool_exact_stats` | `0` | Global | tùy phiên bản | Unix | Nếu là `1`, dùng timestamp độ chính xác cao hơn để thống kê thời gian queue, đổi lại có chi phí nhỏ. |

`thread_pool_dedicated_listener` ảnh hưởng đến vai trò của listener thread.

Nếu `0`:

- listener có thể kiêm worker trong một số tình huống;
- giảm overhead khi workload nhẹ;
- nhưng số liệu queue có thể ít chính xác hơn.

Nếu `1`:

- listener chuyên trách lắng nghe/phân phối I/O event;
- queue trong Information Schema có thể phản ánh chính xác hơn;
- có thể tốn thêm thread/tài nguyên.

`thread_pool_exact_stats` liên quan đến độ chính xác thống kê queue. Nếu bật, MariaDB ghi nhận thời điểm connection vào queue bằng timestamp chính xác hơn. Điều này giúp tính queue time tốt hơn, nhưng có chi phí nhỏ.

Hai biến này thường chỉ có ý nghĩa khi cần phân tích sâu Thread Pool trên Unix.

### 4.9. Nhóm biến ưu tiên

| Biến | Default | Range / giá trị | Scope | Dynamic | Nền tảng | Ý nghĩa |
| --- | --- | --- | --- | --- | --- | --- |
| `thread_pool_priority` | `auto` | `high`, `low`, `auto` | Global, Connection | tùy phiên bản | MariaDB Thread Pool | Điều khiển priority của connection/query trong Thread Pool. |
| `thread_pool_prio_kickup_timer` | `1000` ms | `0` đến `4294967295` | Global | Yes | Unix | Sau bao lâu statement priority thấp được đẩy lên hàng đợi priority cao. |

`thread_pool_priority = auto` thường nghĩa là MariaDB tự quyết định priority dựa trên ngữ cảnh, đặc biệt là connection có đang ở trong transaction hay không.

Lý do transaction thường được ưu tiên:

```text
transaction giữ lock càng lâu
-> transaction khác càng dễ chờ
-> ưu tiên hoàn tất transaction
-> giảm lock wait tổng thể
```

Nhưng nếu low-priority query bị chờ mãi, hệ thống có thể bị starvation. `thread_pool_prio_kickup_timer` giúp giảm rủi ro này bằng cách đưa statement low-priority lên high-priority queue sau một khoảng thời gian.

Nếu giá trị quá thấp:

- low-priority query được nâng nhanh hơn;
- giảm starvation;
- nhưng làm yếu hiệu quả phân biệt priority.

Nếu giá trị quá cao:

- giữ ưu tiên cho high-priority tốt hơn;
- nhưng low-priority có thể chờ lâu.

### 4.10. Bảng tổng hợp system variables

| Biến | Default thường gặp | Dynamic | Nền tảng chính | Nhóm ý nghĩa |
| --- | --- | --- | --- | --- |
| `thread_handling` | Unix: `one-thread-per-connection`; Windows: `pool-of-threads` | No | Tất cả | Chọn mô hình thread. |
| `thread_pool_size` | Số CPU/core logic | Yes | Unix | Số thread group. |
| `thread_pool_max_threads` | Unix: `65536`; Windows: `1000` | Yes | Tất cả | Trần số thread. |
| `thread_pool_min_threads` | `1` | tùy implementation | Windows | Sàn số thread. |
| `thread_pool_idle_timeout` | `60` giây | Yes | Unix | Thời gian giữ idle worker. |
| `thread_pool_stall_limit` | `500` ms | Yes | Unix | Phát hiện stall. |
| `thread_pool_oversubscribe` | `3` | Yes | Unix | Điều tiết oversubscription. |
| `thread_pool_dedicated_listener` | `0` | tùy phiên bản | Unix | Listener chuyên trách. |
| `thread_pool_exact_stats` | `0` | tùy phiên bản | Unix | Thống kê queue chính xác hơn. |
| `thread_pool_priority` | `auto` | theo connection/global | Tất cả | Ưu tiên query/connection. |
| `thread_pool_prio_kickup_timer` | `1000` ms | Yes | Unix | Chống starvation cho low priority. |
| `extra_port` | `0` | No | Tất cả | Cổng phụ quản trị. |
| `extra_max_connections` | `1` | Yes | Tất cả | Số connection trên cổng phụ. |

### 4.11. Status variables của Thread Pool

Status variables chính:

| Biến | Scope | Kiểu dữ liệu | Ý nghĩa |
| --- | --- | --- | --- |
| `Threadpool_threads` | Global, Session | numeric | Số thread hiện có trong Thread Pool. |
| `Threadpool_idle_threads` | Global, Session | numeric | Số thread inactive/idle trong Thread Pool. Có ý nghĩa rõ hơn trên Unix. |

`Threadpool_threads` cho biết pool hiện có bao nhiêu thread. Trong một số trường hợp hiếm, giá trị này có thể cao hơn `thread_pool_max_threads` một chút, vì mỗi thread group cần ít nhất worker/listener để tránh deadlock.

`Threadpool_idle_threads` không chỉ có nghĩa là thread "chưa được giao việc". Một thread cũng có thể được xem là inactive nếu đang bị block khi chờ disk I/O, lock hoặc wait khác.

Cách đọc:

```text
Threadpool_threads cao
-> pool đang giữ nhiều thread
-> có thể do concurrency cao, lock wait, I/O wait hoặc stall

Threadpool_idle_threads cao
-> nhiều thread không làm work hữu ích tại thời điểm đó
-> có thể bình thường nếu vừa qua spike
-> cũng có thể là dấu hiệu giữ thread quá lâu hoặc nhiều wait
```

### 4.12. Cách đọc system variable cùng status variable

Không nên nhìn từng biến riêng lẻ. Nên đọc theo cặp.

| Nếu thấy | Kết hợp với | Cách nghĩ |
| --- | --- | --- |
| `Threadpool_threads` gần `thread_pool_max_threads` | lock wait/I/O wait cao | Pool có thể đang bị kẹt vì worker bị block, không phải thiếu thread đơn thuần. |
| `Threadpool_idle_threads` cao lâu dài | CPU thấp, queue thấp | Có thể pool đang giữ nhiều thread hơn cần thiết. |
| Queue tích tụ nhưng CPU thấp | `thread_pool_size`, stall, listener | Có thể quá ít group hoặc group bị stall. |
| CPU cao, context switch cao | `thread_pool_size`, `thread_pool_oversubscribe` | Tăng thread thường không giúp; cần giảm concurrency hoặc tối ưu query. |
| Transaction chờ lock nhiều | `thread_pool_priority`, query/transaction design | Ưu tiên transaction có thể giúp, nhưng gốc vẫn là lock và transaction dài. |
| Admin khó kết nối khi nghẽn | `extra_port`, `extra_max_connections` | Nên có đường quản trị phụ cho production. |

Ví dụ phân tích:

```text
Threadpool_threads tăng rất cao
CPU không cao
nhiều session chờ lock
-> vấn đề chính có thể là lock wait, không phải thiếu CPU

Threadpool_threads thấp
CPU thấp
queue trong THREAD_POOL_QUEUES dài
-> có thể thread_pool_size thấp hoặc thread group bị stall

CPU 95%
context switch cao
queue vẫn dài
-> tăng thread_pool_size có thể làm tệ hơn
-> cần giảm concurrency hoặc tối ưu query
```

### 4.13. Biến nào nên chỉnh trước?

Thứ tự tư duy an toàn:

1. Xác định workload là OLTP, OLAP, mixed hay bursty.
2. Kiểm tra số connection thật đi vào database.
3. Kiểm tra CPU, RAM, I/O, lock wait và latency.
4. Xem `Threadpool_threads` và `Threadpool_idle_threads`.
5. Nếu dùng Unix, xem thêm các bảng Information Schema của Thread Pool.
6. Chỉ sau đó mới cân nhắc chỉnh `thread_pool_size`, `thread_pool_stall_limit`, `thread_pool_oversubscribe`.

Các biến thường có tác động rõ:

| Biến | Khi nào nghĩ tới |
| --- | --- |
| `thread_pool_size` | Queue dài, CPU còn rảnh, nhiều connection OLTP. |
| `thread_pool_max_threads` | Pool chạm trần thread trong khi nhiều worker bị wait. |
| `thread_pool_stall_limit` | Query trong group bị chờ lâu hoặc group hay stall. |
| `thread_pool_oversubscribe` | Group bị oversubscribed thường xuyên và cần điều tiết số worker active. |
| `thread_pool_idle_timeout` | Workload bursty, idle thread retire quá nhanh hoặc giữ quá lâu. |
| `thread_pool_priority` | Transaction cần hoàn tất nhanh để giảm lock wait. |
| `extra_port` | Cần đường quản trị khi pool/main port bị nghẽn. |

### 4.14. Kết luận thực dụng

Các system variable của Thread Pool là công cụ điều khiển concurrency. Các status variable là công cụ quan sát trạng thái.

Tư duy cuối:

```text
System variable
= cấu hình hành vi

Status variable
= đo điều đang xảy ra

Không chỉnh system variable nếu chưa hiểu status variable.
```

Với Thread Pool, tăng số thread không đồng nghĩa tăng hiệu năng. Nếu bottleneck là lock, I/O, query plan hoặc CPU đã bão hòa, tăng thread chỉ làm nhiều work cùng chờ hơn. Tinh chỉnh đúng là tìm điểm cân bằng giữa:

- đủ worker để không bỏ phí CPU;
- đủ ít worker để tránh context switching;
- đủ stall detection để tránh group bị độc chiếm;
- đủ giới hạn để database không tự biến thành hàng nghìn thread tranh nhau chạy.

## 5. Lựa chọn công bằng giữa Range và Index Merge

### 5.1. `index_merge` là gì?

`index_merge` là một chiến lược optimizer dùng để đọc dữ liệu từ **một bảng** bằng nhiều index scan riêng biệt, sau đó merge kết quả của các scan đó lại.

Ví dụ concept:

```text
WHERE Origin = 'SEA' OR Dest = 'SEA'

Index Origin -> tìm các row có Origin = 'SEA'
Index Dest   -> tìm các row có Dest = 'SEA'
Merge        -> hợp nhất hai tập row lại
```

Khi optimizer chọn chiến lược này, `EXPLAIN` thường hiển thị:

| Cột trong EXPLAIN | Giá trị thường thấy | Ý nghĩa |
| --- | --- | --- |
| `type` | `index_merge` | Optimizer dùng nhiều index trên cùng một bảng rồi merge kết quả. |
| `possible_keys` | nhiều index | Các index có thể dùng. |
| `key` | nhiều index, ví dụ `Origin,Dest` | Các index thực sự được dùng trong plan. |
| `Extra` | `Using union(...)`, `Using intersect(...)`, `Using sort_union(...)` | Cách merge kết quả từ các index scan. |

`index_merge` thường xuất hiện trong các query có điều kiện `OR` trên nhiều cột đã có index:

```text
Origin = 'SEA' OR Dest = 'SEA'
```

Nếu chỉ dùng một index đơn lẻ, database có thể bỏ lỡ một nửa điều kiện. Nếu dùng full table scan, database phải đọc nhiều row hơn. `index_merge` cho phép dùng cả hai index rồi hợp nhất row id.

### 5.2. Range scan là gì?

Range scan là chiến lược dùng **một index** để đọc một khoảng giá trị hoặc một nhóm giá trị phù hợp.

Ví dụ concept:

```text
SecurityDelay = 0
DepDelay < 720
Origin = 'SEA'
```

Nếu optimizer chọn một index duy nhất, `EXPLAIN` có thể hiển thị:

| `type` | Ý nghĩa |
| --- | --- |
| `ref` | Dùng index để tìm các row khớp với một giá trị cụ thể. |
| `range` | Dùng index để đọc một khoảng giá trị. |
| `ALL` | Full table scan, đọc toàn bộ bảng. |

Vấn đề là khi query có nhiều điều kiện, optimizer phải quyết định:

```text
Dùng một index tốt nhất rồi filter phần còn lại?
Hay dùng index_merge để kết hợp nhiều index?
```

Đây chính là bài toán "fair choice" giữa range và index merge.

### 5.3. Vấn đề của lựa chọn không công bằng

Một optimizer luôn phải giới hạn số plan cần xét. Nếu xét mọi tổ hợp index/range/index_merge có thể có, số lượng plan có thể tăng rất nhanh. Để tránh nổ tổ hợp, optimizer đôi khi phải loại bớt một số hướng tìm kiếm.

Vấn đề của logic cũ trong MySQL là: khi query có thêm điều kiện `AND`, optimizer có thể loại bỏ `index_merge` quá sớm, dù `index_merge` vẫn là plan tốt.

Ví dụ bảng `ontime` có khoảng:

```text
1,578,171 row
```

Query cơ bản:

```text
Origin = 'SEA' OR Dest = 'SEA'
```

Cả MySQL và MariaDB đều có thể chọn:

```text
type = index_merge
key  = Origin,Dest
rows ~= 92,800
```

Đây là plan hợp lý: dùng index `Origin`, dùng index `Dest`, rồi union kết quả.

Nhưng khi thêm điều kiện `AND`, ví dụ:

```text
(Origin = 'SEA' OR Dest = 'SEA') AND SecurityDelay = 0
```

hoặc:

```text
(Origin = 'SEA' OR Dest = 'SEA') AND DepDelay < 12 * 60
```

logic cũ có thể bỏ `index_merge` và chọn plan kém hơn.

### 5.4. Ví dụ so sánh MySQL và MariaDB

Với query:

```text
Origin = 'SEA' OR Dest = 'SEA'
```

plan tốt là:

| Database | `type` | `key` | `rows` | Ý nghĩa |
| --- | --- | --- | --- | --- |
| MySQL | `index_merge` | `Origin,Dest` | khoảng `92,800` | Dùng union hai index. |
| MariaDB | `index_merge` | `Origin,Dest` | khoảng `92,800` | Dùng union hai index. |

Khi thêm:

```text
AND SecurityDelay = 0
```

MySQL cũ có thể chọn:

| Database | `type` | `key` | `rows` | Nhận xét |
| --- | --- | --- | --- | --- |
| MySQL | `ref` | `SecurityDelay` | khoảng `791,546` | Chọn một index khác rồi filter điều kiện `Origin/Dest`. |
| MariaDB | `index_merge` | `Origin,Dest` | khoảng `92,800` | Vẫn giữ plan index merge tốt. |

Khi thêm:

```text
AND DepDelay < 12 * 60
```

MySQL cũ có thể chọn:

| Database | `type` | `key` | `rows` | Nhận xét |
| --- | --- | --- | --- | --- |
| MySQL | `ALL` | `NULL` | khoảng `1,583,093` | Gần như full table scan. |
| MariaDB | `index_merge` | `Origin,Dest` | khoảng `92,800` | Vẫn giữ plan index merge tốt. |

Nhìn vào cột `rows`, có thể thấy khác biệt rất lớn:

```text
index_merge Origin,Dest
-> khoảng 92,800 row

SecurityDelay ref plan
-> khoảng 791,546 row
-> kém hơn khoảng 8.5 lần theo estimate rows

full table scan
-> khoảng 1.58 triệu row
-> kém hơn khoảng 17 lần theo estimate rows
```

`rows` không phải thời gian chạy thực tế, nhưng là tín hiệu quan trọng để so sánh plan. Nếu mọi yếu tố khác tương đối giống nhau, plan phải đọc ít row hơn thường có tiềm năng tốt hơn.

### 5.5. MySQL hiện đại có còn tệ như ví dụ này không?

Không nên kết luận rằng MySQL hiện đại luôn tệ như ví dụ trên. Ví dụ này chủ yếu minh họa một vấn đề optimizer từng gặp: `index_merge` có thể bị loại quá sớm khi query có thêm điều kiện `AND`.

Cách hiểu đúng:

```text
Ví dụ cũ:
MySQL chọn plan kém hơn vì loại index_merge quá sớm.

Không có nghĩa:
Mọi phiên bản MySQL hiện đại đều luôn chọn sai kiểu này.
```

MySQL 8.x/8.4 vẫn có `index_merge` và vẫn có các biến thể như:

- `index_merge union`;
- `index_merge intersection`;
- `index_merge sort_union`.

Optimizer hiện đại chọn giữa `index_merge`, range/ref scan, full scan và các access path khác dựa trên cost estimate. Vì vậy một query cụ thể có còn bị chọn plan kém hay không phụ thuộc vào:

- phiên bản MySQL;
- storage engine;
- statistics;
- histogram nếu có;
- phân bố dữ liệu thật;
- độ selective của từng điều kiện;
- số row ước lượng;
- index hiện có;
- query có `SELECT *` hay chỉ select vài cột;
- cost model của phiên bản đó.

Nói cách khác:

```text
Không thể nhìn ví dụ lịch sử rồi kết luận MySQL hiện tại chắc chắn vẫn tệ.
Phải kiểm tra bằng dữ liệu thật và plan thật.
```

Cách kiểm chứng đúng:

1. So sánh `EXPLAIN` giữa MySQL và MariaDB trên cùng schema/index/data.
2. Xem `type`, `key`, `rows`, `filtered`, `Extra`.
3. Nếu có thể, dùng `EXPLAIN ANALYZE` để xem row thực tế và thời gian thực thi.
4. Kiểm tra statistics/histogram có đầy đủ không.
5. Thử rewrite query nếu `AND/OR` lồng nhau phức tạp.
6. So sánh với phương án composite index nếu query là workload quan trọng.

Kết luận thực dụng:

```text
MySQL hiện đại có thể đã xử lý tốt hơn case cũ.
Nhưng index_merge vẫn là vùng optimizer có thể chọn sai nếu estimate sai hoặc WHERE quá phức tạp.
MariaDB 5.3+ nhấn mạnh việc không loại index_merge candidate quá sớm.
```

### 5.6. MariaDB 5.3+ thay đổi gì?

Từ MariaDB 5.3, optimizer trì hoãn việc loại bỏ các plan `index_merge` tiềm năng cho đến khi thật sự cần thiết.

Nói đơn giản:

```text
MySQL cũ:
thêm AND predicate
-> có thể loại index_merge quá sớm
-> chọn range/ref/full scan kém hơn

MariaDB 5.3+:
giữ index_merge candidate lâu hơn
-> so sánh công bằng hơn với range/ref plan
-> chỉ loại khi thật sự cần
```

Đây là ý nghĩa của "fair choice":

```text
Không ưu ái index_merge một cách mù quáng.
Cũng không loại index_merge quá sớm.
Cho index_merge và range/ref plan cùng cơ hội được cost đúng hơn.
```

Kết quả là các query có dạng:

```text
(điều kiện OR dùng nhiều index) AND (điều kiện bổ sung)
```

có cơ hội giữ plan `index_merge` tốt hơn thay vì bị đẩy sang một index đơn lẻ hoặc full table scan.

### 5.7. Vì sao thêm `AND` lại làm optimizer dễ chọn sai?

Query có `OR` và nhiều index đã đủ phức tạp. Khi thêm `AND`, số khả năng tăng lên:

```text
Origin = 'SEA'
Dest = 'SEA'
SecurityDelay = 0
DepDelay < 720
```

Optimizer có thể phải cân nhắc:

- dùng index `Origin`;
- dùng index `Dest`;
- dùng index `SecurityDelay`;
- dùng index `DepDelay`;
- merge `Origin` và `Dest`;
- merge rồi filter `SecurityDelay`;
- merge rồi filter `DepDelay`;
- dùng một index rồi filter các điều kiện còn lại;
- full table scan.

Nếu optimizer xét mọi tổ hợp, chi phí optimize query có thể tăng mạnh. Vì vậy có logic cắt bớt search space.

Vấn đề không nằm ở việc cắt bớt. Optimizer bắt buộc phải cắt bớt. Vấn đề là **cắt quá sớm** có thể làm mất plan tốt.

MariaDB cải thiện bằng cách không loại `index_merge` ngay khi xuất hiện thêm `AND predicate`, mà giữ nó đủ lâu để so sánh cost với các plan khác.

### 5.8. Khi nào `index_merge` có lợi?

`index_merge` thường đáng cân nhắc khi:

- query có `OR` giữa nhiều cột khác nhau;
- mỗi cột trong điều kiện `OR` có index riêng;
- từng index scan trả về tập row không quá lớn;
- union/intersection của các index scan nhỏ hơn nhiều so với full table scan;
- không có composite index phù hợp hơn;
- điều kiện bổ sung `AND` không làm một index đơn lẻ trở nên vượt trội.

Ví dụ concept:

```text
WHERE Origin = 'SEA' OR Dest = 'SEA'
```

Nếu `Origin` và `Dest` đều có index, `index_merge union` có thể tốt.

Nhưng nếu có composite index tốt hơn, ví dụ phù hợp đúng thứ tự điều kiện và workload, composite index có thể thắng `index_merge`.

### 5.9. Khi nào `index_merge` có thể không tốt?

`index_merge` không phải lúc nào cũng là plan tốt nhất.

Nó có thể kém nếu:

- mỗi index scan trả về quá nhiều row;
- merge row id tốn chi phí lớn;
- sau khi merge vẫn phải đọc rất nhiều row từ bảng;
- query `SELECT *` khiến phải lookup nhiều row thực tế, không chỉ đọc index;
- dữ liệu phân bố lệch nhưng statistics không phản ánh đúng;
- composite index phù hợp hơn nhưng chưa có;
- điều kiện `AND` có một index rất selective, tốt hơn nhiều so với merge.

Ví dụ:

```text
WHERE low_cardinality_col = 0 OR another_low_cardinality_col = 0
```

Nếu mỗi điều kiện khớp phần lớn bảng, merge hai index có thể còn tốn hơn full scan.

### 5.10. Cách đọc `EXPLAIN` cho bài toán này

Khi xem `EXPLAIN`, nên nhìn các cột:

| Cột | Cách đọc |
| --- | --- |
| `type` | Nếu là `index_merge`, optimizer đang merge nhiều index. Nếu là `ALL`, đang full scan. |
| `possible_keys` | Các index có thể dùng. |
| `key` | Index thực sự được chọn. Với `index_merge`, có thể thấy nhiều index. |
| `rows` | Số row ước lượng cần đọc. Dùng để so sánh plan, không phải số row chính xác. |
| `Extra` | `Using union(...)`, `Using intersect(...)`, `Using where`, v.v. |

Trong ví dụ `ontime`, điểm quan trọng là:

```text
MariaDB giữ index_merge Origin,Dest
-> rows khoảng 92,800

MySQL cũ có thể chọn SecurityDelay hoặc full scan
-> rows từ khoảng 791,546 đến 1.58 triệu
```

Tức là MariaDB không đơn giản "luôn chọn index_merge", mà cho `index_merge` cơ hội cạnh tranh công bằng hơn với range/ref/full scan.

### 5.11. Liên hệ với thiết kế index

Tối ưu `index_merge` không thay thế thiết kế index tốt.

Nếu query quan trọng và chạy thường xuyên, cần cân nhắc:

- có cần composite index không;
- thứ tự cột trong composite index có phù hợp không;
- query có `SELECT *` làm tăng chi phí đọc bảng không;
- có thể chỉ select cột cần thiết để tận dụng covering index không;
- statistics có đủ chính xác không;
- phân bố dữ liệu có bị lệch không;
- điều kiện `OR` có thể rewrite thành `UNION` rõ ràng hơn không.

`index_merge` hữu ích khi nhiều index đơn có thể phối hợp tốt. Nhưng với workload ổn định, composite index đúng thường vẫn là hướng tối ưu mạnh hơn.

### 5.12. Kết luận thực dụng

Tư duy cuối:

```text
index_merge
= dùng nhiều index trên cùng một bảng rồi merge kết quả

range/ref
= dùng một index chính rồi filter phần còn lại

fair choice
= không loại index_merge quá sớm khi có thêm AND predicate
```

MariaDB 5.3+ cải thiện optimizer bằng cách trì hoãn việc loại bỏ `index_merge` candidate. Điều này giúp các query có `OR` trên nhiều cột indexed, cộng thêm điều kiện `AND`, vẫn có cơ hội dùng plan tốt.

Nhưng `index_merge` không phải thuốc chữa mọi query. Khi đọc plan, luôn so sánh:

- `type`;
- `key`;
- `rows`;
- `Extra`;
- độ selective thật của điều kiện;
- khả năng tạo composite index tốt hơn.

## 6. Multi Range Read Optimization

### 6.1. Multi Range Read là gì?

**Multi Range Read** hay **MRR** là một tối ưu nhằm giảm chi phí I/O cho các query cần đọc nhiều row từ bảng thông qua index.

Vấn đề MRR giải quyết:

```text
index scan tìm được nhiều row id
-> các row thật nằm rải rác trong bảng
-> database phải đọc table rows theo thứ tự ngẫu nhiên
-> nhiều random I/O
-> chậm nếu dữ liệu không nằm sẵn trong buffer pool/OS cache
```

MRR thay đổi cách đọc:

```text
1. Đọc index trước để lấy danh sách row id / lookup key.
2. Buffer danh sách đó lại.
3. Sắp xếp theo thứ tự thuận lợi hơn cho storage engine.
4. Đọc table rows theo thứ tự có tính tuần tự hơn.
```

Mục tiêu là biến nhiều lần đọc ngẫu nhiên thành ít lần đọc tuần tự hơn.

MRR thường liên quan đến:

- `range` access;
- `ref` và `eq_ref` access khi dùng Batched Key Access;
- join dùng index lookup ở bảng phía trong;
- query I/O-bound cần đọc nhiều row.

### 6.2. Vì sao đọc theo thứ tự rowid lại nhanh hơn?

Khi query dùng secondary index, index thường không chứa toàn bộ row. Index chỉ giúp tìm ra row id hoặc primary key để đọc row thật từ bảng.

Không có MRR:

```text
Index scan trả row id theo thứ tự index
-> row thật có thể nằm rải rác trong table pages
-> đọc page 10
-> đọc page 500
-> đọc page 21
-> đọc page 900
-> random I/O nhiều
```

Có MRR:

```text
Index scan thu row id vào buffer
-> sort row id theo thứ tự gần với vị trí vật lý
-> đọc page 10
-> đọc page 21
-> đọc page 500
-> đọc page 900
-> ít seek hơn, dễ prefetch hơn
```

Với HDD, khác biệt có thể rất lớn vì seek cơ học đắt. Với SSD, không có seek cơ học như HDD, nhưng random read vẫn có overhead: nhiều I/O nhỏ, ít tận dụng prefetch, nhiều page lookup hơn. Vì vậy MRR vẫn có thể có lợi, nhưng mức lợi thường không cực đoan như HDD.

Điểm quan trọng:

```text
MRR không làm giảm số row logic cần đọc.
MRR cố làm thứ tự đọc vật lý hiệu quả hơn.
```

### 6.3. Case 1: Rowid sorting cho range access

Với query range:

```text
WHERE key1 BETWEEN 1000 AND 2000
```

Database dùng index `key1` để tìm các entry trong khoảng. Nhưng row thật trong bảng có thể không nằm liên tục theo thứ tự của `key1`.

Không có MRR:

```text
đọc index key1 theo range
-> gặp row id nào thì đọc row thật ngay
-> table row bị hit ngẫu nhiên
```

Có MRR:

```text
đọc index key1 theo range
-> gom row id vào rowid buffer
-> sort row id
-> đọc table rows theo thứ tự rowid
```

Trong `EXPLAIN`, MariaDB có thể thể hiện dạng:

```text
Rowid-ordered scan
```

Ý nghĩa:

```text
database không đọc row thật ngay khi thấy index entry
database trì hoãn, gom row id, sort, rồi đọc row theo thứ tự tốt hơn
```

MRR cho range access hữu ích khi:

- range trả nhiều row;
- table lớn;
- row thật phân tán;
- buffer pool không chứa sẵn phần lớn dữ liệu;
- storage bị I/O-bound.

MRR có thể không lợi khi:

- range rất nhỏ;
- table nhỏ và nằm hoàn toàn trong cache;
- CPU overhead sort/buffer lớn hơn lợi ích I/O;
- query có `ORDER BY ... LIMIT n` nhỏ cần vài row đầu theo thứ tự index, trong khi MRR lại đọc theo thứ tự rowid.

### 6.4. Case 2: Rowid sorting cho Batched Key Access

**Batched Key Access** hay **BKA** là tối ưu cho join dạng nested loop có index lookup ở bảng phía trong.

Không có BKA/MRR:

```text
đọc một row từ t1
-> lookup t2 bằng t2.key1 = t1.col1
-> đọc row t2

đọc row tiếp theo từ t1
-> lookup t2 tiếp
-> đọc row t2 khác
```

Nếu nhiều row từ `t1` lookup vào `t2`, các row của `t2` có thể bị đọc rất ngẫu nhiên.

Có BKA + MRR:

```text
gom nhiều lookup key từ t1
-> gửi theo batch sang lookup t2
-> MRR sắp xếp rowid của t2
-> đọc t2 theo thứ tự tốt hơn
```

Lợi ích:

- giảm random I/O khi đọc bảng phía trong của join;
- tránh lookup trùng lặp nếu nhiều row phía ngoài có cùng key;
- tận dụng buffer/prefetch tốt hơn;
- biến nhiều index lookup nhỏ thành batch lookup có tổ chức hơn.

Điểm cần nhớ:

```text
BKA gom lookup key theo batch.
MRR sắp xếp việc đọc row thật để giảm random I/O.
Hai tối ưu này thường đi cùng nhau trong join.
```

### 6.5. Case 3: Key sorting cho Batched Key Access

Ngoài sắp xếp rowid để đọc row thật, MariaDB còn có thể sắp xếp lookup key để truy cập index hiệu quả hơn.

Không có key sorting:

```text
t1 tạo lookup key theo thứ tự row của t1
-> lookup t2.key1 nhảy lung tung trong index
-> có thể đọc lại cùng index page nhiều lần
```

Có key sorting:

```text
gom lookup keys
-> sort lookup keys theo thứ tự index
-> lookup t2.key1 theo một sweep có trật tự hơn
```

Trong `EXPLAIN`, MariaDB có thể thể hiện:

```text
Key-ordered scan
```

Nếu có cả key sorting và rowid sorting, `EXPLAIN` có thể thể hiện:

```text
Key-ordered Rowid-ordered scan
```

Ý nghĩa:

```text
Key-ordered
= đọc index theo thứ tự key tốt hơn

Rowid-ordered
= đọc table rows theo thứ tự rowid tốt hơn
```

### 6.6. MRR và `ORDER BY ... LIMIT`

MRR có một trade-off quan trọng: nó tối ưu thứ tự đọc vật lý, không nhất thiết giữ thứ tự index mà query đang cần.

Với query:

```text
WHERE key1 BETWEEN ...
ORDER BY key1
LIMIT 10
```

Nếu database đọc theo thứ tự index, nó có thể lấy 10 row đầu khá nhanh.

Nếu MRR gom nhiều row id rồi sort theo rowid, nó có thể phải làm thêm buffer/sort trước khi trả row, trong khi query chỉ cần vài row đầu theo thứ tự index.

Vì vậy:

```text
MRR tốt cho đọc nhiều row I/O-bound.
MRR có thể không tốt cho query nhỏ, LIMIT nhỏ, cần trả nhanh theo thứ tự index.
```

### 6.7. Buffer space management

MRR cần buffer để gom rowid hoặc lookup key trước khi sort và đọc theo thứ tự tốt hơn.

Nếu buffer đủ lớn:

```text
gom đủ batch
-> sort một lần
-> đọc theo một hoặc ít sweep
-> lợi ích cao hơn
```

Nếu buffer quá nhỏ:

```text
gom được ít rowid/key
-> phải refill nhiều lần
-> nhiều pass
-> lợi ích giảm
```

Nhưng buffer quá lớn cũng có giá:

- tốn memory;
- nhiều query đồng thời có thể nhân memory usage lên;
- có thể lấy RAM khỏi buffer pool hoặc workload khác;
- sort batch lớn hơn cũng tốn CPU.

Các biến liên quan:

| Biến | Dùng cho | Ý nghĩa |
| --- | --- | --- |
| `mrr_buffer_size` | MRR cho range access | Giới hạn buffer MRR cho mỗi table/range scan. |
| `join_buffer_size` | BKA/join | Giới hạn bộ nhớ join buffer cho mỗi join/table tùy plan. |
| `join_buffer_space_limit` | BKA/join | Giới hạn tổng bộ nhớ join buffer dùng trong một join. |

Điểm cần nhớ:

```text
range access dùng mrr_buffer_size.
BKA dùng vùng buffer do join buffer quản lý.
```

### 6.8. Status variables của MRR

Các status variables liên quan:

| Biến | Ý nghĩa |
| --- | --- |
| `Handler_mrr_init` | Số lần MRR scan được khởi tạo/thực hiện. |
| `Handler_mrr_key_refills` | Số lần key buffer phải refill, không tính lần fill đầu. |
| `Handler_mrr_rowid_refills` | Số lần rowid buffer phải refill, không tính lần fill đầu. |

Cách đọc:

```text
Handler_mrr_init tăng
-> workload đang có MRR scans

Handler_mrr_key_refills tăng nhiều
-> key buffer không đủ chứa batch lookup key

Handler_mrr_rowid_refills tăng nhiều
-> rowid buffer không đủ chứa batch rowid
```

Nếu các refill counter tăng nhiều, MRR vẫn hoạt động nhưng phải chia thành nhiều pass. Điều này thường làm giảm lợi ích vì database không gom/sort được đủ lớn để đọc theo một sweep hiệu quả.

Khi đó cần xem:

- query có thật sự cần MRR không;
- buffer có quá nhỏ không;
- workload concurrent có cho phép tăng buffer không;
- `mrr_buffer_size`, `join_buffer_size`, `join_buffer_space_limit` có hợp lý không.

### 6.9. Vì sao MRR có thể làm status counter cao hơn?

MRR có thể làm một số status counter nhìn như tăng nhiều hơn, nhưng không nhất thiết là query tệ hơn.

Với scan bình thường không index-only:

```text
1. Đọc index entry để lấy rowid.
2. Đọc table row thật.

Hai bước này có thể được tính như một operation logic gần nhau.
```

Với MRR:

```text
1. Đọc index entry để gom rowid/key.
2. Sau đó đọc table row thật theo thứ tự đã sắp xếp.
```

Vì MRR tách quá trình thành nhiều call rõ ràng hơn với storage engine, một số counter như `Handler_read_XXX` hoặc `Innodb_rows_read` có thể tăng theo cách khiến người đọc tưởng là database làm nhiều việc hơn.

Điểm quan trọng:

```text
MRR không nhất thiết đọc nhiều dữ liệu logic hơn.
MRR thay đổi thứ tự và cách gọi storage engine để đọc hiệu quả hơn về I/O.
```

Vì vậy không nên chỉ nhìn một counter tăng rồi kết luận MRR xấu. Cần nhìn cùng:

- thời gian thực thi;
- số random I/O;
- buffer pool hit ratio;
- refill counters;
- `EXPLAIN`;
- latency thực tế.

### 6.10. Các optimizer switch liên quan

Các flag thường gặp:

| Flag | Ý nghĩa |
| --- | --- |
| `mrr` | Bật/tắt MRR và rowid-ordered scan. |
| `mrr_sort_keys` | Cho phép key-ordered scan, thường cần `mrr` có hiệu lực. |
| `mrr_cost_based` | Cho phép optimizer quyết định có dùng MRR dựa trên cost. |

Theo tài liệu MariaDB, MRR có thể làm chậm query nhỏ trên bảng nhỏ, nên không phải lúc nào cũng nên dùng mù quáng. `mrr_cost_based` tồn tại để optimizer tự quyết định, nhưng trong một số tài liệu MariaDB cũ, cost model cho MRR chưa được xem là đủ tốt để khuyến nghị phụ thuộc hoàn toàn.

Tư duy đúng:

```text
MRR tốt khi query I/O-bound và đọc nhiều row.
MRR không chắc tốt khi query nhỏ, dữ liệu đã nằm trong cache, hoặc LIMIT nhỏ.
```

### 6.11. Khác biệt với MySQL

MariaDB và MySQL cùng có khái niệm Multi Range Read, nhưng cách thể hiện và một số biến/counter khác nhau.

| Điểm | MariaDB | MySQL |
| --- | --- | --- |
| Cách `EXPLAIN` thể hiện | Có thể thấy `Rowid-ordered scan`, `Key-ordered scan`, `Key-ordered Rowid-ordered scan` | Thường thể hiện `Using MRR`. |
| Rowid-ordered scan | Có | Có concept tương đương. |
| Key-ordered scan | Có trong MariaDB với `mrr_sort_keys` | MySQL thường không thể hiện cùng cách. |
| Buffer cho range MRR | `mrr_buffer_size` | MySQL dùng `read_rnd_buffer_size`. |
| Status counters | MariaDB có nhiều counter MRR hơn như `Handler_mrr_init`, `Handler_mrr_key_refills`, `Handler_mrr_rowid_refills` | MySQL có ít counter MRR hơn. |

Điểm thực dụng:

```text
Trong MariaDB, EXPLAIN có thể nói rõ hơn MRR đang rowid-ordered, key-ordered, hay cả hai.
Trong MySQL, thường chỉ thấy Using MRR.
```

### 6.12. MRR factsheet

Tóm tắt nhanh:

| Ý | Nội dung |
| --- | --- |
| Mục tiêu | Giảm random I/O khi query cần đọc nhiều row qua index. |
| Dùng với | `range` access; `ref`/`eq_ref` access khi dùng BKA. |
| Chiến lược chính | Rowid-ordered scan và Key-ordered scan. |
| Có lợi khi | Query đọc nhiều row, table lớn, I/O-bound, row nằm rải rác. |
| Có thể hại khi | Query nhỏ, dữ liệu nằm trong cache, `ORDER BY ... LIMIT n` nhỏ, overhead sort/buffer lớn hơn lợi ích. |
| Biến chính | `mrr_buffer_size`, `join_buffer_size`, `join_buffer_space_limit`, các flag `mrr`, `mrr_sort_keys`, `mrr_cost_based`. |
| Status chính | `Handler_mrr_init`, `Handler_mrr_key_refills`, `Handler_mrr_rowid_refills`. |

Kết luận thực dụng:

```text
MRR không làm optimizer tìm ít row hơn.
MRR làm thứ tự đọc row hiệu quả hơn.

Nếu bottleneck là random I/O:
-> MRR có thể giúp nhiều.

Nếu dữ liệu đã nằm trong cache hoặc query rất nhỏ:
-> MRR có thể chỉ thêm overhead.
```
