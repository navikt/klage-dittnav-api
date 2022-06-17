package no.nav.klage.controller

import io.swagger.annotations.Api
import no.nav.klage.domain.Tema
import no.nav.klage.util.getLogger
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Unprotected
@Api(tags = ["tema-prefixed"])
@RequestMapping("/api/temaer")
class TemaControllerPrefixed {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @GetMapping("/{code}")
    fun getTema(@PathVariable code: String): ResponseEntity<TemaResponse> {
        return try {
            ResponseEntity.ok(Tema.valueOf(code).toResponse())
        } catch (iae: IllegalArgumentException) {
            logger.warn("Trying to get tema with illegal code: $code", iae)
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            logger.error("Unexpected exception. Could not get tema with code: $code", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    private fun Tema.toResponse() = TemaResponse(this.name, this.beskrivelse)

    data class TemaResponse(val code: String, val value: String)
}
