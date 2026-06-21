<!--
Nguồn: Google Docs
Nội dung câu chữ và hình ảnh được giữ nguyên; chỉ bổ sung Markdown để dễ đọc.
-->

- **Config devops:** https://chat.deepseek.com/a/chat/s/121532f4-df5d-4dfc-9714-3d4fb1139016

- Subnet, Network Address, Broadcast Address

- CCNA sisco

- https://www.geeksforgeeks.org/computer-networks/computer-network-tutorials/

- https://vnpro.vn/chuong-trinh-dao-tao/khoa-hoc-ccna-online-200301-78.html

# Computer Network

- Computer network là 1 hệ thống gồm các node được kết nối với nhau để communicate và resource sharing dữ liệu, dịch vụ,... thông qua communication links

- Node chính là các thiết bị như computer, printer, phone, switch, bridge, router, ô tô kết nối mạng... có khả năng gửi nhận dữ liệu

- Communication link (media) giúp các node có thể kết nối với nhau + chỉ support truyền tải thông tin giữa các node.

- Resource sharing tức chia sẻ tài nguyên giữa các thiết bị.

- Số lượng Port đề cập tới nó có thể connect với bao nhiêu device (mỗi port thì có thể có 1 connection)

### Node được chia làm 2 loại:

| Loại node | Nội dung nguyên bản |
|---|---|
| End nodes | End nodes (End system/ Hosts/ Internet Edge): là các thiết bị đầu cuối, sử dụng để khởi tạo và gửi request và là nơi cuối cùng để nhận dữ liệu (máy tính, điện thoại, máy in, VoIP phone, credit card, barcode scanner, web server, ô tô kết nối mạng, ...). Điểm chung là chúng đều chạy các ứng dụng mạng, nằm ở phần rìa của mạng. |
| Intermediary nodes | Intermediary nodes (Internet Core) là các node trung gian trong mạng mà dữ liệu sẽ đi qua khi truyền dữ liệu giữa các end nodes, chỉ đóng vai trò chuyển tiếp, định tuyến, chuyển mạch dữ liệu giữa các end nodes (router, wireless router, bridges, switch, hub, repeater, access endpoint, cell tower, modern, firewall,...) để điều hướng, đảm bảo dữ liệu đến đúng đích. |

### Phân loại network (mỗi một vùng xanh xanh bên dưới sẽ đại diện cho 1 network)

![Phân loại các network trong Internet](assets/image34.png)

*Phân loại các network trong Internet*

![Phân loại các network trong Internet](assets/image87.png)

*Phân loại các network trong Internet*

| Loại network | Nội dung nguyên bản |
|---|---|
| Home network | Home network: hệ thống mạng trong nhà (như máy tính, điện thoại,…) |
| Enterprise network | Enterprise network: hệ thống mạng doanh nghiệp sẽ có nhiều thiết bị hơn, nên cần các switch tốt hơn, cần nhiều bộ router hơn. |
| Mobile network | Mobile network: hệ thống mạng có tính di động cao, di chuyển theo thời gian (thường kết nối với các điểm phát sóng) |
| Content provider network | Content provider network: hệ thống mạng phục vụ các công ty lớn (như youtube, netflix chẳng hạn) nên lượng dữ liệu của họ cực kỳ lớn, họ tự xây dựng hệ thống mạng riêng của mình rồi kết nối tới các nhà cung cấp dịch vụ. |

- ⇒ nên có thể nói Internet = Network of networks do chúng được tạo thành từ nhiều network lại.

## Phân loại quy mô

Mạng có thể được phân loại theo phạm vi địa lý mà nó kết nối. Ranh giới giữa các loại chỉ mang tính tương đối; không có một số mét, số km hoặc tốc độ cố định để quyết định mạng thuộc loại nào.

| Loại mạng | Phạm vi điển hình | Mục đích và ví dụ | Công nghệ/thiết bị thường gặp | Chủ thể quản lý |
|---|---|---|---|---|
| **PAN (Personal Area Network)** | Xung quanh một người, thường trong vài mét | Điện thoại kết nối tai nghe, đồng hồ thông minh hoặc chia sẻ dữ liệu với laptop | Bluetooth, NFC, USB, Wi-Fi Direct | Một cá nhân |
| **LAN (Local Area Network)** | Nhà ở, phòng làm việc, một tầng hoặc một tòa nhà | Kết nối máy tính, máy in, server và các thiết bị trong gia đình/văn phòng | Ethernet, switch, router, access point | Cá nhân hoặc một tổ chức |
| **WLAN (Wireless LAN)** | Phạm vi tương tự LAN nhưng thiết bị truy cập không dây | Wi-Fi tại nhà, văn phòng, quán cà phê | Wi-Fi và access point | Cá nhân hoặc một tổ chức |
| **CAN (Campus Area Network)** | Nhiều tòa nhà trong cùng khuôn viên | Kết nối các LAN của trường đại học, bệnh viện, nhà máy hoặc khu công nghiệp | Ethernet tốc độ cao, cáp quang, switch Layer 3, router | Một tổ chức |
| **MAN (Metropolitan Area Network)** | Một đô thị hoặc khu vực liên đô thị | Kết nối nhiều cơ sở trong một thành phố; mạng Metro Ethernet của nhà cung cấp dịch vụ | Metro Ethernet, cáp quang, microwave | ISP, chính quyền hoặc tổ chức lớn |
| **WAN (Wide Area Network)** | Nhiều tỉnh, quốc gia hoặc châu lục | Kết nối các văn phòng ở xa nhau, mạng đường trục của nhà mạng | MPLS, leased line, SD-WAN, cáp quang đường dài/cáp biển, microwave, vệ tinh | Một hoặc nhiều nhà mạng/tổ chức |
| **GAN (Global Area Network)** | Toàn cầu | Thuật ngữ mô tả mạng hoạt động trên phạm vi toàn thế giới | Kết hợp nhiều công nghệ WAN | Nhiều tổ chức và nhà mạng |

### Những điểm cần phân biệt

- **Phạm vi, không phải tốc độ, là tiêu chí chính:** LAN thường có độ trễ thấp và băng thông cao hơn WAN, nhưng một WAN hiện đại vẫn có thể nhanh hơn một LAN cũ. Vì vậy không thể phân loại chỉ dựa vào Mbps/Gbps.
- **Internet không phải một WAN đơn lẻ:** Internet là một *network of networks* kết nối rất nhiều mạng LAN, MAN, WAN và mạng của các nhà cung cấp dịch vụ bằng bộ giao thức TCP/IP. Có thể xem Internet là ví dụ lớn nhất của một mạng liên kết toàn cầu.
- **CAN và GAN không phổ quát bằng LAN/MAN/WAN:** các tài liệu có thể bỏ qua hai thuật ngữ này. `CAN` trong phần này là **Campus Area Network**, không phải giao thức **Controller Area Network** dùng trong ô tô và hệ thống nhúng.
- **LAN hiện đại:** thiết bị có dây thường kết nối qua switch; thiết bị không dây kết nối qua access point. Hub và bridge rời chủ yếu có ý nghĩa lịch sử hoặc phục vụ học nguyên lý.
- **Kết nối nhiều mạng:** router hoặc switch Layer 3 được dùng để chuyển packet giữa các mạng IP khác nhau; switch Layer 2 chủ yếu chuyển frame giữa các thiết bị trong cùng LAN/VLAN.
- **Quyền sở hữu không quyết định tuyệt đối loại mạng:** LAN thường do một cá nhân/tổ chức quản lý, còn MAN/WAN thường phải thuê hoặc dùng hạ tầng của nhà cung cấp dịch vụ.
- **VPN không phải một loại mạng theo quy mô:** VPN tạo một đường hầm logic, thường được mã hóa, chạy trên hạ tầng mạng có sẵn để kết nối người dùng hoặc các địa điểm từ xa.

### VPN (Virtual Private Network)

**VPN (Virtual Private Network)** tạo một mạng hoặc đường truyền logic (*tunnel*) chạy trên một mạng khác, thường là Internet. Tunnel đóng gói packet gốc vào packet mới để vận chuyển giữa các đầu VPN.

VPN hiện đại thường cung cấp **mã hóa, xác thực và kiểm tra tính toàn vẹn**, nhưng “tunnel” và “mã hóa” không đồng nghĩa tuyệt đối. Một số công nghệ tunneling như GRE tự nó không mã hóa dữ liệu và phải kết hợp với IPsec nếu cần bảo mật.

#### Thành phần chính

| Thành phần | Vai trò |
|---|---|
| **VPN client/peer** | Phần mềm hoặc thiết bị khởi tạo/kết thúc tunnel, mã hóa và giải mã lưu lượng. Trong site-to-site, hai đầu thường được gọi là các VPN peer/gateway thay vì client và server. |
| **VPN gateway/server** | Xác thực peer/người dùng, kết thúc tunnel, cấp cấu hình mạng và chuyển tiếp packet tới mạng nội bộ hoặc Internet theo chính sách. |
| **Virtual network interface** | Interface logic như `tun0`, được hệ điều hành coi gần giống card mạng để đưa lưu lượng phù hợp vào tunnel. |
| **VPN protocol** | Quy định cách bắt tay, xác thực, trao đổi khóa và đóng gói dữ liệu; ví dụ WireGuard, OpenVPN hoặc IKEv2/IPsec. |
| **Routing và policy** | Xác định lưu lượng nào đi qua VPN, người dùng được truy cập mạng/dịch vụ nào và DNS nào được sử dụng. |

> **OpenVPN không chỉ là VPN client:** đây là phần mềm và giao thức VPN mã nguồn mở có cả phía client lẫn server. UDP `1194` là cổng mặc định phổ biến nhưng có thể cấu hình cổng và transport khác; TCP `443` không phải yêu cầu bắt buộc.

#### Các loại VPN phổ biến

| Loại | Kết nối | Trường hợp sử dụng |
|---|---|---|
| **Remote-access VPN** | Một thiết bị người dùng ↔ VPN gateway | Nhân viên từ nhà truy cập server, database hoặc máy tính trong công ty |
| **Site-to-site VPN** | Gateway của mạng A ↔ gateway của mạng B | Kết nối LAN của trụ sở với LAN chi nhánh; máy người dùng thường không chạy VPN client |
| **Consumer VPN** | Thiết bị người dùng ↔ máy chủ của nhà cung cấp VPN | Bảo vệ lưu lượng trên mạng không tin cậy hoặc thay đổi địa chỉ IP thoát ra Internet |
| **Host-to-host VPN** | Một máy ↔ một máy | Bảo vệ trực tiếp lưu lượng giữa hai host cụ thể |

Remote-access và site-to-site là hai mô hình khác nhau, không nên gộp thành một loại.

#### Full tunnel và split tunnel

| Chế độ | Lưu lượng đi qua VPN | Đặc điểm |
|---|---|---|
| **Full tunnel** | Thường có route mặc định `0.0.0.0/0` và/hoặc `::/0`, nên hầu hết lưu lượng đi qua VPN | Dễ áp dụng chính sách tập trung; tăng tải và có thể tăng độ trễ |
| **Split tunnel** | Chỉ các route được chỉ định, chẳng hạn `10.0.0.0/8`, đi qua VPN | Internet thông thường vẫn đi trực tiếp; tiết kiệm băng thông nhưng cần quản lý route và DNS cẩn thận |

Vì vậy, bật VPN công ty không nhất thiết làm mọi truy cập Internet đi qua công ty. Điều này phụ thuộc vào routing policy được cấp cho client.

#### Luồng hoạt động của remote-access VPN

1. **Kết nối và xác thực:** client liên hệ địa chỉ public của VPN gateway. Hai bên bắt tay, xác thực bằng certificate, khóa, tài khoản/MFA hoặc cơ chế do giao thức hỗ trợ, rồi tạo session key.
2. **Cấp cấu hình:** client có thể nhận một IP ảo, DNS và các route được phép. IP ảo này không nhất thiết thuộc cùng subnet Layer 2 với máy công ty; VPN gateway có thể định tuyến giữa subnet VPN và các subnet nội bộ.
3. **Đóng gói:** hệ điều hành chọn interface VPN theo routing table. VPN client mã hóa packet gốc rồi đóng nó trong một packet ngoài có đích là VPN gateway.
4. **Truyền qua Internet:** trước NAT, IP nguồn ngoài có thể là IP private của laptop; router nhà sẽ đổi nó thành public IP của đường truyền. ISP nhìn thấy kết nối tới VPN gateway cùng metadata như thời gian và lượng dữ liệu, nhưng không đọc được payload đã mã hóa.
5. **Tháo gói và kiểm soát truy cập:** gateway xác minh, giải mã packet, sau đó firewall/ACL kiểm tra danh tính, IP, route, port và policy trước khi chuyển tới đích. Kết nối VPN thành công không có nghĩa người dùng được truy cập toàn bộ LAN.
6. **Phản hồi:** packet trả về được gateway mã hóa và đóng gói ngược về client.

Packet có hai lớp địa chỉ khi nằm trong tunnel:

| Lớp packet | IP nguồn | IP đích | Ý nghĩa |
|---|---|---|---|
| **Packet trong (inner)** | IP ảo của VPN client, ví dụ `10.0.5.100` | Máy nội bộ, ví dụ `10.0.1.20` | Giao tiếp logic mà ứng dụng muốn thực hiện |
| **Packet ngoài (outer), sau NAT nhà** | Public IP của mạng nhà | Public IP của VPN gateway | Vận chuyển phần dữ liệu đã mã hóa qua Internet |

#### Truy cập Remote Desktop qua VPN

Công ty có thể giữ máy nội bộ không truy cập trực tiếp được từ Internet và chỉ công khai VPN gateway. Sau khi client kết nối:

- Route tới subnet công ty được đưa qua virtual interface.
- Firewall chỉ cho phép đúng người dùng/nhóm và đúng dịch vụ cần thiết, ví dụ RDP TCP/UDP `3389`.
- Người dùng kết nối RDP tới IP/DNS nội bộ của máy đích mà không cần port-forward trực tiếp cổng RDP trên router công ty.
- VPN gateway vẫn phải có một endpoint có thể truy cập từ Internet, hoặc dùng kiến trúc overlay/relay khác; không thể nói rằng “không public gì ra Internet”.

![Mô hình truy cập mạng công ty qua VPN](assets/image66.png)

*Mô hình remote-access VPN. Việc truy cập từng máy chủ vẫn phụ thuộc vào route và firewall policy.*

#### VPN bảo vệ và không bảo vệ điều gì?

| Nhận định | Đánh giá chính xác |
|---|---|
| VPN bảo vệ trên Wi-Fi công cộng | **Đúng nhưng cần điều kiện:** VPN mã hóa lưu lượng từ thiết bị tới VPN gateway. HTTPS vẫn cần thiết để bảo vệ dữ liệu từ thiết bị tới website và xác thực website. |
| ISP không biết website đang truy cập | **Không tuyệt đối:** với full tunnel và DNS không bị leak, ISP chủ yếu thấy kết nối tới VPN gateway. ISP vẫn thấy metadata và có thể suy luận; split tunnel hoặc DNS leak có thể làm lộ thêm thông tin. |
| Website thấy IP của VPN server | **Thường đúng với consumer full-tunnel:** gateway thường NAT lưu lượng Internet sang exit IP. Với VPN công ty chỉ định tuyến tài nguyên nội bộ, truy cập website công cộng có thể vẫn dùng IP mạng nhà. |
| VPN giúp vượt giới hạn địa lý | **Có thể, không bảo đảm:** dịch vụ có thể phát hiện/chặn IP VPN; việc sử dụng còn phụ thuộc điều khoản dịch vụ và pháp luật tại nơi sử dụng. |
| VPN làm người dùng ẩn danh | **Sai:** nhà cung cấp VPN có thể thấy metadata hoặc lưu lượng không mã hóa sau điểm thoát; website vẫn có cookie, tài khoản, browser fingerprint và các cách nhận diện khác. VPN chuyển điểm phải tin cậy từ ISP sang đơn vị vận hành VPN. |
| Có VPN là an toàn trước malware/phishing | **Sai:** VPN không tự chặn mã độc, trang giả mạo, lộ mật khẩu hay thiết bị đã bị xâm nhập. |
| Kết nối VPN nghĩa là thuộc hoàn toàn vào LAN từ xa | **Không chính xác:** client có một interface/IP logic và các route được cấp; quyền thực tế vẫn do segmentation, firewall, ACL và mô hình zero-trust quyết định. |

#### Hạn chế và rủi ro cần nhớ

- Mã hóa/đóng gói làm tăng overhead, có thể giảm throughput và tăng latency.
- MTU không phù hợp có thể gây fragmentation hoặc lỗi “truy cập được nhưng tải không xong”; thường cần điều chỉnh tunnel MTU/MSS.
- DNS leak, IPv6 leak hoặc route sai có thể khiến một phần lưu lượng đi ngoài tunnel.
- Kill switch có thể chặn lưu lượng khi VPN rớt, nhưng phải được cấu hình và kiểm thử đúng.
- Không nên dùng giao thức cũ/yếu như PPTP cho nhu cầu bảo mật hiện đại.
- VPN gateway là điểm truy cập nhạy cảm: cần cập nhật bản vá, MFA, certificate/khóa mạnh, giới hạn quyền, logging và thu hồi credential khi cần.
- Hai đầu mạng site-to-site không nên dùng các dải IP bị trùng nhau; ví dụ cả hai bên cùng dùng `192.168.1.0/24` sẽ gây xung đột định tuyến.

## Network Edge

- **Internet Edge (End system/ Hosts/ End nodes):** là các thiết bị cuối, sử dụng để khởi tạo và gửi request và là nơi cuối cùng nhận dữ liệu (máy tính, điện thoại, máy in, VoIP phone, credit card, barcode scanner, web server, ô tô kết nối mạng, ...).

