package no.nav.klage.repository

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.Tema
import no.nav.klage.domain.jpa.Klage
import no.nav.klage.domain.titles.Innsendingsytelse
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface KlageRepository : JpaRepository<Klage, UUID> {

    fun findByFoedselsnummerAndStatus(fnr: String, status: KlageAnkeStatus): List<Klage>

    fun getLatestKlageDraft(
        fnr: String,
        tema: Tema,
        internalSaksnummer: String?,
        innsendingsytelse: Innsendingsytelse
    ): Klage? {
        return findByFoedselsnummerAndStatus(fnr = fnr, status = KlageAnkeStatus.DRAFT)
            .filter {
                if (internalSaksnummer != null) {
                    it.tema == tema && it.innsendingsytelse == innsendingsytelse && it.internalSaksnummer == internalSaksnummer
                } else {
                    it.tema == tema && it.innsendingsytelse == innsendingsytelse
                }
            }.maxByOrNull { it.modifiedByUser!! }
    }
}