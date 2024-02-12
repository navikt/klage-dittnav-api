package no.nav.klage.clients.sts

import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.klage.clients.StsClient
import no.nav.klage.clients.createFailingWebClient
import no.nav.klage.clients.createShortCircuitWebClient
import no.nav.klage.clients.createShortCircuitWebClientQueued
import no.nav.slackposter.SlackClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Duration

class StsClientTest {

    private val slackClient = SlackClient("", "", "", "")

    private val retryConfig: RetryConfig = RetryConfig
        .custom<RetryConfig>()
        .maxAttempts(5)
        .waitDuration(Duration.ofMillis(1))
        .retryExceptions(RuntimeException::class.java)
        .build()

    private val retryRegistry = RetryRegistry.of(retryConfig)

    private val retrySts: Retry = retryRegistry.retry("STS")

    @Test
    fun `should provide correct access token`() {
        val stsClient = StsClient(createShortCircuitWebClient(defaultToken), slackClient, retrySts)
        val token = stsClient.oidcToken()

        assertEquals("default access token", token)
    }

    @ExperimentalStdlibApi
    @Test
    fun `should refresh token`() {
        val stsDefaultClient =
            StsClient(createShortCircuitWebClientQueued(shortLivedToken, defaultToken), slackClient, retrySts)
        val token1 = stsDefaultClient.oidcToken()
        val token2 = stsDefaultClient.oidcToken()

        assertEquals("short lived token", token1)
        assertEquals("default access token", token2)
    }

    @ExperimentalStdlibApi
    @Test
    fun `should cache token`() {
        val stsDefaultClient =
            StsClient(createShortCircuitWebClientQueued(defaultToken, shortLivedToken), slackClient, retrySts)
        val token1 = stsDefaultClient.oidcToken()
        val token2 = stsDefaultClient.oidcToken()

        assertEquals(token1, token2)
    }

    @Test
    fun `should post to slack client when sts call fails`() {
        val webClient = createFailingWebClient()
        val mockedSlackClient = mockk<SlackClient>()
        val stsClient = StsClient(webClient, mockedSlackClient, retrySts)

        assertThrows(RuntimeException::class.java) {
            stsClient.oidcToken()
        }

        verify { mockedSlackClient.postMessage(any(), any()) }
    }

    @Test
    fun `should retry sts call 5 times on error`() {
        val webClient = spyk(createFailingWebClient())
        val mockedSlackClient = mockk<SlackClient>()
        val stsClient = StsClient(webClient, mockedSlackClient, retrySts)

        assertThrows(RuntimeException::class.java) {
            stsClient.oidcToken()
        }

        verify(exactly = 5) { webClient.get() }
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
