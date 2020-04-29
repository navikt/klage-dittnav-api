package no.nav.klage.config

import com.natpryce.konfig.ConfigurationProperties.Companion.fromResource
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

private val config = systemProperties() overriding
        EnvironmentVariables overriding
        fromResource("/application.yml")

private fun String.configProperty(): String = config[Key(this, stringType)]