package no.nav.klage.config

import no.nav.klage.services.sts.StsClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class SafClientConfiguration {

    @Value("\${SAF_BASE_URL}")
    private lateinit var safUrl: String

    @Value("\${SERVICE_USER_USERNAME}")
    private lateinit var username: String

    @Value("\${SAF_APIKEY}")
    private lateinit var apiKey: String

    @Bean
    fun safWebClient(stsClient: StsClient): WebClient =
        WebClient
            .builder()
            .baseUrl(safUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${stsClient.oidcToken()}")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Nav-Consumer-Id", username)
            .defaultHeader("Nav-Callid", "TODO From MDC")
            .defaultHeader("x-nav-apiKey", apiKey)
            .build()

}
