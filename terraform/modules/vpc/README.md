# VPC Module

Module này dùng để tạo VPC và các thành phần network liên quan.

Module nằm ở `terraform/modules/vpc` để các root module có thể dùng lại cho nhiều môi trường:

```text
terraform/environments/dev/network
terraform/environments/staging/network
terraform/environments/production/network
```

Hiện tại module mới là skeleton. Resource AWS thật sẽ được thêm ở bước triển khai network tiếp theo.

## Nguyên tắc quan trọng

Module không được hard-code giá trị riêng của `dev`, `staging` hoặc `production`.

Không viết giá trị cố định kiểu:

```hcl
cidr_block = "10.20.0.0/16"
```

Thay vào đó, module nhận giá trị từ biến:

```hcl
cidr_block = var.vpc_cidr
```

Khác biệt giữa các môi trường phải nằm ở root module hoặc file `.tfvars`, không nằm chết trong module.

## Các file trong module

| File | Vai trò |
|---|---|
| `variables.tf` | Khai báo interface đầu vào của module. |
| `main.tf` | Nơi sẽ tạo VPC, subnet, route table, gateway, endpoint và security group. |
| `outputs.tf` | Trả giá trị từ module về root module. |
| `README.md` | Giải thích phạm vi và cách dùng module. |

## Input của module

| Biến | Ý nghĩa |
|---|---|
| `name_prefix` | Tiền tố đặt tên resource, ví dụ `newgate2601-dev`. |
| `vpc_cidr` | CIDR block của VPC, ví dụ `10.20.0.0/16`. |
| `availability_zones` | Danh sách AZ dùng để chia subnet. |
| `tags` | Map tag chung truyền từ root module xuống. |

Root module `dev/network` đang truyền các giá trị này trong `main.tf`:

```hcl
module "vpc" {
  source = "../../../modules/vpc"

  name_prefix        = local.name_prefix
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
  tags               = local.common_tags
}
```

## Output hiện tại

Module hiện trả về:

```hcl
output "name_prefix" {
  description = "Prefix used for VPC resource names."
  value       = var.name_prefix
}
```

Khi thêm resource thật, module sẽ có thêm các output như:

- `vpc_id`
- `public_subnet_ids`
- `private_subnet_ids`
- `data_subnet_ids`
- `nat_gateway_ids`
- `route_table_ids`

Các root module khác như `dev/eks` hoặc `dev/data` sẽ dùng output network để gắn EKS, RDS, MSK và Redis vào đúng VPC/subnet.
