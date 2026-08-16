# Shared Services

Thư mục này dành cho các tài nguyên dùng chung trước khi tạo môi trường `dev`, `staging` hoặc `production`.

Các root module dự kiến:

- `ecr`
- `observability`
- `ci-integration`

Ý nghĩa:

- `ecr`: Amazon ECR lưu image của `gateway`, `uaa`, `post-service`.
- `observability`: Thành phần quan sát dùng chung nếu cần.
- `ci-integration`: Tích hợp CI/CD với AWS service, ví dụ IAM/OIDC/cache/artifact.

Không đặt tài nguyên workload riêng của `dev`, `staging` hoặc `production` trong thư mục này.
