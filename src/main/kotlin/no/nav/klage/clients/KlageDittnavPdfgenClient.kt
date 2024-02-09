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

    fun getKlageAnkePDF(input: PDFInput): ByteArray {
        logger.debug("Creating PDF for ${input.type}.")
        return klageDittnavPdfgenWebClient.post()
            .uri { it.path("/klageanke").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(input)
            .retrieve()
            .bodyToMono<ByteArray>()
            .block() ?: throw RuntimeException("PDF could not be generated")
    }

    fun getEttersendelsePDF(input: PDFInput): ByteArray {
        logger.debug("Creating PDF for ettersendelse for ${input.type}")
        return klageDittnavPdfgenWebClient.post()
            .uri { it.path("/ettersendelse").build() }
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(input)
            .retrieve()
            .bodyToMono<ByteArray>()
            .block() ?: throw RuntimeException("PDF could not be generated")
    }
}