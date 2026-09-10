# Staff review and build plan

**Status:** decisions only. Do not start coding until this document and [SPECS.md](SPECS.md) are accepted.  
**CSV downloaded:** 2026-09-09 (keep this date in the public README).  
**Audience:** this file is **why**. [SPECS.md](SPECS.md) is the frozen **what**.

---

## Verdict

The MVP you described is the right *product* for this challenge. The Magento / Shopware / Elasticsearch / Redis / Kubernetes stack from the generic “enterprise ecommerce” advice is the wrong *delivery*.

This is a 5-business-day take-home. The PDF’s last paragraph is the real prompt: they already know AI can generate CRUD. They are scoring **judgment** — what you include, what you refuse, how you handle dirty data, concurrency, and how you explain trade-offs.

Your instincts that stay: Spring Boot, React, PostgreSQL, Docker Compose, CSV validation, transactional checkout, fake payment, no oversell.

Your instincts that must be cut or postponed: Elasticsearch, S3 + presigned URLs, recommendation-by-profile, cart TTL reservations, product availability as a separate state machine, and a fleet of true microservices.

---

## What they actually asked for

From `Code Challenge..pdf` and the email:

| Requirement | Bar |
|---|---|
| Local DB | SQL or NoSQL. PostgreSQL is the correct default. |
| Product CRUD | UI + API. Not catalog-only. |
| CSV import | Must use **their** example file. Header: `name, sku, description, category, price, stock, weight_kg`. |
| Search | UI + API. Not a search platform. |
| Purchase | Fake payment. Flow matters more than a gateway. |
| UI | CRUD, search, purchase. |
| Docker | Runnable as a container (Compose is enough). |
| README | Decisions, approach, alternatives, local run, **CSV download date**. |
| Extra (email tip) | Separate markdown for bugs/failures, not only README. |
| AI | Allowed. **No AI comments in the code.** |
| Delivery | GitHub repo within 5 business days. |

They did **not** ask for: auth provider, S3, Elasticsearch, Kubernetes, Magento, real payments, recommendations, or multi-region HA.

---

## What this challenge is really testing

1. **Dirty CSV as a first-class problem**, not a happy-path `COPY`. The file is small on purpose (~8.9 KB, 97 data rows) and packed with traps.
2. **SKU identity** — duplicates that look like updates vs conflicts.
3. **Checkout correctness** — no negative stock under concurrent buys.
4. **Scope control** — enterprise *quality* (layers, transactions, tests, Docker, docs) without enterprise *theatre*.
5. **Honesty** — a `KNOWN_ISSUES.md` for remaining gaps.

If Magento or OpenSearch appears in the repo, it reads as “I pasted a blog post”, not “I read the PDF”.

---

## CSV trap analysis (downloaded 2026-09-09)

Header matches the spec. Parser must be a real CSV parser (quoted commas, escaped quotes). Do not `split(',')`.

| Line | SKU | Problem | Import decision (proposed) |
|---|---|---|---|
| 4 | WM-042 | Price `$29.99` | Reject row. Do not silently strip `$` unless we document a currency policy. Prefer reject: the file is testing validation. |
| 7 | YM-015 | Price `free` | Reject. Not a decimal. |
| 16 | DL-007 | Stock `-5` | Reject. Stock cannot be negative. |
| 20 | XS-001 | XSS in `name` | **Accept as data**, store escaped. Never execute. UI must encode. This is a rendering test, not a format test. |
| 25 | HD-099 | Empty name | Reject. Name required. |
| 29 | SQL-001 | `Robert'); DROP TABLE products;--` | **Accept as data**. Parameterized SQL / JPA. Prove injection does not work. |
| 36 | RS-001 | Duplicate SKU vs line 2; looks like an update | **Upsert by SKU, last row wins**, emit a warning. Document this. |
| 41 | WS-001 | Name is whitespace | Reject. Trim then require non-empty. |
| 47 | MB-001 | Price `0.00` | Accept. Free item is valid unless we ban it. Visible, purchasable. |
| 50 | GK-088 | Missing `weight_kg` | Reject. Column present, value required. |
| 51 | VC-001 | Stock `0` | Accept. Product exists, not purchasable. |
| 52 | GC-025 | Empty category, stock `99999`, weight `0` | Reject empty category. (If we later allow digital goods, that is a documented exception — not for v1.) |
| 53 | CI-001 | Comma inside quoted name | Accept. Parser test. |
| 56 | BS-021 | Same SKU, different price/description | Last-write-wins + warning. Same rule as RS-001. |
| 59 | QI-001 | Escaped quotes in name | Accept. Parser test. |
| 62–63 | — | Empty rows | Skip. Not errors. |
| 89 | BS-021 | Repeat of the *original* speaker row | Last-write-wins: this restores 59.99 / 110 after line 56. Warning. |

