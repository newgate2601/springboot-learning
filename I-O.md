# I/O Management

Tài liệu này giải thích chi tiết quá trình một service gửi dữ liệu tới service khác, tập trung vào hai trường hợp:

1. Hai service nằm trên hai host khác nhau
2. Hai service là hai process nằm trên cùng host

Phạm vi chỉ theo chiều gửi và kết thúc khi dữ liệu đã nằm trong socket receive buffer của service đích. Tài liệu gốc: [Operation System.md](./Operation%20System.md).
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
-> Socket API
-> System call
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
-> Socket API
-> System call
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

Application truyền socket, địa chỉ buffer và độ dài vào `send()`/`write()`.

**Mục đích**

OS cần biết dữ liệu nằm ở đâu và có bao nhiêu byte. Application không thể chỉ nói chung chung rằng “hãy gửi object này”.

### 2.4. Socket - điểm đầu cuối do kernel quản lý

Socket là một kernel object đại diện cho một đầu giao tiếp. Application không sở hữu trực tiếp cấu trúc bên trong socket; nó giữ một file descriptor hoặc handle để tham chiếu tới socket đó.

Ví dụ:

```text
fd 7 -> TCP socket
```

Ở đây có hai thứ khác nhau:

- **Socket object:** cấu trúc dữ liệu thật nằm trong kernel
- **File descriptor - fd:** một số nguyên nhỏ mà application dùng để tham chiếu tới kernel object

Application không được nhận địa chỉ thật của socket object vì kernel memory phải được bảo vệ. Thay vào đó, kernel lưu socket trong bảng descriptor của process rồi trả về một số như `7`.

```text
Process A - user space                Kernel

Biến fd = 7                           File descriptor table của process A
                                      ┌──────┬─────────────────────────┐
                                      │ fd 7 │ -> TCP socket object    │
                                      └──────┴─────────────────────────┘
```

Một TCP connection được phân biệt bằng:

```text
source IP + source port + destination IP + destination port
```

Ví dụ:

```text
10.0.0.10:52144 -> 10.0.0.20:8080
```

**Input**

Application cung cấp:

- File descriptor/handle của socket
- Địa chỉ application buffer
- Số byte cần gửi

**Xử lý**

Socket giúp kernel xác định:

- Dữ liệu thuộc connection nào
- Protocol nào được sử dụng, ví dụ TCP
- IP và port nguồn/đích là gì
- Send buffer và trạng thái connection nằm ở đâu

**Output**

Socket cung cấp context để kernel đưa byte vào đúng socket send buffer và xử lý bằng đúng TCP connection.

**Chuyển tới**

Yêu cầu được chuyển vào kernel thông qua system call.

**Mục đích**

Một host có thể chạy hàng nghìn connection. Socket là cách OS tách chúng ra và biết dữ liệu của process nào phải đi tới IP/port nào.

### 2.5. System call - cổng đi từ application vào kernel

Application chạy ở user mode và không được tự chỉnh sửa kernel memory hoặc điều khiển NIC. Nó phải yêu cầu OS qua system call.

**Input**

Ví dụ lời gọi logic:

```text
send(fd = 7, buffer = 0x1000, length = 180)
```

`fd = 7` xuất hiện ở đây vì system call cần biết kernel object nào sẽ được thao tác. Trước đó `socket()` đã tạo descriptor, còn `connect()` đã gắn connection context vào socket object tương ứng.

**Xử lý**

CPU chuyển execution context:

```text
user mode -> kernel mode
```

Kernel:

- Lấy descriptor table của process đang gọi
- Dùng `fd = 7` để tìm đúng socket object
- Kiểm tra descriptor có hợp lệ và có trỏ tới socket không
- Kiểm tra socket có connected không
- Kiểm tra quyền và tham số
- Kiểm tra vùng nhớ bắt đầu tại `buffer = 0x1000`
- Xác định cần đọc `length = 180` byte
- Bắt đầu xử lý yêu cầu gửi

**Output**

Output là một I/O request hợp lệ bên trong kernel và các byte được chấp nhận để gửi.

**Chuyển tới**

Kernel đưa byte vào socket send buffer.

**Mục đích**

System call tạo ranh giới an toàn. Application được dùng dịch vụ của OS nhưng không được tùy ý truy cập kernel hoặc phần cứng.

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
## 4. Trường hợp 1: gửi sang service ở host khác

Ví dụ:

```text
order-service A:   10.0.0.10:52144
payment-service B: 10.0.0.20:8080
```

Service A gửi một HTTP request tới service B. Flow chỉ đi một chiều và dừng tại socket receive buffer B.

### 4.1. Sơ đồ ghép các thành phần

