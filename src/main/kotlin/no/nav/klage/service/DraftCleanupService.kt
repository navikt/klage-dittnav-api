package no.nav.klage.service

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.klage.clients.FileClient
import no.nav.klage.repository.AnkeRepository
import no.nav.klage.repository.AnkeVedleggRepository
import no.nav.klage.repository.KlageRepository
import no.nav.klage.repository.VedleggRepository
import no.nav.klage.util.causeClass
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.klage.util.rootCause
import no.nav.slackposter.Severity
import no.nav.slackposter.SlackClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class DraftCleanupService(
    private val slackClient: SlackClient,
    private val klageRepository: KlageRepository,
    private val ankeRepository: AnkeRepository,
    private val vedleggRepository: VedleggRepository,
    private val ankeVedleggRepository: AnkeVedleggRepository,
    private val fileClient: FileClient
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @Scheduled(cron = "\${DRAFT_CLEANUP_CRON}", zone = "Europe/Oslo")
    @SchedulerLock(name = "markOldDraftsAsDeleted")
    fun markOldDraftsAsDeleted() {

        logger.debug("Looking for expired draft klager")
        slackClient.postMessage("Ser etter utgåtte draft-klager")

        var vedleggFilesSuccessfullyDeleted = 0
        var vedleggSuccessfullyDeleted = 0
        var klagerSuccessfullyDeleted = 0
        var expiredKlager = 0

        var ankerSuccessfullyDeleted = 0
        var ankeVedleggSuccessfullyDeleted = 0
        var ankeVedleggFilesSuccessfullyDeleted = 0
        var expiredAnker = 0

        val oldKlageDrafts = klageRepository.getExpiredDraftKlager()
        expiredKlager = oldKlageDrafts.count()
        logger.debug("Found $expiredKlager expired draft klager")
        slackClient.postMessage("Fant $expiredKlager utgåtte draft-klager")

        oldKlageDrafts.forEach {
            logger.debug("Cleaning up expired draft klage ${it.id}")
            runCatching {
                it.vedlegg.forEach { vedlegg ->
                    logger.debug("Cleaning up vedlegg ${it.id}")
                    kotlin.runCatching {
                        if (fileClient.deleteVedleggFile(vedlegg.ref)) {
                            vedleggFilesSuccessfullyDeleted++
                        }
                        vedlegg.id?.let { vedleggId -> vedleggRepository.deleteVedlegg(vedleggId) }
                        vedleggSuccessfullyDeleted++
                    }.onFailure { failure ->
                        logger.error("Could not remove attachment ${vedlegg.id}. See secure logs for details.")
                        secureLogger.error("Failed to remove attachment", failure)
                        slackClient.postMessage(
                            "Kunne ikke fjerne vedlegg! " +
                                    "(${causeClass(rootCause(failure))})", Severity.ERROR
                        )
                    }
                }
                it.id?.let { klageId -> klageRepository.deleteKlage(klageId) }
                klagerSuccessfullyDeleted++
            }.onFailure { failure ->
                logger.error("Could not clean up draft. See secure logs for details.")
                secureLogger.error("Failed to clean up draft", failure)
                slackClient.postMessage(
                    "Kunne ikke fjerne utgått utkast! " +
                            "(${causeClass(rootCause(failure))})", Severity.ERROR
                )
            }
        }

        val oldAnkeDrafts = ankeRepository.getExpiredDraftAnker()
        expiredAnker = oldAnkeDrafts.count()
        logger.debug("Found $expiredAnker expired draft nker")
        slackClient.postMessage("Fant $expiredAnker utgåtte draft-anker")

        oldAnkeDrafts.forEach {
            logger.debug("Cleaning up expired draft anke ${it.id}")
            runCatching {
                it.vedlegg.forEach { vedlegg ->
                    logger.debug("Cleaning up ankevedlegg ${it.id}")
                    kotlin.runCatching {
                        if (fileClient.deleteVedleggFile(vedlegg.ref)) {
                            ankeVedleggFilesSuccessfullyDeleted++
                        }
                        vedlegg.id?.let { vedleggId -> ankeVedleggRepository.deleteAnkeVedlegg(vedleggId) }
                        ankeVedleggSuccessfullyDeleted++
                    }.onFailure { failure ->
                        logger.error("Could not remove attachment ${vedlegg.id}. See secure logs for details.")
                        secureLogger.error("Failed to remove attachment", failure)
                        slackClient.postMessage(
                            "Kunne ikke fjerne vedlegg! " +
                                    "(${causeClass(rootCause(failure))})", Severity.ERROR
                        )
                    }
                }
                ankeRepository.deleteAnke(it.internalSaksnummer)
                ankerSuccessfullyDeleted++
            }.onFailure { failure ->
                logger.error("Could not clean up draft. See secure logs for details.")
                secureLogger.error("Failed to clean up draft", failure)
                slackClient.postMessage(
                    "Kunne ikke fjerne utgått utkast! " +
                            "(${causeClass(rootCause(failure))})", Severity.ERROR
                )
            }
        }

        if (expiredAnker > 0) {
            logger.debug("Removed $ankeVedleggFilesSuccessfullyDeleted files in file storage, $ankeVedleggSuccessfullyDeleted ankevedlegg in db and $ankerSuccessfullyDeleted draft anker in db")
            slackClient.postMessage("Fjernet $ankeVedleggFilesSuccessfullyDeleted filer i GCP, $ankeVedleggSuccessfullyDeleted ankevedlegg i database og $ankerSuccessfullyDeleted draft-anker i database")
        }

        if (expiredKlager > 0) {
            logger.debug("Removed $vedleggFilesSuccessfullyDeleted files in file storage, $vedleggSuccessfullyDeleted vedlegg in db and $klagerSuccessfullyDeleted draft klager in db")
            slackClient.postMessage("Fjernet $vedleggFilesSuccessfullyDeleted filer i GCP, $vedleggSuccessfullyDeleted vedlegg i database og $klagerSuccessfullyDeleted draft-klager i database")
        }
    }
}