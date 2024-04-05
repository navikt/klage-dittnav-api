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
        validateIdent(input.foedselsnummer)

        val klankePDF = if (input.type.name.contains("ETTERSENDELSE")) {
            klageDittnavPdfgenClient.getEttersendelsePDF(input.toPDFInput())
        } else {
            klageDittnavPdfgenClient.getKlageAnkePDF(input.toPDFInput())
        }

        if (input.innsendingsytelse == Innsendingsytelse.LONNSGARANTI && input.type == Type.KLAGE) {
            return klankePDF
        }

        val foerstesidePDF = foerstesidegeneratorClient.createFoersteside(input.toFoerstesideRequest())
        return mergeDocuments(foerstesidePDF, klankePDF)
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
        val text: String
        val arkivtittel: String
        val navSkjemaId: String
        val foerstesidetype: Foerstesidetype

        when (type) {
            Type.KLAGE -> {
                text = "Klageskjema"
                arkivtittel = "Klage"
                navSkjemaId = "NAV 90-00.08 K"
                foerstesidetype = Foerstesidetype.SKJEMA
            }
            Type.ANKE -> {
                text = "Ankeskjema"
                arkivtittel = "Anke"
                navSkjemaId = "NAV 90-00.08 A"
                foerstesidetype = Foerstesidetype.SKJEMA
            }
            Type.KLAGE_ETTERSENDELSE -> {
                text = "Ettersendelsesskjema"
                arkivtittel = "Ettersendelse til klage"
                navSkjemaId = "NAV 90-00.08 K"
                foerstesidetype = Foerstesidetype.ETTERSENDELSE
            }
            Type.ANKE_ETTERSENDELSE -> {
                text = "Ettersendelsesskjema"
                arkivtittel = "Ettersendelse til anke"
                navSkjemaId = "NAV 90-00.08 A"
                foerstesidetype = Foerstesidetype.ETTERSENDELSE
            }
        }

        val documentList = mutableListOf(text)
        if (hasVedlegg) {
            documentList += "Vedlegg"
        }
        return FoerstesideRequest(
            spraakkode = Spraakkode.valueOf(language.name),
            netsPostboks = "1400", //always
            bruker = Bruker(
                brukerId = foedselsnummer,
                brukerType = Brukertype.PERSON
            ),
            tema = innsendingsytelse.toTema().name,
            arkivtittel = arkivtittel,
            navSkjemaId = navSkjemaId,
            overskriftstittel = "$arkivtittel $navSkjemaId",
            dokumentlisteFoersteside = documentList,
            foerstesidetype = foerstesidetype,
            enhetsnummer = enhetsnummer,
        )
    }
}