from pathlib import Path

path = Path('app/src/main/java/com/steamforge/game/ui/game/GameScreen.kt')
text = path.read_text()

game_over_old = '''        if (compactLandscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                SteamButton(
                    "СЫГРАТЬ СНОВА",
                    onRestart,
                    Modifier.weight(1.12f),
                    style = SteamButtonStyle.Teal,
                )
                SteamButton(
                    "В МАСТЕРСКУЮ",
                    onExit,
                    Modifier.weight(0.88f),
                    style = SteamButtonStyle.Dark,
                )
            }
        } else {
            SteamButton(
                "СЫГРАТЬ СНОВА",
                onRestart,
                Modifier.fillMaxWidth(),
                style = SteamButtonStyle.Teal,
            )
            Spacer(Modifier.height(7.dp))
            SteamButton(
                "В МАСТЕРСКУЮ",
                onExit,
                Modifier.fillMaxWidth(),
                style = SteamButtonStyle.Dark,
            )
        }
'''
game_over_new = '''        SteamButton(
            "СЫГРАТЬ СНОВА",
            onRestart,
            Modifier.fillMaxWidth(),
            style = SteamButtonStyle.Teal,
        )
        Spacer(Modifier.height(7.dp))
        SteamButton(
            "В МАСТЕРСКУЮ",
            onExit,
            Modifier.fillMaxWidth(),
            style = SteamButtonStyle.Dark,
        )
'''
core_old = '''        if (compactLandscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                SteamButton(
                    "ПРОДОЛЖИТЬ ИГРУ",
                    onContinue,
                    Modifier.weight(1.10f),
                    style = SteamButtonStyle.Teal,
                )
                SteamButton(
                    "В МАСТЕРСКУЮ",
                    onExit,
                    Modifier.weight(0.90f),
                    style = SteamButtonStyle.Dark,
                )
            }
        } else {
            SteamButton(
                "ПРОДОЛЖИТЬ ИГРУ",
                onContinue,
                Modifier.fillMaxWidth(),
                style = SteamButtonStyle.Teal,
            )
            Spacer(Modifier.height(7.dp))
            SteamButton(
                "В МАСТЕРСКУЮ",
                onExit,
                Modifier.fillMaxWidth(),
                style = SteamButtonStyle.Dark,
            )
        }
'''
core_new = '''        SteamButton(
            "ПРОДОЛЖИТЬ ИГРУ",
            onContinue,
            Modifier.fillMaxWidth(),
            style = SteamButtonStyle.Teal,
        )
        Spacer(Modifier.height(7.dp))
        SteamButton(
            "В МАСТЕРСКУЮ",
            onExit,
            Modifier.fillMaxWidth(),
            style = SteamButtonStyle.Dark,
        )
'''

assert text.count(game_over_old) == 1
assert text.count(core_old) == 1
text = text.replace(game_over_old, game_over_new, 1)
text = text.replace(core_old, core_new, 1)
path.write_text(text)
