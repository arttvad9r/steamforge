package com.steamforge.game

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.steamforge.game.progression.DailyChallenges
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.ui.achievements.AchievementsScreen
import com.steamforge.game.ui.achievements.AchievementsViewModel
import com.steamforge.game.ui.components.SteamButton
import com.steamforge.game.ui.components.SteamButtonStyle
import com.steamforge.game.ui.components.SteamDecisionDialog
import com.steamforge.game.ui.game.GameScreen
import com.steamforge.game.ui.game.GameViewModel
import com.steamforge.game.ui.settings.SettingsScreen
import com.steamforge.game.ui.settings.SettingsViewModel
import com.steamforge.game.ui.workshop.WorkshopScreen
import com.steamforge.game.ui.workshop.WorkshopViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object Workshop : NavKey
@Serializable data class Game(val daily: Boolean = false) : NavKey
@Serializable data object Achievements : NavKey
@Serializable data object Settings : NavKey

@Composable
fun MainNavigation(container: AppContainer, modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Workshop)
    val systemAnimationsEnabled = LocalContext.current.let {
        android.provider.Settings.Global.getFloat(
            it.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) != 0f
    }

    val consentFlow = androidx.compose.runtime.remember(container.repo) {
        container.repo.progress.map { it.analyticsConsent }
    }
    val consent by consentFlow.collectAsStateWithLifecycle(initialValue = null as Boolean?)
    val consentScope = rememberCoroutineScope()
    if (consent == null) {
        ConsentDialog(
            privacyPolicyUrl = BuildConfig.PRIVACY_POLICY_URL,
            onDecide = { granted ->
                consentScope.launch { container.repo.updateProgress { it.copy(analyticsConsent = granted) } }
            },
        )
    }

    fun back() = backStack.removeLastOrNull()

    NavDisplay(
        backStack = backStack,
        onBack = { back() },
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Workshop> {
                val vm: WorkshopViewModel = viewModel { WorkshopViewModel(container.repo) }
                WorkshopScreen(
                    vm = vm,
                    sfx = container.sfx,
                    onPlay = { backStack.add(Game(daily = false)) },
                    onDaily = { backStack.add(Game(daily = true)) },
                    onAchievements = { backStack.add(Achievements) },
                    onSettings = { backStack.add(Settings) },
                )
            }
            entry<Game> { key ->
                val vm: GameViewModel = viewModel(key = if (key.daily) "daily" else "normal") {
                    GameViewModel(
                        repo = container.repo,
                        analytics = container.analytics,
                        ads = container.ads,
                        dailyMode = key.daily,
                        dailyProvider = { DailyChallenges.forEpochDay(LocalDay.todayEpochDay()) },
                        systemAnimationsEnabled = systemAnimationsEnabled,
                    )
                }
                GameScreen(
                    vm = vm,
                    sfx = container.sfx,
                    ads = container.ads,
                    onExit = { back() },
                )
            }
            entry<Achievements> {
                val vm: AchievementsViewModel = viewModel { AchievementsViewModel(container.repo) }
                AchievementsScreen(vm = vm, onBack = { back() })
            }
            entry<Settings> {
                val vm: SettingsViewModel = viewModel { SettingsViewModel(container.repo) }
                SettingsScreen(vm = vm, onBack = { back() })
            }
        },
    )
}

@Composable
private fun ConsentDialog(
    privacyPolicyUrl: String,
    onDecide: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    SteamDecisionDialog(
        title = "ПРИВАТНОСТЬ",
        onDismissRequest = { /* решение обязательно; до него SDK не активируются */ },
        body = {
            Column {
                Text(
                    "Игра хранит прогресс на устройстве. Для статистики (AppMetrica) и рекламы " +
                        "(Яндекс) могут передаваться технические данные об использовании. " +
                        "Отказ отключит AppMetrica и персонализацию рекламы; игра останется полной.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
                Spacer(Modifier.height(12.dp))
                if (privacyPolicyUrl.isNotBlank()) {
                    SteamButton(
                        text = "ОТКРЫТЬ ПОЛИТИКУ",
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, privacyPolicyUrl.toUri()))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        style = SteamButtonStyle.Dark,
                    )
                } else {
                    Text(
                        "Политика конфиденциальности будет доступна до production-релиза.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }
        },
        actions = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SteamButton(
                    text = "ОТКЛЮЧИТЬ",
                    onClick = { onDecide(false) },
                    modifier = Modifier.weight(1f),
                    style = SteamButtonStyle.Dark,
                )
                SteamButton(
                    text = "РАЗРЕШИТЬ",
                    onClick = { onDecide(true) },
                    modifier = Modifier.weight(1f),
                    style = SteamButtonStyle.Teal,
                )
            }
        },
    )
}
