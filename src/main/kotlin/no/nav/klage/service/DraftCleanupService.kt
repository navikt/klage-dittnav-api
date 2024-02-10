package no.nav.klage.service

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.klage.clients.FileClient
import no.nav.klage.domain.KlageAnkeStatus
import no.nav.klage.domain.jpa.Anke
import no.nav.klage.domain.jpa.Klage
import no.nav.klage.repository.*
import no.nav.klage.util.causeClass
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.klage.util.rootCause
import no.nav.slackposter.Severity
import no.nav.slackposter.SlackClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@Transactional
class DraftCleanupService(
    private val slackClient: SlackClient,
    private val klankeRepository: KlankeRepository,
    private val klageService: KlageService,
    private val ankeService: AnkeService,
    private val fileClient: FileClient,
    @Value("\${MAX_DRAFT_AGE_IN_DAYS}")
    private val maxDraftAgeInDays: String,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @Scheduled(cron = "\${DRAFT_CLEANUP_CRON}", zone = "Europe/Oslo")
    @SchedulerLock(name = "markOldDraftsAsDeleted")
    fun markOldDraftsAsDeleted() {

        val lookingForDraftsMessage = "Looking for expired drafts"
        logger.debug(lookingForDraftsMessage)
        slackClient.postMessage(lookingForDraftsMessage)

        var vedleggFilesSuccessfullyDeleted = 0
        var vedleggSuccessfullyDeleted = 0

        var klagerSuccessfullyDeleted = 0
        var ankerSuccessfullyDeleted = 0

        val oldKlankeDrafts = klankeRepository.findByStatusAndModifiedByUserLessThan(
            status = KlageAnkeStatus.DRAFT,
            LocalDateTime.now().minus(maxDraftAgeInDays.toLong(), ChronoUnit.DAYS)
        )
        val expiredKlankeCount = oldKlankeDrafts.count()

        val foundDraftsMessage = "Found $expiredKlankeCount expired drafts"
        logger.debug(foundDraftsMessage)
        slackClient.postMessage(foundDraftsMessage)

        oldKlankeDrafts.forEach { klanke ->
            logger.debug("Cleaning up expired draft {}", klanke.id)
            runCatching {
                klanke.vedlegg.forEach { vedlegg ->
                    logger.debug("Cleaning up vedlegg {}", vedlegg.id)
                    kotlin.runCatching {
                        if (fileClient.deleteVedleggFile(vedlegg.ref)) {
                            vedleggFilesSuccessfullyDeleted++
                        }
                    }.onFailure { failure ->
                        logger.error("Could not remove attachment ${vedlegg.id}. See secure logs for details.")
                        secureLogger.error("Failed to remove attachment", failure)
                        slackClient.postMessage(
                            "Kunne ikke fjerne vedlegg! " +
                                    "(${causeClass(rootCause(failure))})", Severity.ERROR
                        )
                    }
                }
                vedleggSuccessfullyDeleted += klanke.vedlegg.size
                klanke.vedlegg.clear()

                when (klanke) {
                    is Klage -> {
                        klageService.updateStatusWithoutValidation(klanke.id, KlageAnkeStatus.DELETED)
                        klagerSuccessfullyDeleted++
                    }
                    is Anke -> {
                        ankeService.updateStatusWithoutValidation(klanke.id, KlageAnkeStatus.DELETED)
                        ankerSuccessfullyDeleted++
                    }

                    else -> {}
                }

            }.onFailure { failure ->
                logger.error("Could not clean up draft. See secure logs for details.")
                secureLogger.error("Failed to clean up draft", failure)
                slackClient.postMessage(
                    "Kunne ikke fjerne utgått utkast! " +
                            "(${causeClass(rootCause(failure))})", Severity.ERROR
                )
            }
        }

        if (expiredKlankeCount > 0) {
            val cleanupDoneMessage = """
                Removed $vedleggFilesSuccessfullyDeleted files in file storage.
                Removed $vedleggSuccessfullyDeleted vedlegg in db. 
                Removed $klagerSuccessfullyDeleted klage drafts in db.
                Removed $ankerSuccessfullyDeleted anke drafts in db.
            """.trimIndent()

            logger.debug(cleanupDoneMessage)
            slackClient.postMessage(cleanupDoneMessage)

        }
    }
}