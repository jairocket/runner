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
