# AWS từ cơ bản tới nâng cao

## 1. AWS là gì?

AWS, viết tắt của **Amazon Web Services**, là nền tảng điện toán đám mây của Amazon. Nói đơn giản, AWS cho phép bạn thuê hạ tầng công nghệ qua Internet thay vì tự mua máy chủ, tự đặt trong phòng máy, tự kéo mạng, tự thay ổ cứng và tự vận hành tất cả.

Khi dùng AWS, bạn có thể tạo máy chủ, database, ổ lưu trữ, mạng riêng, load balancer, hệ thống log, hệ thống giám sát, queue, cache, CDN và rất nhiều dịch vụ khác bằng vài thao tác trên giao diện hoặc bằng code.

Ví dụ trước đây muốn chạy một ứng dụng web, bạn thường cần:

- Mua hoặc thuê server vật lý.
- Cài hệ điều hành.
- Cấu hình mạng.
- Cài database.
- Cài web server.
- Mua thiết bị backup.
- Tự lo giám sát, bảo mật, mở rộng.

Với AWS, nhiều phần trong số đó có thể được tạo nhanh hơn:

```text
Ý tưởng ứng dụng
  -> tạo hạ tầng trên AWS
  -> deploy ứng dụng
  -> theo dõi log, metrics, chi phí
  -> scale khi có nhiều người dùng
```

AWS không phải là một dịch vụ duy nhất. AWS là một hệ sinh thái rất lớn gồm nhiều dịch vụ nhỏ, mỗi dịch vụ giải quyết một nhóm bài toán riêng.

## 2. Cloud computing là gì?

**Cloud computing**, hay điện toán đám mây, là mô hình sử dụng tài nguyên tính toán qua mạng. Tài nguyên có thể là CPU, RAM, ổ đĩa, database, network, message queue, AI service hoặc công cụ giám sát.

Điểm quan trọng là bạn không cần sở hữu trực tiếp phần cứng. Bạn dùng bao nhiêu thì trả tiền bấy nhiêu, hoặc trả theo gói đã đặt trước.

Một cách hình dung:

```text
Không dùng cloud:
  Công ty tự mua server, tự lắp đặt, tự bảo trì.

Dùng cloud:
  Công ty thuê tài nguyên từ nhà cung cấp cloud như AWS.
```

Cloud không có nghĩa là "máy tính của người khác" theo nghĩa đơn giản quá mức. Đúng là tài nguyên nằm trong data center của nhà cung cấp, nhưng cloud còn bao gồm hệ thống tự động hóa, API, bảo mật, phân quyền, giám sát, backup, scale và nhiều dịch vụ managed đi kèm.

## 3. Vì sao cần AWS?

AWS giải quyết nhiều vấn đề thường gặp khi xây dựng hệ thống:

- Cần server nhanh mà không muốn chờ mua phần cứng.
- Cần mở rộng khi traffic tăng.
- Cần database có backup, replication, monitoring.
- Cần lưu file bền vững.
- Cần triển khai ở nhiều khu vực địa lý.
- Cần phân quyền chặt chẽ.
- Cần log, metrics, alert.
- Cần giảm công vận hành những phần lặp lại.

AWS đặc biệt hữu ích với DevOps vì hạ tầng có thể được quản lý bằng code thông qua Terraform, CloudFormation, AWS CDK hoặc các công cụ tương tự.

## 4. Một số keyword nền tảng

### 4.1. Cloud provider

**Cloud provider** là nhà cung cấp nền tảng cloud. Ví dụ:

- AWS.
- Microsoft Azure.
- Google Cloud.
- Oracle Cloud.

Cloud provider vận hành data center, phần cứng, mạng, dịch vụ nền tảng và API để người dùng tạo tài nguyên.

### 4.2. Resource

**Resource** là một tài nguyên trên cloud. Ví dụ:

- Một EC2 instance.
- Một S3 bucket.
- Một RDS database.
- Một VPC.
- Một IAM role.

Khi học AWS, bạn có thể hiểu mọi thứ bạn tạo ra trên AWS đều là resource.

### 4.3. Region

**Region** là khu vực địa lý lớn nơi AWS đặt nhiều data center. Ví dụ:

- `us-east-1`: Bắc Virginia, Mỹ.
- `ap-southeast-1`: Singapore.
- `ap-northeast-1`: Tokyo.

Khi tạo tài nguyên, bạn thường phải chọn region. Chọn region ảnh hưởng tới độ trễ, giá tiền, dịch vụ hỗ trợ và yêu cầu pháp lý về nơi lưu dữ liệu.

### 4.4. Availability Zone

**Availability Zone**, thường viết tắt là **AZ**, là một khu vực hạ tầng độc lập bên trong một region. Một AZ có thể gồm một hoặc nhiều data center, nhưng được thiết kế để tách biệt với AZ khác về nguồn điện, mạng và hạ tầng vận hành.

Về mặt vật lý, có thể hiểu một AZ là một cụm hạ tầng thật, không phải chỉ là một nhãn logic trong phần mềm. Cụm hạ tầng này có thể gồm:

- Một hoặc nhiều tòa nhà data center.
- Hệ thống điện riêng hoặc được tách biệt.
- Hệ thống làm mát.
- Thiết bị mạng.
- Máy chủ vật lý.
- Kết nối mạng tốc độ cao tới các AZ khác trong cùng region.

AWS không công bố địa chỉ chính xác của từng AZ. Người dùng chỉ thấy tên AZ như `ap-southeast-1a`, `ap-southeast-1b`, `ap-southeast-1c` và chọn nơi đặt subnet hoặc tài nguyên.

Điểm quan trọng là: nếu một tòa nhà, cụm điện, cụm mạng hoặc một phần hạ tầng trong một AZ gặp sự cố, AZ khác trong cùng region được thiết kế để ít bị ảnh hưởng hơn.

Nói đơn giản:

```text
Region
  -> một khu vực địa lý lớn, ví dụ Singapore

Availability Zone
  -> một cụm data center độc lập bên trong region đó
```

Ví dụ region Singapore có thể có nhiều AZ như:

```text
ap-southeast-1a
ap-southeast-1b
ap-southeast-1c
```

Mục tiêu của AZ là giúp hệ thống chịu lỗi tốt hơn. Nếu một AZ gặp sự cố, tài nguyên ở AZ khác vẫn có thể tiếp tục hoạt động nếu bạn thiết kế đúng.

Ví dụ:

```text
Thiết kế yếu:
  Tất cả server và database nằm trong 1 AZ
  -> AZ đó lỗi
  -> toàn bộ hệ thống có thể dừng

Thiết kế tốt hơn:
  Server nằm ở nhiều AZ
  Database có cơ chế Multi-AZ
  Load Balancer nhận traffic ở nhiều AZ
  -> một AZ lỗi
  -> hệ thống vẫn còn tài nguyên ở AZ khác để phục vụ
```

AZ không phải là region. Đây là điểm rất dễ nhầm:

| Khái niệm | Phạm vi | Ví dụ | Dùng để làm gì |
|---|---|---|---|
| Region | Khu vực địa lý lớn | `ap-southeast-1` | Chọn nơi đặt hệ thống, dữ liệu và dịch vụ. |
| Availability Zone | Vùng hạ tầng độc lập trong region | `ap-southeast-1a` | Trải tài nguyên để giảm rủi ro khi một khu vực hạ tầng lỗi. |

Các AZ trong cùng một region **nằm trong cùng khu vực địa lý lớn**, nhưng **khác cụm hạ tầng/data center**.

Ví dụ:

```text
ap-southeast-1
  -> region Singapore

ap-southeast-1a
ap-southeast-1b
ap-southeast-1c
  -> các AZ khác nhau bên trong region Singapore
```

Như vậy:

```text
ap-southeast-1a và ap-southeast-1b
  -> cùng region Singapore
  -> khác Availability Zone

ap-southeast-1 và ap-northeast-1
  -> khác region
  -> Singapore và Tokyo
```

Vì cùng region, các AZ thường có độ trễ thấp với nhau hơn so với hai region khác nhau. Vì khác AZ, chúng vẫn được tách biệt để giảm rủi ro khi một cụm hạ tầng gặp sự cố.

### 4.4.1. VPC, subnet và AZ liên quan với nhau thế nào?

Để hiểu AZ trong thực tế, cần hiểu thêm ba khái niệm đi cùng nhau:

```text
VPC
CIDR
Subnet
```

**VPC**, viết tắt của **Virtual Private Cloud**, là mạng riêng ảo của bạn trong AWS.

Một VPC luôn thuộc về **một region**. Ví dụ nếu tạo VPC ở `ap-southeast-1`, VPC đó nằm trong region Singapore.

Ví dụ:

```text
VPC newgate2601-dev
Region: ap-southeast-1
CIDR:   10.20.0.0/16
```

Trong VPC, bạn chia dải IP lớn thành nhiều subnet nhỏ hơn.

**CIDR** là cách viết dải địa chỉ IP.

Ví dụ:

```text
10.20.0.0/16
```

Nghĩa đơn giản:

```text
10.20.0.0/16
  -> một dải IP lớn dùng cho cả VPC

10.20.0.0/24
  -> một dải IP nhỏ hơn, thường dùng cho một subnet
```

VPC giống phạm vi mạng lớn:

```text
VPC: 10.20.0.0/16
```

Subnet là phần nhỏ được cắt ra từ VPC:

```text
Subnet public-a:      10.20.0.0/24
Subnet public-b:      10.20.1.0/24
Subnet public-c:      10.20.2.0/24
Subnet private-app-a: 10.20.10.0/24
Subnet private-app-b: 10.20.11.0/24
Subnet private-app-c: 10.20.12.0/24
```

Điểm quan trọng nhất:

```text
VPC nằm trong một region.
Subnet nằm trong một Availability Zone cụ thể.
```

Một subnet **không trải qua nhiều AZ**. Nếu muốn hệ thống có tài nguyên ở 3 AZ, bạn phải tạo 3 subnet tương ứng.

Ví dụ VPC trong region Singapore:

```text
VPC 10.20.0.0/16 trong region ap-southeast-1
├── public subnet a      10.20.0.0/24   trong ap-southeast-1a
├── public subnet b      10.20.1.0/24   trong ap-southeast-1b
├── public subnet c      10.20.2.0/24   trong ap-southeast-1c
├── private app subnet a 10.20.10.0/24  trong ap-southeast-1a
├── private app subnet b 10.20.11.0/24  trong ap-southeast-1b
└── private app subnet c 10.20.12.0/24  trong ap-southeast-1c
```

Nếu cần thêm subnet cho data layer, ta tạo thêm nhóm subnet riêng:

```text
isolated data subnet a 10.20.20.0/24 trong ap-southeast-1a
isolated data subnet b 10.20.21.0/24 trong ap-southeast-1b
isolated data subnet c 10.20.22.0/24 trong ap-southeast-1c
```

Như vậy, trong một AZ có thể có nhiều subnet khác nhau. Ví dụ trong `ap-southeast-1a` có thể có:

```text
public subnet a
private app subnet a
isolated data subnet a
```

Ba subnet này cùng nằm trong `ap-southeast-1a`, nhưng khác dải IP và khác mục đích sử dụng.

### 4.4.2. Public subnet, private subnet và isolated subnet

Subnet không tự public hay private chỉ vì tên của nó. Tên `public`, `private` hoặc `isolated` chỉ là cách mình đặt để dễ hiểu. AWS xác định đường đi của traffic bằng **route table** gắn với subnet đó.

Nói chính xác hơn:

```text
Subnet thuộc loại nào
  -> phụ thuộc vào route table
  -> phụ thuộc vào subnet đó có đường ra Internet Gateway, NAT Gateway hay chỉ có route nội bộ
```

**Route table** là bảng định tuyến. Nó nói traffic từ subnet nên đi đâu.

Mỗi route table có nhiều dòng route. Mỗi dòng route thường gồm:

```text
Destination -> Target
```

Trong đó:

| Thành phần | Ý nghĩa |
|---|---|
| `Destination` | Dải IP đích mà traffic muốn đi tới. |
| `Target` | Nơi AWS sẽ gửi traffic nếu destination khớp. |

Ví dụ:

```text
10.20.0.0/16 -> local
0.0.0.0/0    -> Internet Gateway
```

Đọc như sau:

```text
Traffic tới 10.20.0.0/16
  -> ở lại trong VPC

Traffic tới các IP còn lại
  -> đi ra Internet Gateway
```

Route `local` luôn có trong route table của VPC:

```text
10.20.0.0/16 -> local
```

Route này cho phép các tài nguyên trong cùng VPC giao tiếp nội bộ với nhau nếu security group và network ACL cho phép.

