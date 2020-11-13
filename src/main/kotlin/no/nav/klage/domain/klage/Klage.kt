package no.nav.klage.domain.klage

import no.nav.klage.domain.Tema
import no.nav.klage.domain.vedlegg.Vedlegg
import java.time.Instant
import java.time.LocalDate

data class Klage(
    val id: Int? = null,
    val foedselsnummer: String,
    val fritekst: String,
    var status: KlageStatus = KlageStatus.DRAFT,
    val modifiedByUser: Instant? = Instant.now(),
    val tema: Tema,
    val ytelse: String,
    val vedtak: String? = null,
    val saksnummer: String? = null,
    val vedlegg: List<Vedlegg> = listOf(),
    val journalpostId: String? = null,
    val vedtakType: VedtakType? = null,
    val vedtakDate: LocalDate? = null
)

enum class KlageStatus {
    DRAFT, DONE, DELETED
}

enum class VedtakType {
    LATEST, EARLIER
}

fun Klage.validateAccess(currentIdentifikasjonsnummer: String) = (foedselsnummer == currentIdentifikasjonsnummer)
fun Klage.isFinalized() = (status === KlageStatus.DONE)

