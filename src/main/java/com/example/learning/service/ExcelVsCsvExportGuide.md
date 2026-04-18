# Excel Vs CSV Cho Bài Toán Export

Tài liệu này không bàn về số benchmark cụ thể. Mục tiêu là làm rõ bản chất khác nhau giữa `Excel` và `CSV`, khi nào nên chọn mỗi loại, và cách suy nghĩ thực dụng khi thiết kế tính năng export trong hệ thống backend.

## 1. Khác nhau ở bản chất

## Excel là định dạng tài liệu

Khi nói `Excel export`, đa số trường hợp đang nói đến file `.xlsx`.

`.xlsx` không chỉ là dữ liệu. Nó là một tài liệu bảng tính có cấu trúc:

- workbook
- sheet
- row
- cell
- style
- format số/ngày giờ
- merge cell
- freeze pane
- filter
- formula
- nhiều sheet trong cùng một file

Nói ngắn gọn:

- Excel không chỉ chứa dữ liệu
- Excel còn chứa cách trình bày dữ liệu

## CSV là định dạng dữ liệu

`CSV` chỉ là text theo dạng:

- mỗi dòng là một record
- mỗi cột được ngăn bởi dấu phẩy hoặc ký tự phân tách khác

CSV không có:

- style
- màu sắc
- merge
- freeze pane
- formula thực thụ
- nhiều sheet
- metadata trình bày

Nói ngắn gọn:

- CSV là dữ liệu thô
- Excel là tài liệu bảng tính

## 2. Nếu user mở bằng Excel thì CSV có phải là Excel không

Không.

CSV không phải file Excel native.

Nhưng trong thực tế:

- người dùng vẫn có thể mở CSV bằng Microsoft Excel
- nhiều team business vẫn gọi chung đó là "file Excel"

Vì vậy cần tách rõ hai yêu cầu:

1. user cần `mở bằng Excel`
2. user cần `định dạng .xlsx`

Hai yêu cầu này khác nhau hoàn toàn.

Nếu user chỉ cần:

- tải dữ liệu
- mở lên bằng Excel
- lọc / sort / pivot thủ công

thì CSV thường đã đủ.

Nếu user cần:

- file có định dạng đẹp
- nhiều sheet
- cột đã format sẵn
- heading, title, merge cell
- công thức có sẵn

thì phải dùng Excel thật sự.

## 3. Excel mạnh ở đâu

Excel phù hợp khi export mang tính:

- báo cáo
- trình bày
- gửi khách hàng
- gửi sếp
- backoffice thao tác trực quan

Các thế mạnh của Excel:

- hiển thị đẹp hơn
- format số, tiền, ngày giờ tốt hơn
- có thể freeze header
- có thể filter sẵn
- có thể nhiều sheet
- có thể nhúng formula
- có trải nghiệm người dùng tốt hơn với file báo cáo

Excel đặc biệt hợp với:

- báo cáo tài chính
- báo cáo doanh thu
- bảng đối soát
- file đối chiếu có highlight hoặc format
- export có summary sheet và detail sheet

## 4. CSV mạnh ở đâu

CSV phù hợp khi export mang tính:

- trích xuất dữ liệu thô
- dump dữ liệu
- nhập sang hệ thống khác
- tải dữ liệu lớn
- xử lý bằng script hoặc ETL

Các thế mạnh của CSV:

- đơn giản
- nhẹ
- dễ stream
- dễ tạo
- dễ đọc bằng nhiều công cụ
- dễ import vào database, BI, Python, Spark, Excel

CSV đặc biệt hợp với:

- export dữ liệu lớn
- backup logic
- trao đổi dữ liệu giữa hệ thống
- tải file cho data analyst
- batch integration

## 5. Khi nào nên ưu tiên Excel

Nên ưu tiên Excel khi bài toán thật sự là bài toán tài liệu.

Dấu hiệu rõ ràng:

- người dùng quan tâm cách file nhìn như thế nào
- file sẽ được gửi ra ngoài cho khách hàng hoặc đối tác
- có yêu cầu format cột, màu sắc, font, border
- cần nhiều sheet
- cần formula hoặc subtotal
- cần một file "đẹp" để đọc ngay

Ví dụ:

- báo cáo tháng cho ban điều hành
- file invoice/reconciliation cho khách hàng
- file dashboard export có summary
- file nghiệp vụ mà user mở lên rồi dùng trực tiếp, gần như không cần chỉnh lại

## 6. Khi nào nên ưu tiên CSV

Nên ưu tiên CSV khi bài toán thật sự là bài toán dữ liệu.

Dấu hiệu rõ ràng:

- file rất lớn
- user chủ yếu cần lấy dữ liệu ra
- không có nhu cầu format phức tạp
- mục tiêu là throughput và ổn định hệ thống
- file sẽ được import vào hệ thống khác
- người dùng có thể tự mở bằng Excel hoặc công cụ khác

Ví dụ:

- export lịch sử giao dịch rất lớn
- export log hoặc audit data
- export order data cho team data
- export để đổ vào data warehouse
- export backoffice mà user chỉ lọc tiếp trong Excel

## 7. Khi nào nên dùng ZIP CSV

`ZIP CSV` là lựa chọn trung gian rất thực dụng.

Nó hợp khi:

- dữ liệu lớn
- CSV thường quá to
- muốn giảm băng thông
- muốn giảm dung lượng lưu trữ
- user vẫn chấp nhận tải `.zip` rồi giải nén

`ZIP CSV` thường hợp hơn `CSV plain` khi:

- export qua internet
- file lưu lâu
- số lượng record rất lớn
- cần tiết kiệm network và disk

Nó đặc biệt hợp với:

- export nền
- file tải từ object storage
- file gửi qua email hoặc internal portal

## 8. Excel có nhược điểm gì

Excel thường có các nhược điểm sau:

- tạo file phức tạp hơn
- code writer nặng hơn
- khó stream tối ưu hơn CSV
- tốn CPU hơn
- tốn RAM hơn
- phụ thuộc thư viện nhiều hơn
- dễ phát sinh issue liên quan format, cell type, shared strings, worksheet buffer

Ngoài ra, Excel còn có nhược điểm về mặt sản phẩm:

- user thường kỳ vọng file đẹp hơn theo thời gian
- càng nhiều yêu cầu formatting, file càng thành bài toán report chứ không còn là export đơn giản

Nói cách khác:

- chọn Excel thường kéo theo chi phí maintenance cao hơn CSV

## 9. CSV có nhược điểm gì

CSV cũng có các nhược điểm rõ ràng:

- không đẹp
- không có style
- không có nhiều sheet
- không có formula thực thụ
- không có typing rõ như Excel
- mở trực tiếp bằng Excel đôi khi bị hiểu sai encoding, separator, số điện thoại, mã đơn, số 0 đầu chuỗi

Đây là điểm rất quan trọng:

- CSV là text
- Excel có thể tự đoán kiểu dữ liệu khi mở CSV

Điều đó có thể gây các lỗi như:

- mất số 0 đầu
- mã dài bị chuyển scientific notation
- ngày tháng bị parse sai theo locale

Vì vậy nếu dữ liệu có nhiều field "nhìn giống số nhưng thực ra là mã", CSV cần được cân nhắc kỹ.

## 10. Suy nghĩ đúng theo mục tiêu sản phẩm

Khi thiết kế export, đừng bắt đầu bằng câu hỏi:

- dùng thư viện Excel nào

Nên bắt đầu bằng các câu hỏi:

1. user thật sự cần dữ liệu hay cần báo cáo
2. file có cần đẹp không
3. file có cần nhiều sheet không
4. file có cần mở ngay là dùng được không
5. volume dữ liệu lớn tới mức nào
6. export này là use case hàng ngày hay thỉnh thoảng
7. nếu file rất lớn thì user có chấp nhận `.csv` hoặc `.zip` không

Khi trả lời được các câu hỏi đó, lựa chọn thường sẽ rõ hơn nhiều.

## 11. Một số rule thực dụng

Có thể dùng các rule đơn giản sau:

### Chọn Excel khi

- file là báo cáo
- file cần trình bày
- cần nhiều sheet
- cần format rõ ràng
- số lượng dữ liệu không quá cực đoan
- trải nghiệm người dùng quan trọng hơn throughput tối đa

### Chọn CSV khi

- file là dữ liệu thô
- dữ liệu lớn
- cần export nhanh và ổn định
- file có thể được xử lý tiếp bằng công cụ khác
- người dùng không cần layout đẹp

### Chọn ZIP CSV khi

- vẫn là dữ liệu thô
- nhưng file rất lớn
- cần giảm kích thước tải xuống
- user chấp nhận giải nén

## 12. Góc nhìn kiến trúc hệ thống

Nếu nhìn từ phía backend:

### Excel phù hợp với

- service export báo cáo
- export có business formatting
- export có nhiều bước trình bày
- use case cần file thành phẩm

### CSV phù hợp với

- data export service
- integration export
- async export job
- background batch
- large-volume download

Nói cách khác:

- Excel gần với report generation
- CSV gần với data extraction

## 13. Kết luận ngắn gọn

Nếu mục tiêu là:

- file đẹp
- dễ đọc ngay
- có trình bày
- có nhiều sheet

thì dùng `Excel`.

Nếu mục tiêu là:

- lấy dữ liệu ra nhanh
- dữ liệu lớn
- ổn định hệ thống
- import/export liên hệ thống

thì dùng `CSV`.

Nếu mục tiêu là:

- vẫn là dữ liệu thô
- nhưng muốn file nhỏ hơn nhiều

thì dùng `ZIP CSV`.

Kết luận thực dụng nhất là:

- `Excel` dành cho báo cáo
- `CSV` dành cho dữ liệu
- `ZIP CSV` dành cho dữ liệu lớn

