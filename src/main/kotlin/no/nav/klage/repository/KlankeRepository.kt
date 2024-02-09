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

    fun findByFoedselsnummerAndStatus(fnr: String, status: KlageAnkeStatus): List<Klanke>

    fun getLatestKlankeDraft(
        fnr: String,
        tema: Tema,
        internalSaksnummer: String?,
        innsendingsytelse: Innsendingsytelse
    ): Klanke? {
        return findByFoedselsnummerAndStatus(fnr = fnr, status = KlageAnkeStatus.DRAFT)
            .filter {
                if (internalSaksnummer != null) {
                    it.tema == tema && it.innsendingsytelse == innsendingsytelse && it.internalSaksnummer == internalSaksnummer
                } else {
                    it.tema == tema && it.innsendingsytelse == innsendingsytelse
                }
            }.maxByOrNull { it.modifiedByUser!! }
    }

    fun findByStatusAndModifiedByUserLessThan(status: KlageAnkeStatus, modifiedByUser: LocalDateTime): List<Klanke>

}