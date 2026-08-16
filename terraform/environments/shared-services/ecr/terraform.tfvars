aws_region  = "ap-southeast-1"
project     = "newgate2601"
environment = "shared-services"
owner       = "tony"
account_id  = "150914615641"

repositories = {
  gateway = {
    image_tag_mutability = "IMMUTABLE"
    scan_on_push         = true
    keep_last_images     = 20
  }
  uaa-service = {
    image_tag_mutability = "IMMUTABLE"
    scan_on_push         = true
    keep_last_images     = 20
  }
  post-service = {
    image_tag_mutability = "IMMUTABLE"
    scan_on_push         = true
    keep_last_images     = 20
  }
  service-registry = {
    image_tag_mutability = "IMMUTABLE"
    scan_on_push         = true
    keep_last_images     = 20
  }
}
