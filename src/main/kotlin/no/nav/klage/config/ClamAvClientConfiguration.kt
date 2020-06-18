package no.nav.klage.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class ClamAvClientConfiguration {

    @Value("\${CLAM_AV_URL}")
    private lateinit var url: String

    @Bean
    fun clamAvWebClient(): WebClient {
        return WebClient
            .builder()
            .baseUrl(url)
            .build()
    }
}