- Điểm chung là chúng đều chạy các ứng dụng mạng, nằm ở phần rìa của mạng, không phải thành phần trung gian nào nên được gọi là thiết bị cuối.

- Access network là nơi mà giúp kết nối các End system với thành phần Network Core

![Access network](assets/image42.png)

*Access network*

- **Ví dụ Access network dựa vào Mobile network theo hình:** các máy tính, điện thoại, ô tô kết nối internet tới trạm sóng cố định qua sóng do nó có thể di chuyển theo thời gian + trạm này lại nối tới router đầu tiên qua dây ⇒ lúc này Trạm sóng chính là Access network.

## Network Core

![Network Core và các access network](assets/image55.png)

*Network Core và các access network*

**Network Core (lõi mạng)** là tập hợp các router, switch tốc độ cao, đường truyền và hệ thống điều khiển nằm giữa các access network. Lõi mạng vận chuyển packet qua nhiều mạng trung gian để kết nối các end system ở những vị trí khác nhau.

“Core” là một khái niệm kiến trúc, không phải một mạng trung tâm duy nhất do một tổ chức sở hữu. Internet không có một router trung tâm hay một “Global ISP” duy nhất; nó được hình thành từ hàng chục nghìn mạng độc lập kết nối với nhau.

![Các router trong Network Core](assets/image14.png)

*Các router trong Network Core*

### Thành phần và chức năng

| Thành phần/khái niệm | Vai trò |
|---|---|
| **Core router** | Chuyển tiếp packet giữa các đường truyền tốc độ cao dựa trên forwarding table |
| **Backbone link** | Đường trục dung lượng lớn, thường dùng cáp quang mặt đất hoặc cáp quang biển |
| **PoP (Point of Presence)** | Địa điểm nhà mạng đặt router và thiết bị để khách hàng hoặc mạng khác kết nối vào |
| **Autonomous System (AS)** | Một tập các mạng IP và router do một tổ chức quản lý theo chính sách định tuyến chung; mỗi AS public có ASN |
| **Routing protocol** | Trao đổi thông tin đường đi: OSPF/IS-IS thường dùng bên trong một AS; BGP dùng giữa các AS và cũng có thể dùng trong nội bộ |
| **Forwarding plane** | Tra bảng và chuyển từng packet từ cổng vào sang cổng ra với tốc độ cao |
| **Control plane** | Học/tính toán đường đi, xây dựng thông tin để cài vào forwarding table |

Các nhiệm vụ chính của network core:

- Vận chuyển lượng lớn packet giữa các access network.
- Chọn **đường phù hợp theo metric và policy**, không nhất thiết là đường ngắn nhất về địa lý hoặc có độ trễ thấp nhất.
- Cung cấp nhiều đường đi dự phòng để có thể hội tụ lại khi link/router gặp lỗi.
- Quản lý lưu lượng bằng queue, scheduling, traffic engineering và QoS.
- Kết nối nhiều công nghệ mạng khác nhau; tại lõi nhà mạng có thể gặp IP/MPLS, Ethernet tốc độ cao và mạng truyền dẫn quang.

Network core giúp tăng độ tin cậy nhưng **không bảo đảm tuyệt đối** tránh nghẽn mạng, packet loss hoặc sự cố. Khi lưu lượng tới nhanh hơn khả năng xử lý/truyền đi, queue có thể tăng và packet vẫn có thể bị loại bỏ.

### Vì sao các mạng không kết nối trực tiếp với tất cả mạng khác?

Nếu `N` mạng đều cần một kết nối vật lý riêng tới mọi mạng còn lại, số liên kết cực đại là:

`N × (N - 1) / 2`

Mô hình full-mesh này tăng gần theo `N²`, tốn cổng, đường truyền, chi phí và khó quản trị. Internet mở rộng bằng cách tổ chức các mạng thành AS, dùng kết nối nhà cung cấp/khách hàng và peering thay vì bắt mọi mạng nối trực tiếp với nhau.

![Kết nối thông qua Global ISP](assets/image7.png)

*Mô hình đơn giản hóa: access network kết nối qua một ISP. Trong thực tế có nhiều ISP và nhiều tầng kết nối.*

### ISP, transit, peering và IXP

| Quan hệ | Cách hoạt động | Chi phí/đặc điểm |
|---|---|---|
| **Transit** | Mạng khách hàng trả tiền cho provider để provider chuyển lưu lượng tới phần còn lại của Internet | Provider quảng bá route của khách hàng và cung cấp khả năng tiếp cận rộng |
| **Private peering (PNI)** | Hai mạng kết nối trực tiếp để trao đổi lưu lượng của chính họ và khách hàng của họ theo thỏa thuận | Phù hợp khi lưu lượng giữa hai bên đủ lớn; có thể miễn phí hoặc có phí |
| **Public peering tại IXP** | Nhiều mạng cắm vào hạ tầng switching chung của IXP và tự thiết lập phiên BGP với nhau hoặc qua route server | Giảm số đường nối vật lý riêng và có thể giảm chi phí/độ trễ |

![Các ISP và Internet Exchange Point](assets/image20.png)

*Các mạng có thể mua transit hoặc peering tại IXP/private interconnect.*

![Mô hình phân cấp ISP](assets/image21.png)

*Mô hình phân cấp ISP mang tính khái quát; Internet thực tế có nhiều kết nối chéo và không phải một cây phân cấp cứng.*

**IXP (Internet Exchange Point)** là địa điểm và hạ tầng Layer 2 cho phép nhiều AS trao đổi traffic. IXP:

- Không phải “Internet trung tâm” và không bắt buộc mọi ISP phải đi qua.
- Thường không tự quyết định route cho các thành viên; từng mạng vẫn dùng BGP và chính sách riêng.
- Không đồng nghĩa với transit provider. Thành viên thường trả phí cổng/thành viên, còn việc peering phụ thuộc thỏa thuận giữa các mạng.
- Có thể rút ngắn đường đi, giảm chi phí transit và giữ traffic nội vùng, nhưng không phải lúc nào cũng là đường tốt nhất.

Các mạng lớn như ISP, cloud provider và content provider còn có thể đặt cache/CDN gần người dùng hoặc kết nối private peering để tránh đưa mọi traffic qua transit.

### Routing và forwarding

| Khái niệm | Câu hỏi cần trả lời | Phạm vi/thời gian |
|---|---|---|
| **Routing** | Có những đường nào tới prefix đích và nên chọn đường nào theo metric/policy? | Control plane; diễn ra trên nhiều router và cập nhật khi topology/policy đổi |
| **Forwarding** | Packet vừa tới phải đi ra interface/next hop nào? | Data plane; thực hiện cục bộ cho từng packet |

Router thường không lưu “toàn bộ hành trình cố định” trong mỗi packet. Nó tra IP đích theo quy tắc **longest prefix match**, chọn next hop/interface rồi chuyển packet tới chặng tiếp theo. Mỗi router lặp lại quá trình này.

### Circuit switching

**Circuit switching là chuyển mạch kênh**, không phải “chuyển mạch vòng”. Trước khi truyền dữ liệu, mạng thiết lập một circuit và dành trước tài nguyên trên các chặng, chẳng hạn time slot trong TDM hoặc dải tần trong FDM.

| Đặc điểm | Ý nghĩa |
|---|---|
| **Dành trước tài nguyên** | Băng thông của circuit có tính dự đoán, ít phải cạnh tranh queue với kết nối khác |
| **Có giai đoạn thiết lập** | Phải tạo circuit trước khi truyền và giải phóng sau khi kết thúc |
| **Đường logic tương đối ổn định** | Dữ liệu của phiên đi theo circuit đã thiết lập cho tới khi circuit đổi hoặc bị lỗi |
| **Hiệu quả với traffic liên tục** | Phù hợp thoại truyền thống; kém hiệu quả khi nguồn gửi dữ liệu theo từng đợt vì tài nguyên vẫn bị giữ lúc rảnh |

Circuit switching không đồng nghĩa với “không có delay”: vẫn có propagation delay, transmission delay, processing delay và thời gian thiết lập circuit. Ưu điểm chính là delay/jitter dễ dự đoán hơn sau khi tài nguyên đã được dành trước.

Nói “Internet không sử dụng circuit switching” là quá tuyệt đối. IP của người dùng chủ yếu dùng packet switching, nhưng hạ tầng truyền dẫn bên dưới có thể dùng circuit hoặc lightpath được cấp phát, ví dụ mạng quang/OTN. MPLS tạo đường chuyển tiếp logic nhưng vẫn là công nghệ packet switching, không phải circuit switching cổ điển.

### Packet switching

**Packet switching (chuyển mạch gói)** chia dữ liệu thành các đơn vị có header và payload rồi ghép xen traffic của nhiều luồng trên cùng đường truyền. Tài nguyên thường không được dành riêng cho từng phiên, gọi là **statistical multiplexing**.

| Ưu điểm | Hạn chế |
|---|---|
| Tận dụng đường truyền tốt với traffic gửi theo từng đợt | Queue làm delay và jitter thay đổi |
| Nhiều người dùng chia sẻ cùng hạ tầng | Có thể packet loss khi buffer đầy |
| Linh hoạt khi topology/route thay đổi | Header và xử lý mỗi packet tạo overhead |
| Có thể định tuyến lại khi một đường gặp lỗi | Không mặc định bảo đảm băng thông hoặc thời gian giao hàng |

#### Những điểm cần hiểu chính xác

- Packet của cùng một luồng **có thể**, nhưng không bắt buộc, đi qua các đường khác nhau. Route change hoặc load balancing có thể làm thay đổi đường đi.
- IP cung cấp dịch vụ best-effort: packet có thể đến trễ, mất, lặp hoặc sai thứ tự.
- IP không tự ghép lại toàn bộ file theo thứ tự. TCP dùng sequence number, ACK và retransmission để cung cấp byte stream tin cậy; UDP không cung cấp các bảo đảm đó. Ứng dụng hoặc giao thức tầng trên phải xử lý nếu cần.
- Không phải mọi packet đều có “số thứ tự”. Trường header phụ thuộc giao thức; IPv4 có checksum cho **header**, còn TCP/UDP có checksum bao phủ transport data theo quy tắc của chúng.
- Router thường chỉ kiểm tra/chuyển tiếp packet và không biết nó thuộc file, video hay email nào.
- Có thể nói **Internet ở tầng IP dựa trên packet switching**, nhưng “100% Internet chỉ dùng packet switching” bỏ qua các công nghệ circuit/quang ở hạ tầng bên dưới.

### So sánh circuit switching và packet switching

| Tiêu chí | Circuit switching | Packet switching |
|---|---|---|
| Cấp phát tài nguyên | Dành trước cho circuit | Chia sẻ động giữa nhiều luồng |
| Thiết lập trước khi gửi | Thường có | IP datagram không cần thiết lập circuit; giao thức khác có thể vẫn cần handshake |
| Băng thông/delay | Dễ dự đoán hơn | Thay đổi theo tải mạng |
| Hiệu quả với traffic burst | Thấp hơn nếu circuit nhàn rỗi | Cao hơn nhờ statistical multiplexing |
| Khi nghẽn | Có thể từ chối thiết lập circuit mới | Queue tăng, delay tăng và có thể loss |
| Ví dụ | Mạng điện thoại PSTN truyền thống, circuit TDM | IP/Ethernet và phần lớn traffic Internet |

## Repeater, Hub, Bridge, Switch, Router

### Repeater

- Repeater là 1 thiết bị mạng hoạt động ở Physical layer (bởi đơn giản nó chỉ nhận tín hiệu vật lý rồi khuếch đại, phát lại mà không quan tâm dữ liệu chứa gì) trong mô hình OSI, nhiệm vụ chính của nó là tiếp nhận tín hiệu vật lý (điện, sóng vô tuyến, quang) từ 1 thiết bị khác, khuếch đại tín hiệu để khắc phục sự suy hao do khoảng cách, tái tạo tín hiệu rồi truyền tín hiệu đi xa.

- Nếu như không có repeater, tín hiệu có thể yếu đi dẫn tới device còn không biết packet được gửi tới vì nó cực kỳ yếu/ device nhận biết được packet nhưng không thể giải mã chính xác dẫn tới CRC không khớp ⇒ kết quả cuối cùng là packet bị mất, yêu cầu gửi lại packet mới.

- Bộ khuếch đại đơn thuần (Amplifier) chỉ tăng cường mọi thứ, cả tín hiệu nhiễu lẫn gốc đều được khuếch đại tức là sẽ thu được tín hiệu khuếch đại nhưng vẫn sẽ bị nhiễu >< Repeater không chỉ khuếch đại tín hiệu, mà còn tái tạo tín hiệu: decode tín hiệu yếu ớt thành các bit dữ liệu → tạo ra bản sao mới, sạch sẽ, mạnh mẽ từ tín hiệu gốc này → phát đi dữ liệu mới

- Các router (wifi), switch hầu như đều tích hợp Repeater, nếu không thì cần phải đầu tư 1 thiết bị Repeater riêng + trong hạ tầng ISP, dọc theo đường trục cáp quang thì cứ khoảng 80-100km lại có bộ khuếch quang để bù đắp sự suy hao của tín hiệu ánh sáng trong sợi quang để đảm bảo đi hàng ngàn km dữ liệu không bị lỗi.

- **Cái kích wifi hàng xóm chính là Repeater:** đặt Repeater tại nơi mà vẫn còn bắt được sóng wifi từ router chính (bản thân sẽ kết nối không dây với router chính) sau đó phát lại sóng wifi đó → sóng wifi sẽ được đẩy đi xa hơn tới các khu vực khác >< nhược điểm là chậm hơn, do phải nghe tín hiệu từ wifi chính rồi nói với device của mình (thường là chỉ đạt 50% về tốc độ). Lý do là đây là các repeater giá rẻ, chỉ có 1 bộ thu-phát radio, không thể vừa nghe, nói cùng lúc >< Repeater ISP thì khác biệt, gần như không hề có độ trễ nào.

- **Nhiễu:** tín hiệu không mong muốn trộn lẫn vào tín hiệu gốc. Nguyên nhân có thể là do nhiệt (gây nhiễu nhiệt), từ trường, xung đột tín hiệu từ các device khác, bức xạ từ mặt trời, sóng vô tuyến, nhiễu xuyên âm giữa các cặp dây trong cùng bó cáp → gây tiếng rè rè khi nghe radio, nghe điện thoại (bản chất là do biến dạng tín hiệu gốc, các bit 1 có thể hiểu nhầm thành bit 0 và ngược lại)

- **Suy hao/ mất:** năng lượng của tín hiệu gốc bị giảm dần trên đường truyền. Nguyên nhân có thể là do: điện trở của vật làm cáp, khoảng cách xa,... → làm giảm biên độ của tín hiệu gốc, tín hiệu trở nên nhỏ hơn, yếu hơn

### Hub

- **Hub là 1 thiết bị mạng hoạt động ở Physical layer của mô hình OSI:** khi tín hiệu đi vào 1 cổng của Hub, nó sẽ sao chép tín hiệu rồi phát tín hiệu đó tới toàn bộ các cổng còn lại

![Hub và miền truyền tín hiệu](assets/image1.png)

*Hub và miền truyền tín hiệu*

![Hub và miền truyền tín hiệu](assets/image85.png)

*Hub và miền truyền tín hiệu*

- 1 Hub thường có từ 4, 8, 12, 24 cổng, mỗi cổng chỉ có thể được kết nối tới 1 thiết bị (khi máy 1 đưa dữ liệu vào cổng 1 của Hub, nó sẽ sao chép nguyên tín hiệu và phát ra các cổng khác trong mạng ngoại trừ cổng nhận tín hiệu → các thiết bị kết nối đến Hub sẽ nhận được cùng tín hiệu. Tuy nhiên khi tới các thiết bị, thiết bị sẽ kiểm tra địa chỉ MAC trong tín hiệu có khớp không, nếu không thì bỏ tín hiệu đi → chỉ có đúng máy đích xử lý tín hiệu)

- Hub chỉ nhận tín hiệu rồi phát đi, không kiểm tra dữ liệu

- Băng thông chia sẻ cho tất cả thiết bị kết nối vào Hub + dù các thiết bị không phải thiết bị đích nhưng vẫn nhận được tín hiệu → hiệu suất sẽ thấp, lãng phí băng thông, thậm chí còn gây ra các vấn đề bảo mật khi sniff

- **Collision domain:** do Hub sử dụng Half-duplex, không thể vừa nhận, vừa gửi tín hiệu cùng lúc: (do dùng chung đường dây cáp) khi 2 thiết bị khác nhau gửi tín hiệu cùng lúc sẽ va chạm vào nhau và hủy lẫn nhau (do tín hiệu va chạm với nhau trên cùng phương tiện truyền dẫn) >< Hiện đại đều sử dụng Full-duplex, cơ chế queue của router khiến các packet đi tuần tự mà không lo collision.

### Bridge

- **Bridge:** là 1 thiết bị mạng hoạt động ở Link layer trong mô hình OSI, mục đích là kết nối nhiều mạng LAN lại với nhau để trở thành mạng LAN lớn hơn, quản lý lưu lượng truy cập dựa trên địa chỉ MAC để quyết định xem nên loại bỏ hay chuyển tiếp tín hiệu đi

![Bridge trong mạng](assets/image40.png)

*Bridge trong mạng*

- **Learning:** khi 1 frame đi vào bridge, bridge sẽ xem MAC address nguồn, port bridge vừa nhận frame đó → học được là thiết bị nào đang kết nối vào port nào của bridge.

- **Filtering:** khi 1 frame đi vào, bridge cũng xem MAC address đích để xem frame này đi đến thiết bị có nằm trong cùng mạng LAN của thiết bị gửi hay không. Nếu nằm trên cùng LAN, bridge sẽ bỏ qua frame đó mà không chuyển đi.

