package no.nav.klage.clients.saf

import no.nav.klage.util.TokenUtil
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class SafSelvbetjeningClient(
    private val safSelvbetjeningWebClient: WebClient,
    private val tokenUtil: TokenUtil,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    fun getDocument(getDocumentRequest: GetDocumentRequest): ByteArray {
        return safSelvbetjeningWebClient.get()
            .uri { it.path("/rest/hentdokument/${getDocumentRequest.journalpostId}/${getDocumentRequest.dokumentInfoId}/${getDocumentRequest.variantFormat}}").build() }
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenUtil.getOnBehalfOfTokenWithSafSelvbetjeningScope()}")
            .retrieve()
            .bodyToMono<ByteArray>()
            .block() ?: throw RuntimeException("File could not be fetched")
    }
}