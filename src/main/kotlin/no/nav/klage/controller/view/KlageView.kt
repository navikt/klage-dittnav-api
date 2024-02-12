package no.nav.klage.controller.view

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.klage.CheckboxEnum
import no.nav.klage.domain.titles.Innsendingsytelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class KlageView(
    val id: UUID,
    val fritekst: String,
    val status: KlageAnkeStatus,
    val modifiedByUser: LocalDateTime,
    val vedlegg: List<VedleggView>,
    val journalpostId: String?,
    val finalizedDate: LocalDate?,
    val vedtakDate: LocalDate?,
    val checkboxesSelected: Set<CheckboxEnum>,
    val userSaksnummer: String?,
    val internalSaksnummer: String?,
    val language: LanguageEnum,
    val innsendingsytelse: Innsendingsytelse,
    val hasVedlegg: Boolean,
)