package no.nav.klage.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsGlobalConfiguration {

    @Value("\${allowed.origins}")
    private lateinit var allowedOrigins: List<String>

    @Bean
    fun corsFilter(): CorsFilter {
        val corsConfig = CorsConfiguration()
        corsConfig.allowedOrigins = allowedOrigins
        corsConfig.maxAge = 3600L
        corsConfig.addAllowedMethod("GET")
        corsConfig.addAllowedMethod("POST")
        corsConfig.addAllowedMethod("PUT")
        corsConfig.addAllowedMethod("DELETE")
        corsConfig.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", corsConfig)
        return CorsFilter(source)
    }
}