curl.exe --location "http://localhost:8086/api/v1/integrator/transfer-via-soap" `
  --header "Content-Type: application/json" `
  --data '{
    "requestId": "REQ-001",
    "fromAccount": "970400001",
    "toAccount": "970400002",
    "amount": 500000,
    "currency": "VND"
  }'
