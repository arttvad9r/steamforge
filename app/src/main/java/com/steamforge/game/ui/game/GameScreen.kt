package com.steamforge.game.ui.game

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import com.steamforge.game.theme.Background
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.Copper
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.theme.tileBevel
import com.steamforge.game.theme.tileColors
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val MOVE_MS = 110
private const val POP_MS = 140
private const val SPAWN_MS = 130

private val OutlineDim = Color(0xFF5A4632)

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
    val haptics = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var prevOverdrive by remember { mutableIntStateOf(0) }
    var prevFinished by remember { mutableStateOf(false) }
    var prevWon by remember { mutableStateOf(false) }

    LaunchedEffect(ui.lastResult) {
        val res = ui.lastResult ?: return@LaunchedEffect
        if (res.merges.isNotEmpty()) {
            val maxLevel = res.merges.maxOf { it.tile.level }
            sfx.play(
                when {
                    maxLevel >= 8 -> Sfx.MERGE_HIGH
                    maxLevel >= 4 -> Sfx.MERGE_MID
                    else -> Sfx.MERGE_LOW
                },
            )
            if (ui.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            sfx.play(Sfx.MOVE)
        }
    }
    LaunchedEffect(ui.overdriveRemaining) {
        if (ui.overdriveRemaining > 0 && prevOverdrive == 0) {
            sfx.play(Sfx.OVERDRIVE)
            if (ui.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        prevOverdrive = ui.overdriveRemaining
    }
    LaunchedEffect(ui.finished) {
        if (ui.finished && !prevFinished) {
            sfx.play(if (ui.effects?.levelUps?.isNotEmpty() == true) Sfx.LEVEL_UP else Sfx.GAME_OVER)
            if (ui.hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        prevFinished = ui.finished
    }
    LaunchedEffect(ui.winCelebrated) {
        if (ui.winCelebrated && !prevWon) sfx.play(Sfx.WIN)
        prevWon = ui.winCelebrated
    }

    fun leave() {
        if (!exitHandled) {
            exitHandled = true
            vm.exit()
            onExit()
            if (ui.finished && !ui.rewardDoubled && context is Activity) {
                ads.maybeShowInterstitial(context)
            }
        }
    }

    Scaffold(
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
        containerColor = Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(4.dp))
            GameTopBar(
                score = ui.state.score,
                best = ui.best,
                gems = ui.gems,
                daily = ui.daily,
                dailySatisfied = ui.dailySatisfied,
                onExit = { leave() },
            )
            Spacer(Modifier.height(10.dp))
            PressureGauge(
                pressure = ui.pressure,
                overdriveRemaining = ui.overdriveRemaining,
                animationsActive = ui.animationsActive,
            )
            Spacer(Modifier.height(10.dp))
            BoardView(
                state = ui.state,
                lastResult = ui.lastResult,
                previousTiles = ui.previousTiles,
                animationsActive = ui.animationsActive,
                removingMode = ui.removingMode,
                canRemove = vm::canRemoveTile,
                onTileClick = vm::removeTile,
                onSwipe = vm::onMove,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = vm::undo,
                    enabled = ui.canUndo && !ui.finished && (ui.freeUndosLeft > 0 || ui.gems >= UNDO_COST_PLACEHOLDER),
                ) {
                    Text(if (ui.freeUndosLeft > 0) "Отменить (${ui.freeUndosLeft})" else "Отменить · 5 гемов")
                }
                OutlinedButton(
                    onClick = vm::toggleRemovingMode,
                    enabled = !ui.finished,
                ) {
                    Text(if (ui.removingMode) "Ключ: выбери плитку" else "Ключ · 10 гемов")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Свайпы или стрелки · одинаковые детали объединяются",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (ui.finished) {
        val activity = context as? Activity
        GameOverOverlay(
            ui = ui,
            rewardedAvailable = rewardedReady && (ui.effects?.gemsGained ?: 0) > 0 && !ui.rewardDoubled,
            onRewarded = {
                if (activity != null) {
                    ads.showRewarded(activity) { vm.grantDoubleReward() }
                }
            },
            onRestart = {
                if (activity != null && !ui.rewardDoubled) ads.maybeShowInterstitial(activity)
                vm.restart()
            },
            onExit = { leave() },
        )
    } else if (ui.winCelebrated && !ui.winBannerShown) {
        OverlayPanel {
            Text(
                "CORE ONLINE",
                style = MaterialTheme.typography.displaySmall,
                color = BrassBright,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Механическое ядро собрано — лампы горят, пар шумит. Мастерская ожила!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Button(onClick = vm::markWinBannerShown) { Text("Продолжить смену") }
        }
    }
}

private const val UNDO_COST_PLACEHOLDER = 5

@Composable
private fun GameTopBar(
    score: Int,
    best: Int,
    gems: Int,
    daily: DailyChallenge?,
    dailySatisfied: Boolean,
    onExit: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onExit) { Text("Выход") }
            Spacer(Modifier.width(12.dp))
            Text(
                if (daily != null) "Испытание дня" else "Смена",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            ScoreChip("Гемы", gems, icon = true)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScoreChip("Счёт", score)
            Spacer(Modifier.width(6.dp))
            ScoreChip("Рекорд", best)
            if (daily != null) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = dailyGoalText(daily) + if (dailySatisfied) " — выполнено!" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (dailySatisfied) BrassBright else TextMuted,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

fun dailyGoalText(daily: DailyChallenge): String = when (daily.type) {
    DailyGoalType.REACH_TILE -> "Собери деталь ${daily.target}"
    DailyGoalType.REACH_SCORE -> "Набери ${daily.target} очков"
    DailyGoalType.HIGH_MERGES -> "Сделай ${daily.target} объединения 64+"
}

@Composable
private fun ScoreChip(label: String, value: Int, icon: Boolean = false) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Panel)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics { contentDescription = "$label: $value" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon) {
            com.steamforge.game.ui.workshop.GemIcon()
        } else {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = if (label == "\u2726") BrassBright else TextWarm,
        )
    }
}

@Composable
fun PressureGauge(
    pressure: Int,
    overdriveRemaining: Int,
    animationsActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val overdrive = overdriveRemaining > 0
    val glow = if (animationsActive && overdrive) {
        val pulse = rememberInfiniteTransition(label = "overdrive")
        val g by pulse.animateFloat(
            initialValue = 0.92f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
            label = "glow",
        )
        g
    } else {
        1f
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = if (overdrive) "Overdrive активно" else "Давление пара: $pressure процентов"
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (overdrive) "OVERDRIVE \u00d72" else "ДАВЛЕНИЕ ПАРА",
                style = MaterialTheme.typography.labelLarge,
                color = if (overdrive) BrassBright else TextMuted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (overdrive) "осталось объединений: $overdriveRemaining" else "$pressure%",
                style = MaterialTheme.typography.labelMedium,
                color = if (overdrive) BrassBright else TextMuted,
            )
        }
        Spacer(Modifier.height(4.dp))
        val shape = RoundedCornerShape(8.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(shape)
                .background(Recess)
                .border(1.dp, OutlineDim, shape),
        ) {
            val fraction = if (overdrive) 1f else pressure / 100f
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(14.dp)
                    .graphicsLayer {
                        scaleX = glow
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                    .clip(shape)
                    .background(
                        if (overdrive) Brush.horizontalGradient(listOf(Copper, BrassBright))
                        else Brush.horizontalGradient(listOf(Copper, Brass)),
                    ),
            )
        }
    }
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
        val gap = board * 0.022f
        val cell = (board - gap * 5f) / 4f

        fun cellOffset(row: Int, col: Int): Dp2 = Dp2(gap + (cell + gap) * col, gap + (cell + gap) * row)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Recess)
                .border(1.5.dp, OutlineDim, shape)
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
                            .background(Panel.copy(alpha = 0.55f)),
                    )
                }
            }

            var ghosts by remember { mutableStateOf<List<Pair<Tile, Tile>>>(emptyList()) }
            LaunchedEffect(lastResult) {
                val res = lastResult
                if (res == null || previousTiles.isEmpty()) {
                    ghosts = emptyList()
                    return@LaunchedEffect
                }
                val targetById = res.merges.flatMap { m -> m.consumedIds.map { it to m.tile } }.toMap()
                ghosts = previousTiles
                    .filter { targetById.containsKey(it.id) }
                    .map { it to targetById.getValue(it.id) }
                if (animationsActive) delay(MOVE_MS.toLong() + 30L)
                ghosts = emptyList()
            }
            ghosts.forEach { (from, target) ->
                GhostTileView(from, target, ::cellOffset, cell, animationsActive)
            }

            val spawnedId = lastResult?.spawned?.id
            val mergedIds = lastResult?.merges?.map { it.tile.id }?.toSet() ?: emptySet()
            for (tile in state.tiles) {
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
                    removable = removingMode && canRemove(tile),
                    onClick = { onTileClick(tile) },
                )
            }
        }
    }
}

