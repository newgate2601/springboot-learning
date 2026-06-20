# Process, Thread và CPU Scheduling chuyên sâu

Tài liệu này giải thích cách hệ điều hành quản lý chương trình đang chạy, thread và CPU. Trọng tâm không chỉ là định nghĩa, mà còn là luồng thực tế từ lúc một chương trình được khởi chạy, được scheduler chọn, bị tạm dừng, chờ I/O rồi tiếp tục chạy.

---

## Mục lục

1. [Bức tranh tổng thể](#1-bức-tranh-tổng-thể)
2. [Program và process](#2-program-và-process)
3. [Không gian địa chỉ và tài nguyên của process](#3-không-gian-địa-chỉ-và-tài-nguyên-của-process)
4. [Process Control Block](#4-process-control-block)
5. [Vòng đời của process](#5-vòng-đời-của-process)
6. [Tạo và kết thúc process](#6-tạo-và-kết-thúc-process)
7. [Zombie và orphan process](#7-zombie-và-orphan-process)
8. [Thread](#8-thread)
9. [User thread, kernel thread và Java thread](#9-user-thread-kernel-thread-và-java-thread)
10. [CPU scheduling](#10-cpu-scheduling)
11. [Preemption và time slice](#11-preemption-và-time-slice)
12. [Các thuật toán scheduling](#12-các-thuật-toán-scheduling)
13. [Context switch](#13-context-switch)
14. [Scheduling trên hệ thống đa lõi](#14-scheduling-trên-hệ-thống-đa-lõi)
15. [Priority, starvation và priority inversion](#15-priority-starvation-và-priority-inversion)
16. [CPU-bound và I/O-bound](#16-cpu-bound-và-io-bound)
17. [Load average và CPU utilization](#17-load-average-và-cpu-utilization)
18. [Liên hệ với Java và Spring Boot](#18-liên-hệ-với-java-và-spring-boot)
19. [Thực hành quan sát trên Linux](#19-thực-hành-quan-sát-trên-linux)
20. [Các hiểu lầm thường gặp](#20-các-hiểu-lầm-thường-gặp)
21. [Câu hỏi tự kiểm tra](#21-câu-hỏi-tự-kiểm-tra)

---

## 1. Bức tranh tổng thể

Có thể hình dung quá trình thực thi như sau:

```text
Program nằm trên SSD
        |
        | được yêu cầu thực thi
        v
Hệ điều hành tạo process
        |
        | tạo virtual address space và tài nguyên
        v
Process có một hoặc nhiều thread
        |
        | thread ở trạng thái runnable
        v
Scheduler chọn một thread
        |
        | dispatcher đưa thread lên logical CPU
        v
CPU thực thi instruction của thread
```

Ba khái niệm cần phân biệt:

| Khái niệm | Ý nghĩa |
|---|---|
| Program | Tập lệnh và dữ liệu ở trạng thái tĩnh, thường nằm trên thiết bị lưu trữ |
| Process | Một instance đang chạy của program, có không gian địa chỉ và tài nguyên riêng |
| Thread | Luồng thực thi bên trong process, là đơn vị thường được scheduler lập lịch |

Hệ điều hành không thực sự “chạy một file”. Nó tạo process, chuẩn bị môi trường thực thi rồi lập lịch các thread của process đó lên CPU.

---

## 2. Program và process

### 2.1. Program là gì?

Program là một executable hoặc tập bytecode chưa nhất thiết đang chạy.

Ví dụ:

```text
/usr/bin/java
order-service.jar
chrome.exe
```

Một program có thể được chạy nhiều lần và tạo ra nhiều process độc lập.

### 2.2. Process là gì?

Process là một môi trường thực thi do hệ điều hành quản lý. Nó thường có:

- Process ID, viết tắt là PID.
- Virtual address space riêng.
- Một hoặc nhiều thread.
- Bảng file descriptor hoặc handle.
- Thông tin quyền truy cập.
- Trạng thái scheduling và accounting.
- Signal handler và thông tin giao tiếp với process khác.

Ví dụ, chạy cùng một file JAR hai lần có thể tạo hai process JVM:

```text
PID 4101 -> java -jar order-service.jar --server.port=8080
PID 4288 -> java -jar order-service.jar --server.port=8081
```

Hai process dùng cùng program nhưng có heap, thread và tài nguyên độc lập.

### 2.3. Process isolation

Mỗi process thường nhìn thấy một virtual address space riêng. Cùng một địa chỉ ảo có thể ánh xạ tới hai vùng RAM vật lý khác nhau:

```text
Process A: virtual address 0x1000 -> physical frame X
Process B: virtual address 0x1000 -> physical frame Y
```

Vì vậy, process A không thể mặc nhiên đọc biến trong heap của process B. Muốn trao đổi dữ liệu, chúng phải dùng cơ chế IPC như:

- Pipe.
- Shared memory.
- Message queue.
- Signal.
- Unix domain socket.
- TCP/UDP socket.

---

## 3. Không gian địa chỉ và tài nguyên của process

Không gian địa chỉ điển hình của một process:

```text
Địa chỉ cao
+---------------------------+
| Stack của thread          |
|             ↓             |
|                           |
|             ↑             |
| Heap                      |
+---------------------------+
| Memory-mapped region      |
| Shared library            |
+---------------------------+
| BSS                       |
+---------------------------+
| Data                      |
+---------------------------+
| Text / executable code    |
+---------------------------+
Địa chỉ thấp
```

### 3.1. Các vùng phổ biến

| Vùng | Nội dung |
|---|---|
| Text | Machine code của chương trình |
| Data | Biến global/static đã được khởi tạo |
| BSS | Biến global/static chưa được khởi tạo rõ ràng |
| Heap | Bộ nhớ cấp phát động |
| Memory-mapped region | Shared library, mapped file, anonymous mapping |
| Stack | Stack frame, biến local và thông tin lời gọi hàm của từng thread |

Mỗi thread thường có stack riêng nhưng các thread cùng process chia sẻ:

- Code.
- Heap.
- Biến global/static.
- File descriptor.
- Socket.
- Quyền và thông tin process.

### 3.2. Process không đồng nghĩa với toàn bộ dữ liệu đang nằm trong RAM

Nhờ demand paging, một phần code hoặc dữ liệu chỉ được nạp vào RAM khi được truy cập. Một số page cũng có thể được đưa ra swap hoặc bị thu hồi nếu có thể tạo lại từ file.

Do đó:

```text
Kích thước virtual memory != lượng RAM vật lý đang sử dụng
```

---

## 4. Process Control Block

Kernel cần lưu thông tin để quản lý mỗi process. Cấu trúc logic này thường được gọi là **Process Control Block**, viết tắt là PCB.

PCB có thể chứa:

- PID và Parent PID.
- Trạng thái process.
- Thông tin các thread.
- Page table hoặc tham chiếu tới memory-management structure.
- File descriptor table.
- Credentials và permission.
- Signal state.
- CPU time đã sử dụng.
- Giới hạn tài nguyên.
- Thông tin scheduling.

Tên và cách tổ chức chính xác phụ thuộc từng hệ điều hành. Trên Linux, thông tin process và thread được biểu diễn chủ yếu qua các cấu trúc task của kernel.

### 4.1. Tại sao PCB quan trọng?

Khi một thread bị đưa ra khỏi CPU, kernel phải giữ đủ trạng thái để sau này tiếp tục thực thi gần như tại đúng điểm đã dừng.

```text
Thread A đang chạy
-> timer interrupt
-> kernel lưu execution state của A
-> scheduler chọn B
-> kernel khôi phục execution state của B
-> B tiếp tục chạy
```

---

## 5. Vòng đời của process

Mô hình khái quát:

```text
             admit
   New ----------------> Ready
                          |
                          | dispatch
                          v
                       Running
                      /   |    \
          I/O wait   /    |     \ exit
                    v     |      v
                 Waiting  |   Terminated
                    |     |
        I/O complete|     | preempt
                    v     |
                   Ready <-
```

### 5.1. New

Process đang được tạo. Kernel chuẩn bị:

- Định danh.
- Không gian địa chỉ.
- Tài nguyên ban đầu.
- Thread đầu tiên.
- Thông tin quản lý.

### 5.2. Ready hoặc runnable

Thread đã có đủ điều kiện chạy nhưng đang chờ CPU.

`Ready` không có nghĩa là thread đang thực thi. Nó chỉ có nghĩa:

> Nếu có logical CPU phù hợp, thread có thể được đưa lên chạy ngay.

### 5.3. Running

Thread đang thực thi instruction trên một logical CPU.

Tại một thời điểm:

- Một logical CPU chỉ chạy một hardware thread.
- Một software thread chỉ chạy trên một logical CPU.
- Một process có nhiều thread có thể chạy song song trên nhiều logical CPU.

### 5.4. Waiting hoặc blocked

Thread chưa thể tiếp tục vì đang chờ một sự kiện:

- Dữ liệu từ socket.
- File I/O hoàn thành.
- Lock được giải phóng.
- Timer hết hạn.
- Child process kết thúc.
- Page cần thiết được nạp.

Thread blocked thường không cạnh tranh CPU với runnable thread.

### 5.5. Terminated

Process đã ngừng thực thi. Kernel cần thu hồi tài nguyên, nhưng một số thông tin kết thúc có thể còn được giữ lại để parent process đọc.

---

## 6. Tạo và kết thúc process

### 6.1. `fork()`

Trên Unix-like OS, `fork()` tạo child process dựa trên process hiện tại.

```text
Parent process
     |
     | fork()
     +-------------------+
     |                   |
     v                   v
Parent tiếp tục      Child tiếp tục
```

Parent và child có virtual address space riêng. Hệ điều hành thường không copy toàn bộ RAM ngay lập tức mà dùng **copy-on-write**:

1. Ban đầu, các page có thể được dùng chung ở chế độ phù hợp.
2. Khi một process ghi vào page, page riêng mới được tạo.
3. Process còn lại vẫn nhìn thấy nội dung cũ.

### 6.2. `exec()`

`exec()` thay program đang chạy trong process bằng program mới:

```text
Child process sau fork()
-> exec("/usr/bin/java", ...)
-> address space cũ được thay bằng Java program
```

Thông thường shell khởi chạy lệnh theo flow:

```text
Shell
-> fork()
-> child gọi exec()
-> parent có thể gọi wait()
```

### 6.3. Windows process creation

Windows thường dùng API kiểu `CreateProcess()` để tạo process mới và nạp program, thay vì mô hình `fork()` rồi `exec()` giống Unix.

### 6.4. Kết thúc process

Process có thể kết thúc vì:

- Hàm `main` trả về.
- Gọi `exit`.
- Nhận signal hoặc yêu cầu terminate.
- Exception nghiêm trọng không được xử lý.
- Kernel dừng process vì vi phạm hoặc thiếu tài nguyên.
- Người quản trị chủ động kill process.

Khi process kết thúc, kernel thu hồi:

- Virtual memory.
- Thread.
- File descriptor không còn được tham chiếu.
- Socket và tài nguyên kernel.
- Thông tin scheduling.

---

## 7. Zombie và orphan process

### 7.1. Zombie process

Trên Unix-like OS, khi child kết thúc, kernel giữ lại một lượng nhỏ thông tin như:

- PID.
- Exit status.
- Thống kê sử dụng tài nguyên.

Parent cần gọi `wait()` hoặc `waitpid()` để nhận thông tin này. Nếu parent chưa thu nhận, child ở trạng thái zombie.

```text
Child kết thúc
-> kernel giữ exit status
-> parent chưa gọi wait()
-> zombie
```

Zombie không còn thực thi code và thường không giữ heap như process đang sống, nhưng vẫn chiếm một entry trong bảng process. Quá nhiều zombie có thể làm cạn PID hoặc tài nguyên quản lý.

### 7.2. Orphan process

Orphan là child process vẫn còn chạy sau khi parent kết thúc.

Hệ thống sẽ giao trách nhiệm thu nhận trạng thái kết thúc của orphan cho một process quản lý phù hợp, thường liên quan tới PID 1 hoặc cơ chế subreaper trên Linux.

### 7.3. Zombie khác orphan

| Zombie | Orphan |
|---|---|
| Đã kết thúc | Vẫn đang chạy |
| Còn exit status chưa được thu nhận | Parent đã kết thúc |
| Không tiếp tục thực thi nghiệp vụ | Có thể tiếp tục hoạt động bình thường |

---

## 8. Thread

Thread là một luồng thực thi bên trong process.

Mỗi thread thường có:

- Program counter.
- CPU register state.
- Stack riêng.
- Scheduling state.
- Thread ID.
- Thread-local storage.

Các thread cùng process chia sẻ:

- Virtual address space.
- Heap.
- Code.
- File descriptor và socket.
- Biến global/static.

### 8.1. Lợi ích

- Tận dụng nhiều CPU core.
- Cho phép một phần chương trình chờ I/O trong khi phần khác tiếp tục.
- Chia sẻ dữ liệu nhanh hơn so với IPC giữa process.
- Phù hợp cho server xử lý nhiều tác vụ đồng thời.

### 8.2. Rủi ro

Vì chia sẻ memory, các thread có thể gặp:

- Race condition.
- Deadlock.
- Livelock.
- Starvation.
- Memory visibility problem.
- Lock contention.

Ví dụ race condition:

```java
counter++;
```

Biểu thức trên có thể gồm nhiều bước:

```text
đọc counter
-> tăng giá trị
-> ghi lại counter
```

Nếu hai thread xen kẽ các bước này, một lần tăng có thể bị mất.

---

## 9. User thread, kernel thread và Java thread

### 9.1. Kernel thread

Kernel trực tiếp biết và lập lịch kernel thread. Kernel quản lý:

- Trạng thái runnable/blocked.
- Priority.
- CPU affinity.
- Thời gian CPU.
- Context cần thiết để thực thi.

### 9.2. User-level thread

User-level thread được runtime hoặc thư viện quản lý ở user space. Kernel có thể không biết trực tiếp từng user-level thread mà chỉ nhìn thấy một số kernel thread bên dưới.

### 9.3. Các mô hình ánh xạ

| Mô hình | Ý nghĩa |
|---|---|
| Many-to-one | Nhiều user thread chạy trên một kernel thread |
| One-to-one | Mỗi user thread ánh xạ tới một kernel thread |
| Many-to-many | Nhiều user thread được multiplex trên nhiều kernel thread |

### 9.4. Java Platform Thread

Java platform thread thường ánh xạ gần với mô hình one-to-one:

```text
Java Platform Thread
-> native OS thread
-> kernel scheduler
-> logical CPU
```

Tạo quá nhiều platform thread có thể làm tăng:

- Stack memory.
- Kernel resource.
- Context switch.
- Scheduling overhead.

### 9.5. Java Virtual Thread

Virtual thread do JVM quản lý và được chạy trên các carrier thread:

```text
Nhiều Virtual Thread
-> JVM scheduler
-> một số Platform/Carrier Thread
-> OS scheduler
-> CPU
```

Khi một virtual thread chờ loại I/O được hỗ trợ, JVM có thể unmount nó khỏi carrier để carrier chạy virtual thread khác.

Virtual thread giúp tăng concurrency cho workload chờ I/O, nhưng:

- Không tạo thêm CPU core.
- Không làm CPU-bound task tự nhiên nhanh hơn.
- Không loại bỏ giới hạn database connection hoặc downstream service.
- Vẫn phụ thuộc carrier thread và OS scheduler.

---

## 10. CPU scheduling

**CPU scheduling** là hoạt động kernel quyết định:

> Trong số các thread đang sẵn sàng chạy, thread nào sẽ được sử dụng logical CPU tiếp theo, chạy trên CPU nào và được chạy trong bao lâu?

CPU scheduling cần thiết vì số runnable thread thường lớn hơn số logical CPU:

```text
Hệ thống có:
- 4 logical CPU
- 100 thread đang tồn tại
- 12 thread đang runnable

Tại một thời điểm:
- Tối đa 4 thread có thể thực sự chạy
- 8 runnable thread còn lại phải chờ trong run queue
```

Scheduler không lập lịch những thread đang chờ database, socket, lock hoặc timer. Các thread đó đang ở trạng thái **blocked/sleeping**, chưa đủ điều kiện sử dụng CPU.

### 10.1. Các keyword quan trọng

| Keyword | Giải thích |
|---|---|
| **Task** | Tên gọi tổng quát cho đơn vị thực thi mà kernel quản lý; trên nhiều hệ điều hành nó gần với thread |
| **Runnable thread** | Thread đã có đủ điều kiện chạy, hiện đang chạy hoặc đang chờ CPU |
| **Running thread** | Runnable thread đang thực sự được một logical CPU thực thi |
| **Blocked/Sleeping thread** | Thread đang chờ I/O, lock, timer hoặc sự kiện nên chưa thể tiếp tục chạy |
| **Run queue / Ready queue** | Hàng đợi hoặc cấu trúc dữ liệu chứa các runnable task đang chờ CPU |
| **Scheduler** | Phần kernel chọn task tiếp theo và quyết định CPU nào sẽ chạy task đó |
| **Scheduling policy** | Bộ quy tắc dùng để lựa chọn task, ví dụ fairness, priority hoặc deadline |
| **Scheduling class** | Nhóm task sử dụng một chính sách scheduling cụ thể, ví dụ normal hoặc real-time |
| **Dispatcher** | Cơ chế thực thi quyết định của scheduler: lưu task cũ, khôi phục task mới và chuyển CPU cho task mới |
| **Dispatch** | Hành động đưa task đã được chọn lên CPU |
| **Dispatch latency** | Thời gian từ lúc task có thể chạy đến lúc nó thực sự bắt đầu hoặc tiếp tục chạy |
| **Preemption** | Kernel tạm dừng task đang chạy để một task khác sử dụng CPU |
| **Time slice / Quantum** | Khoảng CPU time một task được phép chạy trước khi scheduler cân nhắc lại |
| **Context switch** | Quá trình lưu execution state của task cũ và khôi phục state của task mới |
| **CPU affinity** | Tập logical CPU mà task được phép hoặc được ưu tiên chạy trên đó |
| **Load balancing** | Phân phối runnable task giữa các CPU để tránh CPU quá tải trong khi CPU khác rảnh |
| **Logical CPU** | Đơn vị xử lý mà OS nhìn thấy và có thể lập lịch task lên; có thể là một hardware thread của physical core |

### 10.2. Run queue chứa gì?

Khi một thread được tạo hoặc vừa hoàn thành việc chờ I/O, kernel có thể chuyển nó sang trạng thái runnable và đặt nó vào **run queue**.

```text
Thread A: đang chạy
Thread B: runnable, chờ CPU
Thread C: runnable, chờ CPU
Thread D: blocked, chờ database
Thread E: sleeping, chờ timer
```

Run queue chỉ liên quan trực tiếp tới:

```text
A, B, C
```

`D` và `E` chưa được scheduler chọn vì dù được cấp CPU, chúng vẫn chưa có điều kiện tiếp tục.

Trên hệ thống đa lõi, kernel thường tổ chức run queue theo từng CPU hoặc theo nhóm CPU:

```text
CPU 0 run queue: A, B
CPU 1 run queue: C
CPU 2 run queue: D, E, F
CPU 3 run queue: trống
```

Scheduler có thể thực hiện load balancing, chẳng hạn chuyển một task từ CPU 2 sang CPU 3. Tuy nhiên migration không phải miễn phí vì task có thể mất dữ liệu đang nóng trong CPU cache cũ.

### 10.3. Scheduler thực sự quyết định những gì?

Scheduler không chỉ trả lời “thread nào chạy tiếp theo”. Nó còn cân nhắc:

1. **Eligibility**: thread có thực sự runnable không?
2. **Priority**: thread có mức ưu tiên nào?
3. **Policy/class**: thread thuộc normal, real-time hay deadline scheduling?
4. **Fairness**: thread đã nhận bao nhiêu CPU time so với thread khác?
5. **CPU affinity**: thread được phép chạy trên CPU nào?
6. **Cache locality**: có nên giữ thread trên CPU cũ để tận dụng cache?
7. **Load balancing**: CPU hiện tại có quá tải so với CPU khác không?
8. **Preemption**: có cần dừng thread đang chạy để nhường cho thread khác không?

Kết quả của scheduler có thể là:

```text
- Tiếp tục cho thread hiện tại chạy
- Chọn một thread khác trên cùng run queue
- Đánh thức hoặc ưu tiên một thread vừa trở thành runnable
- Di chuyển thread sang CPU khác
- Để CPU idle nếu không có task hợp lệ
```

### 10.4. Dispatcher đưa thread lên CPU như thế nào?

**Scheduler** đưa ra quyết định; **dispatcher** biến quyết định đó thành việc thực thi thật.

Giả sử logical CPU đang chạy thread `A`, nhưng scheduler chọn `B`:

```text
1. CPU đang chạy instruction của A ở user mode
2. Timer interrupt, system call hoặc event đưa CPU vào kernel mode
3. Kernel lưu execution state của A
   - program counter
   - stack pointer
   - register
   - scheduling state
4. Scheduler cập nhật trạng thái của A
   - A tiếp tục runnable, hoặc
   - A chuyển sang blocked/terminated
5. Scheduler chọn B từ run queue
6. Dispatcher khôi phục execution state của B
7. Nếu cần, kernel chuyển memory context sang process của B
8. CPU quay lại user mode
9. B tiếp tục từ instruction mà nó đã dừng trước đó
```

Flow đầy đủ:

```text
Thread phát sinh hoặc được đánh thức
        |
        v
Chuyển sang trạng thái RUNNABLE
        |
        v
Được đặt vào RUN QUEUE phù hợp
        |
        v
Scheduler áp dụng policy, priority,
fairness, affinity và load balancing
        |
        v
Chọn thread tiếp theo
        |
        v
Dispatcher thực hiện context switch
        |
        v
Thread chuyển sang RUNNING
        |
        v
Logical CPU thực thi instruction
        |
        +-------------------------------+
        |               |               |
        v               v               v
  Hết time slice    Chờ I/O/lock      Kết thúc
        |               |               |
        v               v               v
   RUNNABLE lại       BLOCKED       TERMINATED
```

### 10.5. Khi nào scheduler được chạy?

Kernel có thể cần đưa ra quyết định scheduling khi:

- Thread đang chạy hết time slice.
- Thread đang chạy block vì I/O, lock hoặc timer.
- Thread tự nhường CPU.
- Thread kết thúc.
- Một thread mới được tạo và trở thành runnable.
- I/O hoàn thành làm một thread được đánh thức.
- Thread priority cao hơn trở thành runnable.
- Kernel cần cân bằng tải giữa các CPU.
- CPU chuyển từ idle sang có công việc.

Không phải mỗi lần kernel chạy scheduler đều dẫn tới context switch. Scheduler có thể quyết định thread hiện tại vẫn là lựa chọn tốt nhất.

### 10.6. Ví dụ: Spring Boot thread chờ database

```text
1. Thread request R đang RUNNING trên CPU 2
2. R xử lý controller và gọi JDBC
3. JDBC đọc socket nhưng database chưa trả dữ liệu
4. R chuyển sang BLOCKED và rời run queue
5. Scheduler chọn thread T đang runnable
6. Dispatcher đưa T lên CPU 2
7. Database trả dữ liệu cho socket của R
8. Kernel đánh thức R
9. R chuyển từ BLOCKED sang RUNNABLE
10. R được đặt lại vào run queue
11. Khi được scheduler chọn, R tiếp tục xử lý kết quả
```

Trong thời gian `R` chờ database, CPU không phải đứng yên. Scheduler dùng CPU đó để chạy thread khác.

Điểm cần phân biệt:

```text
R vừa được đánh thức
!= R chạy ngay lập tức

R vừa được đánh thức
= R đã runnable và có quyền cạnh tranh CPU
```

Khoảng thời gian từ lúc `R` runnable đến lúc được chạy là một phần của **scheduling latency**.

### 10.7. Mục tiêu của scheduler

Tùy hệ điều hành và workload, scheduler cố gắng cân bằng:

- **CPU utilization**: hạn chế để CPU rảnh khi vẫn còn runnable task.
- **Throughput**: hoàn thành nhiều công việc trong một đơn vị thời gian.
- **Turnaround time**: giảm thời gian từ lúc công việc được submit tới lúc hoàn thành.
- **Waiting time**: giảm tổng thời gian task nằm trong run queue.
- **Response time**: giúp task tương tác bắt đầu phản hồi nhanh.
- **Fairness**: phân phối CPU hợp lý, tránh một task chiếm CPU quá lâu.
- **Deadline**: đáp ứng thời hạn của real-time task.
- **Cache locality**: ưu tiên giữ task gần CPU và memory đã sử dụng.
- **Energy efficiency**: giảm điện năng và nhiệt khi tải thấp.

Không có một thuật toán tối ưu cho mọi mục tiêu. Giảm response time có thể làm tăng context switch; tối đa throughput có thể làm tác vụ tương tác cảm thấy chậm.

### 10.8. Long-term, medium-term và short-term scheduler

Trong mô hình học thuật:

| Loại | Vai trò |
|---|---|
| Long-term scheduler | Quyết định job nào được đưa vào hệ thống |
| Medium-term scheduler | Tạm đưa process ra/vào memory hoặc điều chỉnh mức multiprogramming |
| Short-term scheduler / CPU scheduler | Chọn runnable thread tiếp theo để sử dụng CPU |

Trong hệ điều hành hiện đại, cách tổ chức thực tế có thể khác mô hình sách giáo khoa, nhưng short-term scheduling vẫn là phần gần nhất với CPU scheduler hằng ngày.

---

## 11. Preemption và time slice

### 11.1. Non-preemptive scheduling

Với non-preemptive scheduling, task đang chạy giữ CPU cho tới khi:

- Tự kết thúc.
- Block vì chờ I/O hoặc sự kiện.
- Tự nhường CPU.

Ưu điểm là ít context switch, nhưng một task chạy lâu có thể làm task khác phải chờ.

### 11.2. Preemptive scheduling

Với preemptive scheduling, hệ điều hành có thể tạm dừng thread đang chạy để đưa thread khác lên CPU.

Các nguyên nhân thường gặp:

- Time slice hết.
- Thread có priority cao hơn trở thành runnable.
- Scheduler cần cân bằng tải.
- Thread bị giới hạn bởi policy hoặc quota.

### 11.3. Timer interrupt

CPU và timer hardware cho phép kernel giành lại quyền điều khiển định kỳ:

```text
Thread A đang chạy ở user mode
-> timer interrupt
-> CPU chuyển vào kernel mode
-> kernel cập nhật thời gian chạy
-> scheduler quyết định tiếp tục A hoặc chọn B
```

Nếu không có preemption phù hợp, một chương trình có vòng lặp vô hạn có thể giữ CPU quá lâu.

### 11.4. Time slice

Time slice hoặc quantum là khoảng CPU time một task có thể được chạy trước khi scheduler cân nhắc chuyển task.

- Quantum quá dài: response time của task tương tác có thể kém.
- Quantum quá ngắn: tăng context-switch overhead.

Hệ điều hành hiện đại không nhất thiết dùng một quantum cố định đơn giản cho mọi task; quyết định còn phụ thuộc scheduling class, priority và lịch sử chạy.

---

## 12. Các thuật toán scheduling

Các thuật toán dưới đây giúp hiểu nguyên lý. Hệ điều hành thực tế thường dùng thiết kế phức tạp và kết hợp nhiều ý tưởng.

### 12.1. First-Come, First-Served

FCFS chạy task theo thứ tự đến.

Ví dụ:

| Process | Burst time |
|---|---:|
| P1 | 10 |
| P2 | 2 |
| P3 | 1 |

Nếu thứ tự là `P1 -> P2 -> P3`:

```text
0          10  12  13
|----P1----|P2|P3|
```

Waiting time:

- P1: 0
- P2: 10
- P3: 12

Nhược điểm là **convoy effect**: task ngắn phải chờ phía sau một task dài.

### 12.2. Shortest Job First

SJF chọn task có CPU burst dự đoán ngắn nhất.

Với ví dụ trên:

```text
0  1   3          13
|P3|P2|----P1-----|
```

SJF có thể tối ưu average waiting time trong mô hình lý tưởng, nhưng hệ điều hành không biết chính xác CPU burst tương lai. Nó chỉ có thể ước lượng dựa trên lịch sử.

### 12.3. Shortest Remaining Time First

SRTF là phiên bản preemptive của SJF. Nếu một task mới có thời gian còn lại ngắn hơn task đang chạy, task hiện tại có thể bị preempt.

Nhược điểm:

- Task dài có nguy cơ starvation.
- Cần ước lượng thời gian còn lại.
- Có thể tăng số lần context switch.

### 12.4. Round Robin

Round Robin cho mỗi task một quantum theo vòng:

```text
P1 -> P2 -> P3 -> P1 -> P2 -> ...
```

Ví dụ quantum bằng 2:

```text
0   2   4  5   7   9    11  13
|P1|P2|P3|P1|P2|P1|P1|
```

Round Robin phù hợp với time-sharing vì các task đều có cơ hội chạy tương đối sớm.

### 12.5. Priority scheduling

Task có priority cao hơn được ưu tiên.

Rủi ro:

- Task priority thấp bị starvation.
- Priority không được thiết kế đúng có thể làm hệ thống thiếu công bằng.

Một kỹ thuật giảm starvation là **aging**: task chờ càng lâu càng được tăng priority.

### 12.6. Multilevel Queue

Task được chia vào nhiều queue:

```text
Real-time queue
Interactive queue
Batch queue
Background queue
```

Mỗi queue có thể dùng policy khác nhau. Cần có quy tắc phân phối CPU giữa các queue.

### 12.7. Multilevel Feedback Queue

MLFQ cho phép task di chuyển giữa các queue dựa trên hành vi:

- Task thường xuyên nhường CPU để chờ I/O có thể được ưu tiên phản hồi nhanh.
- Task liên tục dùng hết quantum có thể chuyển xuống queue thấp hơn.
- Aging có thể đưa task chờ lâu lên queue cao hơn.

MLFQ cố gắng học hành vi task mà không cần biết trước CPU burst.

### 12.8. Fair scheduling

Một số scheduler hiện đại hướng tới chia CPU công bằng theo trọng số thay vì chỉ quay vòng một queue đơn giản.

Ý tưởng:

```text
Task có trọng số ngang nhau
-> nhận CPU time gần tương đương trong một khoảng đủ dài

Task có trọng số cao hơn
-> nhận tỷ lệ CPU time lớn hơn
```

Linux có nhiều scheduling class. Normal task và real-time task không dùng hoàn toàn cùng một policy.

---

## 13. Context switch

Context switch là việc CPU chuyển từ execution context này sang context khác.

Ví dụ thread switch:

```text
Thread A đang chạy
-> vào kernel
-> lưu trạng thái A
-> scheduler chọn B
-> khôi phục trạng thái B
-> B tiếp tục chạy
```

### 13.1. Trạng thái cần lưu

Tùy kiến trúc CPU và hệ điều hành:

- Program counter.
- Stack pointer.
- General-purpose register.
- Status register.
- SIMD/FPU state khi cần.
- Kernel scheduling state.

Nếu chuyển giữa process, hệ thống còn có thể phải thay đổi memory mapping context.

### 13.2. Context switch không phải lúc nào cũng là process switch

Có thể xảy ra:

- Chuyển giữa hai thread cùng process.
- Chuyển giữa thread của hai process.
- Chuyển từ user mode sang kernel mode rồi quay lại cùng thread.

Một system call không nhất thiết tạo context switch sang thread khác. Nó có thể chỉ là mode switch:

```text
User mode của thread A
-> kernel mode xử lý system call cho A
-> user mode của A
```

### 13.3. Chi phí của context switch

Chi phí gồm:

- Lưu và khôi phục register.
- Chạy code scheduler.
- Thay đổi memory-management context.
- Mất CPU cache locality.
- TLB bị ảnh hưởng.
- Branch predictor và pipeline bị xáo trộn.

Chi phí gián tiếp do cache lạnh đôi khi lớn hơn chi phí lưu register.

### 13.4. Nhiều thread không luôn tốt hơn

Nếu có quá nhiều runnable thread:

```text
nhiều thread
-> run queue dài
-> context switch nhiều
-> cache locality giảm
-> latency tăng
-> throughput có thể giảm
```

---

## 14. Scheduling trên hệ thống đa lõi

### 14.1. Physical core và logical CPU

Một physical core có thể cung cấp một hoặc nhiều logical CPU nhờ công nghệ simultaneous multithreading.

Ví dụ:

```text
8 physical cores
2 hardware threads mỗi core
-> hệ điều hành có thể thấy 16 logical CPU
```

Hai logical CPU cùng physical core vẫn chia sẻ một số execution resource. Vì vậy 16 logical CPU không đồng nghĩa hiệu năng gấp đôi 8 core trong mọi workload.

### 14.2. Per-CPU run queue

Hệ điều hành có thể duy trì run queue theo CPU để giảm contention:

```text
CPU 0 run queue: T1, T4
CPU 1 run queue: T2
CPU 2 run queue: T3, T5, T6
CPU 3 run queue: trống
```

Scheduler cần cân bằng tải để CPU 3 không rảnh khi CPU 2 có quá nhiều runnable task.

### 14.3. Load balancing

Load balancing di chuyển task giữa các CPU nhằm:

- Phân phối runnable task.
- Tăng throughput.
- Tránh một CPU quá tải trong khi CPU khác rảnh.

Nhưng migration cũng có giá:

- Cache của CPU cũ chứa dữ liệu hữu ích.
- CPU mới có thể phải nạp lại cache.
- NUMA node mới có thể xa vùng memory của task.

### 14.4. CPU affinity

CPU affinity giới hạn hoặc ưu tiên task chạy trên một nhóm CPU.

Lợi ích có thể có:

- Giữ cache locality.
- Cô lập workload quan trọng.
- Phục vụ benchmark.
- Phù hợp với topology phần cứng.

Nhược điểm:

- Giảm khả năng load balancing.
- Cấu hình sai có thể làm một CPU quá tải.
- Task bị ghim có thể cạnh tranh với interrupt hoặc task khác trên cùng CPU.

### 14.5. NUMA

Trong hệ thống Non-Uniform Memory Access:

```text
CPU thuộc NUMA node 0
-> truy cập RAM node 0 nhanh hơn
-> truy cập RAM node 1 có thể chậm hơn
```

Scheduler và memory allocator cần cân nhắc:

- Task chạy ở CPU nào.
- Memory của task nằm ở node nào.
- Có nên migrate task hoặc memory hay không.

NUMA đặc biệt quan trọng với database, JVM heap lớn và workload nhiều socket CPU.

---

## 15. Priority, starvation và priority inversion

### 15.1. Priority

Priority thể hiện mức ưu tiên scheduling. Cách biểu diễn phụ thuộc hệ điều hành và scheduling policy.

Không nên hiểu priority là lời hứa tuyệt đối rằng task sẽ chạy ngay. Task priority cao vẫn có thể:

- Đang blocked.
- Bị giới hạn bởi policy.
- Chờ CPU phù hợp.
- Chờ lock hoặc I/O.

### 15.2. Starvation

Starvation xảy ra khi task liên tục không được cấp đủ tài nguyên để tiến triển.

Ví dụ:

```text
Task priority thấp đang chờ
-> task priority cao liên tục xuất hiện
-> task thấp gần như không được chạy
```

Giải pháp có thể gồm:

- Aging.
- Fair scheduling.
- Giới hạn thời gian task priority cao.
- Thiết kế lại lock và tài nguyên.

### 15.3. Priority inversion

Giả sử:

- Thread `L` có priority thấp và đang giữ lock.
- Thread `H` có priority cao cần lock đó.
- Thread `M` có priority trung bình không cần lock.

Flow:

```text
L giữ lock
-> H chạy và block vì chờ lock của L
-> M liên tục preempt L
-> L không chạy đủ để nhả lock
-> H priority cao phải chờ gián tiếp M
```

Đây là priority inversion.

Một cơ chế xử lý là **priority inheritance**: tạm tăng priority của `L` để nó hoàn thành critical section và nhả lock.

---

## 16. CPU-bound và I/O-bound

### 16.1. CPU-bound workload

CPU-bound task dành phần lớn thời gian tính toán:

- Mã hóa.
- Nén dữ liệu.
- Xử lý ảnh.
- Tính toán số học.
- Parse hoặc transform dữ liệu lớn.

Đặc điểm:

- Thường dùng hết quantum.
- Tăng thread vượt số core hữu ích không nhất thiết tăng throughput.
- Có thể làm run queue dài.

Với workload thuần CPU, kích thước thread pool thường được đặt gần số CPU hữu ích, sau đó đo đạc và điều chỉnh.

### 16.2. I/O-bound workload

I/O-bound task dành phần lớn thời gian chờ:

- Database.
- HTTP downstream.
- File.
- Message broker.
- Socket.

Đặc điểm:

- CPU burst thường ngắn.
- Thread thường chuyển sang blocked.
- Có thể cần concurrency lớn hơn số CPU core để giữ CPU bận trong lúc task khác chờ I/O.

### 16.3. Ví dụ

Giả sử một request:

```text
10 ms dùng CPU
90 ms chờ database
```

Một thread chỉ dùng CPU khoảng 10% thời gian. Nhiều request có thể xen kẽ:

```text
Thread A: CPU -> chờ DB ----------------> CPU
Thread B:       CPU -> chờ DB ----------------> CPU
Thread C:             CPU -> chờ DB ----------------> CPU
```

Tuy nhiên, tăng concurrency không vô hạn vì còn giới hạn:

- Database connection pool.
- Database throughput.
- Memory.
- Socket.
- Downstream rate limit.
- Context-switch overhead.

---

## 17. Load average và CPU utilization

### 17.1. CPU utilization

CPU utilization cho biết tỷ lệ thời gian CPU đang làm việc trong một khoảng đo.

Ví dụ:

```text
CPU utilization = 80%
```

Thông tin này chưa đủ để kết luận hệ thống tốt hay xấu. Cần biết:

- Bao nhiêu core?
- User time hay system time?
- Có I/O wait không?
- Run queue dài không?
- Latency ứng dụng thế nào?

### 17.2. Load average

Trên Linux, load average phản ánh số task trung bình đang runnable hoặc ở một số trạng thái chờ không ngắt được, tùy cách kernel thống kê.

Ví dụ:

```text
load average: 4.00, 3.50, 2.80
```

Các số thường đại diện cho khoảng 1, 5 và 15 phút.

Phải đặt load trong tương quan số logical CPU:

```text
Load 4 trên máy 4 CPU
-> có thể đang dùng gần hết khả năng CPU

Load 4 trên máy 32 CPU
-> chưa chắc CPU bị áp lực
```

Load cao cũng có thể đến từ task chờ I/O ở trạng thái được tính vào load, không chỉ từ tính toán CPU.

### 17.3. Run queue

Run queue dài nghĩa là nhiều runnable thread đang chờ CPU:

```text
Runnable threads > logical CPU khả dụng
-> một số thread phải chờ
-> scheduling latency tăng
```

Đối với backend, CPU utilization chưa đạt 100% nhưng latency vẫn có thể tăng do:

- CPU quota của container.
- Một core nóng trong khi core khác rảnh.
- Lock contention.
- Stop-the-world pause.
- I/O wait.
- Thread pool queue.

---

## 18. Liên hệ với Java và Spring Boot

### 18.1. Một request dùng platform thread

Flow đơn giản:

```text
Request tới socket
-> kernel đánh thức server thread
-> OS đưa thread vào runnable queue
-> scheduler chọn thread
-> thread chạy Spring MVC/controller
-> gọi database
-> thread block chờ socket DB
-> scheduler chạy thread khác
-> DB trả dữ liệu
-> thread cũ trở lại runnable
-> scheduler chọn nó
-> hoàn thành response
```

### 18.2. Thread pool

Thread pool tránh tạo và hủy platform thread cho từng task:

```text
Request/task queue
-> worker thread lấy task
-> xử lý
-> quay lại pool
```

Pool quá nhỏ:

- CPU hoặc downstream còn khả năng nhưng request phải chờ queue.

Pool quá lớn:

- Tăng memory.
- Tăng runnable thread và context switch.
- Tăng áp lực lên database.
- Có thể làm latency tail xấu hơn.

### 18.3. Virtual thread

Với virtual thread:

```text
Mỗi request có thể dùng một virtual thread
-> code blocking dễ đọc
-> JVM unmount khi chờ I/O phù hợp
-> carrier thread chạy virtual thread khác
```

Nhưng vẫn cần giới hạn truy cập tài nguyên khan hiếm:

```text
100.000 virtual thread
!= 100.000 database connection
!= 100.000 query chạy hiệu quả cùng lúc
```

### 18.4. Container CPU limit

Ứng dụng chạy trong container có thể nhìn thấy nhiều CPU của host nhưng chỉ được cấp CPU quota giới hạn.

Ví dụ:

```text
Host: 16 logical CPU
Container quota: tương đương 2 CPU
```

Nếu tạo thread pool như thể có đủ 16 CPU, ứng dụng có thể:

- Tạo quá nhiều runnable thread.
- Bị CPU throttling.
- Tăng latency.
- Có kết quả benchmark khó hiểu.

Khi tuning Java service, cần quan sát cả:

- Số CPU JVM nhận diện.
- CPU request/limit.
- Thread pool.
- GC thread.
- Database pool.
- Run queue và throttling.

### 18.5. Little’s Law ở mức trực giác

Với hệ thống ổn định:

```text
Concurrency ≈ Throughput × Response time
```

Ví dụ:

```text
Throughput = 1.000 request/giây
Response time trung bình = 0,1 giây
Concurrency trung bình ≈ 100 request
```

Nếu downstream chậm làm response time tăng nhưng incoming throughput giữ nguyên, số request đồng thời sẽ tăng. Điều đó kéo theo:

- Nhiều thread hoặc virtual thread hơn.
- Nhiều object sống lâu hơn.
- Nhiều memory hơn.
- Queue dài hơn.

Scheduling không thể tự giải quyết một downstream đã quá tải; hệ thống còn cần timeout, bulkhead, backpressure và rate limit.

---

## 19. Thực hành quan sát trên Linux

### 19.1. Xem process

```bash
ps -ef
ps -o pid,ppid,stat,pri,ni,psr,%cpu,%mem,cmd -p <PID>
```

Một số trường:

| Trường | Ý nghĩa |
|---|---|
| PID | Process ID |
| PPID | Parent Process ID |
| STAT | Trạng thái |
| PRI | Priority kernel hiển thị |
| NI | Nice value |
| PSR | CPU gần nhất task đã chạy |
| %CPU | Tỷ lệ CPU trong khoảng đo |

### 19.2. Xem thread của process

```bash
ps -T -p <PID>
top -H -p <PID>
```

Với Java:

```bash
jcmd <PID> Thread.print
jstack <PID>
```

Lưu ý: ID thread trong công cụ Java và native thread ID có thể được biểu diễn theo cơ số khác nhau. Khi đối chiếu, có thể cần chuyển đổi decimal và hexadecimal.

### 19.3. Xem cây process

```bash
pstree -p
pstree -p <PID>
```

### 19.4. Xem CPU và run queue

```bash
top
vmstat 1
mpstat -P ALL 1
pidstat -u -t -p <PID> 1
```

Trong `vmstat`, trường `r` thường giúp quan sát số task runnable. Cần đọc cùng số CPU và các chỉ số khác.

### 19.5. Xem context switch

```bash
pidstat -w -p <PID> 1
cat /proc/<PID>/status
```

Trong `/proc/<PID>/status` có thể thấy:

```text
voluntary_ctxt_switches
nonvoluntary_ctxt_switches
```

- Voluntary context switch: task tự block hoặc nhường CPU.
- Nonvoluntary context switch: task bị preempt.

### 19.6. Xem CPU affinity

```bash
taskset -cp <PID>
```

Chạy chương trình trên một nhóm CPU:

```bash
taskset -c 0,1 java -jar app.jar
```

Không nên ghim CPU trong production chỉ vì benchmark nhỏ cho kết quả đẹp. Cần hiểu topology, interrupt, NUMA và workload thực.

### 19.7. Quan sát system call liên quan process

```bash
strace -f -e trace=process java -jar app.jar
```

`strace` có overhead và có thể tạo output lớn; nên dùng có chọn lọc trên môi trường phù hợp.

### 19.8. Tìm zombie

```bash
ps -eo pid,ppid,stat,cmd | awk '$3 ~ /^Z/'
```

Khi thấy zombie, nguyên nhân cần sửa thường nằm ở parent process không thu nhận child đúng cách. Kill zombie trực tiếp không giải quyết gốc rễ vì nó đã kết thúc.

---

## 20. Các hiểu lầm thường gặp

### 20.1. “Scheduler lập lịch process, không lập lịch thread”

Trong cách nói đơn giản thường nhắc tới process scheduling, nhưng trên nhiều hệ điều hành hiện đại, đơn vị thực thi được scheduler chọn gần với thread/task hơn. Các thread của cùng process có thể được lập lịch độc lập.

### 20.2. “Một process chỉ chạy trên một CPU”

Một thread tại một thời điểm chạy trên một logical CPU. Process có nhiều runnable thread có thể dùng nhiều CPU song song.

### 20.3. “Thread blocked vẫn chiếm một CPU”

Thông thường thread block vì chờ I/O hoặc lock sẽ được đưa khỏi runnable queue, cho phép CPU chạy thread khác. Nó vẫn giữ một số tài nguyên như stack và metadata.

### 20.4. “CPU 100% luôn là lỗi”

Với workload tính toán, CPU 100% có thể là sử dụng tài nguyên hiệu quả. Nó trở thành vấn đề khi:

- Latency vượt yêu cầu.
- Run queue tăng.
- Task quan trọng bị starvation.
- Hệ thống không còn headroom.
- CPU bị dùng bởi bug hoặc vòng lặp vô ích.

### 20.5. “Nhiều thread luôn tăng throughput”

Chỉ đúng đến một giới hạn. Sau đó contention, context switch, memory pressure và cache miss có thể làm throughput giảm.

### 20.6. “System call nào cũng gây context switch sang thread khác”

System call chuyển CPU vào kernel mode, nhưng kernel có thể xử lý rồi quay lại cùng thread. Chỉ khi thread block, bị preempt hoặc scheduler chọn task khác mới có scheduling switch sang thread khác.

### 20.7. “Priority cao nghĩa là luôn chạy trước”

Priority chỉ là một phần của scheduling policy. Task priority cao nhưng đang chờ lock hoặc I/O vẫn không thể chạy.

### 20.8. “Load average bằng CPU utilization”

Không đúng. CPU utilization là tỷ lệ thời gian CPU bận; load average phản ánh số task trung bình thuộc các trạng thái được Linux tính vào load.

### 20.9. “Virtual thread không liên quan OS thread”

Virtual thread vẫn phải chạy trên carrier thread, và carrier thread vẫn do OS scheduler lập lịch lên CPU.

### 20.10. “Logical CPU và physical core có năng lực như nhau”

Hai logical CPU cùng physical core có thể chia sẻ execution resource. Hiệu năng phụ thuộc workload và kiến trúc CPU.

---

## 21. Câu hỏi tự kiểm tra

1. Program khác process ở điểm nào?
2. Process isolation được xây dựng dựa trên những cơ chế nào?
3. Các thread cùng process chia sẻ tài nguyên gì và có tài nguyên gì riêng?
4. `Ready` khác `Running` và `Blocked` như thế nào?
5. `fork()` và `exec()` có vai trò khác nhau ra sao?
6. Copy-on-write giúp `fork()` giảm chi phí như thế nào?
7. Zombie process hình thành khi nào?
8. Tại sao thread blocked thường không tranh CPU?
9. Preemptive scheduling cần timer interrupt để làm gì?
10. Quantum quá ngắn gây ra vấn đề nào?
11. Convoy effect trong FCFS là gì?
12. SJF khó triển khai hoàn hảo vì sao?
13. MLFQ suy đoán hành vi task bằng cách nào?
14. Context switch ảnh hưởng CPU cache và TLB như thế nào?
15. Mode switch và context switch khác nhau ra sao?
16. Tại sao scheduler không nên di chuyển task giữa CPU quá thường xuyên?
17. CPU affinity có lợi và có hại trong trường hợp nào?
18. Priority inversion xảy ra như thế nào?
19. CPU-bound và I/O-bound cần chiến lược concurrency khác nhau ra sao?
20. Vì sao virtual thread không loại bỏ nhu cầu giới hạn database concurrency?

### Checklist ghi nhớ

- Program là dữ liệu tĩnh; process là môi trường thực thi đang sống.
- Thread là luồng thực thi và thường là đơn vị được scheduler lập lịch.
- Mỗi process có virtual address space riêng.
- Các thread cùng process chia sẻ heap nhưng thường có stack riêng.
- Runnable nghĩa là có thể chạy, không có nghĩa đang chạy.
- Thread blocked thường được đưa khỏi runnable queue.
- Preemption cho phép OS giành lại CPU từ task đang chạy.
- Context switch có cả chi phí trực tiếp và chi phí mất cache locality.
- Nhiều runnable thread hơn số CPU có thể làm scheduling latency tăng.
- CPU affinity giúp locality nhưng có thể cản trở load balancing.
- CPU-bound task bị giới hạn bởi CPU; I/O-bound task chủ yếu bị giới hạn bởi tài nguyên đang chờ.
- Virtual thread tăng khả năng chờ đồng thời, không tăng năng lực tính toán của CPU.
- Scheduler chỉ phân phối CPU; nó không sửa được downstream quá tải hoặc thiết kế concurrency sai.
