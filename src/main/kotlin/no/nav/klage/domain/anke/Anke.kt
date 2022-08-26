package no.nav.klage.domain.anke

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.LanguageEnum
import no.nav.klage.domain.Tema
import no.nav.klage.domain.titles.TitleEnum
import java.time.Instant
import java.time.LocalDate
import java.util.*

data class Anke (
    val id: UUID,
    val foedselsnummer: String,
    val fritekst: String,
    var status: KlageAnkeStatus = KlageAnkeStatus.DRAFT,
    val modifiedByUser: Instant? = Instant.now(),
    val tema: Tema,
    val userSaksnummer: String? = null,
    val vedtakDate: LocalDate? = null,
    val enhetsnummer: String? = null,
    val language: LanguageEnum,
    val titleKey: TitleEnum,
)
