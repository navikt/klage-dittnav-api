package no.nav.klage.service

import no.nav.klage.clients.KlageDittnavPdfgenClient
import no.nav.klage.clients.foerstesidegenerator.FoerstesidegeneratorClient
import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideRequest
import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideRequest.*
import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideRequest.Bruker.Brukertype
import no.nav.klage.controller.view.OpenAnkeInput
import no.nav.klage.controller.view.OpenEttersendelseInput
import no.nav.klage.controller.view.OpenKlageInput
import no.nav.klage.domain.exception.InvalidIdentException
import no.nav.klage.domain.titles.Innsendingsytelse
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

        val klagePDF = klageDittnavPdfgenClient.getKlagePDF(input.toPDFInput(sendesIPosten = true))

        return if (input.innsendingsytelse != Innsendingsytelse.LONNSGARANTI) {
            val foerstesidePDF = foerstesidegeneratorClient.createFoersteside(input.toFoerstesideRequest())
            mergeDocuments(foerstesidePDF, klagePDF)
        } else klagePDF
    }

    fun createAnkePdfWithFoersteside(input: OpenAnkeInput): ByteArray {
        validateIdent(input.foedselsnummer)

        val ankePDF = klageDittnavPdfgenClient.getAnkePDF(input.toPDFInput(sendesIPosten = true))
        val foerstesidePDF = foerstesidegeneratorClient.createFoersteside(input.toFoerstesideRequest())

        return mergeDocuments(foerstesidePDF = foerstesidePDF, klageAnkePDF = ankePDF)
    }

    fun createFoerstesideForEttersendelse(input: OpenEttersendelseInput): ByteArray {
        validateIdent(input.foedselsnummer)
        return foerstesidegeneratorClient.createFoersteside(input.toFoerstesideRequest())
    }

    private fun mergeDocuments(foerstesidePDF: ByteArray, klageAnkePDF: ByteArray): ByteArray {
        val merger = PDFMergerUtility()
        val outputStream = ByteArrayOutputStream()
        merger.destinationStream = outputStream

        merger.addSource(ByteArrayInputStream(foerstesidePDF))
        merger.addSource(ByteArrayInputStream(klageAnkePDF))

        merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly())

        return outputStream.toByteArray()
    }

    private fun validateIdent(foedselsnummer: String) {
        if (!isValidFnrOrDnr(foedselsnummer)) {
            throw InvalidIdentException()
        }
    }

    private fun OpenKlageInput.toFoerstesideRequest(): FoerstesideRequest {
        val innsendingsytelse = innsendingsytelse ?: titleKey ?: error("innsendingsytelse or titleKey must be set")
        val documentList = mutableListOf("Klagen din")
        if (hasVedlegg) {
            documentList += "Vedlegg"
        }
        return FoerstesideRequest(
            spraakkode = Spraakkode.NB,
            netsPostboks = "1400", //always
            bruker = Bruker(
                brukerId = foedselsnummer,
                brukerType = Brukertype.PERSON
            ),
            tema = innsendingsytelse.getTema().name,
            arkivtittel = "Klage",
            navSkjemaId = "NAV 90-00.08 K",
            overskriftstittel = "Klage NAV 90-00.08 K",
            dokumentlisteFoersteside = documentList,
            foerstesidetype = Foerstesidetype.SKJEMA,
        )
    }

    private fun OpenAnkeInput.toFoerstesideRequest(): FoerstesideRequest {
        val innsendingsytelse = innsendingsytelse ?: titleKey ?: error("innsendingsytelse or titleKey must be set")
        val documentList = mutableListOf("Anken din")
        if (hasVedlegg) {
            documentList += "Vedlegg"
        }
        return FoerstesideRequest(
            spraakkode = Spraakkode.NB,
            netsPostboks = "1400", //always
            bruker = Bruker(
                brukerId = foedselsnummer,
                brukerType = Brukertype.PERSON
            ),
            tema = innsendingsytelse.getTema().name,
            arkivtittel = "Anke",
            navSkjemaId = "NAV 90-00.08 A",
            overskriftstittel = "Anke NAV 90-00.08 A",
            dokumentlisteFoersteside = documentList,
            foerstesidetype = Foerstesidetype.SKJEMA,
            enhetsnummer = enhetsnummer,
        )
    }

    private fun OpenEttersendelseInput.toFoerstesideRequest(): FoerstesideRequest {
        val tema = innsendingsytelse?.getTema() ?: (tema ?: error("innsendingsytelse or tema must be set"))
        return FoerstesideRequest(
            spraakkode = Spraakkode.NB,
            netsPostboks = "1400", //always
            bruker = Bruker(
                brukerId = foedselsnummer,
                brukerType = Brukertype.PERSON
            ),
            tema = tema.name,
            arkivtittel = "Ettersendelse til klage/anke",
            navSkjemaId = "NAV 90-00.08",
            overskriftstittel = "Ettersendelse til klage/anke NAV 90-00.08",
            dokumentlisteFoersteside = listOf("Vedlegg"),
            foerstesidetype = Foerstesidetype.ETTERSENDELSE,
            enhetsnummer = enhetsnummer,
        )
    }
}