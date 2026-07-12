# SpecGuard MVP — Phase 1: Foundation

Spring Boot monolith. Modules are packages, not services: `repo/`, `governance/`,
`workflow/`, `dashboard/`, `copilot/`, plus `auth/` and `config/`. No event bus,
no multi-tenant enforcement, no Kubernetes — see the LOE doc for what's deferred.

## What's in this phase

- [x] App skeleton with domain-module package structure
- [x] Postgres schema + Flyway migration (`users` table, `tenant_id` stubbed/unused)
- [x] Email/password auth issuing JWTs, stateless Spring Security filter chain
- [x] Config/secrets loaded from env vars via `.env` (no vault)
- [x] `/actuator/health` (wired now, doubles as the Phase 7 health endpoint)

Not in this phase: webhook receiver, analyzers, workflow automation, dashboard,
copilot, containerized app image. Those are Phases 2–7.

## Run it locally

1. **Start Postgres** (dev-only compose, just the DB):
   ```bash
   docker compose -f docker-compose.dev.yml up -d
   ```

2. **Set up secrets**:
   ```bash
   cp .env.example .env
   # at minimum, replace JWT_SECRET with a real random value:
   # openssl rand -base64 48
   ```

3. **Run the app** (Flyway migrates the schema automatically on boot):
   ```bash
   ./mvn spring-boot:run
   ```
   (No `mvnw` wrapper committed yet — use your local `mvn` for now, or ask me to add
   the wrapper jar in a follow-up.)

4. **Check health**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

## Try auth

Register:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"a-real-password"}'
```

Login:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"a-real-password"}'
```

Both return `{"token": "...", "email": "..."}`. Use the token on any future
protected endpoint:
```bash
curl http://localhost:8080/api/whatever -H "Authorization: Bearer <token>"
```

## Notes / decisions made in this phase

- **`tenant_id`** exists on `users` from day one (defaulted, indexed, never enforced).
  This is the "future-proof but don't build enforcement yet" move from the LOE —
  a real multi-tenant migration later becomes a backfill-free refactor.
- **`spring.jpa.hibernate.ddl-auto: validate`** — Flyway owns the schema, Hibernate
  never auto-generates DDL. Every future column/table change is a new
  `V{n}__description.sql` file in `db/migration`.
- **`spring-dotenv`** loads `.env` at boot so this behaves like a standard `.env`
  workflow without a vault dependency, per the LOE's explicit scope cut.
- Webhook and other unauthenticated integration endpoints are pre-permitted in
  `SecurityConfig` (`/api/webhooks/**`) since those will use HMAC signature
  verification instead of JWT, not because they're unauthenticated.

## Next: Phase 2 — Core Pipeline

GitHub webhook receiver + changed-file fetch + OpenAPI diff (wrap oasdiff) +
policy engine + violation storage + risk scoring.
