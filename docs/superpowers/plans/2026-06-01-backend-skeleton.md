# Backend Skeleton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scaffold `runner-backend/` as a Kotlin sub-directory of the `runner` Android project with Ktor, Postgres, and a `/health` endpoint — no domain logic yet. The backend is a self-contained Gradle project living at `runner/runner-backend/` and tracked by runner's git repo.

**Architecture:** Three Gradle submodules (`app`, `domain`, `infra`) wired together; `domain` and `infra` are empty shells. The `app` module owns the Ktor entry point, DI wiring via Koin, Postgres connection via Exposed + HikariCP, and a single health-check route.

**Tech Stack:** Kotlin 2.x, Ktor 3.x, Koin 4.x, Exposed 0.61.x, HikariCP 6.x, PostgreSQL JDBC driver, dotenv-kotlin, JUnit 5, ktor-server-test-host

---

## Checkpoint — 2026-06-07

**All tasks COMPLETE.** `runner-backend/` now lives at `runner/runner-backend/`, committed to runner's git repo (`46b813a chore: move runner-backend into runner as a sub-directory`), and the end-to-end smoke test passed against a Podman Postgres container.

**Follow-up restructure:** `app`, `domain`, and `infra` were moved under `runner-backend/modules/` to keep them visually distinct from Gradle's generated/tooling directories (`build/`, `.gradle/`, `gradle/`) at the project root. `settings.gradle.kts` maps each subproject's `projectDir` to `modules/<name>` accordingly. Verified `./gradlew :app:test` still passes after the move.

Commits previously in standalone `runner-backend` (newest first):
- `feat: add Application entry point`
- `feat: add Koin DI plugin`
- `feat: add Database plugin with HikariCP + Exposed`
- `feat: add GET /health endpoint with test`
- `chore: add app module build file and resources`
- `chore: add infra empty shell module`
- `chore: add domain empty shell module`
- `chore: fix Gradle repo mode, junit5 engine, gitignore coverage`
- `chore: init runner-backend root Gradle project`

---

## File Map

| File | Responsibility |
|---|---|
| `runner-backend/settings.gradle.kts` | Declares root project name and three submodules |
| `runner-backend/build.gradle.kts` | Root build — shared plugin versions, no code |
| `runner-backend/gradle/libs.versions.toml` | Central version catalog for all dependencies |
| `runner-backend/gradlew` + `gradlew.bat` | Gradle wrapper scripts |
| `runner-backend/gradle/wrapper/gradle-wrapper.properties` | Gradle wrapper version |
| `runner-backend/.env.example` | Template with DATABASE_URL and PORT |
| `runner-backend/.gitignore` | Excludes `.env`, `build/`, `.gradle/` |
| `runner-backend/modules/domain/build.gradle.kts` | Empty shell — pure Kotlin, no deps |
| `runner-backend/modules/domain/src/main/kotlin/com/runner/domain/.gitkeep` | Keeps source dir in git |
| `runner-backend/modules/infra/build.gradle.kts` | Empty shell — depends on `domain` |
| `runner-backend/modules/infra/src/main/kotlin/com/runner/infra/.gitkeep` | Keeps source dir in git |
| `runner-backend/modules/app/build.gradle.kts` | App module — all runtime dependencies |
| `runner-backend/modules/app/src/main/kotlin/com/runner/Application.kt` | `main()` entry point |
| `runner-backend/modules/app/src/main/kotlin/com/runner/plugins/Serialization.kt` | Installs ContentNegotiation + JSON |
| `runner-backend/modules/app/src/main/kotlin/com/runner/plugins/Database.kt` | HikariCP + Exposed init, fails fast if DB unreachable |
| `runner-backend/modules/app/src/main/kotlin/com/runner/plugins/DI.kt` | Koin module definitions |
| `runner-backend/modules/app/src/main/kotlin/com/runner/plugins/Routing.kt` | Registers all route blocks |
| `runner-backend/modules/app/src/main/kotlin/com/runner/routes/HealthRoute.kt` | `GET /health` → 200 OK |
| `runner-backend/modules/app/src/test/kotlin/com/runner/routes/HealthRouteTest.kt` | Ktor test-host test for `/health` |

All paths above are relative to `runner/` (i.e. `runner/runner-backend/…`).

---

## Task 0: Migrate standalone repo into runner/runner-backend/

The original scaffolding was built in a sibling directory `/home/jailson/Documents/projects/runner-backend/` with its own git history. This task moves it inside `runner/` and commits it to runner's git repo.

- [x] **Step 1: Move the directory**

```bash
mv /home/jailson/Documents/projects/runner-backend \
   /home/jailson/Documents/projects/runner/runner-backend
```

