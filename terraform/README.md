# Terraform Bootstrap - Giải thích cho người mới

Tài liệu này giải thích bộ Terraform trong thư mục `terraform/`: mỗi file dùng để làm gì, các keyword quan trọng nghĩa là gì, và chạy lệnh theo thứ tự nào.

Terraform chỉ đọc các file có đuôi chính như:

```text
.tf
.tfvars
.tf.json
```

File Markdown như `README.md` chỉ là tài liệu, Terraform không chạy file này. Vì vậy đặt file `.md` trong thư mục Terraform không làm `terraform init`, `terraform plan` hay `terraform apply` bị lỗi.

---

## 1. Mục tiêu của thư mục này

Thư mục này tạo phần nền cho Terraform:

```text
S3 bucket      -> lưu terraform.tfstate
DynamoDB table -> khóa state, tránh nhiều người apply cùng lúc
KMS key        -> mã hóa state
```

Hiểu đơn giản:

- Terraform cần một nơi để nhớ đã tạo tài nguyên gì.
- Nơi đó gọi là `state`.
- Nếu để state trên máy cá nhân thì dễ mất, khó dùng chung.
- Vì vậy ta đưa state lên S3.
- DynamoDB giúp khóa state khi Terraform đang chạy.
- KMS giúp mã hóa state.

---

## 2. Cấu trúc thư mục

```text
terraform/
├── bootstrap/
│   └── backend/
│       ├── versions.tf
│       ├── providers.tf
│       ├── variables.tf
│       ├── locals.tf
│       ├── main.tf
│       ├── outputs.tf
│       ├── terraform.tfvars.example
│       ├── README.md
│       └── remote-state/
│           └── backend.tf
├── environments/
│   ├── shared-services/
│   ├── dev/
│   ├── staging/
│   └── production/
└── modules/
```

Ý nghĩa nhanh:

| Đường dẫn | Ý nghĩa |
|---|---|
| `bootstrap/backend` | Terraform nhỏ chạy đầu tiên để tạo backend lưu state. |
| `remote-state/backend.tf` | File backend dùng sau khi S3 bucket đã được tạo. |
| `modules` | Nơi đặt module dùng lại cho VPC, EKS, RDS, ECR sau này. |
| `environments/dev` | Root module cho môi trường dev sau này. |
| `environments/staging` | Chỉ dùng sau khi dev đã đạt nghiệm thu. |
| `environments/production` | Chỉ dùng sau khi staging đã đạt nghiệm thu. |

---

## 3. Vì sao có bootstrap?

Terraform có một vấn đề đặc biệt:

```text
Terraform cần S3 bucket để lưu state
nhưng S3 bucket cũng cần được tạo bởi Terraform
```

Vì vậy ta làm theo 2 nhịp:

```text
Nhịp 1:
  Chạy bootstrap bằng local state
  -> tạo S3 bucket, DynamoDB table, KMS key

Nhịp 2:
  Chuyển state của bootstrap lên S3
  -> từ đó về sau Terraform dùng remote state
```

Đây là cách gọn, dễ hiểu, và vẫn chuẩn hơn việc bấm tay tạo S3/DynamoDB trên Console.

---

## 4. Giải thích từng file

### 4.1. `versions.tf`

File này khai báo Terraform version và provider version.

```hcl
terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}
```

Giải thích:

| Dòng/keyword | Nghĩa dễ hiểu |
|---|---|
| `terraform {}` | Khối cấu hình chung của Terraform. |
| `required_version` | Yêu cầu version Terraform tối thiểu. |
| `>= 1.6.0` | Dùng Terraform từ bản 1.6.0 trở lên. |
| `required_providers` | Khai báo plugin provider cần dùng. |
| `aws` | Provider AWS, dùng để tạo resource trên AWS. |
| `source = "hashicorp/aws"` | Tải AWS provider chính thức từ HashiCorp. |
| `version = "~> 5.0"` | Dùng provider AWS dòng 5.x, tránh tự nhảy sang 6.x. |

Provider là gì?

