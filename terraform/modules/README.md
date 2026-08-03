# Module Terraform

Thư mục này chứa các module Terraform dùng lại.

Module là một khối Terraform được viết một lần, sau đó các root module gọi lại với biến khác nhau cho `dev`, `staging` và `production`.

Ví dụ:

```text
terraform/modules/vpc
  -> terraform/environments/dev/network
  -> terraform/environments/staging/network
  -> terraform/environments/production/network
```

## Module khác root module thế nào?

| Thành phần | Chạy Terraform trực tiếp không? | Vai trò |
|---|---|---|
| Root module | Có | Đại diện cho một phạm vi hạ tầng cụ thể, ví dụ `dev/network`. |
| Module dùng lại | Không | Chứa logic tạo resource, được root module gọi bằng `module`. |

Root module quyết định giá trị cụ thể của môi trường. Module chỉ nhận biến và tạo resource theo biến được truyền vào.

## Luồng gọi từ environment sang module

Terraform không tự chạy module trong `terraform/modules`.

Module chỉ chạy khi một root module gọi nó:

```text
terraform/environments/dev/network/main.tf
  -> module "vpc"
  -> source = "../../../modules/vpc"
  -> terraform/modules/vpc
```

Luồng chi tiết khi chạy `terraform plan` trong `dev/network`:

```text
1. Terraform đứng ở root module dev/network
2. Đọc backend.tf để biết state lưu ở đâu
3. Đọc providers.tf để biết dùng AWS region nào
4. Đọc variables.tf để biết cần biến nào
5. Đọc terraform.tfvars để lấy giá trị thật cho dev
6. Đọc locals.tf để tạo name_prefix và common_tags
7. Đọc main.tf
8. Gặp module "vpc"
9. Đi vào source ../../../modules/vpc
10. Truyền input từ root module sang module
11. Module tạo resource AWS bằng input đã nhận
12. Module trả output về root module
13. Root module in output cuối cùng cho người chạy Terraform
```

Ví dụ truyền biến:

```text
terraform.tfvars
  vpc_cidr = "10.20.0.0/16"

dev/network/variables.tf
  khai báo variable "vpc_cidr"

dev/network/main.tf
  truyền vpc_cidr = var.vpc_cidr

modules/vpc/variables.tf
  nhận variable "vpc_cidr"

modules/vpc/main.tf
  dùng cidr_block = var.vpc_cidr để tạo aws_vpc
```

Ví dụ output quay ngược lại:

```text
modules/vpc/main.tf
  aws_vpc.this.id

modules/vpc/outputs.tf
  output "vpc_id" = aws_vpc.this.id

dev/network/outputs.tf
  output "vpc_id" = module.vpc.vpc_id

terminal
  terraform output vpc_id
```

Tóm lại:

```text
Root module truyền input xuống
Module tạo resource
Module trả output lên
Root module lưu state và expose output cho bước sau
```

## Các module dự kiến

| Module | Vai trò |
|---|---|
| `vpc` | Tạo VPC, subnet, route table, gateway, endpoint, security group nền và flow logs. |
| `ecr` | Tạo Amazon ECR repository để lưu Docker image. |
| `eks` | Tạo EKS cluster, node group, IAM role và add-on nền. |
| `rds-mariadb` | Tạo MariaDB/RDS và cấu hình liên quan. |
| `msk` | Tạo Kafka/MSK cho event streaming. |
| `elasticache` | Tạo Redis/Valkey cho cache hoặc session. |
| `s3` | Tạo bucket ứng dụng, log hoặc artifact nếu cần. |
| `iam` | Gom IAM role, policy dùng chung nếu cần tách riêng. |
| `observability` | Tạo log group, metric, dashboard, alert hoặc collector nền. |

Hiện tại module đã bắt đầu triển khai thật:

```text
terraform/modules/vpc
```

## Module VPC hiện tạo gì?

Module `vpc` hiện tạo network nền:

- VPC.
- Internet Gateway.
- Public subnets.
- Private application subnets.
- Isolated data subnets.
- Elastic IP cho NAT Gateway.
- NAT Gateway.
- Route table.
- Route table association.
- S3 Gateway VPC endpoint.
- Security group nền cho Interface VPC Endpoint sau này.
- CloudWatch Log Group cho VPC Flow Logs.
- IAM role/policy cho VPC Flow Logs.
- VPC Flow Logs.

Module này chưa tạo EKS, RDS, MSK, ElastiCache hoặc workload ứng dụng.

Khái niệm VPC Endpoint được giải thích riêng trong `AWS/AWS.md`, mục `14.7. VPC Endpoint là gì?`.

## Module VPC tạo resource theo thứ tự nào?

Trong `terraform/modules/vpc/main.tf`, resource được tổ chức theo luồng:

