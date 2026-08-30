package com.steamforge.game.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.theme.Background
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.Danger
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm

private val OutlineDim = Color(0xFF5A4632)

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 560.dp)
            .padding(horizontal = 16.dp)
            .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("Назад") }
            Spacer(Modifier.width(12.dp))
            Text("Настройки", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
        }
        Spacer(Modifier.height(14.dp))

        SettingsGroup {
            ToggleRow("Звук", ui.soundEnabled, vm::setSound)
            Divider()
            ToggleRow("Вибрация", ui.hapticsEnabled, vm::setHaptics)
            Divider()
            ToggleRow("Анимации", ui.animationsEnabled, vm::setAnimations)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Приватность",
            style = MaterialTheme.typography.titleMedium,
            color = TextMuted,
        )
        Spacer(Modifier.height(8.dp))
        SettingsGroup {
            ToggleRow(
                "Анонимная статистика и персонализированная реклама",
                ui.analyticsConsent == true,
                vm::setAnalyticsConsent,
            )
            Divider()
            Text(
                "Прогресс хранится только на устройстве. При включённой опции анонимная " +
                    "статистика (AppMetrica) и реклама (Яндекс) получают технические данные " +
                    "об использовании; отключение прекращает передачу данных.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(14.dp),
            )
            Divider()
            PrivacyPolicyRow()
        }
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { confirmReset = true },
            colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = TextWarm),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сбросить прогресс")
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить прогресс?") },
            text = { Text("Будут удалены: очки, гемы, уровень мастерской, достижения и испытания. Отменить это нельзя.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    vm.resetProgress()
                }) { Text("Сбросить", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun PrivacyPolicyRow() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val url = com.steamforge.game.BuildConfig.PRIVACY_POLICY_URL
    val label = if (url.isBlank()) {
        "Политика конфиденциальности: (URL будет добавлен перед публикацией)"
    } else {
        "Политика конфиденциальности — открыть"
    }
    Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = if (url.isBlank()) TextMuted else BrassBright,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = url.isNotBlank()) {
                runCatching {
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, url.toUri()),
                    )
                }
            }
            .padding(14.dp),
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Panel),
    ) {
        content()
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(1.dp)
            .background(OutlineDim.copy(alpha = 0.5f)),
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextWarm, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = com.steamforge.game.theme.Brass,
                checkedThumbColor = com.steamforge.game.theme.BrassBright,
            ),
        )
    }
}
