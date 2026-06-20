# I/O Management

Tài liệu này giải thích chi tiết quá trình một service gửi dữ liệu tới service khác, tập trung vào hai trường hợp:

1. Hai service nằm trên hai host khác nhau
2. Hai service là hai process nằm trên cùng host

Phạm vi chỉ theo chiều gửi và kết thúc khi dữ liệu đã nằm trong socket receive buffer của service đích. Tài liệu gốc: [01-operation-system-tong-quan.md](01-operation-system-tong-quan.md).
I/O là quá trình chương trình trao đổi dữ liệu với một thành phần bên ngoài vùng xử lý hiện tại của nó. Phần này chỉ tập trung vào hai trường hợp:

1. Service A gửi dữ liệu sang service B ở **host khác**
2. Service A gửi dữ liệu sang service B ở **cùng host**

Chỉ xét **một chiều gửi** từ A sang B. Flow dừng khi dữ liệu đã được đặt vào socket buffer của service B; không xét response.

## 1. Thứ tự các thành phần làm việc

Khi gửi dữ liệu sang host khác, các thành phần tham gia theo thứ tự:

```text
Application A
-> Serializer/HTTP library
-> Application buffer
-> Socket API / system call
-> Kernel tra fd để tìm socket object
-> Socket send buffer
-> TCP
-> IP
-> Ethernet/Wi-Fi
-> NIC driver
-> Transmit ring/descriptor
-> NIC controller
-> DMA
-> NIC
-> Switch/router/network
-> NIC B
-> DMA
-> Receive ring/interrupt
-> NIC driver B
-> Ethernet/IP/TCP B
-> Socket receive buffer B
```

Khi gửi cùng host qua localhost, phần cứng mạng ở giữa được thay bằng loopback:

```text
Application A
-> Serializer/HTTP library
-> Application buffer
-> Socket API / system call
-> Kernel tra fd để tìm socket object
-> Socket send buffer
-> TCP/IP
-> Loopback interface
-> Socket receive buffer B
```

Quy tắc đọc các phần dưới:

- **Input:** thành phần nhận được gì?
- **Xử lý:** thành phần làm gì với input?
- **Output:** sau khi xử lý, nó tạo ra hoặc chuyển tiếp thứ gì?
- **Chuyển tới:** output được giao cho thành phần nào tiếp theo?
- **Mục đích:** tại sao dây chuyền cần thành phần này?

## 2. Giải thích từng thành phần theo thứ tự

### 2.1. Application - nơi tạo dữ liệu nghiệp vụ

Application là code của service đang chạy trong user space, ví dụ `order-service`.

**Input**

Application nhận dữ liệu nghiệp vụ từ code, người dùng hoặc một tác vụ trước đó:

```java
PaymentRequest request =
    new PaymentRequest(123L, 500000L);
```

Input đang là một object trong heap của JVM:

```text
PaymentRequest
├── orderId = 123
└── amount = 500000
```

**Xử lý**

CPU chạy code application để:

- Tạo object
- Validate dữ liệu
- Chọn service và API cần gọi
- Gọi HTTP client

**Output**

Output là object nghiệp vụ và thông tin request:

```text
method = POST
path   = /payments
body   = PaymentRequest object
```

**Chuyển tới**

Application chuyển object và thông tin request cho serializer/HTTP library.

**Mục đích**

Application hiểu ý nghĩa nghiệp vụ như đơn hàng và thanh toán. Các lớp thấp hơn như TCP, IP và NIC không hiểu những khái niệm này.

### 2.2. Serializer và HTTP library - biến object thành message

Serializer có thể là Jackson trong Java. HTTP library có thể là Java HTTP Client, Apache HttpClient, OkHttp hoặc code trong framework.

**Input**

```text
PaymentRequest object
POST
/payments
HTTP headers
```

**Xử lý**

Serializer chuyển object thành JSON:

```json
{"orderId":123,"amount":500000}
```

HTTP library tạo HTTP message:

```http
POST /payments HTTP/1.1
Host: 10.0.0.20:8080
Content-Type: application/json
Content-Length: 35

{"orderId":123,"amount":500000}
```

Sau đó ký tự được encode thành byte, thường theo UTF-8.

**Output**

Output là một dãy byte:

```text
HTTP header bytes + JSON body bytes
```

**Chuyển tới**

Các byte được đặt trong application buffer ở RAM và sau đó được ghi vào socket.

**Mục đích**

Object Java chỉ tồn tại và có ý nghĩa trong JVM hiện tại. Process B không thể dùng trực tiếp địa chỉ object của process A. Hai bên phải thống nhất một định dạng message có thể biểu diễn bằng byte.

### 2.3. Application buffer - vùng RAM giữ byte trước khi gửi

Application buffer là một vùng nhớ thuộc process A, ví dụ `byte[]`, `ByteBuffer` hoặc buffer nội bộ của HTTP client.

**Input**

```text
HTTP byte do HTTP library tạo
```

**Xử lý**

