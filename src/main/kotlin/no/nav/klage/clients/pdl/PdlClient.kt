package no.nav.klage.clients.pdl

import no.nav.klage.clients.sts.StsClient
import no.nav.klage.util.TokenUtil
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class PdlClient(
    private val pdlWebClient: WebClient,
    private val tokenUtil: TokenUtil,
    private val stsClient: StsClient
) {

    fun getPersonInfo(): HentPdlPersonResponse {
        return pdlWebClient.post()
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getToken()}")
            .header("Nav-Consumer-Token", "Bearer ${stsClient.oidcToken()}")
            .bodyValue(hentPersonQuery(tokenUtil.getSubject()))
            .retrieve()
            .bodyToMono<HentPdlPersonResponse>()
            .block()
    }
}
