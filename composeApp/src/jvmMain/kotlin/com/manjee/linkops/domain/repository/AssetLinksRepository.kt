package com.manjee.linkops.domain.repository

import com.manjee.linkops.domain.model.AssetLinksValidation

/**
 * Repository for fetching and validating assetlinks.json files
 */
interface AssetLinksRepository {
    /**
     * Validate assetlinks.json for a domain
     *
     * @param domain The domain to validate (e.g., "example.com")
     * @return Validation result with parsed content and any issues found
     */
    suspend fun validateAssetLinks(domain: String): Result<AssetLinksValidation>

    /**
     * Check if a specific package name with fingerprint exists in assetlinks.json
     *
     * @param domain The domain to check
     * @param packageName The Android package name to look for
     * @param fingerprint The SHA-256 fingerprint to match (optional)
     * @return true if the package (and fingerprint if provided) exists
     */
    suspend fun checkPackageInAssetLinks(
        domain: String,
        packageName: String,
        fingerprint: String? = null
    ): Result<Boolean>

    /**
     * Fetch the raw assetlinks.json content without parsing
     *
     * @param domain The domain to fetch from
     * @return Raw JSON string or error
     */
    suspend fun fetchRawAssetLinks(domain: String): Result<String>
}
