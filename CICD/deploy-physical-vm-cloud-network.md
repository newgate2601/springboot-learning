# Khi nào deploy lên server vật lý, VM, cloud và network nên thiết kế thế nào

## 1. Mục tiêu tài liệu

Tài liệu này giải thích một câu hỏi rất thực tế:

```text
Khi nào nên deploy trực tiếp lên server vật lý?
Khi nào nên tách server vật lý thành nhiều VM?
Khi nào nên dùng cloud?
Staging, UAT, production nên chung mạng hay tách mạng?
Doanh nghiệp ví điện tử, fintech, bank thường triển khai thế nào?
```

Tài liệu này viết cho người mới học DevOps.
Vì vậy sẽ không đi theo kiểu quá học thuật.
Mục tiêu là đọc xong bạn hình dung được bức tranh thật trong doanh nghiệp.

Điều quan trọng nhất cần nhớ:

```text
server vật lý là phần cứng
VM là máy ảo chạy trên phần cứng
cloud là cách thuê hạ tầng theo dịch vụ
container/pod là cách chạy app hiện đại
Kubernetes cluster là cụm để quản lý nhiều container/pod
network là đường đi và hàng rào giữa các hệ thống
```

---

# 2. Các thuật ngữ cần hiểu trước

## 2.1. Server vật lý là gì?

`server vật lý` là máy chủ thật.
Nó có CPU, RAM, disk, card mạng, nguồn điện, mainboard như một máy tính rất mạnh.

Ví dụ:

```text
Dell PowerEdge
HP ProLiant
Lenovo ThinkSystem
Supermicro server
```

Nếu ví hệ thống là một tòa nhà, server vật lý giống như mảnh đất và khung nhà thật.

Trên server vật lý có thể chạy:

```text
Linux trực tiếp
Windows Server trực tiếp
hypervisor để tạo VM
database
Kubernetes node
storage system
appliance bảo mật
```

## 2.2. Bare metal là gì?

`bare metal` nghĩa là chạy trực tiếp trên server vật lý, không qua lớp VM.

Ví dụ:

```text
server vật lý
  -> cài Ubuntu
  -> chạy PostgreSQL trực tiếp
```

Hoặc:

```text
server vật lý
  -> cài Ubuntu
  -> chạy Java service trực tiếp bằng systemd
```

Ưu điểm:

- Ít lớp trung gian.
- Hiệu năng tốt.
- Dễ tận dụng hết CPU/RAM/disk.
- Phù hợp workload rất nặng hoặc rất nhạy về độ trễ.

Nhược điểm:

- Khó chia nhỏ tài nguyên.
- Khó cô lập nhiều hệ thống.
- Khó cấp phát linh hoạt cho nhiều team.
- Nếu cài nhiều thứ trực tiếp lên cùng máy, rất dễ rối.
- Thay đổi, backup, migrate, rebuild thường nặng hơn VM/cloud.

## 2.3. Hypervisor là gì?

`hypervisor` là phần mềm hoặc nền tảng dùng để tạo và quản lý VM.

Nói dễ hiểu:

```text
server vật lý
  -> hypervisor
      -> VM 1
      -> VM 2
      -> VM 3
```

Các hypervisor hay gặp:

```text
VMware ESXi
KVM
Hyper-V
Proxmox
Nutanix AHV
OpenStack Nova dùng KVM bên dưới
```

Hypervisor giống người chia một server vật lý lớn thành nhiều máy nhỏ hơn.

## 2.4. VM là gì?

`VM` là virtual machine, tức máy ảo.

Một VM nhìn từ bên trong gần giống một máy thật:

```text
có CPU ảo
có RAM
có disk
có card mạng
có hệ điều hành riêng
```

Ví dụ:

```text
VM uat-worker-1:
  4 vCPU
  8 GB RAM
  100 GB disk
  Ubuntu 22.04
```

VM giúp chia một server vật lý lớn thành nhiều máy nhỏ, dễ quản lý hơn.

## 2.5. VPS là gì?

`VPS` cũng là một dạng VM, thường được nhà cung cấp bán sẵn.

Khi thuê VPS, bạn không quản server vật lý bên dưới.
Bạn chỉ nhận một máy ảo có IP, CPU, RAM, disk.

Trong tài liệu học Kubernetes, ta hay gọi:

```text
1 VPS = 1 node
```

Điều này đúng ở mức thực hành.
Trong doanh nghiệp, từ chính xác hơn thường là:

```text
1 VM = 1 node
```

## 2.6. Cloud là gì?

`cloud` là mô hình thuê hạ tầng và dịch vụ qua nhà cung cấp.

Ví dụ:

```text
AWS
Azure
Google Cloud
Oracle Cloud
Viettel Cloud
VNPT Cloud
FPT Cloud
```

Cloud không chỉ là thuê VM.
Cloud còn có nhiều dịch vụ sẵn:

```text
VM
managed Kubernetes
managed database
load balancer
object storage
secret manager
monitoring
IAM
firewall/security group
backup
WAF
CDN
```

Nói dễ hiểu:

```text
on-prem:
  công ty tự mua đất, xây nhà, kéo điện, thuê bảo vệ

cloud:
  công ty thuê tòa nhà đã có sẵn nhiều dịch vụ
```

## 2.7. On-prem là gì?

`on-prem` hoặc `on-premises` nghĩa là hạ tầng nằm trong data center của công ty hoặc data center thuê chỗ đặt máy.

Doanh nghiệp tự chịu nhiều việc hơn:

```text
mua server
lắp server
quản điện
quản cooling
quản network
quản storage
quản hypervisor
quản backup
quản disaster recovery
```

Bank và fintech lớn thường có on-prem vì:

- Quy định pháp lý.
- Dữ liệu nhạy cảm.
- Kiểm soát hạ tầng.
- Kết nối với hệ thống core banking, switch, payment gateway nội bộ.
- Lịch sử hệ thống đã có từ lâu.

