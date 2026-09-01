package com.steamforge.game.ui.settings

import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
    val activity = LocalContext.current as? Activity
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrassRoundButton("←", "Назад", onBack)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Настройки", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
                    Text("Игра, покупки и приватность", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
            }
            Spacer(Modifier.height(14.dp))

            SettingsGroupTitle("ИГРА")
            Spacer(Modifier.height(6.dp))
            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                SettingToggleRow("♪", "Звук", "Звуковые эффекты", ui.soundEnabled, vm::setSound)
                SettingDivider()
                SettingToggleRow("▣", "Вибрация", "Виброотклик на действия", ui.hapticsEnabled, vm::setHaptics)
                SettingDivider()
                SettingToggleRow("⚙", "Анимации", "Визуальные эффекты и движение", ui.animationsEnabled, vm::setAnimations)
            }

            Spacer(Modifier.height(16.dp))
            SettingsGroupTitle("ПОКУПКИ")
            Spacer(Modifier.height(6.dp))
            RemoveAdsSection(
                ui = ui,
                canPurchase = activity != null,
                onPurchase = { activity?.let(vm::purchaseRemoveAds) },
                onRefresh = vm::refreshPurchases,
            )

            Spacer(Modifier.height(16.dp))
            SettingsGroupTitle("ПРИВАТНОСТЬ")
            Spacer(Modifier.height(6.dp))
            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                SettingToggleRow(
                    "▥",
                    "Аналитика и реклама",
                    "AppMetrica и рекламный SDK",
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

            Spacer(Modifier.height(18.dp))
            SettingsGroupTitle("ДАННЫЕ")
            Spacer(Modifier.height(6.dp))
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
                        "испытания и сохранённая партия. Настройки, выбор приватности и покупки сохранятся. " +
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
private fun RemoveAdsSection(
    ui: SettingsUiState,
    canPurchase: Boolean,
    onPurchase: () -> Unit,
    onRefresh: () -> Unit,
) {
    SteamPanel(
        modifier = Modifier.fillMaxWidth(),
        highlighted = ui.removeAdsOwned,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SettingIcon("◇", ui.removeAdsOwned)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Без рекламы", style = MaterialTheme.typography.titleMedium, color = TextWarm)
                Text(
                    "Убирает автоматическую рекламу между партиями. Добровольное видео ×2 XP Мастерской остаётся доступным.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            ui.removeAdsOwned -> SteamButton(
                text = "БЕЗ РЕКЛАМЫ АКТИВНО",
                onClick = { },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                style = SteamButtonStyle.Teal,
            )
            !ui.removeAdsConfigured -> SteamButton(
                text = "ПОКУПКА ПОКА НЕДОСТУПНА",
                onClick = { },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                style = SteamButtonStyle.Dark,
            )
            ui.removeAdsLoading || ui.removeAdsPurchaseInProgress -> SteamButton(
                text = if (ui.removeAdsPurchaseInProgress) "ОТКРЫВАЕМ ОПЛАТУ…" else "ПРОВЕРЯЕМ ПОКУПКИ…",
                onClick = { },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                style = SteamButtonStyle.Dark,
            )
            ui.removeAdsProductAvailable && canPurchase -> SteamButton(
                text = buildString {
                    append("УБРАТЬ РЕКЛАМУ")
                    ui.removeAdsPriceLabel?.let { append(" · ").append(it) }
                },
                onClick = onPurchase,
                modifier = Modifier.fillMaxWidth(),
                style = SteamButtonStyle.Brass,
            )
            else -> SteamButton(
                text = "ОБНОВИТЬ ПОКУПКИ",
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                style = SteamButtonStyle.Dark,
            )
        }
        ui.billingMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = TextMuted)
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
private fun PrivacyPolicyRow() {
    val context = LocalContext.current
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
        Text("›", style = MaterialTheme.typography.headlineSmall, color = TextMuted)
    }
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
