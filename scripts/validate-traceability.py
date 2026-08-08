from pathlib import Path
import re
import sys

root = Path(__file__).resolve().parents[1]
java = list(root.glob("*/src/**/*.java"))
text = "\n".join(path.read_text(encoding="utf-8") for path in java)
required = {"FR-010", "FR-021", "FR-030", "FR-040", "FR-041", "FR-050", "FR-060", "FR-080", "FR-090"}
found = set(re.findall(r"(?:FR|BR|BRULE|DATA|INT|SEC|OPS|AC)-\d{3}", text))
missing = sorted(required - found)
if missing:
    print("Missing requirement links:", ", ".join(missing))
    sys.exit(1)
print(f"Traceability OK: {len(java)} Java files, {len(found)} requirement IDs")

