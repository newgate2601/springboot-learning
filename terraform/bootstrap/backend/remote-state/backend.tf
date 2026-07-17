terraform {
  backend "s3" {
    bucket         = "newgate2601-terraform-state-150914615641-ap-southeast-1"
    key            = "bootstrap/backend/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "terraform-state-lock"
    encrypt        = true
    kms_key_id     = "<replace-with-kms-key-arn-output>"
  }
}
