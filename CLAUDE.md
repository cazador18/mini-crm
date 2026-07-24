# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project purpose

Learning MVP: a mini-CRM with clients and tasks. Goal is to practice Claude Code features (skills, subagents, MCP, slash commands) — not to build a production system.

## Stack

- **Backend**: Java 21, Spring Boot 3.3, Spring Data JPA, Bean Validation — in `backend/`
- **Frontend**: Next.js 14 (App Router) + TypeScript + Tailwind — in `frontend/`
- **DB**: PostgreSQL 16 (via Docker); H2 in-memory for local dev and tests

## Commands

```bash
# Full stack (Docker)
docker compose up --build

# Backend — local dev (H2 in-memory, no Docker needed)
cd backend && ./mvnw spring-boot:run

# Backend — all tests
cd backend && ./mvnw test

# Backend — single test class
cd backend && ./mvnw test -Dtest=ClientServiceImplTest

# Backend — single test method
cd backend && ./mvnw test -Dtest=ClientServiceImplTest#createClient_shouldReturnDto

# Frontend — local dev
cd frontend && npm run dev

# Frontend — lint
cd frontend && npm run lint
```

## Spring profiles

| Profile            | Datasource | DDL          | Flyway | When used                          |
|--------------------|------------|--------------|--------|-------------------------------------|
| `dev`              | H2 in-mem  | `update`     | off    | Default for local run              |
| `docker`           | PostgreSQL | `validate`   | on     | Docker Compose                     |
| `test`             | H2 in-mem  | `create-drop`| off    | `./mvnw test` (unit tests)         |
| `integration-test` | PostgreSQL (Testcontainers) | `validate` | on | IT controller tests, real Flyway migrations |

`docker`/`integration-test` never auto-generate schema — they run the versioned migrations in
`db/migration/` and `validate` only checks Hibernate's entity mappings match. `dev`/`test` stay on
H2 with Hibernate-managed DDL for a fast local loop; Flyway is explicitly disabled there
(`spring.flyway.enabled: false`) so the Postgres-flavored migration SQL never runs against H2.

H2 console available at `http://localhost:8080/h2-console` when running with `dev` profile.

### Running integration tests (Testcontainers) via Docker on macOS

```bash
# Requires Docker Desktop running. On macOS, ports are on the host — use host.docker.internal:
docker run --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $(pwd)/backend:/app \
  -w /app \
  -e TESTCONTAINERS_RYUK_DISABLED=true \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  maven:3.9-eclipse-temurin-21 \
  mvn test -Dtest="ClientControllerIT,TaskControllerIT" --no-transfer-progress
```

## Architecture

Strict layered architecture — never bypass a layer:

```
Controller → Service (interface + impl) → Repository → Entity
                ↓
              DTOs  (never expose Entity directly from controllers)
```

