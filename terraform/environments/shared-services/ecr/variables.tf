variable "aws_region" {
  description = "AWS region for shared ECR resources."
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
  description = "AWS account id used for tagging."
  type        = string
}

variable "repositories" {
  description = "ECR repositories to create for Spring Boot services."
  type = map(object({
    image_tag_mutability = optional(string, "IMMUTABLE")
    scan_on_push         = optional(bool, true)
    keep_last_images     = optional(number, 20)
  }))
}
