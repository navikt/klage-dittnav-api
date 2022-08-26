package no.nav.klage.domain.ankeOLD

import no.nav.klage.domain.*
import no.nav.klage.util.klageAnkeIsAccessibleToUser
import java.time.Instant
import java.time.LocalDate

data class AnkeOLD(
    val id: Int? = null,
    val foedselsnummer: String,
    val fritekst: String,
    var status: KlageAnkeStatus = KlageAnkeStatus.DRAFT,
    val modifiedByUser: Instant? = Instant.now(),
    val tema: Tema,
    val vedlegg: List<no.nav.klage.domain.ankevedleggOLD.AnkeVedlegg> = listOf(),
    val journalpostId: String? = null,
    val vedtakDate: LocalDate? = null,
    val internalSaksnummer: String,
    val fullmektig: String? = null,
    val language: LanguageEnum
)

fun AnkeOLD.isAccessibleToUser(usersIdentifikasjonsnummer: String) =
    klageAnkeIsAccessibleToUser(usersIdentifikasjonsnummer, foedselsnummer)

fun AnkeOLD.isFinalized() = status.isFinalized()
fun AnkeOLD.isDeleted() = status.isDeleted()

fun AnkeOLD.writableOnceFieldsMatch(existingAnkeOLD: AnkeOLD): Boolean {
    return foedselsnummer == existingAnkeOLD.foedselsnummer &&
            tema == existingAnkeOLD.tema &&
            journalpostId == existingAnkeOLD.journalpostId &&
            internalSaksnummer == existingAnkeOLD.internalSaksnummer &&
            fullmektig == existingAnkeOLD.fullmektig
}
