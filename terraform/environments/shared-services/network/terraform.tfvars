aws_region  = "ap-southeast-1"
project     = "newgate2601"
environment = "shared-services"
owner       = "tony"
account_id  = "150914615641"

vpc_cidr = "10.10.0.0/16"

availability_zones = [
  "ap-southeast-1a",
  "ap-southeast-1b",
  "ap-southeast-1c"
]

public_subnet_cidrs = [
  "10.10.0.0/24",
  "10.10.1.0/24",
  "10.10.2.0/24"
]

private_app_subnet_cidrs = [
  "10.10.10.0/24",
  "10.10.11.0/24",
  "10.10.12.0/24"
]

isolated_data_subnet_cidrs = [
  "10.10.20.0/24",
  "10.10.21.0/24",
  "10.10.22.0/24"
]

nat_gateway_mode     = "single"
enable_vpc_flow_logs = true
