terraform {
  backend "s3" {
    bucket         = "newgate2601-terraform-state-150914615641-ap-southeast-1"
    key            = "bootstrap/backend/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "terraform-state-lock"
    encrypt        = true
    kms_key_id = "arn:aws:kms:ap-southeast-1:150914615641:key/ac4b9b4e-7e2c-4008-806f-9b4eae719f96"
  }
}
