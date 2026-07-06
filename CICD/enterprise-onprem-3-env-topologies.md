# Topologies CI/CD on-prem cho 3 môi trường staging, UAT, production

Tài liệu này mô tả các topology phổ biến trong doanh nghiệp khi triển khai hệ thống microservices on-prem với 3 môi trường:

```text
staging
uat
production
```

Bối cảnh giả định:

- Có GitLab on-prem hoặc GitLab self-managed.
- Có GitLab CI/CD.
- Có GitLab Runner.
- Có Docker/container image.
- Có Kubernetes on-prem.
- Có Argo CD để deploy theo GitOps.
- Có một repo cấu hình/deployment riêng, trong tài liệu này gọi là `config repo` hoặc `gitops repo`.
- Mỗi khi thay đổi cấu hình application của từng môi trường, team update code/config vào repo này.
- Có các service ví dụ:
  - `uaa-service`: quản lý user, auth, token, permission.
  - `gateway`: API gateway, route request vào các service nội bộ.
  - `post-service`: quản lý bài viết/post.
  - `nginx` hoặc ingress controller/reverse proxy.
  - database, redis, kafka hoặc các dependency khác nếu cần.

Mục tiêu của tài liệu:

- Hiểu doanh nghiệp thường dựng topology on-prem như thế nào.
- Biết mỗi cluster chứa gì.
- Biết công cụ nào chạy ở đâu.
- Biết instance/node phân bổ thế nào.
- Biết config repo nên tổ chức ra sao.
- Có số node cụ thể để thực hành tiết kiệm nhưng vẫn sát thực tế.

---

## 1. Nguyên tắc nền tảng trong doanh nghiệp

Trước khi chọn topology, cần thống nhất vài nguyên tắc.

### 1.1. CI và CD tách trách nhiệm

CI chịu trách nhiệm tạo artifact:

```text
source code
  -> test
  -> package
  -> build image
  -> scan image
  -> push registry
```

CD chịu trách nhiệm đưa artifact vào môi trường chạy:

```text
gitops repo
  -> Argo CD sync
  -> Kubernetes rollout
```

Không nên để production phụ thuộc vào việc build lại image.

Đúng hơn:

```text
Build một lần
  -> image tag immutable
  -> deploy staging
  -> promote UAT
  -> promote production
```

Ví dụ:

```text
registry.company.local/social/uaa-service:8f31a2c
registry.company.local/social/gateway:92bd441
registry.company.local/social/post-service:cc901de
```

Ba môi trường có thể dùng cùng một image tag ở các thời điểm khác nhau.

### 1.2. GitOps repo là nguồn sự thật cho deployment

Source repo chứa code application:

```text
uaa-service repo
gateway repo
post-service repo
```

Config/GitOps repo chứa trạng thái mong muốn của môi trường:

```text
staging đang chạy image nào
uat đang chạy image nào
production đang chạy image nào
biến môi trường nào
replica bao nhiêu
resource request/limit thế nào
ingress host là gì
```

Argo CD đọc config repo và sync vào Kubernetes.

Luồng đúng:

```text
Developer push code
  -> GitLab CI build image
  -> GitLab CI update config repo
  -> Argo CD sync config repo vào cluster
```

### 1.3. Không hard-code secret vào Git

Config repo có thể chứa:

```text
replica count
image tag
hostname
timeout
feature flag không nhạy cảm
resource request/limit
```

Config repo không nên chứa plain text:

```text
database password
JWT private key
OAuth client secret
registry token
API key
```

Secret nên đi qua một trong các cơ chế:

```text
Vault + External Secrets Operator
Sealed Secrets
SOPS
Kubernetes Secret tạo bởi pipeline an toàn
secret manager nội bộ
```

### 1.4. Production nên tách quyền nghiêm ngặt

Thông thường:

```text
staging
  -> auto deploy hoặc auto sync được

uat
  -> deploy sau khi QA/UAT approve

production
  -> manual approval, manual sync, change request hoặc CAB
```

GitLab Runner không nên có kubeconfig production nếu đã dùng Argo CD GitOps.

Production deploy nên qua:

```text
merge request vào config repo production path
  -> approval
  -> merge
  -> Argo CD production sync
```

---

## 2. Các topology phổ biến nhất

Có 4 topology doanh nghiệp hay dùng.

```text
Topology A: Một cluster, tách namespace theo môi trường
Topology B: Hai cluster, non-prod và prod
Topology C: Ba cluster, staging/UAT/prod tách riêng
Topology D: Platform cluster riêng, workload cluster riêng
```

Thứ tự trưởng thành thường là:

```text
A -> B -> C -> D
```

Nếu thuê hạ tầng tốn kém, nên bắt đầu thực hành bằng topology A hoặc B.

---

## 2.1. Ba keyword cần hiểu trước: namespace, control-plane, worker

Đọc tài liệu Kubernetes rất dễ bị ngợp vì nhiều tên mới.
Ở đây chỉ cần hiểu theo cách đơn giản trước.
Hãy tưởng tượng một Kubernetes cluster giống một tòa nhà văn phòng.

### Namespace

`namespace` giống như một phòng hoặc một khu riêng trong cùng tòa nhà.
Tòa nhà vẫn là một, nhưng mỗi phòng có tên riêng, đồ đạc riêng, người được vào riêng.

Ví dụ trong cùng một cluster:

```text
namespace staging
namespace uat
namespace production
```

Trong mỗi namespace có thể có app và config riêng:

```text
gateway
uaa-service
post-service
configmap
secret
ingress
```

Điểm cần nhớ:

- `deployment/gateway` trong `staging` khác với `deployment/gateway` trong `production`.
- Có thể cho dev chỉ được đụng vào `staging`, còn production chỉ team vận hành được đụng.
- Có thể giới hạn staging chỉ được dùng một lượng CPU/RAM nhất định.
- Có thể chặn app ở staging gọi nhầm sang production.

Nhưng namespace không phải là một tòa nhà riêng.
Nếu điện, mạng, thang máy hoặc nền móng của tòa nhà gặp vấn đề, nhiều phòng vẫn bị ảnh hưởng.
Trong Kubernetes cũng vậy: nếu cluster, node, storage, network plugin hoặc ingress chung bị lỗi, nhiều namespace có thể ảnh hưởng cùng lúc.

### Control-plane

`control-plane` giống như ban quản lý của tòa nhà.
Nó không phải nơi nhân viên ngồi làm việc chính, mà là nơi nhận yêu cầu, ghi sổ, phân phòng, kiểm tra mọi thứ có đang đúng kế hoạch không.

Nói dễ hiểu, control-plane làm các việc này:

- Nhận lệnh từ bạn, Argo CD hoặc CI/CD.
- Ghi nhớ cluster nên có những app nào, chạy mấy bản.
- Chọn worker phù hợp để đặt pod.
- Theo dõi nếu pod chết thì tạo lại pod khác.

Ví dụ:

```text
Bạn muốn gateway chạy 3 bản
  -> control-plane ghi nhận mong muốn đó
  -> control-plane chọn worker còn tài nguyên
  -> worker chạy 3 pod gateway
```

