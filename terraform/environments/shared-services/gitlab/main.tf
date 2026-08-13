data "terraform_remote_state" "network" {
  backend = "s3"

  config = {
    bucket = var.network_state_bucket
    key    = var.network_state_key
    region = var.network_state_region
  }
}

module "gitlab" {
  source = "../../../modules/gitlab-self-managed"

  name_prefix              = local.name_prefix
  gitlab_domain_name       = var.gitlab_domain_name
  vpc_id                   = data.terraform_remote_state.network.outputs.vpc_id
  public_subnet_ids        = data.terraform_remote_state.network.outputs.public_subnet_ids
  private_app_subnet_ids   = data.terraform_remote_state.network.outputs.private_app_subnet_ids
  isolated_data_subnet_ids = data.terraform_remote_state.network.outputs.isolated_data_subnet_ids
  admin_cidr_blocks        = var.admin_cidr_blocks
  tags                     = local.common_tags
}
