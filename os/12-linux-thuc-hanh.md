# Thực hành Linux dành cho người học Operating System

Tài liệu này là bộ bài lab quan sát process, thread, CPU, memory, file descriptor, I/O và network trên Linux. Nên thực hiện trong máy ảo, WSL hoặc container thử nghiệm; không chạy lệnh gây tải trên production.

---

## 1. Chuẩn bị

Công cụ hữu ích:

```bash
ps
top
pstree
vmstat
free
pidstat
iostat
mpstat
strace
lsof
ss
ip
tcpdump
taskset
```

Trên Debian/Ubuntu:

```bash
sudo apt update
sudo apt install procps psmisc sysstat strace lsof iproute2 tcpdump
```

---

## 2. Lab 1: Process và cây cha–con

Mở hai terminal.

Terminal 1:

```bash
sleep 300
```

Terminal 2:

```bash
pgrep -a sleep
ps -o pid,ppid,stat,cmd -p <PID>
pstree -p <PID>
```

Quan sát:

- PID của `sleep`.
- PPID là shell đã tạo process.
- `STAT` thường thể hiện process đang ngủ/chờ.

Kết thúc:

```bash
kill <PID>
```

Câu hỏi:

- `sleep` có dùng CPU liên tục không?
- Sau `kill`, shell nhận exit status thế nào?

---

## 3. Lab 2: Process state

Chạy process CPU-bound:

```bash
yes > /dev/null
```

Terminal khác:

```bash
pgrep -a yes
ps -o pid,stat,psr,%cpu,cmd -p <PID>
top -p <PID>
```

So sánh với:

```bash
sleep 300
```

Kỳ vọng:

- `yes` thường runnable/running và dùng CPU cao.
- `sleep` chờ timer và gần như không dùng CPU.

Nhớ kết thúc `yes`:

```bash
kill <PID>
```

---

## 4. Lab 3: Thread trong process

Với Java application:

```bash
ps -T -p <PID>
top -H -p <PID>
jcmd <PID> Thread.print
```

Quan sát:

- Một JVM process có nhiều native thread.
- Thread GC, compiler, application và runtime có vai trò khác nhau.
- Một thread có CPU cao có thể được đối chiếu với Java thread dump.

Chuyển native thread ID decimal sang hexadecimal:

```bash
printf '%x\n' <TID>
```

Tìm `nid=0x...` tương ứng trong thread dump.

---

## 5. Lab 4: Context switch

Quan sát:

```bash
pidstat -w -p <PID> 1
grep -E 'voluntary|nonvoluntary' /proc/<PID>/status
```

So sánh:

- Process thường xuyên `sleep`.
- Process CPU-bound.
- Server đang xử lý nhiều request.

Giải thích:

- Voluntary: task tự block hoặc nhường.
- Nonvoluntary: task bị scheduler preempt.

Không kết luận chỉ từ số context switch; cần đặt cùng throughput và latency.

---

## 6. Lab 5: CPU theo từng core

```bash
mpstat -P ALL 1
```

Chạy một process single-thread CPU-bound:

```bash
yes > /dev/null
```

Quan sát một logical CPU có thể cao hơn các CPU khác. Scheduler có thể migrate task nên CPU nóng có thể thay đổi.

Xem CPU gần nhất:

```bash
ps -o pid,psr,%cpu,cmd -p <PID>
```

---

## 7. Lab 6: CPU affinity

Chạy task chỉ trên CPU 0:

```bash
taskset -c 0 yes > /dev/null
```

Xem affinity:

```bash
taskset -cp <PID>
```

Quan sát bằng:

```bash
mpstat -P ALL 1
```

Sau lab:

```bash
kill <PID>
```

Bài học: affinity kiểm soát nơi task có thể chạy nhưng có thể làm mất cân bằng tải.

---

## 8. Lab 7: Load average và run queue

```bash
uptime
cat /proc/loadavg
vmstat 1
```

Tạo vài task CPU-bound có kiểm soát:

```bash
yes > /dev/null
```

Chạy từng process một và quan sát:

- Load average thay đổi chậm theo cửa sổ thời gian.
- Cột `r` trong `vmstat`.
- Số logical CPU từ `nproc`.

Kết thúc toàn bộ process thử nghiệm ngay sau khi quan sát.

---

## 9. Lab 8: Virtual memory map

```bash
cat /proc/<PID>/maps
pmap -x <PID>
cat /proc/<PID>/status
```

Tìm:

- Executable mapping.
- Shared library.
- Heap.
- Stack.
- Anonymous mapping.

So sánh:

- `VmSize`.
- `VmRSS`.
- `VmSwap`.

Virtual size lớn không mặc nhiên nghĩa process đang chiếm từng đó RAM.

---

## 10. Lab 9: System memory

```bash
free -h
cat /proc/meminfo
vmstat 1
```

Quan sát:

- `MemAvailable`.
- Cache.
- Swap.
- Page-in/page-out.

Không dùng cột “used” đơn lẻ để kết luận thiếu RAM; Linux dùng RAM trống làm cache.

---

## 11. Lab 10: Minor và major page fault

```bash
pidstat -r -p <PID> 1
```

Hoặc:

```bash
ps -o pid,min_flt,maj_flt,cmd -p <PID>
```

Quan sát lúc:

- Application mới khởi động.
- Đọc file lần đầu.
- Đọc lại file có thể còn trong page cache.

