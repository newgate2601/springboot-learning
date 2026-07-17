# Shared Services

Thư mục này dành cho các tài nguyên dùng chung trước khi tạo môi trường `dev`, `staging` hoặc `production`.

Các root module dự kiến:

- `gitlab`
- `runner`
- `ecr`

Ý nghĩa:

- `gitlab`: GitLab Self-Managed.
- `runner`: GitLab Runner chạy trên EC2 Auto Scaling.
- `ecr`: Amazon ECR lưu image của `gateway`, `uaa`, `post-service`.

Không đặt tài nguyên workload riêng của `dev`, `staging` hoặc `production` trong thư mục này.
