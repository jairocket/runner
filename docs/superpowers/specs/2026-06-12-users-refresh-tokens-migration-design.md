# Users & Refresh Tokens Migration Design

## Overview

Add Flyway-based database migrations and Exposed table definitions for `users` and `refresh_tokens` to the runner-backend `:infra` module, with an integration test that verifies the schema and enforces the `google_sub` unique constraint.

## Scope

- Migration mechanism: Flyway (SQL-first, versioned migrations)
- Exposed `Table` objects mapping 1:1 to the schema
- Integration test using TestContainers (real Postgres)

## Data Model

### users

| Column       | Type                     | Constraints             |
|--------------|--------------------------|-------------------------|
| id           | UUID                     | PK, not null            |
| google_sub   | text                     | unique, not null        |
| email        | text                     | not null                |
| display_name | text                     | not null                |
| created_at   | timestamp with time zone | not null, default now() |

### refresh_tokens

| Column     | Type                     | Constraints              |
|------------|--------------------------|--------------------------|
| id         | UUID                     | PK, not null             |
| user_id    | UUID                     | FK → users(id), not null |
| token_hash | text                     | not null                 |
| expires_at | timestamp with time zone | not null                 |
| revoked_at | timestamp with time zone | nullable                 |
| created_at | timestamp with time zone | not null, default now()  |

## Architecture

### Module: `:infra`

New source files:
- `src/main/kotlin/com/runner/db/tables/Users.kt` — Exposed `object Users : Table("users")`
- `src/main/kotlin/com/runner/db/tables/RefreshTokens.kt` — Exposed `object RefreshTokens : Table("refresh_tokens")`
- `src/main/kotlin/com/runner/db/Migrations.kt` — `fun runMigrations(dataSource: DataSource)` using Flyway
- `src/main/resources/db/migration/V1__create_users_and_refresh_tokens.sql` — SQL DDL

New test files:
- `src/test/kotlin/com/runner/db/SchemaTest.kt` — TestContainers integration test

### Module: `:app`

`configureDatabase()` calls `runMigrations(dataSource)` after the `HikariDataSource` is built, before `Database.connect()`.

## Dependencies

Added to `:infra/build.gradle.kts`:
- `implementation`: `exposed-core`, `exposed-jdbc`, `flyway-core`, `postgres-driver`
- `testImplementation(platform(...))`: `testcontainers-bom`
- `testImplementation`: `testcontainers-junit-jupiter`, `testcontainers-postgresql`, `hikari`, `dotenv`
- `testRuntimeOnly`: `junit-jupiter-engine`, `postgres-driver`

Added to `libs.versions.toml`:
- `flyway = "11.8.2"`
- `testcontainers = "1.21.1"`
- Library aliases: `flyway-core`, `testcontainers-bom`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`

## Flyway Wiring

```kotlin
// configureDatabase() in :app, after HikariDataSource is built
runMigrations(dataSource)
Database.connect(dataSource)
```

`runMigrations` uses the default Flyway classpath location `classpath:db/migration`, which resolves the SQL file from `:infra`'s resources (on the classpath transitively).

## Migration SQL (`V1__create_users_and_refresh_tokens.sql`)

```sql
CREATE TABLE users (
    id           UUID PRIMARY KEY,
    google_sub   TEXT NOT NULL UNIQUE,
    email        TEXT NOT NULL,
    display_name TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id),
    token_hash  TEXT NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

## Test: `SchemaTest`

1. Start a Postgres TestContainer
2. Run `runMigrations(dataSource)` against it
3. Connect Exposed via `Database.connect(dataSource)`
4. Insert a row into `users`
5. Insert a second row with the same `google_sub` → assert `ExposedSQLException` is thrown
6. Verify `refresh_tokens` table exists by inserting a valid row referencing the first user

## Acceptance Criteria

- Tables created via Flyway migration (versioned, tracked in `flyway_schema_history`)
- `Users` and `RefreshTokens` Exposed objects compile and map 1:1 to the SQL schema
- `SchemaTest` passes: schema exists and `google_sub` unique constraint is enforced
