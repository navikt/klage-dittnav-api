package no.nav.klage.domain.klage

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.util.getInnsendingsytelse
import java.time.LocalDate

/**
 * Completely new
 */
data class KlageInput (
    val tema: Tema?,
    val internalSaksnummer: String?,
    val fullmaktsgiver: String?,
    val titleKey: Innsendingsytelse?,
    val innsendingsytelse: Innsendingsytelse?,
) {
    fun toKlage(bruker: Bruker): Klage {
        val innsendingsytelse = getInnsendingsytelse(titleKey = titleKey, innsendingsytelse = innsendingsytelse)
        return Klage(
            foedselsnummer = fullmaktsgiver ?: bruker.folkeregisteridentifikator.identifikasjonsnummer,
            tema = innsendingsytelse.getTema(),
            internalSaksnummer = internalSaksnummer,
            fullmektig = fullmaktsgiver?.let { bruker.folkeregisteridentifikator.identifikasjonsnummer },
            language = LanguageEnum.NB,
            innsendingsytelse = innsendingsytelse,
            hasVedlegg = false,
        )
    }
}

/**
 * From not logged in to logged in
 */
data class KlageFullInput (
    val tema: Tema?,
    val titleKey: Innsendingsytelse?,
    val innsendingsytelse: Innsendingsytelse?,
    val checkboxesSelected: Set<CheckboxEnum>,
    val userSaksnummer: String? = null,
    val language: LanguageEnum = LanguageEnum.NB,
    val vedtakDate: LocalDate? = null,
    val internalSaksnummer: String? = null,
    val fritekst: String?,
    val fullmaktsgiver: String? = null,
    val hasVedlegg: Boolean,
) {
    fun toKlage(bruker: Bruker): Klage {
        val innsendingsytelse = getInnsendingsytelse(titleKey = titleKey, innsendingsytelse = innsendingsytelse)

        return Klage(
            foedselsnummer = fullmaktsgiver ?: bruker.folkeregisteridentifikator.identifikasjonsnummer,
            tema = innsendingsytelse.getTema(),
            innsendingsytelse = innsendingsytelse,
            checkboxesSelected = checkboxesSelected,
            userSaksnummer = userSaksnummer,
            language = language,
            vedtakDate = vedtakDate,
            internalSaksnummer = internalSaksnummer,
            fritekst = fritekst,
            fullmektig = fullmaktsgiver?.let { bruker.folkeregisteridentifikator.identifikasjonsnummer },
            hasVedlegg = hasVedlegg,
        )
    }
}