**Implied valid catalog size:** well under 90 after rejects. Search and pagination still required, but Elasticsearch is unjustifiable.

Duplicate SKUs in the file:

- `RS-001` × 2 (line 2 then 36 — intentional update).
- `BS-021` × 3 (line 11, conflicting 56, then original again at 89).

That last pattern is deliberate: last-write-wins is the only rule that is both simple and consistent.

---

## Does your MVP make sense?

### Keep

- Custom app (not Magento/Shopware/Saleor/Medusa).
- Java + **Spring Boot** (not Java from scratch). HikariCP, `@Transactional`, validation, Boot actuator, Testcontainers. Rebuilding that by hand in five days is a negative signal.
- React UI: **two apps in one SPA** — storefront (shopper) and admin (catalog + CSV).
- PostgreSQL: `products`, `orders` / `order_items`, `users` (minimal).
- Docker Compose: `db` + `api` + `web`.
- CSV: header names **and order**, streaming parse, per-row errors, do not abort the whole file on one bad row.
- Fake payment with states (`PENDING` / `APPROVED` / `REJECTED`).
- Checkout as a single DB transaction: lock stock → simulate payment → insert order → decrement stock.
- Idempotency key on purchase (double-click / retry).

### Change

| Your idea | Staff call | Why |
|---|---|---|
| Elasticsearch | No | ~90 rows. Use PostgreSQL (`ILIKE` + btree/GIN). Mention ES as a later extract if catalog hits millions. |
| Spring Batch as default | Maybe later | File is tiny. A streaming service + chunked inserts is enough. Batch is justified if you *demonstrate* chunking and a job report, not because the file is large. |
| Java from scratch vs Boot | Spring Boot | Corporate baseline. Document “plain Java” as the rejected alternative. |
| S3 + presigned URLs | Out of scope | CSV has no images. Placeholder UI images from `sku` or a local `/assets` map. Mention S3 in README as a next step. |
| Recs from past purchases | Out of scope | Home = search + category + recent/featured (deterministic, not random-per-refresh). |
| Cart reservation (5–10 min) | Not for v1 | Requires TTL jobs, reservation rows, expiry, and UX for “someone else holds this”. **Validate at checkout** with `SELECT FOR UPDATE`. Document reservation as the next stock strategy. |
| States `AVAILABLE` / `IN_SOMEONE_CART` / `UNAVAILABLE` | Derive, don’t store | `stock == 0` → unavailable. Cart is client+server list, not a stock state. Reserved inventory is a future table, not a product enum. |
| True microservices (catalog + orders + inventory + users as separate processes) | **Not for this repo** | See next section. |

### Add (you did not mention these; evaluators will look)

- **Partial import report:** persist `import_jobs` + `import_row_results` (INSERTED / UPDATED / FAILED). Admin job detail + notification grouped by error code.
- **SKU unique constraint** in the database, not only in the parser.
- **Optimistic UI is not enough:** checkout must fail with a clear “insufficient stock” if two users race.
- **XSS encoding in React** (default) + never `dangerouslySetInnerHTML` on product name.
- **Tests that use the real CSV**, plus a concurrent checkout test.
- **Health checks** so Compose can `depends_on: condition: service_healthy`.
- **`KNOWN_ISSUES.md`** from day one.
- **No comments generated by AI** in source. Public README explains decisions instead.

