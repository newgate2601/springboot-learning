# Kernel — Nhân hệ điều hành

- **Kernel** là chương trình lõi luôn hoạt động sau khi máy boot, chạy với đặc
  quyền cao nhất để quản lý CPU, memory, process, device và các system call.
- **Operating system** rộng hơn kernel, thường bao gồm kernel, system libraries,
  command-line tools, service nền, UI và các tiện ích hệ thống.

Ví dụ, Linux về mặt kỹ thuật là tên của kernel. Một bản phân phối như Ubuntu gồm
Linux kernel cộng với GNU tools, systemd, package manager, desktop environment và
nhiều thành phần khác.

> Tài liệu chuyên sâu về kernel: vai trò, cách kernel quản lý CPU, process,
> memory, file, network và thiết bị; kèm các flow thực tế với Java/Spring Boot.

## Mục lục

- [1. Kernel xuất hiện ở đâu trong ứng dụng thực tế?](#1-kernel-xuất-hiện-ở-đâu-trong-một-ứng-dụng-thực-tế)
- [2. Kernel quản lý những gì?](#2-kernel-quản-lý-những-gì)
- [3. Một yêu cầu đi qua kernel như thế nào?](#3-một-yêu-cầu-đi-qua-kernel-như-thế-nào)
- [4. Kernel bảo vệ hệ thống](#4-ví-dụ-kernel-bảo-vệ-hệ-thống)
- [5. Kernel nằm ở đâu và chạy khi nào?](#5-kernel-nằm-ở-đâu-và-chạy-khi-nào)
- [6. Kernel có phải một process không?](#6-kernel-có-phải-một-process-không)
- [7. Monolithic kernel và microkernel](#7-monolithic-kernel-và-microkernel)
- [8. Quan sát kernel trong thực tế](#8-ví-dụ-quan-sát-kernel-trong-thực-tế)
- [9. Tóm tắt](#9-tóm-tắt-kernel-trong-một-câu)

---

## 1. Kernel xuất hiện ở đâu trong một ứng dụng thực tế?

Giả sử chạy Spring Boot:

```powershell
java -jar order-service.jar
```

Lệnh này dẫn tới một chuỗi công việc thực tế:

```text
PowerShell yêu cầu OS tạo process
→ kernel tạo PID và thông tin quản lý process
→ kernel thiết lập virtual address space
→ kernel mở java.exe và các DLL/JAR cần thiết
→ kernel ánh xạ code/data vào memory
→ kernel tạo thread ban đầu
→ scheduler cho thread của JVM chạy trên CPU
→ JVM khởi động Spring Boot
```

Kernel không khởi tạo Spring Bean và không hiểu `@RestController`. Đó là công
việc của JVM và Spring. Kernel cung cấp nền tảng thấp hơn để JVM có CPU, memory,
file, thread và network socket.

Sau khi ứng dụng chạy, có thể kiểm tra process thật:

```powershell
Get-Process java | Select-Object Id, ProcessName, CPU, WorkingSet, Threads
```

Kết quả cho thấy PID, thời gian CPU, lượng memory và danh sách thread mà OS đang
quản lý cho Java process.

**Tình huống 1 — Spring Boot đọc `application.yml`**

```text
Spring gọi Java File API
→ JVM yêu cầu OS mở file
→ kernel kiểm tra đường dẫn và quyền truy cập
→ kernel lấy dữ liệu từ page cache hoặc yêu cầu SSD đọc
→ kernel trả các byte cho JVM
→ Spring parse YAML và tạo configuration
```

Kernel biết file, quyền truy cập và các byte; kernel không biết YAML property như
`server.port` có ý nghĩa gì.

**Tình huống 2 — Client gọi REST API**

```text
Client gửi TCP packet
→ card mạng nhận packet
→ driver và kernel network stack xử lý packet
→ kernel tìm socket đang listen ở port 8080
→ dữ liệu được đưa vào socket receive buffer
→ kernel đánh thức thread Java đang chờ
→ Tomcat/Spring parse HTTP request
→ controller xử lý business logic
```

Nếu không có process nào listen ở port đó, kernel không thể chuyển connection cho
Spring Boot và client có thể nhận lỗi `connection refused`.

Có thể quan sát port do Java process sử dụng:

```powershell
Get-NetTCPConnection -LocalPort 8080 |
    Select-Object LocalAddress, LocalPort, State, OwningProcess
```

`OwningProcess` chính là PID mà kernel dùng để liên kết socket với process.

**Tình huống 3 — Repository gọi PostgreSQL**

```text
Java/JDBC tạo dữ liệu query
→ JVM ghi byte vào socket
→ kernel đặt byte vào socket send buffer
→ TCP/IP stack của kernel tạo segment
→ network driver yêu cầu NIC gửi dữ liệu
→ thread Java chờ response
→ scheduler cho CPU chạy thread/process khác
→ response tới, kernel đánh thức thread Java
```

Trong lúc chờ database, CPU không cần đứng yên. Kernel chuyển CPU cho công việc
khác. Đây là nền tảng của concurrency khi thực hiện I/O.

**Tình huống 4 — Hai ứng dụng cùng ghi một file**

Giả sử hai process cùng ghi `orders.log`. Kernel và file system phải quản lý:

- File descriptor/handle của từng process.
- File offset.
- Permission.
- Buffer/cache.
- Thứ tự các thao tác ghi.
- File lock nếu application yêu cầu.

Nếu application không đồng bộ đúng, nội dung vẫn có thể xen kẽ. Kernel cung cấp
primitive và bảo vệ quyền truy cập, nhưng không tự hiểu mỗi log entry phải là một
business transaction nguyên vẹn.

**Tình huống 5 — Java process dùng quá nhiều memory**

```text
JVM yêu cầu thêm memory
→ kernel kiểm tra virtual memory và giới hạn process/container
→ kernel cấp thêm page nếu có thể
→ nếu memory pressure cao, kernel có thể reclaim cache hoặc swap
→ nếu không thể đáp ứng, allocation thất bại hoặc process bị chấm dứt
```

Nếu JVM chạm giới hạn `-Xmx`, JVM có thể ném `OutOfMemoryError` trước khi toàn bộ
RAM của OS cạn. Nếu container vượt memory limit, kernel/cgroup có thể kill process
và Docker hiển thị exit code `137`.

**Tình huống 6 — Java process bị dừng**

```powershell
Stop-Process -Id <PID>
```

PowerShell không tự xóa process khỏi CPU/RAM. Nó yêu cầu OS; kernel xác định PID,
kiểm tra quyền, dừng các thread và thu hồi memory, socket, handle cùng tài nguyên
liên quan.

## 2. Kernel quản lý những gì?

**1. CPU và thread**

Kernel scheduler theo dõi các thread có thể chạy, chọn thread tiếp theo và phân
phối thời gian CPU.

Ví dụ, máy có 8 logical CPU nhưng đang có 300 runnable thread. Kernel không thể
cho tất cả chạy vật lý cùng lúc. Nó liên tục lựa chọn và chuyển đổi giữa chúng để
hệ thống vẫn phản hồi.

```text
Chrome threads ─┐
Java threads  ──┼─→ Kernel scheduler ─→ CPU 0...7
Docker threads ─┤
System threads ─┘
```

**2. Process**

Kernel:

- Tạo và kết thúc process.
- Gán PID.
- Lưu trạng thái thực thi.
- Cô lập address space.
- Theo dõi file, socket và quyền của process.
- Cho parent process nhận exit status của child process.

Khi chạy:

```bash
java -jar order-service.jar
```

shell yêu cầu kernel tạo process. Kernel tạo thông tin quản lý process, thiết lập
virtual address space, nạp executable/runtime cần thiết và tạo thread ban đầu để
JVM bắt đầu chạy.

**3. Bộ nhớ**

Kernel không chỉ “chia RAM thành từng phần”. Nó phối hợp với MMU của CPU để:

- Tạo virtual address space riêng cho từng process.
- Quản lý page table.
- Ánh xạ virtual page tới physical frame.
- Kiểm tra quyền read/write/execute.
- Xử lý page fault.
- Thu hồi hoặc swap page khi memory pressure cao.

Ví dụ, hai process đều có thể nhìn thấy địa chỉ ảo `0x1000`, nhưng kernel có thể
ánh xạ chúng tới hai vùng RAM vật lý hoàn toàn khác nhau.

```text
Process A: virtual 0x1000 ─→ physical frame 42
Process B: virtual 0x1000 ─→ physical frame 91
```

Vì vậy Process A không thể đọc dữ liệu của Process B chỉ bằng cách dùng cùng một
địa chỉ ảo.

**4. Thiết bị và I/O**

Kernel phối hợp với driver để điều khiển thiết bị:

```text
Application
→ system call
→ kernel I/O subsystem
→ device driver
→ device controller
→ hardware
```

Khi application đọc file, kernel kiểm tra file descriptor và permission, tìm dữ
liệu trong page cache hoặc yêu cầu storage driver đọc từ SSD. Nếu phải chờ thiết
bị, kernel có thể block thread hiện tại rồi cho CPU chạy thread khác.

**5. File system**

Kernel cung cấp abstraction file/directory và thực hiện:

- Phân giải đường dẫn.
- Kiểm tra quyền.
- Quản lý open-file state và file offset.
- Cache dữ liệu.
- Chuyển thao tác logic thành thao tác trên file system/storage.

Khi hai process cùng mở `orders.log`, mỗi process có thể có file descriptor riêng,
trong khi kernel liên kết chúng với file và trạng thái mở tương ứng.

**6. Networking**

Kernel thường triển khai phần quan trọng của TCP/IP stack và socket:

```text
Spring Boot
→ socket read/write
→ kernel TCP
→ kernel IP
→ network driver
→ NIC
```

Kernel xử lý các công việc như chia TCP segment, sequence number, retransmission,
port, routing và socket buffer. HTTP/JSON ở tầng cao hơn thường do framework hoặc
application xử lý.

**7. Protection và permission**

Kernel là nơi thực thi ranh giới bảo vệ. Application không thể tự tuyên bố “tôi có
quyền admin”. Mỗi system call đều có thể được kernel kiểm tra dựa trên:

- User/group hoặc security token.
- Permission/ACL.
- Process capability.
- Namespace, cgroup hoặc sandbox policy.
- Trạng thái và ownership của resource.

## 3. Một yêu cầu đi qua kernel như thế nào?

Ví dụ Java đọc file:

```java
String content = Files.readString(Path.of("config.json"));
```

Flow rút gọn:

```text
1. Java code gọi Files.readString
2. Java library/JVM gọi native OS API
3. CPU chuyển từ user mode sang kernel mode qua system call
4. Kernel kiểm tra path và permission
5. Kernel tìm dữ liệu trong page cache
6. Nếu cache miss, kernel yêu cầu driver đọc SSD
7. Thread Java có thể bị block; scheduler chạy thread khác
8. Thiết bị hoàn tất và thông báo cho kernel
9. Kernel đưa dữ liệu vào memory/buffer phù hợp
10. System call trả result về user mode
11. Java tạo String và tiếp tục thực thi
```

Điểm quan trọng: kernel không parse JSON và không tạo Java `String`. Kernel chỉ
cung cấp dữ liệu byte và cơ chế I/O; Java library/JVM xử lý abstraction ở tầng
ngôn ngữ.

## 4. Ví dụ kernel bảo vệ hệ thống

Giả sử một process cố ghi vào memory không thuộc quyền của nó:

```c
int *address = (int *)0x123;
*address = 10;
```

Flow có thể là:

```text
CPU thực hiện instruction ghi
→ MMU phát hiện page không hợp lệ/không có quyền write
→ CPU trap vào kernel
→ kernel xác định đây là invalid memory access
→ kernel gửi signal hoặc kết thúc process
```

Trên Linux, application thường nhận `SIGSEGV`; trên Windows thường thấy access
violation. Kernel dừng process lỗi thay vì cho nó ghi tùy ý và phá dữ liệu hệ
thống.

## 5. Kernel nằm ở đâu và chạy khi nào?

Kernel image được lưu trên SSD/HDD. Trong quá trình boot, bootloader nạp kernel
vào RAM và chuyển quyền điều khiển cho nó.

Sau khi khởi tạo, kernel vẫn hoạt động trong suốt thời gian máy chạy. Tuy nhiên
không nên hiểu rằng CPU chỉ chạy kernel:

```text
Application chạy ở user mode
→ có interrupt/system call/exception
→ CPU chạy kernel
→ kernel xử lý xong
→ CPU quay lại application hoặc chạy thread khác
```

Kernel sử dụng một vùng memory được bảo vệ. User process thông thường không được
đọc hoặc ghi trực tiếp vùng này.

## 6. Kernel có phải một process không?

Không theo nghĩa application process thông thường.

Kernel là môi trường đặc quyền quản lý toàn hệ thống. Kernel có thể tạo các
**kernel thread** để thực hiện công việc nền, nhưng điều đó không biến toàn bộ
kernel thành một user-space process.

Ví dụ trên Linux có thể thấy các kernel thread như `kworker`. Chúng làm công việc
cho kernel và không có user address space giống process Java thông thường.

## 7. Monolithic kernel và microkernel

| Kiến trúc | Ý tưởng chính | Ví dụ khái quát |
|---|---|---|
| Monolithic kernel | Nhiều subsystem và driver chạy trong kernel space | Linux |
| Microkernel | Giữ cơ chế tối thiểu trong kernel; đẩy nhiều service sang user space | MINIX 3, QNX |
| Hybrid kernel | Kết hợp ý tưởng của cả hai theo thiết kế thực tế | Windows NT, XNU |

Monolithic không có nghĩa là “một file khổng lồ không module”. Linux hỗ trợ
loadable kernel module. Microkernel cũng không được định nghĩa đơn giản là “chỉ
nạp phần cần dùng để tiết kiệm RAM”.

Trade-off thường gặp:

- Đưa nhiều thành phần vào kernel space có thể giao tiếp nhanh, nhưng bug driver
  có khả năng ảnh hưởng toàn hệ thống.
- Đưa service sang user space tăng isolation/modularity, nhưng có thể làm tăng
  chi phí IPC và chuyển đổi context.

## 8. Ví dụ quan sát kernel trong thực tế

Trên Linux:

```bash
# Phiên bản kernel
uname -r

# Message của kernel
dmesg

# Các kernel module đang được nạp
lsmod

# Theo dõi system call của một chương trình
strace -f java -jar order-service.jar
```

Trên Windows PowerShell:

```powershell
# Thông tin OS và kernel build
Get-ComputerInfo | Select-Object WindowsProductName, WindowsVersion, OsBuildNumber

# Driver hệ thống
Get-CimInstance Win32_SystemDriver | Select-Object Name, State, StartMode
```

`strace` là ví dụ đặc biệt hữu ích: nó cho thấy application tưởng như chỉ đang
“đọc file” hoặc “gọi API” nhưng bên dưới phát sinh các system call như `openat`,
`read`, `write`, `mmap`, `futex` và `close`.

## 9. Tóm tắt kernel trong một câu

> Kernel là lõi đặc quyền của hệ điều hành, đứng giữa application và phần cứng để
> phân phối tài nguyên, thực thi ranh giới bảo vệ và cung cấp các dịch vụ thấp
> tầng thông qua system call.

---
