package com.steamforge.game.weekly.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.steamforge.game.weekly.server.WeeklyAuthenticatedPrincipal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklySessionTokenCodecTest {
    @Test
    fun `issued session round trips exact authenticated principal and claims`() {
        val codec = codec(clock = CLOCK)
        val principal = WeeklyAuthenticatedPrincipal("vkid:stable-subject-key")

        val token = codec.issue(principal)

        assertEquals(principal, codec.verify(token))
        val decoded = JWT.decode(token)
        assertEquals(WeeklySessionTokenCodec.ISSUER, decoded.issuer)
        assertEquals(listOf(WeeklySessionTokenCodec.AUDIENCE), decoded.audience)
        assertEquals(principal.subject, decoded.subject)
        assertEquals(NOW, decoded.issuedAtAsInstant)
        assertEquals(NOW, decoded.notBeforeAsInstant)
        assertEquals(NOW.plus(TTL), decoded.expiresAtAsInstant)
        assertEquals(
            WeeklySessionTokenCodec.SCOPE_WEEKLY_RANK,
            decoded.getClaim("scope").asString(),
        )
        assertEquals(
            WeeklySessionTokenCodec.SESSION_VERSION,
            decoded.getClaim("session_version").asString(),
        )
        assertTrue(token.length <= WeeklySessionTokenCodec.MAX_TOKEN_CHARS)
    }

    @Test
    fun `session signed by another key is rejected`() {
        val token = codec(secret = SECRET).issue(PRINCIPAL)

        assertNull(codec(secret = OTHER_SECRET).verify(token))
    }

    @Test
    fun `expired session is rejected after allowed clock skew`() {
        val token = codec(clock = CLOCK).issue(PRINCIPAL)
        val afterExpiry = Clock.fixed(
            NOW.plus(TTL).plusSeconds(31),
            ZoneOffset.UTC,
        )

        assertNull(codec(clock = afterExpiry).verify(token))
    }

    @Test
    fun `wrong issuer audience scope or version is rejected`() {
        val invalidTokens = listOf(
            signedToken(issuer = "other"),
            signedToken(audience = "other"),
            signedToken(scope = "other"),
            signedToken(version = "2"),
        )
        val codec = codec()

        invalidTokens.forEach { token -> assertNull(codec.verify(token)) }
    }

    @Test
    fun `signed session with excessive lifetime is rejected`() {
        val token = signedToken(expiresAt = NOW.plus(Duration.ofHours(2)))

        assertNull(codec().verify(token))
    }

    @Test
    fun `session requires not before to equal issued at`() {
        val token = signedToken(notBefore = NOW.plusSeconds(5))

        assertNull(codec().verify(token))
    }

    @Test
    fun `session requires all temporal claims`() {
        val codec = codec()

        listOf("iat", "nbf", "exp").forEach { omitted ->
            assertNull(codec.verify(signedTokenWithoutTemporal(omitted)))
        }
    }

    @Test
    fun `invalid subject is rejected even with valid signature`() {
        val blank = signedToken(subject = "   ")
        val oversized = signedToken(subject = "x".repeat(257))

        assertNull(codec().verify(blank))
        assertNull(codec().verify(oversized))
    }

    @Test
    fun `malformed blank and oversized tokens are rejected`() {
        val codec = codec()

        assertNull(codec.verify(""))
        assertNull(codec.verify("not-a-jwt"))
        assertNull(codec.verify("x".repeat(WeeklySessionTokenCodec.MAX_TOKEN_CHARS + 1)))
    }

    @Test
    fun `codec rejects weak secret and unsafe ttl`() {
        assertTrue(
            runCatching {
                WeeklySessionTokenCodec("short-secret", TTL, CLOCK)
            }.isFailure,
        )
        assertTrue(
            runCatching {
                WeeklySessionTokenCodec(SECRET, Duration.ZERO, CLOCK)
            }.isFailure,
        )
        assertTrue(
            runCatching {
                WeeklySessionTokenCodec(SECRET, Duration.ofHours(2), CLOCK)
            }.isFailure,
        )
        assertTrue(
            runCatching {
                WeeklySessionTokenCodec(SECRET, Duration.ofMillis(1500), CLOCK)
            }.isFailure,
        )
    }

    private fun codec(
        secret: String = SECRET,
        clock: Clock = CLOCK,
    ): WeeklySessionTokenCodec = WeeklySessionTokenCodec(secret, TTL, clock)

    private fun signedToken(
        issuer: String = WeeklySessionTokenCodec.ISSUER,
        audience: String = WeeklySessionTokenCodec.AUDIENCE,
        scope: String = WeeklySessionTokenCodec.SCOPE_WEEKLY_RANK,
        version: String = WeeklySessionTokenCodec.SESSION_VERSION,
        subject: String = PRINCIPAL.subject,
        issuedAt: Instant = NOW,
        notBefore: Instant = NOW,
        expiresAt: Instant = NOW.plus(TTL),
    ): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withSubject(subject)
        .withIssuedAt(issuedAt)
        .withNotBefore(notBefore)
        .withExpiresAt(expiresAt)
        .withClaim("scope", scope)
        .withClaim("session_version", version)
        .sign(Algorithm.HMAC256(SECRET))

    private fun signedTokenWithoutTemporal(omitted: String): String {
        val builder = JWT.create()
            .withIssuer(WeeklySessionTokenCodec.ISSUER)
            .withAudience(WeeklySessionTokenCodec.AUDIENCE)
            .withSubject(PRINCIPAL.subject)
            .withClaim("scope", WeeklySessionTokenCodec.SCOPE_WEEKLY_RANK)
            .withClaim("session_version", WeeklySessionTokenCodec.SESSION_VERSION)
        if (omitted != "iat") builder.withIssuedAt(NOW)
        if (omitted != "nbf") builder.withNotBefore(NOW)
        if (omitted != "exp") builder.withExpiresAt(NOW.plus(TTL))
        return builder.sign(Algorithm.HMAC256(SECRET))
    }

    private companion object {
        const val SECRET = "0123456789abcdef0123456789abcdef"
        const val OTHER_SECRET = "fedcba9876543210fedcba9876543210"
        val NOW: Instant = Instant.parse("2026-09-06T12:00:00Z")
        val TTL: Duration = Duration.ofMinutes(15)
        val CLOCK: Clock = Clock.fixed(NOW, ZoneOffset.UTC)
        val PRINCIPAL = WeeklyAuthenticatedPrincipal("vkid:stable-subject-key")
    }
}
