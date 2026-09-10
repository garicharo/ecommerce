# Specifications (v1 contract)

Frozen for the **4-day** build. If a case is not here, pick the cheapest option that does not oversell stock and write it in `KNOWN_ISSUES.md`.

Related: [PLAN.md](PLAN.md) (why). This file is **what**.

CSV downloaded: **2026-09-09**.

---

## 1. Product and UX

| Item | Spec |
|---|---|
| Language | English UI and README (submission). |
| Currency | USD. CSV has no currency column. Display `$12.99`. Store `numeric(12,2)`. |
| Time | UTC in DB (`timestamptz`). API ISO-8601 (`2026-09-09T18:00:00Z`). |
| Shopper routes | `/`, `/products/:sku`, `/cart`, `/checkout`, `/orders`, `/login`, `/signup`. |
| Admin routes | `/admin`, `/admin/products`, `/admin/products/new`, `/admin/products/:sku`, `/admin/imports`, `/admin/imports/:jobId`. |
| Auth UX | Sign up + sign in for shoppers. Admin is seeded only. No OAuth, no email verification, no password reset. |
| Empty catalog | Shopper sees empty state + “no products yet”. Admin sees CTA to import CSV. |
| Loading | Spinner/skeleton on catalog, import poll, checkout submit. Disable double-submit on checkout. |
| Errors | Toast or inline. Checkout stock errors **per SKU**, not a generic 500. |
| Out of stock | Badge + disable Add to cart. Product remains searchable. |
| Images | `https://picsum.photos/seed/{sku}/600/600`, CSS fallback on error. |
| Responsive | Catalog usable at 1280px and ~375px. Admin can be desktop-first. |
| A11y (minimum) | Labels on search/filters, button names, focus visible, no `dangerouslySetInnerHTML`. |
| XSS | Render product `name` / `description` as text. The XSS CSV row must display, not execute. |

---

## 2. Roles

| Role | Can |
|---|---|
| `SHOPPER` | Read catalog, search, cart, checkout, own orders. |
| `ADMIN` | Same reads + product write + CSV import + import reports. Admin may also shop (same cart APIs). |

- Hiding `/admin` in React is **not** authorization. The API must reject a shopper with **403**.
- Seeded: `admin@shop.local` (**ADMIN only**, cannot self-register). Shoppers use **sign up + sign in**.
- `POST /api/auth/signup` always creates `SHOPPER`. There is no “register as admin”.

### 2.0 How the admin exists (not the Postgres password)

The admin is a **row in `users`**, created on API startup if missing. It is **not** a Docker user and **not** the database login.

| Secret | Where | What it unlocks |
|---|---|---|
| `POSTGRES_PASSWORD` / `SPRING_DATASOURCE_PASSWORD` | Compose → Postgres + JDBC | User `shop` on database `shop` |
| `APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD` | Compose → API env; README | HTTP login for `/admin` |

`application.yml` only **references** env (with demo defaults for local):

```yaml
app:
  admin:
    email: ${APP_ADMIN_EMAIL:admin@shop.local}
    password: ${APP_ADMIN_PASSWORD:admin1234}
```

On boot, `AdminUserInitializer` (or equivalent): if no user with `role=ADMIN`, insert one with **BCrypt(APP_ADMIN_PASSWORD)**. If the admin already exists, **do not** reset the hash (idempotent seed).

Flyway creates the **table**. The initializer creates the **account**. We do **not** put a bcrypt string in SQL (reviewers would not know the password). Reviewers know it because README + `docker-compose.yml` say `admin@shop.local` / `admin1234`.

Do not log the password. Do not put it in GitHub Actions as a real secret for this demo.

### 2.1 Where the role is validated (Spring Security)

The source of truth is the `users.role` column. Spring Security maps it to a `GrantedAuthority` and checks it on **every request**, before controllers.

**Mapping**

- DB `ADMIN` → authority `ROLE_ADMIN`
- DB `SHOPPER` → authority `ROLE_SHOPPER`
- `hasRole("ADMIN")` in Spring looks for `ROLE_ADMIN` (the `ROLE_` prefix is added for you).

