package com.manjee.linkops.domain.model

/**
 * Represents an Android device connected via ADB
 */
data class Device(
    val serialNumber: String,
    val model: String,
    val osVersion: String,
    val sdkLevel: Int,
    val connectionType: ConnectionType,
    val state: DeviceState
) {
    enum class ConnectionType {
        USB, WIFI, EMULATOR
    }

    enum class DeviceState {
        ONLINE, OFFLINE, UNAUTHORIZED, UNKNOWN
    }

    val displayName: String
        get() = "$model (Android $osVersion)"

    val isAvailable: Boolean
        get() = state == DeviceState.ONLINE
}
