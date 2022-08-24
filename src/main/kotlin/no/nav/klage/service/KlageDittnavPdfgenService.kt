package no.nav.klage.service

import no.nav.klage.clients.KlageDittnavPdfgenClient
import no.nav.klage.clients.foerstesidegenerator.FoerstesidegeneratorClient
import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideRequest
import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideRequest.*
import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideRequest.Bruker.Brukertype
import no.nav.klage.controller.view.OpenKlageInput
import no.nav.klage.domain.exception.InvalidIdentException
import no.nav.klage.domain.toPDFInput
import no.nav.klage.util.isValidFnrOrDnr
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

        validateIdent(input.foedselsnummer)

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

    private fun validateIdent(foedselsnummer: String) {
        if (!isValidFnrOrDnr(foedselsnummer)) {
            throw InvalidIdentException()
        }
    }

    private fun OpenKlageInput.toFoerstesideRequest(): FoerstesideRequest {
        val documentList = mutableListOf("NAV 90-00.08 Klage/anke")
        if (hasVedlegg) {
            documentList += "Vedlegg"
        }
        return FoerstesideRequest(
            spraakkode = Spraakkode.NB,
            netsPostboks = "1400", //always?
            bruker = Bruker(
                brukerId = foedselsnummer,
                brukerType = Brukertype.PERSON
            ),
            tema = tema.name,
            arkivtittel = "Klage/anke",
            navSkjemaId = "NAV 90-00.08",
            overskriftstittel = "Klage/anke NAV 90-00.08",
            dokumentlisteFoersteside = documentList,
            foerstesidetype = Foerstesidetype.SKJEMA,
        )
    }
}