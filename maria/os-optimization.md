# MariaDB - Tối ưu hệ điều hành

## 1. Tối ưu hệ điều hành là gì?

Tối ưu hệ điều hành cho MariaDB là nhóm thực hành nhằm làm cho OS hỗ trợ database tốt hơn về:

- filesystem;
- disk I/O;
- page cache;
- durability;
- network filesystem;
- write buffering;
- flush/persist xuống storage;
- tương tác giữa InnoDB buffer pool và OS cache.

Điểm quan trọng: hệ điều hành không phải nơi duy nhất quyết định performance. Với MariaDB, các yếu tố thường có ảnh hưởng lớn hơn gồm:

- RAM khả dụng;
- tốc độ storage;
- cấu hình InnoDB buffer pool;
- workload OLTP/OLAP;
- index/query plan;
- replication/binlog/durability setting;
- số connection và concurrency.

Tuy nhiên, OS vẫn có thể tạo khác biệt lớn trong các hệ thống I/O-bound hoặc durability-sensitive.

Tư duy tổng quát:

```text
Database muốn:
đọc nhanh
ghi nhanh
ghi an toàn
ít double buffering
ít metadata write không cần thiết
filesystem/storage ổn định
```

Tối ưu OS là tìm điểm cân bằng giữa:

```text
performance
durability
reliability
operational simplicity
```

## 2. Tối ưu filesystem

### 2.1. Filesystem có quan trọng không?

Filesystem không phải yếu tố quan trọng nhất của MariaDB performance, nhưng vẫn có thể ảnh hưởng đáng kể trong một số case.

Thứ tự ưu tiên thường là:

```text
RAM / buffer pool
-> storage speed
-> query/index
-> OS/filesystem setting
```

Filesystem tốt không cứu được query plan tệ hoặc storage quá chậm, nhưng filesystem không phù hợp có thể làm hệ thống chậm, khó vận hành hoặc rủi ro hơn.

Các filesystem Linux phổ biến phù hợp với MariaDB:

| Filesystem | Điểm mạnh | Lưu ý |
| --- | --- | --- |
| `ext4` | Ổn định, phổ biến, dễ vận hành, phù hợp đa số workload | Ít feature nâng cao hơn XFS/Btrfs, nhưng độ đơn giản là lợi thế. |
| `XFS` | Tốt cho file lớn, I/O song song, workload database lớn | Không shrink filesystem dễ như ext4; cần hiểu operational tooling. |
| `Btrfs` | Có snapshot, checksum, compression, copy-on-write | Copy-on-write có thể gây overhead với database write-heavy nếu không kiểm soát tốt. |

Giới hạn lý thuyết thường được nhắc đến:

| Giới hạn | ext4 | XFS | Btrfs |
| --- | --- | --- | --- |
| Max file size | khoảng `16-256 TiB` | khoảng `8 EiB` | khoảng `16 EiB` |
| Max filesystem size | khoảng `1 EiB` | khoảng `8 EiB` | khoảng `16 EiB` |

Trong thực tế, giới hạn lý thuyết hiếm khi là vấn đề đầu tiên. Với database, điều quan trọng hơn là:

- latency;
- IOPS;
- throughput;
- fsync behavior;
- fragmentation;
- ổn định khi crash/power loss;
- tooling backup/restore;
- khả năng quan sát và vận hành.

### 2.2. ext4, XFS, Btrfs nên hiểu thế nào?

`ext4` thường là lựa chọn an toàn cho hệ thống nhỏ đến trung bình:

- phổ biến;
- dễ debug;
- nhiều người vận hành quen;
- phù hợp khi không cần feature đặc biệt.

`XFS` thường phù hợp với hệ thống lớn hơn:

- file lớn;
- parallel I/O;
- workload ghi/đọc lớn;
- database data directory lớn.

`Btrfs` hấp dẫn vì snapshot/checksum/compression, nhưng với database cần cẩn thận:

- copy-on-write có thể làm write amplification;
- snapshot quá nhiều có thể ảnh hưởng performance;
- database vốn đã có cơ chế consistency riêng;
- cần hiểu rõ cách tắt/giảm CoW nếu workload write-heavy.

Kết luận thực dụng:

```text
Muốn đơn giản, ổn định: ext4.
Muốn filesystem mạnh cho file lớn/I/O lớn: XFS.
Muốn snapshot/checksum/compression: Btrfs, nhưng phải hiểu trade-off với database writes.
```

## 3. Tắt cập nhật access time

### 3.1. Access time là gì?

Access time hay `atime` là timestamp ghi nhận lần cuối file được đọc.

Với database server, việc ghi nhận mỗi lần đọc file thường không có nhiều giá trị. MariaDB không cần biết mỗi data file được OS đọc lần cuối khi nào để xử lý transaction.

