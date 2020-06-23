package no.nav.klage.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class Norg2ClientConfiguration {

    @Value("\${NORG2_BASE_URL}")
    private lateinit var norg2Url: String

    @Value("\${SERVICE_USER_USERNAME}")
    private lateinit var username: String

    @Value("\${NORG2_APIKEY}")
    private lateinit var apiKey: String

    @Bean
    fun norg2WebClient(): WebClient {
        return WebClient
            .builder()
            .baseUrl(norg2Url)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Nav-Consumer-Id", username)
            .defaultHeader("x-nav-apiKey", apiKey)
            .build()
    }

}
