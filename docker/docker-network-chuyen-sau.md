# Docker Network chuyên sâu

Tài liệu này giải thích Docker Network từ góc nhìn sử dụng hằng ngày đến cơ chế Linux bên dưới. Mục tiêu là trả lời rõ các câu hỏi:

- Container giao tiếp với nhau bằng cách nào?
- Vì sao `localhost` thường gây nhầm lẫn?
- `ports`, `expose`, `EXPOSE` và container port khác nhau ra sao?
- Docker DNS hoạt động như thế nào?
- Khi nào dùng `bridge`, `host`, `none`, `overlay`, `macvlan` hoặc `ipvlan`?
- Docker Compose tạo network và đặt tên DNS ra sao?
- Một packet đi từ trình duyệt đến ứng dụng trong container theo đường nào?
- Làm thế nào để thiết kế network an toàn và debug khi mất kết nối?

> Ghi chú: phần cơ chế kernel như network namespace, `veth`, bridge và firewall mô tả Docker Engine chạy trên Linux. Trên Docker Desktop cho Windows/macOS, Linux container thực tế chạy trong một Linux VM, vì vậy đường đi của packet có thêm lớp VM và hành vi ở biên host có thể khác.

---

## 1. Mô hình tư duy quan trọng nhất

Mỗi container thường có một **network namespace** riêng.

### 1.1 Network namespace là gì?

**Network namespace** là một tính năng của Linux kernel dùng để tạo ra nhiều network stack độc lập trên cùng một Linux kernel.

Bình thường, một máy Linux có một network stack chung:

```text
Linux host
  |- network interfaces: lo, eth0, wlan0...
  |- IP addresses
  |- routing table
  |- ARP/neighbor table
  |- listening sockets
  `- firewall rules
```

Mọi process chạy trong hệ thống mặc định đều nhìn thấy và sử dụng network stack này.

Khi tạo một network namespace mới, Linux tạo thêm một "góc nhìn mạng" độc lập:

```text
Linux kernel
  |
  |-- Network namespace của host
  |     |- eth0: 192.168.1.10
  |     |- lo: 127.0.0.1
  |     `- routing table của host
  |
  |-- Network namespace A
  |     |- eth0: 172.20.0.2
  |     |- lo: 127.0.0.1
  |     `- routing table A
  |
  `-- Network namespace B
        |- eth0: 172.20.0.3
        |- lo: 127.0.0.1
        `- routing table B
```

Namespace A không tự động nhìn thấy interface, IP, route hay socket thuộc namespace B. Cả hai dùng chung một Linux kernel, nhưng kernel trả về một góc nhìn khác tùy process đang thuộc network namespace nào.

Vì vậy, network namespace không phải là:

- Một máy ảo hoàn chỉnh.
- Một card mạng vật lý mới.
- Một firewall rule đơn lẻ.
- Một Docker network.

Nó là một cơ chế cô lập network stack ở cấp kernel.

Docker sử dụng cơ chế này để tạo cảm giác rằng mỗi container có "mạng riêng", dù các container vẫn chạy trên cùng kernel của host.

### 1.2 Tại sao process mới là đối tượng quan trọng?

Một network namespace không trực tiếp "chứa container". Chính xác hơn, các **process** được gắn với một network namespace.

Khi process thực hiện thao tác mạng, Linux kernel kiểm tra process đó thuộc namespace nào:

```text
Process gọi bind(), connect(), listen(), sendto()...
                       |
                       v
Kernel xác định network namespace của process
                       |
                       v
Dùng interface, route, socket và firewall trong namespace đó
```

Ví dụ:

- Process Java của container `api` thuộc namespace A.
- Process PostgreSQL của container `db` thuộc namespace B.
- Docker daemon thuộc namespace của host.

Khi Java gọi:

```text
connect("db", 5432)
```

DNS lookup, route lookup và kết nối TCP được thực hiện từ góc nhìn của namespace A.

Khi PostgreSQL gọi:

```text
bind("0.0.0.0", 5432)
```

Socket được tạo trong namespace B.

Hai socket có cùng số port nhưng nằm trong hai namespace khác nhau không nhất thiết xung đột.

### 1.3 Những tài nguyên nào được cô lập?

Có thể hình dung mỗi network namespace là một "phòng mạng" riêng, có:

- Network interface riêng.
- Địa chỉ IP riêng.
- Routing table riêng.
- ARP/neighbor table riêng.
- Port lắng nghe riêng.
- Firewall rule riêng nếu được cấu hình.
- `localhost` và loopback interface `lo` riêng.

Chi tiết hơn:

| Tài nguyên | Ý nghĩa |
| --- | --- |
| Network interface | Namespace A có thể thấy `eth0`, nhưng không thấy `eth0` của namespace B |
| IPv4/IPv6 address | Mỗi interface trong từng namespace có địa chỉ riêng |
| Routing table | Kernel chọn đường đi dựa trên route của namespace gửi packet |
| Default gateway | Mỗi namespace có thể có gateway khác nhau |
| ARP/neighbor table | Mỗi namespace tự lưu ánh xạ IP sang MAC/neighbor |
| Socket và port | Mỗi namespace có không gian socket/listening port riêng |
| Loopback interface | Mỗi namespace có `lo` và `127.0.0.1` riêng |
| Firewall | Có thể áp dụng rule theo namespace và đường packet đi qua host |
| Connection tracking | Trạng thái kết nối liên quan đến namespace và các lớp firewall/NAT |
| Network-related sysctl | Nhiều cấu hình như forwarding có thể có giá trị riêng theo namespace |

Network namespace không cô lập mọi thứ của container. Docker còn sử dụng các namespace khác:

| Namespace | Cô lập |
| --- | --- |
| Network namespace | Interface, IP, route, port và network stack |
| PID namespace | Process ID và cây process |
| Mount namespace | Mount point và góc nhìn filesystem |
| UTS namespace | Hostname và domain name |
| IPC namespace | Shared memory, semaphore và message queue |
| User namespace | User/group ID |
| Cgroup namespace | Góc nhìn về cgroup |

Một container là kết quả phối hợp của nhiều cơ chế, không chỉ network namespace.

### 1.4 `eth0` là gì?

`eth0` thường là tên của **network interface chính** bên trong container.

Network interface có thể hiểu là một cổng mà network stack dùng để gửi và nhận packet. Interface có các thuộc tính như:

- Tên, ví dụ `eth0`.
- Địa chỉ MAC.
- Một hoặc nhiều địa chỉ IPv4/IPv6.
- MTU.
- Trạng thái `UP` hoặc `DOWN`.
- Các thống kê packet gửi, nhận, lỗi hoặc bị drop.

Trong máy vật lý, tên interface có thể đại diện cho card mạng thật. Trong container dùng bridge network, `eth0` thường không phải card vật lý. Nó thường là một đầu của **virtual Ethernet pair**, gọi tắt là `veth pair`.

```text
Network namespace của container          Network namespace của host

eth0                                     veth8a12
172.20.0.2/16  <====== veth pair =====>  gắn vào Docker bridge
```

Hai đầu hoạt động giống một sợi cáp Ethernet ảo:

- Packet được gửi vào `eth0` sẽ xuất hiện ở đầu `veth` trên host.
- Packet được gửi vào đầu `veth` trên host sẽ xuất hiện ở `eth0` của container.

Docker thường đổi tên đầu nằm trong container thành `eth0` để ứng dụng có một tên interface quen thuộc. Đầu nằm trên host thường có tên sinh tự động như `veth8a12...`.

Ví dụ kiểm tra:

```bash
docker exec api ip addr show eth0
```

Kết quả có thể gần giống:

```text
2: eth0@if15: <BROADCAST,MULTICAST,UP,LOWER_UP> mtu 1500
    link/ether 02:42:ac:14:00:02
    inet 172.20.0.2/16 scope global eth0
```

Ý nghĩa:

| Thành phần | Ý nghĩa |
| --- | --- |
| `eth0@if15` | Interface tên `eth0`; peer của nó có interface index liên quan ở namespace khác |
| `UP` | Interface đã được bật ở mức quản trị |
| `LOWER_UP` | Liên kết bên dưới đang hoạt động |
| `mtu 1500` | Packet IP tối đa thông thường trước khi cần xử lý phân mảnh là 1500 byte trên link này |
| `link/ether` | Địa chỉ MAC của interface |
| `172.20.0.2/16` | Địa chỉ IPv4 và prefix của container |

Prefix `/16` tương ứng subnet mask:

```text
255.255.0.0
```

Do đó container xem các địa chỉ từ `172.20.0.0` đến `172.20.255.255` là thuộc mạng trực tiếp theo route connected tương ứng, trừ các địa chỉ dành riêng và các route cụ thể hơn.

Một container tham gia nhiều Docker network có thể có nhiều interface:

```text
eth0 -> frontend network -> 172.21.0.4
eth1 -> backend network  -> 172.22.0.5
```

Tên `eth0`, `eth1` không nên được hard-code trong business logic. Ứng dụng nên dùng hostname, địa chỉ bind thích hợp và cấu hình service discovery.

### 1.5 `lo` là gì?

`lo` là **loopback interface**.

Nó là một interface ảo đặc biệt cho phép process giao tiếp với chính network namespace hiện tại mà packet không cần rời namespace.

Các địa chỉ loopback quen thuộc:

```text
IPv4: 127.0.0.1
IPv6: ::1
```

Hostname:

```text
localhost
```

thường được ánh xạ đến một hoặc cả hai địa chỉ trên thông qua `/etc/hosts` hoặc cơ chế phân giải tên của hệ thống.

Luồng khi một process gọi `localhost:8080`:

```text
Process client
      |
      v
TCP/IP stack của namespace hiện tại
      |
      v
lo / 127.0.0.1
      |
      v
