output "state_bucket_name" {
  description = "S3 bucket name for Terraform remote state."
  value       = aws_s3_bucket.terraform_state.bucket
}

output "lock_table_name" {
  description = "DynamoDB table name for Terraform state locking."
  value       = aws_dynamodb_table.terraform_lock.name
}

output "kms_key_arn" {
  description = "KMS key ARN for Terraform state encryption."
  value       = aws_kms_key.terraform_state.arn
}
