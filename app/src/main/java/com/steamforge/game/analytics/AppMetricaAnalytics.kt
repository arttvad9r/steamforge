package com.steamforge.game.analytics

import android.content.Context
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import org.json.JSONObject

/**
 * AppMetrica-реализация Analytics. Активируется только после выдачи consent
 * (см. AppContainer.onConsentUpdated); отзыв согласия отключает передачу данных.
 */
class AppMetricaAnalytics(context: Context, apiKey: String, private val debugLogging: Boolean) : Analytics {

    init {
        // Минимизация данных: геолокация и рекламные идентификаторы аналитике не нужны
        val config = AppMetricaConfig.newConfigBuilder(apiKey)
            .withLocationTracking(false)
            .withAdvIdentifiersTracking(false)
            .build()
        AppMetrica.activate(context.applicationContext, config)
    }

    fun setSendingEnabled(enabled: Boolean) {
        AppMetrica.setDataSendingEnabled(enabled)
    }

    override fun logEvent(name: String, params: Map<String, Any?>) {
        val json = JSONObject(params.filterValues { it != null }).toString()
        AppMetrica.reportEvent(name, json)
        if (debugLogging) println("AppMetrica: $name $json")
    }
}
