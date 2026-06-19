# I/O Model: Blocking, Non-blocking và I/O Multiplexing

Tài liệu này tập trung vào cách application chờ và xử lý I/O. Đường đi của byte qua socket, TCP/IP, driver và NIC được trình bày riêng trong [I-O.md](I-O.md).

---

## 1. Hai câu hỏi phải tách biệt

Khi nói về I/O cần phân biệt:

1. Thread có phải chờ operation không?
2. Ai thông báo hoặc hoàn thành operation?

Các thuật ngữ blocking, non-blocking, synchronous và asynchronous thường bị dùng lẫn vì chúng trả lời các góc nhìn khác nhau.

---

## 2. Blocking I/O

Với blocking read:

```text
Application gọi read()
-> chưa có dữ liệu
-> thread bị block
-> scheduler chạy thread khác
-> dữ liệu tới
-> thread trở lại runnable
-> read() trả dữ liệu
```

Điểm quan trọng:

- Thread không nhất thiết tiêu tốn CPU trong lúc block.
- Thread vẫn giữ stack và metadata.
- Với platform thread, quá nhiều thread block có thể tốn memory và tài nguyên kernel.

Ví dụ Java:

```java
byte[] data = inputStream.readNBytes(1024);
```

---

## 3. Non-blocking I/O

File descriptor được đặt non-blocking. Nếu operation chưa thể hoàn thành ngay, system call trả về trạng thái kiểu “thử lại sau”.

```text
read()
-> chưa có dữ liệu
-> trả EAGAIN/EWOULDBLOCK
-> application làm việc khác
```

Nếu application tự gọi `read()` liên tục:

```text
while chưa có dữ liệu:
    read()
```

đó là busy polling và có thể lãng phí CPU. Vì vậy non-blocking I/O thường kết hợp I/O multiplexing.

---

## 4. Synchronous và asynchronous I/O

### Synchronous I/O

Operation hoàn thành trong flow gọi của application. Thread hoặc event loop vẫn tham gia kiểm tra readiness và thực hiện read/write.

Blocking và readiness-based non-blocking thường được xem là synchronous theo nghĩa này.

### Asynchronous I/O

Application submit operation:

```text
hãy đọc buffer này
-> kernel/runtime thực hiện
-> báo completion khi xong
```

Application nhận kết quả qua:

- Callback.
- Future/promise.
- Completion queue.
- Event.

Tên gọi cụ thể phụ thuộc OS và API; không nên suy ra implementation chỉ từ tên framework.

---

## 5. Readiness và completion

### Readiness model

Hệ thống báo:

> Descriptor hiện có khả năng thực hiện operation mà không block theo cách thông thường.

Sau đó application gọi `read()` hoặc `write()`.

Ví dụ:

- `select`
- `poll`
- `epoll`
- `kqueue`

### Completion model

Hệ thống báo:

> Operation đã hoàn thành; kết quả nằm ở đây.

Ví dụ kiến trúc:

- Windows IOCP.
- Một số asynchronous I/O API.
- `io_uring` với submission/completion queue cho các operation được hỗ trợ.

---

## 6. `select`

Application truyền tập descriptor cần theo dõi:

```text
read set
write set
exception set
```

Kernel trả về descriptor sẵn sàng.

Hạn chế:

- Phải xây dựng và copy tập descriptor.
- Thường quét toàn bộ tập.
- Có giới hạn descriptor tùy implementation.
- Không lý tưởng với số connection rất lớn.

---

## 7. `poll`

`poll` dùng mảng descriptor và event:

```text
fd 7: chờ readable
fd 8: chờ writable
fd 9: chờ readable
```

Nó tránh một số giới hạn API của `select`, nhưng vẫn thường cần quét danh sách mỗi lần.

---

## 8. `epoll`

Linux `epoll` cho phép:

1. Tạo epoll instance.
2. Đăng ký descriptor quan tâm.
3. Chờ danh sách event sẵn sàng.

```text
epoll interest list
-> kernel theo dõi
-> epoll_wait()
-> trả các fd có event
```

Lợi ích:

- Không cần truyền lại toàn bộ descriptor mỗi lần.
- Phù hợp với nhiều connection phần lớn đang idle.
- Trả danh sách event ready.

### Level-triggered

Tiếp tục báo khi trạng thái vẫn ready:

```text
socket còn dữ liệu
-> lần wait sau vẫn có thể được báo
```

Dễ dùng hơn nhưng có thể có nhiều thông báo.

### Edge-triggered

Báo khi trạng thái chuyển:

```text
not-ready -> ready
```

Application thường phải đọc tới khi nhận `EAGAIN`; nếu không drain hết, có thể bỏ lỡ cơ hội xử lý tiếp.

---

## 9. `kqueue`

`kqueue` phổ biến trên BSD/macOS. Nó theo dõi event từ:

- Socket.
- File.
- Process.
- Signal.
- Timer.

Ý tưởng tương tự event notification nhưng API và semantics khác `epoll`.

---

## 10. IOCP

I/O Completion Ports trên Windows tổ chức completion event thành queue:

```text
submit I/O
-> operation hoàn thành
-> completion packet vào IOCP
-> worker lấy completion
```

IOCP hỗ trợ kiểm soát số worker hoạt động và phù hợp server concurrency lớn.

---

## 11. `io_uring`

Linux `io_uring` dùng:

- Submission Queue.
- Completion Queue.

```text
Application ghi submission entry
-> kernel xử lý operation
-> completion entry xuất hiện
```

Mục tiêu:

- Giảm system-call overhead.
- Batch operation.
- Hỗ trợ nhiều loại I/O qua mô hình thống nhất hơn.

Không phải mọi operation, kernel version hoặc thư viện đều có behavior giống nhau. Cần kiểm tra implementation thực tế trước khi kết luận “hoàn toàn async”.

---

## 12. Reactor pattern

Reactor thường dùng readiness:

```text
Event loop
-> chờ fd ready
-> dispatch handler
-> handler thực hiện read/write ngắn
-> quay lại event loop
```

Ví dụ:

```text
epoll_wait
-> socket A readable
-> đọc request A
-> gọi handler A
-> socket B writable
-> ghi response B
```

Handler không nên block lâu vì sẽ chặn event loop xử lý connection khác.

---

## 13. Proactor pattern

Proactor xoay quanh completion:

```text
submit read
-> operation diễn ra
-> completion event
-> completion handler xử lý kết quả
```

Ranh giới Reactor/Proactor trong framework có thể bị trộn bởi lớp runtime, worker pool và API bất đồng bộ.

---

## 14. Thread-per-connection và thread-per-request

### Thread-per-connection

Mỗi connection có một platform thread chờ và xử lý.

Ưu:

- Code tuần tự dễ đọc.
- Debug stack trace trực quan.

Nhược:

- Nhiều connection idle vẫn giữ thread.
- Stack và kernel thread resource lớn.

### Thread-per-request

Thread được pool phân cho request:

```text
accept connection
-> request tới
-> đưa vào worker pool
-> worker xử lý
```

Pool giới hạn concurrency nhưng queue có thể tăng khi overload.

---

## 15. Event loop

Một hoặc một số thread xử lý nhiều connection:

```text
Event loop 1 -> connection 1..10.000
Event loop 2 -> connection 10.001..20.000
```

Hiệu quả khi handler:

- Không block.
- Thực hiện ít CPU.
- Nhanh chóng trả quyền điều khiển.

Một callback block 500 ms có thể làm hàng nghìn connection cùng event loop bị trì hoãn.

---

## 16. Java NIO

Java NIO cung cấp:

- `Channel`.
- `Buffer`.
- `Selector`.

Flow:

```text
SocketChannel non-blocking
-> đăng ký Selector
-> select()
-> nhận SelectionKey ready
-> read/write
```

`Selector` ánh xạ xuống cơ chế OS phù hợp tùy nền tảng và JDK.

---

## 17. Netty

Netty dùng event loop và channel pipeline:

```text
OS event mechanism
-> Netty EventLoop
-> ChannelPipeline
-> handler
```