- **Forwarding:** chuyển frame từ LAN này qua LAN khác nếu MAC address đích nằm khác LAN với thiết bị gửi.

![Hình 18](assets/image16.png)

*Hình 18*

![Hình 19](assets/image30.png)

*Hình 19*

![Hình 20](assets/image79.png)

*Hình 20*

![Hình 21](assets/image51.png)

*Hình 21*

![Hình 22](assets/image9.png)

*Hình 22*

![Hình 23](assets/image76.png)

*Hình 23*

### Switch

- **Switch:** là 1 thiết bị mạng hoạt động ở Link layer của mô hình OSI, có thể coi như là 1 Bridge đa cổng với hiệu suất cao hơn, nhiều tính năng hơn

![Switch trong mạng LAN](assets/image27.png)

*Switch trong mạng LAN*

- Nhiệm vụ chính là kết nối các thiết bị trong cùng mạng LAN + sử dụng MAC address để forward dữ liệu thông minh hơn Hub, Bridge để đến đúng thiết bị (mỗi MAC address mapping tới 1 port kết nối tới thiết bị cụ thể trong mạng)

- **Aging:** MAC table không keep MAC address mãi mãi, nếu không gửi tới 1 thiết bị trong 1 khoảng thời gian thì xóa MAC đi.

- **Full-duplex:** sử dụng cáp xoắn đôi, 1 dây chuyên nhận tín hiệu đến, 1 dây chuyên nhận tín hiệu đi nên không xảy ra hiện tượng collision.

- Trong khi 1 port của bridge kết nối tới segment (1 tập các device) thì 1 port của Switch sẽ kết nối tới 1 device → khi sent tới 1 device thì không cần thiết phải sent tới toàn bộ device thuộc 1 port, mà chỉ cần sent tới chính device cần tín hiệu. Đây chính là ưu điểm lớn nhất của Switch so với Bridge.

- Ngày nay họ sử dụng Switch để thay hoàn toàn cho Hub, Bridge + nếu sử dụng mạng gia đình, Switch được tích hợp trong Wifi router.

- **Router:** là thiết bị mạng quan trọng bậc nhất, giúp kết nối các mạng khác lại với nhau hoạt động ở Network layer trong mô hình OSI.

### Router

![Router kết nối LAN với Internet](assets/image73.png)

*Router kết nối LAN với Internet*

![Routing và forwarding](assets/image36.png)

*Routing và forwarding*

- Switch kết nối các thiết bị trong cùng mạng >< Router kết nối các mạng khác nhau lại.

- Khi các packet được truyền đi từ máy, bản chất nó sẽ truyền đi qua các router cho tới khi tới đích + Network core gồm 1 tập các router kết nối với nhau

- Nhiệm vụ chính là định tuyến packet giữa các mạng khác nhau dựa trên IP đích: Router nhận packet từ thiết bị trong mạng/ nhận từ 1 mạng khác → đọc IP address đích → tra cứu Routing table để xác định con đường tốt nhất để chuyển packet tới đích → forward packet theo con đường đã xác định (nếu đích thuộc mạng khác thì phải gửi packet đó tới router tiếp theo).

- **Wifi gia đình cũng là 1 router:** kết nối các thiết bị trong nhà tạo ra mạng LAN, gán IP cho từng thiết bị trong mạng qua DHCP + là cánh cổng duy nhất để tất cả thiết bị trong mạng có thể ra ngoài internet + có các tính năng bảo mật cơ bản để bảo vệ mạng nội bộ khỏi sự truy cập trái phép từ bên ngoài.

### Forwarding, store-and-forward và queue

- **Forwarding (switching) là chức năng thực thi nội bộ của 1 Router:** mỗi 1 Router sẽ có 1 bảng là Local Forwarding Table lưu trữ thông tin mapping giữa header value và output value: giúp dựa vào header packet gửi tới Router, sẽ xác định xem Router nào sẽ nhận được packet này tiếp theo (dựa vào IP đích (VD: IP đích là 10.1.1.5 tới 10.1.1.200 thì sẽ gửi tới Router A chẳng hạn) chứ không hề lưu hết quãng đường tới đích (rất tốn chi phí lưu trữ))

- **Store-and-forward:** khi router nhận được 1 packet: toàn bộ packet được tải vào bộ nhớ đệm buffer (store) -> tính toán checksum để phát hiện packet bị hỏng -> nếu packet hợp lệ, router tra local forwarding table rồi gửi packet đi (forward) ⇒ Thực sự hiệu quả, nếu 1 packet bị lỗi sẽ yêu cầu gửi lại ngay thay vì gửi tới tận nơi xử lý rồi mới nhận ra là packet lỗi (nhanh hơn do router phát hiện lỗi gần với nơi gửi request hơn + giảm nghẽn mạng do bị ảnh hưởng bởi các packet lỗi) >< việc này lại gây delay, không phù hợp với các ứng dụng real-time + router phải lưu tạm thời chúng trên buffer gây tốn tài nguyên (nên không phải cứ tách càng nhiều packet thì performance càng tốt, mỗi packet phải chịu lượng delay này)

![Queue tại router](assets/image52.png)

*Queue tại router*

- **Packet-switching queue:** như đã học ở trên, khi packet tới được router sẽ cần trải qua store-and-forward. Tuy nhiên vấn đề là tốc độ gửi packet tới router này lại > tốc độ gửi ra (do là router có thể nhận được các packet này từ nhiều router khác, nhưng lại đổ dữ liệu lên vài router khác dẫn tới chậm do tổng lưu lượng cùng vào 1 router chậm). Router có Input Queue (packet mới tới router, chưa xử lý gì) + Output Queue (packet đã được xử lý, đợi truyền đi do tốc độ xử lý bị giới hạn) ⇒ Quy trình thực sự là: Router nhận packet từ cổng vào -> lưu lại -> tra bảng định tuyến để xác định router đích -> đưa gói tin vào hàng đợi router đích -> đưa gói tin tới router đích theo thứ tự hàng đợi.

- **Packet loss:** xảy ra khi packet chưa được gửi đi mà đã bị loại bỏ khỏi Router dù bản thân packet không bị lỗi do: queue đầy cần loại bỏ bớt để xử lý/ lỗi phần cứng của router,... (khi buffer đầy, packet nào tới thì sẽ bị loss)

- **Port vật lý:** là những cổng có thể chạm, nhìn thấy ở phía sau router

- **WAN port (Internet port) :** thường là 1 cổng, dùng để kết nối router tới modem/ internet mà ISP cung cấp

- **LAN port:** thường có 2-4 cổng dùng để kết nối có dây tới các thiết bị trong mạng nội bộ

- **Port logic:** là số từ 0 - 65535 hoạt động như cổng dịch vụ để xác định xem khi dữ liệu đến router sẽ dành cho thiết bị nào trong mạng, cho process nào trong thiết bị đó.

- Khi ta request tới 1 website, nhưng khi nhận phản hồi với header là Public IP của router thì ta không chỉ dựa vào đó mà có thể biết được rằng thiết bị nào, chương trình nào trong mạng nội bộ cần nhận phản hồi đó → sử dụng NAT table

- NAT table hoạt động như nhật ký kết nối, ghi lại toàn bộ kết nối từ mạng nội bộ ra ngoài internet, đặc biệt là ánh xạ IP:port nội bộ và IP:port công cộng mà router sử dụng

![Hình 28](assets/image19.png)

*Hình 28*

![Hình 29](assets/image61.png)

*Hình 29*

![Hình 30](assets/image33.png)

*Hình 30*

![Hình 31](assets/image38.png)

*Hình 31*

### Network Firewall

**Firewall (tường lửa)** là điểm thực thi chính sách bảo mật mạng: quan sát traffic đi qua nó rồi cho phép, từ chối, loại bỏ, ghi log hoặc xử lý theo rule đã cấu hình.

Firewall chỉ kiểm soát được traffic **đi qua đường xử lý của nó**. Nếu hai máy trong cùng VLAN giao tiếp trực tiếp qua switch, firewall đặt ở cổng Internet sẽ không nhìn thấy traffic đó. Muốn kiểm soát traffic nội bộ cần phân đoạn mạng và buộc traffic giữa các zone/VLAN đi qua firewall hoặc cơ chế lọc tương đương.

#### Cách firewall xử lý một kết nối

Giả sử firewall có rule:

| Source | Destination | Protocol/port | Action |
|---|---|---|---|
| Mạng nhân viên `10.0.10.0/24` | Web server `10.0.20.10` | TCP `443` | Allow |
| Mạng nhân viên `10.0.10.0/24` | Database `10.0.30.10` | TCP `5432` | Deny |

Máy nhân viên `10.0.10.25` thực hiện hai kết nối:

1. Kết nối tới `10.0.20.10:443`: firewall thấy nguồn, đích và port khớp rule Allow nên cho packet đi qua.
2. Kết nối tới `10.0.30.10:5432`: firewall thấy kết nối khớp rule Deny nên loại packet.

Firewall không tự biết kết nối nào là hợp lệ về mặt nghiệp vụ. Người quản trị phải khai báo rule dựa trên yêu cầu thực tế.

#### Ví dụ stateful firewall

Máy `10.0.10.25:53000` truy cập website `203.0.113.20:443`:

1. Packet đầu tiên đi từ máy nội bộ ra Internet.
2. Firewall kiểm tra rule outbound. Nếu được phép, nó lưu trạng thái kết nối.
3. Website phản hồi từ `203.0.113.20:443` về `10.0.10.25:53000`.
4. Firewall nhận ra đây là packet phản hồi của kết nối đã được cho phép nên cho quay vào.
5. Nếu một máy Internet tự gửi packet mới tới `10.0.10.25:53000` mà không có trạng thái tương ứng, firewall chặn packet đó.

Vì vậy, việc cho phép máy nội bộ mở kết nối ra ngoài không đồng nghĩa với việc mọi máy bên ngoài được tự do mở kết nối vào trong.

#### Ví dụ firewall không nhìn thấy traffic

Máy A và máy B đều nằm trong VLAN 10 và kết nối vào cùng switch:

`Máy A → switch → Máy B`

Nếu firewall chỉ đặt giữa switch và Internet, traffic A–B không đi qua firewall nên rule trên firewall không áp dụng được. Muốn kiểm soát A–B cần một trong các cách:

- Tách A và B sang các VLAN/security zone khác nhau rồi route qua firewall.
- Dùng host firewall trên A/B.
- Dùng cơ chế microsegmentation hoặc policy trên switch/hypervisor.

![Network Firewall](assets/image80.png)

*Network Firewall*

#### Firewall có thể đặt ở đâu?

| Vị trí/loại | Phạm vi bảo vệ | Ví dụ |
|---|---|---|
| **Perimeter/network firewall** | Biên giữa mạng nội bộ và Internet hoặc mạng đối tác | Firewall appliance, virtual firewall, router có ACL/stateful firewall |
| **Internal segmentation firewall** | Traffic giữa user, server, production, database, OT hoặc các VLAN | Firewall giữa các security zone |
| **Host-based firewall** | Một máy cụ thể, kể cả traffic từ máy khác trong cùng LAN | Windows Defender Firewall, Linux nftables/iptables, macOS Application Firewall/PF |
| **Cloud firewall** | Tài nguyên trong VPC/VNet và traffic Internet/cloud-to-cloud | Security group, network ACL, managed cloud firewall |
| **Container/Kubernetes policy** | Traffic giữa workload/pod/service | Kubernetes NetworkPolicy, CNI policy |
| **Distributed/microsegmentation firewall** | Chính sách gần từng workload/VM thay vì chỉ tại biên | Hypervisor firewall, endpoint agent |

Router gia đình thường tích hợp stateful firewall, NAT, DHCP, Wi-Fi access point và switch. Tuy nhiên NAT và firewall là hai chức năng khác nhau: NAT dịch địa chỉ; firewall quyết định traffic có được phép hay không.

#### Firewall kiểm tra những gì?

Khả năng kiểm tra phụ thuộc loại firewall:

| Mức kiểm tra | Thuộc tính có thể dùng |
|---|---|
| **Layer 3** | Source/destination IP, subnet, interface, zone, IP protocol |
| **Layer 4** | TCP/UDP source và destination port, TCP flags, ICMP type/code |
| **State/session** | Kết nối mới hay phản hồi của kết nối đã cho phép, trạng thái TCP, timeout |
| **Identity/device** | User, group, máy được quản lý, certificate hoặc posture của endpoint |
| **Layer 7/application** | HTTP method/host, DNS query, TLS metadata, loại ứng dụng hoặc chữ ký giao thức |
| **Threat/content** | IPS signature, malware/file type, URL category, reputation/IP intelligence |
| **Time/context** | Thời gian, vị trí, tenant, tag cloud hoặc nhãn workload |

Firewall truyền thống thường tập trung Layer 3/4. **NGFW (Next-Generation Firewall)** bổ sung nhận diện ứng dụng, user, IDS/IPS, URL filtering, threat intelligence và đôi khi TLS inspection.

#### Stateless và stateful firewall

| Loại | Cách hoạt động | Ưu/nhược điểm |
|---|---|---|
| **Stateless packet filter/ACL** | Đánh giá từng packet độc lập theo rule | Nhanh và đơn giản, nhưng phải viết rule cho cả hai chiều và không hiểu trạng thái phiên |
| **Stateful firewall** | Lưu connection state và nhận biết packet thuộc phiên nào | Cho phép traffic phản hồi chính xác hơn; tốn state/memory và có thể bị ảnh hưởng bởi asymmetric routing |
| **Proxy/application firewall** | Kết thúc kết nối phía client rồi tạo kết nối mới tới server | Hiểu giao thức sâu và cô lập hai phía tốt hơn, nhưng phức tạp và tốn tài nguyên hơn |

Ví dụ với stateful firewall:

1. Client nội bộ `10.0.10.25:53000` mở TCP tới web server `203.0.113.20:443`.
2. Rule cho phép kết nối HTTPS outbound và firewall tạo connection-tracking entry.
3. Response từ `203.0.113.20:443` về `10.0.10.25:53000` được nhận diện là traffic `ESTABLISHED` và được phép quay lại.
4. Một packet từ Internet tự ý gửi tới `10.0.10.25:53000` nhưng không khớp state hợp lệ sẽ bị chặn.
5. State bị xóa khi phiên kết thúc hoặc hết timeout.

Stateful không có nghĩa firewall hiểu đầy đủ logic ứng dụng. Một kết nối TCP hợp lệ vẫn có thể mang SQL injection, malware hoặc dữ liệu bị đánh cắp nếu không có lớp kiểm tra/phòng vệ phù hợp.

#### Connection tracking và trạng thái

Firewall thường nhận diện flow bằng các trường như:

`source IP + destination IP + source port + destination port + protocol`

Một số trạng thái khái niệm thường gặp:

| Trạng thái | Ý nghĩa |
|---|---|
| **NEW** | Packet bắt đầu một flow mới hoặc chưa có state khớp |
| **ESTABLISHED** | Packet thuộc flow đã được theo dõi |
| **RELATED** | Flow mới có liên hệ với flow đã biết, tùy protocol/helper |
| **INVALID** | Packet không thể gắn vào trạng thái hợp lệ |

Tên và cách xử lý cụ thể phụ thuộc sản phẩm. UDP không có handshake như TCP nhưng firewall vẫn tạo state giả theo flow và timeout. ICMP cũng cần được xử lý có chọn lọc; chặn toàn bộ ICMP có thể làm hỏng chẩn đoán mạng và Path MTU Discovery.

#### Security zone và network segmentation

Thay vì chỉ nghĩ “bên trong đáng tin, Internet không đáng tin”, nên chia mạng thành các **security zone**:

| Zone ví dụ | Tài nguyên | Chính sách gợi ý |
|---|---|---|
| **Internet/Untrusted** | Mạng công cộng | Mặc định không được khởi tạo kết nối vào nội bộ |
| **User** | Laptop, desktop nhân viên | Chỉ truy cập dịch vụ cần thiết; hạn chế kết nối ngang |
| **Server/Application** | API, application server | Chỉ nhận từ reverse proxy/user/service được phép |
| **Database** | Database, cache nhạy cảm | Chỉ nhận từ application server trên đúng port |
| **DMZ** | Reverse proxy, VPN gateway, mail gateway công khai | Tách khỏi LAN; nếu bị xâm nhập vẫn hạn chế đường vào hệ thống bên trong |
| **Management** | SSH/RDP, hypervisor, network management | Chỉ cho admin qua bastion/VPN và MFA |
| **Guest/IoT** | Khách, camera, thiết bị ít tin cậy | Cho ra Internet có giới hạn; chặn vào mạng nội bộ |

Ví dụ luồng ba tầng:

`Internet → Reverse proxy:443 → Application:8080 → Database:5432`

Firewall không nên cho Internet truy cập thẳng database. Mỗi mũi tên là một rule tối thiểu theo nguồn, đích và dịch vụ cụ thể. Đây là **least privilege** và **defense in depth**.

Segmentation giúp hạn chế **lateral movement**: nếu một máy user bị chiếm quyền, kẻ tấn công không mặc nhiên quét hoặc truy cập mọi server trong tổ chức.

#### Inbound, outbound và east-west traffic

| Hướng | Ví dụ | Vì sao cần kiểm soát |
|---|---|---|
| **Inbound** | Internet truy cập web/VPN gateway | Giảm bề mặt tấn công; chỉ công khai dịch vụ cần thiết |
| **Outbound/egress** | Server hoặc user kết nối Internet | Hạn chế malware gọi C2, tải payload hoặc exfiltrate dữ liệu |
| **East-west/lateral** | User ↔ server, service ↔ database | Ngăn di chuyển ngang và cô lập sự cố |
| **North-south** | Traffic vào/ra data center hoặc cloud | Kiểm soát ranh giới giữa các môi trường |

