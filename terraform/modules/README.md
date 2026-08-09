# Module Terraform

Thư mục này chứa các module Terraform dùng lại cho nhiều môi trường.

Module là một khối Terraform được viết một lần, sau đó các root module gọi lại với giá trị khác nhau cho `dev`, `staging` và `production`.

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

Luồng tổng quát khi chạy `terraform plan` trong một root module:

```text
1. Terraform đứng ở root module của môi trường.
2. Đọc backend, provider, variable, local và tfvars của môi trường đó.
3. Gặp block module.
4. Đi vào source của module dùng lại.
5. Truyền input từ root module xuống module.
6. Module tạo resource theo input đã nhận.
7. Module trả output lên root module.
8. Root module lưu state và expose output cho bước tiếp theo.
```

Tóm lại:

```text
Root module truyền input xuống
Module tạo resource
Module trả output lên
Root module lưu state
```

## Các module dự kiến

| Module | Vai trò |
|---|---|
| `vpc` | Tạo network nền như VPC, subnet, route table, gateway, endpoint và flow logs. |
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

Chi tiết triển khai của từng module nên được ghi trong README riêng của module đó, ví dụ:

```text
terraform/modules/vpc/README.md
```

## Nguyên tắc chung

- Không hard-code giá trị riêng của `dev`, `staging`, `production` trong module dùng lại.
- Khác biệt giữa môi trường nên nằm ở root module hoặc file `.tfvars`.
- Module phải dùng lại được, không copy thành nhiều bản riêng cho từng môi trường.
- Module không chứa backend config. Backend config nằm ở root module.
- Module không tự quyết định apply ở môi trường nào. Root module mới là nơi chạy Terraform.
