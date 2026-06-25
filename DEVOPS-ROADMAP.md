# Lộ trình học DevOps chi tiết

## 1. Mục tiêu

Sau khi hoàn thành lộ trình này, bạn có thể:

- Quản trị máy chủ Linux cơ bản.
- Hiểu mạng máy tính, DNS, HTTP/HTTPS và cách ứng dụng giao tiếp.
- Đóng gói ứng dụng bằng Docker.
- Xây dựng quy trình CI/CD tự động.
- Tạo hạ tầng bằng Terraform.
- Triển khai và vận hành ứng dụng trên Kubernetes.
- Giám sát hệ thống bằng Prometheus, Grafana và hệ thống logging tập trung.
- Áp dụng các nguyên tắc bảo mật và GitOps.

Lộ trình dự kiến kéo dài từ **6 đến 9 tháng**, phù hợp với người đang đi học hoặc đi làm và có thể dành khoảng **10–12 giờ mỗi tuần**.

---

## 2. Tổng quan lộ trình

```text
Linux và Networking
        ↓
Git và Bash
        ↓
Docker
        ↓
CI/CD
        ↓
Cloud và Terraform
        ↓
Kubernetes
        ↓
Monitoring và Logging
        ↓
DevSecOps và GitOps
```

---

## 3. Giai đoạn 1: Linux, Networking và Git

**Thời gian:** 4–6 tuần

### 3.1. Linux

#### Kiến thức cần học

- Cấu trúc thư mục Linux:
  - `/etc`: file cấu hình hệ thống.
  - `/var`: log và dữ liệu thường xuyên thay đổi.
  - `/home`: thư mục người dùng.
  - `/opt`: phần mềm cài đặt thêm.
  - `/proc`: thông tin process và kernel.
- Quản lý file và thư mục.
- User, group và phân quyền.
- Process và service.
- Cài đặt package.
- SSH và SCP.
- Biến môi trường.
- Cron job.
- Quản lý và phân tích log.
- Bash scripting cơ bản.

#### Các lệnh quan trọng

```bash
ls
cd
pwd
cp
mv
rm
mkdir
find
grep
sed
awk
chmod
chown
ps
top
kill
systemctl
journalctl
ssh
scp
curl
```

#### Bài tập thực hành

- Cài Ubuntu bằng WSL2, VirtualBox hoặc máy ảo cloud.
- Tạo user và cấu hình quyền truy cập.
- Cài Java, Nginx và PostgreSQL.
- Chạy một ứng dụng Spring Boot bằng `systemd`.
- Cấu hình Nginx làm reverse proxy.
- Viết script sao lưu database.
- Tạo cron job chạy script sao lưu hằng ngày.
- Kiểm tra log khi ứng dụng không khởi động được.

### 3.2. Networking

#### Kiến thức cần học

- Mô hình TCP/IP.
- Địa chỉ IP public và private.
- Port và socket.
- DNS.
- HTTP và HTTPS.
- TLS/SSL certificate.
- CIDR và subnet.
- Gateway và NAT.
- Firewall.
- Reverse proxy.
- Load balancer.

#### Các lệnh thực hành

```bash
ip addr
ping
curl
nslookup
dig
traceroute
ss -tulpn
netstat
telnet
nc
```

### 3.3. Git

#### Kiến thức cần học

- Repository, commit và history.
- Branch.
- Merge và rebase.
- Pull request.
- Code review.
- Tag và release.
- Giải quyết conflict.
- Feature branch workflow.
- Trunk-based development.

#### Kết quả cần đạt

Triển khai thủ công một ứng dụng Spring Boot lên Linux, chạy ứng dụng bằng `systemd`, sử dụng Nginx làm reverse proxy và truy cập ứng dụng qua HTTPS.

---

## 4. Giai đoạn 2: Docker

**Thời gian:** 4 tuần

### 4.1. Kiến thức cần học

