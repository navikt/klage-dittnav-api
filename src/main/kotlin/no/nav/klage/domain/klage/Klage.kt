package no.nav.klage.domain.klage

import no.nav.klage.domain.*
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.domain.vedlegg.Klagevedlegg
import no.nav.klage.util.klageAnkeIsAccessibleToUser
import no.nav.klage.util.klageAnkeIsLonnskompensasjon
import java.time.Instant
import java.time.LocalDate

data class Klage(
    val id: Int? = null,
    val foedselsnummer: String,
    val fritekst: String? = null,
    var status: KlageAnkeStatus = KlageAnkeStatus.DRAFT,
    val modifiedByUser: Instant? = Instant.now(),
    val tema: Tema,
    val userSaksnummer: String? = null,
    val vedlegg: List<Klagevedlegg> = listOf(),
    val journalpostId: String? = null,
    val vedtakDate: LocalDate? = null,
    val checkboxesSelected: Set<CheckboxEnum>? = null,
    val internalSaksnummer: String? = null,
    val fullmektig: String? = null,
    val language: LanguageEnum,
    val innsendingsytelse: Innsendingsytelse,
    val hasVedlegg: Boolean = false,
)

fun Klage.isAccessibleToUser(usersIdentifikasjonsnummer: String) = klageAnkeIsAccessibleToUser(usersIdentifikasjonsnummer, foedselsnummer)
fun Klage.isFinalized() = status.isFinalized()
fun Klage.isDeleted() = status.isDeleted()
fun Klage.isLonnskompensasjon() = klageAnkeIsLonnskompensasjon(tema, innsendingsytelse)


fun Klage.writableOnceFieldsMatch(existingKlage: Klage): Boolean {
    return id == existingKlage.id &&
            foedselsnummer == existingKlage.foedselsnummer &&
            tema == existingKlage.tema &&
            innsendingsytelse == existingKlage.innsendingsytelse &&
            journalpostId == existingKlage.journalpostId &&
            internalSaksnummer == existingKlage.internalSaksnummer &&
            fullmektig == existingKlage.fullmektig
}

