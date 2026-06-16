# MariaDB Compression

## 1. Compression trong MariaDB là gì?

**Compression** là cơ chế nén dữ liệu để giảm dung lượng lưu trữ trên disk, giảm lượng I/O khi đọc/ghi dữ liệu và trong một số workload có thể cải thiện hiệu năng tổng thể.

Trong MariaDB, compression không chỉ có một loại. Tùy storage engine, version và kiểu dữ liệu, có thể gặp nhiều cơ chế khác nhau:

| Nhóm | Mục tiêu | Phạm vi |
| --- | --- | --- |
| Compression plugin | Cung cấp thuật toán nén như `lz4`, `lzma`, `bzip2`, `lzo`, `snappy` | Được một số engine/cơ chế nén sử dụng |
| Storage-engine independent column compression | Nén từng column kiểu text/blob | Không phụ thuộc storage engine, hiện chủ yếu dùng `zlib` |
| InnoDB page compression | Nén page dữ liệu của InnoDB | Chỉ áp dụng cho InnoDB |
| Engine-specific compression | Cơ chế riêng của từng engine như RocksDB | Phụ thuộc engine |

Cần phân biệt rõ:

```text
Nén dữ liệu
= giảm dung lượng lưu trữ và I/O

Không có nghĩa là query tự động nhanh hơn
= vì CPU phải tốn thêm chi phí compress/decompress
```

Compression thường là trade-off:

```text
Tiết kiệm disk / giảm I/O
đổi lấy
tăng CPU / tăng độ phức tạp vận hành
```

## 2. Compression plugins

### 2.1. Compression plugins là gì?

Từ MariaDB 10.7.0, MariaDB bổ sung cơ chế **compression plugins**. Trước đó, nếu muốn dùng một thư viện nén cụ thể, thư viện thường phải được compile sẵn vào MariaDB hoặc engine liên quan.

Từ MariaDB 10.7.0, ngoài `zlib` mặc định, một số thư viện nén có thể được cung cấp dưới dạng plugin:

- `bzip2`
- `lzma`
- `lz4`
- `lzo`
- `snappy`

Ý nghĩa thực tế:

- dễ đóng gói và cài đặt thư viện nén hơn;
- không bắt buộc mọi thuật toán phải được compile trực tiếp vào server;
- chỉ cài plugin cần dùng;
- giảm dependency không cần thiết.

Lưu ý quan trọng:

```text
Compression plugins ảnh hưởng chủ yếu tới InnoDB và Mroonga.
RocksDB vẫn dùng các thuật toán nén từ thư viện riêng của RocksDB.
```

### 2.2. Cài đặt compression plugin

Tùy cách cài MariaDB, package plugin có thể đã có sẵn hoặc cần cài thêm qua `.deb`/`.rpm`.

Ví dụ trên hệ Debian/Ubuntu:

```bash
apt-get install mariadb-plugin-provider-lz4
```

Sau khi package có sẵn, install plugin trong MariaDB:

```sql
INSTALL SONAME 'provider_lz4';
```

Sau đó có thể dùng thuật toán nén, ví dụ với InnoDB compression:

```sql
SET GLOBAL innodb_compression_algorithm = lz4;
```

Kiểm tra plugin đã load chưa:

```sql
SHOW PLUGINS;
```

Hoặc:

```sql
SELECT PLUGIN_NAME, PLUGIN_STATUS
FROM INFORMATION_SCHEMA.PLUGINS
WHERE PLUGIN_NAME LIKE '%lz4%';
```

### 2.3. Khi upgrade cần chú ý gì?

Nếu một table đã được nén bằng thuật toán không phải `zlib`, server cần load đúng provider plugin để đọc table đó.

Nếu thiếu plugin, có thể gặp lỗi kiểu:

```text
Warning : MariaDB tried to use the LZMA compression, but its provider plugin is not loaded
Error   : Table 'test.t' doesn't exist in engine
status  : Operation failed
```

Hoặc:

```text
Error : Table test/t is compressed with lzma, which is not currently loaded.
        Please load the lzma provider plugin to open the table
error : Corrupt
```

Trong trường hợp này, table không hẳn bị corrupt theo nghĩa dữ liệu hỏng. Vấn đề là server không có thư viện giải nén phù hợp để mở table.

Khi upgrade:

