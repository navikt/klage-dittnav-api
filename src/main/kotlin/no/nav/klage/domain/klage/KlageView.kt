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
    val vedtak: String? = null,
    val status: KlageStatus = KlageStatus.DRAFT,
    val modifiedByUser: LocalDateTime = ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("Europe/Oslo"))
        .toLocalDateTime(),
    val saksnummer: String? = null,
    val vedlegg: List<VedleggView> = listOf(),
    val journalpostId: String? = null,
    val finalizedDate: LocalDate? = null,
    val vedtakType: VedtakType? = null,
    val vedtakDate: LocalDate? = null
)

fun KlageView.toKlage(bruker: Bruker, status: KlageStatus = KlageStatus.DRAFT) = Klage(
    id = id,
    foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
    fritekst = fritekst,
    status = status,
    tema = tema,
    ytelse = ytelse,
    vedtak = vedtak,
    saksnummer = saksnummer,
    vedlegg = vedlegg.map { it.toVedlegg() },
    journalpostId = journalpostId,
    vedtakType = vedtakType,
    vedtakDate = vedtakDate
)