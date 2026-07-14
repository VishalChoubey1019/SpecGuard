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
   mvn spring-boot:run
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

## Test the violation pipeline end-to-end

<!-- TODO: once PullRequestService dedups violations per PR, update step 3 — re-sending
     the same webhook currently inserts duplicate violation rows. -->

When a pull request is opened on GitHub, GitHub delivers a `pull_request` event to
`POST /api/webhooks/github`. SpecGuard verifies the delivery's HMAC signature, saves the
pull request, fetches the PR's changed files from GitHub, runs `oasdiff` on any modified
OpenAPI spec files, and stores each breaking change it finds as a violation. Testing
locally means playing GitHub's role: sending that same signed event by hand, then
reading the result back through the dashboard API.

To follow along, there needs to be a real PR (in a repo the configured `GITHUB_APP_TOKEN`
can read) that modifies a file matching `*openapi*`/`*swagger*.{yml,yaml,json}` with a
breaking change in it — for example changing a response property's type. `oasdiff` must
be installed and on `PATH`.

### 1. Send the webhook (`POST /api/webhooks/github`)

This endpoint is authenticated by an HMAC-SHA256 signature in the `X-Hub-Signature-256`
header, not by a JWT. GitHub computes that signature over the exact payload bytes using
the shared webhook secret, so the local test does the same with `openssl`:

```bash
SECRET=local-test-secret-123   # must match GITHUB_WEBHOOK_SECRET in .env
PAYLOAD='{"action":"opened","pull_request":{"id":1,"number":1,"title":"test breaking change","user":{"login":"your-username"},"base":{"sha":"BASE_SHA"},"head":{"sha":"HEAD_SHA"},"merged":false},"repository":{"full_name":"your-org/your-repo"}}'
SIG="sha256=$(echo -n "$PAYLOAD" | openssl dgst -sha256 -hmac "$SECRET" | sed 's/^.* //')"

curl -s -w "\nHTTP %{http_code}\n" -X POST http://localhost:8080/api/webhooks/github \
  -H "X-GitHub-Event: pull_request" \
  -H "X-Hub-Signature-256: $SIG" \
  -H "Content-Type: application/json" \
  -d "$PAYLOAD"
```

`full_name`, the PR `number`, and the `base`/`head` commit SHAs come from the real PR
(`gh pr view --json number,baseRefOid,headRefOid`).

The response is `HTTP 200` with an empty body. A `200` only means the event was received
and the signature matched — analysis failures are logged and swallowed on purpose so
GitHub doesn't endlessly retry a payload that can't be fixed. The app log confirms the
analysis actually ran with a `Recorded N violation(s)` line. A `401` means the signature
didn't match the secret.

### 2. Get a token (`POST /api/auth/login`)

The dashboard endpoints require a JWT, so the next step is logging in (register first if
needed) and grabbing the `token` from the response:

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"you@example.com","password":"a-real-password"}'
```

### 3. Read the violations (`GET /api/dashboard/violations`)

```bash
curl -s http://localhost:8080/api/dashboard/violations \
  -H "Authorization: Bearer <token>"
```

Each breaking change oasdiff found comes back as one entry:

```json
[
  {
    "id": "f8e477ad-d7f2-41e9-956a-f266f72eef95",
    "pullRequestId": "c55b8cd1-bed5-429d-a869-7114b668779a",
    "category": "API_CONTRACT",
    "severity": "CRITICAL"
  }
]
```

An empty list `[]` means no violation was recorded — usually oasdiff isn't on `PATH`,
the changed file didn't match the OpenAPI filename pattern, or the file was `added`
rather than `modified` (a new spec has no prior version to break). The app log tells
which. Re-sending the same webhook re-runs the analysis and inserts new rows rather
than replacing the old ones, so firing it twice shows duplicates.

Violations can also be scoped to a single PR
(`GET /api/dashboard/pull-requests/{pullRequestId}/violations`) or filtered by severity
(`GET /api/dashboard/violations/severity/CRITICAL`).

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