## 2.8. Container là gì?

`container` là cách đóng gói app cùng môi trường chạy của nó.

Ví dụ một Spring Boot service:

```text
source code
  -> build JAR
  -> build Docker image
  -> chạy thành container
```

Container nhẹ hơn VM vì container không mang theo cả hệ điều hành riêng.
Nhiều container có thể chạy trên một VM.

## 2.9. Kubernetes node là gì?

`node` là máy tham gia Kubernetes cluster.
Node có thể là:

```text
VM
VPS
server vật lý
cloud instance
```

Kubernetes node có hai nhóm chính:

```text
control-plane:
  điều khiển cluster

worker:
  chạy app thật
```

App như `gateway`, `uaa-service`, `post-service`, `wallet-service` thường chạy trên worker.

## 2.10. Pod là gì?

`pod` là đơn vị chạy nhỏ nhất trong Kubernetes.
Thường một pod chứa một container app.

Ví dụ:

```text
uaa-service replicas=3
```

Nghĩa là Kubernetes chạy 3 pod của `uaa-service`.

Không nên hiểu:

```text
1 service = 1 VM
```

Trong Kubernetes thường là:

```text
1 service = 1 Deployment
1 Deployment = nhiều pod
nhiều pod được rải lên nhiều worker node
```

---

# 3. Có mấy kiểu deploy chính?

## 3.1. Deploy trực tiếp lên server vật lý

Mô hình:

```text
server vật lý
  -> Linux
  -> app chạy trực tiếp
```

Ví dụ:

```text
payment-batch.jar chạy bằng systemd
PostgreSQL chạy trực tiếp
Nginx chạy trực tiếp
Kafka chạy trực tiếp
```

Đây là cách rất gần phần cứng.
Ít lớp trung gian.

## 3.2. Deploy lên VM

Mô hình:

```text
server vật lý
  -> hypervisor
      -> VM app-1
      -> VM app-2
      -> VM db-1
```

Mỗi VM giống một máy riêng.
Có thể cài app, database, middleware hoặc Kubernetes node trên VM.

## 3.3. Deploy container trên VM

Mô hình:

```text
server vật lý
  -> hypervisor
      -> VM
          -> Docker/containerd
              -> container app
```

Đây là bước chuyển từ app cài tay sang app đóng gói bằng container.

## 3.4. Deploy Kubernetes trên VM

Mô hình phổ biến trong doanh nghiệp:

```text
server vật lý
  -> hypervisor/private cloud
      -> nhiều VM
          -> Kubernetes cluster
              -> pod app
```

Ví dụ:

```text
prod-cp-1 VM
prod-cp-2 VM
prod-cp-3 VM
prod-worker-1 VM
prod-worker-2 VM
prod-worker-3 VM
```

App chạy thành pod:

```text
gateway pod
uaa-service pod
post-service pod
wallet-service pod
payment-service pod
```

## 3.5. Deploy lên cloud VM

Mô hình:

```text
cloud provider
  -> EC2/VM/cloud instance
      -> app hoặc Kubernetes node
```

Ví dụ:

```text
AWS EC2
Azure VM
Google Compute Engine
Viettel Cloud VM
```

Bạn không quản server vật lý.
Bạn quản VM và phần mềm trên VM.

## 3.6. Deploy lên managed Kubernetes

Mô hình:

```text
cloud provider
  -> managed Kubernetes
      -> worker node
          -> pod app
```

Ví dụ:

```text
AWS EKS
Azure AKS
Google GKE
Oracle OKE
```

Cloud provider quản một phần control-plane.
Team DevOps tập trung hơn vào app, node pool, network, security, deploy.

## 3.7. Deploy lên managed service

Mô hình:

```text
cloud provider
  -> managed database
  -> managed cache
  -> managed message queue
  -> managed object storage
```

Ví dụ:

```text
PostgreSQL managed
Redis managed
Kafka managed
S3/object storage
Cloud secret manager
Cloud monitoring
```

Bạn dùng dịch vụ thay vì tự cài từ đầu.

---

# 4. Khi nào deploy trực tiếp lên server vật lý?

Deploy trực tiếp lên server vật lý nên dùng khi bạn có lý do rõ ràng.
Không nên dùng chỉ vì "máy đang rảnh".

## 4.1. Khi cần hiệu năng rất sát phần cứng

Một số workload cần latency thấp hoặc throughput rất cao.

Ví dụ:

```text
database cực lớn
high-performance trading
payment switch latency rất nhạy
storage system
network appliance
log ingestion cực nặng
Kafka cluster rất lớn
```

Bare metal giúp giảm lớp trung gian.
Không phải lúc nào cũng nhanh hơn nhiều, nhưng có lợi khi workload rất nặng.

## 4.2. Khi workload cần phần cứng đặc biệt

Ví dụ:

```text
HSM
GPU
FPGA
card mạng tốc độ cao
NVMe local disk rất nhanh
storage controller đặc biệt
```

`HSM` là Hardware Security Module.
Trong bank/fintech, HSM dùng để bảo vệ key mã hóa, PIN, ký giao dịch hoặc xử lý nghiệp vụ bảo mật cao.

Những phần như HSM thường không deploy như app bình thường.
Nó có thể là thiết bị riêng hoặc server có card riêng.

## 4.3. Khi database cần máy riêng rất mạnh

Database production lớn đôi khi chạy tốt hơn trên server vật lý riêng.

Ví dụ:

```text
Oracle RAC
PostgreSQL cực lớn
SQL Server Enterprise
core banking database
ledger database
transaction database
```

Lý do:

- Cần IOPS cao.
- Cần RAM lớn.
- Cần cấu hình storage nghiêm túc.
- Cần kiểm soát backup/restore.
- Cần kiểm soát replication.
- Cần tối ưu hệ điều hành và kernel.