Route đặc biệt hay gặp là:

```text
0.0.0.0/0
```

Nghĩa là mọi địa chỉ IPv4 không khớp route cụ thể hơn. Đây thường được gọi là **default route**.

Ví dụ nếu một server trong subnet muốn gọi:

```text
8.8.8.8
```

và route table không có route cụ thể cho `8.8.8.8`, AWS sẽ dùng route:

```text
0.0.0.0/0
```

Sau đó xem target của route này là gì.

Ví dụ thực tế:

```text
Một workload trong private subnet cần tải package từ Internet.

Workload gọi tới IP bên ngoài VPC
  -> route table không thấy IP đó thuộc 10.20.0.0/16
  -> dùng route 0.0.0.0/0
  -> nếu target là NAT Gateway thì traffic đi qua NAT Gateway
  -> nếu không có route 0.0.0.0/0 thì request không ra Internet được
```

#### Public subnet

**Public subnet** là subnet có route đi ra Internet Gateway:

```text
0.0.0.0/0 -> Internet Gateway
```

**Internet Gateway**, viết tắt là **IGW**, là thành phần giúp VPC kết nối với Internet.

Nhưng chỉ có route ra Internet Gateway chưa đủ để một tài nguyên thật sự truy cập được từ Internet. Ví dụ một EC2 trong public subnet cần đồng thời có:

- Subnet route tới Internet Gateway.
- EC2 có public IPv4 hoặc Elastic IP.
- Security group cho phép traffic phù hợp.
- Network ACL không chặn traffic.
- Hệ điều hành hoặc ứng dụng bên trong EC2 đang lắng nghe đúng port.

Nếu thiếu public IP, EC2 nằm trong public subnet vẫn không nhận kết nối trực tiếp từ Internet.

Flow đơn giản:

```text
Internet
  -> Internet Gateway
  -> route table của public subnet
  -> EC2 có public IP
  -> security group cho phép
```

Public subnet thường dùng cho:

- Load Balancer public.
- NAT Gateway.
- Bastion host nếu thật sự cần.

Không nên đặt database hoặc workload nội bộ nhạy cảm trong public subnet.

Ví dụ thực tế:

```text
Người dùng mở https://app.example.com
  -> DNS trỏ tới public Load Balancer
  -> Load Balancer nằm trong public subnet
  -> public subnet có route 0.0.0.0/0 tới Internet Gateway
  -> Load Balancer nhận request từ Internet
  -> Load Balancer forward request vào application ở private subnet
```

Ví dụ khác:

```text
NAT Gateway thường đặt trong public subnet.

Private application subnet muốn đi Internet
  -> gửi traffic tới NAT Gateway
  -> NAT Gateway cần nằm ở public subnet
  -> NAT Gateway dùng Internet Gateway để đi ra Internet
```

#### Một NAT Gateway và mỗi AZ một NAT Gateway khác nhau thế nào?

Khi VPC có nhiều AZ, có hai cách đặt NAT Gateway thường gặp.

Cách 1: dùng **một NAT Gateway duy nhất**.

```text
ap-southeast-1a
  public subnet a
  NAT Gateway

ap-southeast-1b
  private app subnet b
  -> route 0.0.0.0/0 tới NAT Gateway ở AZ-a

ap-southeast-1c
  private app subnet c
  -> route 0.0.0.0/0 tới NAT Gateway ở AZ-a
```

Ý nghĩa:

- Chi phí thấp hơn vì chỉ chạy một NAT Gateway.
- Dễ dùng cho dev/lab.
- Private subnet ở AZ khác vẫn có thể đi Internet qua NAT Gateway này.
- Nhưng nếu AZ chứa NAT Gateway gặp sự cố, private subnet ở các AZ khác có thể mất đường outbound ra Internet.
- Traffic từ AZ khác đi qua NAT Gateway ở AZ-a có thể phát sinh cross-AZ data processing/transfer tùy luồng traffic.

Cách 2: dùng **mỗi AZ một NAT Gateway**.

```text
ap-southeast-1a
  public subnet a
  NAT Gateway a
  private app subnet a
  -> route 0.0.0.0/0 tới NAT Gateway a

ap-southeast-1b
  public subnet b
  NAT Gateway b
  private app subnet b
  -> route 0.0.0.0/0 tới NAT Gateway b

ap-southeast-1c
  public subnet c
  NAT Gateway c
  private app subnet c
  -> route 0.0.0.0/0 tới NAT Gateway c
```

Ý nghĩa:

- Chi phí cao hơn vì có nhiều NAT Gateway.
- Mỗi AZ tự có đường outbound riêng.
- Nếu một AZ gặp sự cố, private subnet ở AZ khác vẫn dùng NAT Gateway trong AZ của chính nó.
- Giảm phụ thuộc cross-AZ cho outbound traffic.
- Phù hợp hơn cho staging/production hoặc hệ thống cần tính sẵn sàng cao.

So sánh nhanh:

| Thiết kế | Chi phí | Khả năng chịu lỗi AZ | Phù hợp |
|---|---|---|---|
| Một NAT Gateway | Thấp hơn | Thấp hơn, vì phụ thuộc một AZ chứa NAT Gateway | Dev, lab, môi trường tiết kiệm chi phí |
| Mỗi AZ một NAT Gateway | Cao hơn | Tốt hơn, vì mỗi AZ có NAT riêng | Production hoặc môi trường cần HA hơn |

Trong môi trường dev, có thể dùng:

```hcl
nat_gateway_mode = "single"
```

Nghĩa là subnet vẫn trải trên nhiều AZ, nhưng NAT Gateway chỉ có một để tiết kiệm chi phí.

Trong môi trường cần tính sẵn sàng cao hơn, có thể dùng:

```hcl
nat_gateway_mode = "one_per_az"
```

Nghĩa là mỗi AZ có NAT Gateway riêng và route table của private subnet trong AZ đó trỏ tới NAT Gateway cùng AZ.

#### Private application subnet

**Private application subnet** là subnet không có route trực tiếp ra Internet Gateway. Nếu cần đi ra Internet để tải package, gọi API bên ngoài hoặc update hệ thống, nó thường đi qua NAT Gateway:

```text
0.0.0.0/0 -> NAT Gateway
```

**NAT Gateway** cho phép tài nguyên private đi ra Internet theo chiều outbound. Internet không dùng NAT Gateway để chủ động mở kết nối trực tiếp vào tài nguyên private.

Flow outbound:

```text
EC2 hoặc workload trong private subnet
  -> route table của private subnet
  -> NAT Gateway trong public subnet
  -> Internet Gateway
  -> Internet
```

Flow inbound trực tiếp từ Internet vào private subnet:

```text
Internet
  -> không kết nối trực tiếp vào private subnet
```

Private application subnet thường dùng cho:

- Application server.
- Container workload.
- Internal service.
- Worker xử lý background job.

Trong thiết kế production phổ biến, người dùng Internet không đi thẳng vào private application subnet. Traffic thường đi qua Load Balancer ở public subnet, rồi Load Balancer forward vào private application subnet.

Ví dụ thực tế:

```text
Người dùng Internet
  -> public Load Balancer trong public subnet
  -> application server trong private application subnet
```

Application server không cần public IP.

Khi application server cần gọi API bên ngoài:

```text
Application server
  -> route table của private subnet
  -> 0.0.0.0/0 tới NAT Gateway
  -> Internet Gateway
  -> API bên ngoài
```

Khi người dùng Internet cố gọi trực tiếp vào application server:

```text
Internet
  -> không có public IP của application server
  -> không có route inbound trực tiếp vào private subnet
  -> kết nối trực tiếp không thành công
```

#### Isolated data subnet

**Isolated data subnet** là subnet dành cho data layer hoặc tài nguyên nhạy cảm hơn. Nó thường không có default route ra Internet Gateway hoặc NAT Gateway:

```text
Không có:
0.0.0.0/0 -> Internet Gateway
0.0.0.0/0 -> NAT Gateway
```

Nó vẫn có route local bên trong VPC:

```text
10.20.0.0/16 -> local
```

Điều này nghĩa là tài nguyên trong isolated subnet vẫn có thể giao tiếp với tài nguyên khác trong cùng VPC nếu được phép, nhưng không tự có đường đi Internet mặc định.

Flow nội bộ:

```text
Application trong private app subnet
  -> route local trong VPC
  -> data service trong isolated data subnet
```

Flow ra Internet:

```text
Data service trong isolated subnet
  -> không có default route ra Internet
```

Isolated data subnet thường dùng cho:

- Database subnet.
- Cache subnet.
- Message broker subnet.
- Tài nguyên chỉ nên nhận traffic nội bộ.

Nếu một tài nguyên trong isolated subnet cần truy cập AWS service mà không đi Internet, có thể dùng **VPC endpoint** nếu dịch vụ đó hỗ trợ.

Ví dụ thực tế:

```text
Application trong private application subnet
  -> kết nối tới database trong isolated data subnet
  -> traffic đi qua route local trong VPC
  -> security group của database chỉ cho phép traffic từ application security group
```

Database trong isolated data subnet không cần public IP.

Nếu database cố đi ra Internet:

```text
Database
  -> route table của isolated subnet
  -> chỉ có route 10.20.0.0/16 -> local
  -> không có 0.0.0.0/0
  -> không có đường Internet mặc định
```

Nếu tài nguyên trong isolated subnet cần ghi log hoặc truy cập một AWS service riêng tư:

```text
Isolated subnet
  -> VPC endpoint nếu dịch vụ hỗ trợ
  -> không cần mở default route ra Internet
```

#### Bảng so sánh

Tóm tắt:

| Loại subnet | Default route thường gặp | Internet vào trực tiếp được không? | Đi Internet outbound được không? | Thường dùng cho |
|---|---|---|---|---|
| Public subnet | `0.0.0.0/0 -> Internet Gateway` | Có thể, nếu resource có public IP và rule cho phép. | Có. | Public Load Balancer, NAT Gateway, bastion nếu cần. |
| Private application subnet | `0.0.0.0/0 -> NAT Gateway` | Không trực tiếp. | Có, qua NAT Gateway. | Application server, container workload, worker. |
| Isolated data subnet | Không có default route ra Internet/NAT | Không trực tiếp. | Không mặc định. | Database, cache, message broker, data service. |

#### Ví dụ route table theo từng loại subnet

Public subnet route table:

```text
10.20.0.0/16 -> local
0.0.0.0/0    -> Internet Gateway
```

Private application subnet route table:

```text
10.20.0.0/16 -> local
0.0.0.0/0    -> NAT Gateway
```

Isolated data subnet route table:

```text
10.20.0.0/16 -> local
```

#### Những điểm dễ nhầm

| Dễ nhầm | Đúng hơn là |
|---|---|
| Đặt tên subnet là `public` thì nó tự public. | Không. Route table mới quyết định đường đi. |
| EC2 trong public subnet chắc chắn truy cập được từ Internet. | Không. EC2 còn cần public IP, security group, network ACL và app lắng nghe port. |
| NAT Gateway cho phép Internet gọi vào private subnet. | Không. NAT Gateway chủ yếu phục vụ outbound từ private subnet ra Internet. |
| Private subnet và isolated subnet giống nhau. | Không. Private subnet thường có outbound qua NAT; isolated subnet không có default route ra Internet/NAT. |
| Route `local` nghĩa là Internet local. | Không. `local` nghĩa là traffic nội bộ trong VPC CIDR. |

### 4.4.3. Vì sao dev vẫn nên tạo subnet ở nhiều AZ?

Lý do không nên chỉ dùng một AZ cho hệ thống nghiêm túc:

- Một AZ có thể gặp sự cố hạ tầng.
- Một số dịch vụ cần subnet ở nhiều AZ để bật tính năng high availability.
- Load Balancer, Auto Scaling, Kubernetes, database managed service và cache thường phát huy tốt hơn khi có nhiều AZ.
- Nếu dev chỉ có một AZ, nhiều lỗi thiết kế network sẽ không lộ ra cho tới staging hoặc production.

Tuy nhiên, nhiều AZ không có nghĩa là mọi thứ đều phải nhân ba chi phí. Có thể giữ cấu trúc nhiều AZ nhưng chọn cấu hình tiết kiệm cho dev:

```text
Dev:
  subnet trải trên 3 AZ
  NAT Gateway có thể dùng 1 cái để tiết kiệm

Production:
  subnet trải trên 3 AZ
  NAT Gateway thường nên có theo từng AZ
  database/cache/message broker bật cơ chế Multi-AZ phù hợp
```

Cần phân biệt:

```text
Nhiều AZ
  -> có hạ tầng trải qua nhiều AZ

High Availability thật sự
  -> ứng dụng, database, load balancer, autoscaling, backup, retry và failover đều được thiết kế đúng
```

