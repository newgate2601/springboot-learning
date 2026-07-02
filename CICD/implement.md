# Thực hành CI/CD với Jenkins và Argo CD từ cơ bản tới nâng cao

## 1. Mục tiêu tài liệu

Tài liệu này là lộ trình thực hành CI/CD bằng **Jenkins** và **Argo CD**, đi từ nền tảng tới các bài thực tế hơn trong doanh nghiệp.

Bạn sẽ thực hành theo hai hướng:

- **Self-host:** tự dựng Jenkins, registry, Kubernetes, Argo CD trên máy cá nhân, VM, homelab hoặc server riêng.
- **Cloud:** dùng các dịch vụ cloud như managed Kubernetes, managed registry, IAM, load balancer, secret manager, object storage và monitoring.

Sau khi đi hết lộ trình, bạn nên hiểu được:

- CI/CD không chỉ là viết pipeline, mà là một quy trình đưa code từ máy developer tới môi trường chạy thật.
- Jenkins phù hợp ở đâu trong hệ thống CI/CD.
- Argo CD phù hợp ở đâu trong GitOps và Kubernetes deployment.
- Vì sao CI thường build, test, scan, package; còn CD nên triển khai artifact đã được kiểm soát.
- Cách tách source code repository, image registry, manifest repository và environment configuration.
- Cách deploy theo môi trường `dev`, `staging`, `production`.
- Cách rollback, promote version, quản lý secret, quan sát hệ thống và xử lý lỗi pipeline.

---

## 2. Bức tranh tổng quan

### 2.1. Mô hình CI/CD với Jenkins và Argo CD

```text
Developer
  -> Git push / Pull request
  -> Jenkins CI
      -> Checkout source code
      -> Build
      -> Unit test
      -> Static analysis
      -> Build Docker image
      -> Security scan
      -> Push image to registry
      -> Update Kubernetes manifest / Helm values / Kustomize image tag
  -> GitOps repository
      -> Argo CD detects change
      -> Sync to Kubernetes cluster
      -> Health check
      -> Rollback if needed
```

Trong mô hình này:

- **Jenkins** chủ yếu phụ trách CI và một phần release automation.
- **Argo CD** phụ trách CD theo kiểu GitOps cho Kubernetes.
- **Git** là nguồn sự thật cho cả source code và cấu hình triển khai.
- **Container registry** lưu Docker image đã build.
- **Kubernetes** là môi trường chạy ứng dụng.

### 2.2. Vì sao nên dùng Jenkins cùng Argo CD?

Jenkins rất mạnh ở phần automation:

- Build nhiều loại ứng dụng khác nhau.
- Tích hợp legacy system.
- Chạy script phức tạp.
- Tùy biến pipeline sâu.
- Dùng agent riêng cho từng loại workload.

Argo CD mạnh ở phần deployment Kubernetes:

- Theo dõi Git repository chứa manifest.
- So sánh desired state trong Git với live state trong cluster.
- Sync tự động hoặc thủ công.
- Hiển thị trạng thái application, resource, health.
- Hỗ trợ rollback theo Git history.

Kết hợp lại:

```text
Jenkins làm tốt việc tạo artifact.
Argo CD làm tốt việc đưa artifact vào Kubernetes theo GitOps.
```

---

## 3. Cần chuẩn bị kiến thức gì?

Phần này không đi quá sâu, nhưng bạn nên nắm sơ qua trước khi thực hành. Nếu thiếu phần nào, vẫn có thể làm lab, nhưng khi gặp lỗi sẽ khó debug hơn.

## 3.1. Git

Bạn cần biết:

- Repository, commit, branch, tag.
- Pull request hoặc merge request.
- Conflict và cách xử lý conflict cơ bản.
- Git remote.
- Git credential, SSH key, access token.
- `.gitignore`.

Vì sao cần:

- Jenkins lấy source code từ Git.
- Argo CD đọc manifest từ Git.
- GitOps xem Git là nguồn sự thật.
- Rollback thường là revert commit hoặc quay lại image tag cũ trong Git.

Thực tế cần dùng:

```bash
git clone
git status
git add
git commit
git push
git pull
git branch
git checkout
git tag
git revert
```

## 3.2. Linux và hệ điều hành

Bạn cần biết:

- Cấu trúc thư mục Linux cơ bản: `/etc`, `/var`, `/opt`, `/home`, `/tmp`.
- User, group, permission.
- Process và service.
- Environment variable.
- Log file.
- SSH vào server.
- Cài package bằng `apt`, `yum`, `dnf` hoặc package manager tương ứng.

Vì sao cần:

- Jenkins self-host thường chạy trên Linux server.
- Jenkins agent có thể chạy trong VM hoặc container.
- Docker daemon chạy như một service.
- Khi pipeline lỗi, bạn thường phải đọc log, kiểm tra permission, kiểm tra disk, kiểm tra process.

Lệnh nên quen:

```bash
pwd
ls -la
cd
mkdir
cp
mv
rm
cat
less
tail -f
grep
chmod
chown
ps aux
top
df -h
du -sh
systemctl status
journalctl -u
ssh
scp
```

## 3.3. Networking

Bạn cần biết:

- IP address, subnet, port.
- DNS.
- HTTP và HTTPS.
- TCP connection.
- Firewall, security group.
- NAT.
- Load balancer.
- Reverse proxy.
- TLS certificate.

Vì sao cần:

- Jenkins cần webhook từ Git server.
- Jenkins cần truy cập registry, Kubernetes API, SonarQube, artifact repository.
- Argo CD cần truy cập Git repository và Kubernetes API.
- Ứng dụng trong Kubernetes cần expose qua Service, Ingress hoặc LoadBalancer.
- Cloud lab sẽ dùng public/private subnet, security group, load balancer.

Công cụ nên biết:

```bash
ping
curl
telnet
nc
nslookup
dig
traceroute
ip addr
ss -tulpn
```

## 3.4. Docker và container

Bạn cần biết:

- Image và container khác nhau thế nào.
- Dockerfile.
- Build context.
- Layer cache.
- Tag image.
- Push/pull image.
- Volume, network.
- Multi-stage build.

Vì sao cần:

- CI thường build Docker image.
- Kubernetes chạy container image.
- Argo CD deploy manifest có image tag cụ thể.
- Registry là cầu nối giữa Jenkins và Kubernetes.

Lệnh nên quen:

```bash
docker build
docker run
docker ps
docker logs
docker exec
docker images
docker tag
docker push
docker pull
docker login
```

## 3.5. Kubernetes

Bạn cần biết:

- Pod, Deployment, ReplicaSet.
- Service.
- ConfigMap.
- Secret.
- Ingress.
- Namespace.
- PersistentVolume và PersistentVolumeClaim.
- ServiceAccount, Role, RoleBinding.
- Liveness probe, readiness probe.
- Rolling update và rollback.

Vì sao cần:

- Argo CD triển khai ứng dụng vào Kubernetes.
- Manifest GitOps thường là YAML Kubernetes, Helm chart hoặc Kustomize.
- Debug CD gần như luôn phải đọc trạng thái resource trong cluster.

Lệnh nên quen:

```bash
kubectl get pods
kubectl get deploy
kubectl get svc
kubectl get ingress
kubectl describe pod
kubectl logs
kubectl exec
kubectl apply
kubectl delete
kubectl rollout status
kubectl rollout history
kubectl rollout undo
```

## 3.6. YAML

Bạn cần biết:

- Indentation rất quan trọng.
- List dùng `-`.
- Key-value.
- String, number, boolean.
- Multi-line string.

Vì sao cần:

- Kubernetes manifest là YAML.
- Argo CD Application là YAML.
- Helm values là YAML.
- Jenkins đôi khi dùng CasC bằng YAML.

Lỗi thực tế thường gặp:

- Sai indentation.
- Dùng tab thay vì space.
- String có ký tự đặc biệt nhưng không quote.
- Copy YAML từ web bị lỗi khoảng trắng.

## 3.7. Jenkins

Bạn cần biết:

- Jenkins Controller.
- Jenkins Agent.
- Job, Pipeline, Multibranch Pipeline.
- Jenkinsfile.
- Credential store.
- Plugin.
- Workspace.
- Build parameter.
- Webhook.
- Shared library.

Jenkinsfile cơ bản:

```groovy
pipeline {
  agent any

  stages {
    stage('Build') {
      steps {
        sh 'mvn clean package'
      }
    }
  }
}
```

## 3.8. Argo CD

Bạn cần biết:

- Application.
- Project.
- Repository credential.
- Sync.
- Auto-sync.
- Prune.
- Self-heal.
- Health status.
- Sync wave.
- App of Apps.
- Helm/Kustomize support.
- RBAC.

Application cơ bản:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: demo-app
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/example/demo-gitops.git
    targetRevision: main
    path: apps/demo/dev
  destination:
    server: https://kubernetes.default.svc
    namespace: demo
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

## 3.9. Security cơ bản

Bạn cần biết:

- Không hard-code secret trong Git.
- Dùng credential store của Jenkins.
- Dùng Kubernetes Secret hoặc External Secrets.
- Phân quyền tối thiểu.
- Không dùng cluster-admin nếu không cần.
- Image phải scan lỗ hổng.
- Không chạy container bằng root nếu có thể tránh.

## 3.10. Cloud cơ bản

Bạn cần biết:

- IAM user, role, policy.
- VPC, subnet, route table.
- Security group/firewall.
- Managed Kubernetes: EKS, GKE, AKS.
- Managed registry: ECR, Artifact Registry, ACR.
- Load balancer.
- Object storage.
- Secret manager.
- Log và monitoring service.