---

## Microservices: recommendation

You asked for microservices, or at least separate services in folders.

**Do not ship a distributed system for this challenge.**

A purchase must atomically: read stock, take payment, write order, decrement stock. If `catalog`, `inventory`, and `checkout` are three processes, you inherit sagas, compensating transactions, dual-write, and a failure mode you cannot demo well in five days. That is not “enterprise”. That is unfinished.

What *does* look senior:

1. **Two deployables + one database:** `api` (Spring Boot) and `web` (React), plus Postgres.
2. **Inside the API, module boundaries that could become services later** — packages (or Maven modules) with no cross-table writes.
3. README section: “When we would split this, and what we would need (outbox, auth between services, stock reservation API).”

That is a “base that can grow into microservices”. A Compose file with five Java services and no saga is worse than a monolith.

### Proposed deployable layout (when we build)

```text
ecommerce/
  PLAN.md                          # this file
  KNOWN_ISSUES.md
  README.md                         # English, for GitHub
  docker-compose.yml
  fixtures/
    Code Challenge E-Commerce.csv  # the provided file
  services/
    api/                            # Spring Boot modular monolith
    web/                            # React (Vite)
```

Inside `services/api` (logical services, one JVM):

```text
com.example.shop
  catalog/      # products, csv import, search
  commerce/     # cart, checkout, orders, fake payment
  identity/     # optional thin user (seeded demo user)
  shared/       # errors, idempotency, time, config
```

Rules between modules:

- Only `catalog` writes `products` / stock.
- Only `commerce` writes `orders`.
- Checkout calls a `catalog` **application service** `decrementStock(sku, qty)` in-process. Later that becomes an HTTP/gRPC call with an outbox. Same method name, different adapter.

If you still want two Java processes after reading this, the only split that does not lie is:

- `catalog` owns products **and** stock.
- `checkout` owns orders and calls catalog `decrement` **before** committing the order, with a compensation path.

That is extra Docker, latency, and a 2PC you will not finish. **Default: one API.**

---

## Target architecture

```text
Your browser
    │
    │  http://localhost:8080
    ▼
web  (nginx :80)     ← only published port
    │  GET /            static React
    │  /api/*           reverse proxy
    ▼
api  (Spring Boot :8080, not public)
    │  jdbc:postgresql://db:5432/shop
    ▼
db   (PostgreSQL 15)
```

No Redis, no ES, no LocalStack, no Kafka for v1.

### Docker: how the three boxes talk

This is **required**. The PDF says the app must run as a container. We ship **Dockerfiles + Compose** so a reviewer runs one command:

```bash
docker compose up --build
```

Then opens `http://localhost:8080`.

| File (when we build) | Job |
|---|---|
| `docker-compose.yml` | Wires `db`, `api`, `web` on one bridge network |
| `services/api/Dockerfile` | Multi-stage: Maven/Gradle build → JRE image, runs Spring Boot |
| `services/web/Dockerfile` | Multi-stage: `npm run build` → nginx image + `nginx.conf` |
| `services/web/nginx.conf` | SPA + `proxy_pass http://api:8080` for `/api/` |
| `.dockerignore` | Keep images small |

**Network.** Compose creates `shop_default` (or named `shop`). Service name **is** the hostname:

- From `api`, the database host is `db`, not `localhost`.
- From `web` (nginx), the API host is `api`, not `localhost`.
- From **your laptop browser**, `db` and `api` do not exist. The browser only sees `localhost:8080` (the `web` container).

That is why the React app in Docker must call `/api/...` (same origin). Nginx forwards to `http://api:8080`. If the SPA were built with `http://localhost:8080` as the API URL, the browser would skip nginx and miss the API (or hit the wrong process).

**Who talks to Postgres.** Only `api`. The frontend never gets a JDBC URL. Nginx never talks to Postgres.

