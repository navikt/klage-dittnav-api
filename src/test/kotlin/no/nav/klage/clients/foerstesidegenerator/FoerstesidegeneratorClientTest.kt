package no.nav.klage.clients.foerstesidegenerator

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.klage.clients.createShortCircuitWebClient
import no.nav.klage.clients.foerstesidegenerator.FoerstesidegeneratorClient.Companion.FOERSTESIDE_DIRECT_TOGGLE
import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideRequest
import no.nav.klage.clients.klageunleashproxy.KlageUnleashProxyClient
import no.nav.klage.util.TokenUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.Base64

class FoerstesidegeneratorClientTest {
    private val pdf = "direct-pdf".toByteArray()
    private val proxyPdf = "proxy-pdf".toByteArray()

    private val directWebClient =
        createShortCircuitWebClient(
            """{"foersteside":"${Base64.getEncoder().encodeToString(pdf)}","loepenummer":"123"}""",
        )

    private val proxyWebClient =
        WebClient
            .builder()
            .exchangeFunction(
                ExchangeFunction {
                    Mono.just(
                        ClientResponse
                            .create(HttpStatus.OK)
                            .header("Content-Type", "application/pdf")
                            .body(String(proxyPdf))
                            .build(),
                    )
                },
            ).build()

    private val tokenUtil =
        mockk<TokenUtil> {
            every { getAppAccessTokenWithFoerstesidegeneratorScope() } returns "direct-token"
            every { getAppAccessTokenWithKlageFssProxyScope() } returns "proxy-token"
        }

    private val unleash = mockk<KlageUnleashProxyClient>()

    private val client =
        FoerstesidegeneratorClient(
            foerstesidegeneratorWebClient = directWebClient,
            klageFssProxyFoerstesideWebClient = proxyWebClient,
            tokenUtil = tokenUtil,
            klageUnleashProxyClient = unleash,
        )

    @Test
    fun `calls foerstesidegenerator directly when toggle is on`() {
        every { unleash.isEnabled(FOERSTESIDE_DIRECT_TOGGLE) } returns true

        assertThat(client.createFoersteside(FoerstesideRequest())).isEqualTo(pdf)
        verify(exactly = 0) { tokenUtil.getAppAccessTokenWithKlageFssProxyScope() }
    }

    @Test
    fun `calls klage-fss-proxy when toggle is off`() {
        every { unleash.isEnabled(FOERSTESIDE_DIRECT_TOGGLE) } returns false

        assertThat(client.createFoersteside(FoerstesideRequest())).isEqualTo(proxyPdf)
        verify(exactly = 0) { tokenUtil.getAppAccessTokenWithFoerstesidegeneratorScope() }
    }
}
