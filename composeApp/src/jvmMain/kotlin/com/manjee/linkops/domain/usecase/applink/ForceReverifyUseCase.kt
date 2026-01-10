package com.manjee.linkops.domain.usecase.applink

import com.manjee.linkops.domain.repository.AppLinkRepository

/**
 * UseCase for forcing re-verification of app links
 */
class ForceReverifyUseCase(
    private val appLinkRepository: AppLinkRepository
) {
    /**
     * Forces re-verification of app links for a package
     * @param deviceSerial Device serial number
     * @param packageName Package name to re-verify
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        deviceSerial: String,
        packageName: String
    ): Result<Unit> {
        return appLinkRepository.forceReverify(deviceSerial, packageName)
    }
}
