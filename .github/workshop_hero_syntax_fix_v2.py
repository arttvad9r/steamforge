from pathlib import Path

path = Path('app/src/main/java/com/steamforge/game/ui/workshop/WorkshopScreen.kt')
text = path.read_text()

broken_hero = '''@Composable
private fun WorkshopHero(
    level: Int,
    levelInfo: com.steamforge.game.progression.LevelInfo,
    animationsEnabled: Boolean@Composable
private fun WorkshopHero(
'''
fixed_hero = '''@Composable
private fun WorkshopHero(
'''

broken_inline = '''}

 {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleMedium, color = accent, maxLines = 1)
    }
}

@Composable
private fun GaugeBar'''
fixed_inline = '''}

@Composable
private fun InlineMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = TextWarm,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleMedium, color = accent, maxLines = 1)
    }
}

@Composable
private fun GaugeBar'''

assert text.count(broken_hero) == 1
assert text.count(broken_inline) == 1
text = text.replace(broken_hero, fixed_hero, 1)
text = text.replace(broken_inline, fixed_inline, 1)
path.write_text(text)
