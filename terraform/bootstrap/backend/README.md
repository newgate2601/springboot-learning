# Terraform Bootstrap Backend

Thư mục này dùng để tạo **Terraform remote state backend**.

Nói đơn giản, đây là bước chuẩn bị nơi lưu trạng thái Terraform trước khi ta tạo VPC, EKS, RDS, ECR và các hạ tầng lớn khác.

Thư mục này tạo các tài nguyên:

- S3 bucket để lưu file `terraform.tfstate`.
- DynamoDB table để khóa state khi Terraform đang chạy.
- KMS key để mã hóa state.
- S3 bucket policy để bắt buộc dùng HTTPS và SSE-KMS.

## Vì sao cần thư mục này?

Terraform cần một file state để nhớ nó đã tạo tài nguyên nào.

Nếu để state trên máy cá nhân:

- Dễ mất file.
- Khó dùng chung với GitLab CI.
- Dễ bị hai người chạy `terraform apply` cùng lúc.

Vì vậy ta tạo remote backend bằng S3 và DynamoDB.

## Chạy lần đầu

Copy file biến mẫu:

```powershell
Copy-Item terraform.tfvars.example terraform.tfvars
```

Mở `terraform.tfvars` và sửa lại nếu account id, owner, project hoặc bucket name của bạn khác.

Sau đó chạy:

```powershell
terraform init
terraform fmt
terraform validate
terraform plan
terraform apply
```

Lần chạy đầu tiên này dùng local state vì S3 backend chưa tồn tại.

## Chuyển bootstrap state lên S3

Sau khi `terraform apply` thành công, lấy KMS key ARN:

```powershell
terraform output kms_key_arn
```

Mở file:

```text
remote-state/backend.tf
```

Thay:

```text
<replace-with-kms-key-arn-output>
```

bằng giá trị thật từ output `kms_key_arn`.

Sau đó copy file backend vào thư mục hiện tại:

```powershell
Copy-Item remote-state\backend.tf backend.tf
```

Chuyển state từ local lên S3:

```powershell
terraform init -migrate-state
```

Khi Terraform hỏi có muốn migrate state không, nhập:

```text
yes
```

Từ thời điểm này, chính bootstrap project cũng dùng remote state.
