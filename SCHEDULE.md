# Principal review + 4-day execution

You already specified more than the PDF. That is good judgment. **Four working days is tight.** The scoring center is still: dirty CSV, no oversell, Docker, README. Everything else is cuttable if a day slips.

This file: what to build, in what order, what the PDF did **not** ask, and the enterprise cases that make you feel “something is missing” — as **decisions**, not as extra code.

Related: [PLAN.md](PLAN.md), [SPECS.md](SPECS.md), [DIAGRAMS.md](DIAGRAMS.md).

---

## 1. Verdict on our notes

| Area | Status | 4-day call |
|---|---|---|
| Dirty CSV + job report | Strong. This is the interview. | **Must** |
| Checkout lock + idempotency | Strong. | **Must** |
| Docker Compose three boxes | Required by PDF. | **Must** |
| Shopper signup + admin seed | More than the PDF. Keep it thin. | **Must** (login). Signup can be ugly. |
| Admin orders list | We added it. PDF does not require it. | **Should** if Day 3 finishes; else KNOWN_ISSUES |
| Typeahead dropdown | Same API as the grid. | Skip the dropdown; filtered grid is enough |
| Cart TTL / address / S3 / ES | Correctly out. | Stay out |
| JSON logs, request-id, Playwright | Polish. | Only leftover hours |

If you ship a perfect catalog import and a race-safe checkout, you pass. If you ship Magento-shaped infra and a flaky import, you fail.

---

## 2. Four days, step by step

Assume ~7–8 focused hours/day. End each day with something a reviewer can click.

### Day 1 — Skeleton that runs in Compose

**Morning**

1. Repo layout: `services/api`, `services/web`, `fixtures/` (copy of their CSV), `docker-compose.yml`.
2. API: Spring Boot **4.1.1**, Java 21, Flyway, `users` + `products` tables, health.
3. `AdminUserInitializer` from `APP_ADMIN_*`. SecurityFilterChain as in SPECS (permit catalog GET, protect `/api/admin/**`).
4. Product CRUD JSON (admin) + public `GET /api/products` (empty list OK). Entity includes `@Version`.

**Afternoon**

5. Web Dockerfile + nginx `/api` proxy. Shopper shell + `/admin` shell + login.
6. `docker compose up --build` must show the empty store on `localhost:8080`.

**Done when:** one command, login as admin, see empty catalog. No CSV yet.

### Day 2 — CSV (do not slip this)

1. `import_jobs` + `import_row_results`. `POST` 202 + `@Async` worker + poll.
2. Parser: header **by name**, extra columns ignored, stream, chunk 50–100, `BigDecimal` prices.
3. Tests against **their** file: traps, `RS-001` / `BS-021` last-wins, XSS/SQLi stored as text.
4. Admin UI: drop zone, 64KB preview, toast, job detail (new / updated / errors grouped).

**Done when:** upload their CSV in Docker, report matches SPECS, catalog is searchable as a raw list (even without debounce yet).

### Day 3 — Search + money

1. `GET /api/products?q=&category=&sort=` — `q` on name, description, category, sku; debounce 300ms; min 2 chars.
2. Cart API + checkout `@Transactional` + `SELECT FOR UPDATE` + fake pay always APPROVED + clear cart lines + `Idempotency-Key`.
3. Concurrent test: stock=1, two checkouts, one 201, one 409.
4. Shopper: detail, add to cart, Buy, order confirmation. Admin: `/admin/orders` **if time**.
5. Picsum-by-SKU + CSS fallback.

**Done when:** two browsers cannot both buy the last unit; README can describe the test.

### Day 4 — Make it look finished

1. Empty/loading/error, out-of-stock, XSS row renders as text, pagination.
2. Signup (if not done): email + password + session.
3. Clean `docker compose up --build` on a cold machine path.
4. English README: CSV date **2026-09-09**, decisions, growth table, how to login, how to import.
5. `KNOWN_ISSUES.md`. Strip AI comments. Push GitHub.

**If behind on Day 4 morning:** freeze features. Docs + Docker + tests beat a second UI page.

**Cut order if a day is lost**

1. Admin orders  
2. Suggestion dropdown  
3. Pretty import preview (still upload)  
4. Signup (seed a demo shopper in Flyway as well as admin)

---

## 3. PDF vs our spec — what was extra (keep or drop)

The PDF asked: local DB, product CRUD, CSV, search, fake purchase, UI, Docker, README (decisions + CSV date).

We added, all defensible: job-based import, shopper signup, Spring Security, BigDecimal, no-oversell lock, job detail tabs, ignore extra CSV columns.

