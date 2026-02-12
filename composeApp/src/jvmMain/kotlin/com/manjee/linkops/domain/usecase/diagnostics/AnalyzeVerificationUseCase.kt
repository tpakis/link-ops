package com.manjee.linkops.domain.usecase.diagnostics

import com.manjee.linkops.domain.model.VerificationDiagnostics
import com.manjee.linkops.domain.repository.VerificationDiagnosticsRepository

/**
 * Use case for performing deep verification analysis on a package
 *
 * @param verificationDiagnosticsRepository Repository for diagnostics operations
 */
class AnalyzeVerificationUseCase(
    private val verificationDiagnosticsRepository: VerificationDiagnosticsRepository
) {
    /**
     * Analyze verification status for a package on a device
     *
     * @param deviceSerial Device serial number
     * @param packageName Android package name to analyze
     * @return Complete diagnostics result
     */
    suspend operator fun invoke(
        deviceSerial: String,
        packageName: String
    ): Result<VerificationDiagnostics> {
        return verificationDiagnosticsRepository.analyzeVerification(deviceSerial, packageName)
    }
}
