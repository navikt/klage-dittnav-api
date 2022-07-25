package no.nav.klage.controller

import no.nav.klage.clients.foerstesidegenerator.FoerstesidegeneratorClient
import no.nav.klage.util.getLogger
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Profile("dev-gcp")
@RestController
class DevController(private val foerstesidegeneratorClient: FoerstesidegeneratorClient) {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val logger = getLogger(javaClass.enclosingClass)
    }

    @Unprotected
    @GetMapping("/internal/foersteside")
    fun generateFoersteside() {
        try {
            logger.debug("Test create foersteside")
            foerstesidegeneratorClient.createFoersteside()
        } catch (e: Exception) {
            logger.warn("Failed to create foersteside")
            throw e
        }
    }
}