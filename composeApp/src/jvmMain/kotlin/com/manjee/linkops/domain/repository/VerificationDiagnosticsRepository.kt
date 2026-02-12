package com.manjee.linkops.domain.repository

import com.manjee.linkops.domain.model.VerificationDiagnostics

/**
 * Repository for performing deep verification diagnostics
 */
interface VerificationDiagnosticsRepository {
    /**
     * Analyze verification status for all domains of a package on a device
     *
     * Performs:
     * 1. Fetches domain verification states from the device
     * 2. Extracts local APK certificate fingerprint
     * 3. Fetches and compares assetlinks.json for each domain
     * 4. Identifies failure root causes and generates suggestions
     *
     * @param deviceSerial Device serial number
     * @param packageName Android package name to analyze
     * @return Complete diagnostics result or error
     */
    suspend fun analyzeVerification(
        deviceSerial: String,
        packageName: String
    ): Result<VerificationDiagnostics>
}
