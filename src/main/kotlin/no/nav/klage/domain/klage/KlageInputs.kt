package no.nav.klage.domain.klage

import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.titles.Innsendingsytelse
import java.time.LocalDate

/**
 * Completely new
 */
data class KlageInput(
    val internalSaksnummer: String?,
    val innsendingsytelse: Innsendingsytelse,
)

/**
 * From not logged in to logged in
 */
data class KlageFullInput(
    val innsendingsytelse: Innsendingsytelse,
    val checkboxesSelected: Set<CheckboxEnum>,
    val userSaksnummer: String? = null,
    val language: LanguageEnum,
    val vedtakDate: LocalDate? = null,
    val internalSaksnummer: String? = null,
    val fritekst: String = "",
    val hasVedlegg: Boolean,
)