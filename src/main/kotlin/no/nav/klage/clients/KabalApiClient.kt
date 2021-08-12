package no.nav.klage.clients

import no.nav.klage.domain.availableanke.AvailableAnke
import no.nav.klage.domain.exception.AvailableAnkeNotFoundException
import no.nav.klage.util.getLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.util.*

@Component
class KabalApiClient(
    private val kabalApiWebClient: WebClient,
    private val azureADClient: AzureADClient
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getAvailableAnkerForUser(fnr: String): List<AvailableAnke> {
        logger.debug("Fetching available anker for user with fnr {}", fnr)

        return kabalApiWebClient.get()
            .uri { it.path("/muliganke/{fnr}").build(fnr) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureADClient.kabalApiOidcToken()}")
            .retrieve()
            .bodyToMono<List<AvailableAnke>>()
            .block() ?: throw AvailableAnkeNotFoundException("Available anker could not be fetched")
    }

    fun getSpecificAvailableAnkeForUser(fnr: String, id: UUID): AvailableAnke {
        logger.debug("Fetching available anker for user with fnr {}", fnr)

        return kabalApiWebClient.get()
            .uri { it.path("/muliganke/{fnr}/{id}").build(fnr, id) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureADClient.kabalApiOidcToken()}")
            .retrieve()
            .bodyToMono<AvailableAnke>()
            .block() ?: throw AvailableAnkeNotFoundException("Available anker could not be fetched")

    }
}

