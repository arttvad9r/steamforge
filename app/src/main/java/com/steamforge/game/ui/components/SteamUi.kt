package com.steamforge.game.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.steamforge.game.theme.Background
import com.steamforge.game.theme.Brass
import com.steamforge.game.theme.BrassBright
import com.steamforge.game.theme.BrassDark
import com.steamforge.game.theme.Copper
import com.steamforge.game.theme.Panel
import com.steamforge.game.theme.PanelRaised
import com.steamforge.game.theme.Recess
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TealSurface
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm

private val FrameShape = RoundedCornerShape(18.dp)
private val InnerShape = RoundedCornerShape(14.dp)

@Composable
fun SteamBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF071011),
                        Background,
                        Color(0xFF090705),
                    ),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val pipe = BrassDark.copy(alpha = 0.60f)
            val pipeHi = Copper.copy(alpha = 0.48f)
            val glow = TealGlow.copy(alpha = 0.16f)
            val margin = 14.dp.toPx()
            val pipeW = 8.dp.toPx()

            drawLine(pipe, Offset(margin, 0f), Offset(margin, size.height), pipeW, StrokeCap.Round)
            drawLine(pipe, Offset(size.width - margin, 0f), Offset(size.width - margin, size.height), pipeW, StrokeCap.Round)
            drawLine(pipeHi, Offset(margin + 2.dp.toPx(), 0f), Offset(margin + 2.dp.toPx(), size.height), 1.5.dp.toPx())
            drawLine(pipeHi, Offset(size.width - margin + 2.dp.toPx(), 0f), Offset(size.width - margin + 2.dp.toPx(), size.height), 1.5.dp.toPx())

            drawCircle(glow, radius = 78.dp.toPx(), center = Offset(size.width * 0.10f, size.height * 0.20f))
            drawCircle(glow, radius = 96.dp.toPx(), center = Offset(size.width * 0.90f, size.height * 0.72f))
        }
        content()
    }
}

@Composable
fun SteamLogoHeader(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val vertical = if (compact) 8.dp else 13.dp
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (leading != null) Box(Modifier.align(Alignment.CenterStart)) { leading() }
        SteamPanel(
            modifier = Modifier.fillMaxWidth(if (compact) 0.76f else 0.84f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = vertical),
            highlighted = true,
        ) {
            Text(
                "STEAMFORGE",
                modifier = Modifier.fillMaxWidth(),
                style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.displaySmall,
                color = BrassBright,
                textAlign = TextAlign.Center,
            )
        }
        if (trailing != null) Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}

@Composable
fun SteamSectionTitle(text: String, modifier: Modifier = Modifier) {
    SteamPanel(modifier = modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineSmall,
            color = TextWarm,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SteamPanel(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(14.dp),
    content: @Composable () -> Unit,
) {
    val border = if (highlighted) Brass else BrassDark
    Box(
        modifier = modifier
            .shadow(12.dp, FrameShape, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.7f))
            .clip(FrameShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        PanelRaised.copy(alpha = 0.98f),
                        Panel.copy(alpha = 0.99f),
                        Recess.copy(alpha = 0.98f),
                    ),
                ),
            )
            .border(2.dp, border, FrameShape)
            .padding(3.dp)
            .border(1.dp, BrassBright.copy(alpha = 0.18f), InnerShape),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val inset = 8.dp.toPx()
            val radius = 1.7.dp.toPx()
            val rivet = BrassBright.copy(alpha = if (highlighted) 0.42f else 0.26f)
            val shadow = Color.Black.copy(alpha = 0.55f)
            val centers = listOf(
                Offset(inset, inset),
                Offset(size.width - inset, inset),
                Offset(inset, size.height - inset),
                Offset(size.width - inset, size.height - inset),
            )
            centers.forEach { center ->
                drawCircle(shadow, radius * 1.35f, center + Offset(0.7.dp.toPx(), 0.7.dp.toPx()))
                drawCircle(rivet, radius, center)
                drawCircle(Color.White.copy(alpha = 0.12f), radius * 0.38f, center - Offset(0.45.dp.toPx(), 0.45.dp.toPx()))
            }
        }
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun SteamButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: SteamButtonStyle = SteamButtonStyle.Teal,
    icon: String? = null,
) {
    val (start, end, border, content) = when (style) {
        SteamButtonStyle.Teal -> listOf(TealSurface, Color(0xFF123E3C), TealGlow, TextWarm)
        SteamButtonStyle.Brass -> listOf(Color(0xFF8E5D23), Color(0xFF493015), BrassBright, TextWarm)
        SteamButtonStyle.Dark -> listOf(PanelRaised, Recess, BrassDark, TextWarm)
        SteamButtonStyle.Danger -> listOf(Color(0xFF793019), Color(0xFF3E160E), Color(0xFFE06B3D), TextWarm)
    }
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = modifier
            .height(54.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(start, end)))
            .border(2.dp, border.copy(alpha = if (enabled) 0.92f else 0.35f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp)
            .semantics {
                role = Role.Button
                contentDescription = text
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Text(icon, color = content.copy(alpha = if (enabled) 1f else 0.45f), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            modifier = Modifier.weight(1f),
            color = content.copy(alpha = if (enabled) 1f else 0.45f),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

enum class SteamButtonStyle { Teal, Brass, Dark, Danger }

@Composable
fun BrassRoundButton(
    symbol: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF72501F), Color(0xFF24160B))))
            .border(2.dp, Brass, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = MaterialTheme.typography.titleLarge, color = BrassBright)
    }
}

