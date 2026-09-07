package com.steamforge.game.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.PanelRaised
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TealSurface
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm

@Composable
internal fun HomeNavigationDeck(
    visibility: HomeFeatureVisibility,
    workshopLevel: Int,
    dailyRewardStreak: Int,
    dailyDone: Boolean,
    onWorkshop: () -> Unit,
    onContracts: () -> Unit,
    onDaily: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visibility.showWorkshop && !visibility.showContracts && !visibility.showDaily) return

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(10.dp))

        if (visibility.showWorkshop) {
            HomeWorkshopBay(
                workshopLevel = workshopLevel,
                dailyRewardStreak = dailyRewardStreak,
                onClick = onWorkshop,
            )
        }

        if (visibility.showContracts || visibility.showDaily) {
            if (visibility.showWorkshop) Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                if (visibility.showContracts) {
                    HomeTaskTile(
                        icon = "≡",
                        title = "КОНТРАКТЫ",
                        status = "3 ЗАДАНИЯ",
                        detail = "НАГРАДЫ ЗА ИГРУ",
                        accent = TextWarm,
                        contentDescription = "Контракты. 3 задания сегодня. Награды за игру",
                        onClick = onContracts,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (visibility.showContracts && visibility.showDaily) Spacer(Modifier.width(8.dp))
                if (visibility.showDaily) {
                    HomeTaskTile(
                        icon = if (dailyDone) "✓" else "2048",
                        title = "ИСПЫТАНИЕ",
                        status = if (dailyDone) "ГОТОВО" else "ЦЕЛЬ ДНЯ",
                        detail = if (dailyDone) "ВЫПОЛНЕНО" else "НОВАЯ ЗАДАЧА",
                        accent = if (dailyDone) TealGlow else BrassBright,
                        contentDescription = if (dailyDone) {
                            "Испытание дня. Выполнено"
                        } else {
                            "Испытание дня. Новая задача на сегодня"
                        },
                        onClick = onDaily,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeWorkshopBay(
    workshopLevel: Int,
    dailyRewardStreak: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val subtitle = "Ядро · LV $workshopLevel · серия $dailyRewardStreak"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        TealSurface.copy(alpha = 0.34f),
                        PanelRaised.copy(alpha = 0.60f),
                        Panel.copy(alpha = 0.68f),
                    ),
                ),
            )
            .border(1.dp, TealGlow.copy(alpha = 0.26f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Мастерская. $subtitle"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Recess.copy(alpha = 0.64f))
                .border(1.dp, TealGlow.copy(alpha = 0.26f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("⚒", style = MaterialTheme.typography.titleLarge, color = BrassBright)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("МАСТЕРСКАЯ", style = MaterialTheme.typography.titleMedium, color = TextWarm, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Text("›", style = MaterialTheme.typography.headlineSmall, color = TealGlow.copy(alpha = 0.78f))
    }
}

@Composable
private fun HomeTaskTile(
    icon: String,
    title: String,
    status: String,
    detail: String,
    accent: androidx.compose.ui.graphics.Color,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(13.dp)
    val longIcon = icon.length > 2
    Column(
        modifier = modifier
            .heightIn(min = 92.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        PanelRaised.copy(alpha = 0.48f),
                        Panel.copy(alpha = 0.62f),
                    ),
                ),
            )
            .border(1.dp, accent.copy(alpha = 0.18f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Recess.copy(alpha = 0.52f))
                    .border(1.dp, accent.copy(alpha = 0.20f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    icon,
                    style = if (longIcon) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
                    color = accent,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Spacer(Modifier.weight(1f))
            Text("›", style = MaterialTheme.typography.titleLarge, color = accent.copy(alpha = 0.66f))
        }
        Spacer(Modifier.height(7.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, color = TextWarm, maxLines = 1)
        Text(status, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1)
        Text(detail, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
    }
}