1. Kiểm tra các thuật toán nén đang dùng.
2. Cài package provider tương ứng.
3. Load plugin bằng `INSTALL SONAME`.
4. Restart server nếu cần.
5. Chạy `mariadb-upgrade --force` nếu upgrade từ version cũ.
6. Kiểm tra lại bằng `mariadb-check`.

### 2.4. Ưu điểm của compression plugins

- Linh hoạt hơn trong việc chọn thuật toán nén.
- Không cần build MariaDB với mọi thư viện nén.
- Dễ đóng gói theo package riêng.
- Có thể chọn thuật toán phù hợp với workload: nhanh, nén tốt, cân bằng CPU/I/O.
- Giảm dependency không dùng tới.

### 2.5. Nhược điểm của compression plugins

- Thêm dependency vận hành.
- Thiếu plugin có thể làm table không đọc được.
- Upgrade/phục hồi backup cần đảm bảo plugin tương ứng có mặt.
- Không phải engine nào cũng dùng các plugin này.
- Chọn sai thuật toán có thể làm CPU tăng mạnh hoặc nén không đáng kể.

### 2.6. Use case phù hợp

Phù hợp khi:

- database lớn, chi phí disk cao;
- workload I/O-bound nhiều hơn CPU-bound;
- table có dữ liệu text/blob dễ nén;
- muốn dùng thuật toán nhanh như `lz4`/`snappy` để giảm I/O mà không tăng CPU quá nhiều;
- muốn dùng thuật toán nén mạnh hơn như `lzma` cho dữ liệu ít ghi, đọc không quá thường xuyên.

Không phù hợp khi:

- workload đã CPU-bound;
- dữ liệu đã nén sẵn như JPEG, PNG, ZIP, PDF, encrypted payload;
- table rất nhỏ;
- yêu cầu vận hành đơn giản, ít dependency;
- hệ thống upgrade/restore thường xuyên nhưng chưa kiểm soát plugin tốt.

## 3. Chọn thuật toán nén

Các thuật toán thường khác nhau ở ba điểm:

- tốc độ nén;
- tốc độ giải nén;
- tỉ lệ nén.

Bảng định hướng:

| Thuật toán | Đặc điểm thường gặp | Phù hợp khi |
| --- | --- | --- |
| `zlib` | Cân bằng, phổ biến, mặc định ở nhiều cơ chế | Muốn an toàn, phổ biến, dễ vận hành |
| `lz4` | Rất nhanh, tỉ lệ nén vừa phải | Workload cần latency thấp, đọc/ghi nhiều |
| `snappy` | Nhanh, thiên về tốc độ hơn tỉ lệ nén | Workload realtime, giảm I/O nhẹ |
| `lzma` | Nén tốt hơn nhưng tốn CPU hơn | Dữ liệu lạnh, archive, ít update |
| `bzip2` | Nén khá tốt nhưng thường chậm hơn các thuật toán nhanh | Dữ liệu ít ghi, ưu tiên dung lượng |
| `lzo` | Nhanh, tỉ lệ nén vừa phải | Workload cần tốc độ, nếu môi trường hỗ trợ tốt |

Nguyên tắc:

```text
Dữ liệu nóng, đọc/ghi thường xuyên -> ưu tiên thuật toán nhanh như lz4/snappy.
Dữ liệu lạnh, ít ghi, cần tiết kiệm disk -> cân nhắc lzma/bzip2.
Không chắc -> bắt đầu với zlib hoặc lz4 rồi benchmark.
```

## 4. Storage-engine independent column compression

### 4.1. Column compression là gì?

**Storage-engine independent column compression** là cơ chế nén dữ liệu ở cấp column, không phụ thuộc trực tiếp vào storage engine.

Cơ chế này cho phép nén các column kiểu:

- `TINYBLOB`
- `BLOB`
- `MEDIUMBLOB`
- `LONGBLOB`
- `TINYTEXT`
- `TEXT`
- `MEDIUMTEXT`
- `LONGTEXT`
- `VARCHAR`
- `VARBINARY`

Cú pháp:

```sql
CREATE TABLE cmp (
  i TEXT COMPRESSED
);
```

Hoặc chỉ rõ method:

```sql
CREATE TABLE cmp2 (
  i TEXT COMPRESSED=zlib
);
```

Hiện tại method được hỗ trợ là:

```text
zlib
```

### 4.2. Cơ chế hoạt động

