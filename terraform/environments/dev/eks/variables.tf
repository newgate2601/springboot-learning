variable "aws_region" {
  description = "AWS region used by the dev EKS root module."
  type        = string

  validation {
    condition     = var.aws_region == "ap-southeast-1"
    error_message = "The dev EKS lab must run in ap-southeast-1."
  }
}

variable "account_id" {
  description = "AWS account allowed for this root module."
  type        = string

  validation {
    condition     = can(regex("^[0-9]{12}$", var.account_id))
    error_message = "account_id must contain exactly 12 digits."
  }
}

variable "project" {
  description = "Project name used for resource names and tags."
  type        = string

  validation {
    condition     = length(trimspace(var.project)) > 0
    error_message = "project must not be empty."
  }
}

variable "environment" {
  description = "Environment name. This root module only manages dev."
  type        = string

  validation {
    condition     = var.environment == "dev"
    error_message = "This root module is reserved for dev."
  }
}

variable "owner" {
  description = "Owner tag applied to resources."
  type        = string

  validation {
    condition     = length(trimspace(var.owner)) > 0
    error_message = "owner must not be empty."
  }
}

variable "kubernetes_version" {
  description = "Pinned EKS Kubernetes minor version, for example 1.35."
  type        = string

  validation {
    condition     = can(regex("^1[.][0-9]+$", var.kubernetes_version))
    error_message = "kubernetes_version must be a minor version such as 1.35."
  }
}

variable "operator_role_arn" {
  description = "Permanent IAM role ARN granted Kubernetes administrator access."
  type        = string

  validation {
    condition     = can(regex("^arn:aws:iam::[0-9]{12}:role/.+", var.operator_role_arn))
    error_message = "operator_role_arn must be an IAM role ARN, not an STS session ARN."
  }
}

variable "admin_public_cidrs" {
  description = "Individual public IPv4 /32 addresses allowed to reach the public EKS API."
  type        = list(string)
  default     = []

  validation {
    condition = alltrue([
      for cidr in var.admin_public_cidrs :
      can(cidrnetmask(cidr)) && endswith(cidr, "/32")
    ])
    error_message = "admin_public_cidrs only accepts individual IPv4 /32 addresses."
  }
}

variable "management_security_group_ids" {
  description = "Security groups allowed to reach the private EKS API on TCP 443."
  type        = set(string)
  default     = []

  validation {
    condition     = alltrue([for id in var.management_security_group_ids : can(regex("^sg-[0-9a-f]+$", id))])
    error_message = "Each management_security_group_ids value must be a security group id beginning with sg-."
  }
}

variable "node" {
  description = "Managed node group capacity and pinned AL2023 release."
  type = object({
    instance_type = string
    desired_size  = number
    min_size      = number
    max_size      = number
    ami_release   = string
    disk_size     = number
  })

  validation {
    condition     = length(trimspace(var.node.instance_type)) > 0
    error_message = "node.instance_type must not be empty."
  }

  validation {
    condition = (
      var.node.min_size >= 2 &&
      alltrue([for size in [var.node.min_size, var.node.desired_size, var.node.max_size] : size == floor(size)]) &&
      var.node.min_size <= var.node.desired_size &&
      var.node.desired_size <= var.node.max_size
    )
    error_message = "The enterprise-style dev lab requires integer sizes with 2 <= min_size <= desired_size <= max_size."
  }

  validation {
    condition     = var.node.disk_size >= 20 && var.node.disk_size == floor(var.node.disk_size)
    error_message = "node.disk_size must be an integer of at least 20 GiB."
  }

  validation {
    condition     = can(regex("^[0-9]+[.][0-9]+[.][0-9]+-[0-9]{8}$", var.node.ami_release))
    error_message = "node.ami_release must be the pinned AL2023 release_version returned by SSM."
  }
}

variable "addon_versions" {
  description = "Pinned EKS managed add-on versions compatible with the selected Kubernetes version."
  type = object({
    vpc_cni    = string
    kube_proxy = string
    coredns    = string
  })

  validation {
    condition     = alltrue([for version in values(var.addon_versions) : can(regex("^v[0-9]+[.][0-9]+[.][0-9]+-eksbuild[.][0-9]+$", version))])
    error_message = "All add-on versions must use the full vX.Y.Z-eksbuild.N format."
  }
}
