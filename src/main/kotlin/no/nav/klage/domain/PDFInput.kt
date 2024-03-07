package no.nav.klage.domain

import no.nav.klage.controller.view.OpenKlankeInput
import no.nav.klage.kodeverk.Enhet
import no.nav.klage.util.sanitizeText
import no.nav.klage.util.vedtakFromDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

data class PDFInput (
    val type: String,
    val foedselsnummer: String,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val enhetsnavn: String?,
    val vedtak: String,
    val begrunnelse: String,
    val saksnummer: String?,
    val dato: String,
    val ytelse: String,
    val userChoices: List<String>? = emptyList(),
    val sendesIPosten: Boolean,
)

fun OpenKlankeInput.toPDFInput(): PDFInput {
    return PDFInput(
        type = type.name,
        foedselsnummer = foedselsnummer,
        fornavn = navn.fornavn,
        mellomnavn = navn.mellomnavn,
        etternavn = navn.etternavn,
        vedtak = vedtakFromDate(vedtakDate) ?: "Ikke angitt",
        begrunnelse = sanitizeText(fritekst),
        saksnummer = sanitizeText(getSaksnummerString(userSaksnummer, internalSaksnummer)),
        dato = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
        ytelse = innsendingsytelse.nb.replaceFirstChar { it.lowercase(Locale.getDefault()) },
        userChoices = checkboxesSelected?.map { x -> x.getFullText(language) },
        sendesIPosten = true,
        enhetsnavn = Enhet.entries.find { it.navn == enhetsnummer }?.beskrivelse,
    )
}

private fun getSaksnummerString(userSaksnummer: String? = null, internalSaksnummer: String? = null): String {
    return when {
        userSaksnummer != null -> {
            "$userSaksnummer - Oppgitt av bruker"
        }
        internalSaksnummer != null -> {
            "$internalSaksnummer - Hentet fra internt system"
        }
        else -> "Ikke angitt"
    }
}
