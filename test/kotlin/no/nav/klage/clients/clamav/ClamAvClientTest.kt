package no.nav.klage.clients.clamav

import no.nav.klage.clients.createShortCircuitWebClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse

class ClamAvClientTest {
    @Test
    fun `ok response returns true`() {
        val clamClient = ClamAvClient(createShortCircuitWebClient(okResponse))

        assertTrue(clamClient.scan(ByteArray(0)))
    }

    @Test
    fun `found response returns false`() {
        val clamClient = ClamAvClient(createShortCircuitWebClient(foundResponse))

        assertFalse(clamClient.scan(ByteArray(0)))
    }

    @Test
    fun `response with multiple entries returns false`() {
        val clamClient = ClamAvClient(createShortCircuitWebClient(multipleResponse))

        assertFalse(clamClient.scan(ByteArray(0)))
    }


    @Language("json")
    private val okResponse = """
            [
              {
                "Filename": "testfile",
                "Result": "OK"
              }
            ]
        """.trimIndent()

    @Language("json")
    private val foundResponse = """
            [
              {
                "Filename": "testfile",
                "Result": "FOUND"
              }
            ]
        """.trimIndent()

    @Language("json")
    private val multipleResponse = """
            [
              {
                "Filename": "testfile",
                "Result": "FOUND"
              },
              {
                "Filename": "testfile",
                "Result": "OK"
              }
            ]
        """.trimIndent()
}
