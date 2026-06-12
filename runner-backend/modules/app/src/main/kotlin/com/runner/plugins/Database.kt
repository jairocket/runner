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
