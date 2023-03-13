package no.nav.klage.domain.anke

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.domain.vedlegg.VedleggView
import no.nav.klage.util.getFormattedLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime

data class AnkeView(
    val id: String,
    val fritekst: String?,
    val status: KlageAnkeStatus = KlageAnkeStatus.DRAFT,
    val modifiedByUser: LocalDateTime = getFormattedLocalDateTime(),
    val vedtakDate: LocalDate? = null,
    val userSaksnummer: String? = null,
    val internalSaksnummer: String?,
    val enhetsnummer: String? = null,
    val language: LanguageEnum = LanguageEnum.NB,
    val titleKey: Innsendingsytelse,
    val innsendingsytelse: Innsendingsytelse,
    val hasVedlegg: Boolean,
    val vedlegg: List<VedleggView>,
    val journalpostId: String?,
    val tema: Tema,
)