# ADR 003: GitLab Self-Managed trên AWS

## Trạng thái

Đề xuất.

## Bối cảnh

Tài liệu thực hành chọn GitLab Self-Managed private trên AWS làm nơi lưu source code, Terraform và GitOps configuration. GitLab là trung tâm của quy trình CI/CD, nhưng không trực tiếp triển khai manifest vào Kubernetes production.

Cần chốt cách dựng GitLab ngay từ đầu vì GitLab ảnh hưởng đến repository, runner, quyền truy cập, backup, domain và chứng chỉ TLS.

## Quyết định

Dựng GitLab Self-Managed trong AWS account `shared-services`.

Triển khai ban đầu:

- Chạy GitLab trên EC2 trong private subnet hoặc subnet được kiểm soát truy cập chặt chẽ.
- Truy cập GitLab qua domain riêng, ví dụ `gitlab.example.com`.
- Sử dụng TLS cho toàn bộ truy cập web và Git over HTTPS.
- Lưu backup GitLab định kỳ vào Amazon S3.
- Tách GitLab Runner thành hạ tầng riêng trên EC2 Auto Scaling, không chạy runner tạm trên cùng máy GitLab.

Repository được tạo trong GitLab:

| Repository | Nội dung |
|---|---|
| `springboot-learning` | Source code ứng dụng Spring Boot |
| `platform-infrastructure` | Terraform module và root module theo môi trường |
| `platform-gitops` | Kubernetes manifest, Kustomize hoặc Helm values cho Argo CD |

## Phạm vi trách nhiệm

GitLab chịu trách nhiệm:

- Lưu source code, Terraform và GitOps configuration.
- Quản lý merge request, review và lịch sử thay đổi.
- Kích hoạt pipeline GitLab CI.

GitLab không chịu trách nhiệm:

- Không chạy `kubectl apply` trực tiếp vào production.
- Không giữ secret production dạng plain text trong repository.
- Không thay thế Argo CD trong việc đồng bộ manifest xuống EKS.

## Hệ quả

Ưu điểm:

- Chủ động kiểm soát source code và pipeline trong hạ tầng riêng.
- Phù hợp với bài thực hành doanh nghiệp có mạng riêng, runner riêng và quyền riêng.
- Dễ mô phỏng quy trình platform team quản lý GitLab tập trung.

Đánh đổi:

- Cần tự vận hành backup, upgrade, monitoring và bảo mật GitLab.
- Cần chuẩn bị domain, TLS certificate và cơ chế khôi phục khi có sự cố.

## Việc cần xác nhận

- Domain GitLab: `TBD`
- Cách cấp TLS certificate: `TBD`
- Chính sách backup GitLab: `TBD`
- Người sở hữu GitLab: `TBD`
- Người phê duyệt quyết định: `TBD`