**Env (Compose, not secrets manager):**

- `POSTGRES_DB=shop`, `POSTGRES_USER=shop`, `POSTGRES_PASSWORD=shop` (demo; README says so).
- `SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/shop`
- `SPRING_DATASOURCE_USERNAME` / `PASSWORD` matching the db.
- Flyway runs **inside the API** on boot against `db`. No separate migrate container.

**Health / start order.** `db` healthcheck (`pg_isready`) → `api` waits until DB is healthy → `web` waits until `api` `/actuator/health` is up. `depends_on` without health is not enough (Postgres accepts connections before it is ready).

**Published ports.** Only `8080:80` on `web`. Do **not** publish `5432` or `8080` on `api` in the happy-path README (optional compose override for debugging). Reviewers should not need to know there are three containers.

**Roles do not apply to Docker.** There is not an “admin container” and a “shopper container”. One `api`, one `web`, one `db`. `SHOPPER` vs `ADMIN` is a row in `users` + Spring Security on HTTP. The same `api` process serves both after login. Compose does not know about roles.

**Local without Docker (dev only):** Vite on 5173, Spring on 8080, Postgres on 5432, Vite proxy `/api` → 8080. README can mention it; the **graded path** is Compose.

### Data model (v1)

**users** (minimal, because a purchase needs an owner)

- `id`, `email`, `display_name`, `role` (`SHOPPER` | `ADMIN`), `password_hash` (bcrypt), `created_at`
- Seed: `admin@shop.local` (ADMIN, Flyway). Shoppers **sign up**. No OAuth. Session cookie. Document that a real IdP and email verify are out of scope.

**products**

- `id` (uuid)
- `sku` unique not null
- `name`, `description`, `category`
- `price` numeric(12,2) check `>= 0`
- `stock` integer check `>= 0`
- `weight_kg` numeric(10,3) check `>= 0`
- `origin` (`MANUAL` \| `CSV`) — how it **first** entered the store
- `version` (bigint, JPA `@Version`) — optimistic lock for admin PUT
- `created_at` — first insert; never overwritten on CSV upsert
- `updated_at` — last write (CSV or admin edit)
- Indexes: unique `sku`; btree `category`; GIN FTS on `name` + `description` **or** trigram `pg_trgm` if we want substring search. Start with `ILIKE` + index on `lower(name)` if time is short; FTS is the “foreseeing” upgrade if day 3 has slack.

**import_jobs**

- `id`, `filename`, `started_at`, `finished_at`, `status` (`QUEUED` | `RUNNING` | `COMPLETED` | `FAILED_HEADER`)
- counts: `inserted`, `updated`, `skipped`, `failed`, `warnings`
- `header_ok` boolean
- `created_by` (admin user)

**import_row_results**

- `job_id`, `line_number`, `sku`, `outcome` (`INSERTED` | `UPDATED` | `FAILED` | `SKIPPED`)
- `code`, `severity`, `message`, `raw` (failed/warning rows)

**carts / cart_items** (server-side, no stock reservation)

- Cart is a list. Adding to cart does not decrement stock.
- On checkout, re-read stock.

**orders**

- `id`, `user_id`, `status` (`PENDING_PAYMENT`, `PAID`, `REJECTED`, `FAILED`)
- `idempotency_key` unique
- `total`, `created_at`

**order_items**

- `order_id`, `product_id`, `sku`, `name_snapshot`, `unit_price`, `qty`, `line_total`

Snapshots on the line: after import updates RS-001, old orders must still show what was bought.

### Checkout (v1) — Approach B

Single `@Transactional` method:

1. Load products by SKU with `LockModeType.PESSIMISTIC_WRITE` (`SELECT FOR UPDATE`).
2. If any `stock < qty` → fail, no order.
3. Fake payment (always approve in happy path; a query flag or 10% random reject is optional — if used, must be documented and seeded for tests).
4. If payment rejected → order `REJECTED`, stock unchanged.
5. If approved → decrement stock, order `PAID`.

