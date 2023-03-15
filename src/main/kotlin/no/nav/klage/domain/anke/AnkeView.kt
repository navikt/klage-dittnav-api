package no.nav.klage.domain.anke

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.domain.vedlegg.VedleggView
import java.time.LocalDate
import java.time.LocalDateTime

data class AnkeView(
    val id: String,
    val fritekst: String,
    val status: KlageAnkeStatus,
    val modifiedByUser: LocalDateTime,
    val vedlegg: List<VedleggView>,
    val journalpostId: String?,
    val vedtakDate: LocalDate?,
    val finalizedDate: LocalDate?,
    val userSaksnummer: String?,
    val internalSaksnummer: String?,
    val enhetsnummer: String?,
    val language: LanguageEnum,
    val innsendingsytelse: Innsendingsytelse,
    val hasVedlegg: Boolean,
)