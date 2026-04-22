# Tóm tắt chiến lược export Excel trong Spring Boot

## 1. Mục tiêu tài liệu

Tài liệu này tổng hợp các cách thiết kế chức năng export dữ liệu trong Spring Boot, từ dữ liệu nhỏ đến rất lớn, kèm lý do chọn giải pháp, ưu nhược điểm, workflow xử lý, và các lưu ý về:

- `non-streaming`
- `streaming`
- `batch query`
- `cursor`
- `keyset batch`
- `Excel` và `CSV`
- `sync export` và `async export`

Tài liệu tập trung vào bài toán thực tế:

- export file để người dùng tải xuống
- export dữ liệu lớn mà không làm ứng dụng hết RAM
- kiểm soát transaction read-only, connection pool, và timeout
- benchmark giữa các strategy khác nhau

---

## 2. Phân biệt 2 khái niệm hay bị nhầm

### 2.1. Streaming response

Đây là cách backend trả file dần dần ra `OutputStream` thay vì tạo xong toàn bộ file rồi mới trả về.

Ví dụ:

- `StreamingResponseBody`
- ghi từng row vào file
- bytes được đẩy dần ra client

Mục đích:

- giảm peak RAM
- client có thể bắt đầu nhận file sớm hơn
- phù hợp file lớn

### 2.2. Streaming data read

Đây là cách backend đọc dữ liệu từ DB dần dần thay vì load một cục lớn vào `List`.

Ví dụ:

- JDBC cursor
- keyset batch
- batch query theo `id`

Mục đích:

- giảm RAM phía backend
- giảm object churn
- giữ heap phẳng hơn

### 2.3. Kết luận

Muốn export lớn thật sự ổn, cần tách 2 chuyện:

- đọc dữ liệu có stream hay không
- ghi file có stream hay không

Nếu chỉ `streaming response` nhưng vẫn `load all rows vào List`, thì vẫn có thể tốn RAM rất cao.

---

## 3. Các mức bài toán export

Không nên chỉ nhìn vào số row. Cần nhìn đồng thời:

- số row
- số cột
- kiểu dữ liệu
- có style, formula, auto-size hay không
- file `xlsx` hay `csv`
- có cần filter phức tạp hay không
- có cần người dùng chờ kết quả ngay hay không
- có bao nhiêu request export đồng thời

### 3.1. Mức nhỏ

Quy mô điển hình:

- vài trăm đến vài nghìn row
- ít cột
- file nhỏ
- cần kết quả ngay

Giải pháp:

- export đồng bộ trong request
- có thể non-streaming
- có thể trả `byte[]`

Ưu điểm:

- đơn giản
- code ít
- dễ debug
- dễ trả file ngay

Nhược điểm:

- không scale tốt khi dữ liệu tăng
- dễ tốn RAM hơn nếu file lớn dần

Phù hợp khi:

- export danh sách nhỏ
- export cấu hình
- export dữ liệu admin nhỏ

### 3.2. Mức vừa

Quy mô điển hình:

- vài nghìn đến 50k row
- file vẫn trong tầm kiểm soát
- người dùng vẫn có thể chờ trong request

Giải pháp:

- ưu tiên streaming write
- đọc theo batch query
- ghi Excel hoặc CSV theo từng lô

Ưu điểm:

- RAM ổn hơn non-streaming
- chưa cần hạ tầng async phức tạp
- benchmark dễ hơn

Nhược điểm:

- request bắt đầu dài
- nếu batch lớn quá vẫn tốn RAM

Phù hợp khi:

- export báo cáo nội bộ
- export dữ liệu đối soát vừa phải
- benchmark `POI` và `FastExcel`

### 3.3. Mức trung bình

Quy mô điển hình:

- 50k-200k row
- request có thể bắt đầu chậm
- cần kiểm soát heap

Giải pháp:

- streaming response gần như bắt buộc
- ưu tiên `FastExcel` hoặc `SXSSF`
- đọc theo `keyset batch` hoặc `cursor`
- tránh `load all`

Ưu điểm:

- ứng dụng bền vững hơn
- giảm peak heap
- giảm rủi ro OOM

Nhược điểm:

- code phức tạp hơn
- cần tuning batch size và fetch size

Phù hợp khi:

- export đơn hàng, giao dịch, audit log vừa và lớn

### 3.4. Mức lớn

