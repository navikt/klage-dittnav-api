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

data class OpenAnkeInput (
    //Legg inn validering? Undersøk om det bør være med i det hele tatt.
    val foedselsnummer: String,
    val navn: Navn,
    val enhetsnummer: String,
    val fritekst: String,
    val userSaksnummer: String? = null,
    val vedtakDate: LocalDate?,
    val titleKey: TitleEnum,
    val tema: Tema,
    val language: LanguageEnum = LanguageEnum.NB,
    val hasVedlegg: Boolean,
    val sendesIPosten: Boolean = true,
)