- Sự khác nhau giữa container và máy ảo.
- Docker image và container.
- Docker registry.
- Dockerfile.
- Image layer và build cache.
- Volume và bind mount.
- Container network.
- Port mapping.
- Docker Compose.
- Multi-stage build.
- Health check.
- Resource limit.
- Chạy container bằng non-root user.

### 4.2. Các lệnh quan trọng

```bash
docker build
docker run
docker ps
docker logs
docker exec
docker inspect
docker images
docker pull
docker push
docker network
docker volume
docker compose up
docker compose down
```

### 4.3. Dự án thực hành

Đóng gói một hệ thống gồm:

- Spring Boot.
- PostgreSQL.
- Redis.
- Nginx.

Cấu trúc dự án tham khảo:

```text
project/
├── app/
├── Dockerfile
├── compose.yaml
├── nginx/
└── scripts/
```

### 4.4. Yêu cầu hoàn thành

- Sử dụng multi-stage Dockerfile.
- Container ứng dụng không chạy bằng tài khoản root.
- PostgreSQL sử dụng persistent volume.
- Cấu hình ứng dụng được truyền bằng environment variable.
- Các container có health check.
- Các container giao tiếp qua Docker network.
- Image được đẩy lên Docker Hub hoặc GitHub Container Registry.

---

## 5. Giai đoạn 3: CI/CD

**Thời gian:** 3–4 tuần

### 5.1. Lựa chọn công cụ

Nên chọn một công cụ chính để học:

- **GitHub Actions:** phù hợp với dự án cá nhân và GitHub.
- **GitLab CI:** phổ biến trong nhiều doanh nghiệp.
- **Jenkins:** thường xuất hiện trong các hệ thống lâu năm.

Người mới nên bắt đầu với **GitHub Actions**.

### 5.2. Kiến thức cần học

- Cấu trúc workflow YAML.
- Job, step và runner.
- Trigger theo branch, tag và pull request.
- Dependency cache.
- Artifact.
- Secret management.
- Các môi trường development, staging và production.
- Manual approval.
- Rollback.
- Semantic Versioning.

### 5.3. Pipeline mẫu

```text
Push code
    ↓
Compile
    ↓
Unit test
    ↓
Static analysis
    ↓
Build Docker image
    ↓
Security scan
    ↓
Push image lên registry
    ↓
Deploy
    ↓
Smoke test
```

### 5.4. Bài tập thực hành

- Khi tạo pull request:
  - Compile ứng dụng.
  - Chạy unit test.
  - Kiểm tra chất lượng code.
- Khi merge vào nhánh `main`:
  - Build Docker image.
  - Quét lỗ hổng image.
  - Đẩy image lên registry.
- Khi tạo Git tag:
  - Triển khai lên production.
- Gắn version image bằng Git tag hoặc commit SHA.
- Không sử dụng tag `latest` cho production.

---

## 6. Giai đoạn 4: Cloud và Infrastructure as Code

**Thời gian:** 5–6 tuần

### 6.1. Chọn nền tảng cloud

Ban đầu chỉ nên học sâu một nền tảng:

- **AWS:** phổ biến và có hệ sinh thái lớn.
- **Azure:** phù hợp với hệ sinh thái Microsoft.
- **Google Cloud:** mạnh về dữ liệu và Kubernetes.

Nếu chưa có định hướng cụ thể, có thể bắt đầu với AWS.

### 6.2. Kiến thức cloud cần học

- IAM user, role và policy.
- VPC.
- Public subnet và private subnet.
- Route table.
- Internet Gateway và NAT Gateway.
- Security Group hoặc firewall.
- Virtual Machine.
- Load Balancer.
- Auto Scaling.
- Object Storage.
- Managed Database.
- DNS.
- TLS certificate.
- Logging và monitoring.
- Quản lý chi phí cloud.

### 6.3. Terraform

#### Kiến thức cần học

