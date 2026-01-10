package com.manjee.linkops.data.repository

import com.manjee.linkops.data.parser.ManifestParser
import com.manjee.linkops.domain.model.*
import com.manjee.linkops.domain.repository.ManifestRepository
import com.manjee.linkops.domain.repository.PackageFilter
import com.manjee.linkops.infrastructure.adb.AdbShellExecutor

/**
 * Implementation of ManifestRepository using ADB commands
 */
class ManifestRepositoryImpl(
    private val adbExecutor: AdbShellExecutor,
    private val manifestParser: ManifestParser
) : ManifestRepository {

    override suspend fun getManifestFromDevice(
        deviceSerial: String,
        packageName: String
    ): Result<ManifestAnalysisResult> {
        return runCatching {
            // Get package dump from device
            val dumpsysResult = adbExecutor.executeOnDevice(
                deviceSerial,
                "dumpsys package $packageName"
            )

            val dumpsysOutput = dumpsysResult.getOrElse { error ->
                return@runCatching ManifestAnalysisResult(
                    source = ManifestSource.InstalledApp(deviceSerial, packageName),
                    manifestInfo = null,
                    error = "Failed to get package info: ${error.message}"
                )
            }

            // Check if package exists
            if (dumpsysOutput.contains("Unable to find package") ||
                dumpsysOutput.contains("Package [$packageName] is unknown")) {
                return@runCatching ManifestAnalysisResult(
                    source = ManifestSource.InstalledApp(deviceSerial, packageName),
                    manifestInfo = null,
                    error = "Package not found: $packageName"
                )
            }

            // Parse manifest info
            val manifestInfo = try {
                manifestParser.parse(packageName, dumpsysOutput)
            } catch (e: Exception) {
                return@runCatching ManifestAnalysisResult(
                    source = ManifestSource.InstalledApp(deviceSerial, packageName),
                    manifestInfo = null,
                    error = "Failed to parse manifest: ${e.message}",
                    rawManifest = dumpsysOutput
                )
            }

            // Get domain verification status
            val domainVerification = getDomainVerification(deviceSerial, packageName).getOrNull()

            ManifestAnalysisResult(
                source = ManifestSource.InstalledApp(deviceSerial, packageName),
                manifestInfo = manifestInfo,
                domainVerification = domainVerification,
                rawManifest = dumpsysOutput
            )
        }
    }

    override suspend fun getDomainVerification(
        deviceSerial: String,
        packageName: String
    ): Result<DomainVerificationResult> {
        return runCatching {
            val output = adbExecutor.executeOnDevice(
                deviceSerial,
                "pm get-app-links $packageName"
            ).getOrThrow()

            parseDomainVerification(packageName, output)
        }
    }

    private fun parseDomainVerification(packageName: String, output: String): DomainVerificationResult {
        val domains = mutableListOf<DomainVerificationInfo>()

        // Parse lines like "*.myloveidol.com: verified"
        val domainRegex = Regex("""^\s+([^\s:]+):\s*(\w+)""")

        var inDomainSection = false
        for (line in output.lines()) {
            if (line.contains("Domain verification state:")) {
                inDomainSection = true
                continue
            }

            if (inDomainSection) {
                val match = domainRegex.find(line)
                if (match != null) {
                    val domain = match.groupValues[1]
                    val status = DomainVerificationStatus.fromString(match.groupValues[2])
                    domains.add(DomainVerificationInfo(domain, status))
                } else if (line.isNotBlank() && !line.startsWith(" ")) {
                    // End of domain section
                    break
                }
            }
        }

        return DomainVerificationResult(packageName, domains)
    }

    override suspend fun testDeepLink(
        deviceSerial: String,
        uri: String
    ): Result<String> {
        return adbExecutor.executeOnDevice(
            deviceSerial,
            "am start -a android.intent.action.VIEW -d \"$uri\""
        )
    }

    override suspend fun getInstalledPackages(
        deviceSerial: String,
        filter: PackageFilter
    ): Result<List<String>> {
        val filterFlag = when (filter) {
            PackageFilter.ALL -> ""
            PackageFilter.THIRD_PARTY -> "-3"
            PackageFilter.SYSTEM -> "-s"
        }

        return adbExecutor.executeOnDevice(
            deviceSerial,
            "pm list packages $filterFlag"
        ).map { output ->
            output.lines()
                .filter { it.startsWith("package:") }
                .map { it.removePrefix("package:").trim() }
                .sorted()
        }
    }

    override suspend fun searchPackages(
        deviceSerial: String,
        query: String
    ): Result<List<String>> {
        return getInstalledPackages(deviceSerial, PackageFilter.ALL)
            .map { packages ->
                packages.filter { it.contains(query, ignoreCase = true) }
            }
    }
}
