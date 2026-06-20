# Operating System — Tổng quan hệ điều hành

> Tài liệu này trình bày các khái niệm cốt lõi của hệ điều hành theo hướng thực tế:
> từ lúc bật máy, chạy một chương trình, cấp CPU/RAM, đọc file, gọi API qua mạng,
> cho đến process, thread và Java Virtual Thread.

## Mục lục

- [1. Bức tranh tổng thể](#1-bức-tranh-tổng-thể)
- [2. Hệ điều hành là gì?](#2-hệ-điều-hành-là-gì)
- [3. User mode, kernel mode và system call](#3-user-mode-kernel-mode-và-system-call)
- [4. Các dịch vụ chính của hệ điều hành](#4-các-dịch-vụ-chính-của-hệ-điều-hành)
- [5. Quá trình khởi động máy tính](#5-quá-trình-khởi-động-máy-tính)
- [6. Program, process và không gian bộ nhớ](#6-program-process-và-không-gian-bộ-nhớ)
- [7. Vòng đời của process và PCB](#7-vòng-đời-của-process-và-pcb)
- [8. CPU scheduling và context switch](#8-cpu-scheduling-và-context-switch)
- [9. Thread và multithreading](#9-thread-và-multithreading)
- [10. Java Platform Thread và Virtual Thread](#10-java-platform-thread-và-virtual-thread)
- [11. Quản lý bộ nhớ](#11-quản-lý-bộ-nhớ)
- [12. I/O, driver, interrupt và DMA](#12-io-driver-interrupt-và-dma)
- [13. File system](#13-file-system)
- [14. Giao tiếp giữa các process và networking](#14-giao-tiếp-giữa-các-process-và-networking)
- [15. Protection và security](#15-protection-và-security)
- [16. Ví dụ xuyên suốt: một HTTP request trong Spring Boot](#16-ví-dụ-xuyên-suốt-một-http-request-trong-spring-boot)
- [17. Các hiểu lầm thường gặp](#17-các-hiểu-lầm-thường-gặp)
- [18. Câu hỏi tự kiểm tra](#18-câu-hỏi-tự-kiểm-tra)

---

## 1. Bức tranh tổng thể

Một hệ thống máy tính có thể nhìn theo bốn lớp:

```text
Người dùng
    ↓
Ứng dụng: Chrome, IntelliJ, Postman, Spring Boot...
    ↓
Hệ điều hành: Windows, Linux, macOS...
    ↓
Phần cứng: CPU, RAM, SSD, GPU, card mạng...
```

![Quan hệ giữa user, application, OS và hardware](Operation System.assets/image1.png)

| Lớp | Vai trò | Ví dụ |
|---|---|---|
| Hardware | Cung cấp tài nguyên vật lý | CPU, RAM, SSD, NIC, GPU |
| Operating system | Quản lý và trừu tượng hóa phần cứng | Windows, Linux, macOS |
| System software | Hỗ trợ hệ thống hoạt động | OS, driver, runtime, system service |
| Application software | Giải quyết nhu cầu của người dùng | Word, Chrome, Postman, game |

Ứng dụng hầu như không tự điều khiển phần cứng. Nó yêu cầu hệ điều hành thực hiện
công việc thông qua API, thư viện và system call.

Ví dụ, khi Java đọc một file:

```text
Java application
→ Java I/O API
→ JVM/native runtime
→ system call của OS
→ file system
→ storage driver
→ SSD
```

Sự phân lớp này mang lại ba lợi ích quan trọng:

1. Ứng dụng không cần biết chi tiết từng model SSD, card mạng hoặc bàn phím.
2. Hệ điều hành có thể kiểm tra quyền trước khi cho ứng dụng dùng tài nguyên.
3. Nhiều chương trình có thể chia sẻ phần cứng mà không tùy ý phá dữ liệu của nhau.

---

## 2. Hệ điều hành là gì?

**Operating System (OS) — hệ điều hành** là phần mềm hệ thống quản lý tài nguyên
máy tính và cung cấp môi trường để chương trình chạy an toàn, thuận tiện, hiệu quả.

Có thể nhìn OS qua ba vai trò chính.

### 2.1. Resource manager — bộ quản lý tài nguyên

OS quyết định:

- Process/thread nào được dùng CPU và trong bao lâu.
- Vùng RAM nào được cấp cho process nào.
- Ai được đọc hoặc ghi một file.
- Dữ liệu nào cần gửi tới ổ đĩa, màn hình hoặc card mạng.
- Khi nào tài nguyên được thu hồi.

Ví dụ: Chrome, IntelliJ và Docker cùng cần CPU và RAM. OS phải phân phối tài
nguyên để cả ba có thể tiến triển, đồng thời ngăn một process đọc tùy ý bộ nhớ của
process khác.

### 2.2. Control program — chương trình kiểm soát

OS kiểm soát việc thực thi chương trình và xử lý các tình huống bất thường:

- Truy cập vùng nhớ không hợp lệ.
- Chia cho 0 hoặc instruction không hợp lệ.
- Thiết bị I/O báo lỗi.
- Process treo hoặc bị buộc kết thúc.
- User không có quyền truy cập tài nguyên.

### 2.3. Abstraction provider — lớp trừu tượng

OS che giấu chi tiết phần cứng bằng các abstraction dễ sử dụng:

| Chi tiết vật lý | Abstraction do OS cung cấp |
|---|---|
| Các core CPU | Process, thread |
| Ô nhớ vật lý | Virtual address space |
| Block trên SSD | File, directory |
| Card mạng | Socket |
| Thiết bị I/O | File descriptor/handle, stream |

Nhờ đó, developer làm việc với `file`, `socket`, `process` thay vì phải điều khiển
trực tiếp sector ổ đĩa, thanh ghi thiết bị hoặc tín hiệu điện.

### 2.4. Kernel và hệ điều hành

**Kernel** là lõi đặc quyền của hệ điều hành. Nó quản lý CPU, process/thread,
virtual memory, file system, network và thiết bị; application yêu cầu các dịch vụ
này thông qua system call.

Kernel không phải toàn bộ OS. Một hệ điều hành hoàn chỉnh còn có system library,
service nền, command-line tool, giao diện người dùng và các tiện ích khác.

Ví dụ, Linux là kernel; Ubuntu là một hệ điều hành/distribution gồm Linux kernel
cùng nhiều công cụ và phần mềm user space.

Phần giải thích chuyên sâu, flow Spring Boot và ví dụ quan sát thực tế được tách
tại [03-kernel.md](03-kernel.md).

## 3. User mode, kernel mode và system call

### 3.1. Vì sao cần hai chế độ thực thi?

Nếu mọi ứng dụng đều có toàn quyền điều khiển phần cứng thì một bug nhỏ cũng có
thể ghi đè bộ nhớ kernel, đọc dữ liệu của process khác hoặc làm sập toàn hệ thống.

CPU và OS vì vậy phân tách ít nhất hai mức đặc quyền:

| Chế độ | Đặc điểm |
|---|---|
| User mode | Chạy application; bị giới hạn quyền truy cập |
| Kernel mode | Chạy kernel; có thể thực hiện instruction đặc quyền và quản lý hardware |

Application chạy trong user mode. Khi cần thao tác đặc quyền, nó phải đi qua cổng
kiểm soát của kernel: **system call**.

### 3.2. System call là gì?

System call là cơ chế cho phép chương trình ở user mode yêu cầu kernel thực hiện
một công việc.

Một số nhóm system call phổ biến trên Unix-like OS:

| Nhóm | Ví dụ |
|---|---|
| Process | `fork`, `execve`, `exit`, `wait` |
| File | `open`, `read`, `write`, `close` |
| Memory | `mmap`, `munmap` |
| Network | `socket`, `bind`, `connect`, `send`, `recv` |
| Information | `getpid`, `clock_gettime` |

Flow khái quát:

```text
Application ở user mode
→ gọi library/API
→ đặt syscall number và arguments
→ CPU chuyển sang kernel mode
→ kernel kiểm tra quyền và thực hiện yêu cầu
→ trả result/error
→ CPU quay lại user mode
```

### 3.3. API, library và system call khác nhau thế nào?

Ba khái niệm này liên quan nhưng không đồng nhất:

| Khái niệm | Ý nghĩa |
|---|---|
| API | Giao diện mà code phía trên có thể gọi |
| Library function | Hàm được thư viện cài đặt |
| System call | Điểm vào kernel để yêu cầu dịch vụ đặc quyền |

Một library function có thể:

- Không gọi system call, ví dụ tính `strlen`.
- Gọi một system call.
- Gọi nhiều system call.
- Buffer dữ liệu và chỉ gọi system call khi cần.

Ví dụ `System.out.println("Hello")`:

```text
Java source code
→ PrintStream/JVM
→ native OS API
→ write-like system call
→ terminal
```

### 3.4. Compile-time không phải system call

Compile-time trả lời: **code được chuyển thành dạng gì để chạy?**

System call trả lời: **chương trình đang chạy nhờ OS làm việc gì?**

```text
Java source (.java)
→ javac
→ Java bytecode (.class)
→ JVM interpret/JIT compile
→ machine code
→ CPU thực thi
```

Machine code hoặc bytecode là cách biểu diễn chương trình. Chúng không phải OS API
hay system call. Chỉ khi chương trình đang chạy và cần dịch vụ của OS, nó mới phát
sinh system call.

---

## 4. Các dịch vụ chính của hệ điều hành

![Các dịch vụ của hệ điều hành](Operation System.assets/image2.png)

### 4.1. Program execution

OS hỗ trợ:

- Nạp executable và library cần thiết.
- Tạo process và virtual address space.
- Tạo thread ban đầu.
- Đưa thread vào hàng đợi sẵn sàng.
- Thu hồi tài nguyên khi process kết thúc.

Khi mở Firefox, OS không nhất thiết đọc toàn bộ chương trình vào RAM. Với demand
paging, page cần thiết thường chỉ được nạp khi được truy cập.

### 4.2. I/O operations

OS cung cấp interface thống nhất để ứng dụng đọc/ghi:

- File và ổ đĩa.
- Bàn phím, chuột, màn hình.
- Socket mạng.
- Máy in, camera, microphone.

Ứng dụng thường không nói chuyện trực tiếp với controller của thiết bị mà đi qua
kernel và driver.

### 4.3. File-system manipulation

OS hỗ trợ tạo, mở, đọc, ghi, đóng, đổi tên, di chuyển, xóa và phân quyền file.
Nó cũng quản lý metadata, cache và ánh xạ file logic xuống block của thiết bị lưu
trữ.

### 4.4. Communication

Các process có thể giao tiếp bằng:

- Pipe.
- Signal.
- Message queue.
- Shared memory.
- Socket.

Giao tiếp có thể diễn ra trong cùng máy hoặc giữa các máy qua mạng.

### 4.5. Error detection

OS phát hiện và phản ứng với:

- Page fault.
- Invalid instruction.
- I/O error.
- Network timeout.
- File system corruption.
- Hardware fault.

Tùy trường hợp, OS có thể retry, ghi log, trả error code, gửi signal, kill process
hoặc dừng hệ thống để bảo vệ dữ liệu.

### 4.6. Resource allocation và accounting

OS phân phối tài nguyên và ghi nhận mức sử dụng:

- CPU time.
- Memory.
- Disk I/O.
- Network traffic.
- Số file/socket đang mở.

Thông tin này được dùng cho monitoring, quota, billing, tuning và troubleshooting.

Ví dụ thực tế:

```bash
# Linux: xem process và mức dùng CPU/RAM
top

# Linux: xem process cụ thể
ps -o pid,ppid,stat,%cpu,%mem,cmd -p <PID>

# Windows PowerShell
Get-Process | Sort-Object CPU -Descending | Select-Object -First 10
```

### 4.7. Protection và security

OS xác thực user, kiểm tra quyền, cô lập process và bảo vệ file, memory, device,
network khỏi truy cập trái phép.

---

## 5. Quá trình khởi động máy tính

Tên đúng của quá trình này là **booting** hoặc **bootstrapping**.

```text
Power on
→ CPU chạy firmware
→ BIOS/UEFI thực hiện kiểm tra ban đầu
→ chọn boot device
→ chạy bootloader
→ nạp kernel vào RAM
→ kernel khởi tạo memory, scheduler, driver...
→ khởi chạy process/service hệ thống
→ login/desktop
```

### 5.1. Firmware: BIOS và UEFI

Khi vừa bật nguồn, RAM chưa chứa OS. CPU bắt đầu thực thi firmware được lưu trên
mainboard.

Firmware:

- Khởi tạo phần cứng cơ bản.
- Thực hiện POST (Power-On Self-Test).
- Đọc boot order.
- Tìm bootloader trên thiết bị có thể boot.

Hệ thống cũ thường dùng BIOS cùng MBR. Hệ thống hiện đại thường dùng UEFI cùng
GPT và lưu bootloader trong EFI System Partition (ESP).

### 5.2. Bootloader

Bootloader có nhiệm vụ tìm và nạp kernel vào RAM, truyền boot parameters rồi
chuyển quyền điều khiển cho kernel.

Ví dụ:

- Windows Boot Manager.
- GRUB trên nhiều hệ thống Linux.

### 5.3. Kernel initialization

Kernel khởi tạo:

- Memory management.
- Interrupt handling.
- Scheduler.
- Driver và I/O subsystem.
- File system gốc.
- Process đầu tiên ở user space.

Trên nhiều Linux distribution, process user-space đầu tiên là `systemd` với
PID 1. Nó tiếp tục khởi chạy network, logging, security, login và application
services.

### 5.4. Shutdown đúng cách quan trọng vì sao?

OS có thể đang cache dữ liệu ghi trong RAM. Nếu mất điện hoặc tắt cưỡng bức trước
khi dữ liệu được flush xuống storage, dữ liệu có thể mất hoặc file system cần
recovery. Shutdown đúng cách cho phép OS:

- Yêu cầu application kết thúc.
- Dừng service theo thứ tự.
- Flush buffer/cache.
- Unmount file system.
- Tắt thiết bị an toàn.

---

## 6. Program, process và không gian bộ nhớ

### 6.1. Program và process

| Program | Process |
|---|---|
| Tập lệnh ở trạng thái tĩnh | Một instance đang chạy của program |
| Thường nằm trên SSD/HDD | Có trạng thái thực thi trong RAM/kernel |
| Không có PID | Có PID |
| Không được scheduler lập lịch | Các thread của process được lập lịch |
| Một program có thể tạo nhiều process | Mỗi process có tài nguyên và address space riêng |

Ví dụ: file `chrome.exe` là program. Mở Chrome có thể tạo nhiều process cho
browser, tab, renderer, GPU và extension.

### 6.2. Virtual address space

Mỗi process thường nhìn thấy một không gian địa chỉ ảo riêng:

```text
Địa chỉ cao
┌──────────────────────────┐
│ Kernel mapping           │
├──────────────────────────┤
│ Stack (thường tăng xuống)│
│            ↓             │
│                          │
│            ↑             │
│ Heap (thường tăng lên)   │
├──────────────────────────┤
│ BSS: biến chưa khởi tạo   │
├──────────────────────────┤
│ Data: biến toàn cục       │
├──────────────────────────┤
│ Text/code: machine code  │
└──────────────────────────┘
Địa chỉ thấp
```

Đây là mô hình khái quát; layout thực tế phụ thuộc OS, kiến trúc CPU, runtime và
cơ chế bảo mật như ASLR.

### 6.3. Code, data, heap và stack

| Vùng | Thường chứa |
|---|---|
| Text/code | Machine code, thường chỉ đọc |
| Data/BSS | Biến global/static |
| Heap | Bộ nhớ cấp phát động |
| Stack | Call frame, local variable, return address |

Các điểm cần nhớ:

- Stack frame thường được tạo khi gọi hàm và thu hồi khi hàm return.
- Mỗi thread thường có stack riêng.
- Các thread trong cùng process chia sẻ heap và phần lớn tài nguyên process.
- OS cấp vùng nhớ cho process, nhưng compiler/runtime quản lý cách dùng stack/heap.
- Garbage Collector là cơ chế của runtime như JVM, không phải đặc tính bắt buộc
  của mọi heap. C/C++ có heap nhưng thường phải giải phóng thủ công hoặc dùng RAII.

### 6.4. Out of Memory không đơn giản là “dùng hơn RAM được cấp”

OOM có thể xảy ra vì:

- Process chạm memory limit của container/cgroup.
- JVM heap đạt `-Xmx`.
- Không còn đủ virtual memory hoặc commit.
- Native memory cạn.
- Không thể cấp một vùng liên tục phù hợp.
- Kernel chọn kill process để cứu hệ thống.

Trong Java, `java.lang.OutOfMemoryError: Java heap space` nói về JVM heap, không
đồng nghĩa toàn bộ RAM vật lý của máy đã hết.

---

## 7. Vòng đời của process và PCB

![Mô hình process](Operation System.assets/image5.png)

### 7.1. Các trạng thái cơ bản

```text
             được scheduler chọn
New → Ready ─────────────────────→ Running → Terminated
        ↑                            │
        │      I/O hoàn tất          │ chờ I/O/event
        └──────── Waiting/Blocked ←──┘
              ↑
              └── hết time slice: Running → Ready
```

| Trạng thái | Ý nghĩa |
|---|---|
| New | Process đang được tạo |
| Ready | Có thể chạy, đang chờ CPU |
| Running | Một thread của process đang chạy trên CPU |
| Waiting/Blocked | Đang chờ I/O, lock, timer hoặc event |
| Terminated | Đã kết thúc; OS đang/đã thu hồi tài nguyên |

Không nên hiểu process ở trạng thái waiting là CPU “không cần làm gì”. CPU sẽ
chạy thread khác nếu có công việc sẵn sàng.

### 7.2. PCB — Process Control Block

Kernel cần lưu trạng thái quản lý của mỗi process trong một cấu trúc thường được
gọi khái quát là PCB.

PCB có thể chứa:

- PID và parent PID.
- Trạng thái process.
- Thông tin scheduling và priority.
- CPU register/context.
- Memory mappings/page tables.
- Credentials và permission.
- File descriptor/handle đang mở.
- Accounting information.

Tên và cấu trúc cụ thể khác nhau giữa các OS. Ví dụ Linux dùng `task_struct` cho
thực thể được scheduler quản lý.

### 7.3. Process kết thúc

Process có thể kết thúc vì:

- Chạy xong và gọi `exit`.
- Bị signal hoặc user kill.
- Lỗi nghiêm trọng như invalid memory access.
- Bị OOM killer hoặc policy của container chấm dứt.
- Parent/service manager yêu cầu dừng.

Trên Unix-like OS, process đã kết thúc nhưng parent chưa thu thập exit status có
thể tồn tại tạm thời dưới dạng **zombie**. Zombie không còn chạy và không giữ toàn
bộ memory cũ; nó chủ yếu giữ thông tin tối thiểu để parent gọi `wait`.

---

## 8. CPU scheduling và context switch

### 8.1. OS thực sự lập lịch cái gì?

Trong các OS hiện đại, scheduler thường lập lịch **thread** hoặc scheduling entity,
không đơn giản là “cấp CPU cho cả process”.

- Một CPU core tại một thời điểm chạy một hardware thread/instruction stream.
- Máy nhiều core có thể chạy nhiều thread thật sự song song.
- Số runnable thread thường lớn hơn số core nên OS phải time-share CPU.

### 8.2. Scheduling nhằm tối ưu điều gì?

Tùy loại hệ thống, scheduler cân bằng:

- Throughput.
- Response time.
- Fairness.
- Priority.
- Deadline.
- CPU utilization.

Một số thuật toán thường dùng để học nguyên lý:

| Thuật toán | Ý tưởng | Hạn chế điển hình |
|---|---|---|
| FCFS | Đến trước chạy trước | Convoy effect |
| SJF | Job ngắn chạy trước | Khó biết trước burst time |
| Priority | Ưu tiên cao chạy trước | Starvation |
| Round Robin | Mỗi task nhận một time quantum | Quantum quá nhỏ gây nhiều switch |
| Multilevel feedback queue | Điều chỉnh queue/priority theo hành vi | Phức tạp hơn |

OS thực tế thường dùng thiết kế tinh vi hơn các mô hình nhập môn này.

### 8.3. Context switch

Context switch xảy ra khi CPU chuyển từ thread đang chạy sang thread khác.

Kernel cần:

1. Lưu program counter, stack pointer và register cần thiết.
2. Cập nhật trạng thái scheduling.
3. Chọn thread tiếp theo.
4. Khôi phục context của thread đó.
5. Chuyển quyền thực thi.

Context switch có chi phí vì CPU không trực tiếp làm business logic trong khoảng
thời gian chuyển đổi, đồng thời cache/TLB locality có thể bị ảnh hưởng.

### 8.4. Concurrency và parallelism

- **Concurrency**: nhiều công việc cùng tiến triển trong một khoảng thời gian.
- **Parallelism**: nhiều công việc thực sự chạy cùng lúc trên nhiều core.

Một máy một core vẫn có concurrency nhờ chuyển đổi nhanh giữa các thread, nhưng
không chạy CPU instruction của hai thread song song tại cùng một thời điểm.

---

## 9. Thread và multithreading

### 9.1. Thread là gì?

Thread là đơn vị thực thi được scheduler quản lý. Mỗi thread thường có:

- Program counter.
- CPU registers.
- Stack riêng.
- Scheduling state.

Các thread cùng process thường chia sẻ:

- Code.
- Heap.
- Global/static data.
- File descriptor/handle.
- Socket và nhiều tài nguyên khác.

![Kernel và các thành phần hệ thống](Operation System.assets/image4.png)

### 9.2. Vì sao dùng nhiều thread?

- Giữ UI responsive trong khi làm I/O nền.
- Xử lý nhiều connection đồng thời.
- Tận dụng nhiều CPU core cho công việc có thể chia nhỏ.
- Tách các luồng công việc độc lập.

Ví dụ trình duyệt có thể đồng thời:

- Nhận input từ người dùng.
- Tải tài nguyên qua mạng.
- Parse HTML/CSS.
- Render giao diện.
- Ghi cache xuống disk.

### 9.3. Thread rẻ hơn process nhưng không miễn phí

Tạo thread thường nhẹ hơn tạo process vì thread chia sẻ address space và resource
của process. Tuy nhiên platform thread vẫn tiêu tốn:

- Native stack.
- Kernel bookkeeping.
- Scheduling overhead.
- Context-switch cost.

Tạo quá nhiều runnable thread có thể làm throughput giảm dù máy vẫn còn RAM.

### 9.4. Race condition

Vì các thread chia sẻ memory, kết quả có thể phụ thuộc thứ tự thực thi.

```java
class Counter {
    private int value = 0;

    void increment() {
        value++; // read → add → write, không phải một thao tác atomic hoàn chỉnh
    }
}
```

Nếu hai thread cùng gọi `increment`, một lần tăng có thể bị mất.

Cách xử lý tùy bài toán:

- `synchronized`/mutex/lock.
- Atomic variable.
- Immutable data.
- Thread confinement.
- Message passing.
- Concurrent collection.

Lock cũng có rủi ro: deadlock, contention, priority inversion và giảm throughput.

---

## 10. Java Platform Thread và Virtual Thread

### 10.1. Platform Thread

Java platform thread truyền thống thường được JVM ánh xạ gần theo mô hình 1:1 với
OS thread.

```text
Java Platform Thread
↕
OS Thread
↕
OS Scheduler
↕
CPU core
```

Ưu điểm:

- Phù hợp CPU-bound work.
- Tích hợp trực tiếp với OS scheduler.
- Mô hình quen thuộc, tooling trưởng thành.

Hạn chế:

- Mỗi thread có chi phí native resource đáng kể.
- Mô hình thread-per-request khó scale đến số lượng connection chờ I/O rất lớn.

### 10.2. Virtual Thread

Virtual Thread là thread nhẹ do JVM quản lý. Nhiều virtual thread được multiplex
trên một số platform thread gọi là **carrier thread**.

```text
Nhiều Virtual Thread
        ↓ mount/unmount
Một nhóm Platform/Carrier Thread
        ↓
OS Scheduler
        ↓
CPU
```

Khi virtual thread gặp blocking operation được JVM hỗ trợ, JVM thường có thể
unmount nó khỏi carrier để carrier chạy virtual thread khác. Vì vậy ứng dụng có
thể giữ style code tuần tự, dễ đọc, mà vẫn hỗ trợ concurrency rất lớn cho workload
chờ I/O.

Virtual Thread:

- Được giới thiệu dạng preview trong Java 19 và Java 20.
- Trở thành tính năng chính thức trong Java 21.
- Không tự động thay thế mọi `Thread` bằng virtual thread.
- Không làm CPU-bound task chạy nhanh hơn số CPU core.
- Vẫn phụ thuộc platform thread và OS scheduler ở tầng dưới.
- Không biến OS không hỗ trợ multithreading thành hệ thống multithread.

### 10.3. Ví dụ Java

```java
try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
    var future = executor.submit(() -> {
        Thread.sleep(100);
        return "done";
    });

    System.out.println(future.get());
}
```

Hoặc tạo trực tiếp:

```java
Thread thread = Thread.ofVirtual()
        .name("order-worker")
        .start(() -> processOrder());

thread.join();
```

### 10.4. Khi nào Virtual Thread phù hợp?

Phù hợp:

- Nhiều request độc lập.
- Phần lớn thời gian chờ database, HTTP, file hoặc network I/O.
- Muốn giữ mô hình blocking code dễ đọc.

Không phải lựa chọn thần kỳ cho:

- Tính toán CPU nặng.
- Task giữ lock lâu.
- Code native/blocking chưa tương thích tốt với cơ chế unmount.
- Bài toán cần giới hạn tài nguyên downstream.

Ngay cả khi virtual thread rất rẻ, database connection không rẻ. Nếu database chỉ
chịu được 100 query đồng thời, tạo 100.000 virtual thread không làm giới hạn đó
biến mất. Vẫn cần connection pool, semaphore, rate limit và backpressure phù hợp.

---

## 11. Quản lý bộ nhớ

### 11.1. Virtual memory

Virtual memory cho mỗi process một không gian địa chỉ ảo. Memory Management Unit
(MMU) cùng page table ánh xạ virtual address sang physical frame trong RAM.

Lợi ích:

- Cô lập process.
- Cho phép mapping file/library.
- Chia sẻ page có kiểm soát.
- Hỗ trợ demand paging.
- Đơn giản hóa việc cấp phát memory cho application.

### 11.2. Paging và page fault

Memory được chia thành các page có kích thước cố định ở mức logic quản lý.

Khi process truy cập một page chưa có trong RAM:

1. CPU phát hiện mapping chưa present.
2. Chuyển quyền cho kernel bằng page-fault exception.
3. Kernel kiểm tra truy cập có hợp lệ không.
4. Nếu hợp lệ, kernel nạp hoặc tạo page cần thiết.
5. Cập nhật page table.
6. Instruction được thử lại.

Page fault không luôn là lỗi ứng dụng. Nó có thể là hoạt động bình thường của
demand paging. Truy cập địa chỉ hoàn toàn không hợp lệ mới dẫn tới lỗi như
segmentation fault/access violation.

### 11.3. Swap

Khi áp lực RAM cao, OS có thể chuyển một số memory page ít dùng sang swap area trên
storage. Swap chậm hơn RAM rất nhiều. Nếu hệ thống liên tục chuyển page qua lại,
nó có thể rơi vào **thrashing** và phản hồi cực chậm.

### 11.4. Cache không đồng nghĩa memory leak

OS chủ động dùng RAM trống làm page cache để tăng tốc I/O. “RAM đang được dùng”
không tự động có nghĩa là thiếu memory. Cache thường có thể được thu hồi khi
application cần.

Memory leak là tài nguyên không còn hữu ích nhưng vẫn bị giữ tham chiếu hoặc không
được giải phóng, khiến mức dùng memory tăng không kiểm soát theo thời gian.

---

## 12. I/O, driver, interrupt và DMA

### 12.1. Driver và firmware

| Firmware | Driver |
|---|---|
| Chạy trong/bên cạnh thiết bị | Chạy phía OS |
| Điều khiển logic nội bộ thiết bị | Giúp OS giao tiếp với thiết bị |
| Ví dụ firmware SSD, router | Ví dụ GPU driver, printer driver |

Driver dịch yêu cầu chung của OS thành thao tác phù hợp với thiết bị cụ thể.

Ví dụ in tài liệu:

```text
Application
→ print API
→ OS printing service
→ printer driver
→ dữ liệu/lệnh phù hợp model máy in
→ printer
```

### 12.2. Interrupt

Nếu CPU liên tục hỏi thiết bị “xong chưa?” thì tốn tài nguyên. Với interrupt,
thiết bị/controller có thể thông báo khi có event hoặc khi operation hoàn tất.

Flow đơn giản:

```text
Application yêu cầu I/O
→ kernel/driver cấu hình thiết bị
→ thread có thể block, CPU chạy việc khác
→ thiết bị hoàn tất và phát interrupt
→ kernel xử lý completion
→ thread được đánh thức
```

Hệ thống hiệu năng cao có thể kết hợp interrupt, batching và polling tùy workload.

### 12.3. DMA

Direct Memory Access cho phép controller chuyển block dữ liệu giữa thiết bị và RAM
mà CPU không phải copy từng byte bằng instruction thông thường.

CPU vẫn tham gia thiết lập operation và xử lý completion, nhưng được giải phóng
khỏi phần lớn công việc truyền dữ liệu lặp lại.

### 12.4. Buffer và cache

- **Buffer** giúp hấp thụ chênh lệch tốc độ hoặc gom dữ liệu khi truyền.
- **Cache** giữ bản sao dữ liệu có khả năng dùng lại để truy cập nhanh hơn.

Một vùng memory có thể đóng vai trò khác nhau tùy ngữ cảnh; không nên áp dụng định
nghĩa quá cứng chỉ dựa vào tên.

Phần I/O chuyên sâu hơn nằm tại [08-io-chuyen-sau.md](08-io-chuyen-sau.md).

---

## 13. File system

File system tổ chức dữ liệu trên storage thành file, directory và metadata.

Nó chịu trách nhiệm:

- Đặt tên và tổ chức đường dẫn.
- Ánh xạ file sang data block.
- Lưu kích thước, timestamp, owner và permission.
- Theo dõi block trống/đã dùng.
- Cache dữ liệu.
- Hỗ trợ consistency và recovery.

### 13.1. File descriptor và handle

Trên Unix-like OS, khi process mở file/socket, kernel thường trả về một số nguyên
gọi là file descriptor.

```c
int fd = open("orders.log", O_RDONLY);
read(fd, buffer, size);
close(fd);
```

`fd` không phải nội dung file. Nó là định danh để kernel tìm open-file state phù
hợp của process.

Windows thường sử dụng khái niệm handle rộng hơn cho nhiều loại kernel object.

### 13.2. Quyền truy cập

Ví dụ permission trên Linux:

```text
-rw-r----- 1 app orders 4096 Jun 19 orders.log
```

Có thể đọc khái quát:

- Owner `app`: đọc và ghi.
- Group `orders`: đọc.
- User khác: không có quyền.

Ngay cả khi biết đúng path, process vẫn cần credentials phù hợp.

### 13.3. Ghi file có chắc đã nằm trên SSD chưa?

Không phải lúc nào `write` trả về cũng có nghĩa dữ liệu đã bền vững trên thiết bị.
Dữ liệu có thể còn trong page cache hoặc cache của storage. Ứng dụng cần durability
cao có thể phải dùng cơ chế như `fsync` và thiết kế transaction/journal phù hợp.

Đây là lý do database không chỉ “ghi vài byte vào file” mà còn phải xử lý WAL,
ordering, flush và crash recovery.

---

## 14. Giao tiếp giữa các process và networking

### 14.1. IPC trên cùng máy

| Cơ chế | Điểm mạnh | Ví dụ sử dụng |
|---|---|---|
| Pipe | Đơn giản, stream một chiều/hai chiều tùy loại | Shell pipeline |
| Signal | Thông báo sự kiện nhỏ | Yêu cầu process dừng/reload |
| Message queue | Truyền message có cấu trúc | Producer/consumer cục bộ |
| Shared memory | Nhanh, giảm copy | Trao đổi dữ liệu lớn |
| Unix domain socket | Socket API trong cùng host | Docker daemon, local service |
| TCP loopback | Dễ dùng chung network protocol | Service gọi `localhost` |

Shared memory nhanh nhưng cần synchronization để tránh race condition.

### 14.2. Socket qua mạng

```text
Application A
→ socket API/system call
→ socket send buffer
→ TCP/UDP + IP
→ network driver
→ NIC
→ network
→ NIC máy B
→ kernel network stack
→ socket receive buffer
→ Application B
```

HTTP là giao thức tầng ứng dụng, thường do application/library triển khai. Kernel
cung cấp TCP/IP stack và socket primitives ở tầng thấp hơn.

### 14.3. Localhost có đi qua card mạng không?

Thông thường, traffic tới loopback như `127.0.0.1` được xử lý bên trong kernel:

```text
Process A
→ socket
→ TCP/IP stack
→ loopback interface
→ socket
→ Process B
```

Nó vẫn có overhead của socket, protocol và copy/buffer nhất định, nhưng không cần
đi qua NIC vật lý, dây mạng hoặc switch.

---

## 15. Protection và security

### 15.1. Authentication và authorization

- **Authentication**: bạn là ai?
- **Authorization**: bạn được phép làm gì?

OS dùng user identity, group, token/capability, ACL và policy để quyết định quyền
truy cập.

### 15.2. Process isolation

Process isolation dựa trên:

- Virtual address space riêng.
- User/kernel mode.
- Page permission: read/write/execute.
- File/device permission.
- Security policy và sandbox.

Một process thông thường không thể đọc tùy ý heap của process khác.

### 15.3. Container có phải virtual machine không?

Không.

- Container thường cô lập process bằng namespace, cgroup và security mechanism,
  nhưng chia sẻ kernel của host.
- Virtual machine chạy guest OS/kernel riêng trên virtual hardware do hypervisor
  cung cấp.

Vì chia sẻ kernel, container thường nhẹ hơn VM nhưng ranh giới và mô hình bảo mật
khác VM.

### 15.4. Principle of least privilege

Process chỉ nên có quyền tối thiểu cần thiết.

Ví dụ Spring Boot service:

- Không nên chạy bằng `root` nếu không cần.
- Chỉ nên đọc secret cần thiết.
- Chỉ mở port cần dùng.
- Chỉ có quyền ghi vào thư mục log/data liên quan.
- Nên có CPU/memory limit trong container.

---

## 16. Ví dụ xuyên suốt: một HTTP request trong Spring Boot

Giả sử client gọi:

```http
GET /api/orders/123
```

Một flow rút gọn có thể là:

```text
1. NIC nhận network frame
2. Driver/kernel network stack xử lý Ethernet/IP/TCP
3. Byte được đưa vào socket receive buffer
4. Thread của server được đánh thức hoặc task được báo sẵn sàng
5. JVM/framework đọc request từ socket
6. Spring MVC route request tới controller
7. Controller gọi service
8. Service gọi repository
9. JDBC chờ một connection từ pool
10. Query được gửi qua socket tới database
11. Thread chờ I/O, CPU có thể chạy thread khác
12. Database trả kết quả
13. Java object được tạo trên heap
14. Framework serialize object thành JSON
15. Byte được ghi vào socket
16. Kernel TCP/IP và NIC gửi response về client
```

Trong flow này, OS tham gia ở nhiều điểm:

| Công việc | Thành phần liên quan |
|---|---|
| Nhận/gửi packet | NIC, driver, interrupt/DMA, network stack |
| Chờ socket | I/O subsystem, scheduler |
| Chạy Java code | CPU scheduling, thread |
| Cấp memory | JVM allocator, virtual memory, OS |
| Đọc config/log | File system, page cache |
| Cô lập service | Process, permission, container/cgroup |

Nếu dùng platform thread theo mô hình thread-per-request, thread có thể bị block
trong lúc chờ database. Nếu dùng virtual thread, JVM có thể unmount virtual thread
trong nhiều blocking operation phù hợp để carrier phục vụ công việc khác.

Tuy nhiên cả hai mô hình vẫn bị giới hạn bởi tài nguyên thật:

- Số CPU core.
- Database connection pool.
- Database throughput.
- Memory.
- Network bandwidth.
- Downstream rate limit.

---

## 17. Các hiểu lầm thường gặp

### 17.1. “Toàn bộ OS luôn nằm trong RAM”

Sai. Kernel và các thành phần đang hoạt động cần memory, nhưng nhiều binary,
service, driver/module và dữ liệu chỉ được nạp khi cần.

### 17.2. “Kernel là một tập file luôn nằm nguyên trong RAM”

Chưa chính xác. Kernel là chương trình lõi đang thực thi với đặc quyền cao. Kernel
image được nạp khi boot, nhưng memory của kernel còn gồm code, data structure,
cache và module được quản lý động.

### 17.3. “Microkernel chỉ load module khi cần để tiết kiệm RAM”

Đây không phải điểm định nghĩa chính. Microkernel giữ tối thiểu cơ chế trong kernel
mode và chuyển nhiều service sang user space. Mục tiêu quan trọng là modularity,
isolation và reliability; trade-off thường liên quan IPC/context-switch overhead.

### 17.4. “Stack do OS hoàn toàn tự quản lý”

OS cấp và bảo vệ vùng memory; compiler, ABI, CPU instruction và runtime phối hợp
quản lý stack frame. Không nên quy toàn bộ trách nhiệm cho OS.

### 17.5. “Heap luôn có Garbage Collector”

Sai. GC phụ thuộc runtime/ngôn ngữ. C và C++ vẫn có dynamic heap nhưng không bắt
buộc có GC.

### 17.6. “Một process chỉ có một stack”

Sai với process đa luồng. Mỗi thread thường có stack riêng; các thread chia sẻ
heap và nhiều tài nguyên process.

### 17.7. “OS cấp nhiều CPU cho một OS thread”

Một thread tại một thời điểm chạy trên một logical CPU. Process nhiều thread có
thể tận dụng nhiều core song song.

### 17.8. “Virtual Thread không phụ thuộc OS scheduling”

Sai. JVM lập lịch virtual thread lên carrier thread, còn carrier thread vẫn được
OS scheduler lập lịch lên CPU.

### 17.9. “Java 19 mặc định dùng Virtual Thread”

Sai. Virtual Thread preview ở Java 19/20 và chính thức ở Java 21, nhưng developer
phải chủ động tạo/dùng executor hoặc framework configuration phù hợp.

### 17.10. “Nhiều thread luôn làm chương trình nhanh hơn”

Sai. Quá nhiều thread có thể tăng contention, context switch, memory footprint và
làm giảm cache locality. Mức concurrency phải phù hợp workload và bottleneck thật.

---

## 18. Câu hỏi tự kiểm tra

1. Vì sao application không nên được phép truy cập trực tiếp mọi vùng RAM?
2. API, library function và system call khác nhau ở điểm nào?
3. Khi một thread chờ đọc database, OS có để CPU đứng yên không?
4. Program khác process như thế nào?
5. Các thread trong cùng process chia sẻ gì và có gì riêng?
6. Page fault có luôn là lỗi không?
7. Context switch có những loại chi phí nào?
8. Vì sao Virtual Thread phù hợp I/O-bound hơn CPU-bound?
9. Vì sao 100.000 Virtual Thread không có nghĩa database xử lý được 100.000 query
   đồng thời?
10. Container và virtual machine khác nhau ở kernel như thế nào?

### Checklist ghi nhớ nhanh

- OS quản lý và trừu tượng hóa hardware.
- Kernel là lõi đặc quyền, không phải toàn bộ OS.
- Application dùng system call để yêu cầu kernel làm việc đặc quyền.
- Process có address space và tài nguyên; thread là đơn vị thực thi.
- Mỗi thread thường có stack riêng; các thread cùng process chia sẻ heap.
- Scheduler phân phối logical CPU cho runnable thread.
- Virtual memory giúp ánh xạ, cô lập và demand paging.
- Driver kết nối OS với thiết bị; interrupt/DMA giúp I/O hiệu quả.
- File và socket là abstraction, không phải bản thân hardware.
- Virtual Thread giúp scale workload chờ I/O, không tạo thêm CPU capacity.
