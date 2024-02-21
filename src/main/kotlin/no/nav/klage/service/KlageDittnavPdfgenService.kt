package no.nav.klage.service

import no.nav.klage.clients.KlageDittnavPdfgenClient
import no.nav.klage.clients.foerstesidegenerator.FoerstesidegeneratorClient
import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideRequest
import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideRequest.*
import no.nav.klage.clients.foerstesidegenerator.domain.FoerstesideRequest.Bruker.Brukertype
import no.nav.klage.controller.view.*
import no.nav.klage.domain.Type
import no.nav.klage.domain.exception.InvalidIdentException
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.domain.toPDFInput
import no.nav.klage.util.isValidFnrOrDnr
import org.apache.pdfbox.io.IOUtils
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class KlageDittnavPdfgenService(
    private val klageDittnavPdfgenClient: KlageDittnavPdfgenClient,
    private val foerstesidegeneratorClient: FoerstesidegeneratorClient,
) {

    fun createKlankePdfWithFoersteside(input: OpenKlankeInput): ByteArray {
        if (input.type == null) {
            throw RuntimeException("type must be set")
        }
        validateIdent(input.foedselsnummer)

        val klankePDF = if (input.type.name.contains("ETTERSENDELSE")) {
            klageDittnavPdfgenClient.getEttersendelsePDF(input.toPDFInput())
        } else {
            klageDittnavPdfgenClient.getKlageAnkePDF(input.toPDFInput())
        }

        return if (input.innsendingsytelse != Innsendingsytelse.LONNSGARANTI && input.type == Type.KLAGE) {
            val foerstesidePDF = foerstesidegeneratorClient.createFoersteside(input.toFoerstesideRequest())
            mergeDocuments(foerstesidePDF, klankePDF)
        } else klankePDF
    }

    fun createFoerstesideForEttersendelse(input: OpenEttersendelseInput): ByteArray {
        validateIdent(input.foedselsnummer)
        return foerstesidegeneratorClient.createFoersteside(input.toFoerstesideRequest())
    }

    private fun mergeDocuments(foerstesidePDF: ByteArray, klageAnkePDF: ByteArray): ByteArray {
        val merger = PDFMergerUtility()
        val outputStream = ByteArrayOutputStream()
        merger.destinationStream = outputStream

        merger.addSource(RandomAccessReadBuffer(foerstesidePDF))
        merger.addSource(RandomAccessReadBuffer(klageAnkePDF))

        merger.mergeDocuments(IOUtils.createMemoryOnlyStreamCache())

        return outputStream.toByteArray()
    }

    private fun validateIdent(foedselsnummer: String) {
        if (!isValidFnrOrDnr(foedselsnummer)) {
            throw InvalidIdentException()
        }
    }

    private fun OpenKlankeInput.toFoerstesideRequest(): FoerstesideRequest {
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
            tema = innsendingsytelse.toTema().name,
            arkivtittel = "Klage",
            navSkjemaId = "NAV 90-00.08 K",
            overskriftstittel = "Klage NAV 90-00.08 K",
            dokumentlisteFoersteside = documentList,
            foerstesidetype = Foerstesidetype.SKJEMA,
            enhetsnummer = enhetsnummer,
        )
    }

    private fun OpenEttersendelseInput.toFoerstesideRequest(): FoerstesideRequest {
        return FoerstesideRequest(
            spraakkode = Spraakkode.NB,
            netsPostboks = "1400", //always
            bruker = Bruker(
                brukerId = foedselsnummer,
                brukerType = Brukertype.PERSON
            ),
            tema = innsendingsytelse.toTema().name,
            arkivtittel = "Ettersendelse til klage/anke",
            navSkjemaId = "NAV 90-00.08",
            overskriftstittel = "Ettersendelse til klage/anke NAV 90-00.08",
            dokumentlisteFoersteside = listOf("Vedlegg"),
            foerstesidetype = Foerstesidetype.ETTERSENDELSE,
            enhetsnummer = enhetsnummer,
        )
    }
}