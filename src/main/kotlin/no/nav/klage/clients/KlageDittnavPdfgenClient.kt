package no.nav.klage.clients

import no.nav.klage.domain.PDFInput
import no.nav.klage.util.getLogger
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class KlageDittnavPdfgenClient(
    private val klageDittnavPdfgenWebClient: WebClient,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getKlagePDF(input: PDFInput): ByteArray {
        logger.debug("Creating PDF from klage.")
        return klageDittnavPdfgenWebClient.post()
            .uri { it.path("/klage").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(input)
            .retrieve()
            .bodyToMono<ByteArray>()
            .block() ?: throw RuntimeException("PDF could not be generated")
    }

    fun getAnkePDF(input: PDFInput): ByteArray {
        logger.debug("Creating PDF from anke.")
        return klageDittnavPdfgenWebClient.post()
            .uri { it.path("/anke").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(input)
            .retrieve()
            .bodyToMono<ByteArray>()
            .block() ?: throw RuntimeException("PDF could not be generated")
    }
}