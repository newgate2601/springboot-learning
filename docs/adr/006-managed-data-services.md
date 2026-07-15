# ADR 006: Managed data services

## Trạng thái

Đề xuất.

## Bối cảnh

Ứng dụng Spring Boot trong bài thực hành cần MariaDB, Kafka, Redis/Valkey, secret và object storage. Tài liệu định hướng sử dụng dịch vụ managed của AWS để giảm tải vận hành và gần với mô hình doanh nghiệp.

Cần chốt dịch vụ dữ liệu ngay từ đầu vì quyết định này ảnh hưởng đến Terraform module, network, security group, secret, backup, monitoring và chi phí.

## Quyết định

Sử dụng các dịch vụ managed sau:

| Nhu cầu | Dịch vụ được chọn |
|---|---|
| Database quan hệ | Amazon RDS for MariaDB |
| Kafka | Amazon Managed Streaming for Apache Kafka |
| Redis/Valkey | Amazon ElastiCache |
| Secret | AWS Secrets Manager + External Secrets Operator |
| Object storage | Amazon S3 |

Trong giai đoạn `dev`, chỉ dựng data service cho `dev`. Không tạo RDS, MSK hoặc ElastiCache cho staging/production trước khi dev đạt cổng nghiệm thu.

## Nguyên tắc triển khai

- Tất cả data service chạy trong private subnet.
- Security group chỉ mở đúng luồng cần thiết từ EKS workload.
- Credential được lưu trong AWS Secrets Manager.
- Kubernetes workload lấy secret thông qua External Secrets Operator.
- Backup và retention được cấu hình bằng Terraform.
- Monitoring tích hợp CloudWatch và hệ thống observability của môi trường.

## Thiết lập đề xuất cho dev

| Dịch vụ | Thiết lập dev đề xuất |
|---|---|
| RDS MariaDB | Single-AZ hoặc cấu hình nhỏ để tiết kiệm chi phí |
| MSK | Cấu hình nhỏ nhất phù hợp bài thực hành |
| ElastiCache | Một node hoặc cấu hình tối giản |
| S3 | Bucket riêng cho dev, bật versioning nếu cần |
| Secrets Manager | Secret riêng theo namespace hoặc ứng dụng |

Production có thể dùng cấu hình cao hơn, Multi-AZ, backup dài hơn và chính sách bảo vệ nghiêm ngặt hơn, nhưng chỉ quyết định sau khi staging đã được nghiệm thu.

## Hệ quả

Ưu điểm:

- Giảm công sức tự vận hành database, Kafka và Redis.
- Phù hợp với kiến trúc AWS production.
- Dễ chuẩn hóa Terraform module và tái sử dụng cho nhiều môi trường.
- Secret không nằm trực tiếp trong Git hoặc manifest Kubernetes.

Đánh đổi:

- Chi phí cao hơn so với chạy container tự quản lý trong EKS.
- Một số dịch vụ như MSK có thời gian tạo lâu và cần thiết kế network kỹ.
- Dev cũng cần kiểm soát chi phí để tránh phát sinh không cần thiết.

## Việc cần xác nhận

- Phiên bản MariaDB: `TBD`
- Phiên bản Kafka/MSK: `TBD`
- Loại Redis hay Valkey: `TBD`
- Retention backup dev: `TBD`
- Người sở hữu dữ liệu ứng dụng: `TBD`
- Người phê duyệt quyết định: `TBD`
