package no.nav.klage.domain.klage

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.domain.vedlegg.VedleggView
import no.nav.klage.domain.vedlegg.toVedlegg
import no.nav.klage.util.getFormattedLocalDateTime
import no.nav.klage.util.klageAnkeIsLonnskompensasjon
import no.nav.klage.util.parseTitleKey
import java.time.LocalDate
import java.time.LocalDateTime

data class KlageView(
    val id: Int,
    val fritekst: String,
    val tema: Tema,
    val status: KlageAnkeStatus = KlageAnkeStatus.DRAFT,
    val modifiedByUser: LocalDateTime = getFormattedLocalDateTime(),
    val vedlegg: List<VedleggView> = listOf(),
    val journalpostId: String? = null,
    val finalizedDate: LocalDate? = null,
    val vedtakDate: LocalDate? = null,
    val checkboxesSelected: Set<CheckboxEnum>,
    val userSaksnummer: String? = null,
    val internalSaksnummer: String? = null,
    val fullmaktsgiver: String? = null,
    val language: LanguageEnum = LanguageEnum.NB,
    val titleKey: TitleEnum?,
    val ytelse: String?
)

fun KlageView.isLonnskompensasjon(): Boolean = titleKey?.let { klageAnkeIsLonnskompensasjon(tema, it) } ?: false

fun KlageView.toKlage(bruker: Bruker, status: KlageAnkeStatus = KlageAnkeStatus.DRAFT) = Klage(
    id = id,
    foedselsnummer = fullmaktsgiver ?: bruker.folkeregisteridentifikator.identifikasjonsnummer,
    fritekst = fritekst,
    status = status,
    tema = tema,
    userSaksnummer = userSaksnummer,
    vedlegg = vedlegg.map { it.toVedlegg() },
    journalpostId = journalpostId,
    vedtakDate = vedtakDate,
    checkboxesSelected = checkboxesSelected,
    internalSaksnummer = internalSaksnummer,
    fullmektig = fullmaktsgiver?.let { bruker.folkeregisteridentifikator.identifikasjonsnummer },
    language = language,
    titleKey = parseTitleKey(titleKey, ytelse, tema)
)

