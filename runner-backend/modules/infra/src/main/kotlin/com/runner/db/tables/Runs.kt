package com.runner.db.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone

object Runs : Table("runs") {
    val id = uuid("id")
    val userId = reference("user_id", Users.id)
    val startedAt = timestampWithTimeZone("started_at")
    val durationSeconds = integer("duration_seconds")
    val distanceKm = decimal("distance_km", 6, 3)
    val paceSecPerKm = integer("pace_sec_per_km")
    val createdAt = timestampWithTimeZone("created_at")

    override val primaryKey = PrimaryKey(id)
}