- Provider.
- Resource.
- Data source.
- Variable.
- Output.
- Local value.
- State.
- Remote backend.
- Module.
- Workspace hoặc cấu trúc theo environment.
- Infrastructure drift.
- Import tài nguyên có sẵn.

#### Các lệnh quan trọng

```bash
terraform init
terraform fmt
terraform validate
terraform plan
terraform apply
terraform destroy
terraform output
terraform state
terraform import
```

### 6.4. Dự án thực hành

Dùng Terraform tạo hạ tầng:

```text
Internet
    ↓
Load Balancer
    ↓
Public/Private Subnet
    ↓
Application Server
    ↓
Managed PostgreSQL
```

Cấu trúc Terraform tham khảo:

```text
terraform/
├── modules/
│   ├── network/
│   ├── compute/
│   └── database/
└── environments/
    ├── dev/
    └── prod/
```

### 6.5. Lưu ý

- Không hard-code access key.
- Không commit secret vào Git.
- Bảo vệ Terraform state.
- Luôn chạy `terraform plan` trước `terraform apply`.
- Sử dụng module để tái sử dụng hạ tầng.

---

## 7. Giai đoạn 5: Kubernetes

**Thời gian:** 6–8 tuần

Chỉ nên bắt đầu Kubernetes sau khi đã hiểu Docker, Linux và networking.

### 7.1. Tuần 1–2: Kubernetes cơ bản

- Cluster.
- Control plane.
- Worker node.
- Pod.
- ReplicaSet.
- Deployment.
- Service.
- Namespace.
- Label và selector.
- Declarative YAML.
- Sử dụng `kubectl`.

Các loại Service:

- ClusterIP.
- NodePort.
- LoadBalancer.

### 7.2. Tuần 3–4: Cấu hình và lưu trữ

- ConfigMap.
- Secret.
- Resource request và limit.
- Liveness probe.
- Readiness probe.
- Startup probe.
- Rolling update.
- Rollback.
- Job và CronJob.
- PersistentVolume.
- PersistentVolumeClaim.
- StatefulSet.

### 7.3. Tuần 5–6: Networking và security

- Ingress hoặc Gateway API.
- Cluster DNS.
- NetworkPolicy.
- ServiceAccount.
- Role và ClusterRole.
- RoleBinding và ClusterRoleBinding.
- Security Context.
- Pod Security Standards.
- TLS.
- Image Pull Secret.

### 7.4. Tuần 7–8: Vận hành production

- Helm.
- Horizontal Pod Autoscaler.
- PodDisruptionBudget.
- Node affinity và anti-affinity.
- Taint và toleration.
- Cluster Autoscaler.
- Backup và khôi phục.
- Nâng cấp cluster.
- Debug pod, service, DNS và node.

### 7.5. Môi trường thực hành

- Bắt đầu bằng `kind` hoặc `minikube`.
- Sau đó thực hành với Kubernetes managed:
  - Amazon EKS.
  - Azure AKS.
  - Google GKE.

### 7.6. Kết quả cần đạt

- Viết manifest Kubernetes cho ứng dụng.
- Tạo Helm chart.
- Triển khai Spring Boot và PostgreSQL.
- Cấu hình resource limit và probe.
- Cấu hình Ingress và HTTPS.
- Thực hiện rolling update và rollback.
- Debug được các lỗi phổ biến như `CrashLoopBackOff`, `ImagePullBackOff` và lỗi DNS.

---

## 8. Giai đoạn 6: Monitoring, Logging và Tracing

**Thời gian:** 4 tuần

### 8.1. Metrics

Học các công cụ:

- Prometheus.
- Grafana.
- Node Exporter.
- Kubernetes Metrics Server.
- Spring Boot Actuator.
- Micrometer.

Các chỉ số quan trọng:

- CPU.
- RAM.
- Disk.
- Network.
- Request rate.
- Error rate.
- Response time.
- Database connection.
- JVM heap và garbage collection.

