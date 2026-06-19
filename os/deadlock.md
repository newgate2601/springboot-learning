# Deadlock, Livelock và Starvation

Tài liệu này trình bày cách hệ thống bị kẹt khi các thread giữ và chờ tài nguyên, phương pháp phòng tránh, phát hiện và xử lý, cùng ví dụ Java thực tế.

---

## 1. Deadlock là gì?

Deadlock xảy ra khi một nhóm task chờ lẫn nhau và không task nào có thể tiến triển.

```text
Thread A giữ Lock 1, chờ Lock 2
Thread B giữ Lock 2, chờ Lock 1
```

Không giống một task chạy chậm, deadlock không tự hết nếu không có thay đổi bên ngoài.

---

## 2. Bốn điều kiện Coffman

Deadlock có thể xảy ra khi đồng thời tồn tại:

1. **Mutual exclusion**: tài nguyên chỉ được một task sử dụng tại một thời điểm.
2. **Hold and wait**: task giữ ít nhất một tài nguyên trong khi chờ tài nguyên khác.
3. **No preemption**: tài nguyên không thể bị hệ thống cưỡng chế thu hồi an toàn.
4. **Circular wait**: tồn tại vòng chờ tài nguyên.

```text
T1 chờ tài nguyên của T2
T2 chờ tài nguyên của T3
T3 chờ tài nguyên của T1
```

Phá vỡ ít nhất một điều kiện sẽ ngăn loại deadlock tương ứng.

---

## 3. Ví dụ Java

```java
Object accountA = new Object();
Object accountB = new Object();

Thread t1 = Thread.ofPlatform().start(() -> {
    synchronized (accountA) {
        synchronized (accountB) {
            transferAtoB();
        }
    }
});

Thread t2 = Thread.ofPlatform().start(() -> {
    synchronized (accountB) {
        synchronized (accountA) {
            transferBtoA();
        }
    }
});
```

Nếu `t1` giữ `accountA` còn `t2` giữ `accountB`, cả hai có thể chờ vô hạn.

---

## 4. Resource-allocation graph

Biểu diễn:

- Task là node.
- Resource là node.
- `Task -> Resource`: task đang yêu cầu resource.
- `Resource -> Task`: resource đang được task giữ.

```text
T1 -> R2 -> T2 -> R1 -> T1
```

Với mỗi resource chỉ có một instance, chu trình là dấu hiệu deadlock. Nếu resource có nhiều instance, có chu trình chưa chắc đã đủ kết luận.

---

## 5. Deadlock prevention

Prevention thiết kế hệ thống để ít nhất một điều kiện Coffman không thể xảy ra.

### 5.1. Lock ordering

Mọi thread lấy lock theo cùng thứ tự:

```text
Lock nhỏ hơn trước, lock lớn hơn sau
```

Ví dụ chuyển tiền:

```java
Account first = a.id() < b.id() ? a : b;
Account second = a.id() < b.id() ? b : a;

synchronized (first) {
    synchronized (second) {
        transfer(a, b, amount);
    }
}
```

Lock ordering là giải pháp thực dụng và dễ kiểm tra.

### 5.2. Không hold-and-wait

Yêu cầu toàn bộ tài nguyên cùng lúc hoặc nhả tài nguyên đang giữ trước khi chờ tài nguyên khác.

Trade-off:

- Giảm sử dụng tài nguyên.
- Có thể khó biết trước toàn bộ tài nguyên.
- Tăng retry.

### 5.3. Cho phép timeout hoặc preemption logic

```java
if (lock.tryLock(500, TimeUnit.MILLISECONDS)) {
    try {
        ...
    } finally {
        lock.unlock();
    }
}
```

Timeout không chứng minh hệ thống không deadlock, nhưng tạo đường thoát và cơ hội retry.

### 5.4. Giảm mutual exclusion

Dùng:

- Immutable data.
- Message passing.
- Lock-free structure.
- Transaction hoặc optimistic concurrency.

Không phải tài nguyên nào cũng có thể bỏ mutual exclusion.

---

## 6. Deadlock avoidance

Avoidance chỉ cấp tài nguyên nếu hệ thống vẫn ở **safe state**.

### Banker’s algorithm

Thuật toán cần biết:

- Tổng tài nguyên.
- Tài nguyên đang cấp.
- Nhu cầu tối đa của từng process.

Hệ thống mô phỏng việc cấp và kiểm tra có tồn tại thứ tự hoàn thành an toàn hay không.

Trong server tổng quát, nhu cầu thường khó biết trước nên Banker’s algorithm chủ yếu hữu ích để hiểu khái niệm safe state.

---

## 7. Deadlock detection

Hệ thống có thể cho phép deadlock xảy ra rồi phát hiện.

### 7.1. Wait-for graph

Nếu chỉ quan tâm task:

```text
T1 -> T2: T1 chờ tài nguyên do T2 giữ
```

Chu trình trong wait-for graph biểu thị deadlock.

### 7.2. Java thread dump

```bash
jcmd <PID> Thread.print
jstack <PID>
```

JVM có thể phát hiện một số monitor/ownable synchronizer deadlock và chỉ ra:

- Thread nào đang chờ.
- Lock nào đang cần.
- Thread nào đang giữ lock.
- Stack trace liên quan.

Thread dump không phát hiện được mọi deadlock ở tầng database, distributed system hoặc logic ứng dụng.

---

## 8. Recovery

Khi phát hiện deadlock:

- Hủy một task.
- Rollback transaction.
- Thu hồi tài nguyên nếu an toàn.
- Restart component.
- Fail fast và để supervisor khởi động lại.

Chọn victim cần cân nhắc:

- Công việc đã thực hiện.
- Chi phí rollback.
- Mức độ quan trọng.
- Số tài nguyên đang giữ.
- Nguy cơ lặp lại deadlock.

Restart chỉ là recovery, không phải sửa nguyên nhân.

---

## 9. Database deadlock

Hai transaction có thể khóa row theo thứ tự ngược nhau:

```text
Tx A khóa row 1, chờ row 2
Tx B khóa row 2, chờ row 1
```

Database thường phát hiện chu trình và abort một transaction.

Ứng dụng cần:

- Giữ transaction ngắn.
- Truy cập resource theo thứ tự nhất quán.
- Có retry giới hạn với backoff cho lỗi deadlock phù hợp.
- Không giữ transaction mở trong lúc gọi HTTP chậm.
- Đảm bảo operation retry là idempotent hoặc được bảo vệ.

---

## 10. Distributed deadlock

Deadlock có thể vượt khỏi một process:

```text
Service A giữ tài nguyên A, gọi đồng bộ B
Service B giữ tài nguyên B, gọi đồng bộ A
```

Hoặc:

```text
Tx database giữ lock
-> gọi API ngoài
-> API ngoài chờ operation cần chính lock đó
```

Biện pháp:

- Timeout mọi lời gọi từ xa.
- Tránh call cycle.
- Không giữ lock/transaction qua network call.
- Dùng saga hoặc workflow bất đồng bộ.
- Quan sát dependency graph.

---

## 11. Livelock

Trong livelock, task không block vĩnh viễn; chúng liên tục phản ứng với nhau nhưng không hoàn thành.

```text
T1 thấy xung đột -> nhường
T2 thấy xung đột -> nhường
T1 retry cùng lúc
T2 retry cùng lúc
-> lặp lại
```

Giải pháp:

- Randomized backoff.
- Giới hạn retry.
- Dùng coordinator.
- Thay đổi policy ưu tiên.

---

## 12. Starvation

Thread vẫn có khả năng chạy về lý thuyết nhưng liên tục bị bỏ qua:

- Priority quá thấp.
- Lock không công bằng.
- Reader liên tục khiến writer không vào được.
- Queue ưu tiên luôn có task mới quan trọng hơn.

Giải pháp:

- Fair lock/queue.
- Aging.
- Giới hạn công việc ưu tiên cao.
- Chia quota.

Fairness thường làm giảm một phần throughput, nên cần dựa trên yêu cầu thực tế.

---

## 13. Thread pool starvation

Không có vòng lock vẫn có thể bị kẹt vì task chờ task khác trong cùng pool.

```text
Pool có 10 worker
-> 10 task cha chiếm hết worker
-> mỗi task cha submit task con và chờ kết quả
-> task con không có worker để chạy
```

Giải pháp:

- Không block chờ task con trong pool đã bão hòa.
- Tách pool theo loại workload.
- Dùng structured concurrency hoặc composition phù hợp.
- Đặt queue và timeout.
- Quan sát active thread và queue depth.

---

## 14. Connection pool deadlock

Ví dụ:

```text
Mỗi request giữ connection 1
-> tất cả cùng cần thêm connection 2
-> pool đã hết vì connection 1 chưa được trả
```

Phòng tránh:

- Không lấy nhiều connection nếu không cần.
- Quy định thứ tự lấy resource.
- Timeout khi borrow.
- Đảm bảo `finally`/try-with-resources.
- Kích thước pool phải phù hợp với pattern sử dụng, không chỉ số request.

---

## 15. Checklist thiết kế

- Có đang giữ lock khi gọi database, file hoặc HTTP không?
- Có nhiều lock được lấy theo thứ tự khác nhau không?
- Mọi lần lock có unlock trong `finally` không?
- Có timeout cho lock, connection và network không?
- Task trong thread pool có chờ task cùng pool không?
- Transaction có giữ mở lâu hơn cần thiết không?
- Retry có giới hạn và backoff không?
- Có thể thay shared mutable state bằng message passing không?

---

## 16. Câu hỏi tự kiểm tra

1. Bốn điều kiện Coffman là gì?
2. Lock ordering phá vỡ điều kiện nào?
3. Safe state khác deadlock-free tại thời điểm hiện tại thế nào?
4. Timeout giúp gì và không bảo đảm điều gì?
5. Livelock khác deadlock ra sao?
6. Starvation khác deadlock ra sao?
7. Vì sao không nên giữ database transaction khi gọi HTTP?
8. Thread pool starvation hình thành thế nào?
9. Vì sao retry deadlock cần idempotency?
10. Restart service có phải sửa deadlock không?