Không nên chạy blocking database/HTTP call trực tiếp trên event-loop thread. Nếu buộc phải dùng blocking API:

- Chuyển sang executor phù hợp.
- Giới hạn queue và concurrency.
- Theo dõi context-switch và latency.

---

## 18. Servlet, Spring MVC và WebFlux

### Spring MVC

Thường dùng mô hình blocking request handling trên server thread:

```text
request
-> server worker/platform hoặc virtual thread
-> controller
-> blocking DB/HTTP
```

### Spring WebFlux

Thường dựa trên event loop và reactive pipeline:

```text
event
-> non-blocking operator
-> callback tiếp theo khi dữ liệu sẵn sàng
```

WebFlux không biến JDBC blocking thành non-blocking. Nếu pipeline gọi blocking code trên event loop, lợi ích bị phá vỡ.

---

## 19. Virtual Thread và blocking style

Virtual thread cho phép giữ cách viết tuần tự:

```java
var result = httpClient.send(request, handler);
var data = repository.load(id);
```

Khi operation tương thích, JVM có thể unmount virtual thread khỏi carrier trong lúc chờ.

Điều này giảm nhu cầu tự viết callback cho nhiều workload I/O-bound, nhưng vẫn cần:

- Timeout.
- Rate limit.
- Connection pool.
- Backpressure.
- Giới hạn downstream concurrency.

---

## 20. Backpressure

Backpressure ngăn producer tạo công việc nhanh hơn consumer xử lý vô hạn:

```text
incoming rate > processing rate
-> queue tăng
-> memory tăng
-> latency tăng
-> timeout hàng loạt
```

Các biện pháp:

- Bounded queue.
- Semaphore.
- Rate limit.
- Drop/reject policy.
- Reactive demand.
- Load shedding.

Non-blocking I/O không tự giải quyết overload.

---

## 21. Partial read và partial write

TCP là byte stream. Một lần `read()`:

- Có thể trả ít byte hơn message.
- Có thể chứa một phần hoặc nhiều message.

Một lần `write()` non-blocking cũng có thể chỉ ghi được một phần buffer.

Application protocol cần framing:

- Length prefix.
- Delimiter.
- Fixed-size frame.
- Protocol parser như HTTP.

Không được giả định:

```text
1 lần send = 1 lần recv
```

---

## 22. Thundering herd

Nhiều thread/process cùng thức dậy cho một event:

```text
1 connection tới
-> 100 worker thức dậy
-> 1 worker xử lý
-> 99 worker lãng phí scheduling
```

Kernel và server runtime có nhiều kỹ thuật giảm hiện tượng này, nhưng cấu hình nhiều acceptor hoặc waiter vẫn cần thận trọng.

---

## 23. Chọn mô hình nào?

| Workload | Hướng phù hợp |
|---|---|
| Ít connection, code đơn giản | Blocking platform thread |
| Nhiều request I/O-bound, muốn code tuần tự | Virtual thread |
| Rất nhiều connection idle, stack non-blocking hoàn chỉnh | Event loop/reactive |
| Tính toán CPU nặng | Pool giới hạn gần CPU hữu ích |
| Library bắt buộc blocking | Dedicated bounded pool hoặc virtual thread phù hợp |

Không chọn chỉ theo số request. Cần đo:

- Throughput.
- p95/p99 latency.
- CPU.
- Memory.
- Context switch.
- Queue depth.
- Downstream saturation.

---

## 24. Câu hỏi tự kiểm tra

1. Blocking thread có dùng CPU liên tục khi chờ không?
2. Non-blocking I/O vì sao thường cần multiplexing?
3. Readiness khác completion thế nào?
4. Level-triggered khác edge-triggered ra sao?
5. Reactor khác Proactor ở ý tưởng nào?
6. Vì sao event-loop handler không nên block?
7. Virtual thread khác event loop ra sao?
8. Partial read/write ảnh hưởng protocol thế nào?
9. Non-blocking I/O có tự tạo backpressure không?
10. Khi nào thread pool cho CPU-bound nên được giới hạn?
