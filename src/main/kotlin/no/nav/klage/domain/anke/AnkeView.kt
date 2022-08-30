package no.nav.klage.domain.anke

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.util.getFormattedLocalDateTime
import no.nav.klage.util.parseTitleKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class AnkeView(
    val id: String,
    val fritekst: String?,
    val tema: Tema,
    val status: KlageAnkeStatus = KlageAnkeStatus.DRAFT,
    val modifiedByUser: LocalDateTime = getFormattedLocalDateTime(),
    val vedtakDate: LocalDate? = null,
    val userSaksnummer: String? = null,
    val enhetsnummer: String? = null,
    val language: LanguageEnum = LanguageEnum.NB,
    val titleKey: TitleEnum?,
    val hasVedlegg: Boolean,
)

fun AnkeView.toAnke(bruker: Bruker, status: KlageAnkeStatus = KlageAnkeStatus.DRAFT) = Anke(
    id = UUID.fromString(id),
    foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
    fritekst = fritekst,
    status = status,
    tema = tema,
    userSaksnummer = userSaksnummer,
    vedtakDate = vedtakDate,
    enhetsnummer = enhetsnummer,
    language = language,
    titleKey = parseTitleKey(titleKey, tema),
    hasVedlegg = hasVedlegg,
)