Process server trong cùng namespace
```

Packet không đi qua:

- `eth0`.
- `veth pair`.
- Docker bridge.
- Card mạng vật lý.

Mỗi network namespace có loopback riêng:

```text
Host lo:          127.0.0.1 -> host
Container api lo: 127.0.0.1 -> container api
Container db lo:  127.0.0.1 -> container db
```

Vì vậy, `localhost` không mang nghĩa "máy gần nhất" hoặc "Docker host". Nó mang nghĩa chính xác hơn:

> Địa chỉ loopback của network namespace mà process hiện tại đang sử dụng.

Kiểm tra:

```bash
docker exec api ip addr show lo
```

Kết quả thường có:

```text
1: lo: <LOOPBACK,UP,LOWER_UP> mtu 65536
    inet 127.0.0.1/8 scope host lo
    inet6 ::1/128 scope host
```

Nếu ứng dụng bind:

```text
127.0.0.1:8080
```

thì chỉ client trong cùng network namespace kết nối được.

Nếu ứng dụng bind:

```text
0.0.0.0:8080
```

thì nó yêu cầu kernel listen trên mọi địa chỉ IPv4 local thích hợp trong namespace, thường gồm cả đường nhận traffic qua `eth0`.

### 1.6 Routing table là gì?

**Routing table** là bảng quy tắc được kernel dùng để quyết định packet phải đi theo đường nào.

Routing table trả lời các câu hỏi:

- Địa chỉ đích thuộc mạng kết nối trực tiếp hay ở mạng bên ngoài?
- Packet phải đi qua interface nào?
- Có phải gửi packet đến gateway không?
- Source IP nào nên được dùng?
- Nếu có nhiều route, route nào được ưu tiên?

Kiểm tra routing table trong container:

```bash
docker exec api ip route
```

Ví dụ:

```text
default via 172.20.0.1 dev eth0
172.20.0.0/16 dev eth0 proto kernel scope link src 172.20.0.2
```

Giải thích route thứ hai:

```text
172.20.0.0/16 dev eth0 proto kernel scope link src 172.20.0.2
```

| Thành phần | Ý nghĩa |
| --- | --- |
| `172.20.0.0/16` | Route áp dụng cho địa chỉ đích thuộc subnet này |
| `dev eth0` | Gửi packet qua interface `eth0` |
| `proto kernel` | Route được kernel tạo từ địa chỉ/prefix gán trên interface |
| `scope link` | Đích được xem là reachable trực tiếp trên link |
| `src 172.20.0.2` | Source IP được ưu tiên khi gửi qua route này |

Giải thích default route:

```text
default via 172.20.0.1 dev eth0
```

Nghĩa là:

> Nếu không có route cụ thể hơn cho địa chỉ đích, gửi packet đến gateway `172.20.0.1` qua `eth0`.

#### Kernel chọn route như thế nào?

Kernel chủ yếu ưu tiên route có prefix khớp dài nhất, gọi là **longest prefix match**.

Giả sử có:

```text
default via 172.20.0.1 dev eth0
172.20.0.0/16 dev eth0
172.20.10.0/24 via 172.20.0.254 dev eth0
```

Khi đích là `172.20.10.8`:

- Default route `/0` khớp.
- Route `/16` cũng khớp.
- Route `/24` khớp cụ thể nhất.

Kernel chọn route `/24`.

Mức độ cụ thể:

```text
/24 cụ thể hơn /16
/16 cụ thể hơn /0
```

#### Ví dụ gọi container cùng subnet

Container `api`:

```text
172.20.0.2/16
```

Container `db`:

```text
172.20.0.3/16
```

Khi `api` gửi đến `172.20.0.3`, route:

```text
172.20.0.0/16 dev eth0
```

cho biết đích nằm trên link trực tiếp. Container không gửi packet IP cho default gateway như một next hop; trước hết nó cần xác định địa chỉ Layer 2 của đích bằng ARP.

#### Ví dụ gọi Internet

Khi `api` gửi đến:

```text
8.8.8.8
```

địa chỉ này không thuộc `172.20.0.0/16`, nên kernel dùng:

```text
default via 172.20.0.1 dev eth0
```

Ở đây:

- IP destination trong packet vẫn là `8.8.8.8`.
- Ethernet frame trước mắt được gửi đến MAC của gateway `172.20.0.1`.
- Gateway/host tiếp tục route packet ra ngoài.

Routing table quyết định **next hop và interface**, không đơn giản là thay địa chỉ IP đích thành gateway.

### 1.7 ARP table là gì?

ARP là viết tắt của **Address Resolution Protocol**.

Trong mạng IPv4 Ethernet, kernel cần địa chỉ MAC để tạo Ethernet frame. Tuy nhiên ứng dụng và routing table thường làm việc với địa chỉ IP.

ARP giải bài toán:

```text
Biết IPv4 address -> tìm MAC address tương ứng trên local link
```

Ví dụ `api` muốn gửi trực tiếp đến `db`:

```text
api IP: 172.20.0.2
db IP:  172.20.0.3
```

Nếu chưa biết MAC của `db`, `api` phát ARP request:

```text
Ai đang giữ IP 172.20.0.3?
Hãy trả lời cho 172.20.0.2.
```

Thiết bị/interface giữ IP `172.20.0.3` trả lời:

```text
172.20.0.3 có MAC 02:42:ac:14:00:03
```

Kernel lưu kết quả vào ARP/neighbor table:

```text
172.20.0.3 -> 02:42:ac:14:00:03
```

Sau đó packet IP được đặt trong Ethernet frame:

```text
Ethernet destination MAC: 02:42:ac:14:00:03
IP destination:            172.20.0.3
```

Kiểm tra neighbor table:

```bash
docker exec api ip neigh
```

Ví dụ:

```text
172.20.0.1 dev eth0 lladdr 02:42:ac:14:00:01 REACHABLE
172.20.0.3 dev eth0 lladdr 02:42:ac:14:00:03 STALE
```

Ý nghĩa:

| Thành phần | Ý nghĩa |
| --- | --- |
| `172.20.0.3` | IPv4 neighbor |
| `dev eth0` | Neighbor được tiếp cận qua `eth0` |
| `lladdr ...` | Link-layer address, ở đây là MAC |
| `REACHABLE` | Kernel gần đây xác nhận neighbor có thể tiếp cận |
| `STALE` | Entry vẫn tồn tại nhưng cần được xác nhận lại khi sử dụng |

Tên gọi hiện đại thường là **neighbor table**, vì cùng công cụ `ip neigh` quản lý:

- ARP neighbor cho IPv4.
- Neighbor Discovery cho IPv6.

IPv6 không dùng ARP; nó sử dụng Neighbor Discovery Protocol dựa trên ICMPv6.

#### Khi đích ở ngoài subnet, ARP tìm MAC của ai?

Giả sử container gửi đến `8.8.8.8`.

Route cho biết phải đi qua gateway:

```text
default via 172.20.0.1 dev eth0
```

Container không ARP hỏi MAC của `8.8.8.8`, vì `8.8.8.8` không nằm trên local link. Nó ARP tìm MAC của next hop:

```text
172.20.0.1
```

Frame và packet lúc rời container có thể được hình dung:

```text
Ethernet frame:
  source MAC      = MAC của eth0 container
  destination MAC = MAC của gateway 172.20.0.1

IP packet bên trong:
  source IP       = 172.20.0.2
  destination IP  = 8.8.8.8
```

MAC address thay đổi theo từng Layer 2 hop. IP destination về cơ bản vẫn là đích cuối, trừ khi có NAT hoặc cơ chế khác sửa header.

### 1.8 `eth0`, `lo`, routing table và ARP phối hợp ra sao?

Giả sử container `api` có:

```text
lo:      127.0.0.1
eth0:    172.20.0.2/16
gateway: 172.20.0.1
```

#### Trường hợp A: gọi `localhost:8080`

```text
Destination: 127.0.0.1
       |
       v
Routing quyết định dùng loopback
       |
       v
Packet đi qua lo
       |
       v
Không cần ARP, không qua eth0
```

#### Trường hợp B: gọi `db` có IP `172.20.0.3`

```text
Docker DNS: db -> 172.20.0.3
       |
       v
Routing table: 172.20.0.0/16 dev eth0
       |
       v
ARP/neighbor table: 172.20.0.3 -> MAC của db
       |
       v
Tạo Ethernet frame và gửi qua eth0
       |
       v
veth -> Docker bridge -> container db
```

#### Trường hợp C: gọi `8.8.8.8`

```text
Destination không thuộc subnet local
       |
       v
Routing table chọn default gateway 172.20.0.1 qua eth0
       |
       v
ARP tìm MAC của gateway 172.20.0.1
       |
       v
Gửi frame qua eth0 đến gateway
       |
       v
Host route/NAT packet ra mạng ngoài
```

Tóm tắt vai trò:

| Thành phần | Câu hỏi nó trả lời |
| --- | --- |
| `lo` | Traffic có phải chỉ giao tiếp trong chính namespace không? |
| Routing table | Packet phải đi qua interface và next hop nào? |
| ARP/neighbor table | MAC của destination hoặc next hop trên local link là gì? |
| `eth0` | Packet rời/đi vào namespace qua interface nào? |

### 1.9 Ví dụ hai container có cùng port

Giả sử hai container đều chạy ứng dụng trên port `8080`:

```text
Container api-1                      Container api-2
namespace A                          namespace B

