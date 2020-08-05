package no.nav.klage.domain.klage

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.JournalpostStatus
import no.nav.klage.domain.Tema
import no.nav.klage.domain.vedlegg.Vedlegg

data class KlageView(
    val id: Int,
    val fritekst: String,
    val tema: Tema,
    val ytelse: String,
    val enhetId: String? = null,
    val vedtaksdato: String,
    val referanse: String? = null,
    val vedlegg: List<Vedlegg> = listOf(),
    val journalpostId: String? = null,
    val journalpostStatus: JournalpostStatus
)

fun KlageView.toKlage(bruker: Bruker, status: KlageStatus = KlageStatus.DRAFT) = Klage(
    id = id,
    foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
    fritekst = fritekst,
    status = status,
    tema = tema,
    ytelse = ytelse,
    enhetId = enhetId,
    vedtaksdato = vedtaksdato,
    referanse = referanse,
    vedlegg = vedlegg,
    journalpostId = journalpostId,
    journalpostStatus = journalpostStatus
)