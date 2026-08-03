output "name_prefix" {
  description = "Name prefix used by dev network resources."
  value       = local.name_prefix
}

output "vpc_id" {
  description = "Dev VPC id."
  value       = module.vpc.vpc_id
}

output "public_subnet_ids" {
  description = "Dev public subnet ids."
  value       = module.vpc.public_subnet_ids
}

output "private_app_subnet_ids" {
  description = "Dev private application subnet ids."
  value       = module.vpc.private_app_subnet_ids
}

output "isolated_data_subnet_ids" {
  description = "Dev isolated data subnet ids."
  value       = module.vpc.isolated_data_subnet_ids
}