Idempotency: same `Idempotency-Key` returns the original order, does not charge twice.

**Not in v1:** hold stock when adding to cart. Document why (expiry, abandoned carts, extra states) and that high-contention SKUs would get a reservation table later.

### CSV import (admin only)

Shoppers never see upload. Admin has **catalog CRUD** and **import jobs CRUD**.

**Drag-and-drop:** preview is a **fragment** only — `File.slice(0, 64KB)`, at most 10 rows. The browser never loads a large CSV into memory. Header/row truth is the server.

**After the job finishes, do not mix three worlds in one table.**

| Screen | What it is |
|---|---|
| `/admin/products` | Everything in the store (manual + all past imports). Shows `createdAt` / `updatedAt`. |
| `/admin/imports` | History of files. |
| `/admin/imports/:id` | **This file:** New / Already existed / Errors, paginated. Headline like “200 rows: 198 loaded (150 new, 48 already existed), 2 had errors.” |

**SKU already exists (manual or previous CSV):** update in place. `created_at` stays the first time we saw that SKU. `updated_at` becomes now. Job line = `UPDATED`, not new.

**Same CSV uploaded again:** new job. Catalog content is the same (last-wins). Counts shift toward `UPDATED`. The 2 bad rows fail again. We do not silently ignore a duplicate file.

Parse with a real CSV library, stream row-by-row, persist in chunks (50–100). Map columns **by header name**. Extra columns are ignored. Persist **every row outcome** (`import_row_results`) so the job detail can page through new vs updated vs failed without keeping the file in the browser.

**Job contract:**

1. `POST /api/admin/imports` → `202` + `jobId`.
2. `@Async` worker.
3. UI polls until `COMPLETED` / `FAILED_HEADER`.
4. Toast + open job detail.
5. Failed rows grouped by error code.

Example error groups for their file:

| Code | Severity | Count | What the admin should fix |
|---|---|---|---|
| `INVALID_PRICE` | ERROR | 2 | `$29.99`, `free` — use a plain decimal |
| `NEGATIVE_STOCK` | ERROR | 1 | stock must be ≥ 0 |
| `EMPTY_NAME` | ERROR | 2 | blank / whitespace name |
| `EMPTY_WEIGHT` | ERROR | 1 | missing `weight_kg` |
| `EMPTY_CATEGORY` | ERROR | 1 | blank category |
| `EMPTY_ROW` | skip | 2 | ignored, not an error |
| `DUPLICATE_SKU_UPSERT` | WARNING | 3 | `RS-001`, `BS-021` — last row won |

Header mismatch: job fails immediately, **zero** products written.

### Two surfaces: shopper vs admin

| | Shopper `/` | Admin `/admin` |
|---|---|---|
| Search, category, sort, product detail | yes | yes (read) |
| Cart, checkout, own orders | yes | no (or optional later) |
| Product create/edit/delete | no | yes (delete blocked if sold) |
| All orders (read) | own only | yes `/admin/orders` |
| CSV import + job report | no | yes |
| Import notifications | no | yes |

API enforces `role=ADMIN` on `/api/admin/**`. Hiding buttons in React is not enough.

### Catalog query API (one endpoint, not a search engine)

`GET /api/products?q=&category=&sort=&page=&size=`

| Param | Rule |
|---|---|
| `q` | Optional. Match **name, description, category, sku** (`ILIKE`). Ignore if blank or length `< 2`. |
| `category` | Optional exact match. Values from `GET /api/products/categories` (distinct from DB after import). |
| `sort` | `price_asc` \| `price_desc` \| `name_asc` (default `name_asc`). |
| `page`, `size` | Pagination. |

Frontend:

- **Debounce 300ms** on the search box. Do not fire on every keystroke.
- **Minimum 2 characters** before sending `q` (typeahead-friendly; avoids noise on `a`, `s`).
- With 0–1 characters: still list the catalog. Category + sort still apply. Search is a filter, not a gate.
- Same endpoint for the grid and for a small suggestion dropdown (e.g. `size=5`) while typing. **No second autocomplete service.**
- Category filter and price sort work with an empty search. They are catalog features, not extras bolted on later.

