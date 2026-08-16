resource "aws_ecr_repository" "this" {
  for_each = var.repositories

  name                 = "${var.name_prefix}/${each.key}"
  image_tag_mutability = each.value.image_tag_mutability

  image_scanning_configuration {
    scan_on_push = each.value.scan_on_push
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  tags = merge(var.tags, {
    Name       = "${var.name_prefix}/${each.key}"
    Repository = each.key
  })
}

resource "aws_ecr_lifecycle_policy" "this" {
  for_each = var.repositories

  repository = aws_ecr_repository.this[each.key].name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Keep only the most recent ${each.value.keep_last_images} images for lab cost control."
        selection = {
          tagStatus   = "any"
          countType   = "imageCountMoreThan"
          countNumber = each.value.keep_last_images
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}
