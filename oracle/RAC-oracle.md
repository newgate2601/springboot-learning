# Kiến thức Oracle RAC

## 1. RAC là gì?

Oracle RAC, viết tắt của Real Application Clusters, là công nghệ cho phép nhiều Oracle Database Instance cùng mở và phục vụ một database vật lý duy nhất.

Mô hình tổng quát:

```text
Backend App
   |
   v
SCAN / Listener / Service Name
   |
   +--> RAC Instance 1 trên Node 1
   +--> RAC Instance 2 trên Node 2
   +--> RAC Instance 3 trên Node 3
              |
              v
        Shared Storage / ASM
              |
              v
          Data files chung
```

Cần phân biệt rõ:

- `Node`: máy chủ vật lý hoặc VM trong cluster.
- `Instance`: tập hợp process và memory Oracle chạy trên từng node.
- `Database`: tập hợp datafile, control file, redo log, undo, metadata.
- `RAC`: nhiều instance cùng truy cập một database chung.

RAC không phải là replication kiểu mỗi node có một bản data riêng. RAC là mô hình `shared-disk`: nhiều instance, một database vật lý chung.

## 2. Backend app kết nối vào RAC như thế nào?

Backend app không nên trỏ cứng vào một node cụ thể.

Ví dụ cấu hình rủi ro:

```text
jdbc:oracle:thin:@//rac-node-1.company.com:1521/order_service
```

Nếu `rac-node-1` hoặc listener trên node đó chết, app không tạo được connection mới, dù các node RAC khác vẫn sống.

Cấu hình đúng hơn là connect qua SCAN:

```text
jdbc:oracle:thin:@//rac-scan.company.com:1521/order_service
```

`SCAN` là Single Client Access Name. Đây là endpoint logic của RAC cluster. Tên này thường resolve ra nhiều IP:

```text
rac-scan.company.com -> 10.0.0.11
rac-scan.company.com -> 10.0.0.12
rac-scan.company.com -> 10.0.0.13
```

Khi app tạo connection mới, Oracle listener có thể route connection đến instance phù hợp còn sống.

Có thể dùng descriptor có `LOAD_BALANCE` và `FAILOVER`:

```text
jdbc:oracle:thin:@(DESCRIPTION=
  (ADDRESS_LIST=
    (LOAD_BALANCE=ON)
    (FAILOVER=ON)
    (ADDRESS=(PROTOCOL=TCP)(HOST=rac-scan.company.com)(PORT=1521))
  )
  (CONNECT_DATA=
    (SERVICE_NAME=order_service)
  )
)
```

Điểm quan trọng:

- Fix cứng URL vào `rac-scan.company.com/order_service` là bình thường.
- Fix cứng URL vào `rac-node-1.company.com` là tự tạo single point of failure.
- Listener chủ yếu dùng khi tạo connection mới.
- Connection đã mở sẵn thường không đi qua listener nữa khi thực thi SQL.

## 3. Data file nằm ở đâu?

Trong RAC, datafile không nằm riêng trên từng node.

Datafile thường nằm trên shared storage được tất cả node cùng truy cập, thường quản lý bằng ASM:

```text
Node 1: Instance 1
Node 2: Instance 2
Node 3: Instance 3
       |
       v
ASM Disk Group / Shared Storage
       |
       v
+DATA/db/system01.dbf
+DATA/db/users01.dbf
+DATA/db/undotbs01.dbf
+DATA/db/redo...
```

Mỗi instance có memory riêng, process riêng, buffer cache riêng. Nhưng datafile vật lý là chung.

Nếu shared storage thực sự chết hoàn toàn, RAC không cứu được database. RAC bảo vệ tốt khi mất instance/node, còn storage layer phải có HA riêng.

## 4. Shared storage và Exadata có phải single point of failure không?

Nếu thiết kế shared storage chỉ là một máy duy nhất thì nó là single point of failure.

Trong production, storage layer phải có redundancy:

- nhiều disk;
- nhiều path;
- nhiều controller;
- ASM mirroring;
- failure group;
- multipath;
- trên Exadata thì có nhiều storage cell.

ASM có các kiểu redundancy phổ biến:

- `NORMAL REDUNDANCY`: mirror 2 bản.
- `HIGH REDUNDANCY`: mirror 3 bản.

Trên Exadata, data được phân bổ và mirror qua nhiều storage cell. Nếu một storage cell chết:

- database thường vẫn chạy nếu redundancy còn đủ;
- ASM đọc từ bản mirror ở cell khác;
- hệ thống mất một phần I/O capacity;
- app có thể chậm hơn nếu workload đang nặng;
- ASM có thể rebalance dữ liệu.

Nếu quá nhiều storage cell chết vượt quá khả năng redundancy, database có thể lỗi I/O hoặc dừng.

## 5. Interconnect là gì?

Interconnect là mạng riêng giữa các RAC node.

Nó dùng cho:

- heartbeat giữa node;
- cluster membership;
- Cache Fusion;
- chuyển database block giữa buffer cache của các instance;
- đồng bộ lock/resource toàn cluster.

Mô hình:

