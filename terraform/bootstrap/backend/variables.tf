variable "aws_region" {
  description = "AWS region used for Terraform backend resources."
  type        = string
}

variable "project" {
  description = "Project or platform name used in resource naming."
  type        = string
}

variable "environment" {
  description = "Bootstrap environment name."
  type        = string
  default     = "bootstrap"
}

variable "account_id" {
  description = "AWS account id used to make globally unique names."
  type        = string
}

variable "state_bucket_name" {
  description = "S3 bucket name for Terraform remote state."
  type        = string
}

variable "lock_table_name" {
  description = "DynamoDB table name for Terraform state locking."
  type        = string
  default     = "terraform-state-lock"
}

variable "owner" {
  description = "Owner of these bootstrap resources."
  type        = string
}
