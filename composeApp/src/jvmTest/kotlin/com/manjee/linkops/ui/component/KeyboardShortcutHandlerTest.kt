package com.manjee.linkops.ui.component

import androidx.compose.ui.input.key.Key
import com.manjee.linkops.domain.model.ShortcutAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class KeyboardShortcutHandlerTest {

    private val handler = KeyboardShortcutHandler()

    // --- Ctrl+R -> REFRESH_DEVICES ---

    @Test
    fun `Ctrl+R should return REFRESH_DEVICES`() {
        val result = handler.resolve(
            key = Key.R,
            isCtrlPressed = true,
            isMetaPressed = false,
            isShiftPressed = false
        )
        assertEquals(ShortcutAction.REFRESH_DEVICES, result)
    }

    @Test
    fun `Meta+R should return REFRESH_DEVICES`() {
        val result = handler.resolve(
            key = Key.R,
            isCtrlPressed = false,
            isMetaPressed = true,
            isShiftPressed = false
        )
        assertEquals(ShortcutAction.REFRESH_DEVICES, result)
    }

    @Test
    fun `Ctrl+Meta+R should return REFRESH_DEVICES`() {
        val result = handler.resolve(
            key = Key.R,
            isCtrlPressed = true,
            isMetaPressed = true,
            isShiftPressed = false
        )
        assertEquals(ShortcutAction.REFRESH_DEVICES, result)
    }

    // --- Ctrl+F -> FOCUS_SEARCH ---

    @Test
    fun `Ctrl+F should return FOCUS_SEARCH`() {
        val result = handler.resolve(
            key = Key.F,
            isCtrlPressed = true,
            isMetaPressed = false,
            isShiftPressed = false
        )
        assertEquals(ShortcutAction.FOCUS_SEARCH, result)
    }

    @Test
    fun `Meta+F should return FOCUS_SEARCH`() {
        val result = handler.resolve(
            key = Key.F,
            isCtrlPressed = false,
            isMetaPressed = true,
            isShiftPressed = false
        )
        assertEquals(ShortcutAction.FOCUS_SEARCH, result)
    }

    // --- Escape -> CLOSE_PANEL ---

    @Test
    fun `Escape should return CLOSE_PANEL`() {
        val result = handler.resolve(
            key = Key.Escape,
            isCtrlPressed = false,
            isMetaPressed = false,
            isShiftPressed = false
        )
        assertEquals(ShortcutAction.CLOSE_PANEL, result)
    }

    @Test
    fun `Escape with Ctrl should still return CLOSE_PANEL`() {
        val result = handler.resolve(
            key = Key.Escape,
            isCtrlPressed = true,
            isMetaPressed = false,
            isShiftPressed = false
        )
        assertEquals(ShortcutAction.CLOSE_PANEL, result)
    }

    // --- Shift+/ (?) -> SHOW_SHORTCUTS_HELP ---

    @Test
    fun `Shift+Slash should return SHOW_SHORTCUTS_HELP`() {
        val result = handler.resolve(
            key = Key.Slash,
            isCtrlPressed = false,
            isMetaPressed = false,
            isShiftPressed = true
        )
        assertEquals(ShortcutAction.SHOW_SHORTCUTS_HELP, result)
    }

    // --- Unbound keys should return null ---

    @Test
    fun `plain letter key should return null`() {
        val result = handler.resolve(
            key = Key.A,
            isCtrlPressed = false,
            isMetaPressed = false,
            isShiftPressed = false
        )
        assertNull(result)
    }

    @Test
    fun `R without modifier should return null`() {
        val result = handler.resolve(
            key = Key.R,
            isCtrlPressed = false,
            isMetaPressed = false,
            isShiftPressed = false
        )
        assertNull(result)
    }

    @Test
    fun `F without modifier should return null`() {
        val result = handler.resolve(
            key = Key.F,
            isCtrlPressed = false,
            isMetaPressed = false,
            isShiftPressed = false
        )
        assertNull(result)
    }

    @Test
    fun `Slash without Shift should return null`() {
        val result = handler.resolve(
            key = Key.Slash,
            isCtrlPressed = false,
            isMetaPressed = false,
            isShiftPressed = false
        )
        assertNull(result)
    }

    @Test
    fun `Ctrl+Shift+Slash should return null`() {
        val result = handler.resolve(
            key = Key.Slash,
            isCtrlPressed = true,
            isMetaPressed = false,
            isShiftPressed = true
        )
        assertNull(result)
    }

    @Test
    fun `Meta+Shift+Slash should return null`() {
        val result = handler.resolve(
            key = Key.Slash,
            isCtrlPressed = false,
            isMetaPressed = true,
            isShiftPressed = true
        )
        assertNull(result)
    }

    @Test
    fun `Ctrl+A should return null`() {
        val result = handler.resolve(
            key = Key.A,
            isCtrlPressed = true,
            isMetaPressed = false,
            isShiftPressed = false
        )
        assertNull(result)
    }

    @Test
    fun `Space key should return null`() {
        val result = handler.resolve(
            key = Key.Spacebar,
            isCtrlPressed = false,
            isMetaPressed = false,
            isShiftPressed = false
        )
        assertNull(result)
    }

    // --- allShortcuts() ---

    @Test
    fun `allShortcuts should return all four shortcuts`() {
        val shortcuts = KeyboardShortcutHandler.allShortcuts()
        assertEquals(4, shortcuts.size)
    }

    @Test
    fun `allShortcuts should cover all ShortcutAction values`() {
        val shortcuts = KeyboardShortcutHandler.allShortcuts()
        val actions = shortcuts.map { it.action }.toSet()
        assertEquals(ShortcutAction.entries.toSet(), actions)
    }

    @Test
    fun `allShortcuts descriptions should not be blank`() {
        val shortcuts = KeyboardShortcutHandler.allShortcuts()
        shortcuts.forEach { shortcut ->
            assertNotNull(shortcut.description)
            assert(shortcut.description.isNotBlank()) {
                "Description should not be blank for ${shortcut.action}"
            }
        }
    }

    @Test
    fun `allShortcuts display keys should not be blank`() {
        val shortcuts = KeyboardShortcutHandler.allShortcuts()
        shortcuts.forEach { shortcut ->
            assert(shortcut.displayKeys.isNotBlank()) {
                "Display keys should not be blank for ${shortcut.action}"
            }
        }
    }

    @Test
    fun `allShortcuts display keys should use Cmd or Ctrl prefix`() {
        val shortcuts = KeyboardShortcutHandler.allShortcuts()
        val modifiedShortcuts = shortcuts.filter { it.displayKeys.contains("+") }
        modifiedShortcuts.forEach { shortcut ->
            assert(shortcut.displayKeys.startsWith("Cmd") || shortcut.displayKeys.startsWith("Ctrl")) {
                "Modified shortcut should start with Cmd or Ctrl: ${shortcut.displayKeys}"
            }
        }
    }
}
