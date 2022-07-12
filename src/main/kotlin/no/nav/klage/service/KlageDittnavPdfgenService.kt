package no.nav.klage.service

import no.nav.klage.clients.KlageDittnavPdfgenClient
import no.nav.klage.controller.view.OpenKlageInput
import no.nav.klage.domain.toPDFInput
import org.springframework.stereotype.Service

@Service
class KlageDittnavPdfgenService(
    private val klageDittnavPdfgenClient: KlageDittnavPdfgenClient
) {

    fun createKlagePdf(input: OpenKlageInput): ByteArray {
        return klageDittnavPdfgenClient.getKlagePDF(input.toPDFInput())
    }
}