Chỉ tạo subnet ở 3 AZ chưa tự động làm hệ thống HA. Nó mới là nền mạng để các dịch vụ phía trên có thể triển khai HA.

Một số hiểu nhầm thường gặp:

| Hiểu nhầm | Đúng hơn là |
|---|---|
| Có 3 AZ là chắc chắn không downtime. | 3 AZ chỉ giảm rủi ro hạ tầng; ứng dụng vẫn cần thiết kế retry, replica, health check và failover. |
| Dev thì chỉ cần 1 AZ. | Dev có thể giảm capacity, nhưng nên giữ topology gần production để bắt lỗi sớm. |
| Subnet có thể trải qua nhiều AZ. | Một subnet chỉ nằm trong một AZ. Muốn nhiều AZ thì tạo nhiều subnet. |
| Multi-AZ luôn dùng để scale read. | Multi-AZ thường phục vụ HA/failover; scale read thường cần read replica hoặc cơ chế riêng tùy dịch vụ. |

### 4.5. Edge Location

**Edge Location** là điểm đặt gần người dùng cuối, thường dùng cho CDN như CloudFront. Thay vì người dùng Việt Nam phải tải ảnh từ server ở Mỹ, CloudFront có thể cache nội dung ở vị trí gần hơn để tải nhanh hơn.

### 4.6. Managed service

**Managed service** là dịch vụ mà AWS quản lý nhiều phần vận hành thay bạn.

Ví dụ Amazon RDS là managed database. Bạn vẫn dùng PostgreSQL hoặc MySQL, nhưng AWS hỗ trợ nhiều việc như:

- Tạo database server.
- Backup.
- Patch hệ điều hành.
- Monitoring cơ bản.
- Multi-AZ replication nếu bật.
- Restore từ snapshot.

Managed service không có nghĩa là bạn không cần biết gì. Bạn vẫn cần hiểu cấu hình, bảo mật, chi phí, backup, scaling và giới hạn dịch vụ.

### 4.7. High Availability

**High Availability**, thường viết tắt là **HA**, nghĩa là hệ thống được thiết kế để ít bị gián đoạn. Một hệ thống HA thường tránh phụ thuộc vào một máy duy nhất, một ổ đĩa duy nhất hoặc một AZ duy nhất.

Ví dụ:

```text
Không HA:
  1 EC2 instance + 1 database trên cùng máy

HA hơn:
  Load Balancer
    -> nhiều EC2 ở nhiều AZ
    -> RDS Multi-AZ
```

### 4.8. Scalability

**Scalability** là khả năng mở rộng khi tải tăng.

Có hai kiểu chính:

- **Vertical scaling**: tăng cấu hình một máy, ví dụ từ 2 CPU lên 8 CPU.
- **Horizontal scaling**: tăng số lượng máy, ví dụ từ 2 EC2 instance lên 10 EC2 instance.

Trong cloud, horizontal scaling rất quan trọng vì nó giúp hệ thống linh hoạt hơn và tránh phụ thuộc vào một máy quá lớn.

### 4.9. Elasticity

**Elasticity** là khả năng tự co giãn theo nhu cầu. Khi traffic tăng, hệ thống tăng tài nguyên. Khi traffic giảm, hệ thống giảm tài nguyên để tiết kiệm chi phí.

Auto Scaling Group là ví dụ quen thuộc cho tính elastic.

## 5. Các mô hình dịch vụ cloud

### 5.1. IaaS

**IaaS**, viết tắt của Infrastructure as a Service, là mô hình thuê hạ tầng cơ bản như máy ảo, ổ đĩa, mạng.

Ví dụ AWS:

- EC2.
- EBS.
- VPC.

Bạn có nhiều quyền kiểm soát, nhưng cũng phải tự quản lý nhiều thứ như hệ điều hành, runtime, patch, security hardening.

### 5.2. PaaS

**PaaS**, viết tắt của Platform as a Service, là mô hình nền tảng chạy ứng dụng. Bạn tập trung nhiều hơn vào code, ít phải quản lý server hơn.

Ví dụ:

- AWS Elastic Beanstalk.
- AWS App Runner.
- Một phần của AWS Lambda.

PaaS phù hợp khi bạn muốn deploy nhanh và không muốn tự cấu hình từng server.

### 5.3. SaaS

**SaaS**, viết tắt của Software as a Service, là phần mềm hoàn chỉnh dùng qua Internet.

Ví dụ:

- Gmail.
- Slack.
- Salesforce.

AWS chủ yếu nổi tiếng với IaaS và PaaS, nhưng AWS cũng có nhiều dịch vụ gần với SaaS như QuickSight.

### 5.4. Serverless

**Serverless** không có nghĩa là không có server. Nó nghĩa là bạn không trực tiếp quản lý server. AWS tự lo phần cấp phát, scale và vận hành runtime ở mức nền tảng.

Ví dụ:

- AWS Lambda.
- Amazon API Gateway.
- Amazon DynamoDB.
- Amazon S3.
- Amazon SQS.

Serverless thường tính tiền theo số request, thời gian chạy hoặc dung lượng dùng. Nó rất hợp cho workload không chạy liên tục hoặc có traffic dao động.

## 6. Cách nhìn tổng quan hệ sinh thái AWS

AWS có rất nhiều dịch vụ. Người mới không nên học bằng cách ghi nhớ toàn bộ tên dịch vụ. Nên học theo nhóm bài toán:

```text
Identity and Access
  -> IAM, Organizations

Networking
  -> VPC, Subnet, Route Table, Security Group, NACL, Route 53

Compute
  -> EC2, Auto Scaling, Lambda, ECS, EKS

Storage
  -> S3, EBS, EFS, Glacier

Database
  -> RDS, Aurora, DynamoDB, ElastiCache

Application Integration
  -> SQS, SNS, EventBridge

Observability
  -> CloudWatch, CloudTrail, X-Ray

Security
  -> KMS, Secrets Manager, WAF, Shield, GuardDuty

Deployment and IaC
  -> CloudFormation, CDK, CodePipeline, CodeBuild
```

## 7. AWS Account

**AWS Account** là tài khoản gốc để sử dụng AWS. Mỗi account có billing, resource, IAM và cấu hình riêng.

Khi mới tạo account, bạn có **root user**. Root user có toàn quyền và rất nguy hiểm nếu bị lộ. Không nên dùng root user cho công việc hằng ngày.

Việc nên làm ngay khi tạo AWS account:

- Bật MFA cho root user.
- Không tạo access key cho root user.
- Tạo IAM user hoặc IAM Identity Center cho người dùng.
- Cấu hình billing alert.
- Dùng IAM role cho workload thay vì hard-code access key.

## 8. IAM là gì?

**IAM**, viết tắt của Identity and Access Management, là dịch vụ quản lý danh tính và quyền truy cập trong AWS.

IAM trả lời các câu hỏi:

- Ai được đăng nhập?
- Ai được gọi API?
- Ai được tạo EC2?
- Ai được đọc S3 bucket?
- Service nào được gọi service nào?

IAM là một trong những phần quan trọng nhất của AWS. Nếu IAM cấu hình sai, hệ thống có thể bị lộ dữ liệu hoặc bị chiếm quyền.

## 9. IAM User, Group, Role và Policy

### 9.1. IAM User

**IAM User** là danh tính đại diện cho một người hoặc một hệ thống cụ thể.

Ví dụ:

- User `alice` cho developer Alice.
- User `ci-bot` cho pipeline cũ cần access key.

Ngày nay, với người dùng con người, nên ưu tiên IAM Identity Center hoặc federation thay vì tạo quá nhiều IAM user dài hạn.

### 9.2. IAM Group

**IAM Group** là nhóm chứa nhiều IAM user. Bạn gắn policy vào group để cấp quyền cho nhiều user cùng lúc.

Ví dụ:

- Group `developers` có quyền đọc log và deploy dev.
- Group `admins` có quyền quản trị rộng hơn.

Group giúp tránh việc gắn quyền thủ công từng user.

### 9.3. IAM Role

**IAM Role** là danh tính có thể được "assume", tức là được một user, service hoặc account khác tạm thời sử dụng.

Role rất quan trọng vì nó cấp credential tạm thời thay vì key cố định.

Ví dụ:

- EC2 assume role để đọc S3.
- Lambda assume role để ghi log vào CloudWatch.
- GitHub Actions assume role để deploy qua OIDC.

### 9.4. IAM Policy

**IAM Policy** là tài liệu JSON mô tả quyền được phép hoặc bị từ chối.

Ví dụ policy cho phép đọc một S3 bucket:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject"],
      "Resource": ["arn:aws:s3:::my-bucket/*"]
    }
  ]
}
```

Giải thích:

- `Effect`: cho phép hay từ chối, thường là `Allow` hoặc `Deny`.
- `Action`: hành động được phép, ví dụ `s3:GetObject`.
- `Resource`: tài nguyên áp dụng.
- `ARN`: Amazon Resource Name, định danh duy nhất của resource trong AWS.

Nguyên tắc quan trọng nhất của IAM là **least privilege**, nghĩa là chỉ cấp quyền nhỏ nhất đủ để làm việc.

## 10. Mối liên hệ giữa các thành phần IAM

Khi mới học IAM, rất dễ bị rối vì AWS có nhiều từ giống nhau: user, group, role, policy, permission, trust, principal. Cách dễ hiểu nhất là tách IAM thành 3 câu hỏi:

```text
Ai muốn làm?
  -> Principal: user, role, service, account

Muốn làm hành động gì?
  -> Action: s3:GetObject, ec2:StartInstances, rds:CreateDBInstance

Làm trên tài nguyên nào?
  -> Resource: S3 bucket, EC2 instance, RDS database
```

Một request tới AWS thường được kiểm tra theo kiểu:

```text
Principal
  -> có policy cho phép action đó không?
  -> action đó áp dụng đúng resource không?
  -> có điều kiện nào chặn không?
  -> có explicit deny nào không?
  -> nếu được phép thì AWS cho thực hiện
```

**Principal** nghĩa là danh tính đang gọi AWS API. Principal có thể là IAM user, IAM role, AWS service như EC2/Lambda, hoặc một AWS account khác.

**Permission** nghĩa là quyền được làm gì đó. Permission thường được mô tả trong IAM policy.

**Policy** là bản mô tả quyền. Policy không tự làm gì nếu chưa được gắn vào user, group hoặc role.

### 10.1. User, Group, Policy liên hệ thế nào?

IAM User đại diện cho một danh tính cụ thể. IAM Group gom nhiều user lại. IAM Policy mô tả quyền. Khi gắn policy vào group, các user trong group nhận quyền đó.

Ví dụ:

```text
Policy: ReadOnlyS3Policy
  -> cho phép s3:ListBucket và s3:GetObject

Group: Developers
  -> gắn ReadOnlyS3Policy

User: An
User: Binh
  -> thuộc group Developers

Kết quả:
  An và Binh đều đọc được S3 theo quyền trong policy
```

Điểm cần nhớ:

- User là "ai".
- Group là "nhóm ai".
- Policy là "được làm gì".
- Gắn policy vào group giúp quản lý quyền dễ hơn gắn từng user.

Nếu có 20 developer cùng cần quyền đọc log, bạn không nên gắn policy cho từng người. Nên tạo group `Developers`, gắn policy vào group, rồi thêm user vào group.

### 10.2. Role và Policy liên hệ thế nào?

IAM Role cũng được gắn policy giống user hoặc group, nhưng role không phải tài khoản đăng nhập cố định. Role là một vai trò có thể được nhận tạm thời.

Ví dụ:

```text
Role: AppReadS3Role
  Permission policy:
    - cho phép đọc object trong S3 bucket app-uploads

EC2 instance được gắn role này
  -> app chạy trên EC2 đọc được S3
```

Điểm quan trọng: role thường dùng cho máy, service hoặc hệ thống tự động, vì role cấp credential tạm thời. Nhờ vậy bạn không cần lưu access key dài hạn trong source code.

### 10.3. Trust Policy và Permission Policy khác nhau thế nào?

Một IAM Role thường có 2 phần rất quan trọng:

```text
Trust policy:
  Ai được phép assume role này?

Permission policy:
  Sau khi assume role, role này được phép làm gì?
```

**Assume role** nghĩa là nhận vai trò đó để lấy credential tạm thời. Credential tạm thời này có quyền đúng bằng permission policy của role.

Ví dụ role cho EC2 đọc S3:

```text
Trust policy:
  EC2 service được assume role này

Permission policy:
  Role này được s3:GetObject trên bucket app-uploads
