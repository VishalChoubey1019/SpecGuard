# SpecGuard

Engineering governance platform: checks GitHub PRs against policy rules (API contract
breaks so far; SQL/Dockerfile/PR-metadata analyzers not yet built) and stores violations.
Solo build (+ one collaborator on `dashboard`), Spring Boot monolith, single Postgres
schema. Full scope/priority/hours: `SpecGuard_Solo_LOE.xlsx` (not in this repo — ask for
it if missing). Longer-form session history: `HANDOFF.md`, if present.

## Commands

- Build: `mvn compile`
- Test: `mvn test` — pure Mockito unit tests only right now, no `@DataJpaTest`/Testcontainers
  dependency exists yet, so nothing here starts a real DB.
- Run: `mvn spring-boot:run` — **requires Postgres already running** (`docker-compose.dev.yml`
  if present) and `.env` populated. `spring.jpa.hibernate.ddl-auto=validate` — Hibernate
  never creates tables, so Flyway must successfully apply `V1`...`Vn` on boot or the app
  won't start. Compiling clean does NOT mean this will boot clean.
- DB shell: `psql "postgresql://specguard:specguard@localhost:5432/specguard"`

## Architecture

Packages are per-domain, not per-service, despite the exec-summary doc describing separate
microservices — this is a monolith by deliberate scope decision:
- `auth` — register/login/JWT (`JwtAuthFilter` sets `Authentication.principal` to the real
  `User` entity, not a username string)
- `repo` — GitHub webhook intake, PR tracking, `GithubClient` (real API calls, not mocked)
- `governance` — analyzers (only `openapi` built so far), `Violation` storage, risk scoring
- `workflow` — Jira/Slack/PR-comment automation. Schema exists (`V5`); trigger logic and API
  clients do not yet.
- `dashboard` — **owned by a teammate working in parallel.** Check with the user before
  editing files here; don't assume ownership the way you would for `auth`/`repo`/`governance`.
- `config` — `@ConfigurationProperties` classes only
- `copilot` — not started

## Database

- Flyway-owned, `src/main/resources/db/migration/V{n}__description.sql`, sequential, never
  edit a migration that's already been applied anywhere.
- Every table has a stubbed, unenforced `tenant_id` column defaulting to the zero UUID —
  keep this pattern on new tables even though nothing reads it yet (cheap now, expensive to
  backfill later).
- No `repositories` table yet — `pull_requests.repo_full_name` is a free-text string, not a
  foreign key. There is currently no way to answer "which repos can this user see."

## Code conventions

- **Real naming, not the exec-summary doc's naming**: `GithubClient`/`GithubWebhookController`/
  etc. (capital G-i-t-h-u-b, not `GitHub`), `PullRequestRepo`/`ViolationRepo` (not
  `...Repository`). Grep the actual file before writing code that references a class name —
  this codebase has drifted from earlier docs before, silently.
- **Under 200 lines of code change per delivery.** Bigger tasks: split into steps, stop after
  each.
- Javadoc on every public class and method in new files.
- **Ponytail minimalism** — before writing custom code, in order, stop at the first that
  applies: (1) YAGNI, (2) existing code/util already does this, (3) standard library, (4)
  platform-native, (5) an already-installed dependency, (6) one-liner/config change. Only
  write custom code if all fail, and then the bare minimum.
- **Wrap, don't hand-roll** — governance analyzers shell out to real tools (oasdiff, later
  Squawk/Hadolint) via `ExternalProcessRunner`, never reimplement them.
- **Verify, don't guess, on external schemas and Spring Data query names.** Spring Data
  derived query methods (`findBy...`) are resolved against the entity's real fields at
  **application startup**, not compile time — a name like `findByRepoFullName` on
  `ViolationRepo` compiles fine and then throws `PropertyReferenceException` on boot, because
  `Violation` has no such field directly, only via `pullRequest.repoFullName` (needs
  `findByPullRequest_RepoFullName`, underscore marks the traversal). Check the real entity's
  fields before naming one of these; `mvn compile` will not save you.

## Security

- `/api/auth/**` is **entirely `permitAll()`** in `SecurityConfig` (needed for
  `/register`/`/login`). Never add a new endpoint under that path prefix without checking —
  anything new there is publicly reachable with no JWT. New authenticated endpoints go
  elsewhere (e.g. `/api/users/**`), which falls under the default `anyRequest().authenticated()`.
- `JWT_SECRET` has no safe default — the app boots fine without one, using a placeholder that
  is not safe for anything real. Missing env vars fail silently here, not loudly.
- Never commit `.env`. If a real-looking GitHub PAT shows up in one during a session, flag it
  and confirm rotation before treating it as safe to reference again.

## Testing patterns already in use

- Pure Mockito, no Spring context, for anything with mockable collaborators (`AuthServiceTest`,
  `RiskScoreCalculatorTest`, `OpenApiDiffAnalyzerTest`).
- Real (non-mocked) execution for classes with no real external collaborators —
  `JwtServiceTest` uses real HS256 sign/verify, no point mocking a pure function.
- Servlet filters (`JwtAuthFilterTest`): mock `HttpServletRequest`/`Response`/`FilterChain`
  directly (all interfaces) and call the real inherited `doFilter()` — no MockMvc needed.
  `SecurityContextHolder` is a JVM-wide thread-local; clear it in both `@BeforeEach` and
  `@AfterEach` or it leaks into unrelated tests on the same thread.
- Thin controllers: call methods directly, no MockMvc/`@WebMvcTest` — if a controller just
  delegates to a service plus a couple of `@ExceptionHandler`s, those are plain Java methods.

## Principle
Use the ponytail formula
You are an ultra-minimalist Senior Developer operating under the strict "Ponytail" discipline. Your primary goal is to write the absolute least amount of code possible. You prioritize deleting over writing, and reusing over creating. Before you write a single line of custom code for my request, you MUST explicitly evaluate the "Ponytail Decision Ladder" in your thinking process. Stop at the first step that applies: 1. YAGNI Check: Does this feature actually need to exist right now to solve the core problem? If not, reject it. 2. Codebase Check: Can this be solved by reusing an existing helper, utility, or pattern? 3. Standard Library Check: Does the programming language have a native, built-in feature or method that does this? 4. Platform Check: Does the underlying platform (e.g., native HTML5/CSS, browser APIs, iOS/Android SDKs) already handle this natively without JavaScript/code? 5. Dependency Check: Can an already-installed, standard third-party package handle this? 6. One-Line Check: Can this entire requirement be compressed safely into a single line or a pure configuration change? OUTPUT FORMAT: - If a step applies: Explain which step of the ladder solves the problem, provide the minimal native solution or one-liner, and STOP. Do not generate an over-engineered implementation. - If all steps fail: Only then, provide the bare-minimum custom code required. No extra "just-in-case" features, no fluff, no massive boilerplate.