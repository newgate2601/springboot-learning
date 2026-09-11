# Kubernetes từ cơ bản tới triển khai thực tế

## 1. Kubernetes là gì?

Kubernetes, thường viết tắt là K8s, là một nền tảng mã nguồn mở dùng để điều phối container. Nói đơn giản, Kubernetes giúp chạy, quản lý, mở rộng, tự phục hồi và triển khai các ứng dụng container hóa trên một cụm nhiều máy chủ.

Nếu Docker giúp đóng gói ứng dụng thành container, thì Kubernetes giúp trả lời các câu hỏi vận hành lớn hơn:

- Container nên chạy ở máy chủ nào?
- Nếu container bị chết thì ai khởi động lại?
- Nếu lượng truy cập tăng thì làm sao tăng số lượng container?
- Nếu một máy chủ hỏng thì ứng dụng có tiếp tục chạy không?
- Làm sao triển khai phiên bản mới mà hạn chế downtime?
- Làm sao service này gọi service kia mà không cần biết IP thay đổi liên tục?
- Làm sao quản lý cấu hình, secret, tài nguyên CPU/RAM và phân quyền?

Kubernetes không chỉ là công cụ chạy container. Nó là một hệ thống điều phối hạ tầng theo trạng thái mong muốn. Người vận hành khai báo rằng hệ thống nên có trạng thái như thế nào, ví dụ "luôn chạy 3 bản sao của service order", rồi Kubernetes liên tục quan sát trạng thái thực tế và tự điều chỉnh để đưa hệ thống về trạng thái mong muốn.

Ví dụ:

```yaml
replicas: 3
image: my-company/order-service:1.0.0
```

Ý nghĩa không phải là "hãy chạy lệnh tạo 3 container một lần". Ý nghĩa là "hãy đảm bảo lúc nào cũng có 3 bản sao của order-service đang chạy". Nếu một bản sao chết, Kubernetes tạo bản sao mới. Nếu một node hỏng, Kubernetes chuyển workload sang node khác nếu còn tài nguyên phù hợp.

## 2. Vì sao cần Kubernetes?

Khi ứng dụng còn nhỏ, một server và vài container có thể đủ. Ta có thể dùng Docker Compose để chạy ứng dụng, database, Redis, message broker trên một máy. Nhưng khi hệ thống lớn hơn, những vấn đề sau bắt đầu xuất hiện:

- Có nhiều service cần chạy cùng lúc.
- Mỗi service cần nhiều bản sao để chịu tải.
- Container có thể chết bất kỳ lúc nào.
- Server vật lý hoặc máy ảo có thể hỏng.
- IP container thay đổi liên tục.
- Cần triển khai phiên bản mới mà không dừng toàn hệ thống.
- Cần rollback nhanh khi release lỗi.
- Cần giới hạn tài nguyên để service này không chiếm hết CPU/RAM của service khác.
- Cần quản lý cấu hình theo môi trường dev, staging, production.
- Cần quan sát log, metric, health check và trạng thái workload.

Kubernetes giải quyết các vấn đề này bằng cách cung cấp một lớp điều phối chung cho container. Thay vì quản lý từng container thủ công, ta quản lý các đối tượng cấp cao như Pod, Deployment, Service, ConfigMap, Secret, Ingress, Job, CronJob, StatefulSet.

Tư duy quan trọng của Kubernetes là:

```text
Bạn khai báo trạng thái mong muốn
  -> Kubernetes lưu trạng thái đó
  -> Kubernetes quan sát trạng thái thực tế
  -> Kubernetes tự điều chỉnh khi có sai lệch
```

Ví dụ nếu bạn khai báo một Deployment cần 5 replica nhưng thực tế chỉ còn 4 Pod sống, Kubernetes sẽ tự tạo thêm 1 Pod. Nếu bạn cập nhật image từ `1.0.0` lên `1.1.0`, Kubernetes có thể thay Pod cũ bằng Pod mới theo chiến lược rolling update.

## 3. Kubernetes khác gì Docker và Docker Compose?

Docker là công cụ phổ biến để build image và chạy container. Docker Compose giúp định nghĩa nhiều container chạy cùng nhau trên một máy hoặc một môi trường nhỏ. Kubernetes đi xa hơn: nó quản lý container trên một cụm nhiều máy, hỗ trợ scheduling, self-healing, service discovery, scaling, rollout, rollback và nhiều khía cạnh vận hành production.

| Tiêu chí | Docker | Docker Compose | Kubernetes |
|---|---|---|---|
| Mục tiêu chính | Build và chạy container | Chạy nhiều container theo file compose | Điều phối container trên cluster |
| Phạm vi | Một máy là chủ yếu | Một máy hoặc môi trường nhỏ | Nhiều node, nhiều môi trường |
| Tự phục hồi | Có restart policy cơ bản | Có restart policy cơ bản | Mạnh, dựa trên controller và desired state |
| Scaling | Thủ công hoặc đơn giản | Có thể scale nhưng hạn chế | Scale ngang mạnh, có thể tự động |
| Service discovery | Network nội bộ Docker | DNS theo service trong compose | DNS, Service, Endpoint, Ingress |
| Rolling update | Không phải trọng tâm | Hạn chế | Hỗ trợ tốt qua Deployment |
| Production lớn | Không đủ một mình | Không phù hợp cho hệ thống lớn | Phù hợp nếu vận hành đúng |

Docker Compose rất tốt cho học tập, local development hoặc demo. Kubernetes phù hợp hơn khi hệ thống có nhiều service, cần độ tin cậy cao, cần mở rộng, cần triển khai nhiều môi trường và có đội ngũ đủ năng lực vận hành.

## 4. Kiến trúc tổng quan của Kubernetes

Một Kubernetes cluster gồm hai nhóm thành phần chính:

```text
Kubernetes Cluster
  Control Plane
    API Server
    etcd
    Scheduler
    Controller Manager
    Cloud Controller Manager
  Worker Nodes
    kubelet
    kube-proxy
    Container Runtime
    Pods
```

Control Plane là bộ não điều khiển cluster. Worker Node là nơi workload thực sự chạy. Developer hoặc hệ thống CI/CD thường tương tác với Kubernetes thông qua API Server bằng `kubectl`, Helm, Argo CD, GitLab CI, Jenkins hoặc các controller khác.

## 5. Control Plane

Control Plane là nhóm thành phần quản lý và điều phối Kubernetes cluster. Bạn khai báo **trạng thái mong muốn**, chẳng hạn “chạy 3 bản sao của post-service”; Control Plane phối hợp để tạo Pod, chọn Node và duy trì trạng thái đó. Container của ứng dụng được thực thi bởi container runtime trên Worker Node.

Control Plane gồm nhiều thành phần, mỗi thành phần có trách nhiệm riêng:

