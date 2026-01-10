package com.manjee.linkops.domain.usecase.diagnostics

import com.manjee.linkops.domain.model.AssetLinksValidation
import com.manjee.linkops.domain.repository.AssetLinksRepository

/**
 * Use case for validating assetlinks.json files
 */
class ValidateAssetLinksUseCase(
    private val assetLinksRepository: AssetLinksRepository
) {
    /**
     * Validate assetlinks.json for a domain
     *
     * @param domain The domain to validate (e.g., "example.com")
     * @return Validation result
     */
    suspend operator fun invoke(domain: String): Result<AssetLinksValidation> {
        // Normalize domain (remove protocol, trailing slash, etc.)
        val normalizedDomain = normalizeDomain(domain)
        return assetLinksRepository.validateAssetLinks(normalizedDomain)
    }

    private fun normalizeDomain(domain: String): String {
        return domain
            .removePrefix("https://")
            .removePrefix("http://")
            .removeSuffix("/")
            .removeSuffix("/.well-known/assetlinks.json")
            .trim()
    }
}
