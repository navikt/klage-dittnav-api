package no.nav.klage.domain

import no.nav.klage.controller.view.OpenAnkeInput
import no.nav.klage.controller.view.OpenKlageInput
import no.nav.klage.kodeverk.Enhet
import no.nav.klage.util.sanitizeText
import no.nav.klage.util.vedtakFromDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

data class PDFInput (
    val foedselsnummer: String,
    val fornavn: String,
    val mellomnavn: String? = null,
    val etternavn: String,
    val adresse: String? = null,
    val telefonnummer: String? = null,
    val enhetsnavn: String? = null,
    val vedtak: String,
    val begrunnelse: String,
    val saksnummer: String?,
    val dato: String,
    val ytelse: String,
    val userChoices: List<String>? = emptyList(),
    val sendesIPosten: Boolean = true,
)

fun OpenKlageInput.toPDFInput(): PDFInput {
    return PDFInput(
        foedselsnummer = foedselsnummer,
        fornavn = navn.fornavn,
        mellomnavn = navn.mellomnavn,
        etternavn = navn.etternavn,
        adresse = adresse,
        telefonnummer = telefonnummer,
        vedtak = vedtakFromDate(vedtakDate) ?: "Ikke angitt",
        begrunnelse = sanitizeText(fritekst),
        saksnummer = sanitizeText(getSaksnummerString(userSaksnummer)),
        dato = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
        ytelse = titleKey.nb.replaceFirstChar { it.lowercase(Locale.getDefault()) },
        userChoices = checkboxesSelected?.map { x -> x.getFullText(language) },
        sendesIPosten = sendesIPosten,
    )
}

fun OpenAnkeInput.toPDFInput(): PDFInput {
    return PDFInput(
        foedselsnummer = foedselsnummer,
        fornavn = navn.fornavn,
        mellomnavn = navn.mellomnavn,
        etternavn = navn.etternavn,
        vedtak = vedtakFromDate(vedtakDate) ?: "Ikke angitt",
        begrunnelse = sanitizeText(fritekst),
        saksnummer = sanitizeText(getSaksnummerString(userSaksnummer)),
        dato = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
        ytelse = titleKey.nb.replaceFirstChar { it.lowercase(Locale.getDefault()) },
        enhetsnavn = Enhet.values().find { it.navn == enhetsnummer }?.beskrivelse
    )
}

private fun getSaksnummerString(userSaksnummer: String? = null): String {
    return when {
        userSaksnummer != null -> {
            "$userSaksnummer - Oppgitt av bruker"
        }
        else -> "Ikke angitt"
    }
}