Khi insert/update dữ liệu vào column có `COMPRESSED`, MariaDB sẽ nén giá trị của field nếu đủ điều kiện. Khi đọc ra, dữ liệu được giải nén để trả về cho query.

Luồng xử lý:

```text
INSERT/UPDATE
-> dữ liệu gốc
-> nén bằng zlib nếu vượt threshold
-> lưu dạng nén

SELECT
-> đọc dữ liệu nén
-> giải nén
-> trả dữ liệu gốc cho client
```

Điểm quan trọng:

```text
Ứng dụng vẫn nhìn thấy dữ liệu gốc.
Compression là chi tiết lưu trữ ở phía MariaDB.
```

### 4.3. Field length compatibility

Khi dùng thuộc tính `COMPRESSED`, field length bị giảm đi 1.

Ví dụ:

```text
BLOB thường có length tối đa 65535.
BLOB COMPRESSED có length tối đa 65535 - 1.
```

Lý do là MariaDB cần thêm metadata nội bộ để biết dữ liệu trong field có được nén hay không.

### 4.4. Biến cấu hình

#### `column_compression_threshold`

Ý nghĩa: độ dài tối thiểu của dữ liệu column để được xét nén.

Thông tin chính:

| Thuộc tính | Giá trị |
| --- | --- |
| Scope | Global, Session |
| Dynamic | Yes |
| Data type | Numeric |
| Default | `100` |
| Range | `0` đến `4294967295` |

Ví dụ:

```sql
SET SESSION column_compression_threshold = 200;
```

Cách tuning:

- tăng threshold nếu nhiều giá trị nhỏ bị nén nhưng không tiết kiệm đáng kể;
- giảm threshold nếu muốn nén nhiều giá trị hơn;
- không nên đặt quá thấp nếu workload ghi nhiều, vì nén dữ liệu nhỏ thường không đáng chi phí CPU.

#### `column_compression_zlib_level`

Ý nghĩa: mức nén của `zlib`.

Thông tin chính:

| Thuộc tính | Giá trị |
| --- | --- |
| Scope | Global, Session |
| Dynamic | Yes |
| Data type | Numeric |
| Default | `6` |
| Range | `1` đến `9` |

Quy ước:

```text
1 = ưu tiên tốc độ
9 = ưu tiên tỉ lệ nén
```

Ví dụ:

```sql
SET SESSION column_compression_zlib_level = 1;
```

Cách tuning:

- workload ghi nhiều, latency nhạy cảm: thử level `1` đến `3`;
- workload cân bằng: giữ mặc định `6`;
- dữ liệu archive, ít ghi, cần tiết kiệm disk: thử level `7` đến `9`;
- luôn benchmark vì tăng level không phải lúc nào cũng giảm dung lượng đủ nhiều để đáng chi phí CPU.

#### `column_compression_zlib_strategy`

Ý nghĩa: strategy parameter dùng để tune thuật toán `zlib`.

Giá trị hợp lệ:

| Giá trị | Ý nghĩa định hướng |
| --- | --- |
| `DEFAULT_STRATEGY` | Dữ liệu bình thường |
| `FILTERED` | Dữ liệu đã qua filter/predictor, nhiều giá trị nhỏ, phân bố tương đối random |
| `HUFFMAN_ONLY` | Chỉ dùng Huffman coding, không string match |
| `RLE` | Tối ưu cho run-length encoding, dữ liệu có chuỗi lặp |
| `FIXED` | Dùng fixed Huffman codes |

Ví dụ:

```sql
SET SESSION column_compression_zlib_strategy = DEFAULT_STRATEGY;
```

Cách tuning:

- phần lớn workload dùng `DEFAULT_STRATEGY`;
- thử `RLE` nếu dữ liệu có nhiều chuỗi lặp dài;
- thử `FILTERED` nếu dữ liệu đã được tiền xử lý hoặc có phân bố đặc biệt;
- không đổi strategy nếu chưa benchmark vì ảnh hưởng chủ yếu đến tỉ lệ nén, không làm dữ liệu đúng/sai khác đi.

#### `column_compression_zlib_wrap`

Ý nghĩa: nếu bật, MariaDB tạo zlib header/trailer và tính checksum `adler32`.

Thông tin chính:

| Thuộc tính | Giá trị |
| --- | --- |
| Scope | Global, Session |
| Dynamic | Yes |
| Data type | Boolean |
| Default | `OFF` |

