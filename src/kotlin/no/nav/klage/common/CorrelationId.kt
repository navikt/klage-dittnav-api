package no.nav.klage.common

import org.slf4j.MDC
import java.util.*

const val MDC_CALL_ID = "Nav-Call-Id"

val callIdGenerator: ThreadLocal<String> = ThreadLocal.withInitial {
    UUID.randomUUID().toString()
}

class CorrelationId(private val id: String) {
    override fun toString(): String = id
}

internal fun getCorrelationId(): CorrelationId =
        CorrelationId(MDC.get(MDC_CALL_ID))
