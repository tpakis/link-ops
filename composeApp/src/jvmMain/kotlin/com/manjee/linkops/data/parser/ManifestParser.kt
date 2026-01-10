package com.manjee.linkops.data.parser

import com.manjee.linkops.domain.model.*

/**
 * Parser for Android manifest information from dumpsys output
 *
 * Uses `adb shell dumpsys package <packageName>` to extract:
 * - Package info (version, etc.)
 * - Activity intent filters
 * - Deep link configurations
 */
class ManifestParser {

    /**
     * Parse dumpsys package output to extract manifest info
     */
    fun parse(packageName: String, dumpsysOutput: String): ManifestInfo {
        val versionName = extractVersionName(dumpsysOutput)
        val versionCode = extractVersionCode(dumpsysOutput)
        val activities = extractActivities(dumpsysOutput)
        val deepLinks = extractDeepLinks(activities)

        return ManifestInfo(
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            activities = activities,
            deepLinks = deepLinks
        )
    }

    private fun extractVersionName(output: String): String? {
        val regex = Regex("""versionName=([^\s]+)""")
        return regex.find(output)?.groupValues?.get(1)
    }

    private fun extractVersionCode(output: String): Int? {
        val regex = Regex("""versionCode=(\d+)""")
        return regex.find(output)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractActivities(output: String): List<ActivityInfo> {
        val activities = mutableListOf<ActivityInfo>()

        // Find Activity Resolver Table section
        val activitySection = extractSection(output, "Activity Resolver Table:", "Receiver Resolver Table:")
            ?: extractSection(output, "Activity Resolver Table:", "Service Resolver Table:")
            ?: return activities

        // Parse each activity with its intent filters
        val activityBlocks = parseActivityBlocks(activitySection)

        for ((activityName, filterBlocks) in activityBlocks) {
            val intentFilters = filterBlocks.map { parseIntentFilter(it) }
            activities.add(
                ActivityInfo(
                    name = activityName,
                    exported = null, // Not easily available from dumpsys
                    intentFilters = intentFilters
                )
            )
        }

        return activities
    }

    private fun extractSection(output: String, startMarker: String, endMarker: String): String? {
        val startIndex = output.indexOf(startMarker)
        if (startIndex == -1) return null

        val endIndex = output.indexOf(endMarker, startIndex)
        return if (endIndex == -1) {
            output.substring(startIndex)
        } else {
            output.substring(startIndex, endIndex)
        }
    }

    private fun parseActivityBlocks(section: String): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        val lines = section.lines()

        var currentActivity: String? = null
        var currentFilter = StringBuilder()
        var inFilter = false

        for (line in lines) {
            // Check for activity name pattern (e.g., "3853b45 net.ib.mn/.activity.StartupActivity filter c6ebba8")
            val activityMatch = Regex("""^\s+\w+\s+([a-zA-Z0-9_.]+/[a-zA-Z0-9_.]+)\s+filter""").find(line)
            if (activityMatch != null) {
                // Save previous filter if exists
                if (currentActivity != null && currentFilter.isNotEmpty()) {
                    result.getOrPut(currentActivity) { mutableListOf() }.add(currentFilter.toString())
                }
                currentActivity = activityMatch.groupValues[1]
                currentFilter = StringBuilder()
                inFilter = true // Start filter immediately since "filter" is in the same line
                continue
            }

            // Check for filter content lines (indented with Action:, Category:, Scheme:, etc.)
            if (inFilter && currentActivity != null) {
                // Stop filter when we hit a non-indented line or a new section
                if (line.isNotBlank() && !line.startsWith("          ") && !line.startsWith("\t")) {
                    // Check if this is a new activity line
                    if (Regex("""^\s+\w+\s+[a-zA-Z0-9_.]+/""").containsMatchIn(line)) {
                        // Will be handled in next iteration
                    } else if (!line.startsWith("        ")) {
                        // Not a deeply indented line, might be end of filter
                        inFilter = false
                    }
                }
                if (inFilter) {
                    currentFilter.appendLine(line)
                }
            }
        }

        // Don't forget the last filter
        if (currentActivity != null && currentFilter.isNotEmpty()) {
            result.getOrPut(currentActivity) { mutableListOf() }.add(currentFilter.toString())
        }

        return result
    }

