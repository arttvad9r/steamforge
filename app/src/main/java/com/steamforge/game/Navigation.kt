package com.steamforge.game

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.steamforge.game.analytics.NoopAnalytics
import com.steamforge.game.progression.DailyChallenges
import com.steamforge.game.progression.LocalDay
import com.steamforge.game.ui.achievements.AchievementsScreen
import com.steamforge.game.ui.achievements.AchievementsViewModel
import com.steamforge.game.ui.contracts.ContractsScreen
import com.steamforge.game.ui.contracts.ContractsViewModel
import com.steamforge.game.ui.game.GameViewModel
import com.steamforge.game.ui.game.PersistenceGuardedGameScreen
import com.steamforge.game.ui.game.WeeklyRankingViewModel
import com.steamforge.game.ui.home.HomeScreen
import com.steamforge.game.ui.home.HomeViewModel
import com.steamforge.game.ui.profile.ProfileScreen
import com.steamforge.game.ui.profile.ProfileViewModel
import com.steamforge.game.ui.settings.SettingsScreen
import com.steamforge.game.ui.settings.SettingsViewModel
import com.steamforge.game.ui.workshop.WorkshopScreen
import com.steamforge.game.ui.workshop.WorkshopViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data object Workshop : NavKey
@Serializable data object Contracts : NavKey
@Serializable data class Game(val mode: GameRunMode = GameRunMode.NORMAL) : NavKey
@Serializable data object Profile : NavKey
@Serializable data object Achievements : NavKey
@Serializable data object Settings : NavKey

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
            entry<Home> {
                val vm: HomeViewModel = viewModel { HomeViewModel(container.repo) }
                HomeScreen(
                    vm = vm,
                    onPlay = { backStack.add(Game(GameRunMode.NORMAL)) },
                    onWorkshop = { backStack.add(Workshop) },
                    onContracts = { backStack.add(Contracts) },
                    onDaily = { backStack.add(Game(GameRunMode.DAILY)) },
                    onAchievements = { backStack.add(Profile) },
                    onSettings = { backStack.add(Settings) },
                )
            }
            entry<Workshop> {
                val vm: WorkshopViewModel = viewModel {
                    WorkshopViewModel(
                        repo = container.repo,
                        remoteConfigProvider = container.remoteConfig,
                    )
                }
                WorkshopScreen(
                    vm = vm,
                    sfx = container.sfx,
                    onPlay = { backStack.add(Game(GameRunMode.NORMAL)) },
                    onDaily = { backStack.add(Game(GameRunMode.DAILY)) },
                    onAchievements = { backStack.add(Achievements) },
                    onSettings = { backStack.add(Settings) },
                )
            }
            entry<Contracts> {
                val vm: ContractsViewModel = viewModel {
                    ContractsViewModel(
                        repo = container.repo,
                        remoteConfigProvider = container.remoteConfig,
                    )
                }
                ContractsScreen(vm = vm, onBack = { back() })
            }
            entry<Game> { key ->
                val vm: GameViewModel = viewModel(key = key.mode.wireName) {
                    GameViewModel(
                        repo = container.repo,
                        analytics = NoopAnalytics(),
                        ads = container.ads,
                        runMode = key.mode,
                        dailyProvider = { DailyChallenges.forEpochDay(LocalDay.todayEpochDay()) },
                        systemAnimationsEnabled = systemAnimationsEnabled,
                    )
                }
                val weeklyRankingVm: WeeklyRankingViewModel? = if (key.mode == GameRunMode.WEEKLY) {
                    viewModel(key = "weekly-ranking-${key.mode.wireName}") {
                        WeeklyRankingViewModel(container.weeklyRankingProvider)
                    }
                } else {
                    null
                }
                LaunchedEffect(vm, weeklyRankingVm) {
                    val rankingVm = weeklyRankingVm ?: return@LaunchedEffect
                    vm.weeklySubmission.collect(rankingVm::onSubmission)
                }
                val firstGameFlow = remember(container.repo, key.mode) {
                    container.repo.progress.map { progress ->
                        key.mode == GameRunMode.NORMAL && progress.stats.gamesPlayed == 0
                    }
                }
                val isFirstGame by firstGameFlow.collectAsStateWithLifecycle(initialValue = false)
                PersistenceGuardedGameScreen(
                    vm = vm,
                    sfx = container.sfx,
                    ads = container.ads,
                    isFirstGame = isFirstGame,
                    onExit = { back() },
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
            entry<Profile> {
                val vm: ProfileViewModel = viewModel { ProfileViewModel(container.repo) }
                ProfileScreen(
                    vm = vm,
                    onBack = { back() },
                    onAchievements = { backStack.add(Achievements) },
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
