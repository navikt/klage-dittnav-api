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
        if (throwable != null) {
            if (
                (throwable.javaClass.name == "java.io.IOException" &&
                 throwable.message == "Broken pipe" &&
                 logger?.name?.contains("org.apache.catalina.core.ContainerBase") == true
                )
            ) {
                ourLogger.debug("Suppressing error log message when broken pipe and logger is ${logger.name}. This is probably due to lost client during async/SSE operations.")
                return FilterReply.DENY
            }
        }

        if (level == Level.WARN && logger?.name == "org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver" && format?.contains("java.io.IOException: Broken pipe") == true) {
            ourLogger.debug("Suppressing warning log message when broken pipe and logger is ${logger.name}. This is probably due to lost client during async/SSE operations.")
            return FilterReply.DENY
        }

        if (level == Level.WARN && logger?.name == "no.nav.klage.config.problem.GlobalExceptionHandler" && format?.contains("Response already committed") == true) {
            ourLogger.debug("Suppressing warning log message when response already committed and logger is ${logger.name}. This is probably due to lost client during async/SSE operations.")
            return FilterReply.DENY
        }

        return FilterReply.NEUTRAL
    }
}