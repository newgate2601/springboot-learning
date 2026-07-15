# ADR 004: GitLab Runner trên EC2 Auto Scaling

## Trạng thái

Đề xuất.

## Bối cảnh

Tài liệu yêu cầu GitLab Runner không được đặt tạm trên một máy rồi chuyển sang EKS. Runner trên EC2 Auto Scaling là kiến trúc chính thức của bài thực hành và tiếp tục phục vụ cả dev, staging lẫn production.

Runner cần build, test, scan, tạo container image và push image lên Amazon ECR. Runner cũng có thể tạo merge request hoặc commit đề xuất thay đổi GitOps, nhưng không trực tiếp triển khai ứng dụng bằng `kubectl apply`.

## Quyết định

Triển khai GitLab Runner trong AWS account `shared-services` bằng EC2 Auto Scaling.

Thiết kế chính:

- Runner chạy trên EC2 instance trong private subnet.
- Dùng Auto Scaling Group để có thể tăng giảm số runner theo nhu cầu.
- Runner sử dụng IAM role gắn với EC2 instance profile.
- Runner được đăng ký với GitLab Self-Managed bằng registration token hoặc runner authentication token.
- Job build Docker image sử dụng Docker hoặc công cụ build container phù hợp với chính sách bảo mật của môi trường.

Quyền tối thiểu của runner:

- Pull source code từ GitLab.
- Build và test ứng dụng.
- Push container image lên Amazon ECR.
- Đọc một số thông tin AWS cần thiết cho pipeline.
- Ghi thay đổi vào GitOps repository thông qua GitLab nếu pipeline được phép.

Runner không được:

- Có quyền production khi đang làm dev.
- Chạy `kubectl apply` vào production.
- Giữ secret dài hạn trong file cấu hình.
- Dùng quyền admin AWS.

## Luồng CI chuẩn

```text
Developer push code
  -> GitLab CI chạy test
  -> Build container image
  -> Push image lên Amazon ECR theo digest
  -> Đề xuất thay đổi GitOps repository
  -> Argo CD đồng bộ manifest xuống EKS
```

## Hệ quả

Ưu điểm:

- Runner là hạ tầng lâu dài, không phải giải pháp tạm.
- Tách biệt runner khỏi GitLab và khỏi EKS workload.
- Dễ kiểm soát quyền bằng IAM role.
- Phù hợp với mô hình CI build artifact, CD do Argo CD đảm nhiệm.

Đánh đổi:

- Cần vận hành Auto Scaling Group, AMI, patching và monitoring cho runner.
- Cần kiểm soát kỹ quyền Docker build để tránh rủi ro bảo mật.

## Việc cần xác nhận

- Instance type runner dev: `TBD`
- Số runner tối thiểu: `TBD`
- Số runner tối đa: `TBD`
- Cơ chế build image: `Docker` hoặc `TBD`
- Người sở hữu runner: `TBD`
- Người phê duyệt quyết định: `TBD`
