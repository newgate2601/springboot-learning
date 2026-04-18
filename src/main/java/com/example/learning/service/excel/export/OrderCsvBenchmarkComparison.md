# Phân Tích Benchmark CSV Và ZIP CSV Với 1.000.000 Record

Tài liệu này tổng hợp và phân tích chi tiết các benchmark mới cho nhánh `CSV JDBC`, sau đó đối chiếu với các benchmark export trước đó trong repo:

- `FastExcel XLSX` với `keyset batch + ResultSet`
- `FastExcel XLSX` với `cursor + ResultSet`
- `FastExcel XLSX` với `JPA keyset batch`
- `POI SXSSF XLSX` với `JPA keyset batch`

Dataset benchmark:

- `1.000.000` record `orders`
- `batchSize = 50.000`
- `fetchSize = 50.000`

Các biến thể CSV mới cần so sánh:

1. `csv-jdbc-keyset-batch`
2. `zip-csv-jdbc-keyset-batch`
3. `csv-jdbc-cursor`
4. `zip-csv-jdbc-cursor`

## 1. Lưu ý quan trọng khi đọc số liệu

Trong implementation benchmark hiện tại:

- `queryMs` không phải pure DB time
- `writeMs` cũng không hoàn toàn tách biệt với phần đọc
- với `cursor`, `queryMs` gần như bao trùm toàn bộ vòng `ResultSet.next() + serialize`
- với `keyset batch`, `queryMs` cũng bao gồm thời gian đọc và ghi trong từng vòng batch

Vì vậy:

- `totalMs` là chỉ số đáng tin nhất để so throughput end-to-end
- `heapTransientMb`, `heapAfterMb`, `gcCount`, `gcTimeMs` là chỉ số đáng tin để so memory behavior
- `writeMs` và `queryMs` chỉ nên dùng để nhìn xu hướng tương đối, không nên xem như số đo tách lớp hoàn toàn chính xác

## 2. Kết quả tổng hợp của 4 biến thể CSV

### 2.1 Bảng tổng hợp

| Variant | totalMs | fileSizeKb | heapTransientMb | heapAfterMb | gcCount | gcTimeMs | queries |
|---|---:|---:|---:|---:|---:|---:|---:|
| `csv-jdbc-keyset-batch` | `2599` | `116775.31` | `156.21` | `188.79` | `19` | `66` | `20` |
| `zip-csv-jdbc-keyset-batch` | `3505` | `10944.69` | `179.87` | `212.45` | `14` | `78` | `20` |
| `csv-jdbc-cursor` | `3640` | `116775.31` | `258.75` | `291.35` | `23` | `127` | `1` |
| `zip-csv-jdbc-cursor` | `4812` | `10944.69` | `237.92` | `270.54` | `18` | `114` | `1` |

### 2.2 Thứ hạng theo tốc độ

1. `csv-jdbc-keyset-batch`: `2599ms`
2. `zip-csv-jdbc-keyset-batch`: `3505ms`
3. `csv-jdbc-cursor`: `3640ms`
4. `zip-csv-jdbc-cursor`: `4812ms`

### 2.3 Thứ hạng theo memory profile

Nhìn theo `heapTransientMb`, tốt nhất đến kém nhất:

1. `csv-jdbc-keyset-batch`: `156.21MB`
2. `zip-csv-jdbc-keyset-batch`: `179.87MB`
3. `zip-csv-jdbc-cursor`: `237.92MB`
4. `csv-jdbc-cursor`: `258.75MB`

Nhìn theo `heapAfterMb`, tốt nhất đến kém nhất:

1. `csv-jdbc-keyset-batch`: `188.79MB`
2. `zip-csv-jdbc-keyset-batch`: `212.45MB`
3. `zip-csv-jdbc-cursor`: `270.54MB`
4. `csv-jdbc-cursor`: `291.35MB`

## 3. So sánh chi tiết 4 biến thể CSV

## 3.1 `CSV plain keyset batch` vs `CSV plain cursor`

### Số liệu chính

- `csv-jdbc-keyset-batch`: `totalMs=2599`, `heapTransientMb=156.21`, `heapAfterMb=188.79`
- `csv-jdbc-cursor`: `totalMs=3640`, `heapTransientMb=258.75`, `heapAfterMb=291.35`

### Kết luận

`CSV plain keyset batch` thắng rất rõ so với `CSV plain cursor`.

