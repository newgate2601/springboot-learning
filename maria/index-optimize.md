# MariaDB - Tối ưu index cho SELECT

## 1. Mục tiêu

Mục tiêu của tài liệu này là xây dựng một cách suy nghĩ thực dụng để chọn index tốt cho một câu `SELECT`.

Không phải mọi query đều có một index hoàn hảo, nhưng đa số query thông thường có thể bắt đầu bằng một quy tắc:

```text
1. Cột so sánh bằng hằng số trước.
2. Sau đó chọn một trong ba hướng:
   - một cột range;
   - toàn bộ GROUP BY;
   - toàn bộ ORDER BY.
3. Nếu cần covering index, thêm các cột SELECT còn thiếu vào cuối.
```

Đây không phải luật tuyệt đối. Nó là cookbook đủ tốt cho nhiều query OLTP và giúp đọc `EXPLAIN` có hệ thống hơn.

## 2. Nhắc lại nền tảng về index

### 2.1. B-Tree index

Hầu hết index thông thường trong MySQL/MariaDB dùng cấu trúc **B-Tree**.

B-Tree giúp database làm tốt hai việc:

- tìm row theo key cụ thể;
- range scan, tức bắt đầu từ một giá trị rồi đọc tiếp giá trị kế tiếp/trước đó theo thứ tự index.

Ví dụ:

```sql
WHERE customer_id = 10
```

phù hợp với tìm theo key cụ thể.

```sql
WHERE created_at >= '2026-01-01'
```

phù hợp với range scan.

### 2.2. Primary key, unique key và index

Trong MariaDB/MySQL:

```text
PRIMARY KEY là UNIQUE KEY.
UNIQUE KEY là INDEX có ràng buộc unique.
KEY gần như đồng nghĩa với INDEX.
```

Với InnoDB, primary key được **clustered** cùng dữ liệu.

Điều này nghĩa là:

```text
Tìm được primary key
-> tìm được luôn row data
```

Secondary index trong InnoDB không trỏ trực tiếp tới vị trí vật lý của row theo kiểu đơn giản. Nó chứa giá trị index cộng với primary key của row.

Luồng đọc qua secondary index:

```text
secondary index
-> tìm primary key tương ứng
-> dùng primary key để đọc row thật trong clustered index
```

Vì vậy secondary index lookup thường có thể gồm hai bước:

```text
đọc secondary index
-> đọc clustered primary key / row thật
```

Đây là lý do covering index có thể rất mạnh: nếu secondary index chứa đủ cột cần dùng, database không cần quay lại clustered primary key để lấy row.

### 2.3. Mỗi bảng InnoDB nên có primary key

Mỗi bảng InnoDB nên có primary key rõ ràng.

Nếu không khai báo, InnoDB vẫn cần một row identifier nội bộ, nhưng tốt hơn là thiết kế primary key có chủ đích vì:

- secondary index sẽ chứa primary key;
- primary key quá dài làm secondary index phình to;
- primary key ngẫu nhiên có thể gây page split nhiều hơn;
- primary key ảnh hưởng locality của dữ liệu.

### 2.4. Vì sao primary key ngẫu nhiên gây page split nhiều hơn?

Trong InnoDB, dữ liệu thật của bảng nằm trong **clustered index** theo thứ tự primary key. Có thể hình dung bảng InnoDB không chỉ có một index phụ trỏ tới data file riêng, mà chính primary key B-Tree là nơi chứa row data ở leaf page.

Một leaf page là một block dữ liệu cố định, thường là `16KB` với page size mặc định của InnoDB. Các row trong page được sắp theo thứ tự primary key.

Khi insert primary key tăng dần, ví dụ:

```text
1, 2, 3, 4, 5, 6, ...
```

InnoDB thường ghi vào phía cuối bên phải của B-Tree:

```text
page cuối còn chỗ
-> append row mới vào page cuối
-> khi page cuối đầy thì cấp page mới bên phải
```

Mô hình này có locality tốt:

- page vừa ghi có khả năng vẫn còn trong buffer pool;
- insert tập trung vào vùng cuối của B-Tree;
- ít phải chạm vào nhiều page rải rác;
- I/O có xu hướng tuần tự hơn;
- CPU cache và buffer pool cache dễ tái sử dụng hơn.

Ngược lại, primary key ngẫu nhiên, ví dụ UUID v4 hoặc một giá trị hash ngẫu nhiên:

```text
8f3..., 12a..., f91..., 3bc..., ...
```

sẽ rải insert vào nhiều vị trí khác nhau trong B-Tree:

```text
row mới có key nhỏ hơn/lớn hơn bất kỳ
-> phải tìm đúng leaf page ở giữa cây
-> insert vào page đó
-> nếu page đó đầy, phải split page
```

**Page split** xảy ra khi database cần chèn một row vào một page đã đầy. Vì B-Tree phải giữ thứ tự key, InnoDB không thể đơn giản nhét row vào cuối file. Nó phải:

```text
1. cấp thêm một page mới;
2. chia một phần row từ page cũ sang page mới;
3. chèn row mới vào đúng vị trí theo thứ tự key;
4. cập nhật pointer ở parent page;
5. nếu parent page cũng đầy, split có thể lan lên cấp cao hơn.
```

Chi phí của page split không chỉ là “thêm một page”. Nó còn kéo theo:

- ghi page cũ;
- ghi page mới;
- cập nhật parent page;
- sinh thêm redo log;
- có thể làm bẩn nhiều page trong buffer pool;
- tăng fragmentation của clustered index;
- làm insert latency dao động mạnh hơn.

Vì primary key ngẫu nhiên rải insert vào toàn bộ cây, nó làm tăng xác suất gặp page giữa cây đã đầy. Với primary key tăng dần, phần lớn insert chỉ đụng page cuối, nên ít split rải rác hơn.

Tóm tắt:

```text
PK tăng dần
-> insert chủ yếu vào cuối B-Tree
-> page split ít hơn và dễ dự đoán hơn
-> locality tốt hơn

PK ngẫu nhiên
-> insert rải vào nhiều leaf page
-> page split nhiều hơn
-> buffer pool bị chạm rải rác
-> locality kém hơn
```

### 2.5. Locality của dữ liệu bị ảnh hưởng như thế nào?

Locality nghĩa là các row thường được đọc/ghi cùng nhau có nằm gần nhau trong storage và buffer pool hay không.

Vì InnoDB cluster dữ liệu theo primary key, primary key quyết định thứ tự vật lý tương đối của row trong clustered index. Nếu primary key là giá trị tăng dần theo thời gian, các row mới thường nằm gần nhau.

Ví dụ bảng `orders` dùng primary key tăng dần:

```text
id = 100001, 100002, 100003, ...
```

Các order mới được ghi gần nhau. Query kiểu:

```sql
WHERE id BETWEEN 100000 AND 101000
```

hoặc các thao tác đọc dữ liệu mới nhất có khả năng chạm vào ít page hơn.

Nếu primary key là UUID ngẫu nhiên:

```text
order A -> page 10
order B -> page 8031
order C -> page 221
order D -> page 9902
```

các order được insert cùng thời điểm có thể nằm rất xa nhau trong clustered index. Khi đọc một nhóm dữ liệu mới ghi, database có thể phải chạm nhiều page hơn, dù về mặt business chúng liên quan gần nhau.

Điều này ảnh hưởng cả read và write:

- read theo range primary key kém tuần tự hơn nếu key không phản ánh thứ tự truy cập;
- insert làm bẩn nhiều page rải rác hơn;
- buffer pool phải giữ nhiều page nóng hơn cho cùng một lượng write;
- checkpoint/flush có thể phải xử lý nhiều dirty page phân tán hơn;
- secondary index lookup cũng bị ảnh hưởng vì bước cuối vẫn phải quay về clustered primary key để lấy row thật.

Điểm dễ nhầm: UUID ngẫu nhiên không làm mọi query chậm. Nếu query luôn dùng secondary index selective và trả rất ít row, tác động có thể nhỏ. Vấn đề lớn xuất hiện ở workload ghi nhiều, bảng lớn, buffer pool không chứa hết dữ liệu, hoặc query thường đọc nhiều row gần nhau theo thời gian/business.

### 2.6. Có phải lúc nào cũng nên dùng primary key tăng dần?

Không tuyệt đối.

Primary key tăng dần có ưu điểm lớn về locality và insert path, nhưng có thể có nhược điểm:

- dễ lộ số lượng bản ghi nếu id public;
- trong hệ phân tán, nhiều node cùng sinh id cần cơ chế phối hợp;
- insert cực lớn có thể tạo điểm nóng ở page cuối, dù InnoDB xử lý case này khá tốt trong nhiều workload;
- nếu business cần id không đoán được, id tăng dần có thể không phù hợp để lộ ra ngoài API.

Các lựa chọn thực dụng:

- Dùng `BIGINT AUTO_INCREMENT` làm primary key nội bộ nếu workload OLTP ghi nhiều và chạy trên một primary writer.
- Nếu cần UUID, cân nhắc UUID có tính thời gian/thứ tự như UUID v7, ULID, hoặc cơ chế sinh id gần tuần tự, thay vì UUID v4 hoàn toàn ngẫu nhiên.
- Nếu cần public id không đoán được, có thể tách `id` nội bộ tăng dần làm primary key và thêm `public_id` unique riêng cho API.
- Tránh primary key quá rộng, vì giá trị primary key được lưu trong secondary index.

Tư duy thiết kế:

```text
Primary key không chỉ là định danh logic.
Trong InnoDB, primary key còn là cách database sắp xếp dữ liệu thật.
```

## 3. Ví dụ trực giác: danh bạ tên

Giả sử có danh sách người được sắp xếp theo:

```text
last_name, first_name
```

Index tương đương:

```sql
INDEX(last_name, first_name)
```

Các query:

```sql
WHERE last_name = 'James' AND first_name = 'Rick'
```

Rất tốt, vì dùng đúng thứ tự index.

```sql
WHERE last_name = 'James' AND first_name LIKE 'R%'
```

Cũng khá tốt, vì `last_name` là equality, sau đó `first_name` là range prefix.

```sql
WHERE last_name LIKE 'J%' AND first_name = 'Rick'
```

Kém hơn, vì cột đầu tiên đã là range. Sau khi range trên `last_name`, index khó dùng `first_name` để định vị chính xác như equality nữa.

Tư duy chính:

```text
Trong composite index, thứ tự cột rất quan trọng.
Equality ở đầu index thường mạnh nhất.
Range thường là điểm dừng cho khả năng dùng tiếp index để lọc sâu.
```

## 4. Thuật toán chọn index cho một SELECT

### 4.1. Bước 1: lấy các cột equality

Nhìn vào `WHERE` gồm các điều kiện nối bằng `AND`.

Thêm vào index các cột có dạng:

```text
column = constant
column IS NULL
```

Ví dụ:

```sql
WHERE user_id = 123 AND status = 'PAID'
```

Index bắt đầu tốt:

```sql
INDEX(user_id, status)
```

hoặc:

```sql
INDEX(status, user_id)
```

Nếu cả hai đều là equality, thứ tự giữa chúng thường ít quan trọng hơn so với việc chúng đứng trước range/group/order. Tuy nhiên trong thực tế vẫn nên cân nhắc:

- cột nào selective hơn;
- cột nào hay dùng trong query khác;
- cột nào giúp nối tiếp với `ORDER BY` hoặc `GROUP BY`;
- cột nào có kích thước nhỏ hơn.

Giải thích kỹ hơn từng tiêu chí:

**Cột nào selective hơn** nghĩa là cột nào giúp loại bỏ nhiều row hơn. Ví dụ bảng `orders` có 10 triệu row:

```text
status = 'PAID'    -> match khoảng 6 triệu row
user_id = 10       -> match khoảng 200 row
```

`user_id` selective hơn rất nhiều. Nếu chỉ nhìn một query:

```sql
WHERE user_id = 10 AND status = 'PAID'
```

thì `INDEX(user_id, status)` thường hợp lý hơn `INDEX(status, user_id)`, vì đi từ `user_id = 10` đã thu hẹp vùng tìm kiếm rất mạnh.

Tuy nhiên, với nhiều cột equality, optimizer vẫn có thể dùng cả hai thứ tự tốt hơn so với case range. Lý do là B-Tree có thể định vị đúng tổ hợp giá trị:

```text
(user_id = 10, status = 'PAID')
```

hoặc:

```text
(status = 'PAID', user_id = 10)
```

Điểm khác biệt nằm ở khả năng tái sử dụng index cho query khác, kích thước index, và phần phía sau index có nối tiếp được với `ORDER BY`, `GROUP BY`, hoặc range hay không.

**Cột nào hay dùng trong query khác** là tiêu chí rất thực tế. Ví dụ hệ thống thường có các query:

```sql
WHERE user_id = 10 AND status = 'PAID'
WHERE user_id = 10 ORDER BY created_at DESC
WHERE user_id = 10 AND created_at >= '2026-01-01'
```

Khi đó `user_id` nên đứng đầu nhiều index hơn, vì nó là pattern truy cập chính của ứng dụng. Nếu tạo `INDEX(status, user_id)`, index này không giúp nhiều cho query chỉ có `user_id`.

**Cột nào giúp nối tiếp với ORDER BY hoặc GROUP BY** nghĩa là sau khi các cột equality đã cố định, phần còn lại của index có thể phục vụ sắp xếp hoặc gom nhóm.

Ví dụ:

```sql
WHERE user_id = 10
  AND status = 'PAID'
ORDER BY created_at DESC
LIMIT 20
```

Index tốt có thể là:

```sql
INDEX(user_id, status, created_at)
```

Vì `user_id` và `status` là equality, sau đó `created_at` vẫn giữ được thứ tự hữu ích cho `ORDER BY`.

Nếu query phổ biến hơn là:

```sql
WHERE user_id = 10
ORDER BY created_at DESC
LIMIT 20
```

thì `INDEX(user_id, created_at)` có thể đáng giá hơn một index bắt đầu bằng `status`.

**Cột nào có kích thước nhỏ hơn** là tiêu chí phụ, và phải hiểu cẩn thận.

Nếu bạn chỉ đổi thứ tự giữa hai index có cùng tập cột:

```sql
INDEX(tenant_id, email)
INDEX(email, tenant_id)
```

thì tổng kích thước mỗi entry gần như vẫn chứa cả `tenant_id` và `email`. Nói cách khác, chỉ đổi thứ tự không tự nhiên làm index nhỏ đi nhiều. Index gọn hơn chủ yếu đến từ việc **chọn tập cột nào được đưa vào index**, không phải chỉ từ việc đảo thứ tự hai cột đã có sẵn.

Ví dụ:

```text
tenant_id BIGINT      -> nhỏ, cố định
email VARCHAR(255)    -> dài hơn, phụ thuộc charset
```

Nếu có hai hướng thiết kế đều phục vụ được workload:

```sql
INDEX(tenant_id, created_at)
```

so với:

```sql
INDEX(email, created_at)
```

và `tenant_id` với `email` có selectivity tương đương trong workload cụ thể, index bắt đầu bằng `tenant_id` thường gọn hơn vì `tenant_id` nhỏ hơn `email`.

Index gọn hơn nghĩa là:

- nhiều entry hơn nằm trong một page;
- ít tốn buffer pool hơn;
- scan index rẻ hơn;
- write vào index cũng nhẹ hơn.

Nhưng nếu query luôn cần cả hai cột equality:

```sql
WHERE tenant_id = 1 AND email = 'a@example.com'
```

thì:

```sql
INDEX(tenant_id, email)
```

và:

```sql
INDEX(email, tenant_id)
```

đều lưu cả hai cột. Khi đó tiêu chí “cột nhỏ hơn” không phải lý do chính. Lý do chính để chọn thứ tự là:

- query khác có dùng được prefix bên trái không;
- cột nào selective hơn khi chỉ dùng một phần index;
- cột nào nối tiếp tốt hơn với range, `ORDER BY`, hoặc `GROUP BY`;
- thống kê và runtime thực tế.

Tóm lại:

```text
Đổi thứ tự cùng hai cột
-> thường không làm index nhỏ hơn đáng kể

Chọn index dùng cột nhỏ hơn thay vì cột lớn hơn
-> có thể làm index gọn hơn rõ rệt
```

### 4.2. Điều kiện không được tính là equality đơn giản

Các biểu thức sau không nên xem là `column = constant` đơn giản cho cookbook này:

```sql
DATE(created_at) = '2026-01-01'
LOWER(email) = 'a@example.com'
CAST(id AS CHAR) = '123'
col1 + 1 = 10
```

Lý do: cột bị bọc trong function/expression, B-Tree index thông thường trên cột gốc khó dùng trực tiếp như equality.

Tư duy:

```text
column = constant
-> tốt cho index

function(column) = constant
-> thường làm mất khả năng dùng index thông thường trên column
```

Nếu cần query kiểu này thường xuyên, cân nhắc:

- generated column;
- functional index nếu phiên bản hỗ trợ;
- rewrite điều kiện về range trên cột gốc;
- chuẩn hóa dữ liệu khi ghi.

### 4.3. Chỉ xét cột của bảng hiện tại

Với join:

```sql
WHERE t1.a = 123 AND t2.b = 456
```

Khi thiết kế index cho `t1`, chỉ xét `t1.a`.

Khi thiết kế index cho `t2`, chỉ xét `t2.b`.

Không tạo một index trộn cột từ nhiều bảng, vì index thuộc về một bảng cụ thể.

## 5. Bước 2: chọn một hướng tiếp theo

Sau khi lấy các cột equality, bạn thường chỉ có thêm **một cơ hội chính** để mở rộng index.

Chọn hướng đầu tiên phù hợp trong các hướng sau:

```text
2a. Một cột range.
2b. Các cột GROUP BY.
2c. Các cột ORDER BY.
```

Lý do là B-Tree index có giới hạn tự nhiên: sau khi gặp range, các cột phía sau thường không còn giúp lọc sâu theo kiểu seek chính xác nữa.

Nói cụ thể hơn, composite index không phải là một tập cột để database dùng tùy ý. Nó là một thứ tự sắp xếp cụ thể.

Ví dụ:

```sql
INDEX(status, created_at, amount)
```

Index này được sắp theo thứ tự:

```text
status trước
-> trong cùng status, sắp theo created_at
-> trong cùng status + created_at, sắp theo amount
```

Với query:

```sql
WHERE status = 'PAID'
  AND created_at >= '2026-01-01'
  AND amount > 1000
```

Database dùng rất tốt:

```text
status = 'PAID'
-> seek vào đúng vùng PAID

created_at >= '2026-01-01'
-> bắt đầu range scan từ ngày đó
```

Nhưng sau khi `created_at` đã là range, các row được đọc theo nhiều giá trị `created_at` khác nhau. `amount` phía sau không còn tạo thành một vùng liên tục toàn cục để nhảy thẳng tới `amount > 1000`.

