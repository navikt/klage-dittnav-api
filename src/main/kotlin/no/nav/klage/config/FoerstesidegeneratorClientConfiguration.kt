package no.nav.klage.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class FoerstesidegeneratorClientConfiguration(private val webClientBuilder: WebClient.Builder) {

    @Value("\${FOERSTESIDEGENERATOR_BASE_URL}")
    private lateinit var url: String

    @Bean
    fun foerstesidegeneratorWebClient(): WebClient {
        return webClientBuilder
            .baseUrl(url)
            .build()
    }
}