Buffer giữ các byte liên tiếp trong RAM để application có thể truyền địa chỉ và độ dài dữ liệu cho OS.

Ví dụ:

```text
buffer address = 0x1000
length         = 180 bytes
```

**Output**

Output logic vẫn là HTTP byte, nhưng giờ chúng có một vị trí cụ thể trong RAM.

**Chuyển tới**

Application gọi `send()`/`write()` một lần, truyền file descriptor của socket, địa chỉ buffer và độ dài dữ liệu.

**Mục đích**

OS cần biết dữ liệu nằm ở đâu và có bao nhiêu byte. Application không thể chỉ nói chung chung rằng “hãy gửi object này”.

### 2.4. Socket API và system call - application yêu cầu gửi dữ liệu

Application chạy ở user mode và không được tự chỉnh sửa kernel memory hoặc điều khiển NIC. Nó phải yêu cầu OS qua system call.

Application chỉ truyền thông tin gửi **một lần** khi gọi Socket API như `send()` hoặc `write()`:

```text
Application
    |
    | send(fd, buffer, length)
    v
Socket API / system call
    |
    v
Kernel
```

**Input**

Ví dụ lời gọi logic:

```text
send(fd = 7, buffer = 0x1000, length = 180)
```

- `fd = 7`: gửi qua socket nào
- `buffer = 0x1000`: dữ liệu bắt đầu tại địa chỉ nào trong application memory
- `length = 180`: cần gửi bao nhiêu byte

Tùy hệ điều hành và thư viện, hàm API có thể là một wrapper mỏng trước system call thật. Về mặt luồng dữ liệu, đây vẫn là một yêu cầu gửi duy nhất từ application sang kernel.

**Xử lý**

CPU chuyển execution context:

```text
user mode -> kernel mode
```

Kernel:

- Nhận `fd`, `buffer` và `length`
- Kiểm tra các tham số và quyền truy cập
- Kiểm tra vùng nhớ bắt đầu tại `buffer = 0x1000`
- Xác định cần đọc `length = 180` byte
- Bắt đầu xử lý yêu cầu trên socket được tham chiếu bởi `fd = 7`

**Output**

Output là một yêu cầu gửi hợp lệ bên trong kernel.

**Chuyển tới**

Kernel dùng `fd` để tìm socket object tương ứng.

**Mục đích**

System call tạo ranh giới an toàn. Application được dùng dịch vụ của OS nhưng không được tùy ý truy cập kernel hoặc phần cứng.

### 2.5. Kernel tra fd để tìm socket object

Socket là một kernel object đại diện cho một đầu giao tiếp. Application không giữ địa chỉ thật của object này; nó chỉ giữ file descriptor hoặc handle để tham chiếu tới socket.

Ví dụ:

```text
Process A - user space                Kernel

Biến fd = 7                           File descriptor table của process A
                                      ┌──────┬─────────────────────────┐
                                      │ fd 7 │ -> TCP socket object    │
                                      └──────┴─────────────────────────┘
```

Trước đó, `socket()` đã tạo socket object và descriptor; `connect()` đã gắn connection context vào socket object đó.

**Input**

```text
fd = 7
```

`buffer` và `length` vẫn thuộc cùng yêu cầu `send()` ở mục 2.4. Chúng không được application truyền thêm một lần nữa và không được lưu lâu dài trong socket object.

**Xử lý**

Kernel:

- Lấy descriptor table của process đang gọi
- Dùng `fd = 7` để tìm đúng socket object
- Kiểm tra descriptor có hợp lệ và có trỏ tới socket không
- Kiểm tra trạng thái connection
- Lấy protocol, IP/port nguồn và đích
- Xác định socket send buffer tương ứng

Một TCP connection được phân biệt bằng:

```text
source IP + source port + destination IP + destination port
```

Ví dụ:

```text
10.0.0.10:52144 -> 10.0.0.20:8080
```

**Output**

Kernel đã xác định được đúng socket object và send buffer cần nhận dữ liệu.

**Chuyển tới**

Kernel copy các byte từ application buffer vào socket send buffer.

**Mục đích**

Một process có thể mở nhiều socket. `fd` giúp kernel biết yêu cầu `send()` hiện tại dành cho connection nào.

### 2.6. Socket send buffer - hàng chờ byte phía gửi

Socket send buffer là vùng RAM do kernel quản lý cho một socket.

**Input**

```text
HTTP byte từ application buffer
```

**Xử lý**

Kernel thường copy byte từ user space vào send buffer:

```text
Application buffer
--CPU/kernel copy-->
Socket send buffer
```

Send buffer giữ dữ liệu trong lúc TCP chưa thể gửi hết ngay. TCP có thể phải:

- Chia dữ liệu thành nhiều segment
- Chờ network cho phép gửi
- Giữ dữ liệu chưa được ACK
- Gửi lại dữ liệu bị mất

**Output**

Output là byte stream đang chờ TCP xử lý.

**Chuyển tới**

TCP lấy byte từ send buffer.