```

Luồng thực tế:

```text
EC2 instance
  -> được AWS cho assume AppReadS3Role
  -> nhận credential tạm thời
  -> dùng credential đó gọi S3 GetObject
  -> S3 kiểm tra policy
  -> nếu được phép thì trả file
```

Nếu chỉ có permission policy mà trust policy không cho EC2 assume role, EC2 vẫn không dùng được role.

Nếu trust policy cho EC2 assume role nhưng permission policy không cho đọc S3, EC2 assume được role nhưng vẫn không đọc được S3.

### 10.4. Ví dụ thực tế 1: EC2 đọc file từ S3

Bài toán: ứng dụng Spring Boot chạy trên EC2 cần đọc file trong S3 bucket `app-config-bucket`.

Cách không nên làm:

```text
Lưu AWS_ACCESS_KEY_ID và AWS_SECRET_ACCESS_KEY trong application.properties
```

Cách này nguy hiểm vì key có thể bị lộ qua Git, log, file backup hoặc người khác copy nhầm.

Cách nên làm:

```text
1. Tạo IAM Role: SpringBootEc2Role
2. Trust policy cho phép EC2 assume role
3. Gắn permission policy cho phép s3:GetObject trên bucket app-config-bucket
4. Gắn role vào EC2 instance
5. App dùng AWS SDK, SDK tự lấy credential tạm thời từ EC2 metadata
```

Luồng:

```text
Spring Boot app
  -> AWS SDK hỏi EC2 metadata service
  -> nhận credential tạm thời của SpringBootEc2Role
  -> gọi S3 GetObject
  -> S3 kiểm tra role có quyền đọc bucket không
```

Kết quả: app đọc được S3 mà không cần hard-code access key.

### 10.5. Ví dụ thực tế 2: Lambda ghi log và đọc DynamoDB

Mỗi Lambda function cần một execution role.

**Execution role** là IAM role mà Lambda dùng khi function chạy.

Ví dụ:

```text
Lambda function: GetUserProfile
Execution role: GetUserProfileLambdaRole
```

Role này cần policy:

```text
Cho phép:
  - ghi log vào CloudWatch Logs
  - đọc item từ DynamoDB table Users
```

Luồng:

```text
API Gateway gọi Lambda
  -> Lambda chạy với GetUserProfileLambdaRole
  -> Lambda ghi log vào CloudWatch
  -> Lambda gọi DynamoDB GetItem
  -> DynamoDB kiểm tra role có quyền không
```

Nếu quên quyền CloudWatch Logs, Lambda vẫn có thể chạy nhưng không ghi log được. Khi lỗi xảy ra, debug sẽ rất khó.

Nếu quên quyền DynamoDB, Lambda sẽ bị lỗi kiểu access denied khi gọi DynamoDB.

### 10.6. Ví dụ thực tế 3: GitHub Actions deploy lên AWS

Bài toán: GitHub Actions cần deploy Docker image lên ECS.

Cách cũ hay gặp:

```text
Lưu AWS_ACCESS_KEY_ID và AWS_SECRET_ACCESS_KEY trong GitHub Secrets
```

Cách tốt hơn:

```text
GitHub Actions
  -> dùng OIDC
  -> assume IAM Role trong AWS
  -> nhận credential tạm thời
  -> push image lên ECR
  -> update ECS service
```

**OIDC**, viết tắt của OpenID Connect, là cơ chế giúp GitHub Actions chứng minh với AWS rằng workflow này đến từ repo/branch/workflow hợp lệ. AWS tin thông tin đó thông qua trust policy của role.

Role cho GitHub Actions sẽ có:

```text
Trust policy:
  Cho phép GitHub OIDC provider assume role
  Chỉ nếu repo/branch đúng điều kiện

Permission policy:
  Cho phép push image vào ECR
  Cho phép update ECS service
```

Lợi ích:

- Không cần access key dài hạn.
- Có thể giới hạn chỉ branch `main` mới deploy production.
- Credential tự hết hạn.
- Dễ audit qua CloudTrail.

### 10.7. Resource-based policy là gì?

Ngoài policy gắn vào identity như user, group, role, AWS còn có **resource-based policy**.

**Resource-based policy** là policy gắn trực tiếp vào tài nguyên.

Ví dụ phổ biến:

- S3 bucket policy.
- SQS queue policy.
- SNS topic policy.
- KMS key policy.
- Lambda resource policy.

So sánh:

```text
Identity-based policy:
  Gắn vào user/group/role
  Nói rằng identity này được làm gì

Resource-based policy:
  Gắn vào resource
  Nói rằng ai được truy cập resource này
```

Ví dụ S3 bucket policy:

```text
Bucket app-public-assets
  -> bucket policy cho phép CloudFront đọc object
```

Trong trường hợp cross-account, resource-based policy rất hay gặp. Ví dụ account A muốn cho role ở account B đọc S3 bucket, bucket policy có thể chỉ rõ role từ account B được phép đọc.

### 10.8. Khi AWS quyết định allow hay deny

AWS không chỉ nhìn một policy duy nhất. Nó tổng hợp nhiều lớp quyền.

Quy tắc đơn giản cần nhớ:

```text
Mặc định: deny
Nếu có allow hợp lệ: allow
Nếu có explicit deny: deny thắng tất cả
```

**Explicit deny** nghĩa là policy ghi rõ `Deny`. Nếu một policy cho phép nhưng policy khác deny, kết quả cuối cùng vẫn là deny.

Ví dụ:

```text
Policy A:
  Allow s3:GetObject

Policy B:
  Deny s3:GetObject nếu request không dùng HTTPS

Request đọc S3 qua HTTP
  -> bị deny
```

Điều này giúp ép các điều kiện bảo mật như bắt buộc HTTPS, bắt buộc MFA hoặc chặn truy cập từ IP lạ.

### 10.9. Bảng tóm tắt IAM dễ nhớ

| Thành phần | Hiểu đơn giản | Ví dụ |
| --- | --- | --- |
| IAM User | Một danh tính cụ thể | User của developer |
| IAM Group | Nhóm nhiều user | Developers, Admins |
| IAM Role | Vai trò có thể nhận tạm thời | EC2 role, Lambda role, CI deploy role |
| IAM Policy | Bản mô tả quyền | Cho phép đọc S3 |
| Trust Policy | Ai được nhận role | EC2 được assume role |
| Permission Policy | Role/user được làm gì | Role được s3:GetObject |
| Principal | Ai đang gọi AWS API | User, role, service |
| Action | Hành động muốn làm | s3:GetObject |
| Resource | Tài nguyên bị tác động | S3 bucket/object |
| Condition | Điều kiện đi kèm | Chỉ từ IP này, bắt buộc MFA |

### 10.10. Cách tự hỏi khi thiết kế quyền IAM

Khi viết quyền IAM, hãy hỏi theo thứ tự:

1. Ai hoặc service nào cần quyền?
2. Cần làm hành động gì?
3. Làm trên resource nào?
4. Có cần giới hạn theo environment, region, IP, MFA, branch CI/CD không?
5. Có thể dùng role thay vì access key không?
6. Có đang cấp quá rộng như `Action: "*"` hoặc `Resource: "*"` không?
7. Nếu credential bị lộ, thiệt hại tối đa là gì?

Một policy tốt thường trả lời rõ được 3 câu:

```text
Ai?
Làm gì?
Trên tài nguyên nào?
```

Nếu câu trả lời là "ai cũng được làm mọi thứ trên mọi tài nguyên", policy đó gần như chắc chắn quá rộng.

## 11. VPC, CIDR và subnet

VPC, CIDR và subnet đã được giải thích kỹ ở mục **4.4. Availability Zone**, vì ba khái niệm này gắn trực tiếp với AZ:

```text
VPC nằm trong một region.
Subnet nằm trong một Availability Zone cụ thể.
CIDR là dải IP dùng để chia VPC và subnet.
```

Nhắc lại ngắn:

| Khái niệm | Hiểu ngắn gọn |
|---|---|
| VPC | Mạng riêng ảo của bạn trong AWS, thuộc một region. |
| CIDR | Cách viết dải địa chỉ IP, ví dụ `10.20.0.0/16`. |
| Subnet | Mạng con được cắt ra từ VPC, nằm trong một AZ. |
| Public subnet | Subnet có route ra Internet Gateway. |
| Private subnet | Subnet không nhận kết nối trực tiếp từ Internet. |
| Isolated subnet | Subnet không có default route ra Internet hoặc NAT Gateway. |

Khi thiết kế network, đọc lại mục 4.4 trước, sau đó mới đi tiếp các phần route table, Internet Gateway, NAT Gateway, Security Group và Network ACL.

## 12. CIDR và subnet

CIDR và subnet đã được giải thích chi tiết trong mục **4.4. Availability Zone** vì subnet luôn gắn với AZ.

Nhắc lại ngắn:

```text
CIDR
  -> dải địa chỉ IP, ví dụ 10.20.0.0/16

Subnet
  -> một phần nhỏ cắt ra từ VPC
  -> luôn nằm trong một Availability Zone cụ thể
```

Ví dụ:

```text
VPC 10.20.0.0/16
  -> public subnet 10.20.0.0/24 trong ap-southeast-1a
  -> public subnet 10.20.1.0/24 trong ap-southeast-1b
  -> public subnet 10.20.2.0/24 trong ap-southeast-1c
```

## 13. Public subnet và private subnet

**Public subnet** là subnet có route ra Internet Gateway. Tài nguyên trong public subnet có thể truy cập Internet trực tiếp nếu có public IP và security rule cho phép.

**Private subnet** là subnet không route trực tiếp ra Internet Gateway. Tài nguyên trong private subnet không nhận traffic trực tiếp từ Internet.

Kiến trúc phổ biến:

```text
Internet
  -> Application Load Balancer ở public subnet
  -> EC2/ECS ở private subnet
  -> RDS ở private subnet
```

Ứng dụng backend và database thường nên nằm trong private subnet. Chỉ load balancer hoặc bastion host mới cần ở public subnet nếu thật sự cần.

## 14. Route Table, Internet Gateway và NAT Gateway

### 14.1. Route Table

**Route Table** là bảng định tuyến. Nó nói cho AWS biết traffic từ subnet nên đi đâu.

Ví dụ route:

```text
10.0.0.0/16 -> local
0.0.0.0/0  -> Internet Gateway
```

`0.0.0.0/0` nghĩa là mọi địa chỉ IPv4 không khớp route cụ thể hơn.

### 14.2. Internet Gateway

**Internet Gateway**, thường viết tắt là **IGW**, là cổng giúp VPC kết nối với Internet.

Public subnet thường có route:

```text
0.0.0.0/0 -> Internet Gateway
```

### 14.3. NAT Gateway

**NAT Gateway** giúp tài nguyên trong private subnet đi ra Internet nhưng không cho Internet chủ động đi vào tài nguyên đó.

Ví dụ EC2 private cần tải package từ Internet:

```text
EC2 private subnet
  -> NAT Gateway ở public subnet
  -> Internet Gateway
  -> Internet
```

NAT Gateway thường tốn chi phí đáng kể, nên khi học và làm lab cần chú ý xóa nếu không dùng.

### 14.4. Internet Gateway và NAT Gateway khác nhau thế nào?

Hai thành phần này đều liên quan tới Internet, nhưng vai trò không giống nhau.

| Thành phần | Dùng để làm gì | Đặt ở đâu | Chiều traffic chính |
|---|---|---|---|
| Internet Gateway | Cho VPC có đường kết nối trực tiếp với Internet. | Gắn vào VPC, route từ public subnet trỏ tới nó. | Internet vào resource public và resource public đi ra Internet. |
| NAT Gateway | Cho resource trong private subnet đi ra Internet mà không mở chiều vào trực tiếp từ Internet. | Nằm trong public subnet, có Elastic IP. | Private subnet đi ra Internet. Internet không chủ động đi vào private subnet qua NAT Gateway. |

Hiểu đơn giản:

```text
Internet Gateway = cửa chính của VPC ra/vào Internet
NAT Gateway      = cửa đi ra ngoài cho private subnet, không phải cửa đi vào
```

Ví dụ public subnet:

```text
User Internet
  -> Internet Gateway
  -> public subnet
  -> Load Balancer hoặc EC2 có public IP
```

Ví dụ private application subnet đi ra Internet:

```text
Application trong private subnet
  -> NAT Gateway nằm trong public subnet
  -> Internet Gateway
  -> Internet
```

Chiều ngược lại không tự mở:

```text
User Internet
  -> NAT Gateway
  -> private application subnet
  -> không dùng được theo kiểu này