Nếu control-plane lỗi, app đang chạy có thể vẫn còn phục vụ trong một thời gian.
Nhưng bạn sẽ khó deploy mới, scale, rollback, hoặc để Kubernetes tự chữa khi có pod/node lỗi.
Vì vậy production nghiêm túc thường có 3 control-plane node.

### Worker

`worker` giống như các tầng/phòng làm việc thật.
Đây là nơi app của bạn chạy.

Ví dụ worker chạy:

```text
gateway pod
uaa-service pod
post-service pod
ingress-nginx pod
prometheus pod
redis demo
postgres demo
```

Nếu nói cực ngắn:

```text
control-plane: nơi điều khiển
worker: nơi app chạy
namespace: phòng/khu riêng bên trong cluster
```

Trong lab nhỏ:

```text
1 VPS có thể vừa là control-plane vừa là worker
```

Trong production:

```text
control-plane nên tách riêng
worker nên dành để chạy app
production nên có nhiều worker để một node chết app vẫn còn nơi khác chạy
```

---

# Topology A. Một cluster, ba namespace môi trường

## 3.1. Khi nào dùng topology này?

Topology này dùng một Kubernetes cluster duy nhất, trong đó mỗi môi trường là một namespace.

```text
k8s-cluster-shared
  namespace staging
  namespace uat
  namespace production
```

Phù hợp khi:

- Team nhỏ hoặc vừa.
- Hạ tầng hạn chế.
- Muốn tiết kiệm chi phí.
- Hệ thống chưa yêu cầu isolation quá mạnh.
- Đang ở giai đoạn học, PoC, pilot hoặc non-critical production.

Không lý tưởng khi:

- Production yêu cầu isolation cao.
- Cần compliance nghiêm ngặt.
- Staging/UAT có workload nặng có thể ảnh hưởng production.
- Team vận hành chưa kiểm soát tốt quota, RBAC, network policy.

## 3.2. Sơ đồ tổng quan

Trong topology A, cả 3 môi trường nằm trong cùng một cluster.
Chúng được tách bằng namespace, giống như 3 phòng riêng trong cùng một tòa nhà.

```text
                         +-----------------------+
                         |      GitLab           |
                         | source repo + CI      |
                         +-----------+-----------+
                                     |
                                     | build image
                                     v
                         +-----------+-----------+
                         | Container Registry    |
                         | Harbor/GitLab Registry|
                         +-----------+-----------+
                                     |
                                     | update image tag
                                     v
                         +-----------+-----------+
                         | Config/GitOps Repo    |
                         +-----------+-----------+
                                     |
                                     | watched by
                                     v
                         +-----------+-----------+
                         | Argo CD               |
                         +-----------+-----------+
                                     |
                                     | sync
                                     v
+-------------------------------------------------------------------+
| Kubernetes shared cluster                                          |
|                                                                    |
|  namespace: staging                                                |
|    gateway, uaa-service, post-service, nginx/ingress route          |
|                                                                    |
|  namespace: uat                                                    |
|    gateway, uaa-service, post-service, nginx/ingress route          |
|                                                                    |
|  namespace: production                                             |
|    gateway, uaa-service, post-service, nginx/ingress route          |
|                                                                    |
|  namespace: argocd                                                 |
|    argocd-server, repo-server, application-controller, dex          |
|                                                                    |
|  namespace: ingress-nginx                                          |
|    ingress controller                                               |
|                                                                    |
|  namespace: monitoring                                             |
|    prometheus, grafana, alertmanager                                |
|                                                                    |
|  namespace: external-secrets                                       |
|    external-secrets operator                                        |
+-------------------------------------------------------------------+
```

## 3.3. Thuê VPS và đặt chương trình thế nào cho hợp lý

Topology A chỉ cần một cluster, nên nếu thuê VPS Việt Nam để học thì có thể bắt đầu rất tiết kiệm.

### Phương án A1: một VPS lớn, dễ quản lý nhất

```text
1 VPS: 6 CPU, 8 GB RAM
  cài k3s single-node
```

Đặt chương trình:

```text
namespace argocd:
  Argo CD

namespace ingress-nginx:
  ingress-nginx controller

namespace staging:
  gateway, uaa-service, post-service

namespace uat:
  gateway, uaa-service, post-service

namespace production:
  gateway, uaa-service, post-service

namespace external-secrets:
  external-secrets hoặc sealed-secrets nếu muốn học secret flow
```

Không nên đặt trong lab nhỏ:

```text
GitLab self-managed
Harbor full
ELK/OpenSearch full
Kafka cluster
database production-like
```

Các thành phần này ăn RAM/disk mạnh, dễ làm bạn học topology thành bài toán chữa cháy tài nguyên.

### Phương án A2: nhiều VPS để hiểu control-plane và worker

```text
VPS 1: 2 CPU, 4 GB RAM
  role: control-plane

VPS 2: 4 CPU, 4-8 GB RAM
  role: worker

VPS 3: 4 CPU, 4-8 GB RAM
  role: worker
```

Placement nên dùng:

```text
control-plane:
  kube-apiserver, etcd, scheduler, controller-manager
  hạn chế chạy app nếu có thể

worker-1:
  ingress-nginx
  gateway staging/uat/production
  uaa-service staging/uat

worker-2:
  post-service staging/uat/production
  uaa-service production
  Argo CD
  monitoring nhẹ nếu còn RAM
```

Với topology A, production vẫn chung cluster với staging/UAT.
Vì vậy đây là topology tốt để học namespace, RBAC, ResourceQuota, NetworkPolicy, nhưng không nên xem là mẫu production cho hệ thống tài chính.

## 3.4. Bản doanh nghiệp nhỏ nếu vẫn dùng topology A

Nếu doanh nghiệp thật vẫn chọn topology A, tối thiểu nên nghĩ theo cụm HA nhỏ:

```text
3 control-plane node
3 worker node
```

Control-plane 3 node giúp etcd/control-plane HA hơn.
Worker 3 node giúp app còn chỗ chạy khi một worker lỗi hoặc khi rolling update.
Tuy nhiên với hệ thống quan trọng, nên cân nhắc topology B hoặc C thay vì để production chung cluster với non-prod.

## 3.5. Namespace layout

```text
argocd
ingress-nginx
monitoring
logging
external-secrets
staging
uat
production
```

Trong `staging`:

```text
gateway
uaa-service
post-service
postgres hoặc external database endpoint
redis nếu cần
configmap
secret
service
ingress
```

Trong `uat`:

```text
gateway
uaa-service
post-service
config riêng cho UAT
secret riêng cho UAT
database riêng hoặc schema riêng
```

Trong `production`:

```text
gateway
uaa-service
post-service
replica cao hơn
resource limit chặt hơn
secret production
ingress production host
```

## 3.6. Service layout theo môi trường

Ví dụ staging:

```text
namespace staging
  deployment/gateway
  service/gateway
  ingress/gateway

  deployment/uaa-service
  service/uaa-service
  configmap/uaa-config
  secret/uaa-secret

  deployment/post-service
  service/post-service
  configmap/post-config
  secret/post-secret
```

Luồng request:

```text
Client
  -> DNS staging-api.company.local
  -> Load Balancer VIP
  -> ingress-nginx
  -> gateway service
  -> gateway pod
  -> uaa-service/post-service
```