```text
RAC Instance 1 <==== Interconnect ====> RAC Instance 2
      |                                      |
      +=========== Shared Storage ==========+
```

Interconnect phải có latency thấp và bandwidth cao. Nếu interconnect chậm, RAC có thể vẫn chạy nhưng hiệu năng giảm mạnh. Nếu interconnect đứt, cluster có thể evict node để tránh split brain.

`Split brain` là tình huống hai nhóm node không nhìn thấy nhau nhưng đều tưởng mình là cluster hợp lệ và cùng ghi vào database. Nếu để xảy ra, có thể gây corruption. Vì vậy Clusterware sẽ giữ một phần cluster và loại bỏ phần còn lại.

## 6. Cache Fusion là gì?

Mỗi RAC instance có buffer cache riêng trong RAM.

Ví dụ:

```text
Instance 1 buffer cache
Instance 2 buffer cache
Instance 3 buffer cache
```

Nhưng database vật lý là chung. Khi nhiều instance cùng cần cùng một database block, Oracle phải điều phối quyền đọc/ghi block đó.

Cache Fusion là cơ chế cho phép Oracle chuyển database block từ buffer cache của instance này sang instance khác qua interconnect, thay vì bắt buộc ghi xuống disk rồi instance khác đọc lại.

Ví dụ đọc block:

```text
Instance 2 cần Block A
Instance 1 đang có Block A trong cache
Instance 1 gửi Block A sang Instance 2 qua interconnect
```

Ví dụ update block:

```text
Instance 1 đang giữ Block A để update
Instance 2 cũng muốn update Block A
Global Cache Service xác định ai đang giữ block
Quyền và current version của block được chuyển sang Instance 2
Instance 2 mới có thể update đúng cách
```

Cache Fusion đảm bảo:

- không đọc dữ liệu cũ sai;
- không ghi đè lung tung;
- transaction isolation vẫn đúng;
- block current version được quản lý đúng;
- dirty block và redo/undo được recovery đúng khi instance chết.

## 7. GCS và GES

RAC có hai nhóm cơ chế quan trọng:

`GCS`, Global Cache Service:

- quản lý database block trên toàn cluster;
- biết block đang ở instance nào;
- biết block đang ở mode nào;
- điều phối chuyển block qua interconnect.

`GES`, Global Enqueue Service:

- quản lý lock/resource toàn cluster;
- điều phối row lock, transaction lock, DDL lock, dictionary lock;
- tránh nhiều instance phá nhau khi truy cập cùng tài nguyên logic.

Hiểu đơn giản:

```text
GCS: quản lý block cache toàn cluster
GES: quản lý lock/resource toàn cluster
```

## 8. Vì sao RAC scale tốt nhất khi workload chia tự nhiên?

RAC scale tốt khi mỗi instance xử lý những phần dữ liệu ít đụng nhau.

Ví dụ tốt:

```text
Instance 1 xử lý customer nhóm A
Instance 2 xử lý customer nhóm B
Instance 3 xử lý customer nhóm C
```

Hoặc:

```text
Instance 1 xử lý workload order
Instance 2 xử lý workload payment
Instance 3 xử lý workload report nhẹ
```

Khi đó, mỗi instance có thể tận dụng cache riêng. Ít block phải chuyển qua lại. Chi phí Cache Fusion thấp.

RAC scale kém khi nhiều instance cùng tranh một tài nguyên nóng.

Ví dụ counter table:

```sql
UPDATE system_counter
SET value = value + 1
WHERE name = 'ORDER_NO';
```

Tất cả request tạo order đều update cùng một row. Row đó nằm trong một block. Block bị chuyển qua lại giữa các instance:

```text
Block A ở Instance 1
-> Instance 2 cần update, block chuyển sang Instance 2
-> Instance 3 cần update, block chuyển sang Instance 3
-> Instance 1 lại cần update, block chuyển lại Instance 1
```

Đây là `block pinging`. Thêm node trong trường hợp này có thể làm chậm hơn vì tăng coordination qua interconnect.

## 9. B-tree index hotspot là gì?

Với B-tree index trên cột tăng dần, các insert mới có xu hướng tập trung vào phần cuối của index, hay gọi là `right-hand side`.

Ví dụ:

```text
ORDER_ID: 1, 2, 3, 4, 5, 6, ...
```

Nhiều instance cùng insert order mới. Các entry index mới cùng dồn về vùng leaf block cuối của index.

Kết quả:

- nhiều instance tranh cùng index leaf block;
- block index bị chuyển qua lại qua Cache Fusion;
- interconnect tăng tải;
- wait event tăng;
- insert chậm.

Một số cách giảm hotspot:

- dùng sequence có cache;
- tránh counter table tự tăng thủ công;
- hash partitioned index;
- reverse key index trong một số trường hợp;
- partition table/index theo tenant, ngày, vùng, hoặc domain tự nhiên;
- route workload có liên quan về cùng một service/instance nếu phù hợp.

`Reverse key index` đảo byte của key để phân tán insert trên nhiều leaf block hơn, giảm right-hand hotspot. Đổi lại, nó không phù hợp cho range scan theo thứ tự tăng dần.