This is in scope. It is cheap, expected in a storefront, and still PostgreSQL.

### Cart (yes — keep it, do not reserve)

The cart is the right UX. Click image/card → detail (description, price, **available stock**) → add to cart → badge/count → checkout.

Rules:

- Cart is a **list of SKU + qty**, not a warehouse hold. Adding an item does **not** decrement stock and does **not** mark the product `IN_SOMEONE_CART`.
- Cap qty in the UI at current `stock`, but the **server re-checks at checkout** (two shoppers can still race; one gets `insufficient stock`).
- If stock dropped while the item sat in the cart, checkout returns a clear error per SKU; user adjusts qty.
- Persist the cart for the demo user (API) so refresh does not empty it. `localStorage` only is acceptable if time is short; prefer server cart because checkout already has a user.
- No TTL, no reservation job, no “held for 10 minutes”.

### Images (CSV has none — do not invent an S3 pipeline)

There is no `image` column. Do not add S3, LocalStack, or binary upload for v1.

**v1 recipe:**

1. Database does not store image URLs.
2. The UI derives a **deterministic** photo from `sku` so the same product always looks the same:
   `https://picsum.photos/seed/{sku}/600/600`
3. `onError` (no network, blocked, 429): local fallback — category color + product initials (CSS/SVG in the React app). Docker still looks fine offline.
4. README: placeholders only; production would add an `image_key`, private bucket, and presigned GET.

Do **not** download 90 product photos into the repo (license noise, and they will not match the SKUs). Do **not** use random Unsplash URLs (layout shift, different image per refresh).

### UI (enough to grade, not a design system)

- Login / role switch: Shopper vs Admin (seeded users).
- Shopper: catalog toolbar (search debounce + 2-char min, category, sort by price), cards with placeholder image, detail, cart, checkout.
- Admin catalog: product table with `createdAt` (when it entered our DB), create/edit/delete.
- Admin imports: drag-and-drop + 10-row preview; job list; job detail with New / Already existed / Errors.
- Render the XSS row as text. That is a feature.

---

## If this grows (README section — write it, do not build it)

The challenge asks for decisions and alternatives. A short “growth path” is the right place to show foresight. Tie every next step to a **trigger**, not to a tool list.

### What v1 already does so growth is not a rewrite

- Streaming CSV + chunked writes + job table (file can grow without loading it into memory).
- Job API (`202` + poll) so the worker can move off the HTTP thread later.
- Module packages (`catalog` / `commerce`) with a single `decrementStock` call.
- Checkout uses DB locks, not “read stock in the app and hope”.
- Idempotency key on purchase.

### Growth by dimension

| If this happens | Then | Not before |
|---|---|---|
| CSV is tens of MB / hundreds of thousands of rows | Spring Batch or a queue worker (`import-jobs` consumer), store the file in object storage, same report UI | Kafka “because enterprise” |
| Imports run while admins wait on a toast | Keep poll. Add email/Slack from `import_jobs` when `COMPLETED`. Optional WebSocket later | A notification microservice |
| Catalog hits hundreds of thousands, search latency hurts | Postgres FTS / `pg_trgm` first; **then** OpenSearch if ranking/typos/facets need it | Elasticsearch on day 1 |
| Many concurrent checkouts on the same SKU | Keep `SELECT FOR UPDATE`. If lock wait time hurts, add **reservations** for hot SKUs only | Reservations for every add-to-cart |
| Read traffic dwarfs writes | Read replica for catalog search; checkout still hits primary | Redis cache in front of stock (stale stock sells over) |
| Images become real | `image_key` column, S3, presigned GET, CDN | LocalStack in the take-home |
| Team splits or catalog/checkout scale independently | Extract `catalog` first (it already owns stock). Checkout calls decrement over HTTP + **outbox** | Split inventory away from catalog |
| Real money | Payment provider + webhook; order stays `PENDING` until webhook; never decrement stock on a client “success” | Fake payment forever |
| Multi-instance API | Sticky sessions unnecessary if JWT/session in DB; run `api` N replicas behind nginx. Import worker: one consumer per job (avoid two workers eating the same file) | Kubernetes before you have two instances |

