package no.nav.klage.controller

import no.nav.klage.domain.Journalpost
import no.nav.klage.service.KlageService
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*

@RestController
@ProtectedWithClaims(issuer = "azuread")
class InternalController(
    private val klageService: KlageService
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @PostMapping("/klager/{klageId}/journalpostid")
    fun setJournalpostId(
        @PathVariable klageId: Int,
        @RequestBody journalpost: Journalpost
    ) {
        logger.debug("Set journalpostId on klage is requested. KlageId: {}, journalpostId: {}", klageId, journalpost.id)
        klageService.setJournalpostIdWithoutValidation(klageId, journalpost.id)
    }

    @PostMapping("/klager/{klageId}/internal/journalpostid")
    fun setJournalpostIdInternal(
        @PathVariable klageId: Int,
        @RequestBody journalpost: Journalpost
    ) {
        logger.debug("Set journalpostId on klage is requested. KlageId: {}, journalpostId: {}", klageId, journalpost.id)
        klageService.setJournalpostIdWithoutValidation(klageId, journalpost.id)
    }

    @GetMapping("/klager/{klageId}/internal/journalpostid")
    fun getJournalpostId(
        @PathVariable klageId: Int
    ) {
        logger.debug("Get journalpostId on klage is requested. KlageId: {}", klageId)
        klageService.getJournalpostIdWithoutValidation(klageId)
    }
}