package no.nav.klage.repository

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.anke.*
import no.nav.klage.domain.exception.AnkeNotFoundException
import no.nav.klage.domain.exception.AttemptedIllegalUpdateException
import no.nav.klage.util.getLogger
import org.jetbrains.exposed.sql.and
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Repository
class AnkeRepository {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value("\${MAX_DRAFT_AGE_IN_DAYS}")
    private lateinit var maxDraftAgeInDays: String

    fun getExpiredDraftAnker(): List<Anke> {
        val expiryDate = Instant.now().minus(maxDraftAgeInDays.toLong(), ChronoUnit.DAYS)
        return AnkeDAO.find { Anker.status eq KlageAnkeStatus.DRAFT.name and Anker.modifiedByUser.less(expiryDate) }
            .map {
                it.toAnke()
            }
    }

    fun getAnkeByInternalSaksnummer(internalSaksnummer: String): Anke {
        val inputUUID = UUID.fromString(internalSaksnummer)
        return AnkeDAO.find {
            Anker.internalSaksnummer eq inputUUID
        }.firstOrNull()?.toAnke() ?: throw AnkeNotFoundException("Anke with internalSaksnummer $internalSaksnummer not found in db.")
    }

    fun getLatestDraftAnkeByFnrAndInternalSaksnummer(
        fnr: String,
        internalSaksnummer: String
    ): Anke? {
        val inputUUID = UUID.fromString(internalSaksnummer)
        return AnkeDAO.find {
            Anker.foedselsnummer eq fnr and (Anker.internalSaksnummer eq inputUUID) and (Anker.status eq KlageAnkeStatus.DRAFT.toString())
        }.maxByOrNull { it.modifiedByUser }
            ?.toAnke()
    }

    fun createAnke(anke: Anke): Anke {
        logger.debug("Creating anke in db.")
        return AnkeDAO.new {
            fromAnke(anke)
        }.toAnke().also {
            logger.debug("Anke successfully created in db. Id: {}", it.id)
        }
    }

    fun updateAnke(anke: Anke, checkWritableOnceFields: Boolean = true): Anke {
        logger.debug("Updating anke in db. Internal saksnummer: {}", anke.internalSaksnummer)
        val ankeFromDB = getAnkeToModify(anke.internalSaksnummer)

        if (checkWritableOnceFields && !anke.writableOnceFieldsMatch(ankeFromDB.toAnke())) {
            throw AttemptedIllegalUpdateException()
        }

        ankeFromDB.apply {
            fromAnke(anke)
        }
        logger.debug("Anke successfully updated in db.")
        return ankeFromDB.toAnke()
    }

    fun deleteAnke(internalSaksnummer: String) {
        logger.debug("Deleting anke in db. Internal saksnummer: {}", internalSaksnummer)
        val ankeFromDB = getAnkeToModify(internalSaksnummer)
        ankeFromDB.apply {
            status = KlageAnkeStatus.DELETED.name
            modifiedByUser = Instant.now()
        }
        logger.debug("Anke successfully marked as deleted in db.")
    }

    private fun getAnkeToModify(internalSaksnummer: String): AnkeDAO {
        val inputUUID = UUID.fromString(internalSaksnummer)
        return AnkeDAO.find {
            Anker.internalSaksnummer eq inputUUID
        }.firstOrNull() ?: throw AnkeNotFoundException("Anke with internalSaksnummer $internalSaksnummer not found in db.")
    }
}