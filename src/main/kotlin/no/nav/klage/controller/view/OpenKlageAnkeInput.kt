package no.nav.klage.controller.view

import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Navn
import no.nav.klage.domain.klage.CheckboxEnum
import no.nav.klage.domain.titles.Innsendingsytelse
import java.time.LocalDate

data class OpenKlageInput(
    val foedselsnummer: String,
    val navn: Navn,
    val fritekst: String,
    val userSaksnummer: String? = null,
    val internalSaksnummer: String? = null,
    val vedtakDate: LocalDate?,
    val innsendingsytelse: Innsendingsytelse,
    val checkboxesSelected: Set<CheckboxEnum>? = null,
    val language: LanguageEnum = LanguageEnum.NB,
    val hasVedlegg: Boolean,
)

data class OpenAnkeInput(
    val foedselsnummer: String,
    val navn: Navn,
    val enhetsnummer: String,
    val fritekst: String,
    val userSaksnummer: String? = null,
    val internalSaksnummer: String? = null,
    val vedtakDate: LocalDate?,
    val innsendingsytelse: Innsendingsytelse,
    val language: LanguageEnum = LanguageEnum.NB,
    val hasVedlegg: Boolean,
)

data class OpenEttersendelseInput(
    val foedselsnummer: String,
    val enhetsnummer: String?,
    val innsendingsytelse: Innsendingsytelse,
    val language: LanguageEnum = LanguageEnum.NB,
)