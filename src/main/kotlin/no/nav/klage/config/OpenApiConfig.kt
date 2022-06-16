package no.nav.klage.config

import no.nav.klage.controller.KlageController
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.ResponseEntity
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.Tag
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket

@Configuration
class OpenApiConfig {

    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.OAS_30)
            .select()
            .apis(RequestHandlerSelectors.basePackage(KlageController::class.java.packageName))
            .build()
            .pathMapping("/")
            .genericModelSubstitutes(ResponseEntity::class.java)
            .tags(
                Tag("anker", "API for anker i klage-dittnav-api"),
                Tag("klager", "API for klager i klage-dittnav-api"),
                Tag("bruker", "API for brukerinfo i klage-dittnav-api"),
                Tag("tema", "API for TemaResponse i klage-dittnav-api"),
                Tag("titles", "API for titler"),
            )
    }
}