Không cần học hết cloud trước khi làm lab, nhưng nên hiểu mỗi thành phần dùng để làm gì.

---

## 4. Công cụ nên chuẩn bị

## 4.1. Máy cá nhân

Khuyến nghị:

- CPU: tối thiểu 4 core, tốt hơn là 8 core.
- RAM: tối thiểu 16 GB nếu chạy Kubernetes local, Jenkins, registry, Argo CD cùng lúc.
- Disk: còn trống ít nhất 50 GB.
- OS: Linux, macOS hoặc Windows dùng WSL2.

Nếu dùng Windows:

- Cài WSL2 Ubuntu.
- Cài Docker Desktop hoặc Docker Engine trong WSL2.
- Cài Git.
- Cài kubectl.
- Cài Helm.

## 4.2. Tool local

Nên có:

```bash
git
docker
kubectl
helm
kind
minikube
argocd
jq
yq
curl
```

Tùy bài nâng cao:

```bash
terraform
trivy
sonar-scanner
sops
age
cosign
k6
```

## 4.3. Tài khoản cần chuẩn bị

Cho self-host:

- GitHub, GitLab hoặc Gitea.
- Docker Hub hoặc registry tự host.
- Một VM Linux nếu muốn thực hành ngoài máy cá nhân.

Cho cloud:

- Một tài khoản AWS, GCP hoặc Azure.
- Quyền tạo Kubernetes cluster.
- Quyền tạo registry.
- Quyền tạo IAM role/service account.
- Ngân sách nhỏ để tránh phát sinh chi phí lớn.

Lưu ý cloud:

- Luôn đặt budget alert.
- Xóa tài nguyên sau lab.
- Dùng region gần bạn.
- Không mở public access nếu không cần.

---

## 5. Kiến trúc repository nên dùng khi thực hành

Có hai cách phổ biến.

## 5.1. Một repo chứa cả source và manifest

```text
demo-app/
  src/
  Dockerfile
  Jenkinsfile
  k8s/
    deployment.yaml
    service.yaml
    ingress.yaml
```

Ưu điểm:

- Dễ bắt đầu.
- Ít repo, ít credential.
- Phù hợp học cơ bản.

Nhược điểm:

- Khó quản lý nhiều môi trường.
- Dễ lẫn trách nhiệm giữa developer và platform team.
- Không sạch bằng GitOps repo riêng.

Use case:

- Dự án nhỏ.
- Lab cá nhân.
- Team ít người.

## 5.2. Tách source repo và GitOps repo

```text
demo-app-source/
  src/
  Dockerfile
  Jenkinsfile

demo-app-gitops/
  apps/
    demo-app/
      base/
        deployment.yaml
        service.yaml
      overlays/
        dev/
          kustomization.yaml
        staging/
          kustomization.yaml
        prod/
          kustomization.yaml
```

Ưu điểm:

- Rõ ràng giữa code và deployment config.
- Dễ kiểm soát promotion qua môi trường.
- Phù hợp GitOps.
- Dễ phân quyền: developer sửa app, platform team kiểm soát manifest.

Nhược điểm:

- Cần quản lý nhiều repo.
- Jenkins phải có quyền update GitOps repo.
- Cần thống nhất convention image tag, branch, folder.

Use case:

- Team trung bình/lớn.
- Microservices.
- Production Kubernetes.
- Platform engineering.

Khuyến nghị:

- Giai đoạn cơ bản dùng một repo.
- Từ bài GitOps trở đi nên tách source repo và GitOps repo.

---

## 6. Lộ trình tổng quan

```text
Phần A: Nền tảng local
  -> Lab 1: Chạy app local và viết Dockerfile
  -> Lab 2: Chạy Jenkins local
  -> Lab 3: Pipeline build/test cơ bản
  -> Lab 4: Build và push Docker image

Phần B: Self-host Kubernetes + Argo CD
  -> Lab 5: Dựng Kubernetes local bằng kind/minikube
  -> Lab 6: Cài Argo CD
  -> Lab 7: Deploy app bằng manifest
  -> Lab 8: Jenkins update GitOps repo
  -> Lab 9: Auto-sync, rollback, promotion

Phần C: Self-host nâng cao
  -> Lab 10: Jenkins agent động trên Kubernetes
  -> Lab 11: Helm/Kustomize theo nhiều môi trường
  -> Lab 12: Secret management
  -> Lab 13: Quality gate và security scan
  -> Lab 14: Observability cho pipeline và app
  -> Lab 15: Canary/Blue-Green cơ bản

Phần D: Cloud cơ bản
  -> Lab 16: Tạo managed Kubernetes cluster
  -> Lab 17: Dùng managed container registry
  -> Lab 18: Deploy bằng Argo CD lên cloud cluster
  -> Lab 19: Expose app qua cloud load balancer/ingress

Phần E: Cloud nâng cao
  -> Lab 20: Terraform tạo hạ tầng
  -> Lab 21: External Secrets với cloud secret manager
  -> Lab 22: Multi-environment, multi-cluster
  -> Lab 23: Progressive delivery
  -> Lab 24: Disaster recovery và backup GitOps
```

---

# Phần A: Nền tảng local

## Lab 1. Chạy ứng dụng local và viết Dockerfile

### Mục tiêu

- Biết cách build ứng dụng trên máy local.
- Viết Dockerfile đóng gói ứng dụng.
- Chạy container local.
- Hiểu artifact đầu tiên trong CI/CD là gì.

### Cần chuẩn bị

- Một ứng dụng mẫu, ví dụ Spring Boot, Node.js, Go hoặc Python.
- Git.
- Docker.

Ví dụ với Spring Boot:

```bash
./mvnw clean package
java -jar target/*.jar
```

### Việc cần làm

1. Tạo hoặc dùng một ứng dụng mẫu.
2. Đảm bảo ứng dụng chạy local.
3. Viết Dockerfile.
4. Build image.
5. Run container.
6. Kiểm tra endpoint bằng `curl`.

Dockerfile ví dụ cho Spring Boot:

```dockerfile
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build và run:

```bash
./mvnw clean package
docker build -t demo-app:local .
docker run --rm -p 8080:8080 demo-app:local
curl http://localhost:8080/actuator/health
```

### Kết quả mong đợi

- App chạy được trong container.
- Bạn hiểu image tag là tên phiên bản của artifact container.

### Lưu ý

- Không copy source code thừa vào image nếu không cần.
- Nên dùng `.dockerignore`.
- Không đưa secret vào Dockerfile.
- Image nên càng nhỏ càng tốt nhưng vẫn dễ debug.

### Ưu điểm

- Dễ bắt đầu.
- Môi trường chạy gần giống CI.
- Là nền tảng cho Kubernetes.

### Nhược điểm

- Nếu Dockerfile kém, image sẽ nặng, build chậm, dễ lộ file nhạy cảm.

### Use case thực tế

- Mọi hệ thống deploy bằng Kubernetes đều gần như phải có bước đóng gói container.
- CI sẽ tự động hóa chính các bước bạn vừa làm thủ công.

---

## Lab 2. Chạy Jenkins local bằng Docker

### Mục tiêu

- Dựng Jenkins nhanh để học pipeline.
- Hiểu Jenkins Controller.
- Biết Jenkins lưu dữ liệu ở đâu.

### Cần chuẩn bị

- Docker.
- Port `8080` hoặc port khác còn trống.

### Việc cần làm

Chạy Jenkins:

```bash
docker volume create jenkins_home

docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  jenkins/jenkins:lts
```

Lấy initial admin password:

```bash
docker logs jenkins
```

Hoặc:

```bash
docker exec -it jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Sau đó:

1. Truy cập `http://localhost:8080`.
2. Nhập password.
3. Cài plugin khuyến nghị.
4. Tạo admin user.
5. Vào dashboard Jenkins.

### Kết quả mong đợi

- Jenkins chạy được local.
- Bạn đăng nhập được dashboard.

### Lưu ý

- Jenkins trong Docker không tự có quyền chạy Docker build từ host.
- Bài này chỉ để làm quen UI và pipeline đơn giản.
- Với production, không nên chạy Jenkins tùy tiện không backup volume.

### Ưu điểm

- Nhanh.
- Không cần cài Jenkins trực tiếp lên OS.
- Dễ xóa và dựng lại.

### Nhược điểm

- Build Docker image từ Jenkins container cần cấu hình thêm.
- Nếu mất volume thì mất toàn bộ config.

### Use case thực tế

- Dùng cho lab, PoC, training.
- Doanh nghiệp có thể chạy Jenkins bằng container, nhưng thường cần storage, backup, reverse proxy, TLS và agent tách riêng.

---

## Lab 3. Jenkins Pipeline build/test cơ bản

### Mục tiêu

- Viết Jenkinsfile đầu tiên.
- Chạy build/test tự động.
- Hiểu stage, step, post action.

### Cần chuẩn bị

- Jenkins chạy được.
- Source code nằm trong Git repository.
- Jenkins cài plugin Git và Pipeline.

### Việc cần làm

Tạo `Jenkinsfile` trong source repo:

```groovy
pipeline {
  agent any

  options {
    timestamps()
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build') {
      steps {
        sh './mvnw -DskipTests package'
      }
    }

    stage('Test') {
      steps {
        sh './mvnw test'
      }
    }
  }

  post {
    always {
      junit 'target/surefire-reports/*.xml'
    }
  }
}
```

Trong Jenkins:

1. Tạo Pipeline job.
2. Chọn Pipeline script from SCM.
3. Nhập Git repository URL.
4. Chọn branch.
5. Chạy `Build Now`.

### Kết quả mong đợi

