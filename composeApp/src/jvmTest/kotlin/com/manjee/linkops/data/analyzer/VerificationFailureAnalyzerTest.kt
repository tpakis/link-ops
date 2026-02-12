package com.manjee.linkops.data.analyzer

import com.manjee.linkops.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VerificationFailureAnalyzerTest {

    private val analyzer = VerificationFailureAnalyzer()

    @Test
    fun `analyze should return empty results for verified domain`() {
        val (reasons, suggestions) = analyzer.analyze(
            verificationState = VerificationState.VERIFIED,
            fingerprintResult = FingerprintComparisonResult.Match,
            assetLinksStatus = AssetLinksStatus.VALID,
            packageName = "com.example.app",
            domain = "example.com"
        )

        assertTrue(reasons.isEmpty(), "No failure reasons for verified domain")
        assertTrue(suggestions.isEmpty(), "No suggestions for verified domain")
    }

    @Test
    fun `analyze should return empty results for approved domain`() {
        val (reasons, suggestions) = analyzer.analyze(
            verificationState = VerificationState.APPROVED,
            fingerprintResult = FingerprintComparisonResult.NoLocalFingerprint,
            assetLinksStatus = AssetLinksStatus.VALID,
            packageName = "com.example.app",
            domain = "example.com"
        )

        assertTrue(reasons.isEmpty())
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `analyze should identify missing assetlinks json`() {
        val (reasons, suggestions) = analyzer.analyze(
            verificationState = VerificationState.UNVERIFIED,
            fingerprintResult = FingerprintComparisonResult.RemoteUnavailable,
            assetLinksStatus = AssetLinksStatus.NOT_FOUND,
            packageName = "com.example.app",
            domain = "example.com"
        )

        assertTrue(reasons.contains(FailureReason.ASSET_LINKS_MISSING))
        assertTrue(suggestions.any { it.contains("assetlinks.json") })
    }

    @Test
    fun `analyze should identify invalid json in assetlinks`() {
        val (reasons, suggestions) = analyzer.analyze(
            verificationState = VerificationState.UNVERIFIED,
            fingerprintResult = FingerprintComparisonResult.RemoteUnavailable,
            assetLinksStatus = AssetLinksStatus.INVALID_JSON,
            packageName = "com.example.app",
            domain = "example.com"
        )

        assertTrue(reasons.contains(FailureReason.ASSET_LINKS_INVALID_JSON))
        assertTrue(suggestions.any { it.contains("JSON syntax") })
    }

    @Test
    fun `analyze should identify network error`() {
        val (reasons, suggestions) = analyzer.analyze(
            verificationState = VerificationState.UNVERIFIED,
            fingerprintResult = FingerprintComparisonResult.RemoteUnavailable,
            assetLinksStatus = AssetLinksStatus.NETWORK_ERROR,
            packageName = "com.example.app",
            domain = "example.com"
        )

        assertTrue(reasons.contains(FailureReason.ASSET_LINKS_NETWORK_ERROR))
        assertTrue(suggestions.any { it.contains("reachable") })
    }

    @Test
    fun `analyze should identify redirect issue`() {
        val (reasons, suggestions) = analyzer.analyze(
            verificationState = VerificationState.UNVERIFIED,
            fingerprintResult = FingerprintComparisonResult.RemoteUnavailable,
            assetLinksStatus = AssetLinksStatus.REDIRECT,
            packageName = "com.example.app",
            domain = "example.com"
        )

        assertTrue(reasons.contains(FailureReason.ASSET_LINKS_REDIRECT))
        assertTrue(suggestions.any { it.contains("redirect") })
    }

    @Test
    fun `analyze should identify fingerprint mismatch`() {
        val (reasons, suggestions) = analyzer.analyze(
            verificationState = VerificationState.UNVERIFIED,
            fingerprintResult = FingerprintComparisonResult.Mismatch(
                localFingerprint = "AA:BB",
                remoteFingerprints = listOf("CC:DD")
            ),
            assetLinksStatus = AssetLinksStatus.VALID,
            packageName = "com.example.app",
            domain = "example.com"
        )

        assertTrue(reasons.contains(FailureReason.FINGERPRINT_MISMATCH))
        assertTrue(suggestions.any { it.contains("fingerprint") })
        assertTrue(suggestions.any { it.contains("AA:BB") })
    }

    @Test
    fun `analyze should identify package not in assetlinks`() {
        val (reasons, suggestions) = analyzer.analyze(
            verificationState = VerificationState.UNVERIFIED,
            fingerprintResult = FingerprintComparisonResult.NoRemoteFingerprint,
            assetLinksStatus = AssetLinksStatus.VALID,
            packageName = "com.example.app",
            domain = "example.com"
        )

        assertTrue(reasons.contains(FailureReason.PACKAGE_NOT_IN_ASSET_LINKS))
        assertTrue(suggestions.any { it.contains("com.example.app") })
    }

    @Test
    fun `analyze should identify multiple failure reasons simultaneously`() {
        val (reasons, _) = analyzer.analyze(
            verificationState = VerificationState.UNVERIFIED,
            fingerprintResult = FingerprintComparisonResult.Mismatch(
                localFingerprint = "AA:BB",
                remoteFingerprints = listOf("CC:DD")
            ),
            assetLinksStatus = AssetLinksStatus.REDIRECT,
            packageName = "com.example.app",
            domain = "example.com"
        )

        assertTrue(reasons.contains(FailureReason.ASSET_LINKS_REDIRECT))
        assertTrue(reasons.contains(FailureReason.FINGERPRINT_MISMATCH))
        assertEquals(2, reasons.size, "Should have exactly two failure reasons")
    }

    @Test
    fun `analyze should return unknown reason when no specific cause found`() {
        val (reasons, suggestions) = analyzer.analyze(
            verificationState = VerificationState.DENIED,
            fingerprintResult = FingerprintComparisonResult.Match,
            assetLinksStatus = AssetLinksStatus.VALID,
            packageName = "com.example.app",
            domain = "example.com"
        )

        assertTrue(reasons.contains(FailureReason.UNKNOWN))
        assertTrue(suggestions.isNotEmpty(), "Should have at least one suggestion")
    }

    @Test
    fun `analyze should handle legacy failure state`() {
        val (reasons, _) = analyzer.analyze(
            verificationState = VerificationState.LEGACY_FAILURE,
            fingerprintResult = FingerprintComparisonResult.NoLocalFingerprint,
            assetLinksStatus = AssetLinksStatus.NOT_FOUND,
            packageName = "com.example.app",
            domain = "example.com"
        )

        assertTrue(reasons.contains(FailureReason.ASSET_LINKS_MISSING))
    }

    @Test
    fun `analyze should handle not checked status without adding failures`() {
        val (reasons, _) = analyzer.analyze(
            verificationState = VerificationState.UNKNOWN,
            fingerprintResult = FingerprintComparisonResult.NoLocalFingerprint,
            assetLinksStatus = AssetLinksStatus.NOT_CHECKED,
            packageName = "com.example.app",
            domain = "example.com"
        )

        // NOT_CHECKED + NoLocalFingerprint should only yield UNKNOWN
        assertTrue(reasons.contains(FailureReason.UNKNOWN))
    }

    @Test
    fun `analyze suggestion should include domain name`() {
        val (_, suggestions) = analyzer.analyze(
            verificationState = VerificationState.UNVERIFIED,
            fingerprintResult = FingerprintComparisonResult.RemoteUnavailable,
            assetLinksStatus = AssetLinksStatus.NOT_FOUND,
            packageName = "com.example.app",
            domain = "custom.domain.com"
        )

        assertTrue(suggestions.any { it.contains("custom.domain.com") })
    }
}
