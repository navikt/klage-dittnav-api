package no.nav.klage.domain.anke

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.util.parseTitleKey

data class AnkeInput(
    val tema: Tema,
    val titleKey: TitleEnum,
)

fun AnkeInput.toAnke(bruker: Bruker): Anke {
    return Anke(
        foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
        tema = tema,
        language = LanguageEnum.NB,
        titleKey = parseTitleKey(titleKey, tema)
    )
}