package com.steamforge.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.steamforge.game.progression.DailyChallenge
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.BrassDark
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm

@Composable
internal fun DailyObjectiveStrip(
    daily: DailyChallenge,
    satisfied: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val accent = if (satisfied) TealGlow else BrassBright
    val shape = RoundedCornerShape(if (compact) 9.dp else 10.dp)
    val stateLabel = if (satisfied) "ГОТОВО" else "НАГРАДА"
    val stateDescription = if (satisfied) "Выполнено" else "В процессе"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Recess.copy(alpha = if (compact) 0.62f else 0.68f))
            .border(1.dp, accent.copy(alpha = if (satisfied) 0.36f else 0.20f), shape)
            .padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 5.dp else 6.dp,
            )
            .semantics {
                contentDescription =
                    "Испытание дня. ${dailyGoalText(daily)}. $stateDescription. Награда ${daily.rewardGems} гемов"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 7.dp else 8.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (satisfied) 0.92f else 0.78f)),
        )
        Spacer(Modifier.width(if (compact) 7.dp else 8.dp))

        Column(Modifier.weight(1f)) {
            Text(
                "ЦЕЛЬ ДНЯ",
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1,
            )
            if (!compact) Spacer(Modifier.height(1.dp))
            Text(
                dailyGoalText(daily),
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                color = TextWarm,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "◆ ${daily.rewardGems}",
                style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                color = if (satisfied) TealGlow else TextWarm,
                maxLines = 1,
            )
            Text(
                stateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = if (satisfied) TealGlow else TextMuted,
                maxLines = 1,
            )
        }
    }
}