| Thành phần | Câu hỏi nó giải quyết |
|---|---|
| API Server | Ai gửi yêu cầu, có quyền thực hiện không, cấu hình có hợp lệ không? |
| etcd | Cấu hình và trạng thái quản lý cluster được lưu ở đâu? |
| Scheduler | Pod mới nên được gán vào Node nào? |
| Controller Manager | Cần thay đổi gì để trạng thái thực tế tiến về trạng thái mong muốn? |
| Cloud Controller Manager | Kubernetes phối hợp với tài nguyên của nhà cung cấp cloud như thế nào? |

### 5.1. API Server

API Server là cổng giao tiếp trung tâm để đọc và thay đổi các đối tượng Kubernetes. Các thao tác như tạo Deployment, xem Pod, cập nhật Service hay xóa ConfigMap đều gửi yêu cầu tới API Server.

Ví dụ bạn đã có file `deployment.yaml` khai báo Deployment của post-service với `replicas: 3`:

```bash
kubectl apply -f deployment.yaml
```

Luồng xử lý được đơn giản hóa như sau:

1. `kubectl` đọc file và gửi yêu cầu tới API Server.
2. API Server xác thực danh tính: người hoặc chương trình gửi yêu cầu là ai?
3. API Server kiểm tra phân quyền: danh tính đó có được tạo hoặc cập nhật Deployment trong namespace này không?
4. API Server thực hiện các bước kiểm tra dữ liệu và admission áp dụng cho yêu cầu.
5. Nếu yêu cầu được chấp nhận, đối tượng được lưu vào etcd và API Server trả kết quả cho client.

**Lệnh apply thành công chưa có nghĩa 3 Pod đã chạy khỏe.** Nó cho biết yêu cầu cấu hình được chấp nhận; controller, scheduler và kubelet còn phải thực hiện phần việc tiếp theo.

API Server không trực tiếp chạy container. Controller, scheduler và kubelet theo dõi hoặc cập nhật các đối tượng qua API Server. Trong luồng quản lý thông thường này, chúng không tự đọc/ghi etcd trực tiếp.

API Server cũng không phải API nghiệp vụ của post-service: API Server xử lý yêu cầu quản lý cluster; API của post-service xử lý nghiệp vụ như tạo hoặc đọc bài viết.

### 5.2. etcd

`etcd` là kho dữ liệu key-value dùng để lưu bền vững dữ liệu quản lý Kubernetes, gồm cấu hình mong muốn và trạng thái được các thành phần báo cáo qua API Server.

Ví dụ các đối tượng được lưu gồm:

- Deployment của post-service yêu cầu 3 replica và dùng image nào.
- Pod nào đã được tạo, được gán vào Node nào và có trạng thái được báo cáo ra sao.
- Các Node đã đăng ký với cluster.
- Service, ConfigMap, Secret và các đối tượng cấu hình khác.

Có thể hình dung etcd là “sổ ghi chép” của cluster. API Server quản lý việc đọc và cập nhật sổ; controller và scheduler dựa vào dữ liệu lấy qua API Server để ra quyết định.

**etcd không phải database nghiệp vụ của ứng dụng.** Nội dung bài viết, tài khoản người dùng hay đơn hàng vẫn nằm trong database ứng dụng, chẳng hạn PostgreSQL. etcd cũng không lưu image container hay toàn bộ log của ứng dụng.

Dữ liệu trạng thái trong etcd là trạng thái được báo cáo, có thể có độ trễ so với những gì đang xảy ra trên Node. Nó không phải phép đo trực tiếp liên tục của mọi container.

Nếu mất dữ liệu etcd mà không có backup, cluster có thể mất thông tin cần thiết để quản lý tài nguyên. Với cluster tự vận hành, cần bảo vệ truy cập, backup và kiểm tra khả năng khôi phục etcd.

### 5.3. Scheduler

Scheduler theo dõi các Pod chưa được gán Node, sau đó lựa chọn Node phù hợp và ghi nhận việc gán qua API Server. Nó quyết định **chạy ở đâu**, còn kubelet và container runtime trên Node đảm nhiệm việc chạy container.

Scheduler cân nhắc các điều kiện như:

- Node còn đủ tài nguyên có thể phân bổ theo `requests` của Pod không?
- Pod có `nodeSelector`, affinity hoặc anti-affinity không?
- Node có bị cordon hoặc có taint mà Pod không toleration được không?
- Có ràng buộc về GPU, volume, zone hoặc phân bố Pod không?

Ví dụ Pod khai báo:

```yaml
resources:
  requests:
    cpu: "500m"
    memory: "512Mi"
```

`500m` tương đương 0,5 CPU; `512Mi` là 512 MiB bộ nhớ. Scheduler xét phần tài nguyên có thể phân bổ của Node sau khi tính các request đã được đặt lên Node. Việc chọn Node không đơn giản là tìm máy đang có phần trăm CPU sử dụng thấp nhất.

Nếu không có Node đáp ứng các điều kiện, Pod có thể ở trạng thái `Pending` vì chưa được schedule. Scheduler không tự làm Node có thêm CPU/RAM và cũng không tự tạo EC2 mới. Việc bổ sung Node cần cơ chế quản lý capacity riêng.

Có nhiều Node không mặc định bảo đảm các replica phân bố đều hoặc nằm ở các AZ khác nhau; cần cấu hình ràng buộc phân bố và kiểm tra kết quả thực tế.

### 5.4. Controller Manager

Controller Manager chạy nhiều controller. Mỗi controller là một **vòng lặp điều khiển**: quan sát trạng thái qua API Server, so sánh với trạng thái mong muốn và thực hiện thay đổi thuộc trách nhiệm của mình.

Với Deployment, các controller phối hợp như sau:

```text
Deployment: mong muốn 3 replica
  → Deployment Controller quản lý ReplicaSet tương ứng
  → ReplicaSet Controller tạo các đối tượng Pod để đủ số lượng
  → Scheduler gán các Pod chưa có Node
  → kubelet/container runtime trên Node chạy container
```

Các mũi tên biểu diễn quan hệ công việc. Đây không phải chuỗi gọi hàm trực tiếp; các thành phần phối hợp thông qua đối tượng và trạng thái trên API Server.

Ví dụ bạn xóa một Pod thuộc ReplicaSet đang yêu cầu 3 replica:

```text
Mong muốn: 3 replica
Sau khi xóa: chỉ còn 2 Pod thuộc ReplicaSet
ReplicaSet Controller: tạo một Pod mới để bù
Scheduler: chọn Node cho Pod mới
kubelet trên Node đó: thực hiện chạy container
```

Pod thay thế là đối tượng mới, không phải khôi phục nguyên Pod đã xóa. Pod mới cũng có thể được gán vào Node khác.

Cần phân biệt các trường hợp:

| Tình huống | Thành phần xử lý chính |
|---|---|
| Container trong Pod bị crash | kubelet phối hợp với runtime để restart theo restart policy. |
| Một Pod của ReplicaSet bị xóa | ReplicaSet Controller tạo Pod mới để duy trì số lượng. |
| Có Pod mới chưa được gán Node | Scheduler tìm Node phù hợp. |
| Một Pod còn tồn tại nhưng chưa Ready | Không thể kết luận controller sẽ lập tức tạo thêm Pod; cần xem trạng thái và nguyên nhân lỗi. |

