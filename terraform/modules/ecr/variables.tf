variable "name_prefix" {
  description = "Prefix used for ECR repository names."
  type        = string
}

variable "repositories" {
  description = "ECR repositories to create."
  type = map(object({
    image_tag_mutability = optional(string, "IMMUTABLE")
    scan_on_push         = optional(bool, true)
    keep_last_images     = optional(number, 20)
  }))
}

variable "tags" {
  description = "Tags applied to ECR resources."
  type        = map(string)
}