Chỉ lọc inbound là chưa đủ. Egress filtering còn giúp ngăn IP spoofing, giới hạn DNS/SMTP trực tiếp, buộc traffic web qua proxy và phát hiện máy nội bộ kết nối tới đích bất thường.

#### Cách firewall xử lý rule

Nhiều firewall đánh giá rule từ trên xuống và dừng ở rule khớp đầu tiên, nhưng hành vi cụ thể tùy sản phẩm. Một rule thường gồm:

| Trường | Ví dụ |
|---|---|
| Source zone/address/identity | `User`, `10.0.10.0/24`, nhóm `Developers` |
| Destination zone/address | `Application`, `10.0.20.15` |
| Service/application | TCP `443`, HTTPS |
| Direction/interface | User → Application |
| Action | Allow, reject, drop, log |
| Schedule/profile | Giờ làm việc, IPS profile, URL category |
| Description/owner/expiry | Lý do nghiệp vụ, người chịu trách nhiệm, ngày rà soát |

Ví dụ policy:

| Thứ tự | Source | Destination | Service | Action | Mục đích |
|---:|---|---|---|---|---|
| 10 | Admin VPN | Management subnet | SSH, RDP | Allow + log | Quản trị qua VPN |
| 20 | Reverse proxy | Application server | TCP 8080 | Allow | Chuyển request ứng dụng |
| 30 | Application server | Database | TCP 5432 | Allow | Kết nối PostgreSQL |
| 40 | Guest | Internal networks | Any | Deny + log | Cô lập khách |
| 1000 | Any | Any | Any | Deny | Default deny |

Nguyên tắc quan trọng:

- Bắt đầu từ **default deny**, rồi chỉ allow nhu cầu đã xác định.
- Dùng subnet/host/service cụ thể; tránh `Any → Any → Allow`.
- Đặt rule cụ thể trước rule rộng nếu firewall dùng first-match.
- Không dựa vào source IP như bằng chứng danh tính duy nhất.
- Ghi mô tả, ticket/owner và thời hạn cho rule tạm thời.
- Loại bỏ rule trùng, shadowed, hết hạn hoặc không còn hit.
- Test cả chiều đi, chiều về, failover và các luồng quản trị khẩn cấp.

#### Allow, drop và reject

| Action | Hành vi | Khi nào phù hợp |
|---|---|---|
| **Allow/accept** | Cho traffic đi qua | Flow đã được phê duyệt |
| **Drop/deny silently** | Loại packet, không phản hồi | Giảm thông tin lộ ra cho nguồn không tin cậy; client phải chờ timeout |
| **Reject** | Loại packet và gửi TCP RST hoặc ICMP error phù hợp | Mạng nội bộ hoặc khi muốn client thất bại nhanh |
| **Log** | Ghi sự kiện, thường đi kèm allow/deny | Điều tra và giám sát; không nên log mù quáng mọi packet |

Drop không làm host trở nên “vô hình” tuyệt đối; timing, các dịch vụ khác và nhiều tín hiệu mạng vẫn có thể tiết lộ sự tồn tại.

#### Firewall, NAT, IDS/IPS, WAF và proxy khác nhau thế nào?

| Công nghệ | Mục tiêu chính | Không nên nhầm với |
|---|---|---|
| **Firewall** | Thực thi policy cho flow/traffic | Không tự động hiểu mọi lỗ hổng ứng dụng |
| **NAT/PAT** | Dịch IP/port | Không phải cơ chế bảo mật thay thế firewall |
| **IDS** | Phát hiện và cảnh báo traffic đáng ngờ | Thường không chặn trực tiếp |
| **IPS** | Phát hiện và chặn signature/hành vi mạng | Có nguy cơ false positive; cần tuning |
| **WAF** | Bảo vệ HTTP/HTTPS application khỏi một số tấn công web | Không bảo vệ mọi protocol hoặc sửa code lỗi |
| **Forward proxy/SWG** | Kiểm soát client đi ra web | Khác reverse proxy bảo vệ/publish server |
| **EDR/antimalware** | Giám sát hành vi trên endpoint | Firewall không thay thế bảo vệ endpoint |

Các chức năng có thể nằm chung trong một sản phẩm NGFW, nhưng về mặt khái niệm chúng vẫn giải quyết các bài toán khác nhau.

#### TLS/HTTPS ảnh hưởng thế nào?

Khi traffic dùng HTTPS, firewall thông thường vẫn thấy metadata như IP, port, lưu lượng và một phần thông tin bắt tay TLS, nhưng không đọc được HTTP payload đã mã hóa.

**TLS inspection/decryption** cho phép firewall giải mã, kiểm tra rồi mã hóa lại traffic. Đổi lại:

- Cần triển khai CA tin cậy trên thiết bị được quản lý.
- Tăng tải xử lý và có thể phá vỡ certificate pinning/mTLS.
- Tạo rủi ro riêng tư và pháp lý; không nên giải mã tùy tiện dữ liệu y tế, ngân hàng hoặc thiết bị cá nhân.
- Firewall trở thành điểm nắm plaintext và khóa rất nhạy cảm, cần bảo vệ nghiêm ngặt.

Không thể kết luận “dùng HTTPS thì firewall không kiểm soát được gì”, nhưng khả năng kiểm tra nội dung phụ thuộc kiến trúc và chính sách giải mã.

#### Firewall có thể và không thể bảo vệ điều gì?

| Nhận định | Đánh giá |
|---|---|
| Firewall chặn truy cập trái phép | **Đúng nếu** traffic đi qua firewall và rule được thiết kế đúng |
| Firewall ngăn mọi malware/hacker | **Sai:** malware có thể đi qua traffic được allow, HTTPS, email, USB hoặc credential hợp lệ |
| Firewall chặn website độc hại | **Chỉ khi** có DNS/URL/application filtering phù hợp; packet filter Layer 3/4 đơn thuần không biết đầy đủ URL |
| Firewall chặn DDoS | **Có giới hạn:** có thể rate-limit/drop một số traffic, nhưng nếu đường truyền đã bị bão hòa thì phải xử lý upstream qua ISP/CDN/scrubbing service |
| NAT bảo vệ giống firewall | **Sai:** NAT có thể làm inbound khó tiếp cận hơn nhưng không thay thế policy và stateful inspection |
| Có firewall thì không cần vá lỗi | **Sai:** dịch vụ được allow vẫn có thể bị khai thác; patching và hardening vẫn bắt buộc |
| Host firewall không cần nếu đã có perimeter firewall | **Sai:** host firewall bảo vệ khi roaming và trước lateral traffic trong cùng LAN |
| Chặn toàn bộ ICMP là an toàn | **Sai:** có thể gây lỗi PMTUD và làm chẩn đoán mạng khó khăn |

#### Logging, monitoring và vận hành

Firewall không chỉ là tập rule; vận hành mới quyết định nó có hữu ích lâu dài hay không:

- Đồng bộ thời gian bằng NTP để log có timestamp chính xác.
- Gửi log quan trọng tới hệ thống tập trung/SIEM, tránh chỉ lưu cục bộ.
- Theo dõi deny spike, scan, login/VPN bất thường, rule change và traffic tới đích hiếm gặp.
- Log session start/end hoặc deny có chọn lọc; log mọi packet có thể gây quá tải và che lấp tín hiệu.
- Bảo vệ log khỏi sửa/xóa và đặt retention theo yêu cầu điều tra/compliance.
- Sao lưu cấu hình, version control hoặc audit thay đổi, dùng tài khoản quản trị riêng và MFA.
- Kiểm thử định kỳ bằng flow log, packet capture, port scan có ủy quyền và diễn tập failover.
- Cập nhật firmware/signature; firewall chưa vá lỗi chính nó có thể trở thành cửa ngõ bị tấn công.

#### High availability và lỗi kiến trúc thường gặp

Firewall nằm trên đường truyền có thể trở thành bottleneck hoặc single point of failure. Hệ thống quan trọng thường dùng:

- Cặp firewall active/passive hoặc active/active.
- Đồng bộ connection state để failover ít làm rớt phiên.
- Redundant link, switch, nguồn điện và đường ISP.
- Capacity planning theo throughput thực khi bật IPS/TLS inspection, không chỉ theo con số forwarding tối đa.

Các lỗi phổ biến:

- Có đường mạng phụ bypass firewall.
- Asymmetric routing khiến hai chiều của một flow đi qua firewall stateful khác nhau.
- Rule quá rộng, rule tạm thời không bao giờ bị xóa.
- Management interface mở từ Internet hoặc dùng chung với user traffic.
- Chỉ bảo vệ biên mạng, không segment hệ thống nội bộ.
- Tin rằng traffic trong VPN mặc nhiên an toàn.
- Public dịch vụ quản trị như RDP/SSH trực tiếp thay vì qua VPN/bastion và MFA.

#### Checklist thiết kế firewall

1. Lập sơ đồ zone, subnet, tài sản và trust boundary.
2. Ghi rõ các flow nghiệp vụ cần thiết: ai kết nối tới đâu, bằng protocol/port nào.
3. Chọn default-deny giữa các zone và cho phép tối thiểu theo least privilege.
4. Tách management, user, server, database, guest/IoT và DMZ.
5. Kiểm soát cả inbound, outbound và east-west traffic.
6. Bật logging/alert có chọn lọc và gắn owner cho từng rule.
7. Dùng MFA, bastion/VPN và giới hạn nguồn cho đường quản trị.
8. Rà soát rule, firmware, certificate, backup và capacity định kỳ.
9. Kết hợp firewall với patching, IAM, EDR, IDS/IPS, WAF, backup và giám sát.
10. Kiểm thử chính sách từ góc nhìn attacker lẫn luồng nghiệp vụ hợp lệ.

## IP, ISP

### Địa chỉ IP

- **ISP (Internet service provider):** nhà cung cấp dịch vụ internet, là các công ty, tổ chức cung cấp quyền cho mọi người sử dụng internet

- **ISP kết nối tới thiết bị của mình:** bản thân ISP lắp đặt cáp quang, đường dây điện thoại, cáp đồng trục, thiết lập kết nối không dây,... đến tận nhà/ văn phòng của mình

- **ISP kết nối tới thế giới:** các ISP lớn kết nối lại với nhau qua IXP để giúp các ISP khác nhau có thể trao đổi dữ liệu.

- **Định tuyến lưu lượng:** khi gửi 1 request tới google.com, ISP của mình sẽ đóng vai trò làm trạm trung chuyển đầu tiên, packet đi qua các router của ISP mình → đi qua các ISP của mạng khác cho tới khi tới đích.

- **Kho cấp phát địa chỉ IP:** ISP sở hữu 1 lượng lớn địa chỉ Public IP, thường là các dải IP nhất định được quản lý bởi các tổ chức toàn cầu như IANA và các đăng ký khu vực (APNIC cho Châu Á) → Khi thông qua Public IP, có thể truy ngược lại để biết được IP này thuộc về ISP nào, địa chỉ, quốc gia nào,.. (VD: 1 Public IP là 123.24.*.* có thể xác định là thuộc về FPT Telecom, tại Hà Nội)

- Khi đăng ký dịch vụ internet, ISP sẽ cho thuê, cấp phát 1 Public IP tạm thời/ cố định từ kho lưu trữ của họ (Khi kết nối wifi của FPT cung cấp, router khi bật sẽ xin 1 Public IP từ FPT ISP)

- **Quản lý, theo dõi người dùng:** ISP cấp IP cho mình, nên họ có thể dễ dàng theo dõi các hoạt động trên internet của mình qua IP (nếu web không được mã hóa) để biết truy cập website nào, thời điểm nào để: điều tiết băng thông, khắc phục sự cố, chặn truy cập vào website bị cấm, đưa ra quảng cáo nhờ thói quen, sở thích của người dùng,...

- **Dịch vụ internet cho cá nhân, gia đình:**

- **Cáp quang (FTTH/FTTP):** sử dụng sợi quang học, cho tốc độ cao và ổn định

- **DSL (Digital subscriber line):** tận dụng đường dây điện thoại cũ, tốc độ thấp hơn cáp quang.

- **Cáp đồng trục:** sử dụng hệ thống cáp tivi, phổ biến ở một số nơi

- **Internet vệ tinh:** dành cho khu vực nông thôn, vùng sâu vùng xa không có hạ tầng cáp

### ISP

- **Internet di động (3G/ 4G/ 5G):** sử dụng sóng di động từ các nhà mạng như Viettel, Vinaphone,...

- Dịch vụ cho doanh nghiệp thì thường có tốc độ cao hơn, ổn định hơn, đi kèm với các dịch vụ như IP tĩnh, cam kết băng thông SLA

- **Tại Việt Nam, có nhiều ISP nổi bật như:** VNPT, FPT Telecom, Viettel Telecom,...

- **IP (Internet protocol):** là bộ quy tắc, một giao thức định tuyến giúp các packet đến đúng đích, cho phép các thiết bị giao tiếp với nhau qua internet/ mạng nội bộ

- **Định danh:** đóng vai trò như CCCD cho 1 thiết bị (private IP để phân biệt các thiết bị trên mạng nội bộ), router (public IP để phân biệt các mạng với nhau)

- **Định vị:** IP giúp xác định vị trí mạng của thiết bị trên internet + router có Routable giúp định tuyến tới router tiếp theo dựa trên IP đích.

- **Thiết lập kết nối:** địa chỉ IP là thứ đầu tiên cần có để bắt đầu cuộc trao đổi thông tin, cho dù là truy cập web, gửi mail hay xem video.

### Public IP và Private IP

- **IPv4:** có dạng xxx.xxx.xxx.xxx (4 nhóm số, mỗi nhóm từ 0-255). Do số lượng thiết bị internet bùng nổ, IPv4 sẽ bị cạn kiệt nếu mỗi thiết bị sở hữu 1 unique IPv4

- **IPv6:** được tạo ra để thay thế, có dạng hexadecimal dài hơn nhiều, cung cấp số lượng địa chỉ gần như vô hạn + về lý thuyết thì mỗi thiết bị đều có thể có Public IP unique dựa trên IPv6

- **Public IP:** IP công cộng, là IP duy nhất trên toàn internet, được nhà mạng ISP cung cấp cho router

- Mọi thiết bị ra ngoài internet đều được định danh bằng Public IP dựa vào NAT

- Public IP lộ thiên trên internet, có thể trở thành mục tiêu của các cuộc tấn công scan, dò quét, DDoS,... nếu không được bảo vệ bằng tường lửa

- Sự khan hiếm của IPv4 dẫn tới chỉ sử dụng Public IP trên router thay vì toàn bộ thiết bị trên internet

- **Dynamic IP:** ISP cho router mượn 1 Public IP trong pool của họ, IP này có thể thay đổi sau mỗi lần router khởi động lại/ sau 1 khoảng thời gian nhất định, đặc biệt phổ biến cho hộ gia đình/ cá nhân

- **Static IP:** IP cố định, không bao giờ thay đổi, phù hợp cho Web server, chi phí cao hơn

- **Private IP:** IP riêng tư, là địa chỉ được router cấp phát cho các thiết bị trong mạng nội bộ

- Các địa chỉ này không được sử dụng để truy cập trực tiếp từ internet, mà chỉ sử dụng nội bộ trong cùng mạng để các thiết bị trong mạng giao tiếp với nhau

- Các mạng khác nhau thì các thiết bị có thể có Private IP trùng nhau → tiết kiệm được IPv4.

- Các Private IP được ẩn mình sau router, bên ngoài internet không thể nhìn thấy/ kết nối trực tiếp tới từng thiết bị có Private IP (trừ khi được config NAT Port Forwarding)

### Các dải địa chỉ IPv4

- **Các dải Private IP phổ biến như:**

![Các dải địa chỉ IPv4](assets/image28.png)

*Các dải địa chỉ IPv4*

### NAT (Network Address Translation)

- **NAT (Network Address Translation):** là cơ chế được tích hợp trong router, chuyển đổi Private IP sang Public IP và ngược lại, sử dụng để chuyển lại phản hồi đến đúng thiết bị gửi request.

- NAT bản chất sinh ra để chữa cháy IPv4 cạn kiệt dần

- **Port Forwarding:** để chạy dịch vụ ra ngoài internet như cung cấp trang web chẳng hạn, cần Port Forwarding trên router: hễ có ai gõ vào cổng 80 của Public IP, hãy chuyển nó tới cổng 81 của web server

1. **B1:** Thiết bị nội bộ với Private IP 192.168.1.5 muốn truy cập google.com

1. **B2:** Thiết bị gửi gói tin với thông tin Nguồn: 192.168.1.5:54321 ;Đích: googleIP:80

1. **B3:** Goi tin được gửi tới router + router thực hiện NAT:

- Thay đổi IP nguồn từ Private IP sang Public IP của router 123.456.789.100

- **NAT table:** ghi nhớ mapping này: 192.168.1.5:54321 đang được ánh xạ tới 123.456.789.100:62345

- **Packet bây giờ là:** Nguồn: 123.456.789.100:62345 ;Đích: googleIP:80

1. **B4:** Google nhận gói tin, xử lý và trả thông tin về 123.456.789.100:62345

1. **B5:** Router nhận gói tin, tra NAT table và nhận thấy cần gửi lại về thiết bị 192.168.1.5 + đổi IP đích thành Private IP: 192.168.1.5  + chuyển gói tin về đúng thiết bị nội bộ

### Default gateway

- **Default gateway:** là 1 thiết bị mạng mà thiết bị sử dụng dịch vụ mạng (điện thoại, máy tính) sẽ gửi tất cả các packet tới nếu như đích đến không nằm trong cùng internal network (LAN)

- Thông thường Default gateway chính là router

- Gõ ipconfig, tim dòng Default Gateway để biết được Private IP của router >< https://whatismyipaddress.com để biết được Public IP của router (vì = Public IP của device).

