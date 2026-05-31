# Hybrid Workload Topologies

## 1. Tổng quan

Trong MariaDB, **Hybrid Workload Topologies** là nhóm topology dùng khi hệ thống không chỉ chạy một loại workload duy nhất.

Thông thường workload database được chia thành:

- **OLTP**: xử lý giao dịch, ví dụ tạo đơn hàng, thanh toán, cập nhật tồn kho, cập nhật số dư.
- **OLAP**: phân tích dữ liệu, ví dụ dashboard, báo cáo doanh thu, tổng hợp dữ liệu lịch sử.
- **HTAP**: kết hợp OLTP và OLAP trong cùng một stack.
- **Spider**: truy cập dữ liệu phân tán qua nhiều MariaDB node, có thể dùng cho federation hoặc sharding.

Tài liệu này tập trung vào các topology thuộc nhóm hybrid workload:

- **HTAP Topology**
- **Spider Federated Topology**
- **Spider Sharded Topology**

---

## 2. HTAP Topology

### 2.1. HTAP là gì?

**HTAP** là viết tắt của **Hybrid Transactional/Analytical Processing**.

Mục tiêu của HTAP là cho phép cùng một hệ thống xử lý cả:

- **Transactional workload**: nhiều transaction nhỏ, cần chính xác, latency thấp.
- **Analytical workload**: query lớn, scan nhiều dòng, aggregate dữ liệu, phục vụ báo cáo.

Ví dụ trong một hệ thống thương mại điện tử:

- Khi user đặt hàng, hệ thống ghi vào bảng `orders`, `payments`, `inventory`. Đây là OLTP.
- Khi quản lý xem dashboard doanh thu theo ngày, theo khu vực, theo sản phẩm, hệ thống query dữ liệu lớn. Đây là OLAP.

Nếu dùng kiến trúc truyền thống, dữ liệu OLTP thường được ETL hoặc CDC sang data warehouse riêng. Với HTAP, MariaDB cho phép kết hợp **InnoDB** và **ColumnStore** trong cùng một hệ sinh thái để giảm khoảng cách giữa giao dịch và phân tích.

### 2.2. Thành phần chính

```text
Application / BI Tool
        |
        v
MariaDB Enterprise Server / MaxScale
        |
        +-- InnoDB tables       -> transactional data
        |
        +-- ColumnStore tables  -> analytical data
```

Các thành phần thường gặp:

| Thành phần | Vai trò |
| --- | --- |
| `InnoDB` | Lưu dữ liệu giao dịch, phù hợp cho insert/update/delete, primary key lookup, transaction ACID. |
| `ColumnStore` | Lưu dữ liệu phân tích theo cột, phù hợp cho scan dữ liệu lớn, aggregate, reporting. |
| `MaxScale` | Proxy/router đứng trước MariaDB, có thể hỗ trợ routing, quản lý connection, security, observability. |
| S3-compatible object storage | Dùng với ColumnStore để lưu dữ liệu phân tích trên object storage có khả năng mở rộng. |
| Cross-engine JOIN | Cho phép query join giữa bảng InnoDB và bảng ColumnStore. |

### 2.3. InnoDB trong HTAP

InnoDB là row-store engine. Dữ liệu được lưu theo dòng, rất phù hợp cho OLTP.

Ví dụ bảng giao dịch:

```sql
CREATE TABLE customers (
    id BIGINT PRIMARY KEY,
    full_name VARCHAR(255),
    email VARCHAR(255),
    status VARCHAR(50),
    created_at DATETIME
) ENGINE=InnoDB;

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT,
    amount DECIMAL(12, 2),
    status VARCHAR(50),
    created_at DATETIME,
    INDEX idx_orders_customer_id (customer_id),
    INDEX idx_orders_created_at (created_at)
) ENGINE=InnoDB;
```

Các query OLTP thường là:

```sql
SELECT * FROM customers WHERE id = 1001;

UPDATE orders
SET status = 'PAID'
WHERE id = 90001;

INSERT INTO orders (id, customer_id, amount, status, created_at)
VALUES (90002, 1001, 250000, 'NEW', NOW());
```

Đặc điểm:

- Truy cập ít dòng.
- Dùng index nhiều.
- Cần transaction.
- Cần độ chính xác cao.
- Cần phản hồi nhanh.

