package no.nav.klage

import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableJwtTokenValidation(ignore = ["org.springdoc"])
@EnableOAuth2Client(cacheEnabled = true)
@EnableScheduling
class Application

fun main() {
    runApplication<Application>()
}