Quy mô điển hình:

- 200k-1m row
- file xuất lâu
- request HTTP dễ timeout nếu làm sync kém

Giải pháp:

- streaming read + streaming write
- ưu tiên `cursor` hoặc `keyset batch`
- ưu tiên `CSV` nếu user không bắt buộc phải là `xlsx`
- cân nhắc async export

Ưu điểm:

- chịu được dataset lớn hơn nhiều
- memory profile dễ kiểm soát hơn

Nhược điểm:

- transaction read-only có thể sống lâu
- connection pool bị giữ lâu hơn
- cần giám sát timeout và client disconnect

Phù hợp khi:

- export báo cáo rất lớn
- xuất lịch sử đơn hàng
- xuất archive cho backoffice

### 3.5. Mức rất lớn

Quy mô điển hình:

- 1m+ row
- file rất lớn
- cần phân biệt report online và batch extract

Giải pháp:

- coi như bài toán batch pipeline
- async export
- tạo `jobId`
- worker chạy nền
- lưu file vào storage rồi trả link tải
- ưu tiên `CSV` hoặc `ZIP CSV`

Ưu điểm:

- không phụ thuộc request timeout
- dễ retry
- dễ progress tracking
- dễ giám sát và audit

Nhược điểm:

- cần thêm bảng job và luồng nền
- cần cleanup file tạm
- code và vận hành phức tạp hơn

Phù hợp khi:

- export cực lớn
- report ETL nhỏ
- xuất file để đối tác tải sau

---

## 4. Các strategy export chính

### 4.1. Non-streaming

Logic:

1. Query toàn bộ data.
2. Build toàn bộ workbook trong RAM.
3. Chuyển thành `byte[]`.
4. Trả response.

Ưu điểm:

- dễ code nhất
- dễ debug
- phù hợp file nhỏ

Nhược điểm:

- tốn RAM cao
- file càng lớn càng nguy hiểm
- dễ OOM

Không nên dùng khi:

- dữ liệu lớn
- concurrent export cao

### 4.2. Streaming write

Logic:

1. Đọc data theo batch hoặc cursor.
2. Ghi dần vào `OutputStream`.
3. Trả file dần dần cho client.

Ưu điểm:

- giảm peak RAM
- phù hợp file lớn hơn

Nhược điểm:

- code phức tạp hơn
- không giải quyết được nếu phía đọc vẫn `load all`

### 4.3. Batch query

Logic:

1. Lấy `N` row một lần.
2. Ghi vào file.
3. Lấy batch tiếp theo.

Ví dụ:

- `where id > :lastId order by id limit :batchSize`

Ưu điểm:

- dễ implement
- dễ retry và checkpoint
- không giữ một query sống quá lâu

Nhược điểm:

- vẫn tạo `List` theo từng batch
- batch lớn quá thì RAM tăng
- batch nhỏ quá thì round-trip DB tăng

### 4.4. Cursor

Logic:

1. Mở query cursor.
2. Đọc từng row hoặc fetch-size nhỏ.
3. Ghi ra file ngay.

Ưu điểm:

- RAM thấp hơn
- gần với true streaming hơn
- phù hợp export rất lớn

Nhược điểm:

- giữ connection lâu
- transaction read-only có thể sống lâu
- code JDBC phức tạp hơn

### 4.5. Keyset batch

Đây là điểm cân bằng tốt trong nhiều hệ thống.

Logic:

1. Query theo `id > lastId`.
2. Mỗi lần lấy một lô ổn định.
3. Ghi file xong mới lấy lô tiếp.

Ưu điểm:

- ổn định hơn `offset`
- dễ checkpoint
- dễ tuning
- dễ vận hành hơn cursor trong một số hệ thống

Nhược điểm:

- vẫn tốn RAM theo từng batch
- cần cột sort/index phù hợp

---

## 5. Chọn định dạng file nào

### 5.1. Excel `xlsx`

Nên dùng khi:

- người dùng cần mở bằng Excel ngay
- cần nhiều cột, sheet, style
- cần file thân thiện với business user

Ưu điểm:

- thân thiện
- phổ biến

Nhược điểm:

- ghi chậm hơn CSV
- file format phức tạp hơn
- tốn RAM hơn nếu thư viện xử lý không tốt

### 5.2. CSV

Nên dùng khi:

