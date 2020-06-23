package no.nav.klage.clients.norg2

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class Norg2Client(
    private val norg2WebClient: WebClient
) {

    fun getEnhetsinfo(enhetId: Int): Norg2Response {
        return norg2WebClient.get()
            .uri {uriBuilder ->
                uriBuilder.pathSegment("enhet", "$enhetId").build()
            }
            .retrieve()
            .bodyToMono<Norg2Response>()
            .block() ?: throw RuntimeException("Error from NORG2")
    }

}
