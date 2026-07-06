# Topology C enterprise practical từ cơ bản đến hoàn chỉnh

Tài liệu này giải thích riêng **Topology C**:

```text
staging cluster
uat cluster
production cluster
```

Mục tiêu của tài liệu này là nhắm tới **một bản đích duy nhất**:

```text
Topology C enterprise practical
```

Đây là bản mà nhiều doanh nghiệp nghiêm túc có thể dùng làm nền:

```text
staging cluster:
  1 control-plane
  1 worker

uat cluster:
  1 control-plane
  2 worker

production cluster:
  3 control-plane
  3 worker
```

Tổng:

```text
11 VPS / node
```

Các phần nhỏ hơn trong tài liệu **không phải biến thể để chọn mãi**.
Chúng chỉ là các bước thực hành để đi dần tới bản cuối ở trên.

Lộ trình:

```text
1. Hiểu Topology C là gì
2. Dựng staging trước với nhiều VPS
3. Chuẩn hóa GitOps trên staging
4. Nhân lên UAT với nhiều VPS
5. Nhân lên production với nhiều VPS
6. Nâng production control-plane HA
7. Hoàn thiện monitoring, secret, backup
```

Tài liệu này viết cho người mới, nên sẽ ưu tiên cách giải thích dễ hình dung trước, thuật ngữ kỹ thuật đi sau.

---

# 1. Topology C là gì?

Topology C nghĩa là **mỗi môi trường có một Kubernetes cluster riêng**.

Thay vì:

```text
1 cluster chung
  namespace staging
  namespace uat
  namespace production
```

Ta tách thành:

```text
cluster staging
cluster uat
cluster production
```

Nếu ví Kubernetes cluster là một tòa nhà:

```text
Topology A:
  staging, uat, production là 3 phòng trong cùng một tòa nhà

Topology C:
  staging, uat, production là 3 tòa nhà khác nhau
```

Tách như vậy giúp production an toàn hơn.
Nếu staging bị lỗi nặng, production không dùng chung cluster nên ít bị kéo theo.

---

# 2. Vì sao doanh nghiệp thích Topology C?

Topology C dễ được doanh nghiệp tin dùng vì nó rõ ràng.

```text
staging lỗi       -> không làm chết UAT/prod
UAT lỗi           -> không làm chết staging/prod
production lỗi    -> chỉ tập trung xử lý production
```

Với các hệ thống như ví điện tử, fintech, banking, payment, loyalty, eKYC, lending, core API, Topology C có nhiều điểm hợp lý:

- Tách production khỏi môi trường test.
- Dễ phân quyền: ai được vào staging, ai được vào UAT, ai được vào production.
- Dễ audit: thay đổi production đi qua luồng riêng.
- Dễ giới hạn rủi ro: deploy lỗi ở staging không đụng production.
- Dễ giải thích cho quản lý, security, audit, compliance.
- UAT có thể cấu hình gần giống production hơn.

Nhược điểm là tốn hơn:

- Nhiều cluster hơn.
- Nhiều Argo CD hoặc nhiều context hơn.
- Nhiều ingress hơn.
- Nhiều monitoring/logging/secret setup hơn.
- Cần biết vận hành Kubernetes tốt hơn.

---

# 3. Các keyword cần hiểu trước

## 3.1. Cluster

`cluster` là một cụm Kubernetes.
Nó gồm phần điều khiển và phần chạy app.

Ví dụ:

```text
cluster staging:
  chạy app staging

cluster uat:
  chạy app UAT

cluster production:
  chạy app production
```

Mỗi cluster có API server, node, network, storage, ingress riêng.

## 3.2. Control-plane

`control-plane` là phần điều khiển của cluster.
Hãy tưởng tượng nó là **ban quản lý tòa nhà**.

Nó làm các việc như:

- Nhận lệnh deploy từ `kubectl`, Argo CD, CI/CD.
- Ghi nhớ hệ thống mong muốn chạy app nào, mấy bản.
- Chọn worker nào còn tài nguyên để đặt pod.
- Theo dõi nếu pod chết thì tạo lại pod.

