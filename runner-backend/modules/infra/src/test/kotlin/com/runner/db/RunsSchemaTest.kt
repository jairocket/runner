package com.runner.db

import com.runner.db.tables.RunPositions
import com.runner.db.tables.Runs
import com.runner.db.tables.Users
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertFailsWith

@Testcontainers
class RunsSchemaTest {

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16")

        lateinit var db: Database
        lateinit var dataSource: HikariDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = HikariDataSource(HikariConfig().apply {
                jdbcUrl = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 2
            })
            runMigrations(dataSource)
            db = Database.connect(dataSource)
        }
    }

    @Test
    fun `runs rejects insert when user_id does not exist`() {
        assertFailsWith<ExposedSQLException> {
            transaction(db) {
                Runs.insert {
                    it[id] = UUID.randomUUID()
                    it[userId] = UUID.randomUUID() // non-existent user
                    it[startedAt] = OffsetDateTime.now()
                    it[durationSeconds] = 1800
                    it[distanceKm] = BigDecimal("5.000")
                    it[paceSecPerKm] = 360
                    it[createdAt] = OffsetDateTime.now()
                }
            }
        }
    }

    @Test
    fun `run_positions rejects insert when run_id does not exist`() {
        assertFailsWith<ExposedSQLException> {
            transaction(db) {
                RunPositions.insert {
                    it[runId] = UUID.randomUUID() // non-existent run
                    it[seq] = 1
                    it[lat] = 37.7749
                    it[lon] = -122.4194
                }
            }
        }
    }

    @Test
    fun `run can be inserted with valid user_id and run_position can reference it`() {
        val userId = UUID.randomUUID()
        transaction(db) {
            Users.insert {
                it[id] = userId
                it[googleSub] = "sub-runs-test-${userId}"
                it[email] = "runner@example.com"
                it[displayName] = "Runner"
                it[createdAt] = OffsetDateTime.now()
            }
        }

        val runId = UUID.randomUUID()
        transaction(db) {
            Runs.insert {
                it[id] = runId
                it[Runs.userId] = userId
                it[startedAt] = OffsetDateTime.now()
                it[durationSeconds] = 3600
                it[distanceKm] = BigDecimal("10.500")
                it[paceSecPerKm] = 343
                it[createdAt] = OffsetDateTime.now()
            }
        }

        transaction(db) {
            RunPositions.insert {
                it[RunPositions.runId] = runId
                it[seq] = 1
                it[lat] = 37.7749
                it[lon] = -122.4194
            }
        }
    }
}
