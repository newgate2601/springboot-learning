https://github.com/dhatim/fastexcel/

# Fastexcel Usage Guide

Tài liệu này tóm tắt cách dùng `fastexcel` để export Excel số lượng lớn trong Spring Boot, đồng thời giải thích rõ các điểm dễ hiểu nhầm về `streaming`, `Worksheet.flush()`, `OutputStream.flush()`, và tối ưu RAM.

## 1. Fastexcel là gì

`fastexcel` là thư viện ghi file `.xlsx` tập trung vào:

- tốc độ ghi nhanh
- heap thấp hơn so với `XSSFWorkbook` non-streaming của Apache POI
- API đơn giản cho các case export dữ liệu lớn

Nó phù hợp khi:

- cần export nhiều dòng
- không cần quá nhiều feature Excel nâng cao
- muốn giảm chi phí heap so với mô hình workbook object graph lớn

## 2. Streaming trong Fastexcel nghĩa là gì

Khi dùng `fastexcel`, dữ liệu worksheet không nhất thiết phải giữ toàn bộ trong bộ nhớ đến cuối cùng như `XSSFWorkbook`.

Tuy nhiên cần phân biệt:

- `streaming write`: ghi dữ liệu sheet dần ra workbook/output
- `streaming read from DB`: đọc dữ liệu từ database từng phần hoặc từng dòng

Nếu ứng dụng vẫn làm:

1. query `50_000` record vào `List<OrderExportRow>`
2. loop list đó để ghi Excel
3. sang batch tiếp theo

thì đây mới là:

- `batch-read + streaming-write`

chưa phải:

- `true end-to-end streaming`

Điểm này rất quan trọng. Nhiều người nghĩ dùng `fastexcel` là RAM sẽ luôn thấp, nhưng nếu phía DB/JPA vẫn materialize batch lớn thì heap vẫn tăng mạnh.

## 3. Cách dùng cơ bản

Ví dụ tối giản:

```java
try (Workbook workbook = new Workbook(outputStream, "learning", "1.0")) {
    Worksheet sheet = workbook.newWorksheet("orders");

    sheet.value(0, 0, "id");
    sheet.value(0, 1, "orderNo");

    int rowIndex = 1;
    for (OrderExportRow row : rows) {
        sheet.value(rowIndex, 0, row.id());
        sheet.inlineString(rowIndex, 1, row.orderNo());
        rowIndex++;
    }

    workbook.finish();
}
```

Ý nghĩa:

- `Workbook` quản lý file `.xlsx`
- `Worksheet` đại diện cho sheet đang ghi
- `value(...)` ghi cell
- `inlineString(...)` ghi string inline thay vì shared string
- `finish()` hoàn tất workbook và ghi các phần còn lại ra output

## 4. `value(...)` và `inlineString(...)`

Với string, `fastexcel` có hai hướng chính:

- `value(row, col, string)`: dùng shared string
- `inlineString(row, col, string)`: ghi trực tiếp string vào cell XML

### Shared string

Ưu điểm:

- file nhỏ hơn nếu có nhiều giá trị lặp lại
- phù hợp với cột cardinality thấp như `status`

Nhược điểm:

- thư viện phải giữ shared strings table trong memory
- nếu cột có nhiều giá trị gần như unique, RAM tăng đáng kể

### Inline string

Ưu điểm:

- giảm memory vì không cần giữ shared strings table lớn
- phù hợp với cột có cardinality cao

Nhược điểm:

- file `.xlsx` thường lớn hơn một chút

Trong case export lớn, nên ưu tiên `inlineString(...)` cho:

- mã đơn hàng gần như unique
- timestamp / datetime
- text có mức lặp thấp

Nên giữ shared string cho:

- `status`
- `type`
- `gender`
- các cột enum ngắn, lặp lại nhiều

## 5. `Worksheet.flush()` thực sự làm gì

`Worksheet.flush()` không phải là GC, cũng không phải là flush của network/socket.

Nó là flush ở tầng `fastexcel` worksheet writer.

Ý nghĩa thực tế:

- ghi các row hiện đang nằm trong memory của worksheet ra workbook/output stream
- bỏ state của các row đã flush khỏi worksheet
- sau khi flush, không được quay lại sửa các row đã ghi trước đó

Tác dụng:

- giảm memory nội bộ của chính `fastexcel`
- đặc biệt hữu ích khi ghi rất nhiều row

Không có tác dụng trực tiếp với:

- `List<OrderExportRow>` đang giữ trong Java heap
- object do JPA vừa materialize
- string tạm tạo trong business code
- GC của JVM

Hiểu ngắn gọn:

- `sheet.flush()` giúp giải phóng buffer/state ở tầng Excel writer
- nó không giải phóng batch data phía ứng dụng

## 6. `OutputStream.flush()` thực sự làm gì

`outputStream.flush()` là flush ở tầng I/O.

Nó chỉ cố đẩy các bytes đang buffer xuống tầng dưới:

- servlet response buffer
- socket
- file stream
- buffered stream

