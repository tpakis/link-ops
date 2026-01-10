package com.manjee.linkops.infrastructure.adb

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for AdbBinaryManager
 *
 * Note: Some tests require ADB to be installed on the system.
 * Tests are designed to pass even if ADB is not installed.
 */
class AdbBinaryManagerTest {

    private val adbManager = AdbBinaryManager()

    @Test
    fun `currentOs should return valid OS type`() {
        val os = adbManager.currentOs
        assertTrue(
            os in listOf(
                AdbBinaryManager.OsType.MAC,
                AdbBinaryManager.OsType.WINDOWS,
                AdbBinaryManager.OsType.LINUX,
                AdbBinaryManager.OsType.UNKNOWN
            ),
            "OS type should be one of MAC, WINDOWS, LINUX, or UNKNOWN"
        )
    }

    @Test
    fun `isAdbAvailable and needsDownload should be mutually exclusive`() {
        val isAvailable = adbManager.isAdbAvailable()
        val needsDownload = adbManager.needsDownload()

        assertTrue(
            isAvailable != needsDownload,
            "isAdbAvailable ($isAvailable) and needsDownload ($needsDownload) should be opposite"
        )
    }

    @Test
    fun `getAdbPath should return path when ADB is available`() {
        if (adbManager.isAdbAvailable()) {
            val path = adbManager.getAdbPath()
            assertNotNull(path, "ADB path should not be null when available")
            assertTrue(path.isNotEmpty(), "ADB path should not be empty")
            assertTrue(
                path.contains("adb"),
                "ADB path should contain 'adb': $path"
            )
        }
    }

    @Test
    fun `getAdbPath should return null when ADB is not available`() {
        if (adbManager.needsDownload()) {
            val path = adbManager.getAdbPath()
            assertTrue(
                path == null,
                "ADB path should be null when not available"
            )
        }
    }

    @Test
    fun `currentOs should match system property`() {
        val osName = System.getProperty("os.name").lowercase()
        val currentOs = adbManager.currentOs

        when {
            osName.contains("mac") -> assertTrue(
                currentOs == AdbBinaryManager.OsType.MAC,
                "Expected MAC but got $currentOs"
            )
            osName.contains("win") -> assertTrue(
                currentOs == AdbBinaryManager.OsType.WINDOWS,
                "Expected WINDOWS but got $currentOs"
            )
            osName.contains("linux") || osName.contains("nux") -> assertTrue(
                currentOs == AdbBinaryManager.OsType.LINUX,
                "Expected LINUX but got $currentOs"
            )
        }
    }
}
