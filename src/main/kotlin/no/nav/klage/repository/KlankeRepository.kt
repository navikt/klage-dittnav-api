package no.nav.klage.repository

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.Tema
import no.nav.klage.domain.jpa.Klage
import no.nav.klage.domain.jpa.Klanke
import no.nav.klage.domain.titles.Innsendingsytelse
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.*

interface KlankeRepository : JpaRepository<Klanke, UUID> {

    fun findByStatusAndModifiedByUserLessThan(status: KlageAnkeStatus, modifiedByUser: LocalDateTime): List<Klanke>

}