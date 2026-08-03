variable "name_prefix" {
  description = "Prefix used for VPC resource names."
  type        = string
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
}

variable "availability_zones" {
  description = "Availability zones used by this VPC."
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
  description = "NAT Gateway mode."
  type        = string
}

variable "enable_vpc_flow_logs" {
  description = "Whether to enable VPC Flow Logs."
  type        = bool
}

variable "tags" {
  description = "Tags applied to VPC resources."
  type        = map(string)
}
