package com.manjee.linkops.domain.model

/**
 * Represents a keyboard shortcut bound to an application action
 *
 * @param action The action this shortcut triggers
 * @param displayKeys Human-readable key combination (e.g., "Ctrl+R")
 * @param description Explanation of what the shortcut does
 */
data class KeyboardShortcut(
    val action: ShortcutAction,
    val displayKeys: String,
    val description: String
)

/**
 * Available keyboard shortcut actions
 */
enum class ShortcutAction {
    REFRESH_DEVICES,
    FOCUS_SEARCH,
    CLOSE_PANEL,
    SHOW_SHORTCUTS_HELP
}
