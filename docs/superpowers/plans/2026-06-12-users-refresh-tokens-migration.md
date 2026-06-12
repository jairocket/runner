# Users & Refresh Tokens Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Flyway migrations and Exposed table definitions for `users` and `refresh_tokens`, with a TestContainers integration test that enforces the schema.

**Architecture:** SQL migration lives in `:infra` resources; Exposed `Table` objects live in `:infra` source; a `runMigrations(DataSource)` helper is called from `:app`'s `configureDatabase()` before Exposed connects. Tests use TestContainers (real Postgres) to verify schema and the `google_sub` unique constraint.

**Tech Stack:** Kotlin, Exposed 0.61.0, Flyway 11.8.2, TestContainers 1.21.1, JUnit 5, HikariCP, PostgreSQL

---

## File Map

| Action | Path | Responsibility |
|--------|------|---------------|
| Modify | `runner-backend/gradle/libs.versions.toml` | Add `flyway`, `testcontainers` versions + library aliases |
| Modify | `runner-backend/modules/infra/build.gradle.kts` | Add compile + test dependencies |
| Create | `runner-backend/modules/infra/src/main/resources/db/migration/V1__create_users_and_refresh_tokens.sql` | DDL for both tables |
| Create | `runner-backend/modules/infra/src/main/kotlin/com/runner/db/tables/Users.kt` | Exposed Table object for `users` |
| Create | `runner-backend/modules/infra/src/main/kotlin/com/runner/db/tables/RefreshTokens.kt` | Exposed Table object for `refresh_tokens` |
| Create | `runner-backend/modules/infra/src/main/kotlin/com/runner/db/Migrations.kt` | `runMigrations(DataSource)` helper |
| Create | `runner-backend/modules/infra/src/test/kotlin/com/runner/db/SchemaTest.kt` | Integration test (TestContainers) |
| Modify | `runner-backend/modules/app/src/main/kotlin/com/runner/plugins/Database.kt` | Call `runMigrations` before `Database.connect` |

---

## Task 1: Add Flyway, exposed-java-time, and TestContainers to Gradle

**Files:**
- Modify: `runner-backend/gradle/libs.versions.toml`
- Modify: `runner-backend/modules/infra/build.gradle.kts`

- [ ] **Step 1: Add versions and library aliases to `libs.versions.toml`**

Open `runner-backend/gradle/libs.versions.toml`. Add the new entries shown below (preserving all existing lines):

```toml
[versions]
# ... existing versions ...
flyway        = "11.8.2"
testcontainers = "1.21.1"

[libraries]
# ... existing libraries ...
exposed-java-time             = { module = "org.jetbrains.exposed:exposed-java-time",                  version.ref = "exposed" }
flyway-core                   = { module = "org.flywaydb:flyway-core",                                  version.ref = "flyway" }
flyway-database-postgresql    = { module = "org.flywaydb:flyway-database-postgresql",                   version.ref = "flyway" }
testcontainers-bom            = { module = "org.testcontainers:testcontainers-bom",                     version.ref = "testcontainers" }
testcontainers-junit-jupiter  = { module = "org.testcontainers:junit-jupiter",                          version.ref = "testcontainers" }
testcontainers-postgresql     = { module = "org.testcontainers:postgresql",                             version.ref = "testcontainers" }
```

Note: `flyway-database-postgresql` is required for Flyway 10+ (Postgres support was extracted from core).

- [ ] **Step 2: Replace `runner-backend/modules/infra/build.gradle.kts` with the following**

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.postgres.driver)

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.hikari)
    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 3: Verify Gradle syncs without error**

```bash
cd runner-backend && ./gradlew :infra:dependencies --configuration compileClasspath
```

Expected: output ends with `BUILD SUCCESSFUL` and includes `org.flywaydb:flyway-core` and `org.jetbrains.exposed:exposed-java-time` in the dependency tree.

---

