package com.manjee.linkops.data.analyzer

import com.manjee.linkops.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CertificateFingerprintComparatorTest {

    private val comparator = CertificateFingerprintComparator()

    private fun createContent(packageName: String, fingerprints: List<String>): AssetLinksContent {
        return AssetLinksContent(
            statements = listOf(
                AssetStatement(
                    relation = listOf("delegate_permission/common.handle_all_urls"),
                    target = AssetTarget(
                        namespace = "android_app",
                        packageName = packageName,
                        sha256CertFingerprints = fingerprints
                    )
                )
            )
        )
    }

    @Test
    fun `compare should return Match when fingerprints are equal`() {
        val fingerprint = "AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89"
        val content = createContent("com.example.app", listOf(fingerprint))

        val result = comparator.compare(fingerprint, "com.example.app", content)

        assertIs<FingerprintComparisonResult.Match>(result)
    }

    @Test
    fun `compare should match case-insensitive fingerprints`() {
        val localFp = "ab:cd:ef:01:23:45:67:89:ab:cd:ef:01:23:45:67:89:ab:cd:ef:01:23:45:67:89:ab:cd:ef:01:23:45:67:89"
        val remoteFp = "AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89"
        val content = createContent("com.example.app", listOf(remoteFp))

        val result = comparator.compare(localFp, "com.example.app", content)

        assertIs<FingerprintComparisonResult.Match>(result)
    }

    @Test
    fun `compare should match fingerprints with and without colons`() {
        val localFp = "ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789"
        val remoteFp = "AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89"
        val content = createContent("com.example.app", listOf(remoteFp))

        val result = comparator.compare(localFp, "com.example.app", content)

        assertIs<FingerprintComparisonResult.Match>(result)
    }

    @Test
    fun `compare should return Mismatch when fingerprints differ`() {
        val localFp = "AA:BB:CC:DD"
        val remoteFp = "11:22:33:44"
        val content = createContent("com.example.app", listOf(remoteFp))

        val result = comparator.compare(localFp, "com.example.app", content)

        assertIs<FingerprintComparisonResult.Mismatch>(result)
        assertEquals(localFp, result.localFingerprint)
        assertEquals(listOf(remoteFp), result.remoteFingerprints)
    }

    @Test
    fun `compare should match against any of multiple remote fingerprints`() {
        val localFp = "CC:DD:EE:FF"
        val content = createContent("com.example.app", listOf("AA:BB", "CC:DD:EE:FF", "11:22"))

        val result = comparator.compare(localFp, "com.example.app", content)

        assertIs<FingerprintComparisonResult.Match>(result)
    }

    @Test
    fun `compare should return NoLocalFingerprint when local is null`() {
        val content = createContent("com.example.app", listOf("AA:BB"))

        val result = comparator.compare(null, "com.example.app", content)

        assertIs<FingerprintComparisonResult.NoLocalFingerprint>(result)
    }

    @Test
    fun `compare should return RemoteUnavailable when content is null`() {
        val result = comparator.compare("AA:BB", "com.example.app", null)

        assertIs<FingerprintComparisonResult.RemoteUnavailable>(result)
    }

    @Test
    fun `compare should return NoRemoteFingerprint when package not found in content`() {
        val content = createContent("com.other.app", listOf("AA:BB"))

        val result = comparator.compare("AA:BB", "com.example.app", content)

        assertIs<FingerprintComparisonResult.NoRemoteFingerprint>(result)
    }

    @Test
    fun `compare should return NoRemoteFingerprint when content has empty fingerprints`() {
        val content = createContent("com.example.app", emptyList())

        val result = comparator.compare("AA:BB", "com.example.app", content)

        assertIs<FingerprintComparisonResult.NoRemoteFingerprint>(result)
    }

    @Test
    fun `normalizeFingerprint should remove colons and uppercase`() {
        val normalized = CertificateFingerprintComparator.normalizeFingerprint("ab:cd:ef:01")
        assertEquals("ABCDEF01", normalized)
    }

    @Test
    fun `normalizeFingerprint should handle already normalized fingerprint`() {
        val normalized = CertificateFingerprintComparator.normalizeFingerprint("ABCDEF01")
        assertEquals("ABCDEF01", normalized)
    }

    @Test
    fun `isMatch property should return true for Match`() {
        val match = FingerprintComparisonResult.Match
        assertTrue(match.isMatch)
    }

    @Test
    fun `isMatch property should return false for Mismatch`() {
        val mismatch = FingerprintComparisonResult.Mismatch("AA", listOf("BB"))
        assertTrue(!mismatch.isMatch)
    }
}
