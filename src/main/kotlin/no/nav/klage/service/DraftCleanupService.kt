package no.nav.klage.service

import no.nav.klage.clients.FileClient
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
    private val vedleggRepository: VedleggRepository,
    private val fileClient: FileClient
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @Scheduled(cron = "\${DRAFT_CLEANUP_CRON", zone = "Europe/Oslo")
    fun markOldDraftsAsDeleted() {

        logger.debug("Looking for expired draft klager")
        slackClient.postMessage("Ser etter utgåtte draft-klager")

        var vedleggFilesSuccessfullyDeleted = 0
        var vedleggSuccessfullyDeleted = 0
        var klagerSuccessfullyDeleted = 0
        var expiredKlager = 0

        val oldDrafts = klageRepository.getExpiredDraftKlager()
        expiredKlager = oldDrafts.count()
        logger.debug("Found $expiredKlager expired draft klager")
        slackClient.postMessage("Fant $expiredKlager utgåtte draft-klager")

        oldDrafts.forEach {
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
                logger.error("Could not clean up draft klage. See secure logs for details.")
                secureLogger.error("Failed to clean up draft klage", failure)
                slackClient.postMessage(
                    "Kunne ikke fjerne utgått klage! " +
                            "(${causeClass(rootCause(failure))})", Severity.ERROR
                )
            }
        }

        if (expiredKlager > 0) {
            logger.debug("Removed $vedleggFilesSuccessfullyDeleted files in file storage, $vedleggSuccessfullyDeleted vedlegg in db and $klagerSuccessfullyDeleted draft klager in db")
            slackClient.postMessage("Fjernet $vedleggFilesSuccessfullyDeleted filer i GCP, $vedleggSuccessfullyDeleted vedlegg i database og $klagerSuccessfullyDeleted draft-klager i database")
        }
    }
}