**Mục đích**

Application và network chạy ở tốc độ khác nhau. Buffer cho phép application giao dữ liệu cho kernel mà không cần tự giữ CPU chờ NIC gửi từng byte.

`send()` trả về không đồng nghĩa dữ liệu đã tới B. Nó có thể chỉ có nghĩa byte đã được kernel A nhận vào send buffer.

### 2.7. TCP - vận chuyển byte giữa hai connection endpoint

TCP là protocol transport chạy chủ yếu trong kernel network stack.

**Input**

```text
Byte stream từ socket send buffer
Connection:
10.0.0.10:52144 -> 10.0.0.20:8080
```

**Xử lý**

TCP:

1. Chia byte stream thành các TCP segment
2. Thêm source port và destination port
3. Gắn sequence number
4. Tính checksum
5. Theo dõi ACK
6. Gửi lại segment khi cần
7. Điều khiển luồng và tốc độ gửi

Ví dụ logic:

```text
TCP header
├── source port      = 52144
├── destination port = 8080
├── sequence number
└── checksum

TCP payload
└── một phần HTTP byte
```

**Output**

```text
TCP segment = TCP header + payload
```

**Chuyển tới**

TCP segment được chuyển cho IP layer.

**Mục đích**

TCP giải quyết việc đưa byte tới đúng port/connection, đúng thứ tự và đáng tin cậy hơn. TCP không hiểu HTTP, JSON hoặc nghiệp vụ.

### 2.8. IP - đưa packet tới đúng host

IP là network layer trong kernel.

**Input**

```text
TCP segment
source IP      = 10.0.0.10
destination IP = 10.0.0.20
```

**Xử lý**

IP:

1. Thêm IP header
2. Gắn source IP và destination IP
3. Gắn protocol field để chỉ payload là TCP
4. Đọc routing table
5. Chọn network interface và next hop

**Output**

```text
IP packet = IP header + TCP segment
```

**Chuyển tới**

IP packet được chuyển cho lớp Ethernet hoặc Wi-Fi phù hợp với network interface đã chọn.

**Mục đích**

TCP port xác định application, còn IP address xác định host. Router dựa chủ yếu vào destination IP để chuyển packet qua các mạng.

### 2.9. Ethernet/Wi-Fi - đóng gói cho một chặng vật lý

IP đưa packet tới host đích về mặt logic. Ethernet hoặc Wi-Fi chịu trách nhiệm truyền packet qua chặng mạng hiện tại.

**Input**

```text
IP packet
Network interface đã chọn
Địa chỉ next hop
```

**Xử lý**

Với Ethernet, kernel tạo frame và thêm:

- Source MAC
- Destination MAC của next hop
- Loại payload
- Thông tin kiểm tra lỗi frame

Cấu trúc đóng gói:

```text
Ethernet frame
└── IP packet
    └── TCP segment
        └── HTTP byte
```

**Output**

```text
Ethernet frame hoặc Wi-Fi frame trong RAM
```

**Chuyển tới**

Frame được đưa vào hàng chờ của NIC driver.

**Mục đích**

NIC và switch không truyền object, JSON hay TCP segment trần. Chúng cần frame phù hợp với công nghệ liên kết của chặng hiện tại.

### 2.10. NIC driver - phần mềm điều khiển đúng card mạng

NIC driver là phần mềm trong kernel, được viết cho một loại hoặc họ NIC cụ thể.

**Input**

```text
Frame trong RAM
Yêu cầu gửi từ network stack
```

**Xử lý**

Driver:

1. Chọn vị trí trống trong transmit ring
2. Tạo descriptor mô tả frame
3. Ghi địa chỉ buffer và độ dài vào descriptor
4. Cấu hình các tùy chọn phần cứng
5. Thông báo cho NIC controller có dữ liệu mới

Driver không nhất thiết copy toàn bộ frame vào NIC. Nó có thể chỉ cung cấp địa chỉ vùng RAM để NIC dùng DMA.

**Output**

```text
Transmit descriptor
├── địa chỉ frame trong RAM
├── độ dài frame
└── flags điều khiển
```

**Chuyển tới**

Descriptor được đặt trong transmit ring mà NIC controller có thể đọc.

**Mục đích**

Network stack dùng interface chung, còn mỗi model NIC có thanh ghi, queue và tính năng riêng. Driver là lớp chuyển yêu cầu chung của OS thành thao tác phù hợp với phần cứng cụ thể.

### 2.11. Transmit ring và descriptor - hàng giao việc cho NIC

Transmit ring là một hàng đợi dạng vòng nằm trong RAM và được chia sẻ logic giữa driver với NIC controller.

**Input**

```text
Descriptor do driver tạo
```

Descriptor không phải toàn bộ HTTP request. Nó chủ yếu là metadata chỉ tới dữ liệu:

```text
buffer address = 0x9000
length         = 240 bytes
status         = ready
```

**Xử lý**