0.0.0.0:8080                         0.0.0.0:8080
```

Điều này không gây xung đột vì:

```text
namespace A / TCP port 8080
```

và:

```text
namespace B / TCP port 8080
```

là hai không gian socket khác nhau.

Xung đột chỉ xuất hiện nếu cả hai cùng publish vào một host IP và host port:

```bash
docker run -d --name api-1 -p 8080:8080 my-api
docker run -d --name api-2 -p 8080:8080 my-api
```

Container thứ hai không thể chiếm cùng `host:8080`.

Cách đúng là dùng host port khác nhau:

```bash
docker run -d --name api-1 -p 8081:8080 my-api
docker run -d --name api-2 -p 8082:8080 my-api
```

Khi đó:

```text
host:8081 -> namespace api-1 -> port 8080
host:8082 -> namespace api-2 -> port 8080
```

### 1.10 Vì sao mỗi namespace có `localhost` riêng?

`localhost` thường được phân giải thành:

```text
127.0.0.1
```

Địa chỉ này đi qua loopback interface `lo` của namespace hiện tại.

Ví dụ hai container:

```text
Container api                         Container db
network namespace A                  network namespace B

lo:   127.0.0.1                      lo:   127.0.0.1
eth0: 172.20.0.2                     eth0: 172.20.0.3
app nghe :8080                       PostgreSQL nghe :5432
```

Hai container không dùng chung `localhost`.

Trong container `api`:

```text
localhost:8080  -> chính container api
localhost:5432  -> vẫn là container api, không phải db
db:5432         -> container db nếu cả hai cùng network và DNS phân giải được tên db
```

Quy tắc nên thuộc lòng:

> `localhost` luôn chỉ network namespace hiện tại.

Điều này cũng đúng với host:

- `localhost` trên host là host.
- `localhost` trong container `api` là container `api`.
- `localhost` trong container `db` là container `db`.

Ví dụ, PostgreSQL chạy trong container `db`, còn Java chạy trong container `api`:

```text
Java gọi localhost:5432
       |
       v
loopback lo của namespace api
       |
       v
Không có PostgreSQL ở đây -> Connection refused
```

Khi Java gọi `db:5432`:

```text
Java gọi db:5432
       |
       v
Docker DNS phân giải db -> 172.20.0.3
       |
       v
Routing table của namespace api
       |
       v
eth0 của api -> veth -> bridge -> veth -> eth0 của db
       |
       v
PostgreSQL trong namespace db
```

### 1.11 Network namespace mới ban đầu có gì?

Khi tạo network namespace bằng Linux thuần, namespace mới không tự nhiên có kết nối đến host hoặc Internet.

Ví dụ trên Linux:

```bash
sudo ip netns add demo
sudo ip netns exec demo ip addr
```

Namespace mới thường chỉ có loopback interface:

```text
lo
```

Ngay cả interface `lo` cũng có thể đang ở trạng thái `DOWN`. Có thể bật nó:

```bash
sudo ip netns exec demo ip link set lo up
```

Để namespace giao tiếp với bên ngoài, cần tạo và cấu hình thêm:

1. Một interface, thường là một đầu của `veth pair`.
2. Địa chỉ IP.
3. Route.
4. Gateway hoặc đường nối phù hợp.
5. IP forwarding trên thiết bị/router trung gian nếu cần.
6. NAT nếu namespace dùng private IP để ra Internet.
7. DNS resolver nếu muốn dùng hostname.

Docker tự động hóa hầu hết các bước này.

### 1.12 Docker tạo network namespace cho container như thế nào?

Với user-defined bridge network, luồng khái quát là:

```text
docker run --network app-net ...
                 |
                 v
Docker yêu cầu container runtime tạo container
                 |
                 v
Linux tạo network namespace
                 |
                 v
Docker tạo veth pair
                 |
                 |-- một đầu gắn vào Linux bridge trên host
                 |
                 `-- một đầu chuyển vào namespace container, thường tên eth0
                 |
                 v
Docker cấp IP, route, gateway và DNS
                 |
                 v
Process chính của container chạy trong namespace đó
```

Kết quả có thể trông như sau:

```text
Host namespace

eth0: 192.168.1.10
br-abcd: 172.20.0.1
   |
   `-- veth1234
           ||
           || veth pair
           ||
Container namespace

eth0: 172.20.0.2
lo:   127.0.0.1
default route via 172.20.0.1
```

Tên `eth0` trong container và `veth1234` trên host là hai đầu của cùng một cặp veth, không phải cùng một interface.

### 1.13 Packet rời khỏi namespace như thế nào?

Giả sử container `api` có:

```text
IP:      172.20.0.2
Gateway: 172.20.0.1
```

Và muốn gọi `8.8.8.8:443`.

Kernel trong namespace `api` thực hiện:

1. Ứng dụng tạo TCP socket.
2. Kernel tra routing table của namespace `api`.
3. Route mặc định chỉ ra gateway `172.20.0.1` qua `eth0`.
4. Packet đi vào `eth0` của container.
5. Packet xuất hiện ở đầu `veth` trên host.
6. Linux bridge/host routing xử lý packet.
7. Host thực hiện forwarding.
8. Host có thể source NAT địa chỉ `172.20.0.2` thành IP của host.
9. Packet rời card mạng thật của host.

Luồng khái quát:

```text
Process trong container
        |
        v
TCP/IP stack của namespace container
        |
        v
eth0 trong container
        |
        v
veth pair
        |
        v
Linux bridge trong host
        |
        v
Routing + firewall + NAT của host
        |
        v
Card mạng vật lý
        |
        v
Mạng ngoài
```

Network namespace tạo sự cô lập, còn `veth`, bridge, routing và NAT tạo khả năng kết nối.

### 1.14 Namespace không phải Docker network

Hai khái niệm này liên quan nhưng khác nhau:

```text
Network namespace
  = network stack của một hoặc một nhóm process

Docker network
  = mạng logic kết nối các endpoint/container
```

Một container thường có một network namespace nhưng có thể tham gia nhiều Docker network:

```text
Container api
  `- một network namespace
       |- eth0 -> frontend network
       `- eth1 -> backend network
```

Ngược lại, nhiều container có nhiều network namespace riêng nhưng cùng tham gia một Docker network:

```text
Docker network backend
  |- namespace của api
  |- namespace của db
  `- namespace của redis
```

Vì vậy:

- Namespace xác định góc nhìn network của process.
- Docker network xác định các endpoint được nối với nhau theo topology nào.

### 1.15 Nhiều container có thể dùng chung network namespace

Không phải lúc nào mỗi container cũng có namespace riêng.

Docker cho phép một container dùng network namespace của container khác:

```bash
docker run -d --name web nginx:alpine
docker run --rm --network container:web curlimages/curl \
  http://localhost
```

Container `curl` dùng cùng network namespace với `web`. Vì vậy:

```text
localhost của curl = localhost của web
```

Hai container dùng chung:

- Interface.
- IP.
- Routing table.
- Port space.
- Loopback.

Nếu cả hai process cùng cố listen một IP/port, chúng sẽ xung đột vì đang ở cùng socket namespace.

Đây cũng là ý tưởng nền tảng của mô hình sidecar trong một số hệ thống orchestration. Ví dụ, các container trong cùng Kubernetes Pod chia sẻ một network namespace và có thể gọi nhau qua `localhost`.

### 1.16 `--network host` liên quan gì đến namespace?

Khi dùng:

```bash
docker run --network host my-api
```

Container không nhận một network namespace tách biệt theo mô hình bridge thông thường. Process của nó sử dụng network namespace của host.

Hệ quả:

```text
localhost trong container = network localhost của host
port trong container       = port trong network stack của host
```

Nếu host đã có process nghe port `8080`, app trong container cũng không thể bind port `8080`.

Đây là lý do host network:

- Không cần port mapping theo cách bridge network.
- Có ít network isolation hơn.
- Dễ xung đột port hơn.

### 1.17 Network namespace tồn tại bao lâu?

Về mặt Linux, namespace tồn tại miễn là còn:

- Process đang tham chiếu đến namespace đó.
- File descriptor tham chiếu đến namespace.
- Bind mount hoặc named namespace giữ nó lại.

Khi process cuối cùng rời/kết thúc và không còn tham chiếu nào, kernel có thể giải phóng namespace cùng các tài nguyên chỉ thuộc namespace đó.

Docker quản lý lifecycle này theo container. Khi container bị xóa, Docker cũng dọn:

- Endpoint.
- Interface veth.
- IP allocation.
- Tham chiếu network namespace.
- Các rule liên quan khi không còn cần thiết.

Container dừng và container bị xóa là hai trạng thái khác nhau; chi tiết tài nguyên còn được giữ có thể phụ thuộc runtime và loại network.

### 1.18 Quan sát network namespace của container

Trên Linux host, lấy PID của process chính:

```bash
docker inspect \
  --format '{{.State.Pid}}' \
  api
```

Giả sử PID là `12345`, namespace có thể được quan sát qua:

```bash
readlink /proc/12345/ns/net
```

Kết quả có dạng:

```text
net:[4026533001]
```

Con số trong ngoặc vuông đại diện cho namespace object trong kernel. Hai process có cùng giá trị `net:[...]` đang dùng cùng network namespace.

Vào network namespace của container từ host:

```bash
sudo nsenter -t 12345 -n ip addr
sudo nsenter -t 12345 -n ip route
sudo nsenter -t 12345 -n ss -lntp
```

Ý nghĩa:

- `-t 12345`: chọn process đích.
- `-n`: đi vào network namespace của process đó.
- Lệnh sau cùng chạy với góc nhìn network của namespace container.

Cách này hữu ích khi image container tối giản, không cài `ip`, `ss`, `curl` hoặc công cụ debug.

### 1.19 Thực hành network namespace bằng Linux thuần

Phần này phải chạy trên Linux có quyền quản trị, không chạy trực tiếp trong PowerShell Windows thông thường.

Tạo hai namespace:

```bash
sudo ip netns add ns-api
sudo ip netns add ns-db
```

Bật loopback:

```bash
sudo ip netns exec ns-api ip link set lo up
sudo ip netns exec ns-db ip link set lo up
```

Tạo `veth pair`:

```bash
sudo ip link add veth-api type veth peer name veth-db
```