Major fault phụ thuộc page cache và môi trường nên kết quả có thể khác giữa các lần chạy.

---

## 12. Lab 11: File descriptor

Mở một process server hoặc Java application:

```bash
ls -l /proc/<PID>/fd
lsof -p <PID>
```

Đếm descriptor:

```bash
ls /proc/<PID>/fd | wc -l
```

Xem giới hạn:

```bash
cat /proc/<PID>/limits
ulimit -n
```

Tìm:

- Standard input/output/error.
- File log.
- Socket.
- Pipe.

---

## 13. Lab 12: Theo dõi system call

Theo dõi một lệnh đơn giản:

```bash
strace -o trace.txt ls
```

Lọc file operation:

```bash
strace -e trace=openat,read,write,close ls
```

Đính vào process:

```bash
strace -p <PID>
```

Lưu ý:

- `strace` có overhead.
- Output có thể chứa path và dữ liệu nhạy cảm.
- Không attach production process tùy tiện.

---

## 14. Lab 13: Page cache khi đọc file

Tạo file thử nghiệm trong môi trường lab:

```bash
dd if=/dev/zero of=test-data.bin bs=1M count=256
```

Đọc và đo:

```bash
time cat test-data.bin > /dev/null
time cat test-data.bin > /dev/null
```

Lần sau có thể nhanh hơn nhờ page cache. Kết quả phụ thuộc storage, RAM và trạng thái hệ thống.

Xóa file sau lab:

```bash
rm test-data.bin
```

---

## 15. Lab 14: Disk I/O

```bash
iostat -xz 1
pidstat -d -p <PID> 1
```

Quan sát:

- Throughput đọc/ghi.
- Request latency.
- Queue.
- Utilization.

Không dùng `%util` một mình để kết luận với mọi loại storage hiện đại; cần xem latency và throughput.

---

## 16. Lab 15: Socket đang mở

```bash
ss -lntp
ss -ntp
```

Các trạng thái TCP:

- `LISTEN`
- `ESTAB`
- `SYN-SENT`
- `SYN-RECV`
- `TIME-WAIT`
- `CLOSE-WAIT`

Tìm port của Spring Boot:

```bash
ss -lntp | grep 8080
```

---

## 17. Lab 16: Localhost

Terminal 1:

```bash
python3 -m http.server 8080
```

Terminal 2:

```bash
curl http://127.0.0.1:8080
ss -ntp | grep 8080
```

Quan sát:

- Connection dùng loopback.
- Không cần NIC vật lý để dữ liệu rời máy.
- Vẫn có socket và TCP state.

---

## 18. Lab 17: DNS và route

```bash
ip addr
ip route
getent hosts example.com
ip route get 1.1.1.1
```

Phân biệt:

- DNS đổi hostname thành IP.
- Routing chọn interface và next hop.
- TCP/UDP dùng port để xác định endpoint ứng dụng.

---

## 19. Lab 18: Packet capture

Loopback:

```bash
sudo tcpdump -i lo -nn port 8080
```

Sau đó gọi:

```bash
curl http://127.0.0.1:8080
```

Chỉ capture traffic bạn được phép quan sát. Packet có thể chứa credential hoặc dữ liệu nhạy cảm.

---

## 20. Lab 19: Deleted file vẫn chiếm dung lượng

Trong terminal 1:

```bash
python3 -c "f=open('held.log','w'); f.write('x'*1000000); f.flush(); input()"
```

Terminal 2:

```bash
rm held.log
lsof +L1
```

File không còn tên trong directory nhưng process vẫn giữ descriptor. Nhấn Enter ở terminal 1 để đóng file, sau đó kiểm tra lại.

---

## 21. Lab 20: Container resource

Nếu có Docker:

```bash
docker run --rm --cpus=1 --memory=256m alpine sh -c 'cat /proc/meminfo; nproc'
```

Quan sát thêm từ host:

```bash
docker stats
```

Tùy runtime và phiên bản, `nproc` và `/proc/meminfo` có thể phản ánh namespace/cgroup theo cách khác nhau. Cần đọc CPU quota và memory limit để hiểu tài nguyên thật.

---

## 22. Checklist điều tra Java service chậm

1. Kiểm tra CPU, load và run queue.
2. Kiểm tra memory, swap và page fault.
3. Xem số thread và thread dump.
4. Xem context switch.
5. Kiểm tra file descriptor.
6. Kiểm tra socket state.
7. Kiểm tra disk latency.
8. Kiểm tra container throttling/OOM.
9. Đối chiếu với application metric và trace.

Không tối ưu OS metric tách rời nghiệp vụ. CPU cao có thể là symptom, không phải root cause.

---

## 23. Câu hỏi tự kiểm tra

1. Làm sao phân biệt process CPU-bound và đang sleep?
2. Làm sao xem thread nào trong JVM dùng CPU?
3. `VmSize` và `VmRSS` khác nhau thế nào?
4. Làm sao xem process đang mở file/socket nào?
5. `strace` giúp quan sát ranh giới nào?
6. Vì sao lần đọc file thứ hai có thể nhanh hơn?
7. `TIME-WAIT` và `CLOSE-WAIT` gợi ý điều gì?
8. Vì sao deleted file vẫn chiếm dung lượng?
9. CPU host còn rảnh nhưng container vẫn chậm có thể do đâu?
10. Vì sao phải kết hợp OS metric với application trace?
