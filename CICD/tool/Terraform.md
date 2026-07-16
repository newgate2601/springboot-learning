# Terraform từ cơ bản tới triển khai thực tế

## 1. Terraform là gì?

Terraform là một công cụ dùng để tạo, thay đổi và quản lý hạ tầng bằng code. Hạ tầng ở đây có thể là server, database, network, Kubernetes cluster, load balancer, security group, DNS record, IAM role, S3 bucket, VPC, subnet, firewall rule và rất nhiều thứ khác.

Nói theo kiểu dễ hiểu: thay vì bạn vào giao diện AWS, GCP, Azure rồi bấm từng nút để tạo hạ tầng, bạn viết một bộ file mô tả hạ tầng mình muốn. Terraform đọc các file đó, so sánh với hạ tầng thật đang tồn tại, rồi tự tính xem cần tạo gì, sửa gì, xóa gì để hạ tầng thật khớp với thứ bạn đã khai báo.

Ví dụ thay vì bấm tay:

- Tạo VPC.
- Tạo subnet public.
- Tạo subnet private.
- Tạo security group.
- Tạo EC2.
- Tạo RDS.
- Gắn rule mạng.
- Đặt tag.

Bạn viết code kiểu:

```hcl
resource "aws_instance" "app" {
  ami           = "ami-1234567890abcdef0"
  instance_type = "t3.micro"

  tags = {
    Name = "springboot-app-dev"
  }
}
```

Sau đó chạy:

```bash
terraform init
terraform plan
terraform apply
```

Terraform sẽ gọi API của cloud provider để tạo tài nguyên thật.

Điểm quan trọng: Terraform không phải là ngôn ngữ lập trình để viết logic ứng dụng. Terraform là công cụ khai báo hạ tầng mong muốn.

Bạn không viết kiểu:

```text
Đầu tiên tạo VPC
Sau đó tạo subnet
Sau đó tạo EC2
Sau đó gắn security group
```

Bạn viết kiểu:

```text
Tôi muốn có một VPC như thế này
Tôi muốn có hai subnet như thế này
Tôi muốn có một EC2 như thế này
Tôi muốn security group có rule như thế này
```

Terraform tự hiểu quan hệ giữa các tài nguyên và tự sắp xếp thứ tự thực hiện phù hợp.

## 2. Vì sao cần Terraform?

Khi hệ thống còn nhỏ, bạn có thể tạo hạ tầng bằng tay trên giao diện cloud. Cách đó ban đầu rất nhanh, rất trực quan, rất dễ cảm giác là "mình đang làm được việc". Nhưng càng về sau, vấn đề bắt đầu lộ ra.

Ví dụ bạn có một môi trường dev. Bạn bấm tay tạo:

- 1 VPC.
- 2 subnet.
- 1 EC2.
- 1 RDS.
- 1 security group.
- 1 load balancer.

Sau đó công ty cần thêm môi trường staging giống dev nhưng mạnh hơn một chút. Bạn lại bấm tay lần nữa. Rồi cần production. Bạn lại bấm tay lần nữa. Một ngày nào đó có lỗi xảy ra và bạn phải trả lời:

- Dev và staging có giống nhau không?
- Security group production đang mở port gì?
- Ai đã sửa subnet route table tuần trước?
- Nếu xóa nhầm RDS thì khôi phục cấu hình bằng cách nào?
- Nếu cần dựng lại toàn bộ hạ tầng ở region khác thì mất bao lâu?
- Nếu nhân sự nghỉ việc thì người mới có biết hạ tầng được tạo thế nào không?

Terraform giải quyết các vấn đề này bằng cách đưa hạ tầng vào code.

Lợi ích lớn:

- Hạ tầng có thể review như source code.
- Hạ tầng có lịch sử thay đổi trong Git.
- Có thể tạo lại môi trường giống nhau nhiều lần.
- Giảm thao tác bấm tay dễ sai.
- Có thể chạy trong CI/CD.
- Dễ chuẩn hóa naming, tagging, network, security.
- Dễ biết trước Terraform định thay đổi gì trước khi apply.
- Dễ rollback bằng cách quay lại version code cũ, trong giới hạn phù hợp.

Một câu dễ nhớ:

```text
Nếu hạ tầng quan trọng, đừng để nó chỉ tồn tại trong trí nhớ của một người hoặc trong vài cú click trên giao diện cloud.
```

## 3. Terraform thuộc nhóm Infrastructure as Code

Infrastructure as Code, thường viết tắt là IaC, nghĩa là quản lý hạ tầng bằng code.

Trước IaC, hạ tầng thường được quản lý bằng:

- Tài liệu hướng dẫn.
- Checklist thao tác.
- Script rời rạc.
- Bấm tay trên console.
- Kiến thức nằm trong đầu một vài người.

Vấn đề là các cách này dễ lệch nhau. Tài liệu có thể cũ. Người thao tác có thể quên một bước. Script có thể chỉ chạy được trên máy của một người. Console cloud thì thay đổi liên tục và khó review.

Với IaC, trạng thái mong muốn của hạ tầng nằm trong repository:

```text
Git repository
  -> Terraform code
  -> Review pull request
  -> terraform plan
  -> terraform apply
  -> Hạ tầng thật trên cloud
```

Terraform là một trong những công cụ IaC phổ biến nhất. Ngoài Terraform còn có các công cụ khác như:

| Công cụ | Đặc điểm |
|---|---|
| Terraform | Đa cloud, cộng đồng lớn, dùng HCL, mạnh về quản lý tài nguyên cloud |
| OpenTofu | Fork mã nguồn mở từ Terraform, tương thích cao với Terraform |
| AWS CloudFormation | Công cụ IaC riêng của AWS |
| AWS CDK | Viết hạ tầng bằng ngôn ngữ lập trình rồi sinh CloudFormation |
| Pulumi | Viết hạ tầng bằng TypeScript, Python, Go, C# |
| Ansible | Mạnh về cấu hình máy chủ và automation thủ tục |

Terraform đặc biệt phù hợp khi bạn muốn quản lý tài nguyên cloud theo kiểu khai báo, có plan trước khi thay đổi, có state để theo dõi tài nguyên.

## 4. Terraform khác gì Ansible, Docker, Kubernetes?

Người mới rất dễ bị rối vì DevOps có quá nhiều công cụ. Terraform, Ansible, Docker, Kubernetes đều liên quan tới triển khai hệ thống, nhưng vai trò khác nhau.

| Công cụ | Dùng để làm gì? | Ví dụ |
|---|---|---|
| Terraform | Tạo và quản lý hạ tầng | Tạo VPC, EC2, RDS, EKS, IAM, S3 |
| Ansible | Cấu hình máy chủ và tự động hóa thao tác | Cài Nginx, sửa config Linux, restart service |
| Docker | Đóng gói và chạy ứng dụng trong container | Build image Spring Boot, chạy container |
| Kubernetes | Điều phối container trên cluster | Chạy 5 replica của service, rolling update |

Một cách hình dung:

```text
Terraform tạo "mảnh đất và nhà xưởng"
Ansible sắp xếp "máy móc bên trong nhà xưởng"
Docker đóng gói "sản phẩm thành container"
Kubernetes vận hành "nhiều container trong nhà xưởng lớn"
```

Ví dụ với một hệ thống Spring Boot trên AWS:

```text
Terraform
  -> tạo VPC
  -> tạo subnet
  -> tạo EKS cluster
  -> tạo RDS PostgreSQL
  -> tạo IAM role
  -> tạo S3 bucket

Docker
  -> build image cho Spring Boot app

Kubernetes
  -> chạy app trong EKS
  -> scale replica
  -> expose service

CI/CD
  -> build, test, push image
  -> deploy image mới
```

Terraform không thay thế Kubernetes. Terraform có thể tạo Kubernetes cluster. Sau đó Kubernetes quản lý workload bên trong cluster.

Terraform cũng không thay thế Docker. Terraform không dùng để đóng gói app thành image. Docker làm việc đó tốt hơn.

## 5. Tư duy quan trọng nhất: desired state

Terraform hoạt động theo tư duy "desired state", nghĩa là trạng thái mong muốn.

Bạn không nói với Terraform từng bước nhỏ cần làm. Bạn mô tả kết quả cuối cùng bạn muốn.

Ví dụ:

```hcl
resource "aws_s3_bucket" "logs" {
  bucket = "my-company-dev-logs"
}
```

Ý nghĩa là:

```text
Tôi muốn có một S3 bucket tên my-company-dev-logs.
```

Nếu bucket chưa tồn tại, Terraform tạo bucket.

Nếu bucket đã tồn tại trong state và cấu hình không đổi, Terraform không làm gì.

Nếu bạn sửa code, ví dụ thêm tag:

```hcl
resource "aws_s3_bucket" "logs" {
  bucket = "my-company-dev-logs"

  tags = {
    Environment = "dev"
    Owner       = "platform-team"
  }
}
```

Terraform sẽ tính toán rằng bucket cần được cập nhật tag.

Nếu bạn xóa resource khỏi code, Terraform hiểu rằng tài nguyên đó không còn nằm trong trạng thái mong muốn nữa và có thể lên kế hoạch xóa tài nguyên thật.

Đây là điểm rất mạnh nhưng cũng rất nguy hiểm. Xóa code Terraform không chỉ là xóa chữ trong file. Nó có thể dẫn tới xóa hạ tầng thật khi apply.

## 6. Các khái niệm chính trong Terraform

Một project Terraform thường xoay quanh các khái niệm sau:

```text
Terraform Project
  Provider
  Resource
  Data Source
  Variable
  Output
  State
  Module
  Backend
  Workspace
```

Nếu mới học, bạn chưa cần hiểu sâu tất cả ngay từ đầu. Nhưng cần biết mỗi thứ dùng để làm gì.

| Khái niệm | Ý nghĩa ngắn gọn |
|---|---|
| Provider | Plugin giúp Terraform nói chuyện với một nền tảng như AWS, Azure, GCP, Kubernetes |
| Resource | Tài nguyên Terraform quản lý và có thể tạo/sửa/xóa |
| Data source | Dữ liệu chỉ đọc lấy từ bên ngoài |
| Variable | Biến đầu vào để cấu hình linh hoạt |
| Output | Giá trị đầu ra sau khi apply |
| State | File Terraform dùng để nhớ tài nguyên thật đang quản lý |
| Module | Gói Terraform code có thể tái sử dụng |
| Backend | Nơi lưu state |
| Workspace | Cách tách nhiều state trong cùng một cấu hình, dùng cẩn thận |

Các phần sau sẽ đi từng khái niệm bằng ví dụ dễ hiểu.

## 7. File Terraform có đuôi `.tf`

Terraform code thường nằm trong các file có đuôi `.tf`. Ngôn ngữ khai báo chính là HCL, viết tắt của HashiCorp Configuration Language.

Ví dụ cấu trúc đơn giản:

```text
terraform/
  main.tf
  providers.tf
  variables.tf
  outputs.tf
  versions.tf
```

Terraform không bắt buộc bạn phải đặt tên file như trên. Bạn có thể viết tất cả vào `main.tf`, Terraform vẫn đọc được. Nhưng khi project lớn hơn, tách file giúp dễ đọc hơn.

Ý nghĩa thường gặp:

| File | Vai trò |
|---|---|
| `versions.tf` | Khai báo version Terraform và provider |
| `providers.tf` | Cấu hình provider như AWS region |
| `main.tf` | Khai báo resource chính |
| `variables.tf` | Khai báo biến đầu vào |
| `outputs.tf` | Khai báo output |
| `terraform.tfvars` | Gán giá trị cho biến |

Ví dụ rất nhỏ:

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

provider "aws" {
  region = "ap-southeast-1"
}

resource "aws_s3_bucket" "demo" {
  bucket = "my-company-demo-bucket-001"
}
```

Đoạn trên nói rằng:

- Project cần Terraform version từ `1.6.0` trở lên.
- Project dùng AWS provider.
- AWS provider dùng region `ap-southeast-1`, tức Singapore.
- Terraform cần quản lý một S3 bucket tên `my-company-demo-bucket-001`.

## 8. Provider là gì?

Provider là cầu nối giữa Terraform và hệ thống bên ngoài.

Terraform bản thân nó không tự biết cách tạo EC2, tạo RDS, tạo Kubernetes namespace hay tạo DNS record Cloudflare. Terraform cần provider để biết gọi API nào, gửi dữ liệu gì, đọc trạng thái ra sao.

Ví dụ:

| Provider | Dùng để quản lý |
|---|---|
| `hashicorp/aws` | AWS |
| `hashicorp/azurerm` | Microsoft Azure |
| `hashicorp/google` | Google Cloud |
| `hashicorp/kubernetes` | Kubernetes object |
| `hashicorp/helm` | Helm release |
| `cloudflare/cloudflare` | Cloudflare DNS, rules |
| `gitlabhq/gitlab` | GitLab resource |

Khai báo provider AWS:

```hcl
terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "ap-southeast-1"
}
```

Provider thường cần credential. Với AWS, Terraform có thể lấy credential từ:

- Environment variable như `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`.
- AWS profile trong `~/.aws/credentials`.
- IAM role gắn vào EC2 hoặc runner.
- OIDC trong CI/CD.

Không nên hard-code access key vào file `.tf`.

Ví dụ không nên:

```hcl
provider "aws" {
  region     = "ap-southeast-1"
  access_key = "AKIA..."
  secret_key = "..."
}
```

Lý do: file Terraform thường được commit lên Git. Nếu secret vào Git thì rất dễ lộ.

## 9. Resource là gì?

Resource là tài nguyên mà Terraform quản lý. Đây là phần bạn sẽ viết nhiều nhất.

Cú pháp chung:

```hcl
resource "loai_resource" "ten_noi_bo" {
  cau_hinh = "gia_tri"
}
```

Ví dụ:

```hcl
resource "aws_security_group" "app" {
  name        = "app-sg"
  description = "Security group for app"
  vpc_id      = aws_vpc.main.id
}
```

Trong đó:

- `aws_security_group` là loại resource.
- `app` là tên nội bộ trong Terraform.
- `name`, `description`, `vpc_id` là cấu hình của resource.

Tên đầy đủ trong Terraform là:

```text
aws_security_group.app
```

Tên nội bộ `app` không nhất thiết là tên tài nguyên thật trên AWS. Nó là tên để Terraform và người đọc code tham chiếu.

Ví dụ:

```hcl
resource "aws_s3_bucket" "logs" {
  bucket = "my-company-prod-logs"
}
```

Ở đây:

- Tên Terraform: `aws_s3_bucket.logs`.
- Tên bucket thật trên AWS: `my-company-prod-logs`.

Khi đặt tên resource nội bộ, nên đặt rõ nghĩa:

```hcl
resource "aws_vpc" "main" {}
resource "aws_subnet" "public_a" {}
resource "aws_subnet" "private_a" {}
resource "aws_security_group" "app" {}
resource "aws_db_instance" "postgres" {}
```

Không nên đặt tên mơ hồ:

```hcl
resource "aws_vpc" "x" {}
resource "aws_subnet" "test1" {}
resource "aws_security_group" "abc" {}
```

Code Terraform là tài liệu sống của hạ tầng. Tên rõ ràng giúp người sau đỡ khổ.

## 10. Data source là gì?

Resource là thứ Terraform quản lý. Data source là thứ Terraform chỉ đọc.

Ví dụ bạn muốn lấy AMI Ubuntu mới nhất từ AWS để tạo EC2. Bạn không tạo AMI đó. Bạn chỉ tìm và đọc thông tin.

```hcl
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"]

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }
}

