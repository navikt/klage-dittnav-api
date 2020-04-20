package no.nav.klage.repository

import no.nav.klage.domain.Klage
import no.nav.klage.domain.KlageDAO
import no.nav.klage.domain.Klager
import org.jetbrains.exposed.sql.transactions.transaction

class KlageRepository {

    fun getKlager(): List<Klage> {
        return transaction {
            KlageDAO.all().map {
                it.toKlage()
            }
        }
    }

    fun getKlageById(id: Int): Klage {
        return transaction {
            KlageDAO.findById(id) ?: throw RuntimeException("Klage not found")
        }.toKlage()
    }

    fun getKlagerByKlageId(klageId: Int): List<Klage> {
        return transaction {
            KlageDAO.find { Klager.klageId eq klageId }.map { it.toKlage() }
        }
    }

    fun getKlagerByFnr(fnr: String): List<Klage> {
        return transaction {
            KlageDAO.find { Klager.foedselsnummer eq fnr }.map { it.toKlage() }
        }
    }

    fun addKlage(klage: Klage): Klage {
        return transaction {
            KlageDAO.new {
                klageId = klage.klageId
                foedselsnummer = klage.foedselsnummer
                fritekst = klage.fritekst
            }
        }.toKlage()
    }

    private fun KlageDAO.toKlage() = Klage(
        id = this.id.toString().toInt(),
        klageId = this.klageId,
        foedselsnummer = this.foedselsnummer,
        fritekst = this.fritekst
    )
}