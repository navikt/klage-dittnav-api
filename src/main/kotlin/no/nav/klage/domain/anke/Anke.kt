package no.nav.klage.domain.anke

import no.nav.klage.domain.*
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.util.klageAnkeIsAccessibleToUser
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
    val vedtakDate: LocalDate? = null,
    val enhetsnummer: String? = null,
    val language: LanguageEnum,
    val titleKey: TitleEnum,
)

fun Anke.isAccessibleToUser(usersIdentifikasjonsnummer: String) =
    klageAnkeIsAccessibleToUser(usersIdentifikasjonsnummer, foedselsnummer)

fun Anke.isFinalized() = status.isFinalized()

fun Anke.isDeleted() = status.isDeleted()
