package com.manjee.linkops.data.parser

import com.manjee.linkops.domain.model.VerificationState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GetAppLinksParserTest {

    private val parser = GetAppLinksParser()

    @Test
    fun `parse should return empty list for empty input`() {
        val result = parser.parse("")
        assertTrue(result.isEmpty(), "Empty input should produce empty list")
    }

    @Test
    fun `parse should return empty list for ADB error message`() {
        val result = parser.parse("error: device not found")
        assertTrue(result.isEmpty(), "ADB error should produce empty list")
    }

    @Test
    fun `parse should return empty list for permission denied response`() {
        val result = parser.parse("Permission denied")
        assertTrue(result.isEmpty(), "Permission denied should produce empty list")
    }

    @Test
    fun `parse should return empty list for operation not permitted response`() {
        val result = parser.parse("Operation not permitted")
        assertTrue(result.isEmpty(), "Operation not permitted should produce empty list")
    }

    @Test
    fun `parse should handle single package with verified domain`() {
        val output = """
            com.example.app:
              ID: 12345678-1234-1234-1234-123456789012
              Signatures: [AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89]
              Domain verification state:
                example.com: verified
        """.trimIndent()

        val result = parser.parse(output)

        assertEquals(1, result.size, "Should parse one package")
        assertEquals("com.example.app", result[0].packageName)
        assertEquals(1, result[0].domains.size)
        assertEquals("example.com", result[0].domains[0].domain)
        assertEquals(VerificationState.VERIFIED, result[0].domains[0].verificationState)
        assertEquals(
            "AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89:AB:CD:EF:01:23:45:67:89",
            result[0].domains[0].fingerprint
        )
    }

    @Test
    fun `parse should handle multiple domains with different states`() {
        val output = """
            com.example.app:
              ID: 12345678-1234-1234-1234-123456789012
              Signatures: [AA:BB:CC:DD]
              Domain verification state:
                example.com: verified
                test.example.com: none
                staging.example.com: legacy_failure
        """.trimIndent()

        val result = parser.parse(output)

        assertEquals(1, result.size)
        assertEquals(3, result[0].domains.size)
        assertEquals(VerificationState.VERIFIED, result[0].domains[0].verificationState)
        assertEquals(VerificationState.UNVERIFIED, result[0].domains[1].verificationState)
        assertEquals(VerificationState.LEGACY_FAILURE, result[0].domains[2].verificationState)
    }

    @Test
    fun `parse should attach fingerprint to all domains of a package`() {
        val fingerprint = "AA:BB:CC:DD:EE:FF"
        val output = """
            com.example.app:
              ID: 12345678
              Signatures: [$fingerprint]
              Domain verification state:
                example.com: verified
                test.example.com: none
        """.trimIndent()

        val result = parser.parse(output)

        assertEquals(1, result.size)
        result[0].domains.forEach { domain ->
            assertEquals(fingerprint, domain.fingerprint, "Each domain should have the fingerprint")
        }
    }

    @Test
    fun `parse should handle multiple packages`() {
        val output = """
            com.example.app1:
              ID: 11111111
              Signatures: [AA:BB]
              Domain verification state:
                example.com: verified
            com.example.app2:
              ID: 22222222
              Signatures: [CC:DD]
              Domain verification state:
                test.com: none
        """.trimIndent()

        val result = parser.parse(output)

        assertEquals(2, result.size)
        assertEquals("com.example.app1", result[0].packageName)
        assertEquals("AA:BB", result[0].domains[0].fingerprint)
        assertEquals("com.example.app2", result[1].packageName)
        assertEquals("CC:DD", result[1].domains[0].fingerprint)
    }

    @Test
    fun `parse should handle package without signatures field`() {
        val output = """
            com.example.app:
              ID: 12345678
              Domain verification state:
                example.com: verified
        """.trimIndent()

        val result = parser.parse(output)

        assertEquals(1, result.size)
        assertNull(result[0].domains[0].fingerprint, "Fingerprint should be null when Signatures is missing")
    }

    @Test
    fun `parse should handle empty signatures brackets`() {
        val output = """
            com.example.app:
              ID: 12345678
              Signatures: []
              Domain verification state:
                example.com: verified
        """.trimIndent()

        val result = parser.parse(output)

        assertEquals(1, result.size)
        assertNull(result[0].domains[0].fingerprint, "Empty brackets should produce null fingerprint")
    }

    @Test
    fun `parse should handle unknown verification state`() {
        val output = """
            com.example.app:
              ID: 12345678
              Signatures: [AA:BB]
              Domain verification state:
                example.com: some_future_state
        """.trimIndent()

        val result = parser.parse(output)

        assertEquals(1, result.size)
        assertEquals(VerificationState.UNKNOWN, result[0].domains[0].verificationState)
    }

    @Test
    fun `parse should handle truncated output gracefully`() {
        val output = """
            com.example.app:
              ID: 12345678
              Signatures: [AA:BB]
              Domain verification sta
        """.trimIndent()

        val result = parser.parse(output)
        // Truncated output without complete domain section should yield empty domains
        assertTrue(result.isEmpty(), "Truncated output should not produce incomplete results")
    }

    @Test
    fun `parse should handle special characters in output`() {
        val output = """
            com.example.app:
              ID: 12345678
              Signatures: [AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99]
              Domain verification state:
                sub-domain.example.co.uk: verified
        """.trimIndent()

        val result = parser.parse(output)

        assertEquals(1, result.size)
        assertEquals("sub-domain.example.co.uk", result[0].domains[0].domain)
    }

    @Test
    fun `parse should handle duplicate domain entries`() {
        val output = """
            com.example.app:
              ID: 12345678
              Signatures: [AA:BB]
              Domain verification state:
                example.com: verified
                example.com: none
        """.trimIndent()

        val result = parser.parse(output)

        assertEquals(1, result.size)
        // Both entries should be preserved (real ADB output could have this)
        assertEquals(2, result[0].domains.size)
    }

    @Test
    fun `parse should skip package with no domains`() {
        val output = """
            com.example.app:
              ID: 12345678
              Signatures: [AA:BB]
              Domain verification state:
        """.trimIndent()

        val result = parser.parse(output)

        assertTrue(result.isEmpty(), "Package with no domains should be skipped")
    }

    @Test
    fun `parse should handle error closed response`() {
        val result = parser.parse("error: closed")
        assertTrue(result.isEmpty(), "ADB error:closed should produce empty list")
    }

    @Test
    fun `parse should handle all verification state values`() {
        val output = """
            com.example.app:
              ID: 12345678
              Signatures: [AA:BB]
              Domain verification state:
                a.example.com: verified
                b.example.com: none
                c.example.com: legacy_failure
                d.example.com: restored
        """.trimIndent()

        val result = parser.parse(output)

        assertEquals(1, result.size)
        assertEquals(4, result[0].domains.size)
        assertEquals(VerificationState.VERIFIED, result[0].domains[0].verificationState)
        assertEquals(VerificationState.UNVERIFIED, result[0].domains[1].verificationState)
        assertEquals(VerificationState.LEGACY_FAILURE, result[0].domains[2].verificationState)
        assertEquals(VerificationState.UNKNOWN, result[0].domains[3].verificationState)
    }
}