Ví dụ:

```sql
SET SESSION column_compression_zlib_wrap = 1;
```

Cách tuning:

- bật nếu storage engine không cung cấp kiểm tra toàn vẹn dữ liệu phù hợp và muốn có thêm lớp phát hiện corruption;
- tắt nếu ưu tiên overhead thấp và engine đã có cơ chế checksum/integrity đủ tốt.

### 4.5. Status variables

#### `Column_compressions`

Tăng mỗi khi field data được nén.

```sql
SHOW GLOBAL STATUS LIKE 'Column_compressions';
```

#### `Column_decompressions`

Tăng mỗi khi field data được giải nén.

```sql
SHOW GLOBAL STATUS LIKE 'Column_decompressions';
```

Cách đọc:

```text
Column_compressions tăng nhanh
-> workload đang ghi/update nhiều dữ liệu compressed.

Column_decompressions tăng nhanh
-> workload đang đọc nhiều dữ liệu compressed.
```

Nếu `Column_decompressions` tăng rất nhanh và CPU cao, compression có thể đang làm query đọc bị tốn CPU.

### 4.6. Hạn chế của column compression

Các hạn chế quan trọng:

- method được hỗ trợ hiện tại là `zlib`;
- CSV storage engine lưu dữ liệu dạng không nén trên disk ngay cả khi column có `COMPRESSED`;
- không thể tạo index trên compressed column;
- không nên nén dữ liệu đã nén sẵn;
- không nên dùng nhiều lớp compression trên cùng một dữ liệu;
- đọc column compressed luôn có chi phí decompress.

Ví dụ hạn chế về index:

```sql
CREATE TABLE docs (
  id BIGINT PRIMARY KEY,
  body TEXT COMPRESSED
);
```

Không nên kỳ vọng tạo index trực tiếp hiệu quả trên `body` compressed. Nếu cần search, nên có column phụ, fulltext strategy hoặc hệ thống search riêng tùy use case.

### 4.7. Ưu điểm của column compression

- Nén chính xác những column lớn cần nén.
- Không ép toàn bộ table/page phải nén.
- Có thể đọc các column không nén mà không phải decompress column lớn nếu query không đụng tới chúng.
- Hữu ích cho `TEXT`, `BLOB`, payload JSON/XML/log lớn.
- Không phụ thuộc storage engine theo cách giống InnoDB page compression.

### 4.8. Nhược điểm của column compression

- Chỉ hỗ trợ method `zlib`.
- Không index được compressed column.
- Tăng CPU khi insert/update/select column đó.
- Không phù hợp với dữ liệu nhỏ hoặc đã nén sẵn.
- Có thêm giới hạn field length.
- Có thể làm query chậm nếu đọc column compressed quá thường xuyên.

### 4.9. Use case phù hợp

Phù hợp khi:

- table có column lớn nhưng không phải query nào cũng đọc column đó;
- lưu payload JSON/XML/text/log;
- lưu nội dung document dạng text;
- dữ liệu đọc không quá thường xuyên;
- muốn giảm disk cho column lớn mà vẫn giữ các column filter/order bình thường không nén.

Ví dụ:

```sql
CREATE TABLE api_logs (
  id BIGINT PRIMARY KEY,
  created_at DATETIME NOT NULL,
  service_name VARCHAR(100) NOT NULL,
  status_code INT NOT NULL,
  request_body TEXT COMPRESSED,
  response_body TEXT COMPRESSED,
  KEY idx_api_logs_created_at (created_at),
  KEY idx_api_logs_service_status (service_name, status_code)
);
```

Ở đây:

- `created_at`, `service_name`, `status_code` vẫn dùng để filter/index;
- `request_body`, `response_body` lớn nên được nén;
- query thống kê theo thời gian/status không cần decompress body nếu không select body.

Không phù hợp khi:

- query nào cũng đọc column compressed;
- column cần index;
- dữ liệu là ảnh/video/pdf/zip đã nén;
- workload update column đó liên tục;
- CPU là bottleneck chính.

## 5. So sánh column compression và InnoDB page compression