### 2.4. ColumnStore trong HTAP

ColumnStore là columnar storage. Dữ liệu được tổ chức theo cột, phù hợp với phân tích dữ liệu lớn.

Ví dụ bảng phân tích:

```sql
CREATE TABLE order_events_analytics (
    order_id BIGINT,
    customer_id BIGINT,
    region VARCHAR(100),
    product_category VARCHAR(100),
    amount DECIMAL(12, 2),
    order_date DATE
) ENGINE=ColumnStore;
```

Query OLAP thường là:

```sql
SELECT
    region,
    product_category,
    SUM(amount) AS total_revenue,
    COUNT(*) AS total_orders
FROM order_events_analytics
WHERE order_date BETWEEN '2026-01-01' AND '2026-01-31'
GROUP BY region, product_category
ORDER BY total_revenue DESC;
```

Đặc điểm:

- Scan nhiều dòng.
- Thường chỉ đọc một số cột cần thiết.
- Dùng `SUM`, `COUNT`, `AVG`, `GROUP BY`, `ORDER BY`.
- Ít update từng dòng nhỏ.
- Phù hợp cho dashboard, BI, báo cáo định kỳ.

### 2.5. Cross-engine JOIN

Một điểm quan trọng của HTAP Topology là có thể join giữa bảng giao dịch và bảng phân tích.

Ví dụ:

```sql
SELECT
    c.full_name,
    c.email,
    SUM(a.amount) AS total_spent
FROM customers c
JOIN order_events_analytics a
    ON c.id = a.customer_id
WHERE a.order_date >= '2026-01-01'
GROUP BY c.full_name, c.email
ORDER BY total_spent DESC;
```

Trong ví dụ này:

- `customers` là bảng InnoDB, phục vụ thông tin customer hiện tại.
- `order_events_analytics` là bảng ColumnStore, phục vụ dữ liệu phân tích lịch sử.
- Query có thể kết hợp dữ liệu từ hai engine.

### 2.6. Ưu điểm

- Giảm nhu cầu tách hoàn toàn OLTP database và analytics database.
- Có thể phân tích dữ liệu gần realtime hơn so với ETL batch truyền thống.
- InnoDB vẫn tối ưu cho transaction.
- ColumnStore vẫn tối ưu cho analytics.
- Có thể dùng object storage tương thích S3 cho dữ liệu phân tích lớn.
- Cross-engine JOIN giúp query linh hoạt hơn.
- Phù hợp khi doanh nghiệp muốn một stack MariaDB thống nhất cho cả transactional và analytical workload.

### 2.7. Nhược điểm

- Không đơn giản như một database OLTP thuần.
- Cần hiểu rõ bảng nào nên dùng InnoDB, bảng nào nên dùng ColumnStore.
- Cross-engine JOIN có thể tốn chi phí nếu join dữ liệu quá lớn hoặc thiếu điều kiện lọc tốt.
- Workload OLTP và OLAP có thể tranh chấp tài nguyên CPU, memory, I/O nếu triển khai không cẩn thận.
- Cần chiến lược nạp dữ liệu từ bảng giao dịch sang bảng phân tích.
- Vận hành phức tạp hơn so với chỉ dùng một engine.

### 2.8. Use case phù hợp

HTAP Topology phù hợp khi:

- Hệ thống vừa cần ghi giao dịch vừa cần báo cáo gần realtime.
- Dashboard cần dữ liệu mới nhanh, không muốn chờ ETL qua đêm.
- Dữ liệu transactional nằm trong MariaDB và analytics cũng muốn giữ trong hệ sinh thái MariaDB.
- Cần join dữ liệu master hiện tại với dữ liệu lịch sử phân tích.
- Muốn giảm số lượng hệ thống phải vận hành so với mô hình OLTP database + data warehouse riêng.

Ví dụ cụ thể:

- E-commerce: đơn hàng realtime + báo cáo doanh thu.
- Banking: giao dịch tài khoản + phân tích hành vi khách hàng.
- SaaS: event usage + billing + dashboard tenant.
- IoT: dữ liệu thiết bị realtime + phân tích xu hướng.

### 2.9. Khi không nên dùng

Không nên chọn HTAP chỉ vì muốn "một database làm tất cả".

HTAP có thể không phù hợp khi:

- Analytics cực lớn, yêu cầu data lake hoặc warehouse chuyên biệt.
- OLTP cực nhạy latency và không muốn chia sẻ tài nguyên với analytics.
- Team chưa có kinh nghiệm vận hành ColumnStore.
- Query analytics chủ yếu là batch offline, không cần gần realtime.
- Kiến trúc hiện tại đã có data warehouse mạnh và ổn định.

---

## 3. Spider Topologies

### 3.1. Spider là gì?

**Spider** là một storage engine của MariaDB dùng để truy cập dữ liệu nằm trên các MariaDB server khác.

Ý tưởng chính:

- Trên **Spider Node**, ta tạo các bảng dùng `ENGINE=SPIDER`.
- Các bảng này nhìn giống bảng bình thường với application.
- Nhưng dữ liệu thật nằm ở các **Data Nodes** phía sau.
- Spider dùng cơ chế foreign data wrapper để gửi query đến remote MariaDB server.

Mô hình tổng quát:

```text
Application
    |
    v
Spider Node
    |
    +-- Data Node 1
    +-- Data Node 2
    +-- Data Node 3
```

Trong các Data Node, bảng thật thường dùng engine bình thường như InnoDB.

Spider có hai kiểu topology phổ biến trong ảnh:

- **Spider Federated Topology**
- **Spider Sharded Topology**

Hai topology này nhìn rất giống nhau ở hình vẽ, nhưng khác nhau ở mục tiêu thiết kế.

---

## 4. Spider Federated Topology

### 4.1. Khái niệm

**Spider Federated Topology** là mô hình trong đó Spider Node đóng vai trò cổng truy cập tập trung tới nhiều bảng hoặc database remote.

Federated nghĩa là **liên kết nhiều nguồn dữ liệu phân tán** để truy cập từ một nơi.

Trong mô hình này:

- Application chỉ kết nối tới Spider Node.
- Spider Node chứa các Spider table.
- Spider table là bảng "ảo".
- Dữ liệu thật nằm ở Data Node.
- Data Node dùng engine không phải Spider, ví dụ InnoDB.

Mô hình:

```text
Application
    |
    v
Spider Node
    |
    +-- CRM DB:       customers
    +-- Billing DB:   invoices
    +-- Inventory DB: products
```

Application có thể query qua Spider Node thay vì phải kết nối từng database riêng.

### 4.2. Đặc điểm từ topology

Theo ảnh, Spider Federated Topology có các đặc điểm chính:

- Read/write tới bảng trên remote Enterprise Server nodes.
- Spider Node dùng Spider storage engine cho Federated Spider Tables.
- Federated Spider Table là bảng "ảo".
- Spider dùng MariaDB foreign data wrapper để query Data Table trên Data Node.
- Data Node dùng non-Spider storage engine cho Data Tables.
- Hỗ trợ transaction.
- Dùng với Enterprise Server 10.3+ và Enterprise Spider.

### 4.3. Luồng hoạt động

Giả sử application chạy:

```sql
SELECT * FROM customers WHERE id = 1001;
```

Luồng xử lý:

1. Application gửi query tới Spider Node.
2. Spider Node nhận query như MariaDB server bình thường.
3. Spider engine kiểm tra metadata của bảng `customers`.
4. Spider biết bảng thật nằm ở remote Data Node nào.
5. Spider gửi query tương ứng tới Data Node.
6. Data Node thực thi query trên bảng thật, ví dụ bảng InnoDB.
7. Data Node trả kết quả về Spider Node.
8. Spider Node trả kết quả cuối cùng cho application.

Application không cần biết dữ liệu thật nằm ở đâu.

### 4.4. Ví dụ bảng Spider

Ví dụ trên Data Node có bảng thật:

```sql
CREATE TABLE customers (
    id BIGINT PRIMARY KEY,
    full_name VARCHAR(255),
    email VARCHAR(255),
    country VARCHAR(100)
) ENGINE=InnoDB;
```

Trên Spider Node có thể tạo bảng đại diện:

```sql
CREATE TABLE customers (
    id BIGINT PRIMARY KEY,
    full_name VARCHAR(255),
    email VARCHAR(255),
    country VARCHAR(100)
)
ENGINE=SPIDER
COMMENT='wrapper "mysql", table "customers", host "crm-db", port "3306"';
```

