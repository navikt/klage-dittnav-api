package no.nav.klage.domain.klage

import no.nav.klage.domain.Tema
import no.nav.klage.domain.vedlegg.Vedlegg
import java.time.Instant

data class Klage(
    val id: Int? = null,
    val foedselsnummer: String,
    val fritekst: String,
    var status: KlageStatus = KlageStatus.DRAFT,
    val modifiedByUser: Instant? = Instant.now(),
    val tema: Tema,
    val ytelse: String,
    val vedtak: String,
    val saksnummer: String? = null,
    val vedlegg: List<Vedlegg> = listOf(),
    val journalpostId: String? = null
)

enum class KlageStatus {
    DRAFT, DONE, DELETED
}

fun Klage.validateAccess(currentIdentifikasjonsnummer: String) = (foedselsnummer == currentIdentifikasjonsnummer)
fun Klage.isFinalized() = (status === KlageStatus.DONE)
