package no.nav.klage.repository

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.Type
import no.nav.klage.domain.jpa.Klanke
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.*

interface KlankeRepository : JpaRepository<Klanke, UUID> {

    fun findByStatusAndModifiedByUserLessThan(status: KlageAnkeStatus, modifiedByUser: LocalDateTime): List<Klanke>

    fun findByFoedselsnummerAndStatusAndType(fnr: String, status: KlageAnkeStatus, type: Type): List<Klanke>

}