**Sign up + sign in (session, not JWT)**

Full shopper account flow. No email verification, no password reset, no OAuth (document those). Same origin via nginx.

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/auth/signup` | `{ "email", "password", "displayName" }` → user `SHOPPER` + session cookie. 409 if email exists. Password min 8. |
| `POST` | `/api/auth/login` | `{ "email", "password" }` → session cookie. 401 if bad credentials. |
| `GET` | `/api/auth/me` | `{ "email", "role", "displayName" }`. 401 if anonymous. |
| `POST` | `/api/auth/logout` | Invalidate session. |

**Shipping address:** **out of v1**. No `addresses` table, no checkout step for street/city/zip. The challenge is stock + fake pay, not fulfillment. A later `order.shipping_address` JSON is enough when we add a carrier.

**Filter chain (the real gate)**

```text
GET  /actuator/health                         permitAll
POST /api/auth/signup, /api/auth/login          permitAll
GET  /api/products/**, /api/products/categories   permitAll   (browse without account)
/api/cart/**, /api/checkout, /api/orders/**    hasAnyRole(SHOPPER, ADMIN)
/api/admin/**                                 hasRole(ADMIN)
POST /api/auth/logout, GET /api/auth/me        authenticated
```

Catalog is public. Add-to-cart / checkout require login (redirect to `/login` or `/signup`).

Wrong password / no cookie on a protected route → **401**. SHOPPER hitting `/api/admin/**` → **403**.

**Defense in depth (Java)**

- URL matchers in `SecurityFilterChain` (mandatory).
- `@PreAuthorize("hasRole('ADMIN')")` on admin application services (CSV import, product write). If someone adds a new controller and forgets the matcher, the method still dies.
- Controllers do **not** check `if (user.role.equals("ADMIN"))` by hand.

**Frontend (UX only)**

- After login, `GET /api/auth/me` and route: `role === ADMIN` can open `/admin`.
- Shopper hitting `/admin` → redirect home.
- `fetch` must use `credentials: 'include'`.

**CSRF:** session cookies on a JSON API. v1: `SameSite=Lax` + same origin via nginx. Document leftover CSRF as `KNOWN_ISSUES` **or** send Spring’s `X-XSRF-TOKEN` cookie if time. Do **not** invent JWT + refresh tokens in five days.

**Not used:** `OncePerRequestFilter` custom “role filter”, hard-coded `if (email.contains("admin"))`, roles only in localStorage.

---

## 3. HTTP API

Base: `/api`. JSON camelCase. UTF-8.

### 3.1 Envelope

Success list:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "total": 0
}
```

Error (all 4xx/5xx):

```json
{
  "code": "INSUFFICIENT_STOCK",
  "message": "Not enough stock for SKU VC-001",
  "details": [{ "sku": "VC-001", "requested": 2, "available": 0 }]
}
```

Do not return Java stack traces. Log them server-side.

### 3.2 Catalog (shopper + admin)

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/products` | `q`, `category`, `sort`, `page`, `size`. |
| `GET` | `/api/products/categories` | Distinct categories, sorted. |
| `GET` | `/api/products/{sku}` | 404 if missing. |

Query rules:

- `q`: ignored if missing or `length < 2`. Match **`name`, `description`, `category`, `sku`** with `ILIKE '%'||q||'%'` (case-insensitive). Index `lower(name)` is enough for v1.
- `category`: exact match, optional, AND with `q`.
- `sort`: `name_asc` (default) \| `price_asc` \| `price_desc`.
- `page`: 0-based. `size` default 20, max 50.
- No Elasticsearch. Same endpoint for the grid and a 5-item typeahead (`size=5`).

XSS in `name` is stored and returned as JSON; React text-nodes it. Search for `script` may find that row — that is correct.

### 3.3 Admin catalog writes

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/admin/products` | Create. 409 if SKU exists. |
| `PUT` | `/api/admin/products/{sku}` | Full update except SKU. Body includes `version`. **409** `OPTIMISTIC_LOCK` if `version` is stale (two admins). |
| `DELETE` | `/api/admin/products/{sku}` | **409** if any `order_items` reference it. Admin must set `stock=0` instead. History stays. |

Body (POST/PUT): `name`, `description`, `category`, `price`, `stock`, `weightKg`. PUT also `version` (from GET). Price as decimal string; Java `BigDecimal`.

### 3.3b Admin orders (read-only)

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/admin/orders` | All purchases, newest first, paginated. |
| `GET` | `/api/admin/orders/{id}` | Line snapshots. 404 if missing. |

Shopper `GET /api/orders` remains **own orders only**. Nobody `DELETE`s an order.

### 3.4 Import jobs (CRUD of the load, not of the CSV file)

Jobs are first-class records. List them, open one, see what that file did. Do not delete jobs in v1 (audit). No update except status the worker writes.

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/admin/imports` | `multipart/form-data` field `file`. `202 { jobId }`. |
| `GET` | `/api/admin/imports` | Job list, newest first, paginated. |
| `GET` | `/api/admin/imports/{jobId}` | Summary + `errorsGrouped`. |
| `GET` | `/api/admin/imports/{jobId}/rows` | Paginated rows. Query `outcome=INSERTED\|UPDATED\|FAILED\|SKIPPED`. |

Job summary payload (example):

```json
{
  "jobId": "...",
  "filename": "Code Challenge E-Commerce.csv",
  "status": "COMPLETED",
  "totalRows": 200,
  "inserted": 150,
  "updated": 48,
  "failed": 2,
  "skipped": 0,
  "warnings": 3,
  "headline": "File with 200 rows: 198 loaded (150 new, 48 already existed), 2 had errors.",
  "errorsGrouped": [{ "code": "INVALID_PRICE", "count": 2 }]
}
```

Upload limits: UTF-8 CSV, `.csv` or `text/csv`, **max 2 MB**. 413 if larger.

**Browser must not parse the whole file.** Drag-and-drop preview reads `file.slice(0, 64 * 1024)` only and shows at most **10 data rows** + header. Full validation is the server job. If the local preview header is wrong, still allow upload (server is source of truth) but warn.

### 3.5 Cart and checkout

| Method | Path | Notes |
|---|---|---|
| `GET` | `/api/cart` | Current shopper. |
| `PUT` | `/api/cart/items` | `{ sku, qty }`. `qty=0` removes. |
| `POST` | `/api/checkout` | Header `Idempotency-Key` required. |
| `GET` | `/api/orders` | Current shopper. |
| `GET` | `/api/orders/{id}` | 404 if not owner. |

Checkout:

- Header `Idempotency-Key` (UUID). Same key + same user → original order, HTTP 200. Different body with same key → 409.
- Empty cart → 400 `CART_EMPTY`.
- Any line `qty > stock` → 409 `INSUFFICIENT_STOCK`, **no** partial order, stock unchanged.
- Fake payment **always APPROVED** in v1 (deterministic tests). Document “PSP + webhook” in README.
- Success: 201 `{ orderId, status: "PAID" }`.

### 3.6 Status codes

| Code | When |
|---|---|
| 200 | GET/PUT OK, idempotent replay |
| 201 | Order created |
| 202 | Import accepted |
| 400 | Validation, bad header, empty cart |
| 401 | Not logged in |
| 403 | Shopper hit admin |
| 404 | Unknown sku/job/order |
| 409 | SKU conflict, insufficient stock, product in orders, stale `version` on PUT |
| 413 | CSV too large |
| 500 | Unexpected (generic message) |

---

## 4. CSV import

**Required columns (by name, not by position):**

`name`, `sku`, `description`, `category`, `price`, `stock`, `weight_kg`

- Those seven **must exist** in the header (case-sensitive as in the spec). If any is missing → `FAILED_HEADER`, zero writes.
- **Extra columns are ignored** (e.g. `image_url`, `color`). We only map the seven we own. Log once at INFO: `ignoredColumns=[...]`.
- Duplicate required names → `FAILED_HEADER`.
- Column **order does not matter**. Map with the CSV header row, not `row[3]`.

### Upload process (step by step)

1. Admin opens `/admin/imports` (must be `ROLE_ADMIN` or 403).
2. Drag-and-drop or file picker. Browser reads `file.slice(0, 64KB)` only → table preview of ≤10 rows. This is **not** validation.
3. Admin confirms. `POST /api/admin/imports` multipart `file`. Reject if not `.csv` / `text/csv` or size > 2 MB → **413**.
4. API returns **202** `{ jobId }`. Insert `import_jobs` `QUEUED` then `RUNNING`.
5. `@Async` worker streams the file (Commons CSV). Never load the whole file as one string.
6. Header check (required names present). Fail job → `FAILED_HEADER`. Toast failure. Stop.
7. For each data row:
   - Empty row → `SKIPPED`.
   - Validate fields. Fail → `FAILED` + `code` (do not abort the file).
   - SKU new → insert, `created_at=now`, `origin=CSV`, `INSERTED`.
   - SKU exists → update (not `created_at`), `UPDATED`.
   - Same SKU earlier in this file → last wins + `DUPLICATE_SKU_UPSERT` warning.
8. Flush every 50–100 valid rows.
9. Job `COMPLETED`. Counts on the job row. Every line in `import_row_results`.
10. UI polls ~1s → toast: “File with N rows: X loaded (a new, b already existed), f had errors.”
11. Navigate to `/admin/imports/:jobId`: tabs New / Already existed / Errors (paginated). Errors grouped by `code`.
12. Catalog `/admin/products` is the live store (not this file).

The **why it failed** for a row lives in `import_row_results.message` + `code`. That is the CSV audit. Application logs are extra (below).

### Logging (cheap first)

**Must (little time):**

- Failed CSV rows in `import_row_results` (line, sku, code, message). Do not rely on log files to explain a bad price.
- `log.error` on unexpected 500: exception + `jobId` / `orderId` if present. No stack traces in the HTTP body.
- Do not log passwords or the full CSV line at INFO (XSS payload noise).

**If Day 4 has spare time:** `X-Request-Id` + MDC, JSON logback. **Do not** add ELK/Loki/CloudWatch.

| Rule | Spec |
|---|---|
|---|---|
| Encoding | UTF-8. Strip BOM if present. |
| Parser | RFC 4180 library. Quoted commas and escaped quotes must work. |
| Empty rows | Skip. Count as `skipped`, not errors. |
| Trim | Trim all fields. Name that becomes empty → `EMPTY_NAME`. |
| SKU | Required, unique in DB. **Insert** if sku is new; **update** if it already exists (manual or previous import). Last row in *this* file wins + `DUPLICATE_SKU_UPSERT` warning. |
| name | Required, non-blank. Store raw text (XSS payload allowed). |
| description | Required (may be short). |
| category | Required, non-blank. Free string, not an enum. |
| price | Decimal `>= 0`. Reject `$`, `free`, blanks. Two decimal places stored. |
| stock | Integer `>= 0`. Reject negatives and non-integers. |
| weight_kg | Decimal `>= 0`. Reject blank. |
| Partial success | Invalid rows do not abort the job. |
| Chunk | Persist ~50–100 valid rows per flush. |
| Concurrency | Two admins may import at once. Last writer wins per SKU. |

### Error codes

`INVALID_HEADER`, `EMPTY_NAME`, `EMPTY_SKU`, `EMPTY_CATEGORY`, `EMPTY_DESCRIPTION`, `INVALID_PRICE`, `NEGATIVE_STOCK`, `INVALID_STOCK`, `EMPTY_WEIGHT`, `INVALID_WEIGHT`, `DUPLICATE_SKU_UPSERT` (warning).

Admin UI groups failed rows by `code`, shows count, then line numbers.

### Same SKU already in the store

| How the product got there | This CSV row | `created_at` | `updated_at` | Job row `outcome` |
|---|---|---|---|---|
| Did not exist | Valid | now | now | `INSERTED` (new) |
| Manual create or older import | Valid | **unchanged** (first time in our DB) | now | `UPDATED` |
| Same SKU earlier in **this** file | Valid | first insert time | now | last row `UPDATED`; earlier line warning |
| Invalid | — | unchanged | unchanged | `FAILED` |

Re-uploading **the same CSV**:

- Creates a **new job** (not a no-op). The catalog converges to the same last-wins snapshot (idempotent data).
- Almost every valid SKU is `UPDATED` (0 inserts if nothing new).
- The same 2 bad rows fail again. That is useful: the admin sees the file is still dirty.
- We do **not** skip the file because the checksum matches. Deduping uploads is a later optimization.

### Admin screens (three lists, not one mega-table)

1. **`/admin/products`** — the live catalog (everything that exists: old, new, manual). Columns include `createdAt`, `updatedAt`, `origin` (`MANUAL` \| `CSV`). Filter/sort by created date so “what did we add to the store?” is a DB fact, not a guess.
2. **`/admin/imports`** — CRUD-style **list of jobs** (filename, when, headline counts, status).
3. **`/admin/imports/:jobId`** — detail of **this** load: headline, then tabs **New (`INSERTED`)**, **Already existed (`UPDATED`)**, **Errors**. Each tab is paginated from the API. This is how the admin knows what was new in *this* file without dumping the CSV into the browser.

Manual **Create product** uses the same `products` row: `origin=MANUAL`, `created_at=now`. A later CSV with that SKU updates it; it does not look “new”.

---

## 5. Database

Engine: PostgreSQL 15+. Migrations: Flyway. App user is not superuser.

| Table | Constraints / indexes |
|---|---|
| `users` | unique `email`; `role` check |
| `products` | unique `sku`; `price >= 0`; `stock >= 0`; `weight_kg >= 0`; `origin` `MANUAL`\|`CSV`; `version` bigint not null default 0; `created_at` (first insert, never overwritten on upsert); `updated_at`; index `category`; index `lower(name)` |
| `import_jobs` | FK `created_by` → users; `filename`, `byte_size`, `status`, counts |
| `import_row_results` | FK job; `line_number`; `sku`; `outcome`; `code` nullable; `message`; index `(job_id, outcome)` |
| `carts` | unique `user_id` |
| `cart_items` | unique `(cart_id, sku)`; `qty > 0` |
| `orders` | unique `(user_id, idempotency_key)`; `total >= 0` |
| `order_items` | FK order cascade; FK product **restrict**; snapshot columns not null |

Types: UUID PKs, `numeric` never `float`/`double` for money, `integer` stock, `timestamptz` timestamps.

Checkout transaction: `SELECT … FOR UPDATE` on product rows, then decrement. Isolation: default `READ COMMITTED` is enough with row locks.

Pool: Hikari default is fine for Compose. One API replica in v1.

---

## 6. Frontend engineering

| Item | Spec |
|---|---|
| Stack | React + Vite + TypeScript. |
| API base | `VITE_API_URL` (Compose: `/api` via nginx, or `http://localhost:8080`). |
| Search debounce | 300ms. |
| Search min length | 2 before sending `q`. |
| Sort/filter | Category + price sort without requiring `q`. |
| Cart qty | Input cannot exceed displayed stock; server still authoritative. |
| Checkout | One click → one `Idempotency-Key` (generated once per attempt). |
| Import UX | Drag-and-drop; preview ≤10 rows from a 64KB slice; upload; poll 1s; toast; navigate to job detail. |
| Import poll | 1s until terminal status; then toast + open `/admin/imports/:jobId`. |
| Docker | nginx serving the built SPA, proxy `/api` to the API. |

---

## 7. Backend / ops

| Item | Spec |
|---|---|
| Java | **21 LTS**. **Spring Boot 4.1.1** (current stable). Money in Java: **`BigDecimal` only**, never `double`/`float`. |
| Package | `catalog`, `commerce`, `identity`, `shared` (one JVM). |
| Health | `GET /actuator/health` (and `/actuator/health/readiness` if easy). Compose waits on healthy. |
| Logs | Must: `import_row_results` + `log.error` on 500. JSON/MDC only if Day 4 has time. |
| CORS | Only needed if the web origin differs in local dev. Compose nginx avoids CORS. |
| Secrets | Compose demo password. README: not for production. |
| Docker | `docker compose up --build` from repo root. Only `localhost:8080` is public. Browser → nginx → `/api` → `api:8080` → `db:5432`. Roles are not Docker concepts. |
| Files in git | `fixtures/Code Challenge E-Commerce.csv` copy of the provided file. |

---

## 8. Security (5-day bar, not a pentest)

- JPA/parameterized SQL. The SQL-injection CSV name is a stored string.
- Role checks: Spring Security `hasRole` on `/api/admin/**` + `@PreAuthorize` on admin services. Not a React-only flag.
- Multipart: type + size. Do not execute uploaded files.
- No default Spring open `GET /api/admin` without auth.
- Demo passwords only; call that out in README.

Skip for v1: full CSRF double-submit if time runs out (document it), rate limiting, HTTPS, WAF, JWT/refresh, account lockout.

---

## 9. Tests (must exist)

| Test | Assert |
|---|---|
| Header missing required names | Job `FAILED_HEADER`, 0 products. Extra columns still import. |
| Provided CSV | Known error codes present; `RS-001` last wins (line 36 values); `BS-021` last wins (line 89 values). |
| XSS/SQLi rows | Persist; GET returns them as JSON strings. |
| Checkout race | `stock=1`, two parallel checkouts → one PAID, one 409, stock=0. |
| Idempotency | Two checkout POSTs same key → one order. |
| Shopper import | 403. |
| No cookie on admin | 401. |

---

## 10. Other details still worth locking

| Topic | Spec |
|---|---|
| CSV extra columns | **Ignore.** Map the seven required names. Missing required name → `FAILED_HEADER`. |
| Price in Java | `BigDecimal`. DB `NUMERIC(12,2)`. |
| Versions | Java 21, Spring Boot **4.1.1**, Postgres 16 (or 15-alpine). |
| Sold product delete | 409. Set stock 0. Snapshots on `order_items` keep the old name/price. |
| Admin orders | Read-only `/admin/orders`. Shopper sees only own. Nobody deletes orders. |
| CSV line endings | Accept LF and CRLF (Commons CSV). |
| UTF-8 BOM | Strip. |
| SKU in URLs | `encodeURIComponent`. Path is `SQL-001`, not the injection name. |
| `spring.jpa.open-in-view` | `false`. |
| Actuator | Health only. Do not expose `/actuator/env`. |
| Unknown JSON fields | Ignore on read. |
| Manual create vs CSV | Same validation. Duplicate SKU on `POST` → 409, not upsert (upsert is import-only). |
| Two imports at once | Allowed. Last commit per SKU wins. |
| Session idle | Default server session (e.g. 30 min). No remember-me. |
| Guest | Catalog is public. Cart and checkout require an account (signup or login). |
| Order id | UUID is enough; no human `ORD-0001` unless leftover time. |
| Catalog stock=0 | Visible, not purchasable. |
| Weight on shopper UI | Show on detail (`1.2 kg`). |

### Database — the bits people forget

- `NUMERIC` / `BigDecimal`, never float.
- `created_at` does not change on CSV upsert; `updated_at` does.
- `products.version` (`@Version`): admin PUT with a stale version → 409 `OPTIMISTIC_LOCK`. CSV import and checkout bump `version` when they write the row (JPA). Checkout still uses `SELECT FOR UPDATE` for stock; the two locks are complementary.
- `order_items` snapshots so a later import does not rewrite old receipts.
- Unique `(user_id, idempotency_key)`.
- `ON DELETE RESTRICT` from `order_items` → `products`.
- Checkout uses `SELECT FOR UPDATE`, not a trigger.
- `origin` `MANUAL` vs `CSV` = first insert only.

---

## 11. Out of spec (do not build)

GraphQL, Redis, OpenSearch, Kafka, S3, Kubernetes, real PSP, email, i18n, PWA, SSR, recommendations, cart TTL, image upload, guest checkout, multi-currency, tax, shipping, rate limiter, Swagger unless Day 4 has spare time.
