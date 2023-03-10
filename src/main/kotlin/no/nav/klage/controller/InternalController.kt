package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.domain.Journalpost
import no.nav.klage.service.AnkeService
import no.nav.klage.service.KlageService
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@ProtectedWithClaims(issuer = "azuread")
@RequestMapping("internal")
@Tag(name = "internal")
class InternalController(
    private val klageService: KlageService,
    private val ankeService: AnkeService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @PostMapping("/klager/{klageId}/journalpostid")
    fun setJournalpostIdInternal(
        @PathVariable klageId: Int,
        @RequestBody journalpost: Journalpost
    ) {
        logger.debug("Set journalpostId on klage is requested. KlageId: {}, journalpostId: {}", klageId, journalpost.id)
        klageService.setJournalpostIdWithoutValidation(klageId, journalpost.id)
    }

    @PostMapping("/anker/{ankeId}/journalpostid")
    fun setJournalpostIdInternal(
        @PathVariable ankeId: UUID,
        @RequestBody journalpost: Journalpost
    ) {
        logger.debug("Set journalpostId on anke is requested. AnkeId: {}, journalpostId: {}", ankeId, journalpost.id)
        ankeService.setJournalpostIdWithoutValidation(ankeId, journalpost.id)
    }

    @GetMapping("/klager/{klageId}/journalpostid")
    fun getJournalpostIdKlage(
        @PathVariable klageId: Int
    ): JournalpostIdResponse {
        logger.debug("Get journalpostId on klage is requested from an internal service. KlageId: {}", klageId)
        return JournalpostIdResponse(journalpostId = klageService.getJournalpostIdWithoutValidation(klageId))
    }

    @GetMapping("/anker/{ankeId}/journalpostid")
    fun getJournalpostIdAnke(
        @PathVariable ankeId: UUID
    ): JournalpostIdResponse {
        logger.debug("Get journalpostId on anke is requested from an internal service. AnkeId: {}", ankeId)
        return JournalpostIdResponse(journalpostId = ankeService.getJournalpostIdWithoutValidation(ankeId))
    }

    data class JournalpostIdResponse(val journalpostId: String?)
}