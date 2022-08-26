package no.nav.klage.repository

import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.ankeOLD.*
import no.nav.klage.domain.exception.AnkeNotFoundException
import no.nav.klage.domain.exception.AttemptedIllegalUpdateException
import no.nav.klage.util.getLogger
import org.jetbrains.exposed.sql.and
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
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

    fun getExpiredDraftAnker(): List<AnkeOLD> {
        val expiryDate = Instant.now().minus(maxDraftAgeInDays.toLong(), ChronoUnit.DAYS)
        return AnkeOLDDAO.find { Anker.status eq KlageAnkeStatus.DRAFT.name and Anker.modifiedByUser.less(expiryDate) }
            .map {
                it.toAnke()
            }
    }

    fun getAnkeByInternalSaksnummer(internalSaksnummer: String): AnkeOLD {
        val inputUUID = UUID.fromString(internalSaksnummer)
        return AnkeOLDDAO.find {
            Anker.internalSaksnummer eq inputUUID
        }.firstOrNull()?.toAnke() ?: throw AnkeNotFoundException("Anke with internalSaksnummer $internalSaksnummer not found in db.")
    }

    fun getLatestDraftAnkeByFnrAndInternalSaksnummer(
        fnr: String,
        internalSaksnummer: String
    ): AnkeOLD? {
        val inputUUID = UUID.fromString(internalSaksnummer)
        return AnkeOLDDAO.find {
            Anker.foedselsnummer eq fnr and (Anker.internalSaksnummer eq inputUUID) and (Anker.status eq KlageAnkeStatus.DRAFT.toString())
        }.maxByOrNull { it.modifiedByUser }
            ?.toAnke()
    }

    fun createAnke(ankeOLD: AnkeOLD): AnkeOLD {
        logger.debug("Creating anke in db.")
        return AnkeOLDDAO.new {
            fromAnke(ankeOLD)
        }.toAnke().also {
            logger.debug("Anke successfully created in db. Id: {}", it.id)
        }
    }

    fun updateAnke(ankeOLD: AnkeOLD, checkWritableOnceFields: Boolean = true): AnkeOLD {
        logger.debug("Updating anke in db. Internal saksnummer: {}", ankeOLD.internalSaksnummer)
        val ankeFromDB = getAnkeToModify(ankeOLD.internalSaksnummer)

        if (checkWritableOnceFields && !ankeOLD.writableOnceFieldsMatch(ankeFromDB.toAnke())) {
            throw AttemptedIllegalUpdateException()
        }

        ankeFromDB.apply {
            fromAnke(ankeOLD)
        }
        logger.debug("Anke successfully updated in db.")
        return ankeFromDB.toAnke()
    }


    fun updateFritekst(internalSaksnummer: String, fritekst: String): AnkeOLD {
        logger.debug("Updating anke fritekst in db. Id: {}", internalSaksnummer)
        val ankeFromDB = getAnkeToModify(internalSaksnummer)
        ankeFromDB.apply {
            this.fritekst = fritekst
            this.modifiedByUser = Instant.now()
        }

        logger.debug("Anke fritekst successfully updated in db.")
        return ankeFromDB.toAnke()
    }

    fun updateVedtakDate(internalSaksnummer: String, vedtakDate: LocalDate?): AnkeOLD {
        logger.debug("Updating anke vedtakDate in db. Id: {}", internalSaksnummer)
        val ankeFromDB = getAnkeToModify(internalSaksnummer)
        ankeFromDB.apply {
            this.vedtakDate = vedtakDate
            this.modifiedByUser = Instant.now()
        }

        logger.debug("Anke vedtakDate successfully updated in db.")
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

    private fun getAnkeToModify(internalSaksnummer: String): AnkeOLDDAO {
        val inputUUID = UUID.fromString(internalSaksnummer)
        return AnkeOLDDAO.find {
            Anker.internalSaksnummer eq inputUUID
        }.firstOrNull() ?: throw AnkeNotFoundException("Anke with internalSaksnummer $internalSaksnummer not found in db.")
    }
}