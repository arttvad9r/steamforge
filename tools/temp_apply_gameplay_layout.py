from pathlib import Path

path = Path("app/src/main/java/com/steamforge/game/ui/game/GameScreen.kt")
source = path.read_text()
lines = source.splitlines(keepends=True)

assert lines[346].strip() == "Spacer(Modifier.height(6.dp))"
assert 'BrassRoundButton("←", "В мастерскую", ::leave)' in lines[351]
assert '"STEAMFORGE"' in lines[355]
assert '"Свайпните по полю · одинаковые детали объединяются"' in lines[454]
assert lines[459].strip() == "}"

replacement = '''                    Spacer(Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BrassRoundButton("←", "В мастерскую", ::leave)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (ui.daily != null) "ИСПЫТАНИЕ ДНЯ" else "МЕХАНИЧЕСКОЕ ЯДРО",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWarm,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    Spacer(Modifier.height(6.dp))

                    GameplayStatusStrip(
                        score = ui.state.score,
                        best = ui.best,
                        pressure = ui.pressure,
                        overdriveRemaining = ui.overdriveRemaining,
                        daily = ui.daily,
                        dailySatisfied = ui.dailySatisfied,
                    )
                    Spacer(Modifier.height(6.dp))

                    BoardView(
                        state = ui.state,
                        lastResult = ui.lastResult,
                        previousTiles = ui.previousTiles,
                        animationsActive = ui.animationsActive,
                        removingMode = ui.removingMode,
                        canRemove = vm::canRemoveTile,
                        onTileClick = vm::removeTile,
                        onSwipe = vm::onMove,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "ИНСТРУМЕНТЫ",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted.copy(alpha = 0.78f),
                        )
                        Spacer(Modifier.weight(1f))
                        Text("◆ ${ui.gems}", style = MaterialTheme.typography.labelMedium, color = TealGlow)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToolButton(
                            symbol = "↶",
                            label = if (ui.freeUndosLeft > 0) "ОТМЕНА ${ui.freeUndosLeft}" else "ОТМЕНА ◆5",
                            active = ui.canUndo && !ui.finished,
                            onClick = ::undoWithFeedback,
                            modifier = Modifier.weight(1f),
                        )
                        ToolButton(
                            symbol = "⚒",
                            label = if (ui.removingMode) "ВЫБЕРИ ПЛИТКУ" else "КЛЮЧ ◆10",
                            active = !ui.finished,
                            selected = ui.removingMode,
                            onClick = vm::toggleRemovingMode,
                            modifier = Modifier.weight(1f),
                        )
                    }
'''.splitlines(keepends=True)

lines[346:459] = replacement
source = "".join(lines)

marker = "@Composable\nprivate fun HudMetric(\n"
assert source.count(marker) == 1
helper = '''@Composable
private fun GameplayStatusStrip(
    score: Int,
    best: Int,
    pressure: Int,
    overdriveRemaining: Int,
    daily: DailyChallenge?,
    dailySatisfied: Boolean,
) {
    val active = overdriveRemaining > 0
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Recess.copy(alpha = 0.48f))
            .border(
                1.dp,
                if (active) TealGlow.copy(alpha = 0.60f) else BrassDark.copy(alpha = 0.30f),
                shape,
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HudMetric(
                label = "СЧЁТ",
                value = score.toString(),
                accent = BrassBright,
                modifier = Modifier.weight(1f),
            )
            HudMetric(
                label = "ЛУЧШИЙ",
                value = best.toString(),
                accent = TextWarm,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(5.dp))
        PressureStrip(
            pressure = pressure,
            overdriveRemaining = overdriveRemaining,
        )
        if (daily != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                dailyGoalText(daily) + if (dailySatisfied) " · выполнено" else "",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = if (dailySatisfied) TealGlow else TextWarm,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HudMetric(
'''
source = source.replace(marker, helper, 1)
path.write_text(source)