## Task 2: Write the failing SchemaTest

**Files:**
- Create: `runner-backend/modules/infra/src/test/kotlin/com/runner/db/SchemaTest.kt`

- [ ] **Step 1: Create the test directory and file**

```bash
mkdir -p runner-backend/modules/infra/src/test/kotlin/com/runner/db
```

Create `runner-backend/modules/infra/src/test/kotlin/com/runner/db/SchemaTest.kt`:

```kotlin
package com.runner.db

import com.runner.db.tables.RefreshTokens
import com.runner.db.tables.Users
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertFailsWith

@Testcontainers
class SchemaTest {

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")
    }

    @Test
    fun `schema exists and google_sub unique constraint is enforced`() {
        val dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 2
        })

        runMigrations(dataSource)
        val db = Database.connect(dataSource)

        // Insert the first user
        val userId = UUID.randomUUID()
        transaction(db) {
            Users.insert {
                it[id] = userId
                it[googleSub] = "google-sub-abc"
                it[email] = "alice@example.com"
                it[displayName] = "Alice"
                it[createdAt] = OffsetDateTime.now()
            }
        }

        // Inserting a second user with the same google_sub must throw
        assertFailsWith<ExposedSQLException> {
            transaction(db) {
                Users.insert {
                    it[id] = UUID.randomUUID()
                    it[googleSub] = "google-sub-abc"
                    it[email] = "bob@example.com"
                    it[displayName] = "Bob"
                    it[createdAt] = OffsetDateTime.now()
                }
            }
        }

        // Verify refresh_tokens table exists by inserting a valid row
        transaction(db) {
            RefreshTokens.insert {
                it[id] = UUID.randomUUID()
                it[RefreshTokens.userId] = userId
                it[tokenHash] = "sha256-hash-placeholder"
                it[expiresAt] = OffsetDateTime.now().plusDays(30)
                it[createdAt] = OffsetDateTime.now()
            }
        }

        dataSource.close()
    }
}
```

- [ ] **Step 2: Run the test and confirm it fails to compile**

```bash
cd runner-backend && ./gradlew :infra:test 2>&1 | tail -30
```

Expected: compilation error — `Unresolved reference: runMigrations` (and `Users`, `RefreshTokens`). This is the expected red state.

---

## Task 3: Create the SQL migration file

**Files:**
- Create: `runner-backend/modules/infra/src/main/resources/db/migration/V1__create_users_and_refresh_tokens.sql`

- [ ] **Step 1: Create the directory and file**

```bash
mkdir -p runner-backend/modules/infra/src/main/resources/db/migration
```

Create `runner-backend/modules/infra/src/main/resources/db/migration/V1__create_users_and_refresh_tokens.sql`:

```sql
CREATE TABLE users (
    id           UUID        PRIMARY KEY,
    google_sub   TEXT        NOT NULL UNIQUE,
    email        TEXT        NOT NULL,
    display_name TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users(id),
    token_hash  TEXT        NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

The filename prefix `V1__` (capital V, two underscores) is Flyway's required versioned migration naming convention.

---

## Task 4: Create the Exposed Table objects

**Files:**
- Create: `runner-backend/modules/infra/src/main/kotlin/com/runner/db/tables/Users.kt`
- Create: `runner-backend/modules/infra/src/main/kotlin/com/runner/db/tables/RefreshTokens.kt`

- [ ] **Step 1: Create the tables directory and Users.kt**

```bash
mkdir -p runner-backend/modules/infra/src/main/kotlin/com/runner/db/tables
```

Create `runner-backend/modules/infra/src/main/kotlin/com/runner/db/tables/Users.kt`:

```kotlin
package com.runner.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object Users : Table("users") {
    val id = uuid("id")
    val googleSub = text("google_sub").uniqueIndex()
    val email = text("email")
    val displayName = text("display_name")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}
