variable "name" {
  type = string
}
variable "vpc_id" {
  type = string
}
variable "subnet_ids" {
  type = list(string)
  validation {
    condition     = length(distinct(var.subnet_ids)) >= 2
    error_message = "Use private application subnets in at least two AZs."
  }
}
variable "kubernetes_version" {
  type = string
  validation {
    condition     = can(regex("^1[.][0-9]+$", var.kubernetes_version))
    error_message = "Set a Kubernetes minor version such as 1.35; verify standard support in AWS."
  }
}
variable "operator_role_arn" {
  type = string
  validation {
    condition     = can(regex("^arn:aws:iam::[0-9]{12}:role/.+", var.operator_role_arn))
    error_message = "Use a permanent IAM role ARN, not an STS session ARN."
  }
}
variable "admin_public_cidrs" {
  type    = list(string)
  default = []
  validation {
    condition = alltrue([
      for cidr in var.admin_public_cidrs :
      can(cidrnetmask(cidr)) && endswith(cidr, "/32")
    ])
    error_message = "This baseline accepts only individual IPv4 /32 management addresses."
  }
}
variable "management_security_group_ids" {
  type    = set(string)
  default = []
}
variable "node" {
  type = object({
    instance_type = string
    desired_size  = number
    min_size      = number
    max_size      = number
    ami_release   = string
    disk_size     = number
  })
  validation {
    condition     = var.node.disk_size >= 20 && var.node.disk_size == floor(var.node.disk_size)
    error_message = "Use an integer root volume size of at least 20 GiB for this AL2023 baseline."
  }
  validation {
    condition     = can(regex("^[0-9]+[.][0-9]+[.][0-9]+-[0-9]{8}$", var.node.ami_release))
    error_message = "Set the AL2023 release_version returned by SSM, not an AMI ID or placeholder."
  }
  validation {
    condition = (
      var.node.min_size >= 1 &&
      alltrue([for size in [var.node.min_size, var.node.desired_size, var.node.max_size] : size == floor(size)]) &&
      var.node.min_size <= var.node.desired_size &&
      var.node.desired_size <= var.node.max_size
    )
    error_message = "Require integer sizes with 1 <= min_size <= desired_size <= max_size."
  }
}
variable "addon_versions" {
  type = object({
    vpc_cni    = string
    kube_proxy = string
    coredns    = string
  })
  validation {
    condition     = alltrue([for version in values(var.addon_versions) : can(regex("^v[0-9]+[.][0-9]+[.][0-9]+-eksbuild[.][0-9]+$", version))])
    error_message = "Pin all three add-ons to full versions returned by describe-addon-versions."
  }
}
variable "tags" {
  type = map(string)
}