Nó thường trở thành điều kiện lọc thêm trong lúc scan:

```text
seek tới đầu range created_at
-> scan nhiều index entry
-> với từng entry, kiểm tra amount > 1000
```

Vì vậy sau các cột equality, bạn thường phải chọn một hướng chính:

```text
chọn range
-> tối ưu lọc theo khoảng

chọn GROUP BY
-> tối ưu gom nhóm, giảm sort/temp table

chọn ORDER BY
-> tối ưu thứ tự đọc, đặc biệt khi có LIMIT
```

Không phải các cột sau range hoàn toàn vô dụng. Chúng vẫn có thể giúp covering index hoặc index condition pushdown trong một số trường hợp. Nhưng chúng thường không còn giúp giảm vùng scan bằng seek chính xác như các cột đứng trước range.

## 6. Bước 2a: một cột range

Range gồm các dạng:

```sql
col > 10
col >= 10
col < 10
col <= 10
col BETWEEN 10 AND 20
col LIKE 'abc%'
col IS NOT NULL
```

Không tính:

```sql
col != 10
col <> 10
col LIKE '%abc'
```

Ví dụ:

```sql
WHERE status = 'PAID' AND created_at >= '2026-01-01'
```

Index tốt:

```sql
INDEX(status, created_at)
```

`status` là equality, `created_at` là range.

Ví dụ:

```sql
WHERE created_at >= '2026-01-01' AND status = 'PAID'
```

Về logic SQL, thứ tự trong `WHERE` không quan trọng. Index vẫn nên là:

```sql
INDEX(status, created_at)
```

Không nên là:

```sql
INDEX(created_at, status)
```

nếu mục tiêu là lọc tốt nhất theo cookbook này, vì `created_at` là range và đứng trước sẽ làm `status` phía sau kém hữu ích hơn.

### 6.1. Chỉ một range chính

Nếu có nhiều range:

```sql
WHERE a >= 10 AND b >= 20
```

Thường chỉ một range được dùng hiệu quả trong composite index cho seek/range scan.

Lý do là thứ tự của B-Tree chỉ ưu tiên cột bên trái trước.

Giả sử có:

```sql
INDEX(a, b)
```

Index sẽ được sắp như sau:

```text
a=10, b=1
a=10, b=50
a=10, b=99
a=11, b=2
a=11, b=80
a=12, b=3
...
```

Với query:

```sql
WHERE a >= 10 AND b >= 20
```

Database có thể dùng `a >= 10` để tìm điểm bắt đầu trong index. Nhưng sau đó nó phải scan qua nhiều giá trị `a`. Trong mỗi nhóm `a`, `b` có thứ tự, nhưng trên toàn bộ vùng `a >= 10`, điều kiện `b >= 20` không còn là một đoạn liên tục duy nhất.

Nói cách khác:

```text
INDEX(a, b)
-> dùng tốt range trên a
-> b >= 20 thường là filter phụ trong quá trình scan
```

Nếu đổi sang:

```sql
INDEX(b, a)
```

thì database dùng tốt range trên `b`, còn `a >= 10` trở thành filter phụ.

Vì vậy với nhiều range, câu hỏi thực tế không phải là “nhét cả hai range vào index được không?”, mà là:

```text
range nào nên làm range chính?
range nào chấp nhận làm filter phụ?
```

Bạn có thể chọn:

```sql
INDEX(a)
```

hoặc:

```sql
INDEX(b)
```

hoặc nếu có equality trước:

```sql
WHERE status = 'PAID' AND a >= 10 AND b >= 20
```

có thể cân nhắc:

```sql
INDEX(status, a)
```

hoặc:

```sql
INDEX(status, b)
```

Chọn cột range nào phụ thuộc vào:

- cột nào selective hơn;
- cột nào giảm row nhiều hơn;
- cột nào phục vụ `ORDER BY`;
- statistics thực tế;
- `EXPLAIN` và runtime.

Ví dụ thực tế:

```sql
SELECT *
FROM orders
WHERE created_at >= '2026-01-01'
  AND amount >= 1000;
```

Giả sử bảng có 10 triệu row:

```text
created_at >= '2026-01-01' -> match 4 triệu row
amount >= 1000             -> match 50 nghìn row
```

Nếu mục tiêu chính là lọc ít row nhất, `amount` là range chính tốt hơn:

```sql
INDEX(amount)
```

Lý do không phải là hai plan “đằng nào cũng đọc cùng số page”. Với range scan, cột đứng đầu index quyết định đoạn index phải quét.

Nếu dùng:

```sql
INDEX(created_at)
```

database phải đi vào vùng:

```text
created_at >= '2026-01-01'
```

Vùng này có khoảng 4 triệu index entry. Sau đó `amount >= 1000` chỉ là điều kiện kiểm tra thêm:

```text
scan khoảng 4 triệu entry theo created_at
-> với từng entry, kiểm tra amount >= 1000
-> còn lại khoảng 50 nghìn row phù hợp
```

Nếu dùng:

```sql
INDEX(amount)
```

database đi vào vùng:

```text
amount >= 1000
```

Vùng này có khoảng 50 nghìn index entry. Nếu index chỉ có `amount`, thì `created_at` không nằm trong index. Database phải lấy row tương ứng rồi mới kiểm tra `created_at >= '2026-01-01'`.

```text
scan khoảng 50 nghìn entry theo amount
-> lấy row thật hoặc dữ liệu cần thiết
-> kiểm tra created_at >= '2026-01-01'
```

Nếu index là:

```sql
INDEX(amount, created_at)
```

thì `created_at` có nằm trong index, nên database có thể kiểm tra điều kiện `created_at >= '2026-01-01'` ngay trên index entry. Nhưng điều đó **không có nghĩa** là `created_at` được dùng để seek nhanh như cột đầu tiên.

Lý do là index được sắp theo thứ tự:

```text
amount trước
-> trong cùng một amount, mới sắp theo created_at
```

Ví dụ các entry trong index có dạng:

```text
(amount=1000, created_at='2025-01-01')
(amount=1000, created_at='2026-02-01')
(amount=1001, created_at='2024-05-01')
(amount=1001, created_at='2026-03-01')
(amount=1002, created_at='2025-07-01')
(amount=1002, created_at='2026-04-01')
...
```

Điều kiện:

```sql
amount >= 1000 AND created_at >= '2026-01-01'
```

không tạo thành một đoạn liên tục đơn giản theo thứ tự `(amount, created_at)`.

Với từng giá trị `amount`, `created_at` có thứ tự. Nhưng vì `amount >= 1000` là một range mở rộng qua rất nhiều giá trị `amount`, database không thể nhảy một lần tới “mọi row có `created_at >= '2026-01-01'`” trong toàn bộ vùng đó.

Nó thường phải làm theo kiểu:

```text
seek tới đầu vùng amount >= 1000
-> scan các index entry trong vùng amount
-> entry nào có created_at >= '2026-01-01' thì giữ lại
-> entry nào không đạt thì bỏ qua
```

Điểm quan trọng là **selectivity của điều kiện range đầu tiên**.

Selectivity ở đây nghĩa là: sau khi áp dụng điều kiện trên cột đầu của index, còn bao nhiêu index entry phải đi qua.

Ví dụ cùng là range trên `amount`, nhưng phân phối dữ liệu có thể rất khác:

```text
Điều kiện A trên amount
-> match 50 nghìn entry / 10 triệu row
-> selectivity cao
-> vùng scan hẹp

Điều kiện B trên amount
-> match 4 triệu entry / 10 triệu row
-> selectivity thấp
-> vùng scan rộng
```

Điều kiện A tốt hơn cho `INDEX(amount, created_at)` vì nó làm vùng scan theo `amount` ngắn hơn. Không nên suy luận theo kiểu giá trị literal lớn hay nhỏ; phải suy luận theo **tỷ lệ dữ liệu thực tế bị match**.

Tư duy đúng:

```text
điều kiện range đầu tiên selective hơn
-> ít index entry phải scan hơn
-> ít index page phải đọc hơn
-> ít row thật phải lookup hơn nếu query cần đọc thêm cột
-> cột phía sau có ít candidate hơn để filter
```

Với `INDEX(amount, created_at)`, nếu điều kiện trên `amount` match 50 nghìn entry, database scan khoảng vùng đó rồi kiểm tra `created_at` trong các entry candidate. Nếu điều kiện trên `amount` match 4 triệu entry, database phải scan vùng lớn hơn rất nhiều, dù `created_at` có nằm trong index.

Một số optimizer có thể áp dụng kỹ thuật tối ưu phụ như index condition pushdown để bỏ bớt row không đạt ngay tại tầng storage engine. Nhưng về bản chất, `created_at` sau một range trên `amount` vẫn không biến thành một điều kiện seek toàn cục độc lập.

Nếu điều kiện đầu là equality thì khác:

```sql
WHERE amount = 1000
  AND created_at >= '2026-01-01'
```

Với index:

```sql
INDEX(amount, created_at)
```

database có thể seek chính xác hơn:

```text
amount = 1000
-> trong đúng nhóm amount=1000
-> seek tiếp tới created_at >= '2026-01-01'
```

So sánh hai case:

```sql
WHERE amount = 1000
  AND created_at >= '2026-01-01'
```

với:

```sql
WHERE amount >= 1000
  AND created_at >= '2026-01-01'
```