Nếu filesystem cập nhật access time quá thường xuyên, mỗi lần đọc có thể kéo theo metadata write:

```text
read data file
-> update atime metadata
-> phát sinh write không phục vụ dữ liệu database
```

Điều này không tốt cho database vì:

- tăng write I/O không cần thiết;
- tăng metadata update;
- làm storage bận hơn;
- có thể ảnh hưởng latency.

### 3.2. `noatime` và `relatime`

Các mode thường gặp:

| Mode | Ý nghĩa |
| --- | --- |
| `atime` | Cập nhật access time thường xuyên. Chính xác hơn nhưng tốn write metadata. |
| `relatime` | Chỉ cập nhật access time trong một số điều kiện. Thường là default trên nhiều Linux distribution hiện đại. |
| `noatime` | Không cập nhật access time khi đọc file. Giảm metadata write. |

Với data directory của MariaDB, `noatime` thường hợp lý vì database không cần access time của data file.

Lưu ý:

- log file hoặc file hệ thống khác có thể vẫn cần access time trong một số môi trường;
- nếu cần giữ access time cho log/system file, có thể tách data directory và log/system path ra filesystem khác;
- không nên áp dụng máy móc toàn hệ thống nếu chưa hiểu ứng dụng khác có phụ thuộc access time hay không.

Kết luận:

```text
Database data files thường không cần atime.
Giảm atime update giúp giảm metadata writes.
```

## 4. NFS và MariaDB

### 4.1. Vì sao thường không khuyến nghị NFS?

NFS là Network File System. Về mặt tiện lợi, NFS giúp nhiều server truy cập storage qua network. Nhưng với MariaDB data directory, NFS thường không được khuyến nghị.

Các rủi ro chính:

- locking không ổn định hoặc behavior khác local filesystem;
- network lỗi có thể làm I/O treo hoặc mất thứ tự;
- packet/network issue có thể gây inconsistency;
- latency biến động;
- recovery sau outage phức tạp;
- chia sẻ cùng data directory giữa nhiều MariaDB instance là rủi ro rất cao.

Tư duy quan trọng:

```text
Database không chỉ cần lưu file.
Database cần filesystem có semantics ổn định cho lock, flush, ordering và crash recovery.
```

NFS có thể không đảm bảo các behavior này theo cách database mong đợi trong mọi tình huống.

### 4.2. NFS trong SAN/professional storage

NFS trong môi trường SAN hoặc storage appliance chuyên nghiệp có thể đáng tin cậy hơn NFS thông thường, nhưng vẫn cần thận trọng.

Ngay cả khi reliability tốt hơn, performance có thể vẫn kém hơn:

- direct-attached SSD/NVMe;
- local block storage;
- storage bus-attached non-rotational storage.

Nếu bắt buộc dùng NFS:

- không chia sẻ cùng data directory cho nhiều MariaDB instance;
- cần hiểu locking behavior;
- cần kiểm tra crash recovery;
- cần test power/network failure;
- cần đo latency p95/p99, không chỉ throughput trung bình.

Kết luận thực dụng:

```text
NFS có thể dùng cho một số môi trường được kiểm soát tốt,
nhưng không nên là lựa chọn mặc định cho MariaDB data directory.
```

## 5. Storage I/O: buffering và persistence

### 5.1. Vì sao phần này quan trọng?

MariaDB, đặc biệt là InnoDB, cần kiểm soát chính xác khi nào dữ liệu thật sự xuống non-volatile storage.

Hệ điều hành thường buffer writes trong memory:

```text
application write
-> OS page cache
-> flush xuống disk sau
```

Cách này nhanh, nhưng với database transactional có rủi ro:

```text
MariaDB nghĩ dữ liệu đã ghi
OS vẫn giữ trong memory
power loss xảy ra
-> dữ liệu committed có thể mất nếu chưa persist
```

Vì vậy database cần phân biệt:

- write vào OS cache;
- write-through xuống storage;
- persist/sync dữ liệu từ cache xuống non-volatile storage;
- unbuffered I/O để bypass OS page cache.

Các khái niệm chính:

| Khái niệm | Ý nghĩa |
| --- | --- |
| Buffered I/O | Đọc/ghi qua OS page cache. |
| Unbuffered I/O | Bypass OS page cache, database tự quản lý cache chính. |
| Write-through | Mỗi write chỉ hoàn tất khi storage layer đã nhận dữ liệu bền vững hơn. |
| Persist to non-volatile storage | Ép dữ liệu đã buffered phải được flush xuống storage bền vững. |

## 6. Unbuffered I/O là gì?

### 6.1. Khái niệm

Unbuffered I/O là chế độ đọc/ghi bỏ qua OS page cache.

Luồng concept:

```text
MariaDB/InnoDB
-> đọc/ghi trực tiếp với storage
-> không giữ thêm bản copy trong OS page cache
```

