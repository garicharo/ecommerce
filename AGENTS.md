# Agent instructions (this repo)

You are implementing the take-home in this folder. Follow:

1. [PLAN.md](PLAN.md) — why
2. [SPECS.md](SPECS.md) — frozen contract
3. [SCHEDULE.md](SCHEDULE.md) — 4-day build + enterprise gaps
4. [DIAGRAMS.md](DIAGRAMS.md) — flows
4. `.cursor/rules/` — backend, frontend, database, challenge scope

Do not invent Elasticsearch, S3, or extra microservices. No AI comments in code.

**Admin (HTTP):** `APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD` (Compose + README). Seeded on API boot if no ADMIN row exists.

**Postgres:** `SPRING_DATASOURCE_*` — different secret. Frontend never receives it.