- Driver thêm descriptor vào ring
- NIC controller đọc các descriptor có trạng thái `ready`
- Hai bên cập nhật vị trí đầu/cuối của ring

**Output**

Output là một công việc gửi mà NIC controller có thể thực hiện: lấy frame tại địa chỉ RAM đã cho và gửi nó.

**Chuyển tới**

NIC controller nhận descriptor và bắt đầu DMA.

**Mục đích**

Ring cho phép CPU chuẩn bị nhiều frame theo lô. NIC xử lý lần lượt mà CPU không phải dừng lại điều khiển từng byte hay từng frame theo cách đồng bộ.

### 2.12. NIC controller - bộ điều khiển trên card mạng

NIC controller là mạch xử lý trên NIC. Firmware trong NIC có thể hỗ trợ controller vận hành.

**Input**

```text
Transmit descriptor trong ring
```

**Xử lý**

Controller:

1. Đọc địa chỉ và độ dài frame
2. Yêu cầu DMA đọc vùng RAM đó
3. Quản lý queue gửi
4. Điều khiển phần cứng phát frame
5. Cập nhật trạng thái descriptor khi hoàn tất

**Output**

```text
Yêu cầu DMA
Trạng thái gửi
Frame sẵn sàng để phát
```

**Chuyển tới**

Controller phối hợp với DMA engine và phần truyền tín hiệu của NIC.

**Mục đích**

CPU không cần biết cách vận hành chi tiết mạch phát/nhận của NIC. Controller thực hiện các thao tác phần cứng lặp lại và chuyên biệt.

### 2.13. DMA - chuyển frame giữa RAM và NIC

DMA là cơ chế cho phép thiết bị truy cập vùng RAM đã được OS cho phép mà CPU không phải copy từng byte.

**Input phía gửi**

```text
Địa chỉ frame trong RAM
Độ dài frame
Hướng truyền: RAM -> NIC
```

**Xử lý**

DMA engine đọc một khối dữ liệu từ RAM và chuyển nó tới NIC:

```text
RAM A --DMA--> NIC A
```

**Output phía gửi**

Frame đã có trong vùng xử lý/buffer của NIC và sẵn sàng phát.

**Input phía host B**

```text
Frame NIC B vừa nhận
Địa chỉ receive buffer trong RAM B
Hướng truyền: NIC -> RAM
```

**Output phía host B**

```text
NIC B --DMA--> receive buffer trong RAM B
```

**Chuyển tới**

Sau DMA, NIC controller cập nhật descriptor và có thể tạo completion/interrupt.

**Mục đích**

DMA giảm lượng công việc copy dữ liệu của CPU. CPU vẫn cấu hình request, quản lý buffer và xử lý hoàn tất; DMA chỉ đảm nhận phần chuyển khối dữ liệu chính.

### 2.14. NIC - biến frame thành tín hiệu mạng

NIC là thiết bị phần cứng kết nối host với môi trường mạng.

**Input phía gửi**

```text
Ethernet/Wi-Fi frame từ DMA/controller
```

**Xử lý**

NIC:

- Đọc bit của frame
- Mã hóa chúng theo chuẩn mạng
- Biến bit thành tín hiệu điện, ánh sáng hoặc sóng vô tuyến
- Phát tín hiệu ra môi trường truyền

**Output**

```text
Tín hiệu điện trên cáp đồng
hoặc ánh sáng trên cáp quang
hoặc sóng vô tuyến Wi-Fi
```

**Chuyển tới**

Tín hiệu đi tới switch, access point hoặc thiết bị mạng kế tiếp.

**Mục đích**

RAM và CPU lưu/xử lý bit, còn dây mạng và không khí vận chuyển tín hiệu vật lý. NIC là cầu nối giữa hai dạng này.

### 2.15. Switch, router và network - chuyển dữ liệu giữa hai host

**Input**

Switch nhận frame. Router nhận một frame có chứa IP packet.

**Xử lý**

Switch:

- Đọc destination MAC
- Chọn cổng phù hợp trong mạng LAN
- Chuyển frame sang thiết bị hoặc router tiếp theo

Router:

- Bỏ frame header của chặng cũ
- Đọc destination IP
- Tra routing table
- Chọn next hop
- Đóng IP packet vào frame mới của chặng tiếp theo

**Output**

Output là frame/packet đã được chuyển gần host B hơn.

**Chuyển tới**

Quá trình lặp lại cho tới NIC của host B.

**Mục đích**

Hai host thường không nối dây trực tiếp với nhau. Switch và router tạo đường đi qua mạng. MAC phục vụ từng chặng, còn destination IP xác định host đích cuối cùng.

### 2.16. NIC B, receive ring và interrupt - đưa frame vào host đích

**Input**

```text
Tín hiệu mạng tới NIC B
```

**Xử lý**

NIC B:

1. Khôi phục tín hiệu thành frame
2. Kiểm tra frame cơ bản
3. Chọn receive descriptor do driver B chuẩn bị
4. Dùng DMA ghi frame vào receive buffer trong RAM B
5. Cập nhật trạng thái descriptor thành đã có dữ liệu
6. Phát interrupt hoặc completion notification

