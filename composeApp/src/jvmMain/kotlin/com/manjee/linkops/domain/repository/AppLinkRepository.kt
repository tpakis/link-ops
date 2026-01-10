package com.manjee.linkops.domain.repository

import com.manjee.linkops.domain.model.AppLink
import com.manjee.linkops.domain.model.IntentConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for app link operations
 */
interface AppLinkRepository {
    /**
     * Gets app links for a device
     * @param deviceSerial Device serial number
     * @return Result with list of app links
     */
    suspend fun getAppLinks(deviceSerial: String): Result<List<AppLink>>

    /**
     * Fires an intent on the device
     * @param deviceSerial Device serial number
     * @param config Intent configuration
     * @return Flow of stdout/stderr output
     */
    fun fireIntent(
        deviceSerial: String,
        config: IntentConfig
    ): Flow<String>

    /**
     * Forces re-verification of app links
     * @param deviceSerial Device serial number
     * @param packageName Package to re-verify
     * @return Result indicating success or failure
     */
    suspend fun forceReverify(
        deviceSerial: String,
        packageName: String
    ): Result<Unit>

    /**
     * Observes verification logs from logcat
     * @param deviceSerial Device serial number
     * @return Flow of log lines
     */
    fun observeVerificationLogs(deviceSerial: String): Flow<String>
}