```text
Terraform core không tự biết cách tạo S3, DynamoDB, EKS.
Provider AWS là plugin dạy Terraform cách nói chuyện với AWS API.
```

### 4.2. `providers.tf`

File này cấu hình provider AWS.

```hcl
provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}
```

Giải thích:

| Dòng/keyword | Nghĩa dễ hiểu |
|---|---|
| `provider "aws"` | Cấu hình provider AWS. |
| `region = var.aws_region` | Region lấy từ biến `aws_region`, ví dụ `ap-southeast-1`. |
| `var.aws_region` | Đọc giá trị biến tên `aws_region`. |
| `default_tags` | Tag mặc định gắn vào resource AWS. |
| `local.common_tags` | Đọc bộ tag dùng chung từ file `locals.tf`. |

Vì sao dùng `var.aws_region` thay vì viết thẳng?

```text
Không hard-code giúp dễ đổi region khi cần.
Dev, staging, production có thể dùng biến khác nhau.
```

### 4.3. `variables.tf`

File này khai báo các biến đầu vào.

Ví dụ:

```hcl
variable "aws_region" {
  description = "AWS region used for Terraform backend resources."
  type        = string
}
```

Giải thích:

| Dòng/keyword | Nghĩa dễ hiểu |
|---|---|
| `variable` | Khai báo một biến. |
| `"aws_region"` | Tên biến. |
| `description` | Mô tả biến dùng để làm gì. |
| `type = string` | Biến này là chuỗi chữ. |

Các biến đang có:

| Biến | Dùng để làm gì |
|---|---|
| `aws_region` | Region AWS, ví dụ `ap-southeast-1`. |
| `project` | Tên project, ví dụ `newgate2601`. |
| `environment` | Tên môi trường, ở đây là `bootstrap`. |
| `account_id` | AWS account id, dùng để đặt tên bucket không bị trùng. |
| `state_bucket_name` | Tên S3 bucket lưu Terraform state. |
| `lock_table_name` | Tên DynamoDB table dùng để lock state. |
| `owner` | Người/team sở hữu tài nguyên. |

### 4.4. `locals.tf`

File này khai báo giá trị dùng chung trong nội bộ Terraform.

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

Giải thích:

| Dòng/keyword | Nghĩa dễ hiểu |
|---|---|
| `locals` | Khai báo giá trị nội bộ, không nhập từ bên ngoài. |
| `common_tags` | Một map chứa tag chung. |
| `Project = var.project` | Lấy project từ biến. |
| `ManagedBy = "Terraform"` | Ghi rõ resource này do Terraform quản lý. |
| `Component = "terraform-backend"` | Cho biết resource thuộc phần backend. |

`variable` khác `local` thế nào?

```text
variable = giá trị đưa từ ngoài vào
local    = giá trị tính/ghép/dùng lại bên trong code
```

### 4.5. `main.tf`

File này tạo resource thật trên AWS.

#### KMS key

```hcl
resource "aws_kms_key" "terraform_state" {
  description             = "KMS key for Terraform remote state encryption"
  deletion_window_in_days = 30
  enable_key_rotation     = true
}
```

Giải thích:

| Dòng/keyword | Nghĩa dễ hiểu |
|---|---|
| `resource` | Khai báo một tài nguyên cần tạo. |
| `aws_kms_key` | Loại tài nguyên: KMS key. |
| `terraform_state` | Tên nội bộ trong Terraform. |
| `description` | Mô tả key dùng để làm gì. |
| `deletion_window_in_days = 30` | Nếu xóa key, AWS chờ 30 ngày mới xóa thật. |
| `enable_key_rotation = true` | Bật tự động xoay key hằng năm. |

Tên đầy đủ trong Terraform là:

```text
aws_kms_key.terraform_state
```

#### KMS alias

```hcl
resource "aws_kms_alias" "terraform_state" {
  name          = "alias/${var.project}/terraform-state"
  target_key_id = aws_kms_key.terraform_state.key_id
}
```

Giải thích:

| Dòng/keyword | Nghĩa dễ hiểu |
|---|---|
| `aws_kms_alias` | Tạo tên dễ nhớ cho KMS key. |
| `"alias/${var.project}/terraform-state"` | Ghép tên alias từ biến project. |
| `${var.project}` | Cú pháp chèn giá trị biến vào chuỗi. |
| `target_key_id` | Alias này trỏ tới KMS key nào. |
| `aws_kms_key.terraform_state.key_id` | Lấy `key_id` từ KMS key vừa tạo. |

#### S3 bucket

```hcl
resource "aws_s3_bucket" "terraform_state" {
  bucket = var.state_bucket_name
}
```

Giải thích:

| Dòng/keyword | Nghĩa dễ hiểu |
|---|---|
| `aws_s3_bucket` | Tạo S3 bucket. |
| `bucket` | Tên bucket thật trên AWS. |
| `var.state_bucket_name` | Lấy tên bucket từ biến. |

S3 bucket name phải duy nhất toàn cầu, nên tên thường có:

```text
project + account id + region
```

#### Bật versioning

```hcl
resource "aws_s3_bucket_versioning" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  versioning_configuration {
    status = "Enabled"
  }
}
```

Giải thích:

| Dòng/keyword | Nghĩa dễ hiểu |
|---|---|
| `aws_s3_bucket_versioning` | Cấu hình versioning cho bucket. |
| `bucket = aws_s3_bucket.terraform_state.id` | Áp dụng cho bucket đã tạo bên trên. |
| `status = "Enabled"` | Bật versioning. |

Versioning giúp giữ các bản cũ của state, rất quan trọng nếu state bị ghi sai.

#### Mã hóa S3 bằng KMS

```hcl
resource "aws_s3_bucket_server_side_encryption_configuration" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = aws_kms_key.terraform_state.arn
      sse_algorithm     = "aws:kms"
    }
  }
}
```

Giải thích:

| Dòng/keyword | Nghĩa dễ hiểu |
|---|---|
| `server_side_encryption` | AWS mã hóa dữ liệu khi lưu trong S3. |
| `kms_master_key_id` | Dùng KMS key nào để mã hóa. |
| `arn` | Định danh đầy đủ của resource trên AWS. |
| `sse_algorithm = "aws:kms"` | Dùng cơ chế mã hóa KMS. |

#### Chặn public access

```hcl
resource "aws_s3_bucket_public_access_block" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
```

Giải thích:

| Dòng | Nghĩa dễ hiểu |
|---|---|
| `block_public_acls` | Chặn ACL public mới. |
| `block_public_policy` | Chặn bucket policy public. |
| `ignore_public_acls` | Bỏ qua ACL public nếu có. |
| `restrict_public_buckets` | Hạn chế bucket nếu policy public. |

Terraform state không bao giờ được public.

#### Bucket ownership

```hcl
resource "aws_s3_bucket_ownership_controls" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}
```

Giải thích:

| Dòng/keyword | Nghĩa dễ hiểu |
|---|---|
| `BucketOwnerEnforced` | Bucket owner luôn sở hữu object trong bucket. |

Điều này giúp tránh rắc rối ACL ownership trong S3.

#### Bucket policy

```hcl
data "aws_iam_policy_document" "terraform_state" {
  ...
}
```

`data` là gì?

```text
resource = tạo resource mới
data     = đọc/tạo dữ liệu để dùng, nhưng không tạo resource AWS trực tiếp
```

Ở đây `aws_iam_policy_document` giúp tạo JSON policy đúng format.

Policy hiện có 3 rule:

| Rule | Tác dụng |
|---|---|
| `DenyInsecureTransport` | Chặn truy cập không dùng HTTPS. |
| `DenyUnEncryptedObjectUploads` | Chặn upload object không khai báo encryption. |
| `DenyIncorrectEncryptionHeader` | Chặn upload object không dùng `aws:kms`. |

Sau đó policy được gắn vào bucket:

```hcl
resource "aws_s3_bucket_policy" "terraform_state" {
  bucket = aws_s3_bucket.terraform_state.id
  policy = data.aws_iam_policy_document.terraform_state.json
}
```

#### DynamoDB lock table