Với application, bảng `customers` trên Spider Node nhìn như bảng bình thường.

### 4.5. Federated không bắt buộc khác schema

Một hiểu nhầm thường gặp là Federated nghĩa là các bảng phía sau phải khác schema. Không đúng.

Federated có thể:

- Khác schema, khác domain.
- Cùng schema nhưng thuộc các hệ thống độc lập.

Ví dụ cùng schema nhưng vẫn là Federated:

```text
Spider Node
    |
    +-- Vietnam DB:    customers
    +-- Thailand DB:   customers
    +-- Singapore DB:  customers
```

Ba bảng `customers` có thể cùng schema, nhưng mỗi database là một hệ thống độc lập của từng quốc gia. Nếu mục tiêu là gom truy cập nhiều hệ thống độc lập, đây vẫn là Federated.

Điểm quyết định không phải chỉ là schema, mà là:

- Các node có phải các hệ thống độc lập không?
- Hay chúng là các mảnh của cùng một dataset logic?

### 4.6. Ưu điểm

- Application chỉ cần kết nối tới một endpoint.
- Có thể truy cập nhiều MariaDB server từ một nơi.
- Giảm nhu cầu copy dữ liệu về một database trung tâm.
- Phù hợp cho hệ thống đã có nhiều database độc lập.
- Có thể hỗ trợ read/write tới bảng remote.
- Data Node vẫn dùng engine quen thuộc như InnoDB.
- Có thể hỗ trợ transaction.

### 4.7. Nhược điểm

- Spider Node có thể trở thành bottleneck.
- Query qua network luôn có latency cao hơn query local.
- Join giữa nhiều Data Node có thể chậm.
- Nếu query không lọc tốt, Spider có thể phải kéo nhiều dữ liệu từ remote node.
- Monitoring, backup, schema migration phức tạp hơn vì có nhiều node.
- Lỗi network giữa Spider Node và Data Node có thể ảnh hưởng trực tiếp tới query.
- Transaction phân tán khó vận hành hơn transaction trong một database đơn.

### 4.8. Use case phù hợp

Spider Federated Topology phù hợp khi:

- Doanh nghiệp có nhiều MariaDB database độc lập và muốn query từ một điểm chung.
- Cần tích hợp dữ liệu từ nhiều business domain như CRM, billing, inventory.
- Cần giữ dữ liệu ở từng hệ thống gốc nhưng vẫn muốn truy cập tập trung.
- Cần migration dần từ kiến trúc nhiều database sang một lớp truy cập thống nhất.
- Muốn application ít phải biết về vị trí vật lý của dữ liệu.

Ví dụ cụ thể:

```text
Spider Node
    |
    +-- CRM DB:       customers, customer_contacts
    +-- Billing DB:   invoices, payments
    +-- Inventory DB: products, stock_movements
```

Query ví dụ:

```sql
SELECT
    c.full_name,
    i.invoice_no,
    i.total_amount
FROM customers c
JOIN invoices i
    ON c.id = i.customer_id
WHERE i.created_at >= '2026-01-01';
```

Trong ví dụ này:

- `customers` có thể nằm ở CRM DB.
- `invoices` có thể nằm ở Billing DB.
- Spider Node giúp application query qua một endpoint.

### 4.9. Khi không nên dùng

Không nên dùng Federated nếu:

- Query thường xuyên join dữ liệu rất lớn giữa nhiều remote node.
- Latency mạng không ổn định.
- Hệ thống cần transaction cực nhanh và cực đơn giản.
- Một database đơn hoặc replication thông thường đã đủ.
- Team chưa sẵn sàng vận hành nhiều node và xử lý lỗi phân tán.

---

## 5. Spider Sharded Topology

### 5.1. Khái niệm

**Spider Sharded Topology** là mô hình dùng Spider để chia một bảng hoặc một dataset lớn thành nhiều phần nhỏ trên nhiều Data Node.

Sharding nghĩa là **chia ngang dữ liệu**.

Ví dụ một bảng `orders` quá lớn:

```text
orders
    |
    +-- Shard 1: orders của customer_id 1 - 1,000,000
    +-- Shard 2: orders của customer_id 1,000,001 - 2,000,000
    +-- Shard 3: orders của customer_id 2,000,001 - 3,000,000
```

