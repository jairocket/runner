package com.runner.auth

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import java.text.ParseException
import java.time.Duration
import java.time.Instant

data class GoogleClaims(val sub: String, val email: String, val name: String)

sealed class TokenVerificationException(message: String) : Exception(message) {
    class InvalidSignature : TokenVerificationException("Invalid token signature")
    class TokenExpired : TokenVerificationException("Token has expired")
    class WrongAudience : TokenVerificationException("Token audience does not match")
    class MalformedToken(detail: String) : TokenVerificationException("Malformed token: $detail")
}

class GoogleTokenVerifier(
    private val audience: String,
    private val jwksProvider: () -> JWKSet,
    private val cacheDuration: Duration = Duration.ofHours(1)
) {
    private var cachedJwks: JWKSet? = null
    private var cachedAt: Instant? = null

    fun verify(idToken: String): GoogleClaims {
        val jwt = try {
            SignedJWT.parse(idToken)
        } catch (e: ParseException) {
            throw TokenVerificationException.MalformedToken(e.message ?: "parse error")
        }

        val jwks = getJwks()
        val keyId = jwt.header.keyID
        val candidates = if (keyId != null) {
            jwks.keys.filter { it.keyID == keyId }
        } else {
            jwks.keys
        }
        val rsaKey = candidates.mapNotNull { it as? RSAKey }.firstOrNull()
            ?: throw TokenVerificationException.InvalidSignature()

        val joseVerifier = try {
            RSASSAVerifier(rsaKey)
        } catch (e: JOSEException) {
            throw TokenVerificationException.InvalidSignature()
        }
        val valid = try {
            jwt.verify(joseVerifier)
        } catch (e: JOSEException) {
            throw TokenVerificationException.InvalidSignature()
        }
        if (!valid) throw TokenVerificationException.InvalidSignature()

        val claims = jwt.jwtClaimsSet
        val expiry = claims.expirationTime?.toInstant()
            ?: throw TokenVerificationException.MalformedToken("missing exp")
        if (Instant.now().isAfter(expiry)) throw TokenVerificationException.TokenExpired()
        if (!claims.audience.contains(audience)) throw TokenVerificationException.WrongAudience()

        return GoogleClaims(
            sub = claims.subject ?: throw TokenVerificationException.MalformedToken("missing sub"),
            email = claims.getStringClaim("email") ?: throw TokenVerificationException.MalformedToken("missing email"),
            name = claims.getStringClaim("name") ?: throw TokenVerificationException.MalformedToken("missing name")
        )
    }

    @Synchronized
    private fun getJwks(): JWKSet {
        val now = Instant.now()
        val cached = cachedJwks
        val fetchedAt = cachedAt
        if (cached != null && fetchedAt != null && now.isBefore(fetchedAt.plus(cacheDuration))) {
            return cached
        }
        val fresh = jwksProvider()
        cachedJwks = fresh
        cachedAt = now
        return fresh
    }
}
