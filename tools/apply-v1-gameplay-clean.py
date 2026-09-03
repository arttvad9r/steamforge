from pathlib import Path

path = Path("app/src/main/java/com/steamforge/game/ui/game/GameScreen.kt")
text = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    text = text.replace(old, new, 1)


replace_once(
'''                    Spacer(Modifier.height(6.dp))
                    SteamLogoHeader(
                        compact = true,
                        leading = { BrassRoundButton("←", "В мастерскую", ::leave) },
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(Modifier.fillMaxWidth()) {
                        StatPlate("СЧЁТ", ui.state.score.toString(), Modifier.weight(1f), BrassBright)
                        Spacer(Modifier.width(8.dp))
                        StatPlate("ЛУЧШИЙ", ui.best.toString(), Modifier.weight(1f), TextWarm)
                        Spacer(Modifier.width(8.dp))
                        StatPlate("ГЕМЫ", ui.gems.toString(), Modifier.weight(0.78f), TealGlow)
                    }
                    Spacer(Modifier.height(7.dp))

                    SteamPanel(
                        modifier = Modifier.fillMaxWidth(),
                        highlighted = ui.overdriveRemaining > 0,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HudMetric(
                                label = "ХОДЫ",
                                value = ui.state.moves.toString(),
                                accent = BrassBright,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(6.dp))
                            PressureGauge(
                                pressure = ui.pressure,
                                overdriveRemaining = ui.overdriveRemaining,
                                animationsActive = ui.animationsActive,
                                modifier = Modifier.size(112.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            HudMetric(
                                label = "ОТМЕНА",
                                value = if (ui.freeUndosLeft > 0) "${ui.freeUndosLeft} бесплатно" else "◆ 5",
                                accent = if (ui.freeUndosLeft > 0) TealGlow else BrassBright,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        val daily = ui.daily
                        if (daily != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                dailyGoalText(daily) + if (ui.dailySatisfied) " — выполнено" else "",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (ui.dailySatisfied) TealGlow else TextMuted,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(7.dp))
''',
'''                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BrassRoundButton("←", "В мастерскую", ::leave)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "STEAMFORGE",
                                style = MaterialTheme.typography.titleMedium,
                                color = BrassBright,
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text(
                                if (ui.daily != null) "ИСПЫТАНИЕ ДНЯ" else "МЕХАНИЧЕСКОЕ ЯДРО",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                        Text(
                            "ХОД ${ui.state.moves}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    SteamPanel(
                        modifier = Modifier.fillMaxWidth(),
                        highlighted = ui.overdriveRemaining > 0,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HudMetric(
                                label = "СЧЁТ",
                                value = ui.state.score.toString(),
                                accent = BrassBright,
                                modifier = Modifier.weight(1f),
                            )
                            HudMetric(
                                label = "ЛУЧШИЙ",
                                value = ui.best.toString(),
                                accent = TextWarm,
                                modifier = Modifier.weight(1f),
                            )
                            HudMetric(
                                label = "ГЕМЫ",
                                value = ui.gems.toString(),
                                accent = TealGlow,
                                modifier = Modifier.weight(0.72f),
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            if (ui.overdriveRemaining > 0) {
                                "OVERDRIVE ×2 · ${ui.overdriveRemaining} объедин."
                            } else {
                                "ДАВЛЕНИЕ ${ui.pressure}%"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (ui.overdriveRemaining > 0) TealGlow else TextMuted,
                            textAlign = TextAlign.Center,
                        )
                        val daily = ui.daily
                        if (daily != null) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                dailyGoalText(daily) + if (ui.dailySatisfied) " · выполнено" else "",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (ui.dailySatisfied) TealGlow else TextWarm,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
''',
"portrait HUD",
)

