package no.nav.klage.config

import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun api(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("standard")
            .pathsToExclude("/api/old/**")
            .pathsToMatch("/api/**")
            .build()
    }
}