package com.manjee.linkops.domain.usecase.applink

import com.manjee.linkops.domain.model.IntentConfig
import com.manjee.linkops.domain.repository.AppLinkRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase for firing intents on a device
 */
class FireIntentUseCase(
    private val appLinkRepository: AppLinkRepository
) {
    /**
     * Fires an intent on the specified device
     * @param deviceSerial Device serial number
     * @param config Intent configuration
     * @return Flow of command output (stdout/stderr)
     */
    operator fun invoke(
        deviceSerial: String,
        config: IntentConfig
    ): Flow<String> {
        return appLinkRepository.fireIntent(deviceSerial, config)
    }
}
