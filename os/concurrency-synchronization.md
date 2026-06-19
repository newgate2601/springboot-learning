# Concurrency và Synchronization chuyên sâu

Tài liệu này giải thích cách nhiều thread cùng tiến triển, vì sao dữ liệu chia sẻ có thể sai và các cơ chế đồng bộ phổ biến trong hệ điều hành cũng như Java.

---

## 1. Concurrency và parallelism

- **Concurrency**: nhiều công việc cùng tồn tại và tiến triển xen kẽ.
- **Parallelism**: nhiều công việc thực sự chạy đồng thời trên nhiều logical CPU.

Một CPU vẫn có concurrency nhờ scheduler:

```text
CPU 0: A -> B -> A -> C -> B
```

Nhiều CPU có thể tạo parallelism:

```text
CPU 0: A
CPU 1: B
CPU 2: C
```

Concurrency là cách tổ chức công việc; parallelism là một cách thực thi công việc.

---

## 2. Shared state và race condition

Các thread cùng process chia sẻ heap và biến static. Nếu nhiều thread truy cập dữ liệu có thể thay đổi mà không phối hợp, kết quả phụ thuộc thứ tự xen kẽ.

```java
counter++;
```

Phép tăng có thể gồm:

```text
đọc counter
-> cộng 1
-> ghi counter
```

Hai thread cùng đọc `0`, cùng tính `1` rồi cùng ghi `1`; một lần tăng bị mất.

**Race condition** là lỗi mà kết quả phụ thuộc timing hoặc interleaving giữa các tác vụ.

### Data race

Data race thường chỉ tình huống:

- Hai thread truy cập cùng một vùng nhớ.
- Ít nhất một truy cập là ghi.
- Không có synchronization phù hợp.

Data race có thể dẫn tới race condition, nhưng “race condition” là khái niệm rộng hơn.

---

## 3. Critical section

Critical section là đoạn code truy cập tài nguyên chia sẻ cần được bảo vệ.

Một giải pháp đúng thường hướng tới:

1. **Mutual exclusion**: tại một thời điểm chỉ số thread cho phép được vào.
2. **Progress**: nếu critical section trống, việc chọn thread tiếp theo không bị trì hoãn vô hạn.
3. **Bounded waiting**: một thread không phải chờ vô hạn trong điều kiện bình thường.

Ví dụ:

```java
synchronized (lock) {
    balance = balance - amount;
}
```

Không nên làm critical section lớn hơn cần thiết vì lock bị giữ lâu sẽ làm tăng contention.

---

## 4. Atomicity, visibility và ordering

Đúng trong môi trường đa thread cần xem xét ba thuộc tính.

### 4.1. Atomicity

Một operation atomic xuất hiện như một bước không thể quan sát trạng thái dở dang.

```java
AtomicInteger counter = new AtomicInteger();
counter.incrementAndGet();
```

Atomic không có nghĩa mọi chuỗi operation đều tự động atomic:

```java
if (balance >= amount) {
    balance -= amount;
}
```

Ngay cả khi đọc và ghi riêng lẻ atomic, toàn bộ “check-then-act” vẫn cần bảo vệ.

### 4.2. Visibility

Một thread ghi dữ liệu không đảm bảo thread khác lập tức nhìn thấy nếu thiếu quan hệ đồng bộ.

CPU cache, compiler và runtime có thể giữ hoặc tối ưu truy cập memory. Synchronization tạo ra quy tắc công bố thay đổi giữa các thread.

### 4.3. Ordering

Compiler và CPU có thể reorder instruction nếu không làm thay đổi kết quả đơn luồng. Trong đa luồng, thread khác có thể quan sát thứ tự ngoài dự kiến nếu thiếu synchronization.

---

## 5. Happens-before

Happens-before là quan hệ đảm bảo visibility và ordering giữa các hành động.

Một số ví dụ trong Java:

- Unlock một monitor happens-before lần lock tiếp theo trên cùng monitor.
- Ghi vào biến `volatile` happens-before lần đọc sau đó của biến đó.
- Gọi `Thread.start()` happens-before hành động trong thread được khởi động.
- Mọi hành động trong thread happens-before thread khác trở về thành công từ `join()`.

Happens-before không đơn thuần là “xảy ra sớm hơn theo đồng hồ”; nó là bảo đảm của memory model.

---

## 6. Mutex và monitor

### 6.1. Mutex

Mutex bảo đảm mutual exclusion:

```text
Thread A lock
-> A vào critical section
-> B gọi lock và phải chờ
-> A unlock
-> B có thể tiếp tục
```

Mutex thường có owner: thread lock phải là thread unlock.

### 6.2. Monitor

Monitor kết hợp:

- Mutual exclusion.
- Dữ liệu được bảo vệ.
- Condition để thread chờ trạng thái.

Trong Java, `synchronized` sử dụng monitor:

```java
synchronized void withdraw(long amount) {
    balance -= amount;
}
```

Hoặc:

```java
synchronized (account) {
    account.withdraw(amount);
}
```

### 6.3. Reentrant lock

