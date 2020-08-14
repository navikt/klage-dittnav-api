package no.nav.klage.domain.klage

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.Tema
import no.nav.klage.domain.vedlegg.VedleggView
import no.nav.klage.domain.vedlegg.toVedlegg

data class KlageView(
    val id: Int,
    val fritekst: String,
    val tema: Tema,
    val vedtak: String,
    val saksnummer: String? = null,
    val vedlegg: List<VedleggView> = listOf(),
    val journalpostId: String? = null
)

fun KlageView.toKlage(bruker: Bruker, status: KlageStatus = KlageStatus.DRAFT) = Klage(
    id = id,
    foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
    fritekst = fritekst,
    status = status,
    tema = tema,
    vedtak = vedtak,
    saksnummer = saksnummer,
    vedlegg = vedlegg.map { it.toVedlegg() },
    journalpostId = journalpostId
)