Chuyển mỗi đầu vào một namespace:

```bash
sudo ip link set veth-api netns ns-api
sudo ip link set veth-db netns ns-db
```

Gán IP:

```bash
sudo ip netns exec ns-api \
  ip addr add 10.10.0.1/24 dev veth-api

sudo ip netns exec ns-db \
  ip addr add 10.10.0.2/24 dev veth-db
```

Bật interface:

```bash
sudo ip netns exec ns-api ip link set veth-api up
sudo ip netns exec ns-db ip link set veth-db up
```

Kiểm tra:

```bash
sudo ip netns exec ns-api ip addr
sudo ip netns exec ns-api ip route
sudo ip netns exec ns-api ping -c 3 10.10.0.2
```

Topology:

```text
namespace ns-api                    namespace ns-db

veth-api                             veth-db
10.10.0.1/24  <==== veth pair ====> 10.10.0.2/24
```

Ví dụ này nối trực tiếp hai namespace, chưa cần bridge. Docker dùng bridge khi muốn kết nối nhiều endpoint vào cùng một mạng Layer 2.

Dọn dẹp:

```bash
sudo ip netns del ns-api
sudo ip netns del ns-db
```

Khi namespace bị xóa, các virtual interface chỉ thuộc namespace đó cũng được kernel dọn theo lifecycle tương ứng.

### 1.20 Các hiểu lầm thường gặp

#### "Container có card mạng vật lý riêng"

Thông thường không. `eth0` trong bridge container thường là virtual interface, một đầu của `veth pair`.

#### "Container có kernel network riêng"

Không. Các container dùng chung Linux kernel của host, nhưng kernel duy trì network state tách biệt theo namespace.

#### "Hai container cùng IP luôn xung đột"

Nếu chúng ở các network namespace và network topology hoàn toàn tách biệt, cùng IP có thể tồn tại. Xung đột xảy ra khi các địa chỉ trùng xuất hiện trong cùng miền Layer 2/routing hoặc khi route trở nên mơ hồ.

#### "Có network namespace là tự ra Internet được"

Không. Namespace cần interface, IP, route, forwarding và có thể cần NAT/DNS.

#### "Docker network và network namespace là một"

Không. Namespace là network stack của process; Docker network là mạng logic nối các endpoint.

#### "Publish port làm process trong container đổi port"

Không. Publish port chỉ tạo đường từ host port đến socket nằm trong network namespace container.

---

## 2. Ba phạm vi cần phân biệt

Khi làm việc với Docker, nên tách ba phạm vi:

```text
Internet / mạng LAN
        |
        v
Host chạy Docker
        |
        v
Docker network
        |
        v
Container
```

Một service trong container có thể:

1. Chỉ truy cập được từ chính container.
2. Truy cập được từ container khác trong cùng Docker network.
3. Truy cập được từ host.
4. Truy cập được từ máy khác trong LAN.
5. Truy cập được từ Internet.

Việc ứng dụng "đang chạy" không có nghĩa là cả năm phạm vi đều truy cập được. Khả năng truy cập phụ thuộc vào:

- Process bind vào địa chỉ nào, ví dụ `127.0.0.1` hay `0.0.0.0`.
- Các container có cùng network không.
- Port có được publish ra host không.
- Host firewall có cho phép không.
- Router, cloud firewall, security group hoặc reverse proxy có cho phép không.

---

## 3. Các khái niệm nền tảng

### 3.1 Network namespace

Linux network namespace cô lập network stack giữa các process.

Khi Docker tạo một container bridge thông thường, Docker thường:

1. Tạo network namespace cho container.
2. Tạo một cặp virtual Ethernet gọi là `veth pair`.
3. Đưa một đầu `veth` vào namespace của container và đặt tên thường là `eth0`.
4. Gắn đầu còn lại vào Linux bridge trên host.
5. Cấp IP cho `eth0`.
6. Thêm default route trong container.
7. Thiết lập DNS.
8. Cấu hình forwarding và firewall/NAT cần thiết trên host.

### 3.2 `veth pair`

`veth pair` giống một sợi cáp Ethernet ảo có hai đầu:

```text
Đầu trong container                      Đầu trên host
eth0  <===============================>  vethxxxx
```

Packet đi vào một đầu sẽ xuất hiện ở đầu còn lại.

### 3.3 Linux bridge

Linux bridge hoạt động gần giống một switch Layer 2 bằng phần mềm.

```text
                   Linux bridge
                 docker0 / br-...
                  /      |      \
                 /       |       \
              vethA    vethB    vethC
                |        |        |
              api       db      redis
```

Bridge học địa chỉ MAC và chuyển Ethernet frame đến đúng interface, tương tự switch vật lý.

### 3.4 IPAM

IPAM là **IP Address Management**. Docker dùng IPAM để:

- Chọn subnet cho network.
- Chọn gateway.
- Cấp IP cho container endpoint.
- Tránh cấp trùng IP trong cùng network.

Ví dụ:

```text
Network:  app-net
Subnet:   172.20.0.0/16
Gateway:  172.20.0.1
api:      172.20.0.2
db:       172.20.0.3
```

Không nên phụ thuộc vào IP động của container. Container có thể nhận IP khác sau khi bị xóa và tạo lại. Hãy dùng tên service hoặc network alias.

### 3.5 Endpoint

Một container được kết nối vào một network thông qua một **endpoint**.

Một container có thể kết nối nhiều network, khi đó nó có nhiều endpoint và thường có nhiều interface/IP:

```text
Container api
  |- frontend-net: 172.21.0.5
  `- backend-net:  172.22.0.4
```

---

## 4. Container port, host port và published port

Giả sử ứng dụng Spring Boot nghe port `8080` bên trong container.

```bash
docker run --name api -p 8085:8080 my-api
```

Cú pháp:

```text
-p [host-ip:]host-port:container-port[/protocol]
```

Trong ví dụ trên:

```text
8085 = port trên host
8080 = port trong network namespace của container
```

Đường truy cập:

```text
http://localhost:8085
        |
        | port được publish trên host
        v
container api:8080
```

`-p` không làm ứng dụng đổi từ port `8080` sang `8085`. Ứng dụng vẫn nghe `8080` trong container.

### 4.1 Publish trên mọi interface của host

```bash
docker run -p 8085:8080 my-api
```

Thông thường tương đương publish trên:

```text
0.0.0.0:8085
```

Nghĩa là port có thể nhận traffic qua các IPv4 interface của host, tùy firewall và cấu hình hệ thống.

Đây có thể là một rủi ro bảo mật nếu host có thể truy cập từ mạng công cộng.

### 4.2 Chỉ publish trên loopback của host

```bash
docker run -p 127.0.0.1:8085:8080 my-api
```

Khi đó service chỉ được publish trên loopback của host:

```text
Host truy cập localhost:8085       -> được
Máy khác truy cập host-ip:8085     -> không qua binding này
```

Đây là lựa chọn tốt cho database hoặc service chỉ dành cho local development.

### 4.3 Publish UDP

```bash
docker run -p 5353:53/udp my-dns
```

TCP và UDP là hai không gian port khác nhau:

```bash
docker run \
  -p 5353:53/tcp \
  -p 5353:53/udp \
  my-dns
```

### 4.4 Để Docker tự chọn host port

```bash
docker run -p 8080 my-api
docker port <container-name>
```

Docker sẽ chọn một host port khả dụng và map vào container port `8080`.

### 4.5 Publish tất cả port được khai báo bằng `EXPOSE`

```bash
docker run -P my-api
```

`-P` viết hoa publish các port mà image khai báo bằng `EXPOSE`, sử dụng host port động.

---

## 5. `EXPOSE`, `expose` và `ports` không giống nhau

### 5.1 `EXPOSE` trong Dockerfile

```Dockerfile
EXPOSE 8080
```

`EXPOSE` chủ yếu là metadata/documentation cho biết image dự kiến phục vụ ở port nào.

Nó không tự publish port ra host.

### 5.2 `expose` trong Compose

```yaml
services:
  api:
    expose:
      - "8080"
```

`expose` mô tả port service dùng trong network nội bộ. Nó không publish port ra host.

Các container cùng network vốn đã có thể kết nối đến mọi port mà process trong container đang lắng nghe; `expose` không phải firewall rule.

### 5.3 `ports` trong Compose

```yaml
services:
  api:
    ports:
      - "8085:8080"