- **Controllers** (`controller/`): HTTP mapping only, no business logic. Call service methods, return DTOs.
- **Services** (`service/`): all business logic. Define as interface + impl pair (e.g. `ClientService` / `ClientServiceImpl`).
- **Repositories** (`repository/`): Spring Data JPA only. No custom SQL unless JPA can't handle it.
- **Models** (`model/`): JPA entities — `Client`, `Task`, enums `TaskStatus` (NEW/IN_PROGRESS/DONE), `TaskPriority` (LOW/MEDIUM/HIGH).
- **DTOs** (`dto/`): separate request/response objects. Entities stay in the service layer.
- **Exceptions** (`exception/`): custom exceptions + `@ControllerAdvice` for global error handling.
- **Config** (`config/`): CORS, Spring Security filter chain, and other Spring configuration.
- **Security** (`security/`): JWT issuing/parsing, the JWT auth filter, `CustomUserDetailsService`, REST-friendly 401/403 handlers (`RestAuthenticationEntryPoint`/`RestAccessDeniedHandler`), plus the RBAC helpers `CurrentUserService` (reads the authenticated `User` off `SecurityContextHolder` — inject this in services instead of calling `SecurityContextHolder` directly, it's what makes ownership logic mockable in `*ServiceImplTest`) and `OwnershipGuard` (`check(currentUser, client)` — throws `AccessDeniedException` when a MANAGER isn't the resource's owner; ADMIN/VIEWER always pass).

## Domain model

- `Client`: id, name, email, phone, createdAt (Instant), owner (ManyToOne → User, set server-side to the
  creating user — never accepted from the client in `ClientRequest`)
- `Task`: id, title, description, status, priority, deadline (LocalDate), client (ManyToOne → Client)
- `Note`: id, content, createdAt (Instant), client (ManyToOne → Client) — generated via the `/crud-generator` skill; use it as the reference example when adding a new entity of this shape
- `User`: id, username, email, passwordHash, role (`UserRole`: ADMIN/MANAGER/VIEWER), enabled — implements `UserDetails` directly (no separate principal wrapper class)
- Dashboard: not an entity — `DashboardService`/`DashboardController` aggregate task counts by status for the `/` frontend page

**RBAC**: `Task`/`Note` have no owner of their own — visibility/ownership always traces through
`.getClient().getOwner()`. MANAGER sees/mutates only clients (and their tasks/notes) they own;
ADMIN and VIEWER see everything; VIEWER is blocked from all POST/PUT/DELETE via
`@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")` on the controllers. `findById`/`update`/`delete`
in the three `*ServiceImpl` classes always `findOrThrow` first (real 404 if the id doesn't exist
at all) then `ownershipGuard.check(...)` (403 if it exists but isn't yours) — never conflate the
two, the order matters for getting the right status code.

`Task.client` and `Note.client` are `FetchType.LAZY` — always use DTOs in API responses to avoid lazy-loading issues. Repositories expose `findAllByOrderByIdAsc(...)` instead of `findAll()` for deterministic ordering — follow this convention for any new entity.

## Current state

Fully implemented MVP:

- **Backend**: entities (`Client`, `Task`, `Note`, `User`), DTOs, repositories (with `findAllByOrderByIdAsc` sorting), services, controllers, dashboard aggregation endpoint, global exception handler, CORS config, JWT auth (`POST /api/auth/register`/`login`, all other `/api/**` require a valid `Authorization: Bearer` token), RBAC (ADMIN/MANAGER/VIEWER, `Client.owner`-scoped visibility for MANAGER)
- **Frontend**: `/login` (stores JWT in `localStorage`), dashboard (`/`), `/clients` (CRUD), `/tasks` (CRUD + filters by status/clientId) — these three live under the `(app)` route group so they share the sidebar layout that `/login` deliberately doesn't get
- **Tests**: unit tests (Mockito) for `ClientServiceImpl`, `TaskServiceImpl`, `NoteServiceImpl`, `DashboardServiceImpl`, `AuthServiceImpl`, `JwtService`; integration tests (MockMvc + Testcontainers PostgreSQL) for `ClientController`, `TaskController`, `DashboardController`

Bootstrap login for local/docker environments: `admin` / `admin123` (seeded by Flyway `V4__seed_admin.sql`,
role ADMIN). Self-registration via `POST /api/auth/register` always creates role MANAGER — there is no
API to create ADMIN/VIEWER accounts, only the Flyway seed and direct DB inserts (see Known limitations).

## Claude Code project assets

This repo is also a learning ground for Claude Code's extensibility, and has working examples of each checked in:

- **Skill** — `.claude/skills/crud-generator/SKILL.md`: generates the full CRUD layer (entity, repository, request/response DTOs, service interface+impl, controller, not-found exception, unit test) for a new JPA entity, following the exact patterns of `Task`/`TaskServiceImpl`/etc. Use this instead of hand-writing a new entity's CRUD layer.
- **Subagent** — `.claude/agents/code-reviewer.md`: architectural reviewer that checks files against four rules derived from this document (no business logic in controllers, service interface+impl pairing, entities never returned from services/controllers, constructor injection only).
- **Slash command** — `.claude/commands/security-check.md`: backend security review across input validation, SQL injection, unauthenticated endpoints, CORS config, and stack-trace leakage.

## Known limitations (tracked, not fixed — see README.md for full detail)

- Default seeded ADMIN credentials (`admin`/`admin123`, `V4__seed_admin.sql`) are public in this repo —
  rotate immediately in any real deployment. There is no user-management API to create/promote
  ADMIN or VIEWER accounts; self-register always yields MANAGER.
- DTOs are hand-written classes with getters/setters rather than Java `record`s.
- Frontend pinned to Next.js 14.2.35 (latest patch in the 14.x line, not the true npm latest) —
  `npm audit` still reports one high-severity advisory range (9.x–16.3.0-canary.5, DoS/cache-poisoning/XSS
  in Next.js) whose fix requires the major jump to 16.x. Deliberately deferred: a 14→16 major upgrade
  needs live browser verification (App Router/fetch-caching behavior changes) not available when this
  was assessed. Revisit before any production use.

## Testing approach

- **Unit tests**: service layer with Mockito mocks
- **Integration tests**: REST API + real DB via Testcontainers (PostgreSQL)
- **Regression tests**: key CRUD + filter scenarios

Integration tests that use Testcontainers require Docker to be running.

## Claude Code workflow notes

- Use plan mode before large changes.
- Prefer interface + impl pattern for services.
- Do not return JPA entities from controllers — always map to DTOs.
