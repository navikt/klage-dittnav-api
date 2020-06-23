package no.nav.klage.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class VedleggClientConfiguration {
    @Value("\${ATTACHMENT_BASE_URL}")
    private lateinit var url: String

    @Bean
    fun vedleggWebClient(): WebClient =
        WebClient
            .builder()
            .baseUrl(url)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
}
