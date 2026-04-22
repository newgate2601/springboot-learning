# Tóm tắt chiến lược import Excel trong Spring Boot

## 1. Mục tiêu tài liệu

Tài liệu này tổng hợp các cách thiết kế chức năng import Excel trong Spring Boot, từ dữ liệu nhỏ đến rất lớn, kèm lý do chọn giải pháp, ưu nhược điểm, workflow xử lý, và các lưu ý về `chunk`, `background import`, `staging`, `transaction`.

Tài liệu tập trung vào bài toán thực tế:

- import dữ liệu từ Excel
- validate format
- validate với dữ liệu trong cùng file
- validate với database
- xử lý file lớn
- đảm bảo tính ổn định, dễ theo dõi, dễ mở rộng

---

## 2. Phân biệt 2 khái niệm hay bị nhầm

### 2.1. Chunk khi upload

Đây là bài toán truyền file từ FE lên backend.

Ví dụ:

- FE cắt file thành nhiều phần 5MB hoặc 10MB
- upload nhiều request
- backend hoặc object storage ghép lại thành file hoàn chỉnh

Mục đích:

- tránh lỗi upload file lớn
- hỗ trợ resume khi mạng yếu
- tối ưu transport

### 2.2. Chunk khi xử lý dữ liệu

Đây là bài toán xử lý business sau khi file đã nằm ở backend.

Ví dụ:

- đọc 1000 hoặc 5000 row một lần
- validate
- batch insert vào staging
- commit
- xử lý chunk tiếp theo

Mục đích:

- giảm RAM
- giảm thời gian transaction
- dễ batch validation
- dễ scale cho file lớn

### 2.3. Kết luận

Trong đa số hệ thống:

- FE gửi cả file lên
- backend mới đọc theo chunk để validate và import

Chỉ dùng `chunk upload` ở FE khi file quá lớn theo nghĩa truyền tải. Không nên để FE tự chia business row rồi gửi từng batch để import, vì logic validate và consistency vẫn phải xử lý ở backend.

---

## 3. Các mức bài toán import Excel

Không nên chỉ nhìn vào số row. Cần nhìn đồng thời:

- số row
- dung lượng file
- số lượng rule validate
- có check DB không
- có duplicate trong file không
- có rule liên dòng không
- có cần all-or-nothing không

### 3.1. Mức nhỏ

Quy mô điển hình:

- vài trăm đến vài nghìn row
- validate đơn giản
- ít check DB
- xử lý trong vài giây

Giải pháp:

- FE upload nguyên file
- backend xử lý đồng bộ trong request
- đọc file một lần
- validate từng row
- save trực tiếp DB

Ưu điểm:

- đơn giản
- code ít
- dễ debug
- user có kết quả ngay

Nhược điểm:

- khó scale nếu file tăng
- dễ timeout nếu business logic nặng hơn
- khó mở rộng khi cần progress hoặc retry

Phù hợp khi:

- import dữ liệu nội bộ nhỏ
- import danh sách user đơn giản
- import config hoặc master data ít

### 3.2. Mức vừa

Quy mô điển hình:

- vài nghìn đến khoảng 10k-20k row
- có thêm business validation
- có check DB nhưng chưa quá nặng

Giải pháp:

- FE upload nguyên file
- backend sync hoặc async nhẹ
- xử lý theo block nhỏ
- gom key để prefetch DB
- batch insert hoặc batch update

Ưu điểm:

- nhanh hơn kiểu query DB từng row
- chưa cần hạ tầng batch quá phức tạp
- giảm số round-trip tới DB

Nhược điểm:

- request có thể bắt đầu dài
- chưa có progress rõ ràng nếu vẫn sync
- retry chưa tốt

Phù hợp khi:

- import 5k-10k sản phẩm
- import khách hàng có check email/code
- dữ liệu không quá nhạy

### 3.3. Mức trung bình

Quy mô điển hình:

- 20k-100k row
- nhiều validate business
- có duplicate trong file
- có đối chiếu DB
- cần error report tốt hơn

Giải pháp:

- background import
- tạo `jobId`
- worker xử lý theo chunk
- prefetch DB theo chunk
- batch insert
- theo dõi progress

Ưu điểm:

- không bị request timeout
- user xem được tiến độ
- dễ retry hoặc resume hơn
- ổn định hơn sync import

Nhược điểm:

- phải quản lý job lifecycle
- code nhiều hơn
- nếu ghi trực tiếp vào bảng chính thì vẫn có thể để lại dữ liệu dở dang

Phù hợp khi:

- import file đối tác
- import dữ liệu vài chục nghìn dòng
- cần tách upload và xử lý

### 3.4. Mức lớn

