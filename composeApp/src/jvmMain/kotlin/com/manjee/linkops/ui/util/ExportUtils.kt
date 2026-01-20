package com.manjee.linkops.ui.util

import com.manjee.linkops.domain.model.ManifestAnalysisResult
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Utility functions for exporting analysis results
 */
object ExportUtils {

    /**
     * Generate markdown content from analysis result
     */
    fun generateMarkdown(result: ManifestAnalysisResult): String {
        val info = result.manifestInfo ?: return "# Error\nNo manifest information available."

        val sb = StringBuilder()

        // Header
        sb.appendLine("# Deep Link Analysis Report")
        sb.appendLine()
        sb.appendLine("Generated: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}")
        sb.appendLine()

        // Package Info
        sb.appendLine("## Package Information")
        sb.appendLine()
        sb.appendLine("| Property | Value |")
        sb.appendLine("|----------|-------|")
        sb.appendLine("| Package Name | `${info.packageName}` |")
        info.versionName?.let { sb.appendLine("| Version Name | $it |") }
        info.versionCode?.let { sb.appendLine("| Version Code | $it |") }
        sb.appendLine()

        // Summary
        sb.appendLine("## Summary")
        sb.appendLine()
        sb.appendLine("| Metric | Count |")
        sb.appendLine("|--------|-------|")
        sb.appendLine("| Total Deep Links | ${info.deepLinks.size} |")
        sb.appendLine("| App Links (Auto-Verified) | ${info.appLinks.size} |")
        sb.appendLine("| Custom Scheme Links | ${info.customSchemeLinks.size} |")
        sb.appendLine()

        if (info.schemes.isNotEmpty()) {
            sb.appendLine("**Schemes:** ${info.schemes.joinToString(", ") { "`$it`" }}")
            sb.appendLine()
        }

        // Domain Verification
        result.domainVerification?.let { verification ->
            if (verification.domains.isNotEmpty()) {
                sb.appendLine("## Domain Verification Status")
                sb.appendLine()
                sb.appendLine("| Domain | Status |")
                sb.appendLine("|--------|--------|")
                verification.domains.forEach { domain ->
                    val statusEmoji = when (domain.status.displayName) {
                        "verified", "always" -> "✅"
                        "none" -> "⚪"
                        else -> "❌"
                    }
                    sb.appendLine("| `${domain.domain}` | $statusEmoji ${domain.status.displayName} |")
                }
                sb.appendLine()
            }
        }

        // App Links
        if (info.appLinks.isNotEmpty()) {
            sb.appendLine("## App Links (Auto-Verified)")
            sb.appendLine()
            sb.appendLine("These links use `https://` scheme with `android:autoVerify=\"true\"`.")
            sb.appendLine()
            info.appLinks.forEach { link ->
                sb.appendLine("### ${link.patternDescription}")
                sb.appendLine()
                sb.appendLine("- **Activity:** `${link.activityName}`")
                sb.appendLine("- **Sample URI:** `${link.sampleUri}`")
                sb.appendLine()
            }
        }

        // Custom Scheme Links
        if (info.customSchemeLinks.isNotEmpty()) {
            sb.appendLine("## Custom Scheme Links")
            sb.appendLine()
            info.customSchemeLinks.forEach { link ->
                sb.appendLine("### ${link.patternDescription}")
                sb.appendLine()
                sb.appendLine("- **Scheme:** `${link.scheme}`")
                sb.appendLine("- **Activity:** `${link.activityName}`")
                sb.appendLine("- **Sample URI:** `${link.sampleUri}`")
                sb.appendLine()
            }
        }

        // HTTP Links (non-verified)
        val httpLinks = info.deepLinks.filter {
            (it.scheme == "http" || it.scheme == "https") && !it.autoVerify
        }
        if (httpLinks.isNotEmpty()) {
            sb.appendLine("## HTTP/HTTPS Links (Not Auto-Verified)")
            sb.appendLine()
            httpLinks.forEach { link ->
                sb.appendLine("### ${link.patternDescription}")
                sb.appendLine()
                sb.appendLine("- **Activity:** `${link.activityName}`")
                sb.appendLine("- **Sample URI:** `${link.sampleUri}`")
                sb.appendLine()
            }
        }

        // All Deep Links Table
        sb.appendLine("## All Deep Links")
        sb.appendLine()
        sb.appendLine("| Pattern | Activity | Auto-Verify |")
        sb.appendLine("|---------|----------|-------------|")
        info.deepLinks.forEach { link ->
            val verify = if (link.autoVerify) "✅" else "❌"
            val activityShort = link.activityName.substringAfterLast("/")
            sb.appendLine("| `${link.patternDescription}` | `$activityShort` | $verify |")
        }
        sb.appendLine()

        sb.appendLine("---")
        sb.appendLine("*Report generated by LinkOps*")

        return sb.toString()
    }

    /**
     * Show save dialog and save markdown file
     */
    fun saveMarkdown(result: ManifestAnalysisResult, parentFrame: Frame? = null): Boolean {
        val info = result.manifestInfo ?: return false
        val markdown = generateMarkdown(result)

        val dialog = FileDialog(parentFrame, "Save Markdown Report", FileDialog.SAVE).apply {
            file = "${info.packageName}_deeplinks.md"
            isVisible = true
        }

        val directory = dialog.directory
        val filename = dialog.file

        if (directory != null && filename != null) {
            val file = File(directory, filename)
            file.writeText(markdown)
            return true
        }
        return false
    }

    /**
     * Show save dialog and save PDF file
     */
    fun savePdf(result: ManifestAnalysisResult, parentFrame: Frame? = null): Boolean {
        val info = result.manifestInfo ?: return false

        val dialog = FileDialog(parentFrame, "Save PDF Report", FileDialog.SAVE).apply {
            file = "${info.packageName}_deeplinks.pdf"
            isVisible = true
        }

        val directory = dialog.directory
        val filename = dialog.file

        if (directory != null && filename != null) {
            val file = File(directory, filename)
            return generatePdf(result, file)
        }
        return false
    }

    /**
     * Generate a multi-page PDF from the analysis result
     */
    private fun generatePdf(result: ManifestAnalysisResult, file: File): Boolean {
        val info = result.manifestInfo ?: return false

        try {
            // Build all text lines for the PDF
            val lines = buildPdfLines(result)

            // PDF constants
            val pageWidth = 612
            val pageHeight = 792
            val margin = 50
            val lineHeight = 14
            val linesPerPage = (pageHeight - 2 * margin) / lineHeight

            // Split lines into pages
            val pages = lines.chunked(linesPerPage)

            file.outputStream().use { fos ->
                val writer = fos.bufferedWriter()

                // PDF Header
                writer.write("%PDF-1.4\n")

                // Track byte offsets for xref
                val offsets = mutableListOf<Int>()
                var currentOffset = 9 // After %PDF-1.4\n

                // Catalog object
                offsets.add(currentOffset)
                val catalogObj = "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
                writer.write(catalogObj)
                currentOffset += catalogObj.length

                // Pages object - list all page objects
                offsets.add(currentOffset)
                val pageRefs = (0 until pages.size).joinToString(" ") { "${it + 3} 0 R" }
                val pagesObj = "2 0 obj << /Type /Pages /Kids [$pageRefs] /Count ${pages.size} >> endobj\n"
                writer.write(pagesObj)
                currentOffset += pagesObj.length

                // Font object (will be last)
                val fontObjNum = 3 + pages.size * 2

                // Create page and content objects
                pages.forEachIndexed { pageIndex, pageLines ->
                    val pageObjNum = 3 + pageIndex * 2
                    val contentObjNum = pageObjNum + 1

                    // Page object
                    offsets.add(currentOffset)
                    val pageObj = "$pageObjNum 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 $pageWidth $pageHeight] /Contents $contentObjNum 0 R /Resources << /Font << /F1 $fontObjNum 0 R >> >> >> endobj\n"
                    writer.write(pageObj)
                    currentOffset += pageObj.length

                    // Content stream
                    val contentStream = buildPageContent(pageLines, margin, pageHeight - margin, lineHeight)
                    offsets.add(currentOffset)
                    val contentObj = "$contentObjNum 0 obj << /Length ${contentStream.length} >> stream\n$contentStream\nendstream endobj\n"
                    writer.write(contentObj)
                    currentOffset += contentObj.length
                }

                // Font object
                offsets.add(currentOffset)
                val fontObj = "$fontObjNum 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n"
                writer.write(fontObj)
                currentOffset += fontObj.length

                // Cross-reference table
                val xrefOffset = currentOffset
                writer.write("xref\n")
                writer.write("0 ${offsets.size + 1}\n")
                writer.write("0000000000 65535 f \n")
                offsets.forEach { offset ->
                    writer.write(String.format("%010d 00000 n \n", offset))
                }

                // Trailer
                writer.write("trailer << /Size ${offsets.size + 1} /Root 1 0 R >>\n")
                writer.write("startxref\n")
                writer.write("$xrefOffset\n")
                writer.write("%%EOF")

                writer.flush()
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Build all lines for PDF content
     */
    private fun buildPdfLines(result: ManifestAnalysisResult): List<PdfLine> {
        val info = result.manifestInfo ?: return listOf(PdfLine("No data", false))
        val lines = mutableListOf<PdfLine>()

        lines.add(PdfLine("Deep Link Analysis Report", isTitle = true))
        lines.add(PdfLine(""))
        lines.add(PdfLine("Generated: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}"))
        lines.add(PdfLine(""))

        // Package Info
        lines.add(PdfLine("Package Information", isHeader = true))
        lines.add(PdfLine(""))
        lines.add(PdfLine("Package Name: ${info.packageName}"))
        info.versionName?.let { lines.add(PdfLine("Version Name: $it")) }
        info.versionCode?.let { lines.add(PdfLine("Version Code: $it")) }
        lines.add(PdfLine(""))

        // Summary
        lines.add(PdfLine("Summary", isHeader = true))
        lines.add(PdfLine(""))
        lines.add(PdfLine("Total Deep Links: ${info.deepLinks.size}"))
        lines.add(PdfLine("App Links (Auto-Verified): ${info.appLinks.size}"))
        lines.add(PdfLine("Custom Scheme Links: ${info.customSchemeLinks.size}"))
        if (info.schemes.isNotEmpty()) {
            lines.add(PdfLine("Schemes: ${info.schemes.joinToString(", ")}"))
        }
        lines.add(PdfLine(""))

        // Domain Verification
        result.domainVerification?.let { verification ->
            if (verification.domains.isNotEmpty()) {
                lines.add(PdfLine("Domain Verification Status", isHeader = true))
                lines.add(PdfLine(""))
                verification.domains.forEach { domain ->
                    val status = when (domain.status.displayName) {
                        "verified", "always" -> "[OK]"
                        "none" -> "[--]"
                        else -> "[X]"
                    }
                    lines.add(PdfLine("$status ${domain.domain} - ${domain.status.displayName}"))
                }
                lines.add(PdfLine(""))
            }
        }

        // App Links
        if (info.appLinks.isNotEmpty()) {
            lines.add(PdfLine("App Links (Auto-Verified)", isHeader = true))
            lines.add(PdfLine(""))
            info.appLinks.forEach { link ->
                lines.add(PdfLine("Pattern: ${link.patternDescription}"))
                lines.add(PdfLine("  Activity: ${link.activityName.substringAfterLast("/")}"))
                lines.add(PdfLine("  Sample: ${link.sampleUri}"))
                lines.add(PdfLine(""))
            }
        }

        // Custom Scheme Links
        if (info.customSchemeLinks.isNotEmpty()) {
            lines.add(PdfLine("Custom Scheme Links", isHeader = true))
            lines.add(PdfLine(""))
            info.customSchemeLinks.forEach { link ->
                lines.add(PdfLine("Pattern: ${link.patternDescription}"))
                lines.add(PdfLine("  Scheme: ${link.scheme}"))
                lines.add(PdfLine("  Activity: ${link.activityName.substringAfterLast("/")}"))
                lines.add(PdfLine("  Sample: ${link.sampleUri}"))
                lines.add(PdfLine(""))
            }
        }

        // HTTP Links (non-verified)
        val httpLinks = info.deepLinks.filter {
            (it.scheme == "http" || it.scheme == "https") && !it.autoVerify
        }
        if (httpLinks.isNotEmpty()) {
            lines.add(PdfLine("HTTP/HTTPS Links (Not Auto-Verified)", isHeader = true))
            lines.add(PdfLine(""))
            httpLinks.forEach { link ->
                lines.add(PdfLine("Pattern: ${link.patternDescription}"))
                lines.add(PdfLine("  Activity: ${link.activityName.substringAfterLast("/")}"))
                lines.add(PdfLine("  Sample: ${link.sampleUri}"))
                lines.add(PdfLine(""))
            }
        }

        // All Deep Links summary table
        lines.add(PdfLine("All Deep Links Summary", isHeader = true))
        lines.add(PdfLine(""))
        info.deepLinks.forEach { link ->
            val verify = if (link.autoVerify) "[Verified]" else "[Not Verified]"
            val activity = link.activityName.substringAfterLast("/")
            lines.add(PdfLine("$verify ${link.patternDescription}"))
            lines.add(PdfLine("  -> $activity"))
        }
        lines.add(PdfLine(""))

        lines.add(PdfLine("---"))
        lines.add(PdfLine("Report generated by LinkOps"))

        return lines
    }

    /**
     * Build PDF content stream for a page
     */
    private fun buildPageContent(lines: List<PdfLine>, marginX: Int, startY: Int, lineHeight: Int): String {
        val sb = StringBuilder()
        sb.append("BT\n")

        var y = startY

        lines.forEach { line ->
            val fontSize = when {
                line.isTitle -> 16
                line.isHeader -> 13
                else -> 10
            }

            // Escape special PDF characters
            val escapedText = line.text
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")

            sb.append("/F1 $fontSize Tf\n")
            sb.append("$marginX $y Td\n")
            sb.append("($escapedText) Tj\n")
            sb.append("${-marginX} ${-y} Td\n") // Reset position

            y -= lineHeight
            if (line.isTitle || line.isHeader) {
                y -= 4 // Extra spacing after headers
            }
        }

        sb.append("ET")
        return sb.toString()
    }

    /**
     * Data class for PDF line with formatting info
     */
    private data class PdfLine(
        val text: String,
        val isTitle: Boolean = false,
        val isHeader: Boolean = false
    )
}