- dữ liệu rất lớn
- cần tốc độ
- không cần style
- user chủ yếu tải về để import tiếp hoặc mở bằng công cụ khác

Ưu điểm:

- rất nhanh
- nhẹ
- dễ zip

Nhược điểm:

- không có style
- không nhiều sheet
- cần chú ý encoding và separator

### 5.3. ZIP CSV

Nên dùng khi:

- file CSV lớn
- muốn giảm dung lượng mạng
- user tải file archive

Ưu điểm:

- nhẹ hơn
- phù hợp xuất cực lớn

Nhược điểm:

- user phải giải nén

---

## 6. Workflow khuyến nghị theo mức dữ liệu

### 6.1. Workflow cho dữ liệu nhỏ

1. FE gọi API export.
2. Backend query data.
3. Tạo file ngay.
4. Trả file.

### 6.2. Workflow cho dữ liệu vừa

1. FE gọi API export.
2. Backend query theo batch.
3. Ghi dần vào `OutputStream`.
4. Trả file streaming.

### 6.3. Workflow cho dữ liệu trung bình

1. FE gọi API export.
2. Backend chọn `FastExcel`, `SXSSF`, hoặc `CSV`.
3. Đọc theo `keyset batch` hoặc `cursor`.
4. Ghi streaming.
5. Theo dõi heap, CPU, request duration.

### 6.4. Workflow cho dữ liệu lớn

1. FE gọi API export.
2. Backend mở luồng read-only.
3. Đọc dữ liệu theo `cursor` hoặc `keyset`.
4. Ghi file streaming.
5. Nếu request quá dài hoặc file quá lớn, chuyển sang async export.

### 6.5. Workflow cho dữ liệu rất lớn

1. FE tạo export job.
2. Backend trả `jobId`.
3. Worker nền export file.
4. File được lưu vào storage.
5. FE poll trạng thái.
6. Xong thì FE tải file qua link.

---

## 7. Async export là gì

`Async export` nghĩa là export không chạy trọn vẹn trong request HTTP của người dùng.

Luồng điển hình:

1. FE gọi `POST /exports`.
2. Backend tạo `jobId`.
3. Worker chạy nền.
4. Worker sinh file.
5. Lưu file vào disk hoặc object storage.
6. FE gọi `GET /exports/{jobId}` để xem trạng thái.
7. Xong thì tải file.

Trạng thái thường dùng:

- `PENDING`
- `PROCESSING`
- `COMPLETED`
- `FAILED`
- `EXPIRED`

Vì sao nên dùng:

- tránh timeout FE và gateway
- dễ retry
- dễ theo dõi progress
- phù hợp file cực lớn

---

## 8. Workflow async export chi tiết

### 8.1. Bước 1: FE gửi yêu cầu tạo export job

FE không gọi thẳng API tải file rất lớn.

Thay vào đó:

- FE gọi `POST /api/v1/export-jobs`
- truyền các điều kiện lọc:
  - từ ngày
  - đến ngày
  - trạng thái
  - loại file
  - kiểu export

Backend làm các việc:

1. Validate request.
2. Tính xem request có hợp lệ không.
3. Tạo bản ghi `export_job`.
4. Ghi trạng thái `PENDING`.
5. Trả về:
   - `jobId`
   - `status`
   - thời điểm tạo

Ví dụ response:

```json
{
  "jobId": 12345,
  "status": "PENDING"
}
```

### 8.2. Bước 2: Lưu metadata job

Thường nên có bảng `export_job`.

Ví dụ các cột:

- `id`
- `job_type`
- `status`
- `requested_by`
- `request_payload`
- `file_name`
- `file_path`
- `file_size`
- `total_rows`
- `processed_rows`
- `progress_percent`
- `error_message`
- `started_at`
- `finished_at`
- `expired_at`

Mục đích:

- biết job nào đang chạy
- biết ai tạo
- biết file nằm ở đâu
- biết tiến độ tới đâu
- retry và audit dễ hơn

### 8.3. Bước 3: Đẩy job sang worker nền

Có 3 cách thường gặp:

1. `@Async`
2. scheduler quét `PENDING job`
3. queue thật như RabbitMQ, Kafka, SQS

Với hệ thống business thông thường:

- demo hoặc nhỏ: `@Async`
- thực dụng và ổn định: scheduler + DB polling
- scale lớn: message queue

