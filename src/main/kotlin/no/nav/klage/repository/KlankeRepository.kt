package no.nav.klage.repository

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.Type
import no.nav.klage.domain.jpa.Klanke
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface KlankeRepository : JpaRepository<Klanke, UUID> {
    fun findByStatus(status: KlageAnkeStatus): List<Klanke>

    fun findByStatusAndModifiedByUserLessThan(
        status: KlageAnkeStatus,
        modifiedByUser: LocalDateTime,
    ): List<Klanke>

    fun findByFoedselsnummerAndStatusAndType(
        fnr: String,
        status: KlageAnkeStatus,
        type: Type,
    ): List<Klanke>

    // Returns a projection rather than the entity, so only journalpostId is selected and the eager vedlegg are not loaded.
    fun findJournalpostIdById(id: UUID): KlankeJournalpostId?
}

interface KlankeJournalpostId {
    // Included so a klanke without journalpostId still yields a projection, distinguishing it from a missing klanke.
    val id: UUID
    val journalpostId: String?
}
