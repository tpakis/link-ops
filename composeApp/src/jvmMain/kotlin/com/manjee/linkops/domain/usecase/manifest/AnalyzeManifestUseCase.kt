package com.manjee.linkops.domain.usecase.manifest

import com.manjee.linkops.domain.model.ManifestAnalysisResult
import com.manjee.linkops.domain.repository.ManifestRepository
import com.manjee.linkops.domain.repository.PackageFilter

/**
 * Use case for analyzing Android manifest and extracting deep link information
 */
class AnalyzeManifestUseCase(
    private val manifestRepository: ManifestRepository
) {
    /**
     * Analyze manifest for an installed app
     *
     * @param deviceSerial Serial number of the device
     * @param packageName Package name of the app
     * @return Analysis result with deep links
     */
    suspend operator fun invoke(
        deviceSerial: String,
        packageName: String
    ): Result<ManifestAnalysisResult> {
        return manifestRepository.getManifestFromDevice(deviceSerial, packageName)
    }
}

/**
 * Use case for getting installed packages
 */
class GetInstalledPackagesUseCase(
    private val manifestRepository: ManifestRepository
) {
    /**
     * Get list of installed packages on a device
     *
     * @param deviceSerial Serial number of the device
     * @param filter Package filter (ALL, THIRD_PARTY, SYSTEM)
     * @return List of package names
     */
    suspend operator fun invoke(
        deviceSerial: String,
        filter: PackageFilter = PackageFilter.THIRD_PARTY
    ): Result<List<String>> {
        return manifestRepository.getInstalledPackages(deviceSerial, filter)
    }
}

/**
 * Use case for searching packages
 */
class SearchPackagesUseCase(
    private val manifestRepository: ManifestRepository
) {
    /**
     * Search for packages by name
     *
     * @param deviceSerial Serial number of the device
     * @param query Search query
     * @return List of matching package names
     */
    suspend operator fun invoke(
        deviceSerial: String,
        query: String
    ): Result<List<String>> {
        if (query.isBlank()) {
            return Result.success(emptyList())
        }
        return manifestRepository.searchPackages(deviceSerial, query)
    }
}

/**
 * Use case for testing a deep link
 */
class TestDeepLinkUseCase(
    private val manifestRepository: ManifestRepository
) {
    /**
     * Test a deep link by firing an intent on the device
     *
     * @param deviceSerial Serial number of the device
     * @param uri URI to open
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(
        deviceSerial: String,
        uri: String
    ): Result<String> {
        return manifestRepository.testDeepLink(deviceSerial, uri)
    }
}
