package com.manjee.linkops.ui.component

import androidx.compose.ui.input.key.*
import com.manjee.linkops.domain.model.KeyboardShortcut
import com.manjee.linkops.domain.model.ShortcutAction

/**
 * Handles keyboard events and maps them to shortcut actions
 *
 * Supports platform-aware modifier keys (Ctrl on Windows/Linux, Meta/Cmd on macOS).
 */
class KeyboardShortcutHandler {

    /**
     * Determines the shortcut action for a given Compose key event
     *
     * @param event The Compose key event to evaluate
     * @return The matching shortcut action, or null if no shortcut matches
     */
    fun handleKeyEvent(event: KeyEvent): ShortcutAction? {
        if (event.type != KeyEventType.KeyDown) return null

        return resolve(
            key = event.key,
            isCtrlPressed = event.isCtrlPressed,
            isMetaPressed = event.isMetaPressed,
            isShiftPressed = event.isShiftPressed
        )
    }

    /**
     * Resolves a shortcut action from individual key state parameters
     *
     * @param key The key that was pressed
     * @param isCtrlPressed Whether Ctrl is held
     * @param isMetaPressed Whether Meta/Cmd is held
     * @param isShiftPressed Whether Shift is held
     * @return The matching shortcut action, or null if no shortcut matches
     */
    fun resolve(
        key: Key,
        isCtrlPressed: Boolean,
        isMetaPressed: Boolean,
        isShiftPressed: Boolean
    ): ShortcutAction? {
        val hasModifier = isCtrlPressed || isMetaPressed

        return when {
            hasModifier && key == Key.R -> ShortcutAction.REFRESH_DEVICES
            hasModifier && key == Key.F -> ShortcutAction.FOCUS_SEARCH
            key == Key.Escape -> ShortcutAction.CLOSE_PANEL
            key == Key.Slash && isShiftPressed && !isCtrlPressed && !isMetaPressed -> ShortcutAction.SHOW_SHORTCUTS_HELP
            else -> null
        }
    }

    companion object {
        private val isMac = System.getProperty("os.name")
            .lowercase()
            .contains("mac")

        private val modifierDisplay = if (isMac) "Cmd" else "Ctrl"

        /**
         * Returns all registered keyboard shortcuts with platform-aware display keys
         *
         * @return List of all available keyboard shortcuts
         */
        fun allShortcuts(): List<KeyboardShortcut> = listOf(
            KeyboardShortcut(
                action = ShortcutAction.REFRESH_DEVICES,
                displayKeys = "$modifierDisplay+R",
                description = "Refresh devices"
            ),
            KeyboardShortcut(
                action = ShortcutAction.FOCUS_SEARCH,
                displayKeys = "$modifierDisplay+F",
                description = "Focus search field"
            ),
            KeyboardShortcut(
                action = ShortcutAction.CLOSE_PANEL,
                displayKeys = "Escape",
                description = "Close side panel"
            ),
            KeyboardShortcut(
                action = ShortcutAction.SHOW_SHORTCUTS_HELP,
                displayKeys = "?",
                description = "Show keyboard shortcuts"
            )
        )
    }
}
