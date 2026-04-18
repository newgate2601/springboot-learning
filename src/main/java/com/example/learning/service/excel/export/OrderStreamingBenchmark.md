# Phân Tích Benchmark Streaming Với 1.000.000 Record

Tài liệu này phân tích benchmark streaming mới cho export `1.000.000` record `orders`, dùng:

- `FastExcel` với ghi trực tiếp ra `OutputStream`
- `Apache POI SXSSF` với cửa sổ row nhỏ để flush xuống disk theo cơ chế streaming

Khác với benchmark non-streaming trước đó, luồng này đã chuyển sang mô hình:

1. Đọc dữ liệu theo batch `50.000` record
2. Ghi dần vào workbook theo từng batch
3. Trả response bằng `StreamingResponseBody`
4. Không giữ toàn bộ `List<OrderExportRow>` của toàn dataset trong heap cùng lúc

## Bối cảnh benchmark

Luồng mới là streaming theo kiểu `batch-read + streaming-write`, cụ thể:

- Query theo keyset pagination dựa trên `id`
- Mỗi lần load `50.000` row
- Sau mỗi batch thì ghi ngay vào file Excel
- Cuối cùng stream file trực tiếp ra client

Điều này loại bỏ chi phí nền rất lớn của mô hình `load-all + write-all`.

Tuy nhiên, đây chưa phải là kiểu stream trực tiếp từng row từ JDBC cursor sang socket. Thực tế hiện tại vẫn còn:

- giữ `50.000` row của batch hiện tại trong memory
- workbook/writer vẫn có state nội bộ riêng của từng thư viện
- file `.xlsx` là định dạng zip/xml nên một phần chi phí finalize vẫn dồn về cuối

## Log gốc

### FastExcel streaming ban đầu

Các batch load đầu và cuối:

```text
order-streaming load batchStartAfterId=null rows=50000 loadMs=333 heapBeforeLoadMb=36.41 heapAfterLoadMb=86.0 heapLoadDeltaMb=49.59 gcCountDuringLoad=5 gcTimeMsDuringLoad=20
...
order-streaming load batchStartAfterId=950000 rows=50000 loadMs=47 heapBeforeLoadMb=794.23 heapAfterLoadMb=848.23 heapLoadDeltaMb=54.0 gcCountDuringLoad=0 gcTimeMsDuringLoad=0
order-streaming load batchStartAfterId=1000000 rows=0 loadMs=1 heapBeforeLoadMb=886.23 heapAfterLoadMb=886.23 heapLoadDeltaMb=0.0 gcCountDuringLoad=0 gcTimeMsDuringLoad=0
```

Tổng benchmark:

```text
order-streaming benchmark library=fastexcel-streaming expectedRows=1000000 streamedRows=1000000 batches=20 batchSize=50000 totalMs=7114 loadMs=1597 writeMs=5344 fileSizeKb=40518.53 heapBeforeMb=30.41 heapAfterMb=1418.04 heapAfterForcedGcMb=31.26 heapTransientMb=1387.63 heapRetainedMb=0.85 nonHeapBeforeMb=103.44 nonHeapAfterMb=108.38 gcCountDuringStreaming=28 gcTimeMsDuringStreaming=468
```

### FastExcel streaming sau `sheet.flush()` mỗi batch

Tổng benchmark:

```text
order-streaming benchmark library=fastexcel-streaming expectedRows=1000000 streamedRows=1000000 batches=20 batchSize=50000 totalMs=6718 loadMs=1570 writeMs=4966 fileSizeKb=40518.53 heapBeforeMb=30.4 heapAfterMb=730.43 heapAfterForcedGcMb=31.26 heapTransientMb=700.03 heapRetainedMb=0.86 nonHeapBeforeMb=103.54 nonHeapAfterMb=108.59 gcCountDuringStreaming=25 gcTimeMsDuringStreaming=349
```

### FastExcel streaming sau `sheet.flush()` và `inlineString(...)`

Các cột được đổi sang `inlineString(...)`:

- `orderNo`
- `orderDate`
- `createdAt`
- `updatedAt`

Riêng `status` vẫn giữ shared string vì cardinality thấp.

Tổng benchmark:

```text
order-streaming benchmark library=fastexcel-streaming expectedRows=1000000 streamedRows=1000000 batches=20 batchSize=50000 totalMs=6875 loadMs=1486 writeMs=5216 fileSizeKb=43256.91 heapBeforeMb=30.5 heapAfterMb=260.5 heapAfterForcedGcMb=31.35 heapTransientMb=230.01 heapRetainedMb=0.85 nonHeapBeforeMb=103.19 nonHeapAfterMb=107.98 gcCountDuringStreaming=35 gcTimeMsDuringStreaming=312
```

### POI SXSSF streaming

Các batch load đầu và cuối:

```text
order-streaming load batchStartAfterId=null rows=50000 loadMs=72 heapBeforeLoadMb=60.92 heapAfterLoadMb=80.42 heapLoadDeltaMb=19.49 gcCountDuringLoad=2 gcTimeMsDuringLoad=14
...
order-streaming load batchStartAfterId=950000 rows=50000 loadMs=43 heapBeforeLoadMb=147.04 heapAfterLoadMb=199.04 heapLoadDeltaMb=52.0 gcCountDuringLoad=0 gcTimeMsDuringLoad=0
order-streaming load batchStartAfterId=1000000 rows=0 loadMs=1 heapBeforeLoadMb=156.6 heapAfterLoadMb=156.6 heapLoadDeltaMb=0.0 gcCountDuringLoad=0 gcTimeMsDuringLoad=0
```

Tổng benchmark:

```text
order-streaming benchmark library=poi-sxssf expectedRows=1000000 streamedRows=1000000 batches=20 batchSize=50000 totalMs=22546 loadMs=1115 writeMs=20851 fileSizeKb=38901.67 heapBeforeMb=31.26 heapAfterMb=168.6 heapAfterForcedGcMb=34.44 heapTransientMb=137.35 heapRetainedMb=3.19 nonHeapBeforeMb=105.08 nonHeapAfterMb=117.64 gcCountDuringStreaming=81 gcTimeMsDuringStreaming=431
```

## Phân tích pha load

### 1. Cả hai bên đều load đúng `20` batch

- Tổng row: `1.000.000`
- Batch size: `50.000`
- Số batch thực tế: `20`

Điều này xác nhận cơ chế đọc theo batch đang hoạt động đúng như thiết kế.

### 2. POI có tổng thời gian load thấp hơn FastExcel

- FastExcel: `loadMs = 1597ms`
- POI SXSSF: `loadMs = 1115ms`

POI nhanh hơn khoảng `482ms`, tương đương nhanh hơn khoảng `30%` ở riêng pha load.

Nhưng cần đọc kết quả này đúng ngữ cảnh: repository load của hai bên là giống nhau, nên khác biệt này không phản ánh database tốt hơn hay query tốt hơn. Nó chủ yếu phản ánh rằng trong quá trình benchmark thực tế:

- FastExcel giữ nhiều heap tạm hơn trong suốt quá trình ghi
- GC và allocation pressure từ writer có thể làm nhiễu nhịp độ của các lần load tiếp theo

Nói ngắn gọn: `loadMs` ở benchmark streaming không còn là chỉ số DB thuần túy, mà là load dưới ảnh hưởng của writer cùng tiến trình.

### 3. Dấu hiệu heap của FastExcel cho thấy dữ liệu batch cũ không được giải phóng ngay

Ở FastExcel:

- heap tăng dần khá đều theo các batch sau
- nhiều batch có `heapLoadDeltaMb` khoảng `52MB`
- trước batch cuối cùng, heap đã lên vùng `~886MB`

Mẫu tăng này cho thấy dù ứng dụng chỉ load từng batch `50.000` row, phần memory do writer nội bộ của FastExcel vẫn tích lũy dần trong quá trình build workbook.

Điều này khác hẳn kỳ vọng của một pipeline stream "gần như phẳng" về heap.

### 4. Dấu hiệu heap của POI SXSSF ổn định hơn rõ rệt

Ở POI SXSSF:

- heap dao động lên xuống giữa các batch
- nhiều batch có `heapLoadDeltaMb` âm
- vùng heap chủ yếu nằm quanh `100MB` đến `200MB`

Đây là dấu hiệu của mô hình streaming ổn định hơn:

- batch cũ được GC thu hồi tương đối đều
- row đã flush ra temp storage nên không bị giữ lại nhiều trên heap

Về mặt vận hành JVM, đây là profile lành mạnh hơn hẳn FastExcel trong benchmark này.

## Phân tích tổng thể FastExcel streaming

