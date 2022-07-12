package no.nav.klage.controller.view

import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Navn
import no.nav.klage.domain.Tema
import no.nav.klage.domain.klage.CheckboxEnum
import no.nav.klage.domain.titles.TitleEnum
import java.time.LocalDate

data class OpenKlageInput (
    //Legg inn validering? Undersøk om det bør være med i det hele tatt.
    val foedselsnummer: String,
    val navn: Navn,
    val adresse: String,
    val telefonnummer: String,
    val fritekst: String,
    val userSaksnummer: String? = null,
    val vedtakDate: LocalDate?,
    val ytelse: String,
    val titleKey: TitleEnum,
    val tema: Tema,
    val checkboxesSelected: Set<CheckboxEnum>,
    val language: LanguageEnum = LanguageEnum.NB,
)