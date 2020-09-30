package no.nav.klage.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun getLogger(forClass: Class<*>): Logger = LoggerFactory.getLogger(forClass)
fun getSecureLogger(): Logger = LoggerFactory.getLogger("secure")

fun rootCause(t: Throwable): Throwable = t.cause?.run { rootCause(this) } ?: t
fun causeClass(t: Throwable) = t.stackTrace?.firstOrNull()?.className ?: ""