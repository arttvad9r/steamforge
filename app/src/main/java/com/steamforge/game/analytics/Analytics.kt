package com.steamforge.game.analytics

/**
 * Transitional internal event sink.
 *
 * Steamforge does not ship an analytics SDK or transmit/store telemetry. Existing typed event
 * call sites are kept temporarily so gameplay/progression refactors do not get mixed into the
 * tracking-removal change; every event is discarded in-process.
 */
interface Analytics {
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())
}

class NoopAnalytics : Analytics {
    override fun logEvent(name: String, params: Map<String, Any?>) = Unit
}
