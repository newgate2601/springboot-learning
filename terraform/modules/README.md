# Module Terraform

Thư mục này chứa các module Terraform dùng lại.

Module là một khối Terraform được viết một lần, sau đó gọi lại nhiều lần cho `dev`, `staging` và `production`.

Ví dụ:

```text
modules/vpc
  -> environments/dev/network
  -> environments/staging/network
  -> environments/production/network
```

Các module dự kiến:

- `vpc`
- `ecr`
- `eks`
- `rds-mariadb`
- `msk`
- `elasticache`
- `s3`
- `iam`
- `observability`

Nguyên tắc quan trọng:

- Không hard-code giá trị riêng của `dev`, `staging`, `production` trong module.
- Khác biệt giữa môi trường phải đưa ra biến và file `.tfvars`.
- Module phải dùng lại được, không copy thành ba bản riêng.
