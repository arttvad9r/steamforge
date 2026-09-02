package com.steamforge.game.ui.cosmetics

import androidx.activity.compose.LocalActivity
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.billing.CosmeticProduct
import com.steamforge.game.billing.StoreProductState
import com.steamforge.game.cosmetics.CosmeticCatalog
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.BrassDark
import com.steamforge.game.theme.Copper
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.Patina
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TealSurface
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.theme.tileBevel
import com.steamforge.game.theme.tileColors
import com.steamforge.game.ui.components.BrassRoundButton
import com.steamforge.game.ui.components.SteamBackdrop
import com.steamforge.game.ui.components.SteamButton
import com.steamforge.game.ui.components.SteamButtonStyle
import com.steamforge.game.ui.components.SteamPanel

@Composable
fun CosmeticsScreen(
    vm: CosmeticsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val activity = LocalActivity.current

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
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BrassRoundButton("←", "Назад", onBack)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Оформление", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
                    Text("Только визуальные изменения · без бонусов к игре", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                }
            }
            Spacer(Modifier.height(14.dp))

            SectionTitle("НАБОР ПЛИТОК")
            Spacer(Modifier.height(6.dp))
            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                highlighted = ui.effective.tileSet == CosmeticCatalog.TILE_PATINA,
            ) {
                Text("Patina Tile Set", style = MaterialTheme.typography.titleLarge, color = TextWarm)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Холодная патина и teal-кромка поверх той же контрастной шкалы материалов. Значения и иерархия плиток не меняются.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
                Spacer(Modifier.height(12.dp))
                TileSetPreview(patina = ui.effective.tileSet == CosmeticCatalog.TILE_PATINA)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SteamButton(
                        text = if (ui.effective.tileSet == CosmeticCatalog.TILE_CLASSIC) "КЛАССИКА ВЫБРАНА" else "КЛАССИКА",
                        onClick = { vm.equipTileSet(CosmeticCatalog.TILE_CLASSIC) },
                        enabled = ui.effective.tileSet != CosmeticCatalog.TILE_CLASSIC,
                        modifier = Modifier.weight(1f),
                        style = SteamButtonStyle.Dark,
                    )
                    CosmeticActionButton(
                        owned = ui.tileSetOwned,
                        selected = ui.effective.tileSet == CosmeticCatalog.TILE_PATINA,
                        configured = ui.configured,
                        loading = ui.loading,
                        product = ui.tilePack,
                        ownedText = "PATINA",
                        buyText = "КУПИТЬ PATINA",
                        onEquip = { vm.equipTileSet(CosmeticCatalog.TILE_PATINA) },
                        onPurchase = { activity?.let { vm.purchase(it, CosmeticProduct.TILE_PACK) } },
                        onRefresh = vm::refreshPurchases,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("ТЕМА МАСТЕРСКОЙ")
            Spacer(Modifier.height(6.dp))
            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                highlighted = ui.effective.workshopTheme == CosmeticCatalog.WORKSHOP_FOUNDRY,
            ) {
                Text("Foundry Workshop", style = MaterialTheme.typography.titleLarge, color = TextWarm)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Более тёплая литейная атмосфера: медный свет, тёмный металл и латунный акцент. Навигация и читаемость остаются прежними.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
                Spacer(Modifier.height(12.dp))
                WorkshopThemePreview(foundry = ui.effective.workshopTheme == CosmeticCatalog.WORKSHOP_FOUNDRY)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SteamButton(
                        text = if (ui.effective.workshopTheme == CosmeticCatalog.WORKSHOP_CLASSIC) "КЛАССИКА ВЫБРАНА" else "КЛАССИКА",
                        onClick = { vm.equipWorkshopTheme(CosmeticCatalog.WORKSHOP_CLASSIC) },
                        enabled = ui.effective.workshopTheme != CosmeticCatalog.WORKSHOP_CLASSIC,
                        modifier = Modifier.weight(1f),
                        style = SteamButtonStyle.Dark,
                    )
                    CosmeticActionButton(
                        owned = ui.workshopThemeOwned,
                        selected = ui.effective.workshopTheme == CosmeticCatalog.WORKSHOP_FOUNDRY,
                        configured = ui.configured,
                        loading = ui.loading,
                        product = ui.workshopPack,
                        ownedText = "FOUNDRY",
                        buyText = "КУПИТЬ FOUNDRY",
                        onEquip = { vm.equipWorkshopTheme(CosmeticCatalog.WORKSHOP_FOUNDRY) },
                        onPurchase = { activity?.let { vm.purchase(it, CosmeticProduct.WORKSHOP_PACK) } },
                        onRefresh = vm::refreshPurchases,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("СТАРТОВЫЙ НАБОР")
            Spacer(Modifier.height(6.dp))
            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                highlighted = ui.starterBundle.owned,
            ) {
                Text("Starter Cosmetic Bundle", style = MaterialTheme.typography.titleLarge, color = TextWarm)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Открывает Patina Tile Set и Foundry Workshop. Никаких гемов, XP, ускорителей или игровых преимуществ.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
                Spacer(Modifier.height(10.dp))
                when {
                    ui.allCosmeticsOwned -> SteamButton(
                        text = "ОФОРМЛЕНИЕ ОТКРЫТО",
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        style = SteamButtonStyle.Teal,
                    )
                    !ui.configured -> DisabledPurchaseButton("ПОКУПКА ПОКА НЕДОСТУПНА")
                    ui.loading || ui.starterBundle.purchaseInProgress -> DisabledPurchaseButton(
                        if (ui.starterBundle.purchaseInProgress) "ОТКРЫВАЕМ ОПЛАТУ…" else "ПРОВЕРЯЕМ ПОКУПКИ…",
                    )
                    ui.starterBundle.productAvailable && activity != null -> SteamButton(
                        text = priceText("КУПИТЬ НАБОР", ui.starterBundle.priceLabel),
                        onClick = { vm.purchase(activity, CosmeticProduct.STARTER_BUNDLE) },
                        modifier = Modifier.fillMaxWidth(),
                        style = SteamButtonStyle.Brass,
                    )
                    else -> SteamButton(
                        text = "ОБНОВИТЬ ПОКУПКИ",
                        onClick = vm::refreshPurchases,
                        modifier = Modifier.fillMaxWidth(),
                        style = SteamButtonStyle.Dark,
                    )
                }
            }

            ui.message?.let { message ->
                Spacer(Modifier.height(10.dp))
                Text(
                    message,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Покупки восстанавливаются через магазин. Сброс игрового прогресса не удаляет купленное оформление.",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CosmeticActionButton(
    owned: Boolean,
    selected: Boolean,
    configured: Boolean,
    loading: Boolean,
    product: StoreProductState,
    ownedText: String,
    buyText: String,
    onEquip: () -> Unit,
    onPurchase: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        owned -> SteamButton(
            text = if (selected) "$ownedText ВЫБРАН" else "ВЫБРАТЬ $ownedText",
            onClick = onEquip,
            enabled = !selected,
            modifier = modifier,
            style = if (selected) SteamButtonStyle.Teal else SteamButtonStyle.Dark,
        )
        !configured -> SteamButton(
            text = "НЕДОСТУПНО",
            onClick = { },
            enabled = false,
            modifier = modifier,
            style = SteamButtonStyle.Dark,
        )
        loading || product.purchaseInProgress -> SteamButton(
            text = if (product.purchaseInProgress) "ОПЛАТА…" else "ПРОВЕРКА…",
            onClick = { },
            enabled = false,
            modifier = modifier,
            style = SteamButtonStyle.Dark,
        )
        product.productAvailable -> SteamButton(
            text = priceText(buyText, product.priceLabel),
            onClick = onPurchase,
            modifier = modifier,
            style = SteamButtonStyle.Brass,
        )
        else -> SteamButton(
            text = "ОБНОВИТЬ",
            onClick = onRefresh,
            modifier = modifier,
            style = SteamButtonStyle.Dark,
        )
    }
}

@Composable
private fun DisabledPurchaseButton(text: String) {
    SteamButton(
        text = text,
        onClick = { },
        enabled = false,
        modifier = Modifier.fillMaxWidth(),
        style = SteamButtonStyle.Dark,
    )
}

@Composable
private fun TileSetPreview(patina: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (level in 1..4) {
            val colors = tileColors(level, patina)
            val shape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(shape)
                    .background(tileBevel(level, patina))
                    .border(1.dp, if (patina) Patina.copy(alpha = 0.72f) else BrassDark.copy(alpha = 0.54f), shape),
                contentAlignment = Alignment.Center,
            ) {
                Text((1 shl level).toString(), style = MaterialTheme.typography.titleMedium, color = colors.content)
            }
        }
    }
}

@Composable
private fun WorkshopThemePreview(foundry: Boolean) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    if (foundry) {
                        listOf(Copper.copy(alpha = 0.34f), Panel, Recess)
                    } else {
                        listOf(TealSurface.copy(alpha = 0.42f), Panel, Recess)
                    },
                ),
            )
            .border(1.dp, if (foundry) BrassBright.copy(alpha = 0.48f) else TealGlow.copy(alpha = 0.38f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚙", style = MaterialTheme.typography.headlineSmall, color = if (foundry) Copper else TealGlow)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(if (foundry) "FOUNDRY" else "CLASSIC", style = MaterialTheme.typography.labelLarge, color = TextWarm)
                Text(if (foundry) "медь · латунь · тёмная сталь" else "teal · латунь · сталь", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = BrassBright.copy(alpha = 0.82f),
    )
}

private fun priceText(label: String, price: String?): String = buildString {
    append(label)
    price?.let { append(" · ").append(it) }
}
