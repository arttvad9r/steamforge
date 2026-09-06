from pathlib import Path

path = Path("app/src/main/java/com/steamforge/game/ui/home/HomeScreen.kt")
source = path.read_text()

replacements = [
    (
        "import androidx.compose.foundation.shape.RoundedCornerShape\n",
        "import androidx.compose.foundation.shape.CircleShape\nimport androidx.compose.foundation.shape.RoundedCornerShape\n",
        "CircleShape import",
    ),
    (
        "            HomeCoreScene()\n",
        "            HomeCoreScene(expanded = !visibility.showWorkshop)\n",
        "HomeCoreScene call",
    ),
]

for old, new, label in replacements:
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one {label} anchor, found {count}")
    source = source.replace(old, new)

old_scene = '''@Composable
private fun HomeCoreScene() {
    val transition = rememberInfiniteTransition(label = "home-core")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
        label = "home-core-angle",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(184.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val unit = size.minDimension
            drawCircle(TealGlow.copy(alpha = 0.055f), unit * 0.48f, c)
            drawCircle(Brass.copy(alpha = 0.045f), unit * 0.37f, c)
            drawHomeGear(c, unit * 0.27f, angle, Brass.copy(alpha = 0.62f))
            drawHomeGear(
                Offset(size.width * 0.68f, size.height * 0.66f),
                unit * 0.105f,
                -angle * 1.4f,
                Copper.copy(alpha = 0.66f),
            )
            drawHomeGear(
                Offset(size.width * 0.34f, size.height * 0.39f),
                unit * 0.075f,
                angle * 1.8f,
                BrassDark.copy(alpha = 0.74f),
            )
            drawCircle(TealGlow.copy(alpha = 0.26f), unit * 0.18f, c, style = Stroke(3.dp.toPx()))
        }
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.radialGradient(listOf(TealSurface.copy(alpha = 0.74f), Recess)))
                .border(1.dp, Brass.copy(alpha = 0.58f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("2048", style = MaterialTheme.typography.headlineSmall, color = BrassBright)
                Text("CORE", style = MaterialTheme.typography.labelSmall, color = TealGlow)
            }
        }
    }
}
'''

new_scene = '''@Composable
private fun HomeCoreScene(expanded: Boolean) {
    val transition = rememberInfiniteTransition(label = "home-core")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
        label = "home-core-angle",
    )
    val sceneHeight = if (expanded) 310.dp else 184.dp
    val reactorSize = if (expanded) 132.dp else 96.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(sceneHeight),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val unit = size.minDimension
            drawCircle(TealGlow.copy(alpha = if (expanded) 0.045f else 0.040f), unit * 0.49f, c)
            drawCircle(Brass.copy(alpha = 0.040f), unit * 0.38f, c)
            drawHomeGear(c, unit * 0.27f, angle, Brass.copy(alpha = 0.54f))
            drawHomeGear(
                Offset(size.width * 0.69f, size.height * 0.65f),
                unit * 0.105f,
                -angle * 1.4f,
                Copper.copy(alpha = 0.58f),
            )
            drawHomeGear(
                Offset(size.width * 0.33f, size.height * 0.39f),
                unit * 0.075f,
                angle * 1.8f,
                BrassDark.copy(alpha = 0.68f),
            )
            drawCircle(BrassDark.copy(alpha = 0.72f), unit * 0.205f, c, style = Stroke(2.dp.toPx()))
            drawCircle(TealGlow.copy(alpha = 0.20f), unit * 0.176f, c, style = Stroke(2.dp.toPx()))
        }
        Box(
            modifier = Modifier
                .size(reactorSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            TealSurface.copy(alpha = 0.72f),
                            PanelRaised.copy(alpha = 0.94f),
                            Recess,
                        ),
                    ),
                )
                .border(2.dp, BrassDark.copy(alpha = 0.92f), CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(TealGlow.copy(alpha = 0.10f), Recess)))
                    .border(1.dp, Brass.copy(alpha = 0.66f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "2048",
                        style = if (expanded) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                        color = BrassBright,
                    )
                    Text("CORE", style = MaterialTheme.typography.labelSmall, color = TealGlow)
                }
            }
        }
    }
}
'''

count = source.count(old_scene)
if count != 1:
    raise SystemExit(f"Expected exactly one HomeCoreScene anchor, found {count}")
source = source.replace(old_scene, new_scene)

path.write_text(source)
