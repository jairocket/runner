package com.runner.db

import javax.sql.DataSource
import org.flywaydb.core.Flyway

fun runMigrations(dataSource: DataSource) {
    Flyway.configure()
        .dataSource(dataSource)
        .load()
        .migrate()
}