Ví dụ:

```text
Bạn muốn uaa-service chạy 3 bản
  -> control-plane nhận yêu cầu
  -> control-plane chọn worker phù hợp
  -> worker chạy pod uaa-service
```

Control-plane không phải nơi chính để chạy app business.
Nó giống người điều phối.

## 3.3. Worker

`worker` là nơi app chạy thật.
Hãy tưởng tượng worker là **các phòng làm việc**.

Worker có thể chạy:

```text
gateway
uaa-service
post-service
payment-service
wallet-service
ingress-nginx
prometheus
redis demo
postgres demo
```

Nếu app ăn CPU/RAM, worker là nơi chịu tải chính.

## 3.4. Pod

`pod` là đơn vị chạy nhỏ nhất trong Kubernetes.
Thường một pod chứa một container app.

Ví dụ:

```text
gateway pod
uaa-service pod
post-service pod
```

Khi nói:

```text
uaa-service replicas=3
```

Nghĩa là Kubernetes sẽ chạy 3 pod của `uaa-service`.

## 3.5. Namespace

`namespace` là khu vực logic trong một cluster.

Trong Topology C, mỗi môi trường đã có cluster riêng, nhưng vẫn dùng namespace để chia nhóm tài nguyên.

Ví dụ trong production cluster:

```text
namespace production
namespace argocd
namespace ingress-nginx
namespace monitoring
namespace external-secrets
```

Namespace không phải cluster riêng.
Nó chỉ là khu vực bên trong cluster.

---

# 4. Bước nền: dựng staging trước với nhiều VPS

Bước đầu tiên chỉ cần dựng staging, nhưng vẫn dùng nhiều VPS.
Đây là bước nền để đi tới bản 11 VPS ở mục 6, không phải bản cuối.

```text
VPS 1 -> staging control-plane
VPS 2 -> staging worker
```

Đây là cách rất hợp để học.
Nó chưa phải Topology C đầy đủ, nhưng đã giúp bạn học network thật giữa các VPS:

```text
worker join vào control-plane
control-plane gọi kubelet trên worker
pod chạy trên worker
ingress nhận request
firewall mở đúng port
private IP giữa các VPS
```

## 4.1. Cấu hình VPS đề xuất

Tối thiểu cho bước staging:

```text
staging-cp-1:
  2 CPU, 4 GB RAM, 50-80 GB disk

staging-worker-1:
  4 CPU, 8 GB RAM, 80-120 GB disk
```

Dễ thở hơn:

```text
staging-cp-1:
  2-4 CPU, 4-8 GB RAM

staging-worker-1:
  4-6 CPU, 8-12 GB RAM
```

Nếu app là Java/Spring Boot, nên ưu tiên RAM.
Java app, Argo CD, Prometheus, ingress, database demo đều ăn RAM khá đều.

## 4.2. Đặt chương trình ở đâu?

### Staging control-plane

```text
staging-cp-1:
  kube-apiserver
  etcd
  scheduler
  controller-manager
```

### Staging worker

```text
staging-worker-1:
  namespace argocd:
    Argo CD staging

  namespace ingress-nginx:
    ingress-nginx

  namespace staging:
    gateway
    uaa-service
    post-service
    postgres demo nếu cần
    redis demo nếu cần
```

Network cần học ngay từ bước này:

```text
SSH vào từng VPS
private IP giữa control-plane và worker
port Kubernetes cần mở nội bộ
firewall chỉ public port cần thiết
worker join vào cluster qua control-plane private IP
```

UAT và production chưa dựng ở mục này.
Sau khi staging chạy ổn, ta mới nhân cấu trúc này sang UAT ở bước 3 và production ở bước 4.

Monitoring có thể để sau.
Đừng bật Prometheus/Grafana/Loki ngay từ ngày đầu nếu staging VPS chỉ có 4 GB RAM.

## 4.3. Bản này học được gì?

Bạn học được:

```text
1 cluster staging
2 VPS/node
control-plane và worker tách nhau
1 kubeconfig context staging
1 Argo CD instance
1 ingress host
config repo cho staging
cách deploy app bằng GitOps
network giữa control-plane và worker
```

Ví dụ host:

```text
staging-api.example.com -> cluster staging
```

---

# 5. Vì sao không để control-plane và worker chung mãi?

Lúc học, để chung là bình thường.
Một VPS vừa làm control-plane vừa làm worker giúp tiết kiệm tiền.

Nhưng trong môi trường nghiêm túc, nên tách dần.

## 5.1. Lý do 1: control-plane cần ổn định

Control-plane là nơi điều khiển cluster.
Nếu worker chạy app nặng làm hết CPU/RAM, control-plane cũng bị ảnh hưởng nếu chúng ở cùng VPS.

Ví dụ:

```text
payment-service bị memory leak
  -> ăn hết RAM trên VPS
  -> control-plane trên cùng VPS cũng chậm hoặc chết
  -> bạn khó deploy/rollback/debug
```

Khi control-plane tách riêng:

```text
worker bị nặng
  -> app có thể lỗi
  -> nhưng control-plane vẫn còn để điều phối, scale, rollback
```

## 5.2. Lý do 2: app business và hệ điều khiển có nhiệm vụ khác nhau

Control-plane giống người điều phối.
Worker giống người làm việc trực tiếp.

Nếu để người điều phối vừa phải điều phối vừa phải gánh việc nặng, lúc cao điểm dễ loạn.

Trong Kubernetes cũng vậy:

```text
control-plane:
  nhận lệnh
  lưu trạng thái
  điều phối pod

worker:
  chạy app
  chịu traffic
  dùng CPU/RAM chính
```

## 5.3. Lý do 3: dễ nâng cấp và bảo trì

Nếu tách control-plane và worker:

```text
bảo trì worker
  -> app có thể chuyển sang worker khác

bảo trì control-plane
  -> ít đụng trực tiếp workload app hơn
```

Nếu tất cả nằm trên một VPS, bảo trì VPS đó là ảnh hưởng toàn cluster.

## 5.4. Lý do 4: production cần chịu lỗi

Nếu production chỉ có một VPS:

```text
VPS chết
  -> cluster production chết
  -> app production chết
```

Nếu production có nhiều worker:

```text
worker-1 chết
  -> pod có thể chạy trên worker-2/worker-3
```

Nếu production có nhiều control-plane:

```text
control-plane-1 chết
  -> control-plane-2/control-plane-3 vẫn giữ cluster điều khiển được
```

Vì vậy production nghiêm túc thường đi về hướng:

```text
3 control-plane
3 worker trở lên
```

---

# 6. Bản ultimate cần build tới

Từ đây trở đi, ta chốt một bản đích duy nhất.
Các bước thực hành phía sau chỉ là đường đi để build được bản này.

## 6.1. Topology cuối

```text
staging cluster:
  staging-cp-1
  staging-worker-1

uat cluster:
  uat-cp-1
  uat-worker-1
  uat-worker-2

production cluster:
  prod-cp-1
  prod-cp-2
  prod-cp-3
  prod-worker-1
  prod-worker-2
  prod-worker-3
```

Tổng:

```text
11 VPS = 11 Kubernetes node
3 cluster riêng
```

Đây là bản hợp lý vì:

- Staging đủ để test sớm.
- UAT đã có nhiều worker để test gần production hơn.
- Production có 3 control-plane để HA phần điều khiển.
- Production có 3 worker để rải pod và chịu lỗi một worker.
- Production tách hẳn staging/UAT ở cấp cluster.

## 6.2. Sizing VPS cho bản cuối

### Staging

```text
staging-cp-1:
  CPU: 2 vCPU
  RAM: 4 GB
  Disk: 50-80 GB SSD

staging-worker-1:
  CPU: 4 vCPU
  RAM: 8 GB
  Disk: 80-120 GB SSD
```

### UAT

