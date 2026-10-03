#!/usr/bin/env bash
# Manual checks against a running shop (docker compose up).
# Usage: ./scripts/edge-cases.sh
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin@shop.local}"
ADMIN_PASS="${ADMIN_PASS:-admin1234}"
AUTH=(-u "${ADMIN_USER}:${ADMIN_PASS}")
SKU="EDGE-$(date +%s)"

echo "== 1. Create valid product ${SKU}"
curl -sS "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"sku\":\"${SKU}\",\"name\":\"Edge Lamp\",\"description\":\"desk\",\"category\":\"Home\",\"price\":19.99,\"stock\":5,\"weightKg\":0.8}" \
  "${BASE}/api/admin/products"
echo

echo "== 2. Duplicate SKU (expect 409 SKU_EXISTS)"
curl -sS -w "\nHTTP %{http_code}\n" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"sku\":\"${SKU}\",\"name\":\"Edge Lamp 2\",\"description\":\"desk\",\"category\":\"Home\",\"price\":10,\"stock\":1,\"weightKg\":0.8}" \
  "${BASE}/api/admin/products"
echo

echo "== 3. Negative price (expect 400 INVALID_PRICE)"
curl -sS -w "\nHTTP %{http_code}\n" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"sku\":\"${SKU}-NEG\",\"name\":\"Bad\",\"description\":\"d\",\"category\":\"Cat\",\"price\":-1,\"stock\":1,\"weightKg\":0.1}" \
  "${BASE}/api/admin/products"
echo

echo "== 4. Dollar price (expect 400 INVALID_PRICE)"
curl -sS -w "\nHTTP %{http_code}\n" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"sku\":\"${SKU}-DLR\",\"name\":\"Bad\",\"description\":\"d\",\"category\":\"Cat\",\"price\":\"\$29.99\",\"stock\":1,\"weightKg\":0.1}" \
  "${BASE}/api/admin/products"
echo

echo "== 5. Blank name (expect 400 EMPTY_NAME)"
curl -sS -w "\nHTTP %{http_code}\n" "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"sku\":\"${SKU}-NM\",\"name\":\"  \",\"description\":\"d\",\"category\":\"Cat\",\"price\":1,\"stock\":1,\"weightKg\":0.1}" \
  "${BASE}/api/admin/products"
echo

echo "== 6. Update negative stock (expect 400 NEGATIVE_STOCK)"
curl -sS -w "\nHTTP %{http_code}\n" "${AUTH[@]}" -H 'Content-Type: application/json' -X PUT \
  -d "{\"name\":\"Edge Lamp\",\"description\":\"desk\",\"category\":\"Home\",\"price\":19.99,\"stock\":-3,\"weightKg\":0.8,\"version\":0}" \
  "${BASE}/api/admin/products/${SKU}"
echo

echo "== 7. Delete missing SKU (expect 404)"
curl -sS -w "\nHTTP %{http_code}\n" "${AUTH[@]}" -X DELETE \
  "${BASE}/api/admin/products/NO-SUCH-SKU"
echo

echo "== 8. Shopper cannot create (expect 401/403 — sign up first or this 401)"
curl -sS -w "\nHTTP %{http_code}\n" -H 'Content-Type: application/json' \
  -d "{\"sku\":\"${SKU}-SH\",\"name\":\"Nope\",\"description\":\"d\",\"category\":\"Cat\",\"price\":1,\"stock\":1,\"weightKg\":0.1}" \
  "${BASE}/api/admin/products"
echo

echo "== 9. CSV with SKU / Product Name aliases"
TMP="$(mktemp /tmp/gila-XXXX.csv)"
printf 'Product Name,SKU,description,category,price,stock,weight\nAlias Hat,%s-CSV,warm,Caps,8.50,4,0.2\n' "${SKU}" > "${TMP}"
curl -sS "${AUTH[@]}" -F "file=@${TMP};type=text/csv" "${BASE}/api/admin/imports"
echo
rm -f "${TMP}"

echo "== 10. Wrong column names (Spanish) — job FAILED_HEADER, no products"
TMP="$(mktemp /tmp/gila-XXXX.csv)"
printf 'producto,codigo,detalle,grupo,costo,piezas,kilos\nSilla,S-1,madera,Casa,10,2,1\n' > "${TMP}"
curl -sS "${AUTH[@]}" -F "file=@${TMP};type=text/csv" "${BASE}/api/admin/imports"
echo
rm -f "${TMP}"

echo "== 11. Fake .xlsx (expect 400 UNSUPPORTED_FILE)"
TMP="$(mktemp /tmp/gila-XXXX.xlsx)"
printf 'PK\003\004not-a-real-workbook' > "${TMP}"
curl -sS -w "\nHTTP %{http_code}\n" "${AUTH[@]}" -F "file=@${TMP};type=application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" \
  "${BASE}/api/admin/imports"
echo
rm -f "${TMP}"

echo "== 12. Get ${SKU}"
curl -sS "${BASE}/api/products/${SKU}"
echo

echo
echo "UI: log in as ${ADMIN_USER} / ${ADMIN_PASS}"
echo "  Add product: /admin (form) — try price -1 (browser min=0) then DevTools fetch with price -1"
echo "  Edit/delete: /admin/products → a row → Save / Delete"
echo "  Import: /admin/imports — fixtures CSV, then the Spanish-header CSV (FAILED_HEADER)"
echo "  Excel: save the catalog as .xlsx and drop it — 400, export as CSV instead"
echo "  Sold SKU: buy one unit as a shopper, then Delete that SKU as admin — 409"
