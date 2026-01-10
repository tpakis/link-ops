package com.manjee.linkops.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manjee.linkops.ui.theme.LinkOpsColors

/**
 * Terminal-like log panel for displaying command output
 *
 * @param logText The log text to display
 * @param title Panel title
 * @param onClear Callback when clear button is clicked
 * @param modifier Modifier for the panel
 */
@Composable
fun LogPanel(
    logText: String,
    title: String = "Log Output",
    onClear: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom when log updates
    LaunchedEffect(logText) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (onClear != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear log",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Log content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    LinkOpsColors.TerminalBackground,
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            SelectionContainer {
                Text(
                    text = logText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    color = LinkOpsColors.TerminalText,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                )
            }
        }
    }
}

/**
 * Compact log panel for inline display
 */
@Composable
fun LogPanelCompact(
    logText: String,
    maxLines: Int = 5,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                LinkOpsColors.TerminalBackground,
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        SelectionContainer {
            Text(
                text = logText.lines().takeLast(maxLines).joinToString("\n"),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                color = LinkOpsColors.TerminalText,
                maxLines = maxLines
            )
        }
    }
}

/**
 * Log entry with timestamp and level
 */
data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val message: String
)

enum class LogLevel {
    INFO, WARNING, ERROR, DEBUG
}

/**
 * Formatted log panel with colored log levels
 */
@Composable
fun FormattedLogPanel(
    entries: List<LogEntry>,
    title: String = "Log Output",
    onClear: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(entries.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (onClear != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear log",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    LinkOpsColors.TerminalBackground,
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                entries.forEach { entry ->
                    LogEntryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    val levelColor = when (entry.level) {
        LogLevel.INFO -> LinkOpsColors.TerminalInfo
        LogLevel.WARNING -> LinkOpsColors.TerminalWarning
        LogLevel.ERROR -> LinkOpsColors.TerminalError
        LogLevel.DEBUG -> LinkOpsColors.TerminalText.copy(alpha = 0.7f)
    }

    val levelTag = when (entry.level) {
        LogLevel.INFO -> "[INFO]"
        LogLevel.WARNING -> "[WARN]"
        LogLevel.ERROR -> "[ERROR]"
        LogLevel.DEBUG -> "[DEBUG]"
    }

    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = entry.timestamp,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            ),
            color = LinkOpsColors.TerminalText.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = levelTag,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            ),
            color = levelColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        SelectionContainer {
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                color = LinkOpsColors.TerminalText
            )
        }
    }
}