## 4.4. Khi có yêu cầu pháp lý hoặc chính sách nội bộ

Một số bank hoặc fintech có quy định:

```text
dữ liệu giao dịch phải nằm trong data center được phê duyệt
không được đưa database production lên public cloud
key bảo mật phải nằm trong HSM nội bộ
một số hệ thống phải chạy trong vùng mạng riêng
```

Khi đó on-prem hoặc bare metal có thể là lựa chọn bắt buộc.

## 4.5. Khi hệ thống legacy chưa container hóa được

`legacy` là hệ thống cũ nhưng vẫn đang chạy nghiệp vụ thật.

Ví dụ:

```text
ứng dụng cũ chạy trên WebLogic
ứng dụng cũ cần thư viện hệ điều hành đặc biệt
ứng dụng cũ gắn với license theo hardware
ứng dụng cũ deploy bằng script nhiều năm
```

Không phải hệ thống nào cũng đưa ngay lên Kubernetes được.
Nhiều doanh nghiệp vẫn giữ một số app chạy trực tiếp trên server hoặc VM.

## 4.6. Khi nào không nên deploy trực tiếp lên server vật lý?

Không nên deploy trực tiếp lên server vật lý nếu:

```text
nhiều team cùng dùng một máy
cần tạo/xóa môi trường thường xuyên
cần rollback nhanh
cần scale linh hoạt
cần cô lập staging/UAT/prod
cần chuẩn hóa bằng CI/CD
app là microservice stateless bình thường
```

Với app Spring Boot bình thường như:

```text
gateway
uaa-service
post-service
wallet-service
notification-service
```

Trong doanh nghiệp hiện đại, thường không deploy trực tiếp từng service lên từng server vật lý.
Thường sẽ đi qua VM, container hoặc Kubernetes.

---

# 5. Khi nào nên tách server vật lý thành nhiều VM?

Đây là mô hình rất phổ biến trong doanh nghiệp.

## 5.1. Khi muốn chia tài nguyên rõ ràng

Một server vật lý có thể rất mạnh:

```text
64 CPU core
512 GB RAM
10 TB disk
```

Thay vì cài tất cả vào một hệ điều hành, ta chia thành VM:

```text
VM 1: GitLab
VM 2: Jenkins
VM 3: Nexus/Harbor
VM 4: UAT worker
VM 5: Prod worker
VM 6: Monitoring
```

Mỗi VM có CPU/RAM/disk riêng.
Nếu một VM lỗi, các VM khác ít bị ảnh hưởng hơn.

## 5.2. Khi muốn cô lập môi trường

Staging, UAT, production không nên trộn bừa trên cùng OS.

Không nên:

```text
1 server vật lý
  -> chạy staging app
  -> chạy UAT app
  -> chạy production app
  -> chạy production database
```

Vì nếu máy đó bị lỗi hoặc cấu hình sai, ảnh hưởng rất rộng.

Nên hơn:

```text
server vật lý
  -> hypervisor
      -> VM staging
      -> VM UAT
      -> VM production
```

Tốt hơn nữa:

```text
staging VM nằm trong staging network
UAT VM nằm trong UAT network
production VM nằm trong production network
```

## 5.3. Khi muốn dễ backup, snapshot, migrate

VM dễ thao tác hơn bare metal:

```text
snapshot trước khi nâng cấp
clone VM tạo môi trường test
migrate VM sang host khác
backup disk VM
restore VM khi lỗi
```

Không nên lạm dụng snapshot cho database production lâu dài.
Nhưng với app server, tool server, test server, VM snapshot rất tiện.

## 5.4. Khi muốn chuẩn hóa vận hành

Doanh nghiệp thường muốn có template VM:

```text
Ubuntu base image
hardening security
agent monitoring
agent logging
NTP
DNS
standard users
standard firewall
standard disk layout
```

Khi cần máy mới, tạo từ template.
Không phải cài tay lại từ đầu.

## 5.5. Khi muốn chạy Kubernetes on-prem

Mô hình rất thường gặp:

```text
physical server pool
  -> VMware/KVM/OpenStack
      -> VM Kubernetes nodes
          -> pods
```

Ví dụ production:

```text
prod-cp-1 VM
prod-cp-2 VM
prod-cp-3 VM
prod-worker-1 VM
prod-worker-2 VM
prod-worker-3 VM
```

VM là node.
Service không phải là VM.
Service chạy thành pod bên trong cluster.

## 5.6. Khi nào VM chưa đủ?

VM tốt, nhưng không giải quyết hết.

Nếu công ty có 100 service, deploy thủ công lên 100 VM sẽ rất mệt:

```text
SSH từng máy
copy JAR
restart service
check log từng máy
rollback từng máy
```

Lúc đó nên dùng:

```text
container
Kubernetes
GitOps
CI/CD
observability
secret manager
```

VM là nền.
Kubernetes là lớp quản lý app phía trên.

---

# 6. Khi nào nên dùng cloud?

Cloud nên dùng khi lợi ích về tốc độ, dịch vụ sẵn có và khả năng mở rộng lớn hơn ràng buộc chi phí/pháp lý.

## 6.1. Khi cần tạo hạ tầng nhanh

On-prem có thể mất:

```text
mua server
đợi giao hàng
lắp rack
cắm mạng
cài hypervisor
cấu hình storage
cấu hình firewall
```

Cloud có thể tạo trong vài phút:

```text
VM
VPC
subnet
Kubernetes cluster
database
load balancer
object storage
```

Với team cần thử nghiệm nhanh, cloud rất tiện.

## 6.2. Khi cần scale linh hoạt

Ví dụ ví điện tử có chiến dịch:

```text
sale lớn
cashback lớn
ngày lương
sự kiện truyền thông
traffic tăng đột biến
```

