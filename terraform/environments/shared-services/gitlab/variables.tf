variable "aws_region" {
  description = "AWS region for GitLab shared-services resources."
  type        = string
}

variable "project" {
  description = "Project name used for naming and tagging."
  type        = string
}

variable "environment" {
  description = "Environment name."
  type        = string
}

variable "owner" {
  description = "Owner of the resources."
  type        = string
}

variable "account_id" {
  description = "AWS account id used for tagging and validation."
  type        = string
}

variable "gitlab_domain_name" {
  description = "DNS name used by GitLab, for example gitlab.example.com."
  type        = string
}

variable "admin_cidr_blocks" {
  description = "CIDR blocks allowed to access GitLab administration paths such as SSH when needed."
  type        = list(string)
}

variable "network_state_bucket" {
  description = "S3 bucket that stores shared-services network Terraform state."
  type        = string
}

variable "network_state_key" {
  description = "S3 key of shared-services network Terraform state."
  type        = string
}

variable "network_state_region" {
  description = "AWS region of the shared-services network Terraform state."
  type        = string
}
