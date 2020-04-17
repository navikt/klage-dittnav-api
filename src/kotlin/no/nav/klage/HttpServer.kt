package no.nav.klage

import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.klage.common.exceptionHandler
import no.nav.klage.common.objectMapper
import no.nav.klage.routes.klageRoutes
import no.nav.klage.routes.naisRoutes

fun createHttpServer(
    applicationState: ApplicationState,
    prometheusRegistry: PrometheusMeterRegistry
): ApplicationEngine = embeddedServer(Netty, 7070) {

    install(StatusPages) {
        exceptionHandler()
    }

    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }

    install(MicrometerMetrics) {
        registry = prometheusRegistry
        meterBinders = listOf(
            ClassLoaderMetrics(),
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
            JvmThreadMetrics(),
            FileDescriptorMetrics()
        )
    }

    routing {
        klageRoutes()
        naisRoutes(
                readinessCheck = { applicationState.initialized },
                livenessCheck = { applicationState.running },
                collectorRegistry = prometheusRegistry.prometheusRegistry
        )
    }

    applicationState.initialized = true
}
