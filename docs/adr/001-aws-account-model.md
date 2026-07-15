# ADR 001: Mô hình AWS account

## Trạng thái

Đề xuất.

## Bối cảnh

Bài thực hành triển khai hệ thống Spring Boot theo hướng gần với doanh nghiệp, gồm GitLab Self-Managed, GitLab Runner, Amazon ECR, Amazon EKS, Amazon RDS for MariaDB, Amazon MSK, Amazon ElastiCache, AWS Secrets Manager và Argo CD.

Tài liệu yêu cầu không dựng đồng thời `dev`, `staging` và `production`. Thứ tự bắt buộc là hoàn thành nền tảng dùng chung, sau đó hoàn thành `dev`, tiếp theo mới dựng `staging`, cuối cùng mới dựng `production`.

Vì hệ thống có nhiều môi trường và nhiều loại tài nguyên, cần chốt mô hình account trước khi tạo hạ tầng để tránh trộn lẫn quyền truy cập, chi phí, dữ liệu và rủi ro vận hành.

## Quyết định

Sử dụng mô hình nhiều AWS account, tách theo trách nhiệm:

| Account | Mục đích |
|---|---|
| `shared-services` | Chạy GitLab Self-Managed, GitLab Runner trên EC2 Auto Scaling, lưu Terraform state dùng chung, quản lý ECR dùng chung nếu cần |
| `dev` | Chạy môi trường phát triển: EKS dev, RDS MariaDB dev, MSK dev, ElastiCache dev |
| `staging` | Chạy môi trường kiểm thử trước production, chỉ tạo sau khi `dev` đạt cổng nghiệm thu |
| `production` | Chạy môi trường production, chỉ tạo sau khi `staging` đạt cổng nghiệm thu |

Trong giai đoạn đầu của bài thực hành, chỉ chuẩn bị quyền và cấu trúc cho `shared-services` và `dev`. Không tạo sẵn tài nguyên `staging` hoặc `production`.

## Quyền truy cập

- Người quản trị nền tảng có quyền triển khai hạ tầng bằng Terraform trong `shared-services` và `dev`.
- GitLab Runner chỉ được cấp quyền tối thiểu để build, push image lên Amazon ECR và đề xuất thay đổi GitOps.
- Argo CD chỉ được cấp quyền đồng bộ manifest xuống EKS của môi trường tương ứng.
- Không cấp quyền production cho GitLab Runner hoặc Argo CD trong giai đoạn đang làm `dev`.
- Quyền truy cập AWS sử dụng IAM role thay vì access key dài hạn nếu có thể.

## Hệ quả

Ưu điểm:

- Tách biệt rõ tài nguyên dùng chung, dev, staging và production.
- Giảm nguy cơ thao tác nhầm vào production.
- Dễ kiểm soát chi phí theo từng môi trường.
- Phù hợp với quy trình nghiệm thu từng giai đoạn.

Đánh đổi:

- Cần cấu hình IAM role, trust policy và Terraform provider phức tạp hơn.
- Cần quản lý nhiều account và nhiều backend state.

## Việc cần xác nhận

- AWS Organization ID: `TBD`
- Account ID `shared-services`: `TBD`
- Account ID `dev`: `TBD`
- Account ID `staging`: `TBD`
- Account ID `production`: `TBD`
- Người phê duyệt quyết định: `TBD`
