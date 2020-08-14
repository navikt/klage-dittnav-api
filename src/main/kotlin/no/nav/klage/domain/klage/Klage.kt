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
    val vedtak: String,
    val saksnummer: String? = null,
    val vedlegg: List<Vedlegg> = listOf(),
    val journalpostId: String? = null
)

enum class KlageStatus {
    DRAFT, DONE, DELETED
}

fun Klage.validateAccess(currentIdentifikasjonsnummer: String, checkFinalized: Boolean = true) {
    if (checkFinalized && status === KlageStatus.DONE) {
        throw RuntimeException("Klage is already finalized.")
    }
    if (foedselsnummer != currentIdentifikasjonsnummer) {
        throw RuntimeException("Folkeregisteridentifikator in klage does not match current user.")
    }
}