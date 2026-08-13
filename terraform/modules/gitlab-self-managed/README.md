# GitLab Self-Managed Module

Module này dùng để dựng GitLab Self-Managed theo hướng doanh nghiệp.

Module dự kiến tạo:

- Application Load Balancer, listener và target group.
- Security group theo lớp.
- EC2 GitLab application.
- EC2 Gitaly và EBS riêng cho repository storage.
- RDS PostgreSQL.
- ElastiCache Redis/Valkey.
- S3 buckets cho artifact, upload, LFS và backup.
- IAM role/profile cần thiết.
- CloudWatch log/metric nền.

Bước skeleton chưa tạo resource AWS thật. Hiện tại module chỉ nhận input và trả output tối thiểu để kiểm tra wiring với root module `shared-services/gitlab`.

Không hard-code giá trị riêng của account lab trong module này. Giá trị thật phải được truyền từ root module.
