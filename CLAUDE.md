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
cd backend && ./mvnw test -Dtest=ClientServiceTest

# Backend — single test method
cd backend && ./mvnw test -Dtest=ClientServiceTest#createClient_shouldReturnDto

# Frontend — local dev
cd frontend && npm run dev

# Frontend — lint
cd frontend && npm run lint
```

## Spring profiles

| Profile            | Datasource | DDL          | When used                          |
|--------------------|------------|--------------|------------------------------------|
| `dev`              | H2 in-mem  | `update`     | Default for local run              |
| `docker`           | PostgreSQL | `update`     | Docker Compose                     |
| `test`             | H2 in-mem  | `create-drop`| `./mvnw test` (unit tests)         |
| `integration-test` | PostgreSQL (Testcontainers) | `create-drop` | IT controller tests |

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
- **Config** (`config/`): CORS, OpenAPI/Swagger, and other Spring configuration.

## Domain model

- `Client`: id, name, email, phone, createdAt (Instant)
- `Task`: id, title, description, status, priority, deadline (LocalDate), client (ManyToOne → Client)

`Task.client` is `FetchType.LAZY` — always use DTOs in API responses to avoid lazy-loading issues.

## Current state

Fully implemented MVP:

- **Backend**: entities, DTOs, repositories (with `findAllByOrderByIdAsc` sorting), services, controllers, global exception handler, CORS config
- **Frontend**: dashboard (`/`), `/clients` (CRUD), `/tasks` (CRUD + filters by status/clientId)
- **Tests**: unit tests (Mockito) for ClientServiceImpl + TaskServiceImpl; integration tests (Testcontainers PostgreSQL) for ClientController + TaskController

## Testing approach

- **Unit tests**: service layer with Mockito mocks
- **Integration tests**: REST API + real DB via Testcontainers (PostgreSQL)
- **Regression tests**: key CRUD + filter scenarios

Integration tests that use Testcontainers require Docker to be running.

## Claude Code workflow notes

- Use plan mode before large changes.
- Prefer interface + impl pattern for services.
- Do not return JPA entities from controllers — always map to DTOs.
