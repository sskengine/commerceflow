$ErrorActionPreference = "Stop"
$correlationId = [guid]::NewGuid().ToString()
$headers = @{ "X-Correlation-Id" = $correlationId }

Invoke-RestMethod http://127.0.0.1:28080/api/catalog/products -Headers $headers | Out-Null

$order = @{
  customerId = "11111111-1111-1111-1111-111111111111"
  currency = "USD"
  items = @(@{ productId = "22222222-2222-2222-2222-222222222222"; quantity = 1; unitPrice = 25.00 })
} | ConvertTo-Json -Depth 4

$created = Invoke-RestMethod http://127.0.0.1:28080/api/orders `
  -Method Post -ContentType "application/json" -Headers $headers -Body $order

Write-Host "Smoke test passed. orderId=$($created.orderId) correlationId=$correlationId"
