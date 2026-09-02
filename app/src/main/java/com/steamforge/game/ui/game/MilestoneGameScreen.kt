package com.steamforge.game.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.cosmetics.CosmeticCatalog
import com.steamforge.game.monetization.AdsManager
import com.steamforge.game.sound.SfxPlayer
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.PanelRaised
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.ui.components.SteamButton
import com.steamforge.game.ui.components.SteamButtonStyle
import com.steamforge.game.ui.components.SteamDecisionDialog

/** Adds rare first-discovery reveals around the existing gameplay surface without changing GameScreen. */
@Composable
fun MilestoneGameScreen(
    vm: GameViewModel,
    sfx: SfxPlayer,
    ads: AdsManager,
    onExit: () -> Unit,
    tileSet: String = CosmeticCatalog.TILE_CLASSIC,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    GameScreen(
        vm = vm,
        sfx = sfx,
        ads = ads,
        onExit = onExit,
        tileSet = tileSet,
        modifier = modifier,
    )

    val milestone = ui.tileMilestone ?: return
    SteamDecisionDialog(
        title = "НОВАЯ ДЕТАЛЬ",
        onDismissRequest = vm::dismissTileMilestone,
        body = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(126.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    TealGlow.copy(alpha = 0.24f),
                                    PanelRaised,
                                    Recess,
                                ),
                            ),
                        )
                        .border(1.dp, Brass.copy(alpha = 0.72f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        milestone.value.toString(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = BrassBright,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    milestone.title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWarm,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    milestone.subtitle,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Впервые собрано в вашей мастерской",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    color = TealGlow,
                    textAlign = TextAlign.Center,
                )
            }
        },
        actions = {
            SteamButton(
                text = if (milestone.value >= 2048) "ЯДРО В СТРОЮ" else "ПРОДОЛЖИТЬ",
                onClick = vm::dismissTileMilestone,
                modifier = Modifier.fillMaxWidth(),
                style = SteamButtonStyle.Teal,
            )
        },
    )
}
