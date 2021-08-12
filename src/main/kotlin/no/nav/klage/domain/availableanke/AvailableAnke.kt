package no.nav.klage.domain.availableanke

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.Tema
import no.nav.klage.domain.Utfall
import java.time.LocalDate

data class AvailableAnke(
    val internalSaksnummer: String,
    val tema: Tema,
    val utfall: Utfall,
    val innsendtDate: LocalDate? = null,
    val vedtakDate: LocalDate? = null,
    val ankeStatus: KlageAnkeStatus = KlageAnkeStatus.OPEN,
    val foedselsnummer: String
)

fun AvailableAnke.toAvailableAnkeView() = AvailableAnkeView(
    internalSaksnummer,
    tema,
    utfall,
    innsendtDate,
    vedtakDate,
    ankeStatus
)