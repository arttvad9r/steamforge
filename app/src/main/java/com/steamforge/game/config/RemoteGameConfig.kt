package com.steamforge.game.config

import com.steamforge.game.progression.ProgressionConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val REMOTE_CONFIG_SCHEMA_VERSION = 1
private val DEFAULT_WORKSHOP_UPGRADE_COSTS = listOf(20, 35, 55, 80)

/**
 * Product/meta configuration that may eventually come from a remote provider.
 *
 * Fundamental core rules such as board size, spawn probability and RNG behavior are intentionally
 * excluded. Changing those values remotely would require explicit per-run snapshot/version support
 * to preserve deterministic save/replay semantics.
 */
data class RemoteGameConfig(
    val schemaVersion: Int = REMOTE_CONFIG_SCHEMA_VERSION,
    val workshopUpgradeCosts: List<Int> = DEFAULT_WORKSHOP_UPGRADE_COSTS,
    val contractRewardMultiplier: Double = 1.0,
    val rewardMultiplier: Double = 1.0,
    val featureFlags: RemoteFeatureFlags = RemoteFeatureFlags(),
) {
    /** Unknown/invalid payloads collapse to a known-good local config instead of blocking startup. */
    fun sanitized(fallback: RemoteGameConfig = LocalDefaultConfig.value): RemoteGameConfig {
        if (schemaVersion != REMOTE_CONFIG_SCHEMA_VERSION) return fallback

        val safeCosts = workshopUpgradeCosts.takeIf { costs ->
            costs.size == fallback.workshopUpgradeCosts.size &&
                costs.all { it > 0 } &&
                costs.zipWithNext().all { (left, right) -> right > left }
        } ?: fallback.workshopUpgradeCosts

        return copy(
            workshopUpgradeCosts = safeCosts,
            contractRewardMultiplier = contractRewardMultiplier.validMultiplierOr(
                fallback.contractRewardMultiplier,
            ),
            rewardMultiplier = rewardMultiplier.validMultiplierOr(fallback.rewardMultiplier),
        )
    }

    /**
     * Safe bridge for meta progression consumers. Only Workshop costs are overridden in this first
     * slice; pressure, XP, undo/wrench and other gameplay-adjacent values keep their compiled defaults.
     */
    fun toProgressionConfig(base: ProgressionConfig = ProgressionConfig()): ProgressionConfig {
        val safe = sanitized()
        return base.copy(workshopCoreUpgradeCosts = safe.workshopUpgradeCosts)
    }
}

data class RemoteFeatureFlags(
    val weeklyChallengeEnabled: Boolean = false,
    val liveOpsEnabled: Boolean = false,
    val returnLoopEnabled: Boolean = false,
)

/** Canonical compiled fallback. Steamforge must be fully launchable and playable with this alone. */
object LocalDefaultConfig {
    val value: RemoteGameConfig = RemoteGameConfig()
}

enum class RemoteConfigSource {
    LOCAL_DEFAULT,
    CACHE,
    REMOTE,
}

data class RemoteConfigSnapshot(
    val config: RemoteGameConfig,
    val source: RemoteConfigSource,
    val revision: String,
)

enum class RemoteConfigRefreshResult {
    UPDATED,
    LOCAL_FALLBACK,
    FAILED_USING_FALLBACK,
}

/** Provider boundary. Gameplay/meta code never depends on Firebase, RuStore or another SDK directly. */
interface RemoteConfigProvider {
    val snapshot: StateFlow<RemoteConfigSnapshot>

    suspend fun refresh(): RemoteConfigRefreshResult
}

/**
 * Offline provider used until a network-backed adapter is introduced. It is also the guaranteed
 * fallback when a future provider cannot fetch or validate a payload.
 */
class LocalDefaultRemoteConfigProvider(
    defaults: RemoteGameConfig = LocalDefaultConfig.value,
) : RemoteConfigProvider {
    private val _snapshot = MutableStateFlow(
        RemoteConfigSnapshot(
            config = defaults.sanitized(),
            source = RemoteConfigSource.LOCAL_DEFAULT,
            revision = "local-schema-$REMOTE_CONFIG_SCHEMA_VERSION",
        ),
    )

    override val snapshot: StateFlow<RemoteConfigSnapshot> = _snapshot.asStateFlow()

    override suspend fun refresh(): RemoteConfigRefreshResult = RemoteConfigRefreshResult.LOCAL_FALLBACK
}

private fun Double.validMultiplierOr(fallback: Double): Double =
    if (isFinite() && this in 0.25..4.0) this else fallback
