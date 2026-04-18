# Phân Tích Benchmark Non-Streaming Với 300.000 Record

Tài liệu này chỉ giữ lại phần phân tích sâu cho benchmark `300.000` record.

## Bối cảnh benchmark

Service hiện tại dùng mô hình `load-all + write-all`:

1. Query toàn bộ dữ liệu `OrderExportRow` từ database vào `List<OrderExportRow>`
2. Gọi `entityManager.clear()` để giảm ảnh hưởng của persistence context
3. Ghi Excel vào `ByteArrayOutputStream`
4. Trả file qua `ResponseEntity<byte[]>`

Điều đó có nghĩa là benchmark này **chưa phải streaming end-to-end**. Dữ liệu vẫn được giữ trong JVM ở hai lớp chính:

- Danh sách `List<OrderExportRow>` sau khi query
- `byte[]` kết quả sau khi serialize file Excel

Sự khác biệt giữa hai thư viện chủ yếu nằm ở chi phí tạm thời phát sinh trong **pha write**.

## Log gốc

### POI

Pha load:

```text
order-export load rows=300000 loadMs=886 heapBeforeLoadMb=33.43 heapAfterLoadMb=305.5 heapAfterLoadForcedGcMb=181.01 heapLoadTransientMb=272.07 heapLoadRetainedMb=147.58 gcCountDuringLoad=17 gcTimeMsDuringLoad=134
```

Pha write:

```text
order-export benchmark library=poi rows=300000 writeMs=19233 fileSizeKb=10737.64 heapBeforeWriteMb=181.01 heapAfterWriteMb=3081.24 heapAfterWriteForcedGcMb=193.02 heapWriteTransientMb=2900.23 heapWriteRetainedMb=12.01 nonHeapBeforeWriteMb=119.91 nonHeapAfterWriteMb=120.77 gcCountDuringWrite=44 gcTimeMsDuringWrite=1650
```

### FastExcel

Pha load:

```text
order-export load rows=300000 loadMs=938 heapBeforeLoadMb=33.44 heapAfterLoadMb=373.04 heapAfterLoadForcedGcMb=180.81 heapLoadTransientMb=339.6 heapLoadRetainedMb=147.37 gcCountDuringLoad=17 gcTimeMsDuringLoad=112
```

Pha write:

```text
order-export benchmark library=fastexcel rows=300000 writeMs=1771 fileSizeKb=12035.62 heapBeforeWriteMb=180.81 heapAfterWriteMb=368.28 heapAfterWriteForcedGcMb=192.87 heapWriteTransientMb=187.47 heapWriteRetainedMb=12.06 nonHeapBeforeWriteMb=104.66 nonHeapAfterWriteMb=104.94 gcCountDuringWrite=13 gcTimeMsDuringWrite=79
```

## Phân tích pha load

### 1. Thời gian load gần như tương đương

- POI: `886ms`
- FastExcel: `938ms`

Chênh lệch này nhỏ. Điều đó cho thấy phần truy vấn database và map sang `OrderExportRow` không phải là nguyên nhân chính tạo ra khác biệt lớn ở benchmark tổng thể.

### 2. Nền heap trước khi write gần như giống nhau

Sau khi forced GC ở cuối pha load:

- POI: `heapAfterLoadForcedGcMb = 181.01MB`
- FastExcel: `heapAfterLoadForcedGcMb = 180.81MB`

Điểm này rất quan trọng. Nó cho thấy trước khi bước vào pha ghi Excel, cả hai thư viện đang xuất phát trên một mặt bằng heap gần như giống hệt nhau.

Nói cách khác, sự khác biệt lớn ở phía sau gần như đến từ **cách thư viện tạo file Excel**, không đến từ database hoặc JPA.

### 3. Chi phí nền của mô hình load-all là rất rõ

`heapLoadRetainedMb` ở cả hai bên đều khoảng `147MB`.

Điều này phản ánh chi phí nền của việc giữ `300.000` dòng trong `List<OrderExportRow>` trên heap JVM. Đây là phần chi phí mà cả POI lẫn FastExcel đều phải gánh như nhau trong kiến trúc hiện tại.

## Phân tích pha write của POI

### 1. Thời gian ghi rất lớn

- `writeMs = 19233ms`

Tức là chỉ riêng pha ghi file đã mất hơn `19 giây`.

### 2. Peak heap tăng đột biến

- `heapBeforeWriteMb = 181.01MB`
- `heapAfterWriteMb = 3081.24MB`
- `heapWriteTransientMb = 2900.23MB`

Đây là dấu hiệu rất điển hình của `XSSFWorkbook` non-streaming. Thư viện này dựng một workbook object model rất lớn trong memory:

- row object
- cell object
- shared strings hoặc string-related structures
- XML object graph cần để build workbook

Toàn bộ các object này làm heap tăng vọt lên hơn `3GB`.

### 3. GC bị kéo vào rất mạnh

- `gcCountDuringWrite = 44`
- `gcTimeMsDuringWrite = 1650ms`

Riêng thời gian GC đã chiếm khoảng `1.65 giây`. Điều này cho thấy JVM phải liên tục thu gom rác để giữ cho tiến trình tiếp tục chạy. Một phần đáng kể của tổng thời gian `19.2 giây` là trả giá cho áp lực cấp phát object và GC churn.

