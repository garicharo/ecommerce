# Shop

Enterprise-style take-home: catalog CRUD, CSV import, search, fake checkout, Docker Compose.

**Example CSV downloaded:** 2026-09-09

This repository is the `ecommerce/` project. Implementation follows the 4-day schedule; application code (Spring Boot + React) is not started yet.

## Docs

| File | What |
|---|---|
| [SCHEDULE.md](SCHEDULE.md) | 4-day build order |
| [SPECS.md](SPECS.md) | Frozen contract |
| [PLAN.md](PLAN.md) | Why those choices |
| [DIAGRAMS.md](DIAGRAMS.md) | Flows |
| [AGENTS.md](AGENTS.md) | Agent instructions |

## Run (when Day 1 lands)

```bash
docker compose up --build
```

Then open http://localhost:8080

Admin (planned): `admin@shop.local` / `admin1234` from Compose env, not the Postgres password.

## Challenge files

- `Code Challenge E-Commerce.csv`
- `Code Challenge..pdf`
