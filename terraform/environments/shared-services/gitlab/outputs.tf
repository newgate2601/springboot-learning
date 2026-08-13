output "name_prefix" {
  description = "Name prefix used by GitLab resources."
  value       = local.name_prefix
}

output "gitlab_domain_name" {
  description = "GitLab DNS name."
  value       = module.gitlab.gitlab_domain_name
}

output "vpc_id" {
  description = "VPC id used by GitLab."
  value       = module.gitlab.vpc_id
}
