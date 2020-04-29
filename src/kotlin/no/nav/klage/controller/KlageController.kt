package no.nav.klage.controller

import no.nav.klage.common.KlageMetrics
import no.nav.klage.domain.Klage
import no.nav.klage.getLogger
import no.nav.klage.repository.KlageRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletResponse

@RestController
class KlageController(
    private val klageRepository: KlageRepository, private val metrics: KlageMetrics
) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    //Security: verify user token for all

    @GetMapping("/bruker")
    fun getBruker() {
        //return user based on token
        TODO()
    }

    @GetMapping("/klager")
    fun getKlager(): List<Klage> {
        //Fetch from user based on token
        return klageRepository.getKlager()
    }

    @PostMapping("/klager")
    fun createKlage(@RequestBody klage: Klage, response: HttpServletResponse): Klage {
        val createdKlage = klageRepository.addKlage(klage)
        response.status = HttpStatus.CREATED.value()
        return createdKlage.also {
            metrics.incrementKlagerCreated()
        }
    }

    @PutMapping("/klager/{id}")
    fun updateKlage(@PathVariable id: String, @RequestBody klage: Klage) {
        //Only possible for status = draft
        //Update in DB
        //return updated klage
    }

    @DeleteMapping("/klager/{id}")
    fun deleteKlage(@PathVariable id: String) {
        //mark as deleted in DB. Only possible for status = draft
        TODO()
    }
}