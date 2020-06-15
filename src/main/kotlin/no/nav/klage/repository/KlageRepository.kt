package no.nav.klage.repository

import no.nav.klage.domain.klage.Klage
import no.nav.klage.domain.klage.KlageDAO
import no.nav.klage.domain.klage.KlageStatus.DELETED
import no.nav.klage.domain.klage.KlageStatus.DRAFT
import no.nav.klage.domain.klage.Klager
import no.nav.klage.domain.klage.toKlage
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class KlageRepository {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    fun getKlager(): List<Klage> {
        return KlageDAO.find { Klager.status neq DELETED.name }.map {
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
        logger.debug("Creating klage in db.")

        return KlageDAO.new {
            fromKlage(klage)
        }.toKlage().also {
            logger.debug("Klage successfully created in db. Id: {}", it.id)
        }
    }

    fun updateKlage(klage: Klage): Klage {
        val klageFromDB = getKlageToModify(klage.id)
        klageFromDB.apply {
            fromKlage(klage)
        }
        return klageFromDB.toKlage()
    }

    fun deleteKlage(id: Int) {
        val klageFromDB = getKlageToModify(id)
        klageFromDB.apply {
            status = DELETED.name
            modifiedByUser = Instant.now()
        }
    }

    private fun getKlageToModify(id: Int?): KlageDAO {
        val klageFromDB = KlageDAO.findById(checkNotNull(id))
        if (klageFromDB?.status != DRAFT.name) {
            throw IllegalStateException("Klage can only be modified if status == DRAFT")
        }
        return klageFromDB
    }

    private fun KlageDAO.fromKlage(klage: Klage) {
        foedselsnummer = klage.foedselsnummer
        fritekst = klage.fritekst
        status = klage.status.name
        modifiedByUser = Instant.now()
        tema = klage.tema.name
        klage.enhetId?.let { enhetId = it }
        vedtaksdato = klage.vedtaksdato
        klage.referanse?.let { referanse = it }
    }


}
