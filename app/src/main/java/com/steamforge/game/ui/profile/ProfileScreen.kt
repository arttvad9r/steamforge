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
    ProfileContent(
        profile = ui.profile,
        onBack = onBack,
        onAchievements = onAchievements,
        modifier = modifier,
    )
}

@Composable
fun ProfileContent(
    profile: PermanentProfileSnapshot,
    onBack: () -> Unit,
    onAchievements: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Spacer(Modifier.height(13.dp))

            Text("ИСТОРИЯ ПАРТИЙ", style = MaterialTheme.typography.labelLarge, color = BrassBright)
            Spacer(Modifier.height(7.dp))
            ProfileLedger(profile)
            Spacer(Modifier.height(13.dp))

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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Panel.copy(alpha = 0.58f))
            .border(1.dp, Brass.copy(alpha = 0.24f), shape)
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Профиль мастерской: уровень ${profile.level}, рекорд ${profile.bestScore}, " +
                    "лучшая деталь ${profile.highestTile}"
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    "Уровень ${profile.level} · ${grouped(profile.gamesPlayed.toLong())} партий",
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
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.045f)))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ЛУЧШАЯ ДЕТАЛЬ", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Text(
                if (profile.highestTile > 0) grouped(profile.highestTile.toLong()) else "—",
                style = MaterialTheme.typography.titleMedium,
                color = TealGlow,
            )
        }
    }
}

@Composable
private fun ProfileLedger(profile: PermanentProfileSnapshot) {
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Panel.copy(alpha = 0.46f))
            .border(1.dp, Color.White.copy(alpha = 0.055f), shape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        LedgerPair(
            leftLabel = "ПАРТИЙ",
            leftValue = grouped(profile.gamesPlayed.toLong()),
            rightLabel = "ВСЕГО ОЧКОВ",
            rightValue = grouped(profile.totalScore),
        )
        LedgerDivider()
        LedgerPair(
            leftLabel = "ОБЪЕДИНЕНИЙ",
            leftValue = grouped(profile.totalMerges.toLong()),
            rightLabel = "МАКС. КОМБО",
            rightValue = if (profile.largestCombo > 0) "×${profile.largestCombo}" else "—",
        )
        LedgerDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(vertical = 8.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "ЛУЧШАЯ ЕЖЕДНЕВНАЯ СЕРИЯ: ${dailyStreak(profile.highestDailyStreak)}"
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "ЛУЧШАЯ ЕЖЕДНЕВНАЯ СЕРИЯ",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 2,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                dailyStreak(profile.highestDailyStreak),
                style = MaterialTheme.typography.titleMedium,
                color = TealGlow,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun LedgerPair(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LedgerMetric(leftLabel, leftValue, Modifier.weight(1f))
        LedgerMetric(rightLabel, rightValue, Modifier.weight(1f), alignEnd = true)
    }
}

@Composable
private fun LedgerMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    Column(
        modifier = modifier
            .heightIn(min = 62.dp)
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            maxLines = 2,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = TextWarm,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            maxLines = 1,
        )
    }
}

@Composable
private fun LedgerDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.04f)))
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
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label: $value"
        },
    ) {
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

private fun dailyStreak(value: Int): String = if (value > 0) "$value дн." else "—"

private fun grouped(value: Long): String {
    val safe = value.coerceAtLeast(0L).toString()
    return safe.reversed().chunked(3).joinToString(" ").reversed()
}