UAA service quản lý auth:

```text
Client login
  -> gateway
  -> uaa-service
  -> database user
  -> trả token
```

Post service dùng token:

```text
Client gọi API post
  -> gateway validate hoặc forward token
  -> post-service
  -> nếu cần kiểm tra user/permission
  -> gọi uaa-service nội bộ hoặc verify JWT
```

## 3.7. Ingress/nginx đặt ở đâu?

Trong Kubernetes, thường không chạy một `nginx` riêng cho từng app nếu mục tiêu là expose HTTP chung. Doanh nghiệp thường dùng:

```text
ingress-nginx controller
```

Nó chạy trong namespace:

```text
ingress-nginx
```

Các app tạo `Ingress` trong namespace riêng.

Ví dụ:

```text
Ingress staging/gateway
  host: staging-api.company.local
  service: gateway

Ingress uat/gateway
  host: uat-api.company.local
  service: gateway

Ingress production/gateway
  host: api.company.local
  service: gateway
```

Nếu doanh nghiệp có nginx ngoài cluster:

```text
External NGINX / F5 / HAProxy
  -> NodePort hoặc LoadBalancer ingress-nginx
  -> ingress controller trong cluster
```

## 3.8. Ưu điểm topology A

- Rẻ nhất.
- Dễ thực hành.
- Dễ quản lý ban đầu.
- Chỉ cần một cluster.
- Dễ demo đủ staging, UAT, production.

## 3.9. Nhược điểm topology A

- Production chung cluster với staging/UAT.
- Nếu cluster lỗi, cả 3 môi trường ảnh hưởng.
- Workload staging có thể ăn tài nguyên production nếu không có quota.
- RBAC, NetworkPolicy, ResourceQuota phải làm cẩn thận.
- Không phù hợp hệ thống production quan trọng.

## 3.10. Khi thực hành nên dùng topology A thế nào?

Nếu bạn muốn tiết kiệm hạ tầng nhất, hãy dùng:

```text
1 cluster kind/k3s/rke2 nhỏ
3 namespace: staging, uat, production
1 namespace argocd
1 namespace ingress-nginx
```

Ví dụ với `kind`:

```text
1 control-plane
2 workers
```

Bạn vẫn học được:

- tách environment;
- Argo CD Application theo từng environment;
- config repo theo từng environment;
- image tag promotion;
- ingress host khác nhau;
- resource khác nhau;
- secret khác nhau.

---

# Topology B. Hai cluster: non-prod và production

## 4.1. Khi nào dùng topology này?

Đây là topology rất phổ biến trong doanh nghiệp vừa.

```text
cluster non-prod
  namespace staging
  namespace uat

cluster production
  namespace production
```

Phù hợp khi:

- Muốn production tách khỏi staging/UAT.
- Hạ tầng có giới hạn nhưng vẫn cần isolation cơ bản.
- Team chưa muốn vận hành 3 cluster riêng.
- Production quan trọng hơn non-prod rõ ràng.

Đây thường là lựa chọn cân bằng giữa:

```text
chi phí
độ an toàn
độ phức tạp vận hành
```

## 4.2. Sơ đồ tổng quan

```text
                    +-------------------------+
                    | GitLab + CI             |
                    +------------+------------+
                                 |
                                 v
                    +------------+------------+
                    | Registry                |
                    +------------+------------+
                                 |
                                 v
                    +------------+------------+
                    | Config/GitOps Repo      |
                    +------+-------------+----+
                           |             |
                           |             |
                           v             v
              +------------+---+     +---+-------------+
              | Argo CD nonprod|     | Argo CD prod    |
              +------------+---+     +---+-------------+
                           |             |
                           v             v
+--------------------------+--+     +----+-------------------------+
| Kubernetes non-prod          |     | Kubernetes production         |
|                              |     |                              |
| namespace staging            |     | namespace production          |
| namespace uat                |     |                              |
| namespace ingress-nginx      |     | namespace ingress-nginx       |
| namespace monitoring         |     | namespace monitoring          |
| namespace external-secrets   |     | namespace external-secrets    |
+------------------------------+     +------------------------------+
```

Có thể đặt Argo CD theo hai cách.

Cách phổ biến và an toàn:

```text
Argo CD nonprod chạy trên cluster non-prod
Argo CD prod chạy trên cluster production
```

Cách tập trung:

```text
Một Argo CD quản lý cả non-prod và prod
```

Với production, doanh nghiệp thường thích Argo CD riêng cho prod hơn để giảm blast radius.

## 4.3. Thuê VPS và đặt chương trình thế nào cho hợp lý

Topology B là lựa chọn rất đáng học bằng nhiều VPS, vì nó cho bạn cảm giác production đã tách khỏi môi trường test.

### Phương án B1: hai VPS, tiết kiệm nhất

```text
VPS 1: 4-6 CPU, 8 GB RAM
  cluster: non-prod
  namespace: staging, uat, argocd, ingress-nginx

VPS 2: 4-6 CPU, 8 GB RAM
  cluster: production
  namespace: production, argocd, ingress-nginx
```

Đặt chương trình:

```text
VPS non-prod:
  k3s single-node
  Argo CD non-prod
  ingress-nginx
  gateway/uaa/post cho staging
  gateway/uaa/post cho uat
  monitoring nhẹ nếu còn RAM

VPS production:
  k3s single-node
  Argo CD prod
  ingress-nginx
  gateway/uaa/post production
  external-secrets hoặc sealed-secrets
```

Phương án này đủ học:

```text
prod tách non-prod
2 kubeconfig context
2 Argo CD instance
promotion staging -> uat -> production
manual approval cho production
```

Nhưng nó chưa HA, vì mỗi cluster chỉ có một VPS.

### Phương án B2: bốn VPS, gần thực tế hơn

```text
VPS 1: 2 CPU, 4 GB RAM
  non-prod control-plane

VPS 2: 4-6 CPU, 8 GB RAM
  non-prod worker

VPS 3: 2 CPU, 4 GB RAM
  production control-plane

VPS 4: 6 CPU, 8 GB RAM
  production worker
```

Placement:

```text
non-prod control-plane:
  kube-apiserver, etcd, scheduler, controller-manager
  có thể đặt Argo CD nếu worker thiếu RAM

non-prod worker:
  ingress-nginx
  staging apps
  uat apps
  monitoring nhẹ

production control-plane:
  kube-apiserver, etcd, scheduler, controller-manager
  hạn chế chạy workload

production worker:
  ingress-nginx
  production apps
  Argo CD prod nếu muốn tách rõ control-plane
```

### Phương án B3: doanh nghiệp nhỏ đến vừa

```text
non-prod:
  3 control-plane nhỏ
  2-3 worker vừa

production:
  3 control-plane
  tối thiểu 3 worker
```

Production thật cần headroom cho rolling update, node maintenance và failover.
Nếu chỉ có một production worker, pod có nhiều replica nhưng vẫn có thể chết cùng một node.

## 4.4. App instance phân bổ như thế nào?

Non-prod staging:

```text
gateway:      1-2 replicas
uaa-service:  1-2 replicas
post-service: 1-2 replicas
```

Non-prod UAT:

