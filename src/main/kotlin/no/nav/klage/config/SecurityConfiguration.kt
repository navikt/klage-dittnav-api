package no.nav.klage.config

import no.nav.klage.auth.AzureAuthProperties
import no.nav.klage.auth.TokenXAuthProperties
import no.nav.klage.auth.azureJwtDecoder
import no.nav.klage.auth.tokenXJwtDecoder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * Two filter chains, one per token type, so a token is only accepted where its issuer is expected:
 * Azure for the endpoints klage-arkiver-journalpost calls, TokenX (citizens) for everything else.
 * The decoders are created inside the chains, not as JwtDecoder beans, so Spring Boot cannot pick
 * the wrong one.
 */
@Configuration
@EnableWebSecurity
class SecurityConfiguration {
    companion object {
        const val AZURE_PATHS = "/internal/klanker/**"

        // Swagger is only enabled in dev (application-dev.yaml). In prod these paths give 404.
        val UNAUTHENTICATED_PATHS =
            listOf(
                "/internal/health",
                "/internal/health/**",
                "/internal/info",
                "/internal/prometheus",
                "/error",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/favicon.ico",
                "/ads.txt",
                "/robots.txt",
                "/api/innsendingsytelser/*",
            )
    }

    @Bean
    @Order(1)
    fun azureFilterChain(
        http: HttpSecurity,
        environment: Environment,
    ): SecurityFilterChain =
        http
            .securityMatcher(AZURE_PATHS)
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .oauth2ResourceServer { resourceServer ->
                resourceServer.jwt { it.decoder(azureJwtDecoder(AzureAuthProperties.from(environment))) }
            }.build()

    @Bean
    @Order(2)
    fun tokenXFilterChain(
        http: HttpSecurity,
        environment: Environment,
    ): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(*UNAUTHENTICATED_PATHS.toTypedArray()).permitAll()
                // PDF for citizens who are not logged in (uinnlogget klage).
                it.requestMatchers(HttpMethod.POST, "/api/pdf/klanke").permitAll()
                if (environment.acceptsProfiles(Profiles.of("dev"))) {
                    it.requestMatchers(HttpMethod.POST, "/internal/uinnloggetklage").permitAll()
                }
                it.anyRequest().authenticated()
            }.oauth2ResourceServer { resourceServer ->
                resourceServer.jwt { it.decoder(tokenXJwtDecoder(TokenXAuthProperties.from(environment))) }
            }.build()
}
