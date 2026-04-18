# Cảm giác méo khác biệt mấy :))

# Batch Query vs Cursor Export Cho Excel

Tài liệu này giải thích chi tiết khi nào nên dùng `batch query` và khi nào nên dùng `cursor` để export Excel trong Java/Spring Boot, đặc biệt với các bài toán export dữ liệu lớn.

Mục tiêu là trả lời rõ:

- hai cách này khác nhau ở đâu
- ưu nhược điểm thực tế của từng cách
- tác động đến RAM, GC, throughput, database, connection pool, độ ổn định hệ thống
- use case nào phù hợp với mỗi cách
- nên chọn thế nào trong production

## 1. Tổng quan ngắn

Hai mô hình phổ biến để đọc dữ liệu export Excel là:

### Batch query

Đọc dữ liệu theo từng lô, ví dụ:

- `1_000` row mỗi lần
- `5_000` row mỗi lần
- `50_000` row mỗi lần

Mỗi vòng:

1. query một batch
2. map kết quả thành object/list
3. ghi batch đó vào Excel
4. tiếp tục batch kế tiếp

### Cursor

Đọc dữ liệu tuần tự từ database bằng server-side cursor hoặc fetch streaming, nghĩa là application không materialize cả một list lớn cho từng batch logic. Application nhận row lần lượt hoặc theo network fetch nhỏ, xử lý tới đâu ghi Excel tới đó.

Mỗi vòng logic:

1. mở query
2. giữ cursor mở
3. đọc từng row hoặc nhóm nhỏ từ `ResultSet`
4. ghi ngay ra Excel
5. kết thúc khi hết dữ liệu

## 2. Khác biệt cốt lõi

Sự khác biệt quan trọng nhất không nằm ở chữ "streaming", mà nằm ở số lượng object sống đồng thời trong JVM.

### Batch query

Bạn vẫn tạo ra một `List<RowDto>` hoặc `List<Entity>` cho từng lô. Điều đó có nghĩa là trong mỗi thời điểm, JVM đang giữ:

- danh sách của batch hiện tại
- object cho từng row
- field object của từng row như `String`, `BigDecimal`, `OffsetDateTime`
- object tạm phát sinh trong lúc ghi Excel

Batch càng lớn:

- peak RAM càng cao
- GC pressure càng lớn
- độ dao động heap càng mạnh

### Cursor

Bạn gần như không giữ một `List` lớn của business object. Dữ liệu được đọc dần từ `ResultSet` rồi ghi ngay vào file.

Điều đó giúp:

- giảm số lượng object sống cùng lúc
- giảm peak heap
- làm memory profile phẳng hơn

Nói ngắn gọn:

- `batch query` tối ưu độ đơn giản
- `cursor` tối ưu memory behavior

## 3. Batch query là gì

Ví dụ tư duy:

```java
Long lastId = null;
while (true) {
    List<OrderExportRow> rows = repository.findNextBatch(lastId, 5000);
    if (rows.isEmpty()) {
        break;
    }

    for (OrderExportRow row : rows) {
        writeToExcel(row);
    }

    lastId = rows.get(rows.size() - 1).id();
}
```

Đây thường là keyset pagination hoặc paging bằng `id`.

Biến thể phổ biến:

- JPA projection + `setMaxResults`
- Spring Data `PageRequest`
- native query phân trang theo `id`
- query kiểu `where id > :lastId order by id limit :batchSize`

## 4. Cursor là gì

Ví dụ tư duy:

```java
jdbcTemplate.query(connection -> {
    PreparedStatement ps = connection.prepareStatement(sql,
            ResultSet.TYPE_FORWARD_ONLY,
            ResultSet.CONCUR_READ_ONLY);
    ps.setFetchSize(1000);
    return ps;
}, rs -> {
    while (rs.next()) {
        writeToExcel(rs);
    }
    return null;
});
```

Ở đây:

- query được giữ mở trong suốt thời gian export
- database trả dữ liệu dần về client
- application xử lý tới đâu ghi file tới đó

Tùy DB/driver:

- có thể là server-side cursor thực sự
- có thể là fetch window tuần tự

Trong PostgreSQL, `fetchSize > 0` trong transaction read-only thường là mô hình phù hợp để lấy dần dữ liệu thay vì tải toàn bộ về ngay.