### 1. Phiên bản ban đầu rất nhanh nhưng heap rất cao

- `totalMs = 7114ms`
- `writeMs = 5344ms`

FastExcel hoàn thành toàn bộ export `1.000.000` row chỉ trong khoảng `7.1 giây`.

Nếu chỉ nhìn throughput, đây là kết quả rất mạnh.

### 2. Chi phí write thấp hơn POI rất nhiều

- FastExcel `writeMs = 5344ms`
- POI SXSSF `writeMs = 20851ms`

FastExcel nhanh hơn khoảng `3.9 lần` ở pha write.

Đây là khác biệt lớn nhất của benchmark.

### 3. Heap transient rất cao

- `heapBeforeMb = 30.41MB`
- `heapAfterMb = 1418.04MB`
- `heapTransientMb = 1387.63MB`

Điểm này rất đáng chú ý. Dù đây là benchmark streaming, FastExcel vẫn đẩy heap tạm thời lên hơn `1.4GB`.

Điều đó nói rằng:

- FastExcel không phải streaming theo nghĩa "heap gần như phẳng"
- thư viện này vẫn giữ một lượng state/structure khá lớn trong quá trình build workbook
- lợi thế chính của nó nằm ở tốc độ write, không nằm ở peak heap thấp

### 4. Retained heap sau forced GC gần như bằng 0

- `heapAfterForcedGcMb = 31.26MB`
- `heapRetainedMb = 0.85MB`

Đây là điểm tốt. Nó cho thấy phần lớn `1.38GB` chỉ là memory tạm trong lúc ghi, không phải leak dài hạn.

### 5. GC pressure ở mức vừa phải

- `gcCountDuringStreaming = 28`
- `gcTimeMsDuringStreaming = 468ms`

GC time chiếm khoảng `6.6%` của tổng thời gian chạy (`468 / 7114`).

Mức này không quá xấu với throughput đạt được, nhưng vẫn là dấu hiệu có áp lực cấp phát đáng kể.

### 6. `sheet.flush()` giảm mạnh peak heap

Sau khi thêm `sheet.flush()` sau mỗi batch `50.000` row:

- `heapTransientMb`: từ `1387.63MB` xuống `700.03MB`
- `heapAfterMb`: từ `1418.04MB` xuống `730.43MB`
- `writeMs`: từ `5344ms` xuống `4966ms`
- `totalMs`: từ `7114ms` xuống `6718ms`

Điều này xác nhận một phần memory cao trước đó đến từ row state của worksheet chưa được flush sớm.

Điểm quan trọng là `OutputStream.flush()` không giải quyết được việc này. Tác dụng đến từ `sheet.flush()` ở tầng FastExcel, không phải ở tầng socket/output stream.

### 7. `inlineString(...)` giải quyết phần còn lại của shared strings

Sau khi giữ `status` ở shared string và chuyển các cột cardinality cao sang `inlineString(...)`:

- `heapTransientMb`: từ `700.03MB` xuống `230.01MB`
- `heapAfterMb`: từ `730.43MB` xuống `260.5MB`
- `gcTimeMsDuringStreaming`: từ `349ms` xuống `312ms`
- `writeMs`: từ `4966ms` tăng nhẹ lên `5216ms`
- `fileSizeKb`: từ `40518.53KB` tăng lên `43256.91KB`

Kết quả này cho thấy phần memory lớn còn lại chủ yếu đến từ shared strings table của các giá trị gần như unique:

- `orderNo`
- `orderDate`
- `createdAt`
- `updatedAt`

Đây là trade-off hợp lý:

- heap giảm rất mạnh
- thời gian ghi tăng nhẹ
- file lớn hơn một chút

Trong thực tế production, đây thường là đánh đổi tốt hơn nhiều so với việc giữ peak heap hàng trăm MB đến hơn 1GB.

## Phân tích tổng thể POI SXSSF streaming

### 1. Thời gian tổng thể chậm hơn rõ rệt

- `totalMs = 22546ms`
- `writeMs = 20851ms`

POI SXSSF mất khoảng `22.5 giây` để export `1.000.000` row, chậm hơn FastExcel khoảng `15.4 giây`.

### 2. Ưu điểm lớn nhất là heap rất thấp

- `heapBeforeMb = 31.26MB`
- `heapAfterMb = 168.6MB`
- `heapTransientMb = 137.35MB`