Case `amount = 1000` tốt hơn cho cột thứ hai vì `amount` cố định một nhóm duy nhất. Trong nhóm đó, `created_at` là thứ tự tiếp theo của index, nên database có thể range scan theo `created_at`.

Case `amount >= 1000` mở ra rất nhiều nhóm `amount`:

```text
amount=1000
amount=1001
amount=1002
...
```

Mỗi nhóm có thứ tự `created_at` riêng, nhưng toàn bộ vùng `amount >= 1000` không được sắp xếp theo `created_at` một cách toàn cục. Vì vậy database không thể chỉ nhảy tới một điểm `created_at >= '2026-01-01'` cho toàn bộ range này.

Tóm tắt:

```text
INDEX(amount, created_at)

amount = constant
-> created_at dùng range rất tốt trong nhóm amount đó

amount >= constant
-> amount đã là range
-> created_at chủ yếu giúp filter/covering/ICP trong lúc scan
-> điều kiện amount càng selective thì càng tốt
```

Đây là lý do cookbook nói:

```text
equality trước
-> các cột sau vẫn dùng tốt

range trước
-> các cột sau thường chỉ còn hỗ trợ filter/covering/ICP
```

Vậy khác biệt nằm ở **số index entry và index page phải scan trước khi filter phụ được áp dụng**.

Tư duy đúng:

```text
Cột range đứng đầu index
-> quyết định đoạn B-Tree phải quét

Range selective hơn
-> đoạn quét ngắn hơn
-> ít index page hơn
-> ít lookup về row thật hơn nếu cần đọc thêm cột
```

Nếu query là `SELECT *`, sau khi tìm index entry, database thường còn phải đọc row thật trong clustered index. Khi scan 4 triệu candidate thay vì 50 nghìn candidate, số lần phải cân nhắc/đọc row thật cũng có thể lớn hơn rất nhiều, đặc biệt nếu điều kiện còn lại không được kiểm tra hoàn toàn trong index.

Điểm cần tách bạch:

```text
số row kết quả cuối cùng
-> có thể giống nhau

số row/index entry phải đi qua để tìm ra kết quả đó
-> có thể khác rất xa
```

hoặc nếu có equality:

```sql
WHERE status = 'PAID'
  AND created_at >= '2026-01-01'
  AND amount >= 1000
```

có thể cân nhắc:

```sql
INDEX(status, amount)
```

Nhưng nếu query là:

```sql
WHERE created_at >= '2026-01-01'
  AND amount >= 1000
ORDER BY created_at DESC
LIMIT 20
```

thì `created_at` có thể đáng chọn hơn, vì nó vừa là range vừa phục vụ `ORDER BY ... LIMIT`. Database có thể đọc theo thứ tự thời gian và dừng sớm khi đủ 20 row phù hợp.

Tóm lại:

```text
range selective nhất
-> thường tốt cho lọc nhiều row

range khớp ORDER BY + LIMIT
-> có thể tốt hơn dù lọc kém hơn

range theo thống kê sai
-> optimizer có thể chọn nhầm, phải kiểm chứng bằng EXPLAIN và runtime
```

## 7. Bước 2b: GROUP BY

Nếu không dùng range làm hướng chính, hoặc query phù hợp hơn với grouping, thêm các cột `GROUP BY` vào index theo đúng thứ tự.

Ý chính: `GROUP BY` hưởng lợi khi database đọc dữ liệu đã được sắp theo đúng thứ tự group. Nếu các row cùng group nằm gần nhau trong index, database có thể gom nhóm theo dòng chảy của index thay vì phải gom một tập row lộn xộn rồi sort hoặc tạo temporary table lớn.

Ví dụ:

```sql
WHERE status = 'PAID'
GROUP BY customer_id, product_id
```

Index tốt:

```sql
INDEX(status, customer_id, product_id)
```

Lợi ích:

- lọc theo `status`;
- dữ liệu đã được đọc theo thứ tự group;
- giảm hoặc tránh sort/temp table cho grouping trong một số plan.

Ví dụ thực tế: bảng `orders` có nhiều triệu row, query thống kê doanh thu theo khách hàng:

```sql
SELECT customer_id, COUNT(*), SUM(total_amount)
FROM orders
WHERE status = 'PAID'
GROUP BY customer_id;
```

Index:

```sql
INDEX(status, customer_id)
```

có hai tác dụng:

```text
status = 'PAID'
-> giới hạn vùng dữ liệu cần đọc

customer_id
-> trong vùng PAID, dữ liệu đã đi theo thứ tự customer_id
```

Khi đó các row của cùng `customer_id` có xu hướng được đọc liền nhau trong index. Database có thể cộng dồn group hiện tại rồi chuyển sang group kế tiếp.

Nếu group nhiều cột:

```sql
WHERE tenant_id = 1
GROUP BY customer_id, product_id
```

Index phù hợp:

```sql
INDEX(tenant_id, customer_id, product_id)
```

`tenant_id` là equality. Sau đó `customer_id, product_id` giữ đúng thứ tự group.

Nhưng nếu có range trước group:

```sql
WHERE tenant_id = 1
  AND created_at >= '2026-01-01'
GROUP BY customer_id;
```

Index:

```sql
INDEX(tenant_id, created_at, customer_id)
```

không còn giúp `GROUP BY customer_id` mạnh như bạn kỳ vọng. Sau `created_at` range, dữ liệu được đọc theo thứ tự thời gian, không phải theo thứ tự `customer_id` toàn cục.

Trong trường hợp này có hai hướng thiết kế khác nhau:

```text
INDEX(tenant_id, created_at)
-> ưu tiên lọc theo thời gian
-> GROUP BY có thể phải xử lý thêm

INDEX(tenant_id, customer_id)
-> ưu tiên gom nhóm theo customer_id
-> created_at có thể thành filter phụ
```

Chọn hướng nào phụ thuộc vào workload:

- nếu range thời gian lọc rất mạnh, ưu tiên `created_at`;
- nếu query thống kê theo group là đường nóng và range thời gian rộng, ưu tiên thứ tự `GROUP BY` có thể tốt hơn;
- nếu cả hai đều quan trọng, cần so sánh bằng `EXPLAIN` và runtime thực tế.

### 7.1. GROUP BY expression

Nếu `GROUP BY` dùng expression/function:

```sql
GROUP BY DATE(created_at)
```

Index thông thường trên `created_at` không giống như index trên `DATE(created_at)`.

Cookbook này coi đó là trường hợp không dùng được GROUP BY trực tiếp để thêm vào index, trừ khi có:

- generated column;
- functional index;
- cột đã được materialize sẵn.

## 8. Bước 2c: ORDER BY

Nếu query có `ORDER BY`, có thể thêm các cột `ORDER BY` vào index theo đúng thứ tự.

Ví dụ:

```sql
WHERE status = 'PAID'
ORDER BY created_at
```

Index tốt:

```sql
INDEX(status, created_at)
```

Lợi ích:

- lọc theo `status`;
- rows được đọc theo thứ tự `created_at`;
- có thể tránh sort;
- đặc biệt mạnh khi có `LIMIT`.

### 8.1. ORDER BY và LIMIT

`ORDER BY ... LIMIT` là case rất quan trọng.

Ví dụ:

```sql
WHERE status = 'PAID'
ORDER BY created_at
LIMIT 10
```

Nếu index phù hợp:

```sql
INDEX(status, created_at)
```

database có thể tìm `status = 'PAID'`, sau đó lấy 10 row đầu theo `created_at` mà không cần gom nhiều row rồi sort.

Không có index phù hợp:

```text
lọc nhiều row
-> sort theo created_at
-> lấy 10 row đầu
```

Tốn hơn rất nhiều nếu số row sau lọc lớn.

### 8.2. ASC/DESC lẫn lộn

Nếu `ORDER BY` có nhiều cột và trộn chiều:

```sql
ORDER BY a ASC, b DESC
```

index có thể khó giúp đầy đủ, tùy phiên bản và khả năng descending index. Cookbook đơn giản này coi đây là case cần kiểm tra kỹ bằng `EXPLAIN`, không tự động thêm toàn bộ `ORDER BY` như trường hợp cùng chiều.

### 8.3. ORDER BY expression

Nếu `ORDER BY` dùng expression:

```sql
ORDER BY DATE(created_at)
ORDER BY a + b
```

index thường trên cột gốc không dùng trực tiếp để tránh sort. Cần cân nhắc generated column hoặc functional index nếu query quan trọng.

## 9. Kết thúc thuật toán

Sau các bước:

```text
equality columns
-> một range hoặc GROUP BY hoặc ORDER BY
```

bạn có candidate index.

Ví dụ:

```sql
WHERE status = 'PAID'
  AND user_id = 10
  AND created_at >= '2026-01-01'
```

Candidate:

```sql
INDEX(status, user_id, created_at)
```

hoặc:

```sql
INDEX(user_id, status, created_at)
```

Tùy query khác và selectivity, nhưng điểm chính là equality trước, range sau.

## 10. Khi thuật toán có thể sai

### 10.1. Low cardinality

Index trên cột có rất ít giá trị thường không hữu ích nếu đứng một mình.

Ví dụ:

```sql
WHERE flag = true
```

Nếu `flag = true` chiếm 50% bảng, index `flag` thường không tốt. Optimizer có thể thích full table scan hơn vì:

```text
dùng index flag
-> đọc rất nhiều index entries
-> quay lại đọc rất nhiều row
-> random lookup nhiều

full scan
-> đọc tuần tự cả bảng
-> có thể rẻ hơn
```

Rule of thumb:

```text
Cột chỉ có vài giá trị và match hơn khoảng 20-30% bảng
-> index đơn trên cột đó thường không đáng giá.
```

Nhưng low cardinality vẫn có thể hữu ích khi là prefix của composite index:

```sql
WHERE flag = true AND created_at >= '2026-01-01'
```

Index:

```sql
INDEX(flag, created_at)
```

có thể tốt nếu `created_at` tiếp tục giảm rất nhiều row.

### 10.2. UPDATE/INSERT phải trả giá cho index

Index giúp `SELECT`, nhưng làm chậm write.

Mỗi lần:

- `INSERT`;
- `DELETE`;
- `UPDATE` cột nằm trong index;

database phải cập nhật B-Tree index tương ứng.

Ví dụ:

```sql
INDEX(x)
UPDATE t SET x = ... WHERE ...;
```

Update `x` nghĩa là:

```text
xóa entry cũ khỏi index
thêm entry mới vào index
```

Nếu index là:

```sql
INDEX(z, x)
```

update `x` vẫn phải cập nhật index đó.

Vì vậy không nên tạo index chỉ vì một query hiếm khi chạy, nếu index đó làm chậm write path chính.

## 11. Giới hạn và rule of thumb

Các giới hạn phụ thuộc version, engine, row format, charset và page size, nhưng có vài rule thực dụng:

- không nên tạo index quá rộng;
- không nên nhét quá nhiều cột vào một index;
- rule of thumb: khoảng 5 cột là ngưỡng nên bắt đầu nghi ngờ;
- cột text/varchar dài có thể cần prefix index;
- prefix index không thể làm covering index đầy đủ;
- nên tránh redundant index;
- index càng nhiều thì insert/update/delete càng đắt.

Với charset nhiều byte, độ dài index có thể tăng nhanh.

Ví dụ:

```text
VARCHAR(255) utf8mb4
-> tối đa 255 * 4 bytes
-> index rộng hơn nhiều so với tưởng tượng
```

### 11.1. Prefix index là gì?

Prefix index là index chỉ lấy **một phần đầu** của giá trị cột, thay vì index toàn bộ giá trị.

Nó thường dùng với cột dài như:

- `VARCHAR` dài;
- `TEXT`;
- `BLOB`;
- URL;
- email rất dài;
- user agent;
- path;
- token dạng chuỗi dài.

Ví dụ:

```sql
INDEX(url(100))
```

Nghĩa là index chỉ lưu 100 ký tự đầu của `url`.

Nếu row có các giá trị:

```text
https://example.com/products/iphone-15-pro-max?...
https://example.com/products/macbook-pro-16?...
https://example.com/blog/database-indexing?...
```

thì index không lưu toàn bộ chuỗi URL, mà chỉ lưu phần prefix đầu tiên theo độ dài đã chọn.

Mục tiêu của prefix index:

```text
cột quá dài
-> index full quá tốn dung lượng hoặc không phù hợp
-> index một phần đầu đủ phân biệt
-> giảm kích thước index
```

### 11.2. Vì sao prefix index có thể hữu ích?

Index càng rộng thì càng tốn:

- disk;
- buffer pool;
- CPU khi so sánh key;
- chi phí insert/update/delete;
- số page phải đọc khi scan index.

Ví dụ bảng `access_logs` có cột:

```sql
url TEXT
```

Nếu query thường tìm URL chính xác:

```sql
WHERE url = 'https://example.com/products/iphone-15-pro-max?...'
```

Index toàn bộ `url` có thể quá rộng. Prefix index:

```sql
INDEX(url(128))
```

có thể giúp database lọc nhanh các row có 128 ký tự đầu giống nhau. Sau đó database vẫn kiểm tra lại giá trị `url` đầy đủ ở row thật để đảm bảo match chính xác.

Luồng xử lý về mặt concept:

```text
dùng prefix trong index để tìm candidate rows
-> đọc row thật
-> so sánh toàn bộ giá trị url
-> trả row đúng
```

Prefix index vì vậy là một cách giảm chi phí index, không phải là cách thay đổi logic so sánh của SQL.

### 11.3. Khi nào prefix index kém hiệu quả?

Prefix index kém nếu phần đầu của chuỗi giống nhau quá nhiều.

Ví dụ nhiều URL đều bắt đầu bằng:

```text
https://example.com/products/
```

Nếu tạo:

```sql
INDEX(url(20))
```

thì 20 ký tự đầu có thể gần như giống nhau cho phần lớn row. Index này có selectivity thấp, vì nó không phân biệt được nhiều.

Khi đó database vẫn phải đọc rất nhiều candidate rows:

```text
prefix match rất nhiều row
-> đọc row thật rất nhiều
-> kiểm tra full url rất nhiều
-> lợi ích thấp
```

Chọn prefix length cần cân bằng:

```text
prefix quá ngắn
-> index nhỏ nhưng kém selective

prefix quá dài
-> selective tốt hơn nhưng index to hơn

prefix vừa đủ
-> index còn gọn và vẫn lọc tốt
```

Ví dụ trực giác:

```text
url(10)   -> hầu như toàn "https://..."
url(30)   -> phân biệt được domain/path đầu
url(100)  -> có thể đủ phân biệt phần lớn URL
url(255)  -> selective hơn nhưng index lớn hơn
```

Không có số cố định đúng cho mọi bảng. Phải nhìn dữ liệu thật.

### 11.4. Prefix index không phải covering index đầy đủ

Đây là điểm rất dễ nhầm.

Giả sử có:

```sql
INDEX(url(100))
```

Query:

```sql
SELECT url
FROM access_logs
WHERE url = 'https://example.com/products/iphone-15-pro-max?...';
```

Index chỉ chứa 100 ký tự đầu của `url`, không chứa toàn bộ `url`. Vì vậy nó không thể cover đầy đủ cột `url`.

Database vẫn cần đọc row thật để:

- lấy toàn bộ giá trị `url`;
- kiểm tra phần sau ký tự thứ 100;
- trả kết quả chính xác.

Tóm lại:

```text
INDEX(url)
-> có thể cover url nếu index full và query chỉ cần các cột trong index

INDEX(url(100))
-> chỉ có prefix của url
-> không cover đầy đủ url
```

Prefix index phù hợp để lọc candidate rows, nhưng không phù hợp nếu mục tiêu chính là covering index đầy đủ.

### 11.5. Prefix index nên dùng trong tình huống nào?

Nên cân nhắc prefix index khi:

- cột quá dài để index full một cách hiệu quả;
- query lọc theo equality hoặc prefix của chuỗi;
- phần đầu của chuỗi đủ phân biệt;
- query không cần index đó cover toàn bộ giá trị cột;
- workload bị tốn RAM/disk vì index quá rộng.

Ví dụ thường hợp lý:

```sql
WHERE url = '...'
WHERE path = '...'
WHERE user_agent = '...'
```

với điều kiện prefix đã đủ selective.

Ví dụ thường không cần prefix:

```sql
WHERE email = 'a@example.com'
```

Nếu `email` không quá dài và cần unique chính xác, index full trên `email` thường rõ ràng hơn. Prefix index trên email có thể gây nhiều candidate trùng prefix và không đảm bảo unique toàn bộ email nếu dùng sai mục đích.

Quy tắc thực dụng:

```text
Prefix index là giải pháp cho cột dài.
Không dùng prefix index chỉ vì muốn "index nhỏ hơn" nếu full index vẫn gọn và rõ ràng.
```

## 12. Covering index

### 12.1. Covering index là gì?

Covering index là index chứa đủ các cột mà query cần.

Ví dụ:

```sql
SELECT name
FROM users
WHERE email = 'a@example.com';
```

Nếu có:

```sql
INDEX(email, name)
```

database có thể đọc `email` để tìm row và lấy luôn `name` từ index, không cần quay lại row thật.

Concept:

```text
index chứa đủ dữ liệu query cần
-> query có thể hoàn tất trong index
-> giảm lookup về clustered primary key/table row
```

### 12.2. Cách tạo covering index theo cookbook

Mini-cookbook:

1. Chọn cột theo thuật toán chính: equality, range/group/order.
2. Thêm các cột còn lại trong `SELECT` vào cuối index.

Ví dụ:

```sql
SELECT x
FROM t
WHERE y = 5;
```

Cookbook chính nói:

```sql
INDEX(y)
```

Covering index:

```sql
INDEX(y, x)
```

Ví dụ:

```sql
SELECT x, z
FROM t
WHERE y = 5 AND q = 7;
```

Index:

```sql
INDEX(y, q, x, z)
```

`y`, `q` phục vụ lọc; `x`, `z` giúp cover SELECT.

### 12.3. Khi covering index không nên dùng

Covering index không miễn phí.

Không nên tạo index quá rộng chỉ để cover mọi query:

- index lớn hơn;
- tốn RAM hơn;
- tốn disk hơn;
- update/insert/delete chậm hơn;
- buffer pool chứa được ít page hơn;
- optimizer có thêm nhiều lựa chọn phức tạp hơn.

Không nên dùng prefix index trong covering index nếu cần cover đủ giá trị:

```sql
INDEX(name(10))
```

Index này chỉ chứa 10 ký tự đầu, không cover đầy đủ `name`.

Kết luận:

```text
Covering index rất mạnh cho query nóng.
Nhưng index rộng quá sẽ làm hệ thống write-heavy chậm đi.
```

## 13. Redundant và excessive indexes

### 13.1. Redundant index là gì?

Nếu có:

```sql
INDEX(a, b)
```