Cụ thể:

- nhanh hơn khoảng `1041ms`
- nhanh hơn khoảng `1.40x`
- transient heap thấp hơn khoảng `102.54MB`
- heap cuối phiên thấp hơn khoảng `102.56MB`
- GC count ít hơn: `19` so với `23`
- GC time thấp hơn nhiều: `66ms` so với `127ms`

### Ý nghĩa

Trong benchmark hiện tại, `cursor` không đem lại lợi thế memory như kỳ vọng. Ngược lại:

- `keyset batch` giữ được profile heap thấp hơn
- `cursor` có xu hướng giữ nhiều data/serialization state hơn trong suốt vòng export
- với CSV plain, `keyset batch` là đáp án tốt hơn rõ ràng

## 3.2 `ZIP CSV keyset batch` vs `ZIP CSV cursor`

### Số liệu chính

- `zip-csv-jdbc-keyset-batch`: `totalMs=3505`, `heapTransientMb=179.87`, `heapAfterMb=212.45`
- `zip-csv-jdbc-cursor`: `totalMs=4812`, `heapTransientMb=237.92`, `heapAfterMb=270.54`

### Kết luận

`ZIP CSV keyset batch` cũng thắng `ZIP CSV cursor` rất rõ.

Cụ thể:

- nhanh hơn khoảng `1307ms`
- nhanh hơn khoảng `1.37x`
- transient heap thấp hơn khoảng `58.05MB`
- heap cuối phiên thấp hơn khoảng `58.09MB`
- GC count thấp hơn: `14` so với `18`
- GC time thấp hơn: `78ms` so với `114ms`

### Ý nghĩa

Ngay cả khi thêm cost nén ZIP:

- `keyset batch` vẫn ổn định hơn
- `cursor` vẫn không đảo ngược được thế cờ
- compression làm tăng CPU cost, nhưng không giúp `cursor` trở thành giải pháp nhẹ hơn

## 3.3 `CSV plain` vs `ZIP CSV` trên cùng `keyset batch`

### Số liệu chính

- `csv-jdbc-keyset-batch`: `2599ms`, `116775.31KB`, `156.21MB`
- `zip-csv-jdbc-keyset-batch`: `3505ms`, `10944.69KB`, `179.87MB`

### Kết luận

Nếu giữ nguyên cách đọc là `keyset batch`:

- `CSV plain` nhanh hơn khoảng `906ms`
- `ZIP CSV` chậm hơn khoảng `1.35x`
- `ZIP CSV` dùng transient heap cao hơn khoảng `23.66MB`
- nhưng file output nhỏ hơn cực mạnh

Mức giảm kích thước file:

- từ `116775.31KB` xuống `10944.69KB`
- giảm khoảng `105830.62KB`
- tương đương giảm khoảng `90.63%`
- file ZIP còn khoảng `9.37%` kích thước CSV gốc

### Ý nghĩa

Đây là trade-off rất đáng giá:

- nếu ưu tiên tốc độ tuyệt đối trong LAN hoặc ghi local disk: `CSV plain`
- nếu ưu tiên bandwidth, thời gian tải xuống, lưu trữ: `ZIP CSV`

## 3.4 `CSV plain` vs `ZIP CSV` trên cùng `cursor`

### Số liệu chính

- `csv-jdbc-cursor`: `3640ms`, `116775.31KB`, `258.75MB`
- `zip-csv-jdbc-cursor`: `4812ms`, `10944.69KB`, `237.92MB`

### Kết luận

Trong mode `cursor`:

- `ZIP CSV` chậm hơn khoảng `1172ms`
- chậm hơn khoảng `1.32x`
- nhưng file nhỏ hơn khoảng `90.63%`
- transient heap lại thấp hơn khoảng `20.83MB`

### Ý nghĩa

Với `cursor`, nén ZIP làm giảm output size rất mạnh nhưng không cứu được bài toán throughput tổng thể. Mode này vẫn chậm hơn đáng kể so với `keyset batch`.

## 4. Kết luận riêng cho nhánh CSV

Nếu chỉ xét 4 biến thể CSV hiện tại, thứ tự khuyến nghị là:

1. `csv-jdbc-keyset-batch`
2. `zip-csv-jdbc-keyset-batch`
3. `csv-jdbc-cursor`
4. `zip-csv-jdbc-cursor`

