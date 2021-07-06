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

@Repository
class AnkeRepository {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value("\${MAX_DRAFT_AGE_IN_DAYS}")
    private lateinit var maxDraftAgeInDays: String

    fun getAnkeById(id: Int): Anke {
        return AnkeDAO.findById(id)?.toAnke() ?: throw AnkeNotFoundException("Anke with id $id not found in db.")
    }

    fun getLatestDraftAnkeByFnrAndInternalSaksnummer(
        fnr: String,
        internalSaksnummer: String
    ): Anke? {
        return AnkeDAO.find {
            Anker.foedselsnummer eq fnr and (Anker.internalSaksnummer eq internalSaksnummer) and (Anker.status eq KlageAnkeStatus.DRAFT.toString())
        }.maxBy { it.modifiedByUser }
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
        logger.debug("Updating anke in db. Id: {}", anke.id)
        val ankeFromDB = getAnkeToModify(anke.id)

        if (checkWritableOnceFields && !anke.writableOnceFieldsMatch(ankeFromDB.toAnke())) {
            throw AttemptedIllegalUpdateException()
        }

        ankeFromDB.apply {
            fromAnke(anke)
        }
        logger.debug("Anke successfully updated in db.")
        return ankeFromDB.toAnke()
    }

    fun deleteAnke(id: Int) {
        logger.debug("Deleting anke in db. Id: {}", id)
        val ankeFromDB = getAnkeToModify(id)
        ankeFromDB.apply {
            status = KlageAnkeStatus.DELETED.name
            modifiedByUser = Instant.now()
        }
        logger.debug("Anke successfully marked as deleted in db.")
    }

    private fun getAnkeToModify(id: Int?): AnkeDAO {
        return AnkeDAO.findById(checkNotNull(id)) ?: throw AnkeNotFoundException("Anke with id $id not found in db.")
    }
}