resource "aws_instance" "app" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = "t3.micro"
}
```

Ở đây:

- `data "aws_ami" "ubuntu"` chỉ tìm AMI.
- `resource "aws_instance" "app"` tạo EC2.
- EC2 dùng `data.aws_ami.ubuntu.id` làm AMI ID.

Một cách nhớ:

```text
resource = Terraform sở hữu và quản lý vòng đời
data     = Terraform chỉ hỏi thông tin để dùng
```

Data source thường dùng để lấy:

- AMI có sẵn.
- VPC có sẵn.
- Subnet có sẵn.
- IAM policy có sẵn.
- Caller identity hiện tại.
- Secret từ hệ thống quản lý secret.

## 11. Variable là gì?

Variable là biến đầu vào giúp code Terraform linh hoạt hơn.

Nếu không dùng variable, bạn dễ viết cứng mọi thứ:

```hcl
resource "aws_instance" "app" {
  instance_type = "t3.micro"
}
```

Khi muốn dev dùng `t3.micro`, staging dùng `t3.small`, production dùng `t3.medium`, bạn phải sửa code trực tiếp. Cách tốt hơn là dùng variable:

```hcl
variable "instance_type" {
  description = "EC2 instance type for application server"
  type        = string
  default     = "t3.micro"
}

resource "aws_instance" "app" {
  instance_type = var.instance_type
}
```

Gán giá trị qua file `terraform.tfvars`:

```hcl
instance_type = "t3.small"
```

Hoặc qua command:

```bash
terraform apply -var="instance_type=t3.small"
```

Các kiểu dữ liệu thường gặp:

```hcl
variable "environment" {
  type = string
}

variable "enable_backup" {
  type = bool
}

variable "replica_count" {
  type = number
}

variable "allowed_ports" {
  type = list(number)
}

variable "tags" {
  type = map(string)
}
```

Variable nên có:

- `description` để người đọc hiểu biến dùng làm gì.
- `type` để Terraform kiểm tra dữ liệu.
- `default` nếu có giá trị mặc định hợp lý.
- `validation` nếu cần giới hạn giá trị.

Ví dụ validation:

```hcl
variable "environment" {
  description = "Deployment environment"
  type        = string

  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "environment must be one of: dev, staging, prod."
  }
}
```

Validation giúp bắt lỗi sớm. Nếu ai đó nhập `productionn` sai chính tả, Terraform báo lỗi trước khi tạo tài nguyên sai tên.

## 12. Output là gì?

Output là giá trị Terraform in ra sau khi apply. Nó hữu ích khi bạn muốn biết thông tin vừa tạo ra.

Ví dụ tạo EC2 xong muốn biết public IP:

```hcl
output "app_public_ip" {
  description = "Public IP address of the application server"
  value       = aws_instance.app.public_ip
}
```

Sau `terraform apply`, Terraform có thể in:

```text
app_public_ip = "13.215.10.20"
```

Output thường dùng cho:

- IP của server.
- DNS name của load balancer.
- ARN của IAM role.
- Endpoint của database.
- Cluster name.
- VPC ID.
- Subnet IDs.

Ví dụ:

```hcl
output "vpc_id" {
  value = aws_vpc.main.id
}

output "private_subnet_ids" {
  value = [
    aws_subnet.private_a.id,
    aws_subnet.private_b.id
  ]
}
```

Lưu ý: không nên output secret nếu không cần. Output có thể xuất hiện trong terminal, log CI/CD hoặc state.

Nếu buộc phải output dữ liệu nhạy cảm, đánh dấu `sensitive = true`:

```hcl
output "database_password" {
  value     = random_password.db.result
  sensitive = true
}
```

Nhưng tốt hơn là thiết kế để secret nằm trong secret manager, không truyền lung tung qua output.

## 13. State là gì?

State là một trong những khái niệm quan trọng nhất của Terraform.

Terraform cần biết:

- Resource nào trong code tương ứng với tài nguyên thật nào trên cloud?
- Lần trước Terraform đã tạo những gì?
- ID thật của resource là gì?
- Thuộc tính hiện tại Terraform biết là gì?

Terraform lưu thông tin đó trong state.

Mặc định, state nằm trong file local:

```text
terraform.tfstate
```

Ví dụ bạn có code:

```hcl
resource "aws_s3_bucket" "logs" {
  bucket = "my-company-dev-logs"
}
```

Sau khi apply, state sẽ nhớ rằng:

```text
aws_s3_bucket.logs -> bucket thật my-company-dev-logs
```

Lần sau bạn chạy `terraform plan`, Terraform dùng state để so sánh:

```text
Code hiện tại
  vs
State Terraform đang nhớ
  vs
Hạ tầng thật đọc từ provider
```

Từ đó Terraform tính ra cần thêm, sửa, xóa gì.

State rất quan trọng vì nếu mất state, Terraform có thể không biết tài nguyên nào đang thuộc quyền quản lý của nó. Hạ tầng thật có thể vẫn còn, nhưng Terraform mất bản đồ liên kết.

State cũng nhạy cảm vì có thể chứa dữ liệu quan trọng:

- Resource ID.
- Endpoint.
- ARN.
- Một số secret hoặc password, tùy resource.

Vì vậy:

- Không nên commit `terraform.tfstate` lên Git.
- Nên dùng remote backend cho team.
- Nên bật lock state để tránh nhiều người apply cùng lúc.
- Nên phân quyền chặt nơi lưu state.

## 14. Backend là gì?

Backend là nơi Terraform lưu state.

Mặc định là local backend, tức lưu state trên máy bạn. Cách này chỉ phù hợp khi học hoặc demo nhỏ.

Khi làm team, state nên đặt ở nơi dùng chung, ví dụ:

- AWS S3.
- Terraform Cloud.
- Azure Storage Account.
- Google Cloud Storage.
- Consul.

Ví dụ backend S3:

```hcl
terraform {
  backend "s3" {
    bucket         = "my-company-terraform-state"
    key            = "dev/network/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "terraform-locks"
    encrypt        = true
  }
}
```

Ý nghĩa:

- State được lưu trong S3 bucket `my-company-terraform-state`.
- File state nằm ở key `dev/network/terraform.tfstate`.
- DynamoDB table dùng để lock, tránh hai người apply cùng lúc.
- State được mã hóa.

Tại sao cần lock?

Giả sử hai người cùng chạy `terraform apply` trên cùng một state:

```text
Người A đang thêm subnet
Người B đang sửa route table
```

Nếu không lock, state có thể bị ghi đè hoặc hạ tầng bị thay đổi lẫn lộn. Lock giống như tấm biển "đang sửa, vui lòng chờ". Một người apply xong, người khác mới được apply tiếp.

## 15. Terraform workflow cơ bản

Workflow cơ bản thường là:

```text
Viết code
  -> terraform fmt
  -> terraform init
  -> terraform validate
  -> terraform plan
  -> terraform apply