| Tiêu chí | Column compression | InnoDB page compression |
| --- | --- | --- |
| Phạm vi | Từng column | Page dữ liệu InnoDB |
| Storage engine | Độc lập hơn với engine | Chỉ InnoDB |
| Thuật toán | Chủ yếu `zlib` | Có thể dùng thuật toán khác tùy version/plugin |
| Truy cập column khác | Không cần decompress column nén nếu không đọc | Có thể phải xử lý page nén |
| Index | Không index được compressed column | Index vẫn hoạt động theo cơ chế InnoDB |
| Use case chính | BLOB/TEXT lớn | Nén tổng quát cho table/index InnoDB |
| Rủi ro | Tăng CPU khi đọc column lớn | Phức tạp hơn ở cấp page/filesystem/I/O |

Điểm khác biệt cốt lõi:

```text
Column compression
= nén một vài column lớn.

InnoDB page compression
= nén page lưu trữ của InnoDB, tác động rộng hơn lên table/index.
```

Không nên nén chồng nhiều lớp trên cùng dữ liệu nếu không có lý do rất rõ:

```text
TEXT COMPRESSED
+ InnoDB page compression
+ dữ liệu bên trong đã gzip
= thường tốn CPU, ít lợi ích thêm
```

## 6. Tuning compression như thế nào?

### 6.1. Xác định mục tiêu

Trước khi bật compression, cần biết mình muốn tối ưu gì:

| Mục tiêu | Hướng tuning |
| --- | --- |
| Giảm disk | Ưu tiên thuật toán nén tốt hơn, level cao hơn |
| Giảm I/O | Dùng compression nếu workload I/O-bound |
| Giữ latency thấp | Ưu tiên thuật toán nhanh, level thấp |
| Giảm backup size | Compression có thể hữu ích nếu backup không nén thêm |
| Tiết kiệm cache/buffer | Dữ liệu nhỏ hơn có thể giúp đọc ít page hơn, tùy cơ chế |

### 6.2. Đo trước khi bật

Cần ghi lại baseline:

- kích thước table/index;
- thời gian query đọc;
- thời gian insert/update;
- CPU usage;
- disk read/write throughput;
- slow query log;
- tỉ lệ buffer pool hit nếu dùng InnoDB.

Một số lệnh hữu ích:

```sql
SHOW TABLE STATUS LIKE 'api_logs';
```

```sql
SELECT table_schema, table_name, data_length, index_length
FROM information_schema.tables
WHERE table_schema = 'your_db'
  AND table_name = 'api_logs';
```

### 6.3. Test trên dữ liệu đại diện

Không nên test compression trên vài row mẫu quá nhỏ.

Cần test:

- dữ liệu text ngắn;
- dữ liệu text dài;
- dữ liệu có nhiều pattern lặp;
- dữ liệu gần random;
- dữ liệu đã nén sẵn;
- workload đọc nhiều;
- workload ghi nhiều.

### 6.4. Đo sau khi bật

Sau khi bật compression, so sánh:

- disk giảm bao nhiêu phần trăm;
- CPU tăng bao nhiêu;
- query đọc nhanh hơn hay chậm hơn;
- insert/update chậm hơn bao nhiêu;
- `Column_compressions` và `Column_decompressions` tăng thế nào;
- temporary table hoặc I/O có tăng không.

Nếu disk chỉ giảm ít nhưng CPU tăng nhiều, compression không đáng dùng cho workload đó.

### 6.5. Chọn threshold và level

Với column compression:

```sql
SET SESSION column_compression_threshold = 200;
SET SESSION column_compression_zlib_level = 3;
```

Gợi ý:

- dữ liệu nhỏ nhiều: tăng `column_compression_threshold`;
- dữ liệu lớn, ít ghi: tăng `column_compression_zlib_level`;
- workload ghi nhiều: giảm `column_compression_zlib_level`;
- CPU cao: giảm level hoặc tắt compression cho column nóng.

## 7. Use case tổng hợp

### 7.1. Log/API payload lớn

Phù hợp:

```sql
CREATE TABLE api_logs (
  id BIGINT PRIMARY KEY,
  created_at DATETIME NOT NULL,
  endpoint VARCHAR(255) NOT NULL,
  status_code INT NOT NULL,
  request_payload TEXT COMPRESSED,
  response_payload TEXT COMPRESSED,
  KEY idx_api_logs_created_at (created_at),
  KEY idx_api_logs_endpoint_status (endpoint, status_code)
);
```

Lý do:

- payload thường lớn và dễ nén;
- query thống kê thường filter theo thời gian/status, không cần đọc payload;
- giảm dung lượng log table đáng kể.

### 7.2. Dữ liệu archive

Phù hợp:

- dữ liệu cũ ít update;
- đọc không thường xuyên;
- ưu tiên tiết kiệm disk;
- chấp nhận CPU decompress khi cần đọc.

Có thể cân nhắc thuật toán nén mạnh hơn hoặc zlib level cao hơn nếu workload cho phép.

### 7.3. Table đọc/ghi realtime

Cần cẩn thận:

- nếu mỗi request đều đọc/ghi dữ liệu compressed, CPU có thể tăng;
- nên ưu tiên thuật toán nhanh như `lz4` nếu dùng plugin phù hợp;
- với column compression, cân nhắc level thấp và threshold cao hơn.

### 7.4. Dữ liệu đã nén sẵn

Không phù hợp:

- ảnh JPEG/PNG;
- video;
- PDF đã nén;
- file ZIP/GZIP;
- encrypted payload.

Lý do:

```text
Dữ liệu đã nén hoặc mã hóa thường gần random.
Nén thêm thường không giảm dung lượng đáng kể,
nhưng vẫn tốn CPU.
```

### 7.5. Column cần index/search

Không phù hợp với column compression nếu cần index trực tiếp.

Thay vào đó có thể:

- lưu thêm column metadata để filter;
- dùng generated column cho phần cần search;
- dùng FULLTEXT nếu phù hợp;
- dùng search engine riêng như Elasticsearch/OpenSearch cho search phức tạp.

## 8. Ưu điểm tổng quát của compression

- Giảm dung lượng disk.
- Giảm lượng I/O vật lý.
- Có thể cải thiện hiệu năng nếu bottleneck là disk I/O.
- Giảm kích thước backup nếu backup không nén thêm.
- Hữu ích cho dữ liệu text/blob lớn.
- Có thể giúp lưu nhiều dữ liệu hơn trên cùng hạ tầng.

## 9. Nhược điểm tổng quát của compression

- Tăng CPU do compress/decompress.
- Tăng độ phức tạp vận hành.
- Có rủi ro thiếu plugin khi upgrade/restore.
- Một số cơ chế có hạn chế về index.
- Không hiệu quả với dữ liệu đã nén hoặc dữ liệu random.
- Có thể làm latency xấu hơn với workload đọc/ghi nóng.
- Cần benchmark, không nên bật theo cảm tính.

## 10. Checklist trước khi dùng compression trên production

- Đã xác định mục tiêu: giảm disk, giảm I/O hay giảm backup size.
- Đã biết workload đang I/O-bound hay CPU-bound.
- Đã kiểm tra dữ liệu có dễ nén không.
- Đã test trên dữ liệu gần production.
- Đã đo baseline trước khi bật.
- Đã so sánh CPU, latency, disk sau khi bật.
- Đã kiểm tra query có cần index trên column compressed không.
- Đã kiểm soát package/plugin khi backup, restore, upgrade.
- Đã có rollback plan.
- Đã ghi chú rõ column/table nào được nén và vì sao.

## 11. Kết luận thực dụng

Compression trong MariaDB rất hữu ích khi dữ liệu lớn, dễ nén và bottleneck nằm ở disk I/O hoặc dung lượng lưu trữ. Tuy nhiên compression không phải tối ưu miễn phí. Nó đổi dung lượng và I/O lấy CPU và độ phức tạp vận hành.

Cách dùng đúng:

```text
1. Xác định dữ liệu có đáng nén không.
2. Đo baseline.
3. Chọn cơ chế nén đúng phạm vi: column, page, plugin hoặc engine-specific.
4. Test trên dữ liệu đại diện.
5. Theo dõi CPU, latency, disk và status variables.
6. Không nén chồng nhiều lớp nếu không có bằng chứng rõ ràng.
```

Tóm gọn:

```text
Compression tốt = dữ liệu lớn, dễ nén, I/O-bound, có benchmark.
Compression xấu = dữ liệu đã nén, CPU-bound, bật theo cảm tính, không có kế hoạch vận hành plugin.
```

## 12. Tài liệu tham khảo

- MariaDB Docs: Compression Plugins
- MariaDB Docs: Storage-Engine Independent Column Compression
- MariaDB Docs: InnoDB Page Compression