Nói ngắn gọn:

- muốn nhanh nhất: chọn `CSV plain + keyset batch`
- muốn file nhỏ nhất nhưng vẫn hợp lý về performance: chọn `ZIP CSV + keyset batch`
- không có dấu hiệu nào cho thấy `cursor` đang là lựa chọn tốt hơn trong benchmark này

## 5. Đối chiếu với các benchmark XLSX trước đó

## 5.1 Bảng tổng hợp tất cả biến thể chính

| Variant | Format | Reader | totalMs | fileSizeKb | heapTransientMb | heapAfterMb |
|---|---|---|---:|---:|---:|---:|
| `csv-jdbc-keyset-batch` | `csv` | `jdbc keyset` | `2599` | `116775.31` | `156.21` | `188.79` |
| `zip-csv-jdbc-keyset-batch` | `zip(csv)` | `jdbc keyset` | `3505` | `10944.69` | `179.87` | `212.45` |
| `csv-jdbc-cursor` | `csv` | `jdbc cursor` | `3640` | `116775.31` | `258.75` | `291.35` |
| `zip-csv-jdbc-cursor` | `zip(csv)` | `jdbc cursor` | `4812` | `10944.69` | `237.92` | `270.54` |
| `fastexcel-jdbc-keyset-batch` | `xlsx` | `jdbc keyset` | `5600` | `43256.91` | `157.13` | `189.81` |
| `fastexcel-jdbc-cursor` | `xlsx` | `jdbc cursor` | `6333` | `43359.46` | `660.01` | `692.70` |
| `fastexcel-streaming` | `xlsx` | `jpa keyset` | `8072` | `43256.91` | `335.82` | `366.33` |
| `poi-sxssf` | `xlsx` | `jpa keyset` | `22741` | `38901.67` | `406.79` | `438.12` |

## 5.2 `CSV plain keyset batch` vs `FastExcel XLSX keyset batch`

### Số liệu chính

- `csv-jdbc-keyset-batch`: `2599ms`, `116775.31KB`, `156.21MB`
- `fastexcel-jdbc-keyset-batch`: `5600ms`, `43256.91KB`, `157.13MB`

### Kết luận

Nếu cùng là `JDBC keyset batch`:

- `CSV plain` nhanh hơn khoảng `2.15x`
- transient heap gần như tương đương
- `CSV` lớn file hơn khoảng `2.70x`

Ý nghĩa:

- phần đắt nhất của `.xlsx` nằm ở việc generate cấu trúc workbook/worksheet/xml/zip, không phải riêng DB read
- nếu user không cần file Excel native thì `CSV plain` là baseline throughput mạnh hơn rất nhiều

## 5.3 `ZIP CSV keyset batch` vs `FastExcel XLSX keyset batch`

### Số liệu chính

- `zip-csv-jdbc-keyset-batch`: `3505ms`, `10944.69KB`, `179.87MB`
- `fastexcel-jdbc-keyset-batch`: `5600ms`, `43256.91KB`, `157.13MB`

### Kết luận

So với `FastExcel XLSX keyset batch`:

- `ZIP CSV` vẫn nhanh hơn khoảng `2095ms`
- nhanh hơn khoảng `1.60x`
- file nhỏ hơn rất mạnh, chỉ bằng khoảng `25.30%`
- đổi lại transient heap cao hơn khoảng `22.74MB`

Đây là một kết quả rất đáng chú ý:

- nếu user chấp nhận tải `.zip` rồi mở `.csv`
- thì `ZIP CSV + keyset batch` vừa nhanh hơn `.xlsx`, vừa nhỏ file hơn `.xlsx`

## 5.4 `CSV plain cursor` vs `FastExcel XLSX cursor`

### Số liệu chính

- `csv-jdbc-cursor`: `3640ms`, `258.75MB`
- `fastexcel-jdbc-cursor`: `6333ms`, `660.01MB`

### Kết luận

Ngay cả khi cùng dùng `cursor`:

- `CSV plain` nhanh hơn khoảng `1.74x`
- transient heap thấp hơn khoảng `401.26MB`

Điều này cho thấy phần overhead cực lớn của `XLSX cursor` nằm ở writer `.xlsx`, không chỉ do bản thân cursor.

## 5.5 `ZIP CSV cursor` vs `FastExcel XLSX cursor`

### Số liệu chính

