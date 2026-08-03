# Môi trường Dev

`dev` là môi trường đầy đủ đầu tiên được dựng trong bài thực hành.

Mục tiêu của `dev` là tạo hạ tầng thật trước, kiểm tra cách thiết kế có chạy ổn không, rồi sau đó mới nhân rộng sang `staging` và `production`.

## Cấu trúc dự kiến

Các root module trong môi trường `dev`:

```text
terraform/environments/dev/
├── network
├── eks
├── data
└── observability
```

Hiện tại đã bắt đầu với:

```text
terraform/environments/dev/network
```

## Root module là gì?

Root module là thư mục Terraform được chạy trực tiếp bằng các lệnh:

```powershell
terraform init
terraform validate
terraform plan
terraform apply
```

Ví dụ khi làm network dev, đứng trong thư mục:

```powershell
cd terraform\environments\dev\network
```

Không chạy Terraform ở toàn bộ thư mục `terraform/`, vì mỗi root module phải có state riêng và phạm vi riêng.

## Các thành phần trong môi trường dev

| Root module | Vai trò | Trạng thái |
|---|---|---|
| `network` | Tạo VPC, subnet, route table, Internet Gateway, NAT Gateway, VPC endpoint, security group nền và VPC Flow Logs. | Đang triển khai trước. |
| `eks` | Tạo EKS cluster, node group và add-on nền. | Làm sau khi network ổn. |
| `data` | Tạo RDS MariaDB, MSK/Kafka, ElastiCache Redis/Valkey, S3, Secrets Manager. | Làm sau network và compute nền. |
| `observability` | Tạo log, metric, dashboard và alert. | Làm sau khi có workload cần quan sát. |

Luồng phụ thuộc:

```text
dev/network
  -> dev/eks
  -> dev/data
  -> dev/observability
```

Network phải đi trước vì EKS, RDS, MSK, Redis và logging đều cần VPC/subnet để gắn vào.

## Network dev gồm những gì?

Root module `network` gọi module dùng lại:

```text
terraform/modules/vpc
```

Các nhóm tài nguyên network chính:

- VPC: mạng riêng trong AWS cho project.
- Public subnet: subnet có route `0.0.0.0/0` qua Internet Gateway.
- Private application subnet: subnet cho workload private, có thể đi Internet qua NAT Gateway.
- Isolated data subnet: subnet cho data layer, không có default route ra Internet.
- Internet Gateway: cổng giúp public subnet kết nối trực tiếp với Internet.
- NAT Gateway: cổng outbound cho private subnet đi ra Internet.
- Route table: bảng định tuyến traffic cho từng nhóm subnet.
- VPC endpoint: đường riêng để VPC gọi một số AWS service.
- Security group nền: nhóm rule dùng cho endpoint/internal traffic.
- VPC Flow Logs: log network để audit và troubleshoot.

## Luồng Terraform chạy từ dev sang module

Khi đứng trong root module:

```powershell
cd terraform\environments\dev\network
```

và chạy:

```powershell
terraform plan
```

Terraform xử lý theo luồng:

```text
terraform/environments/dev/network
  -> đọc versions.tf
  -> đọc backend.tf
  -> đọc providers.tf
  -> đọc variables.tf
  -> đọc terraform.tfvars
  -> đọc locals.tf
  -> đọc main.tf
  -> thấy module "vpc"
  -> đi tới terraform/modules/vpc
  -> truyền input từ root module sang module
  -> module tạo resource AWS
  -> module trả output về root module
  -> root module output lại giá trị cần dùng sau
```

Nói ngắn hơn:

```text
dev/network = nơi chọn môi trường, backend, region, CIDR, tag
modules/vpc = nơi chứa công thức tạo VPC/subnet/gateway/route
```

Root module truyền giá trị xuống module trong `main.tf`:

```hcl
module "vpc" {
  source = "../../../modules/vpc"

  name_prefix                = local.name_prefix
  vpc_cidr                   = var.vpc_cidr
  availability_zones         = var.availability_zones
  public_subnet_cidrs        = var.public_subnet_cidrs
  private_app_subnet_cidrs   = var.private_app_subnet_cidrs
  isolated_data_subnet_cidrs = var.isolated_data_subnet_cidrs
  nat_gateway_mode           = var.nat_gateway_mode
  enable_vpc_flow_logs       = var.enable_vpc_flow_logs
  tags                       = local.common_tags
}
```

Nguồn của các giá trị này:

| Giá trị | Đến từ đâu | Dùng để làm gì |
|---|---|---|
| `var.vpc_cidr` | `terraform.tfvars` | Tạo CIDR chính cho VPC. |
| `var.public_subnet_cidrs` | `terraform.tfvars` | Tạo public subnets. |
| `var.private_app_subnet_cidrs` | `terraform.tfvars` | Tạo private application subnets. |
| `var.isolated_data_subnet_cidrs` | `terraform.tfvars` | Tạo isolated data subnets. |
| `local.name_prefix` | `locals.tf` | Đặt tên resource theo project + environment. |
| `local.common_tags` | `locals.tf` | Gắn tag chung vào resource AWS. |
| `var.nat_gateway_mode` | `terraform.tfvars` hoặc default | Chọn tạo một NAT Gateway hay mỗi AZ một NAT Gateway. |
| `var.enable_vpc_flow_logs` | `terraform.tfvars` hoặc default | Bật/tắt VPC Flow Logs. |

## Giải thích `dev/network/main.tf`

File `terraform/environments/dev/network/main.tf` hiện chỉ có một block chính:

```hcl
module "vpc" {
  source = "../../../modules/vpc"

  name_prefix                = local.name_prefix
  vpc_cidr                   = var.vpc_cidr
  availability_zones         = var.availability_zones
  public_subnet_cidrs        = var.public_subnet_cidrs
  private_app_subnet_cidrs   = var.private_app_subnet_cidrs
  isolated_data_subnet_cidrs = var.isolated_data_subnet_cidrs
  nat_gateway_mode           = var.nat_gateway_mode
  enable_vpc_flow_logs       = var.enable_vpc_flow_logs
  tags                       = local.common_tags
}
```

Giải thích từng dòng:

| Dòng | Nghĩa |
|---|---|
| `module "vpc"` | Khai báo root module này sẽ gọi một module con tên nội bộ là `vpc`. |
| `source = "../../../modules/vpc"` | Chỉ đường dẫn tới module dùng lại nằm ở `terraform/modules/vpc`. |
| `name_prefix = local.name_prefix` | Truyền tiền tố tên resource, ví dụ `newgate2601-dev`, xuống module. |
| `vpc_cidr = var.vpc_cidr` | Truyền CIDR chính của VPC, ví dụ `10.20.0.0/16`. |
| `availability_zones = var.availability_zones` | Truyền danh sách AZ để module tạo subnet trải trên nhiều AZ. |
| `public_subnet_cidrs = var.public_subnet_cidrs` | Truyền danh sách CIDR cho public subnets. |
| `private_app_subnet_cidrs = var.private_app_subnet_cidrs` | Truyền danh sách CIDR cho private application subnets. |
| `isolated_data_subnet_cidrs = var.isolated_data_subnet_cidrs` | Truyền danh sách CIDR cho isolated data subnets. |
| `nat_gateway_mode = var.nat_gateway_mode` | Chọn tạo một NAT Gateway hay mỗi AZ một NAT Gateway. |
| `enable_vpc_flow_logs = var.enable_vpc_flow_logs` | Bật/tắt VPC Flow Logs. |
| `tags = local.common_tags` | Truyền tag chung xuống module để gắn lên resource AWS. |

Điểm quan trọng: file này **không tự tạo VPC trực tiếp**. Nó chỉ chọn module VPC, truyền giá trị của môi trường `dev` xuống module, rồi module mới tạo resource AWS thật.

So sánh dễ hiểu:

```text
dev/network/main.tf
  -> chọn công thức modules/vpc
  -> đưa nguyên liệu của dev vào công thức

modules/vpc/main.tf
  -> dùng nguyên liệu đó để tạo VPC, subnet, gateway, route
```

Sau khi module tạo resource, root module lấy output từ module:

```hcl
output "vpc_id" {
  value = module.vpc.vpc_id
}
```

Luồng output:

```text
aws_vpc.this.id trong modules/vpc
  -> output "vpc_id" của modules/vpc
  -> module.vpc.vpc_id trong dev/network
  -> output "vpc_id" của dev/network
  -> terraform output
```

Các bước sau như `dev/eks` hoặc `dev/data` sẽ cần đọc lại các output này để biết nên gắn EKS, RDS, MSK hoặc Redis vào VPC/subnet nào.

## State của dev

Mỗi root module trong `dev` phải có state riêng trên S3 backend.

Ví dụ network dev dùng key:

```hcl
key = "dev/network/terraform.tfstate"
```

Sau này các root module khác sẽ có key riêng:

```text
dev/eks/terraform.tfstate
dev/data/terraform.tfstate
dev/observability/terraform.tfstate
```

Không dùng chung state key giữa nhiều root module, vì Terraform có thể trộn tài nguyên và plan sai phạm vi.

## Nguyên tắc làm dev trước

Chỉ sau khi `dev` đạt cổng nghiệm thu mới tạo `staging`.

`staging` và `production` phải dùng lại cùng module đã chạy tốt ở `dev`, chỉ khác biến đầu vào như CIDR, sizing, số node, retention hoặc mức HA.

Không copy module thành ba bản riêng cho ba môi trường.
