package no.nav.klage.domain.anke

import no.nav.klage.domain.*
import no.nav.klage.domain.ankevedlegg.AnkeVedlegg
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.util.klageAnkeIsAccessibleToUser
import no.nav.klage.util.klageAnkeIsLonnskompensasjon
import java.time.Instant
import java.time.LocalDate

data class Anke(
    val id: Int? = null,
    val foedselsnummer: String,
    val fritekst: String,
    var status: KlageAnkeStatus = KlageAnkeStatus.DRAFT,
    val modifiedByUser: Instant? = Instant.now(),
    val tema: Tema,
    val vedlegg: List<AnkeVedlegg> = listOf(),
    val journalpostId: String? = null,
    val vedtakDate: LocalDate? = null,
    val internalSaksnummer: String? = null,
    val fullmektig: String? = null,
    val language: LanguageEnum,
    val titleKey: TitleEnum
)

fun Anke.isAccessibleToUser(usersIdentifikasjonsnummer: String) =
    klageAnkeIsAccessibleToUser(usersIdentifikasjonsnummer, foedselsnummer)

fun Anke.isFinalized() = status.isFinalized()
fun Anke.isDeleted() = status.isDeleted()
fun Anke.isLonnskompensasjon() = klageAnkeIsLonnskompensasjon(tema, titleKey)

fun Anke.writableOnceFieldsMatch(existingAnke: Anke): Boolean {
    return id == existingAnke.id &&
            foedselsnummer == existingAnke.foedselsnummer &&
            tema == existingAnke.tema &&
            titleKey == existingAnke.titleKey &&
            journalpostId == existingAnke.journalpostId &&
            internalSaksnummer == existingAnke.internalSaksnummer &&
            fullmektig == existingAnke.fullmektig
}
