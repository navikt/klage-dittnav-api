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
                ) ||
                (throwable.javaClass.name == "AsyncRequestNotUsableException" &&
                 throwable.message?.contains("Broken pipe", ignoreCase = true) == true
                )
            ) {
                ourLogger.debug("Suppressing error log message when broken pipe and logger is ${logger?.name}. This is probably due to lost client during async/SSE operations.")
                return FilterReply.DENY
            } else {
                ourLogger.debug("Got another type of exception: ${throwable.javaClass.name} with message: ${throwable.message}")
                return FilterReply.NEUTRAL
            }
        }

        return FilterReply.NEUTRAL
    }
}