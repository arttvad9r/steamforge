package com.steamforge.game.analytics

/** Абстракция аналитики. Реализации не должны проникать в GameEngine. */
interface Analytics {
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())
}

/** Безопасная реализация по умолчанию: без сети, без ключей, пишет в Logcat в debug-сборках. */
class NoopAnalytics(private val debugLogging: Boolean = false) : Analytics {
    override fun logEvent(name: String, params: Map<String, Any?>) {
        if (debugLogging) println("Analytics: $name $params")
    }
}
