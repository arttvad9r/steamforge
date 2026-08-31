package com.steamforge.game.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.steamforge.game.theme.BrassBright

/**
 * Steamforge-styled modal shell used for destructive and privacy decisions.
 * Keeps modal UI in the same visual language as the rest of the game instead
 * of falling back to a generic Material surface.
 */
@Composable
fun SteamDecisionDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        SteamPanel(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            highlighted = true,
            contentPadding = PaddingValues(18.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall,
                color = BrassBright,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            body()
            Spacer(Modifier.height(18.dp))
            actions()
        }
    }
}
