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

## 5. Exadata, Storage Cell, Mirror và ASM Disk Group

### 5.1. Exadata là gì?

Exadata là hệ thống phần cứng và phần mềm chuyên dụng của Oracle để chạy Oracle Database. Nó không chỉ là một server database, mà là một kiến trúc gồm nhiều lớp:

```text
Database Server
Database Server
Database Server
      |
      v
Storage Network tốc độ cao
      |
      v
Storage Cell
Storage Cell
Storage Cell
```

Trong mô hình này:

- `Database Server` chạy Oracle Database Instance, RAC Instance, SQL execution, session, transaction.
- `Storage Cell` là server lưu trữ chuyên dụng, chứa disk, flash, CPU, memory và Exadata Storage Server Software.
- `Storage Network` là mạng tốc độ cao giữa database server và storage cell.

Exadata tối ưu Oracle Database vì storage cell không chỉ trả block một cách thụ động. Nó có thể xử lý một phần công việc ở tầng storage, ví dụ filter dữ liệu, đọc column cần thiết, bỏ qua vùng dữ liệu không liên quan, hoặc dùng flash cache.

### 5.2. Storage Cell là gì?

Storage cell là một server lưu trữ trong Exadata.

Nó thường có:

- hard disk hoặc flash storage;
- CPU;
- memory;
- flash cache;
- phần mềm Exadata Storage Server;
- kết nối mạng tốc độ cao tới database server.

Database server gửi I/O request xuống storage cell. Storage cell đọc dữ liệu từ disk/flash, có thể xử lý một phần dữ liệu, rồi trả kết quả về database server.

Ví dụ:

```text
DB Server cần đọc dữ liệu bảng ORDERS
   |
   v
Storage Cell đọc block từ disk/flash
   |
   v
Storage Cell có thể filter bớt dữ liệu
   |
   v
DB Server nhận ít dữ liệu hơn để xử lý tiếp
```

### 5.3. Storage Cell có tự scale không?

Không nên hiểu là storage cell tự scale kiểu app container tự tăng pod.

Trong Exadata on-premise, muốn tăng storage cell hoặc tăng tài nguyên storage thì thường là thao tác hạ tầng có kế hoạch:

- mua thêm storage cell hoặc expansion rack;
- thêm disk/flash theo cấu hình được hỗ trợ;
- cấu hình để cluster nhận thêm storage;
- thêm disk vào ASM disk group;
- ASM rebalance lại dữ liệu qua các disk/cell mới.

Oracle/Exadata cung cấp cơ chế tự động ở một số phần, ví dụ ASM rebalance dữ liệu sau khi thêm/bớt disk, nhưng việc tăng resource vật lý không phải tự xảy ra.

Trong Exadata Cloud hoặc Exadata Cloud@Customer, mức độ scale phụ thuộc dịch vụ đang dùng. Có thể có thao tác scale OCPU, storage hoặc VM cluster qua console/API, nhưng vẫn là hành động cấu hình/quản trị, không phải database tự ý thêm storage cell khi tải tăng.

Tóm lại:

```text
Storage Cell không tự sinh thêm.
Admin/DBA/Cloud operation phải tăng resource.
ASM/Exadata hỗ trợ phân bổ lại dữ liệu sau khi resource được thêm.
```

### 5.4. Mirror là gì?

`Mirror` nghĩa là lưu nhiều bản sao của cùng một dữ liệu ở nhiều failure domain khác nhau.

Ví dụ:

```text
Block A
  bản mirror 1 nằm ở Storage Cell 1
  bản mirror 2 nằm ở Storage Cell 2
```

Nếu Storage Cell 1 chết, Oracle vẫn có thể đọc Block A từ Storage Cell 2.

ASM có các mức redundancy phổ biến:

- `NORMAL REDUNDANCY`: thường lưu 2 bản.
- `HIGH REDUNDANCY`: thường lưu 3 bản.

Mirror giúp database sống sót khi disk/cell/path lỗi. Nhưng mirror không thay thế backup. Nếu app xóa nhầm dữ liệu, hoặc dữ liệu bị thay đổi sai ở mức logic, bản mirror cũng phản ánh thay đổi đó.

### 5.5. ASM Disk Group là gì?

ASM là Automatic Storage Management. Đây là lớp quản lý storage của Oracle.

ASM gom nhiều disk thành một nhóm logic gọi là `disk group`.

Ví dụ:

```text
Disk 1
Disk 2
Disk 3
Disk 4
   |
   v
ASM Disk Group: +DATA
   |
   v
Datafile, redo log, control file
```

Một hệ thống có thể có nhiều disk group:

```text
+DATA  -> chứa datafile chính
+RECO  -> chứa recovery file, archive log, backup liên quan
+FRA   -> Fast Recovery Area, tùy cách đặt tên
```

ASM chịu trách nhiệm:

- phân bổ dữ liệu qua nhiều disk;
- mirror dữ liệu;
- quản lý failure group;
- rebalance khi thêm/bớt disk;
- giúp database dùng storage theo dạng logic thay vì tự quản từng file vật lý.

## 6. Exadata Smart Scan trả dữ liệu như thế nào và vì sao tối ưu?

Với storage thông thường, database server thường yêu cầu đọc block/page, storage trả nguyên block/page về database server.

Ví dụ query:

```sql
SELECT order_id, amount
FROM orders
WHERE status = 'PAID';
```

Nếu bảng `orders` rất lớn, database server có thể phải kéo rất nhiều block từ storage lên:

```text
Storage thường:

DB Server yêu cầu đọc nhiều block
Storage trả nguyên block/page
DB Server tự lọc status = 'PAID'
DB Server tự lấy order_id, amount
DB Server bỏ phần dữ liệu không cần
```

