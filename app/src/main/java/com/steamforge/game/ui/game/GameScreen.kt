package com.steamforge.game.ui.game

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.core.Elements
import com.steamforge.game.core.GameState
import com.steamforge.game.core.Move
import com.steamforge.game.core.MoveResult
import com.steamforge.game.core.Tile
import com.steamforge.game.monetization.AdsManager
import com.steamforge.game.progression.DailyChallenge
import com.steamforge.game.progression.DailyGoalType
import com.steamforge.game.sound.Sfx
import com.steamforge.game.sound.SfxPlayer
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.BrassDark
import com.steamforge.game.theme.Copper
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TealSurface
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.theme.tileBevel
import com.steamforge.game.theme.tileColors
import com.steamforge.game.ui.components.BrassRoundButton
import com.steamforge.game.ui.components.PressureDial
import com.steamforge.game.ui.components.StatPlate
import com.steamforge.game.ui.components.SteamBackdrop
import com.steamforge.game.ui.components.SteamButton
import com.steamforge.game.ui.components.SteamButtonStyle
import com.steamforge.game.ui.components.SteamLogoHeader
import com.steamforge.game.ui.components.SteamPanel
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val MOVE_MS = 110
private const val POP_MS = 140
private const val SPAWN_MS = 130

@Composable
fun GameScreen(
    vm: GameViewModel,
    sfx: SfxPlayer,
    ads: AdsManager,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val rewardedReady by ads.rewardedReady.collectAsStateWithLifecycle()
    var exitHandled by remember { mutableStateOf(false) }
    val view = LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var prevOverdrive by remember { mutableIntStateOf(0) }
    var prevFinished by remember { mutableStateOf(false) }
    var prevWon by remember { mutableStateOf(false) }

    fun performGameplayHaptic(feedbackConstant: Int) {
        if (ui.hapticsEnabled) ViewCompat.performHapticFeedback(view, feedbackConstant)
    }

    fun undoWithFeedback() {
        val beforeUndo = vm.ui.value.state
        vm.undo()
        if (vm.ui.value.state != beforeUndo) {
            sfx.play(Sfx.UNDO)
            performGameplayHaptic(HapticFeedbackConstantsCompat.CONFIRM)
        }
    }

    LaunchedEffect(ui.lastResult) {
        val res = ui.lastResult ?: return@LaunchedEffect
        if (res.merges.isNotEmpty()) {
            val maxLevel = res.merges.maxOf { it.tile.level }
            val feedback = mergeFeedbackProfile(maxLevel, res.merges.size)
            sfx.play(
                when (feedback.tier) {
                    MergeFeedbackTier.HIGH -> Sfx.MERGE_HIGH
                    MergeFeedbackTier.MID -> Sfx.MERGE_MID
                    MergeFeedbackTier.LOW -> Sfx.MERGE_LOW
                },
                rate = feedback.playbackRate,
            )
            performGameplayHaptic(HapticFeedbackConstantsCompat.CONFIRM)
        } else {
            sfx.play(Sfx.MOVE)
        }
    }
    LaunchedEffect(ui.overdriveRemaining) {
        if (ui.overdriveRemaining > 0 && prevOverdrive == 0) {
            sfx.play(Sfx.OVERDRIVE)
            performGameplayHaptic(HapticFeedbackConstantsCompat.CONFIRM)
        }
        prevOverdrive = ui.overdriveRemaining
    }
    LaunchedEffect(ui.finished) {
        if (ui.finished && !prevFinished) {
            sfx.play(if (ui.effects?.levelUps?.isNotEmpty() == true) Sfx.LEVEL_UP else Sfx.GAME_OVER)
            performGameplayHaptic(HapticFeedbackConstantsCompat.REJECT)
        }
        prevFinished = ui.finished
    }
    LaunchedEffect(ui.winCelebrated) {
        if (ui.winCelebrated && !prevWon) {
            sfx.play(Sfx.WIN)
            performGameplayHaptic(HapticFeedbackConstantsCompat.CONFIRM)
        }
        prevWon = ui.winCelebrated
    }

    fun leave() {
        if (!exitHandled) {
            exitHandled = true
            vm.exit()
            onExit()
            if (ui.finished && context is Activity) ads.maybeShowInterstitial(context)
        }
    }

    BackHandler { leave() }

    SteamBackdrop(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> vm.onMove(Move.LEFT)
                    Key.DirectionRight -> vm.onMove(Move.RIGHT)
                    Key.DirectionUp -> vm.onMove(Move.UP)
                    Key.DirectionDown -> vm.onMove(Move.DOWN)
                    else -> return@onPreviewKeyEvent false
                }
                true
            },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compactLandscape = maxWidth > maxHeight && maxHeight < 600.dp
            if (compactLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center,
                    ) {
                        val boardSize = minOf(maxWidth, maxHeight)
                        BoardView(
                            state = ui.state,
                            lastResult = ui.lastResult,
                            previousTiles = ui.previousTiles,
                            animationsActive = ui.animationsActive,
                            removingMode = ui.removingMode,
                            canRemove = vm::canRemoveTile,
                            onTileClick = vm::removeTile,
                            onSwipe = vm::onMove,
                            modifier = Modifier.size(boardSize),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight()
                            .widthIn(max = 400.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            BrassRoundButton("←", "В мастерскую", ::leave)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "STEAMFORGE",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = BrassBright,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                                Text(
                                    if (ui.daily != null) "ИСПЫТАНИЕ ДНЯ" else "МЕХАНИЧЕСКОЕ ЯДРО",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))

                        Row(Modifier.fillMaxWidth()) {
                            StatPlate("СЧЁТ", ui.state.score.toString(), Modifier.weight(1f), BrassBright)
                            Spacer(Modifier.width(5.dp))
                            StatPlate("ЛУЧШИЙ", ui.best.toString(), Modifier.weight(1f), TextWarm)
                        }
                        Spacer(Modifier.height(4.dp))

                        SteamPanel(
                            modifier = Modifier.fillMaxWidth(),
                            highlighted = ui.overdriveRemaining > 0,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            PressureStrip(
                                pressure = ui.pressure,
                                overdriveRemaining = ui.overdriveRemaining,
                            )
                            ui.daily?.let { daily ->
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    dailyGoalText(daily) + if (ui.dailySatisfied) " — выполнено" else "",
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (ui.dailySatisfied) TealGlow else TextMuted,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))

                        SteamPanel(
                            Modifier.fillMaxWidth(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("ИНСТРУМЕНТЫ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Spacer(Modifier.weight(1f))
                                Text("◆ ${ui.gems}", style = MaterialTheme.typography.labelMedium, color = TealGlow)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ToolButton(
                                    symbol = "↶",
                                    label = if (ui.freeUndosLeft > 0) "ОТМЕНА ${ui.freeUndosLeft}" else "ОТМЕНА ◆5",
                                    active = ui.canUndo && !ui.finished,
                                    onClick = ::undoWithFeedback,
                                    modifier = Modifier.weight(1f),
                                )
                                ToolButton(
                                    symbol = "⚒",
                                    label = if (ui.removingMode) "ВЫБЕРИ ПЛИТКУ" else "КЛЮЧ ◆10",
                                    active = !ui.finished,
                                    selected = ui.removingMode,
                                    onClick = vm::toggleRemovingMode,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Свайпните по полю · одинаковые детали объединяются",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                        .widthIn(max = 560.dp)
                        .padding(horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BrassRoundButton("←", "В мастерскую", ::leave)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "STEAMFORGE",
                                style = MaterialTheme.typography.titleMedium,
                                color = BrassBright,
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text(
                                if (ui.daily != null) "ИСПЫТАНИЕ ДНЯ" else "МЕХАНИЧЕСКОЕ ЯДРО",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    SteamPanel(
                        modifier = Modifier.fillMaxWidth(),
                        highlighted = ui.overdriveRemaining > 0,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HudMetric(
                                label = "СЧЁТ",
                                value = ui.state.score.toString(),
                                accent = BrassBright,
                                modifier = Modifier.weight(1f),
                            )
                            HudMetric(
                                label = "ЛУЧШИЙ",
                                value = ui.best.toString(),
                                accent = TextWarm,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        PressureStrip(
                            pressure = ui.pressure,
                            overdriveRemaining = ui.overdriveRemaining,
                        )
                        val daily = ui.daily
                        if (daily != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                dailyGoalText(daily) + if (ui.dailySatisfied) " · выполнено" else "",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (ui.dailySatisfied) TealGlow else TextWarm,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    BoardView(
                        state = ui.state,
                        lastResult = ui.lastResult,
                        previousTiles = ui.previousTiles,
                        animationsActive = ui.animationsActive,
                        removingMode = ui.removingMode,
                        canRemove = vm::canRemoveTile,
                        onTileClick = vm::removeTile,
                        onSwipe = vm::onMove,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    )
                    Spacer(Modifier.height(9.dp))

                    SteamPanel(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(7.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("ИНСТРУМЕНТЫ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Spacer(Modifier.weight(1f))
                            Text("◆ ${ui.gems}", style = MaterialTheme.typography.labelMedium, color = TealGlow)
                        }
                        Spacer(Modifier.height(5.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ToolButton(
                                symbol = "↶",
                                label = if (ui.freeUndosLeft > 0) "ОТМЕНА ${ui.freeUndosLeft}" else "ОТМЕНА ◆5",
                                active = ui.canUndo && !ui.finished,
                                onClick = ::undoWithFeedback,
                                modifier = Modifier.weight(1f),
                            )
                            ToolButton(
                                symbol = "⚒",
                                label = if (ui.removingMode) "ВЫБЕРИ ПЛИТКУ" else "КЛЮЧ ◆10",
                                active = !ui.finished,
                                selected = ui.removingMode,
                                onClick = vm::toggleRemovingMode,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Свайпните по полю · одинаковые детали объединяются",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    if (ui.finished) {
        val activity = context as? Activity
        GameOverOverlay(
            ui = ui,
            rewardedAvailable = rewardedReady && (ui.effects?.gemsGained ?: 0) > 0 && !ui.rewardDoubled,
            onRewarded = {
                if (activity != null) ads.showRewarded(activity) { vm.grantDoubleReward() }
            },
            onRestart = {
                if (activity != null) ads.maybeShowInterstitial(activity)
                vm.restart()
            },
            onExit = ::leave,
        )
    } else if (ui.winCelebrated && !ui.winBannerShown) {
        CoreOnlineOverlay(onContinue = vm::markWinBannerShown, onExit = ::leave)
    }
}

@Composable
private fun HudMetric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val compactScreen = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 390
    val valueStyle = when {
        compactScreen && value.length > 7 -> MaterialTheme.typography.labelMedium
        value.length <= 5 -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.labelLarge
    }
    val labelStyle = if (compactScreen) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    Column(
        modifier = modifier.semantics { contentDescription = "$label: $value" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = labelStyle,
            color = TextMuted,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = value,
            modifier = Modifier.fillMaxWidth(),
            style = valueStyle,
            color = accent,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun PressureStrip(
    pressure: Int,
    overdriveRemaining: Int,
    modifier: Modifier = Modifier,
) {
    val active = overdriveRemaining > 0
    val fraction = if (active) 1f else pressure.coerceIn(0, 100) / 100f
    val description = if (active) {
        "Overdrive x2, осталось $overdriveRemaining объединений"
    } else {
        "Давление $pressure процентов"
    }
    val shape = RoundedCornerShape(99.dp)

    Column(modifier = modifier.semantics { contentDescription = description }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (active) "OVERDRIVE ×2" else "ДАВЛЕНИЕ",
                style = MaterialTheme.typography.labelSmall,
                color = if (active) TealGlow else TextMuted,
            )
            Text(
                if (active) "$overdriveRemaining объедин." else "$pressure%",
                style = MaterialTheme.typography.labelSmall,
                color = if (active) TealGlow else TextWarm,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(shape)
                .background(Recess.copy(alpha = 0.92f))
                .border(1.dp, BrassDark.copy(alpha = 0.45f), shape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(shape)
                    .background(if (active) TealGlow else Brass),
            )
        }
    }
}

fun dailyGoalText(daily: DailyChallenge): String = when (daily.type) {
    DailyGoalType.REACH_TILE -> "Собери деталь ${daily.target}"
    DailyGoalType.REACH_SCORE -> "Набери ${daily.target} очков"
    DailyGoalType.HIGH_MERGES -> "Сделай ${daily.target} объединения 64+"
}

@Composable
fun PressureGauge(
    pressure: Int,
    overdriveRemaining: Int,
    animationsActive: Boolean,
    modifier: Modifier = Modifier,
) {
    PressureDial(pressure, overdriveRemaining, modifier)
}

@Composable
fun BoardView(
    state: GameState,
    lastResult: MoveResult?,
    previousTiles: List<Tile>,
    animationsActive: Boolean,
    removingMode: Boolean,
    canRemove: (Tile) -> Boolean,
    onTileClick: (Tile) -> Unit,
    onSwipe: (Move) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    BoxWithConstraints(modifier) {
        val board = maxWidth
        val gap = board * 0.021f
        val cell = (board - gap * 5f) / 4f

        fun cellOffset(row: Int, col: Int): Dp2 = Dp2(gap + (cell + gap) * col, gap + (cell + gap) * row)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(10.dp, shape, ambientColor = Color.Black.copy(alpha = 0.40f), spotColor = Color.Black.copy(alpha = 0.56f))
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Panel.copy(alpha = 0.94f),
                            Color(0xFF131C24),
                            Color(0xFF0D151B),
                            Recess,
                        ),
                    ),
                )
                .border(1.dp, Brass.copy(alpha = 0.48f), shape)
                .swipeDetector(onSwipe),
        ) {
            for (r in 0 until state.size) {
                for (c in 0 until state.size) {
                    val off = cellOffset(r, c)
                    val cellShape = RoundedCornerShape(11.dp)
                    Box(
                        modifier = Modifier
                            .offset(off.x, off.y)
                            .size(cell)
                            .clip(cellShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF0A1015),
                                        Recess.copy(alpha = 0.98f),
                                        Color(0xFF101820),
                                    ),
                                ),
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.045f), cellShape),
                    )
                }
            }

            var ghosts by remember { mutableStateOf<List<Pair<Tile, Tile>>>(emptyList()) }
            LaunchedEffect(lastResult, animationsActive) {
                val res = lastResult
                if (res == null || previousTiles.isEmpty()) {
                    ghosts = emptyList()
                    return@LaunchedEffect
                }
                val targetById = res.merges.flatMap { m -> m.consumedIds.map { it to m.tile } }.toMap()
                ghosts = previousTiles.filter { targetById.containsKey(it.id) }.map { it to targetById.getValue(it.id) }
                if (animationsActive) delay(MOVE_MS.toLong() + 16L)
                ghosts = emptyList()
            }
            ghosts.forEach { (from, target) ->
                key(from.id, target.id) {
                    GhostTileView(from, target, ::cellOffset, cell, animationsActive)
                }
            }

            val spawnedId = lastResult?.spawned?.id
            val mergedIds = lastResult?.merges?.map { it.tile.id }?.toSet() ?: emptySet()
            val mergeCount = lastResult?.merges?.size ?: 0
            for (tile in state.tiles) {
                key(tile.id) {
                    val appear = when (tile.id) {
                        spawnedId -> Appear.SPAWN
                        in mergedIds -> Appear.MERGE
                        else -> Appear.NONE
                    }
                    TileView(
                        tile = tile,
                        target = cellOffset(tile.row, tile.col),
                        cell = cell,
                        appear = appear,
                        animationsActive = animationsActive,
                        mergeCount = mergeCount,
                        removable = removingMode && canRemove(tile),
                        onClick = { onTileClick(tile) },
                    )
                }
            }
        }
    }
}

private data class Dp2(val x: Dp, val y: Dp)
private enum class Appear { NONE, SPAWN, MERGE }

private fun Modifier.swipeDetector(onSwipe: (Move) -> Unit): Modifier =
    pointerInput(onSwipe) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val threshold = viewConfiguration.touchSlop
            var total = Offset.Zero
            var dispatched = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (!dispatched) {
                    total += change.positionChange()
                    if (abs(total.x) >= threshold || abs(total.y) >= threshold) {
                        dispatched = true
                        onSwipe(
                            if (abs(total.x) > abs(total.y)) {
                                if (total.x > 0) Move.RIGHT else Move.LEFT
                            } else {
                                if (total.y > 0) Move.DOWN else Move.UP
                            },
                        )
                        change.consume()
                    }
                } else {
                    change.consume()
                }
                if (!change.pressed) break
            }
        }
    }

@Composable
private fun TileView(
    tile: Tile,
    target: Dp2,
    cell: Dp,
    appear: Appear,
    animationsActive: Boolean,
    mergeCount: Int,
    removable: Boolean,
    onClick: () -> Unit,
) {
    val spec = tween<Dp>(if (animationsActive) MOVE_MS else 0, easing = LinearOutSlowInEasing)
    val x by animateDpAsState(target.x, spec, label = "x${tile.id}")
    val y by animateDpAsState(target.y, spec, label = "y${tile.id}")
    val scale = remember(tile.id) { Animatable(if (animationsActive && appear != Appear.NONE) 0f else 1f) }
    val mergePeak = mergePopScale(tile.level, mergeCount)
    LaunchedEffect(tile.id, appear, animationsActive, mergeCount) {
        when {
            !animationsActive -> scale.snapTo(1f)
            appear == Appear.NONE -> scale.snapTo(1f)
            appear == Appear.SPAWN -> {
                scale.snapTo(0f)
                delay(MOVE_MS.toLong())
                scale.animateTo(1f, tween(SPAWN_MS, easing = LinearOutSlowInEasing))
            }
            else -> {
                scale.snapTo(0f)
                delay(MOVE_MS.toLong())
                scale.snapTo(1f)
                scale.animateTo(
                    1f,
                    keyframes {
                        durationMillis = POP_MS
                        1f at 0
                        mergePeak at POP_MS / 2
                        1f at POP_MS
                    },
                )
            }
        }
    }
    val colors = tileColors(tile.level)
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToPx(), y.roundToPx()) }
            .size(cell)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .then(
                if (colors.glow) {
                    Modifier.shadow(8.dp, shape, ambientColor = BrassBright.copy(alpha = 0.45f), spotColor = TealGlow.copy(alpha = 0.42f))
                } else {
                    Modifier.shadow(2.dp, shape, ambientColor = Color.Black.copy(alpha = 0.22f), spotColor = Color.Black.copy(alpha = 0.30f))
                },
            )
            .clip(shape)
            .background(tileBevel(tile.level))
            .border(1.dp, if (colors.glow) BrassBright.copy(alpha = 0.92f) else BrassDark.copy(alpha = 0.54f), shape)
            .then(if (removable) Modifier.border(2.dp, TealGlow, shape).clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = "${Elements.name(tile.level)}, ${tile.value}" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = 6.dp.toPx()
            val bevelHighlight = Color.White.copy(alpha = if (tile.level <= 2) 0.18f else 0.11f)
            val bevelShadow = Color.Black.copy(alpha = 0.24f)

            if (tile.level >= 9) {
                drawCircle(
                    TealGlow.copy(alpha = if (colors.glow) 0.11f else 0.045f),
                    radius = size.minDimension * 0.31f,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
            }

            drawLine(bevelHighlight, Offset(inset, inset), Offset(size.width - inset, inset), 1.dp.toPx())
            drawLine(bevelShadow, Offset(inset, size.height - inset), Offset(size.width - inset, size.height - inset), 1.dp.toPx())
        }
        Text(
            tile.value.toString(),
            style = if (tile.value >= 1024) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            color = colors.content,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun GhostTileView(
    from: Tile,
    target: Tile,
    cellOffset: (Int, Int) -> Dp2,
    cell: Dp,
    animationsActive: Boolean,
) {
    var current by remember(from.id, target.id) { mutableStateOf(cellOffset(from.row, from.col)) }
    val x by animateDpAsState(current.x, tween(if (animationsActive) MOVE_MS else 0, easing = LinearOutSlowInEasing), label = "gx${from.id}")
    val y by animateDpAsState(current.y, tween(if (animationsActive) MOVE_MS else 0, easing = LinearOutSlowInEasing), label = "gy${from.id}")
    LaunchedEffect(from.id, target.id, animationsActive) {
        if (!animationsActive) {
            current = cellOffset(target.row, target.col)
            return@LaunchedEffect
        }
        withFrameNanos { }
        current = cellOffset(target.row, target.col)
    }
    val colors = tileColors(from.level)
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToPx(), y.roundToPx()) }
            .size(cell)
            .clip(shape)
            .background(tileBevel(from.level))
            .border(1.dp, BrassDark.copy(alpha = 0.50f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(from.value.toString(), style = MaterialTheme.typography.headlineSmall, color = colors.content)
    }
}

@Composable
private fun ToolButton(
    symbol: String,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (selected) listOf(TealSurface, Color(0xFF17373D)) else listOf(Panel.copy(alpha = 0.92f), Recess),
                ),
            )
            .border(1.dp, if (selected) TealGlow.copy(alpha = 0.82f) else BrassDark.copy(alpha = 0.62f), shape)
            .clickable(enabled = active, onClick = onClick)
            .padding(horizontal = 12.dp)
            .semantics { role = Role.Button; contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(symbol, color = if (active) BrassBright else TextMuted, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = if (active) TextWarm else TextMuted,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun GameOverOverlay(
    ui: GameUiState,
    rewardedAvailable: Boolean,
    onRewarded: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    val bestTile = if (ui.state.maxLevel > 0) (1 shl ui.state.maxLevel).toString() else "—"
    val gemsGained = ui.effects?.gemsGained ?: 0
    val xpGained = ui.effects?.xpGained ?: 0
    val newBest = ui.effects?.newBest == true

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.82f)).padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        SteamPanel(Modifier.fillMaxWidth().widthIn(max = 500.dp), highlighted = true) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "ПАРТИЯ ЗАВЕРШЕНА",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextWarm,
                    textAlign = TextAlign.Center,
                )
                if (newBest) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "НОВЫЙ РЕКОРД",
                        style = MaterialTheme.typography.labelLarge,
                        color = TealGlow,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    ui.state.score.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = if (newBest) TealGlow else BrassBright,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    "ИТОГОВЫЙ СЧЁТ",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ResultMetric("ЛУЧШАЯ ПЛИТКА", bestTile, BrassBright, Modifier.weight(1f))
                    ResultMetric("ХОДОВ", ui.state.moves.toString(), TextWarm, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ResultMetric("ГЕМЫ", "+$gemsGained", TealGlow, Modifier.weight(1f))
                    ResultMetric("ОПЫТ", "+$xpGained XP", TealGlow, Modifier.weight(1f))
                }

                if (ui.effects?.levelUps?.isNotEmpty() == true) {
                    Spacer(Modifier.height(9.dp))
                    ResultNotice("⚙", "Мастерская · уровень ${ui.effects.levelUps.last()}", BrassBright)
                }
                if (ui.effects?.newAchievements?.isNotEmpty() == true) {
                    Spacer(Modifier.height(6.dp))
                    ResultNotice(
                        "✦",
                        "Открыто: " + ui.effects.newAchievements.joinToString { it.title },
                        TealGlow,
                    )
                }

                Spacer(Modifier.height(14.dp))
                SteamButton(
                    "СЫГРАТЬ СНОВА",
                    onRestart,
                    Modifier.fillMaxWidth(),
                    style = SteamButtonStyle.Teal,
                )
                Spacer(Modifier.height(7.dp))
                SteamButton(
                    "В МАСТЕРСКУЮ",
                    onExit,
                    Modifier.fillMaxWidth(),
                    style = SteamButtonStyle.Dark,
                )

                if (ui.rewardDoubled) {
                    Spacer(Modifier.height(11.dp))
                    Text(
                        "◆ Награда за партию удвоена",
                        modifier = Modifier.fillMaxWidth(),
                        color = TealGlow,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                } else if (rewardedAvailable) {
                    Spacer(Modifier.height(11.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Recess.copy(alpha = 0.78f))
                            .border(1.dp, Color.White.copy(alpha = 0.055f), RoundedCornerShape(12.dp))
                            .padding(9.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Необязательно · ещё +$gemsGained гемов за видео",
                                modifier = Modifier.fillMaxWidth(),
                                color = TextMuted,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(6.dp))
                            SteamButton(
                                "УДВОИТЬ ГЕМЫ",
                                onRewarded,
                                Modifier.fillMaxWidth(),
                                style = SteamButtonStyle.Dark,
                                icon = "▶",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultMetric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(11.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Recess.copy(alpha = 0.76f))
            .border(1.dp, Color.White.copy(alpha = 0.055f), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = accent,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun ResultNotice(icon: String, text: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Panel.copy(alpha = 0.56f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, color = accent, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            modifier = Modifier.weight(1f),
            color = TextWarm,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CoreOnlineOverlay(onContinue: () -> Unit, onExit: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.82f)).padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        SteamPanel(Modifier.fillMaxWidth().widthIn(max = 500.dp), highlighted = true) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CORE ONLINE", style = MaterialTheme.typography.displaySmall, color = BrassBright, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(TealGlow.copy(alpha = 0.42f), TealSurface, Recess)))
                        .border(4.dp, Brass, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ВЫ СОЗДАЛИ", style = MaterialTheme.typography.labelMedium, color = TextWarm)
                        Text("2048", style = MaterialTheme.typography.displaySmall, color = BrassBright)
                        Text("CORE", style = MaterialTheme.typography.labelLarge, color = TealGlow)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("ПОЗДРАВЛЯЕМ!", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
                Text(
                    "Механическое ядро собрано. Мастерская ожила: давление стабильно, лампы горят, механизмы запущены.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                SteamPanel(Modifier.fillMaxWidth()) {
                    Text("ДОСТИЖЕНИЕ РАЗБЛОКИРОВАНО", modifier = Modifier.fillMaxWidth(), color = TealGlow, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                    Text("Механическое ядро · 2048", modifier = Modifier.fillMaxWidth(), color = TextWarm, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    SteamButton("ПРОДОЛЖИТЬ", onContinue, Modifier.weight(1f), style = SteamButtonStyle.Teal)
                    Spacer(Modifier.width(8.dp))
                    SteamButton("В МАСТЕРСКУЮ", onExit, Modifier.weight(1f), style = SteamButtonStyle.Brass)
                }
            }
        }
    }
}