private data class Dp2(val x: Dp, val y: Dp)
private enum class Appear { NONE, SPAWN, MERGE }

private fun Modifier.swipeDetector(onSwipe: (Move) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val threshold = 24.dp.toPx()
            var total = Offset.Zero
            var pressed = true
            while (pressed) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                if (!change.isConsumed) total += change.positionChange()
                change.consume()
                if (!change.pressed) pressed = false
            }
            if (abs(total.x) < threshold && abs(total.y) < threshold) return@awaitEachGesture
            onSwipe(
                if (abs(total.x) > abs(total.y)) {
                    if (total.x > 0) Move.RIGHT else Move.LEFT
                } else {
                    if (total.y > 0) Move.DOWN else Move.UP
                },
            )
        }
    }

@Composable
private fun TileView(
    tile: Tile,
    target: Dp2,
    cell: Dp,
    appear: Appear,
    animationsActive: Boolean,
    removable: Boolean,
    onClick: () -> Unit,
) {
    val spec = tween<Dp>(if (animationsActive) MOVE_MS else 0, easing = LinearOutSlowInEasing)
    val x by animateDpAsState(target.x, spec, label = "x${tile.id}")
    val y by animateDpAsState(target.y, spec, label = "y${tile.id}")
    val scale = remember(tile.id) { Animatable(if (appear == Appear.NONE) 1f else 0.35f) }
    LaunchedEffect(tile.id, appear) {
        when {
            appear == Appear.NONE -> if (scale.value < 1f) scale.snapTo(1f)
            !animationsActive -> scale.snapTo(1f)
            appear == Appear.SPAWN -> scale.animateTo(1f, tween(SPAWN_MS, easing = LinearOutSlowInEasing))
            else -> scale.animateTo(
                1f,
                keyframes {
                    durationMillis = POP_MS
                    1.18f at POP_MS / 2
                },
            )
        }
    }
    val colors = tileColors(tile.level)
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToPx(), y.roundToPx()) }
            .size(cell)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .then(
                if (colors.glow) {
                    Modifier.shadow(10.dp, shape, ambientColor = BrassBright, spotColor = BrassBright)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(tileBevel(tile.level))
            .then(if (removable) Modifier.border(2.dp, BrassBright, shape).clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = "${Elements.name(tile.level)}, ${tile.value}" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tile.value.toString(),
            style = if (tile.value >= 1024) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            color = colors.content,
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
    var current by remember { mutableStateOf(cellOffset(from.row, from.col)) }
    val x by animateDpAsState(current.x, tween(MOVE_MS, easing = LinearOutSlowInEasing), label = "gx")
    val y by animateDpAsState(current.y, tween(MOVE_MS, easing = LinearOutSlowInEasing), label = "gy")
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (!animationsActive) return@LaunchedEffect
        withFrameNanos { }
        current = cellOffset(target.row, target.col)
        alpha.animateTo(0.1f, tween(MOVE_MS))
    }
    val colors = tileColors(from.level)
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToPx(), y.roundToPx()) }
            .size(cell)
            .graphicsLayer { this.alpha = alpha.value }
            .clip(RoundedCornerShape(10.dp))
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = from.value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.content,
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
    OverlayPanel {
        Text(
            if (ui.state.won) "ЯДРО В СБОРЕ!" else "СМЕНА ОКОНЧЕНА",
            style = MaterialTheme.typography.displaySmall,
            color = BrassBright,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Счёт: ${ui.state.score}" + if (ui.effects?.newBest == true) "  ·  НОВЫЙ РЕКОРД!" else "",
            style = MaterialTheme.typography.titleMedium,
        )
        val eff = ui.effects
        if (eff != null) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "+${eff.xpGained} XP  ·  +${eff.gemsGained} ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Brass,
                )
                com.steamforge.game.ui.workshop.GemIcon()
            }
            if (eff.levelUps.isNotEmpty()) {
                Text("Мастерская: уровень ${eff.levelUps.last()}!", color = BrassBright)
            }
            if (eff.newAchievements.isNotEmpty()) {
                Text(
                    "Достижения: " + eff.newAchievements.joinToString { it.title },
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (ui.rewardDoubled) {
            Text(
                "Гемы удвоены: +${ui.effects?.gemsGained ?: 0}",
                color = BrassBright,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(8.dp))
        } else if (rewardedAvailable) {
            Button(
                onClick = onRewarded,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = com.steamforge.game.theme.Patina,
                    contentColor = com.steamforge.game.theme.Background,
                ),
            ) {
                Text("Удвоить гемы (+${ui.effects?.gemsGained ?: 0}) за рекламу")
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onRestart) { Text("Заново") }
            OutlinedButton(onClick = onExit) { Text("В мастерскую") }
        }
    }
}

@Composable
private fun OverlayPanel(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Panel)
                .border(1.5.dp, OutlineDim, RoundedCornerShape(18.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}
