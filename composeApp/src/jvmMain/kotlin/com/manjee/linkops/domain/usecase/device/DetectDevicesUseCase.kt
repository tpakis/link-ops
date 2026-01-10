package com.manjee.linkops.domain.usecase.device

import com.manjee.linkops.domain.model.Device
import com.manjee.linkops.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase for detecting and observing connected devices
 */
class DetectDevicesUseCase(
    private val deviceRepository: DeviceRepository
) {
    /**
     * Observes connected devices in real-time
     * @return Flow of device list that emits on connection changes
     */
    operator fun invoke(): Flow<List<Device>> {
        return deviceRepository.observeDevices()
    }
}