- Jenkins clone source code.
- Jenkins chạy build.
- Jenkins chạy test.
- Jenkins hiển thị test report.

### Lưu ý

- Nếu Jenkins chạy trong container, cần đảm bảo có Java/Maven hoặc dùng agent image phù hợp.
- Nên commit `mvnw` để pipeline không phụ thuộc Maven cài sẵn.
- Test fail thì pipeline phải fail.

### Ưu điểm

- Feedback tự động.
- Bắt lỗi sớm.
- Tạo nền tảng cho các stage sau.

### Nhược điểm

- Nếu test chậm, developer sẽ ít quan tâm pipeline.
- Nếu test flaky, team mất niềm tin vào CI.

### Use case thực tế

- Mọi team nên có pipeline tối thiểu: checkout, build, unit test.
- Đây là baseline trước khi thêm Docker, scan, deploy.

---

## Lab 4. Build và push Docker image từ Jenkins

### Mục tiêu

- Jenkins build Docker image.
- Push image lên registry.
- Quản lý credential registry trong Jenkins.
- Dùng image tag theo commit SHA.

### Cần chuẩn bị

- Docker registry: Docker Hub, Harbor, Nexus, GitLab Registry hoặc registry local.
- Jenkins có khả năng chạy Docker command.
- Credential registry trong Jenkins.

### Việc cần làm

Tạo credential trong Jenkins:

- Kind: Username with password.
- ID: `docker-registry-credential`.
- Username/password: tài khoản registry.

Jenkinsfile ví dụ:

```groovy
pipeline {
  agent any

  environment {
    REGISTRY = 'docker.io'
    IMAGE_NAME = 'your-user/demo-app'
    IMAGE_TAG = "${env.GIT_COMMIT.take(7)}"
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build and Test') {
      steps {
        sh './mvnw clean test package'
      }
    }

    stage('Build Image') {
      steps {
        sh 'docker build -t $REGISTRY/$IMAGE_NAME:$IMAGE_TAG .'
      }
    }

    stage('Push Image') {
      steps {
        withCredentials([usernamePassword(
          credentialsId: 'docker-registry-credential',
          usernameVariable: 'REGISTRY_USER',
          passwordVariable: 'REGISTRY_PASSWORD'
        )]) {
          sh '''
            echo "$REGISTRY_PASSWORD" | docker login "$REGISTRY" -u "$REGISTRY_USER" --password-stdin
            docker push "$REGISTRY/$IMAGE_NAME:$IMAGE_TAG"
          '''
        }
      }
    }
  }
}
```

### Kết quả mong đợi

- Mỗi commit tạo một image tag riêng.
- Image được push lên registry.
- Jenkins log không lộ password.

### Lưu ý

- Không dùng tag `latest` làm tag duy nhất cho môi trường thật.
- Nên dùng commit SHA, semantic version hoặc build number.
- Không in secret ra log.
- Jenkins agent cần quyền Docker cẩn thận vì Docker socket có quyền rất mạnh.

### Ưu điểm

- Artifact rõ ràng, có thể truy vết.
- Image có thể được deploy ở nhiều môi trường.

### Nhược điểm

- Quản lý Docker-in-Docker hoặc Docker socket cần hiểu security.
- Registry public có thể giới hạn rate hoặc lộ image nếu cấu hình sai.

### Use case thực tế

- Đây là CI flow phổ biến: build, test, build image, push image.
- Argo CD ở phần sau sẽ dùng image này để deploy.

---

# Phần B: Self-host Kubernetes và Argo CD

## Lab 5. Dựng Kubernetes local bằng kind hoặc minikube

### Mục tiêu

- Có Kubernetes cluster local để thực hành CD.
- Biết dùng `kubectl`.
- Deploy thử một workload.

### Cần chuẩn bị

- Docker.
- `kubectl`.
- `kind` hoặc `minikube`.

### Cách 1: dùng kind

```bash
kind create cluster --name cicd-lab
kubectl cluster-info --context kind-cicd-lab
kubectl get nodes
```

### Cách 2: dùng minikube

```bash
minikube start
kubectl get nodes
```

### Việc cần làm

1. Tạo namespace:

```bash
kubectl create namespace demo
```

2. Deploy thử nginx:

```bash
kubectl create deployment nginx --image=nginx -n demo
kubectl expose deployment nginx --port=80 --target-port=80 -n demo
kubectl get all -n demo
```

3. Port-forward để kiểm tra:

```bash
kubectl port-forward svc/nginx 8081:80 -n demo
curl http://localhost:8081
```

### Kết quả mong đợi

- Cluster chạy được.
- Bạn deploy được workload đầu tiên.
- Bạn hiểu namespace và service cơ bản.

### Lưu ý

- kind phù hợp CI/local vì nhẹ và dễ tạo/xóa.
- minikube thân thiện hơn cho người mới.
- Kubernetes local không giống hoàn toàn cloud, nhất là phần load balancer và storage.

### Ưu điểm

- Không tốn tiền cloud.
- Dễ thử sai.
- Dễ xóa cluster.

### Nhược điểm

- Không mô phỏng đầy đủ network, IAM, load balancer cloud.
- Máy yếu sẽ chậm.

### Use case thực tế

- Developer dùng cluster local để test manifest.
- Platform engineer dùng kind để test chart, controller, policy.

---

## Lab 6. Cài Argo CD trên Kubernetes local

### Mục tiêu

- Cài Argo CD.
- Truy cập UI.
- Đăng nhập bằng CLI.
- Hiểu namespace `argocd`.

### Cần chuẩn bị

- Kubernetes cluster.
- `kubectl`.
- `argocd` CLI nếu muốn dùng dòng lệnh.

### Việc cần làm

Cài Argo CD:

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl get pods -n argocd
```

Port-forward UI:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Lấy password admin ban đầu:

```bash
kubectl get secret argocd-initial-admin-secret -n argocd \
  -o jsonpath="{.data.password}" | base64 -d
```

Truy cập:

```text
https://localhost:8080
```

### Kết quả mong đợi

- Argo CD UI chạy được.
- Đăng nhập được user `admin`.

### Lưu ý

- Password ban đầu nên đổi sau khi đăng nhập.
- Với production, không nên expose Argo CD admin UI public không bảo vệ.
- Nên cấu hình SSO và RBAC cho môi trường thật.

### Ưu điểm

- Dễ nhìn trạng thái resource.
- Học GitOps trực quan.

### Nhược điểm

- Thêm một hệ thống cần vận hành.
- Nếu phân quyền sai, Argo CD có thể có quyền quá rộng trong cluster.

### Use case thực tế

- Team dùng Argo CD để triển khai hàng chục hoặc hàng trăm service lên Kubernetes.
- Platform team dùng UI để quan sát drift và sync status.

---

## Lab 7. Deploy ứng dụng bằng Argo CD từ Git repository

### Mục tiêu

- Tạo GitOps repo.
- Viết Kubernetes manifest.
- Tạo Argo CD Application.
- Deploy app bằng sync.

### Cần chuẩn bị

- Image đã push từ Lab 4.
- Git repository chứa manifest.
- Argo CD chạy được.

### Cấu trúc GitOps repo

```text
demo-app-gitops/
  apps/
    demo-app/
      dev/
        deployment.yaml
        service.yaml
        application.yaml
```

`deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: demo-app
  namespace: demo
spec:
  replicas: 1
  selector:
    matchLabels:
      app: demo-app
  template:
    metadata:
      labels:
        app: demo-app
    spec:
      containers:
        - name: demo-app
          image: docker.io/your-user/demo-app:CHANGE_ME
          ports:
            - containerPort: 8080
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
            initialDelaySeconds: 30
            periodSeconds: 20
```

`service.yaml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: demo-app
  namespace: demo
spec:
  selector:
    app: demo-app
  ports:
    - port: 80
      targetPort: 8080
```

`application.yaml`:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: demo-app-dev
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/your-user/demo-app-gitops.git
    targetRevision: main
    path: apps/demo-app/dev
  destination:
    server: https://kubernetes.default.svc
    namespace: demo
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
```

Apply Application:

```bash
kubectl apply -f apps/demo-app/dev/application.yaml
```

### Kết quả mong đợi

- Argo CD tạo Deployment và Service.
- App chạy trong namespace `demo`.
- UI hiển thị app `Healthy` và `Synced`.

### Lưu ý

- `repoURL` phải đúng và Argo CD phải có quyền đọc repo.
- Namespace trong manifest và destination nên thống nhất.
- Nếu image private, cluster cần image pull secret.

### Ưu điểm

- Deployment được quản lý bằng Git.
- Dễ biết phiên bản nào đang chạy.
- Dễ rollback bằng Git.

### Nhược điểm

- Cần discipline: không sửa live resource bằng tay.
- Nếu auto-sync bật sai môi trường, có thể deploy ngoài ý muốn.

### Use case thực tế

- Deploy service lên `dev` tự động sau khi merge.
- Cho phép team xem trạng thái release trên Argo CD UI.

---

## Lab 8. Jenkins update GitOps repo để Argo CD tự deploy

### Mục tiêu

- Hoàn thiện flow Jenkins + Argo CD.
- Jenkins build image và cập nhật image tag trong GitOps repo.
- Argo CD tự phát hiện thay đổi và sync.

### Cần chuẩn bị

- Source repo.
- GitOps repo.
- Jenkins credential để push GitOps repo.
- Registry credential.
- Argo CD đang theo dõi GitOps repo.

### Luồng thực hành

```text
Push code vào source repo
  -> Jenkins chạy CI
  -> Jenkins build image tag abc1234
  -> Jenkins push image lên registry
  -> Jenkins sửa manifest image tag thành abc1234
  -> Jenkins commit/push vào GitOps repo
  -> Argo CD sync
  -> Kubernetes rollout version mới
```

