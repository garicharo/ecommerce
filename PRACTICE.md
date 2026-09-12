# Day 1 — you write these classes

Scaffold is in `services/api` and `services/web`. `ShopApplication`, Compose, and a **minimal** `SecurityConfig` (health only + BCrypt bean) are already there. Do **not** add AI comments. Contract: [SPECS.md](SPECS.md).

Work in order. After each step, `cd services/api && ./mvnw -q test` (or `mvn`) once the wrapper exists; until then `docker compose up --build` can wait until step 5.

## 1. Flyway — you create the file

`services/api/src/main/resources/db/migration/V1__init.sql`

Tables: `users`, `products` as in SPECS §5. Include `products.version BIGINT NOT NULL DEFAULT 0`. No bcrypt hash in SQL.

## 2. Identity — you create these files

| File | What |
|---|---|
| `identity/Role.java` | Enum `SHOPPER`, `ADMIN` |
| `identity/User.java` | JPA entity: id UUID, email unique, passwordHash, displayName, role, createdAt |
| `identity/UserRepository.java` | `Optional<User> findByEmail(String email)` |
| `identity/ShopUserDetailsService.java` | `UserDetailsService`: load by email, `ROLE_` + role name |
| `identity/AdminUserInitializer.java` | `ApplicationRunner`: if no ADMIN, insert from `app.admin.email` / `password` (BCrypt). Do not overwrite an existing admin. |

Then **expand** `SecurityConfig` using SPECS §2.1 (`/api/auth/**`, `GET /api/products/**` permitAll, `/api/admin/**` hasRole ADMIN, etc.). Add JSON login later if you want; Day 1 can be HTTP basic for admin CRUD.

## 3. Catalog — you create these files

| File | What |
|---|---|
| `catalog/ProductOrigin.java` | Enum `MANUAL`, `CSV` |
| `catalog/Product.java` | sku, name, description, category, **BigDecimal** price, stock, weightKg, origin, `@Version` version, createdAt, updatedAt |
| `catalog/ProductRepository.java` | `Optional<Product> findBySku(String sku)` |
| `catalog/ProductController.java` | Public `GET /api/products`, `GET /api/products/{sku}`. Admin: `POST/PUT/DELETE /api/admin/products` per SPECS. PUT requires `version` → 409 on stale. |

Keep controllers thin. Validation on DTOs, not on the entity if you prefer.

## 4. Shared (optional Day 1)

`shared/ApiException.java` + `@RestControllerAdvice` mapping to `{ code, message, details }` and HTTP 400/404/409.

## Check

- [ ] `docker compose up --build` → http://localhost:8080  
- [ ] `GET /actuator/health` → 200  
- [ ] Admin can POST a product; `GET /api/products` lists it  
- [ ] No `double` for money  

CSV import is **Day 2**. Search + checkout is **Day 3** — you write those; see [PRACTICE-DAY3.md](PRACTICE-DAY3.md).

When you are stuck, ask. When you want a review, say which files you wrote.
