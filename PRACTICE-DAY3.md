# Day 3 — you write search + checkout

CSV is done. I will **not** write the search query, `CheckoutService`, or the race test. You write those. Ask when stuck; ask for a review when a step compiles.

Contract: [SPECS.md](SPECS.md) §3.2 (search), §3.5 (cart/checkout), §5 (tables), §9 (tests).  
Manual checks: [MANUAL_TEST.md](MANUAL_TEST.md) §3 and §6.

Keep HTTP Basic for now (admin can shop). Signup/session is Day 4 unless you want it earlier.

Work **in this order**. After each step, hit the API with `curl` (or `mvn test` once tests exist).

---

## 1. Flyway — you create

`services/api/src/main/resources/db/migration/V3__commerce.sql`

Tables from SPECS §5: `carts`, `cart_items`, `orders`, `order_items`.

Must have:

- `carts.user_id` unique
- `cart_items (cart_id, sku)` unique; `qty > 0`
- `orders (user_id, idempotency_key)` unique; `total >= 0`
- `order_items.product_id` **ON DELETE RESTRICT**
- snapshot columns on `order_items`: `sku`, `name_snapshot`, `unit_price`, `qty` (and `line_total` if you want)

Postgres only. UUID PKs, `numeric` money, `timestamptz`. Copy style from `V1__init.sql`.

---

## 2. Search — you change existing catalog files

This is half of Day 3. Do it before cart.

| File | What you add |
|---|---|
| `catalog/ProductRepository.java` | Query: `q` ILIKE on **name, description, category, sku**; optional exact `category`; ignore `q` if null or length `< 2`. |
| `catalog/ProductController.java` | `GET /api/products?q=&category=&sort=&page=&size=`. `GET /api/products/categories` (distinct, sorted). Put **categories before `{sku}`** or Spring will treat `categories` as a SKU. |
| `shared/` (small DTO) | List envelope `{ items, page, size, total }` — not Spring’s `content` / `totalElements`. |

Rules (SPECS §3.2):

- `sort`: `name_asc` (default) \| `price_asc` \| `price_desc`
- `page` 0-based; `size` default 20, **max 50**
- Category filter and sort work with empty `q`

Then fix the shop list: `index.html` and `admin.html` currently read `page.content`. Switch them to `items`.

**Check:** [MANUAL_TEST.md](MANUAL_TEST.md) §3. `q=s` must not filter; `q=sh` must.

---

## 3. Commerce entities — you create the package

New package: `com.garicharo.shop.commerce`

| File | What |
|---|---|
| `Cart.java` / `CartItem.java` | One cart per user. Lines are SKU + qty. Adding does **not** change stock. |
| `Order.java` / `OrderItem.java` | Status `PENDING_PAYMENT` / `PAID` / `REJECTED` / `FAILED`. Snapshots on lines. |
| Repositories | `findByUserId`, `findByUserIdAndIdempotencyKey`, etc. |

---

## 4. Cart API — you write

| File | What |
|---|---|
| `CartService.java` | `@Transactional` writes. `GET` cart; `PUT { sku, qty }` (`qty=0` removes). Unknown SKU → 404. |
| `CartController.java` | `GET /api/cart`, `PUT /api/cart/items`. Auth required (Basic is fine). |

Do not decrement stock here.

---

## 5. Checkout — you write this (the interview)

One `@Transactional` method. Failure rolls back. No leftover `PAID` order with stock unchanged.

| File | What |
|---|---|
| `catalog/ProductRepository.java` | `SELECT … FOR UPDATE` by SKU list (`LockModeType.PESSIMISTIC_WRITE`). |
| `commerce/PaymentClient.java` | Fake. **Always APPROVED** in v1. |
| `commerce/CheckoutService.java` | The flow below. |
| `commerce/CheckoutController.java` | `POST /api/checkout`. Header `Idempotency-Key` required. |
| `shared/ApiException` (or subclass) | `INSUFFICIENT_STOCK` with `details: [{ sku, requested, available }]`. Handler must return those details, not always `[]`. |

Flow (PLAN “Approach B”, DIAGRAMS §5):

1. Same `Idempotency-Key` + same user → return original order, **200**, do not charge again. Different body, same key → **409**.
2. Empty cart → **400** `CART_EMPTY`.
3. Lock product rows. If any `stock < qty` → **409**, **no** partial order, stock unchanged.
4. Fake pay APPROVED.
5. Decrement stock, insert `PAID` order + line snapshots, **clear cart lines**.
6. Success → **201** `{ orderId, status: "PAID" }`.

Also: `GET /api/orders`, `GET /api/orders/{id}` (404 if not owner). Admin `/admin/orders` only if you have time (SCHEDULE: first cut).

---

## 6. Tests — you write (must exist)

`services/api/src/test/java/...` — Testcontainers Postgres is already on the classpath (I added the deps). Use the real DB, not H2.

| Test | Assert |
|---|---|
| Search | `q` length 1 ignored; matches name/description/category/sku |
| Checkout race | `stock=1`, two parallel checkouts → one **201**, one **409**, stock=0 |
| Idempotency | two POSTs, same key → one order |

---

## 7. Shopper UI — you write after the API works

Keep the static HTML (same as Day 1–2). nginx already rewrites `/cart`, `/checkout`, `/orders`, `/products/*`.

| Page | What |
|---|---|
| `index.html` | Search box debounce **300ms**, send `q` only if length ≥ 2. Category + sort without `q`. Cards: picsum `https://picsum.photos/seed/{sku}/600/600`, CSS fallback `onerror`. Out of stock: badge, still listed. |
| `product.html` | `/products/:sku` — description, price, weight, stock. Add to cart disabled if stock=0. |
| `cart.html` | Lines, qty capped at displayed stock (server still wins). |
| `checkout.html` | Confirm + **Buy**. One click → one `Idempotency-Key`. Disable button while in-flight. 409 shows **per SKU**. Do not empty the cart on 409. |
| `orders.html` | Confirmation / own orders. |

No `dangerouslySetInnerHTML` equivalent: `textContent` only (XSS row must display, not run).

---

## Check (Day 3 done)

- [ ] Search + category + price sort (MANUAL_TEST §3)
- [ ] Add to cart does not change stock
- [ ] Checkout decrements stock, cart empty, order has snapshots
- [ ] Race: two buyers, last unit, no negative stock
- [ ] Same `Idempotency-Key` twice → one order

Do **not** build: Elasticsearch, cart reservation/TTL, real payments, shipping address, typeahead dropdown.

When you are stuck, ask. When you want a review, say which files you wrote.