- `zip-csv-jdbc-cursor`: `4812ms`, `237.92MB`
- `fastexcel-jdbc-cursor`: `6333ms`, `660.01MB`

### Kết luận

Ngay cả phiên bản `ZIP CSV cursor` vẫn:

- nhanh hơn `FastExcel XLSX cursor`
- nhẹ heap hơn rất nhiều

Nói cách khác:

- nếu đã chọn `cursor`, thì `CSV` và `ZIP CSV` vẫn thực dụng hơn `XLSX`
- `XLSX cursor` hiện là một trong những mode kém hiệu quả nhất trong toàn bộ bộ benchmark

## 5.6 `FastExcel XLSX JDBC keyset` vs `FastExcel XLSX JPA keyset`

### Số liệu chính

- `fastexcel-jdbc-keyset-batch`: `5600ms`, `157.13MB`
- `fastexcel-streaming`: `8072ms`, `335.82MB`

### Kết luận

Khi cùng là `.xlsx + FastExcel + keyset`:

- chuyển từ `JPA DTO batch` sang `JDBC ResultSet batch` giúp giảm thời gian đáng kể
- transient heap giảm cực mạnh

Điều này xác nhận:

- materialize `List<OrderExportRow>` trong JPA batch là cost lớn
- `ResultSet` direct read mang lại lợi thế rõ rệt cho cả throughput lẫn memory

## 5.7 `POI SXSSF` đang đứng ở đâu

`poi-sxssf` có số liệu:

- `totalMs=22741`
- `heapTransientMb=406.79`
- `fileSizeKb=38901.67`

So với tất cả biến thể CSV và FastExcel JDBC:

- chậm nhất rất xa
- heap cũng không còn lợi thế khi `rowWindow = 50.000`
- file nhỏ hơn `FastExcel XLSX` nhưng không đủ bù cho cost CPU và heap

Với cấu hình benchmark hiện tại, `POI SXSSF` không còn là ứng viên cạnh tranh tốt.

## 6. Kết luận thực dụng cho production

Nếu yêu cầu là `bắt buộc .xlsx`:

1. `FastExcel XLSX + JDBC keyset batch` là lựa chọn tốt nhất hiện tại
2. tránh `FastExcel XLSX + cursor` với implementation hiện tại
3. `POI SXSSF` chỉ đáng cân nhắc nếu đổi lại cấu hình row window nhỏ và có nhu cầu tương thích/thao tác Excel riêng của POI

Nếu yêu cầu là `mở được bằng Excel`, không bắt buộc `.xlsx`:

1. `CSV plain + JDBC keyset batch` là nhanh nhất
2. `ZIP CSV + JDBC keyset batch` là lựa chọn cân bằng rất mạnh
3. `cursor` không có lợi thế rõ rệt trong bộ benchmark này

## 7. Kết luận cuối cùng

Từ toàn bộ benchmark hiện có, thứ tự mạnh nhất theo hiệu năng thực dụng là:

1. `CSV plain + JDBC keyset batch`
2. `ZIP CSV + JDBC keyset batch`
3. `CSV plain + JDBC cursor`
4. `ZIP CSV + JDBC cursor`
5. `FastExcel XLSX + JDBC keyset batch`
6. `FastExcel XLSX + JDBC cursor`
7. `FastExcel XLSX + JPA keyset batch`
8. `POI SXSSF + JPA keyset batch`

Điểm đáng chú ý nhất của loạt benchmark này là:

- `ResultSet keyset batch` đang là mô hình đọc dữ liệu tốt nhất trong repo
- `cursor` hiện không thắng về memory như trực giác ban đầu
- `CSV/ZIP CSV` cho throughput vượt trội so với `XLSX`
- `ZIP CSV + keyset batch` là điểm cân bằng rất mạnh giữa tốc độ, memory và kích thước file

## 8. Hướng benchmark tiếp theo nên làm

Để làm bộ benchmark chặt hơn nữa, nên bổ sung:

1. `peakHeapMb` thay vì chỉ log `heapAfterMb` và `heapTransientMb`
2. tách riêng `dbReadMs` khỏi `serializeMs`
3. thêm benchmark `COPY TO STDOUT CSV`
4. benchmark `FastExcel XLSX + ZIP response` nếu muốn so thêm cost nén ở tầng HTTP/file
5. benchmark `SXSSF` với `rowWindow = 100`, `500`, `1000`

