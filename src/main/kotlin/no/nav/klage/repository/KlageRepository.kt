package no.nav.klage.repository

import no.nav.klage.domain.Klage
import no.nav.klage.domain.KlageDAO
import no.nav.klage.domain.KlageStatus.DELETED
import no.nav.klage.domain.KlageStatus.DRAFT
import no.nav.klage.domain.Klager
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class KlageRepository {

    fun getKlager(): List<Klage> {
        return KlageDAO.find { Klager.status neq DELETED }.map {
            it.toKlage()
        }
    }

    fun getKlageById(id: Int): Klage {
        return KlageDAO.findById(id)?.toKlage() ?: throw RuntimeException("Klage not found")
    }

    fun getKlagerByFnr(fnr: String): List<Klage> {
        return KlageDAO.find { Klager.foedselsnummer eq fnr }.map { it.toKlage() }
    }

    fun createKlage(klage: Klage): Klage {
        return KlageDAO.new {
            foedselsnummer = klage.foedselsnummer
            fritekst = klage.fritekst
            status = klage.status
        }.toKlage()
    }

    fun updateKlage(klage: Klage): Klage {
        val klageFromDB = getKlageToModify(klage.id)
        klageFromDB.apply {
            foedselsnummer = klage.foedselsnummer
            fritekst = klage.fritekst
            status = klage.status
            modifiedByUser = Instant.now()
        }
        return klageFromDB.toKlage()
    }

    fun deleteKlage(id: Int) {
        val klageFromDB = getKlageToModify(id)
        klageFromDB.apply {
            status = DELETED
            modifiedByUser = Instant.now()
        }
    }

    private fun getKlageToModify(id: Int?): KlageDAO {
        val klageFromDB = KlageDAO.findById(checkNotNull(id))
        if (klageFromDB?.status != DRAFT) {
            throw IllegalStateException("Klage can only be modified if status == DRAFT")
        }
        return klageFromDB
    }

    private fun KlageDAO.toKlage() = Klage(
        id = this.id.toString().toInt(),
        foedselsnummer = this.foedselsnummer,
        fritekst = this.fritekst,
        status = this.status,
        modifiedByUser = this.modifiedByUser
    )
}