```

### 15.1. `terraform fmt`

Lệnh này format code Terraform cho gọn và thống nhất.

```bash
terraform fmt
```

Nó giống việc format code Java hoặc JavaScript. Không thay đổi logic, chỉ chỉnh trình bày.

Nên chạy trước khi commit.

### 15.2. `terraform init`

Lệnh này khởi tạo project Terraform:

```bash
terraform init
```

Nó làm các việc như:

- Tải provider.
- Khởi tạo backend.
- Tạo thư mục `.terraform`.
- Tạo hoặc cập nhật file lock provider `.terraform.lock.hcl`.

Bạn cần chạy `init` khi:

- Clone project Terraform lần đầu.
- Thêm provider mới.
- Đổi backend.
- Đổi module source.

### 15.3. `terraform validate`

Lệnh này kiểm tra cấu hình Terraform có hợp lệ về cú pháp và cấu trúc không:

```bash
terraform validate
```

Nó giúp bắt lỗi kiểu:

- Sai cú pháp HCL.
- Tham chiếu biến không tồn tại.
- Resource viết thiếu thuộc tính bắt buộc.

Validate không đảm bảo apply sẽ thành công 100%, vì nhiều lỗi chỉ provider hoặc cloud API mới biết khi plan/apply. Nhưng validate vẫn rất nên chạy.

### 15.4. `terraform plan`

Đây là lệnh cực kỳ quan trọng:

```bash
terraform plan
```

Plan cho bạn xem Terraform định làm gì trước khi nó làm thật.

Ví dụ:

```text
Plan: 2 to add, 1 to change, 0 to destroy.
```

Nghĩa là Terraform định:

- Tạo mới 2 resource.
- Sửa 1 resource.
- Không xóa resource nào.

Khi đi làm thật, đừng xem `plan` như thủ tục cho có. Hãy đọc kỹ, nhất là các dòng có dấu:

| Ký hiệu | Ý nghĩa |
|---|---|
| `+` | Tạo mới |
| `~` | Cập nhật |
| `-` | Xóa |
| `-/+` | Xóa rồi tạo lại |

Dấu `-/+` rất đáng chú ý. Nó nghĩa là resource không thể sửa tại chỗ, Terraform phải destroy rồi create lại. Với database, load balancer, cluster, đây có thể là thay đổi rất nguy hiểm.

### 15.5. `terraform apply`

Lệnh này thực thi thay đổi thật:

```bash
terraform apply
```

Terraform sẽ hiển thị plan và hỏi xác nhận:

```text
Do you want to perform these actions?
  Enter a value: yes
```

Bạn nhập `yes`, Terraform bắt đầu gọi API để tạo/sửa/xóa tài nguyên.

Trong CI/CD, có thể dùng plan file:

```bash
terraform plan -out=tfplan
terraform apply tfplan
```

Cách này giúp đảm bảo thứ được apply chính là plan đã review.

### 15.6. `terraform destroy`

Lệnh này xóa toàn bộ tài nguyên trong state hiện tại:

```bash
terraform destroy
```

Đây là lệnh nguy hiểm. Dùng tốt cho môi trường lab, demo, temporary environment. Với production, phải cực kỳ cẩn thận.

Trước khi destroy, luôn đọc plan. Nếu thấy database, bucket, cluster production chuẩn bị bị xóa, dừng lại ngay.

## 16. Đọc `terraform plan` như người tỉnh táo

Người mới thường chạy `plan`, thấy dài quá rồi bỏ qua. Đây là thói quen nguy hiểm. Terraform plan là bản hợp đồng giữa bạn và Terraform trước khi thay đổi hạ tầng.

Ví dụ plan tạo mới:

```text
+ resource "aws_s3_bucket" "logs" {
    bucket = "my-company-dev-logs"
  }
```

Dấu `+` nghĩa là Terraform sẽ tạo bucket.

Ví dụ plan sửa tag:

```text
~ tags = {
    "Environment" = "dev"
  + "Owner"       = "platform-team"
  }
```

Dấu `~` nghĩa là cập nhật. Dấu `+` bên trong nghĩa là thêm tag `Owner`.

Ví dụ đáng sợ hơn:

```text
-/+ resource "aws_db_instance" "postgres" {
      identifier = "prod-postgres"
    ~ storage_type = "gp2" -> "gp3"
  }
```

`-/+` nghĩa là Terraform sẽ xóa resource cũ rồi tạo resource mới. Nếu đây là database production, bạn phải hiểu rất rõ tác động trước khi apply.

Khi đọc plan, hãy tự hỏi:

- Có resource nào bị destroy không?
- Có resource nào bị replace không?
- Thay đổi có đúng môi trường không?
- Có đang apply nhầm workspace/backend không?
- Có đang đổi tên resource khiến Terraform tưởng là xóa cũ tạo mới không?
- Có resource nhạy cảm như database, bucket, IAM, network bị ảnh hưởng không?

Một người dùng Terraform tốt không phải là người gõ lệnh nhanh. Là người đọc plan cẩn thận.

## 17. Dependency giữa các resource

Terraform có thể tự hiểu dependency thông qua tham chiếu.

Ví dụ:

```hcl
resource "aws_vpc" "main" {
  cidr_block = "10.0.0.0/16"
}

resource "aws_subnet" "public_a" {
  vpc_id     = aws_vpc.main.id
  cidr_block = "10.0.1.0/24"
}
```

Vì subnet dùng `aws_vpc.main.id`, Terraform hiểu rằng:

```text
Phải tạo VPC trước
Sau đó mới tạo subnet
```

Bạn không cần viết thứ tự thủ công.

Terraform xây một dependency graph. Graph này giúp Terraform:

- Tạo resource theo thứ tự đúng.
- Chạy song song những resource không phụ thuộc nhau.
- Xóa resource theo thứ tự ngược phù hợp.

Trong một số ít trường hợp, dependency không thể hiện qua tham chiếu. Khi đó có thể dùng `depends_on`.

```hcl
resource "aws_instance" "app" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = "t3.micro"

  depends_on = [
    aws_iam_role_policy_attachment.app
  ]
}
```

Không nên lạm dụng `depends_on`. Nếu có thể tạo dependency bằng tham chiếu thật, hãy dùng tham chiếu thật. Code sẽ rõ nghĩa hơn.

## 18. `count` và `for_each`

Khi muốn tạo nhiều resource giống nhau, bạn không nên copy paste quá nhiều.

Ví dụ muốn tạo 3 subnet. Cách dở:

```hcl
resource "aws_subnet" "subnet_1" {}
resource "aws_subnet" "subnet_2" {}
resource "aws_subnet" "subnet_3" {}
```

Terraform hỗ trợ `count` và `for_each`.

### 18.1. `count`

`count` phù hợp khi bạn chỉ cần số lượng.

```hcl
resource "aws_instance" "worker" {
  count = 3

  ami           = data.aws_ami.ubuntu.id
  instance_type = "t3.micro"

  tags = {
    Name = "worker-${count.index}"
  }
}
```

`count.index` bắt đầu từ `0`, nên sẽ có:

```text
worker-0
worker-1
worker-2
```

Nhược điểm của `count`: resource được nhận diện bằng index. Nếu bạn xóa phần tử ở giữa danh sách, index có thể dịch chuyển, làm Terraform muốn thay đổi nhiều resource hơn dự kiến.

### 18.2. `for_each`

`for_each` phù hợp hơn khi mỗi phần tử có tên rõ ràng.

```hcl
variable "subnets" {
  type = map(object({
    cidr_block        = string
    availability_zone = string
  }))
}

