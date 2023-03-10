package no.nav.klage.domain.anke

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.util.parseTitleKey
import java.time.LocalDate

data class AnkeFullInput(
    val tema: Tema,
    val titleKey: TitleEnum,
    val userSaksnummer: String? = null,
    val language: LanguageEnum = LanguageEnum.NB,
    val vedtakDate: LocalDate? = null,
    val internalSaksnummer: String? = null,
    val fritekst: String?,
    val enhetsnummer: String? = null,
    val hasVedlegg: Boolean,
)

fun AnkeFullInput.toAnke(bruker: Bruker) = Anke(
    foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
    tema = tema,
    titleKey = parseTitleKey(titleKey, tema),
    userSaksnummer = userSaksnummer,
    language = language,
    vedtakDate = vedtakDate,
    internalSaksnummer = internalSaksnummer,
    fritekst = fritekst,
    enhetsnummer = enhetsnummer,
    hasVedlegg = hasVedlegg,
)

