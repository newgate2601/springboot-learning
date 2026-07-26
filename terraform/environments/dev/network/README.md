# Dev Network

Root module này quản lý network nền cho môi trường `dev`.

Đây là root module Terraform đầu tiên sau bước bootstrap backend. Khi cần thao tác network dev, chạy Terraform trực tiếp trong thư mục này:

```powershell
cd terraform\environments\dev\network
terraform init
terraform validate
terraform plan
```

Không chạy `terraform apply` ở toàn bộ thư mục `terraform/`, vì Terraform chỉ nên chạy trong đúng root module cần quản lý.

## State backend

State của root module này được lưu riêng ở:

```text
s3://newgate2601-terraform-state-150914615641-ap-southeast-1/dev/network/terraform.tfstate
```

Key quan trọng trong `backend.tf` là:

```hcl
key = "dev/network/terraform.tfstate"
```

Key này tách state network dev khỏi state bootstrap, EKS, data và observability. Không dùng chung một backend key cho nhiều root module.

## Các file trong thư mục này

| File | Vai trò |
|---|---|
| `versions.tf` | Khai báo Terraform CLI tối thiểu và AWS provider cần dùng. |
| `backend.tf` | Cấu hình S3 backend, DynamoDB lock table và KMS key cho state của `dev/network`. |
| `providers.tf` | Cấu hình AWS provider, region và default tags. |
| `variables.tf` | Khai báo các biến đầu vào như region, project, owner, CIDR và availability zones. |
| `locals.tf` | Gom giá trị dùng chung, ví dụ `name_prefix` và `common_tags`. |
| `main.tf` | Gọi module dùng lại `terraform/modules/vpc`. |
| `outputs.tf` | In ra giá trị quan trọng của root module sau khi plan/apply. |
| `terraform.tfvars.example` | File mẫu để tạo `terraform.tfvars` khi chạy thật. |
| `README.md` | Giải thích phạm vi và cách dùng root module này. |

## Giải thích các thành phần chính

`versions.tf` giúp khóa nền tảng chạy Terraform. Dòng `required_version = ">= 1.6.0"` yêu cầu Terraform từ bản 1.6.0 trở lên. AWS provider dùng version `~> 5.0`, nghĩa là dùng dòng 5.x và tránh tự nhảy sang major version mới ngoài kiểm soát.

`backend.tf` nói với Terraform nơi lưu state. S3 bucket lưu file state, DynamoDB table dùng để lock state khi đang chạy, KMS key dùng để mã hóa state. Đây là backend đã được tạo ở bước bootstrap.

`providers.tf` cấu hình AWS provider dùng region từ `var.aws_region`. Phần `default_tags` giúp tự gắn tag chung vào resource AWS để sau này nhìn trên AWS Console biết resource thuộc project, môi trường và component nào.

`variables.tf` chỉ khai báo biến, chưa cấp giá trị thật. Giá trị thật lấy từ `terraform.tfvars`, từ CI/CD variable hoặc từ tham số `-var`. File `terraform.tfvars.example` chỉ là mẫu được commit vào Git.

`locals.tf` tạo giá trị nội bộ để dùng lại trong code. `name_prefix` giúp đặt tên resource nhất quán, ví dụ `newgate2601-dev`. `common_tags` gom các tag chung như `Project`, `Environment`, `ManagedBy`, `Owner`, `Component` và `AccountId`.

`main.tf` gọi module VPC bằng đường dẫn:

```hcl
source = "../../../modules/vpc"
```

Đường dẫn này đi từ `terraform/environments/dev/network` lên thư mục `terraform`, rồi vào `modules/vpc`.

## Các tài nguyên dự kiến

Ở bước skeleton này, module VPC chưa tạo resource AWS thật. Các tài nguyên network sẽ được thêm ở bước triển khai tiếp theo:

- VPC.
- Public subnet.
- Private application subnet.
- Isolated data subnet.
- Internet Gateway.
- NAT Gateway.
- Route table.
- VPC endpoint.
- Security group nền.
- VPC Flow Logs.

Không đặt tài nguyên `staging` hoặc `production` trong root module này.

## Cách chạy lần đầu

Copy file biến mẫu:

```powershell
Copy-Item terraform.tfvars.example terraform.tfvars
```

Kiểm tra lại giá trị trong `terraform.tfvars`, sau đó chạy:

```powershell
terraform fmt -recursive
terraform init
terraform validate
terraform plan
```

Vì hiện tại `modules/vpc` mới là skeleton, `terraform plan` có thể báo `No changes`. Đây là bình thường. Mục tiêu của bước này là kiểm tra root module, backend và module wiring trước khi tạo VPC thật.
