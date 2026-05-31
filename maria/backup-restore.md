Full tutorial: https://mariadb.com/docs/server/server-usage/backup-and-restore

# MariaDB Backup and Restore

## Overview

Backup và restore trong MariaDB là nhóm thao tác dùng để bảo vệ dữ liệu khi có sự cố như lỗi server, lỗi ổ đĩa, deploy sai, xóa nhầm dữ liệu, hoặc cần di chuyển dữ liệu sang môi trường khác.

Một bản backup tốt không chỉ là "có file backup". Nó phải thỏa mãn các điều kiện sau:

- Có thể restore lại được.
- Dữ liệu sau restore phải nhất quán.
- Quy trình restore phải được test trước.
- Thời gian restore phù hợp với yêu cầu của hệ thống.
- Backup phải được bảo vệ như dữ liệu production.

Trong MariaDB có hai hướng backup chính:

- **Logical backup**: backup thành các câu SQL.
- **Physical backup**: backup trực tiếp các file dữ liệu của MariaDB.

---

## Forming a backup strategy

Chiến lược backup không nên bắt đầu bằng câu hỏi "dùng tool nào?". Nó nên bắt đầu bằng câu hỏi "business cần khôi phục dữ liệu như thế nào khi có sự cố?".

Một chiến lược backup tốt thường phải trả lời được:

- Dữ liệu nào cần bảo vệ?
- Dữ liệu đó phục vụ mục đích nghiệp vụ gì?
- Được phép mất tối đa bao nhiêu dữ liệu?
- Cần khôi phục dịch vụ trong bao lâu?
- Backup cần giữ lại trong bao lâu?
- Backup lưu ở đâu?
- Backup có cần mã hóa không?
- Quy trình restore đã được test chưa?

Các yếu tố chính khi thiết kế backup strategy:

- Data inventory.
- Recovery objectives: RPO và RTO.
- Replication hoặc clustering environment.
- Encryption requirements.
- Backup storage strategy.
- Backup and recovery testing.

---

## Data inventory

Data inventory là bước kiểm kê dữ liệu trước khi thiết kế backup. Nếu không biết hệ thống đang có loại dữ liệu nào, rất khó quyết định backup cái gì, backup bao lâu, và restore như thế nào.

Các câu hỏi cần trả lời:

1. Database đang chứa dữ liệu gì?
2. Dữ liệu này phục vụ mục đích nghiệp vụ nào?
3. Dữ liệu cần được giữ trong bao lâu?
4. Có yêu cầu pháp lý, audit, compliance hoặc regulatory nào ảnh hưởng đến thời gian lưu trữ không?
5. Dữ liệu nào là critical, dữ liệu nào có thể tái tạo?
6. Dữ liệu nào cần restore trước khi có sự cố lớn?

Ví dụ:

| Nhóm dữ liệu | Mức độ quan trọng | Gợi ý backup |
| --- | --- | --- |
| Orders, payments | Rất cao | Backup thường xuyên, test restore kỹ |
| User profile | Cao | Backup thường xuyên |
| Report cache | Trung bình | Có thể tái tạo nếu có source data |
| Application logs | Tùy yêu cầu audit | Retention phụ thuộc compliance |
| Temporary data | Thấp | Có thể không cần backup dài hạn |

Data inventory giúp tránh hai lỗi phổ biến:

- Backup quá ít: thiếu dữ liệu quan trọng khi restore.
- Backup quá nhiều: tốn storage, restore chậm, quản lý phức tạp.

---

## Recovery objectives: RPO và RTO

Backup strategy thường được đo bằng hai chỉ số quan trọng:

- **RPO**: Recovery Point Objective.
- **RTO**: Recovery Time Objective.

## RPO là gì?

RPO là lượng dữ liệu tối đa mà business chấp nhận mất khi có sự cố.

Ví dụ:

```text
RPO = 24 giờ
```

Nghĩa là nếu có sự cố, business chấp nhận mất tối đa dữ liệu phát sinh trong 24 giờ gần nhất.

Nếu backup chạy mỗi ngày lúc 00:00 và sự cố xảy ra lúc 23:00, hệ thống có thể mất gần 23 giờ dữ liệu nếu không có replication, binlog hoặc incremental backup bổ sung.

RPO càng nhỏ thì backup phải càng thường xuyên hoặc phải kết hợp thêm replication/binlog.

Ví dụ:

| RPO | Ý nghĩa | Chiến lược thường gặp |
| --- | --- | --- |
| 24 giờ | Mất tối đa 1 ngày dữ liệu | Full backup hằng ngày |
| 1 giờ | Mất tối đa 1 giờ dữ liệu | Full backup + incremental/binlog |
| 5 phút | Gần như không được mất dữ liệu | Replication, binlog, HA, backup thường xuyên |

Điểm quan trọng: RPO được tính theo thời điểm backup hoàn tất, không phải thời điểm backup bắt đầu. Nếu backup bắt đầu lúc 00:00 nhưng hoàn tất lúc 02:00, recovery point thực tế thường gắn với bản backup đã hoàn tất và usable.

## Cách đạt RPO

Một số kỹ thuật giúp đạt RPO:

- Chạy incremental backup thường xuyên hơn full backup.
- Chạy full backup ít thường xuyên hơn để giảm tải.
- Kết hợp backup với replication hoặc clustering để backup trên replica/node ít ảnh hưởng production.
- Theo dõi tự động trạng thái backup.
- Test tự động hoặc định kỳ để chắc chắn backup restore được.
- Lưu binary log nếu cần point-in-time recovery.

Ví dụ lịch backup theo RPO:

```text
RPO 24 giờ:
- Full backup mỗi ngày

RPO 1 giờ:
- Full backup mỗi tuần
- Incremental backup mỗi giờ
- Lưu binlog

RPO vài phút:
- Replication
- Binlog shipping
- Backup định kỳ
- Monitoring và alerting nghiêm ngặt
```

## RTO là gì?

RTO là thời gian tối đa business muốn dịch vụ được khôi phục sau khi quyết định restore.

Ví dụ:

```text
RTO = 8 giờ
```

Nghĩa là sau khi quyết định recovery, hệ thống cần được khôi phục trong vòng 8 giờ.

RTO không chỉ phụ thuộc vào file backup. Nó phụ thuộc vào toàn bộ quy trình:

- Tìm đúng backup.
- Download/copy backup từ storage.
- Prepare backup.
- Restore vào data directory.
- Fix permission.
- Start MariaDB.
- Kiểm tra dữ liệu.
- Chuyển application traffic về hệ thống đã restore.

## Cách đạt RTO

Một số kỹ thuật giúp đạt RTO:

- Lưu backup ở nơi có tốc độ truy cập nhanh.
- Hiểu rõ performance của backup storage cho cả backup và restore.
- Có runbook restore cụ thể, gồm command và thứ tự thao tác.
- Test restore định kỳ.
- Dùng delayed replication để có đường recovery nhanh hơn trong một số lỗi logic như xóa nhầm dữ liệu.
- Dùng thông tin từ incident response để restore đúng phạm vi, tránh restore thừa dữ liệu không cần thiết.
- Tổ chức drill/exercise để xác nhận team có thể restore trong thời gian yêu cầu.

Ví dụ:

```text
Backup nằm ở remote object storage rất rẻ nhưng download mất 10 giờ
=> Không thể đạt RTO 2 giờ nếu không có bản backup gần hơn hoặc storage nhanh hơn.
```

---

## Replication considerations

Replication không thay thế backup, nhưng có thể là một phần rất hữu ích của backup strategy.

Cơ chế cơ bản:

```text
Application ghi dữ liệu vào Primary
Primary ghi thay đổi vào binary log
Replica đọc thay đổi từ Primary
Replica apply lại thay đổi vào dữ liệu local của nó
Backup chạy trên Replica thay vì chạy trên Primary
```

Mục tiêu là tách workload backup ra khỏi primary. Primary vẫn phục vụ ứng dụng chính, còn replica chịu phần tải do backup tạo ra.

## Vì sao replication không phải backup?

Lý do replication không thay thế backup:

- Nếu application xóa nhầm dữ liệu, lỗi có thể replicate sang replica.
- Nếu deploy sai update hàng loạt, replica cũng có thể nhận thay đổi sai.
- Nếu bị corruption logic, replica có thể chứa cùng dữ liệu sai.
- Nếu có câu lệnh nguy hiểm như drop/truncate/update sai, replica thường cũng apply câu lệnh đó.

Replication là cơ chế sao chép thay đổi, không phải cơ chế lưu lịch sử nhiều điểm khôi phục.

Nói ngắn:

```text
Replica giúp có bản sao đang chạy gần realtime.
Backup giúp có điểm khôi phục lịch sử.
```

Nếu primary bị hỏng phần cứng, replica có thể giúp failover hoặc lấy dữ liệu gần nhất. Nhưng nếu dữ liệu sai đã được ghi hợp lệ vào primary rồi replicate sang replica, replica cũng có thể sai theo.

## Replica giúp backup như thế nào?

Replication hữu ích cho backup vì có thể dùng replica làm nguồn backup, giảm ảnh hưởng lên primary.

Ví dụ:

```text
Application ghi vào Primary
Replica nhận dữ liệu từ Primary
Backup chạy trên Replica
```

Cơ chế này có các lợi ích:

- Giảm I/O backup trên primary.
- Giảm CPU/network/disk workload trên primary.
- Có thể lock, stop hoặc throttle replica khi cần backup mà không chặn primary.
- Có thể backup thường xuyên hơn vì ít ảnh hưởng workload chính.
- Primary có thể tiếp tục phục vụ application trong khi backup chạy trên replica.

Điểm quan trọng là backup từ replica chỉ đáng tin nếu replica đang đồng bộ đúng với primary ở thời điểm backup mong muốn.

## Dedicated backup replica

Trong hệ thống lớn, có thể có một replica riêng chỉ dùng cho backup.

Mô hình:

```text
Primary
  -> Replica phục vụ read traffic
  -> Replica riêng cho backup
```

Replica backup có thể được cấu hình để không nhận application read traffic. Nhờ vậy backup workload không cạnh tranh tài nguyên với user query.

Replica backup hữu ích khi:

- Backup nặng và kéo dài.
- Primary không thể bị lock hoặc tăng I/O.
- Read replica phục vụ user query không nên bị backup làm chậm.
- Muốn tách rõ vai trò: replica cho read, replica cho backup.

## GTID và vị trí replication ở mức cơ chế

Khi tạo replica, cần biết replica bắt đầu sao chép từ điểm nào trong lịch sử thay đổi của primary. MariaDB có thể dùng GTID để biểu diễn vị trí này.

Hiểu đơn giản:

```text
GTID = định danh vị trí giao dịch trong luồng replication
```

Replica cần biết:

```text
Tôi đã có dữ liệu tới điểm nào?
Từ điểm nào tôi cần đọc tiếp thay đổi từ primary?
```

Nếu vị trí replication sai, replica có thể:

- Thiếu transaction.
- Apply trùng transaction.
- Bị lệch dữ liệu so với primary.
- Không thể dùng làm nguồn backup đáng tin cậy.

Vì vậy backup từ replica cần gắn với việc kiểm tra replication status và replication lag.

## Replication lag ảnh hưởng backup thế nào?

Replica thường có thể chậm hơn primary.

Ví dụ:

```text
10:00 primary đã commit transaction T100
10:00 replica mới apply tới T95
```

Nếu backup chạy trên replica lúc này, backup chỉ chứa dữ liệu tới T95, không phải T100.

Điều này không nhất thiết sai, nhưng phải được hiểu rõ vì nó ảnh hưởng RPO. Nếu business nghĩ backup là dữ liệu tới 10:00 nhưng replica đang lag 10 phút, recovery point thực tế cũ hơn kỳ vọng.

Do đó trước khi backup từ replica cần kiểm tra:

- Replication có đang chạy không?
- Replica có bị lag nhiều không?
- Replica đã apply tới vị trí mong muốn chưa?
- Có lỗi replication nào không?

## Rủi ro lệch dữ liệu primary và replica

Primary và replica có thể lệch dữ liệu nếu:

- Có write trực tiếp vào replica.
- Replication bị dừng hoặc lỗi mà không được phát hiện.
- Statement không deterministic tạo kết quả khác nhau.
- Foreign key, trigger, function hoặc cấu hình môi trường khác nhau.
- Backup chạy khi replica chưa apply đủ thay đổi.

Vì vậy replica dùng để backup nên được quản lý như một thành phần production:

- Không cho application write vào replica backup.
- Monitor replication status.
- Monitor replication lag.
- Alert khi replication dừng.
- Test restore từ backup lấy trên replica.

Với cluster, ví dụ MariaDB Enterprise Cluster hoặc Galera-style topology, mỗi node có thể có bản sao dữ liệu. Backup có thể chạy từ một node trong cluster, nhưng vẫn cần kiểm tra:

- Node có đang synced không?
- Backup có ảnh hưởng cluster flow control không?
- Node backup có đủ tài nguyên I/O không?
- Backup có nhất quán với yêu cầu restore không?

Nếu dùng delayed replication, replica cố tình chạy chậm hơn primary một khoảng thời gian. Cách này hữu ích khi có lỗi logic.

Ví dụ:

```text
Primary bị xóa nhầm dữ liệu lúc 10:00
Delayed replica chậm 1 giờ, vẫn chưa apply lệnh xóa
Team có thể dừng replica và lấy dữ liệu trước thời điểm lỗi
```

Delayed replication không thay thế backup dài hạn, nhưng có thể giảm RTO trong một số tình huống.

---

## Backup optimization

Backup optimization là các kỹ thuật giảm thời gian backup, giảm thời gian restore, giảm tải lên production hoặc giảm dung lượng lưu trữ.

Điểm cần nhớ: hầu hết tối ưu đều có trade-off.

Ví dụ:

| Tối ưu | Giải quyết vấn đề | Đánh đổi |
| --- | --- | --- |
| Prepare backup sớm | Giảm thời gian restore | Tốn thêm tài nguyên xử lý sau backup |
| `--move-back` | Tiết kiệm disk hoặc thời gian copy | Làm thay đổi nội dung thư mục backup |
| `--parallel` | Tận dụng nhiều CPU/core | Tăng tải CPU/I/O khi backup |
| Incremental backup | Giảm thời gian backup và dung lượng | Restore phức tạp hơn |
| Storage snapshot | Giảm thời gian tác động lên production | Phụ thuộc storage, có thể cần crash recovery |

Vì vậy không nên bật tối ưu chỉ vì nó "nhanh hơn". Cần đối chiếu với RPO, RTO, tài nguyên server, độ an toàn backup và khả năng test restore.

---

## Logical vs Physical Backups

## Logical backup

Logical backup là backup ở mức logic của database. File backup thường chứa các câu SQL như:

```sql
CREATE DATABASE ...
CREATE TABLE ...
INSERT INTO ...
```

Khi restore, MariaDB chạy lại các câu SQL đó để tạo lại database, table và dữ liệu.

Công cụ phổ biến:

```bash
mariadb-dump
```

Ví dụ backup:

```bash
mariadb-dump db_name > backup-file.sql
```

Ví dụ restore:

```bash
mariadb db_name < backup-file.sql
```

Logical backup phù hợp khi:

- Database nhỏ hoặc vừa.
- Cần migrate dữ liệu sang server khác.
- Cần restore sang MariaDB version khác.
- Cần đọc, sửa, lọc hoặc kiểm tra nội dung backup.
- Cần backup từng database hoặc từng table đơn giản.

Nhược điểm:

- Backup và restore thường chậm hơn physical backup.
- File backup thường lớn hơn.
- Với database lớn, restore có thể mất rất nhiều thời gian.
- Không backup file cấu hình, log file, redo log, undo log.
- Với InnoDB, logical backup thường scan toàn bộ table, có thể ảnh hưởng buffer pool.

## Physical backup

Physical backup là backup ở mức file. Thay vì xuất ra câu SQL, công cụ backup copy các file dữ liệu thật trong data directory của MariaDB.

