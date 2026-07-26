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

variable "tags" {
  description = "Tags applied to VPC resources."
  type        = map(string)
}
