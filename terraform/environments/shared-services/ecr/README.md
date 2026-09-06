# Shared Services ECR

Root module này quản lý Amazon ECR repositories dùng chung cho các Spring Boot service.

State backend:

```text
s3://newgate2601-terraform-state-150914615641-ap-southeast-1/shared-services/ecr/terraform.tfstate
```

Repositories mặc định:

```text
gateway
uaa-service
post-service
```

Chạy:

```powershell
cd C:\code\springboot-learning\terraform\environments\shared-services\ecr
terraform init
terraform validate
terraform plan -out=tfplan
```

Chỉ apply sau khi plan chỉ tạo ECR repository và lifecycle policy, không đụng VPC, EKS, RDS, MSK hoặc ElastiCache.