```text
gateway:      2 replicas
uaa-service:  2 replicas
post-service: 2 replicas
```

Production:

```text
gateway:      3 replicas
uaa-service:  3 replicas
post-service: 3 replicas
```

Vì sao production thường tối thiểu 3 replicas?

```text
1 pod lỗi
  -> còn pod khác phục vụ

rolling update
  -> có pod cũ và mới cùng tồn tại

node maintenance
  -> pod có thể phân tán sang nhiều node
```

## 4.5. Placement theo node

Production nên cố gắng phân tán pod cùng service ra nhiều node.

Dùng:

```text
podAntiAffinity
topologySpreadConstraints
```

Ví dụ mong muốn:

```text
worker-1:
  gateway-1
  uaa-service-1
  post-service-1

worker-2:
  gateway-2
  uaa-service-2
  post-service-2

worker-3:
  gateway-3
  uaa-service-3
  post-service-3
```

Không nên để cả 3 pod `uaa-service` nằm trên cùng một node.

Nếu node đó chết:

```text
uaa-service mất toàn bộ replicas
```

## 4.6. Ưu điểm topology B

- Production tách khỏi non-prod.
- Chi phí thấp hơn 3 cluster riêng.
- Vận hành vừa phải.
- Phù hợp doanh nghiệp vừa.
- Dễ chuyển tiếp từ topology A.

## 4.7. Nhược điểm topology B

- Staging và UAT vẫn chung cluster.
- Nếu non-prod cluster lỗi, cả staging và UAT mất.
- Cần quản lý hai Argo CD hoặc một Argo CD multi-cluster.
- Cần config repo phân biệt cluster rõ ràng.

---

# Topology C. Ba cluster riêng cho staging, UAT, production

## 5.1. Khi nào dùng topology này?

Mỗi môi trường có cluster riêng.

```text
cluster staging
cluster uat
cluster production
```

Phù hợp khi:

- Doanh nghiệp lớn.
- Production cần cô lập mạnh.
- UAT cần gần giống production.
- Có nhiều team dùng chung platform.
- Có yêu cầu compliance/audit cao.
- Có đủ ngân sách và năng lực vận hành.

## 5.2. Sơ đồ tổng quan

```text
                              +------------------+
                              | GitLab           |
                              +--------+---------+
                                       |
                                       v
                              +--------+---------+
                              | Registry         |
                              +--------+---------+
                                       |
                                       v
                              +--------+---------+
                              | Config Repo      |
                              +---+----+----+----+
                                  |    |    |
                                  |    |    |
                                  v    v    v
                  +---------------+    |    +----------------+
                  |                    |                     |
        +---------+--------+  +--------+---------+  +--------+---------+
        | Argo CD staging  |  | Argo CD UAT      |  | Argo CD prod     |
        +---------+--------+  +--------+---------+  +--------+---------+
                  |                    |                     |
                  v                    v                     v
        +---------+--------+  +--------+---------+  +--------+---------+
        | Cluster staging  |  | Cluster UAT      |  | Cluster prod     |
        +------------------+  +------------------+  +------------------+
```

## 5.3. Thuê VPS và đặt chương trình thế nào cho hợp lý

Topology C nghĩa là mỗi môi trường có cluster riêng.
Nếu ví cluster như tòa nhà, thì staging, UAT và production là 3 tòa nhà khác nhau, không chỉ là 3 phòng trong cùng một tòa nhà.

### Phương án C1: ba VPS, học được đúng ý chính

```text
VPS 1: 4 CPU, 4-8 GB RAM
  cluster staging

VPS 2: 4 CPU, 4-8 GB RAM
  cluster uat

VPS 3: 6 CPU, 8 GB RAM
  cluster production
```

Mỗi VPS cài một cluster k3s single-node.
Nghĩa là mỗi VPS vừa là control-plane vừa là worker.

Đặt chương trình:

```text
staging VPS:
  Argo CD staging
  ingress-nginx
  gateway/uaa/post staging
  database demo nếu cần

uat VPS:
  Argo CD UAT
  ingress-nginx
  gateway/uaa/post UAT
  database demo nếu cần

production VPS:
  Argo CD prod
  ingress-nginx
  gateway/uaa/post production
  external-secrets hoặc sealed-secrets
  monitoring nhẹ
```

Phương án này rất hợp để học:

```text
3 kubeconfig context
3 Argo CD Application set
config repo tách theo cluster
promote image tag từ staging -> UAT -> production
DNS/ingress host riêng từng môi trường
```

Điểm yếu:

```text
mỗi cluster chỉ có một VPS
không chịu được lỗi VPS
chưa mô phỏng HA thật
```

### Phương án C2: năm VPS, production học thực tế hơn

```text
VPS 1: 4 CPU, 4-8 GB RAM
  cluster staging single-node

VPS 2: 4 CPU, 4-8 GB RAM
  cluster uat single-node

VPS 3: 2 CPU, 4 GB RAM
  production control-plane

VPS 4: 4-6 CPU, 8 GB RAM
  production worker-1

VPS 5: 4-6 CPU, 8 GB RAM
  production worker-2
```

Placement:

```text
staging:
  Argo CD staging
  ingress-nginx
  app staging

uat:
  Argo CD UAT
  ingress-nginx
  app UAT

production control-plane:
  kube-apiserver, etcd, scheduler, controller-manager
  hạn chế chạy app

production worker-1:
  ingress-nginx
  gateway-1
  uaa-service-1
  post-service-1

production worker-2:
  gateway-2
  uaa-service-2
  post-service-2
  Argo CD prod
  monitoring nhẹ
```

Phương án này bắt đầu cho bạn cảm giác production có worker riêng.
Tuy vậy, production vẫn chưa HA chuẩn vì control-plane chỉ có một node và worker mới có 2 node.

### Phương án C3: production-like nhỏ

```text
staging:
  1-2 VPS

uat:
  1-2 VPS

production:
  3 control-plane
  tối thiểu 3 worker
```

Production 3 worker giúp trải pod ra nhiều máy.
Khi rolling update hoặc một worker chết, app còn khả năng chạy ở worker khác.
Đây mới là hướng gần production hơn, nhưng chi phí VPS sẽ tăng rõ rệt.

## 5.4. Ưu điểm topology C

- Isolation rõ nhất theo môi trường.
- Production an toàn hơn.
- UAT có thể mô phỏng production tốt hơn.
- Lỗi staging không ảnh hưởng UAT/prod.
- RBAC và quota dễ tách.

## 5.5. Nhược điểm topology C

- Tốn hạ tầng hơn.
- Vận hành nhiều cluster hơn.
- Upgrade Kubernetes phức tạp hơn.
- Monitoring/logging/secret/ingress phải nhân bản hoặc quản lý tập trung.
- Cần platform team có kinh nghiệm.

---

# Topology D. Platform cluster riêng và workload clusters

## 6.1. Khi nào dùng topology này?

Topology này thường gặp trong doanh nghiệp lớn.

```text
platform cluster:
  GitLab Runner
  Argo CD trung tâm hoặc Argo CD non-prod
  monitoring trung tâm
  logging trung tâm
  registry nếu chạy trong Kubernetes
  Vault hoặc External Secrets controller management

workload clusters:
  staging
  uat
  production
```

