package com.steamforge.game

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.steamforge.game.cosmetics.CosmeticLoadout
import com.steamforge.game.progression.DailyChallenges
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.progression.Onboarding
import com.steamforge.game.progression.WeeklyChallenges
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.ui.achievements.AchievementsScreen
import com.steamforge.game.ui.achievements.AchievementsViewModel
import com.steamforge.game.ui.blueprints.BlueprintsScreen
import com.steamforge.game.ui.blueprints.BlueprintsViewModel
import com.steamforge.game.ui.components.SteamButton
import com.steamforge.game.ui.components.SteamButtonStyle
import com.steamforge.game.ui.components.SteamDecisionDialog
import com.steamforge.game.ui.contracts.ContractsScreen
import com.steamforge.game.ui.contracts.ContractsViewModel
import com.steamforge.game.ui.cosmetics.CosmeticsScreen
import com.steamforge.game.ui.cosmetics.CosmeticsViewModel
import com.steamforge.game.ui.event.EventScreen
import com.steamforge.game.ui.event.EventViewModel
import com.steamforge.game.ui.game.GameViewModel
import com.steamforge.game.ui.game.MilestoneGameScreen
import com.steamforge.game.ui.home.HomeScreen
import com.steamforge.game.ui.home.HomeViewModel
import com.steamforge.game.ui.onboarding.OnboardingGameScreen
import com.steamforge.game.ui.onboarding.OnboardingWorkshopScreen
import com.steamforge.game.ui.settings.SettingsScreen
import com.steamforge.game.ui.settings.SettingsViewModel
import com.steamforge.game.ui.weekly.WeeklyScreen
import com.steamforge.game.ui.weekly.WeeklyViewModel
import com.steamforge.game.ui.workshop.WorkshopScreen
import com.steamforge.game.ui.workshop.WorkshopViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data object Workshop : NavKey
@Serializable data object FoundryEvent : NavKey
@Serializable data object Weekly : NavKey
@Serializable data object WeeklyGame : NavKey
@Serializable data object Contracts : NavKey
@Serializable data object Blueprints : NavKey
@Serializable data class Game(val daily: Boolean = false) : NavKey
@Serializable data object Achievements : NavKey
@Serializable data object Settings : NavKey
@Serializable data object Cosmetics : NavKey

