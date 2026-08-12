variable "aws_region" {
  description = "AWS region for shared-services network resources."
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
  description = "CIDR block for the shared-services VPC."
  type        = string
}

variable "availability_zones" {
  description = "Availability zones used by the shared-services network."
  type        = list(string)
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets."
  type        = list(string)
}

variable "private_app_subnet_cidrs" {
  description = "CIDR blocks for private application subnets."
  type        = list(string)
}

variable "isolated_data_subnet_cidrs" {
  description = "CIDR blocks for isolated data subnets."
  type        = list(string)
}

variable "nat_gateway_mode" {
  description = "NAT Gateway mode. Use single for lab cost saving, one_per_az for higher availability."
  type        = string
  default     = "single"

  validation {
    condition     = contains(["single", "one_per_az"], var.nat_gateway_mode)
    error_message = "nat_gateway_mode must be single or one_per_az."
  }
}

variable "enable_vpc_flow_logs" {
  description = "Whether to enable VPC Flow Logs."
  type        = bool
  default     = true
}
