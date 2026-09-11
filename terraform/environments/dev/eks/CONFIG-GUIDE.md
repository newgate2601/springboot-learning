# Điền terraform.tfvars

`terraform.tfvars` đã được tạo từ `terraform.tfvars.example`. Giữ các placeholder cho đến khi có kết quả thực tế; không plan/apply với placeholder.

| Trường | Cách điền |
|---|---|
| `kubernetes_version` | Minor version còn standard support tại ap-southeast-1, tra theo mục 3.6 của bước 4.1. |
| `operator_role_arn` | ARN IAM role quản trị đã tồn tại; không dùng ARN `arn:aws:sts::...:assumed-role/...`. |
| `node.ami_release` | SSM release_version của AL2023 x86_64 đúng minor Kubernetes, dạng `1.xx.y-YYYYMMDD`; không dùng AMI ID. |
| `addon_versions.vpc_cni` | Version VPC CNI tương thích, có hậu tố `-eksbuild.*`; kiểm tra schema hỗ trợ enableNetworkPolicy. |
| `addon_versions.kube_proxy` | Version kube-proxy tương thích với minor Kubernetes đã chọn. |
| `addon_versions.coredns` | Version CoreDNS tương thích với minor Kubernetes đã chọn. |
| `admin_public_cidrs` | Nếu quản trị từ Windows qua public API, điền public IPv4 thực tế dạng /32. Rỗng nghĩa private-only. |
| `management_security_group_ids` | SG management host được phép vào private API TCP 443 khi dùng đường private. |

Các giá trị account/region/project/environment/owner theo tài liệu. Backend đã đối chiếu với backend network hiện có; EKS dùng key riêng. Cấu hình lab theo hướng doanh nghiệp dùng hai `t3.medium` On-Demand, min 2/max 3 và EBS 30 GiB. Hai node cho phép thực hành multi-AZ, phân bố workload và rolling update; kích thước này chưa phải kết quả sizing production.

## Trước khi chạy

1. Xác nhận AWS profile hạ tầng và account; không ghi access key/secret vào tfvars.
2. Hoàn tất operator role/profile theo mục 3.4 của bước 4.1.
3. Network dev phải tồn tại và export `vpc_id`, `private_app_subnet_ids`.
4. Điền đủ phiên bản và role; chọn đường truy cập API phù hợp. Private-only cần kết nối mạng vào VPC.
5. Review plan trước apply. File source hoàn chỉnh không có nghĩa input đã sẵn sàng triển khai.

`terraform.tfvars` được ignore. `.terraform.lock.hcl`, `.terraform/`, plan, state và kubeconfig không được tạo thủ công trong lần sinh file này.
