package no.nav.klage.service

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.klage.clients.FileClient
import no.nav.klage.repository.AnkeRepository
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
    private val fileClient: FileClient
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    //TODO create for anke

    @Scheduled(cron = "\${DRAFT_CLEANUP_CRON}", zone = "Europe/Oslo")
    @SchedulerLock(name = "markOldDraftsAsDeleted")
    fun markOldDraftsAsDeleted() {

        logger.debug("Looking for expired draft klager")
        slackClient.postMessage("Ser etter utgåtte draft-klager")

        var klageVedleggFilesSuccessfullyDeleted = 0
        var klageVedleggSuccessfullyDeleted = 0
        var klagerSuccessfullyDeleted = 0

        var ankerSuccessfullyDeleted = 0

        val oldKlageDrafts = klageRepository.getExpiredDraftKlager()
        val expiredKlager = oldKlageDrafts.count()
        logger.debug("Found $expiredKlager expired draft klager")
        slackClient.postMessage("Fant $expiredKlager utgåtte draft-klager")

        oldKlageDrafts.forEach {
            logger.debug("Cleaning up expired draft klage ${it.id}")
            runCatching {
                it.vedlegg.forEach { vedlegg ->
                    logger.debug("Cleaning up vedlegg ${it.id}")
                    kotlin.runCatching {
                        if (fileClient.deleteVedleggFile(vedlegg.ref)) {
                            klageVedleggFilesSuccessfullyDeleted++
                        }
                        vedlegg.id?.let { vedleggId -> vedleggRepository.deleteVedleggFromKlage(vedleggId) }
                        klageVedleggSuccessfullyDeleted++
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
        val expiredAnker = oldAnkeDrafts.count()
        logger.debug("Found $expiredAnker expired draft nker")
        slackClient.postMessage("Fant $expiredAnker utgåtte draft-anker")

        oldAnkeDrafts.forEach {
            logger.debug("Cleaning up expired draft anke ${it.id}")
            runCatching {
                ankeRepository.deleteAnke(it.id!!)
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
            logger.debug("Removed $ankerSuccessfullyDeleted draft anker in db")
            slackClient.postMessage("Fjernet $ankerSuccessfullyDeleted draft-anker i database")
        }

        if (expiredKlager > 0) {
            logger.debug("Removed $klageVedleggFilesSuccessfullyDeleted files in file storage, $klageVedleggSuccessfullyDeleted vedlegg in db and $klagerSuccessfullyDeleted draft klager in db")
            slackClient.postMessage("Fjernet $klageVedleggFilesSuccessfullyDeleted filer i GCP, $klageVedleggSuccessfullyDeleted vedlegg i database og $klagerSuccessfullyDeleted draft-klager i database")
        }
    }
}