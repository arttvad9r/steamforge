package com.steamforge.game.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.Danger
import com.steamforge.game.theme.Panel
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
import com.steamforge.game.ui.components.SteamPanel

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }

    SettingsContent(
        ui = ui,
        onBack = onBack,
        onSoundChange = vm::setSound,
        onHapticsChange = vm::setHaptics,
        onAnimationsChange = vm::setAnimations,
        onReset = { confirmReset = true },
        modifier = modifier,
    )

    if (confirmReset) {
        SteamDecisionDialog(
            title = "СБРОСИТЬ ПРОГРЕСС?",
            onDismissRequest = { confirmReset = false },
            body = {
                Text(
                    "Будут удалены очки, гемы, уровень мастерской, достижения, статистика, " +
                        "испытания и сохранённая партия. Настройки звука, вибрации и анимаций сохранятся. " +
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
internal fun SettingsContent(
    ui: SettingsUiState,
    onBack: () -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrassRoundButton("←", "Назад", onBack)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Настройки", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
                    Text("Параметры игры", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
            }
            Spacer(Modifier.height(14.dp))

            SettingsGroupTitle("ИГРА")
            Spacer(Modifier.height(6.dp))
            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                SettingToggleRow("♪", "Звук", "Звуковые эффекты", ui.soundEnabled, onSoundChange)
                SettingDivider()
                SettingToggleRow("▣", "Вибрация", "Виброотклик на действия", ui.hapticsEnabled, onHapticsChange)
                SettingDivider()
                SettingToggleRow("⚙", "Анимации", "Визуальные эффекты и движение", ui.animationsEnabled, onAnimationsChange)
            }

            Spacer(Modifier.height(18.dp))
            SettingsGroupTitle("ДАННЫЕ")
            Spacer(Modifier.height(6.dp))
            DangerSection(onReset = onReset)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsGroupTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = BrassBright.copy(alpha = 0.82f),
    )
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
            .padding(vertical = 9.dp)
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
    val shape = RoundedCornerShape(9.dp)
    Box(
        Modifier
            .size(36.dp)
            .clip(shape)
            .background(if (active) TealSurface.copy(alpha = 0.72f) else Panel)
            .border(1.dp, if (active) TealGlow.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.06f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = if (active) TextWarm else TextMuted, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SettingDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.055f)),
    )
}

@Composable
private fun DangerSection(onReset: () -> Unit) {
    SteamPanel(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingIcon("!", false)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Сброс прогресса", style = MaterialTheme.typography.titleMedium, color = TextWarm)
                Text("Удалить игровой прогресс и начать заново", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Recess.copy(alpha = 0.72f))
                .border(1.dp, Danger.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .padding(8.dp),
        ) {
            Text(
                "Это действие нельзя отменить.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
        }
        Spacer(Modifier.height(10.dp))
        SteamButton(
            text = "СБРОСИТЬ ПРОГРЕСС",
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(),
            style = SteamButtonStyle.Danger,
        )
    }
}