Interrupt mang ý nghĩa:

```text
"Đã có frame trong RAM, kernel hãy xử lý."
```

Interrupt không mang toàn bộ HTTP request. Dữ liệu thật đã nằm trong RAM nhờ DMA.

**Output**

```text
Frame trong RAM B
Receive descriptor đã hoàn thành
Tín hiệu interrupt/completion
```

**Chuyển tới**

NIC driver B và kernel network stack.

**Mục đích**

NIC cần một cách đặt dữ liệu vào bộ nhớ và báo cho OS biết dữ liệu đã sẵn sàng. DMA thực hiện việc chuyển; interrupt/completion thực hiện việc thông báo.

### 2.17. Driver và network stack B - tìm đúng socket đích

**Input**

```text
Frame trong receive buffer B
Descriptor báo frame đã sẵn sàng
```

**Xử lý**

Các lớp được tháo theo thứ tự:

```text
Ethernet frame
-> IP packet
-> TCP segment
-> HTTP byte
```

Ethernet layer kiểm tra frame và chuyển IP packet lên trên.

IP layer kiểm tra destination IP:

```text
10.0.0.20
```

TCP layer đọc source/destination IP và port:

```text
10.0.0.10:52144 -> 10.0.0.20:8080
```

TCP dùng bộ bốn này để tìm đúng connection socket. Nó cũng kiểm tra checksum, sequence number và sắp xếp byte đúng thứ tự.

**Output**

```text
HTTP byte stream thuộc đúng connection
```

**Chuyển tới**

TCP đặt payload vào socket receive buffer B.

**Mục đích**

Host B có thể nhận packet cho nhiều process và connection. Network stack phải tháo các lớp header và xác định byte thuộc socket nào.

### 2.18. Socket receive buffer B - điểm kết thúc của flow gửi

Socket receive buffer là vùng RAM trong kernel dành cho connection socket của service B.

**Input**

```text
HTTP byte do TCP B đã kiểm tra và sắp xếp
```

**Xử lý**

Kernel:

- Đưa byte vào đúng receive buffer
- Cập nhật trạng thái socket là có dữ liệu
- Giữ byte cho tới khi application B đọc

**Output**

```text
HTTP byte đã nằm trong socket receive buffer B
```

**Chuyển tới**

Theo phạm vi của phần này, flow dừng tại đây. Không mô tả application B đọc, parse, xử lý hoặc gửi response.

**Mục đích**

Network có thể đưa dữ liệu tới nhanh hơn thời điểm application B được CPU chạy. Receive buffer giữ dữ liệu an toàn trong kernel cho tới khi B sẵn sàng đọc.

### 2.19. CPU và RAM tham gia xuyên suốt thế nào?

CPU không phải một bước đứng riêng trong dây chuyền. CPU chạy code ở nhiều bước:

- Application và serializer
- System call
- TCP/IP stack
- NIC driver
- Cấu hình descriptor và DMA
- Xử lý interrupt/completion
- Network stack của host B

RAM cũng xuất hiện xuyên suốt:

```text
Object
-> application buffer
-> socket send buffer
-> TCP/IP/frame buffer
-> transmit ring
-> receive ring
-> socket receive buffer
```

Trong thời gian frame đang được NIC, switch và router truyền:

- CPU không phải tự đẩy từng byte qua dây
- DMA chuyển dữ liệu giữa RAM và NIC
- Thiết bị mạng tự chuyển tiếp tín hiệu/frame
- CPU có thể chạy công việc khác

Vì vậy, I/O vẫn dùng CPU và RAM. Điểm tối ưu là CPU không phải thực hiện toàn bộ việc truyền vật lý hoặc chờ liên tục.

## 3. File descriptor - mã tham chiếu resource của process

File descriptor, viết tắt là d, không chỉ dùng cho socket. Trên Unix/Linux, process có thể dùng descriptor để tham chiếu tới file, socket, pipe và một số resource I/O khác.

Phần này tách riêng d khỏi Socket để làm rõ:

- d được kernel tạo và lưu thế nào
- Vì sao cùng một d xuất hiện trong nhiều system call
- connect(), send() và close() dùng d ra sao
- Phạm vi của d trong từng process

### 3.1. `fd` được tạo ra khi nào?

Application yêu cầu kernel tạo socket:

```text
fd = socket(AF_INET, SOCK_STREAM, TCP)
```

Flow:

```text
Application gọi socket()
-> system call
-> kernel tạo TCP socket object
-> kernel tìm một vị trí trống trong descriptor table
-> kernel gắn vị trí 7 với socket object
-> kernel trả số 7 về application
```

Kết quả:

```text
Application giữ: fd = 7
Kernel giữ:      socket object thật
```

Lúc mới tạo, socket có thể chưa biết địa chỉ đích:

```text
fd 7
-> protocol = TCP
-> state = CREATED
-> local IP/port = chưa hoàn chỉnh
-> remote IP/port = chưa có
```

