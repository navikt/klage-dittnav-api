package no.nav.klage.domain.availableanke

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.Tema
import no.nav.klage.domain.Utfall
import java.time.LocalDate

data class AvailableAnkeView(
    val ankeInternalSaksnummer: String,
    val tema: Tema,
    val utfall: Utfall,
    val innsendtDate: LocalDate? = null,
    val vedtakDate: LocalDate? = null,
    val ankeStatus: KlageAnkeStatus = KlageAnkeStatus.OPEN
)