```

Vì vậy:

- Public subnet có route `0.0.0.0/0` tới Internet Gateway.
- Private application subnet có route `0.0.0.0/0` tới NAT Gateway nếu cần tải package, gọi API ngoài hoặc update hệ thống.
- Isolated data subnet không có route `0.0.0.0/0` tới Internet Gateway hoặc NAT Gateway.
- Database thường đặt ở isolated/private subnet, không đặt ở public subnet.

### 14.5. Elastic IP của NAT Gateway là gì?

**Elastic IP** là địa chỉ IPv4 public cố định do AWS cấp cho account.

Với NAT Gateway, Elastic IP là public IP đại diện khi tài nguyên trong private subnet đi ra Internet.

Ví dụ:

```text
Private EC2 IP:          10.20.10.15
NAT Gateway Elastic IP:  13.x.x.x
Server bên ngoài thấy:   13.x.x.x
```

Luồng traffic:

```text
EC2 hoặc app trong private subnet
  -> NAT Gateway
  -> dùng Elastic IP của NAT Gateway
  -> Internet
```

Server bên ngoài không thấy private IP `10.20.10.15`. Nó chỉ thấy request đến từ Elastic IP của NAT Gateway.

Elastic IP của NAT Gateway khác với public IP của resource khác:

```text
EC2 trong public subnet có public IP riêng
NAT Gateway có Elastic IP riêng
Application Load Balancer public có DNS/public IP riêng
```

NAT Gateway IP chỉ đại diện cho traffic outbound từ private subnet. Nó không phải public IP của toàn bộ AWS account và cũng không phải public IP của mọi resource trong VPC.

### 14.6. Subnet IP là public IP hay private IP?

CIDR của VPC và subnet thường là private IP nội bộ VPC.

Ví dụ:

```text
VPC:                         10.20.0.0/16
public subnet:               10.20.0.0/24
private application subnet:  10.20.10.0/24
isolated data subnet:        10.20.20.0/24
```

Các dải `10.20.x.x` ở trên đều là private IP. Tên **public subnet** không có nghĩa IP trong subnet là public IP.

Subnet được gọi là public vì route table của nó có route:

```text
0.0.0.0/0 -> Internet Gateway
```

Subnet được gọi là private vì nó không route trực tiếp ra Internet Gateway. Nếu cần đi ra Internet, nó thường đi qua NAT Gateway:

```text
0.0.0.0/0 -> NAT Gateway
```

Tóm tắt:

| Khái niệm | Là public IP hay private IP? | Ý nghĩa |
|---|---|---|
| `10.20.0.0/16` của VPC | Private IP range | Dải IP nội bộ của VPC. |
| `10.20.0.0/24` của public subnet | Private IP range | Subnet nội bộ nhưng có route ra Internet Gateway. |
| `10.20.10.0/24` của private subnet | Private IP range | Subnet nội bộ, không nhận Internet trực tiếp. |
| Elastic IP của NAT Gateway | Public IP | IP đại diện khi private subnet đi ra Internet. |
| Public IP của EC2 | Public IP | IP riêng của EC2 nếu EC2 được gán public IP. |

Ghi nhớ ngắn:

```text
"Public subnet" không có nghĩa IP trong subnet là public.
Nó chỉ nghĩa là subnet đó có đường trực tiếp ra Internet Gateway.
```

### 14.7. VPC Endpoint là gì?

**VPC Endpoint** là một đường kết nối riêng để resource trong VPC gọi tới một số AWS service mà không cần đi qua Internet public.

Ví dụ app trong private subnet cần gọi S3, ECR, CloudWatch Logs hoặc Secrets Manager. Nếu không có VPC Endpoint, traffic thường đi theo một trong các đường:

```text
Private subnet
  -> NAT Gateway
  -> Internet Gateway
  -> AWS service public endpoint
```

Khi có VPC Endpoint, traffic có thể đi theo đường riêng trong AWS:

```text
Private subnet
  -> VPC Endpoint
  -> AWS service
```

Nói dễ hiểu:

```text
NAT Gateway  = cửa để private subnet đi ra Internet
VPC Endpoint = cửa riêng trong VPC để gọi AWS service được hỗ trợ
```

VPC Endpoint không thay thế mọi kết nối Internet. Nó chỉ dùng được cho các AWS service có hỗ trợ endpoint.

#### Vì sao cần VPC Endpoint?

VPC Endpoint giúp:

- Resource private gọi AWS service mà không cần public IP.
- Giảm phụ thuộc vào NAT Gateway cho một số traffic AWS service.
- Giữ traffic trong mạng AWS thay vì đi qua Internet public.
- Dễ kiểm soát route và security hơn.
- Có thể giảm chi phí NAT Gateway data processing cho traffic tới dịch vụ hỗ trợ endpoint.

Ví dụ EKS node trong private subnet cần pull image từ ECR:

```text
EKS node private subnet
  -> ECR API endpoint
  -> ECR DKR endpoint
  -> S3 endpoint nếu layer image nằm ở S3 backend
```

Nếu không có endpoint, node có thể phải đi qua NAT Gateway để gọi các service đó.

#### Gateway Endpoint và Interface Endpoint

Có hai loại VPC Endpoint thường gặp:

| Loại endpoint | Dùng cho service nào | Gắn vào đâu | Có security group không? |
|---|---|---|---|
| Gateway Endpoint | S3, DynamoDB | Route table | Không dùng security group. |
| Interface Endpoint | ECR, CloudWatch Logs, Secrets Manager, SSM, KMS và nhiều service khác | Elastic Network Interface trong subnet | Có dùng security group. |

**Gateway Endpoint** hoạt động qua route table.

Ví dụ S3 Gateway Endpoint:

```text
Private subnet
  -> route table
  -> S3 Gateway Endpoint
  -> S3
```

Terraform thường gắn Gateway Endpoint vào route table:

```hcl
route_table_ids = [
  private_app_route_table_id,
  isolated_data_route_table_id
]
```

Nghĩa là các subnet dùng route table đó có đường private để gọi S3.

**Interface Endpoint** tạo một ENI trong subnet.

ENI có private IP trong VPC và có security group kiểm soát ai được gọi vào endpoint.

Ví dụ Secrets Manager Interface Endpoint:

```text
App trong private subnet
  -> gọi HTTPS tới private IP của endpoint
  -> Security Group của endpoint kiểm tra rule
  -> endpoint chuyển request tới Secrets Manager
```

#### Security group của Interface Endpoint dùng để làm gì?

Security group của Interface Endpoint trả lời câu hỏi:

```text
Ai trong VPC được gọi vào endpoint?
Được gọi bằng port nào?
```

Ví dụ rule:

```hcl
from_port   = 443
to_port     = 443
protocol    = "tcp"
cidr_blocks = [var.vpc_cidr]
```

Đọc thành câu:

```text
Cho phép IP nội bộ trong VPC gọi HTTPS TCP/443 vào endpoint.
```

Nếu:

```hcl
vpc_cidr = "10.20.0.0/16"
```

thì rule nghĩa là:

```text
Các IP 10.20.x.x trong VPC được gọi endpoint qua port 443.
IP ngoài VPC không được phép gọi endpoint qua rule này.
```

Nội bộ vẫn dùng HTTPS vì AWS API vẫn cần mã hóa và xác thực service. Private network là đường đi riêng hơn, còn HTTPS là lớp bảo vệ dữ liệu.

#### Luồng chi tiết khi app gọi AWS service qua Interface Endpoint

Ví dụ app gọi Secrets Manager:

```text
1. App trong private subnet gọi https://secretsmanager...
2. DNS trong VPC resolve domain đó về private IP của Interface Endpoint.
3. App gửi request TCP/443 tới endpoint.
4. Security group của endpoint kiểm tra ingress:
   - source IP có nằm trong VPC CIDR không?
   - port có phải 443 không?
   - protocol có phải TCP không?
5. Nếu đúng, request được phép đi vào endpoint.
6. Endpoint chuyển request tới AWS Secrets Manager qua mạng AWS.
7. Secrets Manager trả response.
8. Vì security group là stateful, response được cho quay lại app theo connection đã mở.
```

Tóm tắt:

```text
Ingress endpoint = cho app trong VPC gọi vào endpoint
Egress endpoint  = cho endpoint gửi tiếp request hoặc trả response
```

#### S3 Gateway Endpoint trong module VPC hiện tại

Trong module Terraform hiện tại, ta tạo S3 Gateway Endpoint:

```hcl
resource "aws_vpc_endpoint" "s3" {
  vpc_endpoint_type = "Gateway"
}
```

Vì là Gateway Endpoint nên nó không dùng security group. Nó được gắn vào route table của private app subnet và isolated data subnet:

```text
private app route table
isolated data route table
  -> S3 Gateway Endpoint
  -> S3
```

Kết quả là workload trong private/isolated subnet có thể gọi S3 qua endpoint mà không cần đi ra Internet qua NAT Gateway.

## 15. Security Group và Network ACL

### 15.1. Security Group

**Security Group** là firewall ở mức resource, thường gắn với EC2, RDS, Load Balancer.

Đặc điểm:

- Stateful: response traffic tự được cho phép nếu request ban đầu được cho phép.
- Chỉ có allow rule, không có deny rule trực tiếp.
- Áp dụng ở cấp instance hoặc network interface.

Ví dụ:

```text
ALB security group:
  allow inbound 80/443 từ Internet

App security group:
  allow inbound 8080 từ ALB security group

Database security group:
  allow inbound 5432 từ App security group
```

Điểm hay là security group có thể tham chiếu security group khác. Nhờ vậy bạn không cần hard-code IP của app server.

### 15.2. Network ACL

**Network ACL**, thường viết tắt là **NACL**, là firewall ở mức subnet.

Đặc điểm:

- Stateless: inbound và outbound phải được khai báo riêng.
- Có allow và deny.
- Rule có thứ tự ưu tiên.

Người mới thường làm việc với Security Group nhiều hơn. NACL nên dùng cẩn thận vì dễ cấu hình sai làm mất kết nối.

## 16. EC2 là gì?

**EC2**, viết tắt của Elastic Compute Cloud, là dịch vụ máy ảo trên AWS.

Bạn có thể chọn:

- Hệ điều hành.
- Loại CPU/RAM.
- Ổ đĩa.
- Network.
- Security group.
- Key pair SSH.

EC2 phù hợp khi bạn muốn kiểm soát server rõ ràng, ví dụ cài app Spring Boot, Nginx, Docker hoặc agent riêng.

Luồng tạo EC2 đơn giản:

```text
Chọn AMI
  -> chọn instance type
  -> chọn VPC/subnet
  -> gắn security group
  -> gắn key pair hoặc IAM role
  -> launch instance
```

## 17. AMI, Instance Type, Key Pair và User Data

### 17.1. AMI

**AMI**, viết tắt của Amazon Machine Image, là image dùng để tạo EC2 instance.

AMI chứa hệ điều hành và có thể chứa phần mềm đã cài sẵn.

Ví dụ:

- Ubuntu AMI.
- Amazon Linux AMI.
- Windows Server AMI.
- Custom AMI của công ty.

### 17.2. Instance Type

**Instance type** là cấu hình phần cứng ảo của EC2.

Ví dụ:

- `t3.micro`: nhỏ, phù hợp lab hoặc workload nhẹ.
- `m6i.large`: general purpose.
- `c7g.large`: thiên về compute.
- `r7g.large`: thiên về memory.

Tên instance thường có họ máy và kích thước. Người mới không cần thuộc hết, chỉ cần hiểu mỗi loại tối ưu cho workload khác nhau.

### 17.3. Key Pair

**Key Pair** dùng để SSH vào EC2 Linux hoặc lấy password Windows.

Với Linux, bạn dùng private key trên máy mình để SSH:

```bash
ssh -i my-key.pem ec2-user@<public-ip>
```

Không nên commit private key vào Git.

### 17.4. User Data

**User Data** là script chạy khi EC2 khởi động lần đầu.

Ví dụ cài Nginx:

```bash
#!/bin/bash
yum update -y
yum install -y nginx
systemctl enable nginx
systemctl start nginx
```

User Data hữu ích để bootstrap server, nhưng với hệ thống lớn nên dùng image baking, configuration management hoặc container deployment rõ ràng hơn.

## 18. EBS và Instance Store

### 18.1. EBS

**EBS**, viết tắt của Elastic Block Store, là ổ đĩa block gắn với EC2.

Bạn có thể hiểu EBS giống ổ cứng mạng dành cho EC2. Khi stop/start EC2, dữ liệu trên EBS vẫn còn nếu volume chưa bị xóa.

EBS thường dùng cho:

- Root volume của EC2.
- Dữ liệu ứng dụng.
- Disk cho database tự quản lý.

EBS có snapshot để backup.

### 18.2. Instance Store

**Instance Store** là ổ đĩa vật lý tạm thời gắn với host chạy EC2. Nó rất nhanh nhưng dữ liệu có thể mất khi instance stop hoặc bị terminate.

Không nên lưu dữ liệu quan trọng lâu dài trên instance store nếu không có replication hoặc backup.

## 19. Elastic Load Balancing

**Elastic Load Balancing**, thường gọi là **ELB**, phân phối traffic tới nhiều target như EC2, container hoặc IP.

Các loại phổ biến:

- **Application Load Balancer (ALB)**: tầng HTTP/HTTPS, phù hợp web app, API, path routing.
- **Network Load Balancer (NLB)**: tầng TCP/UDP, hiệu năng cao, latency thấp.
- **Gateway Load Balancer (GWLB)**: dùng cho appliance mạng như firewall.

Ví dụ luồng ALB:

```text
User
  -> Route 53
  -> ALB
  -> Target Group
  -> EC2 instances
