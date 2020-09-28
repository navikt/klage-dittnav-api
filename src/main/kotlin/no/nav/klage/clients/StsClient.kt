package no.nav.klage.clients

import io.github.resilience4j.kotlin.retry.executeFunction
import io.github.resilience4j.retry.Retry
import no.nav.klage.domain.OidcToken
import no.nav.klage.util.causeClass
import no.nav.klage.util.getLogger
import no.nav.klage.util.rootCause
import no.nav.slackposter.Severity
import no.nav.slackposter.SlackClient
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class StsClient(
    private val stsWebClient: WebClient,
    private val slackClient: SlackClient,
    private val retrySts: Retry
) {

    private var cachedOidcToken: OidcToken? = null

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun oidcToken(): String {

        if (cachedOidcToken.shouldBeRenewed()) {
            logger.debug("Getting token from STS")
            runCatching {
                retrySts.executeFunction {
                    cachedOidcToken = stsWebClient.get()
                        .uri { uriBuilder ->
                            uriBuilder
                                .queryParam("grant_type", "client_credentials")
                                .queryParam("scope", "openid")
                                .build()
                        }
                        .retrieve()
                        .bodyToMono<OidcToken>()
                        .block()
                }
            }.onFailure {
                slackClient.postMessage("Kontakt med sts feilet! (${causeClass(rootCause(it))})", Severity.ERROR)
                throw RuntimeException("STS could not be reached")
            }
        }

        return cachedOidcToken!!.token
    }

    private fun OidcToken?.shouldBeRenewed(): Boolean = this?.hasExpired() ?: true
}
