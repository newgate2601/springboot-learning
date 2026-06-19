# File System chuyên sâu

Tài liệu này giải thích cách file system ánh xạ tên file thành dữ liệu trên storage, quản lý metadata, cache, durability và phục hồi sau sự cố.

---

## 1. File system cung cấp gì?

File system biến storage dạng block thành abstraction:

- File.
- Directory.
- Path.
- Metadata.
- Permission.
- Link.
- Mount point.

```text
/var/log/app.log
-> tra từng directory
-> tìm metadata file
-> tìm data block
-> đọc nội dung
```

---

## 2. VFS

Virtual File System là lớp abstraction trong kernel cung cấp API chung cho nhiều file system:

```text
Application
-> open/read/write
-> VFS
-> ext4 / XFS / tmpfs / NFS / ...
-> block layer hoặc network
```

Nhờ VFS, application dùng cùng system call dù implementation phía dưới khác nhau.

---

## 3. Inode và directory entry

Trong file system kiểu Unix:

- **Inode** lưu metadata và tham chiếu tới data block.
- **Directory entry** ánh xạ tên sang inode.

```text
Directory entry: "orders.log" -> inode 8241

Inode 8241:
- owner
- permission
- size
- timestamp
- data-block mapping
```

Tên file thường nằm trong directory entry, không phải chính inode.

---

## 4. Path resolution

Với:

```text
/var/log/app/orders.log
```

Kernel lần lượt tra:

```text
/
-> var
-> log
-> app
-> orders.log
```

Permission trên directory ảnh hưởng khả năng đi qua và liệt kê. Biết path không có nghĩa chắc chắn có quyền mở file.

Path resolution có thể dùng dentry cache để tránh đọc storage mỗi lần.

---

## 5. Hard link và symbolic link

### Hard link

Nhiều tên trỏ cùng inode:

```text
name-a --+
         +-> inode 100 -> data
name-b --+
```

Xóa một tên chỉ giảm link count. Dữ liệu được giải phóng khi không còn hard link và không còn reference mở phù hợp.

### Symbolic link

Symlink là file đặc biệt chứa path tới target:

```text
current.log -> app-2026-06-19.log
```

Target có thể không tồn tại, tạo dangling symlink.

---

## 6. Open file và file descriptor

```text
fd của process
-> open-file description
-> inode/file object
```

Open-file state có thể chứa:

- Current offset.
- Open flags.
- Reference count.

Sau `fork()`, parent và child có thể chia sẻ underlying open-file description nên thay đổi offset có thể ảnh hưởng nhau.

File descriptor chỉ có ý nghĩa trong process descriptor table.

---

## 7. File allocation

File system phải ánh xạ logical offset sang block vật lý.

Các ý tưởng:

- Contiguous allocation.
- Linked allocation.
- Indexed allocation.
- Extent.

Extent mô tả một dải block liên tục:

```text
logical blocks 0..127
-> physical blocks 9000..9127
```

Extent giảm metadata cho file lớn so với lưu mapping từng block.

---

## 8. Free-space management

File system theo dõi block trống bằng:

- Bitmap.
- Free list.
- Tree hoặc cấu trúc extent.

Allocation cần cân bằng:

- Tốc độ.
- Giảm fragmentation.
- Locality.
- Khả năng mở rộng.

---

## 9. Page cache

Kernel dùng RAM cache nội dung file:

```text
read()
-> tìm page cache
-> hit: trả dữ liệu từ RAM
-> miss: đọc storage rồi cache
```

Khi ghi buffered I/O:

```text
write()
-> copy vào page cache
-> page trở thành dirty
-> kernel ghi xuống storage sau
```

`write()` thành công không mặc nhiên nghĩa dữ liệu đã bền vững trên SSD.

---

## 10. Buffered I/O và direct I/O

### Buffered I/O

Đi qua page cache:

- Dễ dùng.
- Tận dụng cache và readahead.
- Có thể copy dữ liệu qua nhiều buffer.

### Direct I/O

Cố gắng bỏ qua page cache cho data path:

- Database có thể tự quản lý cache.
- Thường có yêu cầu alignment.
- Semantics phụ thuộc OS và file system.
- Metadata vẫn có thể được cache.

Direct I/O không tự động nhanh hơn; workload tuần tự nhỏ có thể hưởng lợi mạnh từ page cache.

---

## 11. Readahead và writeback

### Readahead

Kernel dự đoán đọc tuần tự và nạp block tiếp theo trước khi application yêu cầu.

### Writeback

Dirty page được ghi xuống storage:

- Theo chu kỳ.
- Khi dirty memory đạt ngưỡng.
- Khi application yêu cầu sync.
- Khi memory pressure.