thì index này có thể phục vụ nhiều query mà `INDEX(a)` phục vụ được, vì `a` là prefix bên trái.

Vì vậy thường không cần cả hai:

```sql
INDEX(a)
INDEX(a, b)
```

Trong nhiều case, `INDEX(a)` là redundant và có thể bỏ.

### 13.2. Leftmost prefix rule

Composite index dùng tốt từ trái sang phải.

```sql
INDEX(a, b, c)
```

có thể hỗ trợ:

```sql
WHERE a = ...
WHERE a = ... AND b = ...
WHERE a = ... AND b = ... AND c = ...
```

nhưng không hỗ trợ tốt:

```sql
WHERE b = ...
WHERE c = ...
WHERE b = ... AND c = ...
```

nếu không có điều kiện trên `a`.

### 13.3. Quá nhiều index

Quá nhiều index gây:

- insert chậm hơn;
- update chậm hơn;
- delete chậm hơn;
- tốn disk;
- tốn buffer pool;
- optimizer phải cân nhắc nhiều plan hơn;
- maintenance phức tạp hơn.

Rule of thumb:

```text
Một bảng OLTP thường không nên có quá nhiều index.
Khoảng 5-6 index đã là mức cần xem lại.
```

Không phải luật cứng. Bảng read-heavy có thể có nhiều index hơn. Bảng write-heavy nên tiết chế hơn.

### 13.4. Gộp index cho nhiều query

Nếu có hai query:

```sql
WHERE a = 1 AND b = 2
```

và:

```sql
WHERE a > 1 AND b = 2
```

Query đầu có thể dùng:

```sql
INDEX(a, b)
```

hoặc:

```sql
INDEX(b, a)
```

Query thứ hai thường hợp hơn với:

```sql
INDEX(b, a)
```

vì `b` là equality, `a` là range.

Vậy nếu cần một index phục vụ cả hai, `INDEX(b, a)` có thể là lựa chọn tốt hơn.

## 14. Optimizer chọn ORDER BY thay vì WHERE

Optimizer đôi khi bỏ qua index phù hợp với `WHERE` và chọn index phù hợp với `ORDER BY`.

Điều này có thể đúng nếu:

- `WHERE` lọc ít;
- `ORDER BY ... LIMIT` cần trả vài row đầu nhanh;
- index theo `ORDER BY` giúp tránh sort lớn.

Ví dụ:

```sql
WHERE status IN ('A', 'B', 'C')
ORDER BY created_at
LIMIT 10
```

Nếu `status` lọc không nhiều, index:

```sql
INDEX(created_at)
```

có thể được optimizer chọn để đọc theo thứ tự rồi dừng sớm ở `LIMIT 10`.

Nhưng nếu `WHERE` lọc rất mạnh, index theo `WHERE` có thể tốt hơn:

```sql
INDEX(status, created_at)
```

Tư duy:

```text
WHERE lọc ít + LIMIT nhỏ
-> ORDER BY index có thể thắng

WHERE lọc mạnh
-> WHERE index thường thắng
```

Optimizer không phải lúc nào cũng đoán đúng. Với query quan trọng, phải kiểm tra `EXPLAIN`, `EXPLAIN ANALYZE` nếu có, và thời gian thực tế.

## 15. OR trong WHERE

`OR` là một trong các pattern dễ làm index kém hiệu quả, vì nó có thể khiến optimizer phải chọn giữa nhiều nhánh điều kiện khác nhau.

### 15.1. OR trên cùng một cột

Case này thường ổn:

```sql
WHERE status = 'PAID' OR status = 'REFUNDED'
```

Về mặt logic, nó tương đương:

```sql
WHERE status IN ('PAID', 'REFUNDED')
```

Index phù hợp:

```sql
INDEX(status)
```

Database có thể tìm nhiều giá trị trong cùng một index. Đây là case `OR` tương đối thân thiện.

### 15.2. OR trên nhiều cột khác nhau

Case khó hơn:

```sql
WHERE user_id = 10 OR email = 'a@example.com'
```

Nếu có:

```sql
INDEX(user_id)
INDEX(email)
```

optimizer có thể cân nhắc `index_merge`, tức đọc từ hai index rồi hợp nhất kết quả. Nhưng không nên mặc định rằng `index_merge` luôn nhanh. Nó có thể phải:

- đọc nhiều entry từ index `user_id`;
- đọc nhiều entry từ index `email`;
- hợp nhất row id;
- loại trùng;
- quay về đọc row thật.

Một cách rewrite thường rõ hơn là tách thành `UNION`:

```sql
SELECT ...
FROM users
WHERE user_id = 10

UNION DISTINCT

SELECT ...
FROM users
WHERE email = 'a@example.com';
```

Mỗi nhánh `SELECT` được optimize riêng:

```text
nhánh user_id
-> dùng INDEX(user_id)

nhánh email
-> dùng INDEX(email)
```

Dùng `UNION DISTINCT` nếu cần loại trùng. Nếu chắc chắn hai nhánh không trùng, `UNION ALL` có thể rẻ hơn.

### 15.3. OR qua nhiều bảng

Case này thường tệ hơn:

```sql
WHERE users.email = 'a@example.com'
   OR orders.order_code = 'A001'
```

Điều kiện nằm trên hai bảng khác nhau. Optimizer khó dùng một index đơn giản để xử lý cả hai nhánh. Thường nên xem lại logic query, hoặc tách thành `UNION` để mỗi nhánh bắt đầu từ bảng phù hợp.

Ví dụ concept:

```sql
SELECT ...
FROM users
JOIN orders ON orders.user_id = users.id
WHERE users.email = 'a@example.com'

UNION DISTINCT

SELECT ...
FROM users
JOIN orders ON orders.user_id = users.id
WHERE orders.order_code = 'A001';
```

Nhánh đầu có thể bắt đầu từ `users.email`. Nhánh sau có thể bắt đầu từ `orders.order_code`.

### 15.4. OR kèm ORDER BY và LIMIT

Nếu query gốc có:

```sql
ORDER BY created_at DESC
LIMIT 190, 10
```

rewrite bằng `UNION` phức tạp hơn. Mỗi nhánh không thể chỉ lấy 10 row, vì row sau khi merge/sort toàn cục có thể đến từ bất kỳ nhánh nào.

Pattern an toàn hơn về logic:

```sql
(
  SELECT ...
  FROM ...
  WHERE điều_kiện_1
  ORDER BY created_at DESC
  LIMIT 200
)
UNION DISTINCT
(
  SELECT ...
  FROM ...
  WHERE điều_kiện_2
  ORDER BY created_at DESC
  LIMIT 200
)
ORDER BY created_at DESC
LIMIT 190, 10;
```

Vì cần offset `190` và lấy `10`, mỗi nhánh phải lấy tối thiểu `190 + 10 = 200` candidate để không làm mất row có thể lọt vào trang kết quả cuối.

Tóm tắt:

```text
OR cùng cột
-> thường rewrite thành IN

OR khác cột
-> cân nhắc index_merge hoặc UNION

OR khác bảng
-> thường nên tách UNION để mỗi nhánh có plan riêng

OR + ORDER BY + LIMIT/OFFSET
-> rewrite UNION phải rất cẩn thận
```

## 16. TEXT, BLOB và prefix index

`TEXT`, `BLOB`, `VARCHAR` rất dài hoặc `VARBINARY` rất dài không nên được index một cách tùy tiện. Index toàn bộ giá trị dài làm index phình to, tốn buffer pool và tăng chi phí write.

Prefix index là cách index một phần đầu của cột:

```sql
INDEX(title(20))
```

Nghĩa là chỉ 20 ký tự đầu của `title` được đưa vào index.

Ví dụ:

```sql
INDEX(last_name(2), first_name)
```

Nếu `last_name = 'James'`, prefix 2 ký tự là:

```text
Ja
```

Index sẽ phân biệt kém giữa:

```text
James
Jamison
Jackson
Jarvis
```

Tức là prefix quá ngắn làm selectivity kém. Optimizer có thể bỏ qua index, hoặc dùng index nhưng vẫn phải kiểm tra rất nhiều row thật.

### 16.1. Prefix index và UNIQUE

Cẩn thận với unique prefix index:

```sql
UNIQUE INDEX uq_url(url(100))
```

Ràng buộc unique này chỉ unique trên 100 ký tự đầu, không phải toàn bộ `url`. Hai URL khác nhau nhưng giống 100 ký tự đầu có thể bị xem là trùng.

Vì vậy không nên dùng unique prefix index nếu mục tiêu là đảm bảo toàn bộ chuỗi unique, trừ khi bạn hiểu rõ dữ liệu và chấp nhận ràng buộc đó.

### 16.2. Khi nào prefix index đáng dùng?

Prefix index đáng cân nhắc khi:

- cột dài;
- query lọc theo equality hoặc prefix;
- prefix đủ selective;
- không cần covering đầy đủ giá trị cột;
- index full quá lớn hoặc không phù hợp.

Ví dụ:

```sql
WHERE url = 'https://example.com/products/abc...'
```

`INDEX(url(128))` có thể giúp tìm candidate row nhanh hơn. Nhưng database vẫn phải kiểm tra toàn bộ URL ở row thật.

## 17. Date, DATETIME và cách viết điều kiện để dùng index

Cột ngày giờ thường bị viết sai theo kiểu làm mất index.

Không nên viết:

```sql
WHERE created_at LIKE '2026-01%'
```

Nếu `created_at` là `DATE` hoặc `DATETIME`, cách này ép database xử lý như chuỗi hoặc chuyển đổi kiểu, không còn là range tự nhiên trên B-Tree ngày giờ.

