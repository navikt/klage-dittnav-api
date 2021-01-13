package no.nav.klage.clients.pdl

import io.github.resilience4j.kotlin.retry.executeFunction
import io.github.resilience4j.retry.Retry
import no.nav.klage.clients.StsClient
import no.nav.klage.util.*
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

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val secureLogger = getSecureLogger()
    }

    fun getPersonInfo(): HentPdlPersonResponse {
        var results = HentPdlPersonResponse(null, null)

        runCatching {
            retryPdl.executeFunction {

                results = pdlWebClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getToken()}")
                    .header("Nav-Consumer-Token", "Bearer ${stsClient.oidcToken()}")
                    .bodyValue(hentPersonQuery(tokenUtil.getSubject()))
                    .retrieve()
                    .bodyToMono<HentPdlPersonResponse>()
                    .block() ?: throw RuntimeException("Person not found")

            }
        }.onFailure {
            slackClient.postMessage("Kontakt med pdl feilet! (${causeClass(rootCause(it))})", Severity.ERROR)
            throw RuntimeException("PDL could not be reached")
        }

        return results
    }

    fun getPersonInfoWithSystemUser(fnr: String): HentPdlPersonResponse {
        var results = HentPdlPersonResponse(null, null)
        secureLogger.debug(
            "Getting personInfo from PDL using service user token. User: {}, Requested person: {}",
            tokenUtil.getSubject(), fnr
        )
        runCatching {
            retryPdl.executeFunction {

                results = pdlWebClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${stsClient.oidcToken()}")
                    .header("Nav-Consumer-Token", "Bearer ${stsClient.oidcToken()}")
                    .bodyValue(hentPersonQuery(fnr))
                    .retrieve()
                    .bodyToMono<HentPdlPersonResponse>()
                    .block() ?: throw RuntimeException("Person not found")

            }
        }.onFailure {
            slackClient.postMessage("Kontakt med pdl feilet! (${causeClass(rootCause(it))})", Severity.ERROR)
            throw RuntimeException("PDL could not be reached")
        }

        return results
    }

    fun getFullmektigInfoWithSystemUser(fnr: String): HentFullmektigResponse {
        var results = HentFullmektigResponse(null, null)
        secureLogger.debug(
            "Getting fullmektig info from PDL using service user token. User: {}, Requested person: {}",
            tokenUtil.getSubject(), fnr
        )
        runCatching {
            retryPdl.executeFunction {

                results = pdlWebClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${stsClient.oidcToken()}")
                    .header("Nav-Consumer-Token", "Bearer ${stsClient.oidcToken()}")
                    .bodyValue(hentFullmektigQuery(fnr))
                    .retrieve()
                    .bodyToMono<HentFullmektigResponse>()
                    .block() ?: throw RuntimeException("Person not found")

            }
        }.onFailure {
            slackClient.postMessage("Kontakt med pdl feilet! (${causeClass(rootCause(it))})", Severity.ERROR)
            throw RuntimeException("PDL could not be reached")
        }

        return results
    }
}