resource "aws_subnet" "private" {
  for_each = var.subnets

  vpc_id            = aws_vpc.main.id
  cidr_block        = each.value.cidr_block
  availability_zone = each.value.availability_zone

  tags = {
    Name = "private-${each.key}"
  }
}
```

Giá trị biến:

```hcl
subnets = {
  a = {
    cidr_block        = "10.0.1.0/24"
    availability_zone = "ap-southeast-1a"
  }
  b = {
    cidr_block        = "10.0.2.0/24"
    availability_zone = "ap-southeast-1b"
  }
}
```

Resource sẽ có địa chỉ:

```text
aws_subnet.private["a"]
aws_subnet.private["b"]
```

`for_each` thường an toàn hơn `count` cho hạ tầng thật vì key rõ ràng và ít bị xáo trộn.

## 19. Module là gì?

Module là cách đóng gói Terraform code để tái sử dụng.

Thật ra mọi project Terraform đều có một module gốc, gọi là root module. Khi bạn tách code ra thư mục riêng và gọi lại, đó là child module.

Ví dụ cấu trúc:

```text
terraform/
  environments/
    dev/
      main.tf
      terraform.tfvars
    prod/
      main.tf
      terraform.tfvars
  modules/
    vpc/
      main.tf
      variables.tf
      outputs.tf
    ec2-app/
      main.tf
      variables.tf
      outputs.tf
```

Gọi module:

```hcl
module "vpc" {
  source = "../../modules/vpc"

  environment = "dev"
  vpc_cidr    = "10.0.0.0/16"
}
```

Module giống như một bộ Lego đã đóng gói. Thay vì mỗi lần tạo VPC lại viết từ đầu 200 dòng, bạn tạo module `vpc` một lần, sau đó dev/staging/prod gọi lại với tham số khác nhau.

Module tốt nên:

- Có mục đích rõ.
- Có input vừa đủ.
- Có output cần thiết.
- Không ôm quá nhiều trách nhiệm.
- Có README nếu dùng cho team.
- Pin version nếu lấy từ remote source.

Module dở thường là module "siêu nhân" làm tất cả mọi thứ:

```text
module "everything" {
  source = "../modules/everything"
}
```

Module kiểu này ban đầu có vẻ tiện, nhưng về sau rất khó sửa, khó test, khó tái sử dụng.

## 20. Cấu trúc thư mục Terraform nên như thế nào?

Không có một cấu trúc duy nhất đúng cho mọi công ty. Nhưng với người mới, có thể bắt đầu bằng cấu trúc dễ hiểu:

```text
infra/
  modules/
    network/
    database/
    eks/
    app/
  environments/
    dev/
      backend.tf
      providers.tf
      main.tf
      variables.tf
      terraform.tfvars
    staging/
      backend.tf
      providers.tf
      main.tf
      variables.tf
      terraform.tfvars
    prod/
      backend.tf
      providers.tf
      main.tf
      variables.tf
      terraform.tfvars
```

Ý tưởng:

- `modules/` chứa code tái sử dụng.
- `environments/dev` chứa cấu hình riêng cho dev.
- `environments/staging` chứa cấu hình riêng cho staging.
- `environments/prod` chứa cấu hình riêng cho prod.

Mỗi môi trường nên có state riêng. Không nên để dev và prod dùng chung state.

Ví dụ state key:

```text
dev/network/terraform.tfstate
staging/network/terraform.tfstate
prod/network/terraform.tfstate
```

Tách state giúp giảm rủi ro. Nếu apply dev lỗi, không ảnh hưởng trực tiếp state prod.

Với hệ thống lớn, còn có thể tách theo layer:

```text
infra/
  environments/
    prod/
      01-network/
      02-security/
      03-database/
      04-eks/
      05-app-platform/
```

Tách layer giúp plan nhỏ hơn, blast radius thấp hơn. Nhưng nếu tách quá vụn, dependency và vận hành sẽ phức tạp hơn. Hãy bắt đầu đơn giản, rồi tách khi có lý do thật.

## 21. Terraform trong CI/CD

Terraform rất hợp để chạy trong CI/CD, nhưng cần thiết kế cẩn thận vì apply là thay đổi hạ tầng thật.

Workflow thường gặp:

```text
Pull Request
  -> terraform fmt -check
  -> terraform validate
  -> terraform plan
  -> comment plan vào PR
  -> review

Merge vào main
  -> terraform plan
  -> manual approval
  -> terraform apply
```

Với production, không nên để bất kỳ commit nào cũng tự apply thẳng nếu team chưa đủ kiểm soát. Thường cần:

- Review code.
- Review plan.
- Approval thủ công.
- Giới hạn ai được approve.
- Audit log.

Ví dụ GitHub Actions ý tưởng:

```yaml
name: Terraform

on:
  pull_request:
    paths:
      - "infra/**"
  push:
    branches:
      - main
    paths:
      - "infra/**"

jobs:
  plan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: hashicorp/setup-terraform@v3
      - run: terraform init
        working-directory: infra/environments/dev
      - run: terraform fmt -check
        working-directory: infra/environments/dev
      - run: terraform validate
        working-directory: infra/environments/dev
      - run: terraform plan
        working-directory: infra/environments/dev
```

Trong thực tế, bạn cần thêm credential an toàn. Với cloud provider hiện đại, nên dùng OIDC thay vì lưu access key dài hạn trong secret CI.

Nguyên tắc:

- Pull request chỉ plan, không apply.
- Apply cần chạy từ nhánh tin cậy.
- Production cần approval.
- CI token chỉ có quyền cần thiết.
- State backend phải có lock.
- Plan log không được lộ secret.

## 22. Terraform và GitOps

GitOps là tư duy lấy Git làm nguồn sự thật cho hệ thống. Với Terraform, điều này nghĩa là:

```text
Muốn đổi hạ tầng
  -> sửa Terraform code
  -> tạo pull request
  -> review
  -> xem plan
  -> merge
  -> apply qua pipeline
