package no.nav.klage.clients.pdl

import io.github.resilience4j.kotlin.retry.executeFunction
import io.github.resilience4j.retry.Retry
import no.nav.klage.clients.StsClient
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.causeClass
import no.nav.klage.util.rootCause
import no.nav.slackposter.Severity
import no.nav.slackposter.SlackClient
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class PdlClient(
        private val pdlWebClient: WebClient,
        private val tokenUtil: TokenUtil,
        private val stsClient: StsClient,
        private val slackClient: SlackClient,
        private val retryPdl: Retry
) {

    fun getPersonInfo(): HentPdlPersonResponse {
        var results = HentPdlPersonResponse(null, null)

        retryPdl.executeFunction {
            runCatching {
                results = pdlWebClient.post()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getToken()}")
                        .header("Nav-Consumer-Token", "Bearer ${stsClient.oidcToken()}")
                        .bodyValue(hentPersonQuery(tokenUtil.getSubject()))
                        .retrieve()
                        .bodyToMono<HentPdlPersonResponse>()
                        .block() ?: throw RuntimeException("Person not found")

            }.onFailure {
                slackClient.postMessage("Kontakt med pdl feilet! (${causeClass(rootCause(it))})", Severity.ERROR)
            }
        }

        return results
    }
}
