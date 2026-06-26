package com.runner.auth

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.time.Instant
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class GoogleTokenVerifierTest {

    private val rsaKey = RSAKeyGenerator(2048).keyID("test-key-1").generate()
    private val fixtureJwks = JWKSet(rsaKey.toPublicJWK())
    private val audience = "test-audience-123"

    private val verifier = GoogleTokenVerifier(
        audience = audience,
        jwksProvider = { fixtureJwks }
    )

    private fun buildToken(
        sub: String = "user-123",
        email: String = "test@example.com",
        name: String = "Test User",
        aud: String = audience,
        expiresAt: Instant = Instant.now().plusSeconds(3600),
        signingKey: com.nimbusds.jose.jwk.RSAKey = rsaKey
    ): String {
        val claims = JWTClaimsSet.Builder()
            .subject(sub)
            .claim("email", email)
            .claim("name", name)
            .audience(aud)
            .expirationTime(Date.from(expiresAt))
            .issueTime(Date.from(Instant.now()))
            .build()
        val header = JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.keyID).build()
        val jwt = SignedJWT(header, claims)
        jwt.sign(RSASSASigner(signingKey))
        return jwt.serialize()
    }

    @Test
    fun `valid token returns sub, email, and name`() {
        val claims = verifier.verify(buildToken())
        assertEquals("user-123", claims.sub)
        assertEquals("test@example.com", claims.email)
        assertEquals("Test User", claims.name)
    }

    @Test
    fun `token signed with unknown key is rejected with InvalidSignature`() {
        val wrongKey = RSAKeyGenerator(2048).keyID("unknown-key").generate()
        assertFailsWith<TokenVerificationException.InvalidSignature> {
            verifier.verify(buildToken(signingKey = wrongKey))
        }
    }

    @Test
    fun `expired token is rejected with TokenExpired`() {
        assertFailsWith<TokenVerificationException.TokenExpired> {
            verifier.verify(buildToken(expiresAt = Instant.now().minusSeconds(60)))
        }
    }

    @Test
    fun `token with wrong audience is rejected with WrongAudience`() {
        assertFailsWith<TokenVerificationException.WrongAudience> {
            verifier.verify(buildToken(aud = "wrong-audience"))
        }
    }
}
