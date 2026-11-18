package no.nav.klage.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class FoerstesidegeneratorClientConfiguration(
    private val webClientBuilder: WebClient.Builder,
) {
    @Value($$"${FOERSTESIDEGENERATOR_BASE_URL}")
    private lateinit var foerstesidegeneratorUrl: String

    @Value($$"${KLAGE_FSS_PROXY_FOERSTESIDE_URL}")
    private lateinit var klageFssProxyFoerstesideUrl: String

    @Bean
    fun foerstesidegeneratorWebClient(): WebClient =
        webClientBuilder
            .clone()
            .baseUrl(foerstesidegeneratorUrl)
            .defaultHeader("Nav-Consumer-Id", "klage-dittnav-api")
            .build()

    @Bean
    fun klageFssProxyFoerstesideWebClient(): WebClient =
        webClientBuilder
            .clone()
            .baseUrl(klageFssProxyFoerstesideUrl)
            .build()
}
