package no.nav.klage.clients.klageunleashproxy

import no.nav.klage.util.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class KlageUnleashProxyClient(
    private val klageUnleashProxyWebClient: WebClient,
    @Value($$"${NAIS_APP_NAME}")
    private val appName: String,
    @Value($$"${NAIS_POD_NAME}")
    private val podName: String,
) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    /** Returns false if the toggle cannot be read, so callers fall back to the default behaviour. */
    fun isEnabled(feature: String): Boolean =
        runCatching {
            klageUnleashProxyWebClient
                .post()
                .uri("/features/{feature}", feature)
                .bodyValue(UnleashProxyRequest(appName = appName, podName = podName))
                .retrieve()
                .bodyToMono<FeatureToggleResponse>()
                .block()
                ?.enabled ?: false
        }.getOrElse {
            logger.warn("Could not check feature toggle '$feature'. Returning false.", it)
            false
        }
}
