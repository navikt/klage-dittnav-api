package no.nav.klage.controller

import no.nav.klage.controller.view.OpenKlageInput
import no.nav.klage.service.KlageDittnavPdfgenService
import no.nav.klage.util.getLogger
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@Profile("dev-gcp")
@RestController
class DevController(private val klageDittnavPdfgenService: KlageDittnavPdfgenService) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Unprotected
    @ResponseBody
    @PostMapping("/internal/uinnloggetklage")
    fun generateFoersteside(
        @RequestBody input: OpenKlageInput
    ): ResponseEntity<ByteArray> {
        logger.debug("Test create foersteside with input: {}", input)
        val data = klageDittnavPdfgenService.createKlagePdfWithFoersteside(input)

        val responseHeaders = HttpHeaders()
        responseHeaders.contentType = MediaType.APPLICATION_PDF
        responseHeaders.add("Content-Disposition", "inline; filename=klage.pdf")
        return ResponseEntity(
            data,
            responseHeaders,
            HttpStatus.OK
        )
    }
}