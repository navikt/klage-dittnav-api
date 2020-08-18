package no.nav.klage.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class FileClientConfiguration(private val webClientBuilder: WebClient.Builder) {

    @Value("\${FILE_BASE_URL}")
    private lateinit var url: String

    @Bean
    fun fileWebClient(): WebClient =
        webClientBuilder
            .baseUrl(url)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
}
