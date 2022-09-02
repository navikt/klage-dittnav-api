package no.nav.klage.domain.klage

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.util.parseTitleKey

data class KlageInput (
    val tema: Tema,
    val internalSaksnummer: String?,
    val fullmaktsgiver: String?,
    val titleKey: TitleEnum?,
)

fun KlageInput.toKlage(bruker: Bruker): Klage {
    return Klage(
        foedselsnummer = fullmaktsgiver ?: bruker.folkeregisteridentifikator.identifikasjonsnummer,
        tema = tema,
        internalSaksnummer = internalSaksnummer,
        fullmektig = fullmaktsgiver?.let { bruker.folkeregisteridentifikator.identifikasjonsnummer },
        language = LanguageEnum.NB,
        titleKey = parseTitleKey(titleKey, tema),
        hasVedlegg = false,
    )
}