    private fun parseIntentFilter(filterBlock: String): IntentFilterInfo {
        val actions = mutableListOf<String>()
        val categories = mutableListOf<String>()
        val schemes = mutableListOf<String>()
        val paths = mutableListOf<Triple<String?, String?, String?>>() // path, pathPrefix, pathPattern
        var host: String? = null
        var port: String? = null
        var autoVerify = false

        val lines = filterBlock.lines()

        for (line in lines) {
            val trimmed = line.trim()

            // Parse Action
            if (trimmed.startsWith("Action:")) {
                val action = trimmed.removePrefix("Action:").trim().removeSurrounding("\"")
                if (action.isNotEmpty()) actions.add(action)
            }

            // Parse Category
            if (trimmed.startsWith("Category:")) {
                val category = trimmed.removePrefix("Category:").trim().removeSurrounding("\"")
                if (category.isNotEmpty()) categories.add(category)
            }

            // Parse AutoVerify
            if (trimmed.contains("AutoVerify=true", ignoreCase = true)) {
                autoVerify = true
            }

            // Parse Scheme
            if (trimmed.startsWith("Scheme:")) {
                val scheme = trimmed.removePrefix("Scheme:").trim().removeSurrounding("\"")
                if (scheme.isNotEmpty()) schemes.add(scheme)
            }

            // Parse Authority (host:port) - format: Authority: "host": -1 or Authority: "host": port
            if (trimmed.startsWith("Authority:")) {
                val authorityMatch = Regex(""""([^"]+)":\s*(-?\d+)""").find(trimmed)
                if (authorityMatch != null) {
                    host = authorityMatch.groupValues[1]
                    val portStr = authorityMatch.groupValues[2]
                    port = if (portStr == "-1") null else portStr
                }
            }

            // Parse Path - format: Path: "PatternMatcher{LITERAL: /path}" or Path: "PatternMatcher{PREFIX: /path}"
            if (trimmed.startsWith("Path:")) {
                val pathMatch = Regex("""PatternMatcher\{(\w+):\s*([^}]+)\}""").find(trimmed)
                if (pathMatch != null) {
                    val pathType = pathMatch.groupValues[1]
                    val pathValue = pathMatch.groupValues[2].trim()
                    when (pathType.uppercase()) {
                        "LITERAL" -> paths.add(Triple(pathValue, null, null))
                        "PREFIX" -> paths.add(Triple(null, pathValue, null))
                        "GLOB", "ADVANCED_GLOB" -> paths.add(Triple(null, null, pathValue))
                    }
                }
            }

            // Parse PathPrefix (may also appear separately)
            if (trimmed.startsWith("PathPrefix:")) {
                val pathPrefix = trimmed.removePrefix("PathPrefix:").trim().removeSurrounding("\"")
                if (pathPrefix.isNotEmpty()) paths.add(Triple(null, pathPrefix, null))
            }

            // Parse PathPattern (may also appear separately)
            if (trimmed.startsWith("PathPattern:")) {
                val pathPattern = trimmed.removePrefix("PathPattern:").trim().removeSurrounding("\"")
                if (pathPattern.isNotEmpty()) paths.add(Triple(null, null, pathPattern))
            }
        }

        // Build data list - create entries for each scheme + path combination
        val dataList = mutableListOf<IntentDataInfo>()

        if (schemes.isEmpty()) {
            // No scheme, just create one entry if there are paths
            if (paths.isNotEmpty()) {
                for ((path, pathPrefix, pathPattern) in paths) {
                    dataList.add(IntentDataInfo(
                        scheme = null, host = host, port = port,
                        path = path, pathPrefix = pathPrefix, pathPattern = pathPattern,
                        mimeType = null
                    ))
                }
            }
        } else if (paths.isEmpty()) {
            // No paths, just create entries for schemes
            for (scheme in schemes) {
                dataList.add(IntentDataInfo(
                    scheme = scheme, host = host, port = port,
                    path = null, pathPrefix = null, pathPattern = null,
                    mimeType = null
                ))
            }
        } else {
            // Both schemes and paths exist - create combinations
            for (scheme in schemes) {
                for ((path, pathPrefix, pathPattern) in paths) {
                    dataList.add(IntentDataInfo(
                        scheme = scheme, host = host, port = port,
                        path = path, pathPrefix = pathPrefix, pathPattern = pathPattern,
                        mimeType = null
                    ))
                }
            }
        }

        return IntentFilterInfo(
            actions = actions,
            categories = categories,
            data = dataList,
            autoVerify = autoVerify
        )
    }

    private fun extractDeepLinks(activities: List<ActivityInfo>): List<DeepLinkInfo> {
        val deepLinks = mutableListOf<DeepLinkInfo>()

        for (activity in activities) {
            for (filter in activity.intentFilters) {
                // Only consider VIEW + BROWSABLE + DEFAULT filters
                if (!filter.isViewAction || !filter.isBrowsable) continue

                for (data in filter.data) {
                    if (data.scheme != null) {
                        deepLinks.add(
                            DeepLinkInfo(
                                scheme = data.scheme,
                                host = data.host,
                                path = data.path,
                                pathPrefix = data.pathPrefix,
                                pathPattern = data.pathPattern,
                                activityName = activity.name,
                                autoVerify = filter.autoVerify
                            )
                        )
                    }
                }
            }
        }

        return deepLinks
    }
}
