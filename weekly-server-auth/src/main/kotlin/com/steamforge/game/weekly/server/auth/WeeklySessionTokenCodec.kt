package com.steamforge.game.weekly.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.RegisteredClaims
import com.auth0.jwt.algorithms.Algorithm
import com.steamforge.game.weekly.server.WeeklyAuthenticatedPrincipal
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * Issues and verifies short-lived Steamforge bearer sessions after provider identity is validated.
 *
 * The signing secret is a backend-only runtime input (for production, from Lockbox or equivalent)
 * and must never be committed or shipped in the Android APK. Tokens contain only the pseudonymous
 * server principal needed by Weekly ranking; VK access/refresh tokens and profile PII are never copied
 * into this session.
 */
class WeeklySessionTokenCodec(
    secret: String,
    private val sessionTtl: Duration,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val algorithm: Algorithm
    private val verifier: com.auth0.jwt.interfaces.JWTVerifier

    init {
        require(secret.toByteArray(StandardCharsets.UTF_8).size >= MIN_SECRET_BYTES) {
            "Weekly session signing secret must be at least $MIN_SECRET_BYTES UTF-8 bytes"
        }
        require(!sessionTtl.isZero && !sessionTtl.isNegative) {
            "Weekly session TTL must be positive"
        }
        require(sessionTtl <= MAX_SESSION_TTL) {
            "Weekly session TTL must not exceed $MAX_SESSION_TTL"
        }
        require(sessionTtl.nano == 0) {
            "Weekly session TTL must use whole-second precision"
        }

        algorithm = Algorithm.HMAC256(secret)
        val verification = JWT.require(algorithm)
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withClaim(SCOPE_CLAIM, SCOPE_WEEKLY_RANK)
            .withClaim(VERSION_CLAIM, SESSION_VERSION)
            .withClaimPresence(RegisteredClaims.SUBJECT)
            .withClaimPresence(RegisteredClaims.ISSUED_AT)
            .withClaimPresence(RegisteredClaims.NOT_BEFORE)
            .withClaimPresence(RegisteredClaims.EXPIRES_AT)
            .acceptLeeway(CLOCK_SKEW_SECONDS)

        // Auth0 exposes the Clock overload specifically to make date verification deterministic in tests.
        verifier = (verification as JWTVerifier.BaseVerification).build(clock)
    }

    fun issue(principal: WeeklyAuthenticatedPrincipal): String {
        val issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS)
        val expiresAt = issuedAt.plus(sessionTtl)
        val token = JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withSubject(principal.subject)
            .withIssuedAt(issuedAt)
            .withNotBefore(issuedAt)
            .withExpiresAt(expiresAt)
            .withClaim(SCOPE_CLAIM, SCOPE_WEEKLY_RANK)
            .withClaim(VERSION_CLAIM, SESSION_VERSION)
            .sign(algorithm)

        check(token.length <= MAX_TOKEN_CHARS) { "Weekly server session token exceeded transport bound" }
        return token
    }

    /** Returns null for malformed, forged, expired, incorrectly scoped or otherwise invalid sessions. */
    fun verify(token: String): WeeklyAuthenticatedPrincipal? {
        if (token.isBlank() || token.length > MAX_TOKEN_CHARS) return null

        val decoded = runCatching { verifier.verify(token) }.getOrNull() ?: return null
        val issuedAt = decoded.issuedAtAsInstant ?: return null
        val notBefore = decoded.notBeforeAsInstant ?: return null
        val expiresAt = decoded.expiresAtAsInstant ?: return null
        if (notBefore != issuedAt) return null

        val lifetime = Duration.between(issuedAt, expiresAt)
        if (lifetime.isZero || lifetime.isNegative || lifetime > MAX_SESSION_TTL) return null

        val subject = decoded.subject ?: return null
        return runCatching { WeeklyAuthenticatedPrincipal(subject) }.getOrNull()
    }

    companion object {
        const val ISSUER: String = "steamforge"
        const val AUDIENCE: String = "weekly-ranking"
        const val SCOPE_WEEKLY_RANK: String = "weekly:rank"
        const val SESSION_VERSION: String = "1"
        const val MAX_TOKEN_CHARS: Int = 4 * 1024

        private const val SCOPE_CLAIM = "scope"
        private const val VERSION_CLAIM = "session_version"
        private const val MIN_SECRET_BYTES = 32
        private const val CLOCK_SKEW_SECONDS = 30L
        private val MAX_SESSION_TTL: Duration = Duration.ofHours(1)
    }
}