```

**Target Group** là nhóm backend mà load balancer gửi traffic tới. Health check trong target group giúp ALB chỉ gửi request tới target còn khỏe.

## 20. Auto Scaling Group

**Auto Scaling Group**, thường viết tắt là **ASG**, quản lý số lượng EC2 instance theo cấu hình mong muốn.

ASG có thể:

- Tạo instance mới khi instance cũ hỏng.
- Tăng số lượng instance khi CPU cao.
- Giảm số lượng instance khi tải thấp.
- Phân bổ instance qua nhiều AZ.

Ba con số quan trọng:

- **Minimum capacity**: số instance tối thiểu.
- **Desired capacity**: số instance mong muốn hiện tại.
- **Maximum capacity**: số instance tối đa.

Ví dụ:

```text
min = 2
desired = 3
max = 10
```

ASG thường đi cùng ALB để tạo hệ thống web có khả năng mở rộng.

## 21. S3 là gì?

**S3**, viết tắt của Simple Storage Service, là dịch vụ lưu trữ object.

Object storage khác file system truyền thống. Trong S3, bạn lưu object vào bucket. Mỗi object có key, metadata và nội dung.

Ví dụ:

```text
Bucket: my-app-assets
Object key: images/avatar.png
Content: file ảnh
```

S3 thường dùng để:

- Lưu ảnh, video, file upload.
- Lưu log.
- Lưu backup.
- Host static website.
- Lưu artifact build.
- Làm data lake.

S3 nổi tiếng vì độ bền dữ liệu rất cao, nhưng bạn vẫn cần cấu hình quyền truy cập đúng.

## 22. Bucket, Object, Versioning và Lifecycle

### 21.1. Bucket

**Bucket** là container cấp cao để chứa object trong S3. Tên bucket phải unique toàn cầu trong AWS.

Không nên đặt tên bucket chứa thông tin nhạy cảm nếu không cần, vì tên bucket có thể xuất hiện trong URL hoặc log.

### 21.2. Object

**Object** là dữ liệu được lưu trong S3. Object gồm:

- Key.
- Value/content.
- Metadata.
- Version ID nếu bật versioning.

### 21.3. Versioning

**Versioning** cho phép giữ nhiều phiên bản của cùng một object.

Nếu lỡ ghi đè hoặc xóa file, bạn có thể khôi phục version cũ nếu versioning đã bật.

### 21.4. Lifecycle Policy

**Lifecycle Policy** tự động chuyển hoặc xóa object theo thời gian.

Ví dụ:

```text
Sau 30 ngày -> chuyển log sang storage class rẻ hơn
Sau 365 ngày -> xóa log
```

Lifecycle giúp tiết kiệm chi phí lưu trữ.

## 23. S3 Storage Class

**Storage class** là lớp lưu trữ với giá, độ trễ và mục đích khác nhau.

Một số lớp phổ biến:

| Storage class | Dùng khi nào |
| --- | --- |
| S3 Standard | Dữ liệu truy cập thường xuyên |
| S3 Intelligent-Tiering | Không biết trước tần suất truy cập |
| S3 Standard-IA | Dữ liệu ít truy cập nhưng cần lấy nhanh |
| S3 One Zone-IA | Dữ liệu ít truy cập, chấp nhận lưu ở một AZ |
| S3 Glacier Instant Retrieval | Archive nhưng vẫn cần lấy nhanh |
| S3 Glacier Flexible Retrieval | Archive, lấy chậm hơn |
| S3 Glacier Deep Archive | Archive rất rẻ, lấy rất chậm |

Không nên chọn Glacier chỉ vì rẻ. Cần xét chi phí lấy dữ liệu, thời gian restore và mục đích sử dụng.

## 24. EFS và FSx

### 23.1. EFS

**EFS**, viết tắt của Elastic File System, là file system dùng chung cho nhiều EC2 hoặc container Linux.

EFS phù hợp khi nhiều máy cần đọc/ghi cùng một hệ thống file.

Ví dụ:

```text
EC2 A
EC2 B
EC2 C
  -> cùng mount EFS
```

EFS khác EBS ở chỗ EBS thường gắn cho một EC2 tại một thời điểm, còn EFS được thiết kế để nhiều client dùng chung.

### 23.2. FSx

**FSx** là nhóm dịch vụ file system managed cho các nhu cầu chuyên biệt, ví dụ:

- FSx for Windows File Server.
- FSx for Lustre.
- FSx for NetApp ONTAP.

Người mới thường chỉ cần hiểu S3, EBS và EFS trước.

## 25. RDS là gì?

**RDS**, viết tắt của Relational Database Service, là dịch vụ managed relational database.

RDS hỗ trợ nhiều engine:

- PostgreSQL.
- MySQL.
- MariaDB.
- Oracle.
- SQL Server.
- Amazon Aurora.

RDS giúp bạn đỡ phải tự vận hành database từ đầu. AWS hỗ trợ backup, snapshot, patching, monitoring, Multi-AZ và read replica tùy cấu hình.

Nhưng RDS không tự làm schema tốt hơn, query nhanh hơn hoặc index đúng hơn. Bạn vẫn cần hiểu database.

## 26. RDS Multi-AZ, Read Replica và Backup

### 25.1. Multi-AZ

**Multi-AZ** tạo bản dự phòng đồng bộ ở AZ khác để tăng khả năng chịu lỗi.

Nếu primary database gặp sự cố, RDS có thể failover sang standby.

Multi-AZ chủ yếu phục vụ high availability, không phải để scale read trong mô hình truyền thống.

### 25.2. Read Replica

**Read Replica** là bản sao dùng để đọc. Nó giúp giảm tải đọc cho primary database.

Ví dụ:

```text
Write queries -> primary database
Read queries  -> read replicas
```

Read replica thường replication bất đồng bộ, nên có thể có độ trễ dữ liệu ngắn.

### 25.3. Backup và Snapshot

RDS hỗ trợ automated backup và manual snapshot.

- **Automated backup** dùng để restore tới một thời điểm trong retention period.
- **Manual snapshot** giữ lại cho tới khi bạn xóa.

Backup không có giá trị nếu bạn chưa từng thử restore. Trong production nên định kỳ kiểm tra khôi phục dữ liệu.

## 27. Aurora là gì?

**Amazon Aurora** là database relational do AWS xây dựng, tương thích với MySQL hoặc PostgreSQL.

Aurora thường có khả năng scale, replication và availability tốt hơn RDS MySQL/PostgreSQL truyền thống, nhưng cũng cần hiểu chi phí và giới hạn.

Các khái niệm hay gặp:

- **Aurora cluster**: cụm database.
- **Writer instance**: instance nhận ghi.
- **Reader instance**: instance phục vụ đọc.
- **Cluster endpoint**: endpoint cho writer.
- **Reader endpoint**: endpoint phân phối đọc tới reader.
- **Aurora Serverless**: Aurora tự động scale capacity theo nhu cầu trong một số kiểu workload.

Người mới không cần bắt đầu bằng Aurora. Học RDS PostgreSQL hoặc MySQL trước thường dễ hiểu hơn.

## 28. DynamoDB là gì?

**DynamoDB** là NoSQL database managed của AWS, dạng key-value và document.

DynamoDB phù hợp với workload cần:

- Latency thấp.
- Scale lớn.
- Không muốn quản lý server database.
- Access pattern rõ ràng.

Keyword quan trọng:

- **Table**: bảng.
- **Item**: một dòng dữ liệu.
- **Attribute**: thuộc tính trong item.
- **Partition key**: khóa dùng để phân phối dữ liệu.
- **Sort key**: khóa sắp xếp trong cùng partition.
- **GSI**: Global Secondary Index, index phụ để query theo pattern khác.

Điểm quan trọng: DynamoDB không nên thiết kế như SQL database. Bạn cần biết trước cách query chính, rồi thiết kế key theo access pattern.

## 29. ElastiCache

**ElastiCache** là dịch vụ managed cache. Nó hỗ trợ Redis-compatible engine và Memcached.

Cache dùng để lưu dữ liệu truy cập thường xuyên nhằm giảm tải database và tăng tốc ứng dụng.

Ví dụ:

```text
Request user profile
  -> kiểm tra cache
  -> nếu có: trả ngay
  -> nếu không: đọc database, rồi ghi vào cache
```

Cache không thay thế database chính. Dữ liệu trong cache có thể hết hạn hoặc bị mất. Ứng dụng phải chịu được trường hợp cache miss.

## 30. Lambda là gì?

**AWS Lambda** là dịch vụ chạy function theo mô hình serverless.

Bạn viết code, chọn runtime, cấu hình trigger. AWS lo phần chạy, scale và quản lý server nền.

Lambda phù hợp cho:

- Xử lý event từ S3.
- Xử lý message từ SQS.
- API nhỏ qua API Gateway.
- Job định kỳ.
- Tự động hóa hạ tầng.
- Xử lý file nhẹ.

Ví dụ flow:

```text
User upload file vào S3
  -> S3 event trigger Lambda
  -> Lambda resize ảnh
  -> ghi ảnh đã xử lý sang bucket khác
```

Điểm cần nhớ:

- Lambda có giới hạn thời gian chạy.
- Cold start có thể ảnh hưởng latency.
- Không nên dùng local disk như nơi lưu trữ lâu dài.
- Cần cấu hình IAM role đúng.

## 31. API Gateway

**API Gateway** là dịch vụ tạo và quản lý API endpoint.

API Gateway thường đứng trước Lambda hoặc backend service.

Nó có thể xử lý:

- HTTP routing.
- Authentication/authorization tích hợp.
- Rate limiting.
- Request/response transformation.
- API key.
- Logging.

Ví dụ:

```text
Client
  -> API Gateway
  -> Lambda
  -> DynamoDB
```

API Gateway giúp bạn không phải tự dựng một reverse proxy riêng cho nhiều API serverless đơn giản.

## 32. ECS, ECR và Fargate

### 31.1. ECR

**ECR**, viết tắt của Elastic Container Registry, là Docker registry của AWS.

Bạn build Docker image rồi push vào ECR:

```text
Source code
  -> docker build
  -> push image to ECR
  -> deploy image to ECS/EKS/App Runner
```

### 31.2. ECS

**ECS**, viết tắt của Elastic Container Service, là dịch vụ orchestration container của AWS.

Các khái niệm:

- **Cluster**: nhóm tài nguyên để chạy container.
- **Task Definition**: bản mô tả container chạy thế nào.
- **Task**: instance đang chạy từ task definition.
- **Service**: duy trì số lượng task mong muốn.

### 31.3. Fargate

**Fargate** là chế độ chạy container không cần quản lý EC2 instance.

Nếu ECS dùng EC2 launch type, bạn phải quản lý cụm EC2. Nếu dùng Fargate, bạn chỉ định CPU/RAM cho task và AWS quản lý phần server nền.

Fargate phù hợp khi bạn muốn chạy container nhưng không muốn quản lý node.

## 33. EKS là gì?

**EKS**, viết tắt của Elastic Kubernetes Service, là Kubernetes managed trên AWS.

EKS giúp bạn chạy Kubernetes nhưng AWS quản lý control plane.

Bạn vẫn cần hiểu Kubernetes:

- Pod.
- Deployment.
- Service.
- Ingress.
- ConfigMap.
- Secret.
- Namespace.
- RBAC.
- Node group.

EKS phù hợp khi team đã có nhu cầu Kubernetes rõ ràng. Người mới không nên nhảy vào EKS trước khi hiểu Docker, networking, IAM và Kubernetes cơ bản.

## 34. Route 53 và DNS

**Route 53** là dịch vụ DNS của AWS.

DNS biến tên miền dễ nhớ thành địa chỉ mà máy tính dùng được.

Ví dụ:

```text
api.example.com -> ALB DNS name
```

Các record hay gặp:

- **A record**: trỏ domain tới IPv4.
- **AAAA record**: trỏ domain tới IPv6.
- **CNAME record**: alias từ tên này sang tên khác.
- **MX record**: email server.
- **TXT record**: xác minh domain, SPF, DKIM.
- **Alias record**: record đặc biệt của Route 53 trỏ tới AWS resource như ALB, CloudFront, S3 website.

Route 53 còn hỗ trợ health check và routing policy như latency-based, weighted, failover.

## 35. CloudFront và CDN

**CloudFront** là CDN của AWS.

**CDN**, viết tắt của Content Delivery Network, là mạng phân phối nội dung. CDN cache nội dung ở edge location gần người dùng để giảm latency và giảm tải origin.

Origin có thể là:

- S3 bucket.
- ALB.
- EC2.
- API endpoint.

Ví dụ:

```text
User ở Việt Nam
  -> CloudFront edge gần người dùng
  -> nếu cache hit: trả nội dung ngay
  -> nếu cache miss: lấy từ origin rồi cache lại
