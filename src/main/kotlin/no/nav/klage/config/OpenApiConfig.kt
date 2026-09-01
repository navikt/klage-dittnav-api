package no.nav.klage.config

import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun api(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("standard")
            .pathsToMatch("/api/**")
            .build()
}
