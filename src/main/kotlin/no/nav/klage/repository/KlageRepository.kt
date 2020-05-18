package no.nav.klage.repository

import no.nav.klage.domain.*
import no.nav.klage.domain.KlageStatus.DELETED
import no.nav.klage.domain.KlageStatus.DRAFT
import org.springframework.stereotype.Repository
import java.lang.IllegalArgumentException
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
        modifiedByUser = this.modifiedByUser,
        tema = this.tema.toTema(),
        enhetId = this.enhetId,
        vedlegg = this.vedlegg.map { it.toVedlegg() }
    )

    private fun String.toTema() = try {
        Tema.valueOf(this)
    } catch(e: IllegalArgumentException) {
        Tema.UKJ
    }
}
