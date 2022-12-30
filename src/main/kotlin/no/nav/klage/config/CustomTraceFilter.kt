package no.nav.klage.config

import brave.Tracer
import brave.baggage.BaggageField
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.GenericFilterBean
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

/**
 * Adding some custom NAV-specific attributes to standard Spring Sleuth
 */
@Component
@Profile("!local")
@Order(-20)
class CustomTraceFilter(
    private val tracer: Tracer,
    @Value("\${navCallIdName}") private val navCallIdName: String
) : GenericFilterBean() {

    override fun doFilter(
        request: ServletRequest?, response: ServletResponse,
        chain: FilterChain
    ) {
        val currentSpan = tracer.currentSpan()

        val navCallIdField = BaggageField.create(navCallIdName)
        navCallIdField.updateValue(tracer.currentSpan().context(), currentSpan.context().traceIdString())

        chain.doFilter(request, response)
    }
}