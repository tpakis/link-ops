package com.manjee.linkops.data.parser

import com.manjee.linkops.domain.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parser for assetlinks.json content
 */
class AssetLinksParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parse assetlinks.json content
     *
     * @param content Raw JSON string
     * @return Parsed content with any issues found
     */
    fun parse(content: String): ParseResult {
        val issues = mutableListOf<ValidationIssue>()

        return try {
            val dtos = json.decodeFromString<List<AssetLinksDto>>(content)

            if (dtos.isEmpty()) {
                issues.add(
                    ValidationIssue(
                        severity = ValidationIssue.Severity.WARNING,
                        code = ValidationIssue.IssueCode.MISSING_RELATION,
                        message = "assetlinks.json is empty"
                    )
                )
            }

            if (dtos.size > 1) {
                issues.add(
                    ValidationIssue(
                        severity = ValidationIssue.Severity.INFO,
                        code = ValidationIssue.IssueCode.MULTIPLE_STATEMENTS,
                        message = "Found ${dtos.size} statements in assetlinks.json"
                    )
                )
            }

            val statements = dtos.mapNotNull { dto ->
                validateAndConvert(dto, issues)
            }

            ParseResult.Success(
                content = AssetLinksContent(statements),
                issues = issues
            )
        } catch (e: Exception) {
            ParseResult.Error(
                message = "Failed to parse JSON: ${e.message}",
                cause = e,
                issues = listOf(
                    ValidationIssue(
                        severity = ValidationIssue.Severity.ERROR,
                        code = ValidationIssue.IssueCode.INVALID_JSON_SYNTAX,
                        message = "Invalid JSON syntax",
                        details = e.message
                    )
                )
            )
        }
    }

    private fun validateAndConvert(
        dto: AssetLinksDto,
        issues: MutableList<ValidationIssue>
    ): AssetStatement? {
        // Validate relation
        if (dto.relation.isEmpty()) {
            issues.add(
                ValidationIssue(
                    severity = ValidationIssue.Severity.ERROR,
                    code = ValidationIssue.IssueCode.MISSING_RELATION,
                    message = "Statement missing 'relation' field"
                )
            )
            return null
        }

        // Validate target
        val target = dto.target
        if (target == null) {
            issues.add(
                ValidationIssue(
                    severity = ValidationIssue.Severity.ERROR,
                    code = ValidationIssue.IssueCode.MISSING_TARGET,
                    message = "Statement missing 'target' field"
                )
            )
            return null
        }

        // Validate namespace
        if (target.namespace != "android_app") {
            issues.add(
                ValidationIssue(
                    severity = ValidationIssue.Severity.WARNING,
                    code = ValidationIssue.IssueCode.INVALID_NAMESPACE,
                    message = "Unexpected namespace: ${target.namespace}",
                    details = "Expected 'android_app' for Android App Links"
                )
            )
        }

        // Validate package name
        if (target.packageName.isNullOrBlank()) {
            issues.add(
                ValidationIssue(
                    severity = ValidationIssue.Severity.ERROR,
                    code = ValidationIssue.IssueCode.MISSING_PACKAGE_NAME,
                    message = "Target missing 'package_name' field"
                )
            )
            return null
        }

        // Validate fingerprints
        val fingerprints = target.sha256CertFingerprints ?: emptyList()
        if (fingerprints.isEmpty()) {
            issues.add(
                ValidationIssue(
                    severity = ValidationIssue.Severity.ERROR,
                    code = ValidationIssue.IssueCode.MISSING_FINGERPRINT,
                    message = "Target missing 'sha256_cert_fingerprints' field",
                    details = "Package: ${target.packageName}"
                )
            )
            return null
        }

        // Validate fingerprint format
        fingerprints.forEach { fp ->
            if (!isValidFingerprint(fp)) {
                issues.add(
                    ValidationIssue(
                        severity = ValidationIssue.Severity.WARNING,
                        code = ValidationIssue.IssueCode.FINGERPRINT_FORMAT,
                        message = "Fingerprint format may be incorrect: $fp",
                        details = "Expected format: XX:XX:XX:... (64 hex characters with colons)"
                    )
                )
            }
        }

        if (fingerprints.size > 1) {
            issues.add(
                ValidationIssue(
                    severity = ValidationIssue.Severity.INFO,
                    code = ValidationIssue.IssueCode.MULTIPLE_FINGERPRINTS,
                    message = "Package ${target.packageName} has ${fingerprints.size} fingerprints"
                )
            )
        }

        return AssetStatement(
            relation = dto.relation,
            target = AssetTarget(
                namespace = target.namespace ?: "android_app",
                packageName = target.packageName,
                sha256CertFingerprints = fingerprints
            )
        )
    }

    private fun isValidFingerprint(fingerprint: String): Boolean {
        // SHA-256 fingerprint should be 64 hex characters, typically with colons
        val normalized = fingerprint.replace(":", "")
        return normalized.length == 64 && normalized.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }
    }

    sealed class ParseResult {
        data class Success(
            val content: AssetLinksContent,
            val issues: List<ValidationIssue>
        ) : ParseResult()

        data class Error(
            val message: String,
            val cause: Throwable?,
            val issues: List<ValidationIssue>
        ) : ParseResult()
    }
}

/**
 * DTO for parsing assetlinks.json
 */
@Serializable
data class AssetLinksDto(
    val relation: List<String> = emptyList(),
    val target: TargetDto? = null
)

@Serializable
data class TargetDto(
    val namespace: String? = null,
    @SerialName("package_name")
    val packageName: String? = null,
    @SerialName("sha256_cert_fingerprints")
    val sha256CertFingerprints: List<String>? = null
)