```

Keyword:

- **Origin**: nguồn dữ liệu gốc.
- **Distribution**: cấu hình CloudFront.
- **Cache behavior**: quy tắc cache theo path.
- **TTL**: thời gian object được cache.
- **Invalidation**: xóa cache để CloudFront lấy bản mới.

## 36. SQS, SNS và EventBridge

### 35.1. SQS

**SQS**, viết tắt của Simple Queue Service, là message queue.

Queue giúp tách producer và consumer.

```text
Producer gửi message
  -> SQS queue
  -> Consumer lấy message xử lý
```

SQS hữu ích khi xử lý tác vụ nền, chống quá tải và tăng độ bền cho event.

### 35.2. SNS

**SNS**, viết tắt của Simple Notification Service, là dịch vụ publish/subscribe.

Một publisher gửi message tới topic, nhiều subscriber có thể nhận.

Subscriber có thể là:

- Email.
- Lambda.
- SQS.
- HTTP endpoint.

### 35.3. EventBridge

**EventBridge** là event bus giúp route event giữa các service, ứng dụng và SaaS.

Nó phù hợp cho kiến trúc event-driven:

```text
OrderCreated event
  -> EventBridge
  -> update inventory
  -> send email
  -> trigger billing
```

Người mới có thể hiểu nhanh:

- SQS: hàng đợi công việc.
- SNS: phát thông báo tới nhiều nơi.
- EventBridge: bus định tuyến event theo rule.

## 37. CloudWatch

**CloudWatch** là dịch vụ quan sát hệ thống gồm metrics, logs, alarms và dashboard.

CloudWatch dùng để trả lời:

- CPU EC2 có cao không?
- Lambda lỗi bao nhiêu lần?
- Log ứng dụng báo lỗi gì?
- RDS còn dung lượng không?
- Có cần gửi alert không?

Thành phần chính:

- **Metrics**: số liệu theo thời gian, ví dụ CPUUtilization.
- **Logs**: log text từ app hoặc service.
- **Alarms**: cảnh báo khi metric vượt ngưỡng.
- **Dashboards**: bảng theo dõi.

Ví dụ alarm:

```text
Nếu CPU EC2 > 80% trong 5 phút
  -> gửi thông báo qua SNS
```

## 38. CloudTrail

**CloudTrail** ghi lại hoạt động API trong AWS account.

Nếu ai đó tạo EC2, xóa S3 object, sửa IAM policy hoặc thay security group, CloudTrail có thể ghi nhận sự kiện đó.

CloudTrail giúp trả lời:

- Ai đã làm hành động này?
- Làm lúc nào?
- Từ IP nào?
- Gọi API nào?
- Thành công hay thất bại?

CloudTrail rất quan trọng cho audit và điều tra sự cố bảo mật.

## 39. KMS và Secrets Manager

### 38.1. KMS

**KMS**, viết tắt của Key Management Service, là dịch vụ quản lý khóa mã hóa.

KMS giúp mã hóa dữ liệu trong nhiều dịch vụ AWS như S3, EBS, RDS, Lambda environment variables.

Keyword:

- **KMS key**: khóa dùng để mã hóa/giải mã.
- **Encryption at rest**: mã hóa dữ liệu khi lưu trên disk/storage.
- **Encryption in transit**: mã hóa dữ liệu khi truyền qua mạng, thường bằng TLS.

### 38.2. Secrets Manager

**Secrets Manager** dùng để lưu secret như password database, API key, token.

Lợi ích:

- Không hard-code secret trong source code.
- Có phân quyền bằng IAM.
- Có thể rotate secret tự động với một số loại database.
- Có audit qua CloudTrail.

Không nên lưu secret trong Git, Docker image, AMI hoặc log.

## 40. WAF, Shield và GuardDuty

### 39.1. WAF

**WAF**, viết tắt của Web Application Firewall, bảo vệ ứng dụng web khỏi một số kiểu request xấu.

WAF có thể chặn:

- IP đáng ngờ.
- Request theo pattern.
- Một số kiểu SQL injection hoặc XSS phổ biến.
- Traffic vượt rate limit.

WAF thường gắn trước CloudFront, ALB hoặc API Gateway.

### 39.2. Shield

**AWS Shield** là dịch vụ bảo vệ DDoS.

**DDoS** là tấn công làm nghẽn hệ thống bằng lượng request hoặc traffic lớn.

AWS Shield Standard được bật mặc định cho nhiều dịch vụ. Shield Advanced là gói cao hơn có thêm bảo vệ và hỗ trợ.

### 39.3. GuardDuty

**GuardDuty** là dịch vụ phát hiện mối đe dọa. Nó phân tích log như CloudTrail, VPC Flow Logs, DNS logs để tìm hành vi bất thường.

Ví dụ:

- Access key bị dùng từ vị trí lạ.
- EC2 giao tiếp với domain độc hại.
- Hoạt động quét port bất thường.

GuardDuty không thay thế việc cấu hình bảo mật đúng, nhưng là lớp phát hiện rất hữu ích.

## 41. AWS CLI và SDK

### 40.1. AWS CLI

**AWS CLI** là công cụ dòng lệnh để thao tác với AWS.

Ví dụ:

```bash
aws s3 ls
aws ec2 describe-instances
aws sts get-caller-identity
```

Lệnh `aws sts get-caller-identity` rất hữu ích để kiểm tra bạn đang dùng identity nào.

### 40.2. AWS SDK

**AWS SDK** là thư viện để code gọi AWS API.

Ví dụ:

- Java: AWS SDK for Java.
- JavaScript/TypeScript: AWS SDK for JavaScript.
- Python: boto3.
- Go: AWS SDK for Go.

Ứng dụng Spring Boot có thể dùng SDK để upload file lên S3, gửi message vào SQS hoặc đọc secret từ Secrets Manager.

## 42. Infrastructure as Code

**Infrastructure as Code**, viết tắt là **IaC**, là cách quản lý hạ tầng bằng file code thay vì click tay.

Lợi ích:

- Tái tạo hạ tầng dễ hơn.
- Review thay đổi qua pull request.
- Giảm lỗi thao tác thủ công.
- Có lịch sử thay đổi trong Git.
- Tạo dev/staging/prod nhất quán hơn.

Công cụ phổ biến:

- Terraform.
- AWS CloudFormation.
- AWS CDK.

Ví dụ Terraform đơn giản:

```hcl
resource "aws_s3_bucket" "app_logs" {
  bucket = "my-app-logs-example"
}
```

Nguyên tắc quan trọng:

- Không commit secret vào code IaC.
- Bảo vệ state file.
- Luôn review plan trước khi apply.
- Tách environment rõ ràng.

## 43. CI/CD với AWS

Một pipeline deploy lên AWS thường có flow:

```text
Developer push code
  -> CI chạy test
  -> build artifact hoặc Docker image
  -> push image vào ECR
  -> deploy lên ECS/EKS/EC2/Lambda
  -> chạy smoke test
  -> gửi thông báo
```

Các dịch vụ AWS liên quan:

- **CodeBuild**: chạy build/test.
- **CodePipeline**: điều phối pipeline.
- **CodeDeploy**: deploy lên EC2, Lambda hoặc ECS.
- **ECR**: lưu Docker image.
- **S3**: lưu artifact.

Bạn không bắt buộc dùng bộ Code* của AWS. GitHub Actions, GitLab CI hoặc Jenkins cũng có thể deploy lên AWS.

Best practice quan trọng: dùng OIDC hoặc IAM role tạm thời cho CI/CD thay vì lưu access key dài hạn trong secret.

## 44. Kiến trúc web app cơ bản trên AWS

Một kiến trúc web app truyền thống:

```text
User
  -> Route 53
  -> CloudFront
  -> AWS WAF
  -> Application Load Balancer
  -> ECS service hoặc EC2 Auto Scaling Group
  -> RDS database
  -> S3 lưu file upload
  -> CloudWatch logs/metrics
```

Giải thích:

- Route 53 quản lý domain.
- CloudFront cache nội dung tĩnh.
- WAF lọc request xấu.
- ALB phân phối traffic.
- ECS/EC2 chạy ứng dụng.
- RDS lưu dữ liệu quan hệ.
- S3 lưu file.
- CloudWatch theo dõi log và metrics.

Không phải dự án nào cũng cần đủ thành phần này. Dự án nhỏ có thể bắt đầu đơn giản hơn rồi mở rộng dần.

## 45. Kiến trúc serverless cơ bản

Ví dụ API serverless:

```text
Client
  -> API Gateway
  -> Lambda
  -> DynamoDB
  -> CloudWatch Logs
```

Ví dụ xử lý file:

```text
User upload file
  -> S3
  -> Lambda xử lý
  -> ghi kết quả vào S3 hoặc DynamoDB
```

Serverless phù hợp khi:

- Traffic không đều.
- Muốn giảm vận hành server.
- Tác vụ ngắn.
- Kiến trúc event-driven.

Serverless không phù hợp cho mọi thứ. Nếu workload chạy liên tục, cần kết nối lâu, cần kiểm soát runtime sâu hoặc có yêu cầu latency rất ổn định, container hoặc EC2 có thể hợp hơn.

## 46. Well-Architected Framework

**AWS Well-Architected Framework** là bộ nguyên tắc giúp thiết kế hệ thống tốt hơn trên AWS.

Các trụ cột chính:

| Trụ cột | Ý nghĩa |
| --- | --- |
| Operational Excellence | Vận hành, tự động hóa, cải tiến quy trình |
| Security | Bảo vệ dữ liệu, phân quyền, phát hiện rủi ro |
| Reliability | Chịu lỗi, khôi phục, HA |
| Performance Efficiency | Dùng tài nguyên phù hợp và hiệu quả |
| Cost Optimization | Tối ưu chi phí |
| Sustainability | Giảm lãng phí tài nguyên |

Người mới không cần thuộc lòng, nhưng nên dùng các trụ cột này làm checklist khi thiết kế hệ thống.

## 47. Shared Responsibility Model

**Shared Responsibility Model** là mô hình chia sẻ trách nhiệm giữa AWS và khách hàng.

AWS chịu trách nhiệm **security of the cloud**:

- Data center.
- Phần cứng.
- Hạ tầng mạng vật lý.
- Nền tảng dịch vụ.

Khách hàng chịu trách nhiệm **security in the cloud**:

- IAM.
- Dữ liệu.
- Cấu hình network.
- Security group.
- Patch hệ điều hành nếu dùng EC2.
- Mã hóa.
- Cấu hình ứng dụng.

Ví dụ:

- Với EC2, bạn phải patch OS.
- Với RDS, AWS quản lý nhiều phần OS/database engine hơn, nhưng bạn vẫn phải quản lý user, password, network, backup policy và schema.
- Với S3, AWS giữ hạ tầng bền vững, nhưng bạn phải cấu hình bucket policy đúng.

## 48. Pricing và quản lý chi phí

AWS tính tiền theo nhiều cách:

- Theo giờ hoặc giây chạy.
- Theo số request.
- Theo GB lưu trữ.
- Theo GB truyền dữ liệu.
- Theo số resource được tạo.
- Theo provisioned capacity.

Các nguồn chi phí hay bị quên:

- NAT Gateway.
- Load Balancer chạy liên tục.
- EBS volume không dùng.
- Snapshot cũ.
- Data transfer ra Internet.
- CloudWatch log lưu quá lâu.
- RDS instance quên tắt.
- Elastic IP không gắn với instance.

Việc nên làm:

- Bật Billing Alarm.
- Dùng AWS Budgets.
- Gắn tag cho resource.
- Xóa resource lab sau khi học.
- Đặt lifecycle cho S3 log.
- Review Cost Explorer định kỳ.

## 49. Tagging

**Tag** là cặp key-value gắn vào resource.

Ví dụ:

```text
Environment = dev
Project     = springboot-learning
Owner       = tony
CostCenter  = learning
```

Tag giúp:

- Tìm resource.
- Phân bổ chi phí.
- Tự động hóa.
- Áp policy.
- Dọn resource lab.

Nên đặt chuẩn tag từ sớm, đặc biệt khi có nhiều environment.

## 50. Backup, Disaster Recovery và RTO/RPO

### 49.1. Backup

**Backup** là bản sao dữ liệu để khôi phục khi mất hoặc hỏng dữ liệu.

Ví dụ:

- RDS automated backup.
- RDS snapshot.
- EBS snapshot.
- S3 versioning.

### 49.2. Disaster Recovery

**Disaster Recovery**, viết tắt là **DR**, là kế hoạch khôi phục khi sự cố lớn xảy ra.

Ví dụ sự cố:

- Một AZ lỗi.
- Một region gặp vấn đề.
- Database bị xóa nhầm.
- Dữ liệu bị ghi sai hàng loạt.

### 49.3. RTO và RPO

**RTO**, Recovery Time Objective, là thời gian tối đa chấp nhận để khôi phục hệ thống.

**RPO**, Recovery Point Objective, là lượng dữ liệu tối đa chấp nhận mất.

Ví dụ:

```text
RTO = 1 giờ
  -> hệ thống phải khôi phục trong vòng 1 giờ