```text
1. Data source
   -> đọc AWS region hiện tại

2. Locals
   -> tính số AZ
   -> tính số NAT Gateway cần tạo

3. VPC
   -> tạo mạng chính bằng vpc_cidr

4. Internet Gateway
   -> gắn cổng Internet vào VPC

5. Subnets
   -> tạo public subnets
   -> tạo private application subnets
   -> tạo isolated data subnets

6. Internet egress
   -> tạo Elastic IP
   -> tạo NAT Gateway trong public subnet

7. Route tables
   -> public route table đi Internet Gateway
   -> private app route table đi NAT Gateway
   -> isolated data route table không có default route Internet

8. Route table associations
   -> gắn subnet vào đúng route table

9. VPC endpoints
   -> tạo security group nền cho Interface Endpoint sau này
   -> tạo S3 Gateway Endpoint

10. Flow logs
   -> tạo CloudWatch Log Group
   -> tạo IAM role/policy
   -> bật VPC Flow Logs
```

Terraform tự xây dependency graph từ các reference như:

```hcl
vpc_id = aws_vpc.this.id
```

Nghĩa là subnet phải chờ VPC có trước, route table phải biết VPC, NAT Gateway phải biết Elastic IP và public subnet. Dù code nằm trong một file, Terraform không chạy từ trên xuống một cách máy móc như script; nó tạo graph phụ thuộc rồi plan/apply theo graph đó.

## Giải thích các dòng chính trong `modules/vpc/main.tf`

### Data source

```hcl
data "aws_region" "current" {}
```

Dòng này đọc region AWS hiện tại từ provider. Module dùng nó để ghép service name cho S3 Gateway Endpoint:

```hcl
service_name = "com.amazonaws.${data.aws_region.current.name}.s3"
```

Nếu provider đang chạy ở `ap-southeast-1`, service name sẽ thành:

```text
com.amazonaws.ap-southeast-1.s3
```

### Locals

```hcl
locals {
  az_count          = length(var.availability_zones)
  nat_gateway_count = var.nat_gateway_mode == "one_per_az" ? local.az_count : 1
}
```

`az_count` đếm số Availability Zone được truyền vào. Nếu `availability_zones` có 3 phần tử thì `az_count = 3`.

`nat_gateway_count` quyết định số NAT Gateway:

- Nếu `nat_gateway_mode = "one_per_az"` thì tạo số NAT Gateway bằng số AZ.
- Nếu không, tạo `1` NAT Gateway để tiết kiệm chi phí lab.

### VPC

```hcl
resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true
}
```

`aws_vpc.this` là VPC chính của môi trường. `cidr_block = var.vpc_cidr` lấy CIDR từ root module, ví dụ `10.20.0.0/16`.

`enable_dns_support = true` bật DNS resolver trong VPC. `enable_dns_hostnames = true` cho resource trong VPC có DNS hostname, cần cho nhiều dịch vụ AWS và EKS sau này.

```hcl
tags = merge(var.tags, {
  Name = "${var.name_prefix}-vpc"
})
```

`merge` ghép tag chung từ root module với tag riêng `Name`.

### Internet Gateway

```hcl
resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id
}
```

Internet Gateway được gắn vào VPC bằng `vpc_id = aws_vpc.this.id`. Nhờ reference này, Terraform biết phải tạo VPC trước rồi mới tạo Internet Gateway.

Internet Gateway chưa tự làm subnet public. Subnet chỉ public khi route table của subnet có route `0.0.0.0/0` trỏ tới Internet Gateway.

### Subnets

Public subnet:

```hcl
resource "aws_subnet" "public" {
  count = local.az_count

  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true
}
```

`count = local.az_count` tạo một public subnet trên mỗi AZ. `map_public_ip_on_launch = true` cho phép EC2 tạo trong subnet này có public IP nếu instance bật tùy chọn auto-assign public IP.

Private application subnet:

```hcl
resource "aws_subnet" "private_app" {
  count = local.az_count

  cidr_block        = var.private_app_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]
}
```

Nhóm subnet này dành cho application workload private sau này, ví dụ EKS node, ECS task hoặc EC2 app.

Isolated data subnet:

```hcl
resource "aws_subnet" "isolated_data" {
  count = local.az_count

  cidr_block        = var.isolated_data_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]
}
```

Nhóm subnet này dành cho data layer như RDS, MSK hoặc Redis sau này.

### Elastic IP và NAT Gateway

```hcl
resource "aws_eip" "nat" {
  count  = local.nat_gateway_count
  domain = "vpc"
}
```

Elastic IP là public IP cố định của NAT Gateway.

```hcl
resource "aws_nat_gateway" "this" {
  count = local.nat_gateway_count

  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id

  depends_on = [aws_internet_gateway.this]
}
```