### What we would tell an interviewer

v1 is a **modular monolith** on Compose because the domain is one bounded context and the sample catalog is tiny. The seams (job record, error codes, `decrementStock`, shopper vs admin) are the scalability story. Adding Redis, ES, and four services now would hide those seams behind infra we do not need.

---

## Explicitly out of scope (write this in the README)

- Real payment provider.
- Image pipeline / S3.
- Elasticsearch / Redis / RabbitMQ.
- Kubernetes.
- Personalized recommendations.
- Cart stock reservation + TTL.
- Social login / JWT complexity beyond a demo user.
- Multi-tenant, multi-currency, tax, shipping quotes.
- Magento / headless commerce platforms.

---

## Questions the challenge wants us to “ask”

Even without a reply from Ximena, the README should show we asked them and **picked a default**:

1. Duplicate SKU in the CSV: update or reject? → **Update, last wins, warn.**
2. Invalid rows: fail the file or continue? → **Continue, report.**
3. Is `stock` allowed to be string garbage? Spec says `string/int`. → **Parse as integer; reject otherwise.**
4. Guest checkout? → **Seeded demo user**, no guests (simpler FK).
5. Should `price = 0` be sellable? → **Yes.**
6. Admin vs shopper? → **Two seeded users, two UI shells, API role checks.** Admin owns CSV and product writes.
7. How does the admin learn about bad rows? → **In-app notification + report grouped by error code.** Email is a later adapter on the same job.
8. Docker: one container or Compose? → **Compose**, because a local DB is required.

---

## Five-day extras by role (include vs skip)

The features above are the product. These are the engineering extras that make a take-home look staff-level **without** eating the week.

### Frontend

**Include**

- Two shells (shopper vs admin) with empty / loading / error / out-of-stock states.
- Debounced search, category filter, price sort (already agreed).
- Checkout button disabled while in-flight; `Idempotency-Key` created once per click.
- Import poll + toast; errors grouped by code.
- Accessible labels on the catalog toolbar. No `dangerouslySetInnerHTML`.
- nginx in Docker so the browser talks to one origin (`/api` proxied). Avoids CORS theatre.

**Skip**

- Design system, Storybook, i18n, PWA, SSR, Next.js, animations library, E2E suite unless Day 4 is empty.

### Backend

**Include**

- Consistent error JSON (`code`, `message`, `details`).
- Flyway, Bean Validation on write DTOs, `@Transactional` checkout.
- Spring Security: `UserDetailsService` from DB, bcrypt, `hasRole("ADMIN")` on `/api/admin/**`, `@PreAuthorize` on admin services.
- `@Async` import + job record (API looks batch even for 8 KB).
- Actuator health for Compose.
- Testcontainers + the real CSV + a race test.

**Skip**

- Spring Cloud, API gateway, Kafka, Redis cache, GraphQL, OpenAPI UI unless leftover time, custom thread pools beyond a small import executor.

### DBA

**Include**

- `numeric` for money (never float). `timestamptz`. UUID PKs.
- Unique `sku`. Check constraints on stock/price. FK from `order_items` to `products` **RESTRICT** (cannot delete a product that was sold; set stock to 0).
- Unique `(user_id, idempotency_key)` so double-click cannot create two orders.
- Index on `category` and `lower(name)` (search).
- Snapshot name/price on `order_items` so later CSV upserts do not rewrite history.

**Skip**

- Read replicas, partitioning, partitioning by date, stored procedures, triggers for stock, Elastic-style materialized search tables.

### Solutions architect

**Include in the repo (docs + seams, not extra containers)**

