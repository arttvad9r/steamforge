package com.steamforge.game.ui.workshop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.steamforge.game.progression.WorkshopMechanism
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.BrassDark
import com.steamforge.game.theme.Copper
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm

@Composable
internal fun WorkshopUpgradeDeck(
    mechanisms: List<WorkshopMechanismUi>,
    onUpgrade: (WorkshopMechanism) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "УЗЛЫ ЦЕХА",
                style = MaterialTheme.typography.labelLarge,
                color = TextWarm,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "ДЕТАЛИ → МОДЕРНИЗАЦИЯ",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(6.dp))

        mechanisms.forEachIndexed { index, mechanism ->
            WorkshopMechanismBay(
                mechanism = mechanism,
                onUpgrade = { onUpgrade(mechanism.mechanism) },
            )
            if (index != mechanisms.lastIndex) Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun WorkshopMechanismBay(
    mechanism: WorkshopMechanismUi,
    onUpgrade: () -> Unit,
) {
    val maxed = mechanism.nextCost == null
    val enabled = !maxed && mechanism.canUpgrade
    val accent = when {
        maxed -> TealGlow
        enabled -> BrassBright
        else -> BrassDark
    }
    val actionTitle = when {
        maxed -> "ГОТОВО"
        enabled -> "УЛУЧШИТЬ"
        else -> "ТРЕБУЕТСЯ"
    }
    val actionValue = mechanism.nextCost?.let { "⚙ $it" } ?: "MAX"
    val semanticAction = when {
        maxed -> "Узел полностью улучшен"
        enabled -> "Улучшить за ${mechanism.nextCost} деталей"
        else -> "Нужно ${mechanism.nextCost} деталей"
    }
    val shape = RoundedCornerShape(15.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Panel.copy(alpha = 0.68f),
                        Recess.copy(alpha = 0.80f),
                    ),
                ),
            )
            .border(
                1.dp,
                accent.copy(alpha = if (enabled || maxed) 0.36f else 0.18f),
                shape,
            )
            .clickable(enabled = enabled, onClick = onUpgrade)
            .padding(horizontal = 10.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${mechanism.mechanism.title}: ${mechanism.stageLabel}. $semanticAction"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MechanismGlyph(
            mechanism = mechanism.mechanism,
            stage = mechanism.stage,
            accent = if (maxed) TealGlow else if (enabled) BrassBright else Brass,
            modifier = Modifier.size(46.dp),
        )
        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(
                mechanism.mechanism.shortTitle,
                style = MaterialTheme.typography.titleSmall,
                color = TextWarm,
                maxLines = 1,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    mechanism.stageLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (mechanism.stage >= 3) TealGlow else TextMuted,
                    maxLines = 1,
                )
                Spacer(Modifier.width(8.dp))
                MechanismStageBars(mechanism.stage)
            }
        }

        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                actionTitle,
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    maxed -> TealGlow
                    enabled -> BrassBright
                    else -> TextMuted
                },
                maxLines = 1,
            )
            Text(
                actionValue,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) TextWarm else TextMuted,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MechanismStageBars(stage: Int) {
    val normalized = stage.coerceIn(0, 4)
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(4) { index ->
            val active = index < normalized
            Box(
                modifier = Modifier
                    .width(11.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (active) {
                            if (normalized >= 3) TealGlow.copy(alpha = 0.78f) else Brass.copy(alpha = 0.74f)
                        } else {
                            Color.White.copy(alpha = 0.07f)
                        },
                    ),
            )
            if (index != 3) Spacer(Modifier.width(3.dp))
        }
    }
}

@Composable
private fun MechanismGlyph(
    mechanism: WorkshopMechanism,
    stage: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val normalized = stage.coerceIn(0, 4)
    Canvas(modifier) {
        val c = center
        val r = size.minDimension * 0.39f
        val metal = accent.copy(alpha = 0.42f + normalized * 0.10f)
        val dim = BrassDark.copy(alpha = 0.44f)

        drawCircle(Recess.copy(alpha = 0.72f), radius = r * 1.18f, center = c)
        drawCircle(
            color = if (normalized >= 3) TealGlow.copy(alpha = 0.20f) else dim,
            radius = r * 1.08f,
            center = c,
            style = Stroke(1.5.dp.toPx()),
        )

        when (mechanism) {
            WorkshopMechanism.CORE -> {
                drawCircle(metal, r * 0.76f, c, style = Stroke(3.dp.toPx()))
                drawCircle(if (normalized >= 3) TealGlow.copy(alpha = 0.34f) else Copper.copy(alpha = 0.42f), r * 0.30f, c)
                repeat(8) { index ->
                    rotate(index * 45f, pivot = c) {
                        drawLine(
                            metal,
                            Offset(c.x, c.y - r * 0.78f),
                            Offset(c.x, c.y - r * 1.02f),
                            2.5.dp.toPx(),
                            StrokeCap.Round,
                        )
                    }
                }
            }

            WorkshopMechanism.PRESSURE_GENERATOR -> {
                drawCircle(metal, r * 0.56f, Offset(c.x, c.y - r * 0.10f), style = Stroke(3.dp.toPx()))
                drawLine(
                    Copper.copy(alpha = 0.58f),
                    Offset(c.x - r * 0.76f, c.y + r * 0.70f),
                    Offset(c.x + r * 0.76f, c.y + r * 0.70f),
                    3.dp.toPx(),
                    StrokeCap.Round,
                )
                drawLine(
                    metal,
                    Offset(c.x, c.y + r * 0.42f),
                    Offset(c.x, c.y + r * 0.70f),
                    3.dp.toPx(),
                    StrokeCap.Round,
                )
                drawLine(
                    if (normalized >= 3) TealGlow else BrassBright.copy(alpha = 0.62f),
                    Offset(c.x, c.y - r * 0.10f),
                    Offset(c.x + r * 0.28f, c.y - r * 0.34f),
                    2.dp.toPx(),
                    StrokeCap.Round,
                )
            }

            WorkshopMechanism.GEAR_PRESS -> {
                drawLine(
                    metal,
                    Offset(c.x - r * 0.62f, c.y - r * 0.78f),
                    Offset(c.x - r * 0.62f, c.y + r * 0.78f),
                    3.dp.toPx(),
                    StrokeCap.Round,
                )
                drawLine(
                    metal,
                    Offset(c.x + r * 0.62f, c.y - r * 0.78f),
                    Offset(c.x + r * 0.62f, c.y + r * 0.78f),
                    3.dp.toPx(),
                    StrokeCap.Round,
                )
                drawLine(
                    metal,
                    Offset(c.x - r * 0.62f, c.y - r * 0.78f),
                    Offset(c.x + r * 0.62f, c.y - r * 0.78f),
                    3.dp.toPx(),
                    StrokeCap.Round,
                )
                drawCircle(Copper.copy(alpha = 0.66f), r * 0.42f, c, style = Stroke(3.dp.toPx()))
                repeat(6) { index ->
                    rotate(index * 60f, pivot = c) {
                        drawLine(
                            Copper.copy(alpha = 0.66f),
                            Offset(c.x, c.y - r * 0.34f),
                            Offset(c.x, c.y - r * 0.55f),
                            2.dp.toPx(),
                            StrokeCap.Round,
                        )
                    }
                }
            }
        }
    }
}
