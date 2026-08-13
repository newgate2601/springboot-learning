# Shared Services GitLab

Root module này quản lý hạ tầng GitLab Self-Managed dùng chung trong `shared-services`.

GitLab là nền tảng dùng chung cho toàn bộ bài thực hành:

- Source code.
- Terraform code.
- GitOps repository.
- Merge Request.
- CI/CD pipeline.
- Protected branch, protected environment và approval rule.

Không tạo GitLab riêng cho `dev`, `staging` hoặc `production`. Các môi trường ứng dụng khác nhau bằng branch, tag, GitOps values, pipeline rule và approval policy.

## State Backend

State của root module này được lưu riêng ở:

```text
s3://newgate2601-terraform-state-150914615641-ap-southeast-1/shared-services/gitlab/terraform.tfstate
```

Key quan trọng trong `backend.tf` là:

```hcl
key = "shared-services/gitlab/terraform.tfstate"
```

Không dùng chung state với `shared-services/network`.

## Network Input

Root module này đọc output từ network shared-services qua remote state:

```text
shared-services/network/terraform.tfstate
```

Các output cần có:

```text
vpc_id
public_subnet_ids
private_app_subnet_ids
isolated_data_subnet_ids
```

## Kiến Trúc Dự Kiến

Module `terraform/modules/gitlab-self-managed` sẽ dựng GitLab theo hướng doanh nghiệp:

- Application Load Balancer, listener và target group.
- Security group theo lớp.
- EC2 GitLab application trong private application subnet.
- EC2 Gitaly và EBS riêng cho repository storage.
- RDS PostgreSQL trong isolated data subnet.
- ElastiCache Redis/Valkey trong isolated data subnet.
- S3 buckets cho artifact, upload, LFS và backup.
- IAM role/profile cần thiết.

## Cách Kiểm Tra Skeleton

```powershell
cd C:\code\springboot-learning\terraform
terraform fmt -recursive
```

```powershell
cd C:\code\springboot-learning\terraform\environments\shared-services\gitlab
terraform init
terraform validate
terraform plan
```

Ở bước skeleton, module chưa tạo resource AWS thật. Không commit các file sinh ra khi chạy Terraform như:

```text
.terraform/
.terraform.lock.hcl
terraform.tfstate
terraform.tfstate.backup
tfplan
```
