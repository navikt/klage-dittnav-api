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
    val id: UUID,
    val foedselsnummer: String,
    val fritekst: String?,
    var status: KlageAnkeStatus,
    val modifiedByUser: Instant?,
    val tema: Tema,
    val userSaksnummer: String?,
    val internalSaksnummer: String?,
    val vedlegg: List<Ankevedlegg>,
    val vedtakDate: LocalDate?,
    val enhetsnummer: String?,
    val language: LanguageEnum,
    val innsendingsytelse: Innsendingsytelse,
    val hasVedlegg: Boolean,
    val journalpostId: String?,
)

fun Anke.isAccessibleToUser(usersIdentifikasjonsnummer: String) =
    klageAnkeIsAccessibleToUser(usersIdentifikasjonsnummer, foedselsnummer)

fun Anke.isFinalized() = status.isFinalized()

fun Anke.isDeleted() = status.isDeleted()
