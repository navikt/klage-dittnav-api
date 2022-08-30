package no.nav.klage.repository

import no.nav.anke.domain.anke.AnkeDAO
import no.nav.anke.domain.anke.Anker
import no.nav.anke.domain.anke.fromAnke
import no.nav.anke.domain.anke.toAnke
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.anke.Anke
import no.nav.klage.domain.exception.AnkeNotFoundException
import no.nav.klage.domain.titles.TitleEnum
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

    fun getExpiredDraftAnker(): List<Anke> {
        val expiryDate = Instant.now().minus(maxDraftAgeInDays.toLong(), ChronoUnit.DAYS)
        return AnkeDAO.find { Anker.status eq KlageAnkeStatus.DRAFT.name and Anker.modifiedByUser.less(expiryDate) }
            .map {
                it.toAnke()
            }
    }

    fun getAnkeById(id: UUID): Anke {
        return AnkeDAO.findById(id)?.toAnke() ?: throw AnkeNotFoundException("Anke with id $id not found in db.")
    }

    fun getDraftAnkerByFnr(fnr: String): List<Anke> {
        return AnkeDAO.find { Anker.foedselsnummer eq fnr and (Anker.status eq KlageAnkeStatus.DRAFT.toString()) }
            .map { it.toAnke() }
    }

    fun getLatestDraftAnkeByFnrTitleKey(
        fnr: String,
        titleKey: TitleEnum
    ): Anke? {
        return AnkeDAO.find {
            Anker.foedselsnummer eq fnr and (Anker.titleKey eq titleKey.name) and (Anker.status eq KlageAnkeStatus.DRAFT.toString())
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

    fun updateFritekst(id: UUID, fritekst: String): Anke {
        logger.debug("Updating anke fritekst in db. Id: {}", id)
        val ankeFromDB = getAnkeToModify(id)
        ankeFromDB.apply {
            this.fritekst = fritekst
            this.modifiedByUser = Instant.now()
        }

        logger.debug("Anke fritekst successfully updated in db.")
        return ankeFromDB.toAnke()
    }

    fun updateUserSaksnummer(id: UUID, userSaksnummer: String?): Anke {
        logger.debug("Updating anke userSaksnummer in db. Id: {}", id)
        val ankeFromDB = getAnkeToModify(id)
        ankeFromDB.apply {
            this.userSaksnummer = userSaksnummer
            this.modifiedByUser = Instant.now()
        }

        logger.debug("Anke userSaksnummer successfully updated in db.")
        return ankeFromDB.toAnke()
    }

    fun updateEnhetsnummer(id: UUID, enhetsnummer: String?): Anke {
        logger.debug("Updating anke enhetsnummer in db. Id: {}", id)
        val ankeFromDB = getAnkeToModify(id)
        ankeFromDB.apply {
            this.enhetsnummer = enhetsnummer
            this.modifiedByUser = Instant.now()
        }

        logger.debug("Anke enhetsnummer successfully updated in db.")
        return ankeFromDB.toAnke()
    }

    fun updateVedtakDate(id: UUID, vedtakDate: LocalDate?): Anke {
        logger.debug("Updating anke vedtakDate in db. Id: {}", id)
        val ankeFromDB = getAnkeToModify(id)
        ankeFromDB.apply {
            this.vedtakDate = vedtakDate
            this.modifiedByUser = Instant.now()
        }

        logger.debug("Anke vedtakDate successfully updated in db.")
        return ankeFromDB.toAnke()
    }

    fun updateHasVedlegg(id: UUID, hasVedlegg: Boolean): Anke {
        logger.debug("Updating anke hasVedlegg in db. Id: {}", id)
        val ankeFromDB = getAnkeToModify(id)
        ankeFromDB.apply {
            this.hasVedlegg = hasVedlegg
            this.modifiedByUser = Instant.now()
        }

        logger.debug("Anke hasVedlegg successfully updated in db.")
        return ankeFromDB.toAnke()
    }

    fun deleteAnke(id: UUID) {
        logger.debug("Deleting anke in db. Id: {}", id)
        val ankeFromDB = getAnkeToModify(id)
        ankeFromDB.apply {
            status = KlageAnkeStatus.DELETED.name
            modifiedByUser = Instant.now()
        }
        logger.debug("Anke successfully marked as deleted in db.")
    }

    private fun getAnkeToModify(id: UUID?): AnkeDAO {
        return AnkeDAO.findById(checkNotNull(id)) ?: throw AnkeNotFoundException("Anke with id $id not found in db.")
    }
}
