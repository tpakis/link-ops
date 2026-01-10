package com.manjee.linkops.data.repository

import com.manjee.linkops.data.parser.DumpsysParser
import com.manjee.linkops.data.parser.GetAppLinksParser
import com.manjee.linkops.data.strategy.AdbCommandStrategyFactory
import com.manjee.linkops.domain.model.AppLink
import com.manjee.linkops.domain.model.IntentConfig
import com.manjee.linkops.domain.repository.AppLinkRepository
import com.manjee.linkops.infrastructure.adb.AdbShellExecutor
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of AppLinkRepository using ADB commands
 */
class AppLinkRepositoryImpl(
    private val adbExecutor: AdbShellExecutor,
    private val strategyFactory: AdbCommandStrategyFactory,
    private val getAppLinksParser: GetAppLinksParser,
    private val dumpsysParser: DumpsysParser
) : AppLinkRepository {

    companion object {
        private const val ANDROID_12_SDK_LEVEL = 31
    }

    override suspend fun getAppLinks(deviceSerial: String): Result<List<AppLink>> {
        // 1. Get device SDK level
        val sdkLevel = getSdkLevel(deviceSerial)
            .getOrElse { return Result.failure(it) }

        // 2. Select appropriate strategy
        val strategy = strategyFactory.create(sdkLevel)
        val command = strategy.getAppLinksCommand(packageName = null)

        // 3. Execute command and parse output
        return adbExecutor.executeOnDevice(deviceSerial, command)
            .mapCatching { output ->
                if (sdkLevel >= ANDROID_12_SDK_LEVEL) {
                    getAppLinksParser.parse(output)
                } else {
                    dumpsysParser.parse(output)
                }
            }
    }

    override fun fireIntent(
        deviceSerial: String,
        config: IntentConfig
    ): Flow<String> {
        val command = config.toAdbCommand()
        return adbExecutor.executeStreamOnDevice(deviceSerial, command)
    }

    override suspend fun forceReverify(
        deviceSerial: String,
        packageName: String
    ): Result<Unit> {
        // 1. Get device SDK level
        val sdkLevel = getSdkLevel(deviceSerial)
            .getOrElse { return Result.failure(it) }

        // 2. Select appropriate strategy
        val strategy = strategyFactory.create(sdkLevel)
        val command = strategy.forceReverifyCommand(packageName)

        // 3. Execute re-verification command
        return adbExecutor.executeOnDevice(deviceSerial, command)
            .map { } // Convert Result<String> to Result<Unit>
    }

    override fun observeVerificationLogs(deviceSerial: String): Flow<String> {
        // Use logcat with relevant filters
        val logcatCommand = "logcat -v time IntentFilterIntentOp:V DomainVerification:V *:S"
        return adbExecutor.executeStreamOnDevice(deviceSerial, logcatCommand)
    }

    private suspend fun getSdkLevel(deviceSerial: String): Result<Int> {
        return adbExecutor
            .executeOnDevice(deviceSerial, "getprop ro.build.version.sdk")
            .mapCatching { output ->
                output.trim().toIntOrNull()
                    ?: throw IllegalStateException("Failed to parse SDK level: $output")
            }
    }
}
