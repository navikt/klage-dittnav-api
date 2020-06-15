package no.nav.klage.domain.klage

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.Tema
import no.nav.klage.domain.Vedlegg
import java.time.LocalDate

data class KlageView(
    val id: Int? = null,
    val fritekst: String,
    val tema: Tema,
    val enhetId: String? = null,
    val vedtaksdato: LocalDate,
    val referanse: String? = null,
    val vedlegg: List<Vedlegg> = listOf()
)

fun KlageView.toKlage(bruker: Bruker, status: KlageStatus = KlageStatus.DRAFT) = Klage(
    id = id,
    foedselsnummer = bruker.folkeregisteridentifikator.identifikasjonsnummer,
    fritekst = fritekst,
    status = status,
    tema = tema,
    enhetId = enhetId,
    vedtaksdato = vedtaksdato,
    referanse = referanse,
    vedlegg = vedlegg
)