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

Ở đây `state` là **Terraform state**: file ghi nhớ Terraform đang quản lý resource thật nào trên AWS. Ví dụ resource trong code `aws_s3_bucket.tf_state` tương ứng với S3 bucket thật nào, `aws_dynamodb_table.lock` tương ứng với DynamoDB table thật nào.

`local state` nghĩa là file state tạm thời nằm trên máy đang chạy Terraform, thường là:

```text
terraform.tfstate
```

Điều này **không có nghĩa là Terraform tạo resource local**. Các resource như S3 bucket, DynamoDB table, KMS key vẫn được tạo thật trên AWS. Chỉ có "sổ ghi chép" của Terraform đang nằm local.

Lý do phải làm vậy là vì ở lần đầu tiên, chính S3 bucket dùng để lưu remote state còn chưa tồn tại. Terraform không thể lưu state lên một S3 bucket chưa được tạo, nên quy trình là:

```text
1. Dùng local state tạm thời
2. Tạo S3 bucket, DynamoDB table, KMS key thật trên AWS
3. Cấu hình S3 backend
4. Chạy terraform init -migrate-state để chuyển state từ máy local lên S3
```

## Tag được gắn vào resource như thế nào?

Tag là các cặp key/value gắn lên resource AWS để sau này dễ nhận biết resource thuộc project nào, môi trường nào, ai quản lý.

Ví dụ tag:

```text
Project     = newgate2601
Environment = bootstrap
ManagedBy   = Terraform
Owner       = tony
Component   = terraform-backend
AccountId   = 150914615641
```

Trong module này, ta không viết lặp lại tag trong từng resource. Thay vào đó Terraform đi theo luồng này:

```text
terraform.tfvars
  -> truyền giá trị thật cho các biến

locals.tf
  -> gom các biến đó thành local.common_tags

providers.tf
  -> cấu hình AWS provider tự gắn local.common_tags làm default_tags

AWS provider
  -> khi tạo resource AWS, tự gửi các tag đó lên AWS
```

Cụ thể hơn, trong `terraform.tfvars` ta có giá trị thật:

```hcl
project     = "newgate2601"
environment = "bootstrap"
account_id  = "150914615641"
owner       = "tony"
```

Trong `locals.tf`, Terraform gom các giá trị đó lại:

```hcl
locals {
  common_tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "Terraform"
    Owner       = var.owner
    Component   = "terraform-backend"
    AccountId   = var.account_id
  }
}
```

Ở đây `local` nghĩa là **giá trị nội bộ trong Terraform code**.

Nó không phải local state, cũng không phải resource trên máy local. Nó chỉ là một tên trung gian để ta dùng lại trong code.

Trong file này:

```hcl
locals {
  common_tags = {
    ...
  }
}
```

Terraform tạo ra một giá trị tên là:

```text
local.common_tags
```

Sau đó ở file khác, ta có thể gọi lại bằng đúng tên đó:

```hcl
tags = local.common_tags
```

Tác dụng của `local.common_tags` là tránh viết lặp lại cùng một nhóm tag nhiều lần.

Nếu không dùng `local.common_tags`, ta có thể phải viết trực tiếp như thế này:

```hcl
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "Terraform"
      Owner       = var.owner
      Component   = "terraform-backend"
      AccountId   = var.account_id
    }
  }
}
```

Cách trên vẫn chạy được, nhưng phần provider sẽ dài hơn. Nếu sau này nhiều nơi cần dùng cùng nhóm tag, ta lại phải copy lại nhiều lần.

Dùng `local.common_tags` thì luồng rõ hơn:

```text
locals.tf
  -> định nghĩa một lần nhóm tag chung

providers.tf
  -> gọi lại local.common_tags

Sau này nếu cần sửa tag chung
  -> sửa ở locals.tf
```

Sau khi thay giá trị từ `terraform.tfvars`, Terraform hiểu thành:

```text
local.common_tags = {
  Project     = "newgate2601"
  Environment = "bootstrap"
  ManagedBy   = "Terraform"
  Owner       = "tony"
  Component   = "terraform-backend"
  AccountId   = "150914615641"
}
```

Trong `providers.tf`, ta cấu hình:

```hcl
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}
```

Ý nghĩa là:

```text
Mỗi lần AWS provider tạo resource,
nếu resource đó hỗ trợ tag,
thì tự gắn local.common_tags vào resource đó.
```

Vì vậy trong `main.tf`, resource S3 bucket chỉ cần viết:

```hcl
resource "aws_s3_bucket" "terraform_state" {
  bucket = var.state_bucket_name
}
```

Ta không thấy dòng `tags = ...` trong resource này, nhưng khi chạy `terraform plan`, Terraform vẫn hiện `tags_all`:

```text
tags_all = {
  "AccountId"   = "150914615641"
  "Component"   = "terraform-backend"
  "Environment" = "bootstrap"
  "ManagedBy"   = "Terraform"
  "Owner"       = "tony"
  "Project"     = "newgate2601"
}
```

`tags_all` nghĩa là toàn bộ tag cuối cùng AWS provider sẽ gửi lên AWS cho resource đó. Trong bài này, các tag đó đến từ `default_tags`.

Ghi nhớ:

- `terraform.tfvars` chỉ cấp giá trị.
- `locals.tf` chỉ gom giá trị thành một nhóm dễ dùng lại.
- `providers.tf` nói với AWS provider: hãy tự gắn nhóm tag này cho resource.
- `main.tf` tập trung mô tả resource, không phải lặp lại tag ở từng resource.
- Resource nào của AWS không hỗ trợ tag thì sẽ không có tag, dù provider có `default_tags`.

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

Lưu ý: `yes` ở đây là câu trả lời tại dòng `Enter a value:` của Terraform, không phải lệnh PowerShell chạy riêng. Nếu Terraform đã kết thúc và bạn gõ `yes` ở prompt `PS ...>`, PowerShell sẽ báo không tìm thấy lệnh `yes`.

Từ thời điểm này, chính bootstrap project cũng dùng remote state.