## 6.2. Sơ đồ

```text
+--------------------------------------------------+
| Platform Cluster                                  |
|                                                  |
| GitLab Runner                                    |
| Argo CD central                                  |
| Prometheus/Grafana central                       |
| Loki/OpenSearch                                  |
| Vault/External Secret tooling                    |
+----------------------+---------------------------+
                       |
                       | deploy/manage
                       v
        +--------------+--------------+
        |                             |
        v                             v
+-------+--------+          +---------+------+
| non-prod       |          | production     |
| staging, uat   |          | prod workloads |
+----------------+          +----------------+
```

## 6.3. Thuê VPS và đặt chương trình thế nào cho hợp lý

Topology D thêm một cluster/platform riêng.
Nói dễ hiểu: thay vì để từng cluster tự ôm hết mọi công cụ, ta có một khu riêng để đặt đồ nghề chung.

Đồ nghề chung thường là:

```text
GitLab Runner
Argo CD central
Prometheus/Grafana central
Loki/OpenSearch nếu học logging
Harbor registry nếu muốn tự host registry
Vault hoặc External Secrets tooling
```

### Phương án D1: bốn VPS, D mini dễ học

```text
VPS 1: 8 CPU, 16 GB RAM
  cluster platform

VPS 2: 4 CPU, 4-8 GB RAM
  cluster staging

VPS 3: 4 CPU, 4-8 GB RAM
  cluster uat

VPS 4: 6 CPU, 8 GB RAM
  cluster production
```

Placement:

```text
platform VPS:
  Argo CD central hoặc Argo CD non-prod
  GitLab Runner
  Prometheus/Grafana
  Loki nhẹ nếu đủ RAM
  Harbor registry nếu không dùng GitLab Registry/Docker Hub
  Vault hoặc External Secrets demo

staging VPS:
  ingress-nginx
  gateway/uaa/post staging

uat VPS:
  ingress-nginx
  gateway/uaa/post UAT

production VPS:
  Argo CD prod nếu muốn prod tự quản riêng
  ingress-nginx
  gateway/uaa/post production
  external-secrets
```

Với người mới, nên để production có Argo CD riêng thay vì để Argo CD central quản lý tất cả ngay từ đầu.
Lý do: dễ hiểu quyền, dễ thấy prod là vùng nhạy cảm, giảm cảm giác mọi thứ dính vào một chỗ.

### Phương án D2: sáu VPS, học sát enterprise hơn

```text
VPS 1: 4 CPU, 8 GB RAM
  platform control-plane

VPS 2: 8 CPU, 16 GB RAM
  platform worker

VPS 3: 4 CPU, 4-8 GB RAM
  staging cluster

VPS 4: 4 CPU, 4-8 GB RAM
  uat cluster

VPS 5: 2 CPU, 4 GB RAM
  production control-plane

VPS 6: 6-8 CPU, 8-16 GB RAM
  production worker
```

Placement:

```text
platform control-plane:
  control-plane của platform cluster

platform worker:
  GitLab Runner
  Argo CD central/non-prod
  Prometheus/Grafana
  logging nhẹ
  registry/secret tooling

staging:
  app staging
  ingress staging

uat:
  app UAT
  ingress UAT

production control-plane:
  control-plane production

production worker:
  app production
  ingress production
  Argo CD prod nếu tách prod riêng
```

### Nên tránh gì khi mới học D?

Đừng bật tất cả tool nặng cùng lúc.
Hãy đi theo thứ tự:

```text
1. Argo CD
2. ingress-nginx
3. app demo
4. GitLab Runner
5. monitoring nhẹ
6. registry riêng
7. logging
8. Vault/secret nâng cao
```

Nếu bật Harbor, Loki/OpenSearch, Prometheus retention dài, GitLab self-managed cùng lúc trên VPS nhỏ, bạn sẽ tốn phần lớn thời gian xử lý thiếu RAM/disk thay vì học topology.

## 6.4. Khi nào không nên dùng?

Không nên dùng nếu:

- Team nhỏ.
- Chưa vận hành tốt Kubernetes cơ bản.
- Chưa có nhu cầu multi-cluster thật.
- Hạ tầng hạn chế.

Topology D mạnh nhưng dễ over-engineer nếu áp dụng quá sớm.

---

# 7. Config repo/GitOps repo nên tổ chức thế nào?

Bạn nói có một repo config map, mỗi khi thay đổi application của từng môi trường thì up code vào đây. Trong thực tế, repo đó nên gọi là:

```text
config repo
gitops repo
deployment repo
environment repo
```

Tên không quan trọng bằng trách nhiệm:

```text
repo này lưu desired state của môi trường
```

## 7.1. Cấu trúc khuyến nghị với Kustomize

```text
platform-config-repo/
  apps/
    gateway/
      base/
        deployment.yaml
        service.yaml
        ingress.yaml
        kustomization.yaml
      overlays/
        staging/
          kustomization.yaml
          configmap.yaml
          patch-replica.yaml
          patch-resource.yaml
        uat/
          kustomization.yaml
          configmap.yaml
          patch-replica.yaml
          patch-resource.yaml
        production/
          kustomization.yaml
          configmap.yaml
          patch-replica.yaml
          patch-resource.yaml

    uaa-service/
      base/
        deployment.yaml
        service.yaml
        kustomization.yaml
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
      apps/
        gateway.yaml
        uaa-service.yaml
        post-service.yaml
    uat/
      apps/
        gateway.yaml
        uaa-service.yaml
        post-service.yaml
    production/
      apps/
        gateway.yaml
        uaa-service.yaml
        post-service.yaml
```

Trong đó:

- `apps/*/base`: manifest chung.
- `apps/*/overlays/staging`: config riêng staging.
- `apps/*/overlays/uat`: config riêng UAT.
- `apps/*/overlays/production`: config riêng production.
- `clusters/*/apps`: Argo CD Application trỏ tới từng app/environment.

## 7.2. Cấu trúc khuyến nghị với Helm

```text
platform-config-repo/
  charts/
    microservice/
      Chart.yaml
      values.yaml
      templates/
        deployment.yaml
        service.yaml
        ingress.yaml
        configmap.yaml

  values/
    staging/
      gateway.yaml
      uaa-service.yaml
      post-service.yaml
    uat/
      gateway.yaml
      uaa-service.yaml
      post-service.yaml
    production/
      gateway.yaml
      uaa-service.yaml
      post-service.yaml

  argocd/
    staging/
      gateway-app.yaml
      uaa-app.yaml
      post-app.yaml
    uat/
      gateway-app.yaml
      uaa-app.yaml
      post-app.yaml
    production/
      gateway-app.yaml
      uaa-app.yaml
      post-app.yaml
```

Helm phù hợp nếu:

- nhiều service dùng template giống nhau;
- cần parameter hóa nhiều;
- platform team muốn chuẩn hóa chart.

Kustomize phù hợp nếu:

- muốn manifest Kubernetes rõ ràng;
- mỗi service có khác biệt vừa phải;
- muốn dễ đọc diff khi thay đổi image tag/config.

## 7.3. Ví dụ config từng môi trường cho UAA