Không nên viết:

```sql
WHERE LEFT(created_at, 7) = '2026-01'
```

hoặc:

```sql
WHERE DATE(created_at) = '2026-01-01'
```

Vấn đề là cột bị bọc trong function:

```text
function(column) = constant
-> index thường khó dùng trực tiếp trên column
```

Nên viết bằng range:

```sql
WHERE created_at >= '2026-01-01'
  AND created_at <  '2026-02-01'
```

Với index:

```sql
INDEX(created_at)
```

database có thể range scan đúng đoạn tháng 01/2026.

### 17.1. Vì sao dùng `< ngày kế tiếp` tốt hơn `23:59:59`

Không nên viết:

```sql
WHERE created_at BETWEEN '2026-01-01 00:00:00'
                     AND '2026-01-31 23:59:59'
```

Lý do:

- có thể sai nếu cột có microsecond;
- phải tính ngày cuối tháng;
- dễ lỗi với tháng có 28/29/30/31 ngày;
- khó compose khi cần quý/năm.

Pattern tốt:

```sql
WHERE created_at >= '2026-01-01'
  AND created_at <  '2026-02-01'
```

Cho quý:

```sql
WHERE created_at >= '2026-01-01'
  AND created_at <  '2026-04-01'
```

Tư duy:

```text
lower bound inclusive
upper bound exclusive
-> không mất dữ liệu ở phần microsecond
-> dễ tính khoảng thời gian
-> dùng index tốt
```

## 18. Đọc EXPLAIN key_len

`key_len` trong `EXPLAIN` cho biết optimizer dự kiến dùng bao nhiêu byte của index cho phần truy cập/lọc.

Ví dụ index:

```sql
INDEX(a, b, c)
```

Query:

```sql
WHERE a = 1 AND b = 2
```

Nếu `key_len` tương ứng với `a + b`, có thể hiểu optimizer đang dùng hai phần đầu của index.

Nhưng cần cẩn thận:

- `key_len` chủ yếu phản ánh phần dùng cho lookup/range trong `WHERE`;
- không nói rõ đầy đủ rằng `ORDER BY` hoặc `GROUP BY` có được xử lý bằng index hay không;
- với kiểu dữ liệu nullable, charset nhiều byte, prefix index, `key_len` có thể lớn hơn bạn tưởng;
- `EXPLAIN FORMAT=JSON` thường dễ đọc chi tiết hơn nếu có.

Tư duy:

```text
key_len nhỏ hơn kỳ vọng
-> có thể index không dùng hết các cột như bạn nghĩ

key_len đúng kỳ vọng
-> chưa đủ kết luận query tối ưu
-> vẫn phải xem rows, filtered, Extra và runtime
```

## 19. IN và IN subquery

`IN` có hai kiểu rất khác nhau:

```sql
WHERE status IN ('PAID', 'REFUNDED')
```

và:

```sql
WHERE user_id IN (
  SELECT user_id
  FROM vip_users
)
```

### 19.1. IN với danh sách hằng

`IN` với danh sách hằng thường tương tự nhiều equality trên cùng một cột:

```sql
WHERE status IN ('PAID', 'REFUNDED')
```

Index:

```sql
INDEX(status)
```

có thể dùng tốt nếu danh sách không quá rộng và selectivity còn ổn.

Nếu danh sách quá lớn hoặc match phần lớn bảng, optimizer có thể chọn scan thay vì index.

### 19.2. IN subquery

Query:

```sql
SELECT ...
FROM orders o
WHERE o.user_id IN (
  SELECT u.id
  FROM users u
  WHERE u.level = 'VIP'
);
```

Về concept, có thể rewrite thành join:

```sql
SELECT ...
FROM orders o
JOIN users u ON u.id = o.user_id
WHERE u.level = 'VIP';
```

Index cần nghĩ theo từng bảng:

```text
users
-> INDEX(level, id) hoặc INDEX(level)

orders
-> INDEX(user_id)
```

Trong các phiên bản hiện đại, optimizer có nhiều chiến lược semijoin/materialization tốt hơn ngày xưa. Nhưng tư duy index vẫn không đổi: mỗi bảng cần index phù hợp với cách nó được truy cập.

### 19.3. Khi materialized subquery nguy hiểm

Nếu subquery được materialize thành bảng tạm, bảng tạm đó không phải lúc nào cũng có index tối ưu như bảng thật. Join hai derived table/subquery lớn với nhau có thể rất chậm.

Tư duy:

```text
IN subquery nhỏ, selective
-> có thể ổn

IN subquery lớn, join phức tạp
-> cân nhắc rewrite thành JOIN
-> kiểm tra EXPLAIN
```

## 20. Explode và implode trong JOIN + GROUP BY

Explode/implode là pattern:

```text
JOIN làm số row phình to
-> GROUP BY hoặc DISTINCT gom lại về số row ban đầu
```

Ví dụ:

```sql
SELECT DISTINCT a.*
FROM articles a
JOIN article_tags at ON at.article_id = a.id
JOIN tags t ON t.id = at.tag_id
WHERE t.name IN ('mysql', 'mariadb');
```

Một article có nhiều tag. Sau join, một article có thể xuất hiện nhiều lần. `DISTINCT` dùng để gom lại.

Chi phí:

- join tạo nhiều row trung gian;
- sort/hash/deduplicate;
- tốn memory/temp table;
- khó giữ `ORDER BY ... LIMIT` hiệu quả.

Nếu chỉ cần kiểm tra tồn tại, dùng `EXISTS` thường đúng ý hơn:

```sql
SELECT a.*
FROM articles a
WHERE EXISTS (
  SELECT 1
  FROM article_tags at
  JOIN tags t ON t.id = at.tag_id
  WHERE at.article_id = a.id
    AND t.name IN ('mysql', 'mariadb')
);
```

Index cần:

```sql
article_tags(article_id, tag_id)
tags(name, id)
```

Nếu cần gom nhiều giá trị con thành một dòng, có thể dùng aggregate phụ:

```sql
SELECT a.*,
       (
         SELECT GROUP_CONCAT(t.name)
         FROM article_tags at
         JOIN tags t ON t.id = at.tag_id
         WHERE at.article_id = a.id
       ) AS tag_names
FROM articles a;
```

Tư duy:

```text
Nếu JOIN chỉ để kiểm tra có tồn tại hay không
-> EXISTS thường tốt hơn JOIN rồi GROUP BY

Nếu JOIN để lấy danh sách con
-> cân nhắc aggregate phụ hoặc query riêng

Nếu JOIN làm row phình to rồi GROUP BY để kéo lại
-> kiểm tra lại thiết kế query
```

## 21. Many-to-many mapping table

Bảng many-to-many nên được thiết kế để phục vụ truy cập theo cả hai chiều.

Ví dụ quan hệ user - role:

```sql
CREATE TABLE user_role (
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (user_id, role_id),
  INDEX (role_id, user_id)
) ENGINE=InnoDB;
```

Không cần surrogate `id` nếu bảng chỉ biểu diễn quan hệ giữa hai khóa.

Lý do:

```text
PRIMARY KEY(user_id, role_id)
-> tìm roles của một user rất nhanh
-> đảm bảo không trùng quan hệ user-role

INDEX(role_id, user_id)
-> tìm users có một role rất nhanh
```

Vì InnoDB cluster dữ liệu theo primary key, `PRIMARY KEY(user_id, role_id)` còn giúp các role của cùng một user nằm gần nhau.

Không nên thiết kế mặc định như:

```sql
id BIGINT AUTO_INCREMENT PRIMARY KEY,
user_id BIGINT,
role_id BIGINT,
INDEX(user_id),
INDEX(role_id)
```

nếu `id` không có ý nghĩa business. Thiết kế này:

- không tự chặn duplicate `(user_id, role_id)`;
- làm clustered primary key không phục vụ query chính;
- có thể cần thêm unique index sau đó;
- tốn storage hơn.

### 21.1. Có cần UNIQUE cho index chiều ngược không?

Nếu đã có:

```sql
PRIMARY KEY(user_id, role_id)
```

thì duplicate quan hệ đã bị chặn. Index chiều ngược:

```sql
INDEX(role_id, user_id)
```

không nhất thiết phải `UNIQUE`. Thêm `UNIQUE` có thể tăng chi phí write mà không thêm nhiều giá trị nếu primary key đã đảm bảo cặp là duy nhất.

## 22. Subquery và UNION

Mỗi `SELECT` trong `UNION` nên được xem như một query riêng để chọn index.

Ví dụ:

```sql
SELECT ...
FROM orders
WHERE user_id = 10

UNION ALL

SELECT ...
FROM orders
WHERE coupon_id = 5;
```

Hai nhánh cần index riêng:

```sql
INDEX(user_id)
INDEX(coupon_id)
```

Không nên cố tạo một composite index:

```sql
INDEX(user_id, coupon_id)
```

và kỳ vọng nó tối ưu cả hai nhánh, vì nhánh `coupon_id = 5` không dùng tốt prefix bên trái nếu không có `user_id`.

### 22.1. Correlated subquery

Correlated subquery phụ thuộc vào row bên ngoài:

```sql
SELECT u.*
FROM users u
WHERE EXISTS (
  SELECT 1
  FROM orders o
  WHERE o.user_id = u.id
    AND o.status = 'PAID'
);
```

Index quan trọng nằm ở bảng bên trong:

```sql
INDEX(user_id, status)
```

