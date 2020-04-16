package no.nav.klage

class ApplicationProperties {
    val dbUrl: String = getProperty(PropertyNames.KLAGE_DITTNAV_API_DATASOURCE_URL)
    val vaultMountPath: String = getProperty(PropertyNames.KLAGE_DITTNAV_API_VAULT_MOUNT_PATH)
}

enum class PropertyNames {
    KLAGE_DITTNAV_API_DATASOURCE_URL,
    KLAGE_DITTNAV_API_VAULT_MOUNT_PATH
}

fun getProperty(property: PropertyNames, default: String? = null): String =
        System.getenv(property.name) ?: System.getProperty(property.name) ?: default
        ?: throw RuntimeException("Missing variable $property")