## 5. Batch query có ưu điểm gì

Batch query rất phổ biến vì nó dễ làm và đủ tốt trong rất nhiều hệ thống.

### 5.1 Dễ implement

Bạn có thể tận dụng ngay:

- JPA repository
- DTO projection
- mapper sẵn có
- transaction pattern quen thuộc

Không cần viết quá nhiều code JDBC thủ công.

### 5.2 Dễ debug

Khi có lỗi ở batch thứ `N`, bạn dễ truy ra:

- batch nào hỏng
- row id cuối cùng là gì
- có thể resume từ đâu

Mô hình này dễ log và dễ replay hơn cursor.

### 5.3 Dễ kiểm soát retry/resume

Vì export chia theo lô rõ ràng, bạn có thể:

- lưu checkpoint `lastId`
- retry từ batch lỗi
- chia export thành job con

Đây là điểm rất mạnh khi export chạy nền hoặc cần khôi phục.

### 5.4 Giảm thời gian giữ connection liên tục

Mỗi batch là một query ngắn. Sau mỗi batch:

- query kết thúc
- DB resource của query đó được giải phóng

Điều này phù hợp với hệ thống mà:

- connection pool nhỏ
- export không phải tác vụ tối quan trọng
- không muốn giữ transaction mở quá lâu

### 5.5 Dễ song song hóa

Bạn có thể chia khoảng dữ liệu:

- id `1-100k`
- id `100k-200k`
- id `200k-300k`

rồi export song song nếu cần.

Điều này khó làm đúng hơn với một cursor đơn.

## 6. Batch query có nhược điểm gì

### 6.1 Peak RAM cao hơn cursor

Đây là nhược điểm lớn nhất.

Mỗi batch tạo ra:

- list
- nhiều row object
- field object
- object tạm để format/serialize

Nếu batch quá lớn, heap sẽ tăng mạnh dù bạn đang "streaming write" ở phía Excel.

### 6.2 GC pressure lớn hơn

JVM phải thu gom số lượng lớn object sống ngắn hạn sau mỗi batch.

Dấu hiệu thường thấy:

- heap lên xuống theo từng đợt
- `gcCount` cao
- `gcTime` tăng
- export bị jitter về tốc độ

### 6.3 Có thể vẫn chưa phải streaming thật sự

Nhiều người dùng batch query rồi nghĩ rằng hệ thống đã stream hoàn toàn. Thực ra chưa chắc.

Nếu bạn:

- query `50k` row vào list
- map xong rồi mới ghi

thì bạn vẫn đang materialize một cục dữ liệu lớn trong RAM.

### 6.4 Batch size rất nhạy cảm

Batch nhỏ quá:

- số query tăng
- round-trip DB tăng
- tổng thời gian có thể tăng

Batch lớn quá:

- RAM tăng
- GC nặng
- nguy cơ OOM tăng

Nghĩa là batch query đòi hỏi tuning.

### 6.5 Paging offset rất tệ cho export lớn

Nếu dùng `offset/limit` thay vì keyset:

- DB càng về sau càng scan nhiều
- hiệu năng giảm mạnh
- dễ không ổn định với bảng lớn

Nếu dùng batch query cho export lớn, nên ưu tiên keyset pagination.

## 7. Cursor có ưu điểm gì

### 7.1 Heap thấp và ổn định hơn

Đây là lợi thế quan trọng nhất.

Vì application không giữ `List` lớn, số object sống cùng lúc giảm mạnh. Với export Excel lớn, đây thường là khác biệt quyết định.

### 7.2 Gần với true streaming end-to-end

Pipeline sẽ gần hơn với mô hình:

- DB trả dần row
- app ghi dần row
- client nhận dần bytes

Đây là mô hình tối ưu cho export rất lớn.

### 7.3 Giảm object churn

Khi dùng JDBC cursor trực tiếp:

- ít DTO/list trung gian hơn
- ít allocation hơn
- GC nhẹ hơn

### 7.4 Phù hợp nhất cho export cực lớn

Ví dụ:

- vài trăm nghìn row
- một triệu row
- nhiều triệu row