Reentrant lock cho phép thread đang giữ lock tiếp tục lock lại cùng lock mà không tự deadlock. Java intrinsic lock và `ReentrantLock` đều hỗ trợ reentrancy.

---

## 7. Semaphore

Semaphore giữ một số permit.

### Binary semaphore

Có tối đa một permit, gần giống mutual exclusion nhưng semantics owner có thể khác mutex.

### Counting semaphore

Cho phép tối đa `N` tác vụ cùng dùng tài nguyên:

```java
Semaphore permits = new Semaphore(20);

permits.acquire();
try {
    callDownstream();
} finally {
    permits.release();
}
```

Ứng dụng:

- Giới hạn số request đồng thời.
- Bảo vệ connection hoặc thiết bị hữu hạn.
- Xây dựng bulkhead.

Semaphore không tự bảo đảm downstream chịu được giá trị `N`; cần đo tải thực tế.

---

## 8. Condition variable

Condition variable cho phép thread ngủ cho tới khi một điều kiện logic có thể đã thay đổi.

Ví dụ producer–consumer:

```text
Consumer thấy queue rỗng
-> chờ notEmpty

Producer thêm phần tử
-> signal notEmpty
-> consumer thức dậy và kiểm tra lại
```

Điều kiện phải được kiểm tra trong vòng lặp:

```java
lock.lock();
try {
    while (queue.isEmpty()) {
        notEmpty.await();
    }
    return queue.remove();
} finally {
    lock.unlock();
}
```

Lý do:

- Có thể thức dậy giả.
- Thread khác có thể lấy dữ liệu trước.
- Signal nghĩa là “điều kiện có thể đúng”, không phải chắc chắn vẫn đúng lúc thread chạy.

---

## 9. Spinlock và blocking lock

### Spinlock

Thread liên tục kiểm tra lock:

```text
while (lock đang bận) {
    spin
}
```

Phù hợp khi:

- Critical section rất ngắn.
- Thời gian chờ nhỏ hơn chi phí sleep/wakeup.
- Dùng trong ngữ cảnh kernel hoặc runtime phù hợp.

Không phù hợp khi lock được giữ lâu vì nó tiêu tốn CPU trong lúc chờ.

### Blocking lock

Thread không lấy được lock sẽ block để scheduler chạy thread khác. Phù hợp với thời gian chờ dài hơn nhưng có chi phí park/unpark và scheduling.

Nhiều implementation hiện đại kết hợp spin ngắn rồi mới block.

---

## 10. Compare-and-swap và lock-free

Compare-and-swap, viết tắt CAS, cập nhật giá trị nếu nó vẫn bằng giá trị mong đợi:

```text
CAS(address, expected, newValue)

nếu current == expected:
    current = newValue
    thành công
ngược lại:
    thất bại
```

Ví dụ logic tăng counter:

```text
lặp:
    old = counter
    new = old + 1
    nếu CAS(counter, old, new) thành công:
        kết thúc
```

CAS là nền tảng của nhiều atomic class và cấu trúc lock-free.

### Trade-off

- Không block khi contention thấp.
- Có thể retry nhiều lần khi contention cao.
- Dễ gặp lỗi thiết kế tinh vi như ABA.
- Lock-free không đồng nghĩa wait-free.
- Code lock-free thường khó chứng minh đúng hơn code dùng lock.

---

## 11. ABA problem

Một thread đọc giá trị `A`, sau đó thread khác đổi:

```text
A -> B -> A
```

CAS chỉ kiểm tra giá trị hiện tại là `A` nên không biết nó đã từng thay đổi.

Giải pháp tùy bài toán:

- Gắn version hoặc stamp.
- Dùng tagged pointer.
- Quản lý vòng đời object phù hợp.
- Dùng primitive cấp cao đã được kiểm chứng.

---

## 12. Producer–consumer

Producer tạo dữ liệu, consumer xử lý dữ liệu:

```text
Producer -> bounded queue -> Consumer
```

Queue hữu hạn tạo backpressure:

- Queue rỗng: consumer chờ.
- Queue đầy: producer chờ, thất bại hoặc áp dụng policy.

Trong Java nên ưu tiên primitive như `BlockingQueue`:

```java
BlockingQueue<Job> queue = new ArrayBlockingQueue<>(1000);

queue.put(job);
Job next = queue.take();
```

Queue vô hạn che giấu overload cho tới khi memory tăng mạnh.

---

## 13. Readers–writers

Bài toán có:

- Nhiều reader có thể đọc đồng thời.
- Writer cần quyền độc quyền.

Các policy:

- Ưu tiên reader: writer có thể starvation.
- Ưu tiên writer: reader có thể phải chờ lâu.
- Công bằng: giảm starvation nhưng có overhead.

Java cung cấp `ReadWriteLock`, nhưng nó chỉ có lợi khi:

- Read nhiều hơn write đáng kể.
- Critical section đủ lớn.
- Contention thực sự tồn tại.

Nếu operation quá ngắn, overhead quản lý read/write lock có thể không đáng.

---

## 14. Thread safety

Một component thread-safe vẫn đúng khi được nhiều thread sử dụng theo contract.

