package no.nav.klage.domain.klage

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.Tema
import no.nav.klage.domain.vedlegg.VedleggView
import no.nav.klage.domain.vedlegg.toVedlegg
import java.time.*

data class KlageView(
    val id: Int,
    val fritekst: String,
    val tema: Tema,
    val ytelse: String,
    val status: KlageStatus = KlageStatus.DRAFT,
    val modifiedByUser: LocalDateTime = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("Europe/Oslo"))
        .toLocalDateTime(),
    val vedlegg: List<VedleggView> = listOf(),
    val journalpostId: String? = null,
    val finalizedDate: LocalDate? = null,
    val vedtakType: VedtakType? = null,
    val vedtakDate: LocalDate? = null,
    val checkboxesSelected: Set<CheckboxEnum>,
    val userSaksnummer: String? = null,
    val internalSaksnummer: String? = null,
    val fullmaktsgiver: String? = null
)

fun KlageView.toKlage(bruker: Bruker, status: KlageStatus = KlageStatus.DRAFT) = Klage(
    id = id,
    foedselsnummer = fullmaktsgiver ?: bruker.folkeregisteridentifikator.identifikasjonsnummer,
    fritekst = fritekst,
    status = status,
    tema = tema,
    ytelse = ytelse,
    userSaksnummer = userSaksnummer,
    vedlegg = vedlegg.map { it.toVedlegg() },
    journalpostId = journalpostId,
    vedtakType = vedtakType,
    vedtakDate = vedtakDate,
    checkboxesSelected = checkboxesSelected,
    internalSaksnummer = internalSaksnummer,
    fullmektig = fullmaktsgiver?.let { bruker.folkeregisteridentifikator.identifikasjonsnummer }
)