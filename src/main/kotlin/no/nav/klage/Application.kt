package no.nav.klage

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableJwtTokenValidation
class KlageDittNavAPIApplication

fun main() {
    runApplication<KlageDittNavAPIApplication>()
}