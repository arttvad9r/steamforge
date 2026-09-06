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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.steamforge.game.theme.SurfaceDark
import com.steamforge.game.theme.TealGlow
import com.steamforge.game.theme.TealSurface
import com.steamforge.game.theme.TextMuted
import com.steamforge.game.theme.TextWarm

private val FrameShape = RoundedCornerShape(14.dp)

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
                        Color(0xFF090F15),
                        Background,
                        Color(0xFF0D151C),
                        Color(0xFF080D12),
                    ),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val edgeMetal = BrassDark.copy(alpha = 0.12f)
            val edgeHighlight = Copper.copy(alpha = 0.055f)
            val tealAtmosphere = TealGlow.copy(alpha = 0.040f)
            val warmAtmosphere = BrassBright.copy(alpha = 0.028f)
            val margin = 10.dp.toPx()

            drawCircle(
                warmAtmosphere,
                radius = size.minDimension * 0.46f,
                center = Offset(size.width * 0.72f, -size.height * 0.01f),
            )
            drawCircle(
                tealAtmosphere,
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.02f, size.height * 0.72f),
            )

            // Industrial structure is only a peripheral cue. The center stays quiet for puzzle play.
            drawLine(edgeMetal, Offset(margin, 0f), Offset(margin, size.height), 2.dp.toPx(), StrokeCap.Round)
            drawLine(edgeMetal, Offset(size.width - margin, 0f), Offset(size.width - margin, size.height), 2.dp.toPx(), StrokeCap.Round)
            drawLine(edgeHighlight, Offset(margin + 1.dp.toPx(), 0f), Offset(margin + 1.dp.toPx(), size.height), 1.dp.toPx())
            drawLine(edgeHighlight, Offset(size.width - margin + 1.dp.toPx(), 0f), Offset(size.width - margin + 1.dp.toPx(), size.height), 1.dp.toPx())
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
    val configuration = LocalConfiguration.current
    val narrowScreen = configuration.screenWidthDp < 390
    val shortScreen = configuration.screenHeightDp < 850
    val vertical = when {
        compact && shortScreen -> 5.dp
        compact -> 8.dp
        narrowScreen -> 9.dp
        else -> 13.dp
    }
    val titleStyle = when {
        compact -> MaterialTheme.typography.headlineSmall
        narrowScreen -> MaterialTheme.typography.headlineLarge
        else -> MaterialTheme.typography.displaySmall
    }
    val hasSideAction = leading != null || trailing != null
    val titleModifier = if (hasSideAction) {
        Modifier.padding(horizontal = 56.dp).fillMaxWidth()
    } else {
        Modifier.fillMaxWidth(if (compact) 0.76f else 0.84f)
    }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (leading != null) Box(Modifier.align(Alignment.CenterStart)) { leading() }
        SteamPanel(
            modifier = titleModifier,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = vertical),
            highlighted = true,
        ) {
            Text(
                "STEAMFORGE",
                modifier = Modifier.fillMaxWidth(),
                style = titleStyle,
                color = BrassBright,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
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
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(10.dp),
    content: @Composable () -> Unit,
) {
    val border = if (highlighted) Brass.copy(alpha = 0.68f) else BrassDark.copy(alpha = 0.42f)
    Box(
        modifier = modifier
            .shadow(
                4.dp,
                FrameShape,
                ambientColor = Color.Black.copy(alpha = 0.24f),
                spotColor = Color.Black.copy(alpha = 0.34f),
            )
            .clip(FrameShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        PanelRaised.copy(alpha = 0.82f),
                        Panel.copy(alpha = 0.94f),
                        SurfaceDark.copy(alpha = 0.98f),
                    ),
                ),
            )
            .border(1.dp, border, FrameShape),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val inset = 15.dp.toPx()
            if (size.width > inset * 2f && size.height > 10.dp.toPx()) {
                drawLine(
                    Color.White.copy(alpha = if (highlighted) 0.065f else 0.035f),
                    Offset(inset, 3.dp.toPx()),
                    Offset(size.width - inset, 3.dp.toPx()),
                    1.dp.toPx(),
                )
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
        SteamButtonStyle.Teal -> listOf(Color(0xFF28585D), Color(0xFF18343A), TealGlow, TextWarm)
        SteamButtonStyle.Brass -> listOf(Color(0xFF72552D), Color(0xFF49341D), BrassBright, TextWarm)
        SteamButtonStyle.Dark -> listOf(PanelRaised, SurfaceDark, BrassDark, TextWarm)
        SteamButtonStyle.Danger -> listOf(Color(0xFF663321), Color(0xFF351C17), Color(0xFFC7603A), TextWarm)
    }
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(54.dp)
            .shadow(3.dp, shape, ambientColor = Color.Black.copy(alpha = 0.22f), spotColor = Color.Black.copy(alpha = 0.32f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(start, end)))
            .border(1.dp, border.copy(alpha = if (enabled) 0.76f else 0.26f), shape)
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
            .size(46.dp)
            .shadow(3.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.22f), spotColor = Color.Black.copy(alpha = 0.32f))
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(PanelRaised, SurfaceDark, Recess)))
            .border(1.dp, Brass.copy(alpha = 0.62f), CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = MaterialTheme.typography.titleLarge, color = BrassBright)
    }
}