Hoặc chia theo tenant:

```text
orders
    |
    +-- Shard 1: tenant_id 1 - 1000
    +-- Shard 2: tenant_id 1001 - 2000
    +-- Shard 3: tenant_id 2001 - 3000
```

Với application, vẫn có thể nhìn thấy một bảng logic `orders`. Nhưng dữ liệu thật được phân tán trên nhiều node.

### 5.2. Đặc điểm từ topology

Theo ảnh, Spider Sharded Topology có các đặc điểm chính:

- Shard tables để mở rộng theo chiều ngang.
- Spider Node dùng Spider storage engine cho Sharded Spider Tables.
- Sharded Spider Table là bảng "ảo" có partition.
- Spider dùng MariaDB foreign data wrapper để query Data Tables trên Data Nodes cho từng partition.
- Data Node dùng non-Spider storage engine cho Data Tables.
- Hỗ trợ transaction.
- Dùng với Enterprise Server 10.3+ và Enterprise Spider.

### 5.3. Luồng hoạt động

Giả sử bảng `orders` được shard theo `tenant_id`.

```text
tenant_id 1 - 1000      -> Data Node A
tenant_id 1001 - 2000   -> Data Node B
tenant_id 2001 - 3000   -> Data Node C
```

Application chạy:

```sql
SELECT * FROM orders WHERE tenant_id = 1200;
```

Luồng xử lý:

1. Application gửi query tới Spider Node.
2. Spider Node nhận query trên bảng logic `orders`.
3. Spider kiểm tra partition rule hoặc shard mapping.
4. Vì `tenant_id = 1200`, Spider biết dữ liệu nằm ở Data Node B.
5. Spider chỉ gửi query tới Data Node B.
6. Data Node B query bảng thật.
7. Kết quả trả về application qua Spider Node.

Nếu query không có shard key:

```sql
SELECT COUNT(*) FROM orders WHERE status = 'PAID';
```

Spider có thể phải gửi query tới nhiều shard, sau đó gom kết quả. Đây là lý do chọn shard key rất quan trọng.

### 5.4. Ví dụ thiết kế shard

Ví dụ bảng logic:

```sql
CREATE TABLE orders (
    id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    amount DECIMAL(12, 2),
    status VARCHAR(50),
    created_at DATETIME,
    PRIMARY KEY (tenant_id, id)
);
```

Nếu chọn shard key là `tenant_id`, dữ liệu có thể phân phối như sau:

| Shard | Điều kiện | Data Node |
| --- | --- | --- |
| Shard 1 | `tenant_id` từ 1 đến 1000 | `orders-node-1` |
| Shard 2 | `tenant_id` từ 1001 đến 2000 | `orders-node-2` |
| Shard 3 | `tenant_id` từ 2001 đến 3000 | `orders-node-3` |

Query tốt:

```sql
SELECT *
FROM orders
WHERE tenant_id = 1200
  AND id = 999;
```

Query này tốt vì Spider có thể route tới đúng shard.

Query kém hơn:

```sql
SELECT *
FROM orders
WHERE status = 'PAID'
ORDER BY created_at DESC
LIMIT 100;
```

Query này không có shard key, nên có thể phải chạy trên nhiều shard rồi merge kết quả.

### 5.5. Chọn shard key

Shard key là yếu tố sống còn của sharding.

Shard key tốt thường có đặc điểm:

- Xuất hiện thường xuyên trong `WHERE`.
- Phân phối dữ liệu tương đối đều.
- Ít thay đổi.
- Giúp route query đến ít shard nhất có thể.
- Phù hợp với ranh giới nghiệp vụ.

Ví dụ shard key phổ biến:

| Shard key | Phù hợp khi |
| --- | --- |
| `tenant_id` | SaaS multi-tenant, mỗi tenant có dữ liệu riêng. |
| `customer_id` | Dữ liệu xoay quanh customer. |
| `region` | Dữ liệu tách theo khu vực địa lý. |
| `created_year` hoặc `order_year` | Dữ liệu lịch sử, thường query theo năm. |
| hash của `id` | Muốn phân phối đều, nhưng query range có thể khó hơn. |

Shard key kém:

- Cột có quá ít giá trị, ví dụ `status`.
- Cột thường xuyên thay đổi.
- Cột không xuất hiện trong query chính.
- Cột gây lệch dữ liệu, ví dụ một tenant quá lớn chiếm phần lớn traffic.

### 5.6. Ưu điểm

- Mở rộng dung lượng theo chiều ngang.
- Có thể phân phối read/write load ra nhiều Data Node.
- Một bảng logic lớn có thể được chia nhỏ để dễ quản lý hơn.
- Nếu query có shard key tốt, Spider chỉ cần query một hoặc vài node.
- Có thể thêm node để tăng sức chứa, tùy chiến lược phân phối dữ liệu.
- Application có thể ít phải biết chi tiết mỗi shard nằm ở đâu.

### 5.7. Nhược điểm

- Thiết kế phức tạp hơn Federated thông thường.
- Chọn sai shard key sẽ gây bottleneck hoặc query scatter-gather.
- Join giữa các shard khó và tốn chi phí.
- Transaction xuyên nhiều shard phức tạp.
- Rebalancing dữ liệu khi thêm/bớt shard không đơn giản.
- Backup/restore phải tính đến tính nhất quán giữa nhiều shard.
- Unique constraint toàn cục khó hơn nếu key không chứa shard key.
- Reporting toàn hệ thống có thể chậm nếu phải gom dữ liệu từ nhiều shard.

### 5.8. Use case phù hợp

Spider Sharded Topology phù hợp khi:

- Một bảng hoặc dataset quá lớn cho một node.
- Write/read load của một dataset vượt khả năng một server.
- Dữ liệu có ranh giới phân chia rõ ràng như tenant, customer, region, year.
- Query chính thường có shard key.
- Hệ thống chấp nhận độ phức tạp vận hành của sharding để đổi lấy scale-out.

Ví dụ cụ thể:

- SaaS multi-tenant: shard theo `tenant_id`.
- E-commerce lớn: shard đơn hàng theo `customer_id` hoặc `order_year`.
- Log/event platform: shard theo ngày/tháng hoặc hash key.
- Hệ thống tài chính: shard dữ liệu giao dịch theo account range hoặc region.

### 5.9. Khi không nên dùng

Không nên dùng Sharded Topology nếu:

- Dữ liệu chưa đủ lớn để cần sharding.
- Query thường không có shard key.
- Cần join phức tạp giữa nhiều phần dữ liệu.
- Team chưa có quy trình vận hành shard, backup, migration, rebalancing.
- Vấn đề hiện tại có thể giải quyết bằng index, partition local, read replica hoặc hardware tốt hơn.

---

## 6. Federated vs Sharding

### 6.1. Vì sao hai mô hình nhìn giống nhau?

Federated và Sharded đều có thể có hình dạng:

```text
Application
    |
    v
Spider Node
    |
    +-- Data Node 1
    +-- Data Node 2
    +-- Data Node 3
```

Vì vậy nhìn topology bên ngoài có vẻ không khác nhau.

Khác biệt chính nằm ở:

- Mục tiêu thiết kế.
- Cách dữ liệu được map xuống các Data Node.
- Quan hệ logic giữa các bảng phía sau.

### 6.2. Federated là gì trong thực tế?

Federated nghĩa là truy cập nhiều nguồn dữ liệu remote qua một lớp chung.

Ví dụ:

```text
Spider Node
    |
    +-- CRM DB:       customers
    +-- Billing DB:   invoices
    +-- Inventory DB: products
```

Ở đây các database phía sau có thể là các hệ thống độc lập.

Câu hỏi thiết kế của Federated thường là:

```text
Bảng này nằm ở hệ thống nào?
```

### 6.3. Sharding là gì trong thực tế?

Sharding nghĩa là chia một dataset lớn thành nhiều phần.

Ví dụ:

```text
Spider Node
    |
    +-- Data Node A: orders shard 1
    +-- Data Node B: orders shard 2
    +-- Data Node C: orders shard 3
```

Ở đây các node phía sau cùng nhau tạo thành một bảng logic `orders`.

Câu hỏi thiết kế của Sharding thường là:

```text
Row này thuộc shard nào?
```

### 6.4. Federated cùng schema có bằng Sharding không?

Không hẳn.

**Federated cùng schema chưa chắc là Sharding.**