Quy mô điển hình:

- 100k-500k row
- validate nhiều
- có duplicate trong file
- có rule liên dòng hoặc cross-chunk
- cần audit và error report chi tiết

Giải pháp:

- async processing gần như bắt buộc
- chunk read
- staging table
- validate 2 pha
- merge cuối sang bảng chính
- transaction ngắn theo chunk

Ưu điểm:

- an toàn hơn nhiều
- hỗ trợ all-or-nothing ở mức nghiệp vụ
- bắt được lỗi cross-chunk
- dễ retry bước merge
- dễ audit

Nhược điểm:

- phức tạp hơn rõ rệt
- tốn storage cho staging
- cần cleanup dữ liệu tạm

Phù hợp khi:

- import catalog lớn
- import master data lớn
- import dữ liệu đối tác quan trọng

### 3.5. Mức rất lớn

Quy mô điển hình:

- 500k-1m+ row
- file xử lý lâu
- validation phức tạp
- đối chiếu DB lớn
- cần retry, resume, audit, monitoring

Giải pháp:

- coi như bài toán batch pipeline hoặc ETL
- upload file tách biệt khỏi xử lý
- worker hoặc service batch riêng
- streaming reader
- chunk processing
- staging đầy đủ
- validate nhiều pha
- SQL validation trên staging
- merge có kiểm soát

Ưu điểm:

- scale tốt nhất
- chịu tải tốt nhất
- xử lý được rule toàn file phức tạp
- dễ quan sát và vận hành

Nhược điểm:

- chi phí thiết kế cao
- cần nhiều thành phần hơn
- cần kỷ luật về idempotency và cleanup

Phù hợp khi:

- import 1 triệu row
- import tài chính, logistics, catalog cực lớn
- import cần độ an toàn và truy vết cao

---

## 4. Workflow khuyến nghị theo mức dữ liệu

### 4.1. Workflow cho dữ liệu nhỏ

1. FE upload file.
2. Backend đọc file.
3. Map row sang DTO.
4. Validate format.
5. Validate business đơn giản.
6. Save DB trực tiếp.
7. Trả summary.

### 4.2. Workflow cho dữ liệu vừa

1. FE upload file.
2. Backend đọc theo block nhỏ.
3. Gom key cần check DB.
4. Query DB theo `IN (...)`.
5. Validate trong memory.
6. Batch save.
7. Trả summary.

### 4.3. Workflow cho dữ liệu trung bình

1. FE upload file.
2. Backend lưu file và tạo `import_job`.
3. Trả `jobId`.
4. Worker đọc file theo chunk.
5. Validate format.
6. Prefetch DB theo chunk.
7. Validate business.
8. Save kết quả hoặc lỗi.
9. Update progress.
10. User query tiến độ qua API.

### 4.4. Workflow cho dữ liệu lớn hoặc rất lớn

1. FE upload file hoặc multipart upload nếu file quá nặng.
2. Backend lưu file vào storage.
3. Tạo `import_job`.
4. Đẩy job vào background worker.
5. Worker đọc file theo chunk.
6. Validate format ngay lúc parse.
7. Prefetch DB theo chunk cho các rule đơn giản.
8. Ghi row vào staging.
9. Ghi lỗi vào `import_error`.
10. Sau khi nạp xong, chạy validate toàn job trên staging.
11. Nếu không có lỗi chặn, merge từ staging sang bảng chính.
12. Cập nhật summary, trạng thái, và file lỗi.

---

## 5. Background import là gì

`Background import` nghĩa là import không chạy trực tiếp trong request HTTP của người dùng.

Luồng điển hình:

1. FE gọi `POST /imports`.
2. Backend lưu file.
3. Backend tạo `jobId`.
4. Backend trả ngay thông tin job.
5. Worker nền xử lý file.
6. FE gọi `GET /imports/{jobId}` để xem trạng thái.

Trạng thái thường dùng:

- `UPLOADED`
- `PARSING`
- `VALIDATING`
- `IMPORTING`
- `COMPLETED`
- `COMPLETED_WITH_ERRORS`
- `FAILED`

Vì sao nên dùng:

- tránh timeout request
- dễ theo dõi tiến độ
- dễ retry
- tách upload khỏi xử lý
- phù hợp file lớn hoặc business nặng

Nhược điểm:

- phải quản lý lifecycle của job
- cần thêm bảng job, log, trạng thái

---

## 6. Chunk read là gì

`Chunk read` là đọc file theo từng lô nhỏ thay vì load toàn bộ file vào memory.

Ví dụ:

- chunk 1: row 1-5000
- chunk 2: row 5001-10000
- chunk 3: row 10001-15000

Với mỗi chunk:

