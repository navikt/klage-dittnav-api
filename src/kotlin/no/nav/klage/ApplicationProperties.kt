package no.nav.klage

class ApplicationProperties {
    val dbUrl: String = "jdbc:postgresql://".plus(getProperty("DB_HOST")).plus("/klage")
    val dbUsername: String = getProperty("DB_USERNAME")
    val dbPassword: String = getProperty("DB_PASSWORD")
}

fun getProperty(property: String, default: String? = null): String =
    System.getenv(property) ?: System.getProperty(property) ?: default
    ?: throw RuntimeException("Missing variable $property")