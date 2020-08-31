package no.nav.klage.clients.sts

import no.nav.klage.clients.StsClient
import no.nav.klage.clients.createShortCircuitWebClient
import no.nav.klage.clients.createShortCircuitWebClientQueued
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class StsClientTest {

    @Test
    fun `should provide correct access token`() {
        val stsClient = StsClient(createShortCircuitWebClient(defaultToken))
        val token = stsClient.oidcToken()

        assertEquals("default access token", token)
    }

    @ExperimentalStdlibApi
    @Test
    fun `should refresh token`() {
        val stsDefaultClient = StsClient(createShortCircuitWebClientQueued(shortLivedToken, defaultToken))
        val token1 = stsDefaultClient.oidcToken()
        val token2 = stsDefaultClient.oidcToken()

        assertEquals("short lived token", token1)
        assertEquals("default access token", token2)
    }

    @ExperimentalStdlibApi
    @Test
    fun `should cache token`() {
        val stsDefaultClient = StsClient(createShortCircuitWebClientQueued(defaultToken, shortLivedToken))
        val token1 = stsDefaultClient.oidcToken()
        val token2 = stsDefaultClient.oidcToken()

        assertEquals(token1, token2)
    }

    @Language("json")
    private val defaultToken = """
        {
          "access_token": "default access token",
          "token_type": "Bearer",
          "expires_in": 3600
        }
    """.trimIndent()

    @Language("json")
    private val shortLivedToken = """
        {
          "access_token": "short lived token",
          "token_type": "Bearer",
          "expires_in": 1
        }
    """.trimIndent()
}
