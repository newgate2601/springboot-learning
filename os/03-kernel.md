# Kernel — Nhân hệ điều hành

## 1. Kernel là gì?

Kernel là phần lõi của hệ điều hành, hoạt động từ khi máy khởi động cho đến khi tắt máy.
Nó chạy với quyền cao nhất và làm trung gian giữa chương trình với tài nguyên của máy tính.

Kernel không phải là toàn bộ hệ điều hành. Hệ điều hành còn bao gồm thư viện hệ thống,
dịch vụ nền, công cụ dòng lệnh, giao diện người dùng và nhiều tiện ích khác.

Ứng dụng không được tự do truy cập trực tiếp CPU, bộ nhớ hoặc thiết bị. Khi cần sử dụng
các tài nguyên này, ứng dụng phải gửi yêu cầu cho kernel thông qua API của hệ điều hành
và các **system call**.

## 2. Kernel được sử dụng để làm gì?

Kernel chịu trách nhiệm quản lý và bảo vệ các tài nguyên quan trọng của hệ thống.

### Quản lý CPU và thread

Kernel quyết định thread nào được chạy, chạy trên CPU nào và chạy trong bao lâu. Thành
phần thực hiện công việc này được gọi là **scheduler**.

Nhờ đó, nhiều chương trình có thể cùng hoạt động dù số lượng thread thường lớn hơn số
lõi CPU vật lý.

### Quản lý process

Kernel tạo, theo dõi và kết thúc process. Mỗi process được cấp một mã định danh, vùng nhớ
và tập tài nguyên riêng như file, socket và thread.

Sự quản lý này giúp các chương trình hoạt động tương đối độc lập và hạn chế một process
làm ảnh hưởng trực tiếp đến process khác.

### Quản lý bộ nhớ

Kernel phối hợp với phần cứng để cấp phát bộ nhớ, tạo không gian địa chỉ riêng cho từng
process và kiểm tra quyền đọc, ghi, thực thi.

Nó cũng xử lý các tình huống như thiếu bộ nhớ, page fault, swap và thu hồi vùng nhớ không
còn được sử dụng.

### Quản lý file và thiết bị

Kernel cung cấp cách làm việc thống nhất với file, thư mục, ổ đĩa, bàn phím, màn hình,
card mạng và các thiết bị khác.

Nó kiểm tra quyền truy cập, quản lý trạng thái mở file, cache dữ liệu và chuyển yêu cầu
của chương trình tới driver phù hợp.

### Quản lý mạng

Kernel quản lý socket và các tầng mạng thấp như TCP/IP, port, routing, buffer và việc
truyền nhận packet.

Các giao thức tầng cao như HTTP, JSON hoặc logic API thường do thư viện, framework và
ứng dụng xử lý.

### Bảo vệ hệ thống

Kernel kiểm tra quyền trước khi cho phép process truy cập file, bộ nhớ, thiết bị hoặc
process khác.

Nó tạo ra ranh giới giữa **user mode** và **kernel mode**, giúp lỗi của một ứng dụng
không dễ dàng phá hỏng toàn bộ hệ thống.

## 3. Kernel làm việc với các thành phần khác như thế nào?

Mối quan hệ tổng quát có thể hiểu như sau:

```text
Application
    ↓
Runtime hoặc thư viện hệ thống
    ↓
System call
    ↓
Kernel
    ↓
Driver
    ↓
Phần cứng
```

### Với application

Application thực hiện nghiệp vụ nhưng không trực tiếp điều khiển phần cứng. Khi cần đọc
file, tạo process, cấp phát bộ nhớ hoặc gửi dữ liệu qua mạng, application yêu cầu kernel
thực hiện.

Kernel chỉ hiểu yêu cầu tài nguyên và dữ liệu ở mức hệ thống. Nó không hiểu nghiệp vụ
của ứng dụng.

### Với runtime và framework

Runtime như JVM chuyển các thao tác cấp cao của Java thành lời gọi phù hợp với hệ điều
hành. Framework như Spring Boot tiếp tục hoạt động ở tầng cao hơn JVM.

Vì vậy:

- Spring Boot quản lý bean, controller và cấu hình ứng dụng.
- JVM thực thi bytecode, quản lý heap và garbage collection.
- Kernel cấp CPU, bộ nhớ, thread, file và socket cho JVM.

### Với driver

Driver là thành phần giúp kernel giao tiếp với từng loại thiết bị cụ thể. Kernel đưa ra
yêu cầu ở mức hệ thống; driver chuyển yêu cầu đó thành thao tác mà thiết bị hiểu được.

Kernel quản lý driver và tạo một giao diện chung để application không cần biết chi tiết
hoạt động của từng loại phần cứng.

### Với phần cứng

CPU thực thi lệnh và hỗ trợ phân chia user mode với kernel mode. MMU hỗ trợ ánh xạ và
bảo vệ bộ nhớ. Các thiết bị gửi interrupt để báo cho kernel khi có sự kiện hoặc khi một
thao tác I/O hoàn tất.

Kernel phối hợp các cơ chế này để phân bổ tài nguyên và phản hồi yêu cầu của chương trình.

### Với container

Container không có kernel riêng như một máy ảo hoàn chỉnh. Các process trong container
vẫn sử dụng kernel của máy chủ.

Kernel dùng namespace để cô lập tài nguyên và cgroup để giới hạn CPU, bộ nhớ cùng các
tài nguyên khác của container.

## 4. Kernel hoạt động khi nào?

Kernel được bootloader nạp vào bộ nhớ khi máy khởi động. Sau đó, nó hoạt động trong toàn
bộ thời gian hệ thống chạy.

Phần lớn code ứng dụng chạy ở user mode. CPU chỉ chuyển sang kernel mode khi có system
call, interrupt hoặc exception. Xử lý xong, kernel trả quyền thực thi cho ứng dụng hoặc
chọn một thread khác để chạy.

Kernel không phải là một application process thông thường. Tuy nhiên, nó có thể tạo các
kernel thread để thực hiện công việc nền của hệ thống.

## 5. Các kiến trúc kernel phổ biến

| Kiến trúc | Đặc điểm |
|---|---|
| Monolithic kernel | Nhiều subsystem và driver chạy trong kernel space; hiệu năng giao tiếp tốt nhưng lỗi có thể ảnh hưởng rộng. |
| Microkernel | Chỉ giữ các chức năng thiết yếu trong kernel; nhiều service chạy ở user space để tăng khả năng cô lập. |
| Hybrid kernel | Kết hợp đặc điểm của monolithic kernel và microkernel theo nhu cầu của hệ điều hành. |

Linux thường được xếp vào monolithic kernel có hỗ trợ module. Windows NT và XNU thường
được mô tả là hybrid kernel.

## 6. Tóm tắt

> Kernel là lõi đặc quyền của hệ điều hành, đứng giữa application và phần cứng để quản
> lý tài nguyên, thực thi quyền truy cập và cung cấp các dịch vụ hệ thống thông qua
> system call.