`Hash partitioned index` chia index thành nhiều partition theo hash của key, giúp tránh việc tất cả insert dồn vào cùng một vùng nóng.

## 10. Khi các thành phần RAC bị lỗi

### 10.1. Backend app chết

RAC không bị ảnh hưởng.

Ảnh hưởng nằm ở tầng app:

- request trên backend đó mất;
- connection DB bị đứt;
- transaction chưa commit thường rollback;
- load balancer đưa traffic sang backend khác nếu có.

### 10.2. Một listener chết

Nếu app connect qua SCAN hoặc address list có nhiều endpoint:

- connection mới có thể đi qua listener khác;
- connection cũ thường vẫn chạy;
- service không route vào listener/node lỗi.

Nếu app fix cứng vào một host listener duy nhất:

```text
jdbc:oracle:thin:@//rac-node-1.company.com:1521/order_service
```

thì listener đó chết sẽ làm connection mới lỗi, dù các instance khác vẫn sống.

### 10.3. Toàn bộ listener/SCAN không hoạt động

Connection đã mở có thể vẫn chạy.

Connection mới thường lỗi vì app không tạo được session mới.

### 10.4. Một RAC instance chết

Ví dụ `Instance 2` chết:

```text
Instance 1 còn
Instance 2 chết
Instance 3 còn
```

Xử lý:

- Clusterware phát hiện instance chết;
- service dừng route connection mới vào instance đó;
- session trên instance đó bị mất;
- transaction chưa commit rollback;
- instance còn lại recovery redo/undo cần thiết;
- app tạo connection mới sang instance còn sống.

Request đang chạy trên instance chết thường lỗi. App cần retry nếu thao tác an toàn để retry.

### 10.5. Một physical node chết

Nếu node chết, instance trên node đó cũng chết.

RAC tiếp tục chạy nếu:

- còn node khác;
- cluster còn quorum;
- storage và interconnect còn hoạt động;
- service được cấu hình failover đúng.

### 10.6. Interconnect chậm hoặc đứt

Nếu interconnect chậm:

- Cache Fusion chậm;
- wait event tăng;
- SQL/update có thể chậm mạnh.

Nếu interconnect đứt:

- cluster có nguy cơ split brain;
- Clusterware có thể evict một số node;
- connection tới node bị evict lỗi;
- node còn lại tiếp tục nếu còn quorum và storage ổn.

### 10.7. ASM trên một node chết

ASM quản lý disk group. Nếu ASM instance trên một node chết, database instance trên node đó có thể bị ảnh hưởng và bị dừng.

Nếu ASM trên node khác và disk group vẫn tốt, các instance khác có thể tiếp tục.

### 10.8. Một disk chết

Nếu ASM redundancy còn đủ:

- database vẫn chạy;
- ASM đọc từ mirror;
- ASM có thể rebalance;
- app có thể không thấy lỗi hoặc chỉ thấy chậm nhẹ.

Nếu không có redundancy, mất disk chứa data có thể làm database lỗi nghiêm trọng.

### 10.9. Một Exadata storage cell chết

Nếu redundancy còn đủ:

- database vẫn chạy;
- ASM đọc từ mirror ở cell khác;
- hệ thống giảm I/O capacity;
- rebalance có thể tạo thêm tải;
- app có thể chậm hơn nếu workload nặng.

Nếu mất quá nhiều cell vượt redundancy:

- disk group có thể không còn đủ bản data;
- database có thể lỗi I/O hoặc dừng.

### 10.10. Toàn bộ shared storage chết

RAC không cứu được.

Database cần storage để:

- đọc datafile;
- ghi redo;
- ghi undo;
- checkpoint;
- đọc control file;
- đảm bảo consistency.

Nếu toàn bộ shared storage không truy cập được, database có thể treo, crash, hoặc bị dừng để bảo vệ dữ liệu.

### 10.11. Cả site chết

RAC không phải disaster recovery đầy đủ.

Nếu cả data center chết, cần DR site riêng. Kiến trúc phổ biến:

```text
Primary Site:
  RAC + Shared Storage

Standby Site:
  Data Guard + Storage riêng

Redo replication:
  Primary -> Standby
```

Oracle Data Guard mới là cơ chế phổ biến để chống mất cả site hoặc cả storage domain.

## 11. Tóm tắt

RAC giải quyết bài toán nhiều instance cùng phục vụ một database để tăng availability và scalability trong phạm vi cluster.

RAC mạnh khi:

- workload có thể chia tự nhiên;
- ít tranh block nóng;
- interconnect nhanh và ổn định;
- storage có redundancy tốt;
- app connect qua SCAN/service đúng cách;
- connection pool, timeout, retry được cấu hình đúng.

RAC không tự động giải quyết:

- app hardcode vào một node;
- workload có hotspot nặng;
- counter table bị update liên tục;
- B-tree index right-hand hotspot;
- interconnect kém;
- shared storage chết hoàn toàn;
- mất cả site.

Kết luận ngắn:

```text
RAC bảo vệ tốt tầng instance/node.
Storage phải có HA riêng.
Muốn chống mất site thì cần Data Guard/DR.
Muốn scale tốt thì workload phải ít tranh block nóng.
```
