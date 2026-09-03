package com.steamforge.game.ui.blueprints

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TealSurface
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm
import com.steamforge.game.ui.components.BrassRoundButton
import com.steamforge.game.ui.components.SteamBackdrop
import com.steamforge.game.ui.components.SteamPanel

@Composable
fun BlueprintsScreen(
    vm: BlueprintsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

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
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BrassRoundButton("←", "Назад", onBack)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ЧЕРТЕЖИ", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
                    Text("Коллекция инженерных узлов", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
                Text(
                    "${ui.collected}/${ui.total}",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (ui.completed) TealGlow else BrassBright,
                )
            }
            Spacer(Modifier.height(14.dp))

            SteamPanel(
                modifier = Modifier.fillMaxWidth(),
                highlighted = ui.completed,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    ui.title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = BrassBright,
                    textAlign = TextAlign.Center,
                )
                Text(
                    ui.description,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                BlueprintHero(ui.completed)
                Spacer(Modifier.height(10.dp))
                BlueprintProgress(ui.fraction)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (ui.completed) "КОМПЛЕКТ СОБРАН · STEAM ENGINE ONLINE" else "Соберите все 6 узлов для установки двигателя в мастерской",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (ui.completed) TealGlow else TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(10.dp))

            ui.pieces.forEachIndexed { index, piece ->
                BlueprintPieceRow(piece)
                if (index != ui.pieces.lastIndex) Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(14.dp))
            Text(
                if (ui.workshopUnlocked) {
                    "Двигатель установлен в мастерской. Следующие коллекции смогут открывать новые физические модули."
                } else {
                    "Первая недостающая деталь дня выдаётся за первый полученный контрактный reward. Дубликаты не выпадают."
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun BlueprintHero(complete: Boolean) {
    val accent = if (complete) TealGlow else BrassBright
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D2229))
            .border(1.dp, accent.copy(alpha = 0.34f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
            val line = Color(0xFF63B7BA).copy(alpha = if (complete) 0.78f else 0.46f)
            val faint = line.copy(alpha = 0.16f)
            val unit = size.minDimension
            val center = center

            var x = 0f
            val step = 18.dp.toPx()
            while (x < size.width) {
                drawLine(faint, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(faint, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                y += step
            }

            drawCircle(line, unit * 0.24f, center, style = Stroke(2.dp.toPx()))
            drawCircle(line, unit * 0.11f, center, style = Stroke(2.dp.toPx()))
            drawRect(
                color = line,
                topLeft = Offset(center.x - unit * 0.33f, center.y - unit * 0.09f),
                size = androidx.compose.ui.geometry.Size(unit * 0.18f, unit * 0.18f),
                style = Stroke(2.dp.toPx()),
            )
            drawRect(
                color = line,
                topLeft = Offset(center.x + unit * 0.15f, center.y - unit * 0.12f),
                size = androidx.compose.ui.geometry.Size(unit * 0.22f, unit * 0.24f),
                style = Stroke(2.dp.toPx()),
            )
            drawLine(line, Offset(center.x - unit * 0.15f, center.y), Offset(center.x - unit * 0.24f, center.y), 3.dp.toPx())
            drawLine(line, Offset(center.x + unit * 0.11f, center.y), Offset(center.x + unit * 0.15f, center.y), 3.dp.toPx())
            drawLine(line, Offset(center.x, center.y - unit * 0.24f), Offset(center.x, center.y - unit * 0.36f), 2.dp.toPx())
            drawLine(line, Offset(center.x, center.y + unit * 0.24f), Offset(center.x, center.y + unit * 0.35f), 2.dp.toPx())
        }
        Text(
            if (complete) "ASSEMBLY VERIFIED" else "STEAM ENGINE · REV A",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp),
            style = MaterialTheme.typography.labelSmall,
            color = accent,
        )
    }
}

@Composable
private fun BlueprintPieceRow(piece: BlueprintPieceUi) {
    val accent = if (piece.collected) TealGlow else TextMuted
    val shape = RoundedCornerShape(11.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Panel.copy(alpha = if (piece.collected) 0.88f else 0.56f))
            .border(1.dp, accent.copy(alpha = if (piece.collected) 0.28f else 0.10f), shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (piece.collected) TealSurface.copy(alpha = 0.46f) else Recess)
                .border(1.dp, accent.copy(alpha = 0.32f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (piece.collected) "✓" else "—", color = accent, style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(piece.def.title, style = MaterialTheme.typography.titleMedium, color = if (piece.collected) TextWarm else TextMuted)
            Text(piece.def.technicalLabel, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        Text(if (piece.collected) "COLLECTED" else "MISSING", style = MaterialTheme.typography.labelSmall, color = accent)
    }
}

@Composable
private fun BlueprintProgress(fraction: Float) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(shape)
            .background(Recess)
            .border(1.dp, Brass.copy(alpha = 0.18f), shape),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(shape)
                .background(TealGlow.copy(alpha = 0.74f)),
        )
    }
}
