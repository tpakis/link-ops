package com.manjee.linkops.domain.usecase.applink

import com.manjee.linkops.domain.model.AppLink
import com.manjee.linkops.domain.repository.AppLinkRepository

/**
 * UseCase for getting app links from a device
 */
class GetAppLinksUseCase(
    private val appLinkRepository: AppLinkRepository
) {
    /**
     * Gets all app links configured on the device
     * @param deviceSerial Device serial number
     * @return Result with list of app links
     */
    suspend operator fun invoke(deviceSerial: String): Result<List<AppLink>> {
        return appLinkRepository.getAppLinks(deviceSerial)
    }
}
