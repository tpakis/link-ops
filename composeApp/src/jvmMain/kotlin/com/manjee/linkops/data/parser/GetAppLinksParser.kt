package com.manjee.linkops.data.parser

import com.manjee.linkops.domain.model.AppLink
import com.manjee.linkops.domain.model.DomainVerification
import com.manjee.linkops.domain.model.VerificationState

/**
 * Parser for `pm get-app-links` output (Android 12+, SDK >= 31)
 *
 * Example output:
 * ```
 * com.example.app:
 *   ID: 12345678-1234-1234-1234-123456789012
 *   Signatures: [AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89]
 *   Domain verification state:
 *     example.com: verified
 *     test.example.com: none
 * ```
 */
class GetAppLinksParser {

    fun parse(output: String): List<AppLink> {
        val appLinks = mutableListOf<AppLink>()
        var currentPackage: String? = null
        var currentSignature: String? = null
        val currentDomains = mutableListOf<DomainVerification>()
        var inDomainSection = false

        output.lines().forEach { line ->
            when {
                // Detect package name (e.g., "com.example.app:")
                isPackageLine(line) -> {
                    // Save previous package if exists
                    saveCurrentPackage(currentPackage, currentSignature, currentDomains, appLinks)

                    currentPackage = line.trim().removeSuffix(":")
                    currentSignature = null
                    currentDomains.clear()
                    inDomainSection = false
                }

                // Extract Signatures field
                line.trim().startsWith("Signatures:") -> {
                    currentSignature = parseSignatures(line)
                    inDomainSection = false
                }

                // Start of domain verification section
                line.trim() == "Domain verification state:" -> {
                    inDomainSection = true
                }

                // Parse domain state (e.g., "  example.com: verified")
                inDomainSection && line.trim().isNotEmpty() && !isMetadataLine(line) -> {
                    parseDomainLine(line)?.let { currentDomains.add(it) }
                }

                // Exit domain section on next metadata
                isMetadataLine(line) -> {
                    inDomainSection = false
                }
            }
        }

        // Save last package
        saveCurrentPackage(currentPackage, currentSignature, currentDomains, appLinks)

        return appLinks
    }

    private fun isPackageLine(line: String): Boolean {
        val trimmed = line.trim()
        // Package name pattern: starts with letter, contains dots, ends with colon
        return trimmed.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+:$"))
    }

    private fun isMetadataLine(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("ID:") ||
                trimmed.startsWith("Signatures:") ||
                trimmed.startsWith("User") ||
                trimmed.isEmpty()
    }

    /**
     * Extract SHA256 fingerprint from Signatures line
     *
     * Input format: "  Signatures: [AB:CD:EF:...]"
     * Returns the fingerprint string without brackets, or null if not parseable
     */
    private fun parseSignatures(line: String): String? {
        val value = line.trim().substringAfter("Signatures:").trim()
        if (value.isEmpty()) return null

        // Remove brackets
        val cleaned = value.removePrefix("[").removeSuffix("]").trim()
        if (cleaned.isEmpty()) return null

        return cleaned
    }

    private fun parseDomainLine(line: String): DomainVerification? {
        val trimmed = line.trim()
        val colonIndex = trimmed.lastIndexOf(":")
        if (colonIndex <= 0) return null

        val domain = trimmed.substring(0, colonIndex).trim()
        val stateString = trimmed.substring(colonIndex + 1).trim()

        // Skip if domain looks like metadata
        if (domain.isEmpty() || domain.contains(" ")) return null

        val state = parseVerificationState(stateString)
        return DomainVerification(domain, state, null)
    }

    private fun parseVerificationState(state: String): VerificationState {
        return when (state.lowercase()) {
            "verified" -> VerificationState.VERIFIED
            "none" -> VerificationState.UNVERIFIED
            "legacy_failure" -> VerificationState.LEGACY_FAILURE
            else -> VerificationState.UNKNOWN
        }
    }

    private fun saveCurrentPackage(
        packageName: String?,
        signature: String?,
        domains: List<DomainVerification>,
        appLinks: MutableList<AppLink>
    ) {
        if (packageName != null && domains.isNotEmpty()) {
            // Attach the package-level signature to each domain
            val domainsWithFingerprint = domains.map { domain ->
                if (signature != null) domain.copy(fingerprint = signature)
                else domain
            }
            appLinks.add(AppLink(packageName, domainsWithFingerprint))
        }
    }
}