### Jenkinsfile minh họa

```groovy
pipeline {
  agent any

  environment {
    REGISTRY = 'docker.io'
    IMAGE_NAME = 'your-user/demo-app'
    IMAGE_TAG = "${env.GIT_COMMIT.take(7)}"
    GITOPS_REPO = 'https://github.com/your-user/demo-app-gitops.git'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build and Test') {
      steps {
        sh './mvnw clean test package'
      }
    }

    stage('Build Image') {
      steps {
        sh 'docker build -t $REGISTRY/$IMAGE_NAME:$IMAGE_TAG .'
      }
    }

    stage('Push Image') {
      steps {
        withCredentials([usernamePassword(
          credentialsId: 'docker-registry-credential',
          usernameVariable: 'REGISTRY_USER',
          passwordVariable: 'REGISTRY_PASSWORD'
        )]) {
          sh '''
            echo "$REGISTRY_PASSWORD" | docker login "$REGISTRY" -u "$REGISTRY_USER" --password-stdin
            docker push "$REGISTRY/$IMAGE_NAME:$IMAGE_TAG"
          '''
        }
      }
    }

    stage('Update GitOps Repo') {
      steps {
        withCredentials([usernamePassword(
          credentialsId: 'gitops-repo-token',
          usernameVariable: 'GIT_USER',
          passwordVariable: 'GIT_TOKEN'
        )]) {
          sh '''
            rm -rf gitops
            git clone https://$GIT_USER:$GIT_TOKEN@github.com/your-user/demo-app-gitops.git gitops
            cd gitops
            sed -i "s|image: .*/demo-app:.*|image: $REGISTRY/$IMAGE_NAME:$IMAGE_TAG|g" apps/demo-app/dev/deployment.yaml
            git config user.email "jenkins@example.com"
            git config user.name "jenkins"
            git add apps/demo-app/dev/deployment.yaml
            git commit -m "Deploy demo-app $IMAGE_TAG to dev"
            git push origin main
          '''
        }
      }
    }
  }
}
```

### Kết quả mong đợi

- Mỗi lần Jenkins build thành công, GitOps repo có commit mới.
- Argo CD sync version mới.
- Kubernetes rollout thành công.

### Lưu ý

- `sed` dễ lỗi nếu format YAML thay đổi; thực tế nên dùng `yq`, Kustomize image transformer hoặc Helm values.
- Jenkins nên tạo pull request cho staging/prod thay vì push thẳng.
- GitOps repo nên có branch protection.
- Commit message nên có app, version, environment.

### Ưu điểm

- CI và CD tách trách nhiệm rõ ràng.
- Argo CD không cần Jenkins gọi trực tiếp Kubernetes API.
- Audit trail nằm trong Git.

### Nhược điểm

- Jenkins cần quyền ghi GitOps repo.
- Có thể tạo nhiều commit nếu build liên tục.
- Cần xử lý conflict khi nhiều pipeline cập nhật cùng file.

### Use case thực tế

- Rất phổ biến trong mô hình GitOps.
- Jenkins chỉ cập nhật desired state, còn Argo CD kéo cluster về desired state.

---

## Lab 9. Auto-sync, rollback và promote version

### Mục tiêu

- Hiểu deploy tự động ở `dev`.
- Hiểu promote thủ công qua `staging` và `prod`.
- Rollback bằng Git.

### Cấu trúc GitOps đề xuất

```text
apps/
  demo-app/
    base/
      deployment.yaml
      service.yaml
    overlays/
      dev/
        kustomization.yaml
      staging/
        kustomization.yaml
      prod/
        kustomization.yaml
```

`base/deployment.yaml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: demo-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: demo-app
  template:
    metadata:
      labels:
        app: demo-app
    spec:
      containers:
        - name: demo-app
          image: docker.io/your-user/demo-app:default
          ports:
            - containerPort: 8080
```

`overlays/dev/kustomization.yaml`:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: demo-dev
resources:
  - ../../base
images:
  - name: docker.io/your-user/demo-app
    newTag: abc1234
```

### Việc cần làm

1. Tạo overlay cho `dev`, `staging`, `prod`.
2. Tạo Argo CD Application riêng cho từng environment.
3. Bật auto-sync cho `dev`.
4. Để manual sync cho `staging` và `prod`.
5. Jenkins chỉ update `dev`.
6. Promote bằng cách copy image tag từ `dev` sang `staging`, sau đó sang `prod`.

### Rollback

Cách 1: revert commit GitOps:

```bash
git revert <commit-id>
git push
```

Cách 2: sửa `newTag` về version cũ:

```yaml
images:
  - name: docker.io/your-user/demo-app
    newTag: old1234
