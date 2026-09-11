# EKS dev — bước 4.1

Root này đọc network dev và gọi module EKS dùng chung. Các file đã được tạo từ bước 4.1 của tài liệu thực hành; chưa triển khai AWS.

- Backend: `dev/eks/terraform.tfstate`.
- Network state chỉ đọc: `dev/network/terraform.tfstate`.
- Account: `150914615641`; region: `ap-southeast-1`.
- Module: `../../../modules/eks`.
- Lab capacity: two `t3.medium` On-Demand nodes, min 2/desired 2/max 3.

Đọc [CONFIG-GUIDE.md](CONFIG-GUIDE.md) và điền `terraform.tfvars` trước khi chạy. File local này đã có sẵn, được `.gitignore` loại khỏi Git. Không chứa AWS credentials.

Sau khi hoàn tất identity/network và input, chạy riêng từng lệnh tại thư mục này:

```powershell
terraform fmt -recursive ..\..\..\modules\eks
terraform fmt
terraform init
terraform validate
terraform plan -out=eks-dev.tfplan
terraform show -no-color eks-dev.tfplan
```

Kiểm tra account, region, backend key, subnet, phiên bản, operator và API CIDR trong plan. Apply là bước thực hành riêng sau review, có tạo tài nguyên tính phí.

Không chép `.terraform`, state hoặc kubeconfig từ root khác. Lock file do `terraform init` sinh ra; commit lock file sau khi kiểm tra. Việc tạo source file chưa xác nhận cluster/node/DNS/ECR đã hoạt động.

Hướng dẫn đầy đủ: [Bước 4.1](../../../../AWS/THUC-HANH-CHI-TIET-AWS-STEP-BY-STEP.md).