NAT Gateway cần Elastic IP và phải nằm trong public subnet. `depends_on` yêu cầu Terraform tạo Internet Gateway trước NAT Gateway.

### Route tables

Public route table:

```hcl
route {
  cidr_block = "0.0.0.0/0"
  gateway_id = aws_internet_gateway.this.id
}
```

Route này làm public subnet có đường trực tiếp ra Internet Gateway.

Private application route table:

```hcl
route {
  cidr_block     = "0.0.0.0/0"
  nat_gateway_id = aws_nat_gateway.this[var.nat_gateway_mode == "one_per_az" ? count.index : 0].id
}
```

Private app subnet đi Internet qua NAT Gateway. Nếu mode là `one_per_az`, subnet ở AZ nào dùng NAT Gateway cùng index với AZ đó. Nếu mode là `single`, mọi private subnet dùng NAT Gateway đầu tiên.

Isolated data route table không có route `0.0.0.0/0`, nên không có đường Internet mặc định.

### Route table association

Association là bước gắn subnet vào route table:

```hcl
subnet_id      = aws_subnet.public[count.index].id
route_table_id = aws_route_table.public.id
```

Nếu chỉ tạo route table mà không association, subnet chưa dùng route table đó.

### VPC Endpoint resources

Module tạo security group nền:

```hcl
resource "aws_security_group" "vpc_endpoint" {
  name   = "${var.name_prefix}-vpc-endpoint-sg"
  vpc_id = aws_vpc.this.id
}
```

Security group này dành cho Interface VPC Endpoint sau này. Khái niệm và luồng traffic được giải thích trong `AWS/AWS.md`, mục `14.7. VPC Endpoint là gì?`.

Module cũng tạo S3 Gateway Endpoint:

```hcl
resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.this.id
  service_name      = "com.amazonaws.${data.aws_region.current.name}.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = concat(aws_route_table.private_app[*].id, aws_route_table.isolated_data[*].id)
}
```

S3 Gateway Endpoint được gắn vào route table của private app và isolated data subnet để các subnet này gọi S3 qua đường AWS private.

### Flow Logs

```hcl
resource "aws_cloudwatch_log_group" "vpc_flow_logs" {
  count = var.enable_vpc_flow_logs ? 1 : 0
}
```

Nếu `enable_vpc_flow_logs = true`, Terraform tạo log group. Nếu `false`, Terraform không tạo resource này.

```hcl
retention_in_days = 30
```

Log được giữ 30 ngày để tránh lưu mãi và phát sinh chi phí không cần thiết.

```hcl
resource "aws_iam_role" "vpc_flow_logs" {
  assume_role_policy = jsonencode(...)
}
```

Role này cho phép dịch vụ `vpc-flow-logs.amazonaws.com` ghi log thay cho VPC Flow Logs.

```hcl
resource "aws_flow_log" "this" {
  iam_role_arn    = aws_iam_role.vpc_flow_logs[0].arn
  log_destination = aws_cloudwatch_log_group.vpc_flow_logs[0].arn
  traffic_type    = "ALL"
  vpc_id          = aws_vpc.this.id
}
```

Resource này bật Flow Logs cho VPC.

## Vì sao module không hard-code môi trường?

Không viết giá trị riêng của `dev` trực tiếp trong module, ví dụ:

```hcl
cidr_block = "10.20.0.0/16"
```

Thay vào đó, module nhận biến:

```hcl
cidr_block = var.vpc_cidr
```

Khác biệt giữa môi trường nằm ở root module hoặc file `.tfvars`:

```text
dev        -> vpc_cidr = "10.20.0.0/16"
staging    -> vpc_cidr = "10.30.0.0/16"
production -> vpc_cidr = "10.40.0.0/16"
```

Cùng một module `vpc` có thể dùng lại cho cả ba môi trường mà không copy code.

## Input và output

Module nên có input rõ ràng:

- Naming: `name_prefix`.
- Network design: `vpc_cidr`, subnet CIDR, availability zones.
- Behavior: `nat_gateway_mode`, `enable_vpc_flow_logs`.
- Tagging: `tags`.

Module nên output đủ thông tin để root module sau dùng tiếp:

- `vpc_id`
- `public_subnet_ids`
- `private_app_subnet_ids`
- `isolated_data_subnet_ids`
- route table ids
- security group ids

Ví dụ `dev/eks` sau này cần `vpc_id` và private subnet ids để tạo EKS cluster/node group trong đúng network.

## Nguyên tắc quan trọng

- Không hard-code giá trị riêng của `dev`, `staging`, `production` trong module.
- Khác biệt giữa môi trường phải đưa ra biến và file `.tfvars`.
- Module phải dùng lại được, không copy thành ba bản riêng.
- Module không chứa backend config. Backend config nằm ở root module.
- Module không tự quyết định apply ở môi trường nào. Root module mới là nơi chạy Terraform.
