package no.nav.klage.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsGlobalConfiguration {

    @Value("\${allowed.origins}")
    private lateinit var allowedOrigins: List<String>

    @Bean
    fun corsServletFilterRegistration(): FilterRegistrationBean<CorsFilter> {
        val config = corsConfiguration()
        val source = corsConfigurationSource(config)
        val corsFilter = CorsFilter(source)
        val bean = FilterRegistrationBean<CorsFilter>()
        bean.filter = corsFilter
        bean.order = Ordered.HIGHEST_PRECEDENCE
        bean.urlPatterns = setOf("/*")
        bean.isEnabled = true
        return bean
    }

    private fun corsConfigurationSource(config: CorsConfiguration): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }

    private fun corsConfiguration(): CorsConfiguration {
        val config = CorsConfiguration().applyPermitDefaultValues()
        config.allowedOrigins = allowedOrigins
        config.addAllowedMethod("*")
        config.allowCredentials = true
        config.maxAge = 3600L
        return config
    }
}