package com.manjee.linkops.domain.model

/**
 * Result of validating an assetlinks.json file
 */
data class AssetLinksValidation(
    val domain: String,
    val url: String,
    val status: ValidationStatus,
    val issues: List<ValidationIssue> = emptyList(),
    val content: AssetLinksContent? = null,
    val rawJson: String? = null
)

/**
 * Parsed content of assetlinks.json
 */
data class AssetLinksContent(
    val statements: List<AssetStatement>
) {
    /**
     * Get all package names declared in the assetlinks.json
     */
    val packageNames: List<String>
        get() = statements.map { it.target.packageName }.distinct()

    /**
     * Get all fingerprints for a specific package
     */
    fun getFingerprintsForPackage(packageName: String): List<String> {
        return statements
            .filter { it.target.packageName == packageName }
            .flatMap { it.target.sha256CertFingerprints }
    }

    /**
     * Check if a package with fingerprint is declared
     */
    fun containsPackageWithFingerprint(packageName: String, fingerprint: String): Boolean {
        val normalizedFingerprint = fingerprint.uppercase().replace(":", "")
        return statements.any { statement ->
            statement.target.packageName == packageName &&
                statement.target.sha256CertFingerprints.any { fp ->
                    fp.uppercase().replace(":", "") == normalizedFingerprint
                }
        }
    }
}

/**
 * A single statement in assetlinks.json
 */
data class AssetStatement(
    val relation: List<String>,
    val target: AssetTarget
) {
    /**
     * Check if this statement allows handling all URLs
     */
    val allowsHandleAllUrls: Boolean
        get() = relation.contains("delegate_permission/common.handle_all_urls")
}

/**
 * Target of an asset statement
 */
data class AssetTarget(
    val namespace: String,
    val packageName: String,
    val sha256CertFingerprints: List<String>
) {
    /**
     * Check if this is an Android app target
     */
    val isAndroidApp: Boolean
        get() = namespace == "android_app"
}

/**
 * Status of assetlinks.json validation
 */
enum class ValidationStatus {
    VALID,              // File exists and is valid JSON
    INVALID_JSON,       // File exists but is not valid JSON
    NOT_FOUND,          // 404 - file not found
    REDIRECT,           // File was served via redirect (warning)
    NETWORK_ERROR,      // Network error (timeout, DNS, etc.)
    FINGERPRINT_MISMATCH, // Fingerprint doesn't match
    INVALID_CONTENT_TYPE  // Wrong content-type header
}

/**
 * An issue found during validation
 */
data class ValidationIssue(
    val severity: Severity,
    val code: IssueCode,
    val message: String,
    val details: String? = null
) {
    enum class Severity {
        ERROR,      // Validation will fail
        WARNING,    // May cause issues
        INFO        // Informational
    }

    enum class IssueCode {
        // Errors
        FILE_NOT_FOUND,
        INVALID_JSON_SYNTAX,
        MISSING_RELATION,
        MISSING_TARGET,
        MISSING_PACKAGE_NAME,
        MISSING_FINGERPRINT,
        INVALID_NAMESPACE,
        NETWORK_TIMEOUT,
        NETWORK_ERROR,
        SSL_ERROR,

        // Warnings
        REDIRECT_DETECTED,
        WRONG_CONTENT_TYPE,
        FINGERPRINT_FORMAT,

        // Info
        MULTIPLE_STATEMENTS,
        MULTIPLE_FINGERPRINTS
    }
}

/**
 * Request to validate a domain's assetlinks.json
 */
data class AssetLinksRequest(
    val domain: String,
    val packageName: String? = null,
    val expectedFingerprints: List<String> = emptyList()
)

/**
 * Comparison result between device app link status and assetlinks.json
 */
data class AssetLinksComparison(
    val domain: String,
    val packageName: String,
    val deviceVerificationState: VerificationState,
    val assetLinksValidation: AssetLinksValidation,
    val fingerprintMatch: FingerprintMatchResult
)

/**
 * Result of fingerprint matching
 */
sealed class FingerprintMatchResult {
    data object Match : FingerprintMatchResult()
    data class Mismatch(
        val deviceFingerprint: String?,
        val assetLinksFingerprints: List<String>
    ) : FingerprintMatchResult()
    data object NoFingerprintOnDevice : FingerprintMatchResult()
    data object NoFingerprintInAssetLinks : FingerprintMatchResult()
    data object AssetLinksNotAvailable : FingerprintMatchResult()
}
