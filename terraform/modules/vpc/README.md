# VPC Module

Module này dùng để tạo VPC và các thành phần network nền có thể dùng lại cho nhiều môi trường.

Các root module có thể gọi lại module này với giá trị khác nhau:

```text
terraform/environments/dev/network
terraform/environments/staging/network
terraform/environments/production/network
```

Module không hard-code giá trị riêng của `dev`, `staging` hoặc `production`. Khác biệt giữa môi trường phải nằm ở root module hoặc file `.tfvars`.

## Module này tạo gì?

Module hiện tạo các nhóm resource chính:

- VPC.
- Internet Gateway.
- Public subnets.
- Private application subnets.
- Isolated data subnets.
- Elastic IP cho NAT Gateway.
- NAT Gateway theo chế độ `single` hoặc `one_per_az`.
- Route table cho từng nhóm subnet.
- Route table association.
- S3 Gateway VPC endpoint.
- Security group nền cho interface VPC endpoint sau này.
- CloudWatch Log Group cho VPC Flow Logs nếu bật.
- IAM role/policy cho VPC Flow Logs nếu bật.
- VPC Flow Logs nếu bật.

Module này chưa tạo EKS, RDS, MSK, ElastiCache, application workload, staging hoặc production.

## Nguyên tắc network

Public subnet có route:

```text
0.0.0.0/0 -> Internet Gateway
```

Private application subnet có route:

```text
0.0.0.0/0 -> NAT Gateway
```

Isolated data subnet không có route mặc định ra Internet Gateway hoặc NAT Gateway. Đây là subnet dành cho data layer sau này.

Với lab cá nhân, `nat_gateway_mode = "single"` giúp giảm chi phí vì chỉ tạo một NAT Gateway. Với môi trường cần khả dụng cao hơn, `nat_gateway_mode = "one_per_az"` tạo một NAT Gateway cho mỗi AZ.

## Các file trong module

| File | Vai trò |
|---|---|
| `variables.tf` | Khai báo interface đầu vào của module. |
| `main.tf` | Tạo VPC, subnet, route table, gateway, endpoint, security group và flow logs. |
| `outputs.tf` | Trả giá trị từ module về root module. |
| `README.md` | Giải thích phạm vi và cách dùng module. |

## Input của module

| Biến | Ý nghĩa |
|---|---|
| `name_prefix` | Tiền tố đặt tên resource, ví dụ `newgate2601-dev`. |
| `vpc_cidr` | CIDR block của VPC, ví dụ `10.20.0.0/16`. |
| `availability_zones` | Danh sách AZ dùng để chia subnet. |
| `public_subnet_cidrs` | CIDR cho public subnets. |
| `private_app_subnet_cidrs` | CIDR cho private application subnets. |
| `isolated_data_subnet_cidrs` | CIDR cho isolated data subnets. |
| `nat_gateway_mode` | Chế độ tạo NAT Gateway: `single` hoặc `one_per_az`. |
| `enable_vpc_flow_logs` | Bật/tắt VPC Flow Logs. |
| `tags` | Map tag chung truyền từ root module xuống. |

Root module `dev/network` truyền các giá trị này trong `main.tf`.

## Output của module

Module trả về các giá trị nền để root module sau dùng lại:

- `name_prefix`
- `vpc_id`
- `public_subnet_ids`
- `private_app_subnet_ids`
- `isolated_data_subnet_ids`
- `public_route_table_id`
- `private_app_route_table_ids`
- `isolated_data_route_table_ids`
- `vpc_endpoint_security_group_id`

Các root module khác như `dev/eks`, `dev/data` hoặc `dev/observability` sẽ dùng output network để gắn EKS, RDS, MSK, Redis và logging vào đúng VPC/subnet.
