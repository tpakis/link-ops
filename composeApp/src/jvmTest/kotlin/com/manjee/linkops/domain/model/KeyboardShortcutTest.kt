package com.manjee.linkops.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class KeyboardShortcutTest {

    @Test
    fun `KeyboardShortcut data class should support equality`() {
        val shortcut1 = KeyboardShortcut(
            action = ShortcutAction.REFRESH_DEVICES,
            displayKeys = "Ctrl+R",
            description = "Refresh devices"
        )
        val shortcut2 = KeyboardShortcut(
            action = ShortcutAction.REFRESH_DEVICES,
            displayKeys = "Ctrl+R",
            description = "Refresh devices"
        )
        assertEquals(shortcut1, shortcut2)
    }

    @Test
    fun `KeyboardShortcut data class should differ by action`() {
        val shortcut1 = KeyboardShortcut(
            action = ShortcutAction.REFRESH_DEVICES,
            displayKeys = "Ctrl+R",
            description = "Refresh devices"
        )
        val shortcut2 = KeyboardShortcut(
            action = ShortcutAction.FOCUS_SEARCH,
            displayKeys = "Ctrl+R",
            description = "Refresh devices"
        )
        assertNotEquals(shortcut1, shortcut2)
    }

    @Test
    fun `KeyboardShortcut copy should work correctly`() {
        val original = KeyboardShortcut(
            action = ShortcutAction.REFRESH_DEVICES,
            displayKeys = "Ctrl+R",
            description = "Refresh devices"
        )
        val copy = original.copy(displayKeys = "Cmd+R")
        assertEquals(ShortcutAction.REFRESH_DEVICES, copy.action)
        assertEquals("Cmd+R", copy.displayKeys)
        assertEquals("Refresh devices", copy.description)
    }

    @Test
    fun `ShortcutAction should have four values`() {
        assertEquals(4, ShortcutAction.entries.size)
    }

    @Test
    fun `ShortcutAction should contain all expected values`() {
        val expectedValues = setOf(
            ShortcutAction.REFRESH_DEVICES,
            ShortcutAction.FOCUS_SEARCH,
            ShortcutAction.CLOSE_PANEL,
            ShortcutAction.SHOW_SHORTCUTS_HELP
        )
        assertEquals(expectedValues, ShortcutAction.entries.toSet())
    }
}
