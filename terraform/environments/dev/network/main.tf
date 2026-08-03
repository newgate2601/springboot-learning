module "vpc" {
  source = "../../../modules/vpc"

  name_prefix                = local.name_prefix
  vpc_cidr                   = var.vpc_cidr
  availability_zones         = var.availability_zones
  public_subnet_cidrs        = var.public_subnet_cidrs
  private_app_subnet_cidrs   = var.private_app_subnet_cidrs
  isolated_data_subnet_cidrs = var.isolated_data_subnet_cidrs
  nat_gateway_mode           = var.nat_gateway_mode
  enable_vpc_flow_logs       = var.enable_vpc_flow_logs
  tags                       = local.common_tags
}