```

`ports` publish container port ra host:

```text
host:8085 -> api:8080
```

Tóm tắt:

| Cấu hình | Publish ra host | Vai trò chính |
| --- | --- | --- |
| Dockerfile `EXPOSE 8080` | Không | Metadata/documentation |
| Compose `expose: 8080` | Không | Mô tả port nội bộ |
| Compose `ports: 8085:8080` | Có | Host port mapping |
| CLI `-p 8085:8080` | Có | Host port mapping |

---

## 6. Process phải bind đúng địa chỉ

Đây là một lỗi rất phổ biến.

Nếu process trong container chỉ nghe:

```text
127.0.0.1:8080
```

thì chỉ process trong cùng container truy cập được. Traffic đi vào qua `eth0` của container không đến được socket bind trên loopback.

Để nhận traffic từ network của container, ứng dụng thường phải bind:

```text
0.0.0.0:8080
```

hoặc địa chỉ IP cụ thể của interface container.

Kiểm tra socket trong container:

```bash
docker exec api ss -lntp
```

Ví dụ kết quả:

```text
LISTEN 0 4096 0.0.0.0:8080 0.0.0.0:*
```

Với Spring Boot, cấu hình thường dùng:

```properties
server.address=0.0.0.0
server.port=8080
```

Thông thường Spring Boot mặc định đã nghe trên các interface phù hợp, nhưng nên kiểm tra cấu hình thực tế nếu container không nhận kết nối.

---

## 7. Các network driver chính

Docker hỗ trợ nhiều network driver. Mỗi driver giải quyết một bài toán khác nhau.

| Driver | Phạm vi điển hình | Trường hợp sử dụng |
| --- | --- | --- |
| `bridge` | Một Docker host | App nhiều container trên một máy |
| `host` | Một Docker host | Dùng trực tiếp network stack của host |
| `none` | Một container | Cô lập network hoàn toàn |
| `overlay` | Nhiều Docker host | Docker Swarm/mạng đa host |
| `macvlan` | LAN vật lý | Container xuất hiện như thiết bị riêng có MAC riêng |
| `ipvlan` | LAN vật lý | Container có IP riêng, giảm số MAC nhìn thấy ở hạ tầng |

Ngoài ra plugin bên thứ ba có thể cung cấp driver khác.

---

## 8. Bridge network

`bridge` là driver phổ biến nhất khi chạy nhiều container trên cùng một Docker host.

### 8.1 Default bridge

Nếu chạy container mà không chỉ định `--network`, Docker Engine thường kết nối nó vào network mặc định tên `bridge`:

```bash
docker run -d --name web nginx
docker network inspect bridge
```

Trên Linux, network này thường liên quan đến bridge interface `docker0`.

Default bridge có một số hạn chế và hành vi legacy. Đặc biệt, việc phân giải tên container tự động không tốt bằng user-defined bridge. Vì vậy, với ứng dụng thực tế, nên tạo network riêng.

### 8.2 User-defined bridge

```bash
docker network create app-net

docker run -d \
  --name db \
  --network app-net \
  -e POSTGRES_PASSWORD=secret \
  postgres:17

docker run -d \
  --name api \
  --network app-net \
  my-api
```

Container `api` có thể dùng:

```text
db:5432
```

Thay vì dùng IP động:

```text
172.x.x.x:5432
```

### 8.3 Vì sao user-defined bridge tốt hơn?

- Có Docker embedded DNS để phân giải tên và alias.
- Cô lập tốt hơn so với dồn mọi container vào default bridge.
- Có thể kết nối/ngắt container trong lúc container đang chạy.
- Cho phép cấu hình subnet, gateway, IPv6 và các option riêng.
- Dễ mô hình hóa từng trust boundary của ứng dụng.

### 8.4 Tạo bridge với subnet cụ thể

```bash
docker network create \
  --driver bridge \
  --subnet 172.30.0.0/16 \
  --gateway 172.30.0.1 \
  app-net
```

Chỉ nên chọn subnet thủ công khi có lý do rõ ràng, ví dụ tránh trùng với:

- VPN của công ty.
- Mạng LAN.
- VPC cloud.
- Mạng Kubernetes.
- Một Docker network khác.

Subnet overlap là nguyên nhân phổ biến làm route sai hoặc mất kết nối khi bật VPN.

### 8.5 Gán IP tĩnh

```bash
docker run -d \
  --name db \
  --network app-net \
  --ip 172.30.0.10 \
  postgres:17
```

IP tĩnh có thể cần cho phần mềm legacy, nhưng không nên là lựa chọn mặc định. DNS name ổn định hơn và giảm coupling với topology.

---

## 9. Docker DNS và service discovery

Trên user-defined network, Docker cung cấp DNS nội bộ để container tìm nhau bằng tên.

Ví dụ:

```bash
docker network create app-net
docker run -d --name redis --network app-net redis:7
docker run --rm --network app-net busybox nslookup redis
```

Tên `redis` được phân giải thành IP endpoint của container trên `app-net`.

### 9.1 Embedded DNS

Trong nhiều container trên user-defined network, `/etc/resolv.conf` sẽ trỏ đến DNS resolver nội bộ của Docker, thường thấy địa chỉ:

```text
nameserver 127.0.0.11
```

Đây là resolver được Docker cung cấp trong namespace của container. Nó:

- Phân giải tên container/service/alias trong Docker network.
- Forward truy vấn bên ngoài đến DNS upstream phù hợp.

Không nên hiểu `127.0.0.11` là một DNS server nằm trong chính image. Đây là cơ chế do Docker runtime thiết lập.

### 9.2 Network alias

Một endpoint có thể có nhiều tên:

```bash
docker run -d \
  --name postgres-primary \
  --network app-net \
  --network-alias db \
  --network-alias database \
  postgres:17
```

Các tên sau có thể trỏ đến cùng endpoint:

```text
postgres-primary
db
database
```

Alias thuộc phạm vi network. Cùng một alias có thể mang nghĩa khác nhau trên network khác.

### 9.3 DNS không phải health check

Phân giải được tên chỉ chứng minh rằng DNS có record phù hợp. Nó không chứng minh:

- Process đích đang chạy.
- Process đang nghe đúng port.
- Ứng dụng đã sẵn sàng nhận request.
- Credential đúng.
- Firewall không chặn.

Debug nên tách từng lớp:

```text
DNS -> route/IP -> TCP/UDP -> TLS -> protocol ứng dụng -> authentication
```

### 9.4 Không cache IP quá lâu

Container có thể được recreate và nhận IP mới. Client nên:

- Kết nối bằng DNS name.
- Có reconnect/retry hợp lý.
- Tránh cache DNS vô thời hạn.
- Dùng connection pool có khả năng thay connection chết.

---

## 10. Container-to-container communication

Hai container giao tiếp trực tiếp qua container port, không cần đi vòng qua host port.

Ví dụ:

```yaml
services:
  api:
    image: my-api
    ports:
      - "8085:8080"

  postgres:
    image: postgres:17
    environment:
      POSTGRES_PASSWORD: secret
```

Từ `api`, địa chỉ database đúng là:

```text
postgres:5432
```

Không phải:

```text
localhost:5432
```

Và thông thường cũng không nên dùng:

```text
host.docker.internal:5432
```

`host.docker.internal` sẽ đi đến host, trong khi database đã nằm cùng Compose network.

Port `5432` của PostgreSQL không cần publish ra host nếu chỉ `api` sử dụng nó.

Thiết kế tốt hơn:

```yaml
services:
  api:
    ports:
      - "127.0.0.1:8085:8080"

  postgres:
    image: postgres:17
    # Không có ports
```

Khi đó:

- Host truy cập API qua `localhost:8085`.
- API truy cập PostgreSQL qua `postgres:5432`.
- PostgreSQL không mở host port.

---

## 11. Host-to-container communication

Host thường truy cập container thông qua published port:

```bash
docker run -d --name api -p 127.0.0.1:8085:8080 my-api
curl http://localhost:8085
```

Trên Linux, host đôi khi có thể route trực tiếp đến container IP tùy driver và cấu hình. Tuy nhiên:

- Container IP không ổn định.
- Hành vi không portable sang Docker Desktop.
- Firewall/routing có thể khác.
- Đây không nên là contract cho ứng dụng.

Published port là cách rõ ràng và portable hơn.

---

## 12. Container-to-host communication

Nếu container cần gọi một service chạy trực tiếp trên host, `localhost` không đúng vì nó trỏ về container.

### 12.1 Docker Desktop

Docker Desktop thường cung cấp hostname đặc biệt:

```text
host.docker.internal
```

Ví dụ:

```bash
curl http://host.docker.internal:9000
```

### 12.2 Docker Engine trên Linux

Có thể thêm mapping đến host gateway:

```bash
docker run \
  --add-host=host.docker.internal:host-gateway \
  my-app
```

Compose:

```yaml
services:
  app:
    extra_hosts:
      - "host.docker.internal:host-gateway"
```

Sau đó app có thể gọi:

```text
host.docker.internal:9000
```

### 12.3 Service trên host cũng phải bind đúng

Nếu service host chỉ bind `127.0.0.1:9000`, container có thể vẫn không truy cập được qua host gateway, vì request đến một interface khác loopback.

Cần cân nhắc bind service host vào địa chỉ phù hợp và dùng firewall để giới hạn truy cập. Không nên tùy tiện bind `0.0.0.0` trên máy có kết nối mạng công cộng.

---

## 13. Từ mạng ngoài vào container: packet đi như thế nào?

Giả sử:

```bash
docker run -d --name api -p 8085:8080 my-api
```

Và client gọi:

```text
http://HOST_IP:8085
```

Mô hình khái quát trên Linux:

```text
Client
  |
  | TCP đến HOST_IP:8085
  v
Network interface của host
  |
  | firewall/NAT hoặc cơ chế port forwarding của Docker
  v
Docker bridge
  |
  v
veth phía host
  |
  v
eth0 trong container
  |
  v
