package com.steamforge.game.analytics

/**
 * Аналитика с согласием: пока consent не выдан, делегата нет и события отбрасываются.
 * AppMetrica активируется только после положительного решения пользователя.
 */
class MutableAnalytics(
    private val fallback: Analytics = NoopAnalytics(),
    private val debugLogging: Boolean = false,
) : Analytics {

    private var delegate: Analytics? = null

    fun setDelegate(delegate: Analytics?) {
        this.delegate = delegate
        if (delegate == null && debugLogging) println("Analytics: delegate cleared (consent revoked)")
    }

    override fun logEvent(name: String, params: Map<String, Any?>) {
        val target = delegate ?: return fallback.let { if (debugLogging) it.logEvent(name, params) }
        target.logEvent(name, params)
    }
}