Cloud giúp tăng giảm tài nguyên nhanh hơn on-prem.

Ví dụ:

```text
Kubernetes node autoscaling
database read replica
object storage gần như không cần tự mua disk
load balancer managed
CDN
```

## 6.3. Khi muốn dùng managed service

Managed service nghĩa là nhà cung cấp lo một phần vận hành.

Ví dụ:

```text
managed Kubernetes:
  cloud lo control-plane phần lớn

managed database:
  cloud hỗ trợ backup, replication, patching

managed object storage:
  cloud lo lưu file bền vững

managed secret manager:
  cloud lo lưu secret an toàn hơn file text
```

Team DevOps vẫn phải hiểu hệ thống.
Nhưng không phải tự gánh mọi lớp từ đầu.

## 6.4. Khi cần triển khai đa vùng địa lý

Công ty lớn có thể cần:

```text
region Hà Nội
region Hồ Chí Minh
region Singapore
region Tokyo
region US
```

Cloud có nhiều region sẵn.
On-prem muốn làm đa vùng sẽ rất tốn.

## 6.5. Khi nào cloud không phải lựa chọn dễ?

Cloud không tự động làm hệ thống tốt hơn.

Các vấn đề vẫn còn:

```text
thiết kế network sai vẫn nguy hiểm
IAM sai vẫn lộ quyền
secret sai vẫn bị leak
database sai vẫn mất dữ liệu
chi phí có thể tăng rất nhanh
latency tới hệ thống nội bộ có thể cao
compliance có thể không cho đưa dữ liệu ra ngoài
vendor lock-in
```

`vendor lock-in` nghĩa là phụ thuộc quá sâu vào dịch vụ riêng của một nhà cung cấp.
Sau này muốn chuyển cloud hoặc quay về on-prem sẽ khó.

## 6.6. Bank, fintech có dùng cloud không?

Có, nhưng thường rất thận trọng.

Một số phần dễ đưa lên cloud hơn:

```text
website public
mobile API gateway lớp ngoài
analytics không chứa dữ liệu nhạy cảm
log đã được mask dữ liệu
AI/ML sandbox
dev/test environment
backup object storage được mã hóa
DR site nếu pháp lý cho phép
```

Một số phần thường bị giữ on-prem hoặc private cloud:

```text
core banking
ledger
payment switch
transaction database
HSM/key management nhạy cảm
PII database
fraud/risk engine rất nhạy
integration với Napas/core nội bộ
```

`PII` là personally identifiable information.
Nói dễ hiểu là dữ liệu định danh cá nhân:

```text
họ tên
số điện thoại
CCCD/CMND
email
địa chỉ
tài khoản ngân hàng
thông tin KYC
```

---

# 7. Network staging, UAT, production nên như thế nào?

## 7.1. Có cần chung mạng không?

Không nên hiểu là staging, UAT, production phải chung một mạng.
Trong doanh nghiệp nghiêm túc, thường tách mạng.

Mô hình tốt hơn:

```text
staging network riêng
UAT network riêng
production network riêng
```

Chúng có thể cùng data center, cùng cloud account, cùng hệ thống firewall.
Nhưng không nên là một mạng phẳng nơi máy nào cũng gọi máy nào.

## 7.2. Mạng phẳng là gì?

`mạng phẳng` nghĩa là các máy nằm trong cùng vùng mạng và thấy nhau quá dễ.

Ví dụ xấu:

```text
staging app -> gọi được production database
developer laptop -> SSH được production worker
UAT app -> gọi được production internal API
test script -> bắn nhầm vào production payment API
```

Mạng phẳng dễ học, nhưng nguy hiểm trong doanh nghiệp.

## 7.3. Nên tách bằng gì?

Tùy hạ tầng.

On-prem thường dùng:

```text
VLAN
subnet
firewall zone
router ACL
load balancer
VPN
jump server/bastion
```

Cloud thường dùng:

```text
VPC/VNet
subnet
security group
network ACL
route table
private endpoint
load balancer
WAF
IAM
```

Kubernetes dùng thêm:

```text
namespace
network policy
ingress
service mesh nếu hệ thống lớn
```

## 7.4. Ví dụ network 3 môi trường

Ví dụ dễ hình dung:

```text
staging:
  subnet 10.10.1.0/24
  domain staging-api.example.com
  database staging riêng

UAT:
  subnet 10.10.2.0/24
  domain uat-api.example.com
  database UAT riêng

production:
  subnet 10.10.3.0/24
  domain api.example.com
  database production riêng
```

Không nên:

```text
staging dùng chung database production
UAT dùng chung Redis production
staging và production dùng chung secret
developer truy cập production node trực tiếp từ Internet
```

## 7.5. Những đường nào được phép mở?

Nên mở theo nguyên tắc:

```text
chỉ mở cái cần mở
mở từ nguồn cụ thể tới đích cụ thể
không mở rộng kiểu 0.0.0.0/0 nếu không cần
production chặt hơn staging/UAT
```

Ví dụ được phép:

```text
Internet -> production load balancer port 443
production app -> production database private port
CI/CD runner -> Git server
Argo CD -> GitOps repo
Argo CD -> Kubernetes API trong môi trường tương ứng
monitoring central -> scrape metric hoặc nhận log theo rule rõ ràng
admin -> bastion/VPN -> production admin endpoint
```

Ví dụ nên chặn:

```text
Internet -> Kubernetes API server
Internet -> database
staging app -> production database
UAT app -> production database
developer laptop -> production database
CI runner không tin cậy -> production network
```

## 7.6. Public network và private network

`public network` là mạng có thể truy cập từ Internet.

Ví dụ:

```text
api.example.com
web.example.com
public load balancer
```

`private network` là mạng nội bộ.
Chỉ hệ thống bên trong hoặc qua VPN/bastion mới truy cập được.