RPO = 15 phút
  -> chấp nhận mất tối đa 15 phút dữ liệu
```

RTO/RPO càng thấp thì chi phí và độ phức tạp thường càng cao.

## 51. Các hiểu lầm phổ biến

### "Đưa lên AWS là tự động bảo mật"

Không đúng. AWS cung cấp công cụ bảo mật, nhưng bạn vẫn phải cấu hình đúng IAM, network, encryption, logging và ứng dụng.

### "Dùng cloud luôn rẻ hơn"

Không luôn đúng. Cloud rẻ khi dùng linh hoạt, scale đúng và dọn tài nguyên tốt. Nếu tạo resource rồi bỏ quên, cloud có thể rất đắt.

### "Private subnet là an toàn tuyệt đối"

Không. Private subnet chỉ giảm việc truy cập trực tiếp từ Internet. Nếu IAM, app, dependency hoặc outbound traffic cấu hình sai, hệ thống vẫn có thể bị tấn công.

### "Security Group mở 0.0.0.0/0 lúc nào cũng sai"

Không hẳn. Với ALB public, mở 80/443 từ Internet là bình thường. Nhưng mở SSH, RDP, database port từ `0.0.0.0/0` thường là nguy hiểm.

### "S3 bucket private thì không cần quan tâm nữa"

Sai. Bạn vẫn cần kiểm tra bucket policy, ACL, public access block, encryption, versioning, lifecycle và log truy cập nếu dữ liệu quan trọng.

### "Serverless không cần vận hành"

Sai. Serverless giảm vận hành server, nhưng vẫn cần monitoring, timeout, retry, IAM, cost control, concurrency, error handling và deployment strategy.

## 52. Best practices nên học sớm

- Bật MFA cho root user.
- Không dùng root user cho công việc hằng ngày.
- Không commit access key vào Git.
- Dùng IAM role thay vì long-lived access key khi có thể.
- Cấp quyền theo least privilege.
- Đặt resource quan trọng trong private subnet.
- Chỉ mở port thật sự cần.
- Dùng security group tham chiếu nhau thay vì mở IP rộng.
- Bật CloudTrail.
- Gửi log quan trọng vào CloudWatch hoặc hệ thống logging.
- Mã hóa dữ liệu nhạy cảm at rest và in transit.
- Bật backup cho database.
- Thử restore backup định kỳ.
- Gắn tag cho resource.
- Dùng IaC cho hạ tầng quan trọng.
- Tạo billing alarm.
- Xóa tài nguyên lab sau khi học.
- Không dùng production account để thử nghiệm bừa bãi.

## 53. Thứ tự học AWS cho người mới

Nên học theo thứ tự:

1. Tổng quan cloud, region, AZ, account.
2. IAM: user, group, role, policy, MFA.
3. VPC: CIDR, subnet, route table, IGW, NAT, security group.
4. EC2: AMI, instance type, EBS, key pair, user data.
5. S3: bucket, object, policy, versioning, lifecycle.
6. RDS: database managed, backup, Multi-AZ, read replica.
7. Load Balancer và Auto Scaling.
8. Route 53 và CloudFront.
9. CloudWatch và CloudTrail.
10. Lambda, API Gateway, SQS, SNS, EventBridge.
11. ECS, ECR, Fargate.
12. IaC bằng Terraform hoặc CloudFormation.
13. Security nâng cao: KMS, Secrets Manager, WAF, GuardDuty.
14. Kiến trúc HA, cost optimization, disaster recovery.
15. EKS nếu đã có nền Docker và Kubernetes.

Không nên học bằng cách mở danh sách tất cả dịch vụ AWS rồi cố nhớ tên. Hãy học theo bài toán thực tế.

## 54. Bài lab thực hành đề xuất

### Lab 1: Account an toàn tối thiểu

- Bật MFA cho root user.
- Tạo IAM admin user hoặc dùng IAM Identity Center.
- Tạo billing alarm.
- Chạy `aws sts get-caller-identity`.

### Lab 2: EC2 web server

- Tạo VPC hoặc dùng default VPC để học ban đầu.
- Tạo EC2 Amazon Linux.
- Mở port 22 cho IP cá nhân.
- Mở port 80 cho Internet.
- Cài Nginx bằng user data.
- Truy cập web qua public IP.

### Lab 3: S3 static website hoặc file storage

- Tạo S3 bucket.
- Upload file.
- Bật versioning.
- Cấu hình lifecycle.
- Thử policy public cho file tĩnh nếu hiểu rõ rủi ro.

### Lab 4: Web app với ALB và Auto Scaling

- Tạo Launch Template.
- Tạo Auto Scaling Group ở nhiều AZ.
- Tạo ALB.
- Cấu hình Target Group health check.
- Tăng giảm desired capacity.

### Lab 5: RDS private

- Tạo RDS PostgreSQL trong private subnet.
- Chỉ cho app security group truy cập database port.
- Bật automated backup.
- Tạo snapshot thủ công.
- Thử restore snapshot sang instance mới.

### Lab 6: Container với ECS Fargate

- Build Docker image.
- Push image vào ECR.
- Tạo ECS task definition.
- Tạo ECS service chạy Fargate.
- Đặt service sau ALB.
- Xem log trong CloudWatch.

### Lab 7: Serverless API

- Tạo Lambda.
- Tạo API Gateway.
- Lambda ghi/đọc DynamoDB.
- Xem log lỗi trong CloudWatch.
- Cấu hình IAM role tối thiểu.

### Lab 8: Terraform cơ bản

- Viết Terraform tạo S3 bucket.
- Viết Terraform tạo VPC đơn giản.
- Chạy `terraform plan`.
- Chạy `terraform apply`.
- Chạy `terraform destroy`.

## 55. Checklist AWS cơ bản

Một người mới học AWS nên nắm được:

- AWS account khác IAM user như thế nào.
- Vì sao root user cần MFA.
- IAM role khác IAM user như thế nào.
- Policy JSON gồm `Effect`, `Action`, `Resource`.
- Region và AZ khác nhau thế nào.
- VPC, subnet, route table dùng để làm gì.
- Public subnet và private subnet khác nhau thế nào.
- Security Group khác NACL thế nào.
- EC2 là gì và dùng khi nào.
- EBS là gì và vì sao cần snapshot.
- ALB dùng để làm gì.
- Auto Scaling Group hoạt động ra sao.
- S3 bucket/object là gì.
- RDS khác tự cài database trên EC2 thế nào.
- CloudWatch và CloudTrail khác nhau thế nào.
- KMS và Secrets Manager dùng để bảo vệ gì.
- NAT Gateway có thể tốn tiền nếu quên.
- Tag giúp quản lý resource và chi phí.

## 56. Checklist AWS nâng cao hơn

Khi đã qua cơ bản, nên học tiếp:

- Multi-account strategy với AWS Organizations.
- IAM Identity Center.
- Permission boundary và Service Control Policy.
- PrivateLink.
- Transit Gateway.
- Centralized logging.
- Cross-region backup.
- Blue/green deployment.
- Canary deployment.
- ECS capacity provider.
- EKS IRSA hoặc Pod Identity.
- RDS parameter group và performance tuning.
- DynamoDB single-table design.
- CloudFront cache policy và origin access control.
- WAF managed rules.
- GuardDuty, Security Hub, Inspector.
- Cost allocation tag và Savings Plans.
- Disaster recovery strategy.

## 57. AWS cho Spring Boot

Một ứng dụng Spring Boot phổ biến có thể dùng AWS như sau:

```text
Spring Boot app
  -> chạy trên ECS Fargate hoặc EC2
  -> image lưu ở ECR
  -> database PostgreSQL trên RDS
  -> file upload lưu ở S3
  -> secret database lưu ở Secrets Manager
  -> log gửi CloudWatch
  -> domain quản lý bằng Route 53
  -> HTTPS qua ACM + ALB
```

Các keyword cần hiểu:

- **ACM**: AWS Certificate Manager, quản lý TLS certificate.
- **TLS certificate**: chứng chỉ giúp bật HTTPS.
- **Environment variable**: biến cấu hình truyền vào app.
- **Connection string**: chuỗi kết nối database.
- **Health check endpoint**: API như `/actuator/health` để ALB biết app còn khỏe không.

Với Spring Boot, nên bật Actuator cho health check và metrics, nhưng cần cấu hình bảo mật endpoint cẩn thận.

## 58. Một kiến trúc production tham khảo

```text
Route 53
  -> CloudFront
  -> WAF
  -> ALB public subnets
  -> ECS Fargate service private subnets
  -> RDS PostgreSQL private subnets
  -> ElastiCache Redis private subnets
  -> S3 for uploads/backups
  -> Secrets Manager for credentials
  -> CloudWatch for logs/metrics/alarms
  -> CloudTrail for audit
```

Đặc điểm:

- Chỉ ALB/CloudFront nhận traffic public.
- App chạy trong private subnet.
- Database không public.
- Secret không nằm trong source code.
- Log và audit được bật.
- Có alarm cho lỗi và tài nguyên.
- Có backup và restore plan.

Đây không phải công thức bắt buộc cho mọi hệ thống, nhưng là khung tư duy tốt.

## 59. Câu hỏi thiết kế AWS cho một hệ thống

Trước khi tạo resource, nên hỏi:

- Người dùng ở khu vực nào?
- Chọn region nào để latency tốt và phù hợp pháp lý?
- Hệ thống cần HA tới mức nào?
- RTO/RPO là bao nhiêu?
- Traffic dự kiến là bao nhiêu?
- Workload chạy liên tục hay theo event?
- Dùng EC2, ECS, EKS hay Lambda?
- Dữ liệu lưu ở đâu: RDS, DynamoDB, S3 hay kết hợp?
- Resource nào cần public, resource nào phải private?
- Ai được deploy?
- CI/CD lấy quyền AWS bằng cách nào?
- Secret lưu ở đâu?
- Log và metrics xem ở đâu?
- Backup restore ra sao?
- Chi phí tối đa mỗi tháng là bao nhiêu?
- Khi lỗi production thì ai nhận alert?

Trả lời được các câu hỏi này giúp bạn tránh tạo hạ tầng theo cảm tính.

## 60. Tóm tắt

AWS là nền tảng cloud lớn gồm nhiều dịch vụ cho compute, storage, database, networking, security, observability và deployment. Để học AWS hiệu quả, không nên bắt đầu bằng việc học thuộc tên dịch vụ. Hãy bắt đầu từ các khái niệm nền: account, region, AZ, IAM, VPC, EC2, S3, RDS, Load Balancer, Auto Scaling, CloudWatch và CloudTrail.

Tư duy quan trọng nhất khi học AWS là hiểu mỗi dịch vụ giải quyết bài toán gì, dịch vụ đó đứng ở đâu trong kiến trúc, ai có quyền truy cập, dữ liệu đi qua đâu, log nằm ở đâu, backup ra sao và chi phí phát sinh như thế nào.

Nếu nắm chắc IAM, networking, compute, storage, database và monitoring, bạn sẽ có nền đủ tốt để đi tiếp sang ECS, EKS, serverless, security nâng cao, Terraform và kiến trúc production.
