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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
internal fun WorkshopMetaDock(
    dailyDone: Boolean,
    dailyRewardAvailable: Boolean,
    dailyRewardDay: Int,
    dailyRewardGems: Int,
    dailyRewardWorkshopParts: Int,
    onDaily: () -> Unit,
    onClaimReward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Panel.copy(alpha = 0.40f))
            .border(1.dp, BrassDark.copy(alpha = 0.24f), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "СЕГОДНЯ В ЦЕХЕ",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "СМЕННЫЕ ЗАДАЧИ",
                style = MaterialTheme.typography.labelSmall,
                color = BrassDark,
            )
        }
        Spacer(Modifier.height(5.dp))

        WorkshopMetaDockRow(
            kind = WorkshopMetaKind.DAILY,
            title = "Испытание дня",
            subtitle = if (dailyDone) "Сегодня выполнено" else "Новая задача на сегодня",
            action = if (dailyDone) "ВЫПОЛНЕНО" else "ОТКРЫТЬ",
            accent = if (dailyDone) TealGlow else BrassBright,
            enabled = !dailyDone,
            onClick = onDaily,
        )

        Box(
            Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 4.dp)
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.055f)),
        )

        val rewardSubtitle = if (dailyRewardAvailable) {
            buildString {
                append("День ")
                append(dailyRewardDay)
                if (dailyRewardGems > 0) append(" · ◆ +$dailyRewardGems")
                if (dailyRewardWorkshopParts > 0) append(" · ⚙ +$dailyRewardWorkshopParts")
            }
        } else {
            "Награда сегодня уже получена"
        }
        WorkshopMetaDockRow(
            kind = WorkshopMetaKind.REWARD,
            title = "Ежедневная награда",
            subtitle = rewardSubtitle,
            action = if (dailyRewardAvailable) "ПОЛУЧИТЬ" else "ПОЛУЧЕНО",
            accent = if (dailyRewardAvailable) TealGlow else TextMuted,
            enabled = dailyRewardAvailable,
            onClick = onClaimReward,
        )
    }
}

private enum class WorkshopMetaKind { DAILY, REWARD }

@Composable
private fun WorkshopMetaDockRow(
    kind: WorkshopMetaKind,
    title: String,
    subtitle: String,
    action: String,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val rowShape = RoundedCornerShape(11.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(rowShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 3.dp)
            .semantics {
                role = Role.Button
                contentDescription = "$title. $subtitle. $action"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkshopMetaGlyph(kind = kind, accent = accent, active = enabled)
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = TextWarm,
                maxLines = 1,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(7.dp))
        Text(
            action,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) accent else TextMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun WorkshopMetaGlyph(
    kind: WorkshopMetaKind,
    accent: Color,
    active: Boolean,
) {
    Canvas(Modifier.size(38.dp)) {
        val c = center
        val r = size.minDimension * 0.42f
        val metal = if (active) accent else BrassDark
        drawCircle(Recess.copy(alpha = 0.70f), radius = r, center = c)
        drawCircle(
            metal.copy(alpha = if (active) 0.42f else 0.25f),
            radius = r,
            center = c,
            style = Stroke(1.5.dp.toPx()),
        )

        when (kind) {
            WorkshopMetaKind.DAILY -> {
                val half = r * 0.48f
                val gap = r * 0.12f
                val tile = r * 0.34f
                listOf(
                    Offset(c.x - half, c.y - half),
                    Offset(c.x + half, c.y - half),
                    Offset(c.x - half, c.y + half),
                    Offset(c.x + half, c.y + half),
                ).forEachIndexed { index, p ->
                    drawRoundRect(
                        color = if (index == 3 && active) BrassBright.copy(alpha = 0.82f) else Brass.copy(alpha = 0.48f),
                        topLeft = Offset(p.x - tile, p.y - tile),
                        size = androidx.compose.ui.geometry.Size(tile * 2f, tile * 2f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(gap, gap),
                    )
                }
            }

            WorkshopMetaKind.REWARD -> {
                val w = r * 1.16f
                val h = r * 0.82f
                val left = c.x - w / 2f
                val top = c.y - h * 0.24f
                drawRoundRect(
                    color = Copper.copy(alpha = 0.44f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(2.dp.toPx()),
                )
                drawLine(
                    Brass.copy(alpha = 0.72f),
                    Offset(c.x, top),
                    Offset(c.x, top + h),
                    2.dp.toPx(),
                    StrokeCap.Round,
                )
                drawLine(
                    metal.copy(alpha = if (active) 0.86f else 0.42f),
                    Offset(c.x - r * 0.28f, top - r * 0.20f),
                    Offset(c.x, top + r * 0.03f),
                    2.dp.toPx(),
                    StrokeCap.Round,
                )
                drawLine(
                    metal.copy(alpha = if (active) 0.86f else 0.42f),
                    Offset(c.x + r * 0.28f, top - r * 0.20f),
                    Offset(c.x, top + r * 0.03f),
                    2.dp.toPx(),
                    StrokeCap.Round,
                )
            }
        }
    }
}
