output "gitlab_domain_name" {
  description = "GitLab DNS name."
  value       = var.gitlab_domain_name
}

output "vpc_id" {
  description = "VPC id where GitLab is deployed."
  value       = var.vpc_id
}