```

### Kết quả mong đợi

- `dev` deploy tự động.
- `staging` và `prod` có kiểm soát.
- Rollback được bằng Git.

### Lưu ý

- Production không nên auto-sync nếu team chưa đủ maturity.
- Rollback database khó hơn rollback app.
- Cần ghi rõ version đang chạy ở từng môi trường.

### Ưu điểm

- Kiểm soát release tốt hơn.
- Dễ audit.
- Dễ phân quyền.

### Nhược điểm

- Thêm thao tác promote.
- Nếu nhiều service, cần tooling để tránh làm tay quá nhiều.

### Use case thực tế

- Dev auto-deploy để test nhanh.
- Staging deploy sau khi QA đồng ý.
- Production deploy sau approval hoặc change request.

---

# Phần C: Self-host nâng cao

## Lab 10. Jenkins agent động trên Kubernetes

### Mục tiêu

- Jenkins Controller không trực tiếp chạy mọi job.
- Mỗi pipeline chạy trong pod agent riêng.
- Tăng khả năng scale và cô lập môi trường build.

### Cần chuẩn bị

- Jenkins.
- Kubernetes cluster.
- Jenkins Kubernetes plugin.
- ServiceAccount cho Jenkins trong cluster.

### Việc cần làm

1. Cài Kubernetes plugin cho Jenkins.
2. Tạo namespace `jenkins-agents`.
3. Tạo ServiceAccount và RBAC cho Jenkins tạo pod.
4. Cấu hình Jenkins Cloud: Kubernetes.
5. Viết Jenkinsfile dùng agent Kubernetes.

Ví dụ Jenkinsfile:

```groovy
pipeline {
  agent {
    kubernetes {
      yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
    - name: maven
      image: maven:3.9-eclipse-temurin-21
      command:
        - cat
      tty: true
    - name: docker
      image: docker:26-cli
      command:
        - cat
      tty: true
'''
    }
  }

  stages {
    stage('Build') {
      steps {
        container('maven') {
          sh 'mvn -version'
          sh 'mvn clean test package'
        }
      }
    }
  }
}
```

### Kết quả mong đợi

- Jenkins tự tạo pod agent khi job chạy.
- Job xong pod bị xóa.
- Build environment được định nghĩa bằng YAML.

### Lưu ý

- Build Docker image trong Kubernetes agent cần cách an toàn: Kaniko, BuildKit, Buildah hoặc Docker-in-Docker có kiểm soát.
- Không nên mount Docker socket bừa bãi.
- Cần giới hạn CPU/memory cho agent pod.

### Ưu điểm

- Scale tốt.
- Môi trường build sạch.
- Dễ dùng image agent riêng.

### Nhược điểm

- Cấu hình phức tạp hơn.
- Debug agent pod cần hiểu Kubernetes.
- Build image cần thiết kế cẩn thận.

### Use case thực tế

- Công ty có nhiều job CI chạy song song.
- Jenkins Controller ổn định hơn vì workload chạy trên agent.

---

## Lab 11. Helm hoặc Kustomize cho nhiều môi trường

### Mục tiêu

- Không copy-paste manifest quá nhiều.
- Quản lý khác biệt giữa môi trường.
- Chuẩn hóa deployment config.

### Chọn Kustomize khi nào?

Phù hợp khi:

- Manifest khá gần Kubernetes YAML gốc.
- Khác biệt môi trường không quá phức tạp.
- Muốn overlay đơn giản.

Ưu điểm:

- Tích hợp sẵn trong `kubectl`.
- Không cần template language.
- Dễ review diff.

Nhược điểm:

- Khi logic phức tạp, Kustomize có thể khó biểu diễn.

### Chọn Helm khi nào?

Phù hợp khi:

- App có nhiều option.
- Muốn package chart tái sử dụng.
- Cần template hóa nhiều resource.

Ưu điểm:

- Ecosystem lớn.
- Dễ publish chart.
- Phù hợp platform chart.

Nhược điểm:

- Template có thể khó đọc.
- Render ra YAML cần kiểm tra kỹ.

### Việc cần làm với Kustomize

```text
base/
  deployment.yaml
  service.yaml
  kustomization.yaml
overlays/
  dev/
    kustomization.yaml
  staging/
    kustomization.yaml
  prod/
    kustomization.yaml
```

Kiểm tra render:

```bash
kubectl kustomize overlays/dev
```

### Việc cần làm với Helm

```text
charts/demo-app/
  Chart.yaml
  values.yaml
  values-dev.yaml
  values-staging.yaml
  values-prod.yaml
  templates/
    deployment.yaml
    service.yaml
```

Kiểm tra render:

```bash
helm template demo-app charts/demo-app -f charts/demo-app/values-dev.yaml
```

### Kết quả mong đợi

- Mỗi môi trường có config riêng.
- Không duplicate toàn bộ manifest.
- Argo CD deploy được từ Helm hoặc Kustomize.

### Lưu ý

- Đừng template hóa quá mức.
- Giá trị nhạy cảm không nên để plain text trong values.
- Luôn render YAML trước khi merge.

### Use case thực tế

- Dev replicas = 1, prod replicas = 3.
- Dev dùng resource nhỏ, prod dùng resource lớn.
- Prod bật HPA, PodDisruptionBudget, network policy.

---

## Lab 12. Secret management

### Mục tiêu

- Không lưu secret plain text trong Git.
- Biết các cách quản lý secret cho Jenkins và Kubernetes.
- Thực hành một hướng phù hợp self-host.

### Secret trong Jenkins

Dùng Jenkins Credentials:

- Username/password.
- Secret text.
- SSH key.
- File credential.

Nguyên tắc:

- Pipeline chỉ tham chiếu `credentialsId`.
- Không echo secret.
- Không truyền secret qua command dễ lộ trong process list nếu tránh được.

### Secret trong Kubernetes

Cách cơ bản:

```bash
kubectl create secret generic demo-secret \
  --from-literal=DB_USERNAME=demo \
  --from-literal=DB_PASSWORD=secret \
  -n demo
```

Nhược điểm:

- Nếu apply thủ công, khó GitOps.
- Kubernetes Secret chỉ base64, không phải mã hóa mạnh.

### GitOps secret option

Các hướng phổ biến:

- Sealed Secrets.
- External Secrets Operator.
- SOPS + age/GPG.
- Vault.

### Bài thực hành gợi ý: SOPS + age

Luồng:

```text
Developer/platform encrypt secret bằng SOPS
  -> Commit encrypted secret vào GitOps repo
  -> Argo CD sync
  -> Controller/plugin decrypt hoặc apply secret an toàn theo mô hình đã chọn
```

### Kết quả mong đợi

- Git không chứa secret plain text.
- App đọc secret từ Kubernetes Secret.
- Jenkins credential không bị lộ log.

### Lưu ý

- Secret management là phần dễ bị làm qua loa nhất trong lab nhưng cực kỳ quan trọng ở production.
- Cần rotation secret.
- Cần audit ai có quyền đọc secret.

### Ưu điểm

- Giảm nguy cơ lộ thông tin nhạy cảm.
- Phù hợp GitOps hơn.

### Nhược điểm

- Tăng độ phức tạp.
- Cần quản lý key mã hóa.
- Debug khó hơn.

### Use case thực tế

- Database password.
- API key bên thứ ba.
- Registry credential.
- TLS private key.

---

## Lab 13. Quality gate và security scan

### Mục tiêu

- Thêm kiểm tra chất lượng vào CI.
- Scan dependency và container image.
- Chặn deploy nếu có lỗi nghiêm trọng.

### Cần chuẩn bị

- SonarQube hoặc SonarCloud.
- Trivy hoặc công cụ scan tương đương.
- Jenkins plugin hoặc CLI.

### Stage gợi ý

```text
Checkout
  -> Unit test
  -> Code coverage
  -> Sonar analysis
  -> Dependency scan
  -> Build image
  -> Image scan
  -> Push image
  -> Update GitOps repo
```

### Trivy scan image

```bash
trivy image --severity HIGH,CRITICAL --exit-code 1 docker.io/your-user/demo-app:abc1234
```

### Quality gate

Quy tắc gợi ý:

- Unit test phải pass.
- Coverage không giảm quá ngưỡng.
- Không có vulnerability critical.
- Không có secret trong source.
- Dockerfile không chạy root nếu policy yêu cầu.

### Kết quả mong đợi

- Pipeline fail trước khi push hoặc trước khi update GitOps nếu artifact không đạt chuẩn.
- Report được lưu trong Jenkins.

### Lưu ý

- Không nên bật quá nhiều rule cùng lúc nếu team chưa quen.
- Phân biệt lỗi cần block và lỗi cần cảnh báo.
- Scan database cần update thường xuyên.

### Ưu điểm

- Bắt lỗi sớm.
- Giảm rủi ro bảo mật.
- Tạo tiêu chuẩn release rõ ràng.

### Nhược điểm

- Pipeline chậm hơn.
- Có thể có false positive.
- Cần người chịu trách nhiệm xử lý findings.

### Use case thực tế

- Enterprise thường yêu cầu security scan trước khi release.
- Dự án tài chính, y tế, thương mại điện tử thường có quality gate nghiêm hơn.

---

## Lab 14. Observability cho pipeline và ứng dụng

### Mục tiêu

- Quan sát app sau deploy.
- Biết pipeline thành công chưa đủ, app phải healthy trong runtime.
- Kết nối CI/CD với monitoring.

### Cần chuẩn bị

- Prometheus.
- Grafana.
- Loki hoặc EFK/ELK nếu muốn logging.
- Alertmanager nếu muốn alert.

### Việc cần làm

1. Cài kube-prometheus-stack bằng Helm.
2. Expose metric endpoint của app.
3. Tạo dashboard cơ bản.
4. Tạo alert khi app down.
5. Sau deploy, kiểm tra rollout và health.

Ví dụ kiểm tra rollout trong Jenkins:

```bash
kubectl rollout status deployment/demo-app -n demo --timeout=120s
```

Nếu dùng GitOps chuẩn, Jenkins không nhất thiết truy cập cluster. Khi đó có thể:

- Jenkins chỉ update GitOps.
- Argo CD sync.
- Alert/monitoring báo nếu app lỗi.
- Một job riêng gọi Argo CD API/CLI để đợi health nếu quy trình cho phép.

### Kết quả mong đợi

- Có metric app.
- Có log app.
- Biết deploy xong app có thật sự chạy không.

### Lưu ý

- Pipeline xanh nhưng app vẫn có thể lỗi runtime.
- Readiness/liveness probe rất quan trọng.
- Alert nên có ngưỡng hợp lý, tránh spam.

### Ưu điểm

- Phát hiện lỗi sau deploy nhanh.
- Hỗ trợ rollback.
- Tăng niềm tin vào CD.

### Nhược điểm

- Cài monitoring tốn tài nguyên.
- Dashboard nhiều nhưng không có alert tốt thì vẫn khó vận hành.

### Use case thực tế

- Sau release production, team cần theo dõi error rate, latency, CPU, memory.
- SRE dùng metric để quyết định rollback hoặc tiếp tục rollout.

---

## Lab 15. Canary hoặc Blue-Green cơ bản

### Mục tiêu

- Hiểu deploy nâng cao để giảm rủi ro.
- Không đưa 100% traffic vào version mới ngay lập tức.

### Blue-Green deployment

Ý tưởng:

```text
Blue = version đang chạy
Green = version mới
Test Green ổn
  -> chuyển traffic từ Blue sang Green
Nếu lỗi
  -> chuyển lại Blue
```

Ưu điểm:

- Rollback nhanh.
- Dễ hiểu.

Nhược điểm:

- Tốn tài nguyên vì chạy hai version.
- Database migration phải rất cẩn thận.

### Canary deployment

Ý tưởng:

```text
Version mới nhận 5% traffic
  -> nếu ổn tăng 25%
  -> nếu ổn tăng 50%
  -> nếu ổn tăng 100%
```

Ưu điểm:

- Giảm rủi ro production.
- Phát hiện lỗi theo traffic thật.

Nhược điểm:

- Cần metric tốt.
- Cần ingress/service mesh hoặc progressive delivery controller.

### Tool có thể dùng

- Argo Rollouts.
- Flagger.
- Istio, Linkerd, NGINX Ingress, ALB Ingress tùy mô hình.

### Việc cần làm

1. Cài Argo Rollouts.
2. Chuyển Deployment sang Rollout.
3. Cấu hình canary steps.
4. Deploy version lỗi có chủ ý.
5. Quan sát rollback hoặc pause.

### Kết quả mong đợi

- Bạn hiểu khác biệt giữa rolling update mặc định và progressive delivery.
- Có thể mô phỏng rollout từng phần.

### Lưu ý

- Không nên nhảy vào canary nếu chưa có monitoring.
- Database change phải backward compatible.
- Feature flag thường đi cùng canary rất tốt.

### Use case thực tế

- Release tính năng rủi ro cao.
- Production traffic lớn.
- Muốn giảm blast radius khi deploy.

---

# Phần D: Cloud cơ bản

## Lab 16. Tạo managed Kubernetes cluster

### Mục tiêu

- Chạy Kubernetes trên cloud.
- Hiểu khác biệt giữa local cluster và managed cluster.
- Kết nối `kubectl` tới cluster cloud.

### Có thể chọn một cloud

AWS:

- EKS.
- ECR.
- IAM.
- VPC.
- ALB/NLB.

GCP:

- GKE.
- Artifact Registry.
- IAM.
- VPC.
- Cloud Load Balancing.

Azure:

- AKS.
- ACR.
- Entra ID/IAM.
- VNet.
- Azure Load Balancer/Application Gateway.

### Việc cần làm

1. Tạo cluster nhỏ.
2. Cấu hình kubeconfig.
3. Kiểm tra node.
4. Tạo namespace.
5. Deploy nginx thử.

Ví dụ thao tác chung:

```bash
kubectl get nodes
kubectl create namespace cloud-demo
kubectl create deployment nginx --image=nginx -n cloud-demo
kubectl expose deployment nginx --port=80 --type=LoadBalancer -n cloud-demo
kubectl get svc -n cloud-demo
```

### Kết quả mong đợi

- Có cluster cloud.
- Service type LoadBalancer tạo được external endpoint.

### Lưu ý

- Managed Kubernetes vẫn cần bạn quản node pool, network, IAM, upgrade.
- LoadBalancer public có thể phát sinh chi phí.
- Xóa service/cluster sau lab nếu không dùng.

### Ưu điểm

- Gần production hơn local.
- Có cloud load balancer, IAM, storage thật.

### Nhược điểm

- Tốn tiền.
- Cấu hình cloud ban đầu phức tạp hơn.
- Sai IAM/network là lỗi rất thường gặp.

### Use case thực tế

- Công ty chạy production Kubernetes thường dùng managed Kubernetes để giảm gánh nặng quản control plane.

---

## Lab 17. Dùng managed container registry

### Mục tiêu

- Push image vào registry của cloud.
- Kubernetes pull image từ registry cùng cloud.
- Jenkins đăng nhập registry bằng credential phù hợp.

### AWS ECR

Luồng:

```text
Jenkins
  -> aws ecr get-login-password
  -> docker login ECR
  -> docker build
  -> docker push
EKS
  -> pull image từ ECR qua IAM/node role
```

### GCP Artifact Registry

Luồng:

```text
Jenkins
  -> gcloud auth configure-docker
  -> docker push Artifact Registry
GKE
  -> pull image bằng service account/IAM
```

### Azure ACR

Luồng:

```text
Jenkins
  -> az acr login
  -> docker push ACR
AKS
  -> attach ACR hoặc dùng imagePullSecret
```

### Việc cần làm

1. Tạo registry/repository.
2. Cấu hình Jenkins credential cloud.
3. Build image.
4. Push image.
5. Update GitOps repo dùng image cloud registry.
6. Argo CD deploy lên cluster cloud.

### Kết quả mong đợi

- Image nằm trong managed registry.
- Cluster cloud pull image được.

### Lưu ý

- Registry private cần IAM hoặc imagePullSecret.
- Nên bật vulnerability scan nếu cloud hỗ trợ.
- Nên dùng lifecycle policy để xóa image cũ.

### Ưu điểm

- Tích hợp tốt với cloud IAM.
- Ít phải vận hành registry.
- Có audit, scan, replication tùy cloud.

### Nhược điểm

- Phụ thuộc cloud provider.
- Có chi phí storage và transfer.

### Use case thực tế

- Production cloud gần như luôn dùng managed registry hoặc registry enterprise.

---

## Lab 18. Deploy bằng Argo CD lên cloud cluster

### Mục tiêu

- Cài Argo CD trên cloud Kubernetes.
- Kết nối GitOps repo.
- Deploy app thật lên cloud.

### Cách triển khai Argo CD

Cách 1: Argo CD nằm trong cùng cluster app.

Ưu điểm:

- Dễ bắt đầu.
- Ít cluster.

Nhược điểm:

- Nếu cluster lỗi, Argo CD cũng lỗi.
- Khó quản lý nhiều cluster lớn.

Cách 2: Argo CD nằm trong management cluster.

Ưu điểm:

- Quản lý nhiều cluster tốt.
- Phù hợp platform team.

Nhược điểm:

- Phức tạp hơn.
- Cần quản lý cluster credential.

### Việc cần làm

1. Cài Argo CD trên cloud cluster.
2. Cấu hình repo credential nếu repo private.
3. Tạo Application cho app.
4. Sync.
5. Kiểm tra pod, service, ingress.

### Kết quả mong đợi

- Argo CD sync app lên cloud cluster.
- App healthy.

### Lưu ý

- Argo CD cần quyền vừa đủ trong cluster.
- Production nên cấu hình SSO.
- Nên bật audit log nếu có.

### Ưu điểm

- Cloud deployment quản lý bằng Git.
- Dễ mở rộng sang nhiều service.

### Nhược điểm

- Cần bảo vệ Argo CD cẩn thận.
- Nếu GitOps repo bị sửa sai, Argo CD có thể sync sai rất nhanh.

### Use case thực tế

- Platform team quản lý deployment toàn bộ microservices qua Argo CD.

---

## Lab 19. Expose ứng dụng qua cloud load balancer và ingress

### Mục tiêu

- Đưa app ra ngoài cluster.
- Hiểu Service type LoadBalancer và Ingress.
- Gắn domain và TLS cơ bản.

### Cách 1: Service type LoadBalancer

```yaml
apiVersion: v1
kind: Service
metadata:
  name: demo-app
  namespace: demo
spec:
  type: LoadBalancer
  selector:
    app: demo-app
  ports:
    - port: 80
      targetPort: 8080
```

Ưu điểm:

- Dễ.
- Phù hợp lab.

Nhược điểm:

- Mỗi service có thể tạo một load balancer riêng, tốn tiền.
- Khó routing nhiều domain/path.

### Cách 2: Ingress

Thường dùng:

- NGINX Ingress Controller.
- AWS Load Balancer Controller.
- GKE Ingress.
- Azure Application Gateway Ingress Controller.

Ingress ví dụ:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: demo-app
  namespace: demo
spec:
  rules:
    - host: demo.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: demo-app
                port:
                  number: 80
```

### Việc cần làm

1. Cài ingress controller.
2. Tạo Ingress cho app.
3. Trỏ DNS vào load balancer.
4. Cài cert-manager nếu muốn TLS tự động.
5. Kiểm tra HTTPS.

### Kết quả mong đợi

- App truy cập được qua domain.
- Có thể bật TLS.

### Lưu ý

- DNS propagation có thể mất thời gian.
- Certificate Let's Encrypt có rate limit.
- Security group/firewall phải mở port phù hợp.

### Ưu điểm

- Gần production.
- Quản lý nhiều app qua một ingress layer.

### Nhược điểm

- Network cloud dễ cấu hình sai.
- Debug cần xem cả Kubernetes và cloud load balancer.

### Use case thực tế

- Public API, web app, internal dashboard.

---

# Phần E: Cloud nâng cao

## Lab 20. Terraform tạo hạ tầng CI/CD

### Mục tiêu

- Infrastructure as Code.
- Tạo lại hạ tầng cloud có kiểm soát.
- Giảm thao tác click tay.

### Thành phần nên tạo bằng Terraform

- VPC/VNet.
- Subnet.
- Security group/firewall.
- Kubernetes cluster.
- Node pool.
- Container registry.
- IAM role/service account.
- DNS record nếu phù hợp.
- Object storage cho log/artifact nếu cần.

### Việc cần làm

1. Tạo module network.
2. Tạo module Kubernetes.
3. Tạo module registry.
4. Output kubeconfig hoặc cluster info.
5. Cài Argo CD bằng Helm hoặc app bootstrap.

Luồng:

```text
terraform init
terraform plan
terraform apply
  -> tạo cloud infrastructure
  -> Jenkins build image
  -> Argo CD deploy app
```

### Kết quả mong đợi

- Hạ tầng tạo được bằng code.
- Có thể destroy sau lab.

### Lưu ý

- Terraform state rất quan trọng, không được làm mất hoặc commit state chứa secret.
- Production nên dùng remote backend.
- Plan phải được review trước khi apply.

### Ưu điểm

- Lặp lại được.
- Review được thay đổi hạ tầng.
- Phù hợp team.

### Nhược điểm

- Cần học thêm Terraform và cloud provider.
- Sai IaC có thể tạo/xóa tài nguyên lớn.

### Use case thực tế

- Tạo cluster mới cho môi trường staging/prod.
- Chuẩn hóa hạ tầng nhiều project.

---

## Lab 21. External Secrets với cloud secret manager

### Mục tiêu

- Secret lưu trong cloud secret manager.
- Kubernetes lấy secret thông qua External Secrets Operator.
- GitOps repo không chứa secret value.

### Luồng

```text
Cloud Secret Manager
  -> External Secrets Operator
  -> Kubernetes Secret
  -> Pod environment variable / mounted secret
```

### Việc cần làm

1. Cài External Secrets Operator.
2. Tạo secret trong cloud secret manager.
3. Tạo SecretStore hoặc ClusterSecretStore.
4. Tạo ExternalSecret manifest trong GitOps repo.
5. App đọc secret từ Kubernetes Secret.

Manifest minh họa:

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: demo-app-secret
  namespace: demo
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: cloud-secret-store
    kind: ClusterSecretStore
  target:
    name: demo-app-secret
  data:
    - secretKey: DB_PASSWORD
      remoteRef:
        key: demo-app/db-password
```

### Kết quả mong đợi

- Secret value không nằm trong Git.
- Kubernetes Secret được tạo tự động.
- App chạy được với secret từ cloud.

### Lưu ý

- IAM của External Secrets phải tối thiểu.
- Cần strategy rotation secret.
- Nếu cloud secret manager lỗi, secret refresh có thể bị ảnh hưởng.

### Ưu điểm

- Bảo mật tốt hơn plain Kubernetes Secret trong Git.
- Tích hợp cloud IAM và audit.

### Nhược điểm

- Phụ thuộc cloud.
- Debug phức tạp hơn.

### Use case thực tế

- Production database password.
- OAuth client secret.
- Payment provider API key.

---

## Lab 22. Multi-environment và multi-cluster

### Mục tiêu

- Quản lý nhiều môi trường chuyên nghiệp hơn.
- Một Argo CD quản lý nhiều cluster hoặc nhiều Argo CD theo từng cluster.
- Tách quyền theo team/environment.

### Mô hình 1: Một cluster, nhiều namespace

```text
cluster-a
  namespace demo-dev
  namespace demo-staging
  namespace demo-prod
```

Ưu điểm:

- Rẻ.
- Dễ học.

Nhược điểm:

- Cách ly kém hơn.
- Production chung cluster với dev có thể rủi ro.

### Mô hình 2: Mỗi môi trường một cluster

```text
dev-cluster
staging-cluster
prod-cluster
```

Ưu điểm:

- Cách ly tốt.
- Production an toàn hơn.

Nhược điểm:

- Tốn chi phí.
- Quản lý phức tạp hơn.

### Mô hình 3: Management cluster chạy Argo CD

```text
management-cluster
  -> Argo CD
      -> dev-cluster
      -> staging-cluster
      -> prod-cluster
```

Ưu điểm:

- Quản lý tập trung.
- Phù hợp platform team.

Nhược điểm:

- Cần bảo mật cluster credential rất kỹ.

### Việc cần làm

1. Tạo ít nhất hai environment.
2. Tạo Argo CD Project riêng.
3. Giới hạn namespace/cluster destination.
4. Tạo ApplicationSet nếu muốn tự sinh app theo environment.
5. Thử promote image tag từ dev sang staging.

### Kết quả mong đợi

- Dev và staging tách biệt.
- Argo CD không được deploy ngoài phạm vi cho phép.

### Lưu ý

- RBAC rất quan trọng.
- Không nên dùng cùng secret cho mọi environment.
- Production nên có approval rõ ràng.

### Use case thực tế

- Công ty có nhiều team, nhiều service, nhiều cluster.
- Platform team cấp template deployment chuẩn cho các team app.

---

## Lab 23. Progressive delivery trên cloud

### Mục tiêu

- Canary/Blue-Green với traffic thật.
- Dựa vào metric để quyết định rollout.
- Giảm rủi ro khi release production.

### Cần chuẩn bị

- Argo Rollouts hoặc Flagger.
- Ingress controller hỗ trợ traffic splitting, hoặc service mesh.
- Prometheus metric.

### Luồng canary nâng cao

```text
Deploy version mới
  -> gửi 5% traffic
  -> kiểm tra error rate, latency
  -> nếu ổn tăng 20%
  -> nếu ổn tăng 50%
  -> nếu ổn promote full
  -> nếu lỗi rollback
```

### Việc cần làm

1. Cài Argo Rollouts.
2. Cấu hình Rollout resource.
3. Cấu hình AnalysisTemplate đọc metric Prometheus.
4. Deploy version tốt.
5. Deploy version lỗi.
6. Quan sát auto rollback hoặc pause.

### Kết quả mong đợi

- Rollout không chỉ dựa vào pod ready mà còn dựa vào metric.
- Version lỗi không nhận toàn bộ traffic.

### Lưu ý

- Metric phải đáng tin.
- Cần traffic đủ để đánh giá.
- Canary không thay thế test, chỉ giảm rủi ro.

### Ưu điểm

- Release an toàn hơn.
- Phù hợp hệ thống có traffic lớn.

### Nhược điểm

- Phức tạp.
- Cần observability tốt.
- Debug khó hơn rolling update.

### Use case thực tế

- Fintech, e-commerce, SaaS nhiều người dùng.
- Release chức năng ảnh hưởng conversion hoặc thanh toán.

---

## Lab 24. Disaster recovery và backup GitOps

### Mục tiêu

- Biết khôi phục khi cluster hoặc Argo CD lỗi.
- Hiểu GitOps giúp rebuild environment như thế nào.
- Backup những thứ không nằm trong Git.

### Cần backup gì?

- Git repository.
- Container registry image.
- Argo CD config quan trọng.
- Kubernetes persistent data.
- Secret trong secret manager.
- Terraform state.
- Jenkins home hoặc Jenkins Configuration as Code.
- Jenkins credential nếu self-host.

### Thực hành

1. Export danh sách Argo CD Application.
2. Xóa một app khỏi cluster.
3. Apply lại Application từ Git.
4. Xóa namespace app.
5. Để Argo CD sync lại.
6. Mô phỏng mất Jenkins và restore từ backup hoặc CasC.
7. Mô phỏng mất cluster local và dựng lại từ GitOps repo.

### Kết quả mong đợi

- App có thể được tái tạo từ GitOps repo.
- Bạn biết phần nào không thể khôi phục chỉ bằng Git.

### Lưu ý

- GitOps không tự backup database.
- Persistent volume cần backup riêng.
- Registry image cũ cần retention hợp lý.
- Terraform state mất là sự cố nghiêm trọng.

### Ưu điểm

- Tăng khả năng phục hồi.
- Giảm phụ thuộc thao tác tay.

### Nhược điểm

- Backup/restore cần được test định kỳ.
- Nhiều hệ thống phải phối hợp với nhau.

### Use case thực tế

- Cluster bị lỗi.
- Người vận hành xóa nhầm resource.
- Cần dựng lại môi trường staging giống production.

---

## 7. Bảng so sánh self-host và cloud

| Tiêu chí | Self-host | Cloud |
|---|---|---|
| Chi phí ban đầu | Thấp nếu dùng máy có sẵn | Có thể phát sinh tiền ngay |
| Độ giống production hiện đại | Trung bình | Cao |
| Kiểm soát hạ tầng | Rất cao | Phụ thuộc provider |
| Độ khó network | Vừa | Cao hơn vì IAM/VPC/LB |
| Vận hành Kubernetes | Tự lo nhiều hơn | Provider lo control plane |
| Registry | Tự host hoặc dùng public | Managed registry |
| Secret | Tự chọn giải pháp | Có secret manager |
| Phù hợp học nền tảng | Rất tốt | Tốt sau khi có nền |
| Phù hợp production | Có, nếu team đủ năng lực vận hành | Rất phổ biến |

Khuyến nghị học:

```text
Self-host local trước
  -> hiểu bản chất
  -> chuyển cloud
  -> học IAM/network/managed service
  -> quay lại tối ưu production pattern
```

---

## 8. Ưu nhược điểm của Jenkins + Argo CD

## 8.1. Ưu điểm

- Jenkins cực kỳ linh hoạt cho CI.
- Argo CD rất mạnh cho Kubernetes GitOps.
- Tách rõ build và deploy.
- Audit tốt vì deployment state nằm trong Git.
- Rollback dễ hiểu hơn khi dùng Git history.
- Không cần Jenkins giữ kubeconfig production nếu chọn GitOps đúng nghĩa.
- Phù hợp enterprise có hệ thống cũ và mới cùng tồn tại.

## 8.2. Nhược điểm

- Vận hành hai hệ thống riêng.
- Jenkins plugin có thể gây rủi ro bảo mật/vận hành nếu không quản lý tốt.
- Jenkinsfile có thể trở nên phức tạp.
- GitOps repo có thể bị commit noise nếu mỗi build đều update.
- Argo CD yêu cầu team hiểu Kubernetes tương đối tốt.
- Secret management cần thiết kế riêng, không tự nhiên giải quyết hết.
- Multi-cluster và multi-tenant cần RBAC cẩn thận.

## 8.3. Khi nào nên dùng Jenkins + Argo CD?

Nên dùng khi:

- Ứng dụng chạy trên Kubernetes.
- Team cần GitOps.
- CI phức tạp, nhiều bước build/test/scan/custom script.
- Doanh nghiệp đã có Jenkins.
- Muốn tách CI khỏi CD.
- Muốn audit deployment bằng Git.

Không nhất thiết dùng khi:

- Dự án rất nhỏ, deploy đơn giản lên một VM.
- Team chưa dùng Kubernetes.
- Pipeline rất đơn giản và GitHub Actions/GitLab CI đã đủ.
- Không có người vận hành Jenkins/Argo CD.

---

## 9. Use case thực tế theo cấp độ

## 9.1. Startup nhỏ

Mô hình:

```text
GitHub
  -> Jenkins hoặc GitHub Actions build image
  -> Registry
  -> Argo CD deploy dev/prod lên một Kubernetes cluster nhỏ
```

Thường dùng:

- Một cloud provider.
- Một cluster.
- Namespace tách dev/prod.
- Auto-sync dev.
- Manual sync prod.

Điểm cần chú ý:

- Đừng over-engineer.
- Monitoring và backup tối thiểu phải có.
- Secret không được để plain text.

## 9.2. Công ty enterprise có Jenkins sẵn

Mô hình:

```text
Legacy Jenkins
  -> build nhiều loại app
  -> publish artifact/image
  -> update GitOps repo
Argo CD
  -> deploy Kubernetes workloads
```

Thường dùng:

- Jenkins shared library.
- Jenkins agent pool.
- SonarQube.
- Nexus/Artifactory/Harbor.
- Argo CD Projects.
- SSO/RBAC.

Điểm cần chú ý:

- Chuẩn hóa pipeline template.
- Quản plugin Jenkins.
- Audit credential.
- Tách quyền production.

## 9.3. Microservices nhiều team

Mô hình:

```text
Mỗi service có source repo riêng
GitOps repo chứa cấu hình môi trường
ApplicationSet sinh Argo CD app
Jenkins shared library chuẩn hóa CI
```

Thường dùng:

- Kustomize hoặc Helm.
- ApplicationSet.
- External Secrets.
- Progressive delivery.
- Central monitoring/logging.

Điểm cần chú ý:

- Convention rất quan trọng.
- Nếu mỗi team làm một kiểu, hệ thống sẽ khó vận hành.
- Cần platform guideline rõ.

## 9.4. Hệ thống yêu cầu kiểm soát release nghiêm ngặt

Mô hình:

```text
CI build artifact bất biến
Security scan
Approval
Promote image tag qua environment
Argo CD manual sync production
Audit log
Rollback plan
```

Thường dùng trong:

- Ngân hàng.
- Bảo hiểm.
- Y tế.
- Chính phủ.
- Hệ thống thanh toán.

Điểm cần chú ý:

- Không deploy production tự động nếu quy trình không cho phép.
- Cần approval, audit, change record.
- Artifact phải immutable.

---

## 10. Checklist thực hành theo tuần

## Tuần 1: Docker và Jenkins cơ bản

- Chạy app local.
- Viết Dockerfile.
- Chạy Jenkins local.
- Viết Jenkinsfile build/test.
- Tạo pipeline từ Git.

## Tuần 2: Registry và Kubernetes local

- Jenkins build Docker image.
- Push image lên registry.
- Tạo Kubernetes local cluster.
- Viết manifest Deployment/Service.
- Deploy app bằng `kubectl`.

## Tuần 3: Argo CD và GitOps

- Cài Argo CD.
- Tạo GitOps repo.
- Deploy app bằng Argo CD.
- Jenkins update GitOps repo.
- Argo CD auto-sync dev.

## Tuần 4: Multi-environment

- Tách dev/staging/prod.
- Dùng Kustomize hoặc Helm.
- Manual promote version.
- Rollback bằng Git.
- Thêm health check.

## Tuần 5: Security và quality

- Thêm SonarQube hoặc static analysis.
- Thêm Trivy scan.
- Quản Jenkins credential.
- Thực hành secret management.
- Chặn deploy nếu scan fail.

## Tuần 6: Cloud cơ bản

- Tạo managed Kubernetes.
- Tạo managed registry.
- Cài Argo CD trên cloud.
- Jenkins push image cloud registry.
- Deploy app lên cloud.

## Tuần 7: Cloud nâng cao

- Tạo hạ tầng bằng Terraform.
- Dùng External Secrets.
- Tạo ingress/domain/TLS.
- Monitoring với Prometheus/Grafana.

## Tuần 8: Production pattern

- Jenkins agent trên Kubernetes.
- Multi-cluster hoặc multi-namespace.
- Canary/Blue-Green.
- Backup/restore.
- Viết runbook xử lý lỗi.

---

## 11. Các lỗi thường gặp và cách nghĩ khi debug

## 11.1. Jenkins không clone được repo

Nguyên nhân thường gặp:

- Sai credential.
- Repo private nhưng chưa cấp quyền.
- SSH known_hosts lỗi.
- Branch không tồn tại.
- Jenkins agent không ra được internet.

Cách debug:

```bash
git ls-remote <repo-url>
```

Kiểm tra:

- Credential ID trong Jenkinsfile.
- URL repo.
- Network/DNS.

## 11.2. Jenkins build Docker image lỗi

Nguyên nhân thường gặp:

- Jenkins agent không có Docker CLI.
- Không truy cập được Docker daemon.
- Dockerfile copy sai path.
- Build context quá lớn.
- Test fail trước khi build image.

Cách debug:

```bash
docker version
docker info
docker build -t test .
```

## 11.3. Push image lỗi

Nguyên nhân thường gặp:

- Sai registry credential.
- Sai image name.
- Repository chưa tồn tại.
- Token hết hạn.
- Registry chặn quyền push.

Cách debug:

```bash
docker login
docker push <image>
```

## 11.4. Argo CD không sync

Nguyên nhân thường gặp:

- Sai repo URL.
- Argo CD không có quyền đọc repo.
- Path sai.
- YAML lỗi.
- Destination namespace không tồn tại.
- Resource bị policy chặn.

Cách debug:

```bash
kubectl get applications -n argocd
kubectl describe application <app-name> -n argocd
```

Trong UI:

- Xem tab Events.
- Xem diff.
- Xem resource nào fail.

## 11.5. Pod ImagePullBackOff

Nguyên nhân thường gặp:

- Image tag không tồn tại.
- Registry private chưa có imagePullSecret.
- Node không có quyền pull registry cloud.
- Sai registry URL.

Cách debug:

```bash
kubectl describe pod <pod-name> -n <namespace>
kubectl get events -n <namespace> --sort-by=.lastTimestamp
```

## 11.6. App deploy thành công nhưng không truy cập được

Nguyên nhân thường gặp:

- Service selector sai label.
- Container port sai.
- Readiness probe fail.
- Ingress host sai.
- DNS chưa trỏ đúng.
- Security group/firewall chưa mở.

Cách debug:

```bash
kubectl get pod,svc,ingress -n <namespace>
kubectl describe svc <service> -n <namespace>
kubectl logs deploy/<deployment> -n <namespace>
kubectl port-forward svc/<service> 8080:80 -n <namespace>
```

---

## 12. Best practices nên áp dụng

## 12.1. Với Jenkins

- Dùng Jenkinsfile lưu trong Git.
- Không cấu hình job thủ công quá nhiều nếu có thể tránh.
- Dùng credential store.
- Không hard-code secret.
- Tách Controller và Agent.
- Dùng shared library cho pipeline lặp lại.
- Pin plugin version nếu môi trường quan trọng.
- Backup Jenkins home hoặc dùng Jenkins Configuration as Code.
- Dọn workspace và artifact cũ.
- Giới hạn quyền admin.

## 12.2. Với Docker image

- Dùng tag bất biến: commit SHA, version, build number.
- Không chỉ dùng `latest`.
- Dùng multi-stage build.
- Không chạy container bằng root nếu có thể.
- Scan image.
- Không copy file nhạy cảm vào image.
- Dùng `.dockerignore`.
- Tối ưu layer cache.

## 12.3. Với Argo CD

- Dùng GitOps repo rõ cấu trúc.
- Tách environment.
- Dùng Project để giới hạn quyền.
- Production nên manual sync hoặc có approval nếu team chưa đủ tự động hóa.
- Bật auto-sync cho dev nếu phù hợp.
- Cẩn thận với `prune`.
- Cẩn thận với `selfHeal` nếu có người vẫn sửa tay trong cluster.
- Dùng SSO/RBAC.
- Theo dõi drift.

## 12.4. Với Kubernetes

- Luôn cấu hình readiness/liveness probe.
- Đặt resource requests/limits.
- Dùng namespace tách môi trường/team.
- Không dùng default namespace cho app thật.
- Dùng ServiceAccount riêng.
- Giới hạn RBAC.
- Dùng NetworkPolicy nếu cần.
- Có strategy cho database migration.

## 12.5. Với cloud

- Dùng IAM least privilege.
- Không dùng access key cá nhân lâu dài nếu có thể dùng workload identity/role.
- Dùng managed registry.
- Bật audit log.
- Đặt budget alert.
- Dùng Terraform cho hạ tầng.
- Xóa tài nguyên sau lab.
- Không mở Kubernetes API public quá rộng.

---

## 13. Bài tập tổng hợp cuối lộ trình

## Đề bài

Xây dựng hệ thống CI/CD hoàn chỉnh cho một ứng dụng web API:

```text
Developer push code
  -> Jenkins chạy test
  -> Jenkins scan code
  -> Jenkins build Docker image
  -> Jenkins scan image
  -> Jenkins push image lên registry
  -> Jenkins update GitOps repo dev
  -> Argo CD auto-sync dev
  -> Promote sang staging bằng pull request
  -> Argo CD sync staging
  -> Promote sang prod bằng approval
  -> Argo CD sync prod
  -> Monitoring xác nhận app healthy
```

## Yêu cầu tối thiểu

- Có source repo.
- Có GitOps repo.
- Có Jenkinsfile.
- Có Dockerfile.
- Có Kubernetes manifest bằng Helm hoặc Kustomize.
- Có Argo CD Application cho dev/staging/prod.
- Có image tag theo commit SHA.
- Có rollback bằng Git.
- Có secret management không lưu plain text secret trong Git.
- Có health check.
- Có README hướng dẫn chạy.

## Yêu cầu nâng cao

- Jenkins agent chạy trên Kubernetes.
- Quality gate với SonarQube.
- Image scan với Trivy.
- External Secrets.
- Ingress + TLS.
- Prometheus/Grafana dashboard.
- Canary bằng Argo Rollouts.
- Terraform tạo cloud infrastructure.
- ApplicationSet quản lý nhiều app hoặc nhiều cluster.

## Tiêu chí tự đánh giá

Bạn nên trả lời được các câu hỏi:

- Commit nào tạo ra image đang chạy?
- Image nào đang chạy ở dev/staging/prod?
- Ai promote version lên production?
- Nếu version mới lỗi, rollback thế nào?
- Secret nằm ở đâu?
- Jenkins có cần quyền production cluster không?
- Argo CD có quyền deploy namespace nào?
- App có readiness/liveness probe chưa?
- Nếu registry mất image cũ thì rollback có được không?
- Nếu cluster mất, dựng lại bằng gì?

---

## 14. Kết luận

Lộ trình thực hành tốt nhất là đi theo thứ tự:

```text
Local build
  -> Docker image
  -> Jenkins CI
  -> Registry
  -> Kubernetes local
  -> Argo CD GitOps
  -> Multi-environment
  -> Security/quality
  -> Cloud Kubernetes
  -> Terraform/External Secrets
  -> Progressive delivery
  -> Backup/restore
```

Điều quan trọng nhất không phải là cài thật nhiều tool, mà là hiểu trách nhiệm của từng phần:

- **Git** lưu source of truth.
- **Jenkins** tạo artifact đáng tin.
- **Registry** lưu artifact bất biến.
- **GitOps repo** mô tả desired state.
- **Argo CD** đưa cluster về desired state.
- **Kubernetes** chạy workload.
- **Monitoring** xác nhận hệ thống thật sự khỏe.

Khi đã hiểu luồng này, bạn có thể thay Jenkins bằng GitHub Actions/GitLab CI, thay Argo CD bằng Flux, thay registry này bằng registry khác, hoặc chuyển từ self-host sang cloud mà không mất bản chất.
