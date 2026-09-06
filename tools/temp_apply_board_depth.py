from pathlib import Path

path = Path("app/src/main/java/com/steamforge/game/ui/game/GameScreen.kt")
source = path.read_text()

old_board = '''        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(10.dp, shape, ambientColor = Color.Black.copy(alpha = 0.40f), spotColor = Color.Black.copy(alpha = 0.56f))
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Panel.copy(alpha = 0.94f),
                            Color(0xFF131C24),
                            Color(0xFF0D151B),
                            Recess,
                        ),
                    ),
                )
                .border(1.dp, Brass.copy(alpha = 0.48f), shape)
                .swipeDetector(onSwipe),
        ) {
            for (r in 0 until state.size) {
                for (c in 0 until state.size) {
                    val off = cellOffset(r, c)
                    val cellShape = RoundedCornerShape(11.dp)
                    Box(
                        modifier = Modifier
                            .offset(off.x, off.y)
                            .size(cell)
                            .clip(cellShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF0A1015),
                                        Recess.copy(alpha = 0.98f),
                                        Color(0xFF101820),
                                    ),
                                ),
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.045f), cellShape),
                    )
                }
            }
'''

new_board = '''        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(12.dp, shape, ambientColor = Color.Black.copy(alpha = 0.44f), spotColor = Color.Black.copy(alpha = 0.60f))
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1A252D),
                            Panel.copy(alpha = 0.90f),
                            Color(0xFF0C141A),
                            Recess,
                        ),
                    ),
                )
                .border(1.dp, BrassDark.copy(alpha = 0.78f), shape)
                .swipeDetector(onSwipe),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val edge = 1.5.dp.toPx()
                val shellLight = Color.White.copy(alpha = 0.035f)
                val shellShadow = Color.Black.copy(alpha = 0.40f)
                drawLine(shellLight, Offset(edge, edge), Offset(size.width - edge, edge), 1.dp.toPx())
                drawLine(shellLight, Offset(edge, edge), Offset(edge, size.height - edge), 1.dp.toPx())
                drawLine(shellShadow, Offset(edge, size.height - edge), Offset(size.width - edge, size.height - edge), 1.dp.toPx())
                drawLine(shellShadow, Offset(size.width - edge, edge), Offset(size.width - edge, size.height - edge), 1.dp.toPx())
            }

            for (r in 0 until state.size) {
                for (c in 0 until state.size) {
                    val off = cellOffset(r, c)
                    val cellShape = RoundedCornerShape(11.dp)
                    Box(
                        modifier = Modifier
                            .offset(off.x, off.y)
                            .size(cell)
                            .clip(cellShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF071016),
                                        Color(0xFF0A131A),
                                        Color(0xFF111B22),
                                    ),
                                ),
                            )
                            .border(1.dp, Color(0xFF29363F).copy(alpha = 0.42f), cellShape),
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val edge = 1.dp.toPx()
                            val recessShadow = Color.Black.copy(alpha = 0.42f)
                            val recessLight = Color.White.copy(alpha = 0.035f)
                            drawLine(recessShadow, Offset(edge, edge), Offset(size.width - edge, edge), 1.dp.toPx())
                            drawLine(recessShadow, Offset(edge, edge), Offset(edge, size.height - edge), 1.dp.toPx())
                            drawLine(recessLight, Offset(edge, size.height - edge), Offset(size.width - edge, size.height - edge), 1.dp.toPx())
                            drawLine(recessLight, Offset(size.width - edge, edge), Offset(size.width - edge, size.height - edge), 1.dp.toPx())
                        }
                    }
                }
            }
'''

old_tile = '''            val inset = 6.dp.toPx()
            val bevelHighlight = Color.White.copy(alpha = if (tile.level <= 2) 0.18f else 0.11f)
            val bevelShadow = Color.Black.copy(alpha = 0.24f)

            if (tile.level >= 9) {
                drawCircle(
                    TealGlow.copy(alpha = if (colors.glow) 0.11f else 0.045f),
                    radius = size.minDimension * 0.31f,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
            }

            drawLine(bevelHighlight, Offset(inset, inset), Offset(size.width - inset, inset), 1.dp.toPx())
            drawLine(bevelShadow, Offset(inset, size.height - inset), Offset(size.width - inset, size.height - inset), 1.dp.toPx())
'''

new_tile = '''            val inset = 6.dp.toPx()
            val bevelHighlight = Color.White.copy(alpha = if (tile.level <= 2) 0.14f else 0.10f)
            val bevelSide = Color.White.copy(alpha = 0.045f)
            val bevelShadow = Color.Black.copy(alpha = 0.28f)

            if (tile.level >= 9) {
                drawCircle(
                    TealGlow.copy(alpha = if (colors.glow) 0.11f else 0.045f),
                    radius = size.minDimension * 0.31f,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
            }

            drawLine(bevelHighlight, Offset(inset, inset), Offset(size.width - inset, inset), 1.dp.toPx())
            drawLine(bevelSide, Offset(inset, inset), Offset(inset, size.height - inset), 1.dp.toPx())
            drawLine(bevelShadow, Offset(inset, size.height - inset), Offset(size.width - inset, size.height - inset), 1.dp.toPx())
            drawLine(bevelShadow.copy(alpha = 0.18f), Offset(size.width - inset, inset), Offset(size.width - inset, size.height - inset), 1.dp.toPx())
'''

for label, old, new in (("board", old_board, new_board), ("tile", old_tile, new_tile)):
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one {label} anchor, found {count}")
    source = source.replace(old, new)

path.write_text(source)
