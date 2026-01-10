package com.manjee.linkops.domain.model

/**
 * Represents an app's deep link configuration
 */
data class AppLink(
    val packageName: String,
    val domains: List<DomainVerification>
)

/**
 * Represents verification state for a single domain
 */
data class DomainVerification(
    val domain: String,
    val verificationState: VerificationState,
    val fingerprint: String? = null
)

/**
 * Domain verification state
 * Covers both Android 11 (legacy) and Android 12+ (modern) states
 */
enum class VerificationState {
    VERIFIED,           // Android 12+ "verified"
    APPROVED,           // Android 11 "always"
    DENIED,             // Android 11 "never"
    UNVERIFIED,         // Android 12+ "none"
    LEGACY_FAILURE,     // Android 11 failure
    UNKNOWN;

    val isSuccessful: Boolean
        get() = this == VERIFIED || this == APPROVED
}