### 8.2. Logging

Chọn một hệ thống:

- Loki, Promtail và Grafana.
- Elasticsearch hoặc OpenSearch, Fluent Bit và Kibana.

Nội dung cần học:

- Log level.
- Structured logging.
- Correlation ID.
- Log aggregation.
- Log retention.
- Tìm kiếm và phân tích log.

### 8.3. Distributed Tracing

- OpenTelemetry.
- Jaeger hoặc Grafana Tempo.
- Trace ID và Span ID.
- Theo dõi request qua nhiều service.

### 8.4. Kiến thức vận hành

- Logs, metrics và traces.
- SLA, SLO và SLI.
- Alerting.
- Incident response.
- Runbook.
- Postmortem.
- Mean Time To Recovery.
- Capacity planning.

### 8.5. Bài tập thực hành

- Tạo dashboard theo dõi CPU, RAM và disk.
- Theo dõi request rate, error rate và latency.
- Gửi cảnh báo khi ứng dụng có tỷ lệ lỗi cao.
- Tìm một request lỗi bằng trace ID.
- Viết runbook xử lý pod bị crash.
- Viết runbook xử lý database hết connection.

---

## 9. Giai đoạn 7: DevSecOps và GitOps

**Thời gian:** 3–4 tuần

### 9.1. DevSecOps

Kiến thức cần học:

- Principle of Least Privilege.
- Quản lý secret bằng Vault hoặc cloud secret manager.
- Dependency scanning.
- Container image scanning.
- Static Application Security Testing.
- Secret scanning.
- Software Bill of Materials.
- Ký và xác minh container image.
- Cập nhật và vá dependency.
- Network segmentation.

Công cụ tham khảo:

- Trivy.
- SonarQube.
- OWASP Dependency-Check.
- HashiCorp Vault.
- Cosign.

### 9.2. GitOps

Chọn một công cụ:

- Argo CD.
- Flux.

Luồng GitOps:

```text
Developer
    ↓
Git Repository
    ↓
CI build và push image
    ↓
Cập nhật deployment repository
    ↓
Argo CD đồng bộ
    ↓
Kubernetes Cluster
```

Kiến thức cần học:

- Helm hoặc Kustomize.
- Automatic sync.
- Infrastructure drift.
- Rollback.
- Promotion từ development lên staging và production.
- Blue/green deployment.
- Canary deployment.

---

## 10. Dự án portfolio hoàn chỉnh

Xây dựng một hệ thống Spring Boot sử dụng PostgreSQL và Redis.

### 10.1. Yêu cầu kỹ thuật

- [ ] Source code được quản lý bằng Git.
- [ ] Có unit test và integration test.
- [ ] Có Dockerfile multi-stage.
- [ ] Container chạy bằng non-root user.
- [ ] Có Docker Compose để chạy local.
- [ ] CI tự động compile và chạy test.
- [ ] CI quét chất lượng code và lỗ hổng bảo mật.
- [ ] Build và push Docker image có version.
- [ ] Terraform tạo hạ tầng cloud.
- [ ] Helm chart triển khai ứng dụng lên Kubernetes.
- [ ] Argo CD triển khai theo GitOps.
- [ ] Prometheus thu thập metrics.
- [ ] Grafana hiển thị dashboard.
- [ ] Có hệ thống logging tập trung.
- [ ] Có distributed tracing.
- [ ] Có HTTPS.
- [ ] Có RBAC và secret management.
- [ ] Có chiến lược rollback.
- [ ] Có runbook xử lý sự cố.

### 10.2. Nội dung README của dự án

- Giới thiệu hệ thống.
- Sơ đồ kiến trúc.
- Công nghệ sử dụng.
- Lý do lựa chọn công nghệ.
- Cách chạy local.
- Cách chạy test.
- Cách triển khai.
- Quy trình CI/CD.
- Hình ảnh dashboard.
- Các sự cố đã giả lập.
- Cách xử lý và bài học rút ra.

