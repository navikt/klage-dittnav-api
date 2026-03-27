package no.nav.klage.clients.clamav

import no.nav.klage.clients.createShortCircuitWebClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClamAvClientTest {
    @Test
    fun `ok response returns true`() {
        val clamClient = ClamAvClient(createShortCircuitWebClient(okResponse))

        assertFalse(clamClient.hasVirus(ByteArray(0)))
    }

    @Test
    fun `found response returns false`() {
        val clamClient = ClamAvClient(createShortCircuitWebClient(foundResponse))

        assertTrue(clamClient.hasVirus(ByteArray(0)))
    }


    @Language("json")
    private val okResponse = """
            [
              {
                "filename": "testfile",
                "result": "OK",
                "virus": "",
                "error": ""
              }
            ]
        """.trimIndent()

    @Language("json")
    private val foundResponse = """
            [
              {
                "filename": "testfile",
                "result": "FOUND",
                "virus": "Eicar-Test-Signature",
                "error": ""
              }
            ]
        """.trimIndent()
}