- Compose: `db` + `api` + `web` only.
- README: growth table (already in this file).
- `KNOWN_ISSUES.md`.
- Demo secrets labelled as demo.
- Copy of the CSV under `fixtures/` with download date in README.

**Skip**

- Kubernetes manifests, Terraform, Magento, service mesh, LocalStack, multi-region diagrams as if they were running.

### If Day 4 has spare hours (optional, in this order)

1. OpenAPI (`/v3/api-docs`) — helps reviewers.
2. `X-Request-Id` on responses.
3. Non-root user in Dockerfiles.
4. Playwright one happy-path: import CSV as admin, search as shopper, checkout.

Do not spend spare hours on a second Java service.

---

## Four-day plan

See [SCHEDULE.md](SCHEDULE.md) for the principal-level 4-day sequence, cut order, and enterprise cases we **document** instead of building.

Do not start S3, ES, or a second Java service. If a day is lost, freeze features and finish Docker + README + CSV tests.

---

## Testing bar (this is the senior signal)

- Unit: CSV header rejection, each trap row, upsert last-wins for `RS-001` / `BS-021`.
- Integration (Testcontainers Postgres): CRUD, import of the real file, checkout stock decrement.
- Concurrency: two checkouts on `stock = 1`.
- Web: not mandatory E2E if time is tight; API tests + a short “manual test” section in README is acceptable. Playwright is a bonus, not a requirement.

---

## README obligations (do not forget)

- Date the example CSV was downloaded: **2026-09-09**.
- How to run: `docker compose up --build`, ports, demo users, how to import the CSV.
- Decisions and alternatives (Boot vs raw Java, Postgres FTS vs ES, lock-at-checkout vs reservation, modular monolith vs microservices, **when we would add a queue / OpenSearch / S3**).
- AI used: yes; comments stripped; this plan guided the model.

Companion file: `KNOWN_ISSUES.md` for anything we ship knowing it is incomplete (example: no real auth, no image upload, no cart reservation).

---

## What would make this look junior

- Happy-path CSV only (`split` + insert), so XSS/SQL/`$29.99`/`free` land as garbage or crash the job.
- Elasticsearch for 80 products.
- Four microservices and a broken checkout.
- No unique `sku`.
- Stock decrement without a transaction (`stock = stock - 1` without a lock or `WHERE stock >= qty`).
- README that lists tools instead of trade-offs.
- AI comments (`// This function handles...`) left in the code.

---

## Decision checklist (accept before coding)

- [ ] Modular monolith: `services/api` + `services/web` + Postgres. Not a microservice mesh.
- [ ] Dockerfiles + `docker-compose.yml`; one command `docker compose up --build` → `http://localhost:8080`.
- [ ] Spring Boot, not raw servlets.
- [ ] PostgreSQL search, not Elasticsearch.
- [ ] CSV: required columns by name; extra columns ignored; per-row errors; upsert last-wins.
- [ ] Checkout: lock at pay time; no cart holds.
- [ ] Fake payment; persist order regardless of approve/reject.
- [ ] No S3. Images = picsum seed by SKU + local CSS fallback.
- [ ] Catalog: category filter, sort by price, debounce 300ms, `q` only if length ≥ 2.
- [ ] Cart as a list; stock checked at checkout, not at add-to-cart.
- [ ] Shopper vs admin; CSV and product writes are admin-only.
- [ ] Shopper **sign up + sign in**. Admin seeded only. No shipping address.
- [ ] Import is a job: poll + in-app notification; errors grouped by code (not by product category).
- [ ] Import is a job: poll + in-app notification; errors grouped by code (not by product category).
- [ ] Follow [SPECS.md](SPECS.md): error envelope, money as numeric, product delete restricted, idempotent checkout, 2 MB CSV cap.
- [ ] `KNOWN_ISSUES.md` + English README with CSV date 2026-09-09 and a growth-path table.
- [ ] Public GitHub repo as the submission.

When this checklist is accepted, the first build slice is Day 1: Compose + Flyway + product CRUD, still using `ecommerce/` as the repo root.