---

## 11. Kế hoạch học theo tháng

| Tháng | Nội dung chính | Kết quả |
|---|---|---|
| 1 | Linux, Bash, Networking | Triển khai thủ công ứng dụng lên Linux |
| 2 | Git và Docker | Chạy hệ thống bằng Docker Compose |
| 3 | CI/CD | Tự động test, build và push image |
| 4 | Cloud và Terraform | Tạo hạ tầng cloud bằng code |
| 5 | Kubernetes cơ bản | Triển khai ứng dụng lên Kubernetes |
| 6 | Kubernetes nâng cao và Helm | Có quy trình deploy và rollback |
| 7 | Monitoring, Logging, Tracing | Có dashboard và cảnh báo |
| 8 | DevSecOps và GitOps | Triển khai an toàn bằng Argo CD |
| 9 | Hoàn thiện portfolio | Có dự án sẵn sàng để phỏng vấn |

---

## 12. Lịch học hằng tuần

Nếu có khoảng 10–12 giờ mỗi tuần:

- **30% thời gian:** đọc tài liệu và học lý thuyết.
- **50% thời gian:** làm lab và dự án.
- **20% thời gian:** debug, ghi chép và viết tài liệu.

Lịch tham khảo:

| Thời gian | Hoạt động |
|---|---|
| Thứ Hai | Học lý thuyết 60–90 phút |
| Thứ Ba | Thực hành lệnh và cấu hình 60–90 phút |
| Thứ Tư | Học lý thuyết 60–90 phút |
| Thứ Năm | Làm bài lab 60–90 phút |
| Thứ Sáu | Nghỉ hoặc ôn tập |
| Thứ Bảy | Làm dự án khoảng 4 giờ |
| Chủ Nhật | Tổng kết và cập nhật README |

Mỗi chủ đề đã học nên tạo ra ít nhất một sản phẩm cụ thể:

- Một script.
- Một Dockerfile.
- Một pipeline.
- Một Terraform module.
- Một Kubernetes manifest.
- Một Helm chart.
- Một dashboard.
- Một tài liệu xử lý sự cố.

---

## 13. Chứng chỉ có thể cân nhắc

Chứng chỉ không bắt buộc. Chỉ nên thi sau khi đã thực hành.

### Linux

- Linux Foundation Certified System Administrator.
- Red Hat Certified System Administrator.

### Cloud

- AWS Certified Solutions Architect – Associate.
- Microsoft Certified: Azure Administrator Associate.
- Google Associate Cloud Engineer.

### Kubernetes

- Kubernetes and Cloud Native Associate.
- Certified Kubernetes Application Developer.
- Certified Kubernetes Administrator.

### Terraform

- HashiCorp Certified: Terraform Associate.

---

## 14. Thứ tự ưu tiên

```text
Linux và Networking
        ↓
Docker
        ↓
CI/CD
        ↓
Cloud
        ↓
Terraform
        ↓
Kubernetes
        ↓
Monitoring
        ↓
Security và GitOps
```

Không nên học đồng thời quá nhiều công cụ có cùng chức năng. Ví dụ:

- Chỉ chọn một cloud để học sâu ban đầu.
- Chỉ chọn một công cụ CI/CD chính.
- Chỉ chọn một hệ thống logging.
- Chỉ chọn Argo CD hoặc Flux khi mới học GitOps.

Điều quan trọng nhất là hiểu nguyên lý, thực hành thường xuyên và hoàn thành một dự án có thể trình bày trong buổi phỏng vấn.

---

## 15. Tài liệu chính thức

- [Docker Get Started](https://docs.docker.com/get-started/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Terraform Tutorials](https://developer.hashicorp.com/terraform/tutorials)
- [Kubernetes Basics](https://kubernetes.io/docs/tutorials/kubernetes-basics/)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Argo CD Documentation](https://argo-cd.readthedocs.io/)

