# Diagrams (v1)

Companion to [PLAN.md](PLAN.md) and [SPECS.md](SPECS.md). Open the mermaid blocks in GitHub or any mermaid renderer.

**Auth:** shopper sign up + sign in. Admin is seeded. **No shipping address** in v1.

---

## 1. System (Docker)

```mermaid
flowchart LR
  Browser["Browser"]
  Web["web nginx :80\nReact SPA"]
  Api["api Spring Boot :8080"]
  Db["db PostgreSQL :5432"]

  Browser -->|"localhost:8080 /"| Web
  Browser -->|"localhost:8080 /api/*"| Web
  Web -->|"proxy_pass http://api:8080"| Api
  Api -->|"jdbc://db:5432/shop"| Db
```

Roles are **not** containers. One `api` serves SHOPPER and ADMIN.

---

## 2. Frontend views

```mermaid
flowchart TB
  subgraph public [Public]
    Home["/ catalog"]
    Detail["/products/:sku"]
    Login["/login"]
    Signup["/signup"]
  end

  subgraph shopper [After login]
    Cart["/cart"]
    Checkout["/checkout"]
    Orders["/orders"]
  end

  subgraph admin [role ADMIN]
    Imports["/admin and /admin/imports"]
    Products["/admin/products"]
    PNew["/admin/products/new"]
    PEdit["/admin/products/:sku"]
    Job["/admin/imports/:jobId"]
    AOrders["/admin/orders"]
  end

  Home --> Detail
  Detail -->|"login required"| Cart
  Cart --> Checkout
  Checkout --> Orders
  Home --> Login
  Login --> Signup
  Signup --> Home
  Imports --> Products
  Products --> PNew
  Products --> PEdit
  Imports --> Job
  Imports --> AOrders
```

`/admin` redirects to **CSV import**. Shopper hitting `/admin` → redirect `/`. Admin can also use the storefront. Header search + category sit on the catalog, not on a second toolbar.

---

## 3. Database: users, roles, permissions

v1 does **not** have `permissions` or `user_roles` join tables. Two values on `users.role` are enough. Permissions are implied in Spring `hasRole`.

```mermaid
erDiagram
  users {
    uuid id PK
    varchar email UK
    varchar password_hash
    varchar display_name
    varchar role "SHOPPER | ADMIN"
    timestamptz created_at
  }

  products {
    uuid id PK
    varchar sku UK
    varchar name
    text description
    varchar category
    numeric price
    int stock
    numeric weight_kg
    varchar origin "MANUAL | CSV"
    timestamptz created_at
    timestamptz updated_at
  }

  import_jobs {
    uuid id PK
    uuid created_by FK
    varchar filename
    varchar status
    int inserted
    int updated
    int failed
    int skipped
    timestamptz started_at
    timestamptz finished_at
  }

  import_row_results {
    uuid id PK
    uuid job_id FK
    int line_number
    varchar sku
    varchar outcome "INSERTED | UPDATED | FAILED | SKIPPED"
    varchar code
    text message
  }

  carts {
    uuid id PK
    uuid user_id UK
  }

  cart_items {
    uuid id PK
    uuid cart_id FK
    varchar sku
    int qty
  }

  orders {
    uuid id PK
    uuid user_id FK
    varchar status
    varchar idempotency_key
    numeric total
    timestamptz created_at
  }

  order_items {
    uuid id PK
    uuid order_id FK
    uuid product_id FK
    varchar sku
    varchar name_snapshot
    numeric unit_price
    int qty
  }

  users ||--o{ import_jobs : creates
  import_jobs ||--o{ import_row_results : has
  users ||--|| carts : has
  carts ||--o{ cart_items : has
  users ||--o{ orders : places
  orders ||--o{ order_items : contains
  products ||--o{ order_items : "RESTRICT delete"
```

Conceptual permissions (code, not a table):

| Role | catalog read | cart/checkout | product write | CSV import |
|---|---|---|---|---|
| anonymous | yes | no | no | no |
| SHOPPER | yes | own | no | no |
| ADMIN | yes | own | yes | yes |

Later: `roles`, `permissions`, `role_permissions` if we have more than two actors.

---

## 4. API flows

### Sign up / sign in

