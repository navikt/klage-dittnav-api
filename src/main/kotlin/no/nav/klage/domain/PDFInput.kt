package no.nav.klage.domain

import no.nav.klage.controller.view.OpenKlageInput
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
    val adresse: String,
    val telefonnummer: String,
    val vedtak: String,
    val begrunnelse: String,
    val saksnummer: String?,
    val dato: String,
    val ytelse: String,
    val userChoices: List<String>? = emptyList(),
)

fun OpenKlageInput.toPDFInput(): PDFInput {
    return PDFInput(
        foedselsnummer = foedselsnummer,
        fornavn = navn.fornavn,
        mellomnavn = navn.mellomnavn,
        etternavn = navn.etternavn,
        adresse = adresse,
        telefonnummer = telefonnummer,
        vedtak = vedtakFromDate(vedtakDate) ?: "",
        begrunnelse = sanitizeText(fritekst),
        saksnummer = sanitizeText(getSaksnummerString(userSaksnummer)),
        dato = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
        ytelse = titleKey.nb.replaceFirstChar { it.lowercase(Locale.getDefault()) },
        userChoices = checkboxesSelected?.map { x -> x.getFullText(language) }
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