### 8.4. Bước 4: Worker claim job

Worker lấy một job `PENDING` và chuyển trạng thái sang `PROCESSING`.

Thường sẽ làm:

1. lock job
2. update status thành `PROCESSING`
3. ghi `started_at`
4. gắn worker hiện tại nếu cần

Mục tiêu:

- tránh 2 worker cùng xử lý 1 job

### 8.5. Bước 5: Chuẩn bị output file

Worker cần quyết định:

- xuất `xlsx` hay `csv`
- ghi ra local disk, network disk, hay object storage

Ví dụ:

- local temp path
- MinIO
- S3

Nếu file rất lớn:

- nên ghi ra file tạm trước
- xong mới upload storage hoặc đổi trạng thái downloadable

### 8.6. Bước 6: Đọc dữ liệu theo strategy phù hợp

Worker không nên `load all`.

Tùy trường hợp:

- `keyset batch`
- `cursor`
- `batch query`

Ví dụ workflow:

1. mở output stream
2. tạo writer
3. bắt đầu query batch 1
4. ghi từng row
5. cập nhật `processed_rows`
6. sang batch tiếp

Nếu là `cursor`:

- giữ transaction read-only
- fetch-size phù hợp
- ghi trực tiếp từng row

Nếu là `keyset batch`:

- mỗi batch là một query ngắn hơn
- dễ checkpoint hơn

### 8.7. Bước 7: Cập nhật tiến độ

Trong lúc chạy, worker nên update:

- `processed_rows`
- `progress_percent`
- `current_step`

Ví dụ:

- `PREPARING`
- `READING_DATA`
- `WRITING_FILE`
- `UPLOADING_FILE`
- `COMPLETED`

FE có thể gọi:

- `GET /api/v1/export-jobs/{jobId}`

để xem:

- đang chạy chưa
- đã xong chưa
- đã xử lý bao nhiêu row

### 8.8. Bước 8: Hoàn tất file

Khi ghi xong:

1. flush writer
2. đóng workbook / output stream
3. lấy kích thước file
4. upload file lên storage nếu cần
5. cập nhật:
   - `file_name`
   - `file_path`
   - `file_size`
   - `total_rows`
   - `processed_rows`

### 8.9. Bước 9: Chuyển trạng thái completed

Khi job xong:

- `status = COMPLETED`
- ghi `finished_at`
- ghi link tải file

FE lúc này có thể:

- hiện nút download
- hoặc gọi API download theo `jobId`

Ví dụ:

- `GET /api/v1/export-jobs/{jobId}/download`

### 8.10. Bước 10: Nếu lỗi thì sao

Nếu job lỗi:

1. bắt exception
2. cập nhật:
   - `status = FAILED`
   - `error_message`
   - `finished_at`
3. giữ log để debug

Nếu cần retry:

- tạo job mới
- hoặc cho phép `RETRY` từ request gốc

### 8.11. Bước 11: Cleanup file cũ

Async export thường phải có cleanup.

Ví dụ:

- file chỉ giữ 1 ngày
- hoặc 3 ngày
- hoặc 7 ngày

Scheduler cleanup sẽ:

1. tìm job `COMPLETED` đã hết hạn
2. xóa file
3. update `status = EXPIRED`
4. hoặc xóa hẳn record nếu policy cho phép

### 8.12. Kết luận về async export

Async export đúng nghĩa thường là:

1. request tạo job rất ngắn
2. worker chạy export ở background
3. file được lưu riêng
4. FE chỉ poll trạng thái và tải file khi xong

Đây là cách phù hợp nhất cho:

- export dữ liệu lớn
- export lâu
- export dễ bị timeout nếu chạy sync

---

## 9. Transaction và connection trong export

Export thường là read-only, nhưng vẫn cần cẩn thận.

### 9.1. Transaction read-only

Thường nên dùng:

- `@Transactional(readOnly = true)` cho pha đọc lớn

Ưu điểm:

- rõ nghĩa vụ business
- tốt hơn cho JPA/JDBC read path

Nhưng cần lưu ý:

- nếu giữ transaction quá lâu với cursor, DB và pool sẽ bị ảnh hưởng

### 9.2. Connection pool

Cursor export dễ giữ connection lâu.

Rủi ro:

- pool starvation
- request khác chờ connection

Nên giám sát:

- số request export đồng thời
- Hikari active connections
- request duration