Lúc này cursor thường là lựa chọn bền vững hơn nếu mục tiêu là an toàn RAM.

### 7.5 Phù hợp với service chuyên export

Nếu bạn có endpoint hoặc job export dành riêng cho:

- tải báo cáo lớn
- xuất dữ liệu archive
- backoffice bulk export

thì cursor rất đáng giá.

## 8. Cursor có nhược điểm gì

### 8.1 Phức tạp hơn

Bạn phải hiểu rõ:

- transaction boundary
- fetch size
- driver behavior
- DB-specific semantics
- connection lifecycle

Không cẩn thận thì "cursor" chỉ là cảm giác cursor, còn driver vẫn kéo cả result về memory.

### 8.2 Giữ connection lâu

Một export kéo dài vài chục giây hoặc vài phút sẽ giữ một connection mở liên tục.

Điều này ảnh hưởng tới:

- connection pool
- throughput của ứng dụng
- các request khác

Nếu nhiều export chạy song song, pool có thể bị nghẽn.

### 8.3 Giữ transaction mở lâu

Để cursor hoạt động đúng trên nhiều DB/driver, bạn thường cần transaction mở trong suốt quá trình đọc.

Điều đó tạo thêm rủi ro:

- transaction sống quá lâu
- snapshot tồn tại lâu
- ảnh hưởng vacuum hoặc resource trên DB
- khó kiểm soát nếu client download chậm

### 8.4 Khó retry/resume hơn

Nếu stream đang đi dở mà lỗi:

- không dễ resume giữa chừng
- thường phải export lại từ đầu

Batch query mạnh hơn ở bài toán checkpoint.

### 8.5 Phụ thuộc driver/database

Cursor behavior không hoàn toàn giống nhau giữa:

- PostgreSQL
- MySQL
- Oracle
- SQL Server

Driver nào cũng có góc cạnh riêng về `fetchSize`, autocommit, transaction, buffering.

### 8.6 Dễ ràng buộc application với JDBC-level code

Nếu codebase của bạn đang chuẩn hóa trên JPA repository, việc thêm cursor export làm tăng:

- code imperative
- SQL thủ công
- mapping thủ công
- chi phí bảo trì

## 9. So sánh trực tiếp

## 9.1 Về RAM

Batch query:

- cao hơn
- biến động theo batch
- phụ thuộc mạnh vào `batchSize`

Cursor:

- thấp hơn
- phẳng hơn
- ít nhạy với kích thước dataset hơn

## 9.2 Về tốc độ

Không có đáp án tuyệt đối.

Batch query có thể nhanh hơn trong một số tình huống vì:

- mỗi query độc lập
- đơn giản hơn ở tầng driver
- ít overhead quản lý cursor

Cursor có thể nhanh hoặc chậm hơn tùy:

- DB
- network
- fetchSize
- tốc độ ghi Excel

Trong thực tế production, chênh lệch tốc độ thường không quan trọng bằng độ an toàn RAM.

## 9.3 Về độ phức tạp code

Batch query:

- đơn giản hơn
- phù hợp với JPA

Cursor:

- khó hơn
- thường phải dùng JDBC hoặc native logic

## 9.4 Về độ thân thiện vận hành

Batch query:

- thân thiện hơn với connection pool
- retry/checkpoint dễ hơn

Cursor:

- thân thiện hơn với heap
- nhưng nặng hơn với connection lifetime

## 9.5 Về khả năng khôi phục khi lỗi

Batch query:

- tốt hơn
- resume dễ hơn

Cursor:

- yếu hơn
- thường phải chạy lại

## 10. Khi nào nên dùng batch query

Bạn nên ưu tiên batch query khi:

- dataset không quá lớn
- vài nghìn đến vài chục nghìn dòng
- có thể lên tới vài trăm nghìn nhưng heap còn rộng
- muốn code đơn giản
- đang dùng JPA và muốn tái sử dụng repository/projection
- cần checkpoint/retry/resume
- export chạy như background job phân lô
- connection pool nhỏ và không muốn giữ connection quá lâu

### Use case phù hợp

- export danh sách khách hàng `10k - 50k` dòng
- export báo cáo admin nội bộ mỗi ngày
- export nền có thể chia batch và retry
- export cần resume từ `lastId`
- hệ thống có nhiều business rule map ở tầng service hiện có