Các controller khác gồm StatefulSet, Job, Node, EndpointSlice và Namespace Controller. Mỗi controller xử lý một phần; Kubernetes liên tục điều chỉnh chứ không chỉ thực hiện cấu hình một lần rồi kết thúc.

### 5.5. Cloud Controller Manager

Cloud Controller Manager tích hợp Kubernetes với nhà cung cấp cloud. Tùy triển khai, các controller của nó có thể:

- Đồng bộ thông tin Node với máy ảo trên cloud, như địa chỉ hoặc thông tin nhận diện.
- Quản lý route cần thiết cho mạng cluster nếu mô hình mạng sử dụng cơ chế đó.
- Phối hợp tạo, cập nhật hoặc xóa Load Balancer cho Service phù hợp.

Ví dụ khi tạo Service có `type: LoadBalancer`, controller phụ trách tích hợp Load Balancer có thể gọi API cloud để tạo tài nguyên rồi cập nhật địa chỉ vào trạng thái Service. Đối tượng Service trong Kubernetes và Load Balancer trên cloud là hai tài nguyên liên quan, không phải cùng một đối tượng.

Không phải mọi chức năng cloud đều do Cloud Controller Manager xử lý. Controller chịu trách nhiệm Load Balancer cụ thể phụ thuộc cách cài đặt cluster. Với volume dùng CSI, việc cấp phát/gắn volume thuộc cơ chế CSI và các thành phần lưu trữ liên quan; không nên gom toàn bộ việc gắn volume vào Cloud Controller Manager.

Với Kubernetes local hoặc on-premise, có thể không cần thành phần này hoặc dùng giải pháp tích hợp hạ tầng khác.

### 5.6. Ghép các thành phần qua một lần deploy

Giả sử yêu cầu là “chạy 3 replica của post-service”:

| Bước | Thành phần | Việc thực hiện |
|---|---|---|
| 1 | kubectl → API Server | Gửi và kiểm tra yêu cầu tạo/cập nhật Deployment. |
| 2 | API Server → etcd | Lưu cấu hình được chấp nhận. |
| 3 | Deployment/ReplicaSet Controller | Quản lý ReplicaSet và tạo đối tượng Pod cần thiết. |
| 4 | Scheduler | Gán từng Pod chưa có Node vào Node đáp ứng điều kiện. |
| 5 | kubelet và runtime trên Worker Node | Chuẩn bị và chạy container của các Pod được giao. |
| 6 | kubelet → API Server | Báo trạng thái Pod/Node; trạng thái quản lý được cập nhật. |
| 7 | Các controller | Tiếp tục quan sát và điều chỉnh khi có thay đổi. |

Cloud Controller Manager tham gia khi có công việc tích hợp cloud thuộc trách nhiệm của nó; không phải bước bắt buộc nối tiếp trong mọi lần tạo Pod.

