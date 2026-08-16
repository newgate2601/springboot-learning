output "repository_urls" {
  description = "ECR repository URLs by logical repository name."
  value       = module.ecr.repository_urls
}

output "repository_arns" {
  description = "ECR repository ARNs by logical repository name."
  value       = module.ecr.repository_arns
}

output "repository_names" {
  description = "ECR repository names by logical repository name."
  value       = module.ecr.repository_names
}