```

Không nên thay đổi hạ tầng production bằng cách bấm tay trên console rồi quên cập nhật Terraform. Nếu làm vậy, Terraform code và hạ tầng thật sẽ lệch nhau. Sự lệch này gọi là drift.

Ví dụ drift:

- Terraform code nói security group chỉ mở port `443`.
- Ai đó vào AWS console mở thêm port `22` từ `0.0.0.0/0`.
- Hạ tầng thật đã khác code.

Lần sau chạy `terraform plan`, Terraform có thể phát hiện và muốn đưa security group về đúng code.

Drift không phải lúc nào cũng xấu. Có khi người ta sửa khẩn cấp để cứu production. Nhưng sau đó cần cập nhật lại code hoặc revert thay đổi tay. Nếu không, hệ thống mất tính kiểm soát.

## 23. Import tài nguyên có sẵn

Nhiều công ty đã có hạ tầng tạo bằng tay trước khi dùng Terraform. Vậy có phải xóa hết tạo lại không? Không nhất thiết.

Terraform có thể import tài nguyên có sẵn vào state.

Ví dụ import S3 bucket:

```bash
terraform import aws_s3_bucket.logs my-company-dev-logs
```

Nhưng import không tự viết code đầy đủ cho bạn theo cách hoàn hảo. Bạn vẫn cần viết resource trong `.tf`:

```hcl
resource "aws_s3_bucket" "logs" {
  bucket = "my-company-dev-logs"
}
```

Sau import, chạy:

```bash
terraform plan
```

Nếu plan vẫn muốn sửa nhiều thứ, nghĩa là code chưa khớp với tài nguyên thật. Bạn cần bổ sung cấu hình cho tới khi plan sạch hoặc chỉ còn thay đổi bạn chấp nhận.

Import nên làm cẩn thận:

- Import từng nhóm nhỏ.
- Backup state trước nếu cần.
- Kiểm tra plan sau mỗi lần.
- Không import production hàng loạt khi chưa hiểu resource.

## 24. Lifecycle và bảo vệ tài nguyên quan trọng

Terraform có block `lifecycle` để điều chỉnh cách quản lý resource.

Ví dụ chống xóa nhầm:

```hcl
resource "aws_db_instance" "postgres" {
  identifier = "prod-postgres"

  lifecycle {
    prevent_destroy = true
  }
}
```

Nếu plan muốn destroy database này, Terraform sẽ báo lỗi và không cho apply.

`prevent_destroy` rất hữu ích cho:

- Database production.
- S3 bucket chứa dữ liệu quan trọng.
- DNS zone.
- KMS key.
- EKS cluster production.

Nhưng đừng xem nó là áo giáp tuyệt đối. Người có quyền vẫn có thể sửa code bỏ `prevent_destroy` rồi apply. Nó là lớp bảo vệ tránh tai nạn, không thay thế quy trình review và phân quyền.

Một lifecycle khác là `ignore_changes`:

```hcl
resource "aws_instance" "app" {
  ami           = data.aws_ami.ubuntu.id
  instance_type = "t3.micro"

  lifecycle {
    ignore_changes = [
      tags["LastPatchedAt"]
    ]
  }
}
```

`ignore_changes` nói Terraform bỏ qua thay đổi ở một số thuộc tính. Dùng khi có hệ thống khác cập nhật thuộc tính đó.

Cẩn thận: lạm dụng `ignore_changes` có thể che giấu drift thật.

## 25. Naming và tagging

Naming và tagging nghe nhỏ nhưng cực kỳ quan trọng khi hạ tầng lớn.

Tên tài nguyên nên trả lời được:

- Thuộc hệ thống nào?
- Thuộc môi trường nào?
- Thuộc region nào nếu cần?
- Vai trò là gì?

Ví dụ:

```text
learning-dev-app-sg
learning-prod-postgres
learning-staging-private-subnet-a
```

Tag nên có các thông tin cơ bản:

```hcl
locals {
  common_tags = {
    Project     = "springboot-learning"
    Environment = var.environment
    ManagedBy   = "terraform"
    Owner       = "platform-team"
  }
}
```

Dùng lại:

```hcl
resource "aws_s3_bucket" "logs" {
  bucket = "springboot-learning-${var.environment}-logs"

  tags = local.common_tags
}
```

Tag giúp:

- Tìm tài nguyên dễ hơn.
- Phân bổ cost.
- Audit ownership.
- Biết tài nguyên nào do Terraform quản lý.
- Viết policy tự động.

Một tag rất nên có:

```text
ManagedBy = terraform
```

Nó giúp người khác biết không nên sửa tay tùy tiện.

## 26. Secret trong Terraform

Secret là vùng dễ mắc lỗi.

Không nên đặt secret trực tiếp trong file `.tf` hoặc `.tfvars` rồi commit lên Git.

Ví dụ không nên:

```hcl
db_password = "SuperSecretPassword123"
```

Vấn đề:

- Secret vào Git history rất khó xóa sạch.
- Secret có thể xuất hiện trong plan.
- Secret có thể nằm trong state.
- Secret có thể xuất hiện trong log CI.

Cách tốt hơn:

- Dùng secret manager như AWS Secrets Manager, SSM Parameter Store, Vault.
- Truyền secret qua biến môi trường trong CI, nhưng vẫn cần hiểu state có lưu không.
- Dùng IAM role/OIDC thay vì access key dài hạn.
- Không output secret.
- Mã hóa và phân quyền chặt remote state.

Ví dụ đọc secret từ AWS Secrets Manager:

```hcl
data "aws_secretsmanager_secret_version" "db_password" {
  secret_id = "prod/postgres/password"
}
```

Nhưng hãy nhớ: nếu bạn đưa secret vào thuộc tính resource, nó vẫn có thể đi vào state. Terraform state phải được bảo vệ như dữ liệu nhạy cảm.

## 27. Các lỗi người mới hay gặp

### 27.1. Commit state lên Git

Không nên commit:

```text
terraform.tfstate
terraform.tfstate.backup
.terraform/
*.tfplan
```

Nên có `.gitignore`:

```gitignore
.terraform/
*.tfstate
*.tfstate.*
*.tfplan
crash.log
override.tf
override.tf.json
*_override.tf
*_override.tf.json
```

File `.terraform.lock.hcl` thì thường nên commit để pin provider version.

### 27.2. Apply khi chưa đọc plan

Đây là lỗi rất phổ biến. Plan dài không có nghĩa là được bỏ qua. Ít nhất phải kiểm tra:

- Có destroy không?
- Có replace không?
- Có đúng môi trường không?
- Có đúng account cloud không?
- Có đúng region không?

### 27.3. Sửa tay trên cloud console

Sửa tay làm code và hạ tầng lệch nhau. Nếu bắt buộc sửa tay để xử lý khẩn cấp, hãy ghi lại và đồng bộ lại Terraform sau đó.

### 27.4. Dùng chung state cho nhiều môi trường

Dev, staging, prod nên có state riêng. Dùng chung state làm rủi ro lan rộng và plan khó đọc.

### 27.5. Đổi tên resource nội bộ tùy tiện

Ví dụ đổi:

```hcl
resource "aws_s3_bucket" "logs" {}
```

thành:

```hcl
resource "aws_s3_bucket" "app_logs" {}
```

Terraform có thể hiểu là xóa `logs` và tạo `app_logs`, dù bucket thật vẫn cùng ý nghĩa.

Nếu cần đổi tên resource trong Terraform mà không muốn destroy/create, dùng `moved` block:

```hcl
moved {
  from = aws_s3_bucket.logs
  to   = aws_s3_bucket.app_logs
}
```

### 27.6. Không pin provider version

Nếu không pin version, lần sau `terraform init` có thể tải provider mới hơn và hành vi thay đổi.

Nên khai báo:

```hcl
required_providers {
  aws = {
    source  = "hashicorp/aws"
    version = "~> 5.0"
  }
}
```

## 28. Ví dụ nhỏ: tạo S3 bucket

Đây là ví dụ tối giản để hiểu luồng.

Cấu trúc:

```text
terraform-s3-demo/
  versions.tf
  providers.tf
  main.tf
  variables.tf
  outputs.tf
