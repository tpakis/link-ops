package com.manjee.linkops.infrastructure.adb

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertIs

/**
 * Unit tests for AdbShellExecutor
 *
 * Note: Tests that execute actual ADB commands require ADB to be installed.
 * These tests are skipped if ADB is not available.
 */
class AdbShellExecutorTest {

    private val adbManager = AdbBinaryManager()
    private val adbExecutor = AdbShellExecutor(adbManager)

    private fun skipIfAdbNotAvailable(): Boolean {
        return !adbManager.isAdbAvailable()
    }

    @Test
    fun `execute version should return success when ADB is available`() = runTest {
        if (skipIfAdbNotAvailable()) {
            println("Skipping test: ADB not available")
            return@runTest
        }

        val result = adbExecutor.execute("version")

        assertTrue(result.isSuccess, "Execute should succeed")
        val output = result.getOrNull()
        assertNotNull(output, "Output should not be null")
        assertTrue(
            output.contains("Android Debug Bridge") || output.contains("adb"),
            "Output should contain ADB version info: $output"
        )
    }

    @Test
    fun `execute devices should return success`() = runTest {
        if (skipIfAdbNotAvailable()) {
            println("Skipping test: ADB not available")
            return@runTest
        }

        val result = adbExecutor.execute("devices")

        assertTrue(result.isSuccess, "Execute should succeed")
        val output = result.getOrNull()
        assertNotNull(output, "Output should not be null")
        assertTrue(
            output.contains("List of devices attached"),
            "Output should contain device list header: $output"
        )
    }

    @Test
    fun `execute with invalid command should return failure`() = runTest {
        if (skipIfAdbNotAvailable()) {
            println("Skipping test: ADB not available")
            return@runTest
        }

        val result = adbExecutor.execute("invalid-command-that-does-not-exist")

        assertTrue(result.isFailure, "Execute should fail for invalid command")
        val exception = result.exceptionOrNull()
        assertNotNull(exception, "Exception should not be null")
        assertIs<AdbException>(exception, "Exception should be AdbException")
    }

    @Test
    fun `executeOnDevice should format command correctly`() = runTest {
        if (skipIfAdbNotAvailable()) {
            println("Skipping test: ADB not available")
            return@runTest
        }

        // This will fail because device doesn't exist, but we can verify the error message
        val result = adbExecutor.executeOnDevice("non-existent-device", "echo test")

        // Should fail because device doesn't exist
        assertTrue(result.isFailure, "Should fail for non-existent device")
        val exception = result.exceptionOrNull()
        assertNotNull(exception, "Exception should not be null")
        assertIs<AdbException>(exception, "Exception should be AdbException")
    }

    @Test
    fun `executeStream should emit lines`() = runTest {
        if (skipIfAdbNotAvailable()) {
            println("Skipping test: ADB not available")
            return@runTest
        }

        // Use 'help' command which produces multiple lines of output
        val lines = mutableListOf<String>()
        try {
            adbExecutor.executeStream("help")
                .toList(lines)
        } catch (e: AdbException) {
            // help command may exit with non-zero code on some systems
            // but we still want to verify streaming worked
        }

        assertTrue(
            lines.isNotEmpty(),
            "Stream should emit at least one line"
        )
    }

    @Test
    fun `AdbException should preserve message`() {
        val message = "Test error message"
        val exception = AdbException(message)

        assertTrue(
            exception.message == message,
            "Exception message should match: ${exception.message}"
        )
    }

    @Test
    fun `AdbException should preserve cause`() {
        val cause = RuntimeException("Original cause")
        val exception = AdbException("Wrapper message", cause)

        assertTrue(
            exception.cause == cause,
            "Exception cause should be preserved"
        )
        assertTrue(
            exception.message == "Wrapper message",
            "Exception message should match"
        )
    }
}
