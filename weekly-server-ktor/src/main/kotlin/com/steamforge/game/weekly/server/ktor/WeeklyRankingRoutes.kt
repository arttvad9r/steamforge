package com.steamforge.game.weekly.server.ktor

import com.steamforge.game.progression.WeeklyRankingResult
import com.steamforge.game.progression.WeeklyRankingWire
import com.steamforge.game.weekly.server.WeeklyAuthenticatedPrincipal
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.bodylimit.RequestBodyLimit
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlin.coroutines.cancellation.CancellationException

/** Resolves an already verified Steamforge server session token into a competitive Weekly principal. */
fun interface WeeklyHttpPrincipalResolver {
    suspend fun resolve(bearerToken: String): WeeklyAuthenticatedPrincipal?
}

/** HTTP-facing seam around the server application service, kept injectable for route tests. */
fun interface WeeklyRankingSubmitter {
    suspend fun submit(
        principal: WeeklyAuthenticatedPrincipal,
        payload: String,
    ): WeeklyRankingResult
}

/**
 * Installs the production Weekly HTTP surface without owning session verification or persistence.
 *
 * This route itself accepts identity only from a bounded Bearer token. The resolver must validate that
 * Steamforge server session before returning a principal; request-body user ids never participate in
 * authentication. The request payload is capped before [receiveText] so chunked requests cannot bypass
 * the shared 16 KiB Weekly wire limit.
 */
fun Application.installWeeklyRankingRoutes(
    principalResolver: WeeklyHttpPrincipalResolver,
    rankingSubmitter: WeeklyRankingSubmitter,
) {
    routing {
        get("/health") {
            call.respondText(
                text = "ok",
                contentType = ContentType.Text.Plain,
                status = HttpStatusCode.OK,
            )
        }

        route("/weekly/rank") {
            install(RequestBodyLimit) {
                bodyLimit { WeeklyRankingWire.MAX_PAYLOAD_BYTES.toLong() }
            }

            post {
                val bearerToken = call.bearerTokenOrNull()
                if (bearerToken == null) {
                    call.respondText(
                        text = "unauthorized",
                        contentType = ContentType.Text.Plain,
                        status = HttpStatusCode.Unauthorized,
                    )
                    return@post
                }

                val principal = try {
                    principalResolver.resolve(bearerToken)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    call.respondText(
                        text = "authentication unavailable",
                        contentType = ContentType.Text.Plain,
                        status = HttpStatusCode.ServiceUnavailable,
                    )
                    return@post
                }
                if (principal == null) {
                    call.respondText(
                        text = "unauthorized",
                        contentType = ContentType.Text.Plain,
                        status = HttpStatusCode.Unauthorized,
                    )
                    return@post
                }

                if (!call.request.contentType().match(ContentType.Application.Json)) {
                    call.respondText(
                        text = "unsupported media type",
                        contentType = ContentType.Text.Plain,
                        status = HttpStatusCode.UnsupportedMediaType,
                    )
                    return@post
                }

                val payload = call.receiveText()
                val result = try {
                    rankingSubmitter.submit(principal, payload)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    call.respondWireResult(
                        result = WeeklyRankingResult.unavailable(),
                        status = HttpStatusCode.ServiceUnavailable,
                    )
                    return@post
                }
                call.respondWireResult(result, HttpStatusCode.OK)
            }
        }
    }
}

private suspend fun ApplicationCall.respondWireResult(
    result: WeeklyRankingResult,
    status: HttpStatusCode,
) {
    respondText(
        text = WeeklyRankingWire.encodeResult(result),
        contentType = ContentType.Application.Json,
        status = status,
    )
}

private fun ApplicationCall.bearerTokenOrNull(): String? {
    val authorization = request.headers[HttpHeaders.Authorization] ?: return null
    val separator = authorization.indexOf(' ')
    if (separator <= 0) return null
    if (!authorization.substring(0, separator).equals(BEARER_SCHEME, ignoreCase = true)) return null

    val token = authorization.substring(separator + 1)
    if (token.isEmpty() || token.length > MAX_BEARER_TOKEN_CHARS) return null
    if (token.any(Char::isWhitespace)) return null
    return token
}

private const val BEARER_SCHEME = "Bearer"
private const val MAX_BEARER_TOKEN_CHARS = 4 * 1024