- parse row
- validate format
- gom key cần check DB
- query DB theo lô
- validate business
- ghi staging hoặc ghi lỗi
- commit
- giải phóng memory

Lợi ích:

- giảm RAM
- giảm thời gian giữ transaction
- giảm nguy cơ crash do file lớn
- phù hợp streaming

Nhược điểm:

- khó hơn khi có rule toàn file
- cần nơi lưu state toàn job nếu có duplicate xuyên chunk hoặc rule cross-chunk

---

## 7. Validate với row trong cùng file

Các loại rule phổ biến:

- duplicate key trong file
- parent-child tham chiếu giữa các row
- rule liên dòng
- ràng buộc tổng thể toàn file

### 7.1. Duplicate trong file

Ví dụ:

- `sku` không được trùng trong file
- `email` không được trùng trong file

Cách xử lý:

- dùng `Set` hoặc `Map` nếu đủ nhỏ
- hoặc lưu key vào bảng staging hoặc bảng seen-key có unique constraint
- hoặc nạp xong staging rồi chạy SQL `group by ... having count(*) > 1`

### 7.2. Rule liên dòng hoặc cross-chunk

Ví dụ:

- `parentSku` phải tồn tại trong file hoặc DB
- các row phải tạo thành cấu trúc hợp lệ

Cách xử lý:

- hai pass: pass 1 thu key, pass 2 validate
- hoặc ghi staging trước rồi validate toàn job bằng SQL

Khuyến nghị:

- với file nhỏ đến vừa: có thể xử lý trong memory
- với file lớn: nên dùng staging + validation toàn job

---

## 8. Validate với database

Sai lầm phổ biến nhất là query DB từng row.

Ví dụ xấu:

- mỗi row check tồn tại `sku`
- check `categoryCode`
- check `email`

Nếu file 1 triệu row và mỗi row 3 query thì có thể thành 3 triệu query.

### 8.1. Cách đúng: prefetch theo chunk

Ví dụ chunk 5000 row:

1. lấy tất cả `sku`, `categoryCode`, `email` khác nhau trong chunk
2. query DB theo `IN (...)`
3. đưa kết quả về `Set` hoặc `Map`
4. validate từng row trong memory

Ưu điểm:

- giảm số query rất mạnh
- tăng throughput
- dễ triển khai

Nhược điểm:

- nếu `IN (...)` quá lớn thì query vẫn nặng
- cần chọn chunk size hợp lý

### 8.2. Khi nào dùng SQL validation trên staging

Nên dùng khi:

- rule phụ thuộc set dữ liệu lớn
- cần join với bảng DB rất lớn
- cần bắt duplicate hoặc conflict toàn job

Ví dụ:

- nạp dữ liệu vào staging
- chạy SQL join giữa staging và bảng chính
- đánh dấu các row lỗi

Khuyến nghị:

- bảng tham chiếu nhỏ: có thể preload toàn bộ
- bảng vừa hoặc lớn: prefetch theo chunk
- rule toàn file hoặc join lớn: staging + SQL validation

---

## 9. Staging table là gì

`Staging table` là bảng tạm dùng để chứa dữ liệu import trước khi ghi vào bảng chính.

Thường có các bảng:

- `import_job`
- `import_row_staging`
- `import_error`

Mục tiêu:

- không ghi trực tiếp vào bảng chính ngay
- hỗ trợ validate nhiều pha
- hỗ trợ retry
- hỗ trợ report lỗi theo row
- hỗ trợ all-or-nothing ở mức nghiệp vụ

Ưu điểm:

- an toàn
- dễ audit
- dễ rollback ở cấp độ business
- dễ re-run bước merge

Nhược điểm:

- phức tạp hơn
- tốn dung lượng lưu dữ liệu tạm
- cần dọn dẹp định kỳ

Nên dùng khi:

- dữ liệu lớn
- validate phức tạp
- cần progress, retry, report tốt
- cần kiểm soát consistency cao

---

## 10. Transaction theo chunk và câu hỏi "lỗi về sau thì sao"

Không nên mở một transaction cho toàn bộ file lớn.

Thay vào đó:

- mỗi chunk xử lý trong transaction ngắn
- commit vào staging
- sang chunk tiếp theo

### 10.1. Vì sao làm vậy

- transaction ngắn hơn
- lock ngắn hơn
- rollback ít tốn kém hơn
- giữ connection ngắn hơn

### 10.2. Nếu chunk sau lỗi nhưng chunk trước đã commit thì sao

Điểm quan trọng là:

- commit các chunk vào `staging`
- chưa commit vào `main table`

Khi đó nếu về sau phát hiện lỗi:

- dữ liệu chính chưa bị ảnh hưởng
- job có thể kết thúc ở trạng thái lỗi
- user xem report rồi sửa file
- có thể import lại mà không làm hỏng main table

### 10.3. All-or-nothing và partial import

#### All-or-nothing

- nạp tất cả vào staging
- validate toàn bộ
- chỉ merge sang bảng chính khi toàn file hợp lệ

Ưu điểm:

- an toàn hơn về business

Nhược điểm:

- phải staging và finalize rõ ràng

#### Partial import

- row đúng thì được merge
- row lỗi thì ghi báo cáo

Ưu điểm:

- linh hoạt
- user không phải sửa toàn bộ file

Nhược điểm:

- dữ liệu vào bảng chính không nguyên khối
- cần business đồng ý rõ mode này

---

## 11. Decision guide: khi nào chọn solution nào

### 11.1. Nếu dữ liệu nhỏ

Chọn:

- sync import
- validate row-by-row
- save trực tiếp

Không cần:

- async
- staging
- progress tracking phức tạp

### 11.2. Nếu dữ liệu vừa

Chọn:

- sync hoặc async nhẹ
- batch read
- prefetch DB
- batch insert

Không nên:

- query DB từng row

### 11.3. Nếu dữ liệu trung bình

Chọn:

- async processing
- chunk read
- job tracking
- progress tracking
- batch validation

### 11.4. Nếu dữ liệu lớn

Chọn:

- async processing
- chunk read
- staging table
- validate 2 pha
- short transactions
- merge cuối

### 11.5. Nếu dữ liệu rất lớn

Chọn:

- batch pipeline đầy đủ
- streaming reader
- worker riêng hoặc queue
- staging đầy đủ
- SQL validation trên staging
- progress, retry, audit

---

## 12. Bộ ưu tiên cho file 50k+ row

Khi file từ khoảng 50k row trở lên, thứ tự ưu tiên thường là:

1. `async processing`
2. `chunk read`
3. `staging table`
4. `batch validation`
5. `batch insert`
6. `prefetch reference data`
7. `short transactions`
8. `progress tracking`

### 12.1. Vì sao lại theo thứ tự này

`async processing`

- vì request HTTP không phù hợp cho tác vụ kéo dài

`chunk read`

- vì không thể giữ toàn bộ file trong memory

`staging table`

- vì không nên ghi trực tiếp bảng chính khi dữ liệu lớn và validate phức tạp

`batch validation`

- vì validate từng row một với DB sẽ rất chậm

`batch insert`

- vì insert từng row không đạt throughput tốt

`prefetch reference data`

- vì cần giảm số query DB

`short transactions`

- vì transaction dài dễ gây lock và rollback đắt

`progress tracking`

- vì user và vận hành cần biết job chạy đến đâu

---

## 13. Những chỗ thường làm sai

- query DB từng row
- giữ toàn bộ file trong `List<RowDto>`
- dùng một transaction cho cả file
- import đồng bộ qua HTTP request cho file rất lớn
- ghi thẳng vào bảng chính dù business validation còn phức tạp
- không phân biệt lỗi format và lỗi business
- không có error report theo row
- không có progress tracking cho job dài
- không có cleanup dữ liệu staging

---

## 14. Khuyến nghị mặc định cho hệ thống business

### 14.1. Nếu dưới 10k row

- sync hoặc async nhẹ
- validate theo block nhỏ
- batch insert

### 14.2. Nếu 10k-50k row

- async
- chunk read
- prefetch DB
- progress tracking

### 14.3. Nếu 50k-200k row

- async
- chunk read
- staging
- error report chi tiết
- merge cuối

### 14.4. Nếu trên 200k row, đặc biệt 500k-1m+

- batch pipeline hoàn chỉnh
- background worker
- streaming hoặc chunk reader
- staging đầy đủ
- validate nhiều pha
- finalize hoặc merge có kiểm soát

---

## 15. Kết luận

Với import Excel, không có một solution duy nhất cho mọi trường hợp.

Nguyên tắc chọn kiến trúc:

- file càng lớn, càng phải tách upload, validate, merge
- validation càng phức tạp, càng không nên ghi thẳng bảng chính
- càng nhiều check DB, càng phải tránh query từng row
- càng nhiều rule toàn file, càng nên dùng staging và validation pha 2
- nếu import kéo dài hơn vài giây, nên nghĩ tới async
- nếu cần all-or-nothing ở mức nghiệp vụ, nên staging rồi merge cuối

Tóm lại:

- nhỏ: giữ đơn giản
- vừa: thêm batch
- trung bình: thêm async và progress
- lớn: thêm staging và validate nhiều pha
- rất lớn: coi như ETL hoặc batch pipeline
