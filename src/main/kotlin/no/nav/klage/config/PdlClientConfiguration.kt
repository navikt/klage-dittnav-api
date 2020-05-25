package no.nav.klage.config

import no.nav.klage.clients.sts.StsClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class PdlClientConfiguration {

    @Value("\${PDL_BASE_URL}")
    private lateinit var pdlUrl: String

    @Value("\${SERVICE_USER_USERNAME}")
    private lateinit var username: String

    @Value("\${PDL_APIKEY}")
    private lateinit var apiKey: String

    @Bean
    fun pdlWebClient(stsClient: StsClient): WebClient {
        return WebClient
            .builder()
            .baseUrl(pdlUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Nav-Consumer-Token", "Bearer ${stsClient.oidcToken()}")
            .defaultHeader("Nav-Consumer-Id", username)
            .defaultHeader("TEMA", "KLA")
            .defaultHeader("x-nav-apiKey", apiKey)
            .build()
    }
}