Mục tiêu là tránh double buffering:

```text
InnoDB buffer pool đã cache data pages
OS page cache lại cache cùng data pages
-> tốn RAM hai lần
```

Nếu InnoDB buffer pool đã chiếm phần lớn RAM để cache data, việc OS cache lại cùng dữ liệu có thể lãng phí.

### 6.2. Cơ chế bên dưới theo OS

| OS | Cơ chế tương ứng |
| --- | --- |
| Unix-like | File mở với `O_DIRECT` nếu filesystem/OS hỗ trợ. |
| Windows | File mở với `FILE_FLAG_NO_BUFFERING`. |

`O_DIRECT` không phải lúc nào cũng được hỗ trợ như nhau trên mọi filesystem/OS. Một số filesystem hoặc OS có hạn chế riêng.

### 6.3. Đánh đổi của Unbuffered I/O

Ưu điểm:

- tránh double buffering;
- RAM tập trung cho InnoDB buffer pool;
- database kiểm soát cache tốt hơn;
- phù hợp khi InnoDB quản lý phần lớn memory cache.

Nhược điểm:

- mất một số tối ưu của OS page cache;
- OS readahead/write coalescing có thể ít hữu ích hơn;
- I/O pattern xấu từ database sẽ lộ rõ hơn xuống storage;
- cần storage và database buffer pool được sizing tốt.

Tư duy:

```text
Nếu InnoDB buffer pool là cache chính:
-> unbuffered I/O thường hợp lý hơn.

Nếu workload phụ thuộc nhiều vào OS page cache:
-> buffered I/O có thể vẫn có lợi.
```

## 7. Write-Through

### 7.1. Write-through là gì?

Write-through nghĩa là mỗi write call chỉ được xem là hoàn tất khi storage layer đã nhận dữ liệu theo cách bền vững hơn.

Concept:

```text
write
-> storage nhận dữ liệu
-> đảm bảo bền vững hơn
-> write call mới return
```

Điều này giống với việc mỗi write đều đi kèm một yêu cầu persist ngay lập tức.

### 7.2. Cơ chế bên dưới theo OS

| OS | Cơ chế tương ứng |
| --- | --- |
| Unix-like | File mở với `O_DSYNC` trên các OS hỗ trợ. |
| Windows | File mở với `FILE_FLAG_WRITE_THROUGH`. |

### 7.3. Đánh đổi của Write-through

Ưu điểm:

- durability mạnh cho từng write;
- application/database không cần gọi persist riêng sau mỗi write;
- giảm rủi ro mất dữ liệu đã ghi nếu power loss.

Nhược điểm:

- latency ghi cao;
- khó tận dụng batch write;
- throughput write có thể giảm mạnh;
- không phù hợp nếu mỗi transaction nhỏ đều phải trả chi phí persist riêng.

Tư duy:

```text
write-through
= an toàn hơn trên từng write
= chậm hơn vì trả durability cost nhiều lần
```

## 8. Persist xuống non-volatile storage

### 8.1. Persist là gì?

Persist nghĩa là sau một hoặc nhiều buffered writes, database gọi cơ chế để ép dữ liệu còn trong OS page cache hoặc storage write cache xuống non-volatile media.

Concept:

```text
write nhiều lần
-> dữ liệu có thể đang ở OS cache / device cache
-> persist call
-> ép dữ liệu xuống non-volatile storage
-> call return khi durable
```

Điểm khác với write-through:

```text
write-through:
mỗi write trả durability cost ngay

persist:
cho phép gom nhiều write
rồi trả durability cost một lần
```

Đây là nền tảng của:

- group commit;
- binary log durability;
- InnoDB redo log flush;
- transaction commit durability.

### 8.2. Cơ chế bên dưới theo OS

| OS | Cơ chế tương ứng |
| --- | --- |
| Unix-like | `fdatasync()` khi có thể; `fsync()` khi cần fallback. |
| Windows | `NtFlushBuffersFileEx()` với flag nhẹ hơn khi có thể; `FlushFileBuffers()` khi cần fallback. |

MariaDB thường ưu tiên cơ chế nhẹ hơn nếu đủ an toàn cho dữ liệu database. Ví dụ, nếu database không cần persist file access/modification timestamp, nó có thể chọn cơ chế chỉ flush data cần thiết thay vì flush cả metadata không quan trọng.

### 8.3. Đánh đổi của Persist

Ưu điểm:

- cho phép batch nhiều write;
- phù hợp group commit;
- cân bằng performance và durability tốt hơn write-through từng write;
- database kiểm soát thời điểm durability rõ hơn.

Nhược điểm:

- nếu persist quá thưa, rủi ro mất dữ liệu committed tăng tùy cấu hình;
- nếu persist quá dày, latency tăng;
- phụ thuộc storage có thật sự honor flush/fsync hay không;
- write cache của disk/controller nếu không có bảo vệ có thể gây rủi ro.

