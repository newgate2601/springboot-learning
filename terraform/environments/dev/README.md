# Môi trường Dev

`dev` là môi trường đầy đủ đầu tiên được dựng trong bài thực hành.

Các root module dự kiến:

- `network`
- `eks`
- `data`
- `observability`

Ý nghĩa:

- `network`: VPC, subnet, route table, NAT, VPC endpoint, security group nền.
- `eks`: EKS cluster, node group, add-on nền.
- `data`: RDS MariaDB, MSK/Kafka, ElastiCache Redis/Valkey, S3, Secrets Manager.
- `observability`: log, metric, dashboard, alert.

Chỉ sau khi `dev` đạt cổng nghiệm thu, mới tạo `staging`.

`staging` và `production` phải dùng lại cùng module đã chạy tốt ở `dev`.
