package com.steamforge.game.weekly.server.ktor

import com.steamforge.game.progression.WeeklyRankingResult
import com.steamforge.game.progression.WeeklyRankingSnapshot
import com.steamforge.game.progression.WeeklyRankingStatus
import com.steamforge.game.progression.WeeklyRankingWire
import com.steamforge.game.weekly.server.WeeklyAuthenticatedPrincipal
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyRankingRoutesTest {
    @Test
    fun `health is public and minimal`() = testApplication {
        application {
            installWeeklyRankingRoutes(
                principalResolver = WeeklyHttpPrincipalResolver { null },
                rankingSubmitter = WeeklyRankingSubmitter { _, _ -> WeeklyRankingResult.unavailable() },
            )
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok", response.bodyAsText())
    }

    @Test
    fun `ranking requires bearer token before resolver or service call`() = testApplication {
        var resolverCalled = false
        var submitCalled = false
        application {
            installWeeklyRankingRoutes(
                principalResolver = WeeklyHttpPrincipalResolver {
                    resolverCalled = true
                    PRINCIPAL
                },
                rankingSubmitter = WeeklyRankingSubmitter { _, _ ->
                    submitCalled = true
                    WeeklyRankingResult.unavailable()
                },
            )
        }

        val response = client.post("/weekly/rank") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertFalse(resolverCalled)
        assertFalse(submitCalled)
    }

    @Test
    fun `ranking rejects malformed bearer token before resolver`() = testApplication {
        var resolverCalled = false
        application {
            installWeeklyRankingRoutes(
                principalResolver = WeeklyHttpPrincipalResolver {
                    resolverCalled = true
                    PRINCIPAL
                },
                rankingSubmitter = WeeklyRankingSubmitter { _, _ -> WeeklyRankingResult.unavailable() },
            )
        }

        val response = client.post("/weekly/rank") {
            header(HttpHeaders.Authorization, "Bearer token with spaces")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertFalse(resolverCalled)
    }

    @Test
    fun `invalid server session is unauthorized and never submitted`() = testApplication {
        var capturedToken: String? = null
        var submitCalled = false
        application {
            installWeeklyRankingRoutes(
                principalResolver = WeeklyHttpPrincipalResolver { token ->
                    capturedToken = token
                    null
                },
                rankingSubmitter = WeeklyRankingSubmitter { _, _ ->
                    submitCalled = true
                    WeeklyRankingResult.unavailable()
                },
            )
        }

        val response = client.post("/weekly/rank") {
            header(HttpHeaders.Authorization, "Bearer $SESSION_TOKEN")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(SESSION_TOKEN, capturedToken)
        assertFalse(submitCalled)
    }

    @Test
    fun `session resolver failure is unavailable and never submitted`() = testApplication {
        var submitCalled = false
        application {
            installWeeklyRankingRoutes(
                principalResolver = WeeklyHttpPrincipalResolver { error("session verifier unavailable") },
                rankingSubmitter = WeeklyRankingSubmitter { _, _ ->
                    submitCalled = true
                    WeeklyRankingResult.unavailable()
                },
            )
        }

        val response = client.post("/weekly/rank") {
            authorizedJson("{}")
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertFalse(submitCalled)
    }

    @Test
    fun `ranking rejects non json payload before service call`() = testApplication {
        var submitCalled = false
        application {
            installWeeklyRankingRoutes(
                principalResolver = WeeklyHttpPrincipalResolver { PRINCIPAL },
                rankingSubmitter = WeeklyRankingSubmitter { _, _ ->
                    submitCalled = true
                    WeeklyRankingResult.unavailable()
                },
            )
        }

        val response = client.post("/weekly/rank") {
            header(HttpHeaders.Authorization, "Bearer $SESSION_TOKEN")
            contentType(ContentType.Text.Plain)
            setBody("not-json")
        }

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
        assertFalse(submitCalled)
    }

    @Test
    fun `ranking request is bounded before service call`() = testApplication {
        var submitCalled = false
        application {
            installWeeklyRankingRoutes(
                principalResolver = WeeklyHttpPrincipalResolver { PRINCIPAL },
                rankingSubmitter = WeeklyRankingSubmitter { _, _ ->
                    submitCalled = true
                    WeeklyRankingResult.unavailable()
                },
            )
        }

        val response = client.post("/weekly/rank") {
            authorizedJson("x".repeat(WeeklyRankingWire.MAX_PAYLOAD_BYTES + 1))
        }

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        assertFalse(submitCalled)
    }

    @Test
    fun `authenticated ranking delegates exact token principal and payload and returns stable wire result`() =
        testApplication {
            var capturedToken: String? = null
            var capturedPrincipal: WeeklyAuthenticatedPrincipal? = null
            var capturedPayload: String? = null
            val expected = WeeklyRankingResult.ranked(
                WeeklyRankingSnapshot(
                    challengeId = "weekly-2026-36",
                    score = 1234,
                    percentile = 87.5,
                    rank = 5,
                    participantCount = 40,
                ),
            )
            application {
                installWeeklyRankingRoutes(
                    principalResolver = WeeklyHttpPrincipalResolver { token ->
                        capturedToken = token
                        PRINCIPAL
                    },
                    rankingSubmitter = WeeklyRankingSubmitter { principal, payload ->
                        capturedPrincipal = principal
                        capturedPayload = payload
                        expected
                    },
                )
            }

            val payload = "{\"protocolVersion\":1}"
            val response = client.post("/weekly/rank") {
                authorizedJson(payload)
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(
                response.headers[HttpHeaders.ContentType]
                    ?.startsWith(ContentType.Application.Json.toString()) == true,
            )
            assertEquals(SESSION_TOKEN, capturedToken)
            assertEquals(PRINCIPAL, capturedPrincipal)
            assertEquals(payload, capturedPayload)

            val decoded = WeeklyRankingWire.decodeResult(response.bodyAsText())
            assertNotNull(decoded)
            assertEquals(WeeklyRankingStatus.RANKED, decoded?.status)
            assertEquals(expected, decoded)
        }

    @Test
    fun `unexpected submitter failure is unavailable wire result`() = testApplication {
        application {
            installWeeklyRankingRoutes(
                principalResolver = WeeklyHttpPrincipalResolver { PRINCIPAL },
                rankingSubmitter = WeeklyRankingSubmitter { _, _ -> error("unexpected") },
            )
        }

        val response = client.post("/weekly/rank") {
            authorizedJson("{}")
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        val decoded = WeeklyRankingWire.decodeResult(response.bodyAsText())
        assertEquals(WeeklyRankingStatus.UNAVAILABLE, decoded?.status)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.authorizedJson(body: String) {
        header(HttpHeaders.Authorization, "Bearer $SESSION_TOKEN")
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private companion object {
        const val SESSION_TOKEN = "session-token-123"
        val PRINCIPAL = WeeklyAuthenticatedPrincipal("vkid:subject-123")
    }
}