Vì với mỗi user candidate, database cần kiểm tra có order tương ứng hay không.

Tư duy:

```text
subquery độc lập
-> có thể materialize hoặc optimize riêng

correlated subquery
-> cần index tốt cho lookup lặp lại từ outer row
```

## 23. JOIN và thứ tự bảng

Với join, không thể chỉ nhìn một bảng. Cần hiểu optimizer sẽ bắt đầu từ bảng nào và đi sang bảng nào.

Ví dụ:

```sql
SELECT *
FROM orders o
JOIN users u ON u.id = o.user_id
WHERE u.email = 'a@example.com'
  AND o.status = 'PAID';
```

Một plan hợp lý:

```text
users
-> tìm user bằng email
-> lấy id
-> sang orders bằng user_id + status
```

Index:

```sql
users(email)
orders(user_id, status)
```

Nếu optimizer bắt đầu từ `orders`, index khác có thể cần:

```sql
orders(status, user_id)
users(id)
```

### 23.1. Bảng đầu tiên và bảng sau khác nhau

Với bảng đầu tiên, index có thể phục vụ:

- `WHERE`;
- `GROUP BY`;
- `ORDER BY`;
- `LIMIT`.

Với bảng thứ hai trở đi, index thường phục vụ:

- điều kiện `JOIN ON`;
- phần `WHERE` liên quan đến bảng đó.

`ORDER BY`/`GROUP BY` thường khó dùng trực tiếp trên bảng sau nếu thứ tự row đã bị quyết định bởi join từ bảng trước.

Ví dụ:

```sql
FROM users u
JOIN orders o ON o.user_id = u.id
WHERE u.email = 'a@example.com'
ORDER BY o.created_at DESC
LIMIT 10
```

Nếu chỉ có một user, `orders(user_id, created_at)` rất tốt:

```text
user_id = constant
-> đọc orders của user đó theo created_at
-> LIMIT 10
```

Nhưng nếu có nhiều users từ bảng đầu, thứ tự `o.created_at` toàn cục có thể phức tạp hơn.

### 23.2. Dùng EXPLAIN sau khi thêm index

Thêm index mới có thể làm optimizer đổi thứ tự join. Vì vậy sau khi thêm index, phải đọc lại:

- bảng nào đứng trước;
- access type là gì;
- key nào được chọn;
- rows estimate;
- có `Using temporary`, `Using filesort` không;
- runtime thực tế.

## 24. Partitioning không thay thế index tốt

Partitioning hiếm khi là giải pháp thay thế cho index tốt.

Partitioning có thể giúp nếu query luôn loại được nhiều partition bằng partition pruning.

Ví dụ:

```sql
PARTITION BY RANGE (YEAR(created_at))
```

Query:

```sql
WHERE created_at >= '2026-01-01'
  AND created_at <  '2027-01-01'
```

có thể chỉ đọc partition năm 2026.

Nhưng trong partition đó vẫn cần index tốt:

```sql
INDEX(created_at)
INDEX(user_id, created_at)
```

Nếu không có index, database vẫn scan cả partition.

Tư duy:

```text
partition pruning
-> giảm số partition phải chạm

index
-> giảm số row/page phải đọc bên trong partition
```

Partition hữu ích hơn trong các bài toán:

- dữ liệu rất lớn theo thời gian;
- xóa/archive theo partition;
- query luôn có điều kiện partition key;
- maintenance theo từng vùng dữ liệu.

Nó không sửa được query/index thiết kế kém.

## 25. FULLTEXT

`FULLTEXT` dùng cho tìm kiếm theo từ trong cột text. Nó khác với B-Tree index.

Query kiểu:

```sql
WHERE content LIKE '%database%'
```

không dùng B-Tree index tốt vì wildcard ở đầu `%database%` phá vỡ khả năng seek theo prefix.

FULLTEXT phù hợp hơn:

```sql
WHERE MATCH(title, content) AGAINST ('database optimization')
```

Nếu query có thêm điều kiện thường:

```sql
WHERE status = 'PUBLISHED'
  AND MATCH(title, content) AGAINST ('mariadb index')
```

optimizer có thể ưu tiên FULLTEXT trước, rồi filter `status`. Khi một điều kiện là `MATCH ... AGAINST`, cookbook equality/range thông thường không còn áp dụng nguyên xi.

Tư duy:

```text
tìm prefix: 'abc%'
-> B-Tree có thể dùng

tìm substring: '%abc%'
-> B-Tree thường không tốt

tìm theo từ/ngôn ngữ
-> FULLTEXT
```

FULLTEXT cũng có trade-off:

- cần index riêng;
- có rule tokenizer/stopword/min token length;
- không giống tìm substring tuyệt đối;
- ranking và matching phụ thuộc engine/cấu hình.

## 26. Dấu hiệu thiết kế index non tay

Một số dấu hiệu thường gặp:

- không có composite index, chỉ index từng cột đơn lẻ;
- bảng InnoDB không có primary key rõ ràng;
- có index redundant như `PRIMARY KEY(id)` và `KEY(id)`;
- index gần như mọi cột vì nghĩ “cứ index là nhanh”;
- dùng comma join kiểu cũ thay vì `JOIN ... ON`, làm logic join khó đọc;
- dùng function bọc cột trong `WHERE` rồi thắc mắc vì sao không dùng index;
- dùng `OR` nhiều nhánh khác cột nhưng không kiểm tra plan;
- tạo index rộng để cover query hiếm khi chạy;
- bỏ qua chi phí write của index.

## 27. Case thực tế: tối ưu `wp_postmeta`

`wp_postmeta` là ví dụ kinh điển của bảng key-value trong WordPress.

Thiết kế phổ biến:

```sql
CREATE TABLE wp_postmeta (
  meta_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  post_id BIGINT UNSIGNED NOT NULL DEFAULT 0,
  meta_key VARCHAR(255) DEFAULT NULL,
  meta_value LONGTEXT,
  PRIMARY KEY (meta_id),
  KEY post_id (post_id),
  KEY meta_key (meta_key)
) ENGINE=InnoDB;
```

Vấn đề:

- `meta_id` làm primary key nhưng phần lớn query lại truy cập theo `post_id` và `meta_key`;
- clustered data bị sắp theo `meta_id`, không theo pattern truy cập chính;
- `KEY(post_id)` chỉ giúp tìm toàn bộ meta của post, nhưng không tối ưu tốt query theo `post_id + meta_key`;
- `KEY(meta_key)` có thể rất kém nếu một meta_key xuất hiện nhiều;
- `meta_value LONGTEXT` không phù hợp để index full theo cách thông thường.

Query phổ biến:

```sql
SELECT meta_value
FROM wp_postmeta
WHERE post_id = 123
  AND meta_key = '_thumbnail_id';
```

Index tốt hơn:

```sql
PRIMARY KEY(post_id, meta_key)
```

hoặc nếu vẫn cần `meta_id` vì tương thích ứng dụng, ít nhất cần:

```sql
INDEX(post_id, meta_key)
```

Lợi ích của `PRIMARY KEY(post_id, meta_key)`:

```text
post_id = 123
-> các meta của cùng post nằm gần nhau trong clustered index

meta_key = '_thumbnail_id'
-> tìm đúng meta trong nhóm post
```

Nếu một post có nhiều meta cùng key, có thể cần thêm cột phân biệt vào primary key, hoặc giữ `meta_id`/surrogate tùy ràng buộc ứng dụng.

Một thiết kế concept:

```sql
CREATE TABLE wp_postmeta (
  post_id BIGINT UNSIGNED NOT NULL,
  meta_key VARCHAR(255) NOT NULL,
  meta_value LONGTEXT NOT NULL,
  PRIMARY KEY(post_id, meta_key),
  INDEX(meta_key)
) ENGINE=InnoDB;
```

Nhưng trong hệ WordPress thật, đổi schema có thể ảnh hưởng plugin/theme. Vì vậy đây là bài học về thiết kế index, không phải khuyến nghị áp dụng mù quáng cho mọi site.

Tư duy rút ra:

```text
Primary key nên phản ánh pattern truy cập chính.
Key-value table rất dễ cần composite index.
AUTO_INCREMENT id không tự động là primary key tốt nhất.
```

## 28. Checklist chọn index cho SELECT

Khi gặp một query, hỏi theo thứ tự:

1. Query thuộc bảng nào? Mỗi bảng cần index riêng.
2. Trong `WHERE`, cột nào là `column = constant`?
3. Có cột equality nào bị bọc function không?
4. Sau equality, có một range quan trọng không?
5. Nếu không chọn range, có `GROUP BY` phù hợp không?
6. Nếu không chọn range/group, có `ORDER BY` phù hợp không?
7. Có `ORDER BY ... LIMIT` nhỏ không?
8. Index có thể cover SELECT không?
9. Cột đầu index có low cardinality không?
10. Index này có redundant với index hiện có không?
11. Query chạy đủ thường xuyên để đáng tạo index không?
12. Index có làm chậm write path chính không?

## 29. Kết luận thực dụng

Cookbook ngắn:

```text
Equality trước.
Sau đó chọn một: range, GROUP BY, hoặc ORDER BY.
Covering index chỉ thêm nếu query đủ quan trọng.
Tránh index low-cardinality đứng một mình.
Tránh redundant index.
Luôn cân bằng SELECT nhanh với INSERT/UPDATE/DELETE chậm hơn.
```

Index tốt không phải index dài nhất. Index tốt là index giảm nhiều work nhất cho query quan trọng, với chi phí write/storage chấp nhận được.