Các chiến lược:

- Immutable object.
- Thread confinement.
- Stateless service.
- Lock bảo vệ mutable state.
- Atomic variable.
- Concurrent collection.
- Message passing.

### Immutable object

Object không thay đổi sau khi tạo thường dễ chia sẻ:

```java
public record Money(long amount, String currency) {}
```

Immutable không tự động đúng nếu object chứa tham chiếu tới collection mutable bị lộ ra ngoài.

### Thread confinement

Dữ liệu chỉ thuộc một thread:

- Local variable.
- Thread-local state.
- Actor/event-loop sở hữu state.

Giảm chia sẻ thường tốt hơn cố đồng bộ mọi thứ.

---

## 15. Java synchronization primitives

| Công cụ | Phù hợp |
|---|---|
| `synchronized` | Mutual exclusion và visibility đơn giản |
| `volatile` | Công bố giá trị, cờ trạng thái; không dành cho compound update |
| `ReentrantLock` | Cần `tryLock`, interruptible lock hoặc nhiều condition |
| `AtomicInteger` | Counter và update đơn giản dựa trên CAS |
| `LongAdder` | Counter contention cao, không cần snapshot tuyệt đối tức thời |
| `Semaphore` | Giới hạn concurrency |
| `CountDownLatch` | Chờ một nhóm tác vụ hoàn thành một lần |
| `CyclicBarrier` | Nhiều thread gặp nhau tại barrier có thể tái sử dụng |
| `BlockingQueue` | Producer–consumer |
| `ConcurrentHashMap` | Map concurrent |

Không nên tự viết lock, queue hoặc atomic primitive nếu thư viện chuẩn đã có giải pháp phù hợp.

---

## 16. `volatile` không thay thế lock

`volatile` phù hợp với cờ đơn giản:

```java
private volatile boolean running = true;
```

Nhưng không làm operation sau thành atomic:

```java
volatile int count;
count++;
```

`count++` vẫn là read–modify–write. Dùng `AtomicInteger`, `synchronized` hoặc lock phù hợp.

---

## 17. Lock contention

Contention xảy ra khi nhiều thread cạnh tranh cùng lock:

```text
nhiều request
-> cùng vào một synchronized block
-> chỉ một thread tiến triển
-> các thread còn lại chờ
```

Giảm contention bằng cách:

- Thu nhỏ critical section.
- Tránh I/O khi đang giữ lock.
- Chia nhỏ lock theo shard/key.
- Dùng immutable snapshot.
- Dùng concurrent collection.
- Giảm shared mutable state.
- Batch operation khi phù hợp.

Không nên chia lock quá nhỏ nếu làm tăng độ phức tạp và nguy cơ deadlock.

---

## 18. False sharing

Hai thread cập nhật hai biến logic độc lập nhưng các biến nằm cùng cache line:

```text
CPU 0 ghi biến A
CPU 1 ghi biến B
A và B cùng cache line
-> cache line liên tục bị invalidation
```

Kết quả là hiệu năng giảm dù không có lock logic.

False sharing thường xuất hiện trong counter, queue và cấu trúc dữ liệu hiệu năng cao. Chỉ nên tối ưu sau khi profiler hoặc benchmark đáng tin cậy chỉ ra vấn đề.

---

## 19. Liên hệ Spring Boot

Spring singleton bean được nhiều request thread dùng chung:

```java
@Service
public class OrderService {
    private long processed;
}
```

Nếu `processed` bị cập nhật từ nhiều request, bean có shared mutable state và cần thiết kế thread-safe.

Ưu tiên service stateless:

```java
@Service
public class PriceService {
    public Money calculate(Order order) {
        return ...;
    }
}
```

Các điểm dễ lỗi:

- Dùng `HashMap` mutable trong singleton.
- Giữ request data trong field.
- Cache tự viết không có synchronization.
- Giữ lock trong lúc gọi database hoặc HTTP.
- Dùng thread-local nhưng quên cleanup trong thread pool.

---

## 20. Câu hỏi tự kiểm tra

1. Concurrency khác parallelism thế nào?
2. `counter++` vì sao không atomic?
3. Atomicity, visibility và ordering khác nhau ra sao?
4. Happens-before cung cấp bảo đảm gì?
5. Mutex khác semaphore ở điểm nào?
6. Vì sao condition phải kiểm tra bằng `while`?
7. Khi nào spinlock có thể hữu ích?
8. CAS hoạt động theo nguyên lý nào?
9. `volatile` giải quyết được và không giải quyết được gì?
10. Tại sao giảm shared mutable state thường tốt hơn thêm nhiều lock?

### Checklist

- Shared mutable state là nguồn chính của lỗi concurrency.
- Lock vừa bảo vệ atomicity vừa tạo quan hệ memory visibility.
- `volatile` không biến compound operation thành atomic.
- Condition signal chỉ báo trạng thái có thể đã thay đổi.
- Queue hữu hạn là một công cụ backpressure.
- Lock-free không có nghĩa đơn giản hoặc luôn nhanh hơn.
- Spring singleton nên stateless hoặc được thiết kế thread-safe.
