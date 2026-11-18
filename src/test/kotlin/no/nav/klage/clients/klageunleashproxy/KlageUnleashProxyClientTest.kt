package no.nav.klage.clients.klageunleashproxy

import no.nav.klage.clients.createFailingWebClient
import no.nav.klage.clients.createShortCircuitWebClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class KlageUnleashProxyClientTest {
    @Test
    fun `returns toggle value from proxy`() {
        val client = klageUnleashProxyClient(createShortCircuitWebClient("""{"enabled":true}"""))

        assertThat(client.isEnabled("foersteside-direct")).isTrue()
    }

    @Test
    fun `returns false when proxy fails`() {
        val client = klageUnleashProxyClient(createFailingWebClient())

        assertThat(client.isEnabled("foersteside-direct")).isFalse()
    }

    private fun klageUnleashProxyClient(webClient: WebClient) =
        KlageUnleashProxyClient(
            klageUnleashProxyWebClient = webClient,
            appName = "klage-dittnav-api",
            podName = "klage-dittnav-api-test",
        )
}