```text
uat-cp-1:
  CPU: 2 vCPU
  RAM: 4 GB
  Disk: 50-80 GB SSD

uat-worker-1:
  CPU: 4-6 vCPU
  RAM: 8-12 GB
  Disk: 100-150 GB SSD

uat-worker-2:
  CPU: 4-6 vCPU
  RAM: 8-12 GB
  Disk: 100-150 GB SSD
```

### Production

```text
prod-cp-1:
  CPU: 2-4 vCPU
  RAM: 4-8 GB
  Disk: 80-120 GB SSD

prod-cp-2:
  CPU: 2-4 vCPU
  RAM: 4-8 GB
  Disk: 80-120 GB SSD

prod-cp-3:
  CPU: 2-4 vCPU
  RAM: 4-8 GB
  Disk: 80-120 GB SSD

prod-worker-1:
  CPU: 8 vCPU
  RAM: 16 GB
  Disk: 150-250 GB SSD

prod-worker-2:
  CPU: 8 vCPU
  RAM: 16 GB
  Disk: 150-250 GB SSD

prod-worker-3:
  CPU: 8 vCPU
  RAM: 16 GB
  Disk: 150-250 GB SSD
```

## 6.3. Tổng tài nguyên ước tính

```text
CPU:
  staging:    6 vCPU
  UAT:        10-14 vCPU
  production: 30-36 vCPU
  total:      khoảng 46-56 vCPU

RAM:
  staging:    12 GB
  UAT:        20-28 GB
  production: 60-72 GB
  total:      khoảng 92-112 GB

Disk:
  staging:    130-200 GB
  UAT:        250-380 GB
  production: 690-1110 GB
  total:      khoảng 1.0-1.7 TB SSD
```

Đây là tài nguyên cho bản thực hành nghiêm túc theo hướng enterprise practical, không phải sizing bắt buộc cho mọi doanh nghiệp.
Hệ thống thật phải tính thêm traffic, số service, database, log volume, retention, backup, peak load.

## 6.4. Chi phí VPS Việt Nam ước tính

Giá VPS thay đổi theo nhà cung cấp, khuyến mãi, loại CPU, disk, backup và băng thông.
Ước lượng thô:

```text
VPS nhỏ 2 vCPU / 4 GB:
  khoảng 150.000 - 300.000đ/tháng

VPS vừa 4 vCPU / 8 GB:
  khoảng 350.000 - 700.000đ/tháng

VPS khá 8 vCPU / 16 GB:
  khoảng 800.000 - 1.500.000đ/tháng
```

Bản 11 VPS:

```text
staging:
  1 VPS nhỏ + 1 VPS vừa
  khoảng 500.000 - 1.000.000đ/tháng

UAT:
  1 VPS nhỏ + 2 VPS vừa
  khoảng 850.000 - 1.700.000đ/tháng

production:
  3 VPS nhỏ + 3 VPS khá
  khoảng 2.850.000 - 5.400.000đ/tháng

Tổng:
  khoảng 4.2 - 8.1 triệu/tháng
```

Nếu muốn tiết kiệm khi học, giảm staging/UAT trước.
Không nên giảm production quá mạnh nếu mục tiêu là học bản enterprise.

---

# 7. Lộ trình thực hành để build tới bản ultimate

Các bước dưới đây không phải các biến thể để chọn.
Chúng là các nấc thang.
Đi hết các bước này thì bạn build được bản cuối ở mục 6.

Điểm quan trọng: **không build đều cả staging/UAT/production ngay từ đầu**.
Ta đi theo đúng dòng chảy tự nhiên:

```text
staging trước, nhưng đã tách nhiều VPS để học network
  -> UAT nhiều VPS
  -> production nhiều VPS
  -> production HA
  -> monitoring, secret, backup
```

## 7.1. Bước 1: dựng staging 2 VPS

Mục tiêu:

```text
hiểu Kubernetes cluster đầu tiên
deploy được app demo
biết deploy app bằng Argo CD
hiểu ingress, namespace, service, deployment
học worker join control-plane qua private network
học firewall/security group cơ bản
```

Hạ tầng tạm thời:

```text
staging:
  VPS 1: staging-cp-1
  VPS 2: staging-worker-1
```

Mục tiêu không phải HA.
Mục tiêu là học network thật giữa 2 VPS.

```text
staging-cp-1:
  control-plane

staging-worker-1:
  worker chạy app
```

Việc cần làm:

```text
1. Cài k3s server trên staging-cp-1
2. Join staging-worker-1 vào cluster
3. Lấy kubeconfig staging
4. Đặt context tên staging
5. Cài ingress-nginx trên worker
6. Cài Argo CD staging
7. Deploy gateway/uaa/post vào namespace staging
8. Tạo host staging-api.example.com
9. Chỉ public port cần thiết, còn node-to-node dùng private IP
```

Kết quả:

```text
staging-api.example.com -> staging cluster
```

## 7.2. Bước 2: chuẩn hóa GitOps repo

Mục tiêu:

```text
không sửa tay trên cluster
mọi deploy staging đi qua GitOps
chuẩn bị cấu trúc để sau này nhân lên UAT/production
```

Cấu trúc repo:

```text
platform-config-repo/
  apps/
    gateway/
      base/
      overlays/
        staging/

    uaa-service/
      base/
      overlays/
        staging/

    post-service/
      base/
      overlays/
        staging/

  clusters/
    staging/
```

Luồng:

```text
code merge
  -> CI build image
  -> push registry
  -> update staging image tag
  -> Argo CD staging sync
```

Ở bước này chưa cần UAT/production.
Làm staging cho chắc trước.

## 7.3. Bước 3: nhân lên UAT 3 VPS

Mục tiêu:

```text
có môi trường UAT riêng
hiểu promote từ staging sang UAT
không để UAT chung cluster với staging
học nhiều worker trong một cluster
```

Hạ tầng tạm thời:

```text
staging:
  1 control-plane
  1 worker

uat:
  1 control-plane
  2 worker
```

Việc cần làm:

```text
1. Cài k3s server trên uat-cp-1
2. Join uat-worker-1 và uat-worker-2
3. Lấy kubeconfig UAT
4. Cài ingress-nginx UAT
5. Cài Argo CD UAT
6. Thêm overlays/uat trong GitOps repo
7. Deploy gateway/uaa/post vào namespace uat
8. Promote cùng image tag từ staging sang UAT
9. Test pod chạy rải trên 2 worker
```

Kết quả:

```text
staging-api.example.com -> staging cluster
uat-api.example.com     -> UAT cluster
```

## 7.4. Bước 4: nhân lên production 1 control-plane + 2 worker

Mục tiêu:

```text
có production cluster riêng
hiểu luồng promote staging -> UAT -> production
production chưa HA control-plane, nhưng đã tách worker
học network/security nghiêm túc hơn production
```

Hạ tầng:

```text
staging:
  1 control-plane
  1 worker

uat:
  1 control-plane
  2 worker

production:
  1 control-plane
  2 worker
```

Việc cần làm:

```text
1. Cài k3s server trên prod-cp-1
2. Join prod-worker-1 và prod-worker-2
3. Lấy kubeconfig production
4. Cài ingress-nginx production
5. Cài Argo CD production
6. Thêm overlays/production trong GitOps repo
7. Deploy gateway/uaa/post vào namespace production
8. Production deploy qua merge request/approval
9. Public ingress production, giữ Kubernetes node traffic trong private network
```

Kết quả:

```text
staging-api.example.com -> staging cluster
uat-api.example.com     -> UAT cluster
api.example.com         -> production cluster
```

Lúc này bạn đã có Topology C đúng nghĩa: 3 cluster riêng.
Production đã có worker riêng, nhưng control-plane vẫn chưa HA.

## 7.5. Bước 5: nâng production thành 1 control-plane + 3 worker

Mục tiêu:

```text
production bắt đầu giống cluster thật hơn
control-plane không chạy app business nặng
rải pod production ra nhiều worker
```

Hạ tầng:

```text
production:
  1 control-plane
  3 worker
```

Việc cần làm:

```text
1. Dựng lại production cluster dạng multi-node
2. Join 3 worker vào production cluster
3. Đặt app production chạy trên worker
4. Không đặt app business trên control-plane
5. Set replicas production = 3
6. Cấu hình podAntiAffinity hoặc topologySpreadConstraints
7. Test rolling update
```

## 7.6. Bước 6: nâng production thành 3 control-plane + 3 worker

Mục tiêu:

```text
production control-plane HA
etcd có quorum
cluster vẫn điều khiển được khi một control-plane lỗi
```

Hạ tầng:

```text
production:
  3 control-plane
  3 worker
```

Việc cần làm:

```text
1. Dựng production control-plane HA
2. Join 3 worker vào cluster
3. Backup etcd
4. Test tắt 1 control-plane
5. Kiểm tra cluster vẫn hoạt động
```

## 7.7. Bước 7: hoàn thiện monitoring, secret, backup

Mục tiêu:

```text
biết hệ thống lỗi ở đâu
secret không nằm plain text trong Git
có đường rollback/restore
```

Cần có:

```text
monitoring:
  Prometheus
  Grafana
  Alertmanager

logging:
  Fluent Bit hoặc Vector
  Loki/OpenSearch tùy tài nguyên

secret:
  External Secrets Operator
  Vault/secret manager nếu có
  hoặc Sealed Secrets cho lab

backup:
  config repo
  registry
  database
  etcd production
```

## 7.8. Bảng tổng hợp mốc hạ tầng

| Mốc | Staging | UAT | Production | Tổng VPS |
|---|---:|---:|---:|---:|
| Bước 1 | 1 cp + 1 worker | - | - | 2 |
| Bước 3 | 1 cp + 1 worker | 1 cp + 2 worker | - | 5 |
| Bước 4 | 1 cp + 1 worker | 1 cp + 2 worker | 1 cp + 2 worker | 8 |
| Bước 5 | 1 cp + 1 worker | 1 cp + 2 worker | 1 cp + 3 worker | 9 |
| Bước 6, bản cuối | 1 cp + 1 worker | 1 cp + 2 worker | 3 cp + 3 worker | 11 |

Trong đó:

```text
cp = control-plane
worker = node chạy app
```

---

# 8. Program/instance nên đặt ở đâu?

## 8.1. Staging

Staging là nơi test sớm.
Không cần quá mạnh.

```text
namespace argocd:
  Argo CD staging

namespace ingress-nginx:
  ingress-nginx

namespace staging:
  gateway: 1 replica
  uaa-service: 1 replica
  post-service: 1 replica
  database demo nếu cần
  redis demo nếu cần
```

Nếu thiếu RAM, có thể không cài monitoring ở staging lúc đầu.

## 8.2. UAT

UAT nên giống production hơn staging.

```text
namespace argocd:
  Argo CD UAT

namespace ingress-nginx:
  ingress-nginx

namespace uat:
  gateway: 1-2 replicas
  uaa-service: 1-2 replicas
  post-service: 1-2 replicas
```

UAT nên có database riêng hoặc schema riêng.
Không nên dùng chung database staging.

## 8.3. Production

Production là nơi chạy thật.
Nên cẩn thận hơn.

```text
namespace argocd:
  Argo CD production

namespace ingress-nginx:
  ingress-nginx

namespace external-secrets:
  external-secrets

namespace monitoring:
  prometheus
  grafana
  alertmanager

namespace production:
  gateway: 2-3 replicas
  uaa-service: 2-3 replicas
  post-service: 2-3 replicas
```

Production database nên để ngoài cluster hoặc dùng database platform riêng nếu học theo hướng enterprise.

---

# 9. Vì sao replica nhiều chưa đủ?

Nhiều người mới nghĩ:

```text
replicas=3 là an toàn
```

Nhưng chưa chắc.

Nếu cả 3 pod nằm trên cùng một worker:

```text
worker-1:
  uaa-service-1
  uaa-service-2
  uaa-service-3
```

Khi `worker-1` chết:

```text
uaa-service mất cả 3 pod
```