Nguồn đối chiếu: [Kubernetes Components](https://kubernetes.io/docs/concepts/overview/components/) và [Cloud Controller Manager](https://kubernetes.io/docs/concepts/architecture/cloud-controller/).

## 6. Worker Node

### 6.1. kubelet

`kubelet` là agent chạy trên mỗi Worker Node. Nó nhận thông tin Pod được gán cho Node từ API Server, sau đó yêu cầu container runtime tạo container tương ứng.

Nhiệm vụ chính của kubelet:

- Theo dõi Pod được phân công cho Node.
- Kéo image nếu cần.
- Khởi động container.
- Chạy liveness probe, readiness probe, startup probe.
- Báo trạng thái Pod và Node về API Server.
- Mount volume vào container.

Nếu container chết, kubelet có thể khởi động lại theo chính sách restart của Pod. Nếu cả Node chết, control plane sẽ phát hiện và lên lịch Pod thay thế ở Node khác.

### 6.2. Container Runtime

Container Runtime là phần mềm thực sự chạy container. Kubernetes từng dùng Docker trực tiếp trong giai đoạn đầu, nhưng hiện nay runtime thường gặp là:

- containerd.
- CRI-O.
- Docker Engine thông qua lớp tương thích trong một số môi trường cũ.

Kubernetes giao tiếp với runtime qua Container Runtime Interface, gọi tắt là CRI. Nhờ đó Kubernetes không bị phụ thuộc cứng vào một runtime duy nhất.

### 6.3. kube-proxy

`kube-proxy` xử lý một phần mạng service trong cluster. Nó giúp request tới Service được chuyển đến Pod backend phù hợp.

Ví dụ Service `order-service` có 3 Pod backend. Khi một Pod khác gọi `http://order-service`, kube-proxy cùng cơ chế mạng của cluster giúp request được route tới một trong các Pod backend.

Tùy môi trường, kube-proxy có thể dùng iptables, IPVS hoặc được thay thế một phần bởi các giải pháp CNI nâng cao như Cilium.

## 7. Các khái niệm cốt lõi

### 7.1. Pod

Pod là đơn vị nhỏ nhất mà Kubernetes trực tiếp quản lý. Một Pod chứa một hoặc nhiều container chia sẻ network namespace và volume.

Trong thực tế, đa số Pod chỉ chứa một container ứng dụng chính. Một Pod có nhiều container thường dùng cho pattern sidecar, ví dụ:

- Container chính chạy ứng dụng.
- Sidecar thu thập log.
- Sidecar proxy như Envoy.
- Sidecar đồng bộ file cấu hình.

Ví dụ Pod đơn giản:

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: hello-pod
spec:
  containers:
    - name: hello
      image: nginx:1.27
      ports:
        - containerPort: 80
```

Giải thích:

- `apiVersion`: phiên bản API của loại tài nguyên.
- `kind`: loại tài nguyên, ở đây là Pod.
- `metadata.name`: tên Pod.
- `spec.containers`: danh sách container trong Pod.
- `image`: image được dùng để tạo container.
- `containerPort`: port ứng dụng lắng nghe bên trong container.

Không nên quản lý Pod đơn lẻ trong production vì nếu Pod bị xóa, Kubernetes không tự tạo lại nếu không có controller phía trên. Thường ta dùng Deployment, StatefulSet hoặc Job để quản lý Pod.

### 7.2. Deployment

Deployment quản lý ứng dụng stateless chạy lâu dài. Nó tạo và quản lý ReplicaSet, ReplicaSet quản lý Pod.

Ví dụ:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: my-company/order-service:1.0.0
          ports:
            - containerPort: 8080
```

Giải thích từng phần:

- `kind: Deployment`: khai báo workload dạng Deployment.
- `replicas: 3`: yêu cầu luôn có 3 Pod chạy.
- `selector.matchLabels`: Deployment quản lý các Pod có label `app: order-service`.
- `template.metadata.labels`: label gắn cho Pod được tạo ra.
- `template.spec.containers`: định nghĩa container trong Pod.
- `image`: phiên bản ứng dụng cần chạy.

Deployment phù hợp cho API service, web app, worker stateless, gateway, backend service không cần định danh Pod cố định.

### 7.3. ReplicaSet

ReplicaSet đảm bảo số lượng Pod replica đúng như mong muốn. Thông thường ta không tạo ReplicaSet trực tiếp mà để Deployment tạo.

Luồng thường gặp:

```text
Deployment
  -> ReplicaSet
    -> Pod
```

Khi update image trong Deployment, Kubernetes tạo ReplicaSet mới cho phiên bản mới và giảm dần số Pod của ReplicaSet cũ tùy chiến lược rollout.

### 7.4. Service

Pod có IP riêng nhưng IP Pod không ổn định. Khi Pod bị thay thế, IP có thể đổi. Service giải quyết vấn đề này bằng cách cung cấp một địa chỉ ổn định để truy cập nhóm Pod.

Ví dụ:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: order-service
spec:
  type: ClusterIP
  selector:
    app: order-service
  ports:
    - port: 80
      targetPort: 8080
```

Giải thích:

- `type: ClusterIP`: Service chỉ truy cập được trong cluster.
- `selector`: chọn các Pod có label `app: order-service`.
- `port: 80`: port của Service.
- `targetPort: 8080`: port bên trong container backend.

Khi Pod khác gọi `http://order-service`, request sẽ được chuyển tới một Pod backend phù hợp.

Các loại Service phổ biến:

| Loại Service | Mục đích |
|---|---|
| ClusterIP | Truy cập nội bộ trong cluster |
| NodePort | Mở port trên mỗi Node để truy cập từ ngoài |
| LoadBalancer | Tạo load balancer bên ngoài, thường dùng trên cloud |
| ExternalName | Trỏ tên service nội bộ tới DNS bên ngoài |

### 7.5. Namespace

Namespace chia cluster thành nhiều không gian logic. Nó giúp tách tài nguyên theo môi trường, team hoặc domain.

Ví dụ:

- `dev`
- `staging`
- `production`
- `monitoring`
- `logging`
- `platform`

Namespace giúp tổ chức tài nguyên tốt hơn, nhưng không phải ranh giới bảo mật tuyệt đối nếu không kết hợp thêm RBAC, NetworkPolicy, ResourceQuota và chính sách vận hành phù hợp.

### 7.6. ConfigMap

ConfigMap lưu cấu hình không nhạy cảm, ví dụ:

- URL service nội bộ.
- Feature flag.
- Timeout.
- Tên topic Kafka.
- Cấu hình log level.

Ví dụ:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: order-config
data:
  LOG_LEVEL: INFO
  PAYMENT_SERVICE_URL: http://payment-service
```

Ứng dụng có thể đọc ConfigMap qua biến môi trường hoặc file mount vào container.

### 7.7. Secret

Secret lưu dữ liệu nhạy cảm như password, token, private key, API key. Secret trong Kubernetes mặc định được encode base64, không phải mã hóa mạnh theo nghĩa bảo mật tuyệt đối.

Ví dụ:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: database-secret
type: Opaque
stringData:
  DB_USERNAME: app_user
  DB_PASSWORD: strong-password
```

Trong production, nên kết hợp Secret với:

- Encryption at rest cho etcd.
- RBAC chặt chẽ.
- External secret manager như HashiCorp Vault, AWS Secrets Manager, Azure Key Vault, GCP Secret Manager.
- Quy trình rotate secret định kỳ.

### 7.8. Ingress

Ingress quản lý truy cập HTTP/HTTPS từ bên ngoài vào Service trong cluster. Ingress thường cần Ingress Controller như NGINX Ingress Controller, Traefik, HAProxy, Kong hoặc cloud load balancer controller.

Ví dụ:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: shop-ingress
spec:
  rules:
    - host: shop.example.com
      http:
        paths:
          - path: /orders
            pathType: Prefix
            backend:
              service:
                name: order-service
                port:
                  number: 80
```

Giải thích:

- Request tới `shop.example.com/orders` đi vào Ingress.
- Ingress Controller đọc rule này.
- Request được chuyển tới Service `order-service` port `80`.
- Service tiếp tục chuyển tới Pod backend.

Ingress phù hợp khi có nhiều service HTTP cần dùng chung một entry point, domain, TLS certificate và rule routing.

### 7.9. Volume và PersistentVolume

Container có filesystem tạm thời. Khi container bị thay thế, dữ liệu bên trong container có thể mất. Kubernetes cung cấp Volume để gắn lưu trữ vào Pod.

Các khái niệm chính:

- `Volume`: khai báo storage ở cấp Pod.
- `PersistentVolume` hoặc `PV`: tài nguyên storage trong cluster.
- `PersistentVolumeClaim` hoặc `PVC`: yêu cầu storage từ ứng dụng.
- `StorageClass`: mô tả cách cấp phát storage động.

Ví dụ PVC:

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-data
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
```

Ý nghĩa:

- Ứng dụng yêu cầu 20Gi storage.
- Kubernetes tìm hoặc cấp phát PV phù hợp.
- Pod có thể mount PVC để lưu dữ liệu bền vững.

### 7.10. StatefulSet

StatefulSet dùng cho workload stateful cần danh tính ổn định, thứ tự triển khai hoặc volume riêng cho từng Pod.

Phù hợp với:

- Database cluster.
- Kafka.
- ZooKeeper.
- Elasticsearch.
- Redis cluster trong một số mô hình.

Khác với Deployment, Pod của StatefulSet có tên ổn định như:

```text
postgres-0
postgres-1
postgres-2
```

Mỗi Pod có thể có PVC riêng. Điều này quan trọng với hệ thống cần dữ liệu gắn với từng instance.

### 7.11. Job và CronJob

Job dùng cho tác vụ chạy xong rồi kết thúc, ví dụ migration, batch processing, import dữ liệu.

CronJob dùng cho tác vụ chạy theo lịch, ví dụ:

- Dọn dữ liệu tạm mỗi đêm.
- Gửi báo cáo hằng ngày.
- Đồng bộ dữ liệu định kỳ.
- Backup metadata.

Ví dụ CronJob:

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: cleanup-temp-files
spec:
  schedule: "0 2 * * *"
  jobTemplate:
    spec:
      template:
        spec:
          restartPolicy: OnFailure
          containers:
            - name: cleanup
              image: my-company/cleanup:1.0.0
```

Ý nghĩa:

- Chạy lúc 02:00 mỗi ngày.
- Nếu job lỗi, có thể retry theo chính sách.
- Phù hợp với tác vụ định kỳ không cần chạy liên tục.

## 8. Luồng hoạt động khi triển khai một ứng dụng

Giả sử ta triển khai `order-service` lên Kubernetes.

```text
Developer
  -> Viết deployment.yaml và service.yaml
  -> kubectl apply
  -> API Server nhận request
  -> etcd lưu trạng thái mong muốn
  -> Deployment Controller tạo ReplicaSet
  -> ReplicaSet Controller tạo Pod
  -> Scheduler chọn Node cho Pod
  -> kubelet trên Node kéo image và chạy container
  -> Service chọn Pod qua label
  -> Người dùng hoặc service khác gửi request tới Service/Ingress
```

Điểm quan trọng là không có một bước duy nhất "Kubernetes chạy app". Thay vào đó, nhiều controller và agent phối hợp với nhau dựa trên trạng thái được lưu trong API Server.

## 9. Ưu điểm của Kubernetes

### 9.1. Tự phục hồi tốt

Kubernetes có thể tự khởi động lại container lỗi, thay Pod chết, reschedule Pod khi Node không còn hoạt động, và dùng health check để tránh gửi traffic tới Pod chưa sẵn sàng.

Ví dụ:

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
```

Giải thích:

- `readinessProbe` cho biết Pod đã sẵn sàng nhận traffic chưa.
- `livenessProbe` cho biết container còn sống đúng nghĩa không.
- Nếu readiness fail, Service tạm ngừng route traffic tới Pod đó.
- Nếu liveness fail nhiều lần, kubelet restart container.

Ưu điểm này đặc biệt quan trọng với hệ thống production vì lỗi runtime luôn có thể xảy ra.

### 9.2. Mở rộng linh hoạt

Kubernetes hỗ trợ scale thủ công và tự động.

Scale thủ công:

```bash
kubectl scale deployment order-service --replicas=10
```

Scale tự động bằng Horizontal Pod Autoscaler:

```text
CPU trung bình vượt ngưỡng
  -> HPA tăng số replica
Traffic giảm
  -> HPA giảm số replica
```

Khi kết hợp với Cluster Autoscaler trên cloud, Kubernetes không chỉ tăng Pod mà còn có thể tăng Node nếu cluster thiếu tài nguyên.

### 9.3. Triển khai không gián đoạn dễ hơn

Deployment hỗ trợ rolling update. Kubernetes tạo Pod phiên bản mới dần dần và giảm Pod phiên bản cũ dần dần.

Ví dụ:

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 1
    maxSurge: 1
```

Giải thích:

- `maxUnavailable: 1`: tối đa 1 Pod có thể không sẵn sàng trong lúc update.
- `maxSurge: 1`: có thể tạo thêm tối đa 1 Pod vượt số replica mong muốn trong lúc update.

Cách này giúp release phiên bản mới an toàn hơn so với tắt toàn bộ phiên bản cũ rồi bật phiên bản mới.

### 9.4. Khai báo hạ tầng bằng manifest

Kubernetes dùng YAML để mô tả tài nguyên. Điều này giúp hạ tầng ứng dụng có thể được lưu trong Git, review qua pull request và triển khai tự động.

Ví dụ repo có thể chứa:

```text
k8s/
  base/
    deployment.yaml
    service.yaml
  overlays/
    dev/
    staging/
    production/
```

Khi hạ tầng được khai báo bằng file, team dễ kiểm soát lịch sử thay đổi, rollback, review và chuẩn hóa môi trường.

### 9.5. Hệ sinh thái rộng

Kubernetes có hệ sinh thái lớn:

- Helm để đóng gói chart.
- Kustomize để tùy biến manifest.
- Argo CD và Flux cho GitOps.
- Prometheus và Grafana cho monitoring.
- Loki, Fluent Bit, Elasticsearch cho logging.
- Istio, Linkerd, Cilium cho service mesh và networking nâng cao.
- External Secrets cho quản lý secret.
- cert-manager cho TLS certificate.

Điều này giúp Kubernetes trở thành nền tảng chung cho nhiều bài toán vận hành hiện đại.

### 9.6. Portable giữa nhiều môi trường

Kubernetes có thể chạy trên cloud, on-premise, laptop, bare metal hoặc môi trường hybrid. Manifest Kubernetes chuẩn có thể dùng lại phần lớn giữa các môi trường, dù vẫn cần điều chỉnh storage, ingress, load balancer và policy theo từng nơi.

Ưu điểm này giúp giảm phụ thuộc vào một nhà cung cấp hạ tầng duy nhất, nhưng không có nghĩa là migration giữa cloud luôn dễ. Phần lõi workload portable hơn, còn các dịch vụ managed bên ngoài vẫn cần thiết kế cẩn thận.

## 10. Nhược điểm của Kubernetes

### 10.1. Độ phức tạp cao

Kubernetes có rất nhiều khái niệm: Pod, Deployment, Service, Ingress, ConfigMap, Secret, PV, PVC, StorageClass, RBAC, NetworkPolicy, HPA, PDB, CRD, Operator. Với team mới, đường cong học tập khá dốc.

Ví dụ để public một service HTTP ra ngoài, có thể cần:

- Deployment.
- Service.
- Ingress.
- Ingress Controller.
- DNS.
- TLS certificate.
- Secret chứa certificate hoặc cert-manager.
- NetworkPolicy.
- Monitoring và log.

Nếu hệ thống nhỏ, Kubernetes có thể tạo ra nhiều chi phí nhận thức hơn giá trị nhận lại.

### 10.2. Chi phí vận hành không nhỏ

Một cluster production cần:

- Nâng cấp version Kubernetes.
- Backup `etcd`.
- Quản lý node.
- Theo dõi tài nguyên.
- Thiết lập bảo mật.
- Cấu hình network.
- Quản lý storage.
- Giám sát control plane và workload.
- Xử lý incident.

Managed Kubernetes như EKS, GKE, AKS giảm nhiều gánh nặng, nhưng vẫn không loại bỏ hoàn toàn trách nhiệm vận hành.

### 10.3. Debug khó hơn môi trường truyền thống

Trong Kubernetes, lỗi có thể nằm ở nhiều lớp:

- Image lỗi.
- Container crash.
- ConfigMap sai.
- Secret thiếu.
- Readiness probe fail.
- Service selector sai label.
- Ingress rule sai.
- DNS lỗi.
- NetworkPolicy chặn traffic.
- PVC không bind được.
- Node thiếu tài nguyên.

Ví dụ service không truy cập được có thể do `targetPort` sai, Pod chưa ready, selector không match label hoặc Ingress Controller chưa nhận rule. Vì vậy người vận hành cần biết cách đọc event, log, describe tài nguyên và kiểm tra luồng mạng.

### 10.4. Không tự động giải quyết mọi vấn đề kiến trúc

Kubernetes giúp chạy và điều phối ứng dụng, nhưng không tự làm ứng dụng trở nên tốt hơn. Nếu ứng dụng không stateless, startup chậm, không có health endpoint, xử lý shutdown kém, phụ thuộc cấu hình cứng hoặc không chịu được retry, Kubernetes có thể làm vấn đề lộ rõ hơn.

Ứng dụng muốn chạy tốt trên Kubernetes nên có:

- Health check rõ ràng.
- Graceful shutdown.
- Cấu hình qua environment hoặc file.
- Log ra stdout/stderr.
- Không lưu state quan trọng trong container filesystem.
- Request/limit tài nguyên hợp lý.
- Khả năng chạy nhiều replica an toàn.

### 10.5. YAML nhiều và dễ sai

Manifest Kubernetes có thể dài và lặp lại. Sai indentation, sai label, sai selector, sai namespace hoặc sai API version đều có thể gây lỗi.

Ví dụ selector của Service không khớp label Pod:

```yaml
selector:
  app: order
```

Trong khi Pod có label:

```yaml
labels:
  app: order-service
```

Kết quả là Service không có endpoint backend. Request tới Service sẽ không tới được Pod nào.

Để giảm vấn đề này, team thường dùng Helm, Kustomize, schema validation, policy check và GitOps review.

## 11. Use case phù hợp

### 11.1. Microservices nhiều service

Kubernetes rất phù hợp với hệ thống microservices có nhiều service độc lập như:

- User service.
- Order service.
- Payment service.
- Inventory service.
- Notification service.
- API Gateway.

Mỗi service có thể được đóng gói thành image riêng, triển khai bằng Deployment riêng và expose qua Service riêng.

Ví dụ kiến trúc:

```text
Client
  -> Ingress
  -> API Gateway
    -> user-service
    -> order-service
    -> payment-service
    -> inventory-service
```

Giải thích từng phần:

- `Ingress`: điểm vào HTTP/HTTPS từ bên ngoài.
- `API Gateway`: xử lý routing, authentication, rate limit hoặc aggregation.
- `user-service`: quản lý người dùng.
- `order-service`: xử lý đơn hàng.
- `payment-service`: xử lý thanh toán.
- `inventory-service`: kiểm tra tồn kho.
- Mỗi service có Deployment riêng để scale độc lập.

Ưu điểm trong use case này:

- Service nào tải cao thì scale service đó.
- Deploy service này không nhất thiết ảnh hưởng service khác.
- Service discovery nội bộ đơn giản qua DNS của Kubernetes.
- Rolling update giúp release từng service an toàn hơn.

Nhược điểm trong use case này:

- Số lượng manifest tăng nhanh.
- Debug request đi qua nhiều service phức tạp.
- Cần observability tốt: tracing, metrics, centralized logging.
- NetworkPolicy và RBAC cần thiết kế cẩn thận để tránh mở quá rộng.

### 11.2. CI/CD và GitOps

Kubernetes rất hợp với mô hình triển khai tự động. Pipeline CI build image, chạy test, push image lên registry. Sau đó CD cập nhật manifest hoặc Helm chart để triển khai.

Luồng ví dụ:

```text
Developer push code
  -> CI chạy test
  -> Build Docker image
  -> Push image my-company/order-service:1.1.0
  -> Update Kubernetes manifest
  -> Argo CD đồng bộ vào cluster
  -> Deployment rolling update Pod mới
```

Giải thích từng phần:

- `CI chạy test`: đảm bảo code đạt điều kiện tối thiểu.
- `Build Docker image`: đóng gói ứng dụng và runtime dependency.
- `Push image`: đưa artifact lên container registry.
- `Update manifest`: thay tag image hoặc giá trị Helm chart.
- `Argo CD`: quan sát Git repo và đồng bộ trạng thái vào cluster.
- `Deployment`: triển khai phiên bản mới theo chiến lược rollout.

Ưu điểm trong use case này:

- Mọi thay đổi deployment có lịch sử trong Git.
- Có thể rollback bằng Git revert hoặc rollback Deployment.
- Giảm thao tác thủ công trên cluster.
- Dễ chuẩn hóa quy trình giữa nhiều service.

Nhược điểm trong use case này:

- Cần quản lý secret trong pipeline và cluster thật cẩn thận.
- Nếu pipeline cập nhật nhầm tag hoặc manifest, lỗi được triển khai rất nhanh.
- GitOps cần quy ước repo rõ ràng, nếu không sẽ khó quản lý ở quy mô lớn.

### 11.3. Ứng dụng cần scale theo tải

Các hệ thống có traffic biến động mạnh như thương mại điện tử, flash sale, booking, livestream, cổng đăng ký có thể hưởng lợi từ autoscaling.

Ví dụ:

```text
Traffic tăng mạnh
  -> CPU order-service tăng
  -> HPA tăng replica từ 4 lên 20
  -> Service load balance request qua nhiều Pod hơn
  -> Traffic giảm
  -> HPA giảm replica để tiết kiệm tài nguyên
```

Giải thích từng phần:

- `HPA`: Horizontal Pod Autoscaler quan sát metric.
- `replica`: số Pod chạy song song.
- `Service`: giữ endpoint ổn định dù số Pod tăng giảm.
- `Cluster Autoscaler`: có thể thêm Node nếu cluster thiếu tài nguyên.

Ưu điểm trong use case này:

- Tận dụng tài nguyên tốt hơn.
- Phản ứng nhanh với tải tăng.
- Giảm nhu cầu scale thủ công.
- Có thể scale từng service theo nhu cầu riêng.

Nhược điểm trong use case này:

- Autoscaling không tức thì, vẫn có độ trễ.
- Nếu ứng dụng startup chậm, scale out có thể không kịp.
- Metric sai hoặc request/limit không hợp lý khiến scale không hiệu quả.
- Database hoặc dependency phía sau có thể trở thành bottleneck dù Pod đã tăng.

### 11.4. Nền tảng nội bộ cho nhiều team

Một công ty có nhiều team có thể dùng Kubernetes làm platform chung. Platform team chuẩn hóa cluster, namespace, logging, monitoring, ingress, secret, policy và template deployment. Các product team triển khai service của mình theo quy ước chung.

Ví dụ tổ chức namespace:

```text
namespaces
  team-user-dev
  team-user-prod
  team-payment-dev
  team-payment-prod
  monitoring
  logging
  ingress-nginx
```

Giải thích từng phần:

- Namespace theo team và môi trường giúp tách tài nguyên logic.
- `monitoring` chứa Prometheus, Grafana hoặc thành phần quan sát.
- `logging` chứa hệ thống thu thập log.
- `ingress-nginx` chứa Ingress Controller.
- RBAC giới hạn team chỉ thao tác trong namespace của mình.

Ưu điểm trong use case này:

- Chuẩn hóa cách deploy giữa các team.
- Tái sử dụng hạ tầng chung.
- Dễ áp dụng policy bảo mật và quota tài nguyên.
- Platform team có thể cung cấp template để team ứng dụng tự phục vụ.

Nhược điểm trong use case này:

- Cần governance rõ ràng.
- Nếu cluster dùng chung bị sự cố, nhiều team bị ảnh hưởng.
- Thiết kế RBAC, quota và network isolation phải nghiêm túc.
- Platform team cần năng lực vận hành cao.

### 11.5. Batch job và tác vụ định kỳ

Kubernetes không chỉ chạy web service. Nó cũng phù hợp với batch job và cron job.

Ví dụ use case:

```text
Mỗi ngày lúc 02:00
  -> CronJob tạo Job backup-report
  -> Job tạo Pod chạy script
  -> Script đọc dữ liệu, tạo file report
  -> Upload report lên object storage
  -> Pod kết thúc
```

Giải thích từng phần:

- `CronJob`: định nghĩa lịch chạy.
- `Job`: đại diện cho một lần thực thi.
- `Pod`: nơi container chạy script.
- `restartPolicy`: quyết định retry khi lỗi.
- Log của Pod giúp kiểm tra kết quả chạy.

Ưu điểm trong use case này:

- Không cần server riêng chỉ để chạy cron.
- Dễ đóng gói job thành image.
- Có thể giới hạn tài nguyên cho từng job.
- Có thể chạy nhiều job song song nếu cần.

Nhược điểm trong use case này:

- Cần xử lý idempotency, tránh job chạy trùng gây lỗi dữ liệu.
- Cần chính sách lưu log và lịch sử job hợp lý.
- Job phụ thuộc external system vẫn cần retry và timeout cẩn thận.

### 11.6. Machine learning và workload cần tài nguyên đặc biệt

Kubernetes có thể chạy workload cần GPU hoặc tài nguyên đặc biệt nếu cluster được cấu hình phù hợp.

Ví dụ:

```text
Training job
  -> Yêu cầu GPU
  -> Scheduler chọn Node có GPU
  -> Pod mount dataset hoặc đọc từ object storage
  -> Container chạy training
  -> Kết quả model được lưu ra storage
```

Giải thích từng phần:

- Node GPU được gắn label hoặc resource đặc biệt.
- Pod khai báo request GPU.
- Scheduler chỉ đặt Pod lên Node phù hợp.
- Job hoặc custom operator quản lý vòng đời training.

Ưu điểm trong use case này:

- Chia sẻ cụm GPU giữa nhiều team.
- Đóng gói môi trường ML bằng image.
- Có thể queue và quản lý tài nguyên tập trung.

Nhược điểm trong use case này:

- Setup GPU device plugin và driver phức tạp.
- Chi phí tài nguyên cao.
- Storage và network có thể ảnh hưởng lớn tới tốc độ training.
- Cần công cụ bổ sung như Kubeflow hoặc operator chuyên dụng nếu bài toán phức tạp.

## 12. Use case không nên dùng hoặc cần cân nhắc kỹ

### 12.1. Ứng dụng nhỏ, ít traffic, một team nhỏ

Nếu chỉ có một ứng dụng nhỏ, một database và traffic thấp, Kubernetes có thể quá nặng. Một máy ảo, Docker Compose hoặc một nền tảng PaaS đơn giản có thể phù hợp hơn.

Lý do:

- Ít service nên không cần orchestration phức tạp.
- Chi phí học và vận hành Kubernetes cao hơn lợi ích.
- Debug trực tiếp trên server đơn giản hơn.
- Backup, log, deploy có thể giải quyết bằng công cụ nhẹ hơn.

### 12.2. Team chưa có nền tảng container và DevOps

Nếu team chưa hiểu Docker image, network, Linux process, log, health check, CI/CD, thì nhảy thẳng vào Kubernetes dễ gây quá tải.

Nên học theo thứ tự:

```text
Linux cơ bản
  -> Docker
  -> Docker Compose
  -> CI/CD
  -> Kubernetes cơ bản
  -> Kubernetes production
```

Kubernetes không thay thế kiến thức nền. Nó khuếch đại cả điểm mạnh lẫn điểm yếu của quy trình vận hành hiện có.

### 12.3. Database production quan trọng nhưng team chưa sẵn sàng

Kubernetes có thể chạy database, nhưng không phải lúc nào cũng nên. Database cần backup, restore, replication, performance tuning, storage ổn định, upgrade an toàn và kế hoạch disaster recovery.

Nếu team chưa có kinh nghiệm, dùng managed database như RDS, Cloud SQL, Azure Database hoặc database vận hành ngoài cluster có thể an toàn hơn.

## 13. Ví dụ hoàn chỉnh: triển khai Spring Boot service

Giả sử có ứng dụng Spring Boot expose port `8080`, image là `my-company/springboot-learning:1.0.0`.

### 13.1. Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: springboot-learning
  labels:
    app: springboot-learning
spec:
  replicas: 3
  selector:
    matchLabels:
      app: springboot-learning
  template:
    metadata:
      labels:
        app: springboot-learning
    spec:
      containers:
        - name: app
          image: my-company/springboot-learning:1.0.0
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: production
          resources:
            requests:
              cpu: 250m
              memory: 512Mi
            limits:
              cpu: 1000m
              memory: 1Gi
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 40
            periodSeconds: 20
```

Giải thích chi tiết:

- `metadata.name`: tên Deployment để quản lý bằng `kubectl`.
- `replicas: 3`: chạy 3 Pod để tăng độ sẵn sàng và chịu tải.
- `selector.matchLabels`: xác định Pod nào thuộc Deployment.
- `template.metadata.labels`: label của Pod, phải khớp selector.
- `image`: image ứng dụng cần triển khai.
- `env`: truyền biến môi trường cho Spring Boot.
- `resources.requests.cpu`: lượng CPU tối thiểu scheduler dùng để đặt Pod.
- `resources.requests.memory`: lượng RAM tối thiểu Pod cần.
- `resources.limits`: giới hạn tối đa để tránh chiếm quá nhiều tài nguyên.
- `readinessProbe`: kiểm tra app đã sẵn sàng nhận traffic.
- `livenessProbe`: kiểm tra app có bị treo và cần restart không.

Nếu thiếu `resources.requests`, scheduler khó phân bổ tài nguyên chính xác. Nếu thiếu probe, Kubernetes có thể gửi traffic tới Pod chưa khởi động xong hoặc không restart container bị treo.

### 13.2. Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: springboot-learning
spec:
  type: ClusterIP
  selector:
    app: springboot-learning
  ports:
    - name: http
      port: 80
      targetPort: 8080
```

Giải thích:

- `type: ClusterIP`: service chỉ dùng nội bộ cluster.
- `selector.app`: chọn Pod có label `app: springboot-learning`.
- `port: 80`: các service khác gọi port 80.
- `targetPort: 8080`: request được chuyển tới container port 8080.

Khi một service khác gọi:

```text
http://springboot-learning
```

DNS nội bộ Kubernetes resolve tên này tới Service. Service route traffic tới một trong các Pod backend.

### 13.3. Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: springboot-learning
spec:
  rules:
    - host: api.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: springboot-learning
                port:
                  number: 80
```

Giải thích:

- Người dùng gọi `https://api.example.com`.
- DNS trỏ domain tới load balancer của Ingress Controller.
- Ingress Controller đọc rule host `api.example.com`.
- Request được chuyển tới Service `springboot-learning` port `80`.
- Service chuyển request tới Pod port `8080`.

Luồng đầy đủ:

```text
Client
  -> DNS api.example.com
  -> Load Balancer
  -> Ingress Controller
  -> Ingress rule
  -> Service springboot-learning:80
  -> Pod springboot-learning:8080
  -> Spring Boot application
```

### 13.4. ConfigMap và Secret

ConfigMap:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: springboot-learning-config
data:
  LOG_LEVEL: INFO
  PAYMENT_SERVICE_URL: http://payment-service
```

Secret:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: springboot-learning-secret
type: Opaque
stringData:
  DB_USERNAME: app_user
  DB_PASSWORD: change-me
```

Dùng trong Deployment:

```yaml
envFrom:
  - configMapRef:
      name: springboot-learning-config
  - secretRef:
      name: springboot-learning-secret
```

Giải thích:

- ConfigMap chứa cấu hình không nhạy cảm.
- Secret chứa thông tin nhạy cảm.
- `envFrom` nạp toàn bộ key trong ConfigMap và Secret thành biến môi trường.
- Ứng dụng Spring Boot có thể đọc qua `System.getenv()` hoặc cơ chế binding cấu hình.

Lưu ý production:

- Không commit secret thật vào Git.
- Không dùng password ví dụ trong môi trường thật.
- Nên dùng external secret manager hoặc sealed secret nếu triển khai GitOps.

## 14. Các lệnh kubectl thường dùng

| Mục đích | Lệnh |
|---|---|
| Xem node | `kubectl get nodes` |
| Xem pod | `kubectl get pods` |
| Xem pod theo namespace | `kubectl get pods -n production` |
| Xem chi tiết pod | `kubectl describe pod <pod-name>` |
| Xem log | `kubectl logs <pod-name>` |
| Xem log container cụ thể | `kubectl logs <pod-name> -c <container-name>` |
| Apply manifest | `kubectl apply -f file.yaml` |
| Xóa manifest | `kubectl delete -f file.yaml` |
| Xem deployment | `kubectl get deploy` |
| Scale deployment | `kubectl scale deploy <name> --replicas=5` |
| Xem rollout | `kubectl rollout status deploy/<name>` |
| Rollback | `kubectl rollout undo deploy/<name>` |
| Vào shell container | `kubectl exec -it <pod-name> -- sh` |
| Port forward | `kubectl port-forward svc/<service-name> 8080:80` |

Khi debug, thứ tự kiểm tra thường nên là:

```text
kubectl get pods
  -> kubectl describe pod
  -> kubectl logs
  -> kubectl get svc
  -> kubectl get endpoints hoặc endpointslice
  -> kubectl describe ingress
  -> kiểm tra DNS/network/policy nếu cần
```

## 15. Best practices quan trọng

### 15.1. Luôn khai báo resource requests và limits

Không khai báo tài nguyên khiến scheduler khó ra quyết định và cluster dễ bị tranh chấp tài nguyên.

Khuyến nghị:

- `requests` nên phản ánh tài nguyên ứng dụng cần để chạy ổn định.
- `limits` nên tránh quá thấp vì có thể làm app bị throttling hoặc OOMKilled.
- Theo dõi metric thực tế để điều chỉnh.

### 15.2. Dùng readiness và liveness probe đúng mục đích

Readiness không nên chỉ kiểm tra process còn sống. Nó nên trả lời câu hỏi: Pod đã sẵn sàng nhận traffic chưa?

Liveness không nên phụ thuộc quá nhiều vào dependency bên ngoài như database. Nếu database chập chờn mà liveness fail, Kubernetes có thể restart hàng loạt Pod dù ứng dụng không thật sự bị treo.

### 15.3. Thiết kế graceful shutdown

Khi rolling update hoặc scale down, Kubernetes gửi tín hiệu dừng container. Ứng dụng nên:

- Ngừng nhận request mới.
- Hoàn tất request đang xử lý.
- Đóng connection sạch sẽ.
- Thoát trong `terminationGracePeriodSeconds`.

Điều này giúp tránh mất request trong lúc deploy.

### 15.4. Không chạy container bằng root nếu không cần

Nên cấu hình security context:

```yaml
securityContext:
  runAsNonRoot: true
  allowPrivilegeEscalation: false
```

Kết hợp với image tối giản, scan vulnerability và policy kiểm soát quyền để giảm rủi ro bảo mật.

### 15.5. Quản lý manifest bằng Git

Không nên chỉnh sửa thủ công trực tiếp trên production rồi quên cập nhật lại Git. Git nên là nguồn sự thật cho manifest, đặc biệt khi dùng GitOps.

Quy trình tốt:

```text
Sửa manifest
  -> Pull request
  -> Review
  -> Merge
  -> CD/GitOps đồng bộ vào cluster
```

### 15.6. Observability là bắt buộc

Kubernetes production cần ít nhất:

- Metrics: CPU, memory, request rate, latency, error rate.
- Logs tập trung.
- Alerting.
- Dashboard.
- Distributed tracing nếu hệ thống microservices phức tạp.

Không có observability, Kubernetes trở thành một hộp đen khó debug.

## 16. Tổng kết

Kubernetes là nền tảng điều phối container mạnh mẽ, phù hợp với hệ thống cần scale, self-healing, rolling update, service discovery, quản lý cấu hình và vận hành nhiều service trên nhiều node. Điểm mạnh lớn nhất của Kubernetes là mô hình khai báo trạng thái mong muốn và hệ sinh thái controller phong phú.

Tuy nhiên, Kubernetes không phải lựa chọn mặc định cho mọi hệ thống. Nó có độ phức tạp cao, chi phí vận hành đáng kể và yêu cầu team hiểu rõ container, network, CI/CD, observability và bảo mật. Với hệ thống nhỏ, giải pháp đơn giản hơn có thể hiệu quả hơn. Với hệ thống lớn hoặc tổ chức nhiều team, Kubernetes có thể trở thành nền tảng hạ tầng rất mạnh nếu được thiết kế và vận hành đúng.

Một cách nhìn ngắn gọn:

```text
Docker giúp đóng gói ứng dụng.
Kubernetes giúp vận hành nhiều ứng dụng container hóa ở quy mô lớn.
CI/CD giúp đưa thay đổi vào hệ thống một cách tự động và có kiểm soát.
Observability giúp biết hệ thống đang khỏe hay đang lỗi.
```

Khi học Kubernetes, không nên chỉ học YAML. Nên hiểu vì sao từng tài nguyên tồn tại, nó giải quyết vấn đề gì, nó phối hợp với tài nguyên khác ra sao, và trong production nó có rủi ro gì. Đó mới là phần quan trọng nhất để dùng Kubernetes hiệu quả.
