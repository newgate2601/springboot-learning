# Virtual Memory chuyên sâu

Tài liệu này đi sâu vào cách CPU, MMU và kernel biến địa chỉ ảo thành RAM vật lý, xử lý page fault, thay thế page và liên hệ với JVM.

---

## 1. Vì sao cần virtual memory?

Virtual memory cung cấp:

- Không gian địa chỉ riêng cho mỗi process.
- Cô lập và bảo vệ memory.
- Demand paging.
- Shared library và shared memory.
- Memory-mapped file.
- Cách nhìn liên tục dù physical frame phân tán.

```text
Process A virtual page 1 -> physical frame 9
Process B virtual page 1 -> physical frame 31
```

---

## 2. Virtual address translation

CPU tạo virtual address:

```text
Virtual address = virtual page number + offset
```

MMU tra page table:

```text
virtual page number
-> page-table entry
-> physical frame number
-> physical address = frame + offset
```

Page-table entry thường chứa:

- Frame number.
- Present/valid bit.
- Read/write/execute permission.
- User/kernel permission.
- Accessed/reference bit.
- Dirty bit.

---

## 3. Page và frame

- **Page**: block trong virtual address space.
- **Frame**: block tương ứng trong physical memory.

Page size phổ biến có thể là 4 KiB, ngoài ra có huge page lớn hơn.

Page nhỏ:

- Giảm internal fragmentation.
- Page table lớn hơn.
- Tăng số lần translation cần quản lý.

Page lớn:

- Giảm TLB miss cho vùng memory lớn.
- Tăng internal fragmentation.
- Chi phí page fault và compaction có thể lớn hơn.

---

## 4. Page table nhiều cấp

Một page table tuyến tính cho address space lớn sẽ rất tốn memory. Multi-level page table chỉ cấp các tầng cần dùng.

```text
Virtual address
-> level 1 index
-> level 2 index
-> level 3 index
-> page-table entry
-> physical frame
```

Đổi lại, page-table walk cần nhiều memory access nếu translation không có trong TLB.

---

## 5. TLB

Translation Lookaside Buffer là cache nhỏ, nhanh cho ánh xạ địa chỉ:

```text
virtual page -> physical frame
```

### TLB hit

MMU tìm thấy translation và tiếp tục truy cập memory nhanh.

### TLB miss

Phần cứng hoặc kernel phải page-table walk. Nếu mapping hợp lệ, translation được đưa vào TLB.

TLB miss không đồng nghĩa page fault:

- TLB miss: chưa có translation trong cache.
- Page fault: page-table state yêu cầu kernel xử lý.

---

## 6. Page fault

Page fault xảy ra khi CPU không thể hoàn thành truy cập theo mapping hiện tại.

Flow:

```text
Instruction truy cập virtual address
-> MMU phát hiện vấn đề
-> exception vào kernel
-> kernel kiểm tra địa chỉ và quyền
-> xử lý hoặc gửi lỗi cho process
```

### Minor page fault

Không cần đọc dữ liệu từ storage, ví dụ:

- Tạo anonymous page mới.
- Mapping đã có trong page cache.
- Copy-on-write cần copy page trong RAM.

### Major page fault

Cần I/O từ storage, thường chậm hơn nhiều.

### Invalid access

Nếu địa chỉ không hợp lệ hoặc vi phạm permission, process có thể nhận segmentation fault/access violation.

---

## 7. Demand paging

Kernel chỉ nạp page khi được truy cập:

```text
exec program
-> tạo mapping
-> chưa đọc toàn bộ executable
-> instruction chạm page
-> page fault
-> kernel nạp page cần thiết
```

Lợi ích:

- Khởi động nhanh hơn.
- Không dùng RAM cho code chưa chạm.
- Cho phép tổng virtual memory lớn hơn RAM.

---

## 8. Copy-on-write

Sau `fork()`, parent và child có thể chia sẻ physical page ở chế độ chỉ đọc logic:

```text
Parent page --+
              +-> physical frame X
Child page ---+
```

Khi child ghi:

```text
write fault
-> kernel cấp frame Y
-> copy dữ liệu
-> child ánh xạ Y
-> parent vẫn ánh xạ X
```

Copy-on-write giảm chi phí `fork()`, đặc biệt khi child nhanh chóng gọi `exec()`.

---

## 9. Anonymous memory và file-backed memory

### Anonymous memory

Không có file gốc trực tiếp:

- Heap.
- Stack.
- Anonymous `mmap`.

Khi cần đẩy khỏi RAM, dữ liệu dirty có thể phải vào swap.

### File-backed memory

Được hậu thuẫn bởi file:

- Executable code.
- Shared library.
- Memory-mapped file.
- Page cache.

Page sạch có thể bị loại khỏi RAM rồi đọc lại từ file khi cần.

---

## 10. Memory-mapped file

`mmap` ánh xạ nội dung file vào virtual address space:

```text
file offset
-> virtual page
-> page cache / physical frame
```

Ứng dụng truy cập bằng load/store thay vì gọi `read`/`write` cho từng đoạn.

Lợi ích:

- Demand paging.
- Chia sẻ page cache.
- Thuận tiện cho random access.

Rủi ro:

- Page fault xuất hiện tại thời điểm truy cập.
- File bị truncate có thể gây lỗi khi truy cập mapping.
- Durability vẫn cần hiểu `msync`, `fsync` và semantics hệ điều hành.

