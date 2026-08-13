variable "name_prefix" {
  description = "Name prefix used by GitLab resources."
  type        = string
}

variable "gitlab_domain_name" {
  description = "DNS name used by GitLab."
  type        = string
}

variable "vpc_id" {
  description = "VPC id where GitLab is deployed."
  type        = string
}

variable "public_subnet_ids" {
  description = "Public subnet ids used by the GitLab load balancer."
  type        = list(string)
}

variable "private_app_subnet_ids" {
  description = "Private application subnet ids used by GitLab application and Gitaly."
  type        = list(string)
}

variable "isolated_data_subnet_ids" {
  description = "Isolated data subnet ids used by RDS and ElastiCache."
  type        = list(string)
}

variable "admin_cidr_blocks" {
  description = "CIDR blocks allowed to access administration endpoints."
  type        = list(string)
}

variable "tags" {
  description = "Tags applied to GitLab resources."
  type        = map(string)
}
