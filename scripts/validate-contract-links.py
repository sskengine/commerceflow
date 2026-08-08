from pathlib import Path
import sys

root = Path(__file__).resolve().parents[1]
order_java = (root / "order-service/src/main/java/com/sskengine/commerceflow/order/OrderApplication.java").read_text(encoding="utf-8")
order_api = (root / "contracts/openapi/order-api-v1.yaml").read_text(encoding="utf-8")

errors = []
if "required: [customerId, currency, items]" not in order_api:
    errors.append("FR-021: OpenAPI no longer requires currency")
if "record CreateOrderRequest" not in order_java or "String currency" not in order_java:
    errors.append("FR-021: Java request no longer contains currency")
if "Idempotency-Key" not in (root / "contracts/openapi/payment-api-v1.yaml").read_text(encoding="utf-8"):
    errors.append("INT-003: payment OpenAPI no longer requires Idempotency-Key")

if errors:
    print("\n".join(errors))
    sys.exit(1)
print("Critical contract links OK: FR-021 and INT-003")
