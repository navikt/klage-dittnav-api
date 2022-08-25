package no.nav.klage.controller.view

import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Navn
import no.nav.klage.domain.Tema
import no.nav.klage.domain.klage.CheckboxEnum
import no.nav.klage.domain.titles.TitleEnum
import java.time.LocalDate

data class OpenKlageInput (
    val foedselsnummer: String,
    val navn: Navn,
    val adresse: String,
    val telefonnummer: String?,
    val fritekst: String,
    val userSaksnummer: String? = null,
    val vedtakDate: LocalDate?,
    val titleKey: TitleEnum,
    val tema: Tema,
    val checkboxesSelected: Set<CheckboxEnum>? = null,
    val language: LanguageEnum = LanguageEnum.NB,
    val hasVedlegg: Boolean,
    val sendesIPosten: Boolean = true,
)