Số `7` không có ý nghĩa đặc biệt. Kernel thường chọn descriptor nhỏ nhất đang trống. Ví dụ process có thể đã dùng:

```text
fd 0 -> standard input
fd 1 -> standard output
fd 2 -> standard error
fd 3 -> file cấu hình
fd 4 -> log file
fd 5 -> socket khác
fd 6 -> file khác
fd 7 -> socket vừa tạo
```

### 3.2. Tại sao `fd = 7` xuất hiện trong nhiều bước?

`fd = 7` được tạo một lần, sau đó được dùng lại để chỉ cùng socket object trong toàn bộ vòng đời của connection:

```text
fd = socket(...)
connect(fd, destination)
send(fd, buffer, length)
send(fd, anotherBuffer, anotherLength)
close(fd)
```

Đây không phải nhiều `fd` khác nhau. Nó là cùng một mã định danh được truyền vào nhiều system call để kernel biết system call đang thao tác với socket nào.

Có thể hình dung `fd` giống số thứ tự trên phiếu gửi đồ:

```text
Application giữ số 7
Kernel dùng số 7 để tìm đúng socket object
```

Application không cần và không được tự truy cập cấu trúc socket bên trong kernel.

### 3.3. `connect(fd = 7, ...)` dùng `fd` để làm gì?

Client gọi:

```text
connect(fd = 7, destination = 10.0.0.20:8080)
```

Kernel xử lý:

1. Dùng process hiện tại để tìm descriptor table của process đó
2. Tra entry số `7`
3. Kiểm tra object được trỏ tới có phải socket không
4. Lấy TCP socket object tương ứng
5. Gán remote IP và remote port
6. Chọn local IP và source port tạm thời
7. Thực hiện TCP handshake
8. Cập nhật trạng thái socket

Trước `connect()`:

```text
fd 7
-> TCP socket
-> state = CREATED
```

Sau `connect()`:

```text
fd 7
-> TCP socket
-> local  = 10.0.0.10:52144
-> remote = 10.0.0.20:8080
-> state  = CONNECTED
-> send buffer
-> receive buffer
-> TCP sequence/ACK state
```

Thông tin connection được lưu trong socket object của kernel, không được lưu trực tiếp trong số `7`. Số `7` chỉ là chìa khóa để tìm object đó.

### 3.4. `send(fd = 7, buffer, length)` dùng từng tham số thế nào?

Ví dụ:

```text
send(fd = 7, buffer = 0x1000, length = 180)
```

Ba tham số trả lời ba câu hỏi khác nhau:

| Tham số | Trả lời câu hỏi | Ý nghĩa |
|---|---|---|
| `fd = 7` | Gửi qua connection nào? | Tìm socket object và socket send buffer |
| `buffer = 0x1000` | Dữ liệu nằm ở đâu? | Địa chỉ vùng nhớ trong user space của process |
| `length = 180` | Gửi bao nhiêu? | Số byte kernel cần đọc từ buffer |

Kernel xử lý logic như sau:

```text
Process hiện tại
-> descriptor table
-> entry fd 7
-> TCP socket object
-> socket send buffer

User memory
-> bắt đầu tại 0x1000
-> đọc 180 byte
-> đưa byte vào send buffer của socket trên
```

Flow chi tiết:

1. CPU chuyển từ user mode sang kernel mode
2. Kernel tìm descriptor table của process gọi `send()`
3. Kernel tra `fd = 7`
4. Kernel lấy đúng TCP socket object
5. Kernel kiểm tra socket đang ở trạng thái cho phép gửi
6. Kernel kiểm tra vùng nhớ từ `0x1000` đến `0x1000 + 180`
7. Kernel lấy 180 byte từ application buffer
8. Kernel đưa byte vào send buffer thuộc socket `fd = 7`
9. TCP tiếp tục xử lý byte của connection đó

Nếu application có hai socket:

```text
fd 7 -> payment-service:8080
fd 8 -> inventory-service:8081
```

thì:

```text
send(7, buffer, 180)
```

sẽ đưa dữ liệu vào socket đi tới `payment-service`, còn:

```text
send(8, buffer, 180)
```

sẽ đưa dữ liệu vào socket đi tới `inventory-service`.

Cùng một buffer có thể được gửi qua hai socket khác nhau. `buffer` nói dữ liệu là gì; `fd` nói dữ liệu phải đi qua connection nào.

### 3.5. Tại sao `send()` không cần nhận lại IP và port?

IP và port đích đã được `connect()` lưu trong socket object:

```text
fd 7
-> TCP socket object
-> remote = 10.0.0.20:8080
```

Khi application gọi:

```text
send(7, buffer, 180)
```

kernel tra `fd 7` và lấy được toàn bộ connection context:

- Source IP và source port
- Destination IP và destination port
- TCP state
- Sequence number
- Send/receive buffer
- Timeout và các socket options

Vì vậy application không phải truyền lại IP/port trong mỗi lần `send()`.

Với một số kiểu socket không thiết lập connection trước, ví dụ UDP dùng `sendto()`, application có thể truyền destination address ngay trong lời gọi gửi:

```text
sendto(fd, buffer, length, destination)
```

### 3.6. `fd` chỉ có ý nghĩa trong một process

Mỗi process có descriptor table riêng:

```text
Process A                         Process B

fd 7 -> payment socket           fd 7 -> log file
fd 8 -> config file              fd 8 -> client socket
```

Vì vậy:

- `fd = 7` của process A không mặc nhiên là `fd = 7` của process B
- Kernel luôn kết hợp **process hiện tại + số fd** để tìm object
- Chỉ nhìn số `7` mà không biết process nào thì chưa đủ xác định resource

Một số trường hợp đặc biệt như `fork()` hoặc truyền descriptor giữa process có thể làm hai process cùng tham chiếu tới một underlying kernel object. Tuy nhiên chúng vẫn có descriptor table riêng.

### 3.7. `close(fd = 7)` làm gì?

Khi không dùng socket nữa, application gọi:

```text
close(fd = 7)
```

Kernel:

1. Tra descriptor table của process
2. Tìm entry `7`
3. Xóa entry khỏi table
4. Giảm số lượng reference tới socket object
5. Thực hiện đóng TCP connection khi phù hợp
6. Giải phóng socket object khi không còn reference

Sau `close()`:

```text
fd 7 -> không còn hợp lệ
```

Nếu application tiếp tục gọi:

```text
send(7, buffer, 180)
```

kernel sẽ báo lỗi vì descriptor không còn trỏ tới socket hợp lệ.

Sau này kernel có thể tái sử dụng số `7` cho một file hoặc socket mới. Vì vậy số fd không phải identity vĩnh viễn của resource.

### 3.8. Vòng đời đầy đủ của `fd`

```text
1. socket()
   Input:  loại socket/protocol
   Output: fd = 7

2. connect(7, 10.0.0.20:8080)
   Input:  fd và địa chỉ đích
   Output: socket 7 ở trạng thái CONNECTED

3. send(7, 0x1000, 180)
   Input:  fd, địa chỉ dữ liệu, số byte
   Output: 180 byte vào send buffer của socket 7

4. send(7, 0x2000, 80)
   Input:  cùng fd, dữ liệu khác
   Output: thêm 80 byte vào cùng connection

5. close(7)
   Input:  fd
   Output: descriptor bị đóng
```

Điểm quan trọng:

```text
fd không chứa dữ liệu request
fd không phải địa chỉ socket trong RAM
fd không phải port
fd không phải connection object

fd là số dùng để kernel tìm đúng object trong descriptor table của process
```
## 4. So sánh gửi khác host và cùng host

Phần đầu của hai trường hợp giống nhau:

```text
Object A
-> HTTP byte
-> Application buffer
-> send(fd, buffer, length)
-> Kernel tra fd
-> Socket send buffer A
-> TCP/IP
```

Sau khi xử lý routing, đường đi bắt đầu khác nhau.

### 4.1. Đường đi chi tiết

**Khác host**

```text
TCP/IP kernel A
-> chọn route qua network interface vật lý
-> tạo packet và frame
-> qdisc/hàng đợi gửi
-> NIC driver A
-> transmit ring
-> NIC A dùng DMA đọc frame từ RAM
-> NIC A phát tín hiệu điện/quang/vô tuyến
-> switch/router chuyển tiếp
-> NIC B nhận tín hiệu
-> NIC B dùng DMA ghi frame vào RAM B
-> receive ring
-> interrupt/polling
-> NIC driver B
-> Ethernet/IP/TCP kernel B
-> socket receive buffer B
```

**Cùng host qua loopback**

```text
TCP/IP kernel
-> routing nhận ra địa chỉ đích thuộc chính host
-> chọn loopback interface
-> kernel chuyển packet nội bộ
-> TCP phía nhận tìm socket của process B
-> Socket receive buffer B
```

Loopback không phải một vùng RAM riêng. Nó là network interface ảo và đường xử lý nội bộ trong kernel. Packet không được đưa ra NIC; dữ liệu và metadata của packet chỉ được xử lý trong CPU/RAM của cùng host.

Tùy hệ điều hành và cơ chế tối ưu, kernel có thể copy dữ liệu giữa các buffer hoặc chuyển/reuse các cấu trúc buffer nội bộ. Vì vậy không nên hiểu loopback là một thao tác đơn giản kiểu “copy thẳng từ heap A sang heap B”.

### 4.2. So sánh chi tiết từng giai đoạn