Ví dụ:

```text
Kubernetes node traffic
database traffic
Redis traffic
Kafka traffic
service-to-service traffic
monitoring internal traffic
```

Production nên có quy tắc:

```text
public chỉ tới load balancer/ingress/WAF/API gateway
app gọi database qua private IP
node join cluster qua private IP
admin đi qua VPN/bastion
database không public Internet
```

## 7.7. Bastion hoặc jump server là gì?

`bastion` hoặc `jump server` là máy trung gian để admin truy cập hệ thống nội bộ.

Thay vì:

```text
developer laptop -> SSH thẳng production server
```

Nên là:

```text
developer laptop
  -> VPN
  -> bastion
  -> production server
```

Bastion giúp:

- Ghi log truy cập.
- Giới hạn ai được vào.
- Không public mọi server ra Internet.
- Tập trung kiểm soát SSH/kubectl/admin tool.

## 7.8. WAF là gì?

`WAF` là Web Application Firewall.
Nó đứng trước web/API để chặn một số kiểu tấn công phổ biến.

Ví dụ:

```text
Internet
  -> WAF
  -> load balancer
  -> ingress/API gateway
  -> service
```

WAF không thay thế code security.
Nhưng nó là lớp bảo vệ quan trọng cho hệ thống public.

## 7.9. API Gateway là gì?

`API Gateway` là cửa vào API.
Nó có thể làm:

```text
routing
authentication
rate limiting
request logging
quota
token validation
header transformation
```

Trong ví điện tử, API gateway thường đứng trước các service nội bộ.

Ví dụ:

```text
mobile app
  -> API gateway
  -> UAA
  -> wallet-service
  -> payment-service
```

## 7.10. Service mesh là gì?

`service mesh` là lớp quản lý traffic giữa service với service.

Ví dụ:

```text
Istio
Linkerd
Consul
```

Nó có thể hỗ trợ:

```text
mTLS giữa service
traffic splitting
retry
timeout
circuit breaking
observability
```

Người mới chưa cần dùng ngay.
Hệ thống lớn mới cần cân nhắc.

---

# 8. App service nên deploy thế nào?

## 8.1. Không nên hiểu 1 service = 1 máy

Với microservice hiện đại, thường không cấp riêng một máy cho từng service.

Không phổ biến:

```text
VM 1 chỉ chạy gateway
VM 2 chỉ chạy uaa
VM 3 chỉ chạy post-service
VM 4 chỉ chạy wallet-service
```

Phổ biến hơn:

```text
Kubernetes worker 1:
  gateway pod
  uaa pod
  post pod

Kubernetes worker 2:
  gateway pod
  uaa pod
  wallet pod

Kubernetes worker 3:
  uaa pod
  payment pod
  post pod
```

Kubernetes sẽ rải pod lên worker dựa theo tài nguyên và rule.

## 8.2. Service stateless là gì?

`stateless service` là service không giữ dữ liệu quan trọng trong RAM/disk local.
Nếu pod chết, pod mới lên vẫn chạy bình thường vì dữ liệu nằm ở database/cache/message queue.

Ví dụ thường stateless:

```text
gateway
uaa-service
post-service
notification-service
profile-service
loyalty-service
```

Các service này phù hợp chạy nhiều replica trong Kubernetes.

## 8.3. Stateful service là gì?

`stateful service` là service giữ trạng thái/dữ liệu quan trọng.

Ví dụ:

```text
database
Kafka
Redis cluster
Elasticsearch
MinIO
```

Stateful service khó vận hành hơn stateless service.
Không phải cứ đưa vào Kubernetes là xong.

Production cần nghĩ kỹ về:

```text
storage
backup
restore
replication
upgrade
split brain
data corruption
disaster recovery
```

## 8.4. UAA, post, gateway nên deploy thế nào?

Với app Spring Boot bình thường:

```text
gateway
uaa-service
post-service
wallet-service
payment-service
notification-service
```

Mô hình hợp lý:

```text
mỗi service là một Docker image
mỗi service là một Kubernetes Deployment
mỗi Deployment có nhiều replica
mỗi service có Kubernetes Service nội bộ
public traffic đi qua ingress/API gateway
config tách theo môi trường
secret lấy từ secret manager
```

Ví dụ:

```text
namespace production:
  gateway:
    replicas: 3

  uaa-service:
    replicas: 3

  post-service:
    replicas: 3

  wallet-service:
    replicas: 3
```

## 8.5. Khi nào service cần node riêng?

Không phải service nào cũng cần node riêng.
Nhưng có vài trường hợp cần tách:

```text
service rất nặng CPU
service rất nặng RAM
service xử lý batch lớn
service có yêu cầu bảo mật riêng
service cần GPU
service cần chạy gần phần cứng đặc biệt
service có license gắn với node
```

Trong Kubernetes có thể dùng:

```text
node pool riêng
taint/toleration
node affinity
pod anti-affinity
resource request/limit
priority class
```

Nói dễ hiểu:

```text
service thường -> chạy chung worker pool
service đặc biệt -> tách node pool riêng
database lớn -> thường tách cụm riêng ngoài app worker
```

---

# 9. Database nên deploy thế nào?

## 9.1. Database khác app service

Database không giống gateway hay UAA.
App stateless chết có thể tạo pod mới.
Database chết hoặc mất dữ liệu là chuyện nghiêm trọng hơn nhiều.

Với ví điện tử, database có thể chứa:

```text
số dư ví
lịch sử giao dịch
thông tin KYC
token
audit log
đối soát
ledger
```

Vì vậy database production cần thiết kế riêng.

## 9.2. Các cách deploy database

### Cách 1: Database trên VM riêng

Mô hình:

```text
database VM
  -> PostgreSQL/Oracle/MySQL
```

App trong Kubernetes gọi database qua private network.

