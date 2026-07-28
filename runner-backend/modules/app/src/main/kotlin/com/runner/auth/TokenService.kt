package com.runner.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID

data class StoredRefreshToken(
    val id: UUID,
    val userId: UUID,
    val tokenHash: String,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val createdAt: Instant
)

interface RefreshTokenStore {
    fun save(token: StoredRefreshToken)
    fun findByHash(tokenHash: String): StoredRefreshToken?
    fun revoke(id: UUID, revokedAt: Instant)
    fun revokeAllForUser(userId: UUID, revokedAt: Instant)
}

data class TokenPair(val accessToken: String, val refreshToken: String)

sealed class TokenServiceException(message: String) : Exception(message) {
    class InvalidRefreshToken : TokenServiceException("Refresh token is invalid, expired, or unknown")
    class RefreshTokenReuseDetected : TokenServiceException("Refresh token reuse detected; token family revoked")
}

class TokenService(
    private val secret: String,
    private val store: RefreshTokenStore,
    private val accessTokenTtl: Duration = Duration.ofMinutes(15),
    private val refreshTokenTtl: Duration = Duration.ofDays(30),
    private val clock: Clock = Clock.systemUTC(),
    private val random: SecureRandom = SecureRandom()
) {
    fun mintAccessToken(userId: UUID): String {
        val now = clock.instant()
        val claims = JWTClaimsSet.Builder()
            .subject(userId.toString())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plus(accessTokenTtl)))
            .build()
        val header = JWSHeader.Builder(JWSAlgorithm.HS256).build()
        val jwt = SignedJWT(header, claims)
        jwt.sign(MACSigner(secret))
        return jwt.serialize()
    }

    fun mintTokenPair(userId: UUID): TokenPair {
        val accessToken = mintAccessToken(userId)
        val refreshToken = generateRefreshToken()
        val now = clock.instant()
        store.save(
            StoredRefreshToken(
                id = UUID.randomUUID(),
                userId = userId,
                tokenHash = sha256Hex(refreshToken),
                expiresAt = now.plus(refreshTokenTtl),
                revokedAt = null,
                createdAt = now
            )
        )
        return TokenPair(accessToken, refreshToken)
    }

    fun refresh(rawRefreshToken: String): TokenPair {
        val stored = store.findByHash(sha256Hex(rawRefreshToken))
            ?: throw TokenServiceException.InvalidRefreshToken()
        val now = clock.instant()

        if (stored.revokedAt != null) {
            store.revokeAllForUser(stored.userId, now)
            throw TokenServiceException.RefreshTokenReuseDetected()
        }
        if (stored.expiresAt.isBefore(now)) {
            throw TokenServiceException.InvalidRefreshToken()
        }

        store.revoke(stored.id, now)
        return mintTokenPair(stored.userId)
    }

    private fun generateRefreshToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256Hex(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
