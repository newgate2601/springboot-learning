# ADR 005: GitOps với Argo CD

## Trạng thái

Đề xuất.

## Bối cảnh

Tài liệu chọn Argo CD làm công cụ Continuous Delivery theo mô hình GitOps. GitLab CI chịu trách nhiệm build, test, scan và tạo image. Argo CD chịu trách nhiệm đọc GitOps repository và đồng bộ manifest xuống EKS.

Điểm quan trọng là GitLab CI không chạy `kubectl apply` vào production, và Argo CD không build source code.

## Quyết định

Sử dụng Argo CD cho từng môi trường Kubernetes.

Trong giai đoạn đầu:

- Chỉ cài Argo CD cho `dev` sau khi nền tảng dùng chung đã hoàn thành.
- Không tạo Argo CD staging khi dev chưa đạt cổng nghiệm thu.
- Không tạo Argo CD production khi staging chưa đạt cổng nghiệm thu.

GitOps repository có cấu trúc đề xuất:

```text
platform-gitops/
├── apps/
│   └── springboot-learning/
│       ├── base/
│       └── overlays/
│           ├── dev/
│           ├── staging/
│           └── production/
└── argocd/
    ├── projects/
    └── applications/
```

Trong giai đoạn dev, chỉ hoàn thiện `base` và `overlays/dev`. Các thư mục staging và production chỉ được tạo hoặc kích hoạt khi đến đúng giai đoạn.

## Nguyên tắc triển khai

- Container image được tham chiếu bằng digest bất biến khi promote qua các môi trường.
- Thay đổi manifest đi qua GitOps repository và có review.
- Argo CD đồng bộ trạng thái mong muốn từ Git xuống EKS.
- Secret không được lưu plain text trong GitOps repository.
- Secret được lấy từ AWS Secrets Manager thông qua External Secrets Operator.

## Luồng triển khai

```text
GitLab CI build image
  -> Push image lên Amazon ECR
  -> Cập nhật GitOps repository
  -> Argo CD phát hiện thay đổi
  -> Argo CD sync xuống EKS dev
```

## Hệ quả

Ưu điểm:

- Tách rõ CI và CD.
- Dễ audit vì mọi thay đổi triển khai đều đi qua Git.
- Có thể promote cùng một image digest từ dev sang staging và production.
- Phù hợp với cổng nghiệm thu từng môi trường.

Đánh đổi:

- Cần vận hành Argo CD, RBAC và Application/Project.
- Cần thiết kế GitOps repository cẩn thận để tránh drift giữa các môi trường.

## Việc cần xác nhận

- Dùng Kustomize hay Helm: `TBD`
- Chính sách sync Argo CD: `manual` cho giai đoạn đầu, có thể đổi sau
- Cơ chế quản lý secret: `AWS Secrets Manager + External Secrets Operator`
- Người sở hữu GitOps repository: `TBD`
- Người phê duyệt quyết định: `TBD`
