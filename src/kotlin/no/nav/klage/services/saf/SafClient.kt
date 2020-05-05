package no.nav.klage.services.saf

import no.nav.klage.domain.Vedtak
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class SafClient(private val safWebClient: WebClient) {

    fun getVedtak(fnr: String): List<Vedtak> =
        getSafResponse(fnr).asVedtakList().filter {
            it.title.contains("Vedtak")
        }

    private fun getSafResponse(fnr: String): SafResponse? =
        safWebClient.post()
            .bodyValue(journalposterQuery(fnr))
            .retrieve()
            .bodyToMono<SafResponse>()
            .block()

    private fun SafResponse?.asVedtakList(): List<Vedtak> =
        this?.let {
            mapSafResponseToVedtak(it)
        } ?: listOf()
}
