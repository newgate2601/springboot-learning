locals {
  name = format("%s-%s-eks", var.project, var.environment)
  tags = {
    Project     = var.project
    Environment = var.environment
    Owner       = var.owner
    ManagedBy   = "Terraform"
    Component   = "eks"
    AccountId   = var.account_id
  }
}

data "terraform_remote_state" "network" {
  backend = "s3"
  config = {
    bucket = "newgate2601-terraform-state-150914615641-ap-southeast-1"
    key    = "dev/network/terraform.tfstate"
    region = var.aws_region
  }
}

module "eks" {
  source = "../../../modules/eks"

  name                          = local.name
  vpc_id                        = data.terraform_remote_state.network.outputs.vpc_id
  subnet_ids                    = data.terraform_remote_state.network.outputs.private_app_subnet_ids
  kubernetes_version            = var.kubernetes_version
  operator_role_arn             = var.operator_role_arn
  admin_public_cidrs            = var.admin_public_cidrs
  management_security_group_ids = var.management_security_group_ids
  node                          = var.node
  addon_versions                = var.addon_versions
  tags                          = local.tags
}
