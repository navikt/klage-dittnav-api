package no.nav.klage.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.klage.controller.view.OpenKlageInput
import no.nav.klage.service.KlageDittnavPdfgenService
import no.nav.klage.util.getLogger
import no.nav.klage.util.getSecureLogger
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "pdf")
@Unprotected
@RequestMapping("/api/pdf")
class PdfController(
    private val klageDittnavPdfgenService: KlageDittnavPdfgenService
){

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
        private val secureLogger = getSecureLogger()
    }

    @ResponseBody
    @PostMapping
    fun createPdf(
        @RequestBody input: OpenKlageInput
    ): ResponseEntity<ByteArray> {
        logger.debug("Create klage pdf is requested.")
        secureLogger.debug(
            "Create klage pdf is requested. Input: {} ",
            input,
        )

        val content = klageDittnavPdfgenService.createKlagePdfWithFoersteside(input)

        val responseHeaders = HttpHeaders()
        responseHeaders.contentType = MediaType.valueOf("application/pdf")
        responseHeaders.add("Content-Disposition", "attachment; filename=" + "klage.pdf")
        return ResponseEntity(
            content,
            responseHeaders,
            HttpStatus.OK
        )
    }
}