variable "aws_region" {
  description = "AWS region for dev network resources."
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

variable "vpc_cidr" {
  description = "CIDR block for the dev VPC."
  type        = string
}

variable "availability_zones" {
  description = "Availability zones used by the dev network."
  type        = list(string)
}
