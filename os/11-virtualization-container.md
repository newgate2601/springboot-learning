# Virtualization và Container chuyên sâu

Tài liệu này giải thích máy ảo, hypervisor, Linux namespace, cgroup, container filesystem và network; đồng thời làm rõ container không phải máy ảo thu nhỏ.

---

## 1. Mục tiêu của virtualization

Virtualization tạo abstraction và isolation cho tài nguyên:

- CPU.
- Memory.
- Storage.
- Network.
- Device.

Nhiều workload có thể chia sẻ một máy vật lý mà vẫn có môi trường tương đối độc lập.

---

## 2. Virtual machine

VM nhìn thấy virtual hardware:

```text
Application
-> Guest OS
-> Guest kernel
-> Virtual hardware
-> Hypervisor
-> Physical hardware
```

Mỗi VM thường có:

- Guest kernel riêng.
- Virtual CPU.
- Guest physical memory.
- Virtual disk.
- Virtual NIC.

VM có thể chạy OS khác host nếu hypervisor và phần cứng hỗ trợ.

---

## 3. Hypervisor type 1 và type 2

### Type 1

Hypervisor chạy trực tiếp trên phần cứng hoặc là nền tảng hệ thống chính:

```text
Hardware
-> Hypervisor
-> VM
```

Thường dùng trong datacenter.

### Type 2

Hypervisor chạy trên host OS:

```text
Hardware
-> Host OS
-> Hypervisor application
-> VM
```

Phân loại thực tế có sắc thái vì một số nền tảng kết hợp driver, kernel module và host management layer.

---

## 4. CPU virtualization

Guest OS tin rằng nó quản lý CPU, nhưng hypervisor lập lịch virtual CPU lên physical CPU:

```text
Guest thread
-> guest scheduler chọn vCPU
-> hypervisor scheduler chọn physical CPU
```

Có hai tầng scheduling:

- Guest scheduler.
- Hypervisor/host scheduler.

Nếu VM có nhiều vCPU nhưng host quá tải, guest có thể thấy hiệu năng không ổn định hoặc steal time.

---

## 5. Memory virtualization

Có nhiều lớp địa chỉ:

```text
Guest virtual address
-> guest physical address
-> host physical address
```

Phần cứng hiện đại hỗ trợ nested page table để giảm overhead translation.

Hypervisor có thể dùng:

- Overcommit.
- Ballooning.
- Page sharing ở một số hệ thống.
- Swap phía host.

Nếu cả guest và host cùng reclaim/swap, latency có thể tăng rất mạnh.

---

## 6. Device virtualization

VM có thể dùng:

- Emulated device.
- Paravirtualized device.
- Device passthrough.

### Emulation

Tương thích rộng nhưng có overhead.

### Paravirtualization

Guest dùng driver biết môi trường ảo hóa, thường hiệu quả hơn.

### Passthrough

Gán thiết bị hoặc virtual function gần trực tiếp cho VM, tăng hiệu năng nhưng giảm linh hoạt migration/chia sẻ.

---

## 7. Container là gì?

Container thường là một hoặc nhiều process được:

- Cô lập góc nhìn bằng namespace.
- Giới hạn/account tài nguyên bằng cgroup.
- Cung cấp root filesystem riêng.
- Áp dụng capability và security policy.

```text
Container process
-> system call
-> host kernel
-> hardware
```

Container Linux chia sẻ kernel Linux của host.

---

## 8. Container khác VM

| Container | Virtual machine |
|---|---|
| Chia sẻ host kernel | Có guest kernel riêng |
| Khởi động process nhanh | Boot guest OS |
| Image thường nhỏ hơn | Disk image thường lớn hơn |
| Isolation dựa nhiều vào kernel | Ranh giới virtual hardware |
| Không tự chạy kernel tùy ý khác host | Có thể chạy guest OS khác |

Container không mặc nhiên kém an toàn trong mọi tình huống, nhưng ranh giới bảo mật và threat model khác VM.

---

## 9. PID namespace

PID namespace cho process góc nhìn PID riêng:

```text
Host thấy container process: PID 31500
Trong container thấy: PID 1
```

PID 1 trong container có trách nhiệm đặc biệt:

- Nhận signal đúng cách.
- Thu nhận orphan/zombie child.

Application không xử lý PID 1 semantics tốt có thể gây shutdown chậm hoặc zombie.

---

## 10. Mount namespace

Mỗi container có góc nhìn mount riêng:

```text
Host /
Container A /
Container B /
```

Bind mount và volume đưa dữ liệu host hoặc managed storage vào container.

Mount namespace không tự mã hóa hoặc bảo vệ dữ liệu nếu host administrator có quyền truy cập.

---

## 11. Network namespace

Network namespace cô lập:

- Interface.
- IP address.
- Routing table.
- Port.
- Firewall state.

Container có thể có virtual Ethernet pair:

```text
Container eth0
-> veth pair
-> host bridge
-> host NIC
-> network
```

Hai container có thể cùng listen port `8080` nếu ở network namespace khác nhau.

---

## 12. User namespace

User namespace ánh xạ UID/GID:

```text
UID 0 trong namespace
-> UID không đặc quyền tương ứng trên host
```

Nó hỗ trợ rootless container và giảm quyền trên host, nhưng cần cấu hình filesystem, capability và runtime phù hợp.

---

## 13. Các namespace khác

- **UTS namespace**: hostname/domain name.
- **IPC namespace**: shared memory, semaphore và message queue System V/POSIX liên quan.
- **Cgroup namespace**: góc nhìn hierarchy cgroup.
- **Time namespace**: một số clock offset.

Namespace tạo isolation về góc nhìn; không trực tiếp giới hạn lượng CPU/RAM.

---

## 14. Cgroup

Control group dùng để:

- Account tài nguyên.
- Giới hạn tài nguyên.
- Phân nhóm process.
- Áp dụng policy.

Các tài nguyên:

- CPU.
- Memory.
- I/O.
- PID.

Cgroup v2 cung cấp hierarchy thống nhất hơn so với mô hình controller tách rời của v1.

---

## 15. CPU limit và throttling

CPU quota giới hạn CPU time trong chu kỳ:

```text
quota = 100 ms mỗi period 100 ms
-> tương đương tối đa khoảng 1 CPU
```

Process có thể dùng nhiều core trong thời gian ngắn rồi bị throttle khi hết quota.

Dấu hiệu:

- Host CPU chưa 100%.
- Container latency tăng.
- Cgroup throttled time/count tăng.

CPU request trong orchestrator phục vụ scheduling/share; CPU limit thường liên quan quota. Semantics chính xác phụ thuộc nền tảng.

---

## 16. Memory limit và OOM

Cgroup memory limit kiểm soát memory process trong group.

Khi chạm giới hạn:

- Kernel reclaim.
- Có thể dùng swap nếu được cấu hình.
- Allocation chậm hoặc thất bại.
- Cgroup OOM có thể kill process.

JVM cần chừa headroom ngoài heap:

```text
container memory limit
> Java heap
+ metaspace
+ thread stack
+ direct memory
+ code cache
+ native overhead
```

Đặt `-Xmx` bằng đúng memory limit dễ gây OOMKill.

---

## 17. PID limit

PID controller giới hạn số process/thread:

```text
pids.max
```

Vì Linux task model tính cả thread trong nhiều ngữ cảnh cgroup, ứng dụng tạo quá nhiều thread có thể chạm PID limit dù số process ít.

---

## 18. I/O control

Cgroup có thể điều chỉnh:

- I/O weight.
- Bandwidth.
- IOPS.

Hiệu lực phụ thuộc:

- Storage stack.
- Device.
- Scheduler.
- Cgroup version.
- Kiểu filesystem.

Container chậm disk có thể do noisy neighbor hoặc host storage, không chỉ code bên trong container.

---

## 19. Container image và layer

Image gồm các layer bất biến:

```text
base image
-> runtime layer
-> dependency layer
-> application layer
```

Khi container chạy, runtime thêm writable layer.

Lợi ích:

- Chia sẻ layer.
- Cache build.
- Phân phối theo content digest.

Không nên lưu dữ liệu bền vững quan trọng chỉ trong writable layer của container.

---

## 20. Overlay filesystem

Overlay filesystem hợp nhất:

- Lower layers chỉ đọc.
- Upper writable layer.
- Merged view.

Khi sửa file từ lower layer, có thể xảy ra copy-up sang upper layer.

Trade-off:

- Thuận tiện cho image.
- Metadata/copy-up có overhead.
- Database thường nên dùng volume phù hợp thay vì writable layer.

---

## 21. Volume và bind mount

### Volume

Runtime quản lý vị trí lưu dữ liệu.

### Bind mount

Gắn path cụ thể của host vào container.

Phải cân nhắc:

- Permission UID/GID.
- SELinux/AppArmor label khi có.
- Backup.
- Durability.
- Portability.

---

## 22. Container networking

Một flow phổ biến:

```text
Application trong container
-> socket
-> container network namespace
-> veth
-> bridge/routing trên host
-> NAT/firewall nếu có
-> NIC
```

Giao tiếp cùng host vẫn có thể đi qua nhiều lớp virtual network, namespace và firewall; không đồng nghĩa loopback đơn giản.

---

## 23. Port publishing

Container listen:

```text
0.0.0.0:8080 trong namespace container
```

Host publish:

```text
host:8080 -> container-ip:8080
```

Port mapping có thể được thực hiện bằng NAT, proxy hoặc cơ chế runtime khác.

`localhost` bên trong container thường trỏ chính container đó, không phải host.

---

## 24. Capability

Linux chia quyền root thành capability nhỏ hơn:

- Bind port đặc quyền.
- Thay đổi network.
- Gửi một số signal.
- Quản trị hệ thống.

Container nên:

- Drop capability không cần.
- Tránh privileged mode.
- Chạy non-root.
- Dùng read-only root filesystem khi phù hợp.

---

## 25. Seccomp, AppArmor và SELinux

- **Seccomp**: giới hạn system call.
- **AppArmor/SELinux**: policy truy cập bắt buộc.
- **Capability**: chia nhỏ đặc quyền.
- **Namespace**: cô lập góc nhìn.
- **Cgroup**: giới hạn/account tài nguyên.

Bảo mật container là sự kết hợp nhiều lớp, không phải một cơ chế duy nhất.

---

## 26. Container escape

Vì container chia sẻ kernel, lỗ hổng kernel/runtime hoặc cấu hình quá quyền có thể cho process vượt ranh giới.

Giảm rủi ro:

- Cập nhật kernel và runtime.
- Không dùng privileged nếu không cần.
- Không mount Docker socket tùy tiện.
- Drop capability.
- Chạy non-root/rootless.
- Giới hạn system call.
- Không mount path nhạy cảm của host.

Mount Docker socket gần như trao quyền điều khiển runtime và có thể tương đương quyền rất cao trên host.

---

## 27. Container và Java

Các điểm cần theo dõi:

- JVM nhận diện CPU quota.
- Heap sizing theo memory limit.
- Native memory ngoài heap.
- Thread count và PID limit.
- GC behavior khi CPU quota thấp.
- DNS/network namespace.
- Graceful shutdown khi nhận `SIGTERM`.

Spring Boot nên:

- Xử lý shutdown trong grace period.
- Không chạy bằng root nếu không cần.
- Ghi log ra stdout/stderr hoặc volume theo kiến trúc.
- Có health/readiness signal phù hợp.

---

## 28. Quan sát container

Docker:

```bash
docker ps
docker inspect <container>
docker stats
docker top <container>
docker exec <container> sh
```

Linux namespace:

```bash
lsns
nsenter
```

Cgroup v2 thường nằm dưới:

```text
/sys/fs/cgroup
```

Không chỉnh trực tiếp cgroup production nếu workload được orchestrator quản lý; thay đổi nên đi qua cấu hình nền tảng.

---

## 29. Khi nào chọn VM, container hoặc cả hai?

### VM phù hợp khi

- Cần guest kernel/OS riêng.
- Cần ranh giới isolation mạnh theo threat model.
- Chạy workload legacy.
- Cần virtual hardware cụ thể.

### Container phù hợp khi

- Đóng gói process nhất quán.
- Khởi động nhanh.
- Mật độ workload cao.
- CI/CD và orchestration.

### Kết hợp

Phổ biến:

```text
Physical host
-> VM
-> container runtime
-> nhiều container
```

VM tạo ranh giới hạ tầng; container tạo đơn vị đóng gói và triển khai.

---

## 30. Câu hỏi tự kiểm tra

1. VM và container khác nhau ở kernel thế nào?
2. Vì sao VM có hai tầng CPU scheduling?
3. Namespace khác cgroup ở chức năng nào?
4. PID 1 trong container có trách nhiệm gì?
5. CPU throttling có thể xảy ra khi host chưa đầy CPU vì sao?
6. Vì sao `-Xmx` không nên bằng container memory limit?
7. Overlay filesystem dùng lower và upper layer ra sao?
8. `localhost` trong container trỏ tới đâu?
9. Capability và seccomp giải quyết hai vấn đề gì?
10. Khi nào nên chạy container bên trong VM?
