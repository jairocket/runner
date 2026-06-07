# Backend Skeleton Design

**Date:** 2026-06-01  
**Status:** Approved  
**Scope:** Project scaffolding and plumbing only — no domain logic, no hexagonal layers yet

---

## Context

The Runner Android app currently uses `MockRunRepository` with no real persistence. This spec covers the creation of a standalone Kotlin backend project that will eventually serve as the persistence and API layer. Domain features and hexagonal architecture are out of scope here; this spec is about getting the skeleton running against a local Postgres instance.

---

## Directory Layout

The backend lives as a sibling to the Android project:

```
runner-backend/                  ← sibling to Runner/
  app/                           ← runnable Ktor server; entry point, DI wiring, routes
  domain/                        ← empty shell; will hold ports + entities in future
  infra/                         ← empty shell; will hold DB + HTTP adapters in future
  gradle/
    libs.versions.toml           ← shared version catalog
  build.gradle.kts               ← root build, shared plugin config
  settings.gradle.kts            ← declares app, domain, infra subprojects
  gradlew / gradlew.bat
  .env.example                   ← DATABASE_URL, PORT
  .gitignore                     ← excludes .env
```

---

## Module Dependency Graph

```
app  →  domain
app  →  infra
infra  →  domain
```

- `domain` has zero external dependencies (pure Kotlin)
- `infra` depends on `domain` only
- `app` wires everything together and owns the Ktor entry point

The dependency graph is final — adding code to `domain` and `infra` later requires no Gradle changes.

---

## Tech Stack

| Concern     | Library                           |
|-------------|-----------------------------------|
| HTTP server | Ktor 3.x                          |
| JSON        | `kotlinx.serialization`           |
| DI          | Koin                              |
| SQL         | Exposed (Kotlin DSL) + HikariCP   |
| Database    | Postgres (local)                  |
| Config      | `dotenv-kotlin` (reads `.env`)    |
| Testing     | JUnit 5 + `ktor-server-test-host` |

Exposed's DSL gives SQL control with Kotlin syntax and will swap cleanly behind a port during the hexagonal refactor.

---

## `app` Module Structure

```
app/src/main/kotlin/com/runner/
  Application.kt        ← main(); starts Ktor, loads Koin, connects DB
  plugins/
    Routing.kt          ← registers all route modules
    Serialization.kt    ← installs ContentNegotiation + kotlinx.serialization
    Database.kt         ← HikariCP pool setup, Exposed init
    DI.kt               ← Koin module definitions
  routes/
    HealthRoute.kt      ← GET /health → 200 OK
```

---

## `domain` and `infra` Modules

Both start as empty Kotlin packages — each with a `build.gradle.kts` declaring its dependencies and a placeholder source directory. No code yet.

---

## Configuration

Runtime config is read from a `.env` file (via `dotenv-kotlin`):

```
DATABASE_URL=jdbc:postgresql://localhost:5432/runner
PORT=8080
```

`.env` is gitignored. `.env.example` is committed as a template.

---

## Success Criteria

- `./gradlew run` (in `runner-backend/`) starts the Ktor server
- `GET /health` returns `200 OK`
- Server connects to local Postgres on startup (fails fast with a clear error if the DB is unreachable)
- All three Gradle subprojects compile cleanly

---

## Out of Scope

- Hexagonal architecture layers (planned: separate Gradle modules `domain` and `infra` populated in a future phase)
- Any domain routes (runs, users, etc.)
- Authentication
- Android app integration (connecting Retrofit/Ktor client on the Android side)