- **Như là 1 cửa ngõ để có thể ra ngoài mạng khác (VD:** gọi vào google, facebook,…), nếu không có Default gateway thì device chỉ có thể giao tiếp được trong cùng mạng. (tất cả các packet đều phải đi qua Default gateway nếu muốn giao tiếp khác mạng)

- Còn nếu như chỉ giao tiếp trong cùng mạng, không cần gửi packet tới Default gateway mà thay vào đó, chỉ cần gửi tới đúng máy đó thông qua Private IP/ tên service (service discovery) trong cùng mạng.

![Hình 34](assets/image8.png)

*Hình 34*

### Subnet, subnet mask và CIDR

- **Subnet (subnetwork):** tức mạng con từ 1 mạng IP lớn được chia thành nhiều mạng nhỏ hơn để quản lý hiệu quả

- **Giảm Broadcast traffic:** trong mạng lớn, các gói tin broadcast sẽ lan truyền khắp nơi trong mạng gây tắc nghẽn, giảm hiệu suất → Subset tạo biên giới để chặn các gói tin broadcast này lại, 1 gói tin trong Subnet A không thể lan sang Subnet B

- **Tăng bảo mật:** các máy chủ quan trọng vào subnet riêng biệt, các máy nhân viên vào subnet khác,... + bằng cách sử dụng các công cụ như Access Control Lists (ACLs) trên router/ firewall, ta có thể dễ dàng kiểm soát luồng dữ liệu giữa các Subnet + cũng dễ dàng theo dõi, khắc phục sự cố, phân quyền cho từng subnet.

- **Tối ưu hóa địa chỉ IP:** các lớp mạng Network Classes tiêu chuẩn (như class A, B, C) thường cung cấp lượng địa chỉ IP rất lớn, có thể vượt quá nhu cầu thực tế của mạng cục bộ → Subnet cho phép tận dụng dải địa chỉ IP một cách linh hoạt, hiệu quả hơn hơn, tránh lãng phí địa chỉ.

- **Subnet mask:** là 1 dãy số 32bit (đối với IPv4) sử dụng để xác định phần network, phần host trong 1 địa chỉ IP

- Các bit network được đặt là 1, các bit phần host được đặt là 0

![Hình 35](assets/image83.png)

*Hình 35*

![Hình 36](assets/image15.png)

*Hình 36*

- **CIDR (Prefix length):** việc đặt Subnet mask quá dài dòng, để ngắn gọn thì dùng CIDR, là cách viết rút gọn của Subnet mask bằng cách đếm số lượng số bit 1.

![Hình 37](assets/image48.png)

*Hình 37*

![Hình 38](assets/image54.png)

*Hình 38*

- Số Host có thể dùng = 2^ số bit host - 2 (trừ đi network address, địa chỉ broadcast)

- Khi 1 thiết bị muốn gửi packet đi, nó cần xác định xem địa chỉ đích có cùng trong mạng nội bộ hay không hay cần phải gửi ra ngoài internet + nhờ Subnet mask mà ta có thể xác định được

- AND(IP nguồn, Subnet mask nguồn) == AND(IP đích, Subnet mask đích) thì 2 thiết bị chung mạng nội bộ → packet sẽ chỉ cần chuyển tới switch và chuyển tới đích >< nếu khác thì 2 thiết bị khác mạng → packet cần chuyển tới default gateway (thường là router) để chuyển ra ngoài internet.

![Hình 39](assets/image35.png)

*Hình 39*

![Hình 40](assets/image31.png)

*Hình 40*

## UDP, TCP

### UDP

- TCP (Transmission Control Protocol) là giao thức truyền tải hướng kết nối, đáng tin cậy nằm ở Transport layer

- **Connection - oriented (hướng kết nối):** trước khi gửi dữ liệu, một lối đi ảo phải được thiết lập giữa client, server thông qua quy trình bắt tay 3 bước + khi server phản hồi thành công lại cho client thì kết nối cũng sẽ được close đi (nói là kết nối thế thôi, nhưng nó chỉ là khái niệm logic, kiểu như là sự xác nhận của 2 bên khi truyền đạt dữ liệu để đảm bảo tin cậy)

- **Reliable (đáng tin cậy):** đảm bảo dữ liệu gửi tới đích nguyên vẹn

- Đảm bảo máy gửi điều chỉnh được tốc độ gửi dữ liệu dựa trên khả năng của máy nhận + tự động giảm tốc độ gửi dữ liệu khi mạng bị tắc nghẽn

- Phù hợp với ứng dụng mà đảm bảo độ tin cậy cao nhất có thể, chấp nhận chậm một chút cũng không sao (gửi mail, truyền file, SSH,...)

- **3 - way handshake:** là bước đầu tiên khi thực hiện thiết lập TCP connection

![TCP three-way handshake](assets/image82.png)

*TCP three-way handshake*

### TCP

- Giả sử A muốn truyền dữ liệu tới B qua 1 TCP connection, trước khi truyền A cần thiết lập TCP connection trước qua 3 - way handshake như sau:

1. **B1:** A gửi B packet có cờ SYN và seq bắt đầu của A (VD: 100) với ý nghĩa: chào B, tôi là A, tôi muốn kết nối với bạn, số thứ tự bắt đầu của tôi là 100 (đảm bảo A có khả năng gửi)

1. **B2:** B nhận packet, gửi lại A 1 packet cờ SYN, seq bắt đầu của B (VD: 300) kèm theo cờ ACK để xác nhận, ACK number (số báo nhận, VD = 101 = seqA + 1) với ý nghĩa: chào A, tôi đã nhận được yêu cầu của bạn (ACK = 101), tôi đồng ý kết nối, số thứ tự bắt đầu của tôi là 300 (đảm bảo B có khả năng nhận, gửi)

1. **B3:** Sau khi TCP connection được thiết lập, gửi lại B 1 packet seq = 101, ACK = 301 (301 = ACK B + 1) để báo rằng đã nhận được packet seq = 300 của B. (đảm bảo A có khả năng nhận)

- **4 - way handshake:** là quy trình để 2 máy tính đồng ý ngắt kết nối TCP sau khi hoàn thành việc trao đổi dữ liệu. Mục đích là để đảm bảo cả 2 bên đều đã gửi hết dữ liệu và sẵn sàng đóng kết nối

1. **B1:** A gửi packet có cờ FIN tới B với ý nghĩa: Này B, tôi đã gửi xong toàn bộ dữ liệu rồi, tôi muốn đóng channel gửi dữ liệu từ tôi tới bạn, tuy nhiên tôi vẫn sẵn sàng nhận dữ liệu từ bạn nếu bạn còn gì để gửi.

1. **B2:** B gửi packet có cờ ACK với ý nghĩa: oke A, tôi đã nhận được yêu cầu đóng kết nối của bạn, nhưng tôi vẫn còn đang xử lý → connection vẫn còn mở

1. **B3:** B gửi packet có cờ FIN tới A với ý nghĩa: này A, tôi đã xử lý xong rồi, tôi sẽ đóng channel gửi dữ liệu của tôi tới bạn

1. **B4:** A gửi packet có cờ ACK tới B với ý nghĩa: đồng ý, tôi đã nhận được, chúng ta hãy cùng đóng kết nối an toàn.

- **UDP (User Datagram Protocol):** là giao thức không kết nối và không đáng tin cậy nằm ở Transport layer

- Được thiết kế để gửi các gói dữ liệu (gọi là diagram) một cách nhanh chóng và hiệu quả

- **Connectionless (không kết nối):** UDP không thiết lập kết nối trước khi gửi dữ liệu, máy cứ thế phóng các gói tin đi mà không cần biết gói nhận có đang sẵn sàng hay không → tiết kiệm thời gian, tài nguyên hơn TCP

- **Unreliable (không đáng tin cậy):** máy gửi không nhận được ACK từ máy nhận + nếu gói tin mất trên đường đi thì UDP không có cơ chế gửi lại → gói tin mất vĩnh viễn

- Phù hợp với các ứng dụng ưu tiên tốc độ, thời gian thực hơn là độ chính xác tuyệt đối (streaming video, game online, DNS,...)

- **Socket:** là cổng logic mà chương trình sử dụng để kết nối với chương trình khác chạy trên máy tính khác trên internet, sử dụng để truyền, nhận dữ liệu qua internet.

### Socket

![Socket giữa hai process](assets/image18.png)

*Socket giữa hai process*

- Giữa 2 chương trình chạy trên mạng cần liên kết 2 chiều để kết nối 2 chương trình này với nhau, điểm cuối của liên kết này được gọi là Socket (biểu diễn kết nối giữa Client và Server, Socket thì như điểm đầu, điểm cuối của connection)

- Socket giúp TCP, UDP định danh ứng dụng mà dữ liệu sẽ được gửi tới qua port

- Socket hỗ trợ hầu hết các hđh (Window, Linux,...) + được sử dụng với nhiều ngôn ngữ lập trình phổ biến (C, C++, Java,…)

- **Stream Socket:** còn được gọi là Socket hướng kết nối, là socket hoạt động dựa trên giao thức TCP

### Stream Socket

![Stream socket](assets/image12.png)

*Stream socket*

- Stream Socket chỉ hoạt động khi server, client đã được kết nối với nhau

- Dữ liệu truyền đi đảm bảo đến đúng nơi nhận, đúng thứ tự và thời gian nhanh chóng.

- Mỗi thông điệp gửi đi đều có xác nhận trả về để thông báo về thông tin truyền tải.

- **Phía Server:**

1. **B1:** Khi chương trình khởi động, tạo main socket: socket() để tạo socket, bind() để gắn socket với port, IP của máy chủ + lúc này main socket có trạng thái là LISTEN, main socket lắng nghe các kết nối từ Client.

1. **B2:** Khi có Client kết nối tới, Server chấp nhận kết nối accept(), tạo 1 Socket khác để có thể giao tiếp với Client đó + main socket tiếp tục lắng nghe các Client khác.

- **Phía Client:**

1. **B1:** Khi Client send request, sẽ khởi tạo Socket: socket()

1. **B2:** connect() Client gửi yêu cầu kết nối tới Server dựa vào IP, port

1. **B3:** Sau khi kết nối được thiết lập, cả 2 bên có thể gửi, nhận dữ liệu qua socket của mình.

1. **B4:** Khi giao tiếp xong, 2 bên sẽ close() để đóng connection, Client sẽ close socket đi.

- **Datagram Socket:** còn được gọi là socket không kết nối, hoạt động qua giao thức UDP

### Datagram Socket

![Datagram socket](assets/image69.png)

*Datagram socket*

- Khác ở chỗ chỉ cần 1 main socket ở phía server để handle toàn bộ request từ các client (không cần tạo socket mới vì không nắm giữ thông tin như TCP: số lượng packet, ACK number, trạng thái kết nối,...)

- Channel là con đường logic/ vật lý để dữ liệu truyền từ điểm này tới điểm khác

- Có thể hình dung nó như đường ống/ làn đường dành riêng cho việc vận chuyển dữ liệu + mỗi Channel cho phép luồng thông tin riêng biệt di chuyển giữa các thiết bị.

### Channel vật lý và Channel logic

- **Channel vật lý:** nằm ở Physical layer, là phương tiện truyền dẫn như: cáp đồng trục, cáp quang, sóng vô tuyến (như wifi),...

- **Channel logic:** nằm ở Transport layer, là channel ảo được tạo ra bởi các giao thức thông qua các socket + chạy trên 1 channel vật lý bằng các kỹ thuật như multiplexing + 1 channel vật lý có thể chứa hàng trăm channel logic khác nhau + 1 channel logic mang dữ liệu cho 1 kết nối/ dịch vụ khác nhau

![Hình 45](assets/image49.png)

*Hình 45*

![Hình 46](assets/image2.png)

*Hình 46*

![Hình 47](assets/image32.png)

*Hình 47*

- **Channel trong mạng không dây:** thường là 2.4GHz/ 5GHz, được chia thành nhiều channel nhỏ hơn (giống như 1 làn đường trên đường cao tốc vậy) để giúp các mạng không dây gần nhau có thể hoạt động mà không nhiễu lẫn nhau bằng cách chọn các channel khác nhau. (VD: nhà mình và hàng xóm cùng dùng channel 6 thì hiệu suất mạng có thể bị kém đi)

- **Channel trong lập trình mạng:** trong lập trình, 1 channel riêng biệt thể hiện như 1 Socket connection giữa Client và Server.

### Simplex, Half-duplex và Full-duplex