Công cụ chính:

```bash
mariadb-backup
```

Physical backup phù hợp khi:

- Database lớn.
- Production cần backup nhanh.
- Cần restore nhanh hơn logical backup.
- Cần incremental backup.
- Cần backup online để giảm downtime.

Nhược điểm:

- Ít linh hoạt hơn logical backup.
- Thường phụ thuộc version, storage engine, cấu trúc file và môi trường MariaDB.
- Không dùng để restore trực tiếp sang DBMS khác.
- Restore thường yêu cầu data directory trống.
- Cần chạy `--prepare` trước khi restore.

## So sánh nhanh

| Tiêu chí | Logical backup | Physical backup |
| --- | --- | --- |
| Dạng backup | SQL statements | File dữ liệu thật |
| Công cụ thường dùng | `mariadb-dump` | `mariadb-backup` |
| Linh hoạt | Cao | Thấp hơn |
| Tốc độ backup | Chậm hơn | Nhanh hơn |
| Tốc độ restore | Chậm hơn | Nhanh hơn |
| Phù hợp database lớn | Không tối ưu | Tốt hơn |
| Restore sang DBMS khác | Có thể nếu SQL tương thích | Không phù hợp |
| Incremental backup | Không phải thế mạnh chính | Có hỗ trợ |

---

## Backup Tools

## `mariadb-backup`

`mariadb-backup` là công cụ physical online backup của MariaDB. Nó được fork từ Percona XtraBackup và được MariaDB phát triển để phù hợp với MariaDB Server.

Điểm quan trọng:

- Backup ở mức file.
- Có thể backup khi MariaDB Server đang chạy.
- Phù hợp với InnoDB và một số storage engine được hỗ trợ.
- Hỗ trợ full backup.
- Hỗ trợ incremental backup.
- Hỗ trợ partial backup.
- Có thể dùng với compression.
- Có thể dùng với encryption-at-rest.

`mariadb-backup` thường là lựa chọn thực tế hơn `mariadb-dump` khi database production lớn và cần RTO thấp hơn.

## Multithreading với `--parallel`

`mariadb-backup` có thể chạy nhiều luồng để copy file dữ liệu song song bằng option `--parallel`.

Ví dụ:

```bash
mariadb-backup \
  --backup \
  --target-dir=/data/backups/full \
  --user=mariadb-backup \
  --password=mbu_passwd \
  --parallel=12
```

Tác dụng:

- Tận dụng server có nhiều CPU core.
- Tăng tốc copy nhiều data file song song.
- Có thể giảm thời gian backup khi bottleneck nằm ở CPU hoặc khả năng đọc/ghi song song.

Đánh đổi:

- Tăng CPU usage.
- Tăng I/O workload.
- Có thể làm production chậm hơn nếu chạy trên cùng server đang phục vụ traffic.
- Không phải lúc nào cũng nhanh hơn nếu bottleneck là disk, network hoặc backup storage.

Nên test nhiều mức `--parallel`, ví dụ `2`, `4`, `8`, `12`, rồi đo backup time và ảnh hưởng lên workload production.

---

## Full backup

Full backup là bản backup đầy đủ của dữ liệu trong phạm vi backup.

Hiểu đơn giản:

```text
Full backup = chụp lại toàn bộ dữ liệu cần bảo vệ tại một thời điểm
```

Nếu backup toàn bộ server, full backup chứa toàn bộ dữ liệu cần thiết để khôi phục MariaDB instance. Nếu backup một phạm vi nhỏ hơn, ví dụ một database cụ thể, thì full backup chứa đầy đủ dữ liệu trong phạm vi đó.

Ví dụ database có:

```text
users
orders
order_items
payments
products
inventory
```

Full backup sẽ copy toàn bộ dữ liệu trong phạm vi được chọn, không chỉ phần vừa thay đổi.

## Full backup dùng để làm gì?

Full backup thường là nền tảng của chiến lược backup.

Ví dụ lịch backup phổ biến:

```text
Chủ nhật: full backup
Thứ hai: incremental backup
Thứ ba: incremental backup
Thứ tư: incremental backup
Thứ năm: incremental backup
Thứ sáu: incremental backup
Thứ bảy: incremental backup
```

Nếu cần restore tới thứ tư, ta dùng:

```text
full backup Chủ nhật
+ incremental backup Thứ hai
+ incremental backup Thứ ba
+ incremental backup Thứ tư
```

Full backup đóng vai trò là mốc gốc. Các incremental backup sau đó chỉ lưu phần thay đổi so với mốc trước.

## Ưu điểm của full backup

- Dễ hiểu nhất.
- Có thể restore độc lập.
- Quy trình restore đơn giản hơn incremental backup.
- Ít phụ thuộc vào chuỗi backup khác.
- Rất quan trọng cho disaster recovery.

## Nhược điểm của full backup

- Tốn nhiều dung lượng.
- Tốn nhiều thời gian hơn incremental backup.
- Tạo nhiều I/O trên server.
- Nếu database rất lớn, chạy full backup mỗi ngày có thể không thực tế.

## Ví dụ lệnh full backup

```bash
mariadb-backup \
  --backup \
  --target-dir=/backup/full-2026-05-18 \
  --user=mariabackup \
  --password=mbu_passwd
```

Ý nghĩa:

| Option | Ý nghĩa |
| --- | --- |
| `--backup` | Chạy ở chế độ tạo backup |
| `--target-dir` | Thư mục chứa bản backup |
| `--user` | User dùng để kết nối MariaDB |
| `--password` | Password của user backup |

---

## Incremental backup

Incremental backup là backup chỉ phần dữ liệu thay đổi kể từ một backup trước đó.

Hiểu đơn giản:

```text
Incremental backup = chỉ lưu phần thay đổi, không copy lại toàn bộ database
```

Ví dụ:

```text
Chủ nhật 00:00: full backup
Thứ hai 00:00: incremental backup, chứa thay đổi từ Chủ nhật đến Thứ hai
Thứ ba 00:00: incremental backup, chứa thay đổi từ Thứ hai đến Thứ ba
Thứ tư 00:00: incremental backup, chứa thay đổi từ Thứ ba đến Thứ tư
```

Incremental backup không phải bản backup hoàn chỉnh. Nó thường không thể restore một mình. Nó phải đi cùng full backup và các incremental backup trước đó.

## Ví dụ dễ hiểu

Giả sử lúc full backup:

```text
users: 1000 rows
orders: 5000 rows
payments: 2000 rows
```

Sang ngày hôm sau:

```text
users: thêm 100 rows
orders: thêm 700 rows
payments: update 50 rows
```

Incremental backup chỉ cần lưu phần thay đổi này, thay vì copy lại toàn bộ `users`, `orders`, `payments`.

Nếu database 1 TB nhưng trong ngày chỉ thay đổi 20 GB, incremental backup có thể nhỏ hơn rất nhiều so với full backup.

## Ưu điểm của incremental backup

- Nhanh hơn full backup.
- Tiết kiệm storage.
- Giảm I/O so với full backup thường xuyên.
- Phù hợp database lớn.
- Cho phép backup thường xuyên hơn với chi phí thấp hơn.

## Nhược điểm của incremental backup

- Restore phức tạp hơn.
- Phải giữ đủ chuỗi backup.
- Phải apply incremental theo đúng thứ tự.
- Mất một bản incremental ở giữa có thể làm hỏng toàn bộ chuỗi restore sau nó.
- Cần quản lý metadata backup cẩn thận.

Ví dụ chuỗi backup:

```text
full
inc1
inc2
inc3
```

Nếu mất `inc2`, thì `inc3` không còn đủ ý nghĩa, vì `inc3` được tạo dựa trên trạng thái sau `inc2`.

## Ví dụ tạo incremental backup

Tạo full backup trước:

```bash
mariadb-backup \
  --backup \
  --target-dir=/backup/full \
  --user=mariabackup \
  --password=mbu_passwd
```

Tạo incremental backup đầu tiên dựa trên full backup:

```bash
mariadb-backup \
  --backup \
  --target-dir=/backup/inc1 \
  --incremental-basedir=/backup/full \
  --user=mariabackup \
  --password=mbu_passwd
```

Tạo incremental backup tiếp theo dựa trên incremental trước đó:

```bash
mariadb-backup \
  --backup \
  --target-dir=/backup/inc2 \
  --incremental-basedir=/backup/inc1 \
  --user=mariabackup \
  --password=mbu_passwd
```

Điểm cần nhớ:

```text
inc1 dựa trên full
inc2 dựa trên inc1
inc3 dựa trên inc2
```

Không nên tự ý đổi thứ tự nếu không hiểu rõ backup chain.

Trong ví dụ trên, `--incremental-basedir` không luôn luôn trỏ về full backup. Nó trỏ về bản backup liền trước trong chain.

Ví dụ đúng:

```text
Tạo inc1:
--incremental-basedir=/backup/full

Tạo inc2:
--incremental-basedir=/backup/inc1

Tạo inc3:
--incremental-basedir=/backup/inc2
```

Lợi ích của cách này là mỗi incremental backup chỉ cần chứa phần thay đổi mới nhất, giúp giảm thời gian backup và giảm tài nguyên sử dụng trong lúc backup.

Đánh đổi là restore phức tạp hơn. Khi restore, phải apply toàn bộ chain theo đúng thứ tự vào full backup:

```text
full -> inc1 -> inc2 -> inc3
```

Nếu chain dài, thời gian prepare/restore cũng tăng. Vì vậy trong production thường không để chain incremental quá dài; sau một khoảng thời gian sẽ tạo full backup mới để reset chain.

---

## Partial backup

Partial backup là backup một phần dữ liệu thay vì toàn bộ MariaDB Server.

Ví dụ partial backup có thể là:

- Chỉ backup một database.
- Chỉ backup một số table.
- Chỉ backup nhóm table quan trọng.

Hiểu đơn giản:

```text
Partial backup = chỉ backup phần mình chọn
```

## Khi nào dùng partial backup?

Partial backup hữu ích khi:

- Database rất lớn nhưng chỉ một vài table cần backup riêng.
- Cần chuyển một nhóm table sang môi trường test.
- Cần backup table quan trọng thường xuyên hơn.
- Cần giảm thời gian backup.
- Cần tách riêng dữ liệu nghiệp vụ và dữ liệu log/archive.

Ví dụ hệ thống có:

```text
app_core
app_log
app_report
app_archive
```

Nếu `app_log` rất lớn nhưng có thể tái tạo hoặc không quan trọng bằng dữ liệu giao dịch, bạn có thể chỉ backup `app_core` cho một số tình huống nhất định.

## Rủi ro của partial backup

Partial backup dễ gây hiểu nhầm vì "backup thành công" không có nghĩa là "restore đủ nghiệp vụ".

Ví dụ các table liên quan:

```text
orders
order_items
payments
shipments
```

Nếu chỉ backup `orders` nhưng không backup `order_items`, sau restore dữ liệu đơn hàng sẽ thiếu chi tiết sản phẩm. Về mặt kỹ thuật restore có thể chạy, nhưng về mặt nghiệp vụ dữ liệu không còn đầy đủ.

## Ưu điểm của partial backup

- Nhanh hơn full backup toàn hệ thống.
- Tiết kiệm storage.
- Linh hoạt cho test, debug, migration một phần.

## Nhược điểm của partial backup

- Không thay thế được full backup cho disaster recovery.
- Dễ thiếu table liên quan.
- Restore có thể phức tạp hơn.
- Với InnoDB, partial restore có thể cần `--export` để tạo file `.cfg`.

---

## Compression

`mariadb-backup` hỗ trợ compression, tức là nén dữ liệu backup để giảm dung lượng lưu trữ.

Hiểu đơn giản:

```text
Backup không nén: copy dữ liệu ra thư mục backup
Backup có nén: copy dữ liệu và nén lại để tiết kiệm disk
```

Compression hữu ích khi:

- Database lớn.
- Cần giữ nhiều bản backup.
- Storage backup giới hạn.
- Cần chuyển backup qua network.
- Backup được lưu lâu dài ở remote storage.

Ví dụ:

```text
Backup không nén: 500 GB
Backup có nén: có thể còn 250 GB hoặc thấp hơn
```

Tỷ lệ nén phụ thuộc loại dữ liệu. Text, JSON, log thường nén tốt. File binary, ảnh, video hoặc dữ liệu đã nén sẵn thường nén kém.

## Đánh đổi khi dùng compression

Compression giúp tiết kiệm disk, nhưng tốn CPU.

| Lợi ích | Chi phí |
| --- | --- |
| Giảm dung lượng backup | Tốn CPU để nén |
| Giảm dữ liệu truyền qua network | Có thể backup chậm hơn |
| Lưu được nhiều bản backup hơn | Restore có thể cần giải nén |

Trong production, cần cân nhắc chạy compression vào thời điểm server ít tải hơn.

---

## Encryption-at-rest

Encryption-at-rest nghĩa là dữ liệu backup được mã hóa khi nằm trên disk hoặc storage.

Hiểu đơn giản:

```text
Ai đó có file backup nhưng không có key thì không đọc được dữ liệu
```

Điều này khác với encryption in transit.

| Loại encryption | Bảo vệ khi nào? |
| --- | --- |
| Encryption in transit | Khi dữ liệu đang truyền qua network |
| Encryption at rest | Khi dữ liệu nằm trên disk/storage |

Backup production thường chứa dữ liệu rất nhạy cảm:

- thông tin user
- đơn hàng
- thanh toán
- dữ liệu nội bộ
- audit log
- dữ liệu cá nhân

Nếu backup bị copy ra ngoài, người có file backup có thể restore database và đọc dữ liệu. Vì vậy backup cần được bảo vệ giống hoặc nghiêm hơn database production.

Trong MariaDB, cần phân biệt hai lớp encryption:

- **Data-at-rest encryption**: dữ liệu được mã hóa khi nằm trên disk của database server.
- **Data-in-transit encryption**: dữ liệu được mã hóa khi truyền qua network, thường dùng TLS.

Khi MariaDB Server bật data-at-rest encryption, physical backup có thể copy các tablespace đã được mã hóa từ disk. Điều này giúp backup không chứa dữ liệu plaintext ở mức file tablespace. Tuy nhiên, vẫn cần quản lý key cẩn thận, vì restore sẽ cần key phù hợp để đọc dữ liệu.

Nếu backup truyền qua network, ví dụ backup server kết nối tới MariaDB Server từ máy khác, nên bật TLS cho connection backup. Khi đó dữ liệu và metadata trao đổi giữa `mariadb-backup` và MariaDB Server được bảo vệ trong quá trình truyền.

Ví dụ cấu hình TLS khi chạy backup:

```bash
mariadb-backup \
  --backup \
  --target-dir=/data/backups/full \
  --user=mariadb-backup \
  --password=mbu_passwd \
  --ssl-ca=/etc/my.cnf.d/certs/ca.pem \
  --ssl-cert=/etc/my.cnf.d/certs/client-cert.pem \
  --ssl-key=/etc/my.cnf.d/certs/client-key.pem
```

Ý nghĩa:

| Option | Ý nghĩa |
| --- | --- |
| `--ssl-ca` | CA certificate dùng để verify server certificate |
| `--ssl-cert` | Client certificate dùng cho TLS client authentication nếu cấu hình yêu cầu |
| `--ssl-key` | Private key tương ứng với client certificate |

Backup strategy nên xem encryption là yêu cầu độc lập, không phải tính năng phụ. Ngay cả khi production database không bật data-at-rest encryption, backup vẫn có thể cần mã hóa vì backup thường được copy ra khỏi server chính, lưu lâu hơn, và có nhiều người hoặc hệ thống truy cập hơn.

## Lưu ý về key

Encryption chỉ có ý nghĩa nếu key được quản lý đúng.

Không nên để:

```text
backup file và encryption key ở cùng một nơi không được bảo vệ
```

Nên:

- Lưu key tách biệt với backup.
- Giới hạn quyền đọc key.
- Có quy trình rotate key.
- Ghi nhận ai được quyền truy cập key.
- Test restore định kỳ để chắc chắn key còn dùng được.

---

## Non-blocking backups

Non-blocking backup là cơ chế backup hạn chế ảnh hưởng tới workload đang chạy.

Khi `mariadb-backup` kết nối vào MariaDB Server, nó thực hiện các thao tác staging để bảo vệ dữ liệu trong lúc đọc file. Mục tiêu là giảm thời gian lock và giảm ảnh hưởng đến ứng dụng.

Điểm cần hiểu:

- Backup vẫn có thể gây tải I/O.
- Backup vẫn có thể ảnh hưởng performance nếu database lớn.
- Nhưng nó cố gắng tránh khóa toàn bộ hệ thống trong thời gian dài.

So với cách cũ dùng `FLUSH TABLES WITH READ LOCK`, non-blocking backup giúp giảm thời gian ứng dụng bị chặn write.

---

## Understanding recovery

Physical backup không phải lúc nào cũng có thể restore ngay sau khi copy file.

Lý do là trong lúc backup:

- MariaDB vẫn có thể đang ghi dữ liệu.
- Một số transaction đã commit.
- Một số transaction chưa commit.
- Một số thay đổi nằm trong redo log hoặc undo log.
- Các file được copy tại các thời điểm hơi khác nhau.

Vì vậy cần bước `--prepare` để đưa backup về trạng thái nhất quán.

## Có cần prepare trước khi backup không?

Không. `--prepare` không chạy trước backup, vì lúc đó chưa có bản backup để xử lý.

Luồng đúng của `mariadb-backup` là:

```text
1. --backup: copy data files và các log cần thiết ra thư mục backup
2. --prepare: xử lý thư mục backup đó để nó nhất quán
3. --copy-back/--move-back: restore thư mục đã prepare vào data directory
```

Câu hỏi thường gặp là: nếu lúc backup, data còn nằm trong RAM và chưa flush xuống data file thì sao?

Với InnoDB, khi transaction commit, dữ liệu không nhất thiết được ghi ngay toàn bộ vào data file. Một phần thay đổi có thể đang ở:

```text
buffer pool trong RAM
redo log
undo log
data file / tablespace
```

Điểm quan trọng là InnoDB dùng redo log để đảm bảo có thể phục hồi thay đổi đã commit. `mariadb-backup` không chỉ copy mỗi data file rồi bỏ qua phần còn lại; nó còn lấy các log/metadata cần thiết trong lúc backup. Sau đó `--prepare` dùng các thông tin này để hoàn tất bản backup.

Vì vậy không cần "prepare database live" trước backup. Thứ cần prepare là **bản backup thô** sau khi đã được tạo.

Nói ngắn:

```text
Data chưa flush hết xuống data file -> backup vẫn có thể lấy trạng thái cần thiết qua log.
Backup thô có thể chưa nhất quán -> --prepare xử lý backup thô thành bản nhất quán.
```

---

## Vì sao cần `--prepare` trước khi restore?

`--prepare` là bước làm cho physical backup có thể restore an toàn.

Hiểu đơn giản:

```text
Backup thô -> --prepare -> Backup nhất quán -> Restore
```

Với InnoDB, bước prepare tương tự quá trình crash recovery, nhưng không nên hiểu quá đơn giản là "chạy lại SQL transaction". `mariadb-backup` đang làm việc ở mức physical file, nên prepare chủ yếu xử lý trạng thái của data pages, redo log, undo log và tablespace trong bản backup.

Khi backup đang chạy, InnoDB có thể có nhiều trạng thái cùng lúc:

```text
1. Một số data page đã được ghi ra tablespace.
2. Một số thay đổi mới chỉ có trong redo log.
3. Một số transaction đã commit nhưng page tương ứng chưa được flush hết.
4. Một số transaction chưa commit vẫn để lại dấu vết trong data page/undo log.
5. Các file trong backup có thể được copy tại các thời điểm khác nhau.
```

Nếu restore ngay bản backup thô, InnoDB có thể thấy một tập file chưa khớp hoàn toàn với nhau. Vì vậy `--prepare` làm các việc như:

- Đọc metadata và log đi kèm bản backup.
- Apply redo log cần thiết để đưa data pages tới trạng thái nhất quán.
- Dùng undo information để xử lý các thay đổi chưa commit.
- Đồng bộ trạng thái tablespace trong thư mục backup.
- Tạo ra một bản backup có thể được MariaDB mở lên như một data directory hợp lệ.

Nói ngắn hơn:

```text
--prepare không phải restore dữ liệu vào server.
--prepare là làm sạch và hoàn tất bản physical backup để nó trở thành một data directory nhất quán.
```

Nếu bỏ qua `--prepare`, restore có thể gặp lỗi hoặc MariaDB có thể crash để bảo vệ dữ liệu.

## Scheduling restore preparation

Vì `--prepare` có thể tốn thời gian, có thể chạy prepare trước khi thật sự cần restore. Đây là cách tối ưu RTO: khi sự cố xảy ra, backup đã ở trạng thái gần sẵn sàng để restore hơn.

Ví dụ:

```text
Backup tạo lúc 00:00
Prepare chạy sau khi backup hoàn tất
Khi sự cố xảy ra lúc 08:00, không cần chờ prepare từ đầu
```

Lợi ích:

- Giảm thời gian restore thực tế.
- Giúp đạt RTO tốt hơn.
- Phát hiện sớm backup lỗi vì prepare fail trước khi có sự cố.

Đánh đổi:

- Tốn thêm CPU/I/O sau khi backup.
- Nếu prepare full backup và apply incremental sớm, cần quản lý cẩn thận bản đã merge và bản incremental gốc.
- Có thể tốn thêm storage nếu muốn giữ cả bản backup chưa merge và bản đã prepare.

Với incremental backup, có thể apply incremental vào full backup trước để rút ngắn thời gian recovery. Tuy nhiên nếu cần giữ nhiều recovery point, không nên làm mất các bản incremental gốc trước khi chắc chắn retention và restore policy cho phép.

## Prepare full backup

```bash
mariadb-backup \
  --prepare \
  --target-dir=/backup/full
```

Sau bước này, `/backup/full` có thể dùng để restore.

## Prepare incremental backup

Với incremental backup, phải apply từng incremental vào full backup theo đúng thứ tự.

Ví dụ có:

```text
/backup/full
/backup/inc1
/backup/inc2
```

Apply `inc1`:

```bash
mariadb-backup \
  --prepare \
  --target-dir=/backup/full \
  --incremental-dir=/backup/inc1
```

Apply `inc2`:

```bash
mariadb-backup \
  --prepare \
  --target-dir=/backup/full \
  --incremental-dir=/backup/inc2
```

Prepare lần cuối:

```bash
mariadb-backup \
  --prepare \
  --target-dir=/backup/full
```

Kết quả là thư mục `/backup/full` đã chứa dữ liệu sau khi apply incremental và sẵn sàng restore.

Thứ tự đúng:

```text
full -> inc1 -> inc2 -> inc3
```

Thứ tự sai:

```text
full -> inc2 -> inc1
full -> inc3
```

---

## Restore requires empty data directory

Khi restore bằng `mariadb-backup`, data directory của MariaDB thường phải trống.

Data directory thường là:

```text
/var/lib/mysql
```

Trong đó có thể chứa:

```text
ibdata files
redo log
undo log
tablespace
database directories
system tables
metadata files
```

Physical restore là đưa nguyên bộ file dữ liệu đã backup vào data directory. Nếu thư mục này còn file cũ, MariaDB có thể rơi vào trạng thái trộn lẫn:

```text
file cũ
+ file restore từ backup
= metadata và tablespace không khớp
```

Hậu quả có thể là:

- MariaDB không start được.
- InnoDB báo lỗi tablespace.
- Dữ liệu bị thiếu hoặc sai.
- System tables không khớp.
- Server start được nhưng phát sinh lỗi sau đó.

Quy trình restore thường là:

```text
1. Stop MariaDB
2. Đảm bảo backup đã được prepare
3. Xử lý data directory hiện tại một cách an toàn
4. Làm trống data directory
5. copy-back hoặc move-back
6. Set owner/permission
7. Start MariaDB
8. Kiểm tra dữ liệu
```

## `--copy-back` và `--move-back`

Restore bằng `--copy-back`:

```bash
mariadb-backup \
  --copy-back \
  --target-dir=/backup/full
```

Restore bằng `--move-back`:

```bash
mariadb-backup \
  --move-back \
  --target-dir=/backup/full
```

Khác nhau:

| Option | Ý nghĩa |
| --- | --- |
| `--copy-back` | Copy file từ backup vào data directory, backup gốc vẫn còn |
| `--move-back` | Move file từ backup vào data directory, backup gốc bị chuyển đi |

Thực tế thường ưu tiên `--copy-back` nếu đủ dung lượng, vì giữ lại bản backup gốc an toàn hơn.

`--move-back` chỉ nên cân nhắc khi:

- Data directory và backup directory nằm trên cùng filesystem/partition nên thao tác move có thể nhanh hơn copy.
- Server không đủ dung lượng để vừa giữ backup vừa copy thêm một bản vào data directory.
- Đã có thêm một bản backup khác ở vị trí an toàn.

Rủi ro của `--move-back`:

- Nội dung backup directory bị di chuyển đi.
- Nếu restore lỗi giữa chừng và không còn bản backup khác, khả năng xử lý sự cố kém hơn.
- Không nên dùng như lựa chọn mặc định trong production nếu chưa có bản backup thứ hai.

Ví dụ:

```bash
mariadb-backup \
  --move-back \
  --target-dir=/data/backups/full
```

Cách hiểu ngắn:

```text
--copy-back: an toàn hơn, tốn thêm dung lượng và thời gian copy
--move-back: tiết kiệm dung lượng/có thể nhanh hơn, nhưng làm mất bản backup tại chỗ cũ
```

Sau restore cần sửa permission:

```bash
chown -R mysql:mysql /var/lib/mysql
```

---

## Creating the backup user

Khi backup, `mariadb-backup` cần kết nối vào MariaDB Server đang chạy để quản lý lock, đọc trạng thái server và lấy metadata.

Nên tạo user riêng cho backup:

```sql
CREATE USER 'mariabackup'@'localhost' IDENTIFIED BY 'mbu_passwd';

GRANT RELOAD, PROCESS, LOCK TABLES, BINLOG MONITOR
ON *.*
TO 'mariabackup'@'localhost';
```

Ý nghĩa quyền:

| Quyền | Ý nghĩa |
| --- | --- |
| `RELOAD` | Cho phép flush logs/tables khi cần |
| `PROCESS` | Xem process/thread đang chạy |
| `LOCK TABLES` | Khóa table khi cần trong backup |
| `BINLOG MONITOR` | Đọc trạng thái binary log phục vụ backup metadata |

Restore không cần user này vì restore thường chạy khi MariaDB Server đã stop.

---

## `mariadb-dump`

`mariadb-dump` là công cụ logical backup, trước đây thường quen gọi là `mysqldump`.

Nó xuất dữ liệu ra SQL format. File SQL này có thể import lại vào MariaDB, MySQL hoặc DBMS khác nếu cú pháp tương thích.

Backup một database:

```bash
mariadb-dump db_name > backup-file.sql
```

Restore:

```bash
mariadb db_name < backup-file.sql
```

`mariadb-dump` thường dump:

- table structure
- data
- triggers

Một số object cần option riêng:

- stored procedures
- functions
- events
- routines

Ví dụ:

```bash
mariadb-dump \
  --routines \
  --events \
  db_name > backup-file.sql
```

`mariadb-dump` phù hợp khi database nhỏ hoặc cần migrate, nhưng không tối ưu cho database rất lớn.

---

## InnoDB logical backups

InnoDB dùng buffer pool để cache data và index trong RAM. Buffer pool rất quan trọng cho performance.

Khi chạy logical backup bằng `mariadb-dump`, MariaDB có thể phải scan toàn bộ table. Việc scan này có thể đẩy dữ liệu thường dùng ra khỏi buffer pool, làm cache bị "ô nhiễm" bởi dữ liệu chỉ được đọc một lần trong lúc backup.

Hậu quả:

- Query production sau backup có thể chậm hơn tạm thời.
- Buffer pool mất bớt dữ liệu hot.
- Server cần thời gian để warm cache lại.

Một số cách giảm ảnh hưởng:

- Điều chỉnh `innodb_old_blocks_time`.
- Điều chỉnh `innodb_old_blocks_pct`.
- Dump buffer pool trước backup.
- Load lại buffer pool sau backup.

Ví dụ:

```sql
SET GLOBAL innodb_buffer_pool_dump_now = ON;
SET GLOBAL innodb_buffer_pool_load_now = ON;
```

---

## `mariadb-hotcopy`

`mariadb-hotcopy` là công cụ physical backup cũ.

Giới hạn quan trọng:

- Chỉ phù hợp MyISAM và ARCHIVE.
- Chỉ chạy trên cùng máy với database directory.
- Không phù hợp cho InnoDB hiện đại.

Ví dụ:

```bash
mariadb-hotcopy db_name /path/to/new_directory
```

Ngày nay, với production dùng InnoDB, nên ưu tiên `mariadb-backup`.

---

## Percona XtraBackup

Percona XtraBackup là công cụ hot backup phổ biến trong hệ sinh thái MySQL/Percona.

Tuy nhiên với MariaDB, nên dùng:

```text
mariadb-backup
```

Lý do:

- MariaDB và MySQL/Percona có khác biệt về storage engine, redo log, encryption, compression và metadata.
- `mariadb-backup` được MariaDB hỗ trợ phù hợp hơn cho MariaDB Server.

---

## Filesystem snapshots

Filesystem snapshot là bản chụp nhanh trạng thái của filesystem hoặc volume tại một thời điểm. Nếu MariaDB lưu dữ liệu ở `/var/lib/mysql`, snapshot có thể tạo ra một bản nhìn giống như `/var/lib/mysql` tại thời điểm snapshot được tạo.

## Snapshot dùng để làm gì trong MariaDB?

Snapshot có ý nghĩa chính là tạo một điểm chụp nhanh ở tầng storage để giảm thời gian tác động lên MariaDB production.

Nếu copy trực tiếp data directory khi MariaDB đang chạy, quá trình copy có thể kéo dài rất lâu:

```text
Database 1 TB
Copy trực tiếp từ /var/lib/mysql có thể mất hàng chục phút hoặc vài giờ
```

Trong thời gian đó, MariaDB vẫn ghi dữ liệu liên tục. File được copy ở đầu quá trình và file được copy ở cuối quá trình có thể không thuộc cùng một thời điểm logic.

Snapshot giúp đổi cách làm:

```text
1. Tạo snapshot rất nhanh tại một thời điểm
2. MariaDB tiếp tục chạy
3. Copy backup từ snapshot, không copy trực tiếp từ live volume
```

Lợi ích:

- Giảm thời gian phải lock/flush database.
- Có một ảnh chụp ổn định hơn để copy backup.
- Tránh copy trực tiếp từ data directory đang thay đổi liên tục.
- Cho phép quá trình copy backup chạy lâu mà không giữ lock lâu trên production.

Nhưng snapshot chỉ là điểm chụp ở tầng storage. Để trở thành backup đúng nghĩa, dữ liệu từ snapshot vẫn nên được copy sang backup storage riêng và phải được test restore.

Điểm mạnh của snapshot là tạo rất nhanh. Nhiều hệ thống dùng cơ chế **copy-on-write**: ban đầu snapshot không copy toàn bộ dữ liệu, mà chỉ giữ lại block cũ khi production bắt đầu ghi đè block đó.

Ví dụ:

```text
10:00 production volume có:
Block 1 = A
Block 2 = B
Block 3 = C

10:00 tạo snapshot S1:
Storage chưa copy toàn bộ A/B/C.
Nó chỉ ghi metadata rằng S1 đại diện cho trạng thái lúc 10:00.

10:05 production muốn đổi Block 2 từ B thành X:
1. Storage copy giá trị cũ B sang vùng snapshot.
2. Storage ghi X vào Block 2 của production volume.

Sau đó:
Production thấy: A X C
Snapshot S1 thấy: A B C
```

Khi đọc snapshot, storage ghép dữ liệu từ hai nơi:

```text
Block chưa đổi sau snapshot -> đọc từ production volume
Block đã đổi sau snapshot -> đọc bản cũ trong vùng snapshot
```

Vì vậy snapshot tạo nhanh: nó không copy toàn bộ volume ngay lập tức, mà chỉ copy các block cũ khi chúng sắp bị ghi đè. Đây là lý do snapshot có thể phình to dần nếu production ghi nhiều sau khi snapshot được tạo.

Tuy nhiên snapshot không tự động là backup MariaDB an toàn. MariaDB là hệ thống có transaction, redo log, undo log, buffer pool và file tablespace. Snapshot chỉ thấy block trên disk, không hiểu transaction nào đã commit, transaction nào chưa commit, page nào đang được flush, hay redo log nào còn cần apply.

## Crash-consistent nghĩa là gì?

Nếu snapshot được tạo đúng lúc MariaDB đang ghi dở, snapshot có thể chỉ ở trạng thái **crash-consistent**. Nghĩa là bản snapshot giống trạng thái disk nếu máy bị mất điện đột ngột tại đúng thời điểm đó.

Ví dụ một transaction đang commit:

```text
Transaction T cập nhật orders và payments.

Tại thời điểm snapshot:
- Một số page của orders đã được ghi xuống disk.
- Một số page của payments chưa kịp ghi xuống disk.
- Redo log có thể đã chứa thông tin cần replay.
- Undo log có thể vẫn cần dùng nếu transaction chưa hoàn tất.
```

Ở tầng storage, snapshot chỉ chụp các block tại thời điểm đó. Nó không biết `orders` và `payments` thuộc cùng một transaction nghiệp vụ. Khi restore từ snapshot, MariaDB/InnoDB có thể phải chạy crash recovery để đọc redo/undo log và đưa database về trạng thái hợp lệ.

Crash-consistent không có nghĩa là chắc chắn hỏng. Nó có nghĩa là:

```text
Snapshot nhất quán ở tầng block/storage,
nhưng chưa chắc đã là trạng thái sạch ở tầng database application.
```

Với InnoDB, crash recovery thường xử lý được nhiều trường hợp crash-consistent. Nhưng rủi ro vẫn cao hơn backup được tạo bằng công cụ database-aware hoặc snapshot được phối hợp đúng với MariaDB.

Với InnoDB, crash recovery có thể xử lý nhiều tình huống, nhưng backup tốt hơn nên cố gắng đạt trạng thái **application-consistent**, tức là MariaDB được flush/lock phù hợp trước khi chụp snapshot.

Quy trình cơ bản:

```text
1. Trong MariaDB client, chạy FLUSH TABLES WITH READ LOCK
2. Giữ session đó mở
3. Tạo filesystem snapshot
4. Chạy UNLOCK TABLES
5. Mount snapshot nếu cần
6. Copy dữ liệu từ snapshot sang backup storage riêng
7. Unmount hoặc xóa snapshot khi xong
```

Ví dụ SQL:

```sql
FLUSH TABLES WITH READ LOCK;
UNLOCK TABLES;
```

Điểm quan trọng:

- Session chạy `FLUSH TABLES WITH READ LOCK` phải được giữ mở cho tới khi tạo snapshot xong.
- Snapshot nên được tạo thật nhanh rồi unlock ngay để giảm thời gian block write.
- Snapshot nên được copy ra backup storage riêng; nếu chỉ để snapshot cùng storage với production thì storage hỏng vẫn mất cả database lẫn snapshot.
- Snapshot phải được test restore, không nên chỉ tin rằng snapshot tạo thành công là backup dùng được.

Storage snapshot thường được xem là một kỹ thuật backup optimization vì nó giảm thời gian tác động trực tiếp lên production. Thay vì copy hàng trăm GB hoặc vài TB từ live data directory trong lúc database đang chạy, ta tạo snapshot nhanh, rồi copy dữ liệu từ snapshot.

Trade-off:

- Chỉ dùng được nếu SAN, NAS, filesystem hoặc volume manager hỗ trợ snapshot.
- Snapshot ở tầng storage không hiểu transaction của MariaDB.
- Bản restore từ snapshot có thể cần crash recovery.
- Vẫn phải test recovery từ snapshot định kỳ như với full/incremental backup.

Với MariaDB có hỗ trợ staged backup, quy trình snapshot có thể dùng các stage để giảm ảnh hưởng:

```text
1. Chạy BACKUP STAGE START
2. Chạy BACKUP STAGE BLOCK_COMMIT
3. Tạo snapshot
4. Chạy BACKUP STAGE END
5. Copy/store snapshot theo cơ chế của storage platform
```

Trong một số trường hợp, khi có DDL như `ALTER TABLE` trong lúc staged backup, MariaDB có thể tạo file tạm có prefix như `#sql`. Sau khi backup hoàn tất, cần kiểm tra hướng dẫn version đang dùng để xử lý các file tạm này đúng cách.

---

## LVM snapshots

LVM snapshot là snapshot ở tầng block device của Linux Logical Volume Manager. Nếu data directory của MariaDB nằm trên một logical volume, có thể tạo snapshot của volume đó rồi copy dữ liệu từ snapshot thay vì copy trực tiếp từ volume production.

Ví dụ minh họa:

```bash
lvcreate --snapshot \
  --name mysql_snap \
  --size 50G \
  /dev/vg0/mysql
```

Sau đó có thể mount snapshot:

```bash
mount /dev/vg0/mysql_snap /mnt/mysql_snap
```

Rồi copy dữ liệu:

```bash
rsync -a /mnt/mysql_snap/ /backup/mysql-2026-05-18/
```

Điểm cần nhớ nhất:

```text
LVM snapshot không phải standalone DBMS backup solution.
```

Lý do:

- LVM hoạt động ở block level, không hiểu transaction của MariaDB.
- Nếu không flush/lock đúng cách, snapshot có thể chỉ crash-consistent.
- Nếu snapshot bắt đúng lúc ghi dở, có thể gặp torn pages hoặc trạng thái không nhất quán.
- Copy-on-write làm write path nặng hơn, có thể ảnh hưởng performance production.
- Snapshot có thể đầy nếu production ghi nhiều trong lúc snapshot còn tồn tại.
- Snapshot thường nằm cùng hạ tầng storage, nên không bảo vệ khỏi lỗi storage vật lý nếu không copy ra nơi khác.

Tham số `--size 50G` trong ví dụ không có nghĩa là database chỉ được 50 GB. Nó là dung lượng dành cho vùng lưu các block cũ bị thay đổi sau khi snapshot được tạo. Nếu lượng block thay đổi vượt quá dung lượng này, snapshot có thể invalid và backup từ snapshot có thể không dùng được.

Quy trình LVM snapshot an toàn hơn cho MariaDB:

```text
1. Mở session MariaDB
2. Chạy FLUSH TABLES WITH READ LOCK
3. Tạo LVM snapshot thật nhanh
4. Chạy UNLOCK TABLES
5. Mount snapshot
6. Copy dữ liệu từ snapshot sang backup storage riêng
7. Unmount snapshot
8. Xóa snapshot
9. Test restore
```

Sau khi copy xong nên xóa snapshot sớm:

```bash
umount /mnt/mysql_snap
lvremove /dev/vg0/mysql_snap
```

So với `mariadb-backup`, LVM snapshot kém "database-aware" hơn. Nó hữu ích khi muốn giảm thời gian khóa database và storage hỗ trợ snapshot nhanh, nhưng không nên thay thế hoàn toàn `mariadb-backup` nếu mục tiêu là backup MariaDB production an toàn, có quy trình prepare/restore rõ ràng.

Cách hiểu đúng:

