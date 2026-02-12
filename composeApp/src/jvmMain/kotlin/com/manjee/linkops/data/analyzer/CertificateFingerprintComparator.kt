package com.manjee.linkops.data.analyzer

import com.manjee.linkops.domain.model.AssetLinksContent
import com.manjee.linkops.domain.model.FingerprintComparisonResult

/**
 * Compares local APK certificate fingerprints against remote assetlinks.json fingerprints
 */
class CertificateFingerprintComparator {

    /**
     * Compare a local fingerprint against fingerprints in assetlinks.json content
     *
     * @param localFingerprint SHA256 fingerprint from the device (may be null)
     * @param packageName Package name to look up in assetlinks.json
     * @param assetLinksContent Parsed assetlinks.json content (may be null if fetch failed)
     * @return Comparison result
     */
    fun compare(
        localFingerprint: String?,
        packageName: String,
        assetLinksContent: AssetLinksContent?
    ): FingerprintComparisonResult {
        if (localFingerprint == null) {
            return FingerprintComparisonResult.NoLocalFingerprint
        }

        if (assetLinksContent == null) {
            return FingerprintComparisonResult.RemoteUnavailable
        }

        val remoteFingerprints = assetLinksContent.getFingerprintsForPackage(packageName)
        if (remoteFingerprints.isEmpty()) {
            return FingerprintComparisonResult.NoRemoteFingerprint
        }

        val normalizedLocal = normalizeFingerprint(localFingerprint)
        val match = remoteFingerprints.any { remote ->
            normalizeFingerprint(remote) == normalizedLocal
        }

        return if (match) {
            FingerprintComparisonResult.Match
        } else {
            FingerprintComparisonResult.Mismatch(
                localFingerprint = localFingerprint,
                remoteFingerprints = remoteFingerprints
            )
        }
    }

    companion object {
        /**
         * Normalize a fingerprint for comparison by removing colons and converting to uppercase
         *
         * @param fingerprint Raw fingerprint string
         * @return Normalized fingerprint
         */
        fun normalizeFingerprint(fingerprint: String): String {
            return fingerprint.uppercase().replace(":", "")
        }
    }
}