Điểm quyết định không chỉ là schema giống nhau, mà là các node có đang chứa các phần không trùng nhau của cùng một bảng logic hay không.

Ví dụ cùng schema nhưng vẫn là Federated:

```text
Spider Node
    |
    +-- Vietnam DB:    customers
    +-- Thailand DB:   customers
    +-- Singapore DB:  customers
```

Cả ba bảng `customers` có cùng schema. Nhưng nếu mỗi database thuộc một chi nhánh hoặc quốc gia riêng, được vận hành như hệ thống độc lập, thì đây vẫn là Federated.

Ví dụ cùng schema và là Sharding:

```text
Spider Node
    |
    +-- Shard 1: customers id 1 - 1,000,000
    +-- Shard 2: customers id 1,000,001 - 2,000,000
    +-- Shard 3: customers id 2,000,001 - 3,000,000
```

Ở đây ba node cùng nhau tạo thành một bảng logic `customers`. Mỗi row thuộc đúng một shard theo rule phân phối. Đây là Sharding.

Kết luận thực dụng:

```text
Federated + cùng schema + phân vùng dữ liệu theo rule rõ ràng
= gần như Sharding
```

Nhưng:

```text
Federated + cùng schema + nhiều hệ thống độc lập
= vẫn là Federated, không nhất thiết là Sharding
```

Có thể xem **Sharding là một trường hợp đặc biệt có kỷ luật hơn của Federation**, trong đó một dataset logic được chia theo shard key.

### 6.5. Bảng so sánh

| Tiêu chí | Spider Federated Topology | Spider Sharded Topology |
| --- | --- | --- |
| Mục tiêu chính | Gom truy cập nhiều nguồn dữ liệu remote | Chia nhỏ một dataset lớn |
| Câu hỏi thiết kế | Bảng này nằm ở system nào? | Row này thuộc shard nào? |
| Dữ liệu phía sau | Có thể khác domain, khác schema, hoặc cùng schema nhưng độc lập | Thường cùng schema, cùng bảng logic |
| Quan hệ giữa các node | Các hệ thống có thể độc lập | Các shard cùng tạo thành một dataset |
| Mapping | Bảng remote theo nguồn dữ liệu | Partition/shard theo shard key |
| Ví dụ | CRM + Billing + Inventory | `orders` chia theo `tenant_id` |
| Mục tiêu scale | Tích hợp và truy cập tập trung | Scale dung lượng và tải |
| Rủi ro chính | Join remote chậm, network latency, vận hành nhiều source | Chọn sai shard key, scatter-gather, rebalancing khó |

---

## 7. So sánh HTAP, Spider Federated và Spider Sharded

| Topology | Bài toán chính | Thành phần nổi bật | Phù hợp nhất khi |
| --- | --- | --- | --- |
| HTAP | Kết hợp transaction và analytics | InnoDB + ColumnStore + MaxScale | Cần OLTP và OLAP trong cùng hệ sinh thái MariaDB |
| Spider Federated | Truy cập nhiều nguồn dữ liệu remote | Spider Node + Federated Spider Tables + Data Nodes | Có nhiều database/hệ thống độc lập cần query tập trung |
| Spider Sharded | Chia dataset lớn thành nhiều shard | Spider Node + partitioned virtual table + Data Nodes | Một bảng/dataset quá lớn cần scale ngang |

### 7.1. Cách chọn nhanh

Chọn **HTAP** nếu câu hỏi chính là:

```text
Làm sao vừa ghi giao dịch vừa phân tích dữ liệu trong cùng một stack?
```

Chọn **Spider Federated** nếu câu hỏi chính là:

```text
Làm sao query nhiều database remote từ một nơi?
```

Chọn **Spider Sharded** nếu câu hỏi chính là:

```text
Làm sao chia một bảng/dataset lớn ra nhiều node để scale?
```

---

## 8. Ví dụ tổng hợp

Giả sử có một nền tảng SaaS lớn.

### 8.1. Dùng HTAP

Hệ thống cần:

- Ghi order, payment, subscription realtime.
- Dashboard doanh thu gần realtime.
- Báo cáo usage theo tenant.

Thiết kế:

```text
InnoDB:
    tenants
    subscriptions
    invoices
    payments

ColumnStore:
    usage_events_analytics
    revenue_daily_fact
    tenant_activity_fact
```

