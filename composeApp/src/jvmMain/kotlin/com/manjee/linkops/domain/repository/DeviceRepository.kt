package com.manjee.linkops.domain.repository

import com.manjee.linkops.domain.model.Device
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for device operations
 */
interface DeviceRepository {
    /**
     * Observes connected devices in real-time
     * Emits new list when devices connect/disconnect
     * @return Flow of device list
     */
    fun observeDevices(): Flow<List<Device>>

    /**
     * Gets a specific device by serial number
     * @param serialNumber Device serial number
     * @return Result with device or error
     */
    suspend fun getDevice(serialNumber: String): Result<Device>

    /**
     * Checks if ADB server is available
     * @return true if ADB is available
     */
    suspend fun isAdbAvailable(): Boolean
}
