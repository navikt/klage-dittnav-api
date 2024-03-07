package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.domain.Journalpost
import no.nav.klage.service.CommonService
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
    private val commonService: CommonService,
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @PostMapping("/klager/{klageId}/journalpostid")
    fun setJournalpostIdInternalOnKlage(
        @PathVariable klageId: UUID,
        @RequestBody journalpost: Journalpost
    ) {
        logger.debug("Set journalpostId on klage is requested. KlageId: {}, journalpostId: {}", klageId, journalpost.id)
        commonService.setJournalpostIdWithoutValidation(klageId, journalpost.id)
    }

    @PostMapping("/anker/{ankeId}/journalpostid")
    fun setJournalpostIdInternalOnAnke(
        @PathVariable ankeId: UUID,
        @RequestBody journalpost: Journalpost
    ) {
        logger.debug("Set journalpostId on anke is requested. AnkeId: {}, journalpostId: {}", ankeId, journalpost.id)
        commonService.setJournalpostIdWithoutValidation(ankeId, journalpost.id)
    }

    @GetMapping("/klager/{klageId}/journalpostid")
    fun getJournalpostIdKlage(
        @PathVariable klageId: UUID
    ): JournalpostIdResponse {
        logger.debug("Get journalpostId on klage is requested from an internal service. KlageId: {}", klageId)
        return JournalpostIdResponse(journalpostId = commonService.getJournalpostIdWithoutValidation(klageId))
    }

    @GetMapping("/anker/{ankeId}/journalpostid")
    fun getJournalpostIdAnke(
        @PathVariable ankeId: UUID
    ): JournalpostIdResponse {
        logger.debug("Get journalpostId on anke is requested from an internal service. AnkeId: {}", ankeId)
        return JournalpostIdResponse(journalpostId = commonService.getJournalpostIdWithoutValidation(ankeId))
    }

    @PostMapping("/klanker/{klankeId}/journalpostid")
    fun setJournalpostIdInternalOnKlanke(
        @PathVariable klankeId: UUID,
        @RequestBody journalpost: Journalpost
    ) {
        logger.debug("Set journalpostId on klanke is requested. KlankeId: {}, journalpostId: {}", klankeId, journalpost.id)
        commonService.setJournalpostIdWithoutValidation(klankeId, journalpost.id)
    }

    @GetMapping("/klanker/{klankeId}/journalpostid")
    fun getJournalpostIdKlanke(
        @PathVariable klankeId: UUID
    ): JournalpostIdResponse {
        logger.debug("Get journalpostId on klanke is requested from an internal service. KlankeId: {}", klankeId)
        return JournalpostIdResponse(journalpostId = commonService.getJournalpostIdWithoutValidation(klankeId))
    }

    data class JournalpostIdResponse(val journalpostId: String?)
}