Vì vậy cần rải pod ra nhiều worker:

```text
worker-1:
  uaa-service-1

worker-2:
  uaa-service-2

worker-3:
  uaa-service-3
```

Trong Kubernetes có thể dùng:

```text
podAntiAffinity
topologySpreadConstraints
```

Người mới chưa cần cấu hình ngay từ đầu.
Nhưng cần hiểu mục tiêu: **pod cùng service không nên dồn hết vào một node**.

---

# 10. Argo CD trong Topology C

Có hai cách phổ biến.

## 10.1. Argo CD mỗi cluster

```text
cluster staging:
  Argo CD staging

cluster uat:
  Argo CD UAT

cluster production:
  Argo CD production
```

Ưu điểm:

- Dễ hiểu.
- Mỗi môi trường tự quản lý deploy của nó.
- Production tách riêng, ít rủi ro hơn.
- Người mới học dễ nhìn luồng.

Nhược điểm:

- Phải cài nhiều Argo CD.
- Phải quản lý nhiều instance.

Khuyến nghị cho người học:

```text
nên bắt đầu bằng Argo CD mỗi cluster
```

## 10.2. Argo CD central

```text
1 Argo CD quản lý staging, UAT, production
```

Ưu điểm:

- Quản lý tập trung.
- Hợp với platform team.

Nhược điểm:

- Quyền production phải làm rất cẩn thận.
- Nếu Argo CD central lỗi, nhiều môi trường bị ảnh hưởng.
- Người mới dễ khó hiểu hơn.

Với hệ thống ví/fintech/bank, production thường được tách quyền rất mạnh.
Vì vậy nếu dùng central Argo CD, phần RBAC, project, cluster credential, approval phải thiết kế kỹ.

---

# 11. GitOps repo cho Topology C

Một cấu trúc dễ hiểu:

```text
platform-config-repo/
  apps/
    gateway/
      base/
      overlays/
        staging/
        uat/
        production/

    uaa-service/
      base/
      overlays/
        staging/
        uat/
        production/

    post-service/
      base/
      overlays/
        staging/
        uat/
        production/

  clusters/
    staging/
      applications/

    uat/
      applications/

    production/
      applications/
```

Luồng promote image:

```text
Developer merge code
  -> CI build image
  -> push registry
  -> update staging image tag
  -> Argo CD staging sync
  -> QA ok
  -> promote cùng image tag sang UAT
  -> UAT ok
  -> promote cùng image tag sang production bằng merge request có approval
  -> Argo CD production sync
```

Điểm quan trọng:

```text
build một lần
deploy nhiều môi trường
không build lại image riêng cho production
```

---

# 12. Những thứ không nên nhồi vào VPS nhỏ

Nếu VPS chỉ có 4 GB RAM, đừng cố chạy quá nhiều thứ.

Nên tránh ở giai đoạn đầu:

```text
GitLab self-managed
Harbor full
Kafka cluster
ELK/OpenSearch
Loki retention dài
Prometheus scrape quá nhiều metric
database production-like
Vault HA
```

Nên bắt đầu với:

```text
k3s
Argo CD
ingress-nginx
3 app demo
sealed-secrets hoặc external-secrets
```

Sau khi đã hiểu topology, mới thêm:

```text
monitoring
logging
registry riêng
secret manager
database nâng cao
```

---

# 13. Checklist khi học Topology C

## 13.1. Sau bước 1: dựng staging 2 VPS

Bạn nên trả lời được:

- Cluster staging ở đâu?
- `kubectl config get-contexts` có thấy context staging không?
- Staging control-plane private IP là gì?
- Staging worker private IP là gì?
- Worker join control-plane qua IP nào?
- Firewall đang mở port nào ra public, port nào chỉ mở nội bộ?
- Argo CD staging sync app nào?
- Host staging trỏ về đâu?
- Gateway, UAA, post-service chạy trong namespace nào?
- Khi sửa image tag staging trong GitOps repo, Argo CD có sync không?

## 13.2. Sau bước 4: có staging, UAT, production đều nhiều VPS