Phù hợp khi:

```text
team DBA muốn quản trực tiếp
hệ thống on-prem
cần kiểm soát backup/replication
chưa dùng managed database
```

### Cách 2: Database trên server vật lý riêng

Mô hình:

```text
server vật lý mạnh
  -> database
```

Phù hợp khi:

```text
database rất lớn
cần IOPS rất cao
cần RAM rất lớn
cần tối ưu storage sâu
có license enterprise
```

### Cách 3: Database cluster riêng

Mô hình:

```text
db-primary
db-standby
db-replica
backup server
```

Hoặc:

```text
Oracle RAC
PostgreSQL HA cluster
MySQL InnoDB Cluster
SQL Server Always On
```

Phù hợp production nghiêm túc.

### Cách 4: Managed database

Mô hình:

```text
cloud managed PostgreSQL/MySQL/Oracle
```

Cloud hỗ trợ nhiều phần:

```text
backup
restore
replication
patching
monitoring
failover
```

Vẫn cần hiểu cấu hình, quyền, network, encryption, backup retention.
Managed không có nghĩa là khỏi cần DBA.

### Cách 5: Database trong Kubernetes

Mô hình:

```text
Kubernetes
  -> database operator
  -> StatefulSet
  -> persistent volume
```

Chỉ nên dùng khi team đã chắc về:

```text
storage class
persistent volume
backup/restore
operator
failover
node failure
volume attach/detach
disaster recovery
```

Với người mới, production database trong Kubernetes là hướng khó.
Lab thì được.
Production ví điện tử thì phải rất cẩn thận.

## 9.3. Database staging/UAT/prod có dùng chung không?

Không nên dùng chung.

Nên là:

```text
staging database riêng
UAT database riêng
production database riêng
```

Có thể dùng dữ liệu giả hoặc dữ liệu đã mask cho staging/UAT.

`mask dữ liệu` nghĩa là che hoặc biến đổi dữ liệu nhạy cảm.

Ví dụ:

```text
Nguyễn Văn A -> User Test 001
0912345678 -> 0900000001
123456789012 -> 999999999999
```

Không nên copy thẳng dữ liệu production thật sang môi trường test nếu không có quy trình bảo mật.

---

# 10. Doanh nghiệp trung bình thường triển khai thế nào?

Doanh nghiệp trung bình ở đây có thể hiểu:

```text
vài chục service
traffic vừa
team DevOps nhỏ
có staging/UAT/prod
có yêu cầu bảo mật nhưng chưa quá phức tạp như bank lớn
```

## 10.1. Mô hình on-prem/VM phổ biến

```text
physical servers
  -> VMware/KVM/Proxmox
      -> VM cho GitLab/Jenkins/Harbor
      -> VM cho Kubernetes staging
      -> VM cho Kubernetes UAT
      -> VM cho Kubernetes production
      -> VM/database riêng
```

Ví dụ:

```text
staging:
  1 control-plane VM
  1-2 worker VM
  database VM riêng hoặc database nhỏ riêng

UAT:
  1 control-plane VM
  2 worker VM
  database VM riêng

production:
  3 control-plane VM
  3-6 worker VM
  database primary/standby VM riêng
```

## 10.2. Network

```text
VLAN staging
VLAN UAT
VLAN production
VLAN management
VLAN database
```

Luồng public:

```text
Internet
  -> firewall/WAF
  -> load balancer
  -> ingress/API gateway
  -> service
```

Luồng nội bộ:

```text
service
  -> private network
  -> database/cache/message broker
```

## 10.3. Deploy

```text
developer push code
  -> GitLab/Jenkins build
  -> test
  -> scan
  -> build image
  -> push Harbor/Nexus registry
  -> update GitOps repo
  -> Argo CD sync vào cluster
```

Production thường cần:

```text
merge request
approval
manual sync hoặc controlled auto-sync
rollback plan
monitoring sau deploy
```

---

# 11. Doanh nghiệp lớn thường triển khai thế nào?

Doanh nghiệp lớn có thể có:

```text
hàng trăm service
nhiều team
nhiều domain nghiệp vụ
nhiều data center
quy trình change management
security team
DBA team
network team
platform team
SRE team
```

## 11.1. Hạ tầng thường là private cloud hoặc hybrid cloud

Mô hình:

```text
on-prem data center
  -> private cloud
      -> VM self-service
      -> Kubernetes/OpenShift platform

public cloud
  -> dùng cho workload phù hợp
```

`hybrid cloud` nghĩa là dùng cả on-prem và cloud.

Ví dụ:

```text
core transaction system: on-prem
mobile API edge: cloud hoặc DMZ
analytics: cloud
backup object storage: cloud hoặc object storage nội bộ
dev/test: cloud hoặc private cloud
```

## 11.2. Kubernetes platform

Doanh nghiệp lớn thường không để mỗi team tự dựng Kubernetes tùy ý.
Họ có platform chung:

```text
OpenShift hoặc Kubernetes chuẩn hóa
node pool theo workload
namespace theo team/app
quota theo namespace
network policy
central logging
central monitoring
secret manager
image registry
policy controller
admission control
```

`quota` là giới hạn tài nguyên.
Ví dụ team A được dùng tối đa:

```text
20 CPU
80 GB RAM
200 pod
```

## 11.3. Network nhiều lớp

Ví dụ:

```text
Internet
  -> DDoS protection
  -> WAF
  -> external load balancer
  -> DMZ
  -> internal load balancer
  -> API gateway
  -> Kubernetes ingress
  -> service
  -> database zone
```

`DMZ` là vùng mạng trung gian.
Nó nằm giữa Internet và mạng nội bộ quan trọng.

Ý tưởng:

```text
không cho Internet đi thẳng vào vùng production core
mọi traffic đi qua các lớp kiểm soát
```

