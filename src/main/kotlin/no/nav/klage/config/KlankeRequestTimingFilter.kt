package no.nav.klage.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.klage.util.getLogger
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.TimeUnit

internal const val TIMED_REQUEST_CONTROLLER_ENTRY_NANOS = "no.nav.klage.timedRequest.controllerEntryNanos"

@Component
class KlankeRequestTimingFilter : OncePerRequestFilter() {
    companion object {
        private val klankePath =
            Regex("^/api/klanker/([a-fA-F0-9]{8}-(?:[a-fA-F0-9]{4}-){3}[a-fA-F0-9]{12})/(vedlegg|fritekst)$")
        private val timingLogger = getLogger(KlankeRequestTimingFilter::class.java)
        private val slowFritekstThresholdNanos = TimeUnit.SECONDS.toNanos(5)
    }

    private data class TimedRequest(
        val klankeId: String,
        val operation: String,
    )

    private fun timedRequest(request: HttpServletRequest): TimedRequest? {
        val path = request.requestURI.removePrefix(request.contextPath)
        val (klankeId, endpoint) = klankePath.matchEntire(path)?.destructured ?: return null
        val operation =
            when {
                request.method == "POST" && endpoint == "vedlegg" -> "attachment_upload"
                request.method == "PUT" && endpoint == "fritekst" -> "fritekst_update"
                else -> null
            }
        return operation?.let { TimedRequest(klankeId, it) }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = timedRequest(request) == null

    internal fun shouldLogTiming(
        operation: String,
        elapsedNanos: Long,
    ): Boolean = operation == "attachment_upload" || elapsedNanos > slowFritekstThresholdNanos

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val timedRequest = checkNotNull(timedRequest(request))
        val started = System.nanoTime()
        var completed = false
        try {
            filterChain.doFilter(request, response)
            completed = true
        } finally {
            val finished = System.nanoTime()
            val elapsedNanos = finished - started
            if (shouldLogTiming(timedRequest.operation, elapsedNanos)) {
                val controllerEntry = request.getAttribute(TIMED_REQUEST_CONTROLLER_ENTRY_NANOS) as? Long
                timingLogger.info(
                    "Klanke request timing: operation={}, klankeId={}, contentLengthBytes={}, beforeControllerMs={}, afterControllerMs={}, totalMs={}, status={}",
                    timedRequest.operation,
                    timedRequest.klankeId,
                    request.contentLengthLong,
                    controllerEntry?.let { TimeUnit.NANOSECONDS.toMillis(it - started) },
                    controllerEntry?.let { TimeUnit.NANOSECONDS.toMillis(finished - it) },
                    TimeUnit.NANOSECONDS.toMillis(elapsedNanos),
                    if (completed) response.status.toString() else "unhandled_exception",
                )
            }
        }
    }
}