@Composable
fun StatPlate(label: String, value: String, modifier: Modifier = Modifier, accent: Color = TextWarm) {
    val compactScreen = LocalConfiguration.current.screenWidthDp < 390 || LocalConfiguration.current.screenHeightDp < 850
    val valueStyle = when {
        value.length <= 5 -> MaterialTheme.typography.titleLarge
        value.length <= 10 -> MaterialTheme.typography.labelLarge
        else -> MaterialTheme.typography.labelMedium
    }
    val labelStyle = if (compactScreen || label.length > 5) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
    val horizontalPadding = if (compactScreen) 9.dp else 12.dp
    val verticalPadding = if (compactScreen) 5.dp else 7.dp
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(Recess.copy(alpha = 0.58f))
            .border(1.dp, Color.White.copy(alpha = 0.045f), shape)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .semantics { contentDescription = "$label: $value" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = labelStyle,
            color = TextMuted,
            maxLines = 1,
            softWrap = false,
        )
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

@Composable
fun MechanicalToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val track = if (checked) TealSurface else Recess
    Row(
        modifier = modifier
            .width(94.dp)
            .height(48.dp)
            .clip(shape)
            .background(track)
            .border(1.dp, if (checked) TealGlow.copy(alpha = 0.82f) else BrassDark.copy(alpha = 0.66f), shape)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(4.dp)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!checked) Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(BrassBright, BrassDark)))
                .border(1.dp, BrassBright.copy(alpha = 0.48f), CircleShape),
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
            val stroke = 11.dp.toPx()
            val inset = stroke / 2f
            val rect = Rect(inset, inset, size.width - inset, size.height - inset)
            drawArc(Color(0xFF29464B), 150f, 80f, false, style = Stroke(stroke, cap = StrokeCap.Round), topLeft = rect.topLeft, size = rect.size)
            drawArc(Color(0xFF776331), 230f, 55f, false, style = Stroke(stroke, cap = StrokeCap.Butt), topLeft = rect.topLeft, size = rect.size)
            drawArc(Color(0xFF87513A), 285f, 105f, false, style = Stroke(stroke, cap = StrokeCap.Round), topLeft = rect.topLeft, size = rect.size)
            val normalized = if (active) 1f else pressure.coerceIn(0, 100) / 100f
            val angle = Math.toRadians((150f + 240f * normalized).toDouble())
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.34f
            val end = Offset(
                center.x + kotlin.math.cos(angle).toFloat() * radius,
                center.y + kotlin.math.sin(angle).toFloat() * radius,
            )
            drawLine(if (active) TealGlow else BrassBright, center, end, 3.dp.toPx(), StrokeCap.Round)
            drawCircle(BrassBright, 6.dp.toPx(), center)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (active) "OVERDRIVE" else "ДАВЛЕНИЕ", style = MaterialTheme.typography.labelMedium, color = if (active) TealGlow else TextMuted)
            Text(if (active) "×2" else "$pressure%", style = MaterialTheme.typography.headlineSmall, color = TextWarm)
            if (active) Text("$overdriveRemaining объедин.", style = MaterialTheme.typography.labelMedium, color = TealGlow)
        }
    }
}