So với FastExcel, heap transient của POI SXSSF thấp hơn khoảng `10 lần`.

Đây là dấu hiệu rất đúng với bản chất `SXSSF`:

- chỉ giữ một cửa sổ row nhỏ trong memory
- phần lớn dữ liệu row được flush ra temp file
- heap của JVM được bảo vệ tốt hơn

### 3. Retained heap sau forced GC vẫn thấp

- `heapAfterForcedGcMb = 34.44MB`
- `heapRetainedMb = 3.19MB`

Tương tự FastExcel, SXSSF cũng không có dấu hiệu giữ lại memory đáng kể sau khi export xong.

### 4. GC count cao nhưng GC time không lớn

- `gcCountDuringStreaming = 81`
- `gcTimeMsDuringStreaming = 431ms`

POI có số lần GC cao hơn FastExcel rất nhiều, nhưng tổng thời gian GC lại gần tương đương:

- FastExcel: `468ms`
- POI SXSSF: `431ms`

Điều này cho thấy:

- POI tạo nhiều đợt rác nhỏ hơn
- JVM thu gom thường xuyên nhưng mỗi lần không quá tốn

Nút thắt chính của POI không phải GC, mà là bản thân tốc độ ghi workbook chậm hơn.

## So sánh trực tiếp

### FastExcel qua 3 phiên bản

| Phiên bản | totalMs | writeMs | heapTransientMb | heapAfterMb | gcTimeMs | fileSizeKb |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Ban đầu | 7114 | 5344 | 1387.63 | 1418.04 | 468 | 40518.53 |
| Sau `sheet.flush()` | 6718 | 4966 | 700.03 | 730.43 | 349 | 40518.53 |
| Sau `sheet.flush()` + `inlineString(...)` | 6875 | 5216 | 230.01 | 260.5 | 312 | 43256.91 |

Nhìn theo từng bước tối ưu:

- `sheet.flush()` giảm khoảng `49.6%` transient heap và còn cải thiện cả thời gian ghi
- `inlineString(...)` giảm tiếp khoảng `67.1%` transient heap so với bản đã `flush`
- so với bản FastExcel ban đầu, bản tuned cuối cùng giảm khoảng `83.4%` transient heap

### Thời gian tổng thể

- FastExcel tuned cuối: `6875ms`
- POI SXSSF: `22546ms`

FastExcel tuned cuối nhanh hơn khoảng `3.28 lần`.

### Thời gian write

- FastExcel tuned cuối: `5216ms`
- POI SXSSF: `20851ms`

FastExcel tuned cuối nhanh hơn khoảng `4 lần`.

### Thời gian load

- FastExcel tuned cuối: `1486ms`
- POI SXSSF: `1115ms`

POI nhanh hơn khoảng `1.43 lần` ở chỉ số `loadMs`, nhưng đây không phải lợi thế DB thực sự mà chỉ là hệ quả của profile memory/GC khác nhau trong cùng một tiến trình benchmark.

### Peak heap tạm thời

- FastExcel tuned cuối: `230.01MB`
- POI SXSSF: `137.35MB`

POI SXSSF chỉ còn thấp hơn khoảng `1.67 lần`.

### Retained heap sau forced GC

- FastExcel tuned cuối: `0.85MB`
- POI SXSSF: `3.19MB`

Cả hai đều rất thấp. Không bên nào cho thấy dấu hiệu leak đáng kể sau khi export hoàn tất.

### GC count

- FastExcel tuned cuối: `35`
- POI SXSSF: `81`

POI GC nhiều hơn khoảng `2.3 lần`.

### GC time

- FastExcel tuned cuối: `312ms`
- POI SXSSF: `431ms`

FastExcel tuned cuối còn thấp hơn POI. GC không phải nguyên nhân chính lý giải chênh lệch lớn về tổng thời gian.

### Kích thước file

- FastExcel tuned cuối: `43256.91KB`
- POI SXSSF: `38901.67KB`

File FastExcel tuned cuối lớn hơn khoảng `4355.24KB`, tương đương khoảng `11.2%`.

Chênh lệch này không lớn, nhưng cho thấy hai thư viện serialize `.xlsx` khác nhau đôi chút.

## Ý nghĩa kỹ thuật

### 1. Streaming đã giải quyết triệt để bài toán retained heap

So với mô hình non-streaming trước đó, điểm cải thiện lớn nhất là:

- không còn giữ toàn bộ `1.000.000` row trong một `List`
- không còn trả file dưới dạng `byte[]`
- heap sau forced GC quay gần về baseline

Đây là cải thiện kiến trúc quan trọng nhất.

### 2. Nhưng không phải thư viện nào cũng stream "nhẹ heap" như nhau nếu chưa tune đúng

Kết quả này cho thấy streaming ở tầng ứng dụng không đồng nghĩa với peak heap thấp tuyệt đối.

- `SXSSF` stream chậm hơn nhưng heap ổn định
- `FastExcel` nếu để mặc định có thể vẫn giữ peak heap rất cao
- `FastExcel` khi flush worksheet và tránh shared strings cho cột unique thì profile heap cải thiện rất rõ

Vì vậy, cần tách bạch hai khái niệm:

- `streaming API design`
- `memory-efficient writer implementation`

Một thư viện có thể phù hợp về API streaming nhưng vẫn giữ state nội bộ lớn.

### 3. Chọn thư viện phụ thuộc mục tiêu vận hành

Nếu ưu tiên lớn nhất là:

- tốc độ export
- throughput cao
- chấp nhận cấp heap lớn hơn

thì FastExcel đang thắng rõ.

Nếu ưu tiên lớn nhất là:

- kiểm soát peak heap
- an toàn khi chạy cùng nhiều request khác
- giảm rủi ro OOM trong JVM nhỏ

thì POI SXSSF an toàn hơn đáng kể.

### 4. FastExcel sau tuning đã tiến gần hơn đến điểm cân bằng giữa nhanh và nhẹ

Kết quả thực tế cho thấy FastExcel mặc định không hẳn vừa nhanh vừa nhẹ heap. Nhưng sau khi tune:

Trong dataset `1.000.000` row:

- nó vẫn nhanh vượt trội
- transient heap giảm từ `1387.63MB` xuống còn `230.01MB`
- khoảng cách memory với POI SXSSF thu hẹp rất mạnh

Đây là điểm rất quan trọng khi đưa vào production.

## Kết luận kỹ thuật

### 1. Cả hai cách đều đã đạt được streaming end-to-end ở mức ứng dụng

Luồng mới đã bỏ được hai nút thắt lớn của bản non-streaming:

- `load-all`
- `response byte[]`

### 2. FastExcel tuned là lựa chọn rất mạnh nếu cần cả tốc độ lẫn memory chấp nhận được

Với `1.000.000` record:

- total nhanh hơn khoảng `3.28 lần`
- write nhanh hơn khoảng `4 lần`
- transient heap chỉ còn cao hơn POI khoảng `92.66MB`

Nếu hệ thống có đủ heap, FastExcel cho hiệu năng tổng thể tốt hơn rõ rệt.

### 3. POI SXSSF là lựa chọn tốt hơn nếu mục tiêu là ổn định memory

POI chậm hơn nhiều, nhưng:

- heap transient thấp hơn khoảng `10 lần`
- profile heap ổn định hơn qua từng batch
- ít rủi ro hơn khi chạy trên JVM giới hạn memory

### 4. Quyết định cuối cùng nên dựa trên ràng buộc production

Nếu production của anh:

- heap rộng
- số request export đồng thời thấp
- ưu tiên thời gian trả file

thì FastExcel là lựa chọn hợp lý hơn.

Nếu production của anh:

- heap bị giới hạn
- nhiều request export có thể chạy song song
- cần bảo vệ JVM khỏi peak memory lớn

thì POI SXSSF đáng tin hơn.

## Kết luận ngắn gọn

Với benchmark streaming `1.000.000` record sau tuning:

- FastExcel hoàn thành trong `6.9 giây`, rất nhanh, với transient heap khoảng `230MB`
- POI SXSSF mất `22.5 giây`, chậm hơn nhiều, với transient heap khoảng `137MB`
- Cả hai đều giải phóng memory tốt sau khi hoàn thành
- Khác biệt lớn nhất lúc này không còn là chênh lệch heap cực lớn, mà là trade-off giữa `throughput`, `peak heap`, và `file size`

Kết luận thực dụng là:

- `FastExcel tuned` thắng rất rõ về tốc độ và đã cải thiện mạnh về memory
- `POI SXSSF` vẫn thắng về độ an toàn bộ nhớ tuyệt đối