```hcl
resource "aws_dynamodb_table" "terraform_lock" {
  name         = var.lock_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "LockID"

  attribute {
    name = "LockID"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }
}
```

Giải thích:

| Dòng/keyword | Nghĩa dễ hiểu |
|---|---|
| `aws_dynamodb_table` | Tạo DynamoDB table. |
| `name` | Tên table. |
| `PAY_PER_REQUEST` | Trả tiền theo request, phù hợp lab và workload nhỏ. |
| `hash_key = "LockID"` | Khóa chính của table. Terraform backend cần tên này. |
| `type = "S"` | `LockID` là kiểu string. |
| `point_in_time_recovery` | Cho phép khôi phục table về thời điểm trước đó. |

### 4.6. `outputs.tf`

File này in ra giá trị quan trọng sau khi apply.

```hcl
output "kms_key_arn" {
  description = "KMS key ARN for Terraform state encryption."
  value       = aws_kms_key.terraform_state.arn
}
```

Giải thích:

| Keyword | Nghĩa dễ hiểu |
|---|---|
| `output` | Giá trị muốn Terraform in ra sau khi apply. |
| `kms_key_arn` | Tên output. |
| `value` | Giá trị thực tế cần in. |

Sau khi chạy apply, xem output bằng:

```powershell
terraform output
terraform output kms_key_arn
```

### 4.7. `terraform.tfvars.example`

File này là mẫu giá trị biến.

```hcl
aws_region        = "ap-southeast-1"
project           = "newgate2601"
environment       = "bootstrap"
account_id        = "150914615641"
state_bucket_name = "newgate2601-terraform-state-150914615641-ap-southeast-1"
lock_table_name   = "terraform-state-lock"
owner             = "tony"
```

Terraform tự đọc file tên:

```text
terraform.tfvars
```

Vì vậy khi chạy thật, copy:

```powershell
Copy-Item terraform.tfvars.example terraform.tfvars
```

Rồi sửa `terraform.tfvars` nếu cần.

Vì sao `.gitignore` bỏ qua `*.tfvars`?

```text
Vì file tfvars thật có thể chứa thông tin riêng của account hoặc secret.
File example thì được commit để người khác biết cần điền gì.
```

### 4.8. `remote-state/backend.tf`

File này chưa được Terraform đọc ở lần chạy đầu vì nó nằm trong thư mục con `remote-state`.

Mục đích:

```text
Lưu sẵn cấu hình backend chuẩn.
Sau khi S3 bucket đã tạo xong, copy file này lên thư mục bootstrap/backend.
```

Nội dung chính:

```hcl
terraform {
  backend "s3" {
    bucket         = "newgate2601-terraform-state-150914615641-ap-southeast-1"
    key            = "bootstrap/backend/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "terraform-state-lock"
    encrypt        = true
    kms_key_id     = "<replace-with-kms-key-arn-output>"
  }
}
```

Giải thích:

| Dòng/keyword | Nghĩa dễ hiểu |
|---|---|
| `backend "s3"` | Nói Terraform lưu state lên S3. |
| `bucket` | Bucket lưu state. |
| `key` | Đường dẫn file state trong bucket. |
| `region` | Region của bucket. |
| `dynamodb_table` | Table dùng để lock state. |
| `encrypt = true` | Bật mã hóa state. |
| `kms_key_id` | KMS key dùng để mã hóa state. |

Lưu ý:

```text
Không đặt backend.tf ở thư mục bootstrap/backend ngay từ đầu.
Nếu đặt ngay từ đầu, terraform init sẽ tìm S3 bucket khi bucket chưa tồn tại và bị lỗi.
```

---

## 5. Cách chạy từng bước

Đi vào thư mục bootstrap:

```powershell
cd D:\document\springboot-learning\terraform\bootstrap\backend
```

Copy file biến:

```powershell
Copy-Item terraform.tfvars.example terraform.tfvars
```

Kiểm tra/sửa `terraform.tfvars`.

Chạy lần đầu:

```powershell
terraform init
terraform fmt
terraform validate
terraform plan
terraform apply
```