socket 0.0.0.0:8080 của ứng dụng
```

Tùy phiên bản và cấu hình Docker, việc publish port có thể liên quan đến:

- Firewall/NAT rules do Docker quản lý.
- `iptables` frontend hoặc `nftables`.
- Userland proxy trong một số trường hợp/cấu hình.

Không nên viết automation phụ thuộc cứng vào một chain cụ thể nếu chưa xác nhận backend firewall và phiên bản Docker đang dùng.

### 13.1 Source NAT khi container ra Internet

Container bridge thường dùng địa chỉ private không được route trên Internet:

```text
172.20.0.2
```

Khi container gọi Internet, host thường thực hiện source NAT/masquerade:

```text
Trước NAT: source 172.20.0.2
Sau NAT:   source IP của host
```

Server bên ngoài thường thấy IP của host/NAT gateway, không thấy container IP private.

---

## 14. `host` network

Chạy:

```bash
docker run --network host my-api
```

Trong host network mode, container dùng network namespace của host thay vì có network namespace độc lập theo cách thông thường.

Hệ quả:

- App bind port `8080` thì port đó nằm trực tiếp trên host.
- Không cần `-p 8080:8080`.
- Port mapping thường không còn ý nghĩa và có thể bị bỏ qua/cảnh báo.
- `localhost` trong container trỏ vào network stack của host.
- Giảm mức cô lập network.
- Dễ xung đột port với process khác trên host.

Phù hợp khi:

- Cần hiệu năng/độ trễ đặc biệt và đã đo được lợi ích.
- Công cụ cần quan sát network host.
- Giao thức cần lượng port động lớn hoặc hành vi broadcast/multicast đặc thù.

Không nên dùng chỉ để "sửa nhanh" lỗi kết nối. Nó có thể che mất lỗi thiết kế hostname, binding hoặc port mapping.

Khả năng hỗ trợ và chi tiết hành vi của host networking trên Docker Desktop khác Linux native, nên phải kiểm tra theo phiên bản đang dùng.

---

## 15. `none` network

```bash
docker run --network none my-job
```

Container gần như không có kết nối mạng bên ngoài; thường chỉ còn loopback.

Phù hợp cho:

- Batch job không cần mạng.
- Xử lý dữ liệu nhạy cảm cần giảm bề mặt tấn công.
- Test xem ứng dụng có thực sự phụ thuộc network không.
- Sandbox có yêu cầu cô lập network.

Kiểm tra:

```bash
docker run --rm --network none alpine ip addr
```

---

## 16. Overlay network

Overlay network kết nối service/container trên nhiều Docker host, thường trong Docker Swarm.

Mô hình:

```text
Docker host A                         Docker host B
  service api                           service worker
       \                                   /
        \________ overlay network ________/
```

Docker tạo một mạng logic phủ lên hạ tầng mạng vật lý bên dưới. Traffic giữa host thường được encapsulate.

Tạo network trong Swarm:

```bash
docker network create \
  --driver overlay \
  --attachable \
  app-overlay
```

Khái niệm cần phân biệt:

- **Underlay network**: mạng thật kết nối các Docker host.
- **Overlay network**: mạng logic mà workload nhìn thấy.
- **Encapsulation**: đóng gói packet overlay bên trong packet underlay.

Overlay yêu cầu các host liên lạc được qua những port/protocol cần thiết của Swarm. Firewall sai có thể gây tình trạng control plane hoạt động nhưng data plane không truyền được traffic.

`overlay` không phải lựa chọn mặc định cho Docker Compose chạy local trên một host.

---

## 17. Macvlan

Macvlan cho phép container xuất hiện trên mạng vật lý như một thiết bị riêng, thường có:

- IP trong subnet LAN.
- MAC address riêng.
- Khả năng được thiết bị khác trong LAN truy cập trực tiếp nếu hạ tầng cho phép.

Ví dụ khái niệm:

```bash
docker network create -d macvlan \
  --subnet=192.168.1.0/24 \
  --gateway=192.168.1.1 \
  -o parent=eth0 \
  lan-net
```

Phù hợp cho:

- Ứng dụng legacy kỳ vọng nằm trực tiếp trên LAN.
- Network appliance.
- Hệ thống giám sát hoặc giao thức cần hiện diện Layer 2.

Điểm cần cẩn thận:

- Switch/NIC/hypervisor phải chấp nhận nhiều MAC trên một cổng.
- Cloud provider thường hạn chế spoofing hoặc nhiều MAC.
- Host và macvlan container mặc định có thể không nói chuyện trực tiếp theo cách người mới kỳ vọng; có thể cần macvlan sub-interface trên host.
- Quản lý IP phải tránh trùng DHCP pool.
- Làm tăng độ phức tạp vận hành.

Không dùng macvlan chỉ vì muốn container có "IP riêng dễ nhìn".

---

## 18. Ipvlan

Ipvlan cũng giúp workload tham gia mạng ngoài trực tiếp hơn, nhưng có mô hình Layer 2/Layer 3 khác macvlan và có thể giảm số lượng MAC mà hạ tầng phải học.

Phù hợp khi:

- Hạ tầng giới hạn số MAC.
- Cần container có IP riêng nhưng không muốn mỗi container có MAC riêng được thấy ở upstream.
- Có đội network hiểu rõ routing và mode ipvlan đang dùng.

Macvlan/ipvlan là chủ đề hạ tầng nâng cao. Trước khi dùng cần kiểm tra:

- Driver NIC.
- Switch port security.
- VLAN.
- DHCP/IPAM.
- Route hai chiều.
- Chính sách cloud/hypervisor.

---

## 19. Docker Compose network

Compose mặc định tạo một network cho project.

Ví dụ:

```yaml
services:
  api:
    image: my-api
    ports:
      - "8085:8080"

  postgres:
    image: postgres:17
    environment:
      POSTGRES_PASSWORD: secret
```

Nếu project tên `learning`, network thường có tên dạng:

```text
learning_default
```

Hai service có thể tìm nhau bằng service name:

```text
api
postgres
```

API kết nối database bằng:

```text
jdbc:postgresql://postgres:5432/learning
```

Không dùng `localhost` và không cần biết IP container.

### 19.1 Khai báo network rõ ràng

```yaml
services:
  api:
    image: my-api
    networks:
      - backend

  postgres:
    image: postgres:17
    networks:
      - backend

networks:
  backend:
    driver: bridge
```

### 19.2 Tách frontend và backend

```yaml
services:
  proxy:
    image: nginx:alpine
    ports:
      - "80:80"
    networks:
      - frontend

  api:
    image: my-api
    networks:
      - frontend
      - backend

  postgres:
    image: postgres:17
    networks:
      - backend

networks:
  frontend:
  backend:
    internal: true
```

Topology:

```text
Internet/host
     |
     v
   proxy
     |
 frontend network
     |
    api
     |
 backend network
     |
 postgres
```

`proxy` không cùng network với `postgres`, vì vậy không có route trực tiếp đến database qua các network này.

`api` đóng vai trò workload nối hai vùng, nhưng nó không tự động trở thành router giữa hai network trừ khi được cấu hình forwarding/routing riêng.

### 19.3 Internal network

```yaml
networks:
  backend:
    internal: true
```

`internal` tạo network bị hạn chế kết nối ra bên ngoài ở mức Docker network. Đây là lớp kiểm soát hữu ích, nhưng không thay thế toàn bộ firewall, authorization và security policy.

### 19.4 External network

Nếu network đã được tạo ngoài Compose:

```bash
docker network create shared-net
```

Compose:

```yaml
services:
  api:
    networks:
      - shared

networks:
  shared:
    external: true
    name: shared-net
```

Compose sẽ dùng `shared-net` thay vì tự tạo và quản lý lifecycle của network đó.

### 19.5 Alias trong Compose

```yaml
services:
  postgres:
    image: postgres:17
    networks:
      backend:
        aliases:
          - db
          - primary-db

networks:
  backend:
```

Trong `backend`, các tên sau có thể trỏ đến service:

```text
postgres
db
primary-db
```

### 19.6 IPAM trong Compose

```yaml
networks:
  backend:
    driver: bridge
    ipam:
      config:
        - subnet: 172.28.0.0/16
          gateway: 172.28.0.1
```

Chỉ cấu hình subnet cố định khi cần tích hợp routing/firewall hoặc tránh overlap có chủ đích.

---

## 20. `depends_on` không có nghĩa là service đã sẵn sàng

Compose có thể start container theo thứ tự, nhưng "container đã start" không đồng nghĩa:

- PostgreSQL đã nhận kết nối.
- Migration đã chạy xong.
- API đã warm up xong.
- Service đã vượt health check.

Network có thể hoàn toàn đúng nhưng request đầu tiên vẫn thất bại vì dependency chưa ready.

Ví dụ health check:

```yaml
services:
  postgres:
    image: postgres:17
    environment:
      POSTGRES_PASSWORD: secret
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 3s
      retries: 10

  api:
    image: my-api
    depends_on:
      postgres:
        condition: service_healthy
```

Ứng dụng vẫn nên có retry/backoff vì dependency có thể restart sau khi toàn bộ stack đã chạy.

---

## 21. `links` là cơ chế legacy

Compose cũ thường có:

```yaml
links:
  - postgres:postgres
```

Với Compose network hiện đại, service discovery bằng service name đã có sẵn. Thông thường không cần `links`.

Thay vì:

```yaml
services:
  api:
    links:
      - postgres:postgres
```

chỉ cần bảo đảm hai service cùng network:

```yaml
services:
  api:
    networks: [backend]

  postgres:
    networks: [backend]

networks:
  backend:
```

Sau đó dùng hostname `postgres`.

---

## 22. Network isolation không đồng nghĩa bảo mật tuyệt đối

Tách network là một lớp phòng thủ hữu ích:

- Giảm số workload có route đến service nhạy cảm.
- Giảm accidental exposure.
- Làm topology và trust boundary rõ ràng.
- Hạn chế lateral movement ở mức kết nối mạng.

Nhưng network isolation không thay thế:

- Authentication.
- Authorization.
- TLS.
- Secret management.
- Least privilege.
- Patch image.
- Read-only filesystem.
- Capability restriction.
- Host firewall.
- Cloud security group.
- Logging và monitoring.

Nếu attacker đã thực thi code trong `api`, mà `api` cần truy cập database, attacker có thể thử sử dụng chính quyền truy cập đó. Network segmentation không thể tự phân biệt request tốt và xấu từ cùng workload.

---

## 23. Inter-container communication và firewall

Docker có thể tạo và quản lý firewall rules để:

- Cho phép forwarding giữa container và bridge.
- NAT traffic đi ra.
- DNAT traffic từ published host port.
- Cô lập các bridge network.

Trên Linux, quản trị viên thường gặp:

- `iptables`.
- `nftables`.
- `firewalld`.
- `ufw`.

Các công cụ này có thể tương tác theo cách không hiển nhiên với rule do Docker tạo.

Nguyên tắc vận hành:

1. Không xóa mù quáng rule do Docker quản lý.
2. Không giả định `ufw allow/deny` là lớp duy nhất quyết định published port.
3. Dùng chain/hook dành cho policy của người quản trị theo hướng dẫn của phiên bản Docker đang chạy.
4. Kiểm tra rule thực tế sau mỗi thay đổi firewall.
5. Test cả từ host, container cùng network và máy bên ngoài.

---

## 24. IPv6

Docker network có thể được cấu hình IPv6 nếu daemon và network hỗ trợ.

Ví dụ khái niệm:

```bash
docker network create \
  --ipv6 \
  --subnet 2001:db8:1234::/64 \
  ipv6-net
