package com.runner.db.tables

import org.jetbrains.exposed.sql.Table

object RunPositions : Table("run_positions") {
    val id = long("id").autoIncrement()
    val runId = reference("run_id", Runs.id)
    val seq = integer("seq")
    val lat = double("lat")
    val lon = double("lon")

    override val primaryKey = PrimaryKey(id)
}