Tư duy:

```text
Persist là điểm database trả chi phí durability.
Trả quá thường xuyên -> chậm.
Trả quá ít -> rủi ro mất dữ liệu cao hơn.
```

## 9. Liên hệ với biến MariaDB

### 9.1. `innodb_flush_method`

`innodb_flush_method` chọn cách InnoDB kết hợp buffering và persistence.

Nó liên quan trực tiếp đến:

- có dùng OS page cache hay không;
- có dùng direct/unbuffered I/O hay không;
- cách flush dữ liệu/log xuống storage;
- mức độ double buffering.

Tư duy:

```text
InnoDB buffer pool lớn
-> thường muốn giảm double buffering
-> cân nhắc unbuffered/direct I/O
```

Không nên chọn `innodb_flush_method` chỉ vì thấy một giá trị được khuyến nghị chung. Cần xét:

- OS;
- filesystem;
- storage;
- buffer pool size;
- workload read/write;
- durability requirement;
- benchmark thực tế.

### 9.2. `sync_binlog`

`sync_binlog` điều khiển tần suất binary log được persist xuống storage.

Concept:

```text
sync_binlog thấp/nghiêm ngặt hơn
-> binlog durable hơn
-> commit/write latency cao hơn

sync_binlog ít nghiêm ngặt hơn
-> performance tốt hơn
-> power loss có thể mất binlog event gần nhất
```

Biến này rất quan trọng nếu dùng replication hoặc cần point-in-time recovery dựa trên binary log.

### 9.3. `innodb_doublewrite`

`innodb_doublewrite` là cơ chế bảo vệ InnoDB khỏi torn page.

Torn page là tình huống một database page chỉ được ghi một phần xuống storage rồi crash/power loss xảy ra.

Concept:

```text
ghi page trực tiếp
-> crash giữa chừng
-> page có thể hỏng một nửa

doublewrite
-> ghi bản an toàn trước
-> sau đó ghi vào vị trí thật
-> crash recovery có thể dùng bản an toàn
```

`innodb_doublewrite` không phải là OS optimization trực tiếp, nhưng liên quan chặt đến storage durability và write behavior.

## 10. Cách chọn chiến lược I/O theo workload

### 10.1. OLTP write-heavy

Đặc điểm:

- nhiều transaction nhỏ;
- commit thường xuyên;
- redo log/binlog quan trọng;
- latency fsync ảnh hưởng trực tiếp throughput.

Ưu tiên:

- storage fsync latency thấp;
- durability setting rõ ràng;
- tránh double buffering nếu InnoDB buffer pool lớn;
- kiểm tra `sync_binlog`, redo log flush, group commit;
- dùng filesystem/storage ổn định với crash recovery.

### 10.2. OLAP/read-heavy

Đặc điểm:

- query dài;
- scan nhiều dữ liệu;
- ít transaction nhỏ;
- throughput đọc quan trọng hơn commit latency.

Ưu tiên:

- đủ RAM/buffer pool;
- storage throughput cao;
- filesystem ổn định với file lớn;
- OS readahead/page cache có thể hữu ích hơn tùy cách đọc;
- tránh cấu hình làm mất lợi ích sequential scan.

### 10.3. Mixed workload

Đặc điểm:

- vừa transaction ngắn;
- vừa report/query dài;
- vừa cần durability;
- vừa cần throughput đọc.

Ưu tiên:

- tách workload nếu có thể;
- replica cho report;
- không chỉ tối ưu cho một loại I/O;
- đo p95/p99 latency;
- kiểm tra flush behavior dưới tải thật.

## 11. Checklist thực dụng

Khi tối ưu OS cho MariaDB, nên hỏi:

1. Data directory dùng filesystem nào?
2. Có đang cập nhật access time không cần thiết không?
3. Có dùng NFS cho data/log không?
4. Storage có đảm bảo flush/fsync thật không?
5. InnoDB buffer pool có đủ lớn để là cache chính không?
6. Có bị double buffering giữa InnoDB và OS page cache không?
7. Workload cần durability mạnh hay ưu tiên throughput?
8. `sync_binlog` có phù hợp replication/PITR không?
9. `innodb_flush_method` có phù hợp OS/filesystem/storage không?
10. Có đo fsync latency, IOPS, throughput, p95/p99 chưa?

Kết luận thực dụng:

```text
Filesystem chọn đúng giúp hệ thống ổn định.
noatime giảm metadata write không cần thiết.
NFS không nên là mặc định cho data directory.
Unbuffered I/O giúp tránh double buffering.
Write-through tối đa durability nhưng tăng latency.
Persist/fsync là điểm cân bằng giữa batching và an toàn dữ liệu.
```
