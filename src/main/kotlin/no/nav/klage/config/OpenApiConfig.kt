package no.nav.klage.config

import no.nav.klage.controller.InternalController
import no.nav.klage.controller.KlageController
import no.nav.klage.controller.KlageControllerPrefixed
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
            .groupName("standard")
            .genericModelSubstitutes(ResponseEntity::class.java)
            .tags(
                Tag("anker", "API for anker i klage-dittnav-api"),
                Tag("klager", "API for klager i klage-dittnav-api"),
                Tag("bruker", "API for brukerinfo i klage-dittnav-api"),
                Tag("tema", "API for TemaResponse i klage-dittnav-api"),
                Tag("titles", "API for titler"),
            )
    }

    @Bean
    fun prefixedApi(): Docket {
        return Docket(DocumentationType.OAS_30)
            .select()
            .apis(RequestHandlerSelectors.basePackage(KlageControllerPrefixed::class.java.packageName))
            .build()
            .pathMapping("/")
            .groupName("prefixed")
            .genericModelSubstitutes(ResponseEntity::class.java)
            .tags(
                Tag("anker-prefixed", "API for anker i klage-dittnav-api"),
                Tag("klager-prefixed", "API for klager i klage-dittnav-api"),
                Tag("bruker-prefixed", "API for brukerinfo i klage-dittnav-api"),
                Tag("tema-prefixed", "API for TemaResponse i klage-dittnav-api"),
                Tag("titles-prefixed", "API for titler"),
            )
    }

    @Bean
    fun internalApi(): Docket {
        return Docket(DocumentationType.OAS_30)
            .select()
            .apis(RequestHandlerSelectors.basePackage(InternalController::class.java.packageName))
            .build()
            .pathMapping("/")
            .groupName("internal")
            .genericModelSubstitutes(ResponseEntity::class.java)
            .tags(
                Tag("internal", "API for intern integrasjon i klage-dittnav-api"),
            )
    }
}