```

`versions.tf`:

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

`providers.tf`:

```hcl
provider "aws" {
  region = var.aws_region
}
```

`variables.tf`:

```hcl
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-southeast-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}
```

`main.tf`:

```hcl
resource "aws_s3_bucket" "logs" {
  bucket = "springboot-learning-${var.environment}-logs-demo"

  tags = {
    Project     = "springboot-learning"
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}
```

`outputs.tf`:

```hcl
output "bucket_name" {
  value = aws_s3_bucket.logs.bucket
}
```

Chạy:

```bash
terraform init
terraform fmt
terraform validate
terraform plan
terraform apply
```

Sau khi học xong, nếu đây chỉ là lab, nhớ destroy:

```bash
terraform destroy
```

## 29. Ví dụ thực tế hơn: network đơn giản trên AWS

Một VPC cơ bản thường có:

```text
VPC
  Public Subnet
    Route tới Internet Gateway
  Private Subnet
    Không public trực tiếp ra internet
```

Ví dụ rút gọn:

```hcl
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "learning-dev-vpc"
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "learning-dev-igw"
  }
}

resource "aws_subnet" "public_a" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "ap-southeast-1a"
  map_public_ip_on_launch = true

  tags = {
    Name = "learning-dev-public-a"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "learning-dev-public-rt"
  }
}

resource "aws_route_table_association" "public_a" {
  subnet_id      = aws_subnet.public_a.id
  route_table_id = aws_route_table.public.id
}
```

Đọc theo nghĩa con người:

- Tạo một VPC dải mạng `10.0.0.0/16`.
- Bật DNS cho VPC.
- Tạo Internet Gateway gắn vào VPC.
- Tạo public subnet ở AZ `ap-southeast-1a`.
- Public subnet tự gán public IP khi EC2 được tạo trong đó.
- Tạo route table public.
- Route `0.0.0.0/0` đi ra Internet Gateway.
- Gắn route table vào subnet.

Terraform tự hiểu thứ tự vì các resource tham chiếu nhau.

## 30. Terraform với Spring Boot project

Với một ứng dụng Spring Boot, Terraform thường không quản lý code Java. Terraform quản lý hạ tầng để ứng dụng chạy.

Ví dụ kiến trúc đơn giản:

```text
User
  -> Load Balancer
  -> EC2 hoặc ECS hoặc EKS
  -> Spring Boot container/app
  -> RDS PostgreSQL
  -> S3
  -> CloudWatch Logs
```

Terraform có thể tạo:

- VPC, subnet, route table.
- Security group.
- EC2 hoặc ECS/EKS.
- RDS PostgreSQL.
- IAM role cho app.
- S3 bucket.
- CloudWatch log group.
- Load balancer.
- DNS record.

CI/CD có thể làm:

- Build Spring Boot app.
- Chạy test.
- Build Docker image.
- Push image lên registry.
- Deploy version mới.

Phân chia trách nhiệm:

```text
Terraform
  -> Hạ tầng nền: network, database, cluster, IAM

CI/CD deploy
  -> Phiên bản app: image tag, rollout, config runtime
```

Có thể dùng Terraform để deploy app, nhưng với hệ thống container/Kubernetes, thường nên để công cụ CD như Argo CD, Helm, GitLab CI hoặc GitHub Actions quản lý rollout app. Terraform nên tập trung vào hạ tầng nền.

## 31. Khi nào không nên dùng Terraform?

Terraform mạnh, nhưng không phải búa thần cho mọi cái đinh.

Không nên dùng Terraform cho:

- Logic deploy ứng dụng thay đổi liên tục từng commit nếu đã có công cụ CD phù hợp.
- Cấu hình chi tiết bên trong server kiểu sửa file config, cài package từng bước. Ansible có thể hợp hơn.
- Tác vụ chạy một lần không cần quản lý vòng đời.
- Dữ liệu nghiệp vụ trong database.
- Những thứ thay đổi quá thường xuyên và không phù hợp với state.

Ví dụ:

- Tạo RDS: Terraform hợp.
- Chạy migration database mỗi release: Flyway/Liquibase hợp hơn.
- Tạo EKS cluster: Terraform hợp.
- Deploy mỗi image mới vào Kubernetes: Helm/Argo CD thường hợp hơn.
- Tạo IAM role: Terraform hợp.
- Rotate secret runtime: Secret Manager/Vault workflow hợp hơn.

Terraform tốt nhất khi tài nguyên có vòng đời hạ tầng rõ ràng: tạo, cập nhật, xóa, theo dõi trạng thái.

### 31.1. Vậy vì sao bài AWS EKS vẫn dùng cả Terraform và Argo CD?

Đây là điểm rất dễ nhầm, nên cần nói rõ:

```text
Không nên dùng Terraform để deploy app liên tục
không có nghĩa là
không nên dùng Terraform trong hệ thống có Argo CD.
```

Trong bài thực hành GitLab CI + Argo CD + AWS + EKS, hai công cụ này không giẫm chân nhau. Chúng làm hai lớp khác nhau.

Terraform lo lớp hạ tầng:

- Tạo VPC.
- Tạo subnet.
- Tạo route table.
- Tạo security group.
- Tạo EKS cluster.
- Tạo node group.
- Tạo RDS.
- Tạo MSK.
- Tạo ElastiCache.
- Tạo IAM role.
- Tạo S3 bucket.
- Tạo ECR repository.
- Tạo Secrets Manager secret nếu cần.

Argo CD lo lớp ứng dụng chạy bên trong Kubernetes:

- Đọc GitOps repository.
- Đồng bộ Kubernetes manifest hoặc Helm chart xuống EKS.
- Tạo Deployment, Service, Ingress, ConfigMap, ExternalSecret.
- Cập nhật image tag hoặc image digest.
- Rollout phiên bản mới.
- Rollback app khi manifest quay về version cũ.
- Giữ trạng thái trong cluster khớp với GitOps repo.

Nhìn theo tầng:

```text
AWS account
  Terraform quản lý:
    VPC
    Subnet
    IAM
    EKS
    RDS
    MSK
    ElastiCache
    ECR

EKS cluster
  Argo CD quản lý:
    Namespace
    Deployment
    Service
    Ingress
    Helm release / Kustomize manifest
    Image version của app
```

Ví dụ khi cần dựng môi trường dev lần đầu:

```text
Terraform apply
  -> tạo network
  -> tạo EKS
  -> tạo database
  -> tạo cache
  -> tạo registry
  -> cài hoặc chuẩn bị Argo CD

Argo CD sync
  -> deploy Spring Boot app
  -> deploy config app
  -> trỏ app tới database/cache đã có
```

Ví dụ khi developer sửa code Spring Boot và build ra image mới:

```text
GitLab CI
  -> test
  -> build Docker image
  -> push image lên ECR
  -> cập nhật GitOps repo với image digest mới

Argo CD
  -> thấy GitOps repo thay đổi
  -> sync xuống EKS
  -> Kubernetes rolling update app