@Composable
fun MainNavigation(container: AppContainer, modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Home)
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
    val onboardingStep by container.onboarding.step.collectAsStateWithLifecycle(initialValue = null as Int?)
    val shellScope = rememberCoroutineScope()

    if (consent == null) {
        ConsentDialog(
            privacyPolicyUrl = BuildConfig.PRIVACY_POLICY_URL,
            onDecide = { granted ->
                shellScope.launch { container.repo.updateProgress { it.copy(analyticsConsent = granted) } }
            },
        )
    }

    fun back() = backStack.removeLastOrNull()

    if (onboardingStep == null) return

    if (consent != null && onboardingStep == Onboarding.CORE) {
        val vm: GameViewModel = viewModel(key = "onboarding-core") {
            GameViewModel(
                repo = container.repo,
                analytics = container.analytics,
                cfg = container.config.config.value.progressionConfig(),
                systemAnimationsEnabled = systemAnimationsEnabled,
            )
        }
        OnboardingGameScreen(
            vm = vm,
            sfx = container.sfx,
            onOpenWorkshop = {
                vm.exit()
                container.analytics.logEvent("onboarding_core_completed", mapOf("moves" to vm.ui.value.state.moves))
                shellScope.launch { container.onboarding.setStep(Onboarding.WORKSHOP) }
            },
            onSkip = {
                vm.exit()
                container.analytics.logEvent("onboarding_skipped", mapOf("moves" to vm.ui.value.state.moves))
                shellScope.launch { container.onboarding.setStep(Onboarding.COMPLETE) }
            },
            modifier = modifier,
        )
        return
    }

    if (consent != null && onboardingStep == Onboarding.WORKSHOP) {
        val vm: WorkshopViewModel = viewModel(key = "onboarding-workshop") {
            WorkshopViewModel(container.repo, configProvider = container.config)
        }
        OnboardingWorkshopScreen(
            vm = vm,
            onOpenContracts = {
                shellScope.launch {
                    container.onboarding.setStep(Onboarding.COMPLETE)
                    container.analytics.logEvent("onboarding_completed")
                    backStack.add(Contracts)
                }
            },
            modifier = modifier,
        )
        return
    }

    NavDisplay(
        backStack = backStack,
        onBack = { back() },
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Home> {
                val vm: HomeViewModel = viewModel {
                    HomeViewModel(container.repo, configProvider = container.config)
                }
                HomeScreen(
                    vm = vm,
                    onPlay = { backStack.add(Game(daily = false)) },
                    onWorkshop = { backStack.add(Workshop) },
                    onEvent = { backStack.add(FoundryEvent) },
                    onWeekly = { backStack.add(Weekly) },
                    onContracts = { backStack.add(Contracts) },
                    onBlueprints = { backStack.add(Blueprints) },
                    onDaily = { backStack.add(Game(daily = true)) },
                    onSettings = { backStack.add(Settings) },
                )
            }
            entry<Workshop> {
                val vm: WorkshopViewModel = viewModel {
                    WorkshopViewModel(container.repo, configProvider = container.config)
                }
                val cosmeticLoadout = effectiveCosmeticLoadout(container)
                WorkshopScreen(
                    vm = vm,
                    sfx = container.sfx,
                    onPlay = { backStack.add(Game(daily = false)) },
                    onDaily = { backStack.add(Game(daily = true)) },
                    onAchievements = { backStack.add(Achievements) },
                    onSettings = { backStack.add(Settings) },
                    workshopTheme = cosmeticLoadout.workshopTheme,
                )
            }
            entry<FoundryEvent> {
                val vm: EventViewModel = viewModel {
                    EventViewModel(
                        repo = container.repo,
                        configProvider = container.config,
                        analytics = container.analytics,
                    )
                }
                EventScreen(
                    vm = vm,
                    onBack = { back() },
                    onPlay = { backStack.add(Game(daily = false)) },
                )
            }
            entry<Weekly> {
                val vm: WeeklyViewModel = viewModel { WeeklyViewModel(container.repo) }
                WeeklyScreen(
                    vm = vm,
                    onBack = { back() },
                    onPlay = { backStack.add(WeeklyGame) },
                )
            }
            entry<WeeklyGame> {
                val challenge = WeeklyChallenges.forEpochDay(LocalDay.todayEpochDay())
                val cosmeticLoadout = effectiveCosmeticLoadout(container)
                val vm: GameViewModel = viewModel(key = "weekly-${challenge.id}") {
                    GameViewModel(
                        repo = container.repo,
                        analytics = container.analytics,
                        ads = container.ads,
                        cfg = container.config.config.value.progressionConfig(),
                        weeklyChallenge = challenge,
                        systemAnimationsEnabled = systemAnimationsEnabled,
                    )
                }
                MilestoneGameScreen(
                    vm = vm,
                    sfx = container.sfx,
                    ads = container.ads,
                    onExit = { back() },
                    tileSet = cosmeticLoadout.tileSet,
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
            entry<Contracts> {
                val vm: ContractsViewModel = viewModel { ContractsViewModel(container.repo) }
                ContractsScreen(vm = vm, onBack = { back() })
            }
            entry<Blueprints> {
                val vm: BlueprintsViewModel = viewModel { BlueprintsViewModel(container.repo) }
                BlueprintsScreen(vm = vm, onBack = { back() })
            }
            entry<Game> { key ->
                val cosmeticLoadout = effectiveCosmeticLoadout(container)
                val vm: GameViewModel = viewModel(key = if (key.daily) "daily" else "normal") {
                    GameViewModel(
                        repo = container.repo,
                        analytics = container.analytics,
                        ads = container.ads,
                        cfg = container.config.config.value.progressionConfig(),
                        dailyMode = key.daily,
                        dailyProvider = { DailyChallenges.forEpochDay(LocalDay.todayEpochDay()) },
                        systemAnimationsEnabled = systemAnimationsEnabled,
                    )
                }
                MilestoneGameScreen(
                    vm = vm,
                    sfx = container.sfx,
                    ads = container.ads,
                    onExit = { back() },
                    tileSet = cosmeticLoadout.tileSet,
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
            entry<Achievements> {
                val vm: AchievementsViewModel = viewModel { AchievementsViewModel(container.repo) }
                AchievementsScreen(vm = vm, onBack = { back() })
            }
            entry<Settings> {
                val vm: SettingsViewModel = viewModel { SettingsViewModel(container.repo, container.billing) }
                SettingsScreen(
                    vm = vm,
                    onBack = { back() },
                    onCosmetics = { backStack.add(Cosmetics) },
                )
            }
            entry<Cosmetics> {
                val vm: CosmeticsViewModel = viewModel { CosmeticsViewModel(container.billing, container.cosmetics) }
                CosmeticsScreen(vm = vm, onBack = { back() })
            }
        },
    )
}

@Composable
private fun effectiveCosmeticLoadout(container: AppContainer): CosmeticLoadout {
    val selected by container.cosmetics.loadout.collectAsStateWithLifecycle(initialValue = CosmeticLoadout())
    val purchases by container.billing.cosmetics.collectAsStateWithLifecycle()
    return selected.effective(
        tileSetOwned = purchases.tileSetOwned,
        workshopThemeOwned = purchases.workshopThemeOwned,
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
        onDismissRequest = { },
        body = {
            Column {
                Text(
                    "Игра хранит прогресс на устройстве. Для статистики (AppMetrica) и рекламы " +
                        "(Яндекс) могут передаваться технические данные об использовании. " +
                        "Отказ отключит AppMetrica и сообщит рекламному SDK, что согласие на обработку " +
                        "данных не дано; реклама может продолжать показываться. Игра останется полной.",
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
