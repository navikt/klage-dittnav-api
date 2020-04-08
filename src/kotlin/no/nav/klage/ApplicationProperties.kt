package no.nav.klage

class ApplicationProperties {
    val dbUrl: String = getProperty(PropertyNames.KLAGE_DITTNAV_API_DATASOURCE_URL)
    val dbUsername: String = getProperty(PropertyNames.KLAGE_DITTNAV_API_DATASOURCE_USERNAME)
    val dbPassword: String = getProperty(PropertyNames.KLAGE_DITTNAV_API_DATASOURCE_PASSWORD)
}

enum class PropertyNames {
    KLAGE_DITTNAV_API_DATASOURCE_URL,
    KLAGE_DITTNAV_API_DATASOURCE_USERNAME,
    KLAGE_DITTNAV_API_DATASOURCE_PASSWORD
}

fun getProperty(property: PropertyNames, default: String? = null): String =
        System.getenv(property.name) ?: System.getProperty(property.name) ?: default
        ?: throw RuntimeException("Missing variable $property")