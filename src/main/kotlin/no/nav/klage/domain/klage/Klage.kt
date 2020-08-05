package no.nav.klage.domain.klage

import no.nav.klage.domain.JournalpostStatus
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
    val enhetId: String? = null,
    val vedtaksdato: String,
    val referanse: String? = null,
    val vedlegg: List<Vedlegg> = listOf(),
    val journalpostId: String? = null,
    val journalpostStatus: JournalpostStatus = JournalpostStatus.UNREGISTERED
)

enum class KlageStatus {
    DRAFT, DONE, DELETED
}

fun Klage.toKlageView() =
    KlageView(id!!, fritekst, tema, ytelse, enhetId, vedtaksdato, referanse, vedlegg, journalpostId, journalpostStatus)

fun Klage.validateAccess(currentIdentifikasjonsnummer: String, checkFinalized: Boolean = true) {
    if (checkFinalized && status === KlageStatus.DONE) {
        throw RuntimeException("Klage is already finalized.")
    }
    if (foedselsnummer != currentIdentifikasjonsnummer) {
        throw RuntimeException("Folkeregisteridentifikator in klage does not match current user.")
    }
}