### 9.3. Timeout

Cần phân biệt:

- request timeout
- transaction timeout
- DB statement timeout
- client disconnect

Với export lớn thật sự, sớm muộn gì cũng sẽ dùng async export nếu request sync bắt đầu quá dài.

---

## 10. Decision guide: khi nào chọn solution nào

### 10.1. Nếu dữ liệu nhỏ

Chọn:

- non-streaming hoặc streaming đơn giản
- sync export

### 10.2. Nếu dữ liệu vừa

Chọn:

- streaming write
- batch query
- `FastExcel` hoặc `SXSSF`

### 10.3. Nếu dữ liệu trung bình

Chọn:

- streaming write
- ưu tiên `keyset batch`
- benchmark `POI`, `FastExcel`, `CSV`

### 10.4. Nếu dữ liệu lớn

Chọn:

- `cursor` hoặc `keyset batch`
- ưu tiên `CSV` nếu có thể
- cân nhắc async export

### 10.5. Nếu dữ liệu rất lớn

Chọn:

- async export
- worker riêng
- lưu file vào storage
- trả link tải file

---

## 11. Bộ ưu tiên cho export 50k+ row

Khi file export từ khoảng 50k row trở lên, thứ tự ưu tiên thường là:

1. `streaming write`
2. `batch query hoặc cursor`
3. `tránh load all vào List`
4. `keyset pagination`
5. `chọn đúng định dạng file`
6. `read-only transaction`
7. `theo dõi heap và request duration`
8. `cân nhắc async export`

### 11.1. Vì sao lại theo thứ tự này

`streaming write`

- vì đây là bước giảm peak RAM thấy rõ nhất

`batch query hoặc cursor`

- vì nếu vẫn load all thì writer có stream cũng chưa đủ

`tránh load all vào List`

- vì đây là nguyên nhân phổ biến nhất làm heap tăng

`keyset pagination`

- vì nó bền vững hơn `offset`

`chọn đúng định dạng file`

- vì `CSV` có thể nhanh và nhẹ hơn nhiều so với `xlsx`

`read-only transaction`

- vì cần rõ ràng về luồng đọc lớn

`theo dõi heap và request duration`

- vì benchmark export không chỉ nhìn mỗi thời gian trả file

`cân nhắc async export`

- vì đến một mức nào đó request sync sẽ không còn hợp lý nữa

---

## 12. Những chỗ thường làm sai

- query toàn bộ data vào `List`
- dùng `byte[]` cho file lớn
- dùng `offset/limit` cho dataset rất lớn
- nhầm `streaming response` với `streaming data read`
- giữ transaction và connection quá lâu mà không giám sát
- export `xlsx` cho data cực lớn dù người dùng chỉ cần CSV
- không benchmark với cùng một dataset
- không theo dõi heap toàn process

---

## 13. Khuyến nghị mặc định cho hệ thống business

### 13.1. Nếu dưới 10k row

- sync export
- có thể non-streaming

### 13.2. Nếu 10k-50k row

- streaming write
- `FastExcel` hoặc `SXSSF`
- batch query

### 13.3. Nếu 50k-200k row

- streaming write
- `keyset batch` hoặc `cursor`
- benchmark thêm `CSV`

### 13.4. Nếu trên 200k row

- ưu tiên `cursor` hoặc `CSV`
- cân nhắc async export
- giám sát connection pool

### 13.5. Nếu 1m+ row

- async export gần như là mặc định
- worker nền
- lưu file vào storage
- trả link tải xuống

---

## 14. Kết luận

Với export dữ liệu, không có một solution duy nhất cho mọi trường hợp.

Nguyên tắc chọn kiến trúc:

- data càng lớn, càng phải tránh `load all`
- file càng lớn, càng nên streaming write
- nếu user không cần `xlsx`, hãy nghiêm túc cân nhắc `CSV`
- nếu request sync bắt đầu quá dài, hãy nghĩ tới async export
- benchmark export cần nhìn đồng thời:
  - thời gian
  - heap
  - CPU
  - kết nối DB
  - khả năng chịu tải đồng thời

Tóm lại:

- nhỏ: giữ đơn giản
- vừa: thêm streaming
- trung bình: thêm batch query hoặc keyset
- lớn: thêm cursor và cân nhắc CSV
- rất lớn: coi như batch job và async export
