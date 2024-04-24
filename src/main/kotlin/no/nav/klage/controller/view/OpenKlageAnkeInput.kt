package no.nav.klage.controller.view

import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Navn
import no.nav.klage.domain.Type
import no.nav.klage.domain.klage.CheckboxEnum
import no.nav.klage.kodeverk.innsendingsytelse.Innsendingsytelse
import java.time.LocalDate

data class OpenKlankeInput(
    val foedselsnummer: String,
    val navn: Navn,
    val fritekst: String,
    val userSaksnummer: String?,
    val internalSaksnummer: String?,
    val vedtakDate: LocalDate?,
    val innsendingsytelse: Innsendingsytelse,
    val checkboxesSelected: Set<CheckboxEnum>?,
    val enhetsnummer: String?,
    val language: LanguageEnum = LanguageEnum.NB,
    val hasVedlegg: Boolean,
    val type: Type,
)