```

Trong luồng này, Terraform không cần chạy mỗi lần có commit ứng dụng. Vì hạ tầng không đổi. Chỉ phiên bản app đổi. Đây là việc của Argo CD.

Ngược lại, nếu cần tạo thêm RDS, đổi node group, thêm subnet, sửa IAM role, tạo thêm ECR repository hoặc dựng EKS staging, đó là việc của Terraform.

Một câu dễ nhớ:

```text
Terraform dựng sân khấu.
Argo CD đưa diễn viên lên sân khấu và đổi phiên bản vở diễn.
```

Vì vậy bài AWS recommend cả hai là đúng. Ý đúng phải hiểu là:

- Dùng Terraform cho hạ tầng AWS/EKS.
- Dùng Argo CD cho deployment ứng dụng vào Kubernetes.
- Không dùng GitLab CI bắn `kubectl apply` thẳng vào production.
- Không dùng Terraform để thay image app cho từng commit nếu đã có Argo CD/GitOps làm việc đó tốt hơn.

## 32. Best practices cho người mới

Nếu mới học Terraform, hãy giữ các nguyên tắc này:

- Bắt đầu với môi trường lab, không học trực tiếp trên production.
- Luôn chạy `terraform fmt`.
- Luôn chạy `terraform validate`.
- Luôn đọc `terraform plan`.
- Không commit state.
- Không commit secret.
- Pin Terraform và provider version.
- Dùng remote backend khi làm team.
- Tách state theo môi trường.
- Đặt tên resource rõ nghĩa.
- Dùng tag `ManagedBy = "terraform"`.
- Dùng variable có type và description.
- Dùng output vừa đủ.
- Không lạm dụng module quá sớm.
- Không copy paste quá nhiều khi có thể dùng `for_each`.
- Cẩn thận với `destroy` và `replace`.
- Thêm `prevent_destroy` cho tài nguyên quan trọng.

Một nguyên tắc thực tế:

```text
Nếu bạn chưa hiểu plan, đừng apply.
```

Câu này nghe đơn giản nhưng cứu được rất nhiều sự cố.

## 33. Checklist trước khi apply

Trước khi chạy `terraform apply`, tự hỏi:

- Tôi đang đứng đúng thư mục môi trường chưa?
- Tôi đang dùng đúng AWS account/cloud account chưa?
- Tôi đang dùng đúng region chưa?
- Backend state có đúng không?
- Plan có resource nào bị destroy không?
- Plan có resource nào bị replace không?
- Có database, bucket, IAM, network quan trọng bị ảnh hưởng không?
- Có secret nào xuất hiện trong log không?
- Thay đổi này đã được review chưa?
- Với production, có approval chưa?

Có thể dùng lệnh kiểm tra AWS identity:

```bash
aws sts get-caller-identity
```

Với Terraform, có thể xem workspace:

```bash
terraform workspace show
```

Nhưng đừng phụ thuộc hoàn toàn vào workspace nếu team chưa quen. Tách thư mục và backend theo môi trường thường dễ hiểu hơn cho người mới.

## 34. Quy trình học Terraform đề xuất

Nếu bạn là người mới, đừng bắt đầu bằng EKS production, module phức tạp, remote state nhiều layer. Bắt đầu nhỏ hơn.

Lộ trình hợp lý:

1. Hiểu Terraform là gì và vì sao cần IaC.
2. Cài Terraform local.
3. Tạo một resource đơn giản như S3 bucket.
4. Học `init`, `fmt`, `validate`, `plan`, `apply`, `destroy`.
5. Học variable và output.
6. Học state và vì sao không commit state.
7. Tạo VPC/subnet/security group nhỏ.
8. Học remote backend.
9. Học module.
10. Học CI/CD plan/apply.
11. Học import tài nguyên có sẵn.
12. Học drift detection và lifecycle.
13. Sau đó mới đi vào EKS, RDS production, multi-account.

Đừng vội học quá rộng. Terraform cần chắc nền tảng, vì sai lầm có thể tác động hạ tầng thật.

## 35. Một workflow mẫu từ dev tới production

Với team nhỏ, workflow có thể như sau:

```text
Developer tạo branch
  -> sửa Terraform code cho dev
  -> chạy fmt/validate/plan local
  -> tạo pull request
  -> CI chạy fmt/validate/plan
  -> team review code và plan
  -> merge
  -> pipeline apply dev
  -> kiểm tra dev ổn
  -> tạo PR cho staging/prod hoặc promote cấu hình
  -> plan production
  -> approval
  -> apply production
```

Điểm quan trọng:

- Production thay đổi chậm hơn dev.
- Plan production phải được đọc kỹ hơn.
- Apply production nên có người chịu trách nhiệm rõ.
- Mọi thay đổi nên đi qua Git.

Nếu có sự cố khẩn cấp và phải sửa tay, sau đó cần tạo PR để đồng bộ lại Terraform code. Đừng để "sửa tạm" trở thành trạng thái vĩnh viễn.

## 36. Terraform anti-patterns

### 36.1. Một state khổng lồ quản lý tất cả

Nếu toàn bộ công ty nằm trong một state, mỗi lần plan sẽ rất lớn, apply rủi ro cao, lock ảnh hưởng nhiều team.

Nên tách theo môi trường, domain hoặc layer hợp lý.

### 36.2. Module quá trừu tượng

Module có 80 biến, 30 option bật tắt, dùng cho mọi trường hợp thường rất khó hiểu.

Module nên giải quyết một bài toán rõ ràng.

### 36.3. Dùng Terraform để làm mọi thứ

Terraform không nên thay thế mọi công cụ. Hãy dùng đúng công cụ cho đúng lớp:

- Terraform cho hạ tầng.
- CI cho build/test.
- CD cho rollout app.
- Kubernetes cho orchestration container.
- Flyway/Liquibase cho database migration.

### 36.4. Không review plan

Đây là anti-pattern nguy hiểm nhất. Terraform cho bạn cơ hội xem trước thay đổi. Bỏ qua plan là tự bịt mắt trước khi đổi hạ tầng.

### 36.5. Để quyền cloud quá rộng

Nếu CI Terraform có quyền admin toàn account, một lỗi nhỏ có thể thành sự cố lớn. Nên cấp quyền theo phạm vi cần thiết, nhất là production.

## 37. Tóm tắt

Terraform là công cụ quản lý hạ tầng bằng code. Bạn viết file `.tf` để mô tả trạng thái mong muốn, Terraform dùng provider để nói chuyện với cloud, dùng state để nhớ tài nguyên đang quản lý, dùng plan để cho bạn xem trước thay đổi, và dùng apply để thực hiện thay đổi thật.

Những ý cần nhớ nhất:

- Terraform không phải công cụ bấm nút tự động đơn giản, nó là hệ thống quản lý trạng thái hạ tầng.
- Code Terraform là nguồn sự thật của hạ tầng nếu bạn vận hành đúng.
- State rất quan trọng và phải được bảo vệ.
- `terraform plan` là bước đọc kỹ, không phải bước lướt qua.
- Không commit secret, không commit state.
- Tách môi trường rõ ràng.
- Dùng module khi có nhu cầu tái sử dụng thật.
- Production cần review, approval và quyền hạn chặt.

Một câu chốt cho người mới:

```text
Terraform giúp bạn biến hạ tầng từ những cú click khó nhớ thành code có thể đọc, review, chạy lại và kiểm soát.
```

Khi dùng đúng, Terraform làm hạ tầng bớt mơ hồ hơn rất nhiều. Khi dùng ẩu, nó cũng có thể phá hạ tầng rất nhanh. Vì vậy hãy đi chậm lúc đầu, đọc plan cẩn thận, bắt đầu từ môi trường nhỏ, rồi nâng dần lên hệ thống thật.
