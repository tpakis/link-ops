package com.manjee.linkops.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manjee.linkops.domain.model.KeyboardShortcut

/**
 * Dialog that displays all available keyboard shortcuts
 *
 * @param shortcuts List of shortcuts to display
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
fun ShortcutsHelpDialog(
    shortcuts: List<KeyboardShortcut>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Keyboard Shortcuts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shortcuts.forEach { shortcut ->
                    ShortcutRow(shortcut = shortcut)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * A single shortcut row showing the key combination and description
 */
@Composable
private fun ShortcutRow(shortcut: KeyboardShortcut) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = shortcut.description,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = shortcut.displayKeys,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
