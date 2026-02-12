package com.manjee.linkops.domain.usecase.diagnostics

import com.manjee.linkops.domain.model.*
import com.manjee.linkops.domain.repository.VerificationDiagnosticsRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnalyzeVerificationUseCaseTest {

    @Test
    fun `invoke should return diagnostics on success`() = runTest {
        val expected = VerificationDiagnostics(
            packageName = "com.example.app",
            deviceSerial = "emulator-5554",
            domainResults = listOf(
                DomainDiagnosticResult(
                    domain = "example.com",
                    verificationState = VerificationState.VERIFIED,
                    fingerprintComparison = FingerprintComparisonResult.Match,
                    assetLinksStatus = AssetLinksStatus.VALID,
                    failureReasons = emptyList(),
                    suggestions = emptyList()
                )
            ),
            localFingerprint = "AA:BB:CC:DD"
        )

        val repository = FakeVerificationDiagnosticsRepository(
            result = Result.success(expected)
        )
        val useCase = AnalyzeVerificationUseCase(repository)

        val result = useCase("emulator-5554", "com.example.app")

        assertTrue(result.isSuccess)
        val diagnostics = result.getOrNull()
        assertNotNull(diagnostics)
        assertEquals("com.example.app", diagnostics.packageName)
        assertEquals("emulator-5554", diagnostics.deviceSerial)
        assertEquals(1, diagnostics.domainResults.size)
        assertEquals("AA:BB:CC:DD", diagnostics.localFingerprint)
    }

    @Test
    fun `invoke should return failure when repository fails`() = runTest {
        val error = RuntimeException("Device not found")
        val repository = FakeVerificationDiagnosticsRepository(
            result = Result.failure(error)
        )
        val useCase = AnalyzeVerificationUseCase(repository)

        val result = useCase("emulator-5554", "com.example.app")

        assertTrue(result.isFailure)
        assertEquals("Device not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `invoke should pass correct parameters to repository`() = runTest {
        val repository = FakeVerificationDiagnosticsRepository(
            result = Result.success(
                VerificationDiagnostics(
                    packageName = "com.test.app",
                    deviceSerial = "device-123",
                    domainResults = emptyList(),
                    localFingerprint = null
                )
            )
        )
        val useCase = AnalyzeVerificationUseCase(repository)

        useCase("device-123", "com.test.app")

        assertEquals("device-123", repository.lastDeviceSerial)
        assertEquals("com.test.app", repository.lastPackageName)
    }

    @Test
    fun `invoke should return diagnostics with multiple domain results`() = runTest {
        val expected = VerificationDiagnostics(
            packageName = "com.example.app",
            deviceSerial = "device-1",
            domainResults = listOf(
                DomainDiagnosticResult(
                    domain = "example.com",
                    verificationState = VerificationState.VERIFIED,
                    fingerprintComparison = FingerprintComparisonResult.Match,
                    assetLinksStatus = AssetLinksStatus.VALID,
                    failureReasons = emptyList(),
                    suggestions = emptyList()
                ),
                DomainDiagnosticResult(
                    domain = "test.example.com",
                    verificationState = VerificationState.UNVERIFIED,
                    fingerprintComparison = FingerprintComparisonResult.Mismatch(
                        localFingerprint = "AA:BB",
                        remoteFingerprints = listOf("CC:DD")
                    ),
                    assetLinksStatus = AssetLinksStatus.VALID,
                    failureReasons = listOf(FailureReason.FINGERPRINT_MISMATCH),
                    suggestions = listOf("Update fingerprint")
                )
            ),
            localFingerprint = "AA:BB"
        )

        val repository = FakeVerificationDiagnosticsRepository(
            result = Result.success(expected)
        )
        val useCase = AnalyzeVerificationUseCase(repository)

        val result = useCase("device-1", "com.example.app")

        assertTrue(result.isSuccess)
        val diagnostics = result.getOrNull()!!
        assertEquals(2, diagnostics.totalDomains)
        assertEquals(1, diagnostics.verifiedDomains)
        assertEquals(1, diagnostics.failedDomains)
        assertTrue(diagnostics.hasIssues)
    }
}

/**
 * Fake implementation of VerificationDiagnosticsRepository for testing
 */
class FakeVerificationDiagnosticsRepository(
    private val result: Result<VerificationDiagnostics>
) : VerificationDiagnosticsRepository {

    var lastDeviceSerial: String? = null
        private set
    var lastPackageName: String? = null
        private set

    override suspend fun analyzeVerification(
        deviceSerial: String,
        packageName: String
    ): Result<VerificationDiagnostics> {
        lastDeviceSerial = deviceSerial
        lastPackageName = packageName
        return result
    }
}