### Batch query đặc biệt phù hợp khi

- bạn muốn thực dụng
- yêu cầu memory chưa quá căng
- ưu tiên tốc độ triển khai

## 11. Khi nào nên dùng cursor

Bạn nên ưu tiên cursor khi:

- dataset rất lớn
- export có thể lên tới hàng trăm nghìn hoặc hàng triệu dòng
- heap của JVM bị giới hạn
- export cần giữ memory càng phẳng càng tốt
- pipeline đã thiết kế theo hướng streaming response
- chấp nhận viết code JDBC chuyên biệt cho export

### Use case phù hợp

- export `1M+` orders
- xuất dữ liệu audit lớn
- tải archive lịch sử dài hạn
- backoffice export khối lượng lớn nhưng ít concurrent request
- batch job export chạy riêng, có quota connection rõ ràng

### Cursor đặc biệt phù hợp khi

- memory là constraint chính
- export size rất khó dự đoán
- không muốn bị bất ngờ vì peak heap lớn

## 12. Khi nào không nên dùng cursor

Không nên dùng cursor nếu:

- hệ thống có pool rất nhỏ và nhiều request cạnh tranh
- client thường tải file rất chậm
- export thường xuyên bị hủy giữa chừng
- cần resume từ giữa
- đội ngũ không muốn vận hành logic JDBC đặc thù
- DB đang rất nhạy với long-running transaction

Trong những trường hợp này, batch query thường cân bằng hơn.

## 13. Khi nào không nên dùng batch query

Không nên dùng batch query nếu:

- batch phải lớn mới đủ nhanh, nhưng batch lớn làm heap tăng quá mạnh
- export vài trăm nghìn đến hàng triệu dòng
- JVM nhỏ
- nhiều request export đồng thời dễ đẩy hệ thống vào áp lực GC
- bạn đã thấy heap profile rung mạnh theo từng batch

Lúc đó batch query không còn là tối ưu phù hợp nữa.

## 14. Góc nhìn về database

Khi chọn giữa batch và cursor, đừng chỉ nhìn Java heap. Cần nhìn cả DB.

### Batch query tác động đến DB như thế nào

- tạo nhiều query nhỏ hoặc vừa
- mỗi query tương đối độc lập
- ít giữ transaction lâu
- dễ scale ngang hơn ở tầng job orchestration

Nhưng:

- số round-trip tăng
- nếu dùng offset paging thì rất tệ
- cần index tốt cho keyset pagination

### Cursor tác động đến DB như thế nào

- một query sống lâu hơn
- connection bị giữ xuyên suốt export
- snapshot hoặc resource của query tồn tại lâu hơn

Nhưng:

- giảm round-trip logic
- đọc tuần tự tốt hơn cho full scan có order
- tránh vật liệu hóa batch lớn ở app

## 15. Tác động đến connection pool

Đây là một trong những tiêu chí chọn phương án rất quan trọng.

### Batch query

Pool chịu áp lực nhẹ hơn theo từng truy vấn ngắn.

Phù hợp khi:

- pool nhỏ
- nhiều request web chung hệ thống

### Cursor

Mỗi export giữ một connection lâu hơn.

Nếu pool có `10` connection và có `5` export lớn chạy cùng lúc, bạn có thể bị:

- pool starvation
- request khác chờ connection
- tăng latency hệ thống

Nghĩa là cursor tốt cho memory, nhưng có thể đắt ở tầng connection.

## 16. Tác động đến client/network

### Batch query

Bạn có thể chủ động điều phối theo từng chặng dễ hơn, nhưng vẫn có khả năng đẩy dữ liệu ra đều nếu writer là streaming.

### Cursor

Nếu client rất chậm:

- export kéo dài hơn
- connection DB bị giữ lâu hơn
- transaction mở lâu hơn

Điểm này rất hay bị bỏ qua. Cursor không chỉ phụ thuộc DB nhanh hay chậm, mà còn phụ thuộc tốc độ tiêu thụ của pipeline end-to-end.

## 17. Khả năng kết hợp

Không nhất thiết hệ thống phải chọn một trong hai cho mọi bài toán.

Một chiến lược rất thực dụng:

- export nhỏ hoặc vừa: dùng batch query
- export lớn: dùng cursor

Ví dụ:

- dưới `100k` dòng: batch query
- trên `100k` hoặc `300k` dòng: cursor

Ngưỡng chính xác phụ thuộc:

- heap
- DB
- thư viện Excel
- mức concurrent export

Đây thường là kiến trúc tốt nhất trong production.

## 18. Về thư viện Excel: đừng nhầm nguyên nhân

Rất nhiều trường hợp RAM cao không phải do thư viện Excel, mà do cách đọc dữ liệu.

Ví dụ:

- dùng `fastexcel`
- nhưng mỗi batch vẫn load `50k` row vào `List`

Khi đó:

- writer có thể khá nhẹ
- nhưng batch JPA vẫn nặng

Nên khi thấy RAM cao, phải tách rõ:

- chi phí do reader
- chi phí do mapper
- chi phí do writer

Batch query và cursor khác nhau chủ yếu ở phần reader.

## 19. Guideline chọn nhanh

Nếu câu hỏi của bạn là:

### "Tôi muốn code nhanh, dễ maintain, dữ liệu không quá lớn"

Chọn:

- batch query

### "Tôi cần export hàng trăm nghìn đến hàng triệu dòng, RAM là vấn đề lớn"

Chọn:

- cursor

### "Tôi cần resume khi lỗi, hoặc checkpoint giữa chừng"

Chọn:

- batch query

### "Tôi cần memory profile ổn định nhất có thể"

Chọn:

- cursor

### "Tôi có connection pool nhỏ, nhiều request đồng thời"

Thường nghiêng về:

- batch query

### "Tôi có một luồng export chuyên biệt, chạy ít concurrent nhưng rất lớn"

Thường nghiêng về:

- cursor

## 20. Checklist quyết định kiến trúc

Trước khi chọn, nên trả lời các câu hỏi này:

1. Export lớn nhất có thể là bao nhiêu dòng?
2. Có bao nhiêu export đồng thời?
3. Heap JVM hiện tại là bao nhiêu?
4. Connection pool hiện tại là bao nhiêu?
5. Export chạy ở request web trực tiếp hay background job?
6. Có cần resume/checkpoint không?
7. DB có chịu được long-running transaction không?
8. Client có thể tải chậm hoặc ngắt giữa chừng không?
9. Team có chấp nhận code JDBC đặc thù không?
10. Mục tiêu chính là tốc độ triển khai, tốc độ export, hay an toàn RAM?

Nếu chưa trả lời rõ các câu hỏi này, rất dễ chọn sai giải pháp.

## 21. Khuyến nghị thực dụng

Trong đa số hệ thống business thông thường:

### Bắt đầu bằng batch query nếu

- export nhỏ đến vừa
- muốn đi nhanh
- chưa có bằng chứng memory là bottleneck

Tối ưu trước bằng:

- keyset pagination
- batch size hợp lý
- `fastexcel`
- `sheet.flush()`
- `inlineString(...)` cho cột cardinality cao

### Chuyển sang cursor khi

- đã có log cho thấy heap tăng mạnh theo batch
- GC pressure trở thành vấn đề
- export đạt cỡ rất lớn
- batch size nhỏ vẫn không đủ an toàn

Đây là lộ trình kỹ thuật rất hợp lý.

## 22. Kết luận

`batch query` và `cursor` không phải là hai lựa chọn "cái nào tốt tuyệt đối", mà là hai lời giải tối ưu cho hai loại ràng buộc khác nhau.

`batch query` mạnh ở:

- đơn giản
- dễ maintain
- dễ checkpoint/retry
- ít giữ connection lâu

`cursor` mạnh ở:

- RAM thấp hơn
- heap phẳng hơn
- phù hợp export rất lớn
- gần với true streaming hơn

Nếu ưu tiên lớn nhất của bạn là:

- đơn giản và dễ vận hành ở mức vừa phải, chọn `batch query`
- kiểm soát memory cho export rất lớn, chọn `cursor`

Trong production thực tế, chiến lược tốt nhất thường là kết hợp cả hai và chọn theo ngưỡng dữ liệu thay vì ép cả hệ thống dùng một mô hình duy nhất.
