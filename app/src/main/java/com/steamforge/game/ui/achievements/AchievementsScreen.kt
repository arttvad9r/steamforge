package com.steamforge.game.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.theme.Background
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm

private val OutlineDim = Color(0xFF5A4632)

@Composable
fun AchievementsScreen(
    vm: AchievementsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items by vm.ui.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("Назад") }
            Spacer(Modifier.width(12.dp))
            Text("Достижения", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp),
        ) {
            items(items, key = { it.def.id }) { item ->
                AchievementRow(item)
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun AchievementRow(item: AchievementUi) {
    val unlocked = item.unlocked
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (unlocked) Color(0xFF37301F) else Panel)
            .padding(14.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${item.def.title}: ${if (unlocked) "разблокировано" else "заблокировано"}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (unlocked) BrassBright.copy(alpha = 0.25f) else Recess),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (unlocked) "\u2699" else "\u2715",
                color = if (unlocked) BrassBright else TextMuted,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.def.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (unlocked) BrassBright else TextWarm,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.def.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!unlocked && item.def.maxProgress > 1) {
                Spacer(Modifier.height(4.dp))
                ProgressLine(item.progress, item.def.maxProgress)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "+${item.def.gemReward} ",
                    color = Brass,
                    style = MaterialTheme.typography.labelLarge,
                )
                com.steamforge.game.ui.workshop.GemIcon()
            }
            if (item.unlockDate != null) {
                Text(
                    item.unlockDate,
                    color = TextMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun ProgressLine(progress: Int, max: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Recess),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.toFloat() / max)
                .height(6.dp)
                .background(Brass),
        )
    }
}