```

`2001:db8::/32` là prefix dành cho tài liệu, không dùng như địa chỉ production.

Khi bật IPv6 cần xem xét:

- IPv6 forwarding.
- Router advertisement/routing.
- Host firewall cho IPv6.
- DNS AAAA record.
- Ứng dụng có listen trên IPv6 không.
- Dual-stack behavior.
- Cloud/VPC support.

Sai lầm phổ biến là cấu hình firewall IPv4 rất kỹ nhưng quên IPv6, tạo một đường truy cập ngoài ý muốn.

---

## 25. MTU và lỗi "kết nối được nhưng request lớn bị treo"

MTU là kích thước packet tối đa trên một link mà không cần phân mảnh.

Trong môi trường có:

- VPN.
- VXLAN/overlay.
- Cloud tunnel.
- PPPoE.
- Nhiều lớp encapsulation.

MTU hiệu dụng có thể nhỏ hơn Ethernet MTU phổ biến `1500`.

Triệu chứng:

- Ping nhỏ thành công.
- TCP handshake thành công.
- Request nhỏ thành công.
- TLS handshake, upload hoặc response lớn bị treo.
- Kết nối hoạt động với một số mạng nhưng hỏng khi bật VPN.

Kiểm tra:

```bash
ip link
docker network inspect <network>
```

Có thể cần cấu hình MTU của Docker bridge/network phù hợp với underlay. Không nên đoán; hãy xác định path MTU và lớp encapsulation thực tế.

---

## 26. Hairpin và gọi lại published port

Một container đôi khi gọi service khác thông qua host IP và published port, dù hai container cùng network:

```text
api -> host:8086 -> worker:8080
```

Đường đi này phức tạp hơn gọi trực tiếp:

```text
api -> worker:8080
```

Gọi qua host port có thể:

- Thêm NAT/hairpin behavior.
- Phụ thuộc cấu hình host.
- Kém portable.
- Làm log source IP khó hiểu hơn.
- Tạo lỗi chỉ xuất hiện trên một hệ điều hành.

Trong cùng Docker network, ưu tiên service DNS + container port.

---

## 27. Connection tracking và port exhaustion

NAT/firewall stateful thường theo dõi connection trong conntrack table.

Hệ thống tải cao có thể gặp:

- Quá nhiều connection ngắn.
- Conntrack table đầy.
- Ephemeral port exhaustion.
- Nhiều socket ở `TIME_WAIT`.
- Load balancer/proxy tái sử dụng connection kém.

Triệu chứng:

- Kết nối mới timeout ngẫu nhiên.
- Một số request thành công, một số thất bại.
- Restart tạm thời làm hệ thống "hết lỗi".

Hướng xử lý:

- Dùng connection pool.
- Bật keep-alive phù hợp.
- Giới hạn concurrency.
- Theo dõi conntrack và socket state.
- Scale đúng tầng.
- Tối ưu timeout thay vì tùy tiện tăng mọi giới hạn.

Đây thường là vấn đề vận hành host/network, không phải lỗi DNS Docker đơn giản.

---

## 28. Các lệnh quản lý network

### 28.1 Liệt kê network

```bash
docker network ls
```

### 28.2 Xem chi tiết network

```bash
docker network inspect app-net
```

Thông tin quan trọng:

- Driver.
- Scope.
- Subnet.
- Gateway.
- Options.
- Internal/attachable.
- Container endpoint và IP.

### 28.3 Tạo network

```bash
docker network create app-net
```

### 28.4 Kết nối container đang chạy

```bash
docker network connect app-net api
```

### 28.5 Ngắt container khỏi network

```bash
docker network disconnect app-net api
```

Nếu đây là đường quản trị duy nhất hoặc ứng dụng đang dùng connection qua network đó, thao tác có thể làm gián đoạn service.

### 28.6 Xóa network

```bash
docker network rm app-net
```

Docker thường không cho xóa network nếu vẫn có endpoint đang gắn vào.

### 28.7 Xóa network không còn dùng

```bash
docker network prune
```

Lệnh này thay đổi hệ thống và có thể ảnh hưởng network bạn định tái sử dụng. Luôn kiểm tra:

```bash
docker network ls
docker network inspect <network>
```

trước khi xác nhận prune.

---

## 29. Quy trình debug theo từng lớp

Không nên thử ngẫu nhiên `restart`, đổi port hoặc dùng `--network host`. Hãy đi từ tên đến ứng dụng.

### Bước 1: Xác định nguồn và đích

Ghi rõ:

```text
Nguồn: container api
Đích:  service postgres
Tên:   postgres
Port:  5432/TCP
```

Phân biệt request xuất phát từ:

- Host.
- Container.
- Máy khác trong LAN.
- Internet.

### Bước 2: Container có đang chạy?

```bash
docker ps
docker compose ps
```

Xem log:

```bash
docker logs api
docker logs postgres
docker compose logs api postgres
```

### Bước 3: Hai container có cùng network?

```bash
docker inspect api
docker inspect postgres
docker network inspect <network-name>
```

Chú ý trường:

```text
NetworkSettings.Networks
```

### Bước 4: DNS có phân giải được không?

Nếu image có công cụ:

```bash
docker exec api getent hosts postgres
```

Hoặc dùng debug container cùng network:

```bash
docker run --rm --network <network-name> busybox nslookup postgres
```

Nếu DNS thất bại:

- Sai service/container name.
- Không cùng network.
- Alias không nằm trên network đó.
- Container/network vừa thay đổi và client cache DNS cũ.

### Bước 5: Kiểm tra TCP port

```bash
docker exec api sh -c "nc -vz postgres 5432"
```

Không phải image nào cũng có `nc`.

Có thể dùng debug container:

```bash
docker run --rm --network <network-name> nicolaka/netshoot \
  nc -vz postgres 5432
```

Khi dùng image debug bên ngoài, cần cân nhắc chính sách supply-chain và pin phiên bản/digest trong môi trường nghiêm ngặt.

### Bước 6: Process đích có listen không?

```bash
docker exec postgres ss -lntp
```

Nếu image không có `ss`, xem log ứng dụng hoặc dùng công cụ phù hợp với image.

Phân biệt:

```text
127.0.0.1:5432 -> chỉ loopback trong container
0.0.0.0:5432   -> tất cả IPv4 interface trong container
[::]:5432      -> IPv6 wildcard, hành vi dual-stack tùy hệ thống
```

### Bước 7: Kiểm tra protocol ứng dụng

TCP connect được chưa có nghĩa là protocol đúng.

HTTP:

```bash
curl -v http://api:8080/actuator/health
```

TLS:

```bash
openssl s_client -connect api:8443 -servername api
```

PostgreSQL:

```bash
pg_isready -h postgres -p 5432
```

### Bước 8: Kiểm tra route và interface

```bash
docker exec api ip addr
docker exec api ip route
docker exec api cat /etc/resolv.conf
```

### Bước 9: Kiểm tra port publish

```bash
docker port api
docker inspect api
```

Xác nhận:

- Host IP.
- Host port.
- Container port.
- Protocol TCP/UDP.

### Bước 10: Kiểm tra từ đúng vị trí client thật

Nếu lỗi xảy ra từ máy khác, test trên host là chưa đủ.

```text
curl trong container -> test container path
curl trên host       -> test host path
curl từ máy LAN      -> test LAN + host firewall path
curl từ Internet     -> test toàn bộ ingress path
```

---

## 30. Ý nghĩa các lỗi thường gặp

### `Connection refused`

Thường có nghĩa:

- Đã đi đến host/IP đích.
- Nhưng không có process listen port đó, hoặc firewall chủ động reject.

Kiểm tra:

- Sai port.
- App chưa start.
- App bind `127.0.0.1`.
- Container restart loop.

### `Connection timed out`

Thường gợi ý packet bị drop hoặc không có route/phản hồi:

- Firewall drop.
- Route sai.
- Subnet overlap.
- Security group.
- Service treo.
- Overlay/underlay hỏng.

### `Name or service not known`

DNS/name resolution thất bại:

- Sai service name.
- Không cùng network.
- DNS config sai.
- Alias sai scope.

### `No route to host`

Có thể do:

- Không có route.
- Interface/network down.
- ICMP reject từ firewall.
- Subnet conflict.

### TCP được nhưng HTTP lỗi `502`/`504`

Thường cần xem reverse proxy:

- `502 Bad Gateway`: proxy không nhận response hợp lệ từ upstream.
- `504 Gateway Timeout`: upstream không trả lời kịp.

Kiểm tra hostname upstream, container port, scheme HTTP/HTTPS, timeout và readiness.

---

## 31. Những sai lầm phổ biến

### 31.1 Dùng `localhost` để gọi container khác

Sai:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/learning
```

Đúng khi service Compose tên `postgres`:

```properties
spring.datasource.url=jdbc:postgresql://postgres:5432/learning
```

### 31.2 Dùng host port giữa các container

Sai về mặt thiết kế phổ biến:

```text
api -> host.docker.internal:5432 -> postgres
```

Đơn giản hơn:

```text
api -> postgres:5432
```

### 31.3 Publish database dù không cần

Không cần:

```yaml
postgres:
  ports:
    - "5432:5432"
```

Nếu chỉ API dùng database, bỏ `ports`.

