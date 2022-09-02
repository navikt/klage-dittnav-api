package no.nav.klage.domain.klage

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.util.parseTitleKey
import java.time.LocalDate

data class KlageFullInput (
    val tema: Tema,
    val titleKey: TitleEnum,
    val checkboxesSelected: Set<CheckboxEnum>,
    val userSaksnummer: String? = null,
    val language: LanguageEnum = LanguageEnum.NB,
    val vedtakDate: LocalDate? = null,
    val internalSaksnummer: String? = null,
    val fritekst: String?,
    val fullmaktsgiver: String? = null,
)

fun KlageFullInput.toKlage(bruker: Bruker): Klage {
    return Klage(
        foedselsnummer = fullmaktsgiver ?: bruker.folkeregisteridentifikator.identifikasjonsnummer,
        tema = tema,
        titleKey = parseTitleKey(titleKey, tema),
        checkboxesSelected = checkboxesSelected,
        userSaksnummer = userSaksnummer,
        language = language,
        vedtakDate = vedtakDate,
        internalSaksnummer = internalSaksnummer,
        fritekst = fritekst,
        fullmektig = fullmaktsgiver?.let { bruker.folkeregisteridentifikator.identifikasjonsnummer },
    )
}