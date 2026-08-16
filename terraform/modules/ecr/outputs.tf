output "repository_urls" {
  description = "ECR repository URLs by logical repository name."
  value = {
    for name, repo in aws_ecr_repository.this : name => repo.repository_url
  }
}

output "repository_arns" {
  description = "ECR repository ARNs by logical repository name."
  value = {
    for name, repo in aws_ecr_repository.this : name => repo.arn
  }
}

output "repository_names" {
  description = "ECR repository names by logical repository name."
  value = {
    for name, repo in aws_ecr_repository.this : name => repo.name
  }
}
