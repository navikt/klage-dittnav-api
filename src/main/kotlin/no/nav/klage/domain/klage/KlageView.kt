package no.nav.klage.domain.klage

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.domain.vedlegg.VedleggView
import no.nav.klage.util.getFormattedLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime

data class KlageView(
    val id: String,
    val fritekst: String?,
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
    val titleKey: Innsendingsytelse,
    val innsendingsytelse: Innsendingsytelse,
    val hasVedlegg: Boolean,
    val tema: Tema,
)