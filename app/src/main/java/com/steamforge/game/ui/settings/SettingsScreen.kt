package com.steamforge.game.ui.settings

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.Danger
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TealSurface
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.ui.components.BrassRoundButton
import com.steamforge.game.ui.components.MechanicalToggle
import com.steamforge.game.ui.components.SteamBackdrop
import com.steamforge.game.ui.components.SteamButton
import com.steamforge.game.ui.components.SteamButtonStyle
import com.steamforge.game.ui.components.SteamDecisionDialog
import com.steamforge.game.ui.components.SteamLogoHeader
import com.steamforge.game.ui.components.SteamPanel

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }

    SteamBackdrop(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
        ) {
            Spacer(Modifier.height(10.dp))
            SteamLogoHeader(
                compact = true,
                leading = { BrassRoundButton("←", "Назад", onBack) },
            )
            Spacer(Modifier.height(8.dp))

            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                highlighted = true,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    "НАСТРОЙКИ",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextWarm,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(5.dp))
                SettingDivider()
                SettingToggleRow("♪", "Звук", "Звуковые эффекты", ui.soundEnabled, vm::setSound)
                SettingDivider()
                SettingToggleRow("▣", "Вибрация", "Виброотклик на действия", ui.hapticsEnabled, vm::setHaptics)
                SettingDivider()
                SettingToggleRow("⚙", "Анимации", "Визуальные эффекты и движение", ui.animationsEnabled, vm::setAnimations)
            }

            Spacer(Modifier.height(10.dp))
            Text("ПРИВАТНОСТЬ", style = MaterialTheme.typography.labelLarge, color = BrassBright, modifier = Modifier.padding(start = 6.dp))
            Spacer(Modifier.height(6.dp))
            SteamPanel(Modifier.fillMaxWidth()) {
                SettingToggleRow(
                    "▥",
                    "Аналитика и реклама",
                    "AppMetrica + согласие для рекламного SDK",
                    ui.analyticsConsent == true,
                    vm::setAnalyticsConsent,
                )
                SettingDivider()
                Text(
                    "При отключении AppMetrica перестаёт отправлять статистику. Рекламный SDK получает " +
                        "признак отсутствия согласия; реклама может продолжать показываться с учётом " +
                        "правил SDK и региона.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 10.dp),
                )
                SettingDivider()
                PrivacyPolicyRow()
            }

            Spacer(Modifier.height(16.dp))
            DangerSection(onReset = { confirmReset = true })
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmReset) {
        SteamDecisionDialog(
            title = "СБРОСИТЬ ПРОГРЕСС?",
            onDismissRequest = { confirmReset = false },
            body = {
                Text(
                    "Будут удалены очки, гемы, уровень мастерской, достижения, статистика, " +
                        "испытания и сохранённая партия. Настройки и выбор приватности сохранятся. " +
                        "Отменить это нельзя.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            },
            actions = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SteamButton(
                        text = "ОТМЕНА",
                        onClick = { confirmReset = false },
                        modifier = Modifier.weight(1f),
                        style = SteamButtonStyle.Dark,
                    )
                    SteamButton(
                        text = "СБРОСИТЬ",
                        onClick = {
                            confirmReset = false
                            vm.resetProgress()
                        },
                        modifier = Modifier.weight(1f),
                        style = SteamButtonStyle.Danger,
                    )
                }
            },
        )
    }
}

@Composable
private fun SettingToggleRow(
    icon: String,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics { contentDescription = title },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon, checked)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextWarm, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(8.dp))
        MechanicalToggle(checked, onChange, title)
    }
}

@Composable
private fun SettingIcon(symbol: String, active: Boolean) {
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(if (active) TealSurface else Color(0xFF3A2A1C), Recess)))
            .border(1.5.dp, if (active) TealGlow.copy(alpha = 0.75f) else Brass.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = if (active) BrassBright else TextMuted, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SettingDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .height(1.dp)
            .background(Brass.copy(alpha = 0.25f)),
    )
}

@Composable
private fun PrivacyPolicyRow() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val url = com.steamforge.game.BuildConfig.PRIVACY_POLICY_URL
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = url.isNotBlank()) {
                runCatching {
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, url.toUri()))
                }
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon("▣", url.isNotBlank())
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Политика конфиденциальности", style = MaterialTheme.typography.titleMedium, color = TextWarm)
            Text(
                if (url.isBlank()) "URL будет добавлен перед публикацией" else "Открыть документ",
                style = MaterialTheme.typography.bodyMedium,
                color = if (url.isBlank()) TextMuted else TealGlow,
            )
        }
        Text("›", style = MaterialTheme.typography.headlineSmall, color = BrassBright)
    }
}

@Composable
private fun DangerSection(onReset: () -> Unit) {
    SteamPanel(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF5A2415))
                    .border(1.dp, Danger.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("⚙  ОПАСНАЯ ЗОНА  ⚙", style = MaterialTheme.typography.labelLarge, color = Color(0xFFF09B68))
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingIcon("☠", false)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Сброс прогресса", style = MaterialTheme.typography.titleMedium, color = TextWarm)
                    Text("Удалить игровой прогресс и начать заново", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
            Spacer(Modifier.height(10.dp))
            SteamButton(
                text = "СБРОСИТЬ",
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                style = SteamButtonStyle.Danger,
            )
        }
    }
}