Query:

```sql
SELECT
    t.name,
    SUM(r.revenue) AS total_revenue
FROM tenants t
JOIN revenue_daily_fact r
    ON t.id = r.tenant_id
WHERE r.revenue_date BETWEEN '2026-01-01' AND '2026-01-31'
GROUP BY t.name;
```

### 8.2. Dùng Spider Federated

Hệ thống đang có nhiều database độc lập:

```text
CRM DB:
    customers

Billing DB:
    invoices
    payments

Support DB:
    tickets
```

Spider Node giúp query tập trung:

```sql
SELECT
    c.full_name,
    i.invoice_no,
    t.ticket_no
FROM customers c
LEFT JOIN invoices i
    ON c.id = i.customer_id
LEFT JOIN tickets t
    ON c.id = t.customer_id
WHERE c.id = 1001;
```

Mục tiêu chính không phải chia nhỏ một bảng lớn, mà là tích hợp nhiều nguồn dữ liệu.

### 8.3. Dùng Spider Sharded

Hệ thống có bảng `usage_events` quá lớn.

Thiết kế shard theo `tenant_id`:

```text
usage_events shard 1: tenant_id 1 - 1000
usage_events shard 2: tenant_id 1001 - 2000
usage_events shard 3: tenant_id 2001 - 3000
```

Query tốt:

```sql
SELECT COUNT(*)
FROM usage_events
WHERE tenant_id = 1200
  AND event_date >= '2026-01-01';
```

Query này có `tenant_id`, nên Spider có thể route tới shard phù hợp.

Query cần cẩn thận:

```sql
SELECT COUNT(*)
FROM usage_events
WHERE event_date >= '2026-01-01';
```

Query này không có `tenant_id`, nên có thể phải chạy trên nhiều shard.

---

## 9. Checklist thiết kế

### 9.1. Checklist cho HTAP

- Bảng nào là transactional và nên dùng InnoDB?
- Bảng nào là analytical và nên dùng ColumnStore?
- Dữ liệu từ OLTP sang analytics được nạp bằng cách nào?
- Query analytics có ảnh hưởng OLTP không?
- Có cần cross-engine JOIN không?
- Có cần object storage cho ColumnStore không?
- Có cần MaxScale đứng trước để routing hoặc quản lý connection không?

### 9.2. Checklist cho Spider Federated

- Mỗi bảng remote thuộc hệ thống nào?
- Data Node có ổn định về network không?
- Query có thường xuyên join nhiều remote node không?
- Schema giữa Spider Table và Data Table có đồng bộ không?
- Cần read-only hay read/write?
- Cần transaction mức nào?
- Backup và migration schema giữa các node sẽ làm thế nào?

### 9.3. Checklist cho Spider Sharded

- Dataset nào thật sự cần sharding?
- Shard key là gì?
- Query chính có luôn chứa shard key không?
- Dữ liệu có phân phối đều không?
- Có tenant hoặc customer quá lớn gây hotspot không?
- Có cần unique constraint toàn cục không?
- Có cần transaction xuyên shard không?
- Khi thêm shard mới, rebalancing dữ liệu sẽ làm thế nào?
- Reporting toàn hệ thống sẽ query trực tiếp trên shard hay đưa sang OLAP/HTAP?

---

## 10. Kết luận

Ba topology này giải quyết ba bài toán khác nhau:

- **HTAP Topology**: dùng khi cần kết hợp giao dịch và phân tích trong cùng một stack MariaDB.
- **Spider Federated Topology**: dùng khi cần truy cập nhiều database hoặc nhiều bảng remote từ một endpoint chung.
- **Spider Sharded Topology**: dùng khi cần chia một dataset lớn thành nhiều phần để mở rộng theo chiều ngang.

Điểm dễ nhầm nhất là Federated và Sharded. Cả hai đều có Spider Node và Data Nodes, nhưng:

- Federated hỏi: **bảng này nằm ở hệ thống nào?**
- Sharding hỏi: **row này thuộc shard nào?**

Nếu cùng schema nhưng các node là các hệ thống độc lập, đó vẫn có thể là Federated. Nếu cùng schema và mỗi node chứa một phần không trùng nhau của cùng một bảng logic theo shard key, đó là Sharding.
