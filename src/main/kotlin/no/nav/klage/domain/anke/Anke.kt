package no.nav.klage.domain.anke

import no.nav.klage.domain.*
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.domain.vedlegg.Ankevedlegg
import no.nav.klage.util.klageAnkeIsAccessibleToUser
import no.nav.klage.util.klageAnkeIsLonnskompensasjon
import java.time.Instant
import java.time.LocalDate
import java.util.*

data class Anke(
    val id: UUID? = null,
    val foedselsnummer: String,
    val fritekst: String? = null,
    var status: KlageAnkeStatus = KlageAnkeStatus.DRAFT,
    val modifiedByUser: Instant? = Instant.now(),
    val tema: Tema,
    val userSaksnummer: String? = null,
    val internalSaksnummer: String? = null,
    val vedlegg: List<Ankevedlegg> = listOf(),
    val vedtakDate: LocalDate? = null,
    val enhetsnummer: String? = null,
    val language: LanguageEnum,
    val innsendingsytelse: Innsendingsytelse,
    val hasVedlegg: Boolean = false,
    val journalpostId: String? = null,
)

fun Anke.isAccessibleToUser(usersIdentifikasjonsnummer: String) =
    klageAnkeIsAccessibleToUser(usersIdentifikasjonsnummer, foedselsnummer)

fun Anke.isLonnskompensasjon() = klageAnkeIsLonnskompensasjon(tema = tema, innsendingsytelse = innsendingsytelse)

fun Anke.isFinalized() = status.isFinalized()

fun Anke.isDeleted() = status.isDeleted()

fun Anke.writableOnceFieldsMatch(existingAnke: Anke): Boolean {
    return id == existingAnke.id &&
            foedselsnummer == existingAnke.foedselsnummer &&
            tema == existingAnke.tema &&
            innsendingsytelse == existingAnke.innsendingsytelse &&
            journalpostId == existingAnke.journalpostId &&
            internalSaksnummer == existingAnke.internalSaksnummer
}