Sau khi apply xong, lấy KMS ARN:

```powershell
terraform output kms_key_arn
```

Sửa file:

```text
remote-state/backend.tf
```

Thay:

```text
<replace-with-kms-key-arn-output>
```

bằng KMS ARN thật.

Copy backend file lên thư mục hiện tại:

```powershell
Copy-Item remote-state\backend.tf backend.tf
```

Chuyển state lên S3:

```powershell
terraform init -migrate-state
```

Khi Terraform hỏi có copy state lên backend mới không, nhập:

```text
yes
```

Lưu ý: `yes` ở đây là câu trả lời tại dòng `Enter a value:` của Terraform, không phải lệnh PowerShell chạy riêng. Nếu Terraform đã kết thúc và bạn gõ `yes` ở prompt `PS ...>`, PowerShell sẽ báo không tìm thấy lệnh `yes`.

Từ lúc này, bootstrap cũng dùng remote state.

---

## 6. Các lệnh Terraform cơ bản

| Lệnh | Dùng để làm gì |
|---|---|
| `terraform init` | Tải provider, chuẩn bị backend. |
| `terraform fmt` | Format file `.tf`. |
| `terraform validate` | Kiểm tra syntax và cấu hình cơ bản. |
| `terraform plan` | Xem Terraform sẽ tạo/sửa/xóa gì. |
| `terraform apply` | Thực sự tạo/sửa/xóa resource. |
| `terraform output` | Xem output sau khi apply. |
| `terraform init -migrate-state` | Chuyển state từ local lên remote backend. |

Nguyên tắc an toàn:

```text
Luôn đọc terraform plan trước khi apply.
Không apply nếu thấy Terraform định xóa resource mà mình không hiểu rõ.
```

---

## 7. Vì sao file Markdown không làm Terraform lỗi?

Terraform chỉ xử lý file cấu hình Terraform.

Trong thư mục làm việc, Terraform quan tâm chủ yếu:

```text
*.tf
*.tfvars
*.tf.json
```

Nó bỏ qua:

```text
README.md
*.txt
*.png
*.drawio
```

Vì vậy có thể để tài liệu ngay cạnh code Terraform để người mới đọc dễ hiểu.

Điều cần tránh là đặt file `.tf` linh tinh trong cùng thư mục root module, vì Terraform sẽ đọc tất cả file `.tf` trong thư mục đó.

Ví dụ:

```text
terraform/bootstrap/backend/
  main.tf        # Terraform đọc
  variables.tf   # Terraform đọc
  README.md      # Terraform bỏ qua
  note.txt       # Terraform bỏ qua
```

---

## 8. Lỗi thường gặp

| Lỗi | Nguyên nhân | Cách xử lý |
|---|---|---|
| `terraform` không được nhận diện | Chưa cài Terraform CLI hoặc chưa thêm vào PATH. | Cài Terraform và mở terminal mới. |
| `No valid credential sources found` | AWS CLI/credential chưa cấu hình. | Chạy `aws configure` hoặc cấu hình profile phù hợp. |
| Bucket name already exists | Tên S3 bucket bị trùng toàn cầu. | Đổi `state_bucket_name` trong `terraform.tfvars`. |
| AccessDenied | IAM user thiếu quyền. | Kiểm tra user lab có quyền tạo KMS, S3, DynamoDB. |
| `terraform init` tìm S3 bucket ngay từ đầu | Đặt `backend.tf` sai chỗ quá sớm. | Chỉ copy `remote-state/backend.tf` thành `backend.tf` sau khi apply bootstrap xong. |
| `terraform init -migrate-state` hỏi migrate | Đây là hành vi đúng. | Nhập `yes` nếu backend đã đúng. |

---

## 9. Ghi nhớ ngắn

```text
Lần đầu:
  bootstrap local state
  -> tạo S3 + DynamoDB + KMS

Sau đó:
  copy backend.tf
  -> terraform init -migrate-state
  -> dùng remote state

Các môi trường sau:
  dev/staging/prod dùng chung module
  chỉ khác backend key và tfvars
```
