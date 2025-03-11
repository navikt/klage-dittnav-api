package no.nav.klage.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.turbo.TurboFilter
import ch.qos.logback.core.spi.FilterReply
import no.nav.klage.util.getLogger
import org.slf4j.Marker

class AuthorizationLogFilter : TurboFilter() {

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
                (level == Level.ERROR &&
                        throwable.javaClass.name == "no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException" &&
                        format?.contains("Exception thrown to client: Unauthorized, No authorization header in request") == true
                        )
            ) {
                ourLogger.debug("Suppressing error log message from missing authorization header. Probably expired token.")
                return FilterReply.DENY
            }
        }

        return FilterReply.NEUTRAL
    }
}