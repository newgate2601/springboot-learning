terraform {
  backend "s3" {
    bucket         = "newgate2601-terraform-state-150914615641-ap-southeast-1"
    key            = "shared-services/network/terraform.tfstate"
    region         = "ap-southeast-1"
    dynamodb_table = "terraform-state-lock"
    encrypt        = true
    kms_key_id     = "arn:aws:kms:ap-southeast-1:150914615641:key/38aaa237-5b16-4d7e-811c-c634ae35de52"
  }
}