- **Simplex (đơn công):** dữ liệu chỉ có thể truyền theo 1 hướng duy nhất (đài phát thanh, truyền hình,... ta chỉ có thể nghe/ xem mà không thể gửi tín hiệu ngược lại qua cùng Channel đó (bản chất là do thiết kế của thiết bị: đài phát được thiết kế chỉ để phát sóng điện từ ra môi trường, radio chỉ có mạch thu, anten để bắt sóng điện từ, không có bộ phận phát tín hiệu về đài)

- **Half-duplex (bán song công):** dữ liệu có thể truyền theo 2 hướng, nhưng không đồng thời (bộ đàm: một người nói “over” để báo hiệu kết thúc và chuyển sang chế độ nghe, cho phép người kia trả lời), chỉ 1 thiết bị được phát tại 1 thời điểm (xảy ra khi tín hiệu cùng tồn tại trên 1 channel chung: nếu 2 đầu cùng send message thì sẽ bị collision gây biến dạng mà 2 đầu không thể nhận biết được message → giải pháp là chờ tới lượt nhau)

- **Full-duplex (song công):** dữ liệu có thể truyền đi đồng thời theo cả 2 hướng (gọi điện, gọi video call,... cả 2 người có thể nói và nghe cùng lúc). Bản chất là do đã loại bỏ phương tiện truyền dẫn chung mà sử dụng đường dẫn tách biệt cho gửi và nhận riêng (Cáp quang sử dụng hai sợi thủy tinh riêng biệt: 1 sợi để gửi, 1 sợi để nhận)

### Multiplexing

- **Multiplexing (ghép kênh):** là việc hợp nhất nhiều Channel logic lại để có thể truyền chúng đồng thời qua 1 Channel vật lý duy nhất

- Thường xảy ra ở phía nguồn phát

- Mục đích là để tiết kiệm chi phí xây dựng nhiều Channel vật lý, tận dụng tối đa băng thông của phương tiện truyền dẫn

- Mỗi Channel logic được gán dấu hiệu nhận dạng (tần số, thời gian, mã,...) trước khi trộn lại và đẩy vào Channel vật lý.

![Hình 48](assets/image13.png)

*Hình 48*

![Hình 49](assets/image59.png)

*Hình 49*

![Hình 50](assets/image41.png)

*Hình 50*

### Demultiplexing

- **Demultiplexing (tách kênh):** ngược lại, tách 1 luồng tín hiệu tổng hợp từ Channel vật lý thành các Channel logic ban đầu

- Thường xảy ra ở phía nhận tín hiệu

- Mục đích để phân phối dữ liệu tới đúng đích cuối cùng (dựa trên dấu hiệu lúc multiplexing, để phân loại lại packet và định tuyến chúng đến đúng đích)

![Hình 51](assets/image25.png)

*Hình 51*

![Hình 52](assets/image63.png)

*Hình 52*

### Workflow

![Hình 53](assets/image75.png)

*Hình 53*

- IP nguồn đích =1 Socket

![Hình 54](assets/image58.png)

*Hình 54*

![Hình 55](assets/image39.png)

*Hình 55*

![Hình 56](assets/image74.png)

*Hình 56*

![Hình 57](assets/image50.png)

*Hình 57*

![Hình 58](assets/image77.png)

*Hình 58*

![Hình 59](assets/image37.png)

*Hình 59*

## Basic Characteristics

- Fault tolerance (khả năng chịu lỗi) là 1 đặc điểm cơ bản tối quan trọng trong mạng máy tính, tức mạng máy tính vẫn tiếp tục hoạt động bất kể là mạng đang có lỗi (continue working despite failures) + sẽ không mất đi request hiện tại đang truyền đi khi mạng bị lỗi (ensure no loss of service)

![Mạng có nhiều đường đi dự phòng](assets/image65.png)

*Mạng có nhiều đường đi dự phòng*

- Giả sử Computer ta muốn communication với Web Server amazon kia thì

- Computer -> Switch -> Wireless Router -> Router1 -> Router3 -> Router5 -> Switch -> WebServer

- Tuy nhiên khi mà link/ Router1 gone down thì request vẫn tiếp tục thực hiện và hệ thống mạng máy tính này vẫn sẽ hoạt động, request vừa rồi thay vì được rout tới Router1 thì sẽ được rout tới Router2 -> Router4 -> Router5 ->…

- Scalability (khả năng mở rộng) tức khi ta thêm nhiều máy tính vào 1 network thì mạng này vẫn sẽ hoạt động như bình thường

- Giả sử tôi connect 100 computer vào Router3 thì mạng vẫn hoạt động bình thường

- Thêm mạng mới thì hệ thống mạng toàn cầu vẫn hoạt động bình thường.

- Quality of Service (QoS) liên quan tới việc đặt mức độ ưu tiên xử lý (priorities) + quản lý lưu lượng dữ liệu để tránh mất dữ liệu, giảm delay trên quá trình truyền tải dữ liệu

- **Khi giữa 2 request:** nói chuyện điện thoại và gửi email và việc handle request này đều trải qua Router1 tại cùng 1 thời điểm + Router có khả năng nhận ra rằng việc nói chuyện điện thoại là realtime nên nó sẽ ưu tiên xử lý trước để routing tới Router khác

### Security (khả năng bảo mật) tức mạng máy tính phải có tính bảo mật

- Giả sử ta send 1 request với thông tin khá nhạy cảm, trong quá trình routing thì tại Router3, hacker đã steal được request của ta tại Router3

- Nếu dữ liệu là nhạy cảm thì cần convert data này sang 1 form khác và chỉ có thể biết được bởi sender và receiver (integrity)

## Network Protocols & Communication

### Protocol

- Data communication là sự trao đổi data giữa các nodes thông qua một số dạng phương tiện truyền dẫn (transmission medium)

- Link chỉ sự kết nối giữa 2 thiết bị, có thể là logic/ vật lý (chỉ mang tính chất là nói về connection) >< Transmission medium (form of Link) là bản chất của phương tiện truyền dẫn (cáp, dây đồng, sóng vô tuyến,...)

- Protocol (giao thức) quản lý toàn bộ hoạt động trao đổi thông tin trong mạng máy tính, là tập các quy tắc, quy ước được thiết lập để các nodes trong 1 network có thể communicate với nhau một cách hiệu quả và an toàn

![Internet là network of networks](assets/image4.png)

*Internet là network of networks*

- Protocol xác định cách data được định dạng, truyền đi, nhận về, cách các lỗi được xử lý trong quá trình truyền nhận dữ liệu

- Bất kể là liên lạc qua đường bưu điện, whatsapp, sms,... đều phải có protocol nhất định

- Các nodes phải chấp thuận các quy tắc đặt ra của protocol để các nodes có thể hiểu thông điệp đưa ra từ mỗi bên

- **Các protocols phổ biến có thể kể tới như:** HTTP, TCP, IP, 4/5G, Ethernet, Skype, Wifi,…

- Các protocols này thường được mô tả ở trong RFC (Request for Comments) + thường được các tổ chức phát hành, bảo trì như: IETF (Internet Engineering Task Force), W3C,...

- Why need protocol

### Syntax, Semantics và Synchronization

- Nếu không tuân thủ các quy tắc đặt ra của protocol thì các nodes sẽ không biết cách trao đổi dữ liệu với nhau, nodes nhận được data có thể sẽ không hiểu/ giải được data đó dẫn đến việc truyền tải không thể chính xác

- Data có thể hỏng/ mất data trong quá trình truyền tải do không tuân thủ nguyên tắc

- Không có cách nào để quản lý luồng dữ liệu, dẫn tới data có thể truyền quá nhanh/ quá chậm làm tắc nghẽn mạng/ gây delay đáng kể

- Các protocol thường tích hợp với các phương thức bảo mật data trong quá trình truyền tải + message encoding + message formatting, encapsulation + message timing + message size,...

- Protocol giúp xác thực các bên tham gia giao tiếp, nếu không có protocol thì không có cách nào mà đảm bảo rằng thông tin đến từ 1 nguồn hợp lệ/ người nhận có quyền nhận thông tin đó

- Các protocol như TCP/ IP là nền tảng của internet, nếu không có protocol này thì Internet và các hệ thống mạng khác sẽ không thể hoạt động

- Bandwidth (băng thông) dùng để chỉ tốc độ truyền dữ liệu/ tiêu thụ tối đa của 1 kết nối mạng trong 1 khoảng thời gian nhất định (2 người dùng trên 2 thiết bị khác nhau, gói mạng khác nhau thì băng thông là khác nhau cho mỗi thiết bị >< nếu 2 người dùng chung 1 wifi thì băng thông sẽ được chia sẻ) >< Throughput (thông lượng) là tốc độ truyền dữ liệu thực tế của 1 kết nối mạng trong 1 khoảng thời gian nhất định.

- Được dùng để đo lường lượng dữ liệu có thể truyền qua 1 kênh truyền (có dây/ không dây) trong 1 giây

- **Đơn vị thường dùng là:** Mbps (Megabit/ giây), Gbps (Gigabit/ giây), Kbps (Kilobit/ giây) ⇒ Băng thông cao thì dữ liệu đi qua càng nhiều, nhanh >< Băng thông thấp dữ liệu sẽ có gặp tình trạng tắc nghẽn, tốc độ truyền chậm.

- **Các yếu tố có thể ảnh hưởng tới băng thông như:** loại kết nối, gói cước nhà mạng, số lượng người dùng chung,...

### Các dạng truyền dữ liệu

- **Khi tải 1 file 10GB với gói mạng 100Mbps:**

- 10GB = 10 * 1024 = 10240MB

- 1MB = 8Mb ⇒ 10240MB = 81920Mbs

- Thời gian tải = 81920Mb / 100Mbps = 13p 39s

- Ngoài ra thực tế mạng không bao giờ đạt 100% như lý thuyết trên, thường delay thêm nhiều.

- Khi xem 1 video netflix 4K (25Mbps)

- Giả sử ta đang dùng gói 50Mbps, xem phim 4K cần 25Mbps

- Băng thông còn lại = 50 - 25 = 25Mbps + khi xem thêm 1 video (35Mbps), lướt web (3Mbps) tổng là 38Mbps dẫn tới vượt giới hạn, video có thể bị giật.

- Giả sử 1 row 50kb.

- Tổng kích thước

- • 1 row = 50 KB → 1,000,000 rows = 50,000,000 KB = 48,828.125 MB = ≈47.68 GiB.

### Bandwidth, Throughput, Delay và Packet loss

- **Nếu đọc toàn bộ:**

- • HDD (seq) ≈ 150 MB/s → 48,828 / 150 ≈ 325 s ≈ 5.4 min.

- • SATA SSD ≈ 500 MB/s → 48,828 / 500 ≈ 98 s ≈ 1.6 min.

- • NVMe high-end ≈ 3000 MB/s → 48,828 / 3000 ≈ 16 s.

- • RAM (nếu dữ liệu đã cached) ~20,000 MB/s → 48,828 / 20,000 ≈ 2.4 s.

- **Tốc độ truyền:**

- • Network 1 Gbps = 125 MB/s → 48,828 / 125 ≈ 391 s ≈ 6.5 min.

- • Network 10 Gbps = 1250 MB/s → 48,828 / 1250 ≈ 39 s.

- **Nếu để xử lý cho người dùng thật thì sẽ kết hợp:** phân trang truy vấn + cache app + client thì dùng lazy load. Não người không load nổi 1M rows đâu nên khỏi lo.

- Nếu để xử lý để export cần tốc độ thì bắt buộc chơi stream cả phía app lẫn client, query cũng không cần phân trang nữa. App phải đọc từng gói tin nhỏ gửi từ stream của db sau đó gửi cho client cũng đọc từng cục nhỏ lấy từ stream.

- Tại sao lại phải chia nhỏ dữ liệu thành các package để truyền qua mạng:

- Dữ liệu lớn khi gửi 1 lần sẽ gây nghẽn mạng >< chia nhỏ thành các packet sẽ tận dụng được băng thông tốt hơn + packet có thể di chuyển theo nhiều cách chỉ cần đến được đích, tránh tắc nghẽn mạng (phù hợp với kiến trúc phân tán)

- Dữ liệu lớn khi gửi gặp lỗi sẽ phải gửi lại toàn bộ >< nếu 1 packet lỗi thì chỉ cần gửi lại packet đó.

- Dữ liệu nguyên khối lớn sẽ chiếm toàn bộ đường truyền >< chia packet thì dữ liệu packet từ nhiều người dùng có thể dùng chung được

- Có một số trường hợp đặc biệt truyền thẳng mà không chia packet như: truyền dữ liệu thời gian thực dùng UDP gửi nguyên khối, không đảm bảo tin cậy nhưng nhanh hơn;  Hoặc trong mạng nội bộ tốc độ cao, đôi khi truyền nguyên frame lớn

## TCP/IP model

### Application Layer

- **OSI (Open System Interconnection):** là mô hình lý thuyết tiêu chuẩn (tức là chỉ là để cho dễ hiểu thôi) để mô tả cách thức hệ thống mạng hoạt động.

- OSI chia quy trình truyền dữ liệu qua mạng thành 7 layer nhỏ hơn, mỗi layer là một chức năng, nhiệm vụ riêng biệt.

- Lý do chia làm nhiều layer là để phân nhiệm vụ cụ thể cho từng layer + dễ dàng quản lý, khắc phục lỗi xem lỗi nằm ở layer nào thay vì kiểm tra toàn bộ hệ thống + các hãng khác nhau có thể phát triển thiết bị/ phần mềm cho từng layer riêng biệt miễn là chúng tuân theo quy chuẩn chung của layer đó thì vẫn có thể hoạt động được với nhau.

- → Tuy nhiên trong thực tế, mô hình sử dụng phổ biến là TCP/IP chỉ có 4 layer còn OSI thường sử dụng để làm chuẩn để giảng dạy, tham chiếu.

- Application Layer =  Application (giao thức sử dụng là gì) + Presentation (mã hóa) + Session (quản lý phiên)

- Layer này tương tác trực tiếp với người dùng, cung cấp giao diện thân thiện cho người dùng + nhận yêu cầu của người dùng, chuyển thành các message đại diện cho yêu cầu của người dùng.

- Không phải các ứng dụng như trình duyệt, zoom, … mà là tập hợp các giao thức, dịch vụ mà những ứng dụng đó sử dụng để giao tiếp qua mạng → hoạt động nằm giữa ứng dụng người dùng và dịch vụ mạng phức tạp bên dưới, xác định cách ứng dụng yêu cầu và sử dụng mạng (xác định cách ứng dụng trao đổi dữ liệu) >< trong khi các layer bên dưới chỉ là phương tiện để truyền dữ liệu một cách tin cậy, hiệu quả.

- **VD:** khi mở trình duyệt lên, bật google lên -> trình duyệt sử dụng HTTP để gửi request tới máy chủ google -> máy chủ phản hồi bằng trang web google

- **Sử dụng các giao thức như:** HTTP, IMAP, SMTP, DNS, Telnet/ SSH, ... (tùy vào loại ứng dụng đang sử dụng)

### Transport Layer

- **Đơn vị dữ liệu:** message/ data, là dữ liệu thuần túy của application, chưa thêm thông tin gì

- **Dạng dữ liệu:** ở dạng con người có thể hiểu được (email, file, video, web request,...), dữ liệu dần chuyển thành dữ liệu nhị phân trước khi xuống các tầng thấp hơn

- ⇒ Có thể nói layer này là cầu nối giữa người sử dụng và mạng máy tính, cung cấp các giao thức để ứng dụng giao tiếp qua mạng, xác định cách ứng dụng trao đổi dữ liệu qua mạng.

- **Transport Layer:** phụ trách vận chuyển dữ liệu từ process máy nguồn tới process máy đích một cách hiệu quả

- **Sử dụng các giao thức như:** UDP (không kết nối nên không cần 3-way handshake, không kiểm tra lỗi, không chờ xác nhận, phù hợp với application real-time), TCP (an toàn hơn, kiểm tra lỗi dùng checksum, yêu cầu gửi lại nếu mất packet nhưng chậm hơn, dùng cho web, email, download file)

- **Đơn vị dữ liệu:** Segment (TCP), Datagram (UDP); dữ liệu được chia thành các khối nhỏ từ message, thêm các thông tin bổ trợ cho các message để biết là cần gửi đến process nào.

- Mỗi application đang chạy có 1 port riêng, Transport layer sử dụng port này để định tuyến message tới đúng process đang chạy này.

- **Kiểm soát luồng:** điều chỉnh tốc độ gửi nếu nghẽn mạng

- ⇒ Layer này giúp gửi message giữa các application trên các thiết bị khác nhau + quyết định cách message được gửi đi + đảm bảo application nhận đúng dữ liệu dựa vào port + không quan tâm máy đích là máy nào, chỉ quan tâm là ứng dụng nào cần nhận thông điệp

### Network Layer

- **Network Layer:** phụ trách việc dẫn packet từ máy nguồn tới máy đích, thậm chí xuyên qua nhiều mạng trung gian (router) khác nhau (router, IP hoạt động giữa các mạng với nhau)

- **Giải quyết vấn đề của Link layer không làm được là:** định tuyến xuyên qua các mạng vật lý khác nhau

- **Sử dụng giao thức như:** IP, ICMP (internet control message protocol), routing protocol, IPSec (bảo mật IP packet), ARP

- **Đơn vị dữ liệu:** packet, mỗi packet có Header chứa source IP, destination IP, TTL, thông tin phân mảnh,... + Payload: dữ liệu tầng Transport

- Xác định đường đi tối ưu cho packet qua nhiều mạng trung gian (Router dựa vào destination IP, bảng định tuyến để gửi packet sang bước tiếp theo)

- **Forwarding:** hành động thực tế của router, nhận packet từ cổng này và chuyển tiếp nó ra cổng khác dẫn tới router tiếp theo cho tới đích

- **Logical Addressing:** tầng này gán cho mỗi thiết bị 1 Private IP

- ⇒ Layer này giúp xác định đường đi (routing), tìm đường đi ngắn nhất, phù hợp nhất với tình trạng mạng + thực hiện truyền packet từ nguồn tới đích qua nhiều mạng trung gian + chia nhỏ packet nếu cần + quan tâm máy đích là máy nào, không quan tâm ứng dụng nào của máy đích nhận thông điệp.

### Link Layer

- **Link Layer:** phụ trách việc truyền dữ liệu đi (Network layer lo việc đi đâu >< Link layer lo việc đi như thế nào trên chặng đường cụ thể)

- Vai trò chính là thiết lập, duy trì, quản lý việc truyền dữ liệu giữa 2 thiết bị trên 1 đường truyền vật lý + nhận packet từ network và chuẩn bị chúng để truyền đi trên phương tiện vật lý.

- **Sử dụng giao thức như:** Wifi, Ethernet,... + các thiết bị như: Switch, Bridge

- **Đơn vị dữ liệu:** frame; Link layer nhận packet từ Network layer, gán source destination MAC header, destination MAC header của máy liền kề (next hop), tính CRC

- Mỗi card mạng (Ethernet, Wifi) có 1 địa chỉ MAC duy nhất do nhà sản xuất gán

- IP là địa chỉ logic, dùng để xác định đích cuối cùng >< MAC là địa chỉ vật lý, dùng để chuyển dữ liệu trong mạng cục bộ (Switch, card mạng hoạt động ở Link layer, không hiểu IP)

- **Framing:** nhận packet, bọc chúng thành các frame bằng cách thêm MAC nguồn, MAC đích

- **Trong mỗi liên kết vật lý:** Host -> router -> router -> … Sau mỗi lần chuyển tới thiết bị tiếp theo, source & destination MAC sẽ được thay đổi lại.

![Link Layer](assets/image43.png)

*Link Layer*

### Physical Layer

- **Physical Layer:** phụ trách việc truyền các bit dữ liệu qua phương tiện truyền dẫn từ thiết bị này sang thiết bị khác

- **Dạng dữ liệu:** chuyển đổi các bit nhị phân 1, 0 thành tín hiệu vật lý (điện, ánh sáng, sóng radio,...) để truyền qua môi trường truyền dẫn (có dây/ không dây)

- Xác định đặc tính vật lý của phương tiện truyền dẫn → quyết định cách thức các bit được mã hóa tín hiệu dựa trên phương tiện truyền dẫn vừa xác định + xác định số lượng bit được truyền đi trong 1s

- **Sử dụng các thiết bị như:** repeater, hub, modem, media converter,...

- **Sử dụng các giao thức như:** Ethernet, Wifi, USB, HDMI, Bluetooth,...

### Workflow khi truy cập một website

- **Workflow:** khi mở trình duyệt, truy cập vào https://www.google.com và nhấn enter

1. **B1:** Application layer tạo 1 HTTP/ HTTPS request GET gửi tới máy chủ google

- **DNS phân giải tên miền thành IP (VD:** 142.250.70.206)

- Message được di chuyển xuống Transport layer

1. **B2:** Transport layer thiết lập kết nối dùng TCP 3-way handshake (vì HTTP/ HTTPS sử dụng TCP)

- Trình duyệt gửi gói SYN tới google (tôi muốn kết nối, SEQ = 100)

- Google nhận SYN, phản hồi SYN-ACK (tôi đồng ý, SEQ = 300, ACK = 101)

- Trình duyệt nhận SYN-ACK, gửi ACK (đã nhận SYN-ACK, bắt đầu truyền dữ liệu)

- Lúc này TCP connection đã được thiết lập

- Transport layer đóng gói HTTP request vào TCP segment (source port, destination port, sequence number: 101, ACK number: 301,...)

- TCP segment được chuyển tiếp xuống Network layer để định tuyến google.

1. **B3:** Network layer đóng gói TCP segment vào IP packet (source IP, destination IP)

- Gửi IP packet qua router, ISP, Internet tới máy chủ google.

## Application Layer

- Các ứng dụng mạng hoạt động trên các End system, liên lạc với nhau qua hệ thống mạng máy tính (VD: web server chạy trên máy tính chủ cũng là ứng dụng + các ứng dụng khác chạy trên máy người dùng đều có thể liên lạc với web server)

- Bất cứ khi nào vào trình duyệt, gõ 1 trang ra bản thân là ứng dụng trình duyệt sẽ gửi yêu cầu tới web server.

- Các máy nằm trên Network core sẽ không chạy ứng dụng người dùng mà chỉ để routing.

- **Peer to peer:** là mô hình phi tập trung, client vừa là server chia sẻ trực tiếp dịch vụ với nhau.

- BitTorrent, Blockchain,...

- **Client server:** là mô hình tập trung, nơi server cung cấp dịch vụ, client yêu cầu và sử dụng dịch vụ.

- **server:** ứng dụng cung cấp dịch vụ (luôn sẵn sàng chờ đợi yêu cầu của ứng dụng người dùng nên nó luôn luôn phải được chạy + luôn có tên miền cố định + ứng dụng này nằm trong các data center để có thể dễ dàng scale)

- **client:** ứng dụng dành cho người dùng cuối (việc khởi tạo kết nối tới server nằm ở client + kết nối này có thể không liên tục + không cần cố định địa chỉ IP vì ai cũng có thể kết nối tới server + các client thường không kết nối trực tiếp tới nhau mà thông qua server vì VD: ta gửi tin nhắn cho 1 người nhưng họ không bật máy thì sẽ không thể gửi được >< con server luôn bật, lưu tin nhắn nếu người dùng không hoạt động).

- Process là ứng dụng mà đang chạy thì được gọi là process (Có thể vào Task manager để xem những ứng dụng đang chạy ở tab Process)

- Nếu 2 process trong cùng thiết bị mà muốn communicate với nhau thì sử dụng Inter-process communication (được cung cấp bởi OS)

- Nếu 2 process khác thiết bị muốn communicate với nhau thì phải thông qua message.

- IP giúp biết chuyển message tới thiết bị nào >< Port giúp biết chuyển message tới process nào (bản thân DNS sẽ phân giải cả IP, Port để biết cần gửi cho máy nào, process nào)

### Mối quan hệ giữa Application Layer và Transfer Layer

![Yêu cầu truyền tải của các ứng dụng](assets/image53.png)

*Yêu cầu truyền tải của các ứng dụng*

![Hình 64](assets/image67.png)

*Hình 64*

- Các dịch vụ ở tầng trên phụ thuộc vào dịch vụ cung cấp ở tầng bên dưới -> khi các ứng dụng hoạt động phải dựa vào Transfer Layer ở bên dưới.

- **Data integrity:** liên quan tới tính toàn vẹn dữ liệu cao như gửi file, gửi mail thì muốn gửi đi thế nào nhận y thế -> cần việc truyền dữ liệu đáng tin cậy 100%

- **Timing:** liên quan tới việc dữ liệu truyền đi nhanh chóng mà không cần đảm bảo chính xác hoàn toàn như: gọi điện (mất vài âm, xoẹt xoẹt nhưng cũng chẳng ảnh hưởng tới cuộc trò chuyện, chấp nhận mất mát nhất định), chơi game ⇒ Transfer layer không cần đảm bảo, miễn nhanh là được.

- **Throughput:** một vài ứng dụng yêu cầu thông lượng tối thiểu như xem phim online, streaming,... truyền đi dữ liệu lớn hỗn hợp cả hình ảnh, âm thanh,... yêu cầu băng thông đạt tới mức độ nhất định.

## Web and HTTP

### Web page, Website và Object

- Web page (trang web) là 1 tài liệu thường được viết bằng HTML, có thể được hiển thị trong trình duyệt web (giống như 1 trang trong quyển sách nhưng ở dạng kỹ thuật số)

- Tập hợp các Web page liên kết với nhau dưới dạng tên miền chung thì tạo thành 1 Website (trang mạng)

- chứa nhiều objects, có thể được lưu trên nhiều Web servers khác nhau

- Objects có thể là HTML file, JPEG, audio file, …

- Mỗi web page đều chứa 1 HTML file cơ sở, nó sẽ tham chiếu tới các objects khác nhau thông qua url.

![Cấu trúc URL](assets/image24.png)

*Cấu trúc URL*

- Khi truy cập url như google.com, trình duyệt gửi 1 HTTP request qua mạng internet đến server lưu trữ web page đó → server nhận được yêu cầu, tìm kiếm các tệp tin (HTML, CSS, JS, image,...) cần thiết cho web page mà mình muốn xem, gửi chúng trở lại trình duyệt dưới dạng packet → trình duyệt nhận được packet, lắp ráp, render HTML, CSS, JS để hiển thị web page hoàn chỉnh lên màn hình.

### Hosting

- **Hosting:** là dịch vụ cho thuê chỗ trên các máy chủ, giúp lưu trữ: code, văn bản, hình ảnh, database,... luôn bật 24/7

- Khi gõ tên miền vào trình duyệt, trình duyệt sẽ kết nối tới máy chủ hosting, tải toàn bộ dữ liệu từ hosting về trình duyệt

- **Shared hosting:** thuê hosting dùng chung với nhiều người thuê khác, rẻ nhưng bị ảnh hưởng tài nguyên vì dùng chung với các người thuê khác

- **VPS (máy chủ riêng ảo):** cũng hosting chung với nhiều người thuê khác trên 1 máy tính (chia 1 máy chủ vật lý thành các máy chủ con độc lập), tuy nhiên phần tài nguyên của từng người thuê sẽ được cố định, riêng tư, vì vậy cũng đắt hơn chút, đòi hỏi hơn về kiến thức kỹ thuật cũng như quản lý.

- **Virtualization Technology (công nghệ ảo hóa):** một máy chủ vật lý mạnh với CPU nhiều core, RAM lớn, ổ cứng SSD được cài phần mềm ảo hóa (như VMWare, KVM, Hyper-V) giúp chia máy chủ vật lý thành nhiều máy ảo độc lập, mỗi máy ảo tương ứng 1 VPS.

- Mỗi VPS có hđh riêng (Window server/ các bản phân phối linux như CentOS, Ubuntu,...) + một phần CPU, RAM, ổ cứng được cấp phát riêng cho từng VPS

- **Dedicated Server:** thuê nguyên 1 máy chủ vật lý, toàn bộ tài nguyên là riêng của mình, vì vậy hiệu năng mạnh mẽ hơn, bảo mật cao, toàn quyền kiểm soát tài nguyên, nhưng giá thành sẽ cao hơn (phù hợp website lớn, lượng truy cập khổng lồ)

- **Cloud hosting:** website được host trên mạng lưới nhiều máy chủ kết nối với nhau, cực kỳ ổn định, dễ dàng mở rộng tài nguyên ngay lập tức, chỉ trả tiền cho những gì mình dùng (phù hợp với mọi website, đặc biệt với loại website biến động về lưu lượng truy cập nhiều)

### Domain

- **Domain (tên miền):** là địa chỉ website, là phần mà mọi người gõ vào thanh địa chỉ của trình duyệt để truy cập website (VD: google.com, youtube.com, facebook.com,…)

- Nếu website là ngôi nhà → domain như địa chỉ của ngôi nhà đó → hosting (dịch vụ lưu trữ) là mảnh đất mà ngôi nhà đó được xây lên.

- Thiết bị và server giao tiếp thông qua IP, domain tạo ra để thay thế cho địa chỉ IP khó nhớ đó + DNS sẽ chuyển đổi domain thành IP tương ứng để trình duyệt có thể tải website.

- **Cấu trúc:** subdomain . second-level-domain . top-level-domain . (root)

- **Root domain:** dấu chấm cuối cùng trong tên miền, đại diện cho gốc của hệ thống tên miền. Từ Root domain đổ đi sẽ bị ẩn bởi trình duyệt (đó là lý do tại sao gõ google.com thì trên thanh url của trình duyệt chỉ hiển thị google).

- **Top-level domain (TLD):** phần đuôi miền, là phần cuối cùng của domain, đứng sau Root domain (tức đứng sau dấu (.) cuối cùng của domain), bị ẩn đi trên thanh url của trình duyệt → cho biết loại hình/ phạm vi hoạt động của website

![Hình 66](assets/image26.png)

*Hình 66*

- **Second-level domain (SLD):** là tên chính của domain, nằm ngay trước TLD, là phần độc nhất do ta tự lựa chọn và đăng ký (youtube.com thì youtube là SLD)

- **Subdomain:** tên miền phụ, là phần mở rộng tách ra từ tên miền chính, đứng ở phía trước SLD (www.youtube.com thì www là 1 subdomain)

- Khi có Domain chính rồi, ta có thể tạo vô hạn subdomain hoàn toàn miễn phí + và subdomain có thể trỏ tới server khác tùy vào cấu hình

![Hình 67](assets/image44.png)

*Hình 67*

### HTTP

- HTTP (hypertext transfer protocol): là giao thức thuộc Application layer được sử dụng để truyền tải dữ liệu giữa Client và Server trên mạng internet

- Mỗi khi nhấp vào 1 link/ nhập 1 địa chỉ web vào trình duyệt, ta đang khởi tạo 1 HTTP request và gửi tới server → sau đó thì server sẽ xử lý và gửi lại HTTP response.

- HTTP hoạt động dựa trên mô hình Client - server: trình duyệt thiết lập kết nối TCP tới web server thông qua 1 port mặc định (thường là port 80 cho HTTP) → trình duyệt gửi 1 HTTP request đến server chứa các thông tin như: HTTP method, URL, Header, Body,... → server nhận request, xử lý, gửi lại HTTP response cho trình duyệt chứa các thông tin: Status Code, Response header, Response body,... → Sau khi phản hồi được gửi đi, kết nối sẽ bị đóng

![HTTP status code](assets/image56.png)

*HTTP status code*

- HTTP hoạt động dựa trên giao thức TCP/IP: HTTP là nội dung request + TCP đóng vai trò chia nhỏ, đánh số request để đảm bảo request đến đúng nơi nhận + IP đóng vai trò làm người chỉ dẫn request tới đúng nơi nhận (routing). Lý do phải dựa trên TCP/IP là vì HTTP không quan tâm dữ liệu được vận chuyển thế nào, chỉ định ra nội dung giao tiếp → cần TCP/IP để nội dung này đến đúng nơi nhận.

- **Stateless:** HTTP là 1 giao thức không trạng thái, tức mỗi request sẽ đều độc lập, server sẽ không nhớ bất kỳ thông tin nào về các request trước đó → mỗi request cần phải gửi thêm định danh/ context.

### HTTP/0.9

- HTTP 0.9: là phiên bản đầu tiên của HTTP, cực kỳ thô sơ chỉ với mục đích duy nhất là truyền HTML qua internet.

- Chỉ sử dụng HTTP GET mà không có bất kỳ phương thức nào khác

- Request, response không có phần header → không thể gửi metadata như: loại dữ liệu, loại mã hóa, ngôn ngữ,...; không thể gửi cookie, không có khả năng xác thực, server không thể biết được loại trình duyệt/ hđh mà client đang sử dụng

- Response chỉ chứa duy nhất dữ liệu HTML, không thể chứa hình ảnh, CSS, JS hay bất kỳ định dạng nào khác + nếu có lỗi thì server sẽ trả về HTML mô tả lỗi đó chứ không có HTTP status code.

- Ngay sau khi server phản hồi HTML file, TCP connection ngay lập tức bị đóng

- ⇒ Ngày nay HTTP 0.9 không còn được sử dụng nữa vì nó quá đơn giản, chỉ phù hợp với thời kỳ đầu của web vì lúc đó mọi thứ quá thô sơ và HTTP 0.9 là đủ rồi >< Website hiện đại thì HTTP 0.9 là không đủ.

### HTTP/1.0

- HTTP 1.0: là phiên bản update của HTTP 0.9, biến nó từ giao thức đơn giản thành giao thức linh hoạt cho web

- Ra đời là để khắc phục những hạn chế lớn của HTTP 0.9

- Giờ đây mỗi khi request cần phải chỉ ra là HTTP version mấy

- Hỗ trợ HTTP status code để giúp client có thể biết chính xác kết quả của request

- Hỗ trợ HTTP header để cung cấp thêm context cho cả request, response: loại trình duyệt, hệ điều hành đang sử dụng của thiết bị, loại dữ liệu mà server cần xử lý, loại dữ liệu trong response, kích thước response,... → nhờ Content-Type header, web không chỉ còn là văn bản mà có thể chứa cả hình ảnh, âm thanh và các định dạng khác → biến website trở thành nền tảng đa phương tiện

- **Hỗ trợ HTTP method khác ngoài GET:** HEAD, POST → cung cấp POST để cho phép người dùng gửi dữ liệu, mở ra kỷ nguyên của biểu mẫu, bình luận, giao dịch,... → tạo nền tảng cho sự tương tác

- Tuy nhiên vẫn còn nhược điểm là kết nối không liên tục như HTTP 0.9, 1 TCP mới được thiết lập cho mỗi request và bị đóng ngay sau khi hoàn thành → gây hiệu suất thấp do mỗi lần request đều phải 3-way handshake.

### HTTP/1.1

- HTTP 1.1: là phiên bản update, khắc phục những điểm yếu về hiệu suất, tính năng của HTTP 1.0, là phiên bản sử dụng phổ biến suốt gần 2 thập kỷ qua.

- **Persistent Connections:** thay vì mỗi request cần TCP connection mới, gây độ trễ thì HTTP 1.1 cung cấp Connection: keep-alive header giúp 1 TCP connection có thể sử dụng cho nhiều request/ response → giảm đáng kể độ trễ so với 3-way handshake liên tục với mỗi request, giảm tải cho cả client, server.

- **Pipeline:** client có thể gửi nhiều request liên tiếp mà không cần chờ phản hồi cho từng cái >< Head-of Line Blocking: nếu request đầu tiên xử lý chậm, tất cả các request sau nó sẽ phải đợi request đầu xử lý, response xong rồi các request sau mới được response + ta có thể không bật pipeline thì sẽ không gặp vấn đề này, nhưng từng request/ response sẽ lần lượt trên 1 TCP/IP → vẫn sử dụng nhiều TCP connection đồng thời.

- HTTP 1.0 thì 1 máy chủ vật lý chỉ có thể host 1 website vì không có cách nào để phân biệt, → HTTP 1.1 cung cấp Host header bắt buộc cho mọi request, lợi ích là support Virtual hosting, cho phép nhiều website với các domain khác nhau cùng chia sẻ 1 IP, 1 máy chủ vật lý

- **Caching mạnh mẽ:**

![Hình 69](assets/image78.png)

*Hình 69*

- **Hỗ trợ Chunked Transfer Encoding:**

![Hình 70](assets/image6.png)

*Hình 70*

- **Cung cấp các method bổ sung:**

![Hình 71](assets/image29.png)

*Hình 71*

### HTTP/2

- HTTP 2.0 xuất bản 2015, là bước đột phá lớn trong hiệu suất web, giải quyết các vấn đề của HTTP 1.1

![So sánh HTTP/1.1 và HTTP/2](assets/image84.png)

*So sánh HTTP/1.1 và HTTP/2*

- HTTP 2.0 được xây dựng dựa trên SPDY protocol của Google, giải quyết hầu hết vấn đề của HTTP 1.1, tăng hiệu suất sử dụng băng thông, giảm độ trễ.

- **Multiplexing over Single Connection:** cho phép gửi, nhận nhiều request/ response đồng thời trên cùng 1 TCP connection mà không cần phải đợi như HTTP 1.1 → đã giải quyết được Head-of-line blocking + cũng cho phép bật độ ưu tiên của từng request (CSS, JS ưu tiên thì lấy trước, request lấy ảnh ở tận phía dưới màn hình cần cuộn chuột thì lấy sau,...) >< nếu 1 packet hỏng/ thất lạc, toàn bộ kết nối TCP sẽ dừng lại để chờ packet đó được gửi lại

- Mỗi request được gán stream id để giúp phân biệt từng request với nhau + mỗi frame của từng request được gán stream id tương ứng + chúng được trộn lẫn với nhau và gửi qua 1 TCP duy nhất → sau khi nhận response thì mỗi frame cũng được gán stream id để nhận biết là response của request nào, lắp ghép lại rồi hiển thị lên màn hình.

- HTTP 2 được chuyển sang hệ nhị phân, các frame được thiết kế để máy tính phân tích nhanh, hiệu quả hơn nhiều

- **HPACK:** giúp nén header để giảm kích thước khi truyền/ nhận dữ liệu trên internet bằng cách: duy trì bảng tra cứu các header đã được sử dụng trước đó, chỉ gửi sự khác biệt trong các request tiếp theo.

- **Server push:** server có thể tự động đẩy các tài nguyên như CSS, JS, ảnh,... về client ngay cả khi client không yêu cầu chúng dựa trên dự đoán rằng client sẽ cần gì (Kiểu lần đầu load HTML, trả về client, đồng thời trả thêm CSS, JS mà không cần client request).

## Email, SMTP, IMAP

### Trong hệ thống thư điện tử gồm các thành phần chính:

![Hệ thống thư điện tử](assets/image64.png)

*Hệ thống thư điện tử*

- **User agent:** trình đọc thư, soạn thư (outlook, gmail,…) ⇒ có thể nói là ứng dụng mà người dùng sử dụng để gửi/ nhận email

- **Mail server:** nơi nhận mail từ người gửi, lưu lại chúng + user agent sẽ kiểm tra thư đến, nếu có sẽ lấy về để đọc

- **Outgoing message queue:** khi 1 thư gửi từ user tới mail server, thư chưa được chuyển đi ngay mà đưa vào queue + dần dần gửi tới mail server đích qua SMTP + nếu server đích lỗi thì thư được giữ lại ở Retry queue và gửi lại sau

- **User mailbox:** là nơi chứa thư đến, khi server đích nhận được thư, sẽ lưu vào mailbox của người nhận + sau đó user agent dùng IMAP hoặc POP3 để lấy thư từ mailbox

### Workflow trong hệ thống thư điện tử:

1. **B1:** email soạn rồi gửi thư từ user@gmail.com tới user@outlook.com

1. **B2:** Mail server của người gửi (google server) nhận được thư, verify tính hợp lệ của thư, lưu thư vào Outgoing message queue

1. **B3:** Mail server (google server) tra DNS MX record của domain đích qua outlook.com, kết nối tới Mail server này (microsoft server) qua SMTP rồi gửi thư.

1. **B4:** Mail server (microsoft server) của người nhận kiểm tra giả mạo, spam, tính hợp lệ rồi thư lưu lại thư vào User mailbox

1. **B5:** Người nhận thư mở User agent, kết nối tới Mail server qua IMAP/ POP3 để lấy thư.

### Lý do phải có nhiều Mail server vì:

- Tất cả người dùng không thể dùng chung 1 server, nó sẽ quá tải

- Việc có nhiều server sẽ đặt ở nhiều nơi khác nhau, gần người dùng sẽ giảm độ trễ mạng.

- Mỗi mail server sẽ phục vụ cho một nhóm người dùng nhất định theo domain + sự triển khai server của các công ti là khác nhau (VD: gmail.com dùng server google, ptit.com dùng server của riêng họ,…)

- Nếu 1 server gặp sự cố, chỉ ảnh hưởng tới 1 phần người dùng.

- Các công ty lớn như Google, Outlook, Yahoo,... xây dựng hệ thống mail phân tán, kết hợp với nhau qua các giao thức chuẩn để tạo thành mạng lưới thư điện tử toàn cầu

- **DNS MX record (Mail exchange record):** là loại bản ghi DNS quan trọng, dùng để chỉ định Mail server nào có trách nhiệm nhận email khi gửi đến 1 domain cụ thể (khi gửi mail tới user@ptit.com, sẽ truy vấn tới DNS để tìm MX record của domain ptit.com, DNS trả về danh sách các mail server được ủy quyền nhận mail + yên tâm là mỗi mail server này đều có backup và độ ưu tiên riêng).

### SMTP

- **SMTP (simple mail transfer protocol):** là giao thức thuộc Application sử dụng để truyền tải thư điện tử email

- Sử dụng để truyền tải thư điện tử từ User agent tới Mail server hoặc giữa các Mail server

- Thiết kế để gửi mail, không phải sử dụng để nhận mail.

- Khi sử dụng giao thức này, Transfer layer sẽ sử dụng TCP để đảm bảo thông tin được gửi tới nơi một cách an toàn, đầy đủ.

- HTTP là client pull (Client chủ động lấy dữ liệu server về) >< SMTP là client push (Client push mail tới server)

- HTTP thì mỗi object (HTML, ảnh, css, js,...) sẽ được trả về trong 1 response riêng biệt (mỗi lần tham chiếu lại tạo 1 request) >< SMTP không gửi nhiều message riêng biệt mà gói toàn bộ object vào 1 mail duy nhất dưới dạng MIME multipart (nội dung text, các file đính kèm vào 1 message)

- HTTP, SMTP thì đều sử dụng ASCII

- SMTP sử dụng TCP persistent connection, giữ nguyên connection sau khi gửi message để tái sử dụng để gửi nhiều email thay vì mở gửi đóng liên tục (VD: khi gửi 1 mail nhưng cho nhiều người dùng thay vì mỗi lần gửi mail cho 1 user thì tạo 1 connection).

- Trong mail, gồm 2 phần là body là nội dung thư điện tử + header để chỉ gửi từ đâu tới đâu (Lúc đầu From là user@gmail.com, To là user@outlook.com. Tuy nhiên khi đã tới Mail server của google nó sẽ đổi From là từ Mail server của google, To là Mail server của microsoft)

### IMAP và POP3

- IMAP (internet mail access protocol) là 1 giao thức thuộc Application layer sử dụng để User agent truy cập, quản lý mail trên Mail server.

- POP3 tải toàn bộ mail về máy >< IMAP giữ mail trên server, chỉ lấy về phần nội dung cần thiết

- **Đồng bộ nhiều device:** do lưu mail trên server, mở ứng dụng thư điện tử trên nhiều thiết bị đều sẽ có chung trạng thái.

## DNS

### Cách DNS hoạt động

- Các thiết bị trong mạng internet có số định danh là IP (định dạng số) + con người thì không giỏi nhớ những con số, mong muốn sử dụng chuỗi có ý nghĩa để dễ nhớ hơn >< các máy tính chỉ hiểu và giao tiếp qua IP.

- DNS (domain name system) là hệ thống phân giải tên miền trên internet (số điện thoại của internet)

- **Sử dụng giao thức DNS ở Application layer:** khi người dùng gõ google.com thì gọi tới DNS để mapping sang IP

- **Distributed database:** hệ thống DNS thì phân tán toàn cầu gồm hàng triệu máy chủ DNS

![Hệ thống DNS phân cấp](assets/image11.png)

*Hệ thống DNS phân cấp*

- **Root DNS server:** máy chủ gốc, là cấp cao nhất trong hệ thống DNS (thực tế có 13 cụm Root server nhưng thực tế lại có ngàn bản sao phân tán trên khắp thế giới)

- **Khi browser cần phân giải tên miền (VD:** nyu.edu), nếu chưa có trong cache nó sẽ truy vấn tới Root server + Root server sẽ không trả về IP trực tiếp mà forward tới Top Level Domain của .edu.

- Top Level Domain server (TDL DNS server) quản lý các miền cấp cao (VD:. com, .org, .edu, .vn,...) + khi được forward từ Root DNS server, nó tiếp tục forward tới Authoritative server của nyu.edu

- Authoritative server là nơi chứa IP cho nyu.edu thực sự, sau khi được forward từ TLD server, nó sẽ trả về IP thực sự.

## Video streaming, Content distribution networks

### Video streaming

- Streaming video = encoding (mã hóa thành chất lượng khác nhau, đặt lên DNS khác nhau) + DASH (adaptive, dynamic theo tình trạng) + buffering.

- Chiếm phần lớn băng thông của internet (80% vào 2020) ⇒ thách thức là làm sao để mở rộng quy mô, tăng số lượng người truy cập đồng thời + sự ổn định khi người dùng ở từng khu vực địa lý, từng cách kết nối mạng (khu vực xa xôi hẻo lánh, 4G, dây,...) ⇒ cần xây dựng hệ thống phân tán trải rộng ra nhiều nơi + người dùng sẽ kết nối tới server gần mình nhất để xem.

- Video bản chất là chuỗi các hình ảnh (> 30 images/ s) và mỗi ảnh chỉ khác nhau 1 chút thôi + mỗi ảnh được cắt ra thành các hàng, các cột tạo thành 1 tập hợp các ô (pixel được thể hiện bằng lượng bit nhất định)

- Khi muốn truyền dữ liệu ảnh này đi thì cần mã hóa để số lượng bit phải truyền ít hơn với lượng bit mô tả hình ảnh này

- **Mã hóa theo không gian:** ở trên 1 bức ảnh, có những điểm ảnh giống nhau cả về màu sắc, hình ảnh, độ sáng ⇒ Thay vì gửi toàn bộ thì những điểm ảnh giống nhau sẽ gửi 1 đi thôi để thể hiện cho toàn bộ điểm ảnh giống nhau.

- **Mã hóa theo thời gian:** video là chuỗi các hình ảnh, các hình sau khác một chút so với hình trước ⇒ Thay vì gửi toàn bộ các ảnh riêng biệt, ta chỉ gửi các phần khác nhau giữa các ảnh.

- Băng thông sẽ thay đổi liên tục (mức độ tắc nghẽn tùy thời điểm sử dụng) + packet loss, delay do tắc nghẽn dẫn tới video bị vỡ, chậm, xoay tròn.

- **Client-side buffer:** sử dụng để hạn chế vấn đề các packet gửi tới client bị chậm → client sử dụng vùng nhớ tạm buffer, tích trữ lượng dữ liệu tạm thời để client có thể xử lý trơn tru mà không phụ thuộc hoàn toàn vào tốc độ internet (VD: khi xem youtube, không phải cứ nhận packet tới đâu là sẽ phát video tới đó vì nếu đường truyền gặp vấn đề nhỏ cũng sẽ gây lag cho video → sử dụng Client-side buffer thì sẽ tải trước một phần video sau vào buffer, client sẽ phát video đã lưu tạm trong buffer thay vì lấy liên tục từ network nên dù network có vấn đề thì buffer vẫn giữ đoạn video sau đó và phát cho người dùng → lúc nào vào xem cũng hơi chậm lúc đầu một chút, kể cả mất wifi thì một lúc sau mới xảy ra hiện tượng không xem tiếp được video) + buffer cũng hạn chế được việc nếu packet gửi về quá nhanh, video sẽ không chạy nhanh mà phụ thuộc vào tốc độ phát của buffer.

### DASH

- DASH (Dynamic Adaptive Streaming over HTTP) là một kỹ thuật quan trọng trong streaming video (Youtube, Netflix đều đang sử dụng).

- Thay vì gửi nguyên cả video một lần, server chia video thành các đoạn nhỏ (chunks), mỗi đoạn (2-10s chẳng hạn) được mã hóa ở nhiều chất lượng khác nhau (144p, 360p,...) (VD: video 10p được chia thành 150 chunks với 4s/ chunk + mỗi chunk là 1 file .ts hoặc .m4s)

![Hình 75](assets/image5.png)

*Hình 75*

- Client sẽ lựa chọn phiên bản phù hợp nhất theo tốc độ mạng hiện tại (adaptive streaming)

- Các file nhỏ này được lưu ở nhiều CDN (Content delivery network) → client thường lấy ở server gần client nhất để tải được nhanh + giảm tải cho server gốc. Ví dụ thư mục sẽ nằm trên CDN như:

![Hình 76](assets/image62.png)

*Hình 76*

- **Manifest file:** chứa danh sách các phiên bản video ở chất lượng khác nhau

- **Master Manifest:** là file Manifest chính được tải về, liệt kê các playlist con cho từng chất lượng

![Hình 77](assets/image68.png)

*Hình 77*

![Hình 78](assets/image3.png)

*Hình 78*

- **Variant Manifest:** chứa các url sử dụng để down chunks

![Hình 79](assets/image17.png)

*Hình 79*

- Client tải Manifest file về khi người dùng bấm play

- Client dựa vào tốc độ mạng hiện tại sẽ quyết định tải chunks ở tốc độ nào

- Client liên tục đo tốc độ mạng để điều chỉnh adaptive theo thời gian thực + dựa vào buffer để có tiếp tục tải nữa không.

## Network Topology

- Network topology (layout) có thể hiểu như khi ta có nhiều nodes và muốn sắp xếp các nodes này để có thể thiết lập liên lạc giữa tất cả các nodes

- Physical Topology là 1 cấu trúc liên kết tập trung vào việc các nodes được kết nối như thế nào, cách thức đặt các node

- Logical Topology là 1 cấu trúc liên kết tập trung vào cách data được truyền đi như thế nào giữa các nodes

### Bus topology

![Bus topology](assets/image23.png)

*Bus topology*

- Bus topology thì có 1 transmission medium chung + tất cả các nodes sẽ được connect tới nó

- Node A muốn send data qua node B thì truyền nhận thông qua transmission medium này (tuy nhiên các nodes trong network này sẽ đều nhận được copy của data này đồng thời với B -> không security)

- Do transmission medium chung này có thể nhận, gửi data nên nó sẽ là bi-directional

- Rẻ, chỉ cần 1 wire cho toàn bộ nodes -> phù hợp khi xây dựng 1 mạng tạm thời

- Các node nối độc lập với transmission medium chung -> việc hỏng 1 node sẽ không ảnh hưởng tới toàn bộ network >< tuy nhiên nếu transmission medium chung mà lỗi thì tất cả các nodes sẽ bị hỏng

- Khó scale khi cable như cũ nhưng số lượng nodes tăng lên

### Ring topology

![Ring topology](assets/image47.png)

*Ring topology*

- Bản thân nó chính là Bus topology nhưng trong 1 vòng tròn khép kín

- Dạng Peer-to-peer LAN

- Mỗi nodes sẽ có 2 connections đến mỗi node hàng xóm

- Việc communicate là Unidirectional

- Ổn định hơn so với Bus + đứt tại 1 nơi thì vẫn có khả năng mạng vẫn work tốt trong 1 số trường hợp (đứt ở C thì A vẫn communicate với B được)

- Giả sử việc node A muốn giao tiếp với node D + thứ tự kim đồng hồ là A -> B -> C -> D -> E -> A và chu kì là chiều kim đồng hồ + giả sử node A muốn communicate với node D thì node A send data tới node B -> node B send data tới node C -> node C send data tới node D và kết thúc quá trình send data

### Star topology

![Star topology](assets/image45.png)

*Star topology*

![Hình 83](assets/image81.png)

*Hình 83*

- Các nodes được connect với nhau gián tiếp qua 1 central node (hub/ switch) + tất cả các data đều phải thông qua central node này

- Nhờ có central node, nó giúp xác định path mà data phải đi thay vì bắn toàn bộ data tới toàn bộ node như Bus

- Dễ scale + dễ implement

- Tuy nhiên có thể gây overload tới Switch/ Hub + chi phí tăng do phải sử dụng Switch/ Hub + việc hỏng central node sẽ gây hỏng toàn bộ hệ thống

### Mess topology

![Mesh topology](assets/image72.png)

*Mesh topology*

- Dạng lưới, các nodes sẽ link tới toàn bộ các nodes còn lại của network

- 1 link bị lỗi không ảnh hưởng, hệ thống vẫn làm việc bình thường + giả sử node A và C bị lỗi connection, tuy nhiên chúng vẫn có thể communicate với nhau gián tiếp qua node khác, miễn là có đường đi với nhau

- Broadcasting issue + tốn chi phí với 1 network lớn

## MAC Addressing

### MAC Address

- Media Access control (MAC) address là địa chỉ duy nhất được gắn cho mọi thiết bị mạng để nhận diện.

- MAC nằm ở lớp liên kết dữ liệu (Layer 2 của OSI), là unique trên toàn bộ internet, tuy nhiên lại thường được sử dụng ở LAN để có thể routing data từ Switch tới đúng thiết bị cần nhận data (Mọi thiết bị trên LAN đều được xác định bởi MAC address)

- Router cần IP để routing đúng + Switch cần MAC address để forward data đúng tới thiết bị cần nhận data

- Nếu khi request ra ngoài mạng, MAC đích của packet sẽ mà MAC của router → khi router nhận phản hồi, nó sẽ dựa vào NAT table để xác định private IP của thiết bị gửi request + có private IP thì tiếp tục tra ARP table để lấy MAC của thiết bị gửi request, đóng thêm MAC đích sẽ là của thiết bị gửi request → switch nhận packet và thấy MAC đích là của thiết bị nào và sẽ gửi tới thiết bị đó.

- Nếu request trong mạng, tìm private IP, MAC address của máy đích (nếu chưa cache) → gán IP đích, MAC đích vừa tìm được vào packet → gửi tới switch, switch tra MAC address table để biết port nào của switch kết nối tới thiết bị đích → truyền packet trực tiếp tới đúng port của thiết bị đích.

- Mọi switch đều giữ MAC address table để biết được node nào cần forward data

- MAC được gán vào phần cứng của card mạng (Network Interface Card - NIC) nên dẫn tới mỗi thiết bị đều có 1 MAC unique và có thể thay đổi bằng cách thay đổi phần cứng/ fake được >< còn IP thì có thể thay đổi khi kết nối tới mạng khác.

- Để xem được MAC address trên máy tính của mình, vào cmd và ấn “ipconfig/all” và nhìn vào mục physical address

- MAC góp phần bảo mật cho LAN, khi ta có thể chặn quyền access vào 1 network cụ thể dựa trên MAC, giúp ngăn chặn sự truy cập trái phép của 1 thiết bị vào 1 network cụ thể, bảo vệ tài nguyên mạng bởi hoạt động độc hại

- MAC cũng sử dụng để tracking, khắc phục sự cố mạng, khi này dùng MAC để tracking luồng dữ liệu trong mạng, xác định nguồn gốc của sự cố mạng

- Khi thiết bị trong mạng gửi tín hiệu ra internet, trước tiên gửi tới router và router cần biết MAC của thiết bị gửi request để có thể gửi response về + nếu là giao tiếp trong mạng thì khi gửi request thì cũng phải biết MAC đích để có thể gửi đúng thiết bị → MAC khi lưu trên chính thiết bị end user thì dùng để gửi các tín hiệu đến đúng đích khi giao tiếp trong mạng + MAC cần lưu trong router để khi nhận phản hồi từ ngoài mạng có thể chuyển tới đúng thiết bị vừa request đó (chỗ này hơi khó hiểu, tại sao cần lưu MAC table trên thiết bị end user, vì request sẽ đi qua wifi đã lưu MAC table rồi mà?)

### ARP

- ARP (Address Resolution Protocol) là giao thức sử dụng để dịch/ tìm ra địa chỉ MAC tương ứng với IP cụ thể trong mạng LAN

![ARP](assets/image10.png)

*ARP*

## Security

### Packet Sniffing: là cách tấn công nghe lén packet trên đường truyền

![Packet sniffing](assets/image71.png)

*Packet sniffing*

- Khi packet được gửi đi, bản chất là nó được chạy trên môi trường mở: các trạm ở trên đường truyền đều có thể bắt được packet

- Đúng chuẩn thì card mạng được thiết kế là không nhận các packet không gửi đến cho mình >< hacker sẽ thiết kế lại card mạng, bất kỳ packet nào mà nó nhận được nó sẽ đều được xử lý dù nó không phải là của mình

### IP spoofing

- **IP spoofing:** là cách tấn công mà hacker giả mạo IP nguồn của packet để che giấu/ đánh lừa hệ thống rằng packet tới từ 1 nguồn đáng tin cậy.

![IP spoofing](assets/image70.png)

*IP spoofing*

- Bởi IP protocol là giao thức best-effort, chỉ quan tâm gửi packet từ nguồn tới đích, không kiểm tra IP nguồn có thật hay không

- Khi gửi tới đích, đích sẽ phản hồi dựa trên IP nguồn được ghi trên packet

- ⇒ Có thể tấn công bằng cách C gửi tới đích nhưng dùng IP B, sau đó khi response đổ lại B thì dùng Packet Sniffing để nghe lén/ gửi quá nhiều request tới đích để nhận được 1 đống response về B (chính là DDoS, che giấu được nguồn gốc tấn công)

### DoS

- DoS (Denial of Service) là kiểu tấn công làm ngập hệ thống mạng/ máy chủ bằng lượng lưu lượng giả mạo khiến nó quá tải, không thể phục vụ người dùng hợp pháp

- Sử dụng lượng lớn máy tính ma liên tục request vào server.
