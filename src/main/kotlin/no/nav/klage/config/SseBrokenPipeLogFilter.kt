package no.nav.klage.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.turbo.TurboFilter
import ch.qos.logback.core.spi.FilterReply
import no.nav.klage.util.getLogger
import org.slf4j.Marker

class SseBrokenPipeLogFilter : TurboFilter() {

    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val ourLogger = getLogger(javaClass.enclosingClass)
    }

    override fun decide(
        marker: Marker?,
        logger: Logger?,
        level: Level?,
        format: String?,
        params: Array<out Any>?,
        throwable: Throwable?
    ): FilterReply {
        if (level == Level.WARN && logger?.name == "org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver" && format?.startsWith("Failure in @ExceptionHandler no.nav.klage.config.problem.GlobalExceptionHandler") == true) {
            ourLogger.debug("Suppressing warning log message from ${logger.name}. This is probably due to lost client during async/SSE operations.")
            return FilterReply.DENY
        }

        if (throwable != null) {
            val rootCause = getRootCause(throwable)
            if (
                throwable.javaClass.name == "jakarta.servlet.ServletException" &&
                rootCause.javaClass.name == "no.nav.security.token.support.core.exceptions.JwtTokenMissingException" &&
                rootCause.message == "No valid token found in validation context"
            ) {
                ourLogger.debug("Suppressing error log message for JwtTokenMissingException wrapped in ServletException. This is probably due to expired token during async/SSE operations.")
                return FilterReply.DENY
            }
            if (
                throwable.javaClass.name == "jakarta.servlet.ServletException" &&
                (rootCause.javaClass.name == "no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException" ||
                 throwable.cause?.javaClass?.name == "no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException")
            ) {
                ourLogger.debug("Suppressing error log message for JwtTokenUnauthorizedException wrapped in ServletException. This is probably due to expired token during async/SSE operations.")
                return FilterReply.DENY
            }
        }

        return FilterReply.NEUTRAL
    }

    private fun getRootCause(throwable: Throwable): Throwable {
        var rootCause = throwable
        while (rootCause.cause != null && rootCause.cause != rootCause) {
            rootCause = rootCause.cause!!
        }
        return rootCause
    }
}