- [x] **Step 2: Remove the nested `.git` directory**

The standalone repo's `.git` would shadow runner's git. Remove it:

```bash
rm -rf /home/jailson/Documents/projects/runner/runner-backend/.git
```

- [x] **Step 3: Verify runner's git sees the new files**

```bash
git -C /home/jailson/Documents/projects/runner status
```
Expected: `runner-backend/` appears as untracked (not as a nested repo).

- [x] **Step 4: Stage and commit to runner**

```bash
git -C /home/jailson/Documents/projects/runner add runner-backend/
git -C /home/jailson/Documents/projects/runner commit -m "chore: move runner-backend into runner as a sub-directory"
```

- [x] **Step 5: Verify the backend builds from its new location**

```bash
cd /home/jailson/Documents/projects/runner/runner-backend && ./gradlew :app:test
```
Expected: `BUILD SUCCESSFUL`, all tests pass.

---

## Task 1: Root Gradle project

> **Already complete.** Files were created and verified in the standalone repo. No action needed — they are now at `runner/runner-backend/` after Task 0.

---

## Task 2: `domain` empty shell

> **Already complete.** See Task 0.

---

## Task 3: `infra` empty shell

> **Already complete.** See Task 0.

---

## Task 4: `app` module build file

> **Already complete.** See Task 0.

---

## Task 5: Health route (TDD)

> **Already complete.** See Task 0.

---

## Task 6: Database plugin

> **Already complete.** See Task 0.

---

## Task 7: DI plugin

> **Already complete.** See Task 0.

---

## Task 8: Application entry point

> **Already complete.** See Task 0.

---

## Task 9: Smoke test end-to-end

> Requires Podman. The database runs in a container — no local Postgres installation needed.

- [x] **Step 1: Start a Postgres container with Podman**

```bash
podman run -d \
  --name runner-db \
  -e POSTGRES_DB=runner \
  -e POSTGRES_USER=runner \
  -e POSTGRES_PASSWORD=runner \
  -p 5432:5432 \
  docker.io/library/postgres:16
```
> Note: this Podman setup has no unqualified-search registries configured, so the
> short name `postgres:16` fails with `short-name did not resolve to an alias`.
> Use the fully-qualified `docker.io/library/postgres:16` instead.

Expected: container ID printed. Verify it's up:
```bash
podman ps --filter name=runner-db
```

- [x] **Step 2: Create `.env` from the example**

```bash
cp /home/jailson/Documents/projects/runner/runner-backend/.env.example \
   /home/jailson/Documents/projects/runner/runner-backend/.env
```
Edit `.env` so credentials match the container:
```
DATABASE_URL=jdbc:postgresql://localhost:5432/runner?user=runner&password=runner
PORT=8080
```

> Note: `dotenv-kotlin` resolves `.env` relative to the JVM process's working
> directory. The Ktor Gradle plugin's `:app:run` task launches the process with
> `modules/app/` as its working directory (not the `runner-backend/` root), so `.env`
> must also be copied to `runner-backend/modules/app/.env` or the server fails fast with
> `jdbcUrl is required with driverClassName` (DATABASE_URL resolves to null):
> ```bash
> cp runner-backend/.env runner-backend/modules/app/.env
> ```
> Both paths are covered by `runner-backend/.gitignore`.

- [x] **Step 3: Start the server (Java 21 required)**

```bash
source ~/.sdkman/bin/sdkman-init.sh && sdk use java 21.0.6-tem
cd /home/jailson/Documents/projects/runner/runner-backend && ./gradlew :app:run
```
Expected output includes:
```
Database connected: jdbc:postgresql://localhost:5432/runner?user=runner&password=runner
Application started in 0.716 seconds.
Responding at http://0.0.0.0:8080
```

- [x] **Step 4: Hit the health endpoint**

```bash
curl -i http://localhost:8080/health
```
Expected:
```
HTTP/1.1 200 OK
```

- [x] **Step 5: Stop the server and verify `.env` is gitignored**

```bash
git -C /home/jailson/Documents/projects/runner status
```
Expected: `runner-backend/.env` does not appear in the output (covered by `runner-backend/.gitignore`).

- [x] **Step 6: Stop the container when done**

```bash
podman stop runner-db
```
To restart later: `podman start runner-db`

---

## Smoke Test Result — 2026-06-07

✅ All steps passed:
- Postgres 16 container started via Podman (`docker.io/library/postgres:16`)
- Server connected: `Database connected: jdbc:postgresql://localhost:5432/runner?user=runner&password=runner`
- `GET /health` → `200 OK`
- `.env` confirmed gitignored (does not appear in `git status`)
- Server stopped, container stopped