```mermaid
sequenceDiagram
  actor U as Browser
  participant W as nginx
  participant A as api
  participant DB as db

  U->>W: POST /api/auth/signup
  W->>A: proxy
  A->>DB: INSERT users role=SHOPPER
  A-->>U: Set-Cookie JSESSIONID
  U->>W: GET /api/auth/me
  A-->>U: role SHOPPER
```

Admin never uses signup. Login with seeded `admin@shop.local`.

### Import CSV

```mermaid
sequenceDiagram
  actor Admin
  participant A as api
  participant DB as db

  Admin->>A: POST /api/admin/imports (file)
  A-->>Admin: 202 jobId
  A->>DB: INSERT import_jobs RUNNING
  loop each CSV row
    A->>DB: INSERT or UPDATE products
    A->>DB: INSERT import_row_results
  end
  A->>DB: job COMPLETED
  Admin->>A: GET /api/admin/imports/id (poll)
  Admin->>A: GET .../rows?outcome=FAILED
```

### Checkout (happy path)

```mermaid
sequenceDiagram
  actor U as Shopper
  participant A as api
  participant DB as db
  participant Pay as Fake payment

  U->>A: PUT /api/cart/items
  U->>A: POST /api/checkout Idempotency-Key
  A->>DB: BEGIN
  A->>DB: SELECT products FOR UPDATE
  alt stock ok
    A->>Pay: charge(total)
    Pay-->>A: APPROVED
    A->>DB: stock = stock - qty
    A->>DB: INSERT orders PAID
    A->>DB: DELETE cart_items for those SKUs
    A->>DB: COMMIT
    A-->>U: 201 PAID
  else not enough stock
    A->>DB: ROLLBACK
    A-->>U: 409 INSUFFICIENT_STOCK
  end
```

---

## 5. Concurrent purchase (last unit)

Product stock = 1. Two shoppers, same SKU, both click Buy.

```mermaid
sequenceDiagram
  participant A as Shopper A
  participant B as Shopper B
  participant Api as api
  participant DB as products row

  A->>Api: POST /checkout qty=1
  B->>Api: POST /checkout qty=1
  Api->>DB: A SELECT FOR UPDATE
  Note over DB: B waits on the row lock
  Api->>DB: A stock 1 >= 1, payment OK, stock=0, COMMIT
  Api->>DB: B SELECT FOR UPDATE (now stock=0)
  Api-->>B: 409 INSUFFICIENT_STOCK
  Api-->>A: 201 PAID
```

- Cart is **not** a reservation. Both could have qty 1 in the cart.
- The loser keeps the item in the cart; UI shows the 409 and they must change qty or remove.
- No oversell: lock + `stock >= qty` inside one transaction.
- Same `Idempotency-Key` twice → one order, not two.

**No address step** between cart and pay. Flow: cart → confirm (lines + total) → Buy → fake pay → order. Adding street/zip is fulfillment, not this challenge.

---

## 6. Search (not Elasticsearch)

```mermaid
sequenceDiagram
  actor U as Browser
  participant UI as React
  participant A as api
  participant DB as Postgres

  U->>UI: type "sh"
  Note over UI: length 1 — do not send q
  U->>UI: type "sho"
  Note over UI: debounce 300ms
  UI->>A: GET /api/products?q=sho&category=&sort=name_asc&page=0&size=24
  A->>DB: ILIKE on name, description, category, sku
  A-->>UI: items + total
  UI-->>U: catalog grid
```

`q` matches name, description, category, and sku. Category is an exact match in the same header control as search. The API still accepts `sort`; the storefront uses the default `name_asc`. Out-of-stock products still appear. The grid is the search UI (no separate typeahead list).

---

## 7. Extra CSV columns

Header is mapped **by name**. Extra columns (`image_url`, …) are skipped. Missing `sku` / `price` / … → `FAILED_HEADER`.

---

## 8. Spring Boot startup

```mermaid
flowchart TB
  main["1 ShopApplication.main"]
  ctx["2 ApplicationContext\napplication.yml + autoconfig"]
  fly["3 Flyway: V1__init.sql"]
  jpa["4 JPA: User entity + UserRepository"]
  sec["5 SecurityConfig + UserDetailsService"]
  seed["6 ApplicationRunner\nAdminUserInitializer"]
  http["7 Tomcat :8080\n/actuator/health"]

  main --> ctx --> fly --> jpa --> sec --> seed --> http
```

Flyway runs **before** `AdminUserInitializer`. If SQL fails, the admin is never inserted.