---

## 11. Shared memory

Hai process ánh xạ cùng physical page:

```text
Process A virtual page -> frame X
Process B virtual page -> frame X
```

Shared memory giảm copy nhưng không tự cung cấp synchronization. Hai process vẫn cần mutex, semaphore, atomic hoặc protocol phù hợp.

---

## 12. Page replacement

Khi cần frame, kernel chọn page để reclaim.

### FIFO

Loại page vào sớm nhất. Đơn giản nhưng không phản ánh mức sử dụng.

### LRU

Loại page lâu không dùng nhất. LRU chính xác tốn chi phí nên hệ điều hành thường dùng approximation.

### Clock/Second chance

Dựa trên reference bit:

```text
reference = 1 -> cho cơ hội, xóa bit
reference = 0 -> có thể chọn làm victim
```

### Dirty và clean page

- Clean file-backed page có thể bỏ nhanh.
- Dirty page cần ghi lại trước.
- Anonymous page cần swap nếu không thể bỏ.

---

## 13. Working set và thrashing

Working set là tập page task đang sử dụng tích cực trong một khoảng thời gian.

Nếu tổng working set vượt RAM:

```text
page bị đẩy ra
-> sớm cần lại
-> page fault và đọc vào
-> page khác bị đẩy ra
-> lặp liên tục
```

Đây là thrashing. Dấu hiệu:

- Major page fault cao.
- Disk I/O lớn.
- CPU utilization hữu ích thấp.
- Latency tăng mạnh.

Thêm concurrency có thể làm working set lớn hơn và tình hình xấu hơn.

---

## 14. Swap

Swap là vùng storage dùng để giữ page memory không nằm trong RAM.

Swap:

- Tạo khoảng đệm khi memory pressure.
- Cho phép giữ page ít dùng.
- Chậm hơn RAM rất nhiều.

Không nên hiểu:

```text
có swap = có thêm RAM với tốc độ tương đương
```

Swap liên tục là dấu hiệu cần xem lại workload, giới hạn memory hoặc memory leak.

---

## 15. Fragmentation

### Internal fragmentation

Memory lãng phí bên trong block đã cấp:

```text
page 4 KiB
chỉ dùng 3,2 KiB
-> phần còn lại không dùng cho allocation khác
```

### External fragmentation

Có đủ tổng memory trống nhưng bị chia thành nhiều vùng nhỏ, không có vùng liên tục đủ lớn.

Paging giảm external fragmentation ở mức cấp frame, nhưng kernel và allocator vẫn có các bài toán vùng liên tục cho một số mục đích.

---

## 16. Huge page

Huge page giảm:

- Số page-table entry.
- TLB pressure cho heap lớn.

Trade-off:

- Internal fragmentation.
- Chi phí allocation/compaction.
- Có thể gây latency bất ngờ.
- Không phải workload nào cũng hưởng lợi.

JVM và database có thể có tùy chọn liên quan huge page; cần benchmark trên môi trường thật.

---

## 17. Memory overcommit và OOM

Một số hệ thống cho phép tổng virtual allocation vượt RAM + swap dựa trên giả định không phải mọi process dùng toàn bộ memory đã request.

Khi memory thật sự cạn:

- Allocation thất bại.
- Kernel reclaim mạnh.
- Hệ thống thrash.
- OOM killer có thể chọn process để kết thúc trên Linux.

Trong container, cgroup memory limit có thể gây OOM trong container dù host còn memory.

---

## 18. JVM và OS memory

Memory của JVM không chỉ có Java heap:

```text
JVM process
├── Java heap
├── Metaspace
├── Code cache
├── Thread stack
├── Direct buffer
├── Native library
├── GC/native structure
└── Memory-mapped file
```

Vì vậy:

```text
-Xmx != toàn bộ memory process
```

Nhiều thread làm tăng native stack reservation/commit. Netty direct buffer và memory-mapped file cũng nằm ngoài Java heap.

---

## 19. Quan sát trên Linux

```bash
free -h
vmstat 1
cat /proc/meminfo
cat /proc/<PID>/status
cat /proc/<PID>/maps
pmap -x <PID>
```

Các khái niệm:

- **VSS/VSZ**: virtual address space.
- **RSS**: page của process đang resident trong RAM, cách tính có sắc thái với shared page.
- **PSS**: shared page được chia tỷ lệ giữa process.
- **Swap**: memory của process đang nằm trong swap.

Với Java:

```bash
jcmd <PID> GC.heap_info
jcmd <PID> VM.native_memory summary
```

Native Memory Tracking cần được bật phù hợp từ khi khởi động JVM và có overhead.

---

## 20. Câu hỏi tự kiểm tra

1. Page khác frame như thế nào?
2. TLB miss khác page fault ra sao?
3. Minor và major page fault khác nhau ở đâu?
4. Copy-on-write giúp `fork()` thế nào?
5. File-backed page và anonymous page được reclaim khác nhau ra sao?
6. Working set liên quan gì tới thrashing?
7. Huge page có lợi và hại gì?
8. Vì sao `-Xmx` không phải toàn bộ memory JVM?
9. Container có thể OOM khi host còn RAM vì sao?
10. RSS khác virtual memory như thế nào?