## 11.4. Database và dữ liệu

Doanh nghiệp lớn thường có DBA riêng.
Database production có thể là:

```text
Oracle RAC
PostgreSQL HA
MySQL cluster
SQL Server Always On
mainframe/core database
data warehouse
data lake
```

Database thường tách khỏi Kubernetes app worker.

App gọi database qua:

```text
private network
firewall rule
service account
secret manager
connection pool
audit logging
```

## 11.5. CI/CD

Không phải ai cũng được deploy production.

Thường có:

```text
protected branch
protected tag
merge request approval
security scan
change request
release window
manual approval
segregation of duties
```

`segregation of duties` nghĩa là tách nhiệm vụ.
Ví dụ:

```text
developer viết code
reviewer approve code
QA approve test
release manager approve production
DevOps/SRE vận hành nền tảng
```

Mục tiêu là không để một người tự code, tự approve, tự deploy production mà không ai kiểm soát.

---

# 12. Doanh nghiệp cực lớn, bank lớn, ví điện tử quốc gia triển khai thế nào?

Mức này thường rất phức tạp.
Không còn là vài server hay vài cluster.

## 12.1. Nhiều data center

Có thể có:

```text
primary data center
secondary data center
DR data center
cloud region phụ trợ
```

`DR` là disaster recovery.
Nghĩa là phương án chạy lại khi data center chính gặp sự cố lớn.

Ví dụ:

```text
DC1 Hà Nội:
  production active

DC2 Hồ Chí Minh:
  standby hoặc active-active một phần

Cloud:
  backup, analytics, temporary scaling, DR workload phụ
```

## 12.2. Active-passive và active-active

`active-passive`:

```text
DC1 đang chạy chính
DC2 chờ
nếu DC1 lỗi thì failover sang DC2
```

Dễ hơn active-active nhưng khi failover cần quy trình rất chắc.

`active-active`:

```text
DC1 nhận traffic
DC2 cũng nhận traffic
cả hai cùng chạy
```

Khó hơn nhiều vì phải xử lý:

```text
đồng bộ dữ liệu
conflict
latency
split brain
routing
consistency
```

Với hệ thống tài chính, active-active không đơn giản.
Không phải cứ nhân đôi cluster là xong.

## 12.3. Tách vùng hệ thống theo độ nhạy

Ví dụ:

```text
Internet zone:
  WAF, public load balancer

DMZ zone:
  API gateway, reverse proxy

application zone:
  Kubernetes app services

data zone:
  database, cache, message broker

security zone:
  HSM, Vault, IAM, PKI

management zone:
  monitoring, logging, CI/CD, bastion
```

Mỗi zone có firewall rule riêng.
Không phải service nào cũng gọi được service nào.

## 12.4. Zero trust và least privilege

`zero trust` nghĩa là không mặc định tin ai chỉ vì họ đang ở trong mạng nội bộ.

`least privilege` nghĩa là chỉ cấp quyền tối thiểu cần thiết.

Ví dụ:

```text
service A chỉ được gọi service B nếu có nhu cầu thật
service A không được tự do query mọi database
CI runner staging không có secret production
developer không có quyền SSH production database
```

Trong hệ thống lớn, security không chỉ là firewall.
Nó nằm trong:

```text
network
identity
secret
certificate
audit log
policy
approval
runtime security
```

## 12.5. Multi-cluster Kubernetes

Doanh nghiệp cực lớn có thể có nhiều cluster:

```text
cluster staging
cluster UAT
cluster pre-prod
cluster production region 1
cluster production region 2
cluster batch
cluster data
cluster shared services
```

Không nên nhồi mọi thứ vào một cluster duy nhất.

Lý do:

- Giảm blast radius.
- Tách quyền.
- Tách network.
- Tách team ownership.
- Tách workload nặng.
- Dễ maintenance theo vùng.

`blast radius` nghĩa là phạm vi ảnh hưởng khi có sự cố.
Ví dụ staging lỗi không được kéo production chết theo.

## 12.6. Observability rất lớn

`observability` là khả năng nhìn thấy hệ thống đang khỏe hay đang lỗi.

Gồm:

```text
metrics
logs
traces
alerts
dashboards
SLO/SLA
```

Với ví điện tử/bank, cần quan sát:

```text
tỷ lệ giao dịch thành công
latency thanh toán
số giao dịch timeout
lỗi theo đối tác
lỗi theo ngân hàng
lỗi theo merchant
số dư bất thường
độ trễ message queue
database replication lag
```

Không chỉ xem CPU/RAM.
Phải xem nghiệp vụ.

---

# 13. Ví dụ tổng hợp: ví điện tử triển khai production

## 13.1. Mô hình vừa đủ nghiêm túc

```text
Internet
  -> DNS
  -> WAF
  -> public load balancer
  -> API gateway / ingress
  -> Kubernetes production cluster
      -> gateway
      -> uaa-service
      -> wallet-service
      -> payment-service
      -> notification-service
  -> private network
      -> database cluster
      -> Redis cluster
      -> Kafka cluster
      -> HSM/Vault
```

Kubernetes production:

```text
3 control-plane VM
3-10 app worker VM
node pool monitoring nếu cần
node pool batch nếu cần
```

Database:

```text
primary database
standby database
read replica nếu cần
backup server/object storage
restore drill định kỳ
```

## 13.2. Môi trường staging/UAT/prod

```text
staging:
  test sớm
  dữ liệu giả
  tài nguyên nhỏ
  deploy tự động hơn

UAT:
  gần giống production hơn
  QA/business test
  dữ liệu giả hoặc dữ liệu đã mask
  deploy có kiểm soát hơn staging

production:
  chạy thật
  dữ liệu thật
  quyền chặt
  network chặt
  deploy cần approval
  monitoring/alert nghiêm túc
```

## 13.3. Network giữa các môi trường

