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
