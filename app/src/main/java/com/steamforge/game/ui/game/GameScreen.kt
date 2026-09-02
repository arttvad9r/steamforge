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
            SteamLogoHeader(
                compact = true,
                leading = { BrassRoundButton("←", "В мастерскую", ::leave) },
            )
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth()) {
                StatPlate("СЧЁТ", ui.state.score.toString(), Modifier.weight(1f), BrassBright)
                Spacer(Modifier.width(8.dp))
                StatPlate("ЛУЧШИЙ", ui.best.toString(), Modifier.weight(1f), TextWarm)
                Spacer(Modifier.width(8.dp))
                StatPlate("ГЕМЫ", ui.gems.toString(), Modifier.weight(0.78f), TealGlow)
            }
            Spacer(Modifier.height(7.dp))

            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                highlighted = ui.overdriveRemaining > 0,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HudMetric(
                        label = "ХОДЫ",
                        value = ui.state.moves.toString(),
                        accent = BrassBright,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    PressureGauge(
                        pressure = ui.pressure,
                        overdriveRemaining = ui.overdriveRemaining,
                        animationsActive = ui.animationsActive,
                        modifier = Modifier.size(112.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    HudMetric(
                        label = "ОТМЕНА",
                        value = if (ui.freeUndosLeft > 0) "${ui.freeUndosLeft} бесплатно" else "◆ 5",
                        accent = if (ui.freeUndosLeft > 0) TealGlow else BrassBright,
                        modifier = Modifier.weight(1f),
                    )
                }
                val daily = ui.daily
                if (daily != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        dailyGoalText(daily) + if (ui.dailySatisfied) " — выполнено" else "",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (ui.dailySatisfied) TealGlow else TextMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(7.dp))

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

            SteamPanel(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ToolButton(
                        symbol = "↶",
                        label = if (ui.freeUndosLeft > 0) "ОТМЕНА ${ui.freeUndosLeft}" else "ОТМЕНА ◆5",
                        active = ui.canUndo && !ui.finished,
                        onClick = {
                            val beforeUndo = vm.ui.value.state
                            vm.undo()
                            if (vm.ui.value.state != beforeUndo) {
                                sfx.play(Sfx.UNDO)
                                performGameplayHaptic(HapticFeedbackConstantsCompat.CONFIRM)
                            }
                        },
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
            Spacer(Modifier.height(7.dp))
            Text(
                "Свайпните по полю · одинаковые детали объединяются",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
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
        Spacer(Modifier.height(2.dp))
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
                .shadow(14.dp, shape)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF2A1B10), Recess)))
                .border(3.dp, Brass, shape)
                .padding(2.dp)
                .border(1.dp, BrassBright.copy(alpha = 0.28f), RoundedCornerShape(15.dp))
                .swipeDetector(onSwipe),
        ) {
            for (r in 0 until state.size) {
                for (c in 0 until state.size) {
                    val off = cellOffset(r, c)
                    Box(
                        modifier = Modifier
                            .offset(off.x, off.y)
                            .size(cell)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Recess.copy(alpha = 0.94f),
                                        Panel.copy(alpha = 0.58f),
                                    ),
                                ),
                            )
                            .border(1.dp, BrassDark.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
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
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToPx(), y.roundToPx()) }
            .size(cell)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .then(
                if (colors.glow) Modifier.shadow(12.dp, shape, ambientColor = BrassBright, spotColor = TealGlow)
                else Modifier.shadow(3.dp, shape),
            )
            .clip(shape)
            .background(tileBevel(tile.level))
            .border(if (colors.glow) 2.dp else 1.dp, if (colors.glow) BrassBright else BrassDark, shape)
            .then(if (removable) Modifier.border(3.dp, TealGlow, shape).clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = "${Elements.name(tile.level)}, ${tile.value}" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = 6.dp.toPx()
            val bevelHighlight = Color.White.copy(alpha = if (tile.level <= 2) 0.24f else 0.14f)
            val bevelShadow = Color.Black.copy(alpha = 0.34f)

            if (tile.level >= 8) {
                drawCircle(
                    TealGlow.copy(alpha = if (colors.glow) 0.16f else 0.07f),
                    radius = size.minDimension * 0.34f,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
            }

            drawLine(bevelHighlight, Offset(inset, inset), Offset(size.width - inset, inset), 1.dp.toPx())
            drawLine(bevelHighlight, Offset(inset, inset), Offset(inset, size.height - inset), 1.dp.toPx())
            drawLine(bevelShadow, Offset(inset, size.height - inset), Offset(size.width - inset, size.height - inset), 1.5.dp.toPx())
            drawLine(bevelShadow, Offset(size.width - inset, inset), Offset(size.width - inset, size.height - inset), 1.5.dp.toPx())

            val r = 2.dp.toPx()
            val p = 7.dp.toPx()
            val rivet = if (tile.level >= 7) BrassBright.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.28f)
            val centers = listOf(
                Offset(p, p),
                Offset(size.width - p, p),
                Offset(p, size.height - p),
                Offset(size.width - p, size.height - p),
            )
            centers.forEach { center ->
                drawCircle(Color.Black.copy(alpha = 0.38f), r * 1.35f, center + Offset(0.6.dp.toPx(), 0.7.dp.toPx()))
                drawCircle(rivet, r, center)
                drawCircle(Color.White.copy(alpha = 0.18f), r * 0.38f, center - Offset(0.45.dp.toPx(), 0.45.dp.toPx()))
            }
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
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToPx(), y.roundToPx()) }
            .size(cell)
            .clip(shape)
            .background(tileBevel(from.level))
            .border(1.dp, BrassDark.copy(alpha = 0.65f), shape),
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
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (selected) listOf(TealSurface, Color(0xFF102B29)) else listOf(Color(0xFF4A311A), Recess),
                ),
            )
            .border(2.dp, if (selected) TealGlow else Brass, shape)
            .clickable(enabled = active, onClick = onClick)
            .padding(horizontal = 10.dp)
            .semantics { role = Role.Button; contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selected) TealSurface else Panel)
                .border(1.dp, if (selected) TealGlow else BrassDark, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, color = if (active) BrassBright else TextMuted, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f), color = if (active) TextWarm else TextMuted, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
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
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.80f)).padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        SteamPanel(Modifier.fillMaxWidth().widthIn(max = 500.dp), highlighted = true) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ИГРА ОКОНЧЕНА", style = MaterialTheme.typography.displaySmall, color = BrassBright, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                ResultRow("★", "ИТОГОВЫЙ СЧЁТ", ui.state.score.toString(), if (ui.effects?.newBest == true) TealGlow else TextWarm)
                ResultRow("▣", "ЛУЧШАЯ ПЛИТКА", if (ui.state.maxLevel > 0) (1 shl ui.state.maxLevel).toString() else "—", TextWarm)
                ResultRow("↔", "ВСЕГО ХОДОВ", ui.state.moves.toString(), TextWarm)
                ResultRow("◆", "ПОЛУЧЕНО ГЕМОВ", "+${ui.effects?.gemsGained ?: 0}", TealGlow)
                ResultRow("XP", "ПОЛУЧЕНО ОПЫТА", "+${ui.effects?.xpGained ?: 0}", TealGlow)

                if (ui.effects?.levelUps?.isNotEmpty() == true) {
                    Spacer(Modifier.height(6.dp))
                    Text("Мастерская: уровень ${ui.effects.levelUps.last()}!", color = BrassBright, style = MaterialTheme.typography.bodyLarge)
                }
                if (ui.effects?.newAchievements?.isNotEmpty() == true) {
                    Text(
                        "Открыто: " + ui.effects.newAchievements.joinToString { it.title },
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(12.dp))

                if (ui.rewardDoubled) {
                    SteamPanel(Modifier.fillMaxWidth(), highlighted = true) {
                        Text("◆ ГЕМЫ УДВОЕНЫ", modifier = Modifier.fillMaxWidth(), color = TealGlow, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    }
                } else if (rewardedAvailable) {
                    SteamPanel(Modifier.fillMaxWidth(), highlighted = true) {
                        Text("УДВОИТЬ ГЕМЫ?", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium, color = TextWarm, textAlign = TextAlign.Center)
                        Text("Видео необязательно · награда +${ui.effects?.gemsGained ?: 0} гемов", modifier = Modifier.fillMaxWidth(), color = TextMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        SteamButton("СМОТРЕТЬ И УДВОИТЬ", onRewarded, Modifier.fillMaxWidth(), style = SteamButtonStyle.Teal, icon = "▶")
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    SteamButton("СЫГРАТЬ СНОВА", onRestart, Modifier.weight(1f), style = SteamButtonStyle.Teal)
                    Spacer(Modifier.width(8.dp))
                    SteamButton("В МАСТЕРСКУЮ", onExit, Modifier.weight(1f), style = SteamButtonStyle.Brass)
                }
            }
        }
    }
}

@Composable
private fun ResultRow(icon: String, label: String, value: String, valueColor: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(TealSurface.copy(alpha = 0.55f)).border(1.dp, Brass, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = BrassBright, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(9.dp))
        Text(label, modifier = Modifier.weight(1f), color = TextMuted, style = MaterialTheme.typography.labelMedium)
        Text(value, color = valueColor, style = MaterialTheme.typography.titleLarge)
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