@Composable
fun StatPlate(label: String, value: String, modifier: Modifier = Modifier, accent: Color = TextWarm) {
    val valueStyle = when {
        value.length <= 5 -> MaterialTheme.typography.titleLarge
        value.length <= 10 -> MaterialTheme.typography.labelLarge
        else -> MaterialTheme.typography.labelMedium
    }
    SteamPanel(modifier = modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                modifier = Modifier.fillMaxWidth(),
                style = valueStyle,
                color = accent,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
fun MechanicalToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val track = if (checked) TealSurface else Color(0xFF4A2518)
    Row(
        modifier = modifier
            .width(94.dp)
            .height(40.dp)
            .clip(shape)
            .background(track)
            .border(2.dp, if (checked) TealGlow else BrassDark, shape)
            .clickable { onCheckedChange(!checked) }
            .padding(4.dp)
            .semantics { contentDescription = "$description: ${if (checked) "включено" else "выключено"}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!checked) Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(BrassBright, BrassDark)))
                .border(1.dp, BrassBright.copy(alpha = 0.6f), CircleShape),
        )
        if (checked) Spacer(Modifier.weight(1f))
    }
}

@Composable
fun PressureDial(
    pressure: Int,
    overdriveRemaining: Int,
    modifier: Modifier = Modifier,
) {
    val active = overdriveRemaining > 0
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2f
            val rect = Rect(inset, inset, size.width - inset, size.height - inset)
            drawArc(Color(0xFF18362A), 150f, 80f, false, style = Stroke(stroke, cap = StrokeCap.Round), topLeft = rect.topLeft, size = rect.size)
            drawArc(Color(0xFF74651D), 230f, 55f, false, style = Stroke(stroke, cap = StrokeCap.Butt), topLeft = rect.topLeft, size = rect.size)
            drawArc(Color(0xFF8D321D), 285f, 105f, false, style = Stroke(stroke, cap = StrokeCap.Round), topLeft = rect.topLeft, size = rect.size)
            val normalized = if (active) 1f else pressure.coerceIn(0, 100) / 100f
            val angle = Math.toRadians((150f + 240f * normalized).toDouble())
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.34f
            val end = Offset(
                center.x + kotlin.math.cos(angle).toFloat() * radius,
                center.y + kotlin.math.sin(angle).toFloat() * radius,
            )
            drawLine(if (active) TealGlow else BrassBright, center, end, 4.dp.toPx(), StrokeCap.Round)
            drawCircle(BrassBright, 7.dp.toPx(), center)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (active) "OVERDRIVE" else "ДАВЛЕНИЕ", style = MaterialTheme.typography.labelMedium, color = if (active) TealGlow else TextMuted)
            Text(if (active) "×2" else "$pressure%", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
            if (active) Text("$overdriveRemaining объедин.", style = MaterialTheme.typography.labelMedium, color = TealGlow)
        }
    }
}
