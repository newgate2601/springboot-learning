output "name_prefix" {
  description = "Name prefix used by shared-services network resources."
  value       = local.name_prefix
}

output "vpc_id" {
  description = "Shared-services VPC id."
  value       = module.vpc.vpc_id
}

output "public_subnet_ids" {
  description = "Shared-services public subnet ids."
  value       = module.vpc.public_subnet_ids
}

output "private_app_subnet_ids" {
  description = "Shared-services private application subnet ids."
  value       = module.vpc.private_app_subnet_ids
}

output "isolated_data_subnet_ids" {
  description = "Shared-services isolated data subnet ids."
  value       = module.vpc.isolated_data_subnet_ids
}