Staging:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: uaa-config
  namespace: staging
data:
  SPRING_PROFILES_ACTIVE: staging
  TOKEN_TTL_SECONDS: "3600"
  LOGIN_MAX_ATTEMPTS: "10"
  LOG_LEVEL: DEBUG
```

UAT:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: uaa-config
  namespace: uat
data:
  SPRING_PROFILES_ACTIVE: uat
  TOKEN_TTL_SECONDS: "3600"
  LOGIN_MAX_ATTEMPTS: "5"
  LOG_LEVEL: INFO
```

Production:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: uaa-config
  namespace: production
data:
  SPRING_PROFILES_ACTIVE: production
  TOKEN_TTL_SECONDS: "1800"
  LOGIN_MAX_ATTEMPTS: "5"
  LOG_LEVEL: WARN
```

Điểm cần nhớ:

```text
ConfigMap khác nhau theo môi trường.
Image có thể giống nhau khi promote.
Secret phải tách riêng theo môi trường.
```

---

# 8. Argo CD topology với 3 môi trường

## 8.1. Một Argo CD quản lý cả ba môi trường

Phù hợp với topology A.

```text
namespace argocd
  application gateway-staging
  application gateway-uat
  application gateway-production
  application uaa-staging
  application uaa-uat
  application uaa-production
  application post-staging
  application post-uat
  application post-production
```

Ví dụ:

```text
uaa-staging
  repo: platform-config-repo
  path: apps/uaa-service/overlays/staging
  dest namespace: staging

uaa-uat
  repo: platform-config-repo
  path: apps/uaa-service/overlays/uat
  dest namespace: uat

uaa-production
  repo: platform-config-repo
  path: apps/uaa-service/overlays/production
  dest namespace: production
```

Sync policy:

```text
staging:
  automated: true
  prune: true
  selfHeal: true

uat:
  automated: false hoặc true tùy quy trình
  prune: cẩn thận
  selfHeal: true nếu team đồng ý

production:
  automated: false
  manual sync
  prune: rất cẩn thận
  selfHeal: tùy policy
```

## 8.2. Argo CD riêng cho non-prod và prod

Phù hợp với topology B.

```text
cluster non-prod:
  argocd-nonprod
    manages staging
    manages uat

cluster production:
  argocd-prod
    manages production
```

Ưu điểm:

- Production Argo CD tách riêng.
- RBAC production chặt hơn.
- Non-prod lỗi không ảnh hưởng Argo CD production.

## 8.3. Argo CD per cluster

Phù hợp topology C.

```text
staging cluster:
  argocd-staging

uat cluster:
  argocd-uat

production cluster:
  argocd-production
```

Ưu điểm:

- Tách biệt tuyệt đối.
- Dễ audit từng môi trường.
- Ít rủi ro deploy nhầm namespace/cluster.

Nhược điểm:

- Nhiều Argo CD phải vận hành.
- Cần quản lý SSO/RBAC nhiều nơi.

---

# 9. GitLab CI topology với config repo

## 9.1. Source repo của từng service

Ví dụ:

```text
gitlab.company.local/social/uaa-service
gitlab.company.local/social/gateway
gitlab.company.local/social/post-service
```

Mỗi repo có:

```text
src/
Dockerfile
.gitlab-ci.yml
pom.xml hoặc package.json
```

Pipeline source repo:

```text
test
package
build-image
scan-image
push-image
update-config-repo-staging
```

## 9.2. Config repo

Ví dụ:

```text
gitlab.company.local/platform/social-platform-config
```

Pipeline config repo có thể làm:

```text
validate yaml
kustomize build
helm template
policy check
notify Argo CD hoặc để Argo CD tự watch
```

## 9.3. Luồng update image tag

Khi `uaa-service` merge vào `develop`:

```text
GitLab CI build image:
  registry.company.local/social/uaa-service:8f31a2c

CI clone config repo
CI sửa apps/uaa-service/overlays/staging/kustomization.yaml
CI commit:
  "Update uaa-service staging image to 8f31a2c"
CI push config repo
Argo CD uaa-staging auto-sync
```

Khi promote UAT:

```text
Tạo merge request config repo:
  copy image tag từ staging sang uat
QA/UAT approve
merge
Argo CD uaa-uat sync
```

Khi promote production:

```text
Tạo merge request config repo:
  copy image tag từ uat sang production
Change approval
Security/Tech lead approve
merge
Argo CD production manual sync
```

## 9.4. Vì sao không build lại khi promote?

Sai:

```text
staging build image A
uat build lại image B
production build lại image C
```

Đúng:

```text
staging dùng image A
uat promote chính image A
production promote chính image A
```

Vì chỉ khi dùng cùng image, bạn mới biết:

```text
artifact production chính là artifact đã test ở UAT
```

---

# 10. Registry topology

## 10.1. Registry đặt ở đâu?

On-prem thường dùng:

```text
Harbor
GitLab Container Registry
Nexus Repository
JFrog Artifactory
Docker Registry nội bộ
```

Topology phổ biến:

```text
GitLab Runner
  -> push image
  -> registry.company.local

Kubernetes node
  -> pull image
  -> registry.company.local
```

## 10.2. Registry production nên có gì?

```text
TLS
authentication
RBAC
project/repository per team
retention policy
vulnerability scan
immutable tag policy cho release
backup
storage đủ lớn
```

Ví dụ namespace registry:

```text
registry.company.local/social/uaa-service
registry.company.local/social/gateway
registry.company.local/social/post-service
```

Image tags:

```text
8f31a2c
staging-8f31a2c
v1.4.0
release-2026-07-06-01
```

Không nên dùng production với:

```text
latest
```

---

# 11. GitLab Runner topology

## 11.1. Runner trên VM

```text
runner-vm-1
  docker executor
  build/test Java
  build image
```

Phù hợp:

- mới bắt đầu;
- dễ debug;
- ít pipeline.

Nhược điểm:

- scale thủ công;
- dễ đầy disk vì image layer;
- cần cleanup định kỳ.

## 11.2. Runner trên Kubernetes

```text
gitlab-runner namespace
  runner controller
  job pod test
  job pod build
  job pod scan
```

Phù hợp:

- nhiều pipeline;
- muốn autoscale job;
- muốn cô lập job tốt hơn.

Với Kubernetes runner, nên build image bằng:

```text
Kaniko
BuildKit
Jib cho Java
```

Hạn chế Docker-in-Docker privileged nếu security team không cho phép.

## 11.3. Runner tách theo quyền

Doanh nghiệp hay tách runner:

```text
runner-build:
  chạy test/package/build image
  không có quyền production

runner-config:
  có quyền push config repo
  không có kubeconfig production

runner-security:
  chạy scan

runner-prod:
  nếu bắt buộc dùng, chỉ chạy job có approval
```

Nếu dùng GitOps đúng nghĩa, runner production không cần kubeconfig production.

---

# 12. Network topology on-prem

## 12.1. Luồng request từ user

```text
User/Internal Client
  -> DNS
  -> Load Balancer VIP
  -> ingress-nginx hoặc external nginx/F5
  -> gateway service
  -> gateway pod
  -> uaa-service/post-service
