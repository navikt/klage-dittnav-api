package no.nav.klage.domain.anke

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.ankevedlegg.AnkeVedleggView
import no.nav.klage.domain.ankevedlegg.toAnkeVedlegg
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.util.getFormattedLocalDateTime
import no.nav.klage.util.klageAnkeIsLonnskompensasjon
import no.nav.klage.util.parseTitleKey
import java.time.LocalDate
import java.time.LocalDateTime

data class AnkeView(
    val id: Int,
    val fritekst: String,
    val tema: Tema,
    val status: KlageAnkeStatus = KlageAnkeStatus.DRAFT,
    val modifiedByUser: LocalDateTime = getFormattedLocalDateTime(),
    val vedlegg: List<AnkeVedleggView> = listOf(),
    val journalpostId: String? = null,
    val finalizedDate: LocalDate? = null,
    val vedtakDate: LocalDate? = null,
    val internalSaksnummer: String? = null,
    val fullmaktsgiver: String? = null,
    val language: LanguageEnum = LanguageEnum.NB,
    val titleKey: TitleEnum?,
    val ytelse: String?
)

fun AnkeView.isLonnskompensasjon(): Boolean = titleKey?.let { klageAnkeIsLonnskompensasjon(tema, it) } ?: false

fun AnkeView.toAnke(bruker: Bruker, status: KlageAnkeStatus = KlageAnkeStatus.DRAFT) = Anke(
    id = id,
    foedselsnummer = fullmaktsgiver ?: bruker.folkeregisteridentifikator.identifikasjonsnummer,
    fritekst = fritekst,
    status = status,
    tema = tema,
    vedlegg = vedlegg.map { it.toAnkeVedlegg() },
    journalpostId = journalpostId,
    vedtakDate = vedtakDate,
    internalSaksnummer = internalSaksnummer,
    fullmektig = fullmaktsgiver?.let { bruker.folkeregisteridentifikator.identifikasjonsnummer },
    language = language,
    titleKey = parseTitleKey(titleKey, ytelse, tema)
)
