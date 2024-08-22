package no.nav.klage.clients.pdl

import io.github.resilience4j.kotlin.retry.executeFunction
import io.github.resilience4j.retry.Retry
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.causeClass
import no.nav.klage.util.getSecureLogger
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
    private val slackClient: SlackClient,
    private val retryPdl: Retry
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val secureLogger = getSecureLogger()
    }

    fun getPersonInfoAsSystemUser(foedselsnummer: String): HentPdlPersonResponse {
        var results = HentPdlPersonResponse(null, null)

        runCatching {
            retryPdl.executeFunction {
                results = pdlWebClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithPdlScope()}")
                    .bodyValue(hentPersonQuery(fnr = foedselsnummer))
                    .retrieve()
                    .bodyToMono<HentPdlPersonResponse>()
                    .block() ?: throw RuntimeException("Person not found")

            }
        }.onFailure {
            slackClient.postMessage("Kontakt med pdl feilet! (${causeClass(rootCause(it))})", Severity.ERROR)
            secureLogger.error("PDL could not be reached", it)
            throw RuntimeException("PDL could not be reached")
        }

        return results
    }

    fun getPersonInfo(): HentPdlPersonResponse {
        var results = HentPdlPersonResponse(null, null)

        runCatching {
            retryPdl.executeFunction {
                results = pdlWebClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getOnBehalfOfTokenWithPdlScope()}")
                    .bodyValue(hentPersonQuery(tokenUtil.getSubject()))
                    .retrieve()
                    .bodyToMono<HentPdlPersonResponse>()
                    .block() ?: throw RuntimeException("Person not found")

            }
        }.onFailure {
            slackClient.postMessage("Kontakt med pdl feilet! (${causeClass(rootCause(it))})", Severity.ERROR)
            secureLogger.error("PDL could not be reached", it)
            throw RuntimeException("PDL could not be reached")
        }

        return results
    }
}
