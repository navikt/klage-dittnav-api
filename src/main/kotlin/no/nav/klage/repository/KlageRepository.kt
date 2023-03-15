package no.nav.klage.repository

import no.nav.klage.domain.Bruker
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.Tema
import no.nav.klage.domain.exception.AttemptedIllegalUpdateException
import no.nav.klage.domain.exception.KlageNotFoundException
import no.nav.klage.domain.klage.*
import no.nav.klage.domain.titles.Innsendingsytelse
import no.nav.klage.util.getLogger
import org.jetbrains.exposed.sql.and
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Repository
class KlageRepository {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Value("\${MAX_DRAFT_AGE_IN_DAYS}")
    private lateinit var maxDraftAgeInDays: String

    fun getExpiredDraftKlager(): List<Klage> {
        val expiryDate = Instant.now().minus(maxDraftAgeInDays.toLong(), ChronoUnit.DAYS)
        return KlageDAO.find { Klager.status eq KlageAnkeStatus.DRAFT.name and Klager.modifiedByUser.less(expiryDate) }
            .map {
                it.toKlage()
            }
    }

    fun getKlageById(id: String): Klage {
        return KlageDAO.findById(id.toInt())?.toKlage() ?: throw KlageNotFoundException("Klage with id $id not found in db.")
    }

    fun getDraftKlagerByFnr(fnr: String): List<Klage> {
        return KlageDAO.find { Klager.foedselsnummer eq fnr and (Klager.status eq KlageAnkeStatus.DRAFT.toString()) }
            .map { it.toKlage() }
    }

    fun getLatestKlageDraft(
        fnr: String,
        tema: Tema,
        internalSaksnummer: String?,
        innsendingsytelse: Innsendingsytelse
    ): Klage? {
        return KlageDAO.find {
            if (internalSaksnummer.isNullOrBlank()) {
                Klager.foedselsnummer eq fnr and (Klager.tema eq tema.name) and (Klager.innsendingsytelse eq innsendingsytelse.name) and (Klager.internalSaksnummer.isNull()) and (Klager.status eq KlageAnkeStatus.DRAFT.toString())
            } else {
                Klager.foedselsnummer eq fnr and (Klager.tema eq tema.name) and (Klager.innsendingsytelse eq innsendingsytelse.name) and (Klager.internalSaksnummer eq internalSaksnummer) and (Klager.status eq KlageAnkeStatus.DRAFT.toString())
            }
        }.maxByOrNull { it.modifiedByUser }
            ?.toKlage()
    }

    fun createKlage(klageFullInput: KlageFullInput, bruker: Bruker): Klage {
        logger.debug("Creating klage in db.")
        return KlageDAO.new {
            fromKlageFullInput(klageFullInput = klageFullInput, bruker = bruker)
        }.toKlage().also {
            logger.debug("Klage successfully created in db. Id: {}", it.id)
        }
    }

    fun createKlage(klageInput: KlageInput, bruker: Bruker): Klage {
        logger.debug("Creating klage in db.")
        return KlageDAO.new {
            fromKlageInput(klageInput = klageInput, bruker = bruker)
        }.toKlage().also {
            logger.debug("Klage successfully created in db. Id: {}", it.id)
        }
    }

    fun updateJournalpostId(id: String, journalpostId: String): Klage {
        logger.debug("Updating klage journalpostId in db. Id: {}", id)
        val klageFromDB = getKlageToModify(id.toInt())
        klageFromDB.apply {
            this.journalpostId = journalpostId
            this.modifiedByUser = Instant.now()
        }

        logger.debug("Klage journalpostId successfully updated in db.")
        return klageFromDB.toKlage()
    }

    fun updateFritekst(id: String, fritekst: String): Klage {
        logger.debug("Updating klage fritekst in db. Id: {}", id)
        val klageFromDB = getKlageToModify(id.toInt())
        klageFromDB.apply {
            this.fritekst = fritekst
            this.modifiedByUser = Instant.now()
        }

        logger.debug("Klage fritekst successfully updated in db.")
        return klageFromDB.toKlage()
    }

    fun updateUserSaksnummer(id: String, userSaksnummer: String?): Klage {
        logger.debug("Updating klage userSaksnummer in db. Id: {}", id)
        val klageFromDB = getKlageToModify(id.toInt())
        klageFromDB.apply {
            this.userSaksnummer = userSaksnummer
            this.modifiedByUser = Instant.now()
        }

        logger.debug("Klage userSaksnummer successfully updated in db.")
        return klageFromDB.toKlage()
    }

    fun updatePdfDownloaded(id: String, pdfDownloaded: Instant?): Klage {
        logger.debug("Updating klage pdfDownloaded in db. Id: {}", id)
        val klageFromDB = getKlageToModify(id.toInt())
        klageFromDB.apply {
            this.pdfDownloaded = pdfDownloaded
            this.modifiedByUser = Instant.now()
        }

        logger.debug("Klage pdfDownloaded successfully updated in db.")
        return klageFromDB.toKlage()
    }

    fun updateVedtakDate(id: String, vedtakDate: LocalDate?): Klage {
        logger.debug("Updating klage vedtakDate in db. Id: {}", id)
        val klageFromDB = getKlageToModify(id.toInt())
        klageFromDB.apply {
            this.vedtakDate = vedtakDate
            this.modifiedByUser = Instant.now()
        }

        logger.debug("Klage vedtakDate successfully updated in db.")
        return klageFromDB.toKlage()
    }

    fun updateHasVedlegg(id: String, hasVedlegg: Boolean): Klage {
        logger.debug("Updating klage hasVedlegg in db. Id: {}", id)
        val klageFromDB = getKlageToModify(id.toInt())
        klageFromDB.apply {
            this.hasVedlegg = hasVedlegg
            this.modifiedByUser = Instant.now()
        }

        logger.debug("Klage hasVedlegg successfully updated in db.")
        return klageFromDB.toKlage()
    }

    fun updateCheckboxesSelected(id: String, checkboxesSelected: Set<CheckboxEnum>?): Klage {
        logger.debug("Updating klage checkboxesSelected in db. Id: {}", id)
        val klageFromDB = getKlageToModify(id.toInt())
        klageFromDB.apply {
            checkboxesSelected?.let {
                checkBoxesSelected = checkboxesSelected.joinToString(",") { x -> x.toString() }
                this.modifiedByUser = Instant.now()
            }
        }

        logger.debug("Klage checkboxesSelected successfully updated in db.")
        return klageFromDB.toKlage()
    }

    fun updateStatus(id: String, newStatus: KlageAnkeStatus): Klage {
        logger.debug("Updating klage status in db. Id: {}", id)
        val klageFromDB = getKlageToModify(id.toInt())
        klageFromDB.apply {
            status = newStatus.name
            modifiedByUser = Instant.now()
        }
        logger.debug("Klage status successfully updated db.")
        return klageFromDB.toKlage()
    }

    private fun getKlageToModify(id: Int?): KlageDAO {
        return KlageDAO.findById(checkNotNull(id)) ?: throw KlageNotFoundException("Klage with id $id not found in db.")
    }
}