```text
staging network:
  không gọi được production database
  không dùng secret production

UAT network:
  không gọi được production database
  có thể gọi mock/sandbox partner

production network:
  chỉ mở public qua WAF/load balancer
  app gọi database qua private network
  admin qua VPN/bastion
```

## 13.4. Service deploy

```text
uaa-service:
  Deployment
  replicas: 3
  config từ ConfigMap
  secret từ secret manager
  token/key qua Vault/HSM nếu cần

wallet-service:
  Deployment
  replicas: 3-6
  gọi ledger database qua private network
  có idempotency

payment-service:
  Deployment
  replicas: 3-6
  gọi partner/bank/Napas qua network route được kiểm soát
  timeout/retry/circuit breaker rõ ràng

post-service:
  Deployment
  replicas: 2-3
  nếu chỉ là service phụ thì tài nguyên nhỏ hơn
```

`idempotency` nghĩa là cùng một request gửi lại nhiều lần nhưng không tạo kết quả sai.
Ví dụ user bấm thanh toán hai lần do mạng lag, hệ thống không được trừ tiền hai lần.

---

# 14. Bảng chọn nhanh

| Nhu cầu | Nên chọn |
| --- | --- |
| Lab học cơ bản | VPS hoặc VM nhỏ |
| App Spring Boot nhỏ | VM + Docker Compose hoặc Kubernetes nhỏ |
| Nhiều microservice | Kubernetes trên VM/cloud |
| Production app stateless | Kubernetes worker node |
| Database production nhỏ-vừa | VM riêng hoặc managed database |
| Database production rất lớn | Cluster riêng hoặc server vật lý riêng |
| Cần tạo môi trường nhanh | Cloud |
| Cần tuân thủ dữ liệu nghiêm ngặt | On-prem/private cloud/hybrid |
| Cần autoscale nhanh | Cloud hoặc Kubernetes tốt |
| Cần kiểm soát phần cứng đặc biệt | Server vật lý |
| Hệ thống legacy khó đổi | VM hoặc server riêng |
| Hệ thống bank/ví lớn | Hybrid, multi-zone, multi-cluster, network tách lớp |

---

# 15. Những hiểu lầm phổ biến

## 15.1. "Mỗi service phải có một server riêng"

Không đúng trong Kubernetes.

Thường là:

```text
một worker chạy nhiều pod của nhiều service
một service có nhiều pod trên nhiều worker
```

Chỉ tách node riêng khi service có lý do đặc biệt.

## 15.2. "Dùng cloud là khỏi cần DevOps"

Không đúng.

Cloud giảm một phần việc, nhưng vẫn cần:

```text
network design
IAM
secret
monitoring
backup
cost control
CI/CD
security
incident response
```

## 15.3. "Database cứ cho vào Kubernetes là hiện đại"

Không hẳn.

Database trong Kubernetes là chủ đề khó.
Lab thì tốt.
Production tài chính thì cần team rất chắc.

## 15.4. "Staging/UAT/prod chung database cho tiện"

Rất nguy hiểm.

Test nhầm có thể ảnh hưởng dữ liệu thật.
Secret, quyền và dữ liệu production phải tách.

## 15.5. "Có firewall là an toàn"

Firewall chỉ là một lớp.
Cần thêm:

```text
IAM
secret manager
network policy
TLS/mTLS
audit log
least privilege
secure CI/CD
runtime monitoring
```

---

# 16. Lộ trình học thực tế cho người mới

Nếu bạn mới học DevOps, nên đi theo thứ tự:

```text
1. Chạy app trên 1 VM bằng systemd
2. Chạy app trên 1 VM bằng Docker
3. Chạy nhiều app bằng Docker Compose
4. Dựng 1 Kubernetes cluster nhỏ
5. Deploy app bằng Deployment/Service/Ingress
6. Tách staging/UAT/prod bằng namespace
7. Tách staging/UAT/prod bằng cluster riêng
8. Thêm GitOps bằng Argo CD
9. Thêm registry, secret, monitoring, logging
10. Học network: subnet, firewall, private/public, bastion
11. Học database backup/restore/replication
12. Học production readiness
```

Đừng nhảy thẳng vào mô hình bank cực lớn khi chưa hiểu VM, network, container, Kubernetes cơ bản.
Nhưng cũng đừng nghĩ lab một máy là giống production.

---

# 17. Kết luận

Cách nghĩ đúng:

```text
server vật lý:
  nền phần cứng, dùng trực tiếp khi cần hiệu năng/kiểm soát/phần cứng đặc biệt

VM:
  lớp chia nhỏ và cô lập tài nguyên, rất phổ biến trong doanh nghiệp

cloud:
  thuê hạ tầng/dịch vụ linh hoạt, mạnh ở tốc độ, managed service và scale

Kubernetes:
  lớp quản lý app/container trên nhiều node

database:
  tài sản dữ liệu, phải thiết kế riêng, không đối xử như app stateless

network:
  hàng rào và đường đi của hệ thống, production phải tách và kiểm soát chặt
```

Với ví điện tử, fintech, bank, mô hình thực tế thường không phải một lựa chọn duy nhất.
Nó thường là kết hợp:

```text
on-prem cho core và dữ liệu nhạy cảm
VM/private cloud để chuẩn hóa hạ tầng
Kubernetes để chạy microservice
database cluster riêng cho dữ liệu quan trọng
cloud cho phần phù hợp như scale, analytics, object storage, dev/test hoặc DR
network tách lớp để giảm rủi ro
CI/CD và GitOps để deploy có kiểm soát
```

Một câu chốt dễ nhớ:

```text
Không chọn physical, VM hay cloud vì nó nghe hiện đại.
Chọn vì workload, dữ liệu, rủi ro, chi phí, pháp lý, khả năng vận hành và quy mô doanh nghiệp.
```