```text
Service A
  |
  | PaymentRequest object
  v
Serializer + HTTP library
  |
  | HTTP byte
  v
Application buffer A
  |
  | send(socket, buffer, length)
  v
System call + socket A
  |
  | byte được kernel chấp nhận
  v
Socket send buffer A
  |
  | byte stream
  v
TCP A
  |
  | TCP segment
  v
IP A
  |
  | IP packet
  v
Ethernet/Wi-Fi A
  |
  | frame trong RAM
  v
NIC driver A
  |
  | transmit descriptor
  v
Transmit ring
  |
  | địa chỉ buffer + độ dài
  v
NIC controller + DMA
  |
  | frame được lấy từ RAM
  v
NIC A
  |
  | tín hiệu vật lý
  v
Switch/router/network
  |
  | tín hiệu tới host B
  v
NIC B + DMA
  |
  | frame trong RAM B
  v
Receive ring + interrupt
  |
  | thông báo frame sẵn sàng
  v
NIC driver + Ethernet/IP/TCP B
  |
  | HTTP byte stream
  v
Socket receive buffer B
```

### 4.2. Input và output của toàn bộ flow

**Input ban đầu**

```text
PaymentRequest object trong heap của service A
```

**Output cuối cùng**

```text
HTTP byte trong socket receive buffer của service B
```

**Các dạng trung gian**

```text
Object
-> HTTP byte
-> TCP segment
-> IP packet
-> Ethernet/Wi-Fi frame
-> tín hiệu vật lý
-> Ethernet/Wi-Fi frame
-> IP packet
-> TCP segment
-> HTTP byte
```

### 4.3. Bảng theo dõi từng bước

| Bước | Thành phần | Input | Output | Chuyển cho |
|---|---|---|---|---|
| 1 | Application A | Dữ liệu nghiệp vụ | `PaymentRequest` object | Serializer |
| 2 | Serializer/HTTP library | Object, method, path | HTTP byte | Application buffer |
| 3 | Application buffer | HTTP byte | Địa chỉ buffer và độ dài | Socket API |
| 4 | Socket/system call | Socket, buffer, length | I/O request trong kernel | Socket send buffer |
| 5 | Socket send buffer | HTTP byte | Byte stream chờ gửi | TCP |
| 6 | TCP A | Byte stream | TCP segment | IP |
| 7 | IP A | TCP segment | IP packet và route | Ethernet/Wi-Fi |
| 8 | Ethernet/Wi-Fi A | IP packet | Frame | NIC driver |
| 9 | NIC driver A | Frame | Transmit descriptor | Transmit ring |
| 10 | NIC controller | Descriptor | Yêu cầu DMA | DMA engine |
| 11 | DMA phía A | Frame trong RAM | Frame trong NIC | NIC A |
| 12 | NIC A | Frame | Tín hiệu mạng | Switch/router |
| 13 | Switch/router | Frame/IP packet | Dữ liệu được chuyển tiếp | NIC B |
| 14 | NIC B | Tín hiệu | Frame | DMA phía B |
| 15 | DMA phía B | Frame trong NIC | Frame trong RAM B | Receive ring |
| 16 | Interrupt/completion | Descriptor hoàn tất | Thông báo có dữ liệu | Driver B |
| 17 | Driver/network stack B | Frame | HTTP byte stream đúng connection | Socket B |
| 18 | Socket receive buffer B | HTTP byte | Byte được giữ trong kernel B | Dừng flow |

### 4.4. Vai trò CPU, RAM và phần cứng

**CPU A**

- Chạy application và serializer
- Thực hiện system call
- Chạy TCP/IP và driver
- Cấu hình descriptor và DMA

**RAM A**

- Chứa object và HTTP byte
- Chứa socket send buffer
- Chứa packet/frame và transmit ring

**Phần cứng mạng**

- DMA chuyển frame giữa RAM và NIC
- NIC phát tín hiệu
- Switch/router chuyển tiếp dữ liệu

**CPU và RAM B**

- DMA đặt frame vào RAM B
- CPU xử lý interrupt, driver và TCP/IP
- RAM B chứa receive ring và socket receive buffer

CPU vẫn tham gia I/O, nhưng không tự copy từng byte qua NIC hoặc đẩy tín hiệu qua mạng.

### 4.5. Điểm kết thúc

Flow kết thúc khi:

```text
HTTP byte đã nằm trong socket receive buffer B
```

Chưa xét:

- Service B gọi `read()`
- Service B parse HTTP
- Service B xử lý nghiệp vụ
- Service B gửi response
## 5. Trường hợp 2: gửi sang service cùng host

Ví dụ:

```text
order-service A:   127.0.0.1:52144
payment-service B: 127.0.0.1:8080
```

A và B là hai process trên cùng máy. Mỗi process có address space riêng, vì vậy A không thể ghi trực tiếp vào heap của B.

### 5.1. Sơ đồ ghép các thành phần

```text
Service A
  |
  | PaymentRequest object
  v
Serializer + HTTP library
  |
  | HTTP byte
  v
Application buffer A
  |
  | send(socket, buffer, length)
  v
System call + socket A
  |
  | byte được kernel chấp nhận
  v
Socket send buffer A
  |
  | TCP segment/IP packet logic
  v
TCP/IP trong kernel
  |
  | route tới 127.0.0.1
  v
Loopback interface
  |
  | kernel chuyển nội bộ trong RAM
  v
TCP B tìm connection socket
  |
  | HTTP byte stream
  v
Socket receive buffer B
```

