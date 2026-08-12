# Shared Services Network

Root module này quản lý network nền cho nhóm tài nguyên dùng chung `shared-services`.

Network này dùng cho các thành phần platform như:

- GitLab Self-Managed.
- GitLab Runner.
- Amazon ECR.
- Các thành phần CI/CD nền tảng.

Không đặt workload riêng của `dev`, `staging` hoặc `production` trong root module này.

## State Backend

State của root module này được lưu riêng ở:

```text
s3://newgate2601-terraform-state-150914615641-ap-southeast-1/shared-services/network/terraform.tfstate
```

Key quan trọng trong `backend.tf` là:

```hcl
key = "shared-services/network/terraform.tfstate"
```

Không dùng chung key này với `dev/network`, `staging`, `production` hoặc bootstrap backend.

## Các File Trong Thư Mục Này

| File | Vai trò |
|---|---|
| `versions.tf` | Khai báo Terraform CLI tối thiểu và AWS provider cần dùng. |
| `backend.tf` | Cấu hình S3 backend, DynamoDB lock table và KMS key cho state của `shared-services/network`. |
| `providers.tf` | Cấu hình AWS provider, region và default tags. |
| `variables.tf` | Khai báo các biến đầu vào như region, project, owner, CIDR và availability zones. |
| `locals.tf` | Gom giá trị dùng chung, ví dụ `name_prefix` và `common_tags`. |
| `main.tf` | Gọi module dùng lại `terraform/modules/vpc`. |
| `outputs.tf` | In ra giá trị quan trọng của root module sau khi plan/apply. |
| `terraform.tfvars.example` | File mẫu để tạo `terraform.tfvars` khi chạy thật. |
| `terraform.tfvars` | File input thực hành cho account lab hiện tại. |
| `README.md` | Giải thích phạm vi và cách dùng root module này. |

## CIDR Đang Dùng

```text
shared-services: 10.10.0.0/16
dev:             10.20.0.0/16
```

Không để CIDR của `shared-services` trùng với `dev`, vì sau này có thể cần VPC peering, Transit Gateway hoặc kết nối private giữa các VPC.

## Cách Chạy

Chạy Terraform trong đúng root module này:

```powershell
cd C:\code\springboot-learning\terraform\environments\shared-services\network
terraform init
terraform validate
terraform plan -out=tfplan
```

Chỉ apply sau khi kiểm tra plan không có thay đổi vào tài nguyên `dev`, `staging` hoặc `production`.

Không commit các file sinh ra khi chạy Terraform như:

```text
.terraform/
.terraform.lock.hcl
terraform.tfstate
terraform.tfstate.backup
tfplan
```