```

Ví dụ DNS:

```text
staging-api.company.local -> 10.10.10.50
uat-api.company.local     -> 10.10.10.60
api.company.local         -> 10.10.10.70
```

Nếu topology A một cluster:

```text
same ingress controller
  host staging-api.company.local -> namespace staging/gateway
  host uat-api.company.local     -> namespace uat/gateway
  host api.company.local         -> namespace production/gateway
```

Nếu topology B/C:

```text
non-prod ingress VIP:
  staging-api.company.local
  uat-api.company.local

prod ingress VIP:
  api.company.local
```

## 12.2. LoadBalancer on-prem

Vì on-prem không tự có cloud LoadBalancer, thường dùng:

```text
F5
HAProxy
external NGINX
MetalLB
keepalived + HAProxy
VMware NSX Advanced Load Balancer
```

Lab tiết kiệm có thể dùng:

```text
NodePort
MetalLB
Ingress NGINX + hostNetwork
```

Production nên dùng:

```text
F5/HAProxy/MetalLB được vận hành nghiêm túc
```

## 12.3. East-west traffic

Traffic giữa service nội bộ:

```text
gateway -> uaa-service
gateway -> post-service
post-service -> uaa-service nếu cần
service -> database
```

Trong Kubernetes:

```text
http://uaa-service.production.svc.cluster.local:8082
http://post-service.production.svc.cluster.local:8083
```

Trong cùng namespace có thể gọi ngắn:

```text
http://uaa-service:8082
http://post-service:8083
```

---

# 13. Database topology

## 13.1. Database trong cluster hay ngoài cluster?

Doanh nghiệp thường đặt database production ngoài Kubernetes hoặc dùng database platform riêng.

Production phổ biến:

```text
Oracle/PostgreSQL/MySQL cluster ngoài Kubernetes
  -> app trong Kubernetes kết nối qua service endpoint/DNS
```

Lab hoặc staging có thể chạy DB trong Kubernetes:

```text
PostgreSQL StatefulSet
PVC
Service
```

Nhưng production database trong Kubernetes cần năng lực vận hành cao:

- backup/restore;
- storage class ổn định;
- replication;
- disaster recovery;
- monitoring;
- upgrade plan.

## 13.2. Khuyến nghị theo môi trường

Staging:

```text
database nhỏ
có thể chạy trong cluster
hoặc dùng DB shared non-prod
```

UAT:

```text
database riêng
data gần giống production nhưng đã mask/anonymize
không dùng chung DB staging
```

Production:

```text
database riêng
HA
backup
monitoring
restricted access
```

## 13.3. Config kết nối DB

ConfigMap:

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://uat-db.company.local:5432/uaa
```

Secret:

```text
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

Không để password trong ConfigMap.

---

# 14. Secret management topology

## 14.1. Cách lab đơn giản

```text
kubectl create secret
```

hoặc file YAML local không commit.

## 14.2. Cách GitOps tốt hơn

```text
Sealed Secrets
```

Luồng:

```text
plain secret
  -> kubeseal
  -> sealed secret commit vào Git
  -> controller giải mã trong cluster
```

## 14.3. Cách enterprise

```text
Vault / CyberArk / secret manager nội bộ
  -> External Secrets Operator
  -> Kubernetes Secret
  -> Pod
```

Ví dụ:

```text
Vault path:
  secret/social/staging/uaa
  secret/social/uat/uaa
  secret/social/production/uaa
```

Kubernetes:

```text
ExternalSecret uaa-secret
  -> đọc Vault
  -> tạo Secret uaa-secret
```

Ưu điểm:

- secret không nằm trong Git;
- rotate dễ hơn;
- audit tốt hơn;
- production secret tách rõ khỏi non-prod.

---

# 15. Monitoring/logging topology

## 15.1. Monitoring

Thành phần phổ biến:

```text
Prometheus
Grafana
Alertmanager
kube-state-metrics
node-exporter
```

Luồng:

```text
uaa-service /actuator/prometheus
gateway /actuator/prometheus
post-service /actuator/prometheus
  -> Prometheus scrape
  -> Grafana dashboard
  -> Alertmanager cảnh báo
```

Metric quan trọng:

```text
HTTP request count
HTTP error rate
latency p95/p99
JVM memory
GC pause
CPU/memory pod
restart count
pod readiness
deployment replica unavailable
```

## 15.2. Logging

Thành phần phổ biến:

```text
Fluent Bit / Vector
Loki / OpenSearch / Elasticsearch
Grafana / Kibana
```

Luồng:

```text
app logs stdout
  -> Fluent Bit DaemonSet
  -> Loki/OpenSearch
  -> query by namespace, pod, traceId, userId
```

Production nên chuẩn hóa log:

```text
JSON log
traceId
spanId
requestId
userId nếu phù hợp và không vi phạm bảo mật
service name
environment
```

---

# 16. Resource sizing ví dụ cho service

Đây là sizing để thực hành, không phải sizing production bắt buộc.

## 16.1. Staging

```text
gateway:
  replicas: 1
  requests: 250m CPU, 512Mi RAM
  limits:   1000m CPU, 1024Mi RAM

uaa-service:
  replicas: 1
  requests: 500m CPU, 768Mi RAM
  limits:   1500m CPU, 1536Mi RAM

post-service:
  replicas: 1
  requests: 500m CPU, 768Mi RAM
  limits:   1500m CPU, 1536Mi RAM
```

## 16.2. UAT

```text
gateway:
  replicas: 2
  requests: 500m CPU, 768Mi RAM
  limits:   1500m CPU, 1536Mi RAM

uaa-service:
  replicas: 2
  requests: 500m CPU, 1024Mi RAM
  limits:   2000m CPU, 2048Mi RAM

post-service:
  replicas: 2
  requests: 500m CPU, 1024Mi RAM
  limits:   2000m CPU, 2048Mi RAM
```

## 16.3. Production nhỏ

```text
gateway:
  replicas: 3
  requests: 500m CPU, 1024Mi RAM
  limits:   2000m CPU, 2048Mi RAM

uaa-service:
  replicas: 3
  requests: 1000m CPU, 1536Mi RAM
  limits:   3000m CPU, 3072Mi RAM

post-service:
  replicas: 3
  requests: 1000m CPU, 1536Mi RAM
  limits:   3000m CPU, 3072Mi RAM
```

Ước tính request production nhỏ:

```text
gateway:      3 * 0.5 CPU = 1.5 CPU, 3 GB RAM
uaa-service:  3 * 1.0 CPU = 3.0 CPU, 4.5 GB RAM
post-service: 3 * 1.0 CPU = 3.0 CPU, 4.5 GB RAM

Tổng app request:
  7.5 CPU
  12 GB RAM
