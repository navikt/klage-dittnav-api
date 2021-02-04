package no.nav.klage.repository

import no.nav.klage.domain.Tema
import no.nav.klage.domain.exception.AttemptedIllegalUpdateException
import no.nav.klage.domain.exception.KlageNotFoundException
import no.nav.klage.domain.klage.*
import no.nav.klage.domain.klage.KlageStatus.DELETED
import no.nav.klage.domain.titles.TitleEnum
import no.nav.klage.util.getLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
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
        return KlageDAO.findById(id)?.toKlage() ?: throw KlageNotFoundException("Klage with id $id not found in db.")
    }

    fun getKlageByJournalpostId(journalpostId: String): Klage {
        return KlageDAO.find { Klager.journalpostId eq journalpostId }.map { it.toKlage() }[0]
    }

    fun getKlagerByFnr(fnr: String): List<Klage> {
        return KlageDAO.find { Klager.foedselsnummer eq fnr }.map { it.toKlage() }
    }

    fun getDraftKlagerByFnr(fnr: String): List<Klage> {
        return KlageDAO.find { Klager.foedselsnummer eq fnr and (Klager.status eq KlageStatus.DRAFT.toString()) }
            .map { it.toKlage() }
    }

    fun getLatestDraftKlageByFnrTemaInternalSaksnummerTitleKey(
        fnr: String,
        tema: Tema,
        internalSaksnummer: String?,
        titleKey: TitleEnum?
    ): Klage? {
        return KlageDAO.find {
            if (titleKey == null && internalSaksnummer.isNullOrBlank()) {
                Klager.foedselsnummer eq fnr and (Klager.tema eq tema.name) and (Klager.ytelse.isNull() and Klager.titleKey.isNull()) and (Klager.internalSaksnummer.isNull()) and (Klager.status eq KlageStatus.DRAFT.toString())
            } else if (titleKey == null) {
                Klager.foedselsnummer eq fnr and (Klager.tema eq tema.name) and (Klager.ytelse.isNull() and Klager.titleKey.isNull()) and (Klager.internalSaksnummer eq internalSaksnummer) and (Klager.status eq KlageStatus.DRAFT.toString())
            } else if (internalSaksnummer.isNullOrBlank()) {
                Klager.foedselsnummer eq fnr and (Klager.tema eq tema.name) and (Klager.ytelse eq titleKey.nb or (Klager.titleKey eq titleKey.name)) and (Klager.internalSaksnummer.isNull()) and (Klager.status eq KlageStatus.DRAFT.toString())
            } else {
                Klager.foedselsnummer eq fnr and (Klager.tema eq tema.name) and (Klager.ytelse eq titleKey.nb or (Klager.titleKey eq titleKey.name)) and (Klager.internalSaksnummer eq internalSaksnummer) and (Klager.status eq KlageStatus.DRAFT.toString())
            }
        }.maxBy { it.modifiedByUser }
            ?.toKlage()
    }

    fun createKlage(klage: Klage): Klage {
        logger.debug("Creating klage in db.")
        return KlageDAO.new {
            fromKlage(klage)
        }.toKlage().also {
            logger.debug("Klage successfully created in db. Id: {}", it.id)
        }
    }

    fun updateKlage(klage: Klage, checkWritableOnceFields: Boolean = true): Klage {
        logger.debug("Updating klage in db. Id: {}", klage.id)
        val klageFromDB = getKlageToModify(klage.id)

        if (checkWritableOnceFields && !klage.writableOnceFieldsMatch(klageFromDB.toKlage())) {
            throw AttemptedIllegalUpdateException()
        }

        klageFromDB.apply {
            fromKlage(klage)
        }
        logger.debug("Klage successfully updated in db.")
        return klageFromDB.toKlage()
    }

    fun deleteKlage(id: Int) {
        logger.debug("Deleting klage in db. Id: {}", id)
        val klageFromDB = getKlageToModify(id)
        klageFromDB.apply {
            status = DELETED.name
            modifiedByUser = Instant.now()
        }
        logger.debug("Klage successfully marked as deleted in db.")
    }

    private fun getKlageToModify(id: Int?): KlageDAO {
        return KlageDAO.findById(checkNotNull(id)) ?: throw KlageNotFoundException("Klage with id $id not found in db.")
    }
}