### 4. Memory tăng mạnh nhưng không giữ lại lâu

- `heapAfterWriteForcedGcMb = 193.02MB`
- `heapWriteRetainedMb = 12.01MB`

Điểm này rất quan trọng để đọc log đúng. `XSSFWorkbook` không nhất thiết bị leak lớn sau khi hoàn tất ghi file. Phần lớn `~2.9GB` tăng thêm là memory tạm trong lúc build workbook và serialize file.

Tuy nhiên, điều đó **không làm mô hình này an toàn hơn trong thực tế**. Vì trong suốt quá trình write, JVM vẫn phải:

- có đủ heap để chứa đỉnh bộ nhớ đó
- chịu nhiều đợt GC
- chấp nhận độ trễ rất lớn

## Phân tích pha write của FastExcel

### 1. Thời gian ghi ngắn hơn rất nhiều

- `writeMs = 1771ms`

So với POI, FastExcel nhanh hơn khoảng `10.9 lần` ở pha write.

### 2. Heap tăng nhưng ở mức kiểm soát được hơn nhiều

- `heapBeforeWriteMb = 180.81MB`
- `heapAfterWriteMb = 368.28MB`
- `heapWriteTransientMb = 187.47MB`

FastExcel vẫn dùng heap JVM, nhưng phần memory tạm phát sinh trong lúc ghi thấp hơn rất xa so với `XSSFWorkbook`.

### 3. GC pressure thấp hơn rõ rệt

- `gcCountDuringWrite = 13`
- `gcTimeMsDuringWrite = 79ms`

Điều này xác nhận rằng FastExcel tạo ít object tạm và ít áp lực lên GC hơn nhiều.

### 4. Retained heap sau GC gần như giống POI

- `heapAfterWriteForcedGcMb = 192.87MB`
- `heapWriteRetainedMb = 12.06MB`

Điểm đáng chú ý là retained heap sau forced GC gần như bằng POI. Điều này cho thấy khác biệt không nằm ở memory còn giữ lại lâu dài sau khi ghi xong, mà nằm ở **chi phí tạm thời trong lúc dựng và serialize workbook**.

## So sánh trực tiếp

### Thời gian write

- POI: `19233ms`
- FastExcel: `1771ms`

FastExcel nhanh hơn khoảng `10.9 lần`.

### Heap tạm thời tăng thêm khi write

- POI: `2900.23MB`
- FastExcel: `187.47MB`

POI cao hơn khoảng `15.5 lần`.

### GC time trong pha write

- POI: `1650ms`
- FastExcel: `79ms`

POI cao hơn khoảng `21 lần`.

### Retained heap sau forced GC

- POI: `12.01MB`
- FastExcel: `12.06MB`

Hai bên gần như tương đương.

## Kết luận kỹ thuật

### 1. Pha load không phải vấn đề chính trong benchmark này

Ở mức `300.000` record, load phase của hai bên gần như ngang nhau. Nền heap sau load cũng gần như giống nhau.

### 2. Điểm khác biệt thật sự nằm ở pha write

- `XSSFWorkbook` tạo workbook object model rất lớn trong memory
- FastExcel tạo ít memory tạm hơn rất nhiều

Vì vậy, khác biệt lớn nhất nằm ở:

- peak heap
- số lần GC
- thời gian write

### 3. POI non-streaming không phù hợp cho export lớn trong thực tế

`XSSFWorkbook` ở mốc `300.000` record không chỉ chậm hơn, mà còn bước vào vùng rủi ro vận hành:

- cần heap lớn
- dễ gây pause do GC
- dễ ảnh hưởng request khác nếu chạy chung JVM
- tổng thời gian phản hồi rất dài

### 4. FastExcel hiệu quả hơn rất nhiều trong cùng mô hình load-all

FastExcel chưa phải streaming end-to-end, nhưng trong cùng kiến trúc `load-all + write-all`, nó vận hành hiệu quả hơn POI non-streaming rất rõ ràng.

### 5. Benchmark này vẫn chưa phải benchmark streaming

Dù FastExcel cho kết quả tốt hơn nhiều, code hiện tại vẫn đang:

- load toàn bộ dữ liệu vào JVM
- giữ file output trong `byte[]`

Vì vậy bước tiếp theo hợp lý là benchmark streaming thật sự, nơi:

- dữ liệu không cần nằm hết trong một `List`
- file không cần nằm hết trong một `byte[]`
- response được đẩy dần ra client

## Kết luận ngắn gọn

Với `300.000` record trong mô hình non-streaming hiện tại:

- Chi phí nền do `load-all` là khoảng `181MB` heap sau load
- `XSSFWorkbook` tạo thêm gần `2.9GB` heap tạm và mất hơn `19 giây` để ghi
- FastExcel chỉ tạo thêm khoảng `187MB` heap tạm và mất khoảng `1.8 giây` để ghi
- Retained heap sau GC của hai bên gần như bằng nhau

Điều này cho thấy vấn đề lớn nhất của POI non-streaming không phải là memory bị giữ lại lâu dài, mà là **peak heap cực lớn và GC pressure rất nặng trong lúc ghi file**.
