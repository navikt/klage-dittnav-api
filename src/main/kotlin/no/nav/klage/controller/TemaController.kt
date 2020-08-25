package no.nav.klage.controller

import no.nav.klage.domain.Tema
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
@Unprotected
class TemaController {

    @GetMapping("/temaer/{code}")
    fun getTema(@PathVariable code: String): ResponseEntity<TemaResponse> {
        return try {
            ResponseEntity.ok(Tema.valueOf(code).toResponse())
        } catch (iae: IllegalArgumentException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    private fun Tema.toResponse() = TemaResponse(this.name, this.beskrivelse)

    data class TemaResponse(val code: String, val value: String)
}
