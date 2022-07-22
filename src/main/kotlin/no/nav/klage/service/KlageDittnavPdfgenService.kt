package no.nav.klage.service

import no.nav.klage.clients.KlageDittnavPdfgenClient
import no.nav.klage.clients.foerstesidegenerator.FoerstesidegeneratorClient
import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideRequest
import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideRequest.*
import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideRequest.Bruker.Brukertype
import no.nav.klage.controller.view.OpenKlageInput
import no.nav.klage.domain.toPDFInput
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Service
class KlageDittnavPdfgenService(
    private val klageDittnavPdfgenClient: KlageDittnavPdfgenClient,
    private val foerstesidegeneratorClient: FoerstesidegeneratorClient,
) {

    fun createKlagePdfWithFoersteside(input: OpenKlageInput): ByteArray {
        val klagePDF = klageDittnavPdfgenClient.getKlagePDF(input.toPDFInput())
        val foerstesidePDF = foerstesidegeneratorClient.createFoersteside(input.toFoerstesideRequest())

        val merger = PDFMergerUtility()
        val outputStream = ByteArrayOutputStream()
        merger.destinationStream = outputStream

        merger.addSource(ByteArrayInputStream(foerstesidePDF))
        merger.addSource(ByteArrayInputStream(klagePDF))

        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())

        return outputStream.toByteArray()
    }

    private fun OpenKlageInput.toFoerstesideRequest(): FoerstesideRequest {
        return FoerstesideRequest(
            spraakkode = Spraakkode.NB,
            netsPostboks = "1400", //always?
            bruker = Bruker(
                brukerId = foedselsnummer,
                brukerType = Brukertype.PERSON
            ),
            tema = tema.name,
            arkivtittel = "Klage/anke",
            vedleggsliste = if (hasVedlegg) listOf("Annet") else emptyList(),
            navSkjemaId = "NAV 90-00.08",
            overskriftstittel = "En tittel", //what to put here?
            dokumentlisteFoersteside = listOf("NAV 90-00.08 Klage/anke", "Annet"),
            foerstesidetype = Foerstesidetype.SKJEMA,
        )
    }
}