package no.nav.klage.controller

import no.nav.klage.domain.JoarkStatus
import no.nav.klage.domain.Journalpost
import no.nav.klage.service.KlageService
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

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
        logger.debug("Set journalpostId on klage is called. KlageId: {}, journalpostId: {}", klageId, journalpost.id)
        klageService.setJournalpostId(klageId, journalpost.id)
    }

    @PostMapping("/joarkstatus/{journalpostId}")
    fun setJoarkStatus(
        @PathVariable journalpostId: String,
        @RequestBody joarkStatus: JoarkStatus
    ) {
        logger.debug("Set Joark status on klage with journalpostId {} is called.", journalpostId)
        klageService.setJoarkstatus(journalpostId, joarkStatus)
    }
}