### 5.2. Input và output của toàn bộ flow

**Input ban đầu**

```text
PaymentRequest object trong heap của process A
```

**Output cuối cùng**

```text
HTTP byte trong socket receive buffer của process B
```

Không có giai đoạn biến frame thành tín hiệu vật lý. Dữ liệu không rời host.

### 5.3. Bảng theo dõi từng bước

| Bước | Thành phần | Input | Output | Chuyển cho |
|---|---|---|---|---|
| 1 | Application A | Dữ liệu nghiệp vụ | Object | Serializer |
| 2 | Serializer/HTTP library | Object | HTTP byte | Application buffer |
| 3 | Application buffer | HTTP byte | Địa chỉ buffer và độ dài | Socket API |
| 4 | Socket/system call | Socket, buffer, length | I/O request trong kernel | Socket send buffer |
| 5 | Socket send buffer A | HTTP byte | Byte stream | TCP |
| 6 | TCP/IP | Byte stream và địa chỉ `127.0.0.1` | Dữ liệu được route nội bộ | Loopback |
| 7 | Loopback interface | Dữ liệu từ TCP/IP A | Dữ liệu chuyển trong kernel/RAM | TCP B |
| 8 | TCP B | Byte stream và bộ bốn IP/port | HTTP byte đúng connection | Socket B |
| 9 | Socket receive buffer B | HTTP byte | Byte được giữ trong kernel | Dừng flow |

### 5.4. Loopback nhận gì và tạo ra gì?

**Input**

```text
Dữ liệu đã được TCP/IP xử lý
Destination = 127.0.0.1
```

**Xử lý**

Routing table nhận ra destination là chính host hiện tại. Kernel chuyển dữ liệu qua network interface ảo `loopback`.

```text
TCP/IP phía A
-> loopback
-> TCP/IP phía B
```

Tùy OS, kernel có thể copy dữ liệu hoặc tối ưu bằng cách chuyển tham chiếu giữa các buffer.

**Output**

```text
Byte stream được chuyển tới TCP connection của B
```

**Mục đích**

Loopback giữ nguyên mô hình socket/TCP/IP quen thuộc nhưng bỏ phần truyền vật lý. Hai process vẫn được OS cô lập và chỉ giao tiếp qua interface được kiểm soát.

### 5.5. Thành phần nào không tham gia?

Khi gửi qua localhost, request này không sử dụng:

- NIC vật lý
- NIC driver để truyền frame ra mạng
- NIC controller
- DMA giữa RAM và NIC
- Transmit/receive ring của NIC
- Tín hiệu điện/quang/Wi-Fi
- Switch hoặc router

Vẫn sử dụng:

- Application và serializer
- CPU và RAM
- Socket và system call
- Socket send/receive buffer
- TCP/IP
- Kernel
- Loopback interface

### 5.6. Điểm kết thúc

Flow kết thúc khi:

```text
HTTP byte đã nằm trong socket receive buffer B
```

Chưa xét B đọc, parse, xử lý hoặc gửi response.
## 6. So sánh hai flow

| Nội dung | Khác host | Cùng host qua localhost |
|---|---|---|
| Application serialize | Có | Có |
| Socket/system call | Có | Có |
| TCP/IP | Có | Có |
| Socket buffer | Có | Có |
| CPU và RAM | Có | Có |
| Loopback interface | Không | Có |
| NIC driver | Có | Không dùng để truyền request này |
| DMA giữa RAM và NIC | Có | Không |
| NIC vật lý | Có | Không |
| Switch/router | Có thể có | Không |
| Tín hiệu mạng vật lý | Có | Không |
| Điểm kết thúc | Receive buffer B | Receive buffer B |

Hai flow có phần đầu và phần cuối giống nhau:

```text
object A
-> HTTP byte
-> socket A
-> kernel
...
-> kernel
-> socket receive buffer B
```

Điểm khác biệt nằm ở đoạn giữa:

```text
Khác host:
kernel A -> driver/DMA/NIC -> network -> NIC/DMA/driver -> kernel B

Cùng host:
kernel -> loopback trong RAM -> kernel
```

## 7. Tóm tắt ngắn

**Gửi khác host**

```text
Object A
-> HTTP byte
-> socket send buffer A
-> TCP
-> IP
-> frame
-> driver
-> DMA
-> NIC A
-> network
-> NIC B
-> DMA
-> driver/kernel B
-> socket receive buffer B
```

**Gửi cùng host**

```text
Object A
-> HTTP byte
-> socket send buffer A
-> TCP/IP
-> loopback trong kernel/RAM
-> socket receive buffer B
```

Trong cả hai trường hợp, application không điều khiển phần cứng trực tiếp. Application chỉ tạo byte và gửi qua socket; kernel chịu trách nhiệm chọn con đường phù hợp.