```text
Snapshot = cơ chế chụp nhanh dữ liệu ở tầng storage
Backup = bản dữ liệu có thể restore, lưu ở nơi an toàn, đã được test
```

---

## dbForge Studio for MySQL

Ngoài các command-line tools, có thể dùng GUI tool như dbForge Studio for MySQL để backup và restore.

Ưu điểm:

- Có giao diện trực quan.
- Có wizard hướng dẫn backup/restore.
- Có thể cấu hình full hoặc partial backup.
- Có thể scheduling backup.
- Có log lỗi.
- Có thể lưu cấu hình để dùng lại.

Nhược điểm:

- Phụ thuộc tool bên thứ ba.
- Cần kiểm tra compatibility với MariaDB version đang dùng.
- Với production lớn, vẫn nên hiểu rõ cơ chế backup bên dưới.

---

## Backup storage considerations

Cách lưu backup ảnh hưởng trực tiếp tới khả năng restore. Backup có thể tạo thành công nhưng vẫn không hữu ích nếu storage chậm, không an toàn, bị mất cùng production, hoặc không đáp ứng RTO/RPO.

Nguyên tắc quan trọng:

```text
Backup phải được lưu tách biệt với hệ thống đang được backup.
```

Không nên chỉ để backup trên cùng server MariaDB. Nếu server hoặc disk hỏng, cả database và backup có thể mất cùng lúc.

Backup storage nên được thiết kế theo các tiêu chí:

- Tách biệt khỏi production database server.
- Tách biệt khỏi hệ thống dùng để recovery nếu có thể.
- Có quyền truy cập được kiểm soát chặt chẽ.
- Có encryption nếu chứa dữ liệu nhạy cảm.
- Có đủ tốc độ đọc để restore trong RTO.
- Có đủ dung lượng theo retention policy.
- Có monitoring về dung lượng, lỗi ghi, lỗi đọc và tuổi đời backup.

## Onsite và offsite backup

Có thể cần cả onsite và offsite backup.

| Loại storage | Mục đích |
| --- | --- |
| Onsite backup | Restore nhanh, giúp đạt RTO |
| Offsite backup | Bảo vệ khi mất toàn bộ site/data center, giúp đạt RPO trong thảm họa lớn |

Ví dụ:

```text
Onsite backup:
- Lưu trên backup server cùng data center
- Restore nhanh
- Hữu ích khi database server hỏng

Offsite backup:
- Lưu ở data center khác hoặc object storage
- Chống mất toàn bộ site
- Có thể restore chậm hơn do network/download
```

Nếu business yêu cầu RTO thấp, chỉ lưu backup ở offsite storage chậm có thể không đủ. Ngược lại, chỉ lưu onsite backup thì không đủ an toàn khi có sự cố lớn ở toàn bộ hạ tầng.

## Retention và capacity planning

Retention là thời gian giữ backup.

Ví dụ:

```text
Giữ backup hằng ngày trong 14 ngày
Giữ backup hằng tuần trong 8 tuần
Giữ backup hằng tháng trong 12 tháng
```

Retention cần dựa trên:

- Yêu cầu business.
- Yêu cầu pháp lý/compliance.
- Tốc độ tăng dữ liệu.
- Dung lượng backup sau compression.
- Số lượng full backup.
- Số lượng incremental backup.
- Chi phí storage.

Capacity planning không chỉ tính dung lượng database hiện tại. Cần tính cả tốc độ tăng trưởng.

Ví dụ:

```text
Database hiện tại: 1 TB
Tăng trưởng: 50 GB/ngày
Retention: 30 ngày
Full backup mỗi tuần
Incremental backup mỗi ngày
```

Nếu không tính trước, backup storage có thể đầy trước khi hệ thống phát hiện, làm backup fail và phá vỡ RPO.

---

## Backup testing

Backup chưa được test restore thì chưa thể xem là đáng tin cậy.

Một lỗi rất phổ biến là chỉ kiểm tra job backup có exit code thành công, nhưng không kiểm tra bản backup có restore được hay không.

Backup testing nên kiểm tra:

- File backup có tồn tại không?
- Backup có đủ metadata không?
- `--prepare` có chạy thành công không?
- Restore vào môi trường test có thành công không?
- MariaDB có start được sau restore không?
- Dữ liệu critical có đọc được không?
- Application có kết nối được không?
- Thời gian restore thực tế có đạt RTO không?

Ví dụ checklist test restore:

```text
1. Lấy bản backup gần nhất
2. Prepare backup
3. Restore vào test server
4. Start MariaDB
5. Chạy sanity check dữ liệu
6. Chạy một số query nghiệp vụ quan trọng
7. Ghi nhận thời gian restore
8. So sánh với RTO
```

Backup testing nên được tự động hóa càng nhiều càng tốt. Nếu không thể tự động hóa hoàn toàn, vẫn nên có lịch drill định kỳ.

Drill giúp phát hiện các vấn đề như:

- Thiếu quyền truy cập backup storage.
- Thiếu encryption key.
- Runbook restore sai command.
- Backup quá chậm để đạt RTO.
- File backup bị corruption.
- Team không biết chính xác ai chịu trách nhiệm bước nào.

---

## Restore workflow tổng quát

Với `mariadb-backup`, quy trình restore thường là:

```text
1. Có full backup
2. Nếu có incremental backup, apply incremental theo đúng thứ tự
3. Chạy --prepare
4. Stop MariaDB
5. Làm trống data directory
6. Chạy --copy-back hoặc --move-back
7. Set lại owner/permission
8. Start MariaDB
9. Kiểm tra dữ liệu
```

Ví dụ:

```bash
mariadb-backup \
  --prepare \
  --target-dir=/backup/full
```

```bash
systemctl stop mariadb
```

```bash
mariadb-backup \
  --copy-back \
  --target-dir=/backup/full
```

```bash
chown -R mysql:mysql /var/lib/mysql
```

```bash
systemctl start mariadb
```

```bash
mariadb -e "SHOW DATABASES;"
```

---

## Tóm tắt

| Khái niệm | Cách hiểu |
| --- | --- |
| Logical backup | Backup thành SQL, linh hoạt, dễ migrate |
| Physical backup | Backup file dữ liệu thật, nhanh hơn với database lớn |
| Data inventory | Kiểm kê dữ liệu để biết cần backup gì và giữ bao lâu |
| RPO | Lượng dữ liệu tối đa business chấp nhận mất |
| RTO | Thời gian tối đa business muốn khôi phục dịch vụ |
| Replication | Giảm ảnh hưởng backup lên primary, nhưng không thay thế backup |
| Backup replica | Replica chuyên dùng để chạy backup, giúp tách tải khỏi primary |
| Replication lag | Độ trễ replica so với primary, ảnh hưởng recovery point thực tế |
| Backup optimization | Tối ưu backup/restore nhưng luôn có trade-off |
| `--parallel` | Chạy backup nhiều luồng để tận dụng CPU/I/O song song |
| Full backup | Backup đầy đủ dữ liệu trong phạm vi chọn |
| Incremental backup | Chỉ backup phần thay đổi từ backup trước |
| Partial backup | Chỉ backup một phần database/table |
| Compression | Nén backup để giảm dung lượng |
| Encryption-at-rest | Mã hóa backup khi nằm trên disk/storage |
| Backup storage | Nơi lưu backup phải tách biệt, đủ an toàn và đủ nhanh để restore |
| Backup testing | Kiểm tra backup có restore được và đạt RTO/RPO không |
| Non-blocking backup | Giảm ảnh hưởng lock trong lúc backup |
| `--prepare` | Làm physical backup nhất quán trước restore |
| Scheduled prepare | Prepare backup sớm để giảm RTO, đổi lại tốn tài nguyên/storage |
| Empty data directory | Tránh trộn file cũ với file restore |
| `--move-back` | Move backup vào data directory, tiết kiệm dung lượng nhưng làm thay đổi backup gốc |

Nếu chỉ nhớ một câu:

```text
Với mariadb-backup: tạo backup xong chưa restore ngay; phải prepare trước, rồi mới copy-back hoặc move-back vào data directory trống.
```
