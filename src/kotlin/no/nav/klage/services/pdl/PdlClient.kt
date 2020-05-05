package no.nav.klage.services.pdl

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class PdlClient(private val pdlWebClient: WebClient) {

    fun getPersonInfo(fnr: String): HentPdlPersonResponse {
        return pdlWebClient.post()
            .bodyValue(hentPersonQuery(fnr))
            .retrieve()
            .bodyToMono<HentPdlPersonResponse>()
            .block()

    }
}