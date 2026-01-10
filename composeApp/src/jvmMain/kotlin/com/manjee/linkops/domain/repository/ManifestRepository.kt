package com.manjee.linkops.domain.repository

import com.manjee.linkops.domain.model.DomainVerificationResult
import com.manjee.linkops.domain.model.ManifestAnalysisResult

/**
 * Repository for analyzing Android manifest and extracting deep link information
 */
interface ManifestRepository {
    /**
     * Get manifest info for an installed app on a device
     *
     * @param deviceSerial Serial number of the device
     * @param packageName Package name of the app
     * @return ManifestAnalysisResult with parsed manifest info
     */
    suspend fun getManifestFromDevice(
        deviceSerial: String,
        packageName: String
    ): Result<ManifestAnalysisResult>

    /**
     * Get domain verification status for an app
     *
     * @param deviceSerial Serial number of the device
     * @param packageName Package name of the app
     * @return DomainVerificationResult with verification status for each domain
     */
    suspend fun getDomainVerification(
        deviceSerial: String,
        packageName: String
    ): Result<DomainVerificationResult>

    /**
     * Test a deep link by firing an intent
     *
     * @param deviceSerial Serial number of the device
     * @param uri URI to open
     * @return Result indicating success or failure
     */
    suspend fun testDeepLink(
        deviceSerial: String,
        uri: String
    ): Result<String>

    /**
     * Get list of installed packages on a device
     *
     * @param deviceSerial Serial number of the device
     * @param filter Optional filter (e.g., "3" for third-party apps only)
     * @return List of package names
     */
    suspend fun getInstalledPackages(
        deviceSerial: String,
        filter: PackageFilter = PackageFilter.ALL
    ): Result<List<String>>

    /**
     * Search for packages by name
     *
     * @param deviceSerial Serial number of the device
     * @param query Search query (partial package name)
     * @return List of matching package names
     */
    suspend fun searchPackages(
        deviceSerial: String,
        query: String
    ): Result<List<String>>
}

/**
 * Filter for installed packages
 */
enum class PackageFilter {
    ALL,            // All packages
    THIRD_PARTY,    // Third-party apps only (-3 flag)
    SYSTEM          // System apps only (-s flag)
}