Vấn đề là nhiều dữ liệu đi qua network nhưng cuối cùng bị bỏ đi.

Với Exadata Smart Scan, Oracle có thể đẩy một phần xử lý xuống storage cell:

```text
Exadata Smart Scan:

DB Server gửi query/offload request xuống Storage Cell
Storage Cell đọc block từ disk/flash
Storage Cell lọc status = 'PAID'
Storage Cell chỉ lấy column cần thiết: order_id, amount
Storage Cell trả về tập dữ liệu đã được giảm kích thước
DB Server xử lý tiếp phần còn lại
```

Storage cell không trả "row object" theo nghĩa app-level. Nó vẫn hiểu và đọc Oracle database block, nhưng với Smart Scan, storage cell có thể trả về kết quả đã được lọc/giảm bớt theo dạng internal result set cho database server, thay vì luôn trả nguyên toàn bộ block chưa xử lý.

Cơ chế này tối ưu vì:

- giảm lượng data truyền qua storage network;
- giảm CPU trên database server vì một phần filter được làm ở storage cell;
- tận dụng CPU của nhiều storage cell chạy song song;
- giảm I/O không cần thiết nhờ Storage Index hoặc offload predicate;
- hiệu quả cao với full table scan, large scan, analytics query, data warehouse workload.

Ví dụ đơn giản:

```text
Bảng ORDERS có 1 TB dữ liệu.
Query chỉ cần các order PAID trong 2 cột.

Storage thường:
  có thể phải đọc và gửi lượng block rất lớn về DB Server.

Exadata:
  Storage Cell lọc bớt ngay tại nơi dữ liệu được đọc.
  DB Server nhận ít dữ liệu hơn nhiều.
```

Nhưng Smart Scan không phải lúc nào cũng xảy ra. Nó phụ thuộc loại query, execution plan, object type, predicate, storage offload eligibility và nhiều điều kiện khác. OLTP query nhỏ theo primary key thường không hưởng lợi nhiều như analytic scan lớn.

## 7. Interconnect là gì?

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

## 8. Cache Fusion là gì?

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

## 9. GCS và GES

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

## 10. Vì sao RAC scale tốt nhất khi workload chia tự nhiên?

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

## 11. B-tree index hotspot là gì?

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

## 12. Khi các thành phần RAC bị lỗi

### 12.1. Backend app chết

RAC không bị ảnh hưởng.

Ảnh hưởng nằm ở tầng app:

- request trên backend đó mất;
- connection DB bị đứt;
- transaction chưa commit thường rollback;
- load balancer đưa traffic sang backend khác nếu có.

### 12.2. Một listener chết

Nếu app connect qua SCAN hoặc address list có nhiều endpoint:

- connection mới có thể đi qua listener khác;
- connection cũ thường vẫn chạy;
- service không route vào listener/node lỗi.

Nếu app fix cứng vào một host listener duy nhất:

```text
jdbc:oracle:thin:@//rac-node-1.company.com:1521/order_service
```

thì listener đó chết sẽ làm connection mới lỗi, dù các instance khác vẫn sống.

### 12.3. Toàn bộ listener/SCAN không hoạt động

Connection đã mở có thể vẫn chạy.

Connection mới thường lỗi vì app không tạo được session mới.

### 12.4. Một RAC instance chết

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

### 12.5. Một physical node chết

Nếu node chết, instance trên node đó cũng chết.

RAC tiếp tục chạy nếu:

- còn node khác;
- cluster còn quorum;
- storage và interconnect còn hoạt động;
- service được cấu hình failover đúng.

### 12.6. Interconnect chậm hoặc đứt

Nếu interconnect chậm:

- Cache Fusion chậm;
- wait event tăng;
- SQL/update có thể chậm mạnh.

Nếu interconnect đứt:

- cluster có nguy cơ split brain;
- Clusterware có thể evict một số node;
- connection tới node bị evict lỗi;
- node còn lại tiếp tục nếu còn quorum và storage ổn.

### 12.7. ASM trên một node chết

ASM quản lý disk group. Nếu ASM instance trên một node chết, database instance trên node đó có thể bị ảnh hưởng và bị dừng.

Nếu ASM trên node khác và disk group vẫn tốt, các instance khác có thể tiếp tục.

### 12.8. Một disk chết

Nếu ASM redundancy còn đủ:

- database vẫn chạy;
- ASM đọc từ mirror;
- ASM có thể rebalance;
- app có thể không thấy lỗi hoặc chỉ thấy chậm nhẹ.

Nếu không có redundancy, mất disk chứa data có thể làm database lỗi nghiêm trọng.

### 12.9. Một Exadata storage cell chết

Nếu redundancy còn đủ:

- database vẫn chạy;
- ASM đọc từ mirror ở cell khác;
- hệ thống giảm I/O capacity;
- rebalance có thể tạo thêm tải;
- app có thể chậm hơn nếu workload nặng.

Nếu mất quá nhiều cell vượt redundancy:

- disk group có thể không còn đủ bản data;
- database có thể lỗi I/O hoặc dừng.

### 12.10. Toàn bộ shared storage chết

RAC không cứu được.

Database cần storage để:

- đọc datafile;
- ghi redo;
- ghi undo;
- checkpoint;
- đọc control file;
- đảm bảo consistency.

Nếu toàn bộ shared storage không truy cập được, database có thể treo, crash, hoặc bị dừng để bảo vệ dữ liệu.

### 12.11. Cả site chết

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

## 13. Tóm tắt

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
