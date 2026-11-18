package no.nav.klage.clients.foerstesidegenerator

import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideRequest
import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideResponse
import no.nav.klage.clients.klageunleashproxy.KlageUnleashProxyClient
import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getTeamLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class FoerstesidegeneratorClient(
    private val foerstesidegeneratorWebClient: WebClient,
    private val klageFssProxyFoerstesideWebClient: WebClient,
    private val tokenUtil: TokenUtil,
    private val klageUnleashProxyClient: KlageUnleashProxyClient,
) {
    companion object {
        const val FOERSTESIDE_DIRECT_TOGGLE = "foersteside-direct"

        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val teamLogger = getTeamLogger()
    }

    fun createFoersteside(foerstesideRequest: FoerstesideRequest): ByteArray {
        val direct = klageUnleashProxyClient.isEnabled(FOERSTESIDE_DIRECT_TOGGLE)
        logger.debug("Creating foersteside {}", if (direct) "directly" else "via klage-fss-proxy")

        val result =
            runCatching {
                if (direct) {
                    createFoerstesideDirectly(foerstesideRequest)
                } else {
                    createFoerstesideViaKlageFssProxy(foerstesideRequest)
                }
            }.onFailure {
                teamLogger.error("Could not fetch foersteside", it)
                throw RuntimeException("Could not fetch foersteside. See team-logs for more information.")
            }
        return result.getOrNull() ?: throw RuntimeException("Null response when getting foersteside")
    }

    private fun createFoerstesideDirectly(foerstesideRequest: FoerstesideRequest): ByteArray? =
        foerstesidegeneratorWebClient
            .post()
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithFoerstesidegeneratorScope()}")
            .bodyValue(foerstesideRequest)
            .retrieve()
            .bodyToMono<FoerstesideResponse>()
            .block()
            ?.foersteside

    private fun createFoerstesideViaKlageFssProxy(foerstesideRequest: FoerstesideRequest): ByteArray? =
        klageFssProxyFoerstesideWebClient
            .post()
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getAppAccessTokenWithKlageFssProxyScope()}")
            .bodyValue(foerstesideRequest)
            .retrieve()
            .bodyToMono<ByteArray>()
            .block()
}
