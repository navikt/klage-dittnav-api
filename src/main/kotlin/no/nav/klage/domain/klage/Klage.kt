package no.nav.klage.domain.klage

import no.nav.klage.domain.LanguageEnum
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
    val userSaksnummer: String? = null,
    val vedlegg: List<Vedlegg> = listOf(),
    val journalpostId: String? = null,
    val vedtakDate: LocalDate? = null,
    val checkboxesSelected: Set<CheckboxEnum>? = null,
    val internalSaksnummer: String? = null,
    val fullmektig: String? = null,
    val language: LanguageEnum
)

enum class KlageStatus {
    DRAFT, DONE, DELETED
}

fun Klage.isAccessibleToUser(usersIdentifikasjonsnummer: String) = (foedselsnummer == usersIdentifikasjonsnummer)
fun Klage.isFinalized() = (status === KlageStatus.DONE)
fun Klage.isDeleted() = (status === KlageStatus.DELETED)

fun Klage.writableOnceFieldsMatch(existingKlage: Klage): Boolean {
    return id == existingKlage.id &&
            foedselsnummer == existingKlage.foedselsnummer &&
            tema == existingKlage.tema &&
            ytelse == existingKlage.ytelse &&
            journalpostId == existingKlage.journalpostId &&
            internalSaksnummer == existingKlage.internalSaksnummer &&
            fullmektig == existingKlage.fullmektig
}

