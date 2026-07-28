package com.runner.auth

import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.SignedJWT
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class TokenServiceTest {

    private val secret = "test-signing-secret-at-least-32-bytes-long"
    private val store = FakeRefreshTokenStore()
    private val tokenService = TokenService(secret = secret, store = store)

    @Test
    fun `mints access token with sub, iat, and exp claims`() {
        val userId = UUID.randomUUID()

        val accessToken = tokenService.mintAccessToken(userId)

        val jwt = SignedJWT.parse(accessToken)
        assertEquals(true, jwt.verify(MACVerifier(secret)))
        val claims = jwt.jwtClaimsSet
        assertEquals(userId.toString(), claims.subject)
        assertEquals(true, claims.issueTime != null)
        assertEquals(true, claims.expirationTime.toInstant().isAfter(Instant.now()))
    }

    @Test
    fun `mints a token pair and stores a hash of the refresh token, not the raw value`() {
        val userId = UUID.randomUUID()

        val pair = tokenService.mintTokenPair(userId)

        assertNotEquals(pair.accessToken, pair.refreshToken)
        val stored = store.tokens.values.single()
        assertEquals(userId, stored.userId)
        assertNotEquals(pair.refreshToken, stored.tokenHash)
        assertEquals(sha256Hex(pair.refreshToken), stored.tokenHash)
        assertNotNull(stored.expiresAt)
    }

    @Test
    fun `refreshing a valid token revokes it and returns a new pair`() {
        val userId = UUID.randomUUID()
        val original = tokenService.mintTokenPair(userId)

        val rotated = tokenService.refresh(original.refreshToken)

        assertNotEquals(original.refreshToken, rotated.refreshToken)
        val originalStored = store.tokens.values.single { it.tokenHash == sha256Hex(original.refreshToken) }
        assertNotNull(originalStored.revokedAt)
        val rotatedStored = store.tokens.values.single { it.tokenHash == sha256Hex(rotated.refreshToken) }
        assertEquals(null, rotatedStored.revokedAt)
    }

    @Test
    fun `replaying an already-revoked refresh token revokes the whole family and throws`() {
        val userId = UUID.randomUUID()
        val original = tokenService.mintTokenPair(userId)
        val rotated = tokenService.refresh(original.refreshToken)

        assertFailsWith<TokenServiceException.RefreshTokenReuseDetected> {
            tokenService.refresh(original.refreshToken)
        }

        val rotatedStored = store.tokens.values.single { it.tokenHash == sha256Hex(rotated.refreshToken) }
        assertNotNull(rotatedStored.revokedAt)
    }

    @Test
    fun `refreshing an unknown token throws InvalidRefreshToken`() {
        assertFailsWith<TokenServiceException.InvalidRefreshToken> {
            tokenService.refresh("not-a-real-token")
        }
    }

    @Test
    fun `refreshing an expired token throws InvalidRefreshToken`() {
        val clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"))
        val expiringService = TokenService(
            secret = secret,
            store = store,
            refreshTokenTtl = Duration.ofDays(30),
            clock = clock
        )
        val pair = expiringService.mintTokenPair(UUID.randomUUID())
        clock.instant = clock.instant.plus(Duration.ofDays(31))

        assertFailsWith<TokenServiceException.InvalidRefreshToken> {
            expiringService.refresh(pair.refreshToken)
        }
    }
}

private class MutableClock(var instant: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = instant
}

private fun sha256Hex(value: String): String =
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

private class FakeRefreshTokenStore : RefreshTokenStore {
    val tokens = mutableMapOf<UUID, StoredRefreshToken>()

    override fun save(token: StoredRefreshToken) {
        tokens[token.id] = token
    }

    override fun findByHash(tokenHash: String): StoredRefreshToken? =
        tokens.values.find { it.tokenHash == tokenHash }

    override fun revoke(id: UUID, revokedAt: Instant) {
        val existing = tokens[id] ?: return
        tokens[id] = existing.copy(revokedAt = revokedAt)
    }

    override fun revokeAllForUser(userId: UUID, revokedAt: Instant) {
        tokens.replaceAll { _, token ->
            if (token.userId == userId && token.revokedAt == null) token.copy(revokedAt = revokedAt) else token
        }
    }
}
