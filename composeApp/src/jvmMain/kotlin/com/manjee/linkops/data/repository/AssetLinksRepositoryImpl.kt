package com.manjee.linkops.data.repository

import com.manjee.linkops.data.parser.AssetLinksParser
import com.manjee.linkops.domain.model.*
import com.manjee.linkops.domain.repository.AssetLinksRepository
import com.manjee.linkops.infrastructure.network.AssetLinksClient
import com.manjee.linkops.infrastructure.network.AssetLinksResponse

/**
 * Implementation of AssetLinksRepository using HTTP client
 */
class AssetLinksRepositoryImpl(
    private val client: AssetLinksClient,
    private val parser: AssetLinksParser
) : AssetLinksRepository {

    override suspend fun validateAssetLinks(domain: String): Result<AssetLinksValidation> {
        val url = "https://$domain/.well-known/assetlinks.json"

        return runCatching {
            when (val response = client.fetchWithRedirects(domain)) {
                is AssetLinksResponse.Success -> {
                    handleSuccessResponse(domain, url, response)
                }

                is AssetLinksResponse.NotFound -> {
                    AssetLinksValidation(
                        domain = domain,
                        url = url,
                        status = ValidationStatus.NOT_FOUND,
                        issues = listOf(
                            ValidationIssue(
                                severity = ValidationIssue.Severity.ERROR,
                                code = ValidationIssue.IssueCode.FILE_NOT_FOUND,
                                message = "assetlinks.json not found at $url"
                            )
                        )
                    )
                }

                is AssetLinksResponse.Redirect -> {
                    AssetLinksValidation(
                        domain = domain,
                        url = url,
                        status = ValidationStatus.REDIRECT,
                        issues = listOf(
                            ValidationIssue(
                                severity = ValidationIssue.Severity.WARNING,
                                code = ValidationIssue.IssueCode.REDIRECT_DETECTED,
                                message = "Request was redirected",
                                details = "Redirected to: ${response.redirectUrl}"
                            )
                        )
                    )
                }

                is AssetLinksResponse.HttpError -> {
                    AssetLinksValidation(
                        domain = domain,
                        url = url,
                        status = ValidationStatus.NETWORK_ERROR,
                        issues = listOf(
                            ValidationIssue(
                                severity = ValidationIssue.Severity.ERROR,
                                code = ValidationIssue.IssueCode.NETWORK_ERROR,
                                message = "HTTP error: ${response.statusCode}",
                                details = response.message
                            )
                        )
                    )
                }

                is AssetLinksResponse.NetworkError -> {
                    val issueCode = if (response.message.contains("timeout", ignoreCase = true)) {
                        ValidationIssue.IssueCode.NETWORK_TIMEOUT
                    } else if (response.message.contains("ssl", ignoreCase = true)) {
                        ValidationIssue.IssueCode.SSL_ERROR
                    } else {
                        ValidationIssue.IssueCode.NETWORK_ERROR
                    }

                    AssetLinksValidation(
                        domain = domain,
                        url = url,
                        status = ValidationStatus.NETWORK_ERROR,
                        issues = listOf(
                            ValidationIssue(
                                severity = ValidationIssue.Severity.ERROR,
                                code = issueCode,
                                message = "Network error: ${response.message}"
                            )
                        )
                    )
                }
            }
        }
    }

    private fun handleSuccessResponse(
        domain: String,
        url: String,
        response: AssetLinksResponse.Success
    ): AssetLinksValidation {
        val issues = mutableListOf<ValidationIssue>()

        // Check content type
        if (!response.contentType.contains("application/json", ignoreCase = true)) {
            issues.add(
                ValidationIssue(
                    severity = ValidationIssue.Severity.WARNING,
                    code = ValidationIssue.IssueCode.WRONG_CONTENT_TYPE,
                    message = "Content-Type is not application/json",
                    details = "Received: ${response.contentType}"
                )
            )
        }

        // Check for redirect
        if (response.wasRedirected) {
            issues.add(
                ValidationIssue(
                    severity = ValidationIssue.Severity.WARNING,
                    code = ValidationIssue.IssueCode.REDIRECT_DETECTED,
                    message = "Request was redirected",
                    details = "Final URL: ${response.finalUrl}"
                )
            )
        }

        // Parse content
        return when (val parseResult = parser.parse(response.content)) {
            is AssetLinksParser.ParseResult.Success -> {
                issues.addAll(parseResult.issues)
                AssetLinksValidation(
                    domain = domain,
                    url = url,
                    status = if (issues.any { it.severity == ValidationIssue.Severity.ERROR }) {
                        ValidationStatus.INVALID_JSON
                    } else {
                        ValidationStatus.VALID
                    },
                    issues = issues,
                    content = parseResult.content,
                    rawJson = response.content
                )
            }

            is AssetLinksParser.ParseResult.Error -> {
                issues.addAll(parseResult.issues)
                AssetLinksValidation(
                    domain = domain,
                    url = url,
                    status = ValidationStatus.INVALID_JSON,
                    issues = issues,
                    rawJson = response.content
                )
            }
        }
    }

    override suspend fun checkPackageInAssetLinks(
        domain: String,
        packageName: String,
        fingerprint: String?
    ): Result<Boolean> {
        return validateAssetLinks(domain).map { validation ->
            val content = validation.content ?: return@map false

            if (fingerprint != null) {
                content.containsPackageWithFingerprint(packageName, fingerprint)
            } else {
                content.packageNames.contains(packageName)
            }
        }
    }

    override suspend fun fetchRawAssetLinks(domain: String): Result<String> {
        return runCatching {
            when (val response = client.fetchWithRedirects(domain)) {
                is AssetLinksResponse.Success -> response.content
                is AssetLinksResponse.NotFound -> throw Exception("assetlinks.json not found")
                is AssetLinksResponse.Redirect -> throw Exception("Too many redirects")
                is AssetLinksResponse.HttpError -> throw Exception("HTTP ${response.statusCode}: ${response.message}")
                is AssetLinksResponse.NetworkError -> throw Exception(response.message)
            }
        }
    }
}