Bạn nên trả lời được:

- Cluster staging ở đâu?
- Cluster UAT ở đâu?
- Cluster production ở đâu?
- `kubectl config get-contexts` có thấy 3 context không?
- Image tag promote từ staging lên UAT rồi lên production thế nào?
- Host staging/UAT/prod khác nhau ra sao?
- Production control-plane nằm ở VPS nào?
- Production worker nằm ở VPS nào?
- Node traffic production có đi qua private network không?
- Port Kubernetes production có bị public bừa ra Internet không?

## 13.3. Sau bước 5: production có 3 worker

Bạn nên trả lời được:

- Production control-plane nằm trên VPS nào?
- Production worker nằm trên VPS nào?
- Nếu worker production chết thì app bị ảnh hưởng thế nào?
- Nếu control-plane production chết thì app đang chạy còn phục vụ không?
- Vì sao control-plane không nên chạy app nặng?
- Pod production đã rải ra 3 worker chưa?

## 13.4. Sau bước 6: production có 3 control-plane và 3 worker

Bạn nên trả lời được:

- Pod cùng service đã rải ra nhiều worker chưa?
- Rolling update có đủ tài nguyên không?
- Monitoring có cảnh báo khi pod restart không?
- Production secret lấy từ đâu?
- Ai có quyền sync production?
- Ai có quyền merge config production?
- Nếu deploy lỗi thì rollback bằng Git revert hay Argo CD rollback?

---

# 14. Lộ trình thực hành chốt lại

Đích cuối không đổi:

```text
staging:
  1 control-plane
  1 worker

uat:
  1 control-plane
  2 worker

production:
  3 control-plane
  3 worker
```

Tức là:

```text
11 VPS / node
```

Để đi tới đó mà không bị ngợp, thực hành theo thứ tự:

```text
Bước 1:
  dựng staging 2 VPS
  1 control-plane + 1 worker

Bước 2:
  chuẩn hóa GitOps repo trên staging

Bước 3:
  nhân lên UAT 3 VPS
  1 control-plane + 2 worker

Bước 4:
  nhân lên production 3 VPS
  1 control-plane + 2 worker

Bước 5:
  production có 1 control-plane + 3 worker

Bước 6:
  production có 3 control-plane + 3 worker

Bước 7:
  hoàn thiện monitoring, secret, backup
```

Điều quan trọng là mỗi bước đều phục vụ bản cuối:

```text
staging trước
  -> học một cluster có nhiều VPS cho chắc

UAT sau staging
  -> học promote và kiểm thử trước production

production sau UAT
  -> tách môi trường chạy thật

tách worker khỏi control-plane
  -> control-plane ổn định hơn

nhiều worker
  -> app chịu lỗi tốt hơn

3 control-plane
  -> cluster điều khiển HA hơn

network/security từng môi trường
  -> học private IP, firewall, node join, ingress public/private

GitOps promotion
  -> deploy production có kiểm soát hơn
```

---

# 15. Kết luận

Bản ultimate trong tài liệu này là:

```text
staging cluster:
  1 control-plane
  1 worker

uat cluster:
  1 control-plane
  2 worker

production cluster:
  3 control-plane
  3 worker
```

Tổng ước tính:

```text
11 VPS / node
46-56 vCPU
92-112 GB RAM
1.0-1.7 TB SSD
4.2 - 8.1 triệu/tháng nếu thuê VPS Việt Nam, tùy nhà cung cấp
```

Lộ trình build:

```text
staging 2 VPS
  -> GitOps repo chuẩn trên staging
  -> UAT 3 VPS
  -> production 3 VPS
  -> production có 3 worker
  -> production có 3 control-plane
  -> monitoring, secret, backup
```

Không nên nghĩ “nhiều replica là đủ”.
Replica chỉ thật sự có ý nghĩa khi pod được rải ra nhiều worker.

Không nên nghĩ “control-plane và worker chung là sai”.
Trong lab thì đúng và tiết kiệm.
Trong production thì nên tách vì control-plane cần ổn định để điều khiển cả cluster.
