package no.nav.klage.clients

import no.nav.klage.domain.availableanke.AvailableAnke
import no.nav.klage.domain.exception.AvailableAnkeNotFoundException
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
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
        private val secureLogger = getSecureLogger()
    }

    fun getAvailableAnkerForUser(fnr: String): List<AvailableAnke> {
        logger.debug("Fetching all available anker from kabal-api for current user")
        secureLogger.debug("Fetching all available anker from kabal-api for user {}", fnr)


        return kabalApiWebClient.get()
            .uri { it.path("/muliganke/{fnr}").build(fnr) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureADClient.kabalApiOidcToken()}")
            .retrieve()
            .bodyToMono<List<AvailableAnke>>()
            .block() ?: throw AvailableAnkeNotFoundException("Available anker could not be fetched from kabal-api")
    }

    fun getSpecificAvailableAnkeForUser(fnr: String, id: UUID): AvailableAnke {
        logger.debug("Fetching specific anke {} from kabal-api for current user", id)
        secureLogger.debug("Fetching specific available anke {} from kabal-api for user with fnr {}", id, fnr)

        return kabalApiWebClient.get()
            .uri { it.path("/muliganke/{fnr}/{id}").build(fnr, id) }
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureADClient.kabalApiOidcToken()}")
            .retrieve()
            .bodyToMono<AvailableAnke>()
            .block() ?: throw AvailableAnkeNotFoundException("Available anke with id $id could not be fetched from kabal-api")

    }
}

