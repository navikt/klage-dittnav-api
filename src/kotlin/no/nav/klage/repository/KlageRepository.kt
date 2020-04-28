package no.nav.klage.repository

import no.nav.klage.domain.Klage
import no.nav.klage.domain.KlageDAO
import no.nav.klage.domain.Klager
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class KlageRepository {

    fun getKlager(): List<Klage> {
        return KlageDAO.all().map {
            it.toKlage()
        }
    }

    fun getKlageById(id: Int): Klage {
        return KlageDAO.findById(id)?.toKlage() ?: throw RuntimeException("Klage not found")
    }

    fun getKlagerByKlageId(klageId: Int): List<Klage> {
        return KlageDAO.find { Klager.klageId eq klageId }.map { it.toKlage() }
    }

    fun getKlagerByFnr(fnr: String): List<Klage> {
        return KlageDAO.find { Klager.foedselsnummer eq fnr }.map { it.toKlage() }
    }

    fun addKlage(klage: Klage): Klage {
        return KlageDAO.new {
            klageId = klage.klageId
            foedselsnummer = klage.foedselsnummer
            fritekst = klage.fritekst
        }.toKlage()
    }

    private fun KlageDAO.toKlage() = Klage(
        id = this.id.toString().toInt(),
        klageId = this.klageId,
        foedselsnummer = this.foedselsnummer,
        fritekst = this.fritekst
    )
}