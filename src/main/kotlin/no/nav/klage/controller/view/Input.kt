package no.nav.klage.controller.view

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Type
import no.nav.klage.domain.klage.CheckboxEnum
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import java.time.LocalDate

data class StringInput(
    val value: String
)

data class StringInputNullable(
    val value: String?
)

data class DateInput(
    val value: LocalDate?
)

data class CheckboxesSelectedInput(
    val value: Set<CheckboxEnum>
)

data class BooleanInput(
    val value: Boolean
)

/**
 * Completely new
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KlankeMinimalInput(
    val internalSaksnummer: String?,
    val innsendingsytelse: Innsendingsytelse,
    val type: Type,
)

/**
 * From not logged in to logged in
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KlankeFullInput(
    val innsendingsytelse: Innsendingsytelse,
    val checkboxesSelected: Set<CheckboxEnum>? = null,
    val enhetsnummer: String? = null,
    val userSaksnummer: String? = null,
    val language: LanguageEnum,
    val vedtakDate: LocalDate? = null,
    val internalSaksnummer: String? = null,
    val fritekst: String = "",
    val hasVedlegg: Boolean,
    val type: Type,
    val caseIsAtKA: Boolean?,
)