# Phân Tích Benchmark Streaming Với 1.000.000 Record

Tài liệu này là bản phân tích cập nhật cho benchmark streaming export `1.000.000` record `orders` theo log mới nhất và cấu hình code hiện tại:

- `BATCH_SIZE = 50_000`
- `POI_ROW_WINDOW_SIZE = 50_000`
- FastExcel đã dùng:
  - `sheet.flush()` theo batch
  - `inlineString(...)` cho các cột cardinality cao

Điểm quan trọng nhất của bản cập nhật này:

- POI SXSSF hiện không còn chạy với `row window` nhỏ
- cửa sổ row của SXSSF đang bằng đúng kích thước batch
- vì vậy các kết luận cũ kiểu "POI nhẹ RAM hơn rõ rệt vì chỉ giữ vài trăm row" không còn đúng với benchmark hiện tại

## Bối cảnh benchmark

Luồng benchmark hiện tại là `batch-read + streaming-write`:

1. Query dữ liệu theo keyset pagination dựa trên `id`
2. Mỗi lần load `50.000` row
3. Ghi ngay batch đó vào workbook
4. Stream file trực tiếp ra client bằng `StreamingResponseBody`

Điều đó giúp loại bỏ hai điểm nặng lớn của mô hình non-streaming:

- không load toàn bộ `1.000.000` row vào một `List`
- không trả file dưới dạng `byte[]`

Tuy nhiên đây vẫn chưa phải cursor streaming thực sự từ DB. Ứng dụng vẫn materialize từng batch `50.000` row vào memory trước khi ghi.

## Cấu hình hiện tại và ý nghĩa của nó

Từ code hiện tại:

- `BATCH_SIZE = 50_000`
- `POI_ROW_WINDOW_SIZE = 50_000`

Điều này có nghĩa:

- FastExcel giữ batch data phía ứng dụng và flush worksheet mỗi batch
- POI SXSSF cũng đang giữ tối đa gần trọn một batch row trong memory trước khi `flushRows(...)`

Nói ngắn gọn:

- benchmark này không còn là so sánh `FastExcel tuned` với `SXSSF row window nhỏ`
- mà là so sánh `FastExcel tuned` với `SXSSF row window lớn bằng batch size`

## Log gốc

### FastExcel streaming

Các batch load đầu và cuối:

```text
order-streaming load batchStartAfterId=null rows=50000 loadMs=359 heapBeforeLoadMb=36.51 heapAfterLoadMb=86.02 heapLoadDeltaMb=49.51 gcCountDuringLoad=5 gcTimeMsDuringLoad=22
...
order-streaming load batchStartAfterId=950000 rows=50000 loadMs=84 heapBeforeLoadMb=448.96 heapAfterLoadMb=210.33 heapLoadDeltaMb=-238.63 gcCountDuringLoad=1 gcTimeMsDuringLoad=22
order-streaming load batchStartAfterId=1000000 rows=0 loadMs=1 heapBeforeLoadMb=366.33 heapAfterLoadMb=366.33 heapLoadDeltaMb=0.0 gcCountDuringLoad=0 gcTimeMsDuringLoad=0
```

Tổng benchmark:

```text
order-streaming benchmark library=fastexcel-streaming expectedRows=1000000 streamedRows=1000000 batches=20 batchSize=50000 totalMs=8072 loadMs=1875 writeMs=5875 fileSizeKb=43256.91 heapBeforeMb=30.51 heapAfterMb=366.33 heapAfterForcedGcMb=31.36 heapTransientMb=335.82 heapRetainedMb=0.85 nonHeapBeforeMb=100.81 nonHeapAfterMb=103.61 gcCountDuringStreaming=54 gcTimeMsDuringStreaming=483
```

### POI SXSSF streaming với `row window = 50.000`

Các batch load đầu và cuối:

```text
order-streaming load batchStartAfterId=null rows=50000 loadMs=71 heapBeforeLoadMb=34.78 heapAfterLoadMb=80.03 heapLoadDeltaMb=45.25 gcCountDuringLoad=1 gcTimeMsDuringLoad=13
...
order-streaming load batchStartAfterId=950000 rows=50000 loadMs=42 heapBeforeLoadMb=336.12 heapAfterLoadMb=390.12 heapLoadDeltaMb=54.0 gcCountDuringLoad=0 gcTimeMsDuringLoad=0
order-streaming load batchStartAfterId=1000000 rows=0 loadMs=1 heapBeforeLoadMb=290.12 heapAfterLoadMb=290.12 heapLoadDeltaMb=0.0 gcCountDuringLoad=0 gcTimeMsDuringLoad=0
```

