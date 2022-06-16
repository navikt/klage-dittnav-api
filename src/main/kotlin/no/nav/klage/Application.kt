package no.nav.klage

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import springfox.documentation.swagger2.annotations.EnableSwagger2

@SpringBootApplication
@EnableJwtTokenValidation(ignore = ["springfox"])
@EnableScheduling
@EnableSwagger2
class Application

fun main() {
    runApplication<Application>()
}