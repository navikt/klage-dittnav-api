package no.nav.klage.clients.foerstesidegenerator

import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideRequest
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getTeamLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class FoerstesidegeneratorClient(
    private val foerstesidegeneratorWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val teamLogger = getTeamLogger()
    }

    fun createFoersteside(foerstesideRequest: FoerstesideRequest): ByteArray {
        val result = runCatching {
            foerstesidegeneratorWebClient.post()
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithKlageFSSProxyScope()}")
                .bodyValue(foerstesideRequest)
                .retrieve()
                .bodyToMono<ByteArray>()
                .block()
        }.onFailure {
            teamLogger.error("Could not fetch foersteside", it)
            throw RuntimeException("Could not fetch foersteside. See team-logs for more information.")
        }
        return result.getOrNull() ?: throw RuntimeException("Null response when getting foersteside")
    }
}
