output "name_prefix" {
  description = "Prefix used for VPC resource names."
  value       = var.name_prefix
}

output "vpc_id" {
  description = "VPC id."
  value       = aws_vpc.this.id
}

output "public_subnet_ids" {
  description = "Public subnet ids."
  value       = aws_subnet.public[*].id
}

output "private_app_subnet_ids" {
  description = "Private application subnet ids."
  value       = aws_subnet.private_app[*].id
}

output "isolated_data_subnet_ids" {
  description = "Isolated data subnet ids."
  value       = aws_subnet.isolated_data[*].id
}

output "public_route_table_id" {
  description = "Public route table id."
  value       = aws_route_table.public.id
}

output "private_app_route_table_ids" {
  description = "Private application route table ids."
  value       = aws_route_table.private_app[*].id
}

output "isolated_data_route_table_ids" {
  description = "Isolated data route table ids."
  value       = aws_route_table.isolated_data[*].id
}

output "vpc_endpoint_security_group_id" {
  description = "Security group id for VPC interface endpoints."
  value       = aws_security_group.vpc_endpoint.id
}