Writeback burst có thể gây latency spike nếu application tạo dirty page nhanh.

---

## 12. `fsync` và durability

Flow ghi có thể gồm:

```text
Application buffer
-> page cache
-> block layer
-> device cache
-> non-volatile media
```

`fsync(fd)` yêu cầu đồng bộ dữ liệu và metadata liên quan theo semantics của hệ thống. Tuy nhiên durability cuối cùng còn phụ thuộc:

- File system.
- Mount option.
- Storage controller.
- Device cache và flush support.
- Phần cứng có tuân thủ lệnh flush không.

Khi tạo file rồi rename để cập nhật atomic, đôi khi còn cần sync directory để bảo đảm directory entry bền vững sau crash.

---

## 13. Atomicity của file operation

Một số operation như rename trong cùng file system thường có tính atomic ở namespace:

```text
reader thấy tên cũ hoặc tên mới
không thấy trạng thái nửa rename
```

Mẫu cập nhật:

```text
ghi file tạm
-> fsync file tạm
-> rename file tạm thành file chính
-> fsync directory khi cần durability chặt
```

Atomic visibility không đồng nghĩa toàn bộ dữ liệu đã bền vững sau mất điện.

---

## 14. Journaling

Journaling ghi trước thông tin thay đổi để file system có thể phục hồi consistency.

Flow khái quát:

```text
ghi transaction vào journal
-> commit journal
-> áp dụng thay đổi vào cấu trúc chính
-> checkpoint
```

Các chế độ có thể ưu tiên metadata hoặc cả data, tùy file system và cấu hình.

Journaling chủ yếu bảo vệ cấu trúc file system; nó không tự thay thế transaction logic của database.

---

## 15. Crash consistency

Crash có thể xảy ra giữa bất kỳ hai bước ghi:

```text
1. ghi data
2. cập nhật size
3. cập nhật directory
4. cập nhật free-space metadata
```

File system cần bảo đảm sau recovery không rơi vào trạng thái cấu trúc nguy hiểm. Ứng dụng cũng cần protocol ghi chịu crash nếu có nhiều file hoặc nhiều bước.

---

## 16. Database và WAL

Database dùng Write-Ahead Logging:

```text
ghi log mô tả thay đổi
-> flush log
-> xác nhận commit
-> data page có thể ghi sau
```

Sau crash, database dùng log để redo/undo theo thiết kế.

WAL cần hiểu rõ ordering và flush; chỉ gọi `write()` chưa đủ cho commit bền vững.

---

## 17. Mount

Mount gắn một file system vào namespace:

```text
device/file system
-> mount tại /data
-> file xuất hiện dưới /data
```

Mount point có thể che nội dung directory cũ trong lúc đang mount.

Container dùng mount namespace để có góc nhìn file system khác host hoặc container khác.

---

## 18. Permission và ACL

Permission Unix cơ bản:

```text
rwx cho owner
rwx cho group
rwx cho others
```

Directory:

- `r`: liệt kê tên.
- `w`: tạo/xóa entry khi điều kiện cho phép.
- `x`: traverse directory.

ACL cung cấp rule chi tiết hơn cho nhiều user/group.

Quyền file không phải toàn bộ bảo mật; còn có capability, mandatory access control, mount option và namespace.

---

## 19. File deletion khi vẫn đang mở

Trên Unix:

```text
process mở file
-> file bị unlink khỏi directory
-> process vẫn dùng fd
-> data chỉ giải phóng khi reference cuối đóng
```

Điều này giải thích trường hợp:

- `df` báo disk vẫn đầy.
- `du` không tìm thấy file lớn.
- Một process vẫn giữ deleted file mở.

Kiểm tra:

```bash
lsof +L1
```

---

## 20. Quan sát trên Linux

```bash
df -h
df -i
du -sh <path>
stat <file>
ls -li <file>
findmnt
mount
lsof <file>
lsof -p <PID>
```

I/O:

```bash
iostat -xz 1
pidstat -d -p <PID> 1
```

System call:

```bash
strace -e trace=openat,read,write,fsync,close <command>
```

---

## 21. Câu hỏi tự kiểm tra

1. Inode khác directory entry thế nào?
2. Hard link khác symlink ra sao?
3. File descriptor liên hệ với open-file state thế nào?
4. Page cache tham gia `read` và `write` ra sao?
5. Vì sao `write()` thành công chưa bảo đảm durability?
6. Atomic rename khác durable rename thế nào?
7. Journaling bảo vệ gì?
8. WAL của database có mục đích gì?
9. Vì sao file đã xóa vẫn có thể chiếm disk?
10. Direct I/O có luôn nhanh hơn buffered I/O không?