Tổng benchmark:

```text
order-streaming benchmark library=poi-sxssf expectedRows=1000000 streamedRows=1000000 batches=20 batchSize=50000 totalMs=22741 loadMs=1164 writeMs=20745 fileSizeKb=38901.67 heapBeforeMb=31.33 heapAfterMb=438.12 heapAfterForcedGcMb=34.53 heapTransientMb=406.79 heapRetainedMb=3.2 nonHeapBeforeMb=103.62 nonHeapAfterMb=117.75 gcCountDuringStreaming=52 gcTimeMsDuringStreaming=1059
```

## Phân tích đúng theo cấu hình mới

## 1. `loadMs` không phải benchmark DB thuần

- FastExcel: `loadMs = 1875ms`
- POI SXSSF: `loadMs = 1164ms`

Sự khác biệt này không có nghĩa POI query DB tốt hơn. Repository load là giống nhau. `loadMs` ở đây đã bị ảnh hưởng bởi:

- trạng thái heap hiện tại
- GC đang chạy hay không
- allocation pressure do writer tạo ra

Vì vậy không nên đọc `loadMs` như số đo thuần của database.

## 2. Cả hai bên đều đang chịu allocation pressure từ batch `50.000`

Ở cả hai log đều thấy:

- nhiều batch tăng thêm `~52MB`
- có batch delta âm lớn khi GC vừa reclaim

Điều đó cho thấy:

- batch `50.000` vẫn là một cục object lớn
- heap không phẳng hoàn toàn
- memory behavior hiện tại phản ánh cả reader lẫn writer

## 3. FastExcel hiện tại đã được tune tương đối tốt

Kết quả FastExcel:

- `totalMs = 8072ms`
- `writeMs = 5875ms`
- `heapTransientMb = 335.82MB`
- `heapRetainedMb = 0.85MB`

Ý nghĩa:

- throughput vẫn rất mạnh
- profile memory tốt hơn rất nhiều so với FastExcel chưa tune
- transient heap đã được kéo xuống mức chấp nhận được
- không có dấu hiệu leak dài hạn

Tuning đang có tác dụng thật:

- `sheet.flush()` giúp xả row state sớm hơn ở tầng worksheet
- `inlineString(...)` giúp giảm shared strings table cho các cột gần như unique

## 4. POI SXSSF không còn có lợi thế RAM như phân tích cũ

Kết quả POI:

- `totalMs = 22741ms`
- `writeMs = 20745ms`
- `heapTransientMb = 406.79MB`
- `heapRetainedMb = 3.2MB`

Điểm quan trọng nhất:

- với `POI_ROW_WINDOW_SIZE = 50.000`, SXSSF không còn giữ một cửa sổ nhỏ vài trăm row nữa
- nó đang giữ gần trọn một batch row trong memory
- vì vậy transient heap của POI tăng mạnh và không còn thấp hơn FastExcel

Nói cách khác:

- SXSSF vẫn là thư viện streaming
- nhưng cách cấu hình hiện tại đã làm mất phần lớn lợi thế memory vốn có của nó

## So sánh trực tiếp theo số liệu mới

## 1. Thời gian tổng thể

- FastExcel: `8072ms`
- POI SXSSF: `22741ms`

FastExcel nhanh hơn khoảng `2.82 lần`.

## 2. Thời gian ghi

- FastExcel: `5875ms`
- POI SXSSF: `20745ms`

FastExcel nhanh hơn khoảng `3.53 lần` ở pha write.

## 3. Peak heap tạm thời

- FastExcel: `335.82MB`
- POI SXSSF: `406.79MB`

FastExcel hiện thấp hơn POI khoảng `70.97MB`, tương đương thấp hơn khoảng `17.4%`.

Đây là thay đổi kết luận quan trọng nhất so với phân tích cũ.

## 4. Heap sau khi benchmark kết thúc, trước forced GC

- FastExcel: `366.33MB`
- POI SXSSF: `438.12MB`

