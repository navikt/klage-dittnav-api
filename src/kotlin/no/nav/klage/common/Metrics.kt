package no.nav.klage.common


import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.micrometer.prometheus.PrometheusRenameFilter


fun configurePrometheusMeterRegistry(): PrometheusMeterRegistry {
    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    prometheusRegistry.config().meterFilter(PrometheusRenameFilter())
    Metrics.globalRegistry.add(prometheusRegistry)
    return prometheusRegistry
}

fun apiCounter(): Counter = Counter
    .builder("api_hit_counter")
    .description("Registers a counter for each hit to the api")
    .register(Metrics.globalRegistry)