| Giai đoạn | Khác host | Cùng host qua loopback |
|---|---|---|
| Routing | Chọn network interface dẫn tới host B và next hop | Nhận ra destination thuộc local host và chọn loopback |
| Network namespace/kernel | Đi từ network stack của host A sang network stack của host B | Thường ở trong cùng kernel/network namespace, trừ cấu hình container hoặc namespace riêng |
| Link layer | Tạo Ethernet/Wi-Fi frame | Không cần frame vật lý để phát ra mạng |
| Neighbor resolution | Có thể cần ARP cho IPv4 hoặc NDP cho IPv6 để tìm địa chỉ MAC của next hop | Không cần tìm MAC của máy đích |
| Hàng đợi gửi | Có qdisc, transmit queue và transmit ring | Có hàng đợi/buffer phần mềm nội bộ nhưng không có transmit ring của NIC vật lý |
| NIC driver | Driver A chuẩn bị descriptor; driver B xử lý dữ liệu nhận | Không dùng driver NIC vật lý cho request này |
| DMA | NIC A đọc dữ liệu từ RAM A; NIC B ghi dữ liệu vào RAM B | Không có DMA giữa RAM và NIC |
| NIC | Serialize frame thành tín hiệu và nhận lại tín hiệu ở host B | Không sử dụng NIC vật lý |
| Môi trường truyền | Cáp đồng, cáp quang hoặc sóng vô tuyến | CPU cache và RAM của cùng máy |
| Thiết bị trung gian | Có thể qua switch, router, firewall, load balancer | Không qua thiết bị mạng vật lý; vẫn có thể qua firewall/filter trong kernel |
| Khoảng cách vật lý | Tín hiệu phải di chuyển giữa các host | Không có propagation giữa hai máy |
| Xử lý phía nhận | NIC B, DMA, receive ring, interrupt/NAPI, driver B rồi network stack B | Kernel chuyển trực tiếp sang đường nhận của TCP trên cùng host |
| Tải CPU | Chia cho CPU của hai host; có thêm xử lý driver và interrupt | Cả hai process và network stack cạnh tranh CPU trên cùng host |
| Memory bandwidth | Dùng RAM của hai host và bus PCIe/NIC | Dùng memory bandwidth/cache của một host |
| Packet loss | Có thể mất do đường truyền, congestion hoặc thiết bị mạng | Không có lỗi đường truyền vật lý; vẫn có thể drop do giới hạn buffer/tài nguyên |
| TCP retransmission | Có thể xảy ra do mất gói hoặc reordering trên mạng | Hiếm hơn nhiều; không có mất gói vật lý nhưng vẫn chịu logic TCP |
| Jitter | Bị ảnh hưởng bởi queueing và tải của nhiều thiết bị/đường truyền | Chủ yếu do CPU scheduling, contention, garbage collection và tải kernel |
| Giới hạn throughput | NIC speed, PCIe, link, switch/router và network congestion | CPU, memory bandwidth, copy cost và socket buffer |
| Dữ liệu rời host | Có | Không |

### 4.3. Những phần trực tiếp tạo chênh lệch latency

Có thể biểu diễn gần đúng:

```text
Latency khác host
= phần chung của application/socket/TCP
+ xếp hàng tại qdisc và NIC A
+ xử lý driver, descriptor và DMA phía gửi
+ thời gian NIC phát frame
+ propagation trên môi trường truyền
+ xử lý và xếp hàng tại switch/router
+ thời gian NIC B nhận frame
+ DMA, interrupt/polling và driver phía nhận
+ nguy cơ chờ TCP retransmission

Latency loopback
= phần chung của application/socket/TCP
+ xử lý route loopback trong kernel
+ memory copy hoặc quản lý buffer nội bộ
+ CPU scheduling giữa process A và process B
```

Chênh lệch lớn nhất thường đến từ:

1. **Khoảng cách vật lý:** tín hiệu cần thời gian đi từ host A tới host B.
2. **Queueing:** packet có thể phải chờ ở NIC, switch, router hoặc đường truyền đang nghẽn.
3. **Xử lý phần cứng và driver:** DMA, descriptor, interrupt/polling và hai NIC đều thêm công việc.
4. **Thiết bị trung gian:** mỗi switch, router, firewall hoặc load balancer cần nhận, kiểm tra và chuyển tiếp dữ liệu.
5. **Mất gói và retransmission:** nếu packet mất, TCP phải phát lại; đây có thể là nguồn tăng latency rất lớn.

Loopback bỏ được toàn bộ đoạn phần cứng và network ở giữa, nhưng không có nghĩa latency bằng không. Nó vẫn chịu:

- System call và TCP/IP processing
- Copy/quản lý buffer trong kernel
- Context switch và process scheduling
- CPU contention
- Garbage collection hoặc pause của application
- Giới hạn socket buffer và memory bandwidth

### 4.4. Điểm dễ hiểu nhầm

Hai process trên cùng host vẫn có address space riêng:

```text
Heap process A != Heap process B
```

Service A không ghi thẳng application buffer vào heap của service B. Với TCP loopback, dữ liệu vẫn đi qua API socket, kernel network stack và socket receive buffer của B.

Trong cả hai trường hợp, application không điều khiển phần cứng trực tiếp. Application chỉ tạo byte và gửi qua socket; kernel chọn đường đi phù hợp.

