module "ecr" {
  source = "../../../modules/ecr"

  name_prefix  = local.name_prefix
  repositories = var.repositories
  tags         = local.common_tags
}
