locals {
  name_prefix = "${var.project}-${var.environment}-gitlab"

  common_tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "Terraform"
    Owner       = var.owner
    Component   = "gitlab"
    AccountId   = var.account_id
  }
}
