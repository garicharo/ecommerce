# Manual test checklist

Run this after a chunk of changes (catalog, security, CSV, checkout, Docker). Contract: [SPECS.md](SPECS.md). Public run path: [README.md](README.md).

**Usual path:** `docker compose up --build` → browser **http://localhost:8080** (nginx). Do not also run `mvn spring-boot:run` on 8080.

Admin login (not the Postgres password):

```text
admin@shop.local
admin1234
```

The storefront uses a session cookie (`POST /api/auth/login`). `curl -u` still works because HTTP Basic is enabled on the API for these checks.

---

## 0. Start (every session)

- [ ] Docker Desktop is running (`docker info` prints Server, no “Cannot connect”).
- [ ] From repo root: `docker compose up --build -d` then `docker compose ps` → `db`, `api`, `web` healthy.
- [ ] Browser: http://localhost:8080 (Gila Store).
- [ ] If `8080` is already in use, stop the other process.

---

## 1. Always (smoke)

These must keep working. If they fail, stop and fix before more features.

```bash
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/actuator/health
# 200

curl -sS http://localhost:8080/actuator/health
# {"status":"UP"}

curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/products
# 200  (anonymous)

curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/admin/products
# 401  (no credentials)

curl -sS -o /dev/null -w "%{http_code}\n" -u shopper@example.com:wrong \
  -X POST http://localhost:8080/api/admin/products
# 401
```

- [ ] Health 200 and `UP`
- [ ] Catalog GET is public
- [ ] Admin without login is 401
- [ ] Restart API twice: still one admin row (seed does not reset the password)

```bash
docker compose exec db psql -U shop -d shop -c \
  "SELECT email, display_name, role FROM users;"
# admin@shop.local | Admin | ADMIN
```

---

## 2. Catalog — admin write + public read (Day 1)

Use a unique SKU each run if the previous product is still in the DB (`MT-001`, `MT-002`, …).

```bash
# Create
curl -sS -u admin@shop.local:admin1234 \
  -H "Content-Type: application/json" \
  -X POST http://localhost:8080/api/admin/products \
  -d '{
    "sku": "MT-001",
    "name": "Manual Test Shoe",
    "description": "Day 1 smoke product",
    "category": "Shoes",
    "price": "49.99",
    "stock": 5,
    "weightKg": "0.800"
  }'
```

- [ ] POST 200/201, `origin` is `MANUAL`, `version` is `0` (or `0` then first write), `price` is decimal not `49.9900000002`
- [ ] Same POST again → **409**

```bash
curl -sS http://localhost:8080/api/products
# content includes MT-001  (Spring Page: "content", "totalElements")

curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/products/MT-001
# 200

curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/products/NOPE
# 404
```

Update (copy `version` from the GET; first create is usually `0`):

```bash
curl -sS -u admin@shop.local:admin1234 \
  -H "Content-Type: application/json" \
  -X PUT http://localhost:8080/api/admin/products/MT-001 \
  -d '{
    "name": "Manual Test Shoe",
    "description": "updated",
    "category": "Shoes",
    "price": "59.99",
    "stock": 4,
    "weightKg": "0.800",
    "version": 0
  }'
```

- [ ] PUT with current `version` → 200, `stock`/`price` changed, `version` bumped, sku unchanged
- [ ] PUT with stale `version` (e.g. `0` after it became `1`) → **409**
- [ ] PUT does not change sku even if the body sends another sku

```bash
curl -sS -u admin@shop.local:admin1234 \
  -X DELETE http://localhost:8080/api/admin/products/MT-001
# 200 or 204

curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/api/products/MT-001
# 404
```

- [ ] DELETE of a product with no orders → gone
- [ ] Shopper must not create products (when signup exists: session cookie + POST admin → **403**)

---

## 3. Search

```bash
curl -sS "http://localhost:8080/api/products?q=sh"
curl -sS "http://localhost:8080/api/products?q=s"
curl -sS "http://localhost:8080/api/products?category=Shoes&sort=price_asc"
curl -sS http://localhost:8080/api/products/categories
```

- [ ] `q` length 1 is ignored (full list / unfiltered)
- [ ] `q` matches name, description, category, **sku** (case-insensitive)
- [ ] `category` exact AND with `q`
- [ ] `sort=name_asc` default; `price_asc` / `price_desc`
- [ ] `size` default 20, max 50
- [ ] XSS in a name is stored and returned as JSON text (not executed)

---

## 4. CSV import

Use `fixtures/` / `Code Challenge E-Commerce.csv`. Admin only.

- [ ] POST import → **202** + `jobId`; poll until `COMPLETED` / `FAILED`
- [ ] Extra CSV columns ignored; missing required header → job failed with header error
- [ ] Duplicate SKU in file (`RS-001`, `BS-021`): last row wins + warning
- [ ] Bad price (`$29.99`, `free`), negative stock, empty rows: row errors, catalog not poisoned
- [ ] XSS / SQLi strings stored as text
- [ ] Preview: first 64KB, max 10 rows, no persist
- [ ] Re-run same file: upsert by sku, `created_at` unchanged, `updated_at` moves
- [ ] Shopper cannot POST import → 401/403

---

## 5. Auth shopper

- [ ] Signup with `email`, `password`, `displayName` → session cookie
- [ ] Login wrong password → 401
- [ ] `GET /api/auth/me` with cookie → email, role `SHOPPER`, displayName
- [ ] Admin cannot be created via signup
- [ ] Logout then `/api/auth/me` → 401
- [ ] Cart/checkout without session → 401

---

## 6. Cart + checkout

- [ ] Add sku, qty; qty 0 removes line
- [ ] Checkout with `Idempotency-Key`: same key twice → one order
- [ ] Fake pay always APPROVED
- [ ] Stock 1, two checkouts: one **201**, one **409** `INSUFFICIENT_STOCK` (two terminals or two cookies)
- [ ] Cart does not reserve stock; only pay does
- [ ] After success: cart empty, `order_items` snapshot, product `stock` decreased
- [ ] Out of stock: still in search; add-to-cart disabled in UI

---

## 7. Docker full stack

Stop host `mvn spring-boot:run` first if it is bound to port 8080.

```bash
docker compose up --build
```

- [ ] Browser `http://localhost:8080` (nginx), not `:5432`
- [ ] `http://localhost:8080/api/products` still 200
- [ ] `http://localhost:8080/actuator/health` 200 (via proxy if mapped)
- [ ] Cold start on a clean machine: one command, no extra README steps

---

## 8. After every big change (short loop)

Do this even if you skip later sections:

1. Health + public `GET /api/products`
2. Admin 401 without auth
3. Admin POST one product + GET by sku
4. PUT stale `version` → 409
5. `docker compose ps` still healthy if you use Compose

If those five pass, the skeleton did not regress.
