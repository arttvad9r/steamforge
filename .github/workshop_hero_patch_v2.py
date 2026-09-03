from pathlib import Path

path = Path('app/src/main/java/com/steamforge/game/ui/workshop/WorkshopScreen.kt')
text = path.read_text()

screen_old = '''            WorkshopHero(
                level = ui.level,
                levelInfo = ui.levelInfo,
                animationsEnabled = ui.animationsEnabled,
                accent = accent,
            )
            Spacer(Modifier.height(12.dp))

            SteamButton(
                text = "ИГРАТЬ",
                icon = "▶",
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))

            WorkshopStatusRail(
                gamesPlayed = ui.gamesPlayed,
                bestScore = ui.bestScore,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
'''
screen_new = '''            WorkshopHero(
                level = ui.level,
                levelInfo = ui.levelInfo,
                animationsEnabled = ui.animationsEnabled,
                accent = accent,
                gamesPlayed = ui.gamesPlayed,
                bestScore = ui.bestScore,
            )
            Spacer(Modifier.height(10.dp))

            SteamButton(
                text = "ИГРАТЬ",
                icon = "▶",
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(14.dp))
'''

hero_start = text.index('@Composable\nprivate fun WorkshopHero(')
status_start = text.index('@Composable\nprivate fun WorkshopStatusRail(', hero_start)
inline_start = text.index('@Composable\nprivate fun InlineMetric(', status_start)

hero_new = '''@Composable
private fun WorkshopHero(
    level: Int,
    levelInfo: com.steamforge.game.progression.LevelInfo,
    animationsEnabled: Boolean,
    accent: Color,
    gamesPlayed: Int,
    bestScore: Int,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "УРОВЕНЬ МАСТЕРСКОЙ",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                )
                Text(
                    "МЕХАНИЧЕСКОЕ ЯДРО",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
            Text(
                level.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = TextWarm,
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(204.dp),
            contentAlignment = Alignment.Center,
        ) {
            WorkshopScene(animationsEnabled, accent)
            Box(
                Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Recess.copy(alpha = 0.90f))
                    .border(1.dp, accent.copy(alpha = 0.58f), RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "CORE",
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("XP", style = MaterialTheme.typography.labelLarge, color = accent)
            Spacer(Modifier.width(8.dp))
            GaugeBar(levelInfo.fraction, accent, Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Text(
                "${levelInfo.xpIntoLevel}/${levelInfo.xpToNext}",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
            )
        }

        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InlineMetric("ПАРТИЙ", gamesPlayed.toString(), Modifier.weight(1f))
            Box(
                Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(Color.White.copy(alpha = 0.07f)),
            )
            InlineMetric("РЕКОРД", bestScore.toString(), Modifier.weight(1f), BrassBright)
        }
    }
}

'''

assert text.count(screen_old) == 1
text = text.replace(screen_old, screen_new, 1)
# Replace WorkshopHero and remove the now-redundant standalone WorkshopStatusRail.
text = text[:hero_start] + hero_new + text[inline_start:]
path.write_text(text)