Nó không:

- xóa dữ liệu trong `Worksheet`
- ép `fastexcel` bỏ row state
- ép JVM thu hồi memory

Hiểu ngắn gọn:

- `sheet.flush()` = flush dữ liệu sheet của `fastexcel`
- `outputStream.flush()` = flush bytes đã ghi xuống stream

Hai lệnh này thuộc hai tầng khác nhau.

## 7. Vì sao dùng Fastexcel rồi mà RAM vẫn cao

Nguyên nhân phổ biến nhất không nằm ở workbook, mà nằm ở cách load dữ liệu.

Ví dụ:

```java
List<OrderExportRow> rows = repository.findNextBatch(lastId, 50_000);
for (OrderExportRow row : rows) {
    sheet.value(...);
}
sheet.flush();
```

Trong flow này, RAM vẫn tăng vì:

- `50_000` row đã được load sẵn thành `List`
- mỗi row có nhiều object con: `String`, `Long`, `BigDecimal`, `OffsetDateTime`
- lúc ghi còn tạo thêm object tạm như string format datetime

Nên `fastexcel` có thể đang nhẹ, nhưng batch JPA vẫn nặng.

Vì vậy:

- `fastexcel` giải quyết tốt bài toán ghi Excel
- nhưng không tự động biến JPA batch thành true streaming

## 8. Batching và true streaming khác nhau thế nào

### Batching

Ví dụ:

- mỗi lần load `5_000` hoặc `50_000` row
- ghi xong batch này mới sang batch khác

Ưu điểm:

- đơn giản
- dễ làm với JPA

Nhược điểm:

- mỗi batch vẫn là một cục object lớn trong heap
- batch càng lớn, peak RAM càng tăng

### True streaming

Ví dụ:

- JDBC cursor
- `fetchSize`
- đọc từng row hoặc một nhóm rất nhỏ từ DB rồi ghi ngay ra Excel

Ưu điểm:

- heap phẳng hơn nhiều
- ít object sống đồng thời hơn

Nhược điểm:

- code phức tạp hơn JPA `getResultList()`
- cần kiểm soát transaction/connection/cursor cẩn thận

## 9. Kinh nghiệm tối ưu RAM khi dùng Fastexcel

### 9.1 Giảm batch size

Nếu đang dùng batch `50_000`, hãy thử:

- `5_000`
- `10_000`

Đây thường là tối ưu dễ ăn nhất.

Đánh đổi:

- có thể tăng số vòng query
- nhưng peak heap giảm rõ rệt

### 9.2 Dùng `sheet.flush()` theo batch

Sau khi ghi xong một batch:

```java
sheet.flush();
```

Điều này giúp row state của worksheet không tích lũy quá lâu.

### 9.3 Dùng `inlineString(...)` cho cột cardinality cao

Rất hiệu quả cho:

- `orderNo`
- `createdAt`
- `updatedAt`
- các mã hoặc text gần như unique

### 9.4 Hạn chế format tạo nhiều string tạm

Ví dụ:

```java
formatDateTime(rowData.createdAt())
```

Nếu gọi cho hàng triệu cell, số string tạm tạo ra là rất lớn.

Nên cân nhắc:

- giảm số cột string hóa
- format đơn giản hơn
- hoặc dùng JDBC đọc trực tiếp kiểu phù hợp để giảm object churn

### 9.5 Nếu cần RAM thấp thật sự, chuyển phần đọc DB sang cursor

Nếu mục tiêu là export rất lớn mà heap vẫn phải thấp, hướng bền vững là:

- bỏ `JPA getResultList()` cho case export lớn
- dùng `JdbcTemplate` hoặc JDBC thuần
- set `fetchSize`
- đọc từng row và ghi ngay

Lúc đó `fastexcel` mới phát huy gần đầy đủ lợi thế memory.

## 10. Khi nào nên dùng Fastexcel

Nên dùng khi:

- cần export nhanh
- dataset lớn
- không cần toàn bộ feature phong phú của POI
- chấp nhận tuning một chút về `flush` và string mode

Không nên kỳ vọng:

- cứ dùng `fastexcel` là RAM luôn rất thấp
- `flush()` sẽ làm heap tụt ngay
- `outputStream.flush()` sẽ giảm memory

## 11. Kết luận ngắn

`fastexcel` rất tốt cho export lớn, nhưng cần hiểu đúng:

- nó tối ưu chủ yếu ở tầng ghi Excel
- `Worksheet.flush()` giúp xả state của worksheet writer
- `OutputStream.flush()` chỉ flush bytes ở tầng I/O
- RAM cao thường đến từ batch data và object allocation phía ứng dụng, không chỉ từ thư viện Excel

Nếu muốn throughput cao mà vẫn giữ RAM ổn:

1. giảm batch size
2. gọi `sheet.flush()` theo batch
3. dùng `inlineString(...)` cho cột cardinality cao
4. nếu cần thấp hơn nữa, chuyển sang JDBC cursor streaming
