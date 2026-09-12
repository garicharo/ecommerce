# README outline (assemble on Day 4)

Not the public README. Copy from here into `README.md` when search + checkout exist. English, short, reviewer-first.

PDF requires: CSV download date, how to run locally, decisions / approach / alternatives considered. GitHub **is** the deliverable.

---

## 1. Target README shape (in this order)

1. **What it is** — one paragraph: catalog + dirty CSV import + search + fake checkout, Docker Compose. Not Magento.
2. **CSV date** — `2026-09-09` (required). File: `fixtures/Code Challenge E-Commerce.csv` (and root copy).
3. **Run** — one command, one URL, admin login. No “when Day 1 lands”.
4. **Try the CSV** — where to upload, what the report should say (counts).
5. **Decisions** — table: choice vs alternative vs why.
6. **Out of v1** — what a real shop would add later (not missing features of the PDF).
7. **Links** — `SPECS.md` / `PLAN.md` / `KNOWN_ISSUES.md` as annex. Do **not** link `PRACTICE.md`, `AGENTS.md`, or `.cursor/`.

---

## 2. Run block (update if ports/env change)

```bash
docker compose up --build
```

Open http://localhost:8080

| Who | Email | Password | Notes |
|---|---|---|---|
| Admin | `admin@shop.local` | `admin1234` | Seeded on API boot. Not the Postgres password (`shop`). |

Import: log in → `/admin/imports` → drop `fixtures/Code Challenge E-Commerce.csv`.

Expected report (their file): 97 rows, 88 loaded (85 new, 3 already existed), 7 errors, 2 skipped (empty). Last-wins: `RS-001` line 36, `BS-021` line 89.

---

## 3. Decisions / alternatives (README table)

| We did | Alternative | Why |
|---|---|---|
| Custom Spring Boot + nginx + Postgres | Magento / Shopware | 5-day take-home; PDF scores judgment, not a platform install. |
| Modular monolith (3 Compose services) | Microservices + Kafka | One team, one DB, one deploy. Split later if search/checkout load diverges. |
| Postgres `ILIKE` for search | Elasticsearch / OpenSearch | ~90 SKUs after import. Add OpenSearch when the catalog is huge or facets matter. |
| `BigDecimal` / `numeric` | `double` | Money. |
| Commons CSV, columns by **name** | `split(",")` | Quoted commas in their file (`CB-010`, etc.). Extra columns ignored. |
| Reject `$29.99` and `free` | Strip `$` / map `free` → 0 | The file is a validation test, not a currency policy. |
| Upsert by SKU, last row wins + warning | Fail the whole file / fail the duplicate | `RS-001` / `BS-021` look like intentional updates. |
| Persist `import_jobs` + row outcomes | One-shot import, logs only | Admin must see new vs updated vs failed without re-reading the CSV. |
| XSS / SQLi stored as text | Sanitize names on write | Rendering test. UI encodes; JPA parameterizes. |
| Lock stock at checkout (`SELECT FOR UPDATE`) | Reserve on add-to-cart | Abandoned carts and TTL states are out of scope. |
| Fake pay always APPROVED, same TX | Stripe / webhooks | PDF says fake. Real PSP: `PENDING` until webhook, then decrement. |
| Seeded admin + (later) shopper signup | OAuth / magic link | No identity provider in the PDF. |
| No shipping address | Full checkout address | Challenge is stock + fake pay, not fulfillment. |
| Flyway, not `ddl-auto` | Hibernate update | Repeatable schema in Docker. |

---

## 4. PDF vs what we built

**PDF asked:** local DB, product CRUD, CSV import, search, purchase (fake pay), UI for those, Docker, README (decisions + CSV date), GitHub.

**PDF did not ask; we added because their CSV / checkout require it:**

| Extra | Why it is defensible |
|---|---|
| Admin vs public catalog | CRUD cannot be anonymous. |
| Seeded admin (`APP_ADMIN_*`), not DB user | HTTP login ≠ Postgres password. |
| Import jobs + per-row audit | Dirty file: `$`, `free`, empty rows, duplicate SKUs, XSS. |
| Last-wins + `DUPLICATE_SKU_UPSERT` | In *their* CSV, not in the PDF. |
| XSS/SQLi as data | Same. |
| `BigDecimal`, Flyway, `@Version` | Quality, not theatre. |
| Three containers, nginx proxies `/api` | “Runnable as a docker container.” |

**PDF asked; still to build (do not claim in README until done):** search (`q` / category / sort), purchase flow, shopper UI for those, pagination in the UI, signup/session if we keep Basic only for Day 2.

**PDF did not ask; do not build — list in README as “when…”:** Elasticsearch, S3, Magento, Kubernetes, cart stock reservation, shipping, admin orders (optional if Day 3 has time), typeahead dropdown.

---

## 5. “Something is missing” paragraph (fulfillment)

v1 ends at **PAID**. Pick/pack/ship, returns, tax, variants, multi-warehouse, coupons are not the challenge. Later: add `orders.status` (`PACKING`, `SHIPPED`) without changing the stock lock.

Stock 0: listed, not purchasable. Price 0: allowed (Mystery Box).

---

## 6. What not to put on the public README

- `PRACTICE.md` / `AGENTS.md` / `.cursor/rules/` — pairing notes, not the product.
- “Day 1 scaffold”, “You write these classes”, “Admin (planned)”.
- Postgres password as if it were the shop login.
- AI comments (none in code; don’t mention the model).

Keep `PLAN.md` / `SPECS.md` / `DIAGRAMS.md` in the repo as annex. Reviewers who want depth can open them.

Email tip: bugs/gaps in `KNOWN_ISSUES.md`, not only README. Create that file on Day 4 (async import still sync, no row-pagination buttons, HTTP Basic until session, etc.).

---

## 7. GitHub for delivery

Push the **runnable app** + README + fixtures CSV. Do not need agent transcripts or `target/`. After Day 3–4, rewrite `README.md` from sections 1–5 above, then push.
