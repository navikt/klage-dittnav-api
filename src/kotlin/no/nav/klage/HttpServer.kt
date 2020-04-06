package no.nav.klage

import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.ApplicationState
import no.nav.klage.common.exceptionHandler
import no.nav.klage.common.objectMapper
import no.nav.klage.routes.naisRoutes

fun createHttpServer(
    applicationState: ApplicationState,
    useAuthentication: Boolean = true
//    configuration: Configuration = Configuration(),
//    azureAdOpenIdConfiguration: AzureAdOpenIdConfiguration = getAadConfig(configuration.azureAd),
//    services: Services = Services(configuration),
): ApplicationEngine = embeddedServer(Netty, 7070) {

    install(StatusPages) {
        exceptionHandler()
    }

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }

    routing {
        naisRoutes(
            readinessCheck = { applicationState.initialized },
            livenessCheck = { applicationState.running }
        )
    }

    applicationState.initialized = true
}