replace_once(
'''                    SteamPanel(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {''',
'''                    SteamPanel(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(7.dp)) {''',
"portrait tool panel padding",
)
replace_once(
'''                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Свайпните по полю · одинаковые детали объединяются",
                        style = MaterialTheme.typography.labelMedium,''',
'''                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Свайпните по полю · одинаковые детали объединяются",
                        style = MaterialTheme.typography.labelSmall,''',
"portrait helper copy",
)
replace_once(
'''        Spacer(Modifier.height(2.dp))
        Text(
            text = value,''',
'''        Spacer(Modifier.height(1.dp))
        Text(
            text = value,''',
"HUD metric spacing",
)
replace_once(
'''    val shape = RoundedCornerShape(18.dp)
    BoxWithConstraints(modifier) {''',
'''    val shape = RoundedCornerShape(16.dp)
    BoxWithConstraints(modifier) {''',
"board shape",
)
replace_once(
'''                .shadow(14.dp, shape)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF2A1B10), Recess)))
                .border(3.dp, Brass, shape)
                .padding(2.dp)
                .border(1.dp, BrassBright.copy(alpha = 0.28f), RoundedCornerShape(15.dp))
                .swipeDetector(onSwipe),''',
'''                .shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.48f))
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF172029), Recess)))
                .border(1.dp, BrassDark.copy(alpha = 0.78f), shape)
                .swipeDetector(onSwipe),''',
"board frame",
)
replace_once(
'''                                        Recess.copy(alpha = 0.94f),
                                        Panel.copy(alpha = 0.58f),''',
'''                                        Recess.copy(alpha = 0.88f),
                                        Panel.copy(alpha = 0.42f),''',
"empty cell fill",
)
replace_once(
'''                            .border(1.dp, BrassDark.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),''',
'''                            .border(1.dp, Color.White.copy(alpha = 0.035f), RoundedCornerShape(10.dp)),''',
"empty cell border",
)
replace_once(
'''    val colors = tileColors(tile.level)
    val shape = RoundedCornerShape(10.dp)
    Box(''',
'''    val colors = tileColors(tile.level)
    val shape = RoundedCornerShape(11.dp)
    Box(''',
"tile shape",
)
replace_once(
'''            .then(
                if (colors.glow) Modifier.shadow(12.dp, shape, ambientColor = BrassBright, spotColor = TealGlow)
                else Modifier.shadow(3.dp, shape),
            )
            .clip(shape)
            .background(tileBevel(tile.level))
            .border(if (colors.glow) 2.dp else 1.dp, if (colors.glow) BrassBright else BrassDark, shape)
            .then(if (removable) Modifier.border(3.dp, TealGlow, shape).clickable(onClick = onClick) else Modifier)''',
'''            .then(
                if (colors.glow) {
                    Modifier.shadow(8.dp, shape, ambientColor = BrassBright.copy(alpha = 0.45f), spotColor = TealGlow.copy(alpha = 0.42f))
                } else {
                    Modifier.shadow(2.dp, shape, ambientColor = Color.Black.copy(alpha = 0.22f), spotColor = Color.Black.copy(alpha = 0.30f))
                },
            )
            .clip(shape)
            .background(tileBevel(tile.level))
            .border(1.dp, if (colors.glow) BrassBright.copy(alpha = 0.92f) else BrassDark.copy(alpha = 0.54f), shape)
            .then(if (removable) Modifier.border(2.dp, TealGlow, shape).clickable(onClick = onClick) else Modifier)''',
"tile frame",
)
replace_once(
'''            val bevelHighlight = Color.White.copy(alpha = if (tile.level <= 2) 0.24f else 0.14f)
            val bevelShadow = Color.Black.copy(alpha = 0.34f)

            if (tile.level >= 8) {
                drawCircle(
                    TealGlow.copy(alpha = if (colors.glow) 0.16f else 0.07f),
                    radius = size.minDimension * 0.34f,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
            }

            drawLine(bevelHighlight, Offset(inset, inset), Offset(size.width - inset, inset), 1.dp.toPx())
            drawLine(bevelHighlight, Offset(inset, inset), Offset(inset, size.height - inset), 1.dp.toPx())
            drawLine(bevelShadow, Offset(inset, size.height - inset), Offset(size.width - inset, size.height - inset), 1.5.dp.toPx())
            drawLine(bevelShadow, Offset(size.width - inset, inset), Offset(size.width - inset, size.height - inset), 1.5.dp.toPx())

            val r = 2.dp.toPx()
            val p = 7.dp.toPx()
            val rivet = if (tile.level >= 7) BrassBright.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.28f)
            val centers = listOf(
                Offset(p, p),
                Offset(size.width - p, p),
                Offset(p, size.height - p),
                Offset(size.width - p, size.height - p),
            )
            centers.forEach { center ->
                drawCircle(Color.Black.copy(alpha = 0.38f), r * 1.35f, center + Offset(0.6.dp.toPx(), 0.7.dp.toPx()))
                drawCircle(rivet, r, center)
                drawCircle(Color.White.copy(alpha = 0.18f), r * 0.38f, center - Offset(0.45.dp.toPx(), 0.45.dp.toPx()))
            }''',
'''            val bevelHighlight = Color.White.copy(alpha = if (tile.level <= 2) 0.18f else 0.11f)
            val bevelShadow = Color.Black.copy(alpha = 0.24f)

            if (tile.level >= 9) {
                drawCircle(
                    TealGlow.copy(alpha = if (colors.glow) 0.11f else 0.045f),
                    radius = size.minDimension * 0.31f,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
            }

            drawLine(bevelHighlight, Offset(inset, inset), Offset(size.width - inset, inset), 1.dp.toPx())
            drawLine(bevelShadow, Offset(inset, size.height - inset), Offset(size.width - inset, size.height - inset), 1.dp.toPx())''',
"tile ornament",
)
replace_once(
'''    val colors = tileColors(from.level)
    val shape = RoundedCornerShape(10.dp)
    Box(''',
'''    val colors = tileColors(from.level)
    val shape = RoundedCornerShape(11.dp)
    Box(''',
"ghost shape",
)
replace_once(
'''            .border(1.dp, BrassDark.copy(alpha = 0.65f), shape),''',
'''            .border(1.dp, BrassDark.copy(alpha = 0.50f), shape),''',
"ghost border",
)
replace_once(
'''    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (selected) listOf(TealSurface, Color(0xFF102B29)) else listOf(Color(0xFF4A311A), Recess),
                ),
            )
            .border(2.dp, if (selected) TealGlow else Brass, shape)
            .clickable(enabled = active, onClick = onClick)
            .padding(horizontal = 10.dp)
            .semantics { role = Role.Button; contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selected) TealSurface else Panel)
                .border(1.dp, if (selected) TealGlow else BrassDark, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, color = if (active) BrassBright else TextMuted, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f), color = if (active) TextWarm else TextMuted, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }''',
'''    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (selected) listOf(TealSurface, Color(0xFF17373D)) else listOf(Panel.copy(alpha = 0.92f), Recess),
                ),
            )
            .border(1.dp, if (selected) TealGlow.copy(alpha = 0.82f) else BrassDark.copy(alpha = 0.62f), shape)
            .clickable(enabled = active, onClick = onClick)
            .padding(horizontal = 12.dp)
            .semantics { role = Role.Button; contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(symbol, color = if (active) BrassBright else TextMuted, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            color = if (active) TextWarm else TextMuted,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }''',
"tool button",
)

path.write_text(text, encoding="utf-8")
print("Applied V1 gameplay clean pass")
