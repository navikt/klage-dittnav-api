package no.nav.klage.domain.klage

import no.nav.klage.domain.*
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.domain.vedlegg.Klagevedlegg
import no.nav.klage.util.klageAnkeIsAccessibleToUser
import no.nav.klage.util.klageAnkeIsLonnskompensasjon
import java.time.Instant
import java.time.LocalDate

data class Klage(
    val id: String,
    val foedselsnummer: String,
    val fritekst: String?,
    var status: KlageAnkeStatus,
    val modifiedByUser: Instant?,
    val tema: Tema,
    val userSaksnummer: String?,
    val vedlegg: List<Klagevedlegg>,
    val journalpostId: String?,
    val vedtakDate: LocalDate?,
    val checkboxesSelected: Set<CheckboxEnum>?,
    val internalSaksnummer: String?,
    val fullmektig: String?,
    val language: LanguageEnum,
    val innsendingsytelse: Innsendingsytelse,
    val hasVedlegg: Boolean,
)

fun Klage.isAccessibleToUser(usersIdentifikasjonsnummer: String) = klageAnkeIsAccessibleToUser(usersIdentifikasjonsnummer, foedselsnummer)
fun Klage.isFinalized() = status.isFinalized()
fun Klage.isDeleted() = status.isDeleted()

