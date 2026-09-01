package com.steamforge.game.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** SDK-neutral source. Firebase/RuStore adapters implement only this boundary. */
fun interface RemoteConfigSource {
    suspend fun fetch(): RemoteGameConfig?
}

interface GameConfigProvider {
    val config: StateFlow<RemoteGameConfig>
    suspend fun refresh(): Boolean
}

/** Полностью offline-safe provider: приложение всегда стартует с LocalDefaultConfig. */
class FallbackGameConfigProvider(
    private val remote: RemoteConfigSource? = null,
    defaults: RemoteGameConfig = LocalDefaultConfig.value,
) : GameConfigProvider {
    private val mutable = MutableStateFlow(defaults)
    override val config: StateFlow<RemoteGameConfig> = mutable.asStateFlow()

    override suspend fun refresh(): Boolean {
        val source = remote ?: return false
        val fetched = runCatching { source.fetch() }.getOrNull() ?: return false
        // Typed config sanitizes individual values at consumption boundaries. A failed fetch never clears defaults.
        mutable.value = fetched
        return true
    }
}

/** Tests/debug builds can update the snapshot without a network SDK. */
class MutableGameConfigProvider(
    initial: RemoteGameConfig = LocalDefaultConfig.value,
) : GameConfigProvider {
    private val mutable = MutableStateFlow(initial)
    override val config: StateFlow<RemoteGameConfig> = mutable.asStateFlow()

    fun set(value: RemoteGameConfig) {
        mutable.value = value
    }

    override suspend fun refresh(): Boolean = false
}
