package no.nav.klage.domain

import no.nav.klage.controller.view.OpenKlankeInput
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
    val vedtak: String,
    val begrunnelse: String,
    val saksnummer: String?,
    val dato: String,
    val ytelse: String,
    val sendesIPosten: Boolean,
    val ettersendelseTilKa: Boolean,
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
        ytelse = innsendingsytelse.nbName.replaceFirstChar { it.lowercase(Locale.getDefault()) },
        sendesIPosten = true,
        ettersendelseTilKa = caseIsAtKA ?: false,
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