```

Cần cộng thêm:

```text
system pods
ingress
monitoring
logging
Argo CD
headroom rolling update
node failure buffer
```

Vì vậy production nhỏ nên có ít nhất:

```text
3 worker x 8 vCPU, 16 GB RAM
```

Tốt hơn:

```text
3 worker x 16 vCPU, 32 GB RAM
```

---

# 17. Nguyên tắc đặt instance/program lên node

Phần `control-plane`, `worker` đã giải thích ở mục 2.1.
Ở đây chỉ cần nhớ vài nguyên tắc khi đặt chương trình lên VPS/node.

## 17.1. Lab nhỏ

Lab nhỏ có thể để một VPS vừa làm control-plane vừa làm worker.
Làm vậy rẻ, dễ dựng, đủ học topology.

Nhưng đừng cài mọi thứ nặng cùng lúc.
Thứ tự nên cài:

```text
k3s
ingress-nginx
Argo CD
app demo
external-secrets hoặc sealed-secrets
monitoring nhẹ
```

Các món nên để sau:

```text
Harbor
Loki/OpenSearch
Kafka
GitLab self-managed
database HA
```

## 17.2. Production hoặc lab nghiêm túc hơn

Nếu đã có nhiều worker, cố gắng rải pod cùng service ra nhiều worker.
Ví dụ production có 3 worker:

```text
worker-1:
  gateway-0
  uaa-service-0
  post-service-0
  ingress-nginx-0

worker-2:
  gateway-1
  uaa-service-1
  post-service-1
  ingress-nginx-1

worker-3:
  gateway-2
  uaa-service-2
  post-service-2
  prometheus/grafana tùy size
```

Mục tiêu:

```text
pod cùng service không dồn vào một node
```

Nếu cả 3 pod `uaa-service` cùng nằm trên `worker-1`, khi `worker-1` chết thì service này vẫn mất dù bạn đã khai báo 3 replicas.

## 17.3. Khi nào cần infra node?

Infra node là worker dành cho tool nền:

```text
ingress controller
monitoring
logging
Argo CD
registry
```

App worker là worker dành cho service business:

```text
gateway
uaa-service
post-service
payment-service
wallet-service
```

Lab ban đầu chưa cần tách infra node.
Khi tài nguyên bắt đầu lớn hoặc muốn học giống enterprise hơn, hãy tách infra node để tool nền không tranh tài nguyên với app chính.

---

# 18. Topology thực hành khuyến nghị cho bạn

Vì bạn muốn thuê nhiều VPS để học dần, nên đi theo lộ trình này:

```text
1. Topology B mini
2. Topology C mini
3. Topology D mini
4. Production-like nhỏ
```

## 18.1. Bắt đầu nên chọn topology nào?

Nếu mục tiêu là học giống doanh nghiệp nhưng vẫn kiểm soát chi phí, nên bắt đầu bằng Topology B.

```text
2 VPS:
  non-prod cluster
  production cluster
```

Lý do:

- Bạn học được việc tách production khỏi môi trường test.
- Chi phí chưa quá cao.
- Dễ hiểu hơn Topology C/D.
- Sau này nâng cấp lên C hoặc D rất tự nhiên.

## 18.2. Khi nào nâng lên C?

Nâng lên Topology C khi bạn muốn mỗi môi trường là một cluster riêng:

```text
3 VPS:
  staging cluster
  uat cluster
  production cluster
```

Lúc này bạn sẽ học rõ:

- mỗi environment có kubeconfig/context riêng;
- mỗi environment có Argo CD hoặc Application riêng;
- config repo phải tách path rõ;
- production không bị staging/UAT ảnh hưởng ở cấp cluster.

## 18.3. Khi nào nâng lên D?

Nâng lên Topology D khi bạn muốn học platform team làm gì.

```text
4 VPS trở lên:
  platform cluster
  staging cluster
  uat cluster
  production cluster
```

Platform cluster dùng để đặt đồ nghề chung:

```text
GitLab Runner
Argo CD central/non-prod
monitoring
logging
registry
secret tooling
```

Đừng bắt đầu bằng D nếu chưa quen B/C.
D mạnh, nhưng nhiều thành phần hơn nên người mới dễ bị rối.

---

# 19. Topology doanh nghiệp khuyến nghị cuối cùng

Nếu chọn một topology cân bằng nhất cho doanh nghiệp đang có 3 môi trường, mình chọn:

```text
Topology B: hai cluster
  non-prod cluster chứa staging + uat
  production cluster chứa production
```

Lý do:

- Production tách khỏi môi trường test.
- Chi phí thấp hơn ba cluster.
- Vận hành dễ hơn topology C.
- Đủ giống thực tế enterprise.
- Dễ mở rộng sau này.

Kiến trúc:

```text
GitLab
  -> GitLab Runner
  -> Registry
  -> Config/GitOps Repo
  -> Argo CD non-prod -> non-prod cluster
  -> Argo CD prod    -> production cluster
```

Non-prod cluster:

```text
namespace argocd
namespace ingress-nginx
namespace external-secrets
namespace monitoring
namespace staging
namespace uat
```

Production cluster:

```text
namespace argocd
namespace ingress-nginx
namespace external-secrets
namespace monitoring
namespace production
```

Service:

```text
staging:
  gateway
  uaa-service
  post-service

uat:
  gateway
  uaa-service
  post-service

production:
  gateway
  uaa-service
  post-service
```

Promotion:

```text
source repo merge
  -> build image
  -> update staging config
  -> staging auto-sync
  -> promote same image tag to UAT by MR
  -> UAT sync
  -> promote same image tag to production by approved MR
  -> production manual sync
```

---

# 20. Checklist khi thiết kế topology thật

Trước khi dựng thật, trả lời các câu hỏi này:

- Production có được chạy chung cluster với staging/UAT không?
- Ai có quyền merge config production?
- Argo CD production có auto-sync không?
- GitLab Runner có giữ kubeconfig production không?
- Image tag có immutable không?
- Registry có scan vulnerability không?
- Secret production nằm ở đâu?
- Database production chạy ở đâu?
- Ingress production đi qua Load Balancer nào?
- Monitoring production có alert không?
- Log có traceId/requestId không?
- Khi deploy lỗi, rollback bằng Git revert hay Argo CD rollback?
- Khi một node chết, app còn đủ replica không?
- Khi rolling update, cluster còn đủ tài nguyên không?
- Có ResourceQuota cho staging/UAT để không ảnh hưởng production không?
- Có NetworkPolicy để hạn chế service gọi lung tung không?
- Có backup config repo, registry, database, secret manager không?

---

# 21. Tóm tắt ngắn

Nếu học/lab tiết kiệm:

```text
1 cluster
3 namespace: staging, uat, production
Argo CD trong cùng cluster
Ingress NGINX
Config repo dùng Kustomize overlays
```

Nếu mô phỏng doanh nghiệp vừa:

```text
2 cluster
non-prod: staging + uat
prod: production
Argo CD riêng non-prod/prod
Registry nội bộ
Config repo riêng
```

Nếu enterprise lớn:

```text
3 cluster hoặc nhiều hơn
staging cluster
uat cluster
production cluster
platform cluster tùy chọn
central monitoring/logging/secret
strict RBAC và approval
```

Luồng chuẩn:

```text
Code repo
  -> GitLab CI
  -> build image
  -> registry
  -> update config repo
  -> Argo CD
  -> Kubernetes
```

Câu cần nhớ:

```text
CI tạo artifact.
Registry lưu artifact.
Config repo mô tả môi trường.
Argo CD sync môi trường.
Kubernetes chạy workload.
Production không nên phụ thuộc vào thao tác tay hoặc build lại image.
```