Optional vs PDF: `/admin/orders`, typeahead, server cart (localStorage would still work). Keep server cart if Day 3 allows — checkout already has a user.

---

## 4. That feeling that something is missing

It is. A real enterprise shop is **many products**: catalog, pricing, tax, payments, fulfillment, returns, identity, fraud. The take-home is **one** slice: ingest a dirty catalog and sell units without going negative.

Do **not** build the rest. Put the list below in the README as “we would add when…”. That is principal-level. Building it in four days is not.

### Catalog

| Case | Question | Our default |
|---|---|---|
| Variants (size/color) | One SKU or parent+children? | CSV is flat SKU. One row = one sellable. |
| Draft / unpublished | Can shoppers see stock=0 and XSS rows? | Yes, all imported rows that passed validation are live. |
| Backorder / preorder | Sell when stock=0? | No. stock=0 → not purchasable, still listed. |
| Multi-warehouse | Which warehouse decrements? | One stock integer. |
| Bundles | Kit of SKUs? | No. |
| Digital goods | Gift card weight 0, empty category (we reject empty category). | Physical-like SKU only. |

### Pricing

| Case | Question | Our default |
|---|---|---|
| List vs sale price | Two prices? | One `price`. |
| Coupons / % off | Who authorizes? | No. |
| Tax / VAT | Inclusive? Per region? | No tax. Display USD as stored. |
| Multi-currency | CSV has `$` on one trap row. | Reject `$`; store USD. |
| Price 0 | Free item? | Yes (Mystery Box). |

### Cart and checkout

| Case | Question | Our default |
|---|---|---|
| Guest checkout | Buy without account? | No. Signup/login at cart. |
| Hold stock in cart | 10 min reserve? | No. Validate at pay. |
| Address / shipping | Zones, rates, delays? | No. |
| Min qty / max per customer | Fraud / drops? | Qty ≥ 1, ≤ current stock at pay. |
| Abandoned cart | Email? | No. Cart persists for the user. |
| Partial success | 2 of 3 items in stock? | All-or-nothing 409. No partial order. |

### Payments

| Case | Question | Our default |
|---|---|---|
| PSP + webhook | Decrement on webhook or on click? | Fake always APPROVED in the same TX. Real money: PENDING until webhook. |
| Refund / cancel | Restock? | No. Orders are PAID forever in v1. |
| Double click | | `Idempotency-Key`. |
| 3-D Secure / SCA | | No. |

### After the order (the big hole you are feeling)

This is **fulfillment**, not the challenge:

- Pick / pack / ship / tracking  
- Partial shipment  
- Invoice / credit note  
- Return / RMA / restock  
- Replace damaged unit  
- Customer support impersonation  

**Advice:** one README paragraph: “v1 ends at PAID. Fulfillment would add `orders.status` (PACKING, SHIPPED) without changing the stock lock.”

### Identity and risk

| Case | Question | Our default |
|---|---|---|
| Verify email | | No. |
| Password reset | | No. |
| GDPR export/delete | | KNOWN_ISSUES. |
| Admin audit (who changed price) | | `updated_at` only, not a full audit table. |
| Two admins edit the same SKU | Lost update? | **`@Version`**. Stale PUT → 409. Form reloads GET. |

`@Version` on `products` is **in spec** (Day 1 with the entity). Checkout still uses pessimistic lock for stock.

### Inventory (besides oversell)

- Returns restocking while someone checks out (lock still works).  
- CSV import **raising** stock while a checkout is in flight: importer should not use a long transaction that blocks checkout; chunk + row lock is OK.  
- Negative stock in CSV: we reject. Good.

---

## 5. Questions to “ask” in the README (even without an answer from them)

1. Duplicate SKU in file → last wins (we chose).  
2. Extra CSV columns → ignore (we chose).  
3. Guest checkout → no.  
4. Admin self-signup → no.  
5. Delete sold SKU → 409, hide via stock=0.  
6. Payment failure → we never reject in v1; model is ready for REJECTED.  
7. Search fields → name, description, category, sku.

That list is the “right questions” the PDF asked for.

---

## 6. Definition of done (ship even if ugly)

- [ ] `docker compose up --build` → `http://localhost:8080`  
- [ ] Admin logs in with Compose env password  
- [ ] Their CSV imports with a job report (errors grouped)  
- [ ] Search + category + price sort  
- [ ] Signup/login, cart, checkout, no negative stock under a race test  
- [ ] README: date **2026-09-09**, decisions, how to run  
- [ ] `KNOWN_ISSUES.md`  
- [ ] No AI comments  

If that box is green, stop adding enterprise modules.
