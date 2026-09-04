package com.steamforge.game.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.progression.PermanentProfileSnapshot
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.ui.components.BrassRoundButton
import com.steamforge.game.ui.components.SteamBackdrop

@Composable
fun ProfileScreen(
    vm: ProfileViewModel,
    onBack: () -> Unit,
    onAchievements: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val profile = ui.profile

    SteamBackdrop(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrassRoundButton("←", "Назад", onBack)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ПРОФИЛЬ", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
                    Text("История мастерской", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("УРОВЕНЬ", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(profile.level.toString(), style = MaterialTheme.typography.titleLarge, color = BrassBright)
                }
            }
            Spacer(Modifier.height(12.dp))

            ProfileHero(profile)
            Spacer(Modifier.height(10.dp))

            Text("СТАТИСТИКА", style = MaterialTheme.typography.labelLarge, color = BrassBright)
            Spacer(Modifier.height(7.dp))
            StatPair(
                leftLabel = "ПАРТИЙ",
                leftValue = grouped(profile.gamesPlayed.toLong()),
                rightLabel = "ВСЕГО ОЧКОВ",
                rightValue = grouped(profile.totalScore),
            )
            Spacer(Modifier.height(7.dp))
            StatPair(
                leftLabel = "РЕКОРД",
                leftValue = grouped(profile.bestScore.toLong()),
                rightLabel = "ЛУЧШАЯ ДЕТАЛЬ",
                rightValue = if (profile.highestTile > 0) grouped(profile.highestTile.toLong()) else "—",
            )
            Spacer(Modifier.height(7.dp))
            StatPair(
                leftLabel = "ОБЪЕДИНЕНИЙ",
                leftValue = grouped(profile.totalMerges.toLong()),
                rightLabel = "МАКС. КОМБО",
                rightValue = if (profile.largestCombo > 0) "×${profile.largestCombo}" else "—",
            )
            Spacer(Modifier.height(7.dp))
            ProfileStatCard(
                label = "ЛУЧШАЯ ЕЖЕДНЕВНАЯ СЕРИЯ",
                value = if (profile.highestDailyStreak > 0) "${profile.highestDailyStreak} дн." else "—",
                modifier = Modifier.fillMaxWidth(),
                accent = TealGlow,
            )
            Spacer(Modifier.height(12.dp))

            Text("ПОСТОЯННЫЙ ПРОГРЕСС", style = MaterialTheme.typography.labelLarge, color = BrassBright)
            Spacer(Modifier.height(7.dp))
            PermanentProgressCard(profile)
            Spacer(Modifier.height(8.dp))
            AchievementsEntry(profile.achievementsUnlocked, onAchievements)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ProfileHero(profile: PermanentProfileSnapshot) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Panel.copy(alpha = 0.58f))
            .border(1.dp, Brass.copy(alpha = 0.24f), shape)
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Профиль мастерской: уровень ${profile.level}, рекорд ${profile.bestScore}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(58.dp)
                .height(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Recess)
                .border(1.dp, TealGlow.copy(alpha = 0.32f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("⚙", style = MaterialTheme.typography.headlineSmall, color = BrassBright)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("МАСТЕР STEAMFORGE", style = MaterialTheme.typography.titleMedium, color = TextWarm)
            Text(
                "${profile.gamesPlayed} партий · ${profile.collectionsCompleted}/${profile.collectionsTotal} коллекций",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 2,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("РЕКОРД", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(grouped(profile.bestScore.toLong()), style = MaterialTheme.typography.titleLarge, color = BrassBright)
        }
    }
}

@Composable
private fun StatPair(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        ProfileStatCard(leftLabel, leftValue, Modifier.weight(1f))
        ProfileStatCard(rightLabel, rightValue, Modifier.weight(1f))
    }
}

@Composable
private fun ProfileStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = TextWarm,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(Panel.copy(alpha = 0.46f))
            .border(1.dp, Color.White.copy(alpha = 0.055f), shape)
            .padding(horizontal = 11.dp, vertical = 9.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 2)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = accent, maxLines = 1)
    }
}

@Composable
private fun PermanentProgressCard(profile: PermanentProfileSnapshot) {
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Panel.copy(alpha = 0.52f))
            .border(1.dp, TealGlow.copy(alpha = 0.16f), shape)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        ProgressRow(
            label = "КОЛЛЕКЦИИ",
            value = "${profile.collectionsCompleted}/${profile.collectionsTotal}",
            fraction = if (profile.collectionsTotal > 0) {
                profile.collectionsCompleted.toFloat() / profile.collectionsTotal
            } else 0f,
        )
        Spacer(Modifier.height(11.dp))
        ProgressRow(
            label = "МАСТЕРСКАЯ",
            value = "${profile.workshopStagesCompleted}/${profile.workshopStagesTotal} этапов",
            fraction = profile.workshopFraction,
        )
    }
}

@Composable
private fun ProgressRow(label: String, value: String, fraction: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = TextWarm)
        Text(value, style = MaterialTheme.typography.labelMedium, color = TealGlow, textAlign = TextAlign.End)
    }
    Spacer(Modifier.height(6.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Recess),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(TealGlow.copy(alpha = 0.72f)),
        )
    }
}

@Composable
private fun AchievementsEntry(unlocked: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(shape)
            .background(Panel.copy(alpha = 0.48f))
            .border(1.dp, Brass.copy(alpha = 0.18f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
            .semantics {
                role = Role.Button
                contentDescription = "Достижения. Открыто $unlocked"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("★", style = MaterialTheme.typography.titleLarge, color = BrassBright)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text("ДОСТИЖЕНИЯ", style = MaterialTheme.typography.titleSmall, color = TextWarm)
            Text("Открыто: $unlocked", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = BrassBright)
    }
}

private fun grouped(value: Long): String {
    val safe = value.coerceAtLeast(0L).toString()
    return safe.reversed().chunked(3).joinToString(" ").reversed()
}