FastExcel cũng thấp hơn ở mốc này.

## 5. Retained heap sau forced GC

- FastExcel: `0.85MB`
- POI SXSSF: `3.2MB`

Cả hai đều rất thấp. Không bên nào có dấu hiệu leak đáng kể.

## 6. GC count

- FastExcel: `54`
- POI SXSSF: `52`

Số lần GC gần như tương đương.

## 7. GC time

- FastExcel: `483ms`
- POI SXSSF: `1059ms`

POI cao hơn khoảng `2.19 lần`. Điều này cho thấy workload SXSSF với window lớn đang nặng hơn đối với JVM.

## 8. Kích thước file

- FastExcel: `43256.91KB`
- POI SXSSF: `38901.67KB`

File FastExcel lớn hơn khoảng `11.2%`. Đây là trade-off hợp lý khi dùng `inlineString(...)` cho cột cardinality cao để đổi lấy memory tốt hơn.

## Vì sao `row window = batch size` làm POI đổi kết quả

Khi `POI_ROW_WINDOW_SIZE = 50.000` và batch cũng là `50.000`, mô hình thực tế của POI gần như là:

1. load một batch `50.000` row vào memory
2. tạo `50.000` row SXSSF trong cửa sổ in-memory
3. sau đó mới `flushRows(50.000)`

Điều này làm mất lợi thế memory lớn nhất của SXSSF:

- giữ row window nhỏ
- flush sớm xuống temp storage

Nói gọn hơn:

- bản chất của SXSSF vẫn tốt cho streaming
- nhưng cấu hình hiện tại đang khiến SXSSF hành xử gần hơn với "giữ cả batch lớn trong memory rồi mới flush"

Vì vậy việc POI không còn nhẹ RAM là hoàn toàn hợp logic.

## Phần nào của kết luận cũ còn đúng, phần nào không còn đúng

### Còn đúng

- FastExcel rất mạnh về tốc độ ghi
- `sheet.flush()` có ích thật
- `inlineString(...)` giúp giảm memory rõ rệt
- cả hai đều không có dấu hiệu leak lớn sau forced GC
- benchmark hiện tại vẫn chưa phải cursor streaming thật sự

### Không còn đúng

- "POI SXSSF nhẹ heap hơn FastExcel rõ rệt"
- "POI an toàn memory hơn hẳn trong benchmark hiện tại"
- "POI chỉ giữ một row window nhỏ trong memory"

Những kết luận đó chỉ đúng khi `POI_ROW_WINDOW_SIZE` nhỏ, không đúng khi bạn tăng nó lên bằng `50.000`.

## Kết luận cập nhật

Với cấu hình hiện tại:

- `BATCH_SIZE = 50.000`
- `POI_ROW_WINDOW_SIZE = 50.000`
- FastExcel đã tune bằng `sheet.flush()` và `inlineString(...)`

thì kết quả thực tế là:

- FastExcel nhanh hơn rõ rệt
- FastExcel cũng đang tốt hơn về transient heap
- POI không còn lợi thế memory vì row window đã bị nâng lên ngang batch size

Nói ngắn gọn:

- nếu tiếp tục giữ `POI_ROW_WINDOW_SIZE = 50.000`, FastExcel đang thắng cả về tốc độ lẫn memory profile
- nếu muốn đánh giá đúng lợi thế vốn có của SXSSF về memory, cần giảm `POI_ROW_WINDOW_SIZE` xuống nhỏ hơn nhiều, ví dụ `100`, `500`, hoặc `1000`

## Kết luận thực dụng

Trong benchmark hiện tại, kết luận hợp lý nhất là:

- `FastExcel tuned` là lựa chọn mạnh hơn rõ ràng
- `POI SXSSF` chỉ thực sự có lợi thế memory khi được cấu hình với row window nhỏ
- nếu window bị đẩy lên bằng batch size, SXSSF đánh mất ưu thế memory chính của nó nhưng vẫn giữ nhược điểm lớn về tốc độ

Vì vậy, khi so sánh công bằng cần tách rõ hai câu hỏi:

1. So thư viện theo cấu hình thực tế sẽ dùng trong production là gì?
2. So khả năng tối đa của từng thư viện khi được tune đúng bản chất của nó là gì?

Trong cấu hình hiện tại của bạn, FastExcel đang là đáp án tốt hơn.
