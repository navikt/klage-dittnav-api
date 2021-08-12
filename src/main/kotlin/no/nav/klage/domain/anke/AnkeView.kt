package no.nav.klage.domain.anke

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.ankevedlegg.AnkeVedleggView
import no.nav.klage.domain.ankevedlegg.toAnkeVedlegg
import no.nav.klage.util.getFormattedLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime

data class AnkeView(
    val fritekst: String,
    val tema: Tema,
    val status: KlageAnkeStatus = KlageAnkeStatus.DRAFT,
    val modifiedByUser: LocalDateTime = getFormattedLocalDateTime(),
    val vedlegg: List<AnkeVedleggView> = listOf(),
    val journalpostId: String? = null,
    val finalizedDate: LocalDate? = null,
    val vedtakDate: LocalDate? = null,
    val ankeInternalSaksnummer: String,
    val fullmaktsgiver: String? = null,
    val language: LanguageEnum = LanguageEnum.NB
)



fun AnkeView.toAnke(bruker: Bruker, status: KlageAnkeStatus = KlageAnkeStatus.DRAFT) = Anke(
    foedselsnummer = fullmaktsgiver ?: bruker.folkeregisteridentifikator.identifikasjonsnummer,
    fritekst = fritekst,
    status = status,
    tema = tema,
    vedlegg = vedlegg.map { it.toAnkeVedlegg() },
    journalpostId = journalpostId,
    vedtakDate = vedtakDate,
    internalSaksnummer = ankeInternalSaksnummer,
    fullmektig = fullmaktsgiver?.let { bruker.folkeregisteridentifikator.identifikasjonsnummer },
    language = language
)
