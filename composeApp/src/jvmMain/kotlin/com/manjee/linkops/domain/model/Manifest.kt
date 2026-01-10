package com.manjee.linkops.domain.model

/**
 * Parsed Android Manifest information focused on deep links
 */
data class ManifestInfo(
    val packageName: String,
    val versionName: String?,
    val versionCode: Int?,
    val activities: List<ActivityInfo>,
    val deepLinks: List<DeepLinkInfo>
) {
    /**
     * Get all unique schemes from deep links
     */
    val schemes: List<String>
        get() = deepLinks.map { it.scheme }.distinct()

    /**
     * Get all unique hosts from deep links
     */
    val hosts: List<String>
        get() = deepLinks.mapNotNull { it.host }.distinct()

    /**
     * Check if app supports App Links (https scheme with autoVerify)
     */
    val supportsAppLinks: Boolean
        get() = deepLinks.any { it.isAppLink }

    /**
     * Get only App Links (verified https links)
     */
    val appLinks: List<DeepLinkInfo>
        get() = deepLinks.filter { it.isAppLink }

    /**
     * Get only custom scheme deep links
     */
    val customSchemeLinks: List<DeepLinkInfo>
        get() = deepLinks.filter { !it.isAppLink && it.scheme != "http" && it.scheme != "https" }
}

/**
 * Activity information with intent filters
 */
data class ActivityInfo(
    val name: String,
    val exported: Boolean?,
    val intentFilters: List<IntentFilterInfo>
) {
    /**
     * Check if this activity handles deep links
     */
    val handlesDeepLinks: Boolean
        get() = intentFilters.any { it.hasDeepLinkData }
}

/**
 * Intent filter information
 */
data class IntentFilterInfo(
    val actions: List<String>,
    val categories: List<String>,
    val data: List<IntentDataInfo>,
    val autoVerify: Boolean = false
) {
    /**
     * Check if this is a browsable intent filter
     */
    val isBrowsable: Boolean
        get() = categories.contains("android.intent.category.BROWSABLE")

    /**
     * Check if this filter handles VIEW action
     */
    val isViewAction: Boolean
        get() = actions.contains("android.intent.action.VIEW")

    /**
     * Check if this filter has deep link data (scheme/host)
     */
    val hasDeepLinkData: Boolean
        get() = data.any { it.scheme != null }

    /**
     * Check if this is an App Link filter (https + autoVerify)
     */
    val isAppLinkFilter: Boolean
        get() = autoVerify && data.any { it.scheme == "https" || it.scheme == "http" }
}

/**
 * Data element in intent filter
 */
data class IntentDataInfo(
    val scheme: String?,
    val host: String?,
    val port: String?,
    val path: String?,
    val pathPrefix: String?,
    val pathPattern: String?,
    val mimeType: String?
) {
    /**
     * Build URI pattern from data elements
     */
    val uriPattern: String?
        get() {
            if (scheme == null) return null

            val sb = StringBuilder(scheme).append("://")
            host?.let { sb.append(it) }
            port?.let { sb.append(":").append(it) }

            when {
                path != null -> sb.append(path)
                pathPrefix != null -> sb.append(pathPrefix).append("*")
                pathPattern != null -> sb.append(pathPattern)
            }

            return sb.toString()
        }
}

/**
 * Extracted deep link information
 */
data class DeepLinkInfo(
    val scheme: String,
    val host: String?,
    val path: String?,
    val pathPrefix: String?,
    val pathPattern: String?,
    val activityName: String,
    val autoVerify: Boolean = false
) {
    /**
     * Check if this is an App Link (https with autoVerify)
     */
    val isAppLink: Boolean
        get() = (scheme == "https" || scheme == "http") && autoVerify

    /**
     * Build a sample URI for this deep link
     */
    val sampleUri: String
        get() {
            val sb = StringBuilder(scheme).append("://")
            host?.let { sb.append(it) } ?: sb.append("example.com")

            when {
                path != null -> sb.append(path)
                pathPrefix != null -> sb.append(pathPrefix).append("example")
                pathPattern != null -> sb.append("/path")
                else -> sb.append("/")
            }

            return sb.toString()
        }

    /**
     * Human-readable pattern description
     */
    val patternDescription: String
        get() {
            val sb = StringBuilder("$scheme://")
            host?.let { sb.append(it) } ?: sb.append("*")

            when {
                path != null -> sb.append(path)
                pathPrefix != null -> sb.append(pathPrefix).append("*")
                pathPattern != null -> sb.append(" (pattern: $pathPattern)")
                else -> sb.append("/*")
            }

            return sb.toString()
        }
}

/**
 * Source of manifest data
 */
sealed class ManifestSource {
    /**
     * From installed app on device
     */
    data class InstalledApp(
        val deviceSerial: String,
        val packageName: String
    ) : ManifestSource()

    /**
     * From APK file
     */
    data class ApkFile(
        val filePath: String
    ) : ManifestSource()
}

/**
 * Result of manifest analysis
 */
data class ManifestAnalysisResult(
    val source: ManifestSource,
    val manifestInfo: ManifestInfo?,
    val domainVerification: DomainVerificationResult? = null,
    val error: String? = null,
    val rawManifest: String? = null
) {
    val isSuccess: Boolean
        get() = manifestInfo != null && error == null
}

/**
 * Domain verification status from pm get-app-links
 */
data class DomainVerificationResult(
    val packageName: String,
    val domains: List<DomainVerificationInfo>
)

/**
 * Individual domain verification status
 */
data class DomainVerificationInfo(
    val domain: String,
    val status: DomainVerificationStatus
)

/**
 * Domain verification status enum
 */
enum class DomainVerificationStatus(val displayName: String) {
    VERIFIED("verified"),
    NONE("none"),
    LEGACY_FAILURE("legacy_failure"),
    ALWAYS("always"),
    NEVER("never"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(value: String): DomainVerificationStatus {
            return entries.find { it.displayName.equals(value, ignoreCase = true) }
                ?: UNKNOWN
        }
    }
}
