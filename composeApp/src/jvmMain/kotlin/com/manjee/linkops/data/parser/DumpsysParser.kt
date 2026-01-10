package com.manjee.linkops.data.parser

import com.manjee.linkops.domain.model.AppLink
import com.manjee.linkops.domain.model.DomainVerification
import com.manjee.linkops.domain.model.VerificationState

/**
 * Parser for `dumpsys package domain-preferred-apps` output (Android 11 and below, SDK <= 30)
 *
 * Example output:
 * ```
 * App linkages for user 0:
 * Package: com.example.app
 *   Domains: example.com test.example.com
 *   Status: always : 200000001
 * ```
 */
class DumpsysParser {

    fun parse(output: String): List<AppLink> {
        val appLinks = mutableListOf<AppLink>()
        var currentPackage: String? = null
        var currentDomains: List<String>? = null
        var currentStatus: VerificationState? = null

        output.lines().forEach { line ->
            val trimmed = line.trim()

            when {
                trimmed.startsWith("Package:") -> {
                    // Save previous package if complete
                    saveCurrentPackage(currentPackage, currentDomains, currentStatus, appLinks)

                    currentPackage = trimmed.substringAfter("Package:").trim()
                    currentDomains = null
                    currentStatus = null
                }

                trimmed.startsWith("Domains:") -> {
                    currentDomains = trimmed.substringAfter("Domains:")
                        .trim()
                        .split(" ")
                        .filter { it.isNotEmpty() }
                }

                trimmed.startsWith("Status:") -> {
                    val statusPart = trimmed.substringAfter("Status:").trim()
                    currentStatus = parseStatus(statusPart)

                    // Save package after status is parsed (complete entry)
                    saveCurrentPackage(currentPackage, currentDomains, currentStatus, appLinks)

                    // Reset for next package
                    currentPackage = null
                    currentDomains = null
                    currentStatus = null
                }
            }
        }

        return appLinks
    }

    private fun parseStatus(statusLine: String): VerificationState {
        return when {
            statusLine.startsWith("always") -> VerificationState.APPROVED
            statusLine.startsWith("never") -> VerificationState.DENIED
            statusLine.startsWith("ask") -> VerificationState.UNVERIFIED
            else -> VerificationState.LEGACY_FAILURE
        }
    }

    private fun saveCurrentPackage(
        packageName: String?,
        domains: List<String>?,
        status: VerificationState?,
        appLinks: MutableList<AppLink>
    ) {
        if (packageName != null && domains != null && status != null && domains.isNotEmpty()) {
            val domainVerifications = domains.map { domain ->
                DomainVerification(domain, status, null)
            }
            appLinks.add(AppLink(packageName, domainVerifications))
        }
    }
}