```

- [ ] **Step 2: Create RefreshTokens.kt**

Create `runner-backend/modules/infra/src/main/kotlin/com/runner/db/tables/RefreshTokens.kt`:

```kotlin
package com.runner.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object RefreshTokens : Table("refresh_tokens") {
    val id = uuid("id")
    val userId = reference("user_id", Users.id)
    val tokenHash = text("token_hash")
    val expiresAt = timestampWithTimeZone("expires_at")
    val revokedAt = timestampWithTimeZone("revoked_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}
```

---

## Task 5: Create Migrations.kt and verify the test passes

**Files:**
- Create: `runner-backend/modules/infra/src/main/kotlin/com/runner/db/Migrations.kt`

- [ ] **Step 1: Create Migrations.kt**

Create `runner-backend/modules/infra/src/main/kotlin/com/runner/db/Migrations.kt`:

```kotlin
package com.runner.db

import javax.sql.DataSource
import org.flywaydb.core.Flyway

fun runMigrations(dataSource: DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .load()
        .migrate()
}
```

Flyway automatically scans `classpath:db/migration` (the default location), which resolves to the SQL file in `:infra`'s resources.

- [ ] **Step 2: Run the SchemaTest and verify it passes**

Prerequisite: Docker must be running (TestContainers launches a real Postgres container).

```bash
cd runner-backend && ./gradlew :infra:test
```

Expected output:
```
> Task :infra:test

SchemaTest > schema exists and google_sub unique constraint is enforced PASSED

BUILD SUCCESSFUL
```

If the test fails with a Flyway error about missing `flyway-database-postgresql`, confirm that dependency is present in `infra/build.gradle.kts` and re-run.

---

## Task 6: Wire runMigrations into configureDatabase()

**Files:**
- Modify: `runner-backend/modules/app/src/main/kotlin/com/runner/plugins/Database.kt`

- [ ] **Step 1: Replace the contents of Database.kt**

```kotlin
package com.runner.plugins

import com.runner.db.runMigrations
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.cdimascio.dotenv.dotenv
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Database")

fun Application.configureDatabase() {
    val env = dotenv { ignoreIfMissing = true }
    val dbUrl = env["DATABASE_URL"]

    val config = HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = dbUrl
        maximumPoolSize = 10
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    val dataSource = HikariDataSource(config)
    runMigrations(dataSource)
    Database.connect(dataSource)
    logger.info("Database connected: $dbUrl")
}
```

- [ ] **Step 2: Run the full backend test suite**

```bash
cd runner-backend && ./gradlew test
```

Expected:
```
> Task :app:test

HealthRouteTest > GET health returns 200 PASSED

> Task :infra:test

SchemaTest > schema exists and google_sub unique constraint is enforced PASSED

BUILD SUCCESSFUL
```

---

## Task 7: Commit

- [ ] **Step 1: Stage and commit all changes**

```bash
cd runner-backend && git add \
  gradle/libs.versions.toml \
  modules/infra/build.gradle.kts \
  modules/infra/src/main/resources/db/migration/V1__create_users_and_refresh_tokens.sql \
  modules/infra/src/main/kotlin/com/runner/db/tables/Users.kt \
  modules/infra/src/main/kotlin/com/runner/db/tables/RefreshTokens.kt \
  modules/infra/src/main/kotlin/com/runner/db/Migrations.kt \
  modules/infra/src/test/kotlin/com/runner/db/SchemaTest.kt \
  modules/app/src/main/kotlin/com/runner/plugins/Database.kt
```

```bash
git commit -m "$(cat <<'EOF'
feat: add Flyway migration and Exposed tables for users and refresh_tokens

- V1 migration creates users (UUID PK, google_sub unique) and refresh_tokens (FK → users)
- Exposed Table objects in :infra map 1:1 to the schema
- runMigrations() wired into configureDatabase() before Exposed connects
- SchemaTest uses TestContainers to verify schema and google_sub unique constraint

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```
