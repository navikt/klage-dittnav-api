package no.nav.klage.repository

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.Tema
import no.nav.klage.domain.jpa.Anke
import no.nav.klage.domain.jpa.Klage
import no.nav.klage.domain.titles.Innsendingsytelse
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface AnkeRepository : JpaRepository<Anke, UUID> {

    fun findByFoedselsnummerAndStatus(fnr: String, status: KlageAnkeStatus): List<Anke>

}