### 31.4 Phụ thuộc vào container IP

Sai:

```text
jdbc:postgresql://172.19.0.4:5432/learning
```

Đúng:

```text
jdbc:postgresql://postgres:5432/learning
```

### 31.5 Nhầm host port với container port

Compose:

```yaml
ports:
  - "8085:8080"
```

Từ host:

```text
localhost:8085
```

Từ container khác:

```text
api:8080
```

### 31.6 Cho tất cả container vào một network

Điều này làm mọi workload có route đến nhau, tăng blast radius.

Nên tách theo nhu cầu giao tiếp:

```text
public/proxy network
application network
data network
observability network
```

Không cần tách cực đoan mỗi container một network; mục tiêu là mô hình hóa trust boundary hợp lý.

### 31.7 Dùng `--network host` để chữa mọi lỗi

Nếu host mode làm app chạy, nguyên nhân gốc thường vẫn là:

- Sai hostname.
- Sai binding.
- Sai port mapping.
- Hai container khác network.
- Firewall/NAT.

Hãy tìm nguyên nhân trước khi chấp nhận giảm isolation.

---

## 32. Thiết kế network cho Spring Boot + PostgreSQL

Ví dụ an toàn hơn cho local development:

```yaml
services:
  api:
    build: ..
    restart: unless-stopped
    ports:
      - "127.0.0.1:8085:8086"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/learning
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - app
      - data

  postgres:
    image: postgres:17
    restart: unless-stopped
    environment:
      POSTGRES_DB: learning
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d learning"]
      interval: 5s
      timeout: 3s
      retries: 10
    networks:
      - data

volumes:
  postgres_data:

networks:
  app:
  data:
    internal: true
```

Lưu ý:

- Host chỉ truy cập API qua `127.0.0.1:8085`.
- PostgreSQL không publish port ra host.
- API gọi `postgres:5432`.
- Password không nên commit trực tiếp; ví dụ dùng biến môi trường chỉ minh họa cách truyền cấu hình, chưa phải secret management production hoàn chỉnh.
- Network `data` là internal.
- Nếu API cần gọi Internet, việc đồng thời nối `app` và `data` cho phép API có đường qua network không internal; database vẫn chỉ nối `data`.

Nếu host cần dùng IDE/database client kết nối PostgreSQL trong local development, có thể tạm publish chỉ trên loopback:

```yaml
postgres:
  ports:
    - "127.0.0.1:5432:5432"
```

Không nên giữ mapping này trong cấu hình production nếu không cần.

---

## 33. Reverse proxy và network

Mô hình production phổ biến:

```text
Internet
   |
   v
Load balancer / reverse proxy
   |
   v
API containers
   |
   v
Database
```

Chỉ reverse proxy publish port public:

```yaml
services:
  proxy:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    networks:
      - edge

  api:
    image: my-api
    networks:
      - edge
      - data

  postgres:
    image: postgres:17
    networks:
      - data

networks:
  edge:
  data:
    internal: true
```

Proxy upstream nên dùng:

```text
http://api:8080
```

không dùng host port của API.

Khi chạy nhiều replica, cơ chế service discovery/load balancing phụ thuộc orchestrator. Docker Compose local, Docker Swarm và Kubernetes có mô hình khác nhau; không nên suy diễn behavior của một hệ sang hệ khác.

---

## 34. Logging và quan sát network

Khi điều tra sự cố, nên thu thập:

- Timestamp có timezone.
- Source container/service.
- Destination hostname/IP/port.
- Protocol.
- DNS result.
- Connection timeout.
- HTTP status hoặc database error.
- Container restart count.
- Network ID và endpoint IP tại thời điểm lỗi.

Công cụ thường dùng trên Linux/debug container:

```bash
ip addr
ip route
ss -lntup
getent hosts <name>
nslookup <name>
dig <name>
nc -vz <host> <port>
curl -v <url>
traceroute <host>
tcpdump
```

`ping` không đủ để kết luận service hoạt động:

- ICMP có thể bị chặn trong khi TCP vẫn hoạt động.
- DNS đúng nhưng app port sai.
- Ping IP được nhưng TLS/HTTP lỗi.
- Container/image có thể không cài `ping`.

Packet capture rất mạnh nhưng có thể chứa dữ liệu nhạy cảm. Chỉ capture đúng interface/filter cần thiết và bảo vệ file capture.

Ví dụ filter khái niệm:

```bash
tcpdump -i any host 172.20.0.3 and port 5432
```

---

## 35. Checklist bảo mật

- Chỉ publish port thực sự cần.
- Khi chỉ cần local, bind host port vào `127.0.0.1`.
- Không publish database/cache/message broker ra public Internet.
- Tách network theo trust boundary.
- Dùng `internal: true` cho vùng không cần egress khi phù hợp.
- Dùng TLS cho traffic qua mạng không tin cậy.
- Vẫn bật authentication giữa các service nhạy cảm.
- Không hard-code secret trong Compose được commit.
- Kiểm tra cả firewall IPv4 và IPv6.
- Theo dõi thay đổi firewall do Docker/OS update.
- Không dựa vào container IP cố định nếu không bắt buộc.
- Không dùng host network nếu không có lý do đã được đánh giá.
- Cập nhật Docker Engine/Desktop và image.
- Ghi log connection failure nhưng không log password/token.

---

## 36. Bài thực hành 1: DNS giữa hai container

Tạo network:

```bash
docker network create lab-net
```

Chạy web server:

```bash
docker run -d --name lab-web --network lab-net nginx:alpine
```

Phân giải tên và gọi HTTP từ container khác:

```bash
docker run --rm --network lab-net busybox nslookup lab-web
docker run --rm --network lab-net busybox wget -qO- http://lab-web
```

Quan sát:

- Không cần publish port.
- Client gọi `lab-web:80`.
- Docker DNS trả về IP của endpoint `lab-web`.

Dọn dẹp:

```bash
docker rm -f lab-web
docker network rm lab-net
```

---

## 37. Bài thực hành 2: Chứng minh `localhost` là riêng

Tạo network và server:

```bash
docker network create localhost-lab
docker run -d --name web-a --network localhost-lab nginx:alpine
```

Từ container khác, lệnh này sai:

```bash
docker run --rm --network localhost-lab busybox \
  wget -qO- http://localhost
```

Vì `localhost` là container BusyBox vừa tạo.

Lệnh đúng:

```bash
docker run --rm --network localhost-lab busybox \
  wget -qO- http://web-a
```

Dọn dẹp:

```bash
docker rm -f web-a
docker network rm localhost-lab
```

---

## 38. Bài thực hành 3: Container port và host port

```bash
docker run -d \
  --name port-lab \
  -p 127.0.0.1:8088:80 \
  nginx:alpine
```

Từ host:

```bash
curl http://localhost:8088
```

Kiểm tra mapping:

```bash
docker port port-lab
```

Quan sát:

```text
host dùng 8088
nginx trong container vẫn dùng 80
```

Dọn dẹp:

```bash
docker rm -f port-lab
```

---

## 39. Bài thực hành 4: Cô lập bằng hai network

```bash
docker network create front-lab
docker network create data-lab

docker run -d --name proxy-lab --network front-lab nginx:alpine
docker run -d --name db-lab --network data-lab redis:7-alpine

docker run -d --name api-lab --network front-lab alpine sleep 1d
docker network connect data-lab api-lab
```

Kết quả topology:

```text
proxy-lab -- front-lab -- api-lab -- data-lab -- db-lab
```

`api-lab` thấy cả hai network. `proxy-lab` và `db-lab` không cùng network.

Kiểm tra:

```bash
docker network inspect front-lab
docker network inspect data-lab
docker inspect api-lab
```

Dọn dẹp:

```bash
docker rm -f proxy-lab db-lab api-lab
docker network rm front-lab data-lab
```

---

## 40. Bảng quyết định nhanh

| Nhu cầu | Cách phù hợp |
| --- | --- |
| Hai container trên cùng host giao tiếp | User-defined `bridge` |
| Stack local bằng Compose | Compose network mặc định hoặc network khai báo rõ |
| Container gọi container khác | Service/container DNS name + container port |
| Host gọi container | Publish port bằng `-p`/`ports` |
| Container gọi service trên host | `host.docker.internal`, cấu hình host gateway khi cần |
| Chỉ cho host local truy cập | Bind `127.0.0.1:host-port:container-port` |
| Container không cần mạng | `none` |
| Workload dùng network stack host | `host`, sau khi đánh giá isolation/port conflict |
| Nhiều Docker host trong Swarm | `overlay` |
| Container xuất hiện trực tiếp trên LAN | Cân nhắc `macvlan`/`ipvlan` |
| Database chỉ dành cho API | Cùng private network, không publish DB port |
| Tách proxy/API/database | Hai network: edge và data |

---

## 41. Tóm tắt nguyên tắc cốt lõi

1. Mỗi container thường có network namespace riêng.
2. `localhost` là chính namespace hiện tại, không phải container khác và không phải host.
3. Container cùng user-defined network gọi nhau bằng DNS name và container port.
4. `-p 8085:8080` nghĩa là host `8085` chuyển đến container `8080`.
5. `EXPOSE` không tự publish port.
6. App phải listen trên interface phù hợp, thường là `0.0.0.0`.
7. Không phụ thuộc vào IP động của container.
8. Không publish database nếu chỉ service nội bộ cần dùng.
9. Tách network theo trust boundary nhưng vẫn cần authentication, TLS và firewall.
10. Debug theo lớp: DNS, route, TCP/UDP, TLS, protocol ứng dụng và authentication.

Mô hình ngắn gọn cuối cùng:

```text
Từ host:
localhost:<host-port>
        |
        | published port
        v
<container>:<container-port>

Từ container khác:
<service-name>:<container-port>

Từ container đến host:
host.docker.internal:<host-service-port>
```
