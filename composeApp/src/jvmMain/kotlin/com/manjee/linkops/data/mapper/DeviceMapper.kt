package com.manjee.linkops.data.mapper

import com.manjee.linkops.domain.model.Device

/**
 * Maps ADB output to Device domain model
 */
class DeviceMapper {
    /**
     * Parses `adb devices -l` output
     *
     * Example output:
     * ```
     * List of devices attached
     * emulator-5554          device product:sdk_gphone64_arm64 model:sdk_gphone64_arm64 device:emu64a transport_id:1
     * 192.168.1.100:5555     device product:OnePlus7Pro model:GM1917 device:OnePlus7Pro transport_id:2
     * ABCD1234               unauthorized
     * ```
     */
    fun parseDeviceList(output: String): List<Device> {
        return output.lines()
            .drop(1) // Skip "List of devices attached" header
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                try {
                    parseDeviceLine(line)
                } catch (e: Exception) {
                    null
                }
            }
    }

    private fun parseDeviceLine(line: String): Device {
        val parts = line.split(Regex("\\s+"))
        val serialNumber = parts[0]
        val stateString = parts.getOrNull(1) ?: "unknown"
        val state = parseDeviceState(stateString)

        // Parse attributes (model:xxx product:xxx format)
        val attributes = parts.drop(2)
            .filter { it.contains(":") }
            .associate { attr ->
                val colonIndex = attr.indexOf(":")
                val key = attr.substring(0, colonIndex)
                val value = attr.substring(colonIndex + 1)
                key to value
            }

        val model = attributes["model"] ?: "Unknown"
        val connectionType = detectConnectionType(serialNumber)

        return Device(
            serialNumber = serialNumber,
            model = model,
            osVersion = "Unknown", // Requires separate adb command to fetch
            sdkLevel = 0,          // Requires separate adb command to fetch
            connectionType = connectionType,
            state = state
        )
    }

    private fun parseDeviceState(state: String): Device.DeviceState {
        return when (state.lowercase()) {
            "device" -> Device.DeviceState.ONLINE
            "offline" -> Device.DeviceState.OFFLINE
            "unauthorized" -> Device.DeviceState.UNAUTHORIZED
            else -> Device.DeviceState.UNKNOWN
        }
    }

    private fun detectConnectionType(serialNumber: String): Device.ConnectionType {
        return when {
            serialNumber.startsWith("emulator") -> Device.ConnectionType.EMULATOR
            serialNumber.contains(":") -> Device.ConnectionType.WIFI
            else -> Device.ConnectionType.USB
        }
    }

    /**
     * Parses device properties from `adb shell getprop` output
     *
     * @param osVersionOutput Output of `getprop ro.build.version.release`
     * @param sdkLevelOutput Output of `getprop ro.build.version.sdk`
     */
    fun parseDeviceProperties(
        device: Device,
        osVersionOutput: String,
        sdkLevelOutput: String
    ): Device {
        return device.copy(
            osVersion = osVersionOutput.trim().ifEmpty { "Unknown" },
            sdkLevel = sdkLevelOutput.trim().